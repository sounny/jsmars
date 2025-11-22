package edu.asu.jmars.viz3d.renderer.gl;

import java.util.ArrayList;

import edu.asu.jmars.viz3d.renderer.gl.text.BasicText;

public class GLRenderableSet {
	private ArrayList<GLRenderable> renderables = new ArrayList<GLRenderable>();
	
	private boolean renderable = false;
	
	private Object lock = new Object();
	
	/**
	 * Default constructor
	 */
	public GLRenderableSet() {
		
	}
	
	/**
	 * The preferred constructor
	 * @param newRenderables
	 */
	public GLRenderableSet(ArrayList<GLRenderable> newRenderables) {
		renderables = newRenderables;		
	}
	
	/**
	 * Method to return the set's Polygons as a list
	 * @return
	 */
	public ArrayList<GLRenderable> getRenderables() {
		return renderables;
	}
	
	public void setRenderables(ArrayList<GLRenderable> rObjs) {
		if (rObjs != null) {
			renderables.clear();
			renderables.addAll(rObjs);
			renderable = true;
		}
	}
	
	/**
	 * Method to determine if the set actually contains any GLRenderables
	 * @return
	 */
	public boolean hasRenderables() {
		if (renderables.size() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Marks the set as ready to render
	 * @param render
	 */
	public void setIsRenderable(boolean render) {
		synchronized (lock) {
			renderable = render;
		}
	}

	/**
	 * Informs whether the set is ready for rendering
	 * @return
	 */
	public boolean isRenderable() {
		return renderable;
	}
	
	/**
	 * Generic method to release resources
	 */
	public void dispose() {
	    for (GLRenderable r : renderables) {
	    	// This is a silly hack to be used until the renderer gets re-written
	    	if (r instanceof BasicText) {
	    		continue;
	    	}
	        r.dispose();
	    }
		renderables.clear();
		renderable = false;
	}
}
