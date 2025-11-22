package edu.asu.jmars.layer.crater.profiler;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.AbstractDocument;
import javax.swing.text.DocumentFilter;

import org.jdesktop.swingx.JXTaskPane;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.asu.jmars.Main;
import edu.asu.jmars.ProjObj;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.crater.Crater;
import edu.asu.jmars.layer.crater.CraterLView;
import edu.asu.jmars.layer.crater.Samples;
import edu.asu.jmars.layer.map2.MapChannelReceiver;
import edu.asu.jmars.layer.map2.MapChannelTiled;
import edu.asu.jmars.layer.map2.MapData;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.util.NumericMapSourceDialog;
import edu.asu.jmars.layer.util.features.FPath;
import edu.asu.jmars.swing.DocumentCharFilter;
import edu.asu.jmars.swing.DocumentIntFilter;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeChart;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;

public class ProfilerView extends JPanel{
	//Related LView
	private CraterLView myLView;
	
	// JFreeChart related stuff
	private JFreeChart chart;
	private ChartPanel chartPanel;
	private String xAxisPrompt = "Distance from center (km)";
	private String yAxisLabel;
	private JCheckBoxMenuItem averageChk;
	
	private int AVG_RECORD_INDEX;
	
	private JFrame myFrame;
	private int frameMinW = 590;
	private int frameMinH = 400;
	
	private final String LEGEND_TIP = "These numbers correspond to the CSV export and the colors plotted above";
	
	private JXTaskPane optionsPane;
	private JButton sourceBtn;
	private JComboBox<Integer> lineNumBx;
	private JComboBox<Integer> lengthBx;
	private JTextField lengthTf;
	private JRadioButton percRad;
	private JRadioButton kmRad;
	private ButtonGroup lengthGroup;
	private JPanel kmPnl;
	private JPanel percPnl;
	private JPanel lengthPnl;
	private final String KM_STR = "km";
	private final String PERC_STR = "perc";
	private JTextField offsetTf;
	
	private int numDefault = 6;
	private int lengthDefault = 120;
	private MapSource source;
	
	private DecimalFormat format = new DecimalFormat("#.###");
    private int row = 0;
    private int pad = 1;
    private Insets in = new Insets(pad,pad,pad,pad);
	
	private DebugLog log = DebugLog.instance();
	
	private ArrayList<ProfileData> allObjs = new ArrayList<ProfileData>();	
	
	private HashMap<Integer, Boolean> exportProfileMap = new HashMap<Integer, Boolean>();
	private boolean expIndFiles = false;
	private JFrame optionsFrame;
	
	private Crater lastCrater;
	private int lastPpd;
	private MapSource lastSource;
	private ProjObj lastProj;
	private int lastNumLines;
	private double lastLength;
	private double lastOffset;

	private Crater selectedCrater;
    private HashMap<ProfileData, Shape> profileDataToPaths = new HashMap<ProfileData, Shape>();
	private HashMap<Shape, Color> pathsToColor = new HashMap<Shape, Color>();
	
	private ThemeProvider themeProvider = ThemeProvider.getInstance();
    
