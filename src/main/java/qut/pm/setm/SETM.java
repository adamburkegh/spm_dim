package qut.pm.setm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.processmining.plugins.etm.ETMAbstract;
import org.processmining.plugins.etm.live.ETMLiveListener.ETMListenerList;
import org.processmining.plugins.etm.live.ETMLiveListener.RunningState;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;

import qut.pm.setm.engines.ProbProcessTreeEvolutionEngine;
import qut.pm.setm.parameters.SETMParam;
import qut.pm.spm.ppt.ProbProcessTree;

/**
 * Basic SETM algorithm class that searches for a process tree that score best on
 * a single dimension.
 * 
 * @author burkeat adapting jbuijs
 * 
 */
public class SETM extends ETMAbstract<ProbProcessTree,ProbProcessTree> {
	// Cancellation does not work, does not return the best result so far. (futures are destroyed in PromTask line 75)

	private static final long serialVersionUID = 1L;

	//Our precious params
	protected final SETMParam params;
	private Set<ProbProcessTree> bestResults;

	/**
	 * Instantiate the ETM algorithm with the provided parameters.
	 * 
	 * @param parameters
	 *            The ETM parameters object to initialize the ETM algorithm
	 *            with. These parameters can not be changed once the algorithm
	 *            started.
	 */
	public SETM(final SETMParam parameters) {
		params = parameters;
		result = null;
		bestResults = new HashSet<ProbProcessTree>();
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
		ProbProcessTreeEvolutionEngine engine = new ProbProcessTreeEvolutionEngine(params);

		//Start the engine!
		currentState = RunningState.RUNNING;
		//We want the whole population
		List<EvaluatedCandidate<ProbProcessTree>> population = 
				engine.evolvePopulation(params.getPopulationSize(), params.getEliteCount(), params.getSeed(),
									   params.getTerminationConditionsAsArray());
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
		for (EvaluatedCandidate<ProbProcessTree> candidate : population) {
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
	public Set<ProbProcessTree> getAllBestResults() {
		return bestResults;
	}

	public ETMListenerList<ProbProcessTree> getListenerList() {
		return params.getListeners();
	}

	public ETMParamAbstract<ProbProcessTree,ProbProcessTree> getParams() {
		return params;
	}
}
