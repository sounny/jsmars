package edu.asu.jmars.layer.map2;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputListener;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.InvestigateData;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.PannerGlass;
import edu.asu.jmars.layer.ProjectionEvent;
import edu.asu.jmars.layer.ProjectionListener;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.map2.msd.PipelineModel;
import edu.asu.jmars.layer.shape2.ShapePath;
import edu.asu.jmars.layer.stamp.networking.StampLayerNetworking;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.DeltaExecutor;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

/**
 * Manages graphic and numeric data within a JMARS view.
 * 
 * The graphic pipeline can issue settings change events that signal a need to
 * reprocess the pipeline. This view refuses to process more than one such
 * settings change in each three second window to prevent stages like the
 * grayscale stage that gradually discover their settings throughout the request
 * from reprocessing any given pixel more than once every three seconds. This
 * mechanism does guarantee that a final reprocess will occur after the last
 * settings change.
 * 
 * The tooltip pipeline exists for chart map sources for the main view only. It
 * always requests at the same resolution as the main view. When a tooltip pops
 * up, the value comes from the first matching numeric tile.
 */
public class MapLView extends LView {
	private static final long serialVersionUID = 1L;
	private static DebugLog log = DebugLog.instance();
	/** The death tile shown in areas where a server error prevented arrival of data */
	private static BufferedImage errorTile = Util.loadImage("resources/checker.png");
	/** The title of a map while its sources are being resolved */
	private static final String LOADING_TITLE = "Loading Map...";
	/** Parent MapLayer of this LView */
	private MapLayer mapLayer;
	/** The graphic map channel */
	private MapChannelTiled graphicRequest;
	/** The numeric map channel, used by the main view to get tooltip data */
	private MapChannelTiled numericRequest;
	/** Line for which the profile is to be plotted */
	protected Shape profileLine;
	/** Stores the profile line and manages mouse events in relation to it */
	protected ProfileLineDrawingListener profileLineMouseListener = null;
	/** Stores the cue position and manages mouse events in relation to it */
	private ProfileLineCueingListener profileLineCueingListener = null;
	/** Name of this layer, automatically created when the layer does not have a defined name */
	private String name;
	/** The extent of the world, used to clip the data received from the server */
	private final Rectangle2D worldClip = new Rectangle2D.Double();
	/** The list of numeric tiles; only used by the main view with at least one plot selection */
	private final List<MapData> numericTiles = new ArrayList<MapData>();

	/** The list of graphic tiles that make up a geologic map, used for looking up the unit type*/
	private final List<MapData> geologicTiles = new ArrayList<MapData>();
	
	/** Set to true if the graphic source for this LView has the geographic tag */
	boolean isGeologic = false;		
			
	/** Ensures that certain painting operations are spaced out enough for something useful to have changed */
	private final DeltaExecutor repainter = new DeltaExecutor(3000, new Runnable() {
		public void run() {
			MapLView.super.repaint();
		}
	});

	public MapLView(MapLayer layer, final boolean mainView, MapLView3D lview3d){
		this(layer, mainView, null, lview3d);
	}
	
	public MapLView(MapLayer layer, final boolean mainView) {
		this(layer, mainView, null, null);
	}
		
	/** Constructs the main and panner views */
		public MapLView(MapLayer layer, final boolean mainView, Object layerIdentifier, LView3D lview3d) {
		super(layer, lview3d);
		if(layerIdentifier instanceof LayerParameters){
			this.layerParams = (LayerParameters)layerIdentifier;
		}else if(layerIdentifier instanceof String){
			this.layerKey = (String)layerIdentifier;
		}
		mapLayer = layer;
		
		Main.addProjectionListener(new ProjectionListener() {
			public void projectionChanged(ProjectionEvent e) {
				updateProjection();
			}
		});
		
		if (mainView) {
			layer.focusPanel = (MapFocusPanel)createFocusPanel();
			focusPanel = layer.focusPanel;
			
			profileLineCueingListener = new ProfileLineCueingListener();
			addMouseMotionListener(profileLineCueingListener);
			
			if(lview3d instanceof MapLView3D){
				((MapLView3D)lview3d).setLView(this);
			}
		}
		
		layer.focusPanel.addPipelineEventListener(new PipelineEventListener() {
			public void pipelineEventOccurred(PipelineEvent e) {
				updateGraphicPipeline(e);
			}
		});
		updateGraphicPipeline(new PipelineEvent(layer.focusPanel, true, false));
		
		if (mainView) {
			mapLayer.mapSettingsDialog.addPipelineEventListener(new PipelineEventListener() {
				public void pipelineEventOccurred(PipelineEvent e) {
					updateNumericPipeline(e);
				}
			});
			updateNumericPipeline(new PipelineEvent(mapLayer.mapSettingsDialog, true, false));
		}
		
		updateStatus();
	}
	
	/** Creates a new channel for the graphic part of the view */
	private MapChannelTiled createGraphicChannel() {
		MapChannelReceiver r = new MapChannelReceiver() {
			public void mapChanged(MapData mapData) {
				updateGraphicData(mapData);
			}
		};
		return new MapChannelTiled(r);
	}
	
	/** Creates a new channel for the numeric part of the view */
	private MapChannelTiled createNumericChannel() {
		MapChannelReceiver r = new MapChannelReceiver() {
			public void mapChanged(MapData mapData) {
				updateNumericData(mapData);
			}
		};
		return new MapChannelTiled(r);
	}
	
	private void alog(String msg) {
		log.aprintln((getChild()!=null?"[Main":"[Child") + " view] " + msg);
	}
	
	private void log(String msg) {
		log.println((getChild()!=null?"[Main":"[Child") + " view] " + msg);
	}
	
	
	public MapSource[] getNumericSources() {
		if (numericRequest==null) {
			return new MapSource[0];
		}
		Pipeline numPipes[] = numericRequest.getPipelines();
		
		MapSource numSources[] = new MapSource[numPipes.length];
		
		for (int i=0; i<numPipes.length; i++) {
			numSources[i] = numPipes[i].getSource();
		}
		return numSources;
	}
	
