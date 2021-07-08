package org.processmining.plugins.etm.tests;

import java.util.ArrayList;
import java.util.UUID;

import org.processmining.framework.util.Pair;
import org.processmining.plugins.etm.model.narytree.conversion.NAryTreeToProcessTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.configuration.controlflow.ControlFlowConfiguration;

/**
 * Testing translations between ProcessTrees and NAryTrees (back and forth)
 * 
 * @author jbuijs
 * 
 */
public class TranslationsTest {

	public static void main(String[] args) {
		testNATtoPT();
	}

	public static void testNATtoPT() {
		String treeString = "SEQ( LEAF: A , AND( LEAF: A , LEAF: B ) ) [[<, -,H,- ] ]";

		ProbProcessArrayTree tree = TreeUtils.fromString(treeString);
		//tree.addConfiguration(new org.processmining.plugins.etm.model.narytree.Configuration(new boolean[] {true},new boolean[] {false}));

		Pair<ProcessTree, ArrayList<ControlFlowConfiguration>> res = NAryTreeToProcessTree.convertWithConfiguration(null, tree,
				"Config test");

		ProcessTree pt = res.getFirst();
		System.out.println("NAT: " + TreeUtils.toString(tree));
		System.out.println("PT: " + pt.toString());

		System.out.println("Config: ");
		for (ControlFlowConfiguration config : res.getSecond()) {
			System.out.println("  " + config.toString(pt));
		}
		System.out.println("");

		res.getFirst().getEdge(new UUID(0, 0));

	}

}
