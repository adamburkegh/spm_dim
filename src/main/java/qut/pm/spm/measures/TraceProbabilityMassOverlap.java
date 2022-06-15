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
import qut.pm.spm.playout.PlayoutGenerator;

public class TraceProbabilityMassOverlap extends AbstractStochasticLogCachingMeasure {

	protected TraceFreq logTraceFreq;
	protected TraceFreq modelTraceFreq;
	
	public Measure getMeasure() {
		return Measure.TRACE_PROBMASS_OVERLAP;
	}

	public TraceProbabilityMassOverlap() {
		super();
	}
	
	public TraceProbabilityMassOverlap(PlayoutGenerator generator) {
		super(generator);
	}

	@Override
	public String getReadableId() {
		return "Trace Probability Mass Overlap";
	}
	
	@Override
	public String getUniqueId() {
		return "trpmo";
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
	protected double calculateForPlayout(TraceFreq playoutLog) {
		modelTraceFreq = playoutLog;
		double result = intersectingProbMass(logTraceFreq,modelTraceFreq);
		return result; 
	}

	
	protected double intersectingProbMass(TraceFreq traceFreq1, TraceFreq traceFreq2) {
		double result = 0;
		if (traceFreq1.getTraceTotal() == 0 || traceFreq1.getTraceTotal() == 0) {
			return 0;
		}
		Set<List<String>> keys = new HashSet<>(traceFreq1.keySet());
		keys.addAll(traceFreq2.keySet());
		for (List<String> trace : keys ) {
			double t1 = traceFreq1.getFreq(trace);
			double t2 = traceFreq2.getFreq(trace);
			double intFreq = Math.min(t1,t2);
			result += intFreq / (double)traceFreq1.getTraceTotal();
		}
		return result;
	}

	public String format() {
		return "TraceProbabilityMassOverlap [logTraceFreq=" + logTraceFreq.format() + ", modelTraceFreq=" + modelTraceFreq.format() + "]";
	}

	
	
}
