package qut.pm.setm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.ppt.PPTOperator;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeCheck;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
import qut.pm.spm.ppt.ProbProcessTreeProjector;
import qut.pm.util.ClockUtil;


public class RandomProbProcessTreeGenerator {

	private static final int PROVIDED_SEED = -1;
	public static final int DEFAULT_MAX_DEPTH = 30;
	public static final int DEFAULT_MAX_TRANSITIONS = 5000;

	private static Logger LOGGER = LogManager.getLogger();

	private Random random; 
	private long seed;
	private int maxDepth;
	private int maxTransitions;

	private static final PPTOperator[] OPERATOR_VALUES = PPTOperator.values();
	
	public RandomProbProcessTreeGenerator(Random random, int maxDepth, int maxTransitions) {
		this.seed = PROVIDED_SEED;
		this.maxDepth = maxDepth;
		this.maxTransitions = maxTransitions;
		this.random = random;
	}
	

	public RandomProbProcessTreeGenerator(long seed, int maxDepth, int maxTransitions) {
		this(new Random(seed),maxDepth,maxTransitions);
		this.seed = seed;
	}

	public RandomProbProcessTreeGenerator(Random random) {
		this(random, DEFAULT_MAX_DEPTH, DEFAULT_MAX_TRANSITIONS );
	}
	
	public RandomProbProcessTreeGenerator() {
		this(ClockUtil.nanoTime(), DEFAULT_MAX_DEPTH, DEFAULT_MAX_TRANSITIONS );
	}

	
	/**
	 * 
	 * Generate random Probabilistic Process trees. They include all of the activities provided 
	 * at least once. 
	 * 
	 * @param activities Activities in expected lexical order
	 * @param targetNumber
	 * @return
	 */
	public Set<ProbProcessTree> generateTrees(List<String> activities, int targetNumber){
		LOGGER.info("Generating trees for activities: " + activities + " using seed: " + seed);
		Set<ProbProcessTree> result = new HashSet<>();
		ProbProcessTreeConverter converter = new ProbProcessTreeConverter();
		for (int i=0; i<targetNumber; ) {
			ProbProcessTree tree = generateTree(activities);
			AcceptingStochasticNet snet = converter.convertToSNet(tree);
			StochasticNet net = snet.getNet();
			if (net.getTransitions().size() > maxTransitions) {
				LOGGER.info("Regenerating candidate model which exceeded maximum of " + maxTransitions 
						+ " transitions. It had " 
						+ net.getTransitions().size() + " transitions, " 
						+ net.getPlaces().size() + " places and "
						+ net.getEdges().size() + " edges.");
				LOGGER.info("Generated " + i + " of " + targetNumber + " models");
				continue;
			}
			i++;
			result.add(tree);
			LOGGER.info("Randomly generated " + i + " trees");
		}
		return result;
	}

	public ProbProcessTree generateTree(List<String> activities) {
		ProbProcessTree result = null;
		Set<String> activitiesCovered = new HashSet<String>();
		LOGGER.info("Randomly generating tree");
		while ( activitiesCovered.size() != activities.size() ) {
			LOGGER.debug("Coverage ... {} ",activitiesCovered );
			ProbProcessTree next = generatePotentiallyIncompleteTree(activities, maxDepth);
			if (result == null) {
				result = next;
			}else {
				ProbProcessTreeNode newResult = 
						ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
				newResult.addChild(result);
				newResult.addChild(next);
				result = newResult;
			}
			activitiesCovered = ProbProcessTreeCheck.findActivities(result);
		}
		return result;
	}

	private ProbProcessTree generatePotentiallyIncompleteTree(List<String> activities, int maxDepth) {
		double randValue = random.nextDouble();
		int range = OPERATOR_VALUES.length + 1 + activities.size();
		int nextNodeType = 0;
		if (maxDepth > 1) {
			nextNodeType = (int)(randValue*(double)range);		
		}else {
			nextNodeType = OPERATOR_VALUES.length + (int)(randValue*(double)activities.size());
		}
		ProbProcessTree next = generateNext(activities, nextNodeType);
		if (nextNodeType < OPERATOR_VALUES.length) {
			generatorOperatorNode(activities, OPERATOR_VALUES[nextNodeType], (ProbProcessTreeNode)next, maxDepth);
		}
		LOGGER.debug("Generated {}", next);
		return next;
	}

	private void generatorOperatorNode(List<String> activities, PPTOperator operator, ProbProcessTreeNode nextNode, int maxDepth) {
		int nrChildren = (int)(random.nextDouble()*activities.size())+1;
		List<ProbProcessTree> children = new ArrayList<>();
		for (int i=0; i<nrChildren; i++) {
			ProbProcessTree child = generatePotentiallyIncompleteTree(activities, maxDepth -1);
			children.add(child);
		}
		addChildrenToOperator(operator, nextNode, children);
	}

	private void addChildrenToOperator(PPTOperator nextNodeOperator, ProbProcessTreeNode nextNode, List<ProbProcessTree> children) {
		switch(nextNodeOperator) {
		case PROBLOOP:
			addChildrenToLoop(nextNode,children);
			break;
		case SEQUENCE:
			addChildrenToSequence(nextNode,children);
			break;
		default:
			for( ProbProcessTree child: children) {
				nextNode.addChild(child);
			}
			break;
		}
	}

	private void addChildrenToSequence(ProbProcessTreeNode nextNode, List<ProbProcessTree> children) {
		double maxWeight = children.stream().map(c -> c.getWeight()).reduce(0d,Double::max);
		for( ProbProcessTree child: children) {
			ProbProcessTree rescaledChild = ProbProcessTreeProjector.rescaleTo(child,maxWeight);
			nextNode.addChild(rescaledChild);
		}
	}

	private void addChildrenToLoop(ProbProcessTreeNode nextNode, List<ProbProcessTree> children) {
		nextNode.addChild(children.get(0));
	}

	private ProbProcessTree generateNext(List<String> activities, int nextNodeType) {
		ProbProcessTree next; 
		if (nextNodeType < OPERATOR_VALUES.length) {
			PPTOperator operator = OPERATOR_VALUES[nextNodeType];
			switch(operator) {
			case PROBLOOP:
				next = ProbProcessTreeFactory.createLoop(2.0);
				break;
			default:
				next = ProbProcessTreeFactory.createNode(operator);
				break;
			}

		}else {
			if (nextNodeType == OPERATOR_VALUES.length) {
				next = ProbProcessTreeFactory.createSilent(1.0d);
			}else {
				int activityIndex = nextNodeType - OPERATOR_VALUES.length - 1;
				next = ProbProcessTreeFactory.createLeaf(activities.get(activityIndex),1.0d);
			}
		}
		return next;
	}
	
	
	
}
