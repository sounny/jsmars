package edu.asu.jmars.viz3d.spatial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.math.geom.AABBox;
import com.jogamp.opengl.math.geom.Frustum;

import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.core.geometry.Cuboid;
import edu.asu.jmars.viz3d.core.geometry.Visibility;
import edu.asu.jmars.viz3d.core.math.Util3D;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;

/**
 * Implementation of a loose volume octtree specifically designed to represent collection
 * 	of geometric objects in 3D.
 * 
 * "Portions Copyright (C) Thatcher Ulrich, 2000"
 */

public class OctTree <T extends SpatialRenderable> implements GLRenderable{
	
	final private int	OBJECT_COUNT;
	public float WORLD_SIZE;
	private int	MAX_DEPTH = 100;
	private float minHalfSize = 0.001f;
	static final float	DefaultFOV = FloatUtil.PI / 4;
	/** represents the 'X' position in a 3D vector */
	public static final int X = 0;
	/** represents the 'Y' position in a 3D vector */
	public static final int Y = 1;
	/** represents the 'Z' position in a 3D vector */
	public static final int Z = 2;
	/** represents the 'W' position in a 4D vector */
	public static final int W = 3;
	/** represents the first vertex position in a 3D triangle */
	/** also represents the 'X' coefficient in the equation of a line: Ax + By + Cz = D */
	public static final int A = 0;
	/** represents the second vertex position in a 3D triangle */
	/** also represents the 'Y' coefficient in the equation of a line: Ax + By + Cz = D */
	public static final int B = 1;
	/** represents the third vertex position in a 3D triangle */
	/** also represents the 'Z' coefficient in the equation of a line: Ax + By + Cz = D */
	public static final int C = 2;
	/** represents the first vertex position in a 3D triangle */
	/** also represents the constant in the equation of a line: Ax + By + Cz = D */
	public static final int D = 3;
	/** The origin */
	public static final float[] ORIGIN = new float[]{0f, 0f, 0f};
	/** Unit vector along the 'X' axis */
	public static final float[] X_AXIS = new float[]{1f, 0f, 0f};
	/** Unit vector along the 'Y' axis */
	public static final float[] Y_AXIS = new float[]{0f, 1f, 0f};
	/** Unit vector along the 'Z' axis */
	public static final float[] Z_AXIS = new float[]{0f, 0f, 1f};
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
	
   	final static float TESSELLATION_LIMIT = 0.0005f;
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
	
	boolean checkRootNodeSize = true;
	
	private Node<T> root = null;
	private float maxFacetRadius = 0f;
	
	private int nodeId = 0;
	
	/**
	 * Default constructor.
	 * Will create an uninitialized OctTree instance (no nodes).
	 */
	public OctTree () {
		root = new Node<>(0, null, 0f, 0f, 0f, 0);
		WORLD_SIZE = 2f;
		OBJECT_COUNT = 0;	
	}

	/**
	 * Constructor. Creates an initialized OctTree.
	 *  
	 * @param o root node
	 */
	public OctTree (Node<T> n) {	
		this(n, 2f, 50, 0f, 4000000);
	}
	
	/**
	 * Constructor 
	 * 
	 * @param o root node
	 * @param worldSize maximum size of the largest node allowed in the tree along a single axis
	 * 		This value should be 2x the size of the largest object to be inserted into the oct tree 
	 * @param maxDepth max number of levels deep the oct tree will allow new sub-node creation
	 * 		The default is usually good
	 * @param maxRadius maximum radius of a single node 
	 * 		worldSize / 2 is usually sufficient
	 * @param maxObjects maximum number of objects that can be inserted into the tree
	 * 		An estimate is sufficient
	 */
	public OctTree (Node<T> n, float worldSize, int maxDepth, float maxRadius, int maxObjects) {	
		root = n;
		WORLD_SIZE = worldSize;
		OBJECT_COUNT = maxObjects;	
		maxFacetRadius = maxRadius;
		MAX_DEPTH = maxDepth;
	}
	
