package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.WritableRaster;
import java.net.URLEncoder;
import java.util.List;

import edu.asu.jmars.layer.stamp.StampImage.StampImageFrames;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.IgnoreComposite;
import edu.asu.jmars.util.Util;


/* An instance of ImageFrame represents a portion of a rendered stamp at a specific zoom level and projection.
 *
 * This class contains the logic necessary to cache the raw, non-JMARS projected data as well as the logic necessary to cache
 * the final, JMARS projected data.
 */
public class ImageFrame {
	public StampImage wholeStamp;
	public String productID;
	public String imageType;
	public Cell cell;   // This defines the extent of this frame in spatial coordinates
	
	public int frameNum;
	public int startx;  // This is the starting x position of this frame in the original non-JMARS projected image
	public int starty;  // This is the starting y position of this frame in the original non-JMARS projected image
	public int height;  // This is the height of this frame in the original non-JMARS projected image
	public int width;   // This is the width of this frame in the original non-JMARS projected image
	
	public int renderPPD;
	public int projHash;
	
	StampImageFrames stampImageFrames;
	
	ImageProjecter ip = null;
	
	private static final DebugLog log = DebugLog.instance();
    
    ImageFrame(StampImage stampImage, String productID, String imageType, Cell cell, Rectangle srcRange, int frameNum, int renderPPD, int projHash, StampImageFrames stampImageFrames)
    {        	
    	this.cell = cell;
        this.wholeStamp = stampImage;
        this.productID = productID;
        this.imageType = imageType;
        this.frameNum = frameNum;
        this.renderPPD = renderPPD;
        this.projHash = projHash;
        this.stampImageFrames = stampImageFrames;
        
    	width = (int)srcRange.getWidth();
   		startx=(int)srcRange.getX();    	
    	starty = (int)srcRange.getY();
    	height = (int)srcRange.getHeight();
    }
    
    /*
     * This method allows for the creation of a projected image that isn't based on the normal cell boundaries of a frame.
     * 
     * There are currently two situations where this is useful:
     *   1) When a low resolution image is viewed at a high resolution, the frame sizes become enormous.  Instead of projecting the entire frame,
     *      we just project the worldWindow, which is a more manageable size.
     *   2) When we just need a single pixel, such as for the Investigate Tool.
     *   
     *   In both cases, we ignore the usual cache.
     */
    public synchronized BufferedImage getProjectedImage(Rectangle2D worldWin) {
//    	String tfileName = wholeStamp.getInstrument()+":"+wholeStamp.productID+":"+wholeStamp.imageType+":"+frameNum+":"+worldWin;
//    	BufferedImage tImage = StampCache.readProj(tfileName, wholeStamp.isNumeric);
//    	
//    	if (tImage!=null) return tImage;
    	
    	ImageProjecter tempProjecter = new ImageProjecter(this, worldWin);
    	
    	BufferedImage tempImage = tempProjecter.getProjectedImage();
    	
//        if (tempImage != null && !wholeStamp.getInstrument().equalsIgnoreCase("davinci")) {
//        	StampCache.writeProj(tempImage, tfileName);
//        }    	
    	
    	return tempImage;
    }

    public BufferedImage getProjectedImage() {
    	if (dstImage==null) {
    		dstImage = StampCache.readProj(getProjectedFileName(), wholeStamp.isNumeric);
    	}
    	    	
    	if (dstImage==null) {
	    	if (ip==null) {
	    		ip = new ImageProjecter(this, cell.getWorldBounds());
	    	}
	    	
	    	// TODO: Does this case fail for really large images trying to do numeric lookups?
	    	dstImage = ip.getProjectedImage();
	    	
    	    // Don't cache davinci images.  We might want to create new ones with the same id, and old cache will be invalid
	        if (dstImage != null && !wholeStamp.getInstrument().equalsIgnoreCase("davinci")) {
	        	StampCache.writeProj(dstImage, getProjectedFileName());
	        }
    	}

    	return dstImage;
    }

    
    public int getWidth() {
    	return width;
    }
    
