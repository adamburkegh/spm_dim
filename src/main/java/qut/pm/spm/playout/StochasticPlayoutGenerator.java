package qut.pm.spm.playout;

import static qut.pm.xes.helpers.XESLogUtils.XES_CONCEPT_NAME;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.deckfour.xes.model.impl.XLogImpl;
import org.deckfour.xes.model.impl.XTraceImpl;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;
import org.processmining.models.semantics.petrinet.EfficientPetrinetSemantics;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.models.semantics.petrinet.impl.EfficientPetrinetSemanticsImpl;

import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;

import qut.pm.spm.AcceptingStochasticNet;

public class StochasticPlayoutGenerator implements PlayoutGenerator {
	

	private final class TimedTransitionComparator implements Comparator<TimedTransition> {
		private final Map<TimedTransition, Integer> leftoverTracker;
		private final Map<TimedTransition, Integer> pathAllocation;

		private TimedTransitionComparator(Map<TimedTransition, Integer> leftoverTracker,
				Map<TimedTransition, Integer> pathAllocation) {
			this.leftoverTracker = leftoverTracker;
			this.pathAllocation = pathAllocation;
		}

		@Override
		public int compare(TimedTransition tran1, TimedTransition tran2) {
			Integer alloc1 = pathAllocation.get(tran1);
			Integer alloc2 = pathAllocation.get(tran2);
			Integer lw1 = leftoverTracker.getOrDefault(tran1,0);
			Integer lw2 = leftoverTracker.getOrDefault(tran2,0);
			if (lw1.intValue() != lw2.intValue() )
				return Integer.compare(lw1,lw2);
			if (alloc1.intValue() != alloc2.intValue()) 
				return Integer.compare(alloc1.intValue(),alloc2.intValue());
			String t1l = tran1.getLabel() == null ? "" : tran1.getLabel(); 
			String t2l = tran2.getLabel() == null ? "" : tran2.getLabel();
			int lexComp = t1l.compareTo(t2l);
			if (lexComp != 0) {
				return lexComp;
			}
			return tran1.compareTo(tran2);
		}
	}

	private static class LightLog implements Iterable<LightTrace>{
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
		
	}
	
	private static class LightTrace implements Comparable<LightTrace>, Iterable<String>{
		private List<String> events = new LinkedList<String>();
		private boolean forcedTruncate = false;
		
		public LightTrace(LightTrace parentTrace) {
			this.events.addAll(parentTrace.events);
		}

		public LightTrace() {
			super();
		}

		public void addEvent(String event) {
			events.add(event);
		}
		
		public void forceTruncate() {
			this.forcedTruncate = true;
		}
		
		public boolean isForcedTruncate() {
			return forcedTruncate;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((events == null) ? 0 : events.hashCode());
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
			LightTrace other = (LightTrace) obj;
			if (events == null) {
				if (other.events != null)
					return false;
			} else if (!events.equals(other.events))
				return false;
			return true;
		}

		@Override
		public int compareTo(LightTrace other) {
			Iterator<String> iter = events.iterator();
			Iterator<String> otherIter = other.events.iterator();
			while (iter.hasNext()) {
				if (!otherIter.hasNext())
					return 1;
				String elem = iter.next();
				String otherElem = otherIter.next();
				int elemCompare = elem.compareTo(otherElem);
				if (elemCompare != 0)
					return elemCompare;
			}
			if (otherIter.hasNext())
				return -1;
			return 0; 
		}

		public int size() {
			return events.size();
		}

		@Override
		public Iterator<String> iterator() {
			return events.iterator();
		}
		
		public String toString() {
			return events.toString() + (isForcedTruncate()?" (truncated)":"");
		}
		
	}

	private static class PlayoutState{
		public int targetSize;
		public Marking ctMarking; 
		public LightTrace parentTrace;
		public int traceLength;
		
