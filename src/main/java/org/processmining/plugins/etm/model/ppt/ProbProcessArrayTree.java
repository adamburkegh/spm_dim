package org.processmining.plugins.etm.model.ppt;


/**
 * Early experiment in incremental change on jbuijs Process Tree into a PPT. Decided against
 * using interface. 
 * 
 * TODO Delete this and related classes.
 * 
 * @author burkeat
 *
 */
public interface ProbProcessArrayTree extends Comparable<ProbProcessArrayTree> {

	// Would be good to move these constants to an Enum
	
	/**
	 * indicates the type for tau-LEAF
	 */
	public static final short TAU = Short.MAX_VALUE;

	/**
	 * indicates the type XOR
	 */
	public static final short XOR = -1;
	/**
	 * indicates the type AND
	 */
	public static final short AND = -2;
	/**
	 * indicates the type OR
	 */
	public static final short OR = -3;
	/**
	 * indicates the type LOOP
	 */
	public static final short LOOP = -4;
	/**
	 * indicates the type SEQ
	 */
	public static final short SEQ = -8;
	/**
	 * indicates the type REVSEQ
	 */
	public static final short REVSEQ = -16;
	/**
	 * indicates the type INTERLEAVED
	 */
	public static final short ILV = -32;
	/**
	 * constant for no child/parent
	 */
	public static final short NONE = -64;

	/**
	 * Sets the type of the node. A semi-positive type indicates a leaf, a
	 * negative type should be one of the constants SEQ, REVSEQ LOOP, OR, XOR,
	 * AND
	 * 
	 * @param index
	 *            Index of the node to update
	 * @param t
	 *            Type to set the node to
	 */
	public void setType(int index, short t);

	/**
	 * sets the type of the n-th child of parent par. A semi-positive type
	 * indicates a leaf, a negative type should be one of the constants SEQ,
	 * LOOP, OR, XOR, AND
	 * 
	 * @param par
	 *            the parent. This should not be a leaf.
	 * @param n
	 *            the child number
	 * @param t
	 */
	public void setType(int par, int n, short t);

	/**
	 * returns a new tree with the two nodes swapped. The two nodes should not
	 * be in a hierarchical relationship
	 * 
	 * @param node1
	 *            First node to swap
	 * @param node2
	 *            Other node to swap with
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree swap(int node1, int node2);

	/**
	 * Returns a new tree where the subtree below the node at index node in the
	 * source tree is copied into this tree under node with index parent at
	 * location index. If index =0, then the new subtree becomes the leftmost
	 * branch of the new parent. If index > the number of children then it
	 * becomes the rightmost child
	 * 
	 * The parent node cannot be a loop or a leaf
	 * 
	 * It is assumed that this tree and the given tree have an equal number of
	 * configurations. The configuration options set per node are copied from
	 * the source trees.
	 * 
	 * @param source
	 *            NAryTree to add a node from
	 * @param node
	 *            Node to add from the source tree to the current tree
	 * @param parent
	 *            Node to add the source node to
	 * @param index
	 *            Child index of the parent to add the node in (if index >
	 *            numChildren then new node becomes rightmost child)
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree add(ProbProcessArrayTree source, int node, int par, int location);

	/**
	 * Returns a new tree in which node from source is replaced by a new node of
	 * the given type and where the existing node becomes a child of this newly
	 * added parent.
	 * 
	 * @param node
	 *            a node in the tree that gets a new parent (can be any node)
	 * @param type
	 *            the type of the new parent (cannot be LOOP, TAU or any leaf
	 *            type)
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree addParent(int node, short type, byte configurationType);

	/**
	 * Returns a new tree in which a child is added to the operator at the given
	 * index
	 * 
	 * 
	 * @param operatorNode
	 *            node to which a child needs to be added (cannot be a leaf or a
	 *            LOOP)
	 * @param index
	 *            the index at which the child should be added. 0 means the new
	 *            child becomes the leftmost, and any value greater than the
	 *            number of children implies the new node becomes the rightmost.
	 * 
	 * @param leafType
	 *            the new type of the new leaf node to be inserted
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree addChild(int operatorNode, int index, short leafType, byte configurationType);

	/**
	 * replaces the n-th child of parent with the node from source.
	 * 
	 * It is assumed that this tree and the given tree have an equal number of
	 * configurations. The configuration options set per node are copied from
	 * the source trees.
	 * 
	 * @param par
	 *            a node. If par is a leaf, then the parameter n is ignored
	 * @param n
	 *            the child to replace. if n==0, the leftmost child is replaced,
	 *            if n>= nChildren the rightmost is.
	 * @param source
	 *            NAryTree to get the new node from
	 * @param node
	 *            Node from the source tree to be the replacement in this tree
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree replace(int par, int n, ProbProcessArrayTree source, int node);

	/**
	 * replaces node from this tree with the node from the source tree
	 * 
	 * It is assumed that this tree and the given tree have an equal number of
	 * configurations. The configuration options set per node are copied from
	 * the source trees.
	 * 
	 * @param node
	 *            Node in this tree to be replaced
	 * @param source
	 *            NAryTree to get the new node from
	 * @param srcNode
	 *            Node from the source tree to be the replacement in this tree
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree replace(int node, ProbProcessArrayTree source, int srcNode);

	/**
	 * removes the child at index from node par. if index >= nChildren(parent)
	 * then the right-most child is removed. If parent is a leaf, then nothing
	 * is removed and a clone is returned;
	 * 
	 * @param par
	 *            the parent of the node to be removed. If par is a aleaf, a
	 *            clone is returned
	 * @param index
	 *            Child index of the node to be removed
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree remove(int par, int index);

	/**
	 * removes the node at index node from the tre
	 * 
	 * @param node
	 *            Index of the node to be removed
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree remove(int node);

	/**
	 * returns the number of leafs in the tree.
	 * 
	 * @return Number of leafs in the tree
	 */
	public int numLeafs();

