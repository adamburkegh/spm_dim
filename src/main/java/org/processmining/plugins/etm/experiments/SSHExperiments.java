package org.processmining.plugins.etm.experiments;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.processmining.plugins.etm.experiments.papers.BPI14experiments;
import org.processmining.plugins.etm.experiments.thesis.ThesisExperimentSettings;

import com.sshtools.j2ssh.SftpClient;
import com.sshtools.j2ssh.SshClient;
import com.sshtools.j2ssh.authentication.AuthenticationProtocolState;
import com.sshtools.j2ssh.authentication.PasswordAuthenticationClient;
import com.sshtools.j2ssh.io.IOStreamConnector;
import com.sshtools.j2ssh.io.IOStreamConnectorState;
import com.sshtools.j2ssh.session.SessionChannelClient;
import com.sshtools.j2ssh.transport.HostKeyVerification;
import com.sshtools.j2ssh.transport.MessageStoreEOFException;
import com.sshtools.j2ssh.transport.TransportProtocolException;
import com.sshtools.j2ssh.transport.publickey.SshPublicKey;
import com.sshtools.j2ssh.util.InvalidStateException;

/**
 * Stripped and cleaned version of
 * {@link org.processmining.plugins.boudewijn.tree.SSHTestOLD} with unused (BUT
 * PROBABLY USEFULL (broken) methods removed!!!)
 * 
 * @author jbuijs
 * 
 */
public class SSHExperiments {

	//TODO add method that lists which nodes are (not) free, using f.i. top (currently top output needs to be manually processed)

	/**
	 * Time formatter to prepend date/time on Eclipse console output
	 */
	public final static SimpleDateFormat consoleTimeFormatter = new SimpleDateFormat("yy-MM-dd_HH_mm_ss");

	/*-*/
	//ALL nodes
	private static String[] allNodes = new String[] { "ngrid01", "ngrid02", "ngrid03", "ngrid04", "ngrid05", "ngrid06",
			"ngrid07", "ngrid08", "ngrid09", "ngrid10", "ngrid11", "ngrid12", "ngrid13", "ngrid14", "ngrid15",
			"ngrid16", "ngrid17", "ngrid18", "ngrid19", "ngrid20", "ngrid21", "ngrid22", "ngrid23", "ngrid24",
			"ngrid25", "ngrid26", "ngrid27", "ngrid28", "ngrid29", "ngrid30", "ngrid31", "ngrid32" };

	/**/

	/*-*/
	private static String[] nodes = new String[] { /*-* /"ngrid01",
													/*-* /"ngrid02", "ngrid03", "ngrid04", "ngrid05", /*-*/
	/*-*/"ngrid06", "ngrid07", "ngrid08", "ngrid09", "ngrid10", /*-*/
	/*-*/"ngrid11", /*-*/"ngrid12", "ngrid13"/*-* /, "ngrid14", "ngrid15", "ngrid17", "ngrid18", /**/
	//"ngrid20", /*-*/ 
	//			"ngrid21", "ngrid22", "ngrid23", "ngrid24",/**/
	/*-* /"ngrid25"/*-* /, "ngrid26",/** /"ngrid27", "ngrid28",/*-* /"ngrid29", /*-* /"ngrid30", "ngrid31",
	"ngrid32"/**/};

	private static String date;
	private static String nodeLogDir;
	private static String localLogDir;

	//To remember how many session have been run in batches before
	private static int sessionOffset = 0;

	/**/

	//Or use only a few nodes... 
	//	private static String[] nodes = new String[] { "ngrid" };

	/**
	 * Provide the arguments for authentication: args[0] = username args[1] =
	 * password
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		final String user = args[0];
		final String password = args[1];

		//		long start = System.currentTimeMillis();

		date = consoleTimeFormatter.format(new Date(System.currentTimeMillis()));
		date = "14-05-25_09_06_59"; // in case aborted session needs to be parsed

		printToConsole("Experiment date is: " + date);

		nodeLogDir = "/scratch/jbuijs/geneticMining/" + date + "/";
		localLogDir = "E:/ETMLocalLogs/CLIexp/ngrid/" + date + "/";

		//in case aborted session needs to be parsed
		copyLogFiles(user, password);
		System.exit(0);

		/*
		 * Shows the running processes on all nodes, then terminates this main
		 * function!!!
		 */
		//executeTopOnAllNodes(user, password);

