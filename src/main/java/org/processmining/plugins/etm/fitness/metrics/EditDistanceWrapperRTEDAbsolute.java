package org.processmining.plugins.etm.fitness.metrics;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;

import util.LblTree;

import com.fluxicon.slickerbox.factory.SlickerFactory;

import distance.RTED_InfoTree_Opt;

/**
 * Wrapper for the RTED (Robust Algorithm for the Tree Edit Distance) package,
 * see <a href=
 * "http://www.inf.unibz.it/dis/projects/tree-distance-repository/tree-edit-
 * distance.php#rted">M.Pawlik and N.Augsten. RTED: A Robust Algorithm for the
 * Tree Edit Distance. PVLDB. 2011.</a>. Assumes sorted process trees.
 * 
 * @author jbuijs
 * 
 */
public class EditDistanceWrapperRTEDAbsolute extends TreeFitnessAbstract {

	@SuppressWarnings("unchecked")
	public static final TreeFitnessInfo info = new TreeFitnessInfo(EditDistanceWrapperRTEDAbsolute.class, "Ea",
			"Edit distance - number of edits from reference model(s)",
			"Calculates the absolute edit distance between candidates and a set of base trees "
					+ "using the RTED library which determines the optimal approach for a quick calculation.",
			Dimension.OTHER, false);

	//Keep list of the trees and their size (in Process Tree form, not in LblTree form)
	protected HashMap<LblTree, Integer> baseTrees = new HashMap<LblTree, Integer>();
	//Keep track of the minimal (normalized) edit distance so far, and the corresponding best tree
	protected LblTree bestTree = null;
	protected double limit = Double.MAX_VALUE;
	protected boolean beAbsolute = false; //Beeing absolute means returning 1 for OK, worstvalue for NOK
	protected double lastBestDist;

	/**
	 * Instantiate the edit distance metric with the given set of base trees
	 * 
	 * @param base
	 */
	public EditDistanceWrapperRTEDAbsolute(Collection<ProbProcessArrayTree> base) {
		for (ProbProcessArrayTree tree : base) {
			LblTree lblTree = toLblTree(tree);
			baseTrees.put(lblTree, tree.size());
		}
	}

	public EditDistanceWrapperRTEDAbsolute(ProbProcessArrayTree[] base) {
		for (ProbProcessArrayTree tree : base) {
			LblTree lblTree = toLblTree(tree);
			baseTrees.put(lblTree, tree.size());
		}
	}

	public EditDistanceWrapperRTEDAbsolute(ProbProcessArrayTree baseTree) {
		LblTree lblTree = toLblTree(baseTree);
		baseTrees.put(lblTree, baseTree.size());
	}

	public EditDistanceWrapperRTEDAbsolute(EditDistanceWrapperRTEDAbsolute original) {
		this.baseTrees = new HashMap<LblTree, Integer>();
		for (Entry<LblTree, Integer> entry : original.baseTrees.entrySet()) {
			baseTrees.put(LblTree.fromString(entry.getKey().toString()), entry.getValue());
		}

		bestTree = original.bestTree;
		limit = original.limit;
		beAbsolute = original.beAbsolute;
		lastBestDist = original.lastBestDist;
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		//If there is no base set to compare with, the edit distance is always 0 right :D
		if (baseTrees.isEmpty()) {
			return 0;
		}

		//convert input tree to an Lbl Tree
		LblTree candidateLblTree = toLblTree(candidate);

		//To find the closest tree amongst the base trees, keep track of the best
		double bestDist = Double.POSITIVE_INFINITY; //absolute distance/# edits

		//Now, for each tree in our base set of trees
		for (Map.Entry<LblTree, Integer> baseTree : baseTrees.entrySet()) {
			double edits = EditDistanceWrapperRTEDAbsolute.getDistance(baseTree.getKey(), candidateLblTree);

			//Find the lowest edit distance or ratio depending on mode and update global memory
			if (edits < bestDist) {
				bestDist = edits;
				bestTree = baseTree.getKey();
			}

		}

		lastBestDist = bestDist;

		/*
		 * Now that we found the best edit distance, check if we exceeded the
		 * limit so we should return the worst value, if not, then check if we
		 * should return the ratio or absolute #edits
		 */
		if (limit != Double.MAX_VALUE) {
			//We have a real limit
			if (bestDist > limit) {
				return Double.MAX_VALUE; //exceeded # edits limit
			} else if (beAbsolute)
				return 1; //under limit so absolutely good
		}

		//TODO remove debug code
		if (bestDist < 0) {
			System.out.println("absolute distance below 0...");
		}

		return bestDist;
	}

