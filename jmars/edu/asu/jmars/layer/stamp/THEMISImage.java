package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

import edu.asu.jmars.util.DebugLog;


/**
 * Supports loading THEMIS DCS stamps
 */
public class THEMISImage extends StampImage
{
	RandomAccessFile raf;
    
    protected THEMISImage(StampShape s, File cacheFile, String newFilename, String imageType, String newInstrument, HashMap<String,String> projectionParams)
    throws IOException
    {
    	super(s, s.getId(), "themis", imageType, null, projectionParams);
    	
    	raf = new RandomAccessFile(cacheFile, "r");
    	
    	instrument=newInstrument;
    	productID=newFilename;
                
        label = null;        
    }
                        
    public Point2D[] getPoints()
    {
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
	    	    
	    	    ois.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
	        
	        // Take the midpoint of the border pixels, to make sure that
	        // each frame butts up exactly against the next. NOT FOR VISIBLE!
	        if (!productID.startsWith("V"))
	            for(int i=0; i<pts.length-4; i+=4) {
	                pts[i+0] = pts[i+6] = midpoint(pts[i+0], pts[i+6]);
	                pts[i+1] = pts[i+7] = midpoint(pts[i+1], pts[i+7]);
	            }
    	}
    	    	
        return  pts;
    }
            
    ImageInputStream iis = null;
    ImageReader reader = null;
    public void fillImage(BufferedImage dstImage, int xsrcArray[][], int ysrcArray[][]) {
    	try {
    		if (iis==null) {
    			iis = ImageIO.createImageInputStream(raf);
    			Iterator readers = ImageIO.getImageReaders(iis);
    			reader = (ImageReader)readers.next();
				
    			reader.setInput(iis, true);
    		}
			
			Rectangle rect = new Rectangle();
			ImageReadParam param = reader.getDefaultReadParam();

			int minx=Integer.MAX_VALUE;
			int miny=Integer.MAX_VALUE;
			int maxx=Integer.MIN_VALUE;
			int maxy=Integer.MIN_VALUE;
			
	    	for (int i=0; i<xsrcArray.length; i++) {
	    		for (int j=0; j<xsrcArray[0].length; j++) {
	    			int xval = xsrcArray[i][j];
	    			int yval = ysrcArray[i][j];
	    			
	    			if (minx>xval) minx=xval;
	    			if (maxx<xval) maxx=xval;
	    			if (miny>yval) miny=yval;
	    			if (maxy<yval) maxy=yval;
	    		}
	    	}
		    	
	    	if (minx<0) minx=0;
	    	if (miny<0) miny=0;
	    	
	    	rect.setBounds(minx, miny, maxx-minx+1, maxy-miny+1);

			param.setSourceRegion(rect);
			
			BufferedImage bi=reader.read(0, param);

	    	for (int i=0; i<xsrcArray.length; i++) {
	    		for (int j=0; j<xsrcArray[0].length; j++) {
	    			int xval = xsrcArray[i][j];
	    			int yval = ysrcArray[i][j];
	    			
	    			if (xval<0 || yval <0) {
	    				continue;
	    			}
	    			
	    			if (xval>=getWidth() || yval>=getHeight()) {
	    				continue;
	    			}
	    			
	    			dstImage.setRGB(i, j, bi.getRGB(xval-minx,yval-miny));
	    		}
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
        
    /**
     * Returns true if the entire image is stored locally at full resolution in a single file.
     * Returns false by default, and if we are working with individual subsampled tiles of an image.
     * @return
     */
    public boolean isFullImageLocal() {
    	return true;
    }
    
    protected double getMaxRenderPPD()
    {
        return 593;
    }
        
}
