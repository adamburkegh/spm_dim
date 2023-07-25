
package qut.pm.spm.measures;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.FiniteStochasticLangGenerator;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.PlayoutGenerator;

public class ExistentialPrecisionMeasure 
	extends AbstractStochasticLogCachingMeasure 
{

	private static Logger LOGGER = LogManager.getLogger();
	
	protected TraceFreq logTraceFreq;
	
	private FiniteStochasticLangGenerator traceFreqGen = new FiniteStochasticLangGenerator();
	private ExistentialPrecisionCalculation alphaCalc = new ExistentialPrecisionCalculation();
	private int maxTraceLength = 0;
	private int logLanguageSize = 0;

	public ExistentialPrecisionMeasure(PlayoutGenerator generator) 
	{
		super(generator);
	}


	@Override
	public String getReadableId() {
		return "Existential Precision";
	}

	
	@Override
	public Measure getMeasure() {
		return Measure.EXISTENTIAL_PRECISION;
	}

	@Override
	public String getUniqueId() {
		return "xpr";
	}
	
	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		if (!validateLogCache(log, classifier)) {
			logTraceFreq = traceFreqGen.calculateTraceFreqForLog(log, classifier);
			maxTraceLength  = calculateMaxTraceLength(log);
			logLanguageSize  = calculateLogLanguageSize(log,classifier);
		}
	}

	
	@Override
	protected double calculateForPlayout(TraceFreq tfModel) {
		LOGGER.debug("Computing " + getReadableId() );
		return alphaCalc.calculateUnrestrictedPrecision(logTraceFreq, tfModel, 
				log.getInfo(classifier).getNumberOfEvents(), 0, 
				logLanguageSize, maxTraceLength);
	}

	
	private int calculateMaxTraceLength(XLog log) {
		int max = 0;
		for( XTrace trace: log) {
			if (trace.size() > max)
				max = trace.size();
		}
		return max;
	}


	private int calculateLogLanguageSize(XLog log, XEventClassifier classifier) {
		XLogInfo logInfo = log.getInfo(classifier);
		XEventClasses ec = logInfo.getEventClasses();
		return ec.size();
	}


	
}
