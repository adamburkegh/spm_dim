package org.processmining.plugins.etm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;
import org.processmining.plugins.etm.utils.ETMUtils;

/**
 * DISABLED Smartly adds an activity where logmoves are detected.
 */
//FIXME check all class contents
//FIXME UNTESTED code
public class InsertActivityGuided extends TreeMutationAbstract {

	@SuppressWarnings("unused")
	private String key = "insertActivityGuided";

	public InsertActivityGuided(CentralRegistry registry) {
		super(registry);
	}

	/**
	 * @see TreeMutationAbstract#mutate(Node)
	 */
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		ETMUtils.lassert (tree.isConsistent());
		noChange();
		return tree;
	}
}
