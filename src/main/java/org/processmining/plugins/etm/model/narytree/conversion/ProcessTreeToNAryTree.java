package org.processmining.plugins.etm.model.narytree.conversion;

import java.util.List;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.impl.XEventImpl;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.processtree.Block;
import org.processmining.processtree.Edge;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.ProcessTree.Type;
import org.processmining.processtree.Task;
import org.processmining.processtree.Task.Automatic;
import org.processmining.processtree.configuration.controlflow.ControlFlowConfiguration;
import org.processmining.processtree.impl.AbstractBlock;
import org.processmining.processtree.impl.AbstractBlock.And;
import org.processmining.processtree.impl.AbstractBlock.Def;
import org.processmining.processtree.impl.AbstractBlock.DefLoop;
import org.processmining.processtree.impl.AbstractBlock.Or;
import org.processmining.processtree.impl.AbstractBlock.PlaceHolder;
import org.processmining.processtree.impl.AbstractBlock.Seq;
import org.processmining.processtree.impl.AbstractBlock.Xor;
import org.processmining.processtree.impl.AbstractBlock.XorLoop;
import org.processmining.processtree.impl.AbstractEvent;
import org.processmining.processtree.impl.AbstractNode;
import org.processmining.processtree.impl.AbstractTask;
import org.processmining.processtree.test.TestProcessTree;

public class ProcessTreeToNAryTree {

	//TODO thoroughly test PT-NATree conversion
	//TODO add method that includes connection to log/XEventClasses and create NAryTree that has that connection

	//Keep a link between task name and int 
	//private ArrayList<String> taskNames;
	private XEventClasses eventClasses;

	/**
	 * Instantiate this class, needed to build the XEventClass list assuming the
	 * STANDARD CLASSIFIER. DO NOT USE this constructor when you actually have
	 * an event log!!! Use the one with the XEventClasses object!!!
	 */
	public ProcessTreeToNAryTree() {
		this(XLogInfoImpl.NAME_CLASSIFIER);
	}

	/**
	 * Instantiate this class, needed to build the XEventClasses object on the
	 * fly, assuming the provided classifier.
	 * 
	 * @param classifier
	 */
	public ProcessTreeToNAryTree(XEventClassifier classifier) {
		eventClasses = new XEventClasses(classifier);
	}

	/**
	 * Instantiate this class and provide it with a list of known event classes
	 * to use for translation
	 * 
	 * @param classes
	 */
	public ProcessTreeToNAryTree(XEventClasses classes) {
		this.eventClasses = classes;
	}

	/**
	 * Convert the provided tree to an NAryTree
	 * 
	 * @param pt
	 *            ProcessTree to translate
	 * @return NAryTree n-ary tree
	 */
	public ProbProcessArrayTree convert(ProcessTree pt) {
		return convert(pt, null);
	}

	/**
	 * Convert the provided ProcessTree to an NAryTree, including the provided
	 * configurations
	 * 
	 * @param pt
	 *            ProcessTree to translated
	 * @param configurations
	 *            List of configurations that are applied ot the NAryTree
	 * @return
	 */
	public ProbProcessArrayTree convert(ProcessTree pt, List<ControlFlowConfiguration> configurations) {
		preprocessEvents(eventClasses, pt.getRoot()); //preprocess event classes
		return convertNode(pt.getRoot(), configurations);
	}

