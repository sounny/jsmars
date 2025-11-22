package edu.asu.jmars.layer.map2.stages;

import java.awt.Color;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStageSettings;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageView;
import edu.asu.jmars.util.DebugLog;

public class ContourStageSettings extends AbstractStageSettings implements Cloneable, Serializable {
	public static final DebugLog log = DebugLog.instance();
	
	public static final String stageName = "Contour";
	public static final String propStep = "step";
	public static final String propBase = "base";
	public static final String propColor = "color";
	public static final String propLineThickness = "line thickness";
	
	double base, step;
	Color color;
	int lineThickness;

	public ContourStageSettings(){
		base = 0;
		step = 100;
		color = Color.white;
		lineThickness = 1;
	}
	
	public Stage createStage() {
		return new ContourStage(this);
	}

	public StageView createStageView() {
		return new ContourStageView(this);
	}

	public synchronized double getBase(){
		return base;
	}
	
	public synchronized double getStep(){
		return step;
	}
	
	public synchronized Color getColor(){
		return color;
	}
	
	public synchronized int getLineThickness(){
		return lineThickness;
	}
	
	public synchronized void setBase(double newStart){
		double oldStart = base;
		base = newStart;
		log.println("Setting start value from "+oldStart+" to "+newStart);
		firePropertyChangeEvent(propBase, new Double(oldStart), new Double(newStart));
	}
	
	public synchronized void setStep(double newStep){
		double oldStep = step;
		step = newStep;
		log.println("Setting step value from "+oldStep+" to "+newStep);
		firePropertyChangeEvent(propStep, new Double(oldStep), new Double(newStep));
	}
	
	public synchronized void setColor(Color newColor){
		Color oldColor = color;
		color = newColor;
		log.println("Setting color value from "+oldColor+" to "+newColor);
		firePropertyChangeEvent(propColor, oldColor, newColor);
	}
	
	public synchronized void setLineThickness(int newThickness) {
		int oldThickness = lineThickness;
		lineThickness = newThickness;
		log.println("Setting line thickness value from "+oldThickness+" to "+newThickness);
		firePropertyChangeEvent(propLineThickness, oldThickness, newThickness);		
	}
	
	public String getStageName() {
		return stageName;
	}

	public Object clone() throws CloneNotSupportedException {
		ContourStageSettings s = (ContourStageSettings)super.clone();
		s.color = new Color(s.color.getRGB());
		
		return s;
	}
}
