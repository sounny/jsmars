package edu.asu.jmars.layer.north;

import java.awt.Font;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import javax.swing.JLabel;
import edu.asu.jmars.ui.looknfeel.ThemeFont;

public class NorthArrow {
	private Shape arrow;
	private Shape leftSide;
	private Shape rightSide;
	private int height;
	private int width;
	private int midHeight;
	private double startX;
	private double startY;
	private Font textFont;
	private double scale;
	
	/**
	 * Creates a north arrow object based on the sized passed in.
	 * This size (between 1-5) is translated to a scale (1 to 1.8)
	 * that is used to adjust the size of the arrow being displayed
	 * @param scale A value between 1-5.
	 */
	public NorthArrow(int size){
		//set defaults
		textFont = ThemeFont.getBold().deriveFont(16f);
		
		height = 50;
		width = 36;
		midHeight = 30;
		
		//keep track of the current size, so that we know when
		// we need to recalculate the points for the arrow
		scale = sizeToScale(size);
		//start x and y define the top point of the arrow
		// (the arrow is centered around 0,0)
		buildArrow();
	}
	
	/**
	 * The scale is related to the size and is calculated
	 * with the following: scale = 1+(size-1)*0.2
	 * @param size  An integer value
	 * @return  A double scale value
	 */
	private double sizeToScale(int size){
		return 1+(size-1)*0.2;
	}
	
	private void buildArrow(){
		startX = 0;
		startY = -height*scale/2;
		Point2D botLeft = new Point2D.Double(startX-(scale*width/2), startY+(scale*height));
		Point2D botCenter = new Point2D.Double(startX, startY+(scale*midHeight));
		Point2D botRight = new Point2D.Double(startX+(scale*width/2), startY+(scale*height));
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(botLeft.getX(), botLeft.getY());
		gp.lineTo(startX, startY);
		gp.lineTo(botRight.getX(), botRight.getY());
		gp.lineTo(botCenter.getX(), botCenter.getY());
		gp.closePath();
		
		arrow = gp;
	}
	

	/**
	 * Create a "half arrow" so that half
	 * the north arrow can be filled in the
	 * color, with the other half transparent
	 */
	private void buildLeftArrow(){
		Point2D botCenter = new Point2D.Double(startX, startY+(scale*midHeight));
		Point2D botLeft = new Point2D.Double(startX-(scale*width/2), startY+(scale*height));

		GeneralPath gp = new GeneralPath();
		gp.moveTo(botLeft.getX(), botLeft.getY());
		gp.lineTo(startX, startY);
		gp.lineTo(botCenter.getX(), botCenter.getY());
		gp.closePath();
		
		leftSide = gp;
	}
	
	/**
	 * Create a "half arrow" so that half
	 * the north arrow can be filled in the
	 * color, with the other half transparent
	 */
	private void buildRightArrow(){
		Point2D botCenter = new Point2D.Double(startX, startY+(scale*midHeight));
		Point2D botRight = new Point2D.Double(startX+(scale*width/2), startY+(scale*height));
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo(startX, startY);
		gp.lineTo(botRight.getX(), botRight.getY());
		gp.lineTo(botCenter.getX(), botCenter.getY());
		gp.closePath();
		
		rightSide = gp;
	}
	
	
	/**
	 * Get the arrow shape to draw.  If the arrow was not 
	 * created using the same size specified, recalculate it
	 * and then return.
	 * @param size The size of the arrow.
	 * @return  arrow shape for drawing
	 */
	public Shape getArrow(int size){
		//if the arrow was not built at this size, rebuild
		// all peices before returning
		if(scale != sizeToScale(size)){
			scale = sizeToScale(size);
			buildArrow();
			buildLeftArrow();
			buildRightArrow();
		}
		return arrow;
	}
	
	/**
	 * Scale the arrow from a new passed in scalar value,
	 * instead of an "Arrow Size".  This is called by the
	 * Hi Res export when scaling shapes.
	 * @param scale  Scalar value which the arrow should be
	 * multiplied by.
	 * @return  Shape computed by applying the scale to the
	 * existing size shape
	 */
	public Shape getArrowForScale(int scale){
		double newScale = this.scale*scale;
		this.scale = newScale;
		buildArrow();
		buildLeftArrow();
		buildRightArrow();
		return arrow;
	}
	
	/**
	 * @return A shape defining the left half of the arrow
	 */
	public Shape getLeftArrow(){
		if (leftSide == null){
			buildLeftArrow();
		}
		return leftSide;
	}
	
	/**
	 * @return A shape defining the right half of the arrow
	 */
	public Shape getRightArrow(){
		if (rightSide == null){
			buildRightArrow();
		}
		return rightSide;
	}
	
	/**
	 * @return The x position to draw the "N".  This is centered on
	 * the middle of the arrow, and then offset to the left by half
	 * the width of the "N", so that the "N" is centered with the
	 * arrow.
	 */
	public double getTextX(){
		//make a fake label to find the size of it to center it properly when drawing
		JLabel nString = new JLabel("N");
		nString.setFont(textFont);
		return -nString.getPreferredSize().width/2;
	}
	
	/**
	 * @return The y position to draw the "N".  This is just 
	 * underneath the arrow.  So it is half the height of the arrow
	 * plus half the height of the "N".
	 */
	public double getTextY(){
		//make a fake label to find the size of it to center it properly when drawing
		JLabel nString = new JLabel("N");
		nString.setFont(textFont);
		return height*scale/2 + nString.getPreferredSize().height/2;
	}
	
	/**
	 * @return The width of the arrow (which is width*scale) plus a 
	 * buffer of 20 pixels for the side of the screen.
	 */
	public int getXOffset(){
		return (int)(width*scale) + 20;
	}

	/**
	 * @return The height of the arrow (which is height*scale) plus a 
	 * buffer of 20 pixels for the bottom of the screen.
	 */
	public int getYOffset(){
		return (int)(height*scale) + 20;
	}
	
	/**
	 * Returns the Font used for the 'N' part of the north arrow.
	 * If the passed in font size is not the same size as the 
	 * current font object, a new font object will be created
	 * and then returned.
	 * @param size  Desired font size
	 * @return Font for the 'N' of the arrow
	 */
	public Font getTextFont(int size){
		//if the current font is not the same as the size, create
		// a new font with that size
		if(textFont.getSize() != size){
			textFont = new Font("Dialog", Font.BOLD, size);
		}
		return textFont;
	}
}