	/**
	 * moves the given node by removing it from its parent and adding is under
	 * the new parent. The node should not be the root and both the existing and
	 * new parent should not be loops.
	 * 
	 * @param node
	 *            Node to be (re)moved
	 * @param newParent
	 *            Index of the new parent
	 * @param location
	 *            Child index of the moved node in the parent node
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree move(int node, int newParent, int location);

	/**
	 * moves the n-th child of the parent "par" by removing it from the tree and
	 * inserting it as a new child under newParent. Both the existing and new
	 * parent should not be loops.
	 * 
	 * @param par
	 *            Index of the parent of the node to be moved
	 * @param location
	 *            Child index of the node to be moved
	 * @param newParent
	 *            Index of the new parent of the moved node
	 * @param newLocation
	 *            Child location in the parent of the moved node
	 * @return NAryTree clone with this operation applied
	 */
	public ProbProcessArrayTree move(int par, int location, int newParent, int newLocation);

	/**
	 * returns the type of a node.
	 * 
	 * @param node
	 *            Index of the node to get the type for
	 * @return short Node type
	 */
	public short getType(int node);

	/**
	 * returns the type of a node. (without sanity checks, e.g. might result in
	 * exceptions)
	 * 
	 * @param node
	 *            Index of the node to get the type for
	 * @return short Node type
	 */
	public short getTypeFast(int node);
	
	/**
	 *  
	 * @return Weight of the parent node of this tree. 
	 */
	public double getWeight();

	/**
	 *  
	 * @return Weight of the given node
	 *
	 * @param node 
	 */
	public double getWeight(int node);

	/**
	 * returns the type of a node.
	 * 
	 * @param node
	 *            Index of the node to get the type for
	 * @return short Node type
	 */
	public short getType(int configurationNumber, int node);

	/**
	 * returns the type of a node. (without sanity checks, e.g. might result in
	 * exceptions)
	 * 
	 * @param node
	 *            Index of the node to get the type for
	 * @return short Node type
	 */
	public short getTypeFast(int configurationNumber, int node);

	/**
	 * returns the id of the n-th child of par. if n ==0 then the leftmost child
	 * is returned, if n>= the number of children the rightmost child is
	 * returned.
	 * 
	 * @param par
	 *            a non-leaf node from which the index of the n-th child to get
	 *            from
	 * @param n
	 *            -th child to return index for
	 * @return int Index of that child
	 */
	public int getChildAtIndex(int par, int n);

	/**
	 * returns the size of the subtree under node
	 * 
	 * @param node
	 *            Index of the node to get the size for
	 * @return Number of nodes under the given node, including that node
	 */
	public int size(int node);

	/**
	 * returns the number of children of a node.
	 * 
	 * @param node
	 *            Index of the node to get the number of children for
	 * @return Number of children
	 */
	public int nChildren(int node);

	/**
	 * returns if the child appears in the subtree of the parent
	 * 
	 * @param par
	 *            Index of the parent node (/root of subtree) to search
	 * @param child
	 *            Index (absolute) of the child node to search for
	 * @return boolean TRUE if child is in subtree of (uber)parent
	 */
	public boolean isInSubtree(int par, int child);

	/**
	 * returns true if a node is a leaf.
	 * 
	 * @param node
	 *            Index of the node to check
	 * @return boolean TRUE if given node is of type leaf
	 */
	public boolean isLeaf(int node);

