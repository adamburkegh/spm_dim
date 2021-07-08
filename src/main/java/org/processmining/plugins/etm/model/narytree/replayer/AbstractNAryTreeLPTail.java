package org.processmining.plugins.etm.model.narytree.replayer;

import gnu.trove.list.TIntList;

import java.io.IOException;

import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

import lpsolve.LpSolve;
import nl.tue.astar.AStarThread;
import nl.tue.astar.Delegate;
import nl.tue.astar.Head;
import nl.tue.astar.Tail;
import nl.tue.astar.util.LPResult;
import nl.tue.storage.CompressedStore;

public abstract class AbstractNAryTreeLPTail implements Tail {

	public static final double PRECISION = 4E-12;

	// The maximum size of this.super:    24
	protected int estimate; //         8
	protected final short[] variables; //   24 +  length   

	public AbstractNAryTreeLPTail(AbstractNAryTreeLPDelegate<?> delegate, NAryTreeHead h, int minCost) {
		if (minCost < 0) {
			minCost = 0;
		}
		LPResult res = delegate.estimate(h.getState(), h.getParikhVector(), minCost);
		variables = new short[res.getVariables().length];

		int solvedEstimate;
		if (res.getSolveResult() == LpSolve.OPTIMAL) {
			// result is optimal, use the result
			for (int i = 0; i < res.getVariables().length; i++) {
				int var = (int) (res.getVariable(i) + 0.5);
				variables[i] = (short) var;
			}
			solvedEstimate = (int) (res.getResult() + 0.5);
		} else {
			// result is relaxed-optimal, round down.
			for (int i = 0; i < res.getVariables().length; i++) {
				int j = (int) (res.getVariable(i));
				if (j < 0) {
					j = 0;
				}
				variables[i] = (short) j;
			}
			solvedEstimate = (int) (res.getResult() + PRECISION);
		}

		if (solvedEstimate < minCost) {
			solvedEstimate = minCost;
		}
		this.estimate = solvedEstimate;
	}

	public AbstractNAryTreeLPTail(int estimate, short[] variables) {
		this.estimate = estimate;
		this.variables = variables;

	}

	public abstract AbstractNAryTreeLPTail getNewTail(int estimate, short[] variables);

	public abstract AbstractNAryTreeLPTail getNewTail(AbstractNAryTreeDelegate<?> delegate, NAryTreeHead h, int minCost);

	@SuppressWarnings("unchecked")
	public Tail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head head, int modelMove, int logMove,
			int activity) {
		AbstractNAryTreeLPDelegate<AbstractNAryTreeLPTail> delegate = ((AbstractNAryTreeLPDelegate<AbstractNAryTreeLPTail>) d);
		//assert modelMove == AStarThread.NOMOVE || movesMade != null;

		AbstractNAryTreeLPTail newTail = null;
		// check if the is move was allowed according to the LP:
		int costMade = ((NAryTreeHead) head).getMovesMadeCost();// (movesMade == null ? 0 : movesMade.size());
		boolean canDerive;

		if (modelMove == AStarThread.NOMOVE) {
			costMade += delegate.getLogMoveCost((short) activity);
			// logMove only.

			int var = delegate.getTree().size() + activity;
			if ((variables[var] & 0xFFFF) >= 1) {
				// move was allowed according to LP.
				short[] newVars = new short[variables.length];
				System.arraycopy(variables, 0, newVars, 0, variables.length);
				newVars[var] -= 1;
				newTail = getNewTail(estimate - costMade, newVars);
			}
		} else {

			final short[] newVars = new short[variables.length];
			System.arraycopy(variables, 0, newVars, 0, variables.length);
			final ProbProcessArrayTree tree = delegate.getTree();
			final byte[] state = ((NAryTreeHead) head).getState();

			if (logMove == AStarThread.NOMOVE) {
				// Move on model.
				//  
				//				if (delegate.getTree().getTypeFast(modelMove) != NAryTree.OR
				//						|| delegate.getState(((NAryTreeHead) head).getState(), modelMove) != StateBuilder.N) {
				costMade += delegate.getModelMoveCost((short) modelMove);
				canDerive = updateVariables(delegate, tree, state, newVars, modelMove, false);

			} else {
				// synchronous move. Compute the corresponding variable.
				//				int leaf = tree.isLeaf(0) ? 0 : tree.getNextLeafFast(0);
				//				int leafNumber = 0;
				//				while (leaf != modelMove) {
				//					leafNumber += tree.getTypeFast(leaf) == NAryTree.TAU ? 0 : 1;
				//					leaf = tree.getNextLeafFast(leaf);
				//				}
				costMade++;
				int var = delegate.getSyncMoveVar(modelMove);
				canDerive = updateVariables(delegate, tree, state, newVars, var, true);
			}

			TIntList movesMade = ((NAryTreeHead) head).getMovesMade();
			if (canDerive && movesMade != null) {
				for (int i = movesMade.size(); canDerive && i-- > 0;) {
					canDerive &= updateVariables(delegate, tree, state, newVars, movesMade.get(i), false);
				}
			}

			if (canDerive) {
				// all moves made by the model, were allowed according to the LP
				assert estimate - costMade >= 0;
				newTail = getNewTail(estimate - costMade, newVars);
			}

		}

		if (newTail == null) {
			newTail = getNewTail(delegate, (NAryTreeHead) head, estimate - costMade);
			//		} else {
			//
			//			AbstractNAryTreeLPTail nt = getNewTail(delegate, (NAryTreeHead) head, 0);
			//			if (nt instanceof FastLowerBoundTail) {
			//				((FastLowerBoundTail) nt).computeEstimate(delegate, head, 0);
			//			}
			//			if (nt.getEstimate() != newTail.getEstimate()) {
			//				// stop
			//				System.err.println("oops");
			//				nt = getNewTail(delegate, (NAryTreeHead) head, 0);
			//				if (nt instanceof FastLowerBoundTail) {
			//					((FastLowerBoundTail) nt).computeEstimate(delegate, head, 0);
			//				}
			//			}
		}

		return newTail;
	}

	protected boolean updateVariables(AbstractNAryTreeDelegate<?> delegate, ProbProcessArrayTree tree, byte[] state, short[] vars,
			int modelMove, boolean syncMove) {
		// Check if the given modelMove was allowed according to the LP. 
		// If so, reduce the variable with 1 and return true
		// If not, return false.

		// For the termination of an OR, such a check cannot (easily) be made
		if (!syncMove && modelMove >= tree.size()) {

			// since we do not have access to the previous state, we cannot check
			// if the solution allowed for skips on all previously enabled children of the OR
			return false;
		}

		// What if the latest move implicitly terminated an OR. This potentially invalidates
		// the LP since the LP row for that OR may now have a tighter upperbound value.

		int v = (vars[modelMove] & 0xFFFF) - 1;
		vars[modelMove] = (short) v;
		return v >= 0;

	}

	public abstract <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d,
			CompressedStore<S> store, long index, int modelMove, int logMove, int activity) throws IOException;

	public int getEstimatedCosts(Delegate<? extends Head, ? extends Tail> d, Head head) {
		return estimate;
	}

	public boolean canComplete() {
		return estimate >= 0;
	}

	public int getEstimate() {
		return estimate;
	}

	public short[] getVariables() {
		return variables;
	}

	public String toString() {
		return "e:" + estimate + " " + toString(variables);
	}

	public static String toString(short[] a) {
		if (a == null)
			return "null";
		int iMax = a.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			b.append(a[i] & 0xFFFF);
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

}
