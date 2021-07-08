package org.processmining.plugins.etm.model.narytree.replayer;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Map;

import nl.tue.astar.AStarThread;
import nl.tue.astar.Tail;
import nl.tue.astar.Trace;

import org.processmining.plugins.boudewijn.treebasedreplay.TreeDelegate;
import org.processmining.plugins.boudewijn.treebasedreplay.astar.ModelPrefix;
import org.processmining.plugins.boudewijn.treebasedreplay.astar.TreeMarkingVisit;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeReplayer.VerboseLevel;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.StateBuilder;

// This class extends statebuilder, since it has to insert a processing step
// when a block finished processing
public class NAryTreePostProcessor<H extends NAryTreeHead, T extends Tail> extends StateBuilder {

	protected final Map<TreeMarkingVisit<ModelPrefix>, TIntSet> marking2modelmove;
	protected final TObjectIntMap<TreeMarkingVisit<ModelPrefix>> marking2visitCount;
	protected final Map<TreeMarkingVisit<ModelPrefix>, TIntSet> marking2treenode;
	protected final int[] syncMoveCount;
	protected final int[] aSyncMoveCount;
	protected final int[] moveCount;
	protected final TreeDelegate<H, T> delegate;
	//Stores ALL the alignments for the log, if NULL is provided then we don't do this :)
	private Map<Trace, TreeRecord> alignments;

	public NAryTreePostProcessor(TreeDelegate<H, T> delegate, ProbProcessArrayTree tree, int configurationNumber,
			Map<TreeMarkingVisit<ModelPrefix>, TIntSet> marking2modelmove,
			TObjectIntMap<TreeMarkingVisit<ModelPrefix>> marking2visitCount,
			//Map<TreeMarkingVisit<ModelPrefix>,TIntSet> marking2treenode, 
			int[] syncMoveCount, int[] aSyncMoveCount, int[] moveCount, Map<Trace, TreeRecord> alignments) {
		super(tree, configurationNumber);
		this.delegate = delegate;
		this.marking2modelmove = marking2modelmove;
		this.marking2visitCount = marking2visitCount;
		this.marking2treenode = null;
		this.syncMoveCount = syncMoveCount;
		this.aSyncMoveCount = aSyncMoveCount;
		this.moveCount = moveCount;
		this.alignments = alignments;
	}

	public void process(NAryTreeReplayer<H, T, ?> replayer, VerboseLevel verbose, Trace trace, TreeRecord r,
			int frequency) {
		// Here we process an alignment to fill "marking2modelmove" and "marking2visitcount"

		if (alignments != null) {
			alignments.put(trace, r);
		}

		//  First, build the modelPrefix based on
		ModelPrefix prefix = new ModelPrefix();
		TreeRecord rec = r;
		while (rec.getPredecessor() != null) {
			if (rec.getModelMove() != AStarThread.NOMOVE && rec.getModelMove() < tree.size()
					&& tree.isLeaf(rec.getModelMove())) {
				prefix.addFirst(rec.getModelMove());
			}
			rec = rec.getPredecessor();
		}
		processAlignment(replayer, r, frequency, prefix);

	}

