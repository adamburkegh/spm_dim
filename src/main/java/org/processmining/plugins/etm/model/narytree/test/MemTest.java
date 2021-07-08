package org.processmining.plugins.etm.model.narytree.test;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import nl.tue.astar.impl.AbstractCompressor;
import nl.tue.storage.CompressedStore;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.StorageException;
import nl.tue.storage.impl.CompressedStoreHashSetImpl;
import nl.tue.storage.impl.CompressedStoreHashSetImpl.Int32G;

public class MemTest {

	public static final int SIZE = 24;

	static class MyObject {
		private byte[] random;
		private int hashCode;

		public MyObject(Random r) {
			random = new byte[SIZE];
			r.nextBytes(random);
			hashCode = Arrays.hashCode(random);
		}

		public MyObject(byte[] random, int hashCode) {
			this.random = random;
			this.hashCode = hashCode;
		}

		public int hashCode() {
			return hashCode;
		}

		public boolean equals(Object o) {
			return (o instanceof MyObject ? Arrays.equals(random, ((MyObject) o).random) : false);
		}

		public int size() {
			return random.length;
		}

		public byte get(int i) {
			return random[i];
		}

		public byte[] array() {
			return random;
		}
	}

	static class Compressor extends AbstractCompressor<MyObject> implements EqualOperation<MyObject>,
			HashOperation<MyObject> {

		public int getHashCode(MyObject object) {
			return object.hashCode();
		}

		public int getHashCode(CompressedStore<MyObject> store, long l) throws StorageException {
			try {
				return readIntFromStream(store.getStreamForObject(l));
			} catch (IOException e) {
				throw new StorageException(e);
			}
		}

		public boolean equals(MyObject object, CompressedStore<MyObject> store, long l) throws StorageException,
				IOException {
			return equalsInflating(object, store, l);
		}

		public void deflate(MyObject object, OutputStream stream) throws IOException {
			writeIntToByteArray(stream, object.hashCode());
			stream.write(object.array());
		}

		public int getMaxByteCount() {
			return SIZE + 4;
		}

		public MyObject inflate(InputStream stream) throws IOException {
			int hashCode = readIntFromStream(stream);
			byte[] stored = new byte[SIZE];
			stream.read(stored);
			return new MyObject(stored, hashCode);
		}

		private boolean equalsInflating(MyObject vector, CompressedStore<MyObject> store, long l) throws IOException {
			InputStream stream = store.getStreamForObject(l);

			int hashCode = readIntFromStream(stream);
			if (hashCode != vector.hashCode()) {
				return false;
			}

			for (int i = 0; i < vector.size(); i++) {
				if (vector.get(i) != (byte) (stream.read() & 0xff)) {
					return false;
				}
			}
			return true;
		}

	}

	/**
	 * @param args
	 * @throws StorageException
	 */
	public static void main(String[] args) throws StorageException {
		Random r = new Random(12l);
		long start = System.currentTimeMillis();
		//testMemEfficient(r, 1 << 24);
		testCPUEfficient(r, 1 << 24);
		long end = System.currentTimeMillis();
		System.out.println("time: " + (end - start) / 1000.);
	}

	public static void testMemEfficient(Random r, int max) throws StorageException {
		Compressor compressor = new Compressor();
		Int32G<MyObject> statespace = new CompressedStoreHashSetImpl.Int32G<MyObject>(compressor, compressor,
				2 * 1024 * 1024, compressor, compressor, 2 * 1024 * 1024);

		for (int i = 0; i < max; i++) {
			MyObject o = new MyObject(r);
			statespace.add(o);
		}
		System.out.println("wasted: " + statespace.getBackingStore().getWastedMemory());
		System.out.println("unused: " + statespace.getBackingStore().getUnusedMemory());
	}

	public static void testCPUEfficient(Random r, int max) throws StorageException {

		List<MyObject> list = new ArrayList<MyObject>(2 * 1024 * 1024);
		TObjectIntMap<MyObject> map = new TObjectIntHashMap<MyObject>(2 * 1024 * 1024);

		for (int i = 0; i < max; i++) {
			MyObject o = new MyObject(r);
			int newIndex = list.size() + 1;
			list.add(o);
			map.put(o, newIndex);
		}

	}
}
