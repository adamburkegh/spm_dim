package org.processmining.plugins.etm.utils;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.procedure.TObjectIntProcedure;

import java.io.File;
import java.util.Collection;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeDiscreteImpl;
import org.deckfour.xes.model.impl.XAttributeMapImpl;
import org.deckfour.xes.model.impl.XLogImpl;

/**
 * A collection of generic XLog utility methods used in the ETM
 * 
 * @author jbuijs
 * 
 */
public class LogUtils {

	/**
	 * Find the first event instance in the provided event log that has the
	 * provided event class using the specified event classifier
	 * 
	 * @param eventlog
	 *            The event log to search
	 * @param eventClassifier
	 *            The event classifier to use
	 * @param clazz
	 *            The XEventClass the event has
	 * @return XEvent the first event instance in the eventlog that has the
	 *         provided clazz
	 */
	public static XEvent findEventWithClass(XLog eventlog, XEventClassifier eventClassifier, XEventClass clazz) {
		for (XTrace trace : eventlog) {
			for (XEvent event : trace) {
				if (eventClassifier.getClassIdentity(event).equals(clazz.getId())) {
					//Found it!
					return event;
				}
			}
		}
		return null;
	}

	/**
	 * Merges the provided XLogs into a new XLog instance. NOTE: only copies the
	 * traces and events, does NOT copy the event log attributes
	 * 
	 * @param logs
	 * @return
	 */
	public static XLog mergeLogs(XLog... logs) {
		//FIXME test and extend (copy each log's attribute into the new one, add common globals, ...)
		XConceptExtension ce = XConceptExtension.instance();

		XLog mergedLog = new XLogImpl(new XAttributeMapImpl());
		ce.assignName(mergedLog, "Merged Event Log");

		for (int i = 0; i < logs.length; i++) {
			XLog log = logs[i];

			//Copy in the traces
			for (XTrace trace : log) {
				XTrace clonedTrace = (XTrace) trace.clone();
				//Keep track of which log it originated
				clonedTrace.getAttributes().put("originalLog", new XAttributeDiscreteImpl("originalLog", i));
				mergedLog.add(trace);
			}
		}

		return mergedLog;
	}

	public static String eventClassesToString(XEventClasses classes) {
		StringBuilder str = new StringBuilder();

		str.append("Index \t Size \t Name \n\r");

		for (XEventClass clazz : classes.getClasses()) {
			str.append(String.format(" %d \t %d \t %s \n\r", clazz.getIndex(), clazz.size(), clazz.getId()));
		}

		return str.toString();
	}

	public static XEventClasses deepCloneXEventClasses(XEventClasses classes, XLog log, XLogInfo logInfo) {
		XEventClasses newClasses = new XEventClasses(classes.getClassifier());

		for (int index = 0; index < classes.size(); index++) {
			XEventClass xClass = classes.getByIndex(index);
			//for(XEventClass xClass : classes.getClasses()){
			addXEventClass(newClasses, xClass, log, logInfo);
		}

		return newClasses;
	}

	public static XEventClass deepCloneXEventClass(XEventClass xClass) {
		return new XEventClass(xClass.getId(), xClass.getIndex());
	}

	public static void addXEventClass(XEventClasses eventClasses, XEventClass currentClass, XLog log, XLogInfo logInfo) {
		//Add the event class to the classes list
		//XEventClass currentClass = logInfo.getEventClasses().getByIndex(i);
		XEvent event = LogUtils.findEventWithClass(log, eventClasses.getClassifier(), currentClass);
		if (event != null) {
			eventClasses.register(event);
			//And set the correct occurrence count
			eventClasses.getByIdentity(currentClass.getId()).setSize(
					logInfo.getEventClasses().getByIdentity(currentClass.getId()).size());
		}
	}

	public static XLog loadFromFile(String path) {
		File logFile = new File(path);

		XUniversalParser parser = new XUniversalParser();
		if (!parser.canParse(logFile)) {
			System.err
					.println("We can not parse the following log file (is the path correct???), skipping this log input: "
							+ path);
		} else {
			try {
				Collection<XLog> parsedLogs = parser.parse(logFile);
				return parsedLogs.iterator().next();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				System.err.println("Error while parsing the following log file, skipping this log input: " + path);
				e.printStackTrace();
			}
		}

		return null;
	}

	/**
	 * WIP
	 * 
	 * @param log
	 * @param classifier
	 * @return
	 */
	public static String logAsLineOfTraces(XLog log, XEventClassifier classifier) {
		XLogInfo info = XLogInfoImpl.create(log, classifier);

		TObjectIntHashMap<String> map = new TObjectIntHashMap<String>();

		for (XTrace trace : log) {
			StringBuilder str = new StringBuilder();
			for (XEvent event : trace) {
				str.append(", " + classifier.getClassIdentity(event));
			}
			map.adjustOrPutValue(str.toString(), 1, 1);
		}

		final StringBuilder out = new StringBuilder();
		map.forEachEntry(new TObjectIntProcedure<String>() {
			public boolean execute(String a, int b) {
				out.append(a + " x" + b);
				return true;
			}
		});

		return out.toString();
	}

}
