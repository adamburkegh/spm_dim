package qut.pm.spm.measures;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.PlayoutGenerator;

public class TraceRatioMeasure extends AbstractStochasticLogCachingMeasure {

	protected TraceFreq logEventFreq;
	protected TraceFreq modelEventFreq;
	protected SubtraceCalculator subtraceCalc = new SubtraceCalculator();
	
	protected final int subtraceLength;
	

	public Measure getMeasure() {
		switch(subtraceLength) {
			case 2: 
				return Measure.TRACE_RATIO_GOWER_2;
			case 3: 
				return Measure.TRACE_RATIO_GOWER_3;
			case 4: 
				return Measure.TRACE_RATIO_GOWER_4;
		}
		return null;
	}

	
	public TraceRatioMeasure(int subtraceLength) {
		super();
		this.subtraceLength = subtraceLength;
	}
	
	public TraceRatioMeasure(PlayoutGenerator generator, int subtraceLength) {
		super(generator);
		this.subtraceLength = subtraceLength;
	}


	@Override
	public String getReadableId() {
		return "Trace Ratio (" + subtraceLength + ") Gower's similarity";
	}
	
	@Override
	public String getUniqueId() {
		return "trgs" + subtraceLength;
	}

	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		validateLogCache(log, classifier);
		logEventFreq = calculateForLog(log, classifier);
	}

	
	protected TraceFreq calculateForLog(XLog log, XEventClassifier classifier) {
		return subtraceCalc.calculateForLog(log,classifier,subtraceLength);
	}
	
	@Override
	protected double calculateForPlayout(TraceFreq playoutLog) {
		modelEventFreq = subtraceCalc.calculateForTraceFreq(playoutLog, subtraceLength);
		return compareSubtraceFrequencies(logEventFreq,modelEventFreq);
	}

	
	protected double compareSubtraceFrequencies(TraceFreq traceFreq1, TraceFreq traceFreq2) {
		// Gower's distance of two vectors of trace ratios
 		double cumulativeSum = 0;
		Set<List<String>> keys = new HashSet<>(traceFreq1.keySet());
		keys.addAll(traceFreq2.keySet());
		if (keys.isEmpty())
			return 0;
		for (List<String> subtrace : keys ) {
			double r1 = calcRatio(traceFreq1, subtrace);
			double r2 = calcRatio(traceFreq2, subtrace);
			double diff = (r1 - r2) / Math.max(r1,r2);
 			cumulativeSum += 1 - Math.abs(diff);
		}
		return cumulativeSum / keys.size();
	}


	private double calcRatio(TraceFreq traceFreq1, List<String> subtrace) {
		if (traceFreq1.getTraceTotal() == 0)
			return 0;
		return (double)traceFreq1.getFreq(subtrace) / (double)traceFreq1.getTraceTotal();
	}


	public String format() {
		return "TraceRatioMeasure [logEventFreq=" + logEventFreq.format() + ", modelEventFreq=" + modelEventFreq.format() + "]";
	}

	
	
}
