package qut.pm.spm.ppt;

import java.util.Random;

public class ProbProcessTreeUtils {

	public static int randomOperatorType(Random random) {
		PPTOperator[] values = PPTOperator.values();
		return random.nextInt(values.length);
	}
	
}
