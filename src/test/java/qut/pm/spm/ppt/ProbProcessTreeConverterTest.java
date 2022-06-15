package qut.pm.spm.ppt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import qut.pm.prom.helpers.NodeMapper;
import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.prom.helpers.PetrinetExportUtils;
import qut.pm.prom.helpers.StochasticPetriNetUtils;
import qut.pm.spm.AcceptingStochasticNet;

public class ProbProcessTreeConverterTest {

	private static final double EPSILON = 0.001;

	private static Logger LOGGER = LogManager.getLogger();
	
	private PetriNetFragmentParser parser;
	private ProbProcessTreeConverter converter;

	@Before
	public void setUp() {
		converter = new ProbProcessTreeConverter();
		parser = new PetriNetFragmentParser();
	}
	
	public void convertsTo(AcceptingStochasticNet expected, ProbProcessTree ppt ) {
		StochasticNet pptNet = converter.convertToSNet(ppt).getNet();
		boolean result = 
				StochasticPetriNetUtils.areEqual(expected.getNet(), 
				pptNet );
		checkNets(result,expected, pptNet);
		assertTrue(result);
	}

	private void checkNets(boolean result, AcceptingStochasticNet expected, StochasticNet pptNet) {
		if (!result) {
			LOGGER.info("Expected :: " + PetrinetExportUtils.convertPetrinetToDOT(expected.getNet())) ;
			LOGGER.info("Result :: " + PetrinetExportUtils.convertPetrinetToDOT(pptNet)) ;
		}
	}

	
	@Test
	public void convertLeaf() {
		ProbProcessTree ppt = ProbProcessTreeFactory.createLeaf("a",1.0d);
		AcceptingStochasticNet expected = parser.createAcceptingNet("ppt", "Start -> {a 1.0} -> End");
		convertsTo(expected, ppt);
	}

	@Test
	public void convertSilent() {
		ProbProcessTree ppt = ProbProcessTreeFactory.createSilent(2.0d);
		AcceptingStochasticNet expected = parser.createAcceptingNet("ppt", "Start -> {tau 2.0} -> End");
		convertsTo(expected, ppt);
	}

	@Test
	public void convertSequence() {
		ProbProcessTree la = ProbProcessTreeFactory.createLeaf("a",1.0d);
		ProbProcessTree lb = ProbProcessTreeFactory.createLeaf("b",1.0d);
		ProbProcessTreeNode seq = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		seq.addChildren(la,lb);
		AcceptingStochasticNet expected = parser.createAcceptingNet("ppt", 
				"Start -> {a 1.0} -> sp1 -> {b 1.0} -> End");
		convertsTo(expected, seq);
	}
	
	@Test
	public void convertChoice() {
		ProbProcessTree la = ProbProcessTreeFactory.createLeaf("a",1.0d);
		ProbProcessTree lb = ProbProcessTreeFactory.createLeaf("b",1.0d);
		ProbProcessTreeNode choice = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		choice.addChildren(la,lb);
		AcceptingStochasticNet expected = parser.createAcceptingNet("ppt", 
				"Start -> {a} -> End");
		parser.addToAcceptingNet(expected,
				"Start -> {b} -> End");
		convertsTo(expected, choice);
	}

	@Test
	public void convertConcurrent() {
		ProbProcessTree la = ProbProcessTreeFactory.createLeaf("a",1.0d);
		ProbProcessTree lb = ProbProcessTreeFactory.createLeaf("b",1.0d);
		ProbProcessTreeNode conc = ProbProcessTreeFactory.createNode(PPTOperator.CONCURRENCY);
		conc.addChildren(la);
		conc.addChildren(lb);
		AcceptingStochasticNet expected = parser.createAcceptingNet("ppt", 
				"Start -> {tau__1} -> pci1 -> {a} -> pco1 -> {tau__2} -> End");
		NodeMapper nme = parser.addToAcceptingNet(expected,
				"Start -> {tau__1} -> pci2 -> {b} -> pco2 -> {tau__2} -> End");
		StochasticNet pptNet = converter.convertToSNet(conc).getNet();
		NodeMapper nmp = new NodeMapper(); 
		Transition tauin = findUniqueTransition(pptNet,"taucin");
		Transition tauout = findUniqueTransition(pptNet,"taucexit");
		Transition ta = findUniqueTransition(pptNet,"a");
		Transition tb = findUniqueTransition(pptNet,"b");
		nmp.put(tauin,"tau__1");
		nmp.put(tauout,"tau__2");
		nmp.put(ta,"a");
		nmp.put(tb,"b");
		boolean result = StochasticPetriNetUtils.areEqualWithDupes(expected.getNet(), 
				pptNet, nme, nmp );
		checkNets(result,expected, pptNet);
		assertTrue( result );		
	}

