package org.processmining.plugins.etm.ui.plugins;

import java.util.Random;

import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitness;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.parameters.ETMParamFactory;

public class TestReuseAlignWithLog {
//			private static final String parentString = "AND( AND( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , LEAF: D+complete ) ";
//			
//			private static final String childString = "AND( SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , LEAF: D+complete ) ";
//		
//			private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "C", "D", "E" } },
//					new int[] { 1 });
//		
//			private static final int pointOfChange = 1;
	@Plugin(name = "Reuse Alignments With Log", parameterLabels = { "Event Log", }, returnLabels = { "Fitness" }, returnTypes = {
			TreeFitness.class }, userAccessible = true, handlesCancel = true, categories = {
					PluginCategory.ConformanceChecking }, keywords = { "ETM", "Process Tree", "Quality", "Precision",
							"Replay fitness", "Generalization", "Simplicity" })
	@UITopiaVariant(uiLabel = "Reuse Alignments With log", affiliation = "Eindhoven University of Technology", author = "B. Vazquez-Barreiros", email = "bvazquez@tue.nl", pack = "EvolutionaryTreeMiner")
	public static TreeFitness reuseAlignmentWithLog(PluginContext context, XLog eventlog) {

		CentralRegistry registry = new CentralRegistry(eventlog, new Random());
		registry.updateEventClassifier(XLogInfoImpl.STANDARD_CLASSIFIER);
		registry.updateLogDerived();
		ProbProcessArrayTree parent = TreeUtils.fromString(parentString, registry.getEventClasses());
		ProbProcessArrayTree child = TreeUtils.fromString(childString, registry.getEventClasses());
		//		NAryTree child2 = TreeUtils.fromString(child2String, registry.getEventClasses());
		System.out.println(parent);
		System.out.println(child);
		//		System.out.println(child2);
		OverallFitness of = ETMParamFactory.createStandardOverallFitness(registry);
		of.getFitness(parent, null);

		registry.saveHistory(child, parent, pointOfChange, TypesOfTreeChange.OTHER);

		of.getFitness(child, null);

		//		registry.saveHistory(child2, child, 5, NAryTreeHistory.TypesOfChange.OTHER);
		//		
		//		 of.getFitness(child2, null);

		TreeFitness fitnessParent = registry.getFitness(parent);
		TreeFitness fitnessChild = registry.getFitness(child);
		//		TreeFitness fitnessChild2 = registry.getFitness(child2);
		System.out.println(fitnessParent);
		System.out.println(fitnessChild);
		//		System.out.println(fitnessChild2);
		return fitnessParent;
	}
	
	private static final String parentString =     "SEQ( LEAF: S+complete , LEAF: p+complete , OR( LEAF: r+complete , LEAF: a+complete ) , LEAF: f+complete , LEAF: h+complete , LEAF: g+complete , XOR( OR( LEAF: n+complete , LEAF: o+complete ) , XOR( LEAF: k+complete , LEAF: b+complete ) ) , LEAF: r+complete , LEAF: t+complete , LEAF: u+complete , OR( XOR( OR( LEAF: c+complete , LEAF: j+complete ) , OR( LEAF: s+complete , LEAF: o+complete ) ) , OR( LEAF: v+complete , XOR( LEAF: k+complete , LEAF: n+complete ) ) ) , LEAF: m+complete , LEAF: b+complete , LEAF: c+complete , XOR( LEAF: d+complete , OR( LEAF: e+complete , LEAF: v+complete ) ) , LEAF: j+complete , LEAF: m+complete , LEAF: f+complete , LEAF: g+complete , LEAF: i+complete , LEAF: h+complete , LEAF: g+complete , LEAF: i+complete , LEAF: g+complete , LEAF: i+complete , LEAF: g+complete , LEAF: k+complete , LEAF: E+complete )";
	//
		private static final String childString =  "SEQ( LEAF: S+complete , LEAF: p+complete , XOR( LEAF: r+complete , LEAF: a+complete ) , LEAF: f+complete , LEAF: h+complete , LEAF: g+complete , XOR( OR( LEAF: n+complete , LEAF: o+complete ) , XOR( LEAF: k+complete , LEAF: b+complete ) ) , LEAF: r+complete , LEAF: t+complete , LEAF: u+complete , OR( XOR( OR( LEAF: c+complete , LEAF: j+complete ) , OR( LEAF: s+complete , LEAF: o+complete ) ) , OR( LEAF: v+complete , XOR( LEAF: k+complete , LEAF: n+complete ) ) ) , LEAF: m+complete , LEAF: b+complete , LEAF: c+complete , XOR( LEAF: d+complete , OR( LEAF: e+complete , LEAF: v+complete ) ) , LEAF: j+complete , LEAF: m+complete , LEAF: f+complete , LEAF: g+complete , LEAF: i+complete , LEAF: h+complete , LEAF: g+complete , LEAF: i+complete , LEAF: g+complete , LEAF: i+complete , LEAF: g+complete , LEAF: k+complete , LEAF: E+complete ) ";

