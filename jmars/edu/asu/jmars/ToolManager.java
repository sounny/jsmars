package edu.asu.jmars;

import java.awt.Color;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import edu.asu.jmars.swing.IconButtonUI;
import edu.asu.jmars.swing.LikeLabelButtonUI;
import edu.asu.jmars.tool.strategy.CommonToolStrategy;
import edu.asu.jmars.tool.strategy.InvestigateToolStrategy;
import edu.asu.jmars.tool.strategy.ProfileToolStrategy;
import edu.asu.jmars.tool.strategy.ShapesToolStrategy;
import edu.asu.jmars.tool.strategy.ToolStrategy;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.*;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.CustomBalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.java.balloontip.utils.TimingUtils;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;

public class ToolManager extends JPanel {

	private static List<ToolListener> listeners = new ArrayList<ToolListener>();
	private static Map<Integer, ToolStrategy> strategyMap = new HashMap<>();

	private static int Mode;
	private static int Prev;

	final public static int ZOOM_IN = 0;
	final public static int ZOOM_OUT = 1;
	final public static int MEASURE = 2;
	final public static int PAN_HAND = 3;
	final public static int SEL_HAND = 4;
	final public static int SUPER_SHAPE = 5;
	final public static int INVESTIGATE = 6;
	final public static int EXPORT = 7;
	final public static int RESIZE = 8;
	final public static int PROFILE = 9;
	final public static int SHAPES = 10;

	private ToolButton selHand;
	private ToolButton panHand;
	private ToolButton zoomIn;
	private ToolButton zoomOut;
	private ToolButton measure;
	private static ToolButton investigate;
	private ToolButton exportButton;
	private ToolButton resizeButton;
	private ToolButton profile;
	private static ToolButton shapes;
	
