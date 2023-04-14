package qut.pm.setm;

import java.net.InetAddress;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import qut.pm.spm.Measure;
import qut.pm.util.ClockUtil;

@Root
public class RunStats {

	private static final String LINE_SEP = "\n ==== ";
	private static final String SEP = " -- ";
		
	@Element
	protected String inputLogFileName;
	
	@Element(required=false)
	protected String inputModelFileName;

	@Element(required=false)
	protected String runExportFileName;
	
	@Attribute
	protected String artifactCreator;
	
	@Attribute
	protected String machineName;

	@Attribute(required=false)
	protected String runnerVersion = "";
	
	@Element 
	protected Date runDateTime;
	
	@ElementList
	protected List<RunStats> nestedRunStats = new LinkedList<RunStats>();
	
	@ElementList
	protected List<TaskStats> taskRunStats = new LinkedList<TaskStats>();
	
	@Element(required=false)
	protected String errorMessage = "";

	private Map<Measure,Number> allMeasures = new TreeMap<Measure,Number>();
	private Map<Measure,String> measureSources = new HashMap<Measure,String>();
	
	@Element
	private RunState runState = RunState.INITIALIZING;
	
	public RunStats(String runnerVersion, String inputLogFileName, String artifactCreator) 
	{
		this.inputLogFileName = inputLogFileName;
		this.runDateTime = ClockUtil.currentTime();
		detectMachineName();
		this.runnerVersion = runnerVersion;
		this.artifactCreator = artifactCreator;
	}

	private void detectMachineName() {
		try {
			this.machineName = InetAddress.getLocalHost().getHostName();
		}catch (Exception e){
			this.machineName = "Unkown (" + e.getMessage() + ")";
		}
	}
	
	protected RunStats(RunStats other) {
		this.inputLogFileName = other.inputLogFileName;
		this.inputModelFileName = other.inputModelFileName;
		this.artifactCreator = other.artifactCreator;
		this.runDateTime = other.runDateTime;
		this.runState = other.runState;
		this.machineName = other.machineName;
		this.taskRunStats = other.taskRunStats;
		this.nestedRunStats = other.nestedRunStats;
		this.errorMessage = other.errorMessage;
		this.runnerVersion = other.runnerVersion;
	}
	
	@SuppressWarnings("unused")
	private RunStats() {
		
	}

	public RunState getState() {
		return runState;
	}
	
	public void addTask(TaskStats taskStats) {
		if (RunState.FAILED == taskStats.getRunState()) {
			markFailed(taskStats.getErrorMessage() );
		}else {
			runState = RunState.RUNNING;
		}
		taskRunStats.add(taskStats);
		acculumateMeasure(taskStats);
	}

	public void addSubRun(RunStats runStats) {
		if (RunState.FAILED == runStats.getState()) {
			markFailed(runStats.errorMessage );
		}else {
			runState = RunState.RUNNING;
		}
		nestedRunStats.add(runStats);
	}
		
	private void acculumateMeasure(TaskStats taskStats) {
		Map<Measure,Number> taskMeasures = taskStats.getMeasures(); 
		allMeasures.putAll(taskMeasures);
		for (Measure m: taskMeasures.keySet()) {
			measureSources.put(m,taskStats.getTaskName() );
		}
	}
	
	public void markEnd() {
		if (runState != RunState.FAILED)
			runState = RunState.SUCCESS;
	}
	
	public void markFailed(String errorMessage) {
		runState = RunState.FAILED;
		this.errorMessage = errorMessage;
	}
	
	public Map<Measure,Number> getAllMeasures(){
		if (allMeasures.isEmpty()) {
			for (TaskStats taskStats: taskRunStats) {
				acculumateMeasure(taskStats);				
			}
		}
		return allMeasures;
	}
	
	public String getArtifactCreator() {
		return artifactCreator;
	}

	public List<TaskStats> getTaskRunStats() {
		return Collections.unmodifiableList( taskRunStats );
	}
	
	public String getMachineName() {
		return machineName;
	}

	public String getBuildVersion() {
		return runnerVersion;
	}
	
	public String getInputLogFileName() {
		return inputLogFileName;
	}

	public String getInputModelFileName() {
		return inputModelFileName;
	}	
	
	public void setInputModelFileName(String inputModelFileName) {
		this.inputModelFileName = inputModelFileName;
	}
	
	public String getRunExportFileName() {
		return runExportFileName;
	}

	public String initRunExportFileName(String runExportFileName) {
		if (this.runExportFileName == null)
			this.runExportFileName = runExportFileName;
		return getRunExportFileName();
	}

	public String formatStats() {
		StringBuilder result = new StringBuilder("Run " + runState + LINE_SEP);
		for (TaskStats task : taskRunStats) {
			result.append(task.getTaskName());
			result.append(SEP);
			result.append(task.getRunState());
			result.append(SEP);
			result.append(task.getDuration());
			result.append(" ms");
			result.append(LINE_SEP);
		}
		result.append(allMeasures);
		for (RunStats nestedRun: nestedRunStats) {
			result.append(nestedRun.formatStats());
			result.append(LINE_SEP);
		}
		return result.toString();
	}

		
	
}