		private static final int pointOfChange = 3;
		
	
}

// with everything 2
//private static final String parentString = "AND( AND( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , AND( LEAF: D+complete , LEAF: E+complete ) ) ";
////
//private static final String childString = "AND( SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , AND( LEAF: D+complete , LEAF: E+complete ) ) ";
//
////private static final String child2String = "AND( SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: E+complete ) ) ";
////
//private static final XLog eventlog = LogCreator.createLog(new String[][] { { "D", "E", "A", "B", "C" } },
//		new int[] { 1 });
//
//private static final int pointOfChange = 1;
////
//@Plugin(name = "Reuse Alignments", parameterLabels = {}, returnLabels = { "Fitness" }, returnTypes = {
//		TreeFitness.class }, userAccessible = true, handlesCancel = true, categories = {
//				PluginCategory.ConformanceChecking }, keywords = { "ETM", "Process Tree", "Quality", "Precision",
//						"Replay fitness", "Generalization", "Simplicity" })
//@UITopiaVariant(uiLabel = "Reuse Alignments", affiliation = "Eindhoven University of Technology", author = "B. Vazquez-Barreiros", email = "bvazquez@tue.nl", pack = "EvolutionaryTreeMiner")
//public static TreeFitness reuseAlignment(PluginContext context) {
//
//	CentralRegistry registry = new CentralRegistry(eventlog, new Random());
//	registry.updateEventClassifier(XLogInfoImpl.STANDARD_CLASSIFIER);
//	registry.updateLogDerived();
//	NAryTree parent = TreeUtils.fromString(parentString, registry.getEventClasses());
//	NAryTree child = TreeUtils.fromString(childString, registry.getEventClasses());
//	NAryTree child2 = TreeUtils.fromString(child2String, registry.getEventClasses());
//	System.out.println(parent);
//	System.out.println(child);
//	System.out.println(child2);
//	OverallFitness of = ETMParamFactory.createStandardOverallFitness(registry);
//
//	of.getFitness(parent, null);
//
//	registry.saveHistory(child, parent, pointOfChange, NAryTreeHistory.TypesOfChange.OTHER);
//
//	of.getFitness(child, null);
//	
////	registry.saveHistory(child2, child, 5, NAryTreeHistory.TypesOfChange.OTHER);
////	
////	 of.getFitness(child2, null);
//
//	TreeFitness fitnessParent = registry.getFitness(parent);
//	TreeFitness fitnessChild = registry.getFitness(child);
////	TreeFitness fitnessChild2 = registry.getFitness(child2);
//	System.out.println(fitnessParent);
//	System.out.println(fitnessChild);
////	System.out.println(fitnessChild2);
//	return fitnessParent;
//}

//	private static final String parentString = "LOOP( SEQ( XOR( SEQ( LEAF: A+complete , LEAF: B+complete ) , LEAF: Z+complete ) , LEAF: P+complete ) , OR( LEAF: C+complete , LEAF: M+complete ) , LEAF: D+complete ) ";
//
//private static final String childString = "LOOP( AND( LEAF: A+complete , LEAF: B+complete ) , LEAF: C+complete , LEAF: D+complete ) ";
//
//private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "B", "C", "A", "B", "D" }, { "A", "B", "Z", "P", "C", "A", "B", "D" } }, new int[] { 1, 1 });
//
//private static final int pointOfChange = 1;
