package org.processmining.plugins.etm.utils;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ETMUtils {

	private static Logger LOG = LogManager.getLogger();
	
	/**
	 * Puts the provided string on the clipboard for easy paste
	 * @param message
	 */
	public static void onClipboard(String message){
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable transferable = new StringSelection(message);
		clipboard.setContents(transferable, null);
	}
	
	public static void lassert(boolean condition) {
		if (!condition) {		
			LOG.error("Assertion failed");
			throw new RuntimeException("Assertion failed");
		}
	}
	
}
