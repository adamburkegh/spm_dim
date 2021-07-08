package org.processmining.plugins.etm.mutation.mutators;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

/**
 * Shuffles the order of the nodes in a randomly selected cluster
 */
//FIXME check all class contents
//FIXME UNTESTED code
public class ShuffleCluster extends TreeMutationAbstract implements EvolutionaryOperator<ProbProcessArrayTree> {

	private String key = "ShuffleChildren";

	public ShuffleCluster(CentralRegistry registry) {
		super(registry);
	}

	public List<ProbProcessArrayTree> apply(List<ProbProcessArrayTree> selectedCandidates, Random rng) {
		List<ProbProcessArrayTree> newTrees = new ArrayList<ProbProcessArrayTree>(selectedCandidates.size());
		for (ProbProcessArrayTree tree : selectedCandidates) {
			newTrees.add(mutate(tree));
		}
		return newTrees;
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		return mutate(tree, node, true);
	}

	/**
	 * Shuffles a selected cluster
	 * 
	 * @param node
	 *            node from which to select a cluster
	 * @param maintainBehavior
	 *            If true then the behavior is not changed (SEQ and LOOP are
	 *            left alone)
	 * @return
	 */
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node, boolean maintainBehavior) {
		assert tree.isConsistent();
		
		Random rng = registry.getRandom();

		//First select a node to shuffle
		int selectedNode = -1;
		int nrTries = MAXTRIES; //max number of tries, when we reach 0: game over, no cigar
		do {
			int suggestedNode = rng.nextInt(tree.size());
			//Leafs and nodes with only 1 child can not be shuffled
			if (tree.nChildren(suggestedNode) > 1) {
				short suggestedNodeType = tree.getType(suggestedNode);
				if (!(maintainBehavior && (suggestedNodeType == ProbProcessArrayTree.SEQ || suggestedNodeType == ProbProcessArrayTree.LOOP))) {
					//Now this node can be the selected node
					suggestedNode = selectedNode;
				}
			}
			nrTries--;
		} while (selectedNode != -1 && nrTries > 0);

		//We tried to find a node to shuffle but did not find one
		if (selectedNode == -1) {
			noChange();
			return tree;
		}

		//Now select two child nodes (there are at least two) from the selected node and swap them
		int firstChild = rng.nextInt(tree.nChildren(selectedNode));

		int secondChild = rng.nextInt(tree.nChildren(selectedNode) - 1); //There is one node we don't want to select
		if (secondChild >= firstChild)
			secondChild++; //prevent selecting the same child twice

		didChange(selectedNode, TypesOfTreeChange.OTHER);
		//And swap
		ProbProcessArrayTree newTree = tree.swap(firstChild, secondChild);
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
