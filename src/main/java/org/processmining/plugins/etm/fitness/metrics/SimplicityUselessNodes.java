package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class SimplicityUselessNodes extends TreeFitnessAbstract {

	public static final TreeFitnessInfo info = new TreeFitnessInfo(
			SimplicityUselessNodes.class,
			"Su",
			"Simplicity - Useless Nodes",
			"Punishes for nodes that are useless, e.g. operators with one child and tau's in a SEQ, AND or OR construct.",
			Dimension.SIMPLICITY, true);

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		return 1 - uselessNodesRatio(candidate);
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}

	public static double uselessNodesRatio(ProbProcessArrayTree tree) {
		double badNodes = 0;
		//Loop through the tree
		for (int i = 0; i < tree.size(); i++) {
			if (isUselessNode(tree, i)) {
				badNodes++;
			}
		}

		return badNodes / tree.size();
	}

	//TODO update javadoc
	/**
	 * Tests if the given node in the tree is a useless node. A node is useless
	 * if at least one of the following conditions hold: (1) it is an operator
	 * with only 1 child, (2) it is a tau in a SEQ or AND, (3) a LOOP with only
	 * TAU children (including the children).
	 * 
	 * @param tree
	 *            NAryTree to inspect
	 * @param n
	 *            Node in the tree to evaluate
	 * @return TRUE is the node can be removed without changing the behaviour
	 *         (does NOT guarantee fitness of f.i. simplicity to remain the
	 *         same!). See conditions above.
	 */
	public static boolean isUselessNode(ProbProcessArrayTree tree, int n) {
		//1. Operator nodes with 1 child
		if (!tree.isLeaf(n) && tree.nChildren(n) == 1) {
			return true;
		}

		//2. TAUs in a AND or SEQ construct
		if (tree.getType(n) == ProbProcessArrayTree.TAU
				&& (tree.getType(tree.getParent(n)) == ProbProcessArrayTree.AND || tree.getType(tree.getParent(n)) == ProbProcessArrayTree.SEQ || tree
						.getType(tree.getParent(n)) == ProbProcessArrayTree.REVSEQ)) {
			return true;
		}
		//Or TAUs in 

		//3. Any operator that consists only of useless and/or TAU child nodes, is useless itself
		if (!tree.isLeaf(n)) {
			boolean allUseless = true;
			for (int c = 0; c < tree.nChildren(n); c++) {
				//If we encounter one non-useless non-Tau node, it's ok...
				if (!isUselessNode(tree, tree.getChildAtIndex(n, c))
						&& tree.getType(tree.getChildAtIndex(n, c)) != ProbProcessArrayTree.TAU) {
					allUseless = false;
				}
			}
			if (allUseless)
				return true;
		}

		//3b. A TAU is useless
		/*-
		if (tree.getType(n) == NAryTree.TAU && tree.getParent(n) != NAryTree.NONE
				&& isUselessNode(tree, tree.getParent(n))) {
			return true;
		}/**/

		//4. not-first TAU's in XOR and OR constructs (e.g. there is already another TAU that allows skipping etc.)
		if (tree.getType(n) == ProbProcessArrayTree.TAU
				&& (tree.getType(tree.getParent(n)) == ProbProcessArrayTree.XOR || tree.getType(tree.getParent(n)) == ProbProcessArrayTree.OR)) {
			//Check if one of the 'earlier' children is a TAU, if so, this TAU is useless!
			int par = tree.getParent(n);
			for (int child = 0; tree.getChildAtIndex(par, child) != n; child++) {
				if (tree.getType(tree.getChildAtIndex(par, child)) == ProbProcessArrayTree.TAU
						|| isUselessNode(tree, tree.getChildAtIndex(par, child))) {
					return true;
				}
			}
			//5.: Loop with 2 TAU children and then again a loop...
		}

		//5. Loops consisting of 2 TAUs and another loop...
		if (tree.getType(n) == ProbProcessArrayTree.LOOP) {
			boolean firstTau = false;
			boolean secondTau = false;
			boolean loop = false;
			//Check all Loop-children
			for (int i = 0; i < tree.nChildren(n); i++) {
				//If child i is a loop
				if (tree.getType(tree.getChildAtIndex(n, i)) == ProbProcessArrayTree.LOOP) {
					if (loop) //already found a loop
						break; //pattern break, pattern not true
					else
						loop = true; //hit first sub-loop
					//If child i is a TAU or another useless node...
				} else if (tree.getType(tree.getChildAtIndex(n, i)) == ProbProcessArrayTree.TAU
						|| isUselessNode(tree, tree.getChildAtIndex(n, i))) {
					if (firstTau) {
						if (secondTau) {
							break; //should have detected 3 Tau's before!, pattern not true
						} else {
							secondTau = true; //2nd tau hit
						}
					} else {
						firstTau = true;//first tau hit
					}
				}
				//If one of the children of the loop is not a LOOP or TAU we can 'early exit'  
				else {
					break; //pattern not true
				}
			}
			//checked all children, now check if pattern of 2xtau 1xloop in a loop is correct
			if (firstTau && secondTau && loop) {
				return true;
			}
		}//end loop with 2 TAU children pattern

		//6. Operator nodes that are of the same type as their parents (except for loops, I guess we covered loops)
		//Exception: configurable process trees! (since both operators can have different configuration options set)
		
		if (tree.getNumberOfConfigurations() == 0 && tree.getType(n) != ProbProcessArrayTree.LOOP
				&& tree.getType(n) == tree.getType(tree.getParent(n))) {
			return true;
		}

		return false;
	}
}
