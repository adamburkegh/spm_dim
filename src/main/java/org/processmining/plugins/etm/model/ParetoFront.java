package org.processmining.plugins.etm.model;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.fitness.TreeComparatorByFitness;
import org.processmining.plugins.etm.fitness.TreeFitnessInfo;
import org.processmining.plugins.etm.fitness.metrics.ParetoFitnessEvaluator;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

/**
 * This class contains the Pareto front of Process Trees including support
 * functions to add, remove and get trees from the front
 * 
 * @author jbuijs
 * 
 */
//FIXME check all class contents
//FIXME Test Class thoroughly
public class ParetoFront {

	//FIXME fix concurrent modification exceptions: synchronize on this

	/*- 
	 * TODO: make more generic (to also suite Dennis):
	 * - independant of object type
	 * - store dimension values in front (not in registry)
	 * - create interfaces for interaction and listeners 
	 */

	/*
	 * TODO: implement 'thinning' of front method by removing trees in between
	 * interesting ones. Or, better, implement a function that clusters the
	 * front on all dimensions
	 * (http://en.wikipedia.org/wiki/Clustering_high-dimensional_data
	 * #Subspace_clustering) and then return X medoids (but maintain the full
	 * front!!!) --> Do this in a visualization!!!
	 */

	//TODO implement 'force add' feature (for seed) that forcefully adds a candidate to the front and locks it. Might be handy to have the seed models as reference. (seperate set?)

	//TODO implement 'soft filter' that does not remove an item from the front but just does not return it (special getFront method?)

	private final CentralRegistry registry;

	/**
	 * An array of TreeFitnessInfo objects with the information for each
	 * dimension (single list is maintained because of the small size) e.g. [Fr,
	 * Pe, Ge, Sd] would indicate that Fr is the dimension with index 0
	 */
	private TreeFitnessInfo[] dimensions;

	/*
	 * TODO also make 'filters' that do not remove the elements from the front
	 * but pretend they're not there. Especially useful for visualizations
	 */

	//double arrays with upper/lower limits for that dimension
	private Double[] upperLimits;
	private Double[] lowerLimits;

	/**
	 * An array of dimensions with per dimension a sorted array list of tree
	 * indices with ascending value in that dimension e.g. [{1,2,3},{3,2,1}]
	 */
	//Initialize in constructor since array size depends on #dimensions
	private TIntArrayList[] dimensionToTree;

	/**
	 * A map of tree to the tree index
	 */
	//Initial size and load factor obtained from Trove constants
	private TObjectIntMap<ProbProcessArrayTree> treeToIndex = new TObjectIntHashMap<ProbProcessArrayTree>(10, .5f, -1);
	/**
	 * A map of tree index to the tree object
	 */
	//Since the indexes of trees are a closed range from 0 to n-1 (n=nr trees) we don't need an expensive map...
	private ArrayList<ProbProcessArrayTree> indexToTree = new ArrayList<ProbProcessArrayTree>();

	/**
	 * A map of the tree index to a list of positions in the dimension arrays
	 * e.g. [1->{0,2},2->{1,1},3->{2,0}]
	 */
	/*
	 * Since updating this list while we're changing the Pareto front takes soo
	 * much time, its best to just go through the dimension list to get the
	 * position of a certain tree when required (mainly from the GUI so
	 * performance is no issue there)
	 */
	//Since the indexes of trees are a closed range from 0 to n-1 (n=nr trees) we don't need an expensive map...
	//private ArrayList<int[]> indexToDimensionPosition = new ArrayList<int[]>();

	/**
	 * Set of Trees that are locked from removal
	 */
	private HashSet<ProbProcessArrayTree> lockedTrees = new HashSet<ProbProcessArrayTree>();

	/**
	 * Set of trees that contain locked trees that should be removed as soon as
	 * the lock is released
	 */
	private HashSet<ProbProcessArrayTree> lockedTreesThatShouldBeRemoved = new HashSet<ProbProcessArrayTree>();

	/**
	 * Maximum size of the Pareto Front
	 */
	private int maxSize;

	/**
	 * Constructor of the Pareto front with a link to the central registry and
	 * the dimensions to consider
	 * 
	 * @param registry
	 *            CentralRegistry instance
	 * @param dimensions
	 *            List of dimensions to consider
	 */
	public ParetoFront(CentralRegistry registry, List<TreeFitnessInfo> dimensions) {
		this(registry, dimensions.toArray(new TreeFitnessInfo[dimensions.size()]));
	}

	/**
	 * Constructor of the Pareto front with a link to the central registry and
	 * the dimensions to consider
	 * 
	 * @param registry
	 *            CentralRegistry instance
	 * @param maxSize
	 *            Maximum size of the Pareto Front
	 * @param dimensions
	 *            List of dimensions to consider
	 */
	public ParetoFront(CentralRegistry registry, int maxSize, List<TreeFitnessInfo> dimensions) {
		this(registry, maxSize, dimensions.toArray(new TreeFitnessInfo[dimensions.size()]));
	}

