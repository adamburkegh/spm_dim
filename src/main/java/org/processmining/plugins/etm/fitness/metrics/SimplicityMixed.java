package org.processmining.plugins.etm.fitness.metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

/**
 * This fitness metric provide a simplicity measure based on a combination of
 * measures. Currently this includes duplication of activities, unnecessary
 * Tau's (e.g. in a sequence) and punishment for OR and Loops. This metric is a
 * bit 'abused' to amend the gaps/quirks in the other metrics and to steer the
 * algorithm towards 'sensible' models.
 * 
 * @author jbuijs
 * 
 */
//FIXME check all class contents
//FIXME Test Class thoroughly
//TODO update comment above, out of date :)
public class SimplicityMixed extends TreeFitnessAbstract {

	public static final TreeFitnessInfo info = new TreeFitnessInfo(SimplicityMixed.class, "Sm", "Simplicity - Mixed",
			"Calculates simplicity by punishing for duplicate activities, OR and LOOP operators and useless nodes.",
			Dimension.SIMPLICITY, true);

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		//We want to know the relative number of duplicates of event classes
		double duplRatio = nrDuplicateActivities(candidate, true) / candidate.size();

		//We want to know which part of the tree contains useless nodes
		double uselessNodesRatio = SimplicityUselessNodes.uselessNodesRatio(candidate);

		//Now return the weighted average of all the things we punish for
		//return (duplRatio + uselessTauRatio + operatorPunishment) / 3;
		//		return (uselessTauRatio + operatorPunishment) / 2;
		return 1 - ((duplRatio + uselessNodesRatio) / 2);
	}

	protected static double nrDuplicateActivities(ProbProcessArrayTree tree) {
		return nrDuplicateActivities(tree, true);
	}

	protected static double nrDuplicateActivities(ProbProcessArrayTree tree, boolean ignoreTaus) {
		//count #duplicate event classes
		double duplication = 0;
		Set<Short> classes = new HashSet<Short>();
		//Go through the leafs and remember the classes we've seen

		int i = tree.isLeaf(0) ? 0 : tree.getNextLeafFast(0);
		do {
			if (classes.contains(tree.getType(i))) {
				//We count tau's depending on the boolean
				if (!(tree.getType(i) == ProbProcessArrayTree.TAU) || !ignoreTaus) {
					duplication++;
				}
			} else {
				classes.add(tree.getType(i));
			}
			i = tree.getNextLeafFast(i);
		} while (i < tree.size());

		return (duplication == 0) ? 0 : duplication / classes.size();
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}
}
