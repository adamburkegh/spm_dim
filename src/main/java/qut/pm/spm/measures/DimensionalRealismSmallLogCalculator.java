package qut.pm.spm.measures;

import qut.pm.spm.playout.PlayoutGenerator;

public class DimensionalRealismSmallLogCalculator extends DimensionalRealismCalculator {

	public DimensionalRealismSmallLogCalculator(PlayoutGenerator generator, double[] factors, double constant,
			double max, double min) {
		super(generator, factors, constant, max, min);
	}

	protected void addSimplicityMeasure() {
		LogStatsCache statsCache = new LogStatsCache();
		measures.add( new SimplicityEdgeCountSmallLogMeasure(statsCache)  );
	}

}
