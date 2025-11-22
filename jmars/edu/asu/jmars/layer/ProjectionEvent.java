package edu.asu.jmars.layer;

import edu.asu.jmars.*;

import java.awt.geom.*;

public class ProjectionEvent
 {
	ProjObj oldPO;

	public ProjectionEvent(ProjObj oldPO)
	 {
		this.oldPO = oldPO;
	 }

	public ProjObj getOldPO()
	 {
		return  oldPO;
	 }

	public Point2D old2newWorld(Point2D oldWorldPt)
	 {
		return
			Main.PO.convSpatialToWorld(
				oldPO.convWorldToSpatial(oldWorldPt));
	 }
 }
