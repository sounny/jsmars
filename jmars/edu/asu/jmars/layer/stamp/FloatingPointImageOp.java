package edu.asu.jmars.layer.stamp;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/*
 * This BufferedImageOp takes a BufferedImage containing true floating point values and converts it into 8-bit space for display to the user.
 * 
 * If min/max values for the image range are known in advance, this operatation will perform a linear stretch between those two values.
 * If min/max values are not known in advance, this first scans through the data, then performs a linear stretch.
 * When the data is scanned for min/max values, extreme values are detected and ignored because many data sets use large negative values
 * as ignore values - but not consistently the same value.  
 */
public class FloatingPointImageOp implements BufferedImageOp {
	
	public double imageMin=Double.NaN;
	public double imageMax=Double.NaN;
	
	public StampImage stampImage;
	
	FloatingPointImageOp(StampImage wholeImage) {
		stampImage=wholeImage;
		imageMin=wholeImage.minValue;
		imageMax=wholeImage.maxValue;
	}
	
	public RenderingHints getRenderingHints() {
		return null;
	}

	public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
		// This op doesn't change the location of points
		return srcPt;
	}

	public Rectangle2D getBounds2D(BufferedImage src) {
		// Unimplemented
		return null;
	}

	public BufferedImage filter(BufferedImage src, BufferedImage dest) {
		if (dest==null) {
			dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
//			dest=new BufferedImage(src.getColorModel(), src.copyData(null), false, null);
		}
		
		int w= src.getWidth();
		int h = src.getHeight();
		
		double min = Double.MAX_VALUE;
		double min2 = Double.MAX_VALUE;
		double max = Double.NEGATIVE_INFINITY;
		
		Raster srcRaster = src.getRaster();
		WritableRaster destRaster = dest.getRaster();
		
		double values[] = new double[w*h];
		int newValues[] = new int[w*h];

		// Getting the data in bulk is substantially faster than reading it pixel by pixel
		srcRaster.getSamples(0, 0, w, h, 0, values);

		if (Double.isNaN(imageMin) || Double.isNaN(imageMax)) {
			// If we weren't told what min/max range to use, scan the data and determine them ourselves
			for (int i=0; i<values.length; i++) {
					double v = values[i];
					if (v == StampImage.IGNORE_VALUE) continue;
					if (v == -Float.MAX_VALUE) continue;   
//					if (v == 0) continue;   // treat as an ignore value
					if (v>max) max = v;
					if (v<min) min = v;
			}

			// Make a second scan through the data to find the second lowest value, ignoring extreme negative values
			// Many datasets use 0 as a null/ignore value.  If the rest of the range is in the 100-200 range, stretching from 0-200 gives a much less
			// useful result than stretching from 100-200.
			for (int i=0; i<values.length; i++) {
					double v = values[i];
					if (v<-300000000) continue;
					if (v<=min) continue;
					if (v<min2 && v!=min) min2 = v;
			}
			
			if (Double.isNaN(stampImage.autoMin) || Double.isNaN(stampImage.autoMax) || min2<stampImage.autoMin || max>stampImage.autoMax) {
				if (Double.isNaN(stampImage.autoMin)) stampImage.autoMin=min2;
				if (Double.isNaN(stampImage.autoMax)) stampImage.autoMax=max;

				if (min2<stampImage.autoMin) {
					stampImage.autoMin=min2;
				} else {
					min2=stampImage.autoMin;
				}
				if (max>stampImage.autoMax) {
					stampImage.autoMax=max;
				} else {
					max=stampImage.autoMax;
				}
				stampImage.autoValuesChanged=true;
			} else {
				min2=stampImage.autoMin;
				max=stampImage.autoMax;
			}
		} else {
			// We were informed of the proper min/max range to use.  Use it.
			min2 = imageMin;
			max = imageMax;
		}
		
		double v;
		double sample;
		int dnValue;
		
		double range = max-min2;
		
		for (int i=0; i<w; i++) {
			for (int j=0; j<h; j++) {
				v = values[i + j*w]; // accessing the one dimensional array for two dimensions
				if (Double.isNaN(v) || v<min2) {
					newValues[i+j*w] = 0;
					continue;
//					v=0;
//					if (v<min2) v=min2;
				}
				
				// Interpolate each point between the min and max values, giving us floating point values from 0.0 - 1.0
				// Values of this form can be rendered using g2.drawImage directly - however we want to use a ColorImageOp later for code reuse reasons
				// This step is kept separate for clarity, and for potential ease  of future use
				sample = (v-min2)/range;
				
				// Bring any outliers back into line
				if (sample<0) sample=0;
				if (sample>1) sample=1;
				
//				dnValue = (int)Math.floor(255*sample);
				dnValue = (int)Math.floor(254*sample)+1;  // Stretch into the range of 1-255, skipping 0
				
				newValues[i + j*w] = dnValue;
			}
		}

		// The rest of the JMARS stamp layer currently wants a 3 band + alpha image.  Set our calculated value on each of the bands
		destRaster.setSamples(0, 0, w, h, 0, newValues);
		destRaster.setSamples(0, 0, w, h, 1, newValues);
		destRaster.setSamples(0, 0, w, h, 2, newValues);
		
		// Alpha is always set to max
		for (int i=0; i<newValues.length; i++) {
			newValues[i]=255;
		}
		destRaster.setSamples(0, 0, w, h, 3, newValues);
		
		return dest;
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
		return null;
	}

}
