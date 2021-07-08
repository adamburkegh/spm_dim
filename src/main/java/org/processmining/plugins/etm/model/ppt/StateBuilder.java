package org.processmining.plugins.etm.model.ppt;

import gnu.trove.iterator.TIntIterator;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import nl.tue.storage.impl.CompressedStoreHashSetImpl.Result;

/**
 * This class stores the state of a tree. It is able to show enabled nodes and
 * to provide the next state when one of the enabled nodes is executed
 * 
 * @author bfvdonge
 * 
 */
public class StateBuilder {

	/**
	 * Interface used in the execution of en enabled node in the current state.
	 * The execution of a node may result in the enabling of new nodes. Some of
	 * these nodes will have to execute in the future (there's no way to disable
	 * them otherwise) and hence these should be executed in a greedy fashion.
	 * However, rather than letting the state take care of this, a queue is used
	 * which can be controlled by the caller, thus leaving the execution of
	 * these nodes in the caller's control.
	 * 
	 * @author bfvdonge
	 * 
	 */
	public static interface StateQueue {
		/**
		 * ads the given node to the queue
		 * 
		 * @param node
		 */
		public void add(int node);

		/**
		 * removes the node at the front of the queue and returns it. This
		 * method should not be called if isEmpty() returns true. The result is
		 * then undefined and the resulting state of the object inconsistent.
		 * 
		 * @return
		 */
		public int poll();

		/**
		 * true if the queue is empty
		 * 
		 * @return
		 */
		public boolean isEmpty();
	}

	private static class DefaultStateQueue implements StateQueue {

		private final int[] queue;

		private int wi;
		private int ri;
		private int pow;

		public DefaultStateQueue(ProbProcessArrayTree tree) {
			// the maximum number of elements in the queue is the number of leafs, since the worst case is
			// that every leaf has it's parent queued. Nodes cannot be queued twice and nodes
			// which are in a hierarchical relationship can also not be queued. Hence the number
			// of leafs is the maximum. We then need 1 spot free to denote the difference between the start en
			// the end of the queue, otherwise we couldn't tell whether the queue was full or empty.
			int max = tree.numLeafs() + 1;
			// find the first power of 2 larger than max and use that as the queue size
			pow = 1;
			while (pow < max) {
				pow = pow << 1;
			}
			queue = new int[pow];
			// reduce pow by one to get the bitmask for cheap modulo operations later.
			pow--;
			wi = 0;
			ri = 0;
		}

		public void add(int node) {
			queue[wi] = node;
			wi = (wi + 1) & pow;//% queue.length;
		}

		/**
		 * method is not fail-safe. Calling poll() on an empty queue results in
		 * an unstable state of the queue which cannot be repaired
		 */
		public int poll() {
			int node = queue[ri];
			ri = (ri + 1) & pow;//% queue.length;
			return node;
		}

		public boolean isEmpty() {
			return wi == ri;
		}

		public String toString() {
			if (isEmpty()) {
				return "[]";
			}
			StringBuilder b = new StringBuilder();
			b.append('[');
			int i = ri;
			do {
				b.append(queue[i]);
				i = (i + 1) & pow;
				if (i < wi) {
					b.append(',');
				}
			} while (i < wi);
			return b.append(']').toString();
		}
	}

	/**
	 * stateQueue that does not store anything. isEmpty() returns true, poll()
	 * returns Integer.MIN_VALUE and add() ignores any call to it.
	 * 
	 * This queue can be (is) used if the caller does not want to use a queue.
	 */
	public static StateQueue MEMORYLESSQUEUE = new StateQueue() {

		public int poll() {
			return Integer.MIN_VALUE;
		}

		public boolean isEmpty() {
			return true;
		}

		public void add(int node) {
			// ignore
		}
	};

	/**
	 * constant indicating no token is present in the subtree under a node
	 */
	public static final int N = 0 & 0xFF;
	/**
	 * constant indicating the current node is enabled
	 */
	public static final int E = 1 & 0xFF;
	/**
	 * constant indicating the current node is not enabled, but there are
	 * enabled nodes in the subtee. This constant is not used for nodes of type
	 * OR, hence P == T.
	 */
	public static final int P = 3 & 0xFF;
	/**
	 * constant indicating the current node of type OR is ready to terminate. It
	 * is counted as an enabled node. Since P is not used for OR, T==P
	 */
	public static final int T = 3 & 0xFF;
	/**
	 * constant indicating the current node is not enabled yet, but will be in
	 * the future
	 */
	public static final int F = 2 & 0xFF;

	protected final ProbProcessArrayTree tree;

	protected final StateQueue queue;

	protected final int configurationNumber;

	private boolean pushDownUnderAND = true;

	private boolean allowImplicitOrTermination = true;

	protected final int treeSize;

	/**
	 * initialize the statebuilder. states are simply byte arrays. A default
	 * queue is initialized which allows for the queueing of as many nodes as
	 * there are leafs.
	 * 
	 * @param tree
	 */
	public StateBuilder(ProbProcessArrayTree tree, int configuationNumber) {
		this(tree, configuationNumber, true);
	}

