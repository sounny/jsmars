/**
 * 
 */
package edu.asu.jmars.viz3d.renderer.textures;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * This is a convenience class for converting a BufferedImage into a JOGL texture
 *
 * not thread safe
 */
public class Texture2D {
	
	private Texture tex;
	
	/**
	 * Contructor
	 * @param gl valid GL2 instance
	 * @param image the BufferedImage to be texturized
	 * @throws IOException
	 */
	public Texture2D (GL2 gl, BufferedImage image) throws IOException {
    	// get the texture
		final ByteArrayOutputStream output = new ByteArrayOutputStream() {
		    @Override
		    public synchronized byte[] toByteArray() {
		        return this.buf;
		    }
		};
		
		ImageIO.write(image, "png", output);
		InputStream dstream = new ByteArrayInputStream(output.toByteArray());

		TextureData decalData = TextureIO.newTextureData(gl.getGLProfile(), dstream, false, "png");

		tex = TextureIO.newTexture(decalData);
	}
	
	/**
	 * Public getter for the JOGL texture object
	 *
	 * @return the associated Texture object
	 *
	 * not thread safe
	 */
	public Texture getTexture() {
		return tex;
	}

}
