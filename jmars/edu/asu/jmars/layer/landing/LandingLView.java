package edu.asu.jmars.layer.landing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;

import edu.asu.jmars.Main;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.SingleProjection;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.shape2.ShapePath;
import edu.asu.jmars.layer.stamp.StampFactory;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

public class LandingLView extends LView {
	
	private static final String VIEW_SETTINGS_KEY = "landing";
	private LandingLayer myLayer = (LandingLayer)getLayer();
	
	private boolean isRubberband = false;
	private Point2D rubberbandBoxStart; // stored as local screen points
	private Point2D rubberbandBoxEnd; // stored as local screen points
	
	private Point2D spUpperLeft; //spatial coords
	private Point2D spLowerRight; //spatial coords
	
	boolean addMode=true;
	boolean selectMode=false;
	
	Point mouseLocation = null;
	private Point prevMouseLoc;
	MouseEvent moveEvent = null;
	
	protected ArrayList<LandingSite> selectedSites;
	
	//Update if the default stats change. As of right now there are five:
	// Elevation, Slope, Albedo, Thermal Inertia, and Dust Index.
	private int numOfDefaultStats = 5;  
	
	//Arrays of the available ellipse sizes (entries in hor and ver arrays 
	//  correspond with each other, they are the axis lengths of the ellipse)
	public ArrayList<Integer>horAxis;
	public ArrayList<Integer>verAxis;
	private String hAxesConfig;
	private String vAxesConfig;
	private ArrayList<StatCalculator> statCalcs;
	
	
	//used for the map sampling refresh when changing the angle
	private int angleCounter = 0;
	private static ExecutorService pool = Executors.newCachedThreadPool(new ThreadFactory(){
		int id = 0;
		public Thread newThread(Runnable r){
			Thread t = new Thread(r);
			t.setName("LandingLView-mapSample-"+(id++));
			t.setPriority(Thread.MIN_PRIORITY);
			t.setDaemon(true);
			return t;
		}
	});
	private LinkedBlockingQueue<CountEvent> angleStatsQueue = new LinkedBlockingQueue<CountEvent>(300);
	static int waitMs = 250;

	private Object createLock = new Object();
	public FocusPanel getFocusPanel() {
		if (focusPanel==null) {
			synchronized(createLock) {
				if (focusPanel==null) {
					if (getChild()==null && getParentLView()!=null) {
						focusPanel=getParentLView().getFocusPanel();
					} else {
						focusPanel=new LandingFocusPanel(this);
					}
					
				}
			}
		}
		return focusPanel; 
	}

	
	public LandingLView(final LandingLayer layer, boolean isMainView, LandingLView3D lview3d){
		super(layer, lview3d);
		
		myLayer = layer;
		
		selectedSites = layer.getSelectedSites();
		
		hAxesConfig = myLayer.configEntry+".horizontalAxes";
		vAxesConfig = myLayer.configEntry+".verticalAxes";
		
		// Buffers for identified craters, selected craters, and the new crater stamp
		setBufferCount(3);
		
		//add mouse and key listeners
		addMouseMotionListener(motionListener);
		addMouseListener(mouseListener);		
		addMouseWheelListener(wheelListener);
		addKeyListener(keyListener);		
		
		//create default stats calculators and add them to array
		createDefaultStats();
		populateEllipseSizes(isMainView);
	}
	
	
	
