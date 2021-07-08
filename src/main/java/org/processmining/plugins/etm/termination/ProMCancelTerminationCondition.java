package org.processmining.plugins.etm.termination;

import nl.tue.astar.AStarThread.Canceller;

import org.processmining.framework.plugin.PluginContext;
import org.processmining.framework.plugin.Progress;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.TerminationCondition;

/**
 * A termination condition that listens to the ProM progress in the context for
 * cancellation
 * 
 * @author jbuijs
 * 
 */
public class ProMCancelTerminationCondition implements TerminationCondition {
	private final Canceller canceller;

	public ProMCancelTerminationCondition(final PluginContext context) {
		this.canceller = buildCanceller(context);
	}

	public ProMCancelTerminationCondition(final Progress progress) {
		this.canceller = buildCanceller(progress);
	}

	public boolean shouldTerminate(PopulationData<?> populationData) {
		return canceller.isCancelled();
	}

	/**
	 * Build a canceller listening to the provided context (if possible)
	 * 
	 * @param context
	 * @return
	 */
	public static Canceller buildCanceller(final PluginContext context) {
		if (context == null || context.getProgress() == null) {
			return buildDummyCanceller();
		}
		return buildCanceller(context.getProgress());
	}

	/**
	 * Build a canceller listening to the provided progress (if possible)
	 * 
	 * @param progress
	 * @return
	 */
	public static Canceller buildCanceller(final Progress progress) {
		if (progress == null) {
			return buildDummyCanceller();
		}
		Canceller canceller = new Canceller() {
			public boolean isCancelled() {
				try {
					return progress.isCancelled();
				} catch (Exception e) {
					//Any exception here should not break the ETM so just assume no cancellation holds
					return false;
				}
			}
		};
		return canceller;
	}

	/**
	 * Build a dummy canceller that will never actually cancel an execution
	 * 
	 * @return
	 */
	public static Canceller buildDummyCanceller() {
		Canceller canceller = new Canceller() {
			public boolean isCancelled() {
				return false;
			}
		};
		return canceller;
	}

	/**
	 * Returns the internally used Canceller object that can(!should!) be used
	 * for instance to cancel executions of the FitnessReplay alignment
	 * calculation.
	 * 
	 * @return
	 */
	public Canceller getCanceller() {
		return canceller;
	}

}