	/*
	 * This method generates the standard Java Tooltip, populated with data from the numeric data contained
	 * within this LView
	 */
	public String getToolTipText(MouseEvent event) {
		// Only show tooltips for a visible Main view of a selected layer (unless in investigate mode)
		if ((viewman.getActiveLView()!=this || event.getSource() instanceof PannerGlass || !isVisible()) && ToolManager.getToolMode()!=ToolManager.INVESTIGATE) {
			return null;
		}
		
		if (isGeologic) {
			// convert screen mouse point to unwrapped world and then normalize it to wrapped world coordinates
			Point2D worldPoint = getProj().screen.toWorld(event.getPoint());
			Rectangle2D worldExtent = Util.toWrappedWorld(new Rectangle2D.Double(worldPoint.getX(), worldPoint.getY(), 0, 0))[0];
			worldPoint.setLocation(worldExtent.getMinX(), worldExtent.getMinY());
			
			for (MapData tile: geologicTiles) {
				Rectangle2D extent = tile.getRequest().getExtent();
				if (extent.contains(worldPoint)) {				
					int ppd = tile.getRequest().getPPD();
					Rectangle2D sampleExtent = new Rectangle2D.Double(worldPoint.getX(), worldPoint.getY(), 1d/ppd, 1d/ppd);
					
					int pixelInts[] = tile.getRasterForWorld(sampleExtent).getPixel(0, 0, (int[])null);
					
					StringBuffer readouts = new StringBuffer(100);
					
					String mapId = tile.getRequest().getSource().getName();
					
					ArrayList<String> strVals = StampLayerNetworking.getUnitsForGeologicMap(mapId);
					ArrayList<Integer[]> mapVals = StampLayerNetworking.getColorMapForGeologicMap(mapId);
																						
					int r = pixelInts[0];
					int g = pixelInts[1];
					int b = pixelInts[2];
					
					for (int i=0; i<strVals.size(); i++) {
						Integer map[] = mapVals.get(i);
						if (r==map[0] && g==map[1] && b==map[2]) {
							readouts.append(strVals.get(i));
							break;
						}
					}

					// Only make the table if we actually have results to show
					if (readouts.length()>0 && ToolManager.getToolMode()!=ToolManager.INVESTIGATE){
						readouts.insert(0, "<html><table cellspacing=0 cellpadding=1>");
						readouts.append("</table>");
						readouts.append("</html>");
					}
	
					return readouts.toString();
				}
			}
		}
		
		// Only show tooltips when we have data to search through
		if (numericRequest==null || numericTiles.size() == 0) {
			return null;
		}
		
		// convert screen mouse point to unwrapped world and then normalize it to wrapped world coordinates
		Point2D worldPoint = getProj().screen.toWorld(event.getPoint());
		Rectangle2D worldExtent = Util.toWrappedWorld(new Rectangle2D.Double(worldPoint.getX(), worldPoint.getY(), 0, 0))[0];
		worldPoint.setLocation(worldExtent.getMinX(), worldExtent.getMinY());
		
		for (MapData tile: numericTiles) {
			Rectangle2D extent = tile.getRequest().getExtent();
			if (extent.contains(worldPoint)) {				
				int ppd = tile.getRequest().getPPD();
				Rectangle2D sampleExtent = new Rectangle2D.Double(worldPoint.getX(), worldPoint.getY(), 1d/ppd, 1d/ppd);
				double[] samples = tile.getRasterForWorld(sampleExtent).getPixels(0, 0, 1, 1, (double[])null);
				
				NumberFormat nf = NumberFormat.getNumberInstance();
				nf.setMaximumFractionDigits(5);
				StringBuffer readouts = new StringBuffer(100);
				
				if (ToolManager.getToolMode()!=ToolManager.INVESTIGATE){
					readouts.append("<html>");
					readouts.append("<table cellspacing=0 cellpadding=1>");
				}
				
				Pipeline numPipes[] = numericRequest.getPipelines();
				
				/*
				 * We want to be able to determine the status of each MapRequest we are
				 * plotting, but by the time we can look at the data, it has all been
				 * compiled into one MapData object.
				 * 
				 * The data will be initialized to NaN, so we can tell if it has been
				 * reset by real data, but we can't currently tell if it hasn't loaded
				 * yet or if we've given up on it. We should solve this problem more
				 * thoroughly when we have time.
				 */
				
readoutloop:				for (int i=0; i<samples.length; i++) {
					MapSource source = numPipes[i].getSource();
					String title = source.getTitle();
					String units = source.getUnits();
					if(units==null){
						units = "";
					}else{
						units=" "+units;
					}
					
					
					StringBuffer row = new StringBuffer(200);
					row.append("<tr><td align=right nowrap><b>");
					row.append(title +":");
					row.append("</b></td>");
					row.append("<td>");
					
					double ignoreValues[] = numPipes[i].getSource().getIgnoreValue();
					
					if (ignoreValues!=null) {
						for (int v=0; v<ignoreValues.length; v++) {
							if (samples[i]==ignoreValues[v]) {
								continue readoutloop;
							}
						}
					}
					
					if (Double.isNaN(samples[i])) {
						row.append("Value Unavailable");
					} else {
						row.append(nf.format(samples[i])+units);
					}
					row.append("</td></tr>");
					readouts.append(row);
				}
				
				if(ToolManager.getToolMode()!=ToolManager.INVESTIGATE){
					readouts.append("</table>");
					readouts.append("</html>");
				}

				return readouts.toString();
			}
		}
		
		return null;
	}
	
	public InvestigateData getInvestigateData(MouseEvent event){
	//Only do this for main view	
		if ((viewman.getActiveLView()!=this || event.getSource() instanceof PannerGlass || !isVisible()) && ToolManager.getToolMode()!=ToolManager.INVESTIGATE) {
			return null;
		}
	//Gets the layer name to display as the first part of investigateData	
		String layerName = LManager.getLManager().getUniqueName(this);
		
	//Instantiates investigate data type object with the layerName	
		InvestigateData invData = new InvestigateData(layerName);
		
	//Fills the hashmap with key and value entries to display under layer name	
		// convert screen mouse point to unwrapped world and then normalize it to wrapped world coordinates
		Point2D worldPoint = getProj().screen.toWorld(event.getPoint());
		Rectangle2D worldExtent = Util.toWrappedWorld(new Rectangle2D.Double(worldPoint.getX(), worldPoint.getY(), 0, 0))[0];
		worldPoint.setLocation(worldExtent.getMinX(), worldExtent.getMinY());
		for (MapData tile: numericTiles) {
			Rectangle2D extent = tile.getRequest().getExtent();
			if (extent.contains(worldPoint)) {			
				int ppd = tile.getRequest().getPPD();
				Rectangle2D sampleExtent = new Rectangle2D.Double(worldPoint.getX(), worldPoint.getY(), 1d/ppd, 1d/ppd);
				double[] samples = tile.getRasterForWorld(sampleExtent).getPixels(0, 0, 1, 1, (double[])null);
				
				NumberFormat nf = new DecimalFormat("0.00000");
				Pipeline numPipes[] = numericRequest.getPipelines();
			
				loop: for (int i=0; i<samples.length; i++) {
					MapSource source = numPipes[i].getSource();
					String mapName = source.getTitle();
					String units = source.getUnits();
					String mapData;
					boolean isNumeric = false;
					
					double ignoreValues[]=numPipes[i].getSource().getIgnoreValue();

					if (ignoreValues!=null) {
						for (int v = 0; v<ignoreValues.length; v++) {
							if (samples[i]==ignoreValues[v]) {
								continue loop; 
							}
						}
					}
					
					if (Double.isNaN(samples[i])) {
						mapData = "Value Unavailable";
					} else {
						mapData = nf.format(samples[i]);
						isNumeric = true;
					}
					if (mapData == null) continue loop;
					invData.add(mapName, mapData, units, isNumeric);
				}
			}
		}
		
		return invData;
		
	}
	
