package edu.asu.jmars.layer.shape2.drawingpalette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observable;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.MultiBorderLayout;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.ShapeActionObserver;
import edu.asu.jmars.Main;
import edu.asu.jmars.ToolManager;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LManager.ActiveViewChangedObservable;
import edu.asu.jmars.layer.Layer;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.layer.shape2.ShapeLView;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.DrawAction;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.DrawActionEnum;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.FontSizeHeaderLabel;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.TextUnderImageLabel;
import edu.asu.jmars.layer.shape2.drawingpalette.swing.ViewChangedObserver;
import edu.asu.jmars.layer.util.features.FeatureMouseHandler.ShapeActionData;
import edu.asu.jmars.layer.util.features.FeatureMouseHandler.ShapeActionObservable;
import edu.asu.jmars.swing.LikeLabelButtonUI;
import edu.asu.jmars.tool.strategy.ShapesToolStrategy;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeButton;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import mdlaf.animation.MaterialUIMovement;
import mdlaf.components.radiobutton.MaterialRadioButtonUI;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.CustomBalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.java.balloontip.utils.TimingUtils;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JDialog;


public enum DrawingPalette implements ShapeActionObserver, ViewChangedObserver, ComponentListener {

	INSTANCE;

	private static Color imgColor;
	private static Color imgSelectedColor;
	private static Color bordercolor;

	// select icon
	public static Icon iselect, iselectSel;
	
	// draw icons
	public  static Icon irectangle, iselrectangle, idisrectangle;
	public  static Icon icircle, iselcircle;
	public  static Icon iellipse, iselellipse, i5ptellipse, isel5ptellipse;
	public  static Icon ipoint, iselpoint;
	public  static Icon iline, iselline;
	public  static Icon ipoly, iselpoly;
	public  static Icon ifreehand, iselfreehand;
	
	// combine icons
	private static Icon imerge;
	private static Icon isubtract;
	private static Icon iintersect;

	// order
	private static Icon iforward;
	private static Icon ibackward;
	private static Icon ifront;
	private static Icon iback;

	private static Icon dotmenu;

	private static Border lineseparator;

	// drawing buttons
	private JRadioButton rbrectangle, rbcircle, rbellipse5, rbline, rbpoint, rbfreehand, rbpoly, rbellipse;
	private JRadioButton rbselect;
	private ButtonGroup drawgroup = new ButtonGroup();
	
	private JLabel currentShapeLayerLbl;
	
	private JDialog drawingPaletteFrame = null;
	private JPanel contentPane;
	private JPanel headerpanel, drawpanel, editorpanel, combinepanel, orderpanel, layersdropdownpanel;
	private Map<DrawActionEnum, AbstractButton> drawActions = new HashMap<>();
	
