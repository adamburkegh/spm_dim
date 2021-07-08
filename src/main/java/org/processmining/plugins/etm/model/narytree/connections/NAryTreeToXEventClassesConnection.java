package org.processmining.plugins.etm.model.narytree.connections;

import org.deckfour.xes.classification.XEventClasses;
import org.processmining.framework.connections.impl.AbstractStrongReferencingConnection;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;

public class NAryTreeToXEventClassesConnection extends AbstractStrongReferencingConnection {

	public static final String NARYTREE = "NARYTREE";

	public static final String XEVENTCLASSES = "XEVENTCLASSES";

	public NAryTreeToXEventClassesConnection(ProbProcessArrayTree tree, XEventClasses classes) {
		super("NAryTree to XEventClasses");
		put(NARYTREE, tree);
		putStrong(XEVENTCLASSES, classes);
	}

}