										killWork(user, password, true);
		//If above killwork is enabled the following code WONT BE REACHED

		//start new work but auto-kill old work before
		killWork(user, password, false);

		//Start the experiments we want to start
		int nrRepeats = 1;

		//Work in batches, after each batch process the files to local, then start next batch
		List<List<String>> expBatches = new ArrayList<List<String>>();

		ThesisExperimentSettings thesisExperimentSettings = new ThesisExperimentSettings(nodeLogDir,
				"/home/jbuijs/logs/", date);

		/*
		 * basicDisc batch
		 */
		//expBatches.add(thesisExperimentSettings.getExpSettings_basicDisc());
		List<String> basicDiscBatch = new ArrayList<String>();
		basicDiscBatch.addAll(thesisExperimentSettings.getExpSettings_basicDisc_runEx_noNoise());
		basicDiscBatch.addAll(thesisExperimentSettings.getExpSettings_basicDisc_runEx_withNoise());
		//expBatches.add(basicDiscBatch);

		/*
		 * randomVsGuided (runEx and buildingPerm
		 */
		List<String> randomVsGuidedBatch = new ArrayList<String>();
		randomVsGuidedBatch.addAll(thesisExperimentSettings.getExpSettings_caseStudies_runEx_randomVsGuided());
		randomVsGuidedBatch.addAll(thesisExperimentSettings.getExpSettings_caseStudies_buildingPerm_randomVsGuided());
		//expBatches.add(randomVsGuidedBatch);

		/*
		 * case studies building Perm and WABO
		 */
		List<String> caseStudiesBatch = new ArrayList<String>();
		caseStudiesBatch.addAll(thesisExperimentSettings.getExpSettings_caseStudies_BuildingPerm());
		caseStudiesBatch.addAll(thesisExperimentSettings.getExpSettings_caseStudies_WABO());
		//expBatches.add(thesisExperimentSettings.getExpSettings_caseStudies_BuildingPerm());
		//expBatches.add(caseStudiesBatch);

		//And a final batch containing both normative and configurable experimetns
		List<String> finalBatch = new ArrayList<String>();
		/*
		 * normative
		 */
		//finalBatch.addAll(thesisExperimentSettings.getExpSettings_normative());
		finalBatch.addAll(thesisExperimentSettings.getExpSettings_normative_buildingPerm());

		/*
		 * configurable runEx
		 */
		//finalBatch.addAll(thesisExperimentSettings.getExpSettings_configurable_runEx());

		/*
		 * configurable WABO
		 */
		//finalBatch.addAll(thesisExperimentSettings.getExpSettings_configurable_caseStudy());

		//expBatches.add(finalBatch);

		/*
		 * BPI14 random versus guided on Maikels EL
		 */
		BPI14experiments bpiExp = new BPI14experiments(nodeLogDir, "/home/jbuijs/logs/", date);
		expBatches.add(bpiExp.getRandomMutRatioExpSettings(5)); //first a batch with 5 so we can do something
		expBatches.add(bpiExp.getRandomMutRatioExpSettings(25)); //then a batch with 25 so we can do something statistically significant

		//		expBatches.clear();
		//		myCustomBatch.addAll(thesisExperimentSettings.getExpSettings_caseStudies_BuildingPerm());
		//		myCustomBatch.addAll(thesisExperimentSettings.getExpSettings_caseStudies_WABO());
		//		myCustomBatch.addAll(ExperimentSettingsAbstract.toList(thesisExperimentSettings
		//				.getExpSettings_configurable_caseStudy().get(1))); //black
		//		expBatches.add(myCustomBatch);
		//		//Do two dummy test runs, using all nodes
		/*-
		expBatches.clear();
		expBatches.add(ExperimentSettingsAbstract.duplicateList(thesisExperimentSettings.getExpSettings_temp(),
				nodes.length));
		expBatches.add(ExperimentSettingsAbstract.duplicateList(thesisExperimentSettings.getExpSettings_temp(),
				nodes.length));
				/**/

