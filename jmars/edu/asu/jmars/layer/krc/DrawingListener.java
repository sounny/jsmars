package edu.asu.jmars.layer.krc;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import edu.asu.jmars.Main;

public class DrawingListener implements MouseInputListener {
	private KRCLView myLView;
	private KRCLayer myLayer;
	
	/**
	 * Creates a mouse listener for a krc layer. Creates 
	 * KRCDataPoints on single left-clicks
	 * @param lview
	 */
	public DrawingListener(KRCLView lview){
		myLView = lview;
		myLayer = (KRCLayer)myLView.getLayer();
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
			String name = "KRC Point "+(myLayer.getKRCDataPoints().size()+1);
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
