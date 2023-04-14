package qut.pm.spm.measures;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.FiniteStochasticLangGenerator;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.PlayoutGenerator;

public class EarthMoversTraceMeasure extends AbstractStochasticLogCachingMeasure {

	private static Logger LOGGER = LogManager.getLogger();
	
	private static final double EPSILON = 0.0001;
	protected TraceFreq logTraceFreq;
	protected TraceFreq modelTraceFreq;
	
	private FiniteStochasticLangGenerator traceFreqGen = new FiniteStochasticLangGenerator();
	
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

	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		validateLogCache(log, classifier);
		logTraceFreq = traceFreqGen.calculateTraceFreqForLog(log, classifier);
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
