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
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.pnml.exporting.StochasticNetToPNMLConverter;
import org.processmining.plugins.pnml.importing.StochasticNetDeserializer;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import qut.pm.prom.helpers.ConsoleUIPluginContext;
import qut.pm.prom.helpers.HeadlessDefinitelyNotUIPluginContext;
import qut.pm.setm.evaluation.SPNQualityCalculator;
import qut.pm.setm.observer.ExportObserver;
import qut.pm.setm.parameters.SETMParam;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.AcceptingStochasticNetImpl;
import qut.pm.spm.FiniteStochasticLangGenerator;
import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.log.LogUtil;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.measures.AlphaPrecisionRestrictedMeasure;
import qut.pm.spm.measures.AlphaPrecisionUnrestrictedMeasure;
import qut.pm.spm.measures.EarthMoversTraceMeasure;
import qut.pm.spm.measures.EdgeCount;
import qut.pm.spm.measures.EntityCount;
import qut.pm.spm.measures.EntropicRelevanceRestrictedMeasure;
import qut.pm.spm.measures.EntropicRelevanceUniformMeasure;
import qut.pm.spm.measures.EntropicRelevanceZeroOrderMeasure;
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
import qut.pm.spm.measures.StochasticStructuralComplexity;
import qut.pm.spm.measures.TraceEntropyFitness;
import qut.pm.spm.measures.TraceEntropyMeasure;
import qut.pm.spm.measures.TraceEntropyPrecision;
import qut.pm.spm.measures.TraceEntropyProjectFitness;
import qut.pm.spm.measures.TraceEntropyProjectPrecision;
import qut.pm.spm.measures.TraceOverlapRatioMeasure;
import qut.pm.spm.measures.TraceRatioMeasure;
import qut.pm.spm.playout.CachingPlayoutGenerator;
import qut.pm.spm.playout.PlayoutGenerator;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.util.ClockUtil;
import qut.pm.xes.helpers.Classifier;

public class ExperimentRunner {

	private enum RunType {
		EXISTING_MODEL, EXISTING_MODEL_COLLECTION_SURVIVAL, 
		EXISTING_MODEL_COLLECTION_EVAL, 
		RANDOM_GENERATION, RANDOM_GENERATION_ONLY, 
		GENETIC_MINER, LOG_STATS_ONLY, EXISTING_RUN_ADD_METRICS;
	}

	private static final Logger LOGGER = LogManager.getLogger();

	private RunType runType = RunType.EXISTING_MODEL;
	private String dataDir = "";
	private String modelDir = "";
	private String outputDir = "";
	private String runDir = "";
	private String logFileName = "";
	private String modelInputFileName = "";
	private String modelInputFilePattern = "";
	private String runFileInputPattern = "";
	private Classifier classifierType = Classifier.NAME;
	private XEventClassifier classifier = classifierType.getEventClassifier();
	private PluginContext pluginContext = null;
	private String buildVersion;
	private List<StochasticLogCachingMeasure> netMeasures;
	private List<StochasticLogCachingMeasure> exploreMetrics;
	private List<StochasticLogCachingMeasure> geneticMeasures;
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
		configurePreviousRuns(cfg);
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