	MouseMotionListener motionListener = new MouseMotionListener() {
		public void mouseMoved(MouseEvent e) {
			if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
				if (addMode) {
					repaint();
					moveEvent=e;
					mouseLocation=e.getPoint();
				}
			}
	
		}
		public void mouseDragged(MouseEvent e) {
			if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
				if(isRubberband && selectMode){
					// get point as local screen position
					Point loc = e.getPoint();
					Point2D screen = viewman.getProj().screen.toScreenLocal(loc.getX(), loc.getY());
					
					// set end point
					rubberbandBoxEnd = screen;
					repaint();
			    }
				else if(selectMode && selectedSites.size()> 0){
					// get old spatial location for crater
					Point2D oldWorld = viewman.getProj().screen.toWorld(prevMouseLoc);
					Point2D oldSpatial = Main.PO.convWorldToSpatial(oldWorld);
					
					// get new spatial location for crater
					Point loc = e.getPoint();
			        Point2D world = viewman.getProj().screen.toWorld(loc);
					Point2D spatial = Main.PO.convWorldToSpatial(world);

					// get change in location
					double deltaX = (360-spatial.getX()) - (360-oldSpatial.getX());
					double deltaY = spatial.getY() - oldSpatial.getY();

					LandingSiteTableModel tm = ((LandingFocusPanel)getFocusPanel()).table.getTableModel();

					ArrayList<LandingSite> selected = (ArrayList<LandingSite>) selectedSites.clone();
					for(LandingSite ls : selected){
						int row = tm.getRow(ls);
						
						tm.setValueAt(ls.getLon()+deltaX, row, 
									  LandingSiteTableModel.CENTERLON_COLUMN);
						tm.setValueAt(ls.getLat()+deltaY, row, 
									  LandingSiteTableModel.CENTERLAT_COLUMN);
					}
					
					prevMouseLoc = loc;	// previous mouse location had changed
				}
			}
		}
	};
	
	
	MouseListener mouseListener = new MouseListener() {
		public void mouseReleased(MouseEvent e) {
			if(isRubberband && selectMode) // second conditional not really needed, but JIC
			{
				ArrayList<LandingSite> sitesToSelect = new ArrayList<LandingSite>(); 
				if (e.isControlDown()){
					sitesToSelect.addAll(selectedSites);
				}

				//change bounding box to world cords
				Point2D worldStart = getProj().screen.toWorld(rubberbandBoxStart);
				Point2D worldEnd = getProj().screen.toWorld(rubberbandBoxEnd);
				
				// get rubberbandBox boundaries from start and end points
				double minX = Math.min(worldStart.getX(), worldEnd.getX());
				double maxX = Math.max(worldStart.getX(), worldEnd.getX());
				double minY = Math.min(worldStart.getY(), worldEnd.getY());
				double maxY = Math.max(worldStart.getY(), worldEnd.getY());
				
				
				Rectangle2D bbox = new Rectangle2D.Double(minX, minY, maxX-minX, maxY-minY);


				LandingSiteSettings settings = getSettings();
				// for each crater in the crater layer
				for (LandingSite ls : settings.sites){
					ls.calcWorldPath(getProj().getProjection());
					if(Util.intersectsInWorldCoords(ls.getWorldPath(), bbox)){
						sitesToSelect.add(ls);
					}
				}
				
				
				isRubberband = false; // don't forget to turn off
				selectSites(sitesToSelect);
				repaint();
			}
			//if a site was moved, recalculate it's stats
			if(selectMode){
				LandingSiteSettings settings = getSettings();
				for(LandingSite ls : settings.sites){
					if(ls.getStats().size()>0){
						Stat aStat = ls.getStats().get(0);
						if (aStat.upperLeft != ls.getUpperLeft() ||
								aStat.lowerRight != ls.getLowerRight() ||
								aStat.angle != ls.getAngle()){
							//set particluar stat dirty so it will be refreshed
							ls.dirty = true;
							
							//read out the points
//							PathIterator pi = ls.getWorldPath().getPathIterator(new AffineTransform());
//							while(!pi.isDone()){
//								double[] coords = new double[6];
//								pi.currentSegment(coords);
//								System.out.println(coords[0]+", "+coords[1]);
//								pi.next();
//							}
//							System.out.println("moved site: "+ls.getWorldPath().getPathIterator(new AffineTransform()));
						}
					}
				}
				//this is to refresh stats
				updateStats();
			}
		}
		public void mousePressed(MouseEvent e) {
			prevMouseLoc = e.getPoint();
			if (ToolManager.getToolMode() == ToolManager.SEL_HAND){
				if (e.getButton()==MouseEvent.BUTTON1) {
					if (addMode) {
						
						LandingSiteSettings settings = getSettings();
					
						final LandingSite newSite = new LandingSite(spUpperLeft,
																	  spLowerRight,
																	  settings.nextColor, 
																	  getProj().getProjection());
						newSite.setUser(Main.USER);
						newSite.setHorizontalAxis(settings.nextHorSize);
						newSite.setVerticalAxis(settings.nextVerSize);
						

						newSite.setComment("");

						//This method refreshes the focus panel and updates the lview when finished
						calculateMapSamplings(newSite, true);
						
						addSiteToLayer(newSite);
						
					}

					else if(selectMode){
						for(LandingSite ls : selectedSites){
							if(ls.getScreenPath().contains(e.getPoint())){
								return;
							}
						}
						// get initial point as local screen position
						Point loc = e.getPoint();
						Point2D screen = viewman.getProj().screen.toScreenLocal(loc.getX(), loc.getY());
						
						// set initial points for rubberband box
						isRubberband = true;
						rubberbandBoxStart = screen;
						rubberbandBoxEnd = screen;
						repaint();
					}
				}
		}
		}
		public void mouseExited(MouseEvent e) {
			if (ToolManager.getToolMode() == ToolManager.SEL_HAND){
				mouseLocation=null;
				repaint();
			}
		}
		public void mouseEntered(MouseEvent e) {
			//requestFocus is platform dependent, changed to requestFocusInWindow to avoid windows and mac
			//issue where focus panel hides behind the main view when the main glass is moused over
			LandingLView.this.requestFocusInWindow();
		}
		public void mouseClicked(MouseEvent e) {
			if (ToolManager.getToolMode() == ToolManager.SEL_HAND && selectMode)
			{
				ArrayList<LandingSite> sitesToSelect=new ArrayList<LandingSite>();
				LandingSiteSettings settings = getSettings();
				
				if(settings.sites.size()>0){
					if(e.isControlDown()){
						sitesToSelect.addAll(selectedSites);
					}
					for(LandingSite ls : settings.sites){
						if(ls.getScreenPath().contains(e.getPoint())){
							if(!selectedSites.contains(ls)){
								sitesToSelect.add(ls);
							}else{
								sitesToSelect.remove(ls);
							}
						}
					}
				}

				selectSites(sitesToSelect);
				repaint();
			}
		}
	};
	
	
	MouseWheelListener wheelListener = new MouseWheelListener() {
		public void mouseWheelMoved(MouseWheelEvent e) {
			// Some mouse wheels seem to send 2 clicks at a minimum, which is
			// very annoying.  We allow larger click counts to accommodate
			// fast changes in size while still hopefully guaranteeing that
			// single click increments can occur.
			if (ToolManager.getToolMode() == ToolManager.SEL_HAND){
				if (e.getClickCount()==2) return;
			
				boolean colorKeyDown = false;
				
				if (e.isControlDown()) {
					colorKeyDown=true;
				}
				
				ArrayList<LandingSite> selected = (ArrayList<LandingSite>) selectedSites.clone();
				ColorCombo cc = ((LandingFocusPanel)getFocusPanel()).newSiteColor;
				LandingSiteTableModel tm = ((LandingFocusPanel)focusPanel).table.getTableModel();
				if (colorKeyDown && addMode) {
					int clicks = e.getWheelRotation();
					int index;
					if (clicks > 0) {
						index = cc.getSelectedIndex() + 1;
					} else{
						index = cc.getSelectedIndex() - 1;
					}
					if(index >= cc.getItemCount()){
						index = 0;
					}
					if(index < 0 ){
						index = cc.getItemCount()-1;
					}
					cc.setSelectedIndex(index);
					repaint();
					return;
				}
				
				else if(colorKeyDown && selectMode){
					int clicks = e.getWheelRotation();
					int index;
					if (clicks > 0) {
						index = cc.getSelectedIndex() + 1;
					} else{
						index = cc.getSelectedIndex() - 1;
					}
					if(index >= cc.getItemCount()){
						index = 0;
					}
					if(index < 0 ){
						index = cc.getItemCount()-1;
					}
					cc.setSelectedIndex(index);
					for(LandingSite ls : selected){
						ls.setColor((Color)cc.getSelectedItem());
						tm.setValueAt(cc.getSelectedItem(), tm.getRow(ls), LandingSiteTableModel.COLOR_COLUMN);
						drawSites();
						drawSelectedSites();
						repaint();
					}
					return;
				}
			
				if (addMode) {
					LandingSiteSettings settings = getSettings();
					if(e.getWheelRotation() > 0){
						settings.axisIndex++;
						if(settings.axisIndex>horAxis.size()-1){
							settings.axisIndex = 0;
						}
					}else{
						settings.axisIndex--;
						if(settings.axisIndex<0){
							settings.axisIndex = horAxis.size()-1;
						}
					}
					swapSize();
					repaint();
				}
				if (selectMode){
					int clicks = e.getWheelRotation();
					int clickCount = e.getClickCount();
					int increment = 10; //degrees
					if(e.isShiftDown()){
						increment = 1;
					}
					
					for(LandingSite ls : selected){
						double theta = ls.getAngle();
						if(clicks>0){
							tm.setValueAt(theta*180/Math.PI+increment*clickCount, 
										  tm.getRow(ls), 
										  LandingSiteTableModel.ANGLE_COLUMN);
						}else{

							tm.setValueAt(theta*180/Math.PI-increment*clickCount, 
										  tm.getRow(ls), 
										  LandingSiteTableModel.ANGLE_COLUMN);						
						}
						//do this so the stats refresh properly
						ls.dirty = true;
					}
					//this is also to refresh stats
					updateStats();
				}
			}
		}
	};
	
	KeyListener keyListener = new KeyListener() {
		public void keyTyped(KeyEvent e) {
			if (ToolManager.getToolMode() == ToolManager.SEL_HAND){
				if (addMode) {
					Character c=e.getKeyChar();
					LandingSiteSettings settings = getSettings();
					if (c=='+'){
						settings.axisIndex++;
						if(settings.axisIndex>horAxis.size()-1){
							settings.axisIndex = 0;
						}
					}else if(c=='-'){
						settings.axisIndex--;
						if(settings.axisIndex<0){
							settings.axisIndex = horAxis.size()-1;
						}
					}
					swapSize();	 
					repaint();
				}
			}
		}
		public void keyReleased(KeyEvent e) {
		}

		public void keyPressed(KeyEvent e) {
		}
	};
	
	//A simple method to do a few things necessary to update
	// the stats on a landing site.  Made this method because
	// these lines were being used in a few different places.
	public void updateStats(){
		angleCounter++;
		angleStatsQueue.add(new CountEvent(angleCounter));
		runPool();
	}
	
	//used in angle counter to check to see if the user has
	// stopped changing the angle of landing sites, to refresh
	// their stats.
	class CountEvent{
		int myCount;
		long reqTime;
		CountEvent(int count){
			myCount = count;
			reqTime = System.currentTimeMillis();
		}
	}
	
	//Actually runs through the queue looking to see if the 
	// user has stopped changing angle, then refreshes the 
	// necessary landingsites' stats.
	private void runPool(){
		pool.execute(new Runnable() {
			CountEvent ce = null;
			int queueCount = 0;
			public void run(){
				ce = angleStatsQueue.poll();
				if(ce == null){
					//nothing to do
					return;
				}
				queueCount = ce.myCount;
				//if count hasn't changed, check time
				if(queueCount == angleCounter){
					//if it's been a second, check count again
					//else wait second, then check count again 
					if(ce.reqTime + waitMs > System.currentTimeMillis()){
						try{
							Thread.sleep(waitMs);
						}catch(Exception e){
						}
					}
					//now that it's been a second, check count again
					if(queueCount == angleCounter){
						LandingSiteSettings settings = getSettings();
						//cycle through all the sites looking for the dirty ones
						for(LandingSite ls : settings.sites){
							//if site is dirty, refresh it's stats
							if(ls.dirty){
								calculateMapSamplings(ls, true);
								//data is now set and hasn't been changed
								ls.dirty = false;
							}
						}
					}
					
				}
			}
		});
	}
	private void createDefaultStats(){
		
		String[] names = Config.getArray(Util.getProductBodyPrefix()+myLayer.configEntry+".stat.name");
		String[] sNames = Config.getArray(Util.getProductBodyPrefix()+myLayer.configEntry+".stat.sName");;
		String[] ppds = Config.getArray(Util.getProductBodyPrefix()+myLayer.configEntry+".stat.ppd");;
		String[] mapNames = Config.getArray(Util.getProductBodyPrefix()+myLayer.configEntry+".stat.mapName");;
		
		//cannot be zero length
		if(names.length == 0 || sNames.length == 0 || ppds.length == 0 || mapNames.length == 0){
			Util.showMessageDialog("Your JMARS config file does not have any predefined" +
					" stats for this layer.\nIt may not function properly "+
					"because of this.\nPlease contact JMARS help if you're come "+
					" accross this problem.", "No Defined Stats", JOptionPane.ERROR_MESSAGE);
			System.err.println("No defined stats for landing layer on "+Main.getBody()+".");

		}
		
		//check to make sure all arrays are same length
		if(names.length != sNames.length || names.length != ppds.length || names.length != mapNames.length){
			System.err.println("Mismatching lengths of horizontal and vertical axes for landing"+
					"ellipses.  Unable to populate ellipse size.  \nPlease check your"+
					" JMARS config for entries 'mars.landing.horizontalAxes.#' and "+
					"'mars.landing.verticalAxes.#' and make sure they have the same"+
					" number of entries each, then restart JMARS.");	
		}
		
		final MapServer server = MapServerFactory.getServerByName("default");
		MapSource source = null;
		int ppd;
		String name;
		String shortName;
		statCalcs = new ArrayList<StatCalculator>();
		
		for(int i = 0; i<names.length; i++){
			source = server.getSourceByName(mapNames[i]);
			ppd = Integer.parseInt(ppds[i]);
			name = names[i];
			shortName = sNames[i];
			
			StatCalculator sc = new StatCalculator(name, shortName, true, source, 
					 ppd, true, true, true, true);
			
			statCalcs.add(sc);
		}
		
	}
	

	//Set default ellipse sizes and add sizes from config file
	private void populateEllipseSizes(boolean isMainView){
		if(isMainView){
			horAxis = new ArrayList<Integer>();
			verAxis = new ArrayList<Integer>();
	
			//pull entries from config (defaults are set there as well)
			String hAxes[] = Config.getArray(Util.getProductBodyPrefix()+hAxesConfig);
			String vAxes[] = Config.getArray(Util.getProductBodyPrefix()+vAxesConfig);
			//if there are no entries, display a popup dialog
			if(hAxes.length == 0 || vAxes.length == 0){
				Util.showMessageDialog("Your JMARS config file does not have any predefined" +
															" ellipse sizes for this layer.\nIt may not function properly "+
															"because of this.\nPlease contact JMARS help if you've come "+
															" across this problem.", "No Defined Ellipse Sizes", JOptionPane.ERROR_MESSAGE);
				System.err.println("No defined ellipse sizes for landing layer on "+Main.getBody()+".");
			}
			//if vertical and horizontal sizes match, add values
			if(hAxes.length == vAxes.length){
				for(int i=0; i<hAxes.length; i++){
					horAxis.add(Integer.parseInt(hAxes[i]));
					verAxis.add(Integer.parseInt(vAxes[i]));
				}
				LandingSiteSettings settings = getSettings();
				//set starting values
				settings.nextHorSize = horAxis.get(horAxis.size()-1);
				settings.nextVerSize = verAxis.get(verAxis.size()-1);
				
				//set index
				settings.axisIndex = hAxes.length - 1;
				
			}else{
				System.err.println("Mismatching lengths of horizontal and vertical axes for landing"+
									"ellipses.  Unable to populate ellipse size.  \nPlease check your"+
									" JMARS config for entries 'mars.landing.horizontalAxes.#' and "+
									"'mars.landing.verticalAxes.#' and make sure they have the same"+
									" number of entries each, then restart JMARS.");	
			}
		}
	}
	
	
	
	public String swapSize(){
		LandingSiteSettings settings = getSettings();
		//change the axes indexes in the settings
		settings.nextHorSize = horAxis.get(settings.axisIndex);
		settings.nextVerSize = verAxis.get(settings.axisIndex);
		
		return settings.nextHorSize+"m x "+settings.nextVerSize+"m";
	}
		
	private Thread workingThread;
	public void calculateMapSamplings(final LandingSite site, final boolean fromRefresh){
	//clear stats array in each site first
		site.getStats().clear();
		final ArrayList<LandingSite> selected = new ArrayList(selectedSites);
		
		workingThread = new Thread(new Runnable() {
			public void run() {
				
				//cycle through calculators and create new stats to add to site	
				for(StatCalculator sc : statCalcs){
					Stat newStat = sc.calculateStat(site);
					site.getStats().add(newStat);
				}
				
				final LandingFocusPanel fp = (LandingFocusPanel)getFocusPanel();
			
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						//draw for the first time
						if(!fromRefresh){
							addSiteToLayer(site);
						}
						//refresh table
						if(fromRefresh){
							fp.table.getTableModel().refreshRow(site);
						}

						selectedSites = selected;
						drawSites();
						drawSelectedSites();
						
					}
				});

			}
		});
		workingThread.start();
	}
	

	
	
	protected Component[] getContextMenuTop(Point2D worldPt)
	{
		if(viewman.getActiveLView().equals(this)){
			List<Component> newItems =
				new ArrayList<Component>( Arrays.asList(super.getContextMenuTop(worldPt)) );
	
	
			JMenu mode = new JMenu(this.getName()+" Mode");
			
			JRadioButtonMenuItem addModeMenu= new JRadioButtonMenuItem("Add mode");
			
			addModeMenu.addActionListener(new ActionListener(){
			
				public void actionPerformed(ActionEvent e) {
					addMode=true;
					selectMode=false;
					repaint();
				}
			});
			
			JRadioButtonMenuItem selectModeMenu= new JRadioButtonMenuItem("Select mode");
			
			selectModeMenu.addActionListener(new ActionListener(){
			
				public void actionPerformed(ActionEvent e) {
					addMode=false;
					selectMode=true;
					repaint();
				}
			});
			
	
			ButtonGroup group = new ButtonGroup();
			group.add(addModeMenu);
			group.add(selectModeMenu);
	
			if(addMode) {
				addModeMenu.setSelected(true);
			} else if (selectMode) {
				selectModeMenu.setSelected(true);
			}
	
			
			mode.add(addModeMenu);
			mode.add(selectModeMenu);
			newItems.add(0, mode);
			
			JMenuItem removeSites = new JMenuItem("Remove selected sites");
			removeSites.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteSelectedSite();
	
					LandingSiteTable table = ((LandingFocusPanel)focusPanel).table;
					table.getSorter().clearSorts();
					LandingLView.this.repaint();
					
					drawSelectedSites();
					LandingLView.this.getChild().repaint();
				}
			});
			if(selectedSites.size()>0){
				removeSites.setEnabled(true);
			}else{
				removeSites.setEnabled(false);
			}
			newItems.add(removeSites);
			
			
			findStamps = new JMenu("Find overlapping stamps");
			if(selectedSites.size()>0){
				findStamps.setEnabled(true);
			}else{
				findStamps.setEnabled(false);
			}
			newItems.add(findStamps);
			findStamps = populateFindIntersectingStamps(findStamps);
				
			
			return  (Component[]) newItems.toArray(new Component[0]);
		}
		else{
			return new Component[0];
		}
	}
	
	JMenu findStamps;
	protected JMenu populateFindIntersectingStamps(JMenu menu){
		Set<String> layerTypes=StampFactory.getLayerTypes();
	    
	    for(final String type : layerTypes) {
			JMenuItem findOverlaps= new JMenuItem("Find intersecting " + type + " stamps");
			findOverlaps.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					ArrayList<GeneralPath> allStampPaths = new ArrayList<GeneralPath>();
					ArrayList<String> allStampIds = new ArrayList<String>();
					
					int cnt=1;
					for (int i=0; i<selectedSites.size(); i++) {
						LandingSiteSettings settings = getSettings();
						Feature f = selectedSites.get(i).getFeature(settings);
					
						allStampPaths.add(new GeneralPath(f.getPath().convertTo(FPath.WORLD).getShape()));
						Field label = new Field("Label", String.class);
						Object attr = f.getAttribute(label);
						allStampIds.add(attr!=null?(String)attr:"Unnamed Shape "+ cnt++);
					}

					StampFactory.createOverlappingStampLayer(type, allStampIds, allStampPaths);
				};
			});		 
			menu.add(findOverlaps);
	    }
	    return menu;
	}
	


	synchronized void drawSelectedSites() {
		
		clearOffScreen(1);
		Graphics2D g2=getOffScreenG2Direct(1);
		
		if (g2==null) {
			return;
		} 
		
		g2=viewman.wrapScreenGraphics(g2);

		for (LandingSite ls : selectedSites) {
			Shape shp = ls.getScreenPath();
			Color color = ls.getColor();
			int red = 0;
			int green = 0;
			int blue = 0;
			
			if(color.getRed()<128){
				red=255;
			}if(color.getGreen()<128){
				green = 255;
			}if(color.getBlue()<128){
				blue = 255;
			}
			color = new Color(red,green,blue);
			g2.setColor(color);
			g2.setStroke(new BasicStroke(3));
			g2.draw(shp);
		}				
	}
	
    
	//@since 3.0.3 - make this object static so that the lock will be across instances of panner view and main view
	private static Object drawSitesLock = new Object();
	
    void drawSites() {	
    	synchronized(drawSitesLock) {
    		
		LandingFocusPanel fp = (LandingFocusPanel)getFocusPanel();

		ArrayList<LandingSite> sitesToDraw = null;
		LandingSiteSettings settings = getSettings();

    	if (getChild()!=null) {  // mainView logic
   
    		((LandingLView)getChild()).drawSites();

    		sitesToDraw = settings.sites;
    		 		
    	} else {  // panner logic
    		sitesToDraw = settings.sites;
    		
    	}
		
		clearOffScreen(0);
		Graphics2D g2=getOffScreenG2Direct(0);
		
		if (g2==null) {
			return;
		} 

		g2=viewman.wrapScreenGraphics(g2);

		if (!sitesToDraw.containsAll(selectedSites)) {
			ArrayList<LandingSite> sitesToDeselect = (ArrayList<LandingSite>)selectedSites.clone();
			
			selectedSites.retainAll(sitesToDraw);
			sitesToDeselect.removeAll(selectedSites);
			
			for (LandingSite ls : sitesToDeselect) {
				
				int row=fp.table.getSorter().sortRow(fp.table.getTableModel().getRow(ls));
				fp.table.getSelectionModel().removeSelectionInterval(row,row);
			}

			drawSelectedSites();
		}
		
		
		//Stroke is determined by the landing settings
		g2.setStroke(setDrawStroke());
		for (LandingSite ls : sitesToDraw) {
			Color color = ls.getColor();
			color = new Color((settings.alpha<<24) | (color.getRGB()&0xFFFFFF), true);
			g2.setColor(color);
			//TODO: if location, zoom and proj haven't changed...draw cached shape.
			//calculate path based off current projection
			Shape shp = ls.calcScreenPath(getProj());
			if(settings.filterSiteFill){
				g2.fill(shp);
				g2.draw(shp);
			}else{
				g2.draw(shp);
			}
		}
    	}
	}
	
	
	private void selectSites(List<LandingSite> sitesToSelect) {
		selectedSites.clear();
		selectedSites.addAll(sitesToSelect);		
		
		drawSelectedSites();
		((LandingFocusPanel)focusPanel).table.selectRows(sitesToSelect);
		
	}
	
	protected void deleteSelectedSite() {
		LandingSiteSettings settings = getSettings();
		settings.sites.removeAll(selectedSites);
		selectedSites.clear();
		
		LandingSiteTable table = ((LandingFocusPanel)focusPanel).table;
		table.getTableModel().removeAll();
		for (LandingSite c: settings.sites) {
			table.getTableModel().addRow(c);
		}
		table.clearSelection();

		
		drawSites();
		drawSelectedSites();
	}
	
	protected void deleteLastSite() {
		LandingSiteSettings settings = getSettings();
		if (settings.sites.size() > 0) {
			LandingSite site = settings.sites.get(settings.sites.size() - 1);
			if (settings.sites.remove(site)) {
				LandingSiteTable table = ((LandingFocusPanel)focusPanel).table;
				table.getTableModel().removeAll();
				for (LandingSite ls : settings.sites) {
					table.getTableModel().addRow(ls);
				}
				table.clearSelection();
			}
		}
		drawSites();
		drawSelectedSites();
	}
	
	
	private BasicStroke setDrawStroke(){
		LandingSiteSettings settings = getSettings();
		//Stroke is determined by the landing settings
		float thickness = (float)settings.siteLineThickness;
		if(settings.styleIndex == 1){  //dashed stroke
			float dash[] = {5.0f};
			BasicStroke dashed = new BasicStroke(thickness, 			//width
												BasicStroke.CAP_ROUND,	//cap
												BasicStroke.JOIN_ROUND,	//join
												5.0f,					//miterlimit
												dash,					//dash
												0.0f);					//dash phase
			return dashed;
		}else{  //solid stroke
			return new BasicStroke(thickness);
		}
	}
	
	
	public synchronized void paintComponent(Graphics g) {
		// Don't try to draw unless the view is visible
		if (!isVisible() || viewman == null) {//@since remove viewman2
			return;
		}

		LandingSiteSettings settings = getSettings();
		Color color = settings.nextColor;
		
		clearOffScreen(2);
		Graphics2D g2=getOffScreenG2Direct(2);
		

		if (g2==null) {
			return;
		} 

		g2=viewman.wrapScreenGraphics(g2);

		color=new Color((settings.alpha<<24) | (color.getRGB()&0xFFFFFF), true);
		g2.setColor(color);
			
		if (addMode && mouseLocation!=null) {
			//Projections used to change points to and from
			SingleProjection scProj = getProj().screen;
			SingleProjection wdProj = getProj().world;
			SingleProjection spProj = getProj().spatial;
			
		//Get the initial h vector of the mouse location (from screen point)
			HVector start = new HVector(scProj.toSpatial(mouseLocation));
		//Get the x and y mouse location in screen coords to be used to find hvector rotation axes
			double scX = mouseLocation.getX();
			double scY = mouseLocation.getY();
		//Find the horizontal component	
			//Find the direction (to the right in screen coords) and translate to spatial coords
			HVector horDir = new HVector(scProj.toSpatial(new Point2D.Double(scX+5, scY)));
			//Take the cross product of the direction vector and initial vector to find rotation vector
			HVector horAxis = start.cross(horDir);
			//Use the rotation vector and the (horizontal axis/Planetary radius) to find the new end point
			HVector horEnd = start.rotate(horAxis, (((double)settings.nextHorSize)/1000)/Util.MEAN_RADIUS);
		//Find the vertical component
			//Find the direction (downwards in screen coords) and translate to spatial coords
			HVector verDir = new HVector(scProj.toSpatial(new Point2D.Double(scX, scY+5)));
			//Take the cross product of the direction vector and initial vector to find rotation vector
			HVector verAxis = start.cross(verDir);
			//Use the rotation vector and the (horizontal axis/Planetary radius) to find the new end point
			HVector verEnd = start.rotate(verAxis, (((double)settings.nextVerSize)/1000)/Util.MEAN_RADIUS);
			
		//Create points for the initial and final points using the HVectors (in screen coords)
			Point2D scrnStart = spProj.toScreen(new Point2D.Double(start.lon(),start.lat()));
			//Get the world start point -- is used when crossing the world prime meridian, so we don't wrap the wrong way
			Point2D wrldStart = spProj.toWorld(new Point2D.Double(start.lon(),start.lat())); 
			Point2D scrnHor = spProj.toScreen(new Point2D.Double(horEnd.lon(), horEnd.lat()));
			Point2D scrnVer = spProj.toScreen(new Point2D.Double(verEnd.lon(), verEnd.lat()));

			Point2D wrldEndH = spProj.toWorld(new Point2D.Double(horEnd.lon(), horEnd.lat()));

			//If the world points are too far apart, then adjust the ending point by 360.
			double endX = scrnHor.getX();
			//if it's more than half the planet, wrap the other way
			if(Math.abs(wrldStart.getX() - wrldEndH.getX())>180){
				endX = (wdProj.toScreen(new Point2D.Double(wrldEndH.getX()+360, wrldEndH.getY()))).getX();
			}
		//Calculate height and width in screen coords		
			int width = (int)Math.sqrt((scrnStart.getX()-endX)*(scrnStart.getX()-endX)+(scrnStart.getY()-scrnHor.getY())*(scrnStart.getY()-scrnHor.getY()));
			int height = (int)Math.sqrt((scrnStart.getX()-scrnVer.getX())*(scrnStart.getX()-scrnVer.getX())+(scrnStart.getY()-scrnVer.getY())*(scrnStart.getY()-scrnVer.getY()));

		//calculate the height and width in spatial coords
			double spW = Math.sqrt((start.lon()-horEnd.lon())*(start.lon()-horEnd.lon())+(start.lat()-horEnd.lat())*(start.lat()-horEnd.lat()));
			double spH = Math.sqrt((start.lon()-verEnd.lon())*(start.lon()-verEnd.lon())+(start.lat()-verEnd.lat())*(start.lat()-verEnd.lat()));

            // If the distance between spatial points is over than 180, we're crossing the meridian.  Connect the other way
			if (spW>180) {
				spW = 360-spW;
			}

			//using height and width, calculate the upperleft and lower right corners of the bounding 
			// box for the ellipse
			Point2D scUpperLeft = new Point2D.Double(mouseLocation.getX()-width/2,mouseLocation.getY()-height/2);
			Point2D scLowerRight = new Point2D.Double(mouseLocation.getX()+width/2,mouseLocation.getY()+height/2);
			
		//define spatial upper left and lower right which will be passed in to 
		// create the actual site shape (this way it is the same no matter what 
		// zoom because it is not related to the screen coordinates).
//			spUpperLeft = new Point2D.Double(start.lon()+spW/2,start.lat()+spH/2);
//			spLowerRight = new Point2D.Double(start.lon()-spW/2, start.lat()-spH/2);

			// The previously used calculations assumed spatial changes were linear, which is not at all true outside of the default projection.
			spUpperLeft = scProj.toSpatial(scUpperLeft);
			spLowerRight = scProj.toSpatial(scLowerRight);
			
			//draw landing ellipse outline
			//Stroke is determined by the landing settings
			g2.setStroke(setDrawStroke());
			g2.drawOval((int)scUpperLeft.getX(), (int)scUpperLeft.getY(), width, height);
			
			
			//draw size label
			g.setColor(Color.white);
			
			String axesString ="";
			DecimalFormat axesFormatter = new DecimalFormat("0.##");

			if(settings.filterVisibleDiameter) {
				double horMeters = settings.nextHorSize;
				double verMeters = settings.nextVerSize;
				
				if (horMeters>500) {
					axesString=""+axesFormatter.format(horMeters/(1000.0))+"km x "+
											axesFormatter.format(verMeters/1000)+"km";
				} else {
					axesString=""+axesFormatter.format(horMeters)+"m x "+
										axesFormatter.format(verMeters)+"m";
				}
			} 
			
			g.drawString(axesString, (int)mouseLocation.getX()-10, (int)mouseLocation.getY()-5);
		}
		

		
		if (selectMode) {
			this.setCursor(Cursor.getDefaultCursor());			
		}

		if(isRubberband && selectMode){		
			// set dimensions since drawRect() only draws in the fourth quadrant
			int x = (int)Math.min(rubberbandBoxStart.getX(), rubberbandBoxEnd.getX());
			int y = (int)Math.min(rubberbandBoxStart.getY(), rubberbandBoxEnd.getY());
			int width = (int)Math.abs(rubberbandBoxEnd.getX() - rubberbandBoxStart.getX());
			int height = (int)Math.abs(rubberbandBoxEnd.getY() - rubberbandBoxStart.getY());
			
			// draw the rubber band box
			g2.setColor(new Color(0, 255, 255, 10)); // translucent cyan
			g2.fill(new Rectangle(x,y,width,height)); //g2.fillRect(x, y, width, height);
			g2.setColor(Color.blue); // outline
			g2.draw(new Rectangle(x,y,width,height)); //g2.drawRect(x, y, width, height);
		}
		
		// super.paintComponent draws the back buffers onto the layer panel 
		//  (call after you're done painting to g2)
		super.paintComponent(g);
	}

	
	
	protected LView _new() {
		// Create a copy of ourself for use in the panner-view.
		return new LandingLView(myLayer, false, null);
	}

	protected Object createRequest(Rectangle2D where) {
		// Build a request object for the layer.
		// The layer will respond back with the data.
		return where;
	}

	public void receiveData(Object layerData) {
		// Process the data returned by the layer.
		// Including displaying the data to the screen.
		Rectangle2D r = (Rectangle2D)layerData;
		drawSites();
		drawSelectedSites();
		repaint();
	}
	
	public String getName() {
		return myLayer.layerName;
	}

	protected synchronized void updateSettings(boolean saving) {
		LandingSiteSettings settings = myLayer.settings;
		if (saving) {
			// save settings into hashtable
			Map<String, Object> layer = new HashMap<String, Object>();

			FeatureCollection fc = new SingleFeatureCollection();

			for (LandingSite ls : settings.sites) {
				fc.addFeature(ls.getFeature(settings));
			}

			// will reload this file from within the session file
			Field[] schema = ((List<Field>) fc.getSchema()).toArray(new Field[0]);
			layer.put("schema", schema);
			Object[][] values = new Object[settings.sites.size()][];
			for (int i = 0; i < values.length; i++) {
				Feature f = fc.getFeature(i);
				values[i] = new Object[schema.length];
				for (int j = 0; j < schema.length; j++) {
					Object value = f.getAttribute(schema[j]);
					if (value instanceof FPath) {
						value = new ShapePath((FPath) value);
					}
					values[i][j] = value;
				}
			}
			layer.put("values", values);
			//save the mouse mode
			if(addMode){
				layer.put("mouseMode", "addMode");
			}else if(selectMode){
				layer.put("mouseMode", "selectMode");
			}

			viewSettings.put(VIEW_SETTINGS_KEY, settings);
			viewSettings.put(VIEW_SETTINGS_KEY + "-data", layer);
		} else {
			if (viewSettings.containsKey(VIEW_SETTINGS_KEY)) {
				myLayer.settings = (LandingSiteSettings) viewSettings
						.get(VIEW_SETTINGS_KEY);
				if (myLayer.settings != null) {
				} else {
					// Restore failed
					myLayer.settings = new LandingSiteSettings();
				}
			}

			settings = myLayer.settings;
			if (viewSettings.containsKey(VIEW_SETTINGS_KEY + "-data")) {
				Map<String, Object> layer = (Map<String, Object>) viewSettings.get(VIEW_SETTINGS_KEY + "-data");

				if (layer.get("schema") instanceof Field[] && layer.get("values") instanceof Object[][]) {
					Field[] schema = (Field[]) layer.get("schema");
					Object[][] values = (Object[][]) layer.get("values");
					FeatureCollection siteCollection = new SingleFeatureCollection();
					// FeatureCollection fc = settings.craterCollection;
					for (Field f : schema) {
						siteCollection.addField(f);
					}
					for (Object[] row : values) {
						LandingSite ls = new LandingSite();
						for (int i = 0; i < schema.length; i++) {
							Object value = row[i];
							if (value instanceof ShapePath) {
								value = ((ShapePath) value).getPath();
							}
							ls.setAttribute(schema[i], value);
						}
						siteCollection.addFeature(ls.getFeature(settings));
					}

					settings.sites = new ArrayList<LandingSite>();
					for (Feature f : siteCollection.getFeatures()) {
						LandingSite ls = new LandingSite(f);
						settings.sites.add(ls);
					}
				}
				
				if(layer.get("mouseMode") instanceof String){
					String mMode = (String)layer.get("mouseMode");
					if(mMode.equals("selectMode")){
						addMode = false;
						selectMode = true;
					}
				}
			}
			
			if (settings.sites != null) {
				for (LandingSite ls : settings.sites) {
					calculateMapSamplings(ls, false); //calculates samples and adds to table
				}
			}

			drawSites();
			repaint();
			
			myLayer.increaseStateId(0);
			if(ThreeDManager.isReady()){
				ThreeDManager.getInstance().updateDecalsForLView(this, true);
			}
		}
	}
	
	
	public ArrayList<StatCalculator> getStatCalculators(){
		return statCalcs;
	}
	
	
	private void addSiteToLayer(LandingSite newSite){
		LandingSiteSettings settings = getSettings();
		settings.sites.add(newSite);
		((LandingSiteTableModel)((LandingFocusPanel)focusPanel).table.getUnsortedTableModel()).addRow(newSite);
		drawSites();
		repaint();
		((LandingLView)getChild()).repaint();
		//update 3D
		myLayer.increaseStateId(0);
		if(ThreeDManager.isReady()){
			ThreeDManager.getInstance().updateDecalsForLView(this, true);
		}
	}
	
//	/**
//	 * Override to handle special needs.
//	 */
	public SerializedParameters getInitialLayerData()
	{
		return getSettings();
	}
	
	
	private LandingSiteSettings getSettings(){
		return myLayer.settings;
	}
	
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
 	public String getLayerKey(){
 		return "Landing Site Ellipse";
 	}
 	public String getLayerType(){
 		return "landing_site";
 	}
 	public void viewChanged() {
 		//When the main view was disabled, and then re-enabled, the sites would not show up until some other action triggered a refresh
 		// this method was created to force a repaint when the main view is re-enabled
		super.viewChanged();
		drawSites();
		repaint();
	}
}
