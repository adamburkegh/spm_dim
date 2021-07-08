package org.processmining.plugins.etm.mutation.mutators.maikelvaneck;

import gnu.trove.map.TObjectIntMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Random;

import nl.tue.astar.Trace;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.CentralRegistryConfigurable;
import org.processmining.plugins.etm.factory.TreeFactoryAbstract;
import org.processmining.plugins.etm.factory.TreeFactoryCoordinator;
import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTreeImpl;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

/**
 * 
 * @author Maikel van Eck
 */
public class IntelligentTreeFactory extends TreeFactoryAbstract {
	//see 3.4.2 thesis

	public IntelligentTreeFactory(CentralRegistry registry) {
		super(registry);
	}

	public ProbProcessArrayTree generateRandomCandidate(Random rng) {
		return generateRandomCandidate(registry);
	}

	/**
	 * The Advanced trace model creation
	 * 
	 * @param registry
	 * @return
	 */
	public static ProbProcessArrayTree generateRandomCandidate(CentralRegistry registry) {
		// Get the converted log
		TObjectIntMap<Trace> converted = registry.getaStarAlgorithm().getConvertedLog();

		// Randomly select a trace from the log
		int traceNr = registry.getRandom().nextInt(registry.getLog().size());
		int currentTrace = 0;
		while (traceNr - converted.get(converted.keys()[currentTrace]) >= 0 && currentTrace < converted.keys().length) {
			traceNr -= converted.get(converted.keys()[currentTrace]);
			currentTrace += 1;
		}

		Trace trace = (Trace) converted.keys()[currentTrace];
		ProbProcessArrayTree tree = null;

		//System.out.println(trace);

		boolean buildIntelligentTree = false;
		if (trace.getSize() > 0) {
			if (registry.getRandom().nextDouble() < 0.75) {
				// Create a loop-tree
				tree = createLoopTree(registry, trace);
				buildIntelligentTree = true;
			} else {
				// Create a SEQ-tree
				tree = createSEQTree(registry, trace);
			}
		} else {
			tree = TreeFactoryCoordinator.generateRandomCandidate(registry);
		}

		//System.out.println(buildIntelligentTree + " " + tree);

		// Build an intelligent tree by merging traces
		// TODO: CHANGE
		int minNrExtraTraces = 3;
		int extraTraces = 0;
		while (buildIntelligentTree) {
			// Randomly select a trace from the log
			traceNr = registry.getRandom().nextInt(registry.getLog().size());
			currentTrace = 0;
			while (traceNr - converted.get(converted.keys()[currentTrace]) >= 0
					&& currentTrace < converted.keys().length) {
				traceNr -= converted.get(converted.keys()[currentTrace]);
				currentTrace += 1;
			}

			trace = (Trace) converted.keys()[currentTrace];
			ProbProcessArrayTree extraTree = createLoopTree(registry, trace);

			// Merge the extraTree into the existing tree
			if (trace.getSize() > 0) {
				tree = mergeTrees(registry, tree, extraTree);
			}

			//System.out.println(tree);

			// Check for duplicates afterwards and stop if we find them
			HashMap<Integer, Integer> eventCounter = new HashMap<Integer, Integer>();
			for (int i = 0; i < tree.size(); i++) {
				if (tree.isLeaf(i) && tree.getType(i) != ProbProcessArrayTree.TAU) {
					if (eventCounter.containsKey((int) tree.getType(i))) {
						// TODO: REMOVE
						//System.out.println("DUPLICATES: " + tree);
						buildIntelligentTree = false;
						break;
					} else {
						eventCounter.put((int) tree.getType(i), 1);
					}
				}
			}

			//System.out.println(extraTraces+1);
			extraTraces++;
			if (extraTraces >= minNrExtraTraces) {
				if (registry.getRandom().nextDouble() < 0.5) {
					//stop
					buildIntelligentTree = false;
				}
			}
		}

		//System.out.println(tree);

		//Now make sure the tree has enough configurations, if required
		if (registry instanceof CentralRegistryConfigurable) {
			CentralRegistryConfigurable cr = (CentralRegistryConfigurable) registry;
			while (tree.getNumberOfConfigurations() < cr.getNrLogs()) {
				tree.addConfiguration(new Configuration(new boolean[tree.size()], new boolean[tree.size()]));
			}
		}

		assert tree.isConsistent();

		return tree;
	}

