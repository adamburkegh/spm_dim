package qut.pm.setm;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.exporting.StochasticNetToPNMLConverter;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.processmining.xeslite.plugin.OpenLogFileLiteImplPlugin;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import qut.pm.prom.helpers.ConsoleUIPluginContext;
import qut.pm.prom.helpers.HeadlessDefinitelyNotUIPluginContext;
import qut.pm.setm.evaluation.SPNQualityCalculator;
import qut.pm.setm.observer.ExportObserver;
import qut.pm.setm.parameters.SETMParam;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.AcceptingStochasticNetImpl;
import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.measures.EarthMoversTraceMeasure;
import qut.pm.spm.measures.EdgeCount;
import qut.pm.spm.measures.EntityCount;
import qut.pm.spm.measures.EventRatioMeasure;
import qut.pm.spm.measures.GenTraceDiffMeasure;
import qut.pm.spm.measures.GenTraceFloorMeasure;
import qut.pm.spm.measures.LogStatsCache;
import qut.pm.spm.measures.ProbProcessTreeDeterminismMeasure;
import qut.pm.spm.measures.ProbProcessTreeMeasure;
import qut.pm.spm.measures.SimplicityEdgeCountMeasure;
import qut.pm.spm.measures.SimplicityEntityCountMeasure;
import qut.pm.spm.measures.SimplicityStructuralStochasticUniqMeasure;
import qut.pm.spm.measures.StochasticLogCachingMeasure;
import qut.pm.spm.measures.StochasticStructuralUniqueness;
import qut.pm.spm.measures.TraceEntropyFitness;
import qut.pm.spm.measures.TraceEntropyMeasure;
import qut.pm.spm.measures.TraceEntropyPrecision;
import qut.pm.spm.measures.TraceEntropyProjectFitness;
import qut.pm.spm.measures.TraceEntropyProjectPrecision;
import qut.pm.spm.measures.TraceOverlapRatioMeasure;
import qut.pm.spm.measures.TraceProbabilityMassOverlap;
import qut.pm.spm.measures.TraceRatioMeasure;
import qut.pm.spm.playout.CachingPlayoutGenerator;
import qut.pm.spm.playout.PlayoutGenerator;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.util.ClockUtil;
import qut.pm.xes.helpers.Classifier;

public class ExperimentRunner {

	private enum RunType {
		EXISTING_MODEL, EXISTING_MODEL_COLLECTION_SURVIVAL, EXISTING_MODEL_COLLECTION_EVAL, RANDOM_GENERATION,
		RANDOM_GENERATION_ONLY, GENETIC_MINER, LOG_STATS_ONLY;
	}

	private static final Logger LOGGER = LogManager.getLogger();

	private RunType runType = RunType.EXISTING_MODEL;
	private String dataDir = "";
	private String modelDir = "";
	private String outputDir = "";
	private String logFileName = "";
	private String modelInputFileName = "";
	private String modelInputFilePattern = "";
	private Classifier classifierType = Classifier.NAME;
	private XEventClassifier classifier = classifierType.getEventClassifier();
	private PluginContext pluginContext = null;
	private String buildVersion;
	private List<StochasticLogCachingMeasure> netMeasures;
	private List<ProbProcessTreeMeasure> pptMeasures;
	private List<SPNQualityCalculator> evalMeasures;
	private long randomSeed = 1l;
	private int randomMaxDepth = 0;
	private int genModelTarget = 0;
	private boolean storeRandomModels;
	private long playoutGranularity;
	private PlayoutGenerator generator;
	private int randomMaxTransitions;
	private int randomModelStartAt;
	private SETMConfigParams setmConfigParams;
	private SETMConfiguration setmConfig;
	private RunStatsExporter runStatsExporter;



