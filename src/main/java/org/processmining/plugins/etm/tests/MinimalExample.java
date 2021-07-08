package org.processmining.plugins.etm.tests;

import java.io.File;
import java.io.IOException;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.ETMPareto;
import org.processmining.plugins.etm.experiments.StandardLogs;
import org.processmining.plugins.etm.fitness.TreeFitness;
import org.processmining.plugins.etm.logging.EvolutionLogger;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.model.serialization.ParetoFrontExport;
import org.processmining.plugins.etm.parameters.ETMParam;
import org.processmining.plugins.etm.parameters.ETMParamFactory;
import org.processmining.plugins.etm.parameters.ETMParamPareto;
import org.processmining.plugins.etm.ui.plugins.ETMwithoutGUI;
import org.processmining.plugins.etm.utils.LogUtils;
import org.processmining.processtree.ProcessTree;

public class MinimalExample {

	/**
	 * Minimal code to run the ETMb to discover a Pareto front. To run this I
	 * use the following 'command line arguments':
	 * "-ea -Djava.library.path=.//lib;.//locallib//win64;.//stdlib -Xmx4G  -XX:+UseCompressedOops"
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		//		ETMp();
		realMinimal();
	}

	private static void realMinimal() {
		//		XLog log = StandardLogs.createDefaultLog();
		XLog log = LogUtils
				.loadFromFile("C:\\Users\\jbuijs\\Documents\\TUe\\Random stuff\\000Event Logs\\BPI12_financial_log.xes.gz"); //BPI12 log
		long seed = 1;

		ETMParam param = ETMParamFactory.buildStandardParam(log, null);
		param.getCentralRegistry().getRandom().setSeed(seed);
		ProbProcessArrayTree[] trees = new ProbProcessArrayTree[] { TreeUtils.fromString("LEAF: tau") };
		param.setSeed(trees);
		param.addTerminationConditionMaxDuration(1000 * 60 * 1);
		//		param.addTerminationConditionMaxGen(10);
		param.setLogModulo(1);
		//		param.setMaxThreads(1);

		EvolutionLogger<ProbProcessArrayTree> logger = new EvolutionLogger<ProbProcessArrayTree>(null, param.getCentralRegistry(), false, 1);
		param.addEvolutionObserver(logger);

		ProcessTree pt = ETMwithoutGUI.minePTWithParameters(null, log, MiningParameters.getDefaultClassifier(), param);

		@SuppressWarnings("unused")
		CentralRegistry cr = param.getCentralRegistry();
		TreeFitness f = cr.getFitness(TreeUtils.fromString("LEAF: tau"));
		f.behaviorCounter.getAlignment(cr.getaStarAlgorithm().traceIterator().next());
		//f = cr.getFitness(TreeUtils.fromString("SEQ( LEAF: A , LEAF: B , LEAF: C , LEAF: D , LEAF: E , LEAF: G )"));

		System.out.println("FINISHED" + pt.toString());
	}

	public static void ETMp() throws IOException {
		//We instantiate one of our default event logs, an external event log can also be loaded.
		XLog eventlog = StandardLogs.createDefaultLogWithNoise();

		//Initialize all parameters:
		int popSize = 10; //population size
		int eliteSize = 2; //elite size
		int nrRandomTrees = 2; //nr of random tree to create each generation
		double crossOverChance = 0.1; //chance applying crossover
		double chanceOfRandomMutation = 0.5; //change of applying a random mutation operator
		boolean preventDuplicates = true; //prevent duplicate process trees within a population (after change operations are applied)
		int maxGen = 10; //maximum number of generation to run
		double targetFitness = 1; //target fitness to stop at when reached
		double frWeight = 10; //weight for replay fitness
		double maxF = 0.6; //stop alignment calculation for trees with a value below 0.6
		double maxFTime = 10; //allow maximum 10 seconds per trace alignment
		double peWeight = 5; //weight for precision
		double geWeight = 0.1; //weight for generalization
		double suWeight = 1; //weight for simplicity
		//the first null parameter is a ProM context, which does not need to be provided
		//the second null parameter is an array of seed process trees, which we do not provide here
		//the last `0' is the similarity weight
		ETMParamPareto etmParam = ETMParamFactory.buildETMParamPareto(eventlog, null, popSize, eliteSize,
				nrRandomTrees, crossOverChance, chanceOfRandomMutation, preventDuplicates, maxGen, targetFitness,
				frWeight, maxF, maxFTime, peWeight, geWeight, suWeight, null, 0);

		ETMPareto etm = new ETMPareto(etmParam); //Instantiate the ETM algorithm
		etm.run(); //Now actually run the ETM, this might take a while

		//Extract the resulting Pareto front
		ParetoFront paretoFront = etm.getResult();

		System.out.println("We have discovered a Pareto front of size " + paretoFront.size()); //output the size
		ParetoFrontExport.export(paretoFront, new File("myParetoFront.PTPareto")); //and write to file
	}

}
