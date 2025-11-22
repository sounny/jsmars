package edu.asu.jmars.layer.map2.stages;

import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.Serializable;
import java.util.Arrays;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.stages.ThresholdSettings.ThresholdMode;
import edu.asu.jmars.util.Util;

/**
 * A very simple thresholding stage. The threshold value is modifiable
 * in the view for this stage.
 */
public class ThresholdStage extends AbstractStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;

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
	
	/**
	 * @param dataType {@link DataBuffer#getDataType()}
	 * @return The minimum value of for the specified data type.
	 */
	private double getMinValue(int dataType){
		double val = 0;
		
		switch(dataType){
		case DataBuffer.TYPE_BYTE:   val = Byte.MIN_VALUE;    break;
		case DataBuffer.TYPE_SHORT:  val = Short.MIN_VALUE;   break;
		case DataBuffer.TYPE_USHORT: val = 0;                 break;
		case DataBuffer.TYPE_INT:    val = Integer.MIN_VALUE; break;
		case DataBuffer.TYPE_FLOAT:  val = Float.MIN_VALUE;   break;
		case DataBuffer.TYPE_DOUBLE: val = Double.MIN_VALUE;  break;
		default:
			throw new IllegalArgumentException("Unhandled DataBuffer data type "+dataType);
		}
		return val;
	}
	
	public MapData process(int inputNumber, MapData data, Area changedArea) {
		if (inputNumber != 0)
			throw new IllegalArgumentException();

		// Get stage settings
		double threshold = getSettings().getThreshold();
		boolean binaryOutput = getSettings().getBinaryOutput();
		ThresholdMode thresholdMode = getSettings().getMode();
		double[] srcNullPixel = data.getNullPixel();
		
		BufferedImage srcImage = data.getImage();
		Raster srcRaster = srcImage.getRaster();
		Raster srcAlphaRaster = srcImage.getAlphaRaster();
		boolean srcImgCanHaveAlpha = srcRaster.getDataBuffer().getDataType() == DataBuffer.TYPE_BYTE; 
		
		int w = srcImage.getWidth();
		int h = srcImage.getHeight();

		BufferedImage outImage;
		if (binaryOutput || (srcImgCanHaveAlpha && srcAlphaRaster == null))
			outImage = Util.createGrayscaleImage(w, h, true);
		else
			outImage = Util.createCompatibleImage(srcImage, w, h);
		
		WritableRaster outRaster = outImage.getRaster();
		WritableRaster outAlphaRaster = outImage.getAlphaRaster();
		
		if (srcNullPixel == null && outAlphaRaster == null){
			srcNullPixel = new double[srcRaster.getNumBands()];
			Arrays.fill(srcNullPixel, getMinValue(srcRaster.getDataBuffer().getDataType()));
		}
		
		// TODO Processing of changed rectangles impeded by unavailability
		//      of read-only sub-Raster extraction via MapData.getRasterForWorld().
		//      Also, I am uncertain that downstream code is clean w.r.t. honoring
		//      the changed-area.
		
		// Fill output image via thresholding
		for(int j=0; j<h; j++){
			for(int i=0; i<w; i++){
				
				// TODO Line by line processing impeded by unavailability of
				//      a vector data.isNull() method.
				
				double sample = srcRaster.getSampleDouble(i, j, 0);
				boolean pass = false;
				if (!data.isNull(i,j)) {
					switch (thresholdMode){
					case MODE_GE: pass = (sample >= threshold); break;
					case MODE_GT: pass = (sample >  threshold); break;
					case MODE_LE: pass = (sample <= threshold); break;
					case MODE_LT: pass = (sample <  threshold); break;
					case MODE_EQ: pass = (sample == threshold); break;
					case MODE_NE: pass = (sample != threshold); break;
					default: pass = true; break;
					}
				}
				
				if (outAlphaRaster != null){
					// Mask only using alpha-channel.
					outAlphaRaster.setSample(i, j, 0, srcAlphaRaster == null? pass? 255:0:
						pass? srcAlphaRaster.getSample(i, j, 0): 0);
					outRaster.setSample(i, j, 0, binaryOutput? pass? 255: 0: sample);
				}
				else {
					outRaster.setSample(i, j, 0, binaryOutput? pass? 255: 0: pass? sample: srcNullPixel[0]);
				}
			}
		}

		// Set the output changed area, which can be smaller/larger
		// than the input changed area.
		changedArea.reset();
		changedArea.add(new Area(data.getRequest().getExtent()));
		
		// Finally return a safe output image that the receiver can 
		// modify with impunity.
		return data.getDeepCopyShell(outImage, binaryOutput? null: srcNullPixel);
	}

	public String getStageName(){
		return getSettings().getStageName();
	}
	
	public Object clone() throws CloneNotSupportedException {
		ThresholdStage stage = (ThresholdStage)super.clone();
		return stage;
	}
}
