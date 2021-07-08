package org.processmining.plugins.etm.fitness;

import java.util.HashSet;

import javax.swing.ImageIcon;

import org.processmining.plugins.properties.processmodel.abstractproperty.PropertyDouble;

/**
 * Wrapper around a {@link TreeFitnessInfo} object to be able to use it as a
 * (root!) property of a {@link org.processmining.processtree.ProcessTree}. Note
 * that most fitness values are about the tree as a whole and should therefore
 * only be attached to the root.
 * 
 * @author jbuijs
 * 
 */
public class TreeFitnessPropertyWrapper<F extends TreeFitnessAbstract> extends PropertyDouble {

	private static final long serialVersionUID = 4600983467471288623L;

	private TreeFitnessInfo info;

	public TreeFitnessPropertyWrapper(TreeFitnessInfo info) {
		this.info = info;
	}

	public Long getID() {
		//TODO check if correct
		return (long) info.hashCode();
	}

	public String getName() {
		return info.getName();
	}

	public int compare(Object o1, Object o2) {
		//TODO Check if correct
		return compare(o1, o2);
	}

	public Double getDefaultValue() {
		return -1.0;
	}

	public ImageIcon getIcon() {
		// TODO auto search for images with info code in JAR and return that one :)
		return null;
	}

	public HashSet<Class<?>> getMeaningfulType() {
		HashSet<Class<?>> ret = new HashSet<Class<?>>();
		ret.add(info.getClazz());
		return ret;
	}

	public boolean hasOrdering() {
		return true;
	}

	public boolean higherBetter() {
		return info.isNatural();
	}

	public static <F extends TreeFitnessAbstract> TreeFitnessPropertyWrapper<F> getClazz() {
		return null;
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
			TreeFitnessPropertyWrapper otherMyClass = (TreeFitnessPropertyWrapper) other;
			return info.equals(otherMyClass.info);
		}
	}
}
