package edu.asu.jmars.layer.map2.stages;

import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.ConvolveOp;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.util.DebugLog;


public class LowPassFilterStage extends AbstractStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	
	private static DebugLog log = DebugLog.instance();
	
	public LowPassFilterStage(LowPassFilterStageSettings settings) {
		super(settings);
	}
	
	public String getStageName() {
		return getSettings().getStageName();
	}
	
	public int getInputCount() {
		return 1;
	}
	
	public MapData process(int inputNumber, MapData data, Area changedArea){
		BufferedImage image = data.getImage();
		if (image.getColorModel().getNumColorComponents() != 1) {
			throw new IllegalArgumentException("Input images must be single band images.");
		}
		
		// TODO probably need to do something additional here for proper output alpha-premultiplied handling (see bug: 3025) 
		image.coerceData(false); // have alpha separated out
		
		LowPassFilterStageSettings s = (LowPassFilterStageSettings)getSettings();
		float temp = 1.0f/(float) s.getGridSize();
		float[] imgData = new float[s.getGridSize() * s.getGridSize()];
		
		for (int x=0; x<imgData.length; x++) {
			imgData[x] = temp;//this only good for a low pass filter where all the values are constant and calculated as 1/gridSize
		}
			
		Kernel kernel = new Kernel(s.getGridSize(),s.getGridSize(), imgData);
		ConvolveOp convolve = new ConvolveOp(kernel,ConvolveOp.EDGE_NO_OP, null);
		BufferedImage outConvolve = null;
		outConvolve = convolve.filter(image, outConvolve);

		return data.getDeepCopyShell(outConvolve, null);
	}

	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.SINGLE_BAND };
	}
	
	public MapAttr produces(){
		return MapAttr.SINGLE_BAND;
	}

	public Object clone() throws CloneNotSupportedException {
		LowPassFilterStage stage = (LowPassFilterStage)super.clone();
		return stage;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
	
}
