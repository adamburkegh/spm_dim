package org.processmining.plugins.etm.mutation;

import org.processmining.framework.plugin.annotations.KeepInProMCache;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;

@KeepInProMCache
@TreeMutationAnnotation
public abstract class TreeMutationAbstract {
	//jbuijs suggestion - make more like fitness (e.g. annotation, info object (focus on dim, smart/dumb, ...)

	/**
	 * A constant that indicates how often a mutation function can try to get to
	 * a correct node before it should give up
	 */
	public static final int MAXTRIES = 5;

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
	 * Mutates the given node (only use if you need to mutate a specific sub
	 * tree, not the whole tree). Otherwise use the mutate(Tree) variant.
	 * 
	 * @param node
	 *            Node to mutate on
	 * @return mutated node (could be a new one)
	 */
	public abstract ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node);

	/**
	 * Calls mutate(Node) on the root and makes sure that the changed node
	 * becomes the new root
	 * 
	 * @param tree
	 */
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree) {
		return mutate(tree, 0);
	}

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
}
