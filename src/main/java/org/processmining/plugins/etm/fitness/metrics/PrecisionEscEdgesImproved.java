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
public class PrecisionEscEdgesImproved extends TreeFitnessAbstract {

	public final static TreeFitnessInfo info = new TreeFitnessInfo(PrecisionEscEdgesImproved.class, "Pi",
			"Precision - Escaping Edges Improved (TESTING)",
			"Calculates the precision by looking at the fraction of escaping edges in the (partial) state space constructed "
					+ "by the Replay Fitness metric and considers the model structure. "
					+ "Includes some improvements ideas from the other metric.", Dimension.PRECISION, true);
	//,FitnessReplay.class

	private CentralRegistry centralRegistry;

	private boolean debugMessages = false;

	protected PrecisionEscEdgesImproved() {
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
	public PrecisionEscEdgesImproved(CentralRegistry centralRegistry) {
		this.centralRegistry = centralRegistry;
	}

	/**
	 * Deep-clone copy constructor for automated cloning
	 * 
	 * @param pe
	 * @param replayFitness
	 */
	public PrecisionEscEdgesImproved(PrecisionEscEdgesImproved pe) {
		this.centralRegistry = pe.centralRegistry;
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		assert (centralRegistry != null);

		BehaviorCounter behC = centralRegistry.getFitness(candidate).behaviorCounter;
		behC.getMarking2ModelMove();
		behC.getMarking2VisitCount();

		if (debugMessages) {
			System.out.println("m2mm: " + behC.getMarking2ModelMove());
			System.out.println("m2vc: " + behC.getMarking2VisitCount());
		}

		// count the number of unused enabled nodes for all markings.
		double numerator = 0;
		double denominator = 0;

		for (Map.Entry<TreeMarkingVisit<ModelPrefix>, TIntSet> entry : behC.getMarking2ModelMove().entrySet()) {
			int f = behC.getMarking2VisitCount().get(entry.getKey());
			int out = entry.getKey().getMarking().size();
			int used = entry.getValue().size();

			double unused = out - used;

			//We do not consider all operators/moves
			boolean shouldBeConsidered = true;
			/*-*/
			for (int n : entry.getValue().toArray()) {
				if (n >= candidate.size() && debugMessages) {
					System.out.println("break");
				}

				//We do not consider the sequence (since it is always perfect precision) and nodes that have 1 child
				if ((n < candidate.size() && candidate.nChildren(n) == 1)
				//|| candidate.getType(n) == NAryTree.SEQ || candidate.getType(n) == NAryTree.REVSEQ
				) {
					shouldBeConsidered = false;
					break;
				}
			}/**/

			if (shouldBeConsidered) {
				//If we consider a node, we punish extra for Loops
				int punishFactor = 1;

				/*-* /
				//Loop through all used model moves in this marking
				for (int n : entry.getValue().toArray()) {
					if (n >= candidate.size() && debugMessages) {
						System.out.println("break");
					}

					//We punish loops extra :)
					if (n < candidate.size() && candidate.getType(candidate.getParent(n)) == NAryTree.LOOP) {
						punishFactor++;
					}
				}/**/

				numerator += f * unused * punishFactor;
				denominator += f * out;
			}

			if (debugMessages) {
				System.out.println(f + " * (" + out + " - " + used + ") / " + f + " * " + out + " = " + numerator
						+ " / " + denominator);
			}
		}

		//We have no results/entries/traces aligned... this is very bad because we won't have an empty log
		if (denominator == 0) {
			return info.getWorstFitnessValue();
		}

		//Numerator can be > denominator, causing precision to become negative, we cap that
		double precision = Math.max(0, 1 - (numerator / denominator));

		if (debugMessages) {
			System.out.println("Precision = 1 - " + numerator + " / " + denominator + " = " + precision);
		}

		return precision;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}
}
