package edu.asu.jmars.layer.stamp.projection;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.asu.jmars.util.HVector;

public class JMARSCylindrical implements Projection {

    double ptx;
    double pty;
    double ptz;

    double prx[];
    double pry[];
    double prz[];
		
    double v1x[];
    double v1y[];
    double v1z[];

    double v2x[];
    double v2y[];
    double v2z[];

    double sinTheta[];
    double cosTheta[];

    private HVector spatialPt = new HVector();

    public JMARSCylindrical(int dstW, int dstH, Rectangle2D where, HVector up, HVector center, int renderPPD) {
	       /////// CONSTANTS FOR THE for() LOOP BELOW
	       // baseX: Pixel-wise world coordinate origin of the destination
	       // baseY: Pixel-wise world coordinate origin of the destination
	       double baseX = where.getMinX();
	       double baseY = where.getMaxY(); // image y coords run top-down

	       // Variables used and reused by the loop below:
	       final double n2 = Math.sqrt(up.x*up.x+up.y*up.y+up.z*up.z);
	   		
	       final double wx = up.x / n2;
	       final double wy = up.y / n2;
	       final double wz = up.z / n2;
	       final double bigw = biggest(wx, wy, wz);
	   		
	       final double rx = wx / bigw;
	       final double ry = wy / bigw;
	       final double rz = wz / bigw;

	       final double dotu = rx*rx + ry*ry + rz*rz;
				
	       prx = new double[dstH];
	       pry = new double[dstH];
	       prz = new double[dstH];
			
	       v1x = new double[dstH];
	       v1y = new double[dstH];
	       v1z = new double[dstH];

	       v2x = new double[dstH];
	       v2y = new double[dstH];
	       v2z = new double[dstH];
	       
	       sinTheta = new double[dstW];
	       cosTheta = new double[dstW];
	       
		   for(int i=0; i<dstW; i++) {
			   double x = Math.toRadians(baseX + (double) i / renderPPD);
			   sinTheta[i]=Math.sin(x);
			   cosTheta[i]=Math.cos(x);
		   }

	       for(int j=0; j<dstH; j++) {
	           double y = Math.toRadians(baseY - (double) j / renderPPD);
	           double sin=Math.sin(y);
	           double cos=Math.cos(y);
	           
	       		double nx = center.x * cos + up.x * sin;
	       		double ny = center.y * cos + up.y * sin;
	       		double nz = center.z * cos + up.z * sin;
	       		double bign = biggest(nx, ny, nz);
	       		
				double tx = nx / bign;
				double ty = ny / bign;
				double tz = nz / bign;

				double dotv = rx*tx + ry*ty + rz*tz;
				
				double scaley = dotv*bign/dotu;
				
				prx[j] = rx * scaley;
				pry[j] = ry * scaley;
				prz[j] = rz * scaley;

				v1x[j] = nx-prx[j];
				v1y[j] = ny-pry[j];
				v1z[j] = nz-prz[j];
				
				v2x[j] = wy * v1z[j] - wz * v1y[j];
				v2y[j] = wz * v1x[j] - wx * v1z[j];
				v2z[j] = wx * v1y[j] - wy * v1x[j]; 				 				
	       }    	   
	
    }
    
    
    
	public Point2D lineSample(double lon, double lat, Point2D returnPoint) {
		// Not sure this is going to be used at all
		return null;
	}

	public HVector spatialPt(int line, int sample) {	
			ptx = v1x[line] * cosTheta[sample] + v2x[line] * sinTheta[sample] + prx[line];
 			pty = v1y[line] * cosTheta[sample] + v2y[line] * sinTheta[sample] + pry[line];
 			ptz = v1z[line] * cosTheta[sample] + v2z[line] * sinTheta[sample] + prz[line];
       		           		
       		final double ptUnitz = ptz / Math.sqrt(ptx*ptx + pty*pty + ptz*ptz);
       		
       		final double cosLon;
       		final double sinLon;
       		           		
       		// This is code to convert arctan2 to atan
	   		int sign=1;
	   		
	   		if (pty<0) { 
	   			sign = -1; 
	   		};

	   		
	   		if (ptx == 0) {    	   			
	   			cosLon = 0;
	   			sinLon = sign * 1;
	   		} else if (pty != 0) {
    	   		double x1 = Math.abs(pty/ptx);

    	   		if (ptx>0) {
           			cosLon = 1.0 / Math.sqrt(x1*x1 + 1);
           			sinLon = sign * x1 / Math.sqrt(x1*x1 + 1);
    	   		} else {
           			cosLon = -1.0 / Math.sqrt(x1*x1 + 1);
           			sinLon = sign * x1 / Math.sqrt(x1*x1 + 1);
    	   		}           			
       		} else if (ptx < 0) {
       			cosLon = -1;
       			sinLon = 0;
       		} else {
       			cosLon = 1;
       			sinLon = 0;
       		}
            		           		
       		double cosLat = Math.sqrt(1-ptUnitz*ptUnitz);
       		           		
       		spatialPt.x = cosLat * cosLon;
       		spatialPt.y = cosLat * sinLon;
       		spatialPt.z = ptUnitz; // since latOf = Math.asin(ptUnitz);

       		return spatialPt;
	}
	
	// WARNING WARNING WARNING
	// This method calls spatialPt, which updates a class variable, which is then
	// read and returned from this method.  Reusing this class variable prevents creating
	// and later garbage collecting hundreds of thousands of objects, but does mean
	// that code that calls these methods needs to be careful not to call them again
	// until it is done with the previous values (or has copied them elsewhere).
	// Do not attempt to use multiple threads to render a single image tile.
	// WARNING WARNING WARNING
	public Point2D lonLat(int line, int sample, Point2D returnPoint) {
		spatialPt(line, sample);
 	    // These expand to surprisingly expensive operations
        double lat=spatialPt.lat();
        double lon=360-spatialPt.lon();
	
		returnPoint.setLocation(lon, lat);
		return returnPoint;
	}

    public double biggest(double x, double y, double z)
	 {
	   double m = Math.abs(x);
	   if(m < Math.abs(y))m=Math.abs(y);
	   if(m < Math.abs(z))m=Math.abs(z);
	   return(m);
	 }

}
