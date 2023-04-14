package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

public class SimplicityEdgeCountMeasure implements StochasticLogCachingMeasure {
	
	protected LogStatsCache logStatsCache = new LogStatsCache();
	protected int uniqueTraceCount = 0;
	
	public SimplicityEdgeCountMeasure (LogStatsCache logStatsCache) {
		this.logStatsCache = logStatsCache; 
	}
	
	@Override
	public String getReadableId() {
		return "Simplicity by edge count";
	}

	@Override
	public Measure getMeasure() {
		return Measure.STRUCTURAL_SIMPLICITY_EDGE_COUNT;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet anet, XEventClassifier classifier) {
		precalculateForLog(log,classifier);
		double result = 1d - ( (double)anet.getNet().getEdges().size() / (double)uniqueTraceCount) ;
		return (result < 0? 0d: result);
	}

	@Override
	public String getUniqueId() {
		return "simpedges";
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		LogStats stats = logStatsCache.getStats(log,classifier);
		uniqueTraceCount = stats.getUniqueTraceCount();
	}


}
