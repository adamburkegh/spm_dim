package org.processmining.plugins.etm.mutation.mutators.maikelvaneck;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.tue.astar.AStarThread;
import nl.tue.astar.Trace;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.BehaviorCounter;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

/**
 * This class stores the information derived from a single alignment move. And
 * methods to construct these for a given tree from alignments. And
 * union/intersection methods for lists of moves (mainly for crossover)
 * 
 * @author Maikel van Eck
 */
public class AlignmentMove {

	public final int traceID; // Index of a unique trace in the ConvertedLog
	public final int traceIndex; // Sync & Log only: Index of the move in the trace
	public final int eventClassID; // Event class of the move
	public final int traceCount; // Number of times this unique trace occurs in the original log
	public final int moveType; // One of the four move types

	public static final int SYNC = 0;
	public static final int MODEL = 1;
	public static final int LOG = 2;
	// LOGP is used to map log moves to the last executed synchronous or model move 
	public static final int LOGP = 3;

	public AlignmentMove(int traceID, int traceIndex, int eventClassID, int traceCount, int moveType) {
		this.traceID = traceID;
		this.traceIndex = traceIndex;
		this.eventClassID = eventClassID;
		this.traceCount = traceCount;
		this.moveType = moveType;
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + eventClassID;
		result = prime * result + moveType;
		result = prime * result + traceCount;
		result = prime * result + traceID;
		result = prime * result + traceIndex;
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AlignmentMove other = (AlignmentMove) obj;
		if (eventClassID != other.eventClassID)
			return false;
		if (moveType != other.moveType)
			return false;
		if (traceCount != other.traceCount)
			return false;
		if (traceID != other.traceID)
			return false;
		if (traceIndex != other.traceIndex)
			return false;
		return true;
	}

	public String toString() {
		//return Integer.toString(eventClassID);
		return "(" + eventClassID + "," + traceID + "," + moveType + ")";
	}

	/**
	 * Creates per node a list of all alignment moves mapped to that node
	 * 
	 * @param registry
	 * @param tree
	 * @return
	 */
	public static ArrayList<AlignmentMoveList> getAlignmentMoveMapping(CentralRegistry registry, ProbProcessArrayTree tree) {
		//This method does ROUGHLY the following:
		/*
		 * Loop over alle traces, loop over alle records, map moves to nodes
		 * array van (per node) set van mappedMoves
		 * (traceID(int),eventID(int),#trace voorkomt
		 * count(int),type(MM,LM,Syn)) eventID = !!!eventclass!!! 2 log moves
		 * met zelfde ec? ook eventID = #in trace opslaan!!? Count updaten? Map
		 * log moves to which nodes? Now only to next synch or model moved node,
		 * but should also map to previous moved node (new type) and to all
		 * active nodes (hard to do)
		 * 
		 * Cascade moves to parents for all nodes #children done = 0 (mapping
		 * for all nodes) stack = root while stack != empty pop node van stack
		 * if leaf & notRoot: voeg moves toe aan parent en #children done += 1
		 * else: if #children done < #totalChildren: push node + children op
		 * stack of else: voeg moves toe aan parent en #children done += 1
		 */

		// Create an empty list of alignmentMoves for all nodes in the tree
		ArrayList<AlignmentMoveList> alignmentMoveMap = new ArrayList<AlignmentMoveList>();
		for (int i = 0; i < tree.size(); i++) {
			alignmentMoveMap.add(new AlignmentMoveList());
		}
		// Create a temporary list for log moves
		ArrayList<AlignmentMove> logMoves = new ArrayList<AlignmentMove>();

		// Create the BehaviorCounter and TreeDelegate needed to replay alignments
		BehaviorCounter behC = registry.getFitness(tree).behaviorCounter;

		// Loop over all (unique) traces and map alignmentMoves to nodes
		for (int i = 0; i < registry.getaStarAlgorithm().getConvertedLog().keys().length; i++) {
			Trace trace = (Trace) registry.getaStarAlgorithm().getConvertedLog().keys()[i];
			TreeRecord r = behC.getAlignment(trace);
			List<TreeRecord> history = TreeRecord.getHistory(r);

			// Keep track of the last executed synchronous or model move and clear log moves from previous trace
			int lastSorMMove = -1;
			logMoves.clear();

			// Loop over the record history containing the alignmentMoves for a single (unique) trace
			for (int j = 0; j < history.size(); j++) {
				r = history.get(j);

				if (r.getModelMove() != AStarThread.NOMOVE && r.getMovedEvent() != AStarThread.NOMOVE) {
					// Synchronous move
					alignmentMoveMap.get(r.getModelMove()).add(
							new AlignmentMove(i, r.getMovedEvent(), trace.get(r.getMovedEvent()), registry
									.getaStarAlgorithm().getConvertedLog().get(trace), SYNC));

					// Map all log moves executed since the last synchronous or model move
					alignmentMoveMap.get(r.getModelMove()).addAll(logMoves);
					logMoves.clear();
					lastSorMMove = r.getModelMove();

				} else if (r.getModelMove() != AStarThread.NOMOVE && r.getMovedEvent() == AStarThread.NOMOVE) {
					// Model move
					int m = r.getModelMove();
					if (m >= tree.size()) {
						// Model move on auxiliary node
						m -= tree.size();
					}

					// Only map moves if they are "bad"/visible model moves
					if (tree.isLeaf(m) && tree.getType(m) != ProbProcessArrayTree.TAU) {
						alignmentMoveMap.get(r.getModelMove()).add(
								new AlignmentMove(i, -1, tree.getType(r.getModelMove()), registry.getaStarAlgorithm()
										.getConvertedLog().get(trace), MODEL));

						// Map all log moves executed since the last synchronous or model move
						alignmentMoveMap.get(r.getModelMove()).addAll(logMoves);
						logMoves.clear();
						lastSorMMove = r.getModelMove();
					}

				} else if (r.getModelMove() == AStarThread.NOMOVE && r.getMovedEvent() != AStarThread.NOMOVE) {
					// Log move
					logMoves.add(new AlignmentMove(i, r.getMovedEvent(), trace.get(r.getMovedEvent()), registry
							.getaStarAlgorithm().getConvertedLog().get(trace), LOG));

					// Map log move to last executed synchronous or model move
					if (lastSorMMove > -1) {
						alignmentMoveMap.get(lastSorMMove).add(
								new AlignmentMove(i, r.getMovedEvent(), trace.get(r.getMovedEvent()), registry
										.getaStarAlgorithm().getConvertedLog().get(trace), LOGP));
					}

				}
			}
		}

		// Cascade moves up the tree for all nodes
		int[] nrChildrenMapped = new int[tree.size()];
		Deque<Integer> nodesToMap = new ArrayDeque<Integer>();
		nodesToMap.addFirst(0);

		while (!nodesToMap.isEmpty()) {
			int node = nodesToMap.removeFirst();
			if (tree.isLeaf(node)) {
				if (tree.getParent(node) != ProbProcessArrayTree.NONE) {
					alignmentMoveMap.get(tree.getParent(node)).addAll(alignmentMoveMap.get(node).getAlignmentMoves());
					nrChildrenMapped[tree.getParent(node)] += 1;
				}
			} else {
				if (nrChildrenMapped[node] < tree.nChildren(node)) {
					nodesToMap.addFirst(node);
					for (int i = 0; i < tree.nChildren(node); i++) {
						nodesToMap.addFirst(tree.getChildAtIndex(node, i));
					}
				} else {
					if (tree.getParent(node) != ProbProcessArrayTree.NONE) {
						alignmentMoveMap.get(tree.getParent(node)).addAll(
								alignmentMoveMap.get(node).getAlignmentMoves());
						nrChildrenMapped[tree.getParent(node)] += 1;
					}
				}
			}
		}

		return alignmentMoveMap;
	}

