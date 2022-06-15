package qut.pm.setm.fitness.metrics;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.uncommons.watchmaker.framework.FitnessEvaluator;

import qut.pm.setm.observer.ExportObserver;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.measures.StochasticLogCachingMeasure;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.util.ClockUtil;

public class MeasureEvaluatorFactory {
	
	// Concurrency risk point
	private Map<ProbProcessTree,AcceptingStochasticNet> netCache = 
			Collections.synchronizedMap( new WeakHashMap<>());
	private ExportObserver exportObserver;

	private static class MeasureBridge implements FitnessEvaluator<ProbProcessTree>{

		private StochasticLogCachingMeasure measure;
		private XLog log;
		private XEventClassifier classifier;
		private ProbProcessTreeConverter converter = new ProbProcessTreeConverter(); 
		private Map<ProbProcessTree,AcceptingStochasticNet> netCache;
		private ExportObserver exportObserver;
		
		public MeasureBridge(StochasticLogCachingMeasure measure, XLog log, 
							 XEventClassifier classifier,
							 Map<ProbProcessTree,AcceptingStochasticNet> netCache,
							ExportObserver exportObserver)
		{
			this.measure = measure;
			this.log = log;
			this.classifier = classifier;
			this.netCache = netCache;
			this.exportObserver = exportObserver;
		}
		
		public void precalculateForLog() {
			measure.precalculateForLog(log,classifier);
		}
		
		@Override
		public double getFitness(ProbProcessTree candidate, List<? extends ProbProcessTree> population) {
			AcceptingStochasticNet anet = netCache.get(candidate);
			if (anet == null) {
				anet = converter.convertToSNet(candidate);
				netCache.put(candidate,anet);
			}
			long start = ClockUtil.currentTimeMillis();
			double result = measure.calculate(log, anet, classifier);
			long end = ClockUtil.currentTimeMillis();
			long duration = end - start;
			exportObserver.recordMeasure(candidate,measure.getMeasure(),result,duration);
			return result;
		}

		@Override
		public boolean isNatural() {
			return true;
		}
		
		@Override
		public String toString() {
			return "MeasureEvaluatorFactory.MeasureBridge[" + measure.getReadableId() + "]";
		}
	}
	
	public MeasureEvaluatorFactory(ExportObserver exportObserver) {
		this.exportObserver = exportObserver;
	}
	
	
	public FitnessEvaluator<ProbProcessTree> createEvaluator(StochasticLogCachingMeasure measure, 
															 XLog log, XEventClassifier classifier)
	{
		MeasureBridge bridge = new MeasureBridge(measure,log,classifier,netCache, exportObserver);
		bridge.precalculateForLog();
		return bridge;
	}
	
}
