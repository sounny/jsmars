package edu.asu.jmars.layer.util.features;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import com.vividsolutions.jts.geom.Geometry;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.ProjObj.Projection_OC;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.MultiProjection;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.profile.ProfileLView;
import edu.asu.jmars.layer.shape2.DivideShapeDialog;
import edu.asu.jmars.layer.shape2.PixelExportDialog;
import edu.asu.jmars.layer.shape2.ShapeLView;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.shape2.drawingpalette.DrawingPalette;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.DrawAction.PaletteObservable;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.DrawActionEnum;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.PaletteObserver;
import edu.asu.jmars.layer.stamp.StampFactory;
import edu.asu.jmars.layer.util.features.GeomSource.AngleUnits;
import edu.asu.jmars.layer.util.features.GeomSource.EllipseData;
import edu.asu.jmars.layer.util.features.GeomSource.LengthUnits;
import edu.asu.jmars.layer.util.features.GeomSource.Units;
import edu.asu.jmars.parsers.gis.CoordinatesParser.LatitudeSystem;
import edu.asu.jmars.parsers.gis.CoordinatesParser.LongitudeSystem;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.HVector;
import edu.asu.jmars.util.ObservableSet;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.ellipse.geometry.Ellipse;
import edu.asu.jmars.util.ellipse.geometry.FitEllipse;
import edu.asu.jmars.viz3d.ThreeDManager;

/**
 *  Defines mouse behavior in the JMARS Shape Framework. Layers may define what behaviors 
 *  are allowed by sending in the or'ed list of behaviors in "definedBehavior".
 *
 * @author James Winburn with copious help from Noel Gorelick, Saadat Anwar, and Eric Engle 
 *              5/2006 MSFF-ASU
 */

 
public class FeatureMouseHandler extends MouseInputAdapter implements PaletteObserver {
	private final ShapeLayer shapeLayer;
	private final ShapeLView         lview;

	// various and sundry variables used by the class.
	private Point2D             mouseLast       = null;
	private Point2D             mouseDown       = null;
	private Point2D             mouseCurr       = null;
	private Point2D				ellipseMouseDown = null;
	private Point2D				ellipseCurrentMouseLocation = null;
	
	// if these are set, then we will be editing a vertex.
	private Feature             selectedVertexFeature = null;
	/** A specific point of editing control in spatial west coordinates */
	private Point2D             selectedVertex  = null;

	private boolean             drawSelectionRectangleOK  = false;
	private boolean             drawSelectionGhostOK      = false;
	private boolean             drawVertexBoundingLinesOK = false;

	private boolean             addPointsOK         = false;
	private boolean             addLinesOK          = false;
	private boolean             addPolysOK          = false;
	private boolean				addEllipseGhostStart = false;
	private boolean				addEllipseGhost		= false;
	private boolean				setEllipseLength	= false;
	private boolean				adjustEllipseGhost	= false;

	private boolean             moveFeaturesOK      = false;
	private boolean             deleteFeaturesOK    = false;

	private boolean             moveVertexOK        = false;
	private boolean             addVertexOK         = false;
	private boolean             deleteVertexOK      = false;

	private boolean             changeModeOK        = false;
	private boolean             zorderOK            = false;

	private boolean				drawRectangleFeatureGhost = false;
	private boolean				drawRectangleFeatureOK = false;
	// context menu items.
	private JRadioButtonMenuItem addStreamModeRadioButton;
	private JRadioButtonMenuItem addPointsOnlyRadioButton;
	private JRadioButtonMenuItem addLinesOnlyRadioButton;
	private JRadioButtonMenuItem addPolygonsOnlyRadioButton;
	private JRadioButtonMenuItem addEllipseRadioButton;
	private JRadioButtonMenuItem addDrawRectangleRadioButton;
	
	private JMenuItem zmenu;

	private JRadioButtonMenuItem addCircleRadioButton;
	private JRadioButtonMenuItem add5PtEllipseRadioButton;
	private JRadioButtonMenuItem selectModeRadioButton;    
	private JMenuItem            deletePointMenuItem;
	private JMenuItem            addPointMenuItem;
	private JMenuItem            editPointMenuItem;
	private JMenuItem            zOrderMenuItem;
	private JMenuItem            deleteRowMenuItem;
	private JMenuItem			 intersectMenuItem;
	private JMenuItem			 subtractMenuItem;
	private JMenuItem			 mergeMenuItem;
	private JMenuItem			 duplicateMenuItem;
	private JMenuItem		     addBufferMenuItem;
	private JMenuItem			 pixelExportMenuItem;
	private JMenuItem			 convertCirclesToPolygonItem;
	private JMenuItem			 convertCirclesToEllipsesItem;
	private JMenuItem            makeLotsOfSubShapesItem;
	private JMenuItem            splitPolylineMenuItem;
	private JMenuItem			 convertToProfileMenuItem;
	private JMenu			     findStampsMenu;
	private JMenu                shapeFunctionsMenu;
	
	// variables used by the ContextMenu controllers.
	private Rectangle2D rect;
	private Point2D     worldPt;
	
	//export variables needed
	FPath path = null;
	
	// cursors to be set up.
	ImageIcon img1    = new ImageIcon(Main.getResource("resources/pencil.png"));
	Image     pointer = img1.getImage(); 
	ImageIcon img2    = new ImageIcon(Main.getResource("resources/vertex.png"));
	Image     vertex  = img2.getImage();
	Toolkit   tk = Toolkit.getDefaultToolkit();
	private final Cursor     ADD_CURSOR       = tk.createCustomCursor(pointer, new Point(0,30), "Add");    
	private final Cursor     VERTEX_CURSOR    = tk.createCustomCursor(vertex,  new Point(15,15), "Vertex");
	private final Cursor     DEFAULT_CURSOR   = new Cursor(Cursor.DEFAULT_CURSOR);
	private final Cursor     PERIMETER_CURSOR = new Cursor(Cursor.CROSSHAIR_CURSOR);
	private final Cursor     SELECT_CURSOR    = new Cursor(Cursor.HAND_CURSOR);
	private final int        TOLERANCE = 5; 
	private final int        PROXIMITY_BOX_SIDE = 5; // proxmity box side in pixels
	private final int        STROKE_WIDTH = 2; // stroke width in pixels
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getCommonFill();
	private static Image profile_c = ImageFactory.createImage(ImageCatalogItem.PROFILE_PENCIL.withDisplayColor(imgColor));
	Dimension bestSize = tk.getBestCursorSize(profile_c.getWidth(null), profile_c.getHeight(null));	
	private final Cursor SHAPE_CURSOR = tk.createCustomCursor(profile_c, new Point(0, (int) (bestSize.height * 9.0/10.0)), "drawshape");				
	private final EditCoordinatesDialog editCoordsDialog;
	private Map<DrawActionEnum, JRadioButtonMenuItem> drawActions = new HashMap<>();
	private final Style<FPath> geomStyle;
	private static ShapeActionObservable observable = null;
	private ButtonGroup toolSelectButtonGroup = new ButtonGroup();
	private static final String DRAW_TXT = "Draw Shape";
	private static final String fromShapeToProfiletooltip = "<html><body><div>"		
		+ "Export the selected polyline(s) to the Profile layer.<br>"
	    + "This allows you to visualize the profile(s) generated from the selected polyline(s)<br>"
	    + "using the Profile layer and Charts viewer.</div></body></html>";
		

	static {
		observable = new ShapeActionObservable();		
	}
	
	// If running in JMARS, this will set the cursor.
	// It does nothing otherwise. 
	private void setCursor( Cursor c){
		if (lview!=null){
			lview.setCursor( c);
		}
	}
	
	// If running in JMARS, this will cause paintComponent
	// to be called.  It does nothing otherwise.
	private void repaint(){
		if (lview!=null){
			lview.repaint();
		}
	}

	/**
	 * Defines the mode in which the layer is running.
	 * This is public as external classes will need this
	 * information.
	 * 
	 * Note that the module always starts in SELECT_FEATURE_MODE.
	 */
	public static final int SELECT_FEATURE_MODE     = 0;
	public static final int ADD_FEATURE_MODE        = 1;
	public static final int MOVE_FEATURE_MODE       = 2;
	public static final int ADD_CIRCLE_MODE         = 3;
	public static final int ADD_FEATURE_STREAM_MODE = 4;
	public static final int ADD_FIVE_PT_ELLIPSE_MODE =5;
	public static final int ADD_RECTANGLE_MODE      = 6;
	public static final int ADD_ELLIPSE_MODE 		= 7;
	
	private static final int CONVERT_CIRCLE_TO_ELLIPSE_MODE = 1000; //used just for setting GeomSource
	
	int mode = SELECT_FEATURE_MODE;
	
	public int getMode(){
		return mode;
	}
	
	public void setMode( int m){
		drawRectangleFeatureGhost = false;
		drawRectangleFeatureOK = false;
		switch (m) {
		case SELECT_FEATURE_MODE:
			selectModeRadioButton.setSelected(true);
			setCursor(shapeLayer.getFeatureCollection().getFeatureCount() > 0 ? SELECT_CURSOR :  DEFAULT_CURSOR);
			break;
		case ADD_FEATURE_MODE:
			setCursor( SHAPE_CURSOR);
			break;
		case ADD_CIRCLE_MODE:
			setCursor(SHAPE_CURSOR);
			addCircleRadioButton.setSelected(true);
			break;
		case ADD_FEATURE_STREAM_MODE:
			setCursor(SHAPE_CURSOR);
			addStreamModeRadioButton.setSelected(true);
			break;
		case ADD_FIVE_PT_ELLIPSE_MODE:
			setCursor(SHAPE_CURSOR);
			add5PtEllipseRadioButton.setSelected(true);
			break;
		case ADD_ELLIPSE_MODE: 
			setCursor(SHAPE_CURSOR);
			addEllipseRadioButton.setSelected(true);
			break;
		case ADD_RECTANGLE_MODE:
			setCursor(SHAPE_CURSOR);
			addDrawRectangleRadioButton.setSelected(true);
			break;
		default:
			setCursor( DEFAULT_CURSOR);
			break;
		}
		mode = m;
	}
	
	/**
	 * Allowable behavior in this class is defined
	 * by or'ing the behavior flags in the "definedBehavior"
	 * argument in the constructor.
	 */
	public static final int ALLOW_ADDING_POINTS     = 1;
	public static final int ALLOW_ADDING_LINES      = 2;
	public static final int ALLOW_ADDING_POLYS      = 4;
	
	public static final int ALLOW_MOVING_FEATURES   = 8;
	public static final int ALLOW_DELETE_FEATURES   = 16;
	
	public static final int ALLOW_MOVING_VERTEX     = 32;
	public static final int ALLOW_DELETING_VERTEX   = 64;
	public static final int ALLOW_ADDING_VERTEX     = 128;
	
	public static final int ALLOW_CHANGE_MODE       = 256;
	public static final int ALLOW_ZORDER            = 512;
	public static final int ALLOW_ELLIPSE			= 1024;
	
	
	// Gets the multi-projection used. 
	private MultiProjection getProj() {
		if (lview != null)
			// Normal case, used in JMars.
			return lview.getProj();
		
		// Abnormal case, used for JUnit tests.
		return MultiProjection.getIdentity();
	}
	
	/**
	 * constructor
	 * @param fc - the FeatureCollection to be added to.     Cannot be null.
	 * @param lview - the LView associated with the module.  Null if not running in JMARS.
	 * @param selectColor - the color of the selection line.  This is abstract to allow users 
	 *            to change the color dynamically.
	 * @param definedBehavior - an or'ed combination of behaviors that this module should
	 *            be sensitive to.
	 * @param history - History or undo log.
	 */
	public FeatureMouseHandler(ShapeLayer shapeLayer, ShapeLView lview, int definedBehavior) {
		this.shapeLayer = shapeLayer;
		this.lview = lview;
		this.geomStyle = shapeLayer.getStylesLive().geometry;
		
		this.editCoordsDialog = new EditCoordinatesDialog();
	
		addPointsOK = (definedBehavior & ALLOW_ADDING_POINTS)==ALLOW_ADDING_POINTS;
		addLinesOK = (definedBehavior & ALLOW_ADDING_LINES)==ALLOW_ADDING_LINES;
		addPolysOK = (definedBehavior & ALLOW_ADDING_POLYS)==ALLOW_ADDING_POLYS;
		moveFeaturesOK = (definedBehavior & ALLOW_MOVING_FEATURES)==ALLOW_MOVING_FEATURES;
		deleteFeaturesOK = (definedBehavior & ALLOW_DELETE_FEATURES)==ALLOW_DELETE_FEATURES;
		moveVertexOK = (definedBehavior & ALLOW_MOVING_VERTEX)==ALLOW_MOVING_VERTEX;
		addVertexOK = (definedBehavior & ALLOW_ADDING_VERTEX)==ALLOW_ADDING_VERTEX;
		deleteVertexOK = (definedBehavior & ALLOW_DELETING_VERTEX)==ALLOW_DELETING_VERTEX;
		changeModeOK = (definedBehavior & ALLOW_CHANGE_MODE)==ALLOW_CHANGE_MODE;
		zorderOK = (definedBehavior & ALLOW_ZORDER)==ALLOW_ZORDER;
		
		setupContextMenu();
		setupDrawingPaletteActions();
		setMode(mode);
	}


	// a list of the points created when drawing. The point could represent
	// just a point or the vertex of a polyline or polygon
	private List<Point2D> points = new ArrayList<Point2D>();
	private List<Point2D> pointsRect = new ArrayList<Point2D>();
	
	private int mouseContext = -1;
	
