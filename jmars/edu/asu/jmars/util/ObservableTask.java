package edu.asu.jmars.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;
import java.util.List;

/**
 * This class provides a JavaBean for tracking a task that consists of some
 * number of units of work.
 * 
 * For each unit, the {@link #exec()} method is called; implements should use
 * {@link #getPosition()} to determine which unit to run.
 * 
 * If the thread the task is running in is interrupted, or if the
 * {@link #cancel()} method is called, the work will be aborted.
 * 
 * Every {@link #getAnnounceIncrement()} units, the bean property listeners are
 * notified of a change in progress.
 * 
 * This class is not thread safe.
 */
public abstract class ObservableTask implements Runnable {
	private int start, end, position, lastAnnounced, announceIncrement;
	private List<PropertyChangeListener> listeners = new LinkedList<PropertyChangeListener>();
	private volatile boolean running = true;
	public ObservableTask(int count) {
		this(0,count-1,0,1);
	}
	public ObservableTask(int count, int delta) {
		this(0,count-1,0,delta);
	}
	public ObservableTask(int start, int end, int position, int announceIncrement) {
		this.start = start;
		this.end = end;
		this.position = position;
		this.lastAnnounced = 0;
		this.announceIncrement = announceIncrement;
	}
	public void addListener(PropertyChangeListener l) {
		listeners.add(l);
	}
	public void removeListener(PropertyChangeListener l) {
		listeners.remove(l);
	}
	private void dispatch(PropertyChangeEvent e) {
		for (PropertyChangeListener l: listeners) {
			l.propertyChange(e);
		}
	}
	public int getStart() {
		return start;
	}
	public void setStart(int start) {
		PropertyChangeEvent e = new PropertyChangeEvent(this, "start", this.start, start);
		this.start = start;
		dispatch(e);
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		PropertyChangeEvent e = new PropertyChangeEvent(this, "end", this.end, end);
		this.end = end;
		dispatch(e);
	}
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		int last = this.position;
		this.position = position;
		if ((position - lastAnnounced) > announceIncrement) {
			lastAnnounced = position;
			PropertyChangeEvent e = new PropertyChangeEvent(this, "position", last, position);
			dispatch(e);
		}
	}
	public int getAnnounceIncrement() {
		return announceIncrement;
	}
	public void setAnnounceIncrement(int announceIncrement) {
		PropertyChangeEvent e = new PropertyChangeEvent(this, "announceIncrement", this.announceIncrement, announceIncrement);
		this.announceIncrement = announceIncrement;
		dispatch(e);
	}
	public final void run() {
		while (getPosition() < getEnd() && running) {
			exec();
			if (Thread.interrupted()) {
				running = false;
			}
			setPosition(getPosition() + 1);
		}
	}
	public void cancel() {
		running = false;
	}
	protected abstract void exec();
}
