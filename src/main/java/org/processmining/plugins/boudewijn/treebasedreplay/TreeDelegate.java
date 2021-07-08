package org.processmining.plugins.boudewijn.treebasedreplay;

import gnu.trove.set.TShortSet;
import nl.tue.astar.Head;
import nl.tue.astar.Tail;
import nl.tue.astar.impl.State;
import nl.tue.astar.impl.memefficient.StorageAwareDelegate;
import nl.tue.storage.CompressedHashSet;
import nl.tue.storage.Deflater;
import nl.tue.storage.EqualOperation;
import nl.tue.storage.HashOperation;
import nl.tue.storage.Inflater;

import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.model.XEvent;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public interface TreeDelegate<H extends Head, T extends Tail> extends StorageAwareDelegate<H, T> {

	public abstract Inflater<H> getHeadInflater();

	public abstract Deflater<H> getHeadDeflater();

	public abstract void setStateSpace(CompressedHashSet<State<H, T>> statespace);

	public abstract XEventClass getClassOf(XEvent e);

	public abstract short getIndexOf(XEventClass c);

	public abstract short numEventClasses();

	public abstract int getCostFor(int node, int activity);

	public abstract TShortSet getActivitiesFor(int node);

	public abstract XEventClass getEventClass(short act);

	public abstract boolean isLeaf(int modelMove);

	public abstract int getLogMoveCost(int i);

	public abstract int getModelMoveCost(int node);

	public abstract int numNodes();

	public abstract HashOperation<State<H, T>> getHeadBasedHashOperation();

	public abstract EqualOperation<State<H, T>> getHeadBasedEqualOperation();

	public abstract String toString(short modelMove, short activity);

	public abstract int getScaling();

	public abstract ProbProcessArrayTree getTree();

}