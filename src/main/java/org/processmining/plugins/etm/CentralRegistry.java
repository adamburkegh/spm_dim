package org.processmining.plugins.etm;

import gnu.trove.iterator.TIntIterator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import nl.tue.astar.Trace;

import org.apache.commons.collections15.map.LRUMap;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.etm.fitness.TreeComparatorByFitness;
import org.processmining.plugins.etm.fitness.TreeFitness;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.ppt.NAryTreeHistory;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.utils.LogUtils;

/**
 * The Central Registry is the central class that keeps track of (or contains a
 * reference to) the event log, the event log info, the known event classes and
 * the tree fitness values.
 * 
 * @author jbuijs
 * 
 */
public class CentralRegistry {

	//jbuijs improvement: constructor is called with new Random() too often, use ETMParam.createRNG()

	/**
	 * An instance of the event classes separate from the log info since we
	 * might want to pretend we have less classes (e.g. consider only the top X
	 * most used eventclasses for discovery).
	 */
	protected XEventClasses eventClasses;
	private final XLog log;
	private XLogInfo logInfo;

	private AStarAlgorithm aStarAlgorithm;

	//USING Arya's defaults (changed from 1:1 on 17-3-2013. Old results will be different!!!) 
	public static final int MMcost = 2;
	public static final int MLcost = 5;
	public static final int CONFIDENCE_LEVEL_LIMIT = 10;

	// It should be 4 times the population size. size = 4*populationSize (each iteration)
	// for now, popSize = 20, 
	private static final int HISTORY_CACHE_SIZE = 80;
	private static final int FITNESS_CACHE_SIZE = 100;

	private Map<XEventClass, Integer> logCosts;
	private int estimatedMinLogCost;
	private double minLogCost;

	protected final Random rng;

	protected transient final PluginContext context;

	/**
	 * Keeps track of the current generation number
	 */
	private int generation = 0;

	//The cache should be transient otherwise we get concurrentModification exceptions during the live view...
	private transient LRUMap<ProbProcessArrayTree, TreeFitness> treeFitnessCache;

	//Watch out with this cache it is HIGHLY dependent of the population size and it grows A LOT.
	//Reaching the max size can be a problem -> lose bonds between family trees.
	// Solution: clean the cache at each iteration
	private transient LRUMap<ProbProcessArrayTree, List<NAryTreeHistory>> treeHistoryCache;

	private AStarAlgorithm emptyAStarAlgorithm;

	/**
	 * Creates a new tree factory that creates new nodes given a list of event
	 * classes for leafs and the probability of introducing intermediate
	 * function nodes
	 * 
	 * @param maxNodes
	 *            Maximum number of nodes of the tree
	 * @param eventClasses
	 *            Candidates for leaf nodes to choose from
	 * @param functionProbability
	 *            Probability of introducing function nodes even though we did
	 *            not reach the maxNodes limit
	 */
	public CentralRegistry(XLog log, Random rng) {
		this(log, XLogInfoImpl.STANDARD_CLASSIFIER, rng);
	}

	/**
	 * Create new treeFactory instance from the event log and eventClassifier
	 * 
	 * @param log
	 * @param eventClassifier
	 */
	public CentralRegistry(XLog log, XEventClassifier eventClassifier, Random rng) {
		this(log, eventClassifier, rng, XLogInfoFactory.createLogInfo(log, eventClassifier));
	}

	public CentralRegistry(XLog log, XEventClassifier eventClassifier, Random rng, XLogInfo logInfo) {
		this(null, log, eventClassifier, rng, logInfo);
	}

	public CentralRegistry(PluginContext context, XLog log, Random rng) {
		this(context, log, XLogInfoImpl.STANDARD_CLASSIFIER, rng, XLogInfoFactory.createLogInfo(log,
				XLogInfoImpl.STANDARD_CLASSIFIER));
	}

