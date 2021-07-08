package org.processmining.plugins.etm.fitness.metrics;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

import util.LblTree;

/**
 * Calculates the relative edit distance using a collection of base trees,
 * calling {@link EditDistanceWrapperRTEDAbsolute} and then normalizing the
 * value to a similarity value (e.g. 1.0 implies equal trees)
 * 
 * @author jbuijs
 * 
 */
//FIXME check all class contents
//FIXME Test Class thoroughly
//TODO left !natural for now, otherwise ratio/nrEdits are inverted and difficult to mix, splitting etc. gets ugly at calculating best match tree
public class EditDistanceWrapperRTEDRelative extends EditDistanceWrapperRTEDAbsolute {

	@SuppressWarnings("unchecked")
	public static final TreeFitnessInfo info = new TreeFitnessInfo(EditDistanceWrapperRTEDRelative.class, "Er",
			"Edit distance - relative part of the reference model(s) that has been modified.",
			"Calculates the relative edit distance between candidates and a set of base trees "
					+ "using the RTED library which determines the optimal approach for a quick calculation.",
			Dimension.OTHER, true);

	/**
	 * Instantiate the edit distance metric with the given set of base trees
	 * 
	 * @param base
	 */
	public EditDistanceWrapperRTEDRelative(Collection<ProbProcessArrayTree> base) {
		super(base);
	}

	public EditDistanceWrapperRTEDRelative(ProbProcessArrayTree[] base) {
		super(base);
	}

	public EditDistanceWrapperRTEDRelative(ProbProcessArrayTree baseTree) {
		super(baseTree);
	}

	public EditDistanceWrapperRTEDRelative(EditDistanceWrapperRTEDRelative original) {
		super(original);
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		//If there is no base set to compare with, the edit distance is always perfect right :D
		if (baseTrees.isEmpty()) {
			return 1;
		}

		//convert input tree to an Lbl Tree
		LblTree candidateLblTree = toLblTree(candidate);

		//To find the closest tree amongst the base trees, keep track of the best
		double bestDist = 0; //absolute distance/# edits
		double bestRatio = 0; //ratio

		//Now, for each tree in our base set of trees
		for (Map.Entry<LblTree, Integer> baseTree : baseTrees.entrySet()) {
			//Calculate both the # edits and the relative/normalized edit ratio
			int totalSize = (baseTree.getValue() + candidate.size());
			double edits = EditDistanceWrapperRTEDAbsolute.getDistance(baseTree.getKey(), candidateLblTree);
			double editRatio = 1 - (edits / totalSize);

			if (editRatio > bestRatio) {
				bestDist = edits;
				bestRatio = editRatio;
				bestTree = baseTree.getKey();
			}
		}

		lastBestDist = bestDist;

		/*
		 * Now that we found the best edit distance, check if we exceeded the
		 * limit so we should return the worst value, if not, then check if we
		 * should return the ratio or absolute #edits
		 */
		if (limit != Double.MAX_VALUE && limit >= 0) {
			//We have a real limit
			if (bestRatio < limit) {
				return info.getWorstFitnessValue(); //exceeded edit ratio
			} else if (beAbsolute)
				return 1;//under limit so absolutely good
		}

		return bestRatio;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}
}
