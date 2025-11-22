package edu.asu.jmars.layer.map2;

import java.awt.image.DataBuffer;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.asu.jmars.layer.map2.stages.BandExtractorStageSettings;
import edu.asu.jmars.layer.map2.stages.GrayscaleStageSettings;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;
import edu.asu.jmars.layer.map2.stages.composite.NoCompositeSettings;
import edu.asu.jmars.util.Util;

/**
 * Describes one path through a series of processing steps. Graphs of
 * processing steps are represented by finding all of the linear paths through
 * the graph and representing them as an array of Pipeline objects.
 * 
 * There are some static methods on this object for working with arrays of
 * Pipeline objects.
 * 
 * The {@link #getDeepCopy(Pipeline[])} method creates a complete copy of a
 * given graph. The {@link #getStageCopy} creates a copy of the <i>nodes</i> of
 * the graph, causing the two copies to retain the same settings. When copies
 * need to be made, choose between these methods based on whether the two
 * pipelines should see changes to each others' settings.
 */
public class Pipeline {
	private MapSource source;
	private Stage[] processes;
	
	public Pipeline(MapSource source, Stage[] processes) {
		this.source = source;
		this.processes = (Stage[])processes.clone();
	}
	
	public Pipeline(MapSource source, Stage[] innerStages, Stage finalStage){
		this(source, concat(innerStages, finalStage));
	}
	
	/**
	 * Clones the entire <code>pipeline</code>, including the settings so
	 * changes to settings on the input pipeline are not seen by the output
	 * pipeline, and vice-versa
	 */
	public static Pipeline[] getDeepCopy(Pipeline[] pipeline) throws CloneNotSupportedException {
		if (pipeline == null)
			return null;
		
		Pipeline[] out = new Pipeline[pipeline.length];
		if (out.length > 0){
			CompositeStage comp = (CompositeStage)pipeline[0].getFinalStage();
			comp = (CompositeStage)comp.clone();
			
			for(int i=0; i<out.length; i++){
				Stage[] inner = pipeline[i].getInnerStages();
				for(int j=0; j<inner.length; j++)
					inner[j] = (Stage)inner[j].clone();
				out[i] = new Pipeline(pipeline[i].getSource(), inner, comp);
			}
		}
		
		return out;
	}
	
	/** Duplicates the given pipeline but preserves the original {@link StageSettings} */
	public static Pipeline[] getStageCopy(Pipeline[] pipes) {
		Map<Stage,Stage> oldToNew = new IdentityHashMap<Stage,Stage>();
		Pipeline[] outPipes = new Pipeline[pipes.length];
		for (int i = 0; i < outPipes.length; i++) {
			MapSource source = pipes[i].getSource();
			Stage[] stages = new Stage[pipes[i].getStageCount()];
			for (int j = 0; j < stages.length; j++) {
				Stage oldStage = pipes[i].getStageAt(j);
				if (! oldToNew.containsKey(oldStage)) {
					oldToNew.put(oldStage, oldStage.getSettings().createStage());
				}
				stages[j] = oldToNew.get(oldStage);
			}
			outPipes[i] = new Pipeline(source, stages);
		}
		return outPipes;
	}
	
	public static CompositeStage getCompStage(Pipeline[] pipeline){
		if (pipeline.length == 0)
			return CompStageFactory.instance().getStageByName(NoCompositeSettings.stageName);
		return (CompositeStage)pipeline[0].getFinalStage();
	}
	
	private static Stage[] concat(Stage[] innerStages, Stage finalStage){
		Stage[] stages = new Stage[innerStages.length+1];
		System.arraycopy(innerStages, 0, stages, 0, innerStages.length);
		stages[innerStages.length] = finalStage;
		return stages;
	}
	
	public MapSource getSource() {
		return source;
	}
	
	public Stage[] getProcessing() {
		return (Stage[])processes.clone();
	}
	
	public static Pipeline[] build(MapSource[] sources, CompositeStage aggStage) {
		if (sources.length != aggStage.getInputCount())
			throw new IllegalArgumentException("Source list not length of composite input list");
		
		try {
			Pipeline[] pipeline = new Pipeline[sources.length];
			for(int i=0; i<pipeline.length; i++) {
				if (! sources[i].getMapAttr().isFailed()) {
					pipeline[i] = new Pipeline(sources[i], new Stage[] { aggStage });
				}
			}
			return pipeline;
		} catch (Exception e) {
			return new Pipeline[0];
		}
	}
	
	public static Pipeline[] replaceAggStage(Pipeline[] pipeline, CompositeStage aggStage){
		Pipeline[] newPipeline = new Pipeline[pipeline.length];
		for(int i=0; i<pipeline.length; i++)
			newPipeline[i] = new Pipeline(pipeline[i].getSource(), pipeline[i].getInnerStages(), aggStage);
		return newPipeline;
	}

	public static Pipeline[] buildAutoFilled(MapSource[] sources, CompositeStage aggStage) throws AutoFillException {
		if (sources.length != aggStage.getInputCount())
			throw new AutoFillException("Source list size does not equal number of composite input stages.",
					new IllegalArgumentException("Sources "+Arrays.asList(sources)+" incompatible with CompositeStage "+aggStage));
		
		Pipeline[] pipeline = new Pipeline[sources.length];
		for(int i=0; i<pipeline.length; i++)
			pipeline[i] = buildAutoFilled(sources[i], aggStage, i);
		
		return pipeline;
	}
	
	public static Pipeline buildAutoFilled(MapSource source, Stage toStage, int toStageInputNumber) throws AutoFillException {
		Stage[] autoFillStages = (Stage[])getAutoFillStages(source, toStage, toStageInputNumber, 10).toArray(new Stage[0]);
		return new Pipeline(source, autoFillStages);
	}
	