	/**
	 * Constructor of the Pareto front with a link to the central registry and
	 * the dimensions to consider
	 * 
	 * @param registry
	 *            CentralRegistry instance
	 * @param dimensions
	 *            Array of dimensions to consider
	 */
	public ParetoFront(CentralRegistry registry, TreeFitnessInfo[] dimensions) {
		this(registry, -1, dimensions);
	}

	/**
	 * Constructor of the Pareto front with a link to the central registry and
	 * the dimensions to consider
	 * 
	 * @param registry
	 *            CentralRegistry instance
	 * @param maxSize
	 *            Maximum size of the Pareto Front
	 * @param dimensions
	 *            Array of dimensions to consider
	 */
	public ParetoFront(CentralRegistry registry, int maxSize, TreeFitnessInfo[] dimensions) {
		this.dimensions = dimensions;
		this.registry = registry;
		this.maxSize = maxSize;

		//Now we know the dimensions, we can initialize the dimension to tree array of TIntArrayList
		dimensionToTree = new TIntArrayList[this.dimensions.length];
		for (int i = 0; i < dimensionToTree.length; i++) {
			dimensionToTree[i] = new TIntArrayList(10, -1);
		}

		upperLimits = new Double[dimensions.length];
		Arrays.fill(upperLimits, Double.POSITIVE_INFINITY);
		lowerLimits = new Double[dimensions.length];
		Arrays.fill(lowerLimits, Double.NEGATIVE_INFINITY);
	}

	/**
	 * Considers adding the provided collection of trees to the Pareto front. In
	 * essence calls consider(Tree) for each tree in the collection.
	 * Additionally reduces the size of the Pareto front after all candidates
	 * are considered.
	 * 
	 * @param trees
	 *            Collection of trees
	 */
	public synchronized void consider(Collection<ProbProcessArrayTree> trees) {
		for (ProbProcessArrayTree t : trees) {
			try {
				consider(t);
			} catch (ConcurrentModificationException e) {
				System.err.println("concurrent modification but continuiing...");
			}
		}

		reduceSize();
	}

	/**
	 * Re-Evaluates all trees in the front and updates their overall fitness
	 * value using the {@link ParetoFitnessEvaluator} relative distance.
	 */
	public synchronized void reEvaluateParetoFitness() {
		for (ProbProcessArrayTree tree : getFront()) {
			double fitness = 1 - ParetoFitnessEvaluator.getRelativeDistance(this, tree);
			registry.getFitness(tree).setOverallFitness(ParetoFitnessEvaluator.info, fitness);
		}
	}

	/**
	 * Consider adding the provided tree to the Pareto front. NOTE: does not
	 * reduce the size of the Pareto front, reduceSize() needs to be called
	 * manually!
	 * 
	 * @param t
	 *            Tree to consider adding
	 * @return TRUE if tree has been added to the Pareto front
	 */
	public synchronized boolean consider(ProbProcessArrayTree t) {
		//First check the limits
		for (int d = 0; d < dimensions.length; d++) {
			double dimValue = registry.getFitness(t).fitnessValues.get(dimensions[d]);

			if ((dimValue < lowerLimits[d]) || (dimValue > upperLimits[d])) {
				return false;
			}
		}

		//TODO implement normalization that does NOT change Qdim values... (possible?)
		//Tree normalizedTree = new Tree(t);
		//normalizedTree.normalize(false);
		//NAryTree normalizedTree = TreeUtils.normalize(t);
		ProbProcessArrayTree normalizedTree = t;

		//For all trees currently in the Pareto front
		for (ProbProcessArrayTree c : treeToIndex.keySet()) {
			//See if we already have this tree or if it dominates the given tree
			if (c.equals(normalizedTree) || dominates(c, normalizedTree))
				//If so, stop
				return false;
		}

		//We found no tree that dominates this one so add it to the Pareto front
		addTree(normalizedTree);
		return true;
	}

	/**
	 * Add the provided tree to the Pareto front and update all internal
	 * lists... Should only be called when the tree does not violate the Pareto
	 * requirements!
	 * 
	 * @param t
	 *            Tree to add
	 */
	private synchronized void addTree(ProbProcessArrayTree t) {
		//Add the tree to the lists
		int newIndex = treeToIndex.size();

		if (treeToIndex.containsKey(t)) {
			//TODO remove debug code
			System.out.println("Already contained...");
			return;
		}

		treeToIndex.put(t, newIndex);
		indexToTree.add(t);

		//Update the lists of all dimensions
		for (int i = 0; i < dimensions.length; i++) {
			updateDimensionForTree(i, newIndex);
		}

		assert checkConsistency();

		//And remove all dominated by this new tree...
		removeAllDominatedBy(t);

		assert checkConsistency();
	}

