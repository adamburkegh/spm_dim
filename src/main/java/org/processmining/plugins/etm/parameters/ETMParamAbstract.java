package org.processmining.plugins.etm.parameters;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.live.ETMLiveListener;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.termination.ExternalTerminationCondition;
import org.processmining.plugins.etm.termination.ProMCancelTerminationCondition;
import org.processmining.watchmaker.TargetFitnessVisible;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.SecureRandomSeedGenerator;
import org.uncommons.maths.random.SeedException;
import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.TerminationCondition;
import org.uncommons.watchmaker.framework.selection.SigmaScaling;
import org.uncommons.watchmaker.framework.termination.ElapsedTime;
import org.uncommons.watchmaker.framework.termination.GenerationCount;
import org.uncommons.watchmaker.framework.termination.Stagnation;
import org.uncommons.watchmaker.framework.termination.TargetFitness;

import nl.tue.astar.AStarThread;
import nl.tue.astar.AStarThread.Canceller;

/**
 * 
 * @author jbuijs
 * 
 * @param <Tree>
 *            The type of object (usually an {@link ProbProcessArrayTree}) that is evolved
 *            in the genetic algorithm
 * @param <Tracker>
 *            The return type of the algorithm calling, f.i. an {@link ProbProcessArrayTree}
 *            itself or a
 *            {@link org.processmining.plugins.etm.model.ParetoFront} of such
 *            trees.
 */
public abstract class ETMParamAbstract<Tracker,Tree> implements ETMListenerParams<Tracker>{
	//jbuijs suggested improvement:
	// Really re-think the parameter classes, distinction ETM/Pareto is ok but 
	// 'configurable' (& live) should be generalized (otherwise way too many variants) 

	protected Random rng;

	protected CentralRegistry centralRegistry;

	protected List<TerminationCondition> terminationConditions = new ArrayList<TerminationCondition>();

	protected List<EvolutionObserver<Tree>> evolutionObservers = new ArrayList<EvolutionObserver<Tree>>();

	protected SelectionStrategy<Object> selectionStrategy;

	protected int maxThreads;

	protected List<Tree> seed = new ArrayList<Tree>();

	protected int populationSize;

	protected int eliteCount;

	/**
	 * If not null then the complete population is written to file for each
	 * generation. If the path is not existent or not writable then the
	 * algorithm will not log anything but continues working correctly.
	 */
	protected String path;

	protected List<EvolutionaryOperator<Tree>> evolutionaryOperators;

	protected FitnessEvaluator<Tree> fitnessEvaluator;

	protected CandidateFactory<Tree> factory;

	protected int logModulo;
	
	protected String runId;
	
	protected String buildVersion;
	
	protected String logFileName;
	

	/**
	 * A list of listeners that will be notified of the ETMs progress
	 */
	protected ETMLiveListener.ETMListenerList<Tracker> listeners = new ETMLiveListener.ETMListenerList<Tracker>();

	/**
	 * Protected constructor without arguments to allow extending classes to
	 * implement own constructor that should of course set all fields
	 * correctly...
	 */
	protected ETMParamAbstract() {
	}

	/**
	 * The most basic constructor for the ETM algorithm. All other parameters
	 * will be initialized with the default (e.g. multi-threaded, NO
	 * TERMINATION, no seed, no observers, etc.)
	 * 
	 * @param factory
	 *            TreeFactory to use
	 * @param terminationConditions
	 *            Termination conditions to stop execution (could include
	 *            cancellation listener)
	 * @param fitnessEvaluator
	 *            Single evaluator to produce a single fitness value for each
	 *            candidate.
	 * @param evolutionaryOperators
	 *            List of evolutionary operators which are ALL applied in each
	 *            generation on the non-elite to change them.
	 * @param populationSize
	 *            Number of candidates to consider in each generation
	 * @param eliteCount
	 *            Number of top candidates to maintain unchanged in each
	 *            generation
	 */
	public ETMParamAbstract(CentralRegistry registry, FitnessEvaluator<Tree> fitnessEvaluator,
			List<EvolutionaryOperator<Tree>> evolutionaryOperators, int populationSize, int eliteCount) {
		//FIXME make a call to a generic setup method or something to allow re-use and correct instantiation
		super();
		this.setCentralRegistry(registry);
		this.setTerminationConditions(terminationConditions);
		this.fitnessEvaluator = fitnessEvaluator;
		this.setEvolutionaryOperators(evolutionaryOperators);
		this.setPopulationSize(populationSize);
		this.setEliteCount(eliteCount);

		//Set the DEFAULTS:
		//Random generator
		this.rng = createRNG();
		//Selection strategy
		this.setSelectionStrategy(new SigmaScaling());

		this.maxThreads = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1);

