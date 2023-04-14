package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

public class SimplicityEntityCountMeasure implements StochasticLogCachingMeasure {
	
	protected LogStatsCache logStatsCache = new LogStatsCache();
	protected int uniqueTraceCount = 0;
	
	public SimplicityEntityCountMeasure (LogStatsCache logStatsCache) {
		this.logStatsCache = logStatsCache; 
	}
	
	@Override
	public String getReadableId() {
		return "Simplicity by entity count";
	}

	@Override
	public Measure getMeasure() {
		return Measure.STRUCTURAL_SIMPLICITY_ENTITY_COUNT;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet anet, XEventClassifier classifier) {
		precalculateForLog(log,classifier);
		int tran = anet.getNet().getTransitions().size();
		int places = anet.getNet().getPlaces().size();
		double result = 1d - ( (double)(tran + places)  / (double)uniqueTraceCount) ;
		return (result < 0? 0d: result);
	}

	@Override
	public String getUniqueId() {
		return "simpentities";
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		LogStats stats = logStatsCache.getStats(log,classifier);
		uniqueTraceCount = stats.getUniqueTraceCount();
	}


}
