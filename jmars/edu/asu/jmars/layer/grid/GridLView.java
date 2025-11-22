package edu.asu.jmars.layer.grid;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.graphics.JFontChooser;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.grid.GridParameters.MyLabelFont;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.swing.ColorButton;
import edu.asu.jmars.swing.PasteField;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FONTS;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;
import mdlaf.utils.MaterialColors;

public class GridLView extends Layer.LView implements Observer
 {
	private GridLayer mylayer;
    private static DebugLog log = DebugLog.instance();
    
    private final Color majorLinesColor = new Color(Config.get("gridcolor", 0));
    private Map<String, Point2D.Float> latloncache = new HashMap<>();
    private Font defaultFontMinor = ThemeFont.getRegular().deriveFont(FONTS.ROBOTO_CHART_SMALL.fontSize());
    
    GridParameters parms = null; // Used to save user grid selections across a save/restore of session or layer.
    MyLabelFont mylabelfontmajor = new MyLabelFont(); //use default values of MyLabelFonr instance
    MyLabelFont mylabelfontminor = new MyLabelFont(defaultFontMinor, MaterialColors.COSMO_MEDIUM_GRAY, null);
    		
    GridSettings   major = new GridSettings(majorLinesColor, 10.0, true, true, true, 1, majorLinesColor, mylabelfontmajor, true, false);
    GridSettings   minor = new GridSettings(Color.gray, 2.0, false, false, false, 1, Color.gray, mylabelfontminor, false, false);
    SettingsPanel  majorSettings = null;
    SettingsPanel  minorSettings = null;  

    // These constants make using the visibility member of GridSetings more understandable.
    static final int  MAIN   = 0;
    static final int  PANNER = 1;
    static final int  THREE_D = 2;

    public GridLView(boolean main, Layer layerParent, GridParameters params, GridLView3D lview3d) {
        super(layerParent, lview3d);
        mylayer = (GridLayer)layerParent;
        setBufferCount(1);
        this.parms = params;
        copyParmsToSettings();
        lview3d.setLView(this);
        Main.longitudeswitch.addObserver(this);
        Main.latitudeswitch.addObserver(this);
    }

	private void copyParmsToSettings() {  //deserialization
        major.setColor(new Color(parms.majorColor));
        major.setLineWidth(parms.majorWidth);
        major.setSpacing(parms.majorSpacing);
        major.setVisible(MAIN, parms.majorMainVisible);
        major.setVisible(PANNER, parms.majorPannerVisible);
        major.setVisible(THREE_D, parms.major3DVisible);
        major.setLabelColor(new Color(parms.getMajorLabelColor()));
        MyLabelFont mlf = new MyLabelFont();
        if (parms.majorLabelFont == null) { //backward comp
        	mlf.withFontFillColor(major.getLabelColor());
        }
        major.setLabelFont(parms.majorLabelFont == null ? mlf : parms.majorLabelFont);
        major.setLabelVisible(MAIN, parms.majorMainLabelVisible);
        major.setLabelVisible(PANNER, parms.majorPannerLabelVisible);
        
        minor.setColor(new Color(parms.minorColor));
        minor.setLineWidth(parms.minorWidth);
        minor.setSpacing(parms.minorSpacing);
        minor.setVisible(MAIN, parms.minorMainVisible);
        minor.setVisible(PANNER, parms.minorPannerVisible);
        minor.setVisible(THREE_D, parms.minor3DVisible);
        minor.setLabelColor(new Color(parms.getMinorLabelColor()));
        MyLabelFont mlf2 = new MyLabelFont(defaultFontMinor, MaterialColors.COSMO_MEDIUM_GRAY, null);
        if (parms.minorLabelFont == null) { //backward comp
        	mlf2.withFontFillColor(minor.getLabelColor());
        }
        minor.setLabelFont(parms.minorLabelFont == null ? mlf2 : parms.minorLabelFont);
        minor.setLabelVisible(MAIN, parms.minorMainLabelVisible);
        minor.setLabelVisible(PANNER, parms.minorPannerLabelVisible);
    }

    public void copySettingsToParms() {  //serialization
        parms.majorColor         = major.getColor().getRGB();
        parms.majorWidth         = major.getLineWidth();
        parms.majorSpacing       = major.getSpacing();
        parms.majorMainVisible   = major.isVisible(MAIN);
        parms.majorPannerVisible = major.isVisible(PANNER);
        parms.major3DVisible	 = major.isVisible(THREE_D);
        
        parms.majorLabelColor         = major.getLabelColor().getRGB();
        parms.majorLabelFont		  = major.myLabelFont;
        parms.majorMainLabelVisible   = major.labelIsVisible(MAIN);
        parms.majorPannerLabelVisible = major.labelIsVisible(PANNER);
        
        parms.minorColor         = minor.getColor().getRGB();
        parms.minorWidth         = minor.getLineWidth();
        parms.minorSpacing       = minor.getSpacing();
        parms.minorMainVisible   = minor.isVisible(MAIN);
        parms.minorPannerVisible = minor.isVisible(PANNER);        
        parms.minor3DVisible	 = minor.isVisible(THREE_D);
        
        parms.minorLabelColor         = minor.getLabelColor().getRGB();
        parms.minorLabelFont		  = minor.myLabelFont;
        parms.minorMainLabelVisible   = minor.labelIsVisible(MAIN);
        parms.minorPannerLabelVisible = minor.labelIsVisible(PANNER);
    }

    static class GridSettings
	 {
		private Color     color;
		public double    spacing;
		private boolean[] visible = new boolean[3]; // Main View,Panner View,3D view
		private int       lineWidth; 
		private Color     labelColor;
		
		MyLabelFont  myLabelFont;
		private boolean[] labelVisible = new boolean[2]; // Main View, Panner View
		
		// Cached data - same for both main & panner views
		private double cachedSpacing = 0;
		private ProjObj cachedProj = null;
		private GeneralPath[] cachedLines = new GeneralPath[0];
		
		public GridSettings(Color color, double spacing, boolean mainVisible, boolean pannerVisible, boolean threeDVisible, int lineWidth,
				Color labelColor, MyLabelFont labelfont,  boolean labelMainVisible, boolean labelPannerVisible){
			this.color = color;
			this.spacing = spacing;
			this.visible[0] = mainVisible;
			this.visible[1] = pannerVisible;
			this.visible[2] = threeDVisible;
			this.lineWidth = lineWidth; 
			this.labelColor = labelColor;
			this.myLabelFont = labelfont;
			this.labelVisible[0] = labelMainVisible;
			this.labelVisible[1] = labelPannerVisible;
		}
		
		public synchronized void setColor(Color newColor){
			color = newColor;
		}
		
		public synchronized Color getColor(){
			return color;
		}

		public synchronized void setLabelColor(Color newColor){
			labelColor = newColor;
		}

		public synchronized Color getLabelColor(){
			return labelColor;
		}	
			
		public synchronized MyLabelFont getLabelFont() {
			return myLabelFont;
		}

		public synchronized void setLabelFont(MyLabelFont labelFont) {
			this.myLabelFont = labelFont;
		}

		public synchronized void setSpacing(double newSpacing){
			spacing = newSpacing;
		}
		public synchronized double getSpacing(){
			return spacing;
		}
		
		public synchronized boolean isVisible(int index){
			return visible[index];
		}
		public synchronized void setVisible(int index, boolean visibility){
			visible[index] = visibility;
		}

		public synchronized boolean labelIsVisible(int index){
			if (index>=labelVisible.length) return false;
			return labelVisible[index];
		}
		public synchronized void setLabelVisible(int index, boolean visibility){
			labelVisible[index] = visibility;
		}

		public synchronized void setLineWidth(int newLineWidth){
			lineWidth = newLineWidth;
		}
		public synchronized int getLineWidth(){
			return lineWidth;
		}

		public GeneralPath[] getLines(ProjObj proj){
			if (cachedSpacing != spacing || cachedProj != proj){
				if (spacing == 0)
					cachedLines = new GeneralPath[0];
				else
					cachedLines = generateLines(proj, spacing);
				
				cachedSpacing = spacing;
				cachedProj = proj;
			}
			
			return cachedLines;
		}
	 }

	
	/**
	 ** Draws a screen-coordinate line, using a spatial graphics
	 ** context
	 **/
	private void drawLine(int x1, int y1,
						  int x2, int y2)
	 {
		Graphics2D g2 = getOnScreenG2();
		prepare(g2);
		g2.setXORMode(Color.gray);
		g2.setStroke(new BasicStroke(0));
		Graphics2D g2s = getProj().createSpatialGraphics(g2);
		Point2D down = getProj().screen.toSpatial(x1, y1);
		Point2D curr = getProj().screen.toSpatial(x2, y2);
		g2s.draw(new Line2D.Double(down, curr));
		g2s.dispose();
	 }
	
	public GridLView()
	 {
		super(null);
	 }
	
	
	/**
	 * This is good - build the focus panel on demand instead of during the view's constructor
	 * That way, the saved session variables get propagated properly.
	 */
	public FocusPanel getFocusPanel() {
		if (focusPanel == null) {
			focusPanel = new FocusPanel(this, true);
			focusPanel.add("Adjustments",new GridPanel());
		}
		return focusPanel;
	}

	/**
	 ** Layer-less view; simply returns null, since it's never used.
	 **
	 ** @return null
	 **/
	protected Object createRequest(Rectangle2D where)
	 {
		return  null;
	 }

	/**
	 ** Layer-less view; simply returns without doing anything, since
	 ** it's never called.
	 **/
	public void receiveData(Object layerData)
	 {
		log.aprintln("PROGRAMMER: SOMETHING'S OFF!");
		log.aprintStack(5);
	 }

	protected Layer.LView _new()
	 {
		GridLView newLView = new GridLView();
		
		newLView.parms = parms;
		// Share the major/minor settings from the very first LView
		newLView.major = major;
		newLView.minor = minor;
		
		newLView.setLayer(getLayer());
		
		return newLView;
	 }

	/**
	 ** Draws in lat/lon lines offscreen.
	 **/
	protected void viewChangedPost()
	 {
		redrawGrid();
	 }

	/**
	 ** Maintains an id to keep track of redraws, so that as soon as a
	 ** new one is issued, the older ones die. Without this, we end up
	 ** overlapping our redraws when the user pans/zooms too quickly.
	 **/
	private void redrawGrid()
	 {
		int id;
		synchronized(viewChangeIDLock)
		 {
			id = ++lastViewChangeID;
		 }
		redrawGrid(id);
	 }
	private int lastViewChangeID = 0;
	private Object viewChangeIDLock = new Object();

	private boolean oldViewChange(int id)
	 {
		// Should really be synchronized over viewChangeIDLock, but
		// it's only ever called while under a redrawLock, so we'll
		// pretend it's safe.
		return  lastViewChangeID != id;
	 }

	private void redrawGrid(int id)
	 {
		synchronized(redrawLock)
		 {
			if(!oldViewChange(id)) {
				clearOffScreen();
			} else {
				return;
			}
			
			if (!isAlive()) {
				return;
			}
			
			// Determine how many periods to draw (360-degree horizontal boxes)
			double minX = viewman.getProj().getWorldWindow().getMinX();
			double maxX = viewman.getProj().getWorldWindow().getMaxX();

			long minPeriod = (long) Math.floor(minX / 360);
			long maxPeriod = (long) Math.ceil (maxX / 360);
			int count = (int) (maxPeriod - minPeriod);

			// Create graphics contexts for each period
			Graphics2D g2 = getOffScreenG2Raw();
			prepare(g2);
			if(g2 == null)
				return;

			Graphics2D[] copies = new Graphics2D[count];
			for(int i=0; i<count; i++)
			 {
				copies[i] = (Graphics2D) g2.create();
				copies[i].translate(360*(i+minPeriod), 0);
				copies[i].clip(new Rectangle(0, -90, 360, 180));
			 }

			// Actually draw the grid elements
			drawLatLon(id, copies, minor);
			drawLatLon(id, copies, major);

			latloncache.clear();
			drawLatText(id, copies, major);
			drawLatText(id, copies, minor);
			
			if(!oldViewChange(id))
				repaint();
		 }
	 }
	

	private Object redrawLock = new Object();

	private static GeneralPath[] generateLines(ProjObj po, double spacing){
		log.println("spacing = " + spacing);
		log.printStack(3);
		int expected = (int) Math.ceil(360/spacing + 180/spacing);
		log.println("Expecting " + expected + " paths");
		List<GeneralPath> paths = new ArrayList<GeneralPath>(expected);
		GeneralPath gp = new GeneralPath();

		final double prec = 2;

		// Lines of latitude
		for(double lat=-90+spacing; lat-0.001<=+90-spacing; lat+=spacing)
		 {
			gp.reset();

			Point2D start = worldPt(po, 0, lat);
			gp.moveTo((float) start.getX(),
					  (float) start.getY());

			for(double lon=prec; lon-0.0000001<=360; lon+=prec)
				connectTo(gp, worldPt(po, lon, lat));

			paths.add((GeneralPath)gp.clone());
		 }

		// Lines of longitude
		for(double lon=0; lon<360; lon+=spacing)
		 {
			gp.reset();

			Point2D start = worldPt(po, lon, -90);
			gp.moveTo((float) start.getX(),
					  (float) start.getY());

			for(double lat=-90+prec; lat-0.0000001<=+90; lat+=prec)
				connectTo(gp, worldPt(po, lon, lat));

			paths.add((GeneralPath)gp.clone());
		 }

		log.println("Generated " + paths.size() + " paths");

		return  (GeneralPath[]) paths.toArray(new GeneralPath[0]);
	}

	private void drawLatLon(int id, Graphics2D[] g2s, GridSettings settings){
		drawLatLon(id, g2s, settings, Main.PO, settings.isVisible(getChild() != null ? 0:1));
	}
	
	protected void drawLatLon(int id, Graphics2D[] g2s, GridSettings settings, ProjObj po, boolean draw)
	 {

		int lineStroke = 1; // default to thinnest for the panner

		if(draw)
		 {
			log.printStack(3);
			GeneralPath[] lines = settings.getLines(po);
			
			// Determine line width stroke to use. Always use thinnest possible for the Panner.
			if (getChild() != null)
				lineStroke = settings.getLineWidth();
			
			// Draw them all
			log.println("Drawing " + lines.length +
						" paths to " + g2s.length + " graphics contexts");
			for(int i=0; i<g2s.length; i++)
			 {
				g2s[i].setColor(settings.getColor());
				g2s[i].setStroke(getProj().getWorldStroke(lineStroke));
				for(int j=0; j<lines.length; j++)
				 {
				 //check the id greater than 0, because the lview3d is calling
				 // with -1 always, so redraw it whenever it's called.
					if(id>0 && oldViewChange(id))
						return;
					g2s[i].draw(lines[j]);
				 }
			 }
		 }
	 }

	private void drawLatText(int id, Graphics2D[] g2s, GridSettings settings){
		drawLatText(id, g2s, settings, Main.PO, settings.labelIsVisible(getChild() != null ? 0:1));
	}

	/**
	 * Note that this function is very similar, but slightly different from, the function in GridLView3D.
	 * The differences are that the GridLView version works in screen coordinates, but we don't have screen coordinates for
	 * our virtual view there, so have to translate to world coordinates instead.  
	 * 
	 * If this function changes here, be sure it still matches the version in GridLView3D and vice versa when HigherResExport is used.
	 * 
	 */
	protected void drawLatText(int id, Graphics2D[] g2s, GridSettings settings, ProjObj po, boolean draw)
	 {
		if(draw)
		 {
			Graphics2D g2 = getOffScreenG2Direct();
			
		if ( settings.myLabelFont != null) {
			Font f = settings.myLabelFont.getLabelFont();  //backward comp
			if (f != null) {
				g2.setFont(f);
			}
		 }
		
		if ( settings.myLabelFont != null) {
			Color c = settings.myLabelFont.getFontFillColor();  //backward comp
			if (c != null) {
			    g2.setColor(c);
			} else {
				g2.setColor(settings.getLabelColor());
			}
		} else {
			g2.setColor(settings.getLabelColor());
		}

			Point2D pts[] = viewman.getWorldBoundingPoints();
//			Rectangle2D screenRect = viewman.getProj().getScreenWindow();

			AffineTransform world2Screen = viewman.getProj().getWorldToScreen();
			AffineTransform screen2World = viewman.getProj().getScreenToWorld();
			
			world2Screen.transform(pts[0], pts[0]);
			world2Screen.transform(pts[1], pts[1]);
			
//			double minY = screenRect.getMaxY();
//			double maxY = screenRect.getMinY();
//			
//			double minX = screenRect.getMinX();
//			double maxX = screenRect.getMaxX();
			
			double minY = pts[0].getY();
			double maxY = pts[1].getY();
			
			double minX = pts[0].getX();
			double maxX = pts[1].getX();		

			String coordOrdering = Config.get(Config.CONFIG_LAT_LON,Ordering.LAT_LON.asString());
			Ordering ordering = Ordering.get(coordOrdering);

			double spacingInterval = settings.spacing;

			DecimalFormat decimalFormat = new DecimalFormat();
			
			double textInset = 5.0;
			
			double lastLatVal = Double.NaN;
			double lastLonVal = Double.NaN;
					
			for (double y=maxY; y<minY; y+=1) {
				Point2D thisWorldPoint = screen2World.transform(new Point2D.Double(minX, y), null);
				Point2D thisSpatialPoint = po.convWorldToSpatial(thisWorldPoint);
			
				if (!Double.isNaN(lastLatVal)) {
					for (double latDraw = -90; latDraw<=90; latDraw+=spacingInterval) {
						if (lastLatVal > latDraw && thisSpatialPoint.getY() <= latDraw) {
							String drawString = decimalFormat.format(ordering.formatLatitude(latDraw)) + ordering.degreeSymbol + ordering.defaultLat;
							float fx = (float) (minX + textInset);
							float fy = (float) (y - textInset);
							Point2D fpoint;
							if (settings == major) { //build cache
								latloncache.put(drawString,  new Point2D.Float(fx, fy));
								g2.drawString(drawString, fx, fy);
							} else if (settings == minor) { //use cache 
								fpoint = latloncache.get(drawString);
							  if (fpoint != null) {
								 Point2D.Float fpoint2 = new Point2D.Float(fx, fy);
								 if (fpoint2.equals(fpoint)) {
									 continue;
								 } else {
									 g2.drawString(drawString, fx, fy); 
								 }
							  } else {
								  g2.drawString(drawString, fx, fy);
							  }
							}
						}
					}
				}
				
				lastLatVal = po.convWorldToSpatial(thisWorldPoint).getY();
			}
			
			lastLatVal = Double.NaN;
			lastLonVal = Double.NaN;
			
			for (double x=minX; x<maxX; x+=1) {
				Point2D thisWorldPoint = screen2World.transform(new Point2D.Double(x, minY), null);
				Point2D thisSpatialPoint = po.convWorldToSpatial(thisWorldPoint);
							
				if (!Double.isNaN(lastLonVal)) {
					for (double lonDraw = 0; lonDraw<=360; lonDraw+=spacingInterval) {
						if (lastLonVal > lonDraw && thisSpatialPoint.getX() <= lonDraw) {
							String drawString = decimalFormat.format(ordering.formatLongitude(lonDraw)) + ordering.degreeSymbol + ordering.getLongitudeSuffix();
							float fx = (float) Math.floor(x+textInset);
							float fy = (float) (minY-textInset);
							Point2D fpoint;
							if (settings == major) { //build cache
								latloncache.put(drawString,  new Point2D.Float(fx, fy));
								g2.drawString(drawString, fx, fy);
							} else if (settings == minor) { //use cache 
								fpoint = latloncache.get(drawString);
							  if (fpoint != null) {
								 Point2D.Float fpoint2 = new Point2D.Float(fx, fy);
								 if (fpoint2.equals(fpoint)) {
									 continue;
								 } else {
									 g2.drawString(drawString, fx, fy); 
								 }
							  } else {
								  g2.drawString(drawString, fx, fy);
							  }
							}							
						}
					}
				}
				
				lastLonVal = po.convWorldToSpatial(thisWorldPoint).getX();
			}
		 }
	 }

	private static void connectTo(GeneralPath gp, Point2D next)
	 {
		//log.println("------------------------------------------");
		//log.println("connectTo " + next);
		Point2D curr = gp.getCurrentPoint();
		if(next.distanceSq(curr) < 90*90)
			gp.lineTo((float)next.getX(),
					  (float)next.getY());
		else
		 {
			/**
			 ** We have a discontinuity, because we've drawn past the
			 ** current period's box. We solve this by drawing
			 ** twice... once from the real curr point to a fake next
			 ** point, and again from a fake curr point to the real
			 ** next point. These fake points are outside the box,
			 ** positioned to emulate wrap-around from one side of the
			 ** box to another.
			 **/

			// Fake x, if necessary
			double fakeCurrX = curr.getX();
			double fakeNextX = next.getX();
			if(Math.abs(fakeCurrX - fakeNextX) > 180)
			 {
				//log.println("Faking X");
				fakeCurrX += curr.getX()<180 ? 360 : -360;
				fakeNextX += next.getX()<180 ? 360 : -360;
			 }

			// Fake y, if necessary
			double fakeCurrY = curr.getY();
			double fakeNextY = next.getY();
			if(Math.abs(fakeCurrY - fakeNextY) > 90)
			 {
				//log.println("Faking Y");
				fakeCurrY += curr.getY()<0 ? 180 : -180;
				fakeNextY += next.getY()<0 ? 180 : -180;
			 }

			// Draw from the real curr to the fake next
			gp.lineTo((float) fakeNextX,
					  (float) fakeNextY);

			// Draw from the fake curr to the real next
			gp.moveTo((float) fakeCurrX,
					  (float) fakeCurrY);
			gp.lineTo((float) next.getX(),
					  (float) next.getY());

			//log.println("line from " + curr.getX() + ", " + curr.getY());
			//log.println("       to " + fakeNextX   + ", " + fakeNextY  );
			//log.println("  move to " + fakeCurrX   + ", " + fakeCurrY  );
			//log.println("  line to " + next.getX() + ", " + next.getY());
		 }
	 }

	// Handy utility
	private static Point2D worldPt(ProjObj po, double lon, double lat)
	 {
		return  po.convSpatialToWorld(new Point2D.Double(lon, lat));
	 }

		private class SettingsPanel extends JPanel implements ActionListener {

			GridSettings settings;

			JTextField txtSpacing;
			ColorButton btnColor;
			JCheckBox chkMain;
			JCheckBox chkPanner;
			JCheckBox chk3D;
			JComboBox lineWidthCombo;

			JCheckBox labelChkMain;
			JCheckBox labelChkPanner;
			JButton modFont;

			SettingsPanel(final String title, GridSettings settings) {
				this.settings = settings;

				// Set up the layout and borders
				setLayout(new GridLayout(0, 1, 0, 5));
				setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(title),
						BorderFactory.createEmptyBorder(5, 5, 5, 5)));

				// Spacing text field
				add(new JLabel("Spacing:", JLabel.LEFT));
				txtSpacing = new PasteField(String.valueOf(settings.getSpacing()), 4);
				txtSpacing.addActionListener(this);
				add(txtSpacing);

				// Color chooser button
				add(new JLabel("Grid Line Color:", JLabel.LEFT));
				btnColor = new ColorButton("", settings.getColor());
				btnColor.addPropertyChangeListener(colorListener);
				add(btnColor);

				// Main checkbox
				add(new JLabel("Visible on:", JLabel.LEFT));
				chkMain = new JCheckBox("Main Window", settings.isVisible(MAIN));
				chkMain.addActionListener(this);
				add(chkMain);

				// Panner checkbox
				chkPanner = new JCheckBox("Panner", settings.isVisible(PANNER));
				chkPanner.addActionListener(this);
				add(chkPanner);

				// 3D checkbox
				chk3D = new JCheckBox("3D", settings.isVisible(THREE_D));
				chk3D.addActionListener(this);
				add(chk3D);

				// Grid line width combo box
				Integer[] widthChoices = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

				lineWidthCombo = new JComboBox(widthChoices);
				lineWidthCombo.setSelectedIndex(settings.getLineWidth() - 1);
				lineWidthCombo.addActionListener(this);

				// Place the label and the combo on the same row
				JPanel lineWidthPanel = new JPanel();
				lineWidthPanel.setLayout(new GridLayout(1, 2, 10, 0));
				lineWidthPanel.add(new JLabel("Line width:", JLabel.LEFT));
				lineWidthPanel.add(lineWidthCombo);
				add(lineWidthPanel);

				// Settings related to line labels
				// Color chooser button
				add(new JLabel("Label Font and Color:", JLabel.LEFT));

				modFont = new JButton("Label Font and Color...".toUpperCase());
				modFont.addActionListener(this);
				JPanel fontpanel = new JPanel(new BorderLayout(4, 0));
				fontpanel.add(modFont, BorderLayout.CENTER);
				add(fontpanel);

				// Main checkbox
				add(new JLabel("Label Visible on:", JLabel.LEFT));
				labelChkMain = new JCheckBox("Main Window", settings.labelIsVisible(MAIN));
				labelChkMain.addActionListener(this);
				add(labelChkMain);

				// Panner checkbox
				labelChkPanner = new JCheckBox("Panner", settings.labelIsVisible(PANNER));
				labelChkPanner.addActionListener(this);
				add(labelChkPanner);

			} // Constructor

			private PropertyChangeListener colorListener = new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					Object source = evt.getSource();

					// Handle color chooser changes
					if (source == btnColor) {
						Color newColor = btnColor.getColor();

						if (newColor != null) {
							// Reflect the new color
							btnColor.setBackground(newColor);

							// Color propagates to the main window and the panner
							settings.setColor(newColor);
						}

						// Only repaint those whose lines are visible
						boolean redrawMain = chkMain.isSelected();
						boolean redrawPanner = chkPanner.isSelected();
						boolean redraw3d = chk3D.isSelected();

						// Based on what changed, trigger appropriate redraws
						if (redrawMain) {
							redrawGrid();
						}
						if (redrawPanner) {
							((GridLView) getChild()).redrawGrid();
						}
						if (redraw3d) {
							mylayer.increaseStateId(0);
							if (ThreeDManager.getInstance().isReady()) {
								ThreeDManager.getInstance().updateDecalsForLView(GridLView.this, true);
							}
						}
						if (redrawMain || redrawPanner) {
							copySettingsToParms(); // If changes have been made, we need to save them to the save/load
													// param object.
						}
					}
				}
			};

			// Handles all events from user, except color buttons (done in property change
			// listener)
			public void actionPerformed(ActionEvent e) {
				Object source = e.getSource();
				// Track what needs redrawing, based on what changed
				boolean redrawMain = false;
				boolean redrawPanner = false;
				boolean redraw3D = false;

				// Handle spacing value changes
				if (source == txtSpacing) {
					double spacing;
					try {
						// Note: parseDouble may throw NumberFormatException
						spacing = Double.parseDouble(txtSpacing.getText().trim());

						// Check to see if it divides evenly
						if (90.0 / spacing != Math.floor(90.0 / spacing))
							throw new NumberFormatException();
					} catch (NumberFormatException ex) {
						// Handle problems by beeping and showing a dialog
						String oldValue = String.valueOf(settings.getSpacing());
						Toolkit.getDefaultToolkit().beep();
						Util.showMessageDialog("Grid spacing value must be a number that divides "
								+ "evenly into 90:\n\t" + txtSpacing.getText(), "Illegal value",
								JOptionPane.ERROR_MESSAGE);

						// Restore the old value and highlight it.
						txtSpacing.setText(oldValue);
						txtSpacing.setSelectionStart(0);
						txtSpacing.setSelectionEnd(oldValue.length());
						txtSpacing.requestFocus();

						return;
					}

					// Spacing propagates to the main window and the panner
					settings.setSpacing(spacing);

					// Determine what needs to be redrawn, based on this change
					redrawMain = chkMain.isSelected();
					redrawPanner = chkPanner.isSelected();
				}

				// Handle main checkbox changes
				else if (source == chkMain) {
					settings.setVisible(MAIN, chkMain.isSelected());
					redrawMain = true;
				}

				// Handle panner checkbox changes
				else if (source == chkPanner) {
					settings.setVisible(PANNER, chkPanner.isSelected());
					redrawPanner = true;
				}

				else if (source == labelChkMain) {
					settings.setLabelVisible(MAIN, labelChkMain.isSelected());
					redrawMain = true;
				}

				// Handle panner checkbox changes
				else if (source == labelChkPanner) {
					settings.setLabelVisible(PANNER, labelChkPanner.isSelected());
					redrawPanner = true;
				}

				else if (source == chk3D) {
					settings.setVisible(THREE_D, chk3D.isSelected());
					redraw3D = true;
				}

				// Handle line width ComboBox changes
				else if (source == lineWidthCombo) {
					settings.setLineWidth(((Integer) lineWidthCombo.getSelectedItem()).intValue());
					redrawMain = true;
					redraw3D = chk3D.isSelected();
				}

				else if (source == modFont) {
					// Handle font chooser changes
					Font initialFont;
					Color outline = null;
					Color fill = null;
					if (settings.myLabelFont != null) {
						initialFont = settings.myLabelFont.getLabelFont();
					} else {
						initialFont = ThemeFont.getRegular();
					}
					
					if (settings.myLabelFont != null) {
						fill = settings.myLabelFont.getFontFillColor();
					} else {
						fill = settings.getLabelColor();
					}
					
					//dbl check - should not happen here
					if (settings.myLabelFont == null) {
						settings.myLabelFont = new MyLabelFont();
					}
					
					JFontChooser jfc = new JFontChooser(initialFont, outline, fill);
							
					if (null != jfc.showDialog(modFont)) {
						settings.myLabelFont.withLabelFont(jfc.selectedFont);
						settings.myLabelFont.withFontFillColor(jfc.getFontColor());
						settings.myLabelFont.withFontOutlineColor(jfc.getOutlineColor());
					}
					// Only repaint those whose lines are visible
					redrawMain = labelChkMain.isSelected();
					redrawPanner = labelChkPanner.isSelected();
					redraw3D = false; //until labels are drawn in 3D
				}

				// Based on what changed, trigger appropriate redraws
				if (redrawMain)
					redrawGrid();
				if (redrawPanner)
					((GridLView) getChild()).redrawGrid();
				if (redraw3D) {
					mylayer.increaseStateId(0);
					if (ThreeDManager.isReady()) {
						ThreeDManager.getInstance().updateDecalsForLView(GridLView.this, true);
					}
				}
				if (redrawMain || redrawPanner || redraw3D)
					copySettingsToParms(); // If changes have been made, we need to save them to the save/load param object
			}
		}

		private class GridPanel extends JPanel {
			JButton refresh;

			GridPanel() {
				majorSettings = new SettingsPanel("Major Lines", major);
				minorSettings = new SettingsPanel("Minor Lines", minor);
				setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

				JPanel settings = new JPanel();
				settings.setLayout(new GridLayout(1, 2));
				settings.add(majorSettings);
				settings.add(minorSettings);

				JPanel button = new JPanel();
				refresh = new JButton(refreshAct);
				button.add(refresh);

				add(settings);
				add(Box.createVerticalStrut(10));
				add(button);
				add(Box.createVerticalGlue());
			}

		}	
	
	Action refreshAct = new AbstractAction("Refresh View".toUpperCase()){
		public void actionPerformed(ActionEvent e) {
			try{
				// Check major spacing
				double majorSpacing;
				majorSpacing = Double.parseDouble(majorSettings.txtSpacing.getText().trim());
				if(90.0 / majorSpacing != Math.floor(90.0 / majorSpacing))
					throw new NumberFormatException();
				majorSettings.settings.setSpacing(majorSpacing);
				
				//Check minor spacing
				double minorSpacing;
				minorSpacing = Double.parseDouble(minorSettings.txtSpacing.getText().trim());
				if(90.0 / minorSpacing != Math.floor(90.0 / minorSpacing))
					throw new NumberFormatException();
				minorSettings.settings.setSpacing(minorSpacing);
				
			}catch(NumberFormatException nfe){
				// Handle problems by showing a dialog
				Toolkit.getDefaultToolkit().beep();
				Util.showMessageDialog(
					"Grid spacing value must be a number that divides " +
					"evenly into 90.",
					"Illegal value",
					JOptionPane.ERROR_MESSAGE);

				return;
			}
			// Determine what needs to be redrawn, based on this change
			boolean redrawMain = minorSettings.chkMain.isSelected() || majorSettings.chkMain.isSelected() || 
					minorSettings.labelChkMain.isSelected() || majorSettings.labelChkMain.isSelected();
			boolean redrawPanner = minorSettings.chkPanner.isSelected() || majorSettings.chkMain.isSelected() ||
					minorSettings.labelChkPanner.isSelected() || majorSettings.labelChkPanner.isSelected();
			
			// Based on what changed, trigger appropriate redraws
			if(redrawMain)
				redrawGrid();
			if(redrawPanner)
				((GridLView) getChild()).redrawGrid();
            if(redrawMain || redrawPanner)
                copySettingsToParms(); // If changes have been made, we need to save them to the save/load param object.
		}
	};

	public String getName()
	{
		return "Lat/Lon Grid";
	}

    private static void prepare(Graphics2D g2)
     {
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
							RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
							RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
     }
    
//The following two methods are used to query for the
// info panel fields (description, citation, etc)	
   	public String getLayerKey(){
   		return "Lat/Lon Grid";
   	}
   	public String getLayerType(){
   		return "llgrid";
   	}

	@Override
	public void update(Observable o, Object arg) {
		redrawGrid();
		if ((GridLView) getChild() != null) {
		    ((GridLView) getChild()).redrawGrid();
		}
	}
 }
