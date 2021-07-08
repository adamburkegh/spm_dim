package org.processmining.plugins.etm.fitness;

import java.util.Comparator;

/**
 * Compares TreeFitness objects based on the provided TreeFitnessInfo. Takes
 * into account whether treefitness info is natural or not and wheter fittest
 * candidates should be sorted first or last.
 * 
 * @author jbuijs
 * 
 */
public class TreeFitnessComparator implements Comparator<TreeFitness> {

	protected TreeFitnessInfo info;
	private boolean fittestFirst;

	/**
	 * 
	 * @param dimension
	 *            The dimension to consider during sorting
	 */
	public TreeFitnessComparator(boolean fittestFirst, TreeFitnessInfo dimension) {
		this.info = dimension;
		this.fittestFirst = fittestFirst;
	}

	/**
	 * Sorts two treeFitness objects such that the 'bigger' one is the fitter
	 * one
	 */
	public int compare(TreeFitness o1, TreeFitness o2) {
		//if isnatural then high value is fitter
		//if fittest first then higher values first
		//if both are false, then higher values still first :)
		if(! o1.fitnessValues.contains(info) || ! o2.fitnessValues.contains(info)){
			//System.out.println("breaky");
		}
		
		double f1 = o1.fitnessValues.contains(info) ? o1.fitnessValues.get(info) : info.getWorstFitnessValue();
		double f2 = o2.fitnessValues.contains(info) ? o2.fitnessValues.get(info) : info.getWorstFitnessValue();
		return (info.isNatural() == !fittestFirst ? 1 : -1) * Double.compare(f1, f2);
	}

}