	/**
	 * Inserts the given tree in the appropriate dimension list at the
	 * appropriate location and updates other book-keeping if necessary.
	 * 
	 * @param dim
	 *            int index of the dimension
	 * @param t
	 *            Tree tree to insert
	 */
	private synchronized void updateDimensionForTree(int dim, int t) {
		TIntArrayList dimensionList = dimensionToTree[dim];

		//Do a binary search to find the position to insert the given tree
		int i = findNewIndexInDimensionList(dimensionList, t, dim);

		//Now insert the tree in the dimension list
		dimensionList.insert(i, t);
	}

	/**
	 * Finds the index at which the given tree should be inserted in the
	 * arraylist such that it is sorted according to the given dimension
	 * 
	 * @param search
	 *            List of tree indices to go through
	 * @param t
	 *            Given tree to position in the list
	 * @param dim
	 *            Dimension to consider between trees
	 * @return int Index at which the tree should be inserted to maintain the
	 *         ordering considering that dimension
	 */
	private synchronized int findNewIndexInDimensionList(TIntArrayList search, int t, int dim) {
		//TODO TEST
		TreeFitnessInfo dimension = dimensions[dim];
		int start, end, midPt;
		start = 0;
		end = search.size() - 1;
		midPt = (start + end) / 2;
		//While we have not narrowed it down
		while (start <= end) {
			midPt = (start + end) / 2;
			if (compareOnDimension(t, search.get(midPt), dimension) > 0) {
				start = midPt + 1;
				midPt++;
			} else {
				end = midPt - 1;
			}
		}
		return midPt;
	}

	/**
	 * Returns the difference between the two trees in the given dimension,
	 * taken into account whether bigger is better or worse. Values below 0
	 * indicate t1 is better, above 0 t2 is better, 0 means equal
	 * 
	 * @param t1
	 *            First tree
	 * @param t2
	 *            Second tree
	 * @param dimension
	 *            TreeFitnessInfo instance of the dimension to check
	 * @return double difference in dimension, e.g. <0 means t1 is better
	 */
	public synchronized double compareOnDimension(ProbProcessArrayTree t1, ProbProcessArrayTree t2, TreeFitnessInfo dimension) {
		if (dimension.isNatural()) {
			return registry.getFitness(t2).fitnessValues.get(dimension)
					- registry.getFitness(t1).fitnessValues.get(dimension);
		} else {
			return registry.getFitness(t1).fitnessValues.get(dimension)
					- registry.getFitness(t2).fitnessValues.get(dimension);
		}
	}

	/**
	 * Wrapper for compareOnDimension(Tree,Tree,String) using tree indeces
	 * 
	 * @param t1
	 *            Index of tree 1
	 * @param t2
	 *            Index of tree 2
	 * @param dimension
	 * @return
	 */
	private synchronized double compareOnDimension(int t1, int t2, TreeFitnessInfo dimension) {
		return compareOnDimension(indexToTree.get(t1), indexToTree.get(t2), dimension);
	}

	/**
	 * Removes all trees from the Pareto front that are dominated by the given
	 * tree
	 * 
	 * @param t
	 */
	private synchronized void removeAllDominatedBy(ProbProcessArrayTree tree) {
		//Since we are updating the list while we are looping through, do it like this
		int i = 0;
		while (i < treeToIndex.size()) {
			ProbProcessArrayTree candidate = indexToTree.get(i);
			if (!tree.equals(candidate) && dominates(tree, candidate, false)
					&& !lockedTreesThatShouldBeRemoved.contains(tree)
					&& !lockedTreesThatShouldBeRemoved.contains(candidate)) {
				//Remove dominated tree and maintain this index
				removeTree(candidate);
			} else {
				//If not removed, move on to the next
				i++;
			}
		}
	}

	/**
	 * Returns the index of the dimension key or -1 if not present
	 * 
	 * @param dimension
	 *            TreeFitnessInfo instance of the dimension
	 * @return int index of the dimension of -1 if not present
	 */
	private synchronized int getIndexOfDimension(TreeFitnessInfo dimension) {
		for (int i = 0; i < dimensions.length; i++) {
			if (dimensions[i].equals(dimension))
				return i;
		}

		return -1;
	}

