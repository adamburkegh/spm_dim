package org.processmining.plugins.etm.parameters;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;

/**
 * Helper class that wraps around the low-level/basic parameter object and
 * requires higher level input to create a standard/common instantiation of a
 * parameter object.
 * 
 * @author jbuijs
 * 
 */
public class ETMParamSimple {

	//FIXME make the GUI wizard smarter and operate on/interpret the original class directly (e.g. show the evals and weights of the Of)

	private int eliteCount = 6;
	private int populationSize = 20;
	private int maxIterations = 2000;
	private double crossoverProbability = .1;
	private double randomMutationRatioOverGuided = 1; //FIXME smart mutation disabled since we have no behC
	private boolean preventDuplicates = false;
	private int randomCandidateCount = 0;
	private double fitnessWeight = 10;
	private double precisionWeight = 10;
	private double generalizationWeight = 1;
	private double simplicityWeight = 1;
	private double targetFitness = 1;
	private int steadyStates = Integer.MAX_VALUE;
	private int maxDuration = Integer.MAX_VALUE;
	private XLog eventlog = null;
	private PluginContext context = null;
	private double maxF = -1;//no getters or setters
	private double maxFTime = 1000;//no getters or setters

	public ETMParam createETMParams() {
		ETMParam param = ETMParamFactory.buildParam(eventlog, context, populationSize, eliteCount,
				randomCandidateCount, crossoverProbability, randomMutationRatioOverGuided, preventDuplicates,
				maxIterations, targetFitness, fitnessWeight, maxF, maxFTime, precisionWeight, generalizationWeight,
				simplicityWeight);
		param.addTerminationConditionSteadyState(steadyStates, false);
		param.addTerminationConditionMaxDuration(maxDuration);
		return param;
	}

	public int getMaxIterations() {
		return maxIterations;
	}

	public double getCrossoverProbability() {
		return crossoverProbability;
	}

	public double getRandomMutationRatioOverGuided() {
		return randomMutationRatioOverGuided;
	}

	public void setCrossoverProbability(double crossoverProbability) {
		this.crossoverProbability = crossoverProbability;
	}

	public void setRandomCandidateCount(int randomCandidateCount) {
		this.randomCandidateCount = randomCandidateCount;
	}

	public void setRandomMutationRatioOverGuided(double randomMutationRatioOverGuided) {
		this.randomMutationRatioOverGuided = randomMutationRatioOverGuided;
	}

	public double getGeneralizationWeight() {
		return generalizationWeight;
	}

	public double getFitnessWeight() {
		return fitnessWeight;
	}

	public double getPrecisionWeight() {
		return precisionWeight;
	}

	public double getSimplicityWeight() {
		return this.simplicityWeight;
	}

	public void setFitnessWeight(double d) {
		this.fitnessWeight = d;
	}

	public void setPrecisionWeight(double d) {
		this.precisionWeight = d;
	}

	public void setGeneralizationWeight(double d) {
		this.generalizationWeight = d;
	}

	public void setSimplicityWeight(double d) {
		this.simplicityWeight = d;
	}

	public double getTargetFitness() {
		return this.targetFitness;
	}

	public int getSteadyStates() {
		return steadyStates;
	}

	public int getMaxDuration() {
		return maxDuration;
	}

	public void setTargetFitness(double targetFitness) {
		this.targetFitness = targetFitness;
	}

	public void setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
	}

	public void setSteadyStates(int steadyStates) {
		this.steadyStates = steadyStates;
	}

	public void setMaxDuration(int maxDuration) {
		this.maxDuration = maxDuration;
	}

	public int getPopulationSize() {
		return populationSize;
	}

	public void setPopulationSize(int populationSize) {
		this.populationSize = populationSize;
	}

	public void setEliteCount(int eliteCount) {
		this.eliteCount = eliteCount;
	}

	/**
	 * @param eventlog
	 *            the eventlog to set
	 */
	public void setEventlog(XLog eventlog) {
		this.eventlog = eventlog;
	}

	/**
	 * @param context
	 *            the context to set
	 */
	public void setContext(PluginContext context) {
		this.context = context;
	}

}