package qut.pm.spm.playout;

import static org.junit.Assert.assertNotNull;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.deckfour.xes.model.XLog;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import qut.pm.prom.helpers.PetriNetFragmentParser;
import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.util.ClockUtil;
import qut.pm.xes.helpers.DelimitedTraceToXESConverter;

public class StochasticPlayoutGeneratorPerfTest {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final long MILLION = 1000000;

	protected static PlayoutGenerator generator ;
	protected static long totalGenTime = 0;

	protected PetriNetFragmentParser parser; 
	protected DelimitedTraceToXESConverter converter = new DelimitedTraceToXESConverter();
	
	@Before
	public void setUp() {
		parser = new PetriNetFragmentParser();
	}
	
	@BeforeClass
	public static void beforeClass() {
		totalGenTime = 0;
		generator = new StochasticPlayoutGenerator(5000,10000);
		Configurator.setLevel(generator.getClass().getName(),Level.WARN);
		LOGGER.info(generator.getClass().getName());
	}
	
	protected void incTotalGenTime(long duration) {
		totalGenTime += duration;
		LOGGER.info( "Duration:"  + duration/MILLION + " ms" );
		LOGGER.info( "Total gen time:" +  totalGenTime/MILLION + " ms" );
	}
	
	@Test
	public void longSilentLoop() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {D} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B 99.0} -> p1 -> {E} -> p2 -> {tau__1} -> p3 -> {tau__3} -> p4 -> {A} -> End");
		parser.addToAcceptingNet(net,                          "p3 -> {tau__2 100.0} -> p3");
		parser.addToAcceptingNet(net,                          
				"Start -> {F 100.0} -> p5 -> {G 10.0} -> End");
		parser.addToAcceptingNet(net,                          
				"		  			   p5 -> {H 30.0} -> End");
		parser.addToAcceptingNet(net,                          
				"		  			   p5 -> {I 30.0} -> End");
		long start = ClockUtil.nanoTime();
		XLog log = generator.buildPlayoutLog(net);
		long end = ClockUtil.nanoTime();
		incTotalGenTime(end-start);
		assertNotNull(log);
	}

	@Test
	public void bigModel() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {A} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {B 50.0} -> p1 -> {C} -> End");
		parser.addToAcceptingNet(net,
				"Start -> {B 50.0} -> p2 -> {D} -> End");
		parser.addToAcceptingNet(net,
				"Start -> {B 50.0} -> p3 -> {E} -> End");
		parser.addToAcceptingNet(net,
				"					  p3 -> {F} -> End");
		parser.addToAcceptingNet(net,
				"					  p3 -> {G} -> End");
		parser.addToAcceptingNet(net,
				"					  p3 -> {H} -> p6 -> {I 20.0} -> p7 -> {J} -> End");
		parser.addToAcceptingNet(net,
				"					  p7 -> {I} -> p7");
		parser.addToAcceptingNet(net,
				"					  p3 -> {K} -> p8 -> {L} -> p9 -> {M 10.0} -> End");
		parser.addToAcceptingNet(net,
				"					  							p9 -> {N 4.0} -> End");
		parser.addToAcceptingNet(net,
				"					  							p9 -> {O 4.0} -> End");
		long start = ClockUtil.nanoTime();
		XLog log = generator.buildPlayoutLog(net);
		long end = ClockUtil.nanoTime();
		incTotalGenTime(end-start);
		assertNotNull(log);
	}

	@Test
	public void bigModelLongLabels() {
		AcceptingStochasticNet net = parser.createAcceptingNet("net", 
				"Start -> {Twohouseholdsbothalikeindignity} -> End");
		parser.addToAcceptingNet(net, 
				"Start -> {InfairVeronawherewelayourscene 50.0} -> p1 -> {Fromancientgrudgebreaktonewmutiny} -> End");
		parser.addToAcceptingNet(net,
				"Start -> {InfairVeronawherewelayourscene 50.0} -> p2 -> {Wherecivilbloodmakescivilhandsunclean} -> End");
		parser.addToAcceptingNet(net,
				"Start -> {InfairVeronawherewelayourscene 50.0} -> p3 -> {Fromforththefatalloinsofthesetwofoes} -> End");
		parser.addToAcceptingNet(net,
				"					  p3 -> {Apairofstarcrossdloverstaketheirlife} -> End");
		parser.addToAcceptingNet(net,
				"					  p3 -> {Whosemisadventuredpiteousoverthrows} -> End");
		parser.addToAcceptingNet(net,
				"					  p3 -> {Dowiththeirdeathburytheirparentsstrife} -> p6 -> {Thefearfulpassageoftheirdeathmarkdlove 20.0} -> p7 -> {Andthecontinuanceoftheirparentsrage} -> End");
		parser.addToAcceptingNet(net,
				"					  p7 -> {Thefearfulpassageoftheirdeathmarkdlove} -> p7");
		parser.addToAcceptingNet(net,
				"					  p3 -> {Whichbuttheirchildrensendnoughtcouldremove} -> p8 -> {Isnowthetwohourstrafficofourstage} -> p9 -> {Thewhichifyouwithpatientearsattend 10.0} -> End");
		parser.addToAcceptingNet(net,
				"					  							p9 -> {Whathereshallmissourtoilshallstrivetomend 4.0} -> End");
		parser.addToAcceptingNet(net,
				"					  							p9 -> {EnterSAMPSONandGREGORYofthehouseofCapuletarmedwithswordsandbucklers 4.0} -> End");
		long start = ClockUtil.nanoTime();
		XLog log = generator.buildPlayoutLog(net);
		long end = ClockUtil.nanoTime();
		incTotalGenTime(end-start);
		assertNotNull(log);
	}

	
}
