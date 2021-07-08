package qut.pm.spm.measures;

import static org.junit.Assert.*;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class TraceRatio3MeasureTest {

	private static final double EPSILON = 0.001d;
	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private AbstractStochasticLogCachingMeasure measure3;
	private PetriNetFragmentParser parser;
	
	@Before
	public void setUp() {
		measure3 = new TraceRatioMeasure(3);
		parser = new PetriNetFragmentParser(); 
	}
	
	@Test
	public void zeroMatchShort() {
		XLog log = converter.convertTextArgs("a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertEquals(0.0d, measure3.calculate(log,net, NAME_CLASSIFIER), 0.01d);
	}

	@Test
	public void zeroShortMatchWithChoice() {
		XLog log = converter.convertTextArgs("a d","a e");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p2 -> {c 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}
	
	@Test
	public void zeroMatchSingle() {
		XLog log = converter.convertTextArgs("b","b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}
	
	@Test
	public void perfectMatchFullLength() {
		XLog log = converter.convertTextArgs("a b c", "a b c", "a d e");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> p2 -> {c 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {d 1.0} -> p3 -> {e 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}

	@Test
	public void perfectMatchSubtrace() {
		XLog log = converter.convertTextArgs("a b d e","a c f g");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	"Start -> {a 2.0} -> p1 -> {b 2.0} -> p2 -> {d 2.0} -> p3 -> {e 2.0} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {a 2.0} -> p1 -> {c 2.0} -> p4 -> {f 2.0} -> p5 -> {g 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}
	
	@Test
	public void partialMatchWithShort() {
		XLog log = converter.convertTextArgs("a b c","g h i", "b c", "d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 1.0} -> p2 -> {c 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {d 1.0} -> p3 -> {e 1.0} -> End");
		// 1/3*( |1| + |1| + |0| + |0|)
		assertMeasureEquals(1.0/3.0, log, net);
	}

	private void assertMeasureEquals(double expected, XLog log, AcceptingStochasticNet net) {
		assertEquals(expected, measure3.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}

	
	
}
