package org.processmining.plugins.etm.live;

import java.util.EventListener;

import org.processmining.framework.plugin.events.ProMEventListenerList;

/**
 * Interface for observing running ETM instances where several triggers are send
 * to those observing/listening to the ETM instance.
 * 
 * @author jbuijs
 * 
 * @param <R>
 *            The object that is the result of the ETM.
 */
public interface ETMLiveListener<R> extends EventListener {

	/**
	 * A list of states an ETM instance can be in (e.g. not started, running and
	 * stopped variants).
	 * 
	 * @author jbuijs
	 * 
	 */
	public static enum RunningState {
		//TODO add state 'PAUZED', but also implement all implied functionality

		/**
		 * Indicating that the algorithm has not been started yet.
		 */
		NOT_STARTED,
		/**
		 * Indicating that the algorithm is currently working.
		 */
		RUNNING,
		/**
		 * Indicating that the algorithm is pauzed because the user cancelled it
		 */
		USERCANCELLED,
		/**
		 * Indicating that the algorithm stopped because a termination condition
		 * has been met (e.g. finished successfully).
		 */
		TERMINATED
	}

	/**
	 * When the ETM starts running this event is triggered (for instance
	 * indicating that updates are expected to come in).
	 */
	public void start();

	/**
	 * Trigger fired when a generation has finished, with the new data of the
	 * generation included.
	 * 
	 * @param result
	 * @param popFitnessMean
	 * @param popFitnessStd
	 */
	public void generationFinished(R result);

	/**
	 * Trigger indicating that the ETM stopped running, together with the state
	 * (e.g. user cancellation or termination).
	 * 
	 * @param type
	 */
	public void finished(RunningState type);

	/**
	 * List implementation that fires the events for each of the listeners.
	 * 
	 * @author jbuijs
	 * 
	 * @param <R>
	 *            The object that is the result of the ETM
	 */
	public class ETMListenerList<R> extends ProMEventListenerList<ETMLiveListener<R>> {

		/**
		 * Fire the start event for all listeners, e.g. the ETM has started
		 * running.
		 */
		public void fireStart() {
			for (ETMLiveListener<R> listener : getListeners()) {
				listener.start();
			}
		}

		/**
		 * Fire the finished event for all listeners, indicating that the ETM
		 * stopped. The type of finished is passed on using the
		 * {@link RunningState} enumeration.
		 * 
		 * @param type
		 */
		public void fireFinished(RunningState type) {
			for (ETMLiveListener<R> listener : getListeners()) {
				listener.finished(type);
			}
		}

		/**
		 * Fire the event that the ETM finished a generation for all listeners
		 * (and hence the new result should be used/inspected).
		 * 
		 * @param result
		 *            The current result after the finished generation
		 * @param popFitnessMean
		 * @param popFitnessStd
		 */
		public void fireGenerationFinished(R result) {
			for (ETMLiveListener<R> listener : getListeners()) {
				listener.generationFinished(result);
			}
		}

	}

}
