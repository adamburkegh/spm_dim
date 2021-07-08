package org.processmining.plugins.etm.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.InductiveMiner.mining.IMLogInfo;
import org.processmining.plugins.InductiveMiner.mining.MinerState;
import org.processmining.plugins.InductiveMiner.mining.MiningParametersIMf;
import org.processmining.plugins.InductiveMiner.mining.fallthrough.FallThrough;
import org.processmining.plugins.InductiveMiner.mining.logs.IMLog;
import org.processmining.plugins.InductiveMiner.plugins.IMProcessTree;
import org.processmining.plugins.etm.ETM;
import org.processmining.plugins.etm.model.narytree.conversion.NAryTreeToProcessTree;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.parameters.ETMParam;
import org.processmining.plugins.etm.parameters.ETMParamFactory;
import org.processmining.processtree.Block;
import org.processmining.processtree.Node;
import org.processmining.processtree.ProcessTree;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.TerminationCondition;

public class InductiveHybridMiner {

	public static void main(String... args) {
		InductiveHybridMiner ihm = new InductiveHybridMiner();
		XLog log = LogCreator.createLog(new String[][] { {"A","B"},{ "A", "B","C","B","C","A","B"} },
				new int[] { 1 ,1 });
		ihm.RunInductiveHybridMiner(log);
	}

	public ProcessTree RunInductiveHybridMiner(XLog log) {
		MiningParametersIMf param = new MiningParametersIMf();
		
		param.setNoiseThreshold(0);

		List<FallThrough> fallThroughs = new ArrayList<FallThrough>(Arrays.asList(
//						new FallThroughActivityOncePerTraceConcurrent(true), 
				new FallThroughBottomUpThenOther(new FallThroughETM())
//								new FallThroughActivityConcurrent(), //most expensive as it tries brute force
//								new FallThroughTauLoopStrict(false), new FallThroughTauLoop(false),
//								new FallThroughFlowerWithoutEpsilon(), new FallThroughFlowerWithEpsilon()
				));

		param.setFallThroughs(fallThroughs);

		ProcessTree pt = IMProcessTree.mineProcessTree(log, param);

		System.out.println("Process Tree:" + pt.toString());

		return pt;
	}

	public class FallThroughBottomUpThenOther implements FallThrough {

		private FallThrough other;

		public FallThroughBottomUpThenOther(FallThrough other) {
			this.other = other;
		}

		public Node fallThrough(IMLog log, IMLogInfo logInfo, ProcessTree tree, MinerState minerState) {

			//First try IM from bottom up, and collapse this smartly (@Sander)
			ProcessTree collapsedTree = tree;

			//Then for the remaining part, call ETM
			Node ETMNode = other.fallThrough(log, logInfo, collapsedTree, minerState);

			//Now unfold (@Sander)

			return ETMNode; //UPDATE
		}

	}

	public class FallThroughETM implements FallThrough {

		public Node fallThrough(IMLog log, IMLogInfo logInfo, ProcessTree tree, final MinerState minerState) {
			XLog xlog = log.toXLog();

			ETMParam param = ETMParamFactory.buildStandardParam(xlog);
			param.addTerminationCondition(new TerminationCondition() {
				public boolean shouldTerminate(PopulationData<?> populationData) {
					return minerState.isCancelled();
				}
			});
			ETM etm = new ETM(param);
			System.out.println("Calling ETM, current tree: " + tree.toString());
			etm.run();

			//return null if we fail, for instance if out of time and current tree is not good enough
			//			return null;

			ProbProcessArrayTree nat = etm.getAllBestResults().iterator().next();
			ProcessTree pt = NAryTreeToProcessTree.convert(nat, param.getCentralRegistry().getEventClasses());
			System.out.println("ETM found " + TreeUtils.toString(nat, param.getCentralRegistry().getEventClasses())
					+ " or " + pt.toString());
			Node root = pt.getRoot();
			addNodes(tree, pt.getRoot());

			//first add all nodes of ETM PT recursively to given tree, then return root of process tree
			return root;
		}

		private void addNodes(ProcessTree tree, Node root) {

			if (!root.isLeaf()) {
				Block block = (Block) root;
				for (Node node : block.getChildren()) {
					addNodes(tree, node);
				}
			}

			tree.addNode(root);
		}

	}

}
