package qut.pm.setm.mutation.mutators;

import static org.junit.Assert.*;

import java.util.Random;

import org.deckfour.xes.model.XLog;
import org.junit.Test;
import org.processmining.plugins.etm.CentralRegistry;

import qut.pm.spm.ppt.PPTOperator;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class RemoveUselessNodeTest {

	private static class RemoveUselessNodesFixture extends RemoveUselessNodes{

		public RemoveUselessNodesFixture(CentralRegistry registry) {
			super(registry);
		}
		
		// Makes deterministic
		protected boolean goAhead() {
			return true;
		}

		
	}
	
	private DelimitedTraceToXESConverter logConverter = new DelimitedTraceToXESConverter(); 

	
	@Test
	public void noChange() {
		XLog log = initWithActivities("a","b","c");
		CentralRegistry registry = new CentralRegistry(log,new Random(1));
		RemoveUselessNodes run = new RemoveUselessNodesFixture(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree child1 = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree child2 = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree silent = ProbProcessTreeFactory.createSilent(1);
		ProbProcessTree child3 = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(child1,child2,silent,child3);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		expected.addChildren(child1,child2,silent,child3);
		ProbProcessTree result = run.mutate(ppt);
		assertEquals(expected,result);
	}

	@Test
	public void simpleSequence() {
		XLog log = initWithActivities("a","b","c");
		CentralRegistry registry = new CentralRegistry(log,new Random(1));
		RemoveUselessNodes run = new RemoveUselessNodesFixture(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree child1 = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree child2 = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree silent = ProbProcessTreeFactory.createSilent(1);
		ProbProcessTree child3 = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(child1,child2,silent,child3);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		expected.addChildren(child1,child2,child3);
		ProbProcessTree result = run.mutate(ppt);
		assertEquals(expected,result);
	}

	@Test
	public void grandchildren() {
		XLog log = initWithActivities("a","b","ca","cb");
		CentralRegistry registry = new CentralRegistry(log,new Random(3));
		RemoveUselessNodes run  = new RemoveUselessNodesFixture(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode seqb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree leafca = ProbProcessTreeFactory.createLeaf("ca",1);
		ProbProcessTree leafcb = ProbProcessTreeFactory.createLeaf("cb",1);
		ProbProcessTree silent = ProbProcessTreeFactory.createSilent(1);
		seqb.addChildren(leafca,leafcb,silent);
		ppt.addChildren(childa,childb,seqb);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTreeNode seqbb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		seqbb.addChildren(leafca,leafcb);
		expected.addChildren(childa,childb,seqbb);
		ProbProcessTree result = run.mutate(ppt);
		assertEquals(expected,result);
	}
	
	@Test
	public void uselessParentOfSilentChild() {
		XLog log = initWithActivities("a","b","ca");
		CentralRegistry registry = new CentralRegistry(log,new Random(3));
		RemoveUselessNodes run  = new RemoveUselessNodesFixture(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode seqb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree silent = ProbProcessTreeFactory.createSilent(1);
		seqb.addChildren(silent);
		ppt.addChildren(childa,childb,seqb);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		expected.addChildren(childa,childb,silent);
		ProbProcessTree result = run.mutate(ppt);
		assertEquals(expected,result);
	}

	@Test
	public void parentOfSingleUselessChild() {
		XLog log = initWithActivities("a","b","ca");
		CentralRegistry registry = new CentralRegistry(log,new Random(3));
		RemoveUselessNodes run  = new RemoveUselessNodesFixture(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CONCURRENCY);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode seqb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree silent = ProbProcessTreeFactory.createSilent(1);
		seqb.addChildren(silent);
		ppt.addChildren(childa,childb,seqb);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CONCURRENCY);
		ProbProcessTree childa15 = ProbProcessTreeFactory.createLeaf("a",1.5);
		ProbProcessTree childb15 = ProbProcessTreeFactory.createLeaf("b",1.5);
		expected.addChildren(childa15,childb15);
		ProbProcessTree result = run.mutate(ppt);
		assertEquals(expected,result);		
	}

	@Test
	public void parentLoopOfSingleUselessChild() {
		XLog log = initWithActivities("a","b","ca");
		CentralRegistry registry = new CentralRegistry(log,new Random(3));
		RemoveUselessNodes run  = new RemoveUselessNodesFixture(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CONCURRENCY);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode loop = ProbProcessTreeFactory.createNode(PPTOperator.PROBLOOP);
		ProbProcessTree silent = ProbProcessTreeFactory.createSilent(1);
		loop.addChildren(silent);
		ppt.addChildren(childa,childb,loop);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CONCURRENCY);
		ProbProcessTree childa15 = ProbProcessTreeFactory.createLeaf("a",1.5);
		ProbProcessTree childb15 = ProbProcessTreeFactory.createLeaf("b",1.5);
		expected.addChildren(childa15,childb15);
		ProbProcessTree result = run.mutate(ppt);
		assertEquals(expected,result);		
	}

	@Test
	public void uselessSubTree() {
		XLog log = initWithActivities("a","b","ca");
		CentralRegistry registry = new CentralRegistry(log,new Random(3));
		RemoveUselessNodes run  = new RemoveUselessNodesFixture(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CONCURRENCY);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode loop = ProbProcessTreeFactory.createNode(PPTOperator.PROBLOOP);
		ProbProcessTreeNode silentSeq = ProbProcessTreeFactory.createSequence();
		ProbProcessTree silent = ProbProcessTreeFactory.createSilent(1);
		silentSeq.addChild(silent);
		loop.addChildren(silentSeq);
		ppt.addChildren(childa,childb,loop);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CONCURRENCY);
		ProbProcessTree childa15 = ProbProcessTreeFactory.createLeaf("a",1.5);
		ProbProcessTree childb15 = ProbProcessTreeFactory.createLeaf("b",1.5);
		expected.addChildren(childa15,childb15);
		ProbProcessTree result = run.mutate(ppt);
		assertEquals(expected,result);		
	}

	
	@Test
	public void uselessParent() {
		XLog log = initWithActivities("a","b","ca");
		CentralRegistry registry = new CentralRegistry(log,new Random(3));
		RemoveUselessNodes run  = new RemoveUselessNodesFixture(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode seqb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree leafca = ProbProcessTreeFactory.createLeaf("ca",1);
		seqb.addChildren(leafca);
		ppt.addChildren(childa,childb,seqb);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		expected.addChildren(childa,childb,leafca);
		ProbProcessTree result = run.mutate(ppt);
		assertEquals(expected,result);
	}
	
	private XLog initWithActivities(String ... activities) {
		XLog log = logConverter.convertTextArgs(activities);
		ProbProcessTreeFactory.initActivityRegistry(activities);
		return log;
	}

}
