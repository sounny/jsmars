package edu.asu.jmars;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.JToolTip;
import javax.swing.SwingUtilities;
import javax.swing.GroupLayout.Alignment;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import org.material.component.swingsnackbar.SnackBar;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.LViewManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.SavedLayer;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.shape2.drawingpalette.DrawingPalette;
import edu.asu.jmars.layer.shape2.xb.XB;
import edu.asu.jmars.layer.stamp.StampFactory;
import edu.asu.jmars.ruler.BaseRuler;
import edu.asu.jmars.ruler.RulerManager;
import edu.asu.jmars.swing.TabLabel;
import edu.asu.jmars.swing.quick.add.layer.CommandReceiver;
import edu.asu.jmars.swing.snackbar.SnackBarBuilder;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUIState;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeButton;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.emory.mathcs.backport.java.util.Collections;


public class TestDriverLayered extends JPanel
{
    private static DebugLog log = DebugLog.instance();   
	public ToolManager toolMgr;
    public  LocationManager locMgr;
    public  LViewManager    mainWindow;
    private LViewManager    panner;   
	protected JLabel centerOfProj;
    private static String cacheDir = Main.getJMarsPath() + "bodies" + File.separator;   
    protected TabLabel statusBar;  
    protected JSplitPane splitPane;
	protected JSplitPane totalPane;
	protected SnackBar distanceNotif = null;
    private static final int preferredHeight = 32; 
    private static final UUID uuid = UUID.randomUUID();    
    public static final int INITIAL_MAIN_ZOOM = 32;
	public static final int INITIAL_PANNER_ZOOM = 8;
	// TODO: Seems like this should be overridden by a property....
    public static final int INITIAL_MAX_ZOOM_LOG2 = 21;    
    boolean ignorePreviousState = false;
    private boolean focusLOST = false;
    private JLabel quickChartsLbl;
    
	/**
	 * If the user assigns a custom name to a layer then it is stored in the customLayerNames Map.
	 */
	private Map<LView,String> customLayerNames = new HashMap<LView, String>();
	