	public FocusPanel getFocusPanel() {
		return mapLayer.focusPanel;
	}
	
	public MapLayer getLayer() {
		return mapLayer;
	}
	
	public MapFocusPanel createFocusPanel() {
		MapFocusPanel focusPanel = new MapFocusPanel(this);
		
		// Add mouse listener for profile line updates
		profileLineMouseListener = new ProfileLineDrawingListener();
		addMouseListener(profileLineMouseListener);
		addMouseMotionListener(profileLineMouseListener);
		addKeyListener(profileLineMouseListener);
		
		return focusPanel;
	}
	
	/**
	 * Called by this.dup(), which is called by the LViewManager to get a panner
	 * instance
	 */
	protected LView _new() {
		return new MapLView(mapLayer, false);
	}
	
	/**
	 * The original intention of this method was to create a request object that
	 * is handed off to the Layer. The layer responds back by passing responses
	 * to the {@link #receiveData(Object)}. However, we are bypassing that mechanism
	 * updating the channel interface with the new parameters. Note that this method
	 * is called whenever there is a viewport change (i.e. pan/zoom/reproject) and
	 * the view is visible.
	 */
	protected Object createRequest(Rectangle2D where) {
		updateChannelDetails();
		return null;
	}
	
	/** Does nothing here - not using the Layer.LView requestData()/receiveData() mechanism */
	public void receiveData(Object layerData) {}
	
	/**
	 * Paints the component using the super's paintComponent(Graphics),
	 * followed by the profile-line drawing onto the on-screen graphics
	 * context.
	 */
	public synchronized void paintComponent(Graphics g) {
		// Don't try to draw unless the view is visible
		if (!isVisible() || viewman == null) { //@since remove viewman2
			return;
		}
		
		// super.paintComponent draws the back buffers onto the layer panel
		super.paintComponent(g);
		
		// then we draw the profile line on top of the layer panel
		Graphics2D g2 = (Graphics2D) g.create();
		g2 = viewman.wrapWorldGraphics(g2);
		g2.transform(getProj().getWorldToScreen());
		g2.setStroke(new BasicStroke(0));
		
		if (profileLineMouseListener != null)
			profileLineMouseListener.paintProfileLine(g2);

		if (profileLineCueingListener != null)
			profileLineCueingListener.paintCueLine(g2);
		
		if (profileLine != null){
			g2.setColor(Color.red);
			g2.draw(profileLine);
		}
	}
	
	/**
	 * Ensures that when a layer is not visible, it is not doing anything, and
	 * that if it was hidden while visible the dirty flag is set properly
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		
		if (!visible) {
			if (graphicRequest != null) {
				if (!graphicRequest.isFinished()) {
					setDirty(true);
				}
				graphicRequest.cancel();
			}
			if (numericRequest != null) {
				if (!numericRequest.isFinished()) {
					setDirty(true);
				}
				numericRequest.cancel();
			}
			updateStatus();
		}
	}
	
	public void viewCleanup() {
		// make sure requests are deactivated
		setVisible(false);
		
		// make sure all sources are ignored
		for (MapSource s: getSources(graphicRequest)) {
			s.removeListener(graphicSourceListener);
		}
		for (MapSource s: getSources(numericRequest)) {
			s.removeListener(numericSourceListener);
		}
	}
	
	/**
	 * Returns default name of this JMARS-Layer as it is displayed in the
	 * Layer Manager window.
	 */
	public String getName() {
		if (mapLayer != null && mapLayer.getName() != null) {
			return mapLayer.getName();
		} else if (name != null) {
			return name;
		} else {
			return LOADING_TITLE;
		}
	}
	
	private void updateName() {
		Pipeline[] lviewPipeline = graphicRequest == null ? new Pipeline[0] : graphicRequest.getPipelines();
		Pipeline[] chartPipeline = numericRequest == null ? new Pipeline[0] : numericRequest.getPipelines();
		
		if (lviewPipeline.length == 0 && chartPipeline.length == 0) {
			name = LOADING_TITLE;
		} else if (lviewPipeline.length == 1 && chartPipeline.length == 1 &&
				lviewPipeline[0].getSource().getTitle().equals(chartPipeline[0].getSource().getTitle())) {
			name = lviewPipeline[0].getSource().getTitle();
		} else if (lviewPipeline.length == 0) {
			name = "Plot: "+ chartPipeline[0].getSource().getTitle();
			if (chartPipeline.length > 1)
				name += " + " + (chartPipeline.length-1) + " other"+((chartPipeline.length-1) > 1? "s": "");
		} else {
			if (lviewPipeline.length == 1) {
				name = lviewPipeline[0].getSource().getTitle();
			} else {
				name = Pipeline.getCompStage(lviewPipeline).getStageName();
			}
			
			if (chartPipeline.length > 0) {
				name += " + " + (chartPipeline.length) + " plots";
			}
		}
		
		// update labels
		if (LManager.getLManager() != null) {
			LManager.getLManager().updateLabels();
		}
	}
	
	/**
	 * Updates the status of this layer: green if done loading, the graphic
	 * request is done, and the tooltip request is done, red otherwise
	 */
	private synchronized void updateStatus() {
		boolean loading = getChild()!=null && getName().equals(LOADING_TITLE);
		boolean mapDone = graphicRequest == null || graphicRequest.isFinished();
		boolean chartDone = numericRequest == null || numericRequest.isFinished();
		boolean done = !loading && mapDone && chartDone;
		mapLayer.monitoredSetStatus(this, done ? Util.darkGreen: Util.darkRed);
	}
	