	private void configure() throws Exception {
		Properties cfg = new Properties();
		cfg.load(new FileInputStream("config/instance.properties"));
		dataDir = cfg.getProperty(SETMConfiguration.CONFIG_DATA_DIR, "data");
		outputDir = cfg.getProperty(SETMConfiguration.CONFIG_OUTPUT_DIR, "var");
		LOGGER.info("Using data location {}", dataDir);
		logFileName = cfg.getProperty(SETMConfiguration.CONFIG_LOG_FILE);
		modelDir = cfg.getProperty(SETMConfiguration.CONFIG_MODEL_DIR);
		configureModelDetails(cfg);
		pluginContext = new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(), "experimentrunner");
		runStatsExporter = new RunStatsExporter(outputDir);
		configureBuildId();
		configureMeasures(cfg);
		configureEval(cfg);
		configureRandomGeneration(cfg);
		configureRunType(cfg);
		configureGeneticMiner(cfg);
	}

	private void configureEval(Properties cfg) {
		evalMeasures = new ArrayList<>();
		String calculatorConfig = cfg.getProperty(SETMConfiguration.CONFIG_CALCULATORS, "");
		for (String calcName : calculatorConfig.split(",")) {
			try {
				Class<?> calcClass = null;
				if (calcName.contains(".")) {
					calcClass = Class.forName(calcName);
				} else {
					calcClass = Class.forName("qut.pm.setm.evaluation." + calcName);
				}
				evalMeasures.add((SPNQualityCalculator) calcClass.getConstructor().newInstance());
			} catch (Exception e) {
				LOGGER.error("Couldn't load calculator class {} ", calcName);
			}
		}

	}

	private void configureModelDetails(Properties cfg) {
		modelInputFileName = cfg.getProperty(SETMConfiguration.CONFIG_MODEL_INPUT_FILE);
		modelInputFilePattern = cfg.getProperty(SETMConfiguration.CONFIG_MODEL_INPUT_FILE_PATTERN);
	}

	private void configureGeneticMiner(Properties cfg) {
		if (runType != RunType.GENETIC_MINER)
			return;
		setmConfig = new SETMConfiguration();
		setmConfigParams = setmConfig.configureFromProperties(cfg);
		setmConfigParams.classifier = classifierType;
		setmConfigParams.logFileName = logFileName;
	}

	private void configureRunType(Properties cfg) {
		runType = RunType
				.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_RUN_TYPE, RunType.EXISTING_MODEL.toString()));
	}

	private void configureRandomGeneration(Properties cfg) {
		randomSeed = Long.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_RANDOM_SEED, "1"));
		randomMaxDepth = Integer.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_RANDOM_MAX_DEPTH, "30"));
		randomMaxTransitions = Integer
				.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_RANDOM_MAX_TRANSITIONS, "1000"));
		randomModelStartAt = Integer.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_RANDOM_MODEL_STARTAT, "0"));
		genModelTarget = Integer.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_RANDOM_TARGET_SIZE, "100"));
		storeRandomModels = Boolean.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_RANDOM_STORE_MODELS,"false"));
	}

	private void configureMeasures(Properties cfg) {
		playoutGranularity = Long.valueOf(cfg.getProperty(SETMConfiguration.CONFIG_PLAYOUT_GRANULARITY, "1000"));
		generator = new CachingPlayoutGenerator(playoutGranularity);
		netMeasures = new ArrayList<>();
		netMeasures.add(new EventRatioMeasure(generator));
		netMeasures.add(new TraceRatioMeasure(generator, 2));
		netMeasures.add(new TraceRatioMeasure(generator, 3));
		netMeasures.add(new TraceRatioMeasure(generator, 4));
		netMeasures.add(new EarthMoversTraceMeasure(generator));
		TraceEntropyMeasure tem = new TraceEntropyMeasure(generator);
		netMeasures.add(new TraceEntropyPrecision(tem));
		netMeasures.add(new TraceEntropyFitness(tem));
		netMeasures.add(new TraceEntropyProjectFitness(tem));
		netMeasures.add(new TraceEntropyProjectPrecision(tem));
		netMeasures.add(new TraceOverlapRatioMeasure(generator));
		netMeasures.add(new TraceProbabilityMassOverlap(generator));
		netMeasures.add(new GenTraceFloorMeasure(generator, 1));
		netMeasures.add(new GenTraceFloorMeasure(generator, 5));
		netMeasures.add(new GenTraceFloorMeasure(generator, 10));
		netMeasures.add(new GenTraceDiffMeasure(generator));
		netMeasures.add(new EntityCount());
		netMeasures.add(new EdgeCount());
		netMeasures.add(new StochasticStructuralUniqueness());
		LogStatsCache statsCache = new LogStatsCache();
		netMeasures.add(new SimplicityEdgeCountMeasure(statsCache));
		netMeasures.add(new SimplicityEntityCountMeasure(statsCache));
		netMeasures.add(new SimplicityStructuralStochasticUniqMeasure(statsCache));
		pptMeasures = new ArrayList<>();
		pptMeasures.add(new ProbProcessTreeDeterminismMeasure());
	}

	private void configureBuildId() {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get("buildid.txt"));
			String text = new String(encoded, StandardCharsets.UTF_8);
			String[] buildIds = text.split(" ");
			buildVersion = buildIds[0] + "." + buildIds[1];
		} catch (Exception e) {
			buildVersion = "dev-no-buildid-file";
		}
	}

	private void initLogInfo(XLog log) {
		XLogInfoFactory.createLogInfo(log, classifier);
	}

	private XLog loadLog(String inputLogName, TaskStats stats) throws Exception {
		stats.markRunning();
		String inputLogFileName = dataDir + File.separator + inputLogName;
		LOGGER.info("Loading log from " + inputLogFileName);
		XLog log = (XLog) new OpenLogFileLiteImplPlugin().importFile(pluginContext, inputLogFileName);
		initLogInfo(log);
		XLogInfo xinfo = log.getInfo(classifier);
		LOGGER.info("Log name: {} Events: {} Activities: {} Traces: {} Total traces: {}",
				inputLogName, xinfo.getNumberOfEvents(), xinfo.getEventClasses().size(), 
				xinfo.getNumberOfTraces(), log.size() );
		stats.setMeasure(Measure.LOG_TRACE_COUNT, log.size());
		stats.setMeasure(Measure.LOG_EVENT_COUNT, log.getInfo(classifier).getNumberOfEvents());
		return log;
	}

	private AcceptingStochasticNet loadNet(File file) throws Exception {
		PluginContext context = new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(),
				"modelrunner_loadnet");
		String shortName = modelInputFileName.replace("osmodel_", "");
		shortName = shortName.substring(0, shortName.indexOf("_"));
		LOGGER.info("Short name {}", shortName);
		Serializer serializer = new Persister();
		PNMLRoot pnml = serializer.read(PNMLRoot.class, file);
		StochasticNetDeserializer converter = new StochasticNetDeserializer();
		Object[] objs = converter.convertToNet(context, pnml, file.getName(), true);
		AcceptingStochasticNet apn = new AcceptingStochasticNetImpl(shortName, (StochasticNet) objs[0],
				(Marking) objs[1], Collections.singleton((Marking) objs[2]));
		return apn;
	}

	private AcceptingStochasticNet loadModel() throws Exception {
		String fname = this.modelDir + File.separator + modelInputFileName.trim();
		LOGGER.info("Loading model from {}", fname);
		AcceptingStochasticNet net = loadNet(new File(fname));
		return net;
	}

	public void runRandomTreeGeneration() {
		LOGGER.info(
				"Generating " + genModelTarget + " random probabilistic process trees" + " using seed " + randomSeed);
		List<String> activities = new LinkedList<String>();
		RunStats runStats = new RunStats(buildVersion, logFileName, "rando-s" + randomSeed);
		TaskStats stats = makeNewTask(runStats, "spmlogparser");
		try {
			XLog log = loadLog(logFileName, stats);
			deriveActivitiesFromLog(activities, log);
			RandomProbProcessTreeGenerator modelGen = new RandomProbProcessTreeGenerator(randomSeed, randomMaxDepth,
					randomMaxTransitions);
			Set<ProbProcessTree> generatedModels = modelGen.generateTrees(activities, genModelTarget);
			// Name the generated models?
			ProbProcessTreeConverter converter = new ProbProcessTreeConverter();
			int i = 1;
			for (ProbProcessTree model : generatedModels) {
				LOGGER.info("Model: " + model);
				String mname = "s" + randomSeed + "m" + i;
				i++;
				if (i <= randomModelStartAt) {
					LOGGER.info("Skipping forward past " + mname);
					continue;
				}
				RunStats modelStats = new RunStats(buildVersion, logFileName, "rando-" + mname);
				AcceptingStochasticNet snet = converter.convertToSNet(model, mname);
				if (storeRandomModels)
					storeModel(new File(outputDir + File.separator + "rando-" + mname + ".pnml"), snet);
				for (StochasticLogCachingMeasure measure : netMeasures) {
					calculateAndRecordMeasure(log, snet, measure, modelStats);
				}
				for (ProbProcessTreeMeasure measure : pptMeasures) {
					calculateAndRecordMeasure(log, model, measure, modelStats);
				}
				modelStats.markEnd();
				runStats.addSubRun(modelStats);
				runStatsExporter.exportRun(runStats);
			}
		} catch (Exception e) {
			closeRunWithError(runStats, e);
			throw new ExperimentRunException(e);
		}

		closeRun(runStats);
	}

	public void runRandomTreeGenerationOnly() {
		LOGGER.info(
				"Generating " + genModelTarget + " random probabilistic process trees" + " using seed " + randomSeed);
		List<String> activities = new LinkedList<String>();
		RunStats runStats = new RunStats(buildVersion, logFileName, "rando-" + randomSeed);
		TaskStats stats = makeNewTask(runStats, "spmlogparser");
		try {
			XLog log = loadLog(logFileName, stats);
			deriveActivitiesFromLog(activities, log);
			RandomProbProcessTreeGenerator modelGen = new RandomProbProcessTreeGenerator(randomSeed, randomMaxDepth,
					randomMaxTransitions);
			Set<ProbProcessTree> generatedModels = modelGen.generateTrees(activities, genModelTarget);
			// Name the generated models?
			ProbProcessTreeConverter converter = new ProbProcessTreeConverter();
			int i = 1;
			for (ProbProcessTree model : generatedModels) {
				String mname = "s" + randomSeed + "m" + i;
				LOGGER.info("Model " + mname + "::" + model);
				i++;
				if (i <= randomModelStartAt) {
					LOGGER.info("Skipping forward past " + mname);
					continue;
				}
				RunStats modelStats = new RunStats(buildVersion, logFileName, "rando-" + mname);
				AcceptingStochasticNet snet = converter.convertToSNet(model, mname);
				LOGGER.info("Model " + mname + " has " + snet.getNet().getTransitions().size() + " transitions, "
						+ snet.getNet().getPlaces().size() + " places and " + snet.getNet().getEdges().size()
						+ " edges.");
				if (storeRandomModels)
					storeModel(new File(outputDir + File.separator + "rando-" + mname + ".pnml"), snet);
				modelStats.markEnd();
				runStats.addSubRun(modelStats);
			}
		} catch (Exception e) {
			closeRunWithError(runStats, e);
			throw new ExperimentRunException(e);
		}
		closeRun(runStats);
	}

	private void storeModel(File modelFile, AcceptingStochasticNet stochasticNetDescriptor) throws Exception {
		StochasticNet net = stochasticNetDescriptor.getNet();
		PNMLRoot root = new StochasticNetToPNMLConverter().convertNet(net, stochasticNetDescriptor.getInitialMarking(),
				new GraphLayoutConnection(net));
		net.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		net.setTimeUnit(TimeUnit.HOURS);
		Serializer serializer = new Persister();
		serializer.write(root, modelFile);
	}

	private void calculateAndRecordMeasure(XLog log, ProbProcessTree model, ProbProcessTreeMeasure measure,
			RunStats runStats) {
		TaskStats task = makeNewTask(runStats, "calculate " + measure.getReadableId());
		double calc = measure.calculate(log, model, classifier);
		LOGGER.info("Calculated " + measure.getReadableId() + " == " + calc);
		task.setMeasure(measure.getMeasure(), calc);
		task.markEnd();
	}

	private void calculateAndRecordEvalMeasure(XLog log, AcceptingStochasticNet model, SPNQualityCalculator calculator,
			RunStats runStats) throws Exception {
		TaskStats task = makeNewTask(runStats, "calculate " + calculator.getReadableId());
		calculator.calculate(pluginContext, model, log, classifier, task);
		LOGGER.info("Calculated " + calculator.getReadableId());
		task.markEnd();

	}

	private void deriveActivitiesFromLog(List<String> activities, XLog log) {
		XLogInfo logInfo = log.getInfo(classifier);
		Collection<XEventClass> eventClasses = logInfo.getEventClasses().getClasses();
		for (XEventClass eventClass : eventClasses) {
			activities.add(eventClass.getId());
		}
	}

	public void runPredefinedModelVsLog() {
		AcceptingStochasticNet model = initModel();
		RunStats runStats = initPredefRunStats(model, "predef");
		TaskStats stats = makeNewTask(runStats, "spmlogparser");
		try {
			XLog log = loadLog(logFileName, stats);
			for (StochasticLogCachingMeasure measure : netMeasures) {
				calculateAndRecordMeasure(log, model, measure, runStats);
			}
		} catch (Exception e) {
			closeRunWithError(runStats, e);
			throw new ExperimentRunException(e);
		}
		closeRun(runStats);
	}

	private RunStats initPredefRunStats(AcceptingStochasticNet model, String prefix) {
		RunStats runStats = new RunStats(buildVersion, logFileName, prefix + "-" + model.getId());
		runStats.setInputModelFileName(modelInputFileName);
		return runStats;
	}

	public void runPredefinedModelVsLogEvalMeasures() {
		AcceptingStochasticNet model = initModel();
		RunStats runStats = initPredefRunStats(model, "pdeval");
		runStats.setInputModelFileName(modelInputFileName);
		TaskStats stats = makeNewTask(runStats, "spmlogparser");
		try {
			XLog log = loadLog(logFileName, stats);
			for (SPNQualityCalculator calculator : evalMeasures) {
				calculateAndRecordEvalMeasure(log, model, calculator, runStats);
			}
		} catch (Exception e) {
			closeRunWithError(runStats, e);
			throw new ExperimentRunException(e);
		}
		closeRun(runStats);
	}

	private AcceptingStochasticNet initModel() {
		AcceptingStochasticNet model = null;
		try {
			model = loadModel();
		} catch (Exception e) {
			LOGGER.error("Couldn't load model");
			throw new ExperimentRunException(e);
		}
		return model;
	}

	public void runPredefinedModelCollectionVsLog() {
		String[] modelFiles = new File(modelDir).list((dir1, name) -> name.matches(modelInputFilePattern));
		LOGGER.info("Loading " + modelFiles.length + " models matching " + modelInputFilePattern);
		for (String modelFile : modelFiles) {
			modelInputFileName = modelFile;
			switch (runType) {
			case EXISTING_MODEL_COLLECTION_SURVIVAL:
				runPredefinedModelVsLog();
				break;
			case EXISTING_MODEL_COLLECTION_EVAL:
				runPredefinedModelVsLogEvalMeasures();
				break;
			default:
				LOGGER.error("Run type:" + runType + " not a predefined model evaluation");
			}
		}
	}

	private TaskStats makeNewTask(RunStats runStats, String newTaskName) {
		TaskStats newTaskStats = new TaskStats(newTaskName);
		newTaskStats.markRunning();
		runStats.addTask(newTaskStats);
		return newTaskStats;
	}

	private void closeRunWithError(RunStats runStats, Exception ex) {
		LOGGER.error("Run ended due to exception ...");
		runStats.markFailed(ex.getMessage());
		try {
			runStatsExporter.exportRun(runStats);
		} catch (Exception e) {
			LOGGER.error("Failed during attempted final export");
		}
	}

	private void closeRun(RunStats runStats) {
		try {
			LOGGER.info("Closing run ...");
			runStats.markEnd();
			runStatsExporter.exportRun(runStats);
		} catch (Exception e) {
			LOGGER.error(e);
			System.exit(1);
		}
	}

	private void calculateAndRecordMeasure(XLog log, AcceptingStochasticNet model, StochasticLogCachingMeasure measure,
			RunStats runStats) throws Exception {
		TaskStats task = makeNewTask(runStats, "calculate " + measure.getReadableId());
		if (measure.isFlaky())
			runStatsExporter.exportRun(runStats);
		double calc = measure.calculate(log, model, classifier);
		LOGGER.info("Calculated " + measure.getReadableId() + " == " + calc);
		task.setMeasure(measure.getMeasure(), calc);
		task.markEnd();
	}

	public void runGeneticMiner() {
		String runId = "setm-s" + randomSeed + "ts" + ClockUtil.dateTime();
		RunStats runStats = new RunStats(buildVersion, logFileName, runId);
		try {
			TaskStats stats = makeNewTask(runStats, "spmlogparser");
			XLog log = loadLog(logFileName, stats);
			stats = makeNewTask(runStats, "etm");
			setmConfigParams.runId = runId;
			ExportObserver exportObserver = new ExportObserver(runStats, outputDir);
			SETMParam etmParamMind = setmConfig.buildETMParam(outputDir, log, setmConfigParams, exportObserver);
			SETM etmMind = new SETM(etmParamMind);
			etmMind.run();
			ProbProcessTree treeMind = etmMind.getResult();
			etmMind.getSatisfiedTerminationConditions();
			LOGGER.info("Termination conditions met: " + etmMind.getTerminationDescription());
			stats = makeNewTask(runStats, "measure");
			String modelName = runId + "-final";
			RunStats modelStats = new RunStats(buildVersion, logFileName, modelName);
			ProbProcessTreeConverter converter = new ProbProcessTreeConverter();
			AcceptingStochasticNet snet = converter.convertToSNet(treeMind, modelName);
			storeModel(new File(outputDir + File.separator + modelName + ".pnml"), snet);
			for (StochasticLogCachingMeasure measure : netMeasures) {
				calculateAndRecordMeasure(log, snet, measure, modelStats);
			}
			for (ProbProcessTreeMeasure measure : pptMeasures) {
				calculateAndRecordMeasure(log, treeMind, measure, modelStats);
			}
			runStatsExporter.exportRun(runStats);
		} catch (Exception e) {
			closeRunWithError(runStats, e);
			throw new ExperimentRunException(e);
		}
		closeRun(runStats);
	}

	private TraceFreq calculateForLog(XLog log, XEventClassifier classifier) {
		TraceFreq result = new TraceFreq();
		for (XTrace trace: log) {
			LinkedList<String> newTrace = new LinkedList<String>();
			for (XEvent event: trace) {
				String classId = classifier.getClassIdentity(event);
				newTrace.add(classId);
			}
			result.incTraceFreq(newTrace);
		}
		return result;
	}
	
	public void runLogStatsOnly() {
		String runId = "logstats" + ClockUtil.dateTime();
		RunStats runStats = new RunStats(buildVersion, logFileName, runId);
		try {
			TaskStats stats = makeNewTask(runStats, "spmlogparser");
			XLog log = loadLog(logFileName, stats);
			stats = makeNewTask(runStats, "logstats");
			TraceFreq tf = calculateForLog(log,classifier);
			LOGGER.info("Trace variants: {}", tf.keySet().size());
			runStatsExporter.exportRun(runStats);
		} catch (Exception e) {
			closeRunWithError(runStats, e);
			throw new ExperimentRunException(e);
		}
		closeRun(runStats);
	}
	
	public void runExperiment() throws Exception {
		LOGGER.info("SPM dimensions experiment runner initializing");
		configure();
		switch (runType) {
		case EXISTING_MODEL:
			runPredefinedModelVsLog();
			break;
		case EXISTING_MODEL_COLLECTION_SURVIVAL:
		case EXISTING_MODEL_COLLECTION_EVAL:
			runPredefinedModelCollectionVsLog();
			break;
		case RANDOM_GENERATION:
			runRandomTreeGeneration();
			break;
		case RANDOM_GENERATION_ONLY:
			runRandomTreeGenerationOnly();
			break;
		case GENETIC_MINER:
			runGeneticMiner();
			break;
		case LOG_STATS_ONLY:
			runLogStatsOnly();
			break;
		default:
			LOGGER.warn("No support for setting:" + runType);
			break;
		}
		LOGGER.info("SPM dimensions experiment runner finished");
	}

	public static void main(String[] args) throws Exception {
		ExperimentRunner exRunner = new ExperimentRunner();
		exRunner.runExperiment();
		System.exit(0);
	}

}
