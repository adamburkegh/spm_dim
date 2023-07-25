package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.IdentifiableTask;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

public interface StochasticNetMeasure extends IdentifiableTask{

	public String getReadableId();
	public Measure getMeasure();
	public double calculate(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier);
	
}