	/**
	 * initialize the statebuilder. states are simply byte arrays. If useQueue
	 * is true, a queue is initialized which allows for the queueing of as many
	 * nodes as there are leafs. If useQueue is false, then a memoryless queue
	 * is initialized.
	 * 
	 * @param tree
	 */
	public StateBuilder(ProbProcessArrayTree tree, int configurationNumber, boolean useQueue) {
		this.tree = tree;
		this.treeSize = tree.size();
		this.configurationNumber = configurationNumber;
		this.queue = useQueue ? new DefaultStateQueue(tree) : MEMORYLESSQUEUE;
	}

	public StateQueue getQueue() {
		return queue;
	}

	public ProbProcessArrayTree getTree() {
		return tree;
	}

	public int getConfigurationNumber() {
		return configurationNumber;
	}

	/**
	 * return the number of bytes needed to store the state for the tree on
	 * which this builder is used.
	 * 
	 * @return 1 + ((treeSize - 1) / 4)
	 */
	public int getStateSizeInBytes() {
		return 1 + ((treeSize - 1) / 4);
	}

	/**
	 * initializes an empty state, with the root enabled
	 * 
	 * @param numNodes
	 */
	public byte[] initializeState() {
		int s = getStateSizeInBytes();
		byte[] state = new byte[s];
		setState(state, 0, E);
		return state;
	}

	/**
	 * initializes a state of the given size
	 * 
	 * @param size
	 */
	private byte[] copyState(byte[] state) {
		byte[] newState = new byte[state.length];
		System.arraycopy(state, 0, newState, 0, state.length);
		return newState;
		//		return Arrays.copyOf(state, state.length);
	}

	public boolean isEnabled(byte[] state, int node) {
		return (getState(state, node) == E || (getState(state, node) == T && tree
				.getTypeFast(configurationNumber, node) == ProbProcessArrayTree.OR)) && !tree.isBlocked(configurationNumber, node);
	}

	public int getState(byte[] state, int node) {
		int idx = node >>> 2;
		return (state[idx] >> 2 * (node & 3)) & 3;
	}

	private void setState(byte[] state, int node, int s) {
		int idx = node >>> 2;
		int bm = (3 << 2 * (node & 3));
		// erase bits
		state[idx] ^= (state[idx] & bm);
		// write bits for s
		state[idx] |= (s << 2 * (node & 3));
	}

	public boolean equals(byte[] state, byte[] state2) {
		return Arrays.equals(state, state2);
	}

	public int hashCode(byte[] state) {
		return Arrays.hashCode(state);
	}

	/**
	 * execute the given node . This node should be enabled in this state, i.e.
	 * isEnabled(node) == true.
	 * 
	 * This method calls execute(tree, node, MEMORYLESSQUEUE)
	 * 
	 * @param node
	 * @return
	 */
	public byte[] execute(byte[] state, int node) {
		return execute(state, node, MEMORYLESSQUEUE);
	}

	/**
	 * execute the given node. This node should be enabled in this state, i.e.
	 * isEnabled(node) == true.
	 * 
	 * The queue is filled with new nodes that should be executed immediately
	 * from the new state. If node is an AND node, then all children are added
	 * to the queue. If node is a SEQ or LOOP node then the leftmost child is
	 * added to the queue. For all other operators, the child is added to the
	 * queue if it is an only child. Finally, if node is a leaf, then nodes
	 * that, as a consequence of executing the leaf, now have to be executed are
	 * added to the queue. Note that leafs are never added to the queue.
	 * 
	 * To reach the next state, the advised code is:
	 * 
	 * <code> 
	 *  // initialize a queue. This necessary only once for the entire tree.
	 *  StateQueue queue = builder.getQueue();
	 *    
	 *  // current is the current state.
	 *  byte[] current;
	 *  
	 *  // add the node that needs execution to the queue
	 *  queue.add(node);
	 *  
	 *  // set the next state to be the current one 
	 *  byte[] next = current; 
	 *  while (!queue.isEmpty()) {
	 *  	// continue while the queue is not empty
	 *      // and overwrite next with the state obtained from
	 *      // executing the next element from the queue.
	 *      next = execute(next, queue.poll(), queue);
	 *  }
	 * </code>
	 * 
	 * failing to properly empty the queue using the code above may lead to
	 * unexpected results as the queue has a maximum capacity equal to the
	 * number of leafs and if full it will "overflow" without warning
	 * 
	 * @param state
	 *            the current state
	 * @param node
	 *            the node that has to execute. This node should be enabled in
	 *            the current state.
	 * @param queue
	 *            a queue for storing implicit nodes.
	 * @return
	 */
	public byte[] execute(byte[] state, int node, StateQueue queue) {

		if (node >= treeSize) {
			node -= treeSize;
		}
		byte[] newState = copyState(state);

		if (tree.isBlocked(configurationNumber, node)) {
			// no state change
			return newState;
		}

		assert isEnabled(state, node);

		// change node from enabled to P
		setState(newState, node, P);

		int nodeType = tree.getType(configurationNumber, node);
		if (tree.isHidden(configurationNumber, node)) {
			// pretend the node is a tau-leaf;
			nodeType = ProbProcessArrayTree.TAU;
		}
		boolean immediateTermination = false;

		// first, handle the children if there are any
		switch (nodeType) {
			case ProbProcessArrayTree.OR :
				if (getState(state, node) == T) {
					executeORTermination(node, newState);
					break;
				}
				// In case of an execution in state E, we enable all children, but we also put the
				// OR node in future.
				setState(newState, node, F);
				//$FALL-THROUGH$
			case ProbProcessArrayTree.XOR :
				executeXORandInitialOR(node, queue, newState);
				break;

			case ProbProcessArrayTree.ILV :
				immediateTermination = executeILV(node, queue, newState, immediateTermination);
				break;
			case ProbProcessArrayTree.AND :
				immediateTermination = executeAND(node, queue, newState, immediateTermination);
				break;
			case ProbProcessArrayTree.REVSEQ :
				immediateTermination = executeREVSEQ(node, queue, newState, immediateTermination);
				break;
			case ProbProcessArrayTree.SEQ :
				immediateTermination = executeSEQ(node, queue, newState, immediateTermination);
				break;
			case ProbProcessArrayTree.LOOP :
				executeLOOP(node, queue, newState);
				break;
			default :
				// LEAF
				// enable nothing further, but change the state of the node to N.
				setState(newState, node, N);
				//newState.enabled--;
		}
		// disable all siblings of node if the parent is a XOR node.
		// re-enable the parent if it is a OR in state F
		int par = tree.getParentFast(node);
		if (par >= 0) {
			processChoiceExecution(newState, tree, node, par);
		}

		if (immediateTermination) {
			setState(newState, node, N);
		}

		if (getState(newState, node) == N) {
			processFinishedSubtree(newState, tree, node, queue);
		}

		return newState;
	}

