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
import org.processmining.plugins.etm.model.narytree.test.LogCreator;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.parameters.ETMParamFactory;

public class TestReuseAlign {
//			private static final String parentString = "AND( AND( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , LEAF: D+complete ) ";
//			
//			private static final String childString = "AND( SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , LEAF: D+complete ) ";
//		
//			private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "C", "D", "E" } },
//					new int[] { 1 });
//		
//			private static final int pointOfChange = 1;
	@Plugin(name = "Reuse Alignments", parameterLabels = {}, returnLabels = { "Fitness" }, returnTypes = {
			TreeFitness.class }, userAccessible = true, handlesCancel = true, categories = {
					PluginCategory.ConformanceChecking }, keywords = { "ETM", "Process Tree", "Quality", "Precision",
							"Replay fitness", "Generalization", "Simplicity" })
	@UITopiaVariant(uiLabel = "Reuse Alignments", affiliation = "Eindhoven University of Technology", author = "B. Vazquez-Barreiros", email = "bvazquez@tue.nl", pack = "EvolutionaryTreeMiner")
	public static TreeFitness reuseAlignment(PluginContext context) {

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
	//	 AND or
//								private static final String parentString = "OR( AND( LEAF: a+complete , LEAF: b+complete , LEAF: c+complete ) , LEAF: j+complete ) ";
//								
//								private static final String childString = "OR( AND( LEAF: a+complete , LEAF: b+complete , XOR( LEAF: d+complete , LEAF: c+complete ) ) , LEAF: j+complete ) ";
//							
//								private static final XLog eventlog = LogCreator.createLog(new String[][] { { "a", "b", "c" }, { "a", "c", "b" },
//										{ "c", "a", "b" }, { "b", "a", "d" }, { "b", "d", "a" }, { "d", "b", "a" }, { "j" }, {} },
//										new int[] { 1, 1, 1, 1, 1, 1, 1, 1 });
//							
//								private static final int pointOfChange = 1;

	//	 LOOP with different optimal alignment
//									private static final String parentString = "AND( LEAF: x+complete , LOOP( XOR( LEAF: A+complete , LEAF: B+complete ) , LEAF: C+complete , LEAF: D+complete ) ) ";
//								
//									private static final String childString = "AND( LEAF: x+complete , LOOP( AND( LEAF: A+complete , LEAF: B+complete ) , LEAF: C+complete , LEAF: D+complete ) ) ";
//								
//									private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "B", "C", "A", "B", "D" } },
//											new int[] { 1 });
//								
//									private static final int pointOfChange = 2;

	//	
	//	How internal moves work.	

//								private static final String parentString = "SEQ( LEAF: k+complete ,  LOOP( SEQ( XOR( SEQ( LEAF: A+complete , LEAF: B+complete ) , LEAF: Z+complete ) , LEAF: P+complete ) , LEAF: C+complete , LEAF: D+complete ) )";
//								
//								private static final String childString = "SEQ( LEAF: k+complete ,  LOOP( AND( LEAF: A+complete , LEAF: B+complete ) , LEAF: C+complete , LEAF: D+complete ) )";
//							
//								private static final XLog eventlog = LogCreator.createLog(new String[][] { { "k", "A", "B", "C", "A", "B", "D" }, { "k" ,"A", "B", "Z", "P", "C", "A", "B", "D" } }, new int[] { 1, 1 });
//							
//								private static final int pointOfChange = 2;

	//  Long example not optimality
//												private static final String parentString = "OR( SEQ( LEAF: A+complete , LEAF: B+complete ) , LEAF: D+complete )";
//											
//												private static final String childString = "OR( LOOP( SEQ( LEAF: A+complete , LEAF: B+complete ) , LEAF: C+complete , LEAF: tau ) , LEAF: D+complete )";
//											
//												private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "B", "C", "D", "A", "B", "C", "A", "B" } }, new int[] { 1 });
//											
//												private static final int pointOfChange = 1;

	// PROBLEMATIC BUG RELATED (I)?
	//
	//									private static final String parentString = "SEQ( SEQ( LEAF: A+complete , LEAF: B+complete ) , SEQ( LEAF: D+complete , LEAF: E+complete ) ) " ;
	//								
	//									private static final String childString = "SEQ( AND( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: E+complete ) ) ";
	//								
	//									private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "C", "D", "B", "E" } }, new int[] { 1 });
	//									
	//									private static final int pointOfChange = 1;

	// PROBLEMATIC BUG RELATED (II)?
//							private static final String parentString = "SEQ( LEAF: X+complete , AND( SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: E+complete ) ) , LEAF: J+complete ) ";
//							
//							private static final String childString = "SEQ( LEAF: X+complete , AND( AND( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: E+complete ) ) , LEAF: J+complete ) ";
//						
//							private static final XLog eventlog = LogCreator.createLog(new String[][] { { "X", "A", "C", "D", "B", "E", "J" } }, new int[] { 1 });
//							
//							private static final int pointOfChange = 3;

	// Interesting 1: where to stop repairing
	//								private static final String parentString = "AND( SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , LEAF: D+complete ) ";
	//							
	//								private static final String childString = "AND( AND( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , LEAF: D+complete ) ";
	//							
	//								private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "C", "D", "E" } },
	//										new int[] { 1 });
	//							
	//								private static final int pointOfChange = 1;

	// Interesting 2: where to stop repairing
//								private static final String parentString = "SEQ( AND( SEQ( LEAF: A+complete , LEAF: C+complete ) , LEAF: D+complete ) , LEAF: E+complete ) ";
//							
//								private static final String childString = "SEQ( AND( SEQ( LEAF: A+complete , LEAF: C+complete , LEAF: E+complete ) , LEAF: D+complete ) , LEAF: E+complete ) ";
//							
//								private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "C", "D",  "E", "E" } },
//										new int[] { 1 });
//							
//								private static final int pointOfChange = 2;

	// Interesting 3: where to stop repairing
//						private static final String parentString = "SEQ( AND( SEQ( LEAF: A+complete , LEAF: C+complete ) , LEAF: D+complete ) , LEAF: T+complete , LEAF: E+complete ) ";
//							
//							private static final String childString = "SEQ( AND( SEQ( LEAF: A+complete , LEAF: C+complete , LEAF: E+complete ) , LEAF: D+complete ) , LEAF: T+complete , LEAF: E+complete ) ";
//						
//							private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "C", "D",  "E", "T" } },
//									new int[] { 1 });
//						
//							private static final int pointOfChange = 2;

//							private static final String parentString = "AND( SEQ( OR( LEAF: learning_activity__0+complete , LEAF: UoL_start+complete ) , OR( LEAF: learning_activity__4+complete , LEAF: learning_activity__0+complete ) ) , OR( LEAF: learning_activity__2+complete , LEAF: learning_activity__1+complete ) ) ";
//					
//							private static final String childString  = "AND( AND( OR( LEAF: learning_activity__0+complete , LEAF: UoL_start+complete ) , OR( LEAF: learning_activity__4+complete , LEAF: learning_activity__0+complete ) ) , OR( LEAF: learning_activity__2+complete , LEAF: learning_activity__1+complete ) ) ";
//						
//							private static final XLog eventlog = LogCreator.createLog(new String[][] { { "learning_activity__1", "learning_activity__4" } },
//									new int[] { 1 });
//						
//							private static final int pointOfChange = 1;

	//LOOP
//							private static final String parentString = "AND( LEAF: m+complete , LOOP( XOR( LEAF: A+complete , LEAF: tau ) , LEAF: C+complete , LEAF: D+complete ) )";
//						
//							private static final String childString = "AND( LEAF: m+complete , LOOP( LEAF: A+complete , LEAF: C+complete , LEAF: D+complete ) )";
//						
//							private static final XLog eventlog = LogCreator.createLog(new String[][] { { "C" }, { "A", "C", "D" } },
//									new int[] { 1, 1 });
//						
//							private static final int pointOfChange = 2;

	//	Problems when parallelism occurs
//						private static final String parentString = "AND( LEAF: D+complete , SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) ) ";
//				
//						private static final String childString = "AND( LEAF: D+complete , AND( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) ) ";
//					
//						private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "C", "B" }, {"D", "A", "B"} },
//								new int[] { 1, 1 });
//					
//						private static final int pointOfChange = 2;

	//TAU
//						private static final String parentString = "SEQ( XOR( LEAF: A+complete , LEAF: tau ) , LEAF: T+complete )";
//					
//						private static final String childString = "SEQ( XOR( LEAF: A+complete ) , LEAF: T+complete )";
//					
//						private static final XLog eventlog = LogCreator.createLog(new String[][] { { "T" }, { "A", "T" } },
//								new int[] { 1, 1 });
//					
//						private static final int pointOfChange = 1;

	// Testing XOR model move outside scope parse=false
	
//				private static final String parentString = "XOR( SEQ( LEAF: A+complete , LEAF: B+complete ) , AND( LEAF: C+complete , LEAF: D+complete ) )";
//				
//				private static final String childString = "XOR( LEAF: B+complete , AND( LEAF: C+complete , LEAF: D+complete ) )";
//			
//				private static final XLog eventlog = LogCreator.createLog(new String[][] { { "C", "D" }, { "A", "B" } }, new int[] { 1,2 });
//			
//				private static final int pointOfChange = 1;

	//test paralellism
//			private static final String parentString = "SEQ( SEQ( SEQ( LEAF: A+complete , LEAF: C+complete ) , LEAF: D+complete ) , LEAF: E+complete ) ";
//			
//			private static final String childString = "SEQ( SEQ( AND( LEAF: A+complete , LEAF: C+complete , LEAF: E+complete ) , LEAF: D+complete ) , LEAF: E+complete ) ";
//		
//			private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "C", "D", "E", "E" } }, new int[] { 1 });
//			
//			private static final int pointOfChange = 2;

//				private static final String parentString = "SEQ( LEAF: Z+complete , AND( AND( SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: E+complete ) ) , SEQ( LEAF: J+complete , LEAF: H+complete ) ) )" ;
//				
//				private static final String childString = "SEQ( LEAF: Z+complete , AND( AND( AND( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: E+complete ) ) , SEQ( LEAF: J+complete , LEAF: H+complete ) ) )";
//			
//				private static final XLog eventlog = LogCreator.createLog(new String[][] { { "Z", "A", "C", "D",  "B", "E" , "J", "H"} }, new int[] { 1 });
//				
//				private static final int pointOfChange = 4;

	// with everything
//			private static final String grandparentString = "SEQ( OR( LEAF: A+complete , LEAF: D+complete ) )";
		
//			private static final String parentString = "SEQ( OR( SEQ( LEAF: A+complete , LEAF: B+complete ) , LEAF: D+complete ) ) ";
//		//
//			private static final String childString = "SEQ ( OR( LOOP( SEQ( LEAF: A+complete , LEAF: B+complete ) , LEAF: C+complete , LEAF: tau ) , LEAF: D+complete ) ) ";
//		//
//			private static final XLog eventlog = LogCreator
//					.createLog(new String[][] { { "A", "B", "C", "D", "A", "B", "C", "A", "B" } }, new int[] { 1 });
//		//
//			private static final int pointOfChange = 2;

	// USELESS
	//			private static final String parentString = "AND( SEQ( LEAF: A+complete , LEAF: B+complete , LEAF: C+complete , LEAF: D+complete ) )";
	//			
	//			private static final String childString = "AND( SEQ(  LEAF: A+complete , AND( LEAF: B+complete ) , LEAF: C+complete , LEAF: D+complete ) )";
	//		
	//			private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "B", "C", "A", "D" } },
	//					new int[] { 1 });
	//		
	//			private static final int pointOfChange = 3;

	// testing internal moves
	//	private static final String parentString = "SEQ( SEQ( LEAF: a+complete , LEAF: b+complete , LEAF: c+complete ) , SEQ( LEAF: d+complete , LEAF: e+complete ) ) ";
	//
	//	private static final String childString = "SEQ( AND( LEAF: a+complete , LEAF: b+complete , LEAF: c+complete ) , SEQ( LEAF: d+complete , LEAF: e+complete ) ) ";
	//
	//	private static final XLog eventlog = LogCreator.createLog(new String[][] { { "a", "c", "d", "b", "e" } },
	//			new int[] { 1 });
	////
	//	private static final int pointOfChange = 1;
//	//
//		private static final String parentString = "AND( AND( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , LEAF: D+complete ) ";
//		
//		private static final String childString = "AND( SEQ( LEAF: A+complete , LEAF: E+complete ,  LEAF: C+complete ) , LEAF: D+complete ) ";
//	
//		private static final XLog eventlog = LogCreator.createLog(new String[][] { { "D","A", "C",  "E" } },
//				new int[] { 1 });
//	
//		private static final int pointOfChange = 1;
	//
//		private static final String parentString = "AND( SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ) , SEQ( LEAF: D+complete , LEAF: E+complete ) ) ";
//		
//		private static final String childString = "AND( AND( LEAF: A+complete , LEAF: B+complete ) , SEQ( LEAF: D+complete , LEAF: E+complete ) ) ";
//	
//		private static final XLog eventlog = LogCreator.createLog(new String[][] { { "A", "D" , "B", "C" , "E",  } },
//				new int[] { 1 });
//	
//		private static final int pointOfChange = 1;

//			private static final String parentString = "SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ,  SEQ( LEAF: D+complete , LEAF: E+complete ) , LEAF: J+complete ) " ;
//			
//			private static final String childString = "SEQ( LEAF: A+complete , LEAF: B+complete ,  LEAF: C+complete ,  SEQ( LEAF: D+complete ) , LEAF: J+complete ) " ;
//		
//			private static final XLog eventlog = 
//					LogCreator.createLog(new String[][] { { "A", "B","C","J"}, {"D","E"} }, new int[] { 1,1 });
//			private static final int pointOfChange = 4;

//	 PROBLEM WITH THE OR-JOINS AND SETTING THE SCOPE TO FALSE ONE ITERATION EARLY
//		private static final String parentString = "SEQ( LEAF: a+complete , LEAF: b+complete , OR( LEAF: c+complete , LEAF: d+complete ) , LEAF: e+complete , XOR( LEAF: c+complete , LEAF: h+complete ) , LEAF: j+complete ) " ;
//		
//		private static final String childString = "SEQ( LEAF: a+complete , LEAF: b+complete , OR( LEAF: d+complete , LEAF: c+complete ) , LEAF: e+complete , XOR( LEAF: c+complete , LEAF: h+complete ) , LEAF: j+complete ) " ;
//	
//		private static final XLog eventlog = 
//				LogCreator.createLog(new String[][] { { "a", "b","d","e", "c" , "j" } , { "a", "b","d","e", "c" , "h" ,"j"}  }, new int[] { 1,1 });
//		private static final int pointOfChange = 3;
//	private static final String parentString = "SEQ( LEAF: 0+complete , XOR( SEQ( AND( SEQ( LEAF: 1+complete , XOR( LEAF: 5+complete , LEAF: 4+complete ) ) , AND( LEAF: 3+complete , LEAF: 2+complete ) ) , LEAF: 6+complete ) , LEAF: 2 ) )";
//
//	private static final String childString = "SEQ( LEAF: 0+complete , XOR( SEQ( AND( SEQ( LEAF: 1+complete ) , AND( LEAF: 3+complete , LEAF: 2+complete ) ) , LEAF: 6+complete ) , LEAF: 2+complete ) ) ";
//
//	private static final XLog eventlog = LogCreator.createLog(
//			new String[][] { { "0", "3", "1", "2", "5", "6" }, { "0", "2", "1", "4", "6" } }, new int[] { 1, 1 });
//	private static final int pointOfChange = 5;
	// PROBLEM WITH INTERNAL MOVE.
//	private static final String parentString = "OR( AND( SEQ( LEAF: 1+complete , SEQ( LEAF: 5+complete , SEQ( LEAF: 6+complete , LEAF: 0+complete ) ) ) , LEAF: 4+complete ) , LEAF: 3+complete )";
//
//	private static final String childString =  "OR( AND( SEQ( LEAF: 1+complete , SEQ( LEAF: 5+complete , SEQ( LEAF: 6+complete , LEAF: 0+complete ) ) ) , OR( LEAF: 4+complete , LEAF: 2+complete ) ) , LEAF: 3+complete ) ";
//
//	private static final XLog eventlog = LogCreator.createLog(
//			new String[][] { { "1", "4", "5", "3", "6", "0" }, { "0", "1", "3", "2", "4", "6" } }, new int[] { 1, 1 });
//	private static final int pointOfChange = 1;
	
//	private static final String parentString =     "SEQ( LEAF: 1+complete , SEQ( LEAF: 4+complete , LEAF: 5+complete ) , XOR( LEAF: 3+complete , LEAF: 2+complete ) , LEAF: 5+complete , LEAF: 6+complete , LEAF: 0+complete )";
//	
//		private static final String childString =  "SEQ( LEAF: 1+complete , SEQ( LEAF: 6+complete , LEAF: 5+complete ) , XOR( LEAF: 3+complete , LEAF: 2+complete ) , LEAF: 5+complete , LEAF: 6+complete , LEAF: 0+complete )";
//	
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { { "1", "4", "5", "6", "3", "0" }, { "0", "1", "3", "2", "4", "6" } }, new int[] { 1, 1 });
//		private static final int pointOfChange = 2;
	
//	private static final String parentString =     "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( XOR( LEAF: 3+complete , LEAF: tau ) , LEAF: 5+complete ) , LEAF: 5+complete , LEAF: 6+complete , LEAF: 0+complete )";
////	
//		private static final String childString =  "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( XOR( LEAF: tau , LEAF: 3+complete ) , LEAF: 5+complete ) , LEAF: 5+complete , LEAF: 6+complete , LEAF: 0+complete )";
//	
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { { "1", "4", "5", "3", "6", "0" } }, new int[] { 1, 1 });
//		private static final int pointOfChange = 4;
	
	// problem with missing or-join at the end because of a internal move to outside the scope
