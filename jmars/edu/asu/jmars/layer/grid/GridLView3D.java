package edu.asu.jmars.layer.grid;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.grid.GridLView.GridSettings;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;

public class GridLView3D extends LView3D{
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	private GridLView myLView;
	private int currentStateId = -1;
	private Map<String, Point2D.Float> latloncache = new HashMap<>();
	
	public GridLView3D(Layer layer) {
		super(layer);
		
		exists = true;
		usesDecals = true;
	}
	
	/**
	 * Set the LView on this LView3d
	 * @param view
	 */
	public void setLView(GridLView view){
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
		final int layerState = myLayer.getStateId(0);
		
		//if the current layer state for this lview3d is not equal
		// to the state on the layer, something has changed, so 
		// redraw the decals
		if(currentStateId != layerState){
			//set the states the same, it can be checked throughout 
			// the process and aborted if it becomes outdated
			currentStateId = layerState;

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
							BufferedImage bi = drawLines(d);
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
	
	
	private BufferedImage drawLines(Decal decal){
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
	
	// Trying to get something closer to screen coordinates
	private Graphics2D createLessTransformedG2(BufferedImage bi, double x, double y, double width, double height, int ppd){
		Graphics2D g2 = bi.createGraphics();
		
		g2 = new GraphicsWrapped(g2, 360, ppd, new Rectangle2D.Double(x, y, width, height), "wrapWorldGraphics");
		//This graphics correction all came from the StampUtil drawShapes method
		Dimension window = new Dimension((int)(width*ppd), (int)(height*ppd));
		double wHeight = window.getHeight();
		double wWidth = window.getWidth();
		AffineTransform world2image = new AffineTransform();
//		// Correct for upside-down-ness
		world2image.scale(1, -1);
		world2image.translate(0, -wHeight);
		world2image.scale(wWidth/width, wHeight/height);
		world2image.translate(-x, -y);
		g2.transform(world2image);	
		
		return g2;
	}
	
	public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor){
		// We use this same code whether we are drawing in the 3D shapemodel view, or HigherResExport, however there are toggles in the 
		// Grid Settings to turn things on/off for 3D vs MainView independently.  Based on the labelScaleFactor, note which
		// context we should be checking for user preferences
		int DRAWING_CONTEXT = labelScaleFactor==-1 ? GridLView.THREE_D : GridLView.MAIN;
		
		BufferedImage bufferedImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));

		double x = worldRect.getX();
		double y = worldRect.getY();
		
		double width = worldRect.getWidth();
		double height = worldRect.getHeight();
		
		//create the array object used for drawing the lines
		Graphics2D[] g2s;
		//create the first graphics object
		Graphics2D g2 = createTransformedG2(bufferedImage, x, y, width, height, ppd);
		//if necessary shift the starting x so that the x+width is within 0-360 
		// and create a second graphics object
		//Populate the array either way
		if(x+width>360){
			x = x-360;
			
			Graphics2D g2b = createTransformedG2(bufferedImage, x, y, width, height, ppd);
			g2s = new Graphics2D[]{g2, g2b};
			
		}else if(x<0){
			x = x+360;
			
			Graphics2D g2b = createTransformedG2(bufferedImage, x, y, width, height, ppd);
			g2s = new Graphics2D[]{g2, g2b};
			
		}else{
			g2s = new Graphics2D[]{g2};
		}
		
		//draw the minor and major lines
		if(myLView.minor.isVisible(DRAWING_CONTEXT)){
			myLView.drawLatLon(-1, g2s, myLView.minor, po, true);
		}
		if(myLView.major.isVisible(DRAWING_CONTEXT)){
			myLView.drawLatLon(-1, g2s, myLView.major, po, true);
		}
				
		if (labelScaleFactor!=-1)
		{  // NOTE: This block is code to draw the lat/lon labels.
			// Try some different graphics objects
			//create the first graphics object
			g2 = createLessTransformedG2(bufferedImage, x, y, width, height, ppd);
			//if necessary shift the starting x so that the x+width is within 0-360 
			// and create a second graphics object
			//Populate the array either way
			if(x+width>360){
				x = x-360;
				
				Graphics2D g2b = createLessTransformedG2(bufferedImage, x, y, width, height, ppd);
				g2s = new Graphics2D[]{g2, g2b};
				
			}else if(x<0){
				x = x+360;
				
				Graphics2D g2b = createLessTransformedG2(bufferedImage, x, y, width, height, ppd);
				g2s = new Graphics2D[]{g2, g2b};
				
			}else{
				g2s = new Graphics2D[]{g2};
			}

			latloncache.clear();
			if (myLView.major.labelIsVisible(DRAWING_CONTEXT)) {
				drawLatText(-1, g2s, myLView.major, po, true, worldRect, ppd, labelScaleFactor);
			}

			if (myLView.minor.labelIsVisible(DRAWING_CONTEXT)) {
				drawLatText(-1, g2s, myLView.minor, po, true, worldRect, ppd, labelScaleFactor);
			}
		}
		
		return bufferedImage;
	}

