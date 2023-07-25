package qut.pm.spm.measures;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.PlayoutGenerator;

public class GenTraceFloorMeasure extends AbstractStochasticLogCachingMeasure {

	protected TraceFreq logTraceFreq;
	protected TraceFreq modelTraceFreq;
	protected int thresholdQ;
	
	public Measure getMeasure() {
		switch (thresholdQ) {
		case(1):
			return Measure.TRACE_GENERALIZATION_FLOOR_1;
		case(5):
			return Measure.TRACE_GENERALIZATION_FLOOR_5;
		case(10):
			return Measure.TRACE_GENERALIZATION_FLOOR_10;
		case(20):
			return Measure.TRACE_GENERALIZATION_FLOOR_20;
		}
		throw new MeasureDefinitionException("No measure enum defined for level:" + thresholdQ);
	}

	public GenTraceFloorMeasure(int thresholdQ) {
		super();
		this.thresholdQ = thresholdQ;
	}
	
	public GenTraceFloorMeasure(PlayoutGenerator generator, int thresholdQ) {
		super(generator);
		this.thresholdQ = thresholdQ;
	}

	@Override
	public String getReadableId() {
		return "Generalization by trace floor (" + thresholdQ + ")";
	}
	
	@Override
	public String getUniqueId() {
		return "genfltr";
	}

	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		validateLogCache(log, classifier);
		logTraceFreq = calculateForLog(log, classifier);
	}

	
	protected TraceFreq calculateForLog(XLog log, XEventClassifier classifier) {
		TraceFreq result = new TraceFreq();
		for (XTrace trace: log) {
			LinkedList<String> newTrace = new LinkedList<String>();
			for (XEvent event: trace) {
				String classId = classifier.getClassIdentity(event);
				newTrace.add(classId);
			}
			result.incTraceFreq(newTrace);
		}
		return result;
	}

	@Override
	protected double calculateForPlayout(TraceFreq playoutLog) {
		modelTraceFreq = playoutLog;
		return gencalc(logTraceFreq,modelTraceFreq);
	}

	
	protected double gencalc(TraceFreq traceFreqLog, TraceFreq traceFreqModel) {
		double ct = 0.0d;
		Set<List<String>> keys = new HashSet<>(traceFreqLog.keySet());
		keys.addAll(traceFreqModel.keySet());
		for (List<String> trace : keys ) {
			double flog = traceFreqLog.getFreq(trace);
			double fmodel = traceFreqModel.getFreq(trace);
			if (flog >= thresholdQ && fmodel > 0)
				ct+= flog;
		}
		return ct / (double)traceFreqLog.getTraceTotal();
	}

	public String format() {
		return "GenTraceFloorMeasure [logFreq=" + logTraceFreq.format() + ", modelFreq=" + modelTraceFreq.format() + "]";
	}

	
	
}
