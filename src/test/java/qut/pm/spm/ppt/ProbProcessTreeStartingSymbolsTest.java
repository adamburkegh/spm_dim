package qut.pm.spm.ppt;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class ProbProcessTreeStartingSymbolsTest {

	private void assertStartingSymbolsEqual(ProbProcessTree ppt, String ... symbols) {
		Set<String> expectedSymbols = new HashSet<>();
		expectedSymbols.addAll(Arrays.asList(symbols));
		assertEquals( expectedSymbols, ProbProcessTreeCheck.startingSymbols(ppt) );
	}
	
	@Test
	public void silent() {
		ProbProcessTree silent = new PPTSilentImpl(2.0d);
		assertTrue( ProbProcessTreeCheck.startingSymbols(silent).isEmpty() );
	}

	@Test
	public void leaf() {
		ProbProcessTree leaf = new PPTLeafImpl("a",2.0d);
		assertStartingSymbolsEqual( leaf, "a" );
	}

	@Test
	public void choiceWithSilent() {
		ProbProcessTree leaf = new PPTLeafImpl("a",2.0d);
		ProbProcessTree silent = new PPTSilentImpl(2.0d);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		choice.addChild(leaf);
		choice.addChild(silent);
		assertStartingSymbolsEqual( choice, "a" );
	}

	@Test
	public void choiceThreeLeaves() {
		ProbProcessTree la = new PPTLeafImpl("a",2.0d);
		ProbProcessTree lb = new PPTLeafImpl("b",2.0d);
		ProbProcessTree lc = new PPTLeafImpl("c",2.0d);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		choice.addChild(la);
		choice.addChild(lb);
		choice.addChild(lc);
		assertStartingSymbolsEqual( choice, "a","b","c" );
	}

	
	@Test
	public void conc() {
		ProbProcessTree la = new PPTLeafImpl("a",2.0d);
		ProbProcessTree lb = new PPTLeafImpl("b",2.0d);
		ProbProcessTree lc = new PPTLeafImpl("c",2.0d);
		ProbProcessTreeNode conc = new PPTNodeImpl(PPTOperator.CONCURRENCY);
		ProbProcessTreeNode choice = new PPTNodeImpl(PPTOperator.CHOICE);
		choice.addChild(la);
		choice.addChild(lb);
		conc.addChild(lc);
		conc.addChild(choice);
		assertStartingSymbolsEqual( choice, "a","b" );
		assertStartingSymbolsEqual( conc, "a","b","c" );
	}

	@Test
	public void seq() {
		ProbProcessTree la = new PPTLeafImpl("a",2.0d);
		ProbProcessTree lb = new PPTLeafImpl("b",2.0d);
		ProbProcessTree lc = new PPTLeafImpl("c",2.0d);
		ProbProcessTreeNode seq = new PPTNodeImpl(PPTOperator.SEQUENCE);
		seq.addChild(la);
		seq.addChild(lb);
		seq.addChild(lc);
		assertStartingSymbolsEqual( seq, "a");
	}

	@Test
	public void loop() {
		ProbProcessTree la = new PPTLeafImpl("a",2.0d);
		ProbProcessTreeNode loop = new PPTNodeImpl(PPTOperator.PROBLOOP);
		loop.addChild(la);
		assertStartingSymbolsEqual( loop, "a");
	}

	
}
