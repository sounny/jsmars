package edu.asu.jmars.layer.map2;

public class AutoFillException extends Exception {
	final MapAttr mapAttr;
	final Stage targetStage;
	
	public AutoFillException(String message, MapAttr mapAttr, Stage targetStage){
		super(message);
		this.mapAttr = mapAttr;
		this.targetStage = targetStage;
	}
	
	public AutoFillException(String message, Throwable cause, MapAttr mapAttr, Stage targetStage){
		super(message, cause);
		this.mapAttr = mapAttr;
		this.targetStage = targetStage;
	}
	
	public AutoFillException(String message, Throwable cause){
		super(message, cause);
		this.mapAttr = null;
		this.targetStage = null;
	}
	
	public MapAttr getMapAttr(){
		return mapAttr;
	}
	
	public Stage getTargetStage(){
		return targetStage;
	}
	
	public String toString(){
		return this.getClass().getName()+" occurred due to "+
			(getCause() == null? getMessage(): getCause().toString());
	}
}
