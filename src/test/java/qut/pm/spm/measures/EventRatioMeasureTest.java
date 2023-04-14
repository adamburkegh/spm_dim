package qut.pm.spm.measures;

import static org.junit.Assert.assertEquals;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.log.ProvenancedLogImpl;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
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
		ProvenancedLog log = plog("a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(1.0d, log, net);
	}

	@Test
	public void perfectMatchWithChoice() {
		ProvenancedLog log = plog("a","b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 2.0} -> End");
		assertMeasureEquals(1.0, log, net );
	}

	
	@Test
	public void zeroMatch() {
		ProvenancedLog log = plog("b","b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}
	
	@Test
	public void zeroMatchMultiActivity() {
		ProvenancedLog log = plog("a","b");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {c 2.0} -> End");
		assertMeasureEquals(0.0, log, net );
	}

	@Test
	public void missingActivity() {
		ProvenancedLog log = plog("a","a");
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 2.0} -> End");
		assertMeasureEquals(0.25, log, net );
	}

	@Test
	public void partialMatch() {
		// three activities six events a = 3/6, b = 2/6, c = 1/6
		ProvenancedLog log = plog("a a","a b","b","c");
		// two activities two events a = 1/2, b = 1/2
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  "Start -> {a 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 2.0} -> End");
		// 1/3*( |1| + |1 - (1/2 - 1/3)/1/2| + |0|)
		assertMeasureEquals(0.55556, log,net);
	}

	@Test
	public void zeroSETMPPTEg() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptChoice1 = ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptChoice2 = ProbProcessTreeFactory.createChoice();
		ProbProcessTreeNode pptChoice3 = ProbProcessTreeFactory.createChoice();
		pptChoice3.addChild( ProbProcessTreeFactory.createLeaf("Completed",1.0d) );
		pptChoice3.addChild( ProbProcessTreeFactory.createSilent(1.0d) );
		pptChoice2.addChild( pptChoice3 );
		pptChoice2.addChild( ProbProcessTreeFactory.createLeaf("Accepted", 1.0d) );
		ProbProcessTreeNode pptConc1 = ProbProcessTreeFactory.createConcurrency();
		pptConc1.addChild( ProbProcessTreeFactory.createLeaf("Unmatched",1.0d)  );
		pptChoice1.addChild( pptChoice2 );
		pptChoice1.addChild( pptConc1 );
		ProbProcessTreeNode pptSeq1= ProbProcessTreeFactory.createSequence();
		ProbProcessTreeNode pptLoop1 = ProbProcessTreeFactory.createLoop(2.0d);
		ProbProcessTreeNode pptLoop2 = ProbProcessTreeFactory.createLoop(2.0d);
		pptLoop2.addChild( ProbProcessTreeFactory.createLeaf("Queued", 1.0d ) ) ;
		pptLoop1.addChild(pptLoop2);
		pptSeq1.addChild (  ProbProcessTreeFactory.createLeaf("Queued", 1.0d ));
		pptSeq1.addChild (  pptLoop1 );
		pptSeq1.addChild (  ProbProcessTreeFactory.createLeaf("Unmatched", 1.0d ));
		ppt.addChild(  pptChoice1 );
		ppt.addChild(  pptSeq1 );
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		ProvenancedLog log = plog("Queued","Completed","Unmatched",
											 "Queued","Completed","Unmatched",
											 "Queued","Completed","Unmatched");
		assertMeasureEquals(0.596d, log, snet);
	}
	
	private void assertMeasureEquals(double expected, ProvenancedLog log, AcceptingStochasticNet net) {
		assertEquals(expected, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}

	private ProvenancedLog plog(String ... args) {
		XLog log = converter.convertTextArgs(args);
		ProvenancedLog plog = new ProvenancedLogImpl(log,"testDummyFileName");
		return plog;
	}

	
}
