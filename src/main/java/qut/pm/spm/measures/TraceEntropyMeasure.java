package qut.pm.spm.measures;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.MathUtils;
import qut.pm.spm.Measure;
import qut.pm.spm.NumUtils;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.PlayoutGenerator;
import qut.pm.spm.playout.StochasticPlayoutGenerator;

public class TraceEntropyMeasure implements StochasticLogCachingMeasure {

	public static class TraceEntropyMeasurement {
		private double logEntropy;
		private double modelEntropy;
		private double intersectEntropy;
		private double projLogOverModelEntropy;
		private double projModelOverLogEntropy;

		public double getLogEntropy() {
			return logEntropy;
		}
		public double getModelEntropy() {
			return modelEntropy;
		}
		public double getIntersectEntropy() {
			return intersectEntropy;
		}
		public double getEntropyPrecision() {
			return (modelEntropy == 0)? 0: 
				Math.min( intersectEntropy / modelEntropy, 1.0d );
		}

		public double getEntropyFitness() {
			return (logEntropy == 0)? 0: 
				Math.min( intersectEntropy / logEntropy, 1.0d );
		}
		
		public double getEntropyProjectPrecision() {
			double prec = (modelEntropy == 0)? 0: projModelOverLogEntropy / modelEntropy;
			if (prec > 1 + NumUtils.EPSILON)
				LOGGER.error("Super-unity precision: " + prec);
			if (Double.isNaN(prec)) {
				LOGGER.error("NaN precision ... "  + 
							 projModelOverLogEntropy + "/"  + modelEntropy + " = " + prec);
				prec = 0;
			}
			return Math.min( 1, prec);
		}

		public double getEntropyProjectFitness() {
			double fit = (logEntropy == 0)? 0: projLogOverModelEntropy / logEntropy;
			if (fit > 1 + NumUtils.EPSILON)
				LOGGER.error("Super-unity fit: " + fit);
			return Math.min(1,fit);
		}
	}
	
	private static Logger LOGGER = LogManager.getLogger();
	private static final List<String> EMPTY_TRACE = new ArrayList<String>();
	
	protected XLog log;
	protected XEventClassifier classifier;
	protected PlayoutGenerator generator;

	private TraceFreq traceFreqLog;
	
	
	public TraceEntropyMeasure(PlayoutGenerator generator) {
		this.generator = generator;
	}
	
	public TraceEntropyMeasure() {
		this( new StochasticPlayoutGenerator() );
	}
	
	protected boolean validateLogCache(ProvenancedLog log, XEventClassifier classifier) {
		if (this.log == log && this.classifier == classifier)
			return true;
		this.log = log;
		this.classifier = classifier;
		return false;
	}
	
	/**
	 * Can be reused in concurrent context only with same log. 
	 */
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		if (!validateLogCache(log, classifier))
			traceFreqLog = preCalculateForLog(log, classifier);
	}
	
	public void setPrecalculatedLog(TraceFreq log) {
		this.traceFreqLog = log;
	}

	private TraceFreq preCalculateForLog(XLog log, XEventClassifier classifier) {
		TraceFreq result = new TraceFreq();
		for (XTrace trace: log) {
			List<String> transTrace = new LinkedList<String>();
			for (XEvent event: trace) {
				String classId = classifier.getClassIdentity(event);
				transTrace.add(classId);
			}
			result.incTraceFreq(transTrace);
		}
		return result;
	}
	
	@Override
	public String getReadableId() {
		return "Trace Entropy";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPY_TRACEWISE;
	}

	@Override
	public String getUniqueId() {
		return "enttr";
	}
	


	
	protected double calculateForPlayout(TraceFreq traceFreqModel) {
		TraceEntropyMeasurement tem = calculateTraceEntropyMeasures(traceFreqModel);
		return tem.intersectEntropy; 
	}

	public TraceEntropyMeasurement calculateTraceEntropyMeasures(TraceFreq traceFreqModel) {
		TraceEntropyMeasurement tem = new TraceEntropyMeasurement();		
		tem.logEntropy = entropyForTraceFreq(traceFreqLog);
		tem.modelEntropy = entropyForTraceFreq(traceFreqModel);
		TraceFreq intersectTF = intersect(traceFreqLog,traceFreqModel);
		tem.intersectEntropy = entropyForTraceFreq(intersectTF);
		TraceFreq projLogOverModel = project(traceFreqLog,traceFreqModel);
		TraceFreq projModelOverLog = project(traceFreqModel,traceFreqLog);
		tem.projLogOverModelEntropy = entropyForTraceFreq(projLogOverModel);
		tem.projModelOverLogEntropy = entropyForTraceFreq(projModelOverLog);
		return tem;
	}

	private TraceFreq intersect(TraceFreq traceFreq1, TraceFreq traceFreq2) {
		TraceFreq result = new TraceFreq();
		for (List<String> trace: traceFreq1.keySet()) {
			double f1 = traceFreq1.getFreq(trace);
			double f2 = traceFreq2.getFreq(trace);
			double freq = Math.min(f1,f2);
			if (freq > 0)
				result.putFreq(trace,freq);
		}
		return result;
	}

	private static TraceFreq project(TraceFreq traceFreq1, TraceFreq traceFreq2) {
		TraceFreq result = new TraceFreq();
		for (List<String> trace: traceFreq1.keySet()) {
			double f1 = traceFreq1.getFreq(trace);
			double f2 = traceFreq2.getFreq(trace);
			double freq = Math.min(f1,f2);
			if (freq > 0)
				result.putFreq(trace,f1);
		}
		double leftover = traceFreq1.getTraceTotal() - result.getInternalTraceTotal();
		if (leftover > 0)
			result.putFreq(EMPTY_TRACE, leftover);
		return result;
	}
	
	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		TraceFreq playoutLog = generateAndScale(log, net, classifier);
		return calculateForPlayout(playoutLog);
	}

	private TraceFreq generateAndScale(ProvenancedLog  log, AcceptingStochasticNet net, XEventClassifier classifier) {
		LOGGER.debug("Calculating ...");
		precalculateForLog(log,classifier);
		int traceCt = log.size();
		TraceFreq playoutLog = generator.buildPlayoutTraceFreq(net);
		playoutLog = generator.scaleTo(playoutLog,traceCt);
		return playoutLog;
	}
	
	public TraceEntropyMeasurement calculateTraceEntropyMeasure(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		TraceFreq playoutLog = generateAndScale(log, net, classifier);
		return calculateTraceEntropyMeasures(playoutLog);
	}
	
	
	public static double entropyForTraceFreq(TraceFreq traceFreq) {
		double entropy = 0;
		if (traceFreq.getTraceTotal() == 0)
			return 0.0d;
		for (List<String> trace: traceFreq.keySet()) {
			double prob = (double)traceFreq.getFreq(trace)/(double)traceFreq.getTraceTotal();
			if (prob <= 0.0) // convention that log 0 == 0
				continue;
			entropy += prob * MathUtils.log2(prob);
		}
		return -1.0d * entropy;
	}

}
