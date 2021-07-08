package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;

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
	public double calculate(XLog log, AcceptingStochasticNet anet, XEventClassifier classifier) {
		return anet.getNet().getEdges().size();
	}

	@Override
	public String getUniqueId() {
		return "pnedges";
	}

	@Override
	public void precalculateForLog(XLog log, XEventClassifier classifier) {
		// No-op
	}

}
