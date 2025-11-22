package edu.asu.jmars.layer.mosaics;

import java.util.HashSet;

import edu.asu.jmars.layer.DataReceiver;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.ProjectionEvent;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.RefWorldCache;
import edu.asu.jmars.layer.util.features.WorldCache;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.ObservableSet;
import edu.asu.jmars.layer.mosaics.MosaicsLView.InitialParams;

public class MosaicsLayer extends Layer {
	FeatureCollection fc;
	ObservableSet<Feature> selections = new ObservableSet<Feature>(new HashSet<Feature>());
	private WorldCache worldCache = new RefWorldCache();
	private InitialParams settings = null;
	
	public MosaicsLayer(String url, InitialParams settingsObj) {
		FeatureProvider fp = new FeatureProviderWMS();
		if (url==null || url.length()==0) {
			url = Config.get("mosaics.server.url");
		}
		fc = fp.load(url);
		if (settingsObj == null) {
			settings = new InitialParams(); //a new one for fresh layer 
			settings.url = url;
		} else {
			settings = settingsObj;//serialized object when loading a session
		}
		
	}

	public void receiveRequest(Object layerRequest, DataReceiver requester) {
		broadcast(fc.getFeatures());
	}
	
	public FeatureCollection getFeatures(){
		return fc;
	}
	
	public void projectionChanged(ProjectionEvent e) {
		super.projectionChanged(e);
		worldCache = new RefWorldCache();
	}
	
	public WorldCache getCache() {
		return worldCache;
	}
	public InitialParams getSettings() {
		return settings;
	}
}
