package qut.pm.spm.ppt;

import java.util.Collections;
import java.util.List;

public class PPTLeafImpl extends AbstractProbProcessTree implements ProbProcessTreeLeaf {

	protected String activity;

	public PPTLeafImpl(String activity, double weight) {
		super(weight);
		this.activity = activity;
		this.type = ProbProcessTreeFactory.getActivityType(activity);
	}
	
	public PPTLeafImpl(String activity, int type, double weight) {
		super(weight);
		this.activity = activity;
		this.type = type;
	}
	
	@Override 
	public String getLabel(){
		return getActivity(); 
	}
	
	@Override
	public List<ProbProcessTree> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public String getActivity() {
		return activity;
	}

	@Override
	public int size() {
		return 1;
	}
	
	
}
