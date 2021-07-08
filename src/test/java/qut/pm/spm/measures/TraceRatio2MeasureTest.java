package qut.pm.spm.measures;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.prom.helpers.PetrinetExportUtils;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class TraceRatio2MeasureTest {

	private static Logger LOGGER = LogManager.getLogger();
	
	private static final double EPSILON = 0.001d;
	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private TraceRatioMeasure measure2;
	private PetriNetFragmentParser parser;
	
	@Before
	public void setUp() {
		measure2 = new TraceRatioMeasure(2);
		parser = new PetriNetFragmentParser(); 
	}
	
	@Test
	public void zeroMatchShort() {
		XLog log = converter.convertTextArgs("a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertEquals(0.0d, measure2.calculate(log,net, NAME_CLASSIFIER), 0.01d);
	}

	@Test
	public void zeroShortMatchWithChoice() {
		XLog log = converter.convertTextArgs("a d","a e");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {c 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}
	
	@Test
	public void zeroShortMatchWithConcurrency() {
		XLog log = converter.convertTextArgs("a d e","a f g");
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
		XLog log = converter.convertTextArgs("a b","a c");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {c 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}

	@Test
	public void perfectMatchSubtrace() {
		XLog log = converter.convertTextArgs("a b c", "a b c", "a d e");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> p2 -> {c 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p1 -> {d 1.0} -> p3 -> {e 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}

	@Test
	public void partialMatchNoShort() {
		XLog log = converter.convertTextArgs("a a","a b","b c","b c");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a__1 2.0} -> p1 -> {a__2 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a__1 2.0} -> p1 -> {b__1 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b__2 2.0} -> p2 -> {e 1.0} -> End");
		// 1/4*( |1| + |1| + |0| + |0|)
		assertMeasureEquals(0.5, log, net);
	}
	
	@Test
	public void partialMatchWithShort() {
		XLog log = converter.convertTextArgs("a a","a b","b c", "d");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a__1 2.0} -> p1 -> {a__2 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a__1 2.0} -> p1 -> {b__1 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b__2 2.0} -> p2 -> {e 1.0} -> End");
		// 1/4*( |1| + |1| + |0| + |0|)
		assertMeasureEquals(3.0/8.0, log, net);
	}

	private void assertMeasureEquals(double expected, XLog log, AcceptingStochasticNet net) {
		double val = measure2.calculate(log,net, NAME_CLASSIFIER);
		if (val - expected > EPSILON || expected - val > EPSILON) {
			LOGGER.warn(measure2.format());
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
