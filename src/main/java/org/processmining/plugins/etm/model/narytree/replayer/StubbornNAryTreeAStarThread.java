package org.processmining.plugins.etm.model.narytree.replayer;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.map.TObjectIntMap;

import java.util.List;

import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.StateBuilder;

import nl.tue.astar.AStarException;
import nl.tue.astar.Delegate;
import nl.tue.astar.Record;
import nl.tue.astar.Tail;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.AbstractAStarThread;
import nl.tue.astar.impl.JavaCollectionStorageHandler;
import nl.tue.astar.impl.State;
import nl.tue.astar.impl.memefficient.MemoryEfficientAStarAlgorithm;
import nl.tue.astar.impl.memefficient.MemoryEfficientStorageHandler;

public abstract class StubbornNAryTreeAStarThread<H extends NAryTreeHead, T extends Tail> extends
		AbstractAStarThread<H, T> {

	/**
	 * CPU efficient variant of the Stubborn set implementation
	 * 
	 * @author bfvdonge
	 * 
	 * @param <H>
	 * @param <T>
	 */
	public static class CPUEfficient<H extends NAryTreeHead, T extends Tail> extends StubbornNAryTreeAStarThread<H, T> {

		public CPUEfficient(ProbProcessArrayTree tree, Delegate<H, T> delegate, TObjectIntMap<H> head2int,
				List<State<H, T>> stateList, H initialHead, Trace trace, int maxStates) throws AStarException {
			super(tree, delegate, trace, maxStates, new JavaCollectionStorageHandler<H, T>(delegate, head2int,
					stateList));
			initializeQueue(initialHead);
		}
	}

	/**
	 * Memory efficient variant of the Stubborn set implementation
	 * 
	 * @author bfvdonge
	 * 
	 * @param <H>
	 * @param <T>
	 */
	public static class MemoryEfficient<H extends NAryTreeHead, T extends Tail> extends
			StubbornNAryTreeAStarThread<H, T> {

		public MemoryEfficient(ProbProcessArrayTree tree, MemoryEfficientAStarAlgorithm<H, T> algorithm, H initialHead,
				Trace trace, int maxStates) throws AStarException {
			super(tree, algorithm.getDelegate(), trace, maxStates, new MemoryEfficientStorageHandler<H, T>(algorithm));
			initializeQueue(initialHead);
		}
	}

	protected final ProbProcessArrayTree tree;
	private final int[] ors;
	private final AbstractNAryTreeDelegate<T> delegate;

	@SuppressWarnings("unchecked")
	public StubbornNAryTreeAStarThread(ProbProcessArrayTree tree, Delegate<H, T> delegate, Trace trace, int maxStates,
			StorageHandler<H, T> storageHandler) {
		super(delegate, trace, maxStates, storageHandler);
		this.tree = tree;
		this.ors = new int[tree.size()];
		this.delegate = (AbstractNAryTreeDelegate<T>) delegate;
		this.delegate.setPushDownUnderAND(false);

	}

	@Override
	protected void processMovesForRecordWithUpToDateTail(Record rec, H head, T tail, int stopAt, double timeLimit,
			long endTime) throws AStarException {

		// signal that this state is being considered
		setConsidered(rec);

		// move model only
		TIntList enabled = head.getModelMoves(rec, delegate);

		TIntCollection nextEvents = rec.getNextEvents(delegate, trace);
		TIntIterator evtIt = nextEvents.iterator();
		int activity = NOMOVE;

		while (evtIt.hasNext()) {
			int nextEvent = evtIt.next();

			TIntList ml = null;

			// move both log and model synchronously;
			activity = trace.get(nextEvent);
			ml = head.getSynchronousMoves(rec, delegate, enabled, activity);

			TIntIterator it = ml.iterator();

			while (it.hasNext()) {
				// syncmove
				int sm = it.next();
				// only allow for synchronous moves within scope
				//				if (sm > scope[0] && sm < scope[1]) {
				processMove(head, tail, rec, sm, nextEvent, activity);
				//				}
			}

			// sorting == ASynchronousMoveSorting.LOGMOVEFIRST implies
			// logMove only after initial move, sync move or log move.
			if (isValidMoveOnLog(rec, nextEvent, activity, enabled, ml)) {
				// allow move on log only if the previous move was
				// 1) the initial move (rec.getPredecessor() == null
				// 2) a synchronous move
				// 3) a log move.
				processMove(head, tail, rec, NOMOVE, nextEvent, activity);
			}

		}

		// sorting == ASynchronousMoveSorting.MODELMOVEFIRST implies
		// modelMove only after initial move, sync move or model move.
		if (isValidMoveOnModel(rec, nextEvents, activity, enabled)) {
			TIntIterator it = enabled.iterator();

			final int[] scope;
			int lastMove = rec.getModelMove();
			if (it.hasNext() && lastMove != NOMOVE && rec.getMovedEvent() == NOMOVE) {
				scope = computeScope(head, lastMove);
			} else {
				scope = new int[] { 0, tree.size() };
			}

			//Determine how we reached rec. If this was through a synchronous or log move, then
			// allow for all enabled model moves (but no children of an OR which can terminate)
			//
			//If we reached rec through a modelMove, allow only moves in the innerMost subtree.

			// we reached rec through a synchronous move or a log move, allow for all moves
			int orPnt = -1;

			while (it.hasNext()) {
				// move model
				int mm = it.next();

				if (mm < tree.size()) {

					while (orPnt >= 0 && mm >= tree.getNextFast(ors[orPnt])) {
						// we passed the scope of the innermost OR
						orPnt--;
					}

					if (mm < scope[0] || mm >= scope[1]) {
						continue;
					}

					if (tree.isLeaf(mm)) {
						int p = tree.getParentFast(mm);
						if (orPnt >= 0 && p == ors[orPnt]) {
							// skip a leaf child of an OR which is allowed to terminate
							continue;
						}
					}
					//				} else if (mm - tree.size() < scope[0] || mm - tree.size() >= scope[1]) {
				} else if (tree.getNextFast(mm - tree.size()) > scope[1]) {
					continue;
				}

				processMove(head, tail, rec, mm, NOMOVE, NOMOVE);

				if (mm >= tree.size()) {
					ors[++orPnt] = mm - tree.size();
					mm -= tree.size();
				}
			}

		}
	}

	private boolean allChildrenN(byte[] state, int scope) {

		boolean allN = true;
		int c = scope + 1;
		do {
			allN &= delegate.getState(state, c) == StateBuilder.N;
			c = tree.getNextFast(c);
		} while (allN && tree.getParent(c) == scope);

		return allN;
	}

	private int[] computeScope(H head, int lastMove) {
		// we reached this record through a modelMove

		byte[] state = head.getState();
		// we reached rec through a model move
		// determine the subtree we are in by looking at the first parent of lastMove which
		// is not state N 
		// We also skip over parents in state F, as these are LOOP-REDO nodes, unless
		// they are OR nodes with at least one child being executed.

		int scope = lastMove > tree.size() ? lastMove - tree.size() : lastMove;
		int lastNode = scope;
		int s = delegate.getState(state, scope);
		// p is the parent of lastMove. 
		while (s == StateBuilder.N
				|| (s == StateBuilder.F && (tree.getTypeFast(scope) != ProbProcessArrayTree.OR || allChildrenN(state, scope)))) {
			scope = tree.getParentFast(scope);
			s = delegate.getState(state, scope);
		}
		// p is the grandparent of lastMove which is not in state N, so allow only for 
		// modelmoves which are in the scope of p
		if (s == StateBuilder.E) {
			// special case. We just finished a subtree which re-enabled itself. Can only be if
			// scope is the middle child of a loop and the leftmost child of that loop is a TAU
			// or a hidden subtree
			assert tree.getTypeFast(tree.getParentFast(scope)) == ProbProcessArrayTree.LOOP;
			assert scope == tree.getNextFast(tree.getParentFast(scope) + 1);
			// make sure to allow for the other sibling to be in the scope
			scope = tree.getParentFast(scope);
		}

		if (scope < lastNode && tree.getTypeFast(scope) == ProbProcessArrayTree.AND) {
			// if the type of node scope is AND, choose the first 
			// child of the AND in which there is something enabled
			// i.e. the first subtree of the AND in state != N
			int i = scope + 1;
			do {
				if ((i < lastNode) || delegate.getState(state, i) == StateBuilder.N) {
					i = tree.getNextFast(i);
				} else {
					return new int[] { i, tree.getNextFast(i) };
				}
			} while (tree.getParent(i) == scope);
			// allow no movel moves
			return new int[] { 0, -1 };
		}
		if (scope < lastNode && tree.getTypeFast(scope) == ProbProcessArrayTree.OR) {
			// if the type of scope is OR, allow for all nodes in 
			// under scope larger than lastMove, but under the OR

			//return new int[] { scope, lastNode };
			return new int[] { lastNode + 1, tree.getNextFast(scope) };
		}

		return new int[] { scope, tree.getNextFast(scope) };
	}

}
