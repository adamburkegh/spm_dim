package org.processmining.plugins.etm.experiments.thesis;

import java.util.ArrayList;
import java.util.List;

import org.processmining.plugins.etm.experiments.ExperimentSettingsAbstract;

/**
 * This class provides methods that create parameter strings for the different
 * experiments shown an discussed in my PhD thesis to be passed to the command
 * line (f.i. via the SSHExperiments method to run these experiments via an SSH
 * connection).
 * 
 * @author jbuijs
 * 
 */
public class ThesisExperimentSettings extends ExperimentSettingsAbstract {

	public static void main(String[] args) {
		ThesisExperimentSettings t = new ThesisExperimentSettings("LOGDIR", "LOGFILEDIR", "DATE");

		List<String> list = t.getExpSettings();
		for (String exp : list) {
			System.out.println(exp);
		}
	}

	/**
	 * Initializes the thesis experiment settings class with a global directory
	 * where the logging files are written to and a directory where the event
	 * log files are stored.
	 * 
	 * @param logDir
	 *            Directory to write logging files to
	 * @param logFileDir
	 *            Directory which contains the required event log files
	 */
	public ThesisExperimentSettings(String logDir, String logFileDir, String date) {
		super(logDir, logFileDir, date);
	}

	/**
	 * The basic settings (weights, generations, popSize etc. etc.) used for all
	 * experiments (which can be deviated from!!!)
	 */
	public static final String basicSettings = " Fr=10 Pe=5 Sm=1 Gv=0.1 maxGen=10000 popSize=100 eliteSize=20 "
			+ "limitFTime=10 randomMutRatio=0.50 logModulo=1000 IMfactoryWeight=0 ";

	/**
	 * The standard string with string replace: mode, logfile,
	 * additionalSettings (overrides basicSettings), expCode, expDesc
	 */
	public String defaultString = "%s \"" + logDir + "\" log=\"" + logFileDir + "%s\" " + basicSettings
			+ "%s expDate=\"" + date + "\" expCode=%s expDesc=\"%s\"";

	/**
	 * String to append to a settings string in case of a pareto mode run on a
	 * real life event log (sets a Pareto front limit on Fr of 0.6 to be applied
	 * in generation 10, but also a Fr replay fitness limit of 0.6, to stop
	 * calculations 'early'), finally the PF is limited to 100 candidates max.
	 */
	public String paretoLimitString = " frLimit=0.6 peLimit=0.6 smLimit=0.8 generationWhenLimitsAreApplied=10 "
			+ "limitF=0.6 PFmaxSize=200 maxGen=1000 logModulo=10 ";

	private static String waboLogName = "WABO%d_01_BB.xes.gz";

	/**
	 * Chapter 7, running example, no noise, normal mode, 30 repetitions!
	 * 
	 * @return
	 */
	public List<String> getExpSettings_basicDisc_runEx_noNoise() {
		return ExperimentSettingsAbstract
				.duplicateList(
						ExperimentSettingsAbstract
								.toList("NORMAL \""
										+ logDir
										+ "\" log=\""
										+ logFileDir
										+ "000RunEx-Default.xez\" "
										+ basicSettings
										+ " logmodulo=2 expDate=\""
										+ date
										+ "\" expCode=basicDisc_runEx_noNoise expDesc=\"Basic application of the ETM on the running example without noise.\""),
						30);
	}

	/**
	 * Chapter 7, running example, with noise, normal mode, 30 repetitions!
	 * 
	 * @return
	 */
	public List<String> getExpSettings_basicDisc_runEx_withNoise() {
		return ExperimentSettingsAbstract
				.duplicateList(
						ExperimentSettingsAbstract
								.toList("NORMAL \""
										+ logDir
										+ "\" log=\""
										+ logFileDir
										+ "000RunEx-Default-Noise.xez\" "
										+ basicSettings
										+ " logmodulo=2 expDate=\""
										+ date
										+ "\" expCode=basicDisc_runEx_noise expDesc=\"Basic application of the ETM on the running example with noise.\""),
						30);
	}

