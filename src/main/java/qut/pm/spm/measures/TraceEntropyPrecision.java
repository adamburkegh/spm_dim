package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;

public class TraceEntropyPrecision implements StochasticLogCachingMeasure {

	private TraceEntropyMeasure traceEntropyMeasure = new TraceEntropyMeasure();
	
	public TraceEntropyPrecision(TraceEntropyMeasure traceEntropyMeasure) {
		this.traceEntropyMeasure = traceEntropyMeasure;
	}
	
	@Override
	public String getReadableId() {
		return "Trace Entropy Precision";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPY_PRECISION_TRACEWISE;
	}

	@Override
	public double calculate(XLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		traceEntropyMeasure.calculate(log,net,classifier);
		return traceEntropyMeasure.getEntropyPrecision();
	}

	@Override
	public String getUniqueId() {
		return "enttrp";
	}

	@Override
	public void precalculateForLog(XLog log, XEventClassifier classifier) {
		traceEntropyMeasure.precalculateForLog(log,classifier);
	}

}