	private int screenX, screenY;
	private static CustomBalloonTip myBalloonTip;
	private static Color imgBlack = Color.BLACK;
	private static Icon close = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgBlack)));
	private static JButton closebutton;
	
	private static final Integer SOME_KEY = 1;
	private static final Integer FIRST_LOAD_KEY = 11;
	
	private static Map<Integer, Integer> userClosedPalette = new HashMap<>(); //closed once, applies to ALL
	private static Map<Integer, Integer> firstLoad = new HashMap<>();
	private static Map<Layer.LView, AbstractButton> drawActionPerView = new HashMap<>();
	private Map<AbstractButton, MySelectionLabel> selectionLabelMap = new HashMap<>();
	
	static {
		createCalloutUI();
		firstLoad.put(FIRST_LOAD_KEY,  1);
		loadGraphics();
	}
	
	private DrawingPalette() {}
	
	public void init() {		
	}

	public void show(LView view) {
		if (drawingPaletteFrame == null) {
			drawingPaletteFrame = new JDialog(Main.mainFrame, false);
			initPaletteDialog();
		}		
		if (firstLoad.containsKey(FIRST_LOAD_KEY) && 
				ToolManager.getToolMode() != ToolManager.SHAPES) {
			return;
		}
		if (!userClosedPalette.containsKey(SOME_KEY)) {
			if (drawingPaletteFrame != null) {
				if (!drawingPaletteFrame.isVisible()) {
					if (screenX != -1 && screenY != -1) {
						drawingPaletteFrame.setLocation(screenX, screenY);
					} else {
						calculateLocationAtTopRight();
					}
					hideCallout();
					if (isCustomShapeInstance(view)) {
						((ShapeLView) view).hideCallout();
					}
				}
				enableOrDisableDrawTools(view);
				if (!drawingPaletteFrame.isVisible()) {
				    drawingPaletteFrame.setVisible(true);
				}
				updateDrawActionPerView(view);
			}
		}
	}
	
	private void initPaletteDialog() {
		drawingPaletteFrame.setSize(375, 130);//450 when ellipse added in
		drawingPaletteFrame.setResizable(true);
		drawingPaletteFrame.setTitle("Custom Shape Editor");
		drawingPaletteFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		// contentPane.setBackground(Color.BLACK); // for standalone testing
		drawingPaletteFrame.setContentPane(contentPane);
		screenX = -1;
		screenY = -1;
		createUI();
		LManager.getLManager().getActiveViewObservable().addObserver(this);
		drawingPaletteFrame.addComponentListener(this);
		drawingPaletteFrame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				JButton shapesTool = Main.testDriver.toolMgr.getShapesToolComponent();
				if (isCalloutVisible()) {
					hideCallout();
				}
				userClosedPalette.put(SOME_KEY, 1); // user closed the palette - remember that but prompt how
													// to reopen
				String msg = (ToolManager.getToolMode() == ToolManager.SHAPES) ? UserPrompt.ACTIVATE_PALETTE.asString()
						: UserPrompt.ACTIVATE_PALETTE_2.asString();
				showCallout(shapesTool, msg, UserPrompt.SHORT_TIME);
			}
		});

	}

	public boolean isPaletteVisible() {
		return (drawingPaletteFrame != null && drawingPaletteFrame.isVisible());
	}	
	
	private void enableOrDisableDrawTools(LView view) {
		AbstractButton currentdrawingselection = drawActionPerView.get(view);
		if (currentdrawingselection == rbselect) {
			rbselect.setSelected(true); //Select mode
			deselectAllDraw();
		} else {
			rbselect.setSelected(false); //Draw mode
			enableDraw();
		}
	}

	public void enableDraw() {
		Enumeration<AbstractButton> enumeration = drawgroup.getElements();
		while (enumeration.hasMoreElements()) {
			JRadioButton btn = (JRadioButton) enumeration.nextElement();
			btn.setEnabled(true);
		}
	}

	public void deselectAllDraw() {
		Enumeration<AbstractButton> enumeration = drawgroup.getElements();
		while (enumeration.hasMoreElements()) {
			JRadioButton btn = ((JRadioButton) enumeration.nextElement());
			btn.setSelected(false);
			resetButtonLabel(btn, false); //set plain text
		}
		drawgroup.clearSelection();
	}

	private void resetButtonLabel(AbstractButton btn, boolean selectedtext) {
		MySelectionLabel mylbl;
		if (selectionLabelMap != null) {
			mylbl = selectionLabelMap.get(btn);
			if (mylbl != null) {
				if (selectedtext == true) {
					mylbl.setSelectedtext();
					// deselect all others
					for (MySelectionLabel my : selectionLabelMap.values()) {
						if (my == mylbl) continue;
						my.setPlaintext();
					}
				} else {
					mylbl.setPlaintext();
				}
			}
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
		

	private void calculateLocationAtTopRight() {
		int margin = 25;
		JPanel owner = Main.testDriver.toolMgr;
		Double dx = owner.getLocationOnScreen().getX();
		Double dy = owner.getLocationOnScreen().getY();
		int x = dx.intValue() + owner.getWidth() / 2 + margin;
		int y = dy.intValue() + owner.getHeight() + margin;
		screenX = x;
		screenY = y;
		drawingPaletteFrame.setLocation(x, y);
	} 

	public void hide() {
		if (drawingPaletteFrame != null && drawingPaletteFrame.isVisible()) {
			drawingPaletteFrame.setVisible(false);
		}
		hideCallout();
	}
	
	
	private void createUI() {
		editorpanel = new JPanel();
	    // editorpanel.setBackground(Color.BLACK); // for standalone testing
		editorpanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		editorpanel.setLayout(new MultiBorderLayout(0, 0));

		buildDrawPanel();
	
		editorpanel.add(drawpanel, BorderLayout.NORTH);
		contentPane.add(editorpanel, BorderLayout.CENTER);
	}

	private void buildOrderPanel() {
		// ORDER
		orderpanel = new JPanel();
		//orderpanel.setBackground(Color.BLACK); // for standalone testing
		orderpanel.setLayout(new BorderLayout(0, 0));
		orderpanel.setBorder(BorderFactory.createEmptyBorder(20, 5, 20, 5));

		JLabel orderLbl = new FontSizeHeaderLabel("ORDER", Font.BOLD, 11);
		orderLbl.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		orderpanel.add(orderLbl, BorderLayout.NORTH);

		JPanel ordericonspanel = new JPanel();
	    //ordericonspanel.setBackground(Color.BLACK); // for standalone testing
		ordericonspanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
		ordericonspanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));

		JLabel forwardLbl = new TextUnderImageLabel("FORWARD");
		forwardLbl.setIcon(iforward);
		ordericonspanel.add(forwardLbl);

		JLabel backwardsLbl = new TextUnderImageLabel("BACKWARD");
		backwardsLbl.setIcon(ibackward);
		ordericonspanel.add(backwardsLbl);

		JLabel tofrontLbl = new TextUnderImageLabel("TO FRONT");
		tofrontLbl.setIcon(ifront);
		ordericonspanel.add(tofrontLbl);

		JLabel tobackLbl = new TextUnderImageLabel("TO BACK");
		tobackLbl.setIcon(iback);
		ordericonspanel.add(tobackLbl);
		orderpanel.add(ordericonspanel, BorderLayout.CENTER);
	}

	private void buildCombinePanel() {
		// COMBINE
		combinepanel = new JPanel();
		//combinepanel.setBackground(Color.BLACK); // for standalone testing
		combinepanel.setLayout(new BorderLayout(0, 0));
		combinepanel.setBorder(lineseparator);
		JLabel combineLbl = new FontSizeHeaderLabel("COMBINE", Font.BOLD, 11);
		combineLbl.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		combinepanel.add(combineLbl, BorderLayout.NORTH);

		JPanel combineiconspanel = new JPanel();
		//combineiconspanel.setBackground(Color.BLACK); // for standalone testing
		combineiconspanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 5, 0));
		combineiconspanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));

		JLabel mergeLbl = new TextUnderImageLabel("MERGE");
		mergeLbl.setIcon(imerge);
		combineiconspanel.add(mergeLbl);

		JLabel subtractLbl = new TextUnderImageLabel("SUBTRACT");
		subtractLbl.setIcon(isubtract);
		combineiconspanel.add(subtractLbl);

		JLabel intersectLbl = new TextUnderImageLabel("INTERSECT");
		intersectLbl.setIcon(iintersect);
		combineiconspanel.add(intersectLbl);

		combinepanel.add(combineiconspanel, BorderLayout.CENTER);
	}

	private void buildDrawPanel() {
		// draw tools
		layersdropdownpanel = new JPanel();
		//layersdropdownpanel.setBackground(Color.BLACK); //// for standalone testing
		layersdropdownpanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 20, 5));
		layersdropdownpanel.setSize(350, 82);

		layersdropdownpanel.setLayout(new BorderLayout(0, 0));

		JLabel lblNewLabel = new FontSizeHeaderLabel("ACTIVE SHAPE LAYER", Font.PLAIN, 9);
		lblNewLabel.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		layersdropdownpanel.add(lblNewLabel, BorderLayout.NORTH);

		currentShapeLayerLbl = new JLabel();
		layersdropdownpanel.add(currentShapeLayerLbl, BorderLayout.SOUTH);

		// DRAW
		drawpanel = new JPanel();
		//drawpanel.setBackground(Color.BLACK); // for standalone testing
		drawpanel.setLayout(new BorderLayout(0, 0));
		//drawpanel.setBorder(lineseparator);  for Phase > 0
		drawpanel.setBorder(BorderFactory.createEmptyBorder(2,5,2,5));  
		JLabel drawLbl = new FontSizeHeaderLabel("DRAW", Font.BOLD, 14);
		drawLbl.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		drawpanel.add(drawLbl, BorderLayout.NORTH);

		JPanel drawiconspanel = new JPanel();
		//drawiconspanel.setBackground(Color.BLACK); // for standalone testing
		drawiconspanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 2, 0));
		drawiconspanel.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 0));
					
		rbselect = new JRadioButton();
		rbselect.setIcon(iselect);  //draw
		rbselect.setSelectedIcon(iselectSel);  //select
		rbselect.setAction(new DrawAction(rbselect, DrawActionEnum.SELECT.asString()));
		drawActions.put(DrawActionEnum.SELECT, rbselect);
			
		MySelectionLabel label1 = new MySelectionLabel("point");
		JPanel inner1 = new JPanel();
		inner1.setLayout(new BorderLayout());
		rbpoint = new JRadioButton();
		rbpoint.setUI(new MyRadioButtonUI());
		rbpoint.setIcon(ipoint);
		rbpoint.setSelectedIcon(iselpoint);
		rbpoint.setAction(new DrawAction(rbpoint, DrawActionEnum.POINT.asString()));
		drawActions.put(DrawActionEnum.POINT, rbpoint);
		drawgroup.add(rbpoint);
		selectionLabelMap.put(rbpoint, label1);
		inner1.add(rbpoint, BorderLayout.NORTH);
		inner1.add(label1.getMylabel(), BorderLayout.SOUTH);
		drawiconspanel.add(inner1);
			
		MySelectionLabel label2 = new MySelectionLabel("line");
		JPanel inner2 = new JPanel();
		inner2.setLayout(new BorderLayout());
		rbline = new JRadioButton();
		rbline.setUI(new MyRadioButtonUI());
		rbline.setIcon(iline);
		rbline.setSelectedIcon(iselline);
		rbline.setAction(new DrawAction(rbline, DrawActionEnum.LINE.asString()));
		drawActions.put(DrawActionEnum.LINE, rbline);
		drawgroup.add(rbline);
		selectionLabelMap.put(rbline, label2);
		inner2.add(rbline,  BorderLayout.NORTH);
		inner2.add(label2.getMylabel(),  BorderLayout.SOUTH);
		drawiconspanel.add(inner2);	
		
		java.awt.Dimension dim = rbline.getPreferredSize(); //align point with everything else
		rbpoint.setPreferredSize(dim);
		
		
		MySelectionLabel label3 = new MySelectionLabel("poly");
		JPanel inner3 = new JPanel(new BorderLayout());
		rbpoly = new JRadioButton();
		rbpoly.setUI(new MyRadioButtonUI());
		rbpoly.setIcon(ipoly);
		rbpoly.setSelectedIcon(iselpoly);
		rbpoly.setAction(new DrawAction(rbpoly, DrawActionEnum.POLYGON.asString()));
		drawActions.put(DrawActionEnum.POLYGON, rbpoly);
		drawgroup.add(rbpoly);
		selectionLabelMap.put(rbpoly, label3);
		inner3.add(rbpoly, BorderLayout.NORTH);
		inner3.add(label3.getMylabel(), BorderLayout.SOUTH);
		drawiconspanel.add(inner3);	
		
		
		MySelectionLabel label4 = new MySelectionLabel("stream");
		JPanel inner4 = new JPanel(new BorderLayout());
		rbfreehand = new JRadioButton();
		rbfreehand.setUI(new MyRadioButtonUI());
		rbfreehand.setIcon(ifreehand);
		rbfreehand.setSelectedIcon(iselfreehand);
		rbfreehand.setAction(new DrawAction(rbfreehand, DrawActionEnum.FREEHAND.asString()));	
		drawActions.put(DrawActionEnum.FREEHAND, rbfreehand);
		drawgroup.add(rbfreehand);
		selectionLabelMap.put(rbfreehand, label4);
		inner4.add(rbfreehand, BorderLayout.NORTH);
		inner4.add(label4.getMylabel(), BorderLayout.SOUTH);
		drawiconspanel.add(inner4);	
		

		MySelectionLabel label5 = new MySelectionLabel("circle");
		JPanel inner5 = new JPanel(new BorderLayout());
		rbcircle = new JRadioButton();
		rbcircle.setUI(new MyRadioButtonUI());
		rbcircle.setIcon(icircle);
		rbcircle.setSelectedIcon(iselcircle);
		rbcircle.setAction(new DrawAction(rbcircle, DrawActionEnum.CIRCLE.asString()));
		drawActions.put(DrawActionEnum.CIRCLE, rbcircle);
		drawgroup.add(rbcircle);
		selectionLabelMap.put(rbcircle, label5);
		inner5.add(rbcircle, BorderLayout.NORTH);
		inner5.add(label5.getMylabel(), BorderLayout.SOUTH);
		drawiconspanel.add(inner5);	
		
		
		MySelectionLabel label6 = new MySelectionLabel("5-point","ellipse");
		JPanel inner6 = new JPanel(new BorderLayout());
		rbellipse5 = new JRadioButton();
		rbellipse5.setUI(new MyRadioButtonUI());
		rbellipse5.setIcon(i5ptellipse);
		rbellipse5.setSelectedIcon(isel5ptellipse);
		rbellipse5.setAction(new DrawAction(rbellipse5, DrawActionEnum.ELLIPSE5.asString()));
		drawActions.put(DrawActionEnum.ELLIPSE5, rbellipse5);
		drawgroup.add(rbellipse5);
		selectionLabelMap.put(rbellipse5, label6);
		inner6.add(rbellipse5, BorderLayout.NORTH);
		inner6.add(label6.getMylabel(), BorderLayout.SOUTH);
		drawiconspanel.add(inner6);	
	
		MySelectionLabel label7 = new MySelectionLabel("rctngl");
		JPanel inner7 = new JPanel(new BorderLayout());
		rbrectangle = new JRadioButton();
		rbrectangle.setUI(new MyRadioButtonUI());
		rbrectangle.setIcon(irectangle);
		rbrectangle.setSelectedIcon(iselrectangle);
		rbrectangle.setAction(new DrawAction(rbrectangle, DrawActionEnum.RECTANGLE.asString()));
		drawActions.put(DrawActionEnum.RECTANGLE, rbrectangle);
		drawgroup.add(rbrectangle);
		selectionLabelMap.put(rbrectangle, label7);
		inner7.add(rbrectangle, BorderLayout.NORTH);
		inner7.add(label7.getMylabel(), BorderLayout.SOUTH);
		drawiconspanel.add(inner7);	
		
		MySelectionLabel label8 = new MySelectionLabel("ellipse ");
		JPanel inner8 = new JPanel(new BorderLayout());
		rbellipse = new JRadioButton();
		rbellipse.setUI(new MyRadioButtonUI());
		rbellipse.setIcon(iellipse);
		rbellipse.setSelectedIcon(iselellipse);
		rbellipse.setAction(new DrawAction(rbellipse, DrawActionEnum.ELLIPSE.asString()));
		drawActions.put(DrawActionEnum.ELLIPSE, rbellipse);
		drawgroup.add(rbellipse);
		selectionLabelMap.put(rbellipse, label8);
		inner8.add(rbellipse, BorderLayout.NORTH);
		inner8.add(label8.getMylabel(), BorderLayout.SOUTH);
