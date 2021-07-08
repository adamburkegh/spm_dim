package org.processmining.plugins.boudewijn.treebasedreplay.astar;

import gnu.trove.TIntCollection;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

public class TreeMarkingVisit<V> {

	private final TIntSet marking;
	private final V visit;

	public TreeMarkingVisit(TIntCollection marking, V visit) {
		this.marking = new TIntHashSet(marking);
		this.visit = visit;
	}

	public TIntSet getMarking() {
		return marking;
	}

	public V getVisit() {
		return visit;
	}

	public boolean equals(Object o) {
		return (o instanceof TreeMarkingVisit) && ((TreeMarkingVisit<?>) o).visit.equals(visit);
	}

	public int hashCode() {
		return visit.hashCode();
	}

	public String toString() {
		return "m: " + marking.toString() + " v: " + visit;
	}
}
