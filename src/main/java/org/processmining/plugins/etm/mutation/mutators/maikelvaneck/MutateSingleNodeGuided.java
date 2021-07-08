package org.processmining.plugins.etm.mutation.mutators.maikelvaneck;

import gnu.trove.map.TObjectIntMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import nl.tue.astar.Trace;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.model.ppt.TypesOfTreeChange;
import org.processmining.plugins.etm.mutation.TreeMutationAbstract;

/**
 * 
 * @author Maikel van Eck
 */
public class MutateSingleNodeGuided extends TreeMutationAbstract {
	//thesis 3.3

	private String key = "MutateSingleNodeGuided";
	private static final int NONE = 0;
	private static final int TREE1 = 1;
	private static final int TREE2 = 2;
	private static final int BOTH = 3;

	public MutateSingleNodeGuided(CentralRegistry registry) {
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
			boolean randomSelection = true;
			if (randomSelection) {
				nodeToBeMutated = registry.getRandom().nextInt(mutatedTree.getNext(node) - node) + node;
				//nodeToBeMutated = 0;
			} else {
				// Select the baddest node in the tree
				// TODO: DOES NOT WORK! ALWAYS SELECTS LEAF NODES!
				int endOfSubtree = tree.getNext(node);
				double baddestFraction = 0;
				ArrayList<AlignmentMoveList> alignmentMoves = AlignmentMove.getAlignmentMoveMapping(registry, tree);

				for (int i = node; i < endOfSubtree; i++) {
					AlignmentMoveList moves = alignmentMoves.get(i);
					double fraction = (moves.getTotalMoveCount() - moves.getTotalSyncMoveCount())
							/ (moves.getTotalMoveCount() + 1.0);
					if (fraction > baddestFraction) {
						baddestFraction = fraction;
						nodeToBeMutated = i;
					}
				}

				if (nodeToBeMutated == -1) {
					// Failed to find a bad node
					noChange();
					return mutatedTree;
				}
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

			if (mutatedTree.equals(tree)) {
				noChange();
			} else {
				didChange(nodeToBeMutated, TypesOfTreeChange.OTHER);
			}
			assert mutatedTree.isConsistent();
		}

		return mutatedTree;
	}

	protected ProbProcessArrayTree mutateLeaf(ProbProcessArrayTree mutatedTree, int nodeToBeMutated,
			ArrayList<AlignmentMoveList> alignmentMoves, TObjectIntMap<Trace> log) {
		AlignmentMoveList moves = alignmentMoves.get(nodeToBeMutated);

		if (moves.getTotalLogMoveCount() == 0 && moves.getTotalModelMoveCount() == 0) {
			// Can't improve node
		} else if (moves.getTotalLogMoveCount() == 0) {
			// This node should be skipabble
			if (moves.getTotalMoveCount() == moves.getTotalModelMoveCount()) {
				ProbProcessArrayTree tempTree = TreeUtils.fromString("LEAF: tau", registry.getEventClasses());
				mutatedTree = mutatedTree.replace(nodeToBeMutated, tempTree, 0);
			} else {
				ProbProcessArrayTree tempTree = TreeUtils.fromString("XOR( LEAF: tau , LEAF: tau )", registry.getEventClasses());
				tempTree = tempTree.replace(1, mutatedTree, nodeToBeMutated);
				mutatedTree = mutatedTree.replace(nodeToBeMutated, tempTree, 0);
			}
		} else {
			// There are events that are missing from this sub-tree
			List<Integer> eventClasses = new ArrayList<Integer>();
			for (AlignmentMove move : moves.getAlignmentMoves()) {
				if (move.moveType == AlignmentMove.LOG || move.moveType == AlignmentMove.LOGP) {
					eventClasses.add(move.eventClassID);
				}
			}

			// Select a random event type from those that are missing
			int newEvent = eventClasses.get(registry.getRandom().nextInt(eventClasses.size()));
			ProbProcessArrayTree newTree = null;

			if (mutatedTree.getType(nodeToBeMutated) == newEvent) {
				// Build a loop
				newTree = TreeUtils.fromString("LOOP( LEAF: tau , LEAF: tau , LEAF: tau )", registry.getEventClasses());
				newTree.setType(1, mutatedTree.getType(nodeToBeMutated));
			} else {
				// Build the new sub-tree
				short smartType = determineSmartRootType(log, moves.getAlignmentMoves(), newEvent);
				newTree = TreeUtils.fromString("XOR( LEAF: tau , LEAF: tau )", registry.getEventClasses());
				if (smartType != ProbProcessArrayTree.REVSEQ) {
					newTree.setType(0, smartType);
					newTree.setType(1, mutatedTree.getType(nodeToBeMutated));
					newTree.setType(2, (short) newEvent);
				} else {
					newTree.setType(0, ProbProcessArrayTree.SEQ);
					newTree.setType(1, (short) newEvent);
					newTree.setType(2, mutatedTree.getType(nodeToBeMutated));

				}
			}

			mutatedTree = mutatedTree.replace(nodeToBeMutated, newTree, 0);
		}

		return mutatedTree;
	}

