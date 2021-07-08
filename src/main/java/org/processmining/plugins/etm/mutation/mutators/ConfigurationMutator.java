package org.processmining.plugins.etm.mutation.mutators;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

public class ConfigurationMutator extends TreeMutationAbstract {

	@SuppressWarnings("unused")
	private String key = "configurationMutator";

	private ConfigurationMutator(CentralRegistry registry) {
		super(registry);

		if (!(registry instanceof CentralRegistryConfigurable)) {
			throw new IllegalArgumentException("Configurable registry required");
		}
	}

	public ConfigurationMutator(CentralRegistryConfigurable registry) {
		super(registry);
	}

	@SuppressWarnings("unused")
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		CentralRegistryConfigurable reg = (CentralRegistryConfigurable) registry;

		//NAryTree mutatedTree = new NAryTreeImpl(tree);
		//Clone and clean:
		ProbProcessArrayTree mutatedTree = cleanUpConfigurations(tree, reg.getNrLogs());

		//FIXME currently randomly adds a hide/block

		//IDEA: apply 1 mutation, check for all logs, keep if improved (e.g. hierarchy of log configs, whether they share config options).
		//IDEA: crossover between configurations (e.g. between conf. x and y copy the good configuration of x to y)
		//IDEA: use per-log behavior counter to decide which operator can be downgrade / which node blocked/hidden

		//First make sure there are enough configurations
		while (mutatedTree.getNumberOfConfigurations() < reg.getNrLogs()) {
			mutatedTree.addConfiguration(new Configuration(new boolean[mutatedTree.size()], new boolean[mutatedTree
					.size()]));
		}

		int i;
		if (reg.getRandom().nextBoolean() || true) {
			i = randomChange(reg, (ProbProcessArrayTreeImpl) mutatedTree);
		} else {
			//Smart change does not work yet
			i = smartChange(reg, (ProbProcessArrayTreeImpl) mutatedTree);
		}

		//We are sure we changed
		didChange(i, TypesOfTreeChange.OTHER);

