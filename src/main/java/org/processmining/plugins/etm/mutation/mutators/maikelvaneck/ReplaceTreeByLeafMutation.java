package org.processmining.plugins.etm.mutation.mutators.maikelvaneck;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * 
 * @author Maikel van Eck
 */
public class ReplaceTreeByLeafMutation extends TreeMutationAbstract {

	public ReplaceTreeByLeafMutation(CentralRegistry registry) {
		super(registry);
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree) {
		return mutate(tree,0);
	}
	
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		ProbProcessArrayTree newTree = LeafFactory.generateRandomCandidate(registry);
		return tree.replace(node, newTree, 0);
	}
	
}
