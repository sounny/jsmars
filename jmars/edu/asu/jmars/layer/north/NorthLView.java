package edu.asu.jmars.layer.north;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;

public class NorthLView extends Layer.LView {

	private NorthLayer myLayer;
	private NorthArrow arrow;
	private NorthSettings settings;
	
	public NorthLView(Layer layerParent, NorthLView3D lview3d) {
		super(layerParent, lview3d);
		myLayer = (NorthLayer) layerParent;
		//buffer for the Arrow
		setBufferCount(1);
		//create north arrow
		arrow = myLayer.getArrow();
		//get settings from layer
		settings = myLayer.getSettings();
	}

	@Override
	protected Object createRequest(Rectangle2D where) {
		repaint();
		return null;
	}

	@Override
	public void receiveData(Object layerData) {
	}

	@Override
	protected LView _new() {
		return new NorthLView(getLayer(), null);
	}
	
	@Override
    public boolean pannerStartEnabled()
    {
        return false;
    }
    
	
	public String getName(){
		return "North Arrow";
	}
	
	public FocusPanel getFocusPanel(){
		if(focusPanel == null){
			focusPanel = new NorthFocusPanel(this);
		}
		return focusPanel;
	}
	
	
	public void paintComponent(Graphics g){
		// Don't try to draw unless the view is visible
		if (!isVisible() || viewman == null) {//@since remove viewman2
			return;
		}
		
		//dont draw for the panner
		if(getChild() == null){
			return;
		}
		clearOffScreen(0);
		Graphics2D g2 = getOffScreenG2Direct(0);
		
		if(g2 == null){
			return;
		}
		
		//create the north arrow based on the correct size
		Shape shape = arrow.getArrow(settings.arrowSize);
		Point2D currentPoint = new Point2D.Double(getWidth()-arrow.getXOffset(), getHeight()-arrow.getYOffset());
		
		double angle = myLayer.getAngleForArrow();
		
		//translate for arrow
		g2.translate(currentPoint.getX(), currentPoint.getY());
		//rotate
		g2.rotate(-angle);
		//draw outline
		g2.setColor(settings.arrowColor);
		g2.setStroke(new BasicStroke(settings.outlineSize));
		//always draw the outline
		g2.draw(shape);
		//check booleans to fill left or right
		if(settings.fillLeft){
			g2.fill(arrow.getLeftArrow());
		}
		if(settings.fillRight){
			g2.fill(arrow.getRightArrow());
		}
		//draw text
		if(settings.showText){
			g2.setColor(settings.textColor);
			g2.setFont(arrow.getTextFont(settings.fontSize));
			g2.drawString("N", (int)arrow.getTextX(), (int)arrow.getTextY());
		}
		
		super.paintComponent(g);
	}
	
	
}