//	private static final String parentString =     "SEQ( LEAF: 1+complete , OR( LEAF: 4+complete , LEAF: 2+complete ) , OR( LEAF: 5+complete , LEAF: 3+complete ) , OR( LEAF: 6+complete , LEAF: 2+complete ) , OR( LEAF: 0+complete , LEAF: 3+complete ) )";
////
//	private static final String childString =      "SEQ( LEAF: 1+complete , OR( LEAF: 4+complete , LEAF: 2+complete ) , OR( LEAF: 5+complete , LEAF: 3+complete ) , OR( LEAF: 6+complete , AND( LEAF: 2+complete ) ) , OR( LEAF: 0+complete , LEAF: 3+complete ) )";
//
//	private static final XLog eventlog = LogCreator.createLog(
//			new String[][] { { "1", "4", "5", "2", "6", "0" } ,{"3"}}, new int[] { 1, 1 });
//	private static final int pointOfChange = 8;
	
//	private static final String parentString =     "SEQ( LEAF: 1+complete , SEQ( LEAF: 4+complete , LEAF: 5+complete ) , XOR( LEAF: 2+complete , LEAF: 3+complete ) , LEAF: 5+complete , LEAF: 6+complete , LEAF: 0+complete )";
////												
//	private static final String childString =      "SEQ( LEAF: 1+complete , SEQ( LEAF: 4+complete , SEQ( LEAF: 1+complete , AND( SEQ( LEAF: 4+complete , LEAF: 0+complete ) , LEAF: 3+complete ) ) ) , XOR( LEAF: 2+complete , LEAF: 3+complete ) , LEAF: 5+complete , LEAF: 6+complete , LEAF: 0+complete )";
//
//	private static final XLog eventlog = LogCreator.createLog(
//			new String[][] { { "1", "4", "5", "2", "6", "0" } ,{"3"}}, new int[] { 1, 1 });
//	private static final int pointOfChange = 4;
//	
	// desync log model
