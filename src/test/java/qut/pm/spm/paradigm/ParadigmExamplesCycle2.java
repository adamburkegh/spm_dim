package qut.pm.spm.paradigm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;
import org.processmining.framework.plugin.PluginContext;

import qut.pm.prom.helpers.ConsoleUIPluginContext;
import qut.pm.prom.helpers.HeadlessDefinitelyNotUIPluginContext;
import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.setm.TaskStats;
import qut.pm.setm.evaluation.EarthMoversCalculator;
import qut.pm.setm.evaluation.EarthMoversTunedCalculator;
import qut.pm.setm.evaluation.EntropyPrecisionRecallCalculator;
import qut.pm.setm.evaluation.SPNQualityCalculator;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.log.ProvenancedLogImpl;
import qut.pm.spm.measures.LogStatsCache;
import qut.pm.spm.measures.SimplicityEdgeCountMeasure;
import qut.pm.spm.measures.SimplicityStructuralStochasticUniqMeasure;
import qut.pm.spm.measures.StochasticLogCachingMeasure;
import qut.pm.spm.measures.StochasticStructuralComplexity;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class ParadigmExamplesCycle2 {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private PetriNetFragmentParser parser;
	private List<StochasticLogCachingMeasure> netMeasures;
	private List<SPNQualityCalculator> evalCalcs;
	private PluginContext context = new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(),
			"paradigm_test");


	static {
		
	}
	
	@Before
	public void beforeClass() {
		parser = new PetriNetFragmentParser();
		netMeasures = new ArrayList<>();
		netMeasures.add(new StochasticStructuralComplexity());
		LogStatsCache statsCache = new LogStatsCache();
		netMeasures.add(new SimplicityStructuralStochasticUniqMeasure(statsCache));
		netMeasures.add(new SimplicityEdgeCountMeasure(statsCache));
		evalCalcs = new ArrayList<>();
		evalCalcs.add(new EarthMoversCalculator() );
		evalCalcs.add(new EarthMoversTunedCalculator() );
		evalCalcs.add(new EntropyPrecisionRecallCalculator());
	}

	private void calcAndReport(String caseName, XLog xlog, AcceptingStochasticNet net) throws Exception {
		ProvenancedLog log = new ProvenancedLogImpl(xlog, "fakeFileName");
		TaskStats stats = new TaskStats(caseName);
		for (StochasticLogCachingMeasure nm: netMeasures) {
			double result = nm.calculate(log, net, NAME_CLASSIFIER);
			stats.setMeasure(nm.getMeasure(), result);
		}
		for (SPNQualityCalculator ec: evalCalcs) {
			try {
				ec.calculate(context, net, log, NAME_CLASSIFIER, stats);
			}catch(Exception e) {
				LOGGER.error(e);
			}
		}
		LOGGER.info(reportStats(stats));
	}

	
	@Test
	public void singletonLog() throws Exception{
		XLog log = converter.convertTextArgs("a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		calcAndReport("singletonLog", log, net);
	}


	@Test
	public void matchingHalfHalfLogModel() throws Exception {
		XLog log = converter.convertTextArgs("a b","c d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		calcAndReport("matchingHalfLogHalfModel", log, net);
	}
	
	@Test
	public void logSmallerPerfectFitness() throws Exception{
		XLog log = converter.convertTextArgs("a b","c d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {e} -> p3 -> {f} -> End");
		calcAndReport("logSmallerPerfectFitness", log, net);
	}

	@Test
	public void modelSmallerPerfectFitness() throws Exception{
		XLog log = converter.convertTextArgs("a b","c d","e f");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {e} -> p3 -> {f} -> End");
		calcAndReport("modelSmallerPerfectFitness", log, net);
	}
	
	@Test
	public void wideChoiceIntersection() throws Exception {
		XLog log = converter.convertTextArgs("a b","c d","e f",
											 "a b","c d","e f");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {e} -> p3 -> {f} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {g} -> p3 -> {h} -> End");
		calcAndReport("wideChoiceIntersection", log, net);
	}
	
	@Test
	public void bigMiddleManySmallVariants() throws Exception {
		XLog log = converter.convertTextArgs("a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a c",
											 "e f",
											 "e g");
		AcceptingStochasticNet netBM = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> End");
		calcAndReport("bigMiddle BM", log, netBM);
		AcceptingStochasticNet netBMimperfect = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> End");
		parser.addToAcceptingNet(netBMimperfect,
				  "Start -> {y} -> End");
		parser.addToAcceptingNet(netBMimperfect,
				  "Start -> {z} -> End");
		calcAndReport("bigMiddle BM imperfect", log, netBMimperfect);
		AcceptingStochasticNet netLV = parser.createAcceptingNet("net", 
				  "Start -> {a} -> p2 -> {c} -> End");
		parser.addToAcceptingNet(netLV,
				  "Start -> {e} -> p3 -> {f} -> End");
		calcAndReport("bigMiddle LV", log, netLV);
		AcceptingStochasticNet netLVSM = parser.createAcceptingNet("net",
				  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(netLVSM,
				  "Start -> {a} -> p2 -> {c} -> End");
		parser.addToAcceptingNet(netLVSM,
				  "Start -> {e} -> End");
		calcAndReport("bigMiddle LVSM", log, netLVSM);
		AcceptingStochasticNet netLVSMwE = parser.createAcceptingNet("net",
				  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(netLVSMwE,
				  "Start -> {a} -> p2 -> {c} -> End");
		parser.addToAcceptingNet(netLVSMwE,
				  "Start -> {e} -> End");
		parser.addToAcceptingNet(netLVSMwE,
				  "Start -> {z} -> End");
		calcAndReport("bigMiddle LVSM with error", log, netLVSMwE);
		AcceptingStochasticNet netBad = parser.createAcceptingNet("net", 
				  "Start -> {a} -> End");
		parser.addToAcceptingNet(netBad,
				  "Start -> {e} -> End");
		parser.addToAcceptingNet(netBad,
				  "Start -> {y 10.0} -> End");
		parser.addToAcceptingNet(netBad,
				  "Start -> {z 10.0} -> End");
		calcAndReport("bigMiddle net bad", log, netBad);
	}
	
	@Test
	public void bigMiddleWithLoop() throws Exception {
		XLog log = converter.convertTextArgs("a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b c","a b c",
											 "a b c c", 
											 "e f");
		AcceptingStochasticNet netBM = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> End");
		calcAndReport("bigMiddleWithLoop BM", log, netBM);
		AcceptingStochasticNet netBMimperfect = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> End");
		parser.addToAcceptingNet(netBMimperfect,
				  "Start -> {y} -> End");
		parser.addToAcceptingNet(netBMimperfect,
				  "Start -> {z} -> End");
		calcAndReport("bigMiddleWithLoop BM imperfect", log, netBMimperfect);
		AcceptingStochasticNet netBMLoop = parser.createAcceptingNet("net", 
				  "Start -> {a 20.0} -> p1 -> {b 20.0} -> p2 -> {tau__1} -> p3 -> {c} -> p3 -> {tau__2 4.0} -> End");
		parser.addToAcceptingNet(netBMLoop,
				  "Start -> {a} -> p4 -> {y} -> End");
		calcAndReport("bigMiddleWithLoop BM Loop", log, netBMLoop);
		AcceptingStochasticNet netLVSM = parser.createAcceptingNet("net",
				  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(netLVSM,
				  "Start -> {a} -> p2 -> {c} -> End");
		parser.addToAcceptingNet(netLVSM,
				  "Start -> {e} -> End");
		calcAndReport("bigMiddleWithLoop LVSM", log, netLVSM);
		AcceptingStochasticNet netLVSMwE = parser.createAcceptingNet("net",
				  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(netLVSMwE,
				  "Start -> {a} -> p2 -> {c} -> End");
		parser.addToAcceptingNet(netLVSMwE,
				  "Start -> {e} -> End");
		parser.addToAcceptingNet(netLVSMwE,
				  "Start -> {z} -> End");
		calcAndReport("bigMiddleWithLoop LVSM with error", log, netLVSMwE);
		AcceptingStochasticNet netBad = parser.createAcceptingNet("net", 
				  "Start -> {a} -> End");
		parser.addToAcceptingNet(netBad,
				  "Start -> {e} -> End");
		parser.addToAcceptingNet(netBad,
				  "Start -> {y 10.0} -> End");
		parser.addToAcceptingNet(netBad,
				  "Start -> {z 10.0} -> End");
		calcAndReport("bigMiddleWithLoop net bad", log, netBad);
	}
	
	
	@Test
	public void paperExtremeExamples() throws Exception {
		XLog log = converter.convertTextArgs("a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b c","a b c",
											 "a b c c", 
											 "e f");
		AcceptingStochasticNet netBMLoop = parser.createAcceptingNet("net", 
				  "Start -> {a 20.0} -> p1 -> {b 20.0} -> p2 -> {tau__1} -> p3 -> {c} -> p3 -> {tau__2 3.0} -> End");
		calcAndReport("paperEg A+ E+", log, netBMLoop);
		AcceptingStochasticNet netAplusEminusHF = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> End");
		parser.addToAcceptingNet(netAplusEminusHF,
				  "Start -> {y} -> End");
		parser.addToAcceptingNet(netAplusEminusHF,
				  "Start -> {z} -> End");
		calcAndReport("paperEg A+ E- (HF-)", log, netAplusEminusHF);
		AcceptingStochasticNet netAplusEminusHP= parser.createAcceptingNet("net", 
				  "Start -> {a 20.0} -> p1 -> {b 20.0} -> p2 -> {tau__1} -> p3 -> {c} -> p3 -> {tau__2 3.0} -> End");
		parser.addToAcceptingNet(netAplusEminusHP,
				  "Start -> {a 20.0} -> p1 -> {tau__3} -> End");
		parser.addToAcceptingNet(netAplusEminusHP,
				  "Start -> {a 20.0} -> p4 -> {x} -> End");
		calcAndReport("paperEg A~ E- (HP-)", log, netAplusEminusHP);
		AcceptingStochasticNet netAminusEplus = parser.createAcceptingNet("net",
				  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(netAminusEplus,
				  "Start -> {a} -> p2 -> {c} -> End");
		parser.addToAcceptingNet(netAminusEplus,
				  "Start -> {e} -> End");
		calcAndReport("paperEg A- E+", log, netAminusEplus);
		AcceptingStochasticNet netBad = parser.createAcceptingNet("net", 
				  "Start -> {a} -> End");
		parser.addToAcceptingNet(netBad,
				  "Start -> {e} -> End");
		parser.addToAcceptingNet(netBad,
				  "Start -> {y 10.0} -> End");
		parser.addToAcceptingNet(netBad,
				  "Start -> {z 10.0} -> End");
		calcAndReport("paperEg net bad", log, netBad);
	}
	
	@Test
	public void bigMiddleWithConc() throws Exception {
		XLog log = converter.convertTextArgs("a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "b a",
											 "f e",
											 "g h");
		AcceptingStochasticNet netBM = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> End");
		calcAndReport("bigMiddleWithConc BM", log, netBM);
		AcceptingStochasticNet netBMimperfect = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> End");
		parser.addToAcceptingNet(netBMimperfect,
				  "Start -> {y} -> End");
		parser.addToAcceptingNet(netBMimperfect,
				  "Start -> {z} -> End");
		calcAndReport("bigMiddleWithConc BM imperfect", log, netBMimperfect);
		AcceptingStochasticNet netLVConc= parser.createAcceptingNet("net", 
				  "Start -> {tau} -> p2 -> {e 10.0} -> End");
		parser.addToAcceptingNet(netLVConc,
				  "Start -> {tau} -> p3 -> {f 10.0} -> End");
		parser.addToAcceptingNet(netLVConc,
				  "Start -> {y} -> p4 -> {z} -> End");
		calcAndReport("bigMiddleWithConc LV conc", log, netLVConc);
		AcceptingStochasticNet netBMConc= parser.createAcceptingNet("net", 
				  "Start -> {tau} -> p3 -> {a  20.0} -> End");
		parser.addToAcceptingNet(netBMConc,
				  "Start -> {tau} -> p4 -> {b} -> End");
		parser.addToAcceptingNet(netBMConc,
				  "Start -> {y} -> p5 -> {z} -> End");
		calcAndReport("bigMiddleWithConc BM conc", log, netBMConc);
	}
	
	@Test
	public void projectionModelGreater() throws Exception{
		XLog log = converter.convertTextArgs(
					"a b","a b",
					"c d","c d",
					"e f","e f","e f","e f",
					"g h","g h");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> p1 -> {b 2.0} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {i} -> p3 -> {j} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {k} -> p3 -> {l} -> End");		
		calcAndReport("projectionModelGreater", log, net);
	}
	

	@Test
	public void loopModelLopsidedMissingActivity() throws Exception{
		/// entropy bounds may be exceeded
		XLog log = converter.convertTextArgs(
				"a",
				"a a",
				"a c a",
				"a c c a",
				"a a c c a",
				"a b", "a b", "a b","a b","a b","a b","a b",
				"d");
		// Tree:
		//		  \/  28
		//		      /\  3
		//		        @ [r2.0] 1
		//		          a 1
		//		        a 1
		//		        @ [r2.0] 1
		//		          c 1
		//		      a 25
		ProbProcessTreeNode pptLoop1 = ProbProcessTreeFactory.createLoop(2.0d);
		pptLoop1.addChild( ProbProcessTreeFactory.createLeaf("a", 1 ) );
		ProbProcessTreeNode pptLoop2 = ProbProcessTreeFactory.createLoop(2.0d);
		pptLoop2.addChild( ProbProcessTreeFactory.createLeaf("c", 1 ) );
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptConc1 = ProbProcessTreeFactory.createConcurrency();
		pptConc1.addChild(pptLoop1);
		pptConc1.addChild( ProbProcessTreeFactory.createLeaf("a", 1 ) );
		pptConc1.addChild(pptLoop2);
		ppt.addChild(pptConc1);
		ppt.addChild( ProbProcessTreeFactory.createLeaf("a", 25 ) ) ;
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		calcAndReport("loopModelMissingActivity", log, snet);
	}
	
	private String reportStats(TaskStats stats) {
		Map<Measure, Number> measures = stats.getMeasures();
		StringBuffer result = new StringBuffer();
		result.append("\n");
		result.append(stats.getTaskName());
		result.append("\n");
		TreeMap<Measure, Number> sm = new TreeMap<Measure, Number>(measures);
		for (Measure measure: sm.keySet()) {
			result.append( String.format("%-40s : %5.2f \n",measure,sm.get(measure)));
		}
		return result.toString();
	}
	
}
