package org.processmining.plugins.etm.parameters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.factory.TreeFactoryCoordinator;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.metrics.ConfigurationFitness;
import org.processmining.plugins.etm.fitness.metrics.EditDistanceWrapperRTEDAbsolute;
import org.processmining.plugins.etm.fitness.metrics.Generalization;
import org.processmining.plugins.etm.fitness.metrics.GeneralizationByFitnessReplayDeviation;
import org.processmining.plugins.etm.fitness.metrics.MultiThreadedFitnessEvaluator;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.fitness.metrics.ParetoFitnessEvaluator;
import org.processmining.plugins.etm.fitness.metrics.PrecisionEscEdges;
import org.processmining.plugins.etm.fitness.metrics.SimplicityUselessNodes;
import org.processmining.plugins.etm.logging.EvolutionLogger;
import org.processmining.plugins.etm.logging.StatisticsLogger;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.mutation.GuidedTreeMutationCoordinator;
import org.processmining.plugins.etm.mutation.TreeCrossover;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;
import org.processmining.plugins.etm.mutation.TreeMutationCoordinator;
import org.processmining.plugins.etm.mutation.mutators.AddNodeRandom;
import org.processmining.plugins.etm.mutation.mutators.ConfigurationMutator;
import org.processmining.plugins.etm.mutation.mutators.InsertActivityGuided;
import org.processmining.plugins.etm.mutation.mutators.MutateOperatorTypeGuided;
import org.processmining.plugins.etm.mutation.mutators.MutateSingleNodeRandom;
import org.processmining.plugins.etm.mutation.mutators.NormalizationMutation;
import org.processmining.plugins.etm.mutation.mutators.RemoveActivityGuided;
import org.processmining.plugins.etm.mutation.mutators.RemoveSubtreeRandom;
import org.processmining.plugins.etm.mutation.mutators.RemoveUselessNodes;
import org.processmining.plugins.etm.mutation.mutators.ReplaceTreeMutation;
import org.processmining.plugins.etm.termination.ProMCancelTerminationCondition;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.TerminationCondition;
import org.uncommons.watchmaker.framework.selection.SigmaScaling;

import nl.tue.astar.AStarThread.Canceller;

public class ETMParamFactory {

	//improvement: remove all specific weights, require map TreeFitnessInfo -> weight, add all 
	// info class instances :) (they all require nothing or CentralReg)
	// move spread out code to this class (CLI, thesis processor, ...)
	// work on ETMParamAbstract as much as possible!

	/**
	 * Standard population size for ETM Parameter objects
	 */
	public static final int STD_POPSIZE = 20;
	/**
	 * Standard elite size for ETM Parameter objects
	 */
	public static final int STD_ELITE_COUNT = 5;
	/**
	 * Standard amount of trees that are created at random in each generation
	 */
	public static final int STD_RANDOMTREES_NR = 2;
	/**
	 * Standard configuration alpha for
	 * {@link org.processmining.plugins.etm.fitness.metrics.ConfigurationFitness}
	 */
	public static final double STD_CONFIGURATION_ALPHA = 0.1;
	/**
	 * Standard crossover chance
	 */
	public static final double STD_CROSSOVER_CHANCE = 0.25;
	/**
	 * Standard chance of applying random (versus guided) mutation
	 */
	public static final double STD_MUTATION_RANDOM_CHANCE = 0.25;
	/**
	 * Standard setting whether duplicate trees should be prevented
	 */
	public static final boolean STD_PREVENT_DUPLICATES = true;
	/**
	 * Standard limit on number of generations
	 */
	public static final int STD_MAXIMUM_NR_GENERATIONS = 1000;
	/**
	 * Standard target overall fitness
	 */
	public static final double STD_TARGET_FITNESS = 1.0;
	/**
	 * Standard weight for the replay fitness quality dimension
	 */
	public static final double STD_REPLAYFITNESS_WEIGHT = 10;
	/**
	 * Standard setting for the maximum allowed fitness value
	 */
	public static final double STD_REPLAYFITNESS_MAXF = 0;
	/**
	 * Standard timeout for aligning a single trace
	 */
	public static final double STD_REPLAYFITNESS_MAXTIME = 1000;
	/**
	 * Standard weight for the precision quality dimension
	 */
	public static final double STD_PRECISION_WEIGHT = 5;
	/**
	 * Standard weight for the generalization quality dimension
	 */
	public static final double STD_GENERALIZATION_WEIGHT = 1;
	/**
	 * Standard weight for the simplicity quality dimension
	 */
	public static final double STD_SIMPLICITY_WEIGHT = 1;

