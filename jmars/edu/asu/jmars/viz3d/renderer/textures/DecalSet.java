/**
 * Class to represent a set of Decals for a single Layer at a specific PPD
 */
package edu.asu.jmars.viz3d.renderer.textures;

import java.util.ArrayList;

public class DecalSet {

	private ArrayList<Decal> decals = new ArrayList<Decal>();
	
	private boolean renderable = false;
	
	private boolean displayable = false;
	
	private Object lock = new Object();
	
	/**
	 * Default constructor
	 */
	public DecalSet() {
		
	}
	
	/**
	 * The preferred constructor
	 * @param newDecals
	 */
	public DecalSet(ArrayList<Decal> newDecals) {
		decals = newDecals;		
	}
	
	/**
	 * Method to return the set's Decals as a list
	 * @return
	 */
	public ArrayList<Decal> getDecals() {
		return decals;
	}
	
	/**
	 * Method to determine if the set actually contains any Decals
	 * @return
	 */
	public boolean hasDecals() {
		if (decals.size() > 0) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Marks the set as ready to render
	 * @param render
	 */
	public void setRenderable(boolean render) {
		synchronized (lock) {
			renderable = render;
			if (render == false) {
				displayable = false;
			}
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
	 * @return the displayable
	 */
	public boolean isDisplayable() {
		return displayable;
	}

	/**
	 * @param displayable the displayable to set
	 */
	public void setDisplayable(boolean displayable) {
		synchronized (lock) {
			this.displayable = displayable;
		}
	}

	/**
	 * Generic method to release resources
	 */
	public void dispose() {
		decals.clear();
	}
	
	public int getId() {
		return System.identityHashCode(this);
	}

}
