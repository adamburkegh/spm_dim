package org.processmining.plugins.etm.model.serialization;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

import org.deckfour.xes.classification.XEventClass;
import org.processmining.contexts.uitopia.annotations.UIExportPlugin;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.plugins.etm.model.ParetoFront;

@Plugin(name = "Pareto front of PT export", returnLabels = {}, returnTypes = {}, parameterLabels = { "Pareto Front",
		"File" }, userAccessible = true)
@UIExportPlugin(description = "PT Pareto Front", extension = "PTPareto")
public class ParetoFrontExport {
	//These characters have no meaning in java regex and are unlikely to be encountered in real life...
	public static final String CLASSDIVIDERSTRING = " :~@#%: ";

	/**
	 * Exports the given workshop model to the given file.
	 * 
	 * @param context
	 *            The given plug-in context.
	 * @param front
	 *            the Pareto front to write to file
	 * @param file
	 *            The given file.
	 * @throws IOException
	 */
	@PluginVariant(variantLabel = "PT Pareto Front", requiredParameterLabels = { 0, 1 })
	public static void export(PluginContext context, ParetoFront front, File file) throws IOException {
		export(front, file);
	}

	/**
	 * Writes the pareto front to file, the result of a call to
	 * {@link ParetoFrontExport#exportToString(ParetoFront)}.
	 * 
	 * @param front
	 * @param file
	 * @throws IOException
	 */
	public static void export(ParetoFront front, File file) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(file));

		writer.append(exportToString(front));

		writer.flush();
		writer.close();
	}

	/**
	 * Returns a string representation of the front as it will/should be written
	 * to file
	 * 
	 * @param front
	 * @return
	 */
	public static String exportToString(ParetoFront front) {
		StringBuilder str = new StringBuilder();

		if (front != null) {
			//First line: XEventClassifier with after that a list of event class instances
			//writer.append(front.getRegistry().getEventClasses().getClassifier().toString());

			Collection<XEventClass> classes = front.getRegistry().getEventClasses().getClasses();
			for (XEventClass clazz : classes) {
				str.append(clazz.getId() + CLASSDIVIDERSTRING);
			}

			str.append("\r\n");

			//Now just output the front :)
			str.append(front.toString());
		}

		return str.toString();
	}

}
