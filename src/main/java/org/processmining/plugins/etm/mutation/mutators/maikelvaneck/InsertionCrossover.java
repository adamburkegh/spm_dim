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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.operators.AbstractCrossover;

/**
 * Fully functional implementation of another crossover that does not provide
 * significant improvements
 * 
 * @author Maikel van Eck
 */
public class InsertionCrossover<R extends ProbProcessArrayTree> extends AbstractCrossover<ProbProcessArrayTree> {
	//Mentioned in chapter 5 of thesis

	CentralRegistry registry;

	public InsertionCrossover(int crossoverPoints, Probability crossoverProbability) {
		super(crossoverPoints, crossoverProbability);
	}

	public InsertionCrossover(int crossoverPoints, Probability crossoverProbability, CentralRegistry registry) {
		this(crossoverPoints, crossoverProbability);
		this.registry = registry;
	}

	/**
	 * Swap two nodes between the trees based on alignment move mapping
	 */
	protected List<ProbProcessArrayTree> mate(ProbProcessArrayTree parent1, ProbProcessArrayTree parent2, int numberOfCrossoverPoints, Random rng) {
		if (registry == null || registry.getFitness(parent1).behaviorCounter == null
				|| registry.getFitness(parent2).behaviorCounter == null) {
			System.out.println("Error: no registry or no alignments stored");
			return dumbMate(parent1, parent2, numberOfCrossoverPoints, rng);
		} else {
			// Do the smart insertion crossover
			ProbProcessArrayTree tree1 = new ProbProcessArrayTreeImpl(parent1);
			ProbProcessArrayTree tree2 = new ProbProcessArrayTreeImpl(parent2);
			ArrayList<AlignmentMoveList> alignmentMoves1 = AlignmentMove.getAlignmentMoveMapping(registry, parent1);
			ArrayList<AlignmentMoveList> alignmentMoves2 = AlignmentMove.getAlignmentMoveMapping(registry, parent2);
			boolean randomSelection = false;
			boolean includeMM = true;

			boolean useAllLogMoves = false;
			AlignmentMoveList tree1LogMoves = new AlignmentMoveList();
			AlignmentMoveList tree2LogMoves = new AlignmentMoveList();
			if (useAllLogMoves) {
				for (AlignmentMove move : alignmentMoves1.get(0).getAlignmentMoves()) {
					if (move.moveType == AlignmentMove.LOG || move.moveType == AlignmentMove.LOGP) {
						tree1LogMoves.add(move);
					}
				}

				for (AlignmentMove move : alignmentMoves2.get(0).getAlignmentMoves()) {
					if (move.moveType == AlignmentMove.LOG || move.moveType == AlignmentMove.LOGP) {
						tree2LogMoves.add(move);
					}
				}
			}

			// Insertion crossover for parent1
			int baddestNode = -1;
			double baddestScore = 0;

			if (randomSelection) {
				// Select a random baddest node in parent1
				baddestNode = registry.getRandom().nextInt(parent1.size());
			} else {
				// Determine baddest node in parent1
				for (int i = 0; i < parent1.size(); i++) {
					AlignmentMoveList moves = alignmentMoves1.get(i);
					// TODO: fix badness calculation!
					double badness = (moves.getTotalMoveCount() - moves.getTotalSyncMoveCount())
							/ (moves.getTotalMoveCount() + 1.0);
					if (badness > baddestScore) {
						baddestNode = i;
						baddestScore = badness;
					}
				}
			}

			if (baddestNode != -1) {
				// Find the best sub-tree in the other tree that is better than the baddest node
				AlignmentMoveList baddestMoves = alignmentMoves1.get(baddestNode);
				double bestScore = 0.0;
				int bestNode = -1;

				for (int j = 0; j < parent2.size(); j++) {
					// Determine the overlap between the baddest node and node j
					AlignmentMoveList overlap = findOverlap(baddestMoves, alignmentMoves2.get(j), tree1LogMoves);
					double coverage = overlap.getTotalSyncMoveCount();
					if (includeMM) {
						coverage += overlap.getTotalModelMoveCount();
					}
					double score = coverage
							/ Math.max(baddestMoves.getTotalSyncMoveCount() + baddestMoves.getTotalLogMoveCount()
									+ tree1LogMoves.getTotalMoveCount(), alignmentMoves2.get(j).getTotalMoveCount());
					//System.out.println(score);
					if (score > bestScore) {
						bestScore = score;
						bestNode = j;
					}
				}

				if (bestNode != -1) {
					// TODO: Remove
					//System.out.println(tree1 + " " + tree2);
					tree1 = tree1.replace(baddestNode, parent2, bestNode);
					//System.out.println(tree1);
				}
			}

			// Insertion crossover for parent2
			baddestNode = -1;
			baddestScore = 0;

			if (randomSelection) {
				// Select a random baddest node in parent2
				baddestNode = registry.getRandom().nextInt(parent2.size());
			} else {
				// Determine baddest node in parent2
				for (int i = 0; i < parent2.size(); i++) {
					AlignmentMoveList moves = alignmentMoves2.get(i);
					// TODO: fix badness calculation!
					double badness = (moves.getTotalMoveCount() - moves.getTotalSyncMoveCount())
							/ (moves.getTotalMoveCount() + 1.0);
					if (badness > baddestScore) {
						baddestNode = i;
						baddestScore = badness;
					}
				}
			}

			if (baddestNode != -1) {
				// Find the best sub-tree in the other tree that is better than the baddest node
				AlignmentMoveList baddestMoves = alignmentMoves2.get(baddestNode);
				double bestScore = 0.0;
				int bestNode = -1;

				for (int j = 0; j < parent1.size(); j++) {
					// Determine the overlap between the baddest node and node j
					AlignmentMoveList overlap = findOverlap(baddestMoves, alignmentMoves1.get(j), tree2LogMoves);
					double coverage = overlap.getTotalSyncMoveCount();
					if (includeMM) {
						coverage += overlap.getTotalModelMoveCount();
					}
					double score = coverage
							/ Math.max(baddestMoves.getTotalSyncMoveCount() + baddestMoves.getTotalLogMoveCount()
									+ tree2LogMoves.getTotalMoveCount(), alignmentMoves1.get(j).getTotalMoveCount());

					if (score > bestScore) {
						bestScore = score;
						bestNode = j;
					}
				}

				if (bestNode != -1) {
					tree2 = tree2.replace(baddestNode, parent1, bestNode);
				}
			}

			//Prepare the offspring list
			List<ProbProcessArrayTree> offspring = new ArrayList<ProbProcessArrayTree>(2);
			offspring.add(tree1);
			offspring.add(tree2);

			return offspring;
		}
	}

