package org.processmining.plugins.etm.model.narytree.replayer.nativeilp;

public class BasisHistory {
	protected final int[] basis;
	protected boolean isInitialized;
	
	public BasisHistory(int basisSize) {
		basis = new int[basisSize];
	}
	
	public int[] getBasis() {
		return basis;
	}
	
	public boolean isInitialized() {
		return isInitialized;
	}
	
	public void setInitialized() {
		isInitialized = true;
	}
}
