package edu.asu.jmars.layer.map2.stages;


import java.awt.color.ColorSpace;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.GrayRescaleToByteOp;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.SigmaStretchOp;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.PolyArea;
import edu.asu.jmars.util.Util;

/**
 * Converts the image stored in the input MapData object to a byte image, with
 * an alpha band if the input image had an alpha band or if there is an ignore
 * value defined on the map source.
 */
public class SigmaStretchStage extends AbstractStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 2L;
	
	public SigmaStretchStage(SigmaStretchStageSettings settings) {
		super(settings);
	}
	
	public String getStageName() {
		return getSettings().getStageName();
	}
	
	public int getInputCount() {
		return 1;
	}
	
	public MapData process(int inputNumber, MapData data, Area changedArea) {
		BufferedImage image = data.getImage();
		
		// Convert from source # bits to 8-bit data per plane
		image.coerceData(false); // have alpha separated out
		
		int w = image.getWidth();
		int h = image.getHeight();
		double ignore = getIgnore(data);

		SigmaStretchStageSettings settings = ((SigmaStretchStageSettings)getSettings());
		
		if (data.getRequest().getPPD()<settings.getMinPPD()) {
			return data.getDeepCopyShell(image, null);
		} 

		BufferedImage outImage = Util.newBufferedImage(w, h);
		
		SigmaStretchOp rescaleOp = new SigmaStretchOp(settings.getVariance(), ignore);

		BufferedImage src = image;
		
		final ColorModel cm = src.getColorModel();
		if (!(cm instanceof DirectColorModel || cm instanceof ComponentColorModel))
			throw new IllegalArgumentException("Unsupported color model :"+cm.getClass().getName()+".");
		
		// set up the destination
		final ColorModel destCM;
		
		if (cm.getNumColorComponents()>=3) {
			destCM = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
					cm.hasAlpha(), false,
					cm.hasAlpha()? ColorModel.TRANSLUCENT: ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
		} else {
			destCM = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
					cm.hasAlpha(), false,
					cm.hasAlpha()? ColorModel.TRANSLUCENT: ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
		}
		
		outImage = rescaleOp.createCompatibleDestImage(src, destCM);

		if (data.isFinished()) {
			rescaleOp.filter(image, outImage);
			
			if (!Double.isNaN(ignore) && outImage.getAlphaRaster()!=null) {
				Ignore tool = createIgnoreFromType(image.getRaster().getTransferType(), ignore);
				Object rpixels = null;
				byte[] apixels = null;
				WritableRaster rdata = Util.getBands(image, 0);
				WritableRaster adata = Util.getBands(outImage, 1);
				
				for (int row = 0; row < h; row++) {
					rpixels = rdata.getDataElements(0, row, w, 1, rpixels);
					apixels = (byte[])adata.getDataElements(0, row, w, 1, apixels);
					tool.setAlpha(rpixels, apixels);
					adata.setDataElements(0, row, w, 1, apixels);
				}
			}
		} else {
			// TODO: Probably not right
			//	changedArea.reset();
		}
		
		return data.getDeepCopyShell(outImage, null);
	}
	
	/**
	 * Returns an Ignore instance optimized for the given DataBuffer type.
	 * 
	 * Note that returning the same final type greatly in this way greatly
	 * increases the level of optimization this code will achieve.
	 */
	private Ignore createIgnoreFromType(int dataType, double ignore) {
		switch(dataType) {
		case DataBuffer.TYPE_BYTE: return new ByteIgnore(ignore);
		case DataBuffer.TYPE_SHORT:
		case DataBuffer.TYPE_USHORT: return new ShortIgnore(ignore);
		case DataBuffer.TYPE_INT: return new IntIgnore(ignore);
		case DataBuffer.TYPE_FLOAT: return new FloatIgnore(ignore);
		case DataBuffer.TYPE_DOUBLE: return new DoubleIgnore(ignore);
		default: throw new IllegalArgumentException("Image has unrecognized data type " + dataType);
		}
	}
	
	private static interface Ignore {
		void setAlpha(Object data, byte[] alpha);
	}
	
	private static final class ByteIgnore implements Ignore {
		private final byte ignore;
		public ByteIgnore(double ignore) {
			this.ignore = (byte)ignore;
		}
		public void setAlpha(Object data, byte[] alpha) {
			byte[] bdata = (byte[])data;
			for (int i = 0; i < bdata.length; i++) {
				if (bdata[i] == ignore) {
					alpha[i] = (byte)0;
				}
			}
		}
	}
	
	/**
	 * Should handle signed or unsigned short values, since Java casts from
	 * double to short for unsigned numbers do end up with the bits in the right
	 * place (e.g. (short)32768 == -32768)
	 */
	private static final class ShortIgnore implements Ignore {
		private final short ignore;
		public ShortIgnore(double ignore) {
			this.ignore = (short)ignore;
		}
		public void setAlpha(Object data, byte[] alpha) {
			short[] sdata = (short[])data;
			for (int i = 0; i < sdata.length; i++) {
				if (sdata[i] == ignore) {
					alpha[i] = (byte)0;
				}
			}
		}
	}
	
	private static final class IntIgnore implements Ignore {
		private final int ignore;
		public IntIgnore(double ignore) {
			this.ignore = (short)ignore;
		}
		public void setAlpha(Object data, byte[] alpha) {
			int[] sdata = (int[])data;
			for (int i = 0; i < sdata.length; i++) {
				if (sdata[i] == ignore) {
					alpha[i] = (byte)0;
				}
			}
		}
	}
	
	private static final class FloatIgnore implements Ignore {
		private final float ignore;
		public FloatIgnore(double ignore) {
			this.ignore = (float)ignore;
		}
		public void setAlpha(Object data, byte[] alpha) {
			float[] sdata = (float[])data;
			for (int i = 0; i < sdata.length; i++) {
				if (sdata[i] == ignore) {
					alpha[i] = (byte)0;
				}
			}
		}
	}
	
	private static final class DoubleIgnore implements Ignore {
		private final double ignore;
		public DoubleIgnore(double ignore) {
			this.ignore = ignore;
		}
		public void setAlpha(Object data, byte[] alpha) {
			double[] sdata = (double[])data;
			for (int i = 0; i < sdata.length; i++) {
				if (sdata[i] == ignore) {
					alpha[i] = (byte)0;
				}
			}
		}
	}
		
	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.SINGLE_BAND, MapAttr.GRAY, MapAttr.COLOR };

	}
	
	public MapAttr produces(){
		return MapAttr.COLOR;
	}

	public Object clone() throws CloneNotSupportedException {
		SigmaStretchStage stage = (SigmaStretchStage)super.clone();
		return stage;
	}
	
	/** @return the ignore value for the first band from the given MapData object */
	private double getIgnore(MapData data) {
		double[] ignoreArray = data.getNullPixel();
		return ignoreArray == null ? Double.NaN : ignoreArray[0];
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
	}
}
