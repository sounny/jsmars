package edu.asu.jmars.layer.investigate;

import java.awt.MouseInfo;
import java.awt.Point;

import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.InvestigateDisplay;
import edu.asu.jmars.viz3d.renderer.gl.event.IntersectListener;
import edu.asu.jmars.viz3d.renderer.gl.event.IntersectResult;

/**
 * Investigate Layer implementation of Investigate Listener 
 * Used to transfer selected mouse intercept data from 3D
 * to the Investigate tool for display to the user.
 *
 * not thread-safe
 */
//TODO this should be moved into the Investigate layer package
public class InvestigateListenerImpl implements IntersectListener{
	public void setResults(IntersectResult result) {
		if(ToolManager.getToolMode() == ToolManager.INVESTIGATE){
			InvestigateDisplay.getInstance().set3DInvData(result);
			Point p = MouseInfo.getPointerInfo().getLocation();
			InvestigateDisplay.showInvDisplayFrom3D(p.x, p.y);
		}
	}

}
