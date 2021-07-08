package org.processmining.plugins.etm.model.narytree.replayer.basisilp2;

import java.util.Arrays;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import nl.tue.astar.util.LPResult;
import nl.tue.astar.util.ShortShortMultiset;

import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.hybridilp.NAryTreeHybridILPDelegate;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NAryTreeBasis2ILPDelegate extends NAryTreeHybridILPDelegate {

	public NAryTreeBasis2ILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads) {
		this(algorithm, tree, configurationNumber, node2cost, threads, false);
	}

	public NAryTreeBasis2ILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads, boolean useOrRows) {
		super(algorithm, tree, configurationNumber, node2cost, threads, useOrRows);
	}

	@Override
	public LPResult estimate(byte[] state, ShortShortMultiset parikh, int minCost) {
		double[] rhs = setupRhs(state, parikh, minCost);
		LpSolve solver = solvers.firstAvailable();
		synchronized (relaxed) {
			relaxed.put(solver, null);
		}

		// the stupid estimate is always a correct estimate
		LPResult res = new LPResult(columns, minCost, LpSolve.SUBOPTIMAL);
		try {

			solver.setRhVec(rhs);

			int r = solver.solve();

			if (r == LpSolve.INFEASIBLE || r == LpSolve.NUMFAILURE) {
				solver.resetBasis();
				r = solver.solve();
				if (r == LpSolve.INFEASIBLE || r == LpSolve.NUMFAILURE) {
					solver.defaultBasis();
					r = solver.solve();
				}
			}
			if (r == LpSolve.INFEASIBLE || r == LpSolve.NUMFAILURE) {

				res = new LPResult(columns, -2.0, LpSolve.INFEASIBLE);

			} else if (r == LpSolve.OPTIMAL || r == LpSolve.PRESOLVED) {
				// the solution was optimal. In case of an integer solution, use rounding
				// in case of an LP solution, round down.
				if (useInt) {
					res = new LPResult(columns, solver.getObjective(), LpSolve.OPTIMAL);
				} else {
					res = new LPResult(columns, solver.getObjective(), LpSolve.SUBOPTIMAL);
				}
				solver.getVariables(res.getVariables());

			} else if (useInt && timeOut > 0 && (r == LpSolve.TIMEOUT || r == LpSolve.SUBOPTIMAL)) {
				// timeout reached while using INT. use the relaxed solution if available
				double[] relaxedFound;
				synchronized (relaxed) {
					relaxedFound = relaxed.get(solver);
				}
				if (relaxedFound != null) {
					// timeout after a relaxed solution was found
					res = new LPResult(relaxedFound, solver.getObjective(), LpSolve.SUBOPTIMAL);
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
}
