package org.processmining.watchmaker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.EvolutionStrategyEngine;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.SteadyStateEvolutionEngine;
import org.uncommons.watchmaker.framework.interactive.InteractiveSelection;

/**
 * <p>
 * This class implements a general-purpose generational evolutionary algorithm.
 * It supports optional concurrent fitness evaluations to take full advantage of
 * multi-processor, multi-core and hyper-threaded machines.
 * </p>
 *
 * <p>
 * If multi-threading is enabled, evolution (mutation, cross-over, etc.) occurs
 * on the request thread but fitness evaluations are delegated to a pool of
 * worker threads. All of the host's available processing units are used (i.e.
 * on a quad-core machine there will be four fitness evaluation worker threads).
 * </p>
 *
 * <p>
 * If multi-threading is disabled, all work is performed synchronously on the
 * request thread. This strategy is suitable for restricted/managed environments
 * where it is not permitted for applications to manage their own threads. If
 * there are no restrictions on concurrency, applications should enable
 * multi-threading for improved performance.
 * </p>
 * 
 * Copy-forked here due to restrictive visiblity in original.
 *
 * @param <T> The type of entity that is to be evolved.
 * @see SteadyStateEvolutionEngine
 * @see EvolutionStrategyEngine
 * @author Daniel Dyer
 */

public class GenerationalTreeEvolutionEngine<T> extends AbstractTreeEvolutionEngine<T> {

	private static class NullFitnessEvaluator implements FitnessEvaluator<Object> {
		/**
		 * Returns a score of zero, regardless of the candidate being evaluated.
		 * 
		 * @param candidate  The individual to evaluate.
		 * @param population {@inheritDoc}
		 * @return Zero.
		 */
		public double getFitness(Object candidate, List<?> population) {
			return 0;
		}

		/**
		 * Always returns true. However, the return value of this method is irrelevant
		 * since no meaningful fitness scores are produced.
		 * 
		 * @return True.
		 */
		public boolean isNatural() {
			return true;
		}
	}

	protected final EvolutionaryOperator<T> evolutionScheme;
	protected final FitnessEvaluator<? super T> fitnessEvaluator;
	protected final SelectionStrategy<? super T> selectionStrategy;

	/**
	 * Creates a new evolution engine by specifying the various components required
	 * by a generational evolutionary algorithm.
	 * 
	 * @param candidateFactory  Factory used to create the initial population that
	 *                          is iteratively evolved.
	 * @param evolutionScheme   The combination of evolutionary operators used to
	 *                          evolve the population at each generation.
	 * @param fitnessEvaluator  A function for assigning fitness scores to candidate
	 *                          solutions.
	 * @param selectionStrategy A strategy for selecting which candidates survive to
	 *                          be evolved.
	 * @param rng               The source of randomness used by all stochastic
	 *                          processes (including evolutionary operators and
	 *                          selection strategies).
	 */
	public GenerationalTreeEvolutionEngine(CandidateFactory<T> candidateFactory,
			EvolutionaryOperator<T> evolutionScheme, FitnessEvaluator<? super T> fitnessEvaluator,
			SelectionStrategy<? super T> selectionStrategy, Random rng) {
		super(candidateFactory, fitnessEvaluator, rng);
		this.evolutionScheme = evolutionScheme;
		this.fitnessEvaluator = fitnessEvaluator;
		this.selectionStrategy = selectionStrategy;
	}

	/**
	 * Creates a new evolution engine for an interactive evolutionary algorithm. It
	 * is not necessary to specify a fitness evaluator for interactive evolution.
	 * 
	 * @param candidateFactory  Factory used to create the initial population that
	 *                          is iteratively evolved.
	 * @param evolutionScheme   The combination of evolutionary operators used to
	 *                          evolve the population at each generation.
	 * @param selectionStrategy Interactive selection strategy configured with
	 *                          appropriate console.
	 * @param rng               The source of randomness used by all stochastic
	 *                          processes (including evolutionary operators and
	 *                          selection strategies).
	 */
	public GenerationalTreeEvolutionEngine(CandidateFactory<T> candidateFactory,
			EvolutionaryOperator<T> evolutionScheme, InteractiveSelection<T> selectionStrategy, Random rng) {
		this(candidateFactory, evolutionScheme, new NullFitnessEvaluator(), // No fitness evaluations to perform.
				selectionStrategy, rng);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected List<EvaluatedCandidate<T>> nextEvolutionStep(List<EvaluatedCandidate<T>> evaluatedPopulation,
			int eliteCount, Random rng) {
		List<T> population = new ArrayList<T>(evaluatedPopulation.size());

		// First perform any elitist selection.
		List<T> elite = new ArrayList<T>(eliteCount);
		Iterator<EvaluatedCandidate<T>> iterator = evaluatedPopulation.iterator();
		while (elite.size() < eliteCount) {
			elite.add(iterator.next().getCandidate());
		}
		// Then select candidates that will be operated on to create the evolved
		// portion of the next generation.
		population.addAll(selectionStrategy.select(evaluatedPopulation, fitnessEvaluator.isNatural(),
				evaluatedPopulation.size() - eliteCount, rng));
		// Then evolve the population.
		population = evolutionScheme.apply(population, rng);
		// When the evolution is finished, add the elite to the population.
		population.addAll(elite);
		return evaluatePopulation(population);
	}
}
