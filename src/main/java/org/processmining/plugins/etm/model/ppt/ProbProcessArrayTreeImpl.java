package org.processmining.plugins.etm.model.ppt;

import static org.processmining.plugins.etm.utils.ETMUtils.lassert;

import java.util.Arrays;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.TShortList;

/**
 * This class implements n-ary process trees. Trees are represented by 3 arrays.
 * The root node has index 0.
 * 
 * The children of node m are located at indices m+1 up to but excluding next[m]. 
 * 
 * The type of node m is stored in type[m], where a negative constant is a type for an operator 
 * node and a semi-positive value indicates a leaf. 
 * 
 * The parent of node m is stored in parent[m].
 * 
 * Extended initial version by bfvdonge to handle Probabilistic Process Trees (with weights).
 * 
 * Removed "configurations", which are just a way of dealing with multiple logs, as far as I can see.
 * They add a lot of complexity for little benefit. It appears they were being used as a poor man's
 * Decorator pattern. 
 * 
 * Pending improvement: remove configuration interface methods. 
 * 
 */
public class ProbProcessArrayTreeImpl implements ProbProcessArrayTree {

	// private static Logger LOG = LogManager.getLogger();
	
	public static int ROOT = 0;
	
	/**
	 * The current number of configurations
	 */
	protected static final int NUM_CONFIGURATIONS = 0;
	
	/**
	 * stores the index of the next node in the subtree under m
	 */
	protected final int[] next;

	/**
	 * stores the type of a node m. If the value is negative, it is one of the
	 * given constants AND, OR, XOR, LOOP. If the value if semi-positive a leaf
	 * is implied
	 */
	protected final short[] type;

	/**
	 * stored the index of the parent of a node m. parent[ROOT]=NONE and for all
	 * other elements m, parent[m]>=0
	 */
	protected final int[] parent;

	/**
	 * stores the index of the next leaf
	 */
	protected final int[] leafs;
	
	/**
	 * weights for each node
	 */
	protected final double[] weights;

	private int hashCode = -1;

	public ProbProcessArrayTreeImpl(ProbProcessArrayTree original) {
		int size = original.size();
		this.type = new short[size];
		this.parent = new int[size];
		this.next = new int[size];
		this.leafs = new int[size];
		this.weights = new double[size];
		for (int i = size; i-- > 0;) {
			this.type[i] = original.getTypeFast(i);
			this.parent[i] = original.getParentFast(i);
			this.next[i] = original.getNextFast(i);
			this.leafs[i] = original.getNextLeafFast(i);
			this.weights[i] = original.getWeight(i);
		}

	}

	public ProbProcessArrayTreeImpl() {
		this.type = new short[] {};
		this.parent = new int[] {};
		this.next = new int[] {};
		this.weights = new double[] {};
		this.leafs = new int[parent.length];
	}
	
	public ProbProcessArrayTreeImpl(int next, short type, int parent, double weight) {
		this( new int[] { next}, new short[] { type} , new int[] { parent}, new double[] { weight} );
	}
	
	
	public ProbProcessArrayTreeImpl(int[] next, short[] type, int[] parent, double[] weights) {
		this.type = type;
		this.parent = parent;
		this.next = next;
		this.weights = weights;
		this.leafs = new int[parent.length];
		setupLeafs();

		lassert(isConsistent());
	}

	protected ProbProcessArrayTreeImpl(int size, int numConfigurations) {
		this.type = new short[size];
		this.parent = new int[size];
		this.next = new int[size];
		this.leafs = new int[size];
		this.weights = new double[size];
		setupLeafs();
	}

	public ProbProcessArrayTreeImpl(TIntList next, TShortList type, TIntList parent, TDoubleList weights) {
		this.type = type.toArray();
		this.parent = parent.toArray();
		this.next = next.toArray();
		this.weights = weights.toArray();
		this.leafs = new int[parent.size()];

		setupLeafs();

		lassert(isConsistent());
	}

	
	private void setupLeafs() {
		int last = leafs.length;
		for (int i = leafs.length; i-- > 0;) {
			leafs[i] = last;
			if (isLeaf(i)) {
				last = i;
			}
		}
	}

