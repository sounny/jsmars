package edu.asu.jmars.layer.stamp.chart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;

import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.stamp.LineShape;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;

/**
 * This listener listens to and generates cueing events. Its
 * functionality is completely isolated from the ProfileLineMouseListener.
 * It however, depends upon the currently set profile line on the MapLView.
 */
public class ProfileLineCueingListener extends MouseMotionAdapter {
	private int cueLineLengthPixels = 4;
	GeneralPath baseCueShape;
	Shape cueShape = null;
	
	StampLView myLView;
	
	public ProfileLineCueingListener(StampLView newView){
		super();
		
		myLView=newView;
		GeneralPath gp = new GeneralPath();
		gp.moveTo(0, -cueLineLengthPixels/2);
		gp.lineTo(0, cueLineLengthPixels/2);
		baseCueShape = gp;
	}
							
	public void setCuePoint(Point2D worldCuePoint){
		Shape oldCueShape = cueShape;
		
		if (worldCuePoint == null)
			cueShape = null;
		else
			cueShape = computeCueLine(worldCuePoint);
		
		if (oldCueShape != cueShape)
			myLView.repaint();			
	}
	
	/**
	 * Generate cueing line for the specified mouse coordinates
	 * specified in world coordinates. A profileLine must be set
	 * in the MapLView for this method to be successful.
	 * @param worldMouse Mouse position in world coordinates.
	 * @return A new cueing line or null depending upon whether the
	 *        mouse coordinate "falls within" the profileLine range or not.
	 */
	private Shape computeCueLine(Point2D worldMouse){
		Shape profileLine = getProfileLine();
		if (profileLine == null)
			return null;
		
		double t = ChartView.uninterpolate(profileLine, worldMouse, null, myLView.getProj());
		Shape newCueShape = null;
		if (!Double.isNaN(t) && t >= 0.0 && t <= 1.0){
			Point2D mid = ChartView.interpolate(profileLine, t, myLView.getProj());
			double angle = angle(profileLine, t);
			double scale = cueLineLengthPixels * myLView.getProj().getPixelWidth();
			//log.aprintln("worldMouse:"+worldMouse+"  -> t:"+t+"  -> mid:"+mid+"  -> angle"+angle);
			
			AffineTransform at = new AffineTransform();
			at.translate(mid.getX(), mid.getY());
			at.rotate(angle);
			//at.scale(scale, 1.0);
			at.scale(scale, scale);
			newCueShape = baseCueShape.createTransformedShape(at);
		}
		
		return newCueShape;
	}
	
	/**
	 * Determines the angle at which the line-segment surrounding
	 * the given parameter <code>t</code> is.
	 * @param shape Line-string in world coordinates.
	 * @param t Linear interpolation parameter.
	 * @return Angle of the line-segment bracketing the parameter
	 * <code>t</code> or <code>null</code> if <code>t</code> is
	 * out of range.
	 */
	public double angle(Shape shape, double t){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double linearDist = 0;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = ChartView.perimeterLength(shape, myLView.getProj())[2];
		
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
			
			double segLength = lseg.getP2().distance(lseg.getP1());//Util.angularAndLinearDistanceW(lseg.getP1(), lseg.getP2(), getProj())[1];
			if (currSeg != PathIterator.SEG_MOVETO && ((linearDist + segLength)/totalLength) >= t){
				HVector p1 = new HVector(lseg.x1, lseg.y1, 0);
				HVector p2 = new HVector(lseg.x2, lseg.y2, 0);
				double angle = HVector.X_AXIS.separationPlanar(p2.sub(p1), HVector.Z_AXIS);
				return angle;
			}
			
			linearDist += segLength;
			pi.next();
		}
		
		return Double.NaN;
	}

	
	public void paintCueLine(Graphics2D g2){
		Shape profileLine = getProfileLine();
		if (profileLine != null && cueShape != null){
			g2.setColor(Color.yellow);
			g2.draw(cueShape);
		}
	}
	
	public void mouseMoved(MouseEvent e) {
		Shape profileLine = getProfileLine();
		if (profileLine == null)
			return;
		
		ChartView chartView=myLView.myFocus.getChartView();
		
		Point2D pt = clampedWorldPoint(ChartView.getFirstPoint(profileLine), e);
		
		//Figure out if the shape goes into negative world coordinates, or is
		// in positive world coordinates
		PathIterator pi = profileLine.getPathIterator(null);
		double minX = 0;
		double coords[] = new double[6];
		while(!pi.isDone()){
			pi.currentSegment(coords);
			double newX = coords[0];
			if(newX<minX){
				minX = newX;
			}
			pi.next();
		}
		
		//If the minimum x value is not negative, then check the x coordinate
		// on the mouse point pt, and add 360 to bring it back into the 
		// positive coordinate space of world coordinates
		if(minX>=0 && pt.getX()<0){
			pt = new Point2D.Double(pt.getX()+360, pt.getY());
		}
		
		double[] distance = new double[1];
		Point2D mid = ChartView.interpolate(profileLine, ChartView.uninterpolate(profileLine, pt, distance, myLView.getProj()), myLView.getProj());
		int distInPixels = (int)Math.round(distance[0] * myLView.getProj().getPPD());
//		System.out.println("mid:"+mid+"  pt:"+pt+"  dist:"+distance[0]+"  pixDist:"+distInPixels);
		if (distInPixels <= 50){
			myLView.tooltipsDisabled(true);
			if (chartView!=null) {
				chartView.cueChanged(mid);
			}
			else if(myLView.stampLayer.lineShapes()){
				//Needs the point to be in spatial coordinates
				Point2D spMid = myLView.getProj().world.toSpatial(mid);
				myLView.myFocus.getRadarView().setCuePoint(spMid);
			}
			
			setCuePoint(mid);
		}
		else {
			myLView.tooltipsDisabled(false);
			if (chartView!=null) {
				chartView.cueChanged(null);
			}
			setCuePoint(null);
		}
	}
	
	/**
	 * BaseGlass proxy wraps the screen coordinates, which we do NOT want, so we use the
	 * real point it remembers IF this event is a wrapped one.
	 */
	public Point2D clampedWorldPoint (Point2D anchor, MouseEvent e) {
		Point mousePoint = e instanceof WrappedMouseEvent ? ((WrappedMouseEvent)e).getRealPoint() : e.getPoint();
		Point2D worldPoint = myLView.getProj().screen.toWorld(mousePoint);
		double x = Util.mod360(worldPoint.getX());
		double a = Util.mod360(anchor.getX());
		if (x - a > 180.0) x -= 360.0;
		if (a - x > 180.0) x += 360.0;
		double y = worldPoint.getY();
		if (y > 90) y = 90;
		if (y < -90) y = -90;
		return new Point2D.Double(x, y);
	}

	
	private Shape getProfileLine(){
		Shape profileLine = null;
		
		StampLayer sl = myLView.stampLayer;
		
		if(sl.lineShapes() && sl.getSelectedStamps().size()>0){
			LineShape ls = (LineShape)myLView.stampLayer.getSelectedStamps().get(0);
			profileLine = ls.getPath().get(0);
		}else{
			profileLine = myLView.getProfileLine();
		}
		
		return profileLine;
	}
}

