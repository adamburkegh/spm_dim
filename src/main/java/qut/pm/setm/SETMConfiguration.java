package qut.pm.setm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.watchmaker.GenerationCountVisible;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

import qut.pm.setm.factory.BasicPPTEvolutionFactory;
import qut.pm.setm.fitness.metrics.BasicOverallFitness;
import qut.pm.setm.fitness.metrics.MeasureEvaluatorFactory;
import qut.pm.setm.mutation.TreeCrossover;
import qut.pm.setm.mutation.TreeMutationAbstract;
import qut.pm.setm.mutation.TreeMutationCoordinator;
import qut.pm.setm.mutation.mutators.AddNodeRandom;
import qut.pm.setm.mutation.mutators.MutateSingleNodeRandom;
import qut.pm.setm.mutation.mutators.RemoveSubtreeRandom;
import qut.pm.setm.mutation.mutators.RemoveUselessNodes;
import qut.pm.setm.observer.ExportObserver;
import qut.pm.setm.parameters.SETMParam;
import qut.pm.spm.measures.EarthMoversTraceMeasure;
import qut.pm.spm.measures.EventRatioMeasure;
import qut.pm.spm.measures.GenTraceDiffMeasure;
import qut.pm.spm.measures.GenTraceFloorMeasure;
import qut.pm.spm.measures.LogStatsCache;
import qut.pm.spm.measures.SimplicityEdgeCountMeasure;
import qut.pm.spm.measures.SimplicityEntityCountMeasure;
import qut.pm.spm.measures.SimplicityStructuralStochasticUniqMeasure;
import qut.pm.spm.measures.StochasticLogCachingMeasure;
import qut.pm.spm.measures.TraceEntropyFitness;
import qut.pm.spm.measures.TraceEntropyMeasure;
import qut.pm.spm.measures.TraceEntropyPrecision;
import qut.pm.spm.measures.TraceEntropyProjectFitness;
import qut.pm.spm.measures.TraceEntropyProjectPrecision;
import qut.pm.spm.measures.TraceOverlapRatioMeasure;
import qut.pm.spm.measures.TraceProbabilityMassOverlap;
import qut.pm.spm.measures.TraceRatioMeasure;
import qut.pm.spm.playout.CachingPlayoutGenerator;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.util.ClockUtil;

public class SETMConfiguration {
	
	private static Logger LOGGER = LogManager.getLogger();
	public static final String CONFIG_RANDOM_MODEL_STARTAT 		= "mr.random.model.startat";
	public static final String CONFIG_RANDOM_MAX_TRANSITIONS 	= "mr.random.max.transitions";
	public static final String CONFIG_RANDOM_TARGET_SIZE 		= "mr.random.target.size";
	public static final String CONFIG_RANDOM_MAX_DEPTH 			= "mr.random.max.depth";
	public static final String CONFIG_RANDOM_SEED 				= "mr.random.seed";
	public static final String CONFIG_RANDOM_STORE_MODELS 		= "mr.random.store.models";
	public static final String CONFIG_MODEL_INPUT_FILE 			= "mr.model.input.file";
	public static final String CONFIG_MODEL_INPUT_FILE_PATTERN	= "mr.model.input.file.pattern";
	public static final String CONFIG_MODEL_DIR 				= "mr.model.dir";
	public static final String CONFIG_LOG_FILE 					= "mr.log.file";
	public static final String CONFIG_OUTPUT_DIR 				= "mr.output.dir";
	public static final String CONFIG_DATA_DIR 					= "mr.data.dir";
	public static final String CONFIG_RUN_TYPE 					= "mr.run.type";
	public static final String CONFIG_SETM_MAX_GEN				= "mr.setm.max.gen";
	public static final String CONFIG_SETM_POPULATION_SIZE		= "mr.setm.population.size";
	public static final String CONFIG_SETM_ELITE_SIZE			= "mr.setm.elite.size";
	public static final String CONFIG_SETM_CROSSOVER_CHANCE		= "mr.setm.crossover.chance";
	public static final String CONFIG_SETM_TARGET_FITNESS		= "mr.setm.target.fitness";		
	public static final String CONFIG_CALCULATORS				= "mr.calculators";
	public static final String CONFIG_PLAYOUT_GRANULARITY 				= "mr.playout.granularity";


