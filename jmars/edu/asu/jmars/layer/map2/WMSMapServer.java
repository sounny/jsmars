package edu.asu.jmars.layer.map2;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
// import java.net.HttpURLConnection;   // TODO (PW) this can go.
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

// TODO These refer to obsolete http libraries AND the code that needs
// them should be refactored to only perform the credentials operations
// using JmarsHttpRequest. 
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.dom4j.DocumentException;
import edu.asu.jmars.Main;
import edu.asu.jmars.swing.UrlLabel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.HttpRequestType;

/**
 * WMSMapServer stores properties for a WMS Map Server.
 * 
 * Note that when updating capabilities, all added/removed sources send a
 * separate event. This can be very expensive if listeners are registered, so if
 * large changes are anticipated it will be much more efficient to create a new
 * WMSMapServer instance.
 * 
 * Internally, this class is driven by a capabilities document read from the
 * WMS server the instance is pointed at.  This document can take a long time
 * to read and parse, so fetching is deferred until the GetMap URI or list of
 * sources is truly needed.
 */
public class WMSMapServer extends MapServerDefault implements MapServer, Serializable {
	private static DebugLog log = DebugLog.instance();
	private static final long serialVersionUID = -691994868567732767L;	
	private static final String filePrefix = "wms_";
	private static final String cacheDir = Main.getJMarsPath();
	
	/**
	 * Get the prefix for map server properties, but append the current product body prefix
	 * @since change bodies
	 * @return
	 */
	public static String getPrefix() {
		return Util.getProductBodyPrefix() + MapServer.prefix;
	}
	/** Gets the jmars.config key that stores this attribute for this MapServer */
	private static String getAttrKey(String name, String attr) {
		return getPrefix() + "." + name + "." + attr; // @since change bodies - added product.body prefix
	}
	
	// Object properties, constructors, and accessors
	
	private String name;
	private boolean userDefined;
	private int timeout;
	private int maxRequests;
	private URI uri;
	private String title;
	
	private transient StringBuffer user;
	private transient StringBuffer pass;
	
	/** Transient since GetMap service URL can be recovered from the capabilities document */
	private transient WMSCapabilities capabilities;
	/** Listeners who care about changes in this MapServer */
	private transient List<MapServerListener> listeners;
	/** Cached map sources for this server */
	private transient List<MapSource> sources;
	/** True if the capabilities completely fail to load, such that it should not be tried again */
	private transient boolean capsFailure;
	
	/** Create a new MapServer object, loading capabilities immediately */
	public WMSMapServer(String newURL, int newTimeout, int newMaxRequests) {
		initTransients();
		name = newURL.replaceAll("[^0-9a-zA-Z]", "_").replaceAll("_+", "_");
		try {
			uri = new URI(newURL);
		} catch (URISyntaxException e) {
			log.println("Failed to create new server from URL " + newURL);
			log.println(e);
			throw new IllegalArgumentException("Invalid URL: " + e.getMessage(), e);
		}
		timeout = newTimeout;
		maxRequests = newMaxRequests;
		userDefined = true;
	}
	
	public WMSMapServer() {
    }
	
	/**
	 * Load a MapServer from jmars.config. The capabilities are loaded from disk.
	 * @param savedName The canonic name of the server. This must contain no spaces
	 * and be unique within this jmars.config.
	 */
	public WMSMapServer(String serverName) {
		initTransients();
		load(serverName);
	}
	
	/**
	 * Loads a MapServer from jmars.config, suffixing the URL with the given string.
	 * The capabilities are loaded from disk if available.
	 */
	public WMSMapServer(String serverName, String urlSuffix) {
		initTransients();
		load(serverName);
		uri = getSuffixedURI(uri, urlSuffix);
	}
	
	/**
	 * Returns a unique (canonic) name for this server.
	 * 
	 * This will be a compressed form of the server URL for custom servers.
	 * 
	 * This value must NOT be null.
	 */
	public final String getName() {
		return name;
	}
	
	/**
	 * Flag indicates whether this MapServer is custom or not. When its true, it
	 * means the server was added by the user, can be removed by the user, and
	 * is saved in a writable jmars.config.
	 */
	public final boolean isUserDefined() {
		return userDefined;
	}
	
