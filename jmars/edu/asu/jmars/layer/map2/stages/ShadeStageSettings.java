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

public class ShadeStageSettings extends AbstractStageSettings implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

	private static final DebugLog log = DebugLog.instance();
	
	public static final String stageName = "Shade";
	
	public static final String propAz = "az";
	public static final String propEl = "el";
	
	double az, el;
	
	public ShadeStageSettings(){
		az = 45;
		el = 25;
	}
	
	public Stage createStage() {
		ShadeStage s = new ShadeStage(this);
		return s;
	}

	public StageView createStageView() {
		ShadeStageView v = new ShadeStageView(this);
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

	public void setAz(double newAzValue){
		if (newAzValue != az && !Double.isNaN(newAzValue)) {
			double oldAzValue = az;
			newAzValue = Math.min(360, Math.max(0, newAzValue));
			az = newAzValue;
			log.println("Setting new az value from "+oldAzValue+" to "+newAzValue);
			firePropertyChangeEvent(propAz, new Double(oldAzValue), new Double(newAzValue));
		}
	}
	
	public void setEl(double newElValue){
		if (newElValue != el && !Double.isNaN(newElValue)) {
			double oldElValue = el;
			newElValue = Math.min(90, Math.max(0, newElValue));
			el = newElValue;
			log.println("Setting new el value from "+oldElValue+" to "+newElValue);
			firePropertyChangeEvent(propEl, new Double(oldElValue), new Double(newElValue));
		}
	}
	
	public double getAz(){
		return az;
	}
	
	public double getEl(){
		return el;
	}
	
	public Object clone() throws CloneNotSupportedException {
		ShadeStageSettings s = (ShadeStageSettings)super.clone();
		return s;
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
