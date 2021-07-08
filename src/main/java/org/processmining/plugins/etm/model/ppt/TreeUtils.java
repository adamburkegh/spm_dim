package org.processmining.plugins.etm.model.ppt;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.processmining.framework.connections.ConnectionCannotBeObtained;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.etm.model.narytree.connections.NAryTreeToXEventClassesConnection;
import org.processmining.plugins.etm.model.ppt.StateSpace.Edge;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.TShortList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TShortArrayList;
import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TObjectShortHashMap;

public class TreeUtils {

	public static ProbProcessArrayTree fromString(String s) {
		throw new UnsupportedOperationException();
//		TShortList type = new TShortArrayList();
//		TIntList parent = new TIntArrayList();
//		TIntList last = new TIntArrayList();
//		TObjectShortMap<String> map = new TObjectShortHashMap<String>();
//		IntPointer intP = new IntPointer();
//		char[] chars = s.toCharArray();
//		fromString(chars, intP, map, type, parent, last, ProbProcessTreeImpl.NONE, true);
//		ProbProcessTree tree = new ProbProcessTreeImpl(last, type, parent);
//		configurationsFromString(chars, intP, tree);
//		return tree;
	}

	/**
	 * Converts the configurations string part to configurations of the
	 * tree/nodes
	 * 
	 * @param chars
	 * @param intP
	 * @param tree
	 */
	private static void configurationsFromString(char[] chars, IntPointer p, ProbProcessArrayTree tree) {
		//Find the first '[' sign that starts the configuration section
		while (p.i < chars.length && chars[p.i] != '[') {
			p.i++;
		}

		//Check if it is not "[ ]" or no ['s at all... 
		if (chars.length <= p.i + 1 || (chars[p.i + 1] == ' ' && chars[p.i + 2] == ']')) {
			//the configuration string is only "[ ]" which indicates NO configuration, so add NONE!
			return;
		}

		int n = 0; //current node index
		byte[] conf = new byte[tree.size()];

		//Add a label to be able to break out of the while from the switch statement
		process: while (p.i < chars.length) {
			//Process the different characters we can encounter:
			switch (chars[p.i]) {
				case '-' : //Not configured
					conf[n] = Configuration.NOTCONFIGURED;
					break;
				case 'B' : //Blocked
					conf[n] = Configuration.BLOCKED;
					break;
				case 'H' : //Hidden
					conf[n] = Configuration.HIDDEN;
					break;
				case 'a' : //AND
					conf[n] = Configuration.AND;
					break;
				case 'x' : //XOR
					conf[n] = Configuration.XOR;
					break;
				case '>' : //Sequence
					conf[n] = Configuration.SEQ;
					break;
				case '<' : //Reverted Sequence
					conf[n] = Configuration.REVSEQ;
					break;
				case ',' : //Signal to move to the next node
					n++;
					break;
				case ']' : //Signal that something ends
					if (chars.length > p.i + 2 && (chars[p.i + 1] == ']' || chars[p.i + 2] == ']')) {
						/*
						 * If we encounter two ]'s after each other we're done
						 * with the configurations, so add the current one and
						 * quit
						 */
						tree.addConfiguration(new Configuration(conf));
						break process;
					} else {
						//Otherwise, only the current configuration ends...
						tree.addConfiguration(new Configuration(conf));
						conf = new byte[tree.size()];
						n = 0;
					}
					break;
				case '[' : //Signal that a new configuration starts

				default :
					break;
			}//switch

			//Next character for processing please
			p.i++;

		}
	}

	public static ProbProcessArrayTree fromString(String s, XEventClasses classes) {
		TObjectShortMap<String> map = new TObjectShortHashMap<String>(classes.size());

		for (XEventClass clazz : classes.getClasses()) {
			map.put(clazz.getId(), (short) clazz.getIndex());
		}

		return fromString(s, map);
	}

	public static ProbProcessArrayTree fromString(String s, TObjectShortMap<String> map) {
		throw new UnsupportedOperationException();
//		TShortList type = new TShortArrayList();
//		TIntList parent = new TIntArrayList();
//		TIntList last = new TIntArrayList();
//		IntPointer intP = new IntPointer();
//		char[] chars = s.toCharArray();
//		fromString(chars, intP, map, type, parent, last, ProbProcessTreeImpl.NONE, false);
//		ProbProcessTree tree = new ProbProcessTreeImpl(last, type, parent);
//		configurationsFromString(chars, intP, tree);
//		return tree;
	}

