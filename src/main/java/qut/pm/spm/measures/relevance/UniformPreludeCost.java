package qut.pm.spm.measures.relevance;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.TraceFreq;

public class UniformPreludeCost implements RelevancePreludeCost {

	@Override
	public double cost(FiniteStochasticLang logTF, TraceFreq modelTF) {
		return 0;
	}

	@Override
	public void initCostRun(FiniteStochasticLang logTF, TraceFreq modelTF) {
		// Deliberately empty
	}

}
