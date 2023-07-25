package qut.pm.spm;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class FiniteStochasticLangGenerator {

	public TraceFreq calculateTraceFreqForLog(XLog log, 
											  XEventClassifier classifier) {
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

	public FiniteStochasticLang calculateFSLForTF(TraceFreq tf) {
		ActivityFreq actFreq = calculateActivityFreqForTF(tf);
		FiniteStochasticLang result = new FiniteStochasticLang(tf,actFreq);
		return result;
	}
	
	public ActivityFreq calculateActivityFreqForTF(TraceFreq tf) {
		ActivityFreq result = new ActivityFreq();
		for (List<String> trace: tf.getTraceFreq().keySet()) {
			for (String event: trace) {
				result.incActiviyFreq(event, tf.getFreq(trace));
			}
		}
		return result;
	}

	public FiniteStochasticLang calculateForLog(XLog log, XEventClassifier classifier) {
		TraceFreq traceFreq = new TraceFreq();
		ActivityFreq activityFreq = new ActivityFreq();
		for (XTrace trace: log) {
			LinkedList<String> newTrace = new LinkedList<String>();
			for (XEvent event: trace) {
				String classId = classifier.getClassIdentity(event);
				newTrace.add(classId);
				activityFreq.incActivityFreq(classId);
			}
			traceFreq.incTraceFreq(newTrace);
		}
		FiniteStochasticLang result = new FiniteStochasticLang(traceFreq,activityFreq);
		return result;
	}

	public FiniteStochasticLang restrictByLog(FiniteStochasticLang logFSL, TraceFreq modelTF) {
		TraceFreq logNotMTF = new TraceFreq();
		Map<List<String>, Double> origTF = logFSL.getTraceFrequency().getTraceFreq();
		for (List<String> trace: origTF.keySet() ) {
			if (! (modelTF.getFreq(trace) > 0)) {
				logNotMTF.incTraceFreq(trace, origTF.get(trace) );
			}
		}
		FiniteStochasticLang logNotM = calculateFSLForTF(logNotMTF);
		return logNotM;
	}
	
}
