package qut.pm.spm.measures;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;
import org.processmining.models.semantics.IllegalTransitionException;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomaton.EdgeIterable;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.helperclasses.StochasticPetriNet2StochasticDeterministicFiniteAutomaton2;
import org.processmining.stochasticawareconformancechecking.helperclasses.UnsupportedPetriNetException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

/**
 * Wrapper around the entropic relevance measure, calculated by the externally
 * packaged entropia tool. This implements (amongst other things):
 * 
 * Alkhammash, H., Polyvyanyy, A., Moffat, A., & Garcia-Banuelos, L. (2021). 
 * Entropic relevance: A mechanism for measuring stochastic process models 
 * discovered from event data. Information Systems, 101922. 
 * https://doi.org/10.1016/j.is.2021.101922
 * 
 * @author burkeat
 *
 */
public class EntropicRelevanceUniformExternalMeasure implements StochasticLogCachingMeasure{

	private static final String ENTROPIA_SUBDIR = "entropia";
	private static final String ENTROPIA_JAR = "jbpt-pm-entropia-1.6.jar";


	/* Data objects for JSON export */
	private class SDFAExport{
		@SuppressWarnings("unused")
		private int initialState;
		private List<SDFAExportTransition> transitions; 
	}
	@SuppressWarnings("unused")
	private class SDFAExportTransition{
		private int from;
		private int to;
		private String label;
		private double prob;
 	}

	
	private static final Logger LOGGER = LogManager.getLogger();
	
	@Override
	public String getUniqueId() {
		return "entrelex";
	}

	@Override
	public String getReadableId() {
		return "Entropic Relevance (External)";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ENTROPIC_RELEVANCE_UNIFORM;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		try {
			StochasticDeterministicFiniteAutomatonMapped automatonB = 
					StochasticPetriNet2StochasticDeterministicFiniteAutomaton2
						.convert(net.getNet(), net.getInitialMarking());
			File sdfaModelFile = File.createTempFile("sdfa", ".sdfa");
			exportSDFA( automatonB, sdfaModelFile.toPath() );
			return invokeRelevanceTool(log,sdfaModelFile);
		} catch (IllegalTransitionException | UnsupportedPetriNetException e) {
			LOGGER.error("Unsupported net",e);
			throw new RuntimeException("Error",e);
		} catch (IOException e) {
			LOGGER.error("Could not write file",e);
			throw new RuntimeException("Error",e);
		} catch (InterruptedException e) {
			LOGGER.error("Could not invoke entropia process",e);
			throw new RuntimeException("Error",e);
		}
	}


	private void exportSDFA(StochasticDeterministicFiniteAutomatonMapped automatonB, 
				Path toPath) throws IOException
	{
		Gson builder = new GsonBuilder().create();
		SDFAExport sdfaExport = prepareExportObject(automatonB);
		String jsonText = builder.toJson(sdfaExport);
		LOGGER.info(jsonText);
		Files.writeString(toPath, jsonText, StandardCharsets.UTF_8);
	}

	private SDFAExport prepareExportObject(StochasticDeterministicFiniteAutomatonMapped automatonB) {
		SDFAExport sdfaExport = new SDFAExport();
		sdfaExport.initialState = automatonB.getInitialState();
		sdfaExport.transitions = new LinkedList<SDFAExportTransition>();
		EdgeIterable iter = automatonB.getEdgesIterator();
		while (iter.hasNext()) {
			iter.next();
			SDFAExportTransition et = new SDFAExportTransition();
			et.from = iter.getSource();
			et.to = iter.getTarget();
			et.label = automatonB.transform( iter.getActivity() );
			et.prob = iter.getProbability();
			sdfaExport.transitions.add(et);
		}
		return sdfaExport;
	}

	private double invokeRelevanceTool(ProvenancedLog log, File sdfaModelFile) 
			throws InterruptedException, IOException
	{
		String libpath = new File("lib" + File.separator + ENTROPIA_SUBDIR).getAbsolutePath() + File.separator;
		String classpath = libpath + ENTROPIA_JAR + File.pathSeparator 
				+ libpath + "lib" + File.separator + "*" + File.pathSeparator;
		LOGGER.debug("Command classpath: {}", classpath);
		ProcessBuilder pb = new ProcessBuilder("java",
				"-jar", libpath + ENTROPIA_JAR,
				"-s", // Entropia silent mode
				"-r", "-rel=" + log.getLogFilePath(), 
				"-ret="+sdfaModelFile.getAbsolutePath());
		LOGGER.debug(  pb.command() );
		Process proc = pb.start();
		String procOut = new String(proc.getInputStream().readAllBytes());
		try {
			return Double.valueOf(procOut.split("\n")[0]);
		}catch(Exception e) {
			LOGGER.error("Unable to parse entropic relevance return value ", procOut );
			throw new RuntimeException("Error",e);
		}
	}

	
	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		// No-op no caching for now
	}

}
