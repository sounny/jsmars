package edu.asu.jmars.layer.mcd;

import java.awt.BasicStroke;
import java.awt.Color;
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

public class MCDLView3D extends LView3D {
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	private DecalSet dSet = null;
	private int currentImageStateId = -1;
	private int currentLabelStateId = -1;
	private MCDLayer myLayer;
	private MCDLView myLView;
	
	public MCDLView3D(MCDLayer layer) {
		super(layer);
		myLayer = layer;
		
		exists = true;
		usesDecals = true;
	}
	
	/**
	 * Set the LView on this LView3d
	 * @param view
	 */
	public void setLView(MCDLView view){
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
	
	/**
	 * @see edu.asu.jmars.layer.Layer.LView3D#getRenderables()
	 * 
	 * Returns the glrenderables representing the labels for each
	 * mcd data point that has their label visible
	 */
	public ArrayList<GLRenderable> getRenderables(){
		
		final int layerState = myLayer.getStateId(MCDLayer.LABELS_BUFFER);
		
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
			for(MCDDataPoint dp : myLayer.getMCDDataPoints()){
				if(layerState != myLayer.getStateId(MCDLayer.LABELS_BUFFER)){
					//stop creating labels if at any time the krc layer
					// has changed state ids since this loop first started
					break;
				}
				//if the label is turned on, add it
				if(dp.showLabel()){
					String text = dp.getName();
					float[] color;
					//if it's selected color the label yellow, else use it's settings
					if(dp == myLView.getFocusPanel().getSelectedDataPoint()){
						color = glColor((Color.YELLOW));
					}else{
						color = glColor(dp.getLabelColor());
					}
					Point2D cen = dp.getPoint();
					float lat = (float) cen.getY();
					float lon = (float) cen.getX();
					
					//LabelText objects expect the longitude to be between -180 and 180
					if(lon > 180){
						lon = lon-360;
					}
					
					LabelText label = new LabelText(text, color, lat, lon);
					//set font size
					label.setFont(ThemeFont.getRegular().deriveFont(dp.getFontSize()*1f));
					
					orderedRenderables.add(label);
				}
			}
		}
		return orderedRenderables;
	}
	
	
	/**
	 * @see edu.asu.jmars.layer.Layer.LView3D#getDecals()
	 * 
	 * Populates the decals with the markers for each mcd
	 * data point that is visible
	 */
	public DecalSet getDecals(){
		
		final int layerState = myLayer.getStateId(MCDLayer.IMAGES_BUFFER);
		
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
							if(layerState != myLayer.getStateId(MCDLayer.IMAGES_BUFFER)){
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
		for(MCDDataPoint dp : myLayer.getMCDDataPoints()){
			//create a FPath for the point
			Point2D[] vertices = {dp.getPoint()};
			FPath path = new FPath(vertices, FPath.SPATIAL_WEST, false);
			//convert to world coords and intersect with decal bounds
			FPath worldPath = path.convertToSpecifiedWorld(po);
			if(worldPath.intersects(worldRect)){
				//get center point
				Point2D worldPt = worldPath.getCenter();
				//draw spike if turned on
				if(dp.showPoint()){
					//create circle from world point
					Shape shp = new Ellipse2D.Double(worldPt.getX()-shapeWidth/2,worldPt.getY()-shapeWidth/2,shapeWidth,shapeWidth);
					
					//fill color
					g2.setColor(dp.getFillColor());
					g2.fill(shp);
					//outline color
					g2.setStroke(new BasicStroke((float)(1.0/ppd)*shapeScale));
					
					//if this is the selected data point, color the outline yellow
					if(dp == myLView.getFocusPanel().getSelectedDataPoint()){
						g2.setColor(Color.YELLOW);
					}else{
					//else color it by its settings
						g2.setColor(dp.getOutlineColor());
					}
					g2.draw(shp);
				}
				
				//draw label if label scale is valid and label is on
				if(labelScaleFactor>0 && dp.showLabel()){
					//draw labels
			        g2.setColor(dp.getLabelColor());
			        
			        int fontSize =  curFont.getSize()*labelScaleFactor;
			        //limit the font size, because otherwise
					// there are weird issues when drawing labels
			        if(fontSize>70){
			        	fontSize = 70;
			        }
			        
			        g2.setFont(new Font(curFont.getName(), curFont.getStyle(), fontSize));
			        
			        float x = (float)worldPt.getX() + (float)(shapeWidth*1.5);
			        float y = (float)worldPt.getY() - (float)shapeWidth;
			        
			        g2.drawString(dp.getName(), x, y);
				}
			}
		}
		
		
		return bufferedImage;
	}
}