    public TestDriverLayered()
    {
	// location manager - look for a save initial value
	String initialX = Main.userProps.getProperty("Initialx", "");
	String initialY = Main.userProps.getProperty("Initialy", "");
	String serverOffset = Main.userProps.getProperty("ServerOffset", "");
	
	locMgr = new LocationManager(Main.initialWorldLocation);
	
	if (initialX != "" && initialY != "" ) {
	    Point2D.Double pt = null;

	    try {

		double offsetX = Double.parseDouble(serverOffset) - Main.PO.getServerOffsetX();
		double newX = Double.parseDouble(initialX) + offsetX;

		pt = new Point2D.Double(newX, (new Double(initialY)).doubleValue());
	    } catch ( Exception ex) {
		//ignore error so default is simply null
	    }
	    if ( pt != null )
		locMgr.setLocation(pt, false);
	}
	
	// LViewManager - first get saved values
	int mainZoomLog2 = Util.log2(Main.userProps.getPropertyInt("MainZoom", INITIAL_MAIN_ZOOM));
	int pannerZoomLog2 = Util.log2(Main.userProps.getPropertyInt("PannerZoom", INITIAL_PANNER_ZOOM));
	int maxZoomLog2 = Config.get("maxzoomlog2", INITIAL_MAX_ZOOM_LOG2);
	
	mainWindow = new LViewManager(locMgr, new ZoomManager(mainZoomLog2, maxZoomLog2), null);
	panner	   = new LViewManager(locMgr, new ZoomManager(pannerZoomLog2, maxZoomLog2), mainWindow);
	
	
	    //Create the status bar
		statusBar = new TabLabel("");
		statusBar.getInnerLabel().setToolTipText("These numbers show cursor position on a map in Main view");
		statusBar.setIconTextGap(10);		
		statusBar.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 5));
		FontMetrics fm = statusBar.getFontMetrics(statusBar.getFont());
		int statusBarWidth = fm.stringWidth("00000OCENTRIC00369.532° N, 886.935° W		OGRAPHIC		699.532° N, 886.935° W");

	   //create ui for center of projection
		centerOfProj = new JLabel();
		centerOfProj.setToolTipText("Center of projection. The values change when using View->Recenter projection");
		centerOfProj.setIconTextGap(10);		
		centerOfProj.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));		
	    int centerOfProjWidth = fm.stringWidth("00000368.532° N, 888.932° W");
	
	    //create distance notification component - material snackbar		   
		distanceNotif = SnackBarBuilder.build(Main.mainFrame, "DISTANCE", "", uuid)
				.setSnackBarBackground(ThemeSnackBar.getBackgroundStandard())
				.setSnackBarForeground(ThemeSnackBar.getForegroundStandard())
				.setPosition(SnackBar.BOTTOM_RIGHT).setMarginBottom(150);
		
		Window parentwindowfordistance = SwingUtilities.windowForComponent(distanceNotif);
		if (parentwindowfordistance != null) {
			parentwindowfordistance.addWindowFocusListener(new WindowAdapter() {				
				public void windowGainedFocus(WindowEvent e) {
				    focusLOST = false;				   
				}

				public void windowLostFocus(WindowEvent e) {
					focusLOST = true;
					dismissDistanceNotif();					
				}
			});
		}		
	
		// lay them out
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 10));

		mainWindow.setMinimumSize(new Dimension(20, 20));
		panner.setMinimumSize(new Dimension(20, 20));
		panner.setPreferredSize(new Dimension(100, 100));

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setLeftComponent(mainWindow);
		splitPane.setResizeWeight(1.0);

		// use the session value if defined, else use the config setting if defined,
		// else use the old horizontal mode
		int index = Main.userProps.getPropertyInt("PannerMode", Config.get("panner.mode", PannerMode.Horiz.ordinal()));
		if (index < 0 || index >= PannerMode.values().length) {
			index = PannerMode.Horiz.ordinal();
		}
		setPannerMode(PannerMode.values()[index]);

		Color dragColor = Color.WHITE;
        String theme = GUIState.getInstance().themeAsString();
		if (GUITheme.LIGHT.asString().equalsIgnoreCase(theme)) {
        	dragColor = Color.GRAY;
        }		
		
		int height = Main.userProps.getPropertyInt("SplitPaneHeight", 500);
		int width = Main.userProps.getPropertyInt("SplitPaneWidth", 600);
		int div = Main.userProps.getPropertyInt("MainDividerLoc", splitPane.getDividerLocation());
		splitPane.setPreferredSize(new Dimension(width, height));
		splitPane.setDividerLocation(div);
		
		BasicSplitPaneDivider divider = ((BasicSplitPaneUI)splitPane.getUI()).getDivider();
        divider.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        divider.setLayout(new BorderLayout());
        divider.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        ImageIcon ic = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DRAG_HANDLES.withDisplayColor(dragColor)));
        JLabel divLbl = new JLabel(ic) {
        	@Override
        	public void paintComponent(Graphics g) {

        		//the drag handles image is vertical. For horizontal dividers, we need to rotate it.
        		Graphics2D g2 = (Graphics2D) g;
        		g2.rotate(Math.toRadians(90.0), getX() + getWidth()/2, getY() + getHeight() / 2);
        		super.paintComponent(g);
        	}
        };
        divider.add(divLbl, BorderLayout.CENTER);

		// places the lviews inside a larger split pane that will also
		// contain the lmanager
		totalPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		totalPane.setRightComponent(splitPane);
		totalPane.setBorder(BorderFactory.createEmptyBorder());
		divider = ((BasicSplitPaneUI)totalPane.getUI()).getDivider();
		divider.setLayout(new BorderLayout());
        divider.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        ic = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DRAG_HANDLES.withDisplayColor(dragColor)));
        divLbl = new JLabel(ic);
        divider.add(divLbl, BorderLayout.CENTER);
        int lManagerDivLoc = Config.get("lManagerDividerLoc", 320);
		totalPane.setDividerLocation(lManagerDivLoc);
		
		PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent changeEvent) {
            	
            	String propertyName = changeEvent.getPropertyName();
	            if (propertyName.equals(JSplitPane.DIVIDER_LOCATION_PROPERTY)) {
	            	Integer last = (Integer) changeEvent.getNewValue();
	            	Config.set("lManagerDividerLoc", String.valueOf(last));
	            }
            }
        };
        totalPane.addPropertyChangeListener(propertyChangeListener);
		

		// creates a toolbar
		toolMgr = new ToolManager();

		// override the tooltip on the memory meter so it is more readable
		// this is a change needed from the new materail UI (TODO: may
		// not be necessary when the memory meter location has changed)
		JToolTip memTT = new JToolTip();
		memTT.setComponent(memmeter);
		memTT.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
		memmeter = new JProgressBar() {
			@Override
			public JToolTip createToolTip() {
				return memTT;
			}
		};
		memmeter.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				log.println("gc running");
				System.gc();
				log.println("gc finished");
			}
		});

	//creates the top panel of the main view
	JPanel top = new JPanel();
	JLabel jmarsLbl = new JLabel("JMARS");
	jmarsLbl.setFont(ThemeFont.getBold().deriveFont(18f));
	top.setLayout(new GridBagLayout());
	top.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
	int pad = 2;
	Insets in = new Insets(0,pad,0,pad);
	top.add(new JLabel(new ImageIcon(Util.getJMarsIcon().getScaledInstance(33, 33, Image.SCALE_SMOOTH))), new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, in, pad, pad));
	top.add(jmarsLbl, new GridBagConstraints(1, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 0, 95), pad, pad));
	top.add(locMgr, new GridBagConstraints(2, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
	top.add(toolMgr, new GridBagConstraints(3, 0, 1, 1, 0.9, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, in, pad, pad));
	top.add(mainWindow.getZoomManager(), new GridBagConstraints(4, 0, 1, 1, 0.1, 0, GridBagConstraints.EAST, GridBagConstraints.NONE, in, pad, pad));
	JPanel quickAccessPanel = getQuickActionPanel();
	bottomRow = new JPanel();
	GroupLayout bottomRowLayout = new GroupLayout(bottomRow);
	bottomRow.setLayout(bottomRowLayout);
	bottomRowLayout.setHonorsVisibility(memmeter, false);
	
	bottomRowLayout.setHorizontalGroup(bottomRowLayout.createSequentialGroup()
		.addGap(2)
        .addComponent(quickAccessPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(ComponentPlacement.RELATED)
		.addComponent(statusBar, statusBarWidth, statusBarWidth, statusBarWidth)//TODO: reset all this on body switch (if to or from earth)
		.addGap(0,0,Short.MAX_VALUE)
		.addComponent(centerOfProj, centerOfProjWidth, centerOfProjWidth, centerOfProjWidth)
		.addGap(0, 200, Short.MAX_VALUE)
		.addComponent(memmeter, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
	bottomRowLayout.setVerticalGroup(bottomRowLayout.createParallelGroup(Alignment.CENTER)
		.addComponent(quickAccessPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
		.addComponent(statusBar, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
		.addComponent(centerOfProj, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
		.addComponent(memmeter, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE));
	
		add(top, BorderLayout.NORTH);
		add(totalPane, BorderLayout.CENTER);
		add(bottomRow, BorderLayout.SOUTH);		
		setMetersVisible(Config.get("main.meters.enable", false));
    }
    private JPanel getQuickActionPanel() {
    	Color inactiveIconColor = ((ThemeImages)GUITheme.get("images")).getIconInactive();
    	Color imgColor = ((ThemeImages) GUITheme.get("images")).getCommonFill();
    	JLabel quick3dLbl = new JLabel();
        quick3dLbl.setIcon(new ImageIcon(ImageFactory.createImage(ImageCatalogItem.QUICK_3D)));
        quick3dLbl.setToolTipText("3D Layer");
        quickChartsLbl = new JLabel();
        new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(((ThemeImages) GUITheme.get("images")).getLayerfill())));
    	quickChartsLbl.setIcon(new ImageIcon(ImageFactory.createImage(ImageCatalogItem.QUICK_CHARTS.withDisplayColor(imgColor))));
        quickChartsLbl.setToolTipText("Charts");
        quickChartsLbl.setEnabled(true);
        JLabel quickCustomMapsLbl = new JLabel();
        quickCustomMapsLbl.setToolTipText("Custom Map Manager (must be logged in)");
        JLabel quickGlobeLbl = new JLabel();
        quickGlobeLbl.setIcon(new ImageIcon(ImageFactory.createImage(ImageCatalogItem.QUICK_GLOBE)));
        quickGlobeLbl.setToolTipText("3D View");
        JLabel quickVrLbl = new JLabel();
        quickVrLbl.setToolTipText("XR Interface (must be logged in)");
        if (Main.isUserLoggedIn()) {
        	quickCustomMapsLbl.setToolTipText("Custom Map Manager");
        	quickVrLbl.setToolTipText("XR Interface");
        	
        	quickCustomMapsLbl.setIcon(new ImageIcon(ImageFactory.createImage(ImageCatalogItem.QUICK_CUSTOM_MAPS)));
            quickVrLbl.setIcon(new ImageIcon(ImageFactory.createImage(ImageCatalogItem.QUICK_VR)));
        } else {
            quickCustomMapsLbl.setIcon(new ImageIcon(ImageFactory.createImage(ImageCatalogItem.QUICK_CUSTOM_MAPS.withDisplayColor(inactiveIconColor))));
            quickVrLbl.setIcon(new ImageIcon(ImageFactory.createImage(ImageCatalogItem.QUICK_VR.withDisplayColor(inactiveIconColor))));
        }
        
        JLabel quickAccess = new JLabel("Quick Access ");
        quickAccess.setForeground(ThemeProvider.getInstance().getAction().getDefaultForeground());  //color same as default button
        
        JPanel panel = new JPanel();
        panel.setBackground(((ThemeButton)GUITheme.get("button")).getHighlight());
        GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);
        layout.setAutoCreateContainerGaps(false);
        layout.setAutoCreateGaps(false);
        
        layout.setHorizontalGroup(layout.createSequentialGroup()
        	.addGap(5)
        	.addComponent(quickAccess)
        	.addGap(30)
        	.addComponent(quick3dLbl)
        	.addGap(10)
        	.addComponent(quickChartsLbl)
        	.addGap(10)
        	.addComponent(quickCustomMapsLbl)
        	.addGap(10)
        	.addComponent(quickGlobeLbl)
        	.addGap(10)
        	.addComponent(quickVrLbl)
        	.addGap(5));
        
        layout.setVerticalGroup(layout.createSequentialGroup()
        	.addGap(10)
        	.addGroup(layout.createParallelGroup(Alignment.CENTER)
            	.addComponent(quickAccess)
            	.addComponent(quick3dLbl)
            	.addComponent(quickChartsLbl)
            	.addComponent(quickCustomMapsLbl)
            	.addComponent(quickGlobeLbl)
            	.addComponent(quickVrLbl))
        	.addGap(10));
        
        quick3dLbl.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (LoginWindow2.getInitialize3DFlag()) {
					CommandReceiver cr = new CommandReceiver();
					cr.load3DLayer(false);
				}
			}
		});
        quickChartsLbl.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				CommandReceiver cr = new CommandReceiver();
				cr.loadChartsView();
			}
		});
        quickCustomMapsLbl.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (Main.isUserLoggedIn()) {
					CommandReceiver cr = new CommandReceiver();
					cr.loadCustomMaps();
				}
			}
		});
        quickGlobeLbl.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (LoginWindow2.getInitialize3DFlag()) {
					CommandReceiver cr = new CommandReceiver();
					cr.load3DView();
				}
			}
		});
        quickVrLbl.addMouseListener(new MouseListener() {
			@Override
			public void mouseReleased(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mouseEntered(MouseEvent e) {}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				if (Main.isUserLoggedIn()) {
					CommandReceiver cr = new CommandReceiver();
					cr.loadVR();
				}
			}
		});
        return panel;
        
    }
	private JPanel bottomRow;
	private JProgressBar memmeter;
	private Timer meterTimer;
	private JPanel meterContainer;
	private Dimension dimensionMemmeter = new Dimension(150, preferredHeight);
	
	/**
	 * @param visible
	 *            If true, will make sure the meter component is next to the
	 *            status bar, otherwise will make sure the status bar has the
	 *            whole bottomRow component to itself.
	 */
	public void setMetersVisible(boolean visible) {
		// always cancel an existing timer and clear all components
		if (meterTimer != null) {
			meterTimer.cancel();
			meterTimer = null;
		}
		memmeter.setVisible(visible);
			if (visible) {
			// insert meter next to status bar and start new timer
			meterTimer = new Timer("memmeter-updater", true);
			final TimerTask meterUpdater = new TimerTask() {
				public void run() {
					Runtime r = Runtime.getRuntime();
					final long max = r.maxMemory();
					final long used = r.totalMemory() - r.freeMemory();
					final int percent = (int) Math.round(100d * used / max);
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							memmeter.setValue(percent);
							memmeter.setToolTipText(MessageFormat.format("Memory: {0}% of {1} MB used, click to clean",
									percent, Math.round(max / 1024 / 1024)));
						}
					});
				}
			};
			meterTimer.scheduleAtFixedRate(meterUpdater, new Date(), 1000);
		}	
	
		Config.set("main.meters.enable", visible);	
	}

	public void updateDistanceNotification(String msg) {
		if (focusLOST) {
			dismissDistanceNotif();
			return;
		}
		if (!msg.trim().isEmpty()) {
			distanceNotif.setText(msg);
			distanceNotif.revalidate();
			distanceNotif.refresh().run();
		} else {
			dismissDistanceNotif();
		}
	}

	private void dismissDistanceNotif() {
		if (distanceNotif != null && distanceNotif.isRunning()) {
			distanceNotif.dismiss();
		}
	}

    public Dimension getMainLViewManagerSize() {
	return mainWindow.getSize();
    }
    
    public void setMainLViewManagerSize(Dimension d) {
	mainWindow.setSize(d);
	mainWindow.validate();
	mainWindow.repaintChildVMan();
    }
    
    public void dumpMainLViewManagerJpg(String filename) {
	mainWindow.dumpJpg(filename);
    }
    
    public void dumpMainLViewManagerPNG(String filename) {
        mainWindow.dumpPNG(filename);
    }

    public void dumpMainLViewManagerTif(String filename) {
        mainWindow.dumpTIF(filename);
    }

	// Get the properties of any defined rulers and general ruler properties.
	public void loadRulersState()
	{
		
		Hashtable<String,Object> allRulerSettings = (Hashtable<String,Object>)Main.userProps.loadUserObject( "AllRulerSettings");
		if (allRulerSettings != null){
			RulerManager.Instance.loadSettings( allRulerSettings);
		}

		int rulerCount = Main.userProps.getPropertyInt("RulerCount", 0);
		for (int j=0; j < rulerCount; j++) {
			String rulerLabel = "Ruler" + String.valueOf(j);
			String rulerName = Main.userProps.getProperty( rulerLabel, "");
			BaseRuler ruler = (BaseRuler)RulerManager.Instance.getRuler( rulerName);
			if (ruler!=null){
				Hashtable<String,Object> rulerSettings = (Hashtable<String,Object>)Main.userProps.loadUserObject( rulerLabel + "Settings");
				ruler.loadSettings( rulerSettings);
				if (j==0){
					Hashtable<String,Object> settings = (Hashtable<String,Object>)Main.userProps.loadUserObject( "BaseRulerSettings");
					ruler.loadBaseRulerSettings( settings);
				}
			} 
		}
	}
	
	// Save the properties of any defined rulers and general ruler properties.
	public void saveRulersState()
	{
		Hashtable<String,Object> allRulerSettings = RulerManager.Instance.saveSettings();
		if (allRulerSettings != null){
			Main.userProps.saveUserObject( "AllRulerSettings", allRulerSettings);
		}

		int rulerCount = RulerManager.Instance.rulerList.size();
		Main.userProps.setPropertyInt("RulerCount", rulerCount);
		for (int j=0; j < rulerCount; j++) {
			BaseRuler ruler = (BaseRuler)RulerManager.Instance.rulerList.get(j);
			Main.userProps.setProperty( "Ruler" + String.valueOf(j), ruler.getClass().getName());
			Hashtable<String,Object> rulerSettings = ruler.saveSettings();
			Main.userProps.saveUserObject( "Ruler" + String.valueOf(j) + "Settings", rulerSettings);
			if (j==0){
				Hashtable<String,Object> settings = ruler.saveBaseRulerSettings();
				Main.userProps.saveUserObject( "BaseRulerSettings", settings);
			}
		}
	}

	public void saveState() {
		Main.userProps.saveWindowPosition(Main.mainFrame);
		// save the general JMARS stuff.
		if (!LManager.getLManager().isDocked()) {
			Main.userProps.saveWindowPosition(LManager.getDisplayFrame());
		}
		try
		{
			Main.userProps.setProperty("LManager.tabDocking",
					LManager.getLManager().getDockingStates());
		}
		catch(Exception e)
		{
			log.aprintln(e);
			log.aprintln("Failed to save layer manager tab locations");
		}
		Main.userProps.setProperty("selectedBody", Main.getCurrentBody());
		Main.userProps.setProperty("versionNumber", Util.getVersionNumber());
		Main.userProps.setProperty(Main.SESSION_KEY_STR, Main.getSessionKey());
		Main.userProps.setProperty("SplitPaneHeight", String.valueOf(splitPane.getSize().height));
		Main.userProps.setProperty("SplitPaneWidth", String.valueOf(splitPane.getSize().width));
		Main.userProps.setProperty("jmars.user", Main.USER);
		Main.userProps.setPropertyInt("MainDividerLoc",	     splitPane.getDividerLocation());
		Main.userProps.setPropertyInt("MainZoom",	 mainWindow.getZoomManager().getZoomPPD());
		Main.userProps.setPropertyInt("PannerZoom",	 panner.getZoomManager().getZoomPPD());
		int pannerModeOrd = Config.get("panner.mode", -1);
		if (pannerModeOrd >= 0 && pannerModeOrd < PannerMode.values().length) {
			Main.userProps.setPropertyInt("PannerMode", pannerModeOrd);
		}
		Main.userProps.setProperty("Initialx",		 String.valueOf( locMgr.getLoc().getX() ));  //this in World
		Main.userProps.setProperty("Initialy",		 String.valueOf( locMgr.getLoc().getY() ));  //this in World
		// note: JMARS west lon => USER east lon
		
		//using getCenterLon() API can lead to an incorrect Lon value display on session "load",
		//when lat = 0.0; the lon value will default to "180" on session "load",
		//regardless of what value user selected and saved session with. So, use getProjectionCenter(), instead.
		//Main.userProps.setProperty("Projection_lon",	     String.valueOf((360-Main.PO.getCenterLon())%360));  //can be wrong when lat=0
		//Main.userProps.setProperty("Projection_lat",	     String.valueOf(Main.PO.getCenterLat()));
		
		//using these API calls will reproject accurately on session "load" if lat=0.0; as well as for other lat values
		Main.userProps.setProperty("Projection_lon",	     String.valueOf((360-Main.PO.getProjectionCenter().getX())%360));
		Main.userProps.setProperty("Projection_lat",	     String.valueOf(Main.PO.getProjectionCenter().getY()));
		
		Main.userProps.setProperty("ServerOffset",	 String.valueOf(Main.PO.getServerOffsetX()));
		LManager.getLManager().saveState();
		
		// Set the general ruler properties.
		RulerManager.Instance.saveSettings();

		// Set the properties of any defined rulers.
		saveRulersState();

		// Set the properties of any defined views. 
		Main.userProps.setPropertyInt("ViewCount", mainWindow.viewList.size());
		Iterator iterViews = mainWindow.viewList.iterator();
		int i=1;
		while(iterViews.hasNext()) {
			Layer.LView lview = (Layer.LView) iterViews.next();
			String basename = "View" + String.valueOf(i);
			if(lview.originatingFactory == null)
				continue;
			
			Main.userProps.setProperty(basename, lview.originatingFactory.getClass().getName());

			//Store the views starting parms in a file if available
			SerializedParameters parms = lview.getInitialLayerData();
			if ( parms != null ) {
				Main.userProps.saveUserObject(basename + "Parms", parms);
			}
			
			String overlayFlag = "false";
			if (lview.isOverlay()) { 
				overlayFlag = "true";
			} 
			Main.userProps.setProperty(basename + "Overlay", overlayFlag);
			
			//Store the views current settings in a file if available
			Hashtable sparms = lview.getViewSettings();
			if ( sparms != null ) {
				Main.userProps.saveUserObject(basename + "Settings", sparms);
			}
			
			String customName = customLayerNames.get(lview);
			if (customName != null && customName.length() > 0) {
				Main.userProps.setProperty(basename + "Name", customName);
			}
			i++;
		}
		saveBodyLayerFiles();
	}
	private void saveBodyLayerFiles() {
		String path = Main.getBodyBaseDir();
		File directory = new File(Main.getJMarsPath()+path);
		if (directory.exists()) {
			File[] fileList = directory.listFiles();
			HashMap<String, ArrayList<SavedLayer>> bodyMap = new HashMap<String, ArrayList<SavedLayer>>();
			InputStream is = null;
			for(File oneFile : fileList) {
				try {
					 is = new FileInputStream(oneFile);
					ArrayList<SavedLayer> list = (ArrayList<SavedLayer>) SavedLayer.load(is);
					bodyMap.put(oneFile.getName(), list);
				} catch (Exception e) {
					log.aprintln(e);
					log.aprintln("Failed to save layer for "+oneFile.getName());
				} finally {
					try {
						is.close();
					} catch (IOException e) {
						log.println(e);
						e.printStackTrace();
					}
				}
			}
			Main.userProps.saveUserObject(Main.BODY_FILE_MAP_STR, bodyMap);
		}
	}
	// builds the views if there were any defined in the application properties, 
	public void buildViews()
	{
		//Determine if there were saved views
		int viewCnt = Main.userProps.getPropertyInt("ViewCount", 0);

		//Starting up from a JLF file
		if (Main.savedLayers != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					ArrayList<String> overlayList = new ArrayList<String>();
					Collections.reverse(Main.savedLayers);
					for (SavedLayer layer: Main.savedLayers) {
						if (layer.isOverlay) {
							overlayList.add(layer.layerName.trim());
						}
						layer.materialize();
					}
					if (!Main.mainFrame.isBodySwitching()) {
						//add in overlay layers but not when switching bodies
						for(LViewFactory overlayFactory : LViewFactory.cartographyList) {
							
							Layer.LView overlayView = overlayFactory.showCartographyLView();
							if (overlayList.contains(overlayView.getName().trim())) {
								continue;
							}
			                if (overlayView != null) {
			                    LManager.receiveCartographyLView(overlayView);
			                }
						}
						LManager.getLManager().getMainPanel().revalidateOverlayPanel();
					}
					
					Main.mainFrame.setBodySwitchingFlag(false);//reset the flag for body switching
					// saved layers could be large, so let the GC reap away
					Main.savedLayers = null;
				}
			});
		} else if ( viewCnt == 0 || ignorePreviousState ) {
			//Default startup. If no JLF is being used, we will start from factory defaults.
			//If a user has specified a startup JLF (option now available), we will use that.
			//If no startup JLF is found, we will look for a startup JLF for the body (set in our jmars.config).
			String startupMode = Config.get("startup_mode", "new");//new startup mode means that we load the startup URL (if it is set)
			String currentProduct = Config.get(Config.CONFIG_PRODUCT, "jmars");
			currentProduct = currentProduct.toLowerCase();
			String userStartupLocation = Main.getJMarsPath()+currentProduct+"_"+Main.getCurrentBody().toLowerCase()+"_start.jlf";//jmars home + product + body + startup.jlf
			File userStartupFile = new File(userStartupLocation);//user startup layer file if created by the user
			String startupUrl = Config.get(Util.getProductBodyPrefix()+"startup_file", null);//this is a startup JLF for the body, created by the JMARS team

			if (userStartupFile.exists()) {
				try {
					Util.loadSavedLayers(userStartupFile);
				} catch (Exception e) {
					log.aprintln("Unable to load startup layer JLF: "+userStartupLocation);
					for (LViewFactory factory : LViewFactory.factoryList) {
				        Layer.LView view = factory.createLView();
				        if(view != null) {
		                    LManager.receiveInitialLView(view);
		                }
				    }
				}
			} else if (startupUrl != null && "new".equalsIgnoreCase(startupMode)) {
				try {
					Util.loadSavedLayers(startupUrl, null);
				} catch (Exception e) {
					log.aprintln("Unable to load startup layer JLF: "+startupUrl);
					for (LViewFactory factory : LViewFactory.factoryList) {
				        Layer.LView view = factory.createLView();
				        if(view != null) {
		                    LManager.receiveInitialLView(view);
		                }
				    }
				}
			} else {
			    for (LViewFactory factory : LViewFactory.factoryList) {
			        Layer.LView view = factory.createLView();
			        if(view != null) {
	                    LManager.receiveInitialLView(view);
	                }
			    }
			}
		    for(LViewFactory overlayFactory : LViewFactory.cartographyList) {
                Layer.LView view = overlayFactory.showCartographyLView();
                if (view != null) {
                    LManager.receiveCartographyLView(view);
                }
            }
		} else {
			//Session file startup
			 try {
				//get overlay factories
				ArrayList<LViewFactory> overlayFactories = new ArrayList<LViewFactory>();
				for(LViewFactory overlayFactory : LViewFactory.cartographyList) {
	                overlayFactories.add(overlayFactory);
	            }
				ArrayList<LViewFactory> toRemove = new ArrayList<LViewFactory>();
				Layer.LView view = null;
				for ( int i=1; i <=viewCnt; i++ ) {
					String basename = "View" + String.valueOf(i);
					String factoryName = Main.userProps.getProperty(basename, "");
					try {
						// Look for a serialized initial parameter block and start the view with the
						// data if present.
						LViewFactory factory = LViewFactory.getFactoryObject(factoryName);
						// TODO: serialization formats change, and code to adapt from an
						// old form to a new form WILL ABSOLUTELY BE REQUIRED. We just
						// need a mechanism to make bolting in such code less of a hack.
						if (factory == null && factoryName.endsWith("StampFactory")) {
							factory = StampFactory.createAdapterFactory(factoryName);
						}
						
						if ( factory != null ) {
							SerializedParameters obj = (SerializedParameters) Main.userProps.loadUserObject(basename + "Parms");
							view = factory.recreateLView(obj);
							if (view != null) {
								//read the session and figure out if this view goes in the overlay section
								String overlayFlag = Main.userProps.getProperty(basename + "Overlay", null);//null means it was not set, older session
								if (overlayFlag == null) { //older session, we did not set the property
									if (LViewFactory.isOverlayType(view)) {//only work with the overlay types for this section
										if (!LManager.getLManager().checkOverlayTypeLoaded(view.getClass())) {//if we haven't loaded one of these yet
											view.setOverlayFlag(true);
										} else {
											view.setOverlayFlag(false);//set it to false so we know later
										}
									} else {
										view.setOverlayFlag(false);//set it to false so we know later
									}
								} else {//we have a flag set
									if (overlayFlag.trim().equalsIgnoreCase("true")) {
										view.setOverlayFlag(true);
									} else {
										view.setOverlayFlag(false);
									}
								}
								if (view.isOverlay()) {
									for (LViewFactory overlayFactory : overlayFactories) {
										if (factory.getClass() == overlayFactory.getClass()) {
											toRemove.add(overlayFactory);
											break;
										}
									}
									
								}
							}
							//done overlay
							
							if (view != null) {
								LManager.receiveSavedLView(view);
								Hashtable sobj =  (Hashtable) Main.userProps.loadUserObject(basename + "Settings");
								if ( sobj != null ){
									view.setViewSettings(sobj);
								}
								String customName = (String)Main.userProps.getProperty(basename + "Name");
								if (customName != null && customName.length() > 0) {
									customLayerNames.put(view, customName);
									LManager.getLManager().updateLabels();
								}
							}
						} else {
							log.aprintln("Failure recreating instance of " + factoryName);
						}
					} catch (Exception e) {
						log.aprintln("Failure recreating instance of " + factoryName + ", caused by:");
						log.aprintln(e);
					}
				}
				//instantiate any overlay layers that we are missing
				boolean olderSession = false;
				overlayFactories.removeAll(toRemove);
				for (LViewFactory overlayFactory : overlayFactories) {
					Layer.LView overlayView = overlayFactory.showCartographyLView();
	                if (overlayView != null) {
	                    LManager.receiveCartographyLView(overlayView);
	                    olderSession = true;
	                }
				}
				LManager.getLManager().loadState(olderSession);
			 } catch (Exception e) {
				 log.println("Error while loading session file: "+e.getMessage());
			 } finally {
				 //refresh main view
				 LManager.getLManager().updateVis();
			 }
		}
	} // end: buildViews()

	/** Recenters all LViewManagers to a new location given by p */
	public void offsetToWorld(Point2D p) {
		mainWindow.getGlassPanel().offsetToWorld(p);
		panner.getGlassPanel().offsetToWorld(p);
		locMgr.setLocation(p, true);
	}
	
	public Map<LView,String> getCustomLayerNames() {
		return customLayerNames;
	}
	
	public void setPannerMode(PannerMode mode) {
		if (panner == null || mode == null) {
			log.aprintln("Null panner or mode, panner controls programmed incorrectly");
		} else {
			switch (mode) {
			case Horiz:
				splitPane.setRightComponent(panner);
				splitPane.setDividerSize(new JSplitPane().getDividerSize());
				panner.setSize(mainWindow.getWidth(), 150);
				break;
			case Off:
				splitPane.setRightComponent(null);
				splitPane.setDividerSize(0);
				break;
			}
			Config.set("panner.mode", ""+mode.ordinal());
		}
	}
	
	public static enum PannerMode {
		// Must append new options to end of this enum, to avoid
		// changing the ordinates of existing values.
		Horiz("Horizontal"), Off("Off");
		PannerMode(String title) {
			this.title = title;
		}
		public final String title;
	}
	
	public void setLManagerMode(LManagerMode mode) {
		if (LManager.getLManager() == null || mode == null) {
		} else {
			switch (mode) {
			case Verti:
				totalPane.setLeftComponent(LManager.getLManager());
				totalPane.setDividerSize(new JSplitPane().getDividerSize());
				break;
			case Off:
				totalPane.setLeftComponent(null);
				totalPane.setDividerSize(0);
				break;
			}
		}
	}

	public static enum LManagerMode {
		Verti("Vertical"), Off("Off");
		LManagerMode(String title) {
			this.title = title;
		}

		public final String title;
	}
	
	public void resetDistanceNotifSnackbar() {
		int x,y;
		if (distanceNotif != null && distanceNotif.isRunning()) {			
			 if (distanceNotif.getOwner().getWidth() > (distanceNotif.getWidth() * 2)) {
                 x = (distanceNotif.getOwner().getX() + distanceNotif.getOwner().getWidth() - distanceNotif.getWidth() - distanceNotif.getMarginRight());
                 y = (distanceNotif.getOwner().getY() + distanceNotif.getOwner().getHeight() - distanceNotif.getHeight() - distanceNotif.getMarginBottom());
             } else {
                 x = (distanceNotif.getOwner().getX() + ((distanceNotif.getOwner().getWidth() - distanceNotif.getWidth()) / 2));
                 y = (distanceNotif.getOwner().getY() + distanceNotif.getOwner().getHeight() - distanceNotif.getHeight() - distanceNotif.getMarginBottom());
             }			
			Point point = new Point(x, y);
			distanceNotif.setLocation(point);
		}
	}
	
	public JLabel getQuickChartsLbl() {
		return quickChartsLbl;
	}
	
	//good place to  init "global" services that various UI components use
	//Data.SERVICE is used by Expression builder and is ready with data when component is ready
	public void initUIServices() {
		edu.asu.jmars.layer.shape2.xb.data.service.Data.SERVICE.init();
		XB.INSTANCE.init();
		DrawingPalette.INSTANCE.init();
	}	
}
