package edu.asu.jmars.layer.shape2;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;

import javax.swing.SwingUtilities;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.FontRenderer;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.ShapeRenderer;
import edu.asu.jmars.layer.util.features.Style;
import edu.asu.jmars.layer.util.features.Styles;
import edu.asu.jmars.layer.util.features.WorldCacheSource;
import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.gl.text.LabelText;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;

public class ShapeLView3D extends LView3D{
	
	private DecalSet dSet = null;
	
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	private int currentImageStateId = -1;
	private int currentLabelStateId = -1;
	
	private ShapeLayer myLayer;
	
	
	public ShapeLView3D(Layer layer) {
		super(layer);
		
		myLayer = (ShapeLayer)layer;
		
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
	 * This list will contain any of the labels that are visible
	 * for the shapes in the shape layer.
	 */
	public ArrayList<GLRenderable> getRenderables() {
		final int layerState = myLayer.getStateId(ShapeLayer.LABELS_BUFFER);
		
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
			
			//make a rectangle big enough to get all the results
			Rectangle2D rect = new Rectangle2D.Double(-180, -90, 540, 180);
			
			ArrayList<Feature> features = new ArrayList<Feature>();
			while (true) {
				if(layerState != myLayer.getStateId(ShapeLayer.LABELS_BUFFER)){
					//stop creating labels if at any time the krc layer
					// has changed state ids since this loop first started
					break;
				}
				try {
					features.clear();
					Iterator<Feature> results = myLayer.getIndex().queryUnwrappedWorld(rect);
					while (results.hasNext()) {
						Feature f = results.next();
						features.add(f);
					}
					break;
				} catch (ConcurrentModificationException e) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException e1) {
						break;
					}
				}
				if (Thread.currentThread().isInterrupted()) {
					break;
				}
			}		
			
			Styles styles = myLayer.getStyles();
			
