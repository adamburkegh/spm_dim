package org.processmining.plugins.etm.model.narytree.visualisation;

import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TObjectShortHashMap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.deckfour.xes.classification.XEventAttributeClassifier;
import org.deckfour.xes.classification.XEventClass;
import org.deckfour.xes.classification.XEventClasses;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.factory.XFactoryNaiveImpl;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.DefaultPort;
import org.jgraph.graph.Edge.Routing;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;
import org.processmining.plugins.etm.model.narytree.test.Test;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

import cern.colt.Arrays;

import com.jgraph.components.labels.CellConstants;
import com.jgraph.components.labels.MultiLineVertexRenderer;

public class TreeLayoutBuilder {

	private static final long serialVersionUID = 1L;
	private final ProbProcessArrayTree tree;
	private final Dim[] dimensions;

	public static enum Type {
		XOR("X"), OR("O"), AND("+"), ILV("<>"), START(""), END("");
		private final String s;

		Type(String s) {
			this.s = s;
		}

		public String toString() {
			return s;
		}
	};

	public static enum EdgeRouting {
		HORIZONTAL_FIRST, VERTICAL_FIRST, DOUBLE;
	}

	public static final int LEAFHEIGHT = 50;
	public static final int LEAFWIDTH = 80;
	public static final int OPERATORHEIGHT = 40;
	public static final int OPERATORWIDTH = 40;
	public static final int EVENTWIDTH = 30;
	public static final int EVENTHEIGHT = 30;
	private static final Routing ROUTING = new OrthogonalRouting();

	public static final int BORDER = 4;
	public static final double SPLINEPOINTFRACTION = .5;
	private final static int STYLE = GraphConstants.STYLE_ORTHOGONAL;
	public static final String EDGEROUTING = "EdgeRouting";
	private final XEventClasses classes;

	//	private final static int STYLE = GraphConstants.STYLE_BEZIER;
	//	private final static int STYLE =  GraphConstants.STYLE_SPLINE;

	private static class Dim {

		public int x = 0;
		public int y = 0;
		public final int width;
		public final int height;
		public final boolean left2right;

		public Dim(int width, int height, boolean left2right) {
			this.width = width;
			this.height = height;
			this.left2right = left2right;
		}

		public String toString() {
			return "[width=" + width + ",height=" + height + ",x=" + x + ",y=" + y + "," + (left2right ? "L2R" : "R2L")
					+ "]";
		}

		public int hashCode() {
			return width + 37 * height + 37 * 37 * x + 37 * 37 * 37 * y;
		}

		public boolean equals(Object o) {
			return (o instanceof Dim) && ((Dim) o).width == width && ((Dim) o).height == height
					&& ((Dim) o).left2right == left2right && ((Dim) o).x == x && ((Dim) o).y == y;
		}

