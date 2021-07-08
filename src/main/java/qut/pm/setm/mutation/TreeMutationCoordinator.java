package qut.pm.setm.mutation;



import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeCheck;
import qut.pm.spm.ppt.ProcessTreeConsistencyException;

/**
 * Mutation operator for the trees of {@link Node}s.
 * 
 */
public class TreeMutationCoordinator implements EvolutionaryOperator<ProbProcessTree> {
	private static Logger LOGGER = LogManager.getLogger();
	
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
	public List<ProbProcessTree> apply(List<ProbProcessTree> selectedCandidates, Random rng) {
		List<ProbProcessTree> mutatedPopulation = new ArrayList<ProbProcessTree>(selectedCandidates.size());

		for (ProbProcessTree tree : selectedCandidates) {
			ProbProcessTreeCheck.exceptIfInconsistent(tree);

			ProbProcessTree mutatedTree = apply(tree, rng);

			/*
			 * If we don't allow duplicates (e.g. first part is not true) then
			 * we continue applying until we find a tree that is not already in
			 * the mutated population
			 */
			while (preventDuplicates && mutatedPopulation.contains(mutatedTree)) {
				mutatedTree = apply(tree, rng);
			}
			ProbProcessTreeCheck.exceptIfInconsistent(mutatedTree);

			//And add the mutated tree
			mutatedPopulation.add(mutatedTree);
		}

		return mutatedPopulation;
	}

	/**
	 * Applies mutation functions to the tree, depending on the tree's fitness
	 * characteristics and the provided probabilities
	 */
	public ProbProcessTree apply(ProbProcessTree tree, Random rng) {
		ProbProcessTree mutatedTree = tree;
		TreeMutationAbstract mutator;

		int nrTries = TreeMutationAbstract.MAX_TRIES;
		do {
			//Get a mutator
			mutator = getMutatorForChance(rng.nextDouble() * totalChance);
			//Get a mutated tree
			try {
				mutatedTree = mutator.mutate(tree);

				if (!ProbProcessTreeCheck.checkConsistent(mutatedTree)) {
					LOGGER.warn("Tree inconsistent: " + tree);
					mutatedTree = tree;
				}
			}catch (ProcessTreeConsistencyException ptce) {
				LOGGER.warn("Inconsistency during mutation for: " + tree, ptce);
				mutatedTree = tree;
			}
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
