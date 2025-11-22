package edu.asu.jmars.util.quadtree;

import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;


import edu.asu.jmars.util.DebugLog;

/**
 * Implementation of a loose volume quadtree specifically designed to represent collection
 * 	of geometric objects in 2D.
 * 
 * "Portions Copyright (C) Thatcher Ulrich, 2000"
 */

public class QuadTree <T extends Shape> {
	
	final private int	OBJECT_COUNT;
	final public float WORLD_SIZE;
	private int	MAX_DEPTH = 100;
	/** represents the 'X' position in a 2D vector */
	public static final int X = 0;
	/** represents the 'Y' position in a 2D vector */
	public static final int Y = 1;
	/** represents the first vertex position in a 2D triangle */
	/** also represents the 'X' coefficient in the equation of a line: Ax + By + Cz = D */
	public static final int A = 0;
	/** represents the second vertex position in a 2D triangle */
	/** also represents the 'Y' coefficient in the equation of a line: Ax + By + Cz = D */
	public static final int B = 1;
	/** represents the third vertex position in a 2D triangle */
	/** also represents the 'Z' coefficient in the equation of a line: Ax + By + Cz = D */
	public static final int C = 2;
	/** represents the first vertex position in a 3D triangle */
	/** also represents the constant in the equation of a line: Ax + By + Cz = D */
	public static final int D = 3;
	/** The origin */
	public static final float[] ORIGIN = new float[]{0f, 0f};
	/** Unit vector along the 'X' axis */
	public static final float[] X_AXIS = new float[]{1f, 0f};
	/** Unit vector along the 'Y' axis */
	public static final float[] Y_AXIS = new float[]{0f, 1f};
	/** Low tolerance epsilon (0.0001) for floating point comparisons */
	public static final float MICRO_EPSILON = 0.0001f;
	/** Standard tolerance epsilon (0.00001) for floating point comparisons */
	public static final float MINI_EPSILON = 0.00001f;
	/** High tolerance epsilon (0.000001) for floating point comparisons */
	public static final float EPSILON = 0.000001f;
	/** epsilon^2 for floating point comparisons */
	public static final float EPSILON_SQUARE = EPSILON * EPSILON;
	/** Double epsilon for floating point comparisons */
	public static final float EPSILON_X2 = 2 * EPSILON;
	/** Super high tolerance epsilon (0.0000000000001) for floating point comparisons */
	public static final float SUPER_EPSILON = 0.0000000000001f;
	/** No division constant of 1/3 for 3D vector magnitude and average calculations */
	public static final float ONE_THIRD = 0.33333333333333333f;
	
    private static DebugLog log = DebugLog.instance();

		
	int[] depthTotals = new int[MAX_DEPTH];
	int	nodesChecked = 0;
	int	objectsActuallyInFrustum = 0;
	int	objectsChecked = 0;
	int numObjs;
	public final float looseK = 2;
	
	boolean	showNodes = true;
	boolean	showNodeContents = true;
	boolean isObj = false;
	
	
	private QNode<T> root = null;
	
	private int nodeId = 0;
	
	/**
	 * Default constructor.
	 * Will create an uninitialized OctTree instance (no nodes).
	 */
	public QuadTree () {
		root = new QNode<>(0, null, 0f, 0f, 0);
		WORLD_SIZE = 2f;
		OBJECT_COUNT = 0;	
	}

	/**
	 * Constructor. Creates an initialized OctTree.
	 *  
	 * @param o root node
	 */
	public QuadTree (QNode<T> o) {	
		this(o, 2f, 50, 4000000);
	}
	
	/**
	 * Constructor 
	 * 
	 * @param o root node
	 * @param worldSize maximum size of the largest node allowed in the tree along a single axis
	 * @param maxDepth max number of levels deep the oct tree will allow new sub-node creation
	 * @param maxObjects maximum number of objects that can be inserted into the tree
	 */
	public QuadTree (QNode<T> o, float worldSize, int maxDepth, int maxObjects) {	
		root = o;
		WORLD_SIZE = worldSize;
		OBJECT_COUNT = maxObjects;	
		MAX_DEPTH = maxDepth;
	}
	