	/**
	 * Creates a tree where activities that occur more than once are placed in
	 * the do or redo parts of a loop
	 * 
	 * @param registry
	 * @param trace
	 * @return
	 */
	private static ProbProcessArrayTree createLoopTree(CentralRegistry registry, Trace trace) {
		HashMap<Integer, Integer> eventCounter = new HashMap<Integer, Integer>();
		for (int i = 0; i < trace.getSize(); i++) {
			// Count how often each event class occurs in the trace
			if (eventCounter.containsKey(trace.get(i))) {
				eventCounter.put(trace.get(i), eventCounter.get(trace.get(i)) + 1);
			} else {
				eventCounter.put(trace.get(i), 1);
			}
		}

		//System.out.println(eventCounter);

		ArrayList<Integer> seqList = new ArrayList<Integer>();
		ArrayList<Integer> loopDoList = new ArrayList<Integer>();
		ArrayList<Integer> loopRedoList = new ArrayList<Integer>();
		int firstLoopEvent = -1;
		for (int i = 0; i < trace.getSize(); i++) {
			if (eventCounter.get(trace.get(i)) == 1) {
				seqList.add(trace.get(i));
			} else if (eventCounter.get(trace.get(i)) != -1) {
				if (registry.getRandom().nextDouble() < 0.5) {
					loopDoList.add(trace.get(i));
				} else {
					loopRedoList.add(trace.get(i));
				}
				eventCounter.put(trace.get(i), -1);
				if (firstLoopEvent == -1) {
					firstLoopEvent = i;
					seqList.add(-1);
				}
			}
		}

		//System.out.println(seqList);
		//System.out.println(loopList);
		//System.out.println(loopDoList);
		//System.out.println(loopRedoList);
		//System.out.println(firstLoopEvent);

		// Build the loopTree
		ProbProcessArrayTree loopTree = TreeUtils.fromString("LOOP( LEAF: tau , LEAF: tau , LEAF: tau )",
				registry.getEventClasses());
		if (loopDoList.size() == 1) {
			loopTree.setType(1, loopDoList.get(0).shortValue());
		} else if (loopDoList.size() > 1) {
			loopTree.setType(1, ProbProcessArrayTree.SEQ);
			for (int i = 0; i < loopDoList.size(); i++) {
				loopTree = loopTree.addChild(1, loopTree.nChildren(1), loopDoList.get(i).shortValue(),
						Configuration.NOTCONFIGURED);
			}
		}
		if (loopRedoList.size() == 1) {
			loopTree.setType(loopTree.getChildAtIndex(0, 1), loopRedoList.get(0).shortValue());
		} else if (loopRedoList.size() > 1) {
			loopTree.setType(loopTree.getChildAtIndex(0, 1), ProbProcessArrayTree.SEQ);
			for (int i = 0; i < loopRedoList.size(); i++) {
				loopTree = loopTree.addChild(loopTree.getChildAtIndex(0, 1),
						loopTree.nChildren(loopTree.getChildAtIndex(0, 1)), loopRedoList.get(i).shortValue(),
						Configuration.NOTCONFIGURED);
			}
		}

		//System.out.println(loopTree);

		ProbProcessArrayTree tree = TreeUtils.fromString(" LEAF: tau ", registry.getEventClasses());
		tree.setType(0, ProbProcessArrayTree.SEQ);
		for (int i = 0; i < seqList.size(); i++) {
			if (seqList.get(i) == -1) {
				tree = tree.add(loopTree, 0, 0, tree.nChildren(0));
			} else {
				tree = tree.addChild(0, tree.nChildren(0), seqList.get(i).shortValue(), Configuration.NOTCONFIGURED);
			}
		}

		return tree;
	}

	private static ProbProcessArrayTree createSEQTree(CentralRegistry registry, Trace trace) {
		int[] next = new int[trace.getSize() + 1];
		short[] type = new short[trace.getSize() + 1];
		int[] parent = new int[trace.getSize() + 1];
		double[] weight = new double[trace.getSize() + 1];

		next[0] = trace.getSize() + 1;
		type[0] = ProbProcessArrayTree.SEQ;
		parent[0] = ProbProcessArrayTree.NONE;
		weight[0] = 1.0;

		for (int i = 0; i < trace.getSize(); i++) {
			next[i + 1] = i + 2;
			type[i + 1] = (short) trace.get(i);
			parent[i + 1] = 0;
		}

		ProbProcessArrayTree tree = new ProbProcessArrayTreeImpl(next, type, parent, weight);
		return tree;
	}

