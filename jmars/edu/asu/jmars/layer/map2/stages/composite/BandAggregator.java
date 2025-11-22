package edu.asu.jmars.layer.map2.stages.composite;

import java.awt.geom.Area;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;

import edu.asu.jmars.graphics.EmptyColorModel;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;

/**
 * Aggregates N 1-band input images into one N-band output image. The aggregated
 * image will use the supplied ColorModel to visualize the data.
 * 
 * When the first input arrives, its width and height are used to create the
 * output image, with dimensions width by height by N, where N is the number of
 * input bands in this class' constructor. All cells are initially Float.NaN.
 * 
 * Each call to process() slips the single band input image into the input band
 * in the output image. The single band of data can be any data type, although
 * if it's not the same as the other buffers, care must be taken to ensure the
 * ColorModel, ColorSpace, and SampleModel can support non-homogeneous buffer
 * types. Care must also be taken to avoid modifying the inputs after they're
 * received by this class, since they are NOT copied.
 * 
 * All inputs must be single band images with the same view proj/ppd/extent.
 * The finished property is true when all inputs are finished.
 * The fuzzy area reflects the area which is fuzzy in all the input images.
 * The finished area reflects the area which is finished in all the input images.
 */
public final class BandAggregator extends CompositeStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 2L;
	
	private static final ColorModel cm = new EmptyColorModel();
	
	
	public BandAggregator(BandAggregatorSettings settings) {
		super(settings);
	}
	
	public int getInputCount(){
		return ((BandAggregatorSettings)getSettings()).getInputCount();
	}
	
	public String getInputName(int inputNumber){
		return ((BandAggregatorSettings)getSettings()).getInputName(inputNumber);
	}
	
	public String[] getInputNames(){
		return ((BandAggregatorSettings)getSettings()).getInputNames();
	}
	
	public MapData process(int input, MapData inputData, Area changedArea) {
		MapData mapData = super.process(input, inputData, changedArea);
		
		int w = mapData.getImage().getWidth();
		int h = mapData.getImage().getHeight();
		int[] bands = {input};
		WritableRaster target = mapData.getImage().getRaster().createWritableChild(0, 0, w, h, 0, 0, bands);
		WritableRaster source = inputData.getImage().getRaster().createWritableChild(0, 0, w, h, 0, 0, new int[]{0});
		target.setRect(source);
		
		return mapData;
	}
	
	public BufferedImage makeBufferedImage(int width, int height) {
		SampleModel sm = new BandedSampleModel(DataBuffer.TYPE_DOUBLE, width, height, getInputCount());
		WritableRaster raster = Raster.createWritableRaster(sm, null);
		
		// Initially fill the raster with NaNs
		double[] nans = new double[width*height*getInputCount()];
		Arrays.fill(nans, Double.NaN);
		raster.setDataElements(0, 0, width, height, nans);
		
		return new BufferedImage(cm, raster, false, null);
	}
	
	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.ANY };
	}
	
	public MapAttr produces() {
		return new MapAttr(DataBuffer.TYPE_DOUBLE, getInputCount(), null);
	}

	public String getStageName(){
		return getSettings().getStageName();
	}
	
	public Object clone() throws CloneNotSupportedException {
		BandAggregator stage = (BandAggregator)super.clone();
		return stage;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}

}

