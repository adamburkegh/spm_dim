package org.processmining.plugins.etm.fitness;

import gnu.trove.TCollections;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import nl.tue.astar.Trace;

import org.processmining.plugins.boudewijn.treebasedreplay.astar.ModelPrefix;
import org.processmining.plugins.boudewijn.treebasedreplay.astar.TreeMarkingVisit;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;

// FIXME make n-ary and re-think

/*
 * IDEA: since the behC is significantly larger than the NAryTreeImpl we need to
 * somehow decide which information is required and which is only usefull for
 * better trees
 */
public class BehaviorCounter {
	//TODO sync and model for leafs only?
	//TODO lighten this object, this is really heavy for larger trees/logs, especially since it is kept in memory for ALL trees in Pareto Front + population (+ history)

	/**
	 * # executions of this subtree containing 1+ sync. moves
	 */
	private int[] syncMoveCount;

	/**
	 * # executions of this subtree containing 1+ async. moves
	 */
	private int[] aSyncMoveCount;

	/**
	 * # execution of this subtree/node
	 */
	private int[] moveCount;

	/**
	 * 
	 */
	private Map<TreeMarkingVisit<ModelPrefix>, TIntSet> marking2ModelMove = null;
	private TObjectIntMap<TreeMarkingVisit<ModelPrefix>> marking2VisitCount = null;

	private Map<Trace, TreeRecord> alignments;

	private int minModelCost;

	/**
	 * How often the node at that index moved synchronously (only leafs will
	 * move sync.)
	 */
	//private int[] syncMoves;

	/**
	 * How often the node at that index moved model only (non-leaf nodes will
	 * always move on model only, leafs might move on model only)
	 */
	//private int[] modelMoves;

	/**
	 * This array contains before which LEAF, which event class was moved on log
	 * only and how often that happened in total. The length of the first array
	 * is one more than the number of leafs to also capture log moves that
	 * happen at the end.
	 * 
	 * E.g. the map at position x denotes which log moves (by XEventClass id)
	 * occurred before leaf x was executed (synchronously or move on model only)
	 * but after leaf x-1, according to the alignments. The array of log moves
	 * is of length XEventClass and the value at a index indicates how often
	 * that event class was observed as a log move in that location
	 */
	//private int[][] logMoves;

	/**
	 * Per non-leaf node (first index) the orders (2nd index) in which the
	 * children (start/end) were executed and how often. (Example for a single
	 * node [ [{0,0,1,1,5}], [{1,1,0,0,9}] ] ) means 5 times child 0 followed by
	 * child 1 and 9 times v.v., never interleaved).
	 * 
	 * This 3D array is of size: tree.size() * nrOrders * (nrChildren*2)+1
	 * 
	 * (+1 for denoting the number of occurrences of that order, tree.size()
	 * could be replaced by tree.size()-nrLeafs but ~complex translations are
	 * needed then)
	 * 
	 * For AND and OR this works. For LOOP you have unlimited length (e.g.
	 * loopbacks), for XOR you have a fixed order size of 2, recording how often
	 * each child was executed (start+end)
	 */
	//private int[][][] childOrder;

	/*
	 * IMPLEMENTED AND WORKING
	 */

	/**
	 * For leafs only, stores the logMoves succeeding a modelMove on that leaf,
	 * before the next synchronous move, but including the first synchronous
	 * move.
	 */
	//private TObjectIntMap<XEventClass> logMoveCount = new TObjectIntHashMap<XEventClass>();
	/**
	 * For AND and OR nodes only, stores the number of times, the subtree of the
	 * given node was executed first, before starting the second child.
	 */
	//private int[] firstSubtreeCount;
	/**
	 * For AND and OR nodes only, stores the number of times, the subtree of the
	 * given node was executed last, after completing the first child.
	 */
	//private int[] lastSubtreeCount;

	/**
	 * Instantiates a new (empty) behavior counter to be filled (mainly) by the
	 * {@link org.processmining.plugins.etm.fitness.metrics.FitnessReplay}
	 * fitness metric.
	 * 
	 * @param candidateSize
	 */
	public BehaviorCounter(int candidateSize) {
		syncMoveCount = new int[candidateSize];
		aSyncMoveCount = new int[candidateSize];
		moveCount = new int[candidateSize];
		marking2ModelMove = new HashMap<TreeMarkingVisit<ModelPrefix>, TIntSet>(candidateSize);
		marking2VisitCount = new TObjectIntHashMap<TreeMarkingVisit<ModelPrefix>>(candidateSize);
	}

	public BehaviorCounter(int[] syncMoveCount, int[] aSyncMoveCount, int[] moveCount,
			HashMap<TreeMarkingVisit<ModelPrefix>, TIntSet> marking2modelMove,
			TObjectIntMap<TreeMarkingVisit<ModelPrefix>> marking2visitCount, Map<Trace, TreeRecord> alignments) {
		this.syncMoveCount = syncMoveCount;
		this.aSyncMoveCount = aSyncMoveCount;
		this.moveCount = moveCount;
		this.marking2ModelMove = marking2modelMove;
		this.marking2VisitCount = marking2visitCount;
		this.setAlignments(alignments);
	}

