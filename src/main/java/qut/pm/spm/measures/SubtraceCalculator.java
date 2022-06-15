package qut.pm.spm.measures;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import qut.pm.spm.TraceFreq;

public class SubtraceCalculator {

	public TraceFreq calculateActivityFreq(XLog log, XEventClassifier classifier) {
		TraceFreq result = new TraceFreq();
		for (XTrace trace: log) {
			for (XEvent event: trace) {
				List<String> subtrace = new ArrayList<>();
				String classId = classifier.getClassIdentity(event);
				subtrace.add(classId);
				result.incTraceFreq(subtrace);
			}
		}
		return result;
	}
	
	protected TraceFreq calculateForLog(XLog log, XEventClassifier classifier, int subtraceLength) {
		TraceFreq result = new TraceFreq();
		for (XTrace trace: log) {
			LinkedList<List<String>> subtraces = new LinkedList<List<String>>();
			for (XEvent event: trace) {
				String classId = classifier.getClassIdentity(event);
				ListIterator<List<String>> lli = subtraces.listIterator();
				while(lli.hasNext() ) {
					List<String> subtrace = lli.next();
					subtrace.add(classId);
					if (subtrace.size() == subtraceLength) {
						result.incTraceFreq(subtrace);
						lli.remove();
					}
				}
				LinkedList<String> newTrace = new LinkedList<String>();
				newTrace.add(classId);
				subtraces.add(newTrace);
			}
		}
		return result;
	}

	public TraceFreq calculateForTraceFreq(TraceFreq log, int subtraceLength) {
		if (subtraceLength == 1)
			return calculateActivityFreq(log);
		TraceFreq result = new TraceFreq();
		Map<List<String>, Double> mlog = log.getTraceFreq();
		for (List<String> trace: mlog.keySet()) {
			LinkedList<List<String>> subtraces = new LinkedList<List<String>>();
			for (String event: trace) {
				ListIterator<List<String>> lli = subtraces.listIterator();
				while(lli.hasNext() ) {
					List<String> subtrace = lli.next();
					subtrace.add(event);
					if (subtrace.size() == subtraceLength) {
						result.incTraceFreq(subtrace, mlog.get(trace) );
						lli.remove();
					}
				}
				List<String> newTrace = new LinkedList<String>();
				newTrace.add(event);
				subtraces.add(newTrace);
			}
		}
		return result;
	}
	

	public TraceFreq calculateActivityFreq(TraceFreq tf) {
		TraceFreq result = new TraceFreq();
		Map<List<String>, Double> mlog = tf.getTraceFreq();
		for (List<String> trace: mlog.keySet() ) {
			for (String event: trace) {
				List<String> subtrace = new ArrayList<>();
				subtrace.add(event);
				result.incTraceFreq(subtrace,mlog.get(trace));
			}
		}
		return result;
	}

}