//	private static final String parentString =     "SEQ( LEAF: 1+complete , SEQ( AND( AND( XOR( LEAF: 3+complete ) ) , SEQ( LEAF: 4+complete , LEAF: 5+complete , LEAF: 6+complete ) ) , LEAF: 0+complete ) )";
//	//
//		private static final String childString =      "SEQ( LEAF: 1+complete , SEQ( AND( AND( XOR( LEAF: 3+complete ) ) , SEQ( LEAF: 4+complete , OR( LEAF: 5+complete , LEAF: 2+complete ) , LEAF: 6+complete ) ) , LEAF: 0+complete ) ) [ ]";
//
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { { "1", "4", "5", "3", "6", "0" } ,{"2"}}, new int[] { 1, 1 });
//		private static final int pointOfChange = 7;
	
//	private static final String parentString =     "SEQ( LEAF: 1+complete , OR( SEQ( SEQ( LEAF: 4+complete , SEQ( OR( LEAF: 5+complete , LEAF: 3+complete ) , LEAF: 6+complete ) ) , LEAF: 0+complete ) , SEQ( XOR( LEAF: tau , LEAF: 3+complete , LEAF: 2+complete ) ) ) )";
//	//
//		private static final String childString =  "SEQ( LEAF: 1+complete , OR( SEQ( SEQ( LEAF: 4+complete , SEQ( OR( LEAF: 3+complete , LEAF: 5+complete ) , LEAF: 6+complete ) ) , LEAF: 0+complete ) , SEQ( XOR( LEAF: tau , LEAF: 3+complete , LEAF: 2+complete ) ) ) )";
//
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { { "1", "4", "5", "6", "3", "0" } ,{"2"}}, new int[] { 1, 1 });
//		private static final int pointOfChange = 7;
	
	// FIX MISSING OR-split
