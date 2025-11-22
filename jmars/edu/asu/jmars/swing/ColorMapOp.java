package edu.asu.jmars.swing;

import edu.asu.jmars.util.*;
import java.awt.*;
import java.awt.image.*;

/**
 * This is a bridge between the ColorMapper and BufferedImage pixels that must
 * be scaled by the mapper. The Java2D LookupOp is the real worker, this only
 * prepares the LookupOp.
 */
public class ColorMapOp
 {
	private static DebugLog log = DebugLog.instance();

	/**
	 ** The byte-index of each color band in a "native" bitmap's
	 ** 24-bit pixels. To determine which band index of a LookupOp
	 ** affects alpha, for example, use A as an index.
	 **/
	private int A,R,G,B;

	/**
	 ** Indicates whether the optimized native BufferedImages use
	 ** premultiplied alphas. This is generally the case under OSX,
	 ** and was added specifically to address some bugs there.
	 **/
	private boolean preMultiplied;

	/*
	 * If a non-null BufferedImage is provided to the Constructor, it is used to create 
	 * a single pixel ARGB image to determine what band indices contain the ARGB channels. 
	 * This result is used to index the ARGB values, which are created by linearly interpolating 
	 * either the grayscale range, or a sequence of colors if specified in the constructor.
	 * 
	 * If a null value is provided for the BufferedImage, a System standard image is created
	 * and used to determine the ordering of the ARGB indices instead.
	 * 
	 * This was originally done as a static block operation, but the order of the ARGB bands 
	 * can vary between images during runtime, and ignoring this difference produces unexpected
	 * results, such as grayscale images suddenly turning blue as the slider values are adjusted.
	 */
	private void determineARGBpos(BufferedImage imageToOperateOn) {
			BufferedImage img = null;
			
			if (imageToOperateOn==null) {
				// Construct a 1x1 image with alpha
				img =
		 			GraphicsEnvironment
					.getLocalGraphicsEnvironment()
					.getDefaultScreenDevice()
					.getDefaultConfiguration()
					.createCompatibleImage(1, 1, Transparency.TRANSLUCENT);
			} else {
				img = Util.createCompatibleImage(imageToOperateOn,1, 1);	
			}				
			
			preMultiplied = img.isAlphaPremultiplied();
			img.coerceData(false); // UN-premultiply the pixels, if necessary
			

			// Create a dummy op to figure out which bands map to which
			// bytes. This will convert any zero-byte pixels to 0x10+band
			// number.
			byte[][] opBands = new byte[4][256];
			opBands[0][0] = 1;
			opBands[1][0] = 2;
			opBands[2][0] = 3;
			opBands[3][0] = 4;

			// Paint in a test pixel with zeros as the ARGB values, then
			// transform it with the op. The result will be an ARGB value
			// composed of bytes that represent op band indices that
			// affected that byte.
			img.setRGB(0, 0, 0);
			LookupOp op = new LookupOp(new ByteLookupTable(0, opBands), null);
			img = op.filter(img, null);

			// For each byte in an ARGB pixel, determine which band number
			// it was affected by in the op.
			int[] argbBands = { -1, -1, -1, -1 };
			int realPixel = img.getRGB(0, 0);
			for(int i=0; i<4; i++)
				argbBands[3-i] = (realPixel >> i*8) & 0xFF;
			A = argbBands[0]-1;
			R = argbBands[1]-1;
			G = argbBands[2]-1;
			B = argbBands[3]-1;
			
			log.println("A:" + A+ " R:" + R+" G:" + G+" B:" + B+" Alpha-premultiplied:"+preMultiplied);
	}
	
	
	
	private Color[] colors;

	public ColorMapOp(BufferedImage image)
	 {
		determineARGBpos(image);
	 }

	public ColorMapOp(Color[] colors, BufferedImage image)
	 {
		determineARGBpos(image);
		this.colors = (Color[]) colors.clone();
	 }

	public ColorMapOp(ColorScale scale, BufferedImage image)
	 {
		determineARGBpos(image);
		if(!scale.isIdentity())
			colors = scale.getColorMap();
	 }

	public boolean isIdentity()
	 {
		return  colors == null;
	 }

	float _alpha = -1;
	BufferedImageOp _forAlpha;
	public BufferedImageOp forAlpha(float alpha)
	 {
		if(alpha != _alpha)
		 {
			_alpha = alpha;
			_forAlpha = createOp(alpha);
		 }
		return  _forAlpha;
	 }

	private BufferedImageOp createOp(float alpha)
	 {
		byte[][] bytes = new byte[4][256];
		if(isIdentity()){
			if(preMultiplied){
				for(int i=0; i<256; i++){
					bytes[A][i] = (byte) Math.round(alpha * i);
					bytes[R][i] = (byte) Math.round(alpha * i);
					bytes[G][i] = (byte) Math.round(alpha * i);
					bytes[B][i] = (byte) Math.round(alpha * i);
				 }
			}
			else{
				for(int i=0; i<256; i++){
					bytes[A][i] = (byte) Math.round(alpha * i);
					bytes[R][i] = (byte) i;
					bytes[G][i] = (byte) i;
					bytes[B][i] = (byte) i;
				 }
			}
		}else{
			if(preMultiplied){
				for(int i=0; i<256; i++){
					Color col = colors[i];
					bytes[A][i] = (byte) Math.round(alpha * i);
					bytes[R][i] = (byte) Math.round(alpha * col.getRed());
					bytes[G][i] = (byte) Math.round(alpha * col.getGreen());
					bytes[B][i] = (byte) Math.round(alpha * col.getBlue());
				 }
			}
			else{
				//Use a different lookup table that also has the alpha channel 
				// passed in.  This LookupTable sets the new alpha based on the
				// incoming color index, not the incoming alpha value. This allows
				// it to map to the alphas of the colors specified on in the colors[] 
				// array, which comes from the color scale.
				for(int i=0; i<256; i++) {
					Color col = colors[i];
					bytes[A][i] = (byte) col.getAlpha();
					bytes[R][i] = (byte) col.getRed();
					bytes[G][i] = (byte) col.getGreen();
					bytes[B][i] = (byte) col.getBlue();
	
				 }
				return new LookupOp(new ColorByteLookupTable(bytes, A), null);
			}
		}

		return  new LookupOp(new ByteLookupTable(0, bytes), null);
	 }
 }
