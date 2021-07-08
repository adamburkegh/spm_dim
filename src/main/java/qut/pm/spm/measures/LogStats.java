package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

public class LogStats {

	protected XLog log;
	protected XEventClassifier classifier;
	protected int uniqueTraceCount = 0;

	public LogStats(XLog log, XEventClassifier classifier, int uniqueTraceCount) {
		super();
		this.log = log;
		this.classifier = classifier;
		this.uniqueTraceCount = uniqueTraceCount;
	}

	public int getUniqueTraceCount() {
		return uniqueTraceCount;
	}
	
	public XLog getLog() {
		return log;
	}
}
