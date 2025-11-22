package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.stamp.projection.Unprojected;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;


/**
 * Supports loading ABR, BTR, and PBT versions of THEMIS IR/VIS stamps
 * 
 * @see edu.asu.jmars.layer.stamp.PdsImageFactory
 */
public class PdsImage extends StampImage
{
    private static final DebugLog log = DebugLog.instance();
    
	RandomAccessFile raf;
    protected int imageBytes;
    protected int sampleCount;
    protected int lineCount;
    protected int qubeOffset;
    protected int spatialSumming;
    
    private static final int BUFF_SIZE = 40960;
    
    protected int imageOffset;
    protected double dataScaleOffset;
    protected double dataScaleFactor;
    
    public int nullConstant=Integer.MIN_VALUE;
    
    protected String sampleType;
    protected int sampleBits;
    
    // Used for Sigma Stretching of ABR images
    double avg=Double.NaN;
    double stdDev=Double.NaN;
    boolean performSigmaStretch=false;
    
    protected PdsImage(StampShape s, File cacheFile, String newFilename, String imageType, String newInstrument, HashMap<String,String> projectionParams)
    throws IOException
    {
    	super(s, s.getId(), newInstrument, imageType, null, projectionParams);
    	
    	raf = new RandomAccessFile(cacheFile, "r");
    	
    	instrument=newInstrument;
    	productID=newFilename;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[BUFF_SIZE];
        
        FileInputStream fin = new FileInputStream(cacheFile);
        
        // Read the entire file into a memory buffer
        try
        {
            int count;
            while((count = fin.read(temp)) >= 0)
                buffer.write(temp, 0, count);
            
            fin.close();
        }
        catch(IOException e)
        {
            log.aprintln("IO error while reading PDS BTR image.");
            throw  e;
        }
        
        // The entire data buffer
        byte[] dataBuffer;
        dataBuffer = buffer.toByteArray();
        
        // The sizes of things
        int recordBytes;
        int labelRecords;
        // Determine just the label, as a big string
        String labelStart = new String(dataBuffer, 0, 1000);
        
        recordBytes = intValue(labelStart, "RECORD_BYTES");
        labelRecords = intValue(labelStart, "LABEL_RECORDS");
        label = new String(dataBuffer, 0, recordBytes * labelRecords).trim();
        
        // Determine where image data starts
        int imageRecords = intValue(label, "^IMAGE");
        imageOffset = (imageRecords-1) * recordBytes;
        
        // Determine the dimensions of the image data
        sampleCount = intValue(label, "LINE_SAMPLES");
        lineCount = intValue(label, "LINES");
        // bandNumber = intValue(label, "BAND_NUMBER");
        
        // Determine the datatype and size of the image data
        sampleType = strValue(label, "SAMPLE_TYPE");
        sampleBits = intValue(label, "SAMPLE_BITS");
                
        // Determine data scaling factors for IR images only,
        // not for VIS. -- except for the new ALB product
        if (productID.startsWith("I") || imageType.startsWith("ALB"))
        {
            dataScaleOffset = doubleValue(label, "   OFFSET");
            dataScaleFactor = doubleValue(label, "SCALING_FACTOR");
            if (label.contains("MINIMUM_BRIGHTNESS_TEMPERATURE")) {   // BTR, PBT
            	minValue = doubleValue(label, "MINIMUM_BRIGHTNESS_TEMPERATURE");
            } else if (label.contains("MINIMUM_ALBEDO")) {
            	minValue = doubleValue(label, "MINIMUM_ALBEDO");            	
            }
            if (label.contains("MAXIMUM_BRIGHTNESS_TEMPERATURE")) {
            	maxValue = doubleValue(label, "MAXIMUM_BRIGHTNESS_TEMPERATURE");
            } else if (label.contains("MAXIMUM_ALBEDO")) {
            	maxValue = doubleValue(label, "MAXIMUM_ALBEDO");            	
            }
            if (label.contains("NULL_CONSTANT")) {
            	nullConstant = intValue(label, "NULL_CONSTANT");
            }
        }
        
        if (productID.startsWith("V"))
            spatialSumming = intValue(label, "SPATIAL_SUMMING");
        else
            spatialSumming = 0;
                
        imageBytes = sampleCount * lineCount;
        
        if (productID.startsWith("V") && imageType.startsWith("ABR")) {
        	performSigmaStretch=true;
        	
			long sum = 0;
			
			for(int i=0; i<imageBytes; i++){
				int val = dataBuffer[imageOffset+i]&0xFF;
				sum += val;
			}

			avg = (sum*1.0) / imageBytes;
				
			double topSum = 0;
			
			for(int i=0; i<imageBytes; i++){
				int pixelVal = dataBuffer[imageOffset+i]&0xFF;
				topSum += Math.pow(pixelVal-avg, 2);
			}
			
			stdDev = Math.sqrt(topSum / imageBytes);
        }
        
        // TODO: What is the implication of having to do this again in here?
        // We don't know numLines and numSamples before parsing this in the constructor above
        // Shouldn't be...
        if (imageProjection==null) {
        	imageProjection = new Unprojected(getNumLines(), getNumSamples(), pts[pts.length-2], pts[pts.length-1], pts[0], pts[1]);
        }
    }

