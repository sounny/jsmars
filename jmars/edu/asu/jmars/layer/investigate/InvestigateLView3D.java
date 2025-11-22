package edu.asu.jmars.layer.investigate;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.SwingUtilities;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.gl.text.LabelText;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;

public class InvestigateLView3D extends LView3D {
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	private DecalSet dSet = null;
	private InvestigateLayer myLayer;
	private int currentImageStateId = -1;
	private int currentLabelStateId = -1;
	
	/**
	 * Creates a new lview3d for the associated InvestigateLayer
	 * @param layer
	 */
	public InvestigateLView3D(InvestigateLayer layer) {
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
	
	
	/**
	 * @see edu.asu.jmars.layer.Layer.LView3D#getRenderables()
	 * 
	 * Returns the glrenderables representing the labels for each
	 * data spike that has their label visible
	 */
	public ArrayList<GLRenderable> getRenderables(){
		
		final int layerState = myLayer.getStateId(InvestigateLayer.LABELS_BUFFER);
		
		//if the current layer state for this lview3d is not equal
		// to the state on the layer, something has changed, so 
		// redraw the labels
		if(currentLabelStateId != layerState){
			//set the states the same, it can be checked throughout 
			// the process and aborted if it becomes outdated
			currentLabelStateId = layerState;
			
			//first, delete old renderables out of 3d memory
			if(orderedRenderables.size()>0){
				for(GLRenderable gl: orderedRenderables){
					mgr.deleteRenderable(gl);
				}
			}
			
			//clear the list
			orderedRenderables.clear();
			
			//Check labels for all the dataspikes
			for(DataSpike ds : myLayer.getDataSpikes()){
				if(layerState != myLayer.getStateId(InvestigateLayer.LABELS_BUFFER)){
					//stop creating labels if at any time the investigate layer
					// has changed state ids since this loop first started
					break;
				}
				//if the label is turned on, add it
				if(ds.isLabelOn()){
					String text = ds.getName();
					float[] color = glColor(ds.getLabelColor());
					Point2D cen = ds.getPoint();
					float lat = (float) cen.getY();
					float lon = (float) cen.getX();
					
					//LabelText objects expect the longitude to be between -180 and 180
					if(lon > 180){
						lon = lon-360;
					}
					
					LabelText label = new LabelText(text, color, lat, lon);
					//set font size
					label.setFont(ThemeFont.getRegular().deriveFont(ds.getLabelSize()*1f));
					
					orderedRenderables.add(label);
				}
			}
		}
		return orderedRenderables;
	}
	
	
	
	/**
	 * @see edu.asu.jmars.layer.Layer.LView3D#getDecals()
	 * 
	 * Populates the decals with the markers for each dataspike
	 * that has their marker turned on
	 */
	public DecalSet getDecals(){
		
		final int layerState = myLayer.getStateId(InvestigateLayer.IMAGES_BUFFER);
		
		//if the current layer state for this lview3d is not equal
		// to the state on the layer, something has changed, so 
		// redraw the decals
		if(currentImageStateId != layerState){
			//set the states the same, it can be checked throughout 
			// the process and aborted if it becomes outdated
			currentImageStateId = layerState;
			ArrayList<DecalSet> decalSets = mgr.getLayerDecalSet(this);
			dSet = decalSets.get(0);
			dSet.setRenderable(false);
			
			//do the decal drawing on a separate thread, so it doesn't
			// hold up the rest of the application
			final ArrayList<Decal> decals = dSet.getDecals();
			if(!decals.isEmpty()){
				final Thread manager = new Thread(new Runnable() {
					public void run() {
						for(Decal d : decals){
							if(layerState != myLayer.getStateId(InvestigateLayer.IMAGES_BUFFER)){
								//break out of creating new threads if at any
								// time the map layer has changed since this work
								// has started
								break;
							}
							BufferedImage bi = drawSpikes(d);
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
		}
		return dSet;
	}
	
	
	private BufferedImage drawSpikes(Decal decal){
		//TODO: maybe this initial logic should be put into the 
		// decal object, since it's done in almost every LView3D class?
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

		int shapeScale = 1;
		if(labelScaleFactor>1){
			shapeScale = labelScaleFactor;
		}
		double shapeWidth = po.getUnitHeight()/ppd*5*shapeScale;
		Font curFont = g2.getFont();
		//filter spikes
		for(DataSpike ds : myLayer.getDataSpikes()){
			//create a FPath for the point
			Point2D[] vertices = {ds.getPoint()};
			FPath path = new FPath(vertices, FPath.SPATIAL_WEST, false);
			//convert to world coords and intersect with decal bounds
			FPath worldPath = path.convertToSpecifiedWorld(po);
			if(worldPath.intersects(worldRect)){
				//get center point
				Point2D worldPt = worldPath.getCenter();
				
				//draw spike if turned on
				if(ds.isMarkerOn()){
					//create shape from point
					Shape spike = null;
					switch(ds.getShapeStyle()){
						case DataSpike.CIRCLE_STYLE:
							spike = new Ellipse2D.Double(worldPt.getX()-shapeWidth/2,worldPt.getY()-shapeWidth/2,shapeWidth,shapeWidth);
							
							break;
						case DataSpike.SQUARE_STYLE:
							spike = new Rectangle2D.Double(worldPt.getX()-shapeWidth/2,worldPt.getY()-shapeWidth/2,shapeWidth,shapeWidth);
							break;
						default: 
							spike = new Ellipse2D.Double(worldPt.getX()-shapeWidth/2,worldPt.getY()-shapeWidth/2,shapeWidth,shapeWidth);
							break;
					}
					
					g2.setStroke(new BasicStroke((float)1.0/ppd));
					
					//fill color
					g2.setColor(ds.getFillColor());
					g2.fill(spike);
					//outline color
					g2.setColor(ds.getOutlineColor());
					g2.draw(spike);
				}
				
				//draw label if label scale is valid and it is showing
				if(labelScaleFactor>0 && ds.isLabelOn()){
					//draw labels
			        g2.setColor(ds.getLabelColor());
			        
			        int fontSize = ds.getLabelSize()*labelScaleFactor;
			        //limit the font size, because otherwise
					// there are weird issues when drawing labels
			        if(fontSize>70){
			        	fontSize = 70;
			        }
			        g2.setFont(new Font(curFont.getName(), curFont.getStyle(), fontSize));
			        
			        float x = (float)worldPt.getX() + (float)(shapeWidth*1.5);
			        float y = (float)worldPt.getY() - (float)shapeWidth;
			        
			        g2.drawString(ds.getName(), x, y);
				}
			}
		}
		
		return bufferedImage;
	}
}
