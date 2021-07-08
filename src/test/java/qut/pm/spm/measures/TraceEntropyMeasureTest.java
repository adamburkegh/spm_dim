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
		XLog log = converter.convertTextArgs("a b","c d","e f");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {e} -> p3 -> {f} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {g} -> p3 -> {h} -> End");
		assertMeasureEquals(1.5849, log,net);
	}

	@Test
	public void superUnityFitness() throws Exception {
		// This test is an example net with intersectionEntropy greater than logEntropy
		AcceptingStochasticNet anet = PetrinetExportUtils.readStochasticPNMLModel("src/test/resources/setm-s1ts20210623-115014-final.pnml");
		PluginContext uipc = 
				new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(), "spn_converter");	
		XLog log = (XLog) new OpenLogFileLiteImplPlugin().importFile(uipc, 
									"data" + File.separator + "BPIC2013_closed.xes");
		TraceEntropyFitness traceEntropyFitnessMeasure = new TraceEntropyFitness(measure);
		double mc = traceEntropyFitnessMeasure.calculate(log,anet, NAME_CLASSIFIER);
		// logEntropy 					~= 4.20 
		// modelEntropy 				~= 7.82 
		// intersectEntropy 			~= 5.81
		// intersectEntropyVsFullLog 	~= 0.76
		assertTrue(mc< 1.0);
	}
	
	private void assertMeasureEquals(double expected, XLog log, AcceptingStochasticNet net) {
		assertEquals(expected, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}
	
}
