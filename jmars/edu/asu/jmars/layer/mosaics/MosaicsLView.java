package edu.asu.jmars.layer.mosaics;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.InvestigateData;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LViewFactory;
import edu.asu.jmars.layer.SerializedParameters;
import edu.asu.jmars.layer.WrappedMouseEvent;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.map2.MapLViewFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.ShapeRenderer;
import edu.asu.jmars.layer.util.features.Styles;
import edu.asu.jmars.layer.util.features.WorldCacheSource;
import edu.asu.jmars.util.ObservableSetListener;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

public class MosaicsLView extends LView implements MouseListener {
	Color fillColor;
	Color borderColor;
	Color borderColorSelected;
	FeatureCollection fc;
	MosaicsLayer layer;
	String name="Mosaics";
	
	public MosaicsLView(final MosaicsLayer layer, MosaicsLView3D lview3d){
		super(layer, lview3d);
		this.layer = layer;
		setBufferCount(2);
		
		if(lview3d!=null){
			lview3d.setLView(this);
		}
		
		addMouseListener(this);
		
		fillColor = Util.alpha(Color.BLUE, 128);
		borderColor = Color.white;
		borderColorSelected = Color.yellow;
		
		fc = layer.getFeatures();
		layer.selections.addListener(new ObservableSetListener<Feature>() {
			public void change(Set<Feature> added, Set<Feature> removed) {
				draw(true, layer.selections);
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(MosaicsLView.this, true);
				}
			}
		});
	}
	
	protected LView _new() {
		return new MosaicsLView((MosaicsLayer)getLayer(), null);
	}
	
	protected Object createRequest(Rectangle2D where) {
		return where;
	}
	
	public FocusPanel getFocusPanel() {
		if (focusPanel == null)
			focusPanel = new MosaicsFocusPanel(this);
		
		return focusPanel;
	}
	
	protected ShapeRenderer getFeatureRenderer(boolean forSelected){
		ShapeRenderer sr = new ShapeRenderer(getProj().getPPD());
		Styles styles = sr.getStyleCopy();
		styles.geometry.setSource(new WorldCacheSource(styles.geometry.getSource(), layer.getCache()));
		styles.lineColor.setConstant(forSelected? borderColorSelected: borderColor);
		styles.lineWidth.setConstant(forSelected? 2f : 1f);
		styles.fillColor.setConstant(fillColor);
		styles.fillPolygons.setConstant(!forSelected);
		styles.showVertices.setConstant(false);
		styles.showLineDir.setConstant(false);
		styles.showLabels.setConstant(false);
		sr.setStyles(styles);
		return sr;
	}
	
	private void draw(boolean selections, Collection<Feature> features) {
		if (!isVisible())
			return;
		
		int screen = selections ? 1 : 0;
		clearOffScreen(screen);
		Graphics2D g2w = getOffScreenG2(screen);
		Graphics2D g2sc = getOffScreenG2Direct();
		if (g2w != null) {
			ShapeRenderer sr = getFeatureRenderer(selections);
			for (Feature f: features) {
				sr.draw(g2w, g2sc, f);
			}
		}
		repaint();
	}
	
	public void receiveData(Object layerData) {
		SwingUtilities.invokeLater(new Runnable(){
			public void run() {
				draw(false, layer.fc.getFeatures());
				draw(true, layer.selections);
				
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(MosaicsLView.this, true);
				}
			}
		});
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String newName) {
		name = newName;
		// update labels
		if (LManager.getLManager() != null) {
			LManager.getLManager().updateLabels();
		}
		if (layer != null && layer.getSettings() != null) {
			layer.getSettings().name = newName;//set the name in the InitialParams object to be written out
		}
	}
	
	private Set<Feature> getFeaturesIntersecting(Rectangle2D worldRect){
		Set<Feature> underPt = new LinkedHashSet<Feature>();
		
		for(int i=0; i<fc.getFeatureCount(); i++){
			Feature f = fc.getFeature(i);
			if (f.getPath().getWorld().intersects(worldRect))
				underPt.add(f);
		}
		
		return underPt;
	}
	
	private Set<Feature> getFeaturesUnder(Point2D worldPt){
		return getFeaturesIntersecting(getProj().getClickBox(worldPt, 1));
	}
	
	public String getToolTipText(MouseEvent event) {
		if (viewman.getActiveLView() != this)
			return null;

		Point2D worldPt = getProj().screen.toWorld(event instanceof WrappedMouseEvent?
				((WrappedMouseEvent)event).getRealPoint(): event.getPoint());
		Set<Feature> features = getFeaturesUnder(worldPt);
		
		if (features.isEmpty())
			return null;
		
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("<html>");
		for(Feature f: features){
			sbuf.append("<b>"+(layer.selections.contains(f)?"<i>":"")+f.getAttribute(Field.FIELD_LABEL)+(layer.selections.contains(f)?"</i>":"")+"</b><br>");
			String abstractText = (String)f.getAttribute(FeatureProviderWMS.FIELD_ABSTRACT);
			if (abstractText != null && abstractText.trim().length() > 0){
				sbuf.append("<table cellspacing=0 cellpadding=0>");
				sbuf.append("<tr>");
				sbuf.append("<td></td>");
				sbuf.append("<td>"+abstractText+"</td>");
				sbuf.append("</tr>");
				sbuf.append("</table>");
				sbuf.append("<br>");
			}
		}
		sbuf.append("</html>");
		
		return sbuf.toString();
	}
	
