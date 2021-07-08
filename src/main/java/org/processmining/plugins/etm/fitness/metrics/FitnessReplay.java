package org.processmining.plugins.etm.fitness.metrics;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;

import org.processmining.framework.util.ui.widgets.ProMTextField;
import org.processmining.plugins.boudewijn.treebasedreplay.astar.ModelPrefix;
import org.processmining.plugins.boudewijn.treebasedreplay.astar.TreeMarkingVisit;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.narytree.replayer.AStarAlgorithm;
import org.processmining.plugins.etm.model.narytree.replayer.NAryTreeReplayer;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.processmining.plugins.etm.termination.ProMCancelTerminationCondition;

import com.fluxicon.slickerbox.factory.SlickerFactory;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.set.TIntSet;
import nl.tue.astar.AStarThread.Canceller;
import nl.tue.astar.Trace;

/**
 * Using a specialized replay algorithm based on Adriansyah an alignment between
 * a process tree and the event log is calculated.
 * 
 * @author jbuijs
 * 
 */
//FIXME check all class contents
//FIXME Test Class thoroughly
@Deprecated // "Non-stochastic alignments"
public class FitnessReplay extends TreeFitnessAbstract {

	public static final TreeFitnessInfo info = new TreeFitnessInfo(FitnessReplay.class, "Fr", "Replay Fitness",
			"Calculates the optimal alignment between the log and the process tree to determine where deviations are",
			Dimension.FITNESS, true);

	protected double fitnessLimit;
	protected double timeLimit;

	protected Canceller c;

	protected final CentralRegistry registry;

	private boolean lastResultReliable = true;

	/**
	 * Which configuration of the NAryTree to apply
	 */
	//protected int configurationNumber = -1;

	/**
	 * Boolean to indicate whether the detailed alignment information should be
	 * stored in the tree fitness cache.
	 */
	protected boolean detailedAlignmentInfoEnabled = true;

	protected long maxBytestoUse = -1;

	/**
	 * Test code added to print trees that take a long time to the console. Make
	 * negative to disable this feature.
	 */
	private long logTreeIfExecutionTookMoreThan = -1 * 1000 * 60; //1000*60 is 1 minute, which is long for 1 alignment

	private boolean caching = true;

	private boolean stubborn = true;

	private boolean useOrRows = true;

	private boolean cpuEfficient = false;

	/**
	 * Nr threads to let the replayer use
	 */
	private int nrThreads;

	/**
	 * Deep-clone copy constructor.
	 * 
	 * @param original
	 */
	public FitnessReplay(FitnessReplay original) {
		this(original.registry, original.c, original.fitnessLimit, original.timeLimit,
				original.detailedAlignmentInfoEnabled, original.maxBytestoUse, 1);
	}

	/**
	 * Instantiate a new FitnessReplay instance that calculates the replay
	 * fitness between log and model and provides information required by some
	 * other metrics (such as Precision and Generalization). Sets default values
	 * for stopAt and timeLimit. Assumes alignments detailed information is ON.
	 * 
	 * @param registry
	 *            {@link CentralRegistry} The Central Registry required for
	 *            single-point access to log etc.
	 * @param c
	 *            A special canceller that cancels the alignment execution on
	 *            the users request
	 */
	public FitnessReplay(CentralRegistry registry, Canceller c) {
		this(registry, c, -1, -1, false, -1, 1);
	}

	/**
	 * Instantiate a new FitnessReplay instance that calculates the replay
	 * fitness between log and model and provides information required by some
	 * other metrics (such as Precision and Generalization). Assumes alignments
	 * detailed information is ON.
	 * 
	 * @param registry
	 *            {@link CentralRegistry} The Central Registry required for
	 *            single-point access to log etc.
	 * @param c
	 *            A special canceller that cancels the alignment execution on
	 *            the users request
	 * @param fitnessLimit
	 *            The minimum fitness level required. If a certain model has a
	 *            worse fitness than this calculations are stopped and the worst
	 *            value is returned. Set to a negative value to disable this
	 *            limit.
	 * @param timeLimit
	 *            The time limit in seconds that a single-trace alignment
	 *            calculation can take. A negative value indicates no limit.
	 */
	public FitnessReplay(CentralRegistry registry, Canceller c, double fitnessLimit, double timeLimit) {
		this(registry, c, fitnessLimit, timeLimit, true, -1, 1);
	}

