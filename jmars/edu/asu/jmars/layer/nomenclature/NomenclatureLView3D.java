package edu.asu.jmars.layer.nomenclature;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView.MarsFeature;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView.NomenclatureSettings;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.gl.text.LabelText;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;

public class NomenclatureLView3D extends LView3D {
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	private NomenclatureLView myLView;
	private int currentImageStateId = -1;
	private int currentLabelStateId = -1;	
	
	public NomenclatureLView3D(Layer layer) {
		super(layer);
		
		exists = true;
		usesDecals = true;
	}
	
	
	/**
	 * Set the LView on this LView3d
	 * @param view
	 */
	public void setLView(NomenclatureLView view){
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
		final int layerState = myLayer.getStateId(NomenclatureLayer.IMAGES_BUFFER);
		
		//if the current layer state for this lview3d is not equal
		// to the state on the layer, something has changed, so 
		// redraw the decals
		if(currentImageStateId != layerState){
			//set the states the same, it can be checked throughout 
			// the process and aborted if it becomes outdated
			currentImageStateId = layerState;
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
							if(layerState != myLayer.getStateId(NomenclatureLayer.IMAGES_BUFFER)){
								//break out of creating new threads if at any
								// time the map layer has changed since this work
								// has started
								break;
							}
							BufferedImage bi = drawLandmarks(d);
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
		// Return value doesn't matter
		return null;
	}
	
	
	private BufferedImage drawLandmarks(Decal decal){
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
	
	/**
	 * @see edu.asu.jmars.layer.Layer.LView3D#getRenderables()
	 * 
	 * Returns the glrenderables representing the labels for each
	 * nomenclature landmark if the labels are visible
	 */
	public ArrayList<GLRenderable> getRenderables(){
		
		final int layerState = myLayer.getStateId(NomenclatureLayer.LABELS_BUFFER);
		
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
		
			NomenclatureSettings settings = myLView.settings;
			//only draw labels if the settings has them turned on
			if(settings.show3DLabels){
				for(MarsFeature mf : myLView.landmarks){
					if(settings.showLandmarkTypes.contains(mf.landmarkType) || settings.showLandmarkTypes.contains(NomenclatureLView.ALL)){
						String text = mf.name;
						float[] color = glColor(settings.labelColor);
						float lat = (float) mf.latitude;
						float lon = (float) mf.longitude;
						
						//LabelText objects expect the longitude to be between -180 and 180
						if(lon > 180){
							lon = lon-360;
						}
						
						LabelText label = new LabelText(text, color, lat, lon);
						//set font size
						label.setFont(ThemeFont.getRegular());
						
						orderedRenderables.add(label);
					}
				}
			}
		}
		return orderedRenderables;
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
		
		
		//only draw landmarks if the settings is checked
		NomenclatureSettings settings = myLView.settings;
		
		//if label scale is -1, this is being called from 3d so
		// use 3d setting entries, otherwise use main view ones
		boolean showPoints = settings.showMainPoints;
		boolean showLabels = settings.showMainLabels;
		if(labelScaleFactor == -1){
			showPoints = settings.show3DPoints;
			showLabels = false;
		}
		
		ArrayList<MarsFeature> featuresToDraw = new ArrayList<MarsFeature>();
		//filter landmarks based on decal bounds and landmark type
		for(MarsFeature mf : myLView.landmarks){
			if(settings.showLandmarkTypes.contains(mf.landmarkType) || settings.showLandmarkTypes.contains(NomenclatureLView.ALL)){
				Point2D worldPt = po.convSpatialToWorld(mf.longitude, mf.latitude);
				// Create a second point, shifted 360 degrees in world coordinates, to check for decals beyond the normal world edge
				Point2D worldPtplus360 = new Point2D.Double(worldPt.getX()+360, worldPt.getY());
				
				//check if landmark is within decal bounds
				if(worldRect.contains(worldPt) || worldRect.contains(worldPtplus360)){
					//add point to draw list
					featuresToDraw.add(mf);
				}
			}
		}
		
		int shapeScale = 1;
		if(labelScaleFactor>1){
			shapeScale = labelScaleFactor;
		}
		double shapeWidth = po.getUnitHeight()/ppd*5*shapeScale;
		
		if(showPoints){
			for(MarsFeature mf : featuresToDraw){
				//draw points
				Point2D pt = po.convSpatialToWorld(mf.longitude, mf.latitude);
				
				Shape shp = new Rectangle2D.Double(pt.getX()-shapeWidth/2, pt.getY()-shapeWidth/2, shapeWidth, shapeWidth);
				Color color = settings.pointColor;
				g2.setColor(color);
				g2.setStroke(new BasicStroke(0));
		        g2.fill(shp);
			}
		}
	
		if(showLabels){
			Font curFont = g2.getFont();
			//draw points and labels
			for(MarsFeature mf : featuresToDraw){
				Point2D pt = po.convSpatialToWorld(mf.longitude, mf.latitude);
		        //draw labels
		        Color color = settings.labelColor;
		        g2.setColor(color);
		        int fontSize = curFont.getSize()*labelScaleFactor;
		        //limit the font size, because otherwise
				// there are weird issues when drawing labels
		        if(fontSize>70){
		        	fontSize = 70;
		        }
		        g2.setFont(new Font(curFont.getName(), curFont.getStyle(), fontSize));
		        
		        float x = (float)pt.getX() + (float)(shapeWidth*1.5);
		        float y = (float)pt.getY() - (float)shapeWidth;
		        
		        g2.drawString(mf.name, x, y);
			}
		}
		
		
		return bufferedImage;
	}	
	
}
