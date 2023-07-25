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
public class AdhesionDRSmallLogMeasure implements StochasticLogCachingMeasure{

	protected final double[] FACTORS = new double[]{1.518,1.540,1.425,-0.0476,1.09,0.61};
	protected final double CONSTANT = 3.09;
	protected final double MAX = 3.10;
	protected final double MIN = -5.94;
	
	protected DimensionalRealismCalculator drCalc;
	protected PlayoutGenerator generator;


	public AdhesionDRSmallLogMeasure(PlayoutGenerator generator) {
		super();
		this.generator = generator;
		drCalc = new DimensionalRealismSmallLogCalculator(generator,FACTORS,CONSTANT,MAX,MIN);
	}
	
	public AdhesionDRSmallLogMeasure() {
		super();
	}
	
	@Override
	public String getUniqueId() {
		return "addr";
	}

	@Override
	public String getReadableId() {
		return "Adhesion by Dim Real";
	}

	@Override
	public Measure getMeasure() {
		return Measure.ADHESION_DR_SL;
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
