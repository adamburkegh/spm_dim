package qut.pm.setm;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import qut.pm.util.ClockUtil;
import qut.pm.xes.helpers.XESLogUtils;

public class RunStatsExporter{

	private static final Logger LOGGER = LogManager.getLogger();
	private String outputDir;
	
	public RunStatsExporter(String outputDir) {
		this.outputDir = outputDir;
	}
	
	public void exportRun(RunStats runStats) 
			throws Exception
	{
		String outputDataName = runStats.initRunExportFileName( nameFile(runStats, "mrun","xml"));
		Serializer serializer = new Persister();
		File outputData = new File(outputDataName);
		serializer.write(runStats, outputData);
		LOGGER.info("exported run ({}) details to {}", runStats.getState(), outputDataName);
	}

	public String nameFile(RunStats runStats, String fileTypePrefix, String suffix) {
		String dateTime = ClockUtil.dateTime();
		LOGGER.debug(runStats.getInputLogFileName());
		String inputFileName = logPrefix(runStats.getInputLogFileName());
		final String SEP = "_";
		if (runStats.getArtifactCreator() != null)
			return outputDir + File.separator + fileTypePrefix + SEP + inputFileName + SEP + runStats.getArtifactCreator() 
					+ SEP + dateTime + "." + suffix;
		return outputDir + File.separator + fileTypePrefix + SEP + inputFileName + SEP 
				+ runStats.getArtifactCreator() + SEP + dateTime + "." + suffix;
		
			
	}
	
	private String logPrefix(String inputLogName) {
		return inputLogName.substring(0, inputLogName.lastIndexOf("." + XESLogUtils.XES_FILE_SUFFIX));
	}
	
}