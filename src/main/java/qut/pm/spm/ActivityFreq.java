package qut.pm.spm;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Allows R+ valued frequencies. This makes it possible to model scaled play-out logs with non-natural
 * frequencies.  
 * 
 * @author burkeat
 *
 */
public class ActivityFreq{
	private Map<String,Double> actFreq = new HashMap<>();
	private double actTotal = 0;
	
	
	public Map<String, Double> getActivityFreq() {
		return actFreq;
	}

	public double getTotalEvents() {
		return actTotal;
	}

	public double getFreq(String act) {
		Double result = actFreq.get(act);
		if (result == null) {
			return 0;
		}
		return result;
	}
	
	public void incActivityFreq(String activity) {
		double freq = getFreq(activity);
		actFreq.put(activity,freq+1);
		actTotal += 1;
	}
	
	public void incActiviyFreq(String activity, double plusFreq) {
		double freq = getFreq(activity);
		actFreq.put(activity,freq+plusFreq);
		actTotal += plusFreq;
	}
		
	public void putFreq(String activity, double freq) {
		double old = getFreq(activity);
		actFreq.put(activity, freq);
		actTotal = freq - old;
	}

	public Set<String> getActivities() {
		return Collections.unmodifiableSet(actFreq.keySet());
	}


	
	public String format() {
		return "ActivityFreq [actFreq=" + actFreq + "]";
	}
	
	public String toString() {
		return format();
	}


	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((actFreq == null) ? 0 : actFreq.hashCode());
		long temp;
		temp = Double.doubleToLongBits(actTotal);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ActivityFreq other = (ActivityFreq) obj;
		if (actFreq == null) {
			if (other.actFreq != null)
				return false;
		} else if (!actFreq.equals(other.actFreq))
			return false;
		if (Double.doubleToLongBits(actTotal) != Double.doubleToLongBits(other.actTotal))
			return false;
		return true;
	}

	
}