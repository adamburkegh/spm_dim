package qut.pm.setm.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;
import org.processmining.earthmoversstochasticconformancechecking.parameters.EMSCParametersLogModelDefault;
import org.processmining.earthmoversstochasticconformancechecking.parameters.LanguageGenerationStrategyFromModelAbstract;
import org.processmining.earthmoversstochasticconformancechecking.parameters.LanguageGenerationStrategyFromModelDefault;
import org.processmining.earthmoversstochasticconformancechecking.plugins.EarthMoversStochasticConformancePlugin;
import org.processmining.earthmoversstochasticconformancechecking.tracealignments.StochasticTraceAlignmentsLogModel;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.ProMCanceller;

import qut.pm.setm.TaskStats;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

public class EarthMoversCalculator implements SPNQualityCalculator {

	private static Logger LOGGER = LogManager.getLogger();

	@Override
	public String getReadableId() {
		return "Earth Movers Similarity";
	}

	@Override
	public String getUniqueId() {
		return "ems";
	}

	
	@Override
	public void calculate(PluginContext context, AcceptingStochasticNet net, ProvenancedLog log, 
			XEventClassifier classifier, TaskStats stats) throws Exception 
	{
		LOGGER.info("Computing earth-movers' distance (SL) ");
		EMSCParametersLogModelDefault parameters = new EMSCParametersLogModelDefault();
		parameters.setComputeStochasticTraceAlignments(false);
		parameters.setDebug(false);
		parameters.setLogClassifier(classifier);
		LanguageGenerationStrategyFromModelAbstract terminationStrategy = new LanguageGenerationStrategyFromModelDefault();
		parameters.setModelTerminationStrategy(terminationStrategy);
		
		LOGGER.debug("Initial marking {}",net.getInitialMarking());
		StochasticTraceAlignmentsLogModel stAlign = 
					EarthMoversStochasticConformancePlugin.measureLogModel(log, net.getNet(),
							net.getInitialMarking(), parameters, new ProMCanceller() {
								public boolean isCancelled() {
									return context.getProgress().isCancelled();
								}
							});
		stats.setMeasure(Measure.EARTH_MOVERS_SIMILARITY, stAlign.getSimilarity()); 
	}

}
