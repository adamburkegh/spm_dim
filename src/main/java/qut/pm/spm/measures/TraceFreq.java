package qut.pm.spm.measures;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TraceFreq{
	private Map<List<String>,Long> traceFreq = new HashMap<>();
	private long traceTotal = 0;
	
	public Map<List<String>, Long> getTraceFreq() {
		return traceFreq;
	}

	public long getTraceTotal() {
		return traceTotal;
	}

	public long getFreq(List<String> subtrace) {
		Long result = traceFreq.get(subtrace);
		if (result == null) {
			return 0;
		}
		return result;
	}
	
	public void incTraceFreq(List<String> subtrace) {
		long freq = getFreq(subtrace);
		traceFreq.put(subtrace,freq+1);
		traceTotal += 1;
	}
	
	public void putFreq(List<String> subtrace, long freq) {
		long oldFreq = getFreq(subtrace);
		traceFreq.put(subtrace,freq);
		traceTotal += freq - oldFreq;
	}

	public void forceTraceTotal(long traceTotal) {
		this.traceTotal = traceTotal;
	}
	
	public Set<List<String>> keySet() {
		return traceFreq.keySet();
	}

	public String format() {
		return "TraceFreq [traceFreq=" + traceFreq + "]";
	}
	
}