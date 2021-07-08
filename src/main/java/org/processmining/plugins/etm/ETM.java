package org.processmining.plugins.etm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.processmining.plugins.etm.engines.ProbProcessArrayTreeEvolutionEngine;
import org.processmining.plugins.etm.live.ETMLiveListener.ETMListenerList;
import org.processmining.plugins.etm.live.ETMLiveListener.RunningState;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMParam;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;

/**
 * Basic ETM algorithm class that searches for a process tree that score best on
 * a single dimension.
 * 
 * @author jbuijs
 * 
 */
public class ETM extends ETMAbstract<ProbProcessArrayTree,ProbProcessArrayTree> {
	//FIXME Cancellation does not work, does not return the best result so far. (futures are destroyed in PromTask line 75)

	private static final long serialVersionUID = 1L;

	//Our precious params
	protected final ETMParam params;
	private Set<ProbProcessArrayTree> bestResults;

	/**
	 * Instantiate the ETM algorithm with the provided parameters.
	 * 
	 * @param parameters
	 *            The ETM parameters object to initialize the ETM algorithm
	 *            with. These parameters can not be changed once the algorithm
	 *            started.
	 */
	public ETM(final ETMParam parameters) {
		params = parameters;
		result = null;
		bestResults = new HashSet<ProbProcessArrayTree>();
	}

	/**
	 * Run the ETM algorithm with the provided parameters
	 */
	@Override
	public void run() {
		/*
		 * We need to recalculate the seed to get an overall fitness that
		 * corresponds to our weights. The ED will be 0 since each seed is in
		 * the list of trees to be compared and therefore its ED will be 0 (but
		 * it will influence the overall fitness)
		 */
		reEvaluateSeed(params.getSeed(), params.getFitnessEvaluator());

		/*
		 * Instantiate a new Watchmaker evolution engine.
		 */
		ProbProcessArrayTreeEvolutionEngine engine = new ProbProcessArrayTreeEvolutionEngine(params);

		//Start the engine!
		currentState = RunningState.RUNNING;
		//We want the whole population
		List<EvaluatedCandidate<ProbProcessArrayTree>> population = engine.evolvePopulation();
		if (!currentState.equals(RunningState.USERCANCELLED)) {
			/*
			 * Only switch to state 'terminated' if we did not get here after a
			 * user cancellation (which is a termination condition in itself...)
			 */
			currentState = RunningState.TERMINATED;
		}

		//The 'real' result is the best tree
		result = population.get(0).getCandidate();

		//Now store ALL the best trees
		double bestFitness = population.get(0).getFitness();
		for (EvaluatedCandidate<ProbProcessArrayTree> candidate : population) {
			if (candidate.getFitness() == bestFitness) {
				bestResults.add(candidate.getCandidate());
			} else {
				break;
			}
		}

		this.satisfiedTerminationConditions = engine.getSatisfiedTerminationConditions();
	}

	/**
	 * Since it could be that the population contains multiple trees with the
	 * same 'best' fitness value, this method returns all those trees. The
	 * {@link getResult()} method returns the tree that is sorted first in this
	 * list.
	 * 
	 * @return Set<NAryTree> all trees with the 'top' fitness
	 */
	public Set<ProbProcessArrayTree> getAllBestResults() {
		return bestResults;
	}

	public ETMListenerList<ProbProcessArrayTree> getListenerList() {
		return params.getListeners();
	}

	public ETMParamAbstract<ProbProcessArrayTree,ProbProcessArrayTree> getParams() {
		return params;
	}
}
