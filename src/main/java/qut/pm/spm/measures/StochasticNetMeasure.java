package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.IdentifiableTask;
import qut.pm.spm.Measure;

public interface StochasticNetMeasure extends IdentifiableTask{

	public String getReadableId();
	public Measure getMeasure();
	public double calculate(XLog log, AcceptingStochasticNet net, XEventClassifier classifier);
	
}
