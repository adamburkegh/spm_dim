package org.processmining.plugins.etm.fitness.metrics;

import gnu.trove.set.TIntSet;

import java.util.List;
import java.util.Map;

import org.processmining.plugins.boudewijn.treebasedreplay.astar.ModelPrefix;
import org.processmining.plugins.boudewijn.treebasedreplay.astar.TreeMarkingVisit;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.BehaviorCounter;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

// FIXME check all class contents
// FIXME Test Class thoroughly
public class PrecisionEscEdges extends TreeFitnessAbstract {

	public final static TreeFitnessInfo info = new TreeFitnessInfo(PrecisionEscEdges.class, "Pe",
			"Precision - Escaping Edges",
			"Calculates the precision by looking at the fraction of escaping edges in the (partial) state space constructed "
					+ "by the Replay Fitness metric", Dimension.PRECISION, true);
	//, FitnessReplay.class

	private CentralRegistry centralRegistry;

	protected PrecisionEscEdges() {
		centralRegistry = null;
	}

	/**
	 * This precision metric uses the escaping edges as calculated by the replay
	 * alignment. Therefore, we require a replayFitness instance that is called
	 * first so we can use that information.
	 * 
	 * @param replayFitness
	 *            Instance of the replay Fitness metric to use information from
	 */
	public PrecisionEscEdges(CentralRegistry centralRegistry) {
		this.centralRegistry = centralRegistry;
	}

	/**
	 * Deep-clone copy constructor for automated cloning
	 * 
	 * @param pe
	 * @param replayFitness
	 */
	public PrecisionEscEdges(PrecisionEscEdges pe) {
		this.centralRegistry = pe.centralRegistry;
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		assert (centralRegistry != null);

		BehaviorCounter behC = centralRegistry.getFitness(candidate).behaviorCounter;
		//Map<TreeMarkingVisit<ModelPrefix>, TIntSet> marking2modelmove = behC.getMarking2ModelMove();
		//TObjectIntMap<TreeMarkingVisit<ModelPrefix>> marking2visitCount = behC.getMarking2VisitCount();

		// count the number of unused enabled nodes for all markings.
		double numerator = 0;
		double denominator = 0;

		synchronized (behC) {
			for (Map.Entry<TreeMarkingVisit<ModelPrefix>, TIntSet> entry : behC.getMarking2ModelMove().entrySet()) {
				int f = behC.getMarking2VisitCount().get(entry.getKey());
				int out = entry.getKey().getMarking().size();
				int used = entry.getValue().size();

				assert used <= out;
				
				numerator += f * (double) (out - used);
				denominator += f * out;

				//System.out.println(entry.getKey().toString() + "=" + entry.getValue().toString()+"\t f:"+f+"\t out/used:"+out+"/"+used+"\t num/denum:"+numerator+"/"+denominator);
				
				//System.out.println(f + " * (" + out + " - " + used + ") / " + f + " * " + out + " = " + numerator + " / "	+ denominator);
			}
		}

		//We have no results/entries/traces aligned... this is very bad because we won't have an empty log
		if (denominator == 0) {
			return info.getWorstFitnessValue();
		}

		double precision = 1 - (numerator / denominator);

		//System.out.println("Precision = 1 - " + numerator + " / " + denominator + " = " + precision);

		return precision;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}

	/*-
	public TreeFitnessGUISettingsAbstract<PrecisionEscEdges> getGUISettingsPanel(ETMParam param) {
		return new TreeFitnessGUISettingsAbstract<PrecisionEscEdges>(param);
	}/**/

}
