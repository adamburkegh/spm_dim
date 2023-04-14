package qut.pm.setm;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.measures.StochasticLogCachingMeasure;

public class RunCalculator {

	private static Logger LOGGER = LogManager.getLogger();
	
	public Collection<StochasticLogCachingMeasure> netMeasures;
	private RunStatsExporter runStatsExporter;
	private XEventClassifier classifier;

	private ProvenancedLog log;
	
	public RunCalculator(Collection<StochasticLogCachingMeasure> netMeasures,
			RunStatsExporter runStatsExporter,
			ProvenancedLog log,
			XEventClassifier classifier) 
	{
		this.netMeasures = netMeasures;
		this.runStatsExporter = runStatsExporter;
		this.log = log;
		this.classifier = classifier;
	}

	public Collection<StochasticLogCachingMeasure> getNetMeasures(){
		return netMeasures;
	}
	
	public void calculateMeasures(RunStats modelStats, AcceptingStochasticNet snet)
					throws Exception 
	{
		justCalculateMeasures(modelStats, snet);
		runStatsExporter.exportRun(modelStats);
	}

	public void justCalculateMeasures(RunStats modelStats, AcceptingStochasticNet snet) throws Exception {
		for (StochasticLogCachingMeasure measure : getNetMeasures()) {
			calculateAndRecordMeasure(snet, measure, modelStats);
		}
	}

	public void calculateAndRecordMeasure(AcceptingStochasticNet model, StochasticLogCachingMeasure measure,
			RunStats runStats) throws Exception 
	{
		TaskStats task = runStatsExporter.makeNewTask(runStats, "calculate " + measure.getReadableId());
		if (measure.isFlaky())
			runStatsExporter.exportRun(runStats);
		double calc = measure.calculate(log, model, classifier);
		LOGGER.info("Calculated " + measure.getReadableId() + " == " + calc);
		task.setMeasure(measure.getMeasure(), calc);
		task.markEnd();
	}

	public RunStatsExporter getRunStatsExporter() {
		return runStatsExporter;
	}

	
}
