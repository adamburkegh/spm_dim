package qut.pm.spm.measures;

import static org.junit.Assert.*;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class EventRatioMeasureTest {

	private static final double EPSILON = 0.001d;
	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private EventRatioMeasure measure;
	private PetriNetFragmentParser parser;
	
	@Before
	public void setUp() {
		measure = new EventRatioMeasure();
		parser = new PetriNetFragmentParser(); 
	}
	
	@Test
	public void perfectMatchSingle() {
		XLog log = converter.convertTextArgs("a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(1.0d, log, net);
	}

	@Test
	public void perfectMatchWithChoice() {
		XLog log = converter.convertTextArgs("a","b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}

	
	@Test
	public void zeroMatch() {
		XLog log = converter.convertTextArgs("b","b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}
	
	@Test
	public void zeroMatchMultiActivity() {
		XLog log = converter.convertTextArgs("a","b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {c 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}

	@Test
	public void missingActivity() {
		XLog log = converter.convertTextArgs("a","a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 2.0} -> End");
		assertMeasureEquals(0.25, log, net );
	}

	@Test
	public void partialMatch() {
		// three activities six events a = 3/6, b = 2/6, c = 1/6
		XLog log = converter.convertTextArgs("a a","a b","b","c");
		// two activities two events a = 1/2, b = 1/2
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 2.0} -> End");
		// 1/3*( |1| + |1 - (1/2 - 1/3)/1/2| + |0|)
		assertMeasureEquals(0.55556, log,net);
	}

	private void assertMeasureEquals(double expected, XLog log, AcceptingStochasticNet net) {
		assertEquals(expected, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}

	
	
}