	/** Timeout in milliseconds */
	public final int getTimeout() {
		return timeout;
	}
	
	/** Maximum number of times downloads from this server should be retried */
	public final int getMaxRequests() {
		return maxRequests;
	}
	
	/** Get the Capabilities service URI */
	public final URI getURI() {
		return uri;
	}
	
	/**
	 * Returns a new URI based on the given base but with the given args added
	 * onto the end of the query portion of the URI
	 */
	public static final URI getSuffixedURI(URI baseURI, String ... args) {
		try {
			String query = baseURI.getQuery();
			if (query == null)
				query = "";
			if (query.length() > 0 && !query.endsWith("&"))
				query += "&";
			query += Util.join("&", args);
			query += "&"+CustomMapServer.getServiceVersion()+"="+CustomMapServer.getVersionNumber();
			return new URI(baseURI.getScheme(), baseURI.getUserInfo(), baseURI.getHost(),
				baseURI.getPort(), baseURI.getPath(), query, baseURI.getFragment());
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Could not construct modified URI: " + e.getMessage(), e);
		}
	}
	
	/** Get the GetMap service URI */
	public final URI getMapURI() {
		if (capsFailure) {
			return URI.create("http://ms.mars.asu.edu");
		}
		if (capabilities == null) {
			loadCapabilities(false);
		}
		return capabilities.getMapURI();
	}
	
	public final String getTitle() {
		return title;
	}
	
	public String toString() {
		return title;
	}
	
	/**
	 * Returns list of MapSource objects, first retrieving the GetCapabilities
	 * response if not already done. The result will be cached for faster
	 * results next time. The {@link #loadCapabilities(boolean)} method can be
	 * called before this method if cached capabilities should be used.
	 */
	public List<MapSource> getMapSources() {
		if (capabilities == null) {
			loadCapabilities(false);
		}
		return Collections.unmodifiableList(sources);
	}
	
	/** Defines equality as identity - other attributes than URL can vary */
	public boolean equals(Object o){
		return o instanceof MapServer && ((MapServer)o).getURI().toString().equals(getURI().toString());
	}
	
	/** Defines equality as identity - other attributes than URL can vary */
	public int hashCode(){
		return getURI().hashCode();
	}
	
	/** Returns the MapSource with the given name, or null if not found */
	public MapSource getSourceByName(String name) {
		for (MapSource source: getMapSources()) {
			if (source.getName().equals(name)) {
				return source;
			}
		}
		return null;
	}
	
	public final void addListener(MapServerListener l) {
		listeners.add(l);
	}
	
	public final void removeListener(MapServerListener l) {
		listeners.remove(l);
	}
	
	/**
	 * Notifies all listeners of a change in the available maps.
	 * For now this is hard-coded to those parts of the user interface that need to know.
	 */
	private final void notifyListeners(MapSource source, MapServerListener.Type changeType) {
		for (MapServerListener l: listeners) {
			l.mapChanged(source, changeType);
		}
	}
	
	/** Provides the basename of the cached capabilities XML file */
	protected String getCacheName() {
		return "wms_" + getURI().toString().replaceAll("[^0-9a-zA-Z]+", "_") + ".xml";
	}
	
	public final URI getCapabilitiesURI() {
		return getSuffixedURI(uri, "service=wms","wmsver=1.1.1","request=GetCapabilities");
	}
	
//	/** returns an unconnected method for downloading the GetCapabilities response from this server */
	protected JmarsHttpRequest getCapsMethod() {
		return new JmarsHttpRequest(getCapabilitiesURI().toString(), HttpRequestType.GET);
	}
	