	public boolean isConsistent() {
		boolean consistent = (this.type.length == this.parent.length) && (this.parent.length == this.next.length);

		/*- invariants
		 * 1) (type[m] >= 0) == (next[m] == m+1); A leaf has no children
		 * 2) (parent[n] == m) => (m + 1 <= n < next[m]); Each node is in the subtree of its parent
		 */

		consistent &= getParent(ROOT) == NONE; // root has no parent
		consistent &= getNext(ROOT) == size(); // next node out of this subtree is the totals size of the tree (e.g. not existent)
		consistent &= getType(ROOT) >= 0 ? (getNext(ROOT) == 1) : true; //if root is leaf then next node out of subtree is node 1, if root is operator then this does not hold

		//Now while consistent, for each node
		for (int i = 1; consistent && i < size(); i++) {
			consistent &= (getType(i) >= 0) == (getNext(i) == i + 1); //if leaf then next node out of subtree is the next node...
			int m = getParent(i);
			consistent &= m >= 0; //parent is root or later
			consistent &= (m + 1 <= i) && (i < getNext(i)); //parent has index lower than child and next node out of this subtree is greater than current node
			//Added by Joos: if the node at i+1 refers to node i as the parent then the Next of i should be => next of i+1
			if (i < size() - 1) {
				if (getParent(i + 1) == i) {
					consistent &= (getNext(i) >= getNext(i + 1));
				}
			}
			//Added by Joos: if operator is LOOP then exactly 3 children
			if (getType(i) == ProbProcessArrayTree.LOOP) {
				if (nChildren(i) != 3) {
					consistent = false;
				}
			}
		}

		return consistent;

	}

	/**
	 * Sets the values of the internal arrays according to the given parameters.
	 * 
	 * @param index
	 * @param values
	 */
	protected void set(int index, short typeVal, int parentVal, int nextVal) {
		type[index] = typeVal;
		parent[index] = parentVal;
		next[index] = nextVal;
	}


	public void setType(int index, short t) {
		type[index] = t;
	}


	public void setType(int par, int n, short t) {
		assert !isLeaf(par);
		setType(getChildAtIndex(par, n), t);
	}


	public ProbProcessArrayTreeImpl swap(int node1, int node2) {
		if (node1 > node2) {
			int t = node1;
			node1 = node2;
			node2 = t;
		} else if (node1 == node2) {
			return new ProbProcessArrayTreeImpl(this);
		}
		lassert(node1 < node2);
		lassert(!isInSubtree(node2, node1));

		ProbProcessArrayTreeImpl tree = new ProbProcessArrayTreeImpl(size(), NUM_CONFIGURATIONS);

		// swapping subtrees is fairly easy, simply loop until node1
		int offset = size(node1) - size(node2);
		int j = 0;
		for (int i = 0; i < node1; i++, j++) {
			tree.set(j, type[i], parent[i], next[i] - (next[j] > node1 && next[j] <= node2 ? offset : 0));
		}
		// then copy in node 2, while changing the locations of the children and parents
		offset = node2 - node1;
		tree.set(j, type[node2], parent[node1], next[node2] - offset);

		j++;
		for (int i = node2 + 1; i < next[node2]; i++, j++) {
			tree.set(j, type[i], parent[i] - (i > node2 ? offset : 0), next[i] - offset);
		}

		// then copy until node 2 is reached.
		offset = size(node1) - size(node2);
		for (int i = next[node1]; i < node2; i++, j++) {
			tree.set(j, type[i], parent[i] - (parent[i] >= node1 ? offset : 0), next[i]
					- (next[i] < next[node2] ? offset : 0));
		}

		// then copy node 1;

		// then copy node 1;
		tree.set(j, type[node1], parent[node2] - (parent[node2] > node1 ? offset : 0), next[node1] + j - node1);

		offset = j - node1;
		j++;
		for (int i = node1 + 1; i < next[node1]; i++, j++) {
			tree.set(j, type[i], parent[i] + offset, next[i] + offset);
		}

		// finally the tail of the tree unchanged
		for (int i = next[node2]; i < size(); i++, j++) {
			tree.set(j, type[i], parent[i], next[i]);
		}
		tree.setupLeafs();
		return tree;
	}


