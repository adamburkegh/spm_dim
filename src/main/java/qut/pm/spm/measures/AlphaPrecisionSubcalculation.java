package qut.pm.spm.measures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import qut.pm.spm.TraceFreq;
import weka.core.matrix.Matrix;

public class AlphaPrecisionSubcalculation {

	private static Logger LOGGER = LogManager.getLogger();

	/**
	 * 
	 *  K = sum_{i=1}^gamma o^T D^{i-1}f
	 *	gamma = maxTraceLength
	 *  o^T = headV of start activities
	 *	f = finalV of tail activities
	 *  D = indicator matrix of direct follow pairs
	 * 
	 * @param tfLog
	 * @param activities
	 * @param maxTraceLength
	 * @return
	 */
	public double calculateRestrictedSystemSupportSize(TraceFreq tfLog, 
			Set<String> activities, int maxTraceLength) {
		// activity vector and index maps
		List<String> acts = new ArrayList<String>(activities);
		Map<String,Integer> actToIndex = new HashMap<String,Integer>();
		for(int i=0; i<acts.size(); i++) {
			actToIndex.put(acts.get(i), i);
		}
		// build head, tails and direct follows matrix
		Set<String> heads = new HashSet<String>();
		Set<String> tails = new HashSet<String>();
		Matrix dfm = new Matrix(acts.size(),acts.size());
		Map<List<String>, Double> tfl = tfLog.getTraceFreq();
		buildIndicatorMatrices(actToIndex, tfl.keySet(), heads, tails, dfm);
		Matrix headV = new Matrix(1,activities.size());
		Matrix finalV = new Matrix(activities.size(),1);
		for (String act: acts) {
			int actInd = actToIndex.get(act);
			if (heads.contains(act))
				headV.set(0, actInd, 1);
			if (tails.contains(act))
				finalV.set(actInd, 0, 1);
		}
		// calculate support 
		Set<String> both = new HashSet<String>(heads);
		both.retainAll(tails);
		double support = both.size(); // 
		Matrix dfi = Matrix.identity(acts.size(),acts.size());;
		LOGGER.debug("o^T =" + headV);
		LOGGER.debug("f =\n" + finalV);
		for (int i=2; i<=maxTraceLength; i++) {
			dfi = dfi.times(dfm);
			LOGGER.debug("D^" + (i-1) + "=\n" + dfi.toString());
			double inc = matrixTotal( headV.times(dfi).times(finalV) );
			LOGGER.debug("o^T D^" + (i-1) + "=\n" + headV.times(dfi)  );
			LOGGER.debug("o^T D^" + (i-1) + " f=" + inc);
			LOGGER.debug("o^T D^" + (i-1) + " f=" + headV.times(dfi).times(finalV));
			support += inc;
		}
		return support;
	}

	/** 
	 * Note heads and tails, and dfm are mutated.
	 *  
	 * @param actToIndex
	 * @param tflKeys
	 * @param heads
	 * @param tails
	 * @param dfm
	 */
	private void buildIndicatorMatrices(Map<String, Integer> actToIndex, 
			Set<List<String>> tflKeys, Set<String> heads, Set<String> tails, 
			Matrix dfm) 
	{
		for (List<String> trace: tflKeys) {
			heads.add(trace.get(0));
			tails.add(trace.get(trace.size()-1));
			String prev = null;
			for (String event: trace) {
				if (prev == null) {
					prev = event;
					continue;
				}
				int ctInd = actToIndex.get(event);
				int prevInd = actToIndex.get(prev);
				dfm.set(prevInd, ctInd, 1);
				prev = event;
			}
		}
	}

	private double matrixTotal(Matrix mat) {
		double result = 0;
		for (int i=0; i<mat.getRowDimension(); i++) {
			for (int j=0; j<mat.getColumnDimension(); j++) {
				result += mat.get(i, j);
			}
		}
		return result;
	}

	public double calculateUnrestrictedSystemSupportSize(int numberActivities, int maxTraceLength) {
		double result = 0;
		for (int i=1; i<maxTraceLength; i++) {
			result += Math.pow(numberActivities,i);
		}
		return result;
	}

	
}
