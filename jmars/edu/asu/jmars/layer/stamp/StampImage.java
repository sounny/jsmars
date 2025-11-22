package edu.asu.jmars.layer.stamp;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.layer.stamp.StampLayer.StampTask;
import edu.asu.jmars.layer.stamp.StampLayer.Status;
import edu.asu.jmars.layer.stamp.projection.EquiRectangular;
import edu.asu.jmars.layer.stamp.projection.SimpleCylindrical;
import edu.asu.jmars.layer.stamp.projection.PolarStereographic;
import edu.asu.jmars.layer.stamp.projection.Projection;
import edu.asu.jmars.layer.stamp.projection.Sinusoidal;
import edu.asu.jmars.layer.stamp.projection.Unprojected;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;

public class StampImage 
{ 	
	// A value that will be used throughout the stamp layer to indicate numeric pixels that should be drawn transparently
	// (For graphic images, 0 is frequently used to indicate transparent pixels)
	public static final int IGNORE_VALUE = Short.MIN_VALUE;
	
    protected String productID;
    protected String label;
    
    protected ImageFrame frames[];
    
    // These values represent how many lines, in the full resolution image, relate to each subFrame for the frames we generate ourselves.
    // The goal is for the scaled image pieces we receive from the server to be as close to 500x500 pixels as is reasonably possible.
    protected int linesPerFrame;
    protected int linesLastFrame;
    
    protected int samplesPerFrame;
    protected int samplesLastFrame;
    
    protected int projHash;
    protected int renderPPD;
        
    protected String imageType="";
     
    // Numeric variables
    protected double minValue=Double.NaN;
    protected double maxValue=Double.NaN;
    
    protected double autoMin=Double.NaN;
    protected double autoMax=Double.NaN;
    protected boolean autoValuesChanged=false;
    
    protected boolean isNumeric = false;
    protected String units = "";
    protected String unitDesc = "";
    
    protected boolean isPDSImage = false;
    protected boolean isTHEMISDCS = false;
    protected boolean realFramePoints = false;
    
    protected boolean isGlobalImage = false;
    
    public String getImageType() {
    	return imageType;
    }
    
    public StampImage(StampShape shape, String productId, String instrument, String imageType, BufferedImage image, HashMap<String,String> params) {
    	myStamp=shape;
    	this.instrument = instrument;
    	this.imageType = imageType;
        this.productID = productId;
        label = null;                
        this.image = image;
        
        projectionParams = params;
        parseProjectionParams();
        	
    	if (instrument.equalsIgnoreCase("davinci")) {
    		fullImageLocal=true;
    		pts=new Point2D.Double[4];
    		pts[0]=new Point2D.Double(360-Double.parseDouble(params.get("lon0")), Double.parseDouble(params.get("lat0")));
    		pts[1]=new Point2D.Double(360-Double.parseDouble(params.get("lon1")), Double.parseDouble(params.get("lat1")));
    		pts[2]=new Point2D.Double(360-Double.parseDouble(params.get("lon3")), Double.parseDouble(params.get("lat3")));        	
    		pts[3]=new Point2D.Double(360-Double.parseDouble(params.get("lon2")), Double.parseDouble(params.get("lat2")));
    		
    		if (image!=null) {
    			numLines = image.getHeight();
    			numSamplesPerLine = image.getWidth();
    		}    		
    	} else {
    		// TODO: Do we need to call this for projected images?  Could we calculate points on our own?
    		getPoints();
    	}
    	
    	    // Points at 90 or -90 exactly aren't treated as distinct points - the longitude values are ignored by JMARS.  Possibly a general bug
       		// that should be addressed.  This works around it, and makes the point non-ambiguous.
    		for (int i=0; i<pts.length; i++) {
    			if (pts[i].getY()>=90.0) {
    				pts[i].setLocation(pts[i].getX(), 89.999);
    			} else if (pts[i].getY()<=-90.0) {
    				pts[i].setLocation(pts[i].getX(), -89.999);
    			}
    		}
    	
        if (map_projection_type.equalsIgnoreCase("EQUIRECTANGULAR")) {        	
       		imageProjection = new EquiRectangular(line_proj_offset, sample_proj_offset, center_lon, center_lat, map_resolution);
        } else if (map_projection_type.equalsIgnoreCase("POLAR_STEREO")) {
        	imageProjection = new PolarStereographic(line_proj_offset, sample_proj_offset, center_lon, center_lat, map_scale, radius);
        } else if (map_projection_type.equalsIgnoreCase("SINUSOIDAL")) { 
        	imageProjection = new Sinusoidal(line_proj_offset, sample_proj_offset, center_lon, center_lat, map_resolution);
        } else if (map_projection_type.equalsIgnoreCase("CYLINDRICAL")) {
        	imageProjection = new SimpleCylindrical(numLines, numSamplesPerLine, pts[0], pts[1], pts[2], pts[3], isGlobalImage);
        } else {
        	imageProjection = new Unprojected(numLines, numSamplesPerLine, pts[0], pts[1], pts[2], pts[3]);
        }
    }
    
    BufferedImage image = null;
    
    StampShape myStamp = null;
    
    Projection imageProjection = null;
        
    protected String instrument;
    
    public String getInstrument() {
    	return instrument;
    }
        
    public String getLabel()
    {
        return  label;
    }
    
    public String getUnits() {
    	return units;
    }
    
