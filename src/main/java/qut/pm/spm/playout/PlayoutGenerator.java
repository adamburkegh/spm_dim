package qut.pm.spm.playout;

import org.deckfour.xes.model.XLog;

import qut.pm.spm.AcceptingStochasticNet;

public interface PlayoutGenerator {

	/** 
	 * GSPNs only (even though the type interface takes GDT_SPNs).
	 * 
	 * @param net
	 * @param targetSize
	 * @param ctMarking
	 * @return
	 */
	XLog buildPlayoutLog(AcceptingStochasticNet net, int targetSize);

}