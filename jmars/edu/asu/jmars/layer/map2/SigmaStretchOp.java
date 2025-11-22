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
import java.awt.image.DataBufferByte;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import edu.asu.jmars.util.Util;

public class SigmaStretchOp implements BufferedImageOp {
	
	double variance=40;
	double ignore=-1;
	
	// Used when displaying in 3D shapemodel view, to make the result consistent across all 8 ppd requests
	double avg=Double.NaN;
	double stdDev = Double.NaN;
	
	public SigmaStretchOp(double newVariance, double newIgnore){
		this(newVariance, newIgnore, Double.NaN, Double.NaN);
	}

	public SigmaStretchOp(double newVariance, double newIgnore, double newAvg, double newStdDev){
		variance = newVariance;
		ignore = newIgnore;
		avg = newAvg;
		stdDev = newStdDev;
	}
	
	public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
		int w = src.getWidth();
		int h = src.getHeight();
		
		WritableRaster outRaster = destCM.createCompatibleWritableRaster(w, h);
		
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
		
		final WritableRaster destRaster = dest.getRaster().createWritableChild(0, 0, w, h, 0, 0, getBands(src.getRaster().getNumBands()));
		final WritableRaster destAlpha = dest.getAlphaRaster();
				
		int numBands = src.getRaster().getNumBands();
				
		for (int band = 0; band<destCM.getNumColorComponents(); band++) {
			long sum = 0;
			
			int ignoreCnt = 0;
			
			for(int y=0; y<h; y++){
				for(int x=0; x<w; x++){
					long val = srcRaster.getSample(x, y, band);
					if (val==ignore || (srcAlpha!=null && srcAlpha.getSample(x,  y,  0)==0)) {
						ignoreCnt++;
						continue;
					}
					sum += val;
				}
			}

			// If we weren't given a specific avg to use, calculate it
			if (Double.isNaN(avg)) {
				avg = sum / ((w*h)-ignoreCnt);
			}
				
			double topSum = 0;
			
			ignoreCnt = 0;
			
			for(int y=0; y<h; y++){
				for(int x=0; x<w; x++){
					int pixelVal = srcRaster.getSample(x, y, band);
					if (pixelVal==ignore || (srcAlpha!=null && srcAlpha.getSample(x,  y,  0)==0)) {
						ignoreCnt++;
						continue;
					}
					topSum += Math.pow(pixelVal-avg, 2);
				}
			}
			
			// If we weren't given a specific stddev to use, calculate it
			if (Double.isNaN(stdDev)) {
				stdDev = Math.sqrt(topSum / ((w*h)-ignoreCnt));
			}
			
			for(int y=0; y<h; y++){
				for(int x=0; x<w; x++){
					int pixelVal = srcRaster.getSample(x, y, band);
					
					if (pixelVal==ignore) continue;
					
					int newVal = (int)((pixelVal-avg)*(variance/stdDev))+127;
					
					if (newVal<0) newVal=0;
					if (newVal>255) newVal=255;
					destRaster.setSample(x, y, band, newVal);
				}
			}
		}

		// Set the alpha value
		if (destCM.hasAlpha()) {
			WritableRaster alphaRaster = destCM.getAlphaRaster(destRaster);
			WritableRaster srcAlphaRaster = src.getAlphaRaster();
			
			for(int y=0; y<h; y++){
				for(int x=0; x<w; x++){
					if (srcAlphaRaster!=null) {
						alphaRaster.setSample(x, y, 0, srcAlphaRaster.getSample(x, y, 0));
					} else {
						alphaRaster.setSample(x, y, 0, 255);
					}
				}
			}
		}

		return dest;
	}

	
	/** Returns an array of band numbers from 0 inclusive to <code>count</code> exclusive */
	public int[] getBands(int count) {
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
		
		return srcPt;
	}

	public RenderingHints getRenderingHints() {
		return null;
	}

}
