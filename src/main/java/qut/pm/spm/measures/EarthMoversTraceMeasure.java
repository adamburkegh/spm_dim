package qut.pm.spm.measures;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.playout.PlayoutGenerator;

public class EarthMoversTraceMeasure extends AbstractStochasticLogCachingMeasure {

	private static Logger LOGGER = LogManager.getLogger();
	
	private static final double EPSILON = 0.0001;
	protected TraceFreq logTraceFreq;
	protected TraceFreq modelTraceFreq;
	
	public Measure getMeasure() {
		return Measure.EARTH_MOVERS_TRACEWISE;
	}

	public EarthMoversTraceMeasure() {
		super();
	}
	
	public EarthMoversTraceMeasure(PlayoutGenerator generator) {
		super(generator);
	}

	@Override
	public String getReadableId() {
		return "Earth movers' distance by trace";
	}
	
	@Override
	public String getUniqueId() {
		return "emtr";
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
		return emcalc(logTraceFreq,modelTraceFreq);
	}

	
	protected double emcalc(TraceFreq traceFreqLog, TraceFreq traceFreqModel) {
		double sigma = 0.0d;
		Set<List<String>> keys = new HashSet<>(traceFreqLog.keySet());
		keys.addAll(traceFreqModel.keySet());
		for (List<String> trace : keys ) {
			double flog = traceFreqLog.getFreq(trace);
			double fmodel = traceFreqModel.getFreq(trace);
			double dist = flog/(double)traceFreqLog.getTraceTotal() 
					   -  fmodel/(double)traceFreqModel.getTraceTotal();
			if (dist > 0)
				sigma += dist;
		}
		if (sigma > 1.0d + EPSILON)
			LOGGER.error("EarthMoversTrace sum component > 1");
		if (sigma > 1.0d)
			return 0.0d;
		return 1.0d - sigma;
	}

	public String format() {
		return "EarthMoversTraceMeasure [logFreq=" + logTraceFreq.format() + ", modelFreq=" + modelTraceFreq.format() + "]";
	}

	
	
}
