package org.processmining.plugins.etm.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;
import org.processmining.plugins.etm.mutation.mutators.ConfigurationMutator;

public class MutationTests {

	public static void main(String[] args) {
		dontTouchOriginalTest();
	}

	/**
	 * Tests whether the mutation functions don't touch the tree they get
	 */
	private static void dontTouchOriginalTest() {
		int nrTries = 1000;

		XLog log = LogCreator.createLog(new String[][] { { "A", "B", "C", "D", "E", "F" } });
		XLog log2 = LogCreator.createLog(new String[][] { { "A", "B", "C", "D", "E" } });
		CentralRegistry registry = new CentralRegistry(log, new Random());
		CentralRegistryConfigurable registryConf = new CentralRegistryConfigurable(XLogInfoImpl.STANDARD_CLASSIFIER,
				new Random(), log, log2);

		//A list of mutators to test
		List<TreeMutationAbstract> mutators = new ArrayList<TreeMutationAbstract>();
		//mutators.add(new AddNodeRandom(registry));
		//		mutators.add(new MutateSingleNodeRandom(registry));
		//		mutators.add(new RemoveSubtreeRandom(registry));
		//mutators.add(new TreeCrossover(1, new Probability(1.)));
		mutators.add(new ConfigurationMutator(registryConf));

		for (int i = 0; i < nrTries; i++) {
			//Create a random tree
			ProbProcessArrayTree tree = TreeUtils.randomTree(1, .25, 2, 12, registry.getRandom());

			//Create a backup copy
			ProbProcessArrayTree backup = new ProbProcessArrayTreeImpl(tree);

			for (TreeMutationAbstract mut : mutators) {
				//Check if the candidate still equals the backup
				@SuppressWarnings("unused")
				ProbProcessArrayTree mutated = mut.mutate(tree);

				if (!tree.equals(backup)) {
					System.out.println("Found The guilty! Do it again...");
					mutated = mut.mutate(tree);
				}
			}
			System.out.println("Passed round " + i);
		}
	}
}