	public SETMConfigParams configureFromProperties(Properties cfg) {
		SETMConfigParams result = new SETMConfigParams();
		result.maxGen = Integer.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_SETM_MAX_GEN,"1000"));
		result.popSize = Integer.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_SETM_POPULATION_SIZE,"20"));
		result.eliteSize = Integer.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_SETM_ELITE_SIZE,"-1"));
		if (result.eliteSize == -1)
			result.eliteSize = (int) (result.popSize * .3);
		result.crossOverChance = Double.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_SETM_CROSSOVER_CHANCE,"0.1"));
		result.targetFitness = Double.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_SETM_TARGET_FITNESS,"1.0"));
		result.playoutGranularity = Long.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_PLAYOUT_GRANULARITY,"1000") );
		String seedCfg = cfg.getProperty(SETMConfiguration.CONFIG_RANDOM_SEED,"");
		if (!seedCfg.equals(""))
			result.seed = Integer.valueOf(seedCfg);
		else
			result.seed = ClockUtil.nanoTime();
		return result;
	}
	
	protected List<String> initActivities(SETMConfigParams cp, XLog log) {
		XLogInfo logInfo = log.getInfo(cp.classifier.getEventClassifier());
		XEventClasses eventClasses = logInfo.getEventClasses();
		List<String> activities = new ArrayList<String>();
		for (int i=0; i<eventClasses.size(); i++) {
			activities.add( eventClasses.getByIndex(i).getId());
		}
		ProbProcessTreeFactory.initActivityRegistry( activities.toArray(new String[] {}) );
		LOGGER.info("Using activities:" + activities);
		return activities;
	}

	protected void addMeasure(XLog log, SETMConfigParams cp, LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> alg, 
							 MeasureEvaluatorFactory mef, StochasticLogCachingMeasure measure) 
	{
		FitnessEvaluator<ProbProcessTree> newEval = 
				mef.createEvaluator(measure, log, cp.classifier.getEventClassifier());
		alg.put( newEval, 1.0 );
	}

	protected LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> initMeasures(XLog log, 
			SETMConfigParams cp, ExportObserver exportObserver) 
	{
		LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> alg = new LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double>();
		
		MeasureEvaluatorFactory mef = new MeasureEvaluatorFactory(exportObserver);
		CachingPlayoutGenerator generator = new CachingPlayoutGenerator( cp.playoutGranularity );
		addMeasure(log, cp, alg, mef, new EventRatioMeasure(generator));
		addMeasure(log, cp, alg, mef, new TraceRatioMeasure(generator,2));
		addMeasure(log, cp, alg, mef, new TraceRatioMeasure(generator,3));
		addMeasure(log, cp, alg, mef, new TraceRatioMeasure(generator,4));
		addMeasure(log, cp, alg, mef, new EarthMoversTraceMeasure(generator));
		TraceEntropyMeasure tem = new TraceEntropyMeasure(generator);
		addMeasure(log, cp, alg, mef, new TraceEntropyPrecision(tem));
		addMeasure(log, cp, alg, mef, new TraceEntropyFitness(tem));
		addMeasure(log, cp, alg, mef, new TraceEntropyProjectFitness(tem));
		addMeasure(log, cp, alg, mef, new TraceEntropyProjectPrecision(tem));
		addMeasure(log, cp, alg, mef, new TraceOverlapRatioMeasure(generator));
		addMeasure(log, cp, alg, mef, new TraceProbabilityMassOverlap(generator));
		addMeasure(log, cp, alg, mef, new GenTraceFloorMeasure(generator,1));
		addMeasure(log, cp, alg, mef, new GenTraceFloorMeasure(generator,5));
		addMeasure(log, cp, alg, mef, new GenTraceFloorMeasure(generator,10));
		addMeasure(log, cp, alg, mef, new GenTraceDiffMeasure(generator));
		LogStatsCache statsCache = new LogStatsCache();
		addMeasure(log, cp, alg, mef, new SimplicityEdgeCountMeasure(statsCache));
		addMeasure(log, cp, alg, mef, new SimplicityEntityCountMeasure(statsCache));
		addMeasure(log, cp, alg, mef, new SimplicityStructuralStochasticUniqMeasure(statsCache));
		return alg;
	}

	public SETMParam buildETMParam(String loggingPath, XLog log, SETMConfigParams cp, ExportObserver exportObserver )
	{
		CentralRegistry registry;
		registry = new CentralRegistry(log, cp.classifier.getEventClassifier(), new Random(cp.seed)); 

		LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> alg = initMeasures(log, cp, exportObserver);
		
		BasicOverallFitness of = new BasicOverallFitness(alg);
		FitnessEvaluator<ProbProcessTree> evaluator = of;
	
		//Make multi threaded......
	//		evaluator = new MultiThreadedFitnessEvaluator(registry, evaluator, Math.max(Runtime.getRuntime()
	//				.availableProcessors() / 2, 1));
	//
		//Evolutionary Operators
		ArrayList<EvolutionaryOperator<ProbProcessTree>> evolutionaryOperators = new ArrayList<EvolutionaryOperator<ProbProcessTree>>();
	
		LinkedHashMap<TreeMutationAbstract, Double> dumbMutators = new LinkedHashMap<TreeMutationAbstract, Double>();
		dumbMutators.put(new AddNodeRandom(registry), 1d);
		dumbMutators.put(new MutateSingleNodeRandom(registry), 1d);
		dumbMutators.put(new RemoveSubtreeRandom(registry), 1d );
		dumbMutators.put(new RemoveUselessNodes(registry), 1d );
		TreeMutationCoordinator dumbCoordinator = new TreeMutationCoordinator(dumbMutators, cp.preventDuplicates);
	
		//Dumb only
		evolutionaryOperators.add(new TreeCrossover<ProbProcessTree>(1, new Probability(cp.crossOverChance),registry));
	
		evolutionaryOperators.add(dumbCoordinator);
	
		List<String> activities = initActivities(cp, log);
		BasicPPTEvolutionFactory basicFactory = 
				new BasicPPTEvolutionFactory(activities);
	
		SETMParam etmParam = new SETMParam(registry, evaluator, evolutionaryOperators, cp.popSize, cp.eliteSize);
	
		etmParam.setFactory(basicFactory);
		etmParam.addTerminationCondition(new GenerationCountVisible(cp.maxGen));
		etmParam.addTerminationConditionTargetFitness(cp.targetFitness, evaluator.isNatural());
		etmParam.setPath(loggingPath);
	
	 	etmParam.setRunId(cp.runId);
	 	etmParam.setBuildVersion(cp.buildVersion);
	 	etmParam.setLogFileName(cp.logFileName);
		etmParam.addEvolutionObserver(exportObserver);
		return etmParam;
	}

}