	/** Receives a tile of map data and paints the visible portion of it to the back buffer */
	private synchronized void updateGraphicData(MapData newData) {
		if (!isAlive()) {
			// we have nothing to do when the LView is not selected for viewing
			log("Received mapData, but layer invisible.");
			updateStatus();
			return;
		}
		
		// clear the screen and get out if we don't have good data
		if (newData == null) {
			alog("Received null mapData, ignoring tile.");
			updateStatus();
			return;
		}
		
		BufferedImage img = newData.getImage();
		
		Graphics2D g2 = null;
		try {
			// At this point, we have something to draw
			g2 = getOffScreenG2();
			if (newData.isFinished() && newData.getFinishedArea().isEmpty()) {
				// paint an indication of error, the 'death tile'
				double length = 50d / newData.getRequest().getPPD();
				Paint p = new TexturePaint(errorTile, new Rectangle2D.Double(0,0,length,length));
				g2.setPaint(p);
				g2.fill(newData.getRequest().getExtent());
			} else if (!newData.getValidArea().isEmpty() && img != null) {
				// paint a good MapData object by passing through ignore filter if defined, and clipping to extent
				BufferedImageOp op = newData.getOperator();
				if (op != null) {
					img = op.filter(img, Util.newBufferedImage(img.getWidth(), img.getHeight()));
				}
				Rectangle2D dataBounds = newData.getRequest().getExtent();
				worldClip.setFrame(-180,-90,720,180);
				Rectangle bounds = MapData.getRasterBoundsForWorld(img.getRaster(), dataBounds, worldClip);
				if (!bounds.isEmpty()) {
					g2.setComposite(AlphaComposite.Src);
					Rectangle2D.intersect(worldClip, dataBounds, worldClip);
					img = img.getSubimage(bounds.x, bounds.y, bounds.width, bounds.height);
					g2.drawImage(img, Util.image2world(img.getWidth(), img.getHeight(), worldClip), null);
				}
			} else {
				log("No valid data to draw, and not finished, skipping until next update");
			}
		} catch (Exception ex) {
			alog("Error drawing image");
			log.aprintln(ex);
		} finally {
			if (g2 != null) {
				g2.dispose();
			}
		}
		
		if (newData.isFinished() && isGeologic) {
			geologicTiles.add(newData);
		}
		
		updateStatus();
		repaintDeferred(graphicRequest == null || graphicRequest.isFinished());
	}
	
	/**
	 * Receives a tile of data and adds it to the numeric tiles list for the
	 * tooltip handler to search through
	 */
	private synchronized void updateNumericData(MapData newData) {
		if (isVisible()) {
			if (newData.isFinished()) {
				numericTiles.add(newData);
			}
			updateStatus();
		}
	}
	
	/**
	 * Returns the portion of the wrapped world coordinate system that this view
	 * can see, or null if there is no associated view manager as of yet.
	 */
	private Rectangle2D getClippedExtent() {
		if (!isVisible()) {//@since remove viewman2
			return null;
		} else {
			// get portion of view over the OC-projected world
			Rectangle2D viewExtent = getProj().getWorldWindow();
			double y1 = Math.max(-90, viewExtent.getMinY());
			double y2 = Math.min(90, viewExtent.getMaxY());
			viewExtent.setRect(viewExtent.getMinX(), y1, viewExtent.getWidth(), y2-y1);
			return viewExtent;
		}
	}
	
	private static final Point2D zero = new Point2D.Double(0,0);
	
	private synchronized void updateProjection() {
		log("Proj changed event");
		if (getChild() != null) {
			// main view loses the profile line
			setProfileLine(null);
		}

		// Reset all MapSources' nudging offsets
		if (MapServerFactory.getMapServers() != null) {
			for(MapServer mapServer: MapServerFactory.getMapServers()) {
				for(MapSource mapSource: mapServer.getMapSources()){
					if (!zero.equals(mapSource.getOffset())) {
						mapSource.setOffset(new Point2D.Double(0,0));
					}
				}
			}
		}
	}
	
	/**
	 * Updates channel detail parameters of resolution/ppd, view-extent, and
	 * projection.
	 * 
	 * Also manages back buffers so we get consistent paints between when
	 * something changes, and when updated data begins arriving.
	 */
	private void updateChannelDetails() {
		log("Window changed event");
		
		// get projection, extent, and magnification level
		ProjObj proj = getPO();
		Rectangle2D viewExtent = getClippedExtent();
		int ppd = viewman.getZoomManager().getZoomPPD();
		
		clearOffScreen();
		
		if (viewExtent.isEmpty()) {
			// If the current view window doesn't intersect the world, then don't do anything
			log("channel not updated: view not touching world");
			if (graphicRequest != null) {
				graphicRequest.cancel();
			}
			if (numericRequest != null) {
				numericRequest.cancel();
			}
		} else {
			log(MessageFormat.format(
				"[''{7}'' {8}]  proj[{0,number,#.##},{1,number,#.##}] ppd[{2}] " +
				"extent[{3,number,#.###},{4,number,#.###} - {5,number,#.###},{6,number,#.###}]",
				proj.getCenterLon(), proj.getCenterLat(), ppd, viewExtent.getMinX(),
				viewExtent.getMinY(), viewExtent.getMaxX(), viewExtent.getMaxY(),
				getName(), getChild() == null ? "panner" : "main"));
			
			if (graphicRequest != null) {
				graphicRequest.setRequest(proj, viewExtent, ppd, graphicRequest.getPipelines());
				printPipeline(graphicRequest.getPipelines(), "Using old pipeline");
				geologicTiles.clear();
			}
			
			if (numericRequest != null) {
				numericRequest.setRequest(proj, viewExtent, ppd, numericRequest.getPipelines());
				numericTiles.clear();
			}
			updateStatus();
		}
	}
	
	/**
	 * Sets line for which profile is to be extracted.
	 * @param newProfileLine Set the new line to be profiled to this line.
	 *        A null value may be passed as this argument to clear the profile line.
	 */
	public void setProfileLine(Shape newProfileLine){
		log("update profile line: "+newProfileLine);
		if (numericRequest == null
				|| numericRequest.getPipelines() == null
				|| numericRequest.getPipelines().length == 0) {
			profileLine = null;
		} else {
			profileLine = newProfileLine;
		}
		if (focusPanel != null && (((MapFocusPanel)focusPanel).getChartView()) != null){
			ChartView chartView = ((MapFocusPanel)focusPanel).getChartView();
			chartView.setProfileLine(profileLine, profileLine == null? 1: getProj().getPPD());
		}
	}
	
	/**
	 * Receive cueChanged events from chartView.
	 * @param worldCuePoint The new point within the profileLine segment boundaries
	 *        where the cue is to be generated.
	 */
	public void cueChanged(Point2D worldCuePoint){
		profileLineCueingListener.setCuePoint(worldCuePoint);
	}
	
	private void printPipeline(Pipeline[] pipes, String msg) {
		log(msg + " " + (pipes==null ? "[]" : Arrays.asList(pipes)));
	}
	
	private MapSourceListener graphicSourceListener = new MapSourceListener() {
		public void changed(MapSource source) {
			updateGraphicPipeline(new PipelineEvent(mapLayer.focusPanel, false, true));
		}
	};
	
	private MapSourceListener numericSourceListener = new MapSourceListener() {
		public void changed(MapSource source) {
			updateNumericPipeline(new PipelineEvent(mapLayer.mapSettingsDialog, false, true));
		}
	};
	