	public static void writeTreeToDot(ProbProcessArrayTree tree, int configurationNumber, OutputStreamWriter out)
			throws IOException {
		writeTreeToDotInternal(tree, configurationNumber, out);
	}

	public static void writeTreeToDot(ProbProcessArrayTree tree, int configurationNumber, OutputStreamWriter out,
			String[] leafLabels) throws IOException {
		writeTreeToDotInternal(tree, configurationNumber, out, leafLabels);
	}

	private static void writeTreeToDotInternal(ProbProcessArrayTree tree, int configurationNumber, OutputStreamWriter out,
			String... leafLabels) throws IOException {
		// write the header.
		out.write("digraph g{\n");

		//		for (int i = 0; i < tree.size(); i++) {
		//			out.write("n" + i + " [label=<(" + i + ")");
		//			if (tree.isHidden(configurationNumber, i)) {
		//				out.write("h");
		//			}
		//			if (tree.isBlocked(configurationNumber, i)) {
		//				out.write("b");
		//			}
		//			out.write("<BR/>");
		//			switch (tree.getType(i)) {
		//				case NAryTree.AND :
		//					out.write("AND");
		//					break;
		//				case NAryTree.OR :
		//					out.write("OR");
		//					break;
		//				case NAryTree.XOR :
		//					out.write("XOR");
		//					break;
		//				case NAryTree.LOOP :
		//					out.write("LOOP");
		//					break;
		//				case NAryTree.SEQ :
		//					out.write("SEQ");
		//					break;
		//				default :
		//					out.write("" + tree.getType(i));
		//					break;
		//			}
		//			out.write(">];\n");
		//		}

		writeChildren(tree, 0, configurationNumber, out, tree.isBlocked(configurationNumber, 0),
				tree.isHidden(configurationNumber, 0), leafLabels);
		out.write("}\n");
	}

	private static void writeChildren(ProbProcessArrayTree tree, int node, int configurationNumber, OutputStreamWriter out,
			boolean blocked, boolean hidden, String[] leafLabels) throws IOException {
		String colorSubTree = "black";
		if (hidden) {
			colorSubTree = "gray";
		}
		if (blocked) {
			colorSubTree = "red";
		}

		out.write("n" + node + " [color=\"" + colorSubTree + "\",label=<(" + node + ")");
		if (tree.isHidden(configurationNumber, node)) {
			out.write("h");
		}
		if (tree.isBlocked(configurationNumber, node)) {
			out.write("b");
		}
		out.write("<BR/>");
		switch (tree.getType(configurationNumber, node)) {
			case ProbProcessArrayTree.AND :
				out.write("AND");
				break;
			case ProbProcessArrayTree.OR :
				out.write("OR");
				break;
			case ProbProcessArrayTree.ILV :
				out.write("ILV");
				break;
			case ProbProcessArrayTree.XOR :
				out.write("XOR");
				break;
			case ProbProcessArrayTree.LOOP :
				out.write("LOOP");
				break;
			case ProbProcessArrayTree.SEQ :
				out.write("SEQ");
				break;
			case ProbProcessArrayTree.REVSEQ :
				out.write("RSQ");
				break;
			default :
				int n = tree.getType(configurationNumber, node);
				if (n >= 0 && n < leafLabels.length) {
					out.write(leafLabels[n]);
				} else if (n == ProbProcessArrayTree.TAU) {
					out.write("TAU");
				} else {
					out.write("" + n);
				}
				break;
		}
		out.write(">];\n");

		int c = 1;
		int child = node + 1;
		while (tree.getParent(child) == node) {
			out.write("n" + node + " -> n" + child + " [color=\"" + colorSubTree + "\",taillabel=" + (c++) + "];\n");
			writeChildren(tree, child, configurationNumber, out, blocked || tree.isBlocked(configurationNumber, child),
					hidden || tree.isHidden(configurationNumber, child), leafLabels);
			child = tree.getNext(child);
		}

	}

	public static String readableSize(long size) {
		if (size <= 0)
			return "0";
		final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}

	public static void writeBehaviorToDot(ProbProcessArrayTree tree, int configurationNumber, OutputStreamWriter out,
			boolean doPushDown) throws IOException {
		StateBuilder builder = new StateBuilder(tree, configurationNumber);
		writeBehaviorToDot(out, doPushDown, builder);
	}

