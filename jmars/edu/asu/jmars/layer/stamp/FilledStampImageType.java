package edu.asu.jmars.layer.stamp;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.swing.ColorMapOp;
import edu.asu.jmars.swing.ColorMapper;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;


public class FilledStampImageType extends FilledStamp {
	private static DebugLog log = DebugLog.instance();
    
    /** Offset in east-lon ocentric-lat */
    private Point2D offset = null;
    public ColorMapper.State colors = ColorMapper.State.DEFAULT;


    public FilledStampImageType(StampShape stamp, StampImage pdsi, FilledStamp.State state) {
    	super(stamp, pdsi, state);
		
		offset = loadOffset();
		
		if (state != null && (state instanceof FilledStampImageType.State || state instanceof FilledStamp.State)) {
			// Slightly bizarre means for restoring colors since our
		    // internal representation cannot be serialized.
		    ColorMapper mapper = new ColorMapper();
		    ByteArrayInputStream buf = null;
		    //this part is done becasue old sessions will have the colorMap on a FilledStamp.State object
		    // and new sessions will have it on a proper FilledStampImageType.State object
		    if(state instanceof FilledStampImageType.State){
		    	buf = new ByteArrayInputStream(((FilledStampImageType.State)state).colorMap);
		    }else{
		    	buf = new ByteArrayInputStream(state.colorMap);
		    }
		    if (mapper != null && buf != null) {
				try {
				    mapper.loadColors(buf);
				    colors = mapper.getState();
				}
				catch (Exception e) {
				    // ignore
				}
		    }
		}
    }
    
    public ColorMapOp getColorMapOp(BufferedImage image) {
    	return  colors.getColorMapOp(image);
    }
    
    /**
	 * Return the world coordinate offset that moves the stamp center to the
	 * image center
	 */
    public Point2D getOffset() {
    	Point2D spatialStamp = stamp.getCenter();
    	Point2D spatialImage = add(spatialStamp, offset);
    	// TODO: Needs to take a projection input
    	Point2D worldStamp = Main.PO.convSpatialToWorld(spatialStamp);
    	Point2D worldImage = Main.PO.convSpatialToWorld(spatialImage);
    	return sub(worldImage, worldStamp);
    }
    
    /**
     * Set the world coordniate offset that moves the stamp center to the
     * image center.
     */
    public void setOffset(Point2D worldOffset) {
    	Point2D spatialStamp = stamp.getCenter();
    	Point2D worldStamp = Main.PO.convSpatialToWorld(spatialStamp);
    	Point2D worldImage = add(worldStamp, worldOffset);
    	Point2D spatialImage = Main.PO.convWorldToSpatial(worldImage);
    	offset = sub(spatialImage, spatialStamp);
    }
    
    private Point2D add(Point2D p1, Point2D p2) {
    	return new Point2D.Double(p1.getX() + p2.getX(), p1.getY() + p2.getY());
    }
    
    private Point2D sub(Point2D p1, Point2D p2) {
    	return new Point2D.Double(p1.getX() - p2.getX(), p1.getY() - p2.getY());
    }
    
    private Point2D.Double loadOffset() {
    	// We won't persist offsets for local images
    	if (pdsi.getInstrument().equalsIgnoreCase("davinci")) {
    		return new Point2D.Double(0.0, 0.0);
    	};
    	
    	Point2D.Double offset = null;
    	
		String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");// @since change bodies – added prefix

    	try
    	{
    		
            String urlStr = "OffsetFetcher?stamp="+stamp.getId();
            
            urlStr+="&body="+body;
            
            urlStr+="&instrument="+pdsi.getInstrument();
                            
            ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(urlStr));

            double pt[] = (double[]) ois.readObject();
            offset = new Point2D.Double(pt[0], pt[1]);
            ois.close();
    	}
    	catch(Exception e)
    	{
    		log.aprintln(e.getMessage());
    	}
    	
    	return offset;
    }
    
    public void saveOffset() {
    	// We won't persist offsets for local images
    	if (pdsi.getInstrument().equalsIgnoreCase("davinci")) return;

    	stampUpdates.add(this);
    	lastOffsetUpdateTime = System.currentTimeMillis();
    }
        
    static Timer saveTimer = new Timer("Stamp Save Timer");
    static TimerTask timerTask = null;
    static long lastOffsetUpdateTime = Long.MAX_VALUE;
    
    static Set<FilledStampImageType> stampUpdates = new HashSet<FilledStampImageType>(20);
    
    static {    	    	
    	timerTask = new TimerTask() {	
 			public void run() {
 				// Wait until 10 seconds after the last update to commit offset
 				// values to the database.
 				if (System.currentTimeMillis()-lastOffsetUpdateTime < 10000) {
 					log.println("FilledStamp TimerTask not running yet...");
 					return;
 				}
 				
 				if (stampUpdates.size()==0) return;
 				
				log.println("FilledStamp TimerTask Running...");
								
				try
				{
					String body=Config.get(Util.getProductBodyPrefix() + Config.CONFIG_BODY_NAME, "Mars");// @since change bodies – added prefix

					final DecimalFormat decimalFormatter = new DecimalFormat(".########");

					for(FilledStampImageType stamp : stampUpdates) {				
						String updateStr = "OffsetUpdater?stamp="+stamp.stamp.getId();
						
						updateStr += "&xoffset="+decimalFormatter.format(stamp.offset.getX())+"&yoffset="+decimalFormatter.format(stamp.offset.getY());
						
			            updateStr+="&body="+body;
			            
			            updateStr+="&instrument="+stamp.pdsi.getInstrument();

		 	            StampLayer.queryServer(updateStr);
					} 
					
		   			stampUpdates.clear();
		   			lastOffsetUpdateTime = System.currentTimeMillis();
				}
				catch(Exception e)
				{
					log.aprintln(e.getMessage());
				}
			}			
		};
    	
    	saveTimer.schedule(timerTask, 10000, 10000);
    }
    
    public State getState() {
		State state = new State();
		
		if (stamp != null)
		    state.id = stamp.getId();
	
		// Slightly bizarre means for storing colors since our
		// internal representation cannot be serialized.
		ColorMapper mapper = new ColorMapper();
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		if (mapper != null &&
		    buf != null) {
		    try {
			mapper.setState(colors);
			mapper.saveColors(buf);
			state.colorMap = buf.toByteArray();
		    }
		    catch (Exception e) {
			// ignore
		    }
		}

		state.imageType = pdsi.imageType;
		return state;
    }

    /**
     * Minimal description of state needed to recreate
     * a FilledStamp.
     */
    public static class State extends FilledStamp.State implements SerializedParameters {
    	private static final long serialVersionUID = -2396089407110933527L;
    	byte[] colorMap;
    	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
    		ois.defaultReadObject();
    	}
    	
    	public void setImageType(String newType) {
    		imageType=newType;
    	}
    }
}
