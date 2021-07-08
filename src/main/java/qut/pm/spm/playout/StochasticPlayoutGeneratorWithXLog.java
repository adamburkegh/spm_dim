package qut.pm.spm.playout;

import static qut.pm.xes.helpers.XESLogUtils.XES_CONCEPT_NAME;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

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

import qut.pm.spm.AcceptingStochasticNet;

public class StochasticPlayoutGeneratorWithXLog implements PlayoutGenerator {
	
	private static Logger LOGGER = LogManager.getLogger();
	
	private static final int MAX_TRACE_LENGTH = 300;
	private int maxTraceLength;

	private boolean maxTraceLengthWarn = true;
	
	public StochasticPlayoutGeneratorWithXLog(int maxTraceLength) {
		this.maxTraceLength = maxTraceLength;
	}
	
	public StochasticPlayoutGeneratorWithXLog() {
		this(MAX_TRACE_LENGTH);
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
		LOGGER.info("Generating playout log for net " + net.getId() 
					+ " with " + targetSize + " traces");
		XAttributeMap traceAttrMap = new XAttributeMapImpl();
		XTrace trace = new XTraceImpl(traceAttrMap);
		return buildPlayoutLog(net,targetSize,net.getInitialMarking(), trace, 0);
	}
	
	/** 
	 * GSPNs only (even though the type interface takes GDT_SPNs).
	 * 
	 * @param net
	 * @param targetSize
	 * @param ctMarking
	 * @return
	 */
	public XLog buildPlayoutLog(AcceptingStochasticNet net, int targetSize, Marking ctMarking, 
			XTrace parentTrace, int traceLength) 
	{
		if (targetSize < 0) {
			LOGGER.error("Negative target size");
			throw new RuntimeException("Negative target size building playout log");
		}
		XAttributeMap attrMap = new XAttributeMapImpl();
		if (targetSize == 0)
			return new XLogImpl(attrMap);
		if (traceLength >= maxTraceLength){
			maxTraceLengthWarn(net);
			return generateDuplicateTraces(parentTrace,attrMap ,targetSize);
		}
		EfficientPetrinetSemantics semantics = 
				new EfficientPetrinetSemanticsImpl(net.getNet(),ctMarking);
		if ( semantics.getExecutableTransitions().isEmpty() 
				||  net.getFinalMarkings().contains(ctMarking)  ) {
			return generateDuplicateTraces(parentTrace,attrMap ,targetSize);
		}
		Map<TimedTransition, Integer> pathAllocation = 
				allocateEnabledByWeight(net, targetSize, semantics);
		LOGGER.debug( "Budget:" + targetSize + "; Parent trace length:" + parentTrace.size() 
					+ "; allocation:" + pathAllocation );
		XLog result = new XLogImpl(attrMap);
		for (TimedTransition tran: pathAllocation.keySet()) {
			semantics.setStateAsMarking(ctMarking);
			LOGGER.debug("Transition: " + tran.getLabel());
			XTrace trace = generateTraceWithEvent(parentTrace, tran);
			semantics.directExecuteExecutableTransition(tran);
			Integer sublogBudget = pathAllocation.get(tran);
			XLog subLog = buildPlayoutLog(net, sublogBudget, semantics.getStateAsMarking(), trace, traceLength+1);
			logUnion(result, subLog);
		}		
		return result;
	}

	private void maxTraceLengthWarn(AcceptingStochasticNet net) {
		if (maxTraceLengthWarn) {
			LOGGER.warn("Max trace length (" + maxTraceLength 
					+ ") tripped for playout log for net " + net.getId());
			maxTraceLengthWarn = false;
		}
	}

	private XLog generateDuplicateTraces(XTrace parentTrace, XAttributeMap attrMap, int targetSize) {
		XLog result = new XLogImpl(attrMap);
		for (int i=0; i<targetSize; i++) {
			result.add(parentTrace);
		}
		return result;
	}

	private XTrace generateTraceWithEvent(XTrace parentTrace, TimedTransition tran) {
		XTrace trace = (XTrace)parentTrace.clone();
		if (tran.isInvisible())
			return trace;
		XAttributeMap eventAttrMap = new XAttributeMapImpl();
		XAttribute attr = new XAttributeLiteralImpl(XES_CONCEPT_NAME,tran.getLabel());
		eventAttrMap.put(XES_CONCEPT_NAME, attr);
		XEvent event = new XEventImpl(eventAttrMap);
		trace.add(event);
		return trace;
	}

	private void logUnion(XLog result, XLog subLog) {
		for (XTrace subtrace: subLog) {
			result.add(subtrace);
		}
	}

	private Map<TimedTransition, Integer> allocateEnabledByWeight(AcceptingStochasticNet net, int targetSize,
			EfficientPetrinetSemantics semantics) {
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
			LOGGER.warn("Overalloated path for marking", semantics.getStateAsMarking());
		if (leftover > 0)
			allocateLeftover(pathAllocation,leftover);
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


	protected void allocateLeftover(Map<TimedTransition, Integer> pathAllocation, int leftover) {
		SortedSet<TimedTransition> leftoverPriority = new TreeSet<>(new Comparator<TimedTransition>() {
			@Override
			public int compare(TimedTransition tran1, TimedTransition tran2) {
				Integer alloc1 = pathAllocation.get(tran1);
				Integer alloc2 = pathAllocation.get(tran2);
				if (0 == alloc1.intValue() ) {
					if (alloc2.intValue() > 0) {
						return -1;
					}
					int lexComp = tran1.getLabel().compareTo(tran2.getLabel());
					if (lexComp != 0) {
						return lexComp;
					}
					return tran1.compareTo(tran2);
				}
				return 0;
			}
		});
		for (TimedTransition tran: pathAllocation.keySet()) {
			leftoverPriority.add(tran);
		}
		for (TimedTransition tran: leftoverPriority) {
			if (leftover == 0)
				return;
			Integer weight = pathAllocation.get(tran);
			pathAllocation.put(tran,weight+1);
			leftover--;
		}
	}


}
