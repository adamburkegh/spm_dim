package org.processmining.plugins.etm.model.narytree.replayer.hybridilp;

import nl.tue.astar.Trace;
import nl.tue.astar.impl.memefficient.CachedStorageAwareDelegate;
import nl.tue.astar.impl.memefficient.HeadDeflater;

import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NAryTreeHybridILPDelegate extends AbstractNAryTreeLPDelegate<NAryTreeHybridILPTail> implements
		CachedStorageAwareDelegate<NAryTreeHead, NAryTreeHybridILPTail> {

	private final NAryTreeHybridILPTailCompressor tailCompressor;

	public NAryTreeHybridILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads) {
		this(algorithm, tree, configurationNumber, node2cost, threads, false);
	}

	public NAryTreeHybridILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber, int[] node2cost,
			int threads, boolean useOrRows) {
		this(algorithm, tree, configurationNumber, node2cost, threads, false, true);
	}

	protected NAryTreeHybridILPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber,
			int[] node2cost, int threads, boolean useOrRows, boolean useInt) {
		super(algorithm, tree, configurationNumber, node2cost, threads, useInt, false, 1, algorithm
				.getLengthLongestTrace() + 1, useOrRows);
		this.tailCompressor = new NAryTreeHybridILPTailCompressor(getStateSizeInBytes(), columns, (short) algorithm
				.getClasses().size(), algorithm.getLengthLongestTrace() + 1);
	}

	public NAryTreeHybridILPTailCompressor getTailInflater() {
		return tailCompressor;
	}

	public NAryTreeHybridILPTailCompressor getTailDeflater() {
		return tailCompressor;
	}

	public NAryTreeHybridILPTail createInitialTail(NAryTreeHead head) {
		return new NAryTreeHybridILPTail(this, head);
	}

	public HeadDeflater<NAryTreeHead> getHeadDeflater() {
		return headCompressor;
	}

	public TreeRecord createInitialRecord(NAryTreeHead head, Trace trace) {
		return new TreeRecord(head.getMovesMadeCost(), head.getMovesMade(), head.getMovesMadeCost(), trace.getSize(),
				false);
	}

}
