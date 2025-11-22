package edu.asu.jmars.layer.stamp;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import edu.asu.jmars.Main;
import edu.asu.msff.StampInterface;

public class LineShape extends StampShape {
	private GeneralPath path;

	public LineShape(StampInterface stamp, StampLayer stampLayer) {
		super(stamp, stampLayer);
	}
	
	/* (non-Javadoc)
	 * @see edu.asu.jmars.layer.stamp.StampShape#getPath()
	 * 
	 * Return a list with AT MOST one path for a radar stamp shape
	 */
	public synchronized List<GeneralPath> getPath(){
		if(paths == null){
			paths = new ArrayList<GeneralPath>();
			//This is the old logic used by the stamp shape class,
			// which worked for the radar layer.
			if(path == null)
		    {
		        path = new GeneralPath();
		        Point2D pt;
		                    
		        double pts[] = myStamp.getPoints();
		        
		        boolean moveNext=true;
		        boolean closePath=true;
		        
		        for (int i=0; i<pts.length; i=i+2) {
		        	if (Double.isNaN(pts[i]) || Double.isNaN(pts[i+1])) {
		        		moveNext=true;
		        		closePath=false;
		        		continue;
		        	} else {
		        		pt = Main.PO.convSpatialToWorld(pts[i], pts[i+1]);
		        	}
		        	
		            if (moveNext) {
		            	path.moveTo((float)pt.getX(), (float)pt.getY());
		            	moveNext=false;
		            } else {
		            	float x = (float) pt.getX();
		            	float y = (float) pt.getY();
		           		path.lineTo(x, y);
		            }
		        }
		        
		        if (closePath && pts.length>0) {
		        	path.closePath();
		        }
		    } 
			
			paths.add(path);
		}
		
        return paths;
	}

}
