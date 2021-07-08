package qut.pm.setm.factory;

import java.util.List;
import java.util.Random;

import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;

import qut.pm.setm.RandomProbProcessTreeGenerator;
import qut.pm.spm.ppt.ProbProcessTree;

public class BasicPPTEvolutionFactory extends AbstractCandidateFactory<ProbProcessTree>{

	protected List<String> activities;
	
	public BasicPPTEvolutionFactory(List<String> activities) {
		this.activities = activities;
	}
	
	@Override
	public ProbProcessTree generateRandomCandidate(Random rng) {
		RandomProbProcessTreeGenerator treeGenerator = new RandomProbProcessTreeGenerator(rng);
		return treeGenerator.generateTree(activities);
	}

}
