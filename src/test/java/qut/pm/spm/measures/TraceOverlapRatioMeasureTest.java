package qut.pm.spm.measures;

import static org.junit.Assert.assertEquals;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class TraceOverlapRatioMeasureTest {

	private static final double EPSILON = 0.001d;
	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private PetriNetFragmentParser parser;
	
	private TraceOverlapRatioMeasure measure;
	
	@Before
	public void setUp() {
		measure = new TraceOverlapRatioMeasure();
		parser = new PetriNetFragmentParser();
	}
	
	@Test
	public void zeroOverlap() {
		XLog log = converter.convertTextArgs("b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertEquals(0.0d, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}

	@Test
	public void perfectOverlap() {
		XLog log = converter.convertTextArgs("a","b c");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		parser.addToAcceptingNet(net, 
				  "Start -> {b 2.0} -> p1 -> {c 2.0} -> End");
		assertEquals(1.0d, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}
	
	@Test
	public void middlingOverlap() {
		XLog log = converter.convertTextArgs("a","b c");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {d 2.0} -> End");
		parser.addToAcceptingNet(net, 
				  "Start -> {b 2.0} -> p1 -> {c 2.0} -> End");
		assertEquals(0.5d, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}


	
}