	/**
	 * Method to return the radius of the largest facet in the Tree
	 * @return max radius
	 */
	public float getMaxFacetRadius() {
		return maxFacetRadius;
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
	public int	countNodes(Node<T> n)
	{
		int	count = 1;	// Count ourself.

		// Count descendants.
		int	i, j, k;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						count += countNodes((Node<T>)n.child[k][j][i]);
					}
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
	public void clearAllNodes(Node<T> n)
	{
		int	i, j, k;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						clearAllNodes((Node<T>)n.child[k][j][i]);
					}
				}
			}
		}
		if (n.object != null) {
			n.object.clear();
			
			if (numObjs > 0) {
				numObjs--;
			}
		}
		return;
	}
	//                           |
	// Loose octtree functions. |
	//                           V


	/**
	 * Insert the given Triangle into the initialized tree.
	 * <Description>
	 *
	 * @param t the Triangle to insert
	 * @return Returns the depth of the node the object was placed in.
	 *
	 * not thread-safe
	 */
	public int insert(T t) {
		if (root == null ) {
			root = new Node<>(0, null, 0f, 0f, 0f, 0);
		}
		return insert(root, t);		
	}

	
	
	/**
	 * Insert the given Triangle into the tree given by Node.
	 * <Description>
	 *
	 * @param o the root node of the tree to insert into
	 * @param t the Triangle to insert
	 * @return Returns the depth of the node the object was placed in.
	 *
	 * not thread-safe
	 */
	@SuppressWarnings("unchecked")
	public int insert(Node<T> n, T t)
	{
		if (n == null) {
			n = root;
		}
		// Check child nodes to see if object fits in one of them.
		if (n.depth + 1 < MAX_DEPTH) {
			float halfSize = looseK * WORLD_SIZE / (2 << n.depth+1);
			if (halfSize >= minHalfSize) {
				float offset = (WORLD_SIZE / (2 << n.depth)) / 2;
				
				if (halfSize != Float.POSITIVE_INFINITY && halfSize != Float.NEGATIVE_INFINITY && halfSize != Float.NaN) {
	
					// Pick child based on classification of object's center point.
					float[] center = t.getCenter();
					int	i = (center[X] <= n.cx) ? 0 : 1;
					int	j = (center[Y] <= n.cy) ? 0 : 1;
					int	k = (center[Z] <= n.cz) ? 0 : 1;
		
					float	cx = n.cx + ((i == 0) ? -offset : offset);
					float	cy = n.cy + ((j == 0) ? -offset : offset);
					float	cz = n.cz + ((k == 0) ? -offset : offset);
		
					if (Util3D.fitsInBox(t, cx, cy, cz, halfSize)) {
						// Recurse into this node.
						if (n.child[k][j][i] == null) {
							n.child[k][j][i] = new Node<T>(nodeId++, n, cx, cy, cz, n.depth + 1);
						}
						return insert((Node<T>)n.child[k][j][i], t);
					}
				}
			}
		}

		if (n.depth == 0 && checkRootNodeSize) {
			// we are in the root node and need to check if the SpatialRenderable fits here.
			float rootHalfSize = looseK * WORLD_SIZE / (2 << n.depth);
			float offset = (WORLD_SIZE / (2 << n.depth)) / 2;
			
			if (rootHalfSize != Float.POSITIVE_INFINITY && rootHalfSize != Float.NEGATIVE_INFINITY && rootHalfSize != Float.NaN) {
				
				float[] center = t.getCenter();
				int	i = (center[X] <= n.cx) ? 0 : 1;
				int	j = (center[Y] <= n.cy) ? 0 : 1;
				int	k = (center[Z] <= n.cz) ? 0 : 1;
	
				float	cx = n.cx + ((i == 0) ? -offset : offset);
				float	cy = n.cy + ((j == 0) ? -offset : offset);
				float	cz = n.cz + ((k == 0) ? -offset : offset);
	
				if (!Util3D.fitsInBox(t, cx, cy, cz, rootHalfSize)) {
					// the existing root node is too small we need a larger root node
					checkRootNodeSize = false;
					
					createNewRootNode(t);
					
					checkRootNodeSize = true;
					
					return 0;
				}
			}
		}
		
		// Keep object in this node.
		n.object.add(t);
		t.setNode(n);
		depthTotals[n.depth]++;
		numObjs++;
		
		if (t.getRadius() > this.maxFacetRadius) {
			maxFacetRadius = t.getRadius();
		}
		
		return n.depth;
	}
	
	/**
	 * Insert a List of SpatialRenderables into the OctTree
	 * @param all the List to insert
	 */
	public void insertAll(List<? extends T> all) {
		if (all == null) {
			throw new IllegalArgumentException("Cannot insert a null list into Oct Tree");
		}
		for (T sr : all) {
			insert(sr);
		}
	}

	/*
	 * 	Counts the objects in the ONode which might touch the given frustum, based on
	 *	the information in the loose octtree partitioning.
	 *
	 * @param o
	 * @param frustum 
	 * @param vis the desired visibility
	 * @return count of the number of objects partially or fully in the given frustum
	 *
	 * thread-safe
	 */
	@SuppressWarnings("unchecked")
	private int countObjectsInJoglFrustum(Node<T> n, com.jogamp.opengl.math.geom.Frustum frustum, Visibility vis)
	{
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);
		
		if (vis == Visibility.SomeClip) {
			AABBox box = n.getAABbox(halfSize);
			boolean notVisible = frustum.isAABBoxOutside(box);
			if (notVisible) {
				return 0;
			}
		}

		nodesChecked++;
		
		int	count = 0;
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						count += countObjectsInJoglFrustum((Node<T>)n.child[k][j][i], frustum, vis);
					}
				}
			}
		}

		// Count objects in this node.
		for (SpatialRenderable t : n.object) {
			if (Util3D.checkAgainstJoglFrustum(t, frustum) != Visibility.NotVisible) {
				objectsActuallyInFrustum++;
			}
			
			count++;
		}

		return count;
	}

	/*
	 * Counts the objects in o which might touch the given frustum, based on
	 * the information in the loose octtree partitioning.
	 *
	 * @param o the node to count
	 * @param frustum the desired visibility
	 * @param vis the desired visibility
	 *
	 * thread-safe
	 */
	@SuppressWarnings({ "unused", "unchecked" })
	private int	countObjectsInFrustum(Node<T> n, float[][] frustum, Visibility vis)
	{
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);
		
		if (vis == Visibility.SomeClip) {
			vis = Util3D.checkBoxAgainstFrustum(n.cx, n.cy, n.cz, halfSize, frustum);
			if (vis == Visibility.NotVisible) {
				return 0;
			}
		}

		nodesChecked++;
		
		int	count = 0;
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						count += countObjectsInFrustum((Node<T>)n.child[k][j][i], frustum, vis);
					}
				}
			}
		}

		// Count objects in this node.
		for (SpatialRenderable t : n.object) {
			if (Util3D.checkAgainstFrustum(t, frustum) != Visibility.NotVisible) {
				objectsActuallyInFrustum++;
			}
			
			count++;
		}

		return count;
	}
	
	/*
	 * Debug method to render the objects in a given node
	 *
	 * @param gl Current JOGL Profile/Context
	 * @param o the node to render
	 *
	 * not thread-safe
	 */
	// Render the objects in the frustum.
	@SuppressWarnings("unchecked")
	public void renderLooseNode(GL2 gl, Node<T> n) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);
		
		float	d = (n.depth / 5.0f) + 0.2f;
		if (d > 1) d = 1;

		// Draw this node.
		gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
		gl.glColor3f(0f, d, 0f);
		gl.glBegin(GL2.GL_QUADS);
		
		// closest in XY plane
		gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz - halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz - halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz - halfSize);
		gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz - halfSize);
		
		// farthest in XY plane
		gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz + halfSize);
		gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz + halfSize);
		gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz + halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz + halfSize);
		
		
		// closest in YZ plane
		gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz + halfSize);
		gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz - halfSize);
		gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz - halfSize);
		gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz + halfSize);
		
		// farthest in YZ plane
		gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz - halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz + halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz + halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz - halfSize);
		
		// closest in XZ plane
		gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz - halfSize);
		gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz + halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz + halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz - halfSize);
		
		// farthest in XZ plane
		gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz - halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz - halfSize);
		gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz + halfSize);
		gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz + halfSize);
		
		gl.glEnd();
		
		// Render objects in this node.
		for (SpatialRenderable t : n.object) {
			gl.glColor3f(0.5f, 0.0f, 0.5f);
			t.render(gl);
		}
	}

	/**
	 * Debug method to render the objects in a node and its children that fully or partially fit within the given frustum. 
	 *
	 * @param gl Current JOGL Profile/Context
	 * @param o the node to render
	 * @param frustum
	 * @param vis the desired visibility
	 *
	 * not thread-safe
	 */
	// Render the objects in the frustum.
	@SuppressWarnings("unchecked")
	public void renderLoose(GL2 gl, Node<T> n, float[][] frustum, Visibility vis, boolean showChildren, boolean showNodes)
	{
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);
		
		if (vis != null && vis == Visibility.SomeClip) {
			vis = Util3D.checkBoxAgainstFrustum(n.cx, n.cy, n.cz, halfSize, frustum);
			if (vis == Visibility.NotVisible) {
				return;
			}
		}

		float	d = (n.depth / 7.0f) + 0.2f;
		if (d > 1) d = 1;

		if (showNodes) {
			gl.glPushMatrix();
			// Draw this node.
			gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
			gl.glColor4f(0f, d, 0f, 0.25f);
			gl.glBegin(GL2.GL_TRIANGLES);
			
			// closest in XY plane
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz - halfSize);
			
			// farthest in XY plane
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz + halfSize);
			
			
			// closest in YZ plane
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz + halfSize);
			
			// farthest in YZ plane
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz - halfSize);
			
			// closest in XZ plane
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz - halfSize);
			
			// farthest in XZ plane
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz + halfSize);
			
			gl.glEnd();
			
			gl.glPopMatrix();
		}
		
		// Render objects in this node.
		if (showNodeContents) {
			for (SpatialRenderable t : n.object) {
					gl.glColor3f(1f, 1f, 0f);
					t.render(gl);
			}
	
			if (showChildren) {
				// Render children.
				int	k, j, i;
				for (k = 0; k < 2; k++) {
					for (j = 0; j < 2; j++) {
						for (i = 0; i < 2; i++) {
							if (n.child[k][j][i] != null) {
								renderLoose(gl, (Node<T>)n.child[k][j][i], frustum, vis, showChildren, showNodes);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Debug method to render the objects in a node and its children that fully or partially fit within the given frustum. 
	 *
	 * @param gl Current JOGL Profile/Context
	 * @param o the node to render
	 * @param frustum
	 * @param vis the desired visibility
	 *
	 * not thread-safe
	 */
	// Render the objects in the frustum.
	@SuppressWarnings("unchecked")
	public void renderAllNodes(GL2 gl, Node<T> n, boolean showChildren, boolean showNodes)
	{
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);
		
		float	d = (n.depth / 7.0f) + 0.2f;
		if (d > 1) d = 1;

		if (showNodes) {
			gl.glPushMatrix();
			// Draw this node.
			gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
			gl.glColor4f(0f, d, 0f, 0.25f);
			gl.glBegin(GL2.GL_TRIANGLES);
			
			// closest in XY plane
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz - halfSize);
			
			// farthest in XY plane
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz + halfSize);
			
			
			// closest in YZ plane
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz + halfSize);
			
			// farthest in YZ plane
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz - halfSize);
			
			// closest in XZ plane
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy - halfSize, n.cz - halfSize);
			
			// farthest in XZ plane
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz - halfSize);
			gl.glVertex3f(n.cx + halfSize, n.cy + halfSize, n.cz + halfSize);
			gl.glVertex3f(n.cx - halfSize, n.cy + halfSize, n.cz + halfSize);
			
			gl.glEnd();
			
			gl.glPopMatrix();
		}
		
		// Render objects in this node.
		if (showNodeContents) {
			for (SpatialRenderable t : n.object) {
					gl.glColor3f(1f, 1f, 0f);
					t.render(gl);
			}
	
			if (showChildren) {
				// Render children.
				int	k, j, i;
				for (k = 0; k < 2; k++) {
					for (j = 0; j < 2; j++) {
						for (i = 0; i < 2; i++) {
							if (n.child[k][j][i] != null) {
								renderAllNodes(gl, (Node<T>)n.child[k][j][i], showChildren, showNodes);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Debug method to render the objects in a node and its children that fully or partially fit within the given frustum. 
	 *
	 * @param gl Current JOGL Profile/Context
	 * @param o the node to render
	 * @param frustum
	 * @param vis the desired visibility
	 *
	 * not thread-safe
	 */
	// Render the objects in the frustum.
	@SuppressWarnings("unchecked")
	public void renderAllNodesAsLines(GL2 gl, Node<T> n, boolean showChildren, boolean showNodes) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);
		
		float	d = (n.depth / 7.0f) + 0.2f;
		if (d > 1) d = 1;
		
		float[][] octantPoints = octantSizeToPoints(new float[] {n.cx, n.cy, n.cz}, halfSize);

		if (showNodes) {
			gl.glPushMatrix();			
			// Draw this node.
			gl.glColor4f(0f, d, 0f, 0.25f);
			gl.glLineWidth(3);
			gl.glBegin(GL2.GL_LINES);
	
			// 0 to 1
			gl.glVertex3d(octantPoints[0][0], octantPoints[0][1], octantPoints[0][2]);
			gl.glVertex3d(octantPoints[1][0], octantPoints[1][1], octantPoints[1][2]);
	
			// 1 to 2
			gl.glVertex3d(octantPoints[1][0], octantPoints[1][1], octantPoints[1][2]);
			gl.glVertex3d(octantPoints[2][0], octantPoints[2][1], octantPoints[2][2]);
	
			// 2 to 3
			gl.glVertex3d(octantPoints[2][0], octantPoints[2][1], octantPoints[2][2]);
			gl.glVertex3d(octantPoints[3][0], octantPoints[3][1], octantPoints[3][2]);
			
			// 3 to 0
			gl.glVertex3d(octantPoints[3][0], octantPoints[3][1], octantPoints[3][2]);
			gl.glVertex3d(octantPoints[0][0], octantPoints[0][1], octantPoints[0][2]);
	
			// 4 to 5
			gl.glVertex3d(octantPoints[4][0], octantPoints[4][1], octantPoints[4][2]);
			gl.glVertex3d(octantPoints[5][0], octantPoints[5][1], octantPoints[5][2]);
			
			// 5 to 6
			gl.glVertex3d(octantPoints[5][0], octantPoints[5][1], octantPoints[5][2]);
			gl.glVertex3d(octantPoints[6][0], octantPoints[6][1], octantPoints[6][2]);
					
			// 6 to 7
			gl.glVertex3d(octantPoints[6][0], octantPoints[6][1], octantPoints[6][2]);
			gl.glVertex3d(octantPoints[7][0], octantPoints[7][1], octantPoints[7][2]);
			
			// 7 to 4
			gl.glVertex3d(octantPoints[7][0], octantPoints[7][1], octantPoints[7][2]);
			gl.glVertex3d(octantPoints[4][0], octantPoints[4][1], octantPoints[4][2]);
	
			// 0 to 4
			gl.glVertex3d(octantPoints[0][0], octantPoints[0][1], octantPoints[0][2]);
			gl.glVertex3d(octantPoints[4][0], octantPoints[4][1], octantPoints[4][2]);
	
			// 1 to 5
			gl.glVertex3d(octantPoints[1][0], octantPoints[1][1], octantPoints[1][2]);
			gl.glVertex3d(octantPoints[5][0], octantPoints[5][1], octantPoints[5][2]);
	
			// 2 to 6
			gl.glVertex3d(octantPoints[2][0], octantPoints[2][1], octantPoints[2][2]);
			gl.glVertex3d(octantPoints[6][0], octantPoints[6][1], octantPoints[6][2]);
	
			// 3 to 7
			gl.glVertex3d(octantPoints[3][0], octantPoints[3][1], octantPoints[3][2]);
			gl.glVertex3d(octantPoints[7][0], octantPoints[7][1], octantPoints[7][2]);
	
			gl.glEnd();
			
			gl.glPopMatrix();
		}
		
		// Render objects in this node.
		if (showNodeContents) {
			for (SpatialRenderable t : n.object) {
					gl.glColor3f(1f, 1f, 0f);
					t.render(gl);
			}
	
			if (showChildren) {
				// Render children.
				int	k, j, i;
				for (k = 0; k < 2; k++) {
					for (j = 0; j < 2; j++) {
						for (i = 0; i < 2; i++) {
							if (n.child[k][j][i] != null) {
								renderAllNodesAsLines(gl, (Node<T>)n.child[k][j][i], showChildren, showNodes);
							}
						}
					}
				}
			}
		}
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
	private boolean looseNodeNodeIntersect(Node<T> a, Node<T> b) {
		
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
		if (a.getMinZ(aHalfSize) > b.getMaxZ(bHalfSize)) {
			return false;
		}
		if (a.getMaxZ(aHalfSize) < b.getMinZ(bHalfSize)) {
			return false;
		}
		
		// we have overlap on all 3 axes so we have an intersection
		return true;
	}

	/**
	 * Prints the number of nodes at each depth for the Octtree to System.err
	 *
	 * thread-safe
	 */
	public void printDepthTotals()
	{
		log.aprintln("Oct Tree Depth Totals:");
		for (int i = 0; i < MAX_DEPTH; i++) {
			if (depthTotals[i] != 0) {
				log.aprintln("ct["+i+"] = "+depthTotals[i]);
			}
		}
	}
	
	/**
	 * Resets depth totals to zero
	 */
	public void resetDepthTotals() {
		if (depthTotals == null) {
			depthTotals = new int[MAX_DEPTH];
		}
		for (int i = 0; i < MAX_DEPTH; i++) {
			depthTotals[i] = 0;
		}
		
	}

	/**
	 * Returns the root ONode of the OctTree
	 *
	 * @return root node
	 *
	 * thread-safe
	 */
	public Node<T> getRoot() {
		return root;
	}

	/**
	 * Search an Node and all its children for Triangles that are intersected by the input Ray
	 *
	 * @param ray the Ray to intersect the node with
	 * @param n the Node to test for intersection
	 * @param list List of all the Triangles intersected by the Ray
	 *
	 * possibly thread-safe
	 */
	@SuppressWarnings("unchecked")
	public void rayIntersectShortCircuit(RayWithEpsilon ray, Node<T> n, ArrayList<IntersectResultData<SpatialRenderable>> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		com.jogamp.opengl.math.Ray nRay = new com.jogamp.opengl.math.Ray();
		nRay.orig[X] = ray.origin[X];
		nRay.orig[Y] = ray.origin[Y];
		nRay.orig[Z] = ray.origin[Z];
		float[] direction = VectorUtil.normalizeVec3(new float[3], ray.direction);
		nRay.dir[X] = direction[X];
		nRay.dir[Y] = direction[Y];
		nRay.dir[Z] = direction[Z];
		
		AABBox box = n.getAABbox(halfSize);

		if (!box.intersectsRay(nRay)) {
			return;
		}

		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						rayIntersectShortCircuit(ray, (Node<T>)n.child[k][j][i], list);
					}
				}
			}
		}

		// Count objects in this node.
		if (n.object != null) {
			for (SpatialRenderable o : n.object) {
	
				IntersectResultData<SpatialRenderable> result = o.intersectRay(ray);
				
				if (result != null) {
					list.add(result);
				} 
			}
		}
	}

	/**
	 * Returns the closest SpatialRenderable to the origin of the Ray
	 * @param ray
	 * @param n start node 
	 * @param list the entire intersection result
	 * @return nearest SpatialRenderable
	 */
	public SpatialRenderable rayIntersectNearest(RayWithEpsilon ray, Node<T> n, ArrayList<IntersectResultData<SpatialRenderable>> list) {
		rayIntersectShortCircuit(ray, n, list);
		
        SpatialRenderable result = null;
        
        if (list.isEmpty()) {
        	return result;
        }
        
        Collections.sort(list, new Comparator<IntersectResultData<SpatialRenderable>>() {

            public int compare(IntersectResultData<SpatialRenderable> o1, IntersectResultData<SpatialRenderable> o2) {

                float x1 = o1.intersectDistance;
                float x2 = o2.intersectDistance;
                int sComp = Float.compare(x1, x2);

                if (sComp != 0) {
                   return sComp;
                } 
                
                float z1 = o1.distanceToCenter;
                float z2 = o1.distanceToCenter;
                int cComp = Float.compare(z1, z2);
                
                if (cComp != 0) {
                	return cComp;
                }               

                float y1 = o1.distanceToRayOrigin;
                float y2 = o2.distanceToRayOrigin;
                return Float.compare(y1, y2);
        }});
        
		return list.get(0).object;
	}
	/*
	 * Tests for intersection of the input Ray with the axis-aligned bounding box of the input ONode
	 *
	 * @param ray the Ray to test
	 * @param n the ONode to intersect test
	 * @return true if the Ray intersects the axis-aligned bounding box of the ONode
	 *
	 * probably thread-safe
	 */
	public boolean looseRayNodeIntersectTest(RayWithEpsilon ray, Node<T> n) {	
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		float intersects = Util3D.rayIntersectBox(ray.origin, ray.direction, n.cx, n.cy, n.cz, halfSize, null);
		if (Float.compare(intersects, Float.MAX_VALUE) < 0) {
			return true;
		} else {
			return false;
		}
	}
		
	/*
	 * Method to return the number of triangles in the OctTree
	 *
	 * @return number of triangles
	 *
	 * <thread-safe?>
	 */
	private int getSize() {
		return OBJECT_COUNT;
	}
	
	/**
	 * Method to return all the Objects of the shape model currently in use, if one is in use.
	 *
	 * @return a Map of all the Facets in the shape model in the form of Triangle objects
	 *
	 * probably thread-safe
	 */
	public List<SpatialRenderable> getAllObjects() {
		ArrayList<SpatialRenderable> list = new ArrayList<>();
		if (this.root != null) {
			this.looseGetAllObjects(this.root, list);
		}
		return list;
	}
	
	@SuppressWarnings("unchecked")
	private void looseGetAllObjects(Node<T> n, ArrayList<SpatialRenderable> list) {
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseGetAllObjects((Node<T>) n.child[k][j][i], list);
					}
				}
			}
		}

		for (SpatialRenderable o : n.object) {
			list.add(o);
		}
	}
	
	/**
	 * Method to return a count of all the Objects in the shape model.
	 *
	 * @return a count of all the objects in the oct tree
	 *
	 * probably thread-safe
	 */
	public Integer getObjectCount() {
		Integer count = 0;
		if (this.root != null) {
			this.looseGetObjectCount(this.root, count);
		}
		return count;
	}
	
	private void looseGetObjectCount(Node<T> n, Integer count) {
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						looseGetObjectCount((Node<T>) n.child[k][j][i], count);
					}
				}
			}
		}
		int objs = n.getObjects().size();
		if (objs > 0) {
			count += objs;
		}
	}
	
	
