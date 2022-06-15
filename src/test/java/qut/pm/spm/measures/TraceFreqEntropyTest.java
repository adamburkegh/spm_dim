package qut.pm.spm.measures;

import static org.junit.Assert.*;

import org.junit.Test;

import qut.pm.spm.TraceFreq;
import qut.pm.spm.TraceFreqTest;

public class TraceFreqEntropyTest {

	private static final double EPSILON = 0.0001d;

	@Test
	public void empty() {
		TraceFreq tf = new TraceFreq();
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		assertEquals(0.0d,result,EPSILON);
	}

	@Test
	public void singleton() {
		TraceFreq tf = TraceFreqTest.createTraceFreq("a");
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		assertEquals(0.0d,result,EPSILON);
	}

	@Test
	public void halfAndHalf() {
		TraceFreq tf = TraceFreqTest.createTraceFreq("a b","c d");
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		// 0.5*log2 0.5 + 0.5*log2 0.5
		assertEquals(1.0d,result,EPSILON);
	}

	@Test
	public void multi() {
		TraceFreq tf = TraceFreqTest.createTraceFreq("a b","a b","c d");
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		// 2/3*log2 2/3 + 1/3*log2 1/3
		assertEquals(0.91829,result,EPSILON);
	}

	@Test
	public void nonEmptyZeroFreq() {
		TraceFreq tf = TraceFreqTest.createTraceFreq("a b","a b","c d");
		tf.putFreq(TraceFreqTest.toTrace("a b"), 0 );
		tf.putFreq(TraceFreqTest.toTrace("c d"), 0 );
		double result = TraceEntropyMeasure.entropyForTraceFreq(tf);
		// 2/3*log2 2/3 + 1/3*log2 1/3
		assertEquals(0,result,EPSILON);
	}
	
	
}
