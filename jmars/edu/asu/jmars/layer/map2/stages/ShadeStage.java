package edu.asu.jmars.layer.map2.stages;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Area;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.ReliefShadeOp;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;


public class ShadeStage extends AbstractStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 1L;
	
	private static DebugLog log = DebugLog.instance();
	
	public ShadeStage(ShadeStageSettings settings) {
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
		
		if (image.getColorModel().getNumColorComponents() != 1)
			throw new IllegalArgumentException("Input images must be single band images.");
		
		// TODO probably need to do something additional here for proper output alpha-premultiplied handling (see bug: 3025) 
		image.coerceData(false); // have alpha separated out
		
		int w = image.getWidth();
		int h = image.getHeight();
		boolean outputAlpha = image.getColorModel().hasAlpha();//TODO: do conditions exist where we need to address the alpha?
		double ppd = data.getRequest().getPPD();
		
		//we will use a max ppd, check to see if the current ppd is higher than that. If it is, we will create a new BufferedImage, 
		//populate it by scaling it down to the max ppd, then running it through the ReliefShadeOp, and then scale the image back to its original
		//ppd
		int maxPPD = new Double(data.getRequest().getSource().getMaxPPD()).intValue(); 
		int currentPPD = new Double(ppd).intValue();
		WritableRaster tempRaster = null;
		
		if (currentPPD > maxPPD) {
			int factor = currentPPD / maxPPD;
			Raster newRaster = image.getData();
			int width = w/factor;
			int height = h/factor;
			SampleModel sm = new BandedSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1);
			tempRaster = Raster.createWritableRaster(sm, null);
			
			//scale down the image to max ppd
			double dArray[] = null;
			for(int y=0; y<height; y++){
				for(int x=0; x<width; x++){
					dArray = newRaster.getPixel(x * factor, y * factor, dArray);
					tempRaster.setPixel(x, y, dArray);
				}
			}
		} else {
			tempRaster = image.getRaster();
		}
		
		ShadeStageSettings s = (ShadeStageSettings)getSettings();
		double radius = Util.MEAN_RADIUS * 1000;
		double cellSize = ((2*Math.PI*radius)/360.0)/ppd;
		
		ReliefShadeOp op = new ReliefShadeOp(s.getAz(), s.getEl(), cellSize);
		WritableRaster outRaster = op.createCompatibleDestRaster(tempRaster);
		ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), 
				false, false, Transparency.OPAQUE, outRaster.getTransferType());
		BufferedImage outImage = new BufferedImage(cm, outRaster, false, null);
		op.filter(tempRaster, outRaster);
		
		WritableRaster finalRaster = null;
		BufferedImage finalImage = null;
		if (currentPPD > maxPPD) {
			int factor = currentPPD / maxPPD;
			Raster newRaster = outImage.getData();
			int width = newRaster.getWidth() * factor;
			int height = newRaster.getHeight() * factor;
			SampleModel sm = new BandedSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1);
			finalRaster = Raster.createWritableRaster(sm, null);
			
			//scale up the image to current ppd
			double dArray[] = null;
			for(int y=0; y<height; y++){
				for(int x=0; x<width; x++){
					dArray = newRaster.getPixel(x/factor, y/factor, dArray);
					finalRaster.setPixel(x, y, dArray);
				}
			}
			ColorModel cm2 = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY), 
					false, false, Transparency.OPAQUE, finalRaster.getTransferType());
			finalImage = new BufferedImage(cm2, finalRaster, false, null);
		} else {
			finalImage = outImage;
		}
		
		return data.getDeepCopyShell(finalImage, null);
	}

	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.SINGLE_BAND };
	}
	
	public MapAttr produces(){
		return MapAttr.SINGLE_BAND;
	}

	public Object clone() throws CloneNotSupportedException {
		ShadeStage stage = (ShadeStage)super.clone();
		return stage;
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
	
}