	/**
	 * Chapter 7, Pareto front discovery example on running example with noise,
	 * 30 repetitions
	 * 
	 * @return
	 */
	public List<String> getExpSettings_basicDisc_pareto() {
		return ExperimentSettingsAbstract
				.duplicateList(
						ExperimentSettingsAbstract
								.toList("PARETO \""
										+ logDir
										+ "\" log=\""
										+ logFileDir
										+ "000RunEx-Default-Noise.xez\" "
										+ basicSettings
										+ " logmodulo=2 expDate=\""
										+ date
										+ "\" expCode=basicDisc_runExNoise_pareto expDesc=\"Application of the ETM to discover a Pareto front on the running example with noise.\""),
						30);
	}

	/**
	 * Chapter 8, random versus guided experiments (ie varying the weights for
	 * (non)random changes) on running example data, 5 repetitions each
	 * 
	 * @return
	 */
	public List<String> getExpSettings_caseStudies_runEx_randomVsGuided() {
		//FIXME add parameter for these experiments!
		String baseSettings = "NORMAL \"" + logDir + "\" log=\"" + logFileDir + "000RunEx-Default-Noise.xez\" "
				+ basicSettings + " logmodulo=1 ";

		List<String> expList = ExperimentSettingsAbstract
				.toList(baseSettings
						+ " randomMutRatio=0.0 "
						+ " expDate=\""
						+ date
						+ "\" expCode=caseStudies_randomVsGuided_onlyGuided expDesc=\"Application of the ETM with only guided mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=0.25 "
				+ " expDate=\""
				+ date
				+ "\" expCode=caseStudies_randomVsGuided_random25 expDesc=\"Application of the ETM with 25% random mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=0.5 "
				+ " expDate=\""
				+ date
				+ "\" expCode=caseStudies_randomVsGuided_random50 expDesc=\"Application of the ETM with 50% random mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=0.75 "
				+ " expDate=\""
				+ date
				+ "\" expCode=caseStudies_randomVsGuided_random75 expDesc=\"Application of the ETM with 75% random mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=1.0 "
				+ " expDate=\""
				+ date
				+ "\" expCode=caseStudies_randomVsGuided_onlyRandom expDesc=\"Application of the ETM with only random mutations.\"");

		return ExperimentSettingsAbstract.duplicateList(expList, 5);
	}

	/**
	 * Chapter 8, random versus guided experiments (ie varying the weights for
	 * (non)random changes) on building permits data , 5 repetitions each
	 * 
	 * @return
	 */
	public List<String> getExpSettings_caseStudies_buildingPerm_randomVsGuided() {
		//FIXME add parameter for these experiments!
		String baseSettings = "NORMAL \"" + logDir + "\" log=\"" + logFileDir
				+ "buildingPermits_receiptPhase.xes.gz\" " + basicSettings + " logmodulo=1 maxGen=1000 ";

		List<String> expList = ExperimentSettingsAbstract
				.toList(baseSettings
						+ " randomMutRatio=0.0 "
						+ " expDate=\""
						+ date
						+ "\" expCode=caseStudies_buildingPerm_randomVsGuided_onlyGuided expDesc=\"Application of the ETM with only guided mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=0.25 "
				+ " expDate=\""
				+ date
				+ "\" expCode=caseStudies_buildingPerm_randomVsGuided_random25 expDesc=\"Application of the ETM with 25% random mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=0.5 "
				+ " expDate=\""
				+ date
				+ "\" expCode=caseStudies_buildingPerm_randomVsGuided_random50 expDesc=\"Application of the ETM with 50% random mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=0.75 "
				+ " expDate=\""
				+ date
				+ "\" expCode=caseStudies_buildingPerm_randomVsGuided_random75 expDesc=\"Application of the ETM with 75% random mutations.\"");

		expList.add(baseSettings
				+ " randomMutRatio=1.0 "
				+ " expDate=\""
				+ date
				+ "\" expCode=caseStudies_buildingPerm_randomVsGuided_onlyRandom expDesc=\"Application of the ETM with only random mutations.\"");

		return ExperimentSettingsAbstract.duplicateList(expList, 5);
	}

