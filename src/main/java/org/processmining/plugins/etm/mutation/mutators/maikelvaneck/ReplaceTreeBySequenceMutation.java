package org.processmining.plugins.etm.mutation.mutators.maikelvaneck;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * 
 * @author Maikel van Eck
 */
public class ReplaceTreeBySequenceMutation extends TreeMutationAbstract {

	public ReplaceTreeBySequenceMutation(CentralRegistry registry) {
		super(registry);
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree) {
		return mutate(tree,0);
	}
	
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		ProbProcessArrayTree mutatedTree = tree.replace(node, SequenceFactory.generateRandomCandidate(registry), 0);
		if (mutatedTree.equals(tree)) {
			noChange();
		} else {
			didChange(node, TypesOfTreeChange.OTHER);
		}

		return mutatedTree;
	}

}
