package qut.pm.spm.measures;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class TraceFreqEntropyTest {

	private static final double EPSILON = 0.0001d;

	private static List<String> toTrace(String traceStr){
		List<String> result = new LinkedList<>();
		for (String substr: traceStr.split(" ")) {
			result.add(substr);
		}
		return result;
	}
	
	private static TraceFreq createTraceFreq(String ... traces) {
		TraceFreq result = new TraceFreq();
		for (String trace: traces) {
			result.incTraceFreq( toTrace(trace) );
		}
		return result;
	}
	
	@Test
	public void empty() {
		TraceFreq tf = new TraceFreq();
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		assertEquals(0.0d,result,EPSILON);
	}

	@Test
	public void singleton() {
		TraceFreq tf = createTraceFreq("a");
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		assertEquals(0.0d,result,EPSILON);
	}

	@Test
	public void halfAndHalf() {
		TraceFreq tf = createTraceFreq("a b","c d");
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		// 0.5*log2 0.5 + 0.5*log2 0.5
		assertEquals(1.0d,result,EPSILON);
	}

	@Test
	public void multi() {
		TraceFreq tf = createTraceFreq("a b","a b","c d");
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		// 2/3*log2 2/3 + 1/3*log2 1/3
		assertEquals(0.91829,result,EPSILON);
	}

	@Test
	public void nonEmptyZeroFreq() {
		TraceFreq tf = createTraceFreq("a b","a b","c d");
		tf.putFreq(toTrace("a b"), 0 );
		tf.putFreq(toTrace("c d"), 0 );
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		// 2/3*log2 2/3 + 1/3*log2 1/3
		assertEquals(0,result,EPSILON);
	}
	
	
}
