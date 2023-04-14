package qut.pm.spm.measures;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import qut.pm.prom.helpers.ConsoleUIPluginContext;
import qut.pm.prom.helpers.HeadlessDefinitelyNotUIPluginContext;
import qut.pm.spm.FiniteStochasticLangGenerator;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.log.LogUtil;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.xes.helpers.Classifier;

public class AlphaPrecisionLogCalibration {

	private static final XEventClassifier CLASSIFIER = Classifier.NAME.getEventClassifier();

	private static class CrudeLogger{
		public void info(String message) {
			System.out.println(message);
		}
	}
	
	private static CrudeLogger LOGGER = new CrudeLogger();
	
	private AlphaPrecisionSubcalculation alphaSubCalc = new AlphaPrecisionSubcalculation();
	
	private static String[] LOG_FILES = new String[] 
			{"BPIC2012","BPIC2013_closed","BPIC2013_incidents","BPIC2013_open",
					"BPIC2018_control","BPIC2018_reference","BPIC2020",
					"rtfmp","sepsis"};
	
	private static double[] TRACE_PROB_LEVELS = new double[] {0.5, 0.2, 0.1, 0.05, 0.02, 0.01};


	private void calibrateLogs(String path) {
		for (String xesLog: LOG_FILES) {
			ProvenancedLog xlog = loadLog( path, xesLog );
			if (xlog == null) {
				LOGGER.info("Could not find " + xesLog + " in path " + path);
			}else {
				calibrateXLog(xlog);
			}
		}
	}

	private void calibrateXLog(ProvenancedLog plog) {
		LOGGER.info("Calibrating " + plog.getLogFilePath());
		TraceFreq logTF = 
				new FiniteStochasticLangGenerator().
					calculateTraceFreqForLog(plog,CLASSIFIER);
		XLogInfoFactory.createLogInfo(plog, CLASSIFIER);
		int maxTraceLength = calculateMaxTraceLength(plog);
		Set<String> activities = calculateActivities(plog,CLASSIFIER);
		double uss = alphaSubCalc.calculateUnrestrictedSystemSupportSize(activities.size(), maxTraceLength);
		double rss = alphaSubCalc.calculateRestrictedSystemSupportSize(logTF, activities, maxTraceLength);
		LOGGER.info("Max trace length:\t\t" + maxTraceLength);
		LOGGER.info("--");
		LOGGER.info("Unrestricted support size:\t" + uss);
		calibrateMinAlpha(uss,logTF.getTraceTotal());
		LOGGER.info("--");
		LOGGER.info("Restricted support size:\t" + rss);
		calibrateMinAlpha(rss,logTF.getTraceTotal());
		LOGGER.info("============================");
	}

	private void calibrateMinAlpha(double support, double traceTotal) {
		LOGGER.info("Trace probability\tMin alpha significance");
		for (double traceProbLevel: TRACE_PROB_LEVELS) {
			double impliedFreq = traceTotal*traceProbLevel;
			double minAlphaSignificance = (1+impliedFreq)/ (support + traceTotal) ; 
			LOGGER.info(traceProbLevel + "\t\t\t"  + minAlphaSignificance);
		}
	}

	private ProvenancedLog loadLog(String path, String xesLog) {
		HeadlessDefinitelyNotUIPluginContext pluginContext = 
				new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(), 
						"alphaprecisioncalibration");
		String inputLogFileName = path + File.separator + xesLog + ".xes";
		LOGGER.info("Loading log from " + inputLogFileName);
		try {
			ProvenancedLog plog = LogUtil.importLog(inputLogFileName,pluginContext);
			return plog;
		}catch (Exception e) {
			return null;			
		}
	}

	public static void main(String[] args) {
		String path = args[0];
		new AlphaPrecisionLogCalibration().calibrateLogs(path);
	}

	private int calculateMaxTraceLength(XLog log) {
		int max = 0;
		for( XTrace trace: log) {
			if (trace.size() > max)
				max = trace.size();
		}
		return max;
	}

	private Set<String> calculateActivities(XLog log, XEventClassifier classifier) {
		XLogInfo logInfo = log.getInfo(classifier);
		XEventClasses ec = logInfo.getEventClasses();
		Set<String> activities = new HashSet<String>();
		for (int i=0; i<ec.size(); i++) {
			activities.add(ec.getByIndex(i).getId());
		}
		return activities;
	}

	
}
