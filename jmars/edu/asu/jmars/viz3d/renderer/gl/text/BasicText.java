/**
 * 
 */
package edu.asu.jmars.viz3d.renderer.gl.text;

import java.awt.Font;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.util.awt.TextRenderer;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.viz3d.Disposable;
import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;

/**
 * This class abstracts the com.jogamp.opengl.util.awt.TextRenderer class in JOGL 
 *
 * To draw static text in the 3D window
 *
 * JOGL v. 2.3.1 or above
 *
 * not thread safe
 */
public class BasicText implements GLRenderable, Disposable{	
	/**
	 * The default font. Made public for situations where the font 
	 * was changed and needs to be changed back to the default.
	 */
	public static Font BASIC_TEXT_FONT = ThemeFont.getRegular();	
	public static String noText = "";
	Font font = BASIC_TEXT_FONT;
	float[] color;
	String text;
	int xPos = Integer.MAX_VALUE;
	int yPos = Integer.MAX_VALUE;
	int zPos = Integer.MAX_VALUE;
	int actualXPos;
	float scale;
	TextRenderer renderer;
	
	private float alpha = 1f;
	private Float displayAlpha;
	
	private boolean wasRendered = false;
	private boolean hidden = false;

	public BasicText() {
		
	}
	/**
	 * Constructor
	 * @param text a String representing the text to be drawn
	 * @param color color vector (R, G, B, A) with each color band and opacity independently normalized
	 * @param xPosition the 3D window X SCREEN position of the left side of the text in pixels
	 * @param yPosition the 3D window Y SCREEN position of the left side of the text in pixels
	 */
	public BasicText(String text, float[] color, int xPosition, int yPosition) {
		this.text = text;
		this.color = color;
		xPos = xPosition;
		yPos = yPosition;
	}
	
	/**
	 * 
	 * @param text a String representing the text to be drawn
	 * @param color color vector (R, G, B, A) with each color band and opacity independently normalized
	 * @param xPosition the 3D window X position of the left side of the text in pixels
	 * @param yPosition the 3D window Y position of the left side of the text in pixels
	 * @param zPosition the 3D window Y position of the left side of the text in pixels
	 * @param scaleFactor amount to scale the text by
	 */
	public BasicText(String text, float[] color, int xPosition, int yPosition, int zPosition, float scaleFactor) {
		this.text = text;
		this.color = color;
		xPos = xPosition;
		yPos = yPosition;
		zPos = zPosition;
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.Disposable#dispose()
	 */
	@Override
	public void dispose() {
		if (renderer != null) {
			renderer.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#execute(com.jogamp.opengl.GL2)
	 */
	@Override
	public void execute(GL2 gl) {
		GLDrawable drawable = gl.getContext().getGLDrawable();
		renderer.setColor(color[0], color[1], color[2], getDisplayAlpha());	
		String tmpText = (hidden ? noText : text);
		if (zPos == Integer.MAX_VALUE) {
			// There is no Z coord, so we want to render on a virtual "glass pane" at the very front of the display window
			renderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
			renderer.draw(tmpText, xPos, yPos);
			renderer.endRendering();
		} else {
			// There is a valid Z coord, so render this text in 3D
			renderer.begin3DRendering();
			renderer.draw3D(tmpText, xPos, yPos, zPos, scale);
			renderer.endRendering();
		}
		renderer.flush();
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#preRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void preRender(GL2 gl) {
		if (!wasRendered) {
			// following appears to fix the issue with microscopic font rendering on a retina display
			float mult = 100/(float)gl.getContext().getGLDrawable().getNativeSurface().convertToWindowUnits(new int[]{100,100})[0];
			if (mult > 0){
				renderer = new TextRenderer(font.deriveFont(font.getSize2D()*mult));
			} else {
				renderer = new TextRenderer(font);
			}
			wasRendered = true;
		}
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#postRender(com.jogamp.opengl.GL2)
	 */
	@Override
	public void postRender(GL2 gl) {
		wasRendered = false;
		delete(gl);
	}

	/* (non-Javadoc)
	 * @see edu.asu.jmars.viz3d.renderer.gl.GLRenderable#delete(com.jogamp.opengl.GL2)
	 */
	@Override
	public void delete(GL2 gl) {
		if (renderer != null) {
			renderer.dispose();
			renderer = null;
		}
	}

	/**
	 * Returns the Font used to render
	 * 
	 * @return Font
	 *
	 * thread-safe
	 */
	public Font getFont() {
		return font;
	}

	/**
	 * Sets the Font
	 *
	 * @param font
	 *
	 * not thread-safe
	 */
	public void setFont(Font font) {
		this.font = font;
		wasRendered = false;
	}

	/**
	 * Retrieves the current color of the text
	 *
	 * @return color as a float[]
	 * @throws
	 *
	 * thread-safe
	 */
	public float[] getColor() {
		return color;
	}

	/**
	 * Sets the color of the text
	 *
	 * @param color
	 * @throws
	 *
	 * thread-safe
	 */
	public void setColor(float[] color) {
		this.color = color;
	}

	/**
	 * Returns the current text String being rendered
	 *
	 * @return String
	 *
	 * thread-safe
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the text to be rendered
	 *
	 * @param text
	 *
	 * thread-safe
	 */
	public void setText(String text) {
		this.text = text;
	}

	/**
	 * Returns the X screen position the text will be rendered at
	 *
	 * @return int
	 *
	 * thread-safe
	 */
	public int getXPos() {
		return xPos;
	}

	/**
	 * Sets the X screen position the text will be rendered at
	 *
	 * @param xPos
	 *
	 * not thread-safe
	 */
	public void setXPos(int xPos) {
		this.xPos = xPos;
	}

	/**
	 * Returns the Y screen position the text will be rendered at
	 *
	 * @return int
	 *
	 * thread-safe
	 */
	public int getYPos() {
		return yPos;
	}

	/**
	 * Sets the X screen position the text will be rendered at
	 *
	 * @param yPos
	 *
	 * not thread-safe
	 */
	public void setYPos(int yPos) {
		this.yPos = yPos;
	}

	/**
	 * Returns the Z screen position the text will be rendered at
	 *
	 * @return int
	 *
	 * thread-safe
	 */
	public int getZPos() {
		return zPos;
	}

	/**
	 * Sets the Z screen position the text will be rendered at
	 *
	 * @param zPos
	 *
	 * not thread-safe
	 */
	public void setZPos(int zPos) {
		this.zPos = zPos;
	}

	/**
	 * Returns the scale factor used to scale the text when rendered in 3D (depth)
	 *
	 * @return float
	 *
	 * thread-safe
	 */
	public float getScale() {
		return scale;
	}

	/**
	 * Sets the scale factor used to render the text in 3D
	 * <Description>
	 *
	 * @param scale
	 * @throws
	 *
	 * not thread-safe
	 */
	public void setScale(float scale) {
		this.scale = scale;
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
		// NOP
	}
	@Override
	public void scaleToShapeModel(boolean canScale) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean isScaled() {
		return false;
	}
	/**
	 * @return the hidden
	 */
	public boolean isHidden() {
		return hidden;
	}
	/**
	 * @param hidden the hidden to set
	 */
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
		if (renderer != null) {
			renderer.flush();
		}
	}

}
