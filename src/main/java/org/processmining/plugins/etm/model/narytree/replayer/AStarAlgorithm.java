package org.processmining.plugins.etm.model.narytree.replayer;

import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectShortHashMap;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;

import nl.tue.astar.AStarThread;
import nl.tue.astar.Trace;
import nl.tue.astar.util.LinearTrace;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.model.XLog;

public class AStarAlgorithm {

	protected final TObjectIntMap<Trace> converted;
	protected final XEventClasses classes;
	protected final TObjectShortMap<XEventClass> act2index;
	protected final XEventClass[] index2act;
	protected final int[] act2cost;
	//	private final int[] act2maxOccInTrace;
	protected final XLog log;
	protected final int lengthLongestTrace;
	protected final int[] maxOccInTrace;

	public AStarAlgorithm() {
		this.log = null;
		this.classes = new XEventClasses(new XEventNameClassifier());
		act2index = new TObjectShortHashMap<XEventClass>();
		index2act = new XEventClass[0];
		act2cost = new int[0];
		//		act2maxOccInTrace = new int[0];
		converted = new TObjectIntHashMap<Trace>(1);
		converted.put(new LinearTrace("empty trace", 0), 1);
		lengthLongestTrace = 0;
		maxOccInTrace = new int[0];
	}

	public AStarAlgorithm(XEventClasses classes) {
		this.log = null;
		this.classes = classes;

		act2index = new TObjectShortHashMap<XEventClass>((short) classes.size(), 0.5f, (short) -1);
		index2act = new XEventClass[classes.size()];
		act2cost = new int[classes.size()];
		//		act2maxOccInTrace = new int[classes.size()];
		short i = 0;
		Collection<XEventClass> sorted = new TreeSet<XEventClass>(classes.getClasses());
		for (XEventClass c : sorted) {
			index2act[i] = c;
			act2cost[i] = 0;
			act2index.put(c, i++);
		}

		converted = new TObjectIntHashMap<Trace>(1);
		converted.put(new LinearTrace("empty trace", 0), 1);
		lengthLongestTrace = 0;
		maxOccInTrace = new int[classes.size()];
	}

	public AStarAlgorithm(XLog log, XEventClasses classes, Map<XEventClass, Integer> activity2Cost) {
		this.log = log;
		this.classes = classes;

		act2index = new TObjectShortHashMap<XEventClass>((short) classes.size(), 0.5f, (short) -1);
		index2act = new XEventClass[classes.size()];
		act2cost = new int[classes.size()];
		maxOccInTrace = new int[classes.size()];
		//		act2maxOccInTrace = new int[classes.size()];
		short i = 0;
		Collection<XEventClass> sorted = new TreeSet<XEventClass>(classes.getClasses());
		for (XEventClass c : sorted) {
			index2act[i] = c;
			Integer cost = activity2Cost.get(c);
			act2cost[i] = (cost == null ? 0 : cost);
			act2index.put(c, i++);
		}

		int max = 0;
		converted = new TObjectIntHashMap<Trace>(log.size());
		for (int j = 0; j < log.size(); j++) {
			Trace list = getListEventClass(log, j);
			if (list.getSize() > max) {
				max = list.getSize();
			}
			converted.adjustOrPutValue(list, 1, 1);
		}
		lengthLongestTrace = max;
	}

	public AStarAlgorithm(AStarAlgorithm originalAStar, TObjectIntMap<Trace> _converted,  int _lengthLongestTrace, int[] _maxOccInTrace) {
		this.log = originalAStar.log;
		this.classes = originalAStar.classes;

		act2index = originalAStar.act2index;
		index2act = originalAStar.index2act;
		act2cost = originalAStar.act2cost;
		
		maxOccInTrace = _maxOccInTrace;
		lengthLongestTrace = _lengthLongestTrace;
		converted = _converted;
	}
	
	public int getMaxOccurranceInTrace(int activity) {
		if (maxOccInTrace == null || maxOccInTrace.length <= activity) {
			return 0;
		} else
			return maxOccInTrace[activity];
	}

	/**
	 * get list of event class. Record the indexes of non-mapped event classes.
	 * 
	 * @param trace
	 * @param classes
	 * @param mapEvClass2Trans
	 * @param listMoveOnLog
	 * @return
	 */
	public Trace getListEventClass(XLog log, int trace) {
		int s = log.get(trace).size();
		String label = XConceptExtension.instance().extractName(log.get(trace));
		if (label == null || label.isEmpty()) {
			label = "trace_" + trace;
		}
		int[] occ = new int[classes.size()];
		TIntList activities = new TIntArrayList();
		for (int i = 0; i < s; i++) {
			int act = getActivityOf(trace, i);
			if (act != AStarThread.NOMOVE) {
				activities.add(act);
				occ[act]++;
			}
		}
		for (int i = 0; i < occ.length; i++) {
			maxOccInTrace[i] = Math.max(maxOccInTrace[i], occ[i]);
		}
		return new LinearTrace(label, activities);
		//		{
		//			public boolean equals(Object o) {
		//				return this == o;
		//			}
		//		};
	}

	public XEventClasses getClasses() {
		return classes;
	}

	public short getIndexOf(XEventClass c) {
		return act2index.get(c);
	}

	private short getActivityOf(int trace, int event) {
		XEventClass cls = classes.getClassOf(log.get(trace).get(event));
		return act2index.get(cls);
	}

	public XEventClass getEventClass(short act) {
		return index2act[act];
	}

	public int getLogMoveCost(int i) {
		return act2cost[i];
	}

	public int getTraceFreq(Trace trace) {
		return converted.get(trace);
	}

	public Iterator<Trace> traceIterator() {
		return new Iterator<Trace>() {
			private TObjectIntIterator<Trace> it = converted.iterator();

			public boolean hasNext() {
				return it.hasNext();
			}

			public Trace next() {
				it.advance();
				return it.key();
			}

			public void remove() {
				throw new UnsupportedOperationException("Cannot remove trace");
			}

		};
	}

	public int getDifferentTraceCount() {
		return converted.size();
	}

	public TObjectIntMap<Trace> getConvertedLog() {
		return converted;
	}

	public int getLengthLongestTrace() {
		return lengthLongestTrace;
	}

}