	public static void writeBehaviorToDot(OutputStreamWriter out, boolean doPushDown, StateBuilder builder)
			throws IOException {

		StateSpace statespace = builder.buildStateSpace(doPushDown);
		writeBehaviorToDot(out, statespace, builder);

	}

	public static void writeBehaviorToDot(OutputStreamWriter out, StateSpace statespace, StateBuilder builder)
			throws IOException {

		ProbProcessArrayTree tree = builder.getTree();
		int configurationNumber = builder.getConfigurationNumber();

		out.write("digraph g{\n");

		@SuppressWarnings("unused")
		long start = System.currentTimeMillis();

		@SuppressWarnings("unused")
		long current = System.currentTimeMillis();
		/*-
		System.out.println("Statespace size: " + statespace.size() + //
				" memory:" + TreeUtils.readableSize(statespace.memory()) + //
				" time: " + (current - start) / 1000.0 + //
				" bytes/state: " + (statespace.getBytesPerState()) + //
				" bytes/edge: " + (statespace.getBytesPerEdge()) + //
				" states/sec: " + (statespace.size() / ((current - start) / 1000.0)) //
		);/**/

		for (int i = 0; i < statespace.size(); i++) {
			out.write("N" + i + " [label=<" + i + "<BR/>" + builder.toString(statespace.getState(i)) + ">];\n");
		}

		Iterator<Edge> it = statespace.getEdgeIterator();
		while (it.hasNext()) {
			Edge a = it.next();
			int l = a.getLabel();
			if (l >= tree.size()) {
				l -= tree.size();
			}
			out.write("N" + a.getFrom() + " -> N" + a.getTo() + " [label=\"" + a.getLabel() + "\"," + //
					"color=" + (tree.isHidden(configurationNumber, l) ? "gray" : tree.isLeaf(l) ? "red" : "black") + //
					"];\n");
		}
		out.write("}\n");

	}

	private static void fromString(char[] buf, IntPointer p, TObjectShortMap<String> map, TShortList type,
			TIntList parent, TIntList last, int par, boolean addClassIfUnknown) {

		String val = new String();
		while (buf[p.i] != '(' && buf[p.i] != ':') {
			val += buf[p.i++];
		}
		// read the type
		val = val.trim();
		short t = 0;
		if (val.equals("LEAF")) {
			t = 0;
		} else if (val.equals("AND")) {
			t = ProbProcessArrayTree.AND;
		} else if (val.equals("XOR")) {
			t = ProbProcessArrayTree.XOR;
		} else if (val.equals("OR")) {
			t = ProbProcessArrayTree.OR;
		} else if (val.equals("ILV")) {
			t = ProbProcessArrayTree.ILV;
		} else if (val.equals("LOOP")) {
			t = ProbProcessArrayTree.LOOP;
		} else if (val.equals("SEQ")) {
			t = ProbProcessArrayTree.SEQ;
		} else if (val.equals("REVSEQ")) {
			t = ProbProcessArrayTree.REVSEQ;
		}

		// skip 2 chars
		p.i += 2;
		val = "";
		if (t >= 0) {
			// A leaf!
			while (p.i + 1 < buf.length && buf[p.i + 1] != ',' && buf[p.i + 1] != ')' && buf[p.i + 1] != '[') {
				val += buf[p.i++];
			}

			if (buf.length > p.i + 1) {
				p.i++;
			}
			// we need to add a leaf with class clazz.
			if (val.equalsIgnoreCase("TAU")) {
				type.add(ProbProcessArrayTree.TAU);
			} else if (!map.containsKey(val)) {
				if (addClassIfUnknown) {
					short c = (short) map.size();
					map.put(val, c);
					type.add(c);
				} else {
					try {
						//Try to parse the value as a short, then it is an index already
						type.add(Short.parseShort(val));
					} catch (NumberFormatException e) {
						type.add(ProbProcessArrayTree.TAU);
					}

				}
			} else {
				type.add(map.get(val));
			}

			parent.add(par);
			last.add(parent.size());
			return;
		}

		// we need to add an operator node
		int index = type.size();
		type.add(t);
		parent.add(par);
		last.add(ProbProcessArrayTree.NONE);

		// loop until ")"
		do {
			while (buf[p.i] == ',' || buf[p.i] == ' ') {
				p.i++;
			}
			fromString(buf, p, map, type, parent, last, index, addClassIfUnknown);
		} while (p.i < buf.length && buf[p.i] == ',');

		last.set(index, last.size());
		p.i += 2;
	}

