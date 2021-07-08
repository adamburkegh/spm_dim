package org.processmining.plugins.etm.mutation.mutators.maikelvaneck;

import java.util.Random;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;

/**
 * Creates a tree of a single leaf
 * 
 * @author Maikel van Eck
 */
public class LeafFactory extends AbstractCandidateFactory<ProbProcessArrayTree> {
	private CentralRegistry registry;

	public LeafFactory(CentralRegistry registry) {
		if (registry == null) {
			throw new IllegalArgumentException("The central bookkeeper can not be empty");
		}

		this.registry = registry;
	}

	public ProbProcessArrayTree generateRandomCandidate(Random rng) {
		return generateRandomCandidate(registry);
	}

	public static ProbProcessArrayTree generateRandomCandidate(CentralRegistry registry) {
		ProbProcessArrayTree tree;

		tree = TreeUtils.randomTree(registry.nrEventClasses(), 1, 1, 1, registry.getRandom());

		//Now make sure the tree has enough configurations, if required
		if (registry instanceof CentralRegistryConfigurable) {
			CentralRegistryConfigurable cr = (CentralRegistryConfigurable) registry;
			while (tree.getNumberOfConfigurations() < cr.getNrLogs()) {
				tree.addConfiguration(new Configuration(new boolean[tree.size()], new boolean[tree.size()]));
			}
		}

		return tree;
	}
}
