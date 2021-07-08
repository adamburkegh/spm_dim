/**
 * 
 */
package org.processmining.plugins.etm.model.ppt;

// Improvement (from jbuijs) Is this the best way? Borja originally had this hacked into main NAryTree class. 
// Wouldn't it be better to outsource this to a map somewhere in the registry?

/**
 * @author jbuijs
 * @author Borja
 *
 * This class extends the regular NAryTreeImpl to keep track of the ancestory
 *
 */
public class ProbProcessTreeImplWithHistory extends ProbProcessArrayTreeImpl {

	/**
	 * Stores the id to correctly retrieve its parent in the history log. -1
	 * means it is from the initial population. It has to be included in the
	 * constructors, otherwise when copying the elite solutions, the new tree
	 * loses the identifier, breaking the family bonds of its tree of life.
	 */
	protected int treeOfLifeID = -1;

	/**
	 * @param original
	 */
	public ProbProcessTreeImplWithHistory(ProbProcessArrayTree original) {
		super(original);
		if (original instanceof ProbProcessTreeImplWithHistory) {
			this.treeOfLifeID = ((ProbProcessTreeImplWithHistory) original).getTreeOfLifeID();
		}
	}

	private int getTreeOfLifeID() {
		return treeOfLifeID;
	}

	/**
	 * @param next
	 * @param type
	 * @param parent
	 */
	public ProbProcessTreeImplWithHistory(int[] next, short[] type, int[] parent, double[] weights) {
		super(next, type, parent, weights);
	}

	/**
	 * @param size
	 * @param numConfigurations
	 */
	public ProbProcessTreeImplWithHistory(int size, int numConfigurations) {
		super(size, numConfigurations);
	}

	public ProbProcessTreeImplWithHistory swap(int node1, int node2) {
		ProbProcessTreeImplWithHistory tree = this.swap(node1, node2);
		tree.setTreeOfLifeID(this.treeOfLifeID);
		return tree;
	}

	public ProbProcessTreeImplWithHistory add(ProbProcessArrayTree source, int node, int par, int location) {
		ProbProcessTreeImplWithHistory tree = this.add(source, node, par, location);
		tree.setTreeOfLifeID(this.treeOfLifeID);
		return tree;
	}

	public ProbProcessTreeImplWithHistory addParent(int node, short newType, byte configurationType) {
		ProbProcessTreeImplWithHistory tree = this.addParent(node, newType, configurationType);
		tree.setTreeOfLifeID(this.treeOfLifeID);
		return tree;
	}

	public ProbProcessTreeImplWithHistory addChild(int operatorNode, int location, short leafType, byte configurationType) {
		ProbProcessTreeImplWithHistory tree = this.addChild(operatorNode, location, leafType, configurationType);
		tree.setTreeOfLifeID(this.treeOfLifeID);
		return tree;
	}

	public ProbProcessTreeImplWithHistory replace(int node, ProbProcessArrayTree source, int srcNode) {
		ProbProcessTreeImplWithHistory tree = this.replace(node, source, srcNode);
		tree.setTreeOfLifeID(this.treeOfLifeID);
		return tree;
	}

	public ProbProcessTreeImplWithHistory remove(int node) {
		ProbProcessTreeImplWithHistory tree = this.remove(node);
		tree.setTreeOfLifeID(this.treeOfLifeID);
		return tree;
	}

	public ProbProcessTreeImplWithHistory move(int node, int newParent, int location) {
		ProbProcessTreeImplWithHistory tree = this.move(node, newParent, location);
		tree.setTreeOfLifeID(this.treeOfLifeID);
		return tree;
	}

	public ProbProcessArrayTree applyHidingAndOperatorDowngrading(int configurationNumber) {
		ProbProcessTreeImplWithHistory tree = new ProbProcessTreeImplWithHistory(
				this.applyHidingAndOperatorDowngrading(configurationNumber));
		tree.setTreeOfLifeID(this.treeOfLifeID);
		return tree;
	}

	private void setTreeOfLifeID(int id) {
		this.treeOfLifeID = id;
	}

}
