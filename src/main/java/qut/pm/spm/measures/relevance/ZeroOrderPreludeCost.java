package qut.pm.spm.measures.relevance;

import java.util.Set;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.MathUtils;
import qut.pm.spm.TraceFreq;

public class ZeroOrderPreludeCost implements RelevancePreludeCost {

	
	@Override
	/** 
	 * prelude_z(E,M) = 
	 * 	sum_{lambda in E} C_gamma ( n(lambda,E) +1 ) + C_gamma(|E|+1)
	 * 
	 * where C_gamma(x) = 2 hpwr(x) + 1 bits
	 * 		hpwr(z) = the highest power of 2 z is greater than or equal to,
	 * 				  hpwer(z) = N <=> 2^N <= x <= 2^{N+1} 
	 * ie the cost of the Elias gamma code
	 * 
	 * Paper has V for E
	 */
	public double cost(FiniteStochasticLang logTF, TraceFreq modelTF) {
		double prelSum = preludeLogModel(logTF); 
		return	prelSum/logTF.getTraceTotal();
	}

	public double preludeLogModel(FiniteStochasticLang logTF) {
		return preludeLogModel(logTF,logTF.getActivities());
	}
	
	public double preludeLogModel(FiniteStochasticLang logTF, Set<String> activities) {
		double sum = 0d;
		for( String activity: activities) {
			sum += eliasGamma( logTF.getActivityFreq(activity)+ 1 );
		}
		return (sum + eliasGamma(logTF.getTraceTotal() + 1 ));
	}
	
	/**
	 * 
	 * C_gamma(x) = 2 log2floor(x) + 1 bits
	 * ie the cost of the Elias gamma code
	 * 
	 * @param x
	 * @return
	 */
	public double eliasGamma(double x) {
		return 2* MathUtils.log2floor(x) + 1;
	}

	@Override
	public void initCostRun(FiniteStochasticLang logTF, TraceFreq modelTF) {
	}

	
}
