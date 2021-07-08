package org.processmining.plugins.etm.experiments.thesis;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;

/**
 * This class can be fed detailed statistics per generation over different runs.
 * It can also be fed timing information (e.g. how many seconds after start did
 * generation X finish). Furthermore, it can be fed concrete trees that were
 * found such that for those generations the 'most average' tree can be written
 * to file.
 * 
 * 
 * @author jbuijs
 * 
 */
public class StatisticsAggregator {

	private int[][] aggregationDetail;

	private String colSep = "\t";

	/**
	 * generation -> dimension/alias -> statistic
	 */
	protected TIntObjectHashMap<HashMap<String, DescriptiveStatistics>> statistics;

	/**
	 * generation -> map sorted by Of value -> session,treeString
	 */
	protected TIntObjectHashMap<TreeMap<Double, Pair<Integer, String>>> concreteTrees;

	public StatisticsAggregator(int[][] aggregationDetail) {
		this.aggregationDetail = aggregationDetail;
		statistics = new TIntObjectHashMap<HashMap<String, DescriptiveStatistics>>();
		concreteTrees = new TIntObjectHashMap<TreeMap<Double, Pair<Integer, String>>>();
	}

	public void addStatistic(int generation, String alias, double value) {
		if (!statistics.contains(generation)) {
			statistics.put(generation, new HashMap<String, DescriptiveStatistics>());
		}
		HashMap<String, DescriptiveStatistics> map = statistics.get(generation);
		if (!map.containsKey(alias)) {
			map.put(alias, new DescriptiveStatistics());
		}

		map.get(alias).addValue(value);
	}

	/**
	 * Returns whether the provided generation should be logged
	 * 
	 * @param generation
	 * @return
	 */
	public boolean shouldLog(int generation) {
		//TODO make more smart, this loop is gone through VERY often
		int lastKnownModulo = 1;

		for (int i = 0; i < aggregationDetail.length; i++) {
			if (generation >= aggregationDetail[i][0]) {
				lastKnownModulo = aggregationDetail[i][1];
			}
			if (generation < aggregationDetail[i][0]) {
				break;//we are above our range, we can stop now
			}
		}
		return (generation % lastKnownModulo) == 0;
	}

	/**
	 * Stores this specific tree for later such that the 'most average' tree can
	 * be written out.
	 * 
	 * @param generation
	 * @param Of
	 * @param session
	 * @param treeString
	 */
	public void addTree(int generation, double Of, int session, String treeString) {
		if (!concreteTrees.contains(generation)) {
			concreteTrees.put(generation, new TreeMap<Double, Pair<Integer, String>>());
		}

		if (generation == 9999) {
			System.out.println("Adding " + Of + " " + session + " " + treeString);
		}

		concreteTrees.get(generation).put(Of, new Pair<Integer, String>(session, treeString));
	}

	public Pair<Integer, String> getMostAverageTree(int generation) {
		String of = OverallFitness.info.getCode();
		of = "size";
		if (!statistics.contains(generation) || !concreteTrees.contains(generation)
				|| !statistics.get(generation).containsKey(of)) {
			return new Pair<Integer, String>(-1, "");
		} else {
			double average = statistics.get(generation).get(of).getMean();

			Entry<Double, Pair<Integer, String>> higher = concreteTrees.get(generation).higherEntry(average);
			Entry<Double, Pair<Integer, String>> lower = concreteTrees.get(generation).lowerEntry(average);

			if (higher == null && lower == null) {
				return new Pair<Integer, String>(-1, "");
			}

			if (higher == null) {
				return lower.getValue();
			}

			if (lower == null) {
				return higher.getValue();
			}

			double highDiff = Math.abs(higher.getKey() - average);

			double lowDiff = Math.abs(lower.getKey() - average);

			if (highDiff < lowDiff) {
				return higher.getValue();
			} else {
				return lower.getValue();
			}
		}
	}

	/**
	 * Writes the aggregated CSV file, and a file with the 'most average' trees
	 * (if given).
	 * 
	 * @param outputDir
	 */
	public void writeFiles(String outputDir, String expCode) {
		//We write two files at once
		try {
			//The aggregated tab-delimited file
			File statsFile = new File(outputDir + "/" + expCode + "_aggregated.dat");
			statsFile.getParentFile().mkdirs();
			statsFile.createNewFile();
			statsFile.setWritable(true);
			statsFile.setReadable(true);

			FileOutputStream fosStats = new FileOutputStream(statsFile);
			PrintWriter outStats = new PrintWriter(fosStats);
			outStats.flush();

			//And the file with the 'most average' trees
			File treeFile = new File(outputDir + "/" + expCode + "_mostAverageTrees.txt");
			treeFile.getParentFile().mkdirs();
			treeFile.createNewFile();
			treeFile.setWritable(true);
			treeFile.setReadable(true);

			FileOutputStream fosAvgTree = new FileOutputStream(treeFile);
			PrintWriter outAvgTree = new PrintWriter(fosAvgTree);
			outAvgTree.flush();

			String headerString = "Generation ";
			String[] dims = new String[statistics.get(1).keySet().size()];
			statistics.get(1).keySet().toArray(dims);
			for (String dim : dims) {
				headerString += colSep + dim + "_MIN" + colSep + dim + "_AVG" + colSep + dim + "_MAX" + colSep + dim
						+ "_N";
			}

			outStats.println(headerString);

			int[] generations = new int[statistics.keySet().size()];
			statistics.keys(generations);
			Arrays.sort(generations);

			for (int generation : generations) {
				//Stats
				String statLine = "" + generation;

				for (String dim : dims) {
					DescriptiveStatistics dimStat = statistics.get(generation).get(dim);
					statLine += colSep + dimStat.getMin() + colSep + dimStat.getMean() + colSep + dimStat.getMax()
							+ colSep + dimStat.getN();
				}

				outStats.println(statLine);

				//Average tree
				Pair<Integer, String> tree = getMostAverageTree(generation);
				outAvgTree.println("Generation: " + generation + " Session: " + tree.getFirst());
				outAvgTree.println(tree.getSecond());
			}

			//			System.out.println("Trees for generation 9999:");
			//			for (Entry<Double, Pair<Integer, String>> entry : concreteTrees.get(9999).entrySet()) {
			//				System.out.println(entry.getValue().getFirst() + " \t " + entry.getKey() + "\t"
			//						+ entry.getValue().getSecond());
			//			}

			outStats.flush();
			outStats.close();

			outAvgTree.flush();
			outAvgTree.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}