	/**
	 * Instantiate a new FitnessReplay instance that calculates the replay
	 * fitness between log and model and provides information required by some
	 * other metrics (such as Precision and Generalization).
	 * 
	 * @param registry
	 *            {@link CentralRegistry} The Central Registry required for
	 *            single-point access to log etc.
	 * @param c
	 *            A special canceller that cancels the alignment execution on
	 *            the users request
	 * @param configurationNumber
	 *            The configuration of the NAryTree to use. If no configurations
	 *            are used, supply -1
	 * @param fitnessLimit
	 *            The minimum fitness level required. If a certain model has a
	 *            worse fitness than this calculations are stopped and the worst
	 *            value is returned. Set to a negative value to disable this
	 *            limit.
	 * @param timeLimit
	 *            The time limit in seconds that a single-trace alignment
	 *            calculation can take. A negative value indicates no limit.
	 * @param detailedAlignmentInfoEnabled
	 *            If set to TRUE very detailed alignment information is stored
	 *            in the
	 *            {@link org.processmining.plugins.etm.fitness.TreeFitness}
	 *            cache in the {@link CentralRegistry}.
	 * @param maxBytesToUse
	 *            The maximum number of bytes allowed for the algorithm to use,
	 *            if it goes over it clears the cache (e.g. does not guarantee
	 *            the algorithm does not go over but will use it to clear the
	 *            cache). Negative value indicates no limit
	 * @param nrThreads
	 *            The number of threads to use during alignment calculation. For
	 *            ETM use 1 is recommended and parallel calculations of trees is
	 *            recommended.
	 */
	public FitnessReplay(CentralRegistry registry, Canceller c, double fitnessLimit, double timeLimit,
			boolean detailedAlignmentInfoEnabled, long maxBytesToUse, int nrThreads) {
		this.registry = registry;
		this.c = c;
		//		this.configurationNumber = configurationNumber;
		this.fitnessLimit = fitnessLimit;
		this.timeLimit = timeLimit;
		this.detailedAlignmentInfoEnabled = detailedAlignmentInfoEnabled;
		this.maxBytestoUse = maxBytesToUse;
		this.nrThreads = nrThreads;
	}

