package qut.pm.spm.measures.relevance;

import java.util.List;

import org.ujmp.core.util.MathUtil;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.TraceFreq;

public class ZeroOrderBackgroundCost implements RelevanceBackgroundCost{

	@Override
	/**
	 * modelTF is unused in the zero order background cost model.
	 *  
	 *  bits_Z(t,E,M) = - sum_{i=1}^{|th} log_2( P(th_i | Eh ) )
	 *  
	 * P(th_i | Eh) is relative activity frequency of the activity at th(i)
	 *  
 	 * In the paper a hat notation is used to indicate a terminal symbol
	 * is included. Here we include the +1 in the formula itself.
	 */
	public double cost(List<String> trace, FiniteStochasticLang logFSL, 
					   TraceFreq modelTF) 
	{
		final double eventCtWithTerminals = 
				logFSL.getTotalEvents() + logFSL.getTraceTotal();  
		double sum = 0d;
		for (String event: trace) {
			sum += MathUtil.log2( 
					logFSL.getActivityFreq(event) / eventCtWithTerminals );
		}
		sum += MathUtil.log2( 
				logFSL.getTraceTotal() / eventCtWithTerminals );
		return -1 * sum;
	}

	@Override
	public void initCostRun(FiniteStochasticLang logTF, TraceFreq modelTF) {
	}

}
