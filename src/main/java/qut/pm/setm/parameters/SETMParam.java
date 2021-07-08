package qut.pm.setm.parameters;

import java.util.ArrayList;
import java.util.List;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.ETM;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

import qut.pm.spm.ppt.ProbProcessTree;

/**
 * Parameter class for the {@link ETM} algorithm.
 * 
 * @author jbuijs
 * 
 */

public class SETMParam extends ETMParamAbstract<ProbProcessTree,ProbProcessTree> {

	public SETMParam(CentralRegistry registry, FitnessEvaluator<ProbProcessTree> fitnessEvaluator,
			List<EvolutionaryOperator<ProbProcessTree>> evolutionaryOperators, int populationSize, int eliteCount) {
		super(registry, fitnessEvaluator, evolutionaryOperators, populationSize, eliteCount);
	}

	public SETMParam(CentralRegistry registry, FitnessEvaluator<ProbProcessTree> evaluator,
			ArrayList<EvolutionaryOperator<ProbProcessTree>> evolutionaryOperators, int popSize, int eliteSize) {
		super(registry, evaluator, evolutionaryOperators, popSize, eliteSize);
	}

	protected SETMParam() {
		super();
	}


}
