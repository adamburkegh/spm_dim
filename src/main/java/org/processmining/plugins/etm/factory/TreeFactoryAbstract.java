package org.processmining.plugins.etm.factory;

import org.processmining.plugins.etm.CentralRegistry;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.uncommons.watchmaker.framework.factories.AbstractCandidateFactory;

public abstract class TreeFactoryAbstract extends AbstractCandidateFactory<ProbProcessArrayTree>{

	protected CentralRegistry registry;
	
	public TreeFactoryAbstract(CentralRegistry registry){
		this.registry=registry;
	}
	
}
