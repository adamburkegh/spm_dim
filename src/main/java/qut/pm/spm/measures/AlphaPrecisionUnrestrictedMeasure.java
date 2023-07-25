// Copy-paste fork from Toothpaste project
// https://github.com/adamburkegh/toothpaste
// qut.pm.spm.conformance.AlphaPrecisionCalculator
// Feb 1 2023

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

public class AlphaPrecisionUnrestrictedMeasure 
	extends AbstractStochasticLogCachingMeasure 
{
	
	
	private final int alphaSignificanceParam;
	private final double alphaSignificance;
	private static Logger LOGGER = LogManager.getLogger();
	
	protected TraceFreq logTraceFreq;
	
	private FiniteStochasticLangGenerator traceFreqGen = new FiniteStochasticLangGenerator();
	private AlphaPrecisionCalculation alphaCalc = new AlphaPrecisionCalculation();
	private int maxTraceLength = 0;
	private int logLanguageSize = 0;

	public AlphaPrecisionUnrestrictedMeasure(PlayoutGenerator generator, 
											 int alphaSignificanceParam) 
	{
		super(generator);
		this.alphaSignificanceParam = alphaSignificanceParam;
		this.alphaSignificance = alphaSignificanceParam*0.01;
	}


	@Override
	public String getReadableId() {
		return "Alpha Precision Unrestricted alpha-sig " + alphaSignificance;
	}

	
	@Override
	public Measure getMeasure() {
		switch(alphaSignificanceParam) {
		case 0:
			return Measure.ALPHA_PRECISION_UNRESTRICTED_ZERO;
		case 1:
			return Measure.ALPHA_PRECISION_UNRESTRICTED_1_PCT;
		case 2: 
			return Measure.ALPHA_PRECISION_UNRESTRICTED_2_PCT;
		case 5:
			return Measure.ALPHA_PRECISION_UNRESTRICTED_5_PCT;
		}
		throw new RuntimeException("No defined measure for alpha significance=" 
									+ alphaSignificanceParam);
	}

	@Override
	public String getUniqueId() {
		return "apur" + alphaSignificance;
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
				log.getInfo(classifier).getNumberOfEvents(), alphaSignificance, 
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