	/**
	 * adding a feature is done here.
	 */
	public void mousePressed(MouseEvent e){
		if (ToolManager.getToolMode() == ToolManager.SHAPES || 
				ToolManager.getToolMode() == ToolManager.SEL_HAND) {
			mouseContext = MouseEvent.MOUSE_PRESSED;
			mouseDown = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
		
			if ( getMode()==ADD_FEATURE_MODE){
				// add mode processing happens in the mouseReleased portion.
			} else if (getMode()==SELECT_FEATURE_MODE && (e.getModifiers() & InputEvent.CTRL_MASK) == 0 && !adjustEllipseGhost){
				// Find out if we need to go into MOVE_FEATURE_MODE
				Rectangle2D rect = getProj().getClickBox(getProj().spatial.toWorld(mouseDown), PROXIMITY_BOX_SIDE);
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature f: shapeLayer.getSelections()) {
					Point2D vertex = null;
					if (moveVertexOK || deleteVertexOK) {
						// if the cursor is over a vertex, remember it so later mouse events
						// can move or delete it
						vertex = getIntersectingVertex(f, rect);
					}
					if (vertex == null) {
						// if the cursor is over a circle edge, remember the edge position
						// so later mouse events can manipulate it
						vertex = getCircleEdgePoint(f, rect);
					}
					if (vertex != null) {
						selectedVertex = vertex;
						selectedVertexFeature = f;
						break;
					} else {
						// cursor is NOT over a vertex, the entire selection will be moved.
						selectedVertex = null;
						selectedVertexFeature = null;
						if (moveFeaturesOK && idx.getWorldPath(f).intersects(rect)) {
							setMode( MOVE_FEATURE_MODE);
							break;
						}
					}	
				}
			} else if(getMode() == ADD_FIVE_PT_ELLIPSE_MODE){
	        	//points array is cleared as soon as 5 point mode radio 
	        	// button is selected
	        	points.add(mouseDown);
	        	repaint();
	        	if(points.size() == 5){
	        		//create and add the ellipse
	        		addFeature(getCurrentEllipse());
	        		//clear the points list
	        		points.clear();
	        	}
			} else if (getMode() == ADD_ELLIPSE_MODE) {
//				System.out.println("Pressed - addEllipseGhost: "+addEllipseGhost);
				if (!addEllipseGhost) {
					addEllipseGhost = true;
					ellipseMouseDown = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
				}
			}
			mouseLast = mouseCurr;
		}
	}
	
	// The actions for moving and selecting is done in mouseReleased. 
	public void mouseReleased(MouseEvent e){
		if(ToolManager.getToolMode() == ToolManager.SHAPES || 
				ToolManager.getToolMode() == ToolManager.SEL_HAND) {
			mouseContext = MouseEvent.MOUSE_RELEASED;
			drawSelectionRectangleOK  = false;
			drawSelectionGhostOK      = false;
			drawVertexBoundingLinesOK = false;
			
			mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
			double ppd = getProj().getPPD();
			
			switch (getMode()) {
			case ADD_FEATURE_MODE:
				// If the mouse release occurs above or below the edge of the projection, ignore it
				Point2D mouseWorld = getProj().screen.toWorld(((WrappedMouseEvent)e).getRealPoint());
				if (mouseWorld.getY()>90 || mouseWorld.getY()<-90) return;
				
				int featureType = FPath.TYPE_NONE;
				
				// If we can only add Points, then add them with a single click
				if (addPointsOK && !addLinesOK && !addPolysOK && !addEllipseGhost) {
					points.add(mouseCurr);
					featureType=FPath.TYPE_POINT;
				}
				// If there is only one point defined and the user clicked on it, insert a point.
				else if (addPointsOK && points.size()==1 && intersects(mouseCurr, points.get(0), TOLERANCE/ppd))
					featureType = FPath.TYPE_POINT; 
				
				// if the user clicked on the last point, insert a polyline.
				else if (addLinesOK && points.size() > 0 && intersects(mouseCurr, points.get(points.size()-1), TOLERANCE/ppd))
					featureType = FPath.TYPE_POLYLINE;
				
				// if the user clicked on the first point, complete the polygon.
				else if (addPolysOK && points.size() > 0 && intersects(mouseCurr, points.get(0), TOLERANCE/ppd))
					featureType = FPath.TYPE_POLYGON;
				
				// if we can add polys or lines, add the point to the array of points.
				else if (addLinesOK || addPolysOK || (addPointsOK && points.size()==0))
					points.add(mouseCurr);
					
				
				// barf at the user.
				else
					Toolkit.getDefaultToolkit().beep();
				
				// Make a new history frame.
				if (featureType != FPath.TYPE_NONE){
					if (shapeLayer.getHistory() != null)
						shapeLayer.getHistory().mark();
					addFeature(featureType == FPath.TYPE_POLYGON, points);
				}
				break;
				
	
			case ADD_ELLIPSE_MODE: 
				if (addEllipseGhostStart) {//start of drawing
					addEllipseGhostStart = false;
					
					adjustEllipseGhost = false;
					addEllipseGhost = true;	
				} else if (addEllipseGhost && ellipseGhostShape != null) {//addEllipseGhostShape gets set to true on the click. The shape is nulled out on escape key
					addEllipseGhost = false;
					addEllipseGhostStart = true;
					ellipseCurrentMouseLocation = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
//					featureType = FPath.TYPE_ELLIPSE;
					addEllipseFeature();
				}
				repaint();
				break;
			case MOVE_FEATURE_MODE:
				Map<Feature,Object> features = new HashMap<Feature,Object>();
				for (Feature f: shapeLayer.getSelections()) {				
					Point2D[] vertices = f.getPath().getSpatialWest().getVertices();
					vertices = offsetVertices(vertices, mouseDown, mouseCurr);
					FPath newPath = new FPath(vertices, FPath.SPATIAL_WEST, f.getPath().getClosed());
					features.put(f, newPath);
					StyleSource<FPath> styleSource = shapeLayer.getStyles().geometry.getSource();
					if(styleSource instanceof GeomSource){
						Point2D.Double newCenterPt = (Point2D.Double) newPath.getCenter();//stay in west leading for transform using CoordinateParser
						transformPointToUserSystem(newCenterPt);
						GeomSource source = (GeomSource)geomStyle.getSource();
						if (FeatureUtil.isEllipseSource(source, f)) {
							source.setFieldValue(f, source.getLonField(), newCenterPt.getX(), true);
							source.setFieldValue(f, source.getLatField(), newCenterPt.getY(), false);
						}
					}

				}
				if (features.size() > 0) {
					// Make a new history frame.
					if (shapeLayer.getHistory() != null)
						shapeLayer.getHistory().mark();
					shapeLayer.getFeatureCollection().setAttributes( Field.FIELD_PATH, features);
				}
				mouseLast = null;
				setMode( SELECT_FEATURE_MODE);
				break;
				
			case SELECT_FEATURE_MODE :
				if (adjustEllipseGhost) {
					addEllipseFeature();
				} 
				else {
					// If we are editing a vertex, process appropriately.
					if (moveVertexOK && selectedVertex != null && selectedVertexFeature != null){
						// Make a new history frame.
						if (shapeLayer.getHistory() != null)
							shapeLayer.getHistory().mark();
						if (FeatureUtil.isCircle(geomStyle, selectedVertexFeature)) {
							// set radius field so the circle passes through the
							// current mouse position
							updateCircleRadius(selectedVertexFeature);
						} else {
							// move vertex of polyline or polygon -- point vertex moves
							// are converted to the MOVE_FEATURE_MODE and handled in
							// that case above.
							GeomSource currentSource = (GeomSource)geomStyle.getSource();
		        			if (!FeatureUtil.isEllipseSource(currentSource, selectedVertexFeature)) {
		        				moveVertex(selectedVertexFeature, selectedVertex, mouseCurr);
		        			}
							
						}
						selectedVertex = null;
						selectedVertexFeature = null;
						mouseLast = null;
						repaint();
						return;
					}
	    		
				
					// We have no selected vertex. continue processing as though this is a simple select.
					// features that intersect the selectRectangle are marked as selected and drawn as handles. 
					// If the mouse was pressed on a different point than it was released,
					// do a selectionRectangle selection, selecting ALL the features within
					// the extent of the rectangle.
					
					// If the two points are the same, mouseClick will already have processed 
					// the selection.
					// We can't do it here, because pressing the mouse button while over
					// a selected feature sends the app into move feature mode.
					if (!mouseLast.equals(mouseDown)){
							//selectRectangle.setFrameFromDiagonal( getProj().spatial.toScreen(mouseLast),
							//		getProj().spatial.toScreen(mouseDown));
							//Rectangle2D rect = getProj().screen2world(selectRectangle);
							Rectangle2D rect = new Rectangle2D.Double();
							rect.setFrameFromDiagonal(getProj().spatial.toWorld(mouseLast),
									getProj().spatial.toWorld(mouseDown));
							rect = Util.normalize360(rect).getBounds2D();
							if ((e.getModifiers() & MouseEvent.CTRL_MASK) == MouseEvent.CTRL_MASK) {
								toggleFeatures(rect);
							} else {
								selectFeatures( rect);
							}
							mouseLast = null;
	            		
					} else {
						//no selection rectangle drawn. 
						ellipseSelected = false;
						selectedEllipseFeature = null;
						
						Point2D pt = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
						Rectangle2D rect1 = getProj().getClickBox(getProj().spatial.toWorld(pt), PROXIMITY_BOX_SIDE);
		        		selectTopmostFeature(rect1);
						//if there is only one feature selected, and it is an ellipse, we'll adjust the ellipse
		        		if (shapeLayer.getSelections().size() == 1) {	
			        		for (Feature feature : shapeLayer.getSelections()) {
			        			if (geomStyle.getSource() instanceof GeomSource) {
			        				GeomSource currentSource = (GeomSource)geomStyle.getSource();
				        			if (FeatureUtil.isEllipseSource(currentSource, feature)) {
				        				selectedEllipseFeature = feature;
				        				if (is5PtEllipse()) {
				        					ellipseSelected = false;
				        					selectedEllipseFeature.setHidden(false);
				        					selectedEllipseFeature = null;
				        				} else {
					        				ellipseSelected = true;
					        				adjustEllipseGhost = true;
					        				selectedEllipseFeature.setHidden(true);
					        				
					        				//call draw to clear out the selection and normal buffers
					        				lview.draw(true);
					        				lview.draw(false);
					        				
					        				repaint();
				        				}
				        				return;
				        			}
			        			}
			        		}
						}
					}
        		}
				
			}
		}
	}
	/**
	 * If there was a mouseClick in selection mode, as opposed to a mousePressed/mouseReleased pair,
	 * do a selection on the topmost feature.
	 */
	public void mouseClicked(MouseEvent e) {
		if(ToolManager.getToolMode() == ToolManager.SHAPES || ToolManager.getToolMode() == ToolManager.SEL_HAND) {
			mouseContext = MouseEvent.MOUSE_CLICKED;
			int featureType = FPath.TYPE_NONE;
			switch(getMode()) {
				case SELECT_FEATURE_MODE:
					mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
					Rectangle2D rect = getProj().getClickBox(getProj().spatial.toWorld(mouseCurr), PROXIMITY_BOX_SIDE);    
					
					if ( (e.getModifiers() & InputEvent.CTRL_MASK) !=0) {
						toggleTopmostFeature( rect);
						
					} else {
						selectTopmostFeature( rect);
					}
						
					mouseLast = null;
					break;
					
				//NEW
				case ADD_CIRCLE_MODE:
					if (isAddCirclesOkay()) {
						mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent) e).getRealPoint());
						switch (points.size()) {
						case 0:
						case 1:
							points.add(mouseCurr);
							break;
						case 2:
							points.set(1, mouseCurr);
							break;
						}
						if (!points.isEmpty() && points.size() >= 2) {
							Feature temp = getCurrentCircle();
							if (temp != null) {
								addFeature(temp);
							}
						}
					}
					break;
					
				case ADD_RECTANGLE_MODE: //NEW
					mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent) e).getRealPoint());
					if (pointsRect.isEmpty()) { // start of draw
						pointsRect.add(mouseCurr);
						drawRectangleFeatureGhost = true;
						drawRectangleFeatureOK = false;
					} else {
						if (!mouseCurr.equals(pointsRect.get(0))) { /// mouse was moved and click occurred to mark end of draw
							if (drawRectangleFeatureGhost) {
								drawRectangleFeatureGhost = false;
								drawRectangleFeatureOK = true;
								repaint();
								// end of draw
								drawSelectionRectangleOK = false;
								drawSelectionGhostOK = false;
								drawVertexBoundingLinesOK = false;
								pointsRect.clear();
							}
						}
					}
					break;
				case MOVE_FEATURE_MODE:
					// If a mouse-press put us into move-feature mode, snap out of it 
					// if it turns out that we weren't moving at all.
					setMode(SELECT_FEATURE_MODE);
					break;
                case ADD_FEATURE_STREAM_MODE:
                    if (points.isEmpty()) {
                        this.points.add(mouseDown);
					} else {
                        if (SwingUtilities.isLeftMouseButton(e) && e.isControlDown()) {
                            points.add(points.get(0));
                            addFeature(true, points);
						} else if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown()) {
                            addFeature(false, points);
                        }
                        points.clear();
                    }
                    break;
                	
				case ADD_FEATURE_MODE: //NEW - close polygon if dbl click with last point
					if (!(points.isEmpty()) && (e.getClickCount() == 2) && (addPolysOK)) {
						featureType = FPath.TYPE_POLYGON;
						if (shapeLayer.getHistory() != null)
							shapeLayer.getHistory().mark();
						addFeature(featureType == FPath.TYPE_POLYGON, points);
					}
					break;

			}
		}
	}
	// all we need to do when moving the mouse is select the appropriate cursor.
	public void mouseMoved(MouseEvent e){
		if(ToolManager.getToolMode() == ToolManager.SHAPES || 
				ToolManager.getToolMode() == ToolManager.SEL_HAND) {
			mouseContext = MouseEvent.MOUSE_MOVED;
			mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());

			switch( getMode()){
			case ADD_FEATURE_MODE:
				// If the mouse movement occurs above or below the edge of the projection, ignore it
				Point2D mouseWorld = getProj().screen.toWorld(((WrappedMouseEvent)e).getRealPoint());
				if (mouseWorld.getY()>90 || mouseWorld.getY()<-90) return;
	
				// If we're just moving the cursor around after having defined a point, 
				// update the panel showing the length of the line
				if (points.size() > 0 && mouseDown != null){
					updateLength();
				}
				setCursor( SHAPE_CURSOR);
				
				// redraw the feature built so far
				repaint();
				break;

			case ADD_FEATURE_STREAM_MODE:
				if (!this.points.isEmpty()) {
					this.points.add(mouseCurr);
					double ppd = getProj().getPPD();

					if (addPolysOK && points.size() > 10 && intersects(mouseCurr, points.get(0), TOLERANCE/ppd)) {
						this.addFeature(true, this.points);
						this.points.clear();
					}
					else {
						updateLength();
					}

					repaint();
				}
				break;
				
				//NEW - this is preview drawing circle on mouse-move
			case ADD_CIRCLE_MODE:
				if (!isAddCirclesOkay()) {
					setMode(ADD_FEATURE_MODE);
					mouseMoved(e);
				} else {
						switch (points.size()) {
						case 1: points.add(mouseCurr); break;
						case 2: points.set(1, mouseCurr); break;
				}
						repaint();
				}
				break;
				
			case ADD_RECTANGLE_MODE:  //NEW
				repaint();
				break;				
			
			case ADD_ELLIPSE_MODE:
				ellipseCurrentMouseLocation = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
				repaint();
				break;
				
			case SELECT_FEATURE_MODE:
				// Change cursor according to the current proximity to the selected polygons.
				Rectangle2D rect = getProj().getClickBox(getProj().spatial.toWorld(mouseCurr), PROXIMITY_BOX_SIDE);
	
				// If moving vertices is permitted and any features are selected 
				// in the table, check if the cursor is hovering over a vertex or an outline.
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature f: shapeLayer.getSelections()) {
					FPath path = idx.getWorldPath(f);
					if (path.getType() != FPath.TYPE_POINT) {
						// if we don't hit the exterior but we do hit the interior,
						// use the badly named 'perimeter' cursor
						if (addVertexOK && path.intersects(rect) && getIntersectingVertex(f, rect) == null) {
							setCursor( PERIMETER_CURSOR);
							return;
						}
						
						//if this point is from an ellipse, don't change to vertex_cursor
						if (path.intersects(rect)) {
							StyleSource<FPath> source = shapeLayer.getStyles().geometry.getSource();
							if(source instanceof GeomSource){
								if(FeatureUtil.isEllipseSource((GeomSource)source, f)){
									setCursor(SELECT_CURSOR);
									return;
								}
							}
						}
						
						if (null != getCircleEdgePoint(f, rect) ||
								null != getIntersectingVertex(f, rect)) {
							setCursor(VERTEX_CURSOR);
							return;
						}
						
						if ((deleteVertexOK || moveVertexOK) && getIntersectingVertex(f, rect) != null) {
							setCursor( VERTEX_CURSOR);
							return;
						}
					}
				}
	
				// no vertex operation detected.  set cursor to a simple selection.
				setCursor(shapeLayer.getFeatureCollection().getFeatureCount() > 0 ? SELECT_CURSOR :  DEFAULT_CURSOR);
				break;
			}
			mouseLast = mouseCurr;
		}
	}
	
	/**
	 * dragging the mouse involves the positioning of the "Selection Ghost" which can
	 * be a line or a box.
	 */
	public void mouseDragged(MouseEvent e){
		if(ToolManager.getToolMode() == ToolManager.SHAPES || 
				ToolManager.getToolMode() == ToolManager.SEL_HAND) {
			mouseContext = MouseEvent.MOUSE_DRAGGED;
			mouseCurr = getProj().screen.toSpatial(((WrappedMouseEvent)e).getRealPoint());
			switch (getMode()){
			case ADD_FEATURE_MODE:
				// If the mouse drag occurs above or below the edge of the projection, ignore it
				Point2D mouseWorld = getProj().screen.toWorld(((WrappedMouseEvent)e).getRealPoint());
				if (mouseWorld.getY()>90 || mouseWorld.getY()<-90) return;
	
				// drag the point just added if there is one
				if (mouseDown != null) {
					updateLength();
					repaint();
				}
				break;
				
			case MOVE_FEATURE_MODE:
				ellipseMouseDown = null;//set these to null as to not draw a ghost ellipse
				ellipseCurrentMouseLocation = null;
				drawSelectionGhostOK = true; 
				repaint();
				break;
			
			case SELECT_FEATURE_MODE:
				if (adjustEllipseGhost) {
					break;
				}
				ellipseMouseDown = null;//set these to null as to not draw a ghost ellipse
				ellipseCurrentMouseLocation = null;
				
				// If the mouse drag occurs above or below the edge of the projection, ignore it
				mouseWorld = getProj().screen.toWorld(((WrappedMouseEvent)e).getRealPoint());
				if (mouseWorld.getY()>90 || mouseWorld.getY()<-90) return;
	
				// Feature/vertex is being dragged.
				if ( selectedVertex != null && selectedVertexFeature != null) {
					//if the feature is an ellipse, don't allow dragging of vertices
					StyleSource<FPath> source = shapeLayer.getStyles().geometry.getSource();
					if(source instanceof GeomSource){
						if(FeatureUtil.isEllipseSource((GeomSource)source, selectedVertexFeature)){
							return;
						};
					}
					
					// show a ghost of the new outline while user is dragging
					drawVertexBoundingLinesOK = true;
					repaint();
					mouseLast = mouseCurr;
				} else {
					// polygons are being dragged
					if (mouseDown == null){
						return;
					}
					drawSelectionRectangleOK = true;
					repaint();
					mouseLast = mouseCurr;
				}
				break;
			}
		}
	}
    

	
	/** @return true if the geometry has been configured to produce circles. */
	private boolean isAddCirclesOkay() {
		StyleSource<?> source = shapeLayer.getStylesLive().geometry.getSource();
		return source instanceof GeomSource && ((GeomSource)source).getCircleFields().size() == 2;
	}
	
	/** @return true if the geometry has been configured to produce ellipses. */
	private boolean isAdd5PtEllipsesOkay() {
		StyleSource<?> source = shapeLayer.getStylesLive().geometry.getSource();
		return source instanceof GeomSource && ((GeomSource)source).get5ptEllipseFields().size() == 6;//if you add mean axis value, address is5PtEllipse()
	}
	
	/**
	 * @return a spatial west vertex from the given circle feature, if it *is* a
	 *         circle feature and the given rectangle overlaps the line that
	 *         starts with the returned vertex.
	 */
	private Point2D getCircleEdgePoint(Feature f, Rectangle2D rect) {
		if (!FeatureUtil.isCircle(geomStyle, f)) {
			return null;
		}
		// get world coordinates to compare with 'rect'
		FPath path = shapeLayer.getIndex().getWorldPath(f);
		// compute the world coordinate distance we will allow rectangle
		// center to be from an edge
		double tolerance = Math.sqrt(
			Math.pow(getProj().getPixelWidth()*15, 2) +
			Math.pow(getProj().getPixelHeight()*15, 2));
		// circle feature, so use vertex cursor if the mouse is over the edge
		Point2D[] vertices = path.getVertices();
		int[] hitIndices = getBoundingIndices(vertices, rect);
		if (hitIndices != null) {
			// Rectangle hits this line, but if circle size is below
			// tolerance, only return hits *outside* the shape so
			// when the user is zoomed out and the circle is small,
			// user can at least resize it.
			Shape shape = path.getShape();
			Rectangle2D gpSize = shape.getBounds2D();
			if (Math.min(gpSize.getWidth(), gpSize.getHeight()) > tolerance ||
					!contains360(path.getShape(), rect.getCenterX(), rect.getCenterY())) {
				return Main.PO.convWorldToSpatial(vertices[hitIndices[0]]);
			}
		}
		return null;
	}
	
	private void updateCircleRadius(Feature f) {
		double km = Util.angularAndLinearDistanceWorld(
			f.getPath().getWorld().getCenter(),
			getProj().spatial.toWorld(mouseCurr))[1];
		((GeomSource)geomStyle.getSource()).setRadius(f, km);
	}
	
	/**
	 * Uses the first two points in {@link #points} as opposite edges of a
	 * circle and returns a new Feature describing that circle. Returns null if
	 * {@link #isAddCirclesOkay()} is false or there are not enough points.
	 */
	private Feature getCurrentCircle() {
		Feature temp = null;
		if (isAddCirclesOkay() && points.size() >= 2) {
			temp = new Feature();
			Point2D a = getProj().spatial.toWorld(points.get(0));
			Point2D b = getProj().spatial.toWorld(points.get(1));
			if (Math.abs(b.getX() - a.getX()) > 180) {
				b.setLocation(b.getX() + 360*Math.signum(a.getX() - b.getX()), b.getY());
			}
			Point2D mid = new Point2D.Double(
					a.getX()/2 + b.getX()/2,
					a.getY()/2 + b.getY()/2);
			temp.setPath(new FPath(new Point2D[]{mid}, FPath.WORLD, false).getSpatialWest());
			if (isAddCirclesOkay() && points.size() > 1) {
				double km = Util.angularAndLinearDistanceWorld(a, b)[1] / 2;
				((GeomSource)geomStyle.getSource()).setRadius(temp, km);
			}
		}
		return temp;
	}
	
	/**
	 * Uses the 5 points in {@link #points} as opposite edges of an ellipse
	 * and returns a new Feature describing that ellipse. Returns null if
	 * {@link #isAddCirclesOkay()} is false or there are not enough points.
	 */
	private Feature getCurrentEllipse() {
		Feature ellipse = null;
		
		if (isAdd5PtEllipsesOkay() && points.size() == 5) {
			ellipse = new Feature();
			
			//get the world ellipse from the points
			ArrayList info = getCurrentWorldEllipseAndProj();
			Ellipse worldE = (Ellipse)info.get(0);		
			ProjObj po = (ProjObj)info.get(1);
			
			//get the spatial path from the world ellipse
			FPath spPath = GeomSource.getSpatialPathFromWorlEllipse(worldE, po);
			
			//convert that ellipse to spatial values
			Ellipse spE = GeomSource.convertWorldEllipseToSpatialEllipse(worldE, po);
			
			//set everything on the feature object
			ellipse.setPath(spPath);
			
			GeomSource source = (GeomSource)geomStyle.getSource();
			source.setFieldValue(ellipse, source.getAAxisField(), spE.getALength(), true);
			source.setFieldValue(ellipse, source.getBAxisField(), spE.getBLength(), true);
			source.setFieldValue(ellipse, source.getAngleField(), spE.getRotationAngle(), true);
			source.setFieldValue(ellipse, source.getLatField(), spE.getCenterLat(), true);
			source.setFieldValue(ellipse, source.getLonField(), spE.getCenterLon(), false);
		}
		
		return ellipse;
	}
	
	
	
	/**
	 * This method takes the current 5 spatial points from the points list
	 * and uses them to calculate a world ellipse with a projection centered
	 * on the first point in the list.  Since the center of the ellipse 
	 * isn't known till after it's calculation, it calculates another world
	 * ellipse with the center at the new center location. It then compares
	 * centers and when they stop significantly changing, returns the ellipse.
	 * It also returns the ProjObj centered at the calculated spatial center
	 * point.
	 * @return An arraylist with a World Ellipse and ProjObj centered on the 
	 * spatial center of that world ellipse.
	 */
	private ArrayList getCurrentWorldEllipseAndProj(){
		ArrayList result = new ArrayList();
		Ellipse e = null;
		ProjObj po = null;
		
		Point2D prevCenter = new Point2D.Double(450,10);
		//use the first point as the starting center
		Point2D curCenter = points.get(0);
		
		//keep looping until the points are very close together
		while(Point2D.distance(prevCenter.getX(), prevCenter.getY(), curCenter.getX(), curCenter.getY()) > 0.001){
		
			//convert all the points to world points
			List<Point2D> worldPts = new ArrayList<Point2D>();
			//keep track of the world points, to check for greater than 180 separation
			Point2D prevWorld = null;
			//use a new projection to recenter on, based on one of the 5 points
			//find the centroid to use as the center of the projection
			po = new Projection_OC(curCenter.getX(), curCenter.getY());
			for(Point2D spPt : points){
				Point2D wdPt = po.convSpatialToWorld(spPt);
				//check to see if this point is too far away (wrapped in world coords)
				if(prevWorld != null && Math.abs(wdPt.getX() - prevWorld.getX())>180){
					if(wdPt.getX()<prevWorld.getX()){
						wdPt = new Point2D.Double(wdPt.getX()+360, wdPt.getY());
					}else{
						wdPt = new Point2D.Double(wdPt.getX()-360, wdPt.getY());
					}
				}
				worldPts.add(wdPt);
				prevWorld = wdPt;
			}
			//then call 5-point ellipse calculation
		
			//create ellipse from world points
			e = FitEllipse.getEllipseFromPoints(worldPts);
			
			//reset the center points (in spatial)
			prevCenter = curCenter;
			curCenter = po.convWorldToSpatial(e.getCenterPt());
		}
		
		result.add(e);
		result.add(po);
		
		return result;
	}
	
	
	private void setCircleStatusFrom(Feature f) {
		Point2D center = f.getPath().getSpatialWest().getCenter();
		String radius = "";
		if (geomStyle.getSource() instanceof GeomSource) {
			Point2D v1 = geomStyle.getValue(f).getSpatialWest().getVertices()[0];
			double km = Util.angularAndLinearDistanceWorld(
				getProj().spatial.toWorld(center),
				getProj().spatial.toWorld(v1))[1];
			GeomSource source = (GeomSource) geomStyle.getSource();
			double scale = source.getUnits().getScale();
			radius = "    " + source.getUnits().toString() + ": " +
				Main.getFormatterKm(1/scale).format(km / scale);
		}
		Main.setStatus("Center: " + Main.statusFormat(center) + radius);
	}


	
	private void updateLength() {
		Main.setStatusFromWorld(
			getProj().spatial.toWorld(mouseDown),
			getProj().spatial.toWorld(mouseCurr));
	}
    

	


	/**
	 * Offsets <code>vertices</code> according to the motion from the
	 * <code>from</code> point to the <code>to</code> point.<p>
	 * Vertices are offset horizontally around the <code>up</code> vector
	 * according to the signed angle between the from-up and to-up planes.<p>
	 * Vertices are offset vertically within the vertex-up plane according to
	 * the signed difference in the angle between the <code>from</code> 
	 * and <code>to</code> vectors from the <code>up</code> vector.<p>
	 * <em>Note:</em>The <code>from<code> and <code>to<code> are arbitrary
	 * points not linked with the <code>vertices</code> in any way.
	 * <em>Caveat:</em>Pole crossing polygons will get deformed.<p>
	 * <bold>This code is untested on polygons greater than 180 degrees.</bold>
	 * 
	 * @param vertices Vertices to offset (in spatial west coordinates). 
	 * @param from Start point of movement not linked to vertices (in spatial west).
	 * @param to End point of movement not linked to vertices (in spatial west).
	 * @return Offseted vertices (in spatial west).
	 */
	private Point2D[] offsetVertices(Point2D[] vertices, Point2D from, Point2D to){
		/*
		 * From & To vectors from the two (spatial) points determining the direction
		 * of motion.
		 * Up vector from the oblique-cylindrical projection.
		 * Left vector is the normal to the (From, Up) plane.
		 */
		HVector vFrom = getProj().spatial.toHVector(from).unit();
		HVector vTo = getProj().spatial.toHVector(to).unit();
		HVector up = ((Projection_OC)Main.PO).getUp().unit();
		HVector left = vFrom.cross(up).unit();
		
		/*
		 * From and To direction vectors in within the (from, up) and
		 * (to, up) frames respectively. Each of these vectors is perpendicular
		 * to the up vector. Both lie in the plane normal to the up vector. 
		 */
		HVector hFromDir = up.cross(vFrom.cross(up));
		HVector hToDir = up.cross(vTo.cross(up));
		
		/*
		 * Compute angular separation between from & up, and to & up vectors
		 * with proper sign w.r.t. the hemisphere they belong to. Their 
		 * difference will give us the vertical angle of rotation (which is
		 * around the left vector).
		 */
		double va1 = ((hFromDir.dot(vFrom)<0)?-1:1) * vFrom.separation(up);
		double va2 = ((hToDir.dot(vTo)<0)?-1:1) * vTo.separation(up);
		
		/*
		 * Vertical & horizontal separation angles between from & to vectors.
		 * These are signed rotation angles around the left vector and up vector
		 * respectively.
		 */
		double vAngle = -(va2-va1);
		double hAngle = ((left.dot(hToDir)>=0)?-1:1) * hFromDir.separation(hToDir);
		
		Point2D[] out = new Point2D[vertices.length]; // Output points.
		
		// Anchor point which determines which hemisphere other points falls in.
		HVector v0Dir = null;
		if (vertices.length > 0){
			/*
			 * Our anchor is in the (vertex[0], up) plane. It is exactly 90 degrees
			 * from the up vector. Assume it is coming out of the screen, then the
			 * screen partitions the two hemispheres. 
			 */
			HVector v = getProj().spatial.toHVector(vertices[0]).unit();
			v0Dir = up.cross(v.cross(up)).unit();
		}
		
		for(int i=0; i<vertices.length; i++){
			/*
			 * Rotate every vertex in the horizontal and vertical directions
			 * according to the angles determined above.
			 */
			HVector v = getProj().spatial.toHVector(vertices[i]).unit();
			out[i] = new Point2D.Double();
			
			/*
			 * If a point is in the hemisphere towards us (+ve dot product) then
			 * the vertical rotation is positive, otherwise, it is negative. This
			 * ensures that any polygon that goes over the up vector comes back
			 * over the up vector when pulled back.
			 */
			double vAngleSign = (v0Dir.dot(v) > 0)? 1: -1;
			v.rotate(v.cross(up).unit(), vAngleSign * vAngle).rotate(up, hAngle).toLonLat(out[i]);
		}
		
		return out;
	}
	
	/**
	 * clears all the fields of the mouse so that lines will not be set up half way.
	 */
	private void  initializeSelectionLine(){
		points.clear();
		mouseLast=mouseDown=mouseCurr=null;
	}
	
	/**
	 *  Returns whether the points intersect each other within the specified tolarance.
	 *  All values are in spatial coordinates.
	 */
	private boolean intersects( Point2D p1, Point2D p2, double tolarance){
		HVector v1 = getProj().spatial.toHVector(p1);
		HVector v2 = getProj().spatial.toHVector(p2);
		
		if (Math.toDegrees(v1.separation(v2)) <= tolarance)
			return true;
		
		return false;
	}
	
	/**
	 * deletes the most-recently defined point in the selection line and repaints whatever 
	 * is left of the line.  If the selection line consists of a single point, the entire 
	 * selection line is deleted.
	 */
	public void deleteLastVertex(){
		if (points.size()>0){
			points.remove( points.size()-1);
			if (points.size()==0){
				mouseDown = mouseLast=null;
			} else {
				mouseDown = points.get( points.size()-1);
			}
			repaint();
		}
	}
	
	/**
	 * Removes the @param worldPoint from the feature.
	 * Assumes the point is in world coordinates.
	 * If the feature is a line it must have at least 2 points  after 
	 * the delete. If the feature is a polygon it must have at least 3 points
	 * after the delete.
	 * 
	 * @param spatialPoint
	 */
	private void deleteVertex(Feature f, Point2D spatialPoint)
	{
		// Get spatial vertices
		FPath path = f.getPath().getSpatialWest();
		Point2D[] vertices = path.getVertices();
		
		// Remove the deleted point
		Set<Point2D> orderedSet = new LinkedHashSet<Point2D>(Arrays.asList(vertices));
		orderedSet.remove(spatialPoint);
		vertices = orderedSet.toArray(new Point2D[orderedSet.size()]);
		
		// Make a new FPath
		path = new FPath(vertices, FPath.SPATIAL_WEST, path.getClosed());
		
		// Set the new FPath
		// Make a new history frame.
		if (shapeLayer.getHistory() != null)
			shapeLayer.getHistory().mark();
		f.setPath(path.getSpatialWest());
	}

	void editVertex(Feature f, Point2D oldSpatialPoint, Point2D newSpatialPoint)
	{
		FPath path = f.getPath().getSpatialWest();
		Point2D[] vertices = path.getVertices();
		
		List<Point2D> orderedList = new ArrayList<Point2D>(Arrays.asList(vertices));
		int indexOldValue = orderedList.indexOf(oldSpatialPoint);
		if (indexOldValue != -1) {
			orderedList.set(indexOldValue, newSpatialPoint);
		}
		vertices = orderedList.toArray(new Point2D[orderedList.size()]);
		
		path = new FPath(vertices, FPath.SPATIAL_WEST, path.getClosed());

		if (shapeLayer.getHistory() != null)
			shapeLayer.getHistory().mark();
		f.setPath(path.getSpatialWest());
	}	

	/**
	 * Adds a vertex to the feature at the specified worldPoint.
	 * "rect" specifies a small rectangle around the worldPoint.
	 * This is used in determining between which vertices the
	 * new vertex is to be placed.
	 */
	private void addVertex(Feature f, Point2D worldPoint, Rectangle2D rect)
	{
		// get line that we hit
		FPath path = shapeLayer.getIndex().getWorldPath(f);
		Point2D[] vertArray = path.getVertices();
		int[] indices = getBoundingIndices(vertArray, rect);
		if (indices==null)
			return;
		
		// insert given worldPoint into the line we hit and convert to spatial
		List<Point2D> vertices = new ArrayList<Point2D>(Arrays.asList(vertArray));
		vertices.add (indices[1], worldPoint);
		Point2D[] newPoints = vertices.toArray(new Point2D[0]);
		path = new FPath (newPoints, FPath.WORLD, path.getClosed()).getSpatialWest();
		
		// Make a new history frame.
		if (shapeLayer.getHistory() != null)
			shapeLayer.getHistory().mark();
		
		f.setPath(path);
	}
	
	/**
	 * Draws a ghost outline of the new edge of the current feature. For circles
	 * this computes an entirely new perimeter, for polylines and polygons this
	 * draws the two lines touching the manipulated vertex.
	 */
	public void drawVertexBoundingLines( Graphics2D g2world){
		if (!drawVertexBoundingLinesOK || selectedVertexFeature == null){
			return;
		}
		
		g2world.setColor(Color.DARK_GRAY);
		g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH)/getProj().getPPD()));
		
		if (FeatureUtil.isCircle(geomStyle, selectedVertexFeature)) {
			Feature temp = new Feature();
			for (Field field: geomStyle.getSource().getFields()) {
				temp.setAttribute(field, selectedVertexFeature.getAttribute(field));
			}
			updateCircleRadius(temp);
			g2world.draw(geomStyle.getValue(temp).getWorld().getShape());
			setCircleStatusFrom(temp);
		} else {
			Point2D[] vertices = getBoundingVertices( selectedVertexFeature, selectedVertex);
			for(int i=0; i<vertices.length; i++)
				vertices[i] = getProj().spatial.toWorld(vertices[i]);
			
			g2world.setColor(shapeLayer.getStylesLive().lineColor.getValue(null));
			if (vertices.length==2){
				// figgur out what the current position should be. It might be 360 degrees off. 
				// This check must be done in world coordinates.
				Point2D midVert = getProj().spatial.toWorld(mouseCurr);
				if ( (vertices[0].getX() - midVert.getX() > 180)  || (vertices[1].getX() - midVert.getX() > 180) ){
					midVert.setLocation( midVert.getX() + 360, midVert.getY() );
				} else if ( (midVert.getX()- vertices[0].getX() > 180)  || (midVert.getX() - vertices[1].getX() > 180) ){
					midVert.setLocation( midVert.getX() - 360, midVert.getY() );
				} 
				
				Point2D left  = vertices[0];
				Point2D right = vertices[1];
				Point2D mid   = midVert;
				
				g2world.draw(new Line2D.Double(left, mid));
				g2world.draw(new Line2D.Double(mid, right));
				
			} else if (vertices.length==1) {
				// figgur out what the current position should be. It might be 360 degrees off. 
				// This check must be done in world coordinates.
				Point2D midVert = getProj().spatial.toWorld(mouseCurr);
				if ( (vertices[0].getX() - midVert.getX() > 180)){
					midVert.setLocation( midVert.getX() + 360, midVert.getY() );
				} else if ( (midVert.getX()- vertices[0].getX() > 180)){
					midVert.setLocation( midVert.getX() - 360, midVert.getY() );
				} 
				
				Point2D left  = vertices[0];
				Point2D mid   = midVert;
				
				g2world.draw(new Line2D.Double(left, mid));
			}
		}
	}
	private double theta = 90;
	private boolean ellipseSelected = false;
	private Shape ellipseGhostShape = null;
	private Feature selectedEllipseFeature = null;
	private double ellipseGhostBAdjustment = 0;
	private double ellipseGhostAAdjustment = 0;
	private double thetaAdjustment = 0;
	private int wheelRotation = -1;
	private boolean drawCircle = false;
	private double ellipseUpAndDown = 0;
	private double ellipseRightAndLeft = 0;
	
	private void transformPointToUserSystem(Point2D.Double pt) {
		String lonSystemStr = Config.get(Config.CONFIG_LON_SYSTEM, LongitudeSystem.EAST_360.getName());
		String latSystemStr = Config.get(Config.CONFIG_LAT_SYSTEM, LatitudeSystem.PLANETOCENTRIC);
		LongitudeSystem lonSystem = LongitudeSystem.get(lonSystemStr);
		pt.x = lonSystem.format(pt.getX());
		LatitudeSystem latSystem = LatitudeSystem.get(latSystemStr);
		pt.y = latSystem.format(pt.getY());
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		if (addEllipseGhost || adjustEllipseGhost) {
			wheelRotation = e.getWheelRotation();
			if (wheelRotation != 0) {
				if (e.isControlDown()) {
					if (adjustEllipseGhost) {
						thetaAdjustment += wheelRotation;
						repaint();
					}
				} else if (e.isShiftDown()) {
					if (addEllipseGhost || adjustEllipseGhost) {
						ellipseGhostAAdjustment += wheelRotation;
						repaint();
					}
				} else {
					ellipseGhostBAdjustment += wheelRotation;
					repaint();
				}
			}
		}
	}
	
	public void cancelAddEditEllipseByKeystoke() {
		if (addEllipseGhost) {
			addEllipseGhostStart = true;//reset this
			
		} else if (adjustEllipseGhost) {
			//show the feature again with out a click
			selectedEllipseFeature.setHidden(false);
			addEllipseGhostStart = false;
			
			//need to call draw so the edited ellipse is added back to the buffer
			lview.draw(false);
		}
		
		addEllipseGhost = false;//will be set to true on the next click
		setEllipseLength = false;
		adjustEllipseGhost = false;
		ellipseSelected = false;
		selectedEllipseFeature = null;
		ellipseMouseDown = null;
		ellipseCurrentMouseLocation = null;
		ellipseGhostShape = null;
//		theta = 90;
		ellipseGhostBAdjustment = 0;
		ellipseGhostAAdjustment = 0;
		thetaAdjustment = 0;
		repaint();
		
	}
	public void finishAddEditEllipseByKeystoke() {
		if (addEllipseGhost && ellipseGhostShape != null) {//addEllipseGhostShape gets set to true on the click. The shape is nulled out on escape key
			addEllipseFeature();
			addEllipseGhostStart = true;
		} else if (adjustEllipseGhost) {
			addEllipseFeature();
		}
	}
	public void arrowAddEditEllipseByKeystoke(int key) {
		double factor = 1.0 / (1.0*Main.testDriver.mainWindow.getZoomManager().getZoomPPD());
		switch(key) {
		case KeyEvent.VK_J:
			ellipseGhostAAdjustment -= 1;		
			break;
		case KeyEvent.VK_K:
			ellipseGhostAAdjustment += 1;
			break;
		
		
		case KeyEvent.VK_N:
			ellipseGhostBAdjustment -= 1;		
			break;
		
		case KeyEvent.VK_M:
			ellipseGhostBAdjustment += 1;		
			break;
		
		case KeyEvent.VK_Z:
			thetaAdjustment -= 1;
			break;
			
		case KeyEvent.VK_X:
			thetaAdjustment += 1;
			break;
		
		case KeyEvent.VK_UP:
			ellipseUpAndDown += factor;
			break;
		case KeyEvent.VK_DOWN:
			ellipseUpAndDown -= factor;
			break;
		case KeyEvent.VK_LEFT:
			ellipseRightAndLeft -= factor;
			break;
		case KeyEvent.VK_RIGHT:
			ellipseRightAndLeft += factor;
			break;
		}
		
		repaint();
	}
	public boolean isAddEditEllipse() {
		return addEllipseGhost || adjustEllipseGhost;
	}
	private boolean is5PtEllipse() {
		if (selectedEllipseFeature != null) {
			GeomSource source = (GeomSource)geomStyle.getSource();
			//this is the best way to tell for now. In a future version, we will edit 5pt ellipses.
			if (selectedEllipseFeature.getAttribute(source.getMeanAxisField()) == null) {
				return true;
			}
		}
		return false;
	}
	public void drawEllipseGhost(Graphics2D g2world) {
		if (!adjustEllipseGhost && !addEllipseGhost && !setEllipseLength && !ellipseSelected) {
			return;
		}
		g2world.setColor(Color.DARK_GRAY);
		g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH+1)/getProj().getPPD()));
		if (getMode() == SELECT_FEATURE_MODE) {
			if (adjustEllipseGhost && ellipseSelected && selectedEllipseFeature != null) {
				//we have a selected ellipse draw the ghost
				GeomSource source = (GeomSource)geomStyle.getSource();
				FPath ellipseGhostFP2 = selectedEllipseFeature.getPath();
				EllipseData ed = source.calculateEllipseData(ellipseGhostFP2.getWorld(), getProj());


//				System.out.println("edit feature idx/startingpt: "+ed.majorIndex1+"/"+ed.majorPoint1W);
				
				theta = ed.calculatedThetaWorld;
				
				//adjustTheta
				double tempTheta = theta;
				if (Double.compare(theta+thetaAdjustment,359.99) > 0) {
					//Ex: theta: 179, angleAdjustment: 182 = 361   
					// 360 - (361) = -1 angleAdjustment goes from 182 to 181 which puts us back at 360
					thetaAdjustment = 360 - (theta+thetaAdjustment) - theta;
				} else if (Double.compare(theta+thetaAdjustment, 0.00) < 0) {
					//Ex: theta 175, angleAdjustment -176 = -1 angle should be 359, angleAdjustment should be 360 - theta
					//360 - (-1) = 1 
					thetaAdjustment = 360.0 - theta;
				}
				tempTheta += thetaAdjustment;
				
				ellipseGhostShape = source.getEllipseShapeForEdit(ed, selectedEllipseFeature, tempTheta, ellipseGhostBAdjustment, 
						ellipseGhostAAdjustment, ellipseUpAndDown, ellipseRightAndLeft, getProj());
				
				FPath fp = new FPath(ellipseGhostShape, FPath.WORLD);
				EllipseData testEd = source.calculateEllipseData(fp.getWorld(), getProj());
//				System.out.println("screen dist after edit:" + testEd.majorDistanceScr);
				
				g2world.draw(ellipseGhostShape);	
			}
		} else {
			if (addEllipseGhost) {
				addEllipseGhostStart = false;
				if (ellipseMouseDown == null || ellipseCurrentMouseLocation == null) {
					return;
				}
				GeomSource geomSource = (GeomSource)geomStyle.getSource();
//	            AtomicReference<Double> thetaValue = new AtomicReference<Double>(theta);
	            ellipseGhostShape = geomSource.drawInitialEllipse(ellipseMouseDown, ellipseCurrentMouseLocation, ellipseGhostBAdjustment, getProj());
//	            theta = thetaValue.get();
				
				g2world.draw(ellipseGhostShape);
	
			}
		}
	}
	
	private void addEllipseFeature() {
		if (ellipseGhostShape == null) {
			return;
		}
		GeomSource source = (GeomSource)geomStyle.getSource();
		Feature ellipse = null;
		boolean doneEditing = false;
		if (selectedEllipseFeature != null) {
			ellipse = selectedEllipseFeature;
			ellipse.setHidden(false);
			doneEditing = true;
		} else {
			ellipse = new Feature();
		}
		
		FPath fp = new FPath(ellipseGhostShape, FPath.WORLD);
		ellipse.setAttributeQuiet(Field.FIELD_PATH, fp.getSpatialWest());
		fp.setIsEllipse(true);
		
		EllipseData ed = source.calculateEllipseData(fp.getWorld(), getProj());
//		System.out.println("add feature major pt1: "+ed.majorPoint1W);
//		System.out.println("add feature major pt2: "+ed.majorPoint2W);
//		Point2D major1 = getProj().screen.toWorld(ed.majorPoint1);
//		Point2D major2 = getProj().screen.toWorld(ed.majorPoint2);
//		Point2D minor1 = getProj().screen.toWorld(ed.minorPoint1);
//		Point2D minor2 = getProj().screen.toWorld(ed.minorPoint2);
		double majorDistance = Util.angularAndLinearDistanceWorld(ed.majorPoint1W, ed.majorPoint2W)[1];
		double minorDistance = Util.angularAndLinearDistanceWorld(ed.minorPoint1W, ed.minorPoint2W)[1];

//		double majorDistance = ed.majorDistanceW;
//		double minorDistance = ed.minorDistanceW;
		if (Double.compare(majorDistance, 0) == 0 || Double.compare(minorDistance, 0) == 0) {
			return;
		}
		
		double factor = 1.0 / (1.0*Main.testDriver.mainWindow.getZoomManager().getZoomPPD());
		if (Double.compare(ed.majorDistanceW - ed.minorDistanceW, factor) < 0) {
			drawCircle = true; 
		}
		
//		System.out.println("in save major screen: "+ed.majorDistanceScr);
//		System.out.println("in save minor screen: "+ed.minorDistanceScr);
		
		double theta = 0.00;
		if (!drawCircle) {
			theta = ed.calculatedThetaSpatial;//store in feature as spatial
//			System.out.println("in add: "+theta);
			//theta adjustment
//			double tempTheta = theta + thetaAdjustment;
//			if (Double.compare(tempTheta, 180.00) > 0) {
//				tempTheta = tempTheta - 180.0;
//			}
//			theta = tempTheta;
			
//			tempTheta = ed.calculatedTheta;
//			//adjust theta based on the difference between the formula and what we want
//			if (Double.compare(ed.calculatedTheta, 90.0) > 0) {
//				tempTheta = tempTheta - 180.0;
//			} else if (Double.compare(ed.calculatedTheta, -90.0) < 0) {
//				tempTheta = tempTheta + 180;
//			}
//			//all cases from above add 90
//			tempTheta = tempTheta + 90;
//			double diff = theta - tempTheta;
//			if (Double.compare(diff, 60.0) > 0) {//arbitrary number, but needs to be 80.0 or less
//				theta = theta - 90;
//			} else if (Double.compare(diff, -60.0) < 0) {
//				theta = theta + 90;
//			}
		}
		drawCircle = false;//reset this flag as it may not be true on the next draw
		
		
		//use west leading for transform using CoordinateParser
		Point2D.Double newCenterPt = (Point2D.Double) ellipse.getPath().getSpatialWest().getCenter();
		transformPointToUserSystem(newCenterPt);

//		if (shapeLayer.getHistory() != null) {
//			shapeLayer.getHistory().mark();
//			
//		}
		source.setFieldValue(ellipse, source.getAAxisField(), majorDistance, true);
		source.setFieldValue(ellipse, source.getBAxisField(), minorDistance, true);
		
		double mean = ((Double)ellipse.getAttribute(source.getAAxisField()) + (Double)ellipse.getAttribute(source.getBAxisField())) / 2.0;//uses GeomSource to get scaled values
		ellipse.setAttributeQuiet(source.getMeanAxisField(), mean);
		
		source.setFieldValue(ellipse, source.getAngleField(), theta, false);
		
		source.setFieldValue(ellipse, source.getLatField(), newCenterPt.getY(), true);
		source.setFieldValue(ellipse, source.getLonField(), newCenterPt.getX(), false);
		

		if (doneEditing) {
			//draw both selected and non selected features (true,false)
			lview.draw(true);
			lview.draw(false);
		} else {
			String typeString = FeatureUtil.getFeatureTypeString(fp.getType());
			addFeature(ellipse,typeString);
		}
			
		ellipseGhostShape = null;
		addEllipseGhost = false;
		setEllipseLength = false;
		adjustEllipseGhost = false;
		ellipseSelected = false;
		selectedEllipseFeature = null;
		ellipseMouseDown = null;
		ellipseCurrentMouseLocation = null;
		theta = 90;
		ellipseGhostBAdjustment = 0;
		ellipseGhostAAdjustment = 0;
		thetaAdjustment = 0;
		ellipseUpAndDown = 0.0;
		ellipseRightAndLeft = 0.0;
	}

	/**
	 * draws a "ghost" representation of all the selected features translated by "point".
	 * This must be called by the LView if it desires to see a selection ghost during
	 * mouse drags.
	 */
	public void drawSelectionGhost(Graphics2D g2world){
		if (! drawSelectionGhostOK)
			return;
		
		g2world.setColor(Color.DARK_GRAY);
		g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH)/getProj().getPPD()));
		
		for(Feature feature: shapeLayer.getSelections()) {
			// get translated spatial path
			FPath path = feature.getPath();
			Point2D[] vertices = offsetVertices(path.getSpatialWest().getVertices(), mouseDown, mouseCurr);
			path = new FPath(vertices, FPath.SPATIAL_WEST, path.getClosed());
			
			if (path.getType() == FPath.TYPE_POINT) {
				// could be an actual point, or a circle
				if (FeatureUtil.isCircle(geomStyle, feature)) {
					// compute new temporary circle and draw that
					Feature temp = new Feature();
					for (Field field: geomStyle.getSource().getFields()) {
						temp.setAttribute(field, feature.getAttribute(field));
					}
					temp.setPath(path);
					g2world.draw(geomStyle.getValue(temp).getWorld().getShape());
					setCircleStatusFrom(temp);
				} else {
					// compute new temporary point box and draw that
					Point2D p = path.getWorld().getVertices()[0];
					int pointSize = shapeLayer.getStylesLive().pointSize.getValue(feature).intValue();
					Rectangle2D box = getProj().getClickBox(p, pointSize);
					g2world.fill(box);
				}
			} else {
				g2world.draw(path.getWorld().getShape());
			}
		}
	}
	
	/**
	 * draws the line that indicates the incomplete feature.  This includes a control point at the first
	 * point where a polygon or a point can be defined and another control point at the last clicked point
	 * where a polyline can be defined.
	 * This is called by ShapeLView's paintComponent().
	 */
	public void drawSelectionLine( Graphics2D g2world) {
		if (points.isEmpty()) {
			return;
		}
		
		Styles styles = shapeLayer.getStylesLive();
		Color lineColor = styles.lineColor.getValue(null);
		int pointSize = styles.pointSize.getValue(null).intValue();
		lineColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue());
		g2world.setColor(lineColor);
		g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH)/getProj().getPPD()));
		
		if (getMode() == ADD_CIRCLE_MODE) {
			Feature temp = getCurrentCircle();
			if (temp != null) {
				g2world.draw(geomStyle.getValue(temp).getWorld().getShape());
				setCircleStatusFrom(temp);
			}
		} else if(getMode() == ADD_FIVE_PT_ELLIPSE_MODE){
			//draw the points before the ellipse is created
			for(Point2D p : points){
				p = getProj().spatial.toWorld(p);
				g2world.fill(getProj().getClickBox(p, pointSize));
			}
		} else {
			// draw first click point (the one that defines a polygon.)
			Point2D p = getProj().spatial.toWorld(points.get(0));
			g2world.fill(getProj().getClickBox(p, pointSize));
			
			if (points.size()>1){
				// draw the polygonal outline.
				FPath path = new FPath(points.toArray(new Point2D[points.size()]), FPath.SPATIAL_WEST, false);
				g2world.draw(path.getWorld().getShape());
				
				// draw last click point (the one that defines a polyline.)
				Point2D p2 = getProj().spatial.toWorld(points.get(points.size()-1));
				g2world.fill(getProj().getClickBox(p2, pointSize));
			}
			
			g2world.draw(new Line2D.Double(getProj().spatial.toWorld(points.get(points.size()-1)),
				getProj().spatial.toWorld(mouseCurr)));
			
			if (mouseContext == MouseEvent.MOUSE_DRAGGED && getMode() != MOVE_FEATURE_MODE && !drawSelectionRectangleOK)
				g2world.fill(getProj().getClickBox(getProj().spatial.toWorld(mouseCurr), pointSize));
		}
	}

	/**
	 * draws a rubber band box allowing the user to select features
	 * This is called from the ShapeLView's paintComponent()
	 */
	public void drawSelectionRectangle( Graphics2D g2world)
	{
		if (drawSelectionRectangleOK==false){
			return;
		}
		
		Rectangle2D selectRectangle = new Rectangle2D.Double();
		selectRectangle.setFrameFromDiagonal(getProj().spatial.toWorld(mouseDown),
				getProj().spatial.toWorld(mouseCurr));
		
		Styles styles = shapeLayer.getStylesLive();
		Color lineColor = styles.lineColor.getValue(null);
		lineColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue());
		g2world.setColor(lineColor);
		g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH)/getProj().getPPD()));
		g2world.draw(selectRectangle);
	}

	
	private Rectangle2D rectangleGhost = null;
	public void drawRectangleFeature(Graphics2D g2world) {
		if (!drawRectangleFeatureGhost && !drawRectangleFeatureOK) {
			return;
		}
		if (drawRectangleFeatureGhost) {
			rectangleGhost = new Rectangle2D.Double();
			rectangleGhost.setFrameFromDiagonal(getProj().spatial.toWorld(mouseDown),
					getProj().spatial.toWorld(mouseCurr));
			
			Styles styles = shapeLayer.getStylesLive();
			Color lineColor = styles.lineColor.getValue(null);
			lineColor = new Color(lineColor.getRed(), lineColor.getGreen(), lineColor.getBlue());
			g2world.setColor(lineColor);
			g2world.setStroke( new BasicStroke(((float)STROKE_WIDTH)/getProj().getPPD()));
			g2world.draw(rectangleGhost);
		} else if (drawRectangleFeatureOK) {
			FPath fp = new FPath(rectangleGhost, FPath.WORLD);
			fp = fp.convertTo(FPath.SPATIAL_WEST);
			ArrayList<Point2D> points = new ArrayList<Point2D>();
			Collections.addAll(points, fp.getVertices());
			addFeature(true, points);
			drawRectangleFeatureOK = false;
		}
	}
	
	// adds a polyline or polygon to the FeatureCollection.
	private void addFeature(boolean closed, List<Point2D> points){
		Feature feature = new Feature();
		
		// Convert List to array, and get world-coordinates if in JMars
		Point2D[] vertices = points.toArray(new Point2D[points.size()]);
		for (int i = 0; i < vertices.length; i++) {
			vertices[i] = getProj().spatial.toWorld(vertices[i]);
		}
		
		// Convert vertices to FPath and set spatial west version
		FPath path = new FPath (vertices, FPath.WORLD, closed);
		feature.setPath (path.getSpatialWest());
		String typeString = FeatureUtil.getFeatureTypeString(path.getType());
		feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
		addFeature(feature);
	}
	
	
	
	private void addFeature(Feature feature) {
		addFeature(feature, null);
	}
	private void addFeature (Feature feature, String typeString) {
		// Make a new history frame.
		if (shapeLayer.getHistory() != null)
			shapeLayer.getHistory().mark();

		try {
			if (typeString != null) {
				feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
			}
			shapeLayer.getFeatureCollection().addFeature(feature);
		}
		catch(UnsupportedOperationException ex){
			Util.showMessageDialog("Cannot add Feature, since no default FeatureCollection is set.",
					"Error!",
					JOptionPane.ERROR_MESSAGE);
		}
		initializeSelectionLine();
		repaint();
	}
	
	/**
	 * selects features via a bounding box.  The rectangle is a
	 * rubber band type rectangle drawn by the user. Any
	 * feature that intersects this rectangle is flagged as selected.
	 * @param rectangle (assumed never to be null)
	 */
	private void selectFeatures(Rectangle2D rectangle){
		_selectFeature( rectangle, true, false);
	}
	
	private void toggleFeatures(Rectangle2D rectangle){
		_selectFeature( rectangle, true, true);
	}

	// selects (via a bounding box) the topmost feature with respect to Z-order.
	// The rectangle is a rubber band type rectangle drawn by the user.
	// @param rectangle (assumed never to be null)
 	private  void selectTopmostFeature(Rectangle2D rectangle){
		_selectFeature( rectangle, false, false);
	}

	private void toggleTopmostFeature( Rectangle2D rectangle){
		_selectFeature( rectangle, false, true);
	}
	
	// selects features via a bounding box.  The rectangle is a
	// rubber band type rectangle drawn by the user. Any
	// feature that intersects this rectangle is flagged as selected.
	//
	// @param rectangle (assumed never to be null)
	private  void _selectFeature(Rectangle2D rectangle, boolean multipleSelections, boolean toggleIt)
	{
		Iterator<Feature> it = shapeLayer.getIndex().queryUnwrappedWorld(rectangle);
		List<Feature> hits = new ArrayList<Feature>();
		while (it.hasNext()) {
			hits.add(it.next());
		}
		
		if (!multipleSelections && hits.size() > 0) {
			Feature f = hits.get(hits.size()-1);
			hits.clear();
			hits.add(f);
		}
		
		Set<Feature> selections = shapeLayer.getSelections();
		
		if (toggleIt) {
			// remove hits that were selected
			Set<Feature> selectedHits = new HashSet<Feature>(hits);
			selectedHits.retainAll(selections);
			selections.removeAll(selectedHits);
			hits.removeAll(selectedHits);
		} else {
			// retain only hits
			selections.retainAll(hits);
		}
		
		// add hits that were not selected
		selections.addAll(hits);
		
		//update the appropriate state id
		shapeLayer.increaseStateId(ShapeLayer.IMAGES_BUFFER);
		
		repaint();
		
		//repaint 3d if necessary
		if(ThreeDManager.isReady()){
			ThreeDManager.getInstance().updateDecalsForLView(lview, true);
		}
	}
	
	/**
	 * Returns true if the mouse is over the vertex of a line or polygon.
	 * @param feature Feature object to search.
	 * @param worldRect Rectangle (in world coordinates) to use for proximity matching.
	 * @return intersecting vertex in spatial (west) coordinates as it exists in
	 *         the Feature object. This is important for Point2D.equal() match
	 *         elsewhere in the code, since conversion back and forth between
	 *         world and spatial coordinates looses precision.
	 */
	private Point2D getIntersectingVertex(Feature feature, Rectangle2D worldRect) {
		FPath path = feature.getPath();
		if (path.getType()== FPath.TYPE_POINT){
			return null;
		}
		Point2D[] vertices = shapeLayer.getIndex().getWorldPath(feature).getVertices();
		for (int i=0; i<vertices.length; i++) {
			if (contains360(worldRect, vertices[i].getX(), vertices[i].getY())) {
				return path.getSpatialWest().getVertices()[i];
			}
		}
		return null;
	}
	
	/**
	 * Replaces the "from" spatial vertex with the "to" spatial vertex.  
	 * @param spatialFrom Point to be replaced in Spatial West coordinates.
	 * @param spatialTo The replacement point in Spatial West coordinates.
	 */
	private void moveVertex(Feature f, Point2D spatialFrom, Point2D spatialTo) {
		FPath path = f.getPath().getSpatialWest();
		Point2D[] vertices = path.getVertices();
		int index = Arrays.asList(vertices).indexOf(spatialFrom);
		if (index >= 0) {
			vertices[index].setLocation(spatialTo);
			f.setPath(new FPath(vertices, FPath.SPATIAL_WEST, path.getClosed()).getSpatialWest());
		}
	}
	
	/**
	 * Finds the vertices on either side of the specified vertex of a polyline
	 * or polygon feature, or an empty array of vertices if the given vertex is
	 * not found in the given feature.
	 * 
	 * @param f
	 *            Feature to search.
	 * @param centerVertex
	 *            vertex to search in spatial (west) coordinates.
	 * @return zero/one/two vertices adjacent to the specified vertex.
	 */
	private Point2D[] getBoundingVertices( Feature f, Point2D centerVertex)
	{
		FPath path = f.getPath();
		if (path.getType() == FPath.TYPE_POINT)
			return null;
		
		Point2D[] vertices = path.getSpatialWest().getVertices();
		int index = Arrays.asList(vertices).indexOf(centerVertex);
		if (index == -1)
			return new Point2D[0];
		
		// The index is known so now get the vertices on either side.
		// Special cases if index is the first or last element of the array.
		if(index == 0) {
			if (path.getType()==FPath.TYPE_POLYLINE){
				Point2D[] boundingVertices = new Point2D[1];
				boundingVertices[0] = vertices[index+1];
				return boundingVertices;
			} else { // polygon
				Point2D[] boundingVertices = new Point2D[2];
				boundingVertices[0] = vertices[index+1];
				boundingVertices[1] = vertices[vertices.length-1];
				return boundingVertices;
			}
		} else if(index == vertices.length-1) {
			if (path.getType()== FPath.TYPE_POLYLINE){
				Point2D[] boundingVertices = new Point2D[1];
				boundingVertices[0] = vertices[index-1];
				return boundingVertices;
			} else { // polygon
				Point2D[] boundingVertices = new Point2D[2];
				boundingVertices[0] = vertices[index-1];
				boundingVertices[1] = vertices[0];
				return boundingVertices;
			}
		} else {
			// no special case.
			Point2D[] boundingVertices = new Point2D[2];
			boundingVertices[0] = vertices[index+1];
			boundingVertices[1] = vertices[index-1];
			return boundingVertices;
		}
	}
	
	/**
	 * If the feature is a line it must have at least 2 points after the delete.
	 * If the feature is a polygon it must have at least 3 points after the
	 * delete. If the feature is a point or circle this method always returns
	 * false.
	 */
	private boolean canDeleteVertex(Feature f) {
		if (f.getPath().getType() == FPath.TYPE_POINT) {
			return false;
		}
		FPath path = f.getPath();
		int numPoints = path.getVertices().length;
		switch (path.getType()) {
		case FPath.TYPE_POLYGON:
			return numPoints > 3;
		case FPath.TYPE_POLYLINE:
			return numPoints > 2;
		case FPath.TYPE_POINT:
		default:
			return false;
		}
	}
	
	/**
	 * @return the indices of the vertices which form the first line that
	 *         intersects the given rectangle.
	 */
	private int[] getBoundingIndices(Point2D[] vertices, Rectangle2D rect) {
		final Line2D line = new Line2D.Double();
		for (int i = 0; i < vertices.length; i++) {
			int j = (i+1)%vertices.length;
			line.setLine(vertices[i], vertices[j]);
			if (intersects360(line, rect)) {
				return new int[]{i,j};
			}
		}
		return null;
	}
	
	private boolean intersects360(Shape shape, Rectangle2D worldRect) {
		for (Rectangle2D r: Util.toWrappedWorld(worldRect)) {
			for (Rectangle2D r2: Util.toUnwrappedWorld(r, shape.getBounds2D())) {
				if (shape.intersects(r2)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean contains360(Shape shape, double x, double y) {
		Rectangle2D bounds = shape.getBounds2D();
		double start = Math.floor(bounds.getMinX() / 360.0) * 360.0 + x;
		//avoid the possibility of a really long while loop or 
		// infinite while loop by checking to make sure the 
		// two points aren't unrealistically far apart
		if(start-bounds.getMaxX()>20000){
			return false;
		}
		//it's possible the start could be greater than the bounds,
		// if they are bring them below the bounds and go from there
		while(start>bounds.getMaxX()){
			start = start - 360;
		}
		for (double x2 = start; x2 < bounds.getMaxX(); x2 += 360.0) {
			if (bounds.contains(x2,y)) {
				return true;
			}
		}
		return false;
	}
	private void getMinMaxCoord(double[] minMaxCoords, double[] coords, boolean latFirst) {
	    double x = 0;
	    for(int i=0; i<coords.length; i=i+2) {
	        if (latFirst) {
	            //ignore lat values
	            x = coords[i+1];
	        } else {
	            x = coords[i];
	        }
            if (Double.compare(x,minMaxCoords[0]) < 0) {
                //the value is less than the min, set it as the min
                minMaxCoords[0] = x;
            }
            if (Double.compare(x, minMaxCoords[1]) > 0) {
                //the value is greater than the max, set it as the max
                minMaxCoords[1] = x;
            }

//            System.out.println("X: "+x);
         }
	}
	private double[] adjustArrayForPM(double[] coords, boolean latFirst) {
	    double[] newCoords = new double[coords.length];
	    double x = 0;
	    double y = 0;
	    for(int i=0; i<coords.length; i=i+2) {
	        if (latFirst) {
	            y = coords[i];
	            x = coords[i+1];
	        } else {
	            x = coords[i];
                y = coords[i+1];
	        } 
            //check and adjust lon values
            if (Double.compare(x, 180) < 0) {
                //this value is less than 180, add 360
                x = x + 360;
            } else if (Double.compare(x, 360) > 0) {
                //this value is less than 180, add 360
                x = x - 360;
            }
            
            
            if (latFirst) {
                newCoords[i] = y;
                newCoords[i+1] = x;
            } else {
                newCoords[i] = x;
                newCoords[i+1] = y;
            }
            
	        
        }
	    return newCoords;
	}
	private GeneralPath coordsToGeneneralPath(double[] coords, boolean closed, boolean latFirst) {
	    Point2D[] points = new Point2D[coords.length/2];
        int x = (latFirst ? 1 : 0);
        int y = (latFirst ? 0 : 1);
        for (int i = 0; i < coords.length/2; i++) {
            points[i] = new Point2D.Double (coords[i*2 + x], coords[i*2 + y]);
        }
	    GeneralPath path = new GeneralPath();
	    if (points.length > 0) {
            path.moveTo(points[0].getX(), points[0].getY());
            for (int i = 1; i < points.length; i++) {
                path.lineTo(points[i].getX(), points[i].getY());
            }
            if (closed) {
                path.closePath();
            }
        }
	    return path;
    }
	// This is an overwrite of the standard layer getMenuItems() method.
	public Component [] getMenuItems( Point2D wp)
	{
		if (wp==null) {
			return null;
		}
		this.worldPt = wp;
		this.rect = getProj().getClickBox( worldPt, PROXIMITY_BOX_SIDE);
		
		// build the list of menu items.
		List<JMenuItem> menuList = new ArrayList<JMenuItem>();
		// selections are used multiple times to enable/disable options
		ObservableSet<Feature> selections = shapeLayer.getSelections();
		
		if (changeModeOK) {
			menuList.add(zmenu);
			
			if (mode == SELECT_FEATURE_MODE) {
				selectModeRadioButton.setSelected(true); // action will kick in
				resetDrawShapes();
			} else {
				zmenu.setText(DRAW_TXT);
				selectModeRadioButton.setSelected(false); // action will kick in
				enableDrawShapes();
			}
		}
		
		if (deleteFeaturesOK){
			// One should only be able to delete a selected row if there is
			// in fact at least one row selected.
			if (selections.size() >0){
				deleteRowMenuItem.setEnabled(true);
			} else {
				deleteRowMenuItem.setEnabled(false);
			}
			menuList.add( deleteRowMenuItem);
		}
		
		if (zorderOK){
			// The Zorder menu should only be enabled if there is one selection.
			if (selections.size() >0){
				zOrderMenuItem.setEnabled(true);
			} else {
				zOrderMenuItem.setEnabled(false);
			}
			menuList.add(zOrderMenuItem);
		}
		
		if (addVertexOK || deleteVertexOK) {
			boolean addPoints = false;
			boolean delPoints = false;
			boolean isEditPoints = false;
			Iterator<? extends Feature> it = shapeLayer.getIndex().queryUnwrappedWorld(rect);
			while (it.hasNext() && (!addPoints || !delPoints)) {
				Feature f = it.next();
				if (selections.contains(f) &&
						f.getPath().getType() != FPath.TYPE_POINT) {
					if (getIntersectingVertex(f, rect) == null) {
						addPoints = true;
					} else {
						delPoints = true;
					}
				} if (selections.contains(f)
						&& (f.getPath().getType() == FPath.TYPE_POINT || 
						f.getPath().getType() == FPath.TYPE_POLYLINE ||
					    f.getPath().getType() == FPath.TYPE_POLYGON)) {
					
					if (f.getPath().getType() == FPath.TYPE_POINT) {
					    isEditPoints = true;
					} else if (f.getPath().getType() == FPath.TYPE_POLYLINE) {
						if (getIntersectingVertex(f, rect) != null) {
							 isEditPoints = true;
						}
					} else if (f.getPath().getType() == FPath.TYPE_POLYGON) {
						StyleSource<FPath> source = shapeLayer.getStyles().geometry.getSource();
						if (source instanceof GeomSource) {
							GeomSource gs = (GeomSource) source;
							if (FeatureUtil.isEllipseSource(gs, f)) {
								isEditPoints = false;
							} else if (getIntersectingVertex(f, rect) != null) {
								isEditPoints = true;
							}
						} else if (getIntersectingVertex(f, rect) != null) {
							isEditPoints = true;
						}
					}
				} else {
					isEditPoints = false;
				}
			}
			if (addVertexOK) {
				addPointMenuItem.setEnabled(addPoints);
				menuList.add( addPointMenuItem);
			}
			if (deleteVertexOK) {
				deletePointMenuItem.setEnabled(delPoints);
				editPointMenuItem.setEnabled(isEditPoints);
				menuList.add(editPointMenuItem);
				menuList.add(deletePointMenuItem);
			}
		}
		
		if (selections.size()>0) {
			findStampsMenu.setEnabled(true);
		} else {
			findStampsMenu.setEnabled(false);
		}
		menuList.add(findStampsMenu);
	
		for (Feature feature : shapeLayer.getSelections()) {
			  if (feature.getPath().getType() == FPath.TYPE_POLYLINE) {
				menuList.add(convertToProfileMenuItem);	
				break;
			}
		}
		
		//have all polygon functions enabled to start
		// and disable and add tooltips to some depending  
		// on the number of selected shapes
		pixelExportMenuItem.setEnabled(true);
		convertCirclesToPolygonItem.setEnabled(true);
//		convertCirclesToEllipsesItem.setEnabled(true);
		makeLotsOfSubShapesItem.setEnabled(true);
		duplicateMenuItem.setEnabled(true);
		subtractMenuItem.setEnabled(true);
		intersectMenuItem.setEnabled(true);
		mergeMenuItem.setEnabled(true);
		addBufferMenuItem.setEnabled(true);
		splitPolylineMenuItem.setEnabled(true);
		//hide all tooltips to start
		pixelExportMenuItem.setToolTipText("");
		convertCirclesToPolygonItem.setToolTipText("");
		convertCirclesToEllipsesItem.setToolTipText("");
		makeLotsOfSubShapesItem.setToolTipText("");
		duplicateMenuItem.setToolTipText("");
		subtractMenuItem.setToolTipText("");;
		intersectMenuItem.setToolTipText("");;
		mergeMenuItem.setToolTipText("");;
		addBufferMenuItem.setToolTipText("");
		splitPolylineMenuItem.setToolTipText("");;
		if(selections.size()>0){
			shapeFunctionsMenu.setEnabled(true);
			int size = selections.size();
			//pixel export needs exactly one selection
			if(size < 1){
				pixelExportMenuItem.setEnabled(false);
				pixelExportMenuItem.setToolTipText("Select at least 1 shape");
			}
			//intersect and merge need at least two selection
			if(size<2){
				intersectMenuItem.setEnabled(false);
				intersectMenuItem.setToolTipText("Select at least overlapping 2 shapes");
				mergeMenuItem.setEnabled(false);
				mergeMenuItem.setToolTipText("Select at least overlapping 2 shapes");
			}
			if(size != 2){
				//subtract needs exactly two selections
				subtractMenuItem.setEnabled(false);
				subtractMenuItem.setToolTipText("Select 2 overlapping shapes");
			}
		}else{
			shapeFunctionsMenu.setEnabled(false);
		}
		menuList.add(shapeFunctionsMenu);
		Component[] menuItems = (Component [])menuList.toArray( new Component[0]);
		return menuItems;
	}
	
	// The constructor for the class that sets up the components and 
	// all the behavior for those components.
	private void setupContextMenu() {
		// set up the context menu items.
		createDrawShapesMenuItems();
		
		groupDrawShapesMenuItems();
		
		createWrapperMenuForDrawShapes();
		
		zOrderMenuItem           = new ZOrderMenu("Z-order",
				shapeLayer.getFeatureCollection(),
				shapeLayer.getSelections(),
				lview.getFocusPanel().getFeatureTable().getSorter());
		
		deletePointMenuItem      = new JMenuItem( "Delete Point");
		addPointMenuItem         = new JMenuItem( "Add Point");
		editPointMenuItem		=	new JMenuItem( "Edit Point");
		deleteRowMenuItem        = new JMenuItem( "Delete Selected Features");
		findStampsMenu		 = new JMenu( "Find overlapping stamps");
		shapeFunctionsMenu = new JMenu("Polygon Functions");
		intersectMenuItem = new JMenuItem("Intersect Polygons");
		subtractMenuItem = new JMenuItem("Subtract Polygons");
		mergeMenuItem = new JMenuItem("Merge Polygons Together");
		duplicateMenuItem = new JMenuItem("Duplicate Polygons");
		pixelExportMenuItem = new JMenuItem("Export Pixel Data for Polygon...");
		convertCirclesToPolygonItem = new JMenuItem("Convert circles to polygons");
		convertCirclesToEllipsesItem = new JMenuItem("Convert circles to ellipses");
		addBufferMenuItem = new JMenuItem("Add Buffer");
		makeLotsOfSubShapesItem = new JMenuItem("Divide into rectangles");
		splitPolylineMenuItem = new JMenuItem("Split polylines into line segments");
		convertToProfileMenuItem = new JMenuItem("Export polyline to profile");
		convertToProfileMenuItem.setToolTipText(fromShapeToProfiletooltip);

		addStreamModeRadioButton.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (addStreamModeRadioButton.isSelected()) {
					points.clear();
					addPointsOK = true;
					addLinesOK = true;
					addPolysOK = true;
					shapeLayer.getSelections().clear();
					informObserverForAction(addStreamModeRadioButton); 
					setMode(FeatureMouseHandler.ADD_FEATURE_STREAM_MODE);
					addStreamModeRadioButton.setIcon(DrawingPalette.iselfreehand);
				} else {
					addStreamModeRadioButton.setIcon(DrawingPalette.ifreehand);
				}
			}
		});
		
		addPointsOnlyRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addPointsOnlyRadioButton.isSelected()) {
					points.clear();
					addPointsOK=true;
					addLinesOK=false;
					addPolysOK=false;
					informObserverForAction(addPointsOnlyRadioButton);
					setMode( FeatureMouseHandler.ADD_FEATURE_MODE);
					shapeLayer.getSelections().clear();
					addPointsOnlyRadioButton.setIcon(DrawingPalette.iselpoint);
				} else {
					addPointsOnlyRadioButton.setIcon(DrawingPalette.ipoint);
				}
			}
		});

		addLinesOnlyRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addLinesOnlyRadioButton.isSelected()) {
					points.clear();
					addPointsOK=false;
					addLinesOK=true;
					addPolysOK=false;
					informObserverForAction(addLinesOnlyRadioButton);
					setMode( FeatureMouseHandler.ADD_FEATURE_MODE);
					shapeLayer.getSelections().clear();
					addLinesOnlyRadioButton.setIcon(DrawingPalette.iselline);
				} else {
					addLinesOnlyRadioButton.setIcon(DrawingPalette.iline);
				}
			}
		});

		addPolygonsOnlyRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addPolygonsOnlyRadioButton.isSelected()) {
					points.clear();
					addPointsOK=false;
					addLinesOK=false;
					addPolysOK=true;
					informObserverForAction(addPolygonsOnlyRadioButton);
					setMode(FeatureMouseHandler.ADD_FEATURE_MODE);
					shapeLayer.getSelections().clear();
					addPolygonsOnlyRadioButton.setIcon(DrawingPalette.iselpoly);
				} else {
					addPolygonsOnlyRadioButton.setIcon(DrawingPalette.ipoly);
				}
			}
		});
		addEllipseRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addEllipseRadioButton.isSelected()) {
					points.clear();
					int prevMode = getMode();
					boolean success = setGeomSource(ADD_ELLIPSE_MODE);
					if (success) {
						addEllipseGhostStart = true;
						setEllipseLength=false;
						addEllipseGhost=false;
						adjustEllipseGhost=false;
						
						shapeLayer.getSelections().clear();
						
						informObserverForAction(addEllipseRadioButton); 
						setMode( FeatureMouseHandler.ADD_ELLIPSE_MODE);
						shapeLayer.getSelections().clear();
						addEllipseRadioButton.setIcon(DrawingPalette.iselellipse);
					} else {
						//reset the mode and the selected radio button
						switch (prevMode){
						case ADD_FEATURE_MODE:
							if (addPointsOK) { addPointsOnlyRadioButton.setSelected(true); }
							else if (addLinesOK) { addLinesOnlyRadioButton.setSelected(true); }
							else if (addPolysOK) { addPolygonsOnlyRadioButton.setSelected(true); }
							break;
						case ADD_CIRCLE_MODE:
							addCircleRadioButton.setSelected(true);
							break;
						case ADD_FIVE_PT_ELLIPSE_MODE:
							add5PtEllipseRadioButton.setSelected(true);
							break;
						case ADD_RECTANGLE_MODE:
							addDrawRectangleRadioButton.setSelected(true);
							break;														
						case ADD_FEATURE_STREAM_MODE:
							addStreamModeRadioButton.setSelected(true);
							break;
						case SELECT_FEATURE_MODE:
							selectModeRadioButton.setSelected(true);
							DrawingPalette.INSTANCE.resetDrawMode(lview);
							break;
						}
					}
				} else {
					addEllipseRadioButton.setIcon(DrawingPalette.iellipse);
				}
			}
		});

		
		addCircleRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (addCircleRadioButton.isSelected()) {
					
					shapeLayer.getSelections().clear();
					points.clear();
					int prevMode = getMode();
					
					boolean success = setGeomSource(ADD_CIRCLE_MODE);
					if (success) {
						informObserverForAction(addCircleRadioButton);
						setMode(FeatureMouseHandler.ADD_CIRCLE_MODE);
						addCircleRadioButton.setIcon(DrawingPalette.iselcircle);
					} else {//valid field not selected
						switch (prevMode){
						case ADD_FEATURE_MODE:
							if (addPointsOK) { addPointsOnlyRadioButton.setSelected(true); }
							else if (addLinesOK) { addLinesOnlyRadioButton.setSelected(true); }
							else if (addPolysOK) { addPolygonsOnlyRadioButton.setSelected(true); }
							break;
						case ADD_CIRCLE_MODE:
							addCircleRadioButton.setSelected(true);
							break;
						case ADD_FIVE_PT_ELLIPSE_MODE:
							add5PtEllipseRadioButton.setSelected(true);
							break;
						case ADD_RECTANGLE_MODE:
							addDrawRectangleRadioButton.setSelected(true);
							break;								
						case ADD_FEATURE_STREAM_MODE:
							addStreamModeRadioButton.setSelected(true);
							break;
						case SELECT_FEATURE_MODE:
							selectModeRadioButton.setSelected(true);
							DrawingPalette.INSTANCE.resetDrawMode(lview);
							break;
						}
					}
					
				} else {
					addCircleRadioButton.setIcon(DrawingPalette.icircle);
				}
			} //itemStateChanged event
		}); //itemListener
		
		addDrawRectangleRadioButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				if (addDrawRectangleRadioButton.isSelected()) {
					points.clear();
					shapeLayer.getSelections().clear();
					informObserverForAction(addDrawRectangleRadioButton);
					setMode( FeatureMouseHandler.ADD_RECTANGLE_MODE);
					drawRectangleFeatureGhost = false;
					drawRectangleFeatureOK = false;
					addDrawRectangleRadioButton.setIcon(DrawingPalette.iselrectangle);
				} else {
					addDrawRectangleRadioButton.setIcon(DrawingPalette.irectangle);
				}
			}
		}); 
			
		
		add5PtEllipseRadioButton.addItemListener( new ItemListener() {
			public void itemStateChanged(ItemEvent event){
				if (add5PtEllipseRadioButton.isSelected()) {
					//grab the previous mode, in case the user cancels
					// out of the fields dialog
					int prevMode = getMode();
					
					shapeLayer.getSelections().clear();
					boolean success = setGeomSource(ADD_FIVE_PT_ELLIPSE_MODE);
					if (success) {	
						points.clear();
						informObserverForAction(add5PtEllipseRadioButton);
						setMode(FeatureMouseHandler.ADD_FIVE_PT_ELLIPSE_MODE);
						add5PtEllipseRadioButton.setIcon(DrawingPalette.INSTANCE.isel5ptellipse);
					} else {
						//reset the mode and the selected radio button
						switch (prevMode){
						case ADD_FEATURE_MODE:
							if (addPointsOK) { addPointsOnlyRadioButton.setSelected(true); }
							else if (addLinesOK) { addLinesOnlyRadioButton.setSelected(true); }
							else if (addPolysOK) { addPolygonsOnlyRadioButton.setSelected(true); }
							break;
						case ADD_CIRCLE_MODE:
							addCircleRadioButton.setSelected(true);
							break;
						case ADD_FIVE_PT_ELLIPSE_MODE:
							add5PtEllipseRadioButton.setSelected(true);
							break;
						case ADD_RECTANGLE_MODE:
							addDrawRectangleRadioButton.setSelected(true);
							break;														
						case ADD_FEATURE_STREAM_MODE:
							addStreamModeRadioButton.setSelected(true);
							break;
						case SELECT_FEATURE_MODE:
							selectModeRadioButton.setSelected(true);
							DrawingPalette.INSTANCE.resetDrawMode(lview);
							break;
						}
					}
				} else {
					add5PtEllipseRadioButton.setIcon(DrawingPalette.i5ptellipse);
				}
			}
		});
		
		
		
		selectModeRadioButton.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				if (selectModeRadioButton.isSelected()) {
					informObserverForAction(selectModeRadioButton);
					setMode(FeatureMouseHandler.SELECT_FEATURE_MODE);
					resetDrawShapes();
					// if we are changing to select mode, we need to get rid
					// of any selection line that is being drawn.
					initializeSelectionLine();
					setCursor(shapeLayer.getFeatureCollection().getFeatureCount() > 0 ? SELECT_CURSOR :  DEFAULT_CURSOR);
				}
			}
		});
			
		deleteRowMenuItem.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e){
				if (shapeLayer.getHistory() != null)
					shapeLayer.getHistory().mark();
				// if this action is legal for the dataset, it will cascade into
				// removing the features from the selections set as well
				shapeLayer.getFeatureCollection().removeFeatures(shapeLayer.getSelections());
			}
		});
		
		deletePointMenuItem.addActionListener( new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (rect==null){
					return;
				}
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature feature: shapeLayer.getSelections()) {
					FPath path = idx.getWorldPath(feature);
					if (path.intersects(rect)) {
						Point2D vertex = getIntersectingVertex( feature,rect);
						if (vertex!=null){
							if (canDeleteVertex( feature)) {
								deleteVertex( feature, vertex);
							} else {
								String message = "Cannot delete point. \n";
								switch (path.getType()) {
								case FPath.TYPE_POLYGON:
									message += "Polygons must have at least three vertices.";
									break;
								case FPath.TYPE_POLYLINE:
									message += "Polylines must have at least two vertices.";
									break;
								case FPath.TYPE_POINT:
									message += "Points must consist of one vertex";
									break;
								}
								Util.showMessageDialog(message);
							}
						}
					}
				}
			}
		});

		editPointMenuItem.addActionListener( new ActionListener(){
			public void actionPerformed(ActionEvent e){
				if (rect==null){
					return;
				}
				List<String> varPoints = new ArrayList<>();
				Point2D vertex = null;
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature feature: shapeLayer.getSelections()) {
					FPath path = idx.getWorldPath(feature);
					if (path.intersects(rect)) {
						if (feature.getPath().getType()== FPath.TYPE_POINT){
							vertex = feature.getPath().getVertices()[0];
							
						} else {
							vertex = getIntersectingVertex( feature,rect);
						}
						if (vertex!=null){
							String varPoint = getCoordOrdering().format(vertex);
							varPoints.clear();
							varPoints.add(varPoint);
							editCoordsDialog.withPoints(varPoints);
							editCoordsDialog.withFeature(feature);
							editCoordsDialog.withVertex(vertex);
							editCoordsDialog.withMouseHandler(FeatureMouseHandler.this);
							editCoordsDialog.setVisible(true);
						}
					}
				}
			}

			private Ordering getCoordOrdering() {
				String coordOrdering = Config.get(Config.CONFIG_LAT_LON, Ordering.LAT_LON.asString());
				Ordering ordering = Ordering.get(coordOrdering);
				return ordering;
			}
			
		});		
		
		convertToProfileMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				List<FPath> selectedpolylines = new ArrayList<>();				
				for (Feature feature : shapeLayer.getSelections()) {
					if (feature.getPath().getType() == FPath.TYPE_POLYLINE) {
						selectedpolylines.add(feature.getPath());
					}
				}
				if (!selectedpolylines.isEmpty()) {
					ProfileLView profileview = getInstanceOfProfileView();
					profileview.convertToProfile(selectedpolylines);
				}
			}

			private ProfileLView getInstanceOfProfileView() {
				ProfileLView lview = null;
				for (LView view : LManager.getLManager().getViewList()) {
					if (view instanceof ProfileLView) {
						lview = (ProfileLView) view;
						return lview;
					}
				}
				return lview;
			}
		});		
		
		addPointMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (rect == null || worldPt == null)
					return;
				MemoryFeatureIndex idx = shapeLayer.getIndex();
				for (Feature feature: shapeLayer.getSelections()) {
					FPath path = idx.getWorldPath(feature);
					if (path.getType() != FPath.TYPE_POINT &&
							path.intersects(rect)) {
						addVertex(feature, worldPt, rect);
						break;
					}
				}
			}
		});

	    Set<String> layerTypes=StampFactory.getLayerTypes();
	    	    
	    for(final String type : layerTypes) {
			JMenuItem findOverlaps= new JMenuItem("Find intersecting " + type + " stamps");
			findOverlaps.addActionListener(new ActionListener(){
				public void actionPerformed(ActionEvent e) {
					ArrayList<GeneralPath> allStampPaths = new ArrayList<GeneralPath>();
					ArrayList<String> allStampIds = new ArrayList<String>();
					
					// When calculating intersections, do so according to how the shape is being presented to the user, not how it is represented internally.
					// Example: Points that are displayed to the user as Circles should intersect a much larger area than non-circular Points.
					int size=shapeLayer.getSelections().size();
					ShapeRenderer sr = lview.createRenderer(true, size);
					
					int cnt=1;
					for (Feature f : shapeLayer.getSelections()) {
						allStampPaths.add(new GeneralPath(sr.getPath(f).convertTo(FPath.WORLD).getShape()));
						Field label = new Field("Label", String.class);
						Object attr = f.getAttribute(label);
						allStampIds.add(attr!=null?(String)attr:"Unnamed Shape "+ cnt++);
					}

					StampFactory.createOverlappingStampLayer(type, allStampIds, allStampPaths);
				}
			});		 
			findStampsMenu.add(findOverlaps);
	    }		
	    
	    convertCirclesToPolygonItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				int size=shapeLayer.getSelections().size();
				ShapeRenderer sr = lview.createRenderer(true, size);
				
				ArrayList<Feature> toDelete = new ArrayList<Feature>();
				for (Feature f : shapeLayer.getSelections()) {
					if (f.getPath().getType() == FPath.TYPE_POINT) {
						FPath path2 = sr.getPath(f);
						FPath temp = path2.convertTo(FPath.WORLD);
						Feature f2 = new Feature();
						f2.setPath(temp);
						addFeature(f2);
						toDelete.add(f);
					}
				}
				for (int x=0; x<toDelete.size(); x++) {
					Feature f = toDelete.get(x);
					shapeLayer.getFeatureCollection().removeFeature(f);
				}
			}
		});
	    convertCirclesToEllipsesItem.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ArrayList<Feature> toDelete = new ArrayList<Feature>();
				
				boolean success = setGeomSource(CONVERT_CIRCLE_TO_ELLIPSE_MODE);
				if (success) {
					StyleSource<FPath> source = shapeLayer.getStylesLive().geometry.getSource();
					GeomSource geomSource = (GeomSource) source;
					for (Feature f : shapeLayer.getSelections()) {
						if (f.getPath().getType() == FPath.TYPE_POINT) {
							Feature ellipse = new Feature();
							Set<Field> keySet = f.attributes.keySet();
							for (Field field : keySet) {
								ellipse.setAttributeQuiet(field, f.attributes.get(field));
							}
							
							double radius = (Double) f.getAttribute(geomSource.getRadiusField());
							radius = geomSource.scaleRadiusToKm(radius);
							FPath path2 = GeomSource.getCirclePath(f.getPath(), radius, 36);

							
							ellipse.setAttributeQuiet(Field.FIELD_PATH, path2.getSpatialWest());
							path2.setIsEllipse(true);
							
							Point2D.Double centerPt = (Point2D.Double) f.getPath().getCenter();
							transformPointToUserSystem(centerPt);
							double centerX = centerPt.getX();
							double centerY = centerPt.getY();
							
							double majorDistance = radius * 2.0;
							double minorDistance = radius * 2.0;
							geomSource.setFieldValue(ellipse, geomSource.getAAxisField(), majorDistance, true);
							geomSource.setFieldValue(ellipse, geomSource.getBAxisField(), minorDistance, true);
							
							double mean = ((Double)ellipse.getAttribute(geomSource.getAAxisField()) + (Double)ellipse.getAttribute(geomSource.getBAxisField())) / 2.0;//need to get scaled values
							ellipse.setAttributeQuiet(geomSource.getMeanAxisField(), mean);
							
							
							
							geomSource.setFieldValue(ellipse, geomSource.getAngleField(), 0.0, true);
							geomSource.setFieldValue(ellipse, geomSource.getLatField(), centerY, true);
							geomSource.setFieldValue(ellipse, geomSource.getLonField(), centerX, false);
							
							
							String typeString = FeatureUtil.getFeatureTypeString(path2.getType());
							addFeature(ellipse,typeString);
							
							
							toDelete.add(f);
						}
					}
					for (int x=0; x<toDelete.size(); x++) {
						Feature f = toDelete.get(x);
						shapeLayer.getFeatureCollection().removeFeature(f);
					}
					
					ellipseGhostShape = null;
					addEllipseGhost = false;
					setEllipseLength = false;
					adjustEllipseGhost = false;
					ellipseSelected = false;
					selectedEllipseFeature = null;
					ellipseMouseDown = null;
					ellipseCurrentMouseLocation = null;
//					theta = 90;
					ellipseGhostBAdjustment = 0;
					ellipseGhostAAdjustment = 0;
					thetaAdjustment = 0;
					ellipseUpAndDown = 0.0;
					ellipseRightAndLeft = 0.0;
				}
			}
		});

	    makeLotsOfSubShapesItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
        		new DivideShapeDialog(Main.mainFrame, shapeFunctionsMenu, shapeLayer).setVisible(true);
			}
		});
	    
	    
	    //create the polygon functions menu items
	    intersectMenuItem.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent e) {
                Area startingArea = new Area();
                ObservableSet<Feature> selections = shapeLayer.getSelections();
                        
                Feature[] selectedArr = selections.toArray(new Feature[]{});
                
                //need to initially go through each selected shape and see if we need to run them all through the adjustment for crossing the PM
                double minCoord = Double.MAX_VALUE;
                double maxCoord = Double.MIN_VALUE;
                double[] minMaxVals = new double[]{minCoord,maxCoord};
                boolean crossesPM = false;
                for (Feature feature : selectedArr) {
                    if (!(feature.getPath().getType() == FPath.TYPE_POLYGON)) {
                        Util.showMessageDialog(
                                "This function is only available for polygons.",
                                "Intersect results",
                                JOptionPane.PLAIN_MESSAGE);
                        return;
                    }
                    FPath fp = feature.getPath();
                    fp = fp.convertTo(FPath.WORLD);
                    double[] coords = fp.getCoords(false);

                    getMinMaxCoord(minMaxVals, coords, false);
                    
                    //now that we have the min and max coordinates for this shape, see if the difference is greater than 180
                    double difference = minMaxVals[1] - minMaxVals[0];
                    if (Double.compare(difference, 180) > 0) {
                        crossesPM = true;
                        break;
                    }
                }
                boolean firstTime = true;
                for (Feature feature : selectedArr) {
                    
                    //add 360 to all coordinates to avoid prime meridian issues
                    FPath fp = feature.getPath();
                    fp = fp.convertTo(FPath.WORLD);
                    double[] coords = fp.getCoords(false);
                    double[] newCoords = coords;
                    if (crossesPM) {
                        //our ending shape will cross the PM, so we need to adjust all values
                        newCoords = adjustArrayForPM(coords, false);
                    }
                    GeneralPath newPath = coordsToGeneneralPath(newCoords, feature.getPath().getClosed(), false);
                    if (firstTime) {
                        //first time through, we don't have anything in the startingArea, set it to the first shape and go from there
                        
                        startingArea.add(new Area(newPath));
                        firstTime = false;
                    }
                    startingArea.intersect(new Area(newPath));
                }
                
                FPath resultFP = new FPath (startingArea, FPath.WORLD);
                resultFP = resultFP.convertTo(FPath.SPATIAL_WEST);
                if (resultFP.getPathCount() > 0) {//avoid ArrayIndexOutOfBoundsException
                    int cnt = resultFP.getPathCount();
                    for (int i=0; i<cnt; i++) {
                        Feature feature = new Feature();
                        FPath p = new FPath(resultFP.getVertices(i), FPath.SPATIAL_WEST, true);
                        feature.setPath (p);
                        String typeString = FeatureUtil.getFeatureTypeString(resultFP.getType());
                        feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
                        addFeature(feature);
                    }
                } else {
                    Util.showMessageDialog(
                            selections.size() + " shapes were selected with no intersection returned.",
                            "Intersect results",
                            JOptionPane.PLAIN_MESSAGE);
                }
                        
            }
		});	
	    
	    subtractMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                Area intersectArea = new Area();
                ObservableSet<Feature> selections = shapeLayer.getSelections();
     
                Feature[] selectedArr = selections.toArray(new Feature[]{});
	            Feature first = selectedArr[0];
	            if (!(selectedArr[0].getPath().getType() == FPath.TYPE_POLYGON) || !(selectedArr[1].getPath().getType() == FPath.TYPE_POLYGON)) {
	                Util.showMessageDialog(
	                        "This function is only available for polygons.",
	                        "Intersect results",
	                        JOptionPane.PLAIN_MESSAGE);
	                return;
	            }
	            //add 360 to all coordinates to avoid prime meridian issues
	            FPath fp = first.getPath();
	            fp = fp.convertTo(FPath.WORLD);//convert to world before doing any coordinate manipulation
	            double[] coords = fp.getCoords(false);
	            double[] newCoords = coords;
	            double minCoord = Double.MAX_VALUE;
	            double maxCoord = Double.MIN_VALUE;
	            double[] minMaxCoords = new double[]{minCoord,maxCoord};
	            getMinMaxCoord(minMaxCoords, coords, false);
	            
	            //now that we have the min and max coordinates for this shape, see if the difference is greater than 180
	            double difference = minMaxCoords[1] - minMaxCoords[0];
	            if (Double.compare(difference, 180) > 0) {
	                //it is greater than 180, we need to add 360 to any value less than 180
	                newCoords = adjustArrayForPM(coords, false);
	            }
	            
	            GeneralPath firstNewPath = coordsToGeneneralPath(newCoords, first.getPath().getClosed(), false);
	            Area firstArea = new Area(firstNewPath);
	            
	            //add 360 to all coordinates to avoid prime meridian issues
	            Feature second = selectedArr[1];
	            FPath secondFP = second.getPath();
	            secondFP = secondFP.convertTo(FPath.WORLD);
	            double[] secondCoords = secondFP.getCoords(false);
	            double[] secondNewCoords = secondCoords;
	            minMaxCoords[0] = Double.MAX_VALUE;
	            minMaxCoords[1] = Double.MIN_VALUE;
	            getMinMaxCoord(minMaxCoords, secondCoords, false);
	                
	            //now that we have the min and max coordinates for this shape, see if the difference is greater than 180
	            difference = minMaxCoords[1] - minMaxCoords[0];
	            if (Double.compare(difference, 180) > 0) {
	                //it is greater than 180, we need to add 360 to any value less than 180
	                secondNewCoords = adjustArrayForPM(secondCoords, false);
	            }
	            GeneralPath secondNewPath = coordsToGeneneralPath(secondNewCoords, second.getPath().getClosed(), false);
	            //intersect first and second
	            Area secondArea = new Area(secondNewPath);
	            intersectArea.add(firstArea);
	            intersectArea.intersect(secondArea);
	            
	            if (intersectArea.isEmpty()) {
	                Util.showMessageDialog(
	                        "No intersection of selected shapes was returned. No new shapes will be created.",
	                        "Subtract results",
	                        JOptionPane.PLAIN_MESSAGE);
	            } else if (intersectArea.equals(firstArea) || intersectArea.equals(secondArea)) {
	                Util.showMessageDialog(
	                        "It appears that one shape is fully contained within the other shape. JMARS is not currently able to\n "
	                        + "create a shape with an inner polygon subtracted. No new shapes will be created.",
	                        "Subtract results",
	                        JOptionPane.PLAIN_MESSAGE);
	            } else {
	                firstArea.subtract(intersectArea);
	                secondArea.subtract(intersectArea);
	                
	                if (!firstArea.isEmpty()) {
	                    FPath firstResultPath = new FPath (firstArea, FPath.WORLD);
	                    firstResultPath = firstResultPath.convertTo(FPath.SPATIAL_WEST);
	                    int cnt = firstResultPath.getPathCount();
	                    for (int i=0; i<cnt; i++) {
	                        FPath p = new FPath(firstResultPath.getVertices(i),FPath.SPATIAL_WEST, first.getPath().getClosed());
	                        Feature feature = new Feature();
	                        feature.setPath (p);
	                        String typeString = FeatureUtil.getFeatureTypeString(firstResultPath.getType());
	                        feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
	                        addFeature(feature);
	                    }
	                }
	                if (!secondArea.isEmpty()) {
	                    FPath secondResultPath = new FPath(secondArea,FPath.WORLD);
	                    secondResultPath = secondResultPath.convertTo(FPath.SPATIAL_WEST);
	                    int cnt = secondResultPath.getPathCount();
	                    for (int i=0; i<cnt; i++) {
	                        FPath p = new FPath(secondResultPath.getVertices(i),FPath.SPATIAL_WEST,second.getPath().getClosed());
	                        Feature feature = new Feature();
	                        feature.setPath (p);
	                        String typeString = FeatureUtil.getFeatureTypeString(secondResultPath.getType());
	                        feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
	                        addFeature(feature);
	                    }
	                }
	            }
            }
        });
	    
	    mergeMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                Area startingArea = new Area();
                ObservableSet<Feature> selections = shapeLayer.getSelections();
        
                Feature[] selectedArr = selections.toArray(new Feature[]{});
                
                //need to initially go through each selected shape and see if we need to run them all through the adjustment for crossing the PM
                double minCoord = Double.MAX_VALUE;
                double maxCoord = Double.MIN_VALUE;
                double[] minMaxCoords = new double[]{minCoord,maxCoord};
                boolean crossesPM = false;
                for (Feature feature : selectedArr) {
                    if (!(feature.getPath().getType() == FPath.TYPE_POLYGON)) {
                        Util.showMessageDialog(
                                "This function is only available for polygons.",
                                "Intersect results",
                                JOptionPane.PLAIN_MESSAGE);
                        return;
                    }
                    FPath fp = feature.getPath();
                    fp = fp.convertTo(FPath.WORLD);
                    double[] coords = fp.getCoords(false);

                    getMinMaxCoord(minMaxCoords, coords, false);
                    
                    //now that we have the min and max coordinates for this shape, see if the difference is greater than 180
                    double difference = minMaxCoords[1] - minMaxCoords[0];
                    if (Double.compare(difference, 180) > 0) {
                        crossesPM = true;
                        break;
                    }
                }

                for (Feature feature : selectedArr) {
                    //add 360 to all coordinates to avoid prime meridian issues
                    FPath fp = feature.getPath();
                    fp = fp.convertTo(FPath.WORLD);
                    double[] coords = fp.getCoords(false);
                    double[] newCoords = coords;
                    if (crossesPM) {
                        //our ending shape will cross the PM, so we need to adjust all values
                        newCoords = adjustArrayForPM(coords, false);
                    }
                    GeneralPath newPath = coordsToGeneneralPath(newCoords, fp.getClosed(), false);
                    Area area = new Area(newPath);
                    startingArea.add(area);
                }
                
                FPath resultFP = new FPath (startingArea, FPath.WORLD);
                resultFP = resultFP.convertTo(FPath.SPATIAL_WEST);
                if (resultFP.getPathCount() > 0) {//avoid ArrayIndexOutOfBoundsException
                    int cnt = resultFP.getPathCount();
                    for (int i=0; i<cnt; i++) {
                        Feature feature = new Feature();
                        FPath p = new FPath(resultFP.getVertices(i), FPath.SPATIAL_WEST, true);
                        feature.setPath (p);
                        String typeString = FeatureUtil.getFeatureTypeString(resultFP.getType());
                        feature.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
                        addFeature(feature);
                    }
                }
            }
          
        });
	    
	    duplicateMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                ObservableSet<Feature> selections = shapeLayer.getSelections();
                Feature[] selectedArr = selections.toArray(new Feature[]{});
                for (Feature feature : selectedArr) {
                    if (!(feature.getPath().getType() == FPath.TYPE_POLYGON)) {
                        Util.showMessageDialog(
                                "This function is only available for polygons.",
                                "Intersect results",
                                JOptionPane.PLAIN_MESSAGE);
                        return;
                    }
                    FPath fp = feature.getPath();
                    if (fp.getPathCount() > 0) {//avoid ArrayIndexOutOfBoundsException
                        int cnt = fp.getPathCount();
                        for (int i=0; i<cnt; i++) {
                            Feature f = new Feature();
                            FPath p = new FPath(fp.getVertices(i), FPath.SPATIAL_WEST, fp.getClosed());
                            f.setPath (p);
                            String typeString = FeatureUtil.getFeatureTypeString(fp.getType());
                            f.setAttribute( Field.FIELD_FEATURE_TYPE, typeString);
                            addFeature(f);
                        }
                    }
                }
            }
        }); 

	    addBufferMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
            	String bufferSize = displayBufferDialog();
            	
            	if (bufferSize == null) {
            		Util.showMessageDialog("Invalid buffer size entered.");
            		return;
            	}
            	if (bufferSize.equals("cancel")) {
            		//user canceled.
            		return;
            	}
            	
            	
                ObservableSet<Feature> selections = shapeLayer.getSelections();
                Feature[] selectedArr = selections.toArray(new Feature[]{});
                for (Feature feature : selectedArr) {
                	double bufferSizeDouble = Double.parseDouble(bufferSize);;
                	
                	double deg = bufferSizeDouble;
            		deg = (360*bufferSizeDouble)/(2*Math.PI*Util.EQUAT_RADIUS);

                    FPath fp = feature.getPath();
                    
                    //****get center point of feature in west because proj_oc wants it in west*****
                    Point2D featureCenterPt = feature.getPath().getSpatialWest().getCenter();
                    
                    //****** Get new proj with feature x, and y ***********
                    ProjObj proj = new Projection_OC(featureCenterPt.getX(), featureCenterPt.getY());
                    fp = fp.convertToSpecifiedWorld(proj);

                    //Do buffer
                    Geometry geom = new FPath.GeometryAdapter().getGeometry(fp);
                    Geometry buffer = geom.buffer(deg);
                    
                    Path2D.Double path = new FPath.GeometryAdapter().getPath(buffer);                    
                    
                    //******* CONVERT BACK FROM WC TO MAIN PROJECTION ********
                    fp = new FPath(path, FPath.WORLD);
                    fp = fp.convertToSpecifiedWorld(proj, Main.PO);
                    Feature f = new Feature();       

                    f.setPath(fp.convertTo(FPath.SPATIAL_WEST));
                    String typeString = FeatureUtil.getFeatureTypeString(FPath.TYPE_POLYGON);
                    f.setAttribute(Field.FIELD_FEATURE_TYPE, typeString);
                    addFeature(f);
                }
            }
        }); 	    
	    
	    splitPolylineMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                ObservableSet<Feature> selections = shapeLayer.getSelections();        
                Feature[] selectedArr = selections.toArray(new Feature[]{});
                
                //need to initially go through each selected shape and see if we need to run them all through the adjustment for crossing the PM
                double minCoord = Double.MAX_VALUE;
                double maxCoord = Double.MIN_VALUE;
                double[] minMaxCoords = new double[]{minCoord,maxCoord};
                
    			ArrayList<Feature> toAdd = new ArrayList<Feature>();
                
                for (Feature feature : selectedArr) {
                    if (!(feature.getPath().getType() == FPath.TYPE_POLYLINE)) {
                        Util.showMessageDialog(
                                "This function is only available for lines.",
                                "Split line results",
                                JOptionPane.PLAIN_MESSAGE);
                        return;
                    }
                    FPath fp = feature.getPath();
                    fp = fp.convertTo(FPath.WORLD);
                    double[] coords = fp.getCoords(false);

                    for (int i=0; i<coords.length-2; i+=2) {
                    	double[] newCoords = new double[4];
                    	newCoords[0]=coords[i];
                    	newCoords[1]=coords[i+1];
                    	newCoords[2]=coords[i+2];
                    	newCoords[3]=coords[i+3];

                        getMinMaxCoord(minMaxCoords, coords, false);
                        
                        //now that we have the min and max coordinates for this segment, see if the difference is greater than 180
                        double difference = minMaxCoords[1] - minMaxCoords[0];

                        if (difference > 180) {
                            //our ending shape will cross the PM, so we need to adjust all values
                            newCoords = adjustArrayForPM(newCoords, false);                        	
                        }

                        FPath resultFP = new FPath(newCoords, false, FPath.WORLD, false);
                        resultFP = resultFP.convertTo(FPath.SPATIAL_WEST);
                        Feature newFeature = new Feature();
                        newFeature.setPath(resultFP);
                        String typeString = FeatureUtil.getFeatureTypeString(resultFP.getType());
                        newFeature.setAttribute(Field.FIELD_FEATURE_TYPE, typeString);
                        toAdd.add(newFeature);
                    }                    
                }
                
    			if (toAdd.size()>0) {
    				// Make a new history frame.
    				if (shapeLayer.getHistory() != null) {
    					shapeLayer.getHistory().mark();
    				}
    				shapeLayer.getFeatureCollection().addFeatures(toAdd);
    			}
            }
          
        });
	    
        pixelExportMenuItem.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                ObservableSet<Feature> selections = shapeLayer.getSelections();
                
                //create an arraylist containing paths of all selections
                ArrayList<FPath> paths = new ArrayList<FPath>();
                for(Feature f : selections){
                	//get the path this way, so that there is a proper shape for circles
                	FPath fpath = shapeLayer.getStylesLive().geometry.getValue(f).getWorld();
                	paths.add(fpath);
                }
                
        		new PixelExportDialog(Main.mainFrame, shapeFunctionsMenu, paths).setVisible(true);
            }
        });
        
        //add items to the shape functions menu
        shapeFunctionsMenu.add(intersectMenuItem);
        shapeFunctionsMenu.add(subtractMenuItem);
        shapeFunctionsMenu.add(mergeMenuItem);
        shapeFunctionsMenu.add(duplicateMenuItem);
        shapeFunctionsMenu.add(pixelExportMenuItem);
        shapeFunctionsMenu.add(convertCirclesToPolygonItem);