	/**
	 * Standard XEventClass classifier
	 */
	public static final XEventClassifier STD_XEVENTCLASSIFIER = XLogInfoImpl.NAME_CLASSIFIER;

	/**
	 * Constructs a standardized instance of the parameter object using all
	 * public constants defined in this class.
	 * 
	 * @param eventlog
	 * @return
	 */
	public static ETMParam buildStandardParam(XLog eventlog) {
		return buildStandardParam(eventlog, null);
	}

	/**
	 * Constructs a standardized instance of the parameter object using all
	 * public constants defined in this class.
	 * 
	 * @param eventlog
	 * @param context
	 * @return
	 */
	public static ETMParam buildStandardParam(XLog eventlog, PluginContext context) {
		return buildParam(eventlog, context, STD_POPSIZE, STD_ELITE_COUNT, STD_RANDOMTREES_NR, STD_CROSSOVER_CHANCE,
				STD_MUTATION_RANDOM_CHANCE, STD_PREVENT_DUPLICATES, STD_MAXIMUM_NR_GENERATIONS, STD_TARGET_FITNESS,
				STD_REPLAYFITNESS_WEIGHT, STD_REPLAYFITNESS_MAXF, STD_REPLAYFITNESS_MAXTIME, STD_PRECISION_WEIGHT,
				STD_GENERALIZATION_WEIGHT, STD_SIMPLICITY_WEIGHT);
	}

	public static ETMParam buildStandardParamConfigurable(UIPluginContext context, XLog[] logs) {
		return buildParamConfigurable(logs, context, STD_XEVENTCLASSIFIER, STD_POPSIZE, STD_ELITE_COUNT,
				STD_RANDOMTREES_NR, STD_CROSSOVER_CHANCE, STD_MUTATION_RANDOM_CHANCE, STD_PREVENT_DUPLICATES,
				STD_MAXIMUM_NR_GENERATIONS, STD_TARGET_FITNESS, STD_REPLAYFITNESS_WEIGHT, STD_REPLAYFITNESS_MAXF,
				STD_REPLAYFITNESS_MAXTIME, STD_PRECISION_WEIGHT, STD_GENERALIZATION_WEIGHT, STD_SIMPLICITY_WEIGHT,
				STD_CONFIGURATION_ALPHA, null);
	}

	private static ETMParam buildParamConfigurable(XLog[] logs, UIPluginContext context,
			XEventClassifier eventClassifier, int popSize, int eliteSize, int nrRandomTrees, double crossOverChance,
			double chanceOfRandomMutation, boolean preventDuplicates, int maxGen, double targetFitness,
			double frWeight, double fitnessLimit, double maxFTime, double peWeight, double geWeight, double sdWeight,
			double alpha, String loggingPath) {

		Random rng = ETMParamAbstract.createRNG();
		CentralRegistryConfigurable registry = new CentralRegistryConfigurable(context, eventClassifier, rng, logs);

		OverallFitness[] oFitnesses = new OverallFitness[logs.length];
		for (int l = 0; l < logs.length; l++) {
			oFitnesses[l] = createStandardOverallFitness(registry.getRegistry(l));
		}

		ConfigurationFitness cFitness = new ConfigurationFitness(registry, alpha, false, oFitnesses);

		//Wrap in a multithreaded fitness evaluator
		int nrThreads = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);

		//param.setMaxThreads(Math.max(Runtime.getRuntime().availableProcessors() / 2, 1));
		MultiThreadedFitnessEvaluator mfitness = new MultiThreadedFitnessEvaluator(registry, cFitness, nrThreads);

		//Evolutionary Operators
		ArrayList<EvolutionaryOperator<ProbProcessArrayTree>> evolutionaryOperators = new ArrayList<EvolutionaryOperator<ProbProcessArrayTree>>();
		evolutionaryOperators.add(new TreeCrossover<ProbProcessArrayTree>(1, new Probability(crossOverChance),registry));
		LinkedHashMap<TreeMutationAbstract, Double> smartMutators = new LinkedHashMap<TreeMutationAbstract, Double>();
		smartMutators.put(new InsertActivityGuided(registry), 1.);
		// Target claims disabled smartMutators.put(new MutateLeafClassGuided(registry), 1.);
		smartMutators.put(new MutateOperatorTypeGuided(registry), 1.);
		smartMutators.put(new RemoveActivityGuided(registry), 1.);

