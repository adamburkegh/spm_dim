package org.processmining.plugins.etm.engines;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.parameters.ETMParam;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;

/**
 * Specific implementation of an evolution engine for {@link ProbProcessArrayTree}s. Adds
 * functionality such as logging whole populations for analysis.
 * 
 * @author jbuijs
 * 
 */
public class ProbProcessArrayTreeEvolutionEngine extends LoggingEvolutionEngine<ProbProcessArrayTree> {

	public ProbProcessArrayTreeEvolutionEngine(ETMParam param) {
		super(param);
	}


	@Override
	protected List<EvaluatedCandidate<ProbProcessArrayTree>> nextEvolutionStep(
			List<EvaluatedCandidate<ProbProcessArrayTree>> evaluatedPopulation, int eliteCount, Random rng) {
		centralRegistry.increaseGeneration();

		System.gc(); 

		//Copy, paste and adjust from generational evolution engine.
		List<ProbProcessArrayTree> population = new ArrayList<ProbProcessArrayTree>(evaluatedPopulation.size());

		// First perform any elitist selection.
		List<ProbProcessArrayTree> elite = new ArrayList<ProbProcessArrayTree>(eliteCount);
		Iterator<EvaluatedCandidate<ProbProcessArrayTree>> iterator = evaluatedPopulation.iterator();
		while (elite.size() < eliteCount && iterator.hasNext()) {
			ProbProcessArrayTree candidate = iterator.next().getCandidate();
			//Prevent exact duplicate elite trees...
			if (!elite.contains(candidate)) {
				//Deep clone the elite since somewhere it might be that trees are still touched... :(
				elite.add(new ProbProcessArrayTreeImpl(candidate));
			}
		}

		/*
		 * It could be that there are more duplicates than
		 * populationsize-eliteCount, e.g. we tried all candidates but could not
		 * fill the elite with enough candidate
		 */
		if (elite.size() < eliteCount) {
			//If so, restart and add the best candidates until we have enough
			iterator = evaluatedPopulation.iterator();
			while (elite.size() < eliteCount) {
				elite.add(iterator.next().getCandidate());
			}
		}

		// Then select candidates that will be operated on to create the evolved
		// portion of the next generation.
		population.addAll(selectionStrategy.select(evaluatedPopulation, fitnessEvaluator.isNatural(),
				evaluatedPopulation.size() - eliteCount, rng));
		// Then evolve the population.
		population = evolutionScheme.apply(population, rng);
		// When the evolution is finished, add the elite to the population.

		population.addAll(elite);
		List<EvaluatedCandidate<ProbProcessArrayTree>> newEvaluatedPopulation = evaluatePopulation(population);
		if (params != null && params.getListeners() != null & newEvaluatedPopulation.get(0) != null) {
			params.getListeners().fireGenerationFinished(newEvaluatedPopulation.get(0).getCandidate());
		}
		return newEvaluatedPopulation;
	}

	/**
	 * Builds a string that described the whole provided result such that it can
	 * be logged
	 */
	public String logResult(List<EvaluatedCandidate<ProbProcessArrayTree>> result) {
		StringBuilder str = new StringBuilder();

		for (EvaluatedCandidate<ProbProcessArrayTree> cand : result) {
			ProbProcessArrayTree tree = cand.getCandidate();

			String detailedFitness = "";
			if (centralRegistry != null && centralRegistry.isFitnessKnown(tree)) {
				detailedFitness = centralRegistry.getFitness(tree).toString();
			}

			//Log the fitnessValue tree detailedFitness
			str.append(String.format("f: %2.10f  %s  %s \r\n", cand.getFitness(),
					TreeUtils.toString(tree, centralRegistry.getEventClasses()), detailedFitness));
		}

		return str.toString();
	}
}
