package qut.pm.setm.engines;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.processmining.watchmaker.GenerationalTreeEvolutionEngine;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;

import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeFactory;

/**
 * Specific implementation of an evolution engine for {@link ProbProcessTree}s. Adds
 * functionality such as logging whole populations for analysis.
 * 
 * 
 */
public class ProbProcessTreeEvolutionEngine extends GenerationalTreeEvolutionEngine<ProbProcessTree> {

	private static Logger LOGGER = LogManager.getLogger();
	
	private CentralRegistry centralRegistry;
	private ETMParamAbstract<ProbProcessTree, ProbProcessTree> params;
	private int generation = 0;

	public ProbProcessTreeEvolutionEngine(ETMParamAbstract<ProbProcessTree,ProbProcessTree> params) {
		super(params.getFactory(), 
				new EvolutionPipeline<ProbProcessTree>(params.getEvolutionaryOperators()), 
				params.getFitnessEvaluator(), params.getSelectionStrategy(), params.getRng());
		centralRegistry = params.getCentralRegistry();
		// Add the evolutionary observers
		for (EvolutionObserver<ProbProcessTree> obs : params.getEvolutionObservers()) {
			addEvolutionObserver(obs);
		}
		setSingleThreaded(false);
		this.params = params;
	}

	@Override
	protected List<EvaluatedCandidate<ProbProcessTree>> evaluatePopulation(List<ProbProcessTree> population) {
		//Call the existing function on the whole
		LOGGER.info("Generation " + generation);
		List<EvaluatedCandidate<ProbProcessTree>> result = super.evaluatePopulation(population);

		//Sort the result list to create easier to read/scan logs (last result has highest fitness value!)
		// Collections.sort(result);
		exportPopulation(result);

		//And the next generation will have one number higher
		generation++;
		return result;
	}
	
	private void exportPopulation(List<EvaluatedCandidate<ProbProcessTree>> result) {
		double maxFitness = 0d;
		for (EvaluatedCandidate<ProbProcessTree> evalCandidate: result) {
			LOGGER.debug( evalCandidate.getCandidate() );
			if (evalCandidate.getFitness() > maxFitness)
				maxFitness = evalCandidate.getFitness();
			LOGGER.debug( "Fitness:", evalCandidate.getFitness() );
		}
		LOGGER.info("Max Fitness for this generation: {}", maxFitness);
	}

	@Override
	protected List<EvaluatedCandidate<ProbProcessTree>> nextEvolutionStep(
			List<EvaluatedCandidate<ProbProcessTree>> evaluatedPopulation, int eliteCount, Random rng) {
		centralRegistry.increaseGeneration();

		//Copy, paste and adjust from generational evolution engine.
		List<ProbProcessTree> population = new ArrayList<ProbProcessTree>(evaluatedPopulation.size());

		// First perform any elitist selection.
		List<ProbProcessTree> elite = new ArrayList<ProbProcessTree>(eliteCount);
		Iterator<EvaluatedCandidate<ProbProcessTree>> iterator = evaluatedPopulation.iterator();
		while (elite.size() < eliteCount && iterator.hasNext()) {
			ProbProcessTree candidate = iterator.next().getCandidate();
			//Prevent exact duplicate elite trees...
			if (!elite.contains(candidate)) {
				//Deep clone the elite since somewhere it might be that trees are still touched... :(
				elite.add( ProbProcessTreeFactory.copy(candidate));
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
		List<EvaluatedCandidate<ProbProcessTree>> newEvaluatedPopulation = evaluatePopulation(population);
		if (params != null && params.getListeners() != null & newEvaluatedPopulation.get(0) != null) {
			params.getListeners().fireGenerationFinished(newEvaluatedPopulation.get(0).getCandidate());
		}
		return newEvaluatedPopulation;
	}

	/**
	 * Builds a string that described the whole provided result such that it can
	 * be logged
	 */
	public String logResult(List<EvaluatedCandidate<ProbProcessTree>> result) {
		StringBuilder str = new StringBuilder();

		for (EvaluatedCandidate<ProbProcessTree> cand : result) {
			ProbProcessTree tree = cand.getCandidate();

			//Log the fitnessValue tree detailedFitness
			str.append(String.format("f: %2.10f  %s  %s \r\n", cand.getFitness(), tree));
		}

		return str.toString();
	}
}
