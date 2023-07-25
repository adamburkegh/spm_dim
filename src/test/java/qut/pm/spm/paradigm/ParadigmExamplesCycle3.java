package qut.pm.spm.paradigm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XLogImpl;
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
import qut.pm.spm.measures.AdhesionDRMeasure;
import qut.pm.spm.measures.AdhesionDRSmallLogMeasure;
import qut.pm.spm.measures.EdgeCount;
import qut.pm.spm.measures.EntityCount;
import qut.pm.spm.measures.EntropicRelevanceZeroOrderMeasure;
import qut.pm.spm.measures.ExistentialPrecisionMeasure;
import qut.pm.spm.measures.GenTraceDiffMeasure;
import qut.pm.spm.measures.GenTraceFloorMeasure;
import qut.pm.spm.measures.LogStatsCache;
import qut.pm.spm.measures.RelevanceDRMeasure;
import qut.pm.spm.measures.RelevanceDRSmallLogMeasure;
import qut.pm.spm.measures.SimplicityDRMeasure;
import qut.pm.spm.measures.SimplicityDRSmallLogMeasure;
import qut.pm.spm.measures.SimplicityEdgeCountMeasure;
import qut.pm.spm.measures.SimplicityStructuralStochasticUniqMeasure;
import qut.pm.spm.measures.StochasticLogCachingMeasure;
import qut.pm.spm.measures.StochasticStructuralComplexity;
import qut.pm.spm.measures.TraceRatioMeasure;
import qut.pm.spm.playout.CachingPlayoutGenerator;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class ParadigmExamplesCycle3 {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private PetriNetFragmentParser parser;
	private List<StochasticLogCachingMeasure> netMeasures;
	private List<SPNQualityCalculator> evalCalcs;
	private PluginContext context = new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(),
			"paradigm_test");
	private final XLog LOG_E = converter.convertTextArgs("a b","a b","a b","a b","a b",
			 "a b","a b","a b","a b","a b",
			 "a b","a b","a b","a b","a b",
			 "a b","a b","a b","a b","a b",
			 "a b c","a b c",
			 "a b c c",
			 "e f");

	
	@Before
	public void before() {
		parser = new PetriNetFragmentParser();
		netMeasures = new ArrayList<>();
		netMeasures.add(new StochasticStructuralComplexity());
		LogStatsCache statsCache = new LogStatsCache();
		netMeasures.add(new SimplicityStructuralStochasticUniqMeasure(statsCache));
		netMeasures.add(new SimplicityEdgeCountMeasure(statsCache));
		netMeasures.add(new EdgeCount());
		netMeasures.add(new EntityCount());
		CachingPlayoutGenerator generator = new CachingPlayoutGenerator( 50 );
		netMeasures.add(new AdhesionDRMeasure(generator));
		netMeasures.add(new RelevanceDRMeasure(generator));
		netMeasures.add(new SimplicityDRMeasure(generator));
		netMeasures.add(new AdhesionDRSmallLogMeasure(generator));
		netMeasures.add(new RelevanceDRSmallLogMeasure(generator));
		netMeasures.add(new SimplicityDRSmallLogMeasure(generator));
		netMeasures.add(new ExistentialPrecisionMeasure(generator));
		netMeasures.add(new GenTraceFloorMeasure(generator,5));
		netMeasures.add( new GenTraceDiffMeasure(generator) );	
		netMeasures.add(new EntropicRelevanceZeroOrderMeasure(generator) );
		netMeasures.add( new TraceRatioMeasure(generator,2)  );
		evalCalcs = new ArrayList<>();
		evalCalcs.add(new EarthMoversCalculator() );
		evalCalcs.add(new EarthMoversTunedCalculator() );
		evalCalcs.add(new EntropyPrecisionRecallCalculator());
	}

	private void calcAndReport(String caseName, XLog xlog, AcceptingStochasticNet net) throws Exception {
		XLogInfoFactory.createLogInfo(xlog, NAME_CLASSIFIER);
		XLog slog = scale(xlog,20); 
		ProvenancedLog log = new ProvenancedLogImpl(slog, "fakeFileName");
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
	public void matchingHalfLogHalfModel() throws Exception {
		XLog log = converter.convertTextArgs("a b","c d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		calcAndReport("matchingHalfLogHalfModel", log, net);
	}

	@Test
	public void matchingHalfModel() throws Exception {
		XLog log = converter.convertTextArgs("a b","c d");
		XLog slog = scale(log,50); // 4.43
		// XLog slog = scale(log,1); // 5.55
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		calcAndReport("matchingHalfModel", slog, net);
	}
	
	@Test
	public void matchingHalfModelLogG() throws Exception {
		XLog logg = converter.convertTextArgs("a b c d","e f g");
		XLog slog = scale(logg,50); // 4.43
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> p2 -> {c 20.0} -> p3 -> {d 20.0}  -> End");
		calcAndReport("matchingHalfModel Log G", slog, net);
	}
		
	// pretty close to trace model
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
	public void paperExtremeExamplesCycle2() throws Exception {
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
		AcceptingStochasticNet netmidEminusHP= parser.createAcceptingNet("net", 
				  "Start -> {a 20.0} -> p1 -> {b 20.0} -> p2 -> {tau__1} -> p3 -> {c} -> p3 -> {tau__2 3.0} -> End");
		parser.addToAcceptingNet(netmidEminusHP,
				  "Start -> {a 20.0} -> p1 -> {tau__3} -> End");
		parser.addToAcceptingNet(netmidEminusHP,
				  "Start -> {a 20.0} -> p4 -> {x} -> End");
		calcAndReport("paperEg A~ E- (HP-)", log, netmidEminusHP);
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
	public void paperExtremeExamplesCycle3() throws Exception {
		AcceptingStochasticNet netAplusRplusSdoubleplus = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> End"); // or is this R~ ?
		calcAndReport("paperEg A+ R+ S++", LOG_E, netAplusRplusSdoubleplus);
		AcceptingStochasticNet netBMLoop = parser.createAcceptingNet("net", 
				  "Start -> {a 20.0} -> p1 -> {b 20.0} -> p2 -> {tau__1} -> p3 -> {c} -> p3 -> {tau__2 3.0} -> End");
		calcAndReport("paperEg A+ E+ S+~", LOG_E, netBMLoop);
		trace();
		flowerFreq(); 
		partialMajorTrace();
		matchingHalfModelLogG();
	}

	@Test
	public void flowerUnity() throws Exception {
		AcceptingStochasticNet flowerUnity = parser.createAcceptingNet("net", 
			 	  "Start -> {tau__1} -> p1");
		parser.addToAcceptingNet(flowerUnity,
				  "p1 -> {a} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerUnity,
				  "Start -> {b} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerUnity,
				  "Start -> {c} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerUnity,
				  "Start -> {e} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerUnity,
				  "Start -> {f} -> p1 -> {tau__2} -> End");
		// low TRG keeps DR relevance high
		calcAndReport("paperEg flower unity A- R- (NM) R+ (DR) S~", LOG_E, flowerUnity);
	}

	@Test
	public void trace() throws Exception {
		AcceptingStochasticNet traceModel = parser.createAcceptingNet("net", 
			 	  "Start -> {a__1 20.0} -> p1 -> {b__1 20.0} -> End");
		parser.addToAcceptingNet(traceModel,
				  "Start -> {a__2 2.0} -> p2 -> {b__2 2.0} -> p3 -> {c__1 2.0} -> End");
		parser.addToAcceptingNet(traceModel,
				  "Start -> {a__3 1.0} -> p4 -> {b__3 1.0} -> p5 -> {c__2 1.0} -> p6 -> {c__3 1.0}  -> End");
		parser.addToAcceptingNet(traceModel,
				  "Start -> {e 1.0} -> p7 -> {f 1.0} -> End");
		calcAndReport("trace A+ R+ (NM) R- (DR) S- (F+ P+)", LOG_E, traceModel);
	}
	
	@Test
	public void revTrace() throws Exception {
		XLog log = converter.convertTextArgs("a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b","a b","a b","a b","a b",
											 "a b c","a b c",
											 "a b c c",
											 "e f");
		AcceptingStochasticNet traceModel = parser.createAcceptingNet("net", 
			 	  "Start -> {b__1 20.0} -> p1 -> {a__1 20.0} -> End");
		parser.addToAcceptingNet(traceModel,
				  "Start -> {c__2 2.0} -> p2 -> {b__2 2.0} -> p3 -> {a__1 2.0} -> End");
		parser.addToAcceptingNet(traceModel,
				  "Start -> {c__3 1.0} -> p4 -> {c__3 1.0} -> p5 -> {b__2 1.0} -> p6 -> {a__3 1.0}  -> End");
		parser.addToAcceptingNet(traceModel,
				  "Start -> {f 1.0} -> p7 -> {e 1.0} -> End");
		// only earth movers picks this up
		calcAndReport("rev trace A~ R- (NM) R+ (DR maybe) S- (F- P-)", log, traceModel);
	}

	@Test
	public void partialTraceShort() throws Exception {
		XLog log = converter.convertTextArgs(
									"a b d","a b d","a b d","a b d","a b d",
									"a b d","a b d","a b d","a b d","a b d",
									"a b d","a b d","a b d","a b d","a b d",
									"a b d","a b d","a b d","a b d","a b d",
									"a b","b d");
		AcceptingStochasticNet partialTraceModel = parser.createAcceptingNet("net", 
			 	  "Start -> {a__1 10.0} -> p1 -> {b__1 10.0} -> End");
		parser.addToAcceptingNet(partialTraceModel,
				  "Start -> {b__2 1.0} -> p2 -> {d__1 1.0} -> End");
		// this might buff trace ratios
		calcAndReport("partial trace short A+~ (NM) A~(DR) R- (NM) R~ (DR) S+", log, partialTraceModel);
	}

	@Test
	public void partialTraceLong() throws Exception {
		XLog log = converter.convertTextArgs(
						"a b c d e f g","a b c d e f g","a b c d e f g","a b c d e f g","a b c d e f g",
						"a b c d e f g","a b c d e f g","a b c d e f g","a b c d e f g","a b c d e f g",
						"a b c d e f g","a b c d e f g","a b c d e f g","a b c d e f g","a b c d e f g",
						"a b c d e f g","a b c d e f g","a b c d e f g","a b c d e f g","a b c d e f g",
						"a b c d e f",
						"a b", "b c", "c d", "d e", "e f",
						"a b c", "b c d", "c d e", "d e f",
						"a b c d", "b c d e",
						"a b c d e", "b c d e f",
						"b c d e f g");
		AcceptingStochasticNet partialTraceModel = parser.createAcceptingNet("net", 
			 	  "Start -> {a 10.0} -> p1 -> {b 10.0} -> p2 -> {c 10.0} -> p3 -> {d 10.0} -> p4 -> {e 10.0} -> p5 -> {f 10.0} -> End");
		// buff trace ratios a bit, which hits DR simp and rel
		calcAndReport("partial trace long A~ R- (NM) R~ (DR) S+", log, partialTraceModel);
	}
	
	@Test
	public void partialMajorTrace() throws Exception {
		XLog log = converter.convertTextArgs(
						"a b c d e","a b c d e","a b c d e","a b c d e","a b c d e",
						"a b c d e","a b c d e","a b c d e","a b c d e","a b c d e",
						"a b c d e","a b c d e","a b c d e","a b c d e","a b c d e",
						"a b c d e","a b c d e","a b c d e","a b c d e","a b c d e",
						"a b", "b c", "c d", "d e" );
		AcceptingStochasticNet partialTraceModel = parser.createAcceptingNet("net", 
			 	  "Start -> {a 20.0} -> p1 -> {b 20.0} -> p2 -> {c 20.0} -> p3 -> {d 20.0}  -> End");
		// buff trace ratios a bit, which hits DR simp and rel
		calcAndReport("partial major trace long2 A~ R- (NM) R~ (DR) S+", log, partialTraceModel);
	}

	
	@Test
	public void flowerFreq() throws Exception {
		XLog log = converter.convertTextArgs("a b","a b","a b","a b","a b",
				 "a b","a b","a b","a b","a b",
				 "a b","a b","a b","a b","a b",
				 "a b","a b","a b","a b","a b",
				 "a b c","a b c",
				 "a b c c", 
				 "e f");
		AcceptingStochasticNet flowerFreq = parser.createAcceptingNet("net", 
			 	  "Start -> {tau__1} -> p1");
		parser.addToAcceptingNet(flowerFreq,
				  "p1 -> {a 20.0} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerFreq,
				  "Start -> {b 20.0} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerFreq,
				  "Start -> {c 2.0} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerFreq,
				  "Start -> {e 1.0} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerFreq,
				  "Start -> {f 1.0} -> p1 -> {tau__2} -> End");
		calcAndReport("flower freq", log, flowerFreq);		
	}

	@Test
	public void flowerInverseFreq() throws Exception {
		XLog log = converter.convertTextArgs("a b","a b","a b","a b","a b",
				 "a b","a b","a b","a b","a b",
				 "a b","a b","a b","a b","a b",
				 "a b","a b","a b","a b","a b",
				 "a b c","a b c",
				 "a b c c", 
				 "e f");
		AcceptingStochasticNet flowerFreq = parser.createAcceptingNet("net", 
			 	  "Start -> {tau__1} -> p1");
		parser.addToAcceptingNet(flowerFreq,
				  "p1 -> {a 1.0} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerFreq,
				  "Start -> {b 1.0} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerFreq,
				  "Start -> {c 10.0} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerFreq,
				  "Start -> {e 20.0} -> p1 -> {tau__2} -> End");
		parser.addToAcceptingNet(flowerFreq,
				  "Start -> {f 20.0} -> p1 -> {tau__2} -> End");
		calcAndReport("flower inverse freq", log, flowerFreq);		
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

	private XLog scale(XLog log, int scale) {
		XAttributeMap attrMap = new XAttributeMapImpl();
		XLog result = new XLogImpl(attrMap);
		for (XTrace trace: log) {
			for (int i=0; i<scale; i++) {
				result.add(trace);
			}
		}
		XLogInfoFactory.createLogInfo(result, NAME_CLASSIFIER);
		return result;
	}
	
}
