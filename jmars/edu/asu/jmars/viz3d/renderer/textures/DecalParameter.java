package edu.asu.jmars.viz3d.renderer.textures;

import java.awt.geom.Point2D;
import java.io.Serializable;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.viz3d.renderer.gl.terrain.TerrainTile;


/**
 * Container class for the parameters necessary to construct a valid on-body Decal.
 * @author whagee
 */
public class DecalParameter implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6650073334922290962L;
	private Point2D start;
	private Point2D end;
	private Point2D mid;
	private int ppd;
	private TerrainTile tile;
	private transient Projection_OC projection = null;
	
	/**
	 * Constructor
	 * @param start Point2D representing the geographical extent starting point
	 * @param end   Point2D representing the geographical extent ending point
	 * @param mid   Point2D representing the geographical extent mid point
	 * @param projection Geographical projection to be used in creating the associated image
	 * @param ppd integer value of the pixels per degree to be used in creating the associated image
	 * @param tile TerrainTile object that contains the Cartesian space surface of the tile
	 * 		as well as the image texture coordinate mapping
	 */
	DecalParameter(Point2D start, Point2D end, Point2D mid,/* Projection_OC projection, */int ppd, TerrainTile tile) {
		if (start == null) {
			throw new IllegalArgumentException("Invalid DecalParameter Argument: Point2D start is null.");
		} else {
			this.start = start;
		}
		if (end == null) {
			throw new IllegalArgumentException("Invalid DecalParameter Argument: Point2D end is null.");
		} else {
			this.end = end;
		}
		if (mid == null) {
			throw new IllegalArgumentException("Invalid DecalParameter Argument: Point2D mid is null.");
		} else {
			this.mid = mid;
		}
//		if (projection == null) {
//			throw new IllegalArgumentException("Invalid DecalParameter Argument: projection is null.");
//		} else {
//			this.projection = projection;
//		}
		if (ppd < 2 || ppd > 512 || !isPowerOf2(ppd)) {
			throw new IllegalArgumentException("Invalid DecalParameter Argument: ppd is not valid: "+ppd);
		} else {
			this.ppd = ppd;
		}
		if (tile == null) {
			throw new IllegalArgumentException("Invalid DecalParameter Argument: TerrainTile is null.");
		} else {
			this.tile = tile;
		}
	}


	/**
	 * @return the start
	 */
	public Point2D getStart() {
		return start;
	}


	/**
	 * @return the end
	 */
	public Point2D getEnd() {
		return end;
	}


	/**
	 * @return the mid
	 */
	public Point2D getMid() {
		return mid;
	}


	/**
	 * @return the projection
	 */
//	public ProjObj getProjection() {
//		return projection;
//	}


	/**
	 * @return the ppd
	 */
	public int getPpd() {
		return ppd;
	}


	/**
	 * @return the tile
	 */
	public TerrainTile getTile() {
		return tile;
	}
	
	/**
	 * Method to release all object references
	 */
	public void dispose() {
		start = null;
		end = null;
		mid = null;
//		projection = null;
		if (tile != null) {
			tile.dispose();
		}
		tile = null;
	}
	
	private boolean isPowerOf2(int i) {
		return i > 2 && ((i&-i)==i);
	}


	/**
	 * @return the projection
	 */
	public ProjObj getProjection() {
		return tile.getProjection();
	}


	/**
	 * @param projection the projection to set
	 */
	public void setProjection(Projection_OC projection) {
		this.projection = projection;
	}
}
