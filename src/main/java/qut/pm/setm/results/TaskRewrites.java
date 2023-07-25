package qut.pm.setm.results;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import qut.pm.setm.RunState;
import qut.pm.setm.RunStats;
import qut.pm.setm.TaskStats;
import qut.pm.spm.Measure;

public class TaskRewrites {

	public static interface Finder{
		public TaskStats findSourceTask(String sourceFile, String stat, RunStats sourceRunStats)
				throws Exception;
	}
	
	public static class NameFinder implements Finder{
		public TaskStats findSourceTask(String sourceFile, String stat, RunStats sourceRunStats)
			throws Exception
		{
			TaskStats sourceTask = null;
			List<TaskStats> taskRunStats = sourceRunStats.getTaskRunStats();
			for(TaskStats tr: taskRunStats) {
				if (stat.equals( tr.getTaskName() )) {
					sourceTask = tr;
					break;
				}
			}
			if (sourceTask == null) {
				throw new Exception("Task " + stat + " not found in " + sourceFile);
			}
			return sourceTask;
		}
	}

	public static class StatFinder implements Finder{
		public TaskStats findSourceTask(String sourceFile, String stat, RunStats sourceRunStats)
			throws Exception
		{
			TaskStats sourceTask = null;
			List<TaskStats> taskRunStats = sourceRunStats.getTaskRunStats();
			for(TaskStats tr: taskRunStats) {
				for (Measure m : tr.getMeasures().keySet()) {
					if (stat.equals( m.toString() )) {
						sourceTask = tr;
						break;
					}
				}
			}
			if (sourceTask == null) {
				throw new Exception("Task " + stat + " not found in " + sourceFile);
			}
			return sourceTask;
		}
	}

	
	public void addNote(String sourceFile, String stat, String note, Finder finder) 
			throws Exception
	{
		File sourceRunFile = new File(sourceFile);
		Serializer serializer = new Persister();
		RunStats sourceRunStats = serializer.read(RunStats.class, sourceRunFile );
		TaskStats sourceTask = finder.findSourceTask(sourceFile, stat, sourceRunStats);
		sourceTask.setNote(note);
		serializer.write(sourceRunStats, sourceRunFile);		
	}
	
	public void copyTask(String sourceFile, String stat, String targetFile, Finder finder) 
			throws Exception
	{
		Serializer serializer = new Persister();
		File sourceRunFile = new File(sourceFile);
		File targetRunFile = new File(targetFile);
		RunStats sourceRunStats = serializer.read(RunStats.class, sourceRunFile );
		RunStats targetRunStats = serializer.read(RunStats.class, targetRunFile );
		TaskStats sourceTask = finder.findSourceTask(sourceFile, stat, sourceRunStats);
		sourceTask.setNote("Copied from " + sourceFile + " on " + new Date());
		RunState origRunState = targetRunStats.getState();
		targetRunStats.addTask(sourceTask);
		if (origRunState == RunState.SUCCESS )
			targetRunStats.markEnd();
		serializer.write(targetRunStats, targetRunFile);
	}


	
}
