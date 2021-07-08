package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

public interface StochasticLogCachingMeasure extends StochasticNetMeasure{

	public void precalculateForLog(XLog log, XEventClassifier classifier) ;
	public default boolean isFlaky() {
		return false;
	}
		
}
