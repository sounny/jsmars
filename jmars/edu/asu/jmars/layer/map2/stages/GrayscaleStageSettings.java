package edu.asu.jmars.layer.map2.stages;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.swing.DeferredPropertyChangeListener;
import edu.asu.jmars.util.DebugLog;

public class GrayscaleStageSettings extends AbstractStageSettings implements Cloneable, Serializable {
	private static final long serialVersionUID = -6697335572481467237L;
	
	private static final DebugLog log = DebugLog.instance();
	
	public static final String stageName = "Grayscale";
	
	public static final String propMin = "min";
	public static final String propMax = "max";
	public static final String propAutoMinMax = "auto";
	
	double minValue, maxValue;
	boolean autoMinMax;
	
	/**
	 * @deprecated This value is now stored on the {@link MapSource}. This field
	 *             remains here for backwards compatibility with saved sessions.
	 */
	private double ignore = Double.NaN;
	
	public GrayscaleStageSettings() {
		minValue = Double.POSITIVE_INFINITY;
		maxValue = Double.NEGATIVE_INFINITY;
		autoMinMax = true;
	}

	public Stage createStage() {
		GrayscaleStage s = new GrayscaleStage(this);
		return s;
	}

	public StageView createStageView() {
		GrayscaleStageView v = new GrayscaleStageView(this);
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
	
	public String getStageName(){
		return stageName;
	}

	public void setMinValue(double newMinValue){
		if (newMinValue != minValue && !Double.isNaN(newMinValue)) {
			double oldMinValue = minValue;
			minValue = newMinValue;
			log.println("Setting new min value from "+oldMinValue+" to "+newMinValue);
			firePropertyChangeEvent(propMin, new Double(oldMinValue), new Double(minValue));
		}
	}
	
	public void setMaxValue(double newMaxValue){
		if (newMaxValue != maxValue && !Double.isNaN(newMaxValue)) {
			double oldMaxValue = maxValue;
			maxValue = newMaxValue;
			log.println("Setting new max value from "+oldMaxValue+" to "+newMaxValue);
			firePropertyChangeEvent(propMax, new Double(oldMaxValue), new Double(maxValue));
		}
	}
	
	public void setAutoMinMax(boolean newAutoMinMax) {
		autoMinMax = newAutoMinMax;
		log.println("Setting new auto min/max value from "+(!newAutoMinMax)+" to "+newAutoMinMax);
		firePropertyChangeEvent(propAutoMinMax, new Boolean(!newAutoMinMax), new Boolean(newAutoMinMax));
	}
	
	public double getMinValue(){
		return minValue;
	}
	
	public double getMaxValue(){
		return maxValue;
	}
	
	public boolean getAutoMinMax(){
		return autoMinMax;
	}
	
	public Object clone() throws CloneNotSupportedException {
		GrayscaleStageSettings s = (GrayscaleStageSettings)super.clone();
		return s;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		// set a default that can be overridden by the stream
		//ignore = Double.NaN;
		in.defaultReadObject();
	}
}
