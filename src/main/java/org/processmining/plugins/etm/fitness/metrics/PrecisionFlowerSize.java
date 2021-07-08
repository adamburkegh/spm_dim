package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

// FIXME check all class contents
// FIXME Test Class thoroughly
public class PrecisionFlowerSize extends TreeFitnessAbstract {

	public static final TreeFitnessInfo info = new TreeFitnessInfo(PrecisionFlowerSize.class, "Pf",
			"Precision - Flower Size",
			"Calculates the precision using the size of the flower-like structure in the tree.", Dimension.PRECISION,
			true);

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		return 1 - (countFlowerSize(candidate, false, 0) / ((double) candidate.size() * 2));
	}

	/**
	 * For precision, loops are a bad idea, so, we punish for them...
	 * 
	 * @param node
	 * @param inLoop
	 * @return
	 */
	protected static int countFlowerSize(ProbProcessArrayTree tree, boolean inLoop, int node) {
		//Stop the recursion for leafs
		if (tree.isLeaf(node))
			if (inLoop && tree.getType(node) != ProbProcessArrayTree.TAU)
				return 1;
			else
				return 0;

		/*-*/
		//As soon as we encounter a seq or and we leave the flowerloop if we were in one
		if (tree.getType(node) == ProbProcessArrayTree.AND || tree.getType(node) == ProbProcessArrayTree.SEQ)
			inLoop = false;
		/**/

		//Then, turn it back on for each loop operator we encounter
		if (tree.getType(node) == ProbProcessArrayTree.LOOP)
			inLoop = true;

		//So the size of the flower is ourselves plus that of our children...
		int flowerSize = 0;
		for (int c = 0; c < tree.nChildren(node); c++) {
			flowerSize += countFlowerSize(tree, inLoop, tree.getChildAtIndex(node, c));
		}

		return flowerSize;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}
}