	public BehaviorCounter(BehaviorCounter behC) {
		this.syncMoveCount = behC.syncMoveCount;
		this.aSyncMoveCount = behC.aSyncMoveCount;
		this.moveCount = behC.moveCount;
		this.marking2ModelMove = behC.marking2ModelMove;
		this.marking2VisitCount = behC.marking2VisitCount;
	}

	public String toString() {
		return "    Sync. move: " + Arrays.toString(syncMoveCount) + " \n" + //
				"    aSync. mov: " + Arrays.toString(aSyncMoveCount) + " \n" + //
				"    total move: " + Arrays.toString(moveCount);
	}

	/**
	 * Returns true if moves are recorded
	 * 
	 * @return
	 */
	public boolean isSet() {
		return !(moveCount[0] == 0);
	}

	/**
	 * @return the syncMoveCount
	 */
	public int[] getSyncMoveCount() {
		return syncMoveCount;
	}

	/**
	 * @return the aSyncMoveCount
	 */
	public int[] getASyncMoveCount() {
		return aSyncMoveCount;
	}

	/**
	 * @return the moveCount
	 */
	public int[] getMoveCount() {
		return moveCount;
	}

	/**
	 * @param syncMoveCount
	 *            the syncMoveCount to set
	 */
	public void setSyncMoveCount(int[] syncMoveCount) {
		this.syncMoveCount = syncMoveCount;
	}

	/**
	 * @param aSyncMoveCount
	 *            the aSyncMoveCount to set
	 */
	public void setASyncMoveCount(int[] aSyncMoveCount) {
		this.aSyncMoveCount = aSyncMoveCount;
	}

	/**
	 * @param moveCount
	 *            the moveCount to set
	 */
	public void setMoveCount(int[] moveCount) {
		this.moveCount = moveCount;
	}

	/**
	 * The Marking to Model move map records for all markings (which is a set of
	 * enabled nodes, the marking, and the sequence of nodes followed to get to
	 * the marking), the set of model moves that have been made from this
	 * marking (considering model moves and synchronous moves).
	 * 
	 * @return the marking2modelmove
	 */
	public synchronized Map<TreeMarkingVisit<ModelPrefix>, TIntSet> getMarking2ModelMove() {
		return marking2ModelMove;
	}

	/**
	 * The Marking to Visit Count map records for all markings (which is a set
	 * of enabled nodes, the marking, and the sequence of nodes followed to get
	 * to the marking) how often it was visited while replaying the entire log
	 * (considering model moves and synchronous moves).
	 * 
	 * @return the marking2visitCount
	 */
	public synchronized TObjectIntMap<TreeMarkingVisit<ModelPrefix>> getMarking2VisitCount() {
		return marking2VisitCount;
	}

	/**
	 * @param marking2modelMove
	 *            the marking2modelMove to set
	 */
	public void setMarking2ModelMove(Map<TreeMarkingVisit<ModelPrefix>, TIntSet> marking2ModelMove) {
		//this.marking2ModelMove = marking2ModelMove;
		this.marking2ModelMove = Collections.synchronizedMap(marking2ModelMove);
	}

	/**
	 * @param marking2visitCount
	 *            the marking2visitCount to set
	 */
	public void setMarking2VisitCount(TObjectIntMap<TreeMarkingVisit<ModelPrefix>> marking2VisitCount) {
		//this.marking2VisitCount = marking2VisitCount;
		this.marking2VisitCount = TCollections.synchronizedMap(marking2VisitCount);
	}

	/**
	 * @return the alignments
	 */
	public Map<Trace, TreeRecord> getAlignments() {
		if (alignments == null) {
			alignments = new HashMap<Trace, TreeRecord>();
		}
		return alignments;
	}

	/**
	 * Returns the alignment for a particular trace
	 * 
	 * @param trace
	 * @return
	 */
	public TreeRecord getAlignment(Trace trace) {
		return alignments.get(trace);
	}

	/**
	 * @param alignments
	 *            the alignments to set
	 */
	public void setAlignments(Map<Trace, TreeRecord> alignments) {
		this.alignments = alignments;
	}

	/**
	 * Returns TRUE if there is alignment inforamtion available, otherwise
	 * returns false.
	 * 
	 * @return
	 */
	public boolean isAlignmentSet() {
		return alignments != null && !alignments.isEmpty();
	}

	/**
	 * @return the minModelCost
	 */
	public int getMinModelCost() {
		return minModelCost;
	}

	/**
	 * @param minModelCost
	 *            the minModelCost to set
	 */
	public void setMinModelCost(int minModelCost) {
		this.minModelCost = minModelCost;
	}

}
