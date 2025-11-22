package edu.asu.jmars.layer.stamp.radar;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.MouseInputListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYSeriesCollection;

import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampImageFactory;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.StampLayer.StampSelectionListener;
import edu.asu.jmars.layer.stamp.networking.StampLayerNetworking;
import edu.asu.jmars.swing.OverlapLayout;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeChart;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeMenuBar;

//TODO Some restructuring in the RadarFocusPanel still needs to be done, 
// so that the panel is pulling it's information directly from the 
// FilledStampRadarType object instead of copying the values to local variables.
public class RadarFocusPanel extends JPanel implements StampSelectionListener{
	
	final private StampLView parent;
	final private StampLayer stampLayer;
	private JLayeredPane browseLp;
	private JLayeredPane fullResLp;
	private BufferedImage browseImage;
	private BufferedImage overlayBrowseImage;
	private BrowseDrawPanel browseDrawPnl;
	private FullResDrawPanel fullResDrawPnl;
	private FullResImagePanel fullResPnl;
	private JFrame fullResControlFrame;
	private JButton chartBtn;
	private JButton fullResBtn;
	private JCheckBox singleOverlayChk;
	private JComboBox<String> baseBx;
	private JComboBox<String> overlayBx;
	private JCheckBox showOverlayChk;
	private int curSample;
	private double curLon, curLat;
	private double[] plotData;
	private BufferedImage fullResNumeric;
	private BufferedImage fullResImage;
	private BufferedImage overlayFullImage;
	private FullResImagePanel fullResOverlayPnl;
	private StampImage fullResStampImage;
	private JSplitPane split;
	private JFrame fullResFrame;
	private JPanel holdPnl;
	private String xAxisStr;
	private String yAxisStr;
	private HashMap<String,String> addYAxesMap;
	private HashMap<String,int[]> axesToRangeMap;
	private JFrame chartFrame;
	private ChartPanel chartPnl;
	private JFreeChart chart;
	private JFileChooser fileChooser;
	private JMenuItem csvSaveItem;
	private JPanel samplePnl;
	private JLabel sampleLbl;
	private String samplePrompt = "Current sample: ";
	private JPanel idPnl;
	private JLabel idLbl;
	private String idPrompt = "ID: ";
	private JScrollPane browseSp;
	private HorizonPanel horizonPnl;
	
	private final String CHART_NORMAL_STR = "LOAD GRAPH";
	private final String CHART_TOOLTIP;
	private final String FULL_RES_NORMAL_STR = "VIEW FULL RES";
	private final String LOADING_STR = "LOADING...";
	private final String SELECT_PROD_STR = "SELECT PRODUCT";
	private final String OVERLAY_STR;
	
	//map of display names and imageTypes stamp server expects
	private ArrayList<String> numericProducts;
	private HashMap<String, String> productImagetypeMap = new HashMap<String, String>();
	private boolean showOverlay = false;
	private Vector<String> imageProducts;
	
	//layers with multiple radar products
	private String selectedBaseName;
	private String selectedOverlayName;
	
	private StampShape ss = null;
	private String stampId;
	
	/** This is what percent the cue line is at in regards to entire radar image */
	private double cuePercent = .5;
	/** This is the x pixel of the full resolution image, which the full resolution image panel begins at. */
	private int fullResXStart = 0;
	/** This is the y pixel of the full resolution image, which the full resolution image panel begins at. */
	private int fullResYStart = 0;
	/** This is the width in pixels of the full resolution image panel. */
	private int fullResViewWidth = 0;
	/** This is the width in pixels of the raster of the full resolution image. */
	private int fullResWidth = 0;
	/** This is the height in pixels of the raster of the full resolution image (3600 for SHARAD, 3072 for MARSIS). */
	private int fullResHeight = 0;
	
	//These constants are set based on the StampLayer and are used when
	// retrieving data, and translating between scaled and full res images.
	private double scale;
	private String instrument;
	private int maxHeight;
	private int zoom;
	
	//Variables used for the drawing of horizons
	private ArrayList<Integer> xPts = new ArrayList<Integer>();
	private ArrayList<Integer> yPts = new ArrayList<Integer>();
	private int xEnd = -1;
	private int yEnd = -1;
	private boolean isDrawingHorizon = false;
	private ArrayList<RadarHorizon> horizons = new ArrayList<RadarHorizon>();
	private int horizonInt = 0;
	private Color horizonTmpColor = Color.ORANGE;
	
	private int pad = 0;
	private Insets in = new Insets(pad,pad,pad,pad);
	
    private static DebugLog log = DebugLog.instance();
	
	public RadarFocusPanel(final StampLView lview){
		parent = lview;
		stampLayer = parent.stampLayer;
		
		stampLayer.addSelectionListener(this);
		
		instrument = stampLayer.getInstrument();
		scale = Double.parseDouble(stampLayer.getParam(StampLayer.RADAR_BROWSE_SCALE)); //15% works for sharad, 26% seems good for marsis
		maxHeight = Integer.parseInt(stampLayer.getParam(StampLayer.RADAR_MAX_HEIGHT));
		zoom = (int)(scale*maxHeight);
		
		//get the chart axes names
		xAxisStr = stampLayer.getRadarXAxisName();
		yAxisStr = stampLayer.getRadarYAxisName();
		addYAxesMap = stampLayer.getAdditionalRadarYAxesNames();
		axesToRangeMap = stampLayer.getRadarYAxesRanges();
		
		//populate the product Instrument map and numeric list
		productImagetypeMap = stampLayer.getRadarProductMap();
		numericProducts = stampLayer.getNumericRadarList();
		
		//if there are only 1 or 2 products, populate the default and overlay variables
		// and tooltip for the chart button
		int numProducts = productImagetypeMap.size();
		if(numProducts < 3){
			int index = 0;
			for(String key : productImagetypeMap.keySet()){
				//first product is default
				if(index == 0){
					selectedBaseName = key;
				}
				//second product is overlay
				else if(index == 1){
					selectedOverlayName = key;
				}
				index++;
			}
			CHART_TOOLTIP = "Load numeric data for the "+selectedBaseName;
			OVERLAY_STR = "SHOW "+selectedOverlayName;
		}
		//create products vector used for comboboxes
		else{
			imageProducts = new Vector<String>();
			for(String key : productImagetypeMap.keySet()){
				imageProducts.add(key);
			}
			CHART_TOOLTIP = "Load numeric data for the selected Base product";
			OVERLAY_STR = "SHOW OVERLAY";
		}
		
		buildLayout();
	}
	
