package org.processmining.plugins.etm.experiments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExperimentSettingsAbstract {
	/**
	 * Directory into which logging files of the execution are to be written
	 */
	public String logDir = "";

	/**
	 * Directory in which the event log files are stored
	 */
	public String logFileDir = "";

	public String date;

	/**
	 * Initializes the thesis experiment settings class with a global directory
	 * where the logging files are written to and a directory where the event
	 * log files are stored.
	 * 
	 * @param logDir
	 *            Directory to write logging files to
	 * @param logFileDir
	 *            Directory which contains the required event log files
	 */
	public ExperimentSettingsAbstract(String logDir, String eventLogFileDir, String date) {
		this.logDir = logDir;
		this.logFileDir = eventLogFileDir;
		this.date = date;
	}

	/**
	 * Utility method to go from an array/varargs to a List of strings
	 * 
	 * @param elements
	 * @return
	 */
	public static List<String> toList(String... elements) {
		return new ArrayList<String>(Arrays.asList(elements));
	}

	/**
	 * Duplicates all items in the provided list nrDuplications times and returns the list with duplications
	 * @param list
	 * @param nrDuplications
	 * @return
	 */
	public static List<String> duplicateList(List<String> list, int nrDuplications) {
		List<String> duplList = new ArrayList<String>();

		for (String element : list) {
			for (int i = 0; i < nrDuplications; i++) {
				duplList.add(element);
			}
		}

		return duplList;
	}

}
