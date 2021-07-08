package org.processmining.plugins.etm.fitness.metrics;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

/**
 * This fitness metric provide a simplicity measure based on the duplication and
 * missing of activities.
 * 
 * @author jbuijs
 * 
 */
//FIXME check all class contents
//FIXME Test Class thoroughly
public class SimplicityDuplMissingAct extends TreeFitnessAbstract {

	public static final TreeFitnessInfo info = new TreeFitnessInfo(SimplicityDuplMissingAct.class, "Sd",
			"Simplicity - Duplicate and missing activities.",
			"The tree should contain every activity at least once to have the perfect size.", Dimension.SIMPLICITY,
			true);

	CentralRegistry registry;

	protected SimplicityDuplMissingAct() {
		registry = null;
	}

	/**
	 * @param logInfo
	 *            Log information to prevent re-calculation for each call
	 */
	public SimplicityDuplMissingAct(CentralRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Deep-clone copy constructor
	 * 
	 * @param original
	 */
	public SimplicityDuplMissingAct(SimplicityDuplMissingAct original) {
		this(original.registry);
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		//We need #duplicates, #missing, #event classes, #leafs (and #tau but we hide those)

		//First, count #duplicate
		int duplication = 0;
		Set<Short> classes = new HashSet<Short>();
		//Go through the leafs and remember the classes we've seen (including null=tau)
		int i = candidate.isLeaf(0) ? 0 : candidate.getNextLeafFast(0);
		do {
			if (classes.contains(candidate.getType(i))) {
				duplication++;
			} else {
				classes.add(candidate.getType(i));
			}
			i = candidate.getNextLeafFast(i);
		} while (i < candidate.size());

		//We actually also punish for a single tau (null in the classes set) so remove it from our list.
		//Furthermore, a tau also disrupts our calculation of #missing events later on so remove it from the class list
		if (classes.contains(ProbProcessArrayTree.TAU)) {
			duplication++; //add one duplication more
			classes.remove(ProbProcessArrayTree.TAU); //remove the tau in question
		}

		//Now also count the number of missing event classes
		int missing = registry.getLogInfo().getEventClasses().size() - classes.size();

		return (double) 1
				- ((duplication + missing) / (registry.getLogInfo().getEventClasses().size() + candidate.numLeafs()));
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}
}
