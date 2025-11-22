package edu.asu.jmars.util.quadtree;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;


/**
 * Represent a node in an util.quadtree.QuadTree
 * "Portions Copyright (C) Thatcher Ulrich, 2000"
*/
public class QNode<T> {
	QNode<T>	parent;
	QNode<?>[][] child = new QNode<?>[2][2];
	float	cx, cy;
	int	depth;
	ArrayList<T> objects;
	Rectangle2D box = null;
	int id;

	/** 
	 * Constructor
	 * @param p parent Node in the QuadTree
	 * @param x X coordinate of the center of the node
	 * @param y Y coordinate of the center of the node
	 * @param d the number of levels deep in the QuadTree where this node is located
	 */
	public QNode (int id, QNode<T> p, float x, float y, int d) {
		this.id = id;
		parent = p;
		cx = x;
		cy = y;
		depth = d;
		
		objects = new ArrayList<T>(0);
	}

	/**
	 * Returns the Axis Aligned Bounding Box of this node
	 *
	 * @param halfSize the shortest distance from the center of the node to one side
	 * 
	 * @return The AABBox for this node
	 *
	 * thread-safe
	 */
	public Rectangle2D getAABbox(float halfSize) {
		if (box == null) {
			double x = cx - halfSize;
			double y = cy - halfSize;
			double width = halfSize * 2.0;
			double height = halfSize * 2.0;
			
			box = new Rectangle2D.Double(x, y, width, height);
		}
		return box;	
	}
	
	/**
	 * Returns the Axis Aligned Bounding Box of a generic node
	 *
	 * @param 
	 * @param halfSize the shortest distance from the center of the node to one side
	 * 
	 * @return The AABBox for this node
	 *
	 * thread-safe
	 */
	public static Rectangle2D getAABbox(float cx, float cy, float halfSize) {

		double x = cx - halfSize;
		double y = cy - halfSize;
		double width = halfSize * 2.0;
		double height = halfSize * 2.0;
		
		return new Rectangle2D.Double(x, y, width, height);
	}

	
	/**
	 * Returns distance of the minimum X side of the AABBox from the node center
	 *
	 * @param halfSize the shortest distance from the center of the node to one side
	 * 
	 * @return float
	 *
	 * thread-safe
	 */
	public float getMinX(float halfSize) {
		return cx - halfSize;
	}

	/**
	 * Returns distance of the maximum X side of the AABBox from the node center
	 *
	 * @param halfSize the shortest distance from the center of the node to one side
	 * 
	 * @return float
	 *
	 * thread-safe
	 */
	public float getMaxX(float halfSize) {
		return cx + halfSize;
	}

	/**
	 * Returns distance of the minimum Y side of the AABBox from the node center
	 *
	 * @param halfSize the shortest distance from the center of the node to one side
	 * 
	 * @return float
	 *
	 * thread-safe
	 */
	public float getMinY(float halfSize) {
		return cy - halfSize;
	}

	/**
	 * Returns distance of the maximum Y side of the AABBox from the node center
	 *
	 * @param halfSize the shortest distance from the center of the node to one side
	 * 
	 * @return float
	 *
	 * thread-safe
	 */
	public float getMaxY(float halfSize) {
		return cy + halfSize;
	}
	
	/**
	 * Returns all the objects that have been stored in this node.
	 * @return
	 */
	public ArrayList<T> getObjects() {
		return objects;
	}	
}
