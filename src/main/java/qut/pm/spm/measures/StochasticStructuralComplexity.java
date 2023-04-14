package qut.pm.spm.measures;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.deckfour.xes.classification.XEventClassifier;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet.DistributionType;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.elements.Transition;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;

public class StochasticStructuralComplexity implements StochasticLogCachingMeasure{

	private static class Distribution {
		private DistributionType distType;
		private double[] distributionParams;
		
		public Distribution(TimedTransition ttran) {
			distType = ttran.getDistributionType();
			distributionParams = ttran.getDistributionParameters();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((distType == null) ? 0 : distType.hashCode());
			result = prime * result + Arrays.hashCode(distributionParams);
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
			Distribution other = (Distribution) obj;
			if (distType != other.distType)
				return false;
			if (!Arrays.equals(distributionParams, other.distributionParams))
				return false;
			return true;
		}
		
		
	}
	
	@Override
	public String getReadableId() {
		return "Stochastic Structural Uniqueness";
	}

	@Override
	public Measure getMeasure() {
		return Measure.MODEL_STRUCTURAL_STOCHASTIC_COMPLEXITY;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet anet, XEventClassifier classifier) {
		StochasticNet net = anet.getNet();
		Set<Distribution> distributions = new HashSet<>();
		Set<Double> weights = new HashSet<>();
		Set<Integer> priorities = new HashSet<>();
		for (Transition tran: net.getTransitions()) {
			TimedTransition ttran = (TimedTransition)tran;
			distributions.add( new Distribution(ttran));
			weights.add(ttran.getWeight());
			priorities.add(ttran.getPriority());
		}
		return net.getPlaces().size() + net.getTransitions().size() + net.getEdges().size()
				+ distributions.size() + weights.size() + priorities.size();
	}

	@Override
	public String getUniqueId() {
		return "snsunq";
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		// No-op
	}

}
