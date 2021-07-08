package qut.pm.spm.ppt;

public enum PPTOperator {
	SEQUENCE("->"), CHOICE("\\/"), CONCURRENCY("/\\"), PROBLOOP("@");
	
	private String formatRepr;
	
	private PPTOperator(String formatRepr) {
		this.formatRepr = formatRepr;
	}
	
	@Override
	public String toString() {
		return formatRepr;
	}
}
