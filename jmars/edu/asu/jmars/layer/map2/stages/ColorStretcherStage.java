package edu.asu.jmars.layer.map2.stages;

import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.StageSettings;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class ColorStretcherStage extends AbstractStage implements Cloneable, Serializable {
	private static final long serialVersionUID = -1320855669272199638L;
	private static final Object globalLock = new Object();
	public static DebugLog log = DebugLog.instance();
	
	public static String inputName = "Input";
	public static final String[] outputNames = new String[] {"Red", "Green", "Blue"};


	public ColorStretcherStage(StageSettings settings){
		super(settings);
	}
	
	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.GRAY, MapAttr.COLOR };
	}
	
	public MapAttr produces() {
		return MapAttr.COLOR;
	}
	
	public String getStageName() {
		return getSettings().getStageName();
	}

	public int getInputCount() {
		return 1;
	}
	
	public MapData process(int inputNumber, MapData data, Area changedArea) {
		BufferedImage image = data.getImage();

		// Create an output image which is compatible with the FancyColorMapper's color map op
		BufferedImage outImage = Util.newBufferedImage(image.getWidth(), image.getHeight());

		// the color convert op may be the cause of a relatively rare jvm crash
		// that is rumored to occur as a result of a race condition within
		// libcmm.so on linux versions of Java, that supposedly does not occur
		// if access to the module occurs in a single threaded fashion; now this
		// is by no means the only way within Java 2D to use the operator in
		// such a way, but with N CPUs, and therefore N pipelines, and very long
		// lists of tiles to filter, this could be a high frequency cause, so we
		// at least ensure that this location is synchronized
		synchronized(globalLock) {
			ColorConvertOp cco = new ColorConvertOp(null);
			cco.filter(image, outImage);
		}
		
		// TODO: fcm is a Swing object while the Stage is multi-threaded. How do we cope?
		// TODO: Don't know what alpha to use here, "1" seems like a reasonable choice.
		ColorStretcherStageSettings settings = (ColorStretcherStageSettings)getSettings(); 
		settings.getColorMapperState().getColorMapOp(outImage).forAlpha(1.0f).filter(outImage, outImage);
		

		/*
		 * For pixels using premultiplied RGBA, it is not sufficient to just set the A channel to zero for
		 * null pixels. One also has to multiply the R, G, B channels with the new A.
		 * Alternatively, one switch to non-premultiplied RGBA, then only needs to set A to zero for null pixels.
		 * This is what we do here.
		 */
		boolean alphaPre = outImage.isAlphaPremultiplied();
		outImage.coerceData(false);
		WritableRaster alpha = outImage.getAlphaRaster();
		if (alpha != null) {
			for (int j = image.getHeight()-1; j>=0; j--) {
				for (int i = image.getWidth()-1; i>=0; i--) {
					int curVal = alpha.getSample(i, j, 0);
					alpha.setSample(i, j, 0, data.isNull(i, j) ? 0 : curVal);
				}
			}
		}
		if (alphaPre != outImage.isAlphaPremultiplied()) {
			outImage.coerceData(alphaPre);
		}
		
		return data.getDeepCopyShell(outImage, null);
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
