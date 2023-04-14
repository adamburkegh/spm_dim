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
 * Entropic relevance measure - normed so higher is better - with brute scaling 
 * by a constant.
 *  
 * @author burkeat
 *
 */
public class EntropicRelevanceZeroOrderMeasureBruteScale extends AbstractStochasticLogCachingMeasure{
	
	private static final double BRUTE_CONSTANT = 60.0d;
	private RelevanceCalculator relCalc = RelevanceFactory.createRelevanceCalcZeroOrder();
	private FiniteStochasticLangGenerator fslGen = new FiniteStochasticLangGenerator();
	private FiniteStochasticLang logFSL = null; 

	public EntropicRelevanceZeroOrderMeasureBruteScale(PlayoutGenerator generator) {
		super(generator);
	}
	
	public EntropicRelevanceZeroOrderMeasureBruteScale() {
		super();
	}
	
	@Override
	public String getUniqueId() {
		return "entrelunibrute";
	}

	@Override
	public String getReadableId() {
		return "Entropic Relevance Uniform Brute Norm";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPIC_RELEVANCE_ZERO_ORDER_BRUTE;
	}


	@Override
	protected double calculateForPlayout(TraceFreq playoutLog) {
		double screl = Math.min( relCalc.relevance(logFSL, playoutLog) / BRUTE_CONSTANT, 1.0 );
		return 1- Math.max(screl,0.0);
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		if (!validateLogCache(log, classifier)) {
			logFSL = fslGen.calculateForLog(log, classifier);
		}
	}

}
