package qut.pm.spm;

public class MathUtils {

	private static final double LOG2 = Math.log(2);
	
	public static double log2(double in) {
		return Math.log(in) / LOG2;
	}

	/**
	 * 
	 * 		hpwr(z) = the highest power of 2 that z is greater than or equal to,
	 * 				  hpwer(z) = N <=> 2^N <= z <= 2^{N+1} 
	 * 		sometimes written [log_2 z]
	 * 
	 * @param x
	 * @return
	 */
	
	public static double log2floor(double x) {
		return Math.floor(  MathUtils.log2(x) );
	}
	
}
