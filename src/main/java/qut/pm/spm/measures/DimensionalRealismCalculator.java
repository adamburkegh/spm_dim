package qut.pm.spm.measures;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.PlayoutGenerator;

public class DimensionalRealismCalculator {

	protected List<StochasticLogCachingMeasure> measures = new ArrayList<>();
	protected double[] factors;
	protected double constant;
	protected double min;
	protected double max;
	
	public DimensionalRealismCalculator(PlayoutGenerator generator, 
			double[] factors,double constant,double max, double min) 
	{
		if (factors.length != 6) 
			throw new RuntimeException("Invalid DR measure factors");
		measures.add( new ExistentialPrecisionMeasure(generator)  );
		measures.add( new GenTraceFloorMeasure(generator,5)  );
		measures.add( new GenTraceDiffMeasure(generator) );
		measures.add( new EntropicRelevanceZeroOrderMeasure(generator) );
		measures.add( new TraceRatioMeasure(generator,2)  );
		addSimplicityMeasure();
		this.factors = factors;
		this.constant = constant;
		this.max = max ; 
		this.min = min ;
	}

	protected void addSimplicityMeasure() {
		LogStatsCache statsCache = new LogStatsCache();
		measures.add( new SimplicityEdgeCountMeasure(statsCache)  );
	}

	public double calculate(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		double sum = 0.0d;
		int i=0;
		for (StochasticLogCachingMeasure measure:measures) {
			sum +=  factors[i]* measure.calculate(log, net, classifier);
			i++;
		}
		return ((sum - constant)-min)/(max-min);
	}

	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		for (StochasticLogCachingMeasure measure:measures) {
			measure.precalculateForLog(log, classifier);
		}		
	}

	
}