	private AlignmentMoveList findOverlap(AlignmentMoveList receivingMoveList, AlignmentMoveList donatingMoveList,
			AlignmentMoveList treeLogMovesList) {
		AlignmentMoveList matchMoves = new AlignmentMoveList();
		ArrayList<AlignmentMove> receiverMoves = receivingMoveList.getAlignmentMoves();
		ArrayList<AlignmentMove> donatorMoves = donatingMoveList.getAlignmentMoves();
		ArrayList<AlignmentMove> treeLogMoves = treeLogMovesList.getAlignmentMoves();
		HashMap<Pair, Integer> synchMoveMap = new HashMap<Pair, Integer>();
		HashMap<Pair, Integer> modelMoveMap = new HashMap<Pair, Integer>();
		HashMap<Integer, Boolean> inMatchMoves = new HashMap<Integer, Boolean>();

		// Put all synchronous and model moves from the donator list in a HashMap for quick contains checking
		for (int i = 0; i < donatorMoves.size(); i++) {
			if (donatorMoves.get(i).moveType == AlignmentMove.SYNC) {
				synchMoveMap.put(new Pair(donatorMoves.get(i).traceID, donatorMoves.get(i).traceIndex), i);
			} else if (donatorMoves.get(i).moveType == AlignmentMove.MODEL) {
				modelMoveMap.put(new Pair(donatorMoves.get(i).traceID, donatorMoves.get(i).eventClassID), i);
			}
		}

		// Check for all synchronous and log moves from the receiver list if the donator can match them
		for (int i = 0; i < receiverMoves.size(); i++) {
			if (receiverMoves.get(i).moveType != AlignmentMove.MODEL) {
				if (synchMoveMap.containsKey(new Pair(receiverMoves.get(i).traceID, receiverMoves.get(i).traceIndex))) {
					Integer donatorMoveNr = synchMoveMap.get(new Pair(receiverMoves.get(i).traceID, receiverMoves
							.get(i).traceIndex));
					if (!inMatchMoves.containsKey(donatorMoveNr)) {
						matchMoves.add(donatorMoves.get(donatorMoveNr));
						inMatchMoves.put(donatorMoveNr, true);
					}
				} else if (modelMoveMap.containsKey(new Pair(receiverMoves.get(i).traceID,
						receiverMoves.get(i).eventClassID))) {
					Integer donatorMoveNr = modelMoveMap.get(new Pair(receiverMoves.get(i).traceID, receiverMoves
							.get(i).eventClassID));
					if (!inMatchMoves.containsKey(donatorMoveNr)) {
						matchMoves.add(donatorMoves.get(donatorMoveNr));
						inMatchMoves.put(donatorMoveNr, true);
					}
				}
			}
		}

		// Check for all log moves from the tree moves list if the donator can match them
		for (int i = 0; i < treeLogMoves.size(); i++) {
			if (synchMoveMap.containsKey(new Pair(treeLogMoves.get(i).traceID, treeLogMoves.get(i).traceIndex))) {
				Integer donatorMoveNr = synchMoveMap.get(new Pair(treeLogMoves.get(i).traceID,
						treeLogMoves.get(i).traceIndex));
				if (!inMatchMoves.containsKey(donatorMoveNr)) {
					matchMoves.add(donatorMoves.get(donatorMoveNr));
					inMatchMoves.put(donatorMoveNr, true);
				}
			} else if (modelMoveMap
					.containsKey(new Pair(treeLogMoves.get(i).traceID, treeLogMoves.get(i).eventClassID))) {
				Integer donatorMoveNr = modelMoveMap.get(new Pair(treeLogMoves.get(i).traceID,
						treeLogMoves.get(i).eventClassID));
				if (!inMatchMoves.containsKey(donatorMoveNr)) {
					matchMoves.add(donatorMoves.get(donatorMoveNr));
					inMatchMoves.put(donatorMoveNr, true);
				}
			}
		}

		return matchMoves;
	}

	private class Pair {

		public final int first; // Trace ID
		public final int second; // Trace Index for Log/Sync and Event Class for Model

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

		private InsertionCrossover getOuterType() {
			return InsertionCrossover.this;
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

}