	/**
	 * returns the number of nodes in the tree
	 * 
	 * @return Size of the whole tree
	 */
	public int size();

	/**
	 * returns the parent of the given node. If the node is the root, or not in
	 * the tree, then NONE is returned
	 * 
	 * @param node
	 *            Index of the node to get the parent for
	 * @return index of the parent of given node
	 */
	public int getParent(int node);

	/**
	 * returns the first node larger than the given node, which is not part of
	 * the subtree of the given node. May return size() if no such node exists.
	 * 
	 * @param node
	 *            Index of the node to get the first outside the subtree for
	 * @return Index of first node not part of subtree of given node
	 */
	public int getNext(int node);

	/**
	 * returns the parent of the given node. If the node is the root, or not in
	 * the tree, then NONE is returned. Fast means not sanity checks e.g.
	 * exceptions may occur
	 * 
	 * @param node
	 *            Index of the node to get the parent for
	 * @return index of parent node
	 */
	public int getParentFast(int node);

	/**
	 * returns the first node larger than the given node, which is not part of
	 * the subtree of the given node. May return size() if no such node exists..
	 * Fast means no sanity checks e.g. exceptions may occur
	 * 
	 * @param node
	 *            Index of the node to get the next node outside this subtree
	 *            for
	 * @return Index of next node outside of subtree.
	 */
	public int getNextFast(int node);

	/**
	 * returns the leaf greater than the given node. May return size() if no
	 * such leaf exists
	 * 
	 * @param node
	 *            Index of the node to get the next leaf for
	 * @return Index of next leaf
	 */
	public int getNextLeaf(int node);

	/**
	 * returns the leaf greater than the given node. May return size() if no
	 * such leaf exists. Fast means no sanity checks e.g. exceptions may occur
	 * 
	 * @param node
	 *            Index of the node to get the next leaf for
	 * @return Index of next leaf
	 */
	public int getNextLeafFast(int node);

	/**
	 * returns a string representation of the internal datastructures of the
	 * tree (for debugging only)
	 * 
	 * @return
	 */
	public String toInternalString();

	/**
	 * returns true if the tree is (internally) consistent
	 * 
	 * @return
	 */
	public boolean isConsistent();

	/**
	 * adds the given configurations to the list of configurations.
	 * 
	 * @param blockeable
	 * @param hideable
	 */
	public void addConfiguration(Configuration configuration);

	/**
	 * Returns the configuration stored at the given index.
	 * 
	 * @param configurationNumber
	 * @return
	 */
	public Configuration getConfiguration(int configurationNumber);

	/**
	 * Applies a configuration to the tree and returns the result. The applied
	 * configuration should not result in deadlock, i.e. no blocking under SEQ,
	 * REVSEQ, AND nodes or as a non-middle child of a LOOP.
	 * 
	 * Not all children of a node may be blocked (at least one is not blocked)
	 * 
	 * Any blocked subtree is completely removed from the tree
	 * 
	 * Any hidden subtree is replaced by a tau
	 * 
	 * @param configurationNumber
	 * @return
	 */
	public ProbProcessArrayTree applyConfiguration(int configurationNumber);

	/**
	 * Removes the configuration at the given index from the list of
	 * configurations.
	 * 
	 * @param configurationNumber
	 */
	public void removeConfiguration(int configurationNumber);

	/**
	 * Returns the number of configurations.
	 * 
	 * @return
	 */
	public int getNumberOfConfigurations();

	/**
	 * returns true if the given node is blocked in the given configuration
	 * 
	 * @param configurationNumber
	 * @param node
	 * @return
	 */
	public boolean isBlocked(int configurationNumber, int node);

	/**
	 * returns true if the given node is hidden in the given configuration
	 * 
	 * @param configurationNumber
	 * @param node
	 * @return
	 */
	public boolean isHidden(int configurationNumber, int node);

	/**
	 * Sets the configuration option for a node. Should be either of the
	 * constants BLOCKED, HIDDEN or NOTCONFIGURABLE
	 * 
	 * @param configurationNumber
	 * @param configurationOption
	 */
	public void setNodeConfiguration(int configurationNumber, int Node, byte configurationOption);

	/**
	 * returns the configuration of the node in the given configuration. Returns
	 * one of the constants BLOCKED, HIDDEN or NOTCONFIGURABLE
	 * 
	 * @param configurationNumber
	 * @param node
	 * @return
	 */
	public byte getNodeConfiguration(int configurationNumber, int node);

	/**
	 * counts the number of nodes of the given type
	 * 
	 * @param node
	 * @return
	 */
	public int countNodes(short type);

	public int findBlockEffect(int node);

	public void removeAllConfigurations();

}