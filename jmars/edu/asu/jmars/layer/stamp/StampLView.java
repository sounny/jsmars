package edu.asu.jmars.layer.stamp;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Dimension2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.WritableRaster;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputListener;
import javax.swing.table.TableColumn;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ZoomManager;
import edu.asu.jmars.layer.InvestigateData;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.shape2.ShapeFactory;
import edu.asu.jmars.layer.shape2.ShapeLView;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.stamp.StampLayer.StampSelectionListener;
import edu.asu.jmars.layer.stamp.StampLayer.StampTask;
import edu.asu.jmars.layer.stamp.StampLayer.Status;
import edu.asu.jmars.layer.stamp.chart.ChartView;
import edu.asu.jmars.layer.stamp.chart.ProfileLineCueingListener;
import edu.asu.jmars.layer.stamp.chart.ProfileLineDrawingListener;
import edu.asu.jmars.layer.stamp.focus.OutlineFocusPanel;
import edu.asu.jmars.layer.stamp.focus.OutlineOrderDialog.OrderRule;
import edu.asu.jmars.layer.stamp.focus.ScatterView;
import edu.asu.jmars.layer.stamp.focus.SpectraView;
import edu.asu.jmars.layer.stamp.focus.StampFocusPanel;
import edu.asu.jmars.layer.stamp.radar.FilledStampRadarType;
import edu.asu.jmars.layer.stamp.radar.FullResHighlightListener;
import edu.asu.jmars.layer.stamp.radar.RadarFocusPanel;
import edu.asu.jmars.layer.stamp.radar.RadarHorizon;
import edu.asu.jmars.layer.stamp.spectra.SpectraPixelDialog;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.FeatureProviderStamp;
import edu.asu.jmars.layer.util.features.GeomSource;
import edu.asu.jmars.swing.ValidClipboard;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.IgnoreComposite;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.FilteringColumnModel;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.emory.mathcs.backport.java.util.Collections;


/**
 * Base view implementation for stamp image layer.  
 */

public class StampLView extends Layer.LView implements StampSelectionListener
{
	private static final long serialVersionUID = 1L;

