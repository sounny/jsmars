package edu.asu.jmars.layer.map2.msd;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;
import edu.asu.jmars.util.Util;

public class PipelineLegModel implements PropertyChangeListener {
	MapSource mapSource;
	CompositeStage aggStage;
	int aggStageInputNumber;
	List<Stage> innerStages; // [mapSource]-> ::: [innerStages] ::: ->[aggInputStage]
	List<PipelineLegModelListener> listeners;
	
	public PipelineLegModel(MapSource mapSource, CompositeStage aggStage, int aggStageInputNumber){
		this.mapSource = mapSource;
		this.aggStage = aggStage;
		this.aggStageInputNumber = aggStageInputNumber;
		innerStages = new ArrayList<Stage>();
		commonInit();
	}
	
	public PipelineLegModel(Pipeline pp, int aggStageInputNumber){
		this.mapSource = pp.getSource();
		this.aggStage = (CompositeStage)pp.getFinalStage();
		this.aggStageInputNumber = aggStageInputNumber;
		innerStages = new ArrayList<Stage>(Arrays.asList(pp.getInnerStages()));
		commonInit();
	}
	
	protected void commonInit(){
		listeners = new ArrayList<PipelineLegModelListener>();
		for(Stage s: innerStages){
			s.getSettings().addPropertyChangeListener(this);
		}
	}
	
	public int getAggStageInputNumber(){
		return aggStageInputNumber;
	}
	
	public Stage getStage(int index){
		if (index == innerStages.size())
			return aggStage;
		return innerStages.get(index);
	}
	
	public Stage[] getInnerStages(){
		return innerStages.toArray(new Stage[0]);
	}
	
	public int indexOf(Stage stage){
		if (stage.equals(aggStage))
			return innerStages.size();
		return innerStages.indexOf(stage);
	}
	
	public MapSource getMapSource(){
		return mapSource;
	}
	
	public void setMapSource(MapSource source) {
		mapSource = source;
	}
	
	public void insertStage(int index, Stage stage){
		innerStages.add(index, stage);
		stage.getSettings().addPropertyChangeListener(this);
		firePipelineLegModelEvent(new PipelineLegModelEvent(this, PipelineLegModelEvent.STAGES_ADDED, new int[]{ index }, new Stage[]{ stage }));
	}
	
	public void removeStage(Stage stage){
		int idx = innerStages.indexOf(stage);
		removeStage(idx);
	}
	
	public void removeStage(int index){		
		Stage stage = innerStages.remove(index);
		stage.getSettings().removePropertyChangeListener(this);
		firePipelineLegModelEvent(new PipelineLegModelEvent(this, PipelineLegModelEvent.STAGES_REMOVED, new int[]{ index }, new Stage[]{ stage }));
	}
	
	public void setInnerStages(Stage[] newInnerStages){
		Stage[] oldStages = innerStages.toArray(new Stage[0]);
		for(Stage s: innerStages)
			s.getSettings().removePropertyChangeListener(this);

		innerStages.clear();
		innerStages.addAll(Arrays.asList(newInnerStages));
		for(Stage s: innerStages)
			s.getSettings().addPropertyChangeListener(this);
		
		firePipelineLegModelEvent(new PipelineLegModelEvent(this, oldStages, innerStages.toArray(new Stage[0])));
	}
	
	public int getInnerStageCount(){
		return innerStages.size();
	}
	
	public void addPipelineLegModelListener(PipelineLegModelListener l){
		listeners.add(l);
	}
	
	public void removePipelineLegModelListener(PipelineLegModelListener l){
		listeners.remove(l);
	}
	
	private void firePipelineLegModelEvent(PipelineLegModelEvent e){
		for(PipelineLegModelListener l: new ArrayList<PipelineLegModelListener>(listeners)){
			switch(e.getEventType()){
				case PipelineLegModelEvent.STAGES_ADDED:         l.stagesAdded(e); break;
				case PipelineLegModelEvent.STAGES_REMOVED:       l.stagesRemoved(e); break;
				case PipelineLegModelEvent.STAGES_REPLACED:      l.stagesReplaced(e); break;
				case PipelineLegModelEvent.STAGE_PARAMS_CHANGED: l.stageParamsChanged(e); break;
			}
		}
		LManager.repaintAll();
	}
	
	public void propertyChange(PropertyChangeEvent e) {
		firePipelineLegModelEvent(new PipelineLegModelEvent(this, e));
	}
	
	public Pipeline buildPipeline(){
		return new Pipeline(mapSource, innerStages.toArray(new Stage[0]), aggStage);
	}
	
	public String toString(){
		String[] strings = new String[2+innerStages.size()];
		strings[0] = mapSource.getTitle();
		for(int i=0; i<innerStages.size(); i++)
			strings[i+1] = innerStages.get(i).toString();
		strings[strings.length-1] = aggStage.getInputName(aggStageInputNumber);
		
		return "["+Util.join(",", strings)+"]";
	}
}
