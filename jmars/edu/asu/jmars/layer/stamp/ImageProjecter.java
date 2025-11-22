package edu.asu.jmars.layer.stamp;

import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferDouble;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.layer.stamp.projection.JMARSCylindrical;
import edu.asu.jmars.layer.stamp.projection.SimpleCylindrical;
import edu.asu.jmars.layer.stamp.projection.Projection;
import edu.asu.jmars.layer.stamp.projection.Unprojected;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;

public class ImageProjecter {

	ImageFrame frame;

	/*
	 * DO NOT CALL.  Used only to allow the GlobalDataReprojector subclass to function.
	 */
	protected ImageProjecter() {
		// DO NOT CALL
	}
	
	public ImageProjecter(ImageFrame frame, Rectangle2D worldBounds) {
		this.frame=frame;
		cell=frame.cell;

		where = worldBounds;

		renderPPD = frame.stampImageFrames.renderPPD;

		srcOffsetX=frame.startx;
		srcOffsetY=frame.starty;

		// Used to determine whether we are rendering from a single frame of a bigger image, rather
		// than from an entire image (such as is done with THEMIS BTR)
		String instrument = frame.wholeStamp.getInstrument();

		this.frameNum=frame.frameNum;

		isCTX=(instrument.equalsIgnoreCase("CTX"));

		// Set via one of two methods in ImageFrame;
		if (frame.wholeStamp.isFullImageLocal()) {
			srcImage = frame.wholeStamp.image;  // Maybe? For THEMIS BWS
			srcWidth=frame.width;
			srcHeight=frame.height;
		} else {
			srcImage = frame.loadSrcImage();

			if (srcImage==null) {
				srcWidth=0;
				srcHeight=0;
			} else {
				srcWidth=srcImage.getWidth();
				srcHeight=srcImage.getHeight();
			}
		}

		// Set from srcImage.getNumLines
		numSrcLines=frame.wholeStamp.getNumLines();
		numSrcSamples=frame.wholeStamp.getNumSamples();

		// Determine the size of the projected frame image
		dstW = (int) Math.ceil(where.getWidth()  * renderPPD);
		dstH = (int) Math.ceil(where.getHeight() * renderPPD);

		dstW--;
		dstH--;       

		// BufferedImage gets all cranky about images with 0 sized dimensions
		if (dstW<=0) {
			dstW=1;
		}

		if (dstH<=0) {
			dstH=1;
		}

		Projection_OC proj = (Projection_OC)frame.stampImageFrames.po;

		dstProj = new JMARSCylindrical(dstW, dstH, where, proj.getUp(), proj.getCenter(), renderPPD);

		this.proj = frame.wholeStamp.imageProjection;

	}

	Cell cell = null; // Not used in all cases.  How do we initialize?
	BufferedImage srcImage = null;
	BufferedImage dstImage = null;
	Rectangle2D where = null; // World Bounds
	int renderPPD = -1;  // from srcImage.renderPPD, when srcImage isn't a BufferedImage

	// Replacing srcRange.x and srcRange.y
	// These represent the offsets into a full source image, rather than a frame sized source
	int srcOffsetX=-1;
	int srcOffsetY=-1;

	// What frame number is this?  Note that this may (or may not) be the entire image
	int frameNum=-1;

	// Used to tell if we are dealing with a CTX image
	boolean isCTX=false;

	// Used to tell if we are dealing with a PDS IMG (only used for THEMIS BTR/PBT images) file
	boolean isPDS=false;

	// Set via one of two methods in ImageFrame;
	int srcWidth=-1;
	int srcHeight=-1;

	// Set from srcImage.getNumLines
	int numSrcLines=-1;
	int numSrcSamples=-1;

	// These get reused for each pixel.  Cheaper than creating and destroying an object every time
	private HVector spatialPt = new HVector();
	private Point srcPt = new Point();
	Point2D.Double unitPt = new Point2D.Double();

	private int dstW=-1;
	private int dstH=-1;

	HashMap<String, String> projParams = null;

	JMARSCylindrical dstProj = null;

	// TODO: Are we loading these into memory multiple times?
			// Can we point to the same memory being used by the ACTUAL frame these belong to?
	BufferedImage prevImage = null;
	BufferedImage nextImage = null;
	BufferedImage aboveImage = null;
	BufferedImage belowImage = null;