    public String getUnitDesc() {
    	return unitDesc;
    }
    
    public boolean isNumeric() {
    	return isNumeric;
    }
    
    boolean fullImageLocal = false;
    /**
     * Returns true if the entire image is stored locally at full resolution in a single file.
     * Returns false by default, and if we are working with individual subsampled tiles of an image.
     * @return
     */
    public boolean isFullImageLocal() {
    	return fullImageLocal;
    }
    
    boolean framePointsFaked=false;
    
    /**
     ** Generates a new set of geometry points to subdivide a single 
     ** frame of geometry points into the specified number of divisions.
     ** Frames are created along lower-to-upper axis.
     ** <p>
     ** NOTE: This method uses the approximate interpolation scheme
     **       present in the HVector class.  Should probably only be used
     **       for client-side image subtiling purposes when more accurate
     **       data is unavailable from an image geometry database, etc.
     **
     ** @param pts  an array of four points corresponding to the four
     **             corners of an image frame:
     ** 
     ** <ul>          
     ** <li>          points[0]   lower left corner coordinates
     ** <li>          points[1]   lower right corner coordinates
     ** <li>          points[2]   upper left corner coordinates
     ** <li>          points[3]   upper right corners coordinates
     ** </ul>
     **
     ** @return array of points organized as described for #getPoints(),
     **         whole image frame + subframes
     **
     **/
    protected Point2D[] getNewFakeFramePoints(Point2D[] pts, StampImageFrames newStampImageFrames)
    {
        if (pts == null ||
                pts.length != 4) {
        	// Nothing more we can do here.
            return pts;
        }

        double maxPPD = getMaxRenderPPD();

    	if (newStampImageFrames.renderPPD > maxPPD) {
        	newStampImageFrames.linesPerFrame = LINES_PER_FRAME;
    	} else {
        	newStampImageFrames.linesPerFrame = (int)Math.ceil(LINES_PER_FRAME * (maxPPD/newStampImageFrames.renderPPD));        		
    	}
    	
    	if (newStampImageFrames.linesPerFrame > getNumLines()) {
    		newStampImageFrames.linesPerFrame=getNumLines();
    	}
    	        	
    	// This is only the number of frames per column, we'll determine 
    	// how to split (or not split) horizontally later
        int numFrames = (int)Math.ceil(1.0* getNumLines() / newStampImageFrames.linesPerFrame);
        
        Point2D[] newPoints = new Point2D[numFrames * 4];
        
        // If there's only 1 frame, this makes the linesLastFrame==linesPerFrame.
    	newStampImageFrames.linesLastFrame = getNumLines() - newStampImageFrames.linesPerFrame*(numFrames-1);

    	if (realFramePoints || numFrames == 1) {
    		newPoints = pts;
    		// TODO: Just because we didn't fake frames, doesn't mean we couldn't
    		// still act like we did.  THEMIS might be the only exception
    		// And not necessarily all THEMIS?
//    		framePointsFaked=false;
    	} else {    	
	        // Prepare uppper part of first subframe.
	        Point2D nextUL = pts[2];
	        Point2D nextUR = pts[3];
	        
	        // Create image subframe geometry points through
	        // interpolation from whole image.  Do this
	        // for all but the last subframe.  Frames start
	        // from upper part of image.
	        for (int i=0; i < numFrames-1; i++) {
	            // Store geometry for new subframe
	            newPoints[i*4]   = imageProjection.lonLat((int)(newStampImageFrames.linesPerFrame * (i+1)), 1, new Point2D.Double());
	            newPoints[i*4+1] = imageProjection.lonLat((int)(newStampImageFrames.linesPerFrame * (i+1)), numSamplesPerLine, new Point2D.Double());
	            newPoints[i*4+2] = nextUL;
	            newPoints[i*4+3] = nextUR;
	            
	            // Prepare upper part of next subframe
	            nextUL = newPoints[i*4];
	            nextUR = newPoints[i*4+1];
	        }
	        
	        // Create last subframe using residual part of whole image frame
	        newPoints[(numFrames-1) * 4]   = pts[0];
	        newPoints[(numFrames-1) * 4+1] = pts[1];
	        newPoints[(numFrames-1) * 4+2] = nextUL;
	        newPoints[(numFrames-1) * 4+3] = nextUR;
	                
	        framePointsFaked=true;
	        
	        for(int i=0; i<newPoints.length-4; i+=4) {
	            newPoints[i+0] = newPoints[i+6] = midpoint(newPoints[i+0], newPoints[i+6]);
	            newPoints[i+1] = newPoints[i+7] = midpoint(newPoints[i+1], newPoints[i+7]);
	        }
    	}
    	
        return splitFrames(newPoints, newStampImageFrames);
    }
        
    private static final int LINES_PER_FRAME=500;
    
