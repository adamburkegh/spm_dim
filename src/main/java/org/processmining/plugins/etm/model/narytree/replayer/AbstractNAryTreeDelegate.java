package org.processmining.plugins.etm.model.narytree.replayer;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TShortSet;
import gnu.trove.set.hash.TShortHashSet;
import nl.tue.astar.AStarThread;
import nl.tue.astar.Tail;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.State;
import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.Deflater;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.Inflater;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.boudewijn.treebasedreplay.TreeDelegate;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.StateBuilder;

public abstract class AbstractNAryTreeDelegate<T extends Tail> implements TreeDelegate<NAryTreeHead, T> {

	protected final AStarAlgorithm algorithm;
	//protected final NAryTree tree;
	protected final int[] node2cost;
	protected final int threads;
	protected final short classes;
	protected final int scaling = 1000;

	protected final StateBuilder localStateBuilder;

	protected final NAryTreeHeadCompressor<T> headCompressor;
	protected final ProbProcessArrayTree tree;
	protected final int configurationNumber;

	public AbstractNAryTreeDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads) {
		localStateBuilder = new StateBuilder(tree, configurationNumber, true);
		//super(tree, configurationNumber, true);
		this.algorithm = algorithm;
		this.tree = tree;
		this.configurationNumber = configurationNumber;
		//this.tree = tree;
		this.node2cost = node2cost;

		this.threads = threads;

		this.classes = (short) algorithm.getClasses().size();
		this.headCompressor = new NAryTreeHeadCompressor<T>(this, classes);

	}

	public abstract TreeRecord createInitialRecord(NAryTreeHead head, Trace trace);

	public Inflater<NAryTreeHead> getHeadInflater() {
		return headCompressor;
	}

	public Deflater<NAryTreeHead> getHeadDeflater() {
		return headCompressor;
	}

	public void setStateSpace(CompressedHashSet<State<NAryTreeHead, T>> statespace) {
	}

	public XEventClass getClassOf(XEvent e) {
		return algorithm.getClasses().getClassOf(e);
	}

	public short getIndexOf(XEventClass c) {
		return algorithm.getIndexOf(c);
	}

	public short numEventClasses() {
		return (short) algorithm.getClasses().size();
	}

	public int getCostFor(int node, int activity) {
		if (node == AStarThread.NOMOVE) {
			// logMove only
			return getLogMoveCost(activity);
		}
		if (activity == AStarThread.NOMOVE) {
			return getModelMoveCost(node);
		}
		// synchronous move. Don't penalize that.
		return 1;
	}

	private static TShortSet EMPTYSET = new TShortHashSet(1);
	private boolean allowImplicitOrTermination;
	private boolean pushDown;

	public TShortSet getActivitiesFor(int node) {
		// leafs are mapped to activities.
		if (tree.isLeaf(node) && tree.getType(configurationNumber, node) != ProbProcessArrayTree.TAU) {
			return new TShortHashSet(new short[] { tree.getType(configurationNumber, node) });
		} else {
			return EMPTYSET;
		}
	}

	public XEventClass getEventClass(short act) {
		return algorithm.getEventClass(act);
	}

	public boolean isLeaf(int modelMove) {
		return tree.isLeaf(modelMove);
	}

	public int getLogMoveCost(int i) {
		return 1 + scaling * algorithm.getLogMoveCost(i);
	}

	public int getModelMoveCost(int node) {
		if (node >= node2cost.length) {
			// this is an OR-node which is terminating.
			return 0;
		}
		return 1 + (tree.isHidden(configurationNumber, node) ? 0 : scaling * node2cost[node]);
	}

	public int numNodes() {
		return tree.size();
	}

	public HashOperation<State<NAryTreeHead, T>> getHeadBasedHashOperation() {
		return headCompressor;
	}

	public EqualOperation<State<NAryTreeHead, T>> getHeadBasedEqualOperation() {
		return headCompressor;
	}

	public String toString(short modelMove, short activity) {
		int m = modelMove;
		if (m >= tree.size()) {
			m -= tree.size();
		}

		if (tree.isLeaf(m)) {
			if (tree.getType(configurationNumber, m) == ProbProcessArrayTree.TAU) {
				return "tau (" + modelMove + ")";
			}
			return algorithm.getEventClass(tree.getType(configurationNumber, m)).toString() + " (" + modelMove + ")";
		} else {
			switch (tree.getType(configurationNumber, m)) {
				case ProbProcessArrayTree.OR :
					return "OR (" + modelMove + ")";
				case ProbProcessArrayTree.ILV :
					return "ILV (" + modelMove + ")";
				case ProbProcessArrayTree.XOR :
					return "XOR (" + modelMove + ")";
				case ProbProcessArrayTree.AND :
					return "AND (" + modelMove + ")";
				case ProbProcessArrayTree.LOOP :
					return "LOOP (" + modelMove + ")";
				case ProbProcessArrayTree.SEQ :
					return "SEQ (" + modelMove + ")";
				case ProbProcessArrayTree.REVSEQ :
					return "REVSEQ (" + modelMove + ")";
				default :
					assert false;
					return null;

			}
		}
	}

	public int getScaling() {
		return scaling;
	}

	public boolean isBlocked(int node) {
		return tree.isBlocked(configurationNumber, node);
	}

	public ProbProcessArrayTree getTree() {
		return tree;
	}

	public AStarAlgorithm getAStarAlgorithm() {
		return algorithm;
	}

	public int getStateSizeInBytes() {
		return localStateBuilder.getStateSizeInBytes();
	}

	public void setPushDownUnderAND(boolean pushDown) {
		this.pushDown = pushDown;
		localStateBuilder.setPushDownUnderAND(pushDown);
	}

	public int getState(byte[] state, int node) {
		return localStateBuilder.getState(state, node);
	}

	public TIntIterator enabledIterator(byte[] state, boolean skipLeafsUnderORIfOREnabled) {
		return localStateBuilder.enabledIterator(state, skipLeafsUnderORIfOREnabled);
	}

	public TIntIterator enabledIterator(byte[] state, short activity) {
		return localStateBuilder.enabledIterator(state, activity);
	}

	public void setAllowImplicitOrTermination(boolean allowImplicitOrTermination) {
		this.allowImplicitOrTermination = allowImplicitOrTermination;
		localStateBuilder.setAllowImplicitOrTermination(allowImplicitOrTermination);
	}

	public StateBuilder getStateBuilder() {
		StateBuilder builder = new StateBuilder(tree, configurationNumber, true);
		builder.setAllowImplicitOrTermination(allowImplicitOrTermination);
		builder.setPushDownUnderAND(pushDown);
		return builder;
	}

	public boolean isFinal(byte[] state) {
		return localStateBuilder.isFinal(state);
	}

}
