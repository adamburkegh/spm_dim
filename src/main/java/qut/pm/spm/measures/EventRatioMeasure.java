package qut.pm.spm.measures;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import qut.pm.spm.Measure;
import qut.pm.spm.playout.PlayoutGenerator;

public class EventRatioMeasure extends TraceRatioMeasure {

	public EventRatioMeasure() {
		super(1);
	}

	public EventRatioMeasure(PlayoutGenerator generator) {
		super(generator,1);
	}
	
	@Override
	public String getReadableId() {
		return "Activity Ratio Gower's similarity";
	}
	
	@Override
	public String getUniqueId() {
		return "ergs";
	}

	public Measure getMeasure() {
		return Measure.EVENT_RATIO_GOWER;
	}
	
	protected TraceFreq calculateForLog(XLog log, XEventClassifier classifier) {
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

	protected double calculateForPlayout(XLog playoutLog, XEventClassifier classifier) {
		modelEventFreq = calculateForLog(playoutLog,classifier);
		return compareSubtraceFrequencies(logEventFreq,modelEventFreq);
	}


	
}
