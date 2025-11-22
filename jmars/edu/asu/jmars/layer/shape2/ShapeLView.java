package edu.asu.jmars.layer.shape2;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.PROFILE_PENCIL;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import edu.asu.jmars.Main;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.InvestigateData;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.LManager.ActiveViewChangedObservable;
import edu.asu.jmars.layer.shape2.ShapeLayer.LEDStateProcessing;
import edu.asu.jmars.layer.shape2.ShapeLayer.LoadData;
import edu.asu.jmars.layer.shape2.ShapeLayer.LoadListener;
import edu.asu.jmars.layer.shape2.drawingpalette.DrawingPalette;
import edu.asu.jmars.layer.shape2.drawingpalette.UserPrompt;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.ViewChangedObserver;
import edu.asu.jmars.layer.shape2.xb.data.service.Data;
import edu.asu.jmars.layer.util.features.CalculatedField;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureEvent;
import edu.asu.jmars.layer.util.features.FeatureListener;
import edu.asu.jmars.layer.util.features.FeatureMouseHandler;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.FeatureProviderReadOnly;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.MemoryFeatureIndex;
import edu.asu.jmars.layer.util.features.ShapeRenderer;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.layer.util.features.Style;
import edu.asu.jmars.layer.util.features.StyleSource;
import edu.asu.jmars.layer.util.features.WorldCacheSource;
import edu.asu.jmars.layer.util.filetable.FileTableModel;
import edu.asu.jmars.swing.LikeLabelButtonUI;
import edu.asu.jmars.tool.strategy.ShapesToolStrategy;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.ObservableSetListener;
import edu.asu.jmars.util.SerializingThread;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.CustomBalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.java.balloontip.utils.TimingUtils;

public class ShapeLView extends LView implements FeatureListener, ObservableSetListener<Feature>, ViewChangedObserver {
    private static DebugLog log = DebugLog.instance();
    static final int DRAWING_BUFFER_INDEX = 0;
    static final int SELECTION_BUFFER_INDEX = 1;
    
    private ShapeLayer shapeLayer;
    private ShapeFocusPanel focusPanel;
    private FeatureMouseHandler featureMouseHandler;
    
    private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getCommonFill();
	private static Image profile_c = ImageFactory.createImage(PROFILE_PENCIL.withDisplayColor(imgColor));	
	Dimension bestSize = Toolkit.getDefaultToolkit().getBestCursorSize(profile_c.getWidth(null), profile_c.getHeight(null));	
	
