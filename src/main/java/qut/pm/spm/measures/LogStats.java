package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

public class LogStats {

	protected XLog log;
	protected XEventClassifier classifier;
	protected int uniqueTraceCount = 0;
	private double avgTraceLength = 0;

	public LogStats(XLog log, XEventClassifier classifier, int uniqueTraceCount, 
			double avgTraceLength) 
	{
		super();
		this.log = log;
		this.classifier = classifier;
		this.uniqueTraceCount = uniqueTraceCount;
		this.avgTraceLength  = avgTraceLength;
	}

	public int getUniqueTraceCount() {
		return uniqueTraceCount;
	}
	
	public double getAvgTraceLength() {
		return avgTraceLength;
	}
	
	public XLog getLog() {
		return log;
	}
}