	/**
	 * Returns the edit distance between the two provided trees using the RTED
	 * library (see class javadoc).
	 * 
	 * @param base
	 * @param candidate
	 * @return
	 */
	public static double getDistance(LblTree base, LblTree candidate) {
		RTED_InfoTree_Opt rted = new RTED_InfoTree_Opt(1, 1, 1);
		return rted.nonNormalizedTreeDist(base, candidate);
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
	 * Returns the tree to which the last call of
	 * {@link #getFitness(Tree, List)} was measured against. This is the tree
	 * that resulted in the best fitness value.
	 * 
	 * @return
	 */
	public LblTree getBestTree() {
		return bestTree;
	}

	/**
	 * Set the limit of edits. If the value is => 1 then actual edits are used,
	 * 1< ratios is used. To disable the limit use resetLimit(). The effect is
	 * that when the # edit or edit ratio exceeds this value, the edit fitness
	 * will be 1 (bad)
	 */
	public void setLimit(double limit) {
		this.limit = limit;
	}

	/**
	 * Returns the currently set limit after which a very bad fitness will be
	 * returned
	 * 
	 * @return
	 */
	public double getLimit() {
		return limit;
	}

	/**
	 * Resets the max number of edits allowed to go back to relative mode
	 */
	public void resetLimit() {
		this.limit = Double.MAX_VALUE;
	}

	/**
	 * Returns the best (lowest) edit distance obtained in the last
	 * {@link #getFitness()} call.
	 * 
	 * @return
	 */
	public double getLastBestDist() {
		return lastBestDist;
	}

	/**
	 * {@inheritDoc}
	 */
	public TreeFitnessInfo getInfo() {
		return info;
	}

	public static EditDistanceWrapperRTEDAbsoluteGUI getGUISettingsPanel(ETMParamAbstract param) {
		return new EditDistanceWrapperRTEDAbsoluteGUI(param);
	}

	public static class EditDistanceWrapperRTEDAbsoluteGUI extends
			TreeFitnessGUISettingsAbstract<EditDistanceWrapperRTEDAbsolute> {

		private static final long serialVersionUID = 1L;

		private JLabel messageLabel;

		public EditDistanceWrapperRTEDAbsoluteGUI(ETMParamAbstract param) {
			super(param);

			if (param.getSeed().isEmpty()) {
				messageLabel = SlickerFactory.instance().createLabel(
						"WARNING: no seed or input process models found! Edit distance will always return same value!");
			} else {
				messageLabel = SlickerFactory.instance().createLabel("Seed found, edit distance can be used.");
			}

			this.add(messageLabel);
		}

		public EditDistanceWrapperRTEDAbsolute getTreeFitnessInstance(final CentralRegistry registry,
				Class<TreeFitnessAbstract> clazz) {
			return new EditDistanceWrapperRTEDAbsolute(this.param.getSeed());
		}

		public void init(EditDistanceWrapperRTEDAbsolute instance) {
			if (instance.baseTrees.isEmpty()) {
				messageLabel = SlickerFactory.instance().createLabel(
						"WARNING: no seed or input process models found! Edit distance will always return same value!");
			} else {
				messageLabel = SlickerFactory.instance().createLabel("Seed found, edit distance can be used.");
			}
		}
	}
}
