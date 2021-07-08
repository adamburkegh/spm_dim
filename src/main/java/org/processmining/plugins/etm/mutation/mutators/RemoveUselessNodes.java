package org.processmining.plugins.etm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.metrics.SimplicityUselessNodes;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

public class RemoveUselessNodes extends TreeMutationAbstract {

	public RemoveUselessNodes(CentralRegistry registry) {
		super(registry);
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {

		//First try to find useless nodes
		for (int i = node; i < tree.size(); i++) {
			//We prefer to remove useless nodes at random so we might continue 50/50
			if (SimplicityUselessNodes.isUselessNode(tree, i) && registry.getRandom().nextBoolean()) {
				ProbProcessArrayTree mutatedTree = null;
				/*
				 * Now remove that node with the least side effects
				 */
				if (tree.isLeaf(i)) {
					//if the node is a leaf, simply remove the node 
					mutatedTree = tree.remove(i);
				} else if (tree.nChildren(i) == 1) {
					//If the node is not a leaf but has only one child, replace it with its child
					mutatedTree = tree.replace(i, tree, i + 1);
				} else if (tree.getType(i) == ProbProcessArrayTree.LOOP) {
					//If we are a useless loop, replace ourselves with the only child we have that is a non TAU (which will be a loop)
					int goodChild = -1;
					for (int c = 0; c < tree.nChildren(i); c++) {
						if (tree.getType(tree.getChildAtIndex(i, c)) != ProbProcessArrayTree.TAU) {
							goodChild = c;
						}
					}

					//If we found a good child, replace
					if (goodChild > -1) {
						mutatedTree = tree.replace(i, tree, i + 1);
					} else {
						//Otherwise, remove all together...
						mutatedTree = tree.remove(i);
					}
					//Maybe add specific mutation for equal type as parent useless qualification (re-use code from flattening function but local to the parent of i)
				} else {
					//If all children of the node are useless, remove this node and all its children
					boolean allUseless = true;
					for (int c = 0; c < tree.nChildren(i) && allUseless; c++) {
						allUseless &= SimplicityUselessNodes.isUselessNode(tree, tree.getChildAtIndex(i, c));
					}
					if (allUseless) {
						mutatedTree = tree.remove(i);
					}
				}

				if (mutatedTree != null && !mutatedTree.equals(tree)) {
					assert mutatedTree.isConsistent();

					didChange(0, TypesOfTreeChange.USELESS);
					return mutatedTree;
				}
			}//change
		}//for

		noChange();
		return tree;
	}
}
