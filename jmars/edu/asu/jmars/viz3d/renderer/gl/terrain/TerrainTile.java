/**
 * 
 */
package edu.asu.jmars.viz3d.renderer.gl.terrain;

import java.awt.geom.Rectangle2D;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;

import com.jogamp.opengl.math.VectorUtil;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.viz3d.core.geometry.Triangle;

/**
 * A container Class to represent all the geometry and extent required to apply a Decal (Texture) to a specific triangle mesh
 *
 */
public class TerrainTile implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1562994904992929675L;
	private Rectangle2D extent;
	private float[] verts;
	private float[] norms;
	private int[] indices;
	private float[] texs;
	private float[] corners; // 3 space [X, Y, Z]
	private float[] lonLats;
	private volatile ArrayList<Triangle> facets = new ArrayList<>();
	private boolean isPolar;
	private boolean isMeridian;
	private transient ProjObj proj;
	private float projLonCenter;
	private float projLatCenter;	
    
	/**
	 * Default TerrainTile size
	 */
    public static final int TILE_SIZE = 512; // needs to be a power of 2
    /** 
     * Default contructor
     */
    public TerrainTile() {
    	
    }
    
    /**
     * Constructor
     * @param cornerPts four 3D corner points of a tile. 
     *        Points may be duplicated for tiles whose top or bottom edge are at a pole.
     * @param vertices the vertices of all the 3D geometry (triangles) the Decal will eventually be applied to
     * @param textureCoords the Texture coordinates that map regions of the Decal image to the geometry
     * @param indexes array of indices that map vertices in the vertex array to texture coords in the texture coords array 
     */
    public TerrainTile(float[] cornerPts, float[] vertices, float[] textureCoords, int[] indexes, float[] lonLats) {
	    	corners = cornerPts;
	    	verts = vertices;
	    	texs = textureCoords;
	    	indices = indexes;
	    	this.lonLats = lonLats;
	    	generateNorms();
    }

    /**
     * Unique ID for this class
     * @return
     */
	public int getId() {
		return System.identityHashCode(this);
	}

//	public float[][] getFrustum() {
//		if (corners == null) {
//			return null;
//		}
//				
//		return null;
//	}
	
	private void generateNorms() {
		if (verts != null && verts.length > 3) {
			// first pass, we'll try a normal for each vertex
			norms = new float[verts.length];
			for (int i=0; i<verts.length; i+=3) {
				float[] tmp = VectorUtil.normalizeVec3(new float[3], new float[] {verts[i], verts[i+1], verts[i+2]});
				norms[i] = tmp[0];
				norms[i+1] = tmp[1];
				norms[i+2] = tmp[2];
			}
		}
	}

	/**
	 * @return the extent
	 */
	public Rectangle2D getExtent() {
		return extent;
	}

	/**
	 * @param extent the extent to set
	 */
	public void setExtent(Rectangle2D extent) {
		this.extent = extent;
	}

	/**
	 * @return the corners
	 */
	public float[] getCorners() {
		return corners;
	}
	
	public float[] getLonLats() {
		return lonLats;
	}

	/**
	 * @param corners the corners to set
	 */
	public void setCorners(float[] corners) {
		this.corners = corners;
	}

	/**
	 * @return the verts
	 */
	public float[] getVerts() {
		return verts;
	}

	/**
	 * @return the norms
	 */
	public float[] getNorms() {
		if (norms == null && verts != null && verts.length > 3) {
			generateNorms();
		}
		return norms;
	}

	/**
	 * @param verts the verts to set
	 */
	public void setVerts(float[] verts) {
		this.verts = verts;
	}

	/**
	 * @return the indices
	 */
	public int[] getIndices() {
		return indices;
	}
	
	public void setLonLats(float[] ll) {
		lonLats = ll;
	}
	
	public synchronized void addFacet(Triangle tri) {
		facets.add(tri);
	}
	
	public ArrayList<Triangle> getFacets() {
		return facets;
	}

	/**
	 * @param indices the indices to set
	 */
	public void setIndices(int[] indices) {
		this.indices = indices;
	}

	/**
	 * @return the texs
	 */
	public float[] getTexs() {
		return texs;
	}

	/**
	 * @param texs the texs to set
	 */
	public void setTexs(float[] texs) {
		this.texs = texs;
	}
	
	/**
	 * @return the isBoolean
	 */
	public boolean isPolar() {
		return isPolar;
	}

	/**
	 * @param isBoolean the isBoolean to set
	 */
	public void setPolar(boolean isPolar) {
		this.isPolar = isPolar;
	}

	public boolean isMeridian() {
		return isMeridian;
	}

	/**
	 * @param isBoolean the isBoolean to set
	 */
	public void setMeridian(boolean isMeridian) {
		this.isMeridian = isMeridian;
	}
	
	public void setProjection(ProjObj proj) {
		this.proj = proj;
	}
	
	public ProjObj getProjection() {
		return proj;
	}
	/**
	 * @return the projLonCenter
	 */
	public float getProjLonCenter() {
		return projLonCenter;
	}

	/**
	 * @param projLonCenter the projLonCenter to set
	 */
	public void setProjLonCenter(float projLonCenter) {
		this.projLonCenter = projLonCenter;
	}

	/**
	 * @return the projLatCenter
	 */
	public float getProjLatCenter() {
		return projLatCenter;
	}

	/**
	 * @param projLatCenter the projLatCenter to set
	 */
	public void setProjLatCenter(float projLatCenter) {
		this.projLatCenter = projLatCenter;
	}
	
	Object readResolve() throws ObjectStreamException {
		this.proj = new ProjObj.Projection_OC(projLonCenter, projLatCenter);
		
		return this;
	}

	/**
	 * Generic resource cleanup method
	 */
	public void dispose() {
		extent = null;
		verts = null;
		indices = null;
		texs = null;
		corners = null; 
	}
}
