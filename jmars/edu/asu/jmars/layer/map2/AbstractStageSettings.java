package edu.asu.jmars.layer.map2;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

public abstract class AbstractStageSettings implements StageSettings {
	private static final long serialVersionUID = -787452580306026300L;
	
	private transient List<PropertyChangeListener> listeners;
	
	public AbstractStageSettings(){
		commonInit();
	}
	
	private void commonInit(){
		listeners = new ArrayList<PropertyChangeListener>();
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l) {
		listeners.add(l);
	}
	
	public final void removePropertyChangeListener(PropertyChangeListener l) {
		listeners.remove(l);
	}

	/**
	 * Fires {@link PropertyChangeEvent} on the AWT thread.
	 * @param propertyName Name of the property that changed.
	 * @param oldValue Old value of the property.
	 * @param newValue New value of the property.
	 * 
	 * @see #addPropertyChangeListener(PropertyChangeListener)
	 */
	public final void firePropertyChangeEvent(String propertyName, Object oldValue, Object newValue){
		final PropertyChangeEvent e = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
		final List<PropertyChangeListener> ll = new ArrayList<PropertyChangeListener>(listeners);
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				for(PropertyChangeListener l: ll){
					l.propertyChange(e);
				}
			}
		});
	}
	
	public Object clone() throws CloneNotSupportedException {
		AbstractStageSettings s = (AbstractStageSettings)super.clone();
		s.commonInit();
		return s;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		commonInit();
	}
}
