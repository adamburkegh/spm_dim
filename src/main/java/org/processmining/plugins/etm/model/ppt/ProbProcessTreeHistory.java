package org.processmining.plugins.etm.model.ppt;

import qut.pm.spm.ppt.ProbProcessTree;

/**
 * 
 * Class to store the history of changes of the trees through the evolutionary process.
 *
 */
public class ProbProcessTreeHistory {
	
	
	private final ProbProcessTree parent;
	
	private final int locationOfChange;
	
	private final TypesOfTreeChange typeOfChange;
	
	public ProbProcessTreeHistory(ProbProcessTree _parent, int _locationOfChange, TypesOfTreeChange _typeOfChange) {
		this.parent = _parent;
		this.locationOfChange = _locationOfChange;
		this.typeOfChange = _typeOfChange;
	}

	public ProbProcessTree getParent() {
		return this.parent;
	}

	public int getLocationOfChange() {
		return this.locationOfChange;
	}
	
	public TypesOfTreeChange getTypeOfChange(){
		return this.typeOfChange;
	}

}
