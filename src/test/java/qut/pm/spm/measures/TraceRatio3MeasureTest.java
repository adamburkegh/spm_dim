package qut.pm.spm.measures;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.StochasticPlayoutGenerator;

public class TraceRatio3MeasureTest extends MeasureTest{

	private PetriNetFragmentParser parser;
	
	@Before
	public void setUp() {
		measure = new TraceRatioMeasure(3);
		parser = new PetriNetFragmentParser(); 
	}
	
	@Test
	public void zeroMatchShort() {
		ProvenancedLog log = plog("a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertEquals(0.0d, measure.calculate(log,net, NAME_CLASSIFIER), 0.01d);
	}

	@Test
	public void zeroShortMatchWithChoice() {
		ProvenancedLog log = plog("a d","a e");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p2 -> {c 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}
	
	@Test
	public void zeroMatchSingle() {
		ProvenancedLog log = plog("b","b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}
	
	@Test
	public void perfectMatchFullLength() {
		ProvenancedLog log = plog("a b c", "a b c", "a d e");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> p2 -> {c 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {d 1.0} -> p3 -> {e 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}

	@Test
	public void perfectMatchSubtrace() {
		ProvenancedLog log = plog("a b d e","a c f g");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	"Start -> {a 2.0} -> p1 -> {b 2.0} -> p2 -> {d 2.0} -> p3 -> {e 2.0} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {a 2.0} -> p1 -> {c 2.0} -> p4 -> {f 2.0} -> p5 -> {g 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}
	
	@Test
	public void partialMatchWithShort() {
		ProvenancedLog log = plog("a b c","g h i", "b c", "d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 1.0} -> p2 -> {c 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {d 1.0} -> p3 -> {e 1.0} -> End");
		// 1/3*( |1| + |1| + |0| + |0|)
		assertMeasureEquals(1.0/3.0, log, net);
	}

	protected void assertMeasureEquals(double expected, ProvenancedLog log, AcceptingStochasticNet net) {
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(log.size());
		measure =  new TraceRatioMeasure(generator,3);		
		assertEquals(expected, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}

	
	
}
