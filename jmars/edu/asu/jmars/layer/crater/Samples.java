package edu.asu.jmars.layer.crater;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapChannelTiled;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapLView;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.StageUtil;
import edu.asu.jmars.util.Util;

/**
 * Storage for sample data as per a profile line segment.
 */
public class Samples {
	final Shape lsegs[];             // Line-segment in world coordinates
	final double t0, t1;           // [0,1] means the entire lseg, [0.5,1] means mid of lseg to end
	final int ppd;                 // Requested pixel-per-degree of data
	
	Point2D[][]  pts;   //  World coordinates of each of the sampled location along the lseg. 
	double[][][] data;    // 	Sampled data as array of doubles for each sampled location
	double[][]   dist;  // 	Linear distance in Km from the start of lseg. 

	public Rectangle2D.Double combinedExtent;
	
	/**
	 * Constructs a Samples object which holds sample data for the specified
	 * line segment as extracted from the input mapData object. Consecutive
	 * samples are spaced at 1/ppd.
	 * 
	 * @param lseg Line segment along which sampling is to be done.
	 * @param ppd Spacing between consecutive samples.
	 */
	public Samples(Shape lsegs[], double t0, double t1, int ppd){
		this.lsegs = lsegs;
		this.t0 = t0;
		this.t1 = t1;
		this.ppd = ppd;
		
		int numLsegs = lsegs.length;

		pts = new Point2D[numLsegs][];
		data = new double[numLsegs][][];
		dist = new double[numLsegs][];
				
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		
		for (int i=0; i<lsegs.length; i++) {
			Rectangle2D thisExtent = expandByXPixelsEachSide(lsegs[i].getBounds2D(), ppd, 0.5);
			double x1 = Math.floor(thisExtent.getMinX() * ppd) / ppd;
			double x2 = Math.ceil(thisExtent.getMaxX() * ppd) / ppd;
			double y1 = Math.floor(thisExtent.getMinY() * ppd) / ppd;
			double y2 = Math.ceil(thisExtent.getMaxY() * ppd) / ppd;
			
			if (x2-x1>180) {  // Wraps over the world meridian, so the 'biggest' value is misleading
				x1=(Math.floor(thisExtent.getMaxX() * ppd) / ppd)-360;
				x2=Math.ceil(thisExtent.getMinX() * ppd) / ppd;
			}
			
			// If we have some extents on one side of the world meridian, and some on the other, let the extent
			// push past 0 or 360 to make the width of the extent smaller.  Map layer MapChannel will properly split
			// it into two segments, being much more efficient than a 360 degree wide request when 99% of the data
			// isn't needed
			if (minX!=Double.MAX_VALUE) {
				if (x1-minX>180) {
					x1 = x1-360;
				}
				if (x2-minX>180) {
					x2 = x2-360;
				}				
				
				if (minX-x1>180) {
					x1 = x1+360;
				}

				if (minX-x2>180) {
					x2 = x2+360;
				}
			}
			
			if (x1<minX) minX=x1;
			if (x2>maxX) maxX=x2;
			if (y1<minY) minY=y1;
			if (y2>maxY) maxY=y2;
			
		}
		
		combinedExtent = new Rectangle2D.Double(minX,minY,Math.max(1.0/ppd,maxX-minX),Math.max(1.0/ppd,maxY-minY));

		if (combinedExtent.getWidth()>360) {
			combinedExtent.setRect(0,  combinedExtent.getY(), 360, combinedExtent.getHeight());
		}
	}
	
