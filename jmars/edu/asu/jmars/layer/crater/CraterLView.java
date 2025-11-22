package edu.asu.jmars.layer.crater;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
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
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import edu.asu.jmars.Main;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.crater.profiler.ProfilerView;
import edu.asu.jmars.layer.shape2.ShapePath;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.GeomSource;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;


@SuppressWarnings("serial")
public class CraterLView extends LView {
	
	private Point mouseLocation = null;
	private Point prevMouseLoc;
	private MouseEvent moveEvent = null;
	// list of 3 point crater coords (uses screen coords), or used for click-drag circles (uses world coords)
	private ArrayList<Point2D> points = new ArrayList<Point2D>();
	
	private static final int ADD_MODE = 0;
	private static final int SELECT_MODE = 1;
	private static final int THREE_POINT_MODE = 2;
	private static final int CLICK_DRAG_MODE = 3;
	private int mode = ADD_MODE;
	
	private JMenu modeMenu;
	private JRadioButtonMenuItem addModeRadioButton;
	private JRadioButtonMenuItem selectModeRadioButton;
	private JRadioButtonMenuItem threePtModeRadioButton;
	private JRadioButtonMenuItem clickDragModeRadioButton;

	private static final String VIEW_SETTINGS_KEY = "crater";
	private CraterLayer clayer = (CraterLayer)getLayer();
	protected ArrayList<Crater> selectedCraters;

	private boolean isRubberband = false;
	private Point2D rubberbandBoxStart; // stored as local screen points
	private Point2D rubberbandBoxEnd; // stored as local screen points
	
	private Object createLock = new Object();
	
    // This is the set of craters that match the current filter settings
	ArrayList<Crater> matchingCraters;
    
	//@since 3.0.3 - make this object static so that the lock will be across instances of panner view and main view
	private static Object drawCratersLock = new Object();
	
	private JMenuItem centerOnSelected = new JMenuItem("Center on crater");
	
	
	/**
	 * Creates a new Crater LView with no LView3D associated
	 * with it (useful for the panner lview instance)
	 * @param layer
	 */
	public CraterLView(CraterLayer layer){
		this(layer, null);
	}
	
