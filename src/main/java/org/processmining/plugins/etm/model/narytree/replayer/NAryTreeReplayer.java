package org.processmining.plugins.etm.model.narytree.replayer;

import nl.tue.astar.AStarException;
import nl.tue.astar.Tail;
import nl.tue.astar.impl.State;

import org.processmining.plugins.boudewijn.treebasedreplay.TreeDelegate;

public interface NAryTreeReplayer<H extends NAryTreeHead, T extends Tail, D extends TreeDelegate<H, T>> {

	public static enum VerboseLevel {
		NONE, ALL, SOME
	}

	public static enum PerformanceType {
		MEMORYEFFICIENT, CPUEFFICIENT
	}

	public void setType(PerformanceType type);

	/**
	 * Allows the user more control over the type of implementation
	 * 
	 * @param type
	 * @param blockSize
	 *            the size of the blocks of memory. A typical example would be
	 *            1024 times the size of an average state
	 * @param alignment
	 *            the alignment. Each stored object uses a multiple of this many
	 *            bytes. A value of 1 allows for 4GB to be allocated. A value of
	 *            2 for 8GB, etc.
	 */
	public void setType(PerformanceType type, int blockSize, int alignment);

	public int run(final VerboseLevel verbose, final int stopAt, final int minModelCost, final double timeLimit)
			throws AStarException;

	public State<H, T> getState(long index);

	public long getQueuedStateCount();

	public long getTraversedArcCount();

	public D getDelegate();

	public long getNumStoredStates();

	public boolean wasReliable();

	/**
	 * Tell the algorithm to clear the memory (in between trace alignments) if
	 * it is close to using the specified amount of megabytes of memory. When
	 * calling this method make sure you take the number of threads into account
	 * and that you leave some 'general' memory space available. This will not
	 * prevent {@link OutOfMemoryError}s at all times but it will try to, at the
	 * cost of performance for the larger trace/model combinations since it
	 * empties the cache.
	 * 
	 * @param maxMemUsage
	 *            maximum memory usage in bytes (negative value indicates no
	 *            limit)
	 * @return
	 */
	public long setMaxNumberOfBlocksToUse(long maxBytestoUse);

	/**
	 * returns the raw cost of aligning a log and a model after a call to run.
	 * If the call to run was unreliable, the raw cost is a best guess.
	 * 
	 * @return
	 */
	public long getRawCost();

	public boolean isStubborn();

	public void setStubborn(boolean stubborn);

}