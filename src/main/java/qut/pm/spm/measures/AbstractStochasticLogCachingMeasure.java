package qut.pm.spm.measures;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.playout.PlayoutGenerator;
import qut.pm.spm.playout.StochasticPlayoutGenerator;


public abstract class AbstractStochasticLogCachingMeasure implements StochasticLogCachingMeasure {

	private static Logger LOGGER = LogManager.getLogger();
	
	protected XLog log;
	protected XEventClassifier classifier;
	
	protected PlayoutGenerator generator;
	protected XLog playoutLog;

	public AbstractStochasticLogCachingMeasure(PlayoutGenerator generator) {
		this.generator = generator;
	}

	public AbstractStochasticLogCachingMeasure() {
		this.generator = new StochasticPlayoutGenerator();
	}
	
	protected void validateLogCache(XLog log, XEventClassifier classifier) {
		if (this.log == log && this.classifier == classifier)
			return;
		this.log = log;
		this.classifier = classifier;
	}

	public double calculate(XLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		LOGGER.debug("Calculating ...");
		precalculateForLog(log,classifier);
		int traceCt = log.size();
		playoutLog = generator.buildPlayoutLog(net,traceCt);
		return calculateForPlayout(playoutLog,classifier);
	}

	protected abstract double calculateForPlayout(XLog playoutLog, XEventClassifier classifier);


}
