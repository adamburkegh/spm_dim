
package qut.pm.setm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.CentralRegistry;

import qut.pm.setm.observer.ExportObserver;
import qut.pm.setm.parameters.SETMParam;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.util.ClockUtil;

/**
 * Command Line entry point for Stochastic Evolutionary Tree Miner. For usage see 
 * the runMiner() method.
 *
 * Initial version May 2021
 *  
 * @author burkeat
 *
 */
public class SETMCommandLine {

	
	private static Logger LOGGER = LogManager.getLogger();
	
	public static final String NORMAL = "NORMAL"; //Act normal: e.g. 1 log, 1 process tree
	
	private XLog log;
	private String logFilename;
	SETMConfiguration setmConfig = new SETMConfiguration();

	public static void outputTree(String outputDir, String key, ProbProcessTree tree, CentralRegistry centralRegistry) {
		String string = key + " " + tree + " ";
		LOGGER.info(string);

		new File(outputDir).mkdirs();
		File outputFile = new File(outputDir + File.separator + key + ".ptree");
		try {
			outputFile.createNewFile();
			FileWriter writer = new FileWriter(outputFile);
			writer.append(string);
			writer.append("\n");
			writer.flush();
			writer.close();
		} catch (IOException e) {
			LOGGER.error("Problem writing file",e);
		}

	}
	
	private void normalMethod(String loggingPath, SETMConfigParams configParams) {
		LOGGER.info("Starting " + NORMAL);
		RunStats runStats = new RunStats(configParams.buildVersion, configParams.logFileName, configParams.runId);
		ExportObserver exportObserver = new ExportObserver(runStats, loggingPath);
		SETMParam etmParamMind = setmConfig.buildETMParam(loggingPath, log, configParams, exportObserver);
		SETM etmMind = new SETM(etmParamMind);
		etmMind.run();
		ProbProcessTree treeMind = etmMind.getResult();
		etmMind.getSatisfiedTerminationConditions();
		LOGGER.info("Termination conditions met: " + etmMind.getTerminationDescription());
		outputTree(etmParamMind.getPath(), NORMAL + "-log", treeMind, etmParamMind.getCentralRegistry());
	}
	
	public void runMiner(String[] args) {
    	Options options = new Options();
    	options.addOption( Option.builder("log").hasArg().required().desc("log file").build() );
    	options.addOption( Option.builder("logpath").hasArg().required().desc("log path").build() );
    	options.addOption( Option.builder("config").hasArg().desc("config file").build() );
    	CommandLineParser parser = new DefaultParser();
    	try {
	    	CommandLine line = parser.parse( options, args );
	    	String log = line.getOptionValue("log");
	    	String logpath = line.getOptionValue("logpath");
	    	initLogs(log);
	    	SETMConfigParams configParams ;
	    	String cfgArg = line.getOptionValue("config");
	    	if (cfgArg == null)
	    		configParams = configureDefaults();
	    	else
	    		configParams = configureFromFile(cfgArg);
	    	configParams.runId = "setmcmd-s" + configParams.seed + "-" + ClockUtil.dateTime();
    		normalMethod(logpath,configParams);
    	}catch (ParseException e) {
    		HelpFormatter formatter = new HelpFormatter();
    		formatter.printHelp( "setm", options );
    		System.exit(1);
    	}catch (Exception e) {
    		LOGGER.error("Unexpected error, exiting", e);
    		System.exit(1);
    	}
	}
	
    private SETMConfigParams configureFromFile(String optionValue) throws Exception {
		Properties cfg = new Properties();
		cfg.load(new FileInputStream( optionValue ));
		SETMConfigParams result = setmConfig.configureFromProperties(cfg);
		result.logFileName = this.logFilename;
		return result;
	}

	private SETMConfigParams configureDefaults() {
		SETMConfigParams configParams = new SETMConfigParams();
		configParams.logFileName = this.logFilename;
		return configParams;
	}

	private void initLogs(String logFile) throws Exception{
    	try {
    		// One log supported only
	    	XUniversalParser parser = new XUniversalParser();
	    	File lf = new File(logFile);
			Collection<XLog> xlogs = parser.parse(lf);
	    	log = xlogs.iterator().next();
	    	logFilename = lf.getName(); 
    	}catch (Exception ioe) {
    		LOGGER.error("Problem opening file " + logFile + ": " + ioe.getMessage() );
    		throw ioe;
    	}
	}

	public static void main(String[] args) {
        new SETMCommandLine().runMiner(args);
    }
}
