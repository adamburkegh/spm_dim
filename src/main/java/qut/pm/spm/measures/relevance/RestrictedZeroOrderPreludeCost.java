package qut.pm.spm.measures.relevance;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.FiniteStochasticLangGenerator;
import qut.pm.spm.TraceFreq;

public class RestrictedZeroOrderPreludeCost implements RelevancePreludeCost{

	protected FiniteStochasticLang logNotM = null;
	protected TraceFreq modelTF = null;
	protected ZeroOrderPreludeCost zop = new ZeroOrderPreludeCost();
	
	@Override
	public void initCostRun(FiniteStochasticLang logFSL, TraceFreq modelTF) {
		logNotM =  new FiniteStochasticLangGenerator().
						restrictByLog(logFSL, modelTF);
		this.modelTF = modelTF;
		zop.initCostRun(logNotM, modelTF);
	}

	
	@Override
	public double cost(FiniteStochasticLang logTF, TraceFreq modelTF) {
		return zop.preludeLogModel(logNotM,logTF.getActivities()) / logTF.getTraceTotal();
	}


}