	/**
	 * Removes the provided tree from the Pareto front and updates all lists and
	 * indices
	 * 
	 * @param t
	 */
	private synchronized void removeTree(ProbProcessArrayTree tree) {
		assert checkConsistency();

		if (!treeToIndex.containsKey(tree)) {
			//We're done before we even started...
			return;
		}

		//Check if the tree is locked
		if (lockedTrees.contains(tree)) {
			//If so, indicate that it should be removed on lock release
			lockedTreesThatShouldBeRemoved.add(tree);
			//TODO remove debug code
			System.out.println("Keeping locked tree " + tree + " for now...");
			return;
		}

		int t = treeToIndex.get(tree);

		//First, remove the tree from all dimension lists
		for (TIntArrayList dimList : dimensionToTree) {
			dimList.remove(t);
		}

		//Not needed if we don't keep indexToDimensionPosition track
		//Now remove the tree index to dimension position lists
		//		indexToDimensionPosition.remove(t);

		//And then from the index <-> tree lists
		//FIXME indexOutOfBounds here with index > size (originates from ParetoEngine.nextEvolutionStep (->consider>addTree>removeAllDominatedBy->RemoveTree) so probably not sync. issue but 'just' inconsistency. Maybe also the cause for 'not pareto optimal' on re-import?
		indexToTree.remove(t);
		treeToIndex.remove(tree);

		//Now update all trees with higher indices to move one down, in both lists

		//Note: j now goes up to the size since the last tree has this index because we just decreased the size with one
		for (int j = t; j < indexToTree.size(); j++) {
			//Get also the tree currently at position j
			ProbProcessArrayTree movingTree = indexToTree.get(j);

			//for the index to tree list we remove the old reference and add the new one
			/*-
			//NOT NEEDED any more since the indexToTree arraylist is updated automatically...
			indexToTree.remove(j);
			indexToTree.put(j - 1, movingTree); //CAUSE?
			/**/

			//And we decrease the index as referred to by our tree
			treeToIndex.put(movingTree, j);
			//treeToIndex.adjustValue(movingTree, -1);

			//And also the index to dimension position
			//indexToDimensionPosition.put(j - 1, indexToDimensionPosition.remove(j)); //CAUSE?

			//TODO also update the locked and lockedTreesThatShouldBeRemoved lists if we want to go back to int lists :)
		}

		//Now update all dimension->tree lists and decrease the index of trees > t with one
		for (TIntArrayList dimList : dimensionToTree) {
			for (int i = 0; i < dimList.size(); i++) {
				if (dimList.get(i) > t) {
					dimList.replace(i, dimList.get(i) - 1);
				}
			}
		}

		assert checkConsistency();
	}

	/**
	 * Returns the number of trees in the current Pareto front that dominate the
	 * given tree
	 * 
	 * @param t
	 * @return
	 */
	public synchronized int countDominatorsOf(ProbProcessArrayTree t) {
		int nrDominating = 0;

		for (ProbProcessArrayTree candidate : treeToIndex.keySet()) {
			if (dominates(candidate, t))
				nrDominating++;
		}

		return nrDominating;
	}

	/**
	 * Returns TRUE if tree t dominates tree candidate. Dominance means for all
	 * dimensions equal and for at least one better.
	 * 
	 * @param t
	 * @param candidate
	 * @return TRUE/FALSE
	 */
	public synchronized boolean dominates(ProbProcessArrayTree t, ProbProcessArrayTree candidate) {
		return dominates(t, candidate, true);
	}

	/**
	 * Returns TRUE if tree t1 dominates t2. Dominance means for all dimensions
	 * equal and, if the requireStrictlyBetter boolean is set to TRUE, for at
	 * least one better.
	 * 
	 * @param t1
	 *            NAryTree to be tested if it dominates the other tree
	 * @param t2
	 *            NAryTree second tree to be tested for dominance by t
	 * @param requireStrictlyBetter
	 *            Boolean to indicate if we require at least one dimension to be
	 *            strictly better (if false then equal is also good enough)
	 * @return TRUE if t1 dominates t2
	 */
	public synchronized boolean dominates(ProbProcessArrayTree t1, ProbProcessArrayTree t2, boolean requireStrictlyBetter) {
		boolean oneStrictlyBetter = false;

		//For each fitness dimension
		for (TreeFitnessInfo dimension : dimensions) {
			double tF = registry.getFitness(t1).fitnessValues.get(dimension);
			double cF = registry.getFitness(t2).fitnessValues.get(dimension);

			//non-natural means lower is better
			if (!dimension.isNatural()) {
				//if t has a strictly better(/lower) value
				if (tF < cF) {
					//keep track
					oneStrictlyBetter = true;
				} else if (tF > cF) {
					//if one value is strictly worse then its not dominating
					return false;
				}
			} else {
				//if t has a strictly better(/bigger) value
				if (tF > cF) {
					//keep track
					oneStrictlyBetter = true;
				} else if (tF < cF) {
					//if one value is strictly worse then its not dominating
					return false;
				}
			}

		}

		//If we reach it to here, there is none worse, now check if one is strictly better and if we need that
		if (!requireStrictlyBetter || oneStrictlyBetter)
			return true;
		else
			return false;
	}