    public ProfilerView(CraterLView craterLView){
    	myLView = craterLView;
    	source = getDefaultSource();
    	updateSourceLabels();
    	
    	buildUI();
    }
    
    
    private void buildUI(){
		//center panel is the chart
		chart = ChartFactory.createXYLineChart(
				source.getTitle(),			//Title
				xAxisPrompt,	//x axis
				yAxisLabel,				//y axis
				new XYSeriesCollection(),	//dataset
				PlotOrientation.VERTICAL,	//orientation
				true,			//legend
				true,			//tooltips
				false);			//urls
		
		((NumberAxis)chart.getXYPlot().getRangeAxis()).setAutoRangeIncludesZero(false);
		
		ThemeChart.configureUI(chart);
		XYItemRenderer rr = ((XYPlot) chart.getPlot()).getRenderer();
		rr.setSeriesPaint(0, ThemeChart.getPlotColor());
		
		chartPanel = new ChartPanel(chart, true);
		chartPanel.setBorder(new EmptyBorder(5,5,5,5));
		chartPanel.getPopupMenu().add(createCSVExportOptionsItem());
		chartPanel.getPopupMenu().add(createCSVMenuItem());
		averageChk = createAverageMenuItem();
		chartPanel.getPopupMenu().add(averageChk);
		
		
		JPanel inputPnl = new JPanel(new BorderLayout());
		//TODO: possibly remove this if the jxtaskpane gets changed in the theme or library
		inputPnl.setBorder(new LineBorder(Color.DARK_GRAY, 2));
		//source button is instantiated when the default source is loaded
		if(sourceBtn == null){
			sourceBtn = new JButton(sourceAct);
		}
		JLabel linesLbl = new JLabel("Number of Lines:");
		lineNumBx = new JComboBox<Integer>(getLineOptions());
		lineNumBx.setSelectedItem(numDefault);
		lineNumBx.addActionListener(optionListener);
		JLabel lengthLbl = new JLabel("Length of Lines  (");
		lengthBx = new JComboBox<Integer>(getLengthOptions());
		lengthBx.setSelectedItem(lengthDefault);
		lengthBx.addActionListener(optionListener);
		lengthBx.setToolTipText("Length compared to radius of the crater");
		lengthTf = new JTextField(6);
		lengthTf.setText("0");
		//add number filter
		DocumentFilter filter = getNumberFilter();
		((AbstractDocument)lengthTf.getDocument()).setDocumentFilter(filter);
		lengthTf.addFocusListener(textFieldFocusListener);
		lengthTf.addActionListener(optionListener);
		percRad = new JRadioButton("%");
		percRad.addActionListener(radioListener);
		percRad.setToolTipText("Length compared to radius of the crater");
		percRad.setSelected(true);
		kmRad = new JRadioButton("km");
		kmRad.addActionListener(radioListener);
		lengthGroup = new ButtonGroup();
		lengthGroup.add(percRad);
		lengthGroup.add(kmRad);
		kmPnl = new JPanel();
		kmPnl.add(lengthTf);
		percPnl = new JPanel();
		percPnl.add(lengthBx);
		//use a card layout to be able to switch between the
		// length dropdown and length text field
		lengthPnl = new JPanel(new CardLayout());
		lengthPnl.add(percPnl, PERC_STR);
		lengthPnl.add(kmPnl, KM_STR);
		JLabel offsetLbl = new JLabel("Angular offset:");
		offsetLbl.setToolTipText("Should be an integer value between 0-360");
		offsetTf = new JTextField(3);
		offsetTf.setText("0");
		offsetTf.setToolTipText("Should be an integer value between 0-360");
		((AbstractDocument)offsetTf.getDocument()).setDocumentFilter(new DocumentIntFilter(3));
		offsetTf.addActionListener(optionListener);
		offsetTf.addFocusListener(textFieldFocusListener);
		
		JPanel inputTopPnl = new JPanel();
		inputTopPnl.add(sourceBtn);
		inputTopPnl.add(Box.createHorizontalStrut(10));
		inputTopPnl.add(linesLbl);
		inputTopPnl.add(lineNumBx);
		inputTopPnl.add(Box.createHorizontalStrut(10));
		inputTopPnl.add(offsetLbl);
		inputTopPnl.add(offsetTf);
		
		JPanel inputBotPnl = new JPanel();
		inputBotPnl.add(lengthLbl);
		inputBotPnl.add(percRad);
		inputBotPnl.add(kmRad);
		inputBotPnl.add(new JLabel(") :"));
		inputBotPnl.add(lengthPnl);
		
		inputPnl.add(inputTopPnl, BorderLayout.NORTH);
		inputPnl.add(inputBotPnl, BorderLayout.SOUTH);
		
		optionsPane = new JXTaskPane();
		optionsPane.setTitle("Options");
		optionsPane.add(inputPnl);

		//add to this		
		setLayout(new GridBagLayout());
		setBorder(new EmptyBorder(5, 5, 5, 5));
		
		row = 0;
		add(chartPanel, new GridBagConstraints(0, row, 1, 1, 1, .8, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		add(optionsPane, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, in, pad, pad));
    }
    
    private ActionListener radioListener = new ActionListener() {
		public void actionPerformed(ActionEvent arg0) {
			//when a radio button changes, change the layout to 
			// the appropriate panel, and trigger a regeneration
			// of the data and lview repaint if valid
			CardLayout cl = (CardLayout)lengthPnl.getLayout();
			if(percRad.isSelected()){
				cl.show(lengthPnl, PERC_STR);
				generateData();			
				myLView.drawSelectedCraters();
				myLView.repaint();
			}
			else if(kmRad.isSelected()){
				cl.show(lengthPnl, KM_STR);
				if(validateTextField(lengthTf, false)){
					generateData();			
					myLView.drawSelectedCraters();
					myLView.repaint();
				}
			}
		}
	};
    
	private DocumentFilter getNumberFilter(){
		ArrayList<Character> filterList = new ArrayList<Character>();
		filterList.add('0');
		filterList.add('1');
		filterList.add('2');
		filterList.add('3');
		filterList.add('4');
		filterList.add('5');
		filterList.add('6');
		filterList.add('7');
		filterList.add('8');
		filterList.add('9');
		filterList.add('.');
		
		return new DocumentCharFilter(filterList, true);
	}
    
    private AbstractAction sourceAct = new AbstractAction("Select Numeric Source") {
		
		@Override
		public void actionPerformed(ActionEvent arg0) {
			ArrayList<MapSource> sourceArray = NumericMapSourceDialog.getUserSelectedSources(ProfilerView.this, false, true);
			if(sourceArray.size()>0){
				MapSource elevSource = sourceArray.get(0);
				//source can be null if the user cancels out of the dialog
				if(elevSource!=null){
					source = elevSource;
					updateSourceLabels();
					generateData();
				}
			}
		}
	};
    
	private void updateSourceLabels(){
		if(sourceBtn == null){
			sourceBtn = new JButton(sourceAct);
		}
		sourceBtn.setToolTipText("Selected source: "+source.getTitle());
		
		//update the y-axis label
		//check mapsource to properly create the y-axis label
		if(source.hasElevationKeyword()){
			yAxisLabel = "Elevation ("+source.getUnits()+")";
		}
		else{
			yAxisLabel = source.getUnits();
		}
	}
	
    /**
     * Generates a vector with integer options for the number of lines the
     * user can choose to generate
     * @return Currently returns a vector with the values 1-10
     */
    private Vector<Integer> getLineOptions(){
    	Vector<Integer> linesVec = new Vector<Integer>();
    	
    	for (int i=1; i<21; i++){
    		linesVec.add(i);
    	}
    	
    	return linesVec;
    }
    
    /**
     * Generates a vector with integer options for the length of lines the
     * user can choose to generate (as a percent of the crater's radius)
     * @return Currently returns a vector with the values 80-150 
     * in increments of 10
     */
    private Vector<Integer> getLengthOptions(){
    	Vector<Integer> lengthVec = new Vector<Integer>();
    	
    	for (int i=80; i<160; i=i+10){
    		lengthVec.add(i);
    	}
    	
    	return lengthVec;
    }
    
    private ActionListener optionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == lengthTf || e.getSource() == offsetTf){
				if(!validateTextField((JTextField)e.getSource(), true)){
					//validation failed, so do not generate new data
					return;
				}
			}
			