	protected void executeORTermination(int node, byte[] newState) {
		// this OR should now terminate as it's state is T and it is executing.
		//assert isConsistent();
		// terminate all enabled children.
		int s = node + 1;
		do {
			//newState.enabled -= getState(newState,s) == E ? 1 : 0;
			setState(newState, s, N);
			s = tree.getNextFast(s);
		} while (tree.getParent(s) == node);
		//assert isConsistent();
		setState(newState, node, N);
		//newState.enabled--;
	}

	protected void executeXORandInitialOR(int node, StateQueue queue, byte[] newState) {
		int c;
		// enable all children, this is the same for these types of nodes
		// there is always at least one child
		c = node + 1;
		do {
			assert getState(newState, c) == N;
			if (!tree.isBlocked(configurationNumber, c)) {
				// enable only if not blocked
				setState(newState, c, E);
			}
			c = tree.getNextFast(c);
		} while (tree.getParent(c) == node);

		// check if there's only one child and add to queue if enabled
		if (tree.getNextFast(node) == tree.getNextFast(node + 1)
				&& (!tree.isLeaf(node + 1) || tree.isHidden(configurationNumber, node + 1))
				&& getState(newState, node + 1) == E) {
			queue.add(node + 1);
		}
	}

	protected void executeLOOP(int node, StateQueue queue, byte[] newState) {
		int c;
		// enable the first child (if not hidden) and put the last in future.
		c = node + 1;
		assert getState(newState, c) == N;

		if (tree.isBlocked(configurationNumber, c)) {
			// put in future indefinitely (leads to a deadlock)
			setState(newState, c, F);
		} else {
			// enable only if not blocked or hidden
			setState(newState, c, E);
			// add to the queue if non-leaf or hidden
			if (!tree.isLeaf(c) || tree.isHidden(configurationNumber, c)) {
				queue.add(c);
			}
		}

		// go to the right child and put it in future. The middle child is put in future after STARTING the middle one
		c = tree.getNextFast(tree.getNextFast(c));
		assert getState(newState, c) == N;
		setState(newState, c, F);

		//newState.future++;
	}

	protected boolean executeSEQ(int node, StateQueue queue, byte[] newState, boolean immediateTermination) {
		int c;
		// enable the first non-hidden child and put the rest in future.
		c = node + 1;
		int first = -1;

		do {
			assert getState(newState, c) == N;
			// put in future
			if (!tree.isHidden(configurationNumber, c)) {
				setState(newState, c, F);
				if (first < 0) {
					first = c;

				}
			}
			c = tree.getNextFast(c);
		} while (tree.getParent(c) == node);

		if (first > 0 && !tree.isBlocked(configurationNumber, first)) {
			// the first child is stored in first.
			setState(newState, first, E);
			// add first enabled child to the queue if non-leaf
			if (!tree.isLeaf(first)) {// || tree.isHidden(configurationNumber, first)) {
				queue.add(first);
			}
		} else if (first < 0) {
			// all leafs hidden
			immediateTermination = true;
		}

		return immediateTermination;
	}

	protected boolean executeREVSEQ(int node, StateQueue queue, byte[] newState, boolean immediateTermination) {
		int c;
		// enable the last non-hidden child and put the rest in future.
		c = node + 1;
		int last = -1;

		do {
			assert getState(newState, c) == N;
			if (!tree.isHidden(configurationNumber, c)) {
				// put in future
				setState(newState, c, F);
				last = c;
			}
			c = tree.getNext(c);
		} while (tree.getParent(c) == node);

		if (last >= 0 && !tree.isBlocked(configurationNumber, last)) {
			// the last child is stored in last.
			setState(newState, last, E);
			// add first enabled child to the queue if non-leaf
			if (!tree.isLeaf(last)) {// || tree.isHidden(configurationNumber, last)) {
				queue.add(last);
			}
		} else if (last < 0) {
			// all leafs hidden
			immediateTermination = true;
		}
		return immediateTermination;
	}