	protected ProbProcessArrayTree mutateOperator(ProbProcessArrayTree mutatedTree, int nodeToBeMutated,
			ArrayList<AlignmentMoveList> alignmentMoves, TObjectIntMap<Trace> log) {
		if (mutatedTree.getType(nodeToBeMutated) == ProbProcessArrayTree.LOOP) {
			// Try the guided mutation for all leaf children
			// TODO: COME UP WITH SOMETHING SMART INSTEAD!

			int child0 = mutatedTree.getChildAtIndex(nodeToBeMutated, 0);
			int child1 = mutatedTree.getChildAtIndex(nodeToBeMutated, 1);
			int child2 = mutatedTree.getChildAtIndex(nodeToBeMutated, 2);
			if (mutatedTree.isLeaf(child0)) {
				mutatedTree = mutateLeaf(mutatedTree, child0, alignmentMoves, log);
			}
			/*
			 * else { mutatedTree = mutateOperator(mutatedTree, child0,
			 * alignmentMoves, log); }
			 */

			if (child1 == mutatedTree.getChildAtIndex(nodeToBeMutated, 1)) {
				if (mutatedTree.isLeaf(child1)) {
					mutatedTree = mutateLeaf(mutatedTree, child1, alignmentMoves, log);
				}
				/*
				 * else { mutatedTree = mutateOperator(mutatedTree, child1,
				 * alignmentMoves, log); }
				 */
			}

			if (child2 == mutatedTree.getChildAtIndex(nodeToBeMutated, 2)) {
				if (mutatedTree.isLeaf(child2)) {
					mutatedTree = mutateLeaf(mutatedTree, child2, alignmentMoves, log);
				}
				/*
				 * else { mutatedTree = mutateOperator(mutatedTree, child2,
				 * alignmentMoves, log); }
				 */
			}
		} else {
			if (mutatedTree.nChildren(nodeToBeMutated) < 2) {
				// Transform operator into random operator
				mutatedTree.setType(nodeToBeMutated, TreeUtils.getRandomOperatorType(registry.getRandom(), 2)); //was 3
			} else {
				// Combine children to change operator node into a smart operator node

				// Build a list of children for the nodeToBeMutated
				List<Integer> children = new ArrayList<Integer>();
				for (int i = 0; i < mutatedTree.nChildren(nodeToBeMutated); i++) {
					children.add(mutatedTree.getChildAtIndex(nodeToBeMutated, i));
				}

				// Create a new tree containing the first two random children
				int child0 = children.remove(registry.getRandom().nextInt(children.size()));
				int child1 = children.remove(registry.getRandom().nextInt(children.size()));
				short smartType = determineSmartRootType(log, alignmentMoves.get(child0).getAlignmentMoves(),
						alignmentMoves.get(child1).getAlignmentMoves());
				ProbProcessArrayTree newTree = TreeUtils.fromString("LEAF: tau");
				if (smartType != ProbProcessArrayTree.REVSEQ) {
					newTree.setType(0, smartType);
					newTree = newTree.add(mutatedTree, child0, 0, 0);
					newTree = newTree.add(mutatedTree, child1, 0, 1);
				} else {
					newTree.setType(0, ProbProcessArrayTree.SEQ);
					newTree = newTree.add(mutatedTree, child0, 0, 1);
					newTree = newTree.add(mutatedTree, child1, 0, 0);
				}
				AlignmentMoveList combinedMoves = AlignmentMove.getUnionIncMM(alignmentMoves.get(child0),
						alignmentMoves.get(child1));

				for (int i = 0; i < mutatedTree.nChildren(nodeToBeMutated) - 2; i++) {
					// Add all remaining children randomly to the new sub-tree through smart operators
					int nextChild = children.remove(registry.getRandom().nextInt(children.size()));
					smartType = determineSmartRootType(log, combinedMoves.getAlignmentMoves(),
							alignmentMoves.get(nextChild).getAlignmentMoves());
					ProbProcessArrayTree tempTree = TreeUtils.fromString("LEAF: tau");
					if (smartType != ProbProcessArrayTree.REVSEQ) {
						tempTree.setType(0, smartType);
						tempTree = tempTree.add(newTree, 0, 0, 0);
						tempTree = tempTree.add(mutatedTree, nextChild, 0, 1);
					} else {
						tempTree.setType(0, ProbProcessArrayTree.SEQ);
						tempTree = tempTree.add(newTree, 0, 0, 1);
						tempTree = tempTree.add(mutatedTree, nextChild, 0, 0);
					}
					newTree = newTree.replace(0, tempTree, 0);
					combinedMoves = AlignmentMove.getUnionIncMM(combinedMoves, alignmentMoves.get(nextChild));
				}
				mutatedTree = mutatedTree.replace(nodeToBeMutated, newTree, 0);
			}
		}

		return mutatedTree;
	}