	/**
	 * Merges two provided trees, assuming each activity occurs at most once per
	 * tree (for mapping)
	 * 
	 * @param registry
	 * @param tree
	 * @param extraTree
	 * @return
	 */
	private static ProbProcessArrayTree mergeTrees(CentralRegistry registry, ProbProcessArrayTree tree, ProbProcessArrayTree extraTree) {
		//See Fig. 3.5 in thesis: each merge step is the exec. of this function

		// Initialize the merge mapping
		HashMap<Integer, Integer> Tree1ToTree2Map = new HashMap<Integer, Integer>();
		HashMap<Integer, Integer> Tree2ToTree1Map = new HashMap<Integer, Integer>();
		for (int i = 0; i < tree.size(); i++) {
			Tree1ToTree2Map.put(i, -1);
		}
		for (int i = 0; i < extraTree.size(); i++) {
			Tree2ToTree1Map.put(i, -1);
		}

		// Create the merge mapping
		int[] nrChildrenExplored = new int[tree.size()];
		// Number of children being unmapped nodes or containing them somewhere in their subtrees
		int[] nrTree1ChildrenUnmapped = new int[tree.size()];
		int[] nrTree2ChildrenUnmapped = new int[extraTree.size()];
		// Number of children being mapped nodes
		int[] nrTree1ChildrenMapped = new int[tree.size()];
		int[] nrTree2ChildrenMapped = new int[extraTree.size()];
		Deque<Integer> nodesToMap = new ArrayDeque<Integer>();
		nodesToMap.addFirst(0);

		//Map activities/leafs between the two trees
		while (!nodesToMap.isEmpty()) {
			int node = nodesToMap.removeFirst();
			if (tree.getType(node) == ProbProcessArrayTree.TAU) {
				Tree1ToTree2Map.put(node, -2);
				if (tree.getParent(node) != ProbProcessArrayTree.NONE) {
					nrChildrenExplored[tree.getParent(node)] += 1;
				}
			} else if (tree.isLeaf(node)) {
				// Try to map the leaf node
				int mappedTo = -1;
				for (int i = 0; i < extraTree.size(); i++) {
					if (tree.getType(node) == extraTree.getType(i)) {
						// Found a mapping
						Tree1ToTree2Map.put(node, i);
						Tree2ToTree1Map.put(i, node);
						nrTree1ChildrenMapped[node] = 1;
						nrTree2ChildrenMapped[i] = 1;
						mappedTo = i;
						break;
					}
				}

				if (tree.getParent(node) != ProbProcessArrayTree.NONE) {
					nrChildrenExplored[tree.getParent(node)] += 1;
					if (mappedTo == -1) {
						// Found no mapping
						nrTree1ChildrenUnmapped[tree.getParent(node)] += 1;
					}
				}
			} else {
				if (nrChildrenExplored[node] < tree.nChildren(node)) {
					nodesToMap.addFirst(node);
					for (int i = 0; i < tree.nChildren(node); i++) {
						nodesToMap.addFirst(tree.getChildAtIndex(node, i));
					}
				} else {
					// Try to map the operator node
					int mappedTo = -1;
					for (int i = 0; i < tree.nChildren(node); i++) {
						// Check if at least one non-tau child has been mapped to a child of another node
						int childMappedTo = Tree1ToTree2Map.get(tree.getChildAtIndex(node, i));
						if (childMappedTo >= 0 || childMappedTo == -3) {
							// Found a mapping
							if (tree.getType(node) == ProbProcessArrayTree.LOOP
									&& extraTree.getType(extraTree.getParent(childMappedTo)) == ProbProcessArrayTree.LOOP) {
								Tree1ToTree2Map.put(node, extraTree.getParent(childMappedTo));
								Tree2ToTree1Map.put(extraTree.getParent(childMappedTo), node);
								mappedTo = extraTree.getParent(childMappedTo);
							} else {
								Tree1ToTree2Map.put(node, -3);
								mappedTo = -3;
							}
							if (tree.getType(node) == ProbProcessArrayTree.XOR || tree.getType(node) == ProbProcessArrayTree.LOOP) {
								// Fix unmapped children, because we only need 1 child to be mapped for these nodes
								for (int j = 0; j < tree.nChildren(node); j++) {
									int child = tree.getChildAtIndex(node, j);
									if (Tree1ToTree2Map.get(child) == -1) {
										Tree1ToTree2Map.put(child, -3);
										nrTree1ChildrenUnmapped[child] = 0;
										nrTree1ChildrenUnmapped[node] -= 1;
									}
								}
							}
							// Determine total number of mapped children
							for (int j = 0; j < tree.nChildren(node); j++) {
								nrTree1ChildrenMapped[node] += nrTree1ChildrenMapped[tree.getChildAtIndex(node, j)];
							}
							break;
						} else if (childMappedTo == -2 && tree.getType(node) == ProbProcessArrayTree.XOR) {
							// Found a tau child that allows this node to be skipped
							Tree1ToTree2Map.put(node, -2);
							mappedTo = -2;
						}
					}

					if (tree.getParent(node) != ProbProcessArrayTree.NONE) {
						nrChildrenExplored[tree.getParent(node)] += 1;
						if (mappedTo == -2) {
							nrTree1ChildrenUnmapped[node] = 0;
						} else if (mappedTo == -1 || nrTree1ChildrenUnmapped[node] != 0) {
							// Found no mapping or has children that are unmapped
							nrTree1ChildrenUnmapped[tree.getParent(node)] += 1;
						}
					}
				}
			}
		}//while merge

		// Fix nrTree2ChildrenUnmapped and tau-nodes
		int extraTreeUnmapped = 0;
		nrChildrenExplored = new int[extraTree.size()];
		nodesToMap.addFirst(0);

		while (!nodesToMap.isEmpty()) {
			int node = nodesToMap.removeFirst();
			if (extraTree.getType(node) == ProbProcessArrayTree.TAU) {
				Tree2ToTree1Map.put(node, -2);
				if (extraTree.getParent(node) != ProbProcessArrayTree.NONE) {
					nrChildrenExplored[extraTree.getParent(node)] += 1;
				}
			} else if (extraTree.isLeaf(node)) {
				if (extraTree.getParent(node) != ProbProcessArrayTree.NONE) {
					nrChildrenExplored[extraTree.getParent(node)] += 1;
					if (Tree2ToTree1Map.get(node) == -1) {
						// Found no mapping
						extraTreeUnmapped += 1;
						nrTree2ChildrenUnmapped[extraTree.getParent(node)] += 1;
					}
				}
			} else {
				if (nrChildrenExplored[node] < extraTree.nChildren(node)) {
					nodesToMap.addFirst(node);
					for (int i = 0; i < extraTree.nChildren(node); i++) {
						nodesToMap.addFirst(extraTree.getChildAtIndex(node, i));
					}
				} else {
					// Check if at least one non-tau child has been mapped to a child of another node
					for (int i = 0; i < extraTree.nChildren(0); i++) {
						int childMappedTo = Tree2ToTree1Map.get(extraTree.getChildAtIndex(node, i));
						if (childMappedTo >= 0 || childMappedTo == -3) {
							// Found a mapping
							Tree2ToTree1Map.put(node, -3);
							if (extraTree.getType(node) == ProbProcessArrayTree.LOOP) {
								// Fix unmapped children, because we only need 1 child to be mapped for these nodes
								for (int j = 0; j < extraTree.nChildren(node); j++) {
									int child = extraTree.getChildAtIndex(node, j);
									if (Tree2ToTree1Map.get(child) == -1) {
										Tree2ToTree1Map.put(child, -3);
										nrTree2ChildrenUnmapped[child] = 0;
										nrTree2ChildrenUnmapped[node] -= 1;
										if (extraTree.isLeaf(child)) {
											extraTreeUnmapped -= 1;
										}
									}
									if (!extraTree.isLeaf(child)) {
										for (int k = 0; k < extraTree.nChildren(child); k++) {
											int grandChild = extraTree.getChildAtIndex(child, k);
											if (Tree2ToTree1Map.get(grandChild) == -1) {
												Tree2ToTree1Map.put(grandChild, -3);
												nrTree2ChildrenUnmapped[grandChild] = 0;
												nrTree2ChildrenUnmapped[child] -= 1;
												extraTreeUnmapped -= 1;
											}
										}
									}
								}
							}
							// Determine total number of mapped children
							for (int j = 0; j < extraTree.nChildren(node); j++) {
								nrTree2ChildrenMapped[node] += nrTree2ChildrenMapped[extraTree.getChildAtIndex(node, j)];
							}
							break;
						}
					}

					if (extraTree.getParent(node) != ProbProcessArrayTree.NONE) {
						nrChildrenExplored[extraTree.getParent(node)] += 1;
						if (Tree2ToTree1Map.get(node) == -1 || nrTree2ChildrenUnmapped[node] != 0) {
							// Found no mapping or has children that are unmapped
							nrTree2ChildrenUnmapped[extraTree.getParent(node)] += 1;
						}
					}
				}
			}
		}

		// TODO: REMOVE
		/*
		 * System.out.println("BEGIN"); System.out.println(tree);
		 * System.out.println(extraTree); System.out.println(Tree1ToTree2Map);
		 * System.out.println(Tree2ToTree1Map);
		 * System.out.println(Arrays.toString(nrTree1ChildrenUnmapped));
		 * System.out.println(Arrays.toString(nrTree1ChildrenMapped));
		 * System.out.println(Arrays.toString(nrTree2ChildrenMapped));
		 */

		if (nrTree1ChildrenUnmapped[0] == 0 && extraTreeUnmapped == 0) {
			// extraTree contains nothing new
		} else {
			// Merge the trees

			// Look for unmapped children in extraTree
			ArrayList<Integer> unmappedTree2Children = new ArrayList<Integer>();
			for (int i = 0; i < extraTree.nChildren(0); i++) {
				int child = extraTree.getChildAtIndex(0, i);
				if (Tree2ToTree1Map.get(child) == -1) {
					unmappedTree2Children.add(child);
				}
			}

			boolean notMerged = true;
			boolean[] alreadyCovered = new boolean[extraTree.nChildren(0)];
			int node = 0;
			while (notMerged) {
				boolean foundUnmapped = false;
				ArrayList<Integer> unmappedTree1Children = new ArrayList<Integer>();

				// Look through all children of the node in tree for subtrees containing unmapped nodes
				for (int i = 0; i < tree.nChildren(node); i++) {
					int child = tree.getChildAtIndex(node, i);
					if (Tree1ToTree2Map.get(child) == -1) {
						foundUnmapped = true;
						unmappedTree1Children.add(child);
					} else if (nrTree1ChildrenUnmapped[child] > 0) {
						unmappedTree1Children.add(child);
					}
				}

				// Check if we need to merge both trees at this point
				if (foundUnmapped || unmappedTree1Children.size() > 1 || nrTree1ChildrenUnmapped[0] == 0) {
					// Merge both trees
					notMerged = false;

					if (tree.getType(node) == ProbProcessArrayTree.SEQ) {
						ProbProcessArrayTree mergedSubTree = TreeUtils.fromString("SEQ( XOR( LEAF: tau , LEAF: tau ) )",
								registry.getEventClasses());
						int XORIndex = 0;

						int tree1BeginIndex = 0;
						int tree2BeginIndex = 0;
						for (int i = 0; i < extraTree.nChildren(0); i++) {
							if (alreadyCovered[i]) {
								tree2BeginIndex = i + 1;
							} else {
								break;
							}
						}
						boolean noUnmappedBefore = true;
						while (noUnmappedBefore) {
							int tree1Child = tree.getChildAtIndex(node, tree1BeginIndex);
							int tree2Child = extraTree.getChildAtIndex(0, tree2BeginIndex);
							if (Tree1ToTree2Map.get(tree1Child) == -2 && tree1BeginIndex < tree.nChildren(node)) {
								mergedSubTree = mergedSubTree.add(tree, tree1Child, 0, mergedSubTree.nChildren(0) - 1);
								tree1BeginIndex += 1;
								XORIndex += 1;
							} else {
								if (!unmappedTree1Children.contains(tree1Child)
										&& !unmappedTree2Children.contains(tree2Child)
										&& tree1BeginIndex < tree.nChildren(node)) {
									nrTree1ChildrenMapped[tree1Child] -= 1;
									if (nrTree1ChildrenMapped[tree1Child] == 0) {
										mergedSubTree = mergedSubTree.add(tree, tree1Child, 0,
												mergedSubTree.nChildren(0) - 1);
										tree1BeginIndex += 1;
										XORIndex += 1;
									}
									nrTree2ChildrenMapped[tree2Child] -= 1;
									if (nrTree2ChildrenMapped[tree2Child] == 0) {
										tree2BeginIndex += 1;
									}
								} else {
									noUnmappedBefore = false;
								}
							}
						}

						//System.out.println(mergedSubTree);

						int tree1EndIndex = tree.nChildren(node) - 1;
						int tree2EndIndex = extraTree.nChildren(0) - 1;
						for (int i = extraTree.nChildren(0) - 1; i >= 0; i--) {
							if (alreadyCovered[i]) {
								tree2EndIndex = i - 1;
							} else {
								break;
							}
						}
						boolean noUnmappedAfter = true;
						while (noUnmappedAfter) {
							int tree1Child = tree.getChildAtIndex(node, tree1EndIndex);
							int tree2Child = extraTree.getChildAtIndex(0, tree2EndIndex);
							if (Tree1ToTree2Map.get(tree1Child) == -2 && tree1EndIndex >= 0) {
								mergedSubTree = mergedSubTree.add(tree, tree1Child, 0, mergedSubTree.nChildren(0)
										- (tree.nChildren(node) - 1 - tree1EndIndex));
								tree1EndIndex -= 1;
							} else {
								if (!unmappedTree1Children.contains(tree1Child)
										&& !unmappedTree2Children.contains(tree2Child) && tree1EndIndex >= 0) {
									nrTree1ChildrenMapped[tree1Child] -= 1;
									if (nrTree1ChildrenMapped[tree1Child] == 0) {
										mergedSubTree = mergedSubTree
												.add(tree, tree1Child, 0,
														mergedSubTree.nChildren(0)
																- (tree.nChildren(node) - 1 - tree1EndIndex));
										tree1EndIndex -= 1;
									}
									nrTree2ChildrenMapped[tree2Child] -= 1;
									if (nrTree2ChildrenMapped[tree2Child] == 0) {
										tree2EndIndex -= 1;
									}
								} else {
									noUnmappedAfter = false;
								}
							}
						}

						//System.out.println(mergedSubTree);

						ProbProcessArrayTree tree1Seq = TreeUtils.fromString("LEAF: tau", registry.getEventClasses());
						if (tree1BeginIndex <= tree1EndIndex) {
							tree1Seq = TreeUtils.fromString("SEQ( LEAF: tau )", registry.getEventClasses());
							tree1Seq = tree1Seq.replace(1, tree, tree.getChildAtIndex(node, tree1BeginIndex));
							tree1BeginIndex += 1;

							for (int i = tree1BeginIndex; i <= tree1EndIndex; i++) {
								int tree1Child = tree.getChildAtIndex(node, i);
								tree1Seq = tree1Seq.add(tree, tree1Child, 0, tree1Seq.nChildren(0));
							}

							if (tree1Seq.nChildren(0) == 1) {
								tree1Seq = tree1Seq.replace(0, tree1Seq, 1);
							}
						}

						//System.out.println(tree1Seq);

						ProbProcessArrayTree tree2Seq = TreeUtils.fromString("LEAF: tau", registry.getEventClasses());
						if (tree2BeginIndex <= tree2EndIndex) {
							tree2Seq = TreeUtils.fromString("SEQ( LEAF: tau )", registry.getEventClasses());
							tree2Seq = tree2Seq.replace(1, extraTree, extraTree.getChildAtIndex(0, tree2BeginIndex));
							tree2BeginIndex += 1;

							for (int i = tree2BeginIndex; i <= tree2EndIndex; i++) {
								int tree2Child = extraTree.getChildAtIndex(0, i);
								tree2Seq = tree2Seq.add(extraTree, tree2Child, 0, tree2Seq.nChildren(0));
							}

							if (tree2Seq.nChildren(0) == 1) {
								tree2Seq = tree2Seq.replace(0, tree2Seq, 1);
							}
						}

						//System.out.println(tree2Seq);

						mergedSubTree = mergedSubTree.replace(
								mergedSubTree.getChildAtIndex(mergedSubTree.getChildAtIndex(0, XORIndex), 0), tree1Seq,
								0);
						mergedSubTree = mergedSubTree.replace(
								mergedSubTree.getChildAtIndex(mergedSubTree.getChildAtIndex(0, XORIndex), 1), tree2Seq,
								0);

						//System.out.println(mergedSubTree);

						tree = tree.replace(node, mergedSubTree, 0);
						tree = TreeUtils.normalize(tree);
					} else {
						// TODO: REMOVE
						/*
						 * System.out.println("Error " + node + " " + tree);
						 * System.out.println(extraTree);
						 * System.out.println(Tree1ToTree2Map);
						 * System.out.println(Tree2ToTree1Map);
						 */
					}
				} else {
					//We need to go further down tree1 
					//System.out.println("!!!!!!!!!!!");

					int newNode = unmappedTree1Children.get(0);
					int[] oldNrTree1ChildrenMapped = nrTree1ChildrenMapped.clone();
					int[] oldNrTree2ChildrenMapped = nrTree2ChildrenMapped.clone();

					if (tree.getType(node) == ProbProcessArrayTree.SEQ) {
						// Mark everything already covered until this point
						int tree1BeginIndex = 0;
						int tree2BeginIndex = 0;
						for (int i = 0; i < extraTree.nChildren(0); i++) {
							if (alreadyCovered[i]) {
								tree2BeginIndex = i + 1;
							} else {
								break;
							}
						}
						boolean noUnmappedBefore = true;
						while (noUnmappedBefore && tree.getChildAtIndex(node, tree1BeginIndex) != newNode) {
							int tree1Child = tree.getChildAtIndex(node, tree1BeginIndex);
							int tree2Child = extraTree.getChildAtIndex(0, tree2BeginIndex);
							if (Tree1ToTree2Map.get(tree1Child) == -2 && tree1BeginIndex < tree.nChildren(node)) {
								tree1BeginIndex += 1;
							} else {
								if (!unmappedTree1Children.contains(tree1Child)
										&& !unmappedTree2Children.contains(tree2Child)
										&& tree1BeginIndex < tree.nChildren(node)) {
									nrTree1ChildrenMapped[tree1Child] -= 1;
									if (nrTree1ChildrenMapped[tree1Child] == 0) {
										tree1BeginIndex += 1;
									}
									nrTree2ChildrenMapped[tree2Child] -= 1;
									if (nrTree2ChildrenMapped[tree2Child] == 0) {
										alreadyCovered[tree2BeginIndex] = true;
										tree2BeginIndex += 1;
									}
								} else {
									noUnmappedBefore = false;
								}
							}
						}

						int tree1EndIndex = tree.nChildren(node) - 1;
						int tree2EndIndex = extraTree.nChildren(0) - 1;
						for (int i = extraTree.nChildren(0) - 1; i >= 0; i--) {
							if (alreadyCovered[i]) {
								tree2EndIndex = i - 1;
							} else {
								break;
							}
						}
						boolean noUnmappedAfter = true;
						while (noUnmappedAfter && tree.getChildAtIndex(node, tree1EndIndex) != newNode) {
							int tree1Child = tree.getChildAtIndex(node, tree1EndIndex);
							int tree2Child = extraTree.getChildAtIndex(0, tree2EndIndex);
							if (Tree1ToTree2Map.get(tree1Child) == -2 && tree1EndIndex >= 0) {
								tree1EndIndex -= 1;
							} else {
								if (!unmappedTree1Children.contains(tree1Child)
										&& !unmappedTree2Children.contains(tree2Child) && tree1EndIndex >= 0) {
									nrTree1ChildrenMapped[tree1Child] -= 1;
									if (nrTree1ChildrenMapped[tree1Child] == 0) {
										tree1EndIndex -= 1;
									}
									nrTree2ChildrenMapped[tree2Child] -= 1;
									if (nrTree2ChildrenMapped[tree2Child] == 0) {
										alreadyCovered[tree2EndIndex] = true;
										tree2EndIndex -= 1;
									}
								} else {
									noUnmappedAfter = false;
								}
							}
						}
					}

					//System.out.println(Arrays.toString(alreadyCovered));

					node = newNode;
					nrTree1ChildrenMapped = oldNrTree1ChildrenMapped;
					nrTree2ChildrenMapped = oldNrTree2ChildrenMapped;
				}
			}
		}

		return tree;
	}

}
