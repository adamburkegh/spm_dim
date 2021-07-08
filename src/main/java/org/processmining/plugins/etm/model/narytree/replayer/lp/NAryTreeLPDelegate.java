package org.processmining.plugins.etm.model.narytree.replayer.lp;

import java.util.Arrays;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import nl.tue.astar.util.LPResult;
import nl.tue.astar.util.ShortShortMultiset;

import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.hybridilp.NAryTreeHybridILPDelegate;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NAryTreeLPDelegate extends NAryTreeHybridILPDelegate {

	public NAryTreeLPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads) {
		this(algorithm, tree, configurationNumber, node2cost, threads, false);
	}

	public NAryTreeLPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads, boolean useOrRows) {
		super(algorithm, tree, configurationNumber, node2cost, threads, useOrRows, false);
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
				res = new LPResult(columns, solver.getObjective(), LpSolve.SUBOPTIMAL);

				solver.getVariables(res.getVariables());

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

}
