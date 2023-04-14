package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

public class EdgeCount implements StochasticLogCachingMeasure{

	@Override
	public String getReadableId() {
		return "Edge count";
	}

	@Override
	public Measure getMeasure() {
		return Measure.MODEL_EDGE_COUNT;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet anet, XEventClassifier classifier) {
		return anet.getNet().getEdges().size();
	}

	@Override
	public String getUniqueId() {
		return "pnedges";
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		// No-op
	}

}
