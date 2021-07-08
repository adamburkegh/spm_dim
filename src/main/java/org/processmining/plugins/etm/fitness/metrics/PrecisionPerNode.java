package org.processmining.plugins.etm.fitness.metrics;

import java.util.List;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.BehaviorCounter;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

// FIXME check all class contents
// FIXME Test Class thoroughly
public class PrecisionPerNode {// DISABLED!!! extends TreeFitnessAbstract {
	//DISABLED because it is out of date with new behavior recordings
	
	/*-
	public static final TreeFitnessInfo info = new TreeFitnessInfo(PrecisionPerNode.class, "Pn", "Precision per node",
			"Uses the information from the behavior of each node in the tree to determine how precisely it behaved",
			Dimension.PRECISION, true);
	/**/

	private CentralRegistry registry;

	/**
	 * 
	 * @param logSize
	 *            size of the log, e.g. number of traces
	 */
	public PrecisionPerNode(CentralRegistry registry) {
		this.registry = registry;
	}

	public double getFitness(ProbProcessArrayTree candidate, List<? extends ProbProcessArrayTree> population) {
		double precision = 0;
		BehaviorCounter behCounter = registry.getFitness(candidate).behaviorCounter;

		//TODO update to new behavior counter impl.

		//Loop through all the nodes
		for (int i = 0; i < behCounter.getMoveCount().length; i++) {
			@SuppressWarnings("unused")
			short nodeType = candidate.getType(i);

			//TODO The behavior counter does not record enough information about behavior
			/*-
			switch (nodeType) {
				case NAryTree.SEQ :
					//Executing a SEQ in the wrong order is handled in fitness already and is not for precision
					break;
				case NAryTree.OR :
					//An OR is easily not precise enough since it behaves as an XOR or AND.
					//So, combine the 2 punishments for XOR and AND
				case NAryTree.XOR :
					//An XOR is not precise enough if it favors one of its children over the other
					precision += Math.abs(behavior.getBehavedAsL() - behavior.getBehavedAsR());
					if (nodeType == NAryTree.XOR) {
						//Stop the XOR
						break;
					}
					//Let the OR fall-through
					//$FALL-THROUGH$
				case NAryTree.AND :
					//An AND is not precise if it behaves sequential in a preferred direction
					//Calculate sequential preference
					precision += Math.abs(behavior.getBehavedAsSEQLR() - behavior.getBehavedAsSEQRL());
					//If it did that more often than interleaved
					break;
				case NAryTree.LOOP :
					//If a loop contains a leaf, SEQ or AND then all containing leafs should be executed.
					//However, a XOR or OR in a loop allow for all kinds of behavior so punish!
					short childType = candidate.getType(i + 1);
					if (childType == NAryTree.XOR || childType == NAryTree.OR) {
						//TODO check if this call is correct
						precision += behavior.getBehavedAsR();
					}
					break;
				default :
					break;
			}/**/
		}

		return 1 - (precision / registry.getLog().size());
	}

	/*-
	public TreeFitnessInfo getInfo() {
		return info;
	}/**/
}
