package qut.pm.spm.measures;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import qut.pm.spm.TraceFreq;


public class ExistentialPrecisionCalculationTest {

	private static final double EPSILON = 0.001;
	private ExistentialPrecisionCalculation alphaCalc;
	

	@Before
	public void setUp() throws Exception {
		alphaCalc = new ExistentialPrecisionCalculation();
	}

	public void assertClose(double expected, double result) {
		assertEquals(expected, result, EPSILON);
	}
	
	@Test
	public void testPerfect() {
		TraceFreq tfLog = new TraceFreq();
		tfLog.putFreq( Arrays.asList(new String[]{"a","b","c"}) ,4);
		tfLog.putFreq( Arrays.asList(new String[]{"a","d","e"}) ,2);
		Set<String> activities = actSet("a","b","c","d","e");
		assertClose(1.0, 
				alphaCalc.calculateUnrestrictedPrecision(tfLog, tfLog, 
														18, 0.0, 5, 3));
		assertClose(1.0, 
				alphaCalc.calculateRestrictedPrecision(tfLog, tfLog, 
													activities, 18, 0.0, 3));
	}

	@Test
	public void testZero() {
		TraceFreq tfLog = new TraceFreq();
		tfLog.putFreq( Arrays.asList(new String[]{"a","b","c"}) ,4);
		tfLog.putFreq( Arrays.asList(new String[]{"a","d","e"}) ,2);
		Set<String> activities = actSet("a","b","c","d","e");
		TraceFreq tfModel = new TraceFreq();
		tfModel.putFreq( Arrays.asList(new String[]{"b","c"}), 6);
		tfModel.putFreq( Arrays.asList(new String[]{"d","e"}), 9);
		assertClose(0.0, alphaCalc.calculateUnrestrictedPrecision(tfLog, tfModel, 48, 0.0, 5, 3));
		assertClose(0.0, alphaCalc.calculateRestrictedPrecision(tfLog, tfModel, activities, 48, 0.0, 3));
	}

	@Test
	public void testAnother() {
		TraceFreq tfLog = new TraceFreq();
		tfLog.putFreq( Arrays.asList(new String[]{"a","b","c"}) ,4);
		tfLog.putFreq( Arrays.asList(new String[]{"a","d","e"}) ,2);
		Set<String> activities = actSet("a","b","c","d","e");
		TraceFreq tfModel = new TraceFreq();
		tfModel.putFreq( Arrays.asList(new String[]{"a","b","c"}), 2);
		tfModel.putFreq( Arrays.asList(new String[]{"d","e"}), 3);
		assertClose(0.4, alphaCalc.calculateUnrestrictedPrecision(tfLog, tfModel, 48, 0.0, 5, 3));
		assertClose(0.4, alphaCalc.calculateRestrictedPrecision(tfLog, tfModel, activities, 48, 0.0, 3));
	}

	
	private HashSet<String> actSet(String ... activities) {
		return new HashSet<String>(Arrays.asList(activities));
	}
	
}