	public CraterLView(final CraterLayer layer, CraterLView3D lview3d){
		super(layer, lview3d);
		
		selectedCraters = layer.getSelectedCraters();
		matchingCraters = layer.getMatchingCraters();
		
		// Buffers for identified craters, selected craters, and the new crater stamp
		setBufferCount(3);
		
		addMouseMotionListener(new MouseMotionListener() {
		
			public void mouseMoved(MouseEvent e) {
				if(ToolManager.getToolMode() == ToolManager.SEL_HAND){
					if (mode == ADD_MODE || mode == THREE_POINT_MODE) {
						repaint();
						moveEvent=e;
						mouseLocation=e.getPoint();
					}
				}
			}
		
			public void mouseDragged(MouseEvent e)
			{
				if(ToolManager.getToolMode() == ToolManager.SEL_HAND)
				{
					if(isRubberband && mode == SELECT_MODE) // second conditional not really need, but JIC
					{
						// get point as local screen position
						Point loc = e.getPoint();
						Point2D screen = viewman.getProj().screen.toScreenLocal(loc.getX(), loc.getY());
						
						// set end point
						rubberbandBoxEnd = screen;
						repaint();
				    }
					else if(mode == SELECT_MODE && selectedCraters.size() > 0) // second conditional not really needed, but JIC
					{
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

						CraterTableModel tableModel = ((CraterFocusPanel)focusPanel).table.getTableModel();

						for(int i = 0; i<selectedCraters.size(); i++)
				        {
							// for each crater in selected craters
							Crater craterToMove = selectedCraters.get(i);
							
							// move to new location by setting table model value
							int row = tableModel.getRow(craterToMove);
							tableModel.setValueAt(craterToMove.getLon() + deltaX, row, CraterTableModel.CENTERLON_COLUMN);
							tableModel.setValueAt(craterToMove.getLat() + deltaY, row, CraterTableModel.CENTERLAT_COLUMN);
						}
						prevMouseLoc = loc;	// previous mouse location had changed
					}
					else if(mode == CLICK_DRAG_MODE){
						if (points.size() > 0) {
							Point loc = e.getPoint();
					        Point2D world = viewman.getProj().screen.toWorld(loc);
							
							switch (points.size()) {
							case 1: points.add(world); break;
							case 2: points.set(1, world); break;
							}
							repaint();
						}
					}
				}
			}
		});
		
		addMouseListener(new MouseListener(){		
			public void mouseReleased(MouseEvent e) {
				if(isRubberband && mode == SELECT_MODE) // second conditional not really needed, but JIC
				{
					ArrayList<Crater> cratersToSelect = new ArrayList<Crater>(); 
					if (e.isControlDown())
					{
						cratersToSelect.addAll(selectedCraters);
					}

					// get rubberbandBox boundaries from start and end points
					double minX = Math.min(rubberbandBoxStart.getX(), rubberbandBoxEnd.getX());
					double maxX = Math.max(rubberbandBoxStart.getX(), rubberbandBoxEnd.getX());
					double minY = Math.min(rubberbandBoxStart.getY(), rubberbandBoxEnd.getY());
					double maxY = Math.max(rubberbandBoxStart.getY(), rubberbandBoxEnd.getY());

					CraterSettings settings = getSettings();
					// for each crater in the crater layer
					for (Crater c : settings.craters)
					{
						// convert crater spatial point to local screen point
						Point2D spatialPt = new Point2D.Double(360-c.getLon(), c.getLat());
						Point2D worldPt = Main.PO.convSpatialToWorld(spatialPt);
						Point2D p = viewman.getProj().world.toScreen(worldPt);
						p = viewman.getProj().screen.toScreenLocal(p);
						
						// add crater if it is within rubberband box bounds
						if(minX < p.getX() && p.getX() < maxX && minY < p.getY() && p.getY() < maxY)
						{
							cratersToSelect.add(c);
						}
					}
					selectCraters(cratersToSelect);
					isRubberband = false; // don't forget to turn off
					repaint();
				}
				else if(mode == CLICK_DRAG_MODE){
					if(points.size() == 2){
						//draw real crater
						Crater newCrater = getCurrentCircle();
						
						CraterFocusPanel fp = (CraterFocusPanel)getFocusPanel();
						boolean meetsFilter=fp.meetsFilter(newCrater.getLon(), newCrater.getLat(), newCrater.getDiameter());
						
						if(!clayer.settings.filterMainView || meetsFilter){
							addCraterToLayer(newCrater);
						}
						
						points.clear();
					}
				}
			}
			
			public void mousePressed(MouseEvent e) {
				prevMouseLoc = e.getPoint();
				
				if (ToolManager.getToolMode() == ToolManager.SEL_HAND){
					if (e.getButton()==MouseEvent.BUTTON1) {
						if (mode == ADD_MODE) {
							Point loc = e.getPoint();
						
							Point2D world = viewman.getProj().screen.toWorld(loc);
							
							Point2D spatial = Main.PO.convWorldToSpatial(world);
						
							Crater newCrater = new Crater();
							newCrater.setUser(Main.USER);
							newCrater.setLon(360-spatial.getX());
							newCrater.setLat(spatial.getY());
						
							CraterSettings settings = getSettings();
							
							if (settings.toggleDefaultCraterSizeReset) {
								newCrater.setDiameter(settings.getNextSize());
								settings.setNextSize(settings.getDefaultSize());
								((CraterFocusPanel)getFocusPanel()).newCraterSize.setText(""+settings.getNextSize());
								repaint();
							} else {
								newCrater.setDiameter(settings.getNextSize());
							};
						
							newCrater.setColor(settings.nextColor);
							newCrater.setComment(settings.colorToNotesMap.get(settings.nextColor));
							
							CraterFocusPanel fp = (CraterFocusPanel)getFocusPanel();
	
							boolean meetsFilter=fp.meetsFilter(360-spatial.getX(), spatial.getY(), settings.getNextSize());
							
							if (!settings.filterMainView || meetsFilter) {
								addCraterToLayer(newCrater);
							} else {
								return;
							}
						} else if (mode == THREE_POINT_MODE) {
							WrappedMouseEvent wme = (WrappedMouseEvent)e;
							Point loc=wme.getRealPoint();
							
							Point2D screen = new Point2D.Double(loc.getX(), loc.getY());
						
							Point2D world = viewman.getProj().screen.toWorld(loc);
						
							Point2D spatial = Main.PO.convWorldToSpatial(world);
						
							// need to add check here to verify the point falls within any filter parameters
							CraterFocusPanel fp = (CraterFocusPanel)getFocusPanel();
						
							boolean meetsPointFilter=fp.pointMeetsFilter(360.0-spatial.getX(), spatial.getY());
	
							CraterSettings settings = getSettings();
							
							if (settings.filterMainView && !meetsPointFilter) {
								Util.showMessageDialog("You are trying to add a point outside your filtered region", "Outside of current crater filter", JOptionPane.ERROR_MESSAGE);
								return;
							}
						
							points.add(screen);						
						
							if (points.size() == 3) {
								try {
									System.out.println("0 = " + points.get(0));
									System.out.println("1 = " + points.get(1));
									System.out.println("2 = " + points.get(2));
									
									
									Crater crater = Crater.craterFrom3Points(points.get(0), 
																			points.get(1), 
																			points.get(2));
								
									Point2D worldCenter = viewman.getProj().screen.toWorld(new Point2D.Double(crater.getLon(), crater.getLat()));
									Point2D spatialCenter = Main.PO.convWorldToSpatial(worldCenter);
								
									// distance from center of the crater to one of the points on the rim in kilometers
									double linDia = 2.0*Util.angularAndLinearDistanceWorld(world, worldCenter)[1];
							    	crater.setLon(360.0-spatialCenter.getX());
							    	crater.setLat(spatialCenter.getY());
							    	crater.setDiameter(linDia*1000.0);	// crater diameter needs to be in meters
									crater.setUser(Main.USER);
									crater.setColor(settings.nextColor);
									crater.setComment(settings.colorToNotesMap.get(settings.nextColor));
									boolean meetsFilter=fp.meetsFilter(360.0-spatialCenter.getX(), spatialCenter.getY(), crater.getDiameter());
	
									if (!settings.filterMainView || meetsFilter) {
										addCraterToLayer(crater);
									} else {
										return;
									}
								} catch (Exception ex) {
									// pop up informational dialog to user
									Util.showMessageDialog(ex.getMessage(), "Crater definition issue", JOptionPane.ERROR_MESSAGE);
								}
							
								points.clear();
							}
						}
						else if (mode == SELECT_MODE)
						{
							Crater crater = findCraterByScreenPt(e.getPoint());
							if(crater == null || !selectedCraters.contains(crater))
							{
								// get initial point as local screen position
								Point loc = e.getPoint();
								Point2D screen = viewman.getProj().screen.toScreenLocal(loc.getX(), loc.getY());
								
								// set initial points for rubberband box
								isRubberband = true;
								rubberbandBoxStart = screen;
								rubberbandBoxEnd = screen;
							}
							repaint();
						}
						else if (mode == CLICK_DRAG_MODE){
							points.clear();
							Point loc = e.getPoint();
					        Point2D world = viewman.getProj().screen.toWorld(loc);
							points.add(world);
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
				CraterLView.this.requestFocusInWindow();
			}
	
			public void mouseClicked(MouseEvent e) {
				if (ToolManager.getToolMode() == ToolManager.SEL_HAND && mode == SELECT_MODE)
				{
					CraterSettings settings = getSettings();
					if (settings.craters != null) {
						Crater crater = findCraterByScreenPt(e.getPoint());
					
						ArrayList<Crater> cratersToSelect=new ArrayList<Crater>();
						
						if (e.isControlDown()) {
							cratersToSelect.addAll(selectedCraters);
						}
					
						if (crater!=null) {
							if (!selectedCraters.contains(crater)) {
								cratersToSelect.add(crater);
							} else {
								cratersToSelect.remove(crater);
							}
						}
	
						selectCraters(cratersToSelect);
						repaint();
					}
				}
			}
		});		
	
		addMouseWheelListener(new MouseWheelListener(){
	
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
					
					ColorCombo cc = ((CraterFocusPanel)getFocusPanel()).newCraterColor;
					if (colorKeyDown && (mode == ADD_MODE || mode == THREE_POINT_MODE)) {
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
					else if(colorKeyDown && mode == SELECT_MODE)
					{
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
						
						CraterTableModel tableModel = ((CraterFocusPanel)focusPanel).table.getTableModel();

						for(int i = 0; i<selectedCraters.size(); i++)
						{
							// for each crater in selected craters
							Crater craterToEdit = selectedCraters.get(i);
				    		
							// change color by setting table value
							int row = tableModel.getRow(craterToEdit);
							tableModel.setValueAt(cc.getSelectedItem(), row, CraterTableModel.COLOR_COLUMN);
				    	}
				        return;
					}
				
					if (mode == ADD_MODE) {
						int clicks = e.getWheelRotation();
					
						int increment=10;
					
						if (e.isShiftDown()) {
							increment=1;
						}
											
						double newSize;
						if (clicks>0) {
							newSize=decreaseSize(increment);
						} else {
							newSize=increaseSize(increment);
						}
					
						((CraterFocusPanel)getFocusPanel()).newCraterSize.setText(""+newSize);
					
						repaint();
					}
					if(mode == SELECT_MODE)
					{
			        	int clicks = e.getWheelRotation();
			        	int increment = 10;

						if (e.isShiftDown())
						{
							increment=1;
						}
						
						CraterTableModel tableModel = ((CraterFocusPanel)focusPanel).table.getTableModel();
						
						for(int i = 0; i<selectedCraters.size(); i++)
				        {
							// for each crater in selected craters
							Crater craterToEdit = selectedCraters.get(i);
				    		
				    		// Increment/Decrement crater size by step amount
							double newSize = craterToEdit.getDiameter();
							if (clicks>0) // decrement
							{
								newSize = decreaseSelectedSize(newSize, increment);
							}
							else
							{
								newSize = increaseSelectedSize(newSize, increment);
							}
				        	
							// change diameter by setting table value
							int row = tableModel.getRow(craterToEdit);
							tableModel.setValueAt(newSize, row, CraterTableModel.DIAMETER_COLUMN);
				    	}
					}
				}
			}
		});
	
		// Not currently working due to key events not being passed to
		// the LView
		addKeyListener(new KeyListener() {
	
			public void keyTyped(KeyEvent e) {
				if (ToolManager.getToolMode() == ToolManager.SEL_HAND){
					if (mode == ADD_MODE) {
						Character c=e.getKeyChar();
					
						int increment=10;
						if (e.isControlDown()) {
							increment=1;
						}
						if (c=='[') {
							decreaseSize(10);
						} else if (c==']') {
							increaseSize(10);
						} else if (c=='{') {
							decreaseSize(1);
						} else if (c=='}') {
							increaseSize(1);					
						} else if (c=='+') {
							((CraterFocusPanel)getFocusPanel()).newCraterSize.setText(""+increaseSize(increment));
						} else if (c=='-') {
							((CraterFocusPanel)getFocusPanel()).newCraterSize.setText(""+decreaseSize(increment));
						} else if ((c=='c' || c=='C')) {
							Point screenLoc = MouseInfo.getPointerInfo().getLocation();
							Point lviewLoc = getLocationOnScreen();
							Point loc = new Point(screenLoc.x-lviewLoc.x,screenLoc.y-lviewLoc.y);
							
							Point2D world = viewman.getProj().screen.toWorld(loc);
							
							Point2D spatial = Main.PO.convWorldToSpatial(world);
						
							Crater newCrater = new Crater();
							newCrater.setUser(Main.USER);
							newCrater.setLon(360-spatial.getX());
							newCrater.setLat(spatial.getY());
						
							CraterSettings settings = getSettings();
							
							if (settings.toggleDefaultCraterSizeReset) {
								newCrater.setDiameter(settings.getNextSize());
								settings.setNextSize(settings.getDefaultSize());
								((CraterFocusPanel)getFocusPanel()).newCraterSize.setText(""+settings.getNextSize());
								repaint();
							} else {
								newCrater.setDiameter(settings.getNextSize());
							};
						
							newCrater.setColor(settings.nextColor);
							newCrater.setComment(settings.colorToNotesMap.get(settings.nextColor));
							
							CraterFocusPanel fp = (CraterFocusPanel)getFocusPanel();
	
							boolean meetsFilter=fp.meetsFilter(360-spatial.getX(), spatial.getY(), settings.getNextSize());
							
							if (!settings.filterMainView || meetsFilter) {
								addCraterToLayer(newCrater);
							} else {
								return;
							}
							addCraterToLayer(newCrater);
						} else if ((c=='s'||c=='S')) {
							setMode(SELECT_MODE);
						} 
									 
						repaint();
					}
					if(mode == SELECT_MODE)
					{
						Character c = e.getKeyChar();

						if ((c=='d'||c=='D')||c==KeyEvent.VK_DELETE) {
							deleteSelectedCraters();
							return;
						} else if ((c=='a'||c=='A')) {
							setMode(ADD_MODE);
							return;
						}
						
						int increment = 10;
						if(e.isControlDown())
						{
							increment = 1;
						}
						
						CraterTableModel tableModel = ((CraterFocusPanel)focusPanel).table.getTableModel();
						
						for(int i = 0; i<selectedCraters.size(); i++)
				        {
							// for each crater in selected craters
							Crater craterToEdit = selectedCraters.get(i);
				    		
				    		// Increment/Decrement crater size by step amount
							double newSize = craterToEdit.getDiameter();
							if (c=='[')
							{
								newSize = decreaseSelectedSize(newSize, 10);
							}
							else if (c==']')
							{
								newSize = increaseSelectedSize(newSize, 10);
							}
							else if (c=='{')
							{
								newSize = decreaseSelectedSize(newSize, 1);
							}
							else if (c=='}')
							{
								newSize = increaseSelectedSize(newSize, 1);
							}
							else if (c=='+')
							{
								newSize = increaseSelectedSize(newSize, increment);
							}
							else if (c=='-')
							{
								newSize = decreaseSelectedSize(newSize, increment);
							}
							
							// change diameter by setting table value
							int row = tableModel.getRow(craterToEdit);
							tableModel.setValueAt(newSize, row, CraterTableModel.DIAMETER_COLUMN);
						}
					}
				}
			}
	
			public void keyReleased(KeyEvent e) {
			}
	
			public void keyPressed(KeyEvent e) {
			}
		});		
		
		setUpModeContextMenu();
		setMode(mode);
	}
	
	private void setMode(int m){
		mode = m;
		switch(mode){
		case ADD_MODE:
			addModeRadioButton.setSelected(true);
			setCursor(Cursor.getDefaultCursor());
			break;
		case SELECT_MODE:
			selectModeRadioButton.setSelected(true);
			setCursor(Cursor.getDefaultCursor());	
			break;
		case THREE_POINT_MODE:
			threePtModeRadioButton.setSelected(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			break;
		case CLICK_DRAG_MODE:
			clickDragModeRadioButton.setSelected(true);
			setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			break;
		}
	}
	
	
	public FocusPanel getFocusPanel() {
		if (focusPanel==null) {
			synchronized(createLock) {
				if (focusPanel==null) {
					if (getChild()==null && getParentLView()!=null) {
						focusPanel=getParentLView().getFocusPanel();
					} else {
						focusPanel=new CraterFocusPanel(this);
					}
					
				}
			}
		}
		return focusPanel; 
	}
	
	private void addCraterToLayer(Crater newCrater){
		CraterSettings settings = getSettings();
		settings.craters.add(newCrater);
		((CraterTableModel)((CraterFocusPanel)focusPanel).table.getUnsortedTableModel()).addRow(newCrater);
		drawCraters();
		repaint();
		((CraterLView)getChild()).repaint();
		//update 3D
		clayer.increaseStateId(0);
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
	
	// Increment options:
	// 1km
	// 100m
	// 10m
	// 1m
	// Arbitrary meters
		
	public synchronized void setMeterIncrement(double newIncrement) {
		CraterSettings settings = getSettings();
		settings.setMeterIncrement(newIncrement);
	}
	
	// Increase the size of the next crater that will be created
	// by whatever increment the user has chosen
	public synchronized double increaseSize(int numIncrements) {
		CraterSettings settings = getSettings();
		settings.setNextSize(settings.getNextSize() + settings.getMeterIncrement()*numIncrements);
		return settings.getNextSize();		
	}

	// Decrease the size of the next crater that will be created
	// by whatever increment the user has chosen
	public synchronized double decreaseSize(int numIncrements) {
		CraterSettings settings = getSettings();
		settings.setNextSize(settings.getNextSize() - settings.getMeterIncrement()*numIncrements);
		if (settings.getNextSize()<0) settings.setNextSize(0);
		return settings.getNextSize();		
	}
	
	// Increase the current size of a crater
	// by whatever increment the user has chosen
	public synchronized double increaseSelectedSize(double prevSize, int numIncrements) {
		CraterSettings settings = getSettings();
		prevSize+=settings.getMeterIncrement()*numIncrements;
		return prevSize;
	}
	
	// Decrease the current size of a crater
	// by whatever increment the user has chosen
	public synchronized double decreaseSelectedSize(double prevSize, int numIncrements) {
		CraterSettings settings = getSettings();
		prevSize-=settings.getMeterIncrement()*numIncrements;
		if (prevSize<0) prevSize=0;
		return prevSize;		
	}	
	
	private void setUpModeContextMenu(){
		//only create the mode menu and all it's items once
		// and just re-show that every time after
		if(modeMenu == null){
			modeMenu = new JMenu("Crater mode");
			
			addModeRadioButton = new JRadioButtonMenuItem(new AbstractAction("Add mode"){
				public void actionPerformed(ActionEvent e) {
					setMode(ADD_MODE);
					CraterLView.this.points.clear();
				}
			});
			
			selectModeRadioButton= new JRadioButtonMenuItem(new AbstractAction("Select mode"){
				public void actionPerformed(ActionEvent e) {
					setMode(SELECT_MODE);
				}
			});
			
			threePtModeRadioButton = new JRadioButtonMenuItem(new AbstractAction("Three point mode"){
				public void actionPerformed(ActionEvent e) {
					setMode(THREE_POINT_MODE);
					points.clear();
				}
			});
			
			clickDragModeRadioButton = new JRadioButtonMenuItem(new AbstractAction("Click-drag mode") {
				public void actionPerformed(ActionEvent e) {
					setMode(CLICK_DRAG_MODE);
					points.clear();
				}
			}); 
			
			ButtonGroup group = new ButtonGroup();
			group.add(addModeRadioButton);
			group.add(selectModeRadioButton);
			group.add(threePtModeRadioButton);
			group.add(clickDragModeRadioButton);
	
			
			modeMenu.add(addModeRadioButton);
			modeMenu.add(selectModeRadioButton);
			modeMenu.add(threePtModeRadioButton);
			modeMenu.add(clickDragModeRadioButton);
		}
	}
	
	protected Component[] getContextMenuTop(Point2D worldPt)
	{
		if(viewman.getActiveLView().equals(this)){
			List<Component> newItems =
				new ArrayList<Component>( Arrays.asList(super.getContextMenuTop(worldPt)) );
	
			newItems.add(0, modeMenu);
			
			JMenuItem removeCraters = new JMenuItem("Remove selected craters");
			removeCraters.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteSelectedCraters();
	
					CraterTable table = ((CraterFocusPanel)focusPanel).table;
					table.getSorter().clearSorts();
					CraterLView.this.repaint();
					
					drawSelectedCraters();
					CraterLView.this.getChild().repaint();
				}
			});
			
			newItems.add(1, removeCraters);
			
			JMenuItem removeLastCrater = new JMenuItem("Remove last crater");
			removeLastCrater.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					deleteLastCrater();
	
					CraterLView.this.repaint();
					
					drawSelectedCraters();
					CraterLView.this.getChild().repaint();
				}
			});
			