	/**
	 * Returns the number of nodes in the given subtree.
	 *
	 * @param q the root node of the subtree to check
	 * @return node count
	 * @throws
	 *
	 * thread-safe
	 */
	@SuppressWarnings("unchecked")
	public int	countNodes(QNode<T> q)
	{
		int	count = 1;	// Count ourself.

		// Count descendants.
		int	j, k;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				if (q.child[k][j] != null) {
					count += countNodes((QNode<T>)q.child[k][j]);
				}
			}
		}
		return count;
	}

	/*
	 * Do not call this method unless the intent is to remove all nodes
	 * and their contents in their entirety!! 
	 * 
	 */
	@SuppressWarnings("unchecked")
	public void clearAllNodes(QNode<T> q)
	{
		int	j, k;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				if (q.child[k][j] != null) {
					clearAllNodes((QNode<T>)q.child[k][j]);
				}
			}
		}
		q.objects.clear();
		if (numObjs > 0) {
			numObjs--;
		}
		return;
	}

	/**
	 * Insert the given Type into the initialized tree.
	 * <Description>
	 *
	 * @param t the Type to insert
	 * @return Returns the depth of the node the object was placed in.
	 *
	 * not thread-safe
	 */
	public int insert(T t) {
		if (root == null ) {
			root = new QNode<>(0, null, 0f, 0f, 0);
		}
		return insert(root, t);		
	}

	
	
	/**
	 * Insert the given Type into the tree given by Node.
	 *
	 * @param o the root node of the tree to insert into
	 * @param t the Type to insert
	 * @return Returns the depth of the node the object was placed in.
	 *
	 * not thread-safe
	 */
	@SuppressWarnings("unchecked")
	public int insert(QNode<T> o, T t)
	{
		if (o == null) {
			o = root;
		}
		// Check child nodes to see if object fits in one of them.
		if (o.depth + 1 < MAX_DEPTH) {
			float halfSize = looseK * WORLD_SIZE / (2 << o.depth+1);
			float offset = (WORLD_SIZE / (2 << o.depth)) / 2;
			
			if (halfSize != Float.POSITIVE_INFINITY && halfSize != Float.NEGATIVE_INFINITY && halfSize != Float.NaN) {

				// Pick child based on classification of object's center point.
				Rectangle2D tBound = t.getBounds2D();
				int	i = (tBound.getCenterX() <= o.cx) ? 0 : 1;
				int	j = (tBound.getCenterY() <= o.cy) ? 0 : 1;
	
				float	cx = o.cx + ((i == 0) ? -offset : offset);
				float	cy = o.cy + ((j == 0) ? -offset : offset);
				Rectangle2D tmpNode = QNode.getAABbox(cx, cy, halfSize);
				if (tmpNode.contains(t.getBounds2D())) {
					// Recurse into this node.
					if (o.child[j][i] == null) {
						o.child[j][i] = new QNode<T>(nodeId++, o, cx, cy, o.depth + 1);
					}
					return insert((QNode<T>)o.child[j][i], t);
				}
			}
		}
		
		// Keep object in this node.
		o.objects.add(t);
		depthTotals[o.depth]++;
		numObjs++;
		
		return o.depth;
	}	

	/*
	 * Counts the objects in a given region, based on
	 * the information in the octtree partitioning.
	 *
	 * @param o the node to count
	 * @param region
	 * @param return count of number of objects found
	 *
	 * thread-safe
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	private void countObjectsInArea(QNode<T> n, T region, Integer count)
	{
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		Rectangle2D box = n.getAABbox(halfSize);

		if (!box.intersects(region.getBounds2D())) {
			return;
		}

		
		// Check children.
		int	k, j;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				if (n.child[k][j] != null) {
					countObjectsInArea((QNode<T>)n.child[k][j], region, count);
				}
			}
		}

		// Count objects in this node.
		ArrayList<T> o = n.getObjects();
		while (o != null && !o.isEmpty()) {
			for (T t : o) {				
				count++;
			} 
		}

		return;
	}
	
	
	/*
	 * Checks whether two nodes intersect.
	 *
	 * @param a a node
	 * @param b a node to test for intersection
	 * @return true if the nodes intersect
	 *
	 * thread-safe
	 */
	private boolean looseNodeNodeIntersect(QNode<T> a, QNode<T> b) {
		
		float aHalfSize = looseK * WORLD_SIZE / (2 << a.depth);
		float bHalfSize = looseK * WORLD_SIZE / (2 << b.depth);
		
		if (a.getMinX(aHalfSize) > b.getMaxX(bHalfSize)) {
			return false;
		}
		if (a.getMaxX(aHalfSize) < b.getMinX(bHalfSize)) {
			return false;
		}
		if (a.getMinY(aHalfSize) > b.getMaxY(bHalfSize)) {
			return false;
		}
		if (a.getMaxY(aHalfSize) < b.getMinY(bHalfSize)) {
			return false;
		}		
		// we have overlap on both axes so we have an intersection
		return true;
	}

	/**
	 * Prints the number of nodes at each depth for the Octtree to the log
	 *
	 * thread-safe
	 */
	public void printDepthTotals()
	{
		log.aprintln("Oct Tree Depth Totals:");
		
		int	i;
		for (i = 0; i < MAX_DEPTH; i++) {
			if (depthTotals[i] != 0) {
				log.aprintln("ct["+i+"] = "+depthTotals[i]);
			}
		}
	}

	/**
	 * Returns the root QNode of the OctTree
	 *
	 * @return root node
	 *
	 * thread-safe
	 */
	public QNode<T> getRoot() {
		return root;
	}

	/**
	 * Search an Node and all its children for Types that are intersected by the input Ray
	 *
	 * @param ray the Ray to intersect the node with
	 * @param n the Node to test for intersection
	 * @param list List of all the Types intersected by the Ray
	 *
	 * possibly thread-safe
	 */
	@SuppressWarnings("unchecked")
	public void rayIntersectShortCircuit(QPoint ray, QNode<T> n, ArrayList<T> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
				
		Rectangle2D box = n.getAABbox(halfSize);

		if (!box.intersects(ray.getBounds2D())) {
			return;
		}

		
		// Check children.
		int	k, j;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				if (n.child[k][j] != null) {
					rayIntersectShortCircuit(ray, (QNode<T>)n.child[k][j], list);
				}
			}
		}

		// Count objects in this node.
		ArrayList<T> o = n.getObjects();
		if (o != null && !o.isEmpty()) {
			for (T t : o) {				
				if (t.intersects(ray.getBounds2D())) {
					list.add(t);
				} 
			}
		}
	}

	/**
	 * Returns the closest Type to the origin of the Ray
	 * @param ray
	 * @param n start node 
	 * @param list the entire intersection result
	 * @return nearest Type
	 */
	public T rayIntersectNearest(QPoint ray, QNode<T> n, ArrayList<T> list) {
		 rayIntersectShortCircuit(ray, n, list);
		
        // find the closest match
        double dist = Float.MAX_VALUE;
        T result = null;
                   
		for (T sr : list) {            
            double tmp = ray.distance(sr.getBounds2D().getCenterX(), sr.getBounds2D().getCenterY());
            if (tmp < dist) {
                dist = tmp;
                result = sr;
            }
        }	
		return result;
	}
	/*
	 * Tests for intersection of the input Ray with the axis-aligned bounding box of the input QNode
	 *
	 * @param ray the Ray to test
	 * @param n the QNode to intersect test
	 * @return true if the Ray intersects the axis-aligned bounding box of the QNode
	 *
	 * probably thread-safe
	 */
	public boolean looseRayNodeIntersectTest(QPoint ray, QNode<T> n) {	
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		Rectangle2D box = n.getAABbox(halfSize);

		return box.intersects(ray.getBounds2D());
	}
		
	/*
	 * Method to return the number of objects in the OctTree
	 *
	 * @return number of Types
	 *
	 * <thread-safe?>
	 */
	private int getSize() {
		return OBJECT_COUNT;
	}
	
	/**
	 * Method to return all the Objects currently in use.
	 *
	 * @return a Map of all the Facets in the shape objects in the oct tree
	 *
	 * probably thread-safe
	 */
	public List<T> getAllObjects() {
		ArrayList<T> list = new ArrayList<>();
		if (this.root != null) {
			this.looseGetAllObjects(this.root, list);
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	private void looseGetAllObjects(QNode<T> n, ArrayList<T> list) {
		// Check children.
		int	k, j;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				if (n.child[k][j] != null) {
					looseGetAllObjects((QNode<T>) n.child[k][j], list);
				}
			}
		}

		ArrayList<T> o = n.getObjects();
		if (o != null) {
			for (T t : o) {
				list.add(t);
			}
		}
	}
	