	/**
	 * Returns the size of the Pareto front (e.g. number of trees)
	 * 
	 * @return
	 */
	public synchronized int size() {
		return treeToIndex.size();
	}

	/**
	 * Return the complete Pareto front
	 * 
	 * @return
	 */
	public synchronized Collection<ProbProcessArrayTree> getFront() {
		return treeToIndex.keySet();
	}

	/**
	 * Returns the worst tree in the given dimension
	 * 
	 * @param dimension
	 *            Dimension info
	 * @return Tree Worst tree, or NULL if the dimension is not known
	 */
	public synchronized ProbProcessArrayTree getWorst(TreeFitnessInfo dimension) {
		int dim = getIndexOfDimension(dimension);

		if (dim < 0) {
			return null;
		}

		TIntArrayList list = dimensionToTree[dim];
		if (list.size() == 0)
			return null;

		return indexToTree.get(list.get(list.size() - 1));
	}

	/**
	 * Returns the best tree in the given dimension
	 * 
	 * @param dimension
	 *            Dimension info
	 * @return best Tree or NULL if dimension is unknown
	 */
	public synchronized ProbProcessArrayTree getBest(TreeFitnessInfo dimension) {
		if (getIndexOfDimension(dimension) < 0) {
			return null;
		}

		TIntArrayList list = dimensionToTree[getIndexOfDimension(dimension)];
		return indexToTree.get(list.get(0));
	}

	/**
	 * Returns the tree next to the given tree which is slightly worse in the
	 * given dimension
	 * 
	 * @param dimension
	 *            Dimension Info
	 * @param tree
	 *            Tree to use as reference
	 * @return Tree slightly worse tree in the given dimension or NULL if the
	 *         provided dimension or tree is not known
	 */
	public synchronized ProbProcessArrayTree getWorse(TreeFitnessInfo dimension, ProbProcessArrayTree tree) {
		if (!treeToIndex.containsKey(tree)) {
			return null;
		}

		int d = getIndexOfDimension(dimension);

		if (d < 0) {
			return null;
		}

		//First, locate the tree in the current dimension
		int dimPos = findPositionOfTreeInDimension(d, treeToIndex.get(tree));

		//Get the list
		TIntArrayList dimensionTrees = dimensionToTree[d];

		//TODO TEST
		//If we are at the very last position, return the current tree as it is the worst
		if (dimPos == (dimensionTrees.size() - 1))
			return tree;
		else
			//Else, return the tree at one worse position
			return indexToTree.get(dimensionTrees.get(dimPos + 1));

		/*-
		//Otherwise, get the value of the tree in the given dimension
		Double val = registry.getFitness(tree).fitnessValues.get(dimension);

		//And go up the list
		for (int i = dimPos + 1; i < dimensionTrees.size(); i++) {
			dimPos++;
			return indexToTree.get(dimensionTrees.get(dimPos));
		}

		//If we end up here then return the tree we started from
		return tree;
		/**/
	}

	/**
	 * Returns the tree next to the given tree which is slightly better in the
	 * given dimension
	 * 
	 * @param dimension
	 *            Dimension info
	 * @param tree
	 *            Tree to use as reference
	 * @return Tree slightly better tree in the given dimension or NULL if the
	 *         provided dimension or tree is not known
	 */
	public synchronized ProbProcessArrayTree getBetter(TreeFitnessInfo dimension, ProbProcessArrayTree tree) {
		if (!treeToIndex.containsKey(tree)) {
			return null;
		}

		int d = getIndexOfDimension(dimension);

		if (d < 0) {
			return null;
		}

		//First, locate the tree in the current dimension
		int dimPos = findPositionOfTreeInDimension(d, treeToIndex.get(tree));
		//int dimPos = indexToDimensionPosition.get(treeToIndex.get(tree))[d];

		TIntArrayList dimensionTrees = dimensionToTree[d];

		//If we are at the very first position, return the current tree as it is the best
		if (dimPos == 0)
			return tree;
		else
			return indexToTree.get(dimensionTrees.get(dimPos - 1));
	}

	/**
	 * Returns the number of trees that for the given dimension are better
	 * 
	 * @param dimension
	 *            Dimension info
	 * @param tree
	 *            Reference tree
	 * @return int Number of better trees, or -1 if the provided tree is not
	 *         known
	 */
	public synchronized int getNrBetter(TreeFitnessInfo dimension, ProbProcessArrayTree tree) {
		if (!treeToIndex.containsKey(tree)) {
			return -1;
		}

		int dim = getIndexOfDimension(dimension);

		if (dim < 0) {
			return -1;
		}

		/*
		 * The position of the given tree in the requested dimension list
		 * indicates the number of trees that is before, e.g. better than, the
		 * tree
		 */
		return findPositionOfTreeInDimension(dim, treeToIndex.get(tree));
		//		return indexToDimensionPosition.get(treeToIndex.get(tree))[dim];
	}