		public PlayoutState(int targetSize, Marking ctMarking, LightTrace parentTrace, int traceLength) {
			super();
			this.targetSize = targetSize;
			this.ctMarking = ctMarking;
			this.parentTrace = parentTrace;
			this.traceLength = traceLength;
		}
		
	}

	private static Logger LOGGER = LogManager.getLogger();
	
	private static final int TRACE_PROGRESS_INCREMENT = 500;
	private int traceProgressIncrement = TRACE_PROGRESS_INCREMENT;

	
	private static final int DEFAULT_MAX_TRACE_LENGTH = 50000;

	private static final int LOGGING_THRESHOLD = 100;
	private int maxTraceLength;

	private boolean maxTraceLengthWarn = true;
	
	public StochasticPlayoutGenerator(int maxTraceLength) {
		this.maxTraceLength = maxTraceLength;
		if (LOGGER.getLevel() == Level.DEBUG || LOGGER.getLevel() == Level.TRACE)
			traceProgressIncrement = 1;
	}
	
	public StochasticPlayoutGenerator() {
		this(DEFAULT_MAX_TRACE_LENGTH);
	}
	
	private void track(int targetSize, String message) {
		if (targetSize > LOGGING_THRESHOLD) {
			LOGGER.info(message);
		}else {
			LOGGER.debug(message);
		}
	}
	
	/** 
	 * GSPNs only (even though the type interface takes GDT_SPNs).
	 * 
	 * @param net
	 * @param targetSize
	 * @param ctMarking
	 * @return
	 */
	@Override
	public XLog buildPlayoutLog(AcceptingStochasticNet net, int targetSize) {
		track(targetSize, "Generating playout log for net " + net.getId() 
						+ " with " + targetSize + " traces");
		LightTrace trace = new LightTrace();
		resetMaxLengthWarning();
		Stack<PlayoutState> calcStack = new Stack<>();
		PlayoutState initState = new PlayoutState(targetSize, net.getInitialMarking(), 
				  								 trace, 0);
		calcStack.push(initState);
		LightLog result = new LightLog();
		int progressTrack = traceProgressIncrement;
		Map<TimedTransition,Integer> leftoverTracker = new HashMap<>();
		while(!calcStack.isEmpty()) {
			PlayoutState ctState = calcStack.pop();
			logUnion(result, 
					buildPlayoutLog(calcStack,net,leftoverTracker,
							ctState.targetSize,ctState.ctMarking, 
												   ctState.parentTrace, ctState.traceLength));
			if (result.size() > progressTrack) {
				track(targetSize, "Generated " + result.size() + " of " + targetSize + " "
						+ "traces for net " + net.getId() + " with " + targetSize + " traces");
				progressTrack += traceProgressIncrement;
			}
		}
		track(targetSize,"Generated " + result.size() + " of " + targetSize + " "
				+ "traces for net " + net.getId() + " with " + targetSize + " traces");		
		reportTruncation(result);
		return convertToXLog(result);
	}

	private void reportTruncation(LightLog log) {
		int trunc = 0;
		LOGGER.debug("Checking truncation ...");
		for (LightTrace trace: log) {
			if (trace.isForcedTruncate()) {
				LOGGER.debug(trace);
				trunc++;
			}
		}
		if (trunc > 0)
			LOGGER.info("Force truncated " + trunc + " of " + log.size());
	}

	private void resetMaxLengthWarning() {
		maxTraceLengthWarn = true;
	}
	
	private XLog convertToXLog(LightLog llog) {
		XAttributeMap attrMap = new XAttributeMapImpl();
		XLog result = new XLogImpl(attrMap);
		for (LightTrace trace: llog) {
			XTrace xtrace = convertToXEventTrace(trace);
			result.add(xtrace);
		}
		return result;
	}

