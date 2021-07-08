package org.processmining.plugins.etm.factory;

import java.util.Random;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.mining.MiningParameters;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMi;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLogImpl;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.experiments.StandardLogs;
import org.processmining.plugins.etm.model.narytree.conversion.ProcessTreeToNAryTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.processtree.ProcessTree;

/**
 * Class that allows calling the Inductive Tree Miner (by S. Leemans) for
 * process tree creation.
 * 
 * @author jbuijs
 * 
 */
public class InductiveMinerWrapper extends TreeFactoryAbstract {

	private IMLog filteredLog;

	public static void main(String[] args) {

		//		XLog log = BPMTestLogs.createDefaultLog();
		XLog log = StandardLogs.createDefaultLogWithNoise();
		CentralRegistry reg = new CentralRegistry(log, new Random());

		InductiveMinerWrapper IMW = new InductiveMinerWrapper(reg);

		StandardLogs.toFile(log,
				"C://Users//jbuijs//Documents//PhD//Projects//Small things//Simple test logs//000RunEx-Default-Noise");

		int nrTries = 15;
		for (int i = 0; i < nrTries; i++) {
			ProbProcessArrayTree tree = IMW.generateRandomCandidate(reg.getRandom());
			System.out.println("Tree " + i + ": " + TreeUtils.toString(tree, reg.getEventClasses()));
		}
	}

	public InductiveMinerWrapper(CentralRegistry registry) {
		super(registry);

		XLog log = registry.getLog();
		filteredLog = new IMLogImpl(log, registry.getEventClasses().getClassifier(), MiningParameters.getDefaultLifeCycleClassifier());
	}

	public ProbProcessArrayTree generateRandomCandidate(Random rng) {
		return callInductiveTreeMiner(registry, filteredLog);
	}

	/**
	 * Calls the inductive tree miner with random parameters (but never using
	 * SAT)
	 * 
	 * @param registry
	 * @return
	 */
	public static ProbProcessArrayTree callInductiveTreeMiner(CentralRegistry registry, IMLog filteredLog) {

		//		Miner miner = new Miner();

		MiningParametersIMi parameters = constructInductiveMinerParameters(registry.getEventClasses().getClassifier(),
				registry.getRandom());

		//TODO filter log to only include selected event classes (if this is not all)

		ProcessTree pt = IMProcessTree.mineProcessTree(filteredLog, parameters);

		ProcessTreeToNAryTree convertor = new ProcessTreeToNAryTree(registry.getEventClasses());
		ProbProcessArrayTree tree = convertor.convert(pt);

		return tree;
	}

	public static MiningParametersIMi constructInductiveMinerParameters(XEventClassifier classifier, Random random) {
		MiningParametersIMi parameters = new MiningParametersIMi();

		float incompleteThreshold = random.nextFloat();
		float noiseThreshold = random.nextFloat();
		boolean useExhaustiveKSuccessor = false;
		boolean useSAT = false; //SAT can be slooow :)

		parameters.setClassifier(classifier);
		parameters.setDebug(false);
		parameters.setIncompleteThreshold(incompleteThreshold);
		parameters.setNoiseThreshold(noiseThreshold);

		System.out.println("Parameter settings: incompleteThreshold: " + incompleteThreshold + " noiseThreshold: "
				+ noiseThreshold + " use exhaustive K successor: " + useExhaustiveKSuccessor + " use SAT: " + useSAT);

		return parameters;
	}

	/*-
	public void bla(){
		ExecutorService executor = Executors.newSingleThreadExecutor();
	    Future<String> future = executor.submit(new InductiveMinerWrapper(registry));

	    try {
	        System.out.println("Started..");
	        System.out.println(future.get(3, TimeUnit.SECONDS));
	        System.out.println("Finished!");
	    } catch (TimeoutException e) {
	        System.out.println("Terminated!");
	    }

	    executor.shutdownNow();
	}/**/

}
