package org.processmining.plugins.etm.fitness.metrics;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import nl.tue.astar.AStarThread.Canceller;
import nl.tue.astar.Trace;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

/**
 * Using a specialized replay algorithm based on Adriansyah an alignment between
 * a process tree and the event log is calculated.
 * 
 * @author jbuijs
 * 
 */
// FIXME update to use the ReplayFitness metric class instance (disabled extends so we can't be used)
//FIXME check all class contents
//FIXME Test Class thoroughly
public class FitnessReplayConfidenceLevelsWrapper {//extends TreeFitnessAbstract {

	//DISABLED does not work and does not seem to help much
	/*-
	public static final TreeFitnessInfo info = new TreeFitnessInfo(
			FitnessReplayConfidenceWrapper.class,
			"FrCONF",
			"Fitness Replay With Confidence Levels",
			"Calculates the optimal alignment between the log and the process tree to determine where deviations are but uses incremental log sections for performance reasons",
			Dimension.FITNESS, true);
			/**/
	public static final TreeFitnessInfo info = null;

	public static final String CONFIDENCE_KEY = "CONFIDENCE_LEVEL";

	private final HashMap<XEventClass, Integer> logCosts = new HashMap<XEventClass, Integer>();

	private HashMap<Integer, AStarAlgorithm> algorithmsForConfLvl = new HashMap<Integer, AStarAlgorithm>();
	private int nrConfidenceLevels;
	private boolean useTraceCountForConfidence;

	private CentralRegistry registry;

	public FitnessReplayConfidenceLevelsWrapper(CentralRegistry registry, Canceller c, double maxF, int nrConfidenceLevels2,
			boolean useTraceCountForConfidence2) {
		this.nrConfidenceLevels = Math.max(nrConfidenceLevels2, 1);//1 or more levels
		this.useTraceCountForConfidence = useTraceCountForConfidence2;
		this.registry = registry;

		/*
		 * Calculate the Move On Log Costs for each EventClass
		 */
		for (XEventClass eventClass : registry.getEventClasses().getClasses()) {
			logCosts.put(eventClass, 1);
		}

		/*
		 * Create different event logs for different confidence levels and
		 * instantiate algorithms for each confidence level and call the
		 * appropriate one when given a PT that already has a fitness set
		 */
		AStarAlgorithm algorithm = new AStarAlgorithm(registry.getLog(), registry.getEventClasses(), logCosts);

		//First get the clusters, sorted by freq
		TObjectIntMap<Trace> converted = algorithm.getConvertedLog();
		ArrayList<Integer> list = new ArrayList<Integer>();
		TObjectIntIterator<Trace> it = converted.iterator();
		while (it.hasNext()) {
			it.advance();
			list.add(it.value());
		}

		//Sort the cluster counts from small to large
		Collections.sort(list);

		//We can not have more confidence levels than traces or clusters
		if (useTraceCountForConfidence) {
			nrConfidenceLevels = Math.min(nrConfidenceLevels, registry.getLog().size());
		} else {
			nrConfidenceLevels = Math.min(nrConfidenceLevels, converted.size());
		}

		//Now calculate the number of clusters per confidence level (this might be a fraction)
		double traceClustersPerLvl = (double) converted.size() / nrConfidenceLevels;
		double tracesPerLvl = (double) registry.getLog().size() / nrConfidenceLevels;

		//Now instantiate an algorithm per confidence level, with an adjusted converted log
		for (int i = 1; i < nrConfidenceLevels; i++) {
			//Instantiate an alg. for the whole log
			AStarAlgorithm algForThisLvl = new AStarAlgorithm(registry.getLog(), registry.getEventClasses(), logCosts);

			TObjectIntMap<Trace> convertedForThisLvl = algForThisLvl.getConvertedLog();
			TObjectIntIterator<Trace> itForThisLvl = convertedForThisLvl.iterator();

			//Now determine which trace clusters to include
			if (useTraceCountForConfidence) {
				//FIXME incoming list is not sorted so rather arbitrary selection of clusters
				//Furthermore we need to make sure that we add at least one new cluster each confidence level, breaking the pattern possibly...

				//We take those clusters that represent (1/nrConfLvl)*currentLvl of the total #traces
				int totalNrTracesCovered = 0;
				while (itForThisLvl.hasNext()) {
					itForThisLvl.advance();
					if (totalNrTracesCovered > (tracesPerLvl * i)) {
						itForThisLvl.remove();
					}
					totalNrTracesCovered += itForThisLvl.value();
				}
			} else {
				//We take the the top (1/nrConfLvl)*currentLvl of the total #clusters
				int minFreqForLvl = list.get(list.size() - ((int) (traceClustersPerLvl * i)));

				while (itForThisLvl.hasNext()) {
					itForThisLvl.advance();
					if (itForThisLvl.value() < minFreqForLvl)
						itForThisLvl.remove();
				}
			}

			algorithmsForConfLvl.put(i, algForThisLvl);
		}
		/*
		 * The FOR-loop above missed the last confidence level, since that is a
		 * special one since we want to make sure that this level contains the
		 * whole event log, no matter what. That is what we are going to do now.
		 */
		AStarAlgorithm algForThisLvl = new AStarAlgorithm(registry.getLog(), registry.getEventClasses(), logCosts);
		algorithmsForConfLvl.put(nrConfidenceLevels, algForThisLvl);
	}

	public FitnessReplayConfidenceLevelsWrapper(CentralRegistry registry, Canceller c, double maxF) {
		this(registry, c, maxF, 1, false);
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		/*
		 * TODO re-think setup, do we keep a list of FitnessReplay instances
		 * that we initiate with different logs or do we do something smarter in
		 * here, e.g. almost duplicating the FitnessReplay code???
		 */
		assert candidate.isConsistent();

		/*
		 * Instantiate the AStar for this confidence level
		 */
		double confidenceLevel = 0;
		if (registry.isFitnessKnown(candidate)) {
			confidenceLevel = registry.getFitness(candidate).fitnessValues.get(info);
		}

		if (confidenceLevel < nrConfidenceLevels)
			confidenceLevel++;
		else {
			//We already got it at the max. confidence level boy!
			return registry.getFitness(candidate).getOverallFitnessValue();
		}

		//FIXME return something usefull...
		return 0;
	}

	public int getNrConfidenceLevels() {
		return nrConfidenceLevels;
	}

	/**
	 * {@inheritDoc}
	 */
	public TreeFitnessInfo getInfo() {
		return info;
	}

	/**
	 * We can also be added as an observer to the population so we can keep
	 * track of the average fitness, standard deviation etc. to improve
	 * performance of our calculations, especially on bad trees which take most
	 * of our time
	 * 
	 * @param data
	 */
	/*-
	public void populationUpdate(PopulationData<? extends Tree> data) {
		// TODO Auto-generated method stub
	}/**/
}
