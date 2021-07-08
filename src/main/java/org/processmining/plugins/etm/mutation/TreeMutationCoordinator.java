package org.processmining.plugins.etm.mutation;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

/**
 * Mutation operator for the trees of {@link Node}s.
 * 
 * @author jbuijs
 */
public class TreeMutationCoordinator implements EvolutionaryOperator<ProbProcessArrayTree> {
	private LinkedHashMap<TreeMutationAbstract, Double> mutators;
	private double totalChance = 0;
	private boolean preventDuplicates;
	protected int locationOfLastChange;
	protected TypesOfTreeChange typeOfChange;

	/**
	 * The tree mutation operator requires a map of mutators, with weights
	 * assigned to them, to select which one to apply.
	 * 
	 * @param mutators
	 */
	public TreeMutationCoordinator(LinkedHashMap<TreeMutationAbstract, Double> mutators, boolean preventDuplicates) {
		this.mutators = mutators;
		this.preventDuplicates = preventDuplicates;

		calculateTotalChance();
	}

	/**
	 * Applies mutation functions to the tree, depending on the tree's fitness
	 * characteristics and the provided probabilities
	 */
	public List<ProbProcessArrayTree> apply(List<ProbProcessArrayTree> selectedCandidates, Random rng) {
		List<ProbProcessArrayTree> mutatedPopulation = new ArrayList<ProbProcessArrayTree>(selectedCandidates.size());

		for (ProbProcessArrayTree tree : selectedCandidates) {
			assert tree.isConsistent();

			ProbProcessArrayTree mutatedTree = apply(tree, rng);

			/*
			 * If we don't allow duplicates (e.g. first part is not true) then
			 * we continue applying until we find a tree that is not already in
			 * the mutated population
			 */
			while (preventDuplicates && mutatedPopulation.contains(mutatedTree)) {
				mutatedTree = apply(tree, rng);
			}

			assert mutatedTree.isConsistent();

			//And add the mutated tree
			mutatedPopulation.add(mutatedTree);
		}

		return mutatedPopulation;
	}

	/**
	 * Applies mutation functions to the tree, depending on the tree's fitness
	 * characteristics and the provided probabilities
	 */
	public ProbProcessArrayTree apply(ProbProcessArrayTree tree, Random rng) {
		ProbProcessArrayTree mutatedTree;
		TreeMutationAbstract mutator;

		int nrTries = TreeMutationAbstract.MAXTRIES;
		do {
			//Get a mutator
			mutator = getMutatorForChance(rng.nextDouble() * totalChance);
			//Get a mutated tree
			mutatedTree = mutator.mutate(tree);

			assert mutatedTree.isConsistent();
			//Keep trying until one of them actually mutates...
			nrTries--;
		} while ((!mutator.changedAtLastCall()) && nrTries > 0);
		// We have to save the location of the last change
		this.locationOfLastChange = mutator.locationOfLastChange;
		this.typeOfChange = mutator.typeOfChange;
		return mutatedTree;
	}

	public LinkedHashMap<TreeMutationAbstract, Double> getMutators() {
		return mutators;
	}

	private void calculateTotalChance() {
		totalChance = 0;
		for (Double weight : mutators.values()) {
			totalChance += weight;
		}
	}

	private TreeMutationAbstract getMutatorForChance(double chance) {
		if (mutators.size() == 1)
			return mutators.keySet().iterator().next();

		double chanceSoFar = 0;
		for (Map.Entry<TreeMutationAbstract, Double> entry : mutators.entrySet()) {
			chanceSoFar += entry.getValue();
			if (chance <= chanceSoFar) {
				return entry.getKey();
			}
		}
		return null;
	}

	public void addMutator(TreeMutationAbstract mutator, Double chance) {
		mutators.put(mutator, chance);
		calculateTotalChance();
	}

	public boolean isPreventDuplicates() {
		return preventDuplicates;
	}

	public void setPreventDuplicates(boolean preventDuplicates) {
		this.preventDuplicates = preventDuplicates;
	}
}
