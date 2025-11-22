package edu.asu.jmars.samples.layer.threshold;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerListener;
import edu.asu.jmars.layer.map2.MapSource;

class NullMapServer implements MapServer {
	List<MapServerListener> listeners = new ArrayList<MapServerListener>();
	Map<String, MapSource> srcs = new LinkedHashMap<String, MapSource>();

	public List<MapSource> getMapSources() {
		return new ArrayList<MapSource>(srcs.values());
	}

	public URI getMapURI() {
		return null;
	}

	public int getMaxRequests() {
		return 1;
	}

	public String getName() {
		return "Null";
	}

	public MapSource getSourceByName(String name) {
		return srcs.get(name);
	}

	public int getTimeout() {
		return 3000;
	}

	public String getTitle() {
		return "Null MapServer";
	}

	public URI getURI() {
		return null;
	}

	public boolean isUserDefined() {
		return true;
	}

	public void loadCapabilities(boolean cached) {}

	public void add(MapSource source) {
		srcs.put(source.getName(), source);
		fireMapSourceChanged(source, MapServerListener.Type.ADDED);
	}

	public void remove(String name) {
		MapSource removed = srcs.remove(name);
		fireMapSourceChanged(removed, MapServerListener.Type.REMOVED);
	}

	public void addListener(MapServerListener l) {
		listeners.add(l);
	}

	public void removeListener(MapServerListener l) {
		listeners.remove(l);
	}
	
	public void fireMapSourceChanged(MapSource source, MapServerListener.Type changeType){
		for(MapServerListener l: new ArrayList<MapServerListener>(listeners)){
			l.mapChanged(source, changeType);
		}
	}

	public void load(String serverName) {}
	public void save() {}
	public void delete() {}

}
