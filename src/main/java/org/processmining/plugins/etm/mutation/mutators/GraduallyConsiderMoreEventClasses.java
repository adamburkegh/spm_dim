package org.processmining.plugins.etm.mutation.mutators;

import java.util.List;
import java.util.Random;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;

/**
 * This mutation operator tricks the whole ETM by instructing the
 * {@link org.processmining.plugins.etm.CentralRegistry} to not 'show' all event
 * classes. By starting with only a subset, and steadily increasing the number
 * of event classes considered, the trees should be easier to construct.
 * 
 * @author jbuijs
 * 
 */
public class GraduallyConsiderMoreEventClasses implements EvolutionaryOperator<ProbProcessArrayTree> {

	//FIXME broken in central registry <-> AStar (astar finds activity with index -1 since trace contains unknown act)
	
	//FIXME parameterize start and increment sizes
	private final int START_SIZE = 20;

	private final int INCREMENT_SIZE = 10;
	private final int INCREMENT_INTERVAL = 10;

	private int lastIncrementedAtGeneration = 0;

	private CentralRegistry centralRegistry;

	public GraduallyConsiderMoreEventClasses(CentralRegistry centralRegistry) {
		this.centralRegistry = centralRegistry;

		//Instruct the registry to reduce the event class size to our start size
		centralRegistry.considerTopEventClasses(START_SIZE);
	}

	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		doOurThing();

		return tree;
	}

	public List<ProbProcessArrayTree> apply(List<ProbProcessArrayTree> selectedCandidates, Random rng) {
		doOurThing();

		return selectedCandidates;
	}

	private void doOurThing() {
		//Check if the required number of generations has passed and if we did not act during this generation already
		if (centralRegistry.getCurrentGeneration() > 0
				&& centralRegistry.getCurrentGeneration() != lastIncrementedAtGeneration
				&& centralRegistry.getCurrentGeneration() % INCREMENT_INTERVAL == 0) {
			lastIncrementedAtGeneration = centralRegistry.getCurrentGeneration();
			//We should increment!
			if (centralRegistry.getEventClasses().size() != centralRegistry.getLogInfo().getEventClasses().size()) {
				centralRegistry.considerTopEventClasses(centralRegistry.getEventClasses().size() + INCREMENT_SIZE);
				System.out.println("We are now considering " + centralRegistry.getEventClasses().size()
						+ " event classes.");
			}
		}
	}
}
