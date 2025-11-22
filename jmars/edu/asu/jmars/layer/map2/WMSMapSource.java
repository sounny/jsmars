package edu.asu.jmars.layer.map2;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;


//import org.apache.commons.httpclient.Credentials;
//import org.apache.commons.httpclient.UsernamePasswordCredentials;
//import org.apache.commons.httpclient.auth.AuthScope;
//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.HttpConnectionManager;
//import org.apache.commons.httpclient.HttpException;
//import org.apache.commons.httpclient.HttpMethod;
//import org.apache.commons.httpclient.HttpMethodRetryHandler;
//import org.apache.commons.httpclient.methods.GetMethod;
//import org.apache.commons.httpclient.params.HttpClientParams;
//import org.apache.commons.httpclient.params.HttpMethodParams;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.swing.SerializableRectangle2D;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.VicarException;
import edu.asu.jmars.util.HttpRequestType;

/**
 * A WMSMapSource is a MapSource which is available via a WMS Server.
 */
public class WMSMapSource extends MapSourceDefault implements MapSource, Serializable {
	private static final long serialVersionUID = -9016302326239498727L;
	
	private static DebugLog log = DebugLog.instance();
	
	public static final String CONTENT_TYPE_VICAR = "image/vicar";
	public static final String CONTENT_TYPE_PNG = "image/png";
	public static final String CONTENT_TYPE_WMS_ERROR = "application/vnd.ogc.se_xml";
	public static final String RESP_HDR_CONTENT_TYPE = "Content-Type";
	
	// accessible properties
	private String name;          // This is the map name to use when requesting a map from the server
	private MapServer server;
	private String title;	      // This is the map title to display to a user
	private String abstractText;
	private String units;		  // This is the units for the map if it is numeric
	private String[][] categories;  // Position of this map source in map server's hierarchy
	private SerializableRectangle2D latLonBBox; // (lat,lon) bounding box of the MapSource's extent (optional)
	private transient volatile MapAttr attr = null;
	private transient List<MapSourceListener> listeners;
	
	// x and y offset, in degrees, representing how much the user has nudged the map
	// to deal with localized map variances.
	private boolean hasNumericKeyword = false;
	
	private boolean hasElevationKeyword = false;
	
	private boolean hasGeologicKeyword = false;
	
	private double[] ignoreValue;
	
	private double maxPPD;
	
	private String owner;
	
	// internal-only fields for resolving the MapAttr object
	private transient MapChannel channel = null;
	
	private double xOffset = 0;
	private double yOffset = 0;
	
	public Point2D getOffset() {
		return new Point2D.Double(xOffset,yOffset);
	}
	
	public void setOffset(Point2D offset) {
		this.xOffset = offset.getX();
		this.yOffset = offset.getY();
		changed();
	}
	
	public boolean isMovable() {
		// This should check a new keyword from the MapServer.
		// MOLA maps should NOT be moveable
		return !name.startsWith("MOLA");
	}
	
	/**
	 * Constructs a new MapSource from the given arguments.
	 * 
	 * If <code>dataType</code>
	 * is null, a new MapAttr will be created that will poll this new MapSource for
	 * a sample image from which to query the MapAttr values. Code interested in
	 * being notified when the state of this MapAttr is fully set should add a
	 * listener to {@link MapAttr#getChannel() MapAttr's MapChannel}.
	 */
	public WMSMapSource(String newName, String newTitle, String newAbstract, String units, String[][] categories,
			MapServer newServer, boolean hasNumericKeyword, boolean hasElevationKeyword, boolean hasGeologicKeyword, Rectangle2D latLonBBox, 
			double[] ignoreValue, double maxPPD, String owner) {
		this.name=newName;
		this.title=newTitle;
		this.abstractText=newAbstract;
		this.units=units;
		this.server=newServer;
		this.hasNumericKeyword = hasNumericKeyword;
		this.hasElevationKeyword = hasElevationKeyword;
		this.hasGeologicKeyword = hasGeologicKeyword;
		this.categories = categories;
		this.latLonBBox = safeCopy(latLonBBox);
		this.ignoreValue = ignoreValue;
		this.maxPPD = maxPPD;
		this.owner = owner;
	}
	