	private static final String VIEW_SETTINGS_KEY = "stamp";
    private static ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory() {
    	int id = 0;
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("StampLView-render-" + (id++));
			t.setPriority(Thread.MIN_PRIORITY);
			t.setDaemon(true);
			return t;
		}
    });
    
	static DebugLog log = DebugLog.instance();

	private static final int proximityPixels = 8;
	
	private static final int STAMP_RENDER_REPAINT_COUNT_MIN = 1;
	private static final int STAMP_RENDER_REPAINT_COUNT_MAX = 10;
	private static final int STAMP_RENDER_REPAINT_COUNT_BASE = 10;
	
	public StampShape[] stamps;

    // stamp outline drawing state
    private StampShape[] lastStamps;
    private Color lastUnsColor;
    private double lastMag;
    private double lastOriginMag;
    private Color lastOriginColor;
    private int projHash = 0;
    private boolean lastDisplayBoresight = true;
    private boolean lastDrawAsRings = false;
    private float lastRingWidth = 1;
    
    // filled stamp drawing state
	private volatile DrawFilledRequest lastRequest;
	private volatile List<FilledStamp> lastFilledStamps;
	private volatile StampShape[] lastAlphaStamps;
	private volatile Color lastFillColor;
	
	protected boolean restoreStampsCalled = false;

	protected boolean settingsLoaded = false;

	protected StampLayerWrapper wrapper;
	
	public StampLayer stampLayer;
		
	/** Line for which the profile is to be plotted */
	public Shape profileLine;
	/** Stores the profile line and manages mouse events in relation to it */
	private ProfileLineDrawingListener profileLineMouseListener = null;
	/** Stores the cue position and manages mouse events in relation to it */
	private ProfileLineCueingListener profileLineCueingListener = null;
	/** Used for the sharad layer, to highlight the section of a footprint that is
	 * being viewed in full resolution.	 */
	private FullResHighlightListener fullResHighlightListener = null;	
	/** Used for changing the cursor in the CRISM Spectra layer */
	private KeyHandler keyListener = null;
	
	public StampLView(StampFactory factory, StampLayer parent, StampLayerWrapper wrapper, LayerParameters lp, StampLView3D lview3d){
		this(parent, wrapper, false, lp, lview3d);
		originatingFactory = factory;
		if (lview3d!=null) {
			lview3d.setLView(this);
		}
	}
	
	public StampLView(StampFactory factory, StampLayer parent, StampLayerWrapper wrapper, LayerParameters lp) {
		this(parent, wrapper, false, lp, null);
		originatingFactory = factory;
	}
	
	public StampLView(StampLayer parent, StampLayerWrapper wrapper, boolean isChild, LayerParameters lp, LView3D lview3d)
	{
		super(parent, lview3d);
		
		layerParams = lp;

		javax.swing.ToolTipManager.sharedInstance().registerComponent(this);
		
		stampLayer = parent;
		
		myFocus = null;

		// This has to be done BEFORE the call to getFocusPanel, or some of the tabs don't get added
        this.wrapper = wrapper;
	    if (wrapper==null && !parent.getInstrument().equalsIgnoreCase("davinci")) {
	    	this.wrapper = new StampLayerWrapper(stampLayer.getSettings());
	    }

		if (!isChild) {
			updateSettings(false);
			getFocusPanel();
			
			// only the main view will have  stampSources
			if (!parent.globalShapes()) {
				StampServer.getInstance().createSources(stampLayer.getSettings());
			}
			
			stampLayer.addSelectionListener(this);
		} 
		
		// Outlines, Images, and Selections
        setBufferCount(3);
		
		MouseHandler mouseHandler = new MouseHandler();
	
		addMouseListener(mouseHandler);
		addMouseMotionListener(mouseHandler);
		
        if (stampLayer.getParam(stampLayer.PLOT_UNITS).length()>0) {
			// Add mouse listener for profile line updates
			profileLineMouseListener = new ProfileLineDrawingListener(this);
			addMouseListener(profileLineMouseListener);
			addMouseMotionListener(profileLineMouseListener);
			addKeyListener(profileLineMouseListener);
	
			profileLineCueingListener = new ProfileLineCueingListener(this);
			addMouseMotionListener(profileLineCueingListener);
		}
		
        //If it's a sharad layer, add the profile cue listener for line updates
        //Also add the full res listener.
        if (stampLayer.lineShapes()){
			profileLineCueingListener = new ProfileLineCueingListener(this);
			addMouseMotionListener(profileLineCueingListener);
			
			fullResHighlightListener = new FullResHighlightListener(this);
			addMouseMotionListener(fullResHighlightListener);
        }
        
        if (stampLayer.spectraPerPixel()) {
        	keyListener = new KeyHandler();
        	addKeyListener(keyListener);
        }
	}
		
	/**
	 * Something changed, so clear the cache in our stamp sources - eventually triggers an update to listeners
	 */
	public void clearStampSourceCache() {
		StampServer.getInstance().clearCache(getSettings());
	}
	
	public final String getName()
	{ 
		if (stampLayer != null && stampLayer.getSettings() != null && stampLayer.getSettings().getName() != null)
			return  stampLayer.getSettings().getName();
		return "Stamp Layer";
	}

	/**
	 * Override to handle special needs.
	 */
	public SerializedParameters getInitialLayerData()
	{
		return stampLayer.getSettings();
	}

	public List<StampFilter> getFilters() {
		if (wrapper==null) {
			List<StampFilter> blankList = new ArrayList<StampFilter>();
			return blankList;
		}
		return wrapper.getFilters();
	}
	
	public StampFocusPanel myFocus;

	public StampFocusPanel getFocusPanel()
	{
		// Do not create a focus panel for the panner!
		if (stampLayer==null) {
			return null;
		}
		
		// Do not create a focus panel for the panner!
		if (getParentLView()!=null) {
			focusPanel = myFocus = (StampFocusPanel)getParentLView().getFocusPanel();
		}

		if (focusPanel == null) {
			focusPanel = myFocus = new StampFocusPanel(StampLView.this, wrapper);
		}
				
		return (StampFocusPanel)focusPanel;
	}
	
	/**
	 * Returns the settings object from the stampLayer. The stampLayer is public, but still a 
	 * better way of getting the settings.
	 * @return StampLayerSettings
	 * 03/28/2012
	 */
	public StampLayerSettings getSettings() {
		return this.stampLayer.getSettings();
	}
	
	/**
	 * Override to update view specific settings
	 */
	protected synchronized void updateSettings(boolean saving)
	{
		// Don't save OR restore saved Davinci settings.
		if (stampLayer.getInstrument().equalsIgnoreCase("davinci")) {
			return;
		}
		
		if (saving) {
			log.println("saving settings");

			stampLayer.getSettings().setStampStateList(myFocus.getRenderedView().getStampStateList());
			
			FilteringColumnModel colModel = (FilteringColumnModel) myFocus.table.getColumnModel();
			
			Enumeration<TableColumn> colEnum=colModel.getColumns();
			
			String cols[] = new String[colModel.getColumnCount()];
			
			for (int i=0; i<cols.length; i++) {
				cols[i]=colEnum.nextElement().getHeaderValue().toString();
			}
			
			if (cols.length>0) {
				stampLayer.getSettings().setInitialColumns(cols);
			}
			viewSettings.put(VIEW_SETTINGS_KEY, stampLayer.getSettings());
		} else {
			log.println("loading settings");

			if ( viewSettings.containsKey(VIEW_SETTINGS_KEY) ) {
				stampLayer.setSettings((StampLayerSettings) viewSettings.get(VIEW_SETTINGS_KEY));
				if (stampLayer.getSettings() != null) {
					log.println("lookup of settings via key succeeded");
					if (myFocus.getRenderedView() != null)
					{
						// Reload and rerender filled stamps; requires that both MyFocus
						// and MyFilledStampFocus panels exist.
						if (stampLayer.getSettings().getStampStateList() != null && myFocus != null) {
							log.println("calling restoreStamps from updateSettings");
							restoreStamps(stampLayer.getSettings().getStampStateList());
						}
					}
					
					settingsLoaded = true;
				} else {
					log.println("lookup of settings via key failed");
					stampLayer.setSettings(new StampLayerSettings());
				}
			}
		}
	}
	
	/**
	 * Receive cueChanged events from chartView.
	 * @param worldCuePoint The new point within the profileLine segment boundaries
	 *        where the cue is to be generated.
	 */
	public void cueChanged(Point2D worldCuePoint) {
		profileLineCueingListener.setCuePoint(worldCuePoint);
	}

	public Shape getProfileLine() {
		return profileLine;
	}
	
	
	/**
	 * Used explicitly for updating the full resolution highlight
	 * on a selected sharad footprint
	 * @param highlightPath
	 */
	public void highlightChanged(ArrayList<Point2D> spPoints){
		fullResHighlightListener.setHighLightPoints(spPoints);
	}
	
	
	/**
	 * Sets line for which profile is to be extracted.
	 * @param newProfileLine Set the new line to be profiled to this line.
	 *        A null value may be passed as this argument to clear the profile line.
	 */
	public void setProfileLine(Shape newProfileLine) {
		profileLine = newProfileLine;
		
		if (focusPanel != null && myFocus != null){
			ChartView chartView = myFocus.getChartView();
			if (chartView!=null) {
				chartView.setProfileLine(profileLine, profileLine == null? 1: getProj().getPPD());
			}
		}
	}
    
	public void receiveData(Object layerData) {
		if (!isAlive()) {
			return;
		}

		if (layerData instanceof StampShape[]) {
				log.println("STARTED receiveData in "
					    + Thread.currentThread().getName());

				stamps = (StampShape[]) layerData;

				if (stampLayer.pointShapes()) {
	    			OutlineFocusPanel ofp = getOutlineFocusPanel();
	    			
	    			ofp.recalculateMinMaxValues();
				}
    			
				if (getChild()!=null) {
					if (myFocus != null) {
						myFocus.dataRefreshed();
					}
				}
				
				synchronized (this) {
					// Force a redraw of filled stamps
					lastRequest=null;
					
					// It is not necessary to redraw child view (if any)
					// in this context as both Main and Panner views
					// receive data separately for view changes, etc.
					redrawEverything(false);
				}
				
				log.println("STOPPED receiveData in "
				            + Thread.currentThread().getName());
				repaint();
		} else {
			log.aprintln("BAD DATA CLASS: " + layerData.getClass().getName());
		}
	}

    protected void viewChangedPost() {
        if (!isVisible()) {
            setDirty(true);
        }
    }
	
	public void redrawEverything(final boolean redrawChild) {
		if (stamps != null) {
			if (getChild() != null) {
	            drawFilled();
			}
			
            drawOutlines();
			
    		drawSelections(((StampLayer)getLayer()).getSelectedStamps(), true);
			
			if (redrawChild  &&  getChild() != null) {
				((StampLView) getChild()).redrawEverything(false);
			}
		}
	}
    
	/**
	 * @return a Runnable that calls the given runnable, and associates a
	 *         StampTask with the lifetime of the execution of the given
	 *         runnable
	 */
	private Runnable tasked(final Runnable r) {
		return new Runnable() {
			public void run() {
				StampTask task = ((StampLayer)getLayer()).startTask();
				try {
					task.updateStatus(Status.YELLOW);
					r.run();
				} finally {
					task.updateStatus(Status.DONE);
					repaint();
				}
			}
		};
	}
	
	/**
	 * Paints the component using the super's paintComponent(Graphics),
	 * followed by the profile-line drawing onto the on-screen graphics
	 * context.
	 */
	public synchronized void paintComponent(Graphics g) {
		// Don't try to draw unless the view is visible
		if (!isVisible() || viewman == null) {//@since remove viewman2
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
		
		
		//if this is a radar layer
		if(stampLayer.lineShapes()){
			//draw the full res highlight for sharad footprints
			if (fullResHighlightListener !=null){
				fullResHighlightListener.paintHighlight(g2, getProj().getProjection());
			}
			//draw any horizons on the sharad footprints if there are any
			RadarFocusPanel radarPnl = getFocusPanel().getRadarView();
			
			for(FilledStamp fs : getFocusPanel().getRenderedView().getFilled()){
				//TODO there might be a better way than this loop to recast?
				ArrayList<FilledStampRadarType> list = new ArrayList<FilledStampRadarType>();
				if(fs instanceof FilledStampRadarType){
					list.add((FilledStampRadarType)fs);
				}
				for(FilledStampRadarType fsr : list){
					for(RadarHorizon h : fsr.getHorizons()){
						//if the horizon isn't hidden, and if its color is being displayed in 2d 
						// (as determined in the settings focus panel tab and stored in the layer settings)
						if(h.isVisible() && getSettings().getHorizonColorDisplayMap().get(h.getColor())){
							//if this horizon is selected, color it differently
							if(h.equals(radarPnl.getSelectedHorizon())){
								g2.setColor(new Color(~h.getColor().getRGB()));
							}else{
								g2.setColor(h.getColor());
							}
							g2.setStroke(getProj().getWorldStroke(h.getLViewWidth()));
							g2.draw(h.getWorldPathForProj(getProj().getProjection()));
						}
					}
				}
			}
		}
	}
	
	/**
	 * @return The full res highlight listener which is used by
	 * Radar layers (lineshapes) to draw the full res highlight
	 * on the lview for context.  (Can be null)
	 */
	public FullResHighlightListener getFullResHighlightListener(){
		return fullResHighlightListener;
	}
		
	private volatile int drawOutlineSequence = 0, drawFilledSequence = 0, drawSelectionSequence = 0;
	private Object drawOutlineLock = new Object(), drawFilledLock = new Object(), drawSelLock = new Object();
	
	/**
	 * Draws stamp outlines in window. Outlines are only redrawn if the
	 * projection, outline colors, or the in-view stamp list have changed since
	 * the last drawing (or if being drawn for the first time). Otherwise,
	 * outlines are simply drawn to the screen with existing buffer contents.
	 */
	public void drawOutlines() {
		final int seq = ++drawOutlineSequence;
		
		// Nothing to draw, no work to do here
		if (stampLayer.globalShapes()) {
			return;
		}
		
		pool.execute(tasked(new Runnable() {
			public void run() {
				synchronized(drawOutlineLock) {
					
					// Check for various reasons to NOT draw the outlines
					
					// Reason 1: This is not the most recent sequence, ie there's already been another request to redraw, so don't bother
					if (seq != drawOutlineSequence) {
						return;
					}
					
					// Reason 2: We don't actually have any stamps to draw
					if (stamps==null || stamps.length<1) {
						return;
					}

					// Toggle the entire outline buffer on or off based on the user's selected setting.  May be the only thing that's changed
					setBufferVisible(StampLayer.OUTLINES_BUFFER, !stampLayer.getSettings().hideOutlines());

					// Reason 3: The user has selected the 'Hide Outlines' checkbox.  
					if (stampLayer.getSettings().hideOutlines()) {
						return;
					}
					
					// Collect some user settings that trigger redraws upon change
					Color unsColor = stampLayer.getSettings().getUnselectedStampColor();
					Color fillColor = stampLayer.getSettings().getFilledStampColor();
					
					// wind vectors
					double mag = stampLayer.getSettings().getMagnitude();
					double originMag = stampLayer.getSettings().getOriginMagnitude();
					Color originColor = stampLayer.getSettings().getOriginColor();
				
					boolean displayBoresight = stampLayer.getSettings().showBoresight();
					boolean drawAsRings = stampLayer.getSettings().drawAsRing();
					float ringWidth = stampLayer.getSettings().getRingWidth();
					
					// If all of these values are the same as they were last time, there's no need to redraw
		            if ((lastStamps == stamps) && (lastUnsColor == unsColor) &&
		                (projHash == Main.PO.getProjectionSpecialParameters().hashCode()) &&
		                (fillColor.equals(lastFillColor)) && (lastMag == mag) && (lastOriginColor == originColor) &&
		                (lastOriginMag == originMag) && (lastAlphaStamps == stamps) &&
		                (lastDisplayBoresight == displayBoresight) && (lastDrawAsRings == drawAsRings) &&
		                (lastRingWidth == ringWidth)) {
		                return;
		            }
		            		            	
		            // If we've made it this far, something's changed.  Clear the buffer so we can redraw fresh
	                clearOffScreen(StampLayer.OUTLINES_BUFFER);
	                
	                try {
		    			Graphics2D g2 = getOffScreenG2(StampLayer.OUTLINES_BUFFER);
		    			if (g2 == null)
		    				return;
		    			
						g2.setStroke(new BasicStroke(0));
						
		    			g2.setPaint(unsColor);
		    		
		    			if(stampLayer.vectorShapes() || stampLayer.pointShapes() || stampLayer.spectraData()){
			    			// Custom color code
			    			OutlineFocusPanel ofp = getOutlineFocusPanel();
			    						    			
			    			double min = 0;
			    			double max = 0;	
			    			int columnToColor = -1;				    			
		    				Color colors[] = new Color[0];
		    				
		    				// TODO: Silly - vector shapes don't have colors the same way as others.  Logic should likely be refactored
		    				if (ofp!=null && !stampLayer.vectorShapes()) {
		    					min = ofp.getMinValue();
				    			max = ofp.getMaxValue();
		    					columnToColor=ofp.getColorColumn();
		    					colors=ofp.getColorMap();
		    				}
			    			// End custom color code
		    				
							
			    			int ppd=viewman.getZoomManager().getZoomPPD();
			    			
			    			// May or may not apply to outlines
							if (stampLayer.vectorShapes()) {   // Such as Wind
								double userScale = stampLayer.getSettings().getOriginMagnitude();
				    			int factor=(int)(Math.log(ppd)/Math.log(2));
								double degrees = 0.1*(32.0/ppd)*(factor/4.0)*userScale; 
	
	
				    			for (int i=0; i<stamps.length; i++) {
									if (seq != drawOutlineSequence) {
										return;
									}
	
									Point2D p=((WindShape)stamps[i]).getOrigin();
				    				Ellipse2D oval = new Ellipse2D.Double(p.getX()-degrees, (p.getY()-degrees), 2*degrees, 2*degrees);
	
				    				g2.setPaint(originColor);
				    				g2.fill(oval);
				    									    				
					    			g2.setPaint(unsColor);
							    	List<GeneralPath> stampPaths = stamps[i].getPath(Main.PO);
							    			
							    	for (GeneralPath path : stampPaths) {
							    		g2.draw(path);
							    	}
				    				
									if (i % 1000 == 999) {
										repaint();
									}										
				    			}
							} else if (stampLayer.pointShapes()) {   // Such as MOLA Shots
								// Currently only Widmer Stamps are fixed spot size.
								if (stampLayer.fixedSpotSize()) {
									// FIXED_SPOT_SIZE is specified in km
									double radius = Double.parseDouble(stampLayer.getParam(StampLayer.FIXED_SPOT_SIZE));
									
					    			for (int i=0; i<stamps.length; i++) {
										if (seq != drawOutlineSequence) {
											return;
										}
		
										// For PointShapes, the points in the stamp object are just a single lon,lat value
										Point2D p=  new Point2D.Double(-stamps[i].getStamp().getPoints()[0],stamps[i].getStamp().getPoints()[1]); 
										
										Object colorValObj = stamps[i].getStamp().getData()[columnToColor];
										
										if (colorValObj instanceof Color) {
											g2.setPaint((Color)colorValObj);
										} else {
						    				Color color = stamps[i].getCalculatedColor();
						    							
						    				g2.setPaint(color);
										}
					    				g2.fill(StampUtil.getProjectedShape(p.getX(), p.getY(), radius, Main.PO));
					    				
										if (i % 1000 == 999) {
											repaint();
										}
					    			}								
								} else {
									// User scale and spot size now calculated internal to getFillAreas on PointShape
					    			for (int i=0; i<stamps.length; i++) {
										if (seq != drawOutlineSequence) {
											return;
										}

					    				Color color = stamps[i].getCalculatedColor();

										g2.setPaint(color);

					    				List<Area> stampPaths = stamps[i].getFillAreas();
								    	
								    	for (Area path : stampPaths) {
								    		g2.fill(path);
								    	}
					    				
										if (i % 1000 == 999) {
											repaint();
										}
					    			}									
								}
							} else if (stampLayer.spectraData()) { // Such as TES
								// Get a clone of the stamps to avoid future multi-threading issues					  
								StampShape stampsCopy[] = stamps.clone();
								
								StampGroupComparator orderSort = ofp.getOrderSort();

								Arrays.sort(stampsCopy, orderSort);
								
				    			for (int i=0; i<stampsCopy.length; i++) {
									if (seq != drawOutlineSequence) {
										return;
									}
									
									if (stampsCopy[i].isHidden) continue;
													    							    									    								    				
				    				Color color = stampsCopy[i].getCalculatedColor();
				    				if (color==null) continue;
	
									// Apply any transparency specified within the layer itself
				    				int alphaVal = stampLayer.getSettings().getFilledStampColor().getAlpha();
				    				g2.setColor(new Color((alphaVal<<24) | color.getRGB() & 0xFFFFFF, true));
							    	
				    				// Generic reference, for boresights later
				    				List<?> paths = new ArrayList<Shape>();
				    				
									if (drawAsRings) {
										List<GeneralPath> stampPaths = stampsCopy[i].getPath(Main.PO);
										paths=stampPaths;

										g2.setStroke(new BasicStroke(stampLayer.getSettings().getRingWidth()));
										
								    	for (GeneralPath path : stampPaths) {
								    		g2.draw(path);
								    	}
										
									} else {
										List<Area> stampPaths = stampsCopy[i].getFillAreas();
										paths=stampPaths;
										
								    	for (Area path : stampPaths) {
								    		g2.fill(path);
								    	}
								    }
							    	
							    	// Boresight
							    	Point2D bp = stampsCopy[i].getBoresight();
							    	if (displayBoresight) {
							    		StampUtil.drawBoresight(g2, Main.PO, bp, color, paths, false);
							    	}
				    							    				
									if (i % 1000 == 999) {
										repaint();
									}
				    			}
							}
						} else {  // Normal stamp outlines
			    			for (int i=0; i<stamps.length; i++) {
								if (seq != drawOutlineSequence) {
									return;
								}

								if (stampLayer.getSettings().getFilledStampColor().getAlpha() != 0) {
									g2.setPaint(fillColor);

				    				List<Area> stampPaths = stamps[i].getFillAreas();
							    	
							    	for (Area path : stampPaths) {
							    		g2.fill(path);
							    	}
								}								
								
				    			g2.setPaint(unsColor);
			    				List<GeneralPath> stampPaths = stamps[i].getPath(Main.PO);
						    	
						    	for (GeneralPath path : stampPaths) {
						    		g2.draw(path);
						    	}
						    	
						    	Point2D bp = stamps[i].getBoresight();
						    	if (displayBoresight) {
						    		StampUtil.drawBoresight(g2, Main.PO, bp, unsColor, stampPaths, false);
						    	}

								if (i % 1000 == 999) {
									repaint();
								}
			    			}
						}
		    							                
		                lastStamps = stamps;
		                lastUnsColor = unsColor;
		                projHash = Main.PO.getProjectionSpecialParameters().hashCode();
						lastFillColor = fillColor;
						lastAlphaStamps = stamps;
		                lastMag = mag;
		                lastOriginColor = originColor;
		                lastOriginMag = originMag;
		                lastDisplayBoresight = displayBoresight;
		                lastDrawAsRings = drawAsRings;
		                lastRingWidth = ringWidth;
		            } finally {
						repaint();
	                }
	            }
			}
		}));
	}
		
	/** Forces drawFilled to rerender filled stamps. */
	public void clearLastFilled() {
		lastRequest = null;
	}
	
	/** Forces drawOutlines to redraw stamp outlines.  Might be a better way to do this. */
	public void clearLastOutlines() {
		lastUnsColor = null;
	}
    /**
     * Draws specified list of filled stamps to the primary buffer
     * of the stamp view's window.  Does not otherwise alter the
     * state of the buffer, so it may be used to overlay filled
     * stamps to the existing drawing buffer contents.
     * <p>
     * NOTE:  Because of the above functionality, the caller must
     * clear the drawing buffer contents if it is desired that the
     * specified filled stamps be the only ones displayed and/or
     * to be certain of a pristine state.
     * 
     * @return Returns <code>true</code> if drawing of stamps completed
     * without interruption because of current redraw thread becoming
     * stale (i.e., stale call to receiveData()) or if there were no
     * filled stamps specified; returns <code>false</code> if drawing of 
     * stamps was interrupted or if there was some internal error.
     */
	private void drawFilled() {
		final int seq  = ++drawFilledSequence;
		pool.execute(tasked(new Runnable() {
			public void run() {
				synchronized(drawFilledLock) {
					if (seq != drawFilledSequence) {
						return;
					}
					
					int renderPPD = viewman.getZoomManager().getZoomPPD();
					DrawFilledRequest request = new StampRequest(seq, getProj(), renderPPD);
					List<FilledStamp> filledStamps = getFilteredFilledStamps();
					
					if (!request.equals(lastRequest) || !filledStamps.equals(lastFilledStamps)) {
						Graphics2D g2 = getOffScreenG2(StampLayer.IMAGES_BUFFER);
						if (g2 != null) {
							if (stampLayer.globalShapes()) {
								
								if (filledStamps.size()==0) {
									clearOffScreen(StampLayer.IMAGES_BUFFER);
									return;
								}
								
								for (FilledStamp fs : filledStamps) {
									// TODO: Only render one visible?  Does it make sense to render them all if they're all global?
									String idStr = fs.stamp.getId();
									String instrument = stampLayer.getInstrument();
									String urlStr = "ImageServer?instrument="+instrument+"&id="+idStr+"&zoom=100";
									
									String projStr = Main.PO.getCenterLon()+":"+Main.PO.getCenterLat();
									
									String cacheProjStr = instrument + "_"+idStr+":"+projStr;
									
									boolean shift180=stampLayer.shift180();
									
									BufferedImage img = StampCache.readProj(cacheProjStr, true);
									if (img==null) {
										img = StampImageFactory.getImage(urlStr, true);
										if (img==null) return;
										
										GlobalDataReprojecter cachedProjecter = new GlobalDataReprojecter(img.getWidth(), img.getHeight(), shift180);
										
										img = cachedProjecter.getProjectedImage(img, fs.pdsi.isNumeric, Main.PO);
	
										StampCache.writeProj(img, cacheProjStr);
	
									}

									if (img!=null) {
										Rectangle2D worldClip = new Rectangle2D.Double();
										
										worldClip.setFrame(-180,-90, 360,180);
																						
										if (fs.pdsi.isNumeric) {
											try {
												BufferedImage image3 = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);

												// Ignore all black pixels for numeric stamp data
												Graphics2D g3 = image3.createGraphics();
												g3.setComposite(new IgnoreComposite(Color.black));
												
												// Filter using the FloatingPointOp first
								                FloatingPointImageOp op2 = new FloatingPointImageOp(fs.pdsi);														
												g3.drawImage(img, op2, 0, 0);
												
												BufferedImageOp op = ((FilledStampImageType)fs).getColorMapOp(image3).forAlpha(1);

												//perform the filter transform
												op.filter(image3, image3);
												
												//get the original numeric data
												WritableRaster numVals = img.getRaster();
												
												//get the new alpha raster
												WritableRaster alpha = image3.getAlphaRaster();
												
												//replace any transparent values from the src one
												for (int j = image3.getHeight()-1; j>=0; j--) {
													for (int i = image3.getWidth()-1; i>=0; i--) {

														int[] alphaVal = new int[1];
														//get the numeric value from the original image
														double orgVal = numVals.getSampleDouble(i, j, 0);
														
														//if it's NaN or IGNORE_VALUE, then it's supposed to be transparent
														if(Double.isNaN(orgVal) || orgVal == fs.pdsi.IGNORE_VALUE || orgVal == fs.pdsi.ignore_value){
															alphaVal[0] = 0;
															//reset the alpha value
															alpha.setPixel(i, j, alphaVal);
														}
													}
												}
												
												img = image3;
											} catch (Exception e) {
												e.printStackTrace();
											}										
										} else {
											BufferedImage tmpInputImage = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
									    	tmpInputImage.createGraphics().drawImage(img, null,  0, 0);
									    	img = tmpInputImage;
	
											BufferedImageOp op = ((FilledStampImageType)fs).getColorMapOp(img).forAlpha(1);
	
											//perform the filter transform
											op.filter(img, img);
										}
										
										if (filledStamps.size()==1) {
											clearOffScreen(StampLayer.IMAGES_BUFFER);
										}
										g2.drawImage(img, Util.image2world(img.getWidth(), img.getHeight(), worldClip), null);
								
										//draw the image one more time shifted by 360 because the graphics wrapped doesn't
										// seem to draw enough times (sometimes it will end at the prime meridian)
										// So, one more instance of the drawing ensures it always covers the screen
										worldClip.setFrame(180, -90, 360, 180);
										g2.drawImage(img, Util.image2world(img.getWidth(), img.getHeight(), worldClip), null);
									}
									break;
								}
								
								if (seq != drawFilledSequence) {
									lastRequest = request;
									lastFilledStamps = filledStamps;
								}
							} else {
								clearOffScreen(StampLayer.IMAGES_BUFFER);
								
								doRender(request, filledStamps, new RenderBackBufferProgress(renderPPD), g2, null);
							}
						}
					}
					
					if (seq != drawFilledSequence) {
						lastRequest = request;
						lastFilledStamps = filledStamps;
					}
					
					// if the request did not change, then we are here because of a settings change, and must
					// flush any StampSources' cache.
					if (lastRequest == null || lastRequest.equals(request)) {
						clearStampSourceCache();
					}
				}
			}
		}));
	}
	
	/** Gets all stamps that should be displayed */
	public List<FilledStamp> getFilteredFilledStamps() {
		final List<FilledStamp> allFilledStamps;
		
		if (stampLayer.getSettings().renderSelectedOnly()) {
			allFilledStamps = myFocus.getRenderedView().getFilledSelections();
		} else {
			allFilledStamps = myFocus.getRenderedView().getFilled();
		}
		
		// Filter out any stamps that aren't in the view
		List<FilledStamp> filtered = new ArrayList<FilledStamp>();
		for (FilledStamp fs : allFilledStamps) {
			for (int i=0; i<stamps.length; i++) {
				if (fs.stamp.getId().equalsIgnoreCase(stamps[i].getId())) {
					filtered.add(fs);
				}
			}
		}
		
		return filtered;
	}
	
	/**
	 * Draw the given stamps to the given Graphics2D, using the given request
	 * and the interrupted bit of the current thread to check for interruption,
	 * and reporting the progress of rendering to the given RenderProgress.
	 */
	public void doRender(final DrawFilledRequest request, final List<FilledStamp> stamps, final RenderProgress progress, final Graphics2D g2, BufferedImage target) {
		int repaintCount = 0;
		int totalCount = stamps.size();
		try {
			List<StampImage> higherStamps=new ArrayList<StampImage>();
			for (FilledStamp fs : stamps) {
				fs.pdsi.calculateCurrentClip(higherStamps);
				higherStamps.add(fs.pdsi);
			}
			// Draw in reverse order so the top of the list is drawn on top
			for (int i = totalCount - 1; i >= 0; i--) {
				if (request.changed()) {
					return;
				}
				if(stamps.get(i) instanceof FilledStampImageType){
					FilledStampImageType fs = (FilledStampImageType)stamps.get(i);
					Graphics2D stampG2 = (Graphics2D) g2.create();
					try {
						Point2D offset = fs.getOffset();
						stampG2.translate(offset.getX(), offset.getY());
						
						fs.pdsi.renderImage(stampG2, fs, request, stampLayer.startTask(), offset, target, Main.PO, request.getPPD());
						progress.update(repaintCount++, totalCount);
					} finally {
						stampG2.dispose();
					}
				}
			}
		} finally {
			progress.update(totalCount, totalCount);
		}
	}
	
	/**
	 * Translates progress updates into StampLView#repaint() calls at some
	 * interval, and when the last update is received.
	 */
	protected final class RenderBackBufferProgress implements RenderProgress {
		private final long repaintThreshold;
		public RenderBackBufferProgress(int renderPPD) {
			repaintThreshold = Math.round(Math.min(STAMP_RENDER_REPAINT_COUNT_MAX,
				Math.max(STAMP_RENDER_REPAINT_COUNT_MIN,
					STAMP_RENDER_REPAINT_COUNT_BASE -
					Math.log(renderPPD) / Math.log(2))));
			log.println("Repainting every " + repaintThreshold + " images");
		}
		public void update(int current, int max) {
			if (current == max || (current % repaintThreshold) == 0)
				repaint();
		}
	}
	
	public String toString() {
		return getName();
	}
		
	private String buildTypeLookupData() {
		String idList="";
		for (StampShape stamp : stampLayer.getSelectedStamps()) {
			idList+=stamp.getId()+",";
		}
		
		if (idList.endsWith(",")) {
			idList=idList.substring(0,idList.length()-1);
		}
		
		String data = "id="+idList+"&instrument="+stampLayer.getInstrument()+"&format=JAVA";
		
		return data;
	}

	private List<String> getImageTypes() {
		List<String> imageTypes = new ArrayList<String>();

		try {						
			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer("ImageTypeLookup", buildTypeLookupData()));

			List<String> supportedTypes = (List<String>)ois.readObject();

			ois.close();

			boolean btr=false;
			boolean abr=false;
	
			for (String type : supportedTypes) {
				if (type.equalsIgnoreCase("BTR")) {
					btr=true;
				} else if (type.equalsIgnoreCase("ABR")) {
					abr=true;
				} 
				
				imageTypes.add(type);								
			}
			
			if (abr && btr) {
				imageTypes.add(0, "ABR / BTR");
			}

			
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		
		return imageTypes;
	}
	
	protected Component[] getContextMenuTop(final Point2D worldPt)	{
		final List<Component> newItems =
			new ArrayList<Component>( Arrays.asList(super.getContextMenuTop(worldPt)) );

		if (viewman.getActiveLView().equals(this) || viewman.getActiveLView().equals(getChild()) ){
			if (!stampLayer.globalShapes() && stampLayer.enableWeb()) {  // Disable the view page from the MainView if we're working with global stamps
				// See what the user clicked on... leave menu unchanged if nothing.
				final List<StampShape> list = findStampsByWorldPt(worldPt);
				if (list == null)
					return null;
		
				if (list.size()>0) {
				
					JMenu viewMenu = new JMenu("View " + stampLayer.getInstrument() + " Stamps");
					
					for (final StampShape stamp : list)
					{
						StampMenu sub = new StampMenu(stampLayer, myFocus.getRenderedView(), stamp);
										
						viewMenu.add(sub);
					}
		
					newItems.add(0, viewMenu);
					
					JMenuItem selectUnderlying = new JMenuItem("Select Stamps");
					selectUnderlying.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e) {
							final JDialog selectWindow = new JDialog(Main.mainFrame, true);
							selectWindow.setSize(200,300);
							selectWindow.setTitle("Select Stamps");
							Point2D screenPoint = getProj().world.toScreen(worldPt);
							int x = (int)screenPoint.getX()+(int)Main.testDriver.mainWindow.getLocationOnScreen().getX();
							selectWindow.setLocation(x, (int)screenPoint.getY());
							
							final JList selectList = new JList(list.toArray());
							selectList.addListSelectionListener(new ListSelectionListener() {
								public void valueChanged(ListSelectionEvent e) {
									List add = new ArrayList<StampShape>();
									List remove = new ArrayList<StampShape>();
									
									int ids[]=selectList.getSelectedIndices();
									for (int i=0; i<list.size(); i++) {
										if (selectList.isSelectedIndex(i)) {
											add.add(list.get(i));
										} else {
											remove.add(list.get(i));
										}
									}
									
									stampLayer.addAndRemoveSelectedStamps(add, remove);
								}
							});
							
							JButton dismiss = new JButton("Done selecting stamps".toUpperCase());
							dismiss.addActionListener(new ActionListener() {
								public void actionPerformed(ActionEvent e) {
									selectWindow.setVisible(false);
								}
							});
							
							JScrollPane selectSp = new JScrollPane(selectList);
							
							selectWindow.setLayout(new BorderLayout());
							selectWindow.add(selectSp, BorderLayout.CENTER);
							JPanel buttonPanel = new JPanel();
							buttonPanel.setLayout(new FlowLayout());
							buttonPanel.add(dismiss);
							selectWindow.add(buttonPanel, BorderLayout.SOUTH);
							selectWindow.setVisible(true);
						}
					});
					
					newItems.add(selectUnderlying);
				}
			}
			// Check for selected stamps and offer option of loading/rendering these.
			final int[] rowSelections = myFocus.getSelectedRows();
			if (rowSelections != null && rowSelections.length > 0) {
				if (stampLayer.enableRender()) {
					StampMenu renderSelectedMenu = new StampMenu(stampLayer, myFocus.getRenderedView());
					
					newItems.add(renderSelectedMenu);
				}
							
			    JMenuItem copySelected = new JMenuItem("Copy Selected " + stampLayer.getInstrument() + " Stamps to Clipboard");
			
			    copySelected.addActionListener(new ActionListener() {			
					public void actionPerformed(ActionEvent e) {
						StringBuffer buf = new StringBuffer();
						
					    for (StampShape stamp : stampLayer.getSelectedStamps()) {
					    	buf.append( stamp.getId() );
					        buf.append('\n');
					    }
					       
					    StringSelection sel = new StringSelection(buf.toString());
					    Clipboard clipboard = ValidClipboard.getValidClipboard();
					    if (clipboard == null)
					        log.aprintln("no clipboard available");
					    else {
					        clipboard.setContents(sel, sel);
					        Main.setStatus("Stamp list copied to clipboard");
					           
					        log.println("stamp list copied: " + buf.toString());
					    }
					}			
				});
			    
			    newItems.add(copySelected);
	
			    Set<String> layerTypes=StampFactory.getLayerTypes();
			    
			    JMenu overlapMenu = new JMenu("Find stamps intersecting selected " + stampLayer.getInstrument() + " stamps");
			    
			    newItems.add(overlapMenu);
			    
			    if (!stampLayer.globalShapes()) {
			    	newItems.add(overlapMenu);
			    }
			    
			    for(final String type : layerTypes) {
					JMenuItem findOverlaps= new JMenuItem("Find intersecting " + type + " stamps");
					findOverlaps.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e) {
							int maxIntersections = Config.get("stamps.max_intersections", 1000);
							List<StampShape> selectedStamps = stampLayer.getSelectedStamps();
							
							if (selectedStamps.size()>maxIntersections) {
							    Util.showMessageDialog(
					                    "Sorry - you selected " + selectedStamps.size() + " stamps, but stamp intersection is limited to a maximum of " + maxIntersections + " stamps.  " +
					                    		"Please reduce the number of selections and try again.",
					                    "JMARS",
					                    JOptionPane.INFORMATION_MESSAGE);	
								return;
							}
							ArrayList<GeneralPath> allStampPaths = new ArrayList<GeneralPath>();
							ArrayList<String> allStampIds = new ArrayList<String>();
							
							for (StampShape s : selectedStamps) {
									// TODO: Does this work at all correctly?  Probably not.  Path is no longer necessarily closed polygons,
									// But rather segments.  Somehow this should be done better....  Either return world coordinates in one giant chunk,
									// or somehow pass spatial coordinates in one giant chunk.  Fix!!
									allStampPaths.addAll(s.getPath(Main.PO));
								allStampIds.add(s.getId());
							}
	
							StampFactory.createOverlappingStampLayer(type, allStampIds, allStampPaths);
						}
					});		 
					overlapMenu.add(findOverlaps);
			    }			
				}
			    
			    if (stampLayer.spectraData()) {
					JMenuItem lockSelectedRecords = new JMenuItem("Lock Selected Records");
					lockSelectedRecords.addActionListener(new ActionListener() {						
						public void actionPerformed(ActionEvent e) {
							int cnt = 0;
							
							for (StampShape stamp : stampLayer.getSelectedStamps()) {
								if (!stamp.isLocked()) {
									stamp.setLocked(true);
									cnt++;
								}
							}
							Main.setStatus("Added " + cnt+ " stamp records to the lock list " +cnt);
							updateSelections();
						}						
					});
					newItems.add(lockSelectedRecords);						
					
					JMenuItem unlockSelectedRecords = new JMenuItem("Unlock Selected Records");
					unlockSelectedRecords.addActionListener(new ActionListener() {						
						public void actionPerformed(ActionEvent e) {
							int cnt = 0;
							
							for (StampShape stamp : stampLayer.getSelectedStamps()) {
								if (stamp.isLocked()) {
									stamp.setLocked(false);
									cnt++;
								}
							}
							Main.setStatus("Removed " + cnt+ " stamp records from the lock list " +cnt);
							updateSelections();
						}						
					});
					newItems.add(unlockSelectedRecords);						
	
					JMenuItem hideSelectedRecords = new JMenuItem("Hide Selected Records");
					hideSelectedRecords.addActionListener(new ActionListener() {						
						public void actionPerformed(ActionEvent e) {
							int cnt = 0;
							
							for (StampShape stamp : stampLayer.getSelectedStamps()) {
								if (!stamp.isHidden()) {
									stamp.setHidden(true);
									cnt++;
								}
							}
							Main.setStatus("Added " + cnt+ " stamp records to the hide list " +cnt);
							updateSelections();
						}						
					});
					newItems.add(hideSelectedRecords);						
					
					JMenuItem showSelectedRecords = new JMenuItem("Show Selected Records");
					showSelectedRecords.addActionListener(new ActionListener() {						
						public void actionPerformed(ActionEvent e) {
							int cnt = 0;
							
							for (StampShape stamp : stampLayer.getSelectedStamps()) {
								if (stamp.isHidden()) {
									stamp.setHidden(false);
									cnt++;
								}
							}
							Main.setStatus("Removed " + cnt+ " stamp records from the hide list " +cnt);
							updateSelections();
						}						
					});
					newItems.add(showSelectedRecords);						
	
					boolean allLocked = true;
					boolean allUnlocked = true;
					boolean allHidden = true;
					boolean allShown = true;
					
					for (StampShape stamp : stampLayer.getSelectedStamps()) {
						if (!stamp.isLocked()) {
							allLocked=false;
						}
						if (stamp.isLocked()) {
							allUnlocked=false;
						}
						if (stamp.isHidden()) {
							allShown=false;
						}
						if (!stamp.isHidden()) {
							allHidden=false;
						}
					}
					
					lockSelectedRecords.setEnabled(!allLocked);
					unlockSelectedRecords.setEnabled(!allUnlocked);
					hideSelectedRecords.setEnabled(!allHidden);
					showSelectedRecords.setEnabled(!allShown);
				}
				
			    JMenuItem makeShapesMenu = new JMenuItem("Convert selected stamps to a shape layer");
			    makeShapesMenu.addActionListener(new ActionListener() {				
					public void actionPerformed(ActionEvent e) {
						List<StampShape> selectedStamps = stampLayer.getSelectedStamps();
						
						LayerParameters lp = new LayerParameters(ShapeLayer.CUSTOM_SHAPE_NAME, "", "", "", false, "shape", null, null);
						ShapeLView shpLView = (ShapeLView)new ShapeFactory().newInstance(false, lp);
						ShapeLayer shpLayer = (ShapeLayer)shpLView.getLayer();
						
						//
						String instrument = stampLayer.getInstrument();
						FeatureProvider fp = new FeatureProviderStamp(selectedStamps, instrument);
				    	final List<ShapeLayer.LoadData> sources = new ArrayList<ShapeLayer.LoadData>();
				    	sources.add(new ShapeLayer.LoadData(fp,null));
				    	
						shpLayer.loadSources(sources);
						
						///
											
						LManager.getLManager().receiveNewLView(shpLView);
						LManager.getLManager().repaint();
						///
					}
				});
			    
			    if (!stampLayer.globalShapes()) {
			    	newItems.add(makeShapesMenu);
			    }
		}
		return  (Component[]) newItems.toArray(new Component[0]);
	}

	public OutlineFocusPanel getOutlineFocusPanel() {
		OutlineFocusPanel ofp = null;
		
		if (getChild()==null) {
			ofp = ((StampFocusPanel)getParentLView().focusPanel).outlinePanel;
		} else {
			ofp = ((StampFocusPanel)focusPanel).outlinePanel;
		}
		
		return ofp;
	}
	
	/**
	 * This is a convenience method used to redraw selected items when something has been set hidden.  Also recalculates the
	 * min/max values for colorization purposes.
	 */
	public void updateSelections() {
		stampLayer.increaseStateId(StampLayer.OUTLINES_BUFFER);
		stampLayer.increaseStateId(StampLayer.IMAGES_BUFFER);
		stampLayer.increaseStateId(StampLayer.SELECTIONS_BUFFER);
		getOutlineFocusPanel().recalculateMinMaxValues();
		clearLastOutlines();
		drawOutlines();
		drawSelections(stampLayer.getSelectedStamps(), true);
		focusPanel.repaint();
	
		if (ThreeDManager.isReady()) {
			//update the 3d view if has lview3d enabled
			if(getLView3D().isEnabled()){
				ThreeDManager mgr = ThreeDManager.getInstance();
				//If the 3d is already visible, update it
				if(getLView3D().isVisible()){
					mgr.updateDecalsForLView(this, true);
				}
			}
		}
	}

	public String getToolTipText(final MouseEvent event)
	{
		// Don't do this for the panner
 		if (getChild()==null) return null;
 		
		MultiProjection proj = getProj();
		
		if (proj == null) return null;

		Point2D screenPt = event.getPoint();
		Point2D worldPoint = proj.screen.toWorld(screenPt);
			
		StampShape stamp = findStampByWorldPt(worldPoint);

		if (stamp==null) return null;
		
		if (stampLayer.spectraPerPixel()) {
			Point p = new Point(event.getX(), event.getY());

			int renderPPD = viewman.getZoomManager().getZoomPPD();
			
			List<FilledStamp> filledStamps = getFilteredFilledStamps();

			List<StampShape> stampsUnderCursor = findStampsByWorldPt(worldPoint);
			
			for (StampShape ss : stampsUnderCursor){
				for (FilledStamp fs : filledStamps){
					if (fs instanceof FilledStampImageType && (ss.getId().equalsIgnoreCase(fs.stamp.getId()))){	
						HVector vector = screenPointToHVector(p, (FilledStampImageType)fs);	
						
						double data = fs.pdsi.getFloatVal(vector, Main.PO, renderPPD);
						
						short sample = (short)((int)data);
						short line = (short)((int)data>>16);
						
						return "Line: " + line + "  Sample: " + sample;
					}			
				}
			}
		}
		
    	StringBuffer buf = new StringBuffer();
    	
    	// We'll show the stamp ID as a tooltip whether or not the stamp has been rendered	
        buf.append("<html>");
        
        buf.append(stamp.getTooltipText());
         
        buf.append("</html>");
        return buf.toString();
	}
		    	    
	// Used for populating Chart samples
    // samplePoint is in worldCoordinates
	public double getValueAtPoint(Point2D samplePoint){
		MultiProjection proj = getProj();
		if (proj == null) {
			return Double.NaN;
		}
		
		Point2D screenPt=proj.getWorldToScreen().transform(samplePoint, null);
		
		///
		Point2D worldPoint = proj.screen.toWorld(screenPt);
		
		// Get a list of stamp shapes under the cursor		
		List<StampShape> stamps = findStampsByWorldPt(worldPoint);
		if (stamps==null) return Double.NaN;

		List<FilledStamp> filledStamps = getFilteredFilledStamps();
		
		// For each of the stampShapes, get their individual investigateData 
		// and add that to the invData object	
		stampShape: for (StampShape ss : stamps){
			for (FilledStamp fs : filledStamps){
				if (fs.stamp == ss){					

					boolean isNumeric = fs.pdsi.isNumeric;

					if (isNumeric) {						
						try {
							HVector vector = screenPointToHVector(screenPt, (FilledStampImageType)fs);	
												
							double val = fs.pdsi.getFloatVal(vector, proj.getProjection(), viewman.getZoomManager().getZoomPPD());
							
							if (val == StampImage.IGNORE_VALUE) {
								// The value for this stamp is transparent... but another image could still have valid data at this location
								continue stampShape;
							}
							
							return val;
//							return fs.pdsi.getFloatVal(vector);
						}catch (Exception e) {
							e.printStackTrace();
						}
					}
					
					continue stampShape;
				}
			}
		}
		
		return Double.NaN;
	}
		
	public void browse(StampShape stamp, int num)
	{
		String url = null;
		
		try {
			String browseLookupStr = "BrowseLookup?id="+stamp.getId()+"&instrument="+stampLayer.getInstrument()+"&format=JAVA";
					
			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(browseLookupStr));
			
			// Get the num-th URL
			for (int i=0; i<num; i++) {
				url = (String)ois.readObject();
			}
			
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (url == null) {
			Util.showMessageDialog("Sorry - that browse page is not currently available",
                    "JMARS",
                    JOptionPane.INFORMATION_MESSAGE);			
			return;
		}

    		Util.launchBrowser(url);
	}

	public void quickView(StampShape stamp)
	{
		String url = null;
		
		try {
			String browseLookupStr = "BrowseLookup?id="+stamp.getId()+"&instrument="+stampLayer.getInstrument()+"&format=JAVA";
					
			ObjectInputStream ois = new ObjectInputStream(StampLayer.queryServer(browseLookupStr));
			
			url = (String)ois.readObject();
			url = (String)ois.readObject();				
            
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		if (url == null)
			Util.showMessageDialog("Can't determine URL for stamp: "
		                                  + stamp.getId(),
		                                  "JMARS",
		                                  JOptionPane.INFORMATION_MESSAGE);
        
    		Util.launchBrowser(url);
	}


	public List<StampShape> findStampsByWorldPt(Point2D worldPt)
	{
		if (!isVisible() || viewman == null) {//@since remove viewman2 
			log.aprintln("view manager not available");
			return null;
		}

		MultiProjection proj = viewman.getProj();
		if (proj == null) {
			log.aprintln("null projection");
			return null;
		}

		Dimension2D pixelSize = proj.getPixelSize();
		if (pixelSize == null) {
			log.aprintln("no pixel size");
			return null;
		}

		double w = pixelSize.getWidth();
		double h = pixelSize.getHeight();
		double x = worldPt.getX() - w/2;
		double y = worldPt.getY() - h/2;

		return findStampsByWorldRect(new Rectangle2D.Double(x, y, w, h));
	}

	public List<StampShape> findStampsByWorldRect(Rectangle2D proximity) {
		return findStampsByWorldRect(proximity, Main.PO);
	}
	
	public List<StampShape> findStampsByWorldRect(Rectangle2D proximity, ProjObj proj)
	{
		if (stamps == null || proximity == null)
			return null;

		List<StampShape> list = new ArrayList<StampShape>();
		double w = proximity.getWidth();
		double h = proximity.getHeight();
		double x = proximity.getX();
		double y = proximity.getY();

		x -= Math.floor(x/360.0) * 360.0;

		Rectangle2D proximity1 = new Rectangle2D.Double(x, y, w, h);
		Rectangle2D proximity2 = null;
		log.println("proximity1 = " + proximity1);

		// Handle the two cases involving x-coordinate going past
		// 360 degrees:
		// Proximity rectangle extends past 360...
		if (proximity1.getMaxX() >= 360) {
			proximity2 = new Rectangle2D.Double(x-360, y, w, h);
			log.println("proximity2 = " + proximity2);
		}
		// Normalized stamp extends past 360 but
		// proximity rectangle does not...
		else if (proximity1.getMaxX() <= 180) {
			proximity2 = new Rectangle2D.Double(x+360, y, w, h);
			log.println("proximity2 = " + proximity2);
		}

		StampShape clonedStamps[] = stamps.clone();
		
		Arrays.sort(clonedStamps, getOutlineFocusPanel().getOrderSort());
		
		// Perform multiple proximity tests at the same time
		// to avoid re-sorting resulting stamp list.
		for (int i=0; i<clonedStamps.length; i++) {
			if (clonedStamps[i].isHidden()) continue;
			
			// Global stamps automatically match
			if (stampLayer.globalShapes()) {
				list.add(clonedStamps[i]);
				continue;
			}
			
			List<Area> stampAreas = clonedStamps[i].getFillAreas();
			
			shapeloop: for (Area shapeArea : stampAreas) {
			Rectangle2D stampBounds = shapeArea.getBounds2D();
			
				// getBounds2D for a line can return a 0 height for a horizontal line or a 0 width for a veritical line.  Either case will result in
				// Rectangle2D.intersect returning false no matter what.  To get around this, we make sure the height and width are each at least 1.
				if (stampBounds.getHeight()==0) stampBounds.setRect(stampBounds.getX(), stampBounds.getY(), stampBounds.getWidth(), 1);
				if (stampBounds.getWidth()==0) stampBounds.setRect(stampBounds.getX(), stampBounds.getY(), 1, stampBounds.getHeight());
			
			// Do a fast compare with the Rectangle bounds, then do a second
			// more accurate compare if the areas overlap.
				if (stampBounds.intersects(proximity1) || (proximity2 != null && stampBounds.intersects(proximity2))) {
					if (shapeArea.intersects(proximity1) || (proximity2 != null && shapeArea.intersects(proximity2))) {
					if (stampLayer.lineShapes()) {
						if (clonedStamps[i].intersects(proximity1, proximity2)) {
							list.add(clonedStamps[i]);
						}
					} else {
						list.add(clonedStamps[i]);				
					}					
						break shapeloop;
					}
				}
			}			
		}

		return list;
	}

	private StampShape findStampByScreenPt(Point screenPt)
	{
		MultiProjection proj = getProj();
		if (proj == null) {
			log.aprintln("null projection");
			return null;
		}

		Point2D worldPt = proj.screen.toWorld(screenPt);

		return  findStampByWorldPt(worldPt);
	}

	public StampShape findStampByWorldPt(Point2D worldPt)
	{
		if (stamps == null)
			return null;

		if (!isVisible() || viewman == null) {//@since remove viewman2 
			log.aprintln("view manager not available");
			return null;
		}

		MultiProjection proj = viewman.getProj();
		if (proj == null) {
			log.aprintln("null projection");
			return null;
		}

		Dimension2D pixelSize = proj.getPixelSize();
		if (pixelSize == null) {
			log.aprintln("no pixel size");
			return null;
		}

		double w = proximityPixels * pixelSize.getWidth();
		double h = proximityPixels * pixelSize.getHeight();
		double x = worldPt.getX() - w/2;
		double y = worldPt.getY() - h/2;

		x -= Math.floor(x/360.0) * 360.0;

		Rectangle2D proximity1 = new Rectangle2D.Double(x, y, w, h);
		Rectangle2D proximity2 = null;
		log.println("proximity1 = " + proximity1);

		// Handle the two cases involving x-coordinate going past
		// 360 degrees:
		// Proximity rectangle extends past 360...
		if (proximity1.getMaxX() >= 360) {
			proximity2 = new Rectangle2D.Double(x-360, y, w, h);
			log.println("proximity2 = " + proximity2);
		}
		// Normalized stamp extends past 360 but
		// proximity rectangle does not...
		else if (proximity1.getMaxX() <= 180) {
			proximity2 = new Rectangle2D.Double(x+360, y, w, h);
			log.println("proximity2 = " + proximity2);
		}
		
		StampShape clonedStamps[] = stamps.clone();
		
		Arrays.sort(clonedStamps, getOutlineFocusPanel().getOrderSort());
		
		for (int i=0; i<clonedStamps.length; i++) {
			if (clonedStamps[i].isHidden()) continue;
			
			// If these are global, they automatically match
			if (stampLayer.globalShapes()) {
				return clonedStamps[i];
			}
			
			List<Area> stampPaths = clonedStamps[i].getFillAreas();
			
			for (Area path : stampPaths) {
				if (path.intersects(proximity1) ||(proximity2 != null && path.intersects(proximity2))) {
					if (stampLayer.lineShapes()) {
						// TODO: Verify this works for line shapes still - might need to use the paths on this one
						if (clonedStamps[i].intersects(proximity1, proximity2)) {
							return  clonedStamps[i];
						}
					} else {
						return clonedStamps[i];
					}
				}
			}
		}

		return  null;
	}

	protected Object createRequest(Rectangle2D where)
	{
		return  where;
	}

	protected Layer.LView _new()
	{
		return new StampLView((StampLayer)getLayer(), null, true, layerParams, null); //send null for lview3d since this is the panner
	}

	public Layer.LView dup()
	{
		StampLView copy = (StampLView) super.dup();

		copy.stamps = this.stamps;

		return  copy;
	}

	public void selectionsChanged() {
		drawSelections(stampLayer.getSelectedStamps(), true);

		SpectraView spView = ((StampFocusPanel)focusPanel).spectraView;
		if (spView!=null && spView.isVisible()) {
			spView.refreshSpectraData(true);
			spView.addSelectionsFromLView();
			spView.rebuildColorMap(stampLayer.getSelectedStamps());
		}
		
		stampLayer.increaseStateId(StampLayer.SELECTIONS_BUFFER);

		if (ThreeDManager.isReady()) {
			//update the 3d view if has lview3d enabled
			if(getLView3D().isEnabled()){
				ThreeDManager mgr = ThreeDManager.getInstance();
				//If the 3d is already visible, update it
				if(getLView3D().isVisible()){
					mgr.updateDecalsForLView(this, true);
				}
			}
		}
	}
	
	public void selectionsAdded(List<StampShape> newStamps) {
		drawSelections(newStamps, false);
	}
	
    /**
     * Draws outlines for stamp selections.  This reside in their own
     * buffer layer.
     * <p>
     * If a redraw is requested, but no stamps are specified, then any
     * existing selected stamp outlines are cleared.
     * 
     * @param ss        List of stamps to be drawn (partial or all);
     * may be <code>null</code> (useful for clearing all selections
     * in combination with <code>redraw</code> parameter.
     * 
     * @param redraw    If <code>true</code>, then drawn selections are
     * cleared and are completely redrawn using the specified stamps as
     * the complete selection list.  Otherwise, buffer is not cleared
     * and the stamp list represents a partial selection/deselection.
     */
	private void drawSelections(final List<StampShape> selectedStamps, final boolean redraw)
	{
		final int seq = ++drawSelectionSequence;
		
		// Nothing to draw for global stamps
		if (stampLayer.globalShapes()) {
			return;
		}
		
	    if (redraw) {
            clearOffScreen(StampLayer.SELECTIONS_BUFFER);
	    }
        
		if (selectedStamps == null || selectedStamps.size() < 1) {
			repaint();
		    return;
        }

		pool.execute(tasked(new Runnable() {
			// copy the stamps on the calling thread, render them off of it
			public void run() {
				synchronized(drawSelLock) {
					if (seq != drawSelectionSequence) {
						return;
					}
					
					if (getChild()!=null) {
						((StampLView)getChild()).drawSelections(selectedStamps, redraw);
					}
					Graphics2D g2 = getOffScreenG2(StampLayer.SELECTIONS_BUFFER);
					if (g2 == null) {
						return;
					}
			        
					Color inverseColor = new Color(~stampLayer.getSettings().getUnselectedStampColor().getRGB());
					
			        g2.setComposite(AlphaComposite.Src);
			        g2.setStroke(new BasicStroke(0));
					g2.setColor(inverseColor);

					List paths=new ArrayList<Shape>();
					
					for(StampShape selectedStamp : selectedStamps){
						if (selectedStamp.isHidden) continue;
					
						if (stampLayer.pointShapes()) {   // Such as MOLA Shots
			    			double userScale = stampLayer.getSettings().getOriginMagnitude();
	
			    			String spotSize = stampLayer.getParam(stampLayer.SPOT_SIZE);
			    			
							double degrees = 0;
							
							try {
								degrees = Double.parseDouble(spotSize);
							} catch (Exception e) {
								// If SPOT_SIZE was unspecified or otherwise invalid, this might fail.  In that case,
								// it will default to 0 degrees in size, but be drawn at 1 pixel 
							}
							degrees = degrees / 2.0;				
							
							degrees = degrees * userScale;
							
			    			int ppd=viewman.getZoomManager().getZoomPPD();
			    			
							double pixels = ppd * degrees;
							
							// Make the spot size at least 1 pixel on the screen, no matter how far we zoom out.
							if (pixels<1.0) {
								degrees = 1.0 / ppd;
							}
									    			
	
							// For PointShapes, the points in the stamp object are just a single lon,lat value
							Point2D  p=  ((PointShape)selectedStamp).getOrigin();
							
		    				Ellipse2D oval = new Ellipse2D.Double(p.getX()-degrees, (p.getY()-degrees), 2*degrees, 2*degrees);

							g2.setColor(new Color(~stampLayer.getSettings().getUnselectedStampColor().getRGB()));
					        
		    				g2.draw(oval);		    				
						} 
						else if (stampLayer.spectraData()){
							SpectraView spectraView = ((StampFocusPanel)getFocusPanel()).spectraView;
	
				        	Color calcColor = selectedStamp.getCalculatedColor();
				        	
				        	if (calcColor!=null) {
			        			inverseColor = new Color(StampUtil.getContrastVersionForColor(calcColor));
			        		}
				        	Color spectraColor = spectraView.getColorForStamp(selectedStamp);
				     
				        	if (spectraColor!=null) {
				        		inverseColor = spectraColor;
				        	}

				        	g2.setColor(inverseColor);	
				        	
				        	if (seq != drawSelectionSequence) {
								return;
							}	
				        	List<GeneralPath> stampPaths = selectedStamp.getPath(Main.PO);
						
				        	paths = stampPaths;
				        	
							for (GeneralPath path : stampPaths) {
								g2.draw(path);
							}	
						} else {
							g2.setColor(inverseColor);
		
							if (seq != drawSelectionSequence) {
								return;
							}
							List<GeneralPath> stampPaths = selectedStamp.getPath(Main.PO);
						
							paths = stampPaths;
							
							for (GeneralPath path : stampPaths) {
								g2.draw(path);
							}	
						}
						
						// Boresight
				    	Point2D bp = selectedStamp.getBoresight();
				    	if (stampLayer.getSettings().showBoresight()) {
				    		StampUtil.drawBoresight(g2, Main.PO, bp, inverseColor, paths, true);
				    	}
					}
			        repaint();
				}
			}
		}));
	}

	public void panToStamp(StampShape s)
	{
	    centerAtPoint(Main.PO.convSpatialToWorld(s.getCenter()));
	    
		stampLayer.clearSelectedStamps();
		stampLayer.addSelectedStamp(s);
	}

	/**
	 *  Called to restore rendered stamps during view restoration after a program 
	 *  restart.  Only one successful call is allowed.
	 *
	 * @param stampStateList  List of stamp IDs to be restored and related state
	 *                        information.
	 */
	protected synchronized void restoreStamps(FilledStamp.State[] stampStateList)
	{
		if (stampStateList != null &&
		    !restoreStampsCalled)
		{
			restoreStampsCalled = true;

			// Add stamps to focus panel's list in reverse order;
			// the addStamp operation has a push-down behavior and
			// we want to reestablish the same order as in the list
			// of stamp IDs.
			if (myFocus.getRenderedView() != null) {
				log.println("processing stamp ID list of length " + stampStateList.length);
				log.println("with view stamp list of length " + (stamps == null ? 0 : stamps.length));
                
                StampShape[] stampList = new StampShape[stampStateList.length];
				FilledStamp.State[] stateList = new FilledStamp.State[stampStateList.length];

                StampLayer layer = (StampLayer)getLayer();
                int count = 0;
				for (int i=stampStateList.length - 1; i >= 0; i--) {
					log.println("looking for stamp ID " + stampStateList[i].id);

					StampShape s = layer.getStamp(stampStateList[i].id.trim());
					if (s != null) {
						stampList[count] = s;
						stateList[count] = stampStateList[i];

						count++;
						log.println("found stamp ID " + stampStateList[i].id);
					}
				}

				addSelectedStamps(stampList, stateList);
				log.println("actually loaded " + count + " stamps");
				if (count<1) restoreStampsCalled=false;
			}		
		}			

		log.println("exiting");
	}

	/** Adds specified stamps from stamp table list.
	 **
	 ** @param rows                    selected stamp indices in using *unsorted* row numbers
	 **/
	protected void addSelectedStamps(final int[] rows)
	{
	    if (rows != null &&
            rows.length > 0)
	    {
	        StampShape[] selectedStamps = new StampShape[rows.length];
	        for (int i=0; i < rows.length; i++)
	            if (rows[i] >= 0)
	                selectedStamps[i] = stamps[rows[i]];
	            
	        addSelectedStamps(selectedStamps, null);
	    }
	}

	/** Adds specified stamps from stamp table list.
	 **
	 ** @param selectedStamps          selected stamps to be added
	 ** @param stampStateList          stamp state information array with elements corresponding 
	 **                                    to order of indices in rows parameter; may be 'null'.
	 **/
	protected void addSelectedStamps(final StampShape[] selectedStamps,
	                                 final FilledStamp.State[] stampStateList)
	{
		if (selectedStamps != null && selectedStamps.length > 0)
			{
				final Runnable runner = new Runnable() {
					public void run() {
						myFocus.getRenderedView().addStamps(selectedStamps, stampStateList, null);						
					}
				};

				try {
					Thread t = new Thread(runner);
					t.start();
				} catch (Exception e) {
					log.aprintln(e);
				}
			}
	}
	
	// Used to draw all loaded stamps at once; useful after adding
	// multiple stamps.  Displays a progress dialog during a group image
	// frame creation process to bridge the time gap before the image
	// projection progress dialog is displayed.
	protected void drawStampsTogether()
	{
	    if (myFocus.getRenderedView() != null) {
	    	if (!isVisible() || viewman == null) {//@since remove viewman2 
	            log.aprintln("view manager not available");
	            return;
	        }
	        
	        redrawEverything(true);
	    }
	}

	// Currently only used for CRISM Spectra cursor changing
	class KeyHandler implements KeyListener {
		public void keyTyped(KeyEvent e) {
			// Unused
		}

		public void keyPressed(KeyEvent e) {
			if (stampLayer.spectraPerPixel()) {
				if (e.isAltDown()) {
					setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
				} 
			}
		}

		public void keyReleased(KeyEvent e) {
			if (e.getKeyCode() == KeyEvent.VK_ALT) {
				if (stampLayer.spectraPerPixel()) {
					setCursor(Cursor.getDefaultCursor());
				}
			}
		}
		
	}

	class MouseHandler implements MouseInputListener {
		protected void drawSelectionRect(Rectangle2D rect)
		{
			Graphics2D g2 = (Graphics2D) getGraphics();
			if (g2 != null) {
				g2.setStroke(new BasicStroke(2));
				g2.setXORMode(Color.gray);
				g2.draw(rect);

				log.println("drawing rectangle (" + rect.getMinX() + "," + rect.getMinY()+ ") to (" 
					    + rect.getMaxX() + "," + rect.getMaxY() + ")");
			}
		}

		// These three methods are required to implement the interface
		public void mouseEntered(MouseEvent e){};
		public void mouseExited(MouseEvent e){} ;

		public void mouseMoved(MouseEvent e){
//			if (e.isAltDown() && mouseIsOverFilledStamp()) {
//				getNumericToolTip(new Point(e.getX(), e.getY()), e.getComponent());
//			} else {
//				tipWindow.setVisible(false);
//				tipWindow.repaint();
//				tipWindow.toBack();
//				javax.swing.ToolTipManager.sharedInstance().setEnabled(true);
//				requestFocus();
//			}
		};
		
		public void mouseClicked(MouseEvent e)
		{
			// Pass thru for profile cueing
			if (e.isShiftDown()) {
				return;
			}
			
			if (stamps != null) {			
				StampShape stamp = findStampByScreenPt(e.getPoint());
				if (myFocus != null){
					// TODO: Probably not the right control
					if (e.isAltDown()) {
						setCursor(Cursor.getDefaultCursor());
						MultiProjection proj = getProj();	
						if (proj == null) return;

						// Screen point
						Point p = new Point(e.getX(), e.getY());
						
						Point2D worldPt = proj.screen.toWorld(p);

						List<FilledStamp> filledStamps = getFilteredFilledStamps();
						
						List<StampShape> stampsUnderCursor = findStampsByWorldPt(worldPt);
																		
						for (StampShape ss : stampsUnderCursor){
							for (FilledStamp fs : filledStamps){
								if (fs instanceof FilledStampImageType && (ss.getId().equalsIgnoreCase(fs.stamp.getId()))){
									HVector vector = screenPointToHVector(p, (FilledStampImageType)fs);	
										
									int renderPPD = viewman.getZoomManager().getZoomPPD();
									
									double lineSample = fs.pdsi.getFloatVal(vector, Main.PO, renderPPD);
									
									short sample = (short)((int)lineSample);
									short line = (short)((int)lineSample>>16);
									
									SpectraPixelDialog spd = new SpectraPixelDialog(Main.mainFrame, line, sample, StampLView.this);
									
									if (spd.okayClicked) {
										line = (short)spd.line;
										sample = (short)spd.sample;
										
										int xwidth = spd.xsize;
										int ywidth = spd.ysize;
										
										double data[][] = fs.pdsi.getSpectralVals(renderPPD, proj.getProjection(), vector, xwidth, ywidth);
	
										double vals[] = data[0];
										double axis[] = data[1];
																			
										// TODO: Points should be based off of ... image coordinates? or spatial coordinates?  Dunno.
										ss.addSpectra(spd.name, new Point2D.Double(sample, line), vals, axis, xwidth, ywidth, fs.pdsi.productID);
										
										SpectraView spView = ((StampFocusPanel)focusPanel).spectraView;
										if (spView!=null) {
											spView.showInFrame();
										}
										if (spView!=null && spView.isVisible()) {
											spView.refreshSpectraData(true);
										}
									}
									stampLayer.increaseStateId(StampLayer.IMAGES_BUFFER);
									drawFilled();
									return;
								}
							}
						}
					}
					if (!e.isControlDown()) {
						stampLayer.clearSelectedStamps();
					}
					if (stamp!=null) {
						stampLayer.toggleSelectedStamp(stamp);
					}
				}
			}
		}

	    protected boolean mouseDragged = false;
		protected Point mouseDown = null;
		protected Rectangle2D curSelectionRect = null;

		public void mousePressed(MouseEvent e)
		{
			// Pass thru for profile cueing
			if (e.isShiftDown()) {
				return;
			}

			// Initial drawing of rubberband stamp selection box.
			mouseDown = ((WrappedMouseEvent)e).getRealPoint();

			curSelectionRect = new Rectangle2D.Double(mouseDown.x, mouseDown.y, 0, 0);
			drawSelectionRect(curSelectionRect);
	        mouseDragged = false;
		}

		public void mouseDragged(MouseEvent e)
		{
			// Pass thru for profile cueing
			if (e.isShiftDown()) {
				return;
			}
			
			// Update drawing of rubberband stamp selection box.
			if (curSelectionRect != null && mouseDown != null) {
				Point curPoint = ((WrappedMouseEvent)e).getRealPoint();

				drawSelectionRect(curSelectionRect);
				curSelectionRect.setRect(mouseDown.x, mouseDown.y, 0, 0);
				curSelectionRect.add(curPoint);
				drawSelectionRect(curSelectionRect);
	            mouseDragged = true;
			}
		}

		public void mouseReleased(final MouseEvent e)
		{ 
			// Pass thru for profile cueing
			if (e.isShiftDown()) {
				return;
			}

			// Select stamps inside rubberband stamp selection box.
			if (mouseDragged && curSelectionRect != null && mouseDown != null) {
				drawSelectionRect(curSelectionRect);

	            StampTask task = ((StampLayer)getLayer()).startTask();
	            task.updateStatus(Status.YELLOW);
	            
	            getFocusPanel().repaint();
	            
	            MultiProjection proj = getProj();
	            if (proj == null) {
	                log.aprintln("null projection");
	                return;
	            }
	            
	            Point curPoint = ((WrappedMouseEvent)e).getRealPoint();
	            
	            Point2D worldPt1 = proj.screen.toWorld(mouseDown);
	            Point2D worldPt2 = proj.screen.toWorld(curPoint);
	            
	            double offset = Main.PO.getServerOffsetX();
	            
	            worldPt1.setLocation(worldPt1.getX() + offset, worldPt1.getY());
	            
	            worldPt2.setLocation(worldPt2.getX() + offset, worldPt2.getY());
	            
	            final Rectangle2D worldBounds = new Rectangle2D.Double(worldPt1.getX(), worldPt1.getY(), 0, 0);
	            worldBounds.add(worldPt2);
	            
	            List<StampShape> selectedList = null;
	            
	            if (stampLayer.getSettings().selectTopStampsOnly) {
	            	// New functionality (as of Dec 2020) to select only the stamps that are not completely covered by other stamps (Jon Hill request)
	            	findTopStampsByWorldRect(worldBounds, task, e.isControlDown());
	            } else {
	            	selectedList = findStampsByWorldRect(worldBounds);
	            	finalizeMouseRelease(task, selectedList, e.isControlDown());
	            }
			}
		}
		
		/*
		 * This method is called at the end of mouseRelease.  It may be called synchronously or asynchronously depending on the user's 
		 * selection settings.
		 */
		private void finalizeMouseRelease(StampTask task, List<StampShape> selectedList, boolean controlWasDown) {
			if (!controlWasDown) {
				stampLayer.clearSelectedStamps();
			}
			stampLayer.toggleSelectedStamps(selectedList);
			
			task.updateStatus(Status.DONE);
			getFocusPanel().repaint();

	        mouseDragged = false;
			mouseDown = null;
			curSelectionRect = null;			
		}

		/**
		 * Feature requested by Jon Hill to select only the 'top' stamps within a selection box.  'Top' stamps are defined as the stamps that
		 * would be not be completely covered by other stamps when they're drawn in a particular order.  This could be used, for example, to select
		 * a reduced set of observations for creating a mosaic.  Order is based on the outline order, as specified on the OutlineFocusPanel.
		 * 
		 * @param world extent
		 * @return List of StampShapes
		 */
		public void findTopStampsByWorldRect(Rectangle2D extent, final StampTask task, final boolean wasControlDown)	{
			// Now filter by only the stamps on top
			HashSet<StampShape> topList = new HashSet<StampShape>();
			
			Runnable runme = new Runnable() {
				public void run() {

					ProjObj proj = Main.PO;
					
					int ppd = viewman.getZoomManager().getZoomPPD();
					
			
					List<StampShape> stampsWithinExtent = findStampsByWorldRect(extent);
			
					OutlineFocusPanel ofp = getOutlineFocusPanel();
							
					StampGroupComparator orderSort = ofp.getOrderSort();
					
					stampsWithinExtent.sort(orderSort);
					
			//		// Now filter by only the stamps on top
			//		HashSet<StampShape> topList = new HashSet<StampShape>();
					
					double startx = extent.getMinX();
					double starty = extent.getMaxY();
			
					int widthInPixels = (int)(extent.getWidth()*ppd);
					int heightInPixels = (int)(extent.getHeight()*ppd);
					
					ArrayList<StampShape> dataGrid[][] = new ArrayList[widthInPixels][heightInPixels];
			
					for (int i=0; i<widthInPixels; i++) {
						for (int j=0; j<heightInPixels; j++) {
							dataGrid[i][j]=new ArrayList<StampShape>();
						}
					}
			
					final ProgressDialog dialog = new ProgressDialog(Main.mainFrame);
					
					dialog.updateStatus("Performing selection analysis on " + stampsWithinExtent.size() + " outlines.");
					dialog.startDownload(0, stampsWithinExtent.size());
					dialog.setNote("Analyzing possible selections: ");
					
					int cnt = 0;
					
					for (StampShape stamp : stampsWithinExtent) {
						List<GeneralPath> stampPaths = stamp.getPath(proj);
			
						for (GeneralPath shapePath : stampPaths) {
							for (int i=0; i<widthInPixels; i++) {
								for (int j=0; j<heightInPixels; j++) {
									double x = startx + (i*1.0)/ppd;
									double y = starty - (j*1.0)/ppd;
			
								if (shapePath.contains(x, y) || shapePath.contains(x+360,y)) {
										dataGrid[i][j].add(stamp);
									}
								}
							}
						}
						cnt++;
						
						dialog.downloadStatus(cnt);
						dialog.setNote("Analyzing possible selections: " + cnt);
						
						if (dialog.isCanceled()) break;
					}
					
					for (int i=0; i<widthInPixels; i++) {
						for (int j=0; j<heightInPixels; j++) {
							if (dataGrid[i][j].size()>0) {
								topList.add(dataGrid[i][j].get(dataGrid[i][j].size()-1));
							} 
						}
					}

					finalizeMouseRelease(task, new ArrayList(topList), wasControlDown);
				}
			};
					
			new Thread(runme).start();
		}
	}
	
	
    // Converts a screen position (e.g., mouse position) to an HVector
    // coordinate that includes correction for the offset shift of a
    // filled stamp at the current pixel resolution.
    public HVector screenPointToHVector(Point2D screenPt, FilledStampImageType fs)
    {
        HVector vec = null;
        
        if (screenPt != null &&
            fs != null)
        {
            Point2D worldPt = getProj().screen.toWorld(screenPt);
            Point2D offset = fs.getOffset();
            
            worldPt.setLocation( worldPt.getX() - offset.getX(),
                                 worldPt.getY() - offset.getY());
            
            vec = getProj().world.toHVector(worldPt);
        }
        
        return vec;
    }
    
    public void requestFocus() {
    	requestFocusInWindow(true);
    }
    
	public void viewCleanup() {
		if (getChild()!=null) {
			StampServer.getInstance().remove(getSettings());
		}

		stamps=null;
		
		lastFilledStamps=null;
		
	    lastStamps=null;
		
		//make the radar focus panel clean up after itself if it's in use
		RadarFocusPanel rFocus = ((StampFocusPanel)getFocusPanel()).getRadarView();
		if(rFocus!=null){
			rFocus.cleanUp();
		}
		
		//make the spectra view clean up after itself it it exists
		SpectraView spectraView = ((StampFocusPanel)getFocusPanel()).getSpectraView();
		if(spectraView != null){
			spectraView.cleanUp();
		}
		
		//make the scatter view clean up after itself it it exists
		ScatterView scatterView = ((StampFocusPanel)getFocusPanel()).getScatterView();
		if(scatterView != null){
			scatterView.cleanUp();
		}
	
	    if (myFocus!=null) {
	    	myFocus.dispose();
	    }
	    
		stampLayer.dispose();
	}
	
	/** represents a request to this LView to render filled stamps */
	public static interface DrawFilledRequest {
		/** returns true if the parameters the request is dependent on have changed */
		boolean changed();
		/** returns the extent of the request in the current projection */
		Rectangle2D getExtent();
		/** returns the ppd of the request */
		int getPPD();
	}
	
	public class StampRequest implements DrawFilledRequest {
		private final MultiProjection proj;
		private final Rectangle2D extent;
		private final int ppd;
		private final int seq;
		public StampRequest(int seq, MultiProjection proj, int ppd) {
			this.proj = proj;
			extent = proj.getWorldWindow();
			this.ppd = ppd;
			this.seq = seq;
		}
		public boolean changed() {
			return seq == drawFilledSequence && !this.extent.equals(proj.getWorldWindow());
		}
		public Rectangle2D getExtent() {
			return extent;
		}
		public int getPPD() {
			return ppd;
		}
		public boolean equals(Object o) {
			if (o instanceof StampRequest) {
				StampRequest req = (StampRequest)o;
				return ppd == req.ppd && extent.equals(req.extent);
			} else {
				return false;
			}
		}
	}
	
		public class StampRequest3D implements DrawFilledRequest {
		private final Rectangle2D extent;
		private final int ppd;
		private final int seq;
		public StampRequest3D(int seq, Rectangle2D extent, int ppd) {
			this.extent = extent;
			this.ppd = ppd;
			this.seq = seq;
		}
		public boolean changed() {
			return false;
		}
		public Rectangle2D getExtent() {
			return extent;
		}
		public int getPPD() {
			return ppd;
		}
		public boolean equals(Object o) {
			if (o instanceof StampRequest) {
				StampRequest req = (StampRequest)o;
				return ppd == req.ppd && extent.equals(req.extent);
			} else {
				return false;
			}
		}
	}

	public void requestFrameFocus() {
		this.getTopLevelAncestor().setFocusable(true);
		this.getTopLevelAncestor().repaint();
	}
	

	// wait interval for the mouse to settle
	static int waitMs = 250;
		
	public InvestigateData getInvestigateData(MouseEvent event){
		// Don't do this for the panner
 		if (getChild()==null) return null;

		MultiProjection proj = getProj();	
		if (proj == null) return null;

		Point2D screenPt = event.getPoint();
		Point2D worldPoint = proj.screen.toWorld(screenPt);
		
		// Get a list of stamp shapes under the cursor		
		List<StampShape> stamps = findStampsByWorldPt(worldPoint);
		if (stamps==null) return null;

		// Create the investigateData object with the name of the stamp layer	
		InvestigateData invData = new InvestigateData(this.getName());
 		
		
		List<FilledStamp> filledStamps = getFilteredFilledStamps();

		Point p = new Point(event.getX(), event.getY());
						
		// For each of the stampShapes, get their individual investigateData 
		// and add that to the invData object	
		stampShape: for (StampShape ss : stamps){
			for (FilledStamp fs : filledStamps){
				if (fs instanceof FilledStampImageType && (ss.getId().equalsIgnoreCase(fs.stamp.getId()))){
//					Point p = new Point(event.getX(), event.getY());

					boolean isNumeric = fs.pdsi.isNumeric;

					if (isNumeric) {
						String key = fs.stamp.getId() + "-" + fs.pdsi.getImageType();		
						String value = "Invalid";
						
						try {
							HVector vector = screenPointToHVector(p, (FilledStampImageType)fs);	
												
							double floatValue = fs.pdsi.getFloatVal(vector, proj.getProjection(), viewman.getZoomManager().getZoomPPD());
	
							value = "" + floatValue;
							//if Not a Number, set isNumeric to false
							if(Double.isNaN(floatValue)){
								isNumeric = false;
								continue;
							}
							
							if (floatValue==StampImage.IGNORE_VALUE) {
								// No valid data for this filledStamp, but continue to see if others have values
								continue;
							}
						}catch (Exception e) {
							e.printStackTrace();
						}
						invData.add(key, value, fs.pdsi.getUnits(), "ItalicSmallBlue","SmallBlue", isNumeric);
					}
					
					continue stampShape;
				}
			}
			ss.getInvestigateData(invData);
		}

		// Return the invData object with contains information for each
		//stamp under the cursor.	
		return invData;
	}
	
	public String getUnits() {
		List<FilledStamp> stamps = getFilteredFilledStamps();
		if(stamps!=null && stamps.size()>0){
			return stamps.get(0).pdsi.units;
		}
		return null;
	}
	
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
	 public String getLayerKey(){
		 if(layerParams!=null){
			 String s = layerParams.options.get(0);
			 s = s.replace("_", " ");
			 return s;
		 }else{
			 String result = stampLayer.getInstrument();
			 result = result.replace("_", " ");
			 return result;
		 }
	 }
	 public String getLayerType(){
		 if(layerParams!=null)
			 return layerParams.type;
		 else
			 return "stamp";
	 }
} // end: class StampLView
