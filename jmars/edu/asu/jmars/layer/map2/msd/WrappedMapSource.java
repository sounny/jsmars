package edu.asu.jmars.layer.map2.msd;

import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;

/**
 * MapSource holder class. It serves two purposes.
 * <nl>
 * <li> It provides unique references for the {@link ProcTreeModel} paths
 *      even when the same MapSource is being used as an input for multiple sources.</li>
 * <li> Provides a place-holder for MapSource when it is not set for a particular input
 *      of say an RGB stage. This becomes necessary since JTree does not support null nodes.</li>
 * </nl>
 * @author saadat
 *
 */
public class WrappedMapSource {
	private CompositeStage finalStage;
	private int finalStageInputNumber;
	private MapSource wrapped;
	
	public WrappedMapSource(CompositeStage finalStage, int finalStageInputNumber, MapSource src){
		this.finalStage = finalStage;
		this.finalStageInputNumber = finalStageInputNumber;
		this.wrapped = src;
		checkStage();
	}
	
	private void checkStage(){
		if (finalStage == null)
			throw new IllegalArgumentException("Stage argument must not be null.");
	}
	
	public CompositeStage getStage(){
		return finalStage;
	}
	
	public int getStageInputNumber(){
		return finalStageInputNumber;
	}
	
	public void setStage(CompositeStage newFinalStage, int newFinalStageInputNumber){
		this.finalStage = newFinalStage;
		this.finalStageInputNumber = newFinalStageInputNumber;
		checkStage();
	}
	
	public MapSource getWrappedSource(){
		return wrapped;
	}
	
	public void setWrappedSource(MapSource src){
		wrapped = src;
	}
	
	public String toString(){
		return finalStage.getInputName(finalStageInputNumber)+(wrapped != null? (": "+wrapped.toString()): "");
	}
}
