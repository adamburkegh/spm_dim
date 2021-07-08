package org.processmining.plugins.etm.model.narytree.replayer;

import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;

import nl.tue.astar.AStarThread;
import nl.tue.astar.Delegate;
import nl.tue.astar.Head;
import nl.tue.astar.Record;
import nl.tue.astar.Tail;
import nl.tue.astar.Trace;
import nl.tue.astar.util.ShortShortMultiset;

import org.processmining.plugins.boudewijn.treebasedreplay.TreeHead;
import org.processmining.plugins.etm.model.ppt.StateBuilder;

public class NAryTreeHead implements TreeHead {

	//	protected static final TreeHashCodeProvider PROVIDER = new TreeHashCodeProvider();

	private final byte[] state;
	private final ShortShortMultiset parikh; //       8 +  2 * numactivities + 48
	private final int hashCode; //                    4
	private TIntList movesMade;
	private int movesMadeCost = 0;

	public static int computeBitsForParikh(short acts, int nodes) {
		// since activities are in a parikh vector and nodes are not, count the activities double
		// use at least 8 bits but at most the number of activities.
		return Math.min(acts, Math.max(8, (int) (32.0 * 1.5 * acts / (1.5 * acts + nodes))));
	}

	public NAryTreeHead(AbstractNAryTreeDelegate<?> delegate, Trace trace) {
		// the number of nodes is assumed to fit in a short and is final.

		movesMade = new TIntArrayList();

		this.parikh = new ShortShortMultiset(delegate.numEventClasses());
		TIntIterator it = trace.iterator();
		while (it.hasNext()) {
			this.parikh.adjustValue((short) it.next(), (short) 1);
		}

		StateBuilder builder = delegate.getStateBuilder();
		byte[] state = builder.initializeState();
		// we have to execute node "modelMove" in the model
		// and continue with all nodes which are implicitly executed.
		if (builder.getQueue() != StateBuilder.MEMORYLESSQUEUE) {
			synchronized (delegate) {
				assert builder.getQueue().isEmpty();
				if (!delegate.isLeaf(0) && !delegate.isBlocked(0)) {
					builder.getQueue().add(0);
					while (!builder.getQueue().isEmpty()) {
						int node = builder.getQueue().poll();
						//				if (node > 0) {
						movesMade.add(node);
						movesMadeCost += delegate.getModelMoveCost(node);
						//				}
						state = builder.execute(state, node, builder.getQueue());
					}
				}
			}
		}
		this.state = state;

		hashCode = hash(state, parikh);

	}

	NAryTreeHead(ShortShortMultiset parikh, byte[] state, int hashCode, TIntList movesMade, int movesMadeCost) {
		this.state = state;
		this.parikh = parikh;
		this.movesMade = movesMade;
		this.hashCode = hashCode;
		this.movesMadeCost = movesMadeCost;
	}

	public Head getNextHead(Record rec, Delegate<? extends Head, ? extends Tail> d, int first, TIntCollection modelMoves) {
		AbstractNAryTreeDelegate<?> delegate = (AbstractNAryTreeDelegate<?>) d;

		final ShortShortMultiset newParikh = parikh;
		TIntList newMovesMade = null;

		int newMovesMadeCost = 0;
		byte[] newState = state;
		newMovesMade = new TIntArrayList();
		// we have to execute node "modelMove" in the model
		// and continue with all nodes which are implicitly executed.
		StateBuilder builder = delegate.getStateBuilder();
		//		synchronized (delegate) {
		if (builder.getQueue() != StateBuilder.MEMORYLESSQUEUE) {
			assert builder.getQueue().isEmpty();
			builder.getQueue().add(first);
			TIntIterator it = modelMoves.iterator();
			while (it.hasNext()) {
				builder.getQueue().add(it.next());
			}
			while (!builder.getQueue().isEmpty()) {
				int node = builder.getQueue().poll();
				if (node != first) {
					newMovesMade.add(node);
					newMovesMadeCost += delegate.getModelMoveCost(node);
				}
				newState = builder.execute(newState, node, builder.getQueue());
			}
		} else {
			newState = builder.execute(newState, first);
			TIntIterator it = modelMoves.iterator();
			while (it.hasNext()) {
				newState = builder.execute(newState, it.next());
			}
		}
		//		}

		return new NAryTreeHead(newParikh, newState, hash(newState, newParikh), newMovesMade, newMovesMadeCost);
	}

