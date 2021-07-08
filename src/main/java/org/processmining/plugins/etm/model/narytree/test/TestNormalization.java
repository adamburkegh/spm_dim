package org.processmining.plugins.etm.model.narytree.test;

import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.model.ppt.TreeUtils;

import gnu.trove.map.TObjectShortMap;
import gnu.trove.map.hash.TObjectShortHashMap;

public class TestNormalization {

	public static void main(String[] args) {
		//Make a map to which normalization should sort!
		TObjectShortMap<String> map = new TObjectShortHashMap<String>();
		map.put("A", (short) 8);
		map.put("B", (short) 6);
		map.put("C", (short) 4);
		map.put("D", (short) 2);
		map.put("X", (short) 101);
		map.put("Y", (short) 102);
		
		
		ProbProcessArrayTree tree =  TreeUtils
				.fromString("XOR( LEAF: X , XOR( LEAF: A , XOR( LEAF: B , SEQ( LEAF: C , LEAF: D ) ) ) , LEAF: Y , SEQ( LEAF: A ) )", map );

		System.out.println("BEFORE: " + tree.toString());
		ProbProcessArrayTree normalized = TreeUtils.normalize(tree);
		System.out.println("AFTER : " + normalized.toString());

		System.out.println(tree.toInternalString());
		System.out.println(normalized.toInternalString());

		System.out.println("CONSISTENT: " + normalized.isConsistent());
	}

}
