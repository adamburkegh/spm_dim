package org.processmining.plugins.etm.model.narytree.replayer.basisilp;

import java.util.Arrays;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.memefficient.CachedStorageAwareDelegate;
import nl.tue.astar.impl.memefficient.HeadDeflater;
import nl.tue.astar.util.ShortShortMultiset;

import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NAryTreeBasisILPDelegate extends AbstractNAryTreeLPDelegate<NAryTreeBasisILPTail> implements
		CachedStorageAwareDelegate<NAryTreeHead, NAryTreeBasisILPTail> {

	private final NAryTreeBasisILPTailCompressor tailCompressor;

	public NAryTreeBasisILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads) {
		this(algorithm, tree, configurationNumber, node2cost, threads, false);
	}

	public NAryTreeBasisILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads, boolean useOrRows) {
		super(algorithm, tree, configurationNumber, node2cost, threads, true, false, 1, algorithm
				.getLengthLongestTrace() + 1, useOrRows);
		this.tailCompressor = new NAryTreeBasisILPTailCompressor(getStateSizeInBytes(), columns, (short) algorithm
				.getClasses().size(), algorithm.getLengthLongestTrace() + 1, columns + rows + 1);
	}

	public NAryTreeBasisILPTailCompressor getTailInflater() {
		return tailCompressor;
	}

	public NAryTreeBasisILPTailCompressor getTailDeflater() {
		return tailCompressor;
	}

	public NAryTreeBasisILPTail createInitialTail(NAryTreeHead head) {
		return new NAryTreeBasisILPTail(this, head);
	}

	public HeadDeflater<NAryTreeHead> getHeadDeflater() {
		return headCompressor;
	}

	public BasisLPResult estimate(byte[] state, ShortShortMultiset parikh, int minCost, int[] basis) {
		double[] rhs = setupRhs(state, parikh, minCost);
		LpSolve solver = solvers.firstAvailable();
		synchronized (relaxed) {
			relaxed.put(solver, null);
		}

		// the stupid estimate is always a correct estimate
		BasisLPResult res = new BasisLPResult(columns, minCost, LpSolve.SUBOPTIMAL, null);
		try {

			solver.setRhVec(rhs);

			solver.setBasis(basis, true);

			int r = solver.solve();

			if (r == LpSolve.INFEASIBLE) {
				// the model deadlocks due to blocking, or there are issues with the set basis

				res = new BasisLPResult(columns, -2.0, LpSolve.INFEASIBLE, null);
				System.out.println(Arrays.toString(basis));
				solver.printLp();

				solver.defaultBasis();
				System.out.println(solver.solve());

				System.out.println("---------------");

			} else if (r == LpSolve.OPTIMAL || r == LpSolve.PRESOLVED) {
				int[] b2 = new int[rows + columns + 1];
				solver.getBasis(b2, true);
				// the solution was optimal. In case of an integer solution, use rounding
				// in case of an LP solution, round down.
				if (useInt) {
					res = new BasisLPResult(columns, solver.getObjective(), LpSolve.OPTIMAL, b2);
				} else {
					res = new BasisLPResult(columns, solver.getObjective(), LpSolve.SUBOPTIMAL, b2);
				}
				solver.getVariables(res.getVariables());

			} else if (useInt && timeOut > 0 && (r == LpSolve.TIMEOUT || r == LpSolve.SUBOPTIMAL)) {
				int[] b2 = new int[rows + columns + 1];
				solver.getBasis(b2, true);
				// timeout reached while using INT. use the relaxed solution if available
				double[] relaxedFound;
				synchronized (relaxed) {
					relaxedFound = relaxed.get(solver);
				}
				if (relaxedFound != null) {
					// timeout after a relaxed solution was found
					res = new BasisLPResult(relaxedFound, solver.getObjective(), LpSolve.SUBOPTIMAL, b2);
				}

			}

			//			if (rhs[rhs.length - 1] == toCatch) {
			//				System.out.println("Done");
			//			}

			return res;

		} catch (LpSolveException e) {
			System.out.println("_________________________________________________________");
			System.out.println(Arrays.toString(state) + "   " + parikh.toString());
			System.out.println(Arrays.toString(rhs));
			System.out.println(Arrays.toString(res.getVariables()) + " est:" + res.getResult());
			solver.printLp();
			assert false;
			return res;
		} finally {
			solvers.finished(solver);
		}
	}

	// Guessing the basis causes all kinds of problems. SOmetimes this leads to infeasibility, sometimes to numerical issues. DON'T USE THIS
	//	public int[] guessBasis(byte[] state, ShortShortMultiset parikh, int minCost, int[] oldBasis,
	//			short[] feasibleSolution) {
	//		//		return oldBasis;
	//		//		 Guessing a basis does not work properly and leads on infeasibility on feasible models.
	//		double[] rhs = setupRhs(state, parikh, minCost);
	//		LpSolve solver = solvers.firstAvailable();
	//		try {
	//			solver.setRhVec(rhs);
	//			int[] basis = new int[oldBasis.length];
	//			double[] vars = new double[feasibleSolution.length + 1];
	//			for (int i = vars.length - 1; i > 0;) {
	//				vars[i] = feasibleSolution[--i];
	//			}
	//			solver.setBasis(oldBasis, true);
	//			solver.guessBasis(vars, basis);
	//			return basis;
	//		} catch (LpSolveException e) {
	//			return oldBasis;
	//		} finally {
	//			solvers.finished(solver);
	//		}
	//	}

	public int columns() {
		return columns;
	}

	public int rows() {
		return rows;
	}

	public TreeRecord createInitialRecord(NAryTreeHead head, Trace trace) {
		return new TreeRecord(head.getMovesMadeCost(), head.getMovesMade(), head.getMovesMadeCost(), trace.getSize(),
				false);
	}
}
