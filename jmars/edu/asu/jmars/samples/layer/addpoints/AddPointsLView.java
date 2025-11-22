package edu.asu.jmars.samples.layer.addpoints;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.Layer.LView;

/**
 * One step up from PointsLView.
 * This LView starts with no points, but wherever the user clicks
 * a new point is added.
 *
 */
public class AddPointsLView extends LView {
	public AddPointsLView(AddPointsLayer layer){
		super(layer);
		addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e) {
				Point pt;
				if (e instanceof WrappedMouseEvent)
					pt = ((WrappedMouseEvent)e).getRealPoint();
				else 
					pt = e.getPoint();
				
				Point2D spatialPt = getProj().screen.toSpatial(pt);
				((AddPointsLayer)getLayer()).addPoint(spatialPt);
			}
		});
	}
	
	protected LView _new() {
		// Create a copy of ourself for use in the panner-view.
		return new AddPointsLView((AddPointsLayer)getLayer());
	}

	protected Object createRequest(Rectangle2D where) {
		// Build a request object for the layer.
		// The layer will respond back with the data.
		return where;
	}

	public void receiveData(Object layerData) {
		// Process the data returned by the layer.
		// Including displaying the data to the screen.
		List<Point2D> pts = (List<Point2D>)layerData;
		drawData(pts);
	}
	
	private void drawData(List<Point2D> pts){
		if (isAlive()){
			clearOffScreen();
			Graphics2D g2 = getOffScreenSpatial();
			float pixelWidth = getProj().getPixelWidth();
			float pixelHeight = getProj().getPixelHeight();
			int sideLengthPixels = 5;

			g2.setColor(Color.yellow);
			g2.setStroke(new BasicStroke(0.0f));
			for(Point2D p: pts){
				g2.fill(new Rectangle2D.Double(p.getX()-(pixelWidth*sideLengthPixels)/2, p.getY() - (pixelHeight*sideLengthPixels)/2,
						pixelWidth*sideLengthPixels, pixelHeight*sideLengthPixels));
			}
			repaint();
		}
	}
	
	public String getName() {
		return "AddPoints";
	}

}