	/** Called to update the graphic pipeline, not called when the composite stage is set to None */
	protected synchronized void updateGraphicPipeline(PipelineEvent e) {
		log("Graphic pipeline event");
		
		geologicTiles.clear();
		
		// if there was a structure change
		if (!e.settingsChange) {
			updateName();
		}
		
		Collection<MapSource> oldSources = getSources(graphicRequest);
		
		Pipeline[] graphicPipeline = e.source.buildLViewPipeline();
		if (graphicPipeline.length > 0) {
			// new pipeline requires a graphic request so create one if needed
			if (graphicRequest == null) {
				graphicRequest = createGraphicChannel();
			}
			
			if (isVisible() && viewman != null) {//@since remove viewman2
				// view is visible so clear the buffer and set the request
				clearOffScreen();
				if (e.settingsChange) {
					log("Pipeline settings change");
				} else {
					printPipeline(graphicPipeline, "New pipeline");
				}
				ProjObj proj = getPO();
				Rectangle2D viewExtent = getClippedExtent();
				int ppd = viewman.getZoomManager().getZoomPPD();
				graphicRequest.setRequest(proj, viewExtent, ppd, graphicPipeline);
				repaintDeferred(false);
			} else {
				// view is not visible so disable the request and update the pipeline
				graphicRequest.setRequest(null, null, 0, graphicPipeline);
			}
		} else {
			// nothing to do so remove any existing request and erase the view if visible
			if (graphicRequest != null) {
				graphicRequest.cancel();
				graphicRequest = null;
			}
			if (isVisible()) {
				clearOffScreen();
				repaintDeferred(false);
			}
		}
		
		Collection<MapSource> newSources = getSources(graphicRequest);
		updateListeners(oldSources, newSources, graphicSourceListener);
		
		for (MapSource source : newSources) {
			if (source.hasGeologicKeyword()) {
				isGeologic=true;
			}
		}
		
		// We only want to update the state id and 3D view once per pipeline change, so only do it for the MainView, not the panner view
		// Otherwise the 3D view gets confused by mis-matched state ids and does nothing
		if (getChild()!=null) {
			mapLayer.increaseStateId(0);
			if(ThreeDManager.isReady()){
				ThreeDManager.getInstance().updateDecalsForLView(this, false);
			}
		}
		
		updateStatus();
	}
	
	/**
	 * @return The graphicRequest object containing the pipelines 
	 * and sources used to produce the graphic view.
	 */
	public MapChannelTiled getGraphicRequest(){
		return graphicRequest;
	}
	
	public void repaintDeferred(boolean done) {
		if (done) {
			repainter.runImmediately();
		} else {
			repainter.runDeferrable();
		}
	}
	
	/** Called to update the numeric pipeline, not called for the panner view */
	private synchronized void updateNumericPipeline(PipelineEvent e) {
		log("Numeric pipeline event");
		
		numericTiles.clear();
		
		Collection<MapSource> oldSources = getSources(numericRequest);
		
		Pipeline[] numericPipeline = e.source.buildChartPipeline();
		if (numericPipeline.length > 0) {
			if (numericRequest == null) {
				numericRequest = createNumericChannel();
			}
			if (isVisible() && viewman != null) {//@since remove viewman2
				ProjObj proj = getPO();
				Rectangle2D viewExtent = getClippedExtent();
				int ppd = viewman.getZoomManager().getZoomPPD();
				numericRequest.setRequest(proj, viewExtent, ppd, numericPipeline);
			} else {
				numericRequest.setRequest(null, null, 0, numericPipeline);
			}
		} else {
			if (numericRequest != null) {
				numericRequest.cancel();
				numericRequest = null;
			}
			setProfileLine(null); // clear the active profile line, if any
		}
		
		Collection<MapSource> newSources = getSources(numericRequest);
		updateListeners(oldSources, newSources, numericSourceListener);
		
		if(ThreeDManager.isReady()){
			ThreeDManager.getInstance().updateDecalsForLView(this, false);
		}
		
		updateName();
		updateStatus();
	}
	
	/** helper method that gets the set of unique MapSource objects, comparing by reference */
	private static Collection<MapSource> getSources(MapChannelTiled ch) {
		if (ch == null) {
			return new ArrayList<MapSource>();
		} else {
			Pipeline[] pipes = ch.getPipelines();
			Map<MapSource,Object> sources = new IdentityHashMap<MapSource,Object>();
			for (Pipeline p: pipes) {
				sources.put(p.getSource(), null);
			}
			return sources.keySet();
		}
	}
	
	/** helper method that registers and unregisters listeners such that only the new sources will fire events to this MapLView */
	private static void updateListeners(Collection<MapSource> oldSources, Collection<MapSource> newSources, MapSourceListener l) {
		for (MapSource s: oldSources) {
			if (!newSources.contains(s)) {
				s.removeListener(l);
			}
		}
		for (MapSource s: newSources) {
			if (!oldSources.contains(s)) {
				s.addListener(l);
			}
		}
	}
	
	/**
	 * BaseGlass proxy wraps the screen coordinates, which we do NOT want, so we use the
	 * real point it remembers IF this event is a wrapped one.
	 */
	public Point2D clampedWorldPoint (Point2D anchor, MouseEvent e) {
		Point mousePoint = e instanceof WrappedMouseEvent ? ((WrappedMouseEvent)e).getRealPoint() : e.getPoint();
		Point2D worldPoint = getProj().screen.toWorld(mousePoint);
		double x = Util.mod360(worldPoint.getX());
		double a = Util.mod360(anchor.getX());
		if (x - a > 180.0) x -= 360.0;
		if (a - x > 180.0) x += 360.0;
		double y = worldPoint.getY();
		if (y > 90) y = 90;
		if (y < -90) y = -90;
		return new Point2D.Double(x, y);
	}
	
	public SerializedParameters getInitialLayerData(){
		if(layerParams == null)
			return new InitialParams(mapLayer.focusPanel.getLViewPipelineModel(), mapLayer.mapSettingsDialog.getChartPipelineModel(), layerKey, profileLine);
		else
			return new InitialParams(mapLayer.focusPanel.getLViewPipelineModel(), mapLayer.mapSettingsDialog.getChartPipelineModel(), layerParams, profileLine);
	}
	
	public static class InitialParams implements SerializedParameters {
		private static final long serialVersionUID = -6327986391829454142L;
		
		public PipelineModel lviewPPM;
		public PipelineModel chartPPM;

		// Added to make sure map layers restore with the proper citation information
		public LayerParameters layerParams;
		// Added to make sure profile lines are restored.  Note GeneralPath is NOT serializable
		public ShapePath profilePath;

		public String layerKey;
		
		
		public InitialParams(PipelineModel lviewPPM, PipelineModel chartPPM, Object layerIdentifier, Shape profileLine){
			this.lviewPPM = lviewPPM;
			this.chartPPM = chartPPM;
			if(layerIdentifier instanceof LayerParameters){
				this.layerParams = (LayerParameters)layerIdentifier;
			}else if(layerIdentifier instanceof String){
				this.layerKey = (String)layerIdentifier;
			}
			if (profileLine!=null) {
				FPath fp = new FPath(profileLine, FPath.SPATIAL_EAST);
				this.profilePath = new ShapePath(fp);
			}
		}
	}
	
