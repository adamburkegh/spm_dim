package qut.pm.spm.measures.relevance;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.TraceFreqTest;

public class RestrictedZeroOrderRelevanceCalculatorTest {

	private static final double EPSILON = 0.006;
	private RelevanceCalculator calc = null;
	
	protected FiniteStochasticLang logE1 = RelevanceTestUtils.logE1;
	protected FiniteStochasticLang logE2 = RelevanceTestUtils.logE2;

	protected TraceFreq modelA1 = RelevanceTestUtils.modelA1;
	protected TraceFreq modelA2 = RelevanceTestUtils.modelA2;

	
	@Before
	public void setUp() throws Exception {
		calc = RelevanceFactory.createRelevanceCalcRestrictedZeroOrder();
	}


	@Test
	public void paperEgA1E1() {
		double result = calc.relevance(logE1, modelA1);
		assertNear(2.18,result);
		double sel = calc.selectorCost(logE1.getTraceFrequency(), modelA1);
		assertNear(0.0d,sel);
	}

	@Test
	public void paperEgA2E1() {
		double result = calc.relevance(logE1, modelA2);
		assertNear(4.41,result);
		double sel = calc.selectorCost(logE1.getTraceFrequency(), modelA2);
		assertNear(0.72d,sel);
	}
		
	@Test
	public void paperEgA1E2() {
		double result = calc.relevance(logE2, modelA1);
		assertNear(6.42,result);
		double sel = calc.selectorCost(logE2.getTraceFrequency(), modelA1);
		assertNear(0.99d,sel);
	}


	@Test
	public void paperEgA2E2() {
		double result = calc.relevance(logE2, modelA2);
		assertNear(6.13,result);
		double sel = calc.selectorCost(logE2.getTraceFrequency(), modelA2);
		assertNear(0.86d,sel);
	}

	@Test
	public void paperRestrictedZeroOrderBackgroundEg() {
		// In Defn 4.5
		RelevanceBackgroundCost bgCost = new RestrictedZeroOrderBackgroundCost();
		bgCost.initCostRun(logE2, modelA2);
		double result = bgCost.cost(TraceFreqTest.toTrace("a e e"), logE2, modelA2);
		assertNear(result,6.51);
	}

	@Test
	public void paperRestrictedZeroOrderPreludeA1E1() {
		RestrictedZeroOrderPreludeCost prelCost = new RestrictedZeroOrderPreludeCost();
		prelCost.initCostRun(logE1, modelA1);
		double result = prelCost.cost(logE1, modelA1);
		assertNear(0.003d,result);
	}

	@Test
	public void paperRestrictedZeroOrderPreludeA2E1() {
		RestrictedZeroOrderPreludeCost prelCost = new RestrictedZeroOrderPreludeCost();
		prelCost.initCostRun(logE1, modelA2);
		double result = prelCost.cost(logE1, modelA2);
		assertNear(0.03d,result);
	}

	
	@Test
	public void paperRestrictedZeroOrderPreludeA1E2() {
		RestrictedZeroOrderPreludeCost prelCost = new RestrictedZeroOrderPreludeCost();
		prelCost.initCostRun(logE2, modelA1);
		double result = prelCost.cost(logE2, modelA1);
		assertNear(0.27d,result);
	}

	
	@Test
	public void paperRestrictedZeroOrderPreludeA2E2() {
		RestrictedZeroOrderPreludeCost prelCost = new RestrictedZeroOrderPreludeCost();
		prelCost.initCostRun(logE2, modelA2);
		double result = prelCost.cost(logE2, modelA2);
		assertNear(0.21d,result);
	}

	
	private void assertNear(double result, double d) {
		assertEquals(result,d,EPSILON);		
	}

}
