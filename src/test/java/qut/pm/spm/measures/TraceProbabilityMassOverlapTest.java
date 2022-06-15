package qut.pm.spm.measures;

import static org.junit.Assert.assertEquals;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class TraceProbabilityMassOverlapTest {

	private static final double EPSILON = 0.001d;
	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private TraceProbabilityMassOverlap measure;
	private PetriNetFragmentParser parser;
	
	@Before
	public void setUp() {
		measure = new TraceProbabilityMassOverlap();
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
		assertMeasureEquals(1.0d, log,net);
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
		assertMeasureEquals(0.667d, log,net);
	}

	@Test
	public void modelSmaller() {
		XLog log = converter.convertTextArgs("a b","c d","e f");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {e} -> p3 -> {f} -> End");
		assertMeasureEquals(2.0/3.0, log,net);
	}

	@Test
	public void wideChoiceIntersection() {
		XLog log = converter.convertTextArgs("a b","a b","a b","a b",
										     "c d","c d","c d","c d",
										     "e f","e f","e f","e f");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {e} -> p3 -> {f} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {g} -> p4 -> {h} -> End");
		assertMeasureEquals(0.75, log,net);
	}
	
	private void assertMeasureEquals(double expected, XLog log, AcceptingStochasticNet net) {
		assertEquals(expected, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}

	
}
