package org.processmining.plugins.etm.tests;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Random;

import nl.tue.astar.AStarThread.Canceller;

import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.metrics.ConfigurationFitness;
//import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.fitness.metrics.Generalization;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.fitness.metrics.PrecisionEscEdges;
import org.processmining.plugins.etm.fitness.metrics.SimplicityUselessNodes;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.mutation.mutators.ConfigurationMutator;

public class ConfigurationTests {

	public static void main(String[] args) {
		//		testConfig();
		//		testTreeFunctions();
		testConfigurationApplication();
	}

	public static void testConfig() {
		XLog log1 = null, log2 = null;
		try {
			//log1 = LogCreator.createInterleavedLog(6, "A", "B", "C");
			log1 = LogCreator.createLog(new String[][] { { "A", "B", "C", "D", "E", "F" } });
			//log2 = LogCreator.createInterleavedLog(24, "A", "B", "C", "D");
			log2 = LogCreator.createInterleavedLog(720, "A", "B", "C", "D", "E", "F");
			//log2 = LogCreator.createInterleavedLog(2, "A", "B");
		} catch (Exception e) {
			e.printStackTrace();
		}

		XLog[] logs = new XLog[] { log1, log2 };

		CentralRegistryConfigurable registry = new CentralRegistryConfigurable(XLogInfoImpl.STANDARD_CLASSIFIER,
				new Random(), logs);

		Canceller c = new Canceller() {

			public boolean isCancelled() {
				return false;
			}
		};

		ProbProcessArrayTree tree = null;

		/*-* /
		//Tree that works
		 tree = TreeUtils.fromString(
				"OR( LEAF: C+complete , SEQ( LEAF: B+complete ) , SEQ( LEAF: A+complete ) )", registry.getLogInfo()
						.getEventClasses());
		tree.addConfiguration(new Configuration(new boolean[] { false, false, false, false, false, false },
				new boolean[] { false, false, true, false, false, false }));
		tree.addConfiguration(new Configuration(new boolean[] { false, false, false, false, false, false },
				new boolean[] { false, false, false, false, false, false }));
		/**/

		/*-* /
		//Incorrect fitness with these configurations
		tree = TreeUtils.fromString("AND( LEAF: A+complete , LEAF: B+complete )", registry.getLogInfo()
				.getEventClasses());
		tree.addConfiguration(new Configuration(new byte[] { Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED }));
		tree.addConfiguration(new Configuration(new byte[] { Configuration.REVSEQ, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED }));
		/**/

		/*-* /
		//Assertion fails in stateBuilder line 447 for log 6AND and 6SEQ
		//Joos 26-2-2013 11:05
		tree = TreeUtils
				.fromString(
						"LOOP( OR( SEQ( AND( LEAF: C+complete , XOR( OR( XOR( LEAF: D+complete ) ) ) , SEQ( LEAF: tau ) , AND( SEQ( XOR( SEQ( LEAF: E+complete ) ) ) ) ) , LEAF: E+complete ) ) , LEAF: tau , LEAF: tau )",
						registry.getLogInfo().getEventClasses());
		tree.addConfiguration(new Configuration(new byte[] { Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.BLOCKED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.BLOCKED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED }));
		tree.addConfiguration(new Configuration(new byte[] { Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED }));
		/**/

		/*-*/
		//Joos 5-3-2013 9:40 assertion failed in SEQ
		tree = TreeUtils.fromString("LOOP( SEQ( OR( LEAF: D+complete , LEAF: C+complete ) , LEAF: B+complete "
				+ " ) , LEAF: tau , LEAF: tau )", registry.getLogInfo().getEventClasses());
		tree.addConfiguration(new Configuration(new byte[] { Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.HIDDEN, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED,
				Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED, Configuration.NOTCONFIGURED }));
		//[[2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], [0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]]

		/**/

		assert tree.isConsistent();
		System.out.println("Tree: " + TreeUtils.toString(tree, registry.getEventClasses()));
		System.out.println("Configurations:");
		for (int i = 0; i < tree.getNumberOfConfigurations(); i++) {
			System.out.println(i + " Conf: " + tree.getConfiguration(i));
		}

		TreeFitnessAbstract[] fitnesses = new TreeFitnessAbstract[logs.length];
		for (int i = 0; i < logs.length; i++) {
//			FitnessReplay fr = new FitnessReplay(registry.getRegistry(i), c, -1, 100);
			PrecisionEscEdges pe = new PrecisionEscEdges(registry);
			Generalization ge = new Generalization(registry.getRegistry(i));
			//			SimplicityMixed su = new SimplicityMixed();
			SimplicityUselessNodes su = new SimplicityUselessNodes();

			LinkedHashMap<TreeFitnessAbstract, Double> alg = new LinkedHashMap<TreeFitnessAbstract, Double>();
//			alg.put(fr, 10.);
			alg.put(pe, 1.);
			alg.put(ge, .1);
			alg.put(su, 1.);

			OverallFitness of = new OverallFitness(registry.getRegistry(i), alg);
			fitnesses[i] = of;
		}

		ConfigurationFitness of = new ConfigurationFitness(registry, .0001, false, fitnesses);

		long start = new Date().getTime();
		of.getFitness(tree, null);
		long dur = new Date().getTime() - start;
		System.out.println("Duration: " + dur);

		System.out.println("Tree: " + TreeUtils.toString(tree, registry.getEventClasses()));

		System.out.println("Fitness: " + registry.getFitness(tree).fitnessValues);

		System.out.println(registry.getFitness(tree).toString());
		for (int i = 0; i < registry.getNrLogs(); i++) {
			System.out.println(i + ": " + registry.getRegistry(i).getFitness(tree.applyConfiguration(i)).toString());
		}

	}