	public ProbProcessArrayTreeImpl add(ProbProcessArrayTree source, int node, int par, int location) {

		// insert the entire subtree under node from source.
		// in total, we add source.size(node) nodes
		int s = source.size(node);
		ProbProcessArrayTreeImpl tree = new ProbProcessArrayTreeImpl(size() + s, NUM_CONFIGURATIONS);

		// copy the nodes until the parent is reached while increasing the 
		// value of next if the current value is greater than par.
		int i;
		for (i = 0; i <= par; i++) {
			tree.set(i, type[i], parent[i], next[i] + (next[i] > par ? s : 0));

		}

		//copy the first few children of parent, while not
		// increasing the value of next.
		int nc = 0;
		for (i = par + 1; nc < location && i < next[par]; i++) {
			tree.set(i, type[i], parent[i], next[i]);
			nc = nc + (i + 1 == size() || parent[i + 1] == par ? 1 : 0);

		}
		int startFrom = i;

		// index i is the index at which the source subtree needs to be
		// inserted
		int offset = i - node;
		tree.set(i, source.getTypeFast(node), par, source.getNextFast(node) + offset);

		i++;
		for (int j = node + 1; j < source.getNextFast(node); j++, i++) {
			tree.set(i, source.getTypeFast(j), source.getParentFast(j) + offset, source.getNextFast(j) + offset);
		}

		// add the remaining part of the tree,
		for (int j = startFrom; j < size(); j++, i++) {
			tree.set(i, type[j], parent[j] + (parent[j] > par ? s : 0), next[j] + s);

		}

		tree.setupLeafs();
		return tree;
	}

	public ProbProcessArrayTree addParent(int node, short newType, byte configurationType) {
		assert newType != LOOP && newType < 0;
		// insert a node of the given type as a parent of node
		// in total, we add 1 nodes

		ProbProcessArrayTreeImpl tree = new ProbProcessArrayTreeImpl(size() + 1, NUM_CONFIGURATIONS);

		// copy the nodes until the node is reached while increasing the 
		// value of next if the current value is greater than node.
		int i;
		for (i = 0; i < node; i++) {
			tree.set(i, type[i], parent[i], next[i] + (next[i] > node ? 1 : 0));
		}

		// add the new node. This node will have one leaf.
		//tree.set(i, newType, parent[i], i + 2);
		//FIX by Joos: no, it will have one CHILD which can be a subtree itself...
		tree.set(i, newType, parent[i], next[i] + 1);

		// copy the rest
		for (i = node; i < size(); i++) {
			tree.set(i + 1, type[i], (parent[i] < node ? parent[i] : parent[i] + 1), next[i] + 1);
		}
		tree.parent[node + 1] = node;

		tree.setupLeafs();
		return tree;
	}

	public ProbProcessArrayTree addChild(int operatorNode, int location, short leafType, byte configurationType) {
		//Updated assertion, adding a child to a loop that has less than 3 is allowed (to make it consistent again)
		lassert( getType(operatorNode) < 0
				&& (getType(operatorNode) != LOOP || (getType(operatorNode) == LOOP && nChildren(operatorNode) < 3)));
		lassert(leafType >= 0);

		// insert the leaf under node
		// in total, we add 1 nodes
		ProbProcessArrayTreeImpl tree = new ProbProcessArrayTreeImpl(size() + 1, NUM_CONFIGURATIONS);

		// copy the nodes until the parent is reached while increasing the 
		// value of next if the current value is greater than par.
		int i;
		for (i = 0; i <= operatorNode; i++) {
			tree.set(i, type[i], parent[i], next[i] + (next[i] > operatorNode ? 1 : 0));
		}

		//copy the first few children of parent, while not
		// increasing the value of next.
		int nc = 0;
		for (i = operatorNode + 1; nc < location && i < next[operatorNode]; i++) {
			tree.set(i, type[i], parent[i], next[i]);
			nc = nc + (i + 1 == size() || parent[i + 1] == operatorNode ? 1 : 0);
		}

		// index i is the index at which the new leaf needs to be inserted
		tree.set(i, leafType, operatorNode, i + 1);

		i++;

		// add the remaining part of the tree,
		for (int j = i - 1; j < size(); j++, i++) {
			tree.set(i, type[j], parent[j] + (parent[j] > operatorNode ? 1 : 0), next[j] + 1);
		}

		tree.setupLeafs();
		return tree;

	}


	public ProbProcessArrayTree replace(int par, int n, ProbProcessArrayTree source, int node) {
		if (isLeaf(par)) {
			return replace(par, source, node);
		} else {
			// compute the id of the node to replace
			return replace(getChildAtIndex(par, n), source, node);
		}
	}

