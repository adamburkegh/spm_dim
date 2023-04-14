package qut.pm.spm.measures;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import qut.pm.spm.TraceFreq;

/**
 * 
 * Generalization by Trace Uniqueness Difference. 
 * See Definition 15 in van der Aalst (2018) - Relating Process Models and 
 * Event Logs: 21 Conformance Propositions. 
 * Or TGDU in the dimensions paper.
 * 
 * @author burkeat
 *
 */
public class GenTraceDiffCalculation {

	public double calculateGeneralizationTraceDiff(TraceFreq traceFreqLog, TraceFreq traceFreqModel) {
		double ct = 0.0d;
		Set<List<String>> keys = new HashSet<>(traceFreqLog.keySet());
		keys.addAll(traceFreqModel.keySet());
		for (List<String> trace : keys ) {
			double flog = traceFreqLog.getFreq(trace);
			double fmodel = traceFreqModel.getFreq(trace);
			if (flog > 0 && fmodel > 0) {
				double inc = Math.min(flog,fmodel) - 1;
				// This check is required because TraceFreq objects may have
				// fractional frequencies after scaling
				if (inc > 0)
					ct += inc;
			}
		}
		return ct / (double)traceFreqLog.getTraceTotal();
	}

}