    private Point2D[] splitFrames(Point2D pts[], StampImageFrames newStampImageFrames) {
    	// TODO: Maybe this should be always calculated and returned by the server
    	double maxPPD = getMaxRenderPPD();
    	    	
    	if (newStampImageFrames.renderPPD > maxPPD) {
        	newStampImageFrames.samplesPerFrame = LINES_PER_FRAME;
    	} else {
        	newStampImageFrames.samplesPerFrame = (int)Math.ceil(LINES_PER_FRAME * (maxPPD/newStampImageFrames.renderPPD));        		
    	}

    	if (newStampImageFrames.samplesPerFrame > getNumSamples()) {
    		newStampImageFrames.samplesPerFrame=getNumSamples();
    	}    	

    	newStampImageFrames.horizontalSplitCnt=(int)Math.ceil(1.0*getNumSamples()/newStampImageFrames.samplesPerFrame);
    	
    	if (newStampImageFrames.horizontalSplitCnt<1) {
    		newStampImageFrames.horizontalSplitCnt=1;
    	}

    	// Check for frames that are too many degrees wide
    	if (isGlobalImage) {
    		double degrees = distance(pts[0], pts[1]);
			if (degrees<0.000000000000001) {
				degrees=360;
			}
		
			if ((degrees / newStampImageFrames.horizontalSplitCnt)>120) {
				newStampImageFrames.horizontalSplitCnt *= 3;
				newStampImageFrames.samplesPerFrame = (int)Math.ceil(getNumSamples() / (newStampImageFrames.horizontalSplitCnt));
			}
		}
    	
        // If there's only 1 frame, this makes the linesLastFrame==linesPerFrame.
    	newStampImageFrames.samplesLastFrame = getNumSamples() - newStampImageFrames.samplesPerFrame*(newStampImageFrames.horizontalSplitCnt-1);

    	if (newStampImageFrames.horizontalSplitCnt==1) {
    		// No more work to do
    		return pts;
    	}
    	    	
        int numFrames =pts.length/4;
        
        Point2D[] newPoints = new Point2D[newStampImageFrames.horizontalSplitCnt*numFrames * 4];
                
//        double framePercent = 1.0 / horizontalSplitCnt;
        
        int total_samples=getNumSamples();
        
        // Create image subframe geometry points through
        // interpolation from whole image.  Frames start
        // from upper part of image.
        for (int i=0; i < numFrames; i++) {        	
            Point2D[] topRow = new Point2D[newStampImageFrames.horizontalSplitCnt+1];
            Point2D[] botRow = new Point2D[newStampImageFrames.horizontalSplitCnt+1];
            
            for (int n=0; n<(newStampImageFrames.horizontalSplitCnt+1); n++) {
            	if (n==newStampImageFrames.horizontalSplitCnt) { // last column
	        		if (i==numFrames-1) { // last row
	            		topRow[n]=imageProjection.lonLat(i*newStampImageFrames.linesPerFrame, total_samples, new Point2D.Double());
	            		botRow[n]=imageProjection.lonLat(numLines, total_samples, new Point2D.Double());
	        		} else {
	            		topRow[n]=imageProjection.lonLat(i*newStampImageFrames.linesPerFrame, total_samples, new Point2D.Double());
	            		botRow[n]=imageProjection.lonLat((i+1)*newStampImageFrames.linesPerFrame, total_samples, new Point2D.Double());
	        		}            		
            	} else {
	        		if (i==numFrames-1) { // last row
	            		topRow[n]=imageProjection.lonLat(i*newStampImageFrames.linesPerFrame, (newStampImageFrames.samplesPerFrame*n), new Point2D.Double());
	            		botRow[n]=imageProjection.lonLat(numLines, (newStampImageFrames.samplesPerFrame*n), new Point2D.Double());
	        		} else {
	            		topRow[n]=imageProjection.lonLat(i*newStampImageFrames.linesPerFrame, (newStampImageFrames.samplesPerFrame*n), new Point2D.Double());
	            		botRow[n]=imageProjection.lonLat((i+1)*newStampImageFrames.linesPerFrame, (newStampImageFrames.samplesPerFrame*n), new Point2D.Double());
	        		}
            	}
            }            
            
            int newFrameCnt = topRow.length-1;
            
            for (int j=0; j<newFrameCnt; j++) {
            	int newFrameNum = newStampImageFrames.horizontalSplitCnt*i+j;

            	newPoints[(4*newFrameNum)   ]	= botRow[j];
            	newPoints[(4*newFrameNum) +1]	= botRow[j+1];
            	newPoints[(4*newFrameNum) +2]	= topRow[j];
            	newPoints[(4*newFrameNum) +3]	= topRow[j+1];
            }            
        }

        return newPoints;    	
    }    

    
    
    
    Point2D[] pts = null;
    
