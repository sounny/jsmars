package edu.asu.jmars.layer.krc;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.ui.looknfeel.ThemeFont;

public class KRCLView extends LView{
	private boolean isMain;
	private KRCFocusPanel myFocusPanel;
	private KRCLayer myLayer;
	
	public KRCLView(KRCLayer layer, boolean isMainView, KRCLView3D lview3d) {
		super(layer, lview3d);
		myLayer = layer;
		isMain = isMainView;
		if(isMainView){
			addMouseListener(new DrawingListener(this));
		    lview3d.setLView(this);
		}
	}

	@Override
	protected Object createRequest(Rectangle2D where) {
		//need a repaint here so that when a layer is loaded from
		// a jlf, the main view will repaint itself (when the 
		// panner view is also showing)
		repaint();
		return null;
	}

	@Override
	public void receiveData(Object layerData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	//Used for creating a copy for the panner
	protected LView _new() {
		return new KRCLView(myLayer, false, null);
	}
	
	public String getName(){
		return "KRC Layer";
	}
	
	
	public synchronized void paintComponent(Graphics g){
		//TODO: do something with hi res export?
		
		//Don't draw unless visible
		if(!isVisible() || viewman == null){
			return;
		}
		
		clearOffScreen();
		
		//used for points
		Graphics2D g2 = getOffScreenG2();
		if(g2 == null){
			return;
		}
		g2 = viewman.wrapWorldGraphics(g2);
		g2.setStroke(new BasicStroke(0));
		
		//used for labels
		Graphics g2lbl = getOffScreenG2Direct();
		if(g2lbl == null){
			return;
		}
		
		//draw krc data points
		for(KRCDataPoint krcdp : myLayer.getKRCDataPoints()){
			//only draw points that aren't hidden
			Point2D spPt = krcdp.getPoint();
			Point2D wPt = getProj().spatial.toWorld(spPt);
			Point2D scPt = getProj().world.toScreen(wPt);

			if(krcdp.showPoint()){
				Point2D scTL = new Point2D.Double(scPt.getX()-4, scPt.getY()-4);
				Point2D scBR = new Point2D.Double(scPt.getX()+4, scPt.getY()+4);

				//Convert to world
				Point2D wTL = getProj().screen.toWorld(scTL);
				Point2D wBR = getProj().screen.toWorld(scBR);

				
				//display in world coordinates
				double height = wTL.getY() - wBR.getY();	
				double width = wBR.getX() - wTL.getX(); 
				
				//get shape style from ds and create shape to display
				//circle by default
				Shape shp = new Ellipse2D.Double(wPt.getX()-width/2, wPt.getY()-height/2, width, height);;
				g2.setColor(krcdp.getFillColor());
				g2.fill(shp);

				//if this is the selected data point, color the outline yellow
				if(getFocusPanel() != null && krcdp == getFocusPanel().getSelectedDataPoint()){
					g2.setColor(Color.YELLOW);
					g2.draw(shp);
				}else{
				//else color it by its settings
					g2.setColor(krcdp.getOutlineColor());
					g2.draw(shp);
				}
			}
			if(krcdp.showLabel()){
				Point2D lblPt = new Point2D.Double(scPt.getX()+2, scPt.getY()+15);
				
				Font labelFont = ThemeFont.getBold().deriveFont(krcdp.getFontSize()*1f);
				g2lbl.setFont(labelFont);
				//if it's selected color the label yellow, else use it's settings
				if(getFocusPanel() != null && krcdp == getFocusPanel().getSelectedDataPoint()){
					g2lbl.setColor(Color.YELLOW);
				}else{
					g2lbl.setColor(krcdp.getLabelColor());
				}
				
				g2lbl.drawString(krcdp.getName(), (int)lblPt.getX(), (int)lblPt.getY());
			}
		}
		
		// super.paintComponent draws the back buffers onto the layer panel
		super.paintComponent(g);	
	}
	
	public KRCFocusPanel getFocusPanel(){
		//Do not create fp for the panner
		if(!isMain){
			return null;
		}
		if(focusPanel == null || myFocusPanel ==  null){
			focusPanel = myFocusPanel = new KRCFocusPanel(KRCLView.this);
		}
		return myFocusPanel;
	}
	
	public SerializedParameters getInitialLayerData(){
		ArrayList<KRCDataPoint> dps = myLayer.getKRCDataPoints();
		ArrayList<String> sources = new ArrayList<String>();
		if (myLayer.getElevationSource()!=null) {
			sources.add(myLayer.getElevationSource().getName());
		}
		if (myLayer.getAlbedoSource()!=null) {
			sources.add(myLayer.getAlbedoSource().getName());
		}
		if (myLayer.getThermalInertiaSource()!=null) {
			sources.add(myLayer.getThermalInertiaSource().getName());
		}
		if (myLayer.getSlopeSource()!=null) {
			sources.add(myLayer.getSlopeSource().getName());			
		}
		if (myLayer.getAzimuthSource()!=null) {
			sources.add(myLayer.getAzimuthSource().getName());
		}
		
		return new KRCParams(dps, sources);
	}

	static class KRCParams implements SerializedParameters{
		ArrayList<KRCDataPoint> dataPoints;
		ArrayList<String> sourceNames;
		
		public KRCParams(ArrayList<KRCDataPoint> dps, ArrayList<String> mapNames){
			dataPoints = dps;
			sourceNames = mapNames;
		}
	}
	
}
