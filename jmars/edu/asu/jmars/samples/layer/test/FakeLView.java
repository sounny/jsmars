package edu.asu.jmars.samples.layer.test;

import edu.asu.jmars.*;
import edu.asu.jmars.layer.*;
import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

public class FakeLView
 extends Layer.LView
 {
	private static DebugLog log = DebugLog.instance();

	public FakeLView()
	 {
		super(null);

		addMouseListener(
			new MouseAdapter()
			 {
				public void mouseClicked(MouseEvent e)
				 {
					log.println("Received click");
					if(SwingUtilities.isLeftMouseButton(e))
						draw(e);
					else
						clearOffScreen();
					repaint();
				 }

				public void draw(MouseEvent e)
				 {
					Graphics2D g2 =
						getProj().createSpatialGraphics(getOffScreenG2());
					Point2D sp = getProj().world.toSpatial(getProj().screen.toWorld(e.getPoint())); 						
					g2.drawString("ABC xyz",
								  (float) sp.getX(),
								  (float) sp.getY());
					g2.setPaint(Color.red);
					g2.draw(new Rectangle2D.Double(sp.getX(),
												   sp.getY(),
												   5,
												   1));
				 }
			 }
			);
	 }

	public void receiveData(Object layerData)
	 {
	 }
	
	protected Object createRequest(Rectangle2D where)
	 {
		return  null;
	 }

	protected Layer.LView _new()
	 {
		return  new FakeLView();
	 }
 }