//	private static final String parentString =     "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( OR( LEAF: 5+complete , LEAF: 3+complete ) , LEAF: 2+complete ) , OR( LEAF: 3+complete , LEAF: 6+complete ) , XOR( LEAF: 2+complete , LEAF: tau ) , LEAF: 0+complete )";
//	//
//		private static final String childString =  "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( OR( LEAF: 5+complete , LEAF: 3+complete ) , LEAF: 3+complete ) , OR( LEAF: 3+complete , LEAF: 6+complete ) , XOR( LEAF: 2+complete , LEAF: tau ) , LEAF: 0+complete )";
//
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { { "1", "4", "5", "3", "6", "0" } ,{"2"}}, new int[] { 1, 1 });
//		private static final int pointOfChange = 3;
//	
	// or join wrongly placed? unfixable :(
//	private static final String parentString =     "SEQ( AND( LEAF: 1+complete ) , LEAF: 4+complete , OR( LEAF: 5+complete , LEAF: 2+complete ) , LEAF: 6+complete , XOR( LEAF: 2+complete , LEAF: 3+complete ) , LEAF: 0+complete )";
//	//
//		private static final String childString =  "SEQ( AND( LEAF: 1+complete ) , LEAF: 4+complete , OR( OR( LEAF: 5+complete , LEAF: 3+complete ) , LEAF: 2+complete ) , LEAF: 6+complete , XOR( LEAF: 2+complete , LEAF: 3+complete ) , LEAF: 0+complete )";
//
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { { "1", "4", "5", "2", "6", "0" } ,{"3"}}, new int[] { 1, 1 });
//		private static final int pointOfChange = 4;
	
	// missing internalmove --- fixed.