	/**
	 * Computes the perimeter length of the given shape (in world coordinates). The
	 * length is computed using the 
	 * {@link Util#angularAndLinearDistanceW(Point2D, Point2D, edu.asu.jmars.layer.MultiProjection)}
	 * method.
	 * @param shape Shape in world coordinates.
	 * @return Length of perimeter in degrees, kilometers and cartesian-distance.
	 */
	public double[] perimeterLength(Shape shape){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double angularDist = 0;
		double linearDist = 0;
		double cartDist = 0;
		
		while(!pi.isDone()){
			switch(pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double dists[] = Util.angularAndLinearDistanceW(lseg.getP1(), lseg.getP2(), getProj());
			angularDist += dists[0];
			linearDist += dists[1];
			cartDist += lseg.getP2().distance(lseg.getP1());
			pi.next();
		}
		
		return new double[]{ angularDist, linearDist, cartDist };
	}
	
	public static Point2D getFirstPoint(Shape s){
		PathIterator pi = s.getPathIterator(null, 0);
		if (pi.isDone())
			return null;
		
		double coords[] = new double[6];
		pi.currentSegment(coords);
		return new Point2D.Double(coords[0], coords[1]);
	}

	public static Point2D getLastPoint(Shape s){
		PathIterator pi = s.getPathIterator(null, 0);
		if (pi.isDone())
			return null;
		
		double coords[] = new double[6];
		while(!pi.isDone()){
			pi.currentSegment(coords);
			pi.next();
		}
		return new Point2D.Double(coords[0], coords[1]);
	}

	/**
	 * Computes the perimeter length of the given shape (in world coordinates). The
	 * length is computed using the 
	 * {@link Util#angularAndLinearDistanceW(Point2D, Point2D, edu.asu.jmars.layer.MultiProjection)}
	 * method.
	 * @param shape Shape in world coordinates.
	 * @return Length of perimeter in degrees, kilometers and cartesian-distance.
	 */
	public double[] distanceTo(Shape shape, Point2D pt){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double angularDist = 0;
		double linearDist = 0;
		double cartDist = 0;
		double t = uninterpolate(shape, pt, null);
		double lengths[] = perimeterLength(shape);
		double totalLength = lengths[2];
		
		if (t < 0 || t > 1){
			return new double[]{ Double.NaN, Double.NaN, Double.NaN };
		}
		else {
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					first.x = lseg.x1 = lseg.x2 = coords[0];
					first.y = lseg.y1 = lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = coords[0]; lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_CLOSE:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = first.x; lseg.y2 = first.y;
					break;
				}

				double lsegLength = lseg.getP2().distance(lseg.getP1());
				if ((cartDist + lsegLength)/totalLength > t){
					double dists[] = Util.angularAndLinearDistanceW(lseg.getP1(), pt, getProj());
					angularDist += dists[0]; 
					linearDist += dists[1];
					cartDist += pt.distance(lseg.getP1());
					break;
				}
				double dists[] = Util.angularAndLinearDistanceW(lseg.getP1(), lseg.getP2(), getProj());
				angularDist += dists[0];
				linearDist += dists[1];
				cartDist += lsegLength;
				pi.next();
			}
		}
		
