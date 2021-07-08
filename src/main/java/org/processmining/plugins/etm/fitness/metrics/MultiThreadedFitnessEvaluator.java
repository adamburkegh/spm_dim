package org.processmining.plugins.etm.fitness.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

/**
 * This fitness evaluator takes care of multi-threaded calls from the Watchmaker
 * framework. It keeps an array of evaluators and whether or not they are busy.
 * If available it will delegate the getFitness call to the available evaluator.
 * 
 * To avoid computing fitness on two identical candidates in parallel, this
 * class suspends a call for a candidate if the same candidate is currently
 * being processed in another thread. Hence, when using this
 * MultiThreadedFitnessEvaluator, it is wise to use a cached threadpool in the
 * FitnessEvaluationWorker.
 * 
 * @author bfvdonge
 * 
 */
public class MultiThreadedFitnessEvaluator extends TreeFitnessAbstract {

	private final int threads;
	private final Map<ProbProcessArrayTree, ProbProcessArrayTree> currentTrees;
	private final ReentrantLock currentTreeLock = new ReentrantLock();
	private final TreeFitnessAbstract[] evaluators;
	private final boolean[] busy;

	public static final TreeFitnessInfo info = new TreeFitnessInfo(MultiThreadedFitnessEvaluator.class, "Mt",
			"Multi-threaded Evaluator",
			"Preferred way of multi-threading, this evaluator will assign calculations to free evaluator threads, "
					+ "each deep-cloned from the initial one provide.", Dimension.META, true);
	private final CentralRegistry registry;

	public MultiThreadedFitnessEvaluator(CentralRegistry registry, TreeFitnessAbstract... evaluators) {
		this.registry = registry;
		assert evaluators.length > 0;
		this.threads = evaluators.length;
		this.currentTrees = new HashMap<ProbProcessArrayTree, ProbProcessArrayTree>(threads);
		this.evaluators = evaluators;
		this.busy = new boolean[threads];
		Arrays.fill(busy, false);
	}

	public MultiThreadedFitnessEvaluator(CentralRegistry registry, TreeFitnessAbstract evaluator, int threads) {
		this.registry = registry;
		assert evaluator != null;

		this.threads = threads;
		this.currentTrees = new HashMap<ProbProcessArrayTree, ProbProcessArrayTree>(threads);

		TreeFitnessAbstract[] evaluators = new TreeFitnessAbstract[threads];
		for (int i = 0; i < threads; i++) {
			evaluators[i] = TreeFitnessAbstract.deepClone(evaluator);
		}
		this.evaluators = evaluators;
		this.busy = new boolean[threads];
		Arrays.fill(busy, false);
	}

	protected int getFirstAvailable() {
		while (true) {
			synchronized (busy) {
				try {
					int i = 0;
					while (i < threads && busy[i]) {
						i++;
					}
					if (i < threads) {
						busy[i] = true;
						return i;
					}
					busy.wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		// Check if either fitness is already known, or the tree is being processed
		if (registry.isFitnessKnown(candidate)) {
			// fitness is known
			return registry.getFitness(candidate).getOverallFitnessValue();
		} else {
			ProbProcessArrayTree tree = null;
			try {
				currentTreeLock.lock();

				if (currentTrees.containsKey(candidate)) {
					// If fitness is not known (but being computed in another thread) wait until
					// 	fitness is known.
					tree = currentTrees.get(candidate);
					assert tree != candidate;
				} else {
					// add the candidate to currentTrees, as this candidate is now going to be processed
					currentTrees.put(candidate, candidate);
				}

				if (tree != null) {
					synchronized (tree) {
						currentTreeLock.unlock();
						while (!registry.isFitnessKnown(candidate)) {
							try {
								// wait until notified. Notification is done every time a fitness
								// evaluator completes.
								tree.wait();
							} catch (InterruptedException ie) {
								// gracefully ignore
							}
						}
					}
					return registry.getFitness(candidate).getOverallFitnessValue();
				}
			} finally {
				if (currentTreeLock.isHeldByCurrentThread()) {
					currentTreeLock.unlock();
				}
			}
		}

		int i = getFirstAvailable();
		double f;
		try {
			f = evaluators[i].getFitness(candidate, population);
		} finally {
			makeAvailable(i);
		}
		try {
			currentTreeLock.lock();
			currentTrees.remove(candidate);
		} finally {
			currentTreeLock.unlock();
		}
		// we're done with this tree. Remove candidate from currentTrees
		// and notify any listening thread.
		synchronized (candidate) {
			candidate.notifyAll();
		}

		return f;
	}

	protected void makeAvailable(int i) {
		synchronized (busy) {
			busy[i] = false;
			busy.notify();
		}
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}

	public int getNrEvaluators() {
		return evaluators.length;
	}

	public TreeFitnessAbstract[] getEvaluators() {
		return evaluators;
	}

}
