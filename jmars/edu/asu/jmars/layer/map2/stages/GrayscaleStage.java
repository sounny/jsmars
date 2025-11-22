package edu.asu.jmars.layer.map2.stages;


import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.asu.jmars.layer.map2.AbstractStage;
import edu.asu.jmars.layer.map2.GrayRescaleToByteOp;
import edu.asu.jmars.layer.map2.MapAttr;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.PolyArea;
import edu.asu.jmars.util.Util;

/**
 * Converts the image stored in the input MapData object to a byte image, with
 * an alpha band if the input image had an alpha band or if there is an ignore
 * value defined on the map source.
 */
public class GrayscaleStage extends AbstractStage implements Cloneable, Serializable {
	private static final long serialVersionUID = 2L;

	private static DebugLog log = DebugLog.instance();
	
	private static final double DELTA_VAL = 0.000001;
	
	public GrayscaleStage(GrayscaleStageSettings settings) {
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
		if (image.getColorModel().getNumColorComponents() != 1)
			throw new IllegalArgumentException("Input images must be single band images.");
		
		// TODO likely need to do something additional here for the output image to handle premultiplied ARGB correctly (see bug 3025)
		// Convert from source # bits to 8-bit data per plane
		image.coerceData(false); // have alpha separated out
		
		int w = image.getWidth();
		int h = image.getHeight();
		double ignore = getIgnore(data);
		boolean outputAlpha = !Double.isNaN(ignore) || image.getColorModel().hasAlpha();
		
		double[] minMax = getMinMax(data, changedArea);
		double minValue = minMax[0];
		double maxValue = minMax[1];
		
		log.println("GrayscaleStage: "+minValue+","+maxValue);
		
		// create output image
		BufferedImage outImage = Util.createGrayscaleImage(w, h, outputAlpha);
		
		// rescale the data band
		double diff = maxValue - minValue;
		double scaleFactor = diff == 0? 0: 255.0 / (maxValue - minValue);
		double offset = diff == 0? 0: -255 * minValue / (maxValue - minValue);
		if (Double.isInfinite(minValue) || Double.isInfinite(maxValue))
			offset = scaleFactor = 0;
		GrayRescaleToByteOp rescaleOp = new GrayRescaleToByteOp((float)scaleFactor, (float)offset);
		rescaleOp.filter(image, outImage);
		
		// if an ignore value is defined, then since we have already ensured there
		// is an alpha band, go set those pixels to transparent where the data is
		// equal to the ignore value
		if (!Double.isNaN(ignore)) {
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
				if (Math.abs(sdata[i]-ignore)<DELTA_VAL || Float.isNaN(sdata[i])) {
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
				if (Math.abs(sdata[i]-ignore)<DELTA_VAL || Double.isNaN(sdata[i])) {
					alpha[i] = (byte)0;
				}
			}
		}
	}
	
	/**
	 * Computes the min/max range for this stretch and updates the settings if
	 * 'auto' is set and a new min and/or max value is found. Will avoid
	 * 'ignore' pixels if an ignore value is set, and will skip alpha
	 * transparent pixels if ignore is unset
	 * 
	 * @param data
	 *            The data object for the whole request
	 * @param changedArea
	 *            The area affected by the last stage; changes to this area will
	 *            affect this and future stages!
	 * @return [min, max]
	 */
	private double[] getMinMax(MapData data, Area changedArea) {
		GrayscaleStageSettings s = (GrayscaleStageSettings)getSettings();
		
		boolean auto;
		double ignore;
		double min, max, oldMin, oldMax;
		synchronized(s) {
			auto = s.getAutoMinMax();
			ignore = getIgnore(data);
			oldMin = min = s.getMinValue();
			oldMax = max = s.getMaxValue();
		}
		
		Area toProcess = new Area();
		toProcess.add(changedArea);
		toProcess.intersect(new Area(data.getRequest().getExtent()));
		
		if (toProcess.isEmpty())
			return new double[]{min,max};
		
		if (auto) {
			// determine min/max range of each changed block
			BufferedImage bi = data.getImage();
			WritableRaster inRaster = Util.getColorRaster(bi);
			WritableRaster inRasterAlpha = bi.getAlphaRaster();
			Rectangle2D inExtent = data.getRequest().getExtent();
			for (Rectangle2D changedRect: new PolyArea(toProcess).getRectangles()) {
				Raster changedRaster = MapData.getRasterForWorld(inRaster, inExtent, changedRect);
				int x = changedRaster.getWidth();
				int y = changedRaster.getHeight();
				double[] pixels = new double[x];
				if (inRasterAlpha == null || !Double.isNaN(ignore)) {
					// use ignore value
					for (int j = 0; j < y; j++) {
						changedRaster.getPixels(0, j, x, 1, pixels);
						for (int i = 0; i < x; i++) {
							if ((Double.isNaN(ignore) || Math.abs(pixels[i]-ignore)>=DELTA_VAL) && !Double.isNaN(pixels[i])) {
								min = Math.min(min, pixels[i]);
								max = Math.max(max, pixels[i]);
							}
						}
					}
				} else {
					// use alpha band
					Raster changedRasterAlpha = MapData.getRasterForWorld(inRasterAlpha, inExtent, changedRect);
					int[] alpha = new int[x];
					for (int j = 0; j < y; j++) {
						changedRaster.getPixels(0, j, x, 1, pixels);
						changedRasterAlpha.getPixels(0, j, x, 1, alpha);
						for (int i = 0; i < x; i++) {
							if (alpha[i] != 0) {
								min = Math.min(min, pixels[i]);
								max = Math.max(max, pixels[i]);
							}
						}
					}
				}
			}
			
			synchronized (s) {
				s.setMinValue(Math.min(min, s.getMinValue()));
				s.setMaxValue(Math.max(max, s.getMaxValue()));
				min = s.getMinValue();
				max = s.getMaxValue();
			}
			
			if (oldMin != min || oldMax != max) {
				changedArea.reset();
				changedArea.add(data.getValidArea());
			}
		}
		
		return new double[]{min,max};
	}
	
	public MapAttr[] consumes(int inputNumber){
		return new MapAttr[]{ MapAttr.SINGLE_BAND };
	}
	
	public MapAttr produces(){
		return MapAttr.GRAY;
	}

	public Object clone() throws CloneNotSupportedException {
		GrayscaleStage stage = (GrayscaleStage)super.clone();
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
