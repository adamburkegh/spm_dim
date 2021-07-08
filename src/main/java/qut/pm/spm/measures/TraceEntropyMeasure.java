package qut.pm.spm.measures;

import java.util.LinkedList;
import java.util.List;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import qut.pm.spm.Measure;
import qut.pm.spm.playout.PlayoutGenerator;

public class TraceEntropyMeasure extends AbstractStochasticLogCachingMeasure {

	private static final double LOG2 = Math.log(2);
	private TraceFreq traceFreqLog;
	private TraceFreq traceFreqModel;
	private double logEntropy;
	private double modelEntropy;
	private double intersectEntropy;
	private double intersectEntropyVsFullLog;
	private XLog lastPlayout;
	
	public TraceEntropyMeasure(PlayoutGenerator generator) {
		super(generator);
	}
	
	public TraceEntropyMeasure() {
		super();
	}
	
	@Override
	public void precalculateForLog(XLog log, XEventClassifier classifier) {
		validateLogCache(log, classifier);
		traceFreqLog = preCalculateForLog(log, classifier);
	}

	private TraceFreq preCalculateForLog(XLog log, XEventClassifier classifier) {
		TraceFreq result = new TraceFreq();
		for (XTrace trace: log) {
			List<String> transTrace = new LinkedList<String>();
			for (XEvent event: trace) {
				String classId = classifier.getClassIdentity(event);
				transTrace.add(classId);
			}
			result.incTraceFreq(transTrace);
		}
		return result;
	}
	
	@Override
	public String getReadableId() {
		return "Trace Entropy";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPY_TRACEWISE;
	}

	@Override
	public String getUniqueId() {
		return "enttr";
	}
	
	public double getEntropyPrecision() {
		return (modelEntropy == 0)? 0: intersectEntropyVsFullLog / modelEntropy;
	}

	public double getEntropyFitness() {
		return (logEntropy == 0)? 0: intersectEntropyVsFullLog / logEntropy;
	}

	
	@Override
	protected double calculateForPlayout(XLog playoutLog, XEventClassifier classifier) {
		if (lastPlayout == playoutLog)
			return intersectEntropy;
		lastPlayout = playoutLog;
		traceFreqModel = preCalculateForLog(playoutLog,classifier);
		logEntropy = entropyForTraceFreq(traceFreqLog);
		modelEntropy = entropyForTraceFreq(traceFreqModel);
		TraceFreq intersectTF = intersect(traceFreqLog,traceFreqModel);
		intersectEntropy = entropyForTraceFreq(intersectTF);
		intersectTF.forceTraceTotal(traceFreqLog.getTraceTotal());
		intersectEntropyVsFullLog = entropyForTraceFreq(intersectTF);
		return intersectEntropy; /// BUT actually we record all three
	}

	private TraceFreq intersect(TraceFreq traceFreq1, TraceFreq traceFreq2) {
		TraceFreq result = new TraceFreq();
		for (List<String> trace: traceFreq1.keySet()) {
			long f1 = traceFreq1.getFreq(trace);
			long f2 = traceFreq2.getFreq(trace);
			result.putFreq(trace,Math.min(f1,f2));
		}
		return result;
	}

	public static double entropyForTraceFreq(TraceFreq traceFreq) {
		double entropy = 0;
		if (traceFreq.getTraceTotal() == 0)
			return 0.0d;
		for (List<String> trace: traceFreq.keySet()) {
			double prob = (double)traceFreq.getFreq(trace)/(double)traceFreq.getTraceTotal();
			if (prob == 0.0) // convention that log 0 == 0
				continue;
			entropy += prob * Math.log(prob) / LOG2;
		}
		return -1.0d * entropy;
	}

}
