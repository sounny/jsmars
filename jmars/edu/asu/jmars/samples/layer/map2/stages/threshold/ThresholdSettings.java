package edu.asu.jmars.samples.layer.map2.stages.threshold;

import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.util.DebugLog;

public class ThresholdSettings extends AbstractStageSettings implements Cloneable, Serializable {
	public static final DebugLog log = DebugLog.instance();
	
	public static final String stageName = "Threshold";
	public static final String propThresholdValue = "threshold value";
	
	double threshold;

	public ThresholdSettings(){
		threshold = 100;
	}
	
	public Stage createStage() {
		return new ThresholdStage(this);
	}

	public StageView createStageView() {
		return new ThresholdStageView(this);
	}

	public synchronized double getThreshold(){
		return threshold;
	}
	
	public synchronized void setThreshold(double newThreshold){
		double oldThreshold = threshold;
		threshold = newThreshold;
		log.println("Setting threshold value from "+oldThreshold+" to "+newThreshold);
		firePropertyChangeEvent(propThresholdValue, new Double(oldThreshold), new Double(newThreshold));
	}
	
	public String getStageName() {
		return stageName;
	}

	public Object clone() throws CloneNotSupportedException {
		ThresholdSettings s = (ThresholdSettings)super.clone();
		return s;
	}
}
