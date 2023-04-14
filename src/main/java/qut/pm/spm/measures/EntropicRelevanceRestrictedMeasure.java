package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.FiniteStochasticLangGenerator;
import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.measures.relevance.RelevanceCalculator;
import qut.pm.spm.measures.relevance.RelevanceFactory;
import qut.pm.spm.playout.PlayoutGenerator;

/**
 * Entropic relevance with Restricted Zero Order background model.
 *  
 * @author burkeat
 *
 */
public class EntropicRelevanceRestrictedMeasure extends AbstractStochasticLogCachingMeasure{
	
	private RelevanceCalculator relCalc = RelevanceFactory.createRelevanceCalcRestrictedZeroOrder();
	private FiniteStochasticLangGenerator fslGen = new FiniteStochasticLangGenerator();
	private FiniteStochasticLang logFSL = null; 

	public EntropicRelevanceRestrictedMeasure(PlayoutGenerator generator) {
		super(generator);
	}
	
	public EntropicRelevanceRestrictedMeasure() {
		super();
	}
	
	@Override
	public String getUniqueId() {
		return "entrelrz";
	}

	@Override
	public String getReadableId() {
		return "Entropic Relevance Restricted Zero Order";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPIC_RELEVANCE_RESTRICTED_ZO;
	}


	@Override
	protected double calculateForPlayout(TraceFreq playoutLog) {
		return relCalc.relevance(logFSL, playoutLog);
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		if (!validateLogCache(log, classifier)) {
			logFSL = fslGen.calculateForLog(log, classifier);
		}
	}

}