	/**
	 * {@inheritDoc}
	 */
	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		return -1;
//		assert candidate.isConsistent();
//		lastResultReliable = true; //the new evaluation is reliable unless shown otherwise
//
//		//We use the configuration only if that configuration actually exists. Otherwise we assume no configuration
//		//		int localConfNr = configurationNumber <= candidate.getNumberOfConfigurations() - 1 ? configurationNumber : -1;
//
//		//First clear previous results...
//		BehaviorCounter behC = registry.getFitness(candidate).behaviorCounter;
//		//For the rare/Maikel case where you have duplicate trees in a population that are evaluated at the same time (and have the same behC instance in two threads)
//		synchronized (behC) {
//			Map<TreeMarkingVisit<ModelPrefix>, TIntSet> marking2modelMove = behC.getMarking2ModelMove();
//			TObjectIntMap<TreeMarkingVisit<ModelPrefix>> marking2visitCount = behC.getMarking2VisitCount();
//
//			int[] modelCosts = modelCosts(candidate);
//
//			int[] syncMoveCount = new int[candidate.size()];
//			int[] aSyncMoveCount = new int[candidate.size()];
//			int[] moveCount = new int[candidate.size()];
//
//			NAryTreeReplayer<?, ?, ?> treeBasedAStar = null;
//			try {
//				treeBasedAStar = setupReplayer(candidate, registry.getEmptyAStarAlgorithm(),
//						new HashMap<TreeMarkingVisit<ModelPrefix>, TIntSet>(0),
//						new TObjectIntHashMap<TreeMarkingVisit<ModelPrefix>>(0), modelCosts, syncMoveCount,
//						aSyncMoveCount, moveCount, null);
//			} catch (UnsatisfiedLinkError ule) {
//				System.err.println("LP Solve is not in your path so alignments cannot be calculated.");
//				ule.printStackTrace();
//
//			} catch (Exception eGeneral) {
//				eGeneral.printStackTrace();
//				System.out.println("Tree: " + TreeUtils.toString(candidate, registry.getEventClasses()));
//				TreeUtils.printTree(candidate);
//				return info.getWorstFitnessValue();
//			}
//
//			int minModelCost;
//			try {
//				//We don't pass a stopAt cost, but do pass a timeLimit since replaying the empty log should be easiest of all... 
//				minModelCost = treeBasedAStar.run(VerboseLevel.NONE, Integer.MAX_VALUE, Integer.MAX_VALUE, timeLimit);
//			} catch (AStarException e) {
//				e.printStackTrace();
//				return info.getWorstFitnessValue();
//			} catch (NullPointerException npe) {
//				npe.printStackTrace();
//				System.out.println("Tree: " + TreeUtils.toString(candidate, registry.getEventClasses()));
//				TreeUtils.printTree(candidate);
//				return info.getWorstFitnessValue();
//			} catch (Exception eGeneral) {
//				eGeneral.printStackTrace();
//				System.out.println("Tree: " + TreeUtils.toString(candidate, registry.getEventClasses()));
//				TreeUtils.printTree(candidate);
//				return info.getWorstFitnessValue();
//			}
//
//			//Early exit
//			if (!treeBasedAStar.wasReliable() || c.isCancelled()) {
//				//FIXME for this and all other unreliable cases: clear alignments etc. since we are not finished and complete
//				lastResultReliable = false;
//				//System.out.println("Unreliable: " + TreeUtils.toString(candidate, registry.getEventClasses()) + " config " + configurationNumber);
//				//reset behavior counter and alignments since they are not reliable
//				registry.getFitness(candidate).behaviorCounter = new BehaviorCounter(candidate.size());
//				registry.getFitness(candidate).setReliable(false);
//				return info.getWorstFitnessValue();
//			}
//
//			syncMoveCount = new int[candidate.size()]; //# executions of this subtree containing 1+ sync. moves
//			aSyncMoveCount = new int[candidate.size()]; //# executions of this subtree containing 1+ async. moves
//			moveCount = new int[candidate.size()]; //# execution of this subtree/node
//			Map<Trace, TreeRecord> alignments = null;
//			if (detailedAlignmentInfoEnabled) {
//				alignments = behC.getAlignments();
//			}
//
//			// The total move_on_model cost for aligning the empty trace is stored in the last optimal record of treeBasedAStar
//			int rawModelCost = (int) treeBasedAStar.getRawCost();
//
//			treeBasedAStar = setupReplayer(candidate, registry.getaStarAlgorithm(), marking2modelMove,
//					marking2visitCount, modelCosts, syncMoveCount, aSyncMoveCount, moveCount, alignments);
//
//			/*
//			 * And run and return it
//			 */
//			/*
//			 * But first calculate when the alg. can stop because its just
//			 * bad... Since the final Fr value is calculated according to {1-
//			 * (totalCost / (minLogCost + f * minModelCost))} we calculate the
//			 * cost to stop at as {(1-Fr) * (minLogCost + f * minModelCost)}
//			 */
//
//			int stopAt = (int) ((1 - fitnessLimit) * (registry.getEstimatedMinLogCost() + registry.getLog().size()
//					* minModelCost));
//			stopAt *= treeBasedAStar.getDelegate().getScaling();
//
//			if (stopAt < 0) {
//				stopAt = Integer.MAX_VALUE;
//			}
//
//			try {
//				long start = new Date().getTime();
//				double totalCost = treeBasedAStar.run(VerboseLevel.NONE, stopAt, rawModelCost, timeLimit);
//				long end = new Date().getTime();
//				//Stop if not reliable
//				if (!treeBasedAStar.wasReliable() || c.isCancelled()) {
//					//TODO remove debug code
//					if (!c.isCancelled()) {
//						//DEBUG message if not cancelled but still unreliable
//						//System.out.println("Unreliable: " + TreeUtils.toString(candidate, registry.getEventClasses())	+ " config " + configurationNumber);
//					}
//					lastResultReliable = false;
//					//reset behavior counter and alignments since they are not reliable
//					registry.getFitness(candidate).behaviorCounter = new BehaviorCounter(candidate.size());
//					registry.getFitness(candidate).setReliable(false);
//					return info.getWorstFitnessValue();
//				}
//
//				//Calculate the minimal cost for replaying the log on an 'empty' model 
//				int f = 0;
//				double minLogCost = 0;
//				//double recordCost = 0;
//				Iterator<Trace> it = registry.getaStarAlgorithm().traceIterator();
//				while (it.hasNext()) {
//					Trace trace = it.next();
//					int freq = registry.getaStarAlgorithm().getTraceFreq(trace);
//					f += freq;
//					TIntIterator it2 = trace.iterator();
//					while (it2.hasNext()) {
//						minLogCost += registry.getaStarAlgorithm().getLogMoveCost(it2.next()) * freq;
//					}
//
////					TreeRecord.printRecord(treeBasedAStar.getDelegate(), trace, alignments.get(trace));
////					recordCost += alignments.get(trace).getTotalCost() * freq;
//
//				}
//
//				//Set the three move arrays in the Fitness cache
//				//registry.getFitness(candidate).behaviorCounters
//				behC.setSyncMoveCount(syncMoveCount);
//				behC.setASyncMoveCount(aSyncMoveCount);
//				behC.setMoveCount(moveCount);
//				behC.setMinModelCost(minModelCost);
//				if (detailedAlignmentInfoEnabled) {
//					behC.setMarking2ModelMove(marking2modelMove);
//					behC.setMarking2VisitCount(marking2visitCount);
//				}
//
//				//Result is the total cost divided by the 'minimal' cost without synchronous moves
//				double fitness = 1.0 - (totalCost / (minLogCost + f * minModelCost));
////				System.out.println(recordCost + " vs " + totalCost);
//
//				if (logTreeIfExecutionTookMoreThan > 0 && (end - start) > logTreeIfExecutionTookMoreThan) {
//					System.out.println(String.format("Long calculation detected for tree (Fr = %1.3f, %.1fs):",
//							fitness, (end - start) / 1000.0));
//					System.out.println(TreeUtils.toString(candidate, registry.getEventClasses()));
//				}
//
//				return fitness;
//			} catch (AStarException e) {
//				e.printStackTrace();
//				return info.getWorstFitnessValue();
//			}
//		}

	}

	public NAryTreeReplayer<?, ?, ?> setupReplayer(ProbProcessArrayTree candidate, AStarAlgorithm algorithm,
			Map<TreeMarkingVisit<ModelPrefix>, TIntSet> marking2modelMove,
			TObjectIntMap<TreeMarkingVisit<ModelPrefix>> marking2visitCount, int[] modelCosts, int[] syncMoveCount,
			int[] aSyncMoveCount, int[] moveCount, Map<Trace, TreeRecord> alignments) {
		return null;
		// NAryTreeReplayer<?, ?, ?> treeBasedAStar;
		//		if (isCpuEfficient()) {
		//			if (!caching) {
		//				treeBasedAStar = new NAryTreeReplayerWithFullILP(algorithm, c, candidate, localConfNr, modelCosts,
		//						marking2modelMove, marking2visitCount, syncMoveCount, aSyncMoveCount, moveCount, alignments,
		//						useOrRows);
		//			} else {
		//				treeBasedAStar = new NAryTreeReplayerWithHybridILP(algorithm, c, candidate, localConfNr, modelCosts,
		//						marking2modelMove, marking2visitCount, syncMoveCount, aSyncMoveCount, moveCount, alignments,
		//						useOrRows);
		//			}
		//			treeBasedAStar.setType(PerformanceType.CPUEFFICIENT);
		//		} else {
		//			if (!caching) {
		//				treeBasedAStar = new NAryTreeReplayerWithFullILP(algorithm, c, candidate, localConfNr, modelCosts,
		//						marking2modelMove, marking2visitCount, syncMoveCount, aSyncMoveCount, moveCount, alignments,
		//						useOrRows);
		//			} else {
		//				treeBasedAStar = new NAryTreeReplayerWithHybridILP(algorithm, c, candidate, localConfNr, modelCosts,
		//						marking2modelMove, marking2visitCount, syncMoveCount, aSyncMoveCount, moveCount, alignments,
		//						useOrRows);
		//			}
		//			treeBasedAStar.setType(PerformanceType.MEMORYEFFICIENT);
		//		}
//		assert candidate.isConsistent();
//		try {
//			treeBasedAStar = new NAryTreeReplayerWithBasis2ILP(algorithm, c, candidate, -1, modelCosts,
//					marking2modelMove, marking2visitCount, syncMoveCount, aSyncMoveCount, moveCount, alignments, true,
//					nrThreads);
//			/*
//			 * HV: Create a backing store that can be sufficiently large on this
//			 * computer.
//			 */
//			long mem = OsUtil.getPhysicalMemory();
//			long size = 4L * 1024 * 1024 * 1024;
//			int alignment = 1;
//			while (mem > size) {
//				alignment++;
//				mem -= size;
//			}
//			treeBasedAStar.setType(PerformanceType.MEMORYEFFICIENT, 16 * 1024, alignment);
//			treeBasedAStar.setStubborn(true);
//			treeBasedAStar.setMaxNumberOfBlocksToUse(maxBytestoUse);
//			return treeBasedAStar;
//		} catch (Exception e) {
//			System.out.println("Candidate: " + candidate);
//			System.out.println("internal:");
//			TreeUtils.printTree(candidate);
//			e.printStackTrace();
//			return null;
//		}
	}

	public static int[] modelCosts(ProbProcessArrayTree tree) {
		int[] modelCosts = new int[tree.size()];

		int i = tree.isLeaf(0) ? 0 : tree.getNextLeafFast(0);
		do {
			if (tree.getType(i) != ProbProcessArrayTree.TAU) {
				//All the 'real' leafs cost something 
				modelCosts[i] = CentralRegistry.MMcost;
			}
			i = tree.getNextLeafFast(i);
		} while (i < tree.size());
		return modelCosts;
	}

	/**
	 * {@inheritDoc}
	 */
	public TreeFitnessInfo getInfo() {
		return info;
	}

	/**
	 * Returns the threshold at which calculations are stopped.
	 * 
	 * @return the maxF
	 */
	public double getMaxF() {
		return fitnessLimit;
	}

	/**
	 * Set a new limit on the replay fitness value, to improve performance.
	 * 
	 * @param maxF
	 *            the maxF to set
	 */
	public void setMaxF(double maxF) {
		this.fitnessLimit = maxF;
	}

	/**
	 * @return the timeLimit
	 */
	public double getTimeLimit() {
		return timeLimit;
	}

	/**
	 * @param timeLimit
	 *            the timeLimit to set
	 */
	public void setTimeLimit(double timeLimit) {
		this.timeLimit = timeLimit;
	}

	/**
	 * Returns whether the last result was reliable or not. If true, other
	 * algorithms can continue and use the information derived from the
	 * alignment. Otherwise there is no such data, it is incomplete or not
	 * optimal. A result can be not reliable if the time limit or max fitness
	 * has been exceeded.
	 * 
	 * @return the lastResultReliable
	 */
	public boolean isLastResultReliable() {
		return lastResultReliable;
	}

	/**
	 * @return the configurationNumber
	 */
	//	public int getConfigurationNumber() {
	//		return configurationNumber;
	//	}

	/**
	 * @param configurationNumber
	 *            the configurationNumber to set
	 */
	//	public void setConfigurationNumber(int configurationNumber) {
	//		this.configurationNumber = configurationNumber;
	//	}

	/**
	 * @return the detailedAlignmentInfoEnabled
	 */
	public boolean isDetailedAlignmentInfoEnabled() {
		return detailedAlignmentInfoEnabled;
	}

	public static FitnessReplayGUI getGUISettingsPanel(ETMParamAbstract param) {
		return new FitnessReplayGUI(param);
	}

	/**
	 * Enables/disables the alignment cache. More details are then stored in the
	 * {@link org.processmining.plugins.etm.fitness.TreeFitness} cache in the
	 * {@link CentralRegistry}. ONLY ENABLE DURING DEBUGGING!
	 * 
	 * @param detailedAlignmentInfoEnabled
	 *            the detailedAlignmentInfoEnabled to set
	 */
	public void setDetailedAlignmentInfoEnabled(boolean detailedAlignmentInfoEnabled) {
		this.detailedAlignmentInfoEnabled = detailedAlignmentInfoEnabled;
	}

	public static class FitnessReplayGUI extends TreeFitnessGUISettingsAbstract<FitnessReplay> {

		private static final long serialVersionUID = 1L;

		private ProMTextField fitnessLimit;
		private ProMTextField timeLimit;

		public FitnessReplayGUI(ETMParamAbstract param) {
			super(param);

			this.setLayout(new GridLayout(2, 2));

			String fitnessLimitTooltipText = "<html>Stop calculations for a particular process tree as soon as the replay fitness is lower than the provided value (double between 0 and 1). <br>"
					+ "This is used to save time and not waste it on bad process trees. <br>"
					+ "-1 Indicates that this feature is disabled</html>";
			JLabel fitnessLimitLabel = SlickerFactory.instance().createLabel("Fitness limit: ");
			fitnessLimitLabel.setToolTipText(fitnessLimitTooltipText);
			this.add(fitnessLimitLabel);
			fitnessLimit = new ProMTextField("-1");
			fitnessLimit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean isOkay = false;
					try {
						Double.parseDouble(fitnessLimit.getText());
						isOkay = true;
					} catch (Exception e1) {
						isOkay = false;
					}
					fitnessLimit.visualizeStatus(isOkay);
				}
			});
			fitnessLimit.setToolTipText(fitnessLimitTooltipText);
			fitnessLimit.setSize(100, 25);
			this.add(fitnessLimit);

			String timeLimitTooltipText = "<html>Stop calculations for a particular process tree as soon as the replay fitness spend more than the indicated time (in ms, e.g. 1000 = 1 second). <br>"
					+ "This is used to save time and not waste it on bad process trees. <br>"
					+ "-1 Indicates that this feature is disabled</html>";
			JLabel timeLimitLabel = SlickerFactory.instance().createLabel("Time limit (in milliseconds): ");
			this.add(timeLimitLabel);
			timeLimitLabel.setToolTipText(timeLimitTooltipText);
			timeLimit = new ProMTextField("-1");
			timeLimit.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					boolean isOkay = false;
					try {
						Double.parseDouble(timeLimit.getText());
						isOkay = true;
					} catch (Exception e1) {
						isOkay = false;
					}
					timeLimit.visualizeStatus(isOkay);
				}
			});
			timeLimit.setSize(100, 25);

			this.add(timeLimit);

			this.setPreferredSize(new java.awt.Dimension(300, 30));
		}

		public FitnessReplay getTreeFitnessInstance(final CentralRegistry registry, Class<TreeFitnessAbstract> clazz) {
			Canceller c = new ProMCancelTerminationCondition(registry.getContext()).getCanceller();

			return new FitnessReplay(registry, c, Double.parseDouble(fitnessLimit.getText()),
					Double.parseDouble(timeLimit.getText()), true, -1, 1);
		}

		public void init(FitnessReplay instance) {
			fitnessLimit.setText("" + instance.fitnessLimit);
			timeLimit.setText("" + instance.timeLimit);
		}
	}

	/**
	 * The alignment algorithm will clear its internal cache when this number of
	 * bytes is (almost) exceeded. This to try to prevent
	 * {@link OutOfMemoryError} exceptions, especially for larger models and
	 * longer traces. Set this as high as you can to improve performance but as
	 * low as you think is necessary to prevent a memory overrun.
	 * 
	 * @param maxBytes
	 */
	public void updateMaxBytesToUse(long maxBytes) {
		this.maxBytestoUse = maxBytes;
	}

	public void setStubborn(boolean stubborn) {
		this.stubborn = stubborn;
	}

	public void setCaching(boolean caching) {
		this.caching = caching;
	}

	public void setUseOrRows(boolean useOrRows) {
		this.useOrRows = useOrRows;
	}

	public void setCpuEfficient(boolean cpuEfficient) {
		this.cpuEfficient = cpuEfficient;
	}

	public boolean isCpuEfficient() {
		return cpuEfficient;
	}

	/**
	 * @return the nrThreads used to calculate an alignment between 1 model/tree
	 *         and the log
	 */
	public int getNrThreads() {
		return nrThreads;
	}

	/**
	 * @param nrThreads
	 *            the nrThreads to set. Update the number of threads to be used
	 *            for the alignment calculation. Recommended is half the number
	 *            of CPU's Java thinks there are, since one half is 'real' and
	 *            the other is via hyperThreading...
	 */
	public void setNrThreads(int nrThreads) {
		this.nrThreads = nrThreads;
	}

}
