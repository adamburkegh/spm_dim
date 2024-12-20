package qut.pm.spm.measures;

import java.util.LinkedList;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.PlayoutGenerator;

public class GenTraceDiffMeasure extends AbstractStochasticLogCachingMeasure {

	protected TraceFreq logTraceFreq;
	protected TraceFreq modelTraceFreq;
	private GenTraceDiffCalculation gtdCalc = new GenTraceDiffCalculation();
	
	public Measure getMeasure() {
		return Measure.TRACE_GENERALIZATION_DIFF_UNIQ;
	}

	public GenTraceDiffMeasure() {
		super();
	}
	
	public GenTraceDiffMeasure(PlayoutGenerator generator) {
		super(generator);
	}

	@Override
	public String getReadableId() {
		return "Generalization by trace uniqueness diff";
	}
	
	@Override
	public String getUniqueId() {
		return "gentrdiffunq";
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
		return gtdCalc.calculateGeneralizationTraceDiff(logTraceFreq,modelTraceFreq);
	}

	
	public String format() {
		return "GenTraceDiffMeasure [logFreq=" + logTraceFreq.format() + ", modelFreq=" + modelTraceFreq.format() + "]";
	}

	
	
}
