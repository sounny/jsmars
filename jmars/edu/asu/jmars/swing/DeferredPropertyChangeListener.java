package edu.asu.jmars.swing;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.asu.jmars.util.DeltaExecutor;

/**
 * Provides a property change listener that wraps a property change listener
 * and sends it at most one PropertyChangeEvent per msDelay milliseconds, by
 * keeping all events received in the interval and then merging them.
 */
public class DeferredPropertyChangeListener implements PropertyChangeListener, Runnable {
	private final DeltaExecutor exec;
	private final PropertyChangeListener realListener;
	private final List<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();
	public DeferredPropertyChangeListener(PropertyChangeListener realListener, int msDelay) {
		this.realListener = realListener;
		this.exec = new DeltaExecutor(msDelay, this);
	}
	public void propertyChange(PropertyChangeEvent evt) {
		synchronized(events) {
			events.add(evt);
		}
		exec.runDeferrable();
	}
	public void run() {
		Map<String,Object> oldValues = new LinkedHashMap<String,Object>();
		Map<String,PropertyChangeEvent> latestEvents = new LinkedHashMap<String,PropertyChangeEvent>();
		synchronized(events) {
			for (PropertyChangeEvent e: events) {
				if (!oldValues.containsKey(e.getPropertyName())) {
					oldValues.put(e.getPropertyName(), e.getOldValue());
				}
				latestEvents.put(e.getPropertyName(), e);
			}
			events.clear();
		}
		for (String name: latestEvents.keySet()) {
			PropertyChangeEvent latest = latestEvents.get(name);
			realListener.propertyChange(new PropertyChangeEvent(latest.getSource(), name, oldValues.get(name), latest.getNewValue()));
		}
	}
}

