package qut.pm.setm.mutation.mutators;

import static org.junit.Assert.*;

import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XLog;
import org.junit.Test;
import org.processmining.plugins.etm.CentralRegistry;

import qut.pm.spm.ppt.PPTOperator;
import qut.pm.spm.ppt.ProbProcessTree;
import qut.pm.spm.ppt.ProbProcessTreeFactory;
import qut.pm.spm.ppt.ProbProcessTreeNode;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class AddNodeRandomTest {

	private static Logger LOGGER = LogManager.getLogger();
	private DelimitedTraceToXESConverter logConverter = new DelimitedTraceToXESConverter(); 
	
	@Test
	public void genRandomChoice1() {
		XLog log = logConverter.convertTextArgs("a b");
		ProbProcessTreeFactory.initActivityRegistry(new String[] {"a","b"});
		CentralRegistry registry = new CentralRegistry(log,new Random(1));
		AddNodeRandom anr = new AddNodeRandom(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree child1 = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree child2 = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree child3 = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(child1,child2,child3);
		hammerMutate(anr, ppt);
	}

	@Test
	public void genRandomSeq2() {
		XLog log = logConverter.convertTextArgs("a b c d e", "a b");
		ProbProcessTreeFactory.initActivityRegistry(new String[] {"a","b","c","d","e"});
		CentralRegistry registry = new CentralRegistry(log,new Random(2));
		AddNodeRandom anr = new AddNodeRandom(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree child1 = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree child2 = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTree child3 = ProbProcessTreeFactory.createLeaf("c",1);
		ppt.addChildren(child1,child2,child3);
		hammerMutate(anr, ppt);
	}
	
	@Test
	public void genRandomTwoLevel3() {
		XLog log = logConverter.convertTextArgs("a b", "a b");
		ProbProcessTreeFactory.initActivityRegistry(new String[] {"a","b","ca","cb"});
		CentralRegistry registry = new CentralRegistry(log,new Random(3));
		AddNodeRandom anr = new AddNodeRandom(registry);
		ProbProcessTreeNode ppt = ProbProcessTreeFactory.createNode(PPTOperator.CHOICE);
		ProbProcessTree childa = ProbProcessTreeFactory.createLeaf("a",1);
		ProbProcessTree childb = ProbProcessTreeFactory.createLeaf("b",1);
		ProbProcessTreeNode seqb = ProbProcessTreeFactory.createNode(PPTOperator.SEQUENCE);
		ProbProcessTree leafca = ProbProcessTreeFactory.createLeaf("ca",1);
		ProbProcessTree leafcb = ProbProcessTreeFactory.createLeaf("cb",1);
		seqb.addChildren(leafca,leafcb);
		ppt.addChildren(childa,childb,seqb);
		hammerMutate(anr, ppt);
	}

	private void hammerMutate(AddNodeRandom anr, ProbProcessTreeNode ppt) {
		ProbProcessTree current = ppt;
		// Starts to hit integer overflows over ~3500
		for (int i=0; i<3000; i++) {
			try {
				current = anr.mutate(current);
			}catch (Exception e) {
				LOGGER.error(i, e);
				fail(i + " :: " + current.toString());
			}
		}
	}

	
}
