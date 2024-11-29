package qut.pm.setm.fitness;

import java.util.LinkedHashMap;

import org.uncommons.watchmaker.framework.FitnessEvaluator;

import qut.pm.setm.SETMConfigParams;
import qut.pm.setm.SETMConfiguration;
import qut.pm.setm.fitness.metrics.MeasureEvaluatorFactory;
import qut.pm.setm.observer.ExportObserver;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.measures.EntropicRelevanceZeroOrderMeasureBruteScale;
import qut.pm.spm.measures.GenTraceFloorMeasure;
import qut.pm.spm.playout.CachingPlayoutGenerator;
import qut.pm.spm.ppt.ProbProcessTree;

/**
 *  Target Adhesion and Entropy ONLY. Native metrics.
 * @author burkeat
 *
 */
public class SETMConfigAdhesionEntropyNMExp extends SETMConfiguration{

	
	@Override
	protected LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> initMeasures(ProvenancedLog log, 
			SETMConfigParams cp, ExportObserver exportObserver) 
	{
		LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> alg 
			= new LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double>();
		MeasureEvaluatorFactory mef = new MeasureEvaluatorFactory(exportObserver);
		CachingPlayoutGenerator generator = new CachingPlayoutGenerator( cp.playoutGranularity );
		addMeasure(log, cp, alg, mef, new GenTraceFloorMeasure(generator,5));
		addMeasure(log, cp,alg, mef, new EntropicRelevanceZeroOrderMeasureBruteScale(generator));
		return alg;
	}
	


	
}