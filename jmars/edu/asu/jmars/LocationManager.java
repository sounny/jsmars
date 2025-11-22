package edu.asu.jmars;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;
import edu.asu.jmars.swing.snackbar.SnackBarBuilder;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import edu.asu.jmars.layer.ProjectionEvent;
import edu.asu.jmars.layer.ProjectionListener;
import edu.asu.jmars.parsers.ParseException;
import edu.asu.jmars.parsers.gis.CoordinatesParser;
import edu.asu.jmars.parsers.gis.CoordinatesParser.LatitudeSystem;
import edu.asu.jmars.parsers.gis.CoordinatesParser.Ordering;
import edu.asu.jmars.swing.IconButtonUI;
import edu.asu.jmars.swing.PasteField;
import edu.asu.jmars.swing.landmark.search.LandmarkSearchPanel;
import edu.asu.jmars.swing.landmark.search.swing.LatLonBox;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import mdlaf.utils.MaterialImageFactory;
import mdlaf.utils.icons.MaterialIconFont;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.RIGHT_ARROW_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.CROSSHAIRS;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.LEFT_ARROW_IMG;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.SEARCH2;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.SEARCH2_SEL;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.WordUtils;
import org.material.component.swingsnackbar.SnackBar;
import org.material.component.swingsnackbar.action.AbstractSnackBarAction;

public final class LocationManager extends JPanel implements Observer, ProjectionListener {
	public static final String actionCommandSetLocation = "set location";
	private static final String prompt = "Search landmarks or bookmarked places";
	private static final DecimalFormat f = new DecimalFormat("0.###");
	private static DebugLog log = DebugLog.instance();
	public JButton backPlaceBtn; // previous place
	public JButton forwardPlaceBtn; // next place
	private JTextField locInputField = null;
	private LatLonBox locInputFieldControl = null;
	private final Point2D loc = new Point2D.Double(); // default location
	private List<LocationListener> listeners = new ArrayList<LocationListener>(); // list of location listeners
	private String coordOrdering = Ordering.LAT_LON.asString();
	private static Ordering ordering = Ordering.LAT_LON;
	private final char defaultLat = 'N';
	private char defaultLon = 'E';
	private SnackBar snackBar = null;
	private final static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Color imgGrayedColor = ThemeProvider.getInstance().getText().getGrayed();
	private static Icon ellipse = new ImageIcon( ImageFactory.createImage(ImageCatalogItem.ELLIPSE_MENU.withDisplayColor(imgColor)));           
	private static Icon crosshairs = new ImageIcon(ImageFactory.createImage(CROSSHAIRS.withDisplayColor(imgGrayedColor)));	
	Icon closeicon = MaterialImageFactory.getInstance().getImage(MaterialIconFont.CLOSE,UIManager.getColor("SnackBar.foreground"));			
	private static final UUID uuid = UUID.randomUUID();

	/**
	 * Public access point for externally setting the location.
	 *
	 * @param newLoc    The new location point in WORLD COORDINATES, which will be
	 *                  reflected in the input field.
	 * @param propagate If true, the location change will be propagated to all
	 *                  location listeners; if false, the change will not be
	 *                  propagated.
	 */
	public void setLocation(Point2D newLoc, boolean propagate) {
		// set the new location
		loc.setLocation(newLoc);

		refreshLocationString();
		log.println("External location set: " + loc);

		if (propagate) {
			// dispatch a location and zoom update message to every LocationListener
			for (LocationListener ll : listeners) {
				ll.locationChanged(new Point2D.Double(loc.getX(), loc.getY()));
			}

			log.println("propagateLocationAndZoom(): new location+zoom propagated to " + listeners.size()
					+ " LocationListeners.");
		}
	}

	private static String getFormattedTooltipText() {
		String tooltiptext = "<html>&nbsp;Enter coordinates to update center " + "<br/>" 
	                        + "&nbsp;of view. Click magnifying glass to <br/>" +
				            "&nbsp;search landmarks.</html>";
		return tooltiptext;
	}

	/**
	 * Replaces whatever current text is in the location textbox with the
	 * programmatically-generated one.
	 */
	public void refreshLocationString() {
		locInputField.setText(getLocString());
	}

	private void refreshLocationLabel() {
		locInputFieldControl.setPlaceholderText(ordering.getOrderingLabel());
	}

	private String getLocString() {
		return worldToText(loc);
	}

