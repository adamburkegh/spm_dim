package org.processmining.plugins.etm.parameters;

import java.util.ArrayList;
import java.util.List;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.ETM;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

/**
 * Parameter class for the {@link ETM} algorithm.
 * 
 * @author jbuijs
 * 
 */

public class ETMParam extends ETMParamAbstract<ProbProcessArrayTree,ProbProcessArrayTree> {

	public ETMParam(CentralRegistry registry, TreeFitnessAbstract fitnessEvaluator,
			List<EvolutionaryOperator<ProbProcessArrayTree>> evolutionaryOperators, int populationSize, int eliteCount) {
		super(registry, fitnessEvaluator, evolutionaryOperators, populationSize, eliteCount);
	}

	public ETMParam(CentralRegistry registry, TreeFitnessAbstract evaluator,
			ArrayList<EvolutionaryOperator<ProbProcessArrayTree>> evolutionaryOperators, int popSize, int eliteSize) {
		super(registry, evaluator, evolutionaryOperators, popSize, eliteSize);
	}

	//Package restricted empty constructor
	ETMParam() {
		super();
	}

}
