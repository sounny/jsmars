package edu.asu.jmars.layer.mcd;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import edu.asu.jmars.Main;

public class DrawingListener implements MouseInputListener {
	private MCDLView myLView;
	private MCDLayer myLayer;
	
	/**
	 * Creates a mouse listener for a mcd layer. Creates 
	 * MCDDataPoints on single left-clicks
	 * @param lview
	 */
	public DrawingListener(MCDLView lview){
		myLView = lview;
		myLayer = (MCDLayer)myLView.getLayer();
	}
	
	
	@Override
	public void mouseClicked(MouseEvent e) {
		//Don't do anything if it's a right click
		if(SwingUtilities.isRightMouseButton(e)){
			return;
		}
		//only do something if it's a single click
		if(e.getClickCount() == 1){
			//get point in spatial coords
			Point2D spPt = myLView.getProj().screen.toSpatial(e.getPoint());
			String name = "MCD Point "+(myLayer.getMCDDataPoints().size()+1);
			double lat = spPt.getY();
			//lon needs to be in degrees east.
			double lon = 360 - spPt.getX();
			
			//show add dialog
			new AddDataPointDialog(Main.mainFrame, myLView, name, lat, lon, null, false);
		}
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

}