// Return data for the investigate tool to display
	public InvestigateData getInvestigateData(MouseEvent event){
		InvestigateData invData = new InvestigateData(getName());
		
		Point2D worldPt = getProj().screen.toWorld(event instanceof WrappedMouseEvent?
				((WrappedMouseEvent)event).getRealPoint(): event.getPoint());
		Set<Feature> features = getFeaturesUnder(worldPt);
		
		if (features.isEmpty())
			return null;
		
		for(Feature f : features){
			invData.add((String)f.getAttribute(Field.FIELD_LABEL), "");
		}
		
		return invData;
	}

	public Component[] getContextMenu(Point2D worldPt){
		Set<Feature> features = getFeaturesUnder(worldPt);
		List<Component> centerAtMenuItems = new ArrayList<Component>();
		List<Component> loadMenuItems = new ArrayList<Component>();
		List<Component> ViewCitationMenuItems = new ArrayList<Component>();
		JMenuItem menuItem;
		for(Feature f: features){
			menuItem = new JMenuItem(new CenterAtAction(f));
			if (layer.selections.contains(f))
				menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC|Font.BOLD));
			centerAtMenuItems.add(menuItem);
			menuItem = new JMenuItem(new LoadMosaicAction(f));
			if (layer.selections.contains(f))
				menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC|Font.BOLD));
			loadMenuItems.add(menuItem);
			menuItem = new JMenuItem(new ViewCitationAtAction(Main.mainFrame, f));
			if (layer.selections.contains(f))
				menuItem.setFont(menuItem.getFont().deriveFont(Font.ITALIC|Font.BOLD));
				ViewCitationMenuItems.add(menuItem);
			}
		
		List<Component> agg = new ArrayList<Component>();
		agg.addAll(centerAtMenuItems);
		agg.add(new JSeparator(JSeparator.HORIZONTAL));
		agg.addAll(loadMenuItems);
		agg.add(new JSeparator(JSeparator.HORIZONTAL));
		agg.addAll(ViewCitationMenuItems);
		
		return agg.toArray(new Component[0]);
	}
	
	private Feature findClosest(Collection<Feature> underMouse, Point2D worldPt){
		Feature closest = null;
		double  minDistance = Double.MAX_VALUE;
		
		for(Feature f: underMouse){
			FPath wfpath = f.getPath().getWorld();
			Point2D[] vertices = wfpath.getVertices();
			
			double dist = Double.MAX_VALUE;
			Line2D.Double line = new Line2D.Double();
			for(int i=1; i<vertices.length; i++){
				line.setLine(vertices[i-1], vertices[i]);
				Util.normalize360(line);
				dist = Math.min(dist, line.ptSegDist(worldPt));
			}
			
			if (dist < minDistance){
				closest = f;
				minDistance = dist;
			}
		}
		
		return closest;
	}
	
	public SerializedParameters getInitialLayerData(){
		return layer.getSettings();
	}
	
	//
	// Implementation of MouseListener interface
	//
	
	public void mouseClicked(MouseEvent e) {
		if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 1){
			layer.selections.clear();
			Point2D worldPt = getProj().screen.toWorld(e.getPoint());
			layer.selections.addAll(getFeaturesUnder(worldPt));
		}
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}

	
	//
	// Helper classes
	//

	public class CenterAtAction extends AbstractAction {
		private final Feature f;
		
		public CenterAtAction(Feature f){
			super("Center at "+f.getAttribute(Field.FIELD_LABEL));
			this.f = f;
		}
		
		public Feature getFeature(){
			return f;
		}
		
		public void actionPerformed(ActionEvent e) {
			Point2D centerAt = f.getPath().getWorld().getCenter(); 
			centerAtPoint(centerAt);
		}
	}
	
	public class ViewCitationAtAction extends AbstractAction {
		private final Feature f;
		private final Window ownerFrame;
		
		public ViewCitationAtAction(Window ownerFrame, Feature f){
			super("View abstract for "+f.getAttribute(Field.FIELD_LABEL));
			this.f = f;
			this.ownerFrame = ownerFrame;
		}
		
		public Feature getFeature(){
			return f;
		}
		
		public void actionPerformed(ActionEvent e) {
			
			JTextArea ta = new JTextArea((String)f.getAttribute(FeatureProviderWMS.FIELD_ABSTRACT), 5, 40);
			ta.setEditable(false);
			ta.setLineWrap(true);
			ta.setWrapStyleWord(true);
			
			JScrollPane areaScrollPane = new JScrollPane(ta);
			areaScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			
			Util.showMessageDialogObj(areaScrollPane,
					"Abstract for "+f.getAttribute(Field.FIELD_LABEL), JOptionPane.INFORMATION_MESSAGE);
			}
	}
	
	public class LoadMosaicAction extends AbstractAction {
		private final Feature f;
		
		public LoadMosaicAction(Feature f){
			super("Load "+f.getAttribute(Field.FIELD_LABEL));
			this.f = f;
		}
		
		public Feature getFeature(){
			return f;
		}

		public void actionPerformed(ActionEvent e) {
			MapLViewFactory mapLViewFactory = (MapLViewFactory)LViewFactory.getFactoryObject(MapLViewFactory.class.getName());
			mapLViewFactory.createLayer((MapSource)f.getAttribute(FeatureProviderWMS.FIELD_MAP_SOURCE), null);
		}
	}
	

	/**
	 * Initial layer creation parameters that get serialized onto the disk
	 * and layer recreated when JMARS is started using a ".jmars" file.
	 */
	static class InitialParams implements SerializedParameters, Serializable {
		private static final long serialVersionUID = -1L;
		public String url = null;//set in the MosaicsLayer constructor, can be null;
		public String name = null;
	}

	//The following two methods are used to query for the
	// info panel fields (description, citation, etc)	
	   	public String getLayerKey(){
	   		return "Mosaic Outlines";
	   	}
	   	public String getLayerType(){
	   		return "mosaic";
	   	}
	
}
