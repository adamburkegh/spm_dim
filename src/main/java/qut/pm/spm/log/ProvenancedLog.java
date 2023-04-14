package qut.pm.spm.log;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XLog;

public interface ProvenancedLog extends XLog{

	public String getLogFilePath();
	public int size();
	public XLogInfo getInfo(XEventClassifier classifier);
	
}
