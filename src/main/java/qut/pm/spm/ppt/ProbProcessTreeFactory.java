package qut.pm.spm.ppt;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
 
public class ProbProcessTreeFactory {

	private static final int NO_ACTIVITY_TYPE = -1;
	public static final int OFFSET = PPTOperator.values().length+1;
	public static final int TAU = OFFSET;
	private static String[] labels = new String[] {};
	private static Map<String,Integer> labelMap = new HashMap<>();

	
	public static void initActivityRegistry(String[] labelArray) {
		labels = labelArray;
		labelMap = new HashMap<>(labelArray.length);
		for (int i=0; i<labels.length; i++) {
			labelMap.put(labels[i],i);
		}
	}
	
	public static int getActivityType(String activity) {
		return labelMap.getOrDefault(activity,NO_ACTIVITY_TYPE);
	}
	
	public static ProbProcessTree createSilent(double weight) {
		return new PPTSilentImpl(weight);
	}
	
	public static ProbProcessTreeLeaf createLeaf(String activity, double weight) {
		Integer existing = labelMap.get(activity);
		if (existing == null) {
			String[] labelArray = Arrays.copyOf(labels, labels.length+1);
			existing = labels.length;
			labelArray[existing] = activity;
			initActivityRegistry(labelArray);

		}
		return new PPTLeafImpl(activity,existing+OFFSET, weight);
	}
	
	public static ProbProcessTree createLeaf(int type, double weight) {
		int arrIndex = type-OFFSET;
		if (arrIndex == -1)
			return createSilent(weight);
		if (arrIndex < 0 || arrIndex > labels.length)
			throw new ProcessTreeConsistencyException("Invalid type:" + type);
		String activity = labels[arrIndex];
		return new PPTLeafImpl(activity,type,weight);
	}
	
	public static ProbProcessTreeNode createNode(PPTOperator operator) {
		switch(operator) {
		case PROBLOOP:
			return createLoop();
		case SEQUENCE:
			return createSequence();
		default: 
			return new PPTNodeImpl(operator);
		}
	}
	
	public static ProbProcessTreeNode createLoop() {
		 return new PPTLoopImpl();
	}

	public static ProbProcessTreeNode createSequence() {
		 return new PPTSeqImpl();
	}

	public static ProbProcessTreeNode createLoop(ProbProcessTree child) {
		ProbProcessTreeNode loop = createLoop();
		loop.addChild(child);
		return loop;
	}

	public static ProbProcessTree createFrom(ProbProcessTree node, double newWeight) {
		if (node instanceof ProbProcessTreeLeaf )
			return createLeaf(node.getLabel(),newWeight);
		if (node instanceof PPTSilentImpl )
			return createSilent(newWeight);
		return createNode(((ProbProcessTreeNode)node).getOperator());
	}

	public static ProbProcessTreeNode copy(ProbProcessTreeNode node) {
		ProbProcessTreeNode result = createNode(node.getOperator());
		for (ProbProcessTree child: node.getChildren()) {
			result.addChild( copy(child) );
		}
		return result;
	}
	
	public static ProbProcessTree copy(ProbProcessTree ppt) {
		if (ppt instanceof ProbProcessTreeLeaf )
			return createLeaf(ppt.getLabel(),ppt.getWeight());
		if (ppt instanceof PPTSilentImpl )
			return createSilent(ppt.getWeight());
		return copy(((ProbProcessTreeNode)ppt));
		
	}
	
}
