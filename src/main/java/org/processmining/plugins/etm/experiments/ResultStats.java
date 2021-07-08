package org.processmining.plugins.etm.experiments;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.processmining.plugins.etm.experiments.ResultStats.ExperimentResults.SessionResults;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;

/**
 * Class used to collect result statistics while copying the logfiles. When all
 * log files are parsed, this class needs to be written to file such that it can
 * be inspected and used.
 * 
 * @author jbuijs
 * 
 */
class ResultStats {

	private static String newLineSplitChar = "\n";

	private DecimalFormat df = new DecimalFormat("#.######");

	//date_code_node_session

	//Map for experiments, identified by expDate,expCode
	Map<String, Map<String, ExperimentResults>> experimentResults = new HashMap<String, Map<String, ExperimentResults>>();

	public class ExperimentResults {
		public Map<String, Map<String, SessionResults>> sessionResults = new HashMap<String, Map<String, SessionResults>>();
		//Map from 2 char dim code to the respective stats object for mean etc. over runs
		public Map<String, DescriptiveStatistics> dimStats = new HashMap<String, DescriptiveStatistics>();

		public class SessionResults implements Comparable<SessionResults> {
			public String tree = "";
			public Map<String, Double> fitnessValues = new HashMap<String, Double>();

			public int compareTo(SessionResults o) {
				String ofCode = OverallFitness.info.getCode();
				if (!o.fitnessValues.containsKey(ofCode)) {
					if (!this.fitnessValues.containsKey(ofCode)) {
						//If neither has an overall fitness
						return tree.compareTo(o.tree);
					} else {
						//We do have an overall fitness so we are first!!!
						return -1;
					}
				}

				//Sort on overall fitness descending
				return this.fitnessValues.get(ofCode).compareTo(o.fitnessValues.get(ofCode));
			}

			public String toString() {
				String fitnessString = "";
				for (Entry<String, Double> entry : fitnessValues.entrySet()) {
					fitnessString += String.format("%s: %1.4f , ", entry.getKey(), entry.getValue());
				}
				return tree + " ( " + fitnessString + " ) ";
			}
		}

		public SessionResults getSessionResults(String expNode, String expSession) {
			if (!sessionResults.containsKey(expNode)) {
				sessionResults.put(expNode, new HashMap<String, SessionResults>());
			}
			//if(!expResults.trees.get(expNode).containsKey(expSession)){
			sessionResults.get(expNode).put(expSession, new SessionResults());
			//}

			return sessionResults.get(expNode).get(expSession);
		}
	}

	/*-
	public ResultStats() {

	}/**/

