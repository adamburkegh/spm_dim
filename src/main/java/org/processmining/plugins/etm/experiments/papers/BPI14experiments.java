package org.processmining.plugins.etm.experiments.papers;

import java.io.File;
import java.util.List;

import org.processmining.plugins.etm.experiments.ExperimentSettingsAbstract;
import org.processmining.plugins.etm.experiments.thesis.ThesisExperimentProcessor;
import org.processmining.plugins.etm.experiments.thesis.ThesisExperimentProcessor.ProcessingModes;
import org.processmining.plugins.etm.experiments.thesis.ThesisExperimentSettings;

public class BPI14experiments extends ExperimentSettingsAbstract {

	public static String expCode = "BPI14_randomVsGuided";
	
	public BPI14experiments(String logDir, String eventLogFileDir, String date) {
		super(logDir, eventLogFileDir, date);
	}
	
	public static void main(String[] args){
		String experimentDate = "14-05-25_09_06_59";
		
		File ETMLocalLogsDirPreferred = new File(ThesisExperimentProcessor.ETMLocalLogsDirString + "/" + experimentDate);
		
		parse_BPI14_randomVsGuided(ETMLocalLogsDirPreferred, experimentDate);
	}
	
	/**
	 * Parses the BPI14_runEx_randomVsGuided experiment files and
	 * produces a statistics file with the average Of of the different ratios.
	 * It also produces an .tex file with the 'most average' process tree found
	 * in the final generation (which is kinda useless).
	 * 
	 * @param eTMLocalLogsDirMostrecent
	 * @param experimentDate
	 */
	public static void parse_BPI14_randomVsGuided(File eTMLocalLogsDirMostrecent,
			String experimentDate) {

		System.out.println("Starting " + expCode);

		ThesisExperimentProcessor.parse_standardStatsFile(eTMLocalLogsDirMostrecent, experimentDate, expCode, true,
				ProcessingModes.OverallFitness);
	}

	public List<String> getRandomMutRatioExpSettings(int nrReps) {
		String baseSettings = "NORMAL \"" + logDir + "\" log=\"" + logFileDir + "BPI14RunningExample.xes\" "
				+ ThesisExperimentSettings.basicSettings + " logmodulo=1 ";

		List<String> expList = ExperimentSettingsAbstract
				.toList(baseSettings
						+ " randomMutRatio=0.0 "
						+ " expDate=\""
						+ date
						+ "\" expCode=BPI14_randomVsGuided_onlyGuided expDesc=\"Application of the ETM with only guided mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=0.25 "
				+ " expDate=\""
				+ date
				+ "\" expCode=BPI14_randomVsGuided_random25 expDesc=\"Application of the ETM with 25% random mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=0.5 "
				+ " expDate=\""
				+ date
				+ "\" expCode=BPI14_randomVsGuided_random50 expDesc=\"Application of the ETM with 50% random mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=0.75 "
				+ " expDate=\""
				+ date
				+ "\" expCode=BPI14_randomVsGuided_random75 expDesc=\"Application of the ETM with 75% random mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=1.0 "
				+ " expDate=\""
				+ date
				+ "\" expCode=BPI14_randomVsGuided_onlyRandom expDesc=\"Application of the ETM with only random mutations.\"");

		return ExperimentSettingsAbstract.duplicateList(expList, nrReps);
	}
}