/**
 * Method to intersect a frustum with the OctTree and return all SpatialRenderables that fall within the frustum using a point by point test.
 * @param normal surface normal to include only SpatialRenderables facing the large end of the frustum
 * @param frustum 
 * @param n OctTree node starting point
 * @param list return list of entirely contained SpatialRenderables
 */
	@SuppressWarnings("unchecked")
	public void frustumIntersect(float[] normal, float[][] frustum, Node<T> n, ArrayList<SpatialRenderable> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		// check to see if this node is in the frustum
		if (Util3D.cubeInAnyFrustum(n.cx, n.cy, n.cz, halfSize, frustum) == 0) {
			// nothing to do here node is outside the frustum
			return;
		}
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						frustumIntersect(normal, frustum, (Node<T>)n.child[k][j][i], list);
					}
				}
			}
		}

		// test every object in the node and if any part of the object is inside the frustum add to the return list.
		for (SpatialRenderable t : n.object) {
			if (t.intersectFrustum(normal, frustum)) {
				list.add(t);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void frustumIntersect(float[] normal, Cuboid cube, Node<T> n, ArrayList<SpatialRenderable> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		// convert the octant from center/size format to eight corner points
		
		float[][] octantPoints = octantSizeToPoints(new float[] {n.cx, n.cy, n.cz}, halfSize);
		float[][] octFrustum = Util3D.getFrustumFromCube(octantPoints);
		
		// check to see if this node is in the frustum
		if (Util3D.cubeInAnyFrustum(n.cx, n.cy, n.cz, halfSize, cube.getFrustum()) == 0 && Util3D.cuboidInAnyFrustum(cube, octFrustum) == 0) {
			// nothing to do here node is outside the frustum
			return;
		}
		
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						frustumIntersect(normal, cube, (Node<T>)n.child[k][j][i], list);
					}
				}
			}
		}

		// test every object in the node and if any part of the object is inside the frustum add to the return list.
		for (SpatialRenderable t : n.object) {
			if (t.intersectFrustum(normal, cube.getFrustum())) {
				list.add(t);
			}
		}
	}

	/**
	 * Method to intersect a frustum with the OctTree and return all SpatialRenderables that fall within the frustum using a polygon test using JOGL objects.
	 * @param normal surface normal to include only SpatialRenderables facing the large end of the frustum
	 * @param frustum
	 * @param n OctTree node starting point
	 * @param list return list of entirely contained SpatialRenderables
	 */
	@SuppressWarnings("unchecked")
	public void joglFrustumIntersect(float[] normal, Frustum frustum, Node<T> n, ArrayList<SpatialRenderable> list) {
		float halfSize = looseK * WORLD_SIZE / (2 << n.depth);	
		
		if(frustum.isAABBoxOutside(new AABBox(new float[] {n.cx-halfSize, n.cy-halfSize, n.cz-halfSize}, new float[] {n.cx+halfSize, n.cy+halfSize, n.cz+halfSize}))) {
			return;
		}
		// Check children.
		int	k, j, i;
		for (k = 0; k < 2; k++) {
			for (j = 0; j < 2; j++) {
				for (i = 0; i < 2; i++) {
					if (n.child[k][j][i] != null) {
						joglFrustumIntersect(normal, frustum, (Node<T>)n.child[k][j][i], list);
					}
				}
			}
		}

		// Count objects in this node.
		for (SpatialRenderable o : n.object) {
			if (o.intersectJoglFrustum(normal, frustum)) {
				list.add(o);
			}
		}
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

	@Override
	public void execute(GL2 gl) {
		if (root != null) {
			renderAllNodesAsLines(gl, root, true, true);
		}
		
	}

	@Override
	public void preRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public float getAlpha() {
		return 1f;
	}

	@Override
	public float getDisplayAlpha() {
		return 1f;
	}

	@Override
	public void setDisplayAlpha(float alpha) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isScalable() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void scaleByDivision(float scalar) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void scaleToShapeModel(boolean canScale) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isScaled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Method to create a new root node and add everything in the current tree into the new node.
	 * @param t
	 */
	@SuppressWarnings("unchecked")
	public void createNewRootNode(T t) {
		// determine size of new root node
		float[][] pts = t.getPoints();
		if (pts == null) {
			return;
		}
		float maxMag = 0;
		for (float[] p : pts) {
			float magx2 = VectorUtil.normSquareVec3(p);
			if (magx2 > maxMag) {
				maxMag = magx2;
			}
		}
		maxMag = FloatUtil.sqrt(maxMag);
	
		if (maxMag <= 0) {
			DebugLog.instance().aprintln("Error creating a replacement OctTree root node with a size of zero!");
			return;
		}
		
		if (this.WORLD_SIZE > (maxMag * 2)) {
			DebugLog.instance().aprintln("Error creating a replacement OctTree root node with a world size less than the existing world size!");
			return;
		}
		
		this.WORLD_SIZE = maxMag * 2;
		this.maxFacetRadius = t.getRadius();
		
		ArrayList<SpatialRenderable> tlist = new ArrayList<>();
		looseGetAllObjects(root, tlist);		
		System.err.println("orginal object count "+tlist.size());
		
		Node<T> oldRoot = this.root;
		int oldDepth = oldRoot.depth;
		root = new Node<>(0, null, 0f, 0f, 0f, 0);	
		resetDepthTotals();

		insert(root, t);
		
		for (SpatialRenderable sr : tlist) {
			sr.setNode(null);
			insert(root, (T)sr);
		}
		clearAllNodes(oldRoot);
	}

	@Override
	public float[] getColor() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