		smartMutators.put(new RemoveUselessNodes(registry), 1.);

		LinkedHashMap<TreeMutationAbstract, Double> dumbMutators = new LinkedHashMap<TreeMutationAbstract, Double>();
		dumbMutators.put(new AddNodeRandom(registry), 1.);
		dumbMutators.put(new MutateSingleNodeRandom(registry), 1.);
		dumbMutators.put(new RemoveSubtreeRandom(registry), 1.);
		dumbMutators.put(new NormalizationMutation(registry), 1.);
		dumbMutators.put(new ReplaceTreeMutation(registry), 1.);
		dumbMutators.put(new ConfigurationMutator(registry), 10.); //FIXME IS NEVER CALLED... :(
		TreeMutationCoordinator dumbCoordinator = new TreeMutationCoordinator(dumbMutators, preventDuplicates);

		evolutionaryOperators.add(new GuidedTreeMutationCoordinator(registry, chanceOfRandomMutation,
				preventDuplicates, smartMutators, dumbCoordinator));

		ETMParamConfigurable param = new ETMParamConfigurable(registry, cFitness, evolutionaryOperators, popSize,
				eliteSize);

		/*
		 * Now prepare everything for the minimalistic constructor
		 */
		param.setRng(rng);

		/*
		 * If we have a context we can do GUI cancellation
		 */
		Canceller canceller;
		if (context != null) {
			canceller = param.addTerminationConditionProMCancellation(context);
		} else {
			canceller = ProMCancelTerminationCondition.buildDummyCanceller();
		}

		param.setSelectionStrategy(new SigmaScaling());

		/*
		 * Instantiate using default constructor and apply additional settings
		 */
		param.setPopulationSize(popSize);
		param.setEliteCount(eliteSize);
		param.addTerminationConditionMaxGen(maxGen);
		param.addTerminationConditionTargetFitness(targetFitness, param.getFitnessEvaluator().isNatural());

		//Add a GUI cancellation listener, evolution observer and set the progress
		if (context != null) {
			param.addEvolutionObserver(new EvolutionLogger<ProbProcessArrayTree>(context, registry, false));
			//ProM termination condition has already been added when the canceller obj. was instantiated
			//param.addTerminationCondition(new ProMCancelTerminationCondition(canceller));
			context.getProgress().setMaximum(maxGen + 2);
		}
		if (loggingPath != null) {
			param.addEvolutionObserver(new StatisticsLogger(param));
		}

		param.setFactory(new TreeFactoryCoordinator(registry));
		param.setMaxThreads(nrThreads);