	protected boolean executeAND(int node, StateQueue queue, byte[] newState, boolean immediateTermination) {
		int c;
		// enable all children, this is the same for these types of nodes
		// there is always at least one child
		boolean allHidden = true;
		c = node + 1;
		do {
			assert getState(newState, c) == N;
			if (tree.isBlocked(configurationNumber, c)) {
				// put in future indefinitely (leads to a deadlock)
				setState(newState, c, F);
				allHidden = false;
			} else if (!tree.isHidden(configurationNumber, c)) {
				// enable only if not blocked
				setState(newState, c, E);
				allHidden = false;
			}
			c = tree.getNextFast(c);
		} while (tree.getParent(c) == node);

		if (pushDownUnderAND) {
			// add all enabled non-leaf children to the queue
			c = node + 1;
			do {
				if (!tree.isLeaf(c) && getState(newState, c) == E) {
					queue.add(c);
				}
				c = tree.getNextFast(c);
			} while (tree.getParent(c) == node);
		}
		if (allHidden) {
			// all leafs hidden
			immediateTermination = true;
		}
		return immediateTermination;
	}

	protected boolean executeILV(int node, StateQueue queue, byte[] newState, boolean immediateTermination) {
		int c;
		// enable all children, this is the same for these types of nodes
		// there is always at least one child
		boolean allHidden = true;
		c = node + 1;
		do {
			assert getState(newState, c) == N;
			if (tree.isBlocked(configurationNumber, c)) {
				// put in future indefinitely (leads to a deadlock)
				setState(newState, c, F);
				allHidden = false;
			} else if (!tree.isHidden(configurationNumber, c)) {
				// enable only if not blocked
				setState(newState, c, E);
				allHidden = false;
			}
			c = tree.getNextFast(c);
		} while (tree.getParent(c) == node);

		//No pushdown possible

		if (allHidden) {
			// all leafs hidden
			immediateTermination = true;
		}
		return immediateTermination;
	}

	/**
	 * Executes the given node from the given state while filling the queue and
	 * continues to execute implied nodes until the queue is empty again.
	 * 
	 * If the builder was instantiated without a queue, this method does not do
	 * anything and will return state.
	 * 
	 * @param state
	 * @param node
	 * @param queue
	 * @return
	 */
	public byte[] executeAll(byte[] state, int node) {

		// add the node that needs execution to the queue
		queue.add(node);

		// set the next state to be the current one 
		byte[] next = state;
		while (!queue.isEmpty()) {
			// continue while the queue is not empty
			// and overwrite next with the state obtained from
			// executing the next element from the queue.
			next = execute(next, queue.poll(), queue);
		}
		return next;
	}

	protected void processChoiceExecution(byte[] state, ProbProcessArrayTree tree, int node, int parent) {
		// if the parent of node is a XOR then all currently enabled or blocked siblings should become disabled.
		if (tree.getTypeFast(configurationNumber, parent) == ProbProcessArrayTree.XOR) {
			node = parent + 1;
			do {
				if (getState(state, node) == E || tree.isBlocked(configurationNumber, node)) {
					//enabled--;
					setState(state, node, N);
				}
				node = tree.getNextFast(node);
			} while (node < treeSize && tree.getParentFast(node) == parent);
		} else if (tree.getTypeFast(configurationNumber, parent) == ProbProcessArrayTree.ILV) {
			// if parent is an ILV, then all other children should move from E to F
			node = parent + 1;
			do {
				if (getState(state, node) == E || tree.isBlocked(configurationNumber, node)) {
					//enabled--;
					setState(state, node, F);
				}
				node = tree.getNextFast(node);
			} while (node < treeSize && tree.getParentFast(node) == parent);
		} else if (tree.getTypeFast(configurationNumber, parent) == ProbProcessArrayTree.OR && getState(state, parent) == T) {
			// if we started to execute a subtree of an OR, then the OR is no longer in T, but in F
			setState(state, parent, F);
			// also, all blocked children of the OR-parent should change to N
			node = parent + 1;
			do {
				if (tree.isBlocked(configurationNumber, node)) {
					setState(state, node, N);
				}
				node = tree.getNextFast(node);
			} while (node < treeSize && tree.getParentFast(node) == parent);

		} else if (tree.getTypeFast(configurationNumber, parent) == ProbProcessArrayTree.LOOP) {
			// a child of a loop started, 
			int middle = tree.getNextFast(parent + 1);
			int right = tree.getNextFast(middle);
			// If the leftmost child started then put the middle in future.
			if (parent + 1 == node) {
				if ((tree.isLeaf(node) || tree.isHidden(configurationNumber, node) || getState(state, node) != N)) {
					// the leftmost child started and was a leaf (or a hidden node, which is processed as if it was a leaf) or is now not in N 
					// put the middle child in future.
					setState(state, tree.getNextFast(node), F);
				}
			} else if (node == right) {
				// the rightMost child started
				// disable the enabled middle child
				//enabled--;
				setState(state, tree.getNextFast(parent + 1), N);
			} else if (parent + 1 != node) {
				// the middle child started
				assert (tree.getTypeFast(configurationNumber, middle) == ProbProcessArrayTree.OR && getState(state, right) == F)
						|| (getState(state, right) == E || tree.isBlocked(configurationNumber, right));
				// put the rightmost child from enabled to future and move the leftmost to future from N
				assert (tree.getTypeFast(configurationNumber, middle) == ProbProcessArrayTree.OR && getState(state, right) == F)
						|| getState(state, parent + 1) == N;
				// the exit part of the loop is also enabled. Put it into future as now the leftmost
				// needs to be executed once more.
				setState(state, right, F);
				setState(state, parent + 1, F);
			}
		}
	}