	public CentralRegistry(PluginContext context, XLog log, XEventClassifier eventClassifier, Random rng) {
		this(context, log, XLogInfoImpl.STANDARD_CLASSIFIER, rng, XLogInfoFactory.createLogInfo(log, eventClassifier));
	}

	public CentralRegistry(PluginContext context, XLog log, XEventClassifier eventClassifier, Random rng,
			XLogInfo logInfo) {
		if (log == null) {
			throw new IllegalArgumentException("The event log can not be empty");
		}

		if (eventClassifier == null) {
			throw new IllegalArgumentException("The event classifier can not be empty");
		}

		this.context = context;

		this.log = log;

		this.rng = rng;

		this.logInfo = logInfo;

		//CLONE the XEventclasses since we migth want to change it
		eventClasses = LogUtils.deepCloneXEventClasses(logInfo.getEventClasses(), log, logInfo);

		treeFitnessCache = new LRUMap<ProbProcessArrayTree, TreeFitness>(FITNESS_CACHE_SIZE);

		// ArrayList: access nth element in O(1), only insertions (no deletions). Maybe using LRUMap for size limits.
		treeHistoryCache = new LRUMap<ProbProcessArrayTree, List<NAryTreeHistory>>(HISTORY_CACHE_SIZE);

		updateLogDerived();
	}

	public XLog getLog() {
		return log;
	}

	/**
	 * Returns a single randomly selected event class
	 * 
	 * @return
	 */
	public XEventClass getRandomEventClass(Random rng) {
		return getRandomEventClass(false, rng);
	}

	/**
	 * Returns a single randomly selected event class
	 * 
	 * @return
	 */
	public XEventClass getRandomEventClass(boolean allowTau, Random rng) {
		if (!allowTau) {
			//SELECT a random event class
			return eventClasses.getByIndex(rng.nextInt(eventClasses.size()));
		} else {
			//Select a random event class or NULL
			int randomInt = rng.nextInt(eventClasses.size() + 1);
			if (randomInt == eventClasses.size())
				//Return null if we selected it (as the fictitious last element of the eventClasses list)
				return null;
			else
				return eventClasses.getByIndex(randomInt);
		}
	}

	public XLogInfo getLogInfo() {
		return logInfo;
	}

	/**
	 * Returns the internally used int to represent the given XEventClass
	 * 
	 * @param eventClass
	 * @return
	 */
	public short getEventClassID(XEventClass eventClass) {
		return (short) getEventClassID(eventClass.toString());
	}

	/**
	 * Returns the internally used int to represent the XEventClass identified
	 * by the given String
	 * 
	 * @param classIdentity
	 * @return
	 */
	public int getEventClassID(String classIdentity) {
		return eventClasses.getByIdentity(classIdentity).getIndex();
	}

	/**
	 * Returns the XEventClass that is represented by the given index of null if
	 * index < 0 or >= size
	 * 
	 * @param id
	 * @return
	 */
	public XEventClass getEventClassByID(int id) {
		if (id < 0 || id >= eventClasses.size())
			return null;
		return eventClasses.getByIndex(id);
	}

	public int nrEventClasses() {
		return eventClasses.size();
	}

	public XEventClasses getEventClasses() {
		return eventClasses;
	}

	public synchronized double getOverallFitness(ProbProcessArrayTree tree) {
		return getFitness(tree).getOverallFitnessValue();
	}

	public synchronized TreeFitness getFitness(ProbProcessArrayTree tree) {
		if (!treeFitnessCache.containsKey(tree)) {
			treeFitnessCache.put(tree, new TreeFitness(tree.size()));
		}

		return treeFitnessCache.get(tree);
	}

	public synchronized void restartFitness(ProbProcessArrayTree tree) {
		treeFitnessCache.put(tree, new TreeFitness(tree.size()));
	}

