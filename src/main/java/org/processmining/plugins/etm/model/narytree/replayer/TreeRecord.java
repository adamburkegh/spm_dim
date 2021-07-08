package org.processmining.plugins.etm.model.narytree.replayer;

import gnu.trove.TIntCollection;
import gnu.trove.list.TIntList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.tue.astar.AStarThread;
import nl.tue.astar.Delegate;
import nl.tue.astar.Head;
import nl.tue.astar.Record;
import nl.tue.astar.Tail;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.State;
import nl.tue.storage.CompressedStore;
import nl.tue.storage.StorageException;
import nl.tue.storage.compressor.BitMask;

import org.processmining.plugins.boudewijn.treebasedreplay.TreeDelegate;
import org.processmining.plugins.boudewijn.treebasedreplay.TreeHead;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class TreeRecord implements Record {
	//                              header: 16 bytes 
	private long state; //                   8 bytes
	private final int cost; //               4 bytes
	private final TreeRecord predecessor; // 8 bytes
	private final int modelMove; //          4 bytes 
	private final int movedEvent; //         4 bytes 
	private final int modelMoveCount; //          4 bytes
	private double totalCost; //                4 bytes
	private final BitMask executed; //       8 bytes (plus size of object if movedEvent!= AStarThread.NOMOVE
	private final int[] internalMoves; //   >
	private final int internalMoveCost;
	private boolean exact;
	private final int backtraceSize;

	private TreeRecord(long state, int cost, TreeRecord predecessor, int modelMove, int movedEvent, int backtrace,
			int[] internalMoves, int internalMoveCost, BitMask executed, int backTraceSize) {
		this.state = state;
		this.cost = cost;
		this.predecessor = predecessor;
		this.modelMove = modelMove;
		this.movedEvent = movedEvent;
		this.modelMoveCount = backtrace;
		this.internalMoves = internalMoves;
		this.internalMoveCost = internalMoveCost;
		this.executed = executed;
		this.backtraceSize = backTraceSize;
	}

	public TreeRecord(int cost, TIntList internalMoves, int internalMoveCost, int traceLength, boolean isExact) {
		this.cost = cost;
		this.internalMoveCost = internalMoveCost;
		this.predecessor = null;
		this.modelMove = AStarThread.NOMOVE;
		this.movedEvent = AStarThread.NOMOVE;
		this.modelMoveCount = cost;
		if (internalMoves != null) {
			this.internalMoves = new int[internalMoves.size()];
			for (int i = 0; i < internalMoves.size(); i++) {
				this.internalMoves[i] = internalMoves.get(i);
			}
		} else {
			this.internalMoves = null;
		}
		this.executed = new BitMask(traceLength);
		this.backtraceSize = 0;
		this.exact = isExact;

	}

	public <H extends Head, T extends Tail> State<H, T> getState(CompressedStore<State<H, T>> storage)
			throws StorageException {
		return storage.getObject(state);
	}

	public long getState() {
		return state;
	}

	public int getCostSoFar() {
		return cost;
	}

	public double getTotalCost() {
		return totalCost;
	}

	public TreeRecord getPredecessor() {
		return predecessor;
	}

	public void setState(long index) {
		this.state = index;
	}

	/**
	 * In case of a LogMove only, then logMove>=0, modelMove ==
	 * AStarThread.NOMOVE,
	 * 
	 * In case of a ModelMove only, then logMove == AStarThread.NOMOVE,
	 * modelMove >=0,
	 * 
	 * in case of both log and model move, then logMove>=0, modelMove>=0,
	 * 
	 */
	public TreeRecord getNextRecord(Delegate<? extends Head, ? extends Tail> d, Trace trace, Head nextHead, long state,
			int modelMove, int movedEvent, int activity) {
		TreeDelegate<?, ?> delegate = (TreeDelegate<?, ?>) d;

		int c = delegate.getCostFor(modelMove, activity);
		int ci = 0;

		int[] internalMoves = null;

		TIntList moves = ((TreeHead) nextHead).getMovesMade();
		if (moves != null && moves.size() > 0) {
			internalMoves = new int[moves.size()];
			for (int i = moves.size(); i-- > 0;) {
				internalMoves[i] = moves.get(i);
				ci += delegate.getModelMoveCost(internalMoves[i]);
			}
		}
		BitMask newExecuted;
		if (movedEvent != AStarThread.NOMOVE) {
			newExecuted = executed.clone();
			newExecuted.set(movedEvent, true);
			//			newExecuted = Arrays.copyOf(executed, executed.length);
			//			newExecuted[movedEvent] = true;
		} else {
			newExecuted = executed;
		}

		TreeRecord r = new TreeRecord(state, cost + c + ci, this, modelMove, movedEvent, modelMoveCount
				+ (modelMove == AStarThread.NOMOVE ? 0 : modelMove < delegate.getTree().size() ? 1 : 0), internalMoves,
				ci, newExecuted, backtraceSize + 1);
		return r;
		//		}
	}

	public double getEstimatedRemainingCost() {
		return this.totalCost - this.cost;
	}

	public void setEstimatedRemainingCost(double estimate, boolean isExactEstimate) {
		this.exact = isExactEstimate;
		this.totalCost = this.cost + estimate;

	}

	public boolean equals(Object o) {
		return (o instanceof Record) && ((Record) o).getState() == state;
		//&& ((Record) o).getCostSoFar() == cost
		//&& ((Record) o).getEstimatedRemainingCost() == estimate;
	}

	public int hashCode() {
		return (int) state;
	}

	public String toString() {
		return "(" + (getMovedEvent() == AStarThread.NOMOVE ? "_" : getMovedEvent()) + ","
				+ (getModelMove() == AStarThread.NOMOVE ? "_" : getModelMove()) + ")"
				+ (internalMoves == null ? "" : Arrays.toString(internalMoves));
	}

	public int getModelMove() {
		return modelMove;
	}

	public static List<TreeRecord> getHistory(TreeRecord r) {
		if (r == null) {
			return Collections.emptyList();
		}
		List<TreeRecord> history = new ArrayList<TreeRecord>(r.getModelMoveCount());
		while (r != null) {
			history.add(0, r);
			r = r.getPredecessor();
		}
		return history;
	}

	public static void printRecord(TreeDelegate<?, ?> delegate, Trace trace, TreeRecord r) {
		List<TreeRecord> history = getHistory(r);

		for (int i = 0; i < history.size(); i++) {
			r = history.get(i);
			String s = "(";
			short act = -1;
			if (r.getMovedEvent() != AStarThread.NOMOVE) {
				act = (short) trace.get(r.getMovedEvent());
			}
			if (r.getModelMove() == AStarThread.NOMOVE) {
				s += "_";
			} else {
				short m = (short) r.getModelMove();
				s += delegate.toString(m, act);
			}

			s += ",";
			// r.getLogEvent() is the event that was moved, or AStarThread.NOMOVE
			if (r.getMovedEvent() == AStarThread.NOMOVE) {
				s += "_";
			} else {
				assert (act >= 0 || act < 0);
				s += delegate.getEventClass(act);
			}
			s += ")";
			s += "c:" + r.getCostSoFar();
			s += ",e:" + r.getEstimatedRemainingCost();
			s += ",b:" + r.getModelMoveCount();
			s += ",im: " + (r.internalMoves == null ? "[]" : Arrays.toString(r.internalMoves));
			s += (i < history.size() - 1 ? " --> " : " cost: " + (r.getCostSoFar()));

			System.out.print(s);
		}
		System.out.println();
	}

	public int getMovedEvent() {
		return movedEvent;
	}

	public TIntCollection getNextEvents(Delegate<? extends Head, ? extends Tail> delegate, Trace trace) {
		return trace.getNextEvents(executed);
	}

	public int getModelMoveCount() {
		return modelMoveCount;
	}

	public int[] getInternalMoves() {
		return internalMoves == null ? new int[0] : internalMoves;
	}

	public double getInternalMovesCost() {
		return internalMoveCost;
	}

	public boolean isExactEstimate() {
		return exact;
	}

	public int getBacktraceSize() {
		return backtraceSize;
	}

	// Rudimentary checker for the alignments.
	// 1. Log part 
	// 1.1 Checks that the log projection is sequential and in increasing order
	// 1.2 Checks that all the events of the trace are projected.
	// 1.3 Checks that all the events of the trace are projected only once.
	// 1.4 Checks that there are no missing log projections
	// 1.5 Checks that there are no mismatches in the sync moves: a leaf pointing to a different event
	// 2. Model part
	// 2.1 Checks that the or-join moves are correctly shifted.
	// 2.3 Checks that the internal moves do not point to leafs
	// 3. Extra checks
	// 3.1 Checks that the total cost is the same as the input
	// 3.2 Checks the backtraceCounter (this is important for allocating the history -> memory performance)
	public static boolean isConsistent(TreeRecord newRecord, Trace originalTrace, ProbProcessArrayTree tree, int scaling) {
		TreeRecord a = newRecord;
		int newTraceCost = 0;
		int[] auxC = new int[originalTrace.getSize()];
		int auxCC = 0;
		int backtraceCounter = a.getBacktraceSize();
		int adder = originalTrace.getSize() - 1;
		boolean consistent = true;
		do {
			if (backtraceCounter-- != a.getBacktraceSize()) {
				System.out.println("inconsistent backtrace counter = " + a);
				consistent = false;
			}
			final int modelMove = a.getModelMove();
			final int logMove = a.getMovedEvent();
			if (logMove >= 0 && modelMove >= 0) {
				if (logMove != adder) {
					System.out.println("the log/sync moves are unsorted = " + a);
					consistent = false;
				}
				adder--;
				if (tree.getType(modelMove) != originalTrace.get(logMove)) {
					System.out.println(logMove + ":" + tree.getType(modelMove) + ":" + originalTrace.get(logMove));
					System.out.println("mismatch in a sync move = " + a);
					consistent = false;
				}
				if (auxC[logMove] == 0) {
					auxC[logMove] = 1;
				} else {
					System.out.println("same log/sync move twice = " + a);
					consistent = false;
				}
				auxCC++;
			} else if (logMove >= 0 && modelMove == AStarThread.NOMOVE) { // log move
				newTraceCost += CentralRegistry.MLcost;
				if (logMove != adder) {
					System.out.println("the sync/log moves are unsorted = " + a);
					consistent = false;
				}
				adder--;
				if (auxC[logMove] == 0) {
					auxC[logMove] = 1;
				} else {
					System.out.println("same log/sync move twice = " + a);
					consistent = false;
				}
				auxCC++;
			} else if (modelMove >= 0 && logMove == AStarThread.NOMOVE) { // model move
				int type = tree.getType(modelMove);
				if (type >= 0 && type != ProbProcessArrayTree.TAU) {
					newTraceCost += CentralRegistry.MMcost;
				}
				if (modelMove >= tree.size()) {
					if (tree.getType(modelMove - tree.size()) != ProbProcessArrayTree.OR) {
						System.out.println("Or-join incorrectly shifted = " + a);
						consistent = false;
					}
				}
			}
			for (int i = 0; i < a.getInternalMoves().length; i++) {
				if (a.getInternalMoves()[i] < tree.size()) {
					if (tree.getType(a.getInternalMoves()[i]) < 0) {
					} else {
						System.out.println("Internal move leading to a leaf = " + a);
						consistent = false;
					}
				}
			}
			a = a.getPredecessor();
		} while (a != null && consistent);
		if (consistent) {
			if (newRecord.getCostSoFar()/scaling != newTraceCost) {
				System.out.println("Different cost than the calculated during the repair process" + newRecord.getCostSoFar()/scaling  + "!="
						+ newTraceCost);
				consistent = false;
			}
			if (auxCC != originalTrace.getSize()) {
				System.out.println("Missing log/sync moves");
				consistent = false;
			}
			if (backtraceCounter < -1) {
				System.out.println("wrong backtrace size");
				consistent = false;
			}
		}
		return consistent;
	}

	private void setCost(int cost) {
		//this.cost = cost; //FIXME JB don't wnt to make this field non-final, kept method private to see where it is used
	}
	

}
