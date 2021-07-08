package org.processmining.plugins.etm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * This mutation operation randomly selects a node in the tree and them remove
 * one of it's children and let the surviving child take its place at its parent
 * 
 * @author jbuijs
 * 
 */
public class RemoveSubtreeRandom extends TreeMutationAbstract {

	private String key = "RemoveSubtreeRandom";

	public RemoveSubtreeRandom(CentralRegistry registry) {
		super(registry);
	}

	/**
	 * @see TreeMutationAbstract#mutate(Node)
	 */
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		assert tree.isConsistent();

		//Select a random node, then test if it is feasible to remove it, but don't try endlessly
		int nodeToBeRemoved;
		int nrTries = MAXTRIES;
		do {
			if (nrTries == 0) {
				/*
				 * We don't do anything (and rely on the fact that the mutation
				 * coordinator will trigger another mutation that probably makes
				 * more sense)
				 */
				noChange();
				return tree;
			}

			//Select a random node to remove
			nodeToBeRemoved = registry.getRandom().nextInt(tree.getNext(node) - node) + node;
			nrTries--;
			/*
			 * Try another node as long as we have selected the root, the node
			 * is an only child of a parent (we allow loops, see below)
			 */
		} while (nodeToBeRemoved == 0 || (tree.nChildren(tree.getParent(nodeToBeRemoved)) == 1));

		//If we make it here we managed to select a node that we can remove
		ProbProcessArrayTree newTree;
		if (tree.getType(tree.getParent(nodeToBeRemoved)) == ProbProcessArrayTree.LOOP) {
			//For loops, don't remove but replace by TAU
			ProbProcessArrayTree tauTree = new ProbProcessArrayTreeImpl(1, ProbProcessArrayTree.TAU ,
												ProbProcessArrayTree.NONE, 1.0d );
			newTree = tree.replace(nodeToBeRemoved, tauTree, 0);
			didChange(tree.getParent(nodeToBeRemoved), TypesOfTreeChange.REMOVE); //this is the point of change
		} else {
			//For non-loops, remove!
			newTree = tree.remove(nodeToBeRemoved);
			didChange(tree.getParent(nodeToBeRemoved), TypesOfTreeChange.REMOVE);
		}

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