package edu.asu.jmars.viz3d.core.geometry;

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPInputStream;

import javax.swing.JOptionPane;

import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.common.nio.Buffers;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.threed.Vec3dMath;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.renderer.textures.Decal;
import edu.asu.jmars.viz3d.renderer.textures.DecalSet;
import edu.asu.jmars.viz3d.renderer.textures.GlobalDecalFactory;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.gl.OutLineType;
import edu.asu.jmars.viz3d.renderer.gl.PolygonType;
import edu.asu.jmars.viz3d.renderer.gl.outlines.OutLine;
import edu.asu.jmars.viz3d.renderer.input.files.Obj;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * This class represents a triangle mesh in JOGL.
 * It should be instantiated by a factory method.
 * It should be used to create and render a triangle mesh
 * generated from one of the following:
 *  1) a SPICE DSK shape model
 *  2) an OBJ file
 *  3) a single dimension array of vertices and a matching single dimension
 *  	array of triangle indices 
 *
 * This class is dependent on the following classes:
 * ThreeDManager
 * JOGL 2.3.1 or higher
 *
 * This class can be used in a multi-threaded environment as long as 
 * the usual Java Swing consideration in a multi-threaded environment are observed.
 */
public class TriangleMesh implements GLRenderable, Disposable {
	
	private static final int X = 0;
	private static final int Y = 1;
	private static final int Z = 2;
	
	private int[] indices;			// base array of triangle indices
	private float[] vertices;		// base array of triangle vertices
	private float[] normals;		// base array of vertex normal vectors
	private int[] facetIds;			// FITS file facet IDs
	private float[] colors;			// derived FITS facet colors
	private double[] polarWorldCoords;	// polar world coordinates
	private double[] equWorldCoords;	// equatorial world coordinates
	
	private IntBuffer idxBuffer;	// triangle indices buffer
	private FloatBuffer vBuffer;	// triangle vertices buffer
	private FloatBuffer nBuffer;	// triangle vertex normal vector buffer
	private FloatBuffer cBuffer;	// triangle vertex color buffer
	private FloatBuffer tBuffer;	// triangle textures buffer
	
	private IntBuffer polyBuffer;	// decal polygon buffer
	private FloatBuffer directVBuf;	// triangle vertices buffer for non-VBO rendering
	private FloatBuffer directTBuf;	// triangle texture buffer for non-VBO rendering
	
    private float[] lightDiffuse =  { 0.1f, 0.1f, 0.1f, 1.0f };
    private float[] lightAmbient =   { 0.1f, 0.1f, 0.1f, 1.0f };
//    private float[] lightAmbient =   { 0.5f, 0.5f, 0.5f, 1.0f };
    private float[] lightEmission =  { 0f, 0f, 0f, 1.0f };
    private float[] lightSpecular =  { 0.8f, 0.8f, 0.8f, 1.0f };

	
    private ArrayList<DecalSet> decalsToDelete = new ArrayList<DecalSet>(); // dynamic list of decals to delete as in garbage collect
    
    private Object lock = new Object();		// for synchronized operations

	private float[] maxCoords = new float[3];
	private float[] minCoords = new float[3];
	
    float minLen = Float.MAX_VALUE;
    float maxLen = 0f;
    float avgLen = 0f;
    float stdDev = 0f;
    
	private int[] bufferObjs = new int[5];    

    private boolean vboDone = false;
    
    boolean VBO = true;
    
	private final int SHAPE_VERTEX_DATA = 1;
	private final int SHAPE_INDEX_DATA = 2;
	private final int SHAPE_NORMAL_DATA = 3;
	private final int SHAPE_COLOR_DATA = 4;
	
	private final float UPPER_UNITY_SCALE= 2f;
	private final float LOWER_UNITY_SCALE= 0.9f;
	
	boolean dispose = false;
	boolean drawGrid = false;
	boolean drawBody = true;
	boolean drawPoints = false;
	boolean drawOctTree = false;
    boolean clearDecals = false;
    boolean deleteDecals = false;
    boolean isRendered = false;
    private boolean hasBeenScaled = false;

	
	boolean drawObj = false;
	boolean updateColors = false;

    private static DebugLog log = DebugLog.instance();
    
    private OctTree oct;
    private ONode root = new ONode(null, 0, 0, 0, 0);
    
    private String meshName;
    
    private Ellipsoid soid;
    
    GLU glu;
    private boolean isUnitSphere = false;
 
    private float alpha = 1f;
    private float maxFacetRadius = 0f;
    private Float displayAlpha;
    
