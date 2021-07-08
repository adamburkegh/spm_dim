package org.processmining.plugins.etm.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.in.XUniversalParser;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.XLogInfoFactory;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.model.narytree.conversion.ProcessTreeToNAryTree;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.ptml.importing.PtmlImportTree;

public class PTConversionTests {

	public static void main(String[] args) {
		testSanderConversionBug();
	}

	/**
	 * Test code for fixing bug found by sander 4-4-2014
	 */
	public static void testSanderConversionBug() {
		String basedir = "C:\\Users\\jbuijs\\Documents\\PhD\\Projects\\Small things\\000Simple test logs";
		String logFileName = basedir
				+ "Sander_a12 seq(s, par(seq(f, par(h, seq(g, i)), k), seq(b, xor(d, seq(c, e)), j)), e).xes";
		String modelFileName = basedir + "PTtoNATconversionBugtree.ptml";
		XEventClassifier classifier = XLogInfoImpl.NAME_CLASSIFIER;

		XUniversalParser parser = new XUniversalParser();
		XLog log = null;
		try {
			log = parser.parse(new File(logFileName)).iterator().next();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		XLogInfo logInfo = XLogInfoFactory.createLogInfo(log, classifier);

		try {
			PtmlImportTree importer = new PtmlImportTree();
			InputStream inStream = new FileInputStream(modelFileName);
			importer.importPtmlFromStream(null, inStream, modelFileName, -1);
			ProcessTree pt = (ProcessTree) importer.importFile(null, basedir + "PTtoNATconversionBugtree.ptml");

			System.out.println("PT: " + pt.toString());

			ProcessTreeToNAryTree ptToNat = new ProcessTreeToNAryTree(logInfo.getEventClasses());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
