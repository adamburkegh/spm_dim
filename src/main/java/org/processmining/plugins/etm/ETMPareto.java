package org.processmining.plugins.etm;

import org.processmining.framework.providedobjects.ProvidedObjectManager;
import org.processmining.plugins.etm.engines.ParetoEngine;
import org.processmining.plugins.etm.live.ETMLiveListener.RunningState;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMListenerParams;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.processmining.plugins.etm.parameters.ETMParamPareto;

/**
 * Implementation of the ETM algorithm that results in a Pareto Front of
 * {@link Tree}s. For an explanation of the Pareto front see <a
 * href=http://en.wikipedia
 * .org/wiki/Pareto_front#Pareto_efficiency_in_short>Wikipedia on Pareto
 * Front</a> for instance.
 * 
 * @author jbuijs
 * 
 */
public class ETMPareto extends ETMAbstract<ParetoFront,ProbProcessArrayTree> {

	private static class ETMParetoListenerParams extends ETMParamAbstract<ParetoFront,ProbProcessArrayTree> {
	}
	
	private static final long serialVersionUID = 1L;

	//Our precious params
	protected transient final ETMParamPareto params;
	protected transient final ETMListenerParams<ParetoFront> listenerParams = new ETMParetoListenerParams();

	/**
	 * Instantiate the ETM Pareto algorithm with the provided parameters.
	 * 
	 * @param parameters
	 *            The ETM Pareto parameters object to initialize the ETM
	 *            algorithm with. These parameters can not be changed once the
	 *            algorithm started.
	 */
	public ETMPareto(ETMParamPareto parameters) {
		this.params = parameters;
	}

	/**
	 * Instantiate the ETM Pareto algorithm with the provided parameters.
	 * Additionally provide a ProM {@link ProvidedObjectManager} in order to run
	 * in LIVE mode, e.g. updating the view of the Pareto front at each update
	 * of the ETM.
	 * 
	 * @param parameters
	 *            The ETM Pareto parameters object to initialize the ETM
	 *            algorithm with. These parameters can not be changed once the
	 *            algorithm started.
	 * @param manager
	 *            The Provided Object Manager from a ProM Context. Required if
	 *            you want to run the ETM in 'live' mode.
	 */
	public ETMPareto(ETMParamPareto parameters, ProvidedObjectManager manager) {
		this.params = parameters;
		this.manager = manager;
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
		/*-
		ParetoEngine engine = new ParetoEngine(params.getCentralRegistry(), params.getFactory(),
				new EvolutionPipeline<NAryTree>(params.getEvolutionaryOperators()), params.getFitnessEvaluator(),
				params.getParetoFitnessEvaluator(), params.getSelectionStrategy(), params.getRng());/**/
		ParetoEngine engine = new ParetoEngine(params);

		//Set the preliminary pareto front result for live views
		this.result = engine.getParetoFront();

		//Start the engine!
		currentState = RunningState.RUNNING;
		engine.evolve();
		result = engine.getParetoFront();

		if (!currentState.equals(RunningState.USERCANCELLED)) {
			/*
			 * Only switch to state 'terminated' if we did not get here after a
			 * user cancellation (which is a termination condition in itself...)
			 */
			currentState = RunningState.TERMINATED;
		}
		this.satisfiedTerminationConditions = engine.getSatisfiedTerminationConditions();
	}

	@Override
	public ETMListenerParams<ParetoFront> getParams() {
		return listenerParams;
	}

}