	/**
	 * Convert the provided node to the equivalent NAryTree part
	 * 
	 * @param node
	 *            Process Tree node to convert
	 * @param configurations
	 *            List of configurations to apply on the NAryTree, if null this
	 *            is ignored
	 * @return NAryTreeImpl n-ary tree
	 */
	public ProbProcessArrayTree convertNode(Node node, List<ControlFlowConfiguration> configurations) {
		//Quick return tasks/leafs
		if (node instanceof AbstractTask) {
			ProbProcessArrayTree leafTree = new ProbProcessArrayTreeImpl(1 , convertType(node),
					ProbProcessArrayTree.NONE, 1.0d);
			if (configurations == null) {
				return leafTree;
			} else {
				return applyConfiguration(leafTree, 0, node, configurations);
			}
		}

		ProbProcessArrayTreeImpl tree = null;

		/*
		 * Step 0: detect all XEventClasses not already known and add them (to
		 * prevent trouble with indices later on)
		 */

		/*
		 * Step 1: create a tree with (the) one operator node
		 */
		AbstractBlock block = (AbstractBlock) node;

		//Catch the special cases
		if (block instanceof AbstractEvent) {
			//An event is translated as SEQ(Event, ..... ) so instantiate SEQ(Event)
			tree = new ProbProcessArrayTreeImpl(new int[] { 2, 2 }, new short[] { ProbProcessArrayTree.SEQ, convertType(node) }, new int[] {
					ProbProcessArrayTree.NONE, 0 }, new double[] {1.0,1.0});
		} else {
			//Instantiate tree with only that operator as root
			tree = new ProbProcessArrayTreeImpl( 1 , convertType(node) , ProbProcessArrayTree.NONE, 1.0d);
		}

		/*
		 * Step 2: Add the children (recursively so)
		 */
		//Add children
		for (int i = 0; i < block.numChildren(); i++) {
			Node child = block.getChildren().get(i);
			tree = tree.add(convertNode(child, configurations), 0, 0, Integer.MAX_VALUE);
		}

		return tree;
	}

	/**
	 * Preprocesses the XEventClasses object to contain all XEventClass
	 * innstances in the provided (sub)tree. This is done automatically when a
	 * conversion call is made.
	 * 
	 * @param classes
	 * @param node
	 */
	public static void preprocessEvents(XEventClasses classes, Node node) {
		if (node instanceof Automatic) {
			//NO ACTION taus are not event classes...
		} else if (node instanceof Task) {
			classes.register(node.getName());
		} else if (node instanceof Block) {
			for (Node child : ((Block) node).getChildren()) {
				preprocessEvents(classes, child);
			}
		}
		classes.harmonizeIndices();
	}

	private static ProbProcessArrayTree applyConfiguration(ProbProcessArrayTree nAryTree, int i, Node node,
			List<ControlFlowConfiguration> configurations) {
		//first make sure that we have room to set all the configurations
		while (nAryTree.getNumberOfConfigurations() < configurations.size()) {
			nAryTree.addConfiguration(new org.processmining.plugins.etm.model.ppt.Configuration(nAryTree.size()));
		}

		//Now cycle through the configuration list and transfer all configurations, if there are some set
		for (int c = 0; c < configurations.size(); c++) {
			ControlFlowConfiguration config = configurations.get(c);
			Edge inEdge = node.getIncomingEdges().get(0);

			if (config.isBlocked(inEdge)) {
				nAryTree.setNodeConfiguration(c, i, org.processmining.plugins.etm.model.ppt.Configuration.BLOCKED);
			} else if (config.isHidden(inEdge)) {
				nAryTree.setNodeConfiguration(c, i, org.processmining.plugins.etm.model.ppt.Configuration.HIDDEN);
			} else if (config.getReplacedNodes().containsKey(node.getID())) {
				byte configurationOption = org.processmining.plugins.etm.model.ppt.Configuration.NOTCONFIGURED;
				Type downgradedTo = config.getReplacedNodes().get(node.getID());

				switch (downgradedTo) {
					case AND :
						configurationOption = org.processmining.plugins.etm.model.ppt.Configuration.AND;
						break;
					case XOR :
					case DEF :
						configurationOption = org.processmining.plugins.etm.model.ppt.Configuration.XOR;
						break;
					case SEQ :
						configurationOption = org.processmining.plugins.etm.model.ppt.Configuration.SEQ;
						break;
					case PLACEHOLDER :
					case TIMEOUT :
					case AUTOTASK :
					case LOOPDEF :
					case LOOPXOR :
					case MANTASK :
					case MESSAGE :
					case OR :
						//System.out.println("IMPOSSIBLE DOWNGRADE DETECTED");
						break;
				}

				nAryTree.setNodeConfiguration(c, i, configurationOption);
			}
		}

		return nAryTree;
	}

