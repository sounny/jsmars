package edu.asu.jmars.layer.mcd;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.ui.looknfeel.ThemeFont;

public class MCDLView extends LView{
	private boolean isMain;
	private MCDFocusPanel myFocusPanel;	
	
	public MCDLView(MCDLayer layer, boolean isMainView, MCDLView3D lview3d) {
		super(layer, lview3d);
		isMain = isMainView;
		if(isMainView){
			addMouseListener(new DrawingListener(this));
			lview3d.setLView(this);
		}
	}

	@Override
	protected Object createRequest(Rectangle2D where) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void receiveData(Object layerData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	//Used for creating a copy for the panner
	protected LView _new() {
		return new MCDLView((MCDLayer)getLayer(), false, null);
	}
	
	public String getName(){
		return "MCD Point Layer";
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
		
		//draw mcd data points
		for(MCDDataPoint mcdDP : ((MCDLayer)getLayer()).getMCDDataPoints()){
			//only draw points that aren't hidden
			Point2D spPt = mcdDP.getPoint();
			Point2D wPt = getProj().spatial.toWorld(spPt);
			Point2D scPt = getProj().world.toScreen(wPt);

			if(mcdDP.showPoint()){
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
				g2.setColor(mcdDP.getFillColor());
				g2.fill(shp);

				//if this is the selected data point, color the outline yellow
				if(getFocusPanel() != null && mcdDP == getFocusPanel().getSelectedDataPoint()){
					g2.setColor(Color.YELLOW);
					g2.draw(shp);
				}else{
				//else color it by its settings
					g2.setColor(mcdDP.getOutlineColor());
					g2.draw(shp);
				}
			}
			if(mcdDP.showLabel()){
				Point2D lblPt = new Point2D.Double(scPt.getX()+2, scPt.getY()+15);
				
				Font labelFont = ThemeFont.getBold().deriveFont(mcdDP.getFontSize()*1f);
				g2lbl.setFont(labelFont);
				//if it's selected color the label yellow, else use it's settings
				if(getFocusPanel() != null && mcdDP == getFocusPanel().getSelectedDataPoint()){
					g2lbl.setColor(Color.YELLOW);
				}else{
					g2lbl.setColor(mcdDP.getLabelColor());
				}
				
				g2lbl.drawString(mcdDP.getName(), (int)lblPt.getX(), (int)lblPt.getY());
			}
		}
		
		// super.paintComponent draws the back buffers onto the layer panel
		super.paintComponent(g);	
	}
	
	public MCDFocusPanel getFocusPanel(){
		//Do not create fp for the panner
		if(!isMain){
			return null;
		}
		if(focusPanel == null || myFocusPanel ==  null){
			focusPanel = myFocusPanel = new MCDFocusPanel(MCDLView.this);
		}
		return myFocusPanel;
	}

}