	/**
	 * Note that this function is very similar, but slightly different from, the function in GridLView.
	 * The differences are that the GridLView version works in screen coordinates, but we don't have screen coordinates for
	 * our virtual view here, so have to translate to world coordinates instead.  
	 * 
	 * If this function changes here, be sure it still matches the version in GridLView and vice versa when HigherResExport is used.
	 * 
	 */
	protected void drawLatText(int id, Graphics2D[] g2s, GridSettings settings, ProjObj po, boolean draw, Rectangle2D worldRect, int ppd, int labelScaleFactor)
	 {
		DecimalFormat decimalFormat = new DecimalFormat();

		if(draw)
		 {
			for (Graphics2D g2 : g2s) {
				
				Font curFont;
				
				if (settings.myLabelFont != null) {
					Font f = settings.myLabelFont.getLabelFont(); // backward comp
					if (f != null) {
						curFont = f;
					} else {
						curFont = g2.getFont();
					}
				} else {
					curFont = g2.getFont();
				}
				
				if (settings.myLabelFont != null) {
					Color c = settings.myLabelFont.getFontFillColor(); // backward comp
					if (c != null) {
						g2.setColor(c);
					} else {
						g2.setColor(settings.getLabelColor());
					}
				} else {
					g2.setColor(settings.getLabelColor());
				}
				
		        int fontSize = curFont.getSize()*labelScaleFactor;
		        //limit the font size, because otherwise
				// there are weird issues when drawing labels
		        if(fontSize>70){
		        	fontSize = 70;
		        }
		        
		        Font newFont = new Font(curFont.getName(), curFont.getStyle(), fontSize); 
		        g2.setFont(newFont);
								
				// WORLD
				double minY = worldRect.getMinY();
				double maxY = worldRect.getMaxY();
				
				double minX = worldRect.getMinX();
				double maxX = worldRect.getMaxX();
	
				// Adjust for the case where our view extends past the bottom of the projection
				if (minY<=-90) minY=-89.9;  // We use -89.9 because exact -90 can give us ambiguous conversions
								
				String coordOrdering = Config.get(Config.CONFIG_LAT_LON,Ordering.LAT_LON.asString());
				Ordering ordering = Ordering.get(coordOrdering);
	
				double spacingInterval = settings.spacing;
	
				double textInset = 5.0/ppd;
				
				double lastLatVal = Double.NaN;
				double lastLonVal = Double.NaN;
						
				for (double y=minY; y<maxY; y+=1.0/ppd) {
					Point2D thisWorldPoint = new Point2D.Double(minX, y);
					Point2D thisSpatialPoint = po.convWorldToSpatial(thisWorldPoint);
				
					if (!Double.isNaN(lastLatVal)) {
						for (double latDraw = -90; latDraw<=90; latDraw+=spacingInterval) {
							if (lastLatVal <= latDraw && thisSpatialPoint.getY() > latDraw) {
								String drawString = decimalFormat.format(ordering.formatLatitude(latDraw)) + ordering.degreeSymbol + ordering.defaultLat;
								float fx = (float) (minX + textInset);
								float fy = (float) (y + textInset);
								Point2D fpoint;
								if (settings ==  myLView.major) { //build cache
									latloncache.put(drawString,  new Point2D.Float(fx, fy));
									g2.drawString(drawString, fx, fy);
								} else if (settings ==  myLView.minor) { //use cache 
									fpoint = latloncache.get(drawString);
								  if (fpoint != null) {
									 Point2D.Float fpoint2 = new Point2D.Float(fx, fy);
									 if (fpoint2.equals(fpoint)) {
										 continue;
									 } else {
										 g2.drawString(drawString, fx, fy); 
									 }
								  } else {
									  g2.drawString(drawString, fx, fy);
								  }
								}								
							}
						}
					}
					
					lastLatVal = po.convWorldToSpatial(thisWorldPoint).getY();
				}
				
				lastLatVal = Double.NaN;
				lastLonVal = Double.NaN;
				
				for (double x=minX; x<maxX; x+=1.0/ppd) {
					Point2D thisWorldPoint = new Point2D.Double(x, minY);
					Point2D thisSpatialPoint = po.convWorldToSpatial(thisWorldPoint);
								
					if (!Double.isNaN(lastLonVal)) {
						for (int lonDraw = 0; lonDraw<=360; lonDraw+=spacingInterval) {
							if (lastLonVal > lonDraw && thisSpatialPoint.getX() <= lonDraw) {
								String drawString = decimalFormat.format(ordering.formatLongitude(lonDraw)) + ordering.degreeSymbol + ordering.getLongitudeSuffix();
								float fx = (float) Math.floor(x + textInset);
								float fy = (float) (minY + textInset);
								Point2D fpoint;
								if (settings == myLView.major) { //build cache
									latloncache.put(drawString,  new Point2D.Float(fx, fy));
									g2.drawString(drawString, fx, fy);
								} else if (settings == myLView.minor) { //use cache 
									fpoint = latloncache.get(drawString);
								  if (fpoint != null) {
									 Point2D.Float fpoint2 = new Point2D.Float(fx, fy);
									 if (fpoint2.equals(fpoint)) {
										 continue;
									 } else {
										 g2.drawString(drawString, fx, fy); 
									 }
								  } else {
									  g2.drawString(drawString, fx, fy);
								  }
								}															
							}
						}
					}
					
					lastLonVal = po.convWorldToSpatial(thisWorldPoint).getX();
				}
			 }
		 }
	 }

}
