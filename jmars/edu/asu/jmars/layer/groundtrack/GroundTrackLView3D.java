package edu.asu.jmars.layer.groundtrack;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;

public class GroundTrackLView3D extends LView3D{
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	private GroundTrackLView myLView;
	
	public GroundTrackLView3D() {
		super(null);
		exists = true;
		usesDecals = true;
	}
	
	/**
	 * Set the LView on this LView3d
	 * @param view
	 */
	public void setLView(GroundTrackLView view){
		myLView = view;
	}
	

	/**
	 * This layer will only be enabled when the shape model
	 * that was active when it was added, is selected. This is
	 * because changing shape models with decals is not yet 
	 * supported.
	 * 
	 * When updating shape models is supported for decals the 
	 * comment should read:
	 * Is always enabled since it is not shape model dependant
	 * @see edu.asu.jmars.layer.Layer.LView3D#isEnabled()
	 */
	public boolean isEnabled(){
		if(mgr.getShapeModel() == null){
			return true;
		}else{
			//first time through, set the shape model
			if(shapeModel.equals("")){
				shapeModel = mgr.getShapeModelName();
			}
		}
		
		return mgr.getShapeModelName().equals(shapeModel);
		
		//TODO: when updating shape models is supported the
		// method should return true always.
//		return true;
	}
	
	// Don't draw in 3D
	public DecalSet getDecals(){
		return null;
	}
	
	private Graphics2D createTransformedG2(BufferedImage bi, double x, double y, double width, double height, int ppd){
		Graphics2D g2 = bi.createGraphics();
		
		//This graphics correction all came from the StampUtil drawShapes method
		Dimension window = new Dimension((int)(width*ppd), (int)(height*ppd));
		double wHeight = window.getHeight();
		double wWidth = window.getWidth();
		AffineTransform world2image = new AffineTransform();
		// Correct for upside-down-ness
		world2image.scale(1, -1);
		world2image.translate(0, -wHeight);
		world2image.scale(wWidth/width, wHeight/height);
		world2image.translate(-x, -y);
		g2.transform(world2image);	
		
		return g2;
	}
	
	public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor){
		BufferedImage bufferedImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));

		double x = worldRect.getX();
		double y = worldRect.getY();
		
		double width = worldRect.getWidth();
		double height = worldRect.getHeight();
		
		//create the first graphics object
		Graphics2D g2 = createTransformedG2(bufferedImage, x, y, width, height, ppd);
		
		g2 = myLView.viewman.wrapWorldGraphics(g2);
		g2 = myLView.getProj().createSpatialGraphics(g2);
		
		g2.setStroke(new BasicStroke((float)(1.0/ppd*labelScaleFactor)));
		for(int i=0; i<myLView.segs.length; i++)
		 {
		    if(i % 3000 == 0)
			myLView.getLayer().setStatus(Color.yellow);
		    Color c = Util.mixColor(
		    		myLView.pars.begColor,
		    		myLView.pars.endColor,
			(double) i / (myLView.segs.length-1)
			);

		    g2.setColor(c);
		    g2.draw(myLView.segs[i]);
		 }
		g2.dispose();
		
		return bufferedImage;
	}

}