	private static CustomBalloonTip myBalloonTip;
	private static Color imgBlack = Color.BLACK;
	private static Icon close = new ImageIcon(
			ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgBlack)));
	private static JButton closebutton;

	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Color imgSelectedColor = ((ThemeImages) GUITheme.get("images")).getSelectedfill();

	private static Icon select = new ImageIcon(ImageFactory.createImage(MOUSE_POINTER_IMG.withDisplayColor(imgColor)));
	private static Icon selectSel = new ImageIcon(
			ImageFactory.createImage(MOUSE_POINTER_IMG_SEL.withDisplayColor(imgSelectedColor)));

	private static Icon magIn = new ImageIcon(ImageFactory.createImage(ZOOM_IN_IMG.withDisplayColor(imgColor)));
	private static Icon magInSel = new ImageIcon(
			ImageFactory.createImage(ZOOM_IN_IMG_SEL.withDisplayColor(imgSelectedColor)));
	private static Image zoom_in_c = ImageFactory.createImage(CURSOR_ZOOM_IN.withDisplayColor(imgColor));

	private static Icon magOut = new ImageIcon(ImageFactory.createImage(ZOOM_OUT_IMG.withDisplayColor(imgColor)));
	private static Icon magOutSel = new ImageIcon(
			ImageFactory.createImage(ZOOM_OUT_IMG_SEL.withDisplayColor(imgSelectedColor)));
	private static Image zoom_out_c = ImageFactory.createImage(CURSOR_ZOOM_OUT.withDisplayColor(imgColor));

	private static Icon ruler = new ImageIcon(ImageFactory.createImage(RULER_IMG.withDisplayColor(imgColor)));
	private static Icon rulerSel = new ImageIcon(
			ImageFactory.createImage(RULER_IMG_SEL.withDisplayColor(imgSelectedColor)));
	private static Image yard_stick_c = ImageFactory.createImage(CURSOR_RULER.withDisplayColor(imgColor));

	private static Icon hand = new ImageIcon(ImageFactory.createImage(PAN_HAND_IMG.withDisplayColor(imgColor)));
	private static Icon handSel = new ImageIcon(
			ImageFactory.createImage(PAN_HAND_IMG_SEL.withDisplayColor(imgSelectedColor)));
	private static Image pan_hand_c = ImageFactory.createImage(CURSOR_PAN.withDisplayColor(imgColor));

	private static Icon lookAt = new ImageIcon(ImageFactory.createImage(INVESTIGATE_IMG.withDisplayColor(imgColor)));
	private static Icon lookAtSel = new ImageIcon(
			ImageFactory.createImage(INVESTIGATE_IMG_SEL.withDisplayColor(imgSelectedColor)));
	private static Image look_at_c = ImageFactory.createImage(CURSOR_INVESTIGATE.withDisplayColor(imgColor));

	private static Icon exportIcon = new ImageIcon(ImageFactory.createImage(EXPORT_IMG.withDisplayColor(imgColor)));
	private static Icon exportIconSel = new ImageIcon(
			ImageFactory.createImage(EXPORT_IMG_SEL.withDisplayColor(imgSelectedColor)));
	private static Image export_c = ImageFactory.createImage(CURSOR_EXPORT.withDisplayColor(imgColor));

	private static Icon resizeIcon = new ImageIcon(ImageFactory.createImage(RESIZE_IMG.withDisplayColor(imgColor)));
	private static Icon resizeIconSel = new ImageIcon(
			ImageFactory.createImage(RESIZE_IMG_SEL.withDisplayColor(imgSelectedColor)));

	private static Image grab_hand = ImageFactory.createImage(CURSOR_PAN_GRAB.withDisplayColor(imgColor));

	private static Icon profileicon = new ImageIcon(ImageFactory.createImage(PROFILE_IMG.withStrokeColor(imgColor)));
	private static Icon profileSel = new ImageIcon(
			ImageFactory.createImage(PROFILE_IMG.withStrokeColor(imgSelectedColor)));
	private static Image profile_c = ImageFactory.createImage(PROFILE_PENCIL.withDisplayColor(imgColor));

	private static Icon shapesicon = new ImageIcon(
			ImageFactory.createImage(DP_SHAPESTOOLBAR.withDisplayColor(imgColor)));
	private static Icon shapesSel = new ImageIcon(
			ImageFactory.createImage(DP_SHAPESTOOLBAR.withDisplayColor(imgSelectedColor)));
	private static Image shapes_c = ImageFactory.createImage(PROFILE_PENCIL.withDisplayColor(imgColor));


	static {
		createCalloutUI();
	}

	public ToolManager() {
		createStrategyMap();
		createToolbarButtons();
		createToolbarUI();
		setupTooltips();
	}

	private void createToolbarButtons() {
		// creates cursors to be used in each mode
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Point hotSpot = new Point(0, 0);

		Cursor ZoomIn = toolkit.createCustomCursor(zoom_in_c, hotSpot, "zoom in");
		Cursor ZoomOut = toolkit.createCustomCursor(zoom_out_c, hotSpot, "zoom out");
		Cursor Measure = toolkit.createCustomCursor(yard_stick_c, hotSpot, "measure");
		Cursor PanHand = toolkit.createCustomCursor(pan_hand_c, hotSpot, "pan");
		Cursor Default = Cursor.getDefaultCursor();
		Cursor Investigate = toolkit.createCustomCursor(look_at_c, hotSpot, "investigate");
		Cursor Export = toolkit.createCustomCursor(export_c, new Point(10, 10), "export");
		Cursor Resize = toolkit.createCustomCursor(export_c, new Point(10, 10), "resize"); // same as export
		Dimension bestSize = Toolkit.getDefaultToolkit().getBestCursorSize(profile_c.getWidth(null),
				profile_c.getHeight(null));
		Cursor Profile_cursor = Toolkit.getDefaultToolkit().createCustomCursor(profile_c,
				new Point(0, bestSize.height * 4 / 5), "drawprofile");
		Cursor Shapes_cursor = Toolkit.getDefaultToolkit().createCustomCursor(shapes_c,
				new Point(0, bestSize.height * 4 / 5), "drawshapes");
		// creates buttons (and button dimensions) for the toolbar
		zoomIn = new ToolButton(ZOOM_IN, magIn, magInSel, ZoomIn);
		zoomOut = new ToolButton(ZOOM_OUT, magOut, magOutSel, ZoomOut);
		measure = new ToolButton(MEASURE, ruler, rulerSel, Measure);
		panHand = new ToolButton(PAN_HAND, hand, handSel, PanHand);
		selHand = new ToolButton(SEL_HAND, select, selectSel, Default);
		investigate = new ToolButton(INVESTIGATE, lookAt, lookAtSel, Investigate);
		exportButton = new ToolButton(EXPORT, exportIcon, exportIconSel, Export);
		resizeButton = new ToolButton(RESIZE, resizeIcon, resizeIconSel, Resize);
		profile = new ToolButton(PROFILE, profileicon, profileSel, Profile_cursor);
		shapes = new ToolButton(SHAPES, shapesicon, shapesSel, Shapes_cursor);
	}

	private void setupTooltips() {
		// sets tooltip texts
		String modifier = "";
		String shiftup = "\u21E7";
		String osver = System.getProperty("os.name").toLowerCase();
		modifier = (osver.indexOf("mac") != -1) ? "\u2318" : "Ctrl";
		String formatter = "   " + modifier + "+" + shiftup + "-";

		zoomIn.setToolTipText("Zoom In" + formatter + "I");
		zoomOut.setToolTipText("Zoom Out" + formatter + "O");
		measure.setToolTipText("Measure" + formatter + "M");
		panHand.setToolTipText("Pan" + formatter + "P");
		selHand.setToolTipText("Selection" + formatter + "D");
		investigate.setToolTipText("Investigate" + formatter + "C");
		exportButton.setToolTipText("Export" + formatter + "E");
		resizeButton.setToolTipText("Resize Main View" + formatter + "R");
		profile.setToolTipText("Draw Profile" + formatter + "Z");
		shapes.setToolTipText("Draw Shape" + formatter + "X");
	}

	private void createToolbarUI() {
		Dimension buttonDim = new Dimension(35, 25);
		Dimension gap = new Dimension(5, 0);

		// sets button sizes so they don't change when window changes
		zoomIn.setPreferredSize(buttonDim);
		zoomIn.setMaximumSize(buttonDim);
		zoomIn.setMinimumSize(buttonDim);
		zoomOut.setPreferredSize(buttonDim);
		zoomOut.setMaximumSize(buttonDim);
		zoomOut.setMinimumSize(buttonDim);
		measure.setPreferredSize(buttonDim);
		measure.setMaximumSize(buttonDim);
		measure.setMinimumSize(buttonDim);
		panHand.setPreferredSize(buttonDim);
		panHand.setMaximumSize(buttonDim);
		panHand.setMinimumSize(buttonDim);
		selHand.setPreferredSize(buttonDim);
		selHand.setMaximumSize(buttonDim);
		selHand.setMinimumSize(buttonDim);
		profile.setPreferredSize(buttonDim);
		profile.setMaximumSize(buttonDim);
		profile.setMinimumSize(buttonDim);
		shapes.setPreferredSize(buttonDim);
		shapes.setMaximumSize(buttonDim);
		shapes.setMinimumSize(buttonDim);

		investigate.setPreferredSize(buttonDim);
		investigate.setMaximumSize(buttonDim);
		investigate.setMinimumSize(buttonDim);
		exportButton.setPreferredSize(buttonDim);
		exportButton.setMaximumSize(buttonDim);
		exportButton.setMinimumSize(buttonDim);
		resizeButton.setPreferredSize(buttonDim);
		resizeButton.setMaximumSize(buttonDim);
		resizeButton.setMinimumSize(buttonDim);

		// adds buttons to ToolManager JPanel
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		add(Box.createRigidArea(new Dimension(74, 0)));
		add(selHand);
		//add(Box.createRigidArea(gap));
		
		add(shapes);
		add(Box.createRigidArea(gap));
		 
		add(profile);
		add(Box.createRigidArea(gap));

		add(investigate);
		add(Box.createRigidArea(gap));

		add(measure);
		add(Box.createRigidArea(gap));

		add(exportButton);
		add(Box.createRigidArea(gap));

		add(Box.createRigidArea(gap));
		add(Box.createRigidArea(gap));
		add(Box.createRigidArea(gap));

		add(panHand);
		add(Box.createRigidArea(gap));

		add(resizeButton);
		add(Box.createRigidArea(gap));

		add(zoomIn);
		add(Box.createRigidArea(gap));

		add(zoomOut);
		add(Box.createRigidArea(gap));
		add(Box.createRigidArea(gap));
	}

	private void createStrategyMap() {
		strategyMap.put(ToolManager.SEL_HAND, new CommonToolStrategy());
		strategyMap.put(ToolManager.PROFILE, new ProfileToolStrategy());
		strategyMap.put(ToolManager.SHAPES, new ShapesToolStrategy());
		strategyMap.put(ToolManager.INVESTIGATE, new InvestigateToolStrategy());
	}

	private static void createCalloutUI() {
		JLabel dummy = new JLabel();
		EdgedBalloonStyle style = new EdgedBalloonStyle(ThemeSnackBar.getBackgroundStandard(),
				ThemeProvider.getInstance().getBackground().getBorder());
		BalloonTip.setDefaultCloseButtonIcons(close, close, close);
		myBalloonTip = new CustomBalloonTip(dummy, dummy, new Rectangle(), style, BalloonTip.Orientation.LEFT_BELOW,
				BalloonTip.AttachLocation.CENTER, 10, 20, true);
		myBalloonTip.setPadding(5);
		closebutton = BalloonTip.getDefaultCloseButton();
		closebutton.setUI(new LikeLabelButtonUI());
		myBalloonTip.setCloseButton(closebutton, false);
		myBalloonTip.setVisible(false);
	}

	public static void showCallout(Container parent2, String msg, int time) {
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
						+ msg + "</b>" + "</p></html>";
				myBalloonTip.setTextContents(html);
				myBalloonTip.setOffset(rectoffset);
				TimingUtils.showTimedBalloon(myBalloonTip, time); // callout disappears in 'time' sec
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

	// Allows the buttons to change the tool mode and respond to that.
	private class ToolButton extends JButton implements ToolListener {
		private int myMode;
		private Cursor myCursor;
		private Icon img;
		private Icon selImg;

		ToolButton(int tmode, Icon magIn, Icon magInSel, Cursor csr) {
			super(magIn);
			img = magIn;
			selImg = magInSel;
			myMode = tmode;
			myCursor = csr;

			addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					ToolManager.setToolMode(myMode);
				}
			});
			ToolManager.addToolListener(this);
			setBorder(new EmptyBorder(0, 8, 0, 8));
			setUI(new IconButtonUI());
		}

		public void toolChanged(int newMode) {
			if (newMode == myMode) {
				setIcon(selImg);
				if (Main.testDriver != null) {
					Main.testDriver.mainWindow.setCursor(myCursor);
				}
			} else {
				setIcon(img);
				setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}

	public static void addToolListener(ToolListener tl) {
		listeners.add(tl);
	}

	public static boolean removeToolListener(ToolListener tl) {
		return listeners.remove(tl);
	}

	public static void notifyToolListeners() {
		for (ToolListener tl : listeners)
			tl.toolChanged(Mode);
	}

	public static void setToolMode(int newMode) {
		Prev = Mode;
		Mode = newMode;

		for (Integer toolmode : strategyMap.keySet()) {
			ToolStrategy ts = strategyMap.get(toolmode);
			ts.preMode(Mode, Prev);
		}
		
		ToolStrategy strategy = strategyMap.get(Mode);
		if (strategy != null) {
			strategy.doMode(Mode, Prev);
		} 
		
		for (Integer toolmode : strategyMap.keySet()) {
			ToolStrategy ts = strategyMap.get(toolmode);
			ts.postMode(Mode, Prev);
		}
		notifyToolListeners();
	}


	public static int getToolMode() {
		return Mode;
	}

	public static int getPrevMode() {
		return Prev;
	}

	public static void setGrabHand() {
		Cursor gh = Toolkit.getDefaultToolkit().createCustomCursor(grab_hand, new Point(0, 0), "pandrag");
		Main.testDriver.mainWindow.setCursor(gh);
	}

	public JButton getProfileToolComponent() {
		return profile;
	}

	public JButton getShapesToolComponent() {
		return shapes;
	}
}