		public void setLocation(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public TreeLayoutBuilder(ProbProcessArrayTree tree) {
		this(tree, null);
	}

	public TreeLayoutBuilder(ProbProcessArrayTree tree, XEventClasses classes) {
		this.tree = tree;
		this.classes = classes;
		this.dimensions = new Dim[tree.size() + 1];
		buildLayout();
	}

	public void buildLayout() {
		getSizeOfNode(0, true);
		setLocation(0, EVENTWIDTH, 0);
		System.out.println("Tree: " + tree);
		System.out.println("Tree dimensions: " + Arrays.toString(dimensions));
	}

	protected Dim getSizeOfNode(int node, boolean left2right) {
		switch (tree.getType(node)) {
			case ProbProcessArrayTree.LOOP :
				dimensions[node] = getSizeLoop(node, left2right);
				break;
			case ProbProcessArrayTree.REVSEQ :
			case ProbProcessArrayTree.SEQ :
				dimensions[node] = getSizeSeq(node, left2right);
				break;
			case ProbProcessArrayTree.AND :
			case ProbProcessArrayTree.ILV :
			case ProbProcessArrayTree.OR :
			case ProbProcessArrayTree.XOR :
				dimensions[node] = getSizeOther(node, left2right);
				break;
			default :
				dimensions[node] = getSizeLeaf(node, left2right);
		}
		if (node == 0) {
			dimensions[tree.size()] = new Dim(dimensions[0].width + 2 * EVENTWIDTH, dimensions[0].height,
					dimensions[0].left2right);
		}
		return dimensions[node];
	}

	protected Dim getSizeSeq(final int seqNode, final boolean left2right) {
		int width = 0;
		int height = 0;

		int ch = seqNode + 1;
		Dim d;
		do {
			d = getSizeOfNode(ch, left2right);
			width += d.width;
			if (d.height > height) {
				height = d.height;
			}
			ch = tree.getNextFast(ch);
		} while (tree.getParent(ch) == seqNode);
		return new Dim(width, height, left2right);
	}

	protected Dim getSizeOther(final int otherNode, final boolean left2right) {
		int width = 0;
		int height = 0;

		int ch = otherNode + 1;
		Dim d;
		do {
			d = getSizeOfNode(ch, left2right);
			height += d.height;
			if (d.width > width) {
				width = d.width;
			}
			ch = tree.getNextFast(ch);
		} while (tree.getParent(ch) == otherNode);
		return new Dim(width + 2 * OPERATORWIDTH, height, left2right);
	}

	protected Dim getSizeLeaf(final int seqNode, final boolean left2right) {
		//		Random r = new Random();
		//		return new Dim(LEAFWIDTH / 2 + r.nextInt(LEAFWIDTH / 2), LEAFHEIGHT / 2 + r.nextInt(LEAFHEIGHT / 2), left2right);
		return new Dim(LEAFWIDTH, LEAFHEIGHT, left2right);
	}

	protected Dim getSizeLoop(final int loopNode, final boolean left2right) {
		int width = 2 * OPERATORWIDTH;
		int height = 0;

		Dim leftDim = getSizeOfNode(loopNode + 1, left2right);
		Dim middleDim = getSizeOfNode(tree.getNextFast(loopNode + 1), !left2right);
		Dim rightDim = getSizeOfNode(tree.getNextFast(tree.getNextFast(loopNode + 1)), left2right);

		width += rightDim.width;
		width += Math.max(leftDim.width, middleDim.width);

		height = Math.max(rightDim.height, middleDim.height + leftDim.height);

		return new Dim(width, height, left2right);
	}

	protected void setLocation(final int node, final int x, final int y) {
		// set the location of this node
		dimensions[node].setLocation(x, y);
		switch (tree.getType(node)) {
			case ProbProcessArrayTree.LOOP :
				setLocationLoop(node, x, y);
				break;
			case ProbProcessArrayTree.REVSEQ :
			case ProbProcessArrayTree.SEQ :
				setLocationSeq(node, x, y);
				break;
			case ProbProcessArrayTree.AND :
			case ProbProcessArrayTree.ILV :
			case ProbProcessArrayTree.OR :
			case ProbProcessArrayTree.XOR :
				setLocationOther(node, x, y);
				break;
			default :
				setLocationLeaf(node, x, y);
		}

	}

	protected void setLocationLeaf(final int leafNode, final int x, final int y) {
		// skip;
	}

	protected void setLocationLoop(final int loopNode, final int x, final int y) {
		// set the location for this node's children
		int left = loopNode + 1;
		int middle = tree.getNextFast(left);
		int right = tree.getNextFast(middle);

		int h1, h2;
		if (dimensions[right].height < dimensions[left].height + dimensions[middle].height) {
			h1 = 0;
			h2 = (dimensions[left].height + dimensions[middle].height - dimensions[right].height) / 2;
		} else {
			h2 = 0;
			h1 = (dimensions[right].height - dimensions[left].height - dimensions[middle].height) / 2;
		}

		if (dimensions[loopNode].left2right) {
			if (dimensions[left].width < dimensions[middle].width) {
				setLocation(left, //
						x + OPERATORWIDTH + (dimensions[middle].width - dimensions[left].width) / 2,// 
						y + h1);
				setLocation(middle,//
						x + OPERATORWIDTH,//
						y + h1 + dimensions[left].height);
				setLocation(right, //
						x + 2 * OPERATORWIDTH + dimensions[middle].width, //
						y + h2);
			} else {
				setLocation(left, //
						x + OPERATORWIDTH, // 
						y + h1);
				setLocation(middle,//
						x + OPERATORWIDTH + (dimensions[left].width - dimensions[middle].width) / 2,//
						y + dimensions[left].height + h1);
				setLocation(right, //
						x + 2 * OPERATORWIDTH + dimensions[left].width, //
						y + h2);
			}
		} else {
			int wx = OPERATORWIDTH + dimensions[right].width;
			if (dimensions[left].width < dimensions[middle].width) {
				setLocation(left, //
						x + wx + (dimensions[middle].width - dimensions[left].width) / 2,// 
						y + h1);
				setLocation(middle,//
						x + wx,//
						y + dimensions[left].height + h1);
				setLocation(right, //
						x, //
						y + h2);
			} else {
				setLocation(left, //
						x + wx, // 
						y + h1);
				setLocation(middle,//
						x + wx + (dimensions[left].width - dimensions[middle].width) / 2,//
						y + dimensions[left].height + h1);
				setLocation(right, //
						x, //
						y + h2);
			}
		}
	}

	protected void setLocationSeq(final int seqNode, int x, final int y) {
		if (!dimensions[seqNode].left2right) {
			// find the last child.
			int ch = seqNode + 1;
			int w = 0;
			do {
				w += dimensions[ch].width;
				ch = tree.getNextFast(ch);
			} while (tree.getParent(ch) == seqNode);
			x += w;

		}

		int ch = seqNode + 1;
		do {
			if (!dimensions[seqNode].left2right) {
				x -= dimensions[ch].width;
			}
			setLocation(ch, x, y + (dimensions[seqNode].height - dimensions[ch].height) / 2);
			if (dimensions[seqNode].left2right) {
				x += dimensions[ch].width;
			}
			ch = tree.getNextFast(ch);
		} while (tree.getParent(ch) == seqNode);
	}

	protected void setLocationOther(final int otherNode, int x, int y) {
		// order of the nodes does not matter. Can we use that?

		int ch = otherNode + 1;
		x += OPERATORWIDTH;
		do {
			setLocation(ch, x + (dimensions[otherNode].width - 2 * OPERATORWIDTH - dimensions[ch].width) / 2, y);
			y += dimensions[ch].height;
			ch = tree.getNextFast(ch);
		} while (tree.getParent(ch) == otherNode);
	}

	public JGraph getJGraph() {
		GraphModel model = new DefaultGraphModel();
		GraphLayoutCache view = new GraphLayoutCache(model, new TreeCellViewFactory());
		JGraph graph = new JGraph(model, view);
		graph.setAntiAliased(true);
		//graph.setScale(1.0);
		drawNode(graph.getGraphLayoutCache(), tree.size());
		return graph;
	}

	protected DefaultPort[] drawNode(GraphLayoutCache g, int node) {
		if (node == tree.size()) {
			// ensure the entire tree is drawn first
			DefaultPort[] ports = drawNode(g, 0);

			DefaultPort inPort = drawOperator(g, 0, (dimensions[tree.size()].height - EVENTHEIGHT) / 2, Type.START);
			DefaultPort outPort = drawOperator(g, dimensions[tree.size()].width - EVENTWIDTH,
					(dimensions[tree.size()].height - EVENTHEIGHT) / 2, Type.END);
			drawEdge(g, inPort, null, ports[0], null, EdgeRouting.VERTICAL_FIRST);
			drawEdge(g, ports[1], null, outPort, null, EdgeRouting.HORIZONTAL_FIRST);
			return new DefaultPort[] { inPort, outPort };
		}

		// set the location of this node
		switch (tree.getType(node)) {
			case ProbProcessArrayTree.LOOP :
				return drawNodeLoop(g, node);
			case ProbProcessArrayTree.REVSEQ :
			case ProbProcessArrayTree.SEQ :
				return drawNodeSeq(g, node);

			case ProbProcessArrayTree.AND :
			case ProbProcessArrayTree.OR :
			case ProbProcessArrayTree.ILV :
			case ProbProcessArrayTree.XOR :
				return drawNodeOther(g, node);

			default :
				return drawNodeLeaf(g, node);
		}

	}

	protected DefaultPort[] drawNodeOther(GraphLayoutCache g, int node) {
		Dim d = dimensions[node];

		Type t;
		if (tree.getTypeFast(node) == ProbProcessArrayTree.AND) {
			t = Type.AND;
		} else if (tree.getTypeFast(node) == ProbProcessArrayTree.ILV) {
			t = Type.ILV;
		} else if (tree.getTypeFast(node) == ProbProcessArrayTree.OR) {
			t = Type.OR;
		} else { // (tree.getTypeFast(node) == NAryTree.XOR) 
			t = Type.XOR;
		}

		DefaultPort portIn, portOut;
		if (d.left2right) {
			portIn = drawOperator(g, d.x, d.y + (d.height - OPERATORHEIGHT) / 2, t);
			portOut = drawOperator(g, d.x + d.width - OPERATORWIDTH, d.y + (d.height - OPERATORHEIGHT) / 2, t);
		} else {
			portOut = drawOperator(g, d.x, d.y + (d.height - OPERATORHEIGHT) / 2, t);
			portIn = drawOperator(g, d.x + d.width - OPERATORWIDTH, d.y + (d.height - OPERATORHEIGHT) / 2, t);
		}

		DefaultPort[] ports;
		int ch = node + 1;
		do {
			ports = drawNode(g, ch);
			int w1 = dimensions[ch].x - (d.x + OPERATORWIDTH / 2);
			int w2 = (d.x + d.width - OPERATORWIDTH / 2) - (dimensions[ch].x + dimensions[ch].width);
			int y = dimensions[ch].y + dimensions[ch].height / 2;
			double x1, x2;
			if (y != d.y + d.height / 2) {
				if (d.left2right) {
					x1 = d.x + OPERATORWIDTH / 2 + SPLINEPOINTFRACTION * w1;
					x2 = d.x + d.width - OPERATORWIDTH / 2 - SPLINEPOINTFRACTION * w2;
				} else {
					x2 = d.x + OPERATORWIDTH / 2 + SPLINEPOINTFRACTION * w1;
					x1 = d.x + d.width - OPERATORWIDTH / 2 - SPLINEPOINTFRACTION * w2;
				}
				drawEdge(g, portIn, null, ports[0], new Point2D.Double(x1, y), EdgeRouting.VERTICAL_FIRST);
				drawEdge(g, ports[1], new Point2D.Double(x2, y), portOut, null, EdgeRouting.HORIZONTAL_FIRST);
			} else {
				drawEdge(g, portIn, null, ports[0], null, EdgeRouting.VERTICAL_FIRST);
				drawEdge(g, ports[1], null, portOut, null, EdgeRouting.HORIZONTAL_FIRST);
			}
			ch = tree.getNextFast(ch);
		} while (tree.getParent(ch) == node);

		return new DefaultPort[] { portIn, portOut };
	}

	protected DefaultPort[] drawNodeLeaf(GraphLayoutCache g, int node) {
		Dim d = dimensions[node];

		DefaultGraphCell graphCell;
		if (classes == null) {
			graphCell = new DefaultGraphCell(TreeUtils.toString(tree, node));
		} else {
			if (tree.getTypeFast(node) == ProbProcessArrayTree.TAU) {
				graphCell = new DefaultGraphCell("skip");
			} else {
				graphCell = new DefaultGraphCell(classes.getByIndex(tree.getTypeFast(node)));
			}
		}
		GraphConstants.setBounds(graphCell.getAttributes(), new Rectangle2D.Double(d.x + BORDER, d.y + BORDER, d.width
				- 2 * BORDER, d.height - 2 * +BORDER));
		GraphConstants.setBorder(graphCell.getAttributes(), BorderFactory.createLineBorder(Color.BLACK, 2));
		GraphConstants.setConnectable(graphCell.getAttributes(), false);
		GraphConstants.setDisconnectable(graphCell.getAttributes(), false);
		CellConstants.setVertexShape(graphCell.getAttributes(), MultiLineVertexRenderer.SHAPE_ROUNDED);
		CellConstants.setLineWidth(graphCell.getAttributes(), 1);

		DefaultPort inPort = new DefaultPort();
		graphCell.add(inPort);
		inPort.setParent(graphCell);

		DefaultPort outPort = new DefaultPort();
		graphCell.add(outPort);
		outPort.setParent(graphCell);

		GraphConstants.setOffset(inPort.getAttributes(), new Point2D.Double(
				(d.left2right ? 0 : GraphConstants.PERMILLE), GraphConstants.PERMILLE / 2));
		GraphConstants.setOffset(outPort.getAttributes(), new Point2D.Double((d.left2right ? GraphConstants.PERMILLE
				: 0), GraphConstants.PERMILLE / 2));

		g.insert(graphCell);
		return new DefaultPort[] { inPort, outPort };
	}

	protected DefaultPort[] drawNodeLoop(GraphLayoutCache g, int loopNode) {
		Dim d = dimensions[loopNode];
		int left = loopNode + 1;
		int middle = tree.getNextFast(left);
		int right = tree.getNextFast(middle);

		int xi1, xi2;
		if (d.left2right) {
			xi1 = d.x;
			xi2 = d.x + d.width - dimensions[right].width - OPERATORWIDTH;
		} else {
			xi1 = d.x + d.width - OPERATORWIDTH;
			xi2 = d.x + dimensions[right].width;
		}
		int cy = d.y + (d.height - OPERATORHEIGHT) / 2;

		DefaultPort port1 = drawOperator(g, xi1, cy, Type.XOR);
		DefaultPort port2 = drawOperator(g, xi2, cy, Type.XOR);

		DefaultPort[] leftPorts = drawNode(g, left);
		DefaultPort[] middlePorts = drawNode(g, middle);
		DefaultPort[] rightPorts = drawNode(g, right);

		int rw = dimensions[right].width;
		double x1, x2, y;
		if (d.left2right) {
			int w1 = dimensions[left].x - (d.x + OPERATORWIDTH / 2);
			int w2 = (d.x + d.width - rw - OPERATORWIDTH / 2) - (dimensions[left].x + dimensions[left].width);
			x1 = d.x + OPERATORWIDTH / 2 + SPLINEPOINTFRACTION * w1;
			x2 = d.x + d.width - rw - OPERATORWIDTH / 2 - SPLINEPOINTFRACTION * w2;
			y = dimensions[left].y + dimensions[left].height / 2;
		} else {
			int w1 = dimensions[left].x - (d.x + rw + OPERATORWIDTH / 2);
			int w2 = (d.x + d.width - OPERATORWIDTH / 2) - (dimensions[left].x + dimensions[left].width);
			x2 = d.x + rw + OPERATORWIDTH / 2 + SPLINEPOINTFRACTION * w1;
			x1 = d.x + d.width - OPERATORWIDTH / 2 - SPLINEPOINTFRACTION * w2;
			y = dimensions[left].y + dimensions[left].height / 2;
		}
		drawEdge(g, port1, null, leftPorts[0], new Point2D.Double(x1, y), EdgeRouting.VERTICAL_FIRST);
		drawEdge(g, leftPorts[1], new Point2D.Double(x2, y), port2, null, EdgeRouting.HORIZONTAL_FIRST);

		if (d.left2right) {
			int w1 = dimensions[middle].x - (d.x + OPERATORWIDTH / 2);
			int w2 = (d.x + d.width - rw - OPERATORWIDTH / 2) - (dimensions[middle].x + dimensions[middle].width);
			x1 = d.x + OPERATORWIDTH / 2 + SPLINEPOINTFRACTION * w1;
			x2 = d.x + d.width - rw - OPERATORWIDTH / 2 - SPLINEPOINTFRACTION * w2;
			y = dimensions[middle].y + dimensions[middle].height / 2;
		} else {
			int w1 = dimensions[middle].x - (d.x + rw + OPERATORWIDTH / 2);
			int w2 = (d.x + d.width - OPERATORWIDTH / 2) - (dimensions[middle].x + dimensions[middle].width);
			x2 = d.x + rw + OPERATORWIDTH / 2 + SPLINEPOINTFRACTION * w1;
			x1 = d.x + d.width - OPERATORWIDTH / 2 - SPLINEPOINTFRACTION * w2;
			y = dimensions[middle].y + dimensions[middle].height / 2;
		}
		drawEdge(g, port2, null, middlePorts[0], new Point2D.Double(x2, y), EdgeRouting.VERTICAL_FIRST);
		drawEdge(g, middlePorts[1], new Point2D.Double(x1, y), port1, null, EdgeRouting.HORIZONTAL_FIRST);

		drawEdge(g, port2, null, rightPorts[0], null, EdgeRouting.HORIZONTAL_FIRST);

		return new DefaultPort[] { port1, rightPorts[1] };
	}

	protected DefaultPort drawOperator(GraphLayoutCache g, int x, int y, Type type) {

		DefaultGraphCell graphCell = new DefaultGraphCell(type);
		GraphConstants.setSizeable(graphCell.getAttributes(), false);
		GraphConstants.setResize(graphCell.getAttributes(), false);
		GraphConstants.setBounds(graphCell.getAttributes(), new Rectangle2D.Double(x + BORDER, y + BORDER,
				OPERATORWIDTH - 2 * BORDER, OPERATORHEIGHT - 2 * BORDER));
		DefaultPort port = new DefaultPort();
		graphCell.add(port);
		port.setParent(graphCell);
		GraphConstants.setBorder(graphCell.getAttributes(), BorderFactory.createLineBorder(Color.BLACK, 2));
		GraphConstants.setConnectable(graphCell.getAttributes(), false);
		GraphConstants.setDisconnectable(graphCell.getAttributes(), false);
		if (type == Type.END || type == Type.START) {
			GraphConstants.setBounds(graphCell.getAttributes(), new Rectangle2D.Double(x + BORDER, y + BORDER,
					EVENTWIDTH - 2 * BORDER, EVENTWIDTH - 2 * BORDER));
			CellConstants.setVertexShape(graphCell.getAttributes(), MultiLineVertexRenderer.SHAPE_CIRCLE);
		} else {
			CellConstants.setVertexShape(graphCell.getAttributes(), MultiLineVertexRenderer.SHAPE_DIAMOND);
		}
		if (type == Type.END) {
			CellConstants.setLineWidth(graphCell.getAttributes(), 3);
		} else {
			CellConstants.setLineWidth(graphCell.getAttributes(), 1);
		}
		g.insert(graphCell);
		return port;
	}

	@SuppressWarnings("unchecked")
	protected void drawEdge(GraphLayoutCache g, DefaultPort from, Point2D fromPoint, DefaultPort to, Point2D toPoint,
			EdgeRouting startHorizontal) {
		DefaultEdge edge;
		edge = new DefaultEdge();

		//		List<Point2D> list = new ArrayList<Point2D>(4);
		//		list.add(edge.getAttributes().createPoint(10, 10));
		//
		//		if (fromPoint != null) {
		//			list.add(fromPoint);
		//		}
		//		if (toPoint != null) {
		//			list.add(toPoint);
		//		}
		//
		//		list.add(edge.getAttributes().createPoint(30, 30));
		//		GraphConstants.setPoints(edge.getAttributes(), list);

		edge.setSource(from);
		edge.setTarget(to);

		edge.getAttributes().put(EDGEROUTING, startHorizontal);
		GraphConstants.setLineEnd(edge.getAttributes(), GraphConstants.ARROW_TECHNICAL);
		GraphConstants.setEndSize(edge.getAttributes(), 5);
		GraphConstants.setEndFill(edge.getAttributes(), true);
		GraphConstants.setConnectable(edge.getAttributes(), false);
		GraphConstants.setDisconnectable(edge.getAttributes(), false);
		GraphConstants.setLineWidth(edge.getAttributes(), 1);

		GraphConstants.setRouting(edge.getAttributes(), ROUTING);
		//GraphConstants.setRouting(edge.getAttributes(), GraphConstants.ROUTING_DEFAULT);

		GraphConstants.setLineStyle(edge.getAttributes(), STYLE);
		g.insert(edge);

		//System.out.println(GraphConstants.getPoints(edge.getAttributes()));

	}

	protected DefaultPort[] drawNodeSeq(GraphLayoutCache g, int node) {
		DefaultPort[] ports;
		DefaultPort in = null;
		DefaultPort out = null;
		int ch = node + 1;
		do {
			ports = drawNode(g, ch);
			if (in == null) {
				in = ports[0];
			} else {
				drawEdge(g, out, null, ports[0], null, EdgeRouting.DOUBLE);
			}
			out = ports[1];
			ch = tree.getNextFast(ch);
		} while (tree.getParent(ch) == node);

		return new DefaultPort[] { in, out };
	}

	public static void main(String[] args) {
		String A = "A", B = "B", C = "C", D = "D", E = "E";
		XLog log;
		XFactoryRegistry.instance().setCurrentDefault(new XFactoryNaiveImpl());
		log = LogCreator.createLog(new String[][] { { A, B, C, D, E } });
		//log = LogCreator.createLog(new String[][] { { A, B, C, D } });

		XLogInfo info = XLogInfoFactory.createLogInfo(log, new XEventAttributeClassifier("Name",
				XConceptExtension.KEY_NAME));
		final XEventClasses classes = info.getEventClasses();
		TObjectShortMap<String> map = new TObjectShortHashMap<String>();
		// initialize a tree with all node costs 5 for leafs and 0 otherwise.
		short i = 0;
		for (XEventClass clazz : classes.getClasses()) {
			map.put(clazz.toString(), i++);
		}
		System.out.println(map);

		ProbProcessArrayTree tree;
		tree = Test.getTree(map, new Random(), 20);
		//		tree = TreeUtils
		//				.fromString("LOOP( SEQ( LEAF: A , LEAF: B ) , SEQ( LEAF: C , LEAF: D ) , OR( LEAF: E, LEAF: F , LEAF: G , LEAF: H ) )");
		//tree = TreeUtils
		//	.fromString("LOOP( LEAF: 4 , LOOP( SEQ( LEAF: 1 , LEAF: 4 , LOOP( LEAF: 1 , LEAF: 2 , LOOP( AND( LEAF: tau , LEAF: 4 , LEAF: 4 , LEAF: tau ) , LEAF: 4 , OR( LEAF: 2 , LOOP( LEAF: 4 , LEAF: 2 , LEAF: 3 ) ) ) ) ) , LEAF: 2 , LEAF: 0 ) ,  AND( XOR( LEAF: 4 , LEAF: 1 , LEAF: 1 , OR( LEAF: 3 , LEAF: 3 , LEAF: tau , OR( LEAF: 2 , LEAF: 4 ) ) ) , LEAF: 4 , LEAF: 1 , LEAF: 0 ) )");
		//tree = TreeUtils.fromString("LOOP( LEAF: A , LEAF: B , LEAF: C )");
		//tree = TreeUtils
		//		.fromString("LOOP( OR( LEAF: A , LEAF: B , LEAF: C ) , AND( LEAF: A , LEAF: B , LEAF: C ) , XOR( LEAF: A , LEAF: B , LEAF: C ) )");
		//tree = TreeUtils.fromString("LEAF:( C )", map);

		TreeLayoutBuilder builder = new TreeLayoutBuilder(tree, classes);
		JFrame frame = new JFrame("Test of TreeBuilder");
		JGraph graph = builder.getJGraph();
		frame.add(new JScrollPane(graph));
		frame.setSize(builder.getDimension(tree.size()));
		frame.setPreferredSize(builder.getDimension(tree.size()));
		frame.pack();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);

	}

	protected Dimension getDimension(int node) {
		return new Dimension(dimensions[node].width + 40, dimensions[node].height + 50);
	}

}
