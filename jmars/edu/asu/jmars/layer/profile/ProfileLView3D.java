package edu.asu.jmars.layer.profile;

import edu.asu.jmars.layer.Layer.LView3D;


public class ProfileLView3D extends LView3D {
	private ProfileLayer myLayer;

	public ProfileLView3D(ProfileLayer layer) {
		super(layer);
		myLayer = layer;
	}
	
	
	@Override
	public boolean isEnabled(){
		return false;
	}
	
	@Override
	public boolean exists(){
		return false;
	}

}