	/* Return the location in WORLD coordinates */
	public Point2D getLoc() {
		return new Point2D.Double(loc.getX(), loc.getY());
	}

	/**
	 * Attempts to initiate a reprojection based on the current contents of the
	 * location text field.
	 */
	public void reprojectFromText() {
		Point2D newWorld = reprojectFromText(locInputField.getText());
		log.println(newWorld);
		if (newWorld != null)
			setLocation(newWorld, true);
	}

	public void resetProjection() {
		Point2D resetWorld = reproject(new Point2D.Double(0,0));
		log.println(resetWorld);
		if (resetWorld != null) {
			setLocation(resetWorld, true);
		}
	}
	public LocationManager(Point2D initialLocation) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		log.println("initializing(): " + initialLocation);
		loc.setLocation(initialLocation);
		coordOrdering = Config.get(Config.CONFIG_LAT_LON, Ordering.LAT_LON.asString());
		ordering = Ordering.get(coordOrdering);
		/* create back and forward buttons for location */
		backPlaceBtn = new PlaceButton(PlaceButton.BACK);
		backPlaceBtn.setToolTipText("Click to go to Previous Place");
		forwardPlaceBtn = new PlaceButton(PlaceButton.FORWARD);
		forwardPlaceBtn.setToolTipText("Click to go to Next Place");
		add(backPlaceBtn);
		add(forwardPlaceBtn);
		String placeholdertext = ordering.getOrderingLabel();
		/* create the label for the location input field */
		locInputFieldControl = new LatLonBox(new PasteField(getLocString(), 15), crosshairs, ellipse);
		locInputFieldControl.setMaximumSize(new Dimension(350, 33));
		locInputFieldControl.setMinimumSize(new Dimension(220, 33));
		Icon search = new ImageIcon(ImageFactory.createImage(SEARCH2));
		Icon searchSelected = new ImageIcon(ImageFactory.createImage(SEARCH2_SEL));
		locInputFieldControl.setIcon(search).setSelectedIcon(searchSelected).setPlaceholderText(placeholdertext).setPlaceholderTextColor(imgGrayedColor);						
		locInputFieldControl.getIconContainer().addActionListener(new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LandmarkSearchPanel.showHideSearchInput(locInputFieldControl, e);				
			}
		});
		locInputFieldControl.addTooltip(getFormattedTip());
		add(locInputFieldControl);

		locInputField = this.locInputFieldControl.getTextFiled();
		locInputField.setToolTipText(getFormattedTooltipText());
		locInputField.setActionCommand(actionCommandSetLocation);
		locInputField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Main.setStatus(null);
				// read input from locInputField and propagate it
				readInputLoc();
			}
		});
		add(Box.createHorizontalGlue());
	}

	public void updateBookmarkIcon(boolean visible) {
		locInputFieldControl.getIconContainer().setSelected(visible);
	}

	/**
	 ** Returns the location bar's raw value (whether it's been submitted by the user
	 * or not).
	 **
	 ** @return null on error
	 **/
	private Point2D getWorldPt() {
		return textToWorld(locInputField.getText());
	}

	// parse location input within the locInputField and propagate it
	protected void readInputLoc() {
		Point2D world = getWorldPt();
		if (world == null)
			return;

		setLocation(world, true);
	}

	/** Register a LocationListener with this LocationManager. */
	public void addLocationListener(LocationListener ll) {
		listeners.add(ll);
	}

	public boolean removeLocationListener(LocationListener ll) {
		return listeners.remove((Object) ll);
	}

	public String worldToText(Point2D worldPt) {
		Point2D spatial = Main.PO.convWorldToSpatial(worldPt);
		return ordering.format(spatial);
	}

	public Point2D textToWorld(String text) {
		Point2D pt = textToSpatial(text);
		if (pt != null)
			pt = Main.PO.convSpatialToWorld(pt);
		return pt;
	}

	private void setOrdering(CoordinatesParser.Ordering order) {
		ordering = order;
		refreshLocationString();
		refreshLocationLabel();
		updateCenterProjection();
	}

	private Point2D textToSpatial(String text) {
		CoordinatesParser parser = new CoordinatesParser(defaultLat, defaultLon, ordering);
		Pair<Pair<Double, Character>, Pair<Double, Character>> result = null;
		final Pair<Double, Character> p1;
		final Pair<Double, Character> p2;
		boolean orderSwitched = false;
		
		if (text.isEmpty())
			return (null);
		
		try {
			result  = parser.parse(text.trim()).value;
		} catch (ParseException ex) {
			//if here user may've entered correct suffix but opposite to Ordering, so let's try to parse with different order
			Ordering ordering2 = ordering.equals(CoordinatesParser.Ordering.LON_LAT)
					? CoordinatesParser.Ordering.LAT_LON
					: CoordinatesParser.Ordering.LON_LAT;
			parser = new CoordinatesParser(defaultLat, defaultLon, ordering2);
			try {
				result  = parser.parse(text.trim()).value;
				orderSwitched = true;
			} catch (ParseException e) {
				//if here then failed
				Toolkit.getDefaultToolkit().beep();
				String message = "Input error: Provided coordinates are not valid";
				runSnackBar(message, isMessageErrorChanged(message)); 
				return null; 				
			}			
		}
		
		if (!orderSwitched) {
			p1 = ordering.equals(CoordinatesParser.Ordering.LON_LAT) ? result.getLeft() : result.getRight();
		} else {
			p1 = ordering.equals(CoordinatesParser.Ordering.LON_LAT) ? result.getRight() : result.getLeft();
		}

		if (!orderSwitched) {
			p2 = ordering.equals(CoordinatesParser.Ordering.LON_LAT) ? result.getRight() : result.getLeft();
		} else {
			p2 = ordering.equals(CoordinatesParser.Ordering.LON_LAT) ? result.getLeft() : result.getRight();
		}

		double x = p1.getLeft();
		if (p1.getRight() == 'E' || p1.getRight() == 'e') {
			x = (360 - (x % 360.)) % 360.; // Convert to west leading coordinates for internal use
		}

		double y = p2.getLeft();
		String latSystemstr = Config.get(Config.CONFIG_LAT_SYSTEM, LatitudeSystem.OCENTRIC.getName());
		if (LatitudeSystem.PLANETOGRAPHIC.equalsIgnoreCase(latSystemstr)) {
			y = Util.ographic2ocentric(y); //convert to Ocentric for internal use
		}
		if (p2.getRight() == 'S' || p2.getRight() == 's') {
			y = -y;
		}

		if (Math.abs(y) > 90) {
			Toolkit.getDefaultToolkit().beep();
			String message = "Input error: Latitude may not exceed +/-90";
			runSnackBar(message, isMessageErrorChanged(message));
			return null;
		}
		if (snackBar != null && snackBar.isRunning()) {
			snackBar.dismiss();
		}
		return new Point2D.Double(x, y);       
	} 

	public Point2D reprojectFromText(String rawText) {
		Point2D ctrLonLat = textToSpatial(rawText);
		return reproject(ctrLonLat);
	}
	public Point2D reproject(Point2D centerLonLat) {
		if (centerLonLat == null)
			return null;
		ProjObj po = new ProjObj.Projection_OC(centerLonLat.getX(), centerLonLat.getY());
		return reproject(centerLonLat, po);
	}
	public Point2D reproject(Point2D centerLonLat, ProjObj po) {
		Main.setProjection(po);
		Main.setTitle(null, ordering.format(centerLonLat));
		Point2D newWorldPt = po.convSpatialToWorld(centerLonLat);
		return newWorldPt;
	}
	private void updateCenterProjection() {
		Ordering newOrdering = ordering;
		Ordering oldOrdering = newOrdering.equals(Ordering.LAT_LON) ? Ordering.LON_LAT : Ordering.LAT_LON;
		String center = Main.testDriver.centerOfProj.getText();
		ordering = oldOrdering;
		Point2D ctrLonLat = textToSpatial(center);
		if (ctrLonLat == null)
			return;
		ordering = newOrdering;
		Main.setCenterProjection(ordering.format(ctrLonLat));
	}

	private class PlaceButton extends JButton {
		static final int BACK = 1;
		static final int FORWARD = 2;
		final Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
		final Color disabledtext = ((ThemeText) GUITheme.get("text")).getTextDisabled();
		final Icon backImg = new ImageIcon(ImageFactory.createImage(LEFT_ARROW_IMG.withDisplayColor(imgColor)));
		Icon forwardImg = new ImageIcon(ImageFactory.createImage(RIGHT_ARROW_IMG.withDisplayColor(imgColor)));
		Icon backDisabled = new ImageIcon(ImageFactory.createImage(LEFT_ARROW_IMG.withDisplayColor(disabledtext)));
		Icon forwardDisabled = new ImageIcon(ImageFactory.createImage(RIGHT_ARROW_IMG.withDisplayColor(disabledtext)));

		private PlaceButton(int mode) {
			super();
			if (mode == BACK) {
				addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Main.places.getPlaceHistory().back();
					}
				});
				setIcon(backImg);
				setDisabledIcon(backDisabled);
			} else if (mode == FORWARD) {
				addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Main.places.getPlaceHistory().forward();
					}
				});
				setIcon(forwardImg);
				setDisabledIcon(forwardDisabled);
			}

			setUI(new IconButtonUI());
		}
	}

	public boolean isMessageErrorChanged(String message) {
		return (snackBar != null && !snackBar.getText().equals(message));
	}

	private void runSnackBar(String message, boolean messageChanged) {
		int gap = message.length();
		if (snackBar == null) {
			snackBar = SnackBarBuilder.build(Main.mainFrame, message, closeicon, uuid)
					.setSnackBarBackground(ThemeSnackBar.getBackgroundError())
					.setSnackBarForeground(ThemeSnackBar.getForegroundError()).setDuration(SnackBar.LENGTH_LONG)
					.setPosition(SnackBar.BOTTOM).setMarginBottom(150).setGap(gap)
					.setAction(new AbstractSnackBarAction() {
						@Override
						public void mousePressed(MouseEvent e) {
							SnackBarBuilder.getSnackBarOn(uuid).dismiss();
						}
					});
			snackBar.setFocusable(false);
		}
			if (messageChanged) {
				snackBar = SnackBarBuilder.build(Main.mainFrame, message, closeicon, uuid);
				snackBar.refresh().run();
			} else if (!snackBar.isRunning()) {
				snackBar.refresh().run();
			}
			
		Window parentwindowforsnackbar = SwingUtilities.windowForComponent(snackBar);
		if (parentwindowforsnackbar != null) {
			parentwindowforsnackbar.addWindowFocusListener(new WindowAdapter() {
				public void windowGainedFocus(WindowEvent e) {					
				}
				public void windowLostFocus(WindowEvent e) {
					if (snackBar != null && (snackBar.isRunning())) {
						snackBar.dismiss();
					}
				}
			});
		}  		
	}

	@Override
	public void update(Observable o, Object data) {
        if (!(data instanceof Ordering)) {
        	setOrdering(ordering);
        } else {        
		    setOrdering((Ordering) data);
        }
	}

	@Override
	public void projectionChanged(ProjectionEvent e) {
		// update UI for center of projection display
		ProjObj proj = Main.PO;
		Main.setCenterProjection(ordering.format(proj.getProjectionCenter()));
	}

	public void resetSearchlandmarkDialog() {
		LandmarkSearchPanel.setRelativeToOnScreenComponent(this.locInputFieldControl);
		LandmarkSearchPanel.resetSearchLandmarkDialogLocationOnScreen();
		resetLatLonErrorMsgSnackbar();
	}

	private void resetLatLonErrorMsgSnackbar() {
		if (snackBar != null && snackBar.isRunning()) {
			int x = (snackBar.getOwner().getX() + ((snackBar.getOwner().getWidth() - snackBar.getWidth()) / 2));
			int y = (snackBar.getOwner().getY() + snackBar.getOwner().getHeight() - snackBar.getHeight() - snackBar.getMarginBottom());
			Point point = new Point(x, y);
			snackBar.setLocation(point);
		}
	}

	private static String getFormattedTip() {
		String infohtml = "<html><p style=\"border:2px solid #3b3e45; padding:5px; margin:-6;\">&nbsp;";		
		String wrap = WordUtils.wrap(prompt, 30, "&nbsp;<br>&nbsp;", false);
		infohtml += "&nbsp;" + wrap + "&nbsp;</p></html>";		
		return infohtml;
	}

	public Pair<Point2D, String> parse(String newPointStr) {
		Point2D pt = null;
		String errorMsg = null;
		pt = textToSpatial(newPointStr);
		if (snackBar != null) {
			errorMsg = snackBar.getText();
		}
		return (new ImmutablePair<Point2D, String>(pt, errorMsg));
	}

	public void initSearch() {
		if (locInputFieldControl == null) return;
		if (!(locInputFieldControl.getIconContainer() instanceof AbstractButton)) return;
		AbstractButton btn = locInputFieldControl.getIconContainer();
		btn.setSelected(false); //toggle to OFF, so that doClick() toggles it to ON
		btn.doClick();
	}	
}
