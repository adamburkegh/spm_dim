package qut.pm.spm.ppt;

public interface ProbProcessTreeNode extends ProbProcessTree{
	// Consider rewriting as immutable
	
	public PPTOperator getOperator();

	void addChildren(ProbProcessTree ... children);

	void addChild(ProbProcessTree child);
	
}
