package qut.pm.setm.results;

import qut.pm.setm.results.TaskRewrites.NameFinder;

public class AddNote {

	public static void main(String[] args) throws Exception{
		if (args.length < 3 || args.length > 3) {
			System.out.println("Usage: AddNote sourceFile stat targetFile");
		}
		String sourceFile = args[0];
		String stat = args[1];
		String message = args[2];
		new TaskRewrites().addNote(sourceFile, stat, message, new NameFinder());
		System.out.println("Added note to " + stat + " in " + sourceFile);
	}
	
}
