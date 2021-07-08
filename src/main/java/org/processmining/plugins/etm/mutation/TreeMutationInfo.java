package org.processmining.plugins.etm.mutation;

import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;

public class TreeMutationInfo {
	private final String code;
	private final String name;
	private final String description;

	private final boolean isGuided;

	private final Class<?> clazz;

	private final Class<TreeFitnessInfo.Dimension>[] improves;

	private final Class<? extends TreeFitnessAbstract>[] requires;

	/**
	 * Create a new instance of information for a tree mutation operator.
	 * 
	 * @param clazz
	 *            The Class of the mutation operator
	 * @param code
	 *            A two character code for the evaluator that UNIQUELY
	 *            identifies the evaluator
	 * @param name
	 *            A descriptive but short name of a couple of words that can be
	 *            used in GUI, drop down lists, and reports
	 * @param description
	 *            A (longer) description that describes in more detail what the
	 *            evaluator evaluates
	 * @param isGuided
	 *            Whether the mutator operator is 'smart'/guided and uses f.i.
	 *            the event log or alignments to change a process tree or
	 *            whether it is dumb/random and just tries some change.
	 * @param improves
	 *            The dimensions it is likely to improve. NOTE: no guarantee
	 *            needs to be given. This field can be used to select certain
	 *            mutators for a given process tree to improve a certain
	 *            dimension.
	 * @param requires
	 *            A list of specific {@link TreeFitnessAbstract} implementations
	 *            the mutator requires to function. F.i. if alignments are
	 *            reuiqred, the
	 *            {@link org.processmining.plugins.etm.fitness.metrics.FitnessReplay}
	 *            class is required.
	 */
	public TreeMutationInfo(Class<?> clazz, String code, String name, String description, boolean isGuided,
			Class<TreeFitnessInfo.Dimension>[] improves, Class<? extends TreeFitnessAbstract>... requires) {
		this.clazz = clazz;
		this.code = code;
		this.name = name;
		this.description = description;

		this.isGuided = isGuided;

		this.requires = requires;

		this.improves = improves;
	}

	public TreeMutationInfo(TreeMutationInfo original) {
		this(original.clazz, original.code, original.name, original.description, original.isGuided, original.improves,
				original.requires);
	}

	/**
	 * Get the two character code of the fitness metric.
	 * 
	 * @return String 2 character code
	 */
	public String getCode() {
		return this.code;
	}

	/**
	 * The name of the fitness metric. Should be not too long such that is would
	 * show nicely in a dropdown box f.i.
	 * 
	 * @return the name String name of the metric
	 */
	public String getName() {
		return name;
	}

	/**
	 * A brief description of the metric, possibly with pointers to literature.
	 * Used to give the user more information about the specific metric.
	 * 
	 * @return the description String Brief description of the metric
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Returns the class of the fitness metric this information object belongs
	 * to.
	 * 
	 * @return Class
	 */
	public Class<?> getClazz() {
		return clazz;
	}

	/**
	 * Currently implemented to return the code
	 */
	public String toString() {
		return code;
	}

	/**
	 * Returns an array of fitness classes that this mutator depends on.
	 * 
	 * @return the dependsOn
	 */
	public Class<? extends TreeFitnessAbstract>[] getRequires() {
		return requires;
	}

	/**
	 * Returns whether this mutation is guided/smart (TRUE) or random/dumb
	 * (FALSE).
	 * 
	 * @return the isGuided
	 */
	public boolean isGuided() {
		return isGuided;
	}

	/**
	 * The dimensions this mutator is likely to improve. NOTE: no guarantee
	 * needs to be given.
	 * 
	 * @return the improves
	 */
	public Class<TreeFitnessInfo.Dimension>[] getImproves() {
		return improves;
	}

}
