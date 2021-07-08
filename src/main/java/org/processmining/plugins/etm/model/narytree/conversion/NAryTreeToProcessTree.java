package org.processmining.plugins.etm.model.narytree.conversion;

import gnu.trove.iterator.TObjectDoubleIterator;

import java.util.ArrayList;
import java.util.List;

import org.deckfour.xes.classification.XEventClasses;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitness;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessPropertyWrapper;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.processtree.Block;
import org.processmining.processtree.Block.PlaceHolder;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.ProcessTree.Type;
import org.processmining.processtree.configuration.controlflow.ControlFlowConfiguration;
import org.processmining.processtree.configuration.controlflow.impl.ControlFlowConfigurationImpl;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.impl.ProcessTreeImpl;

// FIXME Check class contents
// FIXME UNTESTED CODE!
public class NAryTreeToProcessTree {

	/**
	 * Converts the given NAryTree to a ProcessTree without converting the leaf
	 * pointers to event class names
	 * 
	 * @param tree
	 *            NAryTree to convert
	 * @return ProcessTree the processTree equivalent
	 */
	public static ProcessTree convert(ProbProcessArrayTree tree) {
		return convert(tree, null);
	}

	/**
	 * Converts the given NAryTree to a ProcessTree while converting the leaf
	 * pointers to event class names
	 * 
	 * @param classes
	 *            XEventClasses list of event classes
	 * @param tree
	 *            NAryTree to convert
	 * @return ProcessTree the processTree equivalent
	 */
	public static ProcessTree convert(ProbProcessArrayTree tree, XEventClasses classes) {
		return convert(classes, tree, "");
	}

	/**
	 * Converts the given NAryTree to a ProcessTree while converting the leaf
	 * pointers to event class names
	 * 
	 * @param classes
	 *            XEventClasses list of event classes
	 * @param tree
	 *            NAryTree to convert
	 * @param name
	 *            The name of the ProcessTree
	 * @return ProcessTree the processTree equivalent
	 */
	public static ProcessTree convert(XEventClasses classes, ProbProcessArrayTree tree, String name) {
		ProcessTree pt = new ProcessTreeImpl(name);

		pt.setRoot(convertNode(pt, classes, tree, 0));

		return pt;
	}

	/**
	 * Converts the given NAryTree to a ProcessTree while converting the leaf
	 * pointers to event class names and including the provided configurations
	 * of the NAryTree
	 * 
	 * @param classes
	 *            XEventClasses list of event classes
	 * @param tree
	 *            NAryTree to convert
	 * @param name
	 *            The name of the ProcessTree
	 * @return Pair<ProcessTree, ArrayList<Configuration>> The ProcessTree and a
	 *         list of configurations to be applied on this tree
	 */
	public static Pair<ProcessTree, ArrayList<ControlFlowConfiguration>> convertWithConfiguration(XEventClasses classes,
			ProbProcessArrayTree tree, String name) {
		ProcessTree pt = new ProcessTreeImpl(name);

		//Configurations
		ArrayList<ControlFlowConfiguration> configurations = new ArrayList<ControlFlowConfiguration>(tree.size());
		for (int c = 0; c < tree.getNumberOfConfigurations(); c++) {
			configurations.add(new ControlFlowConfigurationImpl());
		}

		//TRANSLATE!
		Node semiRoot = convertNode(pt, classes, tree, 0, configurations);

		pt.setRoot(semiRoot);

		return new Pair<ProcessTree, ArrayList<ControlFlowConfiguration>>(pt, configurations);
	}

	/**
	 * Convert a single node of the given NAryTree to a node in the ProcessTree
	 * 
	 * @param processTree
	 *            ProcessTree to add the converted node to
	 * @param tree
	 *            NAryTree to convert the node from
	 * @param node
	 *            int index of the node to convert (including subtree!)
	 * @return ProcessTree Node of the corresponding NAryTree node.
	 */
	public static Node convertNode(ProcessTree processTree, ProbProcessArrayTree tree, int node) {
		return convertNode(processTree, null, tree, node);
	}

