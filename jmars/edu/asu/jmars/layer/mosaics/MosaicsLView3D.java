package edu.asu.jmars.layer.mosaics;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.ShapeRenderer;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;

public class MosaicsLView3D extends LView3D {
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	private MosaicsLayer myLayer;
	private MosaicsLView myLView;
	
	public MosaicsLView3D(MosaicsLayer layer) {
		super(layer);
		// TODO Auto-generated constructor stub
		myLayer = layer;
		
		exists = true;
		usesDecals = true;
	}
	
	/**
	 * Set the LView on this LView3d
	 * @param view
	 */
	public void setLView(MosaicsLView view){
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
	
	
	public DecalSet getDecals(){
		ArrayList<DecalSet> decalSets = mgr.getLayerDecalSet(this);
		final DecalSet dSet = decalSets.get(0);
		dSet.setRenderable(false);
		
		//do the decal drawing on a separate thread, so it doesn't
		// hold up the rest of the application
		final ArrayList<Decal> decals = dSet.getDecals();
		if(!decals.isEmpty()){
			final Thread manager = new Thread(new Runnable() {
				public void run() {
					for(Decal d : decals){
						BufferedImage bi = drawMosaics(d);
						d.setImage(bi);
					}
					
					dSet.setRenderable(true);
					
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							mgr.refresh();
						}
					});
				}
			});
			
			manager.start();
		}
		// Return value doesn't matter
		return null;
	}
	
	private BufferedImage drawMosaics(Decal decal){
		//create a buffered image from the information in the decal
		int ppd = decal.getPPD();
		Point2D minExtent = decal.getMinExtent();
		Point2D maxExtent = decal.getMaxExtent();
		double width = maxExtent.getX()-minExtent.getX();
		double height = maxExtent.getY()-minExtent.getY();
		height = Math.abs(height);
		width = Math.abs(width);
		ProjObj po = decal.getProjection();
		Rectangle2D worldRect = new Rectangle2D.Double(minExtent.getX(), maxExtent.getY(), width, height);

		BufferedImage bufferedImage = createDataImage(worldRect, po, ppd, -1);

		
		return bufferedImage;
	}
	
	
	public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor){
		
		BufferedImage bufferedImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));
	
		//create the graphics object to draw on
		Graphics2D g2 = bufferedImage.createGraphics();
		//use a wrapped world graphics object so that if the 
		// footprints go passed 360 world coords, they will 
		// still be drawn
		g2 = new GraphicsWrapped(g2, 360, ppd, worldRect, "wrapWorldGraphics");
		
		//This graphics correction all came from the StampUtil drawShapes method
		Dimension window = new Dimension((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));
		double wHeight = window.getHeight();
		double wWidth = window.getWidth();
		AffineTransform world2image = new AffineTransform();
		// Correct for upside-down-ness
		world2image.scale(1, -1);
		world2image.translate(0, -wHeight);
		world2image.scale(wWidth/worldRect.getWidth(), wHeight/worldRect.getHeight());
		world2image.translate(-worldRect.getX(), -worldRect.getY());
		g2.transform(world2image);
		
		
		//get two renderers (selected and unselected)
		ShapeRenderer unselRenderer = myLView.getFeatureRenderer(false);
		ShapeRenderer selRenderer = myLView.getFeatureRenderer(true);
		
		int shapeScale = 1;
		if(labelScaleFactor>1){
			shapeScale = labelScaleFactor;
		}
		for(Feature f : myLayer.getFeatures().getFeatures()){
			unselRenderer.draw(g2, null, f, po, shapeScale);
			
			if(myLayer.selections.contains(f)){
				selRenderer.draw(g2, null, f, po, shapeScale);
			}
		}
		
		return bufferedImage;
	}
	
}