    public Point2D[] getPoints() { 
    	if (pts==null) {
    		try {
    			String urlStr = "PointFetcher?id="+productID+"&instrument="+getInstrument();
    			
    			if (imageType!=null && imageType.length()>0) {
    				urlStr+="&imageType="+imageType;
    			}
            
    			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));
        
    			double dpts[] = (double[])ois.readObject();
    		               		   
    			pts = new Point2D[dpts.length/2]; 
    	        
	    	    for (int i=0; i<pts.length; i++) {
	    	    	pts[i]=new Point2D.Double(dpts[2*i], dpts[2*i+1]);
	    	    }
	    	    
	    	    //skip this for loop if this is for a radar layer (SHARAD)
	    	    if(!myStamp.stampLayer.lineShapes()){
		    	    // This is used to try and blend real frame points for THEMIS images, 
		    	    // so that there isn't a gap between frames.
		    	    if (imageType!=null && imageType.length()>0 && pts.length>4) {
			            for(int i=0; i<pts.length-4; i+=4) {
			                pts[i+0] = pts[i+6] = midpoint(pts[i+0], pts[i+6]);
			                pts[i+1] = pts[i+7] = midpoint(pts[i+1], pts[i+7]);
			            }
		    	    }
	    	    }
	    	    
	    	    ois.close();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}    		
    	}
    	
    	return pts;
    }
    
    static Point2D midpoint(Point2D a, Point2D b)
    {
        return  new HVector(a).add(new HVector(b)).toLonLat(null);
    }
    
    double maxPPD = -1;
    
    protected double getMaxRenderPPD() {
    	if (map_resolution!=0) return map_resolution;
    	
    	if (maxPPD==-1) {    	
			Point2D points[] = getPoints();
			
			double degrees = distance(points[0], 
					                      points[1]);
				
			if (isGlobalImage && degrees<0.000000000000001) {
				degrees=360;
			}
			
			int pixels = getNumSamples();
			
			maxPPD = pixels / degrees;
    	}
    	
    	return maxPPD;
    }
    
	public double distance(Point2D pointA, Point2D pointB)
	 {
		double lonA = Math.toRadians(pointA.getX());
		double latA = Math.toRadians(pointA.getY());

		double lonB = Math.toRadians(pointB.getX());
		double latB = Math.toRadians(pointB.getY());

		
		HVector a = new HVector(Math.cos(latA)*Math.cos(-lonA),
				Math.cos(latA)*Math.sin(-lonA),
				Math.sin(latA));
				
		HVector b = new HVector(Math.cos(latB)*Math.cos(-lonB),
				Math.cos(latB)*Math.sin(-lonB),
				Math.sin(latB));
		
		return  Math.toDegrees(a.separation(b));
	 }
    
    /**
     ** Renders the image onto the given (world-coordinate) graphics context.
     **/
    public synchronized void renderImage(final Graphics2D wg2, final FilledStamp fs,
                            final StampLView.DrawFilledRequest request, StampTask task, Point2D offset, BufferedImage target, ProjObj po, int renderPPD)
    {
        task.updateStatus(Status.RED);
        
        String key = po.getCenterLon()+":"+po.getCenterLat()+":"+renderPPD;
    	if (!cachedFrames.containsKey(key)) {
    		recreateImageFrames(renderPPD, po);
    	}
    	StampImageFrames imageFrames = cachedFrames.get(key);
        
        try {
            final Rectangle2D worldWin = request.getExtent();
            
            // If stamps have been nudged, we need to make sure that we take the nudge into account when determining whether or not a
            // particular imageframe intersects the visible part of the screen or not
            final Rectangle2D offsetWorldWin=new Rectangle2D.Double();;
            offsetWorldWin.setRect(worldWin.getX()-offset.getX(), worldWin.getY()-offset.getY(), worldWin.getWidth(), worldWin.getHeight());
            
            double maxRenderPPD=getMaxRenderPPD();
            
            // TODO: Revisit this
//            if (!map_projection_type.equalsIgnoreCase("UNPROJECTED") && renderPPD>maxRenderPPD) {
//            	renderPPD=(int)maxRenderPPD;
//            }
            
           	recreateImageFrames(renderPPD, po);

            List<ImageFrame> framesInView = new ArrayList<ImageFrame>();
            for(int i=0; i<imageFrames.frames.length; i++) {        
            	if (doesFrameIntersect(imageFrames.frames[i], offsetWorldWin)) {
            		framesInView.add(imageFrames.frames[i]);
            	}
            }

//            if (framePointsFaked && !getInstrument().equalsIgnoreCase("davinci")) {
//            	// Only expand if we're dealing with projected images
//            	boolean expand = !map_projection_type.equalsIgnoreCase("UNPROJECTED");
//
//            	// TODO: Review this code very closely.  It becomes very expensive for high resolution images zoomed way in, such as 
//            	// HiRISE at 262144 ppd.  Ends up being 14000 frames it works on, in an attempt to optimize network retrieval that may not even
//            	// be necessary.  Maybe check frames in view, then check if they have data locally, and then segment anything that remains somehow?
//            	ImageFrame frameSegmentsToFetch[][][] = FrameFetcher.segment(frames, horizontalSplitCnt, frames.length/horizontalSplitCnt, offsetWorldWin, expand);
//
//            	for (int i=0; i<frameSegmentsToFetch.length; i++) {        	
//            		FrameFetcher ff = new FrameFetcher(frameSegmentsToFetch[i]);
//            		ff.fetchFrames();
//            	}
//            }

            task.updateStatus(Status.YELLOW);

            int loopCnt=0;
            restart: while(true) {
            	loopCnt++;
            	if (loopCnt>framesInView.size()+1) {
            		System.out.println("Autoscale values didn't converge");
            		break;
            	}
	            for(ImageFrame f : framesInView) {
	            	if (!request.changed()) {
	            		autoValuesChanged=false;
	           			f.drawFrame(f, offsetWorldWin, wg2, (FilledStampImageType)fs, target, request);
	           			if (autoValuesChanged) {
	           				// Abort and restart this loop
	           				continue restart;
	           			}
	            	} else {
	            		// Parameters changed, abort frame draw
	            		return;
	            	}
	            }
	            break;
            }
        } finally {
            task.updateStatus(Status.DONE);
        }
    }
        
    public static boolean doesFrameIntersect(ImageFrame frame, Rectangle2D worldWin) {
    	if (frame==null) return false;  // Can occur when testing whether to expand tile requests
    	
        final double base = Math.floor(worldWin.getMinX() / 360.0) * 360.0;
        
        final int numWorldSegments =
            (int) Math.ceil (worldWin.getMaxX() / 360.0) -
            (int) Math.floor(worldWin.getMinX() / 360.0);
        
        Rectangle2D.Double where = new Rectangle2D.Double();
        
        where.setRect(frame.cell.getWorldBounds());
        double origX = where.x;
        
        int start = where.getMaxX() < 360.0 ? 0 : -1;
        
        for(int m=start; m<numWorldSegments; m++) {
            where.x = origX + base + 360.0*m;
            if(worldWin.intersects(where)) {
            	return true;
            }
        }                   
        
        return false;
    }
    
    // currently unused
    Area clipArea=null;
    
    // The area of this stamp generated using the render points
    Area realClipArea = null;
    
    // The realClipArea of this stamp minus the overlapping areas of any
    // other rendered stamps that are higher in the view stack
    // Needed for multithreaded rendering, unused at the moment
    Area currentClipArea = null;
    
    // Not sure this needs to be public
    public Area getRealClipArea() {
    	if (realClipArea==null) {
    		realClipArea=new Area(getNormalPath());
    	}
    	
    	return realClipArea;  	
    }
    
    public void clearCurrentClip() {
    	currentClipArea=null;
    }
    
    public void calculateCurrentClip(List<StampImage> higherImages) {
    	// Short circuited until multithreaded rendering is enabled.
    	if (true) return;
    	if (currentClipArea==null) {
	    	currentClipArea = getAdjustedClipArea(getRealClipArea());
	    	
	    	for (StampImage s : higherImages) { 		
	    		// Somehow we need to worry about +/- 360 issues here too
	    		if (s.getRealClipArea().intersects(realClipArea.getBounds2D())) {
	    			currentClipArea.subtract(s.getRealClipArea());
	    		}    		
	    	}    	
    	}
    }
    
    public Area getCurrentClipArea() {
    	return currentClipArea;
    }
    
    
    public static Area getAdjustedClipArea(Area startingArea) {
		Area newArea = new Area(startingArea);
		
    	// Handle the cases where wrapped coordinates get us into +360 or -360
		// space.
        AffineTransform transformer2 = new AffineTransform();
        transformer2.translate(360, 0);
    	Area area2 = ((Area) newArea.clone());
    	area2.transform(transformer2);
    	
        AffineTransform transformer3 = new AffineTransform();
        transformer3.translate(-360, 0);
    	Area area3 = ((Area) newArea.clone());
    	area3.transform(transformer3);

        AffineTransform transformer4 = new AffineTransform();
        transformer4.translate(720, 0);
    	Area area4 = ((Area) newArea.clone());
    	area4.transform(transformer4);
    	
        AffineTransform transformer5 = new AffineTransform();
        transformer5.translate(-720, 0);
    	Area area5 = ((Area) newArea.clone());
    	area5.transform(transformer5);

        AffineTransform transformer6 = new AffineTransform();
        transformer6.translate(1080, 0);
    	Area area6 = ((Area) newArea.clone());
    	area6.transform(transformer6);
    	
        AffineTransform transformer7 = new AffineTransform();
        transformer7.translate(-1080, 0);
    	Area area7 = ((Area) newArea.clone());
    	area7.transform(transformer7);
	    	
    	Area returnArea = new Area(newArea);
    	returnArea.add(area2);
    	returnArea.add(area3);
    	returnArea.add(area4);
    	returnArea.add(area5);
    	returnArea.add(area6);
    	returnArea.add(area7);    		
    	
    	return returnArea;
    }
    

    
    /**
     ** Not quite sure what this does: see implementation in
     ** PdsImage. -- Joel Hoff
     **/
    public int[] getHistogram() throws IOException
    {
        return null;
    }
                           
    public int getHeight()
    {
    	if (image!=null) {
            return image.getHeight();    		
    	} else {
    		return getNumLines();
    	}
    }
    
    public int getWidth()
    {
    	if (image!=null) {
    		return image.getWidth();
    	} else {
    		return getNumSamples();
    	}
    }
    
    // This block moved over from StampShape - it's tied to imageType,
    // not generic by Shape...
    private int numLines=Integer.MIN_VALUE;
    private int numSamplesPerLine=Integer.MIN_VALUE;
    
    public int getNumLines() {
    	if (numLines==Integer.MIN_VALUE) {
    		getFullImageSize();
    	}
    	return numLines;
    }
    
    public int getNumSamples() {
    	if (numSamplesPerLine==Integer.MIN_VALUE) {
    		getFullImageSize();
    	}
    	return numSamplesPerLine;
    }
    
    private void getFullImageSize() {
		try {
            String sizeLookupStr = "ImageSizeLookup?id="+productID+"&instrument="+getInstrument()+"&imageType="+imageType+"&format=JAVA";
					
			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(sizeLookupStr));
			
			Integer samples = (Integer)ois.readObject();
			Long lines = (Long)ois.readObject();
			
			// Not sure why I ever thought lines needed to be a Long.  An image that's 2 BILLION lines long is... big.
			numLines = lines.intValue();
			numSamplesPerLine = samples.intValue();
			
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
			numLines=0;
			numSamplesPerLine=0;
			e.printStackTrace();
		}
    }
    
    
    // Returns 32-bit RGB color value at specified pixel location; may
    // include alpha component.
    synchronized public int getRGB(int x, int y) throws Exception 
    {
        // TODO: THEMIS VIS image products, particularly BWS and RGB, don't
        // follow the normal rules for how long the last frame is, causing us to
        // throw an exception while rendering, but still display all of the data
        // correctly.  This check prevents that exception.  This should be
        // properly fixed at some point - probably when THEMIS images are
        // rendered via normal projection logic.
    	if (x>image.getWidth() || y>image.getHeight()) {
    		return 0;
    	}
        return image.getRGB(x, y);
    }
    
    String lastCachedProjStr = null;
    BufferedImage cachedImage = null;
    synchronized public float getFloatVal(HVector ptVect, ProjObj po, int renderPPD)
    {
    	if (myStamp.stampLayer.globalShapes()) {
			String projStr = Main.PO.getCenterLon()+":"+Main.PO.getCenterLat();
			
			String cacheProjStr = instrument + "_"+productID+":"+projStr;
			
			BufferedImage img = null;
						
			// We use the original UNPROJECTED image as our source rather than accessing the projected image
			String urlStr = "ImageServer?instrument="+instrument+"&id="+productID+"&zoom=100";
						
			cacheProjStr = urlStr;
			
			if (lastCachedProjStr!=null && lastCachedProjStr.equalsIgnoreCase(cacheProjStr)) {
				img = cachedImage;
			}
			
			if (img==null) {
				img = StampImageFactory.getImage(urlStr, true);
				lastCachedProjStr = cacheProjStr;
				cachedImage = img;
			}
			
			if (img==null) {
				if (img==null) return Float.NaN;
			}

			int width = img.getWidth();
			int height = img.getHeight();
			
			double lon = ptVect.lon();						
			double lat = ptVect.lat(); 
			
			int x = width - (int)Math.ceil((lon / 360.0) * width);
			int y = height - (int)Math.ceil(((lat+90) / 180.0) * height);
			
			boolean shift180 = myStamp.stampLayer.shift180();
			
			// Adjust our x values depending on whether the image is 0-360 or -180 to 180
			if (!shift180) {
				int halfWidth = width / 2;					
				
				if (x<halfWidth) {
					x+=halfWidth;
				} else {
					x-=halfWidth;
				}
    		}
    	
			float f = Float.NaN;
			try {
				f = img.getRaster().getSampleFloat(x, y, 0);
			} catch (Exception e) {
				System.out.println("Bad: x = " + x + " width = " + width);
				e.printStackTrace();
			}
			return f;
    	}
    	
    	String key = po.getCenterLon()+":"+po.getCenterLat()+":"+renderPPD;
    	if (!cachedFrames.containsKey(key)) {
    		recreateImageFrames(renderPPD, po);
    	}
    	StampImageFrames imageFrames = cachedFrames.get(key);
    	
        for (ImageFrame frame : imageFrames.frames) {
            Point2D.Double unitPt = new Point2D.Double();
            
            if (frame==null || frame.cell==null) {
            	continue; 
            }
            
            frame.cell.uninterpolate(ptVect, unitPt);
            
            // Check whether point falls within cell.
            if (unitPt.x >= 0  &&  unitPt.x <= 1  &&
                unitPt.y >= 0  &&  unitPt.y <= 1  )
            {
            	
            	Point2D worldPoint = new Point2D.Double(ptVect.toWorld().getX(), ptVect.toWorld().getY());
				int ppd = renderPPD;
								
				double pixelWidth = 1.0 / ppd;
				
				double x = worldPoint.getX();
				double y = worldPoint.getY();
				
				// get tile range of this wrapped piece				
				double xstart = Math.floor(x / pixelWidth) * pixelWidth;
				double ystart = Math.floor(y / pixelWidth) * pixelWidth;
					
				Rectangle2D tileExtent = new Rectangle2D.Double(xstart, ystart, 1d/ppd, 1d/ppd);
				
				BufferedImage image2 = frame.getProjectedImage(tileExtent);
				
				float v2 = image2.getRaster().getSampleFloat(0, 0,  0);
				return v2;            	
            }
        }
        
        return Float.NaN;
    }
                    
    synchronized public double[][] getSpectralVals(int ppd, ProjObj po, HVector ptVect, int xWidth, int yWidth) {		    	
		double lineSample = getFloatVal(ptVect, Main.PO, ppd);
		
		int sample = (short)((int)lineSample);
		int line = (short)((int)lineSample>>16);
		
			try {
				String yeardoy = (String)myStamp.getVal("year_doy");
				
				//create the url string
				String urlStr = "FetchSpectraValues?instrument="+instrument+"&id="+productID+"&yeardoy="+yeardoy+
						"&line="+line+"&sample="+sample+"&xwidth="+xWidth+"&ywidth="+yWidth;
				
				//access the stamp server and cast the result
				ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));
				double vals[] = (double[])ois.readObject();
				double axis[] = (double[])ois.readObject();
				
				double data[][] = {vals, axis};
				
				return data;
			} catch (Exception e) {
				e.printStackTrace();
			}
        return new double[0][0];
    }
    
    
    // Convenience method, to avoid scattering this silly string of object references all over the code just to get the current zoomPPD
    protected synchronized void recreateImageFrames(ProjObj po) {
    	int zoomPPD = myStamp.stampLayer.viewToUpdate.viewman.getZoomManager().getZoomPPD();
    	recreateImageFrames(zoomPPD, po);
    	
    }
    
    class StampImageFrames {
    	ProjObj po;
	    protected ImageFrame frames[];
	    
	    // These values represent how many lines, in the full resolution image, relate to each subFrame for the frames we generate ourselves.
	    // The goal is for the scaled image pieces we receive from the server to be as close to 500x500 pixels as is reasonably possible.
	    protected int linesPerFrame;
	    protected int linesLastFrame;
	    
	    protected int samplesPerFrame;
	    protected int samplesLastFrame;
	    
        int horizontalSplitCnt=1;
    
	    protected int renderPPD;    	
    }

    HashMap<String, StampImageFrames> cachedFrames = new HashMap<String, StampImageFrames>();
    
    protected synchronized void recreateImageFrames(int renderPPD, ProjObj po)
    {
    	String key = po.getCenterLon()+":"+po.getCenterLat()+":"+renderPPD;
    	
    	if (cachedFrames.containsKey(key)) {
    		
    	} else {
    		StampImageFrames newStampImageFrames = new StampImageFrames();
    		
    		newStampImageFrames.po = po;
	        newStampImageFrames.renderPPD = renderPPD;
	
	        Point2D[] pts = getPoints();
	                
	   		if (pts.length>4 && realFramePoints) {
	   			// TODO: Maybe these should also be server side paramters?
				if (myStamp.getId().startsWith("I")) {
					// Native resolution, IR images are all 256 pixels per frame.  Frame points are provided for us though
					newStampImageFrames.linesPerFrame=256;
					newStampImageFrames.linesLastFrame=getHeight()%256;
					if (newStampImageFrames.linesLastFrame==0) newStampImageFrames.linesLastFrame=256;
				} else {
					// VIS is 192 lines per frame, adjusted for summing mode 
					newStampImageFrames.linesPerFrame=192/summing;
					newStampImageFrames.linesLastFrame=newStampImageFrames.linesPerFrame;
				}
				
				newStampImageFrames.samplesPerFrame=getWidth();
				newStampImageFrames.samplesLastFrame=getWidth();
	   		} else {
	   			pts = getNewFakeFramePoints(pts, newStampImageFrames);   			
	   		}
	   		
	        int frameCount = pts.length / 4;
	        newStampImageFrames.frames = new ImageFrame[frameCount];
	                
	    	for (int i=0; i<frameCount; i++) {	    		      
	    		
	    		// The last horizonalSplitCnt frames are the last row
	    		boolean lastRow= i>=newStampImageFrames.frames.length-newStampImageFrames.horizontalSplitCnt;
	    		
	    		int frameHeight;
	    		if (lastRow) {
	    			frameHeight=(int)newStampImageFrames.linesLastFrame;
	    		} else {
	    			frameHeight=(int)newStampImageFrames.linesPerFrame;
	    		}
	    		
		        Rectangle srcRange;
		        
		        boolean lastCol= (i%newStampImageFrames.horizontalSplitCnt)==newStampImageFrames.horizontalSplitCnt-1;
		        
		        int frameWidth;
		        if (lastCol) {
		        	frameWidth = newStampImageFrames.samplesLastFrame;
		        } else {
		        	frameWidth = newStampImageFrames.samplesPerFrame;
		        }
		        
		        int startx = (i%newStampImageFrames.horizontalSplitCnt) * newStampImageFrames.samplesPerFrame;
	
		        int starty = (i/newStampImageFrames.horizontalSplitCnt) * newStampImageFrames.linesPerFrame;
		        
	        	srcRange = new Rectangle(startx, starty, frameWidth, frameHeight);	        	
		        
		        Cell frameCell = new Cell(
		                 new HVector(pts[i*4]),
		                 new HVector(pts[i*4+1]),
		                 new HVector(pts[i*4+3]),
		                 new HVector(pts[i*4+2]), (Projection_OC)po);
		                
		        newStampImageFrames.frames[i] = new ImageFrame(this, productID, imageType, frameCell, srcRange, i, renderPPD, po.getProjectionSpecialParameters().hashCode(), newStampImageFrames);	                             
	    		
	    		cachedFrames.put(key,  newStampImageFrames);
	    	}
    	}    	
    }
        
    double line_proj_offset = 0;
    double map_resolution = 0;
    double map_scale = 0;
    double sample_proj_offset = 0;
    double center_lon = 0;
    double center_lat = 0;
    double radius = 0;  // TODO Set this to an appropriate default
    String map_projection_type = "UNPROJECTED";

    boolean clip_to_path = false;
    public long ignore_value = -1;
    
    // Used by THEMIS VIS for BWS images
    int summing=1;
    
    HashMap<String,String> projectionParams=null;
        
    /**
     * Return a copy of the projection params, for display to the user or other purposes
     * @return
     */
    public HashMap<String,String> getProjectionParams() {
    	return (HashMap<String,String>)projectionParams.clone();
    }
    
    public void parseProjectionParams() {
		// TODO: Make this not break if any of these parameters aren't present
    	   map_projection_type = "UNPROJECTED";
    	   
    	   if (projectionParams.containsKey("map_projection_type")) {
    		   map_projection_type = projectionParams.get("map_projection_type");
    	   }

    	   if (projectionParams.containsKey("clip_to_path")) {
    		   String ctp=projectionParams.get("clip_to_path");
    		   if (ctp.equalsIgnoreCase("true")) {
    			   clip_to_path=true;
    		   }
    	   }

    	   if (projectionParams.containsKey("ignore_value")) {
    		   String ignore_str=projectionParams.get("ignore_value");
    		   ignore_value = Long.parseLong(ignore_str);
    	   }
    	   
    	   if (projectionParams.containsKey("lines")) {
    		   numLines = Integer.parseInt(projectionParams.get("lines"));
    	   }
    	   
    	   if (projectionParams.containsKey("samples")) {
    		   numSamplesPerLine = Integer.parseInt(projectionParams.get("samples"));
    	   }
    	   
    	   if (projectionParams.containsKey("summing")) {
    		   summing = Integer.parseInt(projectionParams.get("summing"));
    	   }
    	   
    	   if (projectionParams.containsKey("numeric")) {
    		   isNumeric = true;
    	   }
    	   
    	   // New options, to avoid hardcoding things like 'THEMIS' - ABR, BTR, PBT
    	   if (projectionParams.containsKey("pdsImage")) {
    		   isPDSImage = true;
    	   }
    	   
    	   // Older THEMIS DCS images.  New images are properly projected... I think.
    	   if (projectionParams.containsKey("themisDCS")) {
    		   isTHEMISDCS = true;
    	   }    	   
    	   
    	   // Did we download the entire image, or just a subset
    	   // True for old THEMIS BWS images, and possibly others
    	   if (projectionParams.containsKey("fullImageLocal")) {
    		   fullImageLocal = true;
    	   }
    	   
    	   // We have tie points for every frame, so don't need to interpolate fake ones (usually means THEMIS VIS)
    	   if (projectionParams.containsKey("realFramePoints")) {
    		   realFramePoints = true;
    	   }

    	   if (projectionParams.containsKey("units")) {
    		   units = projectionParams.get("units");
    	   }

    	   if (projectionParams.containsKey("unit_desc")) {
    		   unitDesc = projectionParams.get("unit_desc");
    	   }

    	   if (projectionParams.containsKey("minValue")) {
    		   minValue = Double.parseDouble(projectionParams.get("minValue"));
    	   }

    	   if (projectionParams.containsKey("maxValue")) {
    		   maxValue = Double.parseDouble(projectionParams.get("maxValue"));
    	   }

       	   if (projectionParams.containsKey("globalImage")) {
    		   isGlobalImage = true;
    	   }

    	   if (map_projection_type.equalsIgnoreCase("CYLINDRICAL")) {
    		   return;
    	   }

    	   if (map_projection_type.equalsIgnoreCase("UNPROJECTED")) {
    		   return;
    	   }
    	   
    	   // TODO: Add logic to behave gracefully if all of these parameters aren't present
    	   line_proj_offset = Double.parseDouble(projectionParams.get("line_projection_offset"));
    	   map_resolution = Double.parseDouble(projectionParams.get("map_resolution"));
    	   sample_proj_offset = Double.parseDouble(projectionParams.get("sample_projection_offset"));
    	   center_lon = Double.parseDouble(projectionParams.get("center_longitude"));
    	   map_scale = Double.parseDouble(projectionParams.get("map_scale"));
    	   center_lat = Double.parseDouble(projectionParams.get("center_latitude"));
    	   
    	   // TODO: If we get here and don't have a radius, should be abort somehow?
    	   radius = Double.parseDouble(projectionParams.get("radius"));
    }

    /**
     ** Returns a (cached) normalized version of the stamp's path.
     ** @see Util#normalize360
     **/
    private Shape normalPath;
    
    public synchronized Shape getNormalPath()
    {
        if(normalPath == null)
            normalPath = StampShape.normalize360(getPath());
        return  normalPath;
    }
    
    GeneralPath path;

	// This is the path of the rendered shape - note that this may go outside the
	// bounds of the stamp polygon, and may include more area than we want to 
	// make visible to the user.
	public synchronized GeneralPath getPath()
	{
	    if(path == null)
	    {
	        path = new GeneralPath();
	        Point2D pt;
	                    
	        Point2D pts[] = getPoints();
	
	        // trace a line down the left edge
	        for (int i=2; i<pts.length; i=i+4) {
	            pt = Main.PO.convSpatialToWorld(pts[i].getX(),
	                    pts[i].getY());
	
	            if (i==2) {
	            	path.moveTo((float)pt.getX(),
	                    (float)pt.getY());
	            } else {
	            	path.lineTo((float)pt.getX(),
	                        (float)pt.getY());                	
	            }
	        }
	
	        pt = pts[pts.length-4];
	        pt = Main.PO.convSpatialToWorld(pt.getX(),
	                pt.getY());
	        
	    	path.lineTo((float)pt.getX(),
	                (float)pt.getY());                	
	
	        // and then back up the right edge
	        for (int i=pts.length-3; i>0; i=i-4) {
	            pt = Main.PO.convSpatialToWorld(pts[i].getX(),
	                    pts[i].getY());
	
	           	path.lineTo((float)pt.getX(), (float)pt.getY());                	
	        }            
	
	        pt = pts[3];            
	        pt = Main.PO.convSpatialToWorld(pt.getX(),
	                pt.getY());
	        path.lineTo((float)pt.getX(), (float)pt.getY());
	
	        path.closePath();
	    } 
	    return  path;
	}	
}