	private static final class IntPointer {
		public int i = 0;
	}

	/**
	 * Returns a human-readable string representation of an NAryTree
	 * 
	 * @param tree
	 * @return
	 */
	public static String toString(ProbProcessArrayTree tree) {
		return toString(tree, 0);
	}

	/**
	 * Returns a human-readable string representation of an NAryTree using the
	 * provided XEventClasses for leafs
	 * 
	 * @param tree
	 * @param classes
	 * @return
	 */
	public static String toString(ProbProcessArrayTree tree, XEventClasses classes) {
		return toString(tree, 0, classes);
	}

	/**
	 * Returns a human-readable string representation of the provided node in an
	 * NAryTree
	 * 
	 * @param tree
	 * @param node
	 *            node to print
	 * @return
	 */
	public static String toString(ProbProcessArrayTree tree, int node) {
		return toString(tree, node, null);
	}

	/**
	 * Returns a human-readable string representation of the provided node in an
	 * NAryTree using the XEventClasses for the leafs
	 * 
	 * @param tree
	 * @param node
	 * @param classes
	 * @return
	 */
	public static String toString(ProbProcessArrayTree tree, int node, XEventClasses classes) {
		StringBuilder b = new StringBuilder();
		int br = 0;
		int endOfNode = tree.getNext(node);
		for (int i = node; i < endOfNode; i++) {
			if (tree.getType(i) >= 0) {
				// leaf
				b.append("LEAF: ");

				if (tree.getType(i) == ProbProcessArrayTree.TAU) {
					b.append("tau");
				} else if (classes != null && classes.getByIndex(tree.getType(i)) != null) {
					b.append(classes.getByIndex(tree.getType(i)));
				} else
					b.append(tree.getType(i));

				// check for closing brackets
				int p = tree.getParent(i);
				if (p == ProbProcessArrayTree.NONE) {
					break;
				}

				while (br > 0 && tree.getNext(p) == i + 1) {
					b.append(" )");
					br--;
					p = tree.getParent(p);
				}
				if (tree.getNext(p) > i + 1) {
					// there are siblings in this subtree of the provided node
					b.append(" , ");
				}
			} else {
				br++;
				b.append(nodeToString(tree, i) + "( ");
			}
		}
		for (int i = 0; i < br; i++) {
			b.append(" )");
		}

		//Now add the configurations
		b.append(" [");
		for (int c = 0; c < tree.getNumberOfConfigurations(); c++) {
			b.append(tree.getConfiguration(c).toString());
		}
		b.append(" ]");

		return b.toString();
	}

	/**
	 * Returns the string representation of THIS NODE ONLY
	 * 
	 * @param tree
	 * @param node
	 * @return
	 */
	public static String nodeToString(ProbProcessArrayTree tree, int node) {
		return nodeToString(tree, node, (XEventClasses) null);
	}

	/**
	 * Returns the string representation of THIS NODE ONLY, if a leaf then
	 * returns the corresponding XEventClass string representation or TAU if
	 * necessary
	 * 
	 * @param tree
	 * @param node
	 * @param classes
	 * @return
	 */
	public static String nodeToString(ProbProcessArrayTree tree, int node, XEventClasses classes) {
		if (tree.getType(node) >= 0) {
			// leaf
			if (tree.getType(node) == ProbProcessArrayTree.TAU) {
				return "LEAF: tau";
			} else if (classes != null && classes.getByIndex(tree.getType(node)) != null) {
				return "LEAF: " + classes.getByIndex(tree.getType(node));
			} else
				return "LEAF: " + tree.getType(node);
		} else if (tree.getType(node) == ProbProcessArrayTree.XOR) {
			return "XOR";
		} else if (tree.getType(node) == ProbProcessArrayTree.OR) {
			return "OR";
		} else if (tree.getType(node) == ProbProcessArrayTree.ILV) {
			return "ILV";
		} else if (tree.getType(node) == ProbProcessArrayTree.AND) {
			return "AND";
		} else if (tree.getType(node) == ProbProcessArrayTree.LOOP) {
			return "LOOP";
		} else if (tree.getType(node) == ProbProcessArrayTree.SEQ) {
			return "SEQ";
		} else if (tree.getType(node) == ProbProcessArrayTree.REVSEQ) {
			return "REVSEQ";
		}
		return "";
	}

