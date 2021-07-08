package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.Measure;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeCheck;

public class ProbProcessTreeDeterminismMeasure implements ProbProcessTreeMeasure {

	
	@Override
	public String getReadableId() {
		return "Prob Process Tree Determinism";
	}
	
	@Override
	public String getUniqueId() {
		return "pptbeta";
	}

	public Measure getMeasure() {
		return Measure.PROB_PROCESS_TREE_DETERMINISM;
	}

	@Override
	public double calculate(XLog log, ProbProcessTree ppt, XEventClassifier classifier) {
		return ProbProcessTreeCheck.deterministic(ppt)? 1.0: 0.0;
	}

	
}
