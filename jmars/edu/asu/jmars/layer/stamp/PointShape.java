package edu.asu.jmars.layer.stamp;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.util.Util;
import edu.asu.msff.StampInterface;

public class PointShape extends StampShape {

	public PointShape(StampInterface stamp, StampLayer stampLayer) {
		super(stamp, stampLayer);
	}
	    
	// TODO: Needs to be WORLD origin
    private Point2D origin = null;
    
    public synchronized Point2D getOrigin() {
    	if (origin==null) {
    		getPath();
    	}
    	return origin;
    }	
        
    public synchronized List<Area> getFillAreas(ProjObj po) {
    	String key = po.getCenterLon()+":"+po.getCenterLat()+":"+stampLayer.getSettings().getOriginMagnitude();
    	if (proj2AreasMap.containsKey(key)) {
    		return proj2AreasMap.get(key);
    	} else {
    		// We don't want to do all of this work multiple times if we get a flurry of requests, but we also don't want to slow down
    		// multi-threaded calls later, waiting for locks... 
    		synchronized(this) {
    			if (proj2AreasMap.containsKey(key)) {
    				return proj2AreasMap.get(key);
    			}

    			ArrayList<Area> areas = new ArrayList<Area>();

    	    	double pts[] = myStamp.getPoints();

    	    	double radiusKm = 0;
    	    	
    	    	if (stampLayer.fixedSpotSize()) {
    				radiusKm = Double.parseDouble(stampLayer.getParam(StampLayer.FIXED_SPOT_SIZE));
    	    	} else {
    	    		double radiusDeg = Double.parseDouble(stampLayer.getParam(StampLayer.SPOT_SIZE))*stampLayer.getSettings().getOriginMagnitude()/2;

    	    		radiusKm = 2 * Math.PI * Util.MEAN_RADIUS / 360 * radiusDeg;
    	    	}
    	    	
    	        Shape worldShape = StampUtil.getProjectedShape(360-pts[0], pts[1], radiusKm, po);
    	        
    	        areas.add(new Area(worldShape));
    			
	    		proj2AreasMap.put(key, areas);
	        	return areas;
    		}
    	}     	
    }

    public synchronized List<GeneralPath> getPath() {
        if(paths == null)
        {
        	paths = new ArrayList<GeneralPath>();
                        
            double pts[] = myStamp.getPoints();

            origin = Main.PO.convSpatialToWorld(pts[0],pts[1]);
            
            GeneralPath path = new GeneralPath();
            
        	path.moveTo((float)origin.getX(),
                    (float)origin.getY());
        	
        	path.lineTo((float)(origin.getX()+0.0001),
                    (float)origin.getY());

        	path.lineTo((float)(origin.getX()+0.0001),
                    (float)origin.getY()+0.0001);

        	paths.add(path);
        } 
        return paths;
    }

    public synchronized List<GeneralPath> getPath(ProjObj po) {
    	ArrayList<GeneralPath> paths = new ArrayList<GeneralPath>();
                    
        double pts[] = myStamp.getPoints();

        // Local value, NOT the class cached value - don't cache specific projections for points
        Point2D origin = po.convSpatialToWorld(pts[0],pts[1]);
        
        GeneralPath path = new GeneralPath();
        
    	path.moveTo((float)origin.getX(),
                (float)origin.getY());
    	
    	path.lineTo((float)(origin.getX()+0.0001),
                (float)origin.getY());

    	path.lineTo((float)(origin.getX()+0.0001),
                (float)origin.getY()+0.0001);

    	paths.add(path);
        	
        return paths;
    }
}