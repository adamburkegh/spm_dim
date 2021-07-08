package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import gnu.trove.TShortCollection;
import gnu.trove.iterator.TShortIterator;
import gnu.trove.list.array.TShortArrayList;

public class FastCloneTShortArrayList extends TShortArrayList {

	public FastCloneTShortArrayList(int capacity) {
		super(capacity, (short) -1);
	}

	public FastCloneTShortArrayList(FastCloneTShortArrayList list) {
		_data = new short[list._data.length];
		System.arraycopy(list._data, 0, _data, 0, list._pos);
		_pos = list._pos;
		this.no_entry_value = list.no_entry_value;
	}

	public FastCloneTShortArrayList() {
		super(DEFAULT_CAPACITY, (short) -1);
	}

	public FastCloneTShortArrayList(short[] indices) {
		_data = indices;
		_pos = indices.length;
		this.no_entry_value = -1;
	}

	public FastCloneTShortArrayList(int[] indices) {
		_data = new short[indices.length];
		int i = 0;
		for (int idx : indices) {
			_data[i++] = (short) idx;
		}
		_pos = indices.length;
		this.no_entry_value = -1;
	}

	public boolean addAllIfNew(TShortCollection collection) {
		boolean changed = false;
		TShortIterator iter = collection.iterator();
		while (iter.hasNext()) {
			short element = iter.next();
			if (!contains(element)) {
				changed = add(element);
			}
		}
		return changed;
	}

	/** {@inheritDoc} */
	// Note that this implementation assumes sorted lists
	public boolean equalsSorted(FastCloneTShortArrayList that) {
		if (that._pos != this._pos)
			return false;

		int i = -1;
		while (++i < _pos && _data[i] == that._data[i])
			;
		return i == _pos;
	}

}
