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
import qut.pm.spm.ppt.ProbProcessTreeUtils;

/**
 * Randomly mutates/changes a single node
 * 
 */
public class MutateSingleNodeRandom extends TreeMutationAbstract {

	public MutateSingleNodeRandom(CentralRegistry registry) {
		super(registry);
	}

	@Override
	public ProbProcessTree mutate(ProbProcessTree tree) {
		ProbProcessTreeCheck.exceptIfInconsistent(tree);
		ProbProcessTree origTree = ProbProcessTreeFactory.copy(tree);
		ProbProcessTree mutatedTree;
		ProbProcessTree target;
		ProbProcessTree result;
		int nodeToBeMutated = 0;
		if (origTree.isLeaf()) {
			result = mutateLeaf(origTree);
		}else {
			nodeToBeMutated = chooseRandomSubTree(origTree); 
			target = findNodeToBeMutated(origTree, nodeToBeMutated); 
			if (target.isLeaf() )
				mutatedTree = mutateLeaf(target);
			else
				mutatedTree = mutateNode(target);
			if (nodeToBeMutated == 0) {
				result = mutatedTree;
			}else {
				result = ProbProcessTreeProjector.replaceSubNode(
							(ProbProcessTreeNode)origTree, nodeToBeMutated-1, mutatedTree );
			}
		}
		ProbProcessTreeCheck.exceptIfInconsistent(result);
		didChange(nodeToBeMutated, TypesOfTreeChange.OTHER); 
		return result;
	}

	/*
	 * For operator nodes get a new operator type. Now, this is a
	 * good idea, except for loops. Here we choose never to change a
	 * node into a loop and trust on the 'add node random' mutation
	 * to add loop nodes to this parent if it needs looping.
	 */
	private ProbProcessTree mutateNode(ProbProcessTree target) {
		PPTOperator[] operators = PPTOperator.values();
		int newType = -1; 
		do {
			newType = ProbProcessTreeUtils.randomOperatorType(registry.getRandom());
			if (operators[newType] == PPTOperator.PROBLOOP)
				newType = target.getType();
		}while (newType == target.getType());
		ProbProcessTreeNode result = ProbProcessTreeFactory.createNode(operators[newType]);
		addAdjustedChildren(target, newType, result);
		return result;
	}

	private ProbProcessTree mutateLeaf(ProbProcessTree target) {
		int newType;
		do {
			//We might also change the leaf into a tau
			newType = genRandomLeafType();
		}while ( newType == target.getType() );
		return ProbProcessTreeFactory.createLeaf(newType, target.getWeight());
	}

	public String getKey() {
		return "MutateSingleNodeRandom";
	}
}
