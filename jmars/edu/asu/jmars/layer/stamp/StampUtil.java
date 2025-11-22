package edu.asu.jmars.layer.stamp;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.graphics.GraphicsWrapped;
import edu.asu.jmars.layer.stamp.StampLView.DrawFilledRequest;
import edu.asu.jmars.layer.stamp.focus.OutlineFocusPanel;
import edu.asu.jmars.layer.stamp.focus.SpectraView;
import edu.asu.jmars.layer.stamp.focus.StampFocusPanel;
import edu.asu.jmars.layer.stamp.focus.OutlineOrderDialog.OrderRule;
import edu.asu.jmars.layer.stamp.radar.FilledStampRadarType;
import edu.asu.jmars.layer.stamp.radar.FullResHighlightListener;
import edu.asu.jmars.layer.stamp.radar.RadarFocusPanel;
import edu.asu.jmars.layer.stamp.radar.RadarHorizon;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.GeomSource;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.emory.mathcs.backport.java.util.Collections;

public class StampUtil {

    private static Area area360 = null;
    private static AffineTransform shift120 = null;
    private static AffineTransform shift360 = null;
    private static AffineTransform leftShift360 = null;
        
    static {
        GeneralPath gp360 = new GeneralPath();
        gp360.moveTo(0, -90.0);
        gp360.lineTo(0, 90.0);
        gp360.lineTo(360, 90.0);
        gp360.lineTo(360, -90.0);
        gp360.closePath();
    
        area360=new Area(gp360);

    	shift120 = new AffineTransform();
    	shift120.translate(120, 0);
    	
    	shift360 = new AffineTransform();
    	shift360.translate(360, 0);
    	
    	leftShift360 = new AffineTransform();
    	leftShift360.translate(-360, 0);
    }
    
    
	/**
	 * Calls getOutlinesImage and sets that image on the passed in decal.
	 * Gets the extent, ppd, and projection from the decal and uses it to
	 * create the proper Outlines Image.
	 * @param layer  This stamp layer
	 * @param decal  Decal which the image will be created for and set on
	 * @param stateId  Current state id when this method is called
	 */
	public static void drawOutlines(StampLayer layer, Decal decal, int stateId) {
		final int requestStateId = stateId;
		if (requestStateId < decal.getStateId()) {
			return;
		}
		
		if (layer.globalShapes()) return;
		
		int ppd = decal.getPPD();

		Point2D minExtent = decal.getMinExtent();
		Point2D maxExtent = decal.getMaxExtent();
		
		double width = maxExtent.getX()-minExtent.getX();
		double height = maxExtent.getY()-minExtent.getY();
		
		height = Math.abs(height);
		width = Math.abs(width);
		
		ProjObj po = decal.getProjection();
		
		Rectangle2D worldRect = new Rectangle2D.Double(minExtent.getX(), maxExtent.getY(), width, height);
		
		if (requestStateId < decal.getStateId()) {
			return;
		}
		BufferedImage bufferedImage = getOutlinesImage(layer, worldRect, po, ppd, 1);
		
		if (bufferedImage==null || requestStateId < decal.getStateId()) {
			return;
		}
		
		decal.setImage(bufferedImage);
	}
    

	/**
	 * Draws stamp outlines in a buffered image. Outlines are only redrawn if the
	 * projection, outline colors, or the in-view stamp list have changed since
	 * the last drawing (or if being drawn for the first time). Otherwise,
	 * outlines are simply drawn to the screen with existing buffer contents.
	 */
	/**
	 * @param layer      This stamp layer
	 * @param worldRect  The rectangle defining the bounds of the image to draw
	 * @param po         The projection the image is in
	 * @param ppd        The ppd the image is at
	 * @param lineScale  The value to scale the lines (can be 1)
	 * @return A buffered image with the intersecting stamps outlines drawn in it.
	 */
	public static BufferedImage getOutlinesImage(StampLayer layer, Rectangle2D worldRect, ProjObj po, int ppd, int lineScale) {
	
		BufferedImage bufferedImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));

//		pool.execute(tasked(new Runnable() {
//			public void run() {
//				synchronized(drawOutlines1Lock) {
					
					List<StampShape> stamps = findStampsByWorldRect(layer.getVisibleStamps(), worldRect, po);

//					Collections.reverse(stamps);
					
					// Check for various reasons to NOT draw the outlines
					
//					// Reason 1: This is not the most recent sequence, ie there's already been another request to redraw, so don't bother
//					if (seq != drawOutlines1) {
//						return;
//					}
					
//					// Reason 2: We don't actually have any stamps to draw
//					if (stamps==null || stamps.size()<1) {
//						return;
//					}

//					// Toggle the entire outline buffer on or off based on the user's selected setting.  May be the only thing that's changed
//					setBufferVisible(OUTLINES, !getSettings().hideOutlines());

//					// Reason 3: The user has selected the 'Hide Outlines' checkbox.  
//					if (getSettings().hideOutlines()) {
//						return;
//					}
					
					// Collect some user settings that trigger redraws upon change
					Color unsColor = layer.getSettings().getUnselectedStampColor();
					Color fillColor = layer.getSettings().getFilledStampColor();
					
					// wind vectors
					double mag = layer.getSettings().getMagnitude();
					double originMag = layer.getSettings().getOriginMagnitude();
					Color originColor = layer.getSettings().getOriginColor();
				
					// If all of these values are the same as they were last time, there's no need to redraw
//		            if ((lastStamps == stamps) && (lastUnsColor == unsColor) &&
////		                (projHash == Main.PO.getProjectionSpecialParameters().hashCode()) &&
//		                (fillColor.equals(lastFillColor)) && (lastMag == mag) && (lastOriginColor == originColor) &&
//		                (lastOriginMag == originMag) ) {
//		            	return;
//		            }
		            		            	
		            // If we've made it this far, something's changed.  Clear the buffer so we can redraw fresh
