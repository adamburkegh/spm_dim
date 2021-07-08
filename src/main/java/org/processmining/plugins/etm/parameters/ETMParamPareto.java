package org.processmining.plugins.etm.parameters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.ETMPareto;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.metrics.ParetoFitnessEvaluator;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

/**
 * Parameter class for the {@link ETMPareto} algorithm.
 * 
 * @author jbuijs
 * 
 */
public class ETMParamPareto extends ETMParamAbstract<ParetoFront,ProbProcessArrayTree> {


	private ParetoFitnessEvaluator paretoFitnessEvaluator;
	
	//The dimensions we want to have ignored in our Pareto front
	private Set<TreeFitnessInfo> ignoredDimensions = new HashSet<TreeFitnessInfo>();

	private Map<TreeFitnessInfo, Double> upperlimits = new HashMap<TreeFitnessInfo, Double>();
	private Map<TreeFitnessInfo, Double> lowerlimits = new HashMap<TreeFitnessInfo, Double>();

	/**
	 * Since in the beginning it might occur that the random trees do not fall
	 * within the limits, setting the limits can be postponed until a certain
	 * generation has been reached.
	 */
	private int generationWhenLimitsAreApplied = -1;

	private int paretoFrontMaxSize;

	public ETMParamPareto(CentralRegistry centralRegistry, TreeFitnessAbstract fitnessEvaluator,
			ParetoFitnessEvaluator paretoFitnessEvaluator, List<EvolutionaryOperator<ProbProcessArrayTree>> evolutionaryOperators,
			int populationSize, int eliteCount) {
		super(centralRegistry, fitnessEvaluator, evolutionaryOperators, populationSize, eliteCount);

		if (paretoFitnessEvaluator == null) {
			throw new IllegalArgumentException("Pareto Fitness Evaluator can not be null");
		}

		this.paretoFitnessEvaluator = paretoFitnessEvaluator;
	}

	ETMParamPareto() {
		super();
	}

	/**
	 * Set the complete set of dimensions to ignore.
	 * 
	 * @param ignoreddimensions
	 */
	public void setIgnoredDimensions(Set<TreeFitnessInfo> ignoreddimensions) {
		this.ignoredDimensions = ignoreddimensions;
	}

	/**
	 * Add one dimension to the current list of dimensions to ignore
	 * 
	 * @param dimensionCode
	 */
	public void addIgnoredDimension(TreeFitnessInfo ignoredDimension) {
		ignoredDimensions.add(ignoredDimension);
	}

	/**
	 * Get the current list of ignored dimensions
	 * 
	 * @return
	 */
	public Set<TreeFitnessInfo> getIgnoredDimensions() {
		return ignoredDimensions;
	}


	/**
	 * The ParetoFitnessEvaluator is a special kind of evaluator that uses the
	 * Pareto front to assign a fitness (f.i. the number of trees that dominate
	 * the candidate tree). This can be used to grade candidate trees on how
	 * 'close' they are to the Pareto front.
	 * 
	 * @return {@link ParetoFitnessEvaluator} The fitness evaluator used to
	 *         produce a fitness value for the candidate trees using the Pareto
	 *         front.
	 */
	public ParetoFitnessEvaluator getParetoFitnessEvaluator() {
		return paretoFitnessEvaluator;
	}

	public void setParetoFitnessEvaluator(ParetoFitnessEvaluator paretoFitnessEvaluator) {
		this.paretoFitnessEvaluator = paretoFitnessEvaluator;
	}

	/**
	 * Set a new upper limit for the given dimension. No candidates with a value
	 * higher than this limit will be accepted in the Pareto front.
	 * 
	 * @param dimension
	 *            Dimension to update upper limit for
	 * @param limit
	 *            New upper limit
	 */
	public void updateUpperLimit(TreeFitnessInfo dimension, double limit) {
		upperlimits.put(dimension, limit);
	}

	/**
	 * Set a new lower limit for the given dimension. No candidates with a value
	 * lower than this limit will be accepted in the Pareto front.
	 * 
	 * @param dimension
	 *            Dimension to update lower limit for
	 * @param limit
	 *            New lower limit
	 */
	public void updateLowerLimit(TreeFitnessInfo dimension, double limit) {
		lowerlimits.put(dimension, limit);
	}

	/**
	 * @return the upperlimits
	 */
	public Map<TreeFitnessInfo, Double> getUpperlimits() {
		return upperlimits;
	}

	/**
	 * @return the lowerlimits
	 */
	public Map<TreeFitnessInfo, Double> getLowerlimits() {
		return lowerlimits;
	}

	public void setGenerationWhenLimitsAreApplied(int i) {
		generationWhenLimitsAreApplied = i;
	}

	public int getGenerationWhenLimitsAreApplied() {
		return generationWhenLimitsAreApplied;
	}

	/**
	 * Sets the maximum size of the Pareto front, when the front grows larger
	 * worst trees are removed until this size is again reached.
	 * 
	 * @param paretoFrontMaxSize
	 *            If 0 or less no size limit is imposed
	 */
	public void setParetoFrontMaxSize(int paretoFrontMaxSize) {
		this.paretoFrontMaxSize = paretoFrontMaxSize;
	}

	/**
	 * Returns the maximum size of the Pareto front, i.e. when the front grows
	 * larger worst trees are removed until this size is again reached.
	 * 
	 * @return
	 */
	public int getParetoFrontMaxSize() {
		return paretoFrontMaxSize;
	}

}
