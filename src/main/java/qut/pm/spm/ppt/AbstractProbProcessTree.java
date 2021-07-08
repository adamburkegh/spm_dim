package qut.pm.spm.ppt;

public abstract class AbstractProbProcessTree implements ProbProcessTree{

	protected double weight;
	protected String label = "";
	protected int type = -1;
	
	public AbstractProbProcessTree(double weight) {
		this.weight = weight;
	}

	public AbstractProbProcessTree(String label, double weight) {
		this.label = label;
		this.weight = weight;
	}

	@Override
	public double getWeight() {
		return weight;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public int getType() {
		return type;
	}
	
	@Override
	public boolean isLeaf() {
		return false;
	}
	
	@Override
	public boolean isSilent() {
		return false;
	}
	
	@Override
	public String formatLabelForNode() {
		return getLabel();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder(formatLabelForNode() + " " + getWeight());
		for (ProbProcessTree child: getChildren()) {
			builder.append( "(" + child.toString() + ")" );
		}
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		long temp;
		temp = Double.doubleToLongBits(weight);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractProbProcessTree other = (AbstractProbProcessTree) obj;
		if (getType() != other.getType())
			return false;
		if (!ProbProcessTreeCheck.weightsNearlyEqual(weight,other.weight))
			return false;
		return getChildren().equals(other.getChildren());
	}
	
}
