package qut.pm.spm.measures;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.PlayoutGenerator;
import qut.pm.spm.playout.StochasticPlayoutGenerator;


public abstract class AbstractStochasticLogCachingMeasure implements StochasticLogCachingMeasure {

	private static Logger LOGGER = LogManager.getLogger();
	
	protected volatile ProvenancedLog log;
	protected XEventClassifier classifier;
	
	protected PlayoutGenerator generator;
	protected TraceFreq playoutLog; // note concurrency risk

	public AbstractStochasticLogCachingMeasure(PlayoutGenerator generator) {
		this.generator = generator;
	}

	public AbstractStochasticLogCachingMeasure() {
		this.generator = new StochasticPlayoutGenerator();
	}
	
	/**
	 * 
	 * @param log
	 * @param classifier
	 * @return whether cached value was used
	 */
	protected boolean validateLogCache(ProvenancedLog log, XEventClassifier classifier) {
		if (this.log == log && this.classifier == classifier)
			return true;
		this.log = log;
		this.classifier = classifier;
		return false;
	}

	public double calculate(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		LOGGER.debug("Calculating ...");
		precalculateForLog(log,classifier);
		int traceCt = log.size();
		playoutLog = generator.buildPlayoutTraceFreq(net);
		playoutLog = generator.scaleTo(playoutLog,traceCt);
		return calculateForPlayout(playoutLog);
	}

	protected abstract double calculateForPlayout(TraceFreq playoutLog);
}