//	                clearOffScreen(OUTLINES);
	                
	                try {
		    			Graphics2D g2 = bufferedImage.createGraphics();
		    			
		    			// Wrap g2 so if we get tiles outside of 0-360 space, they'll still render
		    			g2 = new GraphicsWrapped(g2,360, ppd, worldRect,"wrapWorldGraphics");
		    			
		    			
//		    			if (g2 == null)
//		    				return;
		    		
		    			///
//		    			Rectangle2D rect = getWorldWindow();
//		    			Dimension window = getScreenSize();
		    			
//		    			Rectangle2D rect = new Rectangle2D.Double(0, -90, 90, 180);
//		    			Rectangle2D rect = new Rectangle2D.Double(minExtent.getX(), maxExtent.getY(), width, height);
		    			
		    			Dimension window = new Dimension((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));
		    			
		    			AffineTransform world2image = new AffineTransform();

		    			// Correct for upside-down-ness
		    			world2image.scale(1, -1);
		    			world2image.translate(0, -window.getHeight());

		    			world2image.scale(window.getWidth()  / worldRect.getWidth(),
		    							  window.getHeight() / worldRect.getHeight());
		    			world2image.translate(-worldRect.getX(),
		    								  -worldRect.getY());

		    			g2.transform(world2image);
		    			
						g2.setStroke(new BasicStroke((float)(1.0/ppd)*lineScale));
						
		    			g2.setPaint(unsColor);
		    			
		    			if(layer.vectorShapes() || layer.pointShapes() || layer.spectraData()) {
			    			// Custom color code
		    				if (layer==null || layer.viewToUpdate==null || layer.viewToUpdate.myFocus==null) return null;
			    			OutlineFocusPanel ofp = layer.viewToUpdate.getOutlineFocusPanel();
			    			
			    			double min = 0;
			    			double max = 0;
	
			    			int columnToColor = -1;				    			
		    				Color colors[] = new Color[0];
		    				
		    				if (ofp!=null) {
		    					min = ofp.getMinValue();
				    			max = ofp.getMaxValue();
		    					columnToColor=ofp.getColorColumn();
		    					colors=ofp.getColorMap();
		    				}
		    				
			    			// May or may not apply to outlines
							if (layer.vectorShapes()) {   // Such as Wind
								double userScale = layer.getSettings().getOriginMagnitude();
				    			int factor=(int)(Math.log(ppd)/Math.log(2));
								double degrees = 0.1*(32.0/ppd)*(factor/4.0)*userScale; 
	
	
				    			for (StampShape stamp : stamps) {
	//								if (seq != drawOutlines1) {
	//									return;
	//								}
	
									Point2D p=((WindShape)stamp).getOrigin();
				    				Ellipse2D oval = new Ellipse2D.Double(p.getX()-degrees, (p.getY()-degrees), 2*degrees, 2*degrees);
	
				    				g2.setPaint(originColor);
				    				g2.fill(oval);
				    									    				
					    			g2.setPaint(unsColor);
							    			List<GeneralPath> stampPaths = stamp.getPath(po);
							    			
							    			for (GeneralPath path : stampPaths) {
							    				g2.draw(path);
							    			}
				    				
				    			}
							} else if (layer.pointShapes()) {   // Such as MOLA Shots
				    			double userScale = layer.getSettings().getOriginMagnitude();

				    			String spotSize = layer.getParam(StampLayer.SPOT_SIZE);
				    			
								double degrees = 0;  // Approximate number of degrees in the diameter of a 150 meter spot
								
								try {
									degrees = Double.parseDouble(spotSize);
								} catch (Exception e) {
									// If SPOT_SIZE was unspecified or otherwise invalid, this might fail.  In that case,
									// it will default to 0 degrees in size, but be drawn at 1 pixel 
								}

								degrees = degrees / 2.0;				
								
								degrees = degrees * userScale;
								
								double pixels = ppd * degrees;
								
								// Make the spot size at least 1 pixel on the screen, no matter how far we zoom out.
								if (pixels<1.0) {
									degrees = 1.0 / ppd;
								}
													    			
				    			for (StampShape stamp : stamps) {
//									if (seq != drawOutlines1) {
//										return;
//									}
	
				    				Color color = stamp.getCalculatedColor();

									g2.setPaint(color);

				    				List<Area> stampPaths = stamp.getFillAreas(po);
							    	
							    	for (Area path : stampPaths) {
							    		g2.fill(path);
							    	}
				    			}
							} else if (layer.spectraData()) { // Such as TES
								
								boolean drawAsRings = false;
								
								if (stamps.size()>0) {
									drawAsRings = stamps.get(0).stampLayer.getSettings().drawAsRing();
								}

//								long startTime = System.currentTimeMillis();
								// TODO: Update non-spectra areas to work with the new code
								
								StampGroupComparator orderSort = ofp.getOrderSort();

								Collections.sort(stamps, orderSort);
								
								HashMap<StampShape, List<Area>> cachedAreas = new HashMap<StampShape, List<Area>>();
								HashMap<StampShape, List<GeneralPath>> cachedPaths = new HashMap<StampShape, List<GeneralPath>>();

				    			// Potentially expensive, don't do this if we don't need to
				    			if (drawAsRings) {
					    			for (StampShape stamp : stamps) {
					    				cachedPaths.put(stamp, stamp.getPath(po));
					    			}
				    			} else {
					    			// Do this in one quick pass, to try and avoid synchonization lag
					    			for (StampShape stamp : stamps) {
					    				cachedAreas.put(stamp, stamp.getFillAreas(po));
					    			}
				    			}

				    			for (StampShape stamp : stamps) {
	//								if (seq != drawOutlines1) {
	//									return;
	//								}
									
				    				if (stamp.isHidden) continue;
				    				
				    				Color color = stamp.getCalculatedColor();

				    				if (color==null) continue;
				    				
				    				int alphaVal = stamp.stampLayer.getSettings().getFilledStampColor().getAlpha();
				    				g2.setPaint(new Color((alphaVal<<24) | color.getRGB() & 0xFFFFFF, true));
				    				
//				    				g2.setPaint(color);
				    				
				    				AffineTransform shift360 = new AffineTransform();
				    				shift360.translate(360,0);
				    			
				    				// Generic paths reference used for the boresight drawing below
				    				List<?> paths = new ArrayList<Shape>();
				    				
									if (drawAsRings) {
			    						List<GeneralPath> stampPaths = cachedPaths.get(stamp);
			    						paths = stampPaths;
			    						
			    						g2.setStroke(new BasicStroke(stamp.stampLayer.getSettings().getRingWidth()));

								    	for (GeneralPath path : stampPaths) {

								    		g2.draw(path);
								    		
						    				Area areaPlus360 = new Area(path);
						    				areaPlus360.transform(shift360);
		
						    				g2.draw(areaPlus360);
								    	}
									} else {
					    				List<Area> stampPaths = cachedAreas.get(stamp);				    			
					    				paths = stampPaths;
					    				
										for (Area path : stampPaths) {
								    		g2.fill(path);
								    		
						    				Area areaPlus360 = new Area(path);
						    				areaPlus360.transform(shift360);
		
						    				g2.fill(areaPlus360);
								    	}
									}
							    	
							    	Point2D bp = stamp.getBoresight();
							    	if (layer.getSettings().showBoresight()) {
							    		drawBoresight(g2, po, bp, color, paths, false);
							    	} 
				    			}											    			
							}
//			    			System.out.println("draw time: " + (System.currentTimeMillis()-startTime) + " for decal " + decal + " : " + decal.getMinExtent() + " state id = " + decal.getStateId());
						} else {  // Normal stamp outlines
			    			for (StampShape stamp : stamps) {
//								if (seq != drawOutlines1) {
//									return;
//								}

								///
								if (layer.getSettings().getFilledStampColor().getAlpha() != 0) {
									g2.setPaint(fillColor);

//				    				List<GeneralPath> stampPaths = stamp.getPath(po);
//							    	
//							    	for (GeneralPath path : stampPaths) {
//							    		g2.fill(path);
//							    	}
//							    	
				    				List<Area> stampPaths = stamp.getFillAreas(po);
				    				
							    	for (Area path : stampPaths) {
							    		g2.fill(path);
							    		
					    				Area areaPlus360 = new Area(path);
					    				areaPlus360.transform(shift360);

					    				g2.fill(areaPlus360);
							    	}

								}								
								///
								
								
				    			g2.setPaint(unsColor);
			    				List<GeneralPath> stampPaths = stamp.getPath(po);
						    	
						    	for (GeneralPath path : stampPaths) {
						    		g2.draw(path);
						    		
						    		path.transform(shift360);
						    		g2.draw(path);
						    	}
						    	
						    	Point2D bp = stamp.getBoresight();
						    	if (layer.getSettings().showBoresight()) {
						    		drawBoresight(g2, po, bp, unsColor, stampPaths, false);
						    	}

			    			}
						}
		    							        
//		    			Color selectedColor = new Color(~layer.getSettings().getUnselectedStampColor().getRGB());
//		    			g2.setColor(selectedColor);		
		    			
//		    			List<StampShape> selectedStamps = layer.getSelectedStamps();
//		    			for (StampShape ss : selectedStamps) {
//		    				if (ss.isHidden()) continue;
//
//		    				// For spectral layers, colorized selected spots the same as their plot colors
//		    				if (layer.spectraData()) {
//			    				SpectraView spectraView = ((StampFocusPanel)(layer.viewToUpdate.getFocusPanel())).getSpectraView();
//					        	Color c = spectraView.getColorForStamp(ss);
//					        	
//					        	if (c!=null) {
//					        		g2.setColor(c);
//					        	} else {
//					       			Color contrastColor = new Color(StampUtil.getContrastVersionForColor(layer.getSettings().getUnselectedStampColor()));
//									g2.setColor(contrastColor);			        		
//					        	}
//		    				}
//				        	
//		    				g2.setStroke(new BasicStroke(1));
//		    				
//		    				for (GeneralPath path : ss.getPath(po)) {
//		    					g2.draw(path);
//					    		path.transform(shift360);
//					    		g2.draw(path);
//
//		    				}
//		    			}
						
//		                lastStamps = stamps;
//		                lastUnsColor = unsColor;
//						lastFillColor = fillColor;
//		                lastMag = mag;
//		                lastOriginColor = originColor;
//		                lastOriginMag = originMag;
	                } finally {
					//	repaint();
	                }
	                
	                
