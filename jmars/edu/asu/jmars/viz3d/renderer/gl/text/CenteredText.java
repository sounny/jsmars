package edu.asu.jmars.viz3d.renderer.gl.text;

import java.awt.geom.Rectangle2D;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLDrawable;

import edu.asu.jmars.viz3d.ThreeDManager;

/**
 * 
 * @author whagee
 *
 */

public class CenteredText extends BasicText {
	
	int verticalSpace = 0;
	
	public CenteredText(String text, float[] color, int xPosition, int yPosition) {
		super(text, color, xPosition, yPosition);
	}

	
	@Override
	public void execute(GL2 gl) {
		GLDrawable drawable = gl.getContext().getGLDrawable();
		renderer.setColor(color[0], color[1], color[2], getDisplayAlpha());	
		
		// center in the screen
		Rectangle2D bounds = renderer.getBounds(text);
		float width = (float)bounds.getWidth();
		xPos = (int) ((ThreeDManager.getInstance().getWindow().getSurfaceWidth() / 2) - (width / 2));
		yPos = ThreeDManager.getInstance().getWindow().getSurfaceHeight() / 2;
		if (verticalSpace != 0) {
			float height = (float)bounds.getHeight();
			height += (height * verticalSpace * 0.1f);	// this is so random
			yPos -= height;
		}
		if (zPos == Integer.MAX_VALUE) {
			// There is no Z coord, so we want to render on a virtual "glass pane" at the very front of the display window
			renderer.beginRendering(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
			renderer.draw(text, xPos, yPos);
			renderer.endRendering();
		} else {
			// There is a valid Z coord, so render this text in 3D
			renderer.begin3DRendering();
			renderer.draw3D(text, xPos, yPos, zPos, scale);
			renderer.endRendering();
		}
		renderer.flush();
	}

	/**
	 * Method to set the vertical order of centered text
	 * @return numerical order
	 */
	public int getVerticalSpace() {
		return verticalSpace;
	}


	public void setVerticalSpace(int verticalSpace) {
		this.verticalSpace = verticalSpace;
	}


}
