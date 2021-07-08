package qut.pm.setm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeCheck;

public class RandomProbProcessTreeGeneratorTest {

	private static Logger LOGGER = LogManager.getLogger();

	private void generateAndCheck(long seed, String ... activityParams ) {
		List<String> activityList = Arrays.asList(activityParams);
		RandomProbProcessTreeGenerator rgen = new RandomProbProcessTreeGenerator(seed,20,10000);
		HashSet<String> expectedActivities = new HashSet<String>(activityList);
		Set<ProbProcessTree> trees = rgen.generateTrees(activityList,10);
		for (ProbProcessTree ppt: trees) {
			LOGGER.info(ppt);
			assertTrue(ProbProcessTreeCheck.checkConsistent(ppt));
			Set<String> activities = ProbProcessTreeCheck.findActivities(ppt);
			assertEquals(expectedActivities , activities);
		}
	}

	
	@Test
	public void generateTreesScenario1() {
		generateAndCheck(1l, "a","b","c");
	}


	@Test
	public void generateTreesScenario2() {
		generateAndCheck(2l, "a","b","c");
	}

	@Test
	public void generateTreesMoreAndLongerLabels() {
		generateAndCheck(3l, "Edmund Barton","Alfred Deakin","Chris Watson","George Reid",
				"Andrew Fisher","Joseph Cook","Billy Hughes","Stanley Bruce","James Scullin");
	}

	
}
