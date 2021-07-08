package org.processmining.plugins.etm.fitness;

import java.util.LinkedList;

import gnu.trove.map.TIntObjectMap;
import nl.tue.astar.Trace;

public class RepairInfo {

	private final TIntObjectMap<Trace> subtraces;
	private final TIntObjectMap<LinkedList<Integer>> logMovesCounter;

	public RepairInfo(TIntObjectMap<Trace> subtraces, TIntObjectMap<LinkedList<Integer>> logMovesCounter) {
		this.subtraces = subtraces;
		this.logMovesCounter = logMovesCounter;
	}

	public TIntObjectMap<Trace> getSubtraces() {
		return subtraces;
	}

	public TIntObjectMap<LinkedList<Integer>> getLogMovesCounter() {
		return logMovesCounter;
	}

}