	private short determineSmartRootType(TObjectIntMap<Trace> log, ArrayList<AlignmentMove> moves, int newEvent) {
		// Determine best matching operator node between sub-tree and new event
		ArrayList<ArrayList<Integer>> logCoverage = new ArrayList<ArrayList<Integer>>();
		int logSize = 0;

		// Initialize log coverage indicator
		for (int i = 0; i < log.keys().length; i++) {
			Trace trace = (Trace) log.keys()[i];
			ArrayList<Integer> eventList = new ArrayList<Integer>();

			for (int j = 0; j < trace.getSize(); j++) {
				if (trace.get(j) == newEvent) {
					eventList.add(TREE2);
				} else {
					eventList.add(NONE);
				}
			}

			logCoverage.add(eventList);
			logSize += log.get(trace);
		}

		// Determine root-node's coverage
		for (int i = 0; i < moves.size(); i++) {
			if (moves.get(i).moveType == AlignmentMove.SYNC) {
				if (logCoverage.get(moves.get(i).traceID).get(moves.get(i).traceIndex) == TREE2) {
					logCoverage.get(moves.get(i).traceID).set(moves.get(i).traceIndex, BOTH);
				} else {
					logCoverage.get(moves.get(i).traceID).set(moves.get(i).traceIndex, TREE1);
				}
			}
		}

		// Aggregate log coverage
		int SEQCount = 0;
		int REVSEQCount = 0;
		int XORCount = 0;
		int ANDCount = 0;
		// Loop over traces
		for (int i = 0; i < logCoverage.size(); i++) {
			int traceCount = log.get(log.keys()[i]);
			int noneCount = 0;
			int bothCount = 0;
			boolean seenTree1 = false;
			boolean seenTree2 = false;
			boolean firstTree1 = false;
			boolean firstTree2 = false;
			boolean notSEQ = false;
			boolean notREVSEQ = false;

			// Loop over moves
			for (int j = 0; j < logCoverage.get(i).size(); j++) {
				switch (logCoverage.get(i).get(j)) {
					case NONE :
						// Do nothing
						noneCount += 1;
						break;
					case TREE1 :
						seenTree1 = true;
						if (!firstTree2) {
							firstTree1 = true;
							notREVSEQ = true;
							if (seenTree2) {
								notSEQ = true;
							}
						}
						break;
					case TREE2 :
						seenTree2 = true;
						if (!firstTree1) {
							firstTree2 = true;
							notSEQ = true;
							if (seenTree1) {
								notREVSEQ = true;
							}
						}
						break;
					case BOTH :
						// Do nothing
						bothCount += 1;
						break;
					default :
						assert false;
				}
			}

			// Determine trace type
			if (!seenTree1 && !seenTree2 && bothCount == 0) {
				// Trace not covered
				logSize -= traceCount;
			} else if (!seenTree1 && !seenTree2) {
				// Entire trace can be executed by both sub-trees
				// To help keep precision high, we do not want to introduce AND or OR nodes here
				SEQCount += traceCount;
				REVSEQCount += traceCount;
				XORCount += traceCount;
			} else if (!notSEQ && seenTree2) {
				// A proper sequence starting with tree1 and containing at least one tree2 node
				SEQCount += traceCount;
				ANDCount += traceCount;
			} else if (!notREVSEQ && seenTree1) {
				// A proper sequence starting with tree2 and containing at least one tree1 node
				REVSEQCount += traceCount;
				ANDCount += traceCount;
			} else if (!seenTree1 || !seenTree2) {
				// A trace covered entirely by a single tree and not the other
				XORCount += traceCount;
			} else if (10 * bothCount < 8 * (logCoverage.get(i).size() - noneCount)) {
				// >20% of moves cannot be executed by both trees
				ANDCount += traceCount;
			}
		}

		// Determine operator type
		if (10 * XORCount >= 8 * logSize) {
			// >=80% XOR traces
			return ProbProcessArrayTree.XOR;
		} else if (10 * SEQCount >= 8 * logSize) {
			// >=80% SEQ traces
			return ProbProcessArrayTree.SEQ;
		} else if (10 * REVSEQCount >= 8 * logSize) {
			// >=80% REVSEQ traces
			return ProbProcessArrayTree.REVSEQ;
		} else if (10 * ANDCount >= 8 * logSize) {
			// >=80% AND traces
			return ProbProcessArrayTree.AND;
		} else if (10 * SEQCount + 10 * REVSEQCount >= 8 * logSize) {
			// >=80% ILV traces
			return ProbProcessArrayTree.ILV;
		} else {
			return ProbProcessArrayTree.OR;
		}
	}

