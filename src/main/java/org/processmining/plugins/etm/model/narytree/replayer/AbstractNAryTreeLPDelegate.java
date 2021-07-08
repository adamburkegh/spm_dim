package org.processmining.plugins.etm.model.narytree.replayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.StateBuilder;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;
import lpsolve.MsgListener;
import nl.tue.astar.Tail;
import nl.tue.astar.util.LPProblemProvider;
import nl.tue.astar.util.LPResult;
import nl.tue.astar.util.ShortShortMultiset;

public abstract class AbstractNAryTreeLPDelegate<T extends Tail> extends AbstractNAryTreeDelegate<T> implements
		MsgListener {

	protected final LPProblemProvider solvers;
	protected final boolean useInt;
	protected final boolean useSemCon;
	protected int rows;
	protected int columns;
	protected final int threads;
	protected final int[] leaf2sync;
	protected final int[] cldCnt;
	protected final short[] types;
	protected final Map<LpSolve, double[]> relaxed;
	protected final int timeOut;

	protected final boolean useORrows;
	protected final int treeSize;
	protected final int varUpBo;
	protected int[] maxValRHS;

	// we use an upperbound of Byte.maxvalue in order to allow for more efficient storage

	static {
		try {
			System.loadLibrary("lpsolve55");
			System.loadLibrary("lpsolve55j");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean isIntVariables() {
		return useInt;
	}

	public AbstractNAryTreeLPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber,
			int[] node2cost, int threads, boolean useInt, boolean useSemiCon, int timeOut, int varUpBo) {
		this(algorithm, tree, configurationNumber, node2cost, threads, useInt, useSemiCon, timeOut, varUpBo, false);
	}

	public AbstractNAryTreeLPDelegate(AStarAlgorithm algorithm, ProbProcessArrayTree tree, int configurationNumber,
			int[] node2cost, int threads, boolean useInt, boolean useSemiCon, int timeOut, int varUpBo,
			boolean useORRows) {
		super(algorithm, tree, configurationNumber, node2cost, threads);
		this.threads = threads;
		this.useInt = useInt;
		this.useSemCon = useSemiCon;
		this.timeOut = timeOut;
		this.varUpBo = varUpBo;
		this.useORrows = useORRows;
		this.treeSize = tree.size();

		leaf2sync = new int[tree.size()];
		types = new short[tree.size()];
		cldCnt = new int[tree.size()];
		//cldCnt = new int[tree.size()];
		LpSolve solver = null;
		LPProblemProvider solvers = null;
		try {
			// The variables (columns):
			// all nodes for move model only, depending on type:
			// - LEAF: single column
			// - XOR, SEQ, AND: single column
			// - OR: 1+ number of children columns
			// all non-tau leafs for sync move
			// all activities for move log only
			//
			// upperbound for the number of columns: 3* tree.size() - tree.numLeafs() + algorithm.getClasses().size();

			// The rows (constraints)
			// - One row for the root
			// - one row for each child of a non-XOR parent
			// - one row for each XOR parent
			// - one row for each activity

			// count the number of loop nodes

			int leafs = 0;
			int orSkips = 0;
			int ors = 0;
			int activities = algorithm.getClasses().size();
			for (int i = 0; i < tree.size(); i++) {
				types[i] = tree.getTypeFast(configurationNumber, i);
				if (tree.isLeaf(i) && types[i] != ProbProcessArrayTree.TAU && !tree.isHidden(configurationNumber, i)) {
					leafs++;
				}
				if (types[i] == ProbProcessArrayTree.OR && !tree.isHidden(configurationNumber, i)
						&& tree.getNextFast(i) > tree.getNextFast(i + 1)) {
					ors++;
				}
				if (getType(tree.getParentFast(i)) == ProbProcessArrayTree.OR
						&& !tree.isHidden(configurationNumber, tree.getParentFast(i))) {
					orSkips++;
				}
			}

			rows = tree.size() + activities + 1;
			if (useORrows) {
				// when using the OR constraints, implicit termination of the OR is not allowed
				super.setAllowImplicitOrTermination(false);
				rows += ors;
			}
			this.maxValRHS = new int[rows + 1];
			maxValRHS[0] = 1;

			columns = tree.size() + activities + leafs + orSkips;

			// we know the number of rows and columns.

			solver = LpSolve.makeLp(rows, columns);

			int or = 0;
			int orCol = 0;
			int lf = 0;
			for (int i = 0; i < tree.size(); i++) {

				maxValRHS[i + 1] = 1;

				types[i] = getType(i);

				// modelMove for node i
				solver.setLowbo(1 + i, 0);
				solver.setUpbo(1 + i, tree.isBlocked(configurationNumber, i) ? 0 : varUpBo);
				// type integer
				solver.setInt(1 + i, useInt);
				solver.setSemicont(1 + i, useSemCon);
				// modelMove cost in objective
				if (tree.isHidden(configurationNumber, i)) {
					int p = tree.getParent(i);
					if (p >= 0) {
						if (types[p] == ProbProcessArrayTree.AND || types[p] == ProbProcessArrayTree.SEQ || types[p] == ProbProcessArrayTree.REVSEQ) {
							solver.setMat(0, 1 + i, 0);
							solver.setMat(rows, 1 + i, 0);
						} else {
							solver.setMat(0, 1 + i, 1);
							solver.setMat(rows, 1 + i, 1);
						}
					} else {
						// root node is hidden, cost is 1
						solver.setMat(0, 1 + i, 1);
						solver.setMat(rows, 1 + i, 1);
					}
				} else {
					solver.setMat(0, 1 + i, getModelMoveCost(i));
					solver.setMat(rows, 1 + i, getModelMoveCost(i));
				}
				int p = tree.getParentFast(i);
				if (getType(p) == ProbProcessArrayTree.XOR) {
					// -effect on parent's first child's row
					solver.setMat(tree.getParent(i) + 2, i + 1, 1);
				} else if (getType(p) == ProbProcessArrayTree.LOOP) {
					// parent is a loop.
					if (p + 1 == i) {
						// left child.
						// consume on own row,
						solver.setMat(i + 1, i + 1, 1);
						// produce on middle child's row
						solver.setMat(tree.getNextFast(i) + 1, i + 1, -1);
					} else if (tree.getNextFast(p + 1) == i) {
						// middle child
						// consume on own row,
						solver.setMat(i + 1, i + 1, 1);
						// produce on left child's row
						solver.setMat(p + 2, i + 1, -1);
					} else {
						// right child
						// consume on own row,
						solver.setMat(i + 1, i + 1, 1);
						// consume on middle child's row,
						solver.setMat(tree.getNextFast(p + 1) + 1, i + 1, 1);
					}
				} else {
					// -effect on own row
					solver.setMat(i + 1, i + 1, 1);
				}
				if (tree.isLeaf(i) || tree.isHidden(configurationNumber, i)) {
					// setup sync move
					// use column only if non-tau leaf that's not hidden
					if (tree.isLeaf(i) && !tree.isHidden(configurationNumber, i) && types[i] != ProbProcessArrayTree.TAU) {
						int col = (++lf) + tree.size() + activities;
						leaf2sync[i] = col - 1;
						// modelMove for node i
						solver.setLowbo(col, 0);
						solver.setUpbo(col, tree.isBlocked(configurationNumber, i) ? 0 : varUpBo);
						// type integer
						solver.setInt(col, useInt);
						solver.setSemicont(col, useSemCon);
						// synchronous move cost in objective
						solver.setMat(0, col, 1);
						solver.setMat(rows, col, 1);
						if (getType(tree.getParentFast(i)) == ProbProcessArrayTree.XOR) {
							// -effect on parent's first child's row
							solver.setMat(tree.getParentFast(i) + 2, col, 1);
						} else if (getType(p) == ProbProcessArrayTree.LOOP) {
							// parent is a loop.
							if (p + 1 == i) {
								// left child.
								// consume on own row,
								solver.setMat(i + 1, col, 1);
								// produce on middle child's row
								solver.setMat(tree.getNextFast(i) + 1, col, -1);
							} else if (tree.getNextFast(p + 1) == i) {
								// middle child
								// consume on own row,
								solver.setMat(i + 1, col, 1);
								// produce on left child's row
								solver.setMat(p + 2, col, -1);
							} else {
								// right child
								// consume on own row,
								solver.setMat(i + 1, col, 1);
								// consume on middle child's row,
								solver.setMat(tree.getNextFast(p + 1) + 1, col, 1);
							}
						} else {
							// -effect on own row
							solver.setMat(i + 1, col, 1);
						}
						solver.setMat(1 + tree.size() + types[i], col, 1);
					}
					// skip ahead to the next node (-1 to account for i++ in for loop;
					i = tree.getNextFast(i) - 1;
				} else if (types[i] == ProbProcessArrayTree.LOOP) {
					// enable left child
					solver.setMat(i + 2, i + 1, -1);
					// enable right child
					solver.setMat(1 + tree.getNextFast(tree.getNextFast(i + 1)), i + 1, -1);

				} else if (types[i] == ProbProcessArrayTree.XOR) {
					// do produce for the first child only.
					solver.setMat(i + 2, i + 1, -1);
				} else {
					// produce token for all children
					int j = i + 1;
					do {
						solver.setMat(j + 1, i + 1, -1);
						j = tree.getNextFast(j);
					} while (tree.getParent(j) == i);
					if (types[i] == ProbProcessArrayTree.OR) {
						// update the cost for an OR, since every OR needs to be cleaned up which is 
						// an explicit action in the tree
						// unless it has only one child.
						if (tree.getNextFast(i) > tree.getNextFast(i + 1)) {

							int row = tree.size() + activities + ++or;
							leaf2sync[i] = row;

							if (useORrows) {
								// count children
								int cld = 0;
								j = i + 1;
								do {
									cld++;
									j = tree.getNextFast(j);
								} while (tree.getParent(j) == i);
								cldCnt[i] = cld;
								solver.setMat(row, i + 1, -cld + 1);
								solver.setConstrType(row, LpSolve.LE);
								maxValRHS[row] = cld - 1;
								if (maxValRHS[row] > maxValRHS[0]) {
									maxValRHS[0] = maxValRHS[row];
								}

							}

							j = i + 1;
							do {
								int col = tree.size() + activities + leafs + (++orCol);
								// modelMove for node i
								solver.setLowbo(col, 0);
								solver.setUpbo(col, varUpBo);
								// type integer
								solver.setInt(col, useInt);
								solver.setSemicont(col, useSemCon);
								// remove a token from the start place of the child 
								solver.setMat(j + 1, col, 1);
								if (useORrows) {
									// add constraint limiting all children together
									solver.setMat(row, col, 1);
								}
								j = tree.getNextFast(j);
							} while (tree.getParent(j) == i);

						}
					}
				}
			}

			for (int i = 0; i < activities; i++) {
				int row = treeSize + i + 1;
				int col = row;
				maxValRHS[row] = algorithm.getMaxOccurranceInTrace(i);
				if (maxValRHS[row] > maxValRHS[0]) {
					maxValRHS[0] = maxValRHS[row];
				}

				// setup the logmoves
				// modelMove for node i
				solver.setLowbo(col, 0);
				solver.setUpbo(col, varUpBo);
				// type integer
				solver.setInt(col, useInt);
				solver.setSemicont(col, useSemCon);
				// modelMove cost in objective
				solver.setMat(0, col, getLogMoveCost(i));
				solver.setMat(rows, col, getLogMoveCost(i));

				// set effect on logMove part.
				solver.setMat(row, col, 1);
			}

			for (int i = 0; i < treeSize + activities; i++) {
				// row type
				solver.setConstrType(i + 1, LpSolve.EQ);
			}
			solver.setConstrType(rows, LpSolve.GE);

			solver.setMinim();

			solver.setScaling(LpSolve.SCALE_GEOMETRIC | LpSolve.SCALE_EQUILIBRATE);//| LpSolve.SCALE_INTEGERS);
			solver.setScalelimit(5);
			solver.setPivoting(LpSolve.PRICER_DEVEX | LpSolve.PRICE_ADAPTIVE);
			solver.setMaxpivot(250);
			solver.setBbFloorfirst(LpSolve.BRANCH_AUTOMATIC);
			solver.setBbRule(LpSolve.NODE_PSEUDONONINTSELECT | LpSolve.NODE_GREEDYMODE | LpSolve.NODE_DYNAMICMODE
					| LpSolve.NODE_RCOSTFIXING);
			solver.setBbDepthlimit(-50);
			solver.setAntiDegen(LpSolve.ANTIDEGEN_FIXEDVARS | LpSolve.ANTIDEGEN_STALLING);
			solver.setImprove(LpSolve.IMPROVE_DUALFEAS | LpSolve.IMPROVE_THETAGAP);
			solver.setBasiscrash(LpSolve.CRASH_NOTHING);
			solver.setSimplextype(LpSolve.SIMPLEX_DUAL_PRIMAL);
			//			solver.setSimplextype(LpSolve.SIMPLEX_PRIMAL_DUAL);
			try {
				solver.setBFPFromPath("bfp_etaPFI");
			} catch (LpSolveException e) {
				solver.setBFP(null);
			}

			//solver.solve();
			//solver.printLp();

			solver.setVerbose(1);
			solver.setTimeout(timeOut);
			if (useInt && timeOut > 0 && canUtilizeMsgfunc()) {
				// only add the message listener to catch the relaxed solutions if using INT and Timeouts
				solvers = new LPProblemProvider(solver, threads, this, LpSolve.MSG_LPOPTIMAL);
			} else {
				solvers = new LPProblemProvider(solver, threads);//, this, LpSolve.MSG_LPOPTIMAL);
			}

		} catch (LpSolveException e) {
			e.printStackTrace();
			solver = null;
		} finally {
			this.solvers = solvers;
			relaxed = new HashMap<LpSolve, double[]>(threads);
		}

		//		System.out.println("Max state size: "
		//				+ (16 + TreeHead.getSizeFor(nodes, numEventClasses()) + TreeTail.getSizeFor(vars)));

	}

	// Override to disallow the LPProblemProvider from being created with a message listener
	protected boolean canUtilizeMsgfunc() {
		return true;
	}

	private short getType(int node) {
		return node >= treeSize || node < 0 ? ProbProcessArrayTree.NONE : types[node];
	}

	@Override
	public void setAllowImplicitOrTermination(boolean allowImplicitOrTermination) {
		// if useing OR rows, ORs must be terminated explicitly, otherwise the estimates are 
		// not guaranteed to be consistent (i.e. h(x) <= d(x,y) + h(y)) .
		if (!useORrows) {
			super.setAllowImplicitOrTermination(allowImplicitOrTermination);
		} else {
			super.setAllowImplicitOrTermination(false);
		}
	}

	public void msgfunc(LpSolve solver, Object handle, int message) throws LpSolveException {
		final double[] res = new double[columns];
		solver.getVariables(res);
		synchronized (relaxed) {
			relaxed.put(solver, res);
		}
	}

	public void deleteLPs() {
		solvers.deleteLps();
	}

	public void printLp() {
		LpSolve solver = solvers.firstAvailable();
		solver.printLp();
	}

	public int getSyncMoveVar(int leaf) {
		return leaf2sync[leaf];
	}

	protected double[] setupRhs(byte[] state, ShortShortMultiset parikh, int minCost) {
		final double[] rhs = new double[rows + 1];
		final short[] p = parikh.getInternalValues();

		int i = 0;
		//for (int i = 0; i < tree.size(); i++) {
		int parent, s, scope;
		do {
			s = getState(state, i);
			scope = tree.getNextFast(i);
			if (s == StateBuilder.N) {
				i = scope;
				continue;
			}
			if (types[i] == ProbProcessArrayTree.OR) {
				if (s == StateBuilder.F) {
					// OR in state F only needs a token iff all children are in N
					int ch = i + 1;
					boolean allN = true;
					int enabledOrExecuting = 0;
					int first = i;
					do {
						if (getState(state, ch) != StateBuilder.N) {
							allN = false;
							if (first == i) {
								first = ch;
							}
							enabledOrExecuting++;
						}
						ch = tree.getNextFast(ch);
					} while ((useORrows || allN) && ch < scope);
					if (!allN) {
						// not all children are in N, hence
						if (useORrows && leaf2sync[i] > 0) {
							// the number of enabled children determines the number of 
							// skips still allowed. 
							if (enabledOrExecuting == cldCnt[i]) {
								// all children enabled, hence all but one skippable.
								rhs[leaf2sync[i]] = enabledOrExecuting - 1;
							} else {
								//At least one child started. All remaining enabled 
								// children can be skipped.
								rhs[leaf2sync[i]] = enabledOrExecuting;
							}
						}
						i = first;
						continue;
					} else {
						parent = tree.getParentFast(i);
						// enable the OR:
						if (i == 0) {
							rhs[1] = 1;
						} else if (types[parent] == ProbProcessArrayTree.XOR) {
							// use the  XOR parent
							rhs[parent + 2] = 1;
						} else {
							rhs[i + 1] = 1;
						}
						// all children in N, skip until next subtree
						i = scope;
						continue;
					}
				} else if (useORrows && s == StateBuilder.T && leaf2sync[i] > 0) {
					//OR ready to terminate
					// all children are either E or N.
					int ch = i + 1;
					int enabled = 0;
					int first = i;
					do {
						if (getState(state, ch) == StateBuilder.E) {
							enabled++;
							if (first == i) {
								first = ch;
							}
						}
						ch = tree.getNextFast(ch);
					} while (ch < scope);
					//At least one child completed. All remaining enabled 
					// children can be skipped.
					rhs[leaf2sync[i]] = enabled;
					if (i == first) {
						i++;
						continue;
					}
					i = first;
					continue;
				}
			}
			if (s == StateBuilder.E || s == StateBuilder.F) {
				parent = tree.getParentFast(i);
				if (i == 0) {
					rhs[1] = 1;
				} else if (types[parent] == ProbProcessArrayTree.XOR) {
					// use the XOR parent's first child's row
					rhs[parent + 2] = 1;
				} else {
					rhs[i + 1] = 1;
				}
			}
			i++;

		} while (i < treeSize);
		for (i = p.length; i-- > 0;) {
			rhs[treeSize + i + 1] = p[i];
		}

		rhs[rows] = minCost;
		return rhs;
	}

	/**
	 * Sets up the right hand side where every variable is represented by one
	 * short. The last two shorts in the array encode the lower bound for the
	 * target function, i.e. the length of the returned array is rows+2 where
	 * element 0 is unused.
	 * 
	 * 
	 * 
	 * @param state
	 * @param parikh
	 * @param minCost
	 * @param length
	 * @return
	 */
	protected short[] setupRhsShort(byte[] state, ShortShortMultiset parikh, int minCost) {
		// the right
		final short[] rhs = new short[rows + 2];
		final short[] p = parikh.getInternalValues();

		int i = 0;
		//for (int i = 0; i < tree.size(); i++) {
		int parent, s, scope;
		do {
			s = getState(state, i);
			scope = tree.getNextFast(i);
			if (s == StateBuilder.N) {
				i = scope;
				continue;
			}
			if (types[i] == ProbProcessArrayTree.OR) {
				if (s == StateBuilder.F) {
					// OR in state F only needs a token iff all children are in N
					int ch = i + 1;
					boolean allN = true;
					int enabledOrExecuting = 0;
					int first = i;
					do {
						if (getState(state, ch) != StateBuilder.N) {
							allN = false;
							if (first == i) {
								first = ch;
							}
							enabledOrExecuting++;
						}
						ch = tree.getNextFast(ch);
					} while ((useORrows || allN) && ch < scope);
					if (!allN) {
						// not all children are in N, hence
						if (useORrows && leaf2sync[i] > 0) {
							// the number of enabled children determines the number of 
							// skips still allowed. 
							if (enabledOrExecuting == cldCnt[i]) {
								// all children enabled, hence all but one skippable.
								rhs[leaf2sync[i]] = (short) ((enabledOrExecuting - 1) & 0xFFFF);
							} else {
								//At least one child started. All remaining enabled 
								// children can be skipped.
								rhs[leaf2sync[i]] = (short) (enabledOrExecuting & 0xFFFF);
							}
						}
						i = first;
						continue;
					} else {
						parent = tree.getParentFast(i);
						// enable the OR:
						if (i == 0) {
							rhs[1] = 1;
						} else if (types[parent] == ProbProcessArrayTree.XOR) {
							// use the  XOR parent
							rhs[parent + 2] = 1;
						} else {
							rhs[i + 1] = 1;
						}
						// all children in N, skip until next subtree
						i = scope;
						continue;
					}
				} else if (useORrows && s == StateBuilder.T && leaf2sync[i] > 0) {
					//OR ready to terminate
					// all children are either E or N.
					int ch = i + 1;
					int enabled = 0;
					int first = i;
					do {
						if (getState(state, ch) == StateBuilder.E) {
							enabled++;
							if (first == i) {
								first = ch;
							}
						}
						ch = tree.getNextFast(ch);
					} while (ch < scope);
					//At least one child completed. All remaining enabled 
					// children can be skipped.
					rhs[leaf2sync[i]] = (short) (enabled & 0xFFFF);
					if (i == first) {
						i++;
						continue;
					}
					i = first;
					continue;
				}
			}
			if (s == StateBuilder.E || s == StateBuilder.F) {
				parent = tree.getParentFast(i);
				if (i == 0) {
					rhs[1] = 1;
				} else if (types[parent] == ProbProcessArrayTree.XOR) {
					// use the XOR parent's first child's row
					rhs[parent + 2] = 1;
				} else {
					rhs[i + 1] = 1;
				}
			}
			i++;

		} while (i < treeSize);
		for (i = p.length; i-- > 0;) {
			rhs[treeSize + i + 1] = p[i];
		}

		rhs[rows + 1] = (short) (minCost & 0xFFFF);
		rhs[rows] = (short) ((minCost >> 16) & 0xFFFF);
		return rhs;
	}

	/**
	 * Sets up the right hand side where every variable is represented by one
	 * byte. The last four bytes in the array encode the lower bound for the
	 * target function, i.e. the length of the returned array is rows+4 where
	 * element 0 is unused.
	 * 
	 * 
	 * 
	 * @param state
	 * @param parikh
	 * @param minCost
	 * @param length
	 * @return
	 */
	protected byte[] setupRhsByte(byte[] state, ShortShortMultiset parikh, int minCost) {
		final byte[] rhs = new byte[rows + 5];
		final short[] p = parikh.getInternalValues();

		int i = 0;
		//for (int i = 0; i < tree.size(); i++) {
		int parent, s, scope;
		do {
			s = getState(state, i);
			scope = tree.getNextFast(i);
			if (s == StateBuilder.N) {
				i = scope;
				continue;
			}
			if (types[i] == ProbProcessArrayTree.OR) {
				if (s == StateBuilder.F) {
					// OR in state F only needs a token iff all children are in N
					int ch = i + 1;
					boolean allN = true;
					int enabledOrExecuting = 0;
					int first = i;
					do {
						if (getState(state, ch) != StateBuilder.N) {
							allN = false;
							if (first == i) {
								first = ch;
							}
							enabledOrExecuting++;
						}
						ch = tree.getNextFast(ch);
					} while ((useORrows || allN) && ch < scope);
					if (!allN) {
						// not all children are in N, hence
						if (useORrows && leaf2sync[i] > 0) {
							// the number of enabled children determines the number of 
							// skips still allowed. 
							if (enabledOrExecuting == cldCnt[i]) {
								// all children enabled, hence all but one skippable.
								rhs[leaf2sync[i]] = (byte) ((enabledOrExecuting - 1) & 0xFF);
							} else {
								//At least one child started. All remaining enabled 
								// children can be skipped.
								rhs[leaf2sync[i]] = (byte) (enabledOrExecuting & 0xFF);
							}
						}
						i = first;
						continue;
					} else {
						parent = tree.getParentFast(i);
						// enable the OR:
						if (i == 0) {
							rhs[1] = 1;
						} else if (types[parent] == ProbProcessArrayTree.XOR) {
							// use the  XOR parent
							rhs[parent + 2] = 1;
						} else {
							rhs[i + 1] = 1;
						}
						// all children in N, skip until next subtree
						i = scope;
						continue;
					}
				} else if (useORrows && s == StateBuilder.T && leaf2sync[i] > 0) {
					//OR ready to terminate
					// all children are either E or N.
					int ch = i + 1;
					int enabled = 0;
					int first = i;
					do {
						if (getState(state, ch) == StateBuilder.E) {
							enabled++;
							if (first == i) {
								first = ch;
							}
						}
						ch = tree.getNextFast(ch);
					} while (ch < scope);
					//At least one child completed. All remaining enabled 
					// children can be skipped.
					rhs[leaf2sync[i]] = (byte) (enabled & 0xFF);
					if (i == first) {
						i++;
						continue;
					}
					i = first;
					continue;
				}
			}
			if (s == StateBuilder.E || s == StateBuilder.F) {
				parent = tree.getParentFast(i);
				if (i == 0) {
					rhs[1] = 1;
				} else if (types[parent] == ProbProcessArrayTree.XOR) {
					// use the XOR parent's first child's row
					rhs[parent + 2] = 1;
				} else {
					rhs[i + 1] = 1;
				}
			}
			i++;

		} while (i < treeSize);
		for (i = p.length; i-- > 0;) {
			rhs[treeSize + i + 1] = (byte) (p[i] & 0xFF);
		}

		rhs[rows + 3] = (byte) (minCost & 0xFF);
		rhs[rows + 2] = (byte) ((minCost >> 8) & 0xFF);
		rhs[rows + 1] = (byte) ((minCost >> 16) & 0xFF);
		rhs[rows] = (byte) ((minCost >> 24) & 0xFF);
		return rhs;
	}

	public LPResult estimate(byte[] state, ShortShortMultiset parikh, int minCost) {
		double[] rhs = setupRhs(state, parikh, minCost);
		LpSolve solver = solvers.firstAvailable();
		synchronized (relaxed) {
			relaxed.put(solver, null);
		}

		// the stupid estimate is always a correct estimate
		LPResult res = new LPResult(columns, minCost, LpSolve.SUBOPTIMAL);
		try {
			// instead of setting the default basis, we call resetBasis() whenever we get
			// the INFEASIBLE answer. The model is guaranteed to be FEASIBLE, hence
			// such answer is wrong and resetting the basis in that case should do the
			// trick.
			//
			solver.defaultBasis();
			solver.setRhVec(rhs);
			//			solver.printLp();

			int r = solver.solve();
			if (r == LpSolve.INFEASIBLE) {
				// a node needs to be used more than varUpBo times, which this IP does not allow
				// or the model deadlocks due to blocking.

				res = new LPResult(columns, -2.0, LpSolve.INFEASIBLE);
				//solver.printLp();

			} else if (r == LpSolve.OPTIMAL || r == LpSolve.PRESOLVED) {
				// the solution was optimal. In case of an integer solution, use rounding
				// in case of an LP solution, round down.
				if (useInt) {
					res = new LPResult(columns, solver.getObjective(), LpSolve.OPTIMAL);
				} else {
					res = new LPResult(columns, solver.getObjective(), LpSolve.SUBOPTIMAL);
				}
				solver.getVariables(res.getVariables());

			} else if (useInt && timeOut > 0 && (r == LpSolve.TIMEOUT || r == LpSolve.SUBOPTIMAL)) {
				// timeout reached while using INT. use the relaxed solution if available
				double[] relaxedFound;
				synchronized (relaxed) {
					relaxedFound = relaxed.get(solver);
				}
				if (relaxedFound != null) {
					// timeout after a relaxed solution was found
					res = new LPResult(relaxedFound, solver.getObjective(), LpSolve.SUBOPTIMAL);
				}

			}

			//			if (rhs[rhs.length - 1] == toCatch) {
			//				System.out.println("Done");
			//			}
			return res;

		} catch (LpSolveException e) {
			System.out.println("_________________________________________________________");
			System.out.println(Arrays.toString(state) + "   " + parikh.toString());
			System.out.println(Arrays.toString(rhs));
			System.out.println(Arrays.toString(res.getVariables()) + " est:" + res.getResult());
			solver.printLp();
			assert false;
			return res;
		} finally {
			solvers.finished(solver);
		}
	}

	public int getNumberVariables() {
		return columns;
	}
}