package org.processmining.plugins.etm.model.narytree.visualisation;

import java.awt.geom.Point2D;

import org.jgraph.graph.CellViewRenderer;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.VertexView;

public class OperatorView extends VertexView {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3412814307616951167L;
	protected static final TreeCellRenderer renderer = new TreeCellRenderer();

	public OperatorView(Object cell) {
		super(cell);
	}

	/**
	 * Returns a renderer for the class.
	 */
	public CellViewRenderer getRenderer() {
		return renderer;
	}

	public Point2D getPerimeterPoint(EdgeView edge, Point2D source, Point2D p) {
		return renderer.getPerimeterPoint(this, source, p);
	}

}
