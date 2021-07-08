package org.processmining.plugins.etm.model.narytree.replayer.fullilp;

import java.io.IOException;
import java.io.InputStream;

import nl.tue.astar.Delegate;
import nl.tue.astar.Head;
import nl.tue.astar.Tail;
import nl.tue.storage.CompressedStore;

import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPTail;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;

public class NAryTreeFullILPTail extends AbstractNAryTreeLPTail {

	public static int LPSolved = 0;
	public static int LPDerived = 0;

	public NAryTreeFullILPTail(NAryTreeFullILPDelegate delegate, NAryTreeHead h, int minCost) {
		super(delegate, h, minCost);
	}

	public NAryTreeFullILPTail(int estimate, short[] variables) {
		super(estimate, variables);
	}

	public NAryTreeFullILPTail(NAryTreeFullILPDelegate delegate, NAryTreeHead head) {
		this(delegate, head, 0);
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		InputStream in = store.getStreamForObject(index);
		((NAryTreeFullILPDelegate) d).getTailDeflater().skipHead(in);
		return ((NAryTreeFullILPDelegate) d).getTailInflater().inflate(in);
	}

	public AbstractNAryTreeLPTail getNewTail(int estimate, short[] variables) {
		LPDerived++;
		return new NAryTreeFullILPTail(estimate, variables);
	}

	public AbstractNAryTreeLPTail getNewTail(AbstractNAryTreeDelegate<?> delegate, NAryTreeHead h, int minCost) {
		LPSolved++;
		return new NAryTreeFullILPTail((NAryTreeFullILPDelegate) delegate, h, minCost);
	}

}