		return new double[]{ angularDist, linearDist, cartDist };
	}

	/**
	 * Linearly uninterpolates the parameter <code>t</code> value of the specified
	 * point from its closest approach to the specified shape (in world 
	 * coordinates).
	 * @param shape Line-string in world-coordinates.
	 * @param pt Point for which the parameter <code>t</code> is to be determined.
	 * @param distance If not <code>null</code>, its first element contains
	 *     the minimum distance to one of the segments in the line-string.
	 * @return The parameter <code>t</code> which will give the specified
	 *     point if {@link #interpolate(Shape, double)} is called using it as the
	 *     second parameter. Returns {@link Double#NaN} if the shape contains only
	 *     a single point.
	 * {@see Util#uninterploate(Line2D, Point2D)}
	 */
	public double uninterpolate(Shape shape, Point2D pt, double[] distance){
		double t = Double.NaN;
		
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double cartDist = 0, linearDistToMinSeg = 0;
		double minDistSq = Double.MAX_VALUE;
		Line2D.Double minSeg = null;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double lsegDistSq = lseg.ptSegDistSq(pt);
			if (currSeg != PathIterator.SEG_MOVETO && lsegDistSq < minDistSq){
				minSeg = new Line2D.Double(lseg.x1, lseg.y1, lseg.x2, lseg.y2);
				minDistSq = lsegDistSq;
				linearDistToMinSeg = cartDist;
			}
			
			cartDist += lseg.getP2().distance(lseg.getP1());
			pi.next();
		}
		
		if (minSeg != null){
			double tt = Util.uninterploate(minSeg, pt);
			double minSegLength = minSeg.getP2().distance(minSeg.getP1());
			if (tt < 0 && linearDistToMinSeg > 0)
				tt = 0;
			if (tt > 1 && (linearDistToMinSeg + minSegLength) < totalLength)
				tt = 1;
			t = (linearDistToMinSeg + tt * minSegLength) / totalLength;
			//log.aprintln("pt:"+pt+"  linearDistToMinSeg:"+linearDistToMinSeg+"  uninterpol:"+Util.uninterploate(minSeg, pt)+"  minSeg:"+minSeg.getP1()+","+minSeg.getP2()+"  minSegDist:"+minSeg.getP2().distance(minSeg.getP1())+"  totalLen:"+totalLength);
			
			// fill the distance value
			if (distance != null && distance.length > 0)
				distance[0] = Math.sqrt(minDistSq);
		}

		return t;
	}
	
	/**
	 * Linearly interpolates a point given the shape (in world coordinates)
	 * and the parameter <code>t</code>.
	 * @param shape Line-string in world coordinates.
	 * @param t Interpolation parameter <code>t</code>.
	 * @return A point obtained by linear-interpolation using the points
	 *     in the shape, given the parameter <code>t</code>.
	 */
	public Point2D interpolate(Shape shape, double t){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double cartDist = 0;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double segLength = lseg.getP2().distance(lseg.getP1());
			if (currSeg != PathIterator.SEG_MOVETO && ((cartDist + segLength)/totalLength) >= t){
				return Util.interpolate(lseg, (t*totalLength-cartDist)/segLength);
			}
			
			cartDist += segLength;
			pi.next();
		}
		
		return Util.interpolate(lseg, (t*totalLength-cartDist)/(lseg.getP2().distance(lseg.getP1())));
	}
	
	/**
	 * Returns the shape formed by sub-selection based on the
	 * parameter range of <code>t</code>, where <code>t</code> ranges
	 * between <code>0</code> and <code>1</code> based on the Cartesian
	 * distance from the first point in the shape.
	 * @param shape Line-string.
	 * @param t0 Starting value of <code>t</code>
	 * @param t1 Ending value of <code>t</code>
	 * @return A sub-selected shape.
	 */
	public Shape spanSelect(Shape shape, double t0, double t1){
		if (t0 < 0 && t1 > 1){
			GeneralPath p = new GeneralPath();
			
			Point2D p0 = interpolate(shape, t0);
			p.moveTo((float)p0.getX(), (float)p0.getY());
			
			PathIterator pi = shape.getPathIterator(null, 0);
			float[] coords = new float[6];
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
					p.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					throw new RuntimeException("Unhandled situation! Expecting unclosed line-string.");
				}
				pi.next();
			}
			
			Point2D p1 = interpolate(shape, t1);
			p.lineTo((float)p1.getX(), (float)p1.getY());
			
			return p;
		}
		else if (t0 < 0){
			GeneralPath p = new GeneralPath();
			
			Point2D p0 = interpolate(shape, t0);
			p.moveTo((float)p0.getX(), (float)p0.getY());
			
			PathIterator pi = shape.getPathIterator(null, 0);
			float[] coords = new float[6];
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
				case PathIterator.SEG_LINETO:
					p.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					throw new RuntimeException("Unhandled situation! Expecting unclosed line-string.");
				}
				pi.next();
			}
			return p;
		}
		else if (t1 > 1){
			GeneralPath p = new GeneralPath();
			PathIterator pi = shape.getPathIterator(null, 0);
			float[] coords = new float[6];
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					p.moveTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_LINETO:
					p.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					throw new RuntimeException("Unhandled situation! Expecting unclosed line-string.");
				}
				pi.next();
			}
			
			Point2D p1 = interpolate(shape, t1);
			p.lineTo((float)p1.getX(), (float)p1.getY());
			
			return p;
		}
		else {
			GeneralPath p = new GeneralPath();
			
			PathIterator pi = shape.getPathIterator(null, 0);
			double coords[] = new double[6];
			Point2D.Double first = new Point2D.Double();
			Line2D.Double lseg = new Line2D.Double();
			double linearDist = 0;
			boolean startDone = false, endDone = false;
			double totalLength = perimeterLength(shape)[2];
			
			while(!pi.isDone()){
				switch(pi.currentSegment(coords)){
				case PathIterator.SEG_MOVETO:
					first.x = lseg.x1 = lseg.x2 = coords[0];
					first.y = lseg.y1 = lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = coords[0]; lseg.y2 = coords[1];
					break;
				case PathIterator.SEG_CLOSE:
					lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
					lseg.x2 = first.x; lseg.y2 = first.y;
					break;
				}
				
				double segLength = lseg.getP2().distance(lseg.getP1());
				if (!startDone && t0 >= (linearDist/totalLength) && t0 <= ((linearDist+segLength)/totalLength)){
					Point2D pt = Util.interpolate(lseg, t0-linearDist/totalLength);
					p.moveTo((float)pt.getX(), (float)pt.getY());
					startDone = true;
				}
				if (!endDone && t1 >= (linearDist/totalLength) && t1 <= ((linearDist+segLength)/totalLength)){
					Point2D pt = Util.interpolate(lseg, t1-linearDist/totalLength);
					p.lineTo((float)pt.getX(), (float)pt.getY());
					endDone = true;
				}
				if (startDone && !endDone){
					p.lineTo((float)coords[0], (float)coords[1]);
				}
				
				linearDist += segLength;
				pi.next();
			}
			return p;
		}
	}
	
	/**
	 * Determines the angle at which the line-segment surrounding
	 * the given parameter <code>t</code> is.
	 * @param shape Line-string in world coordinates.
	 * @param t Linear interpolation parameter.
	 * @return Angle of the line-segment bracketing the parameter
	 * <code>t</code> or <code>null</code> if <code>t</code> is
	 * out of range.
	 */
	public double angle(Shape shape, double t){
		PathIterator pi = shape.getPathIterator(null, 0);
		double coords[] = new double[6];
		Point2D.Double first = new Point2D.Double();
		Line2D.Double lseg = new Line2D.Double();
		double linearDist = 0;
		int currSeg = PathIterator.SEG_MOVETO;
		double totalLength = perimeterLength(shape)[2];
		
		while(!pi.isDone()){
			switch(currSeg = pi.currentSegment(coords)){
			case PathIterator.SEG_MOVETO:
				first.x = lseg.x1 = lseg.x2 = coords[0];
				first.y = lseg.y1 = lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_LINETO:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = coords[0]; lseg.y2 = coords[1];
				break;
			case PathIterator.SEG_CLOSE:
				lseg.x1 = lseg.x2; lseg.y1 = lseg.y2;
				lseg.x2 = first.x; lseg.y2 = first.y;
				break;
			}
			
			double segLength = lseg.getP2().distance(lseg.getP1());//Util.angularAndLinearDistanceW(lseg.getP1(), lseg.getP2(), getProj())[1];
			if (currSeg != PathIterator.SEG_MOVETO && ((linearDist + segLength)/totalLength) >= t){
				HVector p1 = new HVector(lseg.x1, lseg.y1, 0);
				HVector p2 = new HVector(lseg.x2, lseg.y2, 0);
				double angle = HVector.X_AXIS.separationPlanar(p2.sub(p1), HVector.Z_AXIS);
				return angle;
			}
			
			linearDist += segLength;
			pi.next();
		}
		
		return Double.NaN;
	}
	
	/**
	 * This listener listens to and generates cueing events. Its
	 * functionality is completely isolated from the ProfileLineMouseListener.
	 * It however, depends upon the currently set profile line on the MapLView.
	 */
	class ProfileLineCueingListener extends MouseMotionAdapter {
		private int cueLineLengthPixels = 4;
		GeneralPath baseCueShape;
		Shape cueShape = null;
		
		public ProfileLineCueingListener(){
			super();
			
			GeneralPath gp = new GeneralPath();
			gp.moveTo(0, -cueLineLengthPixels/2);
			gp.lineTo(0, cueLineLengthPixels/2);
			baseCueShape = gp;
		}
								
		public void setCuePoint(Point2D worldCuePoint){
			Shape oldCueShape = cueShape;
			
			if (worldCuePoint == null)
				cueShape = null;
			else
				cueShape = computeCueLine(worldCuePoint);
			
			if (oldCueShape != cueShape)
				repaint();			
		}
		
		/**
		 * Generate cueing line for the specified mouse coordinates
		 * specified in world coordinates. A profileLine must be set
		 * in the MapLView for this method to be successful.
		 * @param worldMouse Mouse position in world coordinates.
		 * @return A new cueing line or null depending upon whether the
		 *        mouse coordinate "falls within" the profileLine range or not.
		 */
		private Shape computeCueLine(Point2D worldMouse){
			if (profileLine == null)
				return null;
			
			double t = uninterpolate(profileLine, worldMouse, null);
			Shape newCueShape = null;
			if (!Double.isNaN(t) && t >= 0.0 && t <= 1.0){
				Point2D mid = interpolate(profileLine, t);
				double angle = angle(profileLine, t);
				double scale = cueLineLengthPixels * getProj().getPixelWidth();
				//log.aprintln("worldMouse:"+worldMouse+"  -> t:"+t+"  -> mid:"+mid+"  -> angle"+angle);
				
				AffineTransform at = new AffineTransform();
				at.translate(mid.getX(), mid.getY());
				at.rotate(angle);
				//at.scale(scale, 1.0);
				at.scale(scale, scale);
				newCueShape = baseCueShape.createTransformedShape(at);
			}
			
			return newCueShape;
		}
		
		public void paintCueLine(Graphics2D g2){
			if (profileLine != null && cueShape != null){
				g2.setColor(Color.yellow);
				g2.draw(cueShape);
			}
		}
		
		public void mouseMoved(MouseEvent e) {
			if (profileLine == null)
				return;
			
			ChartView chartView;
			if (focusPanel == null || (chartView = ((MapFocusPanel)focusPanel).getChartView()) == null)
				return;
			
			Point2D pt = clampedWorldPoint(getFirstPoint(profileLine), e);
			
			double[] distance = new double[1];
			Point2D mid = interpolate(profileLine, uninterpolate(profileLine, pt, distance));
			int distInPixels = (int)Math.round(distance[0] * getProj().getPPD());
			//log.aprintln("mid:"+mid+"  pt:"+pt+"  dist:"+distance[0]+"  pixDist:"+distInPixels);
			if (distInPixels <= 50){
				tooltipsDisabled(true);
				chartView.cueChanged(mid);
				setCuePoint(mid);
			}
			else {
				tooltipsDisabled(false);
				chartView.cueChanged(null);
				setCuePoint(null);
			}
		}
	}
	
	/**
	 * Mouse listener for drawing profile line. It also holds the current
	 * in-progess profile line and is responsible for drawing it onto the
	 * on-screen buffer on a repaint. While drawing the profile line, the
	 * LViewManager's status bar is updated on every drag (via 
	 * {@link Main#setStatus(String)}) to show the new position, the 
	 * spherical distance and the linear distance traversed by the line.
	 * 
	 * Once the line is built, the LView is notified via its 
	 * {@link MapLView#setProfileLine(Shape)} method. The profile
	 * line created is either null, if no drag occurred, or an actual line
	 * if a drag really occurred.
	 */
	public class ProfileLineDrawingListener implements MouseInputListener, KeyListener {
		Point2D p2 = null;
		List<Point2D> profileLinePts = new ArrayList<Point2D>();
		boolean closed = false;
		
		public void mouseClicked(MouseEvent e) {
			if(ToolManager.getToolMode()==ToolManager.SEL_HAND){
				if (numericRequest == null) {
					if (profileLine != null) {
						clearPath();
					}
				} else if (SwingUtilities.isLeftMouseButton(e)) {
					if (e.getClickCount() == 1){
						if (closed){
							profileLinePts.clear();
							setProfileLine(null);
							cueChanged(null);
							closed = false;
						}
						
						Point2D p1;
						if (profileLinePts.isEmpty())
							p1 = getProj().screen.toWorld(e.getPoint());
						else
							p1 = clampedWorldPoint(profileLinePts.get(0), e);
						profileLinePts.add(p1);
						p2 = p1;
						repaint();
					}
					else if (e.getClickCount() == 2){
						if (!closed){
							Point2D p1;
							if (profileLinePts.isEmpty())
								p1 = getProj().screen.toWorld(e.getPoint());
							else
								p1 = clampedWorldPoint(profileLinePts.get(0), e);
							profileLinePts.add(p1);
							
							p2 = null;
							setProfileLine(convert(profileLinePts, null));
							profileLinePts.clear();
							repaint();
							closed = true;
						}
					}
				}
			}
		}
		
		private void removeLastPoint(){
			if (profileLinePts.size() > 1){
				p2 = profileLinePts.remove(profileLinePts.size()-1);
			}
			else {
				profileLinePts.clear();
				p2 = null;
			}
			setProfileLine(null); //profileLinePts.isEmpty()? null: convert(profileLinePts, null));
			cueChanged(null);
			closed = false;
			repaint();
		}
		
		private void clearPath() {
			profileLinePts.clear();
			p2 = null;
			setProfileLine(null);
			cueChanged(null);
			closed = false;
			repaint();
		}
		
		private Shape convert(List<Point2D> pts, Point2D lastPt){
			GeneralPath gp = new GeneralPath();
			List<Point2D> tmp = new ArrayList<Point2D>(pts.size()+1);
			tmp.addAll(pts);
			if (lastPt != null)
				tmp.add(lastPt);
			
			for(Point2D pt: tmp){
				if (gp.getCurrentPoint() == null)
					gp.moveTo((float)pt.getX(), (float)pt.getY());
				else
					gp.lineTo((float)pt.getX(), (float)pt.getY());
			}
			
//			return gp;
			FPath fp = new FPath(gp, FPath.SPATIAL_WEST);
			return fp.getShape();
		}
		
		public void mouseMoved(MouseEvent e) {
			if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
				if (!closed && !profileLinePts.isEmpty()){
					p2 = clampedWorldPoint(profileLinePts.get(0), e);
					List<Point2D> points = new ArrayList<Point2D>(profileLinePts);
					points.add(p2);
					Main.setStatusFromWorld(points.toArray(new Point2D[points.size()]));
					
					// Update the view so that it can display the in-progress profile line
					repaint();
				}
			}
		}
		
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		public void mouseDragged(MouseEvent e) {}
		
		public void paintProfileLine(Graphics2D g2){
			if (profileLinePts.isEmpty())
				return;
			
			g2.setColor(Color.yellow);
			g2.draw(convert(profileLinePts, closed? null: p2));
		}

		public void keyPressed(KeyEvent e) {
			if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
				if (e.getKeyChar() == KeyEvent.VK_ESCAPE){
					removeLastPoint();
				}
			}
		}

		public void keyReleased(KeyEvent e) {
		}

		public void keyTyped(KeyEvent e) {
		}
	}
	
//The following two methods are used to query for the maps
// info panel fields (description, citation, etc)	
	public String layerKey;
 	public String getLayerKey(){
 		if (layerKey == null){
 			if(layerParams == null){
 				return getName();
 			}else{
 				return layerParams.name;
 			}
 		}else{
 			return layerKey;
 		}
 	}
 	public String getLayerType(){
 		if (layerParams == null)
 			return "map";
 		return layerParams.type;
 	}
 	public LayerParameters getLayerParameters(){
 		return layerParams;
 	}
}
