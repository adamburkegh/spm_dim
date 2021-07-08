package org.processmining.plugins.etm.model.narytree.replayer.basisilp;

import nl.tue.astar.util.LPResult;

public class BasisLPResult extends LPResult {

	private final int[] basis;

	public BasisLPResult(int variableCount, double result, int solveResult, int[] basis) {
		super(variableCount, result, solveResult);
		this.basis = basis;
	}

	public BasisLPResult(double[] variables, double result, int solveResult, int[] basis) {
		super(variables, result, solveResult);
		this.basis = basis;
	}

	public int[] getBasis() {
		return basis;
	}

}
