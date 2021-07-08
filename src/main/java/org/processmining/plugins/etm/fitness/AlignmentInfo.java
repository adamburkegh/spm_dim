package org.processmining.plugins.etm.fitness;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.collections15.map.LRUMap;
import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

/**
 * Quick class to store alignments in which can be necessary for some
 * experiments etc.
 * 
 * @author jbuijs
 * 
 */
public class AlignmentInfo {
	private Map<ProbProcessArrayTree, TreeRecord> alignmentCache = null;
	private AStarAlgorithm algorithm;

	public AlignmentInfo(Map<ProbProcessArrayTree, TreeRecord> alignmentCache, AStarAlgorithm algorithm) {
		this.algorithm = algorithm;
		this.alignmentCache = alignmentCache;
	}

	public synchronized void put(ProbProcessArrayTree candidate, TreeRecord lastRecord) {
		alignmentCache.put(candidate, lastRecord);
	}

	public AlignmentInfo(int maxSize) {
		alignmentCache = Collections.synchronizedMap(new LRUMap<ProbProcessArrayTree, TreeRecord>(maxSize));
	}

	/**
	 * @return the alignmentCache
	 */
	public Map<ProbProcessArrayTree, TreeRecord> getAlignmentCache() {
		return alignmentCache;
	}

	/**
	 * @return the algorithm
	 */
	public AStarAlgorithm getAlgorithm() {
		return algorithm;
	}

	public void setAlgorithm(AStarAlgorithm algorithm) {
		this.algorithm = algorithm;
	}
}