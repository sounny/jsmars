package edu.asu.jmars.layer.map2.msd;

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.asu.jmars.layer.map2.CompStageFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.stages.composite.BandAggregator;
import edu.asu.jmars.layer.map2.stages.composite.BandAggregatorSettings;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;
import edu.asu.jmars.util.DebugLog;

/**
 * The model backing the vis and plot nodes in the ProcTreeModel.
 * @author saadat
 */
public class PipelineModel implements PipelineLegModelListener, Cloneable, Serializable {
	private static final long serialVersionUID = 1121387655528081060L;

	private static DebugLog log = DebugLog.instance();
	
	transient private List<PipelineModelListener> pipelineModelListeners;
	transient private List<PropertyChangeListener> propertyChangeListeners;
	transient private CompositeStage selComp;
	transient private ArrayList<WrappedMapSource> sources;
	transient private PipelineLegModel[] legs;
	transient private boolean resizable;
	
	public PipelineModel(CompositeStage initialComp){
		commonInit();
		this.selComp = initialComp;
		adjust(true);
	}
	
	public PipelineModel(Pipeline[] pipeline){
		this(Pipeline.getCompStage(pipeline));
		setLegsFromPipeline(pipeline);
	}
	
	private void commonInit(){
		pipelineModelListeners = new ArrayList<PipelineModelListener>();
		sources = new ArrayList<WrappedMapSource>();
	}
	
	public synchronized void setFromPipeline(Pipeline[] pipeline, CompositeStage compStage){
		this.selComp = compStage;
		adjust(true);
		setSourcesFromPipeline(pipeline);
		setLegsFromPipeline(pipeline);
		fireCompChanged();
	}
	
	private void setSourcesFromPipeline(Pipeline[] pipeline){
		for(int i=0; i<pipeline.length; i++){
			sources.get(i).setWrappedSource(pipeline[i].getSource());
		}
	}
	
	private void setLegsFromPipeline(Pipeline[] pipeline){
		if (pipeline.length > 0){
			for(int i=0; i<pipeline.length; i++){
				legs[i] = new PipelineLegModel(pipeline[i], i);
				legs[i].addPipelineLegModelListener(this);
			}
		}
	}
	
	public CompositeStage[] getPossibleCompStages(){
		if (selComp instanceof BandAggregator)
			return new CompositeStage[]{ selComp };
		return (CompositeStage[])CompStageFactory.instance().getCompStages().toArray(new CompositeStage[0]);
	}
	
	public synchronized CompositeStage getCompStage(){
		return selComp;
	}
	
	public synchronized void setCompStage(CompositeStage compStage){
		this.selComp = compStage;
		adjust(true);
		fireCompChanged();
	}
	
	private void adjust(boolean clearLegs){
		resizable = (selComp instanceof BandAggregator);
		
		if (clearLegs){
			legs = new PipelineLegModel[selComp.getInputCount()];
		}
		else {
			PipelineLegModel[] old = legs;
			legs = new PipelineLegModel[selComp.getInputCount()];
			if (old != null)
				System.arraycopy(old, 0, legs, 0, Math.min(old.length, legs.length));
		}
		
		if (sources.size() > selComp.getInputCount())
			sources.subList(selComp.getInputCount(), sources.size()).clear();
		
		for(int i=0; i<sources.size(); i++)
			((WrappedMapSource)sources.get(i)).setStage(selComp, i);
		
		while(sources.size() < selComp.getInputCount())
			sources.add(new WrappedMapSource(selComp, sources.size(), null));
	}
	
	public synchronized WrappedMapSource[] getSources(){
		return (WrappedMapSource[])sources.toArray(new WrappedMapSource[0]);
	}
	
	public synchronized WrappedMapSource getSource(int i){
		return (WrappedMapSource)sources.get(i);
	}
	
	public synchronized int getSourceCount(){
		return sources.size();
	}
	
	public synchronized int getSourceIndex(WrappedMapSource srcWrapper){
		return sources.indexOf(srcWrapper);
	}
	