	// TODO IllegalArgumentExceptions thrown below should be converted to throw/catch.
	// OR better yet, since auto-filling is in response to a user action and whatNeededFor
	// returning null signifies an inability to fail that, this goop should throw a checked
	// exception that the user interface code has to catch and handle.
	public static List<Stage> getAutoFillStages(MapSource source, Stage tgtStage, int tgtInput, int maxStages) throws AutoFillException {
		MapAttr srcAttr = source.getMapAttr();
		
		if (srcAttr == null)
			throw new AutoFillException("Attributes are null.", srcAttr, tgtStage);
		
		if (srcAttr.isFailed())
			throw new AutoFillException("Attributes are marked as failed.", srcAttr, tgtStage);
		
		LinkedList<Stage> list = new LinkedList<Stage>();
		list.add(tgtStage);
		
		while(true){
			MapAttr[] tgtAttrs = tgtStage.consumes(tgtInput);
			if (srcAttr.isCompatible(tgtAttrs))
				return list;
			
			if (tgtAttrs.length == 0)
				throw new AutoFillException("Stage "+tgtStage+" input "+tgtInput+" cannot be connected to.", srcAttr, tgtStage);

			// TODO: Not sure about this logic.
			// Can't think of an easy way to implement it as yet either.
			// I think I want to use the meta information like isColor(), isGrayscale() ...
			// to remain more compatible with changes in the future.
			int i;
			for(i=0; i<tgtAttrs.length; i++){
				if (tgtAttrs[i].getNumColorComp() != null &&
						tgtAttrs[i].getNumColorComp() < srcAttr.getNumColorComp()){
					// Add a band-selector
					BandExtractorStageSettings settings = new BandExtractorStageSettings(srcAttr.getNumColorComp(), tgtInput);
					Stage s = settings.createStage();
					list.add(list.size()-1, s);
					srcAttr = s.produces();
					break;
				}
				else if (tgtAttrs[i].getDataType() != null &&
						tgtAttrs[i].getDataType() == DataBuffer.TYPE_BYTE){
					// grayscalestage only handles one band inputs anyway, so
					// assume the first ignore value component is the only
					// ignore value component
					GrayscaleStageSettings settings = new GrayscaleStageSettings();
					Stage s = settings.createStage();
					list.add(list.size()-1, s);
					srcAttr = s.produces();
					break;
				}
			}
			
			if (i >= tgtAttrs.length) {
				throw new AutoFillException("Unable to fill from srcAttr to "+tgtStage+" on input "+tgtInput+".", srcAttr, tgtStage);
			}
			
			if (list.size() > maxStages)
				throw new AutoFillException("Could not convert from "+srcAttr+" to "+tgtStage+" within "+maxStages+" number of stages", srcAttr, tgtStage);
		}
	}

	
	public Stage getFinalStage(){
		return processes[processes.length-1];
	}
	
	/**
	 * Return the processing stage at the specified index in the pipeline.
	 * @param index Index of the desired stage, zero will return the same stage
	 *        as returned by {@link #getProcessing()}.
	 * @return Stage at the given index or null if the index is past valid range.
	 */
	public Stage getStageAt(int index){
		if (index < 0 || index >= processes.length)
			return null;
		
		return processes[index];
	}
	
	/**
	 * Returns index of the specified stage in the pipeline.
	 * @param stage Stage to be searched.
	 * @return Index of the specified stage if found, or -1 if no such stage exists.
	 */
	public int getStageIndex(Stage stage){
		return Arrays.asList(processes).indexOf(stage);
	}
	
	/**
	 * Returns the count of stages in this pipeline.
	 */
	public int getStageCount() {
		return processes.length;
	}
	
	/**
	 * Returns a new pipeline object which as the specified stage inserted at
	 * the specified index. No range checking is done on the index and no null
	 * checking is done on stage.
	 */
	public Pipeline insert(Stage stage, int atIndex){
		Stage[] stages = new Stage[processes.length+1];
		
		System.arraycopy(processes, 0, stages, 0, atIndex);
		stages[atIndex] = stage;
		System.arraycopy(processes, atIndex, stages, atIndex+1, processes.length-atIndex);
		
		return new Pipeline(source, stages);
	}
	
	/**
	 * Returns a new pipeline with the stage at the specified index 
	 * removed. Final stage cannot be removed via this method.
	 */
	public Pipeline remove(int index){
		if (index < 0 || index >= (processes.length-1))
			throw new IllegalArgumentException("Index out of range: "+index);
		
		Stage[] stages = new Stage[processes.length-1];
		System.arraycopy(processes, 0, stages, 0, index);
		System.arraycopy(processes, index+1, stages, index, processes.length-index-1);
		
		return new Pipeline(source, stages);
	}
	
	/**
	 * Returns a new pipeline with the specified stage removed.
	 */
	public Pipeline remove(Stage stage){
		int index = getStageIndex(stage);
		
		if (index > -1)
			return remove(index);
		
		return new Pipeline(source, processes);
	}
	
	/**
	 * Return stages other than the final stage.
	 */
	public Stage[] getInnerStages(){
		Stage[] innerStages = new Stage[processes.length-1];
		System.arraycopy(processes, 0, innerStages, 0, processes.length-1);
		return innerStages;
	}
	
	public String toString(){
		String[] procNames = new String[processes.length+1];
		procNames[0] = source.toString();
		for(int i=0; i<processes.length; i++)
			procNames[i+1] = processes[i].toString();
		return "["+Util.join(",", procNames)+"]";
	}
}
