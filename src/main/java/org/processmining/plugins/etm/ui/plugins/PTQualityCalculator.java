package org.processmining.plugins.etm.ui.plugins;

import java.util.Random;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.impl.XEventImpl;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginCategory;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitness;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.model.narytree.conversion.ProcessTreeToNAryTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMParamFactory;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.AbstractTask.Manual;

public class PTQualityCalculator {

	@Plugin(name = "Evaluate Process Tree, returns String", parameterLabels = { "Event Log", "Process Tree" }, returnLabels = { "Process Tree Quality" }, returnTypes = { String.class }, userAccessible = true, help = "Evaluate Process Tree on common quality dimensions (and metrics)", handlesCancel = true, categories = { PluginCategory.ConformanceChecking }, keywords = {
			"ETM", "Process Tree", "Quality", "Precision", "Replay fitness", "Generalization", "Simplicity" })
	@UITopiaVariant(uiLabel = "Evaluate Process Tree", affiliation = "Eindhoven University of Technology", author = "J.C.A.M.Buijs", email = "j.c.a.m.buijs@tue.nl", pack = "EvolutionaryTreeMiner", uiHelp = "Evaluate Process Tree on common quality dimensions (and metrics)")
	public static String calculatePTQualityString(PluginContext context, XLog eventlog, ProcessTree pt) {
		return calculatePTQualityString(context, eventlog, pt, null);
	}

	@Plugin(name = "Evaluate Process Tree (with provided classifier), returns String", parameterLabels = { "Event Log",
			"Process Tree", "Event Classifier" }, returnLabels = { "Process Tree Quality" }, returnTypes = { String.class }, userAccessible = true, help = "Evaluate Process Tree on common quality dimensions (and metrics)", handlesCancel = true, categories = { PluginCategory.ConformanceChecking }, keywords = {
			"ETM", "Process Tree", "Quality", "Precision", "Replay fitness", "Generalization", "Simplicity" })
	@UITopiaVariant(uiLabel = "Evaluate Process Tree (with classifier)", affiliation = "Eindhoven University of Technology", author = "J.C.A.M.Buijs", email = "j.c.a.m.buijs@tue.nl", pack = "EvolutionaryTreeMiner", uiHelp = "Evaluate Process Tree on common quality dimensions (and metrics)")
	public static String calculatePTQualityString(PluginContext context, XLog eventlog, ProcessTree pt,
			XEventClassifier classifier) {
		return calculatePTQuality(context, eventlog, pt, classifier).toString();
	}

	@Plugin(name = "Evaluate Process Tree", parameterLabels = { "Event Log", "Process Tree" }, returnLabels = { "Process Tree Quality" }, returnTypes = { TreeFitness.class }, userAccessible = true, help = "Evaluate Process Tree on common quality dimensions (and metrics)", handlesCancel = true, categories = { PluginCategory.ConformanceChecking }, keywords = {
			"ETM", "Process Tree", "Quality", "Precision", "Replay fitness", "Generalization", "Simplicity" })
	@UITopiaVariant(uiLabel = "Evaluate Process Tree", affiliation = "Eindhoven University of Technology", author = "J.C.A.M.Buijs", email = "j.c.a.m.buijs@tue.nl", pack = "EvolutionaryTreeMiner", uiHelp = "Evaluate Process Tree on common quality dimensions (and metrics)")
	public static TreeFitness calculatePTQuality(PluginContext context, XLog eventlog, ProcessTree pt) {
		return calculatePTQuality(context, eventlog, pt, null);
	}

	@Plugin(name = "Evaluate Process Tree (with provided classifier)", parameterLabels = { "Event Log", "Process Tree",
			"Event Classifier" }, returnLabels = { "Process Tree Quality" }, returnTypes = { TreeFitness.class }, userAccessible = true, help = "Evaluate Process Tree on common quality dimensions (and metrics)", handlesCancel = true, categories = { PluginCategory.ConformanceChecking }, keywords = {
			"ETM", "Process Tree", "Quality", "Precision", "Replay fitness", "Generalization", "Simplicity" })
	@UITopiaVariant(uiLabel = "Evaluate Process Tree (with classifier)", affiliation = "Eindhoven University of Technology", author = "J.C.A.M.Buijs", email = "j.c.a.m.buijs@tue.nl", pack = "EvolutionaryTreeMiner", uiHelp = "Evaluate Process Tree on common quality dimensions (and metrics)")
	public static TreeFitness calculatePTQuality(PluginContext context, XLog eventlog, ProcessTree pt,
			XEventClassifier classifier) {

		//FIXME more robust way to determine classifier if none is given...
		if (classifier == null) {
			classifier = XLogInfoImpl.STANDARD_CLASSIFIER;
			//			classifier = XLogInfoImpl.NAME_CLASSIFIER;
		}

		CentralRegistry registry = new CentralRegistry(eventlog, new Random());
		registry.updateEventClassifier(classifier);

		//add the event classes of the tree manually
		addAllLeaves(registry.getEventClasses(), pt.getRoot());

		ProcessTreeToNAryTree ptToNat = new ProcessTreeToNAryTree(registry.getEventClasses());
		ProbProcessArrayTree tree = ptToNat.convert(pt);

		//		System.out.println("Tree:" + TreeUtils.toString(tree, registry.getEventClasses()));
		//		System.out.println("valid? " + tree.isConsistent());

		registry.updateLogDerived();

		OverallFitness of = ETMParamFactory.createStandardOverallFitness(registry);
		of.getFitness(tree, null);

		//		System.out.println(registry.getFitness(tree));

		TreeFitness fitness = registry.getFitness(tree);
		return fitness;
	}

	//Method borrowed from org.processmining.plugins.inductiveVisualMiner.alignment.AlignmentETM revision 16927
	public static void addAllLeaves(XEventClasses classes, Node node) {
		if (node instanceof Manual) {
			XEvent event = new XEventImpl();
			XConceptExtension.instance().assignName(event, node.getName());
			classes.register(event);
		} else if (node instanceof Block) {
			for (Node child : ((Block) node).getChildren()) {
				addAllLeaves(classes, child);
			}
		}
		classes.harmonizeIndices();
	}

}
