package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.Measure;
import qut.pm.spm.log.ProvenancedLog;
import qut.pm.spm.playout.PlayoutGenerator;

/**
 * Adhesion measure by dimensional realism, exploration version.
 *  
 * @author burkeat
 *
 */
public class RelevanceDRMeasure implements StochasticLogCachingMeasure{

	protected final double[] FACTORS = new double[]{-0.692,0.358,0.579,-0.0492,-3.527,-2.157};
	protected final double CONSTANT = -2.92;
	protected final double MAX = 3.87;
	protected final double MIN = -6.40;
	
	protected DimensionalRealismCalculator drCalc;
	protected PlayoutGenerator generator;


	public RelevanceDRMeasure(PlayoutGenerator generator) {
		super();
		this.generator = generator;
		drCalc = new DimensionalRealismCalculator(generator,FACTORS,CONSTANT,MAX,MIN);
	}
	
	public RelevanceDRMeasure() {
		super();
	}
	
	@Override
	public String getUniqueId() {
		return "reldr";
	}

	@Override
	public String getReadableId() {
		return "Adhesion by Dim Real";
	}

	@Override
	public Measure getMeasure() {
		return Measure.RELEVANCE_DR;
	}

	@Override
	public double calculate(ProvenancedLog log, AcceptingStochasticNet net, XEventClassifier classifier) {
		return drCalc.calculate(log, net, classifier);
	}

	@Override
	public void precalculateForLog(ProvenancedLog log, XEventClassifier classifier) {
		drCalc.precalculateForLog(log, classifier);
	}




}