/**
 * Method to intersect a Shape with the OctTree and return all objects that intersect the Shape.
 * @param Shape 
 * @param n OctTree node starting point
 * @param list return list of intersected objects
 */
	@SuppressWarnings("unchecked")
	public void shapeIntersect(T shape, QNode<T> n, ArrayList<T> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		Rectangle2D box = n.getAABbox(halfSize);

		if (!box.intersects(shape.getBounds2D())) {
			return;
		}

		
		// Check children.
		int	k, j;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				if (n.child[k][j] != null) {
					shapeIntersect(shape, (QNode<T>)n.child[k][j], list);
				}
			}
		}

		// Count objects in this node.
		ArrayList<T> o = n.getObjects();
		while (o != null && !o.isEmpty()) {
			for (T t : o) {				
				if (t.intersects(shape.getBounds2D())) {
					list.add(t);
				} 
			}
		}
	}

	/**
	 * Returns the closest object to the origin of the Ray
	 * @param ray
	 * @param n start node 
	 * @param list the entire intersection result
	 * @return nearest object
	 */
	public T shapeIntersectNearest(T shape, QNode<T> n, ArrayList<T> list) {
		 shapeIntersect(shape, n, list);
		
        // find the closest match
        double dist = Float.MAX_VALUE;
        T result = null;
                   
		for (T sr : list) { 
			Point2D shp = new Point2D.Double(shape.getBounds2D().getCenterX(), shape.getBounds2D().getCenterY());
            double tmp = shp.distance(sr.getBounds2D().getCenterX(), sr.getBounds2D().getCenterY());
            if (tmp < dist) {
                dist = tmp;
                result = sr;
            }
        }	
		return result;
	}
	
	public float[][] octantSizeToPoints(float[] center, float size) {
		float xMin = center[0] - size;
		float yMin = center[1] - size;
		float zMin = center[2] - size;
		
		float xMax = center[0] + size;
		float yMax = center[1] + size;
		float zMax = center[2] + size;
		
		return new float[][] {
			{xMin, yMin, zMin},
			{xMax, yMin, zMin},
			{xMax, yMin, zMax},
			{xMin, yMin, zMax},
			{xMin, yMax, zMin},
			{xMax, yMax, zMin},
			{xMax, yMax, zMax},
			{xMin, yMax, zMax}
		};
	}

	/**
	 * @return the WORLD_SIZE
	 */
	public float getWORLD_SIZE() {
		return WORLD_SIZE;
	}

}
