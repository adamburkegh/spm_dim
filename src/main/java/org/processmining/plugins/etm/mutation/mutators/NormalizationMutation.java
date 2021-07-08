package org.processmining.plugins.etm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * This mutation normalizes the process tree.
 * 
 * @author jbuijs
 *
 */
public class NormalizationMutation extends TreeMutationAbstract {

	@SuppressWarnings("unused")
	private String key = "TreeNormalization";

	public NormalizationMutation(CentralRegistry registry) {
		super(registry);
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree) {
		return mutate(tree, 0);
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		ProbProcessArrayTree normalized = TreeUtils.normalize(tree);

		if (normalized.equals(tree)) {
			noChange();
		} else {
			didChange(node, TypesOfTreeChange.OTHER);
		}

		return normalized;
	}

}
