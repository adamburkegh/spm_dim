package qut.pm.spm.measures.relevance;

import java.util.List;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.FiniteStochasticLangGenerator;
import qut.pm.spm.TraceFreq;

public class RestrictedZeroOrderBackgroundCost implements RelevanceBackgroundCost {

	protected FiniteStochasticLang logNotM = null;
	protected TraceFreq modelTF = null;
	protected ZeroOrderBackgroundCost zob = new ZeroOrderBackgroundCost();
	
	@Override
	public void initCostRun(FiniteStochasticLang logFSL, TraceFreq modelTF) {
		FiniteStochasticLang logNotM =  new FiniteStochasticLangGenerator().
				restrictByLog(logFSL, modelTF);
		this.logNotM = logNotM;
		this.modelTF = modelTF;
		zob.initCostRun(logNotM, modelTF);
	}

	@Override
	/**
	 * 
	 *  bits_R(t,E,M) = -bits_z(t,E_{not M},M)
	 *  					if t in E_{not M}
	 *  			  = +infinity, otherwise 
	 *
	 *  See ZeroOrderBackgroundCost for a description of bits_z()
	 */
	public double cost(List<String> trace, FiniteStochasticLang logFSL, 
					   TraceFreq modelTF) 
	{
		return zob.cost(trace, logNotM, modelTF);
	}

}
