package qut.pm.spm.measures.relevance;

import java.util.List;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.TraceFreq;

public interface RelevanceBackgroundCost {

	public void initCostRun(FiniteStochasticLang logFSL, TraceFreq modelTF);
	public double cost(List<String> trace, FiniteStochasticLang logFSL, TraceFreq modelTF);

}
