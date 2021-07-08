package org.processmining.watchmaker;

import org.uncommons.watchmaker.framework.termination.TargetFitness;

/**
 * Legacy-fitting workaround because parameters are kept in termination conditions, instead
 * of just remembered in the registry directly. 

 * @author burkeat
 *
 */
public class TargetFitnessVisible extends TargetFitness{

	private double targetFitness;
	
	public TargetFitnessVisible(double targetFitness, boolean natural)
    {
		super(targetFitness, natural);
        this.targetFitness = targetFitness;
    }
	
	public double getTargetFitness() {
		return this.targetFitness;
	}
	
    public String toString() {
    	return "Target fitness == " + targetFitness;
    }

}
