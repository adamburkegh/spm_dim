package org.processmining.plugins.etm.parameters;

import java.util.List;

import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.metrics.ParetoFitnessEvaluator;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

/**
 * ETM Parameter object for the Pareto ETM engine specifically for the
 * configuration mining setting.
 * 
 * @author jbuijs
 * 
 */
public class ETMParamParetoConfigurable extends ETMParamPareto {

	//TODO apply/check all java bean properties
	//TODO implement null constructor

	protected CentralRegistryConfigurable centralRegistry;

	public ETMParamParetoConfigurable(CentralRegistryConfigurable centralRegistry,
			TreeFitnessAbstract fitnessEvaluator, ParetoFitnessEvaluator paretoFitnessEvaluator,
			List<EvolutionaryOperator<ProbProcessArrayTree>> evolutionaryOperators, int populationSize, int eliteCount) {
		super(centralRegistry, fitnessEvaluator, paretoFitnessEvaluator, evolutionaryOperators, populationSize,
				eliteCount);
		this.centralRegistry = centralRegistry;
	}

	public CentralRegistryConfigurable getCentralRegistry() {
		return centralRegistry;
	}

	public void setCentralRegistry(CentralRegistryConfigurable registry) {
		this.centralRegistry = registry;
	}

}
