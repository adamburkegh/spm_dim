package org.processmining.plugins.etm.model.narytree.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension.StandardModel;
import org.deckfour.xes.factory.XFactoryRegistry;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.out.XMxmlSerializer;

public class LogCreator {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws IOException {

		String folderName = "C://Users//bfvdonge//Documents//My Dropbox//Boudewijn Werk//Research//Papers//2011 ToPNoC ACPN//";
		File folder = new File(folderName);
		for (File f : folder.listFiles()) {
			String newName = f.getName();
			if (f.getName().contains(" ") || f.getName().contains(",")) {
				newName = newName.replace(' ', '-');
				newName = newName.replace(',', '-');
			}
			while (newName.contains("--")) {
				newName = newName.replace("--", "-");
			}
			if (!f.getName().equals(newName)) {
				f.renameTo(new File(folderName + newName));
			}
		}

		return;

		//		String a = "a", b = "b", c = "c", d = "d", e = "e", f = "f", g = "g", h = "c", i = "i", j = "j", k = "k", l = "l";
		//
		//		createLog("L1.mxml", new String[][] { { a, b, c, d }, { a, c, b, d }, { a, e, d } }, new int[] { 5, 8, 9 });
		//		createLog("L2.mxml", new String[][] { { a, b, c, d, e, f, b, d, c, e, g }, { a, b, d, c, e, g },
		//				{ a, b, c, d, e, f, b, c, d, e, f, b, d, c, e, g } }, new int[] { 1, 1, 1 });
		//		createLog("L3.mxml", new String[][] { { a, c, d }, { b, c, d }, { a, c, e }, { b, c, e } }, new int[] { 45, 42,
		//				38, 22 });
		//		createLog("L4.mxml", new String[][] { { a, b, e, f }, { a, b, e, c, d, b, f }, { a, b, c, e, b, d, f },
		//				{ a, b, c, d, e, b, f }, { a, e, b, c, d, b, f } }, new int[] { 2, 3, 2, 4, 3 });
		//		createLog("L5.mxml", new String[][] { { a, c, e, g }, { a, e, c, g }, { b, d, f, g }, { b, f, d, g } },
		//				new int[] { 2, 3, 2, 4 });
		//		createLog("L6.mxml", new String[][] { { a, c }, { a, b, c }, { a, b, b, c } }, new int[] { 2, 3, 2 });
		//		createLog("L7.mxml", new String[][] { { a, c }, { a, b, c }, { a, b, b, c } }, new int[] { 2, 3, 2 });
		//		createLog("L8.mxml", new String[][] { { a, c, d }, { b, c, e } }, new int[] { 45, 42 });

	}

	public static void createLog(String name, String[][] traces, int[] occurences) throws IOException {

		XMxmlSerializer ser = new XMxmlSerializer();
		OutputStream out = new FileOutputStream(
				"C://Users//bfvdonge//Documents//My Dropbox//Boudewijn Werk//Research//Papers//2011 ToPNoC ACPN//"
						+ name);
		XLog log = createLog(traces, occurences);
		XConceptExtension.instance().assignName(log, name);
		ser.serialize(log, out);
		out.close();
	}

	public static XLog createLog(String[][] l, int[] occ) {
		XLog log = XFactoryRegistry.instance().currentDefault().createLog();

		int traceNum = 0;
		for (int i = 0; i < l.length; i++) {

			XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
			XEvent e;

			for (int j = 0; j < l[i].length; j++) {
				e = XFactoryRegistry.instance().currentDefault().createEvent();
				XConceptExtension.instance().assignName(e, l[i][j]);
				XLifecycleExtension.instance().assignStandardTransition(e, StandardModel.COMPLETE);

				trace.add(e);
			}
			if (occ != null) {
				for (int j = 0; j < occ[i]; j++) {
					XTrace t = (XTrace) trace.clone();
					XConceptExtension.instance().assignName(t, "trace " + traceNum++);
					log.add(t);
				}
			} else {
				log.add(trace);
				XConceptExtension.instance().assignName(trace, "trace " + traceNum++);
			}

		}
		return log;
	}

	@SuppressWarnings({ "all", "unused" })
	public static XLog createLog(String[][] l) {
		return createLog(l, null);
	}

	public static XLog createInterleavedLog(int tracelimit, String... evts) {

		XLog log = XFactoryRegistry.instance().currentDefault().createLog();

		XTrace trace = XFactoryRegistry.instance().currentDefault().createTrace();
		createInterleavedTrace(tracelimit, log, trace, Arrays.asList(evts));

		return log;

	}

	private static void createInterleavedTrace(int tracelimit, XLog log, XTrace trace, List<String> evts) {

		if (log.size() >= tracelimit) {
			return;
		}

		for (String evt : evts) {
			// select one,
			// add as event
			XEvent e = XFactoryRegistry.instance().currentDefault().createEvent();
			XConceptExtension.instance().assignName(e, evt);
			XLifecycleExtension.instance().assignStandardTransition(e, StandardModel.COMPLETE);
			trace.add(e);

			// continue with rest
			List<String> rest = new ArrayList<String>(evts);
			rest.remove(evt);
			createInterleavedTrace(tracelimit, log, trace, rest);
			trace.remove(e);
		}

		if (evts.isEmpty()) {
			log.add((XTrace) trace.clone());
		}
	}
}
