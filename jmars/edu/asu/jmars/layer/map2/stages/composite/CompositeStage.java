package edu.asu.jmars.layer.map2.stages.composite;

import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.util.DebugLog;

/**
 * Represents a general Stage that aggregates multiple inputs into a single
 * output.
 * 
 * It provides simpler construction, standard handling of fields <i>other</i>
 * than <code>image</code>, and creates <code>image</code> when
 * <code>process()</code> is first called.
 * 
 * Subclasses should override process(), call super.process() therein to get a
 * MapData object that has all fields but <code>image</code> updated, update
 * the image on the returned MapData object, and then return it.
 * 
 * Subclasses should not change the output data type based on the input data
 * since the output MapData object is only recreated when view parameters change.
 * 
 * The CompositeFactory instantiates an instance of each CompositeStage it knows
 * about, so care should be taken to only allocate image data when process() is
 * called.
 */
public abstract class CompositeStage extends AbstractStage implements Cloneable, Serializable {
	private static DebugLog log = DebugLog.instance();
	
	transient private MapData mapData;
	transient private boolean[] finished;
	transient private Area[] fuzzy;
	transient private Area[] finalArea;
	
	public CompositeStage(StageSettings settings) {
		super(settings);
		initData();
	}
	
	private void initData() {
		mapData = null;
		finished = new boolean[getInputCount()];
		fuzzy = new Area[getInputCount()];
		finalArea = new Area[getInputCount()];
		for (int i=0; i<getInputCount(); i++){
			finished[i] = false;
			fuzzy[i] = new Area();
			finalArea[i] = new Area();
		}
	}

	/**
	 * Returns the label associated with the specified input number of this stage.
	 * @param inputNumber Serial number of the input for which name is returned.
	 * @return A non-null label of the input.
	 */
	public abstract String getInputName(int inputNumber);
	
	/**
	 * Returns the labels associated with all the inputs of this stage. The labels
	 * are in order of the input number.
	 * @return A non-null array of input labels.
	 */
	public abstract String[] getInputNames();

	/**
	 * Create an return a BufferedImage suitable for output of this stage.
	 * The same is used by {@link #process(int, MapData, Area)}.
	 * @param width Width of the image.
	 * @param height Height of the image.
	 * @return A BufferedImage with the specified width and height.
	 */
	public abstract BufferedImage makeBufferedImage(int width, int height);
	
	/**
	 * Returns the aggregated image, with all the fields except 'image' updated with the input.
	 * Subclasses should override this method and call it to set 'image'.
	 * @param changedArea Area that has changed since the last call to this process method
	 * on this composite object.
	 */
	public MapData process(int input, MapData inputData, Area changedArea) {
		if (inputData == null || inputData.getImage() == null)
			throw new IllegalArgumentException("Map data or image is null");
		if (input < 0 || input >= getInputCount())
			throw new IllegalArgumentException("Input out of range");
		
		// If the cached request was cancelled, clear our cache
		if (mapData !=null && mapData.getRequest().isCancelled()) {
			log.println("mapData cleared on input "+ input +" since it was "+(mapData==null? "null": "cancelled"));
			mapData=null;
		}
		
		// if this is the first time, or we're starting on a new request
		if (mapData == null ||
				!mapData.getRequest().getExtent().equals(inputData.getRequest().getExtent()) ||
				 mapData.getRequest().getPPD() != inputData.getRequest().getPPD() ||
				 mapData.getRequest().getProjection() != inputData.getRequest().getProjection()) {
			log.println("Creating new output object");
			initData();
			int w = inputData.getImage().getWidth();
			int h = inputData.getImage().getHeight();
			mapData = inputData.getDeepCopyShell(makeBufferedImage(w,h), null);
		} else if (mapData.getImage().getWidth() != inputData.getImage().getWidth()
				|| mapData.getImage().getHeight() != inputData.getImage().getHeight()) {
			throw new IllegalArgumentException("Not all composite inputs are the same size");
		}
		
		// mark result as finished only when all inputs are finished
		// finished areas must be finished in all bands
		// fuzzy areas must be fuzzy in all bands
		finished[input] = inputData.isFinished();
		finalArea[input] = new Area(inputData.getFinishedArea());
		fuzzy[input] = new Area(inputData.getFuzzyArea());
		
		mapData.getFinishedArea().reset();
		mapData.getFinishedArea().add(new Area(mapData.getRequest().getExtent()));
		mapData.getFuzzyArea().reset();
		mapData.getFuzzyArea().add(new Area(mapData.getRequest().getExtent()));
		
		mapData.setFinished(true);
		for (int i = 0; i < finished.length; i++) {
			mapData.setFinished(mapData.isFinished() && finished[i]);
			mapData.getFinishedArea().intersect(finalArea[i]);
			mapData.getFuzzyArea().intersect(fuzzy[i]);
		}
		
		return mapData;
	}
	
	public Object clone() throws CloneNotSupportedException {
		CompositeStage stage = (CompositeStage)super.clone();
		stage.initData();
		return stage;
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		initData();
	}
}

