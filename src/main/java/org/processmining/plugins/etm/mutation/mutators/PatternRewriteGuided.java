package org.processmining.plugins.etm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * Randomly instantiates a leaf node and adds it to a randomly chosen parent
 * which will add it to its children
 */
//FIXME check all class contents
//FIXME Test Class thoroughly
public class PatternRewriteGuided extends TreeMutationAbstract {
	private String key = "PatternRewriteGuided";

	public PatternRewriteGuided(CentralRegistry registry) {
		super(registry);
	}

	/**
	 * @see TreeMutationAbstract#mutate(Node)
	 */
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int n) {
		assert tree.isConsistent();
		
		//FIXME re-think pattern rewrites and implement for our new fancy n-ary trees 
		/*
		 * IDEA: loop with a SEQ above, SEQ right of loop can be in loop-exit
		 * (and v.v.),
		 */

		//Nothing found... :(
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
