package org.processmining.plugins.etm.parameters;

import org.processmining.plugins.etm.live.ETMLiveListener;

public interface ETMListenerParams<R> {

	/**
	 * @return the listeners
	 */
	public ETMLiveListener.ETMListenerList<R> getListeners();
}