	/**
	 * Loads capabilities for this server. Actually does one of several things depending on the internal
	 * state of this server and the value of 'cached':
	 * 
	 * <ol>
	 * <li>If capabilities have already been loaded and cached data is requested, this method simply returns.
	 * <li>If capabilities haven't been loaded and cached data is requested, capabilities are loaded from disk.
	 * <li>If cached data is not requested, capabilities are loaded from the map server.
	 * </ol>
	 * 
	 * If something goes wrong and cannot be recovered, the state of the map source list will be restored
	 * to the prior state if possible, to an empty list of map sources if not.
	 */
	public final synchronized void loadCapabilities(boolean cached) {
		if ((capabilities != null && cached) || capsFailure) {
			return;
		}
		
		// get backup of layers
		List<WMSLayer> oldLayers;
		if (capabilities != null) {
			oldLayers = new LinkedList<WMSLayer>(capabilities.getLayers());
		} else {
			oldLayers = Collections.emptyList();
		}
		
		refactoredMethod(cached, oldLayers);
	}
	
	private int getHttpDefaultCode(JmarsHttpRequest method, boolean diskCache, final File file, HttpClient client) 
			throws URIException, HttpException, IOException, URISyntaxException {
		client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
		if (diskCache && file.exists() && file.lastModified() != 0)
			method.setRequestHeader("If-Modified-Since", DateUtil.formatDate(new Date(file.lastModified())));
		
		method.send();
		
		return method.getStatus();
	}

	private void refactoredMethod(boolean cached, List<WMSLayer> oldLayers) {
		WMSCapabilities newCaps = null;
		try {
			final String fileName = Main.getJMarsPath() + getCacheName();
			JmarsHttpRequest method = getCapsMethod();
			String networkPath = getCapabilitiesURI().toString();
			
			// determine if we're using disk caching
			// normally the config setting is unset, so we cache if we're running out of a jar
			boolean diskCache = Config.get("map.capabilities.cache", Main.IN_JAR);
			
			// determine where we'll load from this time
			final File file = new File(fileName);
			if (JmarsHttpRequest.getConnectionFailed() && file.exists()) {
				try {
					newCaps = new WMSCapabilities(new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)));
				} catch (Exception e) {
					throw new IllegalStateException("Unable to get capabilities for server named " +
						getName() + " from disk; check network connection and try again.", e);
				}
			} else {
				boolean diskLoad = diskCache && cached && file.exists();
				log.println("Loading capabilities from " + (diskLoad ? file.getAbsolutePath() : networkPath));
				
				// try the network if we're not getting from disk
				if (!diskLoad) {
					try {
						HttpClient client = new HttpClient();
						int code = getHttpDefaultCode(method, diskCache, file, client);
						
						switch (code) {
						case HttpStatus.SC_OK: //HttpURLConnection.HTTP_OK:  TODO remove these comments later
							newCaps = getHttpOk(fileName, method, diskCache, file);
							log.println("Capabilities updated from network");
							break;
						case HttpStatus.SC_NOT_MODIFIED: // HttpURLConnection.HTTP_NOT_MODIFIED:
							log.println("Capabilities are already up to date");
							break;
						case HttpStatus.SC_UNAUTHORIZED: // HttpURLConnection.HTTP_UNAUTHORIZED:
							log.aprintln("Capabilities update failed with response code: " + code +
									", trying implicit user supplied parameters");
							Credentials defaultcreds = new UsernamePasswordCredentials(user.toString(), pass.toString());
							client.getState().setCredentials(AuthScope.ANY, defaultcreds);
							code = getHttpDefaultCode(method, diskCache, file, client);
							switch (code) {
							case HttpStatus.SC_OK: // HttpURLConnection.HTTP_OK:
								newCaps = getHttpOk(fileName, method, diskCache, file);
								log.println("Capabilities updated from network");
								break;
							default:
								log.aprintln("Capabilities update failed with response code: " + code +
								", trying explicit supplied parameters");
								promptUserPass();
								defaultcreds = new UsernamePasswordCredentials(user.toString(), pass.toString());
								client.getState().setCredentials(AuthScope.ANY, defaultcreds);
								code = getHttpDefaultCode(method, diskCache, file, client);
								switch (code) {
								case HttpStatus.SC_OK: // HttpURLConnection.HTTP_OK:
									newCaps = getHttpOk(fileName, method, diskCache, file);
									log.println("Capabilities updated from network");
									break; 
								default:
									log.aprintln("Capabilities update failed with response code: " + code);
									break;
								}
								break;
							}
							break;
						default:
							log.aprintln("Capabilities update failed with response code: " + code);
							break;
						}
					} catch (Exception e) {
						log.aprintln(MessageFormat.format("Unable to load capabilities from \"{0}\" for server \"{1}\", due to:",
								networkPath, getName()));
						e.printStackTrace();
					}
				}
				
				// try the disk if we didn't load capabilities from network
				if (newCaps == null) {
					try {
						newCaps = new WMSCapabilities(new BufferedReader(new FileReader(file)));
					} catch (Exception e) {
						throw new IllegalStateException("Unable to get capabilities for server named " +
							getName() + " from network or disk; check network connection and try again.", e);
					}
				}
			}
			