//		private static final String parentString =     "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( OR( LEAF: 5+complete , LEAF: 3+complete ) , LEAF: 2+complete ) , OR( LEAF: 6+complete , LEAF: 3+complete ) , XOR( LEAF: tau , LEAF: tau ) , LEAF: 0+complete )";
//		//
//			private static final String childString =  "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( XOR( LEAF: 5+complete , LEAF: 3+complete ) , LEAF: 2+complete ) , OR( LEAF: 6+complete , LEAF: 3+complete ) , XOR( LEAF: tau , LEAF: tau ) , LEAF: 0+complete ) ";
//
//			private static final XLog eventlog = LogCreator.createLog(
//					new String[][] { { "1", "4", "5", "2", "6", "0" } ,{"3"}}, new int[] { 1, 1 });
//			private static final int pointOfChange = 4;
//			private static final String parentString =     "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( OR( LEAF: 5+complete , LEAF: 3+complete ) , LEAF: 2+complete ) , OR( LEAF: 6+complete , LEAF: 3+complete ) , LEAF: 2+complete , LEAF: 0+complete )";
//			//
//				private static final String childString =  "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( OR( LEAF: 5+complete , SEQ( LEAF: 3+complete ) ) , LEAF: 2+complete ) , OR( LEAF: 6+complete , LEAF: 3+complete ) , LEAF: 2+complete , LEAF: 0+complete )";
//
//				private static final XLog eventlog = LogCreator.createLog(
//						new String[][] { { "1", "4", "5", "2", "6", "0" } ,{"3"}}, new int[] { 1, 1 });
//				private static final int pointOfChange = 4;
//	private static final String parentString =     "SEQ( LEAF: 1+complete , OR( LEAF: 4+complete , LEAF: 3+complete ) , OR( LEAF: 5+complete , LEAF: 2+complete ) , LEAF: 6+complete , LEAF: 3+complete , LEAF: 0+complete )";
//	//
//		private static final String childString =  "SEQ( LEAF: 1+complete , XOR( LEAF: 4+complete , LEAF: 3+complete ) , OR( LEAF: 5+complete , LEAF: 2+complete ) , LEAF: 6+complete , LEAF: 3+complete , LEAF: 0+complete )";
//
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { { "1", "4", "5", "3", "6", "0" } ,{"2"}}, new int[] { 1, 1 });
//		private static final int pointOfChange = 2;
//		private static final String parentString =     "XOR( SEQ( XOR( XOR( OR( AND( XOR( XOR( OR( AND( OR( XOR( OR( SEQ( LEAF: 1+complete , OR( OR( XOR( AND( OR( OR( AND( OR( SEQ( LEAF: 16+complete , OR( OR( OR( LEAF: 21+complete , LEAF: 7+complete ) , OR( LEAF: 8+complete , LEAF: 10+complete ) ) , LEAF: 11+complete ) ) , LEAF: 10+complete ) , LEAF: 17+complete ) , LEAF: 8+complete ) , LEAF: 9+complete ) , LEAF: 18+complete ) , LEAF: 8+complete ) , OR( LEAF: 10+complete , LEAF: 9+complete ) ) , LEAF: 12+complete ) ) , LEAF: 13+complete ) , LEAF: 10+complete ) , LEAF: 13+complete ) , LEAF: 19+complete ) , LEAF: 3+complete ) , OR( LEAF: 9+complete , LEAF: 10+complete ) ) , LEAF: 12+complete ) , OR( LEAF: 2+complete , LEAF: 20+complete ) ) , LEAF: 5+complete ) , LEAF: 8+complete ) , LEAF: 8+complete ) , LEAF: 0+complete ) , LEAF: 7+complete ) ";
//		//
//			private static final String childString =  "XOR( SEQ( XOR( XOR( OR( AND( XOR( XOR( OR( AND( OR( XOR( OR( SEQ( LEAF: 1+complete , OR( OR( XOR( AND( OR( OR( AND( OR( SEQ( LEAF: 16+complete , OR( LEAF: 11+complete ) ) , LEAF: 10+complete ) , LEAF: 17+complete ) , LEAF: 8+complete ) , LEAF: 9+complete ) , LEAF: 18+complete ) , LEAF: 8+complete ) , OR( LEAF: 10+complete , LEAF: 9+complete ) ) , LEAF: 12+complete ) ) , LEAF: 13+complete ) , LEAF: 10+complete ) , LEAF: 13+complete ) , LEAF: 19+complete ) , LEAF: 3+complete ) , OR( LEAF: 9+complete , LEAF: 10+complete ) ) , LEAF: 12+complete ) , OR( LEAF: 2+complete , LEAF: 20+complete ) ) , SEQ( XOR( XOR( OR( AND( XOR( XOR( OR( AND( OR( XOR( OR( SEQ( LEAF: 1+complete , OR( OR( XOR( AND( OR( OR( AND( OR( SEQ( LEAF: 16+complete , OR( OR( OR( LEAF: 7+complete , LEAF: 21+complete ) , OR( LEAF: 8+complete , LEAF: 10+complete ) ) , LEAF: 11+complete ) ) , LEAF: 10+complete ) , LEAF: 17+complete ) , LEAF: 8+complete ) , LEAF: 9+complete ) , LEAF: 18+complete ) , LEAF: 8+complete ) , OR( LEAF: 10+complete , LEAF: 9+complete ) ) , LEAF: 12+complete ) ) , LEAF: 13+complete ) , LEAF: 10+complete ) , LEAF: 13+complete ) , LEAF: 19+complete ) , LEAF: 3+complete ) , OR( LEAF: 9+complete , LEAF: 10+complete ) ) , LEAF: 12+complete ) , OR( LEAF: 2+complete , LEAF: 20+complete ) ) , LEAF: 5+complete ) , LEAF: 8+complete ) , LEAF: 8+complete ) , LEAF: 0+complete ) ) , LEAF: 8+complete ) , LEAF: 8+complete ) , LEAF: 0+complete ) , LEAF: 7+complete ) ";
//
//			private static final XLog eventlog = LogCreator.createLog(
//					new String[][] { { "1", "16", "2", "3", "17", "5", "18", "11", "13", "7", "8", "19", "20", "9", "12", "13", "7", "8", "10", "8", "9", "10", "8", "10", "8", "10", "8", "10", "8", "10", "8", "10", "8", "10", "8", "12", "13", "3", "5", "11", "13", "14", "15", "0" } ,{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30"}}, new int[] { 1, 1 });
//			private static final int pointOfChange = 4;

