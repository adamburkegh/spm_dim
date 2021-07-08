package org.processmining.plugins.etm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * DISABLED 
 */
public class MutateOperatorTypeGuided extends TreeMutationAbstract {

	private String key = "MutateOperatorTypeGuided";

	public MutateOperatorTypeGuided(CentralRegistry registry) {
		super(registry);
	}

	/**
	 * Walks through the tree to find an operator node that behaves differently
	 * than it should. Then corrects the most serious misbehavior.
	 * 
	 * @return changed node
	 */
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		noChange();
		return tree;
	}

	/**
	 * @see TreeMutationAbstract#getKey()
	 */
	public String getKey() {
		return key;
	}
}