	/**
	 * Returns the string representation of THIS NODE ONLY, if a leaf then
	 * returns the corresponding XEventClass string representation or TAU if
	 * necessary
	 * 
	 * @param tree
	 * @param node
	 * @param classes
	 * @return
	 */
	public static String nodeToString(ProbProcessArrayTree tree, int node, String[] leafLabels) {
		if (tree.getType(node) >= 0) {
			// leaf
			if (tree.getType(node) == ProbProcessArrayTree.TAU) {
				return "LEAF: tau";
			} else if (leafLabels != null && tree.getType(node) >= 0 && tree.getType(node) < leafLabels.length) {
				return "LEAF: " + leafLabels[tree.getType(node)];
			} else
				return "LEAF: " + tree.getType(node);
		} else if (tree.getType(node) == ProbProcessArrayTree.XOR) {
			return "XOR";
		} else if (tree.getType(node) == ProbProcessArrayTree.OR) {
			return "OR";
		} else if (tree.getType(node) == ProbProcessArrayTree.ILV) {
			return "ILV";
		} else if (tree.getType(node) == ProbProcessArrayTree.AND) {
			return "AND";
		} else if (tree.getType(node) == ProbProcessArrayTree.LOOP) {
			return "LOOP";
		} else if (tree.getType(node) == ProbProcessArrayTree.SEQ) {
			return "SEQ";
		} else if (tree.getType(node) == ProbProcessArrayTree.REVSEQ) {
			return "REVSEQ";
		}
		return "";
	}

	public static void printTree(ProbProcessArrayTree tree) {
		System.out.println();
		System.out.println(tree.toInternalString());
		System.out.println(tree);
		System.out.println("Consistent: " + tree.isConsistent());
	}

	public static ProbProcessArrayTreeImpl randomTree(int evtClasses, double leafProb, int minNodes, int maxNodes) {
		return randomTree(evtClasses, leafProb, minNodes, maxNodes, new Random());
	}

	public static ProbProcessArrayTreeImpl randomTree(int evtClasses, double leafProb, int minNodes, int maxNodes, Random r) {
		ProbProcessArrayTreeImpl tree;

		do {
			TIntList last = new TIntArrayList();
			TShortList type = new TShortArrayList();
			TIntList parent = new TIntArrayList();
			TDoubleList weights = new TDoubleArrayList();

			last.add(1);
			parent.add(ProbProcessArrayTree.NONE);
			type.add(ProbProcessArrayTree.NONE);
			weights.add(1.0d);
			fillRandom(type, last, parent, weights, 0, r, leafProb, evtClasses, maxNodes);

			tree = new ProbProcessArrayTreeImpl(last, type, parent, weights);
		} while (tree.size() < minNodes);
		return tree;
	}

	private static void fillRandom(TShortList type, TIntList last, TIntList parent, TDoubleList weights, 
			int operator, Random r,
			double leafProb, int evtClasses, int maxNodes) {
		if (r.nextDouble() < leafProb || type.size() > maxNodes) {
			// make leaf
			int act = r.nextInt(evtClasses + 1);
			if (act == evtClasses) {
				act = ProbProcessArrayTree.TAU;
			}
			type.set(operator, (short) act);
			last.set(operator, operator + 1);
			return;
		}
		// max 4 children;
		int children = r.nextInt(4) + 1;
		// add a non-leaf node.
		short t;
		int rnd = r.nextInt(100);
		if (rnd < 5) { // 5
			t = ProbProcessArrayTree.LOOP;
			children = 3;
		} else if (rnd < 35) { //35
			t = ProbProcessArrayTree.OR;
		} else if (rnd < 55) { //35
			t = ProbProcessArrayTree.ILV;
		} else if (rnd < 65) { //65
			t = ProbProcessArrayTree.AND;
		} else if (rnd < 90) { //90
			t = ProbProcessArrayTree.SEQ;
		} else if (rnd < 95) { //95
			t = ProbProcessArrayTree.REVSEQ;
		} else {
			t = ProbProcessArrayTree.XOR;
		}
		// set the type of the operator node
		type.set(operator, t);

		for (int i = 0; i < children; i++) {
			type.add(ProbProcessArrayTree.NONE);
			parent.add(operator);
			last.add(-1);
			weights.add(1.0d);
			fillRandom(type, last, parent, weights, type.size() - 1, r, leafProb, evtClasses, maxNodes);
		}
		last.set(operator, type.size());

	}