	protected int hash(byte[] state, ShortShortMultiset parikh) {
		return 31 * Arrays.hashCode(state) + Arrays.hashCode(parikh.getInternalValues());
	}

	public Head getNextHead(Record rec, Delegate<? extends Head, ? extends Tail> d, int modelMove, int logMove,
			int activity) {

		AbstractNAryTreeDelegate<?> delegate = (AbstractNAryTreeDelegate<?>) d;

		final ShortShortMultiset newParikh;
		TIntList newMovesMade = null;

		if (logMove != AStarThread.NOMOVE) {
			// there is a logMove
			newParikh = parikh.clone();
			assert (newParikh.get((short) activity) > 0);
			newParikh.adjustValue((short) activity, (short) -1);
		} else {
			newParikh = parikh;
		}

		int newMovesMadeCost = 0;
		byte[] newState = state;
		if (modelMove != AStarThread.NOMOVE) {
			newMovesMade = new TIntArrayList();
			// we have to execute node "modelMove" in the model
			// and continue with all nodes which are implicitly executed.
			StateBuilder builder = delegate.getStateBuilder();
			//			synchronized (delegate) {
			if (builder.getQueue() != StateBuilder.MEMORYLESSQUEUE) {
				assert builder.getQueue().isEmpty();
				builder.getQueue().add(modelMove);
				while (!builder.getQueue().isEmpty()) {
					int node = builder.getQueue().poll();
					if (node != modelMove) {
						newMovesMade.add(node);
						newMovesMadeCost += delegate.getModelMoveCost(node);
					}
					newState = builder.execute(newState, node, builder.getQueue());
				}
			} else {
				newState = builder.execute(newState, modelMove);
			}
			//			}
		}
		return new NAryTreeHead(newParikh, newState, hash(newState, newParikh), newMovesMade, newMovesMadeCost);
	}

	/**
	 * This method assumes that the caller only iterates over the list and
	 * nothing else.
	 */
	public TIntList getSynchronousMoves(Record rec, Delegate<? extends Head, ? extends Tail> delegate,
			TIntList enabled, int activity) {
		return new TIntListIteratorShell((AbstractNAryTreeDelegate<?>) delegate, state, (short) activity);
	}

	/**
	 * This method assumes that the caller only iterates over the list and
	 * nothing else.
	 */
	public TIntList getModelMoves(Record rec, final Delegate<? extends Head, ? extends Tail> delegate) {
		return new TIntListIteratorShell((AbstractNAryTreeDelegate<?>) delegate, state);
	}

	public boolean isFinal(Delegate<? extends Head, ? extends Tail> delegate) {
		return parikh.isEmpty() && ((AbstractNAryTreeDelegate<?>) delegate).isFinal(state);
	}

	public ShortShortMultiset getParikhVector() {
		return parikh;
	}

	public TIntList getMovesMade() {
		return movesMade;
	}

	public int getMovesMadeCost() {
		return movesMadeCost;
	}

	public byte[] getState() {
		return state;
	}

	public int hashCode() {
		return hashCode;
	}

	public boolean equals(Object o) {
		if (!(o instanceof NAryTreeHead)) {
			return false;
		}
		ShortShortMultiset p = ((NAryTreeHead) o).parikh;
		byte[] s = ((NAryTreeHead) o).state;
		return ((NAryTreeHead) o).hashCode == hashCode && p.equals(parikh) && Arrays.equals(s, state);
	}

	public String toString() {
		return "[h:" + hashCode + ",s:" + Arrays.toString(state) + ",p:" + parikh + "]";
	}

}
