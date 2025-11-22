package edu.asu.jmars.layer.map2.stages;

import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.swing.ColorMapper;
import edu.asu.jmars.util.DebugLog;

public class ColorStretcherStageSettings extends AbstractStageSettings {
	private static final long serialVersionUID = 4557550840653396595L;

	private static final DebugLog log = DebugLog.instance();
	
	public static String stageName = "Color Stretcher";
	public static final String propCmap = "cmap";
	private ColorMapper.State cmState;
	transient ColorMapper colorMapper = null;
	
	private MapSource mapSource = null;
	private Stage[] stages = null;
	
	private double minValue = 0.00;
	private double maxValue = 0.00;
	
	public ColorStretcherStageSettings(){
		cmState = ColorMapper.State.DEFAULT;
	}
	
	public double getMinValue() {
		return minValue;
	}
	public double getMaxValue() {
		return maxValue;
	}
	public void setMinValue(double min) {
		this.minValue = min;
	}
	public void setMaxValue(double max) {
		this.maxValue = max;
	}
	
	public void setMapSource(MapSource ms) {
		this.mapSource = ms;
	}
	public void setStages(Stage[] stageList) {
		this.stages = stageList;
	}
	public MapSource getMapSource() {
		return this.mapSource;
	}
	public Stage[] getStages() {
		return this.stages;
	}	
	public void setColorMapper(ColorMapper cs) {
		this.colorMapper = cs;
	}
	public ColorMapper getColorMapper() {
		return this.colorMapper;
	}
	
	public ColorMapper.State getColorMapperState(){
		return cmState;
	}
	
	public void setColorMapperState(ColorMapper.State newCmState){
		ColorMapper.State oldCmState = cmState;
		cmState = newCmState;
		log.println("cmState changed from "+oldCmState+" to "+newCmState);
		firePropertyChangeEvent(propCmap, oldCmState, newCmState);
	}
	
	public Stage createStage() {
		return new ColorStretcherStage(this);
	}

	public StageView createStageView() {
		return new ColorStretcherStageView(this);
	}

	public String getStageName(){
		return stageName;
	}
	
	public Object clone() throws CloneNotSupportedException {
		ColorStretcherStageSettings settings = (ColorStretcherStageSettings)super.clone();
		settings.cmState = (ColorMapper.State)(cmState.clone());
		return settings;
	}
}