	/**
	 * Chapter 8 case study application in Pareto mode on the building permits
	 * event log
	 * 
	 * @return
	 */
	public List<String> getExpSettings_caseStudies_BuildingPerm() {
		return ExperimentSettingsAbstract.toList(String.format(defaultString, "PARETO",
				"buildingPermits_receiptPhase.xes.gz", "", "caseStudies_buildingPerm",
				"Pareto application on the building permits event log.")
				+ paretoLimitString);
	}

	/**
	 * Chapter 8 case study application in Pareto mode on the five similar WABO
	 * event log (seperate application!!!)
	 * 
	 * @return
	 */
	public List<String> getExpSettings_caseStudies_WABO() {
		List<String> list = ExperimentSettingsAbstract.toList(String.format(defaultString, "PARETO",
				//"WABO1_HOOFDtm515_top25.xes.gz", 
				String.format(waboLogName, 1), "", "caseStudies_WABO1",
				"Pareto application on the WABO event log for municipality 1.") + paretoLimitString);
		list.add(String.format(defaultString, "PARETO", String.format(waboLogName, 2), "", "caseStudies_WABO2",
				"Pareto application on the WABO event log for municipality 2.") + paretoLimitString);
		list.add(String.format(defaultString, "PARETO", String.format(waboLogName, 3), "", "caseStudies_WABO3",
				"Pareto application on the WABO event log for municipality 3.") + paretoLimitString);
		list.add(String.format(defaultString, "PARETO", String.format(waboLogName, 4), "", "caseStudies_WABO4",
				"Pareto application on the WABO event log for municipality 4.") + paretoLimitString);
		list.add(String.format(defaultString, "PARETO", String.format(waboLogName, 5), "", "caseStudies_WABO5",
				"Pareto application on the WABO event log for municipality 5.") + paretoLimitString);
		return list;
	}

	/**
	 * Chapter 9, discovery with normative model, running example no noise,
	 * Pareto
	 * 
	 * @return
	 */
	public List<String> getExpSettings_normative_runEx() {
		return ExperimentSettingsAbstract
				.toList(String
						.format(defaultString,
								"PARETO",
								"000RunEx-Default.xez",
								" seedTree=\"SEQ( LEAF: A+complete , XOR( SEQ( LEAF: B+complete , LEAF: C+complete , LEAF: D+complete , LEAF: F+complete ) , SEQ( LEAF: C+complete , XOR( SEQ( LEAF: B+complete , LEAF: F+complete ) , SEQ( LEAF: D+complete , LEAF: B+complete , LEAF: E+complete ) ) ) ) , LEAF: G+complete ) \"",
								"normative_runEx", "Normative discovery on running example"));
	}

