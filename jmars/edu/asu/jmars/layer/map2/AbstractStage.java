package edu.asu.jmars.layer.map2;

import java.io.Serializable;


public abstract class AbstractStage implements Stage, Cloneable, Serializable {
	private static final long serialVersionUID = -6243770128040291243L;
	
	private StageSettings stageSettings;
	
	public AbstractStage(StageSettings stageSettings){
		this.stageSettings = stageSettings;
	}
	
	public StageSettings getSettings(){
		return stageSettings;
	}
	
	/**
	 * Returns the name of this stage.
	 */
	public String getStageName(){
		String name = getClass().getName();
		
		int idx;
		if ((idx = name.lastIndexOf('.')) > -1)
			name = name.substring(idx+1);
		
		return name;
	}
	
	public boolean canTake(int inputNumber, MapAttr mapAttr){
		return mapAttr.isCompatible(consumes(inputNumber));
	}
	
	public String toString(){
		return getStageName();
	}
	
	public Object clone() throws CloneNotSupportedException {
		AbstractStage s = (AbstractStage)super.clone();
		s.stageSettings = (StageSettings)s.stageSettings.clone();
		return s;
	}
}
