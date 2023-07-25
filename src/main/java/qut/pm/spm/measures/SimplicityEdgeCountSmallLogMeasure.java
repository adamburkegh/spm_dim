package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

public class SimplicityEdgeCountSmallLogMeasure implements StochasticLogCachingMeasure {
	
	protected LogStatsCache logStatsCache = new LogStatsCache();
	protected LogStats stats;
	
	public SimplicityEdgeCountSmallLogMeasure (LogStatsCache logStatsCache) {
		this.logStatsCache = logStatsCache; 
	}
	
	@Override
	public String getReadableId() {
		return "Simplicity by edge count (small log)";
	}

	@Override
	public Measure getMeasure() {
		return Measure.STRUCTURAL_SIMPLICITY_EDGE_COUNT_SMALL_LOG;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet anet, XEventClassifier classifier) {
		precalculateForLog(log,classifier);
		double result = 1d - ( (double)anet.getNet().getEdges().size() / 
							 ((double)stats.getUniqueTraceCount() * stats.getAvgTraceLength())) ;
		return (result < 0? 0d: result);
	}

	@Override
	public String getUniqueId() {
		return "simpedgessl";
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		stats = logStatsCache.getStats(log,classifier);
	}


}
