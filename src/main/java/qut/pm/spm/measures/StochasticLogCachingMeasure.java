package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.log.ProvenancedLog;

public interface StochasticLogCachingMeasure extends StochasticNetMeasure{

	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) ;
	public default boolean isFlaky() {
		return false;
	}
	public default String format() {
		return toString();
	}
		
}
