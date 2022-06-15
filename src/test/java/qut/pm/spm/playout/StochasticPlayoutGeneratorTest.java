package qut.pm.spm.playout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.TraceFreq;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class StochasticPlayoutGeneratorTest {

	protected PetriNetFragmentParser parser; 
	protected DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	protected PlayoutGenerator generator ;
	private static final XEventClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	
	
	@Before
	public void setUp() {
		parser = new PetriNetFragmentParser();
		generator = new StochasticPlayoutGenerator(5000);
	}

	/* 
	 * Test depends on no DEFAULT_EVENT_DELIMITER (a space) being in the activity labels. 
	 * Assumes NameClassifier 
	 */
	private SortedMultiset<String> convertXLogToSortedSet(XLog log) {
		SortedMultiset<String> result = TreeMultiset.create();
		for (XTrace trace: log) {
			StringBuilder traceString = new StringBuilder();
			for (XEvent event: trace) {
				traceString.append( NAME_CLASSIFIER.getClassIdentity(event) );
				traceString.append( DelimitedTraceToXESConverter.DEFAULT_EVENT_DELIMITER); 
			}
			// Don't care about trailing space in tests 
			result.add(traceString.toString());
		}
		return result;
	}
	
	
	private void assertLogEquals(XLog log, XLog expected) {
		SortedMultiset<String> logBag = convertXLogToSortedSet(log);
		SortedMultiset<String> expectedBag = convertXLogToSortedSet(expected);
		assertEquals(expectedBag,logBag);
		// XLog equals() is just object equality so doesn't cut it
	}
	
	protected void assertLogEqualsOneOf(XLog log, XLog ... expected) {
		SortedMultiset<String> logBag = convertXLogToSortedSet(log);
		boolean comparison = false;
		StringBuilder message = new StringBuilder();
		boolean first = true;
		for (XLog expectedLog: expected) {
			SortedMultiset<String> expectedBag = convertXLogToSortedSet(expectedLog);
			comparison = comparison || logBag.equals(expectedBag);
			if (comparison)
				return;
			if (!first)
				message.append(" && ");
			message.append(logBag + "!= " + expectedBag);
			first = false;
		}
		assertTrue(message.toString(), comparison); 
	}
	
	@Test
	public void singleTransition() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", "Start -> {a 2.0} -> End");
		XLog log = generator.buildPlayoutLog(net,5);
		XLog expected = converter.convertTextArgs("a","a","a","a","a"); 
		assertLogEquals(log, expected);
	}

	
	@Test
	public void singleChoice() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
								 	  "Start -> {a 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 2.0} -> End");
		XLog log = generator.buildPlayoutLog(net,6);
		XLog expected = converter.convertTextArgs("a","a","a","b","b","b"); 
		assertLogEquals(log, expected);		
	}

	@Test
	public void choiceSequence() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
								 	  "Start -> {a 5.0} -> p1 -> {b 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {c 1.0} -> p2 -> {d 2.0} -> End");
		XLog log = generator.buildPlayoutLog(net,6);
		XLog expected = converter.convertTextArgs("a b","a b","a b","a b","a b","c d"); 
		assertLogEquals(log, expected);		
	}
	
	@Test
	public void singleConcurrency() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
								 	  "Start -> {a 2.0} -> p1 -> {b 2.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p2 -> {c 2.0} -> End");
		XLog log = generator.buildPlayoutLog(net,6);
		XLog expected = converter.convertTextArgs("a b c","a b c","a b c","a c b","a c b","a c b"); 
		assertLogEquals(log, expected);
	}

	@Test
	public void unevenChoice() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 2.0} -> End");
		XLog log = generator.buildPlayoutLog(net,6);
		XLog expected = converter.convertTextArgs("a","a","b","b","b","b"); 
		assertLogEquals(log, expected);				
	}

	@Test
	public void unevenConcurrency() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 5.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p2 -> {c 1.0} -> End");
		XLog log = generator.buildPlayoutLog(net,6);
		XLog expected = converter.convertTextArgs("a b c","a b c","a b c","a b c","a b c","a c b"); 
		assertLogEquals(log, expected);
	}

	@Test
	public void choiceDupes() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				  					  "Start -> {a 4.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b__1 1.0} -> p1 -> {c 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b__2 1.0} -> p2 -> {d 1.0} -> End");
		XLog log = generator.buildPlayoutLog(net,6);
		XLog expected = converter.convertTextArgs("a","a","a","a","b c","b d"); 
		assertLogEquals(log, expected);
	}

	@Test
	public void choiceUnderallocate() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				  					  "Start -> {a 10.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 1.0} -> p1 -> {c 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {d 1.0} -> p2 -> {e 1.0} -> End");
		XLog log = generator.buildPlayoutLog(net,4);
		XLog expected = converter.convertTextArgs("a","a","a","b c"); 
		assertLogEquals(log, expected);
	}
	
	@Test
	public void leftoverChoiceDupesArbitrary() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				  					  "Start -> {a 9.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b__1 1.0} -> p1 -> {c 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b__2 1.0} -> p2 -> {d 1.0} -> End");
		XLog log = generator.buildPlayoutLog(net,5);
		XLog expected1 = converter.convertTextArgs("a","a","a","a","b c"); 
		XLog expected2 = converter.convertTextArgs("a","a","a","a","b d");
		// Choice between b__1 and b__2 is arbitrary. So either is valid
		assertLogEqualsOneOf(log, expected1, expected2);
	}
		
	@Test
	public void leftoverConcurrency() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
			 	  					  "Start -> {a 2.0} -> p1 -> {b 10.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a 2.0} -> p2 -> {c 1.0} -> End");
		XLog log = generator.buildPlayoutLog(net,4);
		XLog expected = converter.convertTextArgs("a b c","a b c","a b c","a c b"); 
		assertLogEquals(log, expected);
	}

	@Test
	public void leftoverZeroWeight() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
					  				  "Start -> {a 9.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 1.0} -> End");
		XLog log = generator.buildPlayoutLog(net,5);
		XLog expected = converter.convertTextArgs("a","a","a","a","b"); 
		assertLogEquals(log, expected);				
	}

	@Test
	public void leftoverLexical() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				  					  "Start -> {a 9.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {c 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 1.0} -> End");
		XLog log = generator.buildPlayoutLog(net,5);
		XLog expected = converter.convertTextArgs("a","a","a","a","b"); 
		assertLogEquals(log, expected);
	}
	
	@Test
	public void leftoverWithDuplicates() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
					  "Start -> {a__1 2.0} -> p1 -> {a__2 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {a__1 2.0} -> p1 -> {b__1 1.0} -> End");
		parser.addToAcceptingNet(net, "Start -> {b__2 2.0} -> p2 -> {e 1.0} -> End");
		XLog log = generator.buildPlayoutLog(net,3);
		XLog expected = converter.convertTextArgs("a a","a b","b e");
		XLog expected2 = converter.convertTextArgs("a a","b e","b e");
		assertLogEqualsOneOf(log, expected, expected2);
	}

	@Test
	public void silentTransitions() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
					  				  "Start -> {a 1.0} -> p1 -> {tau} -> End");
		parser.addToAcceptingNet(net, "Start -> {b 2.0} -> p2 -> {c} -> End");
		XLog log = generator.buildPlayoutLog(net,6);
		XLog expected = converter.convertTextArgs("a","a","b c","b c","b c","b c"); 
		assertLogEquals(log, expected);				
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
		assertLogEqualsOneOf(log, expected1,expected2,expected3,expected4);
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
		for(int i=0; i<98; i++ ) {
			expectedLogStr.append(",B E A");
		}
		XLog expected = converter.convertText(expectedLogStr.toString() + ",B E A", " ", ",");
		assertLogEquals(log, expected);
	}
	
	@Test
	public void silentLoopMaxTraceLengthEven() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {D} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B} -> p1 -> {E} -> p2 -> {tau__1} -> p3 -> {tau__3} -> p4 -> {A} -> End");
		parser.addToAcceptingNet(net,                          "p3 -> {tau__2} -> p3");
		XLog log = generator.buildPlayoutLog(net,2);
		XLog expected1 = converter.convertTextArgs("D","B E A");
		assertLogEquals(log, expected1);
	}

	
	@Test
	public void silentLoopMaxTraceLengthArbitraryLeftover() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {D} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B 2.0} -> p1 -> {E} -> p2 -> {tau__1} -> p3 -> {tau__3} -> p4 -> {A} -> End");
		parser.addToAcceptingNet(net,                          "p3 -> {tau__2} -> p3");
		XLog log = generator.buildPlayoutLog(net,3);
		XLog expected1 = converter.convertTextArgs("D","B E A", "B E A");
		assertLogEquals(log, expected1);
	}

	@Test
	public void wideDuplicateChoiceLeftover() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {D__1} -> p1 -> {A} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {D__2} -> p2 -> {B} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {D__3} -> p3 -> {C} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {D__4} -> p4 -> {E} -> End");
		XLog log = generator.buildPlayoutLog(net,3);
		XLog expected1 = converter.convertTextArgs("D A","D B","D C");
		XLog expected2 = converter.convertTextArgs("D A","D B","D E");
		XLog expected3 = converter.convertTextArgs("D B","D C","D E");
		XLog expected4 = converter.convertTextArgs("D A","D C","D E");
		assertLogEqualsOneOf(log, expected1, expected2,expected3, expected4);
	}
	
	@Test
	public void maxTraceTriggered() {
		generator = new StochasticPlayoutGenerator(10,1000);
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {D 1.0} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B 9.0} -> p1 -> {E} -> p2 -> {tau__1} -> p3 -> {tau__3} -> p4 -> {A} -> End");
		parser.addToAcceptingNet(net,                          "p3 -> {C 10.0} -> p3");
		XLog log = generator.buildPlayoutLog(net,10);
		XLog expected1 = converter.convertTextArgs("D","B E A", "B E C C A", "B E C C C C A", 
												   "B E C C C C C C",  "B E C C C C C C C",
												   "B E C C C C C C C","B E C C C C C C C",
												   "B E C C C C C C C","B E C C C C C C C");
		assertLogEquals(log, expected1);
	}
	
	@Test
	public void traceFreqGen() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
					  "Start -> {a 2.0} -> p1 -> {b 2.0} -> p2 -> {c 2.0} -> End");
		parser.addToAcceptingNet(net, 
					  "Start -> {a 2.0} -> p1 -> {d 1.0} -> p3 -> {e 2.0} -> End");
		TraceFreq tf = generator.buildPlayoutTraceFreq(net,6);
		TraceFreq tfExpected = new TraceFreq();
		tfExpected.putFreq( Arrays.asList(new String[]{"a","b","c"}) ,4);
		tfExpected.putFreq( Arrays.asList(new String[]{"a","d","e"}) ,2);
		assertEquals(tfExpected,tf);
	}
	
}

