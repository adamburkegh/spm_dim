package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.BehaviorCounter;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

// FIXME check all class contents
// FIXME Test Class thoroughly
public class Generalization extends TreeFitnessAbstract {

	@SuppressWarnings("unchecked")
	public static final TreeFitnessInfo info = new TreeFitnessInfo(
			Generalization.class,
			"Gv",
			"Generalization - Nr. visits to node",
			"Based on the idea that more evidence increases the confidence that the model is correct, " +
			"this metric assigns a generalization score to a process tree by using the average number of visits to each node.",
			Dimension.GENERALIZATION, true);

	private CentralRegistry registry;

	protected Generalization() {
		registry = null;
	}

	public Generalization(CentralRegistry registry) {
		this.registry = registry;
	}

	public Generalization(Generalization original) {
		this.registry = original.registry;
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		//Get the arrays
		BehaviorCounter behC = registry.getFitness(candidate).behaviorCounter;
		int[] syncMoveCount = behC.getSyncMoveCount();
		@SuppressWarnings("unused")
		int[] aSyncMoveCount = behC.getASyncMoveCount();
		@SuppressWarnings("unused")
		int[] moveCount = behC.getMoveCount();

		double gen = 0;
		for (int i = 0; i < syncMoveCount.length; i++) {
			//Only count non useless nodes
			double thisGen = 1; //Bad nodes are punished by adding 1 to the numerator, increasing generalization when 0 = best
			//!(candidate.getType(i) == NAryTree.TAU) && 
			//if (moveCount[i] > 0 && candidate.getType(i) != NAryTree.TAU && candidate.isLeaf(i)
			//	&& !SimplicityUselessNodes.isUselessNode(candidate, i)) {
			if (syncMoveCount[i] > 0 /*-&& candidate.getType(i) != NAryTree.TAU/**/
					&& !SimplicityUselessNodes.isUselessNode(candidate, i))/**/
			{
				/*
				 * If the node is used, is not a tau and is not useless, allow
				 * bonus points since thisGen will decrease by the following
				 * formula: / int nrSyncMoves = syncMoveCount[i];
				 * 
				 * //Now, subtract all the TAU moves within this subtree if
				 * (!candidate.isLeaf(i)) { for (int j = i + 1; j <
				 * candidate.getNext(i); j++) { if (candidate.getType(j) ==
				 * NAryTree.TAU) { nrSyncMoves -= syncMoveCount[j]; } } }
				 * 
				 * if (nrSyncMoves <= 0) nrSyncMoves = 1; /*-
				 */

				thisGen = (1 / Math.sqrt(syncMoveCount[i])); //'Original'
				//				thisGen = (1 / Math.sqrt(nrSyncMoves)); //'Original' with internal TAU sync moves reduced
				// thisGen = (1 / Math.sqrt(Math.max(syncMoveCount[i] - aSyncMoveCount[i], 1))); //Punishing for model moves
				//thisGen = (1 / Math.sqrt(Math.max(moveCount[i] - syncMoveCount[i],1))); //To prevent XOR(BLA, A) where 'BLA' is kinda usefull, try this one for size...
			}

			//TODO remove debug code
			if (thisGen > 1 || thisGen < 0) {
				System.out.println("OOPS");
			}
			gen += thisGen;
		}

		//To prevent a single-leaf tree to be the best answer we add/subtract 1
		//FIXME this results in single leaf trees to be very good (why would this be bad)
		double generalization = (gen) / candidate.size();

		//TODO remove debug code
		if (generalization < 0 || generalization > 1) {
			//System.out.println("OOPS");
		}

		return 1 - generalization;

		// OLD CODE BELOW FOR REFERENCE
		/*-
		BehaviorCounter[] behCounters = registry.getFitness(candidate).behaviorCounters;

		double gen = 0;
		double nodes = 0;
		for (int i = 0; i < candidate.size(); i++) {
			BehaviorCounter beh = behCounters[i];

			//Get the number of synchronous executions
			int nrExecuted = beh.getBehavedAsAND() + beh.getBehavedAsSEQLR() + beh.getBehavedAsSEQRL()
					+ beh.getBehavedAsL() + beh.getBehavedAsR();

			//For SEQ and AND L and R means that the other subtree was a modelmove, don't count those...
			if (candidate.getType(i) == NAryTree.SEQ || candidate.getType(i) == NAryTree.AND) {
				nrExecuted -= beh.getBehavedAsL();
				nrExecuted -= beh.getBehavedAsR();
			}

			double thisGen = (1 / Math.sqrt(nrExecuted));
			//It could be that the generalization for this node is INFINITE because the behavior counter counted as 'unused' f.i.
			//FIX ME arbitrary choice of 1000, maybe decide based on nrExecuted?
			if (thisGen > 1000)
				thisGen = 1; //In which case it actually means generalization is bad so 1. 
			gen += thisGen;
			nodes++;
		}
		return gen / nodes;
		/**/

	}

	public TreeFitnessInfo getInfo() {
		return info;
	}
}
