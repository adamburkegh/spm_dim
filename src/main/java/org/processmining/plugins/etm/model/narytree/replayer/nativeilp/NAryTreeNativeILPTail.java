package org.processmining.plugins.etm.model.narytree.replayer.nativeilp;

import java.io.IOException;
import java.io.InputStream;

import lpsolve.LpSolve;
import nl.tue.astar.Delegate;
import nl.tue.astar.FastLowerBoundTail;
import nl.tue.astar.Head;
import nl.tue.astar.Tail;
import nl.tue.astar.util.LPResult;
import nl.tue.storage.CompressedStore;

import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPTail;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;

public class NAryTreeNativeILPTail extends AbstractNAryTreeLPTail implements FastLowerBoundTail {

	public static int LPSolved = 0;
	public static int TailCreated = 0;
	public static int LPDerived = 0;

	private boolean exact = false;
	private BasisHistory basisHistory;

	public NAryTreeNativeILPTail(NAryTreeNativeILPDelegate delegate, NAryTreeHead h, int minCost, BasisHistory basisHistory) {
		super(minCost < h.getParikhVector().getNumElts() ? h.getParikhVector().getNumElts() : minCost,
				new short[delegate.getNumberVariables()]);
		this.exact = false;
		this.basisHistory = basisHistory;
	}

	public NAryTreeNativeILPTail(int estimate, short[] variables, boolean exact, BasisHistory basisHistory) {
		super(estimate, variables);
		this.exact = exact;
		this.basisHistory = basisHistory;
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		InputStream in = store.getStreamForObject(index);
		((NAryTreeNativeILPDelegate) d).getTailDeflater().skipHead(in);
		return ((NAryTreeNativeILPDelegate) d).getTailInflater().inflate(in);
	}

	public AbstractNAryTreeLPTail getNewTail(int estimate, short[] variables) {
		LPDerived++;
		return new NAryTreeNativeILPTail(estimate, variables, true, basisHistory);
	}

	public AbstractNAryTreeLPTail getNewTail(AbstractNAryTreeDelegate<?> delegate, NAryTreeHead h, int minCost) {
		TailCreated++;
		return new NAryTreeNativeILPTail((NAryTreeNativeILPDelegate) delegate, h, minCost, basisHistory);
	}

	public void computeEstimate(Delegate<? extends Head, ? extends Tail> delegate, Head head, int lastEstimate) {
		if (exact) {
			return;
		}
		LPSolved++;
		NAryTreeHead h = (NAryTreeHead) head;
		NAryTreeNativeILPDelegate d = (NAryTreeNativeILPDelegate) delegate;
		//		super(delegate, h, minCost);
		//System.out.println("LpSolve called: " + calls++ + " times");

		LPResult res = d.estimate(h.getState(), h.getParikhVector(), lastEstimate, basisHistory);
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
		//this.basis = d.getBasis();
		//System.out.println(h + " -> " + this);
		exact = true;

	}

	public boolean isExactEstimateKnown() {
		return exact;
	}

	public BasisHistory getBasisHistory() {
		return basisHistory;
	}
}