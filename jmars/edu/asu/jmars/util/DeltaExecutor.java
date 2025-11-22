package edu.asu.jmars.util;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Executes the given task each time run() is called, unless it has been called
 * in the last 'minUpdateDelta' milliseconds, in which case it is guaranteed
 * that the task will run again, but not until that amount of time has elapsed.
 * This does mean the task may not execute once for each call to
 * {@link #runDeferrable()}.
 * 
 * The task must complete quickly and may be called immediately by the thread
 * that calls {@link #run} or some time later by a timer thread.
 */
public class DeltaExecutor {
	private final DebugLog log = DebugLog.instance();
	private final long minUpdateDelta;
	private final Runnable task;
	private long lastUpdate;
	private volatile Timer timer;
	
	/**
	 * @param minUpdateDelta The delta in milliseconds between invocations of the given task.
	 * @param task The task to execute each time {@link #run()} is called.
	 */
	public DeltaExecutor(long minUpdateDelta, Runnable task) {
		this.minUpdateDelta = minUpdateDelta;
		this.task = task;
		lastUpdate = System.currentTimeMillis() - minUpdateDelta;
	}
	
	/** Returns true if the task will be run again when the timer expires */
	public synchronized boolean deferredWaiting() {
		return timer != null;
	}
	
	/**
	 * Executes the task given to the constructor, possibly on the thread that
	 * calls this method, and possibly some time later on a timer thread.
	 */
	public synchronized void runDeferrable() {
		long now = System.currentTimeMillis();
		if (now - lastUpdate < minUpdateDelta) {
			log.println("Deferring action");
			if (timer == null) {
				timer = new Timer("DeltaExecutor timer", true);
				timer.schedule(new TimerTask() {
					public void run() {
						synchronized(DeltaExecutor.this) {
							// check if the timer was canceled just in case it
							// was canceled while waiting to acquire the lock on
							// this
							if (timer != null) {
								timer = null;
								log.println("Deferred action running");
								lastUpdate = System.currentTimeMillis();
								task.run();
							}
						}
					}
				}, minUpdateDelta);
			}
		} else {
			runImmediately();
		}
	}
	
	/**
	 * Cancels any deferred action and sets the delta clock so the next call to
	 * {@link #runDeferred()} will execute immediately. If there is no deferred
	 * action the clock is still reset so the next call runs immediately.
	 */
	public synchronized void reset() {
		if (timer != null) {
			log.println("Cancelling deferred action");
			timer.cancel();
			timer = null;
		}
		lastUpdate = System.currentTimeMillis() - minUpdateDelta;
	}
	
	/**
	 * Immediately executes the task given to the constructor on the calling
	 * thread; calls to this method will finish after the task finishes.
	 * 
	 * If a prior call to {@link #runDeferrable()} was queued for later
	 * execution, this method will cancel it.
	 */
	public synchronized void runImmediately() {
		long now = System.currentTimeMillis();
		if (timer != null) {
			reset();
		}
		log.println("Running action immediately");
		lastUpdate = now;
		task.run();
	}
}
