package org.processmining.plugins.etm.tests;

import org.processmining.plugins.etm.ETM;
import org.processmining.plugins.etm.model.narytree.test.LogCreator;
import org.processmining.plugins.etm.model.ppt.TreeUtils;
import org.processmining.plugins.etm.parameters.ETMParam;
import org.processmining.plugins.etm.parameters.ETMParamFactory;

public class AlfredoBugTest {

	public static void main(String[] args) {

		//CODE ALFREDO CALLS
		ETMParam etm = ETMParamFactory.buildParam(LogCreator.createLog(new String[][] { { "a", "A", "B" } }), null, 20,
				2, 1, 0.1, 0.25, true, 15, 2, 10, 1, 1000, 5, 1, 1);

		ETM realETM = new ETM(etm);

		System.out.println("STARTING");
		realETM.run();
		
		System.out.println(TreeUtils.toString(realETM.getResult(), etm.getCentralRegistry().getEventClasses()));
		System.out.println(etm.getCentralRegistry().getFitness(realETM.getResult()));

		System.out.println("DONE");

	}

}