			// integrate caps with this server
			processSources(oldLayers, newCaps.getLayers(), this);
			
			capabilities = newCaps;
		} catch (Exception e) {
			// log failure and attempt to revert to prior good state
			log.aprintln("Exception getting capabilities from server named " + getName() + ":");
			log.aprintln(e);
			if (capabilities != null) {
				log.aprintln("Attempting to restore previous state");
				try {
					processSources(capabilities.getLayers(), oldLayers, this);
				} catch (Exception e2) {
					log.aprintln("Unable to restore state after failed load, server has no map sources:");
					log.aprintln(e2);
					sources.clear();
				}
			} else {
				log.aprintln("loadCapabilities failed, there are NO capabilities for this server");
				sources.clear();
				capsFailure = true;
			}
		}
	}
	
	public void promptUserPass()
	 {
		class MyLabel extends JLabel
		 {
			MyLabel(String s)
			 {
				super(s);
				setAlignmentX(1);
				setAlignmentY(0.5f);
			 }
		 }

		class MyBox extends Box
		 {
			MyBox(JComponent a, JComponent b)
			 {
				super(BoxLayout.Y_AXIS);
				add(a);
				add(b);
			 }
		 }

		final JTextField txtUser = new JTextField();
		final JPasswordField txtPass = new JPasswordField();

		txtUser.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				// Do not let the Dialog OK button steal the default focus from us
	            if (e.getOppositeComponent() instanceof JButton) {
	            	txtUser.grabFocus();
	            	txtUser.removeFocusListener(this);
	            }
			}
		
			public void focusGained(FocusEvent e) {
				// nop
			}		
		});

		txtPass.addFocusListener(new FocusListener() {
			public void focusLost(FocusEvent e) {
				// Do not let the Dialog OK button steal the default focus from us
	            if (e.getOppositeComponent() instanceof JButton) {
	            	txtPass.grabFocus();
	            	txtPass.removeFocusListener(this);
	            }
			}
		
			public void focusGained(FocusEvent e) {
				// nop
			}		
		});
		
		Box fields = new Box(BoxLayout.X_AXIS);
		fields.add(new MyBox(new MyLabel(Config.get("login.lbl","Username:") + " "), new MyLabel("Password: ")), BorderLayout.WEST);
		fields.add(new MyBox(txtUser, txtPass), BorderLayout.CENTER);
		
		String jmarsWelcomeMsg = "Current authentication credentials are not valid\n and are being requested by one or more layers.\n Please re-enter credentials.\n";
		String hideRegLink = Config.get("hideRegPageLink");
		Object[] loginDialogContents;
		if(hideRegLink != null && !hideRegLink.equalsIgnoreCase("false")) {
			loginDialogContents = new Object[] {
				jmarsWelcomeMsg, "\n", fields, "",
				"\nJMARS homepage: ",new UrlLabel(Config.get("homepage"), null), "\n"};
		} else {
			loginDialogContents = new Object[] {
				jmarsWelcomeMsg, "\n", fields, "",
				"\nTo register, please visit:\n",
				new UrlLabel(Config.get("registrationpage"), null),
				"\nJMARS homepage: ",new UrlLabel(Config.get("homepage"), null), "\n"};		
		}
		
		JOptionPane op =
			new JOptionPane(
				loginDialogContents,
				JOptionPane.WARNING_MESSAGE,
				JOptionPane.OK_CANCEL_OPTION
				);

		JDialog dialog = op.createDialog(
			null, Config.get("edition", "JMARS") + " AUTHENTICATION");
       dialog.addWindowListener(
			new WindowAdapter()
			 {
				public void windowActivated(WindowEvent we)
				 {
					if(txtUser.getText().equals(""))
						txtUser.grabFocus();
					else
						txtPass.grabFocus();
               }
			 }
			);
       
		dialog.setResizable(false);
		dialog.setVisible(true);
		if (JOptionPane.OK_OPTION != ((Integer)op.getValue())) {
			log.println("User exited when challenged for password when getting capabilities.");
			Util.showMessageDialog("Some layers may not load if they require authentication.");
		} else {
			user = new StringBuffer(txtUser.getText());
			pass = new StringBuffer(String.copyValueOf(txtPass.getPassword()));
			if ((user != null && user.toString().trim().length() == 0) || (pass != null && pass.toString().trim().length() == 0)) {
				Util.showMessageDialog("You have not entered a username or password. \nSome layers may not load if they require authentication.");
			}
		}
	 }
	
	private WMSCapabilities getHttpOk(final String fileName, JmarsHttpRequest request, boolean diskCache, final File file) 
			throws IOException,	DocumentException, URISyntaxException {
		WMSCapabilities newCaps;
		String stringDoc = Util.readResponse(request.getResponseAsStream(), StandardCharsets.UTF_8);  // TODO (PW) use JmarsHttpRequest.getResponseAsStream() DONE
		if (stringDoc.startsWith("ERROR:")) {
			String message = stringDoc.replaceAll("^ERROR: *", "");
			throw new IllegalStateException("Server returned error: " + message);
		}
		newCaps = new WMSCapabilities(new StringReader(stringDoc));
		if (diskCache) {
			final long lastModified;
			long time = 0;
			try {
//				Header lastModifiedHeader = method.getResponseHeader("last-modified");
                Header lastModifiedHeader = request.getLastModifiedHeader();  // TODO (PW) Ah!  need a new method!!!  DONE

				if (lastModifiedHeader != null) {
					String lastModifiedString = lastModifiedHeader.getValue();
					SimpleDateFormat formatter = new SimpleDateFormat();
					time = formatter.parse(lastModifiedString).getTime();
				}
			} catch (Exception e) {
				log.println(e);
			}
			lastModified = time;
			
			// save on another thread, since it takes a second that we
			// don't want to slow down startup
			Thread t = new Thread(new Runnable() {
				public void run() {
					try {
						synchronized(WMSMapServer.this) {
							capabilities.save(file, lastModified);
						}
					} catch (Exception e) {
						log.aprintln("Failure saving capabilities for " +
							WMSMapServer.this.getName() + " to " + fileName + ": " + e.getMessage());
						log.println(e);
					}
				}
			});
			t.setName("capabilities-caching-thread");
			t.setPriority(Thread.MIN_PRIORITY);
			t.setDaemon(true);
			t.start();
		}
		request.close();
		return newCaps;
	}

