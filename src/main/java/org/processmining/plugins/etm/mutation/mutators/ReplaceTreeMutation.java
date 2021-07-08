package org.processmining.plugins.etm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.factory.TreeFactoryCoordinator;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

public class ReplaceTreeMutation extends TreeMutationAbstract {

	private TreeFactoryCoordinator treeFactory;

	public ReplaceTreeMutation(CentralRegistry registry) {
		super(registry);
		treeFactory = new TreeFactoryCoordinator(registry);
	}

	public TreeFactoryCoordinator getTreeFactory() {
		return treeFactory;
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree) {
		return mutate(tree, 0);
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		ProbProcessArrayTree newTree = treeFactory.generateRandomCandidate(registry.getRandom());
		return tree.replace(node, newTree, 0);
	}

}
