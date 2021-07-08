package org.processmining.plugins.etm.tests;

import java.util.Random;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;

public class LogUtilsTest {
	
	public static void main(String[] args){
		XLog log = LogCreator.createLog(new String[][] {{"a","A","B"}});
		XEventClassifier classifier = XLogInfoImpl.NAME_CLASSIFIER;
		XLogInfo info = XLogInfoFactory.createLogInfo(log, classifier );
		
		CentralRegistry reg = new CentralRegistry(log, classifier,new Random());
		
		XEventClasses infoClasses = info.getEventClasses();
		XEventClasses regClasses = reg.getEventClasses();
		
		System.out.println("breaky");
	}

}
