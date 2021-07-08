package org.processmining.plugins.boudewijn.treebasedreplay.astar;

import gnu.trove.list.TIntList;
import gnu.trove.list.linked.TIntLinkedList;

public class ModelPrefix {

	protected final TIntList prefix;

	public ModelPrefix() {
		prefix = new TIntLinkedList();
	}

	public ModelPrefix(ModelPrefix prefix) {
		this.prefix = new TIntLinkedList(prefix.prefix);
	}

	public boolean equals(Object o) {
		return (o instanceof ModelPrefix) && (prefix.equals(((ModelPrefix) o).prefix));
	}

	public int hashCode() {
		return prefix.hashCode();
	}

	public String toString() {
		return prefix.toString();
	}

	public void addFirst(int modelMove) {
		if (prefix.isEmpty()) {
			prefix.add(modelMove);
		} else {
			prefix.insert(0, modelMove);
		}
	}

	public void removeLast() {
		prefix.removeAt(prefix.size() - 1);
	}

}
