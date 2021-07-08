package org.processmining.plugins.etm.model.narytree.replayer.hybridilp;

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
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPTail;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;

public class NAryTreeHybridILPTail extends AbstractNAryTreeLPTail implements FastLowerBoundTail {

	public static int LPSolved = 0;
	public static int TailCreated = 0;
	public static int LPDerived = 0;

	private boolean exact = false;

	public NAryTreeHybridILPTail(NAryTreeHybridILPDelegate delegate, NAryTreeHead h, int minCost) {
		super(minCost < h.getParikhVector().getNumElts() ? h.getParikhVector().getNumElts() : minCost,
				new short[delegate.getNumberVariables()]);
		exact = false;
	}

	public NAryTreeHybridILPTail(int estimate, short[] variables, boolean exact) {
		super(estimate, variables);
		this.exact = exact;
	}

	public NAryTreeHybridILPTail(NAryTreeHybridILPDelegate delegate, NAryTreeHead head) {
		this(delegate, head, 0);
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		InputStream in = store.getStreamForObject(index);
		((NAryTreeHybridILPDelegate) d).getTailDeflater().skipHead(in);
		return ((NAryTreeHybridILPDelegate) d).getTailInflater().inflate(in);
	}

	public AbstractNAryTreeLPTail getNewTail(int estimate, short[] variables) {
		LPDerived++;
		return new NAryTreeHybridILPTail(estimate, variables, true);
	}

	public AbstractNAryTreeLPTail getNewTail(AbstractNAryTreeDelegate<?> delegate, NAryTreeHead h, int minCost) {
		TailCreated++;
		return new NAryTreeHybridILPTail((NAryTreeHybridILPDelegate) delegate, h, minCost);
	}

	public void computeEstimate(Delegate<? extends Head, ? extends Tail> delegate, Head head, int lastEstimate) {
		if (exact) {
			return;
		}
		LPSolved++;
		NAryTreeHead h = (NAryTreeHead) head;
		AbstractNAryTreeLPDelegate<?> d = (AbstractNAryTreeLPDelegate<?>) delegate;
		//		super(delegate, h, minCost);
		//System.out.println("LpSolve called: " + calls++ + " times");

		LPResult res = d.estimate(h.getState(), h.getParikhVector(), lastEstimate);
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
		//System.out.println(h + " -> " + this);
		exact = true;

	}

	public boolean isExactEstimateKnown() {
		return exact;
	}

}