	public ProbProcessArrayTreeImpl replace(int node, ProbProcessArrayTree source, int srcNode) {

		// compute the increment in size
		int incS = source.size(srcNode) - size(node);

		ProbProcessArrayTreeImpl tree = new ProbProcessArrayTreeImpl(size() + incS, NUM_CONFIGURATIONS);

		// we replace the node, hence we copy the first part
		// and the value of next is updated where applicable
		int i;
		for (i = 0; i < node; i++) {
			tree.set(i, type[i], parent[i], next[i] + (next[i] > node ? incS : 0));
		}

		// now copy the node from the source, all indices in parent and next are 
		// decreased to math the new location, except the first parent, which becomes
		// the old parent of node
		int offset = srcNode - i;
		int j = i;
		for (i = srcNode; i < source.getNextFast(srcNode); i++, j++) {
			tree.set(j, source.getTypeFast(i), //
					(i > srcNode ? source.getParentFast(i) - offset : parent[node]), //
					source.getNextFast(i) - offset);

		}

		// now copy the remaining nodes from the original
		for (i = next[node]; i < size(); i++, j++) {
			tree.set(j, type[i],//
					(parent[i] < node ? parent[i] : parent[i] + incS),//
					next[i] + incS);
		}

		tree.setupLeafs();
		return tree;
	}


	public ProbProcessArrayTree remove(int par, int index) {
		if (isLeaf(par)) {
			return new ProbProcessArrayTreeImpl(this);
		}
		return remove(getChildAtIndex(par, index));
	}


	public ProbProcessArrayTree remove(int node) {
		node = getHighestParentWithOneChild(node);
		if (node == 0 || type[parent[node]] == LOOP) {
			return new ProbProcessArrayTreeImpl(this);
		}

		int s = size(node);

		ProbProcessArrayTreeImpl tree = new ProbProcessArrayTreeImpl(size() - s, NUM_CONFIGURATIONS);

		// copy the first part upto node. If next[i] > node then reduce next[i] by size; 
		for (int i = 0; i < node; i++) {
			tree.set(i, type[i], parent[i], next[i] - (next[i] > node ? s : 0));
		}
		int j = node;
		// copy the remainder and reduce parent[i] if necessary
		for (int i = next[node]; i < size(); i++, j++) {
			tree.set(j, type[i], parent[i] - (parent[i] > node ? s : 0), next[i] - s);
		}

		tree.setupLeafs();
		return tree;
	}


	public ProbProcessArrayTree move(int node, int newParent, int location) {
		assert (node > 0 && getType(parent[node]) != LOOP && getType(newParent) != LOOP);

		// first add the subtree into the tree
		final ProbProcessArrayTree tree = add(this, node, newParent, location);

		// then remove node from the new tree.
		if (node < newParent) {
			// the node to be removed resides in the new tree at the same index as in the original tree
			return tree.remove(node);
		} else {
			// the node to be removed has shifted right by the difference in size between the two trees
			return tree.remove(node + (tree.size() - size()));
		}
	}


	public ProbProcessArrayTree move(int par, int n, int newParent, int location) {
		assert getType(par) != LOOP;
		return move(getChildAtIndex(par, n), newParent, location);
	}


	public short getType(int node) {
		return node >= 0 && node < size() ? type[node] : NONE;
	}

	public short getTypeFast(int node) {
		return type[node];
	}

	public int getChildAtIndex(int par, int n) {
		int nc = 0;
		int found = par + 1;
		int j = par + 1;
		while (nc <= n && j < next[par]) {
			if (parent[j] == par) {
				nc++;
				found = j;
			}
			j = next[j];
		}
		// found is the n-th child;
		return found;
	}


	public int size(int node) {
		return next[node] - node;
	}


	public int nChildren(int node) {
		int nc = 0;
		int j = node + 1;
		while (j < next[node]) {
			nc += (parent[j] == node ? 1 : 0);
			j = next[j];
		}
		return nc;
	}


	public boolean isInSubtree(int par, int child) {
		// check if child occurs in the subtree of par
		if (par == 0) {
			return true;
		}
		//return child >= parent && child < next[parent];
		return child >= par && child < next[par];
	}

	/**
	 * returns true if all four internal arrays are identical.
	 */
	public boolean equals(Object o) {
		if (o instanceof ProbProcessArrayTreeImpl) {
			ProbProcessArrayTreeImpl tree = (ProbProcessArrayTreeImpl) o;
			return Arrays.equals(type, tree.type) && Arrays.equals(parent, tree.parent)
					&& Arrays.equals(next, tree.next) ;
		} else {
			return false;
		}
	}

	/**
	 * returns hashcode based on the internal arrays
	 */
	public int hashCode() {
		if (hashCode == -1) {
			hashCode = Arrays.hashCode(type) + 37 * Arrays.hashCode(parent) + 37 * 37 * Arrays.hashCode(next) + 37 * 37
					* 37 ;
		}

		return hashCode;
	}

