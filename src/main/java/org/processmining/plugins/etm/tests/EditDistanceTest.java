package org.processmining.plugins.etm.tests;

import java.util.Random;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.experiments.StandardLogs;
import org.processmining.plugins.etm.fitness.metrics.EditDistanceWrapperRTEDRelative;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

public class EditDistanceTest {

	public static void main(String... args) {
		XLog log = StandardLogs.createMergedLog();
		CentralRegistry reg = new CentralRegistry(log, new Random());

		ProbProcessArrayTree baseTree = TreeUtils
				.fromString(
						//"SEQ( SEQ( LEAF: A+complete , XOR( SEQ( SEQ( LEAF: B+complete , LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: F+complete ) ) , SEQ( LEAF: C+complete , XOR( SEQ( LEAF: B+complete , LEAF: F+complete ) , SEQ( LEAF: D+complete , SEQ( LEAF: B+complete , LEAF: E+complete ) ) ) ) ) ) , LEAF: G+complete ) ",
						"SEQ( SEQ( OR( LEAF: A+complete , LEAF: B+complete ) , XOR( SEQ( LEAF: B1+complete , OR( LEAF: B2+complete , LEAF: D2+complete ) , AND( LEAF: C+complete , LEAF: D2+complete ) ) , OR( LEAF: D+complete , LEAF: C+complete ) ) ) , XOR( SEQ( LEAF: B+complete , XOR( LEAF: E+complete , LEAF: F+complete ) ) , SEQ( LEAF: F+complete , LEAF: G+complete ) ) )",
						reg.getEventClasses());
		ProbProcessArrayTree leafUpdOne = TreeUtils
				.fromString(
						"SEQ( SEQ( LEAF: B+complete , XOR( SEQ( SEQ( LEAF: B+complete , LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: F+complete ) ) , SEQ( LEAF: C+complete , XOR( SEQ( LEAF: B+complete , LEAF: F+complete ) , SEQ( LEAF: D+complete , SEQ( LEAF: B+complete , LEAF: E+complete ) ) ) ) ) ) , LEAF: G+complete ) ",
						reg.getEventClasses());
		ProbProcessArrayTree opUpdOne = TreeUtils
				.fromString(
						"SEQ( AND( LEAF: A+complete , XOR( SEQ( SEQ( LEAF: B+complete , LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: F+complete ) ) , SEQ( LEAF: C+complete , XOR( SEQ( LEAF: B+complete , LEAF: F+complete ) , SEQ( LEAF: D+complete , SEQ( LEAF: B+complete , LEAF: E+complete ) ) ) ) ) ) , LEAF: G+complete ) ",
						reg.getEventClasses());
		ProbProcessArrayTree tree1 = TreeUtils
				.fromString(
						"SEQ( LEAF: A+complete , LEAF: B1+complete , LEAF: B2+complete , LEAF: C+complete , LEAF: D2+complete , XOR( LEAF: E+complete , LEAF: F+complete ) , LEAF: G+complete )",
						reg.getEventClasses());
		ProbProcessArrayTree random = TreeUtils.randomTree(8, .4, 6, 8);

		EditDistanceWrapperRTEDRelative ed = new EditDistanceWrapperRTEDRelative(baseTree);

		System.out.println(String.format("ED one leaf updated: %1.3f", ed.getFitness(leafUpdOne, null)));
		System.out.println(String.format("ED one oper updated: %1.3f", ed.getFitness(opUpdOne, null)));
		System.out.println(String.format("ED provided tree   : %1.3f", ed.getFitness(tree1, null)));
		System.out.println("Random tree: " + TreeUtils.toString(random, reg.getEventClasses()));
		System.out.println(String.format("ED random          : %1.3f", ed.getFitness(random, null)));
		System.out.println("code: " + ed.getInfo().toString());
	}
}