	private void configureGeneticMiner(Properties cfg) throws Exception{
		if (runType != RunType.GENETIC_MINER)
			return;
		String ffClass = cfg.getProperty(SETMConfiguration.CONFIG_SETM_FITNESS_FUNCTION,"");
		if ("".equals(ffClass) )
			setmConfig = new SETMConfiguration();
		else {
			Class<?> calcClass = Class.forName("qut.pm.setm.fitness." + ffClass);
			setmConfig  = (SETMConfiguration) calcClass.getConstructor().newInstance();
			LOGGER.info("Using genetic miner fitness function {} ", ffClass);
		}
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
		geneticMeasures = new ArrayList<>();
		geneticMeasures.add(new EventRatioMeasure(generator));
		geneticMeasures.add(new TraceRatioMeasure(generator, 2));
		geneticMeasures.add(new TraceRatioMeasure(generator, 3));
		geneticMeasures.add(new TraceRatioMeasure(generator, 4));
		geneticMeasures.add(new EarthMoversTraceMeasure(generator));
		TraceEntropyMeasure tem = new TraceEntropyMeasure(generator);
		geneticMeasures.add(new TraceEntropyPrecision(tem));
		geneticMeasures.add(new TraceEntropyFitness(tem));
		geneticMeasures.add(new TraceEntropyProjectFitness(tem));
		geneticMeasures.add(new TraceEntropyProjectPrecision(tem));
		geneticMeasures.add(new TraceOverlapRatioMeasure(generator));
		geneticMeasures.add(new GenTraceFloorMeasure(generator, 5));
		geneticMeasures.add(new GenTraceDiffMeasure(generator));
		geneticMeasures.add(new AlphaPrecisionUnrestrictedMeasure(generator,0));
		geneticMeasures.add(new AlphaPrecisionUnrestrictedMeasure(generator,1));
		geneticMeasures.add(new AlphaPrecisionRestrictedMeasure(generator,1));
		LogStatsCache statsCache = new LogStatsCache();
		geneticMeasures.add(new SimplicityEdgeCountMeasure(statsCache));
		geneticMeasures.add(new SimplicityEntityCountMeasure(statsCache));
		geneticMeasures.add(new SimplicityStructuralStochasticUniqMeasure(statsCache));
		pptMeasures = new ArrayList<>();
		pptMeasures.add(new ProbProcessTreeDeterminismMeasure());
		// Metrics
		exploreMetrics = new ArrayList<>();
		exploreMetrics.add(new StochasticStructuralComplexity());
		exploreMetrics.add(new EntropicRelevanceUniformMeasure(generator) );
		exploreMetrics.add(new EntropicRelevanceZeroOrderMeasure(generator) );
		exploreMetrics.add(new EntropicRelevanceRestrictedMeasure(generator) );
		exploreMetrics.add(new EntityCount());
		exploreMetrics.add(new EdgeCount());
		// dropped measures from conf to journal
		// netMeasures.add(new TraceProbabilityMassOverlap(generator));
		// netMeasures.add(new GenTraceFloorMeasure(generator, 1));
		// netMeasures.add(new GenTraceFloorMeasure(generator, 10));
		netMeasures = new ArrayList<>();
		netMeasures.addAll(geneticMeasures);
		netMeasures.addAll(exploreMetrics);
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

	private void configurePreviousRuns(Properties cfg) {
		runDir = cfg.getProperty(SETMConfiguration.CONFIG_INPUT_RUN_FILE_DIR,outputDir);
		runFileInputPattern = cfg.getProperty(SETMConfiguration.CONFIG_INPUT_RUN_FILE_PATTERN);
	}

	
	private void initLogInfo(ProvenancedLog plog) {
		XLogInfoFactory.createLogInfo(plog, classifier);
	}

	private ProvenancedLog loadLog(String inputLogName, TaskStats stats) throws Exception {
		stats.markRunning();
		String inputLogFileName = dataDir + File.separator + inputLogName;
		LOGGER.info("Loading log from " + inputLogFileName);
		ProvenancedLog plog = LogUtil.importLog(inputLogFileName,pluginContext);
		initLogInfo(plog);
		XLogInfo xinfo = plog.getInfo(classifier);
		LOGGER.info("Log name: {} Events: {} Activities: {} Traces: {} Total traces: {}",
				inputLogName, xinfo.getNumberOfEvents(), xinfo.getEventClasses().size(), 
				xinfo.getNumberOfTraces(), plog.size() );
		stats.setMeasure(Measure.LOG_TRACE_COUNT, plog.size());
		stats.setMeasure(Measure.LOG_EVENT_COUNT, xinfo.getNumberOfEvents());
		return plog;
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
		TaskStats stats = runStatsExporter.makeNewTask(runStats, "spmlogparser");
		try {
			ProvenancedLog log = loadLog(logFileName, stats);
			RunCalculator runCalculator = 
					new RunCalculator(netMeasures, runStatsExporter, log, classifier);
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
				runCalculator.calculateMeasures(modelStats, snet);
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
		TaskStats stats = runStatsExporter.makeNewTask(runStats, "spmlogparser");
		try {
			ProvenancedLog log = loadLog(logFileName, stats);
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

	/** 
	 * Load some existing runs, find the model behind it, calculate some new 
	 * metric, add it to the run record. 
	 */
	public void runPredefAddMetric() throws Exception{
		// only ones needed for now
		List<StochasticLogCachingMeasure> extraMetrics = new ArrayList<>();
		extraMetrics.add(new EntityCount());
		extraMetrics.add(new EdgeCount());
		//
		LOGGER.info("Using run file input directory {}",runDir);
		String[] runFiles = new File(runDir).list((dir1, name) -> name.matches(runFileInputPattern));
		LOGGER.info("Loading " + runFiles.length + " run files matching " + runFileInputPattern);
		// RunStats overallRunStats = new RunStats(buildVersion,logFileName,"runPredefAddMetric");
		TaskStats loadTask = new TaskStats("loading log");
		ProvenancedLog log = loadLog(logFileName, loadTask);
		for (String runFile: runFiles) {
			File sourceRunFile = new File(runDir + File.separator + runFile);
			File outRunFile = new File(outputDir + File.separator + runFile);
			Serializer serializer = new Persister();
			RunStats sourceRunStats = serializer.read(RunStats.class, sourceRunFile );
			modelInputFileName = sourceRunStats.getInputModelFileName();
			String origArtifactCreator = sourceRunStats.getArtifactCreator();
			RunStats extraRunStats = new RunStats(buildVersion, logFileName, origArtifactCreator);
			LOGGER.info("Calculating extra measures for model {} against log {} from run {}", 
					modelInputFileName, logFileName, runFile);
			RunCalculator runCalculator = 
					new RunCalculator(extraMetrics, runStatsExporter, log, classifier);
			AcceptingStochasticNet model = loadModel();
			runCalculator.justCalculateMeasures(extraRunStats, model);
			extraRunStats.markEnd();
			for (TaskStats taskStats: extraRunStats.getTaskRunStats()) {
				sourceRunStats.addTask(taskStats);
			}
			sourceRunStats.markEnd();
			serializer.write(sourceRunStats, outRunFile);
		}
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

	private void calculateAndRecordMeasure(ProvenancedLog log, ProbProcessTree model, ProbProcessTreeMeasure measure,
			RunStats runStats) {
		TaskStats task = runStatsExporter.makeNewTask(runStats, "calculate " + measure.getReadableId());
		double calc = measure.calculate(log, model, classifier);
		LOGGER.info("Calculated " + measure.getReadableId() + " == " + calc);
		task.setMeasure(measure.getMeasure(), calc);
		task.markEnd();
	}

	private void calculateAndRecordEvalMeasure(ProvenancedLog log, AcceptingStochasticNet model, SPNQualityCalculator calculator,
			RunStats runStats) throws Exception {
		TaskStats task = runStatsExporter.makeNewTask(runStats, "calculate " + calculator.getReadableId());
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
		TaskStats stats = runStatsExporter.makeNewTask(runStats, "spmlogparser");
		try {
			ProvenancedLog log = loadLog(logFileName, stats);
			RunCalculator runCalculator = 
					new RunCalculator(netMeasures, runStatsExporter, log, classifier);
			runCalculator.calculateMeasures(runStats, model);
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
		TaskStats stats = runStatsExporter.makeNewTask(runStats, "spmlogparser");
		try {
			ProvenancedLog log = loadLog(logFileName, stats);
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


	public void runGeneticMiner() {
		String runId = "setm-s" + randomSeed + "ts" + ClockUtil.dateTime();
		RunStats runStats = new RunStats(buildVersion, logFileName, runId);
		try {
			TaskStats stats = runStatsExporter.makeNewTask(runStats, "spmlogparser");
			ProvenancedLog log = loadLog(logFileName, stats);
			RunCalculator runCalculator = 
					new RunCalculator(netMeasures, runStatsExporter, log, classifier);
			RunCalculator exploreCalc = 
					new RunCalculator(exploreMetrics, runStatsExporter, log, classifier);
			stats = runStatsExporter.makeNewTask(runStats, "etm");
			setmConfigParams.runId = runId;
			ExportObserver exportObserver = new ExportObserver(runStats, exploreCalc );
			SETMParam etmParamMind = setmConfig.buildETMParam(outputDir, log, setmConfigParams, exportObserver);
			SETM etmMind = new SETM(etmParamMind);
			etmMind.run();
			ProbProcessTree treeMind = etmMind.getResult();
			etmMind.getSatisfiedTerminationConditions();
			LOGGER.info("Termination conditions met: " + etmMind.getTerminationDescription());
			stats = runStatsExporter.makeNewTask(runStats, "measure");
			String modelName = runId + "-final";
			RunStats modelStats = new RunStats(buildVersion, logFileName, modelName);
			ProbProcessTreeConverter converter = new ProbProcessTreeConverter();
			AcceptingStochasticNet snet = converter.convertToSNet(treeMind, modelName);
			storeModel(new File(outputDir + File.separator + modelName + ".pnml"), snet);
			runCalculator.calculateMeasures(modelStats, snet);
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
	
	public void runLogStatsOnly() {
		String runId = "logstats" + ClockUtil.dateTime();
		RunStats runStats = new RunStats(buildVersion, logFileName, runId);
		try {
			TaskStats stats = runStatsExporter.makeNewTask(runStats, "spmlogparser");
			ProvenancedLog log = loadLog(logFileName, stats);
			stats = runStatsExporter.makeNewTask(runStats, "logstats");
			TraceFreq tf = new FiniteStochasticLangGenerator().calculateTraceFreqForLog(log,classifier);
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
		case EXISTING_RUN_ADD_METRICS:
			runPredefAddMetric();
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
