package qut.pm.spm.measures;

import static org.junit.Assert.*;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.Before;
import org.junit.Test;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.TraceFreqTest;
import qut.pm.spm.playout.StochasticPlayoutGenerator;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeFormatter;
import qut.pm.spm.ppt.ProbProcessTreeNode;

public class GenTraceDiffCalculationTest {

	private static final double EPSILON = 0.0001;

	private static Logger LOGGER = LogManager.getLogger();
	
	private GenTraceDiffCalculation gtdCalc = null;
	
	@Before
	public void setUp() throws Exception {
		 gtdCalc = new GenTraceDiffCalculation();
	}


	@Test
	/**
	 * This tree was observed giving a negative measure in the genetic miner.
	 * The uneven weights are almost certainly a result of float 
	 * rounding / overflow. Could not reproduce error (yet).
	 */
	public void negativeMeasure() {
		// ->  4.0000000000000036
		//  ->  4.0000000000000036
		//    A 4.0000000000000036
		//    /\  4.0
		//      /\  2.0
		//        B 2.0
		//      \/  2.0
		//        C 1.0
		//        tau 1.0
		//    D 4.0000000000000036
		double nearFour = 4.0000000000000036;
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createSequence();
		ProbProcessTreeNode pptSeq1 = ProbProcessTreeFactory.createSequence();
		ProbProcessTreeNode pptConc1 = ProbProcessTreeFactory.createConcurrency();
		ProbProcessTreeNode pptConc2 = ProbProcessTreeFactory.createConcurrency();
		pptConc2.addChild(ProbProcessTreeFactory.createLeaf("B", 2.0d));
		ProbProcessTreeNode pptChoice1 = ProbProcessTreeFactory.createChoice();
		pptChoice1.addChild( ProbProcessTreeFactory.createLeaf("C",1.0d) );
		pptChoice1.addChild( ProbProcessTreeFactory.createSilent(1.0d) );
		pptConc1.addChild(pptChoice1);
		pptConc1.addChild(pptConc2);
		pptSeq1.addChild( ProbProcessTreeFactory.createLeaf("A", nearFour) );
		pptSeq1.addChild( pptConc1 );
		pptSeq1.addChild( ProbProcessTreeFactory.createLeaf("D", nearFour) );
		ppt.addChild(pptSeq1);
		LOGGER.debug( "\n{}", new ProbProcessTreeFormatter().textTree(ppt) );
		ProbProcessTreeConverter pptConverter = new ProbProcessTreeConverter();
		AcceptingStochasticNet snet = pptConverter.convertToSNet(ppt);
		//
		final int traceCt = 3;
		TraceFreq tfLog = TraceFreqTest.createTraceFreq("A E D","A C B D","A B C D");
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator( traceCt );
		TraceFreq playoutLog = generator.buildPlayoutTraceFreq(snet);
		double result = gtdCalc.calculateGeneralizationTraceDiff(tfLog, playoutLog);
		LOGGER.debug(result);
		assertEquals(0,result,EPSILON);
	}
	
	@Test
	public void fractionalScaleDown() {
		TraceFreq tfLog = TraceFreqTest.createTraceFreq("A E D","A C B D","A B C D");
		TraceFreq tfModel = TraceFreqTest.createTraceFreq(
									"A C B D",
									"A B C D",
									"E F", "E F",
									"D D", "D D");
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(  );
		generator.scaleTo(tfModel, 3);
		double result = gtdCalc.calculateGeneralizationTraceDiff(tfLog, tfModel);
		LOGGER.debug(result);
		assertEquals(0,result,EPSILON);
	}

	@Test
	public void fractionalScaleUp() {
		TraceFreq tfLog = TraceFreqTest.createTraceFreq(
									"A E D",
									"A C B D","A C B D",
									"A B C D");
		TraceFreq tfModel = TraceFreqTest.createTraceFreq(
									"A C B D",
									"A B C D",
									"E F", "E F",
									"D D", "D D");
		StochasticPlayoutGenerator generator = new StochasticPlayoutGenerator(  );
		generator.scaleTo(tfModel, 13);
		double result = gtdCalc.calculateGeneralizationTraceDiff(tfLog, tfModel);
		LOGGER.debug(result);
		assertEquals(0.25,result,EPSILON);
	}

	
}