	/**
	 * returns the highest parent of the given node, such that this parent has
	 * one child and all intermediate parents also have one child.
	 * 
	 * @param node
	 * @return
	 */
	protected int getHighestParentWithOneChild(int node) {
		int par = node;
		//FIXME Joos@Boudewijn please double check if node -> next[node] was a good fix (meaning of next is here still interpreted as last)
		while (par > 0 && nChildren(parent[par]) <= 1) {
			//while (next[par] == node && parent[par] == par - 1) {
			// parent is a parent with one element in its subtree
			par = parent[par];
		}
		return par;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.plugins.etm.model.narytree.NAryTree#isLeaf(int)
	 */
	public boolean isLeaf(int node) {
		return type[node] >= 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.plugins.etm.model.narytree.NAryTree#size()
	 */
	public int size() {
		return parent.length;
	}

	public String toInternalString() {
		return "T:" + toString(type) + "\n" + //
				"N:" + toString(next) + "\n" + //
				"P:" + toString(parent) + "\n";
	}


	public String toString() {
		return TreeUtils.toString(this);
	}

	/* STATIC METHODS */

	private static String toString(int[] a) {
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			if (a[i] >= 0 && a[i] < 10) {
				b.append("  ");
				b.append(a[i]);
			} else if (a[i] >= 10 && a[i] < 100) {
				b.append(" ");
				b.append(a[i]);
			} else if (a[i] >= 100) {
				b.append(a[i]);
			} else {
				if (a[i] == XOR) {
					b.append("XOR");
				} else if (a[i] == OR) {
					b.append("OR ");
				} else if (a[i] == ILV) {
					b.append("ILV ");
				} else if (a[i] == SEQ) {
					b.append("SEQ");
				} else if (a[i] == AND) {
					b.append("AND");
				} else if (a[i] == LOOP) {
					b.append(" LP");
				} else if (a[i] == NONE) {
					b.append("   ");
				} else {
					b.append(a[i]);
				}
			}
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	private static String toString(short[] a) {
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			if (a[i] >= 0 && a[i] < 10) {
				b.append("  ");
				b.append(a[i]);
			} else if (a[i] >= 10 && a[i] < 100) {
				b.append(" ");
				b.append(a[i]);
			} else if (a[i] >= 100 && a[i]!= TAU) {
				b.append(a[i]);
			} else {
				if (a[i] == TAU) {
					b.append("TAU");
				} else if (a[i] == XOR) {
					b.append("XOR ");
				} else if (a[i] == OR) {
					b.append("OR ");
				} else if (a[i] == ILV) {
					b.append("ILV ");
				} else if (a[i] == REVSEQ) {
					b.append("RSQ");
				} else if (a[i] == SEQ) {
					b.append("SEQ");
				} else if (a[i] == AND) {
					b.append("AND");
				} else if (a[i] == LOOP) {
					b.append(" LP");
				} else if (a[i] == NONE) {
					b.append("   ");
				} else if (a[i] == TAU) {
					b.append("TAU");
				} else {
					b.append(a[i]);
				}
			}
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.processmining.plugins.etm.model.narytree.NAryTree#getParent(int)
	 */
	public int getParent(int node) {
		return node > 0 && node < size() ? parent[node] : NONE;
	}

	public int getParentFast(int node) {
		return parent[node];
	}

	/**
	 * returns the first node not in the subtree of the given node .
	 * 
	 * @param node
	 * @return
	 */
	public int getNext(int node) {
		return node > 0 && node < size() ? next[node] : size();
	}

	public int getNextFast(int node) {
		return next[node];
	}

	public int numLeafs() {
		int l = 0;
		int i = 0;
		do {
			l++;
			i = getNextLeafFast(i);
		} while (i < size());
		//		for (int i = 0; i < type.length; i++) {
		//			l += (type[i] >= 0 ? 1 : 0);
		//		}
		return l;
	}

	public int compareTo(ProbProcessArrayTree o) {
		if (size() != o.size()) {
			return size() - o.size();
		} else {
			for (int i = 0; i < size(); i++) {
				if (type[i] != o.getTypeFast(i)) {
					return type[i] - o.getTypeFast(i);
				} else if (next[i] != o.getNextFast(i)) {
					return next[i] - o.getNextFast(i);
				} else if (parent[i] != o.getParentFast(i)) {
					return parent[i] - o.getParentFast(i);
				}
			}
			return 0;
		}
	}

	public int getNextLeaf(int node) {
		return node > 0 && node < size() ? leafs[node] : size();
	}

	public int getNextLeafFast(int node) {
		return leafs[node];
	}

	public void addConfiguration(Configuration configuration) {
		throw new UnsupportedOperationException();
	}

	public void removeConfiguration(int configurationNumber) {
		throw new UnsupportedOperationException();
	}

	public int getNumberOfConfigurations() {
		return NUM_CONFIGURATIONS;
	}

	public Configuration getConfiguration(int configurationNumber) {
		throw new UnsupportedOperationException(); 
	}

	public boolean isBlocked(int configurationNumber, int node) {
		throw new UnsupportedOperationException();
	}

	public boolean isHidden(int configurationNumber, int node) {
		throw new UnsupportedOperationException();
	}

	public boolean isDowngraded(int configurationNumber, int node) {
		throw new UnsupportedOperationException();
	}

	public void setNodeConfiguration(int configurationNumber, int node, byte configurationOption) {
		throw new UnsupportedOperationException();
	}

	public byte getNodeConfigurationFast(int configurationNumber, int node) {
		throw new UnsupportedOperationException();
	}

	public byte getNodeConfiguration(int configurationNumber, int node) {
		throw new UnsupportedOperationException();
	}

	public short getType(int configurationNumber, int node) {
		return getTypeFast(node);
	}

	public short getTypeFast(int configurationNumber, int node) {
		return getTypeFast(node);
	}

	public ProbProcessArrayTree applyConfiguration(int configurationNumber) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Finds the effect of blocking a particular node. If a child of a SEQ or
	 * AND operator is blocked then the operator itself is blocked. The same for
	 * DO and EXIT children of a LOOP operator. Also, if the only (remaining)
	 * child of an operator is blocked, the operator itself is also blocked.
	 * 
	 * @param blockedNode
	 *            the node that is know to be blocked
	 * @return the ID of the node that is also blocked as an effect of the block
	 *         of the provided node. If there is no cascading effect than the
	 *         blockedNode is returned.
	 */
	public int findBlockEffect(int blockedNode) {
		if (blockedNode == 0) {
			//Can not move up!
			return 0;
		}

		int p = getParent(blockedNode);
		short pType = getTypeFast(p);
		//blocking children of SEQ and AND results in that node to be blocked too

		if (pType == ProbProcessArrayTree.SEQ || pType == ProbProcessArrayTree.AND) {
			return findBlockEffect(p);
			//Blocking DO or EXIT of a LOOP results in whole LOOP to be blocked
		} else if (pType == ProbProcessArrayTree.LOOP
				&& (getChildAtIndex(p, 0) == blockedNode || getChildAtIndex(p, 2) == blockedNode)) {
			return p;
			//Also, if the blocked node is the only remaining child then the parent itself is also blocked
		} else if (nChildren(p) == 1) {
			return findBlockEffect(p);
		} else {//no cascading effect...
			return blockedNode;
		}
	}

	/**
	 * Apply all hiding and downgrading configurations of the provided
	 * configuration number. ERGO: blocking is not applied! applyConfiguration
	 * is the preferred method to apply configurations!!! This method only
	 * applies particular configuration options!!!
	 * 
	 * @param configurationNumber
	 * @return
	 */
	public ProbProcessArrayTree applyHidingAndOperatorDowngrading(int configurationNumber) {
		throw new UnsupportedOperationException();
	}

	public int countNodes(short type) {
		int cnt = 0;
		for (int i = this.type.length; i-- > 0;) {
			cnt += this.type[i] == type ? 1 : 0;
		}
		return cnt;
	}

	public void removeAllConfigurations() {
		// no-op
	}

	protected static double expectedCol(int keys, long vals) {
		double n = keys;
		double d = vals;
		return n - d + d * Math.pow(1 - 1 / d, n);
	}


	public int hashCode(int node) {
		if (type[node] >= 0) {
			return (type[node] + 1);
		} else {
			int ch = node + 1;

			assert type[node] != NONE;

			int h = -type[node];
			do {
				h += 31 * hashCode(ch);
				ch = next[ch];
			} while (ch < type.length && parent[ch] == node);
			return h;
		}

	}

	@Override
	public double getWeight() {
		return getWeight(ROOT);
	}

	@Override
	public double getWeight(int node) {
		return weights[node];
	}

}
