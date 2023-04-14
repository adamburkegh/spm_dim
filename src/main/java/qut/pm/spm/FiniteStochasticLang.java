package qut.pm.spm;

import java.util.List;
import java.util.Set;

public class FiniteStochasticLang {

	private TraceFreq traceFreq;
	private ActivityFreq activityFreq;

	public FiniteStochasticLang(TraceFreq traceFreq, ActivityFreq activityFreq) {
		this.traceFreq = traceFreq;
		this.activityFreq = activityFreq;
	}
	
	public TraceFreq getTraceFrequency() {
		return traceFreq;
	}
	
	public ActivityFreq getActivityFrequency() {
		return activityFreq;
	}
	
	public double getFreq(List<String> trace) {
		return traceFreq.getFreq(trace);
	}

	public double getActivityFreq(String trace) {
		return activityFreq.getFreq(trace);
	}

	public double getTotalEvents() {
		return activityFreq.getTotalEvents();
	}

	public Set<String> getActivities() {
		return activityFreq.getActivities();
	}

	public double getTraceTotal() {
		return traceFreq.getTraceTotal();
	}

	
}
