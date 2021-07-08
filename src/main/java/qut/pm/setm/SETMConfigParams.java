package qut.pm.setm;

import qut.pm.util.ClockUtil;
import qut.pm.xes.helpers.Classifier;

public class SETMConfigParams{
	public int maxGen = 800;
	public int popSize = 15;
	public int eliteSize = (int) (popSize * .3);
	public double crossOverChance = 0.1;

	// private double limitF = -1;//.5;
	
	public double targetFitness = 1;
	// private double chanceOfRandomMutation = .95;
	public boolean preventDuplicates = false;

	// public boolean frMultiplication = false; //Whether the overall fitness should be fr * (SUM(weight*fitness)/SUM(weight)) or fr should be weighted.

	// TODO private boolean createStatsFile = true;	
	public Classifier classifier = Classifier.NAME;
	public long seed = ClockUtil.nanoTime();
	public String runId = "run";
	public String buildVersion = "unknown";
	public String logFileName = "unknownLog";
}