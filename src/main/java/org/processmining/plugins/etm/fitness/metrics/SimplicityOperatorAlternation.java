package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

// FIXME check all class contents
// FIXME Test Class thoroughly
public class SimplicityOperatorAlternation extends TreeFitnessAbstract {

	public static final TreeFitnessInfo info = new TreeFitnessInfo(SimplicityOperatorAlternation.class, "Sa",
			"Simplicity - Operator Alternation",
			"Based on the idea that having to switch between different control flow constructs makes a model harder to read, "
					+ "this metric calculates simplicity", Dimension.SIMPLICITY, true);

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		return (double) 1 - (calculateSimplicityOperatorAlternation(candidate) / candidate.size());
	}

	/**
	 * Returns the total number of operator type alternations, needs to be
	 * normalized!
	 * 
	 * @param ProbProcessArrayTree
	 *            tree to evaluate
	 * @return int Number of alternations
	 */
	protected static int calculateSimplicityOperatorAlternation(ProbProcessArrayTree tree) {
		int punishment = 0;

		//We count every time when a non-leaf is of a different type than its parent
		//Therefore we skip the root since it is either a leaf or has no parent to compare with
		for (int i = 1; i < tree.size(); i++) {
			if (!tree.isLeaf(i) && tree.getType(i) != tree.getType(tree.getParent(i))) {
				punishment++;
			}
		}

		return punishment;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}
}
