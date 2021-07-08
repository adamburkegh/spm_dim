package org.processmining.watchmaker;

import org.uncommons.watchmaker.framework.termination.GenerationCount;

/**
 * Legacy-fitting workaround because parameters are kept in termination conditions, instead
 * of just remembered in the registry directly. 
 * 
 * @author burkeat
 *
 */
public class GenerationCountVisible extends GenerationCount{

	private int generationCount;
	
    public GenerationCountVisible(int generationCount) {
    	super(generationCount);
    	this.generationCount = generationCount;
    }
    
    public int getGenerationCount() {
    	return generationCount;
    }

    public String toString() {
    	return "Generation count == " + generationCount;
    }
    
}
