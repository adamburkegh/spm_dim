package org.processmining.plugins.etm.mutation.mutators;

import java.util.Random;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * Randomly instantiates a leaf node and adds it to a randomly chosen parent
 * which will add it to its children
 */
public class AddNodeRandom extends TreeMutationAbstract {

	@SuppressWarnings("unused")
	private String key = "AddNodeRandom";

	public AddNodeRandom(CentralRegistry registry) {
		super(registry);
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree) {
		return mutate(tree, 0);
	}

	/**
	 * @see TreeMutationAbstract#mutate(Node)
	 */
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		assert tree.isConsistent();

		Random rng = registry.getRandom();

		//Select a node within the subtree of the given node (including the provided node itself)
		int subtreeSize = tree.getNext(node) - node;
		int selectedNode;

		//This is a nice idea, except for loops, which have a strict 3 child policy
		int nrTries = MAXTRIES;
		do {
			if (nrTries == 0) {
				//ABORT
				noChange();
				return tree;
			}
			//Try to select a none loop node for a limited time
			selectedNode = rng.nextInt(subtreeSize) + node;
			nrTries--;
		} while (tree.getType(selectedNode) == ProbProcessArrayTree.LOOP);

		ProbProcessArrayTree newSubtree;
		//First test the type of the randomly selected node
		if (tree.isLeaf(selectedNode) || rng.nextBoolean()) {
			//Give leafs always, and operator nodes sometimes, a new operator parent with only them as child

			//Get a random parent operator type
			short parentType = TreeUtils.getRandomOperatorType(rng, 2); //Don't restrict type, otherwise we will never introduce Loops in mutation
			//was 4
			if (parentType == ProbProcessArrayTree.LOOP) {
				//For loops, we first need to create the loop as a tree
				ProbProcessArrayTree loopTree = 
						new ProbProcessArrayTreeImpl(new int[] { 4, 2, 3, 4 }, 
												new short[] { ProbProcessArrayTree.LOOP,ProbProcessArrayTree.TAU, ProbProcessArrayTree.TAU, ProbProcessArrayTree.TAU }, 
												new int[] { ProbProcessArrayTree.NONE, 0, 0, 0 },
												new double[] {1.0d, 1.0d, 1.0d, 1.0d});
				assert loopTree.isConsistent();
				//Now copy the selected node into the loop body part of our loop
				ProbProcessArrayTree newLoopTree = loopTree.replace(1, tree, selectedNode);
				//And then replace the selected node by the newly created loop
				newSubtree = tree.replace(selectedNode, newLoopTree, 0);
				assert newSubtree.isConsistent();
			} else { //inserts a new node of type as a parent to the provided node
				newSubtree = tree.addParent(selectedNode, parentType, Configuration.NOTCONFIGURED);
				assert newSubtree.isConsistent();
			}
			didChange(selectedNode, TypesOfTreeChange.OTHER); // 
//			didChange(tree.getParent(selectedNode), NAryTreeHistory.TypesOfChange.OTHER); // 
		} else {
			//Otherwise give the operator node a new child
			short leafClass = registry.getEventClassID(registry.getRandomEventClass(rng));
			int childPos = tree.nChildren(node) > 0 ? rng.nextInt(tree.nChildren(node)) : 0;
			newSubtree = tree.addChild(selectedNode, childPos, leafClass, Configuration.NOTCONFIGURED);
//			didChange(newSubtree.getChildAtIndex(selectedNode, childPos), NAryTreeHistory.TypesOfChange.ADD); // 
			didChange(selectedNode, TypesOfTreeChange.ADD); 
			assert newSubtree.isConsistent();
		}
		assert newSubtree.isConsistent();
		return newSubtree;
	}

	public boolean changedAtLastCall() {
		return changedAtLastCall;
	}

	public int locationOfLastChange() {
		return locationOfLastChange;
	}
}
