package org.processmining.plugins.etm.model.narytree.replayer.nativeilp;

import org.processmining.framework.plugin.PluginContext;

public class TestPlugins {

	static {
		// LpSolve doesn't load this by itself...
		System.loadLibrary("lpsolve55");
	}

	//FIXME jbuijs: disabled plug-in
	/*-
	@Plugin(
			name = "00ETM TEST C++ link",
				parameterLabels = {},
				returnLabels = { "Info" },
				returnTypes = { String.class },
				userAccessible = true,
				help = "00ETM TEST C++ link")
	@UITopiaVariant(
			uiLabel = "00ETM TEST C++ link",
				affiliation = "Eindhoven University of Technology",
				author = "jsijen",
				email = "j.g.sijen@student.tue.nl",
				pack = "EvolutionaryTreeMiner")
				/**/
	public String testCLink(PluginContext context) {
		try {
			StringBuilder sb = new StringBuilder();

			sb.append("Version = ").append(LpSolveExtension.getVersion());
			// Add other test methods here

			return sb.toString();
		} catch (Exception e) {
			return "Exception! " + e.toString();
		}
	}
}
