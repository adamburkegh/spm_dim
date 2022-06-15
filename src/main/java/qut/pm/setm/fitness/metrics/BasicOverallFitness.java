package qut.pm.setm.fitness.metrics;

import java.util.LinkedHashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeFormatter;
import qut.pm.spm.ppt.ProcessTreeConsistencyException;

/**
 * Meta fitness calculator. Calculates multiple fitnesses and then calculates
 * the weighted average.
 */
public class BasicOverallFitness implements FitnessEvaluator<ProbProcessTree> {

	private static Logger LOGGER = LogManager.getLogger();
	/**
	 * We require the fitness algo in a particular order (e.g. replay fitness
	 * before precision!). Hence we require a linkedhashmap
	 */
	private LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> evaluators;

	protected BasicOverallFitness() {
		evaluators = new LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double>();
	}


	/**
	 * Constructor which is provided a list of instantiated fitness algorithms
	 * and their weights
	 * 
	 * @param seed
	 * 
	 * @param alg
	 */
	public BasicOverallFitness(LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> alg) {
		evaluators = alg;
	}


	public void addEvaluator(FitnessEvaluator<ProbProcessTree> evaluator, double weight) {
		evaluators.put(evaluator, new Double(weight));
	}

	/**
	 * Returns the currently set fitness evaluators and their weights. This
	 * linkedHashMap can be updated if desired.
	 * 
	 * @return
	 */
	public LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> getEvaluators() {
		return evaluators;
	}

	@Override
	public double getFitness(ProbProcessTree candidate, List<? extends ProbProcessTree> population) {
		double result = 0;
		for (FitnessEvaluator<ProbProcessTree> evaluator: evaluators.keySet()) {
			Double evalWeight = evaluators.get(evaluator);
			try {
				 double evRes = evaluator.getFitness(candidate,population) * evalWeight ;
				 if (evRes < 0 || evRes > 1) {
					 LOGGER.error("Measure out of [0,1] bounds. mu = " + evRes + " for " 
							 		+ evaluator + "\n" 
							 		+ new ProbProcessTreeFormatter().textTree(candidate) 
							 		+ "\n");
				 }
				 result += evRes;
			}catch(ProcessTreeConsistencyException ptce) {
				LOGGER.error("Inconsistent tree; fitness = 0" + candidate);
				return 0;
			}
		}
		return result / (double)evaluators.size();
	}

	@Override
	public boolean isNatural() {
		return true;
	}


}