package org.processmining.plugins.etm.model.ppt;

import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Random;

public class Simulator {

	protected final ProbProcessArrayTree tree;
	protected final StateBuilder stateBuilder;
	protected final int configurationNumber;
	protected final Random random;

	public Simulator(ProbProcessArrayTree tree, int configurationNumber, Random random) {
		this.tree = tree;
		this.configurationNumber = configurationNumber;
		this.random = random;
		this.stateBuilder = new StateBuilder(tree, configurationNumber);
	}

	public String[] getRandomTrace(String[] activities) {
		return getRandomTrace(activities, 0.0);
	}

	public String[] getRandomTrace(String[] activities, double noiseLevel) {
		TIntList executedLeafs = new TIntArrayList();

		byte[] state = stateBuilder.initializeState();

		// get a random enabled node
		TIntList enabled = new TIntArrayList(tree.size());
		TIntIterator it;
		do {
			it = stateBuilder.enabledIterator(state);
			while (it.hasNext()) {
				enabled.add(it.next());
			}

			int next = enabled.get(random.nextInt(enabled.size()));
			state = stateBuilder.executeAll(state, next);
			if (next < tree.size() && tree.isLeaf(next) && !tree.isHidden(configurationNumber, next)
					&& tree.getTypeFast(next) != ProbProcessArrayTree.TAU) {
				short act = tree.getTypeFast(next);
				if (random.nextDouble() < noiseLevel) {
					// Noise, so select a new, different activity
					do {
						act = (short) random.nextInt(activities.length);
					} while (act == tree.getTypeFast(next));
				}
				executedLeafs.add(act);
			}
			if (random.nextDouble() < noiseLevel) {
				executedLeafs.add((short) random.nextInt(activities.length));
			}

			enabled.clear();
		} while (!stateBuilder.isFinal(state));

		String[] trace = new String[executedLeafs.size()];
		for (int i = 0; i < executedLeafs.size(); i++) {
			trace[i] = activities[executedLeafs.get(i)];
		}
		return trace;

	}

}
