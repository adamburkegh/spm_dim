// =============================================================================
// Copyright 2006-2010 Daniel W. Dyer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// =============================================================================
package org.processmining.plugins.etm.mutation.mutators.maikelvaneck;

import gnu.trove.map.TObjectIntMap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import nl.tue.astar.Trace;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeFitnessAbstract;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.operators.AbstractCrossover;

/**
 * 
 * @author Maikel van Eck
 */
public class CombinationCrossover<R extends ProbProcessArrayTree> extends AbstractCrossover<ProbProcessArrayTree> {
	//As described in thesis 3.2

	private static final int NONE = 0;
	private static final int TREE1 = 1;
	private static final int TREE2 = 2;
	private static final int BOTH = 3;

	CentralRegistry registry;
	TreeFitnessAbstract fitness;

	public CombinationCrossover(int crossoverPoints, Probability crossoverProbability) {
		super(crossoverPoints, crossoverProbability);
	}

	public CombinationCrossover(int crossoverPoints, Probability crossoverProbability, CentralRegistry registry,
			TreeFitnessAbstract fitness) {
		this(crossoverPoints, crossoverProbability);
		this.registry = registry;
		this.fitness = fitness;
	}

	protected List<ProbProcessArrayTree> mate(ProbProcessArrayTree parent1, ProbProcessArrayTree parent2, int numberOfCrossoverPoints, Random rng) {
		if (registry == null || !registry.getFitness(parent1).behaviorCounter.isAlignmentSet() 
				|| !registry.getFitness(parent2).behaviorCounter.isAlignmentSet()) {
			//FIXME sometimes gets in here, sometimes not, should never happen!
			//System.out.println("Crossover Error: no registry or no alignments stored");
			return dumbMate(parent1, parent2, numberOfCrossoverPoints, rng);
		} else {
			// Do the smart combination crossover

			// Map alignment moves to nodes
			ArrayList<AlignmentMoveList> alignmentMoves1 = AlignmentMove.getAlignmentMoveMapping(registry, parent1);
			ArrayList<AlignmentMoveList> alignmentMoves2 = AlignmentMove.getAlignmentMoveMapping(registry, parent2);

			// Determine the total number of events in the log
			TObjectIntMap<Trace> log = registry.getaStarAlgorithm().getConvertedLog();
			int logSize = 0;
			for (Object obj : log.keys()) {
				Trace trace = (Trace) obj;
				logSize += trace.getSize() * log.get(obj);
			}

			// Search through both trees to find the nodes that are best matched 
			int bestScore = Integer.MAX_VALUE;
			int bestNode1 = -1;
			int bestNode2 = -1;
			for (int node1 = 0; node1 < parent1.size(); node1++) {
				AlignmentMoveList moves1 = alignmentMoves1.get(node1);
				for (int node2 = 0; node2 < parent2.size(); node2++) {
					AlignmentMoveList moves2 = alignmentMoves2.get(node2);
					int currentScore = 0;

					// Calculate size difference & model move penalty
					int sizeDifference = Math.abs(moves1.getTotalSyncMoveCount() - moves2.getTotalSyncMoveCount());
					int modelMovePenalty = 0; //moves1.getTotalModelMoveCount() + moves2.getTotalModelMoveCount();
					currentScore += sizeDifference * 1 + modelMovePenalty * 0;
					if (currentScore > bestScore) {
						continue;
					}

					// Calculate intersection penalty
					AlignmentMoveList intersection = AlignmentMove.getIntersection(moves1, moves2);
					int intersectionPenalty = intersection.getTotalSyncMoveCount();
					// TODO: change scaling
					currentScore += intersectionPenalty * 2;
					if (currentScore > bestScore) {
						continue;
					}

					// Calculate union penalty
					AlignmentMoveList union = AlignmentMove.getUnion(moves1, moves2);
					int unionPenalty = logSize - union.getTotalSyncMoveCount();
					// TODO: change scaling
					currentScore += unionPenalty * 3;
					if (currentScore < bestScore) {
						bestScore = currentScore;
						bestNode1 = node1;
						bestNode2 = node2;
					}
				}
			}

			// Do smart root node selection
			short smartType = determineSmartRootType(log, alignmentMoves1.get(bestNode1).getAlignmentMoves(),
					alignmentMoves2.get(bestNode2).getAlignmentMoves());

			// Combine the two best matching nodes into two new trees
			ProbProcessArrayTree tree1 = TreeUtils.fromString("LEAF: tau");
			if (smartType != ProbProcessArrayTree.REVSEQ) {
				tree1.setType(0, smartType);
				tree1 = tree1.add(parent1, bestNode1, 0, 0);
				tree1 = tree1.add(parent2, bestNode2, 0, 1);
			} else {
				tree1.setType(0, ProbProcessArrayTree.SEQ);
				tree1 = tree1.add(parent1, bestNode1, 0, 1);
				tree1 = tree1.add(parent2, bestNode2, 0, 0);
			}
			ProbProcessArrayTree tree2 = new ProbProcessArrayTreeImpl(tree1);

			//Prepare the offspring list
			List<ProbProcessArrayTree> offspring = new ArrayList<ProbProcessArrayTree>(2);
			offspring.add(tree1);
			offspring.add(tree2);

			return offspring;
		}
	}

	private List<ProbProcessArrayTree> dumbMate(ProbProcessArrayTree parent1, ProbProcessArrayTree parent2, int numberOfCrossoverPoints, Random rng) {
		ProbProcessArrayTree tree1 = new ProbProcessArrayTreeImpl(parent1);
		ProbProcessArrayTree tree2 = new ProbProcessArrayTreeImpl(parent2);

		assert tree1.isConsistent();
		assert tree2.isConsistent();

		//Prepare the offspring list
		List<ProbProcessArrayTree> offspring = new ArrayList<ProbProcessArrayTree>(2);

		//Now apply crossover as many times as required
		for (int i = 0; i < numberOfCrossoverPoints; i++) {
			//Find random crossover points
			int pointTree1 = rng.nextInt(tree1.size());
			int pointTree2 = rng.nextInt(tree2.size());

			//Now apply the swap but create 2 new trees otherwise the 2nd swap swaps a different node than intended
			ProbProcessArrayTree t1new = tree1.replace(pointTree1, tree2, pointTree2);
			ProbProcessArrayTree t2new = tree2.replace(pointTree2, tree1, pointTree1);

			assert t1new.isConsistent();
			assert t2new.isConsistent();

			//Update the pointers
			tree1 = t1new;
			tree2 = t2new;
		}

		offspring.add(tree1);
		offspring.add(tree2);

		return offspring;
	}

	private short determineSmartRootType(TObjectIntMap<Trace> log, ArrayList<AlignmentMove> moves1,
			ArrayList<AlignmentMove> moves2) {
		// Determine best matching operator node at tree root

		// Connect model moves to events in the traces that might be an explanation
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

		// Determine best-node-from-tree1's coverage
		for (int i = 0; i < moves1.size(); i++) {
			if (moves1.get(i).moveType == AlignmentMove.SYNC) {
				logCoverage.get(moves1.get(i).traceID).set(moves1.get(i).traceIndex, TREE1);
			}
		}

		// Determine best-node-from-tree2's coverage
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

		//		System.out.println("LOG:" + logSize + " XOR:" + XORCount + " SEQ:" + SEQCount + " REV:" + REVSEQCount +
		//				" AND:" + ANDCount);

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

		private CombinationCrossover getOuterType() {
			return CombinationCrossover.this;
		}

	}
}
