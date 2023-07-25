package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.measures.TraceEntropyMeasure.TraceEntropyMeasurement;

public class TraceEntropyProjectPrecision implements StochasticLogCachingMeasure {

	private TraceEntropyMeasure traceEntropyMeasure = new TraceEntropyMeasure();
	
	public TraceEntropyProjectPrecision(TraceEntropyMeasure traceEntropyMeasure) {
		this.traceEntropyMeasure = traceEntropyMeasure;
	}
	
	@Override
	public String getReadableId() {
		return "Trace Entropy Projection Precision";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPY_PRECISION_TRACEPROJECT;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		TraceEntropyMeasurement tem = traceEntropyMeasure.calculateTraceEntropyMeasure(log,net,classifier);
		return tem.getEntropyProjectPrecision();
	}

	@Override
	public String getUniqueId() {
		return "enttjp";
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		traceEntropyMeasure.precalculateForLog(log,classifier);
	}

}