	public String[][] getCategories() {
		return categories;
	}
	
	/**
	 * Returns a canonic name for this MapSource within the scope of its MapServer.
	 * 
	 * This value must NOT be null.
	 */
	public String getName() {
		return name;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getAbstract(){
		return abstractText;
	}
	
	public String getUnits(){
		return units;
	}
	
	public MapServer getServer() {
		return server;
	}
	
	private SerializableRectangle2D safeCopy(Rectangle2D r){
		if (r == null)
			return null;
		return new SerializableRectangle2D(r);
	}
	
	public Rectangle2D getLatLonBoundingBox(){
		return safeCopy(latLonBBox);
	}
	
	/**
	 * Returns the MapAttr, or null if it has not been resolved yet. In that
	 * case, callers should call {@link getMapAttr(MapAttrReceiver)}
	 */
	public MapAttr getMapAttr() {
		return attr;
	}
	
	/**
	 * Returns the MapAttr asynchronously by passing it to the given callback
	 * some time later on the AWT thread.
	 */
	public void getMapAttr(final MapAttrReceiver receiver) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (attr != null) {
					receiver.receive(attr);
				} else {
					if (channel == null) {
						log.println("Creating deferred MapAttr");
						int ppd = 32;
						Rectangle2D extent = new Rectangle2D.Double(0,0,1d/ppd,1d/ppd);
						ProjObj previewProj = new ProjObj.Projection_OC(0,0);
						Pipeline[] pipes = new Pipeline[]{new Pipeline(WMSMapSource.this, new Stage[0])};
						channel = new MapChannel(extent, ppd, previewProj, pipes);
					}
					channel.addReceiver(new MapChannelReceiver() {
						public void mapChanged(final MapData mapData) {
							if (mapData.isFinished()) {
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										channel = null;
										attr = new MapAttr(mapData.getImage());
										log.println("Deferred MapAttr resolved succesfully");
										receiver.receive(attr);
									}
								});
							}
						}
					});
				}
			};
		});
	}
	
	private URI getRequestURI(MapRequest mapTileRequest){
		// get up vector and bbox, and shift bbox based on uplon
		Projection_OC poc = (Projection_OC)mapTileRequest.getProjection();
		Point2D up = Util.getJmars1Up(poc, null);
		Rectangle2D tileExtent = mapTileRequest.getExtent();
		double startX = tileExtent.getMinX();
		double startY = tileExtent.getMinY();
		double endX = tileExtent.getMaxX();
		double endY = tileExtent.getMaxY();
		startX = Util.worldXToJmars1X(poc, startX);
		endX = Util.worldXToJmars1X(poc, endX);
		int pixWidth = (int)Math.round(mapTileRequest.getExtent().getWidth() * mapTileRequest.getPPD());
		int pixHeight = (int)Math.round(mapTileRequest.getExtent().getHeight() * mapTileRequest.getPPD());
		return WMSMapServer.getSuffixedURI(
			getServer().getMapURI(),
			"SERVICE=WMS",
			"REQUEST=GetMap",
			"FORMAT="+getMimeType(),
			"SRS=JMARS:1," + up.getX() + "," + up.getY(),
			"STYLES=",
			"VERSION=1.1.1",
			"LAYERS=" + getName(),
			"WIDTH=" + pixWidth,
			"HEIGHT=" + pixHeight,
			"BBOX=" + startX + "," + startY + "," + endX + "," + endY);
	}
	
	public BufferedImage fetchTile(MapRequest mapTileRequest) throws RetryableException, NonRetryableException {
		String urlString = getRequestURI(mapTileRequest).toString();
		String mimeType = getMimeType();
		log.println("Downloading ["+mimeType+"] tile from URL " + urlString);
		
//		HttpClient client = new HttpClient();                                              // TODO (PW) Declare JmarsHttprequest instead, passing in credentials
        JmarsHttpRequest request = new JmarsHttpRequest(urlString, HttpRequestType.GET, Main.USER, Main.PASS);
//		Credentials defaultcreds = new UsernamePasswordCredentials(Main.USER, Main.PASS);
//		client.getState().setCredentials(AuthScope.ANY, defaultcreds);
//		HttpClientParams hcp = new  HttpClientParams();
//		hcp.setAuthenticationPreemptive(true);
//		client.setParams(hcp);
//		HttpConnectionManager conMan = client.getHttpConnectionManager();              // TODO (PW) use TBD method on HttpClientBulder for this in JmarsHttpRequest
//		conMan.getParams().setDefaultMaxConnectionsPerHost(getServer().getMaxRequests());
        request.setConnectionTimeout(getServer().getTimeout());
        request.setMaxConnections(getServer().getMaxRequests());
        request.setIsMapServerRequest(true);
//        conMan.getParams().setConnectionTimeout(getServer().getTimeout());             // TODO (PW) implement setConnectionTimeout() on JmarsHttpRequest
//	    GetMethod method = new GetMethod(urlString);
//		method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new HttpMethodRetryHandler() {
//			public boolean retryMethod(HttpMethod method, IOException exception, int executionCount) {
//				return false; 
//			}
//		});
        request.setRetryNever();
        
		BufferedImage tileImage = null;
	    
		try {
			hookWatch(Thread.currentThread(), request);
			
            request.send();
//			client.executeMethod(method);   // TODO (PW) call send()
            
            //certain types of Exceptions should be retryable and others are not. We need to get the Exception and throw it
			if (request.getGeneratedException() != null) {
				Exception e = request.getGeneratedException();
				log.aprintln("An Exception was caught and is being re-thrown in a map tile request: "+e.getMessage());
				throw e;
			}
			// check the resulting content type
			String contentType = checkForWMSException(request); // TODO (PW) need to pass JmarsHttpRequest object instead 
			
			if (CONTENT_TYPE_VICAR.equals(contentType)) {
				tileImage = MyVicarReaderWriter.read(request.getResponseAsStream());   // TODO (PW) call my getResponseAsStream()
			}
			else if (CONTENT_TYPE_PNG.equals(contentType)) {
				tileImage = ImageIO.read(request.getResponseAsStream());            // TODO (PW) call my getResponseAsStream()
				tileImage = Util.replaceWithLinearGrayCS(tileImage);
			} else {
				throw new NonRetryableException("Unrecognized MIME type " + contentType);
			}

			if (tileImage == null ||
					tileImage.getWidth() != MapRetriever.tiler.getPixelWidth() ||
					tileImage.getHeight() != MapRetriever.tiler.getPixelHeight()) {
				throw new RetryableException(tileImage==null?"Downloaded image was null": "Wrong size image downloaded");
			}
		}
		catch (MalformedURLException mue) {
			throw new NonRetryableException("Malformed URL " + urlString, mue);
		}
//		catch (HttpException he) {
//			throw new NonRetryableException("HttpException for URL: " + urlString, he);
//		}
		catch (SocketTimeoutException ste) {
			// We do NOT want to retry timeouts
			throw new NonRetryableException("Socket Timedout: " + ste.getMessage() + " for " + urlString, ste);
		}
		catch (IOException ioe){
			throw new RetryableException("I/O exception connecting to URL: " + urlString, ioe);
		}
		catch (VicarException ve) {
			throw new RetryableException("A VicarException occured", ve);
		}
		catch (WMSException wmse) {
			throw new NonRetryableException(wmse);
		}
		catch (NonRetryableException nre){
			throw nre;
		}
		catch (RetryableException re){
			throw re;
		}
		catch (Exception e) {
			throw new NonRetryableException("An unknown error occured", e);
		} 
		finally {
			request.close();   // TODO (PW) call my close()
			unhookWatch(Thread.currentThread());
		}
	    
	    return tileImage;
	}
	
	/** millisecond frequency of interrupt polling */
	private static final int INT_POLL_FREQ = 100;
	/** singleton timer to execute the timed poll check */
	private static Thread interruptThread;
	/** list of threads to poll for interrupted state and the method to abort if it is */
	private static final Map<Thread,JmarsHttpRequest> downloads = new IdentityHashMap<Thread,JmarsHttpRequest>();
	/** this task will keep rescheduling itself until there are no downloads to watch */
	private static final Runnable interruptTask = new Runnable() {
		public void run() {
			while (true) {
				// gather methods for interrupted threads
				List<JmarsHttpRequest> toAbort = new ArrayList<JmarsHttpRequest>(downloads.size());
				synchronized(downloads) {
					for (Thread t: downloads.keySet()) {
						if (t.isInterrupted()) {
							toAbort.add(downloads.get(t));
						}
					}
				}
				
				// abort outside the downloads lock since it could take time
				for (JmarsHttpRequest m: toAbort) {
					log.println("poll: aborting socket");
					m.abort();
				}
				
				// if we're out of downloads to monitor, get out
				synchronized(downloads) {
					if (downloads.isEmpty()) {
						interruptThread = null;
						break;
					}
				}
				
				// if we are interrupted, get out, otherwise wait before
				// checking interrupt flags again
				try {
					Thread.sleep(INT_POLL_FREQ);
				} catch (InterruptedException e) {
					interruptThread = null;
					break;
				}
			}
			log.println("poll: quitting");
		}
	};
	
	/**
	 * Schedules a periodic poll of the thread's interrupt state, and if it
	 * becomes interrupted, aborts the given method. All HttpClient3 requests
	 * should call this method before calling
	 * {@link HttpClient#executeMethod(HttpMethod)}.
	 * 
	 * The {@link #fetchTile(MapRequest)} request must close the socket when the
	 * downloading thread becomes interrupted, and while HttpClient 4 uses NIO
	 * for proper abort-on-interrupt behavior, it is still in beta and neither
	 * Java sockets nor HttpClient3 do it, so we have this work around for now.          TODO (PW) Correct this banner
	 */
	private static void hookWatch(final Thread thread, final JmarsHttpRequest request) {                     // TODO (PW) pass in JmarsHttpRequest instead
		synchronized(downloads) {
			downloads.put(thread,request);
			if (interruptThread == null) {
				interruptThread = new Thread(interruptTask);
				interruptThread.setPriority(Thread.MIN_PRIORITY);
				interruptThread.setName("WMSMapSource-interrupt-handler");
				interruptThread.setDaemon(true);
				log.println("poll: starting");
				interruptThread.start();
			}
		}
	}
	
	/**
	 * Stop periodically polling the state of the interrupt flag for the given
	 * thread. This method should always be called when leaving
	 * {@link #fetchTile(MapRequest)}.
	 */
	private static void unhookWatch(Thread thread) {
		synchronized(downloads) {
			downloads.remove(thread);
		}
	}
	
	/**
	 * Checks for WMS Exceptions throwing them as needed, returning the
	 * content type.
	 * @param method 
	 * @return Content type of the data.
	 */
	private String checkForWMSException(JmarsHttpRequest request) throws WMSException, IOException {   // TODO (PW) pass in JmarsHttpRequest object
//		String contentType = method.getResponseHeader(RESP_HDR_CONTENT_TYPE).getValue();   // TODO (PW) call my getContentType()
        String contentType = request.getContentType();
        
		if (contentType == null || contentType.length() == 0 || contentType.equalsIgnoreCase("ERROR")){
			throw new IOException("MapServer did not provide a content type");
		}
		else if (CONTENT_TYPE_WMS_ERROR.equalsIgnoreCase(contentType)) {
			// TODO: these errors have semantic meaning, handle retries appropriately
//			InputStream responseStream = method.getResponseBodyAsStream();  // TODO (PW) call my getResponseAsStream()
            InputStream responseStream = request.getResponseAsStream();  // TODO (PW) call my getResponseAsStream()

			String errorText = Util.readResponse(responseStream); // TODO (PW) Hmm... look into what this really does.
			String pattern1 = "ServiceException code=\"";
			String pattern2 = "\">";
			int start = errorText.indexOf(pattern1) + pattern1.length();
			int end = errorText.indexOf(pattern2, start);

			if (start > -1 && end > -1)
				throw new WMSException("A WMS Error occurred: " + errorText.substring(start, end));
			else
				throw new WMSException("An unknown WMS error occurred.");
		}
		
		return contentType;
	}
	
	public String toString(){
		return getTitle();
	}
	
	/**
	 * The numeric keyword will cause a map to be requested in vicar format,
	 * otherwise in png
	 * 
	 * TODO: this usage of 'numeric' is GROSS, wrong, and may possibly be used
	 * by other servers to mean something else! Get rid of it and deduce map
	 * request types using the original design (using the strongest format
	 * supported both by the server and client.)
	 */
	public boolean hasNumericKeyword() {
		return hasNumericKeyword;
	}
	
	public boolean hasElevationKeyword() {
		return hasElevationKeyword;
	}
	
	public boolean hasGeologicKeyword() {
		return hasGeologicKeyword;
	}
	
	public String getMimeType() {
		if (hasNumericKeyword()) {
			return "image/vicar";
		} else {
			return "image/png";
		}
	}
	
	/** Equal if servers are equal and objects have the same type and name property */
	public boolean equals(Object o) {
		if (o instanceof WMSMapSource) {
			MapSource s = (MapSource)o;
			return s.getServer().equals(getServer()) && s.getName().equals(getName());
		}
		return false;
	}
	
	/** Hash-code is based on the hash of the server and name properties */
	public int hashCode(){
		return (getServer().hashCode() * (1<<31)) + getName().hashCode();
	}
	
	public void setIgnoreValue(double[] newIgnoreValue){
		this.ignoreValue = newIgnoreValue;
		changed();
	}
	
	public void setTitle(String sourceTitle) {
	    this.title = sourceTitle;
	}
	public void setAbstractText(String abstractTxt) {
	    this.abstractText = abstractTxt;
	}
	public double[] getIgnoreValue() {
		return ignoreValue;
	}
	
	/**
	 * Sets the maxPPD to a new maxPPD value.  
	 * Will only reset the maxPPD if the new value is a valid double between the min & max.
	 * @param newMaxPPD.
	 * @throws IllegalArgumentException if the value passed in is not within the acceptable range.
	 */
	public void setMaxPPD(double newMaxPPD) throws IllegalArgumentException{
		if (newMaxPPD >= MAXPPD_MIN && newMaxPPD <= MAXPPD_MAX){
			this.maxPPD = newMaxPPD;
			changed();
		}else{
			throw new IllegalArgumentException("An attempt was made to change the MaxPPD to an invalid value of " + newMaxPPD +  ".  MaxPPD was left at " + this.maxPPD + ".  Valid values are "+MAXPPD_MIN+" to "+MAXPPD_MAX+".");
		}
	}
	
	/**
	 * Returns the maximum PPD at which this source is available, or
	 * Double.POSITIVE_INFINITY if there is no maximum.
	 */
	public double getMaxPPD() {
		return maxPPD;
	}
	
	/**
	 * Returns the user name portion of the email address of the map owner
	 */
	public String getOwner() {		
		return (owner != null) ? owner.split("@", 1)[0] : owner;
	}
	/**
	 * this method lazily creates the listener array; this techique must be used
	 * instead of readObject(), since instances of WMSMapSource exist written by
	 * xstream, written without a writeObject() implementation, that cannot be
	 * read back by a class with a readObject implementation.
	 */
	private List<MapSourceListener> getListeners() {
		if (listeners == null) {
			listeners = new ArrayList<MapSourceListener>();
		}
		return listeners;
	}
	
	public void addListener(MapSourceListener l) {
		getListeners().add(l);
	}
	
	public void removeListener(MapSourceListener l) {
		getListeners().remove(l);
	}
	
	private void changed() {
		final List<MapSourceListener> list = new ArrayList<MapSourceListener>(getListeners());
		if (list.isEmpty()) {
			return;
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (MapSourceListener l: list) {
					l.changed(WMSMapSource.this);
				}
			}
		});
	}
}
