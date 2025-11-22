package edu.asu.jmars.layer.util.filetable;

import edu.asu.jmars.layer.util.features.FeatureCollection;

public class DefaultChangedEvent {
	public FeatureCollection fc;
	public int index;
	
	public DefaultChangedEvent(FeatureCollection fc, int index) {
		super();
		this.fc = fc;
		this.index = index;
	}

}