	private XTrace convertToXEventTrace(LightTrace trace) {
		XAttributeMap traceAttrMap = new XAttributeMapImpl();
		XTrace result = new XTraceImpl(traceAttrMap);
		for (String event: trace) {
			XAttributeMap eventAttrMap = new XAttributeMapImpl();
			XAttribute attr = new XAttributeLiteralImpl(XES_CONCEPT_NAME,event);
			eventAttrMap.put(XES_CONCEPT_NAME, attr);
			XEvent xevent = new XEventImpl(eventAttrMap);		
			result.add(xevent);
		}
		return result;
	}

	/** 
	 * GSPNs only (even though the type interface takes GDT_SPNs).
	 * 
	 * @param net
	 * @param targetSize
	 * @param ctMarking
	 * @return
	 */
	public LightLog buildPlayoutLog(Stack<PlayoutState> calcStack, AcceptingStochasticNet net, 
			Map<TimedTransition,Integer> leftoverTracker,
			int targetSize, Marking ctMarking, LightTrace parentTrace, int traceLength) 
	{	
		if (targetSize < 0) {
			LOGGER.error("Negative target size");
			throw new RuntimeException("Negative target size building playout log");
		}
		if (targetSize == 0)
			return new LightLog();
		if (traceLength >= maxTraceLength){
			maxTraceLengthWarn(net);
			LightTrace newTrace = new LightTrace(parentTrace);
			newTrace.forceTruncate();
			return generateDuplicateTraces(newTrace,targetSize);
		}
		EfficientPetrinetSemantics semantics = 
				new EfficientPetrinetSemanticsImpl(net.getNet(),ctMarking);
		if ( semantics.getExecutableTransitions().isEmpty() 
				||  net.getFinalMarkings().contains(ctMarking)  ) {
			return generateDuplicateTraces(parentTrace,targetSize);
		}
		Map<TimedTransition, Integer> pathAllocation = 
				allocateEnabledByWeight(net, leftoverTracker, targetSize, semantics);
		LOGGER.debug( "Budget:" + targetSize + "; Parent trace length:" + parentTrace.size() 
					+ "; allocation:" + pathAllocation );
		int allocated = 0;
		for (TimedTransition tran: pathAllocation.keySet()) {
			Integer sublogBudget = pathAllocation.get(tran);
			if (sublogBudget == 0)
				continue;
			semantics.setStateAsMarking(ctMarking);
			LOGGER.debug("Transition: " + tran.getLabel());
			LightTrace trace = generateTraceWithEvent(parentTrace, tran);
			semantics.directExecuteExecutableTransition(tran);
			PlayoutState newState = new PlayoutState(sublogBudget, semantics.getStateAsMarking(), 
					  trace, traceLength+1);
			calcStack.push(newState);
			allocated += sublogBudget;
		}	
		if (allocated != targetSize) {
			LOGGER.warn("Allocation " + allocated + " differs from target size " + targetSize);
			LOGGER.info(pathAllocation);
			for (TimedTransition tran: pathAllocation.keySet()) {
				LOGGER.info(tranIdStr(tran));
			}
			LOGGER.info(ctMarking);
		}
		return new LightLog();
	}

	private void maxTraceLengthWarn(AcceptingStochasticNet net) {
		if (maxTraceLengthWarn) {
			LOGGER.warn("Max trace length (" + maxTraceLength 
					+ ") tripped for playout log for net " + net.getId());
			maxTraceLengthWarn = false;
		}
	}

	private LightLog generateDuplicateTraces(LightTrace parentTrace, int targetSize) {
		LightLog result = new LightLog();
		for (int i=0; i<targetSize; i++) {
			result.addTrace(parentTrace);
		}
		return result;
	}

	private LightTrace generateTraceWithEvent(LightTrace parentTrace, TimedTransition tran) {
		LightTrace trace = new LightTrace(parentTrace);
		if (tran.isInvisible())
			return trace;
		trace.addEvent(tran.getLabel());
		return trace;
	}

