package qut.pm.spm.measures.relevance;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.MathUtils;
import qut.pm.spm.TraceFreq;

/**
 * 
 * Alkhammash, H., Polyvyanyy, A., Moffat, A., & Garcia-Banuelos, L. (2021).
 * Entropic relevance: A mechanism for measuring stochastic process models
 * discovered from event data. Information Systems, 101922.
 * https://doi.org/10.1016/j.is.2021.101922
 */
public class RelevanceCalculator {

	private static Logger LOGGER = LogManager.getLogger();
	
	protected RelevanceBackgroundCost bgCost;
	protected RelevancePreludeCost preludeCost;
	
	public RelevanceCalculator(RelevanceBackgroundCost bgCost, RelevancePreludeCost preludeCost) {
		this.bgCost = bgCost;
		this.preludeCost = preludeCost;
	}
	
	/**
	 * 
	 * For log E and model M
	 * 
	 * rel_x(E,M) = H_0(\rho(E,M)) 
	 * 			    + 1/|E| . sum_{t in E}( n(t,E). cost_x(t,E,M))
	 * 				+  prelude_x(E,M) /|E|
	 * 
	 * @param logFSL
	 * @param modelTF
	 * @return
	 */
	public double relevance(FiniteStochasticLang logFSL, TraceFreq modelTF) {
		bgCost.initCostRun(logFSL, modelTF);
		preludeCost.initCostRun(logFSL, modelTF);
		return selectorCost(logFSL.getTraceFrequency(),modelTF)
				+ traceCompressionCost(logFSL, modelTF )
				+ preludeCost.cost(logFSL,modelTF) ;
	}

	private double traceCompressionCost(FiniteStochasticLang logFSL, TraceFreq modelTF) {
		// 1/|E| sum of n * cost_x(t,E,M)
		TraceFreq logTF = logFSL.getTraceFrequency();
		double sum = 0d;
		double totalModelCost = 0d;
		double totalBgCost = 0d;
		for (List<String> trace: logTF.keySet()) {
			double mf = modelTF.getFreq(trace);
			double cost = 0d;
			if (mf > 0) {
				cost = logTF.getFreq(trace)*modelCost(modelTF, trace);
				totalModelCost += cost;
			}else {
				cost = logTF.getFreq(trace)*bgCost.cost(trace,logFSL,modelTF);
				totalBgCost += cost;
			}
			sum += cost;
		}
		LOGGER.debug("Trace compression costs; total: {} model: {} background: {}", 
				sum / logTF.getTraceTotal(), totalModelCost / logTF.getTraceTotal(), 
				totalBgCost / logTF.getTraceTotal());
		return sum / logTF.getTraceTotal();
	}

	private double modelCost(TraceFreq modelTF, List<String> trace) {
		return -1.0d * MathUtils.log2( modelTF.getFreq(trace)/ modelTF.getTraceTotal() );
	}

	public double selectorCost(TraceFreq logTF, TraceFreq modelTF) {
		double sum = 0d;
		boolean fullCoverage = true;
		for (List<String> trace: logTF.keySet()) {
			if (modelTF.getFreq(trace) > 0) {
				sum += logTF.getFreq(trace);
			}else {
				fullCoverage = false;
			}
		}
		if (sum == 0d || fullCoverage)
			return 0;
		double rho = sum / logTF.getTraceTotal();
		// -q log2(q) - (1-q)log2(1-q)
		return -1 * (rho * MathUtils.log2(rho) + (1 - rho)*MathUtils.log2(1 - rho));
	}
	
}
