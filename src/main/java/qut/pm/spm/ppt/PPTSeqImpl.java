package qut.pm.spm.ppt;

public class PPTSeqImpl extends PPTNodeImpl {


	public PPTSeqImpl() {
		super(PPTOperator.SEQUENCE);
	}

	@Override
	public void addChild(ProbProcessTree child) {
		if (children.isEmpty()) {
			weight += child.getWeight();			
		}else {
			if (!ProbProcessTreeCheck.weightsNearlyEqual ( getWeight(), child.getWeight() ) )
				throw new ProcessTreeConsistencyException(
						"Sequences must be of equal weight to their children. Added " + child + " to " + this  );			
		}
		children.add(child);
		size += child.size();
	}
	
}