	/**
	 * Function that checks whether a fitness is known AND PROPERLY SET for this
	 * tree.
	 * 
	 * @param tree
	 * @return
	 */
	public synchronized boolean isFitnessKnown(ProbProcessArrayTree tree) {
		return treeFitnessCache.containsKey(tree) && getFitness(tree).isSet();
	}

	/**
	 * Function that checks whether there is already a fitness object for this
	 * tree in the cache. It ignores whether it is already set (use
	 * isFitnessKnown for that!)
	 * 
	 * @param tree
	 * @return
	 */
	public synchronized boolean containsFitness(ProbProcessArrayTree tree) {
		return treeFitnessCache.containsKey(tree);
	}

	public synchronized void clearFitnessCache() {
		treeFitnessCache.clear();
	}

	/**
	 * Instructs the internal fitness cache that it should contain at least the
	 * specified number of candidates. Mainly used by the Pareto front to make
	 * sure that the whole front + some 'real' cache can be kept. The size of
	 * the new cache will be 1.5 times the requested size, if this is more than
	 * the current size.
	 * 
	 * @param minimal
	 * @return boolean indicating whether the cache has been enlarged
	 */
	public boolean increaseFitnessCache(int minimalSize) {
		int realNewSize = (int) (1.5 * minimalSize);

		if (treeFitnessCache.maxSize() < realNewSize) {
			LRUMap<ProbProcessArrayTree, TreeFitness> newCache = new LRUMap<ProbProcessArrayTree, TreeFitness>(realNewSize);

			newCache.putAll(treeFitnessCache);

			treeFitnessCache = newCache;

			System.out.println("*-*-* Increased cache size to " + realNewSize);

			return true;
		}
		return false;
	}

	public Random getRandom() {
		return rng;
	}

	/**
	 * Increases the generation counter with one. Should only be called from the
	 * evolution engine!!! clean the history cache in each generation. maybe it
	 * should not be here, but testing.
	 */
	public void increaseGeneration() {
		generation++;
		//jbuijs improvement: don't clean every generation
		cleanHistoryCache();
	}

	public int getCurrentGeneration() {
		return generation;
	}

	public PluginContext getContext() {
		return context;
	}

	/**
	 * Updates the classifier to the given one and also creates a new LogInfo
	 * instance and fires a changed event to trigger all listeners.
	 * 
	 * @param selectedItem
	 */
	public void updateEventClassifier(XEventClassifier eventClassifier) {
		if (!eventClasses.getClassifier().equals(eventClassifier)) {
			logInfo = XLogInfoFactory.createLogInfo(getLog(), eventClassifier);
			eventClasses = LogUtils.deepCloneXEventClasses(logInfo.getEventClasses(), log, logInfo);
			updateLogDerived();
		}
	}

	/**
	 * Fools the rest of the ETM into thinking the event log only contains a
	 * subset of the event classes (e.g. the top X). This can be used to only
	 * consider frequent activity classes or to steadily increase the number of
	 * event classes considered during evolution. This will trigger an event to
	 * all listeners of this class.
	 * 
	 * @param topSize
	 *            The number of most frequent event classes to include. Provide
	 *            negative value to consider all.
	 */
	public void considerTopEventClasses(int topSize) {
		//If a negative value or a value eq. or greater than the number of event classes
		if (topSize < 0 || topSize >= logInfo.getEventClasses().size()) {
			//And the eventclasses list is different than the 'full' list 
			if (logInfo.getEventClasses().size() != eventClasses.size()) {
				//UPDATE
				eventClasses = logInfo.getEventClasses();
				updateLogDerived();
			}
			//Done
			return;
		} else {
			//We actually have to be smart
			XEventClassifier classifier = eventClasses.getClassifier();
			eventClasses = new XEventClasses(classifier);

			HashMap<XEventClass, Integer> base = new HashMap<XEventClass, Integer>();
			TreeMap<XEventClass, Integer> sortedClasses = new TreeMap<XEventClass, Integer>(new ValueComparator(base));

			for (XEventClass clazz : logInfo.getEventClasses().getClasses()) {
				base.put(clazz, clazz.size());
			}
			sortedClasses.putAll(base);

			//FIXME include topSize MOST FREQUENT classes... (already fixed, right?)
			for (XEventClass currentClass : sortedClasses.navigableKeySet()) {
				//for (int i = 0; i < topSize; i++) {
				LogUtils.addXEventClass(eventClasses, currentClass, log, logInfo);

				if (eventClasses.size() >= topSize) {
					break;
				}
			}

			updateLogDerived();

			//FIXME update the WHOLE cache...

			System.out.println(String.format("Top %d classes: \n\r", topSize));
			System.out.println(LogUtils.eventClassesToString(eventClasses));
		}
	}