	public void parse(String statsString, String settingsString) {
		try {
			//First parse the settings string to get to know the experiment qualifiers
			String expCode = "UNKNOWN";
			String expDate = "UNKNOWN";
			String expNode = "UNKNOWN";
			String expSession = "UNKNOWN";

			String[] settingLines = settingsString.split(newLineSplitChar);
			String[] args = settingLines[0].split(": \t")[1].split(", ");

			for (String arg : args) {
				String[] values = arg.split("=");
				if (values.length >= 2) {
					String key = values[0];
					String value = values[1];

					//Extract what we need from the settings file
					if (key.equalsIgnoreCase("expCode")) {
						expCode = value;
					} else if (key.equalsIgnoreCase("expDate")) {
						expDate = value;
					} else if (key.equalsIgnoreCase("expNode")) {
						expNode = value;
					} else if (key.equalsIgnoreCase("expSession")) {
						expSession = value.replaceAll("]", ""); //this is the last argument and we don't want the ] tagging along...
					}
				}
			}
			System.out.println("expCode: " + expCode);

			//Get or create the results object for this experiment
			if (!experimentResults.containsKey(expDate)) {
				experimentResults.put(expDate, new HashMap<String, ResultStats.ExperimentResults>());
			}
			if (!experimentResults.get(expDate).containsKey(expCode)) {
				experimentResults.get(expDate).put(expCode, new ResultStats.ExperimentResults());
			}
			ExperimentResults expResults = experimentResults.get(expDate).get(expCode);

			//Now parse the actual experiment statistics
			String[] lines = statsString.split(newLineSplitChar);
			System.out.println(lines.length + " lines");

			//First line: sep=;

			/*
			 * Now parse the header, since we do not know which dimensions there
			 * are (and their order differs)
			 */
			String header = lines[1];
			String[] headers = header.split(";");
			//Columns: Timestamp; Generation; Fittest; Average; Deviation; <DIMENSIONS> ; bestCandidate
			String[] dims = Arrays.copyOfRange(headers, 5, headers.length - 1);

			/*
			 * We are interested in the last line!
			 */
			String lastLine = lines[lines.length - 1];
			String[] cols = lastLine.split(";");
			String[] dimValues = Arrays.copyOfRange(cols, 5, cols.length - 1);
			SessionResults sessionResults = expResults.getSessionResults(expNode, expSession);
			//Now process the dimension values
			for (int d = 0; d < dims.length; d++) {
				String dimCode = dims[d];
				String dimValue = dimValues[d];
				if (!expResults.dimStats.containsKey(dimCode)) {
					expResults.dimStats.put(dimCode, new DescriptiveStatistics());
				}
				Double dimValueDouble = Double.parseDouble(dimValue);
				expResults.dimStats.get(dimCode).addValue(dimValueDouble);
				sessionResults.fitnessValues.put(dimCode, dimValueDouble);
			}
			/*
			 * And store the node+session + tree + dimValues to print out later
			 */
			sessionResults.tree = cols[cols.length - 1];

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes the gathered statistics to a file in the given path
	 * 
	 * @param path
	 */
	public void writeToFile(String path) {
		for (String expDate : experimentResults.keySet()) {
			Map<String, ExperimentResults> dateExps = experimentResults.get(expDate);
			for (String expCode : dateExps.keySet()) {
				ExperimentResults expResults = dateExps.get(expCode);

				//We write one file per experiment
				StringBuffer writer = new StringBuffer();

				for (String dimKey : expResults.dimStats.keySet()) {
					DescriptiveStatistics dimStat = expResults.dimStats.get(dimKey);
					writer.append(String.format("%s: %1.4f (std. dev. %1.4f, N=%d, max: %1.4f, min: %1.4f) \r\n",
							dimKey, dimStat.getMean(), dimStat.getStandardDeviation(), dimStat.getN(),
							dimStat.getMax(), dimStat.getMin()));
				}

				writer.append("\r\n");

				//Now for all the session results, which we want ordered!
				TreeMap<Double, Set<String>> orderedSessionResults = new TreeMap<Double, Set<String>>();
				for (String node : expResults.sessionResults.keySet()) {
					for (String session : expResults.sessionResults.get(node).keySet()) {
						SessionResults sessionResult = expResults.sessionResults.get(node).get(session);
						Double ofValue = sessionResult.fitnessValues.get(OverallFitness.info.getCode());

						//Now build the session result string and store it sorted by Of
						if (!orderedSessionResults.containsKey(ofValue)) {
							orderedSessionResults.put(ofValue, new HashSet<String>());
						}
						orderedSessionResults.get(ofValue).add(
								String.format("%s @%s in session %s", sessionResult.toString(), node, session));
					}
				}

				//Now add the individual trees ordered from high fitness to loooow
				Iterator<Double> keyIt = orderedSessionResults.navigableKeySet().iterator();
				while (keyIt.hasNext()) {
					Double of = keyIt.next();
					Set<String> results = orderedSessionResults.get(of);

					for (String result : results) {
						writer.append(String.format("%1.4f: %s \r\n", of, result));
					}
				}

				writeToFile(path + "/000ResultStats_" + expDate + "_" + expCode + ".txt", writer.toString(), false);
			}
		}
	}

	public static void writeToFile(String path, String message, boolean syso) {
		if (syso) {
			System.out.println(message);
		}

		//And write to file
		try {
			File outputFile = new File(path);
			FileWriter fileWriter = new FileWriter(outputFile);
			outputFile.createNewFile();
			fileWriter.write(message);
			fileWriter.flush();
			fileWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}