	/**
	 * Returns the number of trees that for the given dimension are worse
	 * 
	 * @param dimension
	 *            Dimension info
	 * @param tree
	 *            Reference tree
	 * @return int Number of worse trees, or -1 if the provided tree is not
	 *         known
	 */
	public synchronized int getNrWorse(TreeFitnessInfo dimension, ProbProcessArrayTree tree) {
		if (!treeToIndex.containsKey(tree)) {
			return -1;
		}

		int dim = getIndexOfDimension(dimension);

		if (dim < 0) {
			return -1;
		}

		// The position of the given tree in the requested dimension list
		int pos = findPositionOfTreeInDimension(dim, treeToIndex.get(tree));
		//int pos = indexToDimensionPosition.get(treeToIndex.get(tree))[dim];
		// ..can be used to calculate the nr of worse tree, using the total number of trees
		return treeToIndex.size() - pos - 1;
	}

	/**
	 * Utility function used for debugging to check the internal registries of
	 * the Pareto Front for consistency
	 * 
	 * @return Boolean Whether we think the front is consistent
	 */
	public synchronized boolean checkConsistency() {
		//First check: sizes
		if (treeToIndex.size() != indexToTree.size() || dimensions.length != dimensionToTree.length
				|| getFront().size() != treeToIndex.size()) {
			return false;
		}

		//Second check: tree index consistency
		for (int i = 0; i < indexToTree.size(); i++) {
			ProbProcessArrayTree t = indexToTree.get(i);
			if (treeToIndex.get(t) != i) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the index of the given tree in the list of the given dimension.
	 * (Used to be a registry but since lookup is log(n) and bookkeeping seemed
	 * worse for performance during Pareto front creation)
	 * 
	 * @param dim
	 * @param t
	 * @return
	 */
	private synchronized int findPositionOfTreeInDimension(int dim, int t) {
		TIntArrayList dimension = dimensionToTree[dim];
		for (int i = 0; i < dimension.size(); i++) {
			if (dimension.get(i) == t)
				return i;
		}

		return -1;
	}

	/**
	 * Returns a string representation of the Pareto front
	 */
	public synchronized String toString() {
		StringBuilder str = new StringBuilder();

		//A basic info string
		str.append("Pareto Front of size " + size() + " considering " + dimensions.length + " dimensions ("
				+ Arrays.toString(dimensions) + ")\r\n");

		for (ProbProcessArrayTree t : getFront()) {
			if (shouldBeRemovedButIsLocked(t)) {
				str.append("(X) ");
			}

			TObjectDoubleHashMap<TreeFitnessInfo> fitValues = registry.getFitness(t).fitnessValues;
			//Start fitness section with [
			str.append('[');
			for (TreeFitnessInfo dimension : dimensions) {
				str.append(String.format("%s:%2.10f, ", dimension.getCode(), fitValues.get(dimension)));
			}
			//Remove last ", " part
			str.replace(str.length() - 2, str.length(), "");
			//Add closing ] bracket
			str.append("] ");

			//Now output tree string
			str.append(TreeUtils.toString(t, registry.getEventClasses()));
			str.append("\r\n");
		}

		return str.toString();
	}

	/**
	 * Set a new upper limit for the given dimension. NOTE that this limit is
	 * not applied if that would remove all trees currently in the Pareto
	 * front!!!
	 * 
	 * @param dimension
	 *            Dimension to update upper limit for
	 * @param limit
	 *            New upper limit
	 */
	public synchronized boolean updateUpperLimit(TreeFitnessInfo dimension, double limit) {
		return updateLimit(dimension, limit, true);
	}

	/**
	 * Set a new lower limit for the given dimension. NOTE that this limit is
	 * not applied if that would remove all trees currently in the Pareto
	 * front!!!
	 * 
	 * @param dimension
	 *            Dimension to update lower limit for
	 * @param limit
	 *            New lower limit
	 */
	public synchronized boolean updateLowerLimit(TreeFitnessInfo dimension, double limit) {
		return updateLimit(dimension, limit, false);
	}

	/**
	 * Internal method to update a limit for a given dimension
	 * 
	 * @param dimension
	 *            Dimension to update limit for
	 * @param limit
	 *            New limit
	 * @param upper
	 *            Whether upper or lower limit should be updated
	 * 
	 * 
	 */
	private synchronized boolean updateLimit(TreeFitnessInfo dimension, double limit, boolean upper) {
		int d = getIndexOfDimension(dimension);

		if (d < 0 || d >= dimensions.length) {
			//Failure
			return false;
		}

		Double oldLimit;
		if (upper) {
			oldLimit = upperLimits[d];
		} else {
			oldLimit = lowerLimits[d];
		}

		//Even if we do not enforce the new limit, at least remember it for future additions
		if (upper) {
			upperLimits[d] = limit;
		} else {
			lowerLimits[d] = limit;
		}

		//Now enforce the new limit, but only if it was unset or less restrictive than the new one
		if ((!upper && (oldLimit.compareTo(limit) < 0)) || (upper && (oldLimit.compareTo(limit) > 0))) {
			TIntArrayList dimensionList = dimensionToTree[d];

			//Now check the tree on the edge of this dimension and if it's outside the limit actually remove it
			if (treeToIndex.isEmpty())
				return false;

			//First check if there is an tree that falls outside the limit, e.g. will there be a tree remaining if we apply it?
			if (upper) {
				if (registry.getFitness(indexToTree.get(dimensionList.get(dimensionList.size() - 1))).fitnessValues
						.get(dimension) > limit) {
					return false;
				}
			} else {
				if (registry.getFitness(indexToTree.get(dimensionList.get(0))).fitnessValues.get(dimension) < limit) {
					return false;
				}
			}

			//Now find the first tree that is outside the limit
			ProbProcessArrayTree treeOnEdge;
			if (upper) {
				treeOnEdge = indexToTree.get(dimensionList.get(0));
			} else {
				treeOnEdge = indexToTree.get(dimensionList.get(dimensionList.size() - 1));
			}

			double val = registry.getFitness(treeOnEdge).fitnessValues.get(dimension);
			while ((!upper && (val < limit)) || (upper && (val > limit))) {
				if (treeToIndex.size() == 1) {
					//NEVER ever remove the last tree
					return false;
				}

				//Else, keep pruning...
				removeTree(treeOnEdge);

				//Select the next tree
				if (upper) {
					treeOnEdge = indexToTree.get(dimensionList.get(0));
					//It could be that the last tree was selected and thus is not removed but we want to move on
					if (lockedTreesThatShouldBeRemoved.contains(treeOnEdge)) {
						if (indexToTree.size() > 1)
							treeOnEdge = indexToTree.get(dimensionList.get(dimensionList.get(0)));
						else
							return true;
					}
				} else {
					treeOnEdge = indexToTree.get(dimensionList.get(dimensionList.size() - 1));
					if (lockedTreesThatShouldBeRemoved.contains(treeOnEdge)) {
						if (indexToTree.size() > 1)
							treeOnEdge = indexToTree.get(dimensionList.get(dimensionList.size() - 2));
						else
							return true;
					}
				}

				val = registry.getFitness(treeOnEdge).fitnessValues.get(dimension);
			}
		}

		//if we made it here, we have successfully applied the limit
		return true;
	}

	/**
	 * Returns the current lower limit for the given dimension
	 * 
	 * @param dimension
	 *            Dimension to get the lower limit for
	 * @return double Limit
	 */
	public synchronized double getLowerLimit(TreeFitnessInfo dimension) {
		return lowerLimits[getIndexOfDimension(dimension)];
	}

	/**
	 * Returns the current upper limit for the given dimension
	 * 
	 * @param dimension
	 *            Dimension to get the upper limit for
	 * @return double Limit
	 */
	public synchronized double getUpperLimit(TreeFitnessInfo dimension) {
		return upperLimits[getIndexOfDimension(dimension)];
	}

	/**
	 * Locks the provided tree, meaning that it will not be removed from the
	 * Pareto front during the lock
	 * 
	 * @param tree
	 */
	public synchronized void lockTree(ProbProcessArrayTree tree) {
		lockedTrees.add(tree);
	}

	/**
	 * Unlocks the provided tree, allowing it to be deleted. Returns TRUE if the
	 * tree is deleted or FALSE if immediately after releasing the lock it is
	 * still present in the tree
	 * 
	 * @param tree
	 *            Tree to unlock
	 * @return Boolean TRUE if tree is removed after releasing the lock
	 */
	public synchronized boolean unlockTree(ProbProcessArrayTree tree) {
		//check if the tree was actually locked	
		if (lockedTrees.contains(tree)) {
			//Remove lock
			lockedTrees.remove(tree);

			//Check if the tree is scheduled for deletion
			if (lockedTreesThatShouldBeRemoved.contains(tree)) {
				//Remove from that list
				lockedTreesThatShouldBeRemoved.remove(tree);

				//Actually remove tree
				removeTree(tree);

				//And return true since we removed it
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether the given tree is in the Pareto front
	 * 
	 * @param tree
	 *            NAryTree to check for Pareto front inclusion
	 * @return TRUE if the Pareto front contains this tree
	 */
	public synchronized boolean inParetoFront(ProbProcessArrayTree tree) {
		ProbProcessArrayTree normalizedTree = tree;
		//If we don't know the tree
		if (!treeToIndex.containsKey(normalizedTree)) {
			return false;
		}

		//It could also be that the tree is technically speaking in the Pareto front but it shouldn't be
		return !lockedTreesThatShouldBeRemoved.contains(tree);
	}

	public synchronized boolean shouldBeRemovedButIsLocked(ProbProcessArrayTree tree) {
		if (treeToIndex.containsKey(tree))
			return lockedTreesThatShouldBeRemoved.contains(tree);

		return false;
	}

	/**
	 * To be able to get the fitness values for the trees while visualising the
	 * Pareto front
	 * 
	 * @return
	 */
	public synchronized CentralRegistry getRegistry() {
		return registry;
	}

	/**
	 * Returns an array of the dimensions considered
	 * 
	 * @return
	 */
	public synchronized TreeFitnessInfo[] getDimensions() {
		return dimensions;
	}

	/**
	 * Returns all trees currently in the Pareto Front with the specified values
	 * for the dimensions (e.g. look-up)
	 * 
	 * @param values
	 * @return
	 */
	public synchronized Set<ProbProcessArrayTree> getTreeWithValues(HashMap<TreeFitnessInfo, Double> values) {
		HashSet<ProbProcessArrayTree> trees = new HashSet<ProbProcessArrayTree>();
		//For each tree in the Pareto Front, check the dimension-value and quick fail
		for (ProbProcessArrayTree tree : treeToIndex.keySet()) {
			boolean add = true;
			for (TreeFitnessInfo dimension : values.keySet()) {
				if (registry.getFitness(tree).fitnessValues.get(dimension) != values.get(dimension)) {
					//Found one incorrect value
					add = false;
					break;
				}
			}
			//If all provided values match, add the tree to the return set
			if (add) {
				trees.add(tree);
			}
		}

		return trees;
	}

	/**
	 * Returns a Pareto Front instance with only those members that are
	 * currently in this Pareto front that also form a Pareto Front on a subset
	 * of these dimensions. This can for instance be useful when visualizing the
	 * Pareto Front on two dimensions while the front contains more. In the
	 * visualization the front on these two dimensions can be visualized
	 * differently than the 'inferior' ones that have a right to be in the front
	 * when other dimensions are considered.
	 * 
	 * @param dimensions
	 * @return
	 */
	public synchronized ParetoFront getFrontForDimensions(TreeFitnessInfo[] dimensions) {
		ParetoFront front = new ParetoFront(registry, dimensions);

		front.consider(this.getFront());

		return front;
	}

	/**
	 * Returns a Pareto front instance with only those members that are
	 * currently in this Pareto front and are in between the provided limits.
	 * This method returns another instance of a Pareto front, this Pareto front
	 * is unchanged!
	 * 
	 * @param upperLimits
	 * @param lowerLimits
	 * @return
	 */
	public synchronized ParetoFront getFilteredFront(HashMap<TreeFitnessInfo, Double> upperLimits,
			HashMap<TreeFitnessInfo, Double> lowerLimits) {
		ParetoFront front = new ParetoFront(registry, dimensions);

		if (upperLimits != null) {
			for (Entry<TreeFitnessInfo, Double> upperLimit : upperLimits.entrySet()) {
				front.updateUpperLimit(upperLimit.getKey(), upperLimit.getValue());
			}
		}

		if (lowerLimits != null) {
			for (Entry<TreeFitnessInfo, Double> lowerLimit : lowerLimits.entrySet()) {
				front.updateLowerLimit(lowerLimit.getKey(), lowerLimit.getValue());
			}
		}

		front.consider(this.getFront());

		return front;
	}

	public synchronized void reduceSize() {
		if (size() > maxSize && maxSize > 0) {
			reEvaluateParetoFitness();

			//Get all known trees sorted good to bad
			TreeFitnessInfo[] allDimensions = new TreeFitnessInfo[dimensions.length + 1];
			allDimensions[0] = ParetoFitnessEvaluator.info;
			for (int i = 0; i < dimensions.length; i++) {
				allDimensions[i + 1] = dimensions[i];
			}
			//List<NAryTree> sorted = registry.getSortedOn(false, dimensions);
			ArrayList<ProbProcessArrayTree> trees = new ArrayList<ProbProcessArrayTree>();
			trees.addAll(getFront());
			Collections.sort(trees, new TreeComparatorByFitness(registry, false, allDimensions));

			Iterator<ProbProcessArrayTree> it = trees.iterator();

			//Now start working from the back to the front, removing trees if necessary until we are of the requested size
			while (size() > maxSize && it.hasNext()) {
				ProbProcessArrayTree tree = it.next();
				if (inParetoFront(tree)) {
					//System.out.println("Removing " + registry.getFitness(tree).toString());
					removeTree(tree);
				}
			}
		}
	}

	/**
	 * @return the current maxSize
	 */
	public int getMaxSize() {
		return maxSize;
	}

	/**
	 * @param maxSize
	 *            the maxSize of the Pareto Front to set
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
	}
}