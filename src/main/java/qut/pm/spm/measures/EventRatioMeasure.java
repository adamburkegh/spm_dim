package qut.pm.spm.measures;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.Measure;
import qut.pm.spm.TraceFreq;
import qut.pm.spm.playout.PlayoutGenerator;

public class EventRatioMeasure extends TraceRatioMeasure {

	private SubtraceCalculator subtraceCalc = new SubtraceCalculator();
	
	public EventRatioMeasure() {
		super(1);
	}

	public EventRatioMeasure(PlayoutGenerator generator) {
		super(generator,1);
	}
	
	@Override
	public String getReadableId() {
		return "Activity Ratio Gower's similarity";
	}
	
	@Override
	public String getUniqueId() {
		return "ergs";
	}

	@Override
	public Measure getMeasure() {
		return Measure.EVENT_RATIO_GOWER;
	}
	
	@Override
	public TraceFreq calculateForLog(XLog log, XEventClassifier classifier) {
		return subtraceCalc.calculateActivityFreq(log,classifier);
	}

	@Override
	protected double calculateForPlayout(TraceFreq playoutFreq) {
		modelEventFreq = subtraceCalc.calculateActivityFreq(playoutFreq);
		return compareSubtraceFrequencies(logEventFreq,modelEventFreq);
	}


	
}
