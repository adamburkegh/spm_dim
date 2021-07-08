
package org.processmining.plugins.etm.factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;

/**
 * {@link org.uncommonseditedbyjoosbuijs.watchmaker.framework.CandidateFactory}
 * for generating trees of {@link ProbProcessArrayTree}s for the genetic programming example
 * application. Options are to randomly generate trees but also some predefined
 * structures are available.
 * 
 * @author jbuijs
 */

public class TreeFactoryCoordinator extends AbstractCandidateFactory<ProbProcessArrayTree> {//implements EvolutionaryOperator<NAryTree> {
	protected CentralRegistry registry;
	private Map<TreeFactoryAbstract, Double> otherFactories;
	private double totalWeights;
	private double randomWeight;

	/**
	 * 
	 * @param registry
	 */
	public TreeFactoryCoordinator(CentralRegistry registry) {
		this(registry, null);
	}

	/**
	 * 
	 * @param registry
	 * @param randomWeight
	 */
	public TreeFactoryCoordinator(CentralRegistry registry, double randomWeight) {
		this(registry, randomWeight, null);
	}

	/**
	 * 
	 * @param registry
	 * @param otherFactories
	 *            A map from other factory instances to the chance of them
	 *            creating a candidate. Please keep in mind that the chance of a
	 *            random candidate creation is now set as 1.0
	 */
	public TreeFactoryCoordinator(CentralRegistry registry, Map<TreeFactoryAbstract, Double> otherFactories) {
		this(registry, 1, otherFactories);
	};

	/**
	 * 
	 * @param registry
	 * @param randomWeight
	 *            Chance of a random candidate
	 * @param otherFactories
	 *            A map from other factory instances to the chance of them
	 *            creating a candidate.
	 */
	public TreeFactoryCoordinator(CentralRegistry registry, double randomWeight,
			Map<TreeFactoryAbstract, Double> otherFactories) {
		if (registry == null) {
			throw new IllegalArgumentException("The central bookkeeper can not be empty");
		}

		totalWeights = randomWeight;
		this.randomWeight = randomWeight;

		this.registry = registry;

		if (otherFactories == null) {
			this.otherFactories = new HashMap<TreeFactoryAbstract, Double>();
		} else {
			this.otherFactories = otherFactories;
		}

		for (Entry<TreeFactoryAbstract, Double> entry : this.otherFactories.entrySet()) {
			totalWeights += entry.getValue();
		}
	}

	/**
	 * Clears all other factories used (i.e. only this one remains)
	 */
	public void clearFactories() {
		otherFactories.clear();
		totalWeights = 0;
	}

	/**
	 * Update the random weight
	 * 
	 * @param randomWeight
	 */
	public void setRandomWeight(double randomWeight) {
		this.randomWeight = randomWeight;
	}

	/**
	 * Generates a random candidate by choosing a factory at random (including
	 * ourselves with weight 1). Uses random of CentralRegistry! not the
	 * provided!!!
	 */
	public ProbProcessArrayTree generateRandomCandidate(Random rng) {
		Double dice = registry.getRandom().nextDouble() * totalWeights;

		ProbProcessArrayTree tree = null;
		if (dice < randomWeight) {
			//We're up!
			tree = generateRandomCandidate(registry);
		} else {
			//Try one of the other factories
			dice -= 1;
			for (Entry<TreeFactoryAbstract, Double> entry : otherFactories.entrySet()) {
				if (dice < entry.getValue()) {
					tree = entry.getKey().generateRandomCandidate(registry.getRandom());
					break;
				} else {
					dice -= entry.getValue();
				}
			}
		}

		if (tree == null) {
			assert false;
			tree = generateRandomCandidate(registry);
		}

		//Now make sure the tree has enough configurations, if required
		if (registry instanceof CentralRegistryConfigurable) {
			CentralRegistryConfigurable cr = (CentralRegistryConfigurable) registry;
			while (tree.getNumberOfConfigurations() < cr.getNrLogs()) {
				tree.addConfiguration(new Configuration(new boolean[tree.size()], new boolean[tree.size()]));
			}
		}

		return tree;

	}

	/**
	 * Static function that randomly selects a method to generate a process
	 * model and returns the result
	 * 
	 * @param registry
	 * @return NAryTree created randomly or according to some pattern.
	 */
	public static ProbProcessArrayTree generateRandomCandidate(CentralRegistry registry) {
		return randomTree(registry);
	}

	/**
	 * Returns a random tree
	 * 
	 * @param registry
	 * @return
	 */
	public static ProbProcessArrayTree randomTree(CentralRegistry registry) {
		//TODO check probability and maximum size, correct guesses?
		//TEST with small trees of size 4 that then grow step by step by our mutators
		return TreeUtils.rewriteRevSeq(TreeUtils.randomTree(registry.nrEventClasses(), .4, 1, 4, registry.getRandom()));
	}
}