	@SuppressWarnings("unused")
	private static void testTreeFunctions() {
		XLog log1 = null;
		try {
			log1 = LogCreator.createInterleavedLog(6, "A", "B", "C");
		} catch (Exception e) {
			e.printStackTrace();
		}

		XLog[] logs = new XLog[] { log1 };

		CentralRegistryConfigurable registry = new CentralRegistryConfigurable(XLogInfoImpl.STANDARD_CLASSIFIER,
				new Random(), logs);

		ProbProcessArrayTreeImpl tree = (ProbProcessArrayTreeImpl) TreeUtils
				.fromString("AND( SEQ( LEAF: 0 , LEAF: 1 ) , XOR( LEAF: 0 , LEAF: 1 ) )");
		tree.addConfiguration(new Configuration(new boolean[] { true, false, false, false, false, true, true },
				new boolean[] { true, false, false, false, false, true, true }));

		System.out.println("Tree: " + TreeUtils.toString(tree));
		System.out.println("Configurations:");
		for (int i = 0; i < tree.getNumberOfConfigurations(); i++) {
			System.out.println(i + " Configuration: " + tree.getConfiguration(i));
		}

		//tree.setConfiguration(0, 6, true,false);
		ConfigurationMutator mut = new ConfigurationMutator(registry);
		tree = (ProbProcessArrayTreeImpl) mut.mutate(tree);

		System.out.println("Tree: " + TreeUtils.toString(tree));
		System.out.println("Configurations:");
		for (int i = 0; i < tree.getNumberOfConfigurations(); i++) {
			System.out.println(i + " Configuration: " + tree.getConfiguration(i));
		}
	}

	public static void testConfigurationApplication() {
		XLog log = LogCreator.createLog(new String[][] {{"A1","A2","B","C","D"}});
		XLogInfo info = XLogInfoFactory.createLogInfo(log, XLogInfoImpl.NAME_CLASSIFIER);
		
		String treeString = "LOOP( OR( LEAF: A1 , LEAF: A2 ) , LEAF: B , AND( LEAF: C , LEAF: D ) ) [ [>, x, -, B, H, >, -, -] ]";
		ProbProcessArrayTree tree = TreeUtils.fromString(treeString, info.getEventClasses());
		
		System.out.println("tree: "+TreeUtils.toString(tree,info.getEventClasses()));
		
		ProbProcessArrayTree treeConf = tree.applyConfiguration(0);
		
		System.out.println("tree: " + TreeUtils.toString(treeConf, info.getEventClasses()));
	}
}
