package org.processmining.plugins.etm.model.ppt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;

import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.CompressedStore;
import nl.tue.storage.Deflater;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.Inflater;
import nl.tue.storage.StorageException;
import nl.tue.storage.impl.CompressedStoreHashSetImpl;
import nl.tue.storage.impl.CompressedStoreHashSetImpl.Result;

public class StateSpace {
	private final CompressedHashSet<byte[]> stateSet;
	private byte[][] edges;
	private int blocks = -1;
	private int writeIndex = 0;

	private int edgeCount;
	private final int alignment;
	private static final int BLOCKSIZE = 8 * 1024 * 1024;

	public static class StateCompressor implements Deflater<byte[]>, Inflater<byte[]>, EqualOperation<byte[]>,
			HashOperation<byte[]> {

		private final int alignment;

		public StateCompressor(int alignment) {
			this.alignment = alignment;
		}

		public void deflate(byte[] state, OutputStream stream) throws IOException {
			stream.write(state);
		}

		public int getMaxByteCount() {
			return alignment;
		}

		public byte[] inflate(InputStream stream) throws IOException {
			byte[] state = new byte[alignment];
			stream.read(state);
			return state;
		}

		public boolean equals(byte[] object, CompressedStore<byte[]> store, long l) throws StorageException,
				IOException {
			return Arrays.equals(object, store.getObject(l));
		}

		public int getHashCode(byte[] state) {
			return Arrays.hashCode(state);
		}

		public int getHashCode(CompressedStore<byte[]> store, long l) throws StorageException {
			return Arrays.hashCode(store.getObject(l));
		}

	}

	public static final class Edge {
		private int[] edge = new int[3];

		public Edge() {
		}

		private void set(int index, int val) {
			this.edge[index] = val;
		}

		public int getFrom() {
			return edge[0];
		}

		public int getTo() {
			return edge[1];
		}

		public int getLabel() {
			return edge[2];
		}
	}

	public StateSpace(int alignment) {
		this.alignment = alignment;
		StateCompressor comp = new StateCompressor(alignment);

		this.stateSet = new CompressedStoreHashSetImpl.IntCustomAlignment<byte[]>(alignment, comp, comp, comp, comp,
				1024 * alignment);
		this.blocks = 0;
		this.edges = new byte[8][];
		this.edges[0] = new byte[BLOCKSIZE];
		edgeCount = 0;
	}

	public int size() {
		return stateSet.size();
	}

	public byte[] getState(int index) {
		try {
			return stateSet.getObject(alignment * index);
		} catch (StorageException e) {
			assert false;
			return null;
		}
	}

	public int indexOf(byte[] state) {
		try {
			return (int) (stateSet.contains(state) / alignment);
		} catch (StorageException e) {
			assert false;
			return -1;
		}
	}

	public synchronized void addEdge(int from, int to, int label) {
		// store the edge by using the header byte as follows:
		// bit 0: unused for now
		// bit 1: used to signal that there is a header (neede for iteration).
		// bits 2,3: store the number of bytes needed for from
		// bits 4,5: store the number of bytes needed for to
		// bits 6,7: store the number of byted needed for label;

		// max 2 bytes per label
		assert label < 65536;

		ensureCapacity(11);

		// reserve space for the header at the highest index
		int index = writeIndex++;
		// add the values with minimal byte use
		int val = 64;
		val |= addIntUnSigned(from) << 4;
		val |= addIntUnSigned(to) << 2;
		val |= addIntUnSigned(label);
		// change the header

		edges[blocks][index] = (byte) val;
		edgeCount++;

	}

	private void ensureCapacity(int s) {
		if ((writeIndex + s) >= edges[blocks].length) {
			if (blocks == edges.length - 1) {
				if (blocks > Integer.MAX_VALUE / 2) {
					throw new RuntimeException("Storage Full");
				}
				// double the storage.
				byte[][] oldStore = edges;
				edges = new byte[blocks + 8][];
				System.arraycopy(oldStore, 0, edges, 0, blocks);

			}
			// we need to add a block
			blocks++;
			edges[blocks] = new byte[BLOCKSIZE];
			// make sure we write the whole stream into one block
			writeIndex = 0;
		}

	}

	private int getEdge(int block, int index, Edge edge) {
		// first read the header
		int header = edges[block][index++];
		index = readIntUnSigned(block, index, (header >> 4) & 3, edge, 0);
		index = readIntUnSigned(block, index, (header >> 2) & 3, edge, 1);
		index = readIntUnSigned(block, index, header & 3, edge, 2);
		return index;
	}

	private int readIntUnSigned(int block, int index, int bits, Edge edge, int ei) {
		int val = edges[block][index++] & 0xff;
		while (bits-- > 0) {
			val = (val << 8) | (edges[block][index++] & 0xff);
		}
		edge.set(ei, val);

		return index;
	}

	private int addIntUnSigned(int i) {
		if (i < 256) {
			edges[blocks][writeIndex++] = ((byte) i);
			return 0;
		} else if (i < 65536) {
			edges[blocks][writeIndex++] = ((byte) (i >> 8));
			edges[blocks][writeIndex++] = ((byte) i);
			return 1;
		} else if (i < 16777216) {
			edges[blocks][writeIndex++] = ((byte) (i >> 16));
			edges[blocks][writeIndex++] = ((byte) (i >> 8));
			edges[blocks][writeIndex++] = ((byte) (i));
			return 2;
		} else {
			edges[blocks][writeIndex++] = ((byte) (i >> 24));
			edges[blocks][writeIndex++] = ((byte) (i >> 16));
			edges[blocks][writeIndex++] = ((byte) (i >> 16));
			edges[blocks][writeIndex++] = ((byte) (i));
			return 3;
		}
	}

	public synchronized Result<byte[]> addNode(byte[] state) {
		try {
			Result<byte[]> result = stateSet.add(state);
			assert (result.index % alignment) == 0;
			result.index /= alignment;
			return result;
		} catch (StorageException e) {
			assert false;
			e.printStackTrace();
			return null;
		}
	}

	public Iterator<byte[]> getNodeIterator() {
		return new Iterator<byte[]>() {

			private int i = 0;

			public boolean hasNext() {
				return i < stateSet.size();
			}

			public byte[] next() {
				try {
					return stateSet.getObject(i++);
				} catch (StorageException e) {
					assert false;
					e.printStackTrace();
					return null;
				}
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public Iterator<Edge> getEdgeIterator() {
		return new Iterator<Edge>() {

			private int i = 0;
			private int b = 0;

			public boolean hasNext() {
				return b <= blocks && edges[b] != null && i < edges[b].length;
			}

			public Edge next() {
				assert hasNext();
				Edge edge = new Edge();
				i = getEdge(b, i, edge);
				if (i >= edges[b].length || ((edges[b][i] & 64) != 64)) {
					// go to the next block.
					i = 0;
					b++;
				}
				return edge;
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public int numEdges() {
		return edgeCount;
	}

	public long memory() {
		return alignment * stateSet.size() + blocks * (8 + BLOCKSIZE);
	}

	public double getBytesPerState() {
		return alignment;
	}

	public double getBytesPerEdge() {
		return (blocks * (8 + BLOCKSIZE)) * 1.0 / edgeCount;
	}
}
