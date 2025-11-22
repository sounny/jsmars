package edu.asu.jmars.layer.stamp;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Based on {@link java.util.concurrent.Executors.DefaultThreadFactory}, but
 * lowers the priority.
 */
public class StampThreadFactory implements ThreadFactory {
	static final AtomicInteger poolNumber = new AtomicInteger(1);
	final ThreadGroup group;
	final AtomicInteger threadNumber = new AtomicInteger(1);
	final String namePrefix;

	public StampThreadFactory(String name) {
		SecurityManager s = System.getSecurityManager();
		group = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		namePrefix = name + "-" + poolNumber.getAndIncrement() + "-thread-";
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
		// do NOT want a hung Stage to keep the JVM open
		t.setDaemon(true);
		// do NOT want threads killing the AWT thread
		t.setPriority(Thread.MIN_PRIORITY);
		return t;
	}
}
