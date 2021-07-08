package org.processmining.plugins.etm.fitness;

import java.util.Comparator;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

/**
 * Sorts trees by sorting them on the quality dimensions, in the provided order
 * (e.g. first on the first dimension, if equal, then according to the sorting
 * of the second dimension etc.)
 * 
 * @author jbuijs
 * 
 */
public class TreeComparatorByFitness implements Comparator<ProbProcessArrayTree> {

	private CentralRegistry registry;
	private TreeFitnessComparator[] comparators;

	//private boolean fittestFirst;

	/**
	 * 
	 * @param registry
	 * @param fittestFirst
	 *            if TRUE orders fit -> less fit, if FALSE you get the worst
	 *            candidates first
	 * @param dimensions
	 *            The dimensions in the order as they should be used in the
	 *            sorting!!!
	 */
	public TreeComparatorByFitness(CentralRegistry registry, boolean fittestFirst, TreeFitnessInfo... dimensions) {
		this.registry = registry;
		//this.fittestFirst = fittestFirst;
		this.comparators = new TreeFitnessComparator[dimensions.length];

		for (int d = 0; d < dimensions.length; d++) {
			comparators[d] = new TreeFitnessComparator(fittestFirst, dimensions[d]);
		}
	}

	public int compare(ProbProcessArrayTree tree1, ProbProcessArrayTree tree2) {
		for (TreeFitnessComparator comparator : comparators) {
			TreeFitness f1 = registry.getFitness(tree1);
			TreeFitness f2 = registry.getFitness(tree2);
			int c = comparator.compare(f1, f2);
			if (c != 0) {
				return c;
			}
		}

		//They are the same in every dimension...
		return 0;
	}
}
