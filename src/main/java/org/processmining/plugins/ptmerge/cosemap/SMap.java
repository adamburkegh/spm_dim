package org.processmining.plugins.ptmerge.cosemap;

import java.util.HashMap;

import org.processmining.plugins.ptmerge.cosenet.ActivityNode;
import org.processmining.plugins.ptmerge.cosenet.CoSeNet;

//OVERRIDING PTMerge package
public class SMap {
	public HashMap<ActivityNode, ActivityNode> smap;
	
	public SMap(){
		smap = new HashMap<ActivityNode, ActivityNode>();
	}
	
	public SMap(CoSeNet D, CoSeNet D1){
		this();
		// lets perform some string matching
		for(ActivityNode a: D.Na){
			for(ActivityNode a1: D1.Na){
				if(D.la.get(a).equals(D1.la.get(a1))){
					if(smap.get(a) != null){
						// cannot happen
						//assert(false);
					}
					smap.put(a, a1);
				}
			}
		}
	}
	
	public ActivityNode map1(ActivityNode n1){
		if(smap.containsValue(n1)){
			for(ActivityNode n: smap.keySet()){
				if(smap.get(n).equals(n1)){
					return n;
				}
			}
			// do not get here
			return null;
		}
		else{
			return n1;
		}
	}
}
