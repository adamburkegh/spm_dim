package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

public class SimplicityStructuralStochasticUniqMeasure implements StochasticLogCachingMeasure {
	
	protected LogStatsCache logStatsCache = new LogStatsCache();
	protected int uniqueTraceCount = 0;
	
	@Override
	public String getReadableId() {
		return "Simplicity by stochastic structural uniqueness";
	}
	
	public SimplicityStructuralStochasticUniqMeasure(LogStatsCache logStatsCache) {
		this.logStatsCache = logStatsCache; 
	}

	@Override
	public Measure getMeasure() {
		return Measure.STRUCTURAL_SIMPLICITY_STOCHASTIC;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet anet, XEventClassifier classifier) {
		precalculateForLog(log,classifier);
		StochasticStructuralComplexity ssu = new StochasticStructuralComplexity();  
		double result = 1d - ( (double)(ssu.calculate(log,anet,classifier))  / (double)uniqueTraceCount) ;
		return (result < 0? 0d: result);
	}

	@Override
	public String getUniqueId() {
		return "ssimpstochunq";
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		LogStats stats = logStatsCache.getStats(log,classifier);
		uniqueTraceCount = stats.getUniqueTraceCount();
	}


}