		this.logModulo = Integer.MAX_VALUE;
	}

	/*-
	 * CONVENIENCE METHODS
	 */

	//TERMINATION CONDITIONS

	/**
	 * Adds a new termination condition that stops the algorithm after a maximum
	 * number of generations. If maxGen is less than 0 no condition is added.
	 * 
	 * @param maxGen
	 *            int The maximum number of generations
	 */
	public void addTerminationConditionMaxGen(int maxGen) {
		if (maxGen >= 0) {
			terminationConditions.add(new GenerationCount(maxGen));
		}
	}

	/**
	 * Adds a termination condition that stops the evolution as soon as the
	 * target fitness is reached. If {@link TargetFitness} is less than 0 no
	 * condition is added.
	 * 
	 * @param targetFitness
	 *            double Target fitness to stop at (or over)
	 * @param isNatural
	 *            boolean Whether bigger is better (TRUE) or lower is better
	 *            (FALSE)
	 */
	public void addTerminationConditionTargetFitness(double targetFitness, boolean isNatural) {
		if (targetFitness >= 0)
			terminationConditions.add(new TargetFitnessVisible(targetFitness, isNatural));
	}

	/**
	 * Add a termination condition to the ETM algorithm that listens to the ProM
	 * Cancellation button. NOTE: a separate canceller should be added to a
	 * possible replay Fitness evaluator to stop the evaluations (which take the
	 * most time) for quick cancellation response.
	 * 
	 * @param context
	 *            The context to get the canceller from
	 * @return Returns a {@link AStarThread.Canceller} object which can be used
	 *         in a future Replay Fitness evaluator
	 */
	public Canceller addTerminationConditionProMCancellation(PluginContext context) {
		return addTerminationConditionProMCancellation(context.getProgress());
	}

	/**
	 * Add a termination condition to the ETM algorithm that listens to the ProM
	 * Cancellation button. NOTE: a separate canceller should be added to a
	 * possible replay Fitness evaluator to stop the evaluations (which take the
	 * most time) for quick cancellation response.
	 * 
	 * @param progress
	 *            The progress instance to get the canceller from
	 * @return Returns a {@link AStarThread.Canceller} object which can be used
	 *         in a future Replay Fitness evaluator
	 */
	public Canceller addTerminationConditionProMCancellation(final Progress progress) {
		ProMCancelTerminationCondition promCancelTermCond = new ProMCancelTerminationCondition(progress);
		terminationConditions.add(promCancelTermCond);
		return promCancelTermCond.getCanceller();
	}

	public static Random createRNG() {
		try {
			return new MersenneTwisterRNG(new SecureRandomSeedGenerator());
		} catch (SeedException e) {
			return new Random();
		}
	}

	/*-
	 * GETTERS AND SETTERS
	 */

	/**
	 * @return the rng (random number generator), all algorithms should use this
	 *         one and not their own
	 */
	public Random getRng() {
		return rng;
	}

	/**
	 * @param rng
	 *            A random number generator. All algorithms should use this one
	 *            and not their own
	 */
	public void setRng(Random rng) {
		this.rng = rng;
	}

	/**
	 * @return the terminationConditions
	 */
	public List<TerminationCondition> getTerminationConditions() {
		return terminationConditions;
	}

	/**
	 * @param terminationConditions
	 *            A list of termination conditions that can stop the GA
	 *            algorithm. One of these should listen to cancellation messages
	 *            from the GUI.
	 */
	public void setTerminationConditions(List<TerminationCondition> terminationConditions) {
		this.terminationConditions = terminationConditions;
	}

	/**
	 * Adds a termination condition to the existing list of termination
	 * conditions that can stop the GA algorithm. One of these should listen to
	 * cancellation messages from the GUI.
	 * 
	 * @param terminationCondition
	 */
	public void addTerminationCondition(TerminationCondition terminationCondition) {
		this.terminationConditions.add(terminationCondition);
	}

	/**
	 * @return the evolutionLoggers A list of evolution loggers that report
	 *         about the progress (not really required but should be used to
	 *         update GUIs etc. of the progress)
	 */
	public List<EvolutionObserver<Tree>> getEvolutionObservers() {
		return evolutionObservers;
	}

	/**
	 * @param evolutionLoggers
	 *            The list of evolution observers that report about the progress
	 *            (not really required but should be used to update GUIs etc. of
	 *            the progress)
	 */
	public void setEvolutionObservers(List<EvolutionObserver<Tree>> evolutionObservers) {
		this.evolutionObservers = evolutionObservers;
	}

	/**
	 * Add an evolution observer to the list of evolution observers that report
	 * about the progress (not really required but should be used to update GUIs
	 * etc. of the progress)
	 * 
	 * @param evolutionObserver
	 */
	public void addEvolutionObserver(EvolutionObserver<Tree> evolutionObserver) {
		this.evolutionObservers.add(evolutionObserver);
	}

	/**
	 * @return the selectionStrategy The selection strategy used to select
	 *         candidates from the population to mutate.
	 */
	public SelectionStrategy<Object> getSelectionStrategy() {
		return selectionStrategy;
	}

	/**
	 * @param selectionStrategy
	 *            The selection strategy to use to select candidates from the
	 *            population to mutate.
	 */
	public void setSelectionStrategy(SelectionStrategy<Object> selectionStrategy) {
		this.selectionStrategy = selectionStrategy;
	}

	/**
	 * @return A list of trees from which the algorithm started (can be left
	 *         empty)
	 */
	public List<Tree> getSeed() {
		return seed;
	}

	/**
	 * @param seed
	 *            A list of trees from which the algorithm will start (can be
	 *            left empty)
	 */
	public void setSeed(List<Tree> seed) {
		this.seed = seed;
	}

	/**
	 * @param seed
	 *            An of trees from which the algorithm will start (can be left
	 *            empty)
	 */

	public void setSeed(Tree[] seed) {
		setSeed(Arrays.asList(seed));
	}

	/**
	 * @return the populationSize
	 */
	public int getPopulationSize() {
		return populationSize;
	}

	/**
	 * @param populationSize
	 *            the populationSize to set
	 */
	public void setPopulationSize(int populationSize) {
		this.populationSize = populationSize;
	}

	/**
	 * @return Number of candidates kept unchanged in a generation
	 */
	public int getEliteCount() {
		return eliteCount;
	}

	/**
	 * @param eliteCount
	 *            the eliteCount to set, the number of candidates to keep
	 *            unchanged in a generation. Will be minimally 2 and maximally
	 *            the population size - 1.
	 */
	public void setEliteCount(int eliteCount) {
		this.eliteCount = Math.max(Math.min(populationSize - 1, eliteCount), 1);
	}

	/**
	 * @return The path where even more details to than an evolution observer
	 *         gets are beeing written to. If not null then the complete
	 *         population is written to file for each generation. If the path is
	 *         not existent or not writable then the algorithm will not log
	 *         anything but continues working correctly.
	 */
	public String getPath() {
		return path;
	}

	/**
	 * @param path
	 *            The path to log even more details to than an evolution
	 *            observer gets. This contains every tree in every generation
	 *            but also some overall statistics. If not null then the
	 *            complete population is written to file for each generation. If
	 *            the path is not existent or not writable then the algorithm
	 *            will not log anything but continues working correctly.
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * @return the evolutionaryOperators through which each tree that will be
	 *         changed went.
	 */
	public List<EvolutionaryOperator<Tree>> getEvolutionaryOperators() {
		return evolutionaryOperators;
	}

	/**
	 * @param evolutionaryOperators
	 *            the evolutionaryOperators through which each tree that will be
	 *            changed will go. All evolutionary operators will be applied in
	 *            the provided sequence.
	 */
	public void setEvolutionaryOperators(List<EvolutionaryOperator<Tree>> evolutionaryOperators) {
		this.evolutionaryOperators = evolutionaryOperators;
	}

	/**
	 * @return the fitnessEvaluator The fitness evaluator used to produce a
	 *         fitness value for the candidate trees. Although this is a single
	 *         fitness evaluator, it might be a wrapper around a collection of
	 *         evaluators.
	 */
	public FitnessEvaluator<Tree> getFitnessEvaluator() {
		return fitnessEvaluator;
	}

	/**
	 * @param fitnessEvaluator
	 *            The fitness evaluator used to produce a fitness value for the
	 *            candidate trees. Although this is a single fitness evaluator,
	 *            it might be a wrapper around a collection of evaluators.
	 */
	public void setFitnessEvaluator(FitnessEvaluator<Tree> fitnessEvaluator) {
		this.fitnessEvaluator = fitnessEvaluator;
	}

	public void addTerminationConditionSteadyState(int steadyStates, boolean isNatural) {
		this.terminationConditions.add(new Stagnation(steadyStates, isNatural));
	}

	/**
	 * Terminates the algorithm after a certain amount of time. If maxDuration
	 * is less than 0 no condition is added.
	 * 
	 * @param maxDuration
	 *            Time in milliseconds after which, at the end of a generation,
	 *            the algorithm will stop.
	 */
	public void addTerminationConditionMaxDuration(long maxDuration) {
		if (maxDuration >= 0)
			this.terminationConditions.add(new ElapsedTime(maxDuration));
	}

	public ExternalTerminationCondition addTerminationConditionExternal() {
		ExternalTerminationCondition termCondition = new ExternalTerminationCondition();
		this.terminationConditions.add(termCondition);
		return termCondition;
	}

	/**
	 * Returns the external termination condition, if present, otherwise returns
	 * null
	 * 
	 * @return
	 */
	public ExternalTerminationCondition getTerminationConditionExternal() {
		for (TerminationCondition condition : terminationConditions) {
			if (condition instanceof ExternalTerminationCondition) {
				return (ExternalTerminationCondition) condition;
			}
		}

		return null;
	}

	/**
	 * @return the centralRegistry
	 */
	public CentralRegistry getCentralRegistry() {
		return centralRegistry;
	}

	/**
	 * @param centralRegistry
	 *            the centralRegistry to set
	 */
	public void setCentralRegistry(CentralRegistry centralRegistry) {
		this.centralRegistry = centralRegistry;
	}

	public CandidateFactory<Tree> getFactory() {
		return factory;
	}

	public void setFactory(CandidateFactory<Tree> factory) {
		this.factory = factory;
	}

	/**
	 * Returns the number of evaluation threads (main processing part) used by
	 * the algorithm. By default this is the number or processors detected
	 * divided by 2 assuming hyper-threading is enabled. Experiments showed that
	 * using hyper-threading cores is not beneficial for our algorithm
	 * 
	 * @return the maxCores
	 */
	public int getMaxThreads() {
		return maxThreads;
	}

	/**
	 * Update the maximum number of simultaneous evaluation threads to use. It
	 * is advised to set this to the number of non hyper-threading cores
	 * available to Java. Having more than one thread per core might negatively
	 * impact performance. NOTE: updating the maxThreads has NO effect when the
	 * ETM is already running!!!
	 * 
	 * @param maxCores
	 *            the maxCores to set
	 */
	public void setMaxThreads(int maxCores) {
		this.maxThreads = maxCores;
	}

	/**
	 * After how many generations a new population log should be written to the
	 * logging path.
	 * 
	 * @return
	 */
	public int getLogModulo() {
		return logModulo;
	}

	/**
	 * Set after how many generations a new population log should be written to
	 * the logging path.
	 * 
	 * @param logModulo
	 */
	public void setLogModulo(int logModulo) {
		this.logModulo = logModulo;
	}

	/**
	 * @return the listeners
	 */
	public ETMLiveListener.ETMListenerList<Tracker> getListeners() {
		return listeners;
	}

	/**
	 * Returns the {@link TerminationCondition}s as an array as required by the
	 * Watchmaker framework...
	 * 
	 * @return
	 */
	public TerminationCondition[] getTerminationConditionsAsArray() {
		//Prepare the termination conditions as an array for the evolve method which has a varargs on it
		TerminationCondition[] terminationConditionsArray = new TerminationCondition[getTerminationConditions().size()];

		return getTerminationConditions().toArray(terminationConditionsArray);
	}

	public String getRunId() {
		return runId;
	}

	public void setRunId(String runId) {
		this.runId = runId;
	}

	public String getBuildVersion() {
		return buildVersion;
	}

	public void setBuildVersion(String buildVersion) {
		this.buildVersion = buildVersion;
	}

	public String getLogFileName() {
		return logFileName;
	}

	public void setLogFileName(String logFileName) {
		this.logFileName = logFileName;
	}
	
	
}
