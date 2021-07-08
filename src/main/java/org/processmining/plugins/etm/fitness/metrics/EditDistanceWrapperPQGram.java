package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

import tree.LblTree;
import distance.PQGramDist;

/**
 * Edit distance metric that uses the PQ-gram library to calculate the (sorted)
 * edit distance between two trees.
 * 
 * @author jbuijs
 * 
 */
//FIXME check all class contents
//FIXME Test Class thoroughly
public class EditDistanceWrapperPQGram {//extends TreeFitnessAbstract {

	//DISABLED to avoid confusion with other edit distance metric.
	/*-
	public static final TreeFitnessInfo info = new TreeFitnessInfo(EditDistanceWrapperPQGram.class, "Ep",
			"EditDistance_PQgramWrapper", "Uses the PQgram library to calculate the edit distance between two trees.",
			Dimension.OTHER, false);
		/**/

	private LblTree baseTree = null;

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {

		ProbProcessArrayTree treeNormalized = TreeUtils.normalize(candidate);

		LblTree lblTree = toLblTree(treeNormalized);

		//First call sets the tree reference
		if (baseTree == null) {
			baseTree = lblTree;
			return Integer.MAX_VALUE;
		}

		PQGramDist pqgram = new PQGramDist(2, 3, false);
		return pqgram.treeDist(baseTree, lblTree);
	}

	public LblTree toLblTree(ProbProcessArrayTree tree) {
		return toLblTree(tree, 0);
	}

	public LblTree toLblTree(ProbProcessArrayTree tree, int n) {
		LblTree node = new LblTree(tree.getType(n) + "", -1);
		if (!tree.isLeaf(n)) {
			for (int i = 0; i < tree.nChildren(n); i++) {
				node.add(toLblTree(tree, tree.getChildAtIndex(n, i)));
			}
		}
		return node;
	}

	/**
	 * {@inheritDoc}
	 */
	/*-
	public TreeFitnessInfo getInfo() {
		return info;
	}/**/

}
