package qut.pm.setm.observer;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.processmining.models.connections.GraphLayoutConnection;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.ExecutionPolicy;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.TimeUnit;
import org.processmining.plugins.pnml.exporting.StochasticNetToPNMLConverter;
import org.processmining.plugins.pnml.simple.PNMLRoot;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.PopulationData;

import qut.pm.setm.RunStats;
import qut.pm.setm.RunStatsExporter;
import qut.pm.setm.TaskStats;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeConverter;
import qut.pm.spm.ppt.ProbProcessTreeFormatter;

public class ExportObserver implements EvolutionObserver<ProbProcessTree>{

	private static class TreeMeasureKey{
		public final ProbProcessTree tree;
		public final Measure measure;
		
		public TreeMeasureKey(ProbProcessTree tree, Measure measure) {
			this.tree = tree;
			this.measure = measure;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((measure == null) ? 0 : measure.hashCode());
			result = prime * result + ((tree == null) ? 0 : tree.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TreeMeasureKey other = (TreeMeasureKey) obj;
			if (measure != other.measure)
				return false;
			if (tree == null) {
				if (other.tree != null)
					return false;
			} else if (!tree.equals(other.tree))
				return false;
			return true;
		}
		
	}
	
	private static Logger LOGGER = LogManager.getLogger();

	private PopulationData<? extends ProbProcessTree> lastGen;
	private ProbProcessTreeConverter converter = new ProbProcessTreeConverter();
	private RunStats runStats;
	private RunStatsExporter runStatsExporter;
	private String modelPath;
	private String runId;
	private Map<TreeMeasureKey,Double> currentGenerationResults = new ConcurrentHashMap<>();
	private long durationThreshold = 5000;
	
	public ExportObserver(RunStats runStats, String outputPath) {
		this.runStats = runStats;
		this.runId = runStats.getArtifactCreator();
		runStatsExporter = new RunStatsExporter(outputPath);
		modelPath = outputPath + File.separator + runId;
		createOutputDir();
	}
	
	private void createOutputDir() {
		try {
			new File(modelPath).mkdirs();
		}catch (Exception e) {
			LOGGER.error("Couldn't create model export path",e);
		}
	}

	@Override
	public void populationUpdate(PopulationData<? extends ProbProcessTree> data) {
		LOGGER.debug("Exporting generation ", data.getGenerationNumber());
		if (lastGen != null && lastGen.getBestCandidate() == data.getBestCandidate() ) {
			noProgressGeneration(data);
		}else {
			exportGeneration(data);
		}
		lastGen = data;
	}
	
	public void recordMeasure(ProbProcessTree tree, Measure measure, double result, long duration) {
		if (duration > durationThreshold ) {
			LOGGER.info("Long-running calculation " + duration + " ms for model:");
			LOGGER.info(new ProbProcessTreeFormatter().textTree(tree));
		}
		currentGenerationResults.put( new TreeMeasureKey(tree,measure), result );
	}

	private void exportGeneration(PopulationData<? extends ProbProcessTree> data) {
		String modelName = runId + "-g" + data.getGenerationNumber();
		AcceptingStochasticNet snet = converter.convertToSNet(data.getBestCandidate(),modelName);
		try {
			storeModel(new File(modelPath + File.separator + modelName + ".pnml"),snet);
			RunStats modelRunStats = 
					new RunStats(runStats.getBuildVersion(),runStats.getInputLogFileName(),modelName);
			TaskStats taskStats = new TaskStats("collateMeasures-" + modelName);
			taskStats.markRunning();
			for (Measure measure: Measure.values()) {
				Double val = currentGenerationResults.get( new TreeMeasureKey(data.getBestCandidate(), measure) );
				if (val != null)
					taskStats.setMeasure(measure,val);
			}
			taskStats.markEnd();
			modelRunStats.addTask(taskStats);
			modelRunStats.markEnd();
			runStats.addSubRun(modelRunStats);
			runStatsExporter.exportRun(runStats);
		}catch (Exception e) {
			LOGGER.error("Couldn't export model for generation " + data.getGenerationNumber(),e);
			LOGGER.info("Failed model was:" + data.getBestCandidate());
		}
		resetCurrentGenerationMeasures();
	}

	private void resetCurrentGenerationMeasures() {
		currentGenerationResults.clear();
	}

	private void noProgressGeneration(PopulationData<? extends ProbProcessTree> data) {
		LOGGER.debug("No update to best model for generation ", data.getGenerationNumber());
	}

	private void storeModel(File modelFile, AcceptingStochasticNet stochasticNetDescriptor)
		throws Exception
	{
		StochasticNet net = stochasticNetDescriptor.getNet(); 
		PNMLRoot root = new StochasticNetToPNMLConverter().convertNet(net,
				stochasticNetDescriptor.getInitialMarking(),
				new GraphLayoutConnection(net));
		net.setExecutionPolicy(ExecutionPolicy.RACE_ENABLING_MEMORY);
		net.setTimeUnit(TimeUnit.HOURS);
		Serializer serializer = new Persister();
		serializer.write(root, modelFile);
	}

	public void close() {
		runStats.markEnd();
		try {
			runStatsExporter.exportRun(runStats);
		}catch (Exception e) {
			LOGGER.error("Couldn't export run stats at end of run");
		}
	}
	
}
