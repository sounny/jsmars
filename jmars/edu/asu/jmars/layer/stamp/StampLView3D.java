package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.InvestigateData;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.viz3d.renderer.gl.event.IntersectListener;
import edu.asu.jmars.viz3d.renderer.gl.event.IntersectResult;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.core.geometry.Polygon;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;

/**
 * This 3D LView displays data on the ThreeDManager.
 * Currently only does selections and outlines 
 * (not rendered data).
 */
public class StampLView3D extends LView3D{
	private StampLView myLView;
	private StampLayer myLayer;
	
//	private DecalSet dSet = null;
	static volatile int id = 0;
	
	//TODO: this variable can be deleted when updating
	// shape models for decals is supported
	private String shapeModel = "";
	
//	final int LINE_WIDTH = 1;
//	final float LINE_SCALE = 1.0f;
//	float FOV_FILL_ALPHA = 0.3f;
	
	/**
	 * Creates the data necessary for a 3D representation of
	 * this stamp layer.
	 * @param layer
	 */
	public StampLView3D(StampLayer layer) {
		super(layer);
		myLayer = layer;
		
		exists = true;
		//uses decals instead of glrenderables
		// implement getDecals(), instead of getRenderables().
		usesDecals = true;
		numDecalLayers = 3;
		
		
		//add stamp listener on the 3d view
		if (ThreeDManager.isReady()) {
			mgr.addListener(new StampIntersectListener());
		}
	}
		
	public DecalSet getDecals(){

		final int OUTLINES = StampLayer.OUTLINES_BUFFER;
		final int IMAGES = StampLayer.IMAGES_BUFFER;
		final int SELECTIONS = StampLayer.SELECTIONS_BUFFER;
		
		int startingStateIds[] = new int[3];
		startingStateIds[OUTLINES]=myLayer.getStateId(OUTLINES);
		startingStateIds[IMAGES]=myLayer.getStateId(IMAGES);
		startingStateIds[SELECTIONS]=myLayer.getStateId(SELECTIONS);
		
		ArrayList<DecalSet> decalSets = mgr.getLayerDecalSet(this);
		DecalSet outlineSet = decalSets.get(OUTLINES);
		DecalSet imageSet = decalSets.get(IMAGES);
		DecalSet selectionSet = decalSets.get(SELECTIONS);

		List<StampShape> selectedStamps = myLayer.getSelectedStamps();
			
		//only populate outlines if 'hide outlines' is not selected
		if(!myLayer.getSettings().hideOutlines()){
			for (Decal d : outlineSet.getDecals()) {
				if (myLayer.getStateId(OUTLINES)==startingStateIds[OUTLINES]) {
					if (d.getStateId()!=myLayer.getStateId(OUTLINES)) {
						outlineSet.setRenderable(false);
						d.setStateId(myLayer.getStateId(OUTLINES));
						StampUtil.drawOutlines(myLayer, d, startingStateIds[OUTLINES]);
					}
				}
			}
			outlineSet.setRenderable(true);
		}

		//don't draw rendered images for points, vectors, or line shapes
		if(!myLayer.lineShapes() && !myLayer.pointShapes() && !myLayer.vectorShapes()){
			for (Decal d : imageSet.getDecals()) {
				if (myLayer.getStateId(IMAGES)==startingStateIds[IMAGES]) {
					if (d.getStateId()!=myLayer.getStateId(IMAGES)) {
						imageSet.setRenderable(false);
						d.setStateId(myLayer.getStateId(IMAGES));
						StampUtil.drawImages(myLayer, d, startingStateIds[IMAGES]);
					}
				}
			}
			imageSet.setRenderable(true);
		}


		for (Decal d : selectionSet.getDecals()) {
			if (myLayer.getStateId(2)==startingStateIds[SELECTIONS]) {
				if (d.getStateId()!=myLayer.getStateId(SELECTIONS)) {
					selectionSet.setRenderable(false);
					d.setStateId(myLayer.getStateId(SELECTIONS));
					StampUtil.drawSelections(myLayer, d, selectedStamps, startingStateIds[SELECTIONS]);
					selectionSet.setRenderable(true);
				}
			}
			
//			mgr.getWindow().repaint();
		}
		
//		imageSet.setRenderable(true);
//		selectionSet.setRenderable(true);

		// Not actually necessary to return things...
		return outlineSet;
	}
	
