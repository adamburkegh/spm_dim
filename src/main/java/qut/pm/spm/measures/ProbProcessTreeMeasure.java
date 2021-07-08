package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.IdentifiableTask;
import qut.pm.spm.Measure;
import qut.pm.spm.ppt.ProbProcessTree;

public interface ProbProcessTreeMeasure extends IdentifiableTask{

	public String getReadableId();
	public Measure getMeasure();
	public double calculate(XLog log, ProbProcessTree ppt, XEventClassifier classifier);
	
}