	public synchronized int addSource(MapSource source){
		int i = findEmptySlot();
		if (i >= 0){
			if (isResizable()){
				((BandAggregatorSettings)((BandAggregator)selComp).getSettings()).setInputCount(i+1);
				//adjust(false);
				adjust(true);  // TODO: This is also not the correct way of doing this, but it makes things work.
				sources.set(i, new WrappedMapSource(selComp, i, source));
				fireChildrenAdded(new int[]{ i }, new WrappedMapSource[]{ (WrappedMapSource)sources.get(i) });
			}
			else {
				((WrappedMapSource)sources.get(i)).setWrappedSource(source);
				fireChildrenChanged(new int[]{ i }, new WrappedMapSource[]{ (WrappedMapSource)sources.get(i) });
			}
		}
		return i;
	}
	
	public synchronized int removeSource(WrappedMapSource source){
		int idx = -1;
		
		if (isResizable()){
			idx = getSourceIndex(source);
			if (idx > -1){
				WrappedMapSource removed = (WrappedMapSource)sources.remove(idx);
				((BandAggregatorSettings)((BandAggregator)selComp).getSettings()).setInputCount(sources.size());
				adjust(true); // TODO: This is not the correct way of doing this.
				fireChildrenRemoved(new int[]{ idx }, new WrappedMapSource[]{ removed });
			}
		}
		else {
			throw new UnsupportedOperationException("Trying to remove element from fixed sized source "+source);
		}
		return idx;
	}
	
	public synchronized void setSource(int i, MapSource source){
		WrappedMapSource replaced = (WrappedMapSource)sources.get(i);
		((WrappedMapSource)sources.get(i)).setWrappedSource(source);
		if (legs[i] != null)
			legs[i].removePipelineLegModelListener(this);
		legs[i] = null;
		fireChildrenChanged(new int[]{ i }, new WrappedMapSource[]{ replaced });
	}
	
	public synchronized boolean isResizable(){
		return resizable;
	}
	
	private int findEmptySlot(){
		if (isResizable())
			return getSourceCount();
		
		for(int i=0; i<getSourceCount(); i++)
			if (getSource(i).getWrappedSource() == null)
				return i;
		
		return -1;
	}
	
	public synchronized PipelineLegModel getPipelineLeg(final int legIndex) {
		return legs[legIndex];
	}
	
	public void fireCompChanged(){
		PipelineModelEvent e = new PipelineModelEvent(this, PipelineModelEvent.COMP_CHANGED, selComp, null, null);
		firePipelineModelEvent(e);
	}
	
	public void fireChildrenAdded(int[] indices, WrappedMapSource[] srcs){
		PipelineModelEvent e = new PipelineModelEvent(this, PipelineModelEvent.CHILDREN_ADDED, selComp, indices, srcs);
		firePipelineModelEvent(e);
	}
	
	public void fireChildrenRemoved(int[] indices, WrappedMapSource[] srcs){
		PipelineModelEvent e = new PipelineModelEvent(this, PipelineModelEvent.CHILDREN_REMOVED, selComp, indices, srcs);
		firePipelineModelEvent(e);
	}
	
	public void fireChildrenChanged(int[] indices, WrappedMapSource[] srcs){
		PipelineModelEvent e = new PipelineModelEvent(this, PipelineModelEvent.CHILDREN_CHANGED, selComp, indices, srcs);
		firePipelineModelEvent(e);
	}
	
	private void firePipelineModelEvent(PipelineModelEvent e){
		for(PipelineModelListener pml: new ArrayList<PipelineModelListener>(pipelineModelListeners)){
			switch(e.getEventType()){
			case PipelineModelEvent.COMP_CHANGED:     pml.compChanged(e); break;
			case PipelineModelEvent.CHILDREN_ADDED:   pml.childrenAdded(e); break;
			case PipelineModelEvent.CHILDREN_REMOVED: pml.childrenRemoved(e); break;
			case PipelineModelEvent.CHILDREN_CHANGED: pml.childrenChanged(e); break;
			case PipelineModelEvent.FORWARDED_EVENT:  pml.forwardedEventOccurred(e); break;
			}
		}
	}

	public void addPipelineModelListener(PipelineModelListener l){
		pipelineModelListeners.add(l);
	}
	
