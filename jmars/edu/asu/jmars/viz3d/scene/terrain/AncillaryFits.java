package edu.asu.jmars.viz3d.scene.terrain;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import nom.tam.fits.FitsException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.math.VectorUtil;

import edu.asu.jmars.swing.FancyColorMapper;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.viz3d.core.geometry.TriangleMesh;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.input.files.AncFits;

/**
 * This class provides a GLRenderable wrapper for an AncFits object.
 * It will allow Ancillary FITS data to be rendered in 3D if there is a matching
 * TriangleMesh (DSK or OBJ) already loaded and rendered.
 * *
 * not thread-safe
 */
public class AncillaryFits implements GLRenderable {
	
	private Map<String, float[]> facetValues;	// FITS file facet values
	private int[] facetIds;			// FITS file facet IDs
	private Map<String, float[]> errorRange;		// FITS file error range values
	private float[] colors;			// derived FITS facet colors
    private int[] mappedColors = null;
    private List<String> dataLabel = null; // data column labels
    private List<String> sigmaLabel = null; // sigma column labels
    private HashMap<Integer, float[]> indexedData = new HashMap<>();
    private HashMap<Integer, float[]> indexedErrors = new HashMap<>();

	boolean updateColors = false;
	boolean dispose = false;
	String name = null;
	float[] vertices = null;
	int[] indices = null;
	private float alpha = 1f;
	private Float displayAlpha;
	private FancyColorMapper colorMapper = null;	
	GLU glu;
	
    private static DebugLog log = DebugLog.instance();

