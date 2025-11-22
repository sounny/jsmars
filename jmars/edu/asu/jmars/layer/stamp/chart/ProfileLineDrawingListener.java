package edu.asu.jmars.layer.stamp.chart;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.map2.MapLView;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.util.Util;

/**
 * Mouse listener for drawing profile line. It also holds the current
 * in-progess profile line and is responsible for drawing it onto the
 * on-screen buffer on a repaint. While drawing the profile line, the
 * LViewManager's status bar is updated on every drag (via 
 * {@link Main#setStatus(String)}) to show the new position, the 
 * spherical distance and the linear distance traversed by the line.
 * 
 * Once the line is built, the LView is notified via its 
 * {@link MapLView#setProfileLine(Shape)} method. The profile
 * line created is either null, if no drag occurred, or an actual line
 * if a drag really occurred.
 */
public class ProfileLineDrawingListener implements MouseInputListener, KeyListener {
	Point2D p2 = null;
	List<Point2D> profileLinePts = new ArrayList<Point2D>();
	boolean closed = false;
	
	StampLView myLView;
	
	public ProfileLineDrawingListener(StampLView newView) {
		myLView = newView;
	}
	
	public void mouseClicked(MouseEvent e) {
		if (!e.isShiftDown()) {
			return;
		}
		
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (e.getClickCount() == 1){
				if (closed){
					profileLinePts.clear();
					myLView.setProfileLine(null);
					myLView.cueChanged(null);
					closed = false;
				}
				
				Point2D p1;
				if (profileLinePts.isEmpty())
					p1 = myLView.getProj().screen.toWorld(e.getPoint());
				else
					p1 = clampedWorldPoint(profileLinePts.get(0), e);
				profileLinePts.add(p1);
				p2 = p1;
				myLView.repaint();
			}
			else if (e.getClickCount() == 2){
				if (!closed){
					Point2D p1;
					if (profileLinePts.isEmpty())
						p1 = myLView.getProj().screen.toWorld(e.getPoint());
					else
						p1 = clampedWorldPoint(profileLinePts.get(0), e);
					profileLinePts.add(p1);
					
					p2 = null;
					myLView.setProfileLine(convert(profileLinePts, null));
					profileLinePts.clear();
					myLView.repaint();
					closed = true;
					
					if (myLView.myFocus.chartView!=null) {
						myLView.myFocus.chartView.mapChanged();
					}
				}
			}
		}
	}
	
	private void clearPath() {
		profileLinePts.clear();
		p2 = null;
		myLView.setProfileLine(null);
		myLView.cueChanged(null);
		closed = false;
		myLView.repaint();
	}
	
	private GeneralPath convert(List<Point2D> pts, Point2D lastPt){
		GeneralPath gp = new GeneralPath();
		List<Point2D> tmp = new ArrayList<Point2D>(pts.size()+1);
		tmp.addAll(pts);
		if (lastPt != null)
			tmp.add(lastPt);
		
		for(Point2D pt: tmp){
			if (gp.getCurrentPoint() == null)
				gp.moveTo((float)pt.getX(), (float)pt.getY());
			else
				gp.lineTo((float)pt.getX(), (float)pt.getY());
		}
		
		return gp;
	}
	
	public void mouseEntered(MouseEvent e) {
		if (!e.isShiftDown()) {
			return;
		}

		myLView.requestFocus();
	}
	
	public void mouseExited(MouseEvent e) {}
	public void mouseMoved(MouseEvent e) {
		if (!e.isShiftDown()) {
			return;
		}

		if (!closed && !profileLinePts.isEmpty()){
			p2 = clampedWorldPoint(profileLinePts.get(0), e);
			
			List<Point2D> points = new ArrayList<Point2D>(profileLinePts);
			points.add(p2);
			Main.setStatusFromWorld(points.toArray(new Point2D[points.size()]));
			
			// Update the view so that it can display the in-progress profile line
			myLView.repaint();
		}
	}
	
	public void mousePressed(MouseEvent e) {
	}
	
	public void mouseReleased(MouseEvent e) {
	}
	
	public void mouseDragged(MouseEvent e) {
	}
	
	public void paintProfileLine(Graphics2D g2){
		if (profileLinePts.isEmpty())
			return;
		
		g2.setColor(Color.yellow);
		g2.draw(convert(profileLinePts, closed? null: p2));
	}

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void keyTyped(KeyEvent e) {
		if (e.getKeyChar() == KeyEvent.VK_ESCAPE){
			clearPath();
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

}
