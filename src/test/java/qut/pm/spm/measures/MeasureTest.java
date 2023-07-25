package qut.pm.spm.measures;

import static org.junit.Assert.assertEquals;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.model.XLog;
import org.junit.Before;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.log.ProvenancedLogImpl;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class MeasureTest {

	protected static final double EPSILON = 0.001d;
	protected static final XEventNameClassifier NAME_CLASSIFIER = new XEventNameClassifier();
	protected DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	protected StochasticLogCachingMeasure measure;
	protected PetriNetFragmentParser parser;

	public MeasureTest() {
		super();
	}

	@Before
	public void setUp() {
		measure = new TraceProbabilityMassOverlap();
		parser = new PetriNetFragmentParser();
	}

	protected void assertMeasureEquals(double expected, ProvenancedLog log, AcceptingStochasticNet net) {
		assertEquals(expected, measure.calculate(log,net, NAME_CLASSIFIER), EPSILON);
	}

	protected ProvenancedLog plog(String ... args) {
		XLog log = converter.convertTextArgs(args);
		ProvenancedLog plog = new ProvenancedLogImpl(log,"testDummyFileName");
		return plog;
	}

}