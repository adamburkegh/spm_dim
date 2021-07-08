package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.list.TIntList;
import nl.tue.astar.Head;

public interface TreeHead extends Head {

	public TIntList getMovesMade();
}
