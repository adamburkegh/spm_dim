// Copy-paste fork from Toothpaste project
// https://github.com/adamburkegh/toothpaste
// qut.pm.spm.conformance.AlphaPrecisionCalculator
// Feb 1 2023

package qut.pm.spm.measures;

import java.util.HashSet;
import java.util.Set;

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

public class AlphaPrecisionRestrictedMeasure extends AbstractStochasticLogCachingMeasure {
	
	private final int alphaSignificanceParam;
	private final double alphaSignificance;
	private static Logger LOGGER = LogManager.getLogger();
	
	protected TraceFreq logTraceFreq;
	
	private FiniteStochasticLangGenerator traceFreqGen = new FiniteStochasticLangGenerator();
	private AlphaPrecisionCalculation alphaCalc = new AlphaPrecisionCalculation();
	private int maxTraceLength;
	private Set<String> activities;

	public AlphaPrecisionRestrictedMeasure(PlayoutGenerator generator,
										   int alphaSignificanceParam) 
	{
		super(generator);
		this.alphaSignificanceParam = alphaSignificanceParam;
		this.alphaSignificance = alphaSignificanceParam*0.01;
	}

	@Override
	public String getReadableId() {
		return "Alpha Precision Restricted alpha-sig " + alphaSignificance;
	}

	
	@Override
	public Measure getMeasure() {
		switch(alphaSignificanceParam) {
		case 1:
			return Measure.ALPHA_PRECISION_RESTRICTED_1_PCT;
		case 2: 
			return Measure.ALPHA_PRECISION_RESTRICTED_2_PCT;
		case 5:
			return Measure.ALPHA_PRECISION_RESTRICTED_5_PCT;
		}
		throw new RuntimeException("No defined measure for alpha significance=" 
									+ alphaSignificanceParam);
	}


	@Override
	public String getUniqueId() {
		return "apr" + alphaSignificance;
	}
	
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		if (!validateLogCache(log, classifier)) {
			logTraceFreq = traceFreqGen.calculateTraceFreqForLog(log, classifier);
			maxTraceLength  = calculateMaxTraceLength(log);
			activities = calculateActivities(log,classifier);
		}

	}

	
	
	
	@Override
	protected double calculateForPlayout(TraceFreq tfModel) {
		LOGGER.debug("Computing " + getReadableId() );
		return alphaCalc.calculateRestrictedPrecision(logTraceFreq, tfModel, 
				activities, log.getInfo(classifier).getNumberOfEvents(), 
				alphaSignificance, maxTraceLength);
	}

	
	private Set<String> calculateActivities(XLog log, XEventClassifier classifier) {
		XLogInfo logInfo = log.getInfo(classifier);
		XEventClasses ec = logInfo.getEventClasses();
		Set<String> activities = new HashSet<String>();
		for (int i=0; i<ec.size(); i++) {
			activities.add(ec.getByIndex(i).getId());
		}
		return activities;
	}

	private int calculateMaxTraceLength(XLog log) {
		int max = 0;
		for( XTrace trace: log) {
			if (trace.size() > max)
				max = trace.size();
		}
		return max;
	}


	
}