//	private int getHttpDefaultCode(HttpMethod method, boolean diskCache, final File file, HttpClient client) 
//			throws URIException, HttpException, IOException {
//		client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
//		if (diskCache && file.exists() && file.lastModified() != 0)
//			method.setRequestHeader("If-Modified-Since", DateUtil.formatDate(new Date(file.lastModified())));
//		
//		if (method instanceof PostMethod) {
//			return Util.postWithRedirect(client, (PostMethod)method, 3);
//		} else {
//			return client.executeMethod(method);
//		}
//	}
	
	/**
	 * Adds a new source from the given source
	 */
	public void add(MapSource source) {
		if (source instanceof WMSMapSource) {
			sources.add(source);
			notifyListeners(source, MapServerListener.Type.ADDED);
		} else {
			throw new IllegalStateException("Unsupported source type " +
				(source==null ? "null" : source.getClass().getName()));
		}
	}
	
	public void add(WMSLayer layer) {
		log.println("Adding layer [" + layer + "]");
		add(new WMSMapSource(layer.getName(), layer.getTitle(), layer.getAbstract(), layer.getUnits(),
			layer.getCategories(), this, layer.isNumeric(), layer.hasElevation(), layer.hasGeologic(), layer.getLatLonBoundingBox(),
			layer.getIgnoreValue(), layer.getMaxPPD(), layer.getOwner()));
	}
	
	public void remove(String name) {
		log.println("Removing layer [name: " + name + "]");
		MapSource source = getSourceByName(name);
		if (source == null) {
			throw new IllegalStateException("Couldn't find source to remove with name " + name);
		}
		sources.remove(source);
		notifyListeners(source, MapServerListener.Type.REMOVED);
	}
	
	/** Merge differences between old and new layers onto this server */
	private static final void processSources(List<WMSLayer> oldLayers, List<WMSLayer> newLayers, WMSMapServer server) {
		// process deletes
		Set<WMSLayer> deleted = new LinkedHashSet<WMSLayer>(oldLayers);
		deleted.removeAll(newLayers);
		for (WMSLayer del: deleted) {
			server.remove(del.getName());
		}
		
		// process inserts
		Set<WMSLayer> added = new LinkedHashSet<WMSLayer>(newLayers);
		added.removeAll(oldLayers);
		for (WMSLayer add: added) {
			server.add(add);
		}
		
		// process changes
		for (WMSLayer left: oldLayers) {
			for (WMSLayer right: newLayers) {
				if (left.equals(right)) {
					if (! left.reallyEquals(right)) {
						server.remove(left.getName());
						server.add(right);
					}
					continue;
				}
			}
		}
	}
	
	// MapServer serialization
	
	/** Loads this MapServer's jmars.config properties */
	public void load(String serverName) {
		name = serverName;
		title = Config.get(getAttrKey(name, "title"), null);
		try {
			uri = new URI(Config.getReplaced(getAttrKey(name, "url"), ""));// @since change bodies - added prefix
		} catch (URISyntaxException e) {
			log.println("Unable to load server named " + serverName + ": " + e.getMessage());
			throw new IllegalArgumentException("Server named " + serverName + " refers to invalid server", e);
		}
		userDefined = Config.get(getAttrKey(name, "custom"), false);
		maxRequests = Config.get(getAttrKey(name, "maxRequests"), 0);
		timeout = Config.get(getAttrKey(name, "timeout"), 0);
		if (maxRequests <= 0 || timeout <= 0) {
			throw new IllegalArgumentException("Mapserver not properly saved: " + serverName);
		}
	}
	
	/** Saves this MapServer's jmars.config properties */
	public void save() {
		if (! userDefined) {
			throw new IllegalStateException("Only custom MapServer objects can be saved");
		}
		Config.set(getAttrKey(name, "title"), getTitle());
		Config.set(getAttrKey(name, "url"), uri.toString());
		Config.set(getAttrKey(name, "custom"), true);
		Config.set(getAttrKey(name, "maxRequests"), ""+maxRequests);
		Config.set(getAttrKey(name, "timeout"), ""+timeout);
	}
	
	/** Removes this MapServer's jmars.config properties */
	public void delete() {
		if (! userDefined) {
			throw new IllegalStateException("Only custom MapServer objects can be deleted");
		}
		Config.set(getAttrKey(name, "title"), (String)null);
		Config.set(getAttrKey(name, "url"), (String)null);
		Config.set(getAttrKey(name, "custom"), (String)null);
		Config.set(getAttrKey(name, "maxRequests"), (String)null);
		Config.set(getAttrKey(name, "timeout"), (String)null);
		Config.set(getPrefix() + "." + name, (String)null);
	}
	
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
	}
	
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		initTransients();
		in.defaultReadObject();
	}
	
	private void initTransients() {
		String tempUser = "";
		if (Main.USER != null) {
			tempUser = Main.USER;
		}
		user = new StringBuffer(tempUser);
		pass = new StringBuffer(Main.PASS);
		listeners = new LinkedList<MapServerListener>();
		sources = new ArrayList<MapSource>();
		capsFailure = false;
	}
	
	
}

