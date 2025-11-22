package edu.asu.jmars.layer.map2.stages;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.swing.DeferredPropertyChangeListener;
import edu.asu.jmars.util.DebugLog;

public class LowPassFilterStageSettings extends AbstractStageSettings implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	private static final DebugLog log = DebugLog.instance();
	
	public static final String stageName = "Low Pass Filter";
	
	public static final String propKernelSize = "grid size";
	
	private int gridSize;
	
	public LowPassFilterStageSettings(){
		gridSize = 3;
	}
	
	public Stage createStage() {
		LowPassFilterStage s = new LowPassFilterStage(this);
		return s;
	}

	public StageView createStageView() {
		LowPassFilterStageView v = new LowPassFilterStageView(this);
		super.addPropertyChangeListener(v);
		return v;
	}

	/**
	 * override parent class implementation to wrap property change listeners in
	 * a listener that will gather the values together and only send updates at
	 * most once every 3 seconds, since the impact of an update in this stage is
	 * quite high.
	 */
	public void addPropertyChangeListener(final PropertyChangeListener l) {
		super.addPropertyChangeListener(new DeferredPropertyChangeListener(l, 3000));
	}
	
	public String getStageName() {
		return stageName;
	}

	public void setGridSize(int newValue){
		if (newValue != gridSize) {
			int oldValue = gridSize;
			gridSize = newValue;
			log.println("Setting new grid size value from "+oldValue+" to "+newValue);
			firePropertyChangeEvent(propKernelSize, new Integer(oldValue), new Integer(newValue));
		}
	}
	
	public int getGridSize() {
		return gridSize;
	}
	
	
	public Object clone() throws CloneNotSupportedException {
		LowPassFilterStageSettings s = (LowPassFilterStageSettings)super.clone();
		return s;
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
