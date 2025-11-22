package edu.asu.jmars.layer.stamp;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerDefault;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapServerListener;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.stamp.sources.AverageStampSource;
import edu.asu.jmars.layer.stamp.sources.CountStampSource;
import edu.asu.jmars.layer.stamp.sources.MaxStampSource;
import edu.asu.jmars.layer.stamp.sources.MedianStampSource;
import edu.asu.jmars.layer.stamp.sources.MinStampSource;
import edu.asu.jmars.layer.stamp.sources.NumericStampSource;
import edu.asu.jmars.layer.stamp.sources.StampSource;
import edu.asu.jmars.layer.stamp.sources.StdDevStampSource;

/**
 * Provides a list of stamp sources from each stamp layers, so the user may get
 * at the list of stamp layers in the advanced dialog and other places.
 */
public final class StampServer implements MapServer {
	private static final StampServer instance = new StampServer();
	private static final URI stampServerURI = URI.create("jmars://stamplayers");

	private static HashMap<Class, HashMap<Long, StampSource>> type2Instances = new HashMap<Class, HashMap<Long, StampSource>>();

	private static Class sourceTypes[] = { 
			AverageStampSource.class, 
			CountStampSource.class,
			MaxStampSource.class,
			MedianStampSource.class,
			MinStampSource.class,
			NumericStampSource.class,
			StampSource.class,
			StdDevStampSource.class 
	};

	static {
		initializeStampSources();
	}
	
	public static StampServer getInstance() {
		return instance;
	}
		
	public static void initializeStampSources() {
		// Clear any inProcess work from each MapSource
		for (MapSource source : instance.sources) {
			StampSource stampSource = (StampSource) source;
			if (stampSource.inProcessTiles!=null) {
				stampSource.inProcessTiles.clear();
			}
		}

		// Clear out all of the MapSources
		for (HashMap<Long, StampSource> instances : type2Instances.values()) {
			instances.clear();
		}
		
		// Clear the mapping to the MapSources
		type2Instances.clear();
		
		instance.sources.clear();
		
		// Recreate the mapping to the MapSources
		for (Class<StampSource> type : sourceTypes) {		
			type2Instances.put(type, new HashMap<Long, StampSource>());
		}
				
		MapServerFactory.addNewServer(instance);
	}
	
	private final List<MapSource> sources = new ArrayList<MapSource>();
	private transient Set<MapServerListener> listeners = Collections.synchronizedSet(new HashSet<MapServerListener>());
	
	public void add(MapSource source) {
		if (!sources.contains(source)) {
			
			StampSource ssource = (StampSource) source;
			
			HashMap<Long, StampSource> instances = type2Instances.get(ssource.getClass());
			
			Long key = ssource.settings.id;
				
			if (!instances.containsKey(key)) {
				instances.put(key, ssource);
			} 

			sources.add(source);
			dispatch(source, MapServerListener.Type.ADDED);
		}
	}
	
	public void createSources(StampLayerSettings settings) {
		for (Class<StampSource> type : sourceTypes) {
			HashMap<Long, StampSource> instances = type2Instances.get(type);
			
			Long key = settings.id;
			
			if (!instances.containsKey(key)) {
				StampSource newSource;
				try {
					newSource = type.getConstructor(StampLayerSettings.class).newInstance(settings);
				} catch (Exception e) {
					e.printStackTrace();
					continue;
				}
				instances.put(key, newSource);
				add(newSource);
			} 
		}

	}
	
	public void remove(String name) {
		MapSource source = getSourceByName(name);
		if (sources.remove(source)) {
			dispatch(source, MapServerListener.Type.REMOVED);
		}
	}
	
	public void remove(MapSource source) {
		if (sources.remove(source)) {
			dispatch(source, MapServerListener.Type.REMOVED);
		}
		remove(source.getName());
	}
	
	public void remove(StampLayerSettings settings) {
		// Loop through our type2instances map, look for any sources associated with this settings object,
		// and then remove matches from both the map and our list of sources
		for (HashMap<Long, StampSource> instances : type2Instances.values()) {
			StampSource source = instances.get(settings.id);
			if (source!=null) {
				instances.remove(settings.id);
				if (sources.remove(source)) {
					dispatch(source, MapServerListener.Type.REMOVED);
				}
			} 
		}
	}
	
	public void clearCache(StampLayerSettings settings) {
		// Loop through our type2instances map, look for any sources associated with this settings object,
		// and then remove matches from both the map and our list of sources
		for (HashMap<Long, StampSource> instances : type2Instances.values()) {
			StampSource source = instances.get(settings.id);
			if (source!=null) {
				source.clearCache();
			}
		}		
	}
		
	public void addListener(MapServerListener l) {
		listeners.add(l);
	}
	
	public void removeListener(MapServerListener l) {
		listeners.remove(l);
	}
	
	private void dispatch(MapSource source, MapServerListener.Type type) {
		for (MapServerListener l: listeners) {
			l.mapChanged(source, type);			
		}
	}
	
	public void delete() {
		throw new UnsupportedOperationException("May not remove this map server");
	}

	public List<MapSource> getMapSources() {
		return sources;
	}

	public StampSource getSource(Class type, Long settingsId) {
		HashMap<Long, StampSource> sources = type2Instances.get(type);
		if (sources!=null) {
			return sources.get(settingsId);
		}
		return null;
	}
	
	public URI getMapURI() {
		return stampServerURI;
	}

	public int getMaxRequests() {
		return 2;
	}

	public String getName() {
		return "Stamp Layers";
	}

	public MapSource getSourceByName(String name) {
		for (MapSource s: sources) {
			if (s.getName().equals(name)) {
				return s;
			}
		}
		return null;
	}

	public int getTimeout() {
		// This timeout is necessary to prevent the map layer from killing (and restarting) requests that take longer than 0 ms
		return 60000;
	}

	public String getTitle() {
		return getName();
	}

	public URI getURI() {
		return stampServerURI;
	}

	public boolean isUserDefined() {
		return false;
	}

	public void load(String serverName) {
		throw new UnsupportedOperationException("May not load this map server");
	}

	public void loadCapabilities(boolean cached) {
	}

	public void save() {
		throw new UnsupportedOperationException("May not save this map server");
	}
	
	public String toString(){
		return getTitle();
	}
}

