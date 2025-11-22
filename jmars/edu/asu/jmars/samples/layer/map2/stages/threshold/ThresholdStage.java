package edu.asu.jmars.samples.layer.map2.stages.threshold;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.Hashtable;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.util.Util;

/**
 * A very simple thresholding stage. The threshold value is modifiable
 * in the view for this stage.
 */
public class ThresholdStage extends AbstractStage implements Cloneable, Serializable {
	public ThresholdStage(ThresholdSettings settings){
		super(settings);
	}
	
	public MapAttr[] consumes(int inputNumber) {
		if (inputNumber != 0)
			throw new IllegalArgumentException();
		
		return new MapAttr[]{ MapAttr.SINGLE_BAND };
	}

	public int getInputCount() {
		return 1;
	}

	public MapAttr produces() {
		return MapAttr.SINGLE_BAND;
	}
	
	public ThresholdSettings getSettings(){
		return ((ThresholdSettings)super.getSettings());
	}
	
	public MapData process(int inputNumber, MapData data, Area changedArea) {
		if (inputNumber != 0)
			throw new IllegalArgumentException();
		
		double threshold = getSettings().getThreshold();
		BufferedImage inImage = data.getImage();
		Raster inRaster = inImage.getRaster();
		
		int w = inImage.getWidth();
		int h = inImage.getHeight();

		BufferedImage outImage = createGrayscaleImage(w, h, true, BufferedImage.OPAQUE, null);
		WritableRaster outRaster = outImage.getRaster();
		
		// Fill output image via thresholding
		for(int j=0; j<h; j++){
			for(int i=0; i<w; i++){
				outRaster.setSample(i, j, 0, (inRaster.getSampleDouble(i, j, 0) >= threshold)? 255: 0);
			}
		}

		// Set the output changed area, which can be smaller/larger
		// than the input changed area.
		changedArea.reset();
		changedArea.add(new Area(data.getRequest().getExtent()));
		
		// Finally return a safe output image that the receiver can 
		// modify with impunity.
		return data.getDeepCopyShell(outImage, null);
	}

    private static final BufferedImage createGrayscaleImage(int w, int h, boolean linearColorSpace, int transparency, Hashtable properties){
    	ColorSpace destCS = linearColorSpace? Util.getLinearGrayColorSpace(): ColorSpace.getInstance(ColorSpace.CS_GRAY);
    	ColorModel destCM;
    	if (transparency == Transparency.OPAQUE) 
    		destCM = new ComponentColorModel(destCS, false, false, transparency, DataBuffer.TYPE_BYTE);
    	else
    		destCM = new ComponentColorModel(destCS, true, false, transparency, DataBuffer.TYPE_SHORT);
    	
		return new BufferedImage(destCM, destCM.createCompatibleWritableRaster(w, h), destCM.isAlphaPremultiplied(), properties);
    }
    
	public String getStageName(){
		return getSettings().getStageName();
	}
	
	public Object clone() throws CloneNotSupportedException {
		ThresholdStage stage = (ThresholdStage)super.clone();
		return stage;
	}
}