		return param;
	}

	public static ETMParam buildParam(XLog eventlog, PluginContext context, int popSize, int eliteSize,
			int nrRandomTrees, double crossOverChance, double chanceOfRandomMutation, boolean preventDuplicates,
			int maxGen, double targetFitness, double frWeight, double maxF, double maxFTime, double peWeight,
			double geWeight, double sdWeight) {
		return buildParam(eventlog, context, popSize, eliteSize, nrRandomTrees, crossOverChance,
				chanceOfRandomMutation, preventDuplicates, maxGen, targetFitness, frWeight, maxF, maxFTime, peWeight,
				geWeight, sdWeight, null, 0);
	}

	/**
	 * Instantiates a parameter instance while requesting only 'high level'
	 * information and creating the most common termination conditions, fitness
	 * evaluator(s), evolutionary operators and observers. Will even connect to
	 * the context for cancellation input and progress and message output. It is
	 * however highly recommended to only use this method for quick
	 * experimentation. For 'professional' usage
	 * {@link ETMParam#ETMParam(TreeFactory, TreeFitnessAbstract, List, int, int)}
	 * (and related constructors) are recommended which give you all the
	 * freedom, and responsibility, to set everything up correctly.
	 * 
	 * Overall note: fitness values are non-natural meaning 0 is perfect, 1 is
	 * maximum drama.
	 * 
	 * @param eventlog
	 *            XLog the event log to work on
	 * @param context
	 *            PluginContext The ProM plugin context to listen to for
	 *            cancellation and output messages to. Is allowed to be NULL and
	 *            will then be ignored.
	 * @param popSize
	 *            int the total number of candidates to maintain and evolve in
	 *            each generation. RECOMMENDATION: 100 or 200
	 * @param eliteSize
	 *            int The portion of the population to keep unchanged to prevent
	 *            fall-backs in fitness. RECOMMENDATION: ~25% of population size
	 * @param nrRandomTrees
	 *            int Number of worst trees to be replaced by random trees.
	 *            Might reduce performance for larger event logs, recommendation
	 *            therefore is to keep this value low or even 0. RECOMMENDATION:
	 *            0
	 * @param crossOverChance
	 *            double The chance that crossover will be applied to two trees.
	 *            The total number of applications of crossover in a generation
	 *            will be ((popSize - eliteCount) - 1) * crossOverChance. So far
	 *            experiments have showed that crossover is not beneficial for
	 *            Process Trees so keep this low (< 0.1) or even 0.
	 *            RECOMMENDATION: 0.01
	 * @param chanceOfRandomMutation
	 *            double The chance that for each tree a random mutation,
	 *            instead of a guided(=smart) one is applied. SEtting this to 1
	 *            means only ranodm mutations are applied, which will in general
	 *            mean that more generations are necessary to find a good tree.
	 *            It is not recommended to set it to 0 since smart mutations are
	 *            not always the best thing to do. RECOMMENDATION: 0.25
	 * @param maxGen
	 *            int the maximum number of generations to run and then stop at.
	 *            RECOMMENDATION: 100
	 * @param targetFitness
	 *            double The overall weighted fitness value to stop at, or
	 *            below. RECOMMENDATION: 0
	 * @param frWeight
	 *            double The weight of the Fitness Replay metric among all four
	 *            fitness metrics. RECOMMENDATION: 10
	 * @param fitnessLimit
	 *            double The maximum replay fitness value allowed. If the
	 *            alignment calculation is shown to exceed this value
	 *            calculations are stopped. This can be usefull to improve
	 *            performance. However, setting this value too strict might
	 *            prevent the algorithm from finding trees that will become good
	 *            eventually. RECOMMENDATION: 0.75
	 * @param maxFTime
	 *            double. The maximum time in seconds that the alignment is
	 *            allowed to take to align a single trace on the process model.
	 *            RECOMMENDATION: 10
	 * @param peWeight
	 *            double The weight of the Precision metric among all four
	 *            fitness metrics. RECOMMENDATION: 5
	 * @param geWeight
	 *            double The weight of the Generalization metric among all four
	 *            fitness metrics. RECOMMENDATION: 1
	 * @param sdWeight
	 *            double The weight of the Simplicity metric among all four
	 *            fitness metrics. RECOMMENDATION: 1
	 * @return ETMParam object with these parameters applied.
	 */
	public static ETMParam buildParam(XLog eventlog, final PluginContext context, int popSize, int eliteSize,
			int nrRandomTrees, double crossOverChance, double chanceOfRandomMutation, boolean preventDuplicates,
			int maxGen, double targetFitness, double frWeight, double fitnessLimit, double maxFTime, double peWeight,
			double geWeight, double sdWeight, ProbProcessArrayTree[] seed, double simWeight) {

		ETMParam param = new ETMParam();
		ETMParamFactory.<ProbProcessArrayTree>buildParamAbstract(param, eventlog, context, popSize, eliteSize, nrRandomTrees,
				crossOverChance, chanceOfRandomMutation, preventDuplicates, maxGen, targetFitness, frWeight,
				fitnessLimit, maxFTime, peWeight, geWeight, sdWeight, seed, simWeight, null);
		return param;
	}

	private static <R> void buildParamAbstract(ETMParamAbstract<R,ProbProcessArrayTree> param, XLog eventlog, final PluginContext context,
			int popSize, int eliteSize, int nrRandomTrees, double crossOverChance, double chanceOfRandomMutation,
			boolean preventDuplicates, int maxGen, double targetFitness, double frWeight, double fitnessLimit,
			double maxFTime, double peWeight, double geWeight, double sdWeight, ProbProcessArrayTree[] seed, double simWeight,
			String loggingPath) {
		//FIXME make a call to a generic setup method or something to allow re-use and correct instantiation

		/*
		 * Now prepare everything for the minimalistic constructor
		 */
		Random rng = ETMParamAbstract.createRNG();
		param.setRng(rng);

		/*
		 * If we have a context we can do GUI cancellation
		 */
		Canceller canceller;
		if (context != null) {
			canceller = param.addTerminationConditionProMCancellation(context);
		} else {
			canceller = ProMCancelTerminationCondition.buildDummyCanceller();
		}

		CentralRegistry centralRegistry = new CentralRegistry(eventlog, rng);
		param.setCentralRegistry(centralRegistry);

		/*
		 * There are 3 places for multi-threading. Leaving it up to the
		 * Watchmaker framework is not a good idea since it will re-instantiate
		 * our fitness evaluators each time which takes time. The other option
		 * is within one replay instance but this creates a new thread per
		 * trace, which is not useful for small traces. Therefore, parallelise
		 * the fitness evaluations of trees, for which we use our own multi
		 * threading evaluator wrapper.
		 */

		//Weighted fitness
		// FitnessReplay fr = new FitnessReplay(centralRegistry, canceller, fitnessLimit, maxFTime);
		PrecisionEscEdges pe = new PrecisionEscEdges(centralRegistry);
		@SuppressWarnings("unused")
		//PrecisionOperatorCosts pc = new PrecisionOperatorCosts();
		//PrecisionEscEdgesImproved pi = new PrecisionEscEdgesImproved(centralRegistry);
		Generalization ge = new Generalization(centralRegistry);
		//TODO testing new Simplicity metric since n-ary trees ruin the leafs<->#nodes relation
		//SimplicityDuplMissingAct sd = new SimplicityDuplMissingAct(centralRegistry.getLogInfo());
		SimplicityUselessNodes su = new SimplicityUselessNodes();
		//SimplicityMixed sm = new SimplicityMixed();

		LinkedHashMap<TreeFitnessAbstract, Double> weightedFitnessAlg = new LinkedHashMap<TreeFitnessAbstract, Double>();
//		if (frWeight >= 0)
//			weightedFitnessAlg.put(fr, frWeight);
		if (peWeight >= 0)
			weightedFitnessAlg.put(pe, peWeight);
		//if (peWeight >= 0)
		//	weightedFitnessAlg.put(pi, peWeight);
		if (geWeight >= 0)
			weightedFitnessAlg.put(ge, geWeight);
		if (sdWeight >= 0)
			weightedFitnessAlg.put(su, sdWeight);
		if (seed != null && seed.length > 0 && seed[0] != null && simWeight >= 0) {
			//FIXME distinguish automatically when to use absolute(Pareto) or relative (normal)
			param.setSeed(seed);
			//weightedFitnessAlg.put(new EditDistanceWrapperRTEDRelative(seed), 0.0001);
			weightedFitnessAlg.put(new EditDistanceWrapperRTEDAbsolute(seed), 0.0001);
		}

		OverallFitness of = new OverallFitness(centralRegistry, weightedFitnessAlg);

		//Wrap the evaluator with a multi threading evaluator
		param.setMaxThreads(Math.max(Runtime.getRuntime().availableProcessors() / 2, 1));
		param.setFitnessEvaluator(new MultiThreadedFitnessEvaluator(centralRegistry, of, param.getMaxThreads()));

		//Evolutionary Operators
		ArrayList<EvolutionaryOperator<ProbProcessArrayTree>> evolutionaryOperators = new ArrayList<EvolutionaryOperator<ProbProcessArrayTree>>();
		evolutionaryOperators.add(new TreeCrossover<ProbProcessArrayTree>(1, new Probability(crossOverChance),centralRegistry));
		LinkedHashMap<TreeMutationAbstract, Double> smartMutators = new LinkedHashMap<TreeMutationAbstract, Double>();
		//		smartMutators.put(new InsertActivityGuided(centralRegistry), 1.);
		//		smartMutators.put(new MutateLeafClassGuided(centralRegistry), 1.);
		//		smartMutators.put(new MutateOperatorTypeGuided(centralRegistry), 1.);
		//		smartMutators.put(new RemoveActivityGuided(centralRegistry), 1.);
		smartMutators.put(new RemoveUselessNodes(centralRegistry), 1.);

		LinkedHashMap<TreeMutationAbstract, Double> dumbMutators = new LinkedHashMap<TreeMutationAbstract, Double>();
		dumbMutators.put(new AddNodeRandom(centralRegistry), 1.);
		dumbMutators.put(new MutateSingleNodeRandom(centralRegistry), 1.);
		dumbMutators.put(new RemoveSubtreeRandom(centralRegistry), 1.);
		dumbMutators.put(new NormalizationMutation(centralRegistry), 1.);
		dumbMutators.put(new ReplaceTreeMutation(centralRegistry), 1.);
		TreeMutationCoordinator dumbCoordinator = new TreeMutationCoordinator(dumbMutators, preventDuplicates);

		evolutionaryOperators.add(new GuidedTreeMutationCoordinator(centralRegistry, chanceOfRandomMutation,
				preventDuplicates, smartMutators, dumbCoordinator));
		param.setEvolutionaryOperators(evolutionaryOperators);

		param.setSelectionStrategy(new SigmaScaling());
		//param.setSelectionStrategy(new TournamentSelection(new Probability(0.75)));

		/*
		 * Instantiate using default constructor and apply additional settings
		 */
		param.setPopulationSize(popSize);
		param.setEliteCount(eliteSize);
		param.addTerminationConditionMaxGen(maxGen);
		param.addTerminationConditionTargetFitness(targetFitness, param.getFitnessEvaluator().isNatural());

		//Add a GUI cancellation listener, evolution observer and set the progress
		if (context != null) {
			param.addEvolutionObserver(new EvolutionLogger<ProbProcessArrayTree>(context, centralRegistry, false));
			//ProM termination condition has already been added when the canceller obj. was instantiated
			//param.addTerminationCondition(new ProMCancelTerminationCondition(canceller));
			context.getProgress().setMaximum(maxGen + 2);
		}
		if (loggingPath != null) {
			param.addEvolutionObserver(new StatisticsLogger(param));
		}

		param.setFactory(new TreeFactoryCoordinator(centralRegistry));
		param.setMaxThreads(Math.max(Runtime.getRuntime().availableProcessors() / 2, 1));
	}

	public static ETMParamPareto buildETMParamPareto(XLog eventlog, PluginContext context, int popSize, int eliteSize,
			int nrRandomTrees, double crossOverChance, double chanceOfRandomMutation, boolean preventDuplicates,
			int maxGen, double targetFitness, double frWeight, double maxF, double maxFTime, double peWeight,
			double geWeight, double sdWeight) {
		return buildETMParamPareto(eventlog, context, popSize, eliteSize, nrRandomTrees, crossOverChance,
				chanceOfRandomMutation, preventDuplicates, maxGen, targetFitness, frWeight, maxF, maxFTime, peWeight,
				geWeight, sdWeight, null, 0);
	}

	public static ETMParamPareto buildETMParamPareto(XLog eventlog, PluginContext context, int popSize, int eliteSize,
			int nrRandomTrees, double crossOverChance, double chanceOfRandomMutation, boolean preventDuplicates,
			int maxGen, double targetFitness, double frWeight, double maxF, double maxFTime, double peWeight,
			double geWeight, double sdWeight, ProbProcessArrayTree[] seed, double simWeight) {

		ETMParamPareto param = new ETMParamPareto();
		ETMParamFactory.<ParetoFront>buildParamAbstract(param, eventlog, context, popSize, eliteSize, nrRandomTrees,
				crossOverChance, chanceOfRandomMutation, preventDuplicates, maxGen, targetFitness, frWeight, maxF,
				maxFTime, peWeight, geWeight, sdWeight, seed, simWeight, null);
		param.setParetoFitnessEvaluator(new ParetoFitnessEvaluator(param.getCentralRegistry()));
		return param;
	}

	/**
	 * Returns an instance of the termination condition, if present in the
	 * provided param object. Otherwise returns null
	 * 
	 * @param param
	 * @param condition
	 */
	public static TerminationCondition getTerminationCondition(ETMParamAbstract param,
			Class<? extends TerminationCondition> condition) {
		List<TerminationCondition> conditions = param.getTerminationConditions();

		for (TerminationCondition cond : conditions) {
			if (condition.getClass().equals(cond.getClass())) {
				return cond;
			}
		}

		return null;
	}

	/**
	 * Add the given termination condition to the provided param object and
	 * makes sure that no other instance of this particular class exists in the
	 * list of termination conditions.
	 * 
	 * @param param
	 * @param condition
	 */
	public static void addOrReplaceTerminationCondition(ETMParamAbstract params, TerminationCondition condition) {
		//First remove all that exist
		removeTerminationConditionIfExists(params, condition.getClass());

		params.addTerminationCondition(condition);
	}

	public static void removeTerminationConditionIfExists(ETMParamAbstract param,
			Class<? extends TerminationCondition> clazz) {
		//Remove all instances
		Iterator<TerminationCondition> it = param.getTerminationConditions().iterator();
		while (it.hasNext()) {
			TerminationCondition cond = it.next();
			if (cond.getClass().equals(clazz)) {
				it.remove();
			}
		}
	}

	public static TerminationCondition constructProMCancelTerminationCondition(final PluginContext context) {
		return new ProMCancelTerminationCondition(context);
	}

	/**
	 * Creates a standard overall fitness instance, containing evaluators for
	 * the dimensions Fitness, Precision, Generalization and Simplicity, using
	 * default weights (which might or might not be good for your purpose!!!)
	 * 
	 * @param centralRegistry
	 * @return
	 */
	public static OverallFitness createStandardOverallFitness(CentralRegistry centralRegistry) {
		OverallFitness oF = new OverallFitness(centralRegistry);

//		FitnessReplay fr = new FitnessReplay(centralRegistry,
//				ProMCancelTerminationCondition.buildCanceller(centralRegistry.getContext()));
		//max bytes is total memory - 100Mb for general use, divided over the number of threads used
		long maxBytes = (Runtime.getRuntime().maxMemory() - (100 * 1024 * 1024))
				/ Math.max(1, (Runtime.getRuntime().availableProcessors() / 2));
		//BUG in replayer, division by 0
		//fr.updateMaxBytesToUse(maxBytes);
//		fr.setDetailedAlignmentInfoEnabled(true);
		PrecisionEscEdges pe = new PrecisionEscEdges(centralRegistry);
		Generalization ge = new Generalization(centralRegistry);
		GeneralizationByFitnessReplayDeviation gd = new GeneralizationByFitnessReplayDeviation(centralRegistry);
		SimplicityUselessNodes su = new SimplicityUselessNodes();

//		oF.addEvaluator(fr, STD_REPLAYFITNESS_WEIGHT);
		oF.addEvaluator(pe, STD_PRECISION_WEIGHT);
		oF.addEvaluator(ge, STD_GENERALIZATION_WEIGHT);
		oF.addEvaluator(su, STD_SIMPLICITY_WEIGHT);

		return oF;
	}

	/**
	 * Extracts all considered dimensions from the current evaluator of the
	 * provided parameter object
	 * 
	 * @param params
	 * @return
	 */
	public static List<TreeFitnessInfo> extractConsideredDimensions(ETMParamAbstract params) {
		List<TreeFitnessInfo> allDimensions = new ArrayList<TreeFitnessInfo>();
		FitnessEvaluator mainEval = params.getFitnessEvaluator();

		OverallFitness[] oFarray = null;
		if (mainEval instanceof MultiThreadedFitnessEvaluator) {
			MultiThreadedFitnessEvaluator mtf = (MultiThreadedFitnessEvaluator) mainEval;
			oFarray = new OverallFitness[mtf.getEvaluators().length];
			for (int f = 0; f < mtf.getEvaluators().length; f++) {
				TreeFitnessAbstract eval = mtf.getEvaluators()[f];
				if (eval instanceof ConfigurationFitness) {
					ConfigurationFitness cf = (ConfigurationFitness) eval;
					allDimensions.add(cf.getInfo());
					for (TreeFitnessAbstract of : cf.getFitnessList()) {
						oFarray[f] = (OverallFitness) of;
					}
				} else if (eval instanceof OverallFitness) {
					oFarray[f] = (OverallFitness) eval;
				}
			}
		} else if (mainEval instanceof OverallFitness) {
			oFarray = new OverallFitness[1];
			oFarray[0] = (OverallFitness) mainEval;
		}

		//Add all known dimensions
		for (OverallFitness oF : oFarray) {
			for (TreeFitnessAbstract eval : oF.getEvaluators().keySet()) {
				if (!allDimensions.contains(eval.getInfo())) {
					allDimensions.add(eval.getInfo());
				}
			}
		}

		return allDimensions;

	}

}
