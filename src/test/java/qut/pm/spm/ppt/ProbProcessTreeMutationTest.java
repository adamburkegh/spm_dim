package qut.pm.spm.ppt;

import static org.junit.Assert.*;

import org.junit.Test;

import qut.pm.setm.mutation.MutationException;

public class ProbProcessTreeMutationTest {


	@Test
	public void findLeftmost() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree child1 = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree child2 = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree child3 = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(child1,child2,child3);
		ProbProcessTree result = ProbProcessTreeProjector.findSubNode(ppt,0);
		assertEquals(child1,result);
	}
	
	@Test
	public void findRightmost() {	
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree child1 = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree child2 = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree child3 = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(child1,child2,child3);
		ProbProcessTree result = ProbProcessTreeProjector.findSubNode(ppt,2);
		assertEquals(child3,result);
	}
	
	@Test(expected = MutationException.class)
	public void findPastTree() {	
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree child1 = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree child2 = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree child3 = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(child1,child2,child3);
		ProbProcessTreeProjector.findSubNode(ppt,3);
	}


	@Test
	public void findGrandchild() {	
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree child1 = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTreeNode child2 = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree gc1 = ProbProcessTreeFactory.createLeaf("ba",2);
		ProbProcessTree gc2 = ProbProcessTreeFactory.createLeaf("bb",2);
		ProbProcessTree gc3 = ProbProcessTreeFactory.createLeaf("bc",2);
		child2.addChildren(gc1,gc2,gc3);
		ProbProcessTree child3 = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(child1,child2,child3);
		ProbProcessTree result = ProbProcessTreeProjector.findSubNode(ppt,3);
		assertEquals(gc2,result);
		result = ProbProcessTreeProjector.findSubNode(ppt,0);
		assertEquals(child1,result);
		result = ProbProcessTreeProjector.findSubNode(ppt,1);
		assertEquals(child2,result);
		result = ProbProcessTreeProjector.findSubNode(ppt,2);
		assertEquals(gc1,result);
		result = ProbProcessTreeProjector.findSubNode(ppt,3);
		assertEquals(gc2,result);
		result = ProbProcessTreeProjector.findSubNode(ppt,4);
		assertEquals(gc3,result);
		result = ProbProcessTreeProjector.findSubNode(ppt,5);
		assertEquals(child3,result);
	}
	
	@Test
	public void replaceChildLeaf() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree childc = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(childa,childb,childc);
		ProbProcessTree leafd = ProbProcessTreeFactory.createLeaf("d",1);
		ProbProcessTree result = ProbProcessTreeProjector.replaceSubNode(ppt,1,leafd);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		expected.addChildren(childa,leafd,childc);
		assertEquals(expected,result);
	}

	@Test
	public void replaceGrandchildLeaf() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode seqb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree leafca = ProbProcessTreeFactory.createLeaf("ca",1);
		ProbProcessTree leafcb = ProbProcessTreeFactory.createLeaf("cb",1);
		seqb.addChildren(leafca,leafcb);
		ppt.addChildren(childa,childb,seqb);
		ProbProcessTree childe = ProbProcessTreeFactory.createLeaf("e",1);
		ProbProcessTree result = ProbProcessTreeProjector.replaceSubNode(ppt,3,childe);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTreeNode seqbe = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		seqbe.addChildren(childe,leafcb);
		expected.addChildren(childa,childb,seqbe);
		assertEquals(expected,result);
	}

	@Test
	public void replaceChildNode() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree childc = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(childa,childb,childc);
		ProbProcessTreeNode loopd = ProbProcessTreeFactory.createLoop();
		loopd.addChild(ProbProcessTreeFactory.createLeaf("d",1) );
		ProbProcessTree result = ProbProcessTreeProjector.replaceSubNode(ppt,1,loopd);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		expected.addChildren(childa,loopd,childc);
		assertEquals(expected,result);
	}

	@Test
	public void replaceChoiceLeafWithNewWeight() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree childc = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(childa,childb,childc);
		ProbProcessTree leafd = ProbProcessTreeFactory.createLeaf("d",3);
		ProbProcessTree result = ProbProcessTreeProjector.replaceSubNode(ppt,1,leafd);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa3 = ProbProcessTreeFactory.createLeaf("a",3);
		ProbProcessTree childc3 = ProbProcessTreeFactory.createLeaf("c",3);
		expected.addChildren(childa3,leafd,childc3);
		assertEquals(expected,result);
	}

	@Test
	public void replaceSeqLeafWithNewWeight() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree childc = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(childa,childb,childc);
		ProbProcessTree leafd = ProbProcessTreeFactory.createLeaf("d",3);
		ProbProcessTree result = ProbProcessTreeProjector.replaceSubNode(ppt,1,leafd);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree childa3 = ProbProcessTreeFactory.createLeaf("a",3);
		ProbProcessTree childc3 = ProbProcessTreeFactory.createLeaf("c",3);
		expected.addChildren(childa3,leafd,childc3);
		assertEquals(expected,result);
	}

	
	@Test
	public void replaceLeafWithNode() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode seqb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree leafca = ProbProcessTreeFactory.createLeaf("ca",2);
		ProbProcessTreeNode choicecTBR = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree leafcba = ProbProcessTreeFactory.createLeaf("cba",1);
		ProbProcessTree leafcbb = ProbProcessTreeFactory.createLeaf("cbb",1);
		choicecTBR.addChildren(leafcba,leafcbb);
		seqb.addChildren(leafca,choicecTBR);
		ppt.addChildren(childa,childb,seqb);
		ProbProcessTree choiceReplacement = ProbProcessTreeFactory.createLeaf("f",1.0);
		ProbProcessTree result = ProbProcessTreeProjector.replaceSubNode(ppt,4,choiceReplacement);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTreeNode seqbe = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree leafca1 = ProbProcessTreeFactory.createLeaf("ca",1);
		seqbe.addChildren(leafca1,choiceReplacement);
		ProbProcessTree childahalf = ProbProcessTreeFactory.createLeaf("a",0.5);
		ProbProcessTree childbhalf = ProbProcessTreeFactory.createLeaf("b",0.5);
		expected.addChildren(childahalf,childbhalf,seqbe);
		assertEquals(expected,result);
	}
	
	@Test
	public void replaceGrandchildNode() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode seqb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree leafca = ProbProcessTreeFactory.createLeaf("ca",2);
		ProbProcessTreeNode choicecTBR = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree leafcba = ProbProcessTreeFactory.createLeaf("cba",1);
		ProbProcessTree leafcbb = ProbProcessTreeFactory.createLeaf("cbb",1);
		choicecTBR.addChildren(leafcba,leafcbb);
		seqb.addChildren(leafca,choicecTBR);
		ppt.addChildren(childa,childb,seqb);
		ProbProcessTree childea = ProbProcessTreeFactory.createLeaf("ea",1);
		ProbProcessTree childeb = ProbProcessTreeFactory.createLeaf("eb",1);
		ProbProcessTreeNode choiceReplacement = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		choiceReplacement.addChildren(childea,childeb);
		ProbProcessTree result = ProbProcessTreeProjector.replaceSubNode(ppt,4,choiceReplacement);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTreeNode seqbe = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		seqbe.addChildren(leafca,choiceReplacement);
		expected.addChildren(childa,childb,seqbe);
		assertEquals(expected,result);
	}

	@Test
	public void removeChildLeaf() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree childc = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(childa,childb,childc);
		ProbProcessTree result = ProbProcessTreeProjector.removeSubNode(ppt,1);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		expected.addChildren(childa,childc);
		assertEquals(expected,result);
	}
	
	@Test
	public void removeChildLeafRightmost() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree childc = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(childa,childb,childc);
		ProbProcessTree result = ProbProcessTreeProjector.removeSubNode(ppt,2);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		expected.addChildren(childa,childb);
		assertEquals(expected,result);
	}

	@Test
	public void removeGrandchildren() {
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode seqb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree leafca = ProbProcessTreeFactory.createLeaf("ca",1);
		ProbProcessTree leafcb = ProbProcessTreeFactory.createLeaf("cb",1);
		seqb.addChildren(leafca,leafcb);
		ppt.addChildren(childa,childb,seqb);
		ProbProcessTree result = ProbProcessTreeProjector.removeSubNode(ppt,3);
		ProbProcessTreeNode expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTreeNode seqbe = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		seqbe.addChildren(leafcb);
		expected.addChildren(childa,childb,seqbe);
		assertEquals(expected,result);
		result = ProbProcessTreeProjector.removeSubNode(ppt,2);
		expected = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		seqbe = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		expected.addChildren(childa,childb);
		assertEquals(expected,result);
	}
	
}
