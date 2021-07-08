package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.framework.plugin.annotations.KeepInProMCache;
import org.processmining.plugins.etm.fitness.FitnessAnnotation;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
@KeepInProMCache
@FitnessAnnotation
//FIXME check all class contents
//FIXME Test Class thoroughly
public class SimplicityMaxTreeSize {//extends TreeFitnessAbstract {
	//DISABLED because this requires a GUI (and I don't have time to make one)
	
	private int maxSize;

/*-
	public static final TreeFitnessInfo info = new TreeFitnessInfo(SimplicityMaxTreeSize.class, "St",
			"Simplicity - maximum tree size",
			"Returns 1 (perfect) as long as the tree is below a predefined size, 0 if on or over the size.",
			Dimension.SIMPLICITY, true);
			/**/

	/**
	 * Initialize this simplicity metrics.
	 * 
	 * @param maxSize
	 *            maximum tree size at which this evaluator will go to a bad
	 *            evaluation of the tree
	 */
	public SimplicityMaxTreeSize(int maxSize) {
		this.maxSize = maxSize;
	}

	public SimplicityMaxTreeSize(SimplicityMaxTreeSize original) {
		this(original.maxSize);
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		return candidate.size() < maxSize ? 1 : 0;
	}

	/*-
	public TreeFitnessInfo getInfo() {
		return info;
	}/**/

	/**
	 * Update the maximum tree size, can be done at run time.
	 * 
	 * @param newMaxSize
	 */
	public void updateMaxSize(int newMaxSize) {
		this.maxSize = newMaxSize;
	}

}