	/**
	 * Chapter 9, discovery with normative model, case study, Pareto
	 * 
	 * @return
	 */
	public List<String> getExpSettings_normative_buildingPerm() {
		return ExperimentSettingsAbstract
				.toList(String
						.format(defaultString,
								"PARETO",
								"buildingPermits_receiptPhase_noSpecialChars.xes.gz",
								//" seedTree=\"SEQ( LEAF: t Cre�ren ontvangstbevestiging+complete , AND( SEQ( LOOP( LEAF: RP004 Vaststellen ontvangstbevestiging+complete , LEAF: RP003 Aanpassen ontvangstbevestiging+complete , LEAF: tau ) , LEAF: RP005 Uitdraaien en verzenden ontvangstbevestiging+complete ) , SEQ( SEQ( LOOP( LEAF: RP006 Bepalen noodzaak adviezen aanhouden+complete , OR( SEQ( LEAF: RP008 Opstellen en verzenden adviesaanvraag+complete , LEAF: tau ) , SEQ( SEQ( LEAF: RP007-1 Opstellen intern advies aanhouden Bouw+complete , LEAF: RP007-2 Opstellen intern advies aanhouden Milieu+complete ) , SEQ( LEAF: RP007-3 Opstellen intern advies aanhouden Gebruik+complete , SEQ( LEAF: RP007-4 Opstellen intern advies aanhouden APV/Overig+complete , LEAF: RP007-5 Opstellen intern advies aanhouden Zelf+complete ) ) ) ) , LEAF: tau ) , LEAF: RP010 Bepalen noodzaak indicatie aanhouden+complete ) , XOR( LEAF: tau , XOR( SEQ( SEQ( LEAF: tau , LOOP( SEQ( LOOP( LEAF: RP012 Toetsen document 251 aanvraag vergunningvrij+complete , LEAF: RP013 Aanpassen document 251 aanvraag vergunningvrij+complete , LEAF: tau ) , LEAF: RP014 Vaststellen document 251 aanvraag vergunningvrij+complete ) , LEAF: RP013 Aanpassen document 251 aanvraag vergunningvrij+complete , LEAF: tau ) ) , LEAF: RP015 Uitdraaien en verzenden document 251 vergunningvrij+complete ) , SEQ( SEQ( LEAF: RP016 Vastleggen aanhoudingsgronden+complete , LOOP( SEQ( LOOP( LEAF: RP017 Toetsen melding 295 indicatie aanhouden+complete , LEAF: RP018 Aanpassen melding 295 indicatie aanhouden+complete , LEAF: tau ) , LEAF: RP019 Vaststellen melding 295 indicatie aanhouden+complete ) , LEAF: RP018 Aanpassen melding 295 indicatie aanhouden+complete , LEAF: tau ) ) , LEAF: RP020 Uitdraaien en verzenden melding 295 aanhouden+complete ) ) ) ) ) ) \"",
								" seedTree=\"SEQ( LEAF: t Creeren ontvangstbevestiging+complete , AND( SEQ( LOOP( LEAF: RP006 Bepalen noodzaak adviezen aanhouden+complete , OR( SEQ( LEAF: RP007-1 Opstellen intern advies aanhouden Bouw+complete , LEAF: RP007-2 Opstellen intern advies aanhouden Milieu+complete , LEAF: RP007-3 Opstellen intern advies aanhouden Gebruik+complete , LEAF: RP007-4 Opstellen intern advies aanhouden APV/Overig+complete , LEAF: RP007-5 Opstellen intern advies aanhouden Zelf+complete ) , LEAF: RP008 Opstellen en verzenden adviesaanvraag+complete ) , LEAF: tau ) , LEAF: RP010 Bepalen noodzaak indicatie aanhouden+complete , XOR( LEAF: tau , SEQ( LOOP( SEQ( LOOP( LEAF: RP012 Toetsen document 251 aanvraag vergunningvrij+complete , LEAF: RP013 Aanpassen document 251 aanvraag vergunningvrij+complete , LEAF: tau ) , LEAF: RP014 Vaststellen document 251 aanvraag vergunningvrij+complete ) , LEAF: RP013 Aanpassen document 251 aanvraag vergunningvrij+complete , LEAF: tau ) , LEAF: RP015 Uitdraaien en verzenden document 251 vergunningvrij+complete ) , SEQ( LEAF: RP016 Vastleggen aanhoudingsgronden+complete , LOOP( SEQ( LOOP( LEAF: RP017 Toetsen melding 295 indicatie aanhouden+complete , LEAF: RP018 Aanpassen melding 295 indicatie aanhouden+complete , LEAF: tau ) , LEAF: RP019 Vaststellen melding 295 indicatie aanhouden+complete ) , LEAF: RP018 Aanpassen melding 295 indicatie aanhouden+complete , LEAF: tau ) , LEAF: RP020 Uitdraaien en verzenden melding 295 aanhouden+complete ) ) ) , SEQ( LOOP( LEAF: RP004 Vaststellen ontvangstbevestiging+complete , LEAF: RP003 Aanpassen ontvangstbevestiging+complete , LEAF: tau ) , LEAF: RP005 Uitdraaien en verzenden ontvangstbevestiging+complete ) ) ) [ ] \"",
								"normative_buildingPerm", "Normative discovery on building permits case study.")
						+ paretoLimitString + "PFmaxSize=200 maxGen=10000");
	}