	//private Map<UserPrompt, Integer> userPrompt = new HashMap<>();
	private static CustomBalloonTip myBalloonTip;
	private static Color imgBlack = Color.BLACK;
	private static Icon close = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgBlack)));
	private static JButton closebutton;
    //used to populate info tabs
    public String layerName = "";
    
   
	/**
	 * Time-stamps for two different types of requests/unit-of-works.
	 * Index 0 is for all-Feature draw requests and index 1 is for
	 * selected-Feature redraw requests only. These time-stamps are
	 * used to figure out if a previously enqueued drawing request has
	 * been superseded by a newer request or not.
	 * 
	 * @see #draw(boolean)
	 * @see #drawThread
	 * @see DrawingUow
	 */
	volatile private long[] drawReqTS = new long[] {0l, 0l};
	
	/**
	 * Drawing worker thread. Requests processed by this thread in a 
	 * serial fashion, one by one.
	 */
	SerializingThread drawThread;
	
	private transient boolean isReadOnly = false;
	
	static {
		createCalloutUI();
	}
	
	public ShapeLView(ShapeLayer layerParent, boolean isMain, boolean isReadOnly) {
		this(layerParent, isMain, isReadOnly, null);
	}

	public ShapeLView(ShapeLayer layerParent, boolean isMain, boolean isReadOnly, LayerParameters lp){
		this(layerParent, isMain, isReadOnly, null, null);
	}
	
	public ShapeLView(ShapeLayer layerParent, boolean isMain, boolean isReadOnly, LayerParameters lp, ShapeLView3D lview3d) {
		super(layerParent, lview3d);
		if(lp!=null)
			this.layerName = lp.name;
		else
			this.layerName = "Custom Shape Layer";
		this.layerParams = lp;
		shapeLayer = layerParent;
		if(isMain)
			super.focusPanel = this.focusPanel = new ShapeFocusPanel(layerParent, this);
		
		// Keep two buffers, one for normal drawing, other for selection drawing.
		setBufferCount(2);
		
		shapeLayer.getFeatureCollection().addListener(this);
		shapeLayer.selections.addListener(this);
		
		drawThread = new SerializingThread("ShapeLViewDrawThread");
		drawThread.start();
		
		// set name on lmanager
		if(layerName!=""){
			setName(layerName);
		} 
				
		// Set up the handlers of the mouse.
		int flags = 0;
		
		this.isReadOnly = isReadOnly;
		
		if(isReadOnly){
		// Sets appropriate flags for readOnly shape layers
			flags = FeatureMouseHandler.ALLOW_ZORDER;
		}
		if(!isReadOnly){
		// Sets appropriate flags for custom shape layers	
			flags =
		   FeatureMouseHandler.ALLOW_ADDING_POINTS     |
		   FeatureMouseHandler.ALLOW_ADDING_LINES      |
		   FeatureMouseHandler.ALLOW_ADDING_POLYS      |
		   FeatureMouseHandler.ALLOW_MOVING_FEATURES   |
		   FeatureMouseHandler.ALLOW_DELETE_FEATURES   |

		   FeatureMouseHandler.ALLOW_MOVING_VERTEX     |
		   FeatureMouseHandler.ALLOW_ADDING_VERTEX     |
		   FeatureMouseHandler.ALLOW_DELETING_VERTEX   |

		   FeatureMouseHandler.ALLOW_ZORDER            |
		   FeatureMouseHandler.ALLOW_CHANGE_MODE;
		}
		
		// only set up mouse and key listeners on the main view, which is the
		// only view that will be given a non-null focus panel
		if (focusPanel != null) {
			featureMouseHandler = new FeatureMouseHandler(shapeLayer, this, flags);
			addMouseListener(featureMouseHandler);
			addMouseMotionListener(featureMouseHandler);
			addMouseWheelListener(featureMouseHandler);
			
			// set up the key listener for deleting vertices and features from the
			// view.
			if (!isReadOnly){
				addKeyListener( new KeyAdapter() {
					public void keyPressed(KeyEvent e) {
						int key = e.getKeyCode();
						int mode = featureMouseHandler.getMode();
						if (key == KeyEvent.VK_ESCAPE && (mode == FeatureMouseHandler.ADD_FEATURE_MODE || mode == FeatureMouseHandler.ADD_FIVE_PT_ELLIPSE_MODE)) {
							// delete the last vertex defined.
							featureMouseHandler.deleteLastVertex();
						} else if (key == KeyEvent.VK_DELETE && mode == FeatureMouseHandler.SELECT_FEATURE_MODE) {
							shapeLayer.getHistory().mark();
							// delete selected features
							shapeLayer.getFeatureCollection().removeFeatures(shapeLayer.selections);
						} else if (key == KeyEvent.VK_ESCAPE && featureMouseHandler.isAddEditEllipse()) {
							featureMouseHandler.cancelAddEditEllipseByKeystoke();
						} else if (key == KeyEvent.VK_ENTER && featureMouseHandler.isAddEditEllipse()) {
							featureMouseHandler.finishAddEditEllipseByKeystoke();
						} else if (featureMouseHandler.isAddEditEllipse() && (key == KeyEvent.VK_DOWN || key == KeyEvent.VK_LEFT || 
								key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_UP || key == KeyEvent.VK_Z || key == KeyEvent.VK_X ||
								key == KeyEvent.VK_K || key == KeyEvent.VK_J || key == KeyEvent.VK_M || key == KeyEvent.VK_N)) {
							featureMouseHandler.arrowAddEditEllipseByKeystoke(key);//up and down only work on edit as major axis is determined by the mouse on draw
							e.consume();
						}
						else if (KeyStroke.getKeyStroke(e.getKeyCode(), e.getModifiers()) == KeyStroke.getKeyStroke(KeyEvent.VK_Z,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask())) {
							ShapeLayer.LEDState led = null;
							
							try {
								shapeLayer.begin(led = new ShapeLayer.LEDStateProcessing());
								shapeLayer.getHistory().undo();
							}
							finally {
								shapeLayer.end(led);
							}
						}
					}
				});
			}
		}
		LManager.getLManager().getActiveViewObservable().addObserver(this);
	}

	
	private static void createCalloutUI() {
		JLabel dummy = new JLabel();
		EdgedBalloonStyle style = new EdgedBalloonStyle(ThemeSnackBar.getBackgroundStandard(), 
	                ThemeProvider.getInstance().getBackground().getBorder());
		 BalloonTip.setDefaultCloseButtonIcons(close, close, close);
		 myBalloonTip = new CustomBalloonTip(dummy,
				  dummy,
				  new Rectangle(),
				  style,
				  BalloonTip.Orientation.LEFT_BELOW, BalloonTip.AttachLocation.CENTER,
				  10, 20,
				  true);	
		 myBalloonTip.setPadding(5);
		 closebutton = BalloonTip.getDefaultCloseButton();
		 closebutton.setUI(new LikeLabelButtonUI());		 
		 myBalloonTip.setCloseButton(closebutton,false);		
		 myBalloonTip.setVisible(false);
	}

	
	private void showCallout(Container parent2, String msg, int time) {	
		if (myBalloonTip != null && parent2 != null) {
			if (parent2 instanceof JComponent) {
				JComponent comp = (JComponent) parent2;
				if (comp.getRootPane() == null) {
					return;
				}
				myBalloonTip.setAttachedComponent(comp);
				int xoffset = parent2.getWidth() / 2;
				int yoffset = parent2.getHeight();
				Rectangle rectoffset = new Rectangle(xoffset, yoffset, 1, 1);
				Color foregroundtext = ThemeSnackBar.getForegroundStandard();
				String colorhex = edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex(foregroundtext);
				String html = "<html>" + "<p style=\"color:" + colorhex + "; padding:1em; text-align:center;\">" + "<b>"
						+ msg  + "</b>" + "</p></html>";
				myBalloonTip.setTextContents(html);
				myBalloonTip.setOffset(rectoffset);
				TimingUtils.showTimedBalloon(myBalloonTip, time);
			}
		}
	}
	
	public void hideCallout() {
		if (myBalloonTip != null) {
			myBalloonTip.setVisible(false);
		}
	}

	public boolean isCalloutVisible() {
		return (myBalloonTip != null && myBalloonTip.isVisible());
	}
	
	/**
	 * When it comes time to repaint the view, all we need to do is redraw the selection line.
	 */
	public void paintComponent(Graphics g){		
		super.paintComponent(g);
		if (getChild() != null && viewman != null) {//@since remove viewman2
			Graphics2D g2 = (Graphics2D)g;
			g2.transform(getProj().getWorldToScreen());
			g2 = viewman.wrapWorldGraphics(g2);

			featureMouseHandler.drawSelectionLine(g2);
			featureMouseHandler.drawSelectionRectangle( g2);
			featureMouseHandler.drawSelectionGhost( g2);
			featureMouseHandler.drawVertexBoundingLines( g2);
			featureMouseHandler.drawEllipseGhost(g2);
			featureMouseHandler.drawRectangleFeature(g2);
		}
	}
    
	public String getName() {
		if (layerName!=null && layerName.length()>0){
			return layerName;
		}
		if (shapeLayer == null)
			return getClass().getSimpleName();
		return shapeLayer.name;
	}
	
	public void setName(String newName) {
		shapeLayer.name = newName;
		if (LManager.getLManager() != null)
			LManager.getLManager().updateLabels();
	}

	protected Object createRequest(Rectangle2D where) {
		draw(false); // redraw all data
		draw(true);  // redraw selections
		return null;
	}
	
	/*
	 * (non-Javadoc)
	 * @see edu.asu.jmars.layer.Layer.LView#getContextMenu(java.awt.geom.Point2D)
	 * Adds ShapeLayer functionality to the main and the panner view.
	 */
	protected Component[] getContextMenu(Point2D worldPt){
		if (viewman.getActiveLView().equals(this) ||
			    viewman.getActiveLView().equals(getChild()) ) {
		    return featureMouseHandler.getMenuItems( worldPt);
		}
		else {
		    return new Component[0];
		}
	}
	
	public void receiveData(Object layerData){
		if (layerData instanceof ShapeLayer.StylesChange) {
			stylesChanged(((ShapeLayer.StylesChange)layerData).changes);
		}
	}
	
	protected LView _new() {
		return new ShapeLView(shapeLayer, false, false, layerParams);
	}
	
	public ShapeFocusPanel getFocusPanel(){
		return focusPanel;
	}
	
	public String getToolTipText(MouseEvent e) {
		// get fields that are enabled for tooltips and present in the schema of
		// any selected file
		Set<Field> fields = new TreeSet<Field>(ShapeLayer.fieldByName);
		fields.addAll(shapeLayer.tooltipFields);
		fields.retainAll(shapeLayer.fileTable.getSelectedFileFields());
		if (!fields.isEmpty()) {
			try {
				Point2D mouseWorld = getProj().screen.toWorld(new WrappedMouseEvent(e).getRealPoint());
				Rectangle2D rect = getProj().getClickBox(mouseWorld, 5);
				String text = getStringForRect(fields, rect, true);
				if (text != null) {
					return "<html>" + Util.foldText(text, 100, "<br>") + "</html>";
				}
			} catch ( Exception ex) {
				System.out.println("tooltip exception: " + ex);
			}
		}
		return super.getToolTipText();
	}
