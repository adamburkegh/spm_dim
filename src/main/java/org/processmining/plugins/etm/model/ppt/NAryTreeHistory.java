package org.processmining.plugins.etm.model.ppt;


/**
 * 
 * Class to store the history of changes of the trees through the evolutionary process.
 *
 */
public class NAryTreeHistory {
	
	private final ProbProcessArrayTree parent;
	
	private final int locationOfChange;
	
	private final TypesOfTreeChange typeOfChange;
	
	public NAryTreeHistory(ProbProcessArrayTree _parent, int _locationOfChange, TypesOfTreeChange _typeOfChange) {
		this.parent = _parent;
		this.locationOfChange = _locationOfChange;
		this.typeOfChange = _typeOfChange;
	}

	public ProbProcessArrayTree getParent() {
		return this.parent;
	}

	public int getLocationOfChange() {
		return this.locationOfChange;
	}
	
	public TypesOfTreeChange getTypeOfChange(){
		return this.typeOfChange;
	}

}