    private boolean displayTileGrid = false;
	private int[][] decalColors = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}, {1, 1, 0}};
	private ArrayList<Decal> renderDecals = null;
	private ArrayList<Integer> decalSetIds = new ArrayList<>();
	private boolean rebuildDecals = false;
    
    /** 
     * Default constructor 
     * **/
    public TriangleMesh() {
    	
    }
    
    /**
     * This constructor will use the Ellipsoid as the renderable part of the mesh
     * and use the Vertex/Facet shape model in the meshFile for fitting and all data analysis
     * @param meshFile Vertex/Facet shape model in text format
     * @param body Ellipsoid of the shape to represent the mesh when rendering
     * @param meshName name of the meshFile to use with ThreeDManager as the key for this shape model
     * @throws IOException
     * @throws URISyntaxException 
     */
    public TriangleMesh(String meshFile, Ellipsoid body, String meshName) throws IOException, URISyntaxException {
        loadMeshFile(meshFile);
        this.meshName = meshName;
		soid = body;
		
		float[] unrolledVerts = new float[indices.length * 3];
		int k = 0;
		for (int j=0; j<indices.length; j++) {
			unrolledVerts[k++] = vertices[indices[j]*3]; 
			unrolledVerts[k++] = vertices[indices[j]*3+1]; 
			unrolledVerts[k++] = vertices[indices[j]*3+2]; 
		}		
		
		int[] unrolledIndices = new int[indices.length];
		k=0;
		for (int i=0; i<indices.length; i++) {
			unrolledIndices[k++] = i;
		}
		
		this.getUnrolledVertexNormals(unrolledVerts);
		this.calcMinMaxAvgMagnitudes(unrolledVerts);
		vertices = unrolledVerts;
		indices = unrolledIndices;
		computeVertexColors();

		this.realizeOcttree(false);
		ThreeDManager.getInstance().switchLight(false);
    }
    
    public TriangleMesh(File meshFile, String meshName, boolean isUnitSphere) throws IOException {
        this.meshName = meshName;
        this.isUnitSphere = isUnitSphere;
    	loadMeshFromZippedBinary(meshFile);
		
		hasBeenScaled = true;
    }
    
	public TriangleMesh (Obj obj) throws IOException{
		vertices = obj.getVertices();
		indices = obj.getTriangles();
        this.meshName = obj.getName();
		
		float[] unrolledVerts = new float[indices.length * 3];
		int k = 0;
		for (int j=0; j<indices.length; j++) {
			unrolledVerts[k++] = vertices[indices[j]*3]; 
			unrolledVerts[k++] = vertices[indices[j]*3+1]; 
			unrolledVerts[k++] = vertices[indices[j]*3+2]; 
		}		
		
		int[] unrolledIndices = new int[indices.length];
		k=0;
		for (int i=0; i<indices.length; i++) {
			unrolledIndices[k++] = i;
		}
		
		this.getUnrolledVertexNormals(unrolledVerts);
		this.calcMinMaxAvgMagnitudes(unrolledVerts);
		vertices = unrolledVerts;
		indices = unrolledIndices;
		computeVertexColors();

		this.realizeOcttree(true);
		ThreeDManager.getInstance().switchLight(false);
	}
	
	
	private void loadMeshFromZippedBinary(File file) throws IOException {
		try {
			FileInputStream fis = new FileInputStream(file);
			GZIPInputStream zipReader = new GZIPInputStream(fis);
			DataInputStream data = new DataInputStream(zipReader);
			
			// Read File Line By Line
			int vertexCnt = data.readInt();
			int plateCnt = data.readInt();

//			System.err.println("number of vertices "+vertexCnt);			
//			System.err.println("number of plates "+plateCnt);
			
			float [] vertices = new float[vertexCnt * 3];
			byte[] verts = new byte[Float.SIZE/Byte.SIZE * vertexCnt * 3];
			data.readFully(verts, 0, verts.length);
			ByteBuffer vb = ByteBuffer.wrap(verts);
			for (int i=0; i<vertices.length; i++) {
				vertices[i] = vb.asFloatBuffer().get(i);
			}
			vb.clear();
			vb = null;
			
			int[] indices = new int[plateCnt * 3];
			
			byte[] tris = new byte[Integer.SIZE/Byte.SIZE * plateCnt * 3];
			data.readFully(tris, 0, tris.length);
			
			ByteBuffer tb = ByteBuffer.wrap(tris);
			for (int j=0; j<indices.length; j++) {
				indices[j] = tb.asIntBuffer().get(j);
			}
			
			tb.clear();
			tb = null;
			float[] unrolledVerts = new float[indices.length * 3];
			int k = 0;
			for (int j=0; j<indices.length; j++) {
				unrolledVerts[k++] = vertices[indices[j]*3]; 
				unrolledVerts[k++] = vertices[indices[j]*3+1]; 
				unrolledVerts[k++] = vertices[indices[j]*3+2]; 
			}		
			
			int[] unrolledIndices = new int[indices.length];
			k=0;
			for (int i=0; i<indices.length; i++) {
				unrolledIndices[k++] = i;
			}
			this.getUnrolledVertexNormals(unrolledVerts);
			this.calcMinMaxAvgMagnitudes(unrolledVerts);
			this.vertices = unrolledVerts;
			this.indices = unrolledIndices;
			if (maxLen > this.UPPER_UNITY_SCALE || maxLen < this.LOWER_UNITY_SCALE) { 
				scaleToUnity();
				this.isUnitSphere = true;
				this.calcMinMaxAvgMagnitudes(this.vertices);
			}
			computeVertexColors();
			realizeOcttree(false);
			ThreeDManager.getInstance().switchLight(false);
			// Close the input streams
			data.close();
			zipReader.close();
			fis.close();
		} catch (IOException e) {
			log.aprint(e);
			throw e;
		}
			
	}

	/*
	 * Method to load a TriangleMesh from a Vertex/Facet file
	 * 
	 * @param mesh Vertex/Facet file to read from
	 */
	private void loadMeshFile(String mesh) throws IOException {
		try {
		    BufferedReader br = new BufferedReader(new InputStreamReader(Main.getResourceAsStream(mesh), "UTF-8"));			
			String strLine;
			String delims = "[ ]+";

			// Read File Line By Line
			int vertexCnt = 0;
			int plateCnt = 0;
			
			strLine = br.readLine();
			strLine = strLine.trim();
			String[] toks = strLine.split(delims);
			if (toks.length != 1) {
				log.aprintln("Missing number of vertices in the input shape file: "+mesh);
				br.close();
				return;
			}
			
			vertexCnt = Integer.parseInt(toks[0]);
			
			float [] vertices = new float[vertexCnt * 3];
			int vertIdx = 0;
			
			for (int i = 0; i < vertexCnt; i++) {
				strLine = br.readLine();
				strLine = strLine.trim();
				String[] tokens = strLine.split(delims);
				if (tokens.length != 4) {
					log.aprintln("Incorrect number of vertices in the input shape file: "+mesh+" on line: "+(i+1));
					br.close();
					return;
				}
				vertices[vertIdx++] = (float)(Double.parseDouble(tokens[1]));
				vertices[vertIdx++] = (float)(Double.parseDouble(tokens[2]));
				vertices[vertIdx++] = (float)(Double.parseDouble(tokens[3]));
			}

			
			strLine = br.readLine();
			strLine = strLine.trim();
			if (strLine == null || strLine.length() < 1) {
				log.aprintln("Missing number of facets in the input shape file: "+mesh+" on line: "+(vertexCnt+1));
				br.close();
				return;
			}
			plateCnt = Integer.parseInt(strLine);
		
			int [] tris = new int[plateCnt * 3];
			int triIdx = 0;
			
			for (int k=0; k<plateCnt; k++) {
				strLine = br.readLine();
				strLine = strLine.trim();
				String[] tokens = strLine.split(delims);
				if (tokens.length != 4) {
					log.aprintln("Incorrect number of facet indices in the input shape file: "+mesh+" on line: "+(vertexCnt+k+2));
					br.close();
					return;
				}
				tris[triIdx++] = (Integer.parseInt(tokens[1])-1);
				tris[triIdx++] = (Integer.parseInt(tokens[2])-1);
				tris[triIdx++] = (Integer.parseInt(tokens[3])-1);
				
			}
			
			this.vertices = vertices;
			indices = tris;
			
			// Close the input stream
			br.close();
		} catch (IOException e) {
			log.aprintln("Error loading TriangleMesh from Vertex/Facet file: "+mesh);
			throw new IOException("Error loading TriangleMesh from Vertex/Facet file: "+mesh+ " "+e.getMessage());
		} 
		
	}
	
	/*
	 * This method calculates the minimum and maximum magnitudes of the points in the mesh.
	 * In addition, the average magnitude of the mesh is calculated.
	 *
	 * not thread safe
	 */
	
    private void calcMinMaxAvgMagnitudes(float[] verts){
        float normSum = 0;
        int normCount = 0;
        float normSumSquared = 0f;
        float maxX = 0f, maxY = 0f, maxZ = 0f, minX = 0f, minY = 0f, minZ = 0f;
        maxLen = 0f;
        minLen = 0f;

        for(int i=0; i<verts.length; i+=3){
        	if (verts[i] > maxX) maxX = verts[i];
        	if (verts[i+1] > maxY) maxY = verts[i+1];
        	if (verts[i+2] > maxZ) maxZ = verts[i+2];
        	if (verts[i] < minX) minX = verts[i];
        	if (verts[i+1] < minY) minY = verts[i+1];
        	if (verts[i+2] < minZ) minZ = verts[i+2];
        	
        	
            float m = (float)mag(verts, i);
            if (Float.compare(m, 0f) != 0) {
                maxLen = (float)Math.max(maxLen, m);
                minLen = (float)Math.min(minLen, m);
                normSum += m;
                normCount++;
                normSumSquared += m * m;
            }
        }
        avgLen = normSum / normCount;
        float avgSquaredLen = normSumSquared / normCount;
        stdDev = (float) Math.sqrt((double)(avgSquaredLen - avgLen * avgLen)); 
        
        minCoords[X] = minX;
        minCoords[Y] = minY;
        minCoords[Z] = minZ;
        maxCoords[X] = maxX;
        maxCoords[Y] = maxY;
        maxCoords[Z] = maxZ;

//System.err.println("Is Unit Sphere? "+isUnitSphere);
        log.aprintln("Max mesh mag "+maxLen);        
    }

    /*
     * Convenience method to calculate the magnitude of a vector
     *
     * @param coords single dimension array with [X1, Y1, Z1, X2, Y2, Z2...] organization
     * @param start the index into the array of the vertex to be processed
     * @return vector magnitude
     *
     * thread-safe
     */
    static private double mag(float[] coords, int start){
        double length = Math.sqrt((coords[start+0]*coords[start+0]) +
                        (coords[start+1]*coords[start+1]) +
                        (coords[start+2]*coords[start+2]));
        return length;
    }
    
    /*
     * This method generates vertex normals for vertex based shading of the mesh.
     * Always assumes triangular polygons, right-hand rule, and CCW front face winding order
     * @param tris - triangle indices
     * @param verts - triangle vertices
     */
    private void getUnrolledVertexNormals(float[] verts) {
    	float[] vertexNorms = new float[verts.length];
    	
    	// check for proportionally sized triangle vertex arrays
    	if (verts.length%3 != 0) {
    		// TODO
    		// error and return
    		return;
    	}

		float[] normVec = new float[3];
		float[] tmp1 = new float[3];
		float[] tmp2 = new float[3];
		float[] v1 = new float[3];
		float[] v2 = new float[3];
		float[] v3 = new float[3];
		
    	for (int i=0; i<verts.length; i+=9) {
    		v1[0] = verts[i]; 
    		v1[1] = verts[i+1]; 
    		v1[2] = verts[i+2];
    		v2[0] = verts[i+3];
    		v2[1] = verts[i+4];
    		v2[2] = verts[i+5];
    		v3[0] = verts[i+6];
    		v3[1] = verts[i+7];
    		v3[2] = verts[i+8];
    		
    		normVec = VectorUtil.getNormalVec3(normVec, v1, v2, v3, tmp1, tmp2);
    		vertexNorms[i] = normVec[0];
    		vertexNorms[i+1] = normVec[1];
    		vertexNorms[i+2] = normVec[2];
    		vertexNorms[i+3] = normVec[0];
    		vertexNorms[i+4] = normVec[1];
    		vertexNorms[i+5] = normVec[2];
    		vertexNorms[i+6] = normVec[0];
    		vertexNorms[i+7] = normVec[1];
    		vertexNorms[i+8] = normVec[2];
    	}
    	
    	
    	normals = new float[vertexNorms.length];
    	// normalize the array of normal vectors
    	for (int n=0; n<vertexNorms.length; n+=3) {
    		float[] norm = Vec3dMath.normalized(vertexNorms, n);
    		normals[n] = norm[0];
    		normals[n+1] = norm[1];
    		normals[n+2] = norm[2];   
    	}
    	
    }

	public void clearDecals() {
		clearDecals = true;
		drawGrid = false;
	}
	
	public void deleteDecals(DecalSet decals) {
		deleteDecals = true;
		synchronized (lock) {
			decalsToDelete.add(decals);
		}		
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.asu.jmars.viz3d.renderer.gl.GLRenderable#execute(com.jogamp.opengl
	 * .GL2)
	 */
	@Override
	public void execute(GL2 gl) {
		if (clearDecals) {
			synchronized (lock) {
				for (DecalSet delDecs : decalsToDelete) {
					delDecs.setRenderable(false);
					// clear out all of the sets decals
					for (Decal d : delDecs.getDecals()) {
						d.getDecalTexture().disable(gl);
						d.getDecalTexture().destroy(gl);
						d.delBuffers(gl);
						d.dispose();
					}
				}
			}
			clearDecals = false;
		}

		if (dispose) {
			dispose(gl);
			if (oct != null) {
				oct.clearAllNodes(oct.getRoot());
				oct = null;
			}
			return;
		}
		
		ArrayList<DecalSet> decals = null;
		synchronized (lock) {
			decals = ThreeDManager.getInstance().getCurrentDecalSets();
		}
		
		// check to see if the decal sets have changed...layers added/deleted/hidden
		if (decals != null && decals.size() != decalSetIds.size()) {
			// if so synch IDs
			decalSetIds.clear();
			for (int i=0; i<decals.size(); i++) {
				decalSetIds.add(i, decals.get(i).getId());
			}
			rebuildDecals = true;
		} else {
			// check to see if the layers have been reordered
			for (int i=0; i<decals.size(); i++) {
				if (decalSetIds.get(i) != decals.get(i).getId()) {
					decalSetIds.set(i, decals.get(i).getId());
					rebuildDecals = true;
				}
			}			
		}

			if (renderDecals == null) {
				renderDecals = GlobalDecalFactory.getInstance().getGlobalDecals(meshName);
			} else if (rebuildDecals) {
				// clear out all of the sets decals
				for (Decal d : renderDecals) {
					d.hasBeenDisplayed = false;
				}
			}
			
			// call method to meld currently visible decals into one blended set for rendering
			meldDecals(decals, renderDecals);
			rebuildDecals = false;
			
		if (this.isRendered && renderDecals != null) {

			synchronized (lock) {
				// int decalsProcessed = 0;
				// int totalDecals = 0;
				for (Decal d : renderDecals) {
					// totalDecals++;
					if (!d.hasBeenDisplayed && d.getImage() != null) {
						d.genBuffers(gl);
						d.loadTexture(gl);
						createDecalBuffers(gl, d);
						d.hasBeenDisplayed = true;
						// decalsProcessed++;
					}
				}
			}
		}
		
		gl.glEnable(GL2.GL_CULL_FACE);
		gl.glCullFace(GL2.GL_BACK);
		gl.glFrontFace(GL2.GL_CCW);

		if (this.updateColors) {
			createShapeMeshColors(gl);
			updateColors = false;
		}

		// draw the Ellipsoid ONLY if we have one, otherwise draw the actual
		// mesh
		if (drawBody && soid != null) {
			soid.execute(gl);
		} else {
			if (drawBody && vboDone) {
				gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
				gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
				gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
				gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
				if (VBO) {
					// Vertices
					gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
					gl.glBindBuffer(GL2.GL_ARRAY_BUFFER,
							bufferObjs[SHAPE_VERTEX_DATA]);
					gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);

					// Normals.
					gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
							bufferObjs[SHAPE_NORMAL_DATA]);
					gl.glNormalPointer(GL2.GL_FLOAT, 0, 0);

					// Colors
					gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
							bufferObjs[SHAPE_COLOR_DATA]);
					gl.glColorPointer(4, GL2.GL_FLOAT, 0, 0);

					// Indices
					gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER,
							bufferObjs[SHAPE_INDEX_DATA]);

					gl.glDrawElements(GL2.GL_TRIANGLES, indices.length,
							GL2.GL_UNSIGNED_INT, 0);
					gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);

				} else {
					gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuffer);
					gl.glColorPointer(3, GL2.GL_FLOAT, 0, cBuffer);
					gl.glDrawElements(GL2.GL_TRIANGLES, indices.length,
							GL2.GL_UNSIGNED_INT, idxBuffer);
				}
				gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
				gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
				gl.glDisableClientState(GL2.GL_COLOR_ARRAY);
				gl.glDisableClientState(GL2.GL_INDEX_ARRAY);
			}

			if (drawGrid) {
				for (int i = 0; i < indices.length; i += 3) {
					this.drawTriangle(gl, vertices[indices[i] * 3],
							vertices[indices[i] * 3 + 1],
							vertices[indices[i] * 3 + 2],
							vertices[indices[i + 1] * 3 + 0],
							vertices[indices[i + 1] * 3 + 1],
							vertices[indices[i + 1] * 3 + 2],
							vertices[indices[i + 2] * 3 + 0],
							vertices[indices[i + 2] * 3 + 1],
							vertices[indices[i + 2] * 3 + 2], 1, 1, 0, 0);
				}
			}

			if (drawPoints) {
				gl.glPointSize(1);
				gl.glEnable(GL2.GL_POINT_SMOOTH);
				gl.glBegin(GL2.GL_POINTS);
				for (int i = 0; i < vertices.length; i += 3) {
					gl.glColor4f(colors[i], colors[i + 1], colors[i + 2],
							getDisplayAlpha());
					gl.glVertex4f(vertices[i], vertices[i + 1],
							vertices[i + 2], getDisplayAlpha());
				}
				gl.glEnd();
			}
			
			int color = 0;
			
			if (this.isRendered && renderDecals != null && renderDecals.size()  > 0) {
		        gl.glEnable(GL2.GL_CULL_FACE);
				gl.glCullFace(GL2.GL_BACK);
		    	gl.glFrontFace(GL2.GL_CCW);
				gl.glEnable(GL.GL_BLEND);
				gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
				for (Decal d : renderDecals) {
					if (!d.hasTexture() || !d.hasBeenDisplayed) {
						continue;
					}
	                gl.glMaterialfv(GL.GL_FRONT, GL2.GL_AMBIENT, lightAmbient, 0);
	                gl.glMaterialfv(GL.GL_FRONT, GL2.GL_DIFFUSE, lightDiffuse, 0);
	                gl.glMaterialfv(GL.GL_FRONT, GL2.GL_SPECULAR, lightSpecular, 0);

					d.getDecalTexture().enable(gl);
					d.getDecalTexture().bind(gl);
					
					gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
					gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
					gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
					gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
					if (VBO) {
						gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
						gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_VERTEX_DATA]);
						gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);

						gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_NORMAL_DATA]);
						gl.glNormalPointer(GL2.GL_FLOAT, 0, 0);

						gl.glBindBuffer(GL.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_TEXTURE_DATA]);
						gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, 0);

						// Indices
						gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_INDEX_DATA]);
						gl.glDrawElements(GL2.GL_TRIANGLES, d.getIndices().length, GL2.GL_UNSIGNED_INT, 0);
					} else {
						gl.glVertexPointer(3, GL2.GL_FLOAT, 0, directVBuf);
						gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, directTBuf);
						gl.glDrawElements(GL2.GL_TRIANGLES, d.getIndices().length, GL2.GL_UNSIGNED_INT, polyBuffer);
					}
					d.getDecalTexture().disable(gl);

					if (color < 3) {
						color++;
					} else {
						color = 0;
					}
					if (displayTileGrid) {
						int[] dIndices = d.getIndices();
						float[] dVerts = d.getVertices();
						for (int i = 0; i < dIndices.length; i += 3) {
							this.drawTriangle(gl, dVerts[dIndices[i] * 3], dVerts[dIndices[i] * 3 + 1],
									dVerts[dIndices[i] * 3 + 2], dVerts[dIndices[i + 1] * 3 + 0],
									dVerts[dIndices[i + 1] * 3 + 1], dVerts[dIndices[i + 1] * 3 + 2],
									dVerts[dIndices[i + 2] * 3 + 0], dVerts[dIndices[i + 2] * 3 + 1],
									dVerts[dIndices[i + 2] * 3 + 2], 1, decalColors[color][0], decalColors[color][1],
									decalColors[color][2]);
						}
					}

				}
				gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
				gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);
				gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
				gl.glDisableClientState(GL2.GL_INDEX_ARRAY);	
				
				if (deleteDecals) {
					synchronized (lock) {
						for (DecalSet delDecs : decalsToDelete) {
							// need to remove from the current list of bound DecalSets
							delDecs.setRenderable(false);
							// clear out all of the sets decals
							for (Decal d : delDecs.getDecals()) {
								if (d.getDecalTexture() != null) {
									d.getDecalTexture().disable(gl);
									d.getDecalTexture().destroy(gl);
									d.delBuffers(gl);
								}
								d.dispose();
							}
						}
					}
					deleteDecals = false;
				}


				
			}

		}
		gl.glColor3f(1, 1, 1);

		int errCode = GL2.GL_NO_ERROR;
		if ((errCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
			String errString = glu.gluErrorString(errCode);
			log.aprintln("OpenGL Error: " + errString);
		}
		isRendered = true;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLAction#preRender()
	 */
	@Override
	public void preRender(GL2 gl) {
    	glu = new GLU();
		
		if (soid != null) {
			soid.preRender(gl);
		}
		else if (!vboDone) {
			gl.glGenBuffers(5, bufferObjs, 0);
			try {
				createShapeMeshBuffers(gl);				
			}
			catch(Exception ex){
			        log.aprint(ex);
			        Util.showMessageDialog(
			                "Error processing shape model or image data: " + ex.getLocalizedMessage() + ".",
			                "Shape Model/Image Error",
			                JOptionPane.ERROR_MESSAGE);
			        return;
			}
			vboDone = true;
		}
		
		int errCode = GL2.GL_NO_ERROR;
	    if ((errCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
	       String errString = glu.gluErrorString(errCode);
	       log.aprintln("OpenGL Error: "+errString);
	    }
	    
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLAction#postRender(com.jogamp.opengl.GLAutoDrawable)
	 */
	@Override
	public void postRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}
	
	private void createShapeMeshBuffers(GL2 gl) {
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
		gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
		
		if (VBO) {
			vBuffer = FloatBuffer.wrap(vertices);
			nBuffer = FloatBuffer.wrap(normals);
			cBuffer = FloatBuffer.wrap(colors);
			idxBuffer = IntBuffer.wrap(indices);
			gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, bufferObjs[SHAPE_VERTEX_DATA] );
			// Copy data to the server into the VBO.
			gl.glBufferData( GL2.GL_ARRAY_BUFFER, vertices.length*(Float.SIZE/Byte.SIZE), vBuffer, GL2.GL_STATIC_DRAW );
			// Normals.
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, bufferObjs[SHAPE_NORMAL_DATA] );
			gl.glBufferData( GL.GL_ARRAY_BUFFER, normals.length*(Float.SIZE/Byte.SIZE), nBuffer, GL.GL_STATIC_DRAW );
			
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, bufferObjs[SHAPE_COLOR_DATA] );
			gl.glBufferData( GL.GL_ARRAY_BUFFER, colors.length*(Float.SIZE/Byte.SIZE), cBuffer, GL.GL_STATIC_DRAW );
			gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, bufferObjs[SHAPE_INDEX_DATA] );
			gl.glBufferData( GL.GL_ELEMENT_ARRAY_BUFFER, indices.length*(Integer.SIZE/Byte.SIZE), idxBuffer, GL.GL_STATIC_DRAW );
		} else { // use vertex arrays in the client
			vBuffer = Buffers.newDirectFloatBuffer(vertices);
			nBuffer = Buffers.newDirectFloatBuffer(normals);
			cBuffer = Buffers.newDirectFloatBuffer(colors);
			idxBuffer = Buffers.newDirectIntBuffer(indices);
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuffer);
			gl.glColorPointer(3, GL2.GL_FLOAT, 0, cBuffer);
		}		
		
	}	

	private void createShapeMeshColors(GL2 gl) {
		gl.glEnableClientState(GL2.GL_COLOR_ARRAY);
		
		if (VBO) {
			cBuffer = FloatBuffer.wrap(colors);
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, bufferObjs[SHAPE_COLOR_DATA] );
			gl.glBufferData( GL.GL_ARRAY_BUFFER, colors.length*(Float.SIZE/Byte.SIZE), cBuffer, GL.GL_STATIC_DRAW );
		} else { // use vertex arrays in the client
			cBuffer = Buffers.newDirectFloatBuffer(colors);
		}		
		
	}	

	/**
	 * Method to create default shading for the mesh by doing a gray-scale stretch by elevation
	 *
	 * not thread safe
	 */
    private void computeVertexColors(){
        colors = new float[vertices.length * 4 / 3];
        for(int i=0; i<colors.length; i+=4){
                double norm = mag(vertices, i * 3 / 4);
                double color = norm;               		    		
                if(isUnitSphere){
                	color = 0.4;
                }else{
                	color = Math.min(1.0, (1.0*((norm - minLen) / (maxLen - minLen))));
                }
            colors[i] = colors[i+1] = colors[i+2] = (float)color;	                    
            colors[i+3] = getDisplayAlpha();
        }        
    }
    
    private void meldDecals(ArrayList<DecalSet> layerSets, ArrayList<Decal> decals) {
    	boolean[] flags = null;
    	if (rebuildDecals && layerSets.size() > 0) {
    		flags = new boolean[decals.size()];
    		for (int i=0; i<flags.length; i++) {
    			flags[i] = true;
    		}
    	// if we don't have any to display, clear all the decals	
    	} else if (layerSets.size() == 0) {
    		for (int i=0; i<decals.size(); i++) {
   	    		decals.get(i).clearImage();
    		}
    	// check and see which decals have been updated and flag them	
    	} else {
    		flags = flagDecalsForProcessing (layerSets);
    		
    	}
    	if (flags == null) {
    		return;
    	}
  
    	// make one pass to clear out flagged decals
    	for (DecalSet lSet : layerSets) {
    		ArrayList<Decal> from = lSet.getDecals();
    		if (from.size() != decals.size() || decals.size() != flags.length) {
    			log.aprintln("Mismatched number of decals. "+from.size()+" expected, "+decals.size()+" actual.");
    			return;
    		}
    		for (int i=0; i<from.size(); i++) {
    			if (flags[i]) {
    	    		decals.get(i).clearImage();
    			}
    		}
    	}

    	// blend flagged decals
    	for (DecalSet lSet : layerSets) {
    		ArrayList<Decal> from = lSet.getDecals();
    		for (int i=0; i<from.size(); i++) {
    			if (flags[i]) {
    				decals.get(i).blend(from.get(i));
    				from.get(i).hasBeenDisplayed = true;
    			}
    		}
    		if (lSet.isRenderable()) {
    			lSet.setDisplayable(true);
    		}

    	}   	
    }
    
    private boolean[] flagDecalsForProcessing (ArrayList<DecalSet> sets) {
    	boolean[] flags = null;
    	
    	for (DecalSet set : sets) {
    		ArrayList<Decal> ds = set.getDecals();
    		if (ds.size() <  1) {
    			log.aprintln("Error flagging decals for processing");
    			return flags;
    		} 
    		if (flags == null){
    			flags = new boolean[ds.size()];
    		}
    		
    		for (int i=0; i<ds.size(); i++) {
    			if (!flags[i] && !ds.get(i).hasBeenDisplayed) {
    				flags[i] = true;
    			}
    		}
    	}   	
    	return flags;
    }
    
	/**
	 * Returns the length of the longest vertex in the mesh
	 *
	 * @return float
	 *
	 * thread-safe
	 */
	public float getMaxMeshLen() {
		return maxLen;
	}
	
	/**
	 * Returns the length of the longest vertex in the mesh
	 *
	 * @return float
	 *
	 * thread-safe
	 */
	public float getMaxLen() {
		return maxLen;
	}
	
	/**
	 * Method to return the radius at a specified location
	 * @param longitude WEST-LEADING
	 * @param latitude
	 * @return mesh radius at input point in KILOMETERS
	 */
	public float getRadiusAtLocation(float longitude, float latitude) {
		// convert lat lon to a 3D space location on the shape model
		HVector hv = new HVector(longitude, latitude);
		ArrayList<Triangle> tri = new ArrayList<>();
		float[] rayVec = new float[] {(float)hv.x, (float)hv.y, (float)hv.z};
		rayVec = VectorUtil.scaleVec3(new float[3], rayVec, maxLen * 2f);
		oct.looseOctTreeRayIntersectShortCircuit(new Ray(rayVec, new float[] {-rayVec[X], -rayVec[Y], -rayVec[Z]}), oct.getRoot(), tri);
		if (tri.size() < 1) {
			log.aprintln("Unable to determine radius at Lon: "+longitude+" Lat: "+latitude);
			return Float.MAX_VALUE;
		}
		float[] surfaceLoc = tri.get(0).getIntersection();
		
		float radius = VectorUtil.normVec3(surfaceLoc);		
		
		return radius;		
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.Disposable#dispose()
	 */
	@Override
	public void dispose() {
		dispose = true;
		
	}
	
	/**
	 * This method actually doesn't work correctly for java.nio.Buffers.
	 * A better solution is to use sun.misc.Cleaner...when I figure out how!
	 *
	 * @param gl
	 *
	 * not thread safe
	 */
	public void dispose(GL2 gl) {
		gl.glDeleteBuffers(5, bufferObjs, 0);

		idxBuffer = null;
		vBuffer = null;
		nBuffer = null;
		cBuffer = null;
	}
	
	/*
	 * Method to draw a triangle. Currently used for drawing the triangle mesh.
	 *
	 * @param gl current Open GL context
	 * @param ax x coordinate of first point
	 * @param ay y coordinate of first point
	 * @param az z coordinate of first point
	 * @param bx x coordinate of second point
	 * @param by y coordinate of second point
	 * @param bz z coordinate of second point
	 * @param cx x coordinate of third point
	 * @param cy y coordinate of third point
	 * @param cz z coordinate of third point
	 * @param width line width
	 * @param r red component of line color value between 0 and 1
	 * @param g green component of line color value between 0 and 1
	 * @param b blue component of line color value between 0 and 1
	 *
	 * not thread safe
	 */
	private void drawTriangle(GL2 gl, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, int width, int r, int g, int b)
	{
	    gl.glColor4f( r, g, b, getDisplayAlpha());
	    
	    gl.glLineWidth(width);
	    gl.glBegin(GL.GL_LINES);
	    gl.glVertex3f( ax, ay, az);
	    gl.glVertex3f( bx, by, bz);
	    gl.glVertex3f( cx, cy, cz);
	    gl.glVertex3f( ax, ay, az);
	    gl.glEnd();
	}

	private void drawTriangle(GL2 gl, float[][] points, int width, float[] color)
	{
	    gl.glColor4f( color[0], color[1], color[2], getDisplayAlpha());
	    
	    float[] a = VectorUtil.scaleVec3(new float[3], points[0], 1.005f);
	    float[] b = VectorUtil.scaleVec3(new float[3], points[1], 1.005f);
	    float[] c = VectorUtil.scaleVec3(new float[3], points[2], 1.005f);
	    
	    gl.glBegin(GL.GL_TRIANGLES);
	    gl.glVertex3f( a[0], a[1], a[2]);
	    gl.glVertex3f( b[0], b[1], b[2]);
	    gl.glVertex3f( c[0], c[1], c[2]);
	    gl.glEnd();
	}
	
	private void drawTriangles(GL2 gl, float[] points, float[][] color)
	{
		int colorIdx = 0;	    
	    
		for (int i=0; i<points.length-9; i+=9) {
		    gl.glBegin(GL.GL_TRIANGLES);
			    gl.glColor4f(color[colorIdx][0]/255f, color[colorIdx][1]/255f, color[colorIdx][2]/255f, getDisplayAlpha());
			    gl.glVertex3f( points[i], points[i+1], points[i+2]);
			    gl.glVertex3f( points[i+3], points[i+4], points[i+5]);
			    gl.glVertex3f( points[i+6], points[i+7], points[i+8]);
		    gl.glEnd();
		    colorIdx++;
		}
	}


	public double[] vertsToEquatorialWorldHVector(float[] verts) {
		double[] lonlats = new double[verts.length * 2 / 3];
		double latitude = 0.0;
		double longitude = 0.0;
		int idx = 0;
		HVector temp = new HVector();
		ProjObj proj = GlobalDecalFactory.getEquProjection();

		for (int i = 0; i < verts.length; i += 3) {
			temp = temp.set(verts[i], verts[i + 1], verts[i + 2]);
			latitude = temp.lat();
			longitude = temp.lonE();

			Point2D spw = proj.convSpatialToWorld(360.0 - (longitude), latitude);
			lonlats[idx++] = spw.getX();
			lonlats[idx++] = spw.getY();
		}

		return lonlats;
	}

	public double[] vertsToPolarWorldHVector(float[] verts) {
		double[] lonlats = new double[verts.length * 2 / 3];
		double latitude = 0.0;
		double longitude = 0.0;
		int idx = 0;
		HVector temp = new HVector();
		ProjObj proj = GlobalDecalFactory.getPolarProjection();

		for (int i = 0; i < verts.length; i += 3) {
			temp = temp.set(verts[i], verts[i + 1], verts[i + 2]);
			latitude = temp.lat();
			longitude = temp.lonE();

			Point2D spw = proj.convSpatialToWorld(360.0 - (longitude), latitude);
			lonlats[idx++] = spw.getX();
			lonlats[idx++] = spw.getY();
		}

		return lonlats;
	}
       
    private void vertsToWorldHVector(float[] verts, double[] worlds) {
    	if (verts == null || verts.length < 3) {
    		return;
    	}
    	if (worlds == null) {
    		worlds = new double[verts.length * 2 / 3];
    	}
    	Arrays.fill(worlds, 0f);
        double latitude = 0.0;
        double longitude = 0.0;
    	int idx = 0;
        HVector temp = new HVector();
        ProjObj proj = Main.PO;
        
        for (int i=0; i<verts.length; i+=3) {
        	temp = temp.set(verts[i], verts[i+1], verts[i+2]);
        	latitude = temp.lat();
        	longitude = temp.lonE();
        	
        	Point2D spw = proj.convSpatialToWorld(360.0-(longitude), latitude);
			worlds[idx++] = spw.getX();
			worlds[idx++] = spw.getY();      	
        }
     }
       
	/**
	 * Enables/disables rendering of a grid outline of the facets in this mesh.
	 *
	 * @param draw renders the grid if true
	 *
	 * thread-safe
	 */
	public void drawMesh(boolean draw) {
		drawGrid = draw;
	}
	
	/**
	 * Enables/disables rendering of the mesh.
	 *
	 * @param draw renders the mesh if true
	 *
	 * thread-safe
	 */
	public void setDrawBody(boolean draw) {
		drawBody = draw;
	}
	
	/**
	 * @return If the mesh should be rendered or not
	 */
	public boolean drawBody(){
		return drawBody;
	}
	
	/**
	 * Enables/disables rendering of the vertices in this mesh as a point cloud.
	 *
	 * @param draw renders the point cloud if true
	 *
	 * thread-safe
	 */
	public void drawPoints(boolean draw) {
		drawPoints = draw;
	}

	/**
	 * Reads a source GZip file and writes the uncompressed data to destination 
	 * file.
	 * @param sourcePath path to GZip file to load from.
	 * @param destinationPath path to file to write the uncompressed data to.
	 * @throws IOException
	 * @throws DataFormatException 
	 */
	private static File decompressData(String sourcePath, String destinationPath) 
	        throws IOException, DataFormatException {
	    //Allocate resources.
	    FileInputStream fis = new FileInputStream(sourcePath);
	    FileOutputStream fos = new FileOutputStream(destinationPath);
	    GZIPInputStream gzis = new GZIPInputStream(fis);
	    byte[] buffer = new byte[1024];
	    int len = 0;
	    
	    //Extract compressed content.
	    while ((len = gzis.read(buffer)) > 0) {
	        fos.write(buffer, 0, len);
	    }
	    //Release resources.
	    fos.close();
	    fis.close();
	    gzis.close();
	    buffer = null;
	    File file = new File(destinationPath);
	    return file;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLAction#delete(com.jogamp.opengl.GL2)
	 */
	@Override
	public void delete(GL2 gl) {
		dispose(gl);		
	}

	/**
	 * Returns the vertices that constitute this mesh in [X1, Y1, Z1, X2, Y2, Z2...] organization
	 *
	 * @return float[]
	 *
	 * thread-safe
	 */
	public float[] getVertices() {
		return vertices;
	}
	
	/**
	 * Returns the indices that constitute this mesh in [F1V1, F1V2, F1V3, F2V1, F2V2, F2V3...] organization
	 *
	 * @return float[]
	 *
	 * thread-safe
	 */
	public int[] getIndices() {
		return indices;
	}
	
	/**
	 * Return a reference to the matching OctTree for this mesh.
	 * May be null.
	 *
	 * @return OctTree
	 *
	 * thread-safe
	 */
	public OctTree getOctTree() {
		return oct;
	}
	
	private void realizeOcttree(boolean isObj) {
		long startTime = System.currentTimeMillis();
		float max = VectorUtil.normVec3(maxCoords);
		float min = VectorUtil.normVec3(minCoords);
		oct = new OctTree(root, max*2, min, /*max*/ maxLen, indices.length / 3, isObj);
		int j = 0;
		for (int i=0; i<indices.length; i+=3) {
			Triangle t = new Triangle(new float[][]{{vertices[indices[i]*3], vertices[indices[i]*3+1], vertices[indices[i]*3+2]},
					{vertices[indices[i+1]*3], vertices[indices[i+1]*3+1], vertices[indices[i+1]*3+2]},
					{vertices[indices[i+2]*3], vertices[indices[i+2]*3+1], vertices[indices[i+2]*3+2]}});
			if (isObj) {
				t.id = j;
			} else {
				t.id = j+1;	
			}
			oct.looseOctTreeInsert(root, t);
			j++;
			if (t.getRadius() > this.maxFacetRadius) {
				maxFacetRadius = t.getRadius();
			}
		}
		log.aprintln("Octtree load time: "+(System.currentTimeMillis() - startTime)/1000.0);			
		
		log.aprintln("Number of plates actually loaded: "+oct.numPlates);
		log.aprintln("Number of nodes: "+oct.countNodes(root));
		oct.printDepthTotals();
	}
	
	/**
	 * Returns a List of mesh triangles intersected by the input Ray.
	 * 
	 * @param ray the input Ray
	 * @param tris the returned Triangles if any
	 * @return true if there was an intersection
	 *
	 * not thread-safe
	 */
	public boolean rayIntersect (Ray ray, ArrayList<Triangle> tris) {
		if (oct != null) {
			oct.looseOctTreeRayIntersectShortCircuit(ray, oct.getRoot(), tris);
		}
		if (tris.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Fits the input Polygon to the surface of the mesh.
	 * The resulting fitment will be applied to the Polygon directly.
	 *
	 * @param poly
	 *
	 * not thread-safe
	 */
	public void fitToMesh(Polygon poly) {
		if (!poly.isFitted() && poly.type.equals(PolygonType.OnBody) && oct != null && poly.polyPoints != null && poly.polyPoints.length > 8) {
			FittedPolygonData data = oct.looseFitPolygon(poly.polyPoints);
			if (data != null) {
				poly.setFittedData(data);
			} else {
				poly.setBeingFitted(false);
			}
		}
		return;
	}

	/**
	 * Fits the input OutLine to the surface of the mesh.
	 * The resulting fitment will be applied to the OutLine directly.
	 *
	 * @param outline
	 *
	 * not thread-safe
	 */
	public void fitToMesh(OutLine outline) {
		if (!outline.isFitted() && outline.getOutLineType().equals(OutLineType.OnBody) 
				&& oct != null && outline.getPointsSequential() != null 
				&& outline.getPointsSequential().length > 5 /*need at least 6 values (3 points) to have a valid polygon */) {
			float[] pts = oct.looseFitOutLine(outline.getPointsSequential());
			if (pts != null) {
				outline.setFittedPoints(pts);
			}			
		}
		return;
	}
	
	/**
	 * Returns the name of this mesh if provided during construction.
	 *
	 * @return mesh name if not null, otherwise returns null
	 *
	 * thread-safe
	 */
	public String getMeshName() {
		return meshName;
	}
	
	/**
	 * Method to indicate if this mesh uses an Ellipsoid for rendering
	 *
	 * @return true if Ellipsoid is used for rendering
	 */
	public boolean isEllipsoid() {
		if (soid != null) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Method to return the scalar value of the largest axis in the Ellipsoid
	 * <Description>
	 *
	 * @return length of the longest Ellipsoid axis.
	 */
	public float getMaxEllipsoidAxisLen() {
		if (soid != null) {
			return soid.getMaxLen();
		} else {
			return maxLen;
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getAlpha()
	 */
	@Override
	public float getAlpha() {
		return alpha;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getDisplayAlpha()
	 */
	@Override
	public float getDisplayAlpha() {
		if(displayAlpha == null){
			return alpha;
		}
		return displayAlpha;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#setDisplayAlpha(float)
	 */
	@Override
	public void setDisplayAlpha(float alpha) {
		displayAlpha = alpha;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#isScalable()
	 */
	@Override
	public boolean isScalable() {
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#scaleByDivision(float)
	 */
	@Override
	public void scaleByDivision(float scalar) {
		return;		
	}
	
	public boolean isUnitSphere() {
		return isUnitSphere;
	}

	/**
	 * @param isUnitSphere the isUnitSphere to set
	 */
	public void setUnitSphere(boolean isUnitSphere) {
		this.isUnitSphere = isUnitSphere;
	}

	/**
	 * @return the worldCoords
	 */
	public double[] getEquatorialWorldCoords() {
		return equWorldCoords;
	}

	/**
	 * @return the worldCoords
	 */
	public double[] getPolarWorldCoords() {
		return polarWorldCoords;
	}

	/**
	 * Method to create the buffers necessary to render a Decal object
	 * @param gl JOGL OpenGL interface
	 * @param d the Decal to create buffers for
	 */
	public void createDecalBuffers(GL2 gl, Decal d) {
		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);		
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		
		if (VBO) {
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_VERTEX_DATA] );
			gl.glBufferData( GL2.GL_ARRAY_BUFFER, d.getVertices().length*(Float.SIZE/Byte.SIZE), d.getVertexBuffer(), GL2.GL_STATIC_DRAW );

			gl.glBindBuffer( GL2.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_NORMAL_DATA] );
			gl.glBufferData( GL2.GL_ARRAY_BUFFER, d.getNormals().length*(Float.SIZE/Byte.SIZE), d.getNormalBuffer(), GL2.GL_STATIC_DRAW );
					
			gl.glBindBuffer( GL.GL_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_TEXTURE_DATA] );
			gl.glBufferData( GL.GL_ARRAY_BUFFER, d.getTexCoords().length*(Float.SIZE/Byte.SIZE), d.getTextureBuffer(), GL.GL_STATIC_DRAW );

			gl.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, d.getBufferObj()[Decal.DECAL_INDEX_DATA] );
			gl.glBufferData( GL.GL_ELEMENT_ARRAY_BUFFER, d.getIndices().length*(Integer.SIZE/Byte.SIZE), d.getDecalTris(), GL.GL_STATIC_DRAW );
		} else {
			if (directTBuf == null) {
				directTBuf = Buffers.newDirectFloatBuffer(d.getTexCoords());
			}
			if (directVBuf == null) {
				directVBuf = Buffers.newDirectFloatBuffer(d.getVertices());
			}
			polyBuffer = Buffers.newDirectIntBuffer(d.getIndices());
			
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, directVBuf);
			gl.glTexCoordPointer(3, GL2.GL_FLOAT, 0, directTBuf);
		}
		gl.glDisableClientState(GL2.GL_TEXTURE_COORD_ARRAY);	
		
    		int errCode = GL2.GL_NO_ERROR;
	    if ((errCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
	       String errString = glu.gluErrorString(errCode);
	       log.println("OpenGL Error: "+errString);
	       Util.showMessageDialog(
	                "Error accessing graphics card: " + errString + ".",
	                "Graphics Rendering Error",
	                JOptionPane.ERROR_MESSAGE);
	    }    
	}

	/**
	 * Method to inform at to whether the mesh has been rendered
	 * @return true if has been rendered
	 */
	public boolean isRendered() {
		return isRendered;
	}

	/**
	 * @param displayTileGrid the displayTileGrid to set
	 */
	public void setDisplayTileGrid(boolean displayTileGrid) {
		this.displayTileGrid = displayTileGrid;
	}

	@Override
	public void scaleToShapeModel(boolean canScale) {
		// NOP		
	}

	@Override
	public boolean isScaled() {
		// TODO Auto-generated method stub
		return hasBeenScaled;
	}
	
	private void scaleToUnity() {
		if (vertices != null) {
			float scalr = (float)(1f/Util.EQUAT_RADIUS);
			for (int i=0; i<vertices.length; i++) {
				vertices[i] *= scalr;
			}
		}
	}

	@Override
	/**
	 * This method return a single dimension array of facet colors
	 * Example: {R1, G1, B1, R2, G2, B2,...}
	 * These colors map to the triangles in the TriangleMesh 
	 */
	public float[] getColor() {
		return colors;
	}
}