	public void removePipelineModelListener(PipelineModelListener l){
		pipelineModelListeners.remove(l);
	}

	public synchronized boolean isValid(){
		for(int i=0; i<getSourceCount(); i++){
			if (getSource(i).getWrappedSource() == null)
				return false;
			if (getPipelineLeg(i) == null)
				return false;
		}
		
		return true;
	}
	
	public synchronized Pipeline[] buildPipeline() {
		Pipeline[] pipeline = new Pipeline[selComp.getInputCount()];
		for(int i=0; i<pipeline.length; i++)
			pipeline[i] = getPipelineLeg(i).buildPipeline();
		return pipeline;
	}
	
	//
	// PipelineLegModelListener implementation
	//
	private void wrapAndFirePipelineLegEvent(PipelineLegModelEvent e){
		firePipelineModelEvent(new PipelineModelEvent(this, e));
	}
	
	public void stageParamsChanged(PipelineLegModelEvent e) {
		wrapAndFirePipelineLegEvent(e);
	}

	public void stagesAdded(PipelineLegModelEvent e) {
		wrapAndFirePipelineLegEvent(e);
	}

	public void stagesRemoved(PipelineLegModelEvent e) {
		wrapAndFirePipelineLegEvent(e);
	}

	public void stagesReplaced(PipelineLegModelEvent e) {
		wrapAndFirePipelineLegEvent(e);
	}
	
	//
	// toString override
	//
	public String toString(){
		return getCompStage().getStageName();
	}

	public synchronized void setInnerStages(int i, Stage[] innerStages) {
		if (legs[i] == null){
			legs[i] = new PipelineLegModel(getSource(i).getWrappedSource(), (CompositeStage)getSource(i).getStage(), i);
			legs[i].addPipelineLegModelListener(this);
			legs[i].setInnerStages(innerStages);
		}
		else {
			legs[i].setInnerStages(innerStages);
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		
		SavedState s = new SavedState(this);
		out.writeObject(s);
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		
		SavedState s = (SavedState)in.readObject();

		commonInit();
		selComp = s.comp;
		if (!(s.comp instanceof BandAggregator))
			selComp = CompStageFactory.instance().getStageByName(selComp.getStageName());
		adjust(true);
		
		for(int i=0; i<selComp.getInputCount(); i++){
			if (s.mapSources[i] != null)
				setSource(i, s.mapSources[i]);
			
			if (s.innerStages[i] != null)
				setInnerStages(i, s.innerStages[i]);
		}
	}
	
	public Object clone() throws CloneNotSupportedException {
		PipelineModel ppm = (PipelineModel)super.clone();
		ppm.commonInit();
		ppm.setCompStage(getCompStage());
		for(int i=0; i<sources.size(); i++){
			ppm.setSource(i, sources.get(i).getWrappedSource());
			
			PipelineLegModel plm = ppm.getPipelineLeg(i);
			if (plm != null)
				ppm.setInnerStages(i, (Stage[])plm.getInnerStages().clone());
		}
		
		return ppm;
	}
	
	public static List<MapSource> unwrap(WrappedMapSource[] wrapped){
		List<MapSource> unwrapped = new LinkedList<MapSource>();
		for (WrappedMapSource ws: wrapped) {
			unwrapped.add(ws.getWrappedSource());
		}
		return unwrapped;
	}
	
	/**
	 * Data holder for Serialization.
	 */
	private static class SavedState implements Serializable {
		private static final long serialVersionUID = -1393266546103827514L;
		
		final CompositeStage comp;
		final MapSource[] mapSources;
		final Stage[][] innerStages;
		
		public SavedState(PipelineModel ppm) {
			comp = ppm.getCompStage();
			
			WrappedMapSource[] srcs = ppm.getSources();
			mapSources = new MapSource[srcs.length];
			for(int i=0; i<mapSources.length; i++)
				mapSources[i] = srcs[i].getWrappedSource();
			
			innerStages = new Stage[mapSources.length][];
			for(int i=0; i<innerStages.length; i++){
				PipelineLegModel leg = ppm.getPipelineLeg(i);
				if (leg != null)
					innerStages[i] = leg.getInnerStages(); 
			}
		}
	}
	
}