	protected void processFinishedSubtree(byte[] state, ProbProcessArrayTree tree, int node, StateQueue queue) {
		if (node == 0) {
			// node is the root, terminate
			return;
		}
		// get the parent p 
		int p;

		do {
			p = tree.getParentFast(node);

			assert getState(state, node) == N;
			// the parent must have a positive count, or if it is a OR, it can be in future
			assert getState(state, p) == P
					|| (tree.getTypeFast(configurationNumber, p) == ProbProcessArrayTree.OR && getState(state, p) == F);

			switch (tree.getTypeFast(configurationNumber, p)) {
				case ProbProcessArrayTree.REVSEQ :
					processFinishedSubtreeRevSeq(state, tree, node, queue, p);
					break;
				case ProbProcessArrayTree.SEQ :
					processFinishedSubtreeSeq(state, tree, node, queue, p);
					break;
				case ProbProcessArrayTree.LOOP :
					processFinishedSubtreeLoop(state, tree, node, queue, p);
					break;
				case ProbProcessArrayTree.ILV :
					processFinishedSubtreeIlv(state, tree, queue, p);
					break;
				case ProbProcessArrayTree.OR :
					// if the parent is an OR node which is in state F, we
					// may need to enable the termination of the OR
					if (getState(state, p) == F) {
						processFinishedSubtreeOr(state, tree, queue, p);
						break;
					}
					// if the parent was not in F, we process as if it
					// was an AND, i.e. check if all children are completed.
					//$FALL-THROUGH$
				default : {
					processEmptySubtree(state, tree, queue, p);
				}
			}
			node = p;
		} while (node > 0 && getState(state, node) == N);
	}

	protected void processEmptySubtree(byte[] state, ProbProcessArrayTree tree, StateQueue queue, int p) {
		// check if all siblings are N, in which case
		// the node changes state to N, regardless of its current state
		int s = p + 1;
		boolean all = true;
		do {
			all &= getState(state, s) == N;
			s = tree.getNextFast(s);
		} while (all && s < treeSize && tree.getParentFast(s) == p);
		if (all) {
			// all children of p are in state N, hence p moves to state N
			assert getState(state, p) == P;
			setState(state, p, N);
			//			processFinishedSubtree(state, tree, p, queue);
		}
	}

	protected void processFinishedSubtreeOr(byte[] state, ProbProcessArrayTree tree, StateQueue queue, int p) {
		// if the parent is an OR node in the state F, then it can
		// change to T if all children are either N or E.
		int s = p + 1;
		boolean all = true;
		boolean en = false;
		do {
			all &= getState(state, s) == N || getState(state, s) == E;
			en |= getState(state, s) == E;
			s = tree.getNextFast(s);
		} while (all && s < treeSize && tree.getParentFast(s) == p);
		if (all) {
			// all children of p are in state N OR E, 
			if (isAllowImplicitOrTermination() && !en) {
				// nothing is enabled, OR node is finished.
				// and implicitOrTermination is allowed
				setState(state, p, N);
				//				processFinishedSubtree(state, tree, p, queue);
			} else if (!en) {
				setState(state, p, T);
				queue.add(p + treeSize);
			} else {
				setState(state, p, T);
			}
		}
	}

	protected void processFinishedSubtreeIlv(byte[] state, ProbProcessArrayTree tree, StateQueue queue, int p) {
		// in an interleaving, all children that are in F, are now in E
		int s = p + 1;
		boolean foundOne = false;
		while (s < treeSize && tree.getParentFast(s) == p) {
			if (getState(state, s) == F) {
				if (!tree.isBlocked(configurationNumber, s)) {
					setState(state, s, E);
				}
				foundOne = true;
			}
			s = tree.getNextFast(s);
		}
		if (!foundOne) {
			// no more children enabled, complete the subtree.
			assert getState(state, p) == P;
			setState(state, p, N);
			//			processFinishedSubtree(state, tree, p, queue);
		}
	}

	protected void processFinishedSubtreeLoop(byte[] state, ProbProcessArrayTree tree, int node, StateQueue queue, int p) {
		//			else if (tree.getTypeFast(configurationNumber, p) == NAryTree.LOOP) {
		int middle = tree.getNextFast(p + 1);
		int right = tree.getNextFast(middle);
		if (node == p + 1) {
			// if node is the left-most child, then the other two children become enabled, if not blocked.
			assert getState(state, middle) == F && getState(state, right) == F;
			if (!tree.isBlocked(configurationNumber, middle)) {
				setState(state, middle, E);
			} else {
				setState(state, middle, N);
			}
			if (!tree.isBlocked(configurationNumber, right)) {
				setState(state, right, E);
			} else {
				setState(state, right, F);
			}
		} else if (node == right) {
			// a node is the right-most child if it is a child and the next node is the same.
			// if node is the rightmost child of the loop, then the loop terminates.
			assert getState(state, p + 1) == N;
			assert getState(state, p) == P;
			if (getState(state, middle) == E || getState(state, middle) == F) {
				// disable middle tree
				setState(state, middle, N);
			} else {
				assert getState(state, middle) == N
						|| (getState(state, middle) == T && tree.getTypeFast(configurationNumber, middle) == ProbProcessArrayTree.OR);
			}
			setState(state, p, N);
			//			processFinishedSubtree(state, tree, p, queue);
		} else {
			// the middle child terminates, now the leftmost needs to start. It is in future
			// we can safely assume it isn't blocked since it wouldn't have started the first
			// time around.
			assert getState(state, p + 1) == F;
			//future--;
			//enabled++;
			setState(state, p + 1, E);
			// add p+1 to the queue is non-leaf
			if (!tree.isLeaf(p + 1)) {
				queue.add(p + 1);
			}
		}
	}

