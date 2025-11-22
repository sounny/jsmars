package edu.asu.jmars.viz3d.spatial;

import java.util.ArrayList;

import com.jogamp.opengl.math.geom.AABBox;

/**
 * Represent a node in an edu.asu.jmars.viz3d.core.geometry.Octtree
 * "Portions Copyright (C) Thatcher Ulrich, 2000"
*/
public class Node<T extends SpatialRenderable> {
	Node<T>	parent;
	Node<?>[][][] child = new Node<?>[2][2][2];
	float	cx, cy, cz;
	int	depth;
	ArrayList<T> object = new ArrayList<>();
	AABBox box = null;
	int id;

	/** 
	 * Constructor
	 * @param p parent Node in the OctTree
	 * @param x X coordinate of the center of the node
	 * @param y Y coordinate of the center of the node
	 * @param z Z coordinate of the center of the node
	 * @param d the number of levels deep in the OctTree where this node is located
	 */
	public Node (int id, Node<T> p, float x, float y, float z, int d) {
		this.id = id;
		parent = p;
		cx = x;
		cy = y;
		cz = z;
		depth = d;
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
	public AABBox getAABbox(float halfSize) {
		if (box == null) {
			box = new AABBox(cx - halfSize, cy - halfSize, cz - halfSize, cx + halfSize, cy + halfSize, cz + halfSize);		
		}
		return box;	
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
	 * Returns distance of the minimum Z side of the AABBox from the node center
	 *
	 * @param halfSize the shortest distance from the center of the node to one side
	 * 
	 * @return float
	 *
	 * thread-safe
	 */
	public float getMinZ(float halfSize) {
		return cz - halfSize;
	}

	/**
	 * Returns distance of the maximum Z side of the AABBox from the node center
	 *
	 * @param halfSize the shortest distance from the center of the node to one side
	 * 
	 * @return float
	 *
	 * thread-safe
	 */
	public float getMaxZ(float halfSize) {
		return cz + halfSize;
	}

	/**
	 * Returns all SpatialRenderables that fit in this Node
	 * @return a list of all SpatialRenderables that fit in this Node
	 */
	public ArrayList<T> getObjects() {
		return object;
	}	
}
