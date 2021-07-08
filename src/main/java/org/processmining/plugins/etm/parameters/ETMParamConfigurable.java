package org.processmining.plugins.etm.parameters;

import java.util.List;

import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

/**
 * This parameter object is special for mining process trees with
 * configurations, and is thus suited for multiple event logs.
 * 
 * @author jbuijs
 * 
 */
public class ETMParamConfigurable extends ETMParam {

	protected CentralRegistryConfigurable centralRegistry;


	public ETMParamConfigurable(CentralRegistryConfigurable registry, TreeFitnessAbstract fitnessEvaluator,
			List<EvolutionaryOperator<ProbProcessArrayTree>> evolutionaryOperators, int populationSize, int eliteCount) {
		super(registry, fitnessEvaluator, evolutionaryOperators, populationSize, eliteCount);
		this.centralRegistry = registry;
	}

	public CentralRegistryConfigurable getCentralRegistry() {
		return centralRegistry;
	}

	public void setCentralRegistry(CentralRegistryConfigurable registry) {
		this.centralRegistry = registry;
	}

}
