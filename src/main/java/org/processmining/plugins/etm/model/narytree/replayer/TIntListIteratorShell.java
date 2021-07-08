package org.processmining.plugins.etm.model.narytree.replayer;

import gnu.trove.TIntCollection;
import gnu.trove.function.TIntFunction;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.procedure.TIntProcedure;

import java.util.Collection;
import java.util.Random;

public class TIntListIteratorShell implements TIntList {

	private final AbstractNAryTreeDelegate<?> delegate;
	private final byte[] state;
	private final short activity;
	private final boolean skipLeafsUnderORIfOREnabled;

	public TIntListIteratorShell(AbstractNAryTreeDelegate<?> delegate, byte[] state) {
		this(delegate, state, (short) -1);
	}

	public TIntListIteratorShell(AbstractNAryTreeDelegate<?> delegate, byte[] state, short activity) {
		this(delegate, state, activity, false);
	}

	public TIntListIteratorShell(AbstractNAryTreeDelegate<?> delegate, byte[] state, boolean skipLeafsUnderORIfOREnabled) {
		this(delegate, state, (short) -1, skipLeafsUnderORIfOREnabled);
	}

	public TIntListIteratorShell(AbstractNAryTreeDelegate<?> delegate, byte[] state, short activity,
			boolean skipLeafsUnderORIfOREnabled) {
		this.delegate = delegate;
		this.state = state;
		this.activity = activity;
		this.skipLeafsUnderORIfOREnabled = skipLeafsUnderORIfOREnabled;
	}

	public TIntIterator iterator() {
		if (activity < 0) {
			return delegate.enabledIterator(state, skipLeafsUnderORIfOREnabled);
		} else {
			return delegate.enabledIterator(state, activity);
		}
	}

	public boolean containsAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(TIntCollection collection) {
		throw new UnsupportedOperationException();
	}

	public boolean containsAll(int[] array) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(Collection<? extends Integer> collection) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(TIntCollection collection) {
		throw new UnsupportedOperationException();
	}

	public boolean addAll(int[] array) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(TIntCollection collection) {
		throw new UnsupportedOperationException();
	}

	public boolean retainAll(int[] array) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(Collection<?> collection) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(TIntCollection collection) {
		throw new UnsupportedOperationException();
	}

	public boolean removeAll(int[] array) {
		throw new UnsupportedOperationException();
	}

	public int getNoEntryValue() {
		throw new UnsupportedOperationException();
	}

	public int size() {
		throw new UnsupportedOperationException();
	}

	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	public boolean add(int val) {
		throw new UnsupportedOperationException();
	}

	public void add(int[] vals) {
		throw new UnsupportedOperationException();
	}

	public void add(int[] vals, int offset, int length) {
		throw new UnsupportedOperationException();
	}

	public void insert(int offset, int value) {
		throw new UnsupportedOperationException();
	}

	public void insert(int offset, int[] values) {
		throw new UnsupportedOperationException();
	}

	public void insert(int offset, int[] values, int valOffset, int len) {
		throw new UnsupportedOperationException();
	}

	public int get(int offset) {
		throw new UnsupportedOperationException();
	}

	public int set(int offset, int val) {
		throw new UnsupportedOperationException();
	}

	public void set(int offset, int[] values) {
		throw new UnsupportedOperationException();
	}

	public void set(int offset, int[] values, int valOffset, int length) {
		throw new UnsupportedOperationException();
	}

	public int replace(int offset, int val) {
		throw new UnsupportedOperationException();
	}

	public void clear() {
		throw new UnsupportedOperationException();
	}

	public boolean remove(int value) {
		throw new UnsupportedOperationException();
	}

	public int removeAt(int offset) {
		throw new UnsupportedOperationException();
	}

	public void remove(int offset, int length) {
		throw new UnsupportedOperationException();
	}

	public void transformValues(TIntFunction function) {
		throw new UnsupportedOperationException();
	}

	public void reverse() {
		throw new UnsupportedOperationException();
	}

	public void reverse(int from, int to) {
		throw new UnsupportedOperationException();
	}

	public void shuffle(Random rand) {
		throw new UnsupportedOperationException();
	}

	public TIntList subList(int begin, int end) {
		throw new UnsupportedOperationException();
	}

	public int[] toArray() {
		throw new UnsupportedOperationException();
	}

	public int[] toArray(int offset, int len) {
		throw new UnsupportedOperationException();
	}

	public int[] toArray(int[] dest) {
		throw new UnsupportedOperationException();
	}

	public int[] toArray(int[] dest, int offset, int len) {
		throw new UnsupportedOperationException();
	}

	public int[] toArray(int[] dest, int source_pos, int dest_pos, int len) {
		throw new UnsupportedOperationException();
	}

	public boolean forEach(TIntProcedure procedure) {
		throw new UnsupportedOperationException();
	}

	public boolean forEachDescending(TIntProcedure procedure) {
		throw new UnsupportedOperationException();
	}

	public void sort() {
		throw new UnsupportedOperationException();
	}

	public void sort(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	public void fill(int val) {
		throw new UnsupportedOperationException();
	}

	public void fill(int fromIndex, int toIndex, int val) {
		throw new UnsupportedOperationException();
	}

	public int binarySearch(int value) {
		throw new UnsupportedOperationException();
	}

	public int binarySearch(int value, int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	public int indexOf(int value) {
		throw new UnsupportedOperationException();
	}

	public int indexOf(int offset, int value) {
		throw new UnsupportedOperationException();
	}

	public int lastIndexOf(int value) {
		throw new UnsupportedOperationException();
	}

	public int lastIndexOf(int offset, int value) {
		throw new UnsupportedOperationException();
	}

	public boolean contains(int value) {
		throw new UnsupportedOperationException();
	}

	public TIntList grep(TIntProcedure condition) {
		throw new UnsupportedOperationException();
	}

	public TIntList inverseGrep(TIntProcedure condition) {
		throw new UnsupportedOperationException();
	}

	public int max() {
		throw new UnsupportedOperationException();
	}

	public int min() {
		throw new UnsupportedOperationException();
	}

	public int sum() {
		throw new UnsupportedOperationException();
	}

}
