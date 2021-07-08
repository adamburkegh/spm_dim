package org.processmining.plugins.etm.model.narytree.test;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectShortHashMap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import nl.tue.astar.AStarException;
import nl.tue.astar.AStarThread;
import nl.tue.astar.AStarThread.ASynchronousMoveSorting;
import nl.tue.astar.AStarThread.Canceller;
import nl.tue.astar.AStarThread.QueueingModel;
import nl.tue.astar.AStarThread.Type;
import nl.tue.astar.Delegate;
import nl.tue.astar.Tail;
import nl.tue.astar.Trace;
import nl.tue.astar.impl.AbstractAStarThread;
import nl.tue.astar.impl.DotGraphAStarObserver;
import nl.tue.astar.impl.FSMGraphAStarObserver;
import nl.tue.astar.impl.State;
import nl.tue.astar.impl.memefficient.MemoryEfficientAStarAlgorithm;
import nl.tue.astar.impl.memefficient.StorageAwareDelegate;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.deckfour.xes.model.impl.XEventImpl;
import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPDelegate;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeHead;
import org.processmining.plugins.etm.model.narytree.replayer.StubbornNAryTreeAStarThread;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.narytree.replayer.hybridilp.NAryTreeHybridILPDelegate;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.Simulator;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

public class Test {

	private static final int MODELCOST = 2;
	private static final Integer LOGCOST = 2;
	private static long queuedStateCount;
	private static long traversedArcCount;
	private static long totalRawCost;
	private static int eventCount;
	private static long emptyTraceRawCost;
	private static long totalPenaltyCost;
	private static boolean reliable;
	private static long computedEstimates;
	private static long traceTime;
	private static int reliableTraceCount;

	private static class Stat {

		private final long[] logSize = new long[] { 0, Integer.MAX_VALUE, -1 };
		private final long[] eventCount = new long[] { 0, Integer.MAX_VALUE, -1 };
		private String name;
		private final double[] time = new double[] { 0, Integer.MAX_VALUE, -1 };
		private final double[] fitness = new double[] { 0, Integer.MAX_VALUE, -1 };
		private final double[] timePerTrace = new double[] { 0, Integer.MAX_VALUE, -1 };
		private final long[] treeSize = new long[] { 0, Integer.MAX_VALUE, -1 };
		private final long[] unique = new long[] { 0, Integer.MAX_VALUE, -1 };
		private final long[] queued = new long[] { 0, Integer.MAX_VALUE, -1 };
		private final long[] arcs = new long[] { 0, Integer.MAX_VALUE, -1 };
		private final long[] estimates = new long[] { 0, Integer.MAX_VALUE, -1 };
		private final double[] statePerSec = new double[] { 0, Integer.MAX_VALUE, -1 };
		private final long[] rawCost = new long[] { 0, Integer.MAX_VALUE, -1 };
		private final long[] penaltyCost = new long[] { 0, Integer.MAX_VALUE, -1 };
		private final long[] reliableCount = new long[] { 0, Integer.MAX_VALUE, -1 };
		private double count = 0;
		private boolean reliable = true;

		public Stat(String name, double fitness, double time, double reliableTraceTime, int reliableTraceCount,
				long unique, long queued, long arcs, long estimates, double statePerSec, long rawCost,
				long penaltyCost, int treeSize, int logSize, int eventCount, boolean reliable) {
			this.name = name;
			this.fitness[0] = fitness;
			this.time[0] = time;
			this.unique[0] = unique;
			this.queued[0] = queued;
			this.arcs[0] = arcs;
			this.estimates[0] = estimates;
			this.statePerSec[0] = statePerSec;
			this.rawCost[0] = rawCost;
			this.penaltyCost[0] = penaltyCost;
			this.reliable = reliable;
			this.count = 1;
			this.treeSize[0] = treeSize;
			this.logSize[0] = logSize;
			this.eventCount[0] = eventCount;
			this.reliableCount[0] = reliableTraceCount;
			this.timePerTrace[0] = reliableTraceTime;
		}

		public Stat() {
		}

		public void update(Stat stat) {
			this.reliable &= stat.reliable;

			this.name = stat.name;
			this.time[0] += stat.time[0];
			this.fitness[0] += stat.fitness[0];
			this.unique[0] += stat.unique[0];
			this.queued[0] += stat.queued[0];
			this.arcs[0] += stat.arcs[0];
			this.estimates[0] += stat.estimates[0];
			this.statePerSec[0] += stat.statePerSec[0];
			this.rawCost[0] += stat.rawCost[0];
			this.penaltyCost[0] += stat.penaltyCost[0];
			this.treeSize[0] += stat.treeSize[0];
			this.logSize[0] += stat.logSize[0];
			this.eventCount[0] += stat.eventCount[0];
			this.timePerTrace[0] += stat.timePerTrace[0];
			this.reliableCount[0] += stat.reliableCount[0];
			this.count += 1;

			this.time[1] = Math.min(this.time[1], stat.time[0]);
			this.fitness[1] = Math.min(this.fitness[1], stat.fitness[0]);
			this.unique[1] = Math.min(this.unique[1], stat.unique[0]);
			this.queued[1] = Math.min(this.queued[1], stat.queued[0]);
			this.arcs[1] = Math.min(this.arcs[1], stat.arcs[0]);
			this.estimates[1] = Math.min(this.estimates[1], stat.estimates[0]);
			this.statePerSec[1] = Math.min(this.statePerSec[1], stat.statePerSec[0]);
			this.rawCost[1] = Math.min(this.rawCost[1], stat.rawCost[0]);
			this.penaltyCost[1] = Math.min(this.penaltyCost[1], stat.penaltyCost[0]);
			this.treeSize[1] = Math.min(this.treeSize[1], stat.treeSize[0]);
			this.logSize[1] = Math.min(this.logSize[1], stat.logSize[0]);
			this.reliableCount[1] = Math.min(this.reliableCount[1], stat.reliableCount[0]);
			this.eventCount[1] = Math.min(this.eventCount[1], stat.eventCount[0]);
			this.timePerTrace[1] = Math.min(this.timePerTrace[1], stat.timePerTrace[0]);

			this.time[2] = Math.max(this.time[2], stat.time[0]);
			this.fitness[2] = Math.max(this.fitness[2], stat.fitness[0]);
			this.unique[2] = Math.max(this.unique[2], stat.unique[0]);
			this.queued[2] = Math.max(this.queued[2], stat.queued[0]);
			this.arcs[2] = Math.max(this.arcs[2], stat.arcs[0]);
			this.estimates[2] = Math.max(this.estimates[2], stat.estimates[0]);
			this.statePerSec[2] = Math.max(this.statePerSec[2], stat.statePerSec[0]);
			this.rawCost[2] = Math.max(this.rawCost[2], stat.rawCost[0]);
			this.penaltyCost[2] = Math.max(this.penaltyCost[2], stat.penaltyCost[0]);
			this.treeSize[2] = Math.max(this.treeSize[2], stat.treeSize[0]);
			this.logSize[2] = Math.max(this.logSize[2], stat.logSize[0]);
			this.eventCount[2] = Math.max(this.eventCount[2], stat.eventCount[0]);
			this.timePerTrace[2] = Math.max(this.timePerTrace[2], stat.timePerTrace[0]);
			this.reliableCount[2] += Math.max(this.reliableCount[2], stat.reliableCount[0]);

		}