		return mutatedTree;
	}

	@SuppressWarnings("static-method")
	private int smartChange(CentralRegistryConfigurable reg, ProbProcessArrayTreeImpl mutatedTree) {
		//First find a spot
		int node = -1;
		@SuppressWarnings("unused")
		int configuration = -1;
		@SuppressWarnings("unused")
		double fraction = 0;

		//Find the configuration for a node that results in relatively the most aSync move : total moves
		for (int n = 0; n < mutatedTree.size(); n++) {
			for (int c = 0; c < mutatedTree.getNumberOfConfigurations(); c++) {
				//TODO finish implementation smart configuration mutation
			}
		}

		//Then improve

		return node;
	}

	/**
	 * Randomly mutates a configuration in the tree. NOTE: applies change
	 * directly on provided tree!!!
	 * 
	 * @param reg
	 * @param mutatedTree
	 * @return
	 */
	private static int randomChange(CentralRegistryConfigurable reg, ProbProcessArrayTreeImpl mutatedTree) {
		//Now select a random one and flip it
		int c = reg.getRandom().nextInt(reg.getNrLogs());
		int i = reg.getRandom().nextInt(mutatedTree.size());

		//Determine the action: 0=hide, 1=block 2=changeType (only for changable operators)
		int action = reg.getRandom().nextInt(getDowngradeOptions(mutatedTree.getType(i)).length > 0 ? 3 : 2);

		if (action == 0) {
			//(un)HIDE
			if (mutatedTree.getNodeConfiguration(c, i) == Configuration.HIDDEN) {
				mutatedTree.setNodeConfiguration(c, i, Configuration.NOTCONFIGURED);
			} else {
				mutatedTree.setNodeConfiguration(c, i, Configuration.HIDDEN);
			}
		} else if (action == 1) {
			//(un)BLOCK
			if (mutatedTree.getNodeConfiguration(c, i) == Configuration.BLOCKED) {
				mutatedTree.setNodeConfiguration(c, i, Configuration.NOTCONFIGURED);
			} else {
				mutatedTree.setNodeConfiguration(c, i, Configuration.BLOCKED);
			}
		} else {
			//(un)CHANGE TYPE
			//Get the options to downgrade the given operator
			byte[] options = getDowngradeOptions(mutatedTree.getType(i));
			//If there are actually options
			if (options.length > 0) {
				byte newType = options[reg.getRandom().nextInt(options.length)];
				if (mutatedTree.getNodeConfiguration(c, i) != newType) {
					//Downgrade to a randomly selected option
					downgrade(mutatedTree, c, i, newType);
				} else {
					//If we guessed the currently used downgrade option we remove the downgrade (to make sure we always change)
					mutatedTree.setNodeConfiguration(c, i, Configuration.NOTCONFIGURED);
				}
			}
		}//change type
		return i;
	}

	/**
	 * Cleans up the configurations of this tree. For instance removes operator
	 * 'downgrades' that are equal (or more) than the operator. We chose to do
	 * this here since there might be many places where this can go wrong and
	 * this is one of the configuration aware locations in the code.
	 * 
	 * @param tree
	 *            NAryTree to be cleaned
	 * @return NAryTree clone with cleaned up configuration options
	 */
	public static ProbProcessArrayTree cleanUpConfigurations(ProbProcessArrayTree tree, int nrConfigurationsRequired) {
		ProbProcessArrayTreeImpl cleanedTree = new ProbProcessArrayTreeImpl(tree);

		for (int n = 0; n < cleanedTree.size(); n++) {
			byte[] validOptions = getDowngradeOptions(cleanedTree.getType(n));
			for (int c = 0; c < cleanedTree.getNumberOfConfigurations(); c++) {
				boolean valid = false;
				for (int o = 0; o < validOptions.length; o++) {
					if (validOptions[o] == cleanedTree.getNodeConfiguration(c, n)) {
						valid = true;
					}
				}
				if (!valid) {
					cleanedTree.setNodeConfiguration(c, n, Configuration.NOTCONFIGURED);
				}
			}
		}

		/*
		 * Now also make sure that the tree has the correct number of
		 * configurations
		 */
		//Add more configurations if necessary
		while (cleanedTree.getNumberOfConfigurations() < nrConfigurationsRequired) {
			cleanedTree.addConfiguration(new Configuration(cleanedTree.size()));
		}

		//remove superfluous configurations
		while (cleanedTree.getNumberOfConfigurations() < nrConfigurationsRequired) {
			cleanedTree.removeConfiguration(cleanedTree.getNumberOfConfigurations() - 1);
		}

		return cleanedTree;
	}

	/**
	 * Returns a list of possible downgrade options for the given operator type
	 * 
	 * @param type
	 * @return
	 */
	public static byte[] getDowngradeOptions(int type) {
		switch (type) {
			case ProbProcessArrayTree.LOOP :
				return new byte[] { Configuration.SEQ };
			case ProbProcessArrayTree.OR :
				return new byte[] { Configuration.AND, Configuration.XOR, Configuration.SEQ, Configuration.REVSEQ, Configuration.ILV  };
			case ProbProcessArrayTree.AND :
				return new byte[] { Configuration.SEQ, Configuration.REVSEQ, Configuration.ILV };
			case ProbProcessArrayTree.ILV :
				return new byte[] { Configuration.SEQ, Configuration.REVSEQ};
			default :
				//No downgrade options
				return new byte[] {};
		}
	}

	public static void downgrade(ProbProcessArrayTreeImpl tree, int configuration, int node, byte toType) {
		//For loops we block the 'redo' part since the idea is that a loop can be reduced to a sequence of do+exit
		if (toType == ProbProcessArrayTree.SEQ && tree.getType(node) == ProbProcessArrayTree.LOOP) {
			tree.setNodeConfiguration(configuration, tree.getChildAtIndex(node, 1), Configuration.BLOCKED);
		}

		tree.setNodeConfiguration(configuration, node, toType);
	}
}