	private void buildLayout(){
		this.setLayout(new BorderLayout());
		split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		//give the left panel the resize weight (expand the image panel not the chart)
		split.setResizeWeight(1.0);
		
		idLbl = new JLabel(idPrompt);
		idPnl = new JPanel();
		idPnl.add(idLbl);
		
		browseLp = new JLayeredPane();
		browseLp.setLayout(new DrawLayout());
		
		browseSp = new JScrollPane(browseLp, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
		browseSp.getVerticalScrollBar().setUnitIncrement(15);
		browseSp.setBorder(new EmptyBorder(0, 0, 0, 0));

		samplePnl = new JPanel();
		sampleLbl = new JLabel(samplePrompt);
		JLabel tipsIcnLbl = new JLabel(new ImageIcon(ImageFactory.createImage(ImageCatalogItem.INFO.withDisplayColor(Color.WHITE))));
		tipsIcnLbl.setToolTipText("<html><ul><li>Reads out the current line number as the sample</li>"
								+ "<li>To move the cue line click and drag on the radargram</li>"
								+ "<li>Once full resolution is loaded, hold the shift key to move the viewbox</li></html>");
		samplePnl.add(tipsIcnLbl);
		samplePnl.add(Box.createHorizontalGlue());
		samplePnl.add(sampleLbl);
		samplePnl.add(Box.createHorizontalGlue());
		chartBtn = new JButton(chartAct);
		chartBtn.setEnabled(false);
		chartBtn.setToolTipText(CHART_TOOLTIP);
		fullResBtn = new JButton(fullResAct);
		fullResBtn.setEnabled(false);
		Dimension frbSize = fullResBtn.getPreferredSize();
		fullResBtn.setMinimumSize(frbSize);
		fullResBtn.setPreferredSize(frbSize);
		
		int row = 0;
		JPanel btnPnl = new JPanel(new GridBagLayout());
		btnPnl.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		btnPnl.add(fullResBtn, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 0, 2, 0), pad, pad));
		btnPnl.add(chartBtn, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(2, 0, 2, 0), pad, pad));
		//only add cluttergram button if layer supports it
		//If there are exactly two products, add hide/display check
		if(productImagetypeMap.size() == 2){
			singleOverlayChk = new JCheckBox(showOverlayAct);
			singleOverlayChk.setText(OVERLAY_STR);
			//put it inside a panel so the components don't move if/when
			// the checkbox changes to "LOADING..."
			JPanel chkPnl = new JPanel(new BorderLayout());
			int w = singleOverlayChk.getPreferredSize().width +20;
			int h = singleOverlayChk.getPreferredSize().height;
			if(w < 200){
				w = 200;
			}
			chkPnl.setPreferredSize(new Dimension(w,h));
			chkPnl.add(singleOverlayChk, BorderLayout.WEST);
			btnPnl.add(chkPnl, new GridBagConstraints(1, 0, 1, 2, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(pad, 15, pad, pad), pad, pad));
		}
		//If there is more than two products, then add the comboboxes
		else if(productImagetypeMap.size()>2){
			baseBx = new JComboBox<String>(imageProducts);
			baseBx.addActionListener(baseListener);
			selectedBaseName = baseBx.getSelectedItem().toString();
			showOverlayChk = new JCheckBox(showOverlayAct);
			showOverlayChk.setText(OVERLAY_STR);
			//disable checkbox when "Select Product" is chosen in overlaybx
			showOverlayChk.setEnabled(false);
			//put checkbox in a panel so it doesn't resize when "loading"
			JPanel chkPnl = new JPanel(new BorderLayout());
			Dimension size = showOverlayChk.getPreferredSize();
			chkPnl.setMinimumSize(size);
			chkPnl.setPreferredSize(size);
			chkPnl.add(showOverlayChk, BorderLayout.EAST);
			//create and populate overlay box
			overlayBx = new JComboBox<String>(new DefaultComboBoxModel<String>());
			overlayBx.addActionListener(overlayListener);
			updateOverlayBox();
			
			row = 0;
			btnPnl.add(chkPnl, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
			btnPnl.add(overlayBx, new GridBagConstraints(2, row, 1, 1, 1, 0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, in, pad, pad));
			row++;
			btnPnl.add(new JLabel("BASE: "), new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
			btnPnl.add(baseBx, new GridBagConstraints(2, row, 1, 1, 1, 0, GridBagConstraints.LINE_END, GridBagConstraints.HORIZONTAL, in, pad, pad));
		}

		row = 0;
		JPanel leftPnl = new JPanel(new GridBagLayout());
		leftPnl.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
		leftPnl.add(idPnl, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		leftPnl.add(browseSp, new GridBagConstraints(0, row++, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 7, 0, 0), pad, pad));
		leftPnl.add(samplePnl, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		leftPnl.add(btnPnl, new GridBagConstraints(0, row++, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));

		resetChartData(true, null, null);
		chartPnl.setPreferredSize(new Dimension(300, 0));
		
		boolean enableDiff = false;
		if(instrument.equalsIgnoreCase("SHARAD")){
			enableDiff = true;
		}
		horizonPnl = new HorizonPanel(parent, enableDiff);
		
		//Put the chart panel in another JPanel to add a border so
		// the height lines up with the browse image by default
		JPanel chartTab = new JPanel();
		chartTab.setBackground(Color.WHITE);
		chartTab.setLayout(new BorderLayout());
		chartTab.setBorder(new EmptyBorder(0, 0, 25, 0));
		chartTab.add(chartPnl, BorderLayout.CENTER);
		
		JTabbedPane rightPane = new JTabbedPane();
		rightPane.setTabPlacement(JTabbedPane.TOP);
		rightPane.addTab("Depth Plot".toUpperCase(), chartTab);
		rightPane.addTab("Manage Horizons".toUpperCase(), horizonPnl);

		split.setLeftComponent(leftPnl);
		split.setRightComponent(rightPane);
		
		this.add(split, BorderLayout.CENTER);

		//try and use a decently wide panel size to begin with
		// leave space for the chart on the right of the divider.
		//TODO: this should really be smarter...
		int width = 950;
		if(baseBx !=null){
			width = 700+baseBx.getPreferredSize().width;
		}
		this.setPreferredSize(new Dimension(width, 670));
		split.setDividerLocation(width-400);

		
		//if a selection has already been made, be sure to show 
		// it's radar data...do this by calling selectionChanged.
		if(stampLayer.getSelectedStamps().size()>0){
			selectionsChanged();
		}
	}
	
	private void resetChartData(boolean emptyChart, String title, DefaultXYDataset data){
		// create blank chart if boolean is set, or if the selected base 
		// does not have a numeric product (ie. cluttergram)
		boolean isNotNumeric = !numericProducts.contains(selectedBaseName);
		if(emptyChart || isNotNumeric){
			String blankTitle = selectedBaseName;
			if(isNotNumeric){
				blankTitle = "";
			}
			chart = ChartFactory.createXYLineChart(blankTitle, xAxisStr, yAxisStr, new XYSeriesCollection());
		}else{
			chart = ChartFactory.createXYLineChart(
					title, //Title
					xAxisStr,  //x axis label
					yAxisStr,//y axis label
					data, //dataset
					PlotOrientation.VERTICAL, //orientation
					false, //legend
					false, //tooltips
					false); //url
		}
		
        ThemeChart.configureUI(chart);
		XYItemRenderer rr = ((XYPlot) chart.getPlot()).getRenderer();
	    rr.setSeriesPaint(0, ThemeChart.getPlotColor());
		chart.getXYPlot().getRangeAxis().setRange(0,maxHeight);
		chart.getXYPlot().getRangeAxis().setInverted(true);
		
		//if it is a numeric product, check if there is an additional y-axis
		if(!isNotNumeric){
			//only add the second axis if there is more than one label
			if(addYAxesMap.size()>0){
				//create the second y-axis
				String axisName = addYAxesMap.get(selectedBaseName);
				ValueAxis yAxis2 = new NumberAxis(axisName);
				ThemeChart.applyThemeToAxis(yAxis2);
				yAxis2.setInverted(true);
				int[] range = axesToRangeMap.get(axisName);
				yAxis2.setRange(range[0],range[1]);
				chart.getXYPlot().setRangeAxis(1, yAxis2);
			}
		}

		if(chartPnl == null){
			chartPnl = new ChartPanel(chart);
		}
		chartPnl.setChart(chart);
	}

	private ActionListener baseListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			selectedBaseName = baseBx.getSelectedItem().toString();
			
			//remove current base selection from overlay choices
			updateOverlayBox();
			
			//reset chart and chart button enbabled/disabled based on this selection
			clearChartData();
			chartBtn.setEnabled(numericProducts.contains(selectedBaseName));
			
			//update browse
			//make url for the stamp server, zoom=540 is 0.15*3600 (15% the original image)
			String urlStr = getURL(selectedBaseName, stampId, false, zoom);
			//Get image from the server or cache if exists
			BufferedImage bi = StampImageFactory.getImage(urlStr, false);
			if(bi != null){
				browseImage = bi;
			}
			updateBrowseImages();
			
			//update fullres if it exists
			if(fullResFrame!=null){
				fullResImage = null;
				updateFullResImages();
			}
		}
	};
	
	private void updateOverlayBox(){
		//try to preserve the current selection
		String curSel = null;
		if(overlayBx.getModel().getSize()>0){
			curSel = overlayBx.getSelectedItem().toString();
		}
		
		Vector<String> products = new Vector<String>(imageProducts);
		products.add(0, SELECT_PROD_STR);
		products.remove(selectedBaseName);
		
		DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>)overlayBx.getModel();
		
		model.removeAllElements();
		for(String product : products){
			model.addElement(product);
		}
		
		//reset the selection
		if(curSel != null){
			overlayBx.setSelectedItem(curSel);
		}
	}
	
	private ActionListener overlayListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if(overlayBx.getSelectedItem()!=null){
				selectedOverlayName = overlayBx.getSelectedItem().toString();
				//reset browse image
				overlayBrowseImage = null;
				//overlay has changed so clear current overlay image
				overlayFullImage = null;
				fullResOverlayPnl = null;
				
				if(selectedOverlayName.equals(SELECT_PROD_STR)){
					showOverlayChk.setEnabled(false);
				}
				else{
					showOverlayChk.setEnabled(true);
				}
				
				//if show is checked, update the overlay right away
				if(showOverlayChk.isSelected()){
					updateOverlayBrowse(showOverlayChk);
					
					//if full res is opened, update full res view
					if(fullResFrame!=null && fullResFrame.isVisible()){
						updateFullResImages();
					}
				}
			}
		}
	};
	
	@SuppressWarnings("serial")
	private AbstractAction chartAct = new AbstractAction(CHART_NORMAL_STR) {
		public void actionPerformed(ActionEvent e) {
			//change the button text to give some feedback to the user 
			//TODO: later we should use a progress bar
			chartBtn.setText(LOADING_STR);
			chartBtn.setEnabled(false);
			
			//Pull the raw data file from stamp server
			String id = getSelectedStampID();

			//make URL for the stamp server
			String urlStr = getURL(selectedBaseName, id, true, maxHeight);
			
			//Do this on another thread to not hang the awt thread
			Thread t = new Thread(new Runnable() {
				public void run() {
					//Get image from the server or cache if already exists
					BufferedImage bi = StampImageFactory.getImage(urlStr, true);
					
					if(bi != null){
						fullResNumeric = bi;
						//populate the new chart
						updateChart();
					}
					
					//change the text back when finished
					chartBtn.setText(CHART_NORMAL_STR);
				}
			});
			t.start();
		}
	};
	
	@SuppressWarnings("serial")
	private AbstractAction fullResAct = new AbstractAction(FULL_RES_NORMAL_STR) {
		public void actionPerformed(ActionEvent e) {
			updateFullResImages();
		}
	};
	
	private void updateOverlayBrowse(JCheckBox chk){
		if(!selectedOverlayName.equalsIgnoreCase(SELECT_PROD_STR) && overlayBrowseImage == null){
			Thread t = new Thread(new Runnable() {
				public void run() {
					chk.setText(LOADING_STR);				
					
					String clutterUrl = getURL(selectedOverlayName, stampId, false, zoom);
					overlayBrowseImage = StampImageFactory.getImage(clutterUrl, false);
					
					updateBrowseImages();
					chk.setText(OVERLAY_STR);
				}
			});
			t.start();
		}
		else{
			updateBrowseImages();
		}
	}

	@SuppressWarnings("serial")
	private AbstractAction showOverlayAct = new AbstractAction() {
		public void actionPerformed(ActionEvent arg0) {
			JCheckBox chk = (JCheckBox)arg0.getSource();
			showOverlay = chk.isSelected();
			
			updateOverlayBrowse(chk);
			
			if(fullResFrame != null && fullResFrame.isVisible()){
				updateFullResImages();
			}
		}
	};
	
	/**
	 * Get the proper URL for the stamp server to request the image data
	 * @param cluttergram  True if this is for a cluttergram, false if it's for a regular radar image
	 * @param id  The stamp ID
	 * @param numeric  True if this is numeric (used for plotting/chart)
	 * @param zoom  The height of the image used for the zoom/scale of the image
	 * @return The proper url string for the stamp server request
	 */
	private String getURL(String productKey, String id, boolean numeric, int zoom){
		String imgType = productImagetypeMap.get(productKey);
		if(numeric){
			imgType +="_NUM";
		}
		
		String urlStr = "ImageServer?instrument="+instrument.toLowerCase()
		+"&id="+id+"&imageType="+imgType+"&zoom="+zoom;
		
		return urlStr;
	}
	
	private void updateFullResImages(){
			
		Thread t1 = new Thread(new Runnable(){
			@Override 
			public void run() {
				int i = 10;
				
				fullResBtn.setEnabled(false);
				if(fullResImage == null){
					fullResBtn.setText(LOADING_STR);
					//construct URL for stamp server
					String urlStr = getURL(selectedBaseName, stampId, false, maxHeight);
					//get image from server or cache if it exists already
					fullResImage = StampImageFactory.getImage(urlStr, false);

					//TODO: might have to think about how we do this? (multiple full res images per filled stamp?)
					//create a new filledStampRadarType and add it to the rendered tab
					((FilledStampRadarTypeFocus)parent.getFocusPanel().getRenderedView()).addStamp(ss, instrument, new FilledStampRadarType.State(urlStr, instrument));
				}

				setFullResImage(fullResImage);

				fullResLp.add(fullResPnl, new Integer(i++));
				
				if(showOverlay){
					if(overlayFullImage == null){
						//update button with "loading" string
						fullResBtn.setText(LOADING_STR);
						//if fullres window is open, update with "loading" message
						JLabel fullResLoadLbl = new JLabel("    LOADING "+selectedOverlayName+"...");
						fullResLp.add(fullResLoadLbl, new Integer(i++));
						fullResLp.revalidate();
						
						String oStr = getURL(selectedOverlayName, stampId, false, maxHeight);
						//get image from server or cache
						overlayFullImage = StampImageFactory.getImage(oStr, false);
						
						//remove "loading" message
						fullResLp.remove(fullResLoadLbl);
					}
					
					fullResOverlayPnl = new FullResImagePanel(overlayFullImage);
					fullResLp.add(fullResOverlayPnl, new Integer(i++));
				}
				
				//change the text back when finished
				fullResBtn.setText(FULL_RES_NORMAL_STR);
			
				fullResLp.add(fullResDrawPnl, new Integer(i++));
				fullResLp.revalidate();
				
				fullResFrame.setVisible(true);
			}
		});
		
		
		//if everything already exists, simply draw it
		boolean startThread = false;
		draw: if(fullResFrame!=null){
			fullResLp.removeAll();
			int j = 0;
			if(fullResImage!=null && fullResPnl!=null){
				fullResLp.add(fullResPnl, new Integer(j++));
			}else{
				startThread = true;
				break draw;
			}
			if(overlayFullImage!=null && fullResOverlayPnl != null && showOverlay){
				fullResLp.add(fullResOverlayPnl, new Integer(j++));
			}else{
				startThread = true;
			}
			fullResLp.add(fullResDrawPnl, new Integer(j++));
			fullResLp.revalidate();
			fullResFrame.setVisible(true);
		}else{
			startThread = true;
		}
		if(startThread){
			t1.start();
		}
	}
	
	private void setFullResImage(BufferedImage bi){
		setFullResImage(bi, false);
	}
	
	public void setFullResImage(BufferedImage bi, Boolean setFullRes){
		if(bi != null){
			fullResImage = bi;
			fullResPnl = new FullResImagePanel(fullResImage);
			
			//If the frame hasn't been created, create it
			if(fullResFrame == null){
				fullResFrame = new JFrame();
				fullResFrame.setSize(new Dimension (500,500));
				fullResFrame.setLocationRelativeTo(chartPnl);
				fullResFrame.setLayout(new GridLayout(1,1));
				//add window listener to the frame so if it closes, enable the button
				fullResFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent e){
						fullResBtn.setEnabled(true);
						browseDrawPnl.repaint();
					}
				});
				//add key listener to the frame to allow for arrow panning
				fullResFrame.addKeyListener(fullResKeyListener);
				
				//create the holding panel, that has the arrow key label
				holdPnl = new JPanel(new BorderLayout());			
				fullResFrame.getContentPane().add(holdPnl);
				
				//create layered pane to hold draw and image panels
				fullResLp = new JLayeredPane();
				fullResLp.setLayout(new DrawLayout());
				
				//add the file bar
				JMenuBar fullResBar = new JMenuBar();
				JMenu fileMenu = new JMenu("File");
				JMenuItem pngItm = new JMenuItem(saveFullresImageAct);
				JMenu helpMenu = new JMenu("Help");
				JMenuItem ctrlItm = new JMenuItem(showControlsAct);
				
				fileMenu.add(pngItm);
				fullResBar.add(fileMenu);
				int menuspacing = ((ThemeMenuBar) GUITheme.get("menubar")).getItemSpacing();
				fullResBar.add(Box.createHorizontalStrut(menuspacing));
				fullResFrame.setJMenuBar(fullResBar);
				helpMenu.add(ctrlItm);
				fullResBar.add(helpMenu);
			}
			
			fullResDrawPnl = new FullResDrawPanel();
			fullResDrawPnl.addMouseMotionListener(fullResImageListener);
			fullResDrawPnl.addMouseListener(fullResImageListener);
			
			//set the full res dimensions, because they are used several places
			fullResWidth = bi.getRaster().getWidth();
			fullResHeight = bi.getRaster().getHeight();
						
			holdPnl.add(fullResLp, BorderLayout.CENTER);
			fullResFrame.revalidate();
			fullResFrame.repaint();
			fullResFrame.setTitle("Full Resolution: "+getSelectedStampID());
			
			if(!fullResFrame.isVisible()){
				fullResFrame.setVisible(true);
			}
			
			//reset the stampImage object also
			fullResStampImage = new StampImage(ss, ss.getId(), instrument, instrument, null, StampLayerNetworking.getProjectionParams(ss.getId(), instrument, selectedBaseName));
		
			//if needing to set the full res and draw panel now,
			// like when coming from the rendered stamp tab
			if(setFullRes){
				int i = 0;
				fullResLp.add(fullResPnl, new Integer(i++));
				fullResLp.add(fullResDrawPnl, new Integer(i++));
				fullResLp.revalidate();
			}
		}
	}
	
	@SuppressWarnings("serial")
	private AbstractAction saveFullresImageAct = new AbstractAction("Capture as PNG...") {
		public void actionPerformed(ActionEvent e) {
			
			JFileChooser chooser = new JFileChooser(Util.getDefaultFCLocation());
			chooser.setDialogTitle("Capture screen to PNG");
		    FileNameExtensionFilter filter = new FileNameExtensionFilter(
			        "PNG - Portable Network Graphics", "png", "PNG");
			chooser.setFileFilter(filter);
			
			boolean succeed = true;
			if(chooser.showSaveDialog(fullResFrame) == JFileChooser.APPROVE_OPTION){
				//change the cursor to give some feedback to the user 
				fullResLp.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				
				String fileName = chooser.getSelectedFile().toString();
				//add the extension to the file name if the user didn't specify
				if(!fileName.endsWith(".png")){
					fileName = fileName+".png";
				}
				File file = new File(fileName);

				try {
					//grab the contents of the layered pane displayed in the full res frame
					BufferedImage bi = new BufferedImage(fullResDrawPnl.getWidth(), fullResDrawPnl.getHeight(), BufferedImage.TYPE_INT_RGB);
					fullResLp.paint(bi.getGraphics());
					ImageIO.write(bi, "PNG", file);
				} catch (IOException e1) {
					e1.printStackTrace();
					succeed = false;
				}
				
				if(succeed){
					Util.showMessageDialog("PNG Capture Successful!", "Capture Success", JOptionPane.INFORMATION_MESSAGE);
				}else{
					Util.showMessageDialog("PNG Capture Not Successful.\nSee log for more info.", "Capture Failure", JOptionPane.INFORMATION_MESSAGE);
				}	
			}		
			//change the cursor back when finished
			fullResLp.setCursor(Cursor.getDefaultCursor());
		}
	};
	
	@SuppressWarnings("serial")
	private AbstractAction showControlsAct = new AbstractAction("Show Controls") {
		public void actionPerformed(ActionEvent e) {
			//build frame and panel to display
			if(fullResControlFrame == null){
				fullResControlFrame = new JFrame();
				fullResControlFrame.setTitle("Controls");
				//hide window when it's closed instead of disposing
				fullResControlFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				fullResControlFrame.addWindowListener(new WindowAdapter() {
					public void windowClosing(WindowEvent windowEvent){
						fullResControlFrame.setVisible(false);
					}
				});
				
				//build display
				JPanel mainPnl = new JPanel();				
				
				JPanel controlPnl = new JPanel();
				controlPnl.setBorder(new CompoundBorder(new TitledBorder("Full Resolution Controls"), new EmptyBorder(5, 5, 5, 5)));
				controlPnl.setLayout(new GridBagLayout());
				
				JLabel panLbl = new JLabel("Panning");
				JLabel cueLbl = new JLabel("Profile Cue");
				JLabel horizonLbl = new JLabel("Horizon Drawing");
				Font headerFont = ThemeFont.getBold().deriveFont(15f);
				Map att = headerFont.getAttributes();
				att.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
				panLbl.setFont(headerFont.deriveFont(att));
				cueLbl.setFont(headerFont.deriveFont(att));
				horizonLbl.setFont(headerFont.deriveFont(att));
				
				Font infoFont = ThemeFont.getRegular();
				JLabel panInfoLbl = new JLabel("<html>To pan within the full res view use <b>arrow keys</b>.</html>");
				panInfoLbl.setFont(infoFont);
				
				JLabel cueInfoLbl = new JLabel("<html>To move yellow cue line either <b>click</b> anywhere<br>"
											+ "in the image, or <b>click and drag</b> in the image.</html>");
				cueInfoLbl.setFont(infoFont);
				
				JLabel horizonInfoLbl = new JLabel("<html><center>Hold <b>shift and click</b> on the image to start <br>horizon."
												+ "<b><br>Click</b> (while shift is down) to add a vertex to <br>the horizon."
												+ "<b><br>Double click</b> (while shift is down) to <br>complete horizon."
												+ "<br>Press <b>Esc</b> while drawing to remove last added <br>vertex (before "
												+ "horizon has been completed).</center></html>");
				horizonInfoLbl.setFont(infoFont);
				
				int row = 0;
				controlPnl.add(panLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(3), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(panInfoLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(5), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(cueLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(3), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(cueInfoLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(5), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(horizonLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(3), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(horizonInfoLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				controlPnl.add(Box.createVerticalStrut(5), new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				
				
				mainPnl.add(controlPnl);
				fullResControlFrame.setContentPane(mainPnl);
				fullResControlFrame.pack();
				fullResControlFrame.setLocationRelativeTo(fullResFrame);
			}
			//display frame
			fullResControlFrame.setVisible(true);
		}
	};
	
	
	public void setHorizons(ArrayList<RadarHorizon> newHorizons){
		//TODO I think this is the proper way to set it?
		horizons = new ArrayList<RadarHorizon>(newHorizons);
		repaintHorizon();
		parent.repaint();
		horizonPnl.refreshHorizonTable(horizons);
	}
	
	//Mostly copied from ...map2/ChartView
	@SuppressWarnings("serial")
	private AbstractAction CSVAct = new AbstractAction("Save as CSV") {
		public void actionPerformed(ActionEvent e) {
			if(fileChooser == null){
				fileChooser = new JFileChooser(Util.getDefaultFCLocation());
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.setFileFilter(new FileFilter(){
					public boolean accept(File f) {
						String fileName = f.getName();
						int indexOfDot = fileName.lastIndexOf('.');
						if (indexOfDot > -1 && (fileName.substring(indexOfDot).equalsIgnoreCase(".txt") || fileName.substring(indexOfDot).equals(".csv"))){
							return true;
						}
						return false;
					}

					public String getDescription() {
						return "Text Files";
					}
				});
			}
			
			while (true) {
				int rc = fileChooser.showSaveDialog(chartFrame);
				if (rc != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null) {
					break;
				}
				File selected = fileChooser.getSelectedFile();
				if (!selected.exists() ||
						JOptionPane.YES_OPTION == Util.showConfirmDialog("File exists, overwrite?", "File already exists",
							JOptionPane.YES_NO_OPTION)) {
					try {
						saveAsText(fileChooser.getSelectedFile());
					} catch(Exception ex) {
						Util.showMessageDialog("Unable to save file: "+ex.getMessage(), "Error!", JOptionPane.ERROR_MESSAGE);
					}
					break;
				}
			}
		}
	};
	
	
	private void saveAsText(File outputFile) throws FileNotFoundException {
		String delim = ",";
		
		// Output header
		PrintStream ps = new PrintStream(new FileOutputStream(outputFile));
		ps.println(instrument+" Reading at Lat,Lon: "+curLat+", "+curLon);
		ps.println("Line (Pixels)"+delim+"Value (DN)");
		
		
		// Output data
		for (int i=0; i<plotData.length; i++){
			ps.println(i+delim+plotData[i]);
		}
		
		ps.close();
		log.aprintln("File named '"+outputFile.getName()+"' saved.");
		
	}
	
	
	private String getSelectedStampID(){
		if(ss == null){
			return "";
		}
		return ss.getStamp().getId().toLowerCase();
	}
	
	private void updateBrowseImages(){
		//create the panels that display in the layered pane
		BrowseImagePanel bPnl = new BrowseImagePanel(browseImage);
		
		int i = 0;
		browseLp.removeAll();
		browseLp.add(bPnl, new Integer(i++));
		
		if(showOverlay && overlayBrowseImage!=null){
			BrowseImagePanel cPnl = new BrowseImagePanel(overlayBrowseImage);
			browseLp.add(cPnl, new Integer(i++));
		}
		
		browseLp.add(browseDrawPnl, new Integer(i++));
		
		browseLp.repaint();
		
	}
	
	@Override
	public void selectionsChanged() {
		List<StampShape> list = stampLayer.getSelectedStamps();
		//If a selection has been made, and it's different than the previous selection
		if(list.size()>0 && ss != list.get(0)){
			//clear the cluttergram
			overlayBrowseImage = null;
			
			//close the fullres window
			if(fullResFrame != null){
				fullResFrame.setVisible(false);
				fullResFrame = null;
				fullResImage = null;
				overlayFullImage = null;
				fullResPnl = null;
				fullResOverlayPnl = null;
			}
			if(fullResLp != null){
				fullResLp.removeAll();
			}
			
			//get the stamp id
			ss = list.get(0);
			stampId = getSelectedStampID();
			String urlStr = getURL(selectedBaseName, stampId, false, zoom);
			
			//Get image from the server or cache if exists
			BufferedImage bi = StampImageFactory.getImage(urlStr, false);
			
			if(showOverlay && !selectedOverlayName.equals(SELECT_PROD_STR)){
				String cUrl = getURL(selectedOverlayName, stampId, false, zoom);
				overlayBrowseImage = StampImageFactory.getImage(cUrl, false);
			}
			
			if(bi!=null){				
				browseImage = bi;
				Dimension size = new Dimension(browseImage.getRaster().getWidth(), browseImage.getRaster().getHeight());
				browseDrawPnl = new BrowseDrawPanel(size);
				//mouse listener for changing the profile line on the image and updating the lview
				browseDrawPnl.addMouseMotionListener(browseImageListener);
				browseDrawPnl.addMouseListener(browseImageListener);
				
				browseLp.setPreferredSize(size);
				updateBrowseImages();
				
				updateRadarPanel();
				
				//Clear chart and full res image
				clearChartData();
				//reset all full res data
				fullResImage = null;
				//These are commented out so that when switching between rendered
				// images, the location within the image is presevered....
//				fullResXStart = 0;
//				fullResYStart = 0;
				fullResWidth = 0;
				fullResHeight = 0;
				fullResBtn.setEnabled(true);
				if(fullResFrame!=null){
					fullResFrame.setVisible(false);
				}
				parent.highlightChanged(null);
				
				//remove all horizons
				horizons.clear();
				horizonInt = 0;
			}
		}
		
		horizonPnl.refreshHorizonTable(horizons);
	}
	
	private void clearChartData(){
		resetChartData(true, null, null);
		plotData = null;
		fullResNumeric = null;
		chartBtn.setEnabled(numericProducts.contains(selectedBaseName));
	}

	@Override
	public void selectionsAdded(List<StampShape> newStamps) {
		selectionsChanged();	
	}
	
	/**
	 * This is called from the ProfileLineCueingListener on the LView.
	 * 
	 * Set the spatial point for the cue line from the 
	 * profileLineCueingListener.  This point is used to 
	 * find the nearest point in the radar points array
	 * and then draw the profile line on the radar tiff
	 * in the focus panel accordingly.
	 * @param pt Cue line mid point (in spatial coords) from the lview
	 */
	public void setCuePoint(Point2D pt){
		
		double percent = 0;
		double lowIndex = 0;
		
		if(ss!=null && pt!=null){
			//the points the radar data consists of
			double[] points = ss.getStamp().getPoints();
			
			double[] midPt = new double[2];
			midPt[0] = pt.getX();
			if(midPt[0]<0){
				midPt[0] = midPt[0]+360;
			}
			midPt[1] = pt.getY();
			
			
			double difference = 99999;

			//find the point in the radar data that is 
			// closest to the midpoint of the cue line from
			// the lview
			for(int i =0; i<points.length; i=i+2){
				double x_diff = points[i] - midPt[0];
				double y_diff = points[i+1] - midPt[1];
				double diff = Math.sqrt((x_diff*x_diff)+(y_diff*y_diff));
				
				if(diff<difference){
					difference = diff;
					lowIndex = i;
				}
				
			}
			
			//To get the percent:  The points length is twice as long as the
			// actual image, plus the last two entries in it are NaN, NaN.
			// So remove those last two (-2).  Also, the length is actually one 
			// longer than the number of elements in it, so subtract another.
			// This is why denominator is points.length-3.
			// Add 0.5 to the lowIndex because that is the lon index, and the 
			// point is actually defined by lon and lat (lat being 1 more than 
			// lon).  So lowIndex+0.5 is the average between the lat and lon indices.
			// Now divide that index location by the total number of valid locations
			// to get the percent.
			percent = (lowIndex+0.5)/((points.length-3));

			//set the lat lon of the sample, used in the csv and chart
			curLon = points[(int)lowIndex];
			curLat = points[(int)lowIndex+1];
		}
		
		//this cuePercent is used when drawing the cue line
		// in the radar image on the focus panel
		cuePercent = percent;
		//set the sample number used for plotting the chart
		setCurrentSample((int)(lowIndex/2) *10); //the *10 is because the points array is a 10th of the full data
		
	//redraw the focus panel
		updateRadarPanel();
		
	}
	
	
	public void updateRadarPanel(){
		//update id label
		idLbl.setText(idPrompt+ss.getStamp().getId());
		idPnl.repaint();
		//update profile line
		browseDrawPnl.repaint();
		//if full res is used, update profile line as well
		if(fullResFrame!=null && fullResFrame.isVisible()){
			fullResDrawPnl.repaint();
		}
		//update sample readout
		sampleLbl.setText(samplePrompt+curSample);
		samplePnl.repaint();
		//update chart
		updateChart();
	}
	
	private void updateChart(){
		if(fullResNumeric!=null){
			//get the column of pixel data
			int lines = fullResNumeric.getRaster().getHeight();
			plotData = new double[lines];
			
			//populates the plotData array
			fullResNumeric.getRaster().getPixels(curSample, 0, 1, lines, plotData);
			
			//populate the new chart
			DefaultXYDataset data = new DefaultXYDataset();
			Comparable s = instrument;
			double[][] da = new double[2][plotData.length];
			
			//plot the value (plotData) on the x axis
			// and the location (pixel) on the y axis.
			for(int i=0; i<plotData.length; i++){
				da[0][i] = plotData[i];
				da[1][i] = (double) i;
			}
			
			data.addSeries(s, da);
			
			String title = selectedBaseName+" - "+"ID: "+ss.getStamp().getId(); //Title
			resetChartData(false, title, data);
			
			//add the csv save option to popup menu
			JPopupMenu menu = chartPnl.getPopupMenu();
			if(csvSaveItem == null){
				csvSaveItem = new JMenuItem(CSVAct);
				menu.add(new JPopupMenu.Separator());
			}
			menu.add(csvSaveItem);
			chartPnl.setPopupMenu(menu);
		}
	}
	
	private void setCurrentSample(int newSample){
		curSample = newSample;
		if(curSample<0)	curSample=0;
		//if full res has been loaded.
		if(fullResWidth>0){
			if(curSample>fullResWidth) curSample = fullResWidth-1;
		}
		//else use an approximation from the browse
		else{
			int approxWidth = (int)(browseImage.getRaster().getWidth()/scale);
			if(curSample>approxWidth){
				curSample = approxWidth-1;
			}
		}
	}
	
	

	
	private void calculateFullResBounds(MouseEvent e){
		double x = e.getX();
		double y = e.getY();
		//scale to x location to full res pixel value (15%->100%)
		double full_x = x/scale;
		double full_y = y/scale;
		
		//start with the x at 0
		fullResXStart = 0;
		//if the full resolution x of the mouse point minus half the width
		// of the display frame is greater than 0, then use that point.
		if(full_x-fullResPnl.getWidth()/2 > 0){
			fullResXStart = (int)full_x-fullResPnl.getWidth()/2;
		}
		//now if that position is greater than the image size minus the width 
		// of the display window, then set the start to the image width minus
		// display with.
		if(fullResXStart > fullResWidth-fullResPnl.getWidth()){
			fullResXStart = fullResWidth-fullResPnl.getWidth();
		}
		//finally make sure that is greater than zero, if not, set to zero
		if(fullResXStart<0){
			fullResXStart=0;
		}

		//Set the y at the mouse y minus half the height.
		fullResYStart = (int)full_y - fullResPnl.getHeight()/2;
		//if this makes the start less than 0, set it to 0
		if(fullResYStart<0){
			fullResYStart = 0;	
		}
		if(fullResYStart+fullResPnl.getHeight()>fullResHeight){
			fullResYStart = fullResHeight-fullResPnl.getHeight();
		}
		if(fullResYStart<0){
			fullResYStart = 0;
		}
	}
	

	private void updateProfileLine(){
		//Notify and update lview accordingly
		double[] points = ss.getStamp().getPoints();
		
		int lonIndex = (int) Math.round(cuePercent*(points.length-2));
		//lonIndex needs to be an even number
		if(lonIndex%2 == 1){
			lonIndex++;
		}
		int latIndex = lonIndex + 1;
		
		
		//The last two points in the list are NaN,NaN to signify the 
		// end of the shape. So the last real data point is at index
		// length-4 and length-3 which is why we do the following:
		if(lonIndex>=points.length-4){
			lonIndex = points.length-4;
			latIndex = lonIndex + 1;
		}
		if(lonIndex<0){
			lonIndex = 0;
			latIndex = 1;
		}
		
		Point2D spatialPt = new Point2D.Double(points[lonIndex], points[latIndex]);
					
		Point2D worldPt = parent.getProj().spatial.toWorld(spatialPt);
		
		parent.cueChanged(worldPt);
		
		//update focus panel
		updateRadarPanel();
	}
	
	
	/**
	 * Dispose any open frames.
	 */
	public void cleanUp(){
		//Full resolution view
		if(fullResFrame!=null){
			fullResFrame.dispose();
		}
		//Controls for full res
		if(fullResControlFrame!=null){
			fullResControlFrame.dispose();
		}
	}
	
	
	

	private RadarHorizon createHorizonFromPts(){
		//create the list of spatial points from the x points
		Point2D[] points = fullResStampImage.getPoints();
		
		//make a copy of the xPts arraylist to sort and find the max and min x values
		ArrayList<Integer> xCopy = new ArrayList<Integer>(xPts);
		Collections.sort(xCopy);
		
		//calculate the start and end indices based off
		// the sampling rate of the points with respect 
		// to number of pixels in the width of the image
		double samplingRate = (points.length/(double)fullResWidth);
		int scaledPixelStart = (int)(fullResXStart*samplingRate);
		
		int startIndex = (int)(xCopy.get(0)*samplingRate) + scaledPixelStart;
		int endIndex = (int)(xCopy.get(xCopy.size()-1)*samplingRate) + scaledPixelStart;

		//parse out only the part of the full resolution points that we need for the new horizon
		//also, the points returned from StampImage are in degrees W
		ArrayList<Point2D> spatialPts = new ArrayList<Point2D>();
		for(int i=startIndex; i<=endIndex; i++){
			spatialPts.add(points[i]);
		}
		
		//convert the x and y points to be the absolute x and y coordinats
		ArrayList<Integer> xList = new ArrayList<Integer>();
		ArrayList<Integer> yList = new ArrayList<Integer>();
		
		for(int i=0; i<xPts.size(); i++){
			xList.add(xPts.get(i)+fullResXStart);
			yList.add(yPts.get(i)+fullResYStart);
		}
		
		return new RadarHorizon(getSelectedStampID(), xList, yList, spatialPts, samplingRate, horizonInt++, parent.getSettings());
	}
	
	
	private void updateFullResHighlight(){
		//calculate the path shape that needs to be highlighted
		double[] points = ss.getStamp().getPoints();
		
		double startPercent = (double)fullResXStart/(double)fullResWidth;
		double endPercent = (double)(fullResXStart+fullResViewWidth)/(double)fullResWidth;
	
		int startLonIndex = (int) Math.round(startPercent*(points.length-2));
		//lon index needs to be an even number
		if(startLonIndex%2 == 1){
			startLonIndex++;
		}
		
		int endLonIndex = (int) Math.round(endPercent*(points.length-2));
		//lon index needs to be an even number
		if(endLonIndex%2 == 1){
			endLonIndex++;
		}
		
		//The last two points in the list are NaN,NaN to signify the 
		// end of the shape. So the last real data point is at index
		// length-4 and length-3 which is why we do the following:
		if(endLonIndex>=points.length-4){
			endLonIndex = points.length-4;
		}
		int endLatIndex = endLonIndex + 1;
		
		//build an array with all the spatial points
		ArrayList<Point2D> spPoints = new ArrayList<Point2D>();
		for(int i=startLonIndex; i<=endLatIndex; i=i+2){
			Point2D spatialPt = new Point2D.Double(points[i], points[i+1]);
			spPoints.add(spatialPt);
		}

		//update the lview
		parent.highlightChanged(spPoints);
	}
	
	
	public ArrayList<RadarHorizon> getHorizons(){
		return horizons;
	}
	
	
	/**
	 * @return The selected horizon in the horizon table from 
	 * the horizon panel (manage horizon tab)
	 */
	public RadarHorizon getSelectedHorizon(){
		return horizonPnl.getSelectedHorizon();
	}
	
	
	
	//Override one method of a Layout class that was created to be used
	// with layered panes.  This allows their container to know their real
	// size.  So that they actually work inside scrollpanes (used with the 
	// BrowseImagePanel and BrowseDrawPanel.  The layout that is extended 
	// also allows the contents of the layered pane to expand when their 
	// container expands (as used with the FullResImagePanel and FullResDrawPanel.
	private class DrawLayout extends OverlapLayout{
		public Dimension preferredLayoutSize(Container parent)
		 {			
			if(parent.getComponentCount()>0){
				//return the size of the browseDrawPanel which should
				// be the correct size of the radar gram.  This will 
				// allow the scroll pane to work properly.
				Component c = parent.getComponent(1);				
				return c.getPreferredSize();
			}
			else{
				return new Dimension(200,200);
			}
		 }
	}
	
	public void repaintHorizon(){
		browseDrawPnl.repaint();
		fullResDrawPnl.repaint();
	}
	
// ------------------------------------------------------------------------- //	
// Below is all the code for the extended JPanel classes and the mouse and 
// key listeners for both the browse image and the full resolution image.
// ------------------------------------------------------------------------- //
	
	private class BrowseDrawPanel extends JPanel{
		
		int height;
		int width;
		
		BrowseDrawPanel(Dimension size){
			setOpaque(false);
			height = (int)size.getHeight();
			width = (int)size.getWidth();
		}
		
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			
			Graphics2D g2 = (Graphics2D) g;
			
			//draw the profile line	
			g2.setColor(Color.YELLOW);
			
			int x = (int)(width*cuePercent);

			if(cuePercent>1){
				x=width-1;
			}
			if(cuePercent <=0){
				x=0;
			}
			
			g2.drawLine(x, 0, x, height);
			
			
			//draw the full res box if it's showing
			if(fullResFrame!=null && fullResFrame.isVisible()){
				//convert the full res coordinates back to the browse size
				// which is 15% (3/20).
				int x1 = (int)(fullResXStart*scale);
				int y1 = (int)(fullResYStart*scale);
				int w = (int)(fullResPnl.getWidth()*scale);
				int h = (int)(fullResPnl.getHeight()*scale);
				
				g2.setColor(Color.WHITE);
				g2.drawRect(x1, y1, w, h);
			}
			
			//draw horizons
			for(RadarHorizon h : horizons){
				if(h.isVisible()){
					int[] xtmp = h.getXPoints();
					int[] ytmp = h.getYPoints();
					int[] xList = new int[xtmp.length];
					int[] yList = new int[ytmp.length];
					
					for(int i=0; i<xtmp.length; i++){
						xList[i] = (int)(xtmp[i]*scale);
						yList[i] = (int)(ytmp[i]*scale);
					}
					
					//change the color if this is the selected horizon
					if(h.equals(getSelectedHorizon())){
						g2.setColor(new Color(~h.getColor().getRGB()));
					}else{
						g2.setColor(h.getColor());
					}
					g2.setStroke(new BasicStroke(h.getBrowseWidth()));
					g2.drawPolyline(xList, yList, xList.length);
				}
			}
		}
	}
	
	private class FullResDrawPanel extends JPanel{
		
		private FullResDrawPanel(){
			setOpaque(false);
		}
		
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			
			Graphics2D g2 = (Graphics2D) g;
			
			int viewHeight = fullResPnl.getHeight();
			
			//draw the profile line
			//Calculate where in the entire image the cue line is supposed to be
			int x = (int)(fullResWidth*cuePercent);

			if(cuePercent>1){
				x=fullResWidth-1;
			}
			if(cuePercent <=0){
				x=0;
			}
			
			//find out if that part of the image is currently visible
			if(x>=fullResXStart && x<=(fullResXStart+fullResViewWidth)){
				g2.setColor(Color.YELLOW);
				
				int relX = x-fullResXStart;
				
				g2.drawLine(relX, 0, relX, viewHeight);
			}
			
			
			//draw the current horizon
			if(!xPts.isEmpty()){
				g2.setColor(horizonTmpColor);
				
				int arraySize = xPts.size();
				int[] intX = new int[arraySize+1];
				int[] intY = new int[arraySize+1];
				for(int i=0; i<arraySize; i++){
					intX[i] = xPts.get(i);
					intY[i] = yPts.get(i);
				}
				
				if(xEnd>-1 && yEnd>-1){
					intX[xPts.size()] = xEnd;
					intY[xPts.size()] = yEnd;
					arraySize++;
				}
				
				g2.drawPolyline(intX, intY, arraySize);
			}
			
			
			//draw existing horizons
			for(RadarHorizon rh : horizons){
				if(rh.isVisible()){
					//change the color if this is the selected horizon
					if(rh.equals(getSelectedHorizon())){
						g2.setColor(new Color(~rh.getColor().getRGB()));
					}else{
						g2.setColor(rh.getColor());
					}
					g2.setStroke(new BasicStroke(rh.getFullResWidth()));
					g2.drawPolyline(rh.getXPointsForStartingX(fullResXStart), rh.getYPointsForStartingY(fullResYStart), rh.getNumberOfPoints());
				}
			}
		}
	}
	
	
	private class BrowseImagePanel extends JPanel{
		private BufferedImage myImage;
		
		public BrowseImagePanel(BufferedImage img){
			myImage = img;
			Dimension size = new Dimension(myImage.getRaster().getWidth(), myImage.getRaster().getHeight());
			setSize(size);
		}
		
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			
			g.drawImage(myImage, 0, 0, null);
		}
	}
	
	
	
	private class FullResImagePanel extends JPanel{
		private BufferedImage myImage;
		
		public FullResImagePanel(BufferedImage img){
			myImage = img;
		}
		
		public void paintComponent(Graphics g){
			super.paintComponent(g);
			
			//use the width and height of the full res
			// display frame to get the size of the subimage.
			// Make sure the subimage dimensions are no larger
			// than the buffered image height and width.
			fullResViewWidth = fullResLp.getWidth();
			if(fullResViewWidth>fullResWidth){
				fullResViewWidth = fullResWidth;
			}
			int height = fullResLp.getHeight();
			if(height>fullResHeight){
				height = fullResHeight;
			}
			
			//if a redraw gets triggered by a resize, we won't have
			// the logic from the mouse moved event, so make sure that
			// the cropping bounds still fit inside the fullresimage
			if(fullResXStart+fullResViewWidth>fullResWidth){
				fullResXStart = fullResWidth-fullResViewWidth;
			}
			if(fullResYStart+height>fullResHeight){
				fullResYStart = fullResHeight-height;
			}

			//crop the image
			BufferedImage myImg = myImage.getSubimage(fullResXStart, fullResYStart, fullResViewWidth, height);
			
			//draw the image
			g.drawImage(myImg, 0, 0, null);
			
			//update the outline on the profile panel
			browseDrawPnl.repaint();
			
			//update the highlight on the lview
			updateFullResHighlight();
		}
	}
	
	

	private MouseInputListener browseImageListener = new MouseInputListener() {
		
		public void mouseDragged(MouseEvent e) {
			//Allow user to drag profile line on image
			double x = e.getX();
			cuePercent = x/browseImage.getRaster().getWidth();
			
			//current sample is 15% of the real image.
			setCurrentSample((int)(x/scale));

			updateProfileLine();
		}
		
		public void mouseReleased(MouseEvent e) {
			//if the full res window is open
			if(e.isShiftDown() &&  fullResFrame != null && fullResFrame.isVisible()){
				
				//calculate the bounds
				calculateFullResBounds(e);
				
				//update the full res view
				fullResPnl.repaint();
			}
		}
		public void mousePressed(MouseEvent e) {
		}
		public void mouseMoved(MouseEvent e) {
			//if the full res window is open
			if(e.isShiftDown() && fullResFrame != null && fullResFrame.isVisible()){
				
				//calculate the bounds
				calculateFullResBounds(e);
				
				//update the box
				browseDrawPnl.repaint();
				
				//update the full res view
				fullResLp.repaint();
			}
		}
		public void mouseExited(MouseEvent e) {
		}
		public void mouseEntered(MouseEvent e) {
		}
		public void mouseClicked(MouseEvent e) {
			//Allow user to drag profile line on image
			double x = e.getX();
			cuePercent = x/browseImage.getWidth();
			
			//current sample is 15% of the real image.
			setCurrentSample((int)(x/scale));
			
			updateProfileLine();
		}
	};
	
	
	private MouseInputListener fullResImageListener = new MouseInputListener() {
		
		public void mouseDragged(MouseEvent e) {
			//allow the profile line to be adjusted in full res
			//relative pixel location as long as the user is not
			//drawing a horizon
			if(!isDrawingHorizon){
				double relX = e.getX();
				//adjust to aboslute pixel location in image
				double x = relX + fullResXStart;
				cuePercent = x/fullResWidth;
				
				setCurrentSample((int)x);
	
				updateProfileLine();
			}
		}
		
		public void mouseReleased(MouseEvent e) {
		}
		public void mousePressed(MouseEvent e) {
		}
		public void mouseMoved(MouseEvent e) {
			if(isDrawingHorizon){
				xEnd = e.getX();
				yEnd = e.getY();
				fullResDrawPnl.repaint();
			}
		}
		public void mouseExited(MouseEvent e) {
		}
		public void mouseEntered(MouseEvent e) {
			fullResFrame.requestFocus();
		}
		public void mouseClicked(MouseEvent e) {

			
			//Draw new horizon
			//Start new horizon or add a vertice
			if(e.getClickCount()==1 && e.isShiftDown()){
				
				isDrawingHorizon = true;
				int x = e.getX();
				int y = e.getY();
				xPts.add((int)x);
				yPts.add(y);
				
				xEnd = x;
				yEnd = y;
				
				
				fullResDrawPnl.repaint();
			}
			//close the horizon
			else if(e.getClickCount() == 2 && isDrawingHorizon && e.isShiftDown()){
				
				xEnd = -1;
				yEnd = -1;
				
				//create some sort of horizon object.
				RadarHorizon rh = createHorizonFromPts();
				horizons.add(rh);
				//call an update of the horizon panel when a new horizon is created
				horizonPnl.refreshHorizonTable(horizons);
				
				//add the horizon to the filledstamp object
				((FilledStampRadarTypeFocus)parent.getFocusPanel().getRenderedView()).getFilledStamp().addHorizon(rh);
				
				
				//clear temp horizon data
				xPts.clear();
				yPts.clear();
				isDrawingHorizon = false;
				fullResDrawPnl.repaint();
				
				//increase state id on the selections buffer
				stampLayer.increaseStateId(StampLayer.SELECTIONS_BUFFER);
				//redraw the 3d view if needed
				if(parent.getLView3D().isEnabled()){
					ThreeDManager mgr = ThreeDManager.getInstance();
					//If the 3d is already visible, update it
					if(ThreeDManager.isReady() && parent.getLView3D().isVisible()){
						mgr.updateDecalsForLView(parent, true);
					}
				}
				
			}
			
			//move the cue line
			else if (!isDrawingHorizon){
				//allow the profile line to be adjusted in full res
				//relative pixel location
				double relX = e.getX();
				//adjust to aboslute pixel location in image
				double x = relX + fullResXStart;
				cuePercent = x/fullResWidth;
				
				setCurrentSample((int)x);
				
				updateProfileLine();
			}
		}
		
	};
	
	private KeyListener fullResKeyListener = new KeyListener() {
		public void keyTyped(KeyEvent e) {
			//use escape to remove last point placed for horizon
			if(e.getKeyChar() == KeyEvent.VK_ESCAPE){
				int size = xPts.size();
				if(size>1){
					xPts.remove(size-1);
					yPts.remove(size-1);
				}else{
					xPts.clear();
					yPts.clear();
					xEnd = -1;
					yEnd = -1;
					isDrawingHorizon = false;
				}
				fullResDrawPnl.repaint();
			}
		}
		public void keyReleased(KeyEvent e) {
		}
		
		public void keyPressed(KeyEvent e) {
			//up
			if(e.getKeyCode() == KeyEvent.VK_UP){
				fullResYStart = fullResYStart - 10;
				if(fullResYStart<0){
					fullResYStart = 0;
				}
				fullResPnl.repaint();
			}
			//down
			if(e.getKeyCode() == KeyEvent.VK_DOWN){
				fullResYStart = fullResYStart + 10;
				if(fullResYStart>fullResHeight-fullResPnl.getHeight()){
					fullResYStart = fullResHeight - fullResPnl.getHeight();
				}
				fullResPnl.repaint();
			}
			//left
			if(e.getKeyCode() == KeyEvent.VK_LEFT){
				fullResXStart = fullResXStart - 10;
				if(fullResXStart<0){
					fullResXStart = 0;
				}
				fullResPnl.repaint();
			}
			//right
			if(e.getKeyCode() == KeyEvent.VK_RIGHT){
				fullResXStart = fullResXStart + 10;
				if(fullResXStart>fullResWidth-fullResPnl.getWidth()){
					fullResXStart = fullResWidth-fullResPnl.getWidth();
				}
				fullResPnl.repaint();
			}
		}
	};
}