package org.processmining.plugins.etm;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.Random;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.etm.utils.LogUtils;

// FIXME check this class
// FIXME document this class
public class CentralRegistryConfigurable extends CentralRegistry {

	private final CentralRegistry[] registries;

	/**
	 * Map from log to index in the array (of registries)
	 */
	private TObjectIntHashMap<XLog> logIndexes;

	//FIXME store tree fitness better

	public CentralRegistryConfigurable(XEventClassifier eventClassifier, Random rng, XLog... logs) {
		this(null, eventClassifier, rng, logs);
	}

	public CentralRegistryConfigurable(PluginContext context, XEventClassifier eventClassifier, Random rng,
			XLog... logs) {
		//Call the constructor for the 'normal' registry on the merged log
		super(context, LogUtils.mergeLogs(logs), eventClassifier, rng);

		//Now create the log individual registries
		registries = new CentralRegistry[logs.length];
		logIndexes = new TObjectIntHashMap<XLog>();
		for (int i = 0; i < logs.length; i++) {
			//initialize
			registries[i] = new CentralRegistry(logs[i], eventClassifier, rng);
			logIndexes.put(logs[i], i);

			updateRegistryEClist(registries[i], eventClassifier);
		}

		updateLogDerived(true);
	}

	/**
	 * Updates the given registry's list of event classes with all event classes
	 * known by this instance of the registry, but not by the individual one.
	 * 
	 * @param centralRegistry
	 * @param eventClassifier
	 */
	public void updateRegistryEClist(CentralRegistry centralRegistry, XEventClassifier eventClassifier) {
		//Pointer to the event classes list of the individual registry
		XEventClasses logClasses = centralRegistry.eventClasses;

		boolean tryThis = true;
		if (tryThis) {
			XEventClasses allEventClasses = XEventClasses.deriveEventClasses(eventClassifier, this.getLog());

			for (XEventClass clazz : allEventClasses.getClasses()) {
				String clazzId = clazz.getId();
				XEventClass logClazz = logClasses.getByIdentity(clazzId);
				XEventClass allEventClassesClazz = allEventClasses.getByIdentity(clazzId);
				if (logClazz != null) {
					allEventClassesClazz.setSize(logClazz.size());
				} else {
					allEventClassesClazz.setSize(0);
				}
			}
			centralRegistry.eventClasses = allEventClasses;
		} else {
			/*-*/
			//Now we need to make sure that all classes of each log (/merged log) are known in each individual registry... 
			for (XEventClass clazz : eventClasses.getClasses()) {
				if (logClasses.getByIdentity(clazz.getId()) == null) {
					//We found a class that is present in the merged log but not in this individual log

					/*-
					 * The only way to add the event class to the event class list
					 * of the log is to get an event with that class, register it in
					 * the event classes list of the individual log and then set the
					 * total 'size' to 0.
					 */

					//FIXME very time consuming!!!
					/*
					 * alternative is to somehow clone the eventClasses from the
					 * mergedLog and then reset the sizes using the original
					 * eventClasses
					 */
					XEvent clonedEvent = LogUtils.findEventWithClass(getLog(), eventClassifier, clazz);

					logClasses.register(clonedEvent);

					//And then set the size to 0 so it does not really occur...
					logClasses.getClassOf(clonedEvent).setSize(0);
				}
			}/**/
		}
		centralRegistry.eventClasses.harmonizeIndices();
		centralRegistry.updateLogDerived();
	}

	/**
	 * @param forLog
	 * @return
	 */
	public CentralRegistry getRegistry(int forLog) {
		return registries[forLog];
	}

	public CentralRegistry getRegistry(XLog log) {
		if (logIndexes.contains(log))
			return registries[logIndexes.get(log)];
		else
			return null;
	}

	public CentralRegistry[] getRegistries() {
		return registries;
	}

	public int getNrLogs() {
		return registries.length;
	}

	private void updateLogDerived(boolean includingSelf) {
		if (includingSelf) {
			updateLogDerived();
		}

		for (CentralRegistry reg : registries) {
			reg.updateLogDerived();
		}
	}

}
