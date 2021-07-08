package qut.pm.setm.mutation;

import java.util.List;

import org.processmining.framework.plugin.annotations.KeepInProMCache;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAnnotation;

import qut.pm.spm.ppt.PPTOperator;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
import qut.pm.spm.ppt.ProbProcessTreeProjector;

@KeepInProMCache
@TreeMutationAnnotation
public abstract class TreeMutationAbstract {
	//jbuijs suggestion - make more like fitness (e.g. annotation, info object (focus on dim, smart/dumb, ...)

	protected static final PPTOperator[] OPERATORS = PPTOperator.values();

	/**
	 * A constant that indicates how often a mutation function can try to get to
	 * a correct node before it should give up
	 */
	public static final int MAX_TRIES = 5;

	protected CentralRegistry registry;

	protected String key;

	protected boolean changedAtLastCall = false;
	protected int locationOfLastChange = -1;
	protected TypesOfTreeChange typeOfChange = TypesOfTreeChange.OTHER;

	public TreeMutationAbstract(CentralRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Each tree mutation should define a unique key to be used to identify the
	 * algorithm in maps and such
	 * 
	 * @return key of the mutation
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Calls mutate(Node) on the root and makes sure that the changed node
	 * becomes the new root
	 * 
	 * There is a variant of this in jbuijs to deal with a subtree, but it is never
	 * called except with node=0 (ie, root).
	 * 
	 * @param tree
	 */
	public abstract ProbProcessTree mutate(ProbProcessTree tree);

	/**
	 * Returns true if the last call of the algorithm caused changes to the tree
	 * 
	 * @return
	 */
	public boolean changedAtLastCall() {
		return changedAtLastCall;
	}

	/**
	 * Returns the node closest to where the change was made (e.g. the node
	 * itself or the parent of a deleted node, etc.)
	 * 
	 * @return
	 */
	public int locationOfLastChange() {
		return locationOfLastChange;
	}

	protected void noChange() {
		changedAtLastCall = false;
		locationOfLastChange = -1;
		typeOfChange = TypesOfTreeChange.OTHER;
	}

	protected void didChange(int node, TypesOfTreeChange _typeOfChange) {
		this.changedAtLastCall = true;
		this.locationOfLastChange = node;
		this.typeOfChange = _typeOfChange;
	}

	/**
	 * Can include the tree itself. 
	 * 
	 * @param mutatedTree
	 * @return
	 */
	protected int chooseRandomSubTree(ProbProcessTree mutatedTree) {
		int nodeToBeMutated = registry.getRandom().nextInt(mutatedTree.size() ) ;
		return nodeToBeMutated;
	}
	
	/**
	 * Excludes the root 
	 * 
	 * @param mutatedTree
	 * @return
	 */
	protected int chooseRandomChildTree(ProbProcessTree mutatedTree) {
		int nodeToBeMutated = registry.getRandom().nextInt(mutatedTree.size()-1 ) ;
		return nodeToBeMutated;
	}

	protected ProbProcessTree findNodeToBeMutated(ProbProcessTree mutatedTree, int nodeToBeMutated) {
		ProbProcessTree target;
		if (nodeToBeMutated == 0) {
			target = mutatedTree;
		}else {
			target = ProbProcessTreeProjector.findSubNode((ProbProcessTreeNode)mutatedTree,nodeToBeMutated-1);
		}
		return target;
	}

	protected void addAdjustedChildren(ProbProcessTree target, int newType, ProbProcessTreeNode result) {
		if (PPTOperator.SEQUENCE == OPERATORS[newType])
			addSeqChildren(target, result);
		else
			addNewChildren(target, result);
	}

	private void addSeqChildren(ProbProcessTree target, ProbProcessTreeNode result) {
		List<ProbProcessTree> children = target.getChildren();
		ProbProcessTree firstChild = children.get(0);
		for (ProbProcessTree child: children ) {
			if (firstChild.getWeight() == child.getWeight())
				result.addChild(child);
			else
				result.addChild( 
						ProbProcessTreeProjector.rescale( child , 
								firstChild.getWeight() / child.getWeight() ) );
		}
	}

	private void addNewChildren(ProbProcessTree target, ProbProcessTreeNode result) {
		for (ProbProcessTree child: target.getChildren()) {
			result.addChild(child);
		}
	}
	
	protected int genRandomLeafType() {
		return registry.getRandom().nextInt(registry.getEventClasses().size() + 1) 
				+ ProbProcessTreeFactory.OFFSET-1;
	}
}