	private void logUnion(LightLog result, LightLog subLog) {
		for (LightTrace subtrace: subLog) {
			result.addTrace(subtrace);
		}
	}

	private Map<TimedTransition, Integer> allocateEnabledByWeight(AcceptingStochasticNet net, 
			Map<TimedTransition,Integer> leftoverTracker,
			int targetSize, EfficientPetrinetSemantics semantics) {
		Collection<Transition> enabledTransitions = semantics.getExecutableTransitions();
		double immediateWeights = 0;
		double totalWeights = 0;
		for (Transition tran: enabledTransitions) {
			TimedTransition ttran = (TimedTransition)tran;
			if (DistributionType.IMMEDIATE == ttran.getDistributionType()){
				immediateWeights += ttran.getWeight();
			}
			totalWeights += ttran.getWeight();
		}
		Map<TimedTransition,Integer> pathAllocation = new HashMap<>();
		int pathTotalAllocated = 0;
		if (immediateWeights > 0) { // immediate transitions take priority
			pathTotalAllocated = allocateForImmediates(targetSize, enabledTransitions, 
					immediateWeights, pathAllocation,pathTotalAllocated);			
		}else {
			pathTotalAllocated = allocateForTimedOnly(targetSize, enabledTransitions, totalWeights, 
					pathAllocation, pathTotalAllocated);						
		}
		int leftover = targetSize - pathTotalAllocated;
		if (leftover < 0)
			LOGGER.warn("Overallocated path for marking", semantics.getStateAsMarking());
		if (leftover > 0)
			allocateLeftover(pathAllocation,leftoverTracker,leftover);
		return pathAllocation;
	}


	private int allocateForTimedOnly(int targetSize, Collection<Transition> enabledTransitions, 
			double totalWeights,Map<TimedTransition, Integer> pathAllocation, int pathTotalAllocated) 
	{
		for (Transition tran: enabledTransitions) {
			TimedTransition ttran = (TimedTransition)tran;
			int pathAllocationValue = 
					(int) Math.floor(targetSize* ttran.getWeight() / totalWeights);
			pathAllocation.put(ttran,pathAllocationValue);
			pathTotalAllocated += pathAllocationValue; 
		}
		return pathTotalAllocated;
	}

	private int allocateForImmediates(int targetSize, Collection<Transition> enabledTransitions,
			double immediateWeights, Map<TimedTransition, Integer> pathAllocation, int pathTotalAllocated) {
		for (Transition tran: enabledTransitions) {
			TimedTransition ttran = (TimedTransition)tran;
			if (DistributionType.IMMEDIATE != ttran.getDistributionType())
				continue;
			int pathAllocationValue = 
					(int) Math.floor(targetSize* ttran.getWeight() / immediateWeights);
			pathAllocation.put(ttran,pathAllocationValue);
			pathTotalAllocated += pathAllocationValue; 
		}
		return pathTotalAllocated;
	}


	protected void allocateLeftover(Map<TimedTransition, Integer> pathAllocation, 
			Map<TimedTransition,Integer> leftoverTracker, int leftover) 
	{
		TimedTransitionComparator tc = new TimedTransitionComparator(leftoverTracker, pathAllocation);
		SortedSet<TimedTransition> leftoverPriority 
			= new TreeSet<>(tc);
		for (TimedTransition tran: pathAllocation.keySet()) {
			leftoverPriority.add(tran);
		}
		for (TimedTransition tran: leftoverPriority) {
			if (leftover == 0)
				return;
			Integer weight = pathAllocation.getOrDefault(tran,0);
			pathAllocation.put(tran,weight+1);
			Integer leftoverTrack = leftoverTracker.getOrDefault(tran,0);
			leftoverTracker.put(tran,leftoverTrack+1);
			leftover--;
		}
	}

	private String tranIdStr(TimedTransition tran) {
		return tran.getLabel() + "::" + tran.getId();
	}


}
