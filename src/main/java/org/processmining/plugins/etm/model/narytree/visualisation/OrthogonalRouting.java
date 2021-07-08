package org.processmining.plugins.etm.model.narytree.visualisation;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.EdgeView;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.PortView;
import org.processmining.plugins.etm.model.narytree.visualisation.TreeLayoutBuilder.EdgeRouting;

public class OrthogonalRouting extends DefaultEdge.DefaultRouting {

	private static final long serialVersionUID = -1806452010692814216L;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public List route(GraphLayoutCache cache, EdgeView edge) {
		EdgeRouting startHorizontal = (EdgeRouting) edge.getAllAttributes().get(TreeLayoutBuilder.EDGEROUTING);
		if (startHorizontal == EdgeRouting.DOUBLE) {
			return super.route(cache, edge);
		}

		List newPoints = new ArrayList();
		int n = edge.getPointCount();
		Point2D from = edge.getPoint(0);
		newPoints.add(from);
		if (edge.getSource() instanceof PortView) {
			newPoints.set(0, edge.getSource());
			from = ((PortView) edge.getSource()).getLocation();
		} else if (edge.getSource() != null) {
			Rectangle2D b = edge.getSource().getBounds();
			from = edge.getAttributes().createPoint(b.getCenterX(), b.getCenterY());
		}
		Point2D to = edge.getPoint(n - 1);
		CellView target = edge.getTarget();
		if (target instanceof PortView)
			to = ((PortView) target).getLocation();
		else if (target != null) {
			Rectangle2D b = target.getBounds();
			to = edge.getAttributes().createPoint(b.getCenterX(), b.getCenterY());
		}
		if (from != null && to != null) {

			Point2D routed;

			if (startHorizontal == EdgeRouting.HORIZONTAL_FIRST) {
				routed = edge.getAttributes().createPoint(to.getX(), from.getY());
			} else {
				routed = edge.getAttributes().createPoint(from.getX(), to.getY());
			}

			Rectangle2D targetBounds = null;
			Rectangle2D sourceBounds = null;
			if ((edge.getTarget() != null && edge.getTarget().getParentView() != null)
					&& (edge.getSource() != null && edge.getSource().getParentView() != null)) {
				targetBounds = edge.getTarget().getParentView().getBounds();
				sourceBounds = edge.getSource().getParentView().getBounds();
				if (!targetBounds.contains(routed) && (!sourceBounds.contains(routed))) {
					newPoints.add(routed);
				}
			}
			// Add target point
			if (target != null)
				newPoints.add(target);
			else
				newPoints.add(to);
			return newPoints;
		}
		return null;
	}

	public int getPreferredLineStyle(EdgeView edge) {
		return GraphConstants.STYLE_ORTHOGONAL;
	}

}
