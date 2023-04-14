package qut.pm.spm;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class TraceFreqTest {


	@Test
	public void scaleByOne() {
		TraceFreq tf1 = createTraceFreq("a","b", "a");
		TraceFreq tf2 = createTraceFreq("a","b", "a");
		assertEquals(tf1,tf2);
		tf2.scaleBy(1);
		assertEquals(tf1,tf2);
	}

	@Test
	public void scaleByInt() {
		TraceFreq tf = createTraceFreq("a","b", "a");
		TraceFreq tfe = createTraceFreq("a","b", "a", "a","b", "a");
		tf.scaleBy(2);
		assertEquals(tfe,tf);
	}

	@Test
	public void scaleByFloat() {
		TraceFreq tf  = createTraceFreq("a");
		TraceFreq tfe = createTraceFreq();
		tfe.putFreq( Arrays.asList(new String[]{"a"}) , 3.2 );
		tf.scaleBy(3.2);
		assertEquals(tfe,tf);
	}

	
	
	public static TraceFreq createTraceFreq(String ... traces) {
		TraceFreq result = new TraceFreq();
		// TODO Set<String> activities = new HashSet<String>();
		for (String trace: traces) {
			result.incTraceFreq( toTrace(trace) );
			// activities.addAll( toActivities(trace) );
		}
		return result;
	}

	public static List<String> toTrace(String traceStr){
		List<String> result = new LinkedList<>();
		for (String substr: traceStr.split(" ")) {
			result.add(substr);
		}
		return result;
	}

	public static Set<String> toActivities(String traceStr){
		Set<String> result = new HashSet<String>();
		for (String substr: traceStr.split(" ")) {
			result.add(substr);
		}
		return result;
	}


	
}
