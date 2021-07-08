package org.processmining.plugins.etm.fitness;

import java.awt.Component;

import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * Information class with immutable values that describe a fitness metric. This
 * object can be used, together with a fitness value, to correctly interpret its
 * value.
 * 
 * @author jbuijs
 * 
 */
public class TreeFitnessInfo implements Comparable<TreeFitnessInfo> {
	//TODO add property whether result is 'normalized' to a value between 0 and 1 or not?

	private final String code;
	private final String name;
	private final String description;

	private final Dimension dimension;

	private final boolean isNatural;

	private final Class<? extends TreeFitnessAbstract> clazz;

	private final Class<? extends TreeFitnessAbstract>[] dependsOn;

	/**
	 * The dimension that the fitness metric belongs to.
	 * 
	 * @author jbuijs
	 * 
	 */
	public enum Dimension {
		/**
		 * META: a metric that wraps around metrics of ANY other dimension.
		 */
		META,
		/**
		 * FITNESS: How well the process tree is able to replay the event log
		 */
		FITNESS,
		/**
		 * PRECISION: How precise the process tree is w.r.t. the event log
		 */
		PRECISION,
		/**
		 * GENERALIZATION: How likely the process tree is able to capture a new
		 * event log obtained from the same originating process model (e.g.
		 * assuming NO concept drift has taken place).
		 */
		GENERALIZATION,
		/**
		 * SIMPLICITY: How simple the process tree is, for instance for the user
		 * to understand.
		 */
		SIMPLICITY,
		/**
		 * OTHER: Other dimension that says something about the process tree,
		 * possibly in relationship to the event log. Example: similarity or
		 * process performance metrics.
		 */
		OTHER
	};

	/**
	 * Create a new instance of information for a tree fitness evaluator.
	 * 
	 * @param clazz
	 *            The Class of the fitness evaluator
	 * @param code
	 *            A two character code for the evaluator that UNIQUELY
	 *            identifies the evaluator
	 * @param name
	 *            A descriptive but short name of a couple of words that can be
	 *            used in GUI, drop down lists, and reports
	 * @param description
	 *            A (longer) description that describes in more detail what the
	 *            evaluator evaluates
	 * @param dimension
	 *            The {@link Dimension} the evaluator covers
	 * @param isNatural
	 *            Whether the values of the evaluator are natural. E.g. TRUE
	 *            implies bigger values indicate better quality
	 * @param dependsOn
	 *            A list of other evaluator classes the current evaluator
	 *            depends on, e.g. needs, in that order (!) in a constructor.
	 *            This is required for automatic cloning of evaluators for multi
	 *            threading and sanity checks (last is NOT IMPLEMENTED)
	 */
	public TreeFitnessInfo(Class<? extends TreeFitnessAbstract> clazz, String code, String name, String description,
			Dimension dimension, boolean isNatural, Class<? extends TreeFitnessAbstract>... dependsOn) {
		this.clazz = clazz;
		this.code = code;
		this.name = name;
		this.description = description;
		this.dimension = dimension;
		this.isNatural = isNatural;

		/*-
		if(dependsOn == null){
			this.dependsOn = 
		}/**/

		this.dependsOn = dependsOn;
	}

	public TreeFitnessInfo(TreeFitnessInfo original) {
		this(original.clazz, original.code, original.name, original.description, original.dimension,
				original.isNatural, original.dependsOn);
	}

	/*-
	//This constructor is here to prevent warning popping up all over the place that is now suppressed once by:
	@SuppressWarnings("unchecked")
	public TreeFitnessInfo(Class<?> clazz, String code, String name, String description, Dimension dimension,
			boolean isNatural) {
		this(clazz, code, name, description, dimension, isNatural, (Class<? extends TreeFitnessAbstract>) null);
	}/**/

	/**
	 * Get the two character code of the fitness metric.
	 * 
	 * @return String 2 character code
	 */
	public String getCode() {
		return this.code;
	}

	/**
	 * Whether a bigger fitness value is good (TRUE) or bad (FALSE).
	 * 
	 * @return boolean TRUE if bigger values are better, FALSE if smaller is
	 *         better
	 */
	public boolean isNatural() {
		return isNatural;
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
	 * The {@link Dimension} the metric is concerned with.
	 * 
	 * @return the dimension
	 */
	public Dimension getDimension() {
		return dimension;
	}

	/**
	 * Returns the class of the fitness metric this information object belongs
	 * to.
	 * 
	 * @return Class
	 */
	public Class<? extends TreeFitnessAbstract> getClazz() {
		return clazz;
	}

	/**
	 * Currently implemented to return the code
	 */
	public String toString() {
		return code;
	}

	/**
	 * Returns the worst fitness value for this evaluator using the isNatural
	 * boolean.
	 * 
	 * @return 0 if isNatural otherwise Integer.MAX_VALUE
	 */
	public int getWorstFitnessValue() {
		return isNatural ? 0 : Integer.MAX_VALUE;
	}

	/**
	 * Returns an array of class names that this evaluator depends on.
	 * 
	 * @return the dependsOn
	 */
	public Class<? extends TreeFitnessAbstract>[] getDependsOn() {
		return dependsOn;
	}

	/**
	 * A renderer for TreeFitnessInfo objects which uses the original renderer
	 * but then shows the TreeFitnessInfo name in the combobox
	 * 
	 * @author jbuijs
	 * 
	 */
	public static class TreeFitnessInfoComboboxRenderer implements ListCellRenderer {
		//NOTE: please do not parameterize the ListCellRenderer to ListCellRenderer<TreeFitnessInfo>, Hudson does not understand...

		private ListCellRenderer original;

		public TreeFitnessInfoComboboxRenderer(final ListCellRenderer originalRenderer) {
			original = originalRenderer;
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			String newValue = value.toString();

			if (value instanceof TreeFitnessInfo) {
				TreeFitnessInfo tfInfo = (TreeFitnessInfo) value;
				newValue = tfInfo.getName();
				list.setToolTipText(tfInfo.getDescription());
			}

			return original.getListCellRendererComponent(list, newValue, index, isSelected, cellHasFocus);
		}
	}

	/**
	 * Sorts {@link TreeFitnessInfo} instances based on their provided NAME, for
	 * visualization purposes mainly (e.g. nice sorting in combobox lists).
	 */
	public int compareTo(TreeFitnessInfo o) {
		return this.name.compareTo(o.name);
	}

	@Override
	public boolean equals(Object other) {
		if (other == null)
			return false;
		if (other == this)
			return true;
		if (!(other instanceof TreeFitnessPropertyWrapper))
			return false;
		else {
			TreeFitnessInfo otherMyClass = (TreeFitnessInfo) other;
			if (!code.equals(otherMyClass.code))
				return false;
			if (!name.equals(otherMyClass.name))
				return false;
			if (!description.equals(otherMyClass.description))
				return false;
			if (!dimension.equals(otherMyClass.dimension))
				return false;
			if (!clazz.equals(otherMyClass.clazz))
				return false;
			if (!dependsOn.equals(otherMyClass.dependsOn))
				return false;
			if (isNatural != otherMyClass.isNatural)
				return false;
		}
		
		return true;
	}

}