	public static void main(String[] args) {
		//		String s = "SEQ( LEAF: A , XOR( LOOP( LEAF: B , LEAF: B , LEAF: C ) , LEAF: D ) )";
		//		NAryTree tree = fromString(s);
		//		printTree(tree);

		ProbProcessArrayTree tree = randomTree(4, .3, 1, 1);
		printTree(tree);
		String s2 = tree.toString();
		ProbProcessArrayTree treeRead = fromString(s2);
		printTree(treeRead);

		Random r = new Random();
		ProbProcessArrayTreeImpl tree0 = randomTree(5, 0.4, 3, 5);
		TreeUtils.printTree(tree0);

		if (tree0.size() > 1) {
			int node1, node2;
			do {
				node1 = r.nextInt(tree0.size() - 1);
				node2 = node1 + 1 + r.nextInt(tree0.size() - node1 - 1);

			} while (tree0.isInSubtree(node1, node2) || (tree0.isLeaf(node1) && tree0.isLeaf(node2)));

			System.out.println("Swapping: " + node1 + " (" + node1 + "-" + (node1 + tree0.size(node1) - 1) + ") and "
					+ node2 + " (" + node2 + "-" + (node2 + tree0.size(node2) - 1) + ").");
			ProbProcessArrayTreeImpl tree1 = tree0.swap(node1, node2);
			TreeUtils.printTree(tree1);

			ProbProcessArrayTreeImpl tree2 = randomTree(5, 0.2, 3, 4);
			TreeUtils.printTree(tree2);

			int p = r.nextInt(tree1.size());
			while (tree1.type[p] >= 0 || tree1.type[p] == ProbProcessArrayTree.LOOP) {
				p = r.nextInt(tree1.size());
			}
			int s = r.nextInt(tree2.size());
			while (tree2.isLeaf(s)) {
				s = r.nextInt(tree2.size());
			}
			int l = r.nextInt(tree1.nChildren(p) + 1);

			System.out.println("Adding node " + s + " from tree 2 at location " + l + " of node " + p);
			ProbProcessArrayTreeImpl tree3 = tree1.add(tree2, s, p, l);

			TreeUtils.printTree(tree3);

			System.out.println("Removing child at location " + l + " of node " + p + " from tree 3");
			ProbProcessArrayTree tree4 = tree3.remove(p, l);
			TreeUtils.printTree(tree4);

			System.out.println("Tree 4 equals tree 1 (after swapping): " + tree4.equals(tree1));

			// node2 was shifted
			int node3 = node2 - (tree0.size(node1) - tree0.size(node2));
			System.out.println("Swapping back:" + node1 + " (" + node1 + "-" + (node1 + tree4.size(node1) - 1)
					+ ") and " + node3 + " (" + node3 + "-" + (node3 + tree4.size(node3) - 1) + ").");
			ProbProcessArrayTree tree5 = tree4.swap(node1, node3);
			TreeUtils.printTree(tree5);

			System.out.println("Tree 5 equals tree 1 (before swapping): " + tree5.equals(tree0));

			// replace 
			System.out.println("Replacing node " + node1 + " of tree 5 by node " + node3 + " of tree 4");
			ProbProcessArrayTree tree6 = tree5.replace(node1, tree4, node3);
			TreeUtils.printTree(tree6);

			System.out.println("Tree 6 equals tree 1 (before swapping): " + tree6.equals(tree0));

		}
	}

	/**
	 * Normalizes a tree by first flattening it and then sorting it
	 * 
	 * @param tree
	 *            NAryTree to normalize
	 * @return NAryTree instance which is the normalized version of tree
	 */
	public static ProbProcessArrayTree normalize(ProbProcessArrayTree tree) {
		return sort(flatten(tree));
	}

