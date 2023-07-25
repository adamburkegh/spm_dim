package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.measures.TraceEntropyMeasure.TraceEntropyMeasurement;

public class TraceEntropyFitness implements StochasticLogCachingMeasure {

	private TraceEntropyMeasure traceEntropyMeasure = new TraceEntropyMeasure();
	
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
	public double calculate(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		TraceEntropyMeasurement tem = traceEntropyMeasure.calculateTraceEntropyMeasure(log,net,classifier);
		return tem.getEntropyFitness();
	}

	@Override
	public String getUniqueId() {
		return "enttrf";
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		traceEntropyMeasure.precalculateForLog(log,classifier);
	}

}
