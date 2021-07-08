package qut.pm.setm.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.earthmoversstochasticconformancechecking.parameters.EMSCParametersDefault;
import org.processmining.earthmoversstochasticconformancechecking.parameters.EMSCParametersLogModel;
import org.processmining.earthmoversstochasticconformancechecking.parameters.EMSCParametersLogModelAbstract;
import org.processmining.earthmoversstochasticconformancechecking.parameters.TerminationStrategyImpl;
import org.processmining.earthmoversstochasticconformancechecking.plugins.EarthMoversStochasticConformancePlugin;
import org.processmining.earthmoversstochasticconformancechecking.tracealignments.StochasticTraceAlignmentsLogModel;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.ProMCanceller;

import qut.pm.setm.TaskStats;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;

public class EarthMoversTunedCalculator implements SPNQualityCalculator {
	
	private static final double MASS_COVERAGE = 0.80;
	
	private class EMSCParametersLogModelTuned extends EMSCParametersLogModelAbstract {

		public EMSCParametersLogModelTuned(XEventClassifier classifier) {
			super(EMSCParametersDefault.defaultDistanceMatrix, 
					classifier,
					new TerminationStrategyImpl(1000 * 60 * 20, MASS_COVERAGE), 
					EMSCParametersDefault.defaultDebug, 
					false);
		}

	}
	
	private static Logger LOGGER = LogManager.getLogger();

	@Override
	public String getReadableId() {
		return "Earth Movers Similarity Tuned";
	}

	@Override
	public String getUniqueId() {
		return "emst";
	}

	
	@Override
	public void calculate(PluginContext context, AcceptingStochasticNet net, XLog log, 
			XEventClassifier classifier, TaskStats stats) throws Exception 
	{
		LOGGER.info("Computing earth-movers' distance (SL) with mass coverage: " + MASS_COVERAGE);
		EMSCParametersLogModel parameters = new EMSCParametersLogModelTuned(classifier);
		LOGGER.debug("Initial marking {}",net.getInitialMarking());
		StochasticTraceAlignmentsLogModel stAlign = 
				EarthMoversStochasticConformancePlugin.measureLogModel(log, net.getNet(),
					net.getInitialMarking(), parameters, new ProMCanceller() {
						public boolean isCancelled() {
							return context.getProgress().isCancelled();
						}
					});
		stats.setMeasure(Measure.EARTH_MOVERS_LIGHT_COVERAGE, stAlign.getSimilarity()); 
	}

}