//        shapeFunctionsMenu.add(convertCirclesToEllipsesItem);
        shapeFunctionsMenu.add(addBufferMenuItem);
        shapeFunctionsMenu.add(makeLotsOfSubShapesItem);
        shapeFunctionsMenu.add(splitPolylineMenuItem);
	    
	} // end: private void setupContextMenu()
	
	//maintaining the state of the GeomSource object is tricky, so let's do it all in one complicated place instead of 3 complicated places
	//@return success flag
	private boolean setGeomSource(int mode) {//should be circle, ellipse, 5pt ellipse mode or convert circle to ellipse
		StyleSource<FPath> source = shapeLayer.getStylesLive().geometry.getSource();
		
		//This first section is to short circuit and return if nothing needs to be done to the GeomSource
		GeomSource geomSource = null;
		if (source instanceof GeomSource) {
			geomSource = (GeomSource) source;
			//we have a GeomSource, verify it
			if (mode == ADD_CIRCLE_MODE) {
				if (geomSource.getRadiusField() != null && geomSource.getUnits() != null) {
					//done here
					return true;
				}
			} else if (mode == ADD_FIVE_PT_ELLIPSE_MODE) {
				if (geomSource.getAAxisField() != null && geomSource.getBAxisField() != null && geomSource.getLatField() != null
						&& geomSource.getLonField() != null && geomSource.getAngleField() != null && geomSource.getAngleUnits() != null
						&& geomSource.getAxesUnits() != null) {
					//done here
					return true;
				}
			} else if (mode == ADD_ELLIPSE_MODE) {
				if (geomSource.getAAxisField() != null && geomSource.getBAxisField() != null && geomSource.getLatField() != null
						&& geomSource.getLonField() != null && geomSource.getAngleField() != null && geomSource.getAngleUnits() != null
						&& geomSource.getAxesUnits() != null && geomSource.getMeanAxisField() != null) {
					//done here
					return true;
				}
			} else if (mode == CONVERT_CIRCLE_TO_ELLIPSE_MODE) {
				if (geomSource.getAAxisField() != null && geomSource.getBAxisField() != null && geomSource.getLatField() != null
						&& geomSource.getLonField() != null && geomSource.getAngleField() != null && geomSource.getAngleUnits() != null
						&& geomSource.getAxesUnits() != null && geomSource.getMeanAxisField() != null
						&& geomSource.getRadiusField() != null && geomSource.getUnits() != null) {
					//done here
					return true;
				}
			}
		}
		//end short circuit if GeomSource is already good
		
		//if we got here, we either do not have a GeomSource or need to adjust it
		
		//things we will need to populate the geomSource
		Field aField = new Field("A Axis", Double.class);
		Field bField = new Field("B Axis", Double.class);
		Field angField = new Field("Rotation Angle", Double.class);
		Field lonField = new Field("Longitude", Double.class);
		Field latField = new Field("Latitude", Double.class);
		Field mField = new Field("Mean Axis Value", Double.class);
		Field radField = new Field("Radius", Double.class);
		LengthUnits defaultLengthUnits = LengthUnits.AxesKm;
		String units = Config.get("shape.ellipse.axesUnits", "");
		if (!"".equals(units)) {
			defaultLengthUnits = LengthUnits.getEntryByConfigName(units);
		}
		
		//get all the fields in the schema that are numeric
		List<Field> fields = new ArrayList<Field>();
		for (Field f: shapeLayer.getFeatureCollection().getSchema()) {
			if (Integer.class.isAssignableFrom(f.type) ||
					Float.class.isAssignableFrom(f.type) ||
					Double.class.isAssignableFrom(f.type)) {
				if (!Styles.numericDefaultFields.contains(f.name.trim().toLowerCase())) {
					fields.add(f);
				}
			}
		}
		
		//check for one more short circuit option if this is basically a new shape layer
		if (fields.size() == 0 && geomSource == null) {
			//new shape layer, no relevant fields and no GeomSource, just create a new GeomSource that fits all and do not pop a dialog
			geomSource = new GeomSource(radField, Units.RadiusKm, aField, bField, angField,
					defaultLengthUnits, AngleUnits.Degrees, latField, lonField, mField);
			Style<FPath> geom = new Style<FPath>(shapeLayer.getStylesLive().geometry.getName(),geomSource);
			Set<Style<?>> styles = new HashSet<Style<?>>(1);
			styles.add(geom);
			shapeLayer.applyStyleChanges(styles);
			return true;
		}
		//end short circuit
		
		//start circle
		//if we get here, there are fields that are relevant. We need to analyze and possibly pop a dialog 
		if (mode == FeatureMouseHandler.ADD_CIRCLE_MODE) {
			boolean found = false;
			for (Field f: fields) {//loop through all the numeric feels and look for a radius field
				if (f.name.equalsIgnoreCase("radius")) {
					radField = f;//use the one we found
					found = true;
					break;
				}
			}
			if (found) {
				if (fields.size() == 1) {
					//this was the only relevant field
					if (geomSource == null) {
						//geomSource is null, just add one that is good for all
						geomSource = new GeomSource(radField, Units.RadiusKm, aField, bField, angField,
								defaultLengthUnits, AngleUnits.Degrees, latField, lonField, mField);
						Style<FPath> geom = new Style<FPath>(shapeLayer.getStylesLive().geometry.getName(),geomSource);
						Set<Style<?>> styles = new HashSet<Style<?>>(1);
						styles.add(geom);
						shapeLayer.applyStyleChanges(styles);
					} else {
						//this is the only field, and we have a geomSource, add the field
						geomSource.setRadiusField(radField);
						if (geomSource.getUnits() == null) {
							geomSource.setUnits(Units.RadiusKm);//set units to default km if not set
						}
					}
					return true;
				}
			}
			
			fields.add(radField);//add in our default field as a selection option
			//if we got here, we need to pop a dialog
			Object value = Util.showInputDialog(
					"Choose the name of a column to hold radius in kilometers\n" +
						"(For more options, cancel and use 'Edit Circles' in the Feature menu)",
					"Circle Configuration",
					JOptionPane.OK_CANCEL_OPTION,
					null,
					fields.toArray(),
					fields.get(0));
			
			if (value != null && value instanceof Field) {
				radField = (Field) value;
				if (geomSource == null) {
					//create a geomSource with the selection and also set the rest of the default fields
					geomSource = new GeomSource(radField, Units.RadiusKm, aField, bField, angField,
							defaultLengthUnits, AngleUnits.Degrees, latField, lonField, mField);
					Style<FPath> geom = new Style<FPath>(shapeLayer.getStylesLive().geometry.getName(),geomSource);
					Set<Style<?>> styles = new HashSet<Style<?>>(1);
					styles.add(geom);
					shapeLayer.applyStyleChanges(styles);
				} else {
					geomSource.setRadiusField(radField);
					geomSource.setUnits(Units.RadiusKm);//set units to default km if not set
				}
				return true;
			} else {
				return false;
			}
			
		}//end circle
		
		//because of short circuiting above, we would only get here if we are drawing ellipses or converting circles and have multiple fields 
		//that can be relevant. We may or may not have a GeomSource
//		if (geomSource == null) {
			StyleSource<FPath> newSource = GeomSource.editEllipseSource(Main.mainFrame,shapeLayer,geomSource, getProj());
			if (newSource != null && newSource != geomSource) {
				geomSource = (GeomSource) newSource;
				geomStyle.setSource(geomSource);
				Set<Style<?>> changes = new HashSet<Style<?>>();
				changes.add(geomStyle);
				shapeLayer.applyStyleChanges(changes);
			}
			if (mode == CONVERT_CIRCLE_TO_ELLIPSE_MODE) {
				if (geomSource.getRadiusField() == null) {
					geomSource.setRadiusField(new Field("Radius", Double.class));
					geomSource.setUnits(Units.RadiusKm);
				}
			}
			if (geomSource != null) {
				return true;
			} else {
				return false;
			}
	}
	private String displayBufferDialog() {
		String size = Util.showInputDialog("Enter Buffer size (KM): ", "");
		if (size == null) {
			return "cancel";
		} else {
			if (validateBufferSize(size)) {
				return size;
			} else {
				return null;
			}
		}
    }

	private boolean validateBufferSize(String size) {
		boolean returnVal = true;
		size = size.trim();
		if (size.length() == 0) {
			returnVal = false;
		} else {
			String check = "0123456789.";
			for (int a=0;a<size.length();a++) {
				if (check.indexOf(String.valueOf(size.charAt(a))) < 0) {
					returnVal = false;
				}
			}
		}
		return returnVal;
	}
	private void createDrawShapesMenuItems() {
		addStreamModeRadioButton = new JRadioButtonMenuItem("stream",  DrawingPalette.ifreehand);
		addPointsOnlyRadioButton       = new JRadioButtonMenuItem( "point", DrawingPalette.ipoint);
		addLinesOnlyRadioButton       = new JRadioButtonMenuItem( "line",  DrawingPalette.iline);
		addPolygonsOnlyRadioButton       = new JRadioButtonMenuItem( "polygon",  DrawingPalette.ipoly);
		addCircleRadioButton     = new JRadioButtonMenuItem( "circle",  DrawingPalette.icircle);
		add5PtEllipseRadioButton = new JRadioButtonMenuItem("5-point ellipse",  DrawingPalette.i5ptellipse);
		addDrawRectangleRadioButton = new JRadioButtonMenuItem("rectangle",  DrawingPalette.irectangle);
		addEllipseRadioButton = new JRadioButtonMenuItem("ellipse", DrawingPalette.iellipse);
		selectModeRadioButton    = new JRadioButtonMenuItem( "Select Features");
	}
	
	private void groupDrawShapesMenuItems() {
		toolSelectButtonGroup.add(addStreamModeRadioButton);
		toolSelectButtonGroup.add(addPointsOnlyRadioButton);
		toolSelectButtonGroup.add(addLinesOnlyRadioButton);
		toolSelectButtonGroup.add(addPolygonsOnlyRadioButton);
		toolSelectButtonGroup.add(addCircleRadioButton);
		toolSelectButtonGroup.add(add5PtEllipseRadioButton);
//		toolSelectButtonGroup.add(addEllipseRadioButton);
		toolSelectButtonGroup.add(addDrawRectangleRadioButton);
	}
	

	private void createWrapperMenuForDrawShapes() {
		zmenu = new JMenu(DRAW_TXT);
		addDrawSubMenu();
		resetDrawShapes();
	}
	

	private void addDrawSubMenu() {
		zmenu.add(addPointsOnlyRadioButton);
		zmenu.add(addLinesOnlyRadioButton);
		zmenu.add(addPolygonsOnlyRadioButton);
		zmenu.add(addStreamModeRadioButton);
		zmenu.add(addCircleRadioButton);
		zmenu.add(add5PtEllipseRadioButton);
		zmenu.add(addDrawRectangleRadioButton);
//		zmenu.add(addEllipseRadioButton);
	}

	
	protected void informObserverForAction(JRadioButtonMenuItem action) {
		if (!(isCustomShapeInstance(lview))) return;
		Object data = getActionFor(action);
		ShapeActionData shapeactiondata = new ShapeActionData(lview, data);
		observable.changeData(shapeactiondata);		
	}
	
	private boolean isCustomShapeInstance(LView view) {
		boolean isInstance = false;
		if (view != null) {
			if (view instanceof ShapeLView && (view.getName().toLowerCase().contains("shape"))) {
				isInstance = true;
			}
		}
		return isInstance;
	}	

	private Object getActionFor(JRadioButtonMenuItem value) {
		 for (Entry<DrawActionEnum, JRadioButtonMenuItem > entry : drawActions.entrySet()) {
		        if (entry.getValue().equals(value)) {
		            return entry.getKey();
		        }
		    }
		return null;
	}

	private void setupDrawingPaletteActions() {
		drawActions.put(DrawActionEnum.RECTANGLE, addDrawRectangleRadioButton);
		drawActions.put(DrawActionEnum.CIRCLE, addCircleRadioButton);
		drawActions.put(DrawActionEnum.ELLIPSE5, add5PtEllipseRadioButton);
		drawActions.put(DrawActionEnum.ELLIPSE, addEllipseRadioButton);
		drawActions.put(DrawActionEnum.POINT, addPointsOnlyRadioButton);
		drawActions.put(DrawActionEnum.LINE, addLinesOnlyRadioButton);
		drawActions.put(DrawActionEnum.POLYGON, addPolygonsOnlyRadioButton);
		drawActions.put(DrawActionEnum.FREEHAND, addStreamModeRadioButton);
		drawActions.put(DrawActionEnum.SELECT, selectModeRadioButton);		
	}	

	@Override
	public void update(Observable o, Object arg) {  //called from drawing palette
		if (arg == null) return;
		if (!(o instanceof PaletteObservable)) return;
		if (!(arg instanceof DrawActionEnum)) return;
		JRadioButtonMenuItem selectedAction = drawActions.get(arg);
		if (selectedAction != null) {
			selectedAction.setSelected(false);
		    selectedAction.doClick();
		}
	}
	
	public  static class ShapeActionObservable extends Observable { // right-click menu observable
	
		ShapeActionObservable() {
			super();
		}

		void changeData(Object data) {
			setChanged();
			Observer observer = DrawingPalette.INSTANCE;
			if (observer != null) {
				addObserver(observer);
				notifyObservers(data);
			}
		}
	}
	
	public class ShapeActionData {
		private LView currentViewInstance;
		private Object selectedDrawAction;

		public ShapeActionData(LView currentViewInstance, Object selectedDrawAction) {
			this.currentViewInstance = currentViewInstance;
			this.selectedDrawAction = selectedDrawAction;
		}

		public LView getViewForThisAction() {
			return currentViewInstance;
		}

		public Object getSelectedDrawAction() {
			return selectedDrawAction;
		}
	}
	
	private void enableDrawShapes() {
		Enumeration<AbstractButton> enumeration = toolSelectButtonGroup.getElements();
		while (enumeration.hasMoreElements()) {
			enumeration.nextElement().setEnabled(true);
		}
	}

	private void resetDrawShapes() {
		Enumeration<AbstractButton> enumeration = toolSelectButtonGroup.getElements();
		while (enumeration.hasMoreElements()) {
			AbstractButton element = enumeration.nextElement();
		    element.setSelected(false);    //always enabled
		}
		toolSelectButtonGroup.clearSelection();
	}	
	
} // end: class FeatureMouseHandler