//	     decal.setImage(bufferedImage);
	     return bufferedImage;           
//	            }
//			}
			
			// SEND this buffer to 3D layer
			
//		}));
	}
	
    public static Shape getProjectedShape(double longitude, double latitude, double radius, ProjObj po) {
    	return getUnprojectedCirclePath(longitude, latitude, radius).convertToSpecifiedWorld(po).getShape(); 
	}
	  
    public static FPath getUnprojectedCirclePath(double longitude, double latitude, double radius) {
    	Point2D vert[]=new Point2D[1];
		vert[0]=new Point2D.Double(longitude, latitude);
		FPath path = new FPath(vert, FPath.SPATIAL_EAST, false);
			
		return GeomSource.getCirclePath(path, radius, 36); 
	}
	
	
	/**
	 * Calls getRenderedImage and sets that image on the passed in decal.
	 * Gets the extent, ppd, and projection from the decal and uses it to
	 * create the proper Rendered Image.
	 * @param layer  This stamp layer
	 * @param decal  Decal which the image will be created for and set on
	 * @param stateId  Current state id when this method is called
	 */
	public static void drawImages(StampLayer layer, Decal decal, int stateId) {
		final int requestStateId = stateId;
		
		if (requestStateId < decal.getStateId()) {
			return;
		}
		
		Point2D minExtent = decal.getMinExtent();
		Point2D maxExtent = decal.getMaxExtent();
		
		double width = maxExtent.getX()-minExtent.getX();
		double height = maxExtent.getY()-minExtent.getY();
		
		height = Math.abs(height);
		width = Math.abs(width);

		int ppd = decal.getPPD();
		    			
		Rectangle2D worldRect = new Rectangle2D.Double(minExtent.getX(), maxExtent.getY(), width, height);

		ProjObj po = decal.getProjection();
		
		if (requestStateId < decal.getStateId()) {
			return;
		}
		
		BufferedImage bufferedImage = getRenderedImage(layer, worldRect, po, ppd, decal.getStateId(), true);
		
		if (requestStateId < decal.getStateId()) {
			return;
		}
		
		decal.setImage(bufferedImage);
	}
	
	/**
	 * Draws stamp images in in a buffered image. Images are only redrawn if the
	 * projection, draw only selections, or the in-view stamp list have changed since
	 * the last drawing (or if being drawn for the first time). 
	 */
	/**
	 * @param layer      This stamp layer
	 * @param worldRect  The rectangle defining the bounds of the image to draw
	 * @param po         The projection the image is in
	 * @param ppd        The ppd the image is at
	 * @param stateId    The current state id when this method is called
	 * @param checkId    A flag whether to use the state ids when executing this method
	 * @return A buffered image with the intersecting stamps outlines drawn in it.
	 */
	public static BufferedImage getRenderedImage(StampLayer layer, Rectangle2D worldRect, ProjObj po, int ppd, int stateId, boolean checkId) {
       	final int requestStateId = stateId;
		
		if (checkId &&(requestStateId < layer.getStateId(layer.IMAGES_BUFFER))) {
			return null;
		}
       
       
		BufferedImage renderedImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));
		Graphics2D g2 = renderedImage.createGraphics();
		
		g2.setBackground(new Color(0,0,0,0));
		g2.clearRect(0,0,renderedImage.getWidth(), renderedImage.getHeight());
		// Wrap g2 so if we get tiles outside of 0-360 space, they'll still render
		g2 = new GraphicsWrapped(g2,360,ppd,worldRect,"wrapWorldGraphics");
		
		AffineTransform world2image = new AffineTransform();
		Dimension window = new Dimension((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));

		// Correct for upside-down-ness
		world2image.scale(1, -1);
		world2image.translate(0, -window.getHeight());

		world2image.scale(window.getWidth()  / worldRect.getWidth(),
						  window.getHeight() / worldRect.getHeight());
		world2image.translate(-worldRect.getX(),
							  -worldRect.getY());
       
		g2.transform(world2image);

		// End upfront set-up.
		// TODO: Move this to a common method?
				
		//
		final List<FilledStamp> allFilledStamps;
		
		if (layer.getSettings().renderSelectedOnly()) {
			allFilledStamps = layer.viewToUpdate.myFocus.getRenderedView().getFilledSelections();
		} else {
			allFilledStamps = layer.viewToUpdate.myFocus.getRenderedView().getFilled();
		}
		
		// Filter out any stamps that aren't in the view
