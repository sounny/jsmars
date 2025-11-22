package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import edu.asu.jmars.layer.stamp.spectra.SpectraObject;
import edu.asu.jmars.util.Util;

import java.util.List;
import java.util.Set;

/*
 * This BufferedImageOp takes a BufferedImage containing line/sample values as well as a set of Spectra Points
 *  and converts it into a color map of where the spectra are located on the image.
 */
public class LineSampleSpectraImageOp implements BufferedImageOp {
	
	public StampImage stampImage;
	
	LineSampleSpectraImageOp(StampImage wholeImage) {
		stampImage=wholeImage;
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
//			dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
			
			dest = Util.newBufferedImage(src.getWidth(), src.getHeight());
//			dest=new BufferedImage(src.getColorModel(), src.copyData(null), false, null);
		}
		
		int w= src.getWidth();
		int h = src.getHeight();
		
		Raster srcRaster = src.getRaster();
		WritableRaster destRaster = dest.getRaster();
		
		double values[] = new double[w*h];
		int newValues[][] = new int[4][w*h];

		for (int j=0; j<4; j++) {
			for (int i=0; i<w*h; i++) {
				newValues[j][i]=0;
			}
		}
		
		// Getting the data in bulk is substantially faster than reading it pixel by pixel
		srcRaster.getSamples(0, 0, w, h, 0, values);

		double v;
		
		if (stampImage.myStamp.spectraPoints!=null) {
		List<SpectraObject> points = stampImage.myStamp.spectraPoints;
			 			
		for (int i=0; i<w; i++) {
outer:		for (int j=0; j<h; j++) {
				v = values[i + j*w]; // accessing the one dimensional array for two dimensions
				
				short sample1 = (short)((int)v);
				short line1 = (short)((int)v>>16);

				for (SpectraObject pt : points) {
					Color c= pt.getColor();
					
					if (c==Color.BLACK) {
						c=Color.WHITE;
					}
					
					int x = (int)pt.lineSamplePoint.getX();
					int y = (int)pt.lineSamplePoint.getY();
					
					if (Math.abs(line1-y)<pt.ypad && Math.abs(sample1-x)<pt.xpad) {
						newValues[0][i+j*w] = c.getRed();
						newValues[1][i+j*w] = c.getGreen();
						newValues[2][i+j*w] = c.getBlue();
						continue outer;
					}
				}
			}
		}
		}

		
		// The rest of the JMARS stamp layer currently wants a 3 band + alpha image.  Set our calculated value on each of the bands
		destRaster.setSamples(0, 0, w, h, 0, newValues[0]);
		destRaster.setSamples(0, 0, w, h, 1, newValues[1]);
		destRaster.setSamples(0, 0, w, h, 2, newValues[2]);
				
		// Alpha is always set to max
		for (int i=0; i<w*h; i++) {
			if (newValues[0][i]!=0 && newValues[1][i]!=0 && newValues[2][i]!=0) {
				newValues[3][i]=255;
			} else {
				newValues[3][i]=0;
			}
		}
		destRaster.setSamples(0, 0, w, h, 3, newValues[3]);
		
		return dest;
	}

	public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
		return null;
	}

}
