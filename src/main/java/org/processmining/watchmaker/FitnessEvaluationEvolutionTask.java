package org.processmining.watchmaker;

import java.util.List;
import java.util.concurrent.Callable;

import org.uncommons.watchmaker.framework.EvaluatedCandidate;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

/**
* Callable task for performing parallel fitness evaluations. This is a copy-paste fork of 
* FitnessEvaluationTask in watchmaker due to overenthusiastic use of private keywords.
* 
* @param <T> The type of entity for which fitness is calculated.
* @author Adam Burke adapting Daniel Dyer
*/
class FitnessEvaluationEvolutionTask<T> implements Callable<EvaluatedCandidate<T>>
{
 private final FitnessEvaluator<? super T> fitnessEvaluator;
 private final T candidate;
 private final List<T> population;

 /**
  * Creates a task for performing fitness evaluations.
  * @param fitnessEvaluator The fitness function used to determine candidate fitness.
  * @param candidate The candidate to evaluate.
  * @param population The entire current population.  This will include all
  * of the candidates to evaluate along with any other individuals that are
  * not being evaluated by this task.
  */
 FitnessEvaluationEvolutionTask(FitnessEvaluator<? super T> fitnessEvaluator,
                        T candidate,
                        List<T> population)
 {
     this.fitnessEvaluator = fitnessEvaluator;
     this.candidate = candidate;
     this.population = population;
 }


 public EvaluatedCandidate<T> call()
 {
     return new EvaluatedCandidate<T>(candidate,
                                      fitnessEvaluator.getFitness(candidate, population));
 }
}

