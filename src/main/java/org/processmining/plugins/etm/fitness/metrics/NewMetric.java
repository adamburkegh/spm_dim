package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo.Dimension;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NewMetric extends TreeFitnessAbstract {

	public static final TreeFitnessInfo info = new TreeFitnessInfo(NewMetric.class, "Nm", "New Metric",
			"A new fitness metric template",
			Dimension.FITNESS, true);
	private CentralRegistry registry;

	public NewMetric(CentralRegistry registry) {
		this.registry = registry;
	}

	public NewMetric(NewMetric original) {
		this.registry = original.registry;
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		//TODO IMPLEMENT YOUR LOGIC HERE
		return 0;
	}

	public TreeFitnessInfo getInfo() {
		return info;
	}

}