// Returns an investigate data object with all the selected tooltips for
// each shape under the cursor.	
	public InvestigateData getInvestigateData(MouseEvent event){
		InvestigateData invData = new InvestigateData(getName());
		
		Set<Field> fields = new TreeSet<Field>(ShapeLayer.fieldByName);
		fields.addAll(shapeLayer.tooltipFields);
		fields.retainAll(shapeLayer.fileTable.getSelectedFileFields());
		if(!fields.isEmpty()){
			try{
				Point2D mouseWorld = getProj().screen.toWorld(new WrappedMouseEvent(event).getRealPoint());
				Rectangle2D rect = getProj().getClickBox(mouseWorld, 5);
				String text = getStringForRect(fields, rect, false);
				
				if (text!=null)
					invData.add("", text);
			} catch(Exception e){
				e.printStackTrace();
			}
		}
			
		
		return invData;
	}
	
	/**
	 * @return The feature tooltip for a selected feature, or if no selected
	 * feature is hit, for any feature in the index.
	 */
	private String getStringForRect(Collection<Field> tooltips, Rectangle2D rect, boolean showAsHTML) {
		MemoryFeatureIndex idx = shapeLayer.getIndex();
		for (Feature feature: shapeLayer.getSelections()) {
			if (idx.getWorldPath(feature).intersects(rect)) {
				return getFeatureToolTip(tooltips, feature, showAsHTML);
			}
		}
		Iterator<Feature> fIt = shapeLayer.getIndex().queryUnwrappedWorld(rect);
		String text="";
		while (fIt.hasNext()) {
			text+= getFeatureToolTip(tooltips, fIt.next(), showAsHTML);
			text+="\n   ";
		}
		if (text.length()>5){
			text = text.substring(0, text.length()-4);
			return text;
		}
		else
			return null;
	}
	
	private String getFeatureToolTip(Collection<Field> tooltips, Feature feature, boolean showAsHTML) {
		StringBuffer strBuf = new StringBuffer();
	    for (Field field: tooltips) {
	    	if(showAsHTML)
	    		strBuf.append("<b>");
        	strBuf.append(field.name+": ");
        	if(showAsHTML)
        		strBuf.append("</b> ");
        	Object val = feature.getAttribute(field);
        	String valStr = val == null ? "null" : val.toString();
       		strBuf.append(valStr);
       		if(!showAsHTML)
       			strBuf.append(", ");
       		if(showAsHTML)
       			strBuf.append("<br>");
	    }
	    if(!showAsHTML)
	    	strBuf.delete(strBuf.length()-2, strBuf.length()-1);
		return strBuf.toString();
	}
	
	public boolean hasUnsavedData() {
		boolean saved = true;
		
		FileTableModel ftm = shapeLayer.fileTable.getFileTableModel();
		for (FeatureCollection fc: ftm.getAll()) {
			if (ftm.getTouched(fc)) {
				saved = false;
				break;
			}
		}
		
		return !saved;
	}
	
	/**
	 * Realizes the FeatureListener interface.
	 */
	public void receive(FeatureEvent e) {
		switch(e.type){
		case FeatureEvent.ADD_FIELD:
		case FeatureEvent.REMOVE_FIELD:
			// do nothing - does not pertain to drawing
			break;
		case FeatureEvent.ADD_FEATURE:
		case FeatureEvent.REMOVE_FEATURE:
			// always redraw the data in the drawing buffer
			draw(false);
			if (!Collections.disjoint(shapeLayer.selections, e.features)) {
				draw(true);
			}
			
			//if this is the mainview,
			//increase state id and call redraw on 3d
			if(focusPanel != null){
				shapeLayer.increaseStateId(ShapeLayer.IMAGES_BUFFER);
				shapeLayer.increaseStateId(ShapeLayer.LABELS_BUFFER);
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(this, true);
				}
			}
			break;
		case FeatureEvent.CHANGE_FEATURE:
			// draw buffers that have used the affected field in the
			// last/current paint operation
			stylesChanged(shapeLayer.getStylesFromFields(e.fields));
			
			//if this is the mainview,
			//increase state id and call redraw on 3d
			if(focusPanel != null){
				shapeLayer.increaseStateId(ShapeLayer.IMAGES_BUFFER);
				shapeLayer.increaseStateId(ShapeLayer.LABELS_BUFFER);
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(this, true);
				}
			}
			break;
		default:
			log.aprintln("Unhandled FeatureEvent encountered: "+e);
			break;
		}
	}
	
	private void stylesChanged(Set<Style<?>> changed) {
		// the main view will reset the index when there
		// is a geometry style change
		if (getChild() != null && changed.contains(
				shapeLayer.getStylesLive().geometry)) {
			// TODO: this is a poor way to signal that
			// the indexed shapes have been invalidated
			// by a style change.
			shapeLayer.getIndex().reindex();
		}
		if (!Collections.disjoint(changed, normStyles)) {
			draw(false);
			//if this is the mainview,
			//increase state id and call redraw on 3d
			if(focusPanel!=null){
				shapeLayer.increaseStateId(ShapeLayer.IMAGES_BUFFER);
				shapeLayer.increaseStateId(ShapeLayer.LABELS_BUFFER);
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(this, true);
				}
			}
		}
		if (!Collections.disjoint(changed, selStyles)) {
			draw(true);
			//if this is the mainview,
			//increase state id and call redraw on 3d
			if(focusPanel!=null){
				shapeLayer.increaseStateId(ShapeLayer.IMAGES_BUFFER);
				shapeLayer.increaseStateId(ShapeLayer.LABELS_BUFFER);
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(this, true);
				}
			}
		}
	}
	
	/**
	 * This is an overloading of the updateSettings() method in the superclass.
	 * Either saves the settings to the settings file or loads the settings out
	 * of the settings file.
	 */
	protected void updateSettings(boolean saving) {
		if(this.getChild()!=null){
			if (saving) {
				// save settings into hashtable
				List<Map<String,Object>> layers = new ArrayList<Map<String,Object>>();
				List<FeatureCollection> fcList = shapeLayer.fileTable.getFileTableModel().getAll();
				for (FeatureCollection fc: fcList) {
					Map<String,Object> layer = new HashMap<String,Object>();
					layer.put("filename", fc.getFilename());
					layer.put("id", fcList.indexOf(fc));
					layer.put("calcFields", shapeLayer.calcFieldMap.get(fc).getCalculatedFields());
					Field[] schema = ((List<Field>)fc.getSchema()).toArray(new Field[0]);
					layer.put("schema", schema);
					//Reloading of shapes, specifically depends on if the user created
					// this layer themselves or not.  If it is loaded from the AddLayer,
					// and is not a custom shape layer, there is no point in writing all
					// the shapes out, since they live somewhere already. Just provide 
					// filename and directory that has already been written to the user's
					// jmars directory containing the shape info. Else, write out all the 
					// shapes in this file, because they don't exist elsewhere.
					// When converting from a stamp layer, the layerParameters name is
					// properly set to the Custom Shape Name so this conditional catches
					// that case as well.
					// The check for feature provier to not be null is still needed because
					// some shape layers from the AddLayer are loaded from saved layers, 
					// and that means the shape information is not written to the user's
					// jmars directory.
					FeatureProvider fp = fc.getProvider();
					if (fp != null && layerParams != null && layerParams.name != null && !layerParams.name.equals(ShapeLayer.CUSTOM_SHAPE_NAME)) {
						// will reload this file from outside the session file
						layer.put("providerClass", fp.getClass());
						layer.put("providerFile", fc.getFilename());
						if (fp instanceof FeatureProviderReadOnly) {
							//this is a read only, we will have other attributes to store
							FeatureProviderReadOnly fpRO = (FeatureProviderReadOnly) fp;
							layer.put("providerUrl", fpRO.getUrlString());
							layer.put("providerFilename", fpRO.getFile());
							layer.put("providerDirectory", fpRO.getDirectory());
						}
					}
					// will reload this file from within the session file
					else{
						Object[][] values = new Object[fc.getFeatureCount()][];
						for (int i = 0; i < values.length; i++) {
							Feature f = fc.getFeature(i);
							values[i] = new Object[schema.length];
							for (int j = 0; j < schema.length; j++) {
								Object value = f.getAttribute(schema[j]);
								if (value instanceof FPath) {
									value = new ShapePath((FPath)value);
								}
								values[i][j] = value;
							}
						}
						layer.put("values", values);
					}
					layers.add(layer);
				}
				viewSettings.put("layerName", getName());
				viewSettings.put("showProgress", shapeLayer.showProgress);
				viewSettings.put("layers", layers);
				viewSettings.put("styles", shapeLayer.getStyles().getStyles());
				viewSettings.put("defaultID", fcList.indexOf(shapeLayer.fileTable.getFileTableModel().getDefaultFeatureCollection()));
				viewSettings.put("layerName", layerName); 
				List<FeatureCollection> selections = shapeLayer.fileTable.getSelectedFeatureCollections();
				int[] selOrder = new int[selections.size()];
				for (int i = 0; i < selOrder.length; i++) {
					selOrder[i] = fcList.indexOf(selections.get(i));
				}
				viewSettings.put("selections", selOrder);
				viewSettings.put("tooltips", new ArrayList<Field>(shapeLayer.tooltipFields));
				viewSettings.put("tableSettings", getFocusPanel().getFeatureTable().getViewSettings());
				
			} else {
				
				
				
				// get the focus panel early to force creating the feature table
				// before styles and other changes are applied
				//1787
				//for backward compatibility with aliases
				if (viewSettings.get("aliasNamesMapping") != null) {
					if (viewSettings.get("aliasNamesMapping") instanceof Map) {
						Data.aliasFromSessionMap.clear();
						Data.aliasFromSessionMap.putAll((Map<String, Map<String, String>>) viewSettings.get("aliasNamesMapping"));
					}
				}
				//1787 end
				final ShapeFocusPanel focus = getFocusPanel();
				// remove existing feature collections - whatever the default was, we are replacing it here
				for (FeatureCollection fc: shapeLayer.getFileTable().getFileTableModel().getAll()) {
					shapeLayer.getFileTable().getFileTableModel().remove(fc);
				}
				// load settings from hashtable
				
				if (viewSettings.get("layerName") instanceof String) {
					if (layerName == ""){
						setName("Custom Shape Layer");
					}else{
						setName(layerName);
					}
				}
				if (viewSettings.get("tooltips") instanceof List) {
					shapeLayer.tooltipFields.clear();
					List<?> tooltips = (List<?>)viewSettings.get("tooltips");
					for (Object tip: tooltips) {
						if (tip instanceof Field) {
							shapeLayer.tooltipFields.add((Field)tip);
						}
					}
				}
				layerName = (String)viewSettings.get("layerName");
				if (viewSettings.get("showProgress") instanceof Boolean) {
					shapeLayer.showProgress = (Boolean)viewSettings.get("showProgress");
				}
				
				if (viewSettings.get("styles") instanceof Set) {
					shapeLayer.applyStyleChanges((Set<Style<?>>)viewSettings.get("styles"));
					focus.clearStyleSelection();
				}
				if (viewSettings.get("layers") instanceof List) {
					List<Map<String,Object>> layers = (List<Map<String,Object>>)viewSettings.get("layers");
					final List<LoadData> sources = new ArrayList<LoadData>();
					final Map<Integer,LoadData> idMap = new HashMap<Integer,LoadData>();
					for (final Map<String,Object> layer: layers) {
						// inserts a newly-loaded layer into the ShapeLayer, by
						// setting up the calc field listener, adding it to the
						// filetable, selecting it if necessary, and once all
						// sources are loaded, it restores the table settings
						final LoadListener listener = new LoadListener() {
							public void receive(LoadData data) {
								if (data.fc != null) {
									if (layer.get("id") instanceof Integer) {
										idMap.put((Integer)layer.get("id"), data);
									}
									if (layer.get("calcFields") instanceof Map) {
										
										Map<Field,CalculatedField> calcFields = (Map<Field,CalculatedField>)layer.get("calcFields");
										CalcFieldListener c = new CalcFieldListener(data.fc, shapeLayer, calcFields);
										shapeLayer.calcFieldMap.put(data.fc, c);
										data.fc.addListener(c);
										data.fc.addListener(Data.SERVICE);
									}
									data.fc.setFilename((String)layer.get("filename"));
									shapeLayer.fileTable.getFileTableModel().add(data.fc);
								}
	
								sources.remove(data);
								if (sources.isEmpty()) {
									// when all files have loaded, set the
									// selections, the default collection, table
									// settings, and report any errors to the
									// user
									if (viewSettings.get("defaultID") instanceof Integer) {
										int defaultID = (Integer)viewSettings.get("defaultID");
										LoadData defaultData = idMap.get(defaultID);
										if (defaultData != null) {
											shapeLayer.fileTable.getFileTableModel().setDefaultFeatureCollection(defaultData.fc);
										}
									}
									
									if (viewSettings.get("selections") instanceof int[]) {
										int[] selections = (int[])viewSettings.get("selections");
										for (int idx: selections) {
											// make sure this selected shapelayer loaded okay
											if (idMap.containsKey(idx)) {
												idx = shapeLayer.fileTable.getFileTableModel().getAll().indexOf(idMap.get(idx).fc);
												if (idx >= 0 && idx < shapeLayer.fileTable.getFileTableModel().getRowCount()) {
													shapeLayer.fileTable.getSelectionModel().addSelectionInterval(idx, idx);
												}
											}
										}
									}
									
									//If we have no selected file, select the first one
									if (shapeLayer.fileTable.getSelectionModel().getMinSelectionIndex() == -1) {
										shapeLayer.fileTable.getSelectionModel().addSelectionInterval(0,0);
									}
									//								if (viewSettings.get("tableSettings") instanceof Map) {
									//									SwingUtilities.invokeLater(new Runnable() {
									//										public void run() {
									//											// do this later so the styles listener can update styles columns in response
									//											// to the selection change *before* we restore table settings
									//											focus.getFeatureTable().setViewSettings((Map<String,Object>)viewSettings.get("tableSettings"));
									//										}
									//									});
									//								}
								}
							}
						};
						if (layer.get("providerClass") instanceof Class && layer.get("providerFile") instanceof String) {
							try {
								Class<FeatureProvider> fpClass = (Class<FeatureProvider>)layer.get("providerClass");
								String fileName = (String)layer.get("providerFile");
								String providerUrl = (String) layer.get("providerUrl");
								LoadData source;
								if (providerUrl != null) {
									//here we have a FeatureProviderReadOnly
									String providerDirectory = (String) layer.get("providerDirectory");
									String providerFilename = (String) layer.get("providerFilename");
									FeatureProvider fp = new FeatureProviderReadOnly(providerDirectory, providerFilename, providerUrl);
									source = new LoadData(fp, fileName);
								} else {
									source = new LoadData(fpClass.getConstructor().newInstance(), fileName);
									Field[] schema = (Field[])layer.get("schema");
									Object[][] values = (Object[][])layer.get("values");
									SingleFeatureCollection fc = new SingleFeatureCollection();
									
									for (Field f: schema) {
										fc.addField(f);
									}
									for (Object[] row: values) {
										Feature f = new Feature();
										for (int i = 0; i < schema.length; i++) {
											Object value = row[i];
											if (value instanceof ShapePath) {
												value = ((ShapePath)value).getPath();
											}
											f.setAttribute(schema[i], value);
										}
										fc.addFeature(f);
									}
									source.fc = fc;
								}
								sources.add(source);
								shapeLayer.loadSources(Arrays.asList(source), listener);
							} catch (Exception e) {
								e.printStackTrace();
							}
						} else {
							if (layer.get("schema") instanceof Field[] && layer.get("values") instanceof Object[][]) {
								Field[] schema = (Field[])layer.get("schema");
								Object[][] values = (Object[][])layer.get("values");
								SingleFeatureCollection fc = new SingleFeatureCollection();
								for (Field f: schema) {
									fc.addField(f);
								}
								for (Object[] row: values) {
									Feature f = new Feature();
									for (int i = 0; i < schema.length; i++) {
										Object value = row[i];
										if (value instanceof ShapePath) {
											value = ((ShapePath)value).getPath();
										}
										f.setAttribute(schema[i], value);
									}
									fc.addFeature(f);
								}
								final LoadData load = new LoadData(null, null);
								load.fc = fc;
								sources.add(load);
								// invoke later, so all sources will have been created when the listener is called
								SwingUtilities.invokeLater(new Runnable() {
									public void run() {
										listener.receive(load);
										if(viewSettings.get("tableSettings") instanceof Map){
											getFocusPanel().getFeatureTable().setViewSettings((Map)viewSettings.get("tableSettings"));										}
									}
								});
							} else {
								log.aprintln("Skipping layer with no known collection, found keys " + Arrays.asList(layer.keySet().toArray()));
								continue;
							}
						}
					}
				}
			}
		}
	}
	
    private boolean isMainView() {
    	return getChild() != null;
    }
    
	// styles used in the last rendering of each buffer
	private final Set<Style<?>> normStyles = new HashSet<Style<?>>();
	private final Set<Style<?>> selStyles = new HashSet<Style<?>>();
	
	public ShapeRenderer createRenderer(boolean selBuffer, int featureCount) {
		ShapeLayerStyles styles = shapeLayer.getStyles();
		Set<Style<?>> usedStyles;
		if (selBuffer) {
			styles.showLineDir.setConstant(false);
			// Do not force drawing the "editable vertices" for selected shapes in a read-only shape layer
			if (!isReadOnly) {
				styles.showVertices.setConstant(true);
			}
			styles.showLabels.setConstant(false);
			styles.lineColor.setSource(styles.selLineColor.getSource());
			styles.lineDash.setConstant(new LineType());
			styles.lineWidth.setSource(styles.selLineWidth.getSource());
			styles.fillPolygons.setConstant(false);
			styles.antialias.setConstant(false);
			styles.fillPolygons.setConstant(false);
			styles.drawOutlines.setConstant(true);
			usedStyles = selStyles;
		} else {
			if (!isMainView()) {
				styles.showLabels.setConstant(false);
				styles.showLineDir.setConstant(false);
				styles.showVertices.setConstant(false);
			}
			usedStyles = normStyles;
		}
		
		// set the geometry source to a StyleSource<FPath> that will reach into
		// the shapeLayer's index for world coordinate paths
		styles.geometry.setSource(
			new WorldCacheSource(
				styles.geometry.getSource(),
				shapeLayer.getIndex()));
		
		usedStyles.clear();
		for (Style<?> s: styles.getStyles()) {
			s.setSource(new LogSource(s, usedStyles));
		}
		ShapeRenderer sr = new ShapeRenderer(viewman.getZoomManager().getZoomPPD());
		sr.setStyles(styles);
		return sr;
	}
	
	/**
	 * When the {@link #getValue} method is called, this class adds the
	 * underlying style to the set of styles given to the ctor, to keep track of
	 * which styles are actually used.
	 */
    private static final class LogSource<E> implements StyleSource<E> {
		private static final long serialVersionUID = 1L;
    	private final Style<E> style;
    	private final StyleSource<E> wrappedSource;
    	private final Set<Style<E>> log;
    	public LogSource(Style<E> style, Set<Style<E>> log) {
    		this.style = style;
    		this.wrappedSource = style.getSource();
    		this.log = log;
    	}
		public E getValue(Feature f) {
			log.add(style);
			return wrappedSource.getValue(f);
		}
		public Set<Field> getFields() {
			return wrappedSource.getFields();
		}
    }
    
	/**
	 * Drawing Unit of Work. Each such unit of work is executed on a single serializing
	 * thread. This particular unit of work abandons its current drawing loop as soon 
	 * as it determines that another request has superceeded it.
	 * 
	 *  @see ShapeLView#drawReqTS
	 *  @see ShapeLView#drawThread
	 *  @see ShapeLView#draw(boolean)
	 *  @see SerializingThread#add(Runnable)
	 */
	private class DrawingUow implements Runnable {
		private final Iterator<Feature> features;
		private final long timeStamp;
		private final boolean selected;
		private final int featureCount;
		DrawingProgressDialog pd = null;
		private ShapeRenderer sr;
		
		// don't show the progress dialog for the first five seconds
		private long lastProgress = System.currentTimeMillis() + 2000;
		
		// don't repaint during draw for 1 second
		private long lastPaint = System.currentTimeMillis() + 1000;
		
		/**
		 * Constructs a Drawing Unit of Work for either selected or all the
		 * polygons.
		 * 
		 * @param features the source of features to render, clipped if possible.
		 * @param selected Pass as true to draw selected data only, false for all data.
		 * @param featureCount If > 0, taken to mean the number of calls to features.next(), used by the progress dialog
		 */
		public DrawingUow(Iterator<Feature> features, boolean selected, int featureCount) {
			this.features = features;
			this.selected = selected;
			timeStamp = System.currentTimeMillis();
			sr = createRenderer(selected, featureCount);
	    	log.println(toString()+" created.");
			this.featureCount = featureCount;
		}
		
		public void run() {
			ShapeLayer.LEDState led = null;
			Graphics2D g2screen = getOffScreenG2Direct();
			Graphics2D g2world = getOffScreenG2(selected? 1: 0);
			int position = 0;
			try {
				shapeLayer.begin(led = new ShapeLayer.LEDStateDrawing());
				
				log.println(toString()+" started.");
				
				clearOffScreen(selected? 1: 0);
				
				while (features.hasNext()) {
					Feature f = features.next();
					if (superceeded()){
						log.println(toString()+" superceeded.");
						break;
					}
					
					sr.draw(g2world, g2screen, f, viewman.getProj().getPPD());
					updatePaint();
					updateProgress(position, featureCount);
					position ++;
				}
			} finally {
			    shapeLayer.end(led);
			    sr = null;
			    g2world.dispose();
			    if (!superceeded()) {
			    	repaint();
			    }
			    updateProgress(featureCount+1,featureCount);
				log.println(toString()+" done in " + (System.currentTimeMillis() - timeStamp) + " ms");
			}
		}
		
		private void updatePaint() {
			long now = System.currentTimeMillis();
			if (now - lastPaint > 1000) {
				repaint();
				lastPaint = now;
			}
		}
		
		private synchronized void updateProgress(int progress, int total) {
			if (isMainView() && !selected) {
				if (shapeLayer.showProgress && progress < total) {
					if (pd == null) {
						pd = new DrawingProgressDialog(Main.mainFrame, Main.testDriver.mainWindow, 500L);
						pd.setMaximum(total);
					}
					long now = System.currentTimeMillis();
					// update at most twice a second
					if (now - lastProgress > 500) {
						lastProgress = now;
						final int mark = progress;
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {
								synchronized(DrawingUow.this) {
									if (pd != null) {
										pd.show();
										pd.setValue(mark);
									}
								}
							}
						});
					}
				} else {
					if (pd != null) {
						pd.hide();
						pd = null;
					}
				}
			}
		}
		
		/**
		 * Determines whether the current request has been superceeded.
		 * It makes this determination based on the time stamp of this unit
		 * of work compared to the time stamp of the latest similar unit
		 * of work.
		 * 
		 * @return True if this request has been superceeded.
		 */
		private boolean superceeded(){
			if (drawReqTS[selected? 1: 0] > timeStamp)
				return true;
			return false;
		}
		
		/**
		 * Returns a string representation of this unit of work.
		 */
		public String toString(){
			return getClass().getName()+"["+
				"ts="+timeStamp+","+
				"selected="+selected+"]";
		}
	}
	
	/**
	 * Submit a request to draw the selected data or all data.
	 * 
	 * @param selected When true only the selected data is redrawn,
	 *                 all data is drawn otherwise.
	 */
	public synchronized void draw(final boolean selected) {
		if (this.isAlive()) {
			drawReqTS[selected? 1: 0] = System.currentTimeMillis();
			Thread processor = new Thread(new Runnable() {
				public void run() {
					final LEDStateProcessing led = new ShapeLayer.LEDStateProcessing();
					shapeLayer.begin(led);
					try {
						// Get the features to draw. This must be done in a loop that
						// we break out of when the thread is interrupted, or the
						// index can be checked fully without throwing a
						// ConcurrentModificationException. It would be nice to have
						// the index block until updates to the underlying FeatureCollection
						// are finished, rather than throw exceptions, but the locking
						// logic would have to penetrate all the way down to the
						// guts of FeatureCollection, or into all of the user interface
						// elements.  This is much, much simpler.
						Set<Feature> features = new LinkedHashSet<Feature>();
						while (true) {
							try {
								features.clear();
								
								Rectangle2D displayArea = viewman.getProj().getWorldWindow();
								
								Iterator<Feature> results = shapeLayer.getIndex().queryUnwrappedWorld(displayArea);
								while (results.hasNext()) {
									Feature f = results.next();
									if (!selected || shapeLayer.selections.contains(f)) {
										features.add(f);
									}
								}
								break;
							} catch (ConcurrentModificationException e) {
								try {
									Thread.sleep(500);
								} catch (InterruptedException e1) {
									break;
								}
							}
							if (Thread.currentThread().isInterrupted()) {
								break;
							}
						}
						drawThread.add(new DrawingUow(features.iterator(), selected, features.size()));
					} finally {
						shapeLayer.end(led);
					}
				}
			});
			processor.setPriority(Thread.MIN_PRIORITY);
			processor.setName("ShapeSubsetter" + (isMainView()?"Main":"Panner"));
			processor.start();
		} else {
			setDirty(true);
		}
	}
	
	public FeatureMouseHandler getFeatureMouseHandler(){
		return featureMouseHandler;
	}

	/**
	 * Cleanup code at LView destruction.
	 */
	public void viewCleanup(){
		super.viewCleanup();
		
		// Destroy the focus panel.
		if (focusPanel != null)
			focusPanel.dispose();
		
		shapeLayer.cleanup();
		
		// Destroy the drawing worker thread.
		drawThread.interruptIfBusy(true);
		drawThread.add(SerializingThread.quitRequest);
	}
	
	public void change(Set<Feature> added, Set<Feature> removed) {
		if ((added != null && added.size() > 0) || (removed != null && removed.size() > 0)) {
			draw(true);
		}
	}
	
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
	public String getLayerKey(){
		if (layerParams!=null){
			if (layerParams.name.equals("")){
				return "Custom Shape Layer";
			}else
				return layerParams.name;
		}else{
			return layerName;
		}
 	}
 	public String getLayerType(){
 		if(layerParams!=null)
 			return layerParams.type;
 		else
 			return "shape";
 	}

 	
 //setting initial start up params (sessions)	
	static public class ShapeParams implements SerializedParameters{
		LayerParameters layerParams;
		
		public ShapeParams(LayerParameters lp){
			if(lp!=null){
				this.layerParams=lp;
			}
		}
	}
	public SerializedParameters getInitialLayerData(){
		return new ShapeParams(layerParams);
	}

	@Override
	public void update(Observable o, Object arg) {
		if (arg == null) return;
	    if (o instanceof ActiveViewChangedObservable) {
			updateActiveViewChanged(arg);
		}
	}

	private void updateActiveViewChanged(Object arg) {
		if (!(arg instanceof Layer.LView))
			return;
		// if active view is this Custom Shape - then show callout; else hide it
		LView activeview = LManager.getLManager().getActiveLView();
		if (activeview == this && isCustomShapeInstance(activeview)) {
			if (ToolManager.getToolMode() != ToolManager.SHAPES) {
				conditionallySwitchToShapes(activeview);
			} else if (ToolManager.getToolMode() == ToolManager.SHAPES) {
				conditionallySwitchToSelect(activeview);
				if (!this.isVisible()) {
					promptWhenActiveButNotVisisble();
				}
			}
		} else if (!isCustomShapeInstance(activeview)) {
			if (isCalloutVisible()) {
				hideCallout();
			}
			DrawingPalette.INSTANCE.hideCallout();
		}
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

	private void promptWhenActiveButNotVisisble() {
		// prompt to turn 'M' ON
		ShapesToolStrategy.promptWhenActiveButNotVisisble(this);
	}

	private void conditionallySwitchToSelect(LView activeview) {
		AbstractButton drawaction = DrawingPalette.INSTANCE.getDrawActionPerView(activeview);
		if (drawaction == null) {
			ToolManager.setToolMode(ToolManager.SEL_HAND);
		}
	}

	private void conditionallySwitchToShapes(LView activeview) {
		JButton shapesTool = Main.testDriver.toolMgr.getShapesToolComponent();
		if (isCalloutVisible()) {
			hideCallout();
		} 
		//check if right-click menu of palette instance already had drawing selected, 
		//if so - switch to Shapes tool
		AbstractButton drawaction = DrawingPalette.INSTANCE.getDrawActionPerView(activeview);
		if (drawaction != null) {
			ToolManager.setToolMode(ToolManager.SHAPES);
		} else {
			DrawingPalette.INSTANCE.resetDrawMode(activeview);
			DrawingPalette.INSTANCE.show(activeview);
			//show palette if 
			if (!DrawingPalette.INSTANCE.isPaletteVisible()) {
				String msg = (ToolManager.getToolMode() == ToolManager.SHAPES) ?  
							UserPrompt.ACTIVATE_PALETTE.asString() : UserPrompt.ACTIVATE_PALETTE_2.asString();
				showCallout(shapesTool, msg, UserPrompt.SHORT_TIME);
			}
		}
	}
}
