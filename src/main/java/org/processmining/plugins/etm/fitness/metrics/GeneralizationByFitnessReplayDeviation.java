package org.processmining.plugins.etm.fitness.metrics;

import gnu.trove.iterator.TIntIterator;

import java.util.Iterator;
import java.util.List;

import nl.tue.astar.Trace;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.BehaviorCounter;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

/**
 * Generalization fitness metric that uses the trace-level replay fitness value
 * deviation to estimate how 'stable' the process model describes the event log.
 * E.g. a low standard deviation indicates that the model describes all traces
 * in the event log equally well and therefore captures the general idea.
 * 
 * @author jbuijs
 * 
 */
public class GeneralizationByFitnessReplayDeviation extends TreeFitnessAbstract {

	public static TreeFitnessInfo info = new TreeFitnessInfo(
			GeneralizationByFitnessReplayDeviation.class,
			"Gd",
			"Generalization - trace-level Fr deviation",
			" Generalization fitness metric that uses the trace-level replay fitness value deviation to estimate how 'stable' the process model describes the event log. E.g. a low standard deviation indicates that the model describes all traces in the event log equally well and therefore captures the general idea.",
			Dimension.GENERALIZATION, true);

	private CentralRegistry registry;

	protected GeneralizationByFitnessReplayDeviation() {
		registry = null;
	}

	public GeneralizationByFitnessReplayDeviation(CentralRegistry registry) {
		this.registry = registry;
	}

	public GeneralizationByFitnessReplayDeviation(GeneralizationByFitnessReplayDeviation original) {
		this.registry = original.registry;
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		/*
		 * Minimal model Costs
		 *//*-
			int[] modelCosts = FitnessReplay.modelCosts(candidate);

			//We use the configuration only if that configuration actually exists. Otherwise we assume no configuration
			int localConfNr = fitnessReplay.configurationNumber <= candidate.getNumberOfConfigurations() - 1 ? fitnessReplay.configurationNumber
				: -1;

			int[] syncMoveCount = new int[candidate.size()];
			int[] aSyncMoveCount = new int[candidate.size()];
			int[] moveCount = new int[candidate.size()];
			NAryTreeReplayerWithHybridILP treeBasedAStar = new NAryTreeReplayerWithHybridILP(
				registry.getEmptyAStarAlgorithm(), fitnessReplay.c, candidate, localConfNr, modelCosts,
				new HashMap<TreeMarkingVisit<ModelPrefix>, TIntSet>(),
				new TObjectIntHashMap<TreeMarkingVisit<ModelPrefix>>(),
				//new HashMap<TreeMarkingVisit<ModelPrefix>, TIntSet>(),
				syncMoveCount, aSyncMoveCount, moveCount, null, true);
			int minModelCost;
			try {
			//We don't pass a stopAt cost, but do pass a timeLimit since replaying the empty log should be easiest of all... 
			minModelCost = treeBasedAStar.run(VerboseLevel.NONE, Integer.MAX_VALUE, Integer.MAX_VALUE,
					fitnessReplay.timeLimit);
			} catch (AStarException e) {
			e.printStackTrace();
			return info.getWorstFitnessValue();
			}/**/

		/*
		 * Now go through each trace and calculate the fitness on the trace
		 * level
		 */
		BehaviorCounter behC = registry.getFitness(candidate).behaviorCounter;
		DescriptiveStatistics stats = new DescriptiveStatistics();
		Iterator<Trace> it = registry.getaStarAlgorithm().traceIterator();

		//We need the detailed alignments
		if (!behC.isAlignmentSet()) {
			return info.getWorstFitnessValue();
		}

		while (it.hasNext()) {
			Trace trace = it.next();
			int freq = registry.getaStarAlgorithm().getTraceFreq(trace);

			TreeRecord alignment = behC.getAlignment(trace);
			double traceCost = alignment.getTotalCost() / 1000;

			int minTraceCost = 0;
			TIntIterator it2 = trace.iterator();
			while (it2.hasNext()) {
				//For each event/activity
				minTraceCost += registry.getaStarAlgorithm().getLogMoveCost(it2.next());
			}

			//Now the trace fitness is:
			double traceFitness = 1 - (traceCost / (minTraceCost + behC.getMinModelCost()));

			for (int i = 0; i < freq; i++) {
				stats.addValue(traceFitness);
			}
		}

		registry.getFitness(candidate).fitnessValues.get(FitnessReplay.info);
		stats.getMean();
		stats.getGeometricMean();
		return 1 - stats.getStandardDeviation();
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}

}
