package qut.pm.spm.measures;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.xeslite.plugin.OpenLogFileLiteImplPlugin;

import qut.pm.prom.helpers.ConsoleUIPluginContext;
import qut.pm.prom.helpers.HeadlessDefinitelyNotUIPluginContext;
import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.prom.helpers.PetrinetExportUtils;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.TraceFreqTest;
import qut.pm.spm.measures.TraceEntropyMeasure.TraceEntropyMeasurement;
import qut.pm.spm.playout.StochasticPlayoutGenerator;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class TraceEntropyMeasureTest {

	private static final double EPSILON = 0.001d;
	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private TraceEntropyMeasure measure;
	private PetriNetFragmentParser parser;
	
	@Before
	public void setUp() {
		measure = new TraceEntropyMeasure();
		parser = new PetriNetFragmentParser();
	}
	
	@Test
	public void emptyLog() {
		XLog log = converter.convertTextArgs();
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(0.0d, log,net);
	}

	@Test
	public void matchingSingletonLogModel() {
		XLog log = converter.convertTextArgs("a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(0.0d, log,net);
	}

	@Test
	public void matchingHalfHalfLogModel() {
		XLog log = converter.convertTextArgs("a b","c d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		assertMeasureEquals(1.0d, log,net);
	}

	
	@Test
	public void logSmallerPerfectFitness() {
		XLog log = converter.convertTextArgs("a b","c d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {e} -> p3 -> {f} -> End");
		assertMeasureEquals(1.0d, log,net);
	}

	@Test
	public void modelSmallerPerfectFitness() {
		XLog log = converter.convertTextArgs("a b","c d","e f");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {e} -> p3 -> {f} -> End");
		assertMeasureEquals(1.0d, log,net);
	}

	@Test
	public void wideChoiceIntersection() {
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
		assertMeasureEquals(1.5219, log,net);
	}

	@Test
	public void superUnityFitness() throws Exception {
		// This test is an example net with intersectionEntropy greater than logEntropy
		// Rounding in the measure limits to 1.0 
		AcceptingStochasticNet anet = PetrinetExportUtils.readStochasticPNMLModel("src/test/resources/setm-s1ts20210623-115014-final.pnml");
		XLog log = loadBPIC2013_closed();
		TraceEntropyFitness traceEntropyFitnessMeasure = new TraceEntropyFitness(measure);
		double mc = traceEntropyFitnessMeasure.calculate(log,anet, NAME_CLASSIFIER);
		// logEntropy 					~= 4.20 
		// modelEntropy 				~= 7.82 
		// intersectEntropy 			~= 5.81
		// intersectEntropyVsFullLog 	~= 0.76
		assertTrue(mc<= 1.0);
	}
	
	@Test
	public void nearSuperUnityPrecision1() throws Exception{
		// This test is an example net with intersectionEntropy greater than modelEntropy
		/*
		 *  Tree: /\  8.333333333333334
		 *  		(/\  8.333333333333334
		 *  			(@ [r2.0] 2.0833333333333335
		 *  				(\/  2.0833333333333335
		 *  					(Completed 2.0833333333333335)))
		 *  			(\/  6.25(tau 2.0833333333333335)
		 *  					 (Unmatched 2.0833333333333335)
		 *  					 (@ [r5.0] 2.0833333333333335
		 *  						(Accepted 2.0833333333333335))))
		 */
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createConcurrency();
		ProbProcessTreeNode pptConc1 = ProbProcessTreeFactory.createConcurrency();
		ProbProcessTreeNode pptLoop1 = ProbProcessTreeFactory.createLoop(2.0d);
		ProbProcessTreeNode pptChoice1 = ProbProcessTreeFactory.createChoice();
		pptChoice1.addChild( ProbProcessTreeFactory.createLeaf("Completed", 2.083333d ) );
		pptLoop1.addChild(pptChoice1);
		ProbProcessTreeNode pptChoice2 = ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptLoop2 = ProbProcessTreeFactory.createLoop(5.0d);
		pptLoop2.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 2.083333d ) ) ;
		pptChoice2.addChild( ProbProcessTreeFactory.createLeaf("Unmatched", 2.083333d ) );
		pptChoice2.addChild( pptLoop2 );
		pptConc1.addChild(pptLoop1);
		pptConc1.addChild(pptChoice2);
		ppt.addChild(pptConc1);
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		XLog log = loadBPIC2013_closed();
		measure = new TraceEntropyMeasure(new StochasticPlayoutGenerator(1000));
		TraceEntropyPrecision traceEntropyPrecisionMeasure = new TraceEntropyPrecision(measure);
		double mc = traceEntropyPrecisionMeasure.calculate(log,snet, NAME_CLASSIFIER);
		assertTrue(mc< 1.0);
	}
	
	@Test
	public void nearSuperUnityPrecision2() throws Exception{
		// This test is an example net with intersectionEntropy greater than modelEntropy
		/*
		 *  Tree: \/  1.0
		 *  		(->  1.0
		 *  			(Accepted 1.0)
		 *  			(/\  1.0
		 *  				(Completed 0.5)
		 *  				(@ [r10.0] 0.5
		 *  					(Accepted 0.5))))
		 */
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptConc1 = ProbProcessTreeFactory.createConcurrency();
		ProbProcessTreeNode pptLoop1 = ProbProcessTreeFactory.createLoop(10.0d);
		pptLoop1.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 0.5d ) ) ;
		pptConc1.addChild(ProbProcessTreeFactory.createLeaf("Completed", 0.5d )) ;
		pptConc1.addChild(pptLoop1);
		ProbProcessTreeNode pptSeq1 = ProbProcessTreeFactory.createSequence();
		pptSeq1.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 1.0d ) );
		pptSeq1.addChild(pptConc1);
		ppt.addChild(pptSeq1);
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		XLog log = loadBPIC2013_closed();
		measure = new TraceEntropyMeasure(new StochasticPlayoutGenerator(1000));
		TraceEntropyPrecision traceEntropyPrecisionMeasure = new TraceEntropyPrecision(measure);
		double mc = traceEntropyPrecisionMeasure.calculate(log,snet, NAME_CLASSIFIER);
		assertTrue(mc< 1.0);
	}
	
	@Test
	public void nearSuperUnityPrecision3() throws Exception{
		// Tree: \/  0.11640211640211641
		// 			(->  0.11640211640211641
		//				(->  0.11640211640211641
		//					(\/  0.11640211640211641
		//						(@ [r7.0] 0.058201058201058205
		//							(Accepted 0.058201058201058205))
		//						(tau 0.058201058201058205))
		//					(Completed 0.11640211640211641)))
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptChoice1= ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptLoop1 = ProbProcessTreeFactory.createLoop(7.0d);
		pptLoop1.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 0.058201058201058205d ) ) ;
		pptChoice1.addChild(pptLoop1);
		pptChoice1.addChild(ProbProcessTreeFactory.createSilent(0.058201058201058205) );
		ProbProcessTreeNode pptSeq2= ProbProcessTreeFactory.createSequence();
		pptSeq2.addChild(pptChoice1);
		pptSeq2.addChild( ProbProcessTreeFactory.createLeaf("Completed",0.11640211640211641)  );
		ProbProcessTreeNode pptSeq1= ProbProcessTreeFactory.createSequence();
		pptSeq1.addChild(pptSeq2);
		ppt.addChild(pptSeq1);
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		XLog log = loadBPIC2013_closed();
		measure = new TraceEntropyMeasure(new StochasticPlayoutGenerator(1000));
		TraceEntropyPrecision traceEntropyPrecisionMeasure = new TraceEntropyPrecision(measure);
		double mc = traceEntropyPrecisionMeasure.calculate(log,snet, NAME_CLASSIFIER);
		// logEntropy 					~= ? 
		// modelEntropy 				~= ? 
		// intersectEntropy 			~= ?
		// intersectEntropyVsFullLog 	~= ?
		assertTrue(mc< 1.0);
	}

	@Test
	public void nearSuperUnityPrecision4() throws Exception{
		// Tree:
		//		\/  6.0
		//		  ->  6.0
		//		    ->  6.0
		//		      \/  6.0
		//		        Accepted 4.0
		//		        \/  2.0
		//		          Accepted 1.3333333333333333
		//		          Queued 0.6666666666666666
		//		    \/  6.0
		//		      Completed 6.0
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptChoice2= ProbProcessTreeFactory.createChoice();
		pptChoice2.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 1.3333333333333333 ) ) ;
		pptChoice2.addChild( ProbProcessTreeFactory.createLeaf("Queued", 0.6666666666666666 ) ) ;
		ProbProcessTreeNode pptChoice1= ProbProcessTreeFactory.createChoice();
		pptChoice1.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 4.0 ) ) ;
		pptChoice1.addChild(pptChoice2);
		ProbProcessTreeNode pptSeq2 = ProbProcessTreeFactory.createSequence();
		pptSeq2.addChild(pptChoice1);
		ProbProcessTreeNode pptSeq1 = ProbProcessTreeFactory.createSequence();
		ProbProcessTreeNode pptChoice3 = ProbProcessTreeFactory.createChoice();
		pptChoice3.addChild(ProbProcessTreeFactory.createLeaf("Completed", 6.0 ));
		pptSeq1.addChild(pptSeq2);
		pptSeq1.addChild(pptChoice3);
		ppt.addChild(pptSeq1);
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		XLog log = loadBPIC2013_closed();
		measure = new TraceEntropyMeasure(new StochasticPlayoutGenerator(1000));
		TraceEntropyPrecision traceEntropyPrecisionMeasure = new TraceEntropyPrecision(measure);
		double mc = traceEntropyPrecisionMeasure.calculate(log,snet, NAME_CLASSIFIER);
		assertTrue("Entropy precision = " + mc, mc< 1.0);
	}

	@Test
	public void roundedSuperUnityPrecision() throws Exception{
		// Tree:
		//		  ->  9
		//		      \/  9
		//		        Accepted 8
		//		        Queued 1
		//		   	  Completed 9
		ProbProcessTreeNode pptChoice1 = ProbProcessTreeFactory.createChoice();
		pptChoice1.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 8 ) ) ;
		pptChoice1.addChild( ProbProcessTreeFactory.createLeaf("Queued", 1 ) ) ;
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createSequence();
		ppt.addChild(pptChoice1);
		ppt.addChild( ProbProcessTreeFactory.createLeaf("Completed", 9.0 ) );
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		TraceFreq log = new TraceFreq();
		log.putFreq( TraceFreqTest.toTrace("Accepted Completed"), 4.0 );
		log.putFreq( TraceFreqTest.toTrace("Queued Completed"), 1.0 );
		log.putFreq( TraceFreqTest.toTrace("Buffer"), 8.0 );
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(1000);
		measure = new TraceEntropyMeasure(generator);
		measure.setPrecalculatedLog(log);
		TraceFreq playout = playoutAndScale(snet, log, generator);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasures(playout);
		assertTrue( "Entropy precision = " + tem.getEntropyPrecision(),tem.getEntropyPrecision() <= 1.0);
	}

	private TraceFreq playoutAndScale(AcceptingStochasticNet snet, TraceFreq log,
			StochasticPlayoutGenerator generator) {
		TraceFreq playout = generator.buildPlayoutTraceFreq(snet);
		generator.scaleTo(playout,(int)log.getTraceTotal());
		return playout;
	}

	@Test
	public void nearSuperUnityLoopPrecision1(){
		// This was added as part of diagnosing a bug for loops in ProbProcessTreeConverter
		// Tree:
		//	    ->  0.013061224489795914
		//	      Accepted 0.013061224489795914
		//	      @ [r2.0] 0.013061224489795914
		//	        Queued 0.013061224489795914
		//	      Completed 0.013061224489795914
		ProbProcessTreeNode pptLoop1 = ProbProcessTreeFactory.createLoop(2.0d);
		pptLoop1.addChild( ProbProcessTreeFactory.createLeaf("Queued", 0.013061224489795914 ) );
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createSequence();
		ppt.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 0.013061224489795914 ) ) ;
		ppt.addChild(pptLoop1);
		ppt.addChild( ProbProcessTreeFactory.createLeaf("Completed", 0.013061224489795914 ) ) ;
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		TraceFreq log = new TraceFreq();
		log.putFreq( TraceFreqTest.toTrace("Accepted Queued Completed"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("Accepted Completed"), 1754.0 );
		log.putFreq( TraceFreqTest.toTrace("Buffer"), 5798.0 );
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(1000);
		measure = new TraceEntropyMeasure(generator);
		measure.setPrecalculatedLog(log);
		TraceFreq playout = generator.buildPlayoutTraceFreq(snet);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasures(playout);
		// assertEquals()
		// logEntropy 					~= ? 
		// modelEntropy 				~= ? 
		// intersectEntropy 			~= ?
		// intersectEntropyVsFullLog 	~= ?
		assertTrue( "Entropy precision = " + tem.getEntropyPrecision(),tem.getEntropyPrecision() < 1.0);
	}


	@Test
	public void nearSuperUnityLoopPrecision2(){
		// This was added as part of diagnosing a bug for loops in ProbProcessTreeConverter
		// Tree:
		//	    ->  1
		//	      Accepted 1
		//	      @ [r2.0] 1
		//	        Queued 1
		//	      Completed 1
		ProbProcessTreeNode pptLoop1 = ProbProcessTreeFactory.createLoop(2.0d);
		pptLoop1.addChild( ProbProcessTreeFactory.createLeaf("Queued", 1 ) );
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createSequence();
		ppt.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 1 ) ) ;
		ppt.addChild(pptLoop1);
		ppt.addChild( ProbProcessTreeFactory.createLeaf("Completed", 1 ) ) ;
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		TraceFreq log = new TraceFreq();
		log.putFreq( TraceFreqTest.toTrace("Accepted Queued Completed"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("Accepted Completed"), 1754.0 );
		log.putFreq( TraceFreqTest.toTrace("Buffer"), 5798.0 );
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(1000);
		measure = new TraceEntropyMeasure(generator);
		measure.setPrecalculatedLog(log);
		TraceFreq playout = generator.buildPlayoutTraceFreq(snet);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasures(playout);
		assertTrue( "Entropy precision = " + tem.getEntropyPrecision(),tem.getEntropyPrecision() < 1.0);
	}
	
	@Test
	public void projectionOneIntersectTrace() {
		TraceFreq log = new TraceFreq();
		log.putFreq( TraceFreqTest.toTrace("a b"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("e f"), 2.0 );
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(1000);
		measure = new TraceEntropyMeasure(generator);
		measure.setPrecalculatedLog(log);
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		TraceFreq playout = playoutAndScale(net, log, generator);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasures(playout);
		assertEquals(1.0,tem.getEntropyProjectPrecision(),EPSILON);
		assertEquals(1.0,tem.getEntropyProjectFitness(),EPSILON);
	}

	@Test
	public void projectionTwoIntersectTraces() {
		TraceFreq log = new TraceFreq();
		log.putFreq( TraceFreqTest.toTrace("a b"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("c d"), 4.0 );
		log.putFreq( TraceFreqTest.toTrace("e f"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("g h"), 2.0 );
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(8);
		measure = new TraceEntropyMeasure(generator);
		measure.setPrecalculatedLog(log);
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {i 2.0} -> p3 -> {j 2.0} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {k} -> p2 -> {l} -> End");
		TraceFreq playout = playoutAndScale(net, log, generator);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasures(playout);
		assertEquals(0.60251d,tem.getEntropyProjectPrecision(),EPSILON);
		assertEquals(0.7918d,tem.getEntropyProjectFitness(),EPSILON);
	}

	
	@Test
	public void projectionModelGreater() {
		TraceFreq log = new TraceFreq();
		log.putFreq( TraceFreqTest.toTrace("a b"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("c d"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("e f"), 4.0 );
		log.putFreq( TraceFreqTest.toTrace("g h"), 2.0 );
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(8);
		measure = new TraceEntropyMeasure(generator);
		measure.setPrecalculatedLog(log);
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> p1 -> {b 2.0} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {i} -> p3 -> {j} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {k} -> p3 -> {l} -> End");		
		TraceFreq playout = playoutAndScale(net, log, generator);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasures(playout);
		assertEquals(0.724276,tem.getEntropyProjectPrecision(),EPSILON);
		assertEquals(0.71332d,tem.getEntropyProjectFitness(),EPSILON);
	}
	
	@Test
	public void projectionPerfectPrecisionFitnessDespiteLogGreater() {
		TraceFreq log = new TraceFreq();
		log.putFreq( TraceFreqTest.toTrace("a b"), 4.0 );
		log.putFreq( TraceFreqTest.toTrace("c d"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("e f"), 2.0 );
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(8);
		measure = new TraceEntropyMeasure(generator);
		measure.setPrecalculatedLog(log);
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {h 2.0} -> p3 -> {i 2.0} -> End");
		TraceFreq playout = playoutAndScale(net, log, generator);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasures(playout);
		assertEquals(1.0d,tem.getEntropyProjectPrecision(),EPSILON);
		assertEquals(1.0d,tem.getEntropyProjectFitness(),EPSILON);
	}
	
	@Test
	public void projectionLogGreater() {
		TraceFreq log = new TraceFreq();
		log.putFreq( TraceFreqTest.toTrace("a b"), 4.0 );
		log.putFreq( TraceFreqTest.toTrace("c d"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("e f"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("g h"), 2.0 );
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(8);
		measure = new TraceEntropyMeasure(generator);
		measure.setPrecalculatedLog(log);
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {i 2.0} -> p3 -> {j 2.0} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {k 2.0} -> p3 -> {l 2.0} -> End");		
		TraceFreq playout = playoutAndScale(net, log, generator);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasures(playout);
		assertEquals(0.6d,tem.getEntropyProjectPrecision(),EPSILON);
		assertEquals(0.7919d,tem.getEntropyProjectFitness(),EPSILON);
	}
	
	@Test
	public void projectionSuperUnityPrecision() throws Exception{
		// Tree:
		//		  \/  54
		//		      /\  3
		//		        @ [r2.0] 1
		//		          Accepted 1
		//		        Accepted 1
		//		        @ [r2.0] 1
		//		          Completed 1
		//		      Accepted 51
		ProbProcessTreeNode pptLoop1 = ProbProcessTreeFactory.createLoop(2.0d);
		pptLoop1.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 1 ) );
		ProbProcessTreeNode pptLoop2 = ProbProcessTreeFactory.createLoop(2.0d);
		pptLoop2.addChild( ProbProcessTreeFactory.createLeaf("Completed", 1 ) );
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptConc1 = ProbProcessTreeFactory.createConcurrency();
		pptConc1.addChild(pptLoop1);
		pptConc1.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 1 ) );
		pptConc1.addChild(pptLoop2);
		ppt.addChild(pptConc1);
		ppt.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 51 ) ) ;
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		XLog log = loadBPIC2013_closed();
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(1000);
		measure = new TraceEntropyMeasure(generator);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasure(log,snet,NAME_CLASSIFIER);
		assertTrue("Super unity precision:" + tem.getEntropyProjectPrecision(),
					tem.getEntropyProjectPrecision() <= 1);
		assertTrue("Super unity fitness" + tem.getEntropyProjectFitness(),
				tem.getEntropyProjectFitness() <= 1);
	}
	
	@Test
	public void projectionNegativeLeftover() {
		TraceFreq log = new TraceFreq();
		log.putFreq( TraceFreqTest.toTrace("a b"), 4.0 );
		log.putFreq( TraceFreqTest.toTrace("c d"), 3.0 );
		log.putFreq( TraceFreqTest.toTrace("e f"), 2.0 );
		log.putFreq( TraceFreqTest.toTrace("g h"), 2.0 );
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(9);
		measure = new TraceEntropyMeasure(generator);
		TraceFreq playout = new TraceFreq();
		playout.putFreq( TraceFreqTest.toTrace("a b"), 4.0 );
		playout.putFreq( TraceFreqTest.toTrace("c d"), 2.0000000000001 );
		playout.forceTraceTotal( 6.0d);
		measure.setPrecalculatedLog(log);
		TraceEntropyMeasurement tem = measure.calculateTraceEntropyMeasures(playout);
		assertEquals(1.0d,tem.getEntropyProjectPrecision(),EPSILON);
		assertEquals(0.8122d,tem.getEntropyProjectFitness(),EPSILON);
	}

	private XLog loadBPIC2013_closed() throws Exception {
		return loadResourceLog("BPIC2013_closed.xes" );
	}
	
	private XLog loadResourceLog(String logName) throws Exception {
		PluginContext uipc = 
				new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(), "spn_converter");	
		XLog log = (XLog) new OpenLogFileLiteImplPlugin().importFile(uipc, 
									"src" + File.separator + "test" + File.separator 
								   + "resources" + File.separator + logName);
		return log;
	}	
	
	private void assertMeasureEquals(double expected, XLog log, AcceptingStochasticNet net) {
		assertEquals(expected, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}
	
}
