package edu.asu.jmars.layer.mcdslider;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.layer.stamp.projection.JMARSCylindrical;
import edu.asu.jmars.layer.stamp.projection.Projection;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;

/**
 * This class is based off of StampLayer.ImageProjecter, but then changed to only handle the case where:
 *    The image is global
 *    The image is floating point numeric
 *    The image is Simple Cylindrical (default projection)
 *    
 *    All off these conditions are currently met by the MCD Slider's data products.  Any changes may result in this class not producing
 *    expected results.
 *    
 * @author sdickens
 *
 */
public class GlobalDataReprojecter {

	BufferedImage startingImage;
	
	Projection_OC srcProj = (Projection_OC)new Projection_OC(0, 0);

	int renderPPD;
	
	BufferedImage dstImage = null;
	
	Rectangle2D where = new Rectangle2D.Double(-180,-90, 360,180);
	
	// These get reused for each pixel.  Cheaper than creating and destroying an object every time
	private HVector spatialPt = new HVector();
	private Point srcPt = new Point();
	Point2D.Double unitPt = new Point2D.Double();

	private int dstW=-1;
	private int dstH=-1;

	JMARSCylindrical dstProj = null;

	double minVal = Double.MAX_VALUE;
	double maxVal = Double.MIN_VALUE;
	
	// Takes a global simple cylindrical image and reprojects it into a global image in the current JMARS projection
	public GlobalDataReprojecter(BufferedImage startingImage) {
		this.startingImage = startingImage;

		dstW = startingImage.getWidth();
		dstH = startingImage.getHeight();
		
		Projection_OC po = (Projection_OC)Main.PO;
	
		renderPPD = dstW / 360;
		
		dstProj = new JMARSCylindrical(dstW, dstH, where, po.getUp(), po.getCenter(), renderPPD);
	}


	public void fillImage(int xsrcArray[][], int ysrcArray[][]) {    	  
		try {
			for (int i=0; i<xsrcArray.length; i++) {
				for (int j=0; j<xsrcArray[0].length; j++) {
					int sample = xsrcArray[i][j];
					int line = ysrcArray[i][j];

					if (line<0 && sample<0) {
						continue;
					}
					try {
						boolean isNumeric = true;
						if (isNumeric) {							
							double doubleVal = startingImage.getRaster().getSampleDouble(sample, line, 0);
							
							if (doubleVal < minVal) minVal=doubleVal;
							if (doubleVal > maxVal) maxVal=doubleVal;
							
							dstImage.getRaster().setSample(i, j, 0, doubleVal);
						} else {
							dstImage.setRGB(i, j, startingImage.getRGB(sample, line));
						}
					} catch (Exception e) {
						//   	   	    				System.out.println("Bombed on line: " + line + "   sample = " + sample + " frame = " + frameNum);
					}
					continue;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public double getMinVal() {
		return minVal;
	}
	
	public double getMaxVal() {
		return maxVal;
	}


	// Projection for images, not a JMARS ProjObj
	Projection proj = null;

	synchronized BufferedImage getProjectedImage(boolean isNumeric)
	{
		// We create two two dimensional arrays to store indexes before we do pixel by pixel copying from source tile to destination tile
		// This method is necessary for THEMIS DCS which reads from a random access file, and we want to do all of the work at once.
		// It seems to work fine for other cases as well, so we do everything this way to limit the number of code paths.
		int xsrcCoords[][]=null;
		int ysrcCoords[][]=null;
				
		xsrcCoords = new int[dstW][dstH];
		ysrcCoords = new int[dstW][dstH];

		for (int i=0; i<dstW; i++) {
			for (int j=0; j<dstH; j++) {
				xsrcCoords[i][j] = -1;
				ysrcCoords[i][j] = -1;
			}
		}

//		System.out.println("### Creating new image: " + dstW + " : " + dstH);
		
		if (isNumeric) {
			// This is different from how the StampLayer does it, because we were losing numeric values when caching the projected data.
			dstImage = Util.createCompatibleImage(startingImage, startingImage.getWidth(), startingImage.getHeight());
			
//			SampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_DOUBLE, dstW, dstH, 1, dstW, new int[] {0});
//
//			DataBuffer db = new DataBufferDouble(dstW * dstH);
//			WritableRaster wr = Raster.createWritableRaster(sm, db, null);
//			ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
//			ColorModel cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_DOUBLE);
//			dstImage = new BufferedImage(cm, wr, true, null);
			
			float[] ddata = new float[dstW*dstH];
			
			for (int i=0;i<ddata.length;i++) {
				ddata[i]=Short.MIN_VALUE;  // Initialize to ignore values.  Anything that isn't explicitly set will be transparent
			}
			
			dstImage.getRaster().setDataElements(0, 0, dstW, dstH, ddata);
		} else {
			dstImage = Util.newBufferedImage(dstW, dstH);			
		}

		if (dstImage == null) {
			// TODO: Is there any better way to recover from this?
					System.out.println("out of memory");
		return null;
		}

		ProjObj po = new ProjObj.Projection_OC(0,0);
		HVector up = ((Projection_OC)po).getUp();
		HVector center = ((Projection_OC)po).getCenter();
		HVector centerXUp = center.cross(up);
		HVector temp = new HVector();

		// i and j are essentially lines and samples for our destination image.
		// We're going through and for each line and sample, getting the lat/lon value, then converting that to line/sample in the 
		// source image
		for(int j=0; j<dstH; j++) {
			for(int i=0; i<dstW; i++) {

				projectedSimpleCylindrical(up, temp, center, centerXUp, j, i);

				// Save the indices for now, we'll copy them from src to dest en masse later
				xsrcCoords[i][j]=srcPt.x;
				ysrcCoords[i][j]=srcPt.y;
			}
		}

		fillImage(xsrcCoords, ysrcCoords);

		return dstImage;
	}

	private void projectedSimpleCylindrical(HVector up, HVector temp, HVector center, HVector centerXUp, int j, int i) {
		spatialPt = dstProj.spatialPt(j, i);

		// Very simple logic, since we're only handling the case where we're starting with default simple cylindrical.  Conversion from
		// that to the same orientation of world coordinates is direct.
		
		int x = (int) Math.floor((360-spatialPt.lon())*renderPPD);
		int y = (int) Math.floor((90-spatialPt.lat())*renderPPD);
		
		srcPt.setLocation(x,y);		
	}

	
	public double biggest(double x, double y, double z)
	{
		double m = Math.abs(x);
		if(m < Math.abs(y))m=Math.abs(y);
		if(m < Math.abs(z))m=Math.abs(z);
		return(m);
	}

}
