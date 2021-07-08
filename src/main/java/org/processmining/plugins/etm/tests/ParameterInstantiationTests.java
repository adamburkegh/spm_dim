package org.processmining.plugins.etm.tests;

import org.deckfour.xes.model.XLog;
import org.processmining.plugins.etm.ETM;
import org.processmining.plugins.etm.experiments.StandardLogs;
import org.processmining.plugins.etm.model.narytree.conversion.NAryTreeToProcessTree;
import org.processmining.plugins.etm.model.ppt.ProbProcessArrayTree;
import org.processmining.plugins.etm.parameters.ETMParam;
import org.processmining.plugins.etm.parameters.ETMParamFactory;
import org.processmining.processtree.ProcessTree;
import org.processmining.processtree.conversion.ProcessTree2Petrinet;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.InvalidProcessTreeException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.NotYetImplementedException;
import org.processmining.processtree.conversion.ProcessTree2Petrinet.PetrinetWithMarkings;

public class ParameterInstantiationTests {

	public void minimalCall() {
		//Preparation of minimal objects
		XLog eventlog = StandardLogs.createDefaultLog();

		//Instantiate a parameter object
		ETMParam param = ETMParamFactory.buildStandardParam(eventlog);
		//Instantiate the ETM algorithm
		ETM etm = new ETM(param);
		//Run!
		etm.run();
		//Get the resulting narytree
		ProbProcessArrayTree tree = etm.getResult();
		//Convert it to a process tree
		ProcessTree processTree = NAryTreeToProcessTree.convert(tree, param.getCentralRegistry().getEventClasses());
		//Which we can then convert to a Petri net
		PetrinetWithMarkings petrinet;
		try {
			petrinet = ProcessTree2Petrinet.convert(processTree);
		} catch (NotYetImplementedException e) {
			e.printStackTrace();
		} catch (InvalidProcessTreeException e) {
			e.printStackTrace();
		}
	}

}