	public void sampleData(ArrayList<MapData> mapDatas){
		for (int segNum=0; segNum<lsegs.length; segNum++) {
			double dists[] = perimeterLength(lsegs[segNum]); 
			double segLength = dists[0];
			int nSamples = (int)Math.ceil(ppd * segLength * (t1-t0));

			data[segNum]=new double[nSamples][];
			pts[segNum]=new Point2D[nSamples];
			dist[segNum]=new double[nSamples];
		}
		
		for (MapData mapData : mapDatas) {
			BufferedImage image = mapData.getImage();
			Raster raster = image.getData();
			Rectangle rasterBounds = raster.getBounds();
			
			AffineTransform ext2Pix = StageUtil.getExtentTransform(
					mapData.getImage().getWidth(),
					mapData.getImage().getHeight(),
					mapData.getRequest().getExtent());
			
			double ignoreValues[]=mapData.getRequest().getSource().getIgnoreValue();
			//log.aprintln("extent:"+mapData.getRequest().getExtent()+" rasterBounds:"+rasterBounds);
			Point2D pix = new Point2D.Double();
			
			for (int segNum=0; segNum<lsegs.length; segNum++) {
				Shape lseg = lsegs[segNum];
				double dists[] = perimeterLength(lsegs[segNum]); 
				double segLength = dists[0];
				int nSamples = (int)Math.ceil(ppd * segLength * (t1-t0));
	
				for(int i=0; i<nSamples; i++){ // Loops over the left edge
					double t = t0+((double)i)/(nSamples-1); // TODO: Not quite correct, t never equals 1
					pts[segNum][i] = interpolate(lseg, t);
					dist[segNum][i] = distanceTo(lseg, pts[segNum][i])[1];
					
					if (pts[segNum][i].getX()>=360) {
						pts[segNum][i].setLocation(pts[segNum][i].getX()-360,  pts[segNum][i].getY());
					}
					
					ext2Pix.transform(pts[segNum][i], pix);
					
					if (rasterBounds.contains(pix)) {
						raster.getPixel((int)pix.getX(), (int)pix.getY(), data[segNum][i] = new double[1]);
						for (int b=0; b<1; b++) {
							if (ignoreValues!=null) {
								for (int v=0; v<ignoreValues.length; v++) {
									if (data[segNum][i][b]==ignoreValues[v]) {
										data[segNum][i][b]=Double.NaN;
									}
								}
							}
						}
					} else {
						//data[segNum][i] = null;
					}
				}
			}
		}
	}
		
	public double[][][] getSampleData(){
		return (double[][][])data.clone();
	}
		
	public double[][] getDistances(){
		return (double[][])dist.clone();
	}
	
	public Point2D[][] getSamplePoints(){
		return (Point2D[][])pts.clone();
	}
	
	/**
	 * Computes the perimeter length of the given shape (in world coordinates). The
	 * length is computed using the 
	 * {@link Util#angularAndLinearDistanceWorld(Point2D, Point2D)}
	 * method.
	 * @param shape Shape in world coordinates.
	 * @return Length of perimeter in degrees, kilometers and cartesian-distance.
	 */
	public double[] perimeterLength(Shape shape){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double angularDist = 0;
		double linearDist = 0;
		double cartDist = 0;
		
		while(!pi.isDone()){
			switch(pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double dists[] = Util.angularAndLinearDistanceWorld(lseg.getP1(), lseg.getP2());
			angularDist += dists[0];
			linearDist += dists[1];
			cartDist += lseg.getP2().distance(lseg.getP1());
			pi.next();
		}
		
		return new double[]{ angularDist, linearDist, cartDist };
	}
	
	/**
	 * Linearly interpolates a point given the shape (in world coordinates)
	 * and the parameter <code>t</code>.
	 * @param shape Line-string in world coordinates.
	 * @param t Interpolation parameter <code>t</code>.
	 * @return A point obtained by linear-interpolation using the points
	 *     in the shape, given the parameter <code>t</code>.
	 */
	public Point2D interpolate(Shape shape, double t){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double cartDist = 0;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double segLength = lseg.getP2().distance(lseg.getP1());
			if (currSeg != PathIterator.SEG_MOVETO && ((cartDist + segLength)/totalLength) >= t){
				return Util.interpolate(lseg, (t*totalLength-cartDist)/segLength);
			}
			
			cartDist += segLength;
			pi.next();
		}
		
		return Util.interpolate(lseg, (t*totalLength-cartDist)/(lseg.getP2().distance(lseg.getP1())));
	}
	