	/**
	 * Chapter 10, configurable process tree discovery, running example, all
	 * four approaches
	 * 
	 * @return
	 */
	public List<String> getExpSettings_configurable_runEx() {
		List<String> list = ExperimentSettingsAbstract.toList(String.format(
				defaultString,
				"BLUE",
				createLogString("000RunEx-Default.xez", "000RunEx-Config2.xez", "000RunEx-Config3.xez",
						"000RunEx-Config4.xez"), "", "configurable_runEx_approach1",
				"Discovery of configurable process model using approach 1 on running example."));

		list.add(String.format(
				defaultString,
				"BLACK",
				createLogString("000RunEx-Default.xez", "000RunEx-Config2.xez", "000RunEx-Config3.xez",
						"000RunEx-Config4.xez"), "", "configurable_runEx_approach3",
				"Discovery of configurable process model using approach 3 on running example."));

		list.add(String.format(
				defaultString,
				"GREEN",
				createLogString("000RunEx-Default.xez", "000RunEx-Config2.xez", "000RunEx-Config3.xez",
						"000RunEx-Config4.xez"), "", "configurable_runEx_approach4",
				"Discovery of configurable process model using approach 4 on running example."));

		list.add(String.format(
				defaultString,
				"PARETO_GREEN",
				createLogString("000RunEx-Default.xez", "000RunEx-Config2.xez", "000RunEx-Config3.xez",
						"000RunEx-Config4.xez"), "", "configurable_runEx_approach4_pareto",
				"Discovery of configurable process model using approach 4 in Pareto mode on running example.")
				+ " ");

		list.add(String.format(
				defaultString,
				"RED",
				createLogString("000RunEx-Default.xez", "000RunEx-Config2.xez", "000RunEx-Config3.xez",
						"000RunEx-Config4.xez"), "", "configurable_runEx_approach2",
				"Discovery of configurable process model using approach 2 on running example.")
				+ " Ed=5");

		return list;
		//return ExperimentSettingsAbstract.duplicateList(list, 5);
	}

	/**
	 * Chapter 10, configurable process tree discovery, case study, all four
	 * approaches
	 * 
	 * @return
	 */
	public List<String> getExpSettings_configurable_caseStudy() {
		List<String> list = ExperimentSettingsAbstract.toList(String.format(
				defaultString,
				"BLUE",
				createLogString(String.format(waboLogName, 1), String.format(waboLogName, 2),
						String.format(waboLogName, 3), String.format(waboLogName, 4), String.format(waboLogName, 5)),
				"", "configurable_WABO_approach1",
				"Discovery of configurable process model using approach 1 on WABO event logs.")
				+ "maxGen=1000");

		list.add(String.format(
				defaultString,
				"BLACK",
				createLogString(String.format(waboLogName, 1), String.format(waboLogName, 2),
						String.format(waboLogName, 3), String.format(waboLogName, 4), String.format(waboLogName, 5)),
				"", "configurable_WABO_approach3",
				"Discovery of configurable process model using approach 3 on WABO event logs.")
				+ "maxGen=1000");

		list.add(String.format(
				defaultString,
				"GREEN",
				createLogString(String.format(waboLogName, 1), String.format(waboLogName, 2),
						String.format(waboLogName, 3), String.format(waboLogName, 4), String.format(waboLogName, 5)),
				"", "configurable_WABO_approach4",
				"Discovery of configurable process model using approach 4 on WABO event logs.")
				+ "maxGen=1000");

		list.add(String.format(
				defaultString,
				"PARETO_GREEN",
				createLogString(String.format(waboLogName, 1), String.format(waboLogName, 2),
						String.format(waboLogName, 3), String.format(waboLogName, 4), String.format(waboLogName, 5)),
				"", "configurable_WABO_approach4_pareto",
				"Discovery of configurable process model using approach 4 in Pareto mode on WABO event logs.")
				+ paretoLimitString);

		list.add(String.format(
				defaultString,
				"RED",
				createLogString(String.format(waboLogName, 1), String.format(waboLogName, 2),
						String.format(waboLogName, 3), String.format(waboLogName, 4), String.format(waboLogName, 5)),
				"", "configurable_WABO_approach2",
				"Discovery of configurable process model using approach 2 on WABO event logs.")
				+ "maxGen=1000 Ed=5");

		return list;
		//return ExperimentSettingsAbstract.duplicateList(list, 5);
	}