    /**
     * Constructor
     * @param fits AncFits
     */
	public AncillaryFits(AncFits fits) throws FitsException {
		glu = new GLU();
		
		name = fits.getFile().getName();
		
		dataLabel = fits.getDataColumnNames();
		
		facetIds = fits.getFacetValues();
		
		facetValues = fits.getKeyedDataValues();
		
		errorRange = fits.getKeyedSigmaValues();
		
		sigmaLabel = fits.getSigmaColumnNames();
		
		for (int i=0; i<facetIds.length; i++) {
			float[] tmp = new float[dataLabel.size()];
			for (int j=0; j<dataLabel.size(); j++) {
				tmp[j] = facetValues.get(dataLabel.get(j))[i];
			}
			indexedData.put(facetIds[i], tmp);
		}
		
		
		for (int i=0; i<facetIds.length; i++) {
			float[] tmp = new float[sigmaLabel.size()];
			for (int j=0; j<sigmaLabel.size(); j++) {
				tmp[j] = errorRange.get(sigmaLabel.get(j))[i];
			}
			indexedErrors.put(facetIds[i], tmp);
		}
		
		// need to unroll the vertex colors to allow VBO coloring of individual facets
		computeUnrolledVertexColors();
	}

	
	
	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#execute(com.jogamp.opengl.GL2)
	 */
	@Override
	public void execute(GL2 gl) {
		if (dispose) {
			delete(gl);
			return;
		}
		
    	if (updateColors) {
    		updateColors = false;
    	}
       
    	ThreeDManager mgr = ThreeDManager.getInstance();
    	// make sure we have a matching shape model
		TriangleMesh mesh = mgr.getShapeModel();
		float[] vertices = mesh.getVertices();
		if (colors.length == vertices.length) {
			for (int i=0; i<vertices.length; i+=9) {
			    gl.glColor4f(colors[i+0], colors[i+1], colors[i+2], getDisplayAlpha());
				gl.glEnable(GL2.GL_BLEND);
				gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
			    gl.glPolygonMode(GL2.GL_FRONT, GL2.GL_FILL);
			    
			    // TODO this should probably go away
			    float[] a = VectorUtil.scaleVec3(new float[3], new float[]{vertices[i], vertices[i+1], vertices[i+2]}, 1.00f);
			    float[] b = VectorUtil.scaleVec3(new float[3], new float[]{vertices[i+3], vertices[i+4], vertices[i+5]}, 1.00f);
			    float[] c = VectorUtil.scaleVec3(new float[3], new float[]{vertices[i+6], vertices[i+7], vertices[i+8]}, 1.00f);
			    
			    gl.glBegin(GL.GL_TRIANGLES);
			    gl.glVertex3f( a[0], a[1], a[2]);
			    gl.glVertex3f( b[0], b[1], b[2]);
			    gl.glVertex3f( c[0], c[1], c[2]);
			    gl.glEnd();
			}
		} else {
			log.aprint("Unable to render Ancillary Fits data: mismatched shape model");
		}	
    	
		int errCode = GL2.GL_NO_ERROR;
	    if ((errCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
	       String errString = glu.gluErrorString(errCode);
	       log.aprintln("OpenGL Error: "+errString);
	    }
	    
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#preRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void preRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#postRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void postRender(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#delete(com.jogamp.opengl.GL2)
	 */
	@Override
	public void delete(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	
	/** 
	 * This method updates the mapped range of colors for the Ancillary FITS file.
	 * This method should be called immediately prior to calling updateColors().
	 *
	 * @param min float
	 * @param max float
	 *
	 * not thread-safe
	 */
	public void updateMappedColors(double min, double max) {
		// TODO only mapping colors using the first data column until customer defines 
		// desired behavior for multiple data columns
		String[] facetKeys = facetValues.keySet().toArray(new String[facetValues.size()]);
		float[] vals = facetValues.get(facetKeys[0]);
        for (int i=0; i<vals.length; i++) {
        	mappedColors[i] = (int)mapRange(min, max, 0.0f, 255.0f, Math.max(min, Math.min(max, vals[i])));
        }
	}
	
	/**
	 * This method updates the stretched colors of the Ancillary FITS facets
	 *
	 * @param colorValues Color[] colors to be used for each vertex
	 *
	 * not thread-safe
	 */
    public void updateColors(Color[] colorValues) {
    	int k = 0;
        for (int i=0; i<mappedColors.length; i++) {
        	Color c = colorValues[mappedColors[i]];
        	// create a color map
        	colors[k++] = c.getRed()/255f;
        	colors[k++] = c.getGreen()/255f;
        	colors[k++] = c.getBlue()/255f;        	
        	colors[k++] = c.getRed()/255f;
        	colors[k++] = c.getGreen()/255f;
        	colors[k++] = c.getBlue()/255f;        	
        	colors[k++] = c.getRed()/255f;
        	colors[k++] = c.getGreen()/255f;
        	colors[k++] = c.getBlue()/255f;        	
        } 
        updateColors = true;
    }
 
    /**
     * Returns an array of the facet vertices in a single dimension array.
     * Data will be stored in X, Y, Z format.
     *
     * @return float[]
     *
     * thread-safe
     */
    public float[] getVertices() {
		return vertices;
	}

    /**
     * Sets the facet vertex data.
     * A single dimension float array is expected with X1, Y1, Z1, X2, Y2, Z2...organization.
     *
     * @param vertices
     *
     * not thread-safe
     */
	public void setVertices(float[] vertices) {
		this.vertices = vertices;
	}

    /**
     * Returns an array of the facet indexes into the vertex array.
     * Data will be stored in V1, V2, V3 format.
     *
     * @return float[]
     *
     * thread-safe
     */
	public int[] getIndices() {
		return indices;
	}

	/**
	 * Sets the facet index data. 
	 * A single dimension int array is expected in F1V1, F1V2, F1V3, F2V1, F2V2, F2V3...organization 
	 *
	 * @param indices
	 *
	 * not thread-safe
	 */
	public void setIndices(int[] indices) {
		this.indices = indices;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getAlpha()
	 */
	public float getAlpha() {
		return alpha;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#getDisplayAlpha()
	 */
	public void setDisplayAlpha(float alpha) {
		displayAlpha = alpha;
	}
	
	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#setDisplayAlpha(float)
	 */
	public float getDisplayAlpha(){
		if(displayAlpha == null){
			return alpha;
		}			
		return displayAlpha;
	}
    
	/**
	 * Method to compute a facet vertex based default set of gray scale stretched VBO-compatible colors for the Ancillary FITS data. 
	 * The colors are linearly mapped to the range of facet values.
	 * A FancyColorMapper is also created to allow any calling classes to create and apply new color stretches.
	 *
	 * @param verts
	 *
	 * not thread-safe
	 */
    public void computeUnrolledVertexColors(float[] verts) {
		// TODO only calculating colors using the first data column until customer defines 
		// desired behavior for multiple data columns
		String[] facetKeys = facetValues.keySet().toArray(new String[facetValues.size()]);
		float[] vals = facetValues.get(facetKeys[0]);

    	
    	if (vals == null || vals.length < verts.length / 9) {
    		log.aprint("No facet values from a matching FITS file...");
    		return;
    	}
        colors = new float[(verts.length / 3) * 3];
        
        float maxFacet = 0f;
        float minFacet = Float.MAX_VALUE;
        for (int j=0; j<vals.length; j++) {
        	if (vals[j] > maxFacet) {
        		maxFacet = vals[j];
        	}
        	if (vals[j] < minFacet) {
        		minFacet = vals[j];
        	}        	
        }
        
        mappedColors = new int[vals.length];
        int k=0;
        for (int i=0; i<vals.length; i++) {
        	mappedColors[i] = (int)mapRange(minFacet, maxFacet, 0.0f, 255.0f, vals[i]);
        	// set a default grayscale color mapping
        	float normColor = mappedColors[i] / 255f;
        	colors[k++] = normColor;
        	colors[k++] = normColor;
        	colors[k++] = normColor;        	
        	colors[k++] = normColor;
        	colors[k++] = normColor;
        	colors[k++] = normColor;        	
        	colors[k++] = normColor;
        	colors[k++] = normColor;
        	colors[k++] = normColor;        	
        } 
        
        // create the color mapper for external use
        colorMapper = new FancyColorMapper();
        colorMapper.addChangeListener(
                new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        if (colorMapper.isAdjusting()) {                      
                            return;
                        }
                        Color[] mapValues = colorMapper.getColorScale().getColorMap();
                        updateColors(mapValues);                       
                    }
                }
           );     
    }
    
	/**
	 * Method to compute a facet value based default set of gray scale stretched VBO-compatible colors for the Ancillary FITS data. 
	 * The colors are linearly mapped to the range of facet values.
	 * A FancyColorMapper is also created to allow any calling classes to create and apply new color stretches.
	 *
     * <thread-safe?>
     */
    public void computeUnrolledVertexColors() {
		// TODO only mapping colors using the first data column until customer defines 
		// desired behavior for multiple data columns
		String[] facetKeys = facetValues.keySet().toArray(new String[facetValues.size()]);
		float[] vals = facetValues.get(facetKeys[0]);
    	if (vals == null) {
    		log.aprint("No facet values from a matching FITS file...");
    		return;
    	}
        colors = new float[vals.length * 9];
        
        float maxFacet = 0f;
        float minFacet = Float.MAX_VALUE;
        for (int j=0; j<vals.length; j++) {
        	if (vals[j] > maxFacet) {
        		maxFacet = vals[j];
        	}
        	if (vals[j] < minFacet) {
        		minFacet = vals[j];
        	}        	
        }
        
        mappedColors = new int[vals.length];
        int k=0;
        for (int i=0; i<vals.length; i++) {
        	mappedColors[i] = (int)mapRange(minFacet, maxFacet, 0.0f, 255.0f, vals[i]);
        	// set a default grayscale color mapping
        	float normColor = mappedColors[i] / 255f;
        	colors[k++] = normColor;
        	colors[k++] = normColor;
        	colors[k++] = normColor;        	
        	colors[k++] = normColor;
        	colors[k++] = normColor;
        	colors[k++] = normColor;        	
        	colors[k++] = normColor;
        	colors[k++] = normColor;
        	colors[k++] = normColor;        	
        } 
        
        colorMapper = new FancyColorMapper();
        colorMapper.addChangeListener(
                new ChangeListener()
                {
                    public void stateChanged(ChangeEvent e)
                    {
                        if (colorMapper.isAdjusting()) {                      
                            return;
                        }
                        Color[] mapValues = colorMapper.getColorScale().getColorMap();
                        updateColors(mapValues);                       
                    }
                }
           );
        
    }

       
    /**
     * Get the color mapper used to stretch the Ancillary FITS data.
     * @return FancyColorMapper for this mesh.
     */
    public FancyColorMapper getColorMapper(){
    	return colorMapper;
    }    
	
    /**
     * Given two ranges [a1, a2] and [b1, b2] then value s is linearly mapped to a return value in [b1, b2]
     *
     * @param a1 lower bound of from range
     * @param a2 upper bound of from range
     * @param b1 lower bound of to range
     * @param b2 upper bound of to range
     * @param s value to map
     * @return returns mapped value from [b1, b2]
     *
     * thread safe
     */
	public static double mapRange(double a1, double a2, double b1, double b2, double s){
		if (Double.compare((a2 - a1), 0.0) == 0) {
//			log.aprint("Divide by zero error! \n");
			return 0f;
		}
		return b1 + ((s - a1)*(b2 - b1))/(a2 - a1);
	}

	/**
	 * Method to return the value associated with a specific facet.
	 *
	 * @param facetId
	 * @return float
	 *
	 * thread-safe
	 */
	public float getFacetValue(int facetId) {
		Float f = indexedData.get(facetId)[0];
		if (f == null) {
			log.aprintln("Attempt to access Ancillary FITS data for an invalid facet ID: "+facetId);
			return Float.NaN;
		} else {
			return f;
		}
	}

	/**
	 * Method to return the facet value error range for a specific facet
	 *
	 * @param facetId
	 * @return float
	 *
	 * thread-safe
	 */
	public float getFacetSigma(int facetId) {
		Float f = indexedErrors.get(facetId)[0];
		if (f == null) {
			log.aprintln("Attempt to access Ancillary FITS  sigma data for an invalid facet ID: "+facetId);
			return Float.NaN;
		} else {
			return f;
		}
	}
	
	/**
	 * Method to return the value(s) associated with a specific facet.
	 *
	 * @param facetId
	 * @return float[]
	 *
	 * thread-safe
	 */
	public float[] getFacetValues(int facetId) {
		float[] f = indexedData.get(facetId);
		if (f == null) {
			log.aprintln("Attempt to access Ancillary FITS data for an invalid facet ID: "+facetId);
			return new float[]{Float.NaN};
		} else {
			return f;
		}
	}

	/**
	 * Method to return the facet value error range(s) for a specific facet
	 *
	 * @param facetId
	 * @return float[]
	 *
	 * thread-safe
	 */
	public float[] getFacetSigmas(int facetId) {
		float[] f = indexedErrors.get(facetId);
		if (f == null) {
			log.aprintln("Attempt to access Ancillary FITS  sigma data for an invalid facet ID: "+facetId);
			return new float[]{Float.NaN};
		} else {
			return f;
		}
	}
	
	/**
	 * Method to return the Label of the primary data columns
	 *
	 * @return label
	 *
	 * thread-safe
	 */
	public List<String> getDataLabels() {
		return dataLabel;
	}
	
	/**
	 * Method to return the Label of the sigma data columns
	 *
	 * @return sigmaLabel
	 *
	 * thread-safe
	 */
	public List<String> getSigmaLabels() {
		return sigmaLabel;
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
		// NOP
	}



	@Override
	public void scaleToShapeModel(boolean canScale) {
		// NOP		
	}



	@Override
	public boolean isScaled() {
		return false;
	}



    @Override
    public void dispose() {
        // TODO Auto-generated method stub
        
    }



	@Override
	/**
	 * This method return a single dimension array of facet colors
	 * Example: {R1, G1, B1, R2, G2, B2,...}
	 * These colors map to the triangles in the associated FITS file 
	 */
	public float[] getColor() {
		return colors;
	}
}
