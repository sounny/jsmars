package edu.asu.jmars.layer;

import edu.asu.jmars.*;
import edu.asu.jmars.util.*;
import javax.swing.*;
import java.io.*;

public class WatchedThread extends Thread
 {
	private static final DebugLog log = DebugLog.instance();

	public WatchedThread()
	 {
		super();
	 }

	public WatchedThread(Runnable target)
	 {
		super(target);
	 }

	public WatchedThread(Runnable target, String name)
	 {
		super(target, name);
	 }

	public WatchedThread(String name)
	 {
		super(name);
	 }

	public WatchedThread(ThreadGroup group, Runnable target)
	 {
		super(group, target);
	 }

	public WatchedThread(ThreadGroup group, Runnable target, String name)
	 {
		super(group, target, name);
	 }

	public WatchedThread(ThreadGroup group, String name)
	 {
		super(group, name);
	 }

/*	public void start()
	 {
		log.println("Spawning new thread: " + getName());
		log.printStack(5);
		super.start();
	 }
*/
	public final void run()
	 {
		try
		 {
			super.run();
		 }
		catch(Throwable e)
		 {
			e.printStackTrace();
			Util.showMessageDialog(
				"Uncaught exception (in thread " + getName() + "):\n" +
				"    " + e + "\n" +
				"\n" +
				"The application may or may not have been destabilized.",
				"UNCAUGHT EXCEPTION",
				JOptionPane.ERROR_MESSAGE
				);
		 }
	 }
 }
