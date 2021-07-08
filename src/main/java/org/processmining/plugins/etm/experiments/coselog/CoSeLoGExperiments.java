package org.processmining.plugins.etm.experiments.coselog;

import java.util.ArrayList;
import java.util.List;

import org.processmining.plugins.etm.experiments.ExperimentSettingsAbstract;

public class CoSeLoGExperiments extends ExperimentSettingsAbstract {

	/*-
	public static void main(String[] args) {
		DateFormat dateFormat = new SimpleDateFormat("yy-MM-dd_HH_mm");
		String date = dateFormat.format(new Date(System.currentTimeMillis()));
		String logsDir = "E:\\ETMlocalLogs";
		String eventlogDir = "C:\\Users\\jbuijs\\Documents\\PhD\\Projects\\CoSeLoG\\Mining\\SSC\\WABO4\\WABO1_2013_hoofd.xes.gz";

		CoSeLoGExperiments exp = new CoSeLoGExperiments(logsDir, eventlogDir, date);
		for (String expSettings : exp.getExpSettings_WABO4Mining()) {
			CommandLineInterface cli = new CommandLineInterface(expSettings);
		}
	}/**/

	public CoSeLoGExperiments(String logDir, String logFileDir, String date) {
		super(logDir, logFileDir, date);
	}

	//enableIM=true
	public List<String> getExpSettings_WABO4Mining_Pareto() {
		String logSuffix = "_2013_hoofd_top47Act.xes.gz";
		String[] logs = new String[] { "WABO1" + logSuffix, "WABO2" + logSuffix, "WABO3" + logSuffix,
				"WABO4" + logSuffix, "WABO5" + logSuffix };

		String basicString = "PARETO \""
				+ logDir
				+ "\" log=\"%s\" "
				+ " Fr=10 Pe=5 Sm=1 Gv=.1 maxGen=20 popSize=20 eliteSize=4 limitFTime=1000 randomMutRatio=0.25 "
				+ "maikel=true IMfactoryWeight=.5 "
				+ " expDate=\""
				+ date
				+ "\" expCode=wabo4_mainModelDisc expDesc=\"Pareto application of the ETM on WABO logs of 2013 only top47  main act (with less chance of OR and LOOP).\"";

		List<String> settings = new ArrayList<String>();

		for (String log : logs) {
			settings.add(String.format(basicString, logFileDir + log));
		}

		return settings;
	}

	public List<String> getExpSettings_WABO4Mining_ParetoGreen() {
		String logSuffix = "_2013_hoofd.xes.gz";
		String[] logs = new String[] { "WABO1" + logSuffix, "WABO2" + logSuffix, "WABO3" + logSuffix,
				"WABO4" + logSuffix, "WABO5" + logSuffix };

		String logsString = "";
		for (String log : logs) {
			logsString += " log=\"" + logFileDir + log + "\"";
		}

		String basicString = "PARETO_GREEN \""
				+ logDir
				+ "\" %s "
				+ " Fr=10 Pe=5 Sm=1 Gv=.1 maxGen=20 popSize=20 eliteSize=4 limitFTime=1000 randomMutRatio=0.25 "
				+ "maikel=true IMfactoryWeight=.5 "
				+ " expDate=\""
				+ date
				+ "\" expCode=wabo4_mainModelDisc_configurable expDesc=\"Configurable Pareto application of the ETM on WABO logs of 2013 only main act (with less chance of OR and LOOP).\"";

		List<String> settings = new ArrayList<String>();

		settings.add(String.format(basicString, logsString));

		return settings;
	}

}
