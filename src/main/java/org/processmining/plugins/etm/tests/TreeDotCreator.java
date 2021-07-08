package org.processmining.plugins.etm.tests;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.deckfour.xes.classification.XEventNameClassifier;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

public class TreeDotCreator {

	public static void main(String[] args) {
		String basedir = "e://temp//dotPlots//";
		
		XLog log = LogCreator.createLog(new String[][] { { "A", "B", "C", "D","E" } });
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, new XEventNameClassifier());
		ProbProcessArrayTree tree = TreeUtils.fromString("SEQ( XOR( LEAF: A , LEAF: B ) , AND( LEAF: C , LEAF: D , LEAF: E ) ) [ [ -, -, -, -, -, -, -, ] ]", logInfo.getEventClasses());

		try {
			OutputStreamWriter out;
			out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(new File(basedir+"tree.dot"))));
			TreeUtils.writeTreeToDot(tree, 0, out);
			out.flush();
			out.close();
			System.out.println();

			out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(new File(
					basedir+"behavior-compressed.dot"))));
			TreeUtils.writeBehaviorToDot(tree, 0, out, true);
			out.flush();
			out.close();
			System.out.println();

			out = new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(new File(
					basedir+"behavior.dot"))));
			TreeUtils.writeBehaviorToDot(tree, 0, out, false);
			out.flush();
			out.close();
			System.out.println();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