	/**
	 * @return the aStarAlgorithm
	 */
	public AStarAlgorithm getaStarAlgorithm() {
		return aStarAlgorithm;
	}

	/**
	 * Comparator on value to sort treemap of event classes
	 * 
	 * @author jbuijs
	 * 
	 */
	class ValueComparator implements Comparator<XEventClass> {

		Map<XEventClass, Integer> base;

		public ValueComparator(Map<XEventClass, Integer> base) {
			this.base = base;
		}

		// Note: this comparator imposes orderings that are inconsistent with equals.    
		public int compare(XEventClass a, XEventClass b) {
			if (base.get(a) >= base.get(b)) {
				return -1;
			} else {
				return 1;
			} // returning 0 would merge keys
		}
	}

	/**
	 * Returns the map of assigned costs for the replayer for the given event
	 * class
	 * 
	 * @return the logCosts
	 */
	public Map<XEventClass, Integer> getLogCosts() {
		return logCosts;
	}

	/**
	 * Returns the estimated minimal costs to perform only move on log
	 * 
	 * @return the estimatedMinLogCost
	 */
	public int getEstimatedMinLogCost() {
		return estimatedMinLogCost;
	}

	/**
	 * Updates logCosts, estimatedMinLogCost and the aStarAlgorithm
	 * 
	 * It also Calculates the {@link minLogCost} (the minimal cost) for
	 * replaying the log on an 'empty' model. This value does not change between
	 * individuals, hence it only needs to be compute at the start of the
	 * algorithm.
	 *
	 */
	public void updateLogDerived() {
		// Calculate the Move On Log Costs for each EventClass
		logCosts = new HashMap<XEventClass, Integer>();
		estimatedMinLogCost = 0;
		for (XEventClass eventClass : getLogInfo().getEventClasses().getClasses()) {
			logCosts.put(eventClass, MLcost);
			estimatedMinLogCost += eventClass.size() * MLcost;
		}
		aStarAlgorithm = new AStarAlgorithm(log, eventClasses, logCosts);
		emptyAStarAlgorithm = new AStarAlgorithm(eventClasses);
		updateMinLogCost();
	}

	public void updateMinLogCost() {
		this.minLogCost = 0;
		Iterator<Trace> it = this.aStarAlgorithm.traceIterator();
		while (it.hasNext()) {
			Trace trace = it.next();
			TIntIterator it2 = trace.iterator();
			while (it2.hasNext()) {
				this.minLogCost += this.aStarAlgorithm.getLogMoveCost(it2.next())
						* this.aStarAlgorithm.getTraceFreq(trace);
			}
		}
	}

	/**
	 * Returns the {@link AStarAlgorithm} instance build on an empty event log
	 * (mainly used by the
	 * {@link org.processmining.plugins.etm.fitness.metrics.FitnessReplay}
	 * metric to normalize the costs of the alignments
	 * 
	 * @return the emptyAStarAlgorithm
	 */
	public AStarAlgorithm getEmptyAStarAlgorithm() {
		return emptyAStarAlgorithm;
	}

