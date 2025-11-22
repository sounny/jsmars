package edu.asu.jmars.layer.map2;

import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BandedSampleModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RasterOp;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;

import javax.imageio.ImageIO;

import edu.asu.jmars.util.VicarReader;

/**
 * Algorithm lifted from:
 * Relief Shading using Digital Elevation Models
 * Qiming Zhou - School of Geography, The Univ of New South Wales, P.O. Box 1, Kensington, NSW 2033, Australia
 * in Computers & Geosciences Vol. 18, No. 8, pp. 1035-1045, 1992
 */
public class ReliefShadeOp implements RasterOp {
	/* diagonally opposite points
	 *   6 7 8
	 *   3 4 5
	 *   0 1 2
	 */
	private final int opp[] = {8,7,6,5,4,3,2,1,0};
	
	private final double root2 = Math.sqrt(2.0);
	private final double ratio[] = {root2, 1, root2, 1};
	private double cellSize = 1; // meters
	
	private final double az, el, sinAz, cosAz, sinEl, cosEl;
	
	/**
	 * Constructs a Hill Shading / Relief Shading raster operator.
	 * @param az Solar azimuth in degrees
	 * @param el Solar elevation in degrees
	 * @param cellSize Size of each pixel in meters
	 */
	public ReliefShadeOp(double az, double el, double cellSize){
		this.az = az;
		this.el = el;
		this.cellSize = cellSize;
		
		sinAz = Math.sin(Math.toRadians(this.az));
		cosAz = Math.cos(Math.toRadians(this.az));
		sinEl = Math.sin(Math.toRadians(this.el));
		cosEl = Math.cos(Math.toRadians(this.el));
	}

	public WritableRaster filter(Raster src, WritableRaster dest) {
		if (dest == null)
			dest = createCompatibleDestRaster(src);
		
		int w = src.getWidth();
		int h = src.getHeight();
		
		double d[] = null;
		double slope[] = new double[4];
		double north, east, c, intensity;
		double outIntensity;

		for(int y=0; y<h; y++){
			for(int x=0; x<w; x++){
				d = safeGetPixels(src, x, y, d);
				
				for(int i=0; i<9; i++){
					d[i] = d[4]*2 - d[opp[i]];
				}
				for(int i=0; i<4; i++){
					if ((d[i]-d[4])*(d[4]-d[opp[i]]) >= 0){
						slope[i] = ((double)(d[opp[i]]-d[i]))/(ratio[i]*2.0*cellSize);
						d[i] = 1;
					}
					else {
						slope[i] = 0;
						d[i] = 0;
					}
				}
				
				if ((d[0]+d[1]+d[2] == 0) || (d[0]+d[3]+d[2] == 0)){
					north = 0;
					east = 0;
				}
				else {
					north = (slope[0]+slope[1]+slope[2])/(d[0]+d[1]+d[2]);
					east = (slope[0]+slope[3]-slope[2])/(d[0]+d[3]+d[2]);
					
				}
				c = Math.sqrt(1+north*north+east*east);
				intensity = (sinEl - east*sinAz*cosEl - north*cosAz*cosEl) / c;
				
				// for byte output
				// outIntensity = Math.max(0,(int)Math.rint(intensity*255));
				outIntensity = Math.max(0,intensity*255);
				
				dest.setSample(x, y, 0, outIntensity);
			}
		}
		
		//minimize the obvious tile border that occurs once you get past max ppd by copying the values from the nearest line 
		for(int y=0; y<h; y++){
			double temp = dest.getSampleDouble(1,y,0);
			double temp2 = dest.getSampleDouble(w-2,y,0);
			dest.setSample(0,y,0, temp);
			dest.setSample(w-1,y,0,temp2);
		}
		for (int x=0; x<w; x++) {
			double temp = dest.getSampleDouble(x,1,0);
			double temp2 = dest.getSampleDouble(x,h-2,0);
			dest.setSample(x,0,0,temp);
			dest.setSample(x,h-1,0,temp2);
		}
		return dest;
	}
	
	private double[] safeGetPixels(Raster src, int x, int y, double[] dArray){
		int w = src.getWidth(), h = src.getHeight();
		
		if (dArray == null)
			dArray = new double[9];
		
		if (x >= 1 && y >= 1 && x < (w-1) && y < (h-1)) {
			dArray = src.getSamples(x-1, y-1, 3, 3, 0, dArray);
		}
		else {
			dArray[4] = src.getSampleDouble(x,y,0);
			int n=0;
			for(int j=y-1; j<=(y+1); j++){
				for (int i=x-1; i<=(x+1); i++){
					if (i >= 0 && j >= 0 && i < w && j < h && n != 4)
						dArray[n] = src.getSampleDouble(i,j,0);
					else
						dArray[n] = dArray[4];
					n++;
				}
			}
		}
		
		// Swap top row with bottom row to get element in the order of the algorithm
		for(int i=0; i<3; i++){
			double tmp = dArray[i];
			dArray[i] = dArray[i+6];
			dArray[i+6] = tmp;
		}
		return dArray;
	}

	public Rectangle2D getBounds2D(Raster src) {
		return src.getBounds();
	}

	public WritableRaster createCompatibleDestRaster(Raster src) {
		// for byte output:
		// return Raster.createBandedRaster(DataBuffer.TYPE_BYTE, src.getWidth(), src.getHeight(), 1, null);
		SampleModel sm = new BandedSampleModel(DataBuffer.TYPE_FLOAT, src.getWidth(), src.getHeight(), 1);
		return Raster.createWritableRaster(sm, null);
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
	

	public static void main(String[] args) throws Exception {
		double radius = 3386*1000; // meters

		long t0 = System.currentTimeMillis();
		BufferedImage dem = VicarReader.createBufferedImage("/tmp/mola_16ppd_topo.vic"); int ppd=16;
		long t1 = System.currentTimeMillis();
		System.err.println("Read DEM in "+(t1-t0)+" ms");

		BufferedImage relief = new BufferedImage(dem.getWidth(), dem.getHeight(), BufferedImage.TYPE_BYTE_GRAY);


		t0 = System.currentTimeMillis();
		ReliefShadeOp sop = new ReliefShadeOp(45, 55, ((2*Math.PI*radius)/360.0)/ppd);
		sop.filter(dem.getRaster(), relief.getRaster());
		t1 = System.currentTimeMillis();
		System.err.println("Computed relief in "+(t1-t0)+" ms");

		t0 = System.currentTimeMillis();
		ImageIO.write(relief, "png", new File("/tmp/relief.png"));
		t1 = System.currentTimeMillis();
		System.err.println("Output written in "+(t1-t0)+" ms");

	}
}