	/**
	 * Convert a single node of the given NAryTree to a node in the ProcessTree,
	 * using the provided list of event classes for the translation
	 * 
	 * @param processTree
	 *            ProcessTree to add the converted node to
	 * @param classes
	 *            XEventClasses used to translate the NAryTree leaf pointers to
	 *            class names
	 * @param tree
	 *            NAryTree to convert the node from
	 * @param node
	 *            int index of the node to convert (including subtree!)
	 * @return ProcessTree Node of the corresponding NAryTree node.
	 */
	public static Node convertNode(ProcessTree processTree, XEventClasses classes, ProbProcessArrayTree tree, int node) {
		return convertNode(processTree, classes, tree, node, null);
	}

	/**
	 * Convert a single node of the given NAryTree to a node in the ProcessTree,
	 * using the provided list of event classes for the translation
	 * 
	 * @param processTree
	 *            ProcessTree to add the converted node to
	 * @param classes
	 *            XEventClasses used to translate the NAryTree leaf pointers to
	 *            class names
	 * @param tree
	 *            NAryTree to convert the node from
	 * @param node
	 *            int index of the node to convert (including subtree!)
	 * @param configurations
	 *            An ArrayList of configurations to be filled with the correct
	 *            settings. This method is tolerant regarding the list, e.g. a
	 *            null instance will not break it and if the size if different
	 *            than the number of configurations for the NAryTree it will be
	 *            handled correctly.
	 * @return ProcessTree Node of the corresponding NAryTree node.
	 */
	public static Node convertNode(ProcessTree processTree, XEventClasses classes, ProbProcessArrayTree tree, int node,
			ArrayList<ControlFlowConfiguration> configurations) {
		return convertNode(processTree, null, classes, tree, node, configurations);
	}

