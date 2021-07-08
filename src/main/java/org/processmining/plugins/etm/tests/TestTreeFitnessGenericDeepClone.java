package org.processmining.plugins.etm.tests;

import java.util.LinkedHashMap;
import java.util.Random;

import nl.tue.astar.AStarThread.Canceller;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.fitness.metrics.Generalization;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.fitness.metrics.PrecisionEscEdges;
import org.processmining.plugins.etm.fitness.metrics.SimplicityMixed;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;

public class TestTreeFitnessGenericDeepClone {

	public static void main(String[] args) {
		testDeepClone();
	}

	private static void testDeepClone() {
		int nrTries = 1000;
		XLog log = LogCreator.createLog(new String[][] { { "A" } });

		CentralRegistry registry = new CentralRegistry(log, new Random());

		Canceller c = new Canceller() {

			public boolean isCancelled() {
				return false;
			}
		};

		for (int i = 0; i < nrTries; i++) {
			FitnessReplay fr = new FitnessReplay(registry, c);

			PrecisionEscEdges pe = new PrecisionEscEdges(registry);

			Generalization ge = new Generalization(registry);

			SimplicityMixed sm = new SimplicityMixed();

			/*-
			TreeFitnessAbstract deepClone = TreeFitnessAbstract.deepClone(fr);

			FitnessReplay fr2 = (FitnessReplay) deepClone;
			
			TreeFitnessAbstract peDeepClone = TreeFitnessAbstract.deepClone(pe);
			
			PrecisionEscEdges pe2 = (PrecisionEscEdges) peDeepClone;
			/**/

			LinkedHashMap<TreeFitnessAbstract, Double> alg = new LinkedHashMap<TreeFitnessAbstract, Double>();
			alg.put(fr, 5.);
			alg.put(pe, 1.);
			alg.put(ge, 1.);
			alg.put(sm, 1.);
			OverallFitness of = new OverallFitness(registry, alg);

			TreeFitnessAbstract ofDeepClone = TreeFitnessAbstract.deepClone(of);

			OverallFitness of2 = (OverallFitness) ofDeepClone;

			if (of.getEvaluators().size() != of2.getEvaluators().size()) {
				System.out.println("Incorrect cloning detected");
			}
		}

	}
}
