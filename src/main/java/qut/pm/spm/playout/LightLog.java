package qut.pm.spm.playout;

import java.util.Iterator;

import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.TreeMultiset;

import qut.pm.spm.ActivityFreq;
import qut.pm.spm.FiniteStochasticLang;
import qut.pm.spm.TraceFreq;

public class LightLog implements Iterable<LightTrace>{
	private Multiset<LightTrace> traces = TreeMultiset.create();
	
	public void addTrace(LightTrace trace) {
		traces.add(trace);
	}

	@Override
	public Iterator<LightTrace> iterator() {
		return traces.iterator();
	}
	
	public int size() {
		return traces.size();
	}
	
	public FiniteStochasticLang  convertToFiniteStochasticLang() {
		TraceFreq traceFreq = new TraceFreq();
		ActivityFreq activityFreq = new ActivityFreq();
		for( Entry<LightTrace> traceEntry: traces.entrySet()) {
			traceFreq.putFreq(traceEntry.getElement().asList(), 
							  traceEntry.getCount());
			for (String event: traceEntry.getElement() ) {
				activityFreq.incActiviyFreq(event, traceEntry.getCount());
			}
		}
		FiniteStochasticLang fsl = new FiniteStochasticLang(traceFreq,activityFreq);
		return fsl;
	}
	
}