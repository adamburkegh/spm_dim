package qut.pm.spm.ppt;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ProbProcessTreeDeterminismTest {

	@Test
	public void silent() {
		ProbProcessTree silent = new PPTSilentImpl(2.0d);
		assertTrue( ProbProcessTreeCheck.deterministic(silent) );
	}

	@Test
	public void leaf() {
		ProbProcessTree leaf = new PPTLeafImpl("a",2.0d);
		assertTrue( ProbProcessTreeCheck.deterministic(leaf) );
	}

	@Test
	public void choiceWithSilent() {
		ProbProcessTree leaf = new PPTLeafImpl("a",2.0d);
		ProbProcessTree silent = new PPTSilentImpl(2.0d);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		choice.addChild(leaf);
		choice.addChild(silent);
		assertTrue( ProbProcessTreeCheck.deterministic(choice) );
	}

	@Test
	public void choiceThreeUniqueLeaves() {
		ProbProcessTree la = new PPTLeafImpl("a",2.0d);
		ProbProcessTree lb = new PPTLeafImpl("b",2.0d);
		ProbProcessTree lc = new PPTLeafImpl("c",2.0d);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		choice.addChild(la);
		choice.addChild(lb);
		choice.addChild(lc);
		assertTrue( ProbProcessTreeCheck.deterministic(choice) );
	}

	
	@Test
	public void choiceThreeLeavesNonD() {
		ProbProcessTree la1 = new PPTLeafImpl("a",2.0d);
		ProbProcessTree lb = new PPTLeafImpl("b",2.0d);
		ProbProcessTree la2 = new PPTLeafImpl("a",2.0d);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		choice.addChild(la1);
		choice.addChild(lb);
		choice.addChild(la2);
		assertFalse( ProbProcessTreeCheck.deterministic(choice) );
	}

	
	@Test
	public void concDet() {
		ProbProcessTree la = new PPTLeafImpl("a",2.0d);
		ProbProcessTree lb = new PPTLeafImpl("b",2.0d);
		ProbProcessTree lc = new PPTLeafImpl("c",2.0d);
		ProbProcessTreeNode conc = new PPTNodeImpl(PPTOperator.CONCURRENCY);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		choice.addChild(la);
		choice.addChild(lb);
		conc.addChild(lc);
		conc.addChild(choice);
		assertTrue( ProbProcessTreeCheck.deterministic(conc) );
	}
	
	@Test
	public void concNonDetNest() {
		ProbProcessTree la = new PPTLeafImpl("a",2.0d);
		ProbProcessTree lc1 = new PPTLeafImpl("c",2.0d);
		ProbProcessTree lc2 = new PPTLeafImpl("c",2.0d);
		ProbProcessTreeNode conc = new PPTNodeImpl(PPTOperator.CONCURRENCY);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		choice.addChild(la);
		choice.addChild(lc1);
		conc.addChild(lc2);
		conc.addChild(choice);
		assertFalse( ProbProcessTreeCheck.deterministic(conc) );
	}


	@Test
	public void seq() {
		ProbProcessTree la = new PPTLeafImpl("a",2.0d);
		ProbProcessTree lb = new PPTLeafImpl("b",2.0d);
		ProbProcessTree lc = new PPTLeafImpl("c",2.0d);
		ProbProcessTreeNode seq = new PPTNodeImpl(PPTOperator.SEQUENCE);
		seq.addChild(la);
		seq.addChild(lb);
		seq.addChild(lc);
		assertTrue( ProbProcessTreeCheck.deterministic(seq) );
	}

	@Test
	public void loop() {
		ProbProcessTree la = new PPTLeafImpl("a",2.0d);
		ProbProcessTreeNode loop = new PPTNodeImpl(PPTOperator.PROBLOOP);
		loop.addChild(la);
		assertTrue( ProbProcessTreeCheck.deterministic(loop) );
	}

	
}