			generateData();			
			myLView.drawSelectedCraters();
			myLView.repaint();
		}
	};
	
	private FocusListener textFieldFocusListener = new FocusListener() {
		public void focusLost(FocusEvent e) {
			if(!validateTextField((JTextField)e.getSource(), true)){
				//failed validation, don't regenerate data
				return;
			}
			generateData();			
			myLView.drawSelectedCraters();
			myLView.repaint();
		}
		public void focusGained(FocusEvent arg0) {
			//do nothing
		}
	};
	
	private boolean validateTextField(JTextField tf, boolean showError){
		boolean result = true;
		
		//if the text field is empty, set it to 0 and return true
		if(tf.getText().length()==0){
			tf.setText("0");
			return result;
		}
		
		//do something to make sure the input is a valid double
		try{
			Double.parseDouble(tf.getText());
		}
		catch(Exception e){
			result = false;
			if(showError){
				Util.showMessageDialog("Input must be a valid number greater than zero.", "Invalid Length", JOptionPane.ERROR_MESSAGE);
			}
		}
		
		return result;
	}
    
    
	private MapSource getDefaultSource() {
	    String defaultServer = Config.get("threed.default_elevation.server");
	    String defaultSource = Config.get("threed.default_elevation.source");
		String serverName = Config.get(Util.getProductBodyPrefix() + "threed.default_elevation.server", defaultServer);
		String sourceName = Config.get(Util.getProductBodyPrefix() + "threed.default_elevation.source", defaultSource);
		
		
		MapServer server = MapServerFactory.getServerByName(serverName);
		if (server == null) {
			log.aprintln("Elevation server not accessible");
			return null;
		}
		
		MapSource source = server.getSourceByName(sourceName);
		if (source == null) {
			log.aprintln("Elevation source not accessible");
			return null;
		}
		
		return source;
	}
    
	private synchronized void generateData(){

		MapSource mapSource = getMapSource();
		
        int ppd = Main.testDriver.mainWindow.getZoomManager().getZoomPPD();

        int numLines = getLineNumbers();
        
        double length = getProfileLength();
        
        double offset = getAngularOffset(); // getOffset
        
		// Nothing has changed
		if (selectedCrater==lastCrater && ppd==lastPpd && mapSource==lastSource && Main.PO==lastProj
				&& lastNumLines==numLines && lastLength==length && lastOffset==offset) {
			return;
		}
		//re-calculating profiles so clear color map
		allObjs.clear();
		pathsToColor.clear();

		lastCrater = selectedCrater;
		lastPpd = ppd;
		lastSource = mapSource;
		lastProj=Main.PO;
		lastNumLines = numLines;
		lastLength = length;
		lastOffset = offset;
		
		
		//if the selected crater is null at this point, do not do any
		// map sampling and clear the chart by refreshing profile data
		if(selectedCrater == null){
			allObjs.clear();
			refreshProfileData(true);
			return;
		}
		
		//clear the profile export map
		exportProfileMap.clear();
		
		Point2D radialPoints[] = selectedCrater.getRadialPoints(getLineNumbers(), length, offset);
		Shape paths[] = new Shape[radialPoints.length];
		int idx=0;
		for (Point2D point : radialPoints) {
			//set the default export to true
			exportProfileMap.put(idx, true);
			
			Point2D centerPoint = Main.PO.convSpatialToWorld(360-selectedCrater.getLon(), selectedCrater.getLat());
			Point2D worldRadial = Main.PO.convSpatialToWorld(point);
			Point2D vertices[] = new Point2D[2];
			vertices[0]=centerPoint;
			vertices[1]=worldRadial;
			FPath radialLine = new FPath(vertices, FPath.WORLD, false);
			paths[idx++]=radialLine.getShape();
		}
		
		//add one last export option for the avg path
		exportProfileMap.put(idx, true);
		//refresh the options frame if it's visible
		if(optionsFrame!=null && optionsFrame.isVisible()){
			refreshOptionsFrame();
		}
		
		Samples samples = new Samples(paths, 0.0, 1.0, ppd);
		
		MapSampler sampler = new MapSampler(samples, mapSource, ppd);
		Thread t = new Thread(sampler);
		t.run();
	}

	
	private synchronized void refreshProfileData(boolean refreshLView){
		
		//Whether there are new profile objects to plot or not
		// the chart panel needs to be cleared
		chartPanel.removeAll();
		
		//if there are profile lines to plot, then plot them
		// and keep track of their respective colors in the map,
		// refresh the lview at the end to draw the profile lines
		// with the proper colors
		if(allObjs.size()>0){
			XYSeriesCollection data_series = new XYSeriesCollection();
			
			//if the average option is selected, also plot that
			if(averageChk.isSelected()){
				//calculate the average and display it in the chart
				ProfileData avgPd = avgSpectra(allObjs);
				allObjs.add(avgPd);
				//set the index
				AVG_RECORD_INDEX = allObjs.indexOf(avgPd);
			}
			
			for(ProfileData pd : allObjs){
				XYSeries data = new XYSeries(pd.getName());
				for(int i=0; i<pd.getYValues().length; i++){
					data.add(pd.getXValues()[i], pd.getYValues()[i]);
				}
				data_series.addSeries(data);
			}
			
			//recreate the chart
	        chart = ChartFactory.createXYLineChart(
	        		source.getTitle(),		//title
	        		xAxisPrompt,		 		//x axis label
	        		yAxisLabel,		 	//y axis label
	        		data_series,			 		//data
	        		PlotOrientation.VERTICAL,	//orientation
	        		true,		 				//legend
	        		true, 						//tooltips
	        		false);						//urls
	        
			((NumberAxis)chart.getXYPlot().getRangeAxis()).setAutoRangeIncludesZero(false);

	        
	        ThemeChart.configureUI(chart);
			XYItemRenderer rr = ((XYPlot) chart.getPlot()).getRenderer();
			rr.setSeriesPaint(0, ThemeChart.getPlotColor());
			
			//configure the legend display
			chart.getLegend().setBackgroundPaint(themeProvider.getBackground().getMain());
			chart.getLegend().setBorder(new BlockBorder(themeProvider.getBackground().getBorder()));
			
			//if the average is being shown, set it's color to the main color 
			// of the theme (white -- for the dark theme)
			if(averageChk.isSelected()){
				Color c = ThemeProvider.getInstance().getText().getMain();
				rr.setSeriesPaint(AVG_RECORD_INDEX, c);
			}
			
			LegendItemCollection legendCollection = new LegendItemCollection();
			//cycle through the data to get the correct color for each profile
			//also set the proper legend item for each profile
			for(int i=0; i<allObjs.size(); i++){
				Color c = (Color)rr.getItemPaint(i, 0);
				Shape path = profileDataToPaths.get(allObjs.get(i));
				pathsToColor.put(path, c);
				
				//set the custom legend item so there is no shape, the
				// label is bold, and is the color of the profile line
				LegendItem li = new LegendItem(allObjs.get(i).getName());
				li.setLabelPaint(c);
				li.setLabelFont(ThemeFont.getBold());
				li.setShapeVisible(false);
				legendCollection.add(li);
			}
			
			//set the legend on the chart
			chart.getXYPlot().setFixedLegendItems(legendCollection);
			
	        chartPanel.setChart(chart);
	        
	        XYPlot plot = (XYPlot) chart.getPlot();
	        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
	        
	        renderer.setBaseToolTipGenerator(new XYToolTipGenerator() {
				public String generateToolTip(XYDataset dataset, int series, int item) {
					
					String x = format.format(dataset.getX(series, item));
					String y = format.format(dataset.getY(series, item));
					String name = dataset.getSeriesKey(series).toString();
					
					return name+": "+x+", "+y;
				}
			});
	        
	        
	        if(refreshLView){
		        //redraw the lview now that the profile colors are set
		        myLView.drawSelectedCraters();
		        myLView.repaint();
	        }
		}
		//otherwise, there is non data to show, so clear the chart 
		// and chart panel
		else{
	        chart = ChartFactory.createXYLineChart(
	        		source.getTitle(),		//title
	        		xAxisPrompt,		 		//x axis label
	        		yAxisLabel,		 	//y axis label
	        		null,			 		//data
	        		PlotOrientation.VERTICAL,	//orientation
	        		true,		 				//legend
	        		true, 						//tooltips
	        		false);						//urls
	        
	        ThemeChart.configureUI(chart);
			chartPanel.setChart(chart);
			
		}
	}
	
	/**
	 * Updates the selected crater for the profiler and then regenerates
	 * the plot data
	 * @param newCrater New crater selection
	 */
	public void updateSelectionFromLView(Crater newCrater){
		selectedCrater = newCrater;
		generateData();
	}
	
	/**
	 * @return The map of profile paths to their colors (matches those
	 * in the profiler chart)
	 */
	public synchronized HashMap<Shape, Color> getPathToColorMap(){
		return (HashMap<Shape, Color>) pathsToColor.clone();
	}
    
	private JMenuItem createCSVExportOptionsItem(){
		JMenuItem item = new JMenuItem("Open CSV Export Options...");
		
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(optionsFrame == null){
					optionsFrame = new JFrame("CSV Export Options");
				}
				
				refreshOptionsFrame();
				
				optionsFrame.setLocationRelativeTo(chartPanel);
				optionsFrame.setVisible(true);
			}
		});
		
		return item;
	}
	
	private void refreshOptionsFrame(){
		JPanel mainPnl = new JPanel();
		mainPnl.setLayout(new BorderLayout());
		mainPnl.setBorder(new EmptyBorder(10,10,10,10));
		
		JPanel profilesPnl = new JPanel();
		profilesPnl.setLayout(new GridLayout(1, 1));
		profilesPnl.setBorder(new CompoundBorder(new TitledBorder("Select desired profiles for export"), new EmptyBorder(5, 5, 5, 5)));
		int maxIndex = exportProfileMap.size();
		if (!averageChk.isSelected()){
			maxIndex--;
		}
		int rows = (int)Math.round(maxIndex/2.0);
		JPanel linesPnl = new JPanel(new GridLayout(rows, 2));

		for(int i=0; i<maxIndex; i++){
			JCheckBox cb;
			if(averageChk.isSelected() && i==maxIndex-1){
				cb = new JCheckBox("Avg Profile");
			}else{
				cb = new JCheckBox("Profile "+i);
			}
			cb.setSelected(exportProfileMap.get(i));
			final int index = i;
			cb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					exportProfileMap.put(index, cb.isSelected());
				}
			});
			linesPnl.add(cb);
		}
		JScrollPane linesSp = new JScrollPane(linesPnl);
		profilesPnl.add(linesSp);
		
		JPanel filePnl = new JPanel();
		filePnl.setBorder(new CompoundBorder(new EmptyBorder(10, 0, 0, 0), new TitledBorder("Select export file option")));
		ButtonGroup fileGroup = new ButtonGroup();
		JRadioButton oneFileRad = new JRadioButton("One file for all profiles");
		oneFileRad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				expIndFiles = !oneFileRad.isSelected();
			}
		});
		JRadioButton multiFileRad = new JRadioButton("One file for each profile");
		multiFileRad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				expIndFiles = multiFileRad.isSelected();
			}
		});
		fileGroup.add(oneFileRad);
		fileGroup.add(multiFileRad);
		if(expIndFiles == true){
			multiFileRad.setSelected(true);
		}else{
			oneFileRad.setSelected(true);
		}
		filePnl.add(oneFileRad);
		filePnl.add(multiFileRad);
		
		mainPnl.add(profilesPnl, BorderLayout.CENTER);
		mainPnl.add(filePnl, BorderLayout.SOUTH);
		optionsFrame.setContentPane(mainPnl);
		optionsFrame.revalidate();
		optionsFrame.pack();
	}
	
	private JMenuItem createCSVMenuItem(){
		JMenuItem item = new JMenuItem("Save as CSV");
		
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser(Util.getDefaultFCLocation());
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fileChooser.setFileFilter(new FileFilter() {
					public String getDescription() {
						return "Text Files";
					}
					
					@Override
					public boolean accept(File f) {
						if(f.isDirectory()){
							return true;
						}
						String name = f.getName();
						if(name.contains(".csv") || name.contains(".txt")){
							return true;
						}
						return false;
					}
				});
				
				int result = fileChooser.showSaveDialog(ProfilerView.this);
				if(result == JFileChooser.APPROVE_OPTION){
					boolean succeed = true;
					String error = "";
					File file = fileChooser.getSelectedFile();
					//if the name doesn't have the extension, add it
					if(!file.getName().contains(".csv")){
						String path = file.getPath();
						file = new File(path+".csv");
					}
					//if it exists, confirm the user wants to overwrite before saving
					if (!file.exists() ||
							JOptionPane.YES_OPTION == Util.showConfirmDialog("File exists, overwrite?", "File already exists",
								JOptionPane.YES_NO_OPTION)) {
						try {
							if(expIndFiles){
								int maxIdx = exportProfileMap.size();
								if(!averageChk.isSelected()){
									maxIdx--;
								}
								for(int i=0; i<maxIdx; i++){
									if(exportProfileMap.get(i)){
										String name = file.getPath().substring(0, file.getPath().length()-4);
										File newFile;
										if(i == exportProfileMap.size()-1){
											newFile = new File(name+"-avg.csv");
										}else{
											newFile = new File(name+"-"+i+".csv");
										}
										writeIndividualCSV(newFile, i);
									}
								}
							}else{
								writeCSV(file);
							}
						} catch(Exception ex) {
							succeed = false;
							ex.printStackTrace();
							error = ex.getMessage();
						}
					}
					//if the save worked, notify the user, if it didn't notify the user
					if(succeed){
						Util.showMessageDialog("CSV file exported successfully!", "Save Success", JOptionPane.PLAIN_MESSAGE);
					}else{
						Util.showMessageDialog("Unable to save file: "+error, "Error!", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		
		return item;
	}
	
	private void writeCSV(File outputFile) throws FileNotFoundException{
		
		String xAxis = "km";

		XYDataset dataset = chart.getXYPlot().getDataset(0);
		
		String delim = ",";
		PrintStream ps = new PrintStream(outputFile);
		//Header
		ps.print(xAxis);
		for(int i=0; i<dataset.getSeriesCount(); i++){
			//only export the profile if it is selected for export
			if(exportProfileMap.get(i)){
				ps.print(delim+dataset.getSeriesKey(i));
			}
		}
		ps.println();
		
		//Data
		//cycle through all values of x
		for(int j=0; j<dataset.getItemCount(0); j++){
			//print the x value
			ps.print(dataset.getX(0, j));
			//print the y value of each plot at that x
			for(int i=0; i<dataset.getSeriesCount(); i++){
				//only export the profile if it is selected for export
				if(exportProfileMap.get(i)){
					ps.print(delim + dataset.getYValue(i, j));
				}
			}
			ps.println();
		}
		
		ps.close();
	}
	
	
	private void writeIndividualCSV(File outputFile, int idx) throws FileNotFoundException{
		String xAxis = "km";

		XYDataset dataset = chart.getXYPlot().getDataset(0);
		
		String delim = ",";
		PrintStream ps = new PrintStream(outputFile);
		//Header
		ps.print(xAxis);
		ps.print(delim+dataset.getSeriesKey(idx));
		ps.println();
		//Data
		for(int j=0; j<dataset.getItemCount(0); j++){
			ps.print(dataset.getX(0, j));
			ps.println(delim + dataset.getYValue(idx, j));
		}
		ps.close();
	}
	
	
	/**
	 * @return  Returns the currently selected mapsource
	 */
	private MapSource getMapSource(){
		return source;
	}
	
	/**
	 * @return The number of profile lines to draw
	 */
	private int getLineNumbers(){
		return (int)lineNumBx.getSelectedItem();
	}
	
	/**
	 * @return The length of the profile lines in km based on the user's
	 * selection of either percent or km value.  If the percent radio
	 * button is currently selected, then the percent combobox is used
	 * to calculate the length based off the selected crater's radius.
	 * If the km radio button is selected, then returns the km value the
	 * user entered.
	 * 
	 * Returns 0 if the selected crater is null.
	 */
	private double getProfileLength(){
		
		double length = 0;
		
		if(selectedCrater!=null){
			//if the percent radio button is selected, use the percent
			// to calculate the length of the line in km
			if(percRad.isSelected()){
				double perc = (double)(((int)lengthBx.getSelectedItem())/100.0);
				length = (selectedCrater.getDiameter()*perc)/2000.0;
			}
			//if the km radio button is selected, use the number directly
			else if(kmRad.isSelected()){
				length = Double.parseDouble(lengthTf.getText());
			}
		}
		
		return length;
	}
	
	private double getAngularOffset(){
		double angle=0;
		
		if(offsetTf.getText().length()>0){
			angle = Double.parseDouble(offsetTf.getText());
		}
		else{
			offsetTf.setText("0");
		}
		
		return angle;
	}
	
	
	/**
	 * Disposes of the frame of this ProfilerView
	 */
	public void cleanUp(){
		if(myFrame!=null){
			myFrame.dispose();
		}
	}
    
    /**
     * Creates a frame for this view and sets it visible, relative to the focus panel 
     * that owns it. Also tries to un-minimize it if this frame is minimized.
     */
    public void showInFrame(){
		//create the frame if it hasn't been created
		if(myFrame == null){
			myFrame = new JFrame();
			myFrame.setTitle("Profile Viewer");
			myFrame.setContentPane(this);
			//set the location relative to but centered on the focus panel
			FocusPanel fp = myLView.getFocusPanel();
			Point pt = fp.getLocationOnScreen();
			int xOffset = fp.getWidth()*3/4;
			int yOffset = fp.getHeight()*1/10;
			myFrame.setLocation(pt.x+xOffset, pt.y+yOffset);
			myFrame.pack();
			myFrame.setMinimumSize(new Dimension(frameMinW, frameMinH));
			myFrame.setVisible(true);
		}
		//if it's minimized, set it back to normal
		if(myFrame.getExtendedState() == JFrame.ICONIFIED){
			myFrame.setExtendedState(JFrame.NORMAL);
		}
		//show the frame
		myFrame.setVisible(true);
	}
    
	/** Override the isVisible method to 
	 * return true if the frame is visible
	 **/
	public boolean isVisible(){
		boolean result = false;
		if(myFrame != null){
			result = myFrame.isVisible();
		}
		return result;
	}
	
	private class MapSampler implements Runnable, MapChannelReceiver{
		private MapChannelTiled ch = new MapChannelTiled(this);
		private Rectangle2D.Double bounds;
		private MapSource source;
		private int ppd;
		private ArrayList<MapData> tiles;
		private Samples mySamples;
		
		private MapSampler(Samples samples, MapSource mapSource, int ppd){
			mySamples = samples;
			this.bounds = mySamples.combinedExtent;
			this.ppd = ppd;
			source = mapSource;
			tiles = new ArrayList<MapData>();
		}
		
		@Override
		public void mapChanged(MapData mapData) {
			if(mapData.isFinished()){
				// all fragments for this MapData have arrived or failed
				// so see if the portion of 'bounds' under this request finished
				
				Area finished = mapData.getFinishedArea();
				Rectangle2D.Double tileBounds = getTileBoundsFromFinishedArea(finished, bounds);
				
				if (mapData.getImage() == null || !finished.contains(tileBounds)) {
//					success = false;
					// missing data, failure occurred...
					log.aprintln("Tile did not return succesfully for "+source.getTitle()+".");
					finish();
				} else {
					// include this tile in the running stats
					//tiles.add(mapData);
					tiles.add(mapData);
					if (ch.isFinished()) {
						//close the channel request, add the tiles array to the
						// map that was passed in, and decrease the CountDownLatch
						finish();
					}
				}
			
			}
		}

		private void finish() {
			//close the channel request
			ch.cancel();
			
			mySamples.sampleData(tiles);
			
			double[][][] sampleData = mySamples.getSampleData();
			double[][] distances = mySamples.getDistances();
			
			allObjs.clear();
			pathsToColor.clear();
			for(int i=0; i<sampleData.length; i++){
				double samplesxx[] = new double[sampleData[i].length];
				
				for (int j=0; j<sampleData[i].length; j++) {
					if (sampleData[i][j]==null) continue;
					samplesxx[j]=sampleData[i][j][0];
				}
				ProfileData profile = new ProfileData(i+"", distances[i], samplesxx);
				allObjs.add(profile);
				
				//keep track of the paths to profileData map for color mapping
				profileDataToPaths.put(profile, mySamples.getPathsArray()[i]);
			}

			refreshProfileData(true);
			
		}
		
		@Override
		public void run() {
			try{
				ch.setRequest(Main.PO, bounds, ppd, new Pipeline[]{new Pipeline(source, new Stage[0])});
			}catch (Exception e){
				e.printStackTrace();
				finish();
			}
		}
		
		private Comparator<MapData> byBounds = new Comparator<MapData>() {
			public int compare(MapData o1, MapData o2) {
				//compare x values of the two tiles
				int result = ((Double)o1.getRequest().getExtent().getX()).compareTo((Double)o2.getRequest().getExtent().getX());
				if(result == 0){
					//if they have the same x values, compare y values
					return ((Double)o1.getRequest().getExtent().getY()).compareTo((Double)o2.getRequest().getExtent().getY());
				}else{
					//if they have different x values, return that
					return result;
				}
			}
		};

		
	}

	/**
	 * This code was copied from the old PixelExport, which was believed to 
	 * be copied from the map sampling code.  It seems to check to verify
	 * that the tile returned really does intersect the original area of
	 * interest.  This might fail if the tile does not come back with data
	 * for some reason, and that could be why this code was necessary.
	 * 
	 * @param finished  The finished region of the tile.
	 * @param entireBounds  The original bounds request for all the tiles
	 * @return The bounds of the finished tile that intersect the original 
	 * bounds request.
	 */
	private Rectangle2D.Double getTileBoundsFromFinishedArea(Area finished, Rectangle2D.Double entireBounds){
		Rectangle2D.Double tileBounds = new Rectangle2D.Double();
		tileBounds.setFrame(entireBounds);
		Rectangle2D finishedBounds = finished.getBounds2D();
		if (!finishedBounds.intersects(tileBounds)) {
			double xdelta = finishedBounds.getMinX() - tileBounds.x;
			tileBounds.x += 360 * Math.signum(xdelta);
		}
		Rectangle2D.intersect(tileBounds, finishedBounds, tileBounds);
		
		return tileBounds;
	}
	private JCheckBoxMenuItem createAverageMenuItem(){
		JCheckBoxMenuItem item = new JCheckBoxMenuItem("Display Average Profile");
		
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//if this just became unchecked, remove the pd from the list
				if(!item.isSelected()){
					allObjs.remove(AVG_RECORD_INDEX);
					AVG_RECORD_INDEX = -1;
				}
				refreshProfileData(false);
				
				//refresh the export options pane if it's open
				if(optionsFrame!=null && optionsFrame.isVisible()){
					refreshOptionsFrame();
				}
			}
		});
		
		return item;
	}
	
	/**
	 * Copied and modified from the SpectraMathUtil Class
	 * 
	 * 
	 * Calculates the average of every y value of all the spectra passed in.
	 * 
	 * @param spectra List of spectra objects to be summed
	 * @return A spectraObject with y-values that are the average of each 
	 * individual y value of the spectra passed in.
	 */
	public static ProfileData avgSpectra(ArrayList<ProfileData> data){
		ProfileData firstSpectra = data.get(0);
		int length = firstSpectra.getYValues().length;
		
		String desc = "Avg";
		double[] xVals = new double[length];
		double[] yVals = new double[length];
		
		for(int i=0; i<length; i++){
			double ySum = 0;
			for(ProfileData pd : data){
				ySum = ySum + pd.getYValues()[i];
			}
			yVals[i] = ySum/data.size();
			xVals[i] = firstSpectra.getXValues()[i];
		}
		
		return new ProfileData(desc, xVals, yVals);
	}

}
