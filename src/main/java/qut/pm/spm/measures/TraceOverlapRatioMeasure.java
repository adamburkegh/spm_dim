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
import qut.pm.spm.playout.PlayoutGenerator;

public class TraceOverlapRatioMeasure extends AbstractStochasticLogCachingMeasure {

	protected TraceFreq logTraceFreq;
	protected TraceFreq modelTraceFreq;
	
	public Measure getMeasure() {
		return Measure.TRACE_OVERLAP_RATIO;
	}

	public TraceOverlapRatioMeasure() {
		super();
	}
	
	public TraceOverlapRatioMeasure(PlayoutGenerator generator) {
		super(generator);
	}

	@Override
	public String getReadableId() {
		return "Trace Overlap Ratio";
	}
	
	@Override
	public String getUniqueId() {
		return "tror";
	}

	public void precalculateForLog(XLog log, XEventClassifier classifier) {
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
	protected double calculateForPlayout(XLog playoutLog, XEventClassifier classifier) {
		modelTraceFreq = calculateForLog(playoutLog,classifier);
		TraceFreq intf = intersect(logTraceFreq,modelTraceFreq);
		return ((double)intf.getTraceTotal()) / ((double)logTraceFreq.getTraceTotal()); 
	}

	
	protected TraceFreq intersect(TraceFreq traceFreq1, TraceFreq traceFreq2) {
		TraceFreq result = new TraceFreq();
		Set<List<String>> keys = new HashSet<>(traceFreq1.keySet());
		keys.addAll(traceFreq2.keySet());
		for (List<String> trace : keys ) {
			long t1 = traceFreq1.getFreq(trace);
			long t2 = traceFreq2.getFreq(trace);
			result.putFreq(trace, Math.min(t1,t2));
		}
		return result;
	}

	public String format() {
		return "TraceOverlapMeasure [logTraceFreq=" + logTraceFreq.format() + ", modelTraceFreq=" + modelTraceFreq.format() + "]";
	}

	
	
}