			for(Feature f: features){
				if(layerState != myLayer.getStateId(ShapeLayer.LABELS_BUFFER)){
					//stop creating labels if at any time the krc layer
					// has changed state ids since this loop first started
					break;
				}
				if(styles.showLabels.getValue(f)){
					String text = styles.labelText.getValue(f);
					//if the feature has label text, create a LabelText renderable for it
					if(text.length()>0){
						float[] color = glColor(styles.labelColor.getValue(f));
						Point2D cen = f.getPath().getSpatialWest().getCenter();
						float lat = (float) cen.getY();
						float lon = (float) cen.getX();
						
						//LabelText objects expect the longitude to be between -180 and 180
						if(lon > 180){
							lon = lon-360;
						}
						
						LabelText label = new LabelText(text, color, lat, lon);
						int fontStyle = Font.PLAIN;
						String fontStyleStr = styles.labelStyle.getValue(f);
						if(fontStyleStr.equalsIgnoreCase("bold")){
							fontStyle = Font.BOLD;
						}
						else if(fontStyleStr.equalsIgnoreCase("italic")){
							fontStyle = Font.ITALIC;
						}
						
						//set font, font size?
						label.setFont(new Font(styles.labelFont.getValue(f), fontStyle, (int)styles.labelSize.getValue(f)));

						orderedRenderables.add(label);
					}
				}
			
			}
		}
		
		return orderedRenderables;
	}
	
	
	
	/**
	 * @see edu.asu.jmars.layer.Layer.LView3D#getDecals()
	 * 
	 * These decals have been drawn with the shapes that
	 * are in the shape layer.  Both selections and unselections
	 * are drawn.
	 */
	public DecalSet getDecals(){
		final int layerState = myLayer.getStateId(ShapeLayer.IMAGES_BUFFER);
		
		if(currentImageStateId != layerState){
			currentImageStateId = layerState;
			ArrayList<DecalSet> decalSets = mgr.getLayerDecalSet(this);
			dSet = decalSets.get(0);
			dSet.setRenderable(false);
			
			//do the decal drawing on a separate thread, so it doesn't hold up
			// the rest of the application
			final ArrayList<Decal> decals = dSet.getDecals();
			if(!decals.isEmpty()){
				final Thread manager = new Thread(new Runnable() {
					public void run() {
						for(Decal d : decals){
							if(layerState != myLayer.getStateId(ShapeLayer.IMAGES_BUFFER)){
								//break out of creating new threads if at any
								// time the map layer has changed since this work
								// has started
								break;
							}
							
							BufferedImage bi = drawShapesFromDecal(d);
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
	
	private BufferedImage drawShapesFromDecal(Decal decal){
		
		//create a buffered image from the information in the decal
		int ppd = decal.getPPD();
		Point2D minExtent = decal.getMinExtent();
		Point2D maxExtent = decal.getMaxExtent();
		double width = maxExtent.getX()-minExtent.getX();
		double height = maxExtent.getY()-minExtent.getY();
		height = Math.abs(height);
		width = Math.abs(width);
		ProjObj po = decal.getProjection();
					
		//find the intersecting shapes
		Rectangle2D worldRect = new Rectangle2D.Double(minExtent.getX(), maxExtent.getY(), width, height);
		
		return createDataImage(worldRect, po, ppd, -1);
		
	}
	
	public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor){
		
		BufferedImage bufferedImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));
		
		ArrayList<Feature> features = new ArrayList<Feature>();
		while (true) {
			try {
				features.clear();
				Iterator<Feature> results = myLayer.getIndex().queryUnwrappedWorld(worldRect, po);
				while (results.hasNext()) {
					Feature f = results.next();
					features.add(f);
				}
				break;
			} catch (ConcurrentModificationException e) {

				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					break;
				}
			}
			if (Thread.currentThread().isInterrupted()) {
				break;
			}
		}		
		///
		
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

		
		//create two renderers to used for the selected and the unselected shapes
		ShapeRenderer unselRenderer = createRenderer(false, myLayer, ppd);
		ShapeRenderer selRenderer = createRenderer(true, myLayer, ppd);
		
		
		
		ShapeLayerStyles styles = myLayer.getStyles();
		FontRenderer fr = new FontRenderer();
		int shapeScale = 1;
		if(labelScaleFactor>1){
			shapeScale = labelScaleFactor;
		}
		double shapeWidth = po.getUnitHeight()/ppd*5*shapeScale;
		//cycle through features
		for(Feature f: features){
//			Feature f = results.next();
			//always draw the unselected version
			// (this will cause the fill to look correct)
			unselRenderer.draw(g2, null, f, po, shapeScale, ppd);
			
			//if selected then draw that version
			// (will cause the outline to look correct)
			if(myLayer.selections.contains(f)){
				selRenderer.draw(g2, null, f, po, shapeScale, ppd);
			}
			
			if(labelScaleFactor>0 && styles.showLabels.getValue(f)){
				//draw labels
				String label = styles.labelText.getValue(f);
				Color labelColor = styles.labelColor.getValue(f);
				Color labelOutlineColor = styles.labelBorderColor.getValue(f);
				int fontSize = styles.labelSize.getValue(f).intValue()*labelScaleFactor;
				//limit the font size, because otherwise
				// there are weird issues when drawing labels
				if(fontSize>70){
					fontSize = 70;
				}
				int fontStyleInt = Font.PLAIN;
				String fontStyle = styles.labelStyle.getValue(f);
				if (fontStyle.equalsIgnoreCase("plain")) {
					fontStyleInt = Font.PLAIN;
				} else if (fontStyle.equalsIgnoreCase("bold")) {
					fontStyleInt = Font.BOLD;
				} else if (fontStyle.equalsIgnoreCase("italic")) {
					fontStyleInt = Font.ITALIC;
				}
				String fontName = styles.labelFont.getValue(f);
				FPath path = styles.geometry.getSource().getValue(f).convertToSpecifiedWorld(po);
				Point2D pt = path.getCenter();
				
				fr.setFont(new Font(fontName, fontStyleInt, fontSize));
				fr.setForeground(labelColor);
				fr.setOutlineColor(labelOutlineColor);
				
				float x = (float)pt.getX() + (float)(shapeWidth*1.5);
		        float y = (float)pt.getY();
				
				fr.paintLabel(g2, label, x, y);
			}
		}
		
		return bufferedImage;
	}
	
	
	//This code was copied from ShapeLview.createRenderer()
	private ShapeRenderer createRenderer(boolean selBuffer, ShapeLayer shapeLayer, int ppd){
		ShapeLayerStyles styles = shapeLayer.getStyles();
		Set<Style<?>> usedStyles;
		if (selBuffer) {
			styles.showLineDir.setConstant(false);
			styles.showVertices.setConstant(true);
			styles.showLabels.setConstant(false);
			styles.lineColor.setSource(styles.selLineColor.getSource());
			styles.lineDash.setConstant(new LineType());
			styles.lineWidth.setSource(styles.selLineWidth.getSource());
			styles.fillPolygons.setConstant(false);
			styles.antialias.setConstant(false);
			styles.fillPolygons.setConstant(false);
			styles.drawOutlines.setConstant(true);
			styles.fillStyle.setConstant(new FillStyle());
//			usedStyles = selStyles;
		} else {
//			if (!isMainView()) {
//				styles.showLabels.setConstant(false);
//				styles.showLineDir.setConstant(false);
//				styles.showVertices.setConstant(false);
//			}
//			usedStyles = normStyles;
		}
		
		// set the geometry source to a StyleSource<FPath> that will reach into
		// the shapeLayer's index for world coordinate paths
		styles.geometry.setSource(
			new WorldCacheSource(
				styles.geometry.getSource(),
				shapeLayer.getIndex()));
		
//		usedStyles.clear();
		
		for (Style<?> s: styles.getStyles()) {
//			s.setSource(new LogSource(s, usedStyles));
		}
		ShapeRenderer sr = new ShapeRenderer(ppd);
		sr.setStyles(styles);
		return sr;
	}
	
}
