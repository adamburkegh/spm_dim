package qut.pm.setm.mutation.mutators;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;

import qut.pm.setm.mutation.TreeMutationAbstract;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeCheck;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
import qut.pm.spm.ppt.ProbProcessTreeProjector;

/**
 * For Probabilistic Process Trees, there are fewer scenarios where truly lossless node removal is
 * possible. For example, a loop of a loop with one child is redundant in a normal process tree, 
 * but changes the stochastics in a PPT. Nevertheless there are still scenarios, like silent 
 * children in sequences, where nodes can be removed with no information loss. A non-exhaustive
 * list of such rules is in Burke, Sanders & Wynn  (2021) - Discovering Stochastic Process Models 
 * Through Reduction and Abstraction.
 * 
 * @author burkeat
 *
 */
public class RemoveUselessNodes extends TreeMutationAbstract {
	
	private static Logger LOGGER = LogManager.getLogger();

	public RemoveUselessNodes(CentralRegistry registry) {
		super(registry);
	}

	@Override
	public ProbProcessTree mutate(ProbProcessTree tree) {
		if (tree.getChildren().size() == 0)
			return tree;
		ProbProcessTreeNode origTree = (ProbProcessTreeNode)tree;
		ProbProcessTree result = removeUselessChildren(origTree);
		if (result != tree) {
			didChange(0, TypesOfTreeChange.USELESS);
			return result;
		}
		noChange();
		return tree;
	}

	private ProbProcessTree removeUselessChildren(ProbProcessTreeNode origTree) {
		ProbProcessTreeNode result = (ProbProcessTreeNode)ProbProcessTreeFactory.createFrom(origTree);
		for (ProbProcessTree child: origTree.getChildren()) {
			if ( (ProbProcessTreeCheck.uselessChild(origTree, child) )
					&& goAhead()) 
			{
				LOGGER.debug("Found useless child");
			}else {
				ProbProcessTree newChild = child;
				if (!child.isLeaf() ) {
					ProbProcessTreeNode childNode = (ProbProcessTreeNode)child; 
					if (ProbProcessTreeCheck.uselessParent(childNode)) {
						LOGGER.debug("Found useless parent");
						ProbProcessTree grandchild = childNode.getChildren().get(0);
						if (grandchild.isLeaf()) {
							newChild = ProbProcessTreeFactory.copy(grandchild);
						}else {
							newChild = removeUselessChildren((ProbProcessTreeNode)grandchild);
						}
					}else {
						newChild = removeUselessChildren((ProbProcessTreeNode)child);
					}
				}
				// The new child might be useless too
				if (ProbProcessTreeCheck.uselessChild(result, newChild))
					continue;
				if ( ProbProcessTreeCheck.uselessParent(newChild) 
						&& newChild.getChildren().size() == 0)
					continue;
				result.addChild(newChild);
			}
		}
		if (!result.equals(origTree))
			result = (ProbProcessTreeNode)ProbProcessTreeProjector.rescaleTo(result,origTree.getWeight());
		return result;
	}

	protected boolean goAhead() {
		return registry.getRandom().nextBoolean();
	}
}
