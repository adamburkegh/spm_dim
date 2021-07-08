// =============================================================================
// Copyright 2006-2010 Daniel W. Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// =============================================================================
package org.processmining.plugins.etm.logging;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.islands.IslandEvolutionObserver;

/**
 * Trivial evolution observer for displaying information at the end of each
 * generation.
 * 
 * @param <T>
 *            The type of entity being evolved.
 * 
 * @author jbuijs
 */
//jbuijs suggest split into a ProM Context logger, EclipseConsole/CLI logger and file logger (-> no inside engine), stats logger (engine aware?)
public class EvolutionLogger<T> implements IslandEvolutionObserver<T> {
	private boolean showOnConsole = true;
	private boolean fileLoggingEnabled = false;

	private final PluginContext context;
	private DecimalFormat df = new DecimalFormat("#.######");
	private Calendar cal;

	//Writing to a file
	private FileOutputStream fos;
	private PrintWriter out;
	private double meanFitness = -1.0;
	private double stddev = -1.0;

	private int logModulo;

	private final CentralRegistry registry;
	
	public EvolutionLogger(final PluginContext context, CentralRegistry registry) {
		this(context, registry, false);
	}

	public EvolutionLogger(final PluginContext context, CentralRegistry registry, boolean fileLoggingEnabled) {
		this(context, registry, fileLoggingEnabled, 1);
	}

	public EvolutionLogger(final PluginContext context, CentralRegistry registry, boolean fileLoggingEnabled,
			int logModulo) {
		super();

		this.context = context;
		this.fileLoggingEnabled = fileLoggingEnabled;
		this.registry = registry;
		this.logModulo = logModulo;

		if (this.fileLoggingEnabled) {
			//File writing stuff
			try {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH_mm_ss");
				cal = Calendar.getInstance();
				String timestamp = sdf.format(cal.getTime());

				String filename = "./stats/stats_" + timestamp + ".csv";
				File statsFile = new File(filename);

				//			statsFile.createNewFile();
				statsFile.setWritable(true);
				statsFile.setReadable(true);

				fos = new FileOutputStream(statsFile);
				out = new PrintWriter(fos);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}

			//Write the header line
			out.println("Timestamp; Generation; Fittest; Average; Deviation; " + "replayFitness; " +
			//"BehApp; Coverage; " +
					"bestCandidate");
		}
	}

	public void disableFileLogging() {
		fileLoggingEnabled = false;
	}

	public void populationUpdate(PopulationData<? extends T> data) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		cal = Calendar.getInstance();

		ProbProcessArrayTree tree = (ProbProcessArrayTree) data.getBestCandidate();
		String bestCandadidateString = TreeUtils.toString(tree, registry.getEventClasses());

		//Prepare values
		int generation = data.getGenerationNumber();
		double bestOverallFitness = data.getBestCandidateFitness();
		meanFitness = data.getMeanFitness();
		stddev = data.getFitnessStandardDeviation();

		//TODO check fitness map to string output (makes sense? or custom impl)
		//TODO change to String.format
		/*-*/
		if ((registry.getCurrentGeneration() % logModulo) == 0) {
			if (showOnConsole) {
				System.out.println("Generation " + generation + ": " + df.format(bestOverallFitness) + "("
						+ df.format(meanFitness) + " +/- " + df.format(stddev) + ") - #" + data.getPopulationSize()
						+ " ( " + registry.getFitness(tree).fitnessValues.toString() + ")");
				System.out.println("  " + bestCandadidateString + ") - " + sdf.format(cal.getTime()));
			}
			/**/

			if (fileLoggingEnabled) {
				//And to a file
				out.println(sdf.format(cal.getTime()) + " ; " + generation + " ; " + df.format(bestOverallFitness)
						+ " ; " + df.format(meanFitness) + " ; " + df.format(stddev) + " ; "
						+ df.format(registry.getFitness(tree).fitnessValues.get(FitnessReplay.info)) + " ; "
						//+ df.format(coverage) + " ; "
						+ bestCandadidateString);
				out.flush();
			}

			//And output to ProM, if possible
			if (context != null) {
				context.getProgress().inc();
				context.log(sdf.format(new Date()) + " Generation " + data.getGenerationNumber() + ": "
						+ registry.getOverallFitness(tree) + " ( " + registry.getFitness(tree).fitnessValues.toString()
						+ ")");
			}
		}
	}

	public void islandPopulationUpdate(int islandNr, PopulationData<? extends T> data) {
		//Node node = (Node) data.getBestCandidate();
		//String bestCandadidateString = node.toString();
		if (showOnConsole) {
			System.out.println("  Island " + islandNr + ": " + df.format(data.getBestCandidateFitness()) + "("
					+ df.format(data.getMeanFitness()) + " +/- " + df.format(data.getFitnessStandardDeviation())
					+ ") -#" + data.getPopulationSize());
			//System.out.println("  " + bestCandadidateString + ") - " + sdf.format(cal.getTime()));
		}
	}

	public void closeFile() {
		if (fileLoggingEnabled) {
			out.close();
		}
	}

	public double getWorstFitnessInLastPopulation() {
		if (meanFitness < 0) {
			return Double.MAX_VALUE;
		} else {
			return meanFitness + 2 * stddev;
		}
	}

	/**
	 * Updates the progress of the context such that it scales correctly to the
	 * maximum number of steps
	 * 
	 * @param levels
	 */
	public void setProgressLevels(int levels) {
		if (context != null) {
			context.getProgress().setIndeterminate(false);
			context.getProgress().setMaximum(levels);
		}
	}
}
