package qut.pm.setm.mutation.mutators;

import java.util.Random;

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
 * Randomly instantiates a leaf node and adds it to a randomly chosen parent
 * which will add it to its children
 */
public class AddNodeRandom extends TreeMutationAbstract {

	public AddNodeRandom(CentralRegistry registry) {
		super(registry);
	}

	public ProbProcessTree mutate(ProbProcessTree tree) {
		ProbProcessTreeCheck.exceptIfInconsistent(tree);
		ProbProcessTree target;
		ProbProcessTree origTree = ProbProcessTreeFactory.copy(tree);
		ProbProcessTree mutatedTree;
		ProbProcessTree result;
		int nodeToBeMutated = 0;
		if (tree.isLeaf()) {
			target = origTree;
		}else {
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
				nodeToBeMutated = chooseRandomSubTree(origTree); 
				target = findNodeToBeMutated(origTree, nodeToBeMutated); 
				// loops have a single child only
			} while (target.getType() == PPTOperator.PROBLOOP.ordinal() );
		}
		if (target.isLeaf() ) 
			mutatedTree = addRandomParentToTree(target);
		else 
			mutatedTree = addRandomToNode((ProbProcessTreeNode)target);
		if (nodeToBeMutated == 0) {
			result = mutatedTree;
		}else {
			result = ProbProcessTreeProjector.replaceSubNode(
						(ProbProcessTreeNode)origTree, nodeToBeMutated-1, mutatedTree );
		}
		didChange(nodeToBeMutated, TypesOfTreeChange.ADD);
		ProbProcessTreeCheck.exceptIfInconsistent(result);
		return result;	
	}

	private ProbProcessTree addRandomToNode(ProbProcessTreeNode target) {
		// Add a parent operator?
		Random rng = registry.getRandom();
		if (rng.nextBoolean()) {
			return addRandomParentToTree(target);  
		}
		// ... or a leaf?
		int newType = genRandomLeafType();
		ProbProcessTree leaf = ProbProcessTreeFactory.createLeaf(newType,1.0);
		ProbProcessTreeNode result = ProbProcessTreeFactory.copy(target);
		if (result.getOperator() == PPTOperator.SEQUENCE)
			result.addChild( ProbProcessTreeProjector.rescaleTo(leaf,result.getWeight()) );
		else
			result.addChild(leaf);
		return result; 
	}

	private ProbProcessTree addRandomParentToTree(ProbProcessTree target) {
		ProbProcessTreeNode newParent = genRandomOperatorNode();
		newParent.addChild( ProbProcessTreeFactory.copy(target));
		return newParent;
	}

	private ProbProcessTreeNode genRandomOperatorNode() {
		Random rng = registry.getRandom();
		int oper = rng.nextInt(OPERATORS.length);
		return ProbProcessTreeFactory.createNode(OPERATORS[oper]);
	}

	public boolean changedAtLastCall() {
		return changedAtLastCall;
	}

	public int locationOfLastChange() {
		return locationOfLastChange;
	}
	
	public String getKey() {
		return "AddNodeRandom";
	}

}
