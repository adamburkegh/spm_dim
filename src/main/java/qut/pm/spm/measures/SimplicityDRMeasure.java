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
public class SimplicityDRMeasure implements StochasticLogCachingMeasure{

	// Note polarity switched - multiplied by -1
	protected final double[] FACTORS = new double[]{0.087,-0.709,0.842,0,-3.582,2.518};
	protected final double CONSTANT = 0.97;
	protected final double MIN = -5.37;
	protected final double MAX = 2.38;
	
	protected DimensionalRealismCalculator drCalc;
	protected PlayoutGenerator generator;


	public SimplicityDRMeasure(PlayoutGenerator generator) {
		super();
		this.generator = generator;
		drCalc = new DimensionalRealismCalculator(generator,FACTORS,CONSTANT,MAX,MIN);
	}
	
	public SimplicityDRMeasure() {
		super();
	}
	
	@Override
	public String getUniqueId() {
		return "simpdr";
	}

	@Override
	public String getReadableId() {
		return "Simplicity by Dim Real";
	}

	@Override
	public Measure getMeasure() {
		return Measure.SIMPLICITY_DR;
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
