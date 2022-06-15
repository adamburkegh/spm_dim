package qut.pm.spm.measures;

import static org.junit.Assert.*;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.playout.StochasticPlayoutGenerator;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class EarthMoversTraceMeasureTest {

	private static final double EPSILON = 0.001d;
	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private PetriNetFragmentParser parser;
	
	private EarthMoversTraceMeasure measure;
	
	@Before
	public void setUp() {
		measure = new EarthMoversTraceMeasure();
		parser = new PetriNetFragmentParser();
	}
	
	@Test
	public void zeroOverlap() {
		XLog log = converter.convertTextArgs("b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(log.size());
		measure = new EarthMoversTraceMeasure(generator);
		assertEquals(0.0d, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}

	@Test
	public void zeroOverlapWithScaling() {
		XLog log = converter.convertTextArgs("b","b","b");
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
		XLog log = converter.convertTextArgs("a","a","b c","b c");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {d 3.0} -> End");
		parser.addToAcceptingNet(net, 
				  "Start -> {b 1.0} -> p1 -> {c 2.0} -> End");
		assertEquals(0.25d, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}


}
