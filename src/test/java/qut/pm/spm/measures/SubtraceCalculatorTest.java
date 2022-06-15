package qut.pm.spm.measures;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.Test;

import qut.pm.spm.TraceFreq;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class SubtraceCalculatorTest {

	private static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	
	private DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	private SubtraceCalculator subtraceCalc;
	
	@Before
	public void setUp() {
		subtraceCalc = new SubtraceCalculator();
	}
	
	@Test
	public void singleActivity() {
		XLog log = converter.convertTextArgs("a");
		TraceFreq tfe = new TraceFreq();
		List<String> trace = Arrays.asList( new String[] {"a"} );
		tfe.incTraceFreq(trace);
		TraceFreq tf = subtraceCalc.calculateActivityFreq(log,NAME_CLASSIFIER);
		assertEquals(tfe,tf);
	}

	@Test
	public void singleTrace() {
		XLog log = converter.convertTextArgs("a b");
		TraceFreq tfe = new TraceFreq();
		List<String> trace = Arrays.asList( new String[] {"a","b"} );
		tfe.incTraceFreq(trace);
		TraceFreq tf = subtraceCalc.calculateForLog(log,NAME_CLASSIFIER,2);
		assertEquals(tfe,tf);
	}

	
	@Test
	public void singleTF() {
		TraceFreq log = new TraceFreq();
		TraceFreq tfe = new TraceFreq();
		List<String> trace = Arrays.asList( new String[] {"a"} );
		log.incTraceFreq(trace);
		tfe.incTraceFreq(trace);
		TraceFreq tf = subtraceCalc.calculateForTraceFreq(log,1);
		assertEquals(tfe,tf);
	}
	
	@Test
	public void twoActivityTF() {
		TraceFreq log = new TraceFreq();
		TraceFreq tfe = new TraceFreq();
		List<String> traceAB = Arrays.asList( new String[] {"a","b"} );
		List<String> traceA = Arrays.asList( new String[] {"a"} );
		List<String> traceB = Arrays.asList( new String[] {"b"} );
		log.incTraceFreq(traceAB);
		tfe.incTraceFreq(traceA);
		tfe.incTraceFreq(traceB);
		TraceFreq tf = subtraceCalc.calculateForTraceFreq(log,1);
		assertEquals(tfe,tf);
		tfe = new TraceFreq();
		tfe.incTraceFreq(traceAB);
		TraceFreq tf2 = subtraceCalc.calculateForTraceFreq(log,2);
		assertEquals(tfe,tf2);
	}
	
	@Test
	public void twoTraceTF() {
		TraceFreq log = new TraceFreq();
		TraceFreq tfe = new TraceFreq();
		List<String> traceAB = Arrays.asList( new String[] {"a","b"} );
		List<String> traceAC = Arrays.asList( new String[] {"a","c"} );
		List<String> traceA = Arrays.asList( new String[] {"a"} );
		List<String> traceB = Arrays.asList( new String[] {"b"} );
		List<String> traceC = Arrays.asList( new String[] {"c"} );
		log.incTraceFreq(traceAB);
		log.incTraceFreq(traceAC);
		tfe.incTraceFreq(traceA); tfe.incTraceFreq(traceA);
		tfe.incTraceFreq(traceB);
		tfe.incTraceFreq(traceC);
		TraceFreq tf = subtraceCalc.calculateForTraceFreq(log,1);
		assertEquals(tfe,tf);
	}
	
}
