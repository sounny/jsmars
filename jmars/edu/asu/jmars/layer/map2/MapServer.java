package edu.asu.jmars.layer.map2;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

import edu.asu.jmars.util.Config;

/**
 * MapServer stores properties for a WMS Map Server.
 * 
 * Map servers are serialized by the {@link edu.asu.jmars.util.Config}
 * system. The following properties are saved to jmars.config, with
 * a key of <code>map.map_server.name.prop</code> where <code>name</code>
 * is the 'name' property below, and <code>prop</code> is any of the
 * following properties (except 'name'):
 * 
 * <ul>
 * <li>name: The unique identifier of this map server
 * <li>title: The human-displayable title of the map server
 * <li>url: The location of the server
 * <li>custom: If true, this is a user-defined server in
 * ~/jmars/jmars.config that can be removed.
 * <li>maxRequests: The number of simultaneous requests this server
 * supports.
 * <li>timeout: The number of milliseconds to wait before considering
 * a connection request to have timed out.
 * </ul>
 * @see edu.asu.jmars.util.Config
 * 
 * The {@link MapSource}s in a map server are immutable, for many reasons,
 * so the {@link #add(MapSource)} method sets properties that must have
 * values specific to that MapServer implementation, but that cannot be
 * determined when the MapSource is first created.
 */
public interface MapServer extends Serializable {
	
	/** Prefix for all MapServer attributes stored in jmars.config */
	public static final String prefix = "map.map_server";
	
	/**
	 * Returns a unique (canonic) name for this server.
	 * 
	 * This will be a compressed form of the server URL for custom servers.
	 * 
	 * This value must NOT be null.
	 */
	public String getName();
	
	/**
	 * Flag indicates whether this MapServer is custom or not. When its true, it
	 * means the server was added by the user, can be removed by the user, and
	 * is saved in a writable jmars.config.
	 */
	public boolean isUserDefined();
	
	/** Timeout in milliseconds */
	public int getTimeout();
	
	/** Maximum number of requests to make to this host at the same time */
	public int getMaxRequests();
	
	/** Get the Capabilities service URI */
	public URI getURI();
	
	/** Get the GetMap service URL. */
	public URI getMapURI();
	
	/** Get the server's displayable title */
	public String getTitle();
	
	/**
	 * Returns list of MapSource objects. This method caches map sources to avoid
	 * duplicating the expensive download and XML parsing operations.
	 * @param cached If false, or this method is being called for the first time,
	 * the capabilities document will be redownloaded and parsed.
	 */
	public List<MapSource> getMapSources();
	
	/** Returns the MapSource with the given name, or null if not found */
	public MapSource getSourceByName(String name);
	
	public void addListener(MapServerListener l);
	
	public void removeListener(MapServerListener l);
	
	/**
	 * Loads capabilities for this server.
	 * 
	 * <ol>
	 * <li>If capabilities have already been loaded and cached data is requested, this method simply returns.
	 * <li>If capabilities haven't been loaded and cached data is requested, capabilities are loaded from disk.
	 * <li>If cached data is not requested, capabilities are loaded from the map server.
	 * </ol>
	 * 
	 * @throws IllegalStateException Thrown when this method cannot update
	 * the capabilities, and cannot leave the capabilities in a consistent
	 * state (such as when loading capabilities for the first time.)
	 */
	public void loadCapabilities(boolean cached);
	
	/**
	 * Adds a new source from the given source, creating a new
	 * object to make sure the server is set to 'this'.
	 */
	public void add(MapSource source);
	
	/** Remove a MapSource by name */
	public void remove(String name);
	
	// MapServer serialization
	
	/** Loads this MapServer's jmars.config properties */
	public void load(String serverName);
	
	/** Saves this MapServer's jmars.config properties */
	public void save();
	
	/** Removes this MapServer's jmars.config properties */
	public void delete();
}

