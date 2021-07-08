package qut.pm.spm.ppt;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import qut.pm.setm.mutation.MutationException;

public class ProbProcessTreeProjector {

	private static Logger LOGGER = LogManager.getLogger();
	
	public static ProbProcessTree rescaleTo(ProbProcessTree node, double maxWeight) {
		if (Double.doubleToLongBits(node.getWeight()) == Double.doubleToLongBits(maxWeight))
			return node;
		double ratio = maxWeight / node.getWeight();
		return rescale(node,ratio);
	}

	public static ProbProcessTree rescale(ProbProcessTree node, double ratio) {
		if (ratio == 1.0d)
			return node;
		ProbProcessTree result = ProbProcessTreeFactory.createFrom(node,ratio*node.getWeight());
		for(ProbProcessTree child: node.getChildren()) {
			ProbProcessTree rescaled = rescale(child,ratio);
			ProbProcessTreeNode opNode = (ProbProcessTreeNode)result;
			opNode.addChild(rescaled);
		}
		return result;
	}

	/**
	 *  
	 * @param pptIn
	 * @param targetIndex (includes 0). Walk tree top to bottom, left to right.
	 * @param pptWith
	 * @return
	 */
	public static ProbProcessTreeNode replaceSubNode(ProbProcessTreeNode pptIn, int targetIndex, ProbProcessTree pptWith) {
		if (pptIn.size() < targetIndex || pptIn.getChildren().size() == 0)
			throw new MutationException("Invalid node index " +  targetIndex + " for tree " + pptIn);
		List<ProbProcessTree> children = pptIn.getChildren();
		int childIndex = 0;
		ProbProcessTree child = children.get(childIndex);
		int ctIndex = child.size();
		while (ctIndex <= targetIndex) {
			childIndex++;
			child = children.get(childIndex);
			ctIndex += child.size();
		}
		int prevCtIndex = ctIndex - child.size();
		if (prevCtIndex == targetIndex) {
			return replaceChild(pptIn, childIndex, pptWith);
		}
		// grandchild node
		int gcIndex = targetIndex - prevCtIndex -1;
		ProbProcessTree newChild = replaceSubNode((ProbProcessTreeNode)child, gcIndex, pptWith);
		return replaceChild(pptIn, childIndex, newChild);
	}
	
	public static ProbProcessTree findSubNode(ProbProcessTreeNode pptIn, int targetIndex) {
		if (targetIndex >= pptIn.size()-1) {
			LOGGER.error("Target index past tree range");
			throw new MutationException("Target index " + targetIndex + " past tree range " + pptIn.size());
		}
		List<ProbProcessTree> children = pptIn.getChildren();
		int childIndex = 0;
		ProbProcessTree child = children.get(childIndex);
		int ctIndex = child.size();
		while (ctIndex <= targetIndex) {
			childIndex++;
			child = children.get(childIndex);
			ctIndex += child.size();
		}
		int prevCtIndex = ctIndex - child.size();
		if (prevCtIndex == targetIndex)
			return child;
		int gcIndex = targetIndex - prevCtIndex -1;
		return findSubNode((ProbProcessTreeNode)child, gcIndex);
	}

	private static ProbProcessTreeNode replaceChild(ProbProcessTreeNode pptIn, int childIndex, ProbProcessTree pptWith) {
		ProbProcessTreeNode pptInNode = pptIn;
		ProbProcessTreeNode newTree = ProbProcessTreeFactory.createNode(pptInNode.getOperator());
		ProbProcessTree oldChild = pptIn.getChildren().get(childIndex);
		if (oldChild.getWeight() != pptWith.getWeight()) {
			pptInNode = (ProbProcessTreeNode)adjustWeights(pptIn,oldChild.getWeight(),pptWith.getWeight());
		}
		int ctIndex = 0;
		for (ProbProcessTree child: pptInNode.getChildren()) {
			if (ctIndex == childIndex) {
				newTree.addChild(pptWith);
			}else {
				newTree.addChild( child );
			}
			ctIndex++;
		}
		return newTree;
	}

	private static ProbProcessTree adjustWeights(ProbProcessTree pptIn, double oldWeight, double newWeight) {
		if (oldWeight == newWeight)
			return pptIn;
		return rescale(pptIn,newWeight/oldWeight);
	}

	public static ProbProcessTreeNode removeSubNode(ProbProcessTreeNode pptIn, int targetIndex) {
		if (pptIn.size() <= targetIndex || pptIn.getChildren().size() == 0 )
			throw new MutationException("Invalid node index " +  targetIndex + " for tree " + pptIn);
		List<ProbProcessTree> children = pptIn.getChildren();
		int childIndex = 0;
		ProbProcessTree child = children.get(childIndex);
		int ctIndex = child.size();
		while (ctIndex <= targetIndex) {
			childIndex++;
			child = children.get(childIndex);
			ctIndex += child.size();
		}
		int prevCtIndex = ctIndex - child.size();
		if (prevCtIndex == targetIndex) {
			return removeChild(pptIn, childIndex);
		}
		// grandchild node
		int gcIndex = targetIndex - prevCtIndex -1;
		ProbProcessTree newChild = removeSubNode((ProbProcessTreeNode)child, gcIndex);
		return replaceChild(pptIn, childIndex, newChild);
	}

	public static ProbProcessTreeNode removeChild(ProbProcessTreeNode pptIn, int childIndex) {
		// recreate the tree without that child
		ProbProcessTreeNode pptInNode = pptIn;
		ProbProcessTreeNode newTree = ProbProcessTreeFactory.createNode(pptInNode.getOperator());
		int ctIndex = 0;
		for (ProbProcessTree child: pptInNode.getChildren()) {
			if (ctIndex != childIndex) {
				newTree.addChild( child );
			}
			ctIndex++;
		}
		return newTree;
	}

	
}
