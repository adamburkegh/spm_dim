package qut.pm.setm.fitness;

import java.util.LinkedHashMap;

import org.uncommons.watchmaker.framework.FitnessEvaluator;

import qut.pm.setm.SETMConfigParams;
import qut.pm.setm.SETMConfiguration;
import qut.pm.setm.evaluation.EarthMoversTunedMeasure;
import qut.pm.setm.fitness.metrics.MeasureEvaluatorFactory;
import qut.pm.setm.observer.ExportObserver;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.measures.AntiEntropicRelevanceZeroOrderMeasureBruteScale;
import qut.pm.spm.measures.LogStatsCache;
import qut.pm.spm.measures.SimplicityEdgeCountMeasure;
import qut.pm.spm.playout.CachingPlayoutGenerator;
import qut.pm.spm.ppt.ProbProcessTree;

/**
 *  Target Adhesion and Simplicity ONLY. Native metrics.
 * @author burkeat
 *
 */
public class SETMConfigAdhesionSimpNMEval extends SETMConfiguration{

	// Note to anti-target entropy
	
	@Override
	protected LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> initMeasures(ProvenancedLog log, 
			SETMConfigParams cp, ExportObserver exportObserver) 
	{
		LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> alg 
			= new LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double>();
		MeasureEvaluatorFactory mef = new MeasureEvaluatorFactory(exportObserver);
		CachingPlayoutGenerator generator = new CachingPlayoutGenerator( cp.playoutGranularity );
		addMeasure(log, cp, alg, mef, new EarthMoversTunedMeasure());
		addMeasure(log, cp,alg, mef, new AntiEntropicRelevanceZeroOrderMeasureBruteScale(generator));
		LogStatsCache statsCache = new LogStatsCache();
		addMeasure(log, cp, alg, mef, new SimplicityEdgeCountMeasure(statsCache));
		return alg;
	}

	
}