    /**
     * Override to indicate that we have the entire image locally, not just subscampled frames.
     */
    public boolean isFullImageLocal() {
    	return true;
    }
    
    protected static String strValue(String lines, String key)
    {
        int start = lines.indexOf(key);
//        if (start == -1) start = lines.indexOf("\n" + needle);
//        if (start == -1) start = lines.indexOf("\t" + needle);
//        if (start == -1) start = lines.indexOf(" " + needle);
        if (start == -1) throw  new Error("Can't find key " + key);
        
        start += key.length();
        
        int end = lines.indexOf("=", start+1);
        
        if (end == -1)
            throw  new Error("Can't find end of key " + key);
        
        start=end+1;
        
        end = lines.indexOf("\n", start+1);
        if (end == -1)
            throw  new Error("Can't find end of key " + key);
        
        try {
            String val = lines.substring(start, end);
            if (val.startsWith("\"")) {
            	val=val.substring(1);
            }
            return  val.trim();
        }
        catch(RuntimeException e) {
            log.aprintln("Problem returning key " + key);
            log.aprintln("start = " + start);
            log.aprintln("end = " + end);
            log.aprintln("lines.length() = " + lines.length());
            throw  e;
        }
    }
    
    
    protected static int intValue(String lines, String key)
    {
        String val = strValue(lines, key);
        try {
            return  Integer.parseInt( val );
        }
        catch(NumberFormatException e) {
            log.println("Unable to decipher " + key +
                        " = '" + val + "'");
            throw  e;
        }
    }
    
