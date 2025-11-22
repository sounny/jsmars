package edu.asu.jmars.layer.landing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;

public class LandingLView3D extends LView3D {
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	private LandingLayer myLayer;
	private int currentStateId = -1;

	public LandingLView3D(LandingLayer layer) {
		super(layer);
		myLayer = layer;
		
		exists = true;
		usesDecals = true;
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
		final int layerState = myLayer.getStateId(0);
		
		
		if(currentStateId != layerState){
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
							if(layerState != myLayer.getStateId(0)){
								//break out of creating new threads if at any
								// time the map layer has changed since this work
								// has started
								break;
							}
							
							BufferedImage bi = drawSites(d);
							if(bi == null){
								//if an image is null, kill the thread
								return;
							}
							d.setImage(bi);
						}
						
						dSet.setRenderable(true);
						currentStateId = layerState;
						
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								mgr.refresh();
							}
						});
					}
				});
				
				manager.start();
			}
		}
		// Return value doesn't matter
		return null;
	}
	
	
	private BufferedImage drawSites(Decal decal){
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
		
		//get craters 
		LandingSiteSettings settings = myLayer.settings;
		ArrayList<LandingSite> sitesToDraw = new ArrayList<LandingSite>();
		ArrayList<LandingSite> selSites = new ArrayList<LandingSite>();
		//craters can be filtered from user settings
		ArrayList<LandingSite> selectedSites = myLayer.getSelectedSites();
		ArrayList<LandingSite> sites = settings.sites;
		
		if(sites == null){
			return null;
		}
		
		//filter craters based on decal bounds
		for(LandingSite ls : sites){
			//check if crater is within decal bounds
			Shape path = ls.calcWorldPath(po);

			//if the path is less than 0, translate by 360 degrees in x
			if(path.getBounds().getX()<0){
				AffineTransform at = AffineTransform.getTranslateInstance(360, 0);
				path = at.createTransformedShape(path);
			}
			
			if(path.intersects(worldRect)){
				//add to craters to draw
				sitesToDraw.add(ls);
				
				//check if crater is selected
				if(selectedSites.contains(ls)){
					selSites.add(ls);
				}
			}
		}
		
		//draw craters
		for(LandingSite ls : sitesToDraw){
			
			Shape shape = ls.calcWorldPath(po);
			
			Color color = ls.getColor();
			color=new Color((settings.alpha<<24) | (color.getRGB()&0xFFFFFF), true);
			g2.setColor(color);

			if (settings.filterSiteFill) {
				g2.fill(shape);
			} else {
		        g2.setStroke(new BasicStroke((float)settings.siteLineThickness/(ppd)));
		        g2.draw(shape);
			}
		}
		
		//draw selected craters
		for (LandingSite ls : selSites) {
			//get selection color
			Color color = ls.getColor();
			int red = 0;
			int green = 0;
			int blue = 0;
			if (color.getRed()<128) {
				red=255;
			}
			if (color.getGreen()<128) {
				green=255;
			}
			if (color.getBlue()<128) {
				blue=255;
			}
			color = new Color(red,green,blue,255);
			
			g2.setColor(color);
			Shape craterShape = ls.calcWorldPath(po); 
	        g2.setComposite(AlphaComposite.Src);
	        float strokeSize = (float)(settings.siteLineThickness/(ppd*1.0));
	        g2.setStroke(new BasicStroke(strokeSize));
			g2.draw(craterShape);
		}	
	
		return bufferedImage;
	}
	
}
