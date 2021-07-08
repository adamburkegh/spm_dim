package org.processmining.plugins.etm.termination;

import java.util.Date;

import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.TerminationCondition;

public class TerminateAtCertainTime implements TerminationCondition {

	private Date terminationDateTime;

	/**
	 * Stop the algorithm as soon as the provided date (with time) has passed.
	 * 
	 * @param dateTime
	 */
	public TerminateAtCertainTime(Date dateTime) {
		this.terminationDateTime = dateTime;
	}

	public boolean shouldTerminate(PopulationData<?> populationData) {
		//if now is after termination dateTime then terminate!
		return new Date().after(terminationDateTime);
	}
}