	/**
	 * Returns all trees sorted according to the provided dimensions
	 * 
	 * @param fittestFirst
	 * @param dimensions
	 * @return
	 */
	public List<ProbProcessArrayTree> getSortedOn(boolean fittestFirst, TreeFitnessInfo... dimensions) {
		ArrayList<ProbProcessArrayTree> trees = new ArrayList<ProbProcessArrayTree>();
		trees.addAll(treeFitnessCache.keySet());
		Collections.sort(trees, new TreeComparatorByFitness(this, fittestFirst, dimensions));
		return trees;
		//TreeMap<NAryTree, TreeFitness> sortedmap = new TreeMap<NAryTree, TreeFitness>(new TreeComparatorByFitness(this, dimensions));
	}

	/**
	 * Returns the {@link minLogCost} (the minimal cost) for replaying the log
	 * on an 'empty' model.
	 * 
	 * @return the minLogCost
	 */
	public double getMinLogCost() {
		return minLogCost;
	}

	/**
	 * Function to save the ancestry of each new tree. It returns the index to
	 * correctly identify the parent of the added {@link NAryTree}.
	 * 
	 * @param child
	 * @param parent
	 * @param crossoverPoint
	 * @param mutationPoint
	 * @return the if of the tree of life (the identifier of the parent in the
	 *         history cache)
	 */
	/*-
	// JBUIJS DISABLED MIGRATION FROM BORJA CODE
	public void saveHistory(NAryTree child, NAryTree parent, int pointOfChange, TypesOfChange typeOfChange) {
	List<NAryTreeHistory> potentialParents = treeHistoryCache.get(child);
	if (potentialParents == null) {
		potentialParents = new ArrayList<>();
	}
	potentialParents.add(new NAryTreeHistory(parent, pointOfChange, typeOfChange));
	this.treeHistoryCache.put(child, potentialParents);
	child.setTreeOfLifeID(potentialParents.size() - 1);
	}/**/

	/**
	 * Retrieves the parent of the input child.
	 * 
	 * @param child
	 * @return the {@link NaryTreeHistory} with the parent and point of
	 *         modification.
	 */
	public NAryTreeHistory getHistory(ProbProcessArrayTree child) {
		/*-
		// FIXME JBUIJS DISABLED MIGRATION FROM BORJA CODE
		if (child.getTreeOfLifeID() != -1) {
		List<NAryTreeHistory> potentialParents = this.treeHistoryCache.get(child);
		int treeOfLifeId = child.getTreeOfLifeID();
		if (potentialParents != null && potentialParents.size() > treeOfLifeId) {
		return potentialParents.get(treeOfLifeId);
		}
		}/**/
		return null;
	}

	/**
	 * Function to save the ancestry of each new tree. It returns the index to
	 * correctly identify the parent of the added {@link ProbProcessArrayTree}.
	 * 
	 * @param child
	 * @param parent
	 * @param crossoverPoint
	 * @param mutationPoint
	 * @return the if of the tree of life (the identifier of the parent in the
	 *         history cache)
	 */
	public void saveHistory(ProbProcessArrayTree child, ProbProcessArrayTree parent, int pointOfChange, TypesOfTreeChange typeOfChange) {
		/*-
		// FIXME JBUIJS DISABLED MIGRATION FROM BORJA CODE
		if(child == null || parent == null || pointOfChange <= 0){
		//System.out.println("wrong...");
		}
		List<NAryTreeHistory> potentialParents = treeHistoryCache.get(child);
		if (potentialParents == null) {
		potentialParents = new ArrayList<>();
		}
		potentialParents.add(new NAryTreeHistory(parent, pointOfChange, typeOfChange));
		this.treeHistoryCache.put(child, potentialParents);
		child.setTreeOfLifeID(potentialParents.size() - 1);/**/
	}

	/**
	 * Clears all the history
	 */
	public void cleanHistoryCache() {
		this.treeHistoryCache.clear();
	}

}