	protected void processFinishedSubtreeSeq(byte[] state, ProbProcessArrayTree tree, int node, StateQueue queue, int p) {
		//				else if (tree.getTypeFast(configurationNumber, p) == NAryTree.SEQ) {
		// in a sequence, the next sibling needs to change from future to enabled, if there is one
		int s = tree.getNextFast(node);
		while (s < treeSize && tree.getParentFast(s) == p && tree.isHidden(configurationNumber, s)) {
			s = tree.getNextFast(s);
		}
		if (s < treeSize && tree.getParentFast(s) == p) {
			// there is a right sibling, which has to be in future.
			assert getState(state, s) == F;
			if (!tree.isBlocked(configurationNumber, s)) {
				setState(state, s, E);
				// add s to the queue is non-leaf
				if (!tree.isLeaf(s)) {
					queue.add(s);
				}
			}
			//} else {
			// there should now be a deadlock.
			// i.e. the next sibling remains in future and cannot be executed.
		} else {
			// there are no more siblings, hence all children are in state N
			assert getState(state, p) == P;
			setState(state, p, N);
			//			processFinishedSubtree(state, tree, p, queue);
		}
	}

	protected void processFinishedSubtreeRevSeq(byte[] state, ProbProcessArrayTree tree, int node, StateQueue queue, int p) {
		//		if (tree.getTypeFast(configurationNumber, p) == NAryTree.REVSEQ) {
		// in a reverse sequence, the previous sibling needs to change from future to enabled, if there is one
		// unfortunately, we cannot move quickly through them.
		// start from right and keep track of last node, not hidden

		if (node == p + 1) {
			// there are no more siblings, hence all children are in state N
			assert getState(state, p) == P;
			setState(state, p, N);
			//			processFinishedSubtree(state, tree, p, queue);
		} else {
			int s = p + 1;
			int found = -1;
			if (!tree.isHidden(configurationNumber, s)) {
				found = s;
			}
			while (tree.getNextFast(s) != node) {
				s = tree.getNextFast(s);
				if (!tree.isHidden(configurationNumber, s)) {
					found = s;
				}
			}
			if (found < 0) {
				// there are no more non-hidden siblings, hence all children are in state N
				assert getState(state, p) == P;
				setState(state, p, N);
				//				processFinishedSubtree(state, tree, p, queue);
			} else {

				// found is a sibling to the left.
				assert getState(state, found) == F;
				if (!tree.isBlocked(configurationNumber, found)) {
					setState(state, found, E);
					// add s to the queue is non-leaf
					if (!tree.isLeaf(found)) {
						queue.add(found);
					}
				} else {
					// there should now be a deadlock.
					// i.e. the next sibling remains in future and cannot be executed.
				}
			}
		}
	}

	/**
	 * returns true if and only if this state is an array of 0's, i.e. all
	 * elements are in state N
	 * 
	 * @param state
	 * @return
	 */
	public boolean isFinal(byte[] state) {
		boolean f = true;
		for (int i = state.length; f && i-- > 0;) {
			f &= state[i] == 0;
		}
		return f;
	}

	// ITERATORS *********************************************
	/**
	 * returns an iterator to iterate over the enabled nodes. If a node is an OR
	 * node which is enabled for termination (i.e. state T) then size() + the
	 * index of the node is returned by the iterator BEFORE any of the
	 * potentially enabled children is returned.
	 */
	public TIntIterator enabledIterator(byte[] state) {
		return enabledIterator(state, false);
	}

	/**
	 * returns an iterator to iterate over the enabled nodes. If a node is an OR
	 * node which is enabled for termination (i.e. state T) then size() + the
	 * index of the node is returned by the iterator BEFORE any of the
	 * potentially enabled children is returned.
	 */
	public TIntIterator enabledIterator(byte[] state, boolean skipLeafsUnderORIfOREnabled) {
		return new EnabledIterator(this, state, skipLeafsUnderORIfOREnabled);
	}

	/**
	 * returns an iterator to iterate over the enabled nodes. If type refers to
	 * an activity, then only leafs of that type are returned. Otherwise, this
	 * parameter is ignored.
	 */
	public TIntIterator enabledIterator(byte[] state, short leafType) {
		if (leafType >= 0) {
			return new EnabledLeafIterator(this, state, leafType);
		} else {
			return new EnabledIterator(this, state, false);
		}
	}

	/**
	 * returns an iterator to iterate over the nodes that are in future.
	 */
	public TIntIterator futureIterator(byte[] state) {
		return new FutureIterator(this, state);
	}

