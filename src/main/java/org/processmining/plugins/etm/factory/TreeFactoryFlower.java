package org.processmining.plugins.etm.factory;

import java.util.Random;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;

public class TreeFactoryFlower extends TreeFactoryAbstract {

	public TreeFactoryFlower(CentralRegistry registry) {
		super(registry);
	}

	public static ProbProcessArrayTree createFlower(CentralRegistry registry) {
		//First instantiate a XOR with only the first event class
		ProbProcessArrayTree xor = 
				new ProbProcessArrayTreeImpl(new int[] { 2, 2 }, 
									    new short[] { ProbProcessArrayTree.XOR, 0 }, 
									    new int[] { ProbProcessArrayTree.NONE, 0 },
										new double[] {1.0d, 1.0d} );
		//Then add all others
		for (short i = 1; i < registry.getLogInfo().getEventClasses().size(); i++) {
			xor = xor.addChild(0, xor.nChildren(0), i, Configuration.NOTCONFIGURED);
		}

		//Then instantiate a loop with 3 TAU children
		ProbProcessArrayTree flower = new ProbProcessArrayTreeImpl(new int[] { 4, 2, 3, 4 }, new short[] { ProbProcessArrayTree.LOOP, ProbProcessArrayTree.TAU,
				ProbProcessArrayTree.TAU, ProbProcessArrayTree.TAU }, new int[] { ProbProcessArrayTree.NONE, 0, 0, 0 },
				new double[] {1.0d, 1.0d, 1.0d});

		//Replace the 'do' TAU with this XOR of all event classes
		flower = flower.replace(1, xor, 0);

		return flower;
	}

	public ProbProcessArrayTree generateRandomCandidate(Random rng) {
		return createFlower(registry);
	}
}
