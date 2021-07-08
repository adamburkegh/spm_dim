package org.processmining.plugins.etm.model.narytree.replayer.basisilp;

import gnu.trove.list.TIntList;

import java.io.IOException;
import java.io.InputStream;

import lpsolve.LpSolve;
import nl.tue.astar.AStarThread;
import nl.tue.astar.Delegate;
import nl.tue.astar.FastLowerBoundTail;
import nl.tue.astar.Head;
import nl.tue.astar.Tail;
import nl.tue.storage.CompressedStore;

import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPTail;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NAryTreeBasisILPTail extends AbstractNAryTreeLPTail implements FastLowerBoundTail {
	public static int LPSolved = 0;
	public static int TailCreated = 0;
	public static int LPDerived = 0;

	private boolean exact = false;
	protected int[] basis;

	public NAryTreeBasisILPTail(NAryTreeBasisILPDelegate delegate, NAryTreeHead h, int minCost) {
		super(minCost < h.getParikhVector().getNumElts() ? h.getParikhVector().getNumElts() : minCost,
				new short[delegate.getNumberVariables()]);
		exact = false;
	}

	public NAryTreeBasisILPTail(int estimate, short[] variables, boolean exact) {
		super(estimate, variables);
		this.exact = exact;
	}

	public NAryTreeBasisILPTail(NAryTreeBasisILPDelegate delegate, NAryTreeHead head) {
		this(delegate, head, 0);
		setBasis(getInitBasis(delegate.columns() + delegate.rows() + 1));
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		InputStream in = store.getStreamForObject(index);
		((NAryTreeBasisILPDelegate) d).getTailDeflater().skipHead(in);
		return ((NAryTreeBasisILPDelegate) d).getTailInflater().inflate(in);
	}

	public NAryTreeBasisILPTail getNewTail(int estimate, short[] variables) {
		LPDerived++;
		return new NAryTreeBasisILPTail(estimate, variables, true);
	}

	public NAryTreeBasisILPTail getNewTail(AbstractNAryTreeDelegate<?> delegate, NAryTreeHead h, int minCost) {
		TailCreated++;
		return new NAryTreeBasisILPTail((NAryTreeBasisILPDelegate) delegate, h, minCost);
	}

	public void computeEstimate(Delegate<? extends Head, ? extends Tail> delegate, Head head, int lastEstimate) {
		if (exact) {
			return;
		}
		LPSolved++;
		NAryTreeHead h = (NAryTreeHead) head;
		NAryTreeBasisILPDelegate d = (NAryTreeBasisILPDelegate) delegate;
		//		super(delegate, h, minCost);
		//System.out.println("LpSolve called: " + calls++ + " times");

		// give the old basis to the estimater
		BasisLPResult res = d.estimate(h.getState(), h.getParikhVector(), lastEstimate, basis);
		for (int i = 0; i < res.getVariables().length; i++) {

			int j;
			if (res.getSolveResult() == LpSolve.OPTIMAL) {
				// result is optimal, use the result
				j = (int) (res.getVariable(i) + 0.5);
				assert j >= 0;
			} else {
				// result is relaxed-optimal, round down.
				j = (int) (res.getVariable(i));
				if (j < 0) {
					j = 0;
				}
			}
			variables[i] = (short) j;
			assert (variables[i] & 0xFFFF) >= 0;
		}
		// copy the new basis in.
		setBasis(res.getBasis());

		int solvedEstimate;
		if (res.getSolveResult() == LpSolve.OPTIMAL) {
			solvedEstimate = (int) (res.getResult() + PRECISION + 0.5);
		} else {
			solvedEstimate = (int) (res.getResult() + PRECISION);
		}
		if (solvedEstimate < lastEstimate) {
			solvedEstimate = lastEstimate;
		}
		this.estimate = solvedEstimate;
		//System.out.println(h + " -> " + this);
		exact = true;

	}

	public boolean isExactEstimateKnown() {
		return exact;
	}

	public int[] getBasis() {
		return basis;
	}

	public void setBasis(int[] basis) {
		assert basis != null;
		this.basis = basis;
	}

	protected static int[] getInitBasis(int l) {
		int[] b = new int[l];
		for (int i = 1; i < b.length; i++) {
			b[i] = -i;
		}
		return b;
	}

	@Override
	public Tail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head head, int modelMove, int logMove,
			int activity) {
		NAryTreeBasisILPDelegate delegate = ((NAryTreeBasisILPDelegate) d);
		NAryTreeHead h = (NAryTreeHead) head;
		//assert modelMove == AStarThread.NOMOVE || movesMade != null;

		NAryTreeBasisILPTail newTail = null;
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

				newTail.setBasis(basis);
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

				newTail.setBasis(basis);
			}

		}

		if (newTail == null) {
			newTail = getNewTail(delegate, (NAryTreeHead) head, estimate - costMade);
			newTail.setBasis(basis);
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

		if (newTail.getEstimate() < 0) {
			// impossible to complete
			return newTail;
		}

		return newTail;
	}

}