	public BufferedImage createDataImage(Rectangle2D worldRect, ProjObj po, int ppd, int labelScaleFactor){
		BufferedImage bufferedImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));
		Graphics g2 = bufferedImage.getGraphics();

		List<StampShape> selectedStamps = myLayer.getSelectedStamps();
			
		int scale = 1;
		if(labelScaleFactor>1){
			scale = labelScaleFactor;
		}
		
		//Draw Outlines
		//only populate outlines if 'hide outlines' is not selected
		if(!myLayer.getSettings().hideOutlines()){
			BufferedImage outlinesImage = StampUtil.getOutlinesImage(myLayer, worldRect, po, ppd, scale);
			g2.drawImage(outlinesImage, 0, 0, null);
		}

		//Draw images
		//only draw rendered images if the layer is not vector, point or line shapes
		if(!myLayer.lineShapes() && !myLayer.pointShapes() && !myLayer.vectorShapes()){
			BufferedImage renderedImage = StampUtil.getRenderedImage(myLayer, worldRect, po, ppd, 0, false);
			g2.drawImage(renderedImage, 0, 0, null);
		}
		
		//Draw selections
		BufferedImage selectionsImage = StampUtil.getSelectionsImage(myLayer, selectedStamps, worldRect, po, ppd, scale, 0, false);
		g2.drawImage(selectionsImage, 0, 0, null);
		
		
		return bufferedImage;
	}
	
	
	/**
	 * Set the LView on this LView3d so that the unique name
	 * can be accessed when creating the Investigate Data to 
	 * display
	 * @param view
	 */
	public void setLView(StampLView view){
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
		if (!ThreeDManager.isReady()) return false;
		
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
	
	
	public InvestigateData getInvestigateData(IntersectResult ir){
		//turn on facet highlighting -- debugging purposes
//		mgr.setHighlightFacet(true);
		
		//Gets the layer name to display as the first part of investigateData	
		String layerName = LManager.getLManager().getUniqueName(myLView);
		//Set the name for the investigate data to display (so the user can
		// differentiate between multiple layers)
		InvestigateData data = new InvestigateData(layerName);
		
		//Use a new proj obj centered at the point, to do spatial to world conversions
		ProjObj po = new ProjObj.Projection_OC(ir.getLongitude(), ir.getLatitude());
		Point2D worldPoint = convertIntersectPtToWorldPt(ir, po);
		Point2D screenPoint = myLView.getProj().world.toScreen(worldPoint);
		
		// Get a list of stamp shapes under the cursor		
		List<StampShape> stamps = StampUtil.findStampsByWorldPt(myLayer.getStamps(), worldPoint, po);
		if (stamps==null) return null;
 			
		List<FilledStamp> filledStamps = myLView.getFilteredFilledStamps();
		
		// For each of the stampShapes, get their individual investigateData 
		// and add that to the invData object	
		stampShape: for (StampShape ss : stamps){
			for (FilledStamp fs : filledStamps){
				if (fs.stamp == ss){					

					boolean isNumeric = fs.pdsi.isNumeric;

					if (isNumeric) {
						String key = fs.stamp.getId() + "-" + fs.pdsi.getImageType();		
						String value = "Invalid";
						
						try {
							HVector vector = myLView.screenPointToHVector(screenPoint, (FilledStampImageType)fs);	
												
							// TODO: What ppd should this request at?
							double floatValue = fs.pdsi.getFloatVal(vector, po, 8);
	
							value = "" + floatValue;
							//if Not a Number, set isNumeric to false
							if(Double.isNaN(floatValue)){
								isNumeric = false;
								continue;
							}
							
							if (floatValue==StampImage.IGNORE_VALUE) {
								// No valid data for this filledStamp, but continue to see if others have values
								continue;
							}
						}catch (Exception e) {
							e.printStackTrace();
						}
						data.add(key, value, fs.pdsi.getUnits(), "ItalicSmallBlue","SmallBlue", isNumeric);
					}
					
					continue stampShape;
				}
			}
			ss.getInvestigateData(data);
		}
		
		return data;
	}
	
	
	private Point2D convertIntersectPtToWorldPt(IntersectResult ir, ProjObj po){
		Point2D spatialPt = new Point2D.Double(ir.getLongitude(), ir.getLatitude());
		Point2D wPt =  po.convSpatialToWorld(spatialPt);
		return wPt;
	}
			
	public ArrayList<GLRenderable> getRenderables() {
		orderedRenderables.clear();
		
		if(!myLayer.getSettings().hideOutlines()){
		
			ArrayList<GLRenderable> fovs = new ArrayList<GLRenderable>();
			
			if (myLView==null) return orderedRenderables;
			
			List<StampShape> stamps = myLView.stampLayer.getVisibleStamps();
			
			if (stamps==null) return orderedRenderables;
			
		    orderedRenderables.clear();
		    fovs.clear();
		    
	        StampLayer stampLayer = myLView.stampLayer;
	
			for (StampShape s : stamps) {
				if (s.isHidden()) continue;
				
	//			 If we've already calculated these polygons once, use the ones we had before, but update the colors
				if (s.get3DPolygon()!=null) {
					Polygon polygons[] = s.get3DPolygon();
					
					for (Polygon p : polygons) {
						Color fillColor;
						Color outlineColor;
				        if (stampLayer.spectraData() || stampLayer.lineShapes() || stampLayer.pointShapes()) {
				        	fillColor = s.getCalculatedColor();
				        	outlineColor = fillColor;
				        } else {
				        	fillColor = myLView.getSettings().getFilledStampColor();
				        	outlineColor = myLView.getSettings().getUnselectedStampColor();
				        }
	
				        if (fillColor!=null && outlineColor!=null) {
							p.setFillColor(glColor(fillColor));
							p.setColor(glColor(outlineColor));
						} else {
							continue;
						}
						p.setAlpha(getAlpha());
						fovs.add(p);
					}
					continue;
				}
				
				
				//using 3 space coordinates (lat/lon/radius) from the db
				Double[] points = s.getLonLatRadius();
				
				//if there are no points, don't bother doing anything further
				if(points.length == 0){
					continue;
				}
				
				HVector[] intercepts = new HVector[points.length/3];
				
				//cycle through the array of points
				for (int i=0; i<intercepts.length; i++){
					//take the lat and lon (i and i+1) of each point and convert to a point on a unit sphere (HVector constructor).
					//Also, adjust the lon by 360
					intercepts[i] = new HVector(360-points[3*i], points[3*i+1]);
	
					//multiply each point by the radius of the point
					double pointRadius = points[3*i+2];
					
					intercepts[i].mulEq(pointRadius);
				}
				
	       	    HVector[][] segmentsOn = new HVector[][] {intercepts};
		        
		        // Create an array so we can cache these onto the StampShape
		        Polygon allPolygons[] = new Polygon[segmentsOn.length];
		        
		        Color fillColor;
		        Color outlineColor;
		        
		        // set the color differently if this is a spectra layer
		        if (stampLayer.spectraData() || stampLayer.lineShapes() || stampLayer.pointShapes()) {
		        	fillColor = s.getCalculatedColor();
		        	outlineColor = fillColor;
		        } else {
		        	fillColor = myLView.getSettings().getFilledStampColor();
		        	outlineColor = myLView.getSettings().getUnselectedStampColor();
		        }
		        
		        
		        int LINE_WIDTH = 1;
		        
		        if (fillColor!=null && outlineColor!=null) {
			        for(int j=0; j<segmentsOn.length; j++){
			            allPolygons[j]=
			            new Polygon(++id, 
			                    toFloat1D(segmentsOn[j]),
			                    glColor(outlineColor), glColor(fillColor), 
			                    LINE_WIDTH,
			                    getAlpha(),
			                    true, false);
			            			            
			            fovs.add(allPolygons[j]);
			        }
			        
	       	        s.set3DPolygon(allPolygons);            
		        }
			}
					
	//		//add fovs to renderables list
			orderedRenderables.addAll(fovs);
		
		}
					
		return orderedRenderables;
	}

	private class StampIntersectListener implements IntersectListener{
		public void setResults(IntersectResult result) {
			//if this layer is the selected one, and it came from a 
			// clicked-triggered event, pass on the 3D intersetc
			if(LManager.getLManager().getActiveLView() == myLView && result.wasClicked()){
				//Use a new proj obj centered at the point, to do spatial to world conversions
				ProjObj po = new ProjObj.Projection_OC(result.getLongitude(), result.getLatitude());
				Point2D worldPoint = convertIntersectPtToWorldPt(result, po);
				
				if (myLView.stamps != null) {		
					List<StampShape> stamps = StampUtil.findStampsByWorldPt(myLayer.getStamps(), worldPoint, po);
					if (stamps.size()>0) {
						StampShape stamp = stamps.get(0);
						StampLayer sLayer = ((StampLayer)myLayer);
						if (!result.wasControlDown()) {
							sLayer.clearSelectedStamps();
						}
						if (stamp!=null) {
							sLayer.toggleSelectedStamp(stamp);
						}
					}
				}
			}
		}
	}
	
}
