/**
 * 
 */
package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.BehaviorCounter;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

/**
 * @author jbuijs
 * 
 */
//FIXME check all class contents
//FIXME Test Class thoroughly
//FIXME implement for new (lack of) behavior counters
public class GeneralizationRinooyKan {//extends TreeFitnessAbstract {

	//DISABLED, uses old behaviour counter
	/*-
	public static final TreeFitnessInfo info = new TreeFitnessInfo(GeneralizationRinooyKan.class, "Gr",
			"Generalization RinooyKan",
			"Uses the principle of Rinooy Kan that uses the total number of observations and the number of unique "
					+ "observations to estimate the chance of unseen observations.", Dimension.GENERALIZATION, true);
				/**/

	private final CentralRegistry registry;

	public GeneralizationRinooyKan(CentralRegistry registry) {
		this.registry = registry;
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		double generalization = 0;
		BehaviorCounter behCounter = registry.getFitness(candidate).behaviorCounter;

		for (int i = 0; i < behCounter.getMoveCount().length; i++) {
			double n = 0; //total executions
			double w = 0; //number of different behavior seen

			//TODO The current behavior counter does not track w (e.g. different behaviors)
			/*-
			switch (candidate.getType(i)) {
				case NAryTree.SEQ : //neither do sequences?
					continue;
				case NAryTree.OR :
				case NAryTree.XOR :
					//w is whether we have seen the L/R behavior at all (so 2 if both are observed at least once)
					w += (beh.getBehavedAsL() > 0) ? 1 : 0;
					w += (beh.getBehavedAsR() > 0) ? 1 : 0;
					n += beh.getBehavedAsL() + beh.getBehavedAsR();
					if (candidate.getType(i) == NAryTree.XOR)
						break;
					//An OR Is both an XOR and an AND
					//$FALL-THROUGH$
				case NAryTree.AND :
					w += (beh.getBehavedAsAND() > 0) ? 1 : 0;
					w += (beh.getBehavedAsSEQLR() > 0) ? 1 : 0;
					w += (beh.getBehavedAsSEQRL() > 0) ? 1 : 0;
					n += beh.getBehavedAsAND() + beh.getBehavedAsSEQLR() + beh.getBehavedAsSEQRL();
					break;
				case NAryTree.LOOP :
					w += (beh.getBehavedAsL() > 0) ? 1 : 0;
					w += (beh.getBehavedAsR() > 0) ? 1 : 0;
					n += beh.getBehavedAsL() + beh.getBehavedAsR();
					break;
			}/**/
			if (w == 5) {
				// we saw the full or, generalization is perfect
				generalization += 0;
			} else if (n < w + 2) {
				// node only used once.
				generalization += 1;
			} else {
				//Returns how certain we are that we won't see new unobserved behavior
				generalization += (w * (w + 1)) / (n * (n - 1));
			}
		}

		//Prevent almost 0 values, 0.0001 is close-enough almost zero
		if (generalization < 0.0001)
			generalization = 0;

		return 1 - generalization;
	}

	/*-
	public TreeFitnessInfo getInfo() {
		return info;
	}/**/
}
