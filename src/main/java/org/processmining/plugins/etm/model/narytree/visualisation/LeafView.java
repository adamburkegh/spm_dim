package org.processmining.plugins.etm.model.narytree.visualisation;

import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.CellHandle;
import org.jgraph.graph.CellViewRenderer;
import org.jgraph.graph.GraphContext;
import org.jgraph.graph.VertexRenderer;

public class LeafView extends AbstractCellView {

	private static final long serialVersionUID = 7853071042070988578L;

	protected static final VertexRenderer renderer = new VertexRenderer();

	public LeafView(Object cell) {
		super(cell);
	}

	public CellHandle getHandle(GraphContext arg0) {
		return null;
	}

	public CellViewRenderer getRenderer() {
		return renderer;
	}

}
