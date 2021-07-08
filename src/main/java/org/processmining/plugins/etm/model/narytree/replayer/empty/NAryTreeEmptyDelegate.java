package org.processmining.plugins.etm.model.narytree.replayer.empty;

import nl.tue.astar.Trace;
import nl.tue.astar.impl.memefficient.TailInflater;
import nl.tue.storage.Deflater;

import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NAryTreeEmptyDelegate extends AbstractNAryTreeDelegate<NAryTreeEmptyTail> {

	public NAryTreeEmptyDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2Cost,
			int threads) {
		super(algorithm, tree, configurationNumber, node2Cost, threads);

	}

	public TailInflater<NAryTreeEmptyTail> getTailInflater() {
		return NAryTreeEmptyTail.EMPTY;
	}

	public Deflater<NAryTreeEmptyTail> getTailDeflater() {
		return NAryTreeEmptyTail.EMPTY;
	}

	public NAryTreeEmptyTail createInitialTail(NAryTreeHead head) {
		return NAryTreeEmptyTail.EMPTY;
	}

	public TreeRecord createInitialRecord(NAryTreeHead head, Trace trace) {
		return new TreeRecord(head.getMovesMadeCost(), head.getMovesMade(), head.getMovesMadeCost(), trace.getSize(),
				true);
	}
}