		for (int b = 0; b < expBatches.size(); b++) {
			List<String> params = expBatches.get(b);
			System.out.println("Starting batch " + b + " of " + expBatches.size() + " of " + params.size()
					+ " experiments.");
			startWorkCommands(user, password, date, repeat(params, nrRepeats), true);

			//compress and download created dir
			copyLogFiles(user, password);
		}

		System.exit(0);
	}

	public static void startWork(final String user, final String password, final String date) {
		final List<String> parameters = Collections.synchronizedList(new ArrayList<String>());

		/*
		 * Prepare the parameters for the call
		 */
		String loggingPath = "\"/scratch/jbuijs/geneticMining/\"";

		/*
		 * BPM13 configuration paper
		 */
		/*
		 * All 4 methods, with 2 weight combinations (shown to work best for
		 * GREEn), 10 times each
		 */
		/*-* /
		String base = "log=/home/jbuijs/logs/WABO1_01_BB.xes.gz log=/home/jbuijs/logs/WABO2_01_BB.xes.gz "
				+ "log=/home/jbuijs/logs/WABO3_01_BB.xes.gz log=/home/jbuijs/logs/WABO4_01_BB.xes.gz "
				+ "log=/home/jbuijs/logs/WABO5_01_BB.xes.gz ";
		String[] methods = new String[] { "GREEN", "BLACK", "RED", "BLUE", "PARETO" };
		String[] weights = new String[] { " Fr=10 Pe=5 Sm=10 Gv=.1 maxGen=120000 popSize=20 eliteSize=6 configurationAlpha=0.0010 " };
		int repeats = 4; //Repeat each combi ... times

		for (String method : methods) {
			for (String weight : weights) {
				//Moved repeats iteration inside so sessions are now in blocks instead of 'offset'
				for (int r = 0; r < repeats; r++) {
					parameters.add(method + " " + loggingPath + " " + base + " " + weight);
				}
			}
		}/**/

		/*
		 * 2013 05 27 specific restart and additional experiments
		 */
		int repeats = 1;
		String[] experiments = new String[] {
				/*-
				//BPM RED normal parameters
				"RED \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/WABO1_01_BB.xes.gz "
						+ "log=/home/jbuijs/logs/WABO2_01_BB.xes.gz log=/home/jbuijs/logs/WABO3_01_BB.xes.gz log=/home/jbuijs/logs/WABO4_01_BB.xes.gz log=/home/jbuijs/logs/WABO5_01_BB.xes.gz"
						+ " Fr=10 Pe=5 Sm=10 Gv=.1 maxGen=120000 popSize=20 eliteSize=6 configurationAlpha=0.0010 limitFTime=1000 ",
				//BPM BLUE normal parameters
				"BLUE \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/WABO1_01_BB.xes.gz "
						+ "log=/home/jbuijs/logs/WABO2_01_BB.xes.gz log=/home/jbuijs/logs/WABO3_01_BB.xes.gz log=/home/jbuijs/logs/WABO4_01_BB.xes.gz log=/home/jbuijs/logs/WABO5_01_BB.xes.gz"
						+ " Fr=10 Pe=5 Sm=10 Gv=.1 maxGen=120000 popSize=20 eliteSize=6 configurationAlpha=0.0010 limitFTime=1000 ",
				//BPM GREEN Fr weight improved for case study
				"GREEN \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/WABO1_01_BB.xes.gz "
						+ "log=/home/jbuijs/logs/WABO2_01_BB.xes.gz log=/home/jbuijs/logs/WABO3_01_BB.xes.gz log=/home/jbuijs/logs/WABO4_01_BB.xes.gz log=/home/jbuijs/logs/WABO5_01_BB.xes.gz"
						+ " Fr=50 Pe=5 Sm=10 Gv=.1 maxGen=120000 popSize=20 eliteSize=6 configurationAlpha=0.0010 limitFTime=1000 ",
				//BPM GREEN Fr weight improved for running example
				/** /
				"GREEN \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/000RunEx-Default.xez "
						+ "log=/home/jbuijs/logs/000RunEx-Config2.xez log=/home/jbuijs/logs/000RunEx-Config3.xez log=/home/jbuijs/logs/000RunEx-Config3.xez "
						+ " Fr=50 Pe=5 Sm=10 Gv=.1 maxGen=120000 popSize=20 eliteSize=6 configurationAlpha=0.0010 limitFTime=1000 ",
				/**/
				//BPI PARETO on configuration case study (e.g. logs of BPM paper), adjusted popSize and generation count
				/*-* /
				"PARETO_GREEN \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/WABO1_01_BB.xes.gz "
						+ "log=/home/jbuijs/logs/WABO2_01_BB.xes.gz log=/home/jbuijs/logs/WABO3_01_BB.xes.gz log=/home/jbuijs/logs/WABO4_01_BB.xes.gz log=/home/jbuijs/logs/WABO5_01_BB.xes.gz"
						+ " Fr=10 Pe=5 Sm=10 Gv=.1 maxGen=10000 popSize=200 eliteSize=10 configurationAlpha=0.0010 limitFTime=100 "
				/**/
				//BPI running example
				/*-* /
				"PARETO \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/000RunEx-Default.xez "
						+ " Fr=10 Pe=5 Sm=10 Gv=.1 maxGen=5000 popSize=200 eliteSize=10 "
						+ "seedTree=\"SEQ( LEAF: A+complete , XOR( SEQ( LEAF: B+complete , LEAF: C+complete , LEAF: D+complete , LEAF: F+complete ) , SEQ( LEAF: C+complete , XOR( SEQ( LEAF: B+complete , LEAF: F+complete ) , SEQ( LEAF: D+complete , LEAF: B+complete , LEAF: E+complete ) ) ) ) , LEAF: G+complete ) \""
				/**/
				/*-* /
				"PARETO \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/SIMPDA12_caseStudy_Anonymous.xes.gz "
						+ " Fr=10 Pe=5 Sm=10 Gv=.1 Ed=1 maxGen=5000 popSize=200 eliteSize=10 "
						+ "seedTree=\"SEQ( LEAF: tau , AND( SEQ( LOOP( LEAF: RP004 Vaststellen ontvangstbevestiging+complete , LEAF: RP003 Aanpassen ontvangstbevestiging+complete , LEAF: tau ) , LEAF: RP005 Uitdraaien en verzenden ontvangstbevestiging+complete ) , SEQ( SEQ( LOOP( LEAF: RP006 Bepalen noodzaak adviezen aanhouden+complete , OR( SEQ( LEAF: RP008 Opstellen en verzenden adviesaanvraag+complete , LEAF: tau ) , SEQ( SEQ( LEAF: RP007-1 Opstellen intern advies aanhouden Bouw+complete , LEAF: RP007-2 Opstellen intern advies aanhouden Milieu+complete ) , SEQ( LEAF: RP007-3 Opstellen intern advies aanhouden Gebruik+complete , SEQ( LEAF: RP007-4 Opstellen intern advies aanhouden APV/Overig+complete , LEAF: RP007-5 Opstellen intern advies aanhouden Zelf+complete ) ) ) ) , LEAF: tau ) , LEAF: RP010 Bepalen noodzaak indicatie aanhouden+complete ) , XOR( LEAF: tau , XOR( SEQ( SEQ( LEAF: tau , LOOP( SEQ( LOOP( LEAF: RP012 Toetsen document 251 aanvraag vergunningvrij+complete , LEAF: RP013 Aanpassen document 251 aanvraag vergunningvrij+complete , LEAF: tau ) , LEAF: RP014 Vaststellen document 251 aanvraag vergunningvrij+complete ) , LEAF: RP013 Aanpassen document 251 aanvraag vergunningvrij+complete , LEAF: tau ) ) , LEAF: RP015 Uitdraaien en verzenden document 251 vergunningvrij+complete ) , SEQ( SEQ( LEAF: RP016 Vastleggen aanhoudingsgronden+complete , LOOP( SEQ( LOOP( LEAF: RP017 Toetsen melding 295 indicatie aanhouden+complete , LEAF: RP018 Aanpassen melding 295 indicatie aanhouden+complete , LEAF: tau ) , LEAF: RP019 Vaststellen melding 295 indicatie aanhouden+complete ) , LEAF: RP018 Aanpassen melding 295 indicatie aanhouden+complete , LEAF: tau ) ) , LEAF: RP020 Uitdraaien en verzenden melding 295 aanhouden+complete ) ) ) ) ) )\""
				/**/
				/*-"NORMAL \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/SIMPDA12_caseStudy_Anonymous.xes.gz "
						+ " Fr=5 Pe=5 Sm=1 Gv=.1 maxGen=120000 popSize=20 eliteSize=6 limitFTime=1000 ", };/**/
				"NORMAL \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/WABO1_CommonAct_15.xes.gz "
						+ " Fr=10 Pe=5 Sm=1 Gv=.1 maxGen=10000 popSize=40 eliteSize=15 limitFTime=1000 ",
				"NORMAL \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/WABO2_CommonAct_15.xes.gz "
						+ " Fr=10 Pe=5 Sm=1 Gv=.1 maxGen=10000 popSize=40 eliteSize=15 limitFTime=1000 ",
				"NORMAL \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/WABO3_CommonAct_15.xes.gz "
						+ " Fr=10 Pe=5 Sm=1 Gv=.1 maxGen=10000 popSize=40 eliteSize=15 limitFTime=1000 ",
				"NORMAL \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/WABO4_CommonAct_15.xes.gz "
						+ " Fr=10 Pe=5 Sm=1 Gv=.1 maxGen=10000 popSize=40 eliteSize=15 limitFTime=1000 ",
				"NORMAL \"/scratch/jbuijs/geneticMining/\" log=/home/jbuijs/logs/WABO5_CommonAct_15.xes.gz "
						+ " Fr=10 Pe=5 Sm=1 Gv=.1 maxGen=10000 popSize=40 eliteSize=15 limitFTime=1000 ", };

		for (String experiment : experiments) {
			//Moved repeats iteration inside so sessions are now in blocks instead of 'offset'
			for (int r = 0; r < repeats; r++) {
				parameters.add(experiment);
			}
		}/**/

		//Different combinations (21-3-2013 22:00)
		/*-* /
		//defaultParams (2 lines) updated 18-5-2013 for initial WABO 01 BB log test
		String defaultParams = "GREEN/BLACK/RED/BLUE " + loggingPath
				+ " maxGen=10000 popSize=20 eliteSize=6 configurationAlpha=0.0010 ";
		defaultParams += "log=/home/jbuijs/logs/WABO1_01_BB.xes.gz log=/home/jbuijs/logs/WABO2_01_BB.xes.gz "
				+ "log=/home/jbuijs/logs/WABO3_01_BB.xes.gz log=/home/jbuijs/logs/WABO4_01_BB.xes.gz "
				+ "log=/home/jbuijs/logs/WABO5_01_BB.xes.gz ";
		parameters.add(defaultParams + " Fr=1 Pe=1 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=1 Pe=1 Sm=1 Gv=1");
		parameters.add(defaultParams + " Fr=5 Pe=1 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=10 Pe=1 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=10 Pe=1 Sm=1 Gv=1");
		parameters.add(defaultParams + " Fr=25 Pe=1 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=50 Pe=1 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=75 Pe=1 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=100 Pe=1 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=100 Pe=1 Sm=1 Gv=1");
		parameters.add(defaultParams + " Fr=100 Pe=10 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=100 Pe=100 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=100 Pe=100 Sm=1 Gv=1");
		parameters.add(defaultParams + " Fr=100 Pe=1 Sm=100 Gv=1");
		parameters.add(defaultParams + " Fr=100 Pe=20 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=100 Pe=50 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=100 Pe=75 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=10 Pe=5 Sm=1 Gv=.1");
		parameters.add(defaultParams + " Fr=10 Pe=5 Sm=1 Gv=1");
		/**/

		//Now actually start
		startWorkCommands(user, password, date, parameters, true);
	}

	/**
	 * Starts the provided work (in the list of different parameter strings, one
	 * string per experiment) on the set list of nodes
	 * 
	 * @param user
	 * @param password
	 * @param date
	 * @param parameters
	 * @param copyLogsToLocal
	 *            if true copies the remote log files to the local log dir
	 */
	public static void startWorkCommands(final String user, final String password, final String date,
			final List<String> parameters, final boolean copyLogsToLocal) {
		ExecutorService executor = Executors.newFixedThreadPool(nodes.length);

		final List<String> freeNodes = Collections.synchronizedList(new LinkedList<String>());
		synchronized (freeNodes) {
			freeNodes.addAll(Arrays.asList(nodes));
		}

		final Object lock = new Object();

		for (int i = 0; i < parameters.size(); i++) {

			// change the parameters

			final int sessionNr = i + sessionOffset;
			final String paramString = parameters.get(i);

			executor.execute(new Runnable() {

				public void run() {
					int succes = -1;
					do {
						String gridHost = null;
						synchronized (freeNodes) {
							while (freeNodes.isEmpty()) {
								try {
									freeNodes.wait();
								} catch (InterruptedException e) {
									// continue;
								}
							}
							gridHost = freeNodes.remove(0);
						}
						SshClient ssh = null;
						try {
							killWork(user, password, gridHost);

							ssh = setupSSHClient(user, password, gridHost, lock);
							SessionChannelClient session = ssh.openSessionChannel();

							printToConsole("Starting experiment " + sessionNr + " on " + gridHost);

							long start = System.currentTimeMillis();
							String consoleFileName = localLogDir + sessionNr + "_" + gridHost + ".console";
							printToConsole(">>>Logging console output for " + gridHost + " to: " + consoleFileName);
							File file = new File(consoleFileName);
							file.getParentFile().mkdirs();
							file.createNewFile();
							PrintStream prStream = new PrintStream(file);
							String command = "taskset -c 0-7 ./java/jre1.6.0_30/bin/java -jar -da -D\"java.library.path=./lib/\" "
									+ "-XX:-UseGCOverheadLimit "
									+
									//"-Xmx8G " + //FIXME TEMPORARILY DISABLED
									"-XX:+UseCompressedOops "
									+ "-Djava.awt.headless=true -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/scratch/jbuijs/ "
									+ "ETMCLI.jar " + paramString + " expNode=" + gridHost + " expSession=" + sessionNr;
							System.out.println(gridHost + " sending command: " + command);
							succes = executeCommand(session, command, prStream);
							long end = System.currentTimeMillis();

							session.close();

							printToConsole("Closing session: " + sessionNr + " on " + gridHost + " after "
									+ (end - start) / 1000.0 + " seconds with status: "
									+ (succes > 0 ? "SUCCESSFUL" : "NOT SUCCESSFUL"));

						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							succes = -1;
						} finally {
							if (gridHost != null) {
								synchronized (freeNodes) {
									freeNodes.add(gridHost);
									freeNodes.notify();
								}
							}
							if (ssh != null) {
								ssh.disconnect();
							}
						}
						//repeat until successful
					} while (succes < 0);
				}

			});//runnable
		}//parameter instance
		try {
			Thread.sleep(100);
		} catch (InterruptedException e1) {

		}

		executor.shutdown();

		while (!executor.isTerminated()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		sessionOffset += parameters.size();

		printToConsole("Done");
	}

	public static SshClient setupSSHClient(String user, String password, String gridHost, Object lock)
			throws IOException {
		synchronized (lock) {
			final SshClient ssh = new SshClient();
			final PasswordAuthenticationClient pwd = new PasswordAuthenticationClient();
			pwd.setUsername(user);
			pwd.setPassword(password);
			final String host = gridHost + ".win.tue.nl";
			//			final String gridHost = "sviscl03.win.tue.nl";
			ssh.connect(host, new HostKeyVerification() {

				public boolean verifyHost(String h, SshPublicKey key) throws TransportProtocolException {
					return h.startsWith(host);
				}
			});

			if (ssh.authenticate(pwd) == AuthenticationProtocolState.COMPLETE) {
				printToConsole(gridHost + ": Authentication SUCCESSFUL");
			}

			printToConsole("Starting client: " + gridHost);
			return ssh;
		}
	}

	private final static int executeCommand(SessionChannelClient session, String command) throws IOException,
			InvalidStateException, InterruptedException {
		return executeCommand(session, command, System.out);
	}

	public final static int executeCommand(SessionChannelClient session, String command, PrintStream out)
			throws IOException, InvalidStateException, InterruptedException {
		//IOStreamConnector input = new IOStreamConnector(System.in, session.getOutputStream());
		IOStreamConnector output = new IOStreamConnector(session.getInputStream(), out);

		final StringPointer sp = new StringPointer();
		final OutputStream err = new OutputStream() {

			public void write(int b) throws IOException {
				sp.string += ((char) ((byte) b));
			}
		};

		IOStreamConnector error = new IOStreamConnector(session.getStderrInputStream(), err);
		//		input.setCloseInput(false);
		output.setCloseOutput(false);
		error.setCloseOutput(false);

		session.executeCommand(command);
		while (output.getState().getValue() != IOStreamConnectorState.CLOSED) {
			output.getState().waitForState(IOStreamConnectorState.CLOSED, 1000);
		}
		while (error.getState().getValue() != IOStreamConnectorState.CLOSED) {
			error.getState().waitForState(IOStreamConnectorState.CLOSED, 1000);
		}
		err.flush();
		err.close();
		//		input.close();
		output.close();
		error.close();

		System.err.println(sp.string);
		return (sp.string.isEmpty() ? 1 : -1);
	}

	protected static void killWork(String user, String password, boolean exitAfterwards) {
		for (int i = 0; i < nodes.length; i++) {
			String gridHost = nodes[i];
			killWork(user, password, gridHost);
		}
		System.out.println("All remote processes have been killed.");
		if (exitAfterwards) {
			System.exit(0);
		}
	}

	protected static void killWork(String user, String password, String gridHost) {
		Object lock = new Object();
		SshClient ssh = null;
		SessionChannelClient session;
		try {
			ssh = setupSSHClient(user, password, gridHost, lock);
			session = ssh.openSessionChannel();

			executeCommand(session, "killall -u " + user + " java");

			System.out.println("Killed work on: " + gridHost);

			session.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (ssh != null) {
				ssh.disconnect();
			}
		}
	}

	protected static void executeTopOnAllNodes(String user, String password) {
		Object lock = new Object();
		for (int i = 0; i < allNodes.length; i++) {
			String gridHost = allNodes[i];
			SshClient ssh = null;
			SessionChannelClient session;
			try {
				ssh = setupSSHClient(user, password, gridHost, lock);
				session = ssh.openSessionChannel();

				System.out.println("***");
				System.out.println("   " + gridHost);
				System.out.println("***");

				//				executeCommand(session, "export TERM=xterm");
				//				executeCommand(session, "export TERM");
				executeCommand(session, "top -b -n1 -i");

				System.out.println("Showed current work on: " + gridHost);

				session.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (ssh != null) {
					ssh.disconnect();
				}
			}
		}
		System.out.println("All running processes have been listed.");
		System.exit(0);
	}

	protected static void removeLogFiles(String user, String password, String date) {
		Object lock = new Object();
		for (int i = 0; i < nodes.length; i++) {
			String gridHost = nodes[i];
			SshClient ssh = null;
			SessionChannelClient session;
			try {
				ssh = setupSSHClient(user, password, gridHost, lock);
				session = ssh.openSessionChannel();

				//				executeCommand(session, "rm -rf " + "/scratch/geneticMining" + date + "/experiment*");
				executeCommand(session, "rm -rf " + "/scratch/jbuijs/geneticMining" + date + "/");

				System.out.println("Removed Logs from: " + gridHost);

				session.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (ssh != null) {
					ssh.disconnect();
				}
			}
		}
		System.out.println("All remote logs have been removed.");
		System.exit(0);
	}

	private static class StringPointer {
		public String string = "";
	}

	private static void printToConsole(String message) {
		System.out.println(consoleTimeFormatter.format(new Date()) + ": " + message);
	}

	/**
	 * Returns a list with each item repeated nrRepeats times
	 * 
	 * @param list
	 * @param nrRepeats
	 * @return
	 */
	private static List<String> repeat(List<String> list, int nrRepeats) {
		List<String> returnList = new ArrayList<String>();
		for (String item : list) {
			for (int i = 0; i < nrRepeats; i++) {
				returnList.add(item);
			}
		}
		return returnList;
	}

	protected static void copyLogFiles(String user, String password) {
		Object lock = new Object();

		ResultStats resultStats = new ResultStats();

		for (int i = 0; i < nodes.length; i++) {
			String gridHost = nodes[i];
			SshClient ssh = null;
			SessionChannelClient session;
			try {
				ssh = setupSSHClient(user, password, gridHost, lock);
				session = ssh.openSessionChannel();

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream ps = new PrintStream(out, true, "UTF8");
				//				session.executeCommand("cd " + nodeLogDir);
				//				executeCommand(session, "ls -d /*", ps);				
				executeCommand(session, "ls -d " + nodeLogDir + "*/", ps);
				System.out.println(out);
				ps.close();
				out.close();

				//ls -d */

				//Create tar.gz locally for a given dir
				//tar czvf mytar.tar.gz dir1

				StringTokenizer tok = new StringTokenizer(out.toString());
				//tok now contains all directories created within the experiment dir

				SftpClient sftp = ssh.openSftpClient();

				//				executeCommand(session, "cd " + nodeLogDir);

				//Now compress each directory (= experiment run)
				while (tok.hasMoreTokens()) {
					SessionChannelClient session2 = setupSSHClient(user, password, gridHost, lock).openSessionChannel();
					String token = tok.nextToken();
					String fileName = token.split("/")[5];
					//String fileName = token.substring(token.lastIndexOf('/') + 1);
					File localFile = new File(localLogDir + "/" + fileName + ".tar.gz");

					//Create the local file
					if (localFile.createNewFile()) {
						//And if it did not exist already, fill it!
						//Create the tar gz file and a dummy file that will be created when done
						executeCommand(session2, "tar czvf " + nodeLogDir + "/" + fileName + ".tar.gz " + token
								+ "&& touch " + nodeLogDir + "/" + fileName + ".done");
						//and download that file, when it is there

						ByteArrayOutputStream fileTestOut = new ByteArrayOutputStream();
						PrintStream fileTestPS = new PrintStream(fileTestOut, true, "UTF8");
						boolean done = false;
						do {
							//lock.wait(1000); //check every second if file exists
							Thread.sleep(1000);
							//						fileTestOut.reset();
							String command = "[ -e " + nodeLogDir + "/" + fileName
									+ ".done ] && echo \"TRUE\" || echo \"FALSE\"";
							//executeCommand(session, command, fileTestPS);

							try {
								sftp.get(nodeLogDir + "/" + fileName + ".done");
								done = true;
							} catch (Exception e) {
								done = false;
							}
						} while (!done);
						//} while (fileTestOut.toString().equals("FALSE"));

						BufferedOutputStream localFileStream = new BufferedOutputStream(new FileOutputStream(localFile));
						String path = nodeLogDir + fileName + ".tar.gz";
						System.out.println("Getting  " + path);
						sftp.get(path, localFileStream);

						//And parse the stats.csv file
						String settingsFile = token + "/000settings.txt";
						ByteArrayOutputStream settingsFileStream = new ByteArrayOutputStream();
						System.out.println("Getting " + settingsFile);
						sftp.get(settingsFile, settingsFileStream);

						//FIXME make less fixed, there are other subdirs possible!!! (red/blue etc., but also 0, 1, etc.)
						try {
							String statFileString = token + "/NORMAL/stats.csv";
							ByteArrayOutputStream statStream = new ByteArrayOutputStream();
							System.out.println("Getting " + statFileString);
							sftp.get(statFileString, statStream);

							resultStats.parse(statStream.toString(), settingsFileStream.toString());
						} catch (Exception e) {
							//just skip it
						}

						System.out.println("Copied logs from: " + gridHost + " for " + token);
					}//if did not exist yet
				}//while

				session.close();

			} catch (MessageStoreEOFException mseE) {
				mseE.printStackTrace();
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				if (ssh != null) {
					ssh.disconnect();
				}
			}
		}

		resultStats.writeToFile(localLogDir);

		System.out.println("All remote logs have been copied to " + localLogDir);
	}
}