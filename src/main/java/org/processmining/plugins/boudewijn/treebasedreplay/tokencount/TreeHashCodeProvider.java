package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.iterator.TShortIterator;
import nl.tue.astar.util.ShortShortMultiset;
import nl.tue.storage.hashing.impl.MurMur3HashCodeProvider;

public class TreeHashCodeProvider extends MurMur3HashCodeProvider {

	public int hash(TShortIterator it, short len, ShortShortMultiset parikh) {
		int hash = INIT;
		hash = hashInternal(it, len, hash);
		hash = hashInternal(parikh.getInternalValues(), hash);
		return hash;

	}

	public int hash(byte[] state, ShortShortMultiset parikh) {
		int hash = INIT;
		hash = hashInternal(state, hash);
		hash = hashInternal(parikh.getInternalValues(), hash);
		return hash;

	}

	/** Returns the MurmurHash3_x86_32 hash. */
	protected int hashInternal(final TShortIterator it, short len, int hash) {

		int k1;
		for (int i = 0; i < len - 1; i += 2) {
			k1 = (it.next() & 0xffff) | ((it.next() & 0xffff) << 16);
			k1 *= C1;
			k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
			k1 *= C2;
			hash ^= k1;
			hash = (hash << 13) | (hash >>> 19); // ROTL32(h1,13);
			hash = hash * 5 + 0xe6546b64;

		}

		// tail
		if ((len & 1) == 1) {
			k1 = (it.next() & 0xffff);
			k1 *= C1;
			k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
			k1 *= C2;
			hash ^= k1;
		}

		// finalization
		hash ^= len;

		// fmix(h1);
		hash ^= hash >>> 16;
		hash *= 0x85ebca6b;
		hash ^= hash >>> 13;
		hash *= 0xc2b2ae35;
		hash ^= hash >>> 16;

		return hash;
	}

	/** Returns the MurmurHash3_x86_32 hash. */
	protected int hashInternal(final byte[] data, int hash) {
		final int c1 = 0xcc9e2d51;
		final int c2 = 0x1b873593;

		int roundedEnd = 0 + (data.length & 0xfffffffc); // round down to 4 byte block

		for (int i = 0; i < roundedEnd; i += 4) {
			// little endian load order
			int k1 = (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8) | ((data[i + 2] & 0xff) << 16)
					| (data[i + 3] << 24);
			k1 *= c1;
			k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
			k1 *= c2;

			hash ^= k1;
			hash = (hash << 13) | (hash >>> 19); // ROTL32(h1,13);
			hash = hash * 5 + 0xe6546b64;
		}

		// tail
		int k1 = 0;

		switch (data.length & 0x03) {
			case 3 :
				k1 = (data[roundedEnd + 2] & 0xff) << 16;
				//$FALL-THROUGH$
			case 2 :
				k1 |= (data[roundedEnd + 1] & 0xff) << 8;
				//$FALL-THROUGH$
			case 1 :
				k1 |= (data[roundedEnd] & 0xff);
				k1 *= c1;
				k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
				k1 *= c2;
				hash ^= k1;
		}

		// finalization
		hash ^= data.length;

		// fmix(h1);
		hash ^= hash >>> 16;
		hash *= 0x85ebca6b;
		hash ^= hash >>> 13;
		hash *= 0xc2b2ae35;
		hash ^= hash >>> 16;

		return hash;
	}

}
