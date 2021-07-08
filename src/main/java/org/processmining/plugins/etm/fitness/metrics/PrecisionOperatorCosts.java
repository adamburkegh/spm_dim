package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

public class PrecisionOperatorCosts extends TreeFitnessAbstract {

	private final static boolean debug = false;

	public static final TreeFitnessInfo info = new TreeFitnessInfo(PrecisionOperatorCosts.class, "Pc",
			"Precision - Costs per node",
			"Using a standard cost function for different operator nodes, and their height in the tree, "
					+ "reduces the allowed behavior of the process model.", Dimension.PRECISION, false);

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		return getCost(candidate, 0);
	}

	public static double getCost(ProbProcessArrayTree candidate, int index) {
		//FIXME test and improve
		int nrChildren = candidate.nChildren(index);

		double cost = 1; //Start with 1 to already punish for each node

		switch (candidate.getType(index)) {
		//FIXME: ILV Operator treated as sequence?
			case ProbProcessArrayTree.ILV :
			case ProbProcessArrayTree.SEQ :
			case ProbProcessArrayTree.REVSEQ :
				// A Sequence is the most precise operator, therefore just sum the precision of the children
				for (int c = 0; c < nrChildren; c++) {
					cost += getCost(candidate, candidate.getChildAtIndex(index, c));
				}
				break;
			case ProbProcessArrayTree.XOR :
				//Although an XOR allows for a choice, it does not introduce more behaviour by interleaving its children. Therefore also sum (FOR NOW) 
				for (int c = 0; c < nrChildren; c++) {
					cost += getCost(candidate, candidate.getChildAtIndex(index, c));
				}
				break;
			case ProbProcessArrayTree.AND :
				// An AND allows for interleaving its children. Although we won't calculate the total number of traces, we do punish rather heavily by multiplying the childrens' precision values
				if (cost == 0) {
					cost = 1;
				}
				for (int c = 0; c < nrChildren; c++) {
					cost *= getCost(candidate, candidate.getChildAtIndex(index, c)) + 1; //Add 1 to multiply with at least 2 (sneaky ETM)
				}
				break;
			case ProbProcessArrayTree.OR :
				//Since an OR is an AND with the additional option to skip some of its children, we punish by adding and multiplying the values
				int sum = 0;
				int multiplication = 1;
				for (int c = 0; c < nrChildren; c++) {
					double childCost = getCost(candidate, candidate.getChildAtIndex(index, c));
					sum += childCost;
					multiplication *= childCost + 1; //Add 1 to multiply with at least 2 (sneaky ETM)
				}
				cost = sum + multiplication;
				break;
			case ProbProcessArrayTree.LOOP :
				//LOOPS are really bad, we take the 2nd power of the behavior of both the do and redo parts plus of course the behavior of the exit block
				//DO:
				cost += Math.pow(getCost(candidate, candidate.getChildAtIndex(index, 0)) + 1, 2);
				//REDO:
				cost += Math.pow(getCost(candidate, candidate.getChildAtIndex(index, 1)) + 1, 2);
				//EXIT:
				cost += getCost(candidate, candidate.getChildAtIndex(index, 2));
				break;
			default :
				//LEAFs for instance are the base case, ergo 1:
				cost = 1;
		}

		if (debug) {
			System.out.println("Cost for " + index + ": " + cost);
		}

		if (cost < 1) {
			System.out.println("Low precision cost for " + TreeUtils.toString(candidate));
			cost = 0;
		}

		return cost;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}

}
