package qut.pm.spm.ppt;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class PPTNodeImpl extends AbstractProbProcessTree implements ProbProcessTreeNode {

	protected PPTOperator operator;
	protected List<ProbProcessTree> children;
	protected int size;
	
	public PPTNodeImpl(PPTOperator operator) {
		super(0);
		this.operator = operator;
		children = new LinkedList<>();
		size = 1;
		type = operator.ordinal();
	}
	
	@Override
	public List<ProbProcessTree> getChildren() {
		return Collections.unmodifiableList(children);
	}
	
	@Override
	public void addChild(ProbProcessTree child) {
		children.add(child);
		weight += child.getWeight();
		size += child.size();
	}

	@Override
	public void addChildren(ProbProcessTree ... newChildNodes) {
		for (ProbProcessTree newChild: newChildNodes) {
			addChild(newChild);
		}
	}

	@Override
	public PPTOperator getOperator() {
		return operator;
	}
	
	@Override
	public String formatLabelForNode() {
		return operator.toString() + " " + getLabel();
	}
	
	@Override
	public int size() {
		return size;
	}

	
	
}
