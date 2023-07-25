package qut.pm.spm.measures.relevance;

import java.util.List;

import org.ujmp.core.util.MathUtil;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.TraceFreq;

public class UniformBackgroundCost implements RelevanceBackgroundCost{

	private double logActivityCount = 0;
	
	@Override
	/**
	 * logTF and modelTF are unused in the universal background cost model,
	 * except for the logActivityCount.
	 * 
	 *  bits_U(t,E,M) = (|t|+1)log_2( |u(E)|+1 )
	 *  
 	 * In the paper a hat notation is used to indicate a terminal symbol
	 * is included. Here we include the +1 in the formula itself.
	 */
	public double cost(List<String> trace, FiniteStochasticLang logTF, TraceFreq modelTF) {
		return (trace.size() + 1) * MathUtil.log2( logActivityCount+1 );
	}

	@Override
	public void initCostRun(FiniteStochasticLang logTF, TraceFreq modelTF) {
		this.logActivityCount = logTF.getActivityFrequency().getActivities().size();
	}

}
