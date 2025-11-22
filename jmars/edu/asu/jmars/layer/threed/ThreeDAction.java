package edu.asu.jmars.layer.threed;

import com.jogamp.opengl.GL;

/**
 *	Interface for JOGL actions that need to be executed on the current JOGL context. 
 *
 */
public interface ThreeDAction {
	public void execute(GL target);
}