	/**
	 * All Chapter 7 (basic discovery) experiment settings
	 * 
	 * @return
	 */
	public List<String> getExpSettings_basicDisc() {
		List<String> parList = new ArrayList<String>();
		parList.addAll(getExpSettings_basicDisc_runEx_noNoise());
		parList.addAll(getExpSettings_basicDisc_runEx_withNoise());
		parList.addAll(getExpSettings_basicDisc_pareto());
		return parList;
	}

	/**
	 * All Chapter 8 (case studies) experiment settings
	 * 
	 * @return
	 */
	public List<String> getExpSettings_caseStudies() {
		List<String> parList = new ArrayList<String>();
		parList.addAll(getExpSettings_caseStudies_runEx_randomVsGuided());
		parList.addAll(getExpSettings_caseStudies_buildingPerm_randomVsGuided());
		parList.addAll(getExpSettings_caseStudies_BuildingPerm());
		parList.addAll(getExpSettings_caseStudies_WABO());
		return parList;
	}

	/**
	 * All Chapter 9 (normative discovery) experiment settings
	 * 
	 * @return
	 */
	public List<String> getExpSettings_normative() {
		List<String> parList = new ArrayList<String>();
		parList.addAll(getExpSettings_normative_runEx());
		parList.addAll(getExpSettings_normative_buildingPerm());
		return parList;
	}

	/**
	 * All Chapter 10 (configurable model discovery) experiment settings
	 * 
	 * @return
	 */
	public List<String> getExpSettings_configurable() {
		List<String> parList = new ArrayList<String>();
		parList.addAll(getExpSettings_configurable_runEx());
		parList.addAll(getExpSettings_configurable_caseStudy());
		return parList;
	}

	/**
	 * Returns ALL experiments for the thesis
	 * 
	 * @return
	 */
	public List<String> getExpSettings() {
		List<String> parList = new ArrayList<String>();
		parList.addAll(getExpSettings_basicDisc());
		parList.addAll(getExpSettings_caseStudies());
		parList.addAll(getExpSettings_normative());
		parList.addAll(getExpSettings_configurable());
		return parList;
	}

	public List<String> getExpSettings_temp() {
		return ExperimentSettingsAbstract.toList("PARETO \"" + logDir + "\" log=\"" + logFileDir
				+ "000RunEx-Default.xez\" "
				+ " Fr=10 Pe=5 Sm=1 Gv=.1 maxGen=100 popSize=10 eliteSize=2 limitFTime=1000 randomMutRatio=0.50 "
				+ " expDate=\"" + date + "\" expCode=temp_exp expDesc=\"Temporary experiment for quick testing...\"");
	}

	/**
	 * Returns a single string that can be input for the defaultString String
	 * format
	 * 
	 * @param log
	 * @return
	 */
	public String createLogString(String... log) {
		String logs = log[0];

		//For all others, except the first one
		for (int i = 1; i < log.length; i++) {
			logs += "\" log=\"" + logFileDir + log[i];
		}

		return logs;
	}
}