//		drawiconspanel.add(inner8);	
	
		drawpanel.add(drawiconspanel, BorderLayout.CENTER);
	}

	private void buildHeaderPanel() {
		headerpanel = new JPanel();
		//headerpanel.setBackground(Color.BLACK); // for standalone testing
		headerpanel.setBorder(BorderFactory.createEmptyBorder(15, 5, 20, 15));
		headerpanel.setSize(350, 52);
		headerpanel.setLayout(new BorderLayout(0, 0));

		JLabel shapetoolsLbl = new FontSizeHeaderLabel("Shape Tools", Font.BOLD, 15);
		shapetoolsLbl.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		headerpanel.add(shapetoolsLbl, BorderLayout.WEST);

		JLabel ellipsemenuLbl = new JLabel(dotmenu);
		headerpanel.add(ellipsemenuLbl, BorderLayout.EAST);

		contentPane.add(headerpanel, BorderLayout.NORTH);
	}

	private static void loadGraphics() {
		imgColor = ((ThemeImages) GUITheme.get("images")).getFill(); ////for standalone testing   Color.WHITE;
								
		bordercolor =  ThemeProvider.getInstance().getAction().getBorder(); //for standalone testing Color.GRAY;
									//
		imgSelectedColor = ((ThemeImages) GUITheme.get("images")).getSelectedfill();  //new Color(249, 192, 98);

		//SELECT icon - when selected = SELECT shape; Unselected = DRAW;
		iselect = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.PROFILE_PENCIL.withDisplayColor(imgColor)));
		iselectSel = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.MOUSE_POINTER_IMG_SEL.withDisplayColor(imgColor)));
		
		
		//DRAW icons
		irectangle = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_SQUARE.withDisplayColor(imgColor)));
		iselrectangle = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_SQUARE_SEL.withDisplayColor(imgSelectedColor)));

		icircle = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_CIRCLE.withDisplayColor(imgColor)));
		iselcircle = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_CIRCLE_SEL.withDisplayColor(imgSelectedColor)));

		iellipse = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_ELLIPSE.withStrokeColor(imgColor)));
		iselellipse = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_ELLIPSE_SEL.withDisplayColor(imgSelectedColor)));
		
		i5ptellipse = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_5PTELLIPSE.withDisplayColor(imgColor)));
		isel5ptellipse = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_5PTELLIPSE_SEL.withDisplayColor(imgSelectedColor)));

		ipoint = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_POINT.withDisplayColor(imgColor)));
		iselpoint = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_POINT_SEL.withDisplayColor(imgSelectedColor)));

		iline = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_LINE.withDisplayColor(imgColor)));
		iselline = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_LINE_SEL.withDisplayColor(imgSelectedColor)));
		
		ipoly = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_POLYGON.withDisplayColor(imgColor)));
		iselpoly = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_POLYGON_SEL.withDisplayColor(imgSelectedColor)));

		ifreehand = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_FREEHAND.withDisplayColor(imgColor)));
		iselfreehand = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_FREEHAND_SEL.withDisplayColor(imgSelectedColor)));

		// COMBINE icons
		imerge = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_OBJECTUNION.withDisplayColor(imgColor)));
		isubtract = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_OBJECTSUBTRACT.withDisplayColor(imgColor)));
		iintersect = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_OBJECTINTERSECT.withDisplayColor(imgColor)));

		// ORDER
		iforward = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_BRINGFORWARD.withDisplayColor(imgColor)));
		ibackward = new ImageIcon(
				ImageFactory.createImage(ImageCatalogItem.DP_SENDBACKWARD.withDisplayColor(imgColor)));
		ifront = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_BRINGFRONT.withDisplayColor(imgColor)));
		iback = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.DP_SENDBACK.withDisplayColor(imgColor)));
		dotmenu = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.ELLIPSE_MENU.withDisplayColor(imgColor)));

		lineseparator = BorderFactory.createCompoundBorder(new MatteBorder(0, 0, 1, 0, bordercolor),
				new EmptyBorder(20, 5, 20, 5));
	}
	
	
	@Override
	public void update(Observable o, Object arg) {
		if (arg == null) return;
		if (o instanceof ShapeActionObservable) {
			updateShapeAction(arg);
		} else if (o instanceof ActiveViewChangedObservable) {
			updateActiveViewChanged(arg);
		}
	}

	private void updateActiveViewChanged(Object arg) {
		if (!(arg instanceof Layer.LView)) return;		
		//if active view is not Custom Shape - hide the palette and change tool mode
		Layer.LView view = (Layer.LView)arg;
		if (!(isCustomShapeInstance(view))) {
			changeToolModeFromShapesToSELECT(view);
			hide();
		} else {
			String lbl = LManager.getLManager().getUniqueName(view);
			drawingPaletteFrame.setTitle(lbl);
			initDrawActionPerView(view);
			showOrHidePaletteBasedOnUserChoice(view);
		}
	}

	private void showOrHidePaletteBasedOnUserChoice(LView shapeview) {
		if (ToolManager.getToolMode() == ToolManager.SHAPES) {
			if (userClosedPalette.get(SOME_KEY) == null) {
				show(shapeview); // show first time; is user closed it, don't show just prompt
			} else if (userClosedPalette.containsKey(SOME_KEY) && drawingPaletteFrame.isVisible()) {
				hide();
				JButton shapesTool = Main.testDriver.toolMgr.getShapesToolComponent();
				String msg = (ToolManager.getToolMode() == ToolManager.SHAPES) ?  
						UserPrompt.ACTIVATE_PALETTE.asString() : UserPrompt.ACTIVATE_PALETTE_2.asString();
				showCallout(shapesTool, msg, UserPrompt.SHORT_TIME);
			}
		}
	}

	private void updateDrawActionPerView(LView shapeview) {
		if (drawActionPerView.containsKey(shapeview)) {
			AbstractButton currentdrawingselection = drawActionPerView.get(shapeview);
			if (currentdrawingselection != null) {
				currentdrawingselection.setSelected(true);
				resetButtonLabel(currentdrawingselection, true); //set selected text
			}
		}
	}

	private void activateSHAPESToolMode(LView view) {
		if (ToolManager.getToolMode() != ToolManager.SHAPES) {
			ToolManager.setToolMode(ToolManager.SHAPES);
		} else {
			showOrHidePaletteBasedOnUserChoice(view);
		}
	}

	private void activateSELECTToolMode(LView view) {
		if (ToolManager.getToolMode() != ToolManager.SEL_HAND) {
			ShapesToolStrategy.setToolModeWhenNewCustomShapeCreated();
		}
	}

	private void initDrawActionPerView(LView shapeview) { //initial setting if this view hasn't used palette yet
		if (!drawActionPerView.containsKey(shapeview)) {
			drawActionPerView.put(shapeview, rbselect);  //default - no tool selection
			activateSELECTToolMode(shapeview);
		}
	}

	private void changeToolModeFromShapesToSELECT(LView view) {
		if (ToolManager.getToolMode() == ToolManager.SHAPES) {
			ToolManager.setToolMode(ToolManager.SEL_HAND);
			// if 'Add Layer' dialog was opened, tool mode did not change (see CommonToolStrategy #18)
			//so retry setting it again
			if (ToolManager.getToolMode() != ToolManager.SEL_HAND) {
				ToolManager.setToolMode(ToolManager.SEL_HAND);
			}
		}
	}

	private void updateShapeAction(Object arg) {
		if (!(arg instanceof ShapeActionData)) return;
		ShapeActionData shapeActionData = (ShapeActionData) arg;
		DrawActionEnum action = (DrawActionEnum) shapeActionData.getSelectedDrawAction();
		Layer.LView viewForThisAction = shapeActionData.getViewForThisAction();
		AbstractButton selectedAction = drawActions.get(action);
		if (selectedAction != null) {
			selectedAction.setSelected(true);
			resetButtonLabel(selectedAction, true); //set selected text
			if (viewForThisAction != null) {
				drawActionPerView.put(viewForThisAction, selectedAction);
				if (selectedAction == rbselect) {
					activateSELECTToolMode(viewForThisAction);
				} else {
					activateSHAPESToolMode(viewForThisAction);
				}			
			}	
		}
	}

	/*
	 * public static void main(String[] args) { EventQueue.invokeLater(new
	 * Runnable() { public void run() { try { DrawingPalette.INSTANCE.init();
	 * //dp.show(); } catch (Exception e) { e.printStackTrace(); } } }); } */
	
	@Override
	public void componentResized(ComponentEvent e) {
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		screenX = e.getComponent().getX();
		screenY = e.getComponent().getY();
	}

	@Override
	public void componentShown(ComponentEvent e) {
		firstLoad.remove(FIRST_LOAD_KEY);
		if (userClosedPalette.containsKey(SOME_KEY)) {
			userClosedPalette.remove(SOME_KEY);
		}
	}

	@Override
	public void componentHidden(ComponentEvent e) {
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
						+ msg
						+ "</b>" + "</p></html>";
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
		
		
	private static class MyRadioButtonUI extends MaterialRadioButtonUI {

		@Override
		public void installUI(JComponent c) {
			super.installUI(c);
			mouseHoverEnable = true;
			c.setFocusable(true);
			super.mouseHoverColor = ((ThemeButton) GUITheme.get("button")).getAltOnhover();
			if (mouseHoverEnable) {
				radioButton.addMouseListener(MaterialUIMovement.getMovement(radioButton,super.mouseHoverColor));
			}
		}
	}
	

	private static class MySelectionLabel {
		private final JLabel mylabel;
		private String plaintext;
		private String selectedtext;

		public MySelectionLabel(String... txt) {
			mylabel = new JLabel();
			String colorhex = edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex(imgColor);
			plaintext = "<html>" + "<p style=\"color:" + colorhex + ";text-align:center;\">" + txt[0] + "<br/>"
					+ ((txt.length > 1) ? txt[1] + "&nbsp;</p></html>" : "&nbsp;</p></html>");

			colorhex = edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex(imgSelectedColor);
			selectedtext = "<html>" + "<p style=\"color:" + colorhex + ";text-align:center;\">" + txt[0]
					+ "<br/>" + ((txt.length > 1) ? txt[1] + "&nbsp;</p></html>" : "&nbsp;</p></html>");
			mylabel.setText(plaintext);
		}

		public JLabel getMylabel() {
			return mylabel;
		}

		public void setPlaintext() {
			mylabel.setText(this.plaintext);
		}

		public void setSelectedtext() {
			mylabel.setText(this.selectedtext);
		}
	}

	public AbstractButton getDrawActionPerView(Layer.LView shapeview) {
		AbstractButton action = null;
		action = drawActionPerView.get(shapeview);
		if (action == rbselect) {
			action = null;
		}
		return action;
	}


	public void resetDrawMode(LView activeview) {
		if (!drawActionPerView.isEmpty()) {
			for (Entry<LView, AbstractButton> entry : drawActionPerView.entrySet()) {
				if (activeview == entry.getKey()) {
					drawActionPerView.put(entry.getKey(), rbselect);
					rbselect.setSelected(false);
					rbselect.doClick();
					break;
				}
			}
		} else {
			initDrawActionPerView(activeview);		
		}
	}

	public void clearClose() {
		if (userClosedPalette.containsKey(SOME_KEY)) {
			userClosedPalette.remove(SOME_KEY);
		}
	}

}
