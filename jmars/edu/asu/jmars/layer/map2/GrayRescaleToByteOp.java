package edu.asu.jmars.layer.map2;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.RescaleOp;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;

import edu.asu.jmars.util.Util;

/**
 * This operator requires the input and output rasters to have the same number
 * of bands.
 * 
 * Note that this operator only handles alpha values for a DirectColorModel or a
 * ComponentColorModel, other ColorModels are not handled.
 */
public final class GrayRescaleToByteOp implements BufferedImageOp {
	private final float scaleFactor;
	private final float offset;
	
	public GrayRescaleToByteOp(float scaleFactor, float offset) {
		this.scaleFactor = scaleFactor;
		this.offset = offset;
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
		int w = src.getWidth();
		int h = src.getHeight();
		
		SampleModel outModel = new BandedSampleModel(DataBuffer.TYPE_BYTE, w, h, destCM.getNumComponents());
		WritableRaster outRaster = Raster.createWritableRaster(outModel, null);
		BufferedImage outImage = new BufferedImage(destCM, outRaster, destCM.isAlphaPremultiplied(), null);
		
		return outImage;
	}
	
	public BufferedImage filter(final BufferedImage src, BufferedImage dest) {
		// set up the source
		final int w = src.getWidth();
		final int h = src.getHeight();
		final ColorModel cm = src.getColorModel();
		if (!(cm instanceof DirectColorModel || cm instanceof ComponentColorModel))
			throw new IllegalArgumentException("Unsupported color model :"+cm.getClass().getName()+".");
		final Raster srcRaster = src.getRaster().createWritableChild(0, 0, w, h, 0, 0, getBands(cm.getNumColorComponents()));
		final Raster srcAlpha = src.getAlphaRaster();
		
		// set up the destination
		final ColorModel destCM;
		if (dest == null){
			destCM = new ComponentColorModel(
					Util.getLinearGrayColorSpace(), cm.hasAlpha(), false,
					cm.hasAlpha()? ColorModel.TRANSLUCENT: ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
			dest = createCompatibleDestImage(src, destCM);
		} else {
			destCM = dest.getColorModel();
		}
		final WritableRaster destRaster = dest.getRaster().createWritableChild(0, 0, w, h, 0, 0, getBands(destCM.getNumColorComponents()));
		final WritableRaster destAlpha = dest.getAlphaRaster();
		
		// do an optimized filter using RescaleOp for the types it supports, do it manually otherwise
		final int srcTransferType = srcRaster.getTransferType();
		switch(srcTransferType) {
		case DataBuffer.TYPE_BYTE:
		case DataBuffer.TYPE_SHORT:
		case DataBuffer.TYPE_USHORT:
		case DataBuffer.TYPE_INT:
			// Do the rescale the fast way
			new RescaleOp(scaleFactor, offset, getRenderingHints()).filter(srcRaster, destRaster);
			break;
		case DataBuffer.TYPE_FLOAT:
			// Do the slower way, optimized a bit for float
			int[] destRow = new int[w];
			float[] floatRow = null;
			float f;
			for (int row = 0; row < h; row++) {
				floatRow = srcRaster.getPixels(0, row, w, 1, floatRow);
				for (int i=0; i<floatRow.length; i++) {
					f = floatRow[i]*scaleFactor + offset;
					if (f > 255f)
						destRow[i] = 255;
					else if (f < 0f)
						destRow[i] = 0;
					else
						destRow[i] = (int)f;
				}
				destRaster.setPixels(0, row, w, 1, destRow);
			}
			break;
		case DataBuffer.TYPE_DOUBLE:
			// Do the slower way, optimized a bit for double
			destRow = new int[w];
			double[] dblRow = null;
			double d;
			for (int row = 0; row < h; row++) {
				dblRow = srcRaster.getPixels(0, row, w, 1, dblRow);
				for (int i = 0; i < dblRow.length; i++) {
					d = dblRow[i]*scaleFactor + offset;
					if (d > 255d)
						destRow[i] = 255;
					else if (d < 0d)
						destRow[i] = 0;
					else
						destRow[i] = (int)d;
				}
				destRaster.setPixels(0, row, w, 1, destRow);
			}
			break;
		default:
			throw new IllegalArgumentException("Unhandled src image data (transfer) type "+srcTransferType);
		}
		
		if (destAlpha != null) {
			// set the output alpha to something
			if (srcAlpha != null && destAlpha.getTransferType() == srcAlpha.getTransferType()) {
				// copy a compatible source alpha band to destination
				destAlpha.setRect(srcAlpha);
			} else {
				// If no alpha or its not compatible, make the whole dest image opaque
				fillRaster(destAlpha, 255);
			}
		}
		
		return dest;
	}

	private void fillRaster(WritableRaster raster, int val){
		int w = raster.getWidth();
		int h = raster.getHeight();
		int[] alpha = new int[w*h];
		Arrays.fill(alpha, val);
		raster.setPixels(0, 0, w, h, alpha);
	}
	
	/** Returns an array of band numbers from 0 inclusive to <code>count</code> exclusive */
	private int[] getBands(int count) {
		int[] bands = new int[count];
		for (int i = 0; i < bands.length; i++)
			bands[i] = i;
		return bands;
	}
	
	public Rectangle2D getBounds2D(BufferedImage src) {
		return src.getData().getBounds();
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		if (dstPt == null)
			dstPt = new Point2D.Double();
		dstPt.setLocation(srcPt);
		
		return dstPt;
	}

	public RenderingHints getRenderingHints() {
		return null;
	}

}
