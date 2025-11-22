package edu.asu.jmars.layer.north;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.util.Util;

public class NorthLView3D extends LView3D{
	private NorthLayer myLayer;
	
	public NorthLView3D(NorthLayer layer) {
		super(layer);
		myLayer = layer;
	}

	public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor){
		BufferedImage bufferedImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));
		//create the graphics object to draw on
		Graphics2D g2 = bufferedImage.createGraphics();
		
		NorthSettings settings = myLayer.getSettings();
		NorthArrow arrow = myLayer.getArrow();
		
		//get the shape to draw
		Shape shape = arrow.getArrow(settings.arrowSize);
		//the x and y offsets take the arrow size and arrow scale into account
		// and then pad with 20 pixels.  Scaling with the label scale factor requires
		// that it is incremented by another 20 pixels for every scale greater than 1.
		int xOffset = arrow.getXOffset()*labelScaleFactor;
		int yOffset = arrow.getYOffset()*labelScaleFactor;
		Point2D currentPoint = new Point2D.Double(bufferedImage.getWidth()-xOffset, bufferedImage.getHeight()-yOffset);
		
		double angle = myLayer.getAngleForArrow();

		//translate for arrow
		g2.translate(currentPoint.getX(), currentPoint.getY());
		//rotate
		g2.rotate(-angle);
		g2.scale(labelScaleFactor, labelScaleFactor);
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
		
		return bufferedImage;
	}
	
	@Override
	public boolean isEnabled() {
		return false;
	}
	
	@Override
	public boolean exists(){
		return false;
	}
}
