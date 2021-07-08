package org.processmining.plugins.etm.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.parameters.ETMParamAbstract;
import org.processmining.plugins.etm.parameters.ETMParamPareto;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.PopulationData;

/**
 * When this logger is added as an observer, a statistics file is created where
 * certain statistics are recorded at each generation.
 * 
 * @author jbuijs
 * 
 */
public class StatisticsLogger<Tracker> implements EvolutionObserver<ProbProcessArrayTree> {

	public static String colSep = ";";

	public SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public DecimalFormat df = new DecimalFormat("#.######");

	private PrintWriter out;
	private CentralRegistry registry;

	private boolean shouldInitialize = true;

	private TreeFitnessInfo[] dimensions;

	private ParetoFront pf = null;

	private ETMParamPareto params;

	public StatisticsLogger(ETMParamAbstract<Tracker,ProbProcessArrayTree> params) {
		//NOTE that we require the parameter object to really have the correct logging path (since this is partially set on execution)
		registry = params.getCentralRegistry();

		if (params instanceof ETMParamPareto) {
			this.params = ((ETMParamPareto) params);
		}

		try {
			File statsFile = new File(params.getPath() + File.separatorChar + "stats.csv");

			statsFile.getParentFile().mkdirs();
			statsFile.createNewFile();
			statsFile.setWritable(true);
			statsFile.setReadable(true);

			FileOutputStream fos = new FileOutputStream(statsFile);
			out = new PrintWriter(fos);
			out.flush();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void populationUpdate(PopulationData<? extends ProbProcessArrayTree> data) {
		if (shouldInitialize) {
			//First detect which quality dimensions we have
			dimensions = registry.getFitness(data.getBestCandidate()).fitnessValues.keys(new TreeFitnessInfo[] {});
			Arrays.sort(dimensions);

			String dimensionHeaderColumns = "";
			for (TreeFitnessInfo dim : dimensions) {
				dimensionHeaderColumns += dim.getCode() + colSep;
			}

			//Write the header line
			out.println("sep=" + colSep);
			if (params != null) {
				pf = params.getParetoFitnessEvaluator().getParetoFront();
				out.print("PFsize" + colSep);
			}
			out.println(" Generation" + colSep + "Timestamp" + colSep + " Fittest" + colSep + " Average" + colSep
					+ " Deviation" + colSep + dimensionHeaderColumns + "bestCandidate");

			shouldInitialize = false;
		}

		//Pareto front mode
		ProbProcessArrayTree bestTree = null;
		if (pf != null) {
			//data does not contain best candidate according to Of but to PFf
			Collection<ProbProcessArrayTree> trees = pf.getFront();
			double bestOf = -1;
			for (ProbProcessArrayTree tree : trees) {
				double of = registry.getFitness(tree).fitnessValues.get(OverallFitness.info);
				if (of > bestOf) {
					bestOf = of;
					bestTree = tree;
				}
			}
		} else {
			bestTree = data.getBestCandidate();
		}

		String bestCandadidateString = TreeUtils.toString(bestTree, registry.getEventClasses());//.replaceAll(",", ";");

		//Prepare values
		int generation = data.getGenerationNumber();
		double bestOverallFitness = data.getBestCandidateFitness();
		double meanFitness = data.getMeanFitness();
		double stddev = data.getFitnessStandardDeviation();

		//Dimension values
		String dimensionValues = "";
		for (TreeFitnessInfo dim : dimensions) {
			dimensionValues += df.format(registry.getFitness(bestTree).fitnessValues.get(dim)) + colSep;
		}

		if (pf != null) {
			out.print(pf.size() + colSep);
		}
		out.println(generation + colSep + sdf.format(new Date()) + colSep + df.format(bestOverallFitness) + colSep
				+ df.format(meanFitness) + colSep + df.format(stddev) + colSep + dimensionValues
				+ bestCandadidateString);
		out.flush();

	}

	public void closeFile() {
		out.close();
	}
}
