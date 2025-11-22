package edu.asu.jmars.layer.map2;

import java.beans.PropertyChangeListener;
import java.io.Serializable;

/**
 * Storage for shared settings of a Stage. Multiple Stages which want
 * to get to the same data will use the same {@link StageSettings} object.
 * Listeners can be added to listen to property changes. 
 */
public interface StageSettings extends Cloneable, Serializable {
	public Stage createStage();
	public StageView createStageView();
	public String getStageName();
	public void addPropertyChangeListener(PropertyChangeListener l);
	public void removePropertyChangeListener(PropertyChangeListener l);
	public Object clone() throws CloneNotSupportedException;
}
