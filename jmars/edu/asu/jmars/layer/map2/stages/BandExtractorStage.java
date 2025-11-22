package edu.asu.jmars.layer.map2.stages;

import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.util.Util;

public class BandExtractorStage extends AbstractStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 986482229581280926L;

	public BandExtractorStage(StageSettings settings){
		super(settings);
	}
	
	public String getStageName() {
		return getSettings().getStageName();
	}
	
	public int getInputCount(){
		return 1;
	}

	public MapData process(int inputNumber, MapData data, Area changedArea) {
		BandExtractorStageSettings settings = (BandExtractorStageSettings)getSettings();
		String band = settings.getSelectedBand();
		int bandNumber = Arrays.asList(settings.getBands()).indexOf(band);
		if (bandNumber < 0)
			throw new IllegalArgumentException("Invalid selected band: "+band);
		
		BufferedImage image = data.getImage();
		// TODO probably need to do something additional here for proper output alpha-premultiplied handling (see bug: 3025) 
		// note: image.coerceData(false) does not impact the image type, i.e., if it were ARGB_PRE it will remain that
		image.coerceData(false);
		Raster r = image.getRaster();
		r = r.createChild(r.getMinX(), r.getMinY(), r.getWidth(), r.getHeight(), r.getMinX(), r.getMinY(), new int[]{ bandNumber });
		
		WritableRaster outRaster = r.createCompatibleWritableRaster();
		ColorModel outCM = new ComponentColorModel(Util.getLinearGrayColorSpace(),
				false, false, BufferedImage.OPAQUE, outRaster.getTransferType());
		BufferedImage outImage = new BufferedImage(outCM, outRaster, outCM.isAlphaPremultiplied(), null);
		
		outImage.getRaster().setRect(r);
		//if (image.getAlphaRaster() != null && outImage.getAlphaRaster() != null)
		//	outImage.getAlphaRaster().setRect(image.getAlphaRaster());
		
		double[] oldNull = data.getNullPixel();
		double[] newNull = oldNull == null || bandNumber >= oldNull.length  ? null : new double[]{oldNull[bandNumber]};
		return data.getDeepCopyShell(outImage, newNull);
	}

	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.ANY };
	}
	
	public MapAttr produces() {
		return MapAttr.SINGLE_BAND;
	}
	
	public Object clone() throws CloneNotSupportedException {
		BandExtractorStage s = (BandExtractorStage)super.clone();
		return s;
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
