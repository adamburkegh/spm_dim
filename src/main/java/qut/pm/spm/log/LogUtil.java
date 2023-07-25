package qut.pm.spm.log;

import org.deckfour.xes.model.XLog;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.xeslite.plugin.OpenLogFileLiteImplPlugin;

import qut.pm.prom.helpers.ConsoleUIPluginContext;
import qut.pm.prom.helpers.HeadlessDefinitelyNotUIPluginContext;

public class LogUtil {
	public static ProvenancedLog importLog(String logFilePath, 
			   PluginContext pluginContext) 
					   	throws Exception
	{
		XLog log = (XLog) new OpenLogFileLiteImplPlugin().importFile(pluginContext, logFilePath);
		return new ProvenancedLogImpl(log,logFilePath);
	}


	public static ProvenancedLog importLog(String logFilePath) throws Exception
	{
		HeadlessDefinitelyNotUIPluginContext pluginContext 
			= new HeadlessDefinitelyNotUIPluginContext(new ConsoleUIPluginContext(), "provlogloader");
		XLog log = (XLog) new OpenLogFileLiteImplPlugin().importFile(pluginContext, logFilePath);
		return new ProvenancedLogImpl(log,logFilePath);
	}

}