//	private static final String parentString =     "SEQ( LEAF: a+complete , AND( LEAF: c+complete , XOR( LEAF: d+complete , LEAF: e+complete ) ) , LEAF: f+complete , LOOP( OR( LEAF: g+complete , LEAF: h+complete ) , LEAF: i+complete , LEAF: tau ) )";
//	//
//		private static final String childString =  "XOR( SEQ( XOR( XOR( OR( AND( XOR( XOR( OR( AND( OR( XOR( OR( SEQ( LEAF: 1+complete , OR( OR( XOR( AND( OR( OR( AND( OR( SEQ( LEAF: 16+complete , OR( LEAF: 11+complete ) ) , LEAF: 10+complete ) , LEAF: 17+complete ) , LEAF: 8+complete ) , LEAF: 9+complete ) , LEAF: 18+complete ) , LEAF: 8+complete ) , OR( LEAF: 10+complete , LEAF: 9+complete ) ) , LEAF: 12+complete ) ) , LEAF: 13+complete ) , LEAF: 10+complete ) , LEAF: 13+complete ) , LEAF: 19+complete ) , LEAF: 3+complete ) , OR( LEAF: 9+complete , LEAF: 10+complete ) ) , LEAF: 12+complete ) , OR( LEAF: 2+complete , LEAF: 20+complete ) ) , SEQ( XOR( XOR( OR( AND( XOR( XOR( OR( AND( OR( XOR( OR( SEQ( LEAF: 1+complete , OR( OR( XOR( AND( OR( OR( AND( OR( SEQ( LEAF: 16+complete , OR( OR( OR( LEAF: 7+complete , LEAF: 21+complete ) , OR( LEAF: 8+complete , LEAF: 10+complete ) ) , LEAF: 11+complete ) ) , LEAF: 10+complete ) , LEAF: 17+complete ) , LEAF: 8+complete ) , LEAF: 9+complete ) , LEAF: 18+complete ) , LEAF: 8+complete ) , OR( LEAF: 10+complete , LEAF: 9+complete ) ) , LEAF: 12+complete ) ) , LEAF: 13+complete ) , LEAF: 10+complete ) , LEAF: 13+complete ) , LEAF: 19+complete ) , LEAF: 3+complete ) , OR( LEAF: 9+complete , LEAF: 10+complete ) ) , LEAF: 12+complete ) , OR( LEAF: 2+complete , LEAF: 20+complete ) ) , LEAF: 5+complete ) , LEAF: 8+complete ) , LEAF: 8+complete ) , LEAF: 0+complete ) ) , LEAF: 8+complete ) , LEAF: 8+complete ) , LEAF: 0+complete ) , LEAF: 7+complete ) ";
//
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { {"a","d","c","e","g","i","h" }}, new int[] { 1});
//		private static final int pointOfChange = 4;
	
	
//	private static final String parentString =     "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( OR( LEAF: 5+complete , LEAF: 5+complete ) , LEAF: 3+complete ) , LEAF: 6+complete , XOR( XOR( LEAF: 2+complete , LEAF: tau ) , LEAF: 3+complete ) , LEAF: 0+complete )";
//	//
//		private static final String childString =  "SEQ( LEAF: 1+complete , LEAF: 4+complete , AND( OR( LEAF: 5+complete , LEAF: 5+complete ) , LEAF: 3+complete ) , LEAF: 6+complete , XOR( XOR( LEAF: 2+complete , LEAF: tau ) , LEAF: 3+complete ) , LEAF: 0+complete ) ";
//
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { {"1", "4", "5", "3", "6", "0" }, {"1", "4", "5", "3", "6", "2","0" }}, new int[] { 1,1});
//		private static final int pointOfChange = 3;
//		
//	private static final String parentString =     "SEQ( LEAF: 6+complete , LEAF: 0+complete , LOOP( LEAF: 4+complete , LOOP( LEAF: 4+complete , LEAF: tau , LEAF: tau ) , LEAF: tau ) , AND( LEAF: 2+complete , LEAF: 1+complete ) , LEAF: 3+complete , LEAF: 5+complete )";
////	//
//		private static final String childString =  "SEQ( LEAF: 6+complete , LEAF: 0+complete , LOOP( LEAF: 4+complete , LOOP( SEQ( LEAF: 6+complete , LEAF: 5+complete , LEAF: 5+complete ) , LEAF: tau , LEAF: tau ) , LEAF: tau ) , AND( LEAF: 2+complete , LEAF: 1+complete ) , LEAF: 3+complete , LEAF: 5+complete ) ";
//
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { {"6", "0", "4", "4", "4", "4", "4", "1", "2", "3", "5" }}, new int[] { 1});
//		private static final int pointOfChange = 5;
	
