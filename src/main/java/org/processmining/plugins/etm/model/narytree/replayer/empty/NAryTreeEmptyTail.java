package org.processmining.plugins.etm.model.narytree.replayer.empty;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.astar.Delegate;
import nl.tue.astar.Head;
import nl.tue.astar.Tail;
import nl.tue.astar.impl.memefficient.StorageAwareDelegate;
import nl.tue.astar.impl.memefficient.TailInflater;
import nl.tue.storage.CompressedStore;
import nl.tue.storage.Deflater;

public class NAryTreeEmptyTail implements Tail, TailInflater<NAryTreeEmptyTail>, Deflater<NAryTreeEmptyTail> {

	public static final NAryTreeEmptyTail EMPTY = new NAryTreeEmptyTail();
	private static final String EMPTYSTRING = "";

	private NAryTreeEmptyTail() {

	}

	public Tail getNextTail(Delegate<? extends Head, ? extends Tail> d, Head oldHead, int modelMove, int logMove,
			int activity) {
		return EMPTY;
	}

	public <S> Tail getNextTailFromStorage(Delegate<? extends Head, ? extends Tail> d, CompressedStore<S> store,
			long index, int modelMove, int logMove, int activity) throws IOException {
		return EMPTY;
	}

	public int getEstimatedCosts(Delegate<? extends Head, ? extends Tail> d, Head h) {
		//		NAryTreeHead head = (NAryTreeHead) h;
		return 0;//head.getParikhVector().size();
	}

	public boolean canComplete() {
		return true;
	}

	public void deflate(NAryTreeEmptyTail object, OutputStream stream) throws IOException {
	}

	public NAryTreeEmptyTail inflate(InputStream stream) throws IOException {
		return EMPTY;
	}

	public int getMaxByteCount() {
		return 1;
	}

	public String toString() {
		return EMPTYSTRING;
	}

	public <H extends Head> int inflateEstimate(StorageAwareDelegate<H, NAryTreeEmptyTail> delegate, H head,
			InputStream stream) throws IOException {
		return getEstimatedCosts(delegate, head);
	}

}