	public static AlignmentMoveList getIntersection(AlignmentMoveList moveList1, AlignmentMoveList moveList2) {
		AlignmentMoveList intersection = new AlignmentMoveList();
		Map<AlignmentMove, Boolean> hashMap = new HashMap<AlignmentMove, Boolean>();

		// Put all synchronous moves from the first list in a HashMap for quick contains checking
		for (AlignmentMove move : moveList1.getAlignmentMoves()) {
			if (move.moveType == SYNC) {
				hashMap.put(move, true);
			}
		}

		// Check for intersections
		for (AlignmentMove move : moveList2.getAlignmentMoves()) {
			if (move.moveType == SYNC) {
				if (hashMap.containsKey(move)) {
					intersection.add(move);
				}
			}
		}

		return intersection;
	}

	public static AlignmentMoveList getUnion(AlignmentMoveList moveList1, AlignmentMoveList moveList2) {
		AlignmentMoveList union = new AlignmentMoveList();
		Map<AlignmentMove, Boolean> hashMap = new HashMap<AlignmentMove, Boolean>();

		// Add all synchronous moves to the HashMap to avoid duplication of moves 
		for (AlignmentMove move : moveList1.getAlignmentMoves()) {
			if (move.moveType == SYNC) {
				hashMap.put(move, true);
				union.add(move);
			}
		}

		for (AlignmentMove move : moveList2.getAlignmentMoves()) {
			if (move.moveType == SYNC) {
				if (!hashMap.containsKey(move)) {
					union.add(move);
				}
			}
		}

		return union;
	}

	public static AlignmentMoveList getUnionIncMM(AlignmentMoveList moveList1, AlignmentMoveList moveList2) {
		AlignmentMoveList union = new AlignmentMoveList();
		Map<AlignmentMove, Boolean> hashMap = new HashMap<AlignmentMove, Boolean>();

		// Add all synchronous moves to the HashMap to avoid duplication of moves 
		for (AlignmentMove move : moveList1.getAlignmentMoves()) {
			if (move.moveType == SYNC || move.moveType == MODEL) {
				hashMap.put(move, true);
				union.add(move);
			}
		}

		for (AlignmentMove move : moveList2.getAlignmentMoves()) {
			if (move.moveType == SYNC || move.moveType == MODEL) {
				if (!hashMap.containsKey(move)) {
					union.add(move);
				}
			}
		}

		return union;
	}

}
