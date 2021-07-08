package org.processmining.plugins.etm.model.narytree.visualisation;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.util.StringTokenizer;

import org.jgraph.graph.CellView;
import org.jgraph.graph.DefaultGraphCell;
import org.processmining.plugins.etm.model.narytree.visualisation.TreeLayoutBuilder.Type;

import com.jgraph.components.labels.MultiLineVertexRenderer;

public class TreeCellRenderer extends MultiLineVertexRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4910262989128580150L;
	protected Type type;

	@Override
	protected void paintComponent(Graphics g) {

		Graphics2D g2d = (Graphics2D) g;

		GeneralPath gatewayDecorator = new GeneralPath();
		double s = (getWidth() - 2.0 * borderWidth) / 33.;
		switch (type) {
			case AND :
				drawParallel(g2d, gatewayDecorator, s);
				break;
			case END :
				break;
			case OR :
				drawInclusive(g2d, gatewayDecorator, s);
				break;
			case START :
				break;
			case XOR :
				drawExclusive(g2d, gatewayDecorator, s);
				break;

		}
		//super.paintComponent(g);

		AffineTransform at = new AffineTransform();
		//at.scale(s, s);
		//gatewayDecorator.transform(at);
		//at = new AffineTransform();
		at.translate(borderWidth, borderWidth);
		gatewayDecorator.transform(at);

		g.setColor(Color.BLACK);
		g2d.draw(gatewayDecorator);
		if (type == Type.XOR || type == Type.AND) {
			g2d.fill(gatewayDecorator);
		} else {
			g2d.draw(gatewayDecorator);
		}
	}

	public void installAttributes(CellView view) {
		super.installAttributes(view);
		type = (Type) ((DefaultGraphCell) view.getCell()).getUserObject();
	}

	private void drawExclusive(Graphics2D g2d, GeneralPath gatewayDecorator, double s) {

		parse(g2d, gatewayDecorator, "M 11.5 9.5 L 13.5 9.5 L 20.5 22.5 L 18.5 22.5 L 11.5 9.5", s);
		parse(g2d, gatewayDecorator, "M 11.5 22.5 L 18.5 9.5 L 20.5 9.5 L 13.5 22.5 L 11.5 22.5", s);
		g2d.setStroke(new BasicStroke(1F));

	}

	private void parse(Graphics2D g2d, GeneralPath gatewayDecorator, String path, double s) {
		StringTokenizer tok = new StringTokenizer(path, " ,");
		while (tok.hasMoreTokens()) {
			String op = tok.nextToken();
			double x = Double.parseDouble(tok.nextToken());
			double y = Double.parseDouble(tok.nextToken());

			if (op.equals("M")) {
				gatewayDecorator.moveTo(s * x, s * y);
			} else {
				gatewayDecorator.lineTo(s * x, s * y);
			}
		}
	}

	@SuppressWarnings("unused")
	private void drawEventbased(Graphics2D g2d, GeneralPath gatewayDecorator, double s) {
		gatewayDecorator.append(new Ellipse2D.Double(7.5F, 7.5F, 17F, 17F), false);
		gatewayDecorator.append(new Ellipse2D.Double(5F, 5F, 22F, 22F), false);
		gatewayDecorator.moveTo(20.327514F, 21.344972F);
		gatewayDecorator.lineTo(11.259248F, 21.344216F);
		gatewayDecorator.lineTo(9.4577203F, 13.719549F);
		gatewayDecorator.lineTo(15.794545F, 9.389969F);
		gatewayDecorator.lineTo(22.130481F, 13.720774F);
		gatewayDecorator.closePath();
	}

	private void drawInclusive(Graphics2D g2d, GeneralPath gatewayDecorator, double s) {
		gatewayDecorator.append(new Ellipse2D.Double(s * 8.5, s * 8.5, s * 15, s * 15), false);
		g2d.setStroke(new BasicStroke((float) (s * 2.5)));
	}

	@SuppressWarnings("unused")
	private void drawComplex(Graphics2D g2d, GeneralPath gatewayDecorator, double s) {

		parse(g2d, gatewayDecorator,
				"M 8.5,16 L 23.5,16 M 16,8.5 L 16,23.5	M 10.5,10.5 L 21.5,21.5	M 10.5,21.5 L 21.5,10.5", s);
		g2d.setStroke(new BasicStroke((float) s * 3F));
	}

	private void drawParallel(Graphics2D g2d, GeneralPath gatewayDecorator, double s) {

		parse(g2d,
				gatewayDecorator,
				"M 8.5 15 L 8.5 17 L 15 17 L 15 23.5 L 17 23.5 L 17 17 L 23.5 17 L 23.5 15 L 17 15 L 17 8.5 L 15 8.5 L 15 15 L 8.5 15",
				s);

		//parse(g2d, gatewayDecorator, "M 8.5,16 L 23.5,16 M 16,8.5 L 16,23.5", s);
		//g2d.setStroke(new BasicStroke((float) s * 3F));
	}

	/*-
	<circle
		id="frame" cx="16" cy="16" r="15"
		stroke="black" fill="white" stroke-width="1"
	/>	 
	 */
	// startEvent

	/*-
	 <circle
		id="frame" cx="16" cy="16" r="15"
		stroke="black" fill="white" stroke-width="1"
	/>
	<circle
		id="inner" cx="16" cy="16" r="12"
		stroke="black" fill="none" stroke-width="1"
	/>
	 */

}
