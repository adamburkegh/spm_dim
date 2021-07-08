package qut.pm.spm.ppt;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProcessTreeConsistencyTest {

	private static final double EPSILON = 0.01d;


	private void assertConsistent(ProbProcessTree ppt) {
		assertTrue( ProbProcessTreeCheck.checkConsistent(ppt) );
	}
	
	@Test
	public void leafConsistency() {
		ProbProcessTree leaf = new PPTLeafImpl("a",2.0d);
		assertConsistent(leaf);
		assertEquals(1,leaf.size());
		ProbProcessTree silent = new PPTSilentImpl(2.0d);
		assertConsistent(silent);
		assertEquals(1,silent.size());
	}

	@Test
	public void addChild() {
		ProbProcessTree leaf = new PPTLeafImpl("a",2.0d);
		assertConsistent(leaf);
		ProbProcessTree silent = new PPTSilentImpl(2.0d);
		assertConsistent(silent);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		assertEquals(1,choice.size());
		choice.addChild(leaf);
		assertConsistent(choice);
		assertEquals(2,choice.size());
		choice.addChild(silent);
		assertConsistent(choice);
		assertEquals(3,choice.size());
		assertEquals(4.0d,choice.getWeight(),EPSILON);
	}
	
	@Test
	public void seqChildrenSameWeight() {
		ProbProcessTree leaf = new PPTLeafImpl("a",2.0d);
		assertConsistent(leaf);
		ProbProcessTree silent = new PPTSilentImpl(2.0d);
		assertConsistent(silent);
		ProbProcessTreeNode seq = ProbProcessTreeFactory.createSequence();
		seq.addChild(leaf);
		assertConsistent(seq);
		seq.addChild(silent);
		assertEquals(2,seq.getChildren().size());
		assertConsistent(seq);
		assertEquals(2.0d,seq.getWeight(),EPSILON);
	}

	@Test(expected = ProcessTreeConsistencyException.class)
	public void seqChildInconsistentWeight() {
		ProbProcessTree leaf = new PPTLeafImpl("a",2.0d);
		assertConsistent(leaf);
		ProbProcessTree silent = new PPTSilentImpl(5.0d);
		assertConsistent(silent);
		ProbProcessTreeNode seq = ProbProcessTreeFactory.createSequence();
		seq.addChild(leaf);
		assertConsistent(seq);
		seq.addChild(silent);
	}
	
	@Test
	public void addChoiceChildren() {
		ProbProcessTree leaf = new PPTLeafImpl("a",2.0d);
		assertConsistent(leaf);
		ProbProcessTree silent = new PPTSilentImpl(2.0d);
		assertConsistent(silent);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		choice.addChildren(leaf,silent);
		assertConsistent(choice);
		assertEquals(4.0d,choice.getWeight(),EPSILON);
	}
		
	@Test
	public void withLoop() {
		ProbProcessTree leaf = new PPTLeafImpl("a",3.0d);
		assertConsistent(leaf);
		ProbProcessTreeNode loop = ProbProcessTreeFactory.createLoop();
		loop.addChild(leaf);
		assertConsistent(loop);
		assertEquals(3.0d,loop.getWeight(),EPSILON);
	}
	
	@Test(expected = ProcessTreeConsistencyException.class)
	public void withLoopException() {
		ProbProcessTree silent = new PPTSilentImpl(2.0d);
		assertConsistent(silent);
		ProbProcessTreeNode loop = ProbProcessTreeFactory.createLoop();
		assertFalse( ProbProcessTreeCheck.checkConsistent(loop) );
		loop.addChild(silent);
		assertConsistent(loop);
		loop.addChild(silent);
	}
	
}
