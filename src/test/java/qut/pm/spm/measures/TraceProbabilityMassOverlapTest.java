package qut.pm.spm.measures;

import org.junit.Test;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.log.ProvenancedLog;

public class TraceProbabilityMassOverlapTest extends MeasureTest {

	@Test
	public void emptyLog() {
		ProvenancedLog log = plog();
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(0.0d, log,net);
	}

	@Test
	public void matchingSingletonLogModel() {
		ProvenancedLog log = plog("a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(1.0d, log,net);
	}

	@Test
	public void matchingHalfHalfLogModel() {
		ProvenancedLog log = plog("a b","c d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {c} -> p2 -> {d} -> End");
		assertMeasureEquals(1.0d, log,net);
	}

	
	@Test
	public void logSmallerPerfectFitness() {
		ProvenancedLog log = plog("a b","c d");
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
		ProvenancedLog log = plog("a b","c d","e f");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a} -> p1 -> {b} -> End");
		parser.addToAcceptingNet(net,
				  "Start -> {e} -> p3 -> {f} -> End");
		assertMeasureEquals(2.0/3.0, log,net);
	}

	@Test
	public void wideChoiceIntersection() {
		ProvenancedLog log = plog("a b","a b","a b","a b",
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

	
}
