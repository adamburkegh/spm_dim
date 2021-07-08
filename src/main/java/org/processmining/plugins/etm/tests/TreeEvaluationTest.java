/**
 * 
 */
package org.processmining.plugins.etm.tests;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.set.TIntSet;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Random;

import nl.tue.astar.AStarThread.Canceller;
import nl.tue.astar.Trace;

import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.boudewijn.treebasedreplay.astar.ModelPrefix;
import org.processmining.plugins.boudewijn.treebasedreplay.astar.TreeMarkingVisit;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.experiments.StandardLogs;
import org.processmining.plugins.etm.fitness.BehaviorCounter;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.fitness.metrics.Generalization;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.fitness.metrics.PrecisionEscEdges;
import org.processmining.plugins.etm.fitness.metrics.PrecisionEscEdgesImproved;
import org.processmining.plugins.etm.fitness.metrics.SimplicityUselessNodes;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeReplayer;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.parameters.ETMParamFactory;
import org.processmining.plugins.etm.termination.ProMCancelTerminationCondition;
import org.processmining.plugins.etm.utils.LogUtils;

/**
 * @author jbuijs
 * 
 */
public class TreeEvaluationTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		evaluateBasicTree();
		//		evaluateThesisQualityExamples();
	}

	public static void evaluateBasicTree() {
		//String treeString = "LOOP( LEAF: A , LEAF: B , LEAF: C )";
		//String treeString = "REVSEQ( LEAF: C+complete , LOOP( LEAF: tau , SEQ( LEAF: A+complete , LEAF: B+complete ) , LEAF: A+complete ) ) ";
		//		String treeString = "XOR( LEAF: IL4OIS Intake - Incomplete , LEAF: IL4OIS Order Entry - Special , XOR( SEQ( XOR( LEAF: tau , LEAF: IL4OIS Document - Split ) , XOR( AND( SEQ( LEAF: IL4OIS Document - Sort , XOR( LEAF: tau , LEAF: IL4OIS PV1 - IV ) ) , XOR( LEAF: tau , LEAF: IL4OIS Order Entry - IV ) ) , LEAF: IL4OIS Order Entry - Labs ) ) , LEAF: tau , LEAF: tau , LEAF: tau ) ) ";
		//String treeString = "XOR( LEAF: A , XOR( LEAF: tau , SEQ( LEAF: A , LEAF: B ) ) )";
		//Thesis Fr example (5.4.1) tree:
		//		String treeString = "SEQ( LEAF: A , LEAF: B ,  LEAF: C ,  LEAF: D ,  LEAF: E ,  LEAF: G )";
		//		treeString = "SEQ( LEAF: A_SUBMITTED , LEAF: A_PARTLYSUBMITTED , LOOP( SEQ( XOR( LEAF: W_Afhandelen leads , LEAF: A_PREACCEPTED , LEAF: W_Beoordelen fraude , LEAF: A_DECLINED ) , LOOP( XOR( LEAF: W_Completeren aanvraag , LEAF: W_Nabellen offertes , SEQ( XOR( LEAF: tau , LEAF: A_ACCEPTED , LEAF: A_CANCELLED ) , AND( LEAF: O_SELECTED , XOR( LEAF: O_CANCELLED , LEAF: A_FINALIZED ) ) , LEAF: O_CREATED , LEAF: O_SENT ) , LEAF: W_Wijzigen contractgegevens , AND( LEAF: A_REGISTERED , LEAF: O_ACCEPTED , LEAF: A_APPROVED , LEAF: A_ACTIVATED ) , LEAF: W_Valideren aanvraag , LEAF: W_Nabellen incomplete dossiers , LEAF: tau , LEAF: O_DECLINED ) , LEAF: tau , LEAF: tau ) ) , LEAF: tau , LEAF: tau ) ) ";
		//		treeString = "LEAF: A_SUBMITTED ";

		//		treeString = "LOOP( SEQ( XOR( LEAF: A , LEAF: C ) , AND( XOR( LEAF: tau , LEAF: D ) , XOR( LEAF: tau , LEAF: B ) ) ) , LEAF: tau , LEAF: tau )";

		XFactoryRegistry.instance().setCurrentDefault(new XFactoryNaiveImpl());

		/*
		 * JB 2016-02-18 two trees that break the replayer
		 */
		String treeString = "AND( AND( OR( SEQ( LEAF: 4 , LEAF: tau , LEAF: 2 , LEAF: 1 ) , LEAF: 3 , LEAF: 2 ) ) , LEAF: 2 , LEAF: 4 , LEAF: 6 ) [ ]";
		//treeString = "AND( LEAF: 1 , SEQ( SEQ( LEAF: 5 , LEAF: 2 , LEAF: 2 , LEAF: 2 ) , LEAF: 1 , LEAF: 3 , LEAF: 0 ) , LEAF: tau , LEAF: tau ) [ ]";

		//		String[][] loop3x = new String[][] { { "A", "C" }, { "A", "B", "A", "C" }, { "A", "B", "A", "B", "A", "C" } };
		//String[][] problemLog = new String[][] { { "Z" }, {"A","B"} };
		//String[][] logStr = new String[][]{{"A","C"}};
		//XLog log = LogCreator.createLog(logStr);

		//Thesis Fr example (5.4.1) log:
		XLog log = LogCreator
				.createLog(new String[][] { { "A", "B", "C", "D", "E", "G" }, { "A", "D", "B", "C", "E", "G" },
						{ "A", "B", "D", "C", "E", "F", "E", "G" } }, new int[] { 80, 5, 15 });

		//log = loadExternalLog();

		log = StandardLogs.createDefaultLogWithNoise();

		XLogInfo info = XLogInfoImpl.create(log, XLogInfoImpl.NAME_CLASSIFIER);

		System.out.println("XLogInfo classes before:" + LogUtils.eventClassesToString(info.getEventClasses()));

		CentralRegistry registry = new CentralRegistry(log, XLogInfoImpl.NAME_CLASSIFIER, new Random(1));

		//		System.out.println("XLogInfo classes after:" + LogUtils.eventClassesToString(info.getEventClasses()));

		//		System.out.println("Registry classes after:" + LogUtils.eventClassesToString(registry.getEventClasses()));

		ProbProcessArrayTree tree = TreeUtils.fromString(treeString, registry.getEventClasses());

		/*
		 * index out of bounds test
		 */

		System.out.println(tree.toInternalString());
		System.out.println(TreeUtils.toString(tree, registry.getEventClasses()));
		System.out.println(tree.toString());

		Canceller c = ProMCancelTerminationCondition.buildDummyCanceller();
		FitnessReplay fr = new FitnessReplay(registry, c);
		fr.setDetailedAlignmentInfoEnabled(true);

		fr.getFitness(tree, null);

		PrecisionEscEdges pe = new PrecisionEscEdges(registry);
		@SuppressWarnings("unused")
		PrecisionEscEdgesImproved peImp = new PrecisionEscEdgesImproved(registry);

		Generalization ge = new Generalization(registry);
		SimplicityUselessNodes su = new SimplicityUselessNodes();

		BehaviorCounter behC = registry.getFitness(tree).behaviorCounter;

		System.out.println("m2mm: " + behC.getMarking2ModelMove());
		System.out.println("m2vc: " + behC.getMarking2VisitCount());

		System.out.println("Fr: " + fr.getFitness(tree, null));
		System.out.println("Pe: " + pe.getFitness(tree, null));
		//System.out.println("Pi: " + peImp.getFitness(tree, null));
		System.out.println("Ge: " + ge.getFitness(tree, null));
		System.out.println("Su: " + su.getFitness(tree, null));

		System.out.println("minModelCost: " + behC.getMinModelCost());
		System.out.println("sync moves: " + Arrays.toString(behC.getSyncMoveCount()));
		System.out.println("unsync moves:" + Arrays.toString(behC.getASyncMoveCount()));

		NAryTreeReplayer<?, ?, ?> replayer = fr.setupReplayer(tree, registry.getEmptyAStarAlgorithm(),
				new HashMap<TreeMarkingVisit<ModelPrefix>, TIntSet>(0),
				new TObjectIntHashMap<TreeMarkingVisit<ModelPrefix>>(0), new int[tree.size()], new int[tree.size()],
				new int[tree.size()], new int[tree.size()], null);

		System.out.println("Alignments: ");
		for (Entry<Trace, TreeRecord> entry : behC.getAlignments().entrySet()) {
			System.out.println(" " + entry.getKey() + ": ");
			TreeRecord.printRecord(replayer.getDelegate(), entry.getKey(), entry.getValue());
		}

	}

	/**
	 * Evaluates the precision example of Joos' PhD thesis, Chapter 5, Section
	 * 5.5.1
	 */
	public static void evaluateThesisQualityExamples() {

		String[][] logString = new String[][] { { "A", "B", "C", "D", "E", "G" },
				{ "A", "B", "D", "C", "E", "F", "E", "G" }, { "A", "D", "B", "C", "E", "G" } };
		XLog log = LogCreator.createLog(logString, new int[] { 80, 5, 15 });

		CentralRegistry registry = new CentralRegistry(log, XLogInfoImpl.NAME_CLASSIFIER, new Random(1));

		String treeStringFr = "SEQ( LEAF: A , LEAF: B , LEAF: C , LEAF: D , LEAF: E , LEAF: G ) ) ";
		String treeStringPe = "SEQ( LEAF: A , AND( LEAF: B , LEAF: C , LEAF: D ) , LOOP( LEAF: E , LEAF: F , LEAF: G ) ) ";
		//TODO: update Ge tree
		String treeStringGe = "SEQ( LEAF: A , XOR( SEQ( LEAF: B , XOR( SEQ( LEAF: D , LEAF: C ) , SEQ( LEAF: C , LEAF: D ) ) ) , SEQ( LEAF: D , LEAF: B , LEAF: C ) ) , LEAF: E , LEAF: G ) ";

		ProbProcessArrayTree treeFr = TreeUtils.fromString(treeStringFr, registry.getEventClasses());
		ProbProcessArrayTree treePe = TreeUtils.fromString(treeStringPe, registry.getEventClasses());
		ProbProcessArrayTree treeGe = TreeUtils.fromString(treeStringGe, registry.getEventClasses());

		ProbProcessArrayTree[] trees = new ProbProcessArrayTree[] { treeFr, treePe, treeGe };
		//NAryTree tree = TreeUtils.fromString(treeString, registry.getEventClasses());

		TreeFitnessAbstract of = ETMParamFactory.createStandardOverallFitness(registry);

		/*
		 * Fr tree
		 */

		System.out.println("---");
		System.out.println("Fitness Replay");
		System.out.println("---");
		of.getFitness(treeFr, null);

		System.out.println(TreeUtils.toString(treeFr, registry.getEventClasses()));
		System.out.println(registry.getFitness(treeFr));

		BehaviorCounter behCFr = registry.getFitness(treeFr).behaviorCounter;

		System.out.println("Fr: " + registry.getFitness(treeFr).fitnessValues.get(FitnessReplay.info));

		System.out.println("minModelCost: " + behCFr.getMinModelCost());
		System.out.println("sync moves: " + Arrays.toString(behCFr.getSyncMoveCount()));
		System.out.println("unsync moves:" + Arrays.toString(behCFr.getASyncMoveCount()));

		FitnessReplay fr = null;
		for (TreeFitnessAbstract eval : ((OverallFitness) of).getEvaluators().keySet()) {
			if (eval instanceof FitnessReplay) {
				fr = (FitnessReplay) eval;
			}
		}

		NAryTreeReplayer<?, ?, ?> replayer = fr.setupReplayer(treeFr, registry.getEmptyAStarAlgorithm(),
				new HashMap<TreeMarkingVisit<ModelPrefix>, TIntSet>(0),
				new TObjectIntHashMap<TreeMarkingVisit<ModelPrefix>>(0), new int[treeFr.size()],
				new int[treeFr.size()], new int[treeFr.size()], new int[treeFr.size()], null);

		System.out.println("Alignments: ");
		for (Entry<Trace, TreeRecord> entry : behCFr.getAlignments().entrySet()) {
			System.out.println(" " + entry.getKey() + ": ");
			TreeRecord.printRecord(replayer.getDelegate(), entry.getKey(), entry.getValue());
		}

		/*
		 * Pe tree
		 */

		System.out.println("---");
		System.out.println("Precision");
		System.out.println("---");

		of.getFitness(treePe, null);

		System.out.println(TreeUtils.toString(treePe, registry.getEventClasses()));
		System.out.println(registry.getFitness(treePe));

		BehaviorCounter behCPe = registry.getFitness(treePe).behaviorCounter;

		System.out.println("Pe: " + registry.getFitness(treePe).fitnessValues.get(PrecisionEscEdges.info));

		System.out.println("m2mm: " + behCPe.getMarking2ModelMove());
		System.out.println("m2vc: " + behCPe.getMarking2VisitCount());

		/*
		 * Ge tree
		 */

		System.out.println("---");
		System.out.println("Generalization");
		System.out.println("---");

		of.getFitness(treeGe, null);

		System.out.println(TreeUtils.toString(treeGe, registry.getEventClasses()));
		System.out.println(registry.getFitness(treeGe));

		BehaviorCounter behCGe = registry.getFitness(treeGe).behaviorCounter;

		//System.out.println("Pi: " + peImp.getFitness(tree, null));

		System.out.println("Ge sync moves: " + Arrays.toString(behCGe.getSyncMoveCount()));
		System.out.println("Ge nonSync moves: " + Arrays.toString(behCGe.getASyncMoveCount()));

	}

	public static XLog loadExternalLog() {
		XUniversalParser parser = new XUniversalParser();
		try {
			return parser.parse(new File(
			//							"C:\\Users\\jbuijs\\Documents\\My Received Files\\IL-eventlog_from20120514-filtered IL4OIS.xes.gz"
					"C:\\Users\\jbuijs\\Desktop\\temp\\LogAndModels\\SynthLog.xes")).iterator().next(); //BPIC2012.xes.gz
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}
}
