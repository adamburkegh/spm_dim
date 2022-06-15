package qut.pm.setm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import qut.pm.setm.report.PSVReportFormatter;
import qut.pm.spm.Measure;




public class SETMReporter {
	
	protected static class ReportStats extends RunStats implements Comparable<ReportStats>{

		protected String resultFile;
		protected List<ReportStats> nestedReportStats = new LinkedList<>();
		
		public ReportStats(RunStats runStats, String resultFile) 
		{
			super(runStats);
			this.resultFile = resultFile;
			for (RunStats subRun: nestedRunStats) {
				nestedReportStats.add(new ReportStats(subRun,resultFile));
			}
		}
		
		public int compareTo(ReportStats other) {
			int result = inputLogFileName.compareTo(other.inputLogFileName);
			if (result != 0)
				return result;
			result = artifactCreator.compareTo(other.artifactCreator);
			if (result != 0)
				return result;
			result = resultFile.compareTo(other.resultFile);
			return result;
		}

		public List<ReportStats> getSubReportingRuns() {
			return nestedReportStats;
		}
	}


	private static final String RESULT_FILE_PREFIX = "mrun_";
	public static final String LINE_SEP = "\n";

	
	private static Logger LOGGER = LogManager.getLogger();
	
	protected String reportDir;
	protected Map<ReportStats,ReportStats> statsDb = new TreeMap<>(); // Set interface workaround
	protected PSVReportFormatter reportFormatter = new PSVReportFormatter();
	private Set<Measure> includedMeasures = null;
	protected PrintStream printStream;

	
	public SETMReporter(String reportDir, PrintStream outputStream) {
		this.reportDir = reportDir;
		this.printStream = outputStream;
	}
	
	public SETMReporter(String reportDir) {
		this(reportDir,System.out);
	}
	

	public SETMReporter(String reportDir, String outFile) throws Exception{
		this(reportDir,new PrintStream( new FileOutputStream(outFile)));
	}

	protected void configure() {
		includedMeasures = new HashSet<>();
		includedMeasures.add(Measure.MODEL_EDGE_COUNT);
		includedMeasures.add(Measure.MODEL_ENTITY_COUNT);
		includedMeasures.add(Measure.LOG_EVENT_COUNT);
		includedMeasures.add(Measure.LOG_TRACE_COUNT);
		//
		includedMeasures.add(Measure.TRACE_OVERLAP_RATIO);
		includedMeasures.add(Measure.TRACE_PROBMASS_OVERLAP);
		includedMeasures.add(Measure.EARTH_MOVERS_TRACEWISE);
		includedMeasures.add(Measure.EVENT_RATIO_GOWER);
		includedMeasures.add(Measure.TRACE_RATIO_GOWER_2);
		includedMeasures.add(Measure.TRACE_RATIO_GOWER_3);
		includedMeasures.add(Measure.TRACE_RATIO_GOWER_4);
		includedMeasures.add(Measure.ENTROPY_FITNESS_TRACEWISE);
		includedMeasures.add(Measure.ENTROPY_PRECISION_TRACEWISE);
		includedMeasures.add(Measure.ENTROPY_FITNESS_TRACEPROJECT);
		includedMeasures.add(Measure.ENTROPY_PRECISION_TRACEPROJECT);
		includedMeasures.add(Measure.STRUCTURAL_SIMPLICITY_ENTITY_COUNT);
		includedMeasures.add(Measure.STRUCTURAL_SIMPLICITY_EDGE_COUNT);
		includedMeasures.add(Measure.STRUCTURAL_SIMPLICITY_STOCHASTIC);
		includedMeasures.add(Measure.TRACE_GENERALIZATION_FLOOR_1);
		includedMeasures.add(Measure.TRACE_GENERALIZATION_FLOOR_5);
		includedMeasures.add(Measure.TRACE_GENERALIZATION_FLOOR_10);
		includedMeasures.add(Measure.TRACE_GENERALIZATION_DIFF_UNIQ);
		//
		includedMeasures.add(Measure.EARTH_MOVERS_LIGHT_COVERAGE);
		includedMeasures.add(Measure.ENTROPY_PRECISION);
		includedMeasures.add(Measure.ENTROPY_RECALL);
	}

	
	private void loadReportFilesInDir(String reportingDir) {
		String[] reportFiles = new File(reportingDir).list((dir1, name) -> name.contains(RESULT_FILE_PREFIX));
		loadReportFiles(reportingDir, reportFiles);
	}

