package org.processmining.plugins.etm.termination;

import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.TerminationCondition;

/**
 * Generic termination condition that can be told to cancel the algorithm at the next query. 
 * @author jbuijs
 *
 */
public class ExternalTerminationCondition implements TerminationCondition {
	public boolean shouldTerminate = false;
	
	public ExternalTerminationCondition() {
	}

	public boolean shouldTerminate(PopulationData<?> populationData) {
		return shouldTerminate;
	}
}
