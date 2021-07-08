package qut.pm.spm.ppt;

import java.util.Collections;
import java.util.List;

public class PPTSilentImpl extends AbstractProbProcessTree{

	private static final int SILENT_TYPE = PPTOperator.values().length;
	
	public PPTSilentImpl(double weight) {
		super(weight);
	}

	@Override
	public List<ProbProcessTree> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String getLabel(){
		return "tau"; 
	}

	@Override
	public boolean isLeaf() {
		return true;
	}
	
	@Override
	public boolean isSilent() {
		return true;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public int getType() {
		return SILENT_TYPE;
	}

	
}

