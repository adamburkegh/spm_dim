package org.processmining.plugins.etm.experiments.thesis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.util.Pair;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.experiments.PaperMethods;
import org.processmining.plugins.etm.fitness.FitnessRegistry;
import org.processmining.plugins.etm.fitness.TreeFitness;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.metrics.ConfigurationFitness;
import org.processmining.plugins.etm.fitness.metrics.EditDistanceWrapperRTEDRelative;
import org.processmining.plugins.etm.fitness.metrics.FitnessReplay;
import org.processmining.plugins.etm.fitness.metrics.Generalization;
import org.processmining.plugins.etm.fitness.metrics.OverallFitness;
import org.processmining.plugins.etm.fitness.metrics.PrecisionEscEdges;
import org.processmining.plugins.etm.fitness.metrics.SimplicityUselessNodes;
import org.processmining.plugins.etm.model.ParetoFront;
import org.processmining.plugins.etm.model.narytree.conversion.NAryTreeToProcessTree;
import org.processmining.plugins.etm.model.narytree.conversion.ProcessTreeToNAryTree;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.model.serialization.ParetoFrontImport;
import org.processmining.plugins.etm.parameters.ETMParamFactory;
import org.processmining.plugins.etm.termination.ProMCancelTerminationCondition;
import org.processmining.plugins.etm.utils.ETMUtils;
import org.processmining.plugins.ptmerge.ptmerge.PlugIn;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.impl.ProcessTreeImpl;

/**
 * Convenience class to quickly process the tar.gz files that are the result of
 * the thesis experiments to aggregated files used in Joos' PhD thesis. NOTE:
 * this class is not optimized for performance and may run for a long time.
 * 
 * @author jbuijs
 * @since 2014-04-17
 * 
 */
public class ThesisExperimentProcessor {

	/**
	 * The directory that contains the experiment directories the SSHExperiments
	 * class logged to.
	 */
	public static String ETMLocalLogsDirString = "E:\\ETMlocalLogs\\CLIexp\\ngrid\\";

	public static String ThesisBaseDirectory = "C:\\Users\\jbuijs\\Documents\\PhD\\Projects\\PhD\\Thesis\\";
	//Experiments\configurable_runExApp\14-04-23_17_46_10_configurable_runEx_approach1_112_ngrid07\BLUE
	public static String EventLogDirectory = ThesisBaseDirectory + "\\Experiments\\000Event Logs\\";

	public static String preferredExperimentDate = "14-04-25_20_12_45";//"14-04-15_20_16_12";// "14-04-23_17_46_10";

	public static SimpleDateFormat statsDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	/**
	 * Directory used to temporarily untar the tar.gz files, process the
	 * contents and then delete the files again. This directory should be able
	 * to contain 10+ GB of data!!!
	 */
	public static File TEMPuntarDir = new File("H:\\tmpUntarDir\\");

	/**
	 * Known experiment codes we can parse.
	 */
	public enum ExperimentCodes {
		basicDisc_runEx_noNoise, basicDisc_runEx_noise, basicDisc_runExNoise_pareto, caseStudies_randomVsGuided, caseStudies_buildingPerm_randomVsGuided, caseStudies_buildingPerm_and_WABO, config_runEx, config_wabo
	};

	public enum ProcessingModes {
		OverallFitness, size, durationPerGeneration
	};

	/**
	 * The different configuration approaches available (might also be referred
	 * to by number)
	 * 
	 * @author jbuijs
	 * 
	 */
	public enum ConfigurationApproaches {
		BLUE, RED, BLACK, GREEN, GREEN_PARETO
	}

	public static HashMap<TreeFitnessInfo, Double> standardWeights = new HashMap<TreeFitnessInfo, Double>();
	static {
		standardWeights.put(FitnessReplay.info, 10d);
		standardWeights.put(PrecisionEscEdges.info, 5d);
		standardWeights.put(SimplicityUselessNodes.info, 1d);
		standardWeights.put(Generalization.info, 1d);
	}

	/**
	 * stores which nth generation (using modulo) should be stored. f.i. 0,1 and
	 * 10,2 means every generation from 0-10, from generation 10 only the even
	 * generations
	 */
	public static int[][] aggregationDetail = new int[][] { new int[] { 0, 1 },
			new int[] { 20, 2 }, new int[] { 50, 5 }, new int[] { 100, 10 }, 
			new int[] { 9999, 1 } }; //9999 is added to make sure we record the very last entry

	public static void main(String[] args) {

		System.out.println("Using " + ETMLocalLogsDirString + " as local ETM log directory.");
		System.out.println("Using " + TEMPuntarDir + " as temporary unpack directory.");

		clearUntarDir();

		/*
		 * Step 1: try to auto detect the most recent result subdir in the
		 * result dir
		 */
		File ETMLocalLogsDir = new File(ETMLocalLogsDirString);
		File[] ETMLocalLogsDirFiles = ETMLocalLogsDir.listFiles();

		String experimentDate = preferredExperimentDate;
		File ETMLocalLogsDirPreferred = new File(ETMLocalLogsDirString + "/" + experimentDate);

		System.out.println("Preferred experiment directory: '" + experimentDate + "'.");
		System.out.println("Type in different subdirectory, or press ENTER if OK.");
		Scanner in = new Scanner(System.in);
		boolean ETMLocalLogsDirSuggestionExists = true;
		do {
			String ETMLocalLogsDirSuggestionString = in.nextLine();
			if (!ETMLocalLogsDirSuggestionString.isEmpty()) {
				File ETMLocalLogsDirSuggestion = new File(ETMLocalLogsDirString + ETMLocalLogsDirSuggestionString);
				if (!ETMLocalLogsDirSuggestion.exists()) {
					System.out.println("Directory does not exist, please try again.");
					ETMLocalLogsDirSuggestionExists = false;
				} else {
					ETMLocalLogsDirPreferred = ETMLocalLogsDirSuggestion;
					experimentDate = ETMLocalLogsDirPreferred.getName();
				}
			}
		} while (!ETMLocalLogsDirSuggestionExists);
		System.out.println("Parsing " + ETMLocalLogsDirPreferred.getAbsolutePath());

		/*
		 * Step 2: in which mode do you want to run, e.g. what do you want to
		 * do/produce?
		 */
		System.out.println("In which mode do you want to run, e.g. what do you want to do/produce?");
		for (int i = 0; i < ProcessingModes.values().length; i++) {
			System.out.println(i + ". " + ProcessingModes.values()[i]);
		}
		ProcessingModes currentProcessingMode = ProcessingModes.values()[Integer.parseInt(in.nextLine())];

		/*
		 * Step 3: which experiments should we parse?
		 */
		System.out
				.println("Which experiments do you want us to parse? ENTER = all, or enter the numbers seperated by spaces.");
		for (int i = 0; i < ExperimentCodes.values().length; i++) {
			System.out.println(i + ". " + ExperimentCodes.values()[i]);
		}
		String experimentCodesToParseString = in.nextLine();
		ExperimentCodes[] experimentCodesToParse;
		if (!experimentCodesToParseString.trim().isEmpty()) {
			String[] codes = experimentCodesToParseString.split(" ");
			experimentCodesToParse = new ExperimentCodes[codes.length];
			for (int i = 0; i < codes.length; i++) {
				experimentCodesToParse[i] = ExperimentCodes.values()[Integer.parseInt(codes[i])];
			}
		} else {
			experimentCodesToParse = ExperimentCodes.values();
		}
		System.out.println("Parsing " + experimentCodesToParse.length + " experiments.");

		/*
		 * Step 4: call the methods to parse the experiment files (.tar.gz)
		 */
		for (int i = 0; i < experimentCodesToParse.length; i++) {
			switch (experimentCodesToParse[i]) {
				case basicDisc_runEx_noNoise :
					parse_basicDisc_runEx_noNoise(ETMLocalLogsDirPreferred, experimentDate, currentProcessingMode);
					break;
				case basicDisc_runEx_noise :
					parse_basicDisc_runEx_noise(ETMLocalLogsDirPreferred, experimentDate, currentProcessingMode);
					break;
				case basicDisc_runExNoise_pareto :
					parse_basicDisc_runExNoise_pareto(ETMLocalLogsDirPreferred, experimentDate, currentProcessingMode);
					break;
				case caseStudies_randomVsGuided :
					parse_caseStudies_randomVsGuided(ETMLocalLogsDirPreferred, experimentDate);
					break;/**/
				case caseStudies_buildingPerm_randomVsGuided :
					parse_caseStudies_buildingPerm_randomVsGuided(ETMLocalLogsDirPreferred, experimentDate);
					break;/**/
				case caseStudies_buildingPerm_and_WABO :
					parse_caseStudies_buildingPerm_and_WABO(ETMLocalLogsDirPreferred, experimentDate,
							currentProcessingMode);
					break;
				case config_runEx :
					parse_config_runEx();
					break;
				case config_wabo :
					parse_config_wabo();
					break;
				default :
					System.err.println("UNKNOWN EXPERIMENT CODE " + experimentCodesToParse[i] + " skipping...");
			}
		}

		System.out.println("FINISHED");
	}

	/**
	 * Parses the basicDisc_runEx_noNoise experiment files and produces a
	 * statistics file and a .tex file with the 'most average' process tree
	 * found in the final generation.
	 * 
	 * @param eTMLocalLogsDirMostrecent
	 * @param experimentDate
	 */
	public static void parse_basicDisc_runEx_noNoise(File eTMLocalLogsDirMostrecent, String experimentDate,
			ProcessingModes currentProcessingMode) {
		final String experimentCode = ExperimentCodes.basicDisc_runEx_noNoise.toString();
		System.out.println("Starting " + experimentCode);

		parse_standardStatsFile(eTMLocalLogsDirMostrecent, experimentDate, experimentCode, false, currentProcessingMode);
	}

	/**
	 * Parses the basicDisc_runEx_noise experiment files and produces a
	 * statistics file and a .tex file with the 'most average' process tree
	 * found in the final generation.
	 * 
	 * @param eTMLocalLogsDirMostrecent
	 * @param experimentDate
	 */
	public static void parse_basicDisc_runEx_noise(File eTMLocalLogsDirMostrecent, String experimentDate,
			ProcessingModes currentProcessingMode) {
		final String experimentCode = ExperimentCodes.basicDisc_runEx_noise.toString();

		System.out.println("Starting " + experimentCode);

		parse_standardStatsFile(eTMLocalLogsDirMostrecent, experimentDate, experimentCode, false, currentProcessingMode);
	}