		public void print() {
			System.out.println("___________________________________________________________________________");
			System.out.println();
			System.out.println(name);
			System.out.println("Total time (sec);   " + time[0]);
			System.out.println("Time per     trace; " + timePerTrace[0]);
			System.out.println("Reliable traces   ; " + (1.0 * reliableCount[0]) / logSize[0]);
			System.out.println("Fitness         ;   " + fitness[0]);
			System.out.println("Raw cost;           " + rawCost[0]);
			System.err.println("Penalty cost;       " + penaltyCost[0]);
			System.out.println("Unique states;      " + unique[0]);
			System.out.println("Queued states;      " + queued[0]);
			System.out.println("Traversed arcs;     " + arcs[0]);
			System.out.println("Estimates arcs;     " + estimates[0]);
			System.out.println("States/sec;         " + statePerSec[0]);

		}

		public static boolean printAndCheckConsistent(String[][] traces, Stat... stats) {
			PrintStream out = System.out;
			out.print("___________________");
			for (int i = stats.length; i-- > 0;) {
				if (stats[i] != null)
					out.print("________________________________________________");
			}
			out.println();
			out.println();
			boolean ok = printAndCheckConsistent(out, traces, stats);
			out.print("___________________");
			for (int i = stats.length; i-- > 0;) {
				if (stats[i] != null)
					out.print("________________________________________________");
			}
			out.println();
			return ok;
		}

