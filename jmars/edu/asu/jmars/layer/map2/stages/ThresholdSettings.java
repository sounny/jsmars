package edu.asu.jmars.layer.map2.stages;

import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.util.DebugLog;

public class ThresholdSettings extends AbstractStageSettings implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	
	public static final DebugLog log = DebugLog.instance();
	
	public static enum ThresholdMode { MODE_LE, MODE_GE, MODE_GT, MODE_LT, MODE_EQ, MODE_NE }; 
	
	public static final String stageName = "Threshold";
	public static final String propThresholdValue = "threshold value";
	public static final String propBinOutputValue = "binary output value";
	public static final String propModeValue = "mode value";
	
	private double threshold;
	private boolean binaryOutput;
	private ThresholdMode thresholdMode;

	public ThresholdSettings(){
		threshold = 0;
		binaryOutput = false;
		thresholdMode = ThresholdMode.MODE_GT;
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
	
	public synchronized boolean getBinaryOutput(){
		return binaryOutput;
	}
	
	public synchronized ThresholdMode getMode(){
		return thresholdMode;
	}
	
	public synchronized void setThreshold(double newThreshold){
		double oldThreshold = threshold;
		threshold = newThreshold;
		log.println("Setting threshold value from "+oldThreshold+" to "+newThreshold);
		firePropertyChangeEvent(propThresholdValue, new Double(oldThreshold), new Double(newThreshold));
	}
	
	public synchronized void setBinaryOutput(boolean newBinOutput){
		boolean oldBinOutput = binaryOutput;
		binaryOutput = newBinOutput;
		log.println("Setting binData value from "+oldBinOutput+" to "+newBinOutput);
		firePropertyChangeEvent(propBinOutputValue, new Boolean(oldBinOutput), new Boolean(newBinOutput));
	}
	
	public synchronized void setMode(ThresholdMode newThresholdMode){
		ThresholdMode oldThresholdMode = thresholdMode;
		thresholdMode = newThresholdMode;
		log.println("Setting threshold mode from "+oldThresholdMode+" to "+newThresholdMode);
		firePropertyChangeEvent(propModeValue, oldThresholdMode, newThresholdMode);
	}
	
	public String getStageName() {
		return stageName;
	}

	public Object clone() throws CloneNotSupportedException {
		ThresholdSettings s = (ThresholdSettings)super.clone();
		return s;
	}
}