			newItems.add(2, removeLastCrater);
				
			centerOnSelected.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					CraterSettings settings = getSettings();
					if (settings.craters != null) {
						Crater crater = findCraterByWorldPt(worldPt);
						if (crater != null && selectedCraters.size() > 0) {
							if (selectedCraters.contains(crater)) {
								centerOnSelectedCrater(crater);
							}
						}
					}
				}
			});		
			centerOnSelected.setEnabled(isClickedOnSelectedCrater(worldPt));
			newItems.add(3, centerOnSelected);

			return (Component[]) newItems.toArray(new Component[0]);
		} else {
			return new Component[0];
		}
	}
	
	private Crater findCraterByScreenPt(Point screenPt)
	{
		MultiProjection proj = getProj();
		if (proj == null) {
			return null;
		}

		Point2D worldPt = proj.screen.toWorld(screenPt);

		return  findCraterByWorldPt(worldPt);
	}
	
    private double calculateDistance(Point2D p1, Point2D p2, MultiProjection proj){
    	double angDistance = proj.spatial.distance(p1, p2);
    	double linDistance = angDistance * Util.MEAN_RADIUS * 2*Math.PI / 360.0;
    	
    	return linDistance;
    }
	
	// Ported from stamp layer
	private Crater findCraterByWorldPt(Point2D worldPt)
	{
		//@since remove viewman2
		if (!isVisible() || viewman == null) { 
			return null;
		}

		MultiProjection proj = viewman.getProj();
		if (proj == null) {
			return null;
		}

		CraterSettings settings = getSettings();
		for (Crater crater : settings.craters) {
			// check distance.  If it's within the radius, we have a match.			
			double distance = calculateDistance(Main.PO.convWorldToSpatial(worldPt), new Point2D.Double(360-crater.getLon(), crater.getLat()), getProj());
			
			if (distance < (crater.getDiameter()/2.0/1000.0)) {
				return crater;
			} 
		}

		return  null;
	}
	
	public synchronized void drawSelectedCraters() {
		clearOffScreen(1);
		Graphics2D g2w=getOffScreenG2(1);
		
		if (g2w==null) {
			return;
		} 
		
		ProfilerView pv = ((CraterFocusPanel)getFocusPanel()).getProfilerView();
		
		for (Crater c : selectedCraters) {
			Color color = c.getColor();
	
			int red = 0;
			int green = 0;
			int blue = 0;
			
			if (color.getRed()<128) {
				red=255;
			}
			
			if (color.getGreen()<128) {
				green=255;
			}

			if (color.getBlue()<128) {
				blue=255;
			}
						
			color = new Color(red,green,blue);
			g2w.setColor(color);

			Shape craterShape = c.getProjectedShape(); 
			
	        g2w.setComposite(AlphaComposite.Src);

	        CraterSettings settings = getSettings();
	        float strokeSize = (float)(settings.craterLineThickness/(viewman.getZoomManager().getZoomPPD()*1.0));

	        g2w.setStroke(new BasicStroke(strokeSize));

			g2w.draw(craterShape);

			//if only one crater is selected, draw profiles if they exist and draw them with their respective color
			if (selectedCraters.size() ==1 && pv!=null && pv.isVisible()) {
				HashMap<Shape, Color> pathToColor = pv.getPathToColorMap();
				for(Shape path : pathToColor.keySet()){
					//is possible to have a null path if it is the "average" ProfileData
					if(path != null){
						//get the color
						Color profileColor = pathToColor.get(path);
						g2w.setColor(profileColor);
						g2w.draw(path);
					}
				}
			}
		}						
	}
	
    void drawCraters() {	
    	synchronized(drawCratersLock) {
    		
		CraterFocusPanel fp = (CraterFocusPanel)getFocusPanel();
		CraterSettings settings = getSettings();

		ArrayList<Crater> cratersToDraw = null;

    	if (getChild()!=null) {  // mainView logic
    		if (settings.craters!=null) {
    			fp.updateStats((ArrayList<Crater>)settings.craters.clone());
    		}
   
    		((CraterLView)getChild()).drawCraters();
    		
    		if (settings.filterMainView) {
    			cratersToDraw = matchingCraters;
    		} else {
    			cratersToDraw = settings.craters;
    		}    		
    	} else {  // panner logic
    		if (settings.filterPanView) {
    			cratersToDraw = ((CraterLView)getParentLView()).matchingCraters;
    		} else {
    			cratersToDraw = settings.craters;
    		}
    	}
		
		clearOffScreen(0);
		Graphics2D g2w=getOffScreenG2(0);
		
		if (g2w==null) {
			return;
		} 

//		g2=viewman.wrapScreenGraphics(g2);

		if (!cratersToDraw.containsAll(selectedCraters)) {
			ArrayList<Crater> cratersToDeselect = (ArrayList<Crater>)selectedCraters.clone();
			
			selectedCraters.retainAll(cratersToDraw);
			cratersToDeselect.removeAll(selectedCraters);
			
			for (Crater c: cratersToDeselect) {
				
				int row=fp.table.getSorter().sortRow(fp.table.getTableModel().getRow(c));
				fp.table.getSelectionModel().removeSelectionInterval(row,row);
			}

			drawSelectedCraters();
		}
		
		for (Crater c : cratersToDraw) {
			Color color = c.getColor();
			color=new Color((settings.alpha<<24) | (color.getRGB()&0xFFFFFF), true);
			g2w.setColor(color);

			Shape craterShape = c.getProjectedShape(); 
			
			if (settings.filterCraterFill) {
		        g2w.fill(craterShape);		        
			} else {
				float strokeSize = (float)(settings.craterLineThickness/(viewman.getZoomManager().getZoomPPD()*1.0));
				
		        g2w.setStroke(new BasicStroke(strokeSize));		        
		        g2w.draw(craterShape);		        
			}
		}
    	}
	}
	
	private void selectCraters(List<Crater> cratersToSelect) {
		//the table listener clears the selected craters list and
		// redraws the selected craters
		((CraterFocusPanel)focusPanel).table.selectRows(cratersToSelect);
		
		//increase state id, call 3d redraw
		getLayer().increaseStateId(0);
		if(ThreeDManager.isReady()){
			ThreeDManager.getInstance().updateDecalsForLView(this, true);
		}
	}
	
	protected void deleteSelectedCraters() {
		CraterSettings settings = getSettings();
		settings.craters.removeAll(selectedCraters);
		selectedCraters.clear();
		
		CraterTable table = ((CraterFocusPanel)focusPanel).table;
		table.getTableModel().removeAll();
		for (Crater c: settings.craters) {
			table.getTableModel().addRow(c);
		}
		table.clearSelection();

		drawCraters();
		drawSelectedCraters();
		
		//increase state id, call 3d redraw
		getLayer().increaseStateId(0);
		if(ThreeDManager.isReady()){
			ThreeDManager.getInstance().updateDecalsForLView(this, true);
		}
	}
	
	protected void deleteLastCrater() {
		CraterSettings settings = getSettings();
		if (settings.craters.size() > 0) {
			Crater crater = settings.craters.get(settings.craters.size() - 1);
			if (settings.craters.remove(crater)) {
				CraterTable table = ((CraterFocusPanel)focusPanel).table;
				table.getTableModel().removeAll();
				for (Crater c: settings.craters) {
					table.getTableModel().addRow(c);
				}
				table.clearSelection();
			}
		}
		drawCraters();
		drawSelectedCraters();
	}
	
	/**
	 * Uses the first two points in {@link #points} as opposite edges of a
	 * circle and returns a new Crater describing that circle. Returns null if
	 * there are not enough points.
	 */
	private Crater getCurrentCircle() {
		CraterSettings settings = getSettings();
		Crater crater = null;
		
		if(points.size() >= 2){
			crater = new Crater();
			crater.setUser(Main.USER);
			Color color = settings.nextColor;
			crater.setColor(color);
			crater.setComment(settings.colorToNotesMap.get(color));
			
			//world coordinate points
			Point2D a = points.get(0);
			Point2D b = points.get(1);
			
			if (Math.abs(b.getX() - a.getX()) > 180) {
				b.setLocation(b.getX() + 360*Math.signum(a.getX() - b.getX()), b.getY());
			}
			
			Point2D mid = new Point2D.Double(a.getX()/2 + b.getX()/2, a.getY()/2 + b.getY()/2);
			
			//convert to spatial
			Point2D midSp = Main.PO.convWorldToSpatial(mid);
			crater.setLon(360-midSp.getX());
			crater.setLat(midSp.getY());
			//calculate radius distance
			double diameter = Util.angularAndLinearDistanceW(a, b, getProj())[1];
			crater.setDiameter(diameter*1000);
		}
		
		return crater;
	}
	
	public synchronized void paintComponent(Graphics g) {
		CraterSettings settings = getSettings();
		// Don't try to draw unless the view is visible
		if (!isVisible() || viewman == null) {//@since remove viewman2
			return;
		}

		Color color = settings.nextColor;

		clearOffScreen(2);
		Graphics2D g2=getOffScreenG2Direct(2);
		Graphics2D g2w = getOffScreenG2(2);

		if (g2==null) {
			return;
		} 

		g2=viewman.wrapScreenGraphics(g2);

		color=new Color((settings.alpha<<24) | (color.getRGB()&0xFFFFFF), true);
		g2.setColor(color);
			
		if (ToolManager.getToolMode() == ToolManager.SEL_HAND){
			if (mode == ADD_MODE && mouseLocation!=null) {
				Point2D world = viewman.getProj().screen.toWorld(mouseLocation);
				Point2D spatial = Main.PO.convWorldToSpatial(world);
				
				Point2D vert[]=new Point2D[1];
				vert[0]=new Point2D.Double(spatial.getX(), spatial.getY());
				FPath path = new FPath(vert, FPath.SPATIAL_WEST, false);
					
				double radius = settings.getNextSize() / 2 / 1000; // kilometers
					
				Shape craterShape = GeomSource.getCirclePath(path, radius, 36).getWorld().getShape(); 
				
				float strokeSize = (float)(settings.craterLineThickness/(viewman.getZoomManager().getZoomPPD()*1.0));

		        g2w.setStroke(new BasicStroke(strokeSize));

				g2w.setColor(color);

				g2w.draw(craterShape);
			
				g2w.setColor(new Color(Color.white.getRGB() & 0xFFFFFF));
			}
		}
		
		if(isRubberband)
		{
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
		
		
		if (mode == ADD_MODE && mouseLocation!=null) {	
			
			Point loc = mouseLocation.getLocation();
			
			Point2D world = viewman.getProj().screen.toWorld(loc);
			
			Point2D spatial = Main.PO.convWorldToSpatial(world);
			
			CraterFocusPanel fp = (CraterFocusPanel)getFocusPanel();
			
			// Test whether this matches the current filter
						
			if (moveEvent!=null && moveEvent instanceof WrappedMouseEvent) {
				WrappedMouseEvent wme = (WrappedMouseEvent)moveEvent;
				mouseLocation=wme.getRealPoint();
			}
			boolean meetsFilter=fp.meetsFilter(360-spatial.getX(), spatial.getY(), settings.getNextSize());
			
			if (!settings.filterMainView || meetsFilter) {
				g.setColor(Color.white);
				
				String diameterString ="";
				
				DecimalFormat diameterFormatter = new DecimalFormat("0.##");

				if(settings.filterVisibleDiameter) {
					double meters = settings.getNextSize();
					
					if (meters>500) {
						diameterString=""+diameterFormatter.format(meters/(1000.0))+" km";
					} else {
						diameterString=""+diameterFormatter.format(meters)+" m";
					}
				} 
				
				g.drawString(diameterString, (int)mouseLocation.getX()-10, (int)mouseLocation.getY()-5);
				
			} else {
				g.setColor(Color.red);
				g.drawString("Outside of current filter", (int)mouseLocation.getX()-50, (int)mouseLocation.getY()-5);
			}
		}
		
		if (mode == THREE_POINT_MODE && mouseLocation!=null) {
			
			Point loc = mouseLocation.getLocation();
			
			Point2D world = viewman.getProj().screen.toWorld(loc);
			
			Point2D spatial = Main.PO.convWorldToSpatial(world);
						
			CraterFocusPanel fp = (CraterFocusPanel)getFocusPanel();
			
			if (moveEvent!=null && moveEvent instanceof WrappedMouseEvent) {
				WrappedMouseEvent wme = (WrappedMouseEvent)moveEvent;
				mouseLocation=wme.getRealPoint();
			}			
			boolean meetsFilter=fp.pointMeetsFilter(360-spatial.getX(), spatial.getY());
			
			if (!settings.filterMainView || meetsFilter) {
			
				for (Point2D screenPt: points) {
					
					g.setColor(Color.red);
					
					g.fillOval((int)screenPt.getX() - 2, (int)screenPt.getY() - 2, 4, 4);
				}
			} else {
				g.setColor(Color.red);
				g.drawString("Outside of current crater filter", (int)mouseLocation.getX()-50, (int)mouseLocation.getY()-5);
			}
		}
		
		//Draw the outline of what the crater would be if the user
		// releases the mouse drag
		if (mode == CLICK_DRAG_MODE){
			if(points.size()>=2){
				g2w.setColor(Color.WHITE);
				
				Shape craterShape = getCurrentCircle().getProjectedShape(); 
				
				//draw outline
				float strokeSize = (float)(settings.craterLineThickness/(viewman.getZoomManager().getZoomPPD()*1.0));
		        g2w.setStroke(new BasicStroke(strokeSize));		        
		        g2w.draw(craterShape);	
			}
		}
		
		// super.paintComponent draws the back buffers onto the layer panel
		super.paintComponent(g);
	}

	protected LView _new() {
		// Create a copy of ourself for use in the panner-view.
		return new CraterLView((CraterLayer)getLayer());
	}

	protected Object createRequest(Rectangle2D where) {
		// Build a request object for the layer.
		// The layer will respond back with the data.
		return where;
	}

	public void receiveData(Object layerData) {
		// Process the data returned by the layer.
		// Including displaying the data to the screen.
		drawCraters();
		drawSelectedCraters();
		repaint();
	}
	
	public String getName() {
		return "Crater Counting";
	}

	protected synchronized void updateSettings(boolean saving) {
		CraterSettings settings = clayer.settings;
		if (saving) {
			// save settings into hashtable
			Map<String, Object> layer = new HashMap<String, Object>();

			FeatureCollection fc = new SingleFeatureCollection();

			for (Crater c : settings.craters) {
				fc.addFeature(c.getFeature(settings));
			}

			// will reload this file from within the session file
			Field[] schema = ((List<Field>) fc.getSchema()).toArray(new Field[0]);
			layer.put("schema", schema);
			Object[][] values = new Object[settings.craters.size()][];
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

			viewSettings.put(VIEW_SETTINGS_KEY, settings);
			viewSettings.put(VIEW_SETTINGS_KEY + "-data", layer);
		} else {
			if (viewSettings.containsKey(VIEW_SETTINGS_KEY)) {
				clayer.settings = (CraterSettings) viewSettings.get(VIEW_SETTINGS_KEY);
				if (clayer.settings != null) {
				} else {
					// Restore failed
					clayer.settings = new CraterSettings();
				}
			}

			settings = clayer.settings;
			//set the Focus Panel settings
			((CraterFocusPanel) this.focusPanel).setCraterSettings(settings);
			if (viewSettings.containsKey(VIEW_SETTINGS_KEY + "-data")) {
				Map<String, Object> layer = (Map<String, Object>) viewSettings.get(VIEW_SETTINGS_KEY + "-data");

				if (layer.get("schema") instanceof Field[] && layer.get("values") instanceof Object[][]) {
					Field[] schema = (Field[]) layer.get("schema");
					Object[][] values = (Object[][]) layer.get("values");
					FeatureCollection craterCollection = new SingleFeatureCollection();
					// FeatureCollection fc = settings.craterCollection;
					for (Field f : schema) {
						craterCollection.addField(f);
					}
					for (Object[] row : values) {
						Crater c = new Crater();
						for (int i = 0; i < schema.length; i++) {
							Object value = row[i];
							if (value instanceof ShapePath) {
								value = ((ShapePath) value).getPath();
							}
							c.setAttribute(schema[i], value);
						}
						craterCollection.addFeature(c.getFeature(settings));
					}

					settings.craters = new ArrayList<Crater>();
					for (Feature f : craterCollection.getFeatures()) {
						settings.craters.add(new Crater(f));
					}
				}
			}
			
			CraterFocusPanel fp = (CraterFocusPanel) getFocusPanel();
			
			if (settings.craters != null) {
				CraterTableModel tm = ((CraterTableModel) fp.table.getUnsortedTableModel());
				for (Crater crater : settings.craters) {
					tm.addRow(crater);
				}
			}

			drawCraters();
			repaint();
		}
	}
	
	private CraterSettings getSettings(){
		return clayer.settings;
	}
	
	// Used to display tooltips with the same precision as the table
	private DecimalFormat df = new DecimalFormat("0.###");
	
	public void setDisplayDigits(int newDigits) {
		StringBuffer tmp = new StringBuffer(newDigits);
		for (int i=0; i<newDigits; i++) {
			tmp.append("#");
		}
		df = new DecimalFormat("0." + tmp.toString());

	}
	
	public String getToolTipText(final MouseEvent event)
	{
		// Don't do this for the panner
 		if (getChild()==null) return null;
 		
		MultiProjection proj = getProj();
		
		if (proj == null) return null;

		Point2D screenPt = event.getPoint();
		Point2D worldPoint = proj.screen.toWorld(screenPt);
			
		Crater crater = findCraterByWorldPt(worldPoint);

		if (crater==null) return null;
		
		DecimalFormat diamterFormatter = new DecimalFormat("0.##");

    	StringBuffer buf = new StringBuffer();
    	
        buf.append("<html>");
        
        buf.append("Latitude: " + df.format(crater.getLat()) + "<p>");
        buf.append("Longitude: " + df.format(crater.getLon()) + "<p>");
        buf.append("Diameter: ");
        double val = (Double) crater.getDiameter();
		if (val > 500) {
			buf.append(diamterFormatter.format(val / 1000.0) + " km");
		} else {
			buf.append(diamterFormatter.format(val) + " m");
		}		
         
        buf.append("</html>");
        return buf.toString();
	}
	
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
 	public String getLayerKey(){
 		return "Crater Counting";
 	}
 	public String getLayerType(){
 		return "crater_counting";
 	}
 	public void viewChanged() {
 		//When the main view was disabled, and then re-enabled, the craters would not show up until some other action triggered a refresh
 		// this method was created to force a repaint when the main view is re-enabled
		super.viewChanged();
		drawCraters();
		repaint();
	}
 	
 	public void viewCleanup(){
 		ProfilerView pv = ((CraterFocusPanel)getFocusPanel()).getProfilerView();
 		if(pv != null){
 			pv.cleanUp();
 		}
 	}
 	
	public void centerOnSelectedCrater(Crater crater) {
		
		Point2D p = new Point2D.Double(360 - crater.getLon(), crater.getLat());
		Point2D worldPoint = Main.PO.convSpatialToWorld(p);
		Main.testDriver.locMgr.setLocation(worldPoint, true);
	}	
	
	private boolean isClickedOnSelectedCrater(Point2D worldPt) {
		
		CraterSettings settings = getSettings();
		if (settings.craters != null) {
			Crater crater = findCraterByWorldPt(worldPt);
			if (crater != null && selectedCraters.size() > 0) {
				return (selectedCraters.contains(crater));
			}			
		}
		return false;
	}
	
}
