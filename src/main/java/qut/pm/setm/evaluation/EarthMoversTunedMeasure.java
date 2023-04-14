package qut.pm.setm.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;
import org.processmining.earthmoversstochasticconformancechecking.parameters.EMSCParametersLogModelDefault;
import org.processmining.earthmoversstochasticconformancechecking.parameters.LanguageGenerationStrategyFromModelAbstract;
import org.processmining.earthmoversstochasticconformancechecking.parameters.LanguageGenerationStrategyFromModelDefault;
import org.processmining.earthmoversstochasticconformancechecking.plugins.EarthMoversStochasticConformancePlugin;
import org.processmining.earthmoversstochasticconformancechecking.tracealignments.StochasticTraceAlignmentsLogModel;
import org.processmining.framework.plugin.ProMCanceller;

import qut.pm.prom.helpers.ConsoleUIPluginContext;
import qut.pm.prom.helpers.HeadlessDefinitelyNotUIPluginContext;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.measures.StochasticLogCachingMeasure;

public class EarthMoversTunedMeasure implements StochasticLogCachingMeasure{
	
	private static final double MASS_COVERAGE = 0.80;
	
	
	private static Logger LOGGER = LogManager.getLogger();


	private HeadlessDefinitelyNotUIPluginContext pluginContext;

	@Override
	public String getReadableId() {
		return "Earth Movers Similarity Tuned";
	}

	@Override
	public String getUniqueId() {
		return "emst";
	}

	
	@Override
	public Measure getMeasure() {
		return Measure.EARTH_MOVERS_LIGHT_COVERAGE;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		LOGGER.info("Computing earth-movers' distance (SL) with mass coverage: " + MASS_COVERAGE);
		EMSCParametersLogModelDefault parameters = new EMSCParametersLogModelDefault();
		parameters.setComputeStochasticTraceAlignments(false);
		parameters.setDebug(false);
		parameters.setLogClassifier(classifier);
		LanguageGenerationStrategyFromModelAbstract terminationStrategy = new LanguageGenerationStrategyFromModelDefault();
		terminationStrategy.setMaxMassCovered(MASS_COVERAGE);
		parameters.setModelTerminationStrategy(terminationStrategy);
		LOGGER.debug("Initial marking {}",net.getInitialMarking());
		try {
			StochasticTraceAlignmentsLogModel stAlign = 
					EarthMoversStochasticConformancePlugin.measureLogModel(log, net.getNet(),
						net.getInitialMarking(), parameters, new ProMCanceller() {
							public boolean isCancelled() {
								return pluginContext.getProgress().isCancelled();
							}
						});
			return stAlign.getSimilarity();
		}catch(Exception e) {
			LOGGER.warn(e);
			return 0;
		}
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		// deliberately almost no-op
		pluginContext = new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(), "earthmoverstunedmeasure");
	}

}
