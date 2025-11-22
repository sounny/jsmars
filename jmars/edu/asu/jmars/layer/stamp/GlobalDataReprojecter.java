package edu.asu.jmars.layer.stamp;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;

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
public class GlobalDataReprojecter extends ImageProjecter {
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
	
	boolean shift180 = false;  // Used to shift a global mosaic from -180 to 180 range into 0 to 360 range like we prefer
	
	// Takes a global simple cylindrical image and reprojects it into a global image in the current JMARS projection
	public GlobalDataReprojecter(int width, int height, boolean shift180) {
		dstW = width;
		dstH = height;
		this.shift180=shift180;
		
		renderPPD = dstW / 360;		
	}

	public void fillImage(BufferedImage startingImage, int xsrcArray[][], int ysrcArray[][]) {    	  
		try {
			int startingPixels[] = new int[dstW*dstH]; 
			int endingPixels[] = new int[dstW*dstH];
			
			startingPixels = startingImage.getRGB(0, 0, dstW, dstH, startingPixels, 0, dstW);
						
			for (int i=0; i<xsrcArray.length; i++) {
				for (int j=0; j<xsrcArray[0].length; j++) {
					int sample = xsrcArray[i][j];
					int line = ysrcArray[i][j];

					if (line<0 || sample<0) {
						continue;
					}
					
					if (line*dstW+sample >= dstW*dstH) continue;
					
					endingPixels[j*dstW+i]=startingPixels[line*dstW+sample];							
					continue;
				}				
			}
			
			dstImage.setRGB(0, 0, dstW, dstH, endingPixels, 0, dstW);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void fillNumericImage(BufferedImage startingImage, int xsrcArray[][], int ysrcArray[][]) {    	  
		try {
			double startingPixels[] = new double[dstW*dstH]; 
			double endingPixels[] = new double[dstW*dstH];
			
			startingPixels = startingImage.getRaster().getSamples(0, 0, dstW, dstH, 0, startingPixels);
						
			for (int i=0; i<xsrcArray.length; i++) {
				for (int j=0; j<xsrcArray[0].length; j++) {
					int sample = xsrcArray[i][j];
					int line = ysrcArray[i][j];

					if (line<0 || sample<0) {
						continue;
					}
					
					if (line*dstW+sample >= dstW*dstH) continue;

					endingPixels[j*dstW+i]=startingPixels[line*dstW+sample];
					continue;
				}				
			}
			
			dstImage.getRaster().setSamples(0, 0, dstW, dstH, 0, endingPixels);			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Projection for images, not a JMARS ProjObj
	Projection proj = null;

	// We create two two dimensional arrays to store indexes before we do pixel by pixel copying from source tile to destination tile
	// This method is necessary for THEMIS DCS which reads from a random access file, and we want to do all of the work at once.
	// It seems to work fine for other cases as well, so we do everything this way to limit the number of code paths.
	int xsrcCoords[][]=null;
	int ysrcCoords[][]=null;

	ProjObj cachedProj = null;
	
	public synchronized BufferedImage getProjectedImage(BufferedImage startingImage, boolean isNumeric, ProjObj destPO)
	{
		long startTime = System.currentTimeMillis();

		dstImage = Util.createCompatibleImage(startingImage, dstW, dstH);			

		if (dstImage == null) {
			// TODO: Is there any better way to recover from this?
					System.out.println("out of memory");
		return null;
		}

		if (cachedProj!=destPO) {
			xsrcCoords = new int[dstW][dstH];
			ysrcCoords = new int[dstW][dstH];
	
			for (int i=0; i<dstW; i++) {
				for (int j=0; j<dstH; j++) {
					xsrcCoords[i][j] = -1;
					ysrcCoords[i][j] = -1;
				}
			}

			ProjObj po = srcProj;
			HVector up = ((Projection_OC)po).getUp();
			HVector center = ((Projection_OC)po).getCenter();
			HVector centerXUp = center.cross(up);
			HVector temp = new HVector();
	
			dstProj = new JMARSCylindrical(dstW, dstH, where, ((Projection_OC)destPO).getUp(), ((Projection_OC)destPO).getCenter(), renderPPD);
			
//			System.out.println("Setup projection time: " + (System.currentTimeMillis()-startTime));
			
			lats.clear();
			
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
			
			cachedProj=destPO;
		}

		if (isNumeric) {
			fillNumericImage(startingImage, xsrcCoords, ysrcCoords);
		} else {
			fillImage(startingImage, xsrcCoords, ysrcCoords);
		}

//		System.out.println("Fill image projection time: " + (System.currentTimeMillis()-startTime));

		return dstImage;
	}

	HashSet<Double> lats = new HashSet<Double>();
	private void projectedSimpleCylindrical(HVector up, HVector temp, HVector center, HVector centerXUp, int j, int i) {
		spatialPt = dstProj.spatialPt(j, i);

		// Very simple logic, since we're only handling the case where we're starting with default simple cylindrical.  Conversion from
		// that to the same orientation of world coordinates is direct.
		
		int x;
		
		x = (int) Math.floor((360-spatialPt.lon())*renderPPD);
		
		int y = (int) Math.floor((90-spatialPt.lat())*renderPPD);
				
		// Some of our image sources go from 0-360 and others go from -180 to 180.
		// The logic below swaps which side of the image our x value is in, if
		// the image is not in the form we're expecting
		if (!shift180) {
			int halfWidth = dstW/2;
			
			if (x<halfWidth) {
				x+=halfWidth;
			} else {
				x-=halfWidth;
			}
		}
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
