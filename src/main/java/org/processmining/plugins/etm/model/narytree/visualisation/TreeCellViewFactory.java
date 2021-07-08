package org.processmining.plugins.etm.model.narytree.visualisation;

import org.jgraph.graph.AbstractCellView;
import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultCellViewFactory;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphModel;
import org.processmining.plugins.etm.model.narytree.visualisation.TreeLayoutBuilder.Type;

import com.jgraph.components.labels.MultiLineVertexView;

public class TreeCellViewFactory extends DefaultCellViewFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7288009143327540248L;

	public TreeCellViewFactory() {
	}

	@Override
	public CellView createView(GraphModel model, Object cell) {
		CellView view = null;
		if (model.isPort(cell))
			view = createPortView(cell);
		else if (model.isEdge(cell))
			view = createEdgeView(cell);
		else
			view = createTheVertexView(cell);
		return view;
	}

	protected AbstractCellView createTheVertexView(Object cell) {
		if (((DefaultGraphCell) cell).getUserObject() instanceof Type) {
			// operator
			return new OperatorView(cell);
		}
		return new MultiLineVertexView(cell);
	}

	@Override
	protected EdgeView createEdgeView(Object edge) {
		EdgeView view = super.createEdgeView(edge);
		return view;

	}

}
