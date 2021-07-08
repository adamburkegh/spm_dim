package qut.pm.setm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;

import qut.pm.setm.mutation.TreeMutationAbstract;
import qut.pm.spm.ppt.PPTOperator;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeCheck;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
import qut.pm.spm.ppt.ProbProcessTreeProjector;

/**
 * This mutation operation randomly selects a node in the tree and them remove
 * one of it's children and let the surviving child take its place at its parent
 * 
 * 
 */
public class RemoveSubtreeRandom extends TreeMutationAbstract {

	public RemoveSubtreeRandom(CentralRegistry registry) {
		super(registry);
	}

	public ProbProcessTree mutate(ProbProcessTree tree) {
		ProbProcessTreeCheck.exceptIfInconsistent(tree);
		ProbProcessTree target;
		//Select a random node, then test if it is feasible to remove it, but don't try endlessly
		int nodeToBeRemoved;
		int nrTries = MAX_TRIES;
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
			nodeToBeRemoved = chooseRandomChildTree(tree); 
			target = findNodeToBeMutated(tree, nodeToBeRemoved); 
			nrTries--;
			/*
			 * Try another node if the node
			 * is an only child of a parent (we allow loops, see below)
			 */
		} while ( target.getChildren().size() == 1 );

		//If we make it here we managed to select a node that we can remove
		ProbProcessTreeNode newTree = (ProbProcessTreeNode)ProbProcessTreeFactory.copy(tree);
		if (target.getType() == PPTOperator.PROBLOOP.ordinal() ) {
			//For loops, don't remove but replace by TAU
			ProbProcessTree tauTree = ProbProcessTreeFactory.createSilent(1.0d);
			ProbProcessTreeProjector.replaceSubNode(newTree,nodeToBeRemoved,tauTree);
		} else {
			//For non-loops, remove!
			newTree = ProbProcessTreeProjector.removeSubNode(newTree,nodeToBeRemoved);
		}
		didChange(nodeToBeRemoved, TypesOfTreeChange.REMOVE); //this is the point of change
		ProbProcessTreeCheck.exceptIfInconsistent(newTree);
		return newTree;
	}

	public String getKey() {
		return "RemoveSubtreeRandom";
	}
}