	/**
	 * returns a string representation of the state by listing the enabled nodes
	 * first, then the future nodes.
	 * 
	 * @param state
	 * @return
	 */
	public String toString(byte[] state) {
		StringBuilder b = new StringBuilder();
		b.append("E: [");
		TIntIterator it = enabledIterator(state);
		while (it.hasNext()) {
			b.append(it.next());
			if (it.hasNext()) {
				b.append(",");
			}
		}
		b.append("]<BR/>F: [");
		it = futureIterator(state);
		while (it.hasNext()) {
			b.append(it.next());
			if (it.hasNext()) {
				b.append(",");
			}
		}
		b.append("]");
		return b.toString();// ;

	}

	public StateSpace buildStateSpace(boolean doPushDown) {
		byte[] initial = initializeState();
		final int alignment = initial.length;

		// we have an efficient storage for byte arrays.
		final Queue<byte[]> todo = new LinkedBlockingQueue<byte[]>();

		final StateSpace statespace = new StateSpace(alignment);
		statespace.addNode(initial);

		todo.add(initial);

		byte[] from;

		while (!todo.isEmpty()) {
			from = todo.poll();

			TIntIterator it = enabledIterator(from);
			if (it.hasNext()) {
				int fromIndex = statespace.indexOf(from);
				while (it.hasNext()) {
					int node = it.next();
					byte[] to;
					if (doPushDown) {
						to = executeAll(from, node);
					} else {
						to = execute(from, node);
					}
					Result<byte[]> r = statespace.addNode(to);
					int toIndex = (int) r.index;
					assert toIndex != fromIndex;
					if (r.isNew) {
						todo.add(to);
					}
					statespace.addEdge(fromIndex, toIndex, node);
				}

			}
		}

		return statespace;
	}

