package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

public class EntityCount implements StochasticLogCachingMeasure{

	@Override
	public String getReadableId() {
		return "Entity count";
	}

	@Override
	public Measure getMeasure() {
		return Measure.MODEL_ENTITY_COUNT;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet anet, XEventClassifier classifier) {
		StochasticNet net = anet.getNet();
		return net.getPlaces().size() + net.getTransitions().size();
	}

	@Override
	public String getUniqueId() {
		return "pnentities";
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		// No-op
	}

}
