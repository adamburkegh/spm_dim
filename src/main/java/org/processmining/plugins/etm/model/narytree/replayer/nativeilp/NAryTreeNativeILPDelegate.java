package org.processmining.plugins.etm.model.narytree.replayer.nativeilp;

import java.util.Arrays;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.memefficient.CachedStorageAwareDelegate;
import nl.tue.astar.impl.memefficient.HeadDeflater;
import nl.tue.astar.util.LPResult;
import nl.tue.astar.util.ShortShortMultiset;

import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NAryTreeNativeILPDelegate extends AbstractNAryTreeLPDelegate<NAryTreeNativeILPTail> implements
		CachedStorageAwareDelegate<NAryTreeHead, NAryTreeNativeILPTail> {

	private final NAryTreeNativeILPTailCompressor tailCompressor;
	private final boolean useBasisHistory;

	public NAryTreeNativeILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads, boolean useOrRows) {
		this(algorithm, tree, configurationNumber, node2cost, threads, useOrRows, true);
	}

	public NAryTreeNativeILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads, boolean useOrRows, boolean useBasisHistory) {
		this(algorithm, tree, configurationNumber, node2cost, threads, useOrRows, useBasisHistory, true);
	}

	/**
	 * @deprecated Temporary constructor to allow LP tests
	 */
	@Deprecated
	public NAryTreeNativeILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads, boolean useOrRows, boolean useBasisHistory, boolean useInt) {
		super(algorithm, tree, configurationNumber, node2cost, threads, useInt, false, 1, algorithm
				.getLengthLongestTrace() + 1, useOrRows);
		this.tailCompressor = new NAryTreeNativeILPTailCompressor(getStateSizeInBytes(), columns, (short) algorithm
				.getClasses().size(), algorithm.getLengthLongestTrace() + 1, 1 + rows + columns);
		this.useBasisHistory = useBasisHistory;
	}

	public NAryTreeNativeILPTailCompressor getTailInflater() {
		return tailCompressor;
	}

	public NAryTreeNativeILPTailCompressor getTailDeflater() {
		return tailCompressor;
	}

	public NAryTreeNativeILPTail createInitialTail(NAryTreeHead head) {
		return new NAryTreeNativeILPTail(this, head, 0, useBasisHistory ? new BasisHistory(1 + rows + columns) : null);
	}

	public HeadDeflater<NAryTreeHead> getHeadDeflater() {
		return headCompressor;
	}

	@Override
	protected boolean canUtilizeMsgfunc() {
		return false; // the relaxed solution is tracked internally in LpSolveExtension
	}

	@Override
	public LPResult estimate(byte[] state, ShortShortMultiset parikh, int minCost) {
		return estimate(state, parikh, minCost, null);
	}

	public LPResult estimate(byte[] state, ShortShortMultiset parikh, int minCost, BasisHistory basisHistory) {

		//long start = System.currentTimeMillis();

		LpSolve solver = solvers.firstAvailable();

		long lpHandle = LpSolveExtension.getHandle(solver);

		// the stupid estimate is always a correct estimate
		LPResult res = new LPResult(columns, minCost, LpSolve.SUBOPTIMAL);
		try {
			// the type of the RHS vector is determined by maxValRHS[0], i.e. this stores the maximal
			// value of any element of the RHS vector, except the last which is always an integer.

			// the type of the variables vector is determined by the variable upper bound, i.e. no variable will
			// ever get a value higher than this.varUpBo.

			if (basisHistory != null && basisHistory.isInitialized())
				solver.setBasis(basisHistory.getBasis(), true); // load basis

			double[] variables = new double[columns];
			int r = LpSolveExtension.solveForRhs(lpHandle, setupRhs(state, parikh, minCost), variables);

			if (r != LpSolve.INFEASIBLE) {
				res = new LPResult(columns, solver.getObjective(), useInt ? LpSolve.OPTIMAL : LpSolve.SUBOPTIMAL);
				double[] resVars = res.getVariables();

				for (int i = 0; i < columns; ++i)
					resVars[i] = variables[i];
			} else {
				// a node needs to be used more than varUpBo times, which this IP does not allow
				// or the model deadlocks due to blocking.
				res = new LPResult(columns, -2.0, LpSolve.INFEASIBLE);
				//solver.printLp();
			}

			if (basisHistory != null) {
				solver.getBasis(basisHistory.getBasis(), true); // save basis
				basisHistory.setInitialized();
			}

			return res;

		} catch (LpSolveException e) {
			System.out.println("_________________________________________________________");
			System.out.println(Arrays.toString(state) + "   " + parikh.toString());
			//System.out.println(Arrays.toString(rhs));
			System.out.println(Arrays.toString(res.getVariables()) + " est:" + res.getResult());
			solver.printLp();
			assert false;
			return res;
		} finally {
			solvers.finished(solver);

			//long end = System.currentTimeMillis();
		}
	}

	public TreeRecord createInitialRecord(NAryTreeHead head, Trace trace) {
		return new TreeRecord(head.getMovesMadeCost(), head.getMovesMade(), head.getMovesMadeCost(), trace.getSize(),
				false);
	}

}
