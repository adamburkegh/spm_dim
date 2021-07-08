package org.processmining.plugins.etm.model.narytree.replayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import nl.tue.astar.Tail;
import nl.tue.astar.impl.AbstractCompressor;
import nl.tue.astar.impl.State;
import nl.tue.astar.impl.memefficient.HeadDeflater;
import nl.tue.astar.util.ShortShortMultiset;
import nl.tue.storage.CompressedStore;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.StorageException;
import nl.tue.storage.compressor.BitMask;
import nl.tue.storage.impl.SkippableOutputStream;

public class NAryTreeHeadCompressor<T extends Tail> extends AbstractCompressor<NAryTreeHead> implements
		EqualOperation<State<NAryTreeHead, T>>, HashOperation<State<NAryTreeHead, T>>, HeadDeflater<NAryTreeHead> {

	private final short activities;
	private int bytesState;
	private int bytesParikh;
	private final int maxBytes;

	public NAryTreeHeadCompressor(AbstractNAryTreeDelegate<?> delegate, short activities) {
		this.bytesState = delegate.getStateSizeInBytes();
		this.activities = activities;
		this.bytesParikh = BitMask.getNumBytes(activities);
		// we use double bitmask for the parikh vector
		this.maxBytes = 4 + bytesState + 2 * bytesParikh + activities * 2;

	}

	public void deflate(NAryTreeHead object, OutputStream stream) throws IOException {
		// cache the hashcode for quick lookup
		writeIntToByteArray(stream, object.hashCode());
		// write the state.
		stream.write(object.getState());
		// store the parikh vector
		writeParikhDoubleBitMask(stream, object.getParikhVector());
	}

	public int getMaxByteCount() {
		return maxBytes;
	}

	public NAryTreeHead inflate(InputStream stream) throws IOException {
		// read the hashCode
		int hashCode = readIntFromStream(stream);
		// read state
		byte[] state = new byte[bytesState];
		stream.read(state);
		// read the parikh vector
		ShortShortMultiset parikh = readParikhDoubleBitmask(stream);

		//new ReducedTokenCountMarking(delegate, enabled, future, nodes)
		return new NAryTreeHead(parikh, state, hashCode, null, 0);
	}

	public int getHashCode(State<NAryTreeHead, T> object) {
		return object.getHead().hashCode();
	}

	public int getHashCode(CompressedStore<State<NAryTreeHead, T>> store, long l) throws StorageException {
		try {
			return readIntFromStream(store.getStreamForObject(l));
		} catch (IOException e) {
			throw new StorageException(e);
		}
	}

	public boolean equals(State<NAryTreeHead, T> object, CompressedStore<State<NAryTreeHead, T>> store, long l)
			throws StorageException, IOException {
		return equalsInflating(object, store, l);
	}

	private boolean equalsInflating(State<NAryTreeHead, T> vector, CompressedStore<State<NAryTreeHead, T>> store, long l)
			throws IOException {
		InputStream stream = store.getStreamForObject(l);

		int hashCode = readIntFromStream(stream);
		if (hashCode != vector.getHead().hashCode()) {
			return false;
		}

		byte[] state = vector.getHead().getState();
		byte[] state2 = new byte[bytesState];
		stream.read(state2);

		if (!Arrays.equals(state, state2)) {
			return false;
		}

		// check the parikh vector
		if (checkParikhDoubleBitMask(stream, vector.getHead().getParikhVector())) {
			return true;
		} else {
			return false;
		}
	}

	protected void writeParikhDoubleBitMask(OutputStream stream, ShortShortMultiset set) throws IOException {

		byte[] bitmask1 = new byte[bytesParikh];
		byte[] bitmask2 = new byte[bytesParikh];
		short[] greater = new short[activities];
		//iterating over all elements if possible indices is faster than 
		// first getting the relevant keys.
		int v = 0;
		for (short a = 0; a < activities; a++) {
			short i = set.get(a);
			if (i == 0) {
				continue;
			}
			int bte = a / 8;
			int bit = a % 8;
			if (i == 1) {
				bitmask1[bte] = (byte) (bitmask1[bte] | POWER[bit]);

			} else if (i == 2) {
				bitmask2[bte] = (byte) (bitmask2[bte] | POWER[bit]);

			} else if (i > 2) {
				bitmask1[bte] = (byte) (bitmask1[bte] | POWER[bit]);
				bitmask2[bte] = (byte) (bitmask2[bte] | POWER[bit]);
				greater[v++] = i;
			}
		}
		stream.write(bitmask1);
		stream.write(bitmask2);
		for (int i = 0; i < v; i++) {
			writeShortToByteArray(stream, greater[i]);
		}

	}

	protected boolean checkParikhDoubleBitMask(InputStream stream, ShortShortMultiset set) throws IOException {

		byte[] m1 = new byte[bytesParikh];
		stream.read(m1);
		byte[] m2 = new byte[bytesParikh];
		stream.read(m2);

		short[] greater = new short[activities];
		//iterating over all elements if possible indices is faster than 
		// first getting the relevant keys.
		int v = 0;
		boolean ok = true;
		short a;
		for (a = 0; ok && a < activities; a++) {
			short i = set.get(a);
			int bte = a / 8;
			int bit = a % 8;
			if (i == 1) {
				ok &= (m1[bte] & POWER[bit]) == POWER[bit];
				ok &= (m2[bte] & POWER[bit]) == 0;

			} else if (i == 2) {
				ok &= (m1[bte] & POWER[bit]) == 0;
				ok &= (m2[bte] & POWER[bit]) == POWER[bit];

			} else if (i > 2) {
				ok &= (m1[bte] & POWER[bit]) == POWER[bit];
				ok &= (m2[bte] & POWER[bit]) == POWER[bit];

				greater[v++] = i;
			}
		}

		for (int i = 0; ok && (i < v); i++) {
			ok &= greater[i] == readShortFromStream(stream);
		}

		return ok;
	}

	protected ShortShortMultiset readParikhDoubleBitmask(InputStream stream) throws IOException {
		ShortShortMultiset parikh = new ShortShortMultiset(activities);
		BitMask bitmask1 = readMask(stream, activities, bytesParikh);
		BitMask bitmask2 = readMask(stream, activities, bytesParikh);

		for (int i = 0; i < bitmask1.getBytes().length; i++) {
			byte b1 = bitmask1.getBytes()[i];
			byte b2 = bitmask2.getBytes()[i];
			for (int j = 0; j < 8; j++) {
				int one1 = (b1 & POWER[j]);
				int one2 = (b2 & POWER[j]);
				if (one1 != 0 && one2 == 0) {
					parikh.put((short) (i * 8 + j), (short) 1);
				} else if (one1 == 0 && one2 != 0) {
					parikh.put((short) (i * 8 + j), (short) 2);
				} else if (one1 != 0 && one2 != 0) {
					parikh.put((short) (i * 8 + j), readShortFromStream(stream));
				}
			}
		}

		return parikh;
	}

	public void skip(NAryTreeHead head, SkippableOutputStream out) throws IOException {
		// skip the hashCode
		out.skip(4);
		// skip the state
		out.skip(head.getState().length);
		// skip the parikh vector
		out.skip(2 * bytesParikh);
		for (short a = 0; a < activities; a++) {
			if (head.getParikhVector().get(a) > 2) {
				out.skip(2);
			}
		}
	}

}
