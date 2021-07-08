package qut.pm.setm.evaluation;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;

import qut.pm.setm.TaskStats;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.IdentifiableTask;

public interface SPNQualityCalculator extends IdentifiableTask{

	public String getReadableId();
	public void calculate(PluginContext context, AcceptingStochasticNet net, XLog log, 
			XEventClassifier classifier, TaskStats stats) throws Exception;

}