    protected static double doubleValue(String lines, String key)
    {
        String val = strValue(lines, key);
        try {
            return  Double.parseDouble( val );
        }
        catch(NumberFormatException e) {
            log.println("Unable to decipher " + key +
                        " = '" + val + "'");
            throw  e;
        }
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
            
    public int getHeight()
    {
    	return lineCount;
    }
 
    public int getWidth()
    {
    	return sampleCount;
    }

    public int getNumLines() {
    	return lineCount;
    }
    
    public int getNumSamples() {
    	return sampleCount;
    }

    // Returns 24-bit RGB color value at specified pixel location; no alpha
    // component.
    public int getRGB(int x, int y)	throws Exception
    {
		if (x >= sampleCount || y >= lineCount || x < 0 || y < 0)
		    throw new Exception("Invalid location: x=" + x + " y=" + y);
	
		long pos = x + y * sampleCount + imageOffset;
		raf.seek(pos);
		int b = raf.readByte() & 0xFF;
		
		if (performSigmaStretch) {
			double variance=40;
			int newVal = (int)((b-avg)*(variance/stdDev))+127;
			
			if (newVal<0) newVal=0;
			if (newVal>255) newVal=255;
			
			b=newVal;
		}
		
		return new Color(b, b, b).getRGB();
    }
            
    protected double getMaxRenderPPD()
    {
        if (productID == null)
            return 512.0;
        else
            return productID.startsWith("V") ? 2048/spatialSumming : 512;
    }
        
    private int histograms[][];
    public int[] getHistogram() throws IOException
    {
        if (histograms == null)
            histograms = new int[1][];
        int[] hist = histograms[0];
        
        if (hist == null) {
            final int offset = 0;
            
            // Create the histogram
            hist = histograms[0] = new int[256];
            for(int i=0; i<imageBytes; i++) {
            	raf.seek(i + offset);
                ++hist[ 0xFF & raf.readByte() ];
            }
            
            // Write to a file
            String filename = "band" + 0 + ".h";
            try {
                PrintStream fout = new PrintStream(
                                                   new FileOutputStream(filename));
                fout.println("# Histogram");
                for(int i=0; i<256; i++)
                    fout.println(i + "\t" + histograms[0][i]);
                fout.close();
                log.println("Wrote histogram to file: " + filename);
            }
            catch(Throwable e) {
                log.aprintln("Unable to write histogram file " + filename);
                log.println(e);
            }
        }
        
        return  (int[]) hist.clone();
    }
    
    /**
     * Returns temperature in degrees Kelvin for specified image
     * coordinates.
     *
     * Note: this method is only useful for BTR or PBT images.
     */
    public double getTemp(int x, int y)
    {
    	if (x<0 || y<0 || x>=sampleCount || y>=lineCount) return Double.NaN;

    	int dataSize = sampleBits/8;
    	
        int dataIndex = x*dataSize + sampleCount * y * dataSize + imageOffset;
        
        double pixelVal = 0;
        
        try {
        	raf.seek(dataIndex);

        	// Assume PC_REAL
        	if (dataSize > 1) {
        		byte bytes[] = new byte[dataSize];

        		raf.read(bytes);
        	        	
        		pixelVal = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        	} else { // Assume UNSIGNED_INTEGER
        		byte byteVal = raf.readByte();
        		
        		pixelVal = (int)(byteVal & 0xFF);
        	}

        } catch (IOException ioe) {
        	ioe.printStackTrace();
        	return Double.NaN;
        }

        if (pixelVal==nullConstant) {
        	return Double.NaN;
        }
        
        // Make sure in calculation below that 'pixelVal' is
        // effectively treated as an unsigned value, 0-255 in range.
        double temp = dataScaleFactor * pixelVal + dataScaleOffset;
        
        if (temp==0.0) {
        	return Double.NaN;
        }
        
        return temp;
    }
    
    /**
     * Returns temperature in degrees Kelvin for specified image
     * point; double/float coordinates are converted to integer by
     * simply dropping non-integer portion.
     *
     * Note: this method is only useful for BTR images and PBT images.
     */
    public double getTemp(HVector ptVect, ProjObj po, int renderPPD)
    {
        Point2D.Double imagePt = null;

        String key = po.getCenterLon()+":"+po.getCenterLat()+":"+renderPPD;
    	if (!cachedFrames.containsKey(key)) {
    		recreateImageFrames(renderPPD, po);
    	}
    	StampImageFrames imageFrames = cachedFrames.get(key);
        
        for (int i = 0; i < imageFrames.frames.length; i++)
        {
            Point2D.Double unitPt = new Point2D.Double();
            
            if (imageFrames.frames[i]==null || imageFrames.frames[i].cell==null) {
            	return Double.NaN; 
            }
            
            imageFrames.frames[i].cell.uninterpolate(ptVect, unitPt);
            
            // Check whether point falls within cell.
            if (unitPt.x >= 0  &&  unitPt.x <= 1  &&
                unitPt.y >= 0  &&  unitPt.y <= 1  )
            {
                imagePt = new Point2D.Double();
                imagePt.x = unitPt.x * imageFrames.frames[i].getWidth() + imageFrames.frames[i].getX();
                
                // Might need to be inverted, ie 1-blah
                imagePt.y = (1-unitPt.y) * imageFrames.frames[i].getHeight() + imageFrames.frames[i].getY();

                break;
            } 
        }
        
        if (imagePt == null) {
        	return Double.NaN;
        }
        
        return getTemp( (int)Math.floor(imagePt.getX()), (int)Math.floor(imagePt.getY()) );
    }
 
    
    
    
    
}
