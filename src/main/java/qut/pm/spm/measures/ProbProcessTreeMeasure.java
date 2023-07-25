package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.IdentifiableTask;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.ppt.ProbProcessTree;

public interface ProbProcessTreeMeasure extends IdentifiableTask{

	public String getReadableId();
	public Measure getMeasure();
	public double calculate(ProvenancedLog log, ProbProcessTree ppt, XEventClassifier classifier);
	
}
