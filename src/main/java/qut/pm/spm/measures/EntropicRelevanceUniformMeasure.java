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
 * Entropic relevance with Uniform background model.
 *  
 * @author burkeat
 *
 */
public class EntropicRelevanceUniformMeasure extends AbstractStochasticLogCachingMeasure{
	
	private RelevanceCalculator relCalc = RelevanceFactory.createRelevanceCalcUniform();
	private FiniteStochasticLangGenerator fslGen = new FiniteStochasticLangGenerator();
	private FiniteStochasticLang logFSL = null; 

	public EntropicRelevanceUniformMeasure(PlayoutGenerator generator) {
		super(generator);
	}
	
	public EntropicRelevanceUniformMeasure() {
		super();
	}
	
	@Override
	public String getUniqueId() {
		return "entreluni";
	}

	@Override
	public String getReadableId() {
		return "Entropic Relevance Uniform";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPIC_RELEVANCE_UNIFORM;
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
