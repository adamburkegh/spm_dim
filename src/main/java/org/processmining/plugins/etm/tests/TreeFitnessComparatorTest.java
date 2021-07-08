package org.processmining.plugins.etm.tests;

import java.util.List;
import java.util.Random;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.experiments.StandardLogs;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.metrics.EditDistanceWrapperRTEDAbsolute;
import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

public class TreeFitnessComparatorTest {

	public static void main(String[] args) {
		XLog log = StandardLogs.createDefaultLog();
		XLogInfo info = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.NAME_CLASSIFIER);

		ProbProcessArrayTree tree1 = TreeUtils.fromString("SEQ( LEAF: A )", info.getEventClasses());
		ProbProcessArrayTree tree2 = TreeUtils.fromString("SEQ( LEAF: B )", info.getEventClasses());
		ProbProcessArrayTree tree3 = TreeUtils.fromString("SEQ( LEAF: C )", info.getEventClasses());

		TreeFitnessInfo f1 = EditDistanceWrapperRTEDAbsolute.info;
		TreeFitnessInfo f2 = FitnessReplay.info;

		CentralRegistry reg = new CentralRegistry(log, new Random());

		reg.getFitness(tree1).fitnessValues.put(f1, 0);
		reg.getFitness(tree1).fitnessValues.put(f2, .75);
		reg.getFitness(tree2).fitnessValues.put(f1, 1);
		reg.getFitness(tree2).fitnessValues.put(f2, .9);
		reg.getFitness(tree3).fitnessValues.put(f1, 0);
		reg.getFitness(tree3).fitnessValues.put(f2, .9);

		//Tree 1 is better in f1, tree 2 is better in f2
		List<ProbProcessArrayTree> sorted = reg.getSortedOn(false, new TreeFitnessInfo[] { f1, f2 });
		
		System.out.println("Sorted as: " + sorted);
	}

}
