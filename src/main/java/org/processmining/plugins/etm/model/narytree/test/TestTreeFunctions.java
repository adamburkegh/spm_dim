package org.processmining.plugins.etm.model.narytree.test;

import org.processmining.plugins.etm.model.ppt.Configuration;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

public class TestTreeFunctions {

	public static void main(String[] args) {
		//Instantiate a tree
		ProbProcessArrayTree baseTree = TreeUtils
				.fromString("XOR( LOOP( AND( LOOP( LEAF: 1 , LEAF: 0 , LEAF: tau ) , LEAF: tau , LEAF: tau , LEAF: tau ) , LEAF: tau , LEAF: tau ) , LEAF: tau )");

		//Make sure that it is correct now
		assert baseTree.isConsistent();
		System.out.println("Base tree:");
		System.out.println(baseTree.toInternalString());

		ProbProcessArrayTree newTree = baseTree.addParent(2, (short) -3, Configuration.NOTCONFIGURED);

		System.out.println("New Tree:");
		System.out.println(newTree.toInternalString());

		//inconsistent: loop has 2 children left!
		System.out.println("Consistent: " + newTree.isConsistent());
		assert newTree.isConsistent();
	}
}