	@Test
	public void convertLoop() {
		ProbProcessTree la = ProbProcessTreeFactory.createLeaf("a",1.0d);
		ProbProcessTreeNode loop = ProbProcessTreeFactory.createLoop(2.0d);
		loop.addChildren(la);
		AcceptingStochasticNet expected = parser.createAcceptingNet("ppt", 
				"Start -> {tau__1} -> midloop -> {tau__2} -> End");
		NodeMapper nme = parser.addToAcceptingNet(expected,
				"midloop -> {a} -> midloop");
		StochasticNet pptNet = converter.convertToSNet(loop).getNet();
		NodeMapper nmp = new NodeMapper(); 
		Transition tauin = findUniqueTransition(pptNet,"taulin");
		Transition tauout = findUniqueTransition(pptNet,"taulexit");
		Transition ta = findUniqueTransition(pptNet,"a");
		nmp.put(tauin,"tau__1");
		nmp.put(tauout,"tau__2");
		nmp.put(ta,"a");
		assertTrue( StochasticPetriNetUtils.areEqualWithDupes(expected.getNet(), 
				pptNet, nme, nmp ) );		
	}
	
	@Test
	public void convertLoop2() {
		ProbProcessTree la = ProbProcessTreeFactory.createLeaf("a",3.0d);
		ProbProcessTreeNode loop = ProbProcessTreeFactory.createLoop(2.0);
		loop.addChildren(la);
		AcceptingStochasticNet expected = parser.createAcceptingNet("ppt", 
				"Start -> {tau__1 3.0} -> midloop -> {tau__2 1.5} -> End");
		NodeMapper nme = parser.addToAcceptingNet(expected,
				"midloop -> {a 1.5} -> midloop");
		StochasticNet pptNet = converter.convertToSNet(loop).getNet();
		NodeMapper nmp = new NodeMapper(); 
		TimedTransition tauin = findUniqueTransition(pptNet,"taulin");
		TimedTransition tauout = findUniqueTransition(pptNet,"taulexit");
		TimedTransition ta = findUniqueTransition(pptNet,"a");
		// mapping process doesn't compare weights properly
		assertEquals(3.0d, tauin.getWeight(), EPSILON);
		assertEquals(1.5d, tauout.getWeight(), EPSILON);
		assertEquals(1.5d, ta.getWeight(), EPSILON);
		nmp.put(tauin,"tau__1");
		nmp.put(tauout,"tau__2");
		nmp.put(ta,"a");
		assertTrue( StochasticPetriNetUtils.areEqualWithDupes(expected.getNet(), 
				pptNet, nme, nmp ) );		
	}
		
	private TimedTransition findUniqueTransition(StochasticNet pptNet, String label) {
		Collection<Transition> transitions = pptNet.getTransitions();
		for (Transition tran: transitions) {
			if (label.equals(tran.getLabel())){
				return (TimedTransition)tran;
			}
		}
		return null;
	}

	@Test
	public void convertMixed() {
		ProbProcessTree la = ProbProcessTreeFactory.createLeaf("a",1.0d);
		ProbProcessTreeNode seq = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree lb = ProbProcessTreeFactory.createLeaf("b",1.0d);
		ProbProcessTree tau1 = ProbProcessTreeFactory.createSilent(1.0d);
		ProbProcessTree lc = ProbProcessTreeFactory.createLeaf("c",1.0d);
		seq.addChildren(lb,tau1,lc);
		ProbProcessTreeNode choice = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		choice.addChildren(la,seq);
		AcceptingStochasticNet expected = parser.createAcceptingNet("ppt", 
				"Start -> {a} -> End");
		parser.addToAcceptingNet(expected,
				"Start -> {b} -> sp1 -> {tau} -> sp2 -> {c} -> End");
		convertsTo(expected, choice);
	}

	@Test
	public void playoutStackOverflow() {
		// Just checks for basic errors - S/O bug in playout generation
		//  Model: \/  4.0(\/  3.0(\/  2.0(D 1.0)(C 1.0))(B 1.0))(->  1.0(B 1.0)(E 1.0)(@  1.0(tau 1.0))(A 1.0))
		ProbProcessTree la = ProbProcessTreeFactory.createLeaf("A",1.0d);
		ProbProcessTree lb = ProbProcessTreeFactory.createLeaf("B",1.0d);
		ProbProcessTree lc = ProbProcessTreeFactory.createLeaf("C",1.0d);
		ProbProcessTree ld = ProbProcessTreeFactory.createLeaf("D",1.0d);
		ProbProcessTree le = ProbProcessTreeFactory.createLeaf("E",1.0d);
		ProbProcessTree silent = ProbProcessTreeFactory.createSilent(1.0d);
		ProbProcessTreeNode sloop = ProbProcessTreeFactory.createLoop(7.0);
		sloop.addChild(silent);
		ProbProcessTreeNode seq = ProbProcessTreeFactory.createSequence();
		seq.addChildren(lb,le,sloop,la);
		ProbProcessTreeNode dcchoice = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		dcchoice.addChildren(ld,lc);
		ProbProcessTreeNode bchoice = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		bchoice.addChildren(dcchoice,lb);
		ProbProcessTreeNode top = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		top.addChildren(bchoice,seq);
		StochasticNet snet = converter.convertToSNet(top).getNet();
		LOGGER.info("Net :: " + PetrinetExportUtils.convertPetrinetToDOT(snet)) ;
	}
	
}
