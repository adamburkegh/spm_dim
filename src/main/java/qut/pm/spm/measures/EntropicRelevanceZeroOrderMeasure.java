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
 * Entropic relevance with Zero Order background model.
 *  
 * @author burkeat
 *
 */
public class EntropicRelevanceZeroOrderMeasure extends AbstractStochasticLogCachingMeasure{
	
	private RelevanceCalculator relCalc = RelevanceFactory.createRelevanceCalcZeroOrder();
	private FiniteStochasticLangGenerator fslGen = new FiniteStochasticLangGenerator();
	private FiniteStochasticLang logFSL = null; 

	public EntropicRelevanceZeroOrderMeasure(PlayoutGenerator generator) {
		super(generator);
	}
	
	public EntropicRelevanceZeroOrderMeasure() {
		super();
	}
	
	@Override
	public String getUniqueId() {
		return "entrelzo";
	}

	@Override
	public String getReadableId() {
		return "Entropic Relevance Zero Order";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPIC_RELEVANCE_ZERO_ORDER;
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
