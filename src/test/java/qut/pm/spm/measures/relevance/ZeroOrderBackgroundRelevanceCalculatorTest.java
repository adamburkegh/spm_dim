package qut.pm.spm.measures.relevance;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.TraceFreqTest;

public class ZeroOrderBackgroundRelevanceCalculatorTest {

	private static final double EPSILON = 0.01;
	private RelevanceCalculator calc = null;
	
	protected FiniteStochasticLang logE1 = RelevanceTestUtils.logE1;
	protected FiniteStochasticLang logE2 = RelevanceTestUtils.logE2;

	protected TraceFreq modelA1 = RelevanceTestUtils.modelA1;
	protected TraceFreq modelA2 = RelevanceTestUtils.modelA2;
	
	@Before
	public void setUp() throws Exception {
		calc = RelevanceFactory.createRelevanceCalcZeroOrder();
	}


	@Test
	public void paperEgA1E1() {
		double result = calc.relevance(logE1, modelA1);
		assertNear(2.23,result);
		double sel = calc.selectorCost(logE1.getTraceFrequency(), modelA1);
		assertNear(0.0d,sel);
	}

	@Test
	public void paperEgA2E1() {
		double result = calc.relevance(logE1, modelA2);
		assertNear(5.02,result);
		double sel = calc.selectorCost(logE1.getTraceFrequency(), modelA2);
		assertNear(0.72d,sel);
	}
		
	@Test
	public void paperEgA1E2() {
		double result = calc.relevance(logE2, modelA1);
		assertNear(6.84,result);
		double sel = calc.selectorCost(logE2.getTraceFrequency(), modelA1);
		assertNear(0.99d,sel);
	}


	@Test
	public void paperEgA2E2() {
		double result = calc.relevance(logE2, modelA2);
		assertNear(7.02,result);
		double sel = calc.selectorCost(logE2.getTraceFrequency(), modelA2);
		assertNear(0.86d,sel);
	}

	@Test
	public void paperZeroOrderBackgroundEg() {
		// In Defn 4.4
		ZeroOrderBackgroundCost bgCost = new ZeroOrderBackgroundCost();
		bgCost.initCostRun(logE2, modelA2);
		double result = bgCost.cost(TraceFreqTest.toTrace("a e e"), logE2, modelA2);
		assertNear(8.95,result);
	}

	@Test
	public void paperZeroOrderPreludeEg() {
		// In Defn 4.4
		ZeroOrderPreludeCost prelCost = new ZeroOrderPreludeCost();
		assertNear(17, prelCost.eliasGamma(281));
		double result = prelCost.cost(logE2, modelA2);
		assertNear(93.0d/250.0d,result);
	}
	
	private void assertNear(double expected, double actual) {
		assertEquals(expected,actual,EPSILON);		
	}

}
