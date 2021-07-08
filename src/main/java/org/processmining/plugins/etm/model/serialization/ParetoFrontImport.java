package org.processmining.plugins.etm.model.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.contexts.uitopia.annotations.UIImportPlugin;
import org.processmining.framework.abstractplugins.AbstractImportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.FitnessRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

@Plugin(
		name = "Import PT Pareto front from Pareto file",
			parameterLabels = { "Filename" },
			returnLabels = { "Pareto Front" },
			returnTypes = { ParetoFront.class })
@UIImportPlugin(description = "PT Pareto Front", extensions = { "PTPareto" })
public class ParetoFrontImport extends AbstractImportPlugin {

	/**
	 * Gets the file filter for this import plug-in.
	 * 
	 * @return
	 */
	protected FileFilter getFileFilter() {
		return new FileNameExtensionFilter("PT Pareto Front", "PTPareto");
	}

	@PluginVariant(variantLabel = "PT Pareto Front", requiredParameterLabels = { 0, 1, 2 })
	protected Object importFromStream(PluginContext context, InputStream input, String filename, long fileSizeInBytes)
			throws IOException {
		FitnessRegistry fitnessRegistry = new FitnessRegistry(context);
		TreeFitnessInfo[] fitnessMetrics = fitnessRegistry.getAllMetricInfosAsArray();

		return importParetoFront(input, fitnessMetrics);
	}

	/**
	 * Imports a pareto front without needing a context, in return, we need a
	 * list of known fitness metrics...
	 * 
	 * @param input
	 * @param filename
	 * @param fitnessMetrics
	 * @return
	 */
	public ParetoFront importParetoFront(InputStream input, TreeFitnessInfo[] fitnessMetrics) throws IOException {
		Scanner s = new Scanner(input);
		s.useDelimiter("\r\n");

		//First line of the file contains the event classifier and between '(' ')' the list of event classes present in our trees
		String eventClassLine = s.next();

		//The very first line COULD contain 'generation X' in case of a .log file, let's be resilient to this!
		if (eventClassLine.startsWith("Generation: ")) {
			if (s.hasNext()) {
				s.next();
			} else {
				/*
				 * The file consists only of the generation line, hence the
				 * pareto front is empty... The proper way would be to write the
				 * quality dimensions anyway in the exporter, such that we can
				 * properly instantiate a ParetoFront, albeit empty. Now the
				 * best thing we can do is return a Pareto front with the
				 * 'standard' quality dimensions...
				 */
				return null;
			}

		}

		String[] eventClassStrings = eventClassLine.split(ParetoFrontExport.CLASSDIVIDERSTRING);

		//Now do something tricky: create an event log, add a classifier and then extract the event classes.
		XLog dummyLog = LogCreator.createLog(new String[][] { eventClassStrings });
		XLogInfo info = XLogInfoImpl.create(dummyLog, new XEventNameClassifier());
		XEventClasses eventClasses = info.getEventClasses();

		//Second line contains the considered dimensions
		CentralRegistry registry = new CentralRegistry(dummyLog, XLogInfoImpl.NAME_CLASSIFIER, new Random());
		List<TreeFitnessInfo> dimensions = new ArrayList<TreeFitnessInfo>();

		String dimensionLine = s.next();
		//The part we're looking for is ([xx,xx....,xx]), we only want the xx parts so therefore +2 and -1
		String dimensionString = dimensionLine
				.substring(dimensionLine.indexOf('(') + 2, dimensionLine.indexOf(')') - 1);
		String[] dimensionCodes = dimensionString.split(",");

		for (String dimCode : dimensionCodes) {
			dimCode = dimCode.trim();
			TreeFitnessInfo fitnessInfo = FitnessRegistry.getTreeFitnessInfoByCode(dimCode, fitnessMetrics);
			if (fitnessInfo != null) {
				dimensions.add(fitnessInfo);
			} else {
				System.out.println(String.format(
						"We don't know the fitness metric with code '%s', skipping this dimension", dimCode));
			}
		}

		ParetoFront front = new ParetoFront(registry, dimensions);

		//		registry.increaseFitnessCache(2000);

		//Now each next line contains the different fitness dimensions followed by the tree string
		//So first, instantiate all trees and add to the collection including their fitness values
		HashSet<ProbProcessArrayTree> trees = new HashSet<ProbProcessArrayTree>();
		while (s.hasNext()) {
			String treeString = s.next();
			//First the fitness values between [ and ] separated with ,
			try {
				String[] fitness = treeString.substring(treeString.indexOf('[') + 1, treeString.indexOf(']'))
						.split(",");
				//The remainder is the tree to string
				String realTreeString = treeString.substring(treeString.indexOf(']') + 1, treeString.length());

				if (!realTreeString.isEmpty()) {
					try {
						ProbProcessArrayTree tree = TreeUtils.fromString(realTreeString, eventClasses);
						for (String f : fitness) {
							if (!f.isEmpty() && f.contains(":")) {
								String[] localF = f.split(":");
								String code = localF[0].trim();
								Double value = Double.valueOf(localF[1].trim());
								TreeFitnessInfo treeFitnessInfo = FitnessRegistry.getTreeFitnessInfoByCode(code,
										fitnessMetrics);
								if (treeFitnessInfo != null) {
									registry.getFitness(tree).fitnessValues.put(treeFitnessInfo, value);
								}
							}
						}
						if (!front.consider(tree)) {
							//System.out.println("This tree is not Pareto optimal!!!");
						}

						//Make sure we have enough space left to remember all
						front.getRegistry().increaseFitnessCache(front.size());

						trees.add(tree);
					} catch (ArrayIndexOutOfBoundsException aioobe) {
						//The TreeUtil.fromString might fail
						System.err.println("We could not parse the following tree string, skipping this one. "
								+ realTreeString);
					}
				}
			} catch (StringIndexOutOfBoundsException strE) {
				//Skip this line
			}
		}

		//s.close();
		//input.close();

		//		front.updateLowerLimit(FitnessReplay.info, 0.75);
		//		front.updateLowerLimit(PrecisionEscEdges.info, 0.75);
		//		front.updateLowerLimit(SimplicityUselessNodes.info, 0.75);
		//		front.updateLowerLimit(Generalization.info, 0.75);

		front.reEvaluateParetoFitness();

		front.reduceSize(); //sine we add all one-by-one, size is not taken care of
		//Then add all these trees to the front
		//front.consider(trees);

		//And return the front
		return front;
		/**/
	}

}
