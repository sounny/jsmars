package edu.asu.jmars.layer.stamp.radar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Objects;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.stamp.LineShape;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.viz3d.ThreeDManager;

public class FullResHighlightListener extends MouseMotionAdapter{

	private StampLView myLView;
	private ArrayList<Point2D> spatialPoints = new ArrayList<Point2D>();
	
	public FullResHighlightListener(StampLView view){
		myLView = view;
	}
	
	/**
	 * Point the highlight path onto the given graphics object(g2)
	 * with the given projection (po). 
	 * @param g2
	 * @param po
	 */
	public void paintHighlight(Graphics2D g2, ProjObj po){
		if(getProfileLine() !=null && spatialPoints != null && spatialPoints.size()>0){
			//create the highlight profile
			Shape hp = createWorldPath(po);
			
			g2.setStroke(myLView.getProj().getWorldStroke(4));
			g2.setColor(new Color(255,255,255,150));
			g2.draw(hp);
		}
	}
	
	private Shape getProfileLine(){
		Shape profileLine = null;
		
		StampLayer sl = myLView.stampLayer;
		
		if(sl.lineShapes() && sl.getSelectedStamps().size()>0){
			LineShape ls = (LineShape)myLView.stampLayer.getSelectedStamps().get(0);
			profileLine = ls.getPath().get(0);
		}
		
		return profileLine;
	}
	
	private Shape createWorldPath(ProjObj po){
		//convert spatial points to world points
		ArrayList<Point2D> worldPts = new ArrayList<Point2D>();
		for(Point2D spPt : spatialPoints){
			Point2D wdPt = po.convSpatialToWorld(spPt);
			worldPts.add(wdPt);
		}
		
		//use those points to create a path shape
		GeneralPath worldPath = new GeneralPath();
		for(Point2D pt : worldPts){
			//Start the path
			if(worldPath.getCurrentPoint() == null){
				worldPath.moveTo(pt.getX(), pt.getY());
			}
			//continue the path
			else{
				worldPath.lineTo(pt.getX(), pt.getY());
			}
		}
		
		return worldPath;
	}
	
	/**
	 * Set the array of spatial points that define the highlight
	 * profile path.
	 * @param spPoints
	 */
	public void setHighLightPoints(ArrayList<Point2D> spPoints){
		//only reset the points if they are different from existing ones
		//use this method because it can easily handle if one of the lists is null
		if(!Objects.equals(spatialPoints, spPoints)){

			spatialPoints = spPoints;
			//refresh lview
			myLView.repaint();
			
			if (ThreeDManager.isReady()) {
				//update buffer and refresh 3d if necessary
				myLView.getLayer().increaseStateId(StampLayer.SELECTIONS_BUFFER);
				//update the 3d view if has lview3d enabled
				if(myLView.getLView3D().isEnabled()){
					ThreeDManager mgr = ThreeDManager.getInstance();
					//If the 3d is already visible, update it
					if(myLView.getLView3D().isVisible()){
						mgr.updateDecalsForLView(myLView, true);
					}
				}
			}
		}
	}
}
