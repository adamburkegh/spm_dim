package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;

public class TraceEntropyFitness implements StochasticLogCachingMeasure {

	private TraceEntropyMeasure traceEntropyMeasure;
	
	public TraceEntropyFitness(TraceEntropyMeasure traceEntropyMeasure) {
		this.traceEntropyMeasure = traceEntropyMeasure;
	}
	
	@Override
	public String getReadableId() {
		return "Trace Entropy Fitness";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPY_FITNESS_TRACEWISE;
	}

	@Override
	public double calculate(XLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		traceEntropyMeasure.calculate(log,net,classifier);
		return traceEntropyMeasure.getEntropyFitness();
	}

	@Override
	public String getUniqueId() {
		return "enttrf";
	}

	@Override
	public void precalculateForLog(XLog log, XEventClassifier classifier) {
		traceEntropyMeasure.precalculateForLog(log,classifier);
	}

}
