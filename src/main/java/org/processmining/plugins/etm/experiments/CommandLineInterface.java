package org.processmining.plugins.etm.experiments;

import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.jgraph.JGraph;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.ETM;
import org.processmining.plugins.etm.ETMPareto;
import org.processmining.plugins.etm.factory.InductiveMinerWrapper;
import org.processmining.plugins.etm.factory.TreeFactoryAbstract;
import org.processmining.plugins.etm.factory.TreeFactoryCoordinator;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.metrics.ConfigurationFitness;
import org.processmining.plugins.etm.fitness.metrics.EditDistanceWrapperRTEDAbsolute;
import org.processmining.plugins.etm.fitness.metrics.EditDistanceWrapperRTEDRelative;
// import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.fitness.metrics.Generalization;
import org.processmining.plugins.etm.fitness.metrics.MultiThreadedFitnessEvaluator;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.fitness.metrics.ParetoFitnessEvaluator;
import org.processmining.plugins.etm.fitness.metrics.PrecisionEscEdges;
import org.processmining.plugins.etm.fitness.metrics.SimplicityUselessNodes;
import org.processmining.plugins.etm.logging.StatisticsLogger;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.narytree.conversion.NAryTreeToProcessTree;
import org.processmining.plugins.etm.model.narytree.conversion.ProcessTreeToNAryTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.model.serialization.ParetoFrontExport;
import org.processmining.plugins.etm.mutation.GuidedTreeMutationCoordinator;
import org.processmining.plugins.etm.mutation.TreeCrossover;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;
import org.processmining.plugins.etm.mutation.TreeMutationCoordinator;
import org.processmining.plugins.etm.mutation.mutators.AddNodeRandom;
import org.processmining.plugins.etm.mutation.mutators.ConfigurationMutator;
import org.processmining.plugins.etm.mutation.mutators.MutateSingleNodeRandom;
import org.processmining.plugins.etm.mutation.mutators.NormalizationMutation;
import org.processmining.plugins.etm.mutation.mutators.RemoveActivityGuided;
import org.processmining.plugins.etm.mutation.mutators.RemoveSubtreeRandom;
import org.processmining.plugins.etm.mutation.mutators.RemoveUselessNodes;
import org.processmining.plugins.etm.mutation.mutators.ReplaceTreeMutation;
import org.processmining.plugins.etm.mutation.mutators.ShuffleCluster;
import org.processmining.plugins.etm.mutation.mutators.maikelvaneck.CombinationCrossover;
import org.processmining.plugins.etm.mutation.mutators.maikelvaneck.IntelligentTreeFactory;
import org.processmining.plugins.etm.mutation.mutators.maikelvaneck.MutateSingleNodeGuided;
import org.processmining.plugins.etm.mutation.mutators.maikelvaneck.ReplaceTreeByIntelligentTreeMutation;
import org.processmining.plugins.etm.parameters.ETMParam;
import org.processmining.plugins.etm.parameters.ETMParamConfigurable;
import org.processmining.plugins.etm.parameters.ETMParamPareto;
import org.processmining.plugins.etm.parameters.ETMParamParetoConfigurable;
import org.processmining.plugins.etm.utils.LogUtils;
import org.processmining.plugins.ptmerge.ptmerge.PlugIn;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.ProcessTreeImpl;
import org.processmining.processtree.ptml.exporting.PtmlExportTree;
import org.processmining.processtree.visualization.bpmn.TreeBPMNLayoutBuilder;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.TerminationCondition;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;
import org.uncommons.watchmaker.framework.termination.GenerationCount;

import cern.colt.Arrays;
import nl.tue.astar.AStarThread.Canceller;

/**
 * Command line interface for running the ETM from the command line. Especially
 * usefull for running experiments.
 * 
 * @author jbuijs
 * 
 */
public class CommandLineInterface {

	//FIXME make interactive (help mode etc.) show what options it has
	//TODO use CLI boot context of ProM???
	//TODO rename color modes to meaningful names
	//TODO make generic for all different ETM modes and different metrics/termination etc. properties there are and could/will be
	//TODO add 'produce latex' command
	//TODO add 'max bytes to use' for fitness replay

	public static final String NORMAL = "NORMAL"; //Act normal: e.g. 1 log, 1 process tree

	public static final String PARETO = "PARETO"; //Discover a Pareto front

	public static final String BLUE = "BLUE"; //mine individual models then merge (approach 1)
	public static final String RED = "RED"; //mine central model, then individualize models, then merge (approach 2)
	public static final String BLACK = "BLACK"; //mine one model then configure (approach 3)
	public static final String GREEN = "GREEN"; //mine model and configurations at same time (approach 4)

	private static final String PARETO_GREEN = "PARETO_GREEN"; //Combi of pareto and green approach (e.g. configuration dimension added)

	private int maxGen = 80000;
	private int popSize = 20;
	private int eliteSize = (int) (popSize * .3);
	private double crossOverChance = 0.1;
	/**
	 * Fitness Replay inner limit value
	 */
	private double limitF = -1;//.5;
	/**
	 * Fitness Replay inner time limit
	 */
	private double limitFTime = 100;

	private double targetFitness = 1;
	private double frWeight = 10;
	private double peWeight = 5;
	private double geWeight = .1;
	private double smWeight = 10;
	private double edWeight = 100;
	private int maxED = -1;

	private double frLimit = -1;
	private double peLimit = -1;
	private double geLimit = -1;
	private double smLimit = -1;
	private int generationWhenLimitsAreApplied = -1;
	private int PFmaxSize = -1;

	private double chanceOfRandomMutation = .95;
	private boolean preventDuplicates = false;

	private double configurationAlpha = .001;

	//SPECIAL WEIGHTS FOR BLACK APPROACH
	private Double frWeightBlack;
	private Double peWeightBlack;

	private int logModulo = 10; //create a seperate log file every this much generations
	private boolean useGreenSeed = false; //Whether or not to kick-start green with the ~optimal solution FALSE for real experiments of course!!!
	private boolean frMultiplication = false; //Whether the overall fitness should be fr * (SUM(weight*fitness)/SUM(weight)) or fr should be weighted.

	private boolean enableMaikelsSmartStuff = true; //Whether guided tree factory, crossover and mutation are enabled.
	private double IMfactoryWeight = 0; //whether to use the Inductive Miner to create trees

	private boolean createStatsFile = true;

	private static String comment = "Full redo of experiments after discovery of several issues (23-04-2014).";

	private String expCode = "";
	private String expDesc = "";
	private String expDate = "";
	private String expNode = "";

	/**
	 * Date and time until which the algorithm should run
	 */
	private Date runUntil;

	private XLog[] logs = null;
	private String[] methods = null;
	private String loggingPath;
	private ArrayList<String> seedTrees = new ArrayList<String>();

	private String expSession;

	private String[] arguments;

	/**
	 * 
	 * @param args
	 *            (method{blue,red,black,green}, logging path)
	 */
	public static void main(String[] args) {
		CommandLineInterface cli = new CommandLineInterface(args);

		cli.run();
	}

	public CommandLineInterface(String[] args) {
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryNaiveImpl());

		System.out.println("Arguments: " + Arrays.toString(args));
		arguments = args;

		/*
		 * Sanity check first: we assume at least 2 arguments
		 */
		if (args.length <= 2) {
			System.out
					.println("Not enough parameters provided. "
							+ "The first parameter should be the execution type ('NORMAL', 'PARETO', 'RED','BLUE','BLACK','GREEN'). "
							+ "And the second should be the logging path. "
							+ "Then provide the individual arguments (such as 'log=' for each log that should be processed, 'fr=' etc. for each dimension weight, etc.)");
		}