    public int getHeight() {
    	return height;
    }
    
    public int getX() {
    	return startx;
    }
    
    public int getY() {
    	return starty;
    }
        
    // This is the filename of the JMARS-projected image for this frame
    String projectedFileName = null;
    protected String getProjectedFileName()
    {
    	if (projectedFileName == null) {    	
        	projectedFileName = productID + "_" + projHash + "_" + frameNum + "_" + imageType + "_" + renderPPD;
    	}        
    	
        return projectedFileName;
    }
    
    private String urlStr=null;
    
    public String getUrlStr() {
    	if (urlStr==null) {    		
			int scale =renderPPD; 
	
	    	StampShape s = wholeStamp.myStamp;
			String instrument = wholeStamp.getInstrument();
			String type = wholeStamp.imageType;
		
	    	urlStr="ImageServer?instrument="+instrument+"&id="+s.getId();
	    	
	    	if (type!=null && type.length()>=0) {
	    		urlStr+="&imageType="+URLEncoder.encode(type);
	    	}
	    	
	    	urlStr+="&zoom="+scale;
	    	urlStr+="&startx="+startx;
	    	urlStr+="&starty="+starty;
	    	urlStr+="&height="+height;
	    	urlStr+="&width="+width;
    	}
    	return urlStr;
    }
        
	public BufferedImage dstImage;
	
    synchronized boolean hasImageLocally() {
    	if (dstImage!=null) {
    		return true;
    	}
    	
        dstImage = StampCache.readProj(getProjectedFileName(), wholeStamp.isNumeric);
        
        if (dstImage != null) {
     	   return true;
        }
        
    	if (null!=StampCache.readSrc(getUrlStr(), wholeStamp.isNumeric)) {
    		return true;
    	}
    	    	    	
    	return false;
    }
    
    public BufferedImage image = null; 
    
	public BufferedImage loadSrcImage()
	{
	    try {
	    	if (image!=null) return image;
	    	
	    	boolean numeric = wholeStamp.isNumeric;
	    	
        	String cacheLoc=getUrlStr();
        	        	        	
        	image = StampCache.readSrc(getUrlStr(), numeric);
        	        	
        	if (image==null) {             
        		image = StampImageFactory.loadImage(cacheLoc, numeric);
        		if (image!=null) {
        			StampCache.writeSrc(image, cacheLoc);
        		}

                // Skipping this step makes non-raster images look washed out
        		if (!numeric && image!=null && image.getAlphaRaster()==null) {
        			image = Util.makeBufferedImage(image);
        		}
        	} 
	    }
	    catch (Throwable e) {
	        log.aprintln(e);
	    }
	    
	    return image;
	}
	
	 /**
	 * Creates a copy of the given Graphics2D and prepares it for rendering
	 * frames of this image type.
	 */    
    protected final java.awt.Graphics2D getFrameG2(Graphics2D g2) {
    	g2 = (Graphics2D) g2.create();

    	if (wholeStamp.clip_to_path) {
    		List<Area> areas = wholeStamp.myStamp.getFillAreas();

    		Area clipArea = new Area();
    		
    		for (Area area : areas) {
    			clipArea.add(StampImage.getAdjustedClipArea(area));
    		}
    		
    		g2.setClip(clipArea);
    	}
    	
    	return g2;
    }
	