	private void loadReportFiles(String reportingDir, String[] reportFiles) {
		for (String reportFile: reportFiles) {
			try {
				LOGGER.info("report -- {} -- {}", reportingDir, reportFile);		
				loadResult(reportingDir, reportFile);
			}catch (Exception e) {
				LOGGER.error(reportFile,e);
			}
		}
	}
	
	private void loadResult(String reportingDir, String resultFile) throws Exception{
		Serializer serializer = new Persister();
		RunStats runStats = serializer.read(RunStats.class, new File(reportingDir + File.separator + resultFile));
		String clipped = resultFile.substring(RESULT_FILE_PREFIX.length());
		String[] ids = clipped.split("_");
		// String artifactCreatorId = ids[0];
		// String[] me = artifactCreatorId.split("-");
		// String artifactSource = artifactCreatorId;
		// if (me.length == 2) {
		// 	artifactSource = me[1];
		// }
		String log = ids[1];
		if (ids.length == 3) {
			log = log + "_" + ids[2];
		}
		if (ids.length == 4) {
			log = log + "_" + ids[2] + "_" + ids[3];
		}
		ReportStats reportStats = new ReportStats(runStats, resultFile);
		statsDb.put(reportStats,reportStats);
	}

	protected String rewriteLogName(String logFile) {
		if ("sepsis.xes".equals(logFile)) {
			return "Sepsis";
		}
		if ("rtfmp.xes".equals(logFile)) {
			return "Road Traffic Fines";
		}
		return logFile.substring(0, logFile.length()-4).replace("_", " ");
	}

	
	protected void outputReport() {
		reportFormatter.startReport();
		String[] headers = new String[includedMeasures.size() + 5];
		headers[0] = "Artifact Creator"; headers[1] = "Model Run"; headers[2] = "Log"; 
		int i=3;
		for (Measure measure: includedMeasures) {
			headers[i] = measure.toString();
			i++;
		}
		headers[i++]= "Run file";
		headers[i++]= "Machine";
		reportFormatter.reportHeader(headers);
		reportFormatter.reportNewline();
		for ( ReportStats overallRunStats : statsDb.keySet() ) {
			for ( ReportStats reportStats : overallRunStats.getSubReportingRuns() ) {
				outputLine(overallRunStats, reportStats);
			}
			if (overallRunStats.getSubReportingRuns().isEmpty()) {
				outputLine(overallRunStats, overallRunStats);
			}
		}
		reportFormatter.endReport();
		printStream.println(reportFormatter.format());
	}


	private void outputLine(ReportStats overallRunStats, ReportStats reportStats) {
		reportFormatter.reportStartLine();
		reportFormatter.reportCells(
				overallRunStats.artifactCreator,
				reportStats.artifactCreator,  
				rewriteLogName(reportStats.getInputLogFileName()));
		Set<Measure> runMeasures = reportStats.getAllMeasures().keySet();
		for (Measure measure: includedMeasures) {
			if (runMeasures.contains(measure)) {
				reportFormatter.reportCells(reportStats.getAllMeasures().get(measure).toString());
			}else {
				RunState state = reportStats.getState();
				if ( RunState.FAILED.equals( state ) || RunState.RUNNING.equals( state )) {
					reportFormatter.reportCells(state.toString());
				}else {
					reportFormatter.reportCells("    ");
				}
			}
		}
		reportFormatter.reportCells(overallRunStats.resultFile);
		reportFormatter.reportCells(overallRunStats.getMachineName() );
		reportFormatter.reportNewline();
	}
	
	
	public void report() {
		LOGGER.info("Initializing ...");
		configure();
		LOGGER.info("Loading report data from {}", reportDir);
		loadReportFilesInDir(reportDir);
		outputReport();
	}
	
	public static void main(String[] args) throws Exception{
		SETMReporter reporter = null; 
		if (args.length == 2 && "psv".equals(args[1])){
			File f = new File(args[0]);
			String outFile = "var" + File.separator + f.getName() + ".psv";
			LOGGER.info("Outputting to file {}", outFile);
			reporter = new SETMReporter(args[0],outFile);
		}else {
			reporter = new SETMReporter(args[0]);
		}
		reporter.report();
	}
	
}
