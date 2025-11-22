package edu.asu.jmars.layer.map2.stages;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;

import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.swing.DeferredPropertyChangeListener;
import edu.asu.jmars.util.DebugLog;

public class SigmaStretchStageSettings extends AbstractStageSettings implements Cloneable, Serializable {
	private static final long serialVersionUID = -6697335572481467237L;
	
	private static final DebugLog log = DebugLog.instance();
	
	public static final String stageName = "Sigma Stretch";
	
	public static final String propVar = "variance";
	public static final String propMinPPD = "minPPD";
	
	// User input fields
	double variance;
	double minPPD; // Used for Explore Mars
				
	public SigmaStretchStageSettings() {
		variance = 40.0;
		minPPD = 1.0;
	}

	public Stage createStage() {
		SigmaStretchStage s = new SigmaStretchStage(this);
		return s;
	}

	public StageView createStageView() {
		SigmaStretchStageView v = new SigmaStretchStageView(this);
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
	
	public void setVariance(double newVariance){
		if (newVariance != variance && !Double.isNaN(newVariance)) {
			double oldVariance = variance;
			variance = newVariance;
			log.println("Setting new variance from "+oldVariance+" to "+newVariance);
			firePropertyChangeEvent(propVar, new Double(oldVariance), new Double(variance));
		}
	}
		
	public double getVariance(){
		return variance;
	}
	
	public void setMinPPD(double newPPD) {
		if (newPPD != minPPD && !Double.isNaN(newPPD)) {
			double oldPPD = minPPD;
			minPPD = newPPD;
			log.println("Setting new minPPD from " +oldPPD+" to "+newPPD);
			firePropertyChangeEvent(propMinPPD, new Double(oldPPD), new Double(minPPD));
		}
	}
	
	public double getMinPPD() {
		return minPPD;
	}
		
	public Object clone() throws CloneNotSupportedException {
		SigmaStretchStageSettings s = (SigmaStretchStageSettings)super.clone();
		return s;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
