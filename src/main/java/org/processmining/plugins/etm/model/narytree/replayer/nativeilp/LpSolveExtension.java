package org.processmining.plugins.etm.model.narytree.replayer.nativeilp;

import lpsolve.LpSolve;

public class LpSolveExtension {

	static {
		System.loadLibrary("lpsolve55");
		System.loadLibrary("lpsolve55jext-nobenchmark");
		init();
	}

	private static native void init();

	/**
	 * Gets the LpSolveExtension native library version.
	 * @return The LpSolveExtension native library version.
	 */
	public static native int getVersion();

	/**
	 * Gets the enabled compile features in the native build.
	 * Each bit represents a feature, compare with the FEATURE_* constants.
	 * @return The enabled compile features in the native build.
	 */
	public static native int getFeatures();

	public static final int FEATURE_BENCHMARK = 1;
	public static final int FEATURE_BASISOPT = 2;

	/**
	 * Solves the LP identified by handle for the given right hand side rhs, 
	 * and places the resulting variables in vars if successful.
	 * 
	 * In case of a NUMFAILURE or INFEASIBLE, the default basis is set and 
	 * another solve is automatically triggered.
	 * 
	 * In case of a TIMEOUT or SUBOPTIMAL, the relaxed solution will be returned.
	 * @param handle The handle pointing to the lprec structure.
	 * @param rhs The array representing the right hand side values.
	 * @param vars The array in which the variables will be written.
	 * @return The solve method's result.
	 */
	public static native int solveForRhs(long handle, double[] rhs, double[] vars);
	
	/**
	 * Solves the LP identified by handle for the given right hand side rhs.
	 * 
	 * In case of a NUMFAILURE or INFEASIBLE, the default basis is set and
	 * another solve is automatically triggered.
	 * 
	 * The relaxed solution is not stored. The resulting variables have to be
	 * retrieved manually.
	 * @param handle The handle pointing to the lprec structure.
	 * @param rhs The array representing the right hand side values.
	 * @return The solve method's result.
	 */
	public static native int solveForRhs(long handle, double[] rhs);

	/**
	 * Returns the handle pointing to the lprec structure for a given LpSolve object.
	 * @param lp The LpSolve object.
	 * @return The handle pointing to the lprec structure.
	 */
	public static long getHandle(LpSolve lp) {
		return lp.getLp();
	}
	
	// Benchmark methods (lpsolve55jext needs to be compiled with BENCHMARK defined)
	public static native long getNumSolveCalls();
	public static native long getNumRelaxedUsed();
	public static native long getNumFailures();
	public static native long getNumRHSCopied();
	public static native long getInfeasibles();
	public static native long getTotalRHS();
	public static native long getTotalRelaxed();
	public static native long getTotalEntireSolve();
	public static native void clearStats();
}