	/**
	 * Flattens a tree by allowing non-LOOP nodes to absorp children that are of
	 * the same operator type as they and remove operators that have only 1
	 * child
	 * 
	 * @param tree
	 *            NAryTree to flatten
	 * @return NAryTree instance which is the flattened version of tree
	 */
	public static ProbProcessArrayTree flatten(ProbProcessArrayTree tree) {
		ProbProcessArrayTree flattenedTree = new ProbProcessArrayTreeImpl(tree);

		/*
		 * Then let parents absorb same-type children for each node in the tree,
		 * keep updating since tree changes
		 */
		for (int i = 0; i < flattenedTree.size(); i++) {
			//Only for non-leafs and non-loops

			//First remove nodes that have only one child
			//FIXME TEST
			while (flattenedTree.nChildren(i) == 1) {
				flattenedTree = flattenedTree.replace(i, flattenedTree, i + 1);
			}

			if (!flattenedTree.isLeaf(i) && flattenedTree.getType(i) != ProbProcessArrayTree.LOOP) {
				//FIXME TEST code, OR can be flattened
				//&& flattenedTree.getType(i) != NAryTree.OR) {
				//Loop through the tree, if the child is of the same type as it's parent
				if (flattenedTree.getType(i) == flattenedTree.getType(flattenedTree.getParent(i))) {
					//Add all the direct children of node i to the parent, in the same order at this location
					int parent = flattenedTree.getParent(i);
					//After doing this, we have to check our children, so start one node before the current i
					int restartPoint = i - 1;

					//Find out where node i is in the children's list of its parent
					int nodePos = 0;
					while (flattenedTree.getChildAtIndex(parent, nodePos) != i) {
						nodePos++;
					}

					while (flattenedTree.nChildren(i) > 1) {
						int movedSize = flattenedTree.size(i + 1);
						//Now add the current first child before this node to its parent
						flattenedTree = flattenedTree.move(i, 0, parent, nodePos);
						i = i + movedSize;
						nodePos++;
					}

					//Now move the last child, hence also removing its parent
					flattenedTree = flattenedTree.move(i, 0, parent, nodePos);

					i = restartPoint;
				}
			}
		}
		return flattenedTree;
	}

	/**
	 * Sorts the provided tree. For non loop and sequence operator nodes the
	 * children are sorted by leaf eventclass index (increasing) and then by
	 * operator type and then by size (for equal operators)
	 * 
	 * @param tree
	 *            NAryTree that should be sorted
	 * @return NAryTree copy of the input tree with the nodes sorted
	 */
	public static ProbProcessArrayTree sort(ProbProcessArrayTree tree) {
		ProbProcessArrayTree sortedTree = new ProbProcessArrayTreeImpl(tree);

		for (int i = 0; i < sortedTree.size(); i++) {
			if (!sortedTree.isLeaf(i) && sortedTree.getType(i) != ProbProcessArrayTree.LOOP
					&& sortedTree.getType(i) != ProbProcessArrayTree.SEQ && sortedTree.nChildren(i) > 1) {
				//Bubble sort thingy :)
				boolean swapped = false;
				do {
					swapped = false;
					//j is upper bound for compare
					for (int j = 1; j < sortedTree.nChildren(i); j++) {
						//Compare j and j-1, make sure that biggest gets behind
						int first = sortedTree.getChildAtIndex(i, j - 1);
						int second = sortedTree.getChildAtIndex(i, j);
						short typeFirst = sortedTree.getType(first);
						short typeSecond = sortedTree.getType(second);

						//If both nodes are of the same type, and type is < 0 (e.g. operator), then sort on tree size
						if (typeFirst == typeSecond && typeFirst < 0) {
							typeFirst = (short) sortedTree.size(first);
							typeSecond = (short) sortedTree.size(second);
						}

						//smaller first
						if (typeFirst > typeSecond) {
							sortedTree = sortedTree.swap(second, first);
							swapped = true;
						}
					}
				} while (swapped);
			}
		}
		return sortedTree;
	}

	public static short getRandomOperatorType(Random rng) {
		return getRandomOperatorType(rng, -1);
	}

	/**
	 * Returns a random operator type, less than or equal to the provided
	 * maxType (RevSeq = 5, Loop = 4, OR = 3, AND = 2, XOR = 1 and SEQ = 0).
	 * 
	 * @param rng
	 * @param maxType
	 *            The 'maximum' (inclusive) type allowed to return. If this
	 *            value is less than 0 or bigger than 5, 5 is assumed, e.g. all
	 *            types are allowed.
	 * @return
	 */
	public static short getRandomOperatorType(Random rng, int maxType) {
		if (maxType < 0 || maxType > 6) {
			maxType = 6;
		}

		switch (rng.nextInt(maxType + 1)) {
			case 0 :
				return ProbProcessArrayTree.SEQ;
			case 1 :
				return ProbProcessArrayTree.XOR;
			case 2 :
				return ProbProcessArrayTree.AND;
			case 3 :
				return ProbProcessArrayTree.OR;
			case 4 :
				return ProbProcessArrayTree.ILV;
			case 5 :
				return ProbProcessArrayTree.LOOP;
			case 6 :
				return ProbProcessArrayTree.REVSEQ;
			default :
				assert false;
				return ProbProcessArrayTree.NONE; //NEVER REACH HERE
		}
	}

