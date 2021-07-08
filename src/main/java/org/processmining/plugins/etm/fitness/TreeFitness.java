package org.processmining.plugins.etm.fitness;

import gnu.trove.iterator.TObjectDoubleIterator;
import gnu.trove.map.hash.TObjectDoubleHashMap;


/**
 * An object that stores fitness information for a particular (here unknown)
 * tree
 * 
 * @author jbuijs
 * 
 */
public class TreeFitness {

	public static double NOVALUE = -1;

	/**
	 * The single overall fitness value used in 'normal' ETM execution to sort
	 * and select elite
	 */
	private double overallFitnessValue;

	/**
	 * The {@link TreeFitnessInfo} that set the overall fitness value
	 */
	private TreeFitnessInfo overallFitnessInfo;

	/**
	 * More detailed information about the alignment between log and tree.
	 */
	public BehaviorCounter behaviorCounter;
	
	/**
	 * Is set to FALSE when the alignment calculation was aborted and hence no (real) fitness values are calculated
	 */
	private boolean isReliable = true;
	
	/**
	 * DEBUG Records in which generation this tree fitness was set 
	 */
	private int inGeneration = -1;

	/**
	 * A map of fitness values for the different dimensions
	 */
	public TObjectDoubleHashMap<TreeFitnessInfo> fitnessValues = new TObjectDoubleHashMap<TreeFitnessInfo>();

	/**
	 * the number of times this fitness is based on an approximation
	 */
	private int confidenceLevel = -1;

	public TreeFitness(int treeSize) {
		behaviorCounter = new BehaviorCounter(treeSize);

		overallFitnessValue = NOVALUE;
	}

	public boolean isSet() {
		return overallFitnessValue != NOVALUE;
	}

	public String toString() {
		String str = String.format("%.4f (", overallFitnessValue);

		TObjectDoubleIterator<TreeFitnessInfo> it = fitnessValues.iterator();
		while (it.hasNext()) {
			it.advance();
			str += String.format("%s: %.4f ", it.key(), it.value());
		}
		str += ")";

		return str;
	}

	/**
	 * Sets or updates the overall fitness for this tree. Also sets the info and
	 * value in the fitnessValues list
	 * 
	 * @param fitnessInfo
	 *            The {@link TreeFitnessInfo} object of the fitness measure that
	 *            set the overall value
	 * @param fitnessValue
	 *            The actual overall fitness value
	 */
	public void setOverallFitness(TreeFitnessInfo fitnessInfo, double fitnessValue) {
		this.overallFitnessInfo = fitnessInfo;
		this.overallFitnessValue = fitnessValue;
		fitnessValues.put(fitnessInfo, fitnessValue);
	}

	/**
	 * Returns the overall fitness value for this tree.
	 * 
	 * @return
	 */
	public double getOverallFitnessValue() {
		return overallFitnessValue;
	}

	/**
	 * Returns the {@link TreeFitnessInfo} of the fitness measure that set the
	 * overall fitness value.
	 * 
	 * @return
	 */
	public TreeFitnessInfo getOverallFitnessInfo() {
		return overallFitnessInfo;
	}

	/**
	 * Returns TRUE if the fitness values are reliable but FALSE if the alignment calculation was aborted 
	 * 
	 * @return the isReliable
	 */
	public boolean isReliable() {
		return isReliable;
	}

	/**
	 * Set to FALSE if the results stored in the fitness object are not reliable because calculations were aborted (f.i. alignments in Fr)
	 * @param isReliable the isReliable to set
	 */
	public void setReliable(boolean isReliable) {
		this.isReliable = isReliable;
	}

	/**
	 * @return the inGeneration
	 */
	public int getInGeneration() {
		return inGeneration;
	}

	/**
	 * @param inGeneration the inGeneration to set
	 */
	public void setInGeneration(int inGeneration) {
		this.inGeneration = inGeneration;
	}

	public int getConfidenceLevel() {
		return confidenceLevel;
	}

	public void setConfidenceLevel(int confidenceLevel) {
		this.confidenceLevel = confidenceLevel;
	}


}
