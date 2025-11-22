package edu.asu.jmars.layer.investigate;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import edu.asu.jmars.layer.*;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.looknfeel.ThemeFont;


public class InvestigateLView extends Layer.LView{

	public InvestigateLayer iLayer;
	private DrawingListener drawingMouseListener = null;
	
	InvestigateLView(InvestigateLayer layer, boolean isMainView, InvestigateLView3D lview3d){
		super(layer, lview3d);
		iLayer = layer;
		if(isMainView){		
			drawingMouseListener = new DrawingListener(this);
			addMouseListener(drawingMouseListener);
			addMouseMotionListener(drawingMouseListener);
		}
	}

	protected Object createRequest(Rectangle2D where) {
		return null;
	}
	public void receiveData(Object layerData) {
	}
	protected LView _new() {
		return new InvestigateLView((InvestigateLayer)getLayer(), false, null);
	}
	
	
	
	public FocusPanel getFocusPanel(){
		//Do not create fp for the panner
		if(iLayer == null){
			return null;
		}
		if(focusPanel == null){
			focusPanel = new InvestigateFocus(this);
		}
		return focusPanel;
	}
	
	public synchronized void paintComponent(Graphics g){
		//Don't draw unless visible (should always be visible...)
		if(!isVisible() || viewman == null){
			return;
		}
		
		clearOffScreen();
		//used for points and lines
		Graphics2D g2 = getOffScreenG2();
		if(g2 == null){
			return;
		}
		g2 = viewman.wrapWorldGraphics(g2);
		g2.setStroke(new BasicStroke(0));
		
		//used for labels
		Graphics g1 = getOffScreenG2Direct();
		if(g1 == null){
			return;
		}

		
		//Draw DataSpikes
		for(DataSpike ds : iLayer.getDataSpikes()){

			//Draw shape
			
			//Get point in screen coordinates
			Point2D pt = getProj().spatial.toScreen(ds.getPoint());
			if(ds.isMarkerOn()){
		
				Point2D tL = new Point2D.Double(pt.getX()-4, pt.getY()-4);
				Point2D bR = new Point2D.Double(pt.getX()+4, pt.getY()+4);

				//Convert to world
				Point2D worldPt = getProj().screen.toWorld(pt);
				tL = getProj().screen.toWorld(tL);
				bR = getProj().screen.toWorld(bR);

				
				//display in world coordinates
				double height = tL.getY() - bR.getY();	
				double width = bR.getX() - tL.getX(); 
				
				//get shape style from ds and create shape to display
				//circle by default
				Shape shp = new Ellipse2D.Double(worldPt.getX()-width/2,worldPt.getY()-height/2,width,height);;
				//square if selected
				if(ds.getShapeStyle().equals(DataSpike.SQUARE_STYLE)){
					shp = new Rectangle2D.Double(worldPt.getX()-width/2,worldPt.getY()-height/2,width,height);
				}
				
				g2.setColor(ds.getFillColor());
				g2.fill(shp);
				g2.setColor(ds.getOutlineColor());
				g2.draw(shp);
			}
			
			//Draw Label
			if(ds.isLabelOn()){
				//adjust original point in screen coordinates
				Point2D lblPt = new Point2D.Double(pt.getX()+2, pt.getY()+15);
				//display in screen coordinates
				int x = (int) lblPt.getX();
				int y = (int) lblPt.getY();
				
				
				Font labelFont = ThemeFont.getBold().deriveFont(ds.getLabelSize()*1f);
				g1.setColor(ds.getLabelColor());
				g1.setFont(labelFont);
				
				g1.drawString(ds.getName(), x, y);
			}
		}
		
		
		// super.paintComponent draws the back buffers onto the layer panel
		super.paintComponent(g);	
	}
	
	
	
	public String getName(){
		return "Investigate Layer";
	}
	
	
	public String getLayerKey(){
		return "Investigate";
	}
	public String getLayerType(){
		return "investigate";
	}
	
// session and layer reload information
	static public class InvestigateParms implements SerializedParameters{
		ArrayList<DataSpike> dataSpikes;
		ArrayList<DataProfile> dataProfiles;
		
		public InvestigateParms(ArrayList ds, ArrayList dp){
			this.dataSpikes = ds;
			this.dataProfiles = dp;
		}
	}
	
	public SerializedParameters getInitialLayerData(){
		return new InvestigateParms(iLayer.getDataSpikes(), iLayer.getDataProfiles());
	}
	
}
