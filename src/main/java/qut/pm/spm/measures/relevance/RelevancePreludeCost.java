package qut.pm.spm.measures.relevance;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.TraceFreq;

public interface RelevancePreludeCost {

	public double cost(FiniteStochasticLang logTF, TraceFreq modelTF);
	public void initCostRun(FiniteStochasticLang logTF, TraceFreq modelTF);

}
