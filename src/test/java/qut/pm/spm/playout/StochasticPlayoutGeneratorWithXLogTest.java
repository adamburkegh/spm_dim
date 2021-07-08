package qut.pm.spm.playout;

import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;

public class StochasticPlayoutGeneratorWithXLogTest extends StochasticPlayoutGeneratorTest {
	
	
	@Before
	public void setUp() {
		parser = new PetriNetFragmentParser();
		generator = new StochasticPlayoutGeneratorWithXLog(20);
	}

	@Test
	public void longSilentLoop() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {D} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B 99.0} -> p1 -> {E} -> p2 -> {tau__1} -> p3 -> {tau__3} -> p4 -> {A} -> End");
		parser.addToAcceptingNet(net,                          "p3 -> {tau__2 100.0} -> p3");
		XLog log = generator.buildPlayoutLog(net,100);
		StringBuilder expectedLogStr = new StringBuilder("D");
		for(int i=0; i<83; i++ ) {
			expectedLogStr.append(",B E");
		}
		for(int i=0; i<16; i++ ) {
			expectedLogStr.append(",B E A");
		}
		XLog expected1 = converter.convertText(expectedLogStr.toString() , " ", ",");
		XLog expected2 = converter.convertText(expectedLogStr.toString() , " ", ",");
		assertLogEqualsOneOf(log, expected1,expected2);
	}

	@Test
	public void silentLoopStackOverflowBug() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {D} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {C} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B} -> p1 -> {E} -> p2 -> {tau__1} -> p3 -> {tau__3} -> p4 -> {A} -> End");
		parser.addToAcceptingNet(net,                          "p3 -> {tau__2} -> p3");
		XLog log = generator.buildPlayoutLog(net,3);
		// ambiguous case 
		XLog expected1 = converter.convertTextArgs("D","C","B");
		XLog expected2 = converter.convertTextArgs("D","C","B E A");
		XLog expected3 = converter.convertTextArgs("D","B","B E A");
		XLog expected4 = converter.convertTextArgs("C","B","B E A");
		XLog expected5 = converter.convertTextArgs("D","C","B E");
		XLog expected6 = converter.convertTextArgs("D","B","B E");
		XLog expected7 = converter.convertTextArgs("C","B","B E");
		assertLogEqualsOneOf(log, expected1,expected2,expected3,expected4,expected5,expected6,expected7);
	}

	
	@Test
	public void silentLoopMaxTraceLengthEven() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {D} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B} -> p1 -> {E} -> p2 -> {tau__1} -> p3 -> {tau__3} -> p4 -> {A} -> End");
		parser.addToAcceptingNet(net,                          "p3 -> {tau__2} -> p3");
		XLog log = generator.buildPlayoutLog(net,2);
		// ambiguous case 
		XLog expected1 = converter.convertTextArgs("D","B E A");
		XLog expected2 = converter.convertTextArgs("D","B E");
		assertLogEqualsOneOf(log, expected1,expected2);
	}

	@Test
	public void silentLoopMaxTraceLengthArbitraryLeftover() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {D} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B 2.0} -> p1 -> {E} -> p2 -> {tau__1} -> p3 -> {tau__3} -> p4 -> {A} -> End");
		parser.addToAcceptingNet(net,                          "p3 -> {tau__2} -> p3");
		XLog log = generator.buildPlayoutLog(net,3);
		// ambiguous case 
		XLog expected1 = converter.convertTextArgs("D","B E A", "B E A");
		XLog expected2 = converter.convertTextArgs("D","B E A", "B E");
		XLog expected3 = converter.convertTextArgs("D","B E", "B E");
		assertLogEqualsOneOf(log, expected1,expected2,expected3);
	}

	
}



