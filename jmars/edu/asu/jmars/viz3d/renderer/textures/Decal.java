package edu.asu.jmars.viz3d.renderer.textures;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RescaleOp;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.util.texture.Texture;

import edu.asu.jmars.ProjObj;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;
import edu.asu.jmars.viz3d.renderer.gl.terrain.TerrainTile;

/**
 * This class represents a BufferedImage along with the necessary buffers and geometry
 * necessary to render the image as a Texture (Decal) in JOGL.
 */
public class Decal implements GLRenderable, Disposable {
	/* Buffer indices necessary for the graphics card to reference */
    public static final int DECAL_VERTEX_DATA = 0;
	public static final int DECAL_TEXTURE_DATA = 1;
	public static final int DECAL_INDEX_DATA = 2;
	public static final int DECAL_NORMAL_DATA = 3;
	
	private TerrainTile tile = null;
	private int[] bufferObj = new int[4];
	private float[] vertices = null;
	private BufferedImage image = null;
	private long imageCheckSum = Long.MIN_VALUE;
	private Texture decalTexture = null;
	private IntBuffer decalTris = null;
	private FloatBuffer vertBuffer = null;
	private FloatBuffer texBuffer = null; 
	private FloatBuffer normBuffer = null; 
	private float alpha = 1f;
    private Float displayAlpha = 1f;
	
	private ProjObj projection = null;
	private Point2D minExtent = null;
	private Point2D maxExtent = null;
	private int ppd = 8; // probably a safe default for 3D
	
    private static DebugLog log = DebugLog.instance();
	
   	private int layerStateId = -1;
	
	public int getStateId() {
		return layerStateId;
	}
	
	public void setStateId(int newId) {
		layerStateId = newId;
	}
	

    /* flag to determine if this Decal has been prepped for rendering at least once */
	public boolean hasBeenDisplayed = false;
	/* Show probably be removed in the near future */
	public Point2D center;
	
	private float[] orgAlphaRaster = null;

	/**
	 * Constructor
	 * @param param fully populated DecalParameter object
	 */
	public Decal(DecalParameter param) { 
		minExtent = param.getStart();
		maxExtent = param.getEnd();
		this.setCenter(param.getMid());
		projection = param.getProjection();
		this.ppd = param.getPpd();
		this.tile = param.getTile();
	}
	
	/**
	 * Method to take the BufferedImage and regardless of type, create an equivalent BufferedImage
	 * that is a power of 2 in size in both dimensions and has an alpha channel. A copy of the the
	 * "original" alpha raster is saved for future opacity changes. Finally the system specified
	 * opacity is applied to the image.  
	 */
	private void processImage() {
    	int newHeight = 0;
    	int newWidth = 0;
    	if(!isPowerOf2(image.getWidth())) { 
    		// get the next higher power of two
    		newWidth = (int)Math.pow(2.0, Math.ceil(Math.log(image.getWidth())/Math.log(2)));
    	}
    	
    	if(!isPowerOf2(image.getHeight())) {
    		newHeight = (int)Math.pow(2.0, Math.ceil(Math.log(image.getHeight())/Math.log(2)));
    	}
    	
    	// resize the image to a power of 2 as necessary - TODO do this first using GraphicsConfiguration.createCompatibleImage
    	if (newWidth > 0 || newHeight > 0) {
    		BufferedImage testImage = GraphicsEnvironment.getLocalGraphicsEnvironment()
    				.getDefaultScreenDevice()
    				.getDefaultConfiguration()
    				.createCompatibleImage(newWidth > 0 ? newWidth : image.getWidth(), newHeight > 0 ? newHeight : image.getHeight(), image.getTransparency());

			Graphics2D g = testImage.createGraphics();
			//g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)); // TODO this is a slow operation - you are applying alpha-slider value in setAlpha() differently in addition to here
			g.drawImage(image, 0, 0, testImage.getWidth(), testImage.getHeight(), null);
			g.dispose();

//	       	AffineTransform scaleInstance = AffineTransform.getScaleInstance(newWidth > 0 ? (double)newWidth/image.getWidth() : 1.0, newHeight > 0 ? (double)newHeight/image.getHeight() : 1.0);
//	    	AffineTransformOp scaleOp = new AffineTransformOp(scaleInstance, AffineTransformOp.TYPE_BICUBIC);
//	    	scaleOp.filter(image, testImage);	
    	    image = testImage;
    	}
		
		if (image != null && minExtent != null && maxExtent != null) {
			// create an image with the alpha value applied			
	        	if (image.isAlphaPremultiplied() || image.getType() != BufferedImage.TYPE_4BYTE_ABGR) {
					BufferedImage tmp = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR); // TODO conversion to standard 4-byte ABGR format should be the last thing you do
					Graphics2D g = tmp.createGraphics();
					//g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)); // TODO this is a slow operation - you are applying alpha-slider value in setAlpha() differently in addition to here
					g.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
					g.dispose();
					image = tmp;
	        	}
				
	        	// save the original alpha raster for future opacity applications
//	        	if (orgAlphaRaster == null) {
//		    		WritableRaster raster = image.getAlphaRaster();
//		    		orgAlphaRaster = new float[raster.getWidth() * raster.getHeight()];
//		    		raster.getPixels(0, 0, raster.getWidth(), raster.getHeight(), orgAlphaRaster);
//	        	}
	        	
	        	// apply the system defined opacity level for this Decal
//			setAlpha();
		}
		
	}

	/**
	 * Method to set the alpha channel of the image to the system defined opacity level
	 */
