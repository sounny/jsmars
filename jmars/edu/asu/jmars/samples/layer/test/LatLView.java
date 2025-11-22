package edu.asu.jmars.samples.layer.test;

import edu.asu.jmars.layer.*;
import edu.asu.jmars.swing.*;
import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;

/**
 **
 **/
public class LatLView extends Layer.LView
 {
	private static DebugLog log = DebugLog.instance();

	LatLView(LatLayer layer)
	 {
		super(layer);
		MyMouseListener mml = new MyMouseListener();
		addMouseListener(mml);
		addMouseMotionListener(mml);
	 }

	protected Layer.LView _new()
	 {
		return  new LatLView((LatLayer) getLayer());
	 }

	/**
	 ** Always returns null.
	 **/
	protected Object createRequest(Rectangle2D where)
	 {
		return  null;
	 }

	/**
	 ** Accepts a {@link Double} as data, which is interpreted as a
	 ** new latitude line to plot.
	 **/
	public void receiveData(Object layerData)
	 {
		if(layerData instanceof Double)
			updateLatitude( ( (Double) layerData ).doubleValue() );
		else
			log.aprintln("BAD DATA CLASS: " +
						 layerData.getClass().getName());
		repaint();
	 }

	/**
	 ** Clears and repaints the off-screen buffer with a new latitude
	 ** line, and saves the latitude value.
	 **/
	private void updateLatitude(double lat)
	 {
		// Create a G2 that draws in longitude/latitude
		Graphics2D g2 = getProj().createSpatialGraphics(getOffScreenG2());

		clearOffScreen();
		g2.setColor(Color.blue);
		g2.setStroke(new BasicStroke(0));
		for(int i=0; i<360; i++)
			g2.draw(new Line2D.Double(i, lat, i+1, lat));
	 }

	public FocusPanel getFocusPanel()
	 {
		if(focusPanel == null)
			focusPanel = new LatFocus();
		return  focusPanel;
	 }

	private class LatFocus
	 extends FocusPanel
	 implements DataReceiver, ActionListener
	 {
		JTextField txtLatitude;

		public LatFocus()
		 {
			super(LatLView.this);

			JPanel p = new JPanel();
			p.setLayout(new GridLayout(0, 2));

			p.add(new JLabel("Latitude:"));
			txtLatitude = new PasteField();
			txtLatitude.addActionListener(this);
			p.add(txtLatitude);

			add(p);

			// When the focus panel is added to a visible component,
			// we want to register with the layer. Likewise,
			// unregister when we're removed.
			addAncestorListener(
				new AncestorAdapter()
				 {
					public void ancestorAdded(AncestorEvent e)
					 {
						log.printStack(0);
						getLayer().registerDataReceiver(LatFocus.this);
						getLayer().receiveRequest(null, LatFocus.this);
					 }

					public void ancestorRemoved(AncestorEvent e)
					 {
						log.printStack(0);
						getLayer().unregisterDataReceiver(LatFocus.this);
					 }
				 }
				);
		 }

		// Implements DataReceiver
		public void receiveData(Object layerData)
		 {
			log.printStack(0);

			if(layerData instanceof Double)
				txtLatitude.setText(layerData.toString());

			else
				log.aprintln("BAD DATA CLASS: " +
							 layerData.getClass().getName());
		 }

		// Implements ActionListener (for txtLatitude)
		public void actionPerformed(ActionEvent e)
		 {
			getLayer().receiveRequest(new Double(txtLatitude.getText()),
									  this);
		 }
	 }

	private class MyMouseListener extends MouseInputAdapter
	 {
		public void mousePressed(MouseEvent e)
		 {
			getLayer().receiveRequest(
				new Double( getProj().screen.toSpatial(e.getPoint()).getY() ),
				LatLView.this);
		 }

		public void mouseDragged(MouseEvent e)
		 {
			getLayer().receiveRequest(
				new Double( getProj().screen.toSpatial(e.getPoint()).getY() ),
				LatLView.this);
		 }
	 }
 }
