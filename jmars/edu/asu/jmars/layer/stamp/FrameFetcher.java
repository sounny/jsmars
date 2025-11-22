package edu.asu.jmars.layer.stamp;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import edu.asu.jmars.Main;

public class FrameFetcher {

	int xoffset;
	long yoffset;
	
	int width=0;
	int height=0;
	
	int numXFrames;
	int numYFrames;
	
	ImageFrame frames[][];
		
	String instrument;
	String imageType;
	String id;
	
	public FrameFetcher(ImageFrame framesToFill[][]) {
		ImageFrame firstFrame = framesToFill[0][0];
		
		this.xoffset=firstFrame.startx;
		this.yoffset=firstFrame.starty;

		StampImage srcImage = firstFrame.wholeStamp;
		
		instrument = srcImage.getInstrument();
		imageType = srcImage.imageType;
		id = srcImage.myStamp.getId();
		
		frames=framesToFill;
    	
		this.numXFrames=framesToFill.length;
		this.numYFrames=framesToFill[0].length;
		
		for (int i=0; i<numXFrames; i++) {
			width+=framesToFill[i][0].getWidth();
		}
		
		for (int i=0; i<numYFrames; i++) {
			height+=framesToFill[0][i].getHeight();
		}
	
	}
	
	public void fetchFrames() {
		int scale = Main.testDriver.mainWindow.getZoomManager().getZoomPPD();

    	String urlStr="ImageServer?instrument="+instrument+"&id="+id;
    	
    	if (imageType!=null && imageType.length()>=0) {
    		urlStr+="&imageType="+imageType;
    	}
    	
    	urlStr+="&zoom="+scale;
    	urlStr+="&startx="+xoffset;
    	urlStr+="&starty="+yoffset;    	    	
   		urlStr+="&height="+height;    	
    	urlStr+="&width="+width;
    	    	
    	boolean numeric = frames[0][0].wholeStamp.isNumeric;
    	
		BufferedImage bigImage = StampImageFactory.loadImage(urlStr, numeric);

		if (bigImage==null) return;
		
    	int realWidth = bigImage.getWidth();

    	int fullResWidth = 0;
    	
    	for (int i=0; i<numXFrames; i++) {
    		fullResWidth+=frames[i][0].getWidth();
    	}

    	double ratio = realWidth * 1.0 / fullResWidth;
    	
	    int subX = 0;
	    int subY = 0;
	    
		for (int y=0; y<numYFrames; y++) {
			for (int x=0; x<numXFrames; x++) {
				BufferedImage subImage;
				
				ImageFrame frame=frames[x][y];

				int width = (int)Math.round(frame.getWidth()*ratio);
				
				// This can occur when zoomed way out with large images, ie HiRISE at 2 ppd
				if (width>bigImage.getWidth()) {
					width=bigImage.getWidth();
				}
				
				int height = (int)(frame.getHeight()*ratio);
				
				if (height>bigImage.getHeight()) {
					height=bigImage.getHeight();
				}
				
				subImage = bigImage.getSubimage(subX, subY, width, height);
				frame.image=subImage;

				// Save the image asynchronously to disk
				StampCache.writeSrc(subImage, frame.getUrlStr());
				
				subX+=frame.getWidth()*ratio;
			}
			subX=0;
			subY+=frames[0][y].getHeight()*ratio;
		}
	}
	
    public static boolean frameNeedsLoading(ImageFrame f, ImageFrame leftF, ImageFrame rightF, ImageFrame topF, ImageFrame botF, Rectangle2D worldWin, boolean done, boolean expand) {
    	if (done) return false;
    	
    	boolean intersect = false;
    	if (!expand) {
    		intersect = StampImage.doesFrameIntersect(f, worldWin);
    	} else {
    		// If expand==true, load this frame if it intersects worldWin or if any of its neighboring tiles intersect worldWin
    		intersect = StampImage.doesFrameIntersect(f, worldWin) ||
    		   StampImage.doesFrameIntersect(leftF, worldWin) ||
    		   StampImage.doesFrameIntersect(rightF, worldWin) ||
    		   StampImage.doesFrameIntersect(topF, worldWin) ||
    		   StampImage.doesFrameIntersect(botF, worldWin);
    	}
    	
    	if (intersect) {
    		// Do this check last, because it requires disk IO and is thus somewhat expensive
        	if (f.hasImageLocally()) return false;    		
    	} 
    	
    	return false;
    }
    