//		List<FilledStamp> filtered = new ArrayList<FilledStamp>();
//		for (FilledStamp fs : allFilledStamps) {
//			for (int i=0; i<stamps.length; i++) {
//				if (fs.stamp.getId().equalsIgnoreCase(stamps[i].getId())) {
//					filtered.add(fs);
//				}
//			}
//		}
//		
		//
		
		// TODO: No
		DrawFilledRequest request = layer.viewToUpdate.new StampRequest3D(0, worldRect, ppd);
		
		int repaintCount = 0;
		int totalCount = allFilledStamps.size();
		
		List<StampImage> higherStamps=new ArrayList<StampImage>();
		for (FilledStamp fs : allFilledStamps) {
			fs.pdsi.calculateCurrentClip(higherStamps);
			higherStamps.add(fs.pdsi);
		}
		// Draw in reverse order so the top of the list is drawn on top
		for (int i = totalCount - 1; i >= 0; i--) {
			if (checkId &&(requestStateId < layer.getStateId(layer.IMAGES_BUFFER))) {
				return null;
			}
			FilledStampImageType fs = (FilledStampImageType)allFilledStamps.get(i);
			Graphics2D stampG2 = (Graphics2D) g2.create();
			try {
				Point2D offset = fs.getOffset();
				stampG2.translate(offset.getX(), offset.getY());
				
				fs.pdsi.renderImage(stampG2, fs, request, layer.startTask(), offset, null, po, ppd);
			} finally {
				stampG2.dispose();
			}
		}
		
		return renderedImage;
	}
	
	
	
	/**
	 * 
	 * Calls getSelectionsImage and sets that image on the passed in decal.
	 * Gets the extent, ppd, and projection from the decal and uses it to
	 * create the proper Selections Image.
	 * @param stamplayer  This stamp layer
	 * @param decal  Decal which the image will be created for and set on
	 * @param selectedStamps  A list of the selected stamps
	 * @param stateId  Current state id when this method is called
	 */
	public static void drawSelections(StampLayer stampLayer, Decal decal, final List<StampShape> selectedStamps, int stateId){
		final int requestStateId = stateId;
		
		if (requestStateId < decal.getStateId()) {
			return;
		}
		
		if (stampLayer.globalShapes()) return;
		
		Point2D minExtent = decal.getMinExtent();
		Point2D maxExtent = decal.getMaxExtent();
		
		double width = maxExtent.getX()-minExtent.getX();
		double height = maxExtent.getY()-minExtent.getY();
		
		height = Math.abs(height);
		width = Math.abs(width);

		int ppd = decal.getPPD();
		ProjObj po = decal.getProjection();

		
		Rectangle2D worldRect = new Rectangle2D.Double(minExtent.getX(), maxExtent.getY(), width, height);

		if (requestStateId < decal.getStateId()) {
			return;
		}
        BufferedImage selectionImage = getSelectionsImage(stampLayer, selectedStamps, worldRect, po, ppd, 1, decal.getStateId(), true);

		if (selectionImage==null || requestStateId < decal.getStateId()) {
			return;
		}
        decal.setImage(selectionImage);
	}
	
	/**
	 * Utility method to draw boresights (originally for OREX) without having to redefine the logic 6 places
	 * 
	 * @param g2 - Graphics2D in world space to draw upon
	 * @param po - ProjObj of the projection used by the g2 (necessary when drawing for the 3D body in many projections)
	 * @param spatialPoint - Spatial location of the boresight
	 * @param originalColor - Color used to draw the observation so we can pick an appropriate alternate
	 * @param originalObs - Shape based (GeneralPath, Area) representation of the observation so we can size the boresight appropriately
	 * @param isSelected - boolean to indicate whether or not the boresight should be colorized as selected or not
	 */
	public static void drawBoresight(Graphics2D g2, ProjObj po, Point2D spatialPoint, Color originalColor, List<?> originalObs, boolean isSelected) {
    	if (spatialPoint!=null) {
    		double width=0;
    		double height=0;
    		
    		// Our observation can be broken into multiple pieces due to projection weirdness.  We add up each part to gauage the total size
    		for (Object s : originalObs) {
    			Rectangle2D b = ((Shape)s).getBounds();
    			width += b.getWidth();
    			height += b.getHeight();
    		}
    		
    		// Projection weirdness can make an observation very wide or tall, so base the size on the smaller dimension
    		double size = Math.min(width, height);
    		
    		// Draw the boresight as 10% of the size of the observation
    		size /= 10;
    		
    		if (size<=0) {
    			size=1.0; // We want these to show up somehow
    		}
    		
	    	Point2D bw = po.convSpatialToWorld(spatialPoint);

	    	// If an outline is selected, show the boresight as the same color as the selection, otherwise use a color that contrasts with the footprint
	    	if (isSelected) {
	    		g2.setColor(originalColor);
	    	} else {
	    		g2.setColor(new Color(getContrastVersionForColor(originalColor)));
	    	}
	    	
			Ellipse2D oval = new Ellipse2D.Double(bw.getX()-(size/2.0), bw.getY()-(size/2.0), size, size);
			g2.fill(oval);
    	}
	}
	
	
	/**
     * Draws outlines for stamp selections.  This reside in their own
     * buffer layer.
     * <p>
     * If a redraw is requested, but no stamps are specified, then any
     * existing selected stamp outlines are cleared.
     * 
     * @param ss        List of stamps to be drawn (partial or all);
     * may be <code>null</code> (useful for clearing all selections
     * in combination with <code>redraw</code> parameter.
     */
	/**
	 * @param stampLayer  This stamp layer
	 * @param selectedStamps List of stamps to be drawn (partial or all);
     * may be <code>null</code> (useful for clearing all selections
     * in combination with <code>redraw</code> parameter.
     * @param worldRect  The rectangle defining the bounds of the image to draw
	 * @param po         The projection the image is in
	 * @param ppd        The ppd the image is at
	 * @param scale		 The value to scale the lines (can be 1)
	 * @param stateId    The current state id when this method is called
	 * @param checkId    A flag whether to use the state ids when executing this method
	 * @return A buffered image with the intersecting stamps outlines drawn in it.
	 */
	public static BufferedImage getSelectionsImage(StampLayer stampLayer, final List<StampShape> selectedStamps, Rectangle2D worldRect, ProjObj po, int ppd, int scale, int stateId, boolean checkId){
        final int requestStateId = stateId;
        
		BufferedImage selectionImage = Util.newBufferedImage((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));
		Graphics2D g2 = selectionImage.createGraphics();
		    			
		g2.setBackground(new Color(0,0,0,0));
		g2.clearRect(0,0,selectionImage.getWidth(), selectionImage.getHeight());
		
		// Wrap g2 so if we get tiles outside of 0-360 space, they'll still render
		g2 = new GraphicsWrapped(g2,360,ppd,worldRect,"wrapWorldGraphics");
		
		AffineTransform world2image = new AffineTransform();

		Dimension window = new Dimension((int)(worldRect.getWidth()*ppd), (int)(worldRect.getHeight()*ppd));

		// Correct for upside-down-ness
		world2image.scale(1, -1);
		world2image.translate(0, -window.getHeight());

		world2image.scale(window.getWidth()  / worldRect.getWidth(),
						  window.getHeight() / worldRect.getHeight());
		world2image.translate(-worldRect.getX(),
							  -worldRect.getY());
       
		g2.transform(world2image);
		    									        
		// This will be overriden on a stamp by stamp basis for spectra stamps
		Color inverseColor = new Color(~stampLayer.getSettings().getUnselectedStampColor().getRGB());
		
        g2.setComposite(AlphaComposite.Src);
        g2.setStroke(new BasicStroke((float)(1.0/ppd)*scale));
		g2.setColor(inverseColor);

        for (StampShape selectedStamp : selectedStamps) {
        	if (selectedStamp.isHidden) continue;

        	// For spectral layers, colorize the spots the same as their plot colors
        	if (stampLayer.spectraData()) {
        		Color calcColor = selectedStamp.getCalculatedColor();
        		if (calcColor!=null) {
					inverseColor = new Color(StampUtil.getContrastVersionForColor(calcColor));
					
			        g2.setComposite(AlphaComposite.Src);
			        //TODO: don't do this in jmars
//			        g2.setStroke(new BasicStroke(1));
        		}
        		
   		       	SpectraView spectraView = ((StampFocusPanel)(stampLayer.viewToUpdate.getFocusPanel())).spectraView;

        		Color spectraColor = spectraView.getColorForStamp(selectedStamp);
				     
	        	if (spectraColor!=null) {
	        		inverseColor = spectraColor;
	        	}

				g2.setColor(inverseColor);			        		
	    	}
        	
			if (checkId &&(requestStateId < stampLayer.getStateId(stampLayer.SELECTIONS_BUFFER))) {
				return null;
			}
			
			List<GeneralPath> stampPaths = selectedStamp.getPath(po);
			
			for (GeneralPath path : stampPaths) {
				g2.draw(path);
			}
			
			if(stampLayer.lineShapes()){				    	
        		//this could still be a radar type layer, and if so,
		    	// draw other data (horizons, full res highlight)
		    	StampLView lview = stampLayer.viewToUpdate;
	    		FullResHighlightListener fshl = lview.getFullResHighlightListener();
	    		if(fshl!=null){
	    			fshl.paintHighlight(g2, po);
	    		}
	    		
	    		//draw any horizons on the sharad footprints if there are any
	    		StampFocusPanel focusPanel = (StampFocusPanel)lview.getFocusPanel();
				RadarFocusPanel radarPnl = focusPanel.getRadarView();
				
				for(FilledStamp fs : focusPanel.getRenderedView().getFilled()){
					//TODO there might be a better way than this loop to recast?
					ArrayList<FilledStampRadarType> list = new ArrayList<FilledStampRadarType>();
					if(fs instanceof FilledStampRadarType){
						list.add((FilledStampRadarType)fs);
					}
					for(FilledStampRadarType fsr : list){
						for(RadarHorizon h : fsr.getHorizons()){
							//if the horizon isn't hidden, and if its color is being displayed in 2d 
							// (as determined in the settings focus panel tab and stored in the layer settings)
							if(h.isVisible() && lview.getSettings().getHorizonColorDisplayMap().get(h.getColor())){
								//if this horizon is selected, color it differently
								if(h.equals(radarPnl.getSelectedHorizon())){
									g2.setColor(new Color(~h.getColor().getRGB()));
								}else{
									g2.setColor(h.getColor());
								}
								g2.setStroke(lview.getProj().getWorldStroke(h.getLViewWidth()));
								g2.draw(h.getWorldPathForProj(po));
							}
						}
					}
				}
	    	}
			
	    	Point2D bp = selectedStamp.getBoresight();
	    	if (stampLayer.getSettings().showBoresight()) {
	    		drawBoresight(g2, po, bp, inverseColor, stampPaths, true);
	    	}
        }
        
   		if (checkId &&(requestStateId < stampLayer.getStateId(stampLayer.SELECTIONS_BUFFER))) {
			return null;
		}

        return selectionImage;
	}

	
	private static List<StampShape> findStampsByWorldRect(List<StampShape> stamps, Rectangle2D proximity, ProjObj po)
	{
		if (stamps == null || proximity == null)
			return null;

		List<StampShape> list = new ArrayList<StampShape>();
		double w = proximity.getWidth();
		double h = proximity.getHeight();
		double x = proximity.getX();
		double y = proximity.getY();

		x -= Math.floor(x/360.0) * 360.0;

		Rectangle2D proximity1 = new Rectangle2D.Double(x, y, w, h);
		Rectangle2D proximity2 = null;

		// Handle the two cases involving x-coordinate going past
		// 360 degrees:
		// Proximity rectangle extends past 360...
		if (proximity1.getMaxX() >= 360) {
			proximity2 = new Rectangle2D.Double(x-360, y, w, h);
		}
		// Normalized stamp extends past 360 but
		// proximity rectangle does not...
		else if (proximity1.getMaxX() <= 180) {
			proximity2 = new Rectangle2D.Double(x+360, y, w, h);
		}

		// Perform multiple proximity tests at the same time
		// to avoid re-sorting resulting stamp list.
		for (StampShape ss : stamps) {		
			if (ss.isHidden()) continue;
			List<Area> stampAreas = ss.getFillAreas(po);
			pathloop: for (Area area : stampAreas) {
				Shape shape = area;
				Rectangle2D stampBounds = shape.getBounds2D();
				
				// getBounds2D for a line can return a 0 height for a horizontal line or a 0 width for a veritical line.  Either case will result in
				// Rectangle2D.intersect returning false no matter what.  To get around this, we make sure the height and width are each at least 1.
				if (stampBounds.getHeight()==0) stampBounds.setRect(stampBounds.getX(), stampBounds.getY(), stampBounds.getWidth(), 1);
				if (stampBounds.getWidth()==0) stampBounds.setRect(stampBounds.getX(), stampBounds.getY(), 1, stampBounds.getHeight());
				
				// Do a fast compare with the Rectangle bounds, then do a second
				// more accurate compare if the areas overlap.
				if (stampBounds.intersects(proximity1) || (proximity2 != null && stampBounds.intersects(proximity2)))
				{
					if (shape.intersects(proximity1) ||	(proximity2 != null && shape.intersects(proximity2))) {
						list.add(ss);			
						break pathloop;  // Once any of the stamp's areas intersect, we don't need to check the others.
					}
				}
			}
		}

		return list;
	}
	
	/**
	 * Takes a list of stamp shapes and a point in world coords and 
	 * returns a list of the stamps which contain the point, given 
	 * the passed in projection
	 * (Copied from the private findStampsByWorldRect, but is simpler
	 * logic because we don't have to worry about the rectangle 
	 * wrapping around the coordinate grid)
	 * @param stamps  List of stamps to compare against
	 * @param proximity  World point of interest
	 * @param po  Associated ProjObj
	 * @return  List of stamps which contain the given point
	 */
	public static List<StampShape> findStampsByWorldPt(List<StampShape> stamps, Point2D proximity, ProjObj po){
		if (stamps == null || proximity == null){
			return null;
		}

		List<StampShape> list = new ArrayList<StampShape>();

		// Perform multiple proximity tests at the same time
		// to avoid re-sorting resulting stamp list.
		for (StampShape ss : stamps) {		
			if (ss.isHidden()) continue;
			List<Area> stampAreas = ss.getFillAreas(po);
			pathloop: for (Area area : stampAreas) {
				Shape shape = area;
				Rectangle2D stampBounds = shape.getBounds2D();
				
				// getBounds2D for a line can return a 0 height for a horizontal line or a 0 width for a veritical line.  Either case will result in
				// Rectangle2D.intersect returning false no matter what.  To get around this, we make sure the height and width are each at least 1.
				if (stampBounds.getHeight()==0) stampBounds.setRect(stampBounds.getX(), stampBounds.getY(), stampBounds.getWidth(), 1);
				if (stampBounds.getWidth()==0) stampBounds.setRect(stampBounds.getX(), stampBounds.getY(), 1, stampBounds.getHeight());
				
				// Do a fast compare with the Rectangle bounds, then do a second
				// more accurate compare if the areas overlap.
				if (stampBounds.contains(proximity) && shape.contains(proximity)) {
					list.add(ss);			
					break pathloop;  // Once any of the stamp's areas intersect, we don't need to check the others.
				}
			}
		}

		Collections.reverse(list);
		
		return list;
	}

	/**
	 * This should probably only be called from the StampShape.getFillAreas method.  Anywhere else that needs the fillAreas should call
	 * there, so it can be cached.
	 * 
	 * This is separated out so it can be modified to support generic polygons, not necessarily stamp specific ones
	 * @param po
	 * @param stampShape
	 * @return
	 */
    public synchronized static List<Area> getFillAreas(ProjObj po, StampShape stampShape) {
        ArrayList<Area> po_fillAreas = new ArrayList<Area>();
    	
	        double spatialPts[] = stampShape.myStamp.getPoints();
	        
	        if (spatialPts==null || spatialPts.length==0) return po_fillAreas;

//	    	for (int i=0; i<spatialPts.length-2; i+=2) {
//	    		System.out.println("apt"+i+" = " + (360-spatialPts[i]) + " : " + spatialPts[i+1]);
//	    	}
	    	
	    	spatialPts = StampUtil.cleanedSpatialPoints(spatialPts);

//	    	for (int i=0; i<spatialPts.length-2; i+=2) {
//	    		System.out.println("bpt"+i+" = " + (360-spatialPts[i]) + " : " + spatialPts[i+1]);
//	    	}

	    	Analysis stats = stampShape.analyzePoints(po);
	    	
	        boolean moveNext=true;
	        boolean closePath=true;
	        
	        GeneralPath fillPath = new GeneralPath();
	        
	        double firstX = Double.NaN;
	        double lastX = Double.NaN;
	        double lastY = Double.NaN;
	        
	        double firstY = Double.NaN;
	        
	        // TODO: Don't always do this
	        // If the polygon is 360 degrees wide IN WORLD COORDINATES then we need to treat it differently.
	        // Note depending on the projection, this could potentially happen to ANY polygon, regardless of whether its extent was 360 degrees in
	        // the original spatial coordinates or not.
	        if (stats.is360wide()) {
	        	fillPath = get360WorldPath(po, spatialPts, stats, stampShape);
	        } else {
		        Point2D pt = new Point2D.Double();
                
		        for (int i=0; i<spatialPts.length; i=i+2) {	        	
		        	if (Double.isNaN(spatialPts[i]) || Double.isNaN(spatialPts[i+1])) {
		        		moveNext=true;
		        		closePath=false;
		        		lastX = Double.NaN;
		        		continue;
		        	} else {
		        		pt = po.convSpatialToWorld(spatialPts[i], spatialPts[i+1]);
		        	}
		
		        	double x = pt.getX();
		        	double y = pt.getY();
		        	
		        	x -= Math.floor(x/360)*360;
		        	
		            if (moveNext) {
		            	fillPath.moveTo(x, y);
		            	lastX = x;
		            	firstX = x;
		            	firstY = y;
		            	moveNext=false;
		            } else {	
	            		if (x > lastX+180) {
		            		x-=360;
		            	}
		            	
		            	if (x < lastX-180) {
		            		x+=360;
		            	}
		            		
	           			fillPath.lineTo(x, y);
	           			lastX = x;
		            }	            
		        }
		        		        
		        if (closePath && spatialPts.length>0) {
		        	// TODO: How do we know when to do this?
		        	// TODO: How do we know to draw up or down?
		        	
		        	// I don't think we ever do this for non-360 degree WORLD coordinate polygons
//		        	if (stats.meridianCrosses%2!=0) {
//		        		if (stats.rightCrosses < stats.leftCrosses) {
//					        fillPath.lineTo(lastX, 90);
//					        fillPath.lineTo(firstX, 90);
//					        fillPath.lineTo(firstX, firstY);
//		        		} else {
//					        fillPath.lineTo(lastX, -90);
//					        fillPath.lineTo(firstX, -90);
//					        fillPath.lineTo(firstX, firstY);	        			
//		        		}
//		        	}

		        	// TODO: Might actually care about firstX, firstY here if we have multiple polygons in this shape
					fillPath.closePath();
				}
	        }
	        
	        Rectangle2D bounds = fillPath.getBounds2D();
	        double minxb = bounds.getMinX();

	        // Shapes are at most 360 degrees wide, so no matter where we start, dividing into 3 120 degree sections gets everything

	        GeneralPath a1 = new GeneralPath();
	        
	        a1.moveTo(minxb, -90.0);
	        a1.lineTo(minxb, 90.0);
	        a1.lineTo(minxb+120, 90.0);
	        a1.lineTo(minxb+120, -90.0);
	        a1.closePath();

	        GeneralPath a2 = new GeneralPath();
	        
	        a2.moveTo(minxb+120, -90.0);
	        a2.lineTo(minxb+120, 90.0);
	        a2.lineTo(minxb+240, 90.0);
	        a2.lineTo(minxb+240, -90.0);
	        a2.closePath();

	        GeneralPath a3 = new GeneralPath();
	        
	        a3.moveTo(minxb+240, -90.0);
	        a3.lineTo(minxb+240, 90.0);
	        a3.lineTo(minxb+360, 90.0);
	        a3.lineTo(minxb+360, -90.0);
	        a3.closePath();

	        Area fill0 = new Area(fillPath);

	        Area fill1 = (Area) fill0.clone();

	    	Area a1Area = new Area(a1);
	    	
	        po_fillAreas.addAll(clippedArea(fill1, a1Area));
	        
	        Area fill2 = (Area) fill0.clone();
	        
	        a1Area.transform(shift120);

	        po_fillAreas.addAll(clippedArea(fill2, a1Area));
	        
	        Area fill3 = (Area) fill0.clone();
	        
	        a1Area.transform(shift120);

	        po_fillAreas.addAll(clippedArea(fill3, a1Area));
	        
    	return po_fillAreas;
    }

    /**
     * In order for spatial databases to understand the area covered by a polygon, anything that is 360 degrees wide has to have points added to it
     * to include the north or south poles.  This adds lines that go north or south along the meridan that shouldn't be displayed when we draw
     * the outline into JMARS.  This routine attempts to remove those points.
     * 
     * @param spatialPts
     * @return
     */
    private static double[] cleanedWorldPoints(double spatialPts[], Analysis stats, ProjObj po) {
    	if (spatialPts==null || stats==null || po==null) {
    		return new double[0];
    	}
    	
//    	for (int i = 0 ; i<spatialPts.length; i+=2) {
//    		System.out.println("spatial point["+i+"] = " + spatialPts[i] + " : " + spatialPts[i+1]);
//    	}

    	
    	double worldPoints[] = new double[spatialPts.length];
    	
    	double lastSpatialX=Double.NaN;
    	
    	int skipCnt=0;
    	
    	for (int i=0; i<spatialPts.length; i=i+2) {
    		double spatialX = spatialPts[i];
    		
    		if (stats.is360wideSpatial() && (
    				(lastSpatialX==0 && spatialX==0) ||
    				(lastSpatialX==360 && spatialX==360) ||
    				(lastSpatialX==0 && spatialX==360) ||
    				(lastSpatialX==360 && spatialX==0)	        				
    				)) {
    			
    				skipCnt+=2;
    			
    			// TODO: TODO: TODO
    			// Somehow REMOVE the points that connect the prime meridian to the north pole.
    			// Probably these points should be removed ONCE from the original points, rather than on the fly while parsing the shapes or the paths
    			
    		} else {
    			Point2D worldPoint =  po.convSpatialToWorld(spatialPts[i], spatialPts[i+1]);
    			worldPoints[i-skipCnt]=worldPoint.getX();
    			worldPoints[i+1-skipCnt]=worldPoint.getY();
    		}
    		
    		lastSpatialX=spatialX;
    	}
    	
        double tmpPts[] = new double[spatialPts.length - skipCnt];
        
        for (int i=0; i<tmpPts.length; i++) {
        	tmpPts[i]=worldPoints[i];
        }	
        
        return worldPoints;
    }

    /**
     * In order for spatial databases to understand the area covered by a polygon, anything that is 360 degrees wide has to have points added to it
     * to include the north or south poles.  This adds lines that go north or south along the meridan that shouldn't be displayed when we draw
     * the outline into JMARS.  This routine attempts to remove those points.
     * 
     * @param spatialPts
     * @return cleaned spaitialPts
     */
    public static double[] cleanedSpatialPoints(double spatialPts[]) {
    	if (spatialPts==null || spatialPts.length==0) {
    		return new double[0];
    	}
    	
    	double spatialMinX = Double.NaN;
    	double spatialMaxX = Double.NaN;
    	for (int i = 0 ; i<spatialPts.length; i+=2) {
		    if (Double.isNaN(spatialMinX) || spatialMinX > spatialPts[i]) {
            	spatialMinX = spatialPts[i];
            }
            
            if (Double.isNaN(spatialMaxX) || spatialMaxX < spatialPts[i]) {
            	spatialMaxX = spatialPts[i];
            }
    	}

    	boolean is360WideSpatial = (spatialMaxX - spatialMinX >0360);
    	
    	int minXindex = -1;
    	double minX=Double.MAX_VALUE;
    	
    	for (int i=0; i<spatialPts.length; i=i+2) {
    		if (spatialPts[i+1]==90 || spatialPts[i+1]==-90) {
    			continue;
    		}
    		
    		if (spatialPts[i]<minX) {
    			minX=spatialPts[i];
    			minXindex=i;
    		}
    	}
    	
    	double tmpSpatialPts[] = new double[spatialPts.length];

    	for (int i=minXindex; i<spatialPts.length; i++) {
    		tmpSpatialPts[i-minXindex]=spatialPts[i];
    	}

    	for (int i=0; i<minXindex; i++) {
    		tmpSpatialPts[i+spatialPts.length-minXindex]=spatialPts[i];
    	}

    	spatialPts = tmpSpatialPts;

    	double cleanSpatialPts[] = new double[spatialPts.length];
    	
    	double lastSpatialX=Double.NaN;
    	
    	int skipCnt=0;
    	
    	for (int i=0; i<spatialPts.length; i=i+2) {
    		double spatialX = spatialPts[i];
    		
    		if (is360WideSpatial && (
    				(lastSpatialX==0 && spatialX==0) ||
    				(lastSpatialX==360 && spatialX==360) ||
    				(lastSpatialX==0 && spatialX==360) ||
    				(lastSpatialX==360 && spatialX==0)	        				
    				)) {
    			
    				skipCnt+=2;
    			
    			// TODO: TODO: TODO
    			// Somehow REMOVE the points that connect the prime meridian to the north pole.
    			// Probably these points should be removed ONCE from the original points, rather than on the fly while parsing the shapes or the paths
    			
    		} else {
    			cleanSpatialPts[i-skipCnt]=spatialPts[i];
    			cleanSpatialPts[i+1-skipCnt]=spatialPts[i+1];
    		}
    		
    		lastSpatialX=spatialX;
    	}
    	
        double tmpPts[] = new double[spatialPts.length - skipCnt];
        
        for (int i=0; i<tmpPts.length; i++) {
        	tmpPts[i]=cleanSpatialPts[i];
        }	
        
        return tmpPts;
    }

    
    /**
     * Given a set of world points that are 360 degrees wide, iterate through them until we find one point that can be used as a left-most point so that all points fit within the 0-360 space
     * without wrapping across a world meridian
     * 
     * TODO: Add examples
     * 
     * @param worldPoints
     * @return
     */
    private static double[] getAlignedWorldPoints(double worldPoints[], StampShape shape) {
    	double prevX = Double.NaN;
    	double minX = Double.NaN;
    	double maxX = Double.NaN;	        		        	
    	
    	double wpts2[] = new double[worldPoints.length+2];

    	int loopCnt = 0;
    	
    	Point2D.Double origPt = new Point2D.Double();
    	
//    	for (int i = 0 ; i<worldPoints.length; i+=2) {
//    		System.out.println("point["+i+"] = " + worldPoints[i] + " : " + worldPoints[i+1]);
//    	}
    	
    	
    	boolean foundSolution = false;
    	
ptloop:        	for (loopCnt=0; loopCnt<wpts2.length; loopCnt=loopCnt+2) {

        	wpts2[wpts2.length-2]=wpts2[0];
        	wpts2[wpts2.length-1]=wpts2[1];

        	prevX = Double.NaN;
        	minX = Double.NaN;
        	maxX = Double.NaN;

        	for (int ii=0; ii<wpts2.length; ii=ii+2) {
        		int i = (ii+loopCnt)%worldPoints.length;
        		
        			if (i==wpts2.length-2) {
        				origPt.setLocation(worldPoints[loopCnt], worldPoints[loopCnt+1]);
        			} else {
        				origPt.setLocation(worldPoints[i], worldPoints[i+1]);
        			}
        			        			
        			double x = origPt.getX();
        			double y = origPt.getY();
        			
        			if (!Double.isNaN(prevX)) {
        				if (!(Math.abs(x-prevX)<180)) {
        					if (x<prevX) {
        						while(!(Math.abs(x-prevX)<=180)) {
        							x+=360;
        						}
        					} else {
        						while(!(Math.abs(x-prevX)<=180)) {
        							x-=360;
        						}	        						
        					}
        				}
        			}
	        			
        			wpts2[ii] = x;
        			wpts2[ii+1] = y;
        			prevX = x;
        			
        			if (Double.isNaN(minX) || minX > x) {
        				minX = x;
        			}
        			if (Double.isNaN(maxX) || maxX < x) {
        				maxX = x;
        			}
        			
        			if (Math.abs(minX-maxX)>360) {
        				continue ptloop; // Retry ...
        			}
        	}
        	
        	// If we get here, we've made is successfully through all of the points!
        	foundSolution=true;
        	break;
    	
    	}    	
    	
    	// TODO: After testing, remove this and remove the need for passing in a stamp shape
    	if (!foundSolution) {
    		System.out.println("No solution found for: " + shape.getId() );
    	}
    	
    	return wpts2;
    }
        
	/**
	 * If we're 360 degrees wide:
	 * 
	 * Iterate through the points and adjust such that no two points are more than 180 degrees apart from each other and add points for world meridian crossings.
	 * The end result should be the total width of all points is exactly 360 degrees.
	 * 
	 * Determine the point at minX, maxY (for north connecting) and maxX, maxY or minX, minY and maxX, minY (for south connecting) and add points to connect them.
	 * 
	 * Build a path out of the new set of points.  Convert into an Area.
	 * 
	 * Chop the resulting area into 120 degree chunks of data.
	 * 
	 * Shift the chunks into 0-360 space.
	 * 
	 * Profit.
	 * 
	 */
    private static GeneralPath get360WorldPath(ProjObj po, double spatialPts[], Analysis stats, StampShape shape) {
    	// Convert the spatial points to world points and remove extraneous points in the process
    	double worldPoints[] = cleanedWorldPoints(spatialPts, stats, po);
    	    	
		// Get the same world points, but with the set starting with a left-most point so that all of the rest of the points can be connected to without crossing a world-meridian
    	double alignedWorldPoints[] = getAlignedWorldPoints(worldPoints, shape);

    	// TODO: Ended up with a width of 363.  What does that mean?  Is that bad..?  Is that ok?  Why or why not?

    	// TODO: What if there are more than one path in this shape?  We should process each of them separately
    	
    	GeneralPath worldPath = new GeneralPath();
    	
    	worldPath.moveTo(alignedWorldPoints[0], alignedWorldPoints[1]);
    	
    	for (int i=2; i<alignedWorldPoints.length; i=i+2) {
    		worldPath.lineTo(alignedWorldPoints[i], alignedWorldPoints[i+1]);
    	}
    	
    	// Get a spatial point representing the location of the 'north pole' in this world projection
    	Point2D spatialNorth = po.convWorldToSpatial(180, 90);

    	// Test whether the spatial point equivalent of the 'world north' point is inside the spatial polygon.  If so, we include the world north,
    	// if not, we include the world south
		boolean crossesMeridian = (stats.spatialMeridianCrosses)>0 ? true : false;
		
		
//    	boolean fillUp = isPointInsidePolygon(spatialPts, spatialNorth, crossesMeridian);
		
		boolean fillUp = false;
		
		// Spatial representation of the world North Pole
		HVector northPole = new HVector(spatialNorth);
		
		// Currently, our database lies to us about whether the north or south pole is in view.  Since we can't trust the results,
		// this approach, suggested by Saadat, seems to work.
		// TODO: Perhaps change this back once the database values are fixed
		// This code requires the polgyons to be convex.  If polygons are not (such as when we start to get limb shots off of a weird shape model?), we may need
		// another solution.
		if (true) {
			double originalSpatialPts[] = spatialPts;
						
			boolean allNegative = true;
			
			for (int i = 0; i<originalSpatialPts.length-4; i+=2) {
				HVector p1 = new HVector(originalSpatialPts[i+0], originalSpatialPts[i+1]);
				HVector p2 = new HVector(originalSpatialPts[i+2], originalSpatialPts[i+3]);

				HVector cross = p1.cross(p2);
			
				double dot = northPole.dot(cross);
			
//				System.out.println("adot = " + dot + "    " + p1.lon() + " : " + p1.lat() + "   " + p2.lon() + " : " + p2.lat());
				if (dot>0) {
					allNegative = false;
					break;
				}
			}
						
			if (allNegative) {
				fillUp=true;
			} else {
				fillUp=false;;
			}
		} else {
			if (stats.spatialInteriorPoint!=null) {
	//			System.out.println("We can test this in world coordinates!");
	
				double tempWorldPoints[] = new double[alignedWorldPoints.length+6];
				
				for (int i=0; i<tempWorldPoints.length-6; i++) {
					tempWorldPoints[i]=alignedWorldPoints[i];
				}
				
				int x = tempWorldPoints.length-6;
				
				tempWorldPoints[x] = alignedWorldPoints[alignedWorldPoints.length-2];
				tempWorldPoints[x+1] = 90;
				tempWorldPoints[x+2] = alignedWorldPoints[0];
				tempWorldPoints[x+3] = 90;
				tempWorldPoints[x+4] = alignedWorldPoints[0];
				tempWorldPoints[x+5] = alignedWorldPoints[1];
	
	//			for (int i=0; i<tempWorldPoints.length; i++) {
	//				System.out.println("tempWorldPoints = " + tempWorldPoints[i]);
	//			}
				
				Point2D worldInteriorPoint = po.convSpatialToWorld(stats.spatialInteriorPoint);
				
				if (worldInteriorPoint.getY()>=89.999) {
					fillUp=true;
				} else {
					fillUp = isPointInsidePolygon(tempWorldPoints, worldInteriorPoint, false);								
				}
			} else {
				Point2D eastLeadingSpatialNorth = new Point2D.Double(360-spatialNorth.getX(), spatialNorth.getY());
		    	fillUp = isPointInsidePolygon(spatialPts, eastLeadingSpatialNorth, crossesMeridian);			
			}
		}
		
		if (fillUp) {
	        worldPath.lineTo(alignedWorldPoints[alignedWorldPoints.length-2], 90);
	        worldPath.lineTo(alignedWorldPoints[0], 90);
	        worldPath.lineTo(alignedWorldPoints[0], alignedWorldPoints[1]);
		} else {
    		worldPath.lineTo(alignedWorldPoints[alignedWorldPoints.length-2], -90);
    		worldPath.lineTo(alignedWorldPoints[0], -90);
    		worldPath.lineTo(alignedWorldPoints[0], alignedWorldPoints[1]);	        			
		}
    	
    	return worldPath;    	
    }
    
    /**
     * Returns the Area(s) from fill that intersect a1Area, applying logic for wrapped world coordinates in the process
     * 
     * @param fill
     * @param a1Area
     * @return
     */
    public static List<Area> clippedArea(Area fill, Area a1Area) {
    	ArrayList<Area> returnAreas = new ArrayList<Area>();
    	
        fill.intersect(a1Area);
        
        if (!fill.isEmpty()) {
        	Rectangle2D bounds = fill.getBounds2D();
        	
        	double minX = bounds.getMinX();
        	double maxX = bounds.getMaxX();

        	while (minX>360) {
        		fill.transform(leftShift360);

        		minX-=360;
        		maxX-=360;
        	}

	    	while (minX<0) {
        		fill.transform(shift360);
        		
        		minX+=360;
        		maxX+=360;
        	}

        	if (minX<360 && maxX>360) {
        		Area a360 = new Area(fill);

        		a360.intersect(area360);
        		
        		returnAreas.add(a360);
        		fill.subtract(area360);
        		
        		fill.transform(leftShift360);
        	}      	
        }
        returnAreas.add(fill);
        
    	return returnAreas;
    }
    
    public static Analysis analyzePoints(double pts[], ProjObj po) {
		Analysis analysis = new Analysis();
	
		Point2D spatialPt = new Point2D.Double();
    	Point2D pt=null;
    	
    	double mulToDegrees = 180.0/Math.PI;
    	
    	double lastX=Double.NaN;
    	double lastY=Double.NaN;
    	
    	double lastSpatialX=Double.NaN;
    	
    	Projection_OC poc = (Projection_OC) po;
        		///
		HVector up = poc.getUp();
		HVector center = poc.getCenter();
    	
		// An offset to add to all world coord x-values after we cross a meridian
		double xShift = 0;
		
        for (int i=0; i<=pts.length; i=i+2) {	  
        	int idx = i;
        	// For the final iteration, use the first point once more
        	if (idx>=pts.length) {
        		idx = 0;
        	}
        	if (Double.isNaN(pts[idx]) || Double.isNaN(pts[idx+1])) {
        		// TODO: Arguably, separate polygons could be handled as separate analysis, paths, areas, etc.
        		// TODO: Unless they are actually holes, which is another ball of wax
        		analysis.numShapes++;
        		lastX=Double.NaN;
        		lastY=Double.NaN;
        		continue;
        	} else {
        		double spatialX = pts[idx];
        		double spatialY = pts[idx+1];

            	if (Double.isNaN(analysis.spatialMinX) || analysis.spatialMinX > spatialX) {
                	analysis.spatialMinX = spatialX;
                }
                
                if (Double.isNaN(analysis.spatialMaxX) || analysis.spatialMaxX < spatialX) {
                	analysis.spatialMaxX = spatialX;
                }
        		
            	if (spatialX > lastSpatialX + 180) {
            		// Cross prime meridian
            		analysis.spatialMeridianCrosses++;
//            		analysis.leftCrosses++;
            	}
            	
            	if (spatialX < lastSpatialX - 180) {
            		// Cross prime meridian
            		analysis.spatialMeridianCrosses++;
//            		analysis.rightCrosses++;
            	}
            	
            	// TODO: This po.convSpatialToWorld call is apparently responsible for most of our setup time...
            	spatialPt.setLocation(spatialX, spatialY);
//        		pt = po.convSpatialToWorld(spatialPt);
//        		Projection_OC poc = (Projection_OC) po;
        		///
				HVector ptv = new HVector(spatialPt);
//				HVector up = poc.getUp();
//				HVector center = poc.getCenter();
	
				
				HVector noZ = ptv.sub( up.mul(up.dot(ptv)) );

				double xx = ProjObj.lon_of(new HVector(noZ.dot(center),
											  noZ.dot(center.cross(up)),
											  0));

	//			double y = Math.asin(up.dot(pt)); <-- numerically unstable, NANs!!
//				double yy = Math.PI/2 - up.unitSeparation(ptv);
	
        		double yy = 0;
				// START: unitSeparation
				double dp = up.dot(ptv);
				if(dp > 0)
				 {
					HVector temp = up.sub(ptv);
					double dxp = temp.norm();
					yy = 2 * Math.asin(dxp / 2);
				 }
				else if(dp < 0)
				 {
					HVector temp = up.add(ptv);
					double dxp = temp.norm();
					yy = Math.PI - 2 * Math.asin(dxp / 2);
				 }
				else {
					yy = Math.PI / 2;
				}
				// END: unitSeparation
				
        		
        		// TODO: End unrolling of convSpatialToWorld code
				
        		///
        		
	        	double x = xx *mulToDegrees;
	        	if (x<0 || x>360) {
	        		x = x % 360;
	        	}
	        	double y = yy *mulToDegrees;
                
	        	// Apply any world shifting that we've had to do previously, but don't adjust the first or final points, so we 
	        	// are sure to detect meridian crossings properly
	        	if (idx>0) {
	        		x += xShift;
	        	}
	        	
                if(!Double.isNaN(lastX)) {
                	if (x > lastX + 180) {
                		// Cross prime meridian
                		analysis.meridianCrosses++;
                		analysis.leftCrosses++;
                		xShift-=360;
                		x-=360;
                	}
                	
                	if (x < lastX - 180) {
                		// Cross prime meridian
                		analysis.meridianCrosses++;
                		analysis.rightCrosses++;
                		xShift+=360;
                		x+=360;
                	}
                	
                }
                
                lastX=x;
                lastY=y;
                
                lastSpatialX=spatialX;
        	}
        }
        
    	return analysis;
    }
    
    /**
     * Algorithm taken from http://alienryderflex.com/polygon/
     * 
     * //  The function will return YES if the point x,y is inside the polygon, or
	 * //  NO if it is not.  If the point is exactly on the edge of the polygon,
	 * //  then the function may return YES or NO.
     * 
     * @return
     */
    public static boolean isPointInsidePolygon(double polygonPoints[], Point2D pointToTest, boolean crossesMeridian) {
    	// TODO: Does not handle meridian crosses
    	
    	if (crossesMeridian) {
    		double newPoints[] = new double[polygonPoints.length];
    		
    		for (int i=0; i<newPoints.length; i+=2) {
    			newPoints[i]=polygonPoints[i];
    			newPoints[i+1]=polygonPoints[i+1];
    			if (newPoints[i]<180) {
    				newPoints[i]+=360;
    			}
    		}
    		
    		if (pointToTest.getX()<180) {
    			pointToTest.setLocation(pointToTest.getX()+360,pointToTest.getY());
    		}
    			
    		polygonPoints = newPoints;
    		
    	}
    	
    	
    	// Original C Code
		//    	bool pointInPolygon() {
		//
		//    		  int   i, j=polyCorners-1 ;
		//    		  bool  oddNodes=NO      ;
		//
		//    		  for (i=0; i<polyCorners; i++) {
		//    		    if ((polyY[i]< y && polyY[j]>=y
		//    		    ||   polyY[j]< y && polyY[i]>=y)
		//    		    &&  (polyX[i]<=x || polyX[j]<=x)) {
		//    		      if (polyX[i]+(y-polyY[i])/(polyY[j]-polyY[i])*(polyX[j]-polyX[i])<x) {
		//    		        oddNodes=!oddNodes; }}
		//    		    j=i; }
		//
		//    		  return oddNodes; }
    	    	
    	int numPoints = polygonPoints.length;
    	
    	double x = pointToTest.getX();
    	double y = pointToTest.getY();
    	    	
    	int j=numPoints-2 ;
    	boolean  oddNodes=false;

    	for (int i=0; i<polygonPoints.length; i+=2) {
    		if (polygonPoints[i+1]<y && polygonPoints[j+1]>=y ||  polygonPoints[j+1]<y && polygonPoints[i+1]>=y) {
    			if (polygonPoints[i]+(y-polygonPoints[i+1])/(polygonPoints[j+1]-polygonPoints[i+1])*(polygonPoints[j]-polygonPoints[i])<x) {
    				oddNodes=!oddNodes; 
    			}
    		}
    
    		j=i; 
    	}

    	return oddNodes; 
    }
    
    public static int shiftBand(int oldBand) {
    	int newBand = oldBand + 128;
    	if (newBand>255) {
    		newBand-=255;
    	}
    	return newBand;
    }
    
    
    public static int getContrastVersionForColor(Color color) {
//    	if (true) return Color.black.getRGB();
    	if (color==null) return 0;
	    float[] hsv = new float[3];
	    
	    int newRed = shiftBand(color.getRed());
	    int newBlue = shiftBand(color.getBlue());
	    int newGreen = shiftBand(color.getGreen());
//	    
//	    Color.RGBtoHSB(newRed, newBlue, newGreen, hsv);
//	    
//	    hsv[0] = (hsv[0] + 180) % 360;
//	    if (hsv[2] < 0.5) {
//	        hsv[2] = 0.7f;
//	    } else {
//	        hsv[2] = 0.3f;
//	    }
//	    hsv[1] = hsv[1] * 0.2f;
	    
	    return new Color(newRed, newBlue, newGreen).getRGB();
    }
}