	public static void main(String[] args) throws FileNotFoundException {
		ProbProcessArrayTree tree;
		tree = TreeUtils.randomTree(4, .8, 5, 10);
		//tree = TreeUtils.fromString("AND( AND( AND( LEAF: a ) ) , AND( AND( LEAF: b ) ) )"); //, AND( AND( LEAF: c ) ) ,  AND ( AND( LEAF: d ) ) )");
		//tree = TreeUtils.fromString("LOOP( OR( LEAF: 0 , SEQ( LEAF: 1 ) , LEAF: 0 , LEAF: 1 ) , LEAF: 3 , LEAF: 0 )");
		//tree = TreeUtils.fromString("XOR( SEQ( LOOP( LEAF: 3 , SEQ( LEAF: 1 ) , LEAF: 2 ) , LEAF: 0 ) , LEAF: 0 , LEAF: 1 , LEAF: 3 )");
		//tree = TreeUtils
		//		.fromString("SEQ( LEAF: 0 , LOOP( OR( LEAF: 1 , LOOP( LEAF: 2 , LEAF: 3 , LEAF: 4 ) ) , OR( LEAF: 4 , AND( LEAF: 5 , LEAF: 6 ) ) , OR( LEAF: 7 , AND( LEAF: 8 , LEAF: 9 ) ) ) )");
		//tree = TreeUtils.fromString("LOOP( LEAF: 1 , LOOP( LEAF: 2 , LEAF: 3 , LEAF: 4 ) , LEAF: 5 )");
		//		tree = TreeUtils.fromString("LOOP( LEAF: 0 , LEAF: 1 , LEAF: 2 ) [[-,-,-,-]]");

		//tree = TreeUtils.fromString("LOOP( XOR( LEAF: B , LEAF: B ) , LEAF: C , LEAF: D )");
		tree = TreeUtils.fromString("LOOP( SEQ( LEAF: A , LEAF: B ) , LEAF: C , LEAF: D )");
		//tree = TreeUtils.fromString("XOR( LEAF: C , LOOP( LEAF: A , LEAF: D , LEAF: E ) , LEAF: D )");
		//		tree = TreeUtils
		//				.fromString("AND( AND( LEAF: A , LEAF: tau , OR( LEAF: tau ) , AND( AND( LEAF: D , LEAF: B , LEAF: D ) , AND( LEAF: tau , LEAF: E , LOOP( LEAF: B , LEAF: tau , LEAF: E ) ) , LEAF: A , AND( LEAF: A , LEAF: B , LEAF: E ) ) ) , SEQ( LOOP( LEAF: C , AND( AND( LEAF: D , LEAF: D , LEAF: B ) , AND( LEAF: tau , LEAF: D ) , LEAF: E , LEAF: E ) , LEAF: D ) , LEAF: D , LOOP( LEAF: tau , LOOP( LEAF: tau , SEQ( LEAF: tau , LEAF: D ) , OR( LEAF: B , XOR( LEAF: D , LEAF: E , LEAF: E ) , LEAF: E , LEAF: C ) ) , LEAF: A ) ) , LEAF: C , LEAF: tau )");
		TreeUtils.printTree(tree);
		tree.addConfiguration(new Configuration(tree.size()));
		TreeUtils.printTree(tree.applyConfiguration(0));

		//		System.exit(0);
		//State s = new State(tree);
		//		System.out.println("    " + s);
		//		int cnt = 0;
		//		do {
		//			TIntIterator it = s.enabledIterator();
		//			if (!it.hasNext()) {
		//				break;
		//			}
		//			int i = s.enabledIterator().next();
		//			s = s.execute(tree, i);
		//			System.out.println(i + " (" + tree.getType(i) + ") " + s);
		//			assert s.isConsistent();
		//			cnt++;
		//		} while (cnt < 10);
		OutputStreamWriter out;

		//		boolean f = false;
		//		boolean t = true;
		//		boolean[] b = new boolean[] { f, f, f, f };
		//		boolean[] h = new boolean[] { f, f, f, f };
		//
		//		Configuration c = new Configuration(b, h);
		//		tree.addConfiguration(c);

		out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(new File("d://temp//tree.dot"))));
		try {
			TreeUtils.writeTreeToDot(tree, 0, out);
			out.flush();
			out.close();
			System.out.println();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(new File(
				"d://temp//behavior-compressed.dot"))));
		try {
			TreeUtils.writeBehaviorToDot(tree, 0, out, true);
			out.flush();
			out.close();
			System.out.println();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(new File("d://temp//behavior.dot"))));
		try {
			TreeUtils.writeBehaviorToDot(tree, 0, out, false);
			out.flush();
			out.close();
			System.out.println();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public void setPushDownUnderAND(boolean pushDownUnderAND) {
		this.pushDownUnderAND = pushDownUnderAND;
	}

	public boolean isPushDownUnderAND() {
		return pushDownUnderAND;
	}

	public void setAllowImplicitOrTermination(boolean allowImplicitOrTermination) {
		this.allowImplicitOrTermination = allowImplicitOrTermination;
	}

	public boolean isAllowImplicitOrTermination() {
		return allowImplicitOrTermination;
	}

	private static final class EnabledLeafIterator extends AbstractIterator {

		protected final short type;

		public EnabledLeafIterator(StateBuilder builder, byte[] state, short type) {
			super(builder, state);
			this.type = type;
			if (builder.tree.isLeaf(0)) {
				next = -1;
			} else {
				next = builder.tree.getNextLeafFast(0) - 1;
			}
			findFirstEnabled();
		}

		@Override
		protected void findFirstEnabled() {
			int index = next + 1;
			while (index < builder.treeSize) {
				if (builder.getTree().getTypeFast(builder.configurationNumber, index) == type
						&& !builder.getTree().isHidden(builder.getConfigurationNumber(), index) && accept(index)) {
					next = index;
					return;
				} else {
					int li = index;
					if (builder.getState(state, index) == N) {
						// skip subtree
						index = builder.tree.getNextFast(index);
					}
					if (builder.tree.getNextLeafFast(li) > index) {
						index = builder.tree.getNextLeafFast(li);
					}
				}
			}
			next = -1;
		}

		protected boolean accept(int index) {
			return builder.getState(state, index) == E;
		}
	}

	private static class EnabledIterator extends AbstractIterator {

		private final boolean skipLeafsUnderORIfOREnabled;

		public EnabledIterator(StateBuilder builder, byte[] state, boolean skipLeafsUnderORIfOREnabled) {
			super(builder, state);
			this.skipLeafsUnderORIfOREnabled = skipLeafsUnderORIfOREnabled;
			findFirstEnabled();
		}

		protected void findFirstEnabled() {
			int index = (next >= builder.treeSize ? next - builder.treeSize : next) + 1;
			while (index < builder.treeSize) {
				if (builder.tree.isBlocked(builder.configurationNumber, index)) {
					// blocked node, never enabled.
					// skip subtree
					index = builder.tree.getNextFast(index);
				} else if (builder.getState(state, index) == E) {
					if (skipLeafsUnderORIfOREnabled && builder.tree.isLeaf(index) && index > 0
							&& builder.tree.getTypeFast(builder.tree.getParent(index)) == ProbProcessArrayTree.OR
							&& builder.isEnabled(state, builder.tree.getParent(index))) {
						// index is a leaf under an OR which is ready to terminate, skip.
						index++;
					} else {
						next = index;
						return;
					}
				} else if (builder.getState(state, index) == T
						&& builder.tree.getTypeFast(builder.configurationNumber, index) == ProbProcessArrayTree.OR) {
					next = builder.treeSize + index;
					return;
				} else {
					if (builder.getState(state, index) == N) {
						// skip subtree
						index = builder.tree.getNextFast(index);
					} else {
						index++;
					}
				}
			}
			next = -1;
		}

		protected boolean accept(int index) {
			return builder.isEnabled(state, index);
		}

	}

	private static final class FutureIterator extends AbstractIterator {

		public FutureIterator(StateBuilder builder, byte[] state) {
			super(builder, state);
			findFirstEnabled();
		}

		protected boolean accept(int index) {
			return builder.getState(state, index) == F;
		}

	}

	private static abstract class AbstractIterator implements TIntIterator {

		protected int next = -1;

		protected final byte[] state;
		protected final StateBuilder builder;

		public AbstractIterator(StateBuilder builder, byte[] state) {
			this.builder = builder;
			this.state = state;
		}

		protected void findFirstEnabled() {
			int index = next + 1;
			while (index < builder.treeSize) {
				if (accept(index)) {
					next = index;
					return;
				} else {
					if (builder.getState(state, index) == N) {
						// skip subtree
						index = builder.tree.getNextFast(index);
					} else {
						index++;
					}
				}
			}
			next = -1;
		}

		protected abstract boolean accept(int index);

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public boolean hasNext() {
			return next >= 0;
		}

		public int next() {
			if (next < 0) {
				throw new NoSuchElementException();
			}
			int n = next;
			findFirstEnabled();
			return n;
		}
	}
}
