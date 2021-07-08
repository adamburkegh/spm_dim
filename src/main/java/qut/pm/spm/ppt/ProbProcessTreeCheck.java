package qut.pm.spm.ppt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Based on Probabilistic Process Trees as described in 
 * Burke, Leemans and Wynn (2021) - Discovering Stochastic Process Models By Reduction and 
 * Abstraction
 * 
 * @author burkeat
 *
 */
public class ProbProcessTreeCheck {
	
	private static Logger LOGGER = LogManager.getLogger();
	
	private static final double EPSILON = 0.0000001d;
	
	public static boolean checkConsistent(ProbProcessTree ppt) {
		List<ProbProcessTree> children = ppt.getChildren();
		if (ppt instanceof ProbProcessTreeNode) {
			ProbProcessTreeNode node = (ProbProcessTreeNode)ppt;
			if (children.isEmpty())
				return false;
			boolean consistentChildren =  children.stream().allMatch(c -> checkConsistent(c) );
			if (!consistentChildren)
				return false;
			switch(node.getOperator()) {
			case CHOICE:
				break;
			case CONCURRENCY:
				break;
			case PROBLOOP:
				if (children.size() != 1)
					return false;
				break;
			case SEQUENCE:
				boolean checkWeights = children.stream().allMatch(c -> weightsNearlyEqual( c.getWeight(), ppt.getWeight() ) );
				if (!checkWeights)
					LOGGER.info("Inconsistent weights");
				return checkWeights; 
			default:
				break;
			}
		}
		if (children.isEmpty())
			return true;
		double childWeights = children.stream().map(c -> c.getWeight()).reduce(0d,Double::sum); 
		return weightsNearlyEqual(ppt.getWeight(),childWeights);
	}

	public static void exceptIfInconsistent(ProbProcessTree ppt) {
		if (!checkConsistent(ppt)) {
			LOGGER.warn("Tree inconsistent: " + ppt);
			throw new ProcessTreeConsistencyException("Inconsistent tree"); 
		}
	}
	
	public static Set<String> findActivities(ProbProcessTree ppt) {
		Set<String> result = new HashSet<>();
		if (ppt.getChildren().isEmpty() && !ppt.isSilent()) {
			result.add(ppt.getLabel());
		}else {
			for (ProbProcessTree child: ppt.getChildren()) {
				result.addAll( findActivities(child) );
			}
		}
		return result;
	}

	public static boolean weightsNearlyEqual(double d1, double d2) {
		return Math.abs(d1-d2) < EPSILON;
	}
	
	/**
	 * alpha function in the paper 
	 */
	public static Set<String> startingSymbols(ProbProcessTree ppt){
		Set<String> result = new HashSet<>();
		if (ppt.isSilent())
			return result;
		List<ProbProcessTree> children = ppt.getChildren();
		if (children.isEmpty()) {
			result.add(ppt.getLabel());
			return result;
		}
		if (ppt instanceof ProbProcessTreeNode) {
			ProbProcessTreeNode node = (ProbProcessTreeNode)ppt;
			switch(node.getOperator()) {
			case CHOICE:
				for (ProbProcessTree child: children) {
					result.addAll( startingSymbols(child) );
				}
				break;
			case CONCURRENCY:
				for (ProbProcessTree child: children) {
					result.addAll( expand(child) );
				}				
				break;
			case PROBLOOP:
			case SEQUENCE:
				result.addAll( startingSymbols(children.get(0)) );
				break;
			default:
				break;
			}
		}
		return result;
	}

	/**
	 * exp function in the paper
	 * 
	 * @param child
	 * @return
	 */
	private static Set<String> expand(ProbProcessTree ppt) {
		Set<String> result = new HashSet<>();
		if (ppt.isSilent())
			return result;
		List<ProbProcessTree> children = ppt.getChildren();
		if (children.isEmpty()) {
			result.add(ppt.getLabel());
			return result;
		}
		for (ProbProcessTree child: children) {
			result.addAll( expand(child) );
		}
		return result;
	}

	/**
	 * 
	 * alpha_st function in the paper. This fixes a subtle bug in the paper definition - it is 
	 * not the intersection of all children but the union of all pairwise intersections.
	 * 
	 * @param ppt
	 * @return
	 */
	private static Set<String> nonDetStartingSymbols(ProbProcessTree ppt){
		Set<String> result = new HashSet<>();
		List<ProbProcessTree> children = ppt.getChildren();
		if (ppt instanceof ProbProcessTreeNode) {
			ProbProcessTreeNode node = (ProbProcessTreeNode)ppt;
			switch(node.getOperator()) {
			case CHOICE:
			case CONCURRENCY:
				for (ProbProcessTree child: children) {
					for (ProbProcessTree sibling: children) {
						if (sibling == child)
							continue;
						Set<String> scs = startingSymbols(child);
						Set<String> sss = startingSymbols(sibling);
						scs.retainAll(sss);
						result.addAll(scs);
					}
				}
				break;
			case PROBLOOP:
			case SEQUENCE:
				return startingSymbols(ppt);
			default:
				break;
			}
		}
		return result;
	}

	
	public static boolean deterministic(ProbProcessTree ppt) {
		List<ProbProcessTree> children = ppt.getChildren();
		if (children.isEmpty())
			return true;
		if (ppt instanceof ProbProcessTreeNode) {
			ProbProcessTreeNode node = (ProbProcessTreeNode)ppt;
			switch(node.getOperator()) {
			case CHOICE:
			case CONCURRENCY:
				return nonDetStartingSymbols(ppt).isEmpty();
			case PROBLOOP:
			case SEQUENCE:
				return children.stream().allMatch( c -> deterministic(c) );
			default:
				return false;
			}
		}
		return false;
	}

	public static boolean uselessChild(ProbProcessTreeNode parent, ProbProcessTree child) {
		if (parent.getOperator() == PPTOperator.SEQUENCE 
				|| parent.getOperator() == PPTOperator.CONCURRENCY 
				|| parent.getOperator() == PPTOperator.PROBLOOP) {
			return child.isSilent();
		}
		return child.getWeight() == 0.0d;
	}

	public static boolean uselessParent(ProbProcessTree parentIn) {
		if (parentIn.isLeaf())
			return false;
		ProbProcessTreeNode parent = (ProbProcessTreeNode)parentIn;
		if (parent.getOperator() == PPTOperator.SEQUENCE 
				|| parent.getOperator() == PPTOperator.CONCURRENCY ) {
			return parent.getChildren().size() <= 1;
		}
		if (parent.getOperator() == PPTOperator.PROBLOOP) {
			List<ProbProcessTree> children = parent.getChildren();
			if (children.size() == 0)
				return true;
			return children.get(0).isSilent();
		}
		return false;
	}

	
}