	private short determineSmartRootType(TObjectIntMap<Trace> log, ArrayList<AlignmentMove> moves1,
			ArrayList<AlignmentMove> moves2) {
		// Determine best matching operator node at tree root

		// Initialize model move coverage
		Set<Pair> tree1ModelMoves = new HashSet<Pair>();
		for (int i = 0; i < moves1.size(); i++) {
			if (moves1.get(i).moveType == AlignmentMove.MODEL) {
				tree1ModelMoves.add(new Pair(moves1.get(i).traceID, moves1.get(i).eventClassID));
			}
		}

		Set<Pair> tree2ModelMoves = new HashSet<Pair>();
		for (int i = 0; i < moves2.size(); i++) {
			if (moves2.get(i).moveType == AlignmentMove.MODEL) {
				tree2ModelMoves.add(new Pair(moves2.get(i).traceID, moves2.get(i).eventClassID));
			}
		}

		// Initialize log coverage indicator
		ArrayList<ArrayList<Integer>> logCoverage = new ArrayList<ArrayList<Integer>>();
		int logSize = 0;
		for (int i = 0; i < log.keys().length; i++) {
			Trace trace = (Trace) log.keys()[i];
			ArrayList<Integer> eventList = new ArrayList<Integer>();

			for (int j = 0; j < trace.getSize(); j++) {
				if (tree1ModelMoves.contains(new Pair(i, trace.get(j)))) {
					if (tree2ModelMoves.contains(new Pair(i, trace.get(j)))) {
						eventList.add(BOTH);
					} else {
						eventList.add(TREE1);
					}
				} else if (tree2ModelMoves.contains(new Pair(i, trace.get(j)))) {
					eventList.add(TREE2);
				} else {
					eventList.add(NONE);
				}
			}

			logCoverage.add(eventList);
			logSize += log.get(trace);
		}

		// Determine tree1's coverage
		for (int i = 0; i < moves1.size(); i++) {
			if (moves1.get(i).moveType == AlignmentMove.SYNC) {
				logCoverage.get(moves1.get(i).traceID).set(moves1.get(i).traceIndex, TREE1);
			}
			if (moves1.get(i).moveType == AlignmentMove.MODEL) {
				// Add to tree1ModelMoves

			}
		}

		// Determine tree2's coverage
		for (int i = 0; i < moves2.size(); i++) {
			if (moves2.get(i).moveType == AlignmentMove.SYNC) {
				if (logCoverage.get(moves2.get(i).traceID).get(moves2.get(i).traceIndex) == TREE1) {
					logCoverage.get(moves2.get(i).traceID).set(moves2.get(i).traceIndex, BOTH);
				} else {
					logCoverage.get(moves2.get(i).traceID).set(moves2.get(i).traceIndex, TREE2);
				}
			}
		}

		// Aggregate log coverage
		int SEQCount = 0;
		int REVSEQCount = 0;
		int XORCount = 0;
		int ANDCount = 0;
		// Loop over traces
		for (int i = 0; i < logCoverage.size(); i++) {
			int traceCount = log.get(log.keys()[i]);
			int noneCount = 0;
			int bothCount = 0;
			boolean seenTree1 = false;
			boolean seenTree2 = false;
			boolean firstTree1 = false;
			boolean firstTree2 = false;
			boolean notSEQ = false;
			boolean notREVSEQ = false;

			// Loop over moves
			for (int j = 0; j < logCoverage.get(i).size(); j++) {
				switch (logCoverage.get(i).get(j)) {
					case NONE :
						// Do nothing
						noneCount += 1;
						break;
					case TREE1 :
						seenTree1 = true;
						if (!firstTree2) {
							firstTree1 = true;
							notREVSEQ = true;
							if (seenTree2) {
								notSEQ = true;
							}
						}
						break;
					case TREE2 :
						seenTree2 = true;
						if (!firstTree1) {
							firstTree2 = true;
							notSEQ = true;
							if (seenTree1) {
								notREVSEQ = true;
							}
						}
						break;
					case BOTH :
						// Do nothing
						bothCount += 1;
						break;
					default :
						assert false;
				}
			}

			// Determine trace type
			if (!seenTree1 && !seenTree2 && bothCount == 0) {
				// Trace not covered
				logSize -= traceCount;
			} else if (!seenTree1 && !seenTree2) {
				// Entire trace can be executed by both sub-trees
				// To help keep precision high, we do not want to introduce AND or OR nodes here
				SEQCount += traceCount;
				REVSEQCount += traceCount;
				XORCount += traceCount;
			} else if (!notSEQ && seenTree2) {
				// A proper sequence starting with tree1 and containing at least one tree2 node
				SEQCount += traceCount;
				ANDCount += traceCount;
			} else if (!notREVSEQ && seenTree1) {
				// A proper sequence starting with tree2 and containing at least one tree1 node
				REVSEQCount += traceCount;
				ANDCount += traceCount;
			} else if (!seenTree1 || !seenTree2) {
				// A trace covered entirely by a single tree and not the other
				XORCount += traceCount;
			} else if (10 * bothCount < 8 * (logCoverage.get(i).size() - noneCount)) {
				// >20% of moves cannot be executed by both trees
				ANDCount += traceCount;
			}
		}

		// Determine operator type
		if (10 * XORCount >= 8 * logSize) {
			// >=80% XOR traces
			return ProbProcessArrayTree.XOR;
		} else if (10 * SEQCount >= 8 * logSize) {
			// >=80% SEQ traces
			return ProbProcessArrayTree.SEQ;
		} else if (10 * REVSEQCount >= 8 * logSize) {
			// >=80% REVSEQ traces
			return ProbProcessArrayTree.REVSEQ;
		} else if (10 * ANDCount >= 8 * logSize) {
			// >=80% AND traces
			return ProbProcessArrayTree.AND;
		} else if (10 * SEQCount + 10 * REVSEQCount >= 8 * logSize) {
			// >=80% ILV traces
			return ProbProcessArrayTree.ILV;
		} else {
			return ProbProcessArrayTree.OR;
		}
	}

	private class Pair {

		public final int first; // Trace ID
		public final int second; // Event Class

		public Pair(int first, int second) {
			this.first = first;
			this.second = second;
		}

		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + first;
			result = prime * result + second;
			return result;
		}

		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Pair other = (Pair) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (first != other.first)
				return false;
			if (second != other.second)
				return false;
			return true;
		}

		private MutateSingleNodeGuided getOuterType() {
			return MutateSingleNodeGuided.this;
		}

	}

	/**
	 * @see TreeMutationAbstract#getKey()
	 */
	public String getKey() {
		return key;
	}
}
