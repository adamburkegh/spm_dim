package org.processmining.plugins.etm.mutation.mutators.maikelvaneck;

import gnu.trove.map.TObjectIntMap;

import java.util.ArrayList;
import java.util.Map;

import nl.tue.astar.Trace;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.narytree.replayer.TreeRecord;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * Guided mutation with service repair recommendations (by Artem Polyvyannyy)
 * 
 * @author Joos Buijs
 */
public class MutateSingleNodeGuidedWithRepairRecommendation extends MutateSingleNodeGuided {

	private String key = "MutateSingleNodeGuidedWithRepairRecommendation";

	private static final int NONE = 0;
	private static final int TREE1 = 1;
	private static final int TREE2 = 2;
	private static final int BOTH = 3;

	public MutateSingleNodeGuidedWithRepairRecommendation(CentralRegistry registry) {
		super(registry);
	}

	/**
	 * @see TreeMutationAbstract#mutate(Node)
	 */
	public ProbProcessArrayTree mutate(ProbProcessArrayTree tree, int node) {
		assert tree.isConsistent();

		// Deep clone the tree first...
		ProbProcessArrayTree mutatedTree = new ProbProcessArrayTreeImpl(tree);

		if (registry == null || registry.getFitness(tree).behaviorCounter == null
				|| registry.getFitness(tree).behaviorCounter.isAlignmentSet() == false) {
			//System.out.println("GuidedMutation Error: no registry or no alignments stored");
		} else {
			// Select a node
			int nodeToBeMutated = -1;

			/*
			 * CALLING Artem's repair recommendation here
			 */
			//0. translate tree to PN (for now)
			
			//1. translate PN transition to tree node
			
			
			/*
			 * What follows below are standard pieces of code as demonstration @Artem
			 */
			//Get alignments for this tree
			Map<Trace, TreeRecord> alignments = registry.getFitness(tree).behaviorCounter.getAlignments();
			//A treerecord is a reverse-ordered chain of alignment moves
			
			if (nodeToBeMutated == -1) {
				// Failed to find a bad node
				return mutatedTree;
			}

			// Mutate node
			ArrayList<AlignmentMoveList> alignmentMoves = AlignmentMove.getAlignmentMoveMapping(registry, tree);
			TObjectIntMap<Trace> log = registry.getaStarAlgorithm().getConvertedLog();
			if (mutatedTree.isLeaf(nodeToBeMutated)) {
				// Insert stuff to fix behaviour of leaf node
				mutatedTree = mutateLeaf(mutatedTree, nodeToBeMutated, alignmentMoves, log);
			} else {
				// Combine children to change operator node into a smart operator node
				mutatedTree = mutateOperator(mutatedTree, nodeToBeMutated, alignmentMoves, log);
			}

			didChange(nodeToBeMutated, TypesOfTreeChange.OTHER);

			assert mutatedTree.isConsistent();
		}

		return mutatedTree;
	}

	/**
	 * @see TreeMutationAbstract#getKey()
	 */
	public String getKey() {
		return key;
	}
}
