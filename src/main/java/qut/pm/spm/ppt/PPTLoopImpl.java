package qut.pm.spm.ppt;

public class PPTLoopImpl extends PPTNodeImpl {

	public PPTLoopImpl() {
		super(PPTOperator.PROBLOOP);
	}

	@Override
	public void addChild(ProbProcessTree child) {
		if (children.isEmpty()) {
			super.addChild(child);
			return;
		}
		throw new ProcessTreeConsistencyException("Loops may only have one child");
	}

	
}