    /*
     * Given an array of frames, representing a two dimensional grid xcnt by ycnt in side, and a worldWin representing the area in view,
     * attempt to form a small number of rectangular segments to minimize the number of requests that need to be made to the backend server.
     * 
     * The expand parameter is used to determine whether neighboring grids should be included or not.  Projected images frequently need
     * neighboring grid tiles in order to completely project a tile in view.  Hence in these cases, we expand the set of grids needing to be
     * retrieved to include any grid that is next to a grid that is in view as well.
     */
    public static ImageFrame[][][] segment(ImageFrame frames[], int xcnt, int ycnt, Rectangle2D worldWin, boolean expand) {    	
    	ImageFrame newSegments[][][]=null;
    	
    	List<ImageFrame[][]> segmentList = new ArrayList<ImageFrame[][]>();
    	
    	ImageFrame allFrames[][] = new ImageFrame[xcnt][ycnt];

    	int cnt=0;

    	// Create a 2 dimensional array to make it easier to work with
    	for (int y=0; y<ycnt; y++) {
    		for (int x=0; x<xcnt; x++) {
    			allFrames[x][y]=frames[cnt++];
    		}
    	}

    	boolean done[][]=new boolean[xcnt][ycnt];
    	
    	for (int y=0; y<ycnt; y++) {
    		for (int x=0; x<xcnt; x++) {
    			done[x][y]=false;
    		}
    	}

    	for (int y=0; y<ycnt; y++) {
    		for (int x=0; x<xcnt; x++) {
    			ImageFrame f = allFrames[x][y];
    			ImageFrame leftF = (x-1>=0) ? allFrames[x-1][y] : null;
    			ImageFrame rightF = (x+1<xcnt) ? allFrames[x+1][y] : null;
    			ImageFrame topF = (y-1>=0) ? allFrames[x][y-1] : null;
    			ImageFrame botF = (y+1<ycnt) ? allFrames[x][y+1] : null;
    			    			
    			if (!frameNeedsLoading(f, leftF, rightF, topF, botF, worldWin, done[x][y], expand)) {
    				done[x][y]=true; // make sure if it's local it's marked as done
    				continue;
    			}
    			
    			int xadditional=0;
    			int yadditional=0;
    			
    			for (int x2=x+1; x2<xcnt; x2++) {
    				ImageFrame f2 = allFrames[x2][y];
        			ImageFrame leftF2 = (x2-1>=0) ? allFrames[x2-1][y] : null;
        			ImageFrame rightF2 = (x2+1<xcnt) ? allFrames[x2+1][y] : null;
        			ImageFrame topF2 = (y-1>=0) ? allFrames[x2][y-1] : null;
        			ImageFrame botF2 = (y+1<ycnt) ? allFrames[x2][y+1] : null;
        			
    				if (frameNeedsLoading(f2, leftF2, rightF2, topF2, botF2, worldWin, done[x2][y], expand)) {
    					xadditional++;
    				} else {
    					break;
    				}
    			}
    			
    			boolean stillMatching=true;
    			for (int y2=y+1; y2<ycnt; y2++) {
    				if (!stillMatching) {
    					break;
    				}
    				
        			for (int x2=x; x2<=x+xadditional; x2++) {
        				ImageFrame f2 = allFrames[x2][y2];
            			ImageFrame leftF2 = (x2-1>=0) ? allFrames[x2-1][y2] : null;
            			ImageFrame rightF2 = (x2+1<xcnt) ? allFrames[x2+1][y2] : null;
            			ImageFrame topF2 = (y2-1>=0) ? allFrames[x2][y2-1] : null;
            			ImageFrame botF2 = (y2+1<ycnt) ? allFrames[x2][y2+1] : null;
        				if (!(frameNeedsLoading(f2, leftF2, rightF2, topF2, botF2, worldWin, done[x2][y2], expand))) {
        					stillMatching=false;
        					break;
        				} 
        			}
        			if (stillMatching) yadditional++;
    			}
    			
    			ImageFrame newSegment[][]=new ImageFrame[1+xadditional][1+yadditional];
    			
    			for (int y2=0; y2<(1+yadditional); y2++) {
    				for (int x2=0; x2<(1+xadditional); x2++) {
    					newSegment[x2][y2]=allFrames[x+x2][y+y2];
    					done[x+x2][y+y2]=true;
    				}
    			}
    		
    			segmentList.add(newSegment);    		
    		}
    	}
    	
    	newSegments = new ImageFrame[segmentList.size()][][];
    	
    	newSegments = segmentList.toArray(newSegments);    	
    	
    	return newSegments;
    }
    
    
}
