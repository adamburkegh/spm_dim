package qut.pm.setm.evaluation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.ProMCanceller;
import org.processmining.plugins.InductiveMiner.Pair;
import org.processmining.stochasticawareconformancechecking.automata.Log2StochasticDeterministicFiniteAutomaton;
import org.processmining.stochasticawareconformancechecking.automata.StochasticDeterministicFiniteAutomatonMapped;
import org.processmining.stochasticawareconformancechecking.helperclasses.RelativeEntropy;
import org.processmining.stochasticawareconformancechecking.helperclasses.StochasticPetriNet2StochasticDeterministicFiniteAutomaton2;

import qut.pm.setm.TaskStats;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;



/**
 * Copy by @author burkeat of Sander Leemans' entropy calculation code as described in 
 * 
 * Leemans et al - Stochastic-Aware Conformance Checking: An Entropy-Based Approach
 * and presented at CAISE 2020
 * 
 * Adapted from caise2020.Perform4Entropy revision 43047 as at 10/02/20 in 
 * https://svn.win.tue.nl/repos/prom/Packages/SanderLeemans/Trunk/src/caise2020/Perform4Entropy.java
 * 
 * 
 *
 */
public class EntropyPrecisionRecallCalculator implements SPNQualityCalculator {
	
	private static Logger LOGGER = LogManager.getLogger();
	
	@Override
	public String getReadableId() {
		return "Entropy Precision and Recall";
	}
	
	@Override
	public String getUniqueId() {
		return "H_pr";
	}

	
	@Override
	public void calculate(PluginContext context, AcceptingStochasticNet net, XLog log, 
			XEventClassifier classifier, TaskStats stats) throws Exception 
	{
		LOGGER.info("Computing entropy-based quality measures (SL) ");
		StochasticDeterministicFiniteAutomatonMapped automatonA = Log2StochasticDeterministicFiniteAutomaton
				.convert(log, 
						classifier, 
						new ProMCanceller() {
							public boolean isCancelled() {
								return context.getProgress().isCancelled();
							}
						});

		StochasticDeterministicFiniteAutomatonMapped automatonB = 
				StochasticPetriNet2StochasticDeterministicFiniteAutomaton2
					.convert(net.getNet(), net.getInitialMarking());
		final Pair<Double, Double> p = RelativeEntropy.relativeEntropyHalf(automatonA, automatonB);
		stats.setMeasure(Measure.ENTROPY_RECALL, p.getA());
		stats.setMeasure(Measure.ENTROPY_PRECISION, p.getB());
	}

}