	/**
	 * Computes the perimeter length of the given shape (in world coordinates). The
	 * length is computed using the 
	 * {@link Util#angularAndLinearDistanceWorld(Point2D, Point2D)}
	 * method.
	 * @param shape Shape in world coordinates.
	 * @return Length of perimeter in degrees, kilometers and cartesian-distance.
	 */
	public double[] distanceTo(Shape shape, Point2D pt){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double angularDist = 0;
		double linearDist = 0;
		double cartDist = 0;
		double t = uninterpolate(shape, pt, null);
		double lengths[] = perimeterLength(shape);
		double totalLength = lengths[2];
		
		if (t > 1 && t<1.000001) {
			t = 1;
		}
		
		if (t < 0 || t > 1){
			return new double[]{ Double.NaN, Double.NaN, Double.NaN };
		}
		else {
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					first.x = lseg.x1 = lseg.x2 = coords[0];
					first.y = lseg.y1 = lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = coords[0]; lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_CLOSE:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = first.x; lseg.y2 = first.y;
					break;
				}

				// handle the case where we cross the world meridian
				if (lseg.x2-lseg.x1>180) {
					lseg.x2-=360;
				}
				
				double lsegLength = lseg.getP2().distance(lseg.getP1());
				if ((cartDist + lsegLength)/totalLength > t){
					double dists[] = Util.angularAndLinearDistanceWorld(lseg.getP1(), pt);
					angularDist += dists[0]; 
					linearDist += dists[1];
					cartDist += pt.distance(lseg.getP1());
					break;
				}
				double dists[] = Util.angularAndLinearDistanceWorld(lseg.getP1(), lseg.getP2());
				angularDist += dists[0];
				linearDist += dists[1];
				cartDist += lsegLength;
				pi.next();
			}
		}
		
		return new double[]{ angularDist, linearDist, cartDist };
	}
	
	/**
	 * Linearly uninterpolates the parameter <code>t</code> value of the specified
	 * point from its closest approach to the specified shape (in world 
	 * coordinates).
	 * @param shape Line-string in world-coordinates.
	 * @param pt Point for which the parameter <code>t</code> is to be determined.
	 * @param distance If not <code>null</code>, its first element contains
	 *     the minimum distance to one of the segments in the line-string.
	 * @return The parameter <code>t</code> which will give the specified
	 *     point if {@link #interpolate(Shape, double)} is called using it as the
	 *     second parameter. Returns {@link Double#NaN} if the shape contains only
	 *     a single point.
	 * {@see Util#uninterploate(Line2D, Point2D)}
	 */
	public double uninterpolate(Shape shape, Point2D pt, double[] distance){
		double t = Double.NaN;
		
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double cartDist = 0, linearDistToMinSeg = 0;
		double minDistSq = Double.MAX_VALUE;
		Line2D.Double minSeg = null;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double lsegDistSq = lseg.ptSegDistSq(pt);
			if (currSeg != PathIterator.SEG_MOVETO && lsegDistSq < minDistSq){
				minSeg = new Line2D.Double(lseg.x1, lseg.y1, lseg.x2, lseg.y2);
				minDistSq = lsegDistSq;
				linearDistToMinSeg = cartDist;
			}
			
			cartDist += lseg.getP2().distance(lseg.getP1());
			pi.next();
		}
		
		if (minSeg != null){
			double tt = Util.uninterploate(minSeg, pt);
			double minSegLength = minSeg.getP2().distance(minSeg.getP1());
			if (tt < 0 && linearDistToMinSeg > 0)
				tt = 0;
			if (tt > 1 && (linearDistToMinSeg + minSegLength) < totalLength)
				tt = 1;
			t = (linearDistToMinSeg + tt * minSegLength) / totalLength;
			//log.aprintln("pt:"+pt+"  linearDistToMinSeg:"+linearDistToMinSeg+"  uninterpol:"+Util.uninterploate(minSeg, pt)+"  minSeg:"+minSeg.getP1()+","+minSeg.getP2()+"  minSegDist:"+minSeg.getP2().distance(minSeg.getP1())+"  totalLen:"+totalLength);
			
			// fill the distance value
			if (distance != null && distance.length > 0)
				distance[0] = Math.sqrt(minDistSq);
		}

		return t;
	}
	
	
	/**
	 * Sets the view extent to the specified extent. The specified extent is
	 * converted into effective extent by narrowing it to the profile line's extent.
	 * Appropriate diagonal from this extent is the output of the profile line.
	 */
	private  void setViewExtent(Shape newViewExtent, int newppd){

	}
	
	private Rectangle2D expandByXPixelsEachSide(Rectangle2D in, int ppd, double xPixels){
		Rectangle2D.Double out = new Rectangle2D.Double();
		out.setFrame(in);
		
		double hdpp = xPixels * (1.0/ppd);
		out.setFrame(out.getX() - hdpp, out.getY() - hdpp, out.getWidth() + 2*hdpp, out.getHeight() + 2*hdpp);
		
		return out;
	}
	
	/**
	 * @return  The array of shapes that was passed into the constructor
	 * when this object was created.
	 */
	public Shape[] getPathsArray(){
		return lsegs;
	}


}