	/**
	 * Processes the experiment codes caseStudies_BuildingPerm and
	 * caseStudies_WABO1..5
	 * 
	 * @param eTMLocalLogsDirPreferred
	 * @param experimentDate
	 * @param currentProcessingMode
	 */
	public static void parse_caseStudies_buildingPerm_and_WABO(File eTMLocalLogsDirPreferred, String experimentDate,
			ProcessingModes currentProcessingMode) {
		String[] experimentCodes = new String[] { "caseStudies_buildingPerm", "caseStudies_WABO1", "caseStudies_WABO2",
				"caseStudies_WABO3", "caseStudies_WABO4", "caseStudies_WABO5" };

		//NOTE in case of experiment date '14-04-25_20_12_45' we parse a specific subset
		if (!experimentDate.equalsIgnoreCase("14-04-25_20_12_45")) {
			//ELSE we parse all the files we can find			
			for (final String experimentCode : experimentCodes) {
				System.out.println("Starting " + experimentCode);

				parse_standardStatsFile(eTMLocalLogsDirPreferred, experimentDate, experimentCode, false,
						currentProcessingMode);
			}
		} else {
			//Parse specific files for '14-04-25_20_12_45'
			File[] files = new File[] {
					new File(eTMLocalLogsDirPreferred + "/" + experimentDate + "_" + experimentCodes[0]
							+ "_0_ngrid06.tar.gz"),//
					new File(eTMLocalLogsDirPreferred + "/" + experimentDate + "_" + experimentCodes[1]
							+ "_1_ngrid10.tar.gz"),
					new File(eTMLocalLogsDirPreferred + "/" + experimentDate + "_" + experimentCodes[2]
							+ "_2_ngrid02.tar.gz"),
					new File(eTMLocalLogsDirPreferred + "/" + experimentDate + "_" + experimentCodes[3]
							+ "_3_ngrid10.tar.gz"),
					new File(eTMLocalLogsDirPreferred + "/" + experimentDate + "_" + experimentCodes[4]
							+ "_4_ngrid03.tar.gz"),
					new File(eTMLocalLogsDirPreferred + "/" + experimentDate + "_" + experimentCodes[5]
							+ "_5_ngrid08.tar.gz") };

			/*
			 * For each file: unGzip and inspect the tar stream
			 */
			/*-*/
			//for (File file : files) {
			for (int i = 0; i < experimentCodes.length; i++) {
				File file = files[i];
				String experimentCode = experimentCodes[i];
				try {
					StatisticsAggregator stats = new StatisticsAggregator(aggregationDetail);

					System.out.println(" Unzipping ");
					final GZIPInputStream in = new GZIPInputStream(new FileInputStream(file));
					//File tarFile = unGzip(file, TEMPuntarDir);

					String filename = file.getName();
					filename = filename.substring(0, filename.lastIndexOf('_')); //strip of ngridXX.tar.gz part
					int session = Integer
							.parseInt(filename.substring(filename.lastIndexOf('_') + 1, filename.length()));

					System.out.println(" Parsing session " + session);

					//final InputStream is = new FileInputStream(tarFile);
					final TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
							.createArchiveInputStream("tar", in);
					TarArchiveEntry entry = null;
					ENTRY: while (tarInputStream.getNextTarEntry() != null) {
						while ((entry = (TarArchiveEntry) tarInputStream.getNextEntry()) != null) {
							if (!entry.isDirectory()) {
								String tarFilename = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
								//We only want to parse the stats.csv file and then go to the next session
								if (tarFilename.equalsIgnoreCase("stats.csv")) {
									parseStatsFile(stats, tarInputStream, session, null, currentProcessingMode);
									break ENTRY; //move to the next session
								}

								//break;
							}
						}
					}
					tarInputStream.close();

					clearUntarDir();

					//now write out the stats object
					String outPath = eTMLocalLogsDirPreferred.getAbsolutePath() + "\\results";
					String filePrefix = experimentCode;
					if (currentProcessingMode == ProcessingModes.durationPerGeneration) {
						filePrefix += "_duration";
					}
					stats.writeFiles(outPath, filePrefix);
					System.out.println("Written output to " + outPath);

				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ArchiveException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}//for each file

		}
	}

	public static void parse_basicDisc_runExNoise_pareto(File eTMLocalLogsDirMostrecent, final String experimentDate,
			ProcessingModes currentProcessingMode) {
		final String experimentCode = ExperimentCodes.basicDisc_runExNoise_pareto.toString();

		int maxGen = 10000;

		System.out.println("Start processing " + experimentCode);

		File[] files = eTMLocalLogsDirMostrecent.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches(experimentDate + "_" + experimentCode + "_\\d+_ngrid\\d+.tar.gz");
			}
		});

		StatisticsAggregator stats = new StatisticsAggregator(aggregationDetail);
		ParetoFrontImport importer = new ParetoFrontImport();

		/*
		 * For each file: unGzip and inspect the tar stream
		 */
		for (File file : files) {
			try {
				System.out.println(" Unzipping ");
				final GZIPInputStream in = new GZIPInputStream(new FileInputStream(file));
				//File tarFile = unGzip(file, TEMPuntarDir);

				String filename = file.getName();
				filename = filename.substring(0, filename.lastIndexOf('_')); //strip of ngridXX.tar.gz part
				int session = Integer.parseInt(filename.substring(filename.lastIndexOf('_') + 1, filename.length()));

				System.out.println(" Parsing session " + session);

				//final InputStream is = new FileInputStream(tarFile);
				final TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
						.createArchiveInputStream("tar", in);
				TarArchiveEntry entry = null;
				ENTRY: while (tarInputStream.getNextTarEntry() != null) {
					while ((entry = (TarArchiveEntry) tarInputStream.getNextEntry()) != null) {
						if (!entry.isDirectory()) {
							String tarFilename = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
							//							System.out.println("   tarfile: " + tarFilename);
							switch (currentProcessingMode) {
								case OverallFitness :
								case size :
									String paretoFileName = "paretoFront.PTPareto";
									if (tarFilename.matches("generation\\d*.log")
											|| tarFilename.matches(paretoFileName)) {
										int generation = 0;
										if (tarFilename.matches(paretoFileName)) {
											generation = maxGen;
										} else {
											generation = Integer.parseInt(tarFilename.replace("generation", "")
													.replace(".log", ""));
										}

										if (stats.shouldLog(generation)) {
											//System.out.println("   processing generation " + generation);

											//	            LOG.info(String.format("Creating output file %s.", outputFile.getAbsolutePath()));
											//						final OutputStream outputFileStream = new FileOutputStream(outputFile);
											//						IOUtils.copy(tarInputStream, outputFileStream);
											//						outputFileStream.close();
											if (currentProcessingMode == ProcessingModes.size) {
												ParetoFront pf = importer.importParetoFront(tarInputStream,
														FitnessRegistry.defaultKnownInfos);

												if (pf != null) {
													int size = pf.size();

													/*-
													Scanner scanner = new Scanner(tarInputStream);
													scanner.useDelimiter("\r\n");

													int size = 0;
													try {
														//Second line is what we need
														scanner.next();
														String line = scanner.next();

														//Too hard coded but works!
														String sizeStr = line.split(" ")[4];

														size = Integer.valueOf(sizeStr);
													} catch (NoSuchElementException e) {
													}/**/

													//										if (size <= 0) {
													//											System.out.println("small");
													//										}

													//Lets also store the size
													stats.addStatistic(generation, "size", size);

													stats.addTree(generation, size, session, "DUMMY");
												}
											} else if (currentProcessingMode == ProcessingModes.OverallFitness) {

												ParetoFront pf = importer.importParetoFront(tarInputStream,
														FitnessRegistry.defaultKnownInfos);

												if (pf != null) {
													//Get the best Of
													ProbProcessArrayTree bestTree = getTreeWithBestOf(pf, standardWeights);

													//Add to statistics aggregator
													TreeFitness fitness = pf.getRegistry().getFitness(bestTree);

													stats.addStatistic(generation, "Of",
															fitness.getOverallFitnessValue());
													stats.addTree(generation, fitness.getOverallFitnessValue(),
															session, TreeUtils.toString(bestTree, pf.getRegistry()
																	.getEventClasses()));
												} else {
													System.out.println("   PF is null");
												}

												if (generation == 99999) {
													String outPath = eTMLocalLogsDirMostrecent.getAbsolutePath()
															+ "\\results";
													File outputFile = new File(outPath + "\\s" + session + "g"
															+ generation + ".PTPareto");
													final OutputStream outputFileStream = new FileOutputStream(
															outputFile);
													IOUtils.copy(tarInputStream, outputFileStream);
													outputFileStream.close();

												}
											}
										}
									}//if process filename for size and Of modes
									break;

								case durationPerGeneration :
									//We only want to parse the stats.csv file and then go to the next session
									if (tarFilename.equalsIgnoreCase("stats.csv")) {
										parseStatsFile(stats, tarInputStream, session, null, currentProcessingMode);
										break ENTRY; //move to the next session
									}

									break;
							}
						}

					}
				}
				tarInputStream.close();

				clearUntarDir();

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ArchiveException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}//for each file

		//now write out the stats object
		String outPath = eTMLocalLogsDirMostrecent.getAbsolutePath() + "\\results";
		String filePrefix = experimentCode;
		if (currentProcessingMode == ProcessingModes.durationPerGeneration) {
			filePrefix += "_duration";
		}
		stats.writeFiles(outPath, filePrefix);
		System.out.println("Written output to " + outPath);

		if (currentProcessingMode == ProcessingModes.size) {
			TreeMap<Double, Pair<Integer, String>> lastGen = stats.concreteTrees.get(maxGen);
			for (Entry<Double, Pair<Integer, String>> entry : lastGen.descendingMap().entrySet()) {
				System.out.println("s:" + entry.getValue().getFirst() + " size: " + entry.getKey());
			}
		}

	}

	/**
	 * Parses the caseStudies_buildingPerm_randomVsGuided experiment files and
	 * produces a statistics file with the average Of of the different ratios.
	 * It also produces an .tex file with the 'most average' process tree found
	 * in the final generation (which is kinda useless).
	 * 
	 * @param eTMLocalLogsDirMostrecent
	 * @param experimentDate
	 */
	public static void parse_caseStudies_buildingPerm_randomVsGuided(File eTMLocalLogsDirMostrecent,
			String experimentDate) {
		final String experimentCode = ExperimentCodes.caseStudies_buildingPerm_randomVsGuided.toString();

		System.out.println("Starting " + experimentCode);

		parse_standardStatsFile(eTMLocalLogsDirMostrecent, experimentDate, experimentCode, true,
				ProcessingModes.OverallFitness);
	}

	/**
	 * Parses the caseStudies_randomVsGuided experiment files and produces a
	 * statistics file with the average Of of the different ratios. It also
	 * produces an .tex file with the 'most average' process tree found in the
	 * final generation (which is kinda useless).
	 * 
	 * @param eTMLocalLogsDirMostrecent
	 * @param experimentDate
	 */
	public static void parse_caseStudies_randomVsGuided(File eTMLocalLogsDirMostrecent, String experimentDate) {
		final String experimentCode = ExperimentCodes.caseStudies_randomVsGuided.toString();

		System.out.println("Starting " + experimentCode);

		parse_standardStatsFile(eTMLocalLogsDirMostrecent, experimentDate, experimentCode, true,
				ProcessingModes.OverallFitness);
	}

	/**
	 * Parses the provided experiment code statistic files and produces an
	 * aggregated file.
	 * 
	 * @param eTMLocalLogsDirMostrecent
	 * @param experimentDate
	 * @param experimentCode
	 * @param randomVsGuidedMode
	 *            if false, assumes 'normal' parsing of several stats files
	 * @param currentProcessingMode
	 *            The process mode of this stats file
	 */
	public static void parse_standardStatsFile(File eTMLocalLogsDirMostrecent, final String experimentDate,
			final String experimentCode, final boolean randomVsGuidedMode, ProcessingModes currentProcessingMode) {
		/*
		 * Step 1: get the list of files we should parse
		 */
		final String acceptFileString = experimentDate + "_" + experimentCode + (randomVsGuidedMode ? ".*" : "")
				+ "_\\d+_ngrid\\d+.tar.gz";
		File[] files = eTMLocalLogsDirMostrecent.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches(acceptFileString);
			}
		});

		StatisticsAggregator stats = new StatisticsAggregator(aggregationDetail);

		/*
		 * For each file: untar, process and clear tar dir
		 */
		for (File file : files) {
			try {
				//Can be more efficient but works quite fast for these files
				List<File> sessionFiles = unTar(unGzip(file, TEMPuntarDir), TEMPuntarDir);

				String filename = file.getName();
				filename = filename.substring(0, filename.lastIndexOf('_')); //strip of ngridXX.tar.gz part
				int session = Integer.parseInt(filename.substring(filename.lastIndexOf('_') + 1, filename.length()));

				String alias = null; //by default in 'normal' mode
				if (randomVsGuidedMode) {
					//Extract the alias from the file name...
					//filename already has ngridXX stripped
					//now strip session
					//alias is last _ bit
					filename = filename.substring(0, filename.lastIndexOf('_'));
					alias = filename.substring(filename.lastIndexOf('_') + 1, filename.length());
				}

				System.out.println(" Parsing session " + session);

				//Maybe overkill, since we only want the stats.csv file...
				for (File sessionFile : sessionFiles) {
					if (sessionFile.getName().equals("stats.csv")) {
						parseStatsFile(stats, new FileInputStream(sessionFile), session, alias, currentProcessingMode);
					}
				}

				clearUntarDir();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ArchiveException e) {
				e.printStackTrace();
			}
		}//for each file

		//now write out the stats object
		String outPath = eTMLocalLogsDirMostrecent.getAbsolutePath() + "\\results";
		String filePrefix = experimentCode;
		if (currentProcessingMode == ProcessingModes.durationPerGeneration) {
			filePrefix += "_duration";
		}
		stats.writeFiles(outPath, filePrefix);
		System.out.println("Written output to " + outPath);

	}

	/**
	 * Parses the provided to the statistics object. If fittestAlias is null or
	 * empty then all dimension columns are parsed (for basicDisc_(no)noise 30x
	 * experimetns). If an alias is provided then the 'fittest' column is parsed
	 * as that alias (for the casestudy_random versus guided experiments)
	 * 
	 * @param stats
	 * @param sessionFile
	 * @param fittestAlias
	 */
	private static void parseStatsFile(StatisticsAggregator stats, InputStream statsFile, int session,
			String fittestAlias, ProcessingModes currentProcessingMode) {
		//Now parse this thing
		Scanner in = new Scanner(statsFile);

		in.useDelimiter(";");
		String line2 = in.next();

		//first line is sep=;
		String tmp = in.nextLine();
		//						System.out.println(tmp);

		//inspect part of header to detect if this is a special Pareto stats file
		boolean paretoStatsFile = false;
		if (in.next().equalsIgnoreCase("PFsize")) {
			paretoStatsFile = true;
		}
		//skip rest of header
		tmp = in.nextLine();
		//						System.out.println(tmp);

		Date previousDate = null; //last know date a generation finished
		int previousGeneration = -1; //which generation finished (since we might skip some)

		whileNextLine: while (in.hasNextLine()) {
			String nextLine = in.nextLine();
			Scanner line = new Scanner(nextLine);
			line.useDelimiter(";");

			//first column can be PF size, ignore this one if that is true
			if (paretoStatsFile) {
				line.next();
			}

			//first column is generation number
			int generation = line.nextInt();

			if (stats.shouldLog(generation)) {

				//second column is timestamp
				Date endDate = null;
				try {
					endDate = statsDateFormat.parse(line.next());
				} catch (ParseException e) {
					e.printStackTrace();
				}

				//third column is fittest (Of)
				double fittest = line.nextDouble();

				//fourth is average fitness and fifth is deviation
				line.next();
				line.next();

				//Next are Gv, Of, Pe, Fr, Su (sorted by name, not code)
				double Gv = 0;
				double Of = 0;
				double Pff = 0;
				double Pe = 0;
				double Fr = 0;
				double Su = 0;
				if (paretoStatsFile) {
					//The 6 cols in a PF stats file mean nothing to us
					for (int i = 0; i < 6; i++) {
						line.next();
					}
				} else {
					Gv = line.nextDouble();
					Of = line.nextDouble();
					Pe = line.nextDouble();
					Fr = line.nextDouble();
					Su = line.nextDouble();
				}

				//And then the best candidate
				String bestCandidate = line.next();

				switch (currentProcessingMode) {
					case OverallFitness :
						if (fittestAlias == null || fittestAlias.isEmpty()) {
							stats.addStatistic(generation, OverallFitness.info.getCode(), Of);
							// stats.addStatistic(generation, FitnessReplay.info.getCode(), Fr);
							stats.addStatistic(generation, PrecisionEscEdges.info.getCode(), Pe);
							stats.addStatistic(generation, SimplicityUselessNodes.info.getCode(), Su);
							stats.addStatistic(generation, Generalization.info.getCode(), Gv);
						} else {
							stats.addStatistic(generation, fittestAlias, Of);
						}

						break;
					case durationPerGeneration :
						/*
						 * Unfortunately for session 77 in experiment run
						 * 14-04-15_20_16_12 two PF instances ran in parallel,
						 * one of which stopped around generation 9800, hence in
						 * the stats.csv after generation 9999 there is a line
						 * for generation 9799... therefore we stop processing
						 * this session
						 */
						if (generation > previousGeneration) {
							//for generation 0 we can not calculate a duration
							if (generation > 0) {
								//We calculate the number of seconds per generation
								BigDecimal diff = BigDecimal.valueOf(endDate.getTime() - previousDate.getTime())
										.divide(new BigDecimal(1000));
								BigDecimal correctedDiff = diff.divide(new BigDecimal(generation - previousGeneration));
								//long diff = ((long)endDate.getTime() - previousDate.getTime()) / (long) 1000;
								//correct for skipping generations
								//diff = diff / (generation - previousGeneration);
								stats.addStatistic(generation, "duration", correctedDiff.doubleValue());
							}

							previousDate = endDate;
							previousGeneration = generation;
						} else {
							//stop this session/file
							break whileNextLine;
						}
						break;
					default :
						break;
				}

				stats.addTree(generation, Of, session, bestCandidate);
			}
		}
	}

	/**
	 * Parse the configuration experiments on the running example event logs
	 */
	public static void parse_config_runEx() {
		/*
		 * First load the running example log files
		 */
		ArrayList<XLog> logs = loadConfigRunExLogs();
		String baseLatexCode = "discConfigurable_runEx";

		/*
		 * Lets add the default tree strings such that they do not need to be
		 * entered every time
		 */
		Map<ConfigurationApproaches, String[]> trees = new HashMap<ConfigurationApproaches, String[]>();
		//RESULTS of 14-05-09_10_22_37

		trees.put(
				ConfigurationApproaches.BLUE, //n trees
				new String[] {
						"SEQ( LEAF: A+complete , AND( XOR( AND( LEAF: C+complete , LEAF: D+complete ) , LEAF: C+complete ) , LEAF: B+complete ) , XOR( LEAF: E+complete , LEAF: F+complete ) , LEAF: G+complete ) [ ] 0.9960 (Of: 0.9960 Gv: 0.8717 Pe: 0.9897 Fr: 1.0000 Su: 1.0000 )",
						"SEQ( LEAF: A+complete , LEAF: B1+complete , LEAF: B2+complete , LEAF: C+complete , LEAF: D2+complete , XOR( LEAF: E+complete , LEAF: F+complete ) ) [ ] 0.9992 (Of: 0.9992 Gv: 0.8665 Pe: 1.0000 Fr: 1.0000 Su: 1.0000 )",
						"SEQ( LEAF: A+complete , LEAF: C+complete , LEAF: B+complete , XOR( LEAF: E+complete , LEAF: F+complete ) ) [ ] 0.9995 (Of: 0.9995 Gv: 0.9205 Pe: 1.0000 Fr: 1.0000 Su: 1.0000 )",
						"SEQ( LEAF: A+complete , LEAF: B1+complete , XOR( SEQ( LEAF: D+complete , LEAF: B2+complete , LEAF: C+complete , LEAF: E+complete ) , SEQ( LEAF: D2+complete , LEAF: B2+complete , LEAF: C+complete , LEAF: F+complete ) ) ) [ ] 0.9992 (Of: 0.9992 Gv: 0.8728 Pe: 1.0000 Fr: 1.0000 Su: 1.0000 )",
				//		
				});
		trees.put(
				ConfigurationApproaches.RED, //n + 1, first merged/ Mstar
				new String[] {
						"SEQ( LEAF: A+complete , XOR( SEQ( AND( XOR( AND( LEAF: B+complete , LEAF: D+complete ) , LEAF: B+complete ) , LEAF: C+complete ) , XOR( LEAF: E+complete , LEAF: F+complete ) , XOR( LEAF: G+complete , LEAF: tau ) ) , SEQ( LEAF: B1+complete , XOR( SEQ( LEAF: D2+complete , LEAF: B2+complete ) , SEQ( XOR( LEAF: D+complete , LEAF: tau ) , LEAF: B2+complete ) ) , LEAF: C+complete , XOR( LEAF: D2+complete , LEAF: tau ) , XOR( LEAF: E+complete , LEAF: F+complete ) ) ) ) [ ] 0.9793 (Fr: 1.0000 Gv: 0.9134 Pe: 0.9350 Su: 1.0000 Of: 0.9793 )",
						"SEQ( LEAF: A+complete , XOR( SEQ( AND( XOR( AND( LEAF: B+complete , LEAF: D+complete ) , LEAF: B+complete ) , LEAF: C+complete ) , XOR( LEAF: E+complete , LEAF: F+complete ) , XOR( SEQ( LEAF: G+complete , LEAF: tau ) ) ) , SEQ( LEAF: B1+complete , XOR( SEQ( LEAF: D2+complete , LEAF: B2+complete ) , SEQ( XOR( LEAF: D+complete , LEAF: tau ) , LEAF: B2+complete ) ) , LEAF: C+complete , XOR( LEAF: D2+complete , LEAF: tau ) , XOR( LEAF: E+complete , LEAF: F+complete ) ) ) ) [ ] 0.9736 (Of: 0.9736 Fr: 1.0000 Gv: 0.3898 Pe: 0.9260 Su: 0.9444 Er: 0.9859 )",
						"SEQ( LEAF: A+complete , XOR( SEQ( AND( XOR( AND( LEAF: B+complete , LEAF: D+complete ) , LEAF: B+complete ) , LEAF: C+complete ) , XOR( LEAF: E+complete , LEAF: F+complete ) , XOR( LEAF: G+complete , LEAF: tau ) ) , SEQ( LEAF: B1+complete , XOR( SEQ( XOR( LEAF: tau ) , LEAF: B2+complete ) ) , LEAF: C+complete , XOR( SEQ( LEAF: D2+complete , LEAF: tau ) ) , XOR( LEAF: E+complete , LEAF: F+complete ) ) ) ) [ ] 0.9519 (Of: 0.9519 Fr: 1.0000 Gv: 0.3813 Pe: 0.9091 Su: 0.8750 Er: 0.9254 )",
						"SEQ( LEAF: A+complete , XOR( SEQ( AND( SEQ( AND( LEAF: C+complete ) , LEAF: B+complete ) ) , XOR( LEAF: E+complete , LEAF: F+complete ) , XOR( LEAF: tau ) ) , SEQ( LEAF: B1+complete , XOR( SEQ( LEAF: D2+complete , LEAF: B2+complete ) , SEQ( XOR( LEAF: D+complete , LEAF: tau ) , LEAF: B2+complete ) ) , LEAF: C+complete , XOR( LEAF: D2+complete , LEAF: tau ) , XOR( LEAF: E+complete , LEAF: F+complete ) ) ) ) [ ] 0.9483 (Of: 0.9483 Fr: 1.0000 Gv: 0.3175 Pe: 0.8889 Su: 0.9063 Er: 0.9254 )",
						"SEQ( LEAF: A+complete , XOR( SEQ( AND( XOR( AND( LEAF: B+complete , LEAF: D+complete ) , LEAF: B+complete ) , LEAF: C+complete ) , XOR( LEAF: E+complete , LEAF: F+complete ) , XOR( LEAF: G+complete , LEAF: tau ) ) , SEQ( LEAF: B1+complete , XOR( SEQ( LEAF: D2+complete , LEAF: B2+complete ) , SEQ( LEAF: D+complete , LEAF: tau , LEAF: B2+complete ) ) , LEAF: C+complete , XOR( LEAF: tau ) , XOR( LEAF: E+complete , LEAF: F+complete ) ) ) ) [ ] 0.9523 (Of: 0.9523 Fr: 1.0000 Gv: 0.4548 Pe: 0.8511 Su: 0.9394 Er: 0.9706 )"
				//
				});

		trees.put(
				ConfigurationApproaches.BLACK, // 1 tree, Mind (eg configured)
				new String[] { "SEQ( LEAF: A+complete , XOR( SEQ( LEAF: B1+complete , XOR( LEAF: D+complete , LEAF: tau , LEAF: D2+complete ) , LEAF: B2+complete , LEAF: C+complete , XOR( LEAF: D2+complete , LEAF: tau ) ) , AND( XOR( AND( LEAF: B+complete , LEAF: D+complete ) , LEAF: B+complete ) , LEAF: C+complete ) ) , XOR( SEQ( LEAF: F+complete , XOR( LEAF: G+complete , LEAF: tau ) ) , LEAF: E+complete ) ) [[-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -][-, -, -, -, -, -, -, -, -, -, -, -, -, -, >, -, -, -, -, -, -, -, -, -, -, -, -, -][-, -, -, -, -, -, -, -, -, -, -, -, -, -, <, -, -, B, -, -, -, -, -, -, -, -, -, -][-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -] ] 0.9284 (Gv: 0.4939 Fr: 0.9944 Pe: 0.7941 Of: 0.9284 Su: 0.9832 Cf: 0.9286 )" });

		trees.put(
				ConfigurationApproaches.GREEN,
				new String[] { "SEQ( LEAF: A+complete , XOR( AND( LEAF: C+complete , LEAF: D+complete , LEAF: B+complete ) , SEQ( LEAF: B1+complete , XOR( LEAF: D2+complete , LEAF: D+complete ) , LEAF: B2+complete , LEAF: C+complete , LEAF: D2+complete ) ) , XOR( LEAF: E+complete , SEQ( LEAF: F+complete , LEAF: G+complete ) ) ) [[-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -][-, -, -, -, -, -, -, -, -, -, -, B, -, -, -, -, -, -, -, H][-, -, -, >, -, H, -, -, -, -, -, -, -, B, -, -, -, -, -, -][-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -] ] 0.9469 (Su: 0.9462 Fr: 0.9568 Of: 0.9471 Cf: 0.7500 Pe: 0.9339 Gv: 0.6464 )" });

		parse_config_standard(logs, baseLatexCode, trees);
	}

	public static void parse_config_wabo() {
		ArrayList<XLog> logs = loadConfigCaseStudyLogs();
		String baseLatexCode = "discConfigurable_caseStudy";

		/*
		 * Lets add the default tree strings such that they do not need to be
		 * entered every time
		 */
		Map<ConfigurationApproaches, String[]> trees = new HashMap<ConfigurationApproaches, String[]>();
		//RESULTS of 14-05-09_10_22_37

		trees.put(
				ConfigurationApproaches.BLUE, //n trees
				new String[] {
						"XOR( SEQ( XOR( SEQ( LEAF: 01_BB_540+complete , AND( XOR( LEAF: 01_BB_546+complete , LEAF: tau ) , LEAF: 01_BB_590+complete ) , XOR( SEQ( LEAF: 01_BB_550+complete , LEAF: 01_BB_560+complete ) , SEQ( LEAF: 01_BB_550_1+complete , LEAF: 01_BB_550_2+complete ) ) ) , LEAF: 01_BB_770+complete ) , LEAF: 01_BB_630+complete , XOR( SEQ( XOR( LEAF: 01_BB_730+complete , LEAF: tau ) , XOR( LEAF: 01_BB_730+complete , LEAF: 01_BB_770+complete ) ) , SEQ( AND( LEAF: 01_BB_730+complete , LEAF: 01_BB_740+complete ) , XOR( SEQ( LEAF: 01_BB_755+complete , LEAF: 01_BB_766+complete ) , LEAF: 01_BB_760+complete ) ) ) ) , AND( SEQ( LEAF: 01_BB_540+complete , XOR( SEQ( XOR( LEAF: tau ) ) , LEAF: 01_BB_765+complete ) ) , LEAF: 01_BB_770+complete ) ) [ ] 0.9786 (Pe: 0.9757 Fr: 0.9885 Of: 0.9786 Su: 0.9556 Gv: 0.3546 )",
						"XOR( LEAF: 01_BB_630+complete , SEQ( XOR( LEAF: 01_BB_630+complete , LEAF: tau ) , LEAF: 01_BB_755+complete ) , LEAF: 01_BB_550_1+complete , OR( XOR( LEAF: 01_BB_765+complete , LEAF: 01_BB_770+complete ) , XOR( SEQ( XOR( LEAF: 01_BB_630+complete , LEAF: 01_BB_540+complete , SEQ( LEAF: 01_BB_540+complete , OR( AND( SEQ( LEAF: 01_BB_560+complete , LEAF: 01_BB_630+complete ) , LEAF: 01_BB_590+complete ) , LEAF: 01_BB_550+complete ) ) ) , XOR( LEAF: 01_BB_730+complete , LEAF: 01_BB_770+complete , SEQ( LEAF: 01_BB_770+complete , XOR( SEQ( LEAF: 01_BB_670+complete , LEAF: 01_BB_680+complete ) , LEAF: 01_BB_775+complete ) ) ) ) , LEAF: 01_BB_540+complete ) ) ) [ ] 0.9755 (Pe: 0.9743 Fr: 0.9782 Of: 0.9755 Su: 1.0000 Gv: 0.5198 )",
						"XOR( OR( SEQ( XOR( SEQ( LEAF: 01_BB_540+complete , XOR( LEAF: 01_BB_540+complete , LEAF: 01_BB_765+complete ) ) , AND( XOR( SEQ( LEAF: 01_BB_630+complete , XOR( SEQ( LEAF: 01_BB_640+complete , LEAF: 01_BB_650_1+complete , LEAF: 01_BB_650_2+complete ) , LEAF: 01_BB_730+complete , SEQ( XOR( LEAF: 01_BB_730+complete , LEAF: tau ) , XOR( LEAF: 01_BB_755+complete , LEAF: tau ) ) ) ) , LEAF: 01_BB_540+complete ) ) ) ) , LEAF: 01_BB_770+complete ) , LEAF: 01_BB_630+complete ) [ ] 0.9776 (Pe: 0.9545 Fr: 1.0000 Of: 0.9776 Su: 0.9310 Gv: 0.3530 )",
						"XOR( LEAF: 01_BB_630+complete , SEQ( LEAF: 01_BB_630+complete , XOR( SEQ( LEAF: 01_BB_730+complete , LEAF: 01_BB_755+complete ) , SEQ( LEAF: 01_BB_730+complete , LEAF: 01_BB_770+complete ) ) ) , SEQ( XOR( OR( LEAF: 01_BB_540+complete , LEAF: 01_BB_770+complete ) , LEAF: 01_BB_630+complete ) , XOR( LEAF: 01_BB_765+complete , LEAF: tau ) , XOR( LEAF: tau , LEAF: 01_BB_770+complete , AND( SEQ( LEAF: 01_BB_550+complete , LEAF: 01_BB_560+complete , OR( SEQ( LEAF: 01_BB_730+complete , XOR( LEAF: 01_BB_770+complete , LEAF: tau ) ) , LEAF: 01_BB_630+complete ) ) , LEAF: 01_BB_590+complete ) ) ) ) [ ] 0.9910 (Pe: 0.9804 Fr: 1.0000 Of: 0.9910 Su: 1.0000 Gv: 0.5308 )",
						"OR( AND( SEQ( XOR( LEAF: 01_BB_540+complete , AND( XOR( AND( XOR( SEQ( LEAF: 01_BB_540+complete , AND( LEAF: 01_BB_550+complete , LEAF: 01_BB_590+complete ) , LEAF: 01_BB_600+complete , LEAF: 01_BB_610+complete ) , LEAF: 01_BB_590+complete ) ) , LEAF: 01_BB_766+complete ) , SEQ( LEAF: 01_BB_550+complete , XOR( LEAF: tau , SEQ( XOR( LEAF: 01_BB_560+complete , LEAF: 01_BB_630+complete ) , AND( XOR( LEAF: 01_BB_730+complete , LEAF: 01_BB_640+complete , SEQ( LEAF: 01_BB_730+complete , AND( LEAF: 01_BB_740+complete , SEQ( LEAF: 01_BB_780_1+complete , LEAF: 01_BB_780_2+complete ) ) ) ) , XOR( SEQ( XOR( SEQ( LEAF: 01_BB_780_3+complete , LEAF: 01_BB_790+complete ) , SEQ( LEAF: 01_BB_755+complete , XOR( LEAF: 01_BB_760+complete , LEAF: tau ) ) ) , LEAF: 01_BB_630+complete ) , LEAF: 01_BB_630+complete , SEQ( LEAF: 01_BB_630+complete , LEAF: 01_BB_670+complete , AND( SEQ( LEAF: 01_BB_680+complete , LEAF: 01_BB_700+complete ) , LEAF: 01_BB_730+complete ) ) ) ) ) , LEAF: 01_BB_560+complete ) ) ) , SEQ( LEAF: 01_BB_630+complete , XOR( LEAF: 01_BB_730+complete , LEAF: tau ) ) ) , XOR( LEAF: 01_BB_765+complete , LEAF: tau ) ) ) , LEAF: 01_BB_770+complete ) [ ] 0.9772 (Pe: 0.9673 Fr: 0.9880 Of: 0.9772 Su: 0.9706 Gv: 0.4584 )"
				//
				});
		trees.put(
				ConfigurationApproaches.RED, //n + 1, first merged/ Mstar
				new String[] {
						"OR( XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_765+complete , AND( SEQ( XOR( SEQ( LEAF: 01_BB_550+complete , OR( XOR( SEQ( AND( LEAF: 01_BB_600+complete , LEAF: 01_BB_590+complete ) , XOR( OR( LEAF: 01_BB_550+complete , LEAF: 01_BB_610+complete ) , LEAF: 01_BB_610+complete ) ) , LEAF: 01_BB_546+complete , LEAF: 01_BB_590+complete ) , XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_560+complete , LEAF: 01_BB_550_1+complete ) ) ) , LEAF: 01_BB_765+complete ) , XOR( OR( XOR( LEAF: 01_BB_775+complete , LEAF: 01_BB_765+complete , AND( XOR( AND( SEQ( LEAF: 01_BB_780_3+complete , LEAF: 01_BB_790+complete ) , SEQ( LEAF: 01_BB_780_1+complete , LEAF: 01_BB_780_2+complete ) ) , LEAF: 01_BB_760+complete ) , LEAF: 01_BB_740+complete ) , LEAF: 01_BB_730+complete ) , AND( LEAF: 01_BB_630+complete , XOR( SEQ( LEAF: 01_BB_755+complete , LEAF: 01_BB_766+complete ) , OR( SEQ( LEAF: 01_BB_670+complete , OR( LEAF: 01_BB_680+complete , LEAF: 01_BB_700+complete ) ) , XOR( LEAF: 01_BB_640+complete , LEAF: tau ) ) ) ) ) , LEAF: tau ) ) , XOR( LEAF: 01_BB_590+complete , LEAF: 01_BB_770+complete ) ) ) , XOR( SEQ( LEAF: 01_BB_630+complete , XOR( LEAF: 01_BB_730+complete , SEQ( LEAF: 01_BB_640+complete , LEAF: 01_BB_650_1+complete , LEAF: 01_BB_650_2+complete ) , LEAF: tau ) ) , LEAF: 01_BB_540+complete ) ) [ ] 0.9795 (Su: 1.0000 Pe: 0.9721 Fr: 0.9856 Of: 0.9795 Gv: 0.5276 )",
						"AND( XOR( AND( XOR( LEAF: 01_BB_770+complete , LEAF: tau ) ) , AND( SEQ( XOR( SEQ( LEAF: 01_BB_550+complete , OR( XOR( SEQ( AND( LEAF: 01_BB_600+complete , LEAF: 01_BB_590+complete ) , XOR( OR( LEAF: 01_BB_550+complete , LEAF: 01_BB_610+complete ) , LEAF: 01_BB_610+complete ) ) , LEAF: 01_BB_546+complete , LEAF: 01_BB_590+complete ) , XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_560+complete , LEAF: 01_BB_550_1+complete ) ) ) , LEAF: 01_BB_765+complete ) , XOR( OR( XOR( LEAF: 01_BB_775+complete , LEAF: 01_BB_765+complete , AND( XOR( AND( SEQ( LEAF: 01_BB_780_3+complete , LEAF: 01_BB_790+complete ) , SEQ( LEAF: 01_BB_780_1+complete , LEAF: 01_BB_780_2+complete ) ) , LEAF: 01_BB_760+complete ) , LEAF: 01_BB_740+complete ) , LEAF: 01_BB_730+complete ) , AND( LEAF: 01_BB_630+complete , LEAF: 01_BB_550_2+complete , XOR( SEQ( LEAF: 01_BB_740+complete , LEAF: 01_BB_755+complete , LEAF: 01_BB_766+complete ) , OR( SEQ( LEAF: 01_BB_670+complete , OR( LEAF: 01_BB_680+complete , LEAF: 01_BB_700+complete ) ) , XOR( LEAF: 01_BB_640+complete , LEAF: tau ) ) ) ) ) , LEAF: tau ) ) , XOR( LEAF: 01_BB_590+complete , LEAF: 01_BB_770+complete ) ) ) , AND( XOR( SEQ( LEAF: 01_BB_630+complete , XOR( LEAF: 01_BB_730+complete , SEQ( LEAF: 01_BB_640+complete , LEAF: 01_BB_650_1+complete , LEAF: 01_BB_650_2+complete ) , LEAF: tau ) ) , LEAF: 01_BB_540+complete ) ) ) [ ] 0.9447 (Su: 0.9747 Pe: 0.8520 Fr: 0.9908 Of: 0.9447 Gv: 0.1896 Er: 0.9542 )",
						"OR( XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_765+complete , AND( SEQ( XOR( SEQ( LEAF: 01_BB_550+complete , OR( XOR( SEQ( AND( LEAF: 01_BB_600+complete , LEAF: 01_BB_590+complete ) , XOR( OR( LEAF: 01_BB_550+complete , LEAF: 01_BB_610+complete ) , LEAF: 01_BB_610+complete ) ) , LEAF: 01_BB_546+complete , LEAF: 01_BB_590+complete ) , XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_560+complete , LEAF: 01_BB_550_1+complete ) ) ) ) , XOR( OR( XOR( LEAF: 01_BB_775+complete , LEAF: 01_BB_765+complete , AND( XOR( AND( SEQ( LEAF: 01_BB_780_3+complete , LEAF: 01_BB_790+complete ) , SEQ( LEAF: 01_BB_780_1+complete , LEAF: 01_BB_780_2+complete ) ) , LEAF: 01_BB_760+complete ) , LEAF: 01_BB_740+complete ) , LEAF: 01_BB_730+complete ) , AND( LEAF: 01_BB_630+complete , XOR( SEQ( LEAF: 01_BB_755+complete , LEAF: 01_BB_766+complete ) , OR( SEQ( LEAF: 01_BB_670+complete , OR( LEAF: 01_BB_680+complete , LEAF: 01_BB_700+complete ) ) , XOR( LEAF: 01_BB_640+complete , LEAF: tau ) ) ) ) ) , LEAF: tau ) ) ) ) , XOR( SEQ( LEAF: 01_BB_630+complete , XOR( LEAF: 01_BB_730+complete , SEQ( LEAF: 01_BB_640+complete , LEAF: 01_BB_650_1+complete , LEAF: 01_BB_650_2+complete ) , LEAF: tau ) ) , LEAF: 01_BB_540+complete ) ) [ ] 0.9608 (Su: 0.9714 Pe: 0.9532 Fr: 0.9649 Of: 0.9608 Gv: 0.2622 Er: 0.9722 )",
						"OR( AND( XOR( XOR( SEQ( LEAF: 01_BB_770+complete , SEQ( XOR( SEQ( LEAF: 01_BB_550+complete , OR( XOR( SEQ( AND( LEAF: 01_BB_600+complete , LEAF: 01_BB_590+complete ) , XOR( OR( LEAF: 01_BB_550+complete , LEAF: 01_BB_610+complete ) , LEAF: 01_BB_610+complete ) ) , LEAF: 01_BB_546+complete , LEAF: 01_BB_590+complete ) , XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_560+complete , LEAF: 01_BB_550_1+complete ) ) ) , LEAF: 01_BB_765+complete ) , XOR( OR( XOR( LEAF: 01_BB_775+complete , LEAF: 01_BB_765+complete , AND( XOR( AND( SEQ( LEAF: 01_BB_780_3+complete , LEAF: 01_BB_790+complete ) , SEQ( LEAF: 01_BB_780_1+complete , LEAF: 01_BB_780_2+complete ) ) , LEAF: 01_BB_760+complete ) , LEAF: 01_BB_740+complete ) , LEAF: 01_BB_730+complete ) , AND( LEAF: 01_BB_630+complete , XOR( SEQ( LEAF: 01_BB_755+complete , LEAF: 01_BB_766+complete ) , OR( SEQ( LEAF: 01_BB_670+complete , OR( LEAF: 01_BB_680+complete , LEAF: 01_BB_700+complete ) ) , XOR( LEAF: 01_BB_640+complete , LEAF: tau ) ) ) ) ) , LEAF: tau ) ) ) , LEAF: 01_BB_540+complete ) , XOR( LEAF: 01_BB_770+complete ) ) ) , XOR( SEQ( LEAF: 01_BB_630+complete , XOR( OR( LEAF: 01_BB_730+complete , LEAF: 01_BB_755+complete ) , SEQ( LEAF: 01_BB_640+complete , LEAF: 01_BB_650_1+complete , LEAF: 01_BB_650_2+complete ) , LEAF: tau ) ) , LEAF: 01_BB_540+complete ) ) [ ] 0.9554 (Su: 0.9351 Pe: 0.9027 Fr: 1.0000 Of: 0.9554 Gv: 0.0921 Er: 0.9404 )",
						"OR( XOR( LEAF: 01_BB_770+complete , XOR( LEAF: 01_BB_765+complete , AND( SEQ( XOR( SEQ( LEAF: 01_BB_550+complete , OR( XOR( SEQ( AND( LEAF: 01_BB_600+complete , LEAF: 01_BB_590+complete ) , XOR( OR( LEAF: 01_BB_550+complete , LEAF: 01_BB_610+complete ) , LEAF: 01_BB_610+complete ) ) , LEAF: 01_BB_546+complete , LEAF: 01_BB_590+complete ) , XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_560+complete , LEAF: 01_BB_550_1+complete ) ) ) , LEAF: 01_BB_770+complete , LEAF: 01_BB_765+complete ) , XOR( OR( XOR( LEAF: 01_BB_775+complete , LEAF: 01_BB_765+complete , AND( XOR( AND( SEQ( LEAF: 01_BB_780_3+complete , LEAF: 01_BB_790+complete ) , SEQ( LEAF: 01_BB_780_1+complete , LEAF: 01_BB_780_2+complete ) ) , LEAF: 01_BB_760+complete ) , LEAF: 01_BB_740+complete ) , LEAF: 01_BB_730+complete ) , AND( LEAF: 01_BB_630+complete , XOR( SEQ( LEAF: 01_BB_755+complete , LEAF: 01_BB_766+complete ) , OR( SEQ( LEAF: 01_BB_670+complete , OR( LEAF: 01_BB_680+complete , LEAF: 01_BB_700+complete ) ) , XOR( LEAF: 01_BB_640+complete , LEAF: tau ) ) ) ) ) , LEAF: tau ) ) , XOR( LEAF: 01_BB_590+complete , LEAF: 01_BB_770+complete ) ) ) ) , XOR( SEQ( LEAF: 01_BB_630+complete , XOR( LEAF: 01_BB_730+complete , SEQ( LEAF: 01_BB_640+complete , LEAF: 01_BB_650_1+complete , LEAF: 01_BB_650_2+complete ) , LEAF: tau ) ) , LEAF: 01_BB_540+complete ) ) [ ] 0.9713 (Su: 0.9868 Pe: 0.9119 Fr: 0.9984 Of: 0.9713 Gv: 0.3035 Er: 0.9867 )",
						"OR( XOR( LEAF: 01_BB_770+complete , AND( OR( SEQ( XOR( SEQ( LEAF: 01_BB_550+complete , OR( XOR( SEQ( AND( LEAF: 01_BB_600+complete , LEAF: 01_BB_590+complete ) , XOR( OR( LEAF: 01_BB_550+complete , LEAF: 01_BB_610+complete ) , LEAF: 01_BB_610+complete ) ) , LEAF: 01_BB_546+complete , LEAF: 01_BB_590+complete ) , XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_560+complete , LEAF: 01_BB_550_1+complete ) ) ) , LEAF: 01_BB_765+complete ) , XOR( OR( XOR( LEAF: 01_BB_775+complete , LEAF: 01_BB_765+complete , AND( XOR( AND( SEQ( LEAF: 01_BB_780_3+complete , LEAF: 01_BB_790+complete ) , SEQ( LEAF: 01_BB_780_1+complete , LEAF: 01_BB_780_2+complete ) ) , LEAF: 01_BB_760+complete ) , LEAF: 01_BB_740+complete ) , LEAF: 01_BB_730+complete ) , AND( LEAF: 01_BB_630+complete , XOR( SEQ( LEAF: 01_BB_755+complete , LEAF: 01_BB_766+complete ) , OR( SEQ( LEAF: 01_BB_670+complete , OR( LEAF: 01_BB_680+complete , LEAF: 01_BB_700+complete ) ) , XOR( LEAF: 01_BB_640+complete , LEAF: tau ) ) ) ) ) , LEAF: tau ) ) , XOR( LEAF: 01_BB_770+complete ) ) ) ) , XOR( SEQ( LEAF: 01_BB_630+complete , XOR( LEAF: 01_BB_730+complete , SEQ( LEAF: 01_BB_640+complete , LEAF: 01_BB_650_1+complete , LEAF: 01_BB_650_2+complete ) , LEAF: tau ) ) , LEAF: 01_BB_540+complete ) ) [ ] 0.9746 (Su: 0.9726 Pe: 0.9470 Fr: 0.9915 Of: 0.9746 Gv: 0.4347 Er: 0.9796 )"
				//
				});

		trees.put(
				ConfigurationApproaches.BLACK, // 1 tree, Mind (eg configured)
				new String[] { "OR( XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_765+complete , OR( SEQ( LEAF: 01_BB_550+complete , XOR( LEAF: tau , OR( XOR( OR( SEQ( LEAF: 01_BB_600+complete , LEAF: 01_BB_610+complete ) , LEAF: 01_BB_740+complete , LEAF: 01_BB_770+complete ) , LEAF: 01_BB_630+complete ) , LEAF: 01_BB_560+complete ) , LEAF: 01_BB_755+complete ) ) , XOR( SEQ( LEAF: 01_BB_550_1+complete , LEAF: 01_BB_755+complete , LEAF: 01_BB_766+complete ) , LEAF: 01_BB_590+complete ) ) ) , XOR( LEAF: 01_BB_540+complete , AND( LEAF: 01_BB_540+complete , LEAF: 01_BB_770+complete ) , OR( LEAF: 01_BB_630+complete , LEAF: 01_BB_730+complete ) ) ) [[a, -, -, -, <, -, -, -, -, a, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, >, -, -][-, -, -, -, B, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, >, -, -, >, -, -][-, -, -, -, -, -, -, -, -, >, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, <, -, -, a, -, -][-, -, -, -, >, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, >, -, -, x, -, -][-, -, -, -, >, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, >, -, -, a, -, -] ] 0.9464 (Su: 0.9719 Cf: 0.8529 Of: 0.9465 Fr: 0.9612 Gv: 0.5386 Pe: 0.9201 )" });

		trees.put(
				ConfigurationApproaches.GREEN,
				new String[] { "SEQ( XOR( LEAF: 01_BB_630+complete , LEAF: 01_BB_540+complete , SEQ( XOR( LEAF: 01_BB_770+complete , LEAF: 01_BB_540+complete , SEQ( LEAF: 01_BB_630+complete , LEAF: 01_BB_730+complete , LEAF: 01_BB_755+complete ) ) , XOR( LEAF: tau , LEAF: 01_BB_540+complete , SEQ( LEAF: 01_BB_550+complete , LEAF: 01_BB_560+complete , LEAF: 01_BB_630+complete , LEAF: 01_BB_730+complete ) ) ) , SEQ( LEAF: 01_BB_630+complete , LEAF: 01_BB_730+complete ) ) , XOR( LEAF: tau , LEAF: 01_BB_765+complete ) , XOR( LEAF: tau , LEAF: 01_BB_770+complete ) ) [[-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -][-, -, -, -, -, -, -, -, -, -, H, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -][-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -][-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -][-, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -, -] ] 0.9665 (Pe: 0.9675 Su: 1.0000 Cf: 0.9655 Of: 0.9665 Fr: 0.9656 Gv: 0.6718 )" });

		parse_config_standard(logs, baseLatexCode, trees);
	}

	/**
	 * Processes the configuration experiments, all 5 approaches.
	 * 
	 * @param logs
	 *            Event logs used in the experiments
	 * @param baseLatexCode
	 *            Prefix for latex labels
	 * @param standardTrees
	 *            null or an array of 'standard' trees for given configuration
	 *            approaches, such that they do not need to be entered.
	 */
	public static void parse_config_standard(List<XLog> logs, String baseLatexCode,
			Map<ConfigurationApproaches, String[]> standardTrees) {
		/**
		 * Standard string for subfigures. Takes the following String format
		 * parameters: float: width, string: content of the subfigure, string:
		 * figure caption, int: approach index, string: label suffix (e.g.
		 * model1)
		 */
		String latexSubfigureStd = "\t\\begin{subfigure}{%1.2f\\textwidth}%n" + "\t\t\\centering%n"
				+
				//"\t\t\\begin{tikzpicture}%n" +
				"\t\t\t%s%n"
				+ //tree tikz
				"\t\t\\caption{%s}%n" + "\t\t\\label{fig:discConfigurable_runEx_approach%dResults_%s}%n"
				+ "\t\\end{subfigure}%n";

		XLog[] logArray = new XLog[logs.size()];
		logs.toArray(logArray);
		CentralRegistryConfigurable registry = new CentralRegistryConfigurable(XLogInfoImpl.STANDARD_CLASSIFIER,
				new Random(), logArray);

		/*
		 * Ask which approach or code should be processed
		 */
		System.out
				.println(" Please specify which approaches to parse, space seperated, either by index or code. Or just press ENTER for all!");
		for (int i = 0; i < ConfigurationApproaches.values().length; i++) {
			System.out.println(i + ". " + ConfigurationApproaches.values()[i]);
		}
		Scanner in = new Scanner(System.in);
		String line = in.nextLine();

		ConfigurationApproaches[] approachesToParse = null;
		if (!line.trim().isEmpty()) {
			String[] approaches = line.split(" ");
			approachesToParse = new ConfigurationApproaches[approaches.length];
			for (int i = 0; i < approaches.length; i++) {
				//Test if by number or by code
				ConfigurationApproaches currentApproach = null;
				try {
					currentApproach = ConfigurationApproaches.values()[Integer.parseInt(approaches[i])];
				} catch (NumberFormatException nfe) {
					currentApproach = ConfigurationApproaches.valueOf(approaches[i].toUpperCase());
				}
				approachesToParse[i] = currentApproach;
			}
		} else {
			//Empty line
			approachesToParse = ConfigurationApproaches.values();
		}

		/*
		 * Process each approach
		 */
		APPROACH: for (ConfigurationApproaches currentApproach : approachesToParse) {
			int currentApproachIndex = currentApproach.ordinal() + 1;
			String latexCode = baseLatexCode + "_approach" + currentApproachIndex + "Results";

			System.out.println("Starting processing of approach " + currentApproach + " (approach"
					+ currentApproachIndex + ")");

			//Then ask for the input trees (one per line, empty line ends)
			int nrTreesExpected = logs.size();
			switch (currentApproach) {
				case BLUE :
					System.out.println("Please provide the NAryTree strings that were the intermediate results.");
					break;
				case RED :
					nrTreesExpected++; // we also require the merged process tree.
					System.out
							.println("Please provide first the merged process tree followed by the intermediate/individualized results.");
					break;
				case BLACK :
				case GREEN :
					nrTreesExpected = 1;
					System.out.println("Please provide the discovered configurable process tree.");
					break;
				case GREEN_PARETO :
					nrTreesExpected = 1; //We do not know how many trees
					System.out.println("Please provide 1 configurable process tree from the Pareto front.");
					break;
			}
			System.out.println("We expect " + nrTreesExpected + (nrTreesExpected == 1 ? " tree" : " trees")
					+ " in total.");
			boolean allowStandardTrees = false;
			if (standardTrees != null && standardTrees.containsKey(currentApproach)
					&& standardTrees.get(currentApproach).length == nrTreesExpected) {
				allowStandardTrees = true;
				System.out.println("You can also just press ENTER and we will use the provided standard trees.");
			}

			String[] treeStrings = new String[nrTreesExpected];
			boolean parseStandardTrees = false;
			for (int i = 0; i < nrTreesExpected; i++) {
				//Detect 'enter'
				if (i == 0) {
					String thisLine = in.nextLine();
					if (allowStandardTrees && thisLine.isEmpty()) {
						parseStandardTrees = true;
						treeStrings[i] = standardTrees.get(currentApproach)[i];
					} else {
						treeStrings[i] = thisLine;
					}
				} else {
					//non first lines
					if (parseStandardTrees) {
						treeStrings[i] = standardTrees.get(currentApproach)[i];
					} else {
						treeStrings[i] = in.nextLine();
					}
				}

			}
			System.out.println("Instantiated trees:");
			ProbProcessArrayTree[] trees = new ProbProcessArrayTree[treeStrings.length];
			for (int i = 0; i < trees.length; i++) {
				trees[i] = TreeUtils.fromString(treeStrings[i], registry.getEventClasses());
				System.out.println(TreeUtils.toString(trees[i], registry.getEventClasses()));
			}

			String output = "";

			int treeOffset = 0; //index of first tree to merge
			//For RED/2, first output the merged process tree
			if (currentApproach == ConfigurationApproaches.RED) {
				output += String.format(latexSubfigureStd, 0.99,
						PaperMethods.naryTreeToLatex(trees[0], registry.getEventClasses()),
						"Process model discovered from combined event log ", currentApproachIndex, "commonmodel")
						+ "\t\\\\";
				treeOffset = 1;
			}

			/*
			 * Merge trees and output TikZ code in case of approach BLUE/1 or
			 * RED/2
			 */
			ProbProcessArrayTree configuredTree = null;
			if (currentApproach == ConfigurationApproaches.BLUE || currentApproach == ConfigurationApproaches.RED) {
				//FIXME now depends on unreleased PTMerge package with manual fix to disable assertion
				ProcessTree merged = NAryTreeToProcessTree.convert(trees[treeOffset], registry.getEventClasses());
				for (int t = treeOffset + 1; t < trees.length; t++) {
					ProcessTree currentProcessTree = NAryTreeToProcessTree
							.convert(trees[t], registry.getEventClasses());
					try {
						merged = PlugIn.mergeDAGSActMap(null, merged, currentProcessTree);
					} catch (AssertionError e) {
						System.err.println("ERROR IN MERGE DAGS ACT - manually process files!!! ");
						System.exit(0); //do not produce final results, but terminate as-if OK
					}
				}

				ProcessTreeImpl mergedImpl = ((ProcessTreeImpl) merged);

				//			String latex = PaperMethods.processTreeToLatex(mergedImpl);
				//			System.out.println(latex);

				ProcessTreeToNAryTree converter = new ProcessTreeToNAryTree(registry.getEventClasses());
				configuredTree = converter.convert(mergedImpl);
				//			System.out.println(PaperMethods.naryTreeToLatex(mergedTree, registry.getEventClasses(), registry));

				/*
				 * For BLUE and RED (1 and 2) we know that the resulting merged
				 * trees is a XOR of the originals. However, no configurations
				 * are discovered, but we know what the best approach is.
				 */
				/*
				 * Unfortunately, the resulting process tree has the children in
				 * an unpredictable order... We want them in the same order as
				 * the event logs, for clarity of the end result. We do this by
				 * subtree size. NOTE: this might seems too hard-coded, but this
				 * is the only way to get understandable and consistent
				 * results...
				 */
				int[] childIndexes = new int[logs.size()];
				for (int c = 0; c < configuredTree.nChildren(0); c++) {
					int currNode = configuredTree.getChildAtIndex(0, c);
					//int currSize = configuredTree.size(currNode);

					//Look up the tree to string in the list of input trees to find the index
					String subtreeString = TreeUtils.toString(configuredTree, currNode, registry.getEventClasses());
					subtreeString = subtreeString.substring(0, subtreeString.lastIndexOf(')') + 1);

					for (int t = treeOffset; t < treeStrings.length; t++) {
						String treeString = treeStrings[t].substring(0, treeStrings[t].lastIndexOf('[')).trim();
						if (treeString.equalsIgnoreCase(subtreeString)) {
							childIndexes[t - treeOffset] = c;
						}
					}
				}

				System.out.println("We found the following mapping to fix the order of the merged trees:");
				System.out.println(Arrays.toString(childIndexes));

				ProbProcessArrayTree originalTree = new ProbProcessArrayTreeImpl(configuredTree);
				//				configuredTree = new NAryTreeImpl(new int[] { 1 }, new short[] { NAryTree.XOR },						new int[] { NAryTree.NONE });
				for (int i = 0; i < logs.size(); i++) {
					configuredTree = configuredTree.remove(0, i);
					configuredTree = configuredTree.add(originalTree, originalTree.getChildAtIndex(0, childIndexes[i]),
							0, i);
				}

				//Now we can add blocks
				for (int i = 0; i < logs.size(); i++) {
					configuredTree.addConfiguration(new Configuration(configuredTree.size()));
				}
				for (int c = 0; c < configuredTree.nChildren(0); c++) {
					int index = configuredTree.getChildAtIndex(0, c);

					for (int config = 0; config < logs.size(); config++) {
						if (config != c) {
							configuredTree.setNodeConfiguration(config, index, Configuration.BLOCKED);
						}
					}
				}

				// .each of the individual trees
				for (int i = treeOffset; i < trees.length; i++) {
					int variant = i + 1 - treeOffset;
					output += String.format(latexSubfigureStd, 0.49f,
							PaperMethods.naryTreeToLatex(trees[i], registry.getEventClasses()),
							"Process model mined on event log " + variant, currentApproachIndex, "model" + variant);

					/*-* /
					  \begin{subfigure}{.49\textwidth}
					    \centering
					    \begin{tikzpicture}
					      % SEQ( LEAF: A+complete , LEAF: B1+complete , LEAF: B2+complete , LEAF: C+complete , LEAF: D2+complete , XOR( LEAF: E+complete , LEAF: F+complete ) ) [ ] 0.9992 (Su: 1.0000 Of: 0.9992 Fr: 1.0000 Pe: 1.0000 Gv: 0.8665 )
					      \Tree[.\node(n0){\opSeq}; \node(n1){A}; \node(n2){B1}; \node(n3){B2}; \node(n4){C}; \node(n5){D2}; [.\node(n6){\opXor}; \node(n7){E}; \node(n8){F};  ]  ]
					      \node[above=of n0, anchor=south, outer ysep=-18pt]{\qualityTableCommands
					      \begin{tabular}{|cc|cc|} \hline f: & 1.000 & p: & 1.000\\ \hline s: & 1.000 & g: & 0.866\\ \hline \end{tabular}};
					    \end{tikzpicture}
					    \caption{Process model mined on event log 2}
					    \label{fig:discConfigurable_runEx_approach1Results_model2}
					  \end{subfigure}
					/**/

					if (variant % 2 == 0 && variant <= trees.length - 1) {
						output += String.format("\t\t\\\\%n");
					} else {
						output += String.format("\t\t\\hfill%n");
					}
				}

			} else {
				//Approaches 3-5 (black/green/green_pareto) only have a configurable process tree as input
				configuredTree = trees[treeOffset];
			}

			System.out.println("Merged configured Tree:");
			System.out.println(TreeUtils.toString(configuredTree, registry.getEventClasses()));

			// add the merged tree to the output
			output += String.format(
					"" + latexSubfigureStd,
					0.99,
					PaperMethods.naryTreeToLatex(configuredTree, registry.getEventClasses()).replaceAll(
							String.format("%n"), String.format("%n\t\t\t\t")), "Configurable process tree",
					currentApproachIndex, "configurableModel");

			/*
			 * Construct overview table
			 */
			// . of overall and all individual Of and 4Q
			// . also size, #CP and similarity between configured models

			/*-
			  \begin{subfigure}{\textwidth}
			    \centering
			    \smallTableFontSize
			    \begin{tabular}{|c|c|c|c|c|c|p{0.3cm}|c|c|c|} \hhline{------~---}
			      & Overall & Fitness & Precision & Simplicity & Generalization & & Size & \#C.P. & Similarity \\ \hhline{------~---}
			      Combined & 0.989 & 0.999 & 0.999 & 0.981 & 0.220 & & 53 & 4 & - \\ \hhline{------~---}
			      Variant 0& 0.986 & 0.995 & 0.995 & 0.981 & 0.235 & & 14 & 3 & 0.418 \\ \hhline{------~---}
			      Variant 1& 0.989 & 1.000 & 1.000 & 0.981 & 0.263 & & 16 & 3 & 0.464 \\ \hhline{------~---}
			      Variant 2& 0.989 & 1.000 & 1.000 & 0.981 & 0.174 & & 10 & 3 & 0.317 \\ \hhline{------~---}
			      Variant 3& 0.989 & 1.000 & 1.000 & 0.981 & 0.264 & & 16 & 3 & 0.464 \\ \hhline{------~---}
			    \end{tabular}
			    \caption{Quality statistics of the configurable process model of (e)}
			    \label{tab:discConfigurable_runEx_approach1Results_stats}
			  \end{subfigure}
			/**/

			String latexCompareTable = String
					.format("\t\\begin{subfigure}{\\textwidth}%n"
							+ "\t\t\\centering%n"
							+ "\t\t\\smallTableFontSize%n"

							+ "\t\t\\begin{tabular}{ccccccp{0.3cm}ccc} "
							+ "\t\t\\toprule%n"
							+ "\t\t\t& Overall & Fitness & Precision & Simplicity & Generalization & & Size & \\#C.P. & Similarity \\\\%n"
							+ "\\cmidrule(r){1-6}   \\cmidrule(l){8-10}%n");

			ConfigurationFitness cf = getConfigurationFitness(configuredTree, registry, standardWeights);
			cf.getFitness(configuredTree, null);

			//Create the 'combined' line
			latexCompareTable += "\t\t\tCombined & " + constructComparisonTableLine(configuredTree, registry, -1);

			for (int c = 0; c < logs.size(); c++) {
				latexCompareTable += "\t\t\tVariant " + (c + 1) + " & "
						+ constructComparisonTableLine(configuredTree, registry, c);
			}
			latexCompareTable += String.format("\t\t\\bottomrule%n\t\\end{tabular}%n"
					+ "\t\\caption{Quality statistics of the configurable process model of (e)}%n"
					+ "\t\\label{tab:%s_stats}" + "%n\t\\end{subfigure}%n", latexCode);

			// . The comparison table we constructed

			output += String.format("\\\\%n\t");
			output += latexCompareTable;

			output = output.replaceAll("01_BB_", "");
			output = output.replaceAll("01\\\\_BB\\\\_", "");

			// . And put it on the clipboard :)
			ETMUtils.onClipboard(output);

			/*
			 * Wait for user input to go to the next approach (otherwise
			 * clipboard content is cleared)
			 */
			System.out.println("We have put the code for experiment " + currentApproach + " (approach "
					+ currentApproachIndex + ") on the clipboard.");
			System.out.println("Please press enter when we can continue with the next approach.");
			in.nextLine();
		}
	}

	private static String constructComparisonTableLine(ProbProcessArrayTree configuredTree, CentralRegistryConfigurable registry,
			int configuration) {
		boolean applyConfiguration = (configuration >= 0);

		//configuredTree, the input, is the configured tree. 'tree' is the configured instance
		ProbProcessArrayTree tree = configuredTree;
		TreeFitness fitness = registry.getFitness(configuredTree);
		if (applyConfiguration) {
			tree = configuredTree.applyConfiguration(configuration);
			//			System.out.println("internal t" + configuration + " ");
			//			TreeUtils.printTree(tree);
			//			System.out.println("hash: " + tree.hashCode());
			fitness = registry.getRegistry(configuration).getFitness(tree);
		}

		//TreeUtils.printTree(tree);

		double of = fitness.getOverallFitnessValue();
		double fr = fitness.fitnessValues.get(FitnessReplay.info);
		double pe = fitness.fitnessValues.get(PrecisionEscEdges.info);
		double su = fitness.fitnessValues.get(SimplicityUselessNodes.info);
		double ge = fitness.fitnessValues.get(Generalization.info);

		int cp = TreeUtils.getNumberOfNodesConfigured(configuredTree);
		if (applyConfiguration) {
			cp = TreeUtils.getNumberOfNodesConfiguredForConfiguration(configuredTree, configuration);
		}
		int size = tree.size();
		//		int cp = tree.getNumberOfConfigurations();
		EditDistanceWrapperRTEDRelative ed = new EditDistanceWrapperRTEDRelative(new ProbProcessArrayTree[] { configuredTree });
		double sim = ed.getFitness(tree, null);

		return String.format("%1.3f & %1.3f & %1.3f & %1.3f & %1.3f & & %d & %d & %s \\\\%n", of, fr, pe, su, ge, size,
				cp, (applyConfiguration ? String.format("%1.3f", sim) : "-"));
	}

	private static ArrayList<XLog> loadConfigRunExLogs() {
		String[] filenames = new String[] { "000RunEx-Default.xez", "000RunEx-Config2.xez", "000RunEx-Config3.xez",
				"000RunEx-Config4.xez" };

		return loadLogs(filenames);
	}

	/**
	 * Instantiate the case study logs from the original files
	 * 
	 * @return
	 */
	public static ArrayList<XLog> loadConfigCaseStudyLogs() {

		String[] filenames = new String[] { "WABO1_01_BB.xes.gz", "WABO2_01_BB.xes.gz", "WABO3_01_BB.xes.gz",
				"WABO4_01_BB.xes.gz", "WABO5_01_BB.xes.gz" };

		return loadLogs(filenames);

	}

	/**
	 * Tries to load the provided list of filenames from the EventLogDirectory
	 * 
	 * @param filenames
	 *            list of files to be found in EventLogDirectory
	 * @return
	 */
	public static ArrayList<XLog> loadLogs(String[] filenames) {
		ArrayList<XLog> logs = new ArrayList<XLog>();

		XUniversalParser parser = new XUniversalParser();

		for (String filename : filenames) {
			File file = new File(EventLogDirectory + filename);
			if (parser.canParse(file)) {
				try {
					logs.add(parser.parse(file).iterator().next());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		return logs;
	}

	public static ConfigurationFitness getConfigurationFitness(ProbProcessArrayTree tree, CentralRegistryConfigurable registry,
			HashMap<TreeFitnessInfo, Double> weights) {

		TreeFitnessAbstract[] evaluators = new TreeFitnessAbstract[registry.getNrLogs()];
		for (int i = 0; i < registry.getNrLogs(); i++) {
			//LinkedHashMap<TreeFitnessAbstract, Double> alg = new LinkedHashMap<TreeFitnessAbstract, Double>();

			OverallFitness of = new OverallFitness(registry.getRegistry(i));
			of.addEvaluator(
			//new FitnessReplay(registry.getRegistry(i), ProMCancelTerminationCondition.buildDummyCanceller())
					/*-*/
					new FitnessReplay(registry.getRegistry(i), ProMCancelTerminationCondition.buildDummyCanceller(),
							ETMParamFactory.STD_REPLAYFITNESS_MAXF, ETMParamFactory.STD_REPLAYFITNESS_MAXTIME, true,
							-1, 1)
					/**/
					, weights.get(FitnessReplay.info));
			of.addEvaluator(new PrecisionEscEdges(registry.getRegistry(i)), weights.get(PrecisionEscEdges.info));
			of.addEvaluator(new Generalization(registry.getRegistry(i)), weights.get(Generalization.info));
			of.addEvaluator(new SimplicityUselessNodes(), weights.get(SimplicityUselessNodes.info));

			evaluators[i] = of;
		}

		return new ConfigurationFitness(registry, ETMParamFactory.STD_CONFIGURATION_ALPHA, false, evaluators);
	}

	/**
	 * Untar an input file into an output file.
	 * 
	 * The output file is created in the output folder, having the same name as
	 * the input file, minus the '.tar' extension.
	 * 
	 * SOURCE: http://stackoverflow.com/a/7556307/1478837
	 * 
	 * @param inputFile
	 *            the input .tar file
	 * @param outputDir
	 *            the output directory file.
	 * @throws IOException
	 * @throws FileNotFoundException
	 * 
	 * @return The {@link List} of {@link File}s with the untared content.
	 * @throws ArchiveException
	 * 
	 * 
	 */
	private static List<File> unTar(final File inputFile, final File outputDir) throws FileNotFoundException,
			IOException, ArchiveException {

		//	    LOG.info(String.format("Untaring %s to dir %s.", inputFile.getAbsolutePath(), outputDir.getAbsolutePath()));

		final List<File> untaredFiles = new LinkedList<File>();
		final InputStream is = new FileInputStream(inputFile);
		final TarArchiveInputStream debInputStream = (TarArchiveInputStream) new ArchiveStreamFactory()
				.createArchiveInputStream("tar", is);
		TarArchiveEntry entry = null;
		while ((entry = (TarArchiveEntry) debInputStream.getNextEntry()) != null) {
			final File outputFile = new File(outputDir, entry.getName());
			if (entry.isDirectory()) {
				//	            LOG.info(String.format("Attempting to write output directory %s.", outputFile.getAbsolutePath()));
				if (!outputFile.exists()) {
					//	                LOG.info(String.format("Attempting to create output directory %s.", outputFile.getAbsolutePath()));
					if (!outputFile.mkdirs()) {
						throw new IllegalStateException(String.format("Couldn't create directory %s.",
								outputFile.getAbsolutePath()));
					}
				}
			} else {
				//	            LOG.info(String.format("Creating output file %s.", outputFile.getAbsolutePath()));
				final OutputStream outputFileStream = new FileOutputStream(outputFile);
				IOUtils.copy(debInputStream, outputFileStream);
				outputFileStream.close();
			}
			untaredFiles.add(outputFile);
		}
		debInputStream.close();

		return untaredFiles;
	}

	/**
	 * Ungzip an input file into an output file.
	 * <p>
	 * The output file is created in the output folder, having the same name as
	 * the input file, minus the '.gz' extension.
	 * 
	 * SOURCE: http://stackoverflow.com/a/7556307/1478837
	 * 
	 * @param inputFile
	 *            the input .gz file
	 * @param outputDir
	 *            the output directory file.
	 * @throws IOException
	 * @throws FileNotFoundException
	 * 
	 * @return The {@File} with the ungzipped content.
	 */
	private static File unGzip(final File inputFile, final File outputDir) throws FileNotFoundException, IOException {

		//	    LOG.info(String.format("Ungzipping %s to dir %s.", inputFile.getAbsolutePath(), outputDir.getAbsolutePath()));

		final File outputFile = new File(outputDir, inputFile.getName().substring(0, inputFile.getName().length() - 3));

		final GZIPInputStream in = new GZIPInputStream(new FileInputStream(inputFile));
		final FileOutputStream out = new FileOutputStream(outputFile);

		IOUtils.copy(in, out);

		in.close();
		out.close();

		return outputFile;
	}

	/**
	 * Deletes all contents in the temporary untar dir
	 */
	private static void clearUntarDir() {
		purgeDirectory(TEMPuntarDir);
	}

	public static void purgeDirectory(File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory())
				purgeDirectory(file);
			file.delete();
		}
	}

	/**
	 * Returns the tree with the best overall fitness, using the given weights
	 * 
	 * @param pf
	 * @param weights
	 * @return
	 */
	public static ProbProcessArrayTree getTreeWithBestOf(ParetoFront pf, HashMap<TreeFitnessInfo, Double> weights) {
		double totalWeight = 0;
		for (double value : weights.values()) {
			totalWeight += value;
		}

		ProbProcessArrayTree bestTree = null;
		double bestOf = -1;

		for (ProbProcessArrayTree tree : pf.getFront()) {
			TreeFitness fitness = pf.getRegistry().getFitness(tree);

			double sum = 0;
			for (TreeFitnessInfo dim : weights.keySet()) {
				sum += fitness.fitnessValues.get(dim) * weights.get(dim);
			}

			double of = sum / totalWeight;

			fitness.setOverallFitness(OverallFitness.info, of);

			if (of > bestOf) {
				bestOf = of;
				bestTree = tree;
			}
		}
		return bestTree;
	}

}