	// Draw this frame onto the specified g2.  Draw it multiple times if
    // necessary due to worldwrap.  (How often are we really going to be
    // zoomed out enough to actually worry about this for stamps?)
    public void drawFrame(ImageFrame frame, Rectangle2D worldWin, Graphics2D wg2, FilledStampImageType fs, BufferedImage target, final StampLView.DrawFilledRequest request) {
        final double base = Math.floor(worldWin.getMinX() / 360.0) * 360.0;
        
        final int numWorldSegments = (int) Math.ceil (worldWin.getMaxX() / 360.0) - (int) Math.floor(worldWin.getMinX() / 360.0);
        
        Rectangle2D.Double where = new Rectangle2D.Double();
        
        where.setRect(frame.cell.getWorldBounds());
        
        double origX = where.x;
        
        int start = where.getMaxX() < 360.0 ? 0 : -1;
        
        for(int m=start; m<numWorldSegments; m++) {
            where.x = origX + base + 360.0*m;
            if(worldWin.intersects(where)) {
            	Graphics2D g2 = getFrameG2(wg2);
            	
        		int screenWidth = (int)(worldWin.getWidth()*renderPPD);
        		int screenHeight = (int)(worldWin.getHeight()*renderPPD);
        		
        		int dstW = (int) Math.ceil(where.getWidth()  * renderPPD);
        		int dstH = (int) Math.ceil(where.getHeight() * renderPPD);
        		
        		Rectangle2D regionToProject = null;
        		
        		// TODO: The current stamp projection mechanism is inefficient in that cell bounds get expanded to rectangles, which ultimately overlap
        		// with each other.  These overlapping areas then get processed for each frame, resulting in unnecessary work.  In some cases this excess
        		// work is trivial, in others is can be substantial.  Map layer style tiling may be appropriate, but it still has to be done on an image
        		// by image basis, taking individual image offsets into consideration, as the entire stamp layer is very dynamic in nature.
        		
        		int frame_limit = (int) Math.max(screenWidth, screenHeight)*4;   // This is completely arbitrary and should probably be tuned. 
        		
        		BufferedImage image;
        		        		
        		if (dstW > frame_limit || dstH > frame_limit) {
        			// When we're zoomed really far into a low resolution image (which can happen intentionally or inadvertently when multiple datasets 
        			// are in use), the buffered image for the frame can become gigantic, even though the user only sees a tiny fraction of it.
        			// Just project the bounds of the screen, as it's going to be significantly smaller than a frame
        			regionToProject = worldWin;  
        			image = frame.getProjectedImage(worldWin);
        		} else {
        			image = frame.getProjectedImage();
        			regionToProject = where;
        		}
        		
        		if (request.changed()) return;
        		
        		g2.transform(Util.image2world(image.getWidth(), image.getHeight(), regionToProject));
            	
				if (target!=null) {
					Point2D src = new Point2D.Float();
					Point2D dst = new Point2D.Float();
					
					AffineTransform at=null;
					
					try {
						at = g2.getTransform().createInverse();
					} catch (NoninvertibleTransformException e) {
						e.printStackTrace();
					}
					
					int width=target.getWidth();
					int height=target.getHeight();
					
					for (int i=0; i<width;i++) {
						for (int j=0; j<height;j++) {
							dst.setLocation(i, j);
							at.transform(dst, src);
														
							double srcX = src.getX();
							double srcY = src.getY();
							
							if (srcX<0 && srcX>-0.5) srcX=0;
							if (srcY<0 && srcY>-0.5) srcY=0;
							
							if (srcX>=image.getWidth() && srcX<image.getWidth()+0.5) srcX=image.getWidth()-1;
							if (srcY>=image.getHeight() && srcY<image.getHeight()+0.5) srcY=image.getHeight();
							
							if (srcX>=image.getWidth()) continue;
							if (srcY>=image.getHeight()) continue;
							if (srcX<0) continue;
							if (srcY<0) continue;

							try {
								double sample = image.getRaster().getSampleDouble((int)Math.floor(srcX), (int)Math.floor(srcY), 0);
									
								if (sample<-100000) sample=wholeStamp.IGNORE_VALUE;
									
								if (Double.isNaN(sample)) sample=wholeStamp.IGNORE_VALUE;

								if (sample==wholeStamp.IGNORE_VALUE) {
									// In the case where we're drawing overlapping stamps, do NOT replace actual data from a previous stamp 
									// with transparent pixels from this one
									if (target.getRaster().getSample(i, j, 0)!=0) continue;
								}
								target.getRaster().setSample(i, j, 0, sample);
							} catch (Exception e3) {
								System.out.println("OUT OF BOUNDS: src.getX() " + src.getX() + " : " + src.getY());
								continue;
							}
						}
					}
				} else {
					if (request.changed()) return;
					
					if (wholeStamp.isNumeric) {
						try {
							BufferedImage image3 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

							// Ignore all black pixels for numeric stamp data
							Graphics2D g3 = image3.createGraphics();
							g3.setComposite(new IgnoreComposite(Color.black));
							
							
							BufferedImageOp op2;
							if (wholeStamp.myStamp.stampLayer.spectraPerPixel()) {
								// Filter so we see the areas that have been selected for spectra per pixels
								op2 = new LineSampleSpectraImageOp(wholeStamp);
							} else {
								// Filter using the FloatingPointOp first
				                op2 = new FloatingPointImageOp(wholeStamp);														
							}

							g3.drawImage(image, op2, 0, 0);

							BufferedImageOp op = fs.getColorMapOp(image3).forAlpha(1);

							//perform the filter transform
							op.filter(image3, image3);

							//get the original numeric data
							WritableRaster numVals = image.getRaster();
							
							//get the new alpha raster
							WritableRaster alpha = image3.getAlphaRaster();
							
							//replace any transparent values from the src one
							for (int j = image3.getHeight()-1; j>=0; j--) {
								for (int i = image3.getWidth()-1; i>=0; i--) {

									int[] alphaVal = new int[1];
									//get the numeric value from the original image
									double orgVal = numVals.getSampleDouble(i, j, 0);
									
									//if it's NaN or IGNORE_VALUE, then it's supposed to be transparent
									if(Double.isNaN(orgVal) || orgVal == wholeStamp.IGNORE_VALUE){
										alphaVal[0] = 0;
										//reset the alpha value
										alpha.setPixel(i, j, alphaVal);
									}
								}
							}
							
							//draw the image
							g2.drawImage(image3, null, 0, 0);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
				    	if (wholeStamp.ignore_value!=-1) {
							BufferedImage image2 = Util.createCompatibleImage(image, image.getWidth(), image.getHeight());
							
							Graphics2D g3 = image2.createGraphics();
							g3.setComposite(new IgnoreComposite(Color.black));
							g3.drawImage(image, null, 0, 0);
							image = image2;
				    	}

				    	// Create a TYPE_INT_ARGB image and draw our image into it so we can be sure of what color model we're working with
				    	// TODO: If we're already the right type of image, skip this step
						BufferedImage tmpInputImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
				    	tmpInputImage.createGraphics().drawImage(image, null,  0, 0);
				    	image = tmpInputImage;

				    	// Our color map can handle images with different band ordering, but doesn't know how to convert between colorspaces.
				    	// As a result, we need to be sure to specify the same image type before and after this step.
						BufferedImageOp op = fs.getColorMapOp(image).forAlpha(1);
						
						//transform the image into a new copy (image2) so the original
				    	// does not change (don't want to alter the original)
				    	BufferedImage image2 = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
				    	op.filter(image, image2);
				    	//get the new and original alpha rasters
						WritableRaster alpha = image2.getAlphaRaster();
						WritableRaster orgAlpha = image.getAlphaRaster();
						
						//replace any transparent values based on the source image
						for (int j = image2.getHeight()-1; j>=0; j--) {
							for (int i = image2.getWidth()-1; i>=0; i--) {
								int orgAlphaVal = orgAlpha.getSample(i, j, 0);
								
								int[] alphaVal = new int[1];
								//get the numeric value from the original image
								double orgVal = image.getRaster().getSampleDouble(i, j, 0);
								
								//if it has an ignore val then pixels from the original 
								// image with a value of 0 are supposed to be transparent
								// OR if the orignal image has transparent pixels, keep 
								// them transparent (this happens when the orignal images
								// come back to hide the rest of an unused frame)
								if((orgVal == 0 && wholeStamp.ignore_value !=-1)|| orgAlphaVal == 0){
									alphaVal[0] = 0;
									//reset the alpha value
									alpha.setPixel(i, j, alphaVal);
								}
							}
						}

						g2.drawImage(image2, null, 0, 0);
					}					
				}
            }
        }                    	
    }

}