package qut.pm.setm.fitness;

import java.util.LinkedHashMap;

import org.uncommons.watchmaker.framework.FitnessEvaluator;

import qut.pm.setm.SETMConfigParams;
import qut.pm.setm.SETMConfiguration;
import qut.pm.setm.fitness.metrics.MeasureEvaluatorFactory;
import qut.pm.setm.observer.ExportObserver;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.measures.AdhesionDRMeasure;
import qut.pm.spm.measures.RelevanceDRMeasure;
import qut.pm.spm.measures.SimplicityDRMeasure;
import qut.pm.spm.playout.CachingPlayoutGenerator;
import qut.pm.spm.ppt.ProbProcessTree;

/**
 *  Target Adhesion, Entropy and Simplicity. Dimensional realism.
 * @author burkeat
 *
 */
public class SETMConfigRelevanceSimpDR extends SETMConfiguration{

	
	@Override
	protected LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> initMeasures(ProvenancedLog log, 
			SETMConfigParams cp, ExportObserver exportObserver) 
	{
		LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double> alg 
			= new LinkedHashMap<FitnessEvaluator<ProbProcessTree>, Double>();
		MeasureEvaluatorFactory mef = new MeasureEvaluatorFactory(exportObserver);
		CachingPlayoutGenerator generator = new CachingPlayoutGenerator( cp.playoutGranularity );
		addMeasure(log, cp, alg, mef, new AdhesionDRMeasure(generator));
		addMeasure(log, cp, alg, mef, new RelevanceDRMeasure(generator));
		addMeasure(log, cp, alg, mef, new SimplicityDRMeasure(generator));
		return alg;
	}
	


	
}
