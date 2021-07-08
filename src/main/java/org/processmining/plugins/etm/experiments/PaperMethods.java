package org.processmining.plugins.etm.experiments;

import gnu.trove.map.hash.TObjectDoubleHashMap;

import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;

import org.deckfour.xes.classification.XEventClasses;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.fitness.metrics.Generalization;
import org.processmining.plugins.etm.fitness.metrics.PrecisionEscEdges;
import org.processmining.plugins.etm.fitness.metrics.SimplicityMixed;
import org.processmining.plugins.etm.fitness.metrics.SimplicityUselessNodes;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.ptml.importing.PtmlImportTree;

public class PaperMethods {

	public static String processTreeToLatex(ProcessTree tree) {
		String latex = "\\begin{tikzpicture}[small] \n \\Tree";

		latex += nodeToLatex(tree.getRoot());

		//Empty fitness table
		latex += "\n \\node[above=of n0, anchor=south, outer ysep=-18pt]{\\begin{tabular}{|cc|cc|} \\hline f: &  & p: & \\\\ \\hline s: &  & g: & \\\\ \\hline \\end{tabular}};";

		latex += "\n \\end{tikzpicture}";

		//		System.out.println("LATEX: \n" + latex);

		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable transferable = new StringSelection(latex);
			clipboard.setContents(transferable, null);
		} catch (HeadlessException he) {
			//do nothing if headless...
		}

