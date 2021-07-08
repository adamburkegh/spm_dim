package qut.pm.spm.ppt;

import java.util.List;

public interface ProbProcessTree {

	String getLabel();
	List<ProbProcessTree> getChildren();
	double getWeight();
	boolean isLeaf();
	boolean isSilent();
	int size();
	
	/**
	 * Type is an integer in the range PPTOperator.size() + #{activities} + 1 (silent)
	 */
	int getType();
	String formatLabelForNode();
	
}