	// FillImage copied from THEMISImage... previously just used for DCS
	// For HiRISE, this doesn't seem to make a significant difference.
	// Pro of doing everything this way: Fewer code paths.
	public void fillImage(int xsrcArray[][], int ysrcArray[][]) {    	  
		try {
			for (int i=0; i<xsrcArray.length; i++) {
				for (int j=0; j<xsrcArray[0].length; j++) {
					int sample = xsrcArray[i][j];
					int line = ysrcArray[i][j];

					BufferedImage pixelSrc = srcImage;

					if (line<0 && sample<0) {
						continue;
					}

					if (frame.wholeStamp.isFullImageLocal()) {
						if (line<0 || sample<0) {
							continue;
						}

						try {
							if (frame.wholeStamp.isNumeric) {
								// TODO: How do we know for sure this is a PdsImage?  Could be davinci or other local image
								dstImage.getRaster().setSample(i, j, 0, ((PdsImage)frame.wholeStamp).getTemp(sample, line));
							} else {
								dstImage.setRGB(i, j, frame.wholeStamp.getRGB(sample, line));
							}
						} catch (Exception e) {
							//   	   	    				System.out.println("Bombed on line: " + line + "   sample = " + sample + " frame = " + frameNum);
						}
						continue;
					}    	    

					if (sample<0 && frameNum>0) {
						if (prevImage==null) {
							prevImage=frame.stampImageFrames.frames[frameNum-1].loadSrcImage();
						}

						pixelSrc=prevImage;
						sample+=pixelSrc.getWidth();
					} else if (line<0) {
						int aboveFrameNum = frameNum-frame.stampImageFrames.horizontalSplitCnt;

						if (aboveFrameNum>=0 && aboveImage==null) {
							aboveImage=frame.stampImageFrames.frames[aboveFrameNum].loadSrcImage();
						}

						if (aboveImage!=null) {
							pixelSrc=aboveImage;
							line+=pixelSrc.getHeight();
						}   	        			
					} else if (line>=pixelSrc.getHeight()) {
						int belowFrameNum = frameNum+frame.stampImageFrames.horizontalSplitCnt;

						// TODO: Better way to tell if this is too many frames
						if (belowFrameNum<frame.stampImageFrames.frames.length && belowImage==null) {
							belowImage=frame.stampImageFrames.frames[belowFrameNum].loadSrcImage();
						}

						if (belowImage!=null) {
							line-=pixelSrc.getHeight();
							pixelSrc=belowImage;
						}
					} else {   	        		
						boolean lastFrame=false;
						lastFrame = !(frame.stampImageFrames.frames.length > frameNum+1);

						if (sample>=pixelSrc.getWidth() && !lastFrame) {
							if (nextImage==null) {
								nextImage=frame.stampImageFrames.frames[frameNum+1].loadSrcImage();
							}

							sample-=srcImage.getWidth();
							pixelSrc=nextImage;
						}
					}

					if (sample>=pixelSrc.getWidth()) {
						continue;
					}

					// TODO: Is this reasonable??
					if (line==pixelSrc.getHeight()) {
						line--;
					}

					if (line>=pixelSrc.getHeight()) {
						continue;
					}

					try {

						if (isCTX) {
							double scale = -1;

							scale = renderPPD / frame.wholeStamp.getMaxRenderPPD();

							boolean firstFrameInRow=false;
							boolean lastFrameInRow=false;

							firstFrameInRow = frameNum%frame.stampImageFrames.horizontalSplitCnt==0;
							lastFrameInRow = frameNum%frame.stampImageFrames.horizontalSplitCnt==(frame.stampImageFrames.horizontalSplitCnt-1);

							if ( 
									( firstFrameInRow && sample < (46*scale)             ) || 
									( lastFrameInRow  && sample > (srcWidth-(30*scale))  )
							) {          				
								dstImage.setRGB(i,j,0);
							} else {
								dstImage.setRGB(i, j, pixelSrc.getRGB(sample, line));
							}
						} else {
							if (frame.wholeStamp.isNumeric) {
								double d = pixelSrc.getRaster().getSampleDouble(sample, line, 0);

								if (d<-1000000) {  // This will catch -Float.MAX_VALUE and similar values
									d= StampImage.IGNORE_VALUE;
								}

								if (d==-32768) {  // This is the same currently, but on the off chance StampImage.IGNORE_VALUE changes
									d = StampImage.IGNORE_VALUE;
								}
								
								dstImage.getRaster().setSample(i, j, 0, d);	
							} else {
								dstImage.setRGB(i, j, pixelSrc.getRGB(sample,line));
							}
						}	                		

					} catch (ArrayIndexOutOfBoundsException e) {
						//e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



	// Projection for images, not a JMARS ProjObj
	Projection proj = null;

	synchronized BufferedImage getProjectedImage()
	{
		// We create two two dimensional arrays to store indexes before we do pixel by pixel copying from source tile to destination tile
		// This method is necessary for THEMIS DCS which reads from a random access file, and we want to do all of the work at once.
		// It seems to work fine for other cases as well, so we do everything this way to limit the number of code paths.
		int xsrcCoords[][]=null;
		int ysrcCoords[][]=null;
				
		xsrcCoords = new int[dstW][dstH];
		ysrcCoords = new int[dstW][dstH];

		for (int i=0; i<dstW; i++) {
			for (int j=0; j<dstH; j++) {
				xsrcCoords[i][j] = -1;
				ysrcCoords[i][j] = -1;
			}
		}

//		System.out.println("### Creating new image: " + dstW + " : " + dstH);
		
		if (frame.wholeStamp.isNumeric) {
			SampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_DOUBLE, dstW, dstH, 1, dstW, new int[] {0});

			DataBuffer db = new DataBufferDouble(dstW * dstH);
			WritableRaster wr = Raster.createWritableRaster(sm, db, null);
			ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
			ColorModel cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_DOUBLE);
			dstImage = new BufferedImage(cm, wr, true, null);
			
			double[] ddata = new double[dstW*dstH];
			
			for (int i=0;i<ddata.length;i++) {
				ddata[i]=StampImage.IGNORE_VALUE;  // Initialize to ignore values.  Anything that isn't explicitly set will be transparent
			}
			
			dstImage.getRaster().setDataElements(0, 0, dstW, dstH, ddata);
		} else {
			dstImage = Util.newBufferedImage(dstW, dstH);			
		}

		if (dstImage == null) {
			// TODO: Is there any better way to recover from this?
					System.out.println("out of memory");
		return null;
		}

		// These variable are used and reused by the for loop below
		Point2D lonLat = new Point2D.Double();
		Point2D lineSample = new Point2D.Double();

		// For cylindrical case
		ProjObj po = new ProjObj.Projection_OC(0,0);
		HVector up = ((Projection_OC)po).getUp();
		HVector center = ((Projection_OC)po).getCenter();
		HVector centerXUp = center.cross(up);
		HVector temp = new HVector();
		cell.buildGrid(srcWidth, srcHeight);

		// i and j are essentially lines and samples for our destination image.
		// We're going through and for each line and sample, getting the lat/lon value, then converting that to line/sample in the 
		// source image
		for(int j=0; j<dstH; j++) {
			for(int i=0; i<dstW; i++) {

				if (proj instanceof Unprojected) {
					projectUnprojected(j, i);
				} else if (proj instanceof SimpleCylindrical) {
					projectedSimpleCylindrical(up, temp, center, centerXUp, j, i);
				} else { // Projected
					projectProjected(lonLat, lineSample, j, i);
				} 

				// Save the indices for now, we'll copy them from src to dest en masse later
				xsrcCoords[i][j]=srcPt.x;
				ysrcCoords[i][j]=srcPt.y;
			}
		}

		// Used for THEMIS DCS only
		if (frame.wholeStamp instanceof THEMISImage) {
			((THEMISImage)frame.wholeStamp).fillImage(dstImage, xsrcCoords, ysrcCoords);
		} else {
			fillImage(xsrcCoords, ysrcCoords);
		}

		return dstImage;
	}

	private void projectUnprojected(int j, int i) {
		srcPt.setLocation(-1, -1);
		spatialPt = dstProj.spatialPt(j, i);
		
		// Uninterpolate from spatial coordinates to the unit
		// square.

		// Change to call uninterpolateFast, which saves about 
		// 400 ms per 320,000 pixel tile.  This call is still responsible
		// for about 130 of the 330 remaining ms per tile.
		cell.uninterpolateFast(spatialPt, unitPt);

		double X_ZERO = -0.5 / dstW;
		double Y_ZERO = -0.5 / dstH;
		double X_ONE = 1 + 0.5 / dstW;
		double Y_ONE = 1 + 0.5 / dstH;

		if(unitPt.x < 0)
			if(unitPt.x >= X_ZERO)
				unitPt.x = 0;
			else
				return;
		else if(unitPt.x > 1)
			if(unitPt.x <= X_ONE)
				unitPt.x = 1;
			else
				return;

		if(unitPt.y < 0)
			if(unitPt.y >= Y_ZERO)
				unitPt.y = 0;
			else
				return;
		else if(unitPt.y > 1)
			if(unitPt.y <= Y_ONE)
				unitPt.y = 1;
			else
				return;
		
		// Finally, convert from unit square coordinates to
		// source image pixel coordinates.
		if (frame.wholeStamp.isFullImageLocal()) {
			// Our source is an entire image, not just a frame piece, so we have to add in the offset of this fragment as well
			srcPt.setLocation(
					(int)Math.floor((   unitPt.x *(srcWidth) ) + srcOffsetX),
					(int)Math.floor(((1-unitPt.y)*(srcHeight)) + srcOffsetY));                	
		} else {
			// height = frame height                	
			srcPt.setLocation(
					(int)Math.floor(   unitPt.x *(srcWidth) ) ,
					(int)Math.floor((1-unitPt.y)*(srcHeight)) );	      	               			
		}		                
	}

	private void projectedSimpleCylindrical(HVector up, HVector temp, HVector center, HVector centerXUp, int j, int i) {
		spatialPt = dstProj.spatialPt(j, i);

		// Unrolled logic from ProjObj, convSpatialToWorld to make running this much faster when running hundreds of thousands of times	                	
		double dotp = up.dot(spatialPt);

		temp.x = spatialPt.x - (up.x * dotp);
		temp.y = spatialPt.y - (up.y * dotp);
		temp.z = spatialPt.z - (up.z * dotp);

		double tx=0, ty=0;
		tx = temp.dot(center);
		ty = temp.dot(centerXUp);

		temp.x=tx;
		temp.y=ty;
		temp.z=0;

		double x = 0;

		if(temp.y > 0)
			x= Math.PI * 2 - Math.atan2(temp.y, temp.x);
		else if(temp.y < 0)
			x= -Math.atan2(temp.y, temp.x);
		else if(temp.x < 0)
			x= Math.PI;
		else
			x= 0;

		double y = 0;

		double dp = up.dot(spatialPt);  // doesn't create new objects, fine to use	        			

		if(dp > 0)
		{
			temp.x = up.x - spatialPt.x;
			temp.y = up.y - spatialPt.y;
			temp.z = up.z - spatialPt.z;

			double dxp = temp.norm();  // doesn't create new objects, fine to use

			y = 2 * Math.asin(dxp / 2);
		}
		else if(dp < 0)
		{
			temp.x = up.x+spatialPt.x;
			temp.y = up.y+spatialPt.y;
			temp.z = up.z+spatialPt.z;

			double dxp = temp.norm();
			y =Math.PI - 2 * Math.asin(dxp / 2);
		}
		else
			y = Math.PI / 2;

		y = Math.PI/2 - y;

		Point2D locPt = cell.findGridLoc(Math.toDegrees(x) % 360.0,Math.toDegrees(y));
		
		if (locPt.getX()==Integer.MIN_VALUE || locPt.getY()==Integer.MIN_VALUE) {
			return;
		}


		// If we have multiple cells for one image, it's possible to be outside the bounds of our corresponding image, but still in an area where
		// we should be able to determine how to correctly color a pixel.  This requires looking at adjacent cells and images, and becomes really
		// ugly.  The code below could benefit from some additional clean up, but it seems to work.
		// TODO: Tidy up
		int belowFrameNum = frameNum+frame.stampImageFrames.horizontalSplitCnt;
		int aboveFrameNum = frameNum-frame.stampImageFrames.horizontalSplitCnt;
		int leftFrameNum = frameNum-1;
		int rightFrameNum = frameNum+1;

		BufferedImage otherImage = null;

		int otherFrameNum = -1;

		boolean validFrame = false;

		boolean below=false;
		boolean above=false;
		boolean left=false;
		boolean right=false;

		if (locPt.getY()>=srcHeight) {
			otherFrameNum = belowFrameNum;
			if (belowFrameNum<frame.stampImageFrames.frames.length) {
				validFrame=true;
				below=true;
			}
		} else if (locPt.getY()==-1) {
			otherFrameNum = aboveFrameNum;
			if (aboveFrameNum>=0) {
				validFrame=true;
				above=true;
			}
		} else if (locPt.getX()==-1) {
			otherFrameNum = leftFrameNum;
			if (leftFrameNum>=0 && (frameNum % frame.stampImageFrames.horizontalSplitCnt)>0) {
				validFrame=true;
				left=true;
			}
		} else if (locPt.getX()==srcWidth) {
			otherFrameNum = rightFrameNum;
			if (rightFrameNum<frame.stampImageFrames.frames.length && !(otherFrameNum%frame.stampImageFrames.horizontalSplitCnt==0)) {
				validFrame=true;
				right=true;
			}
		}		                

		ImageFrame otherImageFrame = null;

		if (otherFrameNum>=0 && otherFrameNum<frame.stampImageFrames.frames.length) {
			otherImageFrame=frame.stampImageFrames.frames[otherFrameNum];
		}

		if (validFrame && otherImageFrame!=null) {
			int otherSrcHeight=-1;
			int otherSrcWidth =-1;

			if (frame.wholeStamp.isFullImageLocal()) {
				otherSrcHeight=otherImageFrame.getHeight();
				otherSrcWidth =otherImageFrame.getWidth();
			} else {
				otherImage = otherImageFrame.loadSrcImage();

				otherSrcHeight=otherImage.getHeight();
				otherSrcWidth =otherImage.getWidth();
			}

			otherImageFrame.cell.buildGrid(otherSrcWidth, otherSrcHeight);
			locPt=otherImageFrame.cell.findGridLoc(Math.toDegrees(x) % 360.0,Math.toDegrees(y));

			if (locPt.getY()>=otherSrcHeight) {
				locPt.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			} else if (locPt.getY()==-1) {
				locPt.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			} else if (locPt.getX()==-1) {
				locPt.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			} else if (locPt.getX()==otherSrcWidth) {
				locPt.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			} 
			else if (below) {
				locPt.setLocation(locPt.getX(), locPt.getY()+srcHeight);
			} else if (above) {
				locPt.setLocation(locPt.getX(), locPt.getY() - otherSrcHeight);
			} else if (left) {
				locPt.setLocation(locPt.getX() - otherSrcWidth, locPt.getY());
			} else if (right) {
				locPt.setLocation(srcWidth + locPt.getX(), locPt.getY());
			}		                
		} else {
			if (locPt.getY()>=srcHeight) {
				locPt.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			} else if (locPt.getY()==-1) {
				locPt.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			} else if (locPt.getX()==-1) {
				locPt.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			} else if (locPt.getX()==srcWidth) {
				locPt.setLocation(Integer.MIN_VALUE, Integer.MIN_VALUE);
			}
		}

		if (frame.wholeStamp.isFullImageLocal()) {
			if (locPt.getX()!=Integer.MIN_VALUE && locPt.getY()!=Integer.MIN_VALUE) {
				// Our source is an entire image, not just a frame piece, so we have to add in the offset of this fragment as well
				locPt.setLocation(locPt.getX() + srcOffsetX, locPt.getY()+srcOffsetY);
			} 
		}

		//	                	srcPt.setLocation(locPt);
		srcPt.setLocation(Math.floor(locPt.getX()), Math.floor(locPt.getY()));		
	}

	private void projectProjected(Point2D lonLat, Point2D lineSample, int j, int i) {
		srcPt.setLocation(-1, -1);
		lonLat=dstProj.lonLat(j, i, lonLat);

		double lat=lonLat.getY();
		double lon=lonLat.getX();

		lineSample=proj.lineSample(lon, lat, lineSample);

		double line = lineSample.getX();
		double sample = lineSample.getY();       			

		// These line and sample values are for the full resolution source image.  Obviously anything negative or larger than the 
		// total number of lines/samples is invalid
		if (sample<0 || line <0 || line>numSrcLines || sample>numSrcSamples) {
			// TODO: Print an error ?
			//	               			System.out.println("## Error! line = " + line + " and sample = " + sample);

			return;
		}

		if (!(frame.wholeStamp.isFullImageLocal())) {
			// Adjust line and sample so they are indexes into the current frame, not the entire image
			sample-=srcOffsetX;
			line-=srcOffsetY;   	

			// TODO: Which do we trust more, the map_resolution in the database, or the calculation of map_resolution by pixels/degrees?
			// Scale applies to the source image.  Frequently we will retrieve a scaled down version of the image to work with.  We never 
			//   retrieve the source image at anything higher than full resolution.
			double scale = renderPPD*100.0 / frame.wholeStamp.map_resolution;
			scale/=100;
			if (scale>1) scale = 1;

			// Adjust line and sample so that they are indexes into the scaled down version of our source image frame
			line=(line*scale);
			sample=(sample*scale);
		}

		srcPt.x=(int)Math.floor(sample);
		srcPt.y=(int)Math.floor(line);	        			
	}
	
	public double biggest(double x, double y, double z)
	{
		double m = Math.abs(x);
		if(m < Math.abs(y))m=Math.abs(y);
		if(m < Math.abs(z))m=Math.abs(z);
		return(m);
	}

}
