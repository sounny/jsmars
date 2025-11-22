package edu.asu.jmars.layer.stamp;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;

/**
 * Copied and trimmed from GridDataStore.Cell
 **/
public final class Cell
 {
	// What order were they just specified in?
	public final boolean clockwise;
	// All of the border points, as a 5-point closed quadrilateral chain
	public final HVector[] chain;

	// The normals of each cell wall, pointing inwards.
	public final HVector s;
	public final HVector n;
	public final HVector e;
	public final HVector w;

	public final HVector nw;
	public final HVector ne;
	public final HVector sw;
	public final HVector se;
	
	double magw; // magnitude of w
	double mags; // magnitude of s
	
	// Interpolation axes
	public final HVector wePlane;
	public final HVector snPlane;
	// Interpolation data values
	public final double wePlaneSpan;
	public final double snPlaneSpan;
	
	public final Projection_OC proj;

	public Point2D uninterpolate(HVector pt, Point2D unitPt)
	 {
		if(unitPt == null)
			unitPt = new Point2D.Double();

		HVector pt_we = wePlane.cross(pt).unit();
		HVector pt_sn = snPlane.cross(pt).unit();

		double x = pt_we.separation(w) / wePlaneSpan;
		double y = pt_sn.separation(s) / snPlaneSpan;

		if(pt.dot(w) < 0) x = -x;
		if(pt.dot(s) < 0) y = -y;

		unitPt.setLocation(x, y);
		return  unitPt;
	 }

	double wex;
	double wey;
	double wez;
	
	double snx;
	double sny;
	double snz;
	
	public final Point2D uninterpolateFast(final HVector pt, final Point2D unitPt)
	 {
		
		wex = wePlane.y * pt.z - wePlane.z * pt.y;
		wey = wePlane.z * pt.x - wePlane.x * pt.z;
		wez = wePlane.x * pt.y - wePlane.y * pt.x;

		final double wn = Math.sqrt(wex*wex + wey*wey + wez*wez);
		
		wex /= wn;
		wey /= wn;
		wez /= wn;

		double x;
		
		if(wn == 0  ||  magw == 0)
			x=0;

		double dp = wex * w.x + wey * w.y + wez * w.z;

		if(dp > 0)
		 {
			x = doStuffPos(wex, wey, wez, w.x, w.y, w.z);
		 }
		else if(dp < 0)
		 {
			x = doStuffNeg(wex, wey, wez, w.x, w.y, w.z);
		 }
		else
			x = Math.PI / 2;		
		
		snx = snPlane.y * pt.z - snPlane.z * pt.y;
		sny = snPlane.z * pt.x - snPlane.x * pt.z;
		snz = snPlane.x * pt.y - snPlane.y * pt.x;

		final double sn = Math.sqrt(snx*snx + sny*sny + snz*snz);
		
		snx /= sn;
		sny /= sn;
		snz /= sn;
				
		double y;
		
		if(sn == 0  ||  mags == 0)
			y=0;

		dp = snx * s.x + sny * s.y + snz * s.z;

		if(dp > 0)
		 {
			y = doStuffPos(snx, sny, snz, s.x, s.y, s.z);
		 }
		else if(dp < 0)
		 {
			y = doStuffNeg(snx, sny, snz, s.x, s.y, s.z);
		 }
		else
			y = Math.PI / 2;

		//
			
		x /= wePlaneSpan;
		y /= snPlaneSpan;

		if(pt.x * w.x + pt.y * w.y + pt.z * w.z < 0) x = -x;
		if(pt.x * s.x + pt.y * s.y + pt.z * s.z < 0) y = -y;

		unitPt.setLocation(x, y);
		return  unitPt;
	 }
	
	public static final double getMag(final double x, final double y, final double z) {
		return Math.sqrt(x*x + y*y + z*z);
	}
	
	public static final double doStuffPos(double x1, double y1, double z1, double x2, double y2, double z2) {
		final double xx = x1 - x2;
		final double yy = y1 - y2;
		final double zz = z1 - z2;
		final double dxp = getMag(xx, yy, zz);
		final double tmp = dxp / 2;

		return 2 * Math.atan(tmp / Math.sqrt(1 - tmp*tmp));		
	}
	
	public static final double doStuffNeg(double x1, double y1, double z1, double x2, double y2, double z2) {		 
		final double xx = x1 + x2;
		final double yy = y1 + y2;
		final double zz = z1 + z2;
		final double dxp = getMag(xx, yy, zz);
		final double tmp = dxp / 2;

		return Math.PI - 2 * Math.atan(tmp / Math.sqrt(1 - tmp*tmp));
	}
	
	
	public Cell(HVector sw,
				HVector se,
				HVector ne,
				HVector nw, Projection_OC proj)
	 {
		this.proj = proj;

		this.nw=nw;
		this.sw=sw;
		this.ne=ne;
		this.se=se;
		
		chain = new HVector[] { sw, se, ne, nw, sw };

		HVector _s = sw.cross(se).unit(); // the "real" s is declared final
		clockwise = _s.dot(ne) < 0;

		if(clockwise)
		 {
			s = _s.neg();
			e = ne.cross(se).unit();
			n = nw.cross(ne).unit();
			w = sw.cross(nw).unit();
		 }
		else
		 {
			s = _s;
			e = se.cross(ne).unit();
			n = ne.cross(nw).unit();
			w = nw.cross(sw).unit();
		 }

		wePlane = e.cross(w).unit();
		snPlane = n.cross(s).unit();
		wePlaneSpan = Math.PI - e.separation(w);
		snPlaneSpan = Math.PI - s.separation(n);
		
		magw = Math.sqrt(w.x*w.x + w.y*w.y + w.z*w.z);
		mags = Math.sqrt(s.x*s.x + s.y*s.y + s.z*s.z);
	 }

	private static final class Range
	 {
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		void absorb(double val)
		 {
			if(!Double.isInfinite(min)  &&  Math.abs(val-min) > 180.0)
				if(val > 180)
				 {
					min += 360;
					max += 360;
				 }
				else
					val += 360;

			if(val < min) {
				min = val;
			}
			if(val > max) {
				max = val;
			}
		 }
	 }

	// These variables are used to detect and handle the case where an image crosses the world boundary (ie. starts at 359 degrees and ends at 1 degree)
	// This code tries to avoid making assumptions about what order the image points will be in.  If any of the points are over 180 degrees apart in the
	// X direction, then we try to push them into positive 360 space.  ie. 359 to 361 for the above case
	boolean crossesWorldEdge = false;
	double minx = Double.POSITIVE_INFINITY;
	double maxx = Double.NEGATIVE_INFINITY;
	
	private Rectangle2D worldBounds;
	public Rectangle2D getWorldBounds()
	 {
		if(worldBounds != null)
			return  worldBounds;

		Point2D[] chainW = new Point2D[chain.length];
		for(int i=0; i<chainW.length; i++) {
			Point2D lonLat = chain[i].toLonLat(null);

			chainW[i] =	proj.convSpatialToWorld(lonLat);
			
			double valx = chainW[i].getX(); 

			if(valx < minx) {
				minx = valx;
			}
			if(valx > maxx) {
				maxx = valx;
			}
		}
		
		if (maxx-minx>180) {  // probably crossing world boundary
			crossesWorldEdge = true;
			
			for (int i=0; i<chainW.length; i++) {
				if (Math.abs(chainW[i].getX()-minx)<180) {
					chainW[i].setLocation(chainW[i].getX()+360, chainW[i].getY());
				}
			}			
		}
		
		Range x = new Range();
		Range y = new Range();

		HVector up = proj.getUp();

		for(int i=0; i<4; i++)
		 {
			x.absorb(chainW[i].getX());
			y.absorb(chainW[i].getY());

			// Stuff used to decide if the y range needs refinement
			HVector a = chain[i];
			HVector b = chain[i+1];
			HVector norm = a.cross(b).unit();
			HVector axis = norm.cross(up).unit();

			// TODO: Look at this really closely... this is expanding based on following a great circle for latitudes,
			// when we might not want to do this for world coordinates....
			
			// Determine if we have an extreme condition
			if(Util.sign(axis.dot(a)) == -Util.sign(axis.dot(b)))
			 {
				// Extreme condition: calculate the outer boundary
				double extreme = Math.toDegrees(Math.abs(
					Math.asin(norm.cross(axis).dot(up))));
				y.absorb(a.dot(up) >= 0 ? extreme : -extreme);
			 }
		 }
		
		 // Picked pretty arbitrarily - if a side of a cell is over 5 degrees, there's a decent chance the midpoint of the side might bow out substantially, meaning
		 // part of the image would fall outside of the cell if we defined it with just a Rectangle defined by the corner points.  This quickly calculates the midpoint
		 // of each side, and expands the defining Rectangle if necessary
		for (int i=0; i<chainW.length; i++) {
			int indx1=i;
			int indx2=i+1;
			
			if (i==chainW.length-1) indx2=0;
			
			if (chainW[indx1].distance(chainW[indx2])>5) {
					Point2D p1 = getWorldPoint(chain[indx1].toLonLat(null), proj);
					Point2D p2 = getWorldPoint(chain[indx2].toLonLat(null), proj);
					
					Point2D p3 = new Point2D.Double((p1.getX()+p2.getX())/2, (p1.getY()+p2.getY())/2);
					Point2D p4 = proj.convWorldToSpatial(p3);
					Point2D p5 = getWorldPoint(p4, proj);
					
					if (chainW[indx1].distance(p5)>chainW[indx1].distance(chainW[indx2])) {
						// We've just made things worse... skip!  Hopefully never happens
						continue;
					}
					
					x.absorb(p5.getX());
					y.absorb(p5.getY());
				 } 
		}

		worldBounds = new Rectangle2D.Double(x.min,
											 y.min,
											 x.max-x.min,
											 y.max-y.min);
		return  worldBounds;
	 }
	
	/*
	 * This method is just a convenience method to allow us to shift coordinates in the case when the image crosses the boundary of the world projection,
	 * without having to muddy up the logic for the normal case.
	 */
	private Point2D getWorldPoint(Point2D spatialPt, Projection_OC po) {
		Point2D worldPoint=po.convSpatialToWorld(spatialPt);
		
		if (crossesWorldEdge) {
			if (Math.abs(worldPoint.getX()-minx)<180) {
				worldPoint.setLocation(worldPoint.getX()+360, worldPoint.getY());
			}
		}
		
		return worldPoint;
	}
	
/*
 * Cell support for Simple Cylindrical projections
 * 
 * This was added to handle Davinci Stamps and Custom Stamps, where a user is providing us with corner points and an image and asking us to render it for them.
 * 
 * Since we don't ask the user what projection their image is in, we assume it's in a projection centered at 0,0.  All of our calculations are done in this projected
 * x,y space.
 * 
 * If the user specifies only two points (upperleft and lowerright), we assume upperright has the same latitude as upperleft and the same longitude as lowerright,
 * and we assume lowerleft has the same latitude as lowerright and the same longitude as upperleft.
 * 
 * Once we have four points, user specified or not, we then make an assumption that the top row of pixels in the image are spaced out evenly along a line drawn
 * between upperleft and upperright.  Similarly, we assume the bottom row of pixels in the image are spaced evenly from lowerleft to lowerright.  In the same vein,
 * we assume the leftmost column of pixels in the image are evenly spaced down from the upperleft to the lowerleft, and the rightmost column of pixels are evenly 
 * spaced from upperright to lowerright.
 * 
 * NOTE: We don't assume any particular orientation between upperleft, upperright, lowerleft, and lowerright.  Those merely define the coordinates of the corresponding
 * pixels in the image.  We also DO NOT assume that the lines that define the outer edge of the image are parallel or perpendicular to anything in particular, and we 
 * don't assume that different pixels in the image cover the same amount of area as each other.
 * 
 * Because the image outline can be a square, rectangle, parallelagram, trapezoid, or essentially any other four sized shape, the math to determine the location of
 * any particular pixel becomes tricky.  The solution implemented works thusly:
 * 
 * Given the upperleft and upperright points, calculate their difference in both x and y directions, then divide those differences by the number of pixels across the
 * top row of the image.  Assuming a constant change along the top row, calculate the value for each pixel in the top row.  Repeat this process for the bottom row,
 * the left column, and the rightmost column.
 * 
 * Now, for a given image pixel (i,j), we can calculate it's position by intersecting the line that runs between top[i] and bot[i] with the line that runs between
 * left[j] and right[j].  We could at this point store every point for every pixel, but for a 500x500 source image, this results in 500,000 double values, which is 
 * a lot of extra memory overhead.  Instead, as a tradeoff between memory usage and speed, we store the intermediate values defining the lines from top/bot as arrays 
 * A1, B1, and C1, and the intermediate values defining the lines from left/right as arrays A2, B2, and C2.  For the same 500x500 source image, this uses a total of
 * 3000 double values, while still allowing us to calculate the location of any pixel with a few simple calculations.
 * 
 * The Stamp layer ultimately makes use of this by calling findGridLoc() for each screen pixel that falls within the bounds of this Cell object.  For each value passed
 * in, we start at position (i/width, j/height) in the image, and calculate the distance (in x,y space in the projection centered at 0,0) between the world coordinates
 * of the screen pixel and a 3 by 3 grid of pixels around our search position.  If the center of the grid has the smallest distance, it is the best approximation of
 * the image pixel corresponding to the screen pixel, and we return that value of i,j.  If the center of the grid is not the smallest distance, we shift the grid of
 * pixels in the direction of the smallest distance, and recalculate all 9 values.  This process repeats until the center is the smallest, and then that value is
 * returned.  
 * 
 * Knowing how the stamp layer iterates through screen pixels, we keep track of the last i,j value returned, and use that as the starting value for our search the next
 * time a request is made.  This simple optimization results in most queries completing in an average of 2 or 3 iterations.  There are, undoubtedly, fancier and more
 * elegant methods of looking up values in this situation, but it's tough to come up with something that can beat O(1) performance.
 * 
 * One of the snags of this approach, is that there is a 'closest' pixel to any value, even if that value is outside of the area we're interested in.  To handle this,
 * the top, bottom, left, and right arrays of points (and their corresponding line arrays) each have an extra point added at the beginning and end of their range.  This
 * point is calculated as the -1th point and the length+1th point, but otherwise calculated the same as the other points in the array.  This allows us to idenify any
 * values outside of our desired range, as any point that is 'closest' to one of these extra values.
 * 
 */
	
	double A1[] = null;
	double B1[] = null;
	double C1[] = null;
	double A2[] = null;
	double B2[] = null;
	double C2[] = null;
	
	// These variables handle the world boundary crossing condition for the grid.  See the comments on getWorldBounds().
	// A separate set of variables is needed because the grid always uses the default JMARS projection, whereas the world bounds are based off of the current JMARS
	// projection, which may be different.
	boolean gridcrossesWorldEdge = false;
	double gridminx = Double.POSITIVE_INFINITY;
	double gridmaxx = Double.NEGATIVE_INFINITY;
	
	public void buildGrid(int width, int height) {
		if (A1!=null) return;
		
		// All work is done in x,y space for a simple cylindrical projection centered at 0,0 (the default JMARS projection)
		ProjObj po = new ProjObj.Projection_OC(0,0);
	
		double x1 = po.convSpatialToWorld(nw.lonW(), nw.latC()).getX();
		double x2 = po.convSpatialToWorld(ne.lonW(), ne.latC()).getX();
		
		double x3 = po.convSpatialToWorld(sw.lonW(), sw.latC()).getX();
		double x4 = po.convSpatialToWorld(se.lonW(), se.latC()).getX();
		
		gridminx = Math.min(gridminx, x1);
		gridminx = Math.min(gridminx, x2);
		gridminx = Math.min(gridminx, x3);
		gridminx = Math.min(gridminx, x4);
		
		gridmaxx = Math.max(gridmaxx, x1);
		gridmaxx = Math.max(gridmaxx, x2);
		gridmaxx = Math.max(gridmaxx, x3);
		gridmaxx = Math.max(gridmaxx, x4);

		if (gridmaxx - gridminx > 180) {
			gridcrossesWorldEdge=true;
		}

		if (gridcrossesWorldEdge) {
			if (Math.abs(x1-gridminx)<180) {
				x1+=360;
			}

			if (Math.abs(x2-gridminx)<180) {
				x2+=360;
			}

			if (Math.abs(x3-gridminx)<180) {
				x3+=360;
			}

			if (Math.abs(x4-gridminx)<180) {
				x4+=360;
			}
		}
		
		double y1 = po.convSpatialToWorld(nw.lonW(), nw.latC()).getY();
		double y2 = po.convSpatialToWorld(ne.lonW(), ne.latC()).getY();
		
		double y3 = po.convSpatialToWorld(sw.lonW(), sw.latC()).getY();
		double y4 = po.convSpatialToWorld(se.lonW(), se.latC()).getY();

		// We define arrays of points to represent the edges of the image, with an extra pixel defined in each direction to help determine 
		// what falls outside of the image.
		double topx[] = new double[width+2];
		double topy[] = new double[width+2];
		double botx[] = new double[width+2];
		double boty[] = new double[width+2];
		double leftx[] = new double[height+2];
		double lefty[] = new double[height+2];
		double rightx[] = new double[height+2];
		double righty[] = new double[height+2];

		// We use this offset to move the coordinate values into the center of the pixel, rather than one of it's corners.  When we later pass in a coordinate
		// in x,y space, and ask which pixel is the closest, this makes it much, much easier to determine which pixel we want to draw.  Otherwise, we've only found
		// the closest intersection of 4 pixels, and still have to figure out which of the 4 pixels is the one we want.
		double offset = 0.5;
		
		//top points - we assume x and y vary constantly between these points
		double deltaxtop = (x2-x1)/width;
		double deltaytop = (y2-y1)/width;

		for (int i=0; i<width+2; i++) {
			topx[i]=x1 + deltaxtop*(i-1+offset);
			topy[i]=y1 + deltaytop*(i-1+offset);
		}
		
		// We calculate the extra 0th point and the very last point slightly differently.  In practice, if we have an image defined to start at 0,0, when we pass in
		// 0,0, we expect to get the first pixel in the image.  But due to floating point inaccuracies and many, many floating point calculations converting between
		// projections, if we use equal offsets, we end up with about a 50/50 chance of our exact 0,0 value matching the point we want, or the 'out of bounds' point.
		// To accomodate this, we push the offset for the edge points out by an extra .1 away from the valid points.
		topx[0]=x1 + deltaxtop*(-(offset+0.1));
		topy[0]=y1 + deltaytop*(-(offset+0.1));
		topx[width+1]=x1 + deltaxtop*(width+1 -1 + offset + 0.1);
		topy[width+1]=y1 + deltaytop*(width+1 -1 + offset + 0.1);
		
		//bot points - we assume x and y vary constantly between these points
		double deltaxbot = (x4-x3)/width;
		double deltaybot = (y4-y3)/width;
		for (int i=0; i<width+2; i++) {
			botx[i]=x3 + deltaxbot*(i-1+offset);
			boty[i]=y3 + deltaybot*(i-1+offset);
		}

		botx[0]=x3 + deltaxbot*(-(offset+0.1));
		boty[0]=y3 + deltaybot*(-(offset+0.1));
		botx[width+1]=x3 + deltaxbot*(width+1 -1 + offset + 0.1);
		boty[width+1]=y3 + deltaybot*(width+1 -1 + offset + 0.1);

		//left points - we assume x and y vary constantly between these points
		double deltaxleft = (x3-x1)/height;
		double deltayleft = (y3-y1)/height;
		for (int i=0; i<height+2; i++) {
			leftx[i]=x1 + deltaxleft*(i-1+offset);
			lefty[i]=y1 + deltayleft*(i-1+offset);
		}

		leftx[0]=x1 + deltaxleft*(-(offset+0.1));
		lefty[0]=y1 + deltayleft*(-(offset+0.1));
		leftx[height+1]=x1 + deltaxleft*(height+1 -1 + offset + 0.1);
		lefty[height+1]=y1 + deltayleft*(height+1 -1 + offset + 0.1);

		//right points - we assume x and y vary constantly between these points
		double deltaxright = (x4-x2)/height;
		double deltayright = (y4-y2)/height;
		for (int i=0; i<height+2; i++) {
			rightx[i]=x2 + deltaxright*(i-1+offset);
			righty[i]=y2 + deltayright*(i-1+offset);
		}

		rightx[0]=x2 + deltaxright*(-(offset+0.1));
		righty[0]=y2 + deltayright*(-(offset+0.1));
		rightx[height+1]=x2 + deltaxright*(height+1 -1 + offset + 0.1);
		righty[height+1]=y2 + deltayright*(height+1 -1 + offset + 0.1);

		// Here we calculate the values defining each line between the various top and bottom points
		A1 = new double[width+2];
		B1 = new double[width+2];
		C1 = new double[width+2];
		
		for (int x=0; x<width+2; x++) {
			A1[x]=boty[x]-topy[x];
			B1[x]=topx[x]-botx[x];
			C1[x]=A1[x]*topx[x]+B1[x]*topy[x];			
		}
		
		// Here we calculate the values defining each line between the various left and right points
		A2 = new double[height+2];
		B2 = new double[height+2];
		C2 = new double[height+2];
		
		for (int y=0; y<height+2; y++) {
			A2[y]=righty[y]-lefty[y];
			B2[y]=leftx[y]-rightx[y];
			C2[y]=A2[y]*leftx[y]+B2[y]*lefty[y];			
		}		
	}

	// Used to store the last value we found using findGridLoc, so we can use it as the starting position for the next query, 
	// with the assumption that the next query will probably be fairly close
	int lastX=-1;
	int lastY=-1;
	
	// Used for measuring performance in terms of number of loops per call
	public int findCalls=0;
	public int findLoops=0;
	
	public synchronized Point2D findGridLoc(double xp, double yp) {
		findCalls++;
		Point2D minP = new Point2D.Double();
		
		double testgrid[] = new double[9];
	
		// start in the middle
		int x = A1.length/2;
		int y = A2.length/2;
	
		if (lastX!=-1) {
			x=lastX;
		}
		
		if (lastY!=-1) {
			y=lastY;
		}
		for (int i=0;i<testgrid.length;i++) {
			testgrid[i]=Double.MAX_VALUE;
		}
		
		if (gridcrossesWorldEdge) {
			if (Math.abs(xp-gridminx)<180) {
				xp+=360;
			}
		}
		
loop:	while (true) {
	        findLoops++;
	        // We query a 3 x 3 grid, and then shift it around until the point we are searching for is at the center of the grid
			testgrid[0] = calcDistance(x-1, y+1, xp, yp);			
			testgrid[1] = calcDistance(x, y+1, xp, yp);			
			testgrid[2] = calcDistance(x+1, y+1, xp, yp);			
			testgrid[3] = calcDistance(x-1, y, xp, yp);			
			testgrid[4] = calcDistance(x, y, xp, yp);			
			testgrid[5] = calcDistance(x+1, y, xp, yp);			
			testgrid[6] = calcDistance(x-1, y-1, xp, yp);			
			testgrid[7] = calcDistance(x, y-1, xp, yp);			
			testgrid[8] = calcDistance(x+1, y-1, xp, yp);
			
			int minI = -1;
			double minVal = Double.MAX_VALUE;
			for (int i=0; i<testgrid.length; i++) {
				if (Double.isNaN(testgrid[i])) {
					minP.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
					return minP;
				}
				if (testgrid[i]<minVal) {
					minVal=testgrid[i];
					minI=i;
				}				
			}
			
			switch(minI) {
			case 0:
				x--; y++; continue; // loop
			case 1:
				     y++; continue;
			case 2:
				x++; y++; continue;
			case 3:
				x--; continue;
			case 4:
				// Found it, done
				break loop;
			case 5:
				x++; continue;
			case 6:
				x--; y--; continue;
			case 7:
				     y--; continue;
			case 8:
				x++; y--; continue;
			default:
				// Invalid case!
			}
			
		}
		
		lastX=x;
		lastY=y;
		
		// We subtract 1 from each value to account for the extra pixel we added around the edge.  A -1 result, or a result equal to the height or width, means the 
		// requested location is outside the bounds of the image
		minP.setLocation(x-1, y-1);
		return minP;
	}

	// This is a straightforward implementation of the distance between two points, except we skip the square root at the end for performance reasons.
	// Because we are only interested in which points are closest, the square root isn't necessary.
	private double calcDistance(int x, int y, double xp, double yp) {
		if (x<0 || x>=A1.length || y<0 || y>=A2.length) {
			return Double.MAX_VALUE;
		}
		
		double det = A1[x]*B2[y] - A2[y]*B1[x];
				
		double ptx = (B2[y]*C1[x] - B1[x]*C2[y])/det;
		double pty = (A1[x]*C2[y] - A2[y]*C1[x])/det;
		
		double dist = (ptx - xp)*(ptx - xp) + (pty - yp)*(pty - yp);
				
		return dist;			
	}	
	
    
	public static void main(String args[]) {
		
		Main.PO=new ProjObj.Projection_OC(0,0);
		
		HVector p1 = new HVector(0, 0);
		HVector p2 = new HVector(5, 0);

		HVector p3 = new HVector(0, 5);
		HVector p4 = new HVector(5, 5);

		Cell c = new Cell(p3, p4, p2, p1, null);
		
		int gridSize=5;
				
		c.buildGrid(gridSize, gridSize);

		System.out.println(c.findGridLoc(180-0.00,0.00));
		System.out.println(c.findGridLoc(180-2.01,2.01));
		System.out.println(c.findGridLoc(180-2.9,2.9));
		System.out.println(c.findGridLoc(180-5.0,5.0));		
	}	                                                                                   
 }
