package qut.pm.spm.measures;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.prom.helpers.PetrinetExportUtils;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.StochasticPlayoutGenerator;

public class TraceRatio2MeasureTest extends MeasureTest{

	private static Logger LOGGER = LogManager.getLogger();
	
	private PetriNetFragmentParser parser;
	
	@Before
	public void setUp() {
		super.setUp();
		measure = new TraceRatioMeasure(2);
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
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {c 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}
	
	@Test
	public void zeroShortMatchWithConcurrency() {
		ProvenancedLog log = plog("a d e","a f g");
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
		ProvenancedLog log = plog("a b","a c");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {c 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}

	@Test
	public void perfectMatchSubtrace() {
		ProvenancedLog log = plog("a b c", "a b c", "a d e");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> p2 -> {c 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {d 1.0} -> p3 -> {e 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}

	@Test
	public void partialMatchNoShort() {
		ProvenancedLog log = plog("a a","a b","b c","b c");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a__1 2.0} -> p1 -> {a__2 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a__1 2.0} -> p1 -> {b__1 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b__2 2.0} -> p2 -> {e 1.0} -> End");
		// 1/4*( |1| + |1| + |0| + |0|)
		assertMeasureEquals(0.5, log, net);
	}
	
	@Test
	public void partialMatchWithShort() {
		ProvenancedLog log = plog("a a","a b","b c", "d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a__1 2.0} -> p1 -> {a__2 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a__1 2.0} -> p1 -> {b__1 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b__2 2.0} -> p2 -> {e 1.0} -> End");
		// 1/4*( |1| + |1| + |0| + |0|)
		assertMeasureEquals(3.0/8.0, log, net);
	}

	protected void assertMeasureEquals(double expected, ProvenancedLog log, AcceptingStochasticNet net) {
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(log.size());
		measure =  new TraceRatioMeasure(generator,2);
		double val = measure.calculate(log,net, NAME_CLASSIFIER);
		if (val - expected > EPSILON || expected - val > EPSILON) {
			LOGGER.warn(measure.format());
			try {
				File tmpFile = File.createTempFile("tmm","pnml");
				PetrinetExportUtils.storePNMLModel(tmpFile,net.getNet());
				LOGGER.info("Model file");
				Scanner input = new Scanner(tmpFile);
				StringBuilder builder = new StringBuilder();
				while (input.hasNextLine())
				{
				   builder.append(input.nextLine());
				   builder.append("\n");
				}
				LOGGER.info(builder.toString());
				input.close();
			}catch (Exception e) {
				LOGGER.warn("Error generating model file",e);
			}
		}
		assertEquals(expected, val, EPSILON);
	}

	
	
}