	protected void processAlignment(NAryTreeReplayer<H, T, ?> aStar, TreeRecord rec, int frequency, ModelPrefix prefix) {
		// update visited nodes for precision
		// and at the same time remove the base costs for moving, so only
		// the punishments remain.
		boolean[] sync = new boolean[tree.size()];
		boolean[] async = new boolean[tree.size()];

		do {

			// get the enabled moves in this state. 
			TIntList enabledMoves = new TIntArrayList(10);
			if (rec.getModelMove() != AStarThread.NOMOVE) {
				// get the marking of the tree
				byte[] marking = aStar.getState(rec.getPredecessor().getState()).getHead().getState();
				for (int node = 0; node < tree.size(); node++) {
					if (getState(marking, node) == E) {
						enabledMoves.add(node);
					} else if (getState(marking, node) == T
							&& tree.getTypeFast(configurationNumber, node) == ProbProcessArrayTree.OR) {
						enabledMoves.add(node + tree.size());
					}
				}
			}
			// make sure to process all internal moves, also for the first element of the alignment.
			int[] internal = rec.getInternalMoves();
			for (int i = internal.length; i-- > 0;) {
				if (internal[i] >= tree.size()) {
					// implicit OR termination, i.e. an OR terminated because all its children completed. This should 
					// be ignored in precision as the termination of this OR may not have been enabled (in case of an OR with one child)
					//processModelMove(rec, frequency, prefix, sync, async, internal[i], enabledMoves);
					continue;
				}
				// this node is not an OR-skip and if it is a leaf, it is hidden
				assert internal[i] < tree.size()
						&& (!tree.isLeaf(internal[i]) || tree.isHidden(configurationNumber, internal[i]));
				// check if any of the children was executed synchronously or asynchronously

				// since all children have been completed (if necessary), we can safely check the status of all 
				// of them. If any of them had a synchronous move, then this node has a synchronous move and if
				// any of them has an asynchronous move, then this node has an asynchronous move.
				if (!tree.isLeaf(internal[i])) {
					int j = internal[i] + 1;
					do {
						async[internal[i]] |= async[j];
						sync[internal[i]] |= sync[j];
						// initialize the children (for loops this may be necessary)
						sync[j] = false;
						async[j] = false;
						j = tree.getNext(j);
					} while (j < tree.getNextFast(internal[i]));
				}
				if (sync[internal[i]]) {
					syncMoveCount[internal[i]] += frequency;
				}
				if (async[internal[i]]) {
					aSyncMoveCount[internal[i]] += frequency;
				}
				moveCount[internal[i]] += frequency;
			}

			if (rec.getModelMove() != AStarThread.NOMOVE) {
				int m = rec.getModelMove();
				// In the state "marking", the possible moves are "enabledMoves", 
				// of which "m" was executed, possibly followed by some external 
				// moves stored in "rec".

				// the state is defined by the enabled moves.
				// the visit is defined in the visitCount;

				// Now check for synchronicity of the various moves.
				// We need to traverse the alignment backwards, so first process the various internal moves (if any)

				processModelMove(rec, frequency, prefix, sync, async, m, enabledMoves);

				//			System.out.println("U: " + used);
				//			System.out.println("markingVisit: " + markingVisit);

			}
			rec = rec.getPredecessor();
		} while (rec != null);

	}

	private void processModelMove(TreeRecord rec, int frequency, ModelPrefix prefix, boolean[] sync, boolean[] async,
			int m, TIntList enabledMoves) {
		if (m < tree.size()) {
			moveCount[m] += frequency;
			// we ignore the or-termination. 
			if (tree.isLeaf(m)) {
				prefix.removeLast();
				// a leaf was executed.
				if (tree.getTypeFast(configurationNumber, m) == ProbProcessArrayTree.TAU
						|| rec.getMovedEvent() != AStarThread.NOMOVE) {
					// this was a "skip" or a synchronous move
					sync[m] = true;
					syncMoveCount[m] += frequency;
				} else {
					// this was a move on model only
					async[m] = true;
					aSyncMoveCount[m] += frequency;
				}
			} else {
				// a block node was executed
				// since all children have been completed (if necessary), we can safely check the status of all 
				// of them. If any of them had a synchronous move, then this node has a synchronous move and if
				// any of them has an asynchronous move, then this node has an asynchronous move.
				int j = m + 1;
				do {
					async[m] |= async[j];
					sync[m] |= sync[j];
					// initialize the children (for loops this may be necessary)
					sync[j] = false;
					async[j] = false;
					j = tree.getNext(j);
				} while (j < tree.getNextFast(m));
				if (sync[m]) {
					syncMoveCount[m] += frequency;
				}
				if (async[m]) {
					aSyncMoveCount[m] += frequency;
				}
			}
		}

		TreeMarkingVisit<ModelPrefix> markingVisit = new TreeMarkingVisit<ModelPrefix>(enabledMoves, new ModelPrefix(
				prefix));

		TIntSet used = null;

		//TODO ignore internal moves that are caused by hiding nodes, these TAUs should not be counted as escaping edges!!!
		synchronized (marking2modelmove) {
			synchronized (marking2visitCount) {

				for (TreeMarkingVisit<ModelPrefix> key : marking2modelmove.keySet()) {
					if (key.equals(markingVisit)) {
						// state was visited before.
						used = marking2modelmove.remove(key);
						int visits = marking2visitCount.remove(key);
						markingVisit.getMarking().addAll(key.getMarking());
						marking2modelmove.put(markingVisit, used);
						marking2visitCount.put(markingVisit, visits);
						break;
					}
				}
				if (used == null) {
					used = new TIntHashSet();
					marking2modelmove.put(markingVisit, used);
				}

				/*-
				//Log which node was used to visit a marking
				if(m < tree.size()){
					if(!marking2treenode.containsKey(markingVisit)){
						marking2treenode.put(markingVisit,new TIntHashSet());
					}
					marking2treenode.get(markingVisit).add(m);
				}/**/

				// handle the special case of m >= tree.size() indicating a skip on an OR;
				if (m >= tree.size() || tree.isLeaf(m)) {
					marking2visitCount.adjustOrPutValue(markingVisit, frequency, frequency);
				}
				used.add(m);
			}
		}
	}
}