	/**
	 * Convert a single node of the given NAryTree to a node in the ProcessTree,
	 * using the provided list of event classes for the translation
	 * 
	 * @param processTree
	 *            ProcessTree to add the converted node to
	 * @param parentNode
	 *            The node in the ProcessTree that is the parent of the subtree
	 *            to be translated. If NULL then no parent is assumed. The
	 *            parent reference is used to attach the child to the parent and
	 *            correctly set the configurations.
	 * @param classes
	 *            XEventClasses used to translate the NAryTree leaf pointers to
	 *            class names
	 * @param tree
	 *            NAryTree to convert the node from
	 * @param node
	 *            int index of the node to convert (including subtree!)
	 * @param configurations
	 *            An ArrayList of configurations to be filled with the correct
	 *            settings. This method is tolerant regarding the list, e.g. a
	 *            null instance will not break it and if the size if different
	 *            than the number of configurations for the NAryTree it will be
	 *            handled correctly.
	 * @return ProcessTree Node of the corresponding NAryTree node.
	 */
	public static Node convertNode(ProcessTree processTree, Block parentNode, XEventClasses classes, ProbProcessArrayTree tree,
			int node, ArrayList<ControlFlowConfiguration> configurations) {
		//Rewrite all REVSEQ operators
		ProbProcessArrayTree normalizedTree = TreeUtils.rewriteRevSeq(tree);

		Node newNode = null;

		boolean addPlaceholderRoot = false;

		/*
		 * We might need an extra node above since the root in the NAT might be
		 * blocked and because this is put on the egde for PTs we need an edge
		 * and hence a higher (placeholder) node
		 */
		//If we convert the root node and the pt does not have a root yet
		if (parentNode == null) {
			//First test if this is necessary...
			for (int c = 0; c < normalizedTree.getNumberOfConfigurations(); c++) {
				if (normalizedTree.getNodeConfiguration(c, node) == org.processmining.plugins.etm.model.ppt.Configuration.BLOCKED
						|| normalizedTree.getNodeConfiguration(c, node) == org.processmining.plugins.etm.model.ppt.Configuration.HIDDEN
						|| normalizedTree.getNodeConfiguration(c, node) == org.processmining.plugins.etm.model.ppt.Configuration.REVSEQ) {
					addPlaceholderRoot = true;
					break;
				}
			}

			//Then add placeholder root or just the translated tree as root
			if (addPlaceholderRoot) {
				newNode = new AbstractBlock.PlaceHolder("ROOT");
			}
		}

		//Only create a 'real' new node if we did not add a placeholder node
		if (!addPlaceholderRoot) {
			short nodeType = normalizedTree.getType(node);

			switch (nodeType) {
				case ProbProcessArrayTree.REVSEQ :
					//We rewrote the tree to not have reversed sequences :(
					assert false;
					break;
				case ProbProcessArrayTree.SEQ :
					newNode = new AbstractBlock.Seq("SEQ (" + node + ")");
					break;
				case ProbProcessArrayTree.XOR :
					newNode = new AbstractBlock.Xor("XOR (" + node + ")");
					break;
				case ProbProcessArrayTree.OR :
					newNode = new AbstractBlock.Or("OR (" + node + ")");
					break;
				case ProbProcessArrayTree.ILV :
					//FIXME: Interleaving translated to AND
					newNode = new AbstractBlock.And("AND (" + node + ")");
					break;
				case ProbProcessArrayTree.AND :
					newNode = new AbstractBlock.And("AND (" + node + ")");
					break;
				case ProbProcessArrayTree.LOOP :
					newNode = new AbstractBlock.XorLoop("LOOP (" + node + ")");
					break;
				case ProbProcessArrayTree.TAU :
					newNode = new AbstractTask.Automatic("Tau");

					//Allow only 1 TAU instance...
					/*-
					for (Node existingNode : processTree.getNodes()) {
						if (existingNode instanceof AbstractTask.Automatic) {
							if (existingNode.getName().equals("Tau")) {
								return existingNode;
							}
						}
					}/**/

					break;
				default :
					String taskName;

					if (classes == null) {
						taskName = "" + nodeType;
					} else {
						taskName = classes.getByIndex(nodeType).toString();
					}

					//Check if this activity is already present, if so, take that node and connect it!
					/*-
					for (Node existingNode : processTree.getNodes()) {
						if (existingNode.isLeaf()) {
							if (existingNode.getName().equals(taskName)) {
								return existingNode;
							}
						}
					}/**/

					//Otherwise, instantiate a new node and continue
					newNode = new AbstractTask.Manual(taskName);

					break;
			}
		}

		//The new node should be set
		assert (newNode != null);

		newNode.setProcessTree(processTree);
		processTree.addNode(newNode);

		if (parentNode != null) {
			//Create an edge to our parent block
			Edge e = parentNode.addChild(newNode);
			newNode.addIncomingEdge(e);
			processTree.addEdge(e);
		}

		/*
		 * Apply Configurations and configuration options
		 */
		if (normalizedTree.getNumberOfConfigurations() > 0 && configurations != null && !addPlaceholderRoot) {
			//We should think about configurations

			//We cycle through as much configurations as there are in the tree or there are configurations in the list
			int maxNrConfig = Math.min(normalizedTree.getNumberOfConfigurations(), configurations.size());
			for (int c = 0; c < maxNrConfig; c++) {
				//Translate the configuration
				ControlFlowConfiguration configuration = configurations.get(c);

				switch (normalizedTree.getNodeConfiguration(c, node)) {
					case org.processmining.plugins.etm.model.ppt.Configuration.NOTCONFIGURED :
						break;
					case org.processmining.plugins.etm.model.ppt.Configuration.BLOCKED :
						//Make sure that the incoming edge is blockable and blocked for this configuration
						List<Edge> edgesB = newNode.getIncomingEdges();
						if (edgesB.size() == 0 || edgesB.size() > 1) {
							System.out.println("ZERO, OR MORE THAN ONE INCOMING EDGE!");
						} else {
							Edge eB = edgesB.get(0);
							eB.setBlockable(true);
							configuration.addBlockedEdge(eB);
						}

						break;
					case org.processmining.plugins.etm.model.ppt.Configuration.HIDDEN :
						//Make sure that the incoming edge is hidable and hidden for this configuration
						List<Edge> edgesH = newNode.getIncomingEdges();
						if (edgesH.size() == 0 || edgesH.size() > 1) {
							System.out.println("ZERO, OR MORE THAN ONE INCOMING EDGE!");
						} else {
							Edge eH = edgesH.get(0);
							eH.setHideable(true);
							configuration.addHiddenEdge(eH);
						}

						break;
					//DOWNGRADING:
					case org.processmining.plugins.etm.model.ppt.Configuration.SEQ :
						((Block) newNode).setChangeable(true);
						configuration.setChangeableNode(newNode, Type.SEQ);
						break;
					case org.processmining.plugins.etm.model.ppt.Configuration.AND :
						((Block) newNode).setChangeable(true);
						configuration.setChangeableNode(newNode, Type.AND);
						break;
					case org.processmining.plugins.etm.model.ppt.Configuration.XOR :
						((Block) newNode).setChangeable(true);
						configuration.setChangeableNode(newNode, Type.XOR);
						break;
					case org.processmining.plugins.etm.model.ppt.Configuration.REVSEQ :
						/*
						 * Downgrading to a reversed sequence is translated to a
						 * placeholder node under which the 'normal' and
						 * reversed sequences are put.
						 */

						/*
						 * First check if we are the first REVSEQ configuration
						 * for this node, everything is done and finished at the
						 * first encounter of such a configuration
						 */
						boolean areFirst = true;
						for (int revseqConfigCounter = 0; revseqConfigCounter < c; revseqConfigCounter++) {
							if (normalizedTree.getNodeConfiguration(revseqConfigCounter, node) == org.processmining.plugins.etm.model.ppt.Configuration.REVSEQ) {
								areFirst = false;
								break; //break once
							}
						}
						if (!areFirst)
							break; //break for real (don't like goto here, just breaky breaky)

						//Step 1: insert PlaceHolder between us and our parent
						if (parentNode != null) {
							Block ourBlock = (Block) newNode;

							//Remove edge between our parent node and us
							List<Edge> edgesToBeRemoved = new ArrayList<Edge>();
							for (Edge parentEdge : parentNode.getOutgoingEdges()) {
								for (Edge incomingEdge : ourBlock.getIncomingEdges()) {
									//							while (parentEdgeIt.hasNext()) {
									//								Edge parentEdge = parentEdgeIt.next();
									//								while (incomingEdgeIt.hasNext()) {
									//									Edge incomingEdge = incomingEdgeIt.next();
									if (parentEdge.equals(incomingEdge)) {
										edgesToBeRemoved.add(incomingEdge);
									}
								}
							}
							for (Edge edge : edgesToBeRemoved) {
								parentNode.removeOutgoingEdge(edge);
								ourBlock.removeIncomingEdge(edge);
								processTree.removeEdge(edge);
							}

							//Now we need to instantiate the placeholder node and connect it inbetween us and our parent
							PlaceHolder placeholder = new AbstractBlock.PlaceHolder("REVSEQ configuration enabler");

							processTree.addNode(placeholder);
							placeholder.addIncomingEdge(parentNode.addChild(placeholder));
							ourBlock.addIncomingEdge(placeholder.addChild(ourBlock));

							/*
							 * Okay, now thats done, we need to give the
							 * placeholder another child which is the reversed
							 * sequence
							 */

							ProbProcessArrayTree reversedSequenceSubtree = new ProbProcessArrayTreeImpl(normalizedTree);
							reversedSequenceSubtree.setType(node, ProbProcessArrayTree.REVSEQ);
							reversedSequenceSubtree = TreeUtils.rewriteRevSeq(reversedSequenceSubtree);
							//Remove all revseq downgrade options for this node :)
							for (int revseqDowngradeRemovalConfiguration = 0; revseqDowngradeRemovalConfiguration < reversedSequenceSubtree
									.getNumberOfConfigurations(); revseqDowngradeRemovalConfiguration++) {
								if (reversedSequenceSubtree.getNodeConfiguration(revseqDowngradeRemovalConfiguration,
										node) == org.processmining.plugins.etm.model.ppt.Configuration.REVSEQ) {
									reversedSequenceSubtree.setNodeConfiguration(revseqDowngradeRemovalConfiguration,
											node,
											org.processmining.plugins.etm.model.ppt.Configuration.NOTCONFIGURED);
								}
							}
							//And convert the reverted subtree to a ProcessTree part
							Node revertedSubtree = convertNode(processTree, placeholder, classes,
									reversedSequenceSubtree, node, configurations);

							/*
							 * And now, for all configurations, set the correct
							 * replacement (we WON'T be going in this case for
							 * other REVSEQ configurations for this node, we do
							 * it all right here, right now)
							 */
							for (int revseqConfig = 0; revseqConfig < normalizedTree.getNumberOfConfigurations(); revseqConfig++) {
								if (normalizedTree.getNodeConfiguration(revseqConfig, node) == org.processmining.plugins.etm.model.ppt.Configuration.REVSEQ) {
									//Set the replacement of the placeholder node configured to the right child
									configurations.get(revseqConfig).setSelectedReplacement(placeholder,
											revertedSubtree);
								} else if (normalizedTree.getNodeConfiguration(revseqConfig, node) == org.processmining.plugins.etm.model.ppt.Configuration.SEQ
										|| normalizedTree.getNodeConfiguration(revseqConfig, node) == org.processmining.plugins.etm.model.ppt.Configuration.NOTCONFIGURED) {
									//If it is configured to the normal sequence or not configured (since it likely is the normal sequence), we must replace it with our block
									configurations.get(revseqConfig).setSelectedReplacement(placeholder, ourBlock);
								} else {
									//All other configurations need to be applied on the incoming edge of the placeholder
									//FIXME continue, copy all previous configuration settings? don't think so... but applying configurations is hard to make into a function
								}
							}

						}
						//Now our parent is always a placeholder node

				}
			}
		}
		/*
		 * Give blocks their children
		 */
		if (newNode instanceof Block) {

			//Cast the node safely to a block
			Block newBlock = (Block) newNode;

			if (addPlaceholderRoot) {
				//If we just added a placeholder node, we call ourselves but now with the placeholder node as the parent block
				convertNode(processTree, newBlock, classes, normalizedTree, node, configurations);
			} else {
				//Add the children!
				for (int c = 0; c < normalizedTree.nChildren(node); c++) {
					convertNode(processTree, newBlock, classes, normalizedTree,
							normalizedTree.getChildAtIndex(node, c), configurations);
				}
			}
		}

		return newNode;
	}

	public static void addFitnessProperties(ProcessTree processTree, ProbProcessArrayTree naryTree, CentralRegistry registry) {
		TreeFitness fitness = registry.getFitness(naryTree);

		for (TObjectDoubleIterator<TreeFitnessInfo> it = fitness.fitnessValues.iterator(); it.hasNext();) {
			it.advance();
			TreeFitnessPropertyWrapper<TreeFitnessAbstract> wrapper = new TreeFitnessPropertyWrapper<TreeFitnessAbstract>(
					it.key());

			//processTree.getRoot().setIndependentProperty(wrapper.getClass(), 1.);
			//TODO check if it does not have already (this particular version of generic wrapper), because then we need to overwrite :)
			Node root = processTree.getRoot();
			root.getIndependentProperties().put(wrapper, it.value());
		}
	}
}