	/**
	 * Convenience method to ask the context for a connection between the given
	 * tree and the list of XEventClasses required to correctly interpret the
	 * leafs. Returns NULL if no connection can be found.
	 * 
	 * @param context
	 *            PluginContext to search within
	 * @param tree
	 *            NAryTree that should participate in the connection
	 * @return XEventClasses the list of event classes or NULL if none could be
	 *         found
	 */
	public static XEventClasses getXEventClassesFromConnection(PluginContext context, ProbProcessArrayTree tree) {
		XEventClasses classes = null;
		Collection<NAryTreeToXEventClassesConnection> connections;
		try {
			connections = context.getConnectionManager().getConnections(NAryTreeToXEventClassesConnection.class,
					context, tree);

			for (NAryTreeToXEventClassesConnection connection : connections) {
				if (connection.getObjectWithRole(NAryTreeToXEventClassesConnection.NARYTREE).equals(tree)) {
					classes = connection.getObjectWithRole(NAryTreeToXEventClassesConnection.XEVENTCLASSES);
				}
			}
		} catch (ConnectionCannotBeObtained e) {
			//e.printStackTrace();
		}

		return classes;
	}

	/**
	 * Convenience method to produce a shortmap required for the fromString
	 * method
	 * 
	 * @param classes
	 * @return
	 */
	public static TObjectShortMap<String> xEventClassesToShortmap(XEventClasses classes) {
		TObjectShortMap<String> map = new TObjectShortHashMap<String>(classes.size());

		for (XEventClass clazz : classes.getClasses()) {
			map.put(clazz.getId(), (short) clazz.getIndex());
		}

		return map;
	}

	/**
	 * Rewrites all REVSEQ operators nodes to regular SEQ nodes (also updates
	 * configurations).
	 * 
	 * @param tree
	 *            The tree with REVSEQs that should be rewritten
	 * @return NAryTree (new instance) without REVSEQ occurrences.
	 */
	public static ProbProcessArrayTree rewriteRevSeq(ProbProcessArrayTree tree) {
		ProbProcessArrayTree newTree = new ProbProcessArrayTreeImpl(tree);
		for (int node = 0; node < newTree.size(); node++) {
			if (newTree.getType(node) == ProbProcessArrayTree.REVSEQ) {
				//Reverse-order the direct children of this node
				int nChildren = newTree.nChildren(node);
				for (int newChildIndex = 0; newChildIndex < nChildren; newChildIndex++) {
					newTree = newTree.move(node, nChildren - 1, node, newChildIndex);
				}
				newTree.setType(node, ProbProcessArrayTree.SEQ);
			}
		}
		return newTree;
	}

	/**
	 * Returns the number of nodes that have a configuration option set for the
	 * provided configuration
	 * 
	 * @param tree
	 * @param configuration
	 * @return The number of nodes configured for the provided configuration, or
	 *         -1 if there was an error
	 */
	public static int getNumberOfNodesConfiguredForConfiguration(ProbProcessArrayTree tree, int configuration) {
		if (tree.getNumberOfConfigurations() < configuration) {
			return -1;
		}

		Configuration config = tree.getConfiguration(configuration);
		int count = 0;
		for (int n = 0; n < tree.size(); n++) {
			if (config.conf[n] != Configuration.NOTCONFIGURED) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Returns the number of nodes that have a configuration option set any of
	 * the configurations
	 * 
	 * @param tree
	 * @return The number of nodes configured for the provided configuration, or
	 *         -1 if there was an error
	 */
	public static int getNumberOfNodesConfigured(ProbProcessArrayTree tree) {
		int count = 0;
		for (int n = 0; n < tree.size(); n++) {
			for (int c = 0; c < tree.getNumberOfConfigurations(); c++) {
				if (tree.getConfiguration(c).conf[n] != Configuration.NOTCONFIGURED) {
					count++;
					break; // we only need it for one of the configurations
				}
			}
		}

		return count;
	}
}
