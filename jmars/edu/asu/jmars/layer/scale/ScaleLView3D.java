package edu.asu.jmars.layer.scale;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.FontRenderer;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.util.Util;

public class ScaleLView3D extends LView3D {
	private ScaleLayer myLayer;

	public ScaleLView3D(ScaleLayer layer) {
		super(layer);
		myLayer = layer;
	}
	
	public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor){
		BufferedImage bufferedImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));
		//create the graphics object to draw on
		Graphics2D g2 = bufferedImage.createGraphics();
		
		ScaleParameters parms = myLayer.getParameters();
		Rectangle2D box = myLayer.getRulerBox();

		g2.scale(labelScaleFactor, labelScaleFactor);
		
		//draw ruler
		g2.setColor(parms.barColor);
		g2.fill(box);
		
		//draw tick boxes
		g2.setColor(parms.tickColor);
		for(Rectangle2D tick : myLayer.getTickBoxes()){
			g2.fill(tick);
		}
		
		//draw the text
		FontRenderer fr = new FontRenderer(parms.labelFont, parms.fontOutlineColor, parms.fontFillColor);
		fr.setLabel(myLayer.getFontString());
		fr.setBorder(null);
		fr.setAntiAlias(true);
					
		double labelWidth = fr.getPreferredSize().getWidth();
		double labelHeight = fr.getPreferredSize().getHeight();
		
		//determine the location of the label
		double fontX, fontY;
		switch (parms.h_alignment) {
		case Center:
			fontX = box.getX()+box.getWidth()/2-labelWidth/2;
			break;
		case Right:
			fontX = box.getX()+box.getWidth()-labelWidth;
			break;
		case Left:
		default:
			fontX = box.getX();
			break;
		}
		switch (parms.v_alignment) {
		case Below:
			fontY = box.getMaxY()+labelHeight;
			break;
		case Above:
		default:
			fontY = box.getY()+box.getHeight()-labelHeight;
			break;
		}
		
		g2.translate(fontX, fontY);
		fr.paintLabel(g2, myLayer.getFontString(), 0, 0);
		
		return bufferedImage;
	}
	
	@Override
	public boolean isEnabled(){
		return false;
	}
	
	@Override
	public boolean exists(){
		return false;
	}

}