	/**
	 * Converts the type of the provided node to the NAryTree according short.
	 * 
	 * @param node
	 *            ProcessTree node
	 * @return short NAryTree type
	 */
	private short convertType(Node node) {
		//FIXME: NO ILV Operator in ProcessTree package
		if (node instanceof And) {
			return ProbProcessArrayTree.AND;
		} else if (node instanceof Def || node instanceof Xor || node instanceof PlaceHolder) {
			return ProbProcessArrayTree.XOR;
		} else if (node instanceof Or) {
			return ProbProcessArrayTree.OR;
		} else if (node instanceof Seq) {
			return ProbProcessArrayTree.SEQ;
		} else if (node instanceof DefLoop || node instanceof XorLoop) {
			return ProbProcessArrayTree.LOOP;
		} else if (node instanceof PlaceHolder) {
			return ProbProcessArrayTree.XOR;
		} else {
			//It is a task or event so we need to translate it to a number
			AbstractNode task = (AbstractNode) node;
			String taskname = task.getName();

			if (task instanceof AbstractEvent) {
				taskname = "EVENT: " + taskname;
			}

			/*
			 * NOTE: we NEVER introduce TAU instances! Every task from the
			 * process tree is translated to a task + according XEventClass. The
			 * idea is that when connecting a log those event classes that are
			 * not present in the log are cost free during alignment and will
			 * then become TAUs.
			 */

			if (taskname.equalsIgnoreCase("tau") || taskname.isEmpty()) {
				return ProbProcessArrayTree.TAU;
			}

			if (eventClasses.getByIdentity(taskname) == null) {
				/*-*/
				XEvent event = new XEventImpl();
				//XEventClassifier classifier = eventClasses.getClassifier();
				//keys = eventClasses.getClassifier().getDefiningAttributeKeys();
				XConceptExtension.instance().assignName(event, taskname);
				eventClasses.register(event);
				/**/
			}

			/*-
			//Return the task name ID
			if (!taskNames.contains(taskname)) {
				taskNames.add(taskname);
			}

			return (short) taskNames.indexOf(taskname);
			/**/
			return (short) eventClasses.getByIdentity(taskname).getIndex();
		}
	}

	/**
	 * While converting the ProcessTree to an NAryTree a mapping between the
	 * short reference and event names was build which can be obtained using
	 * this method
	 * 
	 * @return XEventClasses classes mapping
	 */
	public XEventClasses getEventClasses() {
		return eventClasses;
	}

	/**
	 * TEST method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		/*-*/
		// Simple tree
		ProcessTreeToNAryTree simpleUS = new ProcessTreeToNAryTree();

		ProcessTree simpleTree = TestProcessTree.getSimpleTree();
		//NAryTree simpleNAryTree = simpleUS.convert(simpleTree);
		ProbProcessArrayTree simpleNAryTree = simpleUS.convert(simpleTree);
		System.out.println("PT: " + simpleTree.toString());
		System.out.println("NT: " + simpleNAryTree.toString());
		System.out.println(simpleNAryTree.toInternalString());
		System.out.println("Consistent: " + simpleNAryTree.isConsistent());

		//With skips
		/*-*/
		ProcessTreeToNAryTree simpleSkipUS = new ProcessTreeToNAryTree();

		ProcessTree simpleSkipTree = TestProcessTree.getSimpleTreeWithSkips();
		ProbProcessArrayTree simpleSkipNAryTree = simpleSkipUS.convert(simpleSkipTree);
		System.out.println("PT: " + simpleSkipTree.toString());
		System.out.println("NT: " + simpleSkipNAryTree.toString());
		System.out.println(simpleSkipNAryTree.toInternalString());
		System.out.println("Consistent: " + simpleNAryTree.isConsistent());

		//With skips
		/*-*/
		ProcessTreeToNAryTree exampleUS = new ProcessTreeToNAryTree();

		ProcessTree exampleTree = TestProcessTree.getExampleTree();
		ProbProcessArrayTree exampleNAryTree = exampleUS.convert(exampleTree);
		System.out.println("PT: " + exampleTree.toString());
		System.out.println("NT: " + exampleNAryTree.toString());
		System.out.println("Consistent: " + simpleNAryTree.isConsistent());
		System.out.println(exampleNAryTree.toInternalString());
		/**/

	}


}
