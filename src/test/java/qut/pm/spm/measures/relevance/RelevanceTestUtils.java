package qut.pm.spm.measures.relevance;

import java.util.ArrayList;
import java.util.List;

import qut.pm.spm.ActivityFreq;
import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.FiniteStochasticLangGenerator;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.TraceFreqTest;

public class RelevanceTestUtils {

	public static FiniteStochasticLang logE1;
	public static FiniteStochasticLang logE2;

	public static TraceFreq modelA1 = new TraceFreq();
	public static TraceFreq modelA2 = new TraceFreq();
	
	static {
		FiniteStochasticLangGenerator fslGen = new FiniteStochasticLangGenerator (); 
		TraceFreq tfE1 = new TraceFreq();
		tfE1.putFreq( TraceFreqTest.toTrace("a b"), 1200 );
		tfE1.putFreq( TraceFreqTest.toTrace("a e"), 300 );
		tfE1.putFreq( TraceFreqTest.toTrace("a b c d"), 220 );
		tfE1.putFreq( TraceFreqTest.toTrace("a b c"), 100 );
		tfE1.putFreq( TraceFreqTest.toTrace("a e e"), 100 );
		tfE1.putFreq( TraceFreqTest.toTrace("a b c d e"), 80 );
		ActivityFreq afE1 = fslGen.calculateActivityFreqForTF(tfE1);
//		ActivityFreq afE1 = new ActivityFreq();
//		afE1.putFreq("a", 2000);
//		afE1.putFreq("b", 480);
//		afE1.putFreq("c", 400);
//		afE1.putFreq("d", 300);
//		afE1.putFreq("e", 280);
		logE1 = new FiniteStochasticLang(tfE1,afE1);
		TraceFreq tfE2 = new TraceFreq();
		List<String> emptyTrace = new ArrayList<String>();
		tfE2.putFreq( emptyTrace , 50 );
		tfE2.putFreq( TraceFreqTest.toTrace("a b"), 50 );
		tfE2.putFreq( TraceFreqTest.toTrace("a b c"), 50 );
		tfE2.putFreq( TraceFreqTest.toTrace("a e a a e"), 40 );
		tfE2.putFreq( TraceFreqTest.toTrace("a e e"), 20 );
		tfE2.putFreq( TraceFreqTest.toTrace("a b c d"), 10 );
		tfE2.putFreq( TraceFreqTest.toTrace("a b c d e"), 10 );
		tfE2.putFreq( TraceFreqTest.toTrace("a b e e"), 10 );
		tfE2.putFreq( TraceFreqTest.toTrace("a b c f f"), 10 );
		ActivityFreq afE2 = fslGen.calculateActivityFreqForTF(tfE2);
//		ActivityFreq afE2 = new ActivityFreq();
//		afE2.putFreq("a", 112);
//		afE2.putFreq("b", 60);
//		afE2.putFreq("c", 32);
//		afE2.putFreq("d",  4);
//		afE2.putFreq("e", 12);
//		afE2.putFreq("f",  8);
		logE2 = new FiniteStochasticLang(tfE2,afE2);
		modelA1 = new TraceFreq();
		modelA1.putFreq( TraceFreqTest.toTrace("a b"), 160 );
		modelA1.putFreq( TraceFreqTest.toTrace("a b c"), 20 );
		modelA1.putFreq( TraceFreqTest.toTrace("a b c d"), 45 );
		modelA1.putFreq( TraceFreqTest.toTrace("a b c d e"), 15 );
		modelA1.putFreq( TraceFreqTest.toTrace("a b f"), 80 );
		modelA1.putFreq( TraceFreqTest.toTrace("a e"), 60 );
		modelA1.putFreq( TraceFreqTest.toTrace("a e e"), 20 );
		modelA2 = new TraceFreq(); 		// approximation for infinite lang
		modelA2.putFreq( emptyTrace, 256 );
		modelA2.putFreq( TraceFreqTest.toTrace("a"), 384 );
		modelA2.putFreq( TraceFreqTest.toTrace("a b"), 192 );
		modelA2.putFreq( TraceFreqTest.toTrace("a b c"), 48 );
		modelA2.putFreq( TraceFreqTest.toTrace("a b c f"), 12 );
		modelA2.putFreq( TraceFreqTest.toTrace("a b c f f"), 3 );
		modelA2.putFreq( TraceFreqTest.toTrace("a b e"), 96 );
		modelA2.putFreq( TraceFreqTest.toTrace("a b c f d"), 3 );
		modelA2.putFreq( TraceFreqTest.toTrace("a b c d"), 12 );
		modelA2.putFreq( TraceFreqTest.toTrace("a b c d e"), 12 );
		modelA2.putFreq( TraceFreqTest.toTrace("a b c f d e"), 3 );
		modelA2.putFreq( TraceFreqTest.toTrace("null"), 3 );
	}
}
