package edu.asu.jmars.layer.stamp;

import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.msff.StampInterface;

public class WindShape extends StampShape {

	private StampInterface myStamp;
	StampLayer stampLayer;
	
	public WindShape(StampInterface stamp, StampLayer stampLayer) {
		super(stamp, stampLayer);
		myStamp=stamp;
		this.stampLayer = stampLayer;
	}
		
	public String getTooltipText() {        
        String info = "";
        
        double pts[] = myStamp.getPoints();

        double xmag = 360-pts[2];
        double ymag = pts[3];
        
        DecimalFormat df = new DecimalFormat("##.##");
        
        double speed = Math.sqrt(xmag*xmag+ymag*ymag);

        info += "Zonal Wind: ";
        
        info += df.format(xmag) + " m/s<P>";
        
        info += "Meridional Wind: ";
        
        info += df.format(ymag) + " m/s<P>";
        
        info += "Wind Speed: ";
                      
        info += df.format(speed) + " m/s";
                
        return info;
    }

    private double lastMag = 0.0;
    
    private Point2D origin = null;
    
    public synchronized Point2D getOrigin() {
    	if (origin==null) {
    		getPath();
    	}
    	return origin;
    }	
    
    public synchronized List<Area> getFillAreas() {
    	List<GeneralPath> paths = getPath();
    	
    	ArrayList<Area> areas = new ArrayList<Area>();
    	
    	for (GeneralPath s : paths) {
    		areas.add(new Area(s));    		
    	}
    	return areas;    	
    }

    public synchronized List<GeneralPath> getPath() {
    	return getPath(Main.PO);
	}

    public synchronized List<GeneralPath> getPath(ProjObj po) {
    	if (lastMag!=stampLayer.getSettings().getMagnitude()) {
    		proj2PathsMap.clear();
    	}
    	
    	String key = po.getCenterLon()+":"+po.getCenterLat();
    	if (proj2PathsMap.containsKey(key)) {
    		return proj2PathsMap.get(key);
    	}
    	
   		lastMag=stampLayer.getSettings().getMagnitude();
   		ArrayList<GeneralPath> po_paths = new ArrayList<GeneralPath>();
        		
        GeneralPath path = new GeneralPath();
        Point2D pt0, pt1, arrow1, arrow2;
                            
        double pts[] = myStamp.getPoints();

        double xmag = pts[2];
        double ymag = pts[3];
                
        double SCALE_FACTOR=stampLayer.getSettings().getMagnitude();
                
        xmag = (360-xmag);
        xmag = xmag * SCALE_FACTOR;
        xmag = 360-xmag;
                
        ymag = ymag * SCALE_FACTOR;
                
        // First (and only real) point
        pt0 = Main.PO.convSpatialToWorld(pts[0], pts[1]);
        
        origin = pt0;
                
      	pt1 = Main.PO.convSpatialToWorld(pts[0]+xmag, pts[1]+ymag);

      	// Handle the case where our vector crosses the center of
      	// the projection, which plays havok with the angle calculations
      	// and results in weird arrowheads.
      	if (pt0.getX()>300 && pt1.getX()<60) {
      		pt1.setLocation(pt1.getX()+360, pt1.getY());
      	}
            	
      	double y= (pt1.getY()-pt0.getY());
      	double x = (pt1.getX()-pt0.getX());
            	
      	double angle = Math.atan2(y, x);
            	
      	angle += Math.PI/2;

      	double angle1 = angle + 2.26892803; //(130 degrees);
      	double angle2 = angle - 2.26892803; //(130 degrees);

      	double x2, y2, x3, y3;
      	double L = 0.4 * SCALE_FACTOR;
            	
      	x2 = L*Math.sin(angle1);
      	y2 = L*Math.cos(angle1);

      	x3 = L*Math.sin(angle2);
      	y3 = L*Math.cos(angle2);

      	arrow1 = new Point2D.Double(pt1.getX()+x2,
           			pt1.getY()-y2);
            	
        arrow2 = new Point2D.Double(pt1.getX()+x3,
          			pt1.getY()-y3);

        path.moveTo((float)pt0.getX(),
                    (float)pt0.getY());
            	
        path.lineTo((float)pt1.getX(),
                    (float)pt1.getY());

        path.lineTo((float)arrow1.getX(),
                    (float)arrow1.getY());

        path.moveTo((float)pt1.getX(),
                    (float)pt1.getY());

        path.lineTo((float)arrow2.getX(),
                    (float)arrow2.getY());

        po_paths.add(path);
        
   		proj2PathsMap.put(key, po_paths);
		    		
        return  po_paths;
    }
}
