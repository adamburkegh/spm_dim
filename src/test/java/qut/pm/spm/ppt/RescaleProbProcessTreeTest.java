package qut.pm.spm.ppt;

import static org.junit.Assert.*;

import org.junit.Test;

public class RescaleProbProcessTreeTest {

	@Test
	public void noChange() {
		ProbProcessTreeLeaf leaf = ProbProcessTreeFactory.createLeaf("b",2.0);
		ProbProcessTree result = ProbProcessTreeProjector.rescaleTo(leaf,2.0);
		assertEquals(leaf,result);
	}
	
	@Test
	public void leaf() {
		ProbProcessTreeLeaf leaf = ProbProcessTreeFactory.createLeaf("b",2.0);
		ProbProcessTree result = ProbProcessTreeProjector.rescaleTo(leaf,4.0);
		ProbProcessTreeLeaf expected = ProbProcessTreeFactory.createLeaf("b",4.0);
		assertEquals(expected,result);
	}

	@Test
	public void seq() {
		ProbProcessTreeLeaf lbe = ProbProcessTreeFactory.createLeaf("b",6.0);
		ProbProcessTreeLeaf lce = ProbProcessTreeFactory.createLeaf("c",6.0);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createSequence();
		expected.addChildren(lbe,lce);
		ProbProcessTreeLeaf lb = ProbProcessTreeFactory.createLeaf("b",2.0);
		ProbProcessTreeLeaf lc = ProbProcessTreeFactory.createLeaf("c",2.0);
		ProbProcessTreeNode seq = ProbProcessTreeFactory.createSequence();
		seq.addChildren(lb,lc);
		ProbProcessTree result = ProbProcessTreeProjector.rescaleTo(seq,6.0);
		assertEquals(expected,result);
	}

	@Test
	public void seqAboveChoice() {
		ProbProcessTreeLeaf lbe = ProbProcessTreeFactory.createLeaf("b",12.0);
		ProbProcessTreeNode choiceExp = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE); 
		ProbProcessTreeLeaf lce = ProbProcessTreeFactory.createLeaf("c",6.0);
		ProbProcessTreeLeaf lde = ProbProcessTreeFactory.createLeaf("d",6.0);
		choiceExp.addChildren(lce,lde);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createSequence();
		expected.addChildren(lbe,choiceExp);
		ProbProcessTreeLeaf lb = ProbProcessTreeFactory.createLeaf("b",4.0);
		ProbProcessTreeNode choice = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTreeLeaf lc = ProbProcessTreeFactory.createLeaf("c",2.0);
		ProbProcessTreeLeaf ld = ProbProcessTreeFactory.createLeaf("d",2.0);
		choice.addChildren(lc,ld);
		ProbProcessTreeNode seq = ProbProcessTreeFactory.createSequence();
		seq.addChildren(lb,choice);
		ProbProcessTree result = ProbProcessTreeProjector.rescaleTo(seq,12.0);
		assertEquals(expected,result);
	}

	
}
