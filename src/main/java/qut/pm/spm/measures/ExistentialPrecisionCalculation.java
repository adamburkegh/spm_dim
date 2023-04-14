// Copy-paste fork from Toothpaste project
// https://github.com/adamburkegh/toothpaste
// qut.pm.spm.conformance.AlphaPrecisionCalculation
// Feb 1 2023

package qut.pm.spm.measures;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import qut.pm.spm.TraceFreq;

/**
 * Implements a variation on the Alpha Precision measure, from
 * Depaire et al (2022) - Alpha Precision: Estimating the Significant System 
 * Behavior in a Model 
 * 
 * Current implementation is 
 * P_na(M,S) = sum_{sigma in L} prob_M(sigma) I_alpha(sigma)
 * 
 * not 
 * P_alpha(M,S) = sum_{sigma in M} prob_M(sigma) I_alpha(sigma)
 * 
 * as in the paper.
 * 
 * @author burkeat
 *
 */
public class ExistentialPrecisionCalculation {
	
	private static Logger LOGGER = LogManager.getLogger();

	private AlphaPrecisionSubcalculation apsCalc = new AlphaPrecisionSubcalculation();
	
	public double calculateUnrestrictedPrecision(TraceFreq tfLog, TraceFreq tfModel, 
			int logTotalEvents, double alphaSig,int numberActivities, 
			int maxTraceLength) 
	{
		double supportSize = 
				apsCalc.calculateUnrestrictedSystemSupportSize(numberActivities,
														maxTraceLength);
		return calculatePrecision(tfLog, tfModel, logTotalEvents, alphaSig, 
								  supportSize);
	}

	public double calculateRestrictedPrecision(TraceFreq tfLog, TraceFreq tfModel,
			Set<String> activities, int logTotalEvents, double alphaSig,
			int maxTraceLength) 
	{
		double supportSize = 
				apsCalc.calculateRestrictedSystemSupportSize(tfLog,activities,
											         maxTraceLength);
		return calculatePrecision(tfLog, tfModel, logTotalEvents, alphaSig, 
				supportSize);
	}


	private double calculatePrecision(TraceFreq tfLog, TraceFreq tfModel, 
			int logTotalEvents, double alphaSig, double supportSize) {
		Map<List<String>, Double> tfl = tfLog.getTraceFreq();
		double sumFreq = 0.0;
		for (List<String> trace: tfl.keySet()) {
			double probEstimate = (1+tfl.get(trace))/(supportSize + logTotalEvents);
			if (probEstimate > alphaSig)
				sumFreq += tfModel.getFreq(trace);
		}
		double result = sumFreq/tfModel.getTraceTotal();
		if ((result < 0 || result > 1) && LOGGER.isInfoEnabled()) {
			LOGGER.warn("calculatePrecision() out of bounds: {}", result);
			LOGGER.warn("calculatePrecision(tflog={},tfModel={}," );
			LOGGER.warn("..logTotalEvents={},alphaSig={},supportSize={})" );
		}
		return result;
	}


}