//	public void setAlpha() {  
//		if (image == null ) {
//			log.aprintln("Decal image not set. Unable to apply Layer alpha slider value.");
//			return;
//		}
//		WritableRaster raster = image.getAlphaRaster();
//		int width = raster.getWidth();
//		int height = raster.getHeight();
//		float[] alphas = new float[width * height];
//		if (orgAlphaRaster == null || alphas.length != orgAlphaRaster.length) {
//			log.aprintln("Decal image was updated improperly. Original alpha raster does not match current alpha raster. Unable to apply Layer alpha slider value.");
//			return;
//		}
//		raster.getPixels(0, 0, width, height, alphas);
//		for (int i=0; i<alphas.length; i++) {
//			alphas[i] = displayAlpha * orgAlphaRaster[i];
//		}
//		raster.setPixels(0, 0, width, height, alphas);
//	}
	
	private static BufferedImage createTransformed(BufferedImage image, AffineTransform at) {
		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
		Graphics2D g = newImage.createGraphics();
		g.transform(at);
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return newImage;
	}
	
	private BufferedImage deepCopy(BufferedImage bi)/*method to clone BufferedImage*/ {
		if (bi == null) {
			return bi;
		}
		   ColorModel cm = bi.getColorModel();
		   boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		   WritableRaster raster = bi.copyData(null);
		   return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
		}
	
	private BufferedImage copyAndResize(BufferedImage source, int width, int height) {
	    ColorModel cm = source.getColorModel();
	    boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
	    WritableRaster raster = source.copyData(null);
	    SampleModel sm = raster.getSampleModel().createCompatibleSampleModel(width, height);
	    WritableRaster newRaster = WritableRaster.createWritableRaster(sm, null);
	    BufferedImage newBi = new BufferedImage(cm, newRaster, isAlphaPremultiplied, null);
	    return newBi;
	}
	
	/**
	 * Method to load the image into a JOGL Texture object for rendering
	 * @param gl JOGL API reference
	 */
	public void loadTexture(GL gl) {
        if (image != null){       	     	
        	// get the texture
    		decalTexture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);
       }
	}
	
	/**
	 * Method to generate the OpenGL buffer reference needed for rendering
	 * @param gl JOGL API reference
	 */
	public void genBuffers(GL2 gl) {
		if (bufferObj == null) {
			bufferObj = new int[4];
		}
		gl.glGenBuffers(4, bufferObj, 0);
		if (bufferObj[0] < 1) {
			// generate error message
			System.err.println("Error generating VBO ID");
			return;
		}
	}
	
	/**
	 * Method to delete the OpenGL buffer reference needed for rendering
	 * @param gl JOGL API reference
	 */
	public void delBuffers(GL2 gl) {
		gl.glDeleteBuffers(4, bufferObj, 0);
	}
	
	/** 
	 * Method to return the OpenGL Buffer IDs or "pointers"
	 * @return int array of buffer IDs
	 */
	public int[] getBufferObj() {
		return bufferObj;
	}

	/**
	 * Method to return all the vertices of the underlying Decal geometry
	 * @return single dimension array of 3D vertex coordinates
	 */
	public float[] getVertices() {
		if (tile != null) {
			return tile.getVerts();
		} else {
			return vertices;
		}
	}
	
	/**
	 * Method to return all the normal vectors of the underlying Decal geometry
	 * @return single dimension array of 3D vertex coordinates
	 */
	public float[] getNormals() {
		if (tile != null) {
			return tile.getNorms();
		} else {
			return vertices;
		}
	}

	/**
	 * Method to return underlying Decal geometry as a Java NIO buffer
	 * @return single dimension array of 3D vertex coordinates
	 */
	public FloatBuffer getVertexBuffer() {
		if (vertBuffer == null && getVertices() != null) {
			vertBuffer = FloatBuffer.wrap(getVertices());
		}
		return vertBuffer;
	}

	/**
	 * Method to return underlying Decal geometry as a Java NIO buffer
	 * @return single dimension array of 3D vertex coordinates
	 */
	public FloatBuffer getNormalBuffer() {
		if (normBuffer == null && getNormals() != null) {
			normBuffer = FloatBuffer.wrap(getNormals());
		}
		return normBuffer;
	}

	/**
	 * Method to return the underlying geometry (triangle) indices
	 * as an integer array
	 * @return triangle indices
	 */
	public int[] getIndices() {
		if (tile != null) {
			return tile.getIndices();
		} else {
			return null;
		}
	}

	/**
	 * Returns the JOGL Texture coordinates (U, V) of this Decal
	 * @return as a single dimension float array
	 */
	public float[] getTexCoords() {
		if (tile != null) {
			return tile.getTexs();
		} else {
			return null;
		}
	}
	
	/**
	 * Returns the JOGL Texture coordinates (U, V) of this Decal
	 * @return the Texture coordinates as a Java NIO buffer
	 */
	public FloatBuffer getTextureBuffer() {
		if (texBuffer == null && getTexCoords() != null) {
			texBuffer = FloatBuffer.wrap(getTexCoords());
		}
		return texBuffer;
	}

	/**
	 * Returns the JOGL Texture object of the Decal image 
	 * @return the Texture
	 */
	public Texture getDecalTexture() {
		return decalTexture;
	}

	/**
	 * Method to return the underlying geometry (triangle) indices
	 * as a Java NIO buffer
	 * @return buffer of indices
	 */
	public IntBuffer getDecalTris() {
		if (decalTris == null && tile != null) {
			decalTris = IntBuffer.wrap(tile.getIndices());
		}
		return decalTris;
	}

	/**
	 * Method to set the BufferedImage used to generate a JOGL Texture.
	 * This method automatically generates the image processing process
	 * to apply any system dictated opacity changes.
	 * @param img
	 */
	public void setImage(BufferedImage img) {
		if (img == null) {
			log.aprintln("Cannot add a null BufferedImage to a Decal!");
			return;
		}

		//get the new checksum
		long checksum = Util.calcChecksumForImage(img);
		//if it's the same as the old, the images are the same, don't do any more work
		if(checksum == imageCheckSum){
			return;
		}
		
		imageCheckSum = checksum;
		image = img;
		orgAlphaRaster=null;
	    ImageUtil.flipImageVertically(image);
		processImage();
		
		this.hasBeenDisplayed = false;
	}
	
	/**
	 * Method to return the associated BufferedImage
	 * @return the image or null if it does not exist
	 */
	public BufferedImage getImage() {
		return image;
	}

	/**
	 * Return the width of the BufferedImage, if a BufferedImage has been applied
	 * @return width or zero if no image
	 */
	public int getWidth() {
		if (image != null) {
			return image.getWidth();
		} else {
			return 0;
		}
	}
	
	/**
	 * Return the height of the BufferedImage, if a BufferedImage has been applied
	 * @return height or zero if no image
	 */
	public int getHeight() {
		if (image != null) {
			return image.getHeight();
		} else {
			return 0;
		}
	}
	
	/** 
	 * Method to return the preferred Ocentric projection to be 
	 * used in generating the BufferedImage for the Decal.
	 * @return
	 */
	public ProjObj getProjection() {
		return projection;
	}
	
	/**
	 * Method to inform the PPD this Decal was created for
	 * @return Pixels per Degree (PPD)
	 */
	public int getPPD() {
		return ppd;
	}
	
	/**
	 * Method to return the minimum extent of the Decal in world coordinates
	 * @return X, Y world coordinates
	 */
	public Point2D getMinExtent() {
		return minExtent;
	}
	
	/**
	 * Method to return the maximum extent of the Decal in world coordinates
	 * @return X, Y world coordinates
	 */
	public Point2D getMaxExtent() {
		return maxExtent;
	}

	/**
	 * Generic resource cleanup method
	 */
	public void dispose() {
		bufferObj = null;
		vertices = null;
		image = null;
		decalTexture = null;
		tile = null;
	}
	
	/**
	 * Method to inform if an integer is a power of 2
	 * @param i integer of interest
	 * @return true if a power of 2
	 */
	boolean isPowerOf2(int i) {
		return i > 2 && ((i&-i)==i);
	}
	
	/**
	 * Utility method to scale an RGB Integer BufferedImage. 
	 * Does not modify the source image.
	 * @param src source image
	 * @param w desired width
	 * @param h desired height
	 * @return the scaled BufferedImage
	 */
	public static BufferedImage scale(BufferedImage src, int w, int h)
	{
	    BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
	    int x, y;
	    int ww = src.getWidth();
	    int hh = src.getHeight();
	    int[] ys = new int[h];
	    for (y = 0; y < h; y++)
	        ys[y] = y * hh / h;
	    for (x = 0; x < w; x++) {
	        int newX = x * ww / w;
	        for (y = 0; y < h; y++) {
	            int col = src.getRGB(newX, ys[y]);
	            img.setRGB(x, y, col);
	        }
	    }
	    return img;
	}
	
	/**
	 * Method to inform whether the Decal has had a Texture created and applied
	 * @return true if a Texture exists
	 */
	public boolean hasTexture() {
		if (decalTexture != null) {
			return true;
		} else {
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#execute(com.jogamp.opengl.GL2)
	 */
	@Override
	public void execute(GL2 gl) {
		// TODO Auto-generated method stub
		
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#preRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void preRender(GL2 gl) {
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
		delBuffers(gl);
		dispose();	
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
	
	/**
	 * Method to apply a change in the Layer level opacity and force a change in the 3D View
	 * If the value of alpha is to within ~ 1e-7 of the existing value, no change is made.
	 * @param alpha
	 */
	public void setDisplayAlphaAndDisplay(float alpha) {
		if (image == null || FloatUtil.isEqual(displayAlpha, alpha, FloatUtil.EPSILON)) {
			return;
		}
		displayAlpha = alpha;
		this.hasBeenDisplayed = false;
	}
	

	/**
	 * @return the center
	 */
	public Point2D getCenter() {
		return center;
	}

	/**
	 * @param center the center to set
	 */
	public void setCenter(Point2D center) {
		this.center = center;
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
	}
	
	/**
	 * Method to combine the Decal argument to this Decal.
	 * The image in the argument Decal will be drawn OVER the the image in this Decal
	 * blending the alpha channels.
	 * 
	 * @param d
	 */
	public void blend(Decal d) {		
		BufferedImage overlay = d.getImage();
		if (overlay == null) {
			return;
		}
		if (image == null) {
			// we need to handle the case of blending a Decal image into a non-existent base Decal
			// in other words apply the alpha and create a base Decal
			image = new BufferedImage(overlay.getWidth(), overlay.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
			Graphics2D g = image.createGraphics();
			g.drawImage(overlay, new RescaleOp(new float[]{1.0f, 1.0f, 1.0f, /* alpha scaleFactor */ d.getDisplayAlpha()}, new float[]{0f, 0f, 0f, /* alpha offset */ 0f}, null), 0, 0);
			g.dispose();
			return;
		}
		// paint both images, preserving the alpha channels
		Graphics2D g = image.createGraphics();
		g.drawImage(overlay, new RescaleOp(new float[]{1.0f, 1.0f, 1.0f, /* alpha scaleFactor */ d.getDisplayAlpha()}, new float[]{0f, 0f, 0f, /* alpha offset */ 0f}, null), 0, 0);
		g.dispose();
	}
	
	public void clearImage() {
		if (image == null) {
			// nothing to do
			return;
		}
		image = null;
		this.hasBeenDisplayed = false;
	}

	@Override
	public void scaleToShapeModel(boolean canScale) {
		// NOP	
	}

	@Override
	public boolean isScaled() {
		return false;
	}
	
	/**
	 * This method returns null - ALWAYS as color is N/A
	 */
	public float[] getColor() {
		return null;
	}	
}