//	private static final String parentString =     "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( OR( LEAF: 5+complete , LEAF: 3+complete ) , LEAF: 2+complete ) , OR( LEAF: 3+complete , LEAF: 6+complete ) , XOR( LEAF: 2+complete , LEAF: tau ) , LEAF: 0+complete )";
////	//
//		private static final String childString =  "SEQ( LEAF: 1+complete , LEAF: 4+complete , OR( OR( LEAF: 5+complete , LEAF: 3+complete ) , LEAF: 3+complete ) , OR( LEAF: 3+complete , LEAF: 6+complete ) , XOR( LEAF: 2+complete , LEAF: tau ) , LEAF: 0+complete )";
//
//		private static final XLog eventlog = LogCreator.createLog(
//				new String[][] { {"1", "4", "5", "3", "6", "0" }, {"2"}}, new int[] { 1,1});
//		private static final int pointOfChange = 3;
	
	
//	private static final String parentString =     "SEQ( LEAF: 6+complete , OR( LOOP( LEAF: 4+complete , LEAF: tau , LEAF: tau ) , LEAF: 0+complete ) , AND( LEAF: 2+complete , LEAF: 1+complete ) , LEAF: tau , LEAF: 3+complete , LEAF: 5+complete )";
//////
//	private static final String childString =  "SEQ( LEAF: 6+complete , OR( LOOP( LEAF: 4+complete , LEAF: tau , LEAF: tau ) , LEAF: 0+complete ) , AND( AND( LEAF: 2+complete , LEAF: 1+complete ) ) , LEAF: tau , LEAF: 3+complete , LEAF: 5+complete ) ";
//
//	private static final XLog eventlog = LogCreator.createLog(
//			new String[][] { {"6", "0", "2", "1", "3", "5" }, {"4"}}, new int[] { 1,1});
//	private static final int pointOfChange = 8;
//		

	private static final String parentString =     "SEQ( LEAF: 1+complete , XOR( AND( AND( SEQ( LEAF: 3+complete , LEAF: 0+complete ) , OR( LEAF: 5+complete , LEAF: 3+complete ) ) , OR( AND( LEAF: 6+complete ) , LEAF: 2+complete ) ) , LEAF: tau ) )";
////
	private static final String childString =  "SEQ( LEAF: 1+complete , XOR( AND( AND( SEQ( OR( LEAF: 3+complete , LEAF: 4+complete ) , LEAF: 0+complete ) , OR( LEAF: 5+complete , LEAF: 3+complete ) ) , OR( AND( LEAF: 6+complete ) , LEAF: 2+complete ) ) , LEAF: tau ) ) ";

	private static final XLog eventlog = LogCreator.createLog(
			new String[][] { {"1", "4", "5", "2", "6", "0" }, {"3"}}, new int[] { 1,1});
	private static final int pointOfChange = 5;
}