		return latex;

	}

	private static String nodeToLatex(Node node) {
		ProcessTree tree = node.getProcessTree();

		String latex = "[.";
		//leafs are not special nodes, the rest is (because of label placement)

		boolean isSpecialNode = !node.isLeaf();

		if (isSpecialNode) {
			latex += "\\node[label={above:";
		} else {
			latex += "\\node[label={below:";
		}

		switch (tree.getType(node)) {
			case DEF :
			case XOR :
				latex += "\\opXor ";
				break;
			case SEQ :
				latex += "\\opSeq ";
				break;
			case AND :
				latex += "\\opAnd ";
				break;
			case OR :
				latex += "\\opOr ";
				break;
			case LOOPDEF :
			case LOOPXOR :
				latex += "\\opLoop ";
				break;
			case PLACEHOLDER :
				latex += " ";
				break;
			case AUTOTASK :
			case MANTASK :
				latex += node.getName().replace("+complete", "");
				break;
			case MESSAGE :
				break;
			case TIMEOUT :
				break;
			default :
				//DEFAULT means we don't know how to do this...
				latex += node.getName();
				break;
		}

		//Now configuration options
		//TODO implement configuration options to latex

		//Close special commands correctly
		//		if (isSpecialNode) {
		latex += "}]";

		//name the root node
		if (node.isRoot()) {
			latex += "(root)";
		}

		latex += "{}; ";
		//		}

		if (node instanceof Block) {
			Block block = (Block) node;
			for (Edge edge : block.getOutgoingEdges()) {
				latex += nodeToLatex(edge.getTarget());
			}
		}

		latex += " ] ";
		return latex;
	}

	public static String naryTreeToLatex(ProbProcessArrayTree inputTree, XEventClasses eventClasses) {
		return naryTreeToLatex(inputTree, eventClasses, null);
	}

	public static String naryTreeToLatex(ProbProcessArrayTree inputTree, XEventClasses eventClasses, CentralRegistry registry) {
		boolean createForest = inputTree.size() > 20;
		createForest = true;
		ProbProcessArrayTree tree = TreeUtils.rewriteRevSeq(inputTree);
		tree = inputTree;

		String fitnesString = registry != null ? registry.getFitness(tree).toString() : "";
		String latex = String.format("\\begin{%s} %n \t %% %s %s %n \t ", createForest ? "forest" : "tikzpicture",
				TreeUtils.toString(tree, eventClasses), fitnesString);

		latex += createForest ? nodeToLatexForest(tree, 0, eventClasses) : "\\Tree "
				+ nodeToLatex(tree, 0, eventClasses);

		if (registry != null && registry.isFitnessKnown(tree)) {

			if (registry instanceof CentralRegistryConfigurable) {
				CentralRegistryConfigurable registryConf = (CentralRegistryConfigurable) registry;
				//Configurable mode!
				TObjectDoubleHashMap<TreeFitnessInfo> fitnessMap = registry.getFitness(tree).fitnessValues;
				latex += String
						.format("%n\t \\node[below = of n0](tabF) {\\qualityTableCommands\\begin{tabular}{|c|c|c|c|c|c|} \\hline & Overall & Fitness & Precision & Simplicity & Generalization \\\\ \\hline");
				//Combined:
				latex += String.format(
						"%n\t Combined  & %2.3f & %2.3f & %2.3f & %2.3f & %2.3f \\\\ \\hline",
						registry.getOverallFitness(tree),
						fitnessMap.get(FitnessReplay.info),
						fitnessMap.get(PrecisionEscEdges.info),
						fitnessMap.contains(SimplicityMixed.info) ? fitnessMap.get(SimplicityMixed.info) : fitnessMap
								.get(SimplicityUselessNodes.info), fitnessMap.get(Generalization.info));

				for (int c = 0; c < registryConf.getNrLogs(); c++) {
					ProbProcessArrayTree configuredTree = tree.applyConfiguration(c);
					TObjectDoubleHashMap<TreeFitnessInfo> fitnessMapVariant = registryConf.getRegistry(c).getFitness(
							configuredTree).fitnessValues;
					latex += String.format("%n\t Variant " + c
							+ " & %2.3f & %2.3f & %2.3f & %2.3f & %2.3f \\\\ \\hline", registryConf.getRegistry(c)
							.getOverallFitness(configuredTree), fitnessMapVariant.get(FitnessReplay.info),
							fitnessMapVariant.get(PrecisionEscEdges.info), fitnessMapVariant
									.contains(SimplicityMixed.info) ? fitnessMapVariant.get(SimplicityMixed.info)
									: fitnessMapVariant.get(SimplicityUselessNodes.info), fitnessMapVariant
									.get(Generalization.info));
				}

				latex += String.format("%n\t \\end{tabular}};");

				/*-
				\node[below = of n9conf](tabF) {\begin{tabular}{|c|c|c|c|c|c|} \hline
				& Overall & Fitness & Precision & Simplicity & Generalization \\ \hline
				Combined & 0.975 & 0.965 & 1.000 & 0.995 & 0.608 \\ \hline
				Variant 1 & 0.956 & 0.933 & 1.000 & 0.995 & 0.674 \\ \hline
				Variant 2 & 0.998 & 1.000 & 1.000 & 0.995 & 0.666 \\ \hline
				Variant 3 & 0.997 & 1.000 & 1.000 & 0.995 & 0.543 \\ \hline
				Variant 4 & 0.938 & 0.905 & 1.000 & 0.995 & 0.629\\ \hline \end{tabular}};
				/**/
			} else {
				//Single fitness table
				TObjectDoubleHashMap<TreeFitnessInfo> fitnessMap = registry.getFitness(tree).fitnessValues;
				//FIXME make fitness metrics extraction modular (and sorted somehow)
				latex += String
						.format("%n\t \\node[above=of n0, anchor=south, outer ysep=-18pt]{\\qualityTableCommands \n \\begin{tabular}{|cc|cc|} \\hline f: & %2.3f & p: & %2.3f\\\\ \\hline s: & %2.3f & g: & %2.3f\\\\ \\hline \\end{tabular}};",
								fitnessMap.get(FitnessReplay.info), fitnessMap.get(PrecisionEscEdges.info), fitnessMap
										.contains(SimplicityMixed.info) ? fitnessMap.get(SimplicityMixed.info)
										: fitnessMap.get(SimplicityUselessNodes.info), fitnessMap
										.get(Generalization.info));
			}
		} else {
			//Empty fitness table
			latex += String
					.format("%n\t %% \\node[above=of n0, anchor=south, outer ysep=-18pt]{\\begin{tabular}{|cc|cc|} \\hline f: &  & p: & \\\\ \\hline s: &  & g: & \\\\ \\hline \\end{tabular}};");
		}

		//Now for configuration options (for the win!)
		for (int i = 0; i < tree.size(); i++) {
			//Build the configuration text but remember if at least one was set
			boolean hasConfig = false;
			String configuration = "[";
			for (int c = 0; c < tree.getNumberOfConfigurations(); c++) {
				byte thisConfig = tree.getNodeConfiguration(c, i);
				if (thisConfig != Configuration.NOTCONFIGURED) {
					hasConfig = true;
				}

				//Parse configuration option to LaTeX string
				switch (thisConfig) {
					case Configuration.AND :
						configuration += "\\opAnd";
						break;
					case Configuration.BLOCKED :
						configuration += "B";
						break;
					case Configuration.HIDDEN :
						configuration += "H";
						break;
					case Configuration.NOTCONFIGURED :
						configuration += "-";
						break;
					case Configuration.REVSEQ :
						configuration += "\\opSeqRev";
						break;
					case Configuration.SEQ :
						configuration += "\\opSeq";
						break;
					case Configuration.XOR :
						configuration += "\\opXor";
						break;
					default :
						configuration += "???";
				}
				configuration += ",";

				//configuration += Configuration.optionToString(thisConfig) + ",";
			}
			//Now if there has been a configuration, add the configuration to the tikz picture
			if (hasConfig) {
				//remove last ',' and add ']'
				configuration = configuration.substring(0, configuration.length() - 1) + "]";
				latex += String.format("%n\t \\node[configuration={(n" + i + ".south)}, left=of n" + i + "] {"
						+ configuration + "};");
			}
		}

		latex += String.format("%n \\end{%s}", createForest ? "forest" : "tikzpicture");

		//		System.out.println("LATEX: \n" + latex);
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable transferable = new StringSelection(latex);
			clipboard.setContents(transferable, null);
		} catch (HeadlessException eh) {
			//If headless do nothing
		} catch (IllegalStateException ste) {
			//if clipboard is not available
			System.err.println("Clipboard not available, did NOT put the message on the clipboard");
		}

		return latex;

	}

	private static String nodeToLatex(ProbProcessArrayTree tree, int node, XEventClasses classes) {
		//First get the string representation for this node
		String nodeString = "";
		switch (tree.getType(node)) {
			case ProbProcessArrayTree.XOR :
				nodeString = "\\opXor";
				break;
			case ProbProcessArrayTree.SEQ :
				nodeString = "\\opSeq";
				break;
			case ProbProcessArrayTree.AND :
				nodeString = "\\opAnd";
				break;
			case ProbProcessArrayTree.OR :
				nodeString = "\\opOr";
				break;
			case ProbProcessArrayTree.ILV :
				nodeString = "\\opIlv";
				break;
			case ProbProcessArrayTree.LOOP :
				nodeString = "\\opLoop";
				break;
			case ProbProcessArrayTree.REVSEQ :
				nodeString = "\\opSeqRev";
				break;
			default :
				//DEFAULT means LEAF
				if (tree.getType(node) == ProbProcessArrayTree.TAU) {
					nodeString = "$\\tau$";
				} else {
					/*-* /
					if (classes != null) {
						nodeString= PRIVATETranslations.translateEventClasses(classes.getByIndex(tree.getType(node))
								.toString());
					} else {/**/
					nodeString = classes.getByIndex(tree.getType(node)).toString().replace("+complete", "")
							.replace("_", "\\_");
					//We do not want A, B but a, b
					if (nodeString.length() == 1) {
						nodeString = nodeString.toLowerCase();
					}
					//					}
				}
				break;
		}

		/*
		 * Now construct the latex string
		 */
		String latex = "";
		if (!tree.isLeaf(node)) {
			latex += "[.";
		}

		latex += "\\node(n" + node + "){" + nodeString + "}; ";

		//Add the children to it
		for (int c = 0; c < tree.nChildren(node); c++) {
			latex += nodeToLatex(tree, tree.getChildAtIndex(node, c), classes);
		}

		//Close it up
		if (!tree.isLeaf(node)) {
			latex += " ] ";
		}

		return latex;
	}

	public static String nodeToLatexForest(ProbProcessArrayTree tree, int node, XEventClasses classes) {
		//First get the string representation for this node
		String nodeString = "";
		switch (tree.getType(node)) {
			case ProbProcessArrayTree.XOR :
				nodeString = "\\opXor";
				break;
			case ProbProcessArrayTree.SEQ :
				nodeString = "\\opSeq";
				break;
			case ProbProcessArrayTree.AND :
				nodeString = "\\opAnd";
				break;
			case ProbProcessArrayTree.OR :
				nodeString = "\\opOr";
				break;
			case ProbProcessArrayTree.ILV :
				nodeString = "\\opIlv";
				break;
			case ProbProcessArrayTree.LOOP :
				nodeString = "\\opLoop";
				break;
			case ProbProcessArrayTree.REVSEQ :
				nodeString = "\\opSeqRev";
				break;
			default :
				//DEFAULT means LEAF
				if (tree.getType(node) == ProbProcessArrayTree.TAU) {
					nodeString = "$\\tau$";
				} else {
					/*-* /
					if (classes != null) {
						nodeString= PRIVATETranslations.translateEventClasses(classes.getByIndex(tree.getType(node))
								.toString());
					} else {/**/
					nodeString = classes.getByIndex(tree.getType(node)).toString().replace("+complete", "")
							.replace("_", "\\_");
					//We do not want A, B but a, b
					if (nodeString.length() == 1) {
						nodeString = nodeString.toLowerCase();
					}
					//					}
				}
				break;
		}

		/*
		 * Now construct the latex string
		 */
		String latex = "[";

		latex += String.format("%s,name=n%d ", nodeString, node);
		//latex += "\\node(n" + node + "){" + nodeString + "}; ";

		//Add the children to it
		for (int c = 0; c < tree.nChildren(node); c++) {
			latex += nodeToLatexForest(tree, tree.getChildAtIndex(node, c), classes);
		}

		//Close it up
		latex += "] ";

		return latex;
	}

	/**
	 * Produces for each .ptml file in a directory a corresponding BPMN PNG file
	 */
	public static void folderProcessTreeToBPMNPngs(String directory) {
		File dir = new File(directory);
		final String extension = ".ptml";

		PtmlImportTree importer = new PtmlImportTree();
		if (dir.exists() && dir.isDirectory()) {
			for (File file : dir.listFiles()) {
				if (file.getName().endsWith(extension)) {
					try {
						ProcessTree pt = (ProcessTree) importer.importFile(null, file);
						String filename = file.getName();
						filename = filename.substring(0, filename.lastIndexOf('.'));
						CommandLineInterface.treeToBPMN(pt, new File(dir + "/" + filename + ".png"));
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
}