		public static boolean printAndCheckConsistent(PrintStream out, String[][] traces, Stat... stats) {

			out.print("                        ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%-48s", ";" + stats[i].name + " (" + ((int) stats[i].count)
							+ " , reliable: " + stats[i].reliable + ");;"));
				}
			}
			out.println();

			out.print(";                   ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%18s", "avg;"));
					out.print(String.format("%15s", "min;"));
					out.print(String.format("%15s", "max;"));
				}
			}
			out.println();

			out.print("Total time (sec);  ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].time[0] / stats[i].count));
					out.print(String.format("%,15.3f;", (stats[i].time[1] < Integer.MAX_VALUE ? stats[i].time[1] : 0)));
					out.print(String.format("%,15.3f;", (stats[i].time[2] > 0 ? stats[i].time[2] : 0)));
				}
			}
			out.println();

			out.print("Time per trace; ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].timePerTrace[0] / stats[i].count));
					out.print(String.format("%,15.3f;",
							(stats[i].timePerTrace[1] < Integer.MAX_VALUE ? stats[i].timePerTrace[1] : 0)));
					out.print(String.format("%,15.3f;", (stats[i].timePerTrace[2] > 0 ? stats[i].timePerTrace[2] : 0)));
				}
			}
			out.println();

			out.print("Reliable Traces; ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", ((1.0 * stats[i].reliableCount[0]) / stats[i].logSize[0])
							/ stats[i].count));
					out.print(String.format("%,15.3f;",
							(stats[i].timePerTrace[1] < Integer.MAX_VALUE ? stats[i].timePerTrace[1] : 0)));
					out.print(String.format("%,15.3f;", (stats[i].timePerTrace[2] > 0 ? stats[i].timePerTrace[2] : 0)));
				}
			}
			out.println();

			out.print("Fitness         ;  ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].fitness[0] / stats[i].count));
					out.print(String.format("%,15.3f;", (stats[i].fitness[1] < Integer.MAX_VALUE ? stats[i].fitness[1]
							: 0)));
					out.print(String.format("%,15.3f;", (stats[i].fitness[2] > 0 ? stats[i].fitness[2] : 0)));
				}
			}
			out.println();

			out.print("Tree size;         ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].treeSize[0] / stats[i].count));
					out.print(String.format("%,15d;", (stats[i].treeSize[1] < Integer.MAX_VALUE ? stats[i].treeSize[1]
							: 0)));
					out.print(String.format("%,15d;", (stats[i].treeSize[2] > 0 ? stats[i].treeSize[2] : 0)));
				}
			}
			out.println();

			out.print("Log size;          ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].logSize[0] / stats[i].count));
					out.print(String.format("%,15d;", (stats[i].logSize[1] < Integer.MAX_VALUE ? stats[i].logSize[1]
							: 0)));
					out.print(String.format("%,15d;", (stats[i].logSize[2] > 0 ? stats[i].logSize[2] : 0)));
				}
			}
			out.println();

			out.print("Event count;      ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].eventCount[0] / stats[i].count));
					out.print(String.format("%,15d;",
							(stats[i].eventCount[1] < Integer.MAX_VALUE ? stats[i].eventCount[1] : 0)));
					out.print(String.format("%,15d;", (stats[i].eventCount[2] > 0 ? stats[i].eventCount[2] : 0)));
				}
			}
			out.println();

			out.print("Raw cost;          ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].rawCost[0] / stats[i].count));
					out.print(String.format("%,15d;", (stats[i].rawCost[1] < Integer.MAX_VALUE ? stats[i].rawCost[1]
							: 0)));
					out.print(String.format("%,15d;", (stats[i].rawCost[2] > 0 ? stats[i].rawCost[2] : 0)));
				}
			}
			out.println();

			out.print("Penalty cost;      ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].penaltyCost[0] / stats[i].count));
					out.print(String.format("%,15d;",
							(stats[i].penaltyCost[1] < Integer.MAX_VALUE ? stats[i].penaltyCost[1] : 0)));
					out.print(String.format("%,15d;", (stats[i].penaltyCost[2] > 0 ? stats[i].penaltyCost[2] : 0)));
				}
			}
			out.println();

			out.print("Unique states;     ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].unique[0] / stats[i].count));
					out.print(String
							.format("%,15d;", (stats[i].unique[1] < Integer.MAX_VALUE ? stats[i].unique[1] : 0)));
					out.print(String.format("%,15d;", (stats[i].unique[2] > 0 ? stats[i].unique[2] : 0)));
				}
			}
			out.println();

			out.print("Queued states;     ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].queued[0] / stats[i].count));
					out.print(String
							.format("%,15d;", (stats[i].queued[1] < Integer.MAX_VALUE ? stats[i].queued[1] : 0)));
					out.print(String.format("%,15d;", (stats[i].queued[2] > 0 ? stats[i].queued[2] : 0)));
				}
			}
			out.println();

			out.print("Traversed arcs;    ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].arcs[0] / stats[i].count));
					out.print(String.format("%,15d;", (stats[i].arcs[1] < Integer.MAX_VALUE ? stats[i].arcs[1] : 0)));
					out.print(String.format("%,15d;", (stats[i].arcs[2] > 0 ? stats[i].arcs[2] : 0)));
				}
			}
			out.println();

			out.print("Computed Estimates;");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].estimates[0] / stats[i].count));
					out.print(String.format("%,15d;",
							(stats[i].estimates[1] < Integer.MAX_VALUE ? stats[i].estimates[1] : 0)));
					out.print(String.format("%,15d;", (stats[i].estimates[2] > 0 ? stats[i].estimates[2] : 0)));
				}
			}
			out.println();

			out.print("States/sec;        ");
			for (int i = 0; i < stats.length; i++) {
				if (stats[i] != null) {
					out.print(String.format("%,18.3f;", stats[i].statePerSec[0] / stats[i].count));
					out.print(String.format("%,15.3f;",
							(stats[i].statePerSec[1] < Integer.MAX_VALUE ? stats[i].statePerSec[1] : 0)));
					out.print(String.format("%,15.3f;", (stats[i].statePerSec[2] > 0 ? stats[i].statePerSec[2] : 0)));
				}
			}
			out.println();
			out.flush();

			long p = -1;
			boolean ok = true;
			for (int i = stats.length; i-- > 0;) {
				if (stats[i] != null) {
					ok &= !stats[i].reliable || (stats[i].penaltyCost[0] == p || (p == -1));
					//ok &= (stats[i].penaltyCost[0] == p || (p == -1));
					if (stats[i].reliable) {
						p = stats[i].penaltyCost[0];
					}
				}
			}
			if (!ok) {
				System.err.println("INCONSISTENT");
				for (String[] trace : traces) {
					System.out.print("{");
					for (int i = 0; i < trace.length - 1; i++) {
						System.out.print("\"" + trace[i] + "\",");
					}
					System.out.print("\"" + (trace.length > 0 ? trace[trace.length - 1] : "") + "\"},");
				}
				System.out.println();

				throw new RuntimeException("Inconsistent");
			}
			out.println();
			return ok;
		}
	}

	/**
	 * @param args
	 * @throws AStarException
	 * @throws FileNotFoundException
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		long startTime = System.nanoTime();
		String[] activities = new String[] { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N" };

		TObjectShortMap<String> map = new TObjectShortHashMap<String>();
		// initialize a tree with all node costs 5 for leafs and 0 otherwise.
		for (short i = 0; i < activities.length; i++) {
			map.put(activities[i], i);
		}

		//mergeStatFiles("c:\\temp\\NBWIN949\\", "c:\\temp\\nbwin949.txt", map);
		//mergeStatFiles("c:\\temp\\stats\\ngrid 20-25", "c:\\temp\\stats\\ngrid 20-25.txt", map);
		//mergeStatFiles("c:\\temp\\stats\\heuristics 1t", "c:\\temp\\stats\\heuristics-1t.txt", map);

		if (args == null || args.length == 0) {
			args = new String[] { "D:\\temp\\NBWIN949\\", "5897630", "34121", "15", "10", "0" };
		}

		final String folder;
		final long logSeed, treeSeed;
		final int startTree, startNoise, startExp;
		try {
			if (args.length > 0) {
				folder = args[0];
			} else {
				folder = "d:\\temp\\";
			}
			if (args.length > 2) {
				logSeed = Long.parseLong(args[1]);
				treeSeed = Long.parseLong(args[2]);
			} else {
				logSeed = 5897630;
				treeSeed = 34121;
			}
			if (args.length > 4) {
				startTree = Integer.parseInt(args[3]);
				startNoise = Integer.parseInt(args[4]);
				startExp = Integer.parseInt(args[5]);
			} else {
				startTree = 6;
				startNoise = 0;
				startExp = 0;
			}
		} catch (Exception e) {
			System.out.println("Use 4 arguments:");
			System.out.println("1) folder for log files");
			System.out.println("2) log seed (5897630)");
			System.out.println("3) tree seed (34121)");
			System.out.println("4) start at tree size (10)");
			System.out.println("5) start at noise level [0..20] (0)");
			System.out.println("6) start at experiment [0..200] (0)");
			System.exit(0);
			return;
		}

		System.out.println(map);

		final Random logRandom = new Random(logSeed);
		final Random treeRandom = new Random(treeSeed);
		final String date = new SimpleDateFormat("yy-MM-dd-HH.mm").format(new Date());

		String computername = InetAddress.getLocalHost().getHostName();

		//TODO: For logging to disk:
		final PrintStream avgFile = new PrintStream(
				new File(folder + "stats " + computername + " " + date + " avg.txt"));

		//boolean treesOnly = true;

		System.out.println("Skipping first part");
		final int numTraces = 200;
		int nExperiments = 50;
		// skip first part
		//		for (int size = 10; size <= startTree; size += 10) {
		//			System.out.print(".");
		//			for (int n = 0; size == startTree ? n < startNoise : n <= 20; n++) {
		//				if (n == 10 && size == 60) {
		//					nExperiments = 241;
		//				}
		//				final double noise = n / 20.0;
		//				for (int exp = 0; size == startTree && n == startNoise ? exp < startExp : exp <= nExperiments; exp++) {
		//					NAryTree tree = getTree(map, treeRandom, size);
		//					Simulator sim = new Simulator(tree, 0, logRandom);
		//					for (int i = 0; i < numTraces; i++) {
		//						sim.getRandomTrace(activities, noise);
		//					}
		//				}
		//				if (n == 10 && size == 60) {
		//					nExperiments = 200;
		//				}
		//			}
		//		}
		System.out.println();
		System.out.println("Continuing with actual work");
		ProbProcessArrayTree lastTree = null;
		try {
			//			int size = 60;
			//			int n = 5;
			for (int size = startTree; size <= startTree; size += 1) {
				for (int n = size == startTree ? startNoise : 10; n <= startNoise; n += 10) {

					final Stat[] stats = new Stat[1];

					final double noise = n / 20.0;

					final Stat[] average = new Stat[stats.length];
					for (int i = 0; i < stats.length; i++) {
						average[i] = new Stat();
					}
					final Canceller c = new Canceller() {

						public boolean isCancelled() {
							return false;
						}

					};

					PrintStream statFile = null;

					//TODO: ENABLE FOR LOGGING TO DISK
					statFile = new PrintStream(new File(folder + "stats " + computername + " " + date + " "
							+ String.format("%,3.2f %3d", noise, size) + ".txt"));

					try {
						int exp = 0;
						if (n == startNoise && size == startTree && exp == 0) {
							exp = startExp;
						}
						boolean ok = true;
						do {

							final ProbProcessArrayTree tree = getTree(map, treeRandom, size);
							if (tree.equals(lastTree)) {
								return;
							} else {
								lastTree = tree;
							}

							Simulator sim = new Simulator(tree, 0, logRandom);

							String[][] traces = getTraces(sim, numTraces, activities, noise);

							XLog log;
							XFactoryRegistry.instance().setCurrentDefault(new XFactoryNaiveImpl());
							//log = LogCreator.createInterleavedLog(500, A, A, B, C, D, E, F);
							log = LogCreator.createLog(traces);

							//				XLogInfo info = XLogInfoFactory.createLogInfo(log, new XEventAttributeClassifier("Name",
							//						XConceptExtension.KEY_NAME));
							final XEventClasses classes = new XEventClasses(new XEventNameClassifier());
							Map<XEventClass, Integer> activity2Cost = new HashMap<XEventClass, Integer>();
							for (int i = 0; i < activities.length; i++) {
								XEventImpl e = new XEventImpl();
								XAttribute a = new XAttributeLiteralImpl(XConceptExtension.KEY_NAME, activities[i]);
								e.getAttributes().put(XConceptExtension.KEY_NAME, a);
								classes.register(e);
								activity2Cost.put(new XEventClass(activities[i], i), LOGCOST);
							}
							final AStarAlgorithm algorithm = new AStarAlgorithm(log, classes, activity2Cost);

							int[] node2Cost = new int[tree.size()];
							for (int i = 0; i < tree.size(); i++) {
								if (tree.isLeaf(i) && tree.getType(i) != ProbProcessArrayTree.TAU) {
									node2Cost[i] = MODELCOST;
								}
							}
							System.out.println(TreeUtils.toString(tree, classes));
							System.out.println("Number of traces:" + log.size());
							System.out.println("Number of unique traces:" + algorithm.getDifferentTraceCount());
							System.out.println("Longest trace:" + algorithm.getLengthLongestTrace());
							System.out.println("Number of nodes: " + tree.size());

							//TODO: threads
							int threads = 4;//Runtime.getRuntime().availableProcessors();
							final AbstractNAryTreeDelegate<? extends Tail> delegates[] = new AbstractNAryTreeDelegate[stats.length];
							final boolean[] isStorageAware = new boolean[stats.length];
							final boolean[] isCached = new boolean[stats.length];
							final boolean[] isStubborn = new boolean[stats.length];
							final double[] epsilon = new double[stats.length];
							final String[] algorithmNames = new String[stats.length];

							int x = -1;
//							delegates[++x] = new NAryTreeEmptyDelegate(algorithm, tree, 0, node2Cost, threads);
//							isStorageAware[x] = true;
//							isCached[x] = false;
//							isStubborn[x] = false;
//							algorithmNames[x] = "None" + (isStorageAware[x] ? " (MEM)" : " (CPU)");

							//							delegates[++x] = new NAryTreeLPDelegate(algorithm, tree, 0, node2Cost, threads, false);
							//							isStorageAware[x] = true;
							//							isCached[x] = false;
							//							isStubborn[x] = false;
							//							algorithmNames[x] = "LP" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeFullILPDelegate(algorithm, tree, 0, node2Cost, threads, false);
							//							isStorageAware[x] = true;
							//							isCached[x] = false;
							//							isStubborn[x] = false;
							//							algorithmNames[x] = "ILP" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeHybridILPDelegate(algorithm, tree, 0, node2Cost, threads,
							//									false);
							//							isStorageAware[x] = true;
							//							isCached[x] = false;
							//							isStubborn[x] = false;
							//							algorithmNames[x] = "HLP" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeHybridILPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = false;
							//							algorithmNames[x] = "Hybrid ILP Baseline " + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							delegates[++x] = new NAryTreeHybridILPDelegate(algorithm, tree, 0, node2Cost, threads,
									false);
							isStorageAware[x] = true;
							isCached[x] = true;
							isStubborn[x] = true;
							algorithmNames[x] = "HLP C S" + (isStorageAware[x] ? " (MEM)" : " (CPU)");

							//							delegates[++x] = new NAryTreeHybridILPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							useRandomQueue[x] = false;
							//							algorithmNames[x] = "Hybrid" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeBasisILPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							useRandomQueue[x] = false;
							//							algorithmNames[x] = "Basis in tail" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeBasis2ILPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							epsilon[x] = 0.0;
							//							algorithmNames[x] = "Reset basis PLAIN" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeNativeILPDelegate(algorithm, tree, 0, node2Cost, threads,
							//									true, false);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							algorithmNames[x] = "Native reset basis PLAIN" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeNativeILPDelegate(algorithm, tree, 0, node2Cost, threads,
							//									false, false);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							algorithmNames[x] = "Native reset basis No OR" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeNativeILPDelegate(algorithm, tree, 0, node2Cost, threads,
							//									true, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							algorithmNames[x] = "Native basis in tail" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeNativeILPDelegate(algorithm, tree, 0, node2Cost, threads,
							//									true, false);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							epsilon[x] = -.25;
							//							algorithmNames[x] = "Native reset basis STATIC WEIGHTED .25"
							//									+ (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeNativeILPDelegate(algorithm, tree, 0, node2Cost, threads,
							//									true, false);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							epsilon[x] = .25;
							//							algorithmNames[x] = "Native reset basis DYNAMIC WEIGHTED .25"
							//									+ (isStorageAware[x] ? " (MEM)" : " (CPU)");

							//							delegates[++x] = new NAryTreeBasis2ILPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							epsilon[x] = -.5;
							//							algorithmNames[x] = "Reset basis STATIC WEIGHTED .5"
							//									+ (isStorageAware[x] ? " (MEM)" : " (CPU)");

							//							delegates[++x] = new NAryTreeBasis2ILPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							epsilon[x] = -1;
							//							algorithmNames[x] = "Reset basis STATIC WEIGHTED 1"
							//									+ (isStorageAware[x] ? " (MEM)" : " (CPU)");

							//
							//							delegates[++x] = new NAryTreeLPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = true;
							//							isStubborn[x] = true;
							//							useRandomQueue[x] = false;
							//							algorithmNames[x] = "LP Reset basis" + (isStorageAware[x] ? " (MEM)" : " (CPU)");

							//							delegates[++x] = new NAryTreeLPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = false;
							//							isStubborn[x] = true;
							//							algorithmNames[x] = "LP S O" + (isStorageAware[x] ? " (MEM)" : " (CPU)");
							//
							//							delegates[++x] = new NAryTreeFullILPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isCached[x] = false;
							//							isStubborn[x] = true;
							//							algorithmNames[x] = "ILP S O" + (isStorageAware[x] ? " (MEM)" : " (CPU)");

							//							delegates[++x] = new NAryTreeNativeILPDelegate(algorithm, tree, 0, node2Cost, threads, true);
							//							isStorageAware[x] = true;
							//							isStubborn[x] = true;
							//							isCached[x] = true;
							//							algorithmNames[x] = "nILP S+" + (isStorageAware[x] ? " (MEM)" : " (CPU)");

							final Object outLock = new Object();

							//TODO: setup here
							final int maxStates = 1 << 24;
							final int minTrace = 0;
							final int maxTrace = algorithm.getDifferentTraceCount();
							final double maxSecPerTrace = 5.;
							final boolean useObserver = minTrace == maxTrace;
							ok &= !useObserver;

							if (exp == 0 && statFile != null) {
								statFile.print("noise level; tree size; traces; ");
								for (int d = 0; d < delegates.length; d++) {
									statFile.print(algorithmNames[d] + " time;");
									statFile.print(algorithmNames[d] + " time per trace;");
									statFile.print(algorithmNames[d] + " reliable fraction;");
									statFile.print(algorithmNames[d] + " fitness;");
									statFile.print(algorithmNames[d] + " unique states;");
									statFile.print(algorithmNames[d] + " queued states;");
									statFile.print(algorithmNames[d] + " traversed arcs;");
									statFile.print(algorithmNames[d] + " computed estimates;");
									statFile.print(algorithmNames[d] + " penalty costs;");
									statFile.print(algorithmNames[d] + " raw costs;");
								}
								statFile.print("tree ");
								statFile.println();
								statFile.flush();
							}
							if (exp == 0 && n == 0 && size == startTree && avgFile != null) {
								avgFile.print("noise level; tree size;traces ; eventcount;");
								for (int d = 0; d < delegates.length; d++) {
									avgFile.print(algorithmNames[d] + " time;");
									avgFile.print(algorithmNames[d] + " time per trace;");
									avgFile.print(algorithmNames[d] + " reliable fraction;");
									avgFile.print(algorithmNames[d] + " fitness;");
									avgFile.print(algorithmNames[d] + " unique states;");
									avgFile.print(algorithmNames[d] + " queued states;");
									avgFile.print(algorithmNames[d] + " traversed arcs;");
									avgFile.print(algorithmNames[d] + " computed estimates;");
									avgFile.print(algorithmNames[d] + " penalty costs;");
									avgFile.print(algorithmNames[d] + " raw costs;");
								}
								avgFile.println();
								avgFile.flush();
							}

							int d = -1;
							for (final AbstractNAryTreeDelegate<? extends Tail> delegate : delegates) {
								d++;
								final int del = d;
								if (delegate == null) {
									continue;
								}

								int i;
								long start;
								final TObjectIntMap<NAryTreeHead> head2int;
								final List<State<NAryTreeHead, Tail>> stateList;
								final ExecutorService executor;
								final MemoryEfficientAStarAlgorithm<NAryTreeHead, Tail> memEffAlg;
								synchronized (outLock) {
									if (isStorageAware[d]) {
										memEffAlg = new MemoryEfficientAStarAlgorithm<NAryTreeHead, Tail>(
												(StorageAwareDelegate<NAryTreeHead, Tail>) delegate);
										head2int = null;
										stateList = null;
									} else {
										head2int = new TObjectIntHashMap<NAryTreeHead>();
										stateList = new ArrayList<State<NAryTreeHead, Tail>>();
										memEffAlg = null;
									}

									executor = Executors.newFixedThreadPool(threads);

									queuedStateCount = 0;
									traversedArcCount = 0;
									totalRawCost = 0;
									emptyTraceRawCost = 0;
									totalPenaltyCost = 0;
									eventCount = 0;
									computedEstimates = 0;
									reliable = true;
									traceTime = 0;
									reliableTraceCount = 0;
									i = -1;
									start = System.nanoTime();
								}

								for (final Trace trace : algorithm.getConvertedLog().keySet()) {
									if (++i < minTrace || i > maxTrace) {
										continue;
									}
									eventCount += trace.getSize();
									final int job = i;
									executor.execute(new Runnable() {

										public void run() {
											NAryTreeHead initialHead = new NAryTreeHead(delegate, trace);

											AbstractAStarThread<NAryTreeHead, Tail> thread;
											try {
												if (isStorageAware[del]) {
													if (isStubborn[del]) {
														thread = new StubbornNAryTreeAStarThread.MemoryEfficient<NAryTreeHead, Tail>(
																tree, memEffAlg, initialHead, trace, maxStates);
													} else {
														thread = new AStarThread.MemoryEfficient<NAryTreeHead, Tail>(
																memEffAlg, initialHead, trace, maxStates);
													}
												} else if (isStubborn[del]) {
													thread = new StubbornNAryTreeAStarThread.CPUEfficient<NAryTreeHead, Tail>(
															tree, (Delegate<NAryTreeHead, Tail>) delegate, head2int,
															stateList, initialHead, trace, maxStates);
												} else {
													thread = new AStarThread.CPUEfficient<NAryTreeHead, Tail>(
															(Delegate<NAryTreeHead, Tail>) delegate, head2int,
															stateList, initialHead, trace, maxStates);
												}
												thread.setQueueingModel(QueueingModel.DEPTHFIRST);
												if (epsilon[del] < 0) {
													thread.setEpsilon(-epsilon[del]);
													thread.setType(Type.WEIGHTED_STATIC);
												} else if (epsilon[del] > 0) {
													thread.setEpsilon(epsilon[del]);
													thread.setExpectedLength((trace.getSize() + tree.size() + 1) / 2);
													thread.setType(Type.WEIGHTED_DYNAMIC);
												} else {
													thread.setType(Type.PLAIN);
												}
												thread.setASynchronousMoveSorting(ASynchronousMoveSorting.NONE);//MODELMOVEFIRST);

											} catch (AStarException e1) {
												e1.printStackTrace();
												return;
											}
											DotGraphAStarObserver ob = null;
											FSMGraphAStarObserver ob2 = null;

											File dotFile = null;
											String dotFileName = null;
											File fsmFile = null;
											String fsmFileName = null;
											if (useObserver) {
												dotFileName = folder + "astar-" + trace.getLabel() + "-" + del + ".dot";
												dotFile = new File(dotFileName);
												ob = new DotGraphAStarObserver(dotFile);
												thread.addObserver(ob);

												fsmFileName = folder + "astar-" + trace.getLabel() + "-" + del + ".fsm";
												fsmFile = new File(fsmFileName);
												try {
													ob2 = new FSMGraphAStarObserver(fsmFile);
													thread.addObserver(ob2);
												} catch (IOException e) {
													e.printStackTrace();
												}
											}

											TreeRecord rec = null;
											try {
												long traceStart = System.nanoTime();
												rec = (TreeRecord) thread.getOptimalRecord(c, maxSecPerTrace);
												long traceEnd = System.nanoTime();
												//									if (!thread.wasReliable()) {
												//										System.out.println("Result:   " + thread.getOptimalRecord(c));
												//										System.out.println("Trace:    " + job);
												//										System.out.println("Raw cost: " + rec.getCostSoFar());
												//										System.out.println("Time:     " + (threadEnd - threadStart));
												//										//																		System.exit(0);
												//									}

												synchronized (outLock) {
													reliable &= thread.wasReliable();

													if (thread.wasReliable()) {
														reliableTraceCount++;
													}
													traceTime += traceEnd - traceStart;

													double cost = rec.getCostSoFar();

													int len = 0;
													TreeRecord r = rec;
													while (r != null) {
														if (r.getModelMove() < delegate.getTree().size()) {
															cost--;
														}
														cost -= r.getInternalMovesCost();
														r = r.getPredecessor();
														len++;
													}
													cost++;
													assert (cost % delegate.getScaling() == 0);

													if (trace.getSize() == 0) {
														emptyTraceRawCost = (long) cost / delegate.getScaling();
													} else {
														totalRawCost += algorithm.getTraceFreq(trace)
																* rec.getCostSoFar();
														totalPenaltyCost += cost / delegate.getScaling();
													}

													queuedStateCount += thread.getQueuedStateCount();
													traversedArcCount += thread.getTraversedArcCount();
													computedEstimates += thread.getComputedEstimateCount();

													if (useObserver) {
														System.out.println();
														System.out.println(algorithm.getTraceFreq(trace) + " times "
																+ trace);
														System.out.println(TreeUtils.toString(delegate.getTree(),
																classes));
														TreeRecord.printRecord(delegate, trace, rec);
														System.out.println();
													}

													//													ratio += (double) len / (double) thread.getVisitedStateCount();

													if (job % 10 == 0) {
														System.out.print(".");
													}
													//System.out.println(trace);
													//TreeRecord.printRecord(delegate, trace, rec);
												}

											} catch (AStarException e) {
												throw new RuntimeException(e);
												//rec = null;
											}
											if (useObserver) {
												ob.close();
												ob2.close();
												if (rec != null) {
													dotFile.renameTo(new File(dotFileName + "-" + rec.getCostSoFar()));
												}
											}

										}
									});
								}
								executor.shutdown();
								while (!executor.isTerminated()) {
									try {
										executor.awaitTermination(100, TimeUnit.MILLISECONDS);
									} catch (InterruptedException e) {
									}
								}
								long end = System.nanoTime();

								synchronized (outLock) {
									double frac = 1000000000.0;

									// At this point, all traces have been processed.
									// penalty cost were added over the different traces.

									double fitness;
									if (algorithm.getDifferentTraceCount() == 1) {
										fitness = 1.0;
									} else {
										fitness = 1.0
												- (totalPenaltyCost)
												/ ((double) emptyTraceRawCost
														* (algorithm.getDifferentTraceCount() - 1) + eventCount
														* LOGCOST);
									}
									double tt = (traceTime / frac) / algorithm.getDifferentTraceCount();
									if (isStorageAware[d]) {
										stats[d] = new Stat(algorithmNames[d], fitness, (end - start) / frac, tt,
												reliableTraceCount, memEffAlg.getStatespace().size(), queuedStateCount,
												traversedArcCount, computedEstimates, (end == start ? 0
														: queuedStateCount / ((end - start) / frac)), totalRawCost,
												totalPenaltyCost, tree.size(), algorithm.getDifferentTraceCount(),
												eventCount, reliable);
									} else {
										stats[d] = new Stat(algorithmNames[d], fitness, (end - start) / frac, tt,
												reliableTraceCount, stateList.size(), queuedStateCount,
												traversedArcCount, computedEstimates, (end == start ? 0
														: queuedStateCount / ((end - start) / frac)), totalRawCost,
												totalPenaltyCost, tree.size(), algorithm.getDifferentTraceCount(),
												eventCount, reliable);
									}
									System.out.println();
									System.out
											.println("___________________________________________________________________________");
									if (delegates[d] instanceof AbstractNAryTreeLPDelegate) {
										((AbstractNAryTreeLPDelegate<?>) delegates[d]).deleteLPs();
									}
								}
							}
							System.out.println();
							System.out.println("Experiment: " + ++exp);

							//System.out.println(TreeUtils.toString(tree, classes));
							for (int i = 0; i < stats.length; i++) {
								if (stats[i] != null) {
									average[i].update(stats[i]);
								}
							}

							ok &= exp < nExperiments;

							if (statFile != null) {
								statFile.print(noise + "; ");
								statFile.print(tree.size() + "; ");
								statFile.print(algorithm.getDifferentTraceCount() + "; ");
								for (d = 0; d < delegates.length; d++) {
									statFile.print(stats[d].time[0] + "; ");
									statFile.print(stats[d].timePerTrace[0] + "; ");
									statFile.print((1.0 * stats[d].reliableCount[0]) / stats[d].logSize[0] + "; ");
									statFile.print(stats[d].fitness[0] + "; ");
									statFile.print(stats[d].unique[0] + "; ");
									statFile.print(stats[d].queued[0] + "; ");
									statFile.print(stats[d].arcs[0] + "; ");
									statFile.print(stats[d].estimates[0] + "; ");
									statFile.print(stats[d].penaltyCost[0] + "; ");
									statFile.print(stats[d].rawCost[0] + "; ");
								}
								statFile.print(TreeUtils.toString(tree, 0, classes));
								statFile.print(";");
								statFile.println();
								statFile.flush();
							} else {
								ok &= Stat.printAndCheckConsistent(traces, stats);
								Stat.printAndCheckConsistent(traces, average);
							}

						} while (ok);
					} finally {
						if (avgFile != null) {
							avgFile.print(noise + "; ");
							avgFile.print(average[0].treeSize[0] / average[0].count + "; ");
							avgFile.print(average[0].logSize[0] / average[0].count + "; ");
							avgFile.print(average[0].eventCount[0] / average[0].count + "; ");
							for (int d = 0; d < average.length; d++) {
								avgFile.print(average[d].time[0] / average[d].count + "; ");
								avgFile.print(average[d].timePerTrace[0] / average[d].count + "; ");
								avgFile.print(((1.0 * average[d].reliableCount[0]) / average[d].logSize[0]) + "; ");
								avgFile.print(average[d].fitness[0] / average[d].count + "; ");
								avgFile.print(average[d].unique[0] / average[d].count + "; ");
								avgFile.print(average[d].queued[0] / average[d].count + "; ");
								avgFile.print(average[d].arcs[0] / average[d].count + "; ");
								avgFile.print(average[d].estimates[0] / average[d].count + "; ");
								avgFile.print(average[d].penaltyCost[0] / average[d].count + "; ");
								avgFile.print(average[d].rawCost[0] / average[d].count + "; ");
							}
							avgFile.println();
							avgFile.flush();
						}
						if (statFile != null) {
							statFile.flush();
							statFile.close();
						}

					}
				}
			}
		} finally {
			avgFile.flush();
			avgFile.close();
			long endTime = System.nanoTime();

			System.out.println("time: " + (endTime - startTime) / 1.0E9);
		}
	}

	public static String[][] getTraces(Simulator sim, int numTraces, String[] activities, double noise) {

		String[][] traces = new String[numTraces][];
		traces[0] = new String[] {};
		for (int i = 1; i < numTraces; i++) {
			traces[i] = sim.getRandomTrace(activities, noise);
		}

		//		traces = new String[][] { { "A" } };
		return traces;

	}

	public static ProbProcessArrayTree getTree(TObjectShortMap<String> map, Random r, int size) {
		ProbProcessArrayTree tree;
		tree = TreeUtils.randomTree(map.keySet().size(), 0.4, 6, size, r);
		boolean[] b = new boolean[tree.size()];
		boolean[] h = new boolean[tree.size()];
		for (int i = 0; i < tree.size(); i++) {
			if (r.nextFloat() < .15) {
				h[i] = true;
			}
		}
		Configuration c = new Configuration(b, h);
		tree.addConfiguration(c);

		//		tree = TreeUtils
		//				.fromString(
		//						"SEQ( LEAF: C , REVSEQ( AND( SEQ( LEAF: M ) , LEAF: H ) , LEAF: C ) ) [[-, -, -, -, -, -, -, -] ]",
		//						map);
		//		tree = TreeUtils.fromString("LOOP( OR( LEAF: D , LEAF: B ) , LEAF: A , LEAF: E ) [[-, -, -, -, -, -] ]", map);
		//		tree = TreeUtils.fromString(
		//				"OR( SEQ( LEAF: K , LEAF: I ) , LEAF: N , LEAF: J , LEAF: M ) [[-, -, -, -, -, -, -] ]", map);
		//
		//		try {
		//			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(new File("C:\\temp\\tree.dot")));
		//			TreeUtils.writeTreeToDot(tree, 0, out);
		//			out.close();
		//		} catch (FileNotFoundException e) {
		//			e.printStackTrace();
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}

		//		tree = TreeUtils
		//				.fromString(
		//						"  REVSEQ( LEAF: H , SEQ( LEAF: J , SEQ( OR( OR( LEAF: tau , LOOP( REVSEQ( AND( AND( AND( LEAF: L , LEAF: tau , LEAF: tau , LEAF: N ) , LEAF: B , LEAF: B ) , LEAF: I , LEAF: E , LEAF: A ) , LEAF: D , LEAF: C ) , LEAF: D , LEAF: G ) , LEAF: J ) , LEAF: L , LEAF: K , LEAF: K ) , LEAF: E , LEAF: E , LEAF: H ) , LEAF: J , LEAF: M ) , LEAF: N , LEAF: J ) [[-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -] ]",
		//						map);

		//		tree = TreeUtils.fromString("SEQ( LEAF: A , OR( LEAF: B , LEAF: C , LEAF: D ) ) [[-, -, -] ]", map);
		return tree;
	}

	public static void mergeStatFiles(String folder, String outFile, TObjectShortMap<String> map) throws IOException {

		long l = 0;
		int files = 0;
		File dir = new File(folder);
		File file = new File(outFile);
		BufferedWriter out = new BufferedWriter(new FileWriter(file));
		StringTokenizer tok;
		String line;
		for (File f : dir.listFiles()) {
			if (!f.getName().startsWith("stats") || f.getName().contains("avg")) {
				continue;
			}
			files++;
			//			int lines = 0;

			BufferedReader in = new BufferedReader(new FileReader(f));
			line = in.readLine();
			if (l == 0) {
				out.write(line.replace(';', '\t'));
				out.write("configured size \t");
				out.write("tree category \t");
				out.write("fitness category \t");
				out.write("original tree \t");
				out.write("configured tree \t");
				out.newLine();
				l++;
			}

			double fitness = 1.0;
			while ((line = in.readLine()) != null) {
				tok = new StringTokenizer(line, ";");

				int i = 0;
				String token = tok.nextToken();
				do {
					if (i++ == 5) {
						fitness = Double.parseDouble(token);
					}
					out.write(token);
					out.write("\t ");
					token = tok.nextToken();
				} while (tok.hasMoreTokens());

				ProbProcessArrayTree tree = TreeUtils.fromString(token, map);

				tree = tree.applyConfiguration(0);
				out.write(" " + tree.size());
				out.write(" \t ");
				out.write(" " + (int) Math.round(tree.size() / 5.0));
				out.write(" \t ");
				out.write(" " + Math.round(20 * fitness) / 20.0);
				out.write(" \t ");
				out.write(token);
				out.write("\t ");
				out.write(TreeUtils.toString(tree));
				out.write("\t ");

				out.newLine();
				l++;
				//				lines++;
			}
			in.close();
			out.flush();

			//			if (lines < 20) {
			//				System.out.println("File: " + f + " contains too few lines");
			//			}

		}
		out.close();
		System.out.println(l + " lines written from " + files + " files");
		System.exit(0);
	}
}
