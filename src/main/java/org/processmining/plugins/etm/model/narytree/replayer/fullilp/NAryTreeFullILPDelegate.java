package org.processmining.plugins.etm.model.narytree.replayer.fullilp;

import nl.tue.astar.Trace;

import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NAryTreeFullILPDelegate extends AbstractNAryTreeLPDelegate<NAryTreeFullILPTail> {

	private final NAryTreeFullILPTailCompressor tailCompressor;

	public NAryTreeFullILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads, boolean useOrRows) {
		super(algorithm, tree, configurationNumber, node2cost, threads, true, false, 1, 255, useOrRows);
		this.tailCompressor = new NAryTreeFullILPTailCompressor(getStateSizeInBytes(), columns, (short) algorithm
				.getClasses().size());
	}

	public NAryTreeFullILPTailCompressor getTailInflater() {
		return tailCompressor;
	}

	public NAryTreeFullILPTailCompressor getTailDeflater() {
		return tailCompressor;
	}

	public NAryTreeFullILPTail createInitialTail(NAryTreeHead head) {
		return new NAryTreeFullILPTail(this, head);
	}

	public TreeRecord createInitialRecord(NAryTreeHead head, Trace trace) {
		return new TreeRecord(head.getMovesMadeCost(), head.getMovesMade(), head.getMovesMadeCost(), trace.getSize(),
				true);
	}

}
