package org.processmining.plugins.etm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.BehaviorCounter;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * DISABLED!!!
 */
//FIXME check all class contents
//FIXME untested code
public class RemoveActivityGuided extends TreeMutationAbstract {

	private String key = "removeActivityGuided";

	public RemoveActivityGuided(CentralRegistry registry) {
		super(registry);
	}

	/**
	 * @see TreeMutationAbstract#mutate(Node)
	 */
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		assert tree.isConsistent();

		double mostMMfraction = 0;
		int mostMMNode = -1;

		//1. loop through all nodes, searching for a log move
		int endOfSubtree = tree.getNext(node);
		BehaviorCounter b = registry.getFitness(tree).behaviorCounter;
		for (int i = node; i < endOfSubtree; i++) {

			@SuppressWarnings("unused")
			int parent = tree.getParent(i);

			double fraction = b.getASyncMoveCount()[i] / (b.getSyncMoveCount()[i] + 1.0);
			if (fraction > mostMMfraction) {
				mostMMfraction = fraction;
				mostMMNode = i;
			}

			//OLD IMPL:
			/*
			 * If that parent is not an XOR or OR (which allows for skipping)
			 * AND if the parent is skipped less than ourselves (if not, please
			 * select the parent :))
			 */

			//FIXME correct code for n-ary trees and new behavior counter
			/*-* /
			if (parent >= 0 && tree.getType(parent) != NAryTree.XOR && tree.getType(parent) != NAryTree.OR) {
				
				int notUsed = b.getNotUsed();
				if (tree.isLeaf(i))
					notUsed = b.getBehavedAsR(); //leafs are not used if behaved as R...
				if (notUsed > registry.getFitness(tree).behaviorCounter[parent].getNotUsed() && notUsed > mostMM) {
					//Remember this node if it is even less used than the one we remembered before
					//or by chance we won't...
					if (registry.getRandom().nextBoolean() || mostMM == 0) {
						mostMM = notUsed;
						mostMMNode = i;
					}
				}
			}/**/
		}

		//If the tree has no model moves then don't even dare to improve it
		if (mostMMfraction == 0) {
			noChange();
			return tree;
		}

		//Otherwise, improve it
		didChange(tree.getParent(mostMMNode), TypesOfTreeChange.REMOVE); //this is the point of change, the parent of the removed node

		//Return the tree with the most model-moved node removed
		ProbProcessArrayTree newTree = tree.remove(mostMMNode);
		assert newTree.isConsistent();
		return newTree;
	}

	/**
	 * @see TreeMutationAbstract#getKey()
	 */
	public String getKey() {
		return key;
	}
}
