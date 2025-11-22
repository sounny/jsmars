package edu.asu.jmars.graphics;

import edu.asu.jmars.*;
import edu.asu.jmars.util.*;
import java.awt.geom.*;

/**
 ** Superclass for all spatial-based graphics contexts. Basically just
 ** supplies an extra utility function for doing spatial to world
 ** conversions.
 **/
public abstract class SpatialGraphics2D
 extends Graphics2DAdapter
 {
	/**
	 ** Given a point in spatial coordinates, returns an array of
	 ** every occurrence of that point within the world-range of this
	 ** graphics context.
	 **/
	public abstract Point2D[] spatialToWorlds(Point2D s);
 }
