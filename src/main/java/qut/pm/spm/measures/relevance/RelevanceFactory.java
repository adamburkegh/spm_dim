package qut.pm.spm.measures.relevance;

public class RelevanceFactory {

	public static RelevanceCalculator createRelevanceCalcUniform() {
		return new RelevanceCalculator(new UniformBackgroundCost(), 
									   new UniformPreludeCost() );
	}

	public static RelevanceCalculator createRelevanceCalcZeroOrder() {
		return new RelevanceCalculator(new ZeroOrderBackgroundCost(), 
				   new ZeroOrderPreludeCost() );
	}

	public static RelevanceCalculator createRelevanceCalcRestrictedZeroOrder() {
		return new RelevanceCalculator(new RestrictedZeroOrderBackgroundCost(), 
									   new RestrictedZeroOrderPreludeCost() );
	}
	
}