		/*
		 * PARSE ARGUMENTS
		 */

		int argIndex = 0;

		//First argument is list of methods to run (e.g. GREEN/BLUE/RED/BLACK/NORMAL/PARETO)
		methods = args[argIndex++].split("/");

		//Second argument is always the logging path
		loggingPath = args[argIndex++];

		//For parsing an unknown number of input logs we need a list
		ArrayList<XLog> logsList = new ArrayList<XLog>();

		//FIXME create a more structured approach to parse and publish command line arguments
		//We now go into keyword search mode
		for (int i = argIndex; i < args.length; i++) {
			//Split on '=' sign
			String[] arg = args[i].split("=");
			String key = arg[0].trim();
			String value = arg[1].trim();

			//Now switch on key (Java 1.6 style) and set the corresponding value
			//TODO auto-detect fitness metric codes using registry
			if (key.equalsIgnoreCase("Fr")) {
				frWeight = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("Pe")) {
				peWeight = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("Sm")) {
				smWeight = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("Ge") || key.equalsIgnoreCase("Gv")) {
				geWeight = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("frLimit")) {
				frLimit = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("peLimit")) {
				peLimit = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("geLimit")) {
				geLimit = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("smLimit")) {
				smLimit = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("PFmaxSize")) {
				PFmaxSize = Integer.parseInt(value);
			} else if (key.equalsIgnoreCase("generationWhenLimitsAreApplied")) {
				generationWhenLimitsAreApplied = Integer.parseInt(value);
			} else if (key.equalsIgnoreCase("cAlpha")) {
				configurationAlpha = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("maxED")) {
				maxED = Integer.parseInt(value);
			} else if (key.equalsIgnoreCase("Ed")) {
				edWeight = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("eliteSize")) {
				eliteSize = Integer.parseInt(value);
			} else if (key.equalsIgnoreCase("popSize")) {
				popSize = Integer.parseInt(value);
			} else if (key.equalsIgnoreCase("maxGen")) {
				maxGen = Integer.parseInt(value);
			} else if (key.equalsIgnoreCase("runUntil")) {
				SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				try {
					runUntil = format.parse(value);
				} catch (ParseException e) {
					System.err
							.println("Error parsing the provided runUntil value, please provide it in the following format: yyyy-MM-dd HH:mm:ss");
					//e.printStackTrace();
					return;
				}
			} else if (key.equalsIgnoreCase("FrMulti")) {
				frMultiplication = Boolean.parseBoolean(value);
			} else if (key.equalsIgnoreCase("FrBlack")) {
				frWeightBlack = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("limitF")) {
				limitF = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("limitFTime")) {
				limitFTime = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("logModulo")) {
				logModulo = Integer.parseInt(value);
			} else if (key.equalsIgnoreCase("seedTree")) {
				seedTrees.add(value);
			} else if (key.equalsIgnoreCase("randomMutRatio")) {
				chanceOfRandomMutation = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("IMfactoryWeight")) {
				IMfactoryWeight = Double.parseDouble(value);
			} else if (key.equalsIgnoreCase("maikel")) {
				enableMaikelsSmartStuff = parseBooleanArgument(value);
				//Don't change the setting if no correct command was given
			} else if (key.equalsIgnoreCase("stats")) {
				if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("on")) {
					createStatsFile = true;
				} else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("off")) {
					createStatsFile = false;
				}
				//Don't change the setting if no correct command was given
			} else if (key.equalsIgnoreCase("expCode")) {
				expCode = value;
			} else if (key.equalsIgnoreCase("expDesc")) {
				expDesc = value;
			} else if (key.equalsIgnoreCase("expDate")) {
				expDate = value;
			} else if (key.equalsIgnoreCase("expNode")) {
				expNode = value;
			} else if (key.equalsIgnoreCase("expSession")) {
				expSession = value;
			} else if (key.equalsIgnoreCase("log")) {
				//the log value should point to an event log which we should load and add to our array

				File logFile = new File(value);

				XUniversalParser parser = new XUniversalParser();
				if (!parser.canParse(logFile)) {
					System.err
							.println("We can not parse the following log file (is the path correct???), skipping this log input: "
									+ value);
				} else {
					try {
						Collection<XLog> parsedLogs = parser.parse(logFile);
						Iterator<XLog> iterator = parsedLogs.iterator();
						while (iterator.hasNext()) {
							logsList.add(iterator.next());
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						System.err.println("Error while parsing the following log file, skipping this log input: "
								+ value);
						e.printStackTrace();
					}
				}
			}
		}

		//If no input logs were given
		if (!logsList.isEmpty()) {
			//copy to array
			logs = logsList.toArray(new XLog[logsList.size()]);
		} else {
			logs = new XLog[] { /*-*/StandardLogs.createDefaultLog(),/**/StandardLogs.createConfig2(),
					StandardLogs.createConfig3(), StandardLogs.createConfig4() };
		}
	}

	/*-
	public CommandLineInterface() {
		logs = new XLog[] { /*-* /BPMTestLogs.createDefaultLog(),/** /BPMTestLogs.createConfig2(),
				BPMTestLogs.createConfig3(), BPMTestLogs.createConfig4() };
	}/**/

	public void run() {
		//Construct and set the logging path
		if (loggingPath.charAt(loggingPath.length() - 1) != '/') {
			loggingPath += '/';
		}
		if (expDate.isEmpty() || expCode.isEmpty() || expNode.isEmpty()) {
			long expStart = System.currentTimeMillis();
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH_mm");
			String date = dateFormat.format(new Date(expStart));
			loggingPath = loggingPath + date + "/";
		} else {
			loggingPath += expDate + "_" + expCode + "_" + expSession + "_" + expNode + "/";
		}

		//New method uses experiment code
		File createDir = new File(loggingPath);
		createDir.mkdirs();

		logSettings(true);
		//createKozaTableau();

		/*-*/
		for (int m = 0; m < methods.length; m++) {
			if (methods[m].equalsIgnoreCase(RED)) {
				redMethod(loggingPath + RED + "/");
			} else if (methods[m].equalsIgnoreCase(BLUE)) {
				blueMethod(loggingPath + BLUE + "/");
			} else if (methods[m].equalsIgnoreCase(NORMAL)) {
				normalMethod(loggingPath + NORMAL + "/");
			} else if (methods[m].equalsIgnoreCase(PARETO)) {
				paretoMethod(loggingPath + PARETO + "/");
			} else if (methods[m].equalsIgnoreCase(PARETO_GREEN)) {
				paretoGreenMethod(loggingPath + PARETO_GREEN + "/");
			} else {
				//Unknown method type:
				System.out.println(String.format("Could not find the method '%s', skipping this method type.",
						methods[m]));
			}
		}

		System.out.println("Finished");
		System.exit(0);
	}

	/**
	 * This is a 'normal' run of the ETM algorithm. E.g. one run per event log,
	 * producing one process tree.
	 * 
	 * @param loggingPath
	 * @param logs
	 */
	private void normalMethod(String loggingPath) {
		System.out.println("Starting " + NORMAL);

		for (int l = 0; l < logs.length; l++) {
			ETMParam etmParamMind = buildETMParam(loggingPath + (logs.length > 1 ? "log/" + l : ""),
					new XLog[] { logs[l] });

			ETM etmMind = new ETM(etmParamMind);

			etmMind.run();
			ProbProcessArrayTree treeMind = etmMind.getResult();
			etmMind.getSatisfiedTerminationConditions();
			System.out.println("Termination conditions met: " + etmMind.getTerminationDescription());

			//FIXME bug: mapping wrong class names in output during translation to PT
			outputTree(etmParamMind.getPath(), NORMAL + "-log" + l, treeMind, etmParamMind.getCentralRegistry());

			//Now write as a BPMN picture
			ProcessTree processTree = NAryTreeToProcessTree.convert(treeMind, etmParamMind.getCentralRegistry()
					.getEventClasses());
			treeToBPMN(processTree, new File(loggingPath + "/BPMN" + l + ".png"));
		}
	}

	/**
	 * This is run of the ETM algorithm producing a
	 * {@link org.processmining.plugins.etm.model.ParetoFront} of
	 * {@link ProcessTree} for each of the event logs.
	 * 
	 * @param loggingPath
	 * @param logs
	 */
	private void paretoMethod(String loggingPath) {
		System.out.println("Starting " + PARETO);

		for (int l = 0; l < logs.length; l++) {
			ETMParamPareto etmParamPareto = buildETMParetoParam(loggingPath + (logs.length > 1 ? "log/" + l : ""),
					logs[l]);

			ETMPareto etmPareto = new ETMPareto(etmParamPareto);

			etmPareto.run();

			ParetoFront paretoFront = etmPareto.getResult();

			//Now write the resulting Pareto Front to file
			try {
				ParetoFrontExport.export(paretoFront, new File(loggingPath + "paretoFront.PTPareto"));
			} catch (IOException e) {
				e.printStackTrace();
			}

			ParetoFront normalizedFront = new ParetoFront(paretoFront.getRegistry(), paretoFront.getDimensions());
			try {
				Collection<ProbProcessArrayTree> pfTrees = paretoFront.getFront();
				for (ProbProcessArrayTree tree : pfTrees) {
					ProbProcessArrayTree normalized = TreeUtils.normalize(tree);
					etmParamPareto.getFitnessEvaluator().getFitness(normalized, null);
					normalizedFront.consider(normalized);
				}
				ParetoFrontExport.export(normalizedFront, new File(loggingPath + "paretoFrontPostProcessed.PTPareto"));
			} catch (ConcurrentModificationException cme) {
				//let user know we failed
				System.out
						.println("We failed in creating the normalized Pareto front, but we exit gracefully. Reason: "
								+ cme.getMessage());
			} catch (IOException e) {
				//e.printStackTrace();
				System.out
						.println("We failed in creating the normalized Pareto front, but we exit gracefully. Reason: "
								+ e.getMessage());
			}

		}
	}

	/**
	 * 
	 * @param loggingPath
	 */
	private void paretoGreenMethod(String loggingPath) {
		System.out.println("Starting " + PARETO_GREEN);

		if (logs.length <= 1) {
			System.out.println("Only one log provided, stopping Pareto GREEN");
			return;
		}

		ETMParamParetoConfigurable etmParamPareto = (ETMParamParetoConfigurable) buildETMParetoParam(loggingPath
				+ "log/", logs);

		ETMPareto etmPareto = new ETMPareto(etmParamPareto);

		etmPareto.run();

		ParetoFront paretoFront = etmPareto.getResult();

		//Now write the resulting Pareto Front to file
		try {
			ParetoFrontExport.export(paretoFront, new File(loggingPath + "paretoFront.PTPareto"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		Collection<ProbProcessArrayTree> pfTrees = new HashSet<ProbProcessArrayTree>();
		pfTrees.addAll(paretoFront.getFront()); //TODO fix pfTrees is still backed by PF
		Collection<ProbProcessArrayTree> pfTreesNormalized = new HashSet<ProbProcessArrayTree>();
		for (ProbProcessArrayTree tree : pfTrees) {
			ProbProcessArrayTree normalized = TreeUtils.normalize(tree);
			etmParamPareto.getFitnessEvaluator().getFitness(normalized, null);
			pfTreesNormalized.add(normalized);
		}
		paretoFront.consider(pfTreesNormalized);

		try {
			ParetoFrontExport.export(paretoFront, new File(loggingPath + "paretoFrontPostProcessed.PTPareto"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * The BLUE method is like red but without the M* in between, and is the
	 * first approach as presented by Gottschalk: Mine a model for each log
	 * individually, then merge those. Approach 1 in paper.
	 * 
	 * @param string
	 * @param logs
	 */
	private void blueMethod(String loggingPath) {
		System.out.println("Starting " + BLUE);
		ProbProcessArrayTree[] trees = new ProbProcessArrayTree[logs.length];

		for (int l = 0; l < logs.length; l++) {
			ETMParam etmParamMind = buildETMParam(loggingPath + "M/" + l, new XLog[] { logs[l] });

			ETM etmMind = new ETM(etmParamMind);

			etmMind.run();
			ProbProcessArrayTree treeMind = etmMind.getResult();

			//FIXME bug: mapping wrong class names in output during translation to PT
			outputTree(etmParamMind.getPath(), BLUE + "-M" + l, treeMind, etmParamMind.getCentralRegistry());

			trees[l] = treeMind;
		}

		CentralRegistryConfigurable registry = new CentralRegistryConfigurable(XLogInfoImpl.STANDARD_CLASSIFIER,
				new Random(), logs);
		mergeTrees(loggingPath, BLUE, logs, trees, registry);

	}

	/**
	 * The RED method: mine normal model for M*, then Mx for Lx within certain
	 * edit distance, then merge Mx's to configurable model. Approach 2 in paper
	 * 
	 * @param loggingPath
	 * @param logs
	 */
	private void redMethod(String loggingPath) {
		System.out.println("Starting RED");
		//First merge the log files and mine a model on that log
		XLog mergedLog = LogUtils.mergeLogs(logs);
		ETMParam etmParamMstar = buildETMParam(loggingPath + "Mstar/", new XLog[] { mergedLog });

		//Since 'RED' mines twice, half each run's max. generation to be comparable
		Iterator<TerminationCondition> it = etmParamMstar.getTerminationConditions().iterator();
		while (it.hasNext()) {
			//for (TerminationCondition condition : etmParamMstar.getTerminationConditions()) {
			TerminationCondition condition = it.next();
			if (condition instanceof GenerationCount) {
				it.remove();
			}
		}
		etmParamMstar.addTerminationConditionMaxGen(maxGen / 2);

		ETM etmMstar = new ETM(etmParamMstar);

		etmMstar.run();
		ProbProcessArrayTree treeMStar = etmMstar.getResult();

		outputTree(etmParamMstar.getPath(), "RED-M*", treeMStar, etmParamMstar.getCentralRegistry());

		/*
		 * Then for each individual log individualize the model for that log
		 * within a certain edit distance
		 */
		//First prepare a configurable registry to update the individual ones to 'known' the missing event classes (which will then hopefully be removed)
		CentralRegistry registry = etmParamMstar.getCentralRegistry();
		XEventClassifier eventClassifier = registry.getEventClasses().getClassifier();
		CentralRegistryConfigurable registryConfigurable = new CentralRegistryConfigurable(eventClassifier,
				registry.getRandom(), mergedLog);
		//And the seed
		ProbProcessArrayTree[] seed = new ProbProcessArrayTree[] { treeMStar };

		ProbProcessArrayTree[] trees = new ProbProcessArrayTree[logs.length];
		for (int l = 0; l < logs.length; l++) {
			ETMParam etmParamMind = buildETMParam(loggingPath + "M/" + l, new XLog[] { logs[l] });
			/*-*/
			registryConfigurable.updateRegistryEClist(etmParamMind.getCentralRegistry(), eventClassifier);
			etmParamMind.setSeed(seed);
			//Since 'RED' mines twice, half each run's max. generation to be comparable
			Iterator<TerminationCondition> itInd = etmParamMstar.getTerminationConditions().iterator();
			while (itInd.hasNext()) {
				//for (TerminationCondition condition : etmParamMstar.getTerminationConditions()) {
				TerminationCondition condition = itInd.next();
				if (condition instanceof GenerationCount) {
					itInd.remove();
				}
			}
			etmParamMstar.addTerminationConditionMaxGen(maxGen / 2);
			/**/

			//For EACH OverallFitness Evaluator
			FitnessEvaluator<ProbProcessArrayTree> mainEval = etmParamMind.getFitnessEvaluator();

			OverallFitness[] oFarray = null;
			if (mainEval instanceof MultiThreadedFitnessEvaluator) {
				MultiThreadedFitnessEvaluator mtf = (MultiThreadedFitnessEvaluator) mainEval;
				oFarray = new OverallFitness[mtf.getEvaluators().length];
				for (int f = 0; f < mtf.getEvaluators().length; f++) {
					oFarray[f] = (OverallFitness) mtf.getEvaluators()[f];
				}
			} else if (mainEval instanceof OverallFitness) {
				oFarray = new OverallFitness[1];
				oFarray[0] = (OverallFitness) mainEval;
			}

			for (OverallFitness oF : oFarray) {
				//Add the edit distance fitness
				//Set<NAryTree> base = new HashSet<NAryTree>();
				//base.add(new NAryTreeImpl(treeMStar));
				EditDistanceWrapperRTEDRelative editDistance = new EditDistanceWrapperRTEDRelative(seed);
				editDistance.setLimit(maxED);
				oF.addEvaluator(editDistance, edWeight);
				/**/
			}

			//The fitness has changed!
			etmParamMind.getCentralRegistry().clearFitnessCache();

			ETM etmMind = new ETM(etmParamMind);

			etmMind.run();
			ProbProcessArrayTree treeMind = etmMind.getResult();

			outputTree(etmParamMind.getPath(), RED + "-M" + l, treeMind, etmParamMind.getCentralRegistry());

			trees[l] = treeMind;
		}

		mergeTrees(loggingPath, RED, logs, trees, etmParamMstar.getCentralRegistry());

	}

//	private void blackMethod(String loggingPath) {
//		System.out.println("Starting BLACK");
//		//First merge the log files and mine a model on that log
//		XLog mergedLog = LogUtils.mergeLogs(logs);
//		ETMParam etmParamMstar = buildETMParam(loggingPath + "Mstar/", new XLog[] { mergedLog });
//		ETM etmMstar = new ETM(etmParamMstar);
//
//		/*
//		 * To discover Mstar for this method, we use a different precision
//		 * weight since we need this model to be fitting, not precise (we do
//		 * that during configuration)
//		 */
//		frWeightBlack = frWeight;
//		peWeightBlack = peWeight / 10d;
//		if (etmParamMstar.getFitnessEvaluator() instanceof OverallFitness) {
//			OverallFitness eval = (OverallFitness) etmParamMstar.getFitnessEvaluator();
//			for (TreeFitnessAbstract key : eval.getEvaluators().keySet()) {
//				} if (key instanceof PrecisionEscEdges) {
//					eval.getEvaluators().put(key, peWeightBlack);
//				}
//			}
//		}
//
//		etmMstar.run();
//		ProbProcessTree treeMStar = etmMstar.getResult();
//		etmParamMstar.getCentralRegistry().getFitness(treeMStar);
//
//		outputTree(etmParamMstar.getPath(), "BLACK-M*", treeMStar, etmParamMstar.getCentralRegistry());
//
//		if (logs.length == 1) {
//			System.out.println("Only one log provided, no need to start configuring");
//			return;
//		}
//
//		//Then for each individual log configure the model for that log without changing it
//		ETMParam etmParamMind = buildETMParam(loggingPath + "Mind/", logs);
//		//We only mutate the configuration, not the tree itself
//		ArrayList<EvolutionaryOperator<ProbProcessTree>> evolutionaryOperators = new ArrayList<EvolutionaryOperator<ProbProcessTree>>();
//		LinkedHashMap<TreeMutationAbstract, Double> mutators = new LinkedHashMap<TreeMutationAbstract, Double>();
//		mutators.put(new ConfigurationMutator((CentralRegistryConfigurable) etmParamMind.getCentralRegistry()), 1.);
//		TreeMutationCoordinator oneMut = new TreeMutationCoordinator(mutators, preventDuplicates);
//		evolutionaryOperators.add(oneMut);
//		etmParamMind.setEvolutionaryOperators(evolutionaryOperators);
//
//		/*-* /
//		ConfigurationFitness cF = (ConfigurationFitness) etmParamMind.getFitnessEvaluator();
//		TreeFitnessAbstract[] fitnessList = cF.getFitnessList();
//		for (TreeFitnessAbstract fitness : fitnessList) {
//			OverallFitness oF = (OverallFitness) fitness;
//			Set<NAryTree> base = new HashSet<NAryTree>();
//			base.add(treeMStar);
//			EditDistanceWrapperRTED editDistance = new EditDistanceWrapperRTED(base);
//			editDistance.setBeAbsolute(true);
//			editDistance.setLimit(0);//Don't edit the model itself
//			oF.addEvaluator(editDistance, edWeight);
//		}/**/
//
//		ProbProcessTree[] seed = new ProbProcessTree[] { treeMStar };
//		etmParamMind.setSeed(seed);
//
//		ETM etmMind = new ETM(etmParamMind);
//
//		etmMind.run();
//		ProbProcessTree treeMind = etmMind.getResult();
//		etmParamMind.getCentralRegistry().getFitness(treeMind);
//		//		CentralRegistryConfigurable cregconf = (CentralRegistryConfigurable) etmParamMind.getCentralRegistry();
//		//		cregconf.getRegistry(1).getFitness(treeMind);
//
//		outputTree(etmParamMind.getPath(), "BLACK-Mc", treeMind, etmParamMind.getCentralRegistry());
//	}
//
//	private void greenMethod(String loggingPath) {
//		System.out.println("Starting GREEN");
//
//		if (logs.length <= 1) {
//			System.out.println("Only one log provided, no need for GREEN method, run NORMAL instead.");
//			return;
//		}
//
//		ETMParamConfigurable etmParam = (ETMParamConfigurable) buildETMParam(loggingPath, logs);
//
//		CentralRegistryConfigurable registry = etmParam.getCentralRegistry();
//
//		if (useGreenSeed) {
//			etmParam.setSeed(setGreenSeed(registry));
//		}
//
//		ETM etm = new ETM(etmParam);
//		etm.run();
//
//		ProbProcessTree result = etm.getResult();
//
//		outputTree(etmParam.getPath(), "GREEN-Mc", result, registry);
//	}

//	private static ProbProcessTree[] setGreenSeed(CentralRegistry registry) {
//		/*
//		 * TEMP: start from the ideal tree to see if another tree is better and
//		 * why
//		 */
//		ProbProcessTree perfectTree = TreeUtils.fromString("SEQ( LEAF: A+complete , "
//				+ "AND( AND( LEAF: B+complete , SEQ( LEAF: B1+complete , LEAF: B2+complete ) ) , "
//				+ "LEAF: C+complete , XOR( LEAF: D+complete , LEAF: D2+complete ) ) , "
//				+ "XOR( LEAF: E+complete , LEAF: F+complete ) , LEAF: G+complete )", registry.getEventClasses());
//		/*-*/
//		//4 configurations all 'not configured'
//		perfectTree.addConfiguration(new Configuration(new byte[] { Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED }));
//		perfectTree.addConfiguration(new Configuration(new byte[] { Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED }));
//		perfectTree.addConfiguration(new Configuration(new byte[] { Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED }));
//		perfectTree.addConfiguration(new Configuration(new byte[] { Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
//				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED }));
//		/*-*/
//		//Then update where necessary
//		perfectTree.setNodeConfiguration(0, 5, Configuration.HIDDEN); //Hide SEQ(B1,B2)
//		perfectTree.setNodeConfiguration(0, 11, Configuration.HIDDEN); //Hide D2
//		//BROKEN:
//		perfectTree.setNodeConfiguration(1, 4, Configuration.HIDDEN); //Conf2: Hide B
//		perfectTree.setNodeConfiguration(1, 10, Configuration.BLOCKED); //disable D (e.g. always D2)
//		perfectTree.setNodeConfiguration(1, 2, Configuration.SEQ); //AND to SEQ because always b1-b2-c-d 
//		//BROKEN:
//		perfectTree.setNodeConfiguration(2, 5, Configuration.HIDDEN); //Conf 3: hide SEQ B1 B2
//		perfectTree.setNodeConfiguration(2, 9, Configuration.HIDDEN); //hide Xor D
//		perfectTree.setNodeConfiguration(2, 15, Configuration.HIDDEN); //hide G
//		//BROKEN:
//		perfectTree.setNodeConfiguration(3, 4, Configuration.HIDDEN); //Conf 4: hide B
//		perfectTree.setNodeConfiguration(3, 15, Configuration.HIDDEN); //hide G
//		/**/
//
//		ProbProcessTree[] seed = new ProbProcessTree[] { perfectTree };
//		/*
//		 * END TEMP seed perfect tree
//		 */
//		return seed;
//	}

	/**
	 * Takes an array of NAryTrees and logs and merges them using Dennis' merge
	 * code on CoSeNets, also writes the ProcessTree to a file and picture.
	 * 
	 * @param loggingPath
	 * @param method
	 * @param logs
	 * @param trees
	 */
	private void mergeTrees(String loggingPath, String method, XLog[] logs, ProbProcessArrayTree[] trees, CentralRegistry registry) {
		if (trees.length < 2) {
			System.out.println("merge trees method called with only 1 tree provided, hence nothing to merge!");
			return;
		}
		CentralRegistryConfigurable mergedRegistry = (CentralRegistryConfigurable) buildETMParam(loggingPath + "TMP/",
				logs).getCentralRegistry();

		//And then merge the individual models into one configurable model
		//i.e. call Dennis' code on CoSeNets translated from ProcessTrees variants of our NAryTrees
		ProcessTree merged = NAryTreeToProcessTree.convert(trees[0], mergedRegistry.getEventClasses());
		writePT(loggingPath + "/pt" + 0, merged);
		for (int t = 1; t < trees.length; t++) {
			ProcessTree currentProcessTree = NAryTreeToProcessTree.convert(trees[t], mergedRegistry.getEventClasses());
			writePT(loggingPath + "/pt" + t, currentProcessTree);
			try {
				merged = PlugIn.mergeDAGSActMap(null, merged, currentProcessTree);
			} catch (AssertionError e) {
				System.err.println("ERROR IN MERGE DAGS ACT - manually process files!!! ");
				System.exit(0); //do not produce final results, but terminate as-if OK
			}
		}

		ProcessTreeImpl mergedImpl = ((ProcessTreeImpl) merged);
		System.out.println(method + "-Mc " + mergedImpl.toString());

		try {
			PtmlExportTree ptExport = new PtmlExportTree();
			File ptFile = new File(loggingPath + "/processTree.ptml");
			ptExport.exportDefault(null, mergedImpl, ptFile);

			//seems to hang on frame.pack using SSH
			//treeToBPMN(mergedImpl, new File(loggingPath + "/processTree.png"));
		} catch (IOException e) {
			e.printStackTrace(System.err);
		} catch (NullPointerException npe) {
			npe.printStackTrace(System.err);
		}

		String latex = PaperMethods.processTreeToLatex(mergedImpl);
		System.out.println(latex);

		ProcessTreeToNAryTree converter = new ProcessTreeToNAryTree(mergedRegistry.getEventClasses());
		System.out.println(PaperMethods.naryTreeToLatex(converter.convert(mergedImpl),
				mergedRegistry.getEventClasses(), registry));
	}

	public ETMParam buildETMParam(String loggingPath, XLog[] logs) {
		Canceller canceller = new Canceller() {
			public boolean isCancelled() {
				return false;
			}
		};

		boolean configurable = false;
		if (logs.length > 1) {
			configurable = true;
		}

		CentralRegistry registry;
		CentralRegistryConfigurable cReg = null;
		if (configurable) {
			registry = new CentralRegistryConfigurable(XLogInfoImpl.STANDARD_CLASSIFIER, new Random(), logs);
			cReg = (CentralRegistryConfigurable) registry;
		} else {
			registry = new CentralRegistry(logs[0], XLogInfoImpl.STANDARD_CLASSIFIER, new Random());
		}

		TreeFitnessAbstract evaluator;
		if (configurable) {
			TreeFitnessAbstract[] evaluators = new TreeFitnessAbstract[logs.length];
			for (int i = 0; i < logs.length; i++) {
				LinkedHashMap<TreeFitnessAbstract, Double> alg = new LinkedHashMap<TreeFitnessAbstract, Double>();

				// FitnessReplay fr = new FitnessReplay(cReg.getRegistry(i), canceller, limitF, limitFTime, true, -1, 1);
				// alg.put(fr, frWeight);
				alg.put(new PrecisionEscEdges(cReg.getRegistry(i)), peWeight);
				alg.put(new Generalization(cReg.getRegistry(i)), geWeight);
				//				alg.put(new SimplicityMixed(), smWeight);
				alg.put(new SimplicityUselessNodes(), smWeight);

				OverallFitness of = new OverallFitness(cReg.getRegistry(i), alg);
				of.fitnessMultiplication = frMultiplication;
				evaluators[i] = of;
			}

			evaluator = new ConfigurationFitness(cReg, configurationAlpha, false, evaluators);
		} else {
			LinkedHashMap<TreeFitnessAbstract, Double> alg = new LinkedHashMap<TreeFitnessAbstract, Double>();

//			FitnessReplay fr = new FitnessReplay(registry, canceller, limitF, limitFTime);
//			fr.updateMaxBytesToUse(1024 * 1024 * 1);
//			fr.setDetailedAlignmentInfoEnabled(enableMaikelsSmartStuff);
//			alg.put(fr, frWeight);
			alg.put(new PrecisionEscEdges(registry), peWeight);
			alg.put(new Generalization(registry), geWeight);
			//			alg.put(new SimplicityMixed(), smWeight);
			alg.put(new SimplicityUselessNodes(), smWeight);

			OverallFitness of = new OverallFitness(registry, alg);
			of.fitnessMultiplication = frMultiplication;
			evaluator = of;
		}

		/*-*/
		//Make multi threaded......
		evaluator = new MultiThreadedFitnessEvaluator(registry, evaluator, Math.max(Runtime.getRuntime()
				.availableProcessors() / 2, 1));
		/**/

		//Evolutionary Operators
		ArrayList<EvolutionaryOperator<ProbProcessArrayTree>> evolutionaryOperators = new ArrayList<EvolutionaryOperator<ProbProcessArrayTree>>();

		LinkedHashMap<TreeMutationAbstract, Double> dumbMutators = new LinkedHashMap<TreeMutationAbstract, Double>();
		dumbMutators.put(new AddNodeRandom(registry), 1.);
		dumbMutators.put(new MutateSingleNodeRandom(registry), 1.);
		dumbMutators.put(new RemoveSubtreeRandom(registry), 1.);
		dumbMutators.put(new NormalizationMutation(registry), 1.);
		if (configurable) {
			dumbMutators.put(new ConfigurationMutator(cReg), 1.);
		}
		dumbMutators.put(new RemoveUselessNodes(registry), 1.);
		dumbMutators.put(new ShuffleCluster(registry), 1.);
		TreeMutationCoordinator dumbCoordinator = new TreeMutationCoordinator(dumbMutators, preventDuplicates);

		AbstractCandidateFactory<ProbProcessArrayTree> factory;

		Map<TreeFactoryAbstract, Double> otherFactories = new HashMap<TreeFactoryAbstract, Double>();

		if (enableMaikelsSmartStuff) {
			otherFactories.put(new IntelligentTreeFactory(registry), (1. - chanceOfRandomMutation));

			evolutionaryOperators.add(new CombinationCrossover<ProbProcessArrayTree>(1, new Probability(crossOverChance), registry,
					null));

			LinkedHashMap<TreeMutationAbstract, Double> smartMutators = new LinkedHashMap<TreeMutationAbstract, Double>();
			/*-*/
			//smartMutators.put(new InsertActivityGuided(registry), 1.); //Improves F
			//			smartMutators.put(new MutateLeafClassGuided(registry), 1.); //Improves F
			//			smartMutators.put(new MutateOperatorTypeGuided(registry), 1.); //Can improve both F and P
			smartMutators.put(new RemoveActivityGuided(registry), 1.); //Improves F and/or P
			smartMutators.put(new MutateSingleNodeGuided(registry), 1.);

			dumbMutators.put(new ReplaceTreeByIntelligentTreeMutation(registry), 1.);

			evolutionaryOperators.add(new GuidedTreeMutationCoordinator(registry, chanceOfRandomMutation,
					preventDuplicates, smartMutators, dumbCoordinator));

		} else {
			//Dumb only
			evolutionaryOperators.add(new TreeCrossover(1, new Probability(crossOverChance),registry));

			dumbMutators.put(new ReplaceTreeMutation(registry), 1.);
			evolutionaryOperators.add(dumbCoordinator);
		}

		if (IMfactoryWeight > 0) {
			otherFactories.put(new InductiveMinerWrapper(registry), IMfactoryWeight);
		}
		TreeFactoryCoordinator treeFactoryCoordinator = new TreeFactoryCoordinator(registry, chanceOfRandomMutation,
				otherFactories);

		ETMParam etmParam;
		if (configurable) {
			etmParam = new ETMParamConfigurable(cReg, evaluator, evolutionaryOperators, popSize, eliteSize);
		} else {
			etmParam = new ETMParam(registry, evaluator, evolutionaryOperators, popSize, eliteSize);
		}

		etmParam.setFactory(treeFactoryCoordinator);
		etmParam.addTerminationCondition(new GenerationCount(maxGen));
		etmParam.addTerminationConditionTargetFitness(targetFitness, evaluator.getInfo().isNatural());
		etmParam.setPath(loggingPath);
		etmParam.setLogModulo(logModulo);

		if (createStatsFile) {
			etmParam.addEvolutionObserver(new StatisticsLogger(etmParam));
		}

		/*-* /
		EvolutionLogger<NAryTree> obs = new EvolutionLogger<NAryTree>(context, registry, false);
		obs.setProgressLevels(maxGen);
		etmParam.addEvolutionObserver(obs);

		progress.inc();
		/**/

		/*-* /
		try {
			PackageManager.getInstance().findOrInstallPackages("LpSolve");
		} catch (Exception e) {
			e.printStackTrace();
		}/**/

		return etmParam;
	}

	public ETMParamPareto buildETMParetoParam(String loggingPath, XLog log) {
		return buildETMParetoParam(loggingPath, new XLog[] { log });
	}

	public ETMParamPareto buildETMParetoParam(String loggingPath, XLog[] log) {
		Canceller canceller = new Canceller() {
			public boolean isCancelled() {
				return false;
			}
		};

		boolean configurable = false;
		if (logs.length > 1) {
			configurable = true;
		}

		//START
		//		CLIContext global = new CLIContext();
		//		CLIPluginContext clic = new CLIPluginContext(global, "CLI ngrid context");

		CentralRegistry registry;
		CentralRegistryConfigurable cReg = null;
		int cacheSize = popSize + PFmaxSize; //make sure that there is enough space for the population + whole PF
		if (configurable) {
			registry = new CentralRegistryConfigurable(XLogInfoImpl.STANDARD_CLASSIFIER, new Random(), logs);
			cReg = (CentralRegistryConfigurable) registry;
			cReg.increaseFitnessCache(cacheSize);
		} else {
			registry = new CentralRegistry(logs[0], XLogInfoImpl.STANDARD_CLASSIFIER, new Random());
			registry.increaseFitnessCache(cacheSize);
		}

		ArrayList<ProbProcessArrayTree> seed = new ArrayList<ProbProcessArrayTree>();
		if (!seedTrees.isEmpty()) {
			for (String tree : seedTrees) {
				seed.add(TreeUtils.fromString(tree, registry.getEventClasses()));
			}
		}

		TreeFitnessAbstract evaluator;
		if (configurable) {
			TreeFitnessAbstract[] evaluators = new TreeFitnessAbstract[logs.length];
			for (int i = 0; i < logs.length; i++) {
				LinkedHashMap<TreeFitnessAbstract, Double> alg = new LinkedHashMap<TreeFitnessAbstract, Double>();

				if (!seed.isEmpty()) {
					EditDistanceWrapperRTEDAbsolute editDistance = new EditDistanceWrapperRTEDAbsolute(seed);
					//Need weight of 0 otherwise overallFitness results in negative value...
					alg.put(editDistance, 0.0001);
				}

//				FitnessReplay fr = new FitnessReplay(cReg.getRegistry(i), canceller, limitF, limitFTime, true, -1, 1);
//				alg.put(fr, frWeight);
				alg.put(new PrecisionEscEdges(cReg.getRegistry(i)), peWeight);
				alg.put(new Generalization(cReg.getRegistry(i)), geWeight);
				//alg.put(new SimplicityMixed(), smWeight);
				alg.put(new SimplicityUselessNodes(), smWeight);

				OverallFitness of = new OverallFitness(cReg.getRegistry(i), alg);
				of.fitnessMultiplication = frMultiplication;
				evaluators[i] = of;
			}

			evaluator = new ConfigurationFitness(cReg, configurationAlpha, true, evaluators);
		} else {
			LinkedHashMap<TreeFitnessAbstract, Double> alg = new LinkedHashMap<TreeFitnessAbstract, Double>();

			if (!seed.isEmpty()) {
				EditDistanceWrapperRTEDAbsolute editDistance = new EditDistanceWrapperRTEDAbsolute(seed);
				//Need weight of 0 otherwise overallFitness results in negative value...
				alg.put(editDistance, 0.0001);
			}

//			FitnessReplay fr = new FitnessReplay(registry, canceller, limitF, limitFTime);
//			alg.put(fr, frWeight);
			alg.put(new PrecisionEscEdges(registry), peWeight);
			alg.put(new Generalization(registry), geWeight);
			//alg.put(new SimplicityMixed(), smWeight);
			alg.put(new SimplicityUselessNodes(), smWeight);

			OverallFitness of = new OverallFitness(registry, alg);
			of.fitnessMultiplication = frMultiplication;
			evaluator = of;
		}

		//The special Pareto fitness evaluator...
		ParetoFitnessEvaluator paretoFitnessEvaluator = new ParetoFitnessEvaluator(registry);

		/*-*/
		//Make multi threaded......
		evaluator = new MultiThreadedFitnessEvaluator(registry, evaluator, Math.max(Runtime.getRuntime()
				.availableProcessors() / 2, 1));
		/**/

		//Evolutionary Operators
		ArrayList<EvolutionaryOperator<ProbProcessArrayTree>> evolutionaryOperators = new ArrayList<EvolutionaryOperator<ProbProcessArrayTree>>();
		//		evolutionaryOperators.add(new GraduallyConsiderMoreEventClasses(registry));
		LinkedHashMap<TreeMutationAbstract, Double> dumbMutators = new LinkedHashMap<TreeMutationAbstract, Double>();

		dumbMutators.put(new AddNodeRandom(registry), 1.);
		dumbMutators.put(new MutateSingleNodeRandom(registry), 1.);
		dumbMutators.put(new RemoveSubtreeRandom(registry), 1.);
		dumbMutators.put(new NormalizationMutation(registry), 1.);
		//		dumbMutators.put(new ReplaceTreeMutation(registry), 1.); //also added in the else dumb only
		dumbMutators.put(new RemoveUselessNodes(registry), 1.);
		dumbMutators.put(new ShuffleCluster(registry), 1.);
		if (configurable) {
			dumbMutators.put(new ConfigurationMutator(cReg), 1.);
		}

		TreeMutationCoordinator dumbCoordinator = new TreeMutationCoordinator(dumbMutators, preventDuplicates);

		HashMap<TreeFactoryAbstract, Double> otherFactories = new HashMap<TreeFactoryAbstract, Double>();

		if (enableMaikelsSmartStuff) {
			otherFactories.put(new IntelligentTreeFactory(registry), (1. - chanceOfRandomMutation));

			evolutionaryOperators.add(new CombinationCrossover<ProbProcessArrayTree>(1, new Probability(crossOverChance), registry,
					null));

			LinkedHashMap<TreeMutationAbstract, Double> smartMutators = new LinkedHashMap<TreeMutationAbstract, Double>();
			/*-*/
			//smartMutators.put(new InsertActivityGuided(registry), 1.); //Improves F
			//			smartMutators.put(new MutateLeafClassGuided(registry), 1.); //Improves F
			//			smartMutators.put(new MutateOperatorTypeGuided(registry), 1.); //Can improve both F and P
			smartMutators.put(new RemoveActivityGuided(registry), 1.); //Improves F and/or P
			smartMutators.put(new MutateSingleNodeGuided(registry), 1.);

			dumbMutators.put(new ReplaceTreeByIntelligentTreeMutation(registry), 1.);

			evolutionaryOperators.add(new GuidedTreeMutationCoordinator(registry, chanceOfRandomMutation,
					preventDuplicates, smartMutators, dumbCoordinator));

		} else {
			//Dumb only
			evolutionaryOperators.add(new TreeCrossover(1, new Probability(crossOverChance),registry));

			dumbMutators.put(new ReplaceTreeMutation(registry), 1.);
			evolutionaryOperators.add(dumbCoordinator);
		}

		ETMParamPareto etmParam;
		if (configurable) {
			etmParam = new ETMParamParetoConfigurable(cReg, evaluator, paretoFitnessEvaluator, evolutionaryOperators,
					popSize, eliteSize);
		} else {
			etmParam = new ETMParamPareto(registry, evaluator, paretoFitnessEvaluator, evolutionaryOperators, popSize,
					eliteSize);
		}

		TreeFactoryCoordinator treeFactoryCoordinator = new TreeFactoryCoordinator(registry, chanceOfRandomMutation,
				otherFactories);
		etmParam.setFactory(treeFactoryCoordinator);
		etmParam.addTerminationCondition(new GenerationCount(maxGen));
		//etmParam.addTerminationConditionTargetFitness(targetFitness, ConfigurationFitness.info.isNatural());
		etmParam.setPath(loggingPath);
		etmParam.setLogModulo(logModulo);
		etmParam.addIgnoredDimension(OverallFitness.info);
		etmParam.setSeed(seed);
		etmParam.setParetoFrontMaxSize(PFmaxSize);

		if (createStatsFile) {
			etmParam.addEvolutionObserver(new StatisticsLogger(etmParam));
		}

		//Copy the limits
//		if (frLimit != -1) {
//			etmParam.getLowerlimits().put(FitnessReplay.info, frLimit);
//		}
		if (peLimit != -1) {
			etmParam.getLowerlimits().put(PrecisionEscEdges.info, peLimit);
		}
		if (geLimit != -1) {
			etmParam.getLowerlimits().put(Generalization.info, geLimit);
		}
		if (smLimit != -1) {
			etmParam.getLowerlimits().put(SimplicityUselessNodes.info, smLimit);
		}
		etmParam.setGenerationWhenLimitsAreApplied(generationWhenLimitsAreApplied);

		return etmParam;
	}

	public static JGraph treeToBPMN(ProcessTree tree, File imageFile) {

		TreeBPMNLayoutBuilder builder = new TreeBPMNLayoutBuilder(tree, null);
		//TreeLayoutBuilder builder = new TreeLayoutBuilder(tree);
		JGraph graph = builder.getJGraph();

		//Now save the image already to a fixed dir to easy working on papers...
		try {
			//FROM JGraph manual p. 97
			//http://touchflow.googlecode.com/hg-history/75fada644b2a19c744130923cbd34747fba861a2/doc/jgraphmanual.pdf

			/*-*/
			//Required to make the graph do the layout thingy otherwise image is empty
			JFrame frame = new JFrame("Test of TreeBuilder");
			JScrollPane scroll = new JScrollPane(graph);
			frame.add(scroll);
			frame.setSize(builder.getDimension(null));
			frame.setPreferredSize(builder.getDimension(null));
			frame.pack();
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			//frame.setVisible(true);
			/*-*/

			OutputStream out = new FileOutputStream(imageFile);
			Color bg = null; // Use this to make the background transparent
			int inset = 0;
			BufferedImage img = graph.getImage(bg, inset);
			String ext = "png";
			ImageIO.write(img, ext, out);
			out.flush();
			out.close();

			//Put on Clipboard
			/*-* /
			Toolkit tolkit = Toolkit.getDefaultToolkit();
			Clipboard clip = tolkit.getSystemClipboard();
			clip.setContents(new ImageSelection(img), null);
			/**/
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // Replace with your output stream
		catch (HeadlessException eh) {
			//If headless stop here silently
		}

		return graph;
	}

	/**
	 * Outputs the NAryTree to the outputDir (e.g. string representation and
	 * fitness information).
	 * 
	 * @param outputDir
	 * @param key
	 * @param tree
	 * @param centralRegistry
	 */
	public static void outputTree(String outputDir, String key, ProbProcessArrayTree tree, CentralRegistry centralRegistry) {
		String string = key + " " + TreeUtils.toString(tree, centralRegistry.getEventClasses()) + " "
				+ centralRegistry.getFitness(tree).toString();
		if (centralRegistry instanceof CentralRegistryConfigurable) {
			CentralRegistryConfigurable centralRegistryConf = (CentralRegistryConfigurable) centralRegistry;
			for (int i = 0; i < centralRegistryConf.getNrLogs(); i++) {
				string += "\n " + i + ": "
						+ centralRegistryConf.getRegistry(i).getFitness(tree.applyConfiguration(i)).toString();
			}

		}

		//LaTeX representation for easy paper processing
		string += "\n " + PaperMethods.naryTreeToLatex(tree, centralRegistry.getEventClasses(), centralRegistry);

		System.out.println(string);

		new File(outputDir).mkdirs();
		File outputFile = new File(outputDir + "/000result.txt");
		try {
			outputFile.createNewFile();
			FileWriter writer = new FileWriter(outputFile);
			writer.append(string);
			writer.append("\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void logSettings(boolean syso) {
		//Build the string
		StringBuffer writer = new StringBuffer();
		writer.append("ARGUMENTS: \t" + Arrays.toString(arguments) + "\n");
		writer.append("METHODS: \t" + Arrays.toString(methods) + "\n");
		writer.append("Logging Path: \t" + loggingPath + "\n");
		writer.append("chanceOfRandomMutation: \t" + chanceOfRandomMutation + "\n");
		writer.append("configurationAlpha: \t" + configurationAlpha + "\n");
		writer.append("crossOverChance: \t" + crossOverChance + "\n");
		writer.append("edWeight: \t" + edWeight + "\n");
		writer.append("eliteSize: \t" + eliteSize + "\n");
		writer.append("experiment code: \t " + expCode + "\n");
		writer.append("experiment date and time: \t " + expDate + "\n");
		writer.append("experiment description: \t " + expDesc + "\n");
		writer.append("experiment node: \t" + expNode + "\n");
		writer.append("frMultiplication: \t" + frMultiplication + "\n");
		writer.append("frWeight: \t" + frWeight + "\n");
		writer.append("frWeightBlack: \t" + frWeightBlack + "\n");
		writer.append("geWeight: \t" + geWeight + "\n");
		writer.append("greenSeed: \t" + useGreenSeed + "\n");
		writer.append("limitF: \t" + limitF + "\n");
		writer.append("limitFTime: \t" + limitFTime + "\n");
		writer.append("Lower limit Fitness Replay: \t" + frLimit + "\n");
		writer.append("Lower limit Precision: \t" + peLimit + "\n");
		writer.append("Lower limit Generalization: \t" + geLimit + "\n");
		writer.append("Lower limit Simplicity: \t" + smLimit + "\n");
		writer.append("Limit applied after generation: \t" + generationWhenLimitsAreApplied + "\n");
		writer.append("logModulo: \t" + logModulo + "\n");
		writer.append("maxED: \t" + maxED + "\n");
		writer.append("maxGen: \t" + maxGen + "\n");
		writer.append("Maikel: \t" + enableMaikelsSmartStuff + "\n");
		writer.append("peWeight: \t" + peWeight + "\n");
		writer.append("peWeightBlack: \t" + peWeightBlack + "\n");
		writer.append("popSize: \t" + popSize + "\n");
		if (runUntil != null)
			writer.append("runUntil: \t" + runUntil.toString() + "\n");
		else
			writer.append("runUntil: \t NOT SET \n");
		writer.append("smWeight: \t" + smWeight + "\n");
		writer.append("targetFitness: \t" + targetFitness + "\n");
		writer.append("comment: \t" + comment + "\n");

		writer.append("\n\nThe following seed tree strings were received: \n");
		Iterator<String> seedTreesIt = seedTrees.iterator();
		while (seedTreesIt.hasNext()) {
			String str = seedTreesIt.next();
			writer.append(str + "\n");
		}

		//Now write the section with the log files
		writer.append("\n\nThe following logs were used: \n");
		for (XLog log : logs) {
			XLogInfo info = XLogInfoFactory.createLogInfo(log);
			writer.append(XConceptExtension.instance().extractName(log) + "\n");
			writer.append(String.format("\t #traces: %d \t #events: %d \n ", info.getNumberOfTraces(),
					info.getNumberOfEvents()));
		}

		String outputMessage = writer.toString();

		outputMessage(outputMessage, "/000settings.txt", syso);
	}

	private void outputMessage(String message, String filename, boolean syso) {
		//Print to console if desired
		if (syso) {
			System.out.println(message);
		}
		//		System.out.println("Methods: " + Arrays.toString(methods));
		//		System.out.println("Logging path " + loggingPath);

		//And write to file
		try {
			File outputFile = new File(loggingPath + filename);
			FileWriter fileWriter = new FileWriter(outputFile);
			outputFile.createNewFile();
			fileWriter.write(message);
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void writePT(String filename, ProcessTree pt) {
		try {
			PtmlExportTree ptExport = new PtmlExportTree();
			File ptFile = new File(filename + ".ptml");
			ptFile.createNewFile();
			ptExport.exportDefault(null, pt, ptFile);

			CommandLineInterface.treeToBPMN(pt, new File(filename + ".png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public XLog[] getLogs() {
		return logs;
	}

	/**
	 * Returns a string of a LaTeX table representing the Koza Tableau with the
	 * settings used. NOTE: WORK IN PROGRESS!!!
	 * 
	 * @return
	 */
	public void createKozaTableau() {
		String noValue = "\\ldots";
		StringBuffer writer = new StringBuffer();
		writer.append("\\begin{table}");
		writer.append("  \\centering");
		writer.append("	 \\begin{tabular}{ll}");
		writer.append("	   	\\addlinespace");
		writer.append("		\\toprule");
		writer.append("		\\textbf{Parameter}            & \\textbf{Setting} \\");
		writer.append("		\\midrule");
		writer.append("		Objective                     & \\ldots \\");
		writer.append("		Candidate Selection           & " + noValue + " \\");
		writer.append("		Population size               & " + popSize + "- \\");
		writer.append("		Elite size                    & " + eliteSize + " \\");
		writer.append("		Cross-over probability        & " + crossOverChance + " \\");
		writer.append("		Guided mutation probability   & - \\");
		writer.append("		Mutation operators            & - \\");
		writer.append("		Termination criterion         & - \\");
		writer.append("		Initialisation method(s)      & - \\");
		writer.append("		\\bottomrule");
		writer.append("  \\end{tabular}");
		writer.append("	 \\caption{Preliminary Koza tableau with experiment settings.}");
		writer.append("	 \\label{tab:" + expCode + "_kozaTableau}");
		writer.append("\\end{table}");
		outputMessage(writer.toString(), expCode + "_kozaTableau.tex", false);
	}

	/**
	 * Tries to 'freely' interpret the argument as a boolean
	 * 
	 * @param argument
	 * @return TRUE if argument is 'true', 'on' or '1', FALSE otherwise
	 */
	public static boolean parseBooleanArgument(String argument) {
		if (argument.equalsIgnoreCase("true") || argument.equalsIgnoreCase("on") || argument.equalsIgnoreCase("1")) {
			return true;
		}
		return false;
	}
}
