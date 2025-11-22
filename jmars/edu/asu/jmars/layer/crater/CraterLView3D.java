package edu.asu.jmars.layer.crater;

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
import java.util.HashMap;

import javax.swing.SwingUtilities;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.crater.profiler.ProfilerView;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.util.HighResExport2;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;

public class CraterLView3D extends LView3D{
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	private CraterLayer myLayer;
	private int currentStateId = -1;
	
	
	public CraterLView3D(CraterLayer layer){
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
							
							BufferedImage bi = drawCraters(d);
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
	
	
	private BufferedImage drawCraters(Decal decal){
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

		BufferedImage bufferedImage = createDataImage(worldRect, po, ppd, 1, false);
		
		return bufferedImage;
	}

	/**
	 * Do to the Crater Layer settings allowing the user to pick whether a crater filter applies for the Main View or the 3D view, we can
	 * not 100% generically use this routine for both 3D decals and HiRes (based on MainView) export.  To fit the version called generically
	 * from HiResExport2, this method passes a 'forHiResExport==true' parameter to the real implementation of this method.  This class, and
	 * anything else that wants a 3D decal view, must pass the 'forHiResExport==false' parameter explicitly, and NOT use this generic stub 
	 * implementation.
	 */
	public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor){
		return createDataImage(worldRect, po, ppd, labelScaleFactor, true);
	}
	
	public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor, boolean forHiResExport){
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
		CraterSettings settings = myLayer.settings;
		ArrayList<Crater> cratersToDraw = new ArrayList<Crater>();
		ArrayList<Crater> selCraters = new ArrayList<Crater>();
		
		ArrayList<Crater> matchingCraters = myLayer.getMatchingCraters();
		ArrayList<Crater> selectedCraters = myLayer.getSelectedCraters();
		//craters can be filtered from user settings
		ArrayList<Crater> craters = settings.craters;
		
		if((forHiResExport && settings.filterMainView) || (!forHiResExport && settings.filter3dView)){
			craters = matchingCraters;
		}
		
		if(craters == null){
			return null;
		}
		
		//filter craters based on decal bounds
		for(Crater c : craters){
			//check if crater is within decal bounds
			if(c.getUnprojectedCirclePath().convertToSpecifiedWorld(po).intersects(worldRect)){
				//add to craters to draw
				cratersToDraw.add(c);
				
				//check if crater is selected
				if(selectedCraters.contains(c)){
					selCraters.add(c);
				}
			}
		}

		g2.setStroke(new BasicStroke((float)(labelScaleFactor * settings.craterLineThickness/(ppd))));

		//draw craters
		for(Crater c : cratersToDraw){
			FPath craterPath = c.getUnprojectedCirclePath().convertToSpecifiedWorld(po);
			Shape shape = craterPath.getShape();
			
			Color color = c.getColor();
			color=new Color((settings.alpha<<24) | (color.getRGB()&0xFFFFFF), true);
			g2.setColor(color);

			if (settings.filterCraterFill) {
				g2.fill(shape);
			} else {
		        g2.draw(craterPath.getShape());
			}
		}
		
		//draw selected craters
		for (Crater c : selCraters) {
			//get selection color
			Color color = c.getColor();
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
			Shape craterShape = c.getUnprojectedCirclePath().convertToSpecifiedWorld(po).getShape(); 
	        g2.setComposite(AlphaComposite.Src);
			g2.draw(craterShape);
		}	
		
		if (myLview!=null) {
			ProfilerView pv = ((CraterFocusPanel)((CraterLView)myLview).getFocusPanel()).getProfilerView();

			//if only one crater is selected, draw profiles if they exist and draw them with their respective color
			if (selectedCraters.size() ==1 && pv!=null && pv.isVisible()) {
				HashMap<Shape, Color> pathToColor = pv.getPathToColorMap();
				for(Shape path : pathToColor.keySet()){
					//is possible to have a null path if it is the "average" ProfileData
					if(path != null){
						//get the color
						Color profileColor = pathToColor.get(path);
						g2.setColor(profileColor);
						g2.draw(path);
					}
				}
			}
		}
		
		return bufferedImage;
	}

}
