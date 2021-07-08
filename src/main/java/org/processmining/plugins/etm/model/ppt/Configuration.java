package org.processmining.plugins.etm.model.ppt;

import java.util.Arrays;

/**
 * A configuration is an array with for each node it's configuration which are
 * public statics of this class. 
 * 
 * @author bfvdonge
 * 
 * (It's a flipping what? -- AB)
 * 
 */
public final class Configuration {
	public static final byte NOTCONFIGURED = 0;
	public static final byte BLOCKED = 1;
	public static final byte HIDDEN = 2;
	//FIXME why different constants than NAryTree operator types????
	public static final byte XOR = ProbProcessArrayTree.XOR;//3;
	public static final byte AND = ProbProcessArrayTree.AND;//4;
	public static final byte SEQ = ProbProcessArrayTree.SEQ;//5;
	public static final byte ILV = ProbProcessArrayTree.ILV;//5;
	public static final byte REVSEQ = ProbProcessArrayTree.REVSEQ;//6;

	public final byte[] conf;

	public Configuration(byte[] conf) {
		this.conf = conf;
	}

	/**
	 * Creates a configuration of given size with only NOTCONFIGURED set.
	 * 
	 * @param size
	 */
	public Configuration(int size) {
		this.conf = new byte[size];
	}

	public Configuration(boolean[] blocked, boolean[] hidden) {
		this.conf = new byte[blocked.length];
		for (int i = 0; i < conf.length; i++) {
			if (blocked[i]) {
				conf[i] = BLOCKED;
			} else if (hidden[i]) {
				conf[i] = HIDDEN;
			}
		}
	}

	public boolean isHidden(int i) {
		return conf[i] == HIDDEN;
	}

	public boolean isBlocked(int i) {
		return conf[i] == BLOCKED;
	}

	/**
	 * Returns TRUE if the node is downgrade to another operator type. The exact
	 * type should be obtained from getOption(node i)
	 * 
	 * @param i
	 * @return
	 */
	public boolean isDowngraded(int i) {
		return (conf[i] == SEQ || conf[i] == REVSEQ || conf[i] == AND || conf[i] == XOR);
	}

	/**
	 * Returns the configuration option set for the provided node. NOTE that the
	 * returned byte does NOT correspond to {@link ProbProcessArrayTree} operator types but
	 * to our own {@link Configuration} types!!!
	 * 
	 * @param i
	 * @return
	 */
	public byte getOption(int i) {
		return conf[i];
	}

	public String toString() {
		if (conf == null)
			return "null";
		int iMax = conf.length - 1;
		if (iMax == -1)
			return "[]";

		StringBuilder b = new StringBuilder();
		b.append('[');
		for (int i = 0;; i++) {
			b.append(optionToString(conf[i]));
			if (i == iMax)
				return b.append(']').toString();
			b.append(", ");
		}
	}

	public static char optionToString(byte b) {
		switch (b) {
			case BLOCKED :
				return 'B';
			case HIDDEN :
				return 'H';
			case XOR :
				return 'x';
			case AND :
				return 'a';
			case SEQ :
				return '>';
			case REVSEQ :
				return '<';
			default :
				return '-';
		}
	}

	public int hashCode() {
		return Arrays.hashCode(conf);
	}

	public boolean equals(Object o) {
		return o instanceof Configuration ? Arrays.equals(conf, ((Configuration) o).conf) : false;
	}
}
