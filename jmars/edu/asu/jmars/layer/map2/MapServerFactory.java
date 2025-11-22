package edu.asu.jmars.layer.map2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import javax.swing.SwingUtilities;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

/** MapServer cache and retrieval. This data is shared amongst all map2 layers. */
public class MapServerFactory {
	private static DebugLog log = DebugLog.instance();
	
	private static Thread loaderThread;
	private static Object lock = new Object();
	private static List<Runnable> whenReady = new LinkedList<Runnable>();
	
	/** Cached MapServer objects */
	private static List<MapServer> mapServers;
	
	private static List<MapServer> _mapServersInternal = new ArrayList<MapServer>();
	
	/** Held reference to the custom map server, or null if there isn't one */
	private static CustomMapServer customServer;
	
	/**
	 * Returns an unmodifiable list of MapServer objects, or null if the map
	 * servers haven't been fully loaded yet.
	 */
	public synchronized static List<MapServer> getMapServers() {
		kickLoader();
		return mapServers;
	}
	
	/**
	 * Initialize the map servers
	 */
	public synchronized static void initializeMapServers() {
		kickLoader();
	}
	
	/**
	 * Calls the given Runnable on the AWT event thread as soon as the list of
	 * MapServer objects has been fully loaded (possibly before returning from
	 * this method.) Any code that cannot assume the map servers have already
	 * been fully loaded should use this method, and call
	 * {@link #getMapServers()} inside the given Runnable.
	 */
	public synchronized static void whenMapServersReady(Runnable r) {
		kickLoader();
		synchronized(lock) {
			if (mapServers != null)
				if (SwingUtilities.isEventDispatchThread())
					r.run();
				else
					SwingUtilities.invokeLater(r);
			else
				whenReady.add(r);
		}
	}
	
	private synchronized static void kickLoader() {
		if (mapServers == null && loaderThread == null) {
			loaderThread = new Thread(new Runnable() {
				public void run() {
					_mapServersInternal.addAll(_getMapServersImpl());
					mapServers = Collections.unmodifiableList(_mapServersInternal);
					synchronized(lock) {
						for (Runnable r: whenReady) {
							SwingUtilities.invokeLater(r);
						}
						whenReady.clear();
					}
					// allow the GC to reap the loader thread
					loaderThread = null;
				}
			});
			loaderThread.setName("MapServerFactory loader");
			loaderThread.setPriority(Thread.MIN_PRIORITY);
			loaderThread.start();
		}
	}
	
	public synchronized static void addNewServer(MapServer server) {
		_mapServersInternal.add(server);
	}
	
	public synchronized static void removeMapServer(MapServer server) {
		_mapServersInternal.remove(server);
	}
	
	/**
	* @since change bodies
	*/
	public static void disposeOfMapServerReferences() {
		mapServers = null;
		loaderThread = null;
		customServer = null;
	}
	/**
	 * <p>
	 * Finds each named server in the config file, and loads its capabilities
	 * document. Since each server could take awhile to load each document, this
	 * method splits the loading out to a separate thread per server.
	 * 
	 * <p>
	 * The caller is responsible for synchronizing on method invocation.
	 */
	private static List<MapServer> _getMapServersImpl() {
		log.println("Loading map sources");
		final List<MapServer> servers = new ArrayList<MapServer>();
		String serverLines[] = Config.getChildKeys(Util.getProductBodyPrefix() + MapServer.prefix);// @since change bodies - added prefix
		final CountDownLatch latch = new CountDownLatch(serverLines.length);
		for(final String name: serverLines) {
			new Thread(new Runnable() {
				public void run() {
					try {
						MapServer newServer = null;
						if (name.equals(CustomMapServer.customMapServerName)) {
							if (Main.USER != null && Main.USER.length() > 0 && Main.PASS.length() > 0) {
								// create the custom server based on JMARS user/password
								// and rebuild "Add New Layer" menus when it changes
								newServer = customServer = new CustomMapServer(name, Main.USER, Main.PASS,
										Main.AUTH_DOMAIN, Main.PRODUCT, Main.getBody());// @since change bodies - added getBody()
							}
						} else {
							// TODO: This code will change when we have multiple types of
							// MapServers. In that case MapServers will be created via 
							// the MapServerFactory with some abstracted parameters.
							newServer = new WMSMapServer(name);
						}
						if (newServer != null) {
							newServer.loadCapabilities(false);
							newServer.addListener(new MapServerListener() {
								public void mapChanged(MapSource source, Type changeType) {
									// rebuild menus
									LManager.getLManager().refreshAddMenu();
								}
							});
							synchronized(servers) {
								servers.add(newServer);
							}
						}
					} catch(Exception ex) {
						log.aprintln("Error parsing map server named " + name + ":");
						log.aprintln(ex.getMessage());
						log.println(ex);
					} finally {
						latch.countDown();
					}
				}
			}).start();
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			log.println("Shouldn't happen, but the countdown latch was interrupted");
		}
		return servers;
	}
	
	/**
	 * Returns the CustomMapServer element from the map server list, or null if
	 * a custom server is not found.
	 */
	public synchronized static CustomMapServer getCustomMapServer() {
		if (customServer == null)
			getMapServers();
		return customServer;
	}
	
	/**
	 * Returns the default map in the default map source, or null if not found.
	 * 
	 * @see MapSource.DEFAULT_NAME
	 */
	public static MapSource getDefaultMapSource() {
		MapServer server = MapServerFactory.getServerByName(MapServerDefault.DEFAULT_NAME);
		if (server == null)
			return null;
		return server.getSourceByName(MapSourceDefault.DEFAULT_NAME);
	}

	/**
	 * Returns the default map in the default map server, or null if not found.
	 * 
	 * @see MapSource.DEFAULT_PLOT
	 */
	public static MapSource getDefaultPlotSource() {
		MapServer server = MapServerFactory.getServerByName(MapServerDefault.DEFAULT_NAME);
		if (server == null)
			return null;
		return server.getSourceByName(MapSourceDefault.DEFAULT_PLOT);
	}

	
	/**
	 * Returns the MapServer with the given URL, or null if it cannot be found.
	 */
	public synchronized static MapServer getServerByName(String name) {
	    //bug 2203
	    int count = 0;
	    List<MapServer> servers = getMapServers();
	    while (servers == null && count < 3) {
	        try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
	        servers = getMapServers();
	        count++;
	    }
	    if (servers != null) {
    		for (MapServer server: servers) {
    			if (server.getName().equals(name)) {
    				return server;
    			}
    		}
	    }
		return null;
	}
}
