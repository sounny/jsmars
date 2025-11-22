package edu.asu.jmars.layer.profile.chart;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.SETTINGS;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.profile.ChartDataConverter;
import edu.asu.jmars.layer.profile.IChartEventHandler;
import edu.asu.jmars.layer.profile.IProfileModel;
import edu.asu.jmars.layer.profile.IProfileModelEventListener;
import edu.asu.jmars.layer.profile.ProfileFactory;
import edu.asu.jmars.layer.profile.ProfileLView;
import edu.asu.jmars.layer.profile.ProfileLView.ProfileLine;
import edu.asu.jmars.layer.profile.ProfileLayer.NumericMapDataSampleWrapper.NumericMapDataSample;
import edu.asu.jmars.layer.profile.chart.table.PlotObject;
import edu.asu.jmars.layer.profile.chart.table.ReadoutTable;
import edu.asu.jmars.layer.profile.chart.table.ReadoutTableModel;
import edu.asu.jmars.layer.profile.config.Config;
import edu.asu.jmars.layer.profile.config.ConfigType;
import edu.asu.jmars.layer.profile.swing.ProfileTabelCellObject;
import edu.asu.jmars.layer.shape2.drawingpalette.UserPrompt;
import edu.asu.jmars.swing.AncestorAdapter;
import edu.asu.jmars.swing.LikeLabelButtonUI;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeChart;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeSnackBar;
import edu.asu.jmars.util.CSVFilter;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.CustomBalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.java.balloontip.utils.TimingUtils;


public class ProfileChartView extends JPanel implements IProfileModelEventListener, TableModelListener {
	private static final String UNDEFINED_MAP_SOURCE = "undefined";
	private static int DATASET_INDEX_START = 0;
	private JFreeChart chart;
	private ChartPanel chartPanel;
	private JMenuItem saveAsTextMenuItem;
	private JFileChooser fileChooser;
	private JMenuItem moreProperties;
	private JComboBox ppdComboBox;
	private DefaultComboBoxModel ppdComboBoxModel;
	private ItemListener ppdactionListener;
	private JTable readoutTable;
	private JPanel cards;
	private JPanel cardCharts, cardEmpty;
	private JLabel legendtext = new JLabel();
	private JLabel legendline = new JLabel();
	private MoreChartProperties morepropspanel = null;
	private JFrame morepropsframe = null;
	private ReadoutTableModel roTableModel;
	private Pipeline[] chartData = null;
	private ProfileLView profileLView;
	private ProfileChartView profileChartView;
	private ChartDataConverter dataconversion;
	private Map<Integer,Shape> plotSeries = new HashMap<>(); // multiple profilelines and their IDs
	private List<Double> profileLineLengthKm = new ArrayList<>();
	private final ProfileFactory controller;
	private Range effRange;
	private static final int MAX_ZOOM_POWER = 14; // Maximum power of 2 that the zoom can take.
	private final static int NO_ZOOM = -1;
	private static DebugLog log = DebugLog.instance();
	private boolean isChartIntialized = false;	
	private boolean isChartPanelDisplayed = false;	
	private boolean isRangeAxisPaintChanged = false;
	private List<NumericMapDataSample> numericDataForPlots = new ArrayList<>();		
	private Map<Integer, Set<Integer>> datasetIDs = new HashMap<>(); //// maps unique numeric sample ID to series id
	private Map<String, XYItemRenderer> sourceRenderer = new HashMap<>();  //stores plot renderer per numeric source
	private Map<Integer, String> seriesID_maptitle = new HashMap<>(); //maps unique chart seriesID to numeric source title
	private Map<Integer, String> seriesID_unit = new HashMap<>(); //maps unique chart seriesID to numeric source title
	Map<Integer, Shape> request = new HashMap<>(); //map with only visible profiles to request data for
	private CustomBalloonTip myBalloonTip;
	private Color imgBlack = Color.BLACK;
	private Icon close = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CLEAR.withDisplayColor(imgBlack)));
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
    private static Icon settingsicon = new ImageIcon(ImageFactory.createImage(SETTINGS.withDisplayColor(imgColor)));
	static int datasetIndex = DATASET_INDEX_START;
	private Config chartConfig = null;
	private static final String CHARTCMD = "Charts";
	private static final String EMPTYCMD = "EMPTY";
	private static Map<Integer, Integer> inner_counter = new HashMap<Integer, Integer>();
	private static  Map<String, Supplier<IChartEventHandler>> EVENT_HANDLER;

	
	public ProfileChartView(ProfileLView profileview, ProfileFactory profileFactory) {
		super();
		profileLView = profileview;
		controller = profileFactory;		
		dataconversion = new ChartDataConverter(profileLView);
		roTableModel = new ReadoutTableModel();
		readoutTable = new ReadoutTable(roTableModel);
		this.effRange = new Range(0.0, 1.0);
		createCalloutUI();
		initChartEventHandlers();
		inner_counter.put(1, 0);
		if (!isChartIntialized) {
			initChartUI();
		}	
		profileChartView = this;
	}

	private void initChartEventHandlers() {
		final Map<String, Supplier<IChartEventHandler>> handlers = new HashMap<>();
		handlers.put(IProfileModel.CREATE_CHART, CreateChartEventHandler::new);
		handlers.put(IProfileModel.PLOT_DATA_EVENT, PlotDataEventHandler::new);
		handlers.put(IProfileModel.UPDATE_CROSSHAIR_FOR_PROFILE, UpdateCrosshairEventHandler::new);
		handlers.put(IProfileModel.REQUEST_CHART_CONFIG_CHANGED, ResetNewConfig::new);
		EVENT_HANDLER = Collections.unmodifiableMap(handlers);		
	}

	private void initChartData(Pipeline[] data) {		
		if (data != null) {
			chartData = Arrays.stream(data).toArray(Pipeline[]::new);		
			this.effRange = new Range(0.0, 1.0);
			chart.setTitle(buildChartTitle(chartData));
			configureChartAxes();
		}
	 }

	private String buildChartTitle(Pipeline[] chartdata) {
		StringBuffer chartTitle = new StringBuffer("");
		String token = "\n";
		for (int ind = 0; ind < chartdata.length; ind++) {
		    chartTitle.append(chartData[ind].getSource().getTitle());
		    if (!(chartTitle.toString()).contains(UNDEFINED_MAP_SOURCE)) {
			    chartTitle.append(" (units: " + chartData[ind].getSource().getUnits() + " )");
		    }
		    chartTitle.append(token);
		}
		return chartTitle.toString();
	}

	private void initChartUI() {

		setLayout(new BorderLayout());
		
		cards = new JPanel(new CardLayout());
		cardCharts = new JPanel(new BorderLayout());
		cardEmpty = new JPanel(new BorderLayout());
		
		JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		sp.setDividerLocation(0.8);
		sp.setResizeWeight(0.8);
		
		JPanel chartpanel = createChartPanel();		
		configureChartPanelEvents();
		configureChartEvents();
		createChartPopupMenu();
		sp.setLeftComponent(chartpanel);
		sp.setRightComponent(createReadoutPanel());
		
		cardCharts.add(sp, BorderLayout.CENTER);				
		
		JPanel emptyChartPanel = createEmptyChartPanel();	
		cardEmpty.add(emptyChartPanel, BorderLayout.CENTER);
		
		cards.add(cardCharts, CHARTCMD);
		cards.add(cardEmpty, EMPTYCMD);
		
		add(cards, BorderLayout.CENTER);
		
		isChartIntialized = true;
	}

	private JPanel createEmptyChartPanel() {
		JFreeChart chartEmpty = ChartFactory.createXYLineChart("No Data", "Distance (Km)", "Value", null,
				PlotOrientation.VERTICAL, false, false, false);

		ThemeChart.configureUI(chartEmpty);
		
		chartEmpty.getXYPlot().setDomainCrosshairVisible(true);
		chartEmpty.getXYPlot().setDomainCrosshairPaint(ThemeChart.getProfileCrosshairColor());
		chartEmpty.getXYPlot().setDomainCrosshairStroke(new BasicStroke(1.5f));						  

		ChartPanel chartPanelEmpty = new ChartPanel(chartEmpty, true);
		chartPanelEmpty.setMaximumDrawHeight(10000);
		chartPanelEmpty.setMinimumDrawHeight(0);
		chartPanelEmpty.setMaximumDrawWidth(10000);
		chartPanelEmpty.setMinimumDrawWidth(0);

		JPanel p = new JPanel(new BorderLayout());	
		p.add(chartPanelEmpty, BorderLayout.CENTER);
		
		return p;
	}

	private JPanel createChartPanel() {
		chart = ChartFactory.createXYLineChart("", "Distance (Km)", "Value", null,
				PlotOrientation.VERTICAL, false, false, false);

		ThemeChart.configureUI(chart);
		
		chart.getXYPlot().setDomainCrosshairVisible(true);
		chart.getXYPlot().setDomainCrosshairPaint(ThemeChart.getProfileCrosshairColor());
		chart.getXYPlot().setDomainCrosshairStroke(new BasicStroke(1.5f));						  

		chartPanel = new ChartPanel(chart, true);
		//set these values to prevent font stretching when resizing the chart
		chartPanel.setMaximumDrawHeight(10000);
		chartPanel.setMinimumDrawHeight(0);
		chartPanel.setMaximumDrawWidth(10000);
		chartPanel.setMinimumDrawWidth(0);

		JPanel ppdPanel = createPPDPanel();

		JPanel p = new JPanel(new BorderLayout());
		p.add(ppdPanel, BorderLayout.NORTH);
		p.add(chartPanel, BorderLayout.CENTER);
		
		return p;
	}

	private JPanel createReadoutPanel() {
		int readoutTblWidth = 500;
		JPanel roPanel = new JPanel(new BorderLayout());
		
		JPanel plotLegend = new JPanel(new BorderLayout());
		JPanel p1 = new JPanel(new FlowLayout(0, 10, 0));
		p1.setBorder(BorderFactory.createEmptyBorder(15, 1, 10, 5));
		JLabel lbl = new JLabel("Currently Plotting");
		JLabel settings = new JLabel(settingsicon); 
		settings.setToolTipText("Click here to view current configuration");
		settings.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
               profileLView.viewConfig();
            }

        });
		p1.add(lbl);
		p1.add(settings);
		
		plotLegend.add(p1, BorderLayout.NORTH);
		
		JPanel legendpanel = createLegendUI();
		legendpanel.setBorder(BorderFactory.createEmptyBorder(1, 5, 15, 5));
		
		plotLegend.add(legendpanel, BorderLayout.LINE_START);
		
		roPanel.add(plotLegend, BorderLayout.NORTH);
		
		readoutTable.getModel().addTableModelListener(this);
		
		JScrollPane sp = new JScrollPane(readoutTable);
		sp.setPreferredSize(new Dimension(readoutTblWidth, 200));
		roPanel.add(sp, BorderLayout.CENTER);
		return roPanel;
	}
	
	private JPanel createLegendUI() {
		legendtext = new JLabel();
		legendtext.setFont(ThemeFont.getMedium());
		legendtext.setHorizontalAlignment(SwingConstants.LEFT);
		legendline = new JLabel();
		JPanel panel = new JPanel(new BorderLayout());
		JPanel txtPanel = new JPanel(new BorderLayout());
		txtPanel.add(legendtext, BorderLayout.WEST);
		JPanel btnPanel = new JPanel(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 5));
		btnPanel.add(legendline, BorderLayout.WEST);
		panel.add(btnPanel, BorderLayout.WEST);
		panel.add(txtPanel, BorderLayout.CENTER);
		return panel;
	}

	private void configureChartAxes() {
		XYPlot plot = chart.getXYPlot();
		NumberAxis numberaxis = new NumberAxis("Value"); // Y axis
		ThemeChart.applyDefaultThemeToAxisLabels(numberaxis);
		if (!this.isRangeAxisPaintChanged) {
			ThemeChart.applyDefaultThemeToAxisTickLabels(numberaxis);
		} else {
			ThemeChart.applyUserThemeToAxisTickLabels(numberaxis, chart.getXYPlot().getRangeAxis());
		}
		plot.setRangeAxis(0, numberaxis);
		int chartdatasourcecount;
		String units;

		removeExtraYAxisIfAny(plot);

		for (chartdatasourcecount = 0; chartData != null
				&& chartdatasourcecount < chartData.length; chartdatasourcecount++) {

			final String label;
			String chartTitle = chartData[chartdatasourcecount].getSource().getTitle();
			units = (chartTitle.contains(UNDEFINED_MAP_SOURCE)) ? null
					: chartData[chartdatasourcecount].getSource().getUnits();
			label = (units != null) ? units
					: "Units not available for " + chartData[chartdatasourcecount].getSource().getName();
			NumberAxis newAxis = new NonStickyZeroNumberAxis(label);
			ThemeChart.applyDefaultThemeToAxisLabels(newAxis);
			if (!this.isRangeAxisPaintChanged) {
				ThemeChart.applyDefaultThemeToAxisTickLabels(newAxis);
			} else {
				ThemeChart.applyUserThemeToAxisTickLabels(newAxis, chart.getXYPlot().getRangeAxis());
			}
			plot.setRangeAxis(chartdatasourcecount, newAxis);
		}
		this.numericDataForPlots.clear();
		this.removeAllSeriesByDataset();
	}

	private void removeExtraYAxisIfAny(XYPlot plot) {
		int YAxisCount = plot.getRangeAxisCount();
		if (YAxisCount > chartData.length) {
			for (int y = YAxisCount - 1; y >= chartData.length; y--) {
				plot.setRangeAxis(y, null);
			}
		}
	}

	private void configureChartPanelEvents() {
		chartPanel.addChartMouseListener(new ChartMouseListener() {
			public void chartMouseClicked(ChartMouseEvent arg0) {
			}

			public void chartMouseMoved(ChartMouseEvent e) {
				chartMouseMovedEventOccurred(e);
			}
		});

		chartPanel.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				updateMaxAndSelectedPpd();
			}
		});

		chartPanel.addAncestorListener(new AncestorAdapter() {
			@Override
			public void ancestorAdded(AncestorEvent e) {
				updateMaxAndSelectedPpd();
			}
		});
	}

	private void configureChartEvents() {
		chart.getXYPlot().getDomainAxis().addChangeListener(new AxisChangeListener() {
			public void axisChanged(AxisChangeEvent event) {
				domainAxisChangeEvent(event);
			}
		});
	}

	private void createChartPopupMenu() {
		fileChooser = createSaveAsTextFileChooser();
		saveAsTextMenuItem = createSaveAsTextMenuItem();
		saveAsTextMenuItem.setEnabled(false);
		moreProperties = createMorePropertiesMenuItem();
		JPopupMenu popupMenu = chartPanel.getPopupMenu();
		popupMenu.add(moreProperties, 1);
		JMenu saveasmenu = (JMenu) popupMenu.getComponent(4); //Save As is 3rd menu item in chart's popup menu
		saveasmenu.add(saveAsTextMenuItem, 1); //first item in PNG, second is our CSV				
		chartPanel.setPopupMenu(popupMenu);
	}
	
	private void updateMaxAndSelectedPpd() {
		Map<Integer, Shape> varplots = new HashMap<>();
		varplots.putAll(this.plotSeries);
		if (!varplots.isEmpty()) {
			ppdComboBox.removeItemListener(ppdactionListener);
			calculateMinOfAllMaxPPDsAndUpdateCombobox(varplots);
			ppdComboBox.addItemListener(ppdactionListener);
		}
	}

	private void updateMaxPpd(Map<Integer, Shape> inputprofiles, int minPpdIndex) {
		// Get new angular span in degrees, determine max-ppd and update ppd combo-box.
		Map<Integer,Shape> effLine = recalculateEachPlotBasedOnRange(inputprofiles, this.effRange);   
		List<Double> aSpan = new ArrayList<>();
		effLine.forEach((id,effline) -> {
		         aSpan.add(dataconversion.perimeterLength(effline)[0]);
		});
		double maxaSpan = (aSpan.stream().count() == 0 ? 0.0 : 
				          aSpan.
						  stream().
				          max(Comparator.comparing(Double::valueOf)).
				          get());				
		double pixSpan = chartPanel.getScreenDataArea().getWidth();
		if (pixSpan == 0.0)
			return; // Do not set the interface to 1 ppd if the components haven't been sized yet
		int maxPpdIndex = maxaSpan > 0.0 && pixSpan > 0 ? getPPDNextLargestPowerOf2(pixSpan / maxaSpan) : minPpdIndex;
		maxPpdIndex = Math.max(0, Math.min(maxPpdIndex, minPpdIndex));
		log.println("aSpan:" + maxaSpan + "  pixSpan:" + pixSpan + "  maxPpdIndex:" + maxPpdIndex);
		updatePpdComboBox(inputprofiles, maxPpdIndex);
	}

	private void updatePlotSeries(Map<Integer, Shape> newViewExtents, Range span) {
		// Set profile lines as this plot's series		
 		Map<Integer,Shape> clonePlotSeries = new HashMap<>(newViewExtents); 
		
		if (! this.plotSeries.isEmpty()) {
			this.plotSeries.clear();	
		}
		this.plotSeries.putAll(clonePlotSeries);
       				
		request.clear();
		request.putAll(this.plotSeries);
		int requestedPPD = -1;
		if (ppdComboBox.getSelectedItem() != null) {
			requestedPPD  = ((Integer) ppdComboBox.getSelectedItem()).intValue();
		} else {
			if (!request.isEmpty()) {
			    requestedPPD = calculateMinOfAllMaxPPDs(request);
			}
		}
		
		if (!request.isEmpty() && requestedPPD != -1) {
			configureChartAxes();
			this.updateChartViewForEachPlot(request, this.effRange, requestedPPD);
		}
	}
	
	private boolean isProfileNotVisibleInChart(Integer ID) {
		return roTableModel.isNotVisible(ID);
	}

	private void calculateMinOfAllMaxPPDsAndUpdateCombobox(Map<Integer, Shape> inputprofiles) {
		List<Integer> maxppds = new ArrayList<>();
		Map<Integer, Shape> varinput = new HashMap<>();
		varinput.putAll(inputprofiles);
		// update max allowable ppd per profile
		maxppds.add(getMinOfMaxDataSourcesPPD());
		varinput.forEach((id, shape) -> {
			if (isProfileNotVisibleInChart(id) == false) { //means visible
			    int maxppdperprofile = calculateMaxPPDPerProfileLength(shape); 						
                if (maxppdperprofile != NO_ZOOM) {
					maxppds.add(maxppdperprofile);
				} 
			}
		});
		int minOfAllMaxAllowedPPD = (maxppds.stream().count() == 0 ? getMinOfMaxDataSourcesPPD()
				: maxppds.stream().min(Comparator.comparing(Integer::valueOf)).get());
		int idx = getPPDNextLargestPowerOf2(minOfAllMaxAllowedPPD);
		updateMaxPpd(inputprofiles, idx);
	}

	
	private int calculateMinOfAllMaxPPDs(Map<Integer, Shape> inputprofiles) {
		List<Integer> maxppds = new ArrayList<>();
		Map<Integer, Shape> varinput = new HashMap<>();
		varinput.putAll(inputprofiles);
		// update max allowable ppd per profile
		maxppds.add(getMinOfMaxDataSourcesPPD());
		varinput.forEach((id, shape) -> {
			if (isProfileNotVisibleInChart(id) == false) { //means visible
			    int maxppdperprofile = calculateMaxPPDPerProfileLength(shape); 						
                if (maxppdperprofile != NO_ZOOM) {
					maxppds.add(maxppdperprofile);
				} 
			}
		});
		int minOfAllMaxAllowedPPD = (maxppds.stream().count() == 0 ? getMinOfMaxDataSourcesPPD()
				: maxppds.stream().min(Comparator.comparing(Integer::valueOf)).get());
		int idx = getPPDNextLargestPowerOf2(minOfAllMaxAllowedPPD);
		int ppd;
		if (idx < MAX_ZOOM_POWER) {
		    ppd =  (int) Math.pow(2, idx);
		}  else {  //instead of highest possible index, get ppd from LView
			int viewppd = this.profileLView.getProj().getPPD();
			int maxchartppd = (int) Math.pow(2, MAX_ZOOM_POWER);
			if (viewppd < maxchartppd) {
				ppd = viewppd;
			} else {
				ppd = maxchartppd;
			}
		}
    	return ppd;
	}
	
	private Integer getMinOfMaxDataSourcesPPD() {
		int maxOfAllSourcesPPD = 1;  //check for visibility of data source if config is MULTISOURCES
		int minOfAllMaxPPDs = 1;
		int constant = 10;
		List<PlotObject> notvisible = new ArrayList<>();		
		Map<String, Integer> notvisible_map = new HashMap<>();
		
		if (this.chartConfig != null) {
			if (this.chartConfig.getConfigType() == ConfigType.MANYNUMSOURCES) {
				notvisible = roTableModel.getNotVisiblePlots();
				for (PlotObject po : notvisible) {
					if (po.isNumericSource() && !po.isVisible()) {
					    notvisible_map.put(po.getPlotname(), constant); // we need "key";
					 }													// "value" doesn't matter, so just put any number
				} 													  
			}
		}
		
		if (chartData != null) {
			for (int i = 0; i < chartData.length; i++) {
				if (!notvisible_map.isEmpty()) {
					String sourcetitle = chartData[i].getSource().getTitle();
					if (notvisible_map.get(sourcetitle) != null) continue;  //means not visible, so don't count it
				}
			    int sourcePPD = (int) chartData[i].getSource().getMaxPPD();
			    if (sourcePPD > maxOfAllSourcesPPD) {
			    	maxOfAllSourcesPPD = sourcePPD;
			    }
			}
		}

		minOfAllMaxPPDs = maxOfAllSourcesPPD;
		if (chartData != null) {
			for (int i = 0; i < chartData.length; i++) {
				if (!notvisible_map.isEmpty()) {
					String sourcetitle = chartData[i].getSource().getTitle();
					if (notvisible_map.get(sourcetitle) != null) continue;  //means not visible, so don't count it
				}				
			    int sourcePPD = (int) chartData[i].getSource().getMaxPPD();
			    if (sourcePPD < minOfAllMaxPPDs) {
			    	minOfAllMaxPPDs = sourcePPD;
			    }
			}
		}		
		return minOfAllMaxPPDs;
	}
	
	private Integer getPPDNextLargestPowerOf2(double ppd) {
		int nextPower2 = (int) Math.ceil(Math.log(ppd) / Math.log(2.0));
		return nextPower2;
	}	

	private int calculateMaxPPDPerProfileLength(Shape plotseriesshape) {
		int maxPPDPerProfile = NO_ZOOM;
		Shape spanselectshape = dataconversion.spanSelect(plotseriesshape, this.effRange.getLowerBound(),
				this.effRange.getUpperBound());
		Double aSpan = dataconversion.perimeterLength(spanselectshape)[0];
		Double maxaSpan = aSpan;
		double pixSpan = chartPanel.getScreenDataArea().getWidth();
		if (pixSpan != 0.0) { //do not calculate max ppd if ui was not initialized yet
			int maxPpdIndex = maxaSpan > 0.0 && pixSpan > 0
					? getPPDNextLargestPowerOf2(pixSpan / maxaSpan)
					: 0;		
			maxPpdIndex = Math.max(0, maxPpdIndex);
			Integer[] allZooms = getZoomValues(new int[] { 0, MAX_ZOOM_POWER });
				if (allZooms.length > maxPpdIndex) {
					maxPPDPerProfile = allZooms[maxPpdIndex];
				} else {
					maxPPDPerProfile = allZooms[allZooms.length - 1];
				}
		}
		return maxPPDPerProfile;
	}

	/**
	 * Sets the view extent to the specified extent. The specified extent is
	 * converted into effective extent by narrowing it to the profile line's extent.
	 * Appropriate diagonal from this extent is the output of the profile line.
	 * @param requestedPPD 
	 * @param notVisibleProfiles 
	 */
	private void updateChartViewForEachPlot(Map<Integer, Shape> plotSeries2, Range span, int ppd) {
		disableChartSave();			
		controller.requestDataUpdate(plotSeries2, span, ppd);   		
	}

	/**
	 * Outline: This method is called when user selects Zoom In/Zoom Out from chart right-click popup menu
	 * 1. Determine the Km range displayed by the chart. 2. Determine the
	 * span of profile-line in terms of the linear parameter t. 3. Calculate the
	 * angular distance covered by the span. 4. Determine the pixel width of the
	 * plot area. 5. Compute the maximum PPD value based on 3 & 4. 6. Update the PPD
	 * combo-box based on maximum PPD computed in 5. 6.1. If PPD is smaller than max
	 * combo-box PPD, reset view to smaller PPD. 6.2. Add/remove combo-box as
	 * required. 7. If zooming out, check to see if the newly exposed area will be
	 * outside currently cached range. If so, issue a fetch request.
	 */
	private void domainAxisChangeEvent(AxisChangeEvent event) {	
		if (plotSeries.isEmpty())
			return;
		
		profileLineLengthKm.clear();			
		
		this.plotSeries.forEach((id, shape) -> {
			if (isProfileNotVisibleInChart(id) == false) {			
         		double profilelength = dataconversion.perimeterLength(shape)[1];
				profileLineLengthKm.add(profilelength);
			}
		});								
				
		// Get the exposed Km range on the x-axis.
		Range kmRange = chart.getXYPlot().getDomainAxis().getRange();
		
		double lowBound = 0.0; 
		List<Double> lowbound = new ArrayList<>();
		lowbound.add(lowBound);
		for (Double profilelinelengthkm : profileLineLengthKm) {
			double km =  kmRange.getLowerBound()  / profilelinelengthkm;
			lowbound.add(km);
		}
		double low = (lowbound.stream().count() == 0 ? 0.0
				: lowbound.stream().max(Comparator.comparing(Double::valueOf)).get());
		
		double upBound = 1.0;
		List<Double> upbound = new ArrayList<>();
		upbound.add(upBound);		
		for (Double profilelinelengthkm : profileLineLengthKm) {
			double km =   kmRange.getUpperBound() / profilelinelengthkm;
			upbound.add(km);
		}
		double upper = (upbound.stream().count() == 0 ? 1.0
				: upbound.stream().min(Comparator.comparing(Double::valueOf)).get());
			
		if (low > upper) return;
		
		this.effRange = new Range(low, upper);
				
		log.println("axisChanged: " + kmRange + "  t:" + this.effRange);		
		
		// Get new angular span in degrees, determine max-ppd and update ppd combo-box.		
		boolean fetch = false; 		
		for (NumericMapDataSample numericsample : numericDataForPlots) {		
			if (numericsample == null || (numericsample != null && (this.effRange.getLowerBound() < numericsample.t0)
					|| this.effRange.getUpperBound() > numericsample.t1)) {
				fetch = true;
				break;
			}
		}		
		if (fetch) {
			Map<Integer, Shape> varplots = new HashMap<>();
			varplots.putAll(this.plotSeries);
			updatePlotSeries(varplots, this.effRange);
		}
	}

	
	private Map<Integer, Shape> recalculateEachPlotBasedOnRange(Map<Integer, Shape> inputprofiles, Range span) {
		Map<Integer, Shape> varinput = new HashMap<>();
		varinput.putAll(inputprofiles);
		Map<Integer, Shape> recalculatedProfileLine = new HashMap<>();
		varinput.forEach((id, plotseries) -> {
			if (isProfileNotVisibleInChart(id) == false) {			
				Shape reqLine = dataconversion.spanSelect(plotseries, span.getLowerBound(), span.getUpperBound());
				recalculatedProfileLine.put(id, reqLine);
			}
		});
		return recalculatedProfileLine;
	}
	

	private void updatePpdComboBox(Map<Integer, Shape> inputprofiles, int maxPpdIndex) {
		Map<Integer, Shape> varinput = new HashMap<>();
		varinput.putAll(inputprofiles);
		int currMaxPpdIndex = ppdComboBoxModel.getSize() - 1;
		int currPPD = NO_ZOOM;
		int currPpdIndex = NO_ZOOM;

		if (ppdComboBox.getSelectedItem() != null) {
			currPPD = ((Integer) ppdComboBox.getSelectedItem()).intValue();
			currPpdIndex = this.getPPDNextLargestPowerOf2(currPPD);
		}
		if (maxPpdIndex > currMaxPpdIndex) {
			// Add new zoom levels to the ppd combo box
			log.println("Adding zoom indices [" + currMaxPpdIndex + ".." + maxPpdIndex + "]");
			Integer[] newZooms = getZoomValues(new int[] { currMaxPpdIndex + 1, maxPpdIndex });
			ppdComboBox.removeItemListener(ppdactionListener);
			for (int i = 0; i < newZooms.length; i++) {
				ppdComboBoxModel.addElement(newZooms[i]);
			}
			ppdComboBox.addItemListener(ppdactionListener);
		} else if (maxPpdIndex < currMaxPpdIndex) {			
			// Remove zoom levels from the ppd combo box
			log.println("Removing ppd indices [" + (maxPpdIndex + 1) + ".." + currMaxPpdIndex + "]");
			ppdComboBox.removeItemListener(ppdactionListener);
			while ((ppdComboBoxModel.getSize() - 1) > maxPpdIndex) {
				ppdComboBoxModel.removeElementAt(ppdComboBoxModel.getSize() - 1);
			}
			ppdComboBox.addItemListener(ppdactionListener);
		}	
		
		int ppd = this.profileLView.getProj().getPPD();
		int mainviewppdpower2 = this.getPPDNextLargestPowerOf2(ppd);
		int ppdcombosize = ppdComboBoxModel.getSize() -1;
		if (currPpdIndex > ppdcombosize) {
			currPpdIndex = ppdcombosize;
		}
		if (mainviewppdpower2 > ppdcombosize) {
			mainviewppdpower2 = ppdcombosize;
		}
		int selectedppd = (currPpdIndex != NO_ZOOM ? currPpdIndex : mainviewppdpower2);
		
		ppdComboBox.removeItemListener(ppdactionListener);
		setSelectedItemInPPDCombobox(selectedppd);	
		ppdComboBox.addItemListener(ppdactionListener);
	}

	private void setSelectedItemInPPDCombobox(int selected) {
		if ((ppdComboBoxModel.getSize() - 1) > selected) {
			ppdComboBoxModel.setSelectedItem(ppdComboBoxModel.getElementAt(selected));
		} else {
			ppdComboBoxModel.setSelectedItem(ppdComboBoxModel.getElementAt(ppdComboBoxModel.getSize() - 1));
		}
	}

	private Integer[] getZoomValues(int[] zoomPwrRange) {
		List<Integer> al = new ArrayList<>();
		int maximumSourcePPD = 1;
		int maximumSourcePPDNextPowerOf2 = 0;
		if (chartData != null) {
			maximumSourcePPD = getMinOfMaxDataSourcesPPD();
			maximumSourcePPDNextPowerOf2 = (int) Math.ceil(Math.log(maximumSourcePPD) / Math.log(2));

			if (zoomPwrRange[0] < 0 || zoomPwrRange[1] < 0 || zoomPwrRange[1] < zoomPwrRange[0])
				throw new IllegalArgumentException(
						"Illegal zoom power range [" + zoomPwrRange[0] + ".." + zoomPwrRange[1] + "]");
			for (int i = 0; i < MAX_ZOOM_POWER - 1; i++) {
				int zoomValue = new Integer(1 << (zoomPwrRange[0] + i));
				if (zoomValue <= Math.pow(2, maximumSourcePPDNextPowerOf2)) {
					al.add(zoomValue);
				} else {
					break;
				}
			}
		}
		Integer[] zooms = new Integer[al.size()];
		zooms = al.toArray(zooms);
		return zooms;
	}

	private JPanel createPPDPanel() {		
		ppdComboBoxModel = new DefaultComboBoxModel(getZoomValues(new int[] { 0, MAX_ZOOM_POWER }));
		ppdComboBox = new JComboBox(ppdComboBoxModel);
	    ppdComboBox.setSelectedItem(null);	
	    ppdactionListener = (new ItemListener() {
	    	@Override
			public void itemStateChanged(ItemEvent ie) {
				ppdComboBoxActionPerformed(ie);
			}
		});
		ppdComboBox.addItemListener(ppdactionListener);

		JPanel ppdPanel = new JPanel(new BorderLayout());	
		ppdPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 15, 15));  
        JPanel ppdLabelPanel = new JPanel(new BorderLayout(5,0));     
        JLabel chartppdLabel = new JLabel("CHART PPD:");
        Dimension gap = new Dimension(5,0);
        ppdLabelPanel.add(chartppdLabel, BorderLayout.CENTER);     
        ppdLabelPanel.add(ppdComboBox, BorderLayout.EAST);      
        ppdPanel.add(Box.createRigidArea(gap));
        ppdPanel.add(ppdLabelPanel, BorderLayout.EAST);				
		return ppdPanel;
	}
	
	private void ppdComboBoxActionPerformed(ItemEvent itemevent) {
		if (itemevent.getStateChange() == ItemEvent.SELECTED) { // swing always calls combobx ActionPerformed or
																// ItemEvent twice; so on second(same) call, we don't need to repeat 
																//building chart
			try {
				int count = inner_counter.get(1);
				count += 1;
				inner_counter.put(1, count);
				if (!this.plotSeries.isEmpty()) {
					int count2 = inner_counter.get(1);
					if (count2 % 2 != 0) {
						Map<Integer, Shape> varplots = new HashMap<>();
						varplots.putAll(this.plotSeries);
						// if this is a "repeat" call, swing calls ActionPerformed/ItemSelected twice,
						// so do nothing
						updatePlotSeries(varplots, this.effRange);
					}
				}

			} catch (NumberFormatException ex) {
				log.aprintln("Exception while getting new zoom level: " + ex);
			}
		}
	}

	private JFileChooser createSaveAsTextFileChooser() {
		JFileChooser fileChooser = new JFileChooser(Util.getDefaultFCLocation());
		CSVFilter filter = new CSVFilter();
		fileChooser.setFileFilter(filter);
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);		
		return fileChooser;
	}

	private JMenuItem createSaveAsTextMenuItem() {
		JMenuItem saveAsTextMenuItem = new JMenuItem("CSV...");
		saveAsTextMenuItem.setEnabled(false);
		saveAsTextMenuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveAsTextActionPerformed(e);
			}
		});
		return saveAsTextMenuItem;
	}		

	private void saveAsTextActionPerformed(ActionEvent e) {
		while (true) {
			int rc = fileChooser.showSaveDialog(this);
			if (rc != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null) {
				break;
			}
			File selected = fileChooser.getSelectedFile();
			if (!selected.exists() || JOptionPane.YES_OPTION == Util.showConfirmDialog("File exists, overwrite?",
					"File already exists", JOptionPane.YES_NO_OPTION)) {
				try {					
					File csvoutputfile = fileChooser.getSelectedFile();
					if (!csvoutputfile.getName().contains(".csv")) {
						String path = csvoutputfile.getPath();
						csvoutputfile = new File(path+".csv");
					}
					saveAsText(csvoutputfile);
				} catch (Exception ex) {
					Util.showMessageDialog("Unable to save file: " + ex.getMessage(), "Error!",
							JOptionPane.ERROR_MESSAGE);
				}
				break;
			}
		}
	}

	private JMenuItem createMorePropertiesMenuItem() {
		 this.moreProperties = new JMenuItem("More properties...");
		 this.moreProperties.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					morePropertiesActionPerformed(e);
				}
			});
		return moreProperties;
	}	
	
	private void morePropertiesActionPerformed(ActionEvent e) {
		Axis domain = chart.getXYPlot().getDomainAxis();
		Axis range = chart.getXYPlot().getRangeAxis();
		this.isRangeAxisPaintChanged = true;
		morepropspanel = new MoreChartProperties(domain, range);
		morepropsframe = new JFrame("More chart axis properties");
		morepropsframe.add(morepropspanel);
		morepropsframe.setSize(400,100);
		morepropsframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		morepropsframe.setLocationRelativeTo(this.chartPanel);
		morepropsframe.setVisible(true);		
	}
	
	private void chartMouseMovedEventOccurred(ChartMouseEvent e) {
		Point2D pt = chartPanel.translateScreenToJava2D(e.getTrigger().getPoint());
		double km = chart.getXYPlot().getDomainAxis().java2DToValue(pt.getX(),
				chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea(), chart.getXYPlot().getDomainAxisEdge());
		
		if (chart.getXYPlot().getDomainAxis().getRange().contains(km)) {
			setCrosshair(km);			
			buildDataForChartReadoutTable(km);	
			for (NumericMapDataSample numericsample : numericDataForPlots) {
			    if (numericsample != null) {
				    int ID = numericsample.getUniqueID();
				    //check if corresponding plot object is visible. if not, don't move its cue
					if (isProfileNotVisibleInChart(ID) == false) {				    
						Point2D cuePoint = numericsample.getPointAtDist(km);
						controller.crosshairChanged(new ImmutablePair<Integer, Point2D>(ID, cuePoint), profileLView);
					}
				}
			}
		}		
	}
	

	private void buildDataForChartReadoutTable(double XCoord) {	
		List<NumericMapDataSample> numsamples = getNumericDataForPlots();
		roTableModel.updateSampleData(numsamples, XCoord);			
	}
	
	public void setCrosshair(double km) {
		chart.getXYPlot().setDomainCrosshairValue(km);
	}
	
	private void updateCrosshairWithNewPosition(int ID, Point2D worldCuePoint) {
		if (worldCuePoint == null) return;
		NumericMapDataSample sample = this.getNumericSampleByID(ID);
		if (sample == null) return;		
		double km = Double.NaN;	
		km = sample.getDistance(worldCuePoint);				
		if (chart.getXYPlot().getDomainAxis().getRange().contains(km)) {			
			setCrosshair(km);
			this.buildDataForChartReadoutTable(km);			
		}		
	}

	private void resetChartAxesAndRange() {
		chart.getXYPlot().getDomainAxis().setAutoRange(true);
		chart.getXYPlot().configureDomainAxes();
		chart.getXYPlot().configureRangeAxes();
		// The effRange has to be set here, since configureDomainAxes() generates an
		// AxisChangedEvent,which modifies the effRange. Doing this outside, i.e. in setProfileLine does not work
		this.effRange = new Range(0.0, 1.0);
	}

	class NonStickyZeroNumberAxis extends NumberAxis {
		public NonStickyZeroNumberAxis() {
			super();
			setAutoRangeIncludesZero(false);
			setAutoRangeStickyZero(false);
		}

		public NonStickyZeroNumberAxis(String label) {
			super(label);
			setAutoRangeIncludesZero(false);
			setAutoRangeStickyZero(false);
		}

		public void setAutoRangeIncludesZero(boolean flag) {
			super.setAutoRangeIncludesZero(flag);
		}
	}	

	private void populatePlot(Pipeline[] pipeline, NumericMapDataSample samples) {
		// Build XYSeries for data from each data source
		double[][] data = samples.getSampleData();
		double[] distances = samples.getDistances();
		int ID = samples.getUniqueID();
		 
		if (datasetIDs.get(ID) == null) { // maps numericsample (profile line's) ID to a Set of unique series ids
			datasetIDs.put(ID, new HashSet<Integer>());
		}
		/*
		 * Create a new series of XY-data, with distances as the X-values and samples as
		 * Y-values. Add the start and end value of distance (i.e. domain) with null
		 * range value to the series. This enables the chart to zoom in and out
		 * properly. We need this because every time a zoom happens, we re-fetch and
		 * re-populate the data in the zoomed range at the new chart PPD.
		 */
		for (int datasourcecount = 0; datasourcecount < pipeline.length; datasourcecount++) {			
			XYSeries xyseries = new XYSeries(datasourcecount);
			xyseries.add(new Double(0), null);
			for (int i = 0; i < distances.length; i++) {
				xyseries.add(new Double(distances[i]), data[i] == null || data[i].length <= datasourcecount ? null
						: new Double(data[i][datasourcecount]));
				
			}
			xyseries.add(new Double(samples.lsegLengthKm), null);
			
			XYDataset newDataset = new XYSeriesCollection(xyseries);
			//check if we already have unique set of series ids for this ID
			// [ID - set of XYSeries unique ids, for ex, [ Profile ID 2 - set of XYSeries [4,7,9]; Profile ID 1 - set if XYSeries[5,11,23]

			int Index = datasetIndex;  //new every time
			
			ProfileLine profileline = (ProfileLine) this.profileLView.getProfilelineByID(ID);
			if (profileline == null) break;
			
			String uniquekey = profileline.getIdentifier() + this.chartData[datasourcecount].getSource().getTitle();
			
			seriesID_maptitle.put(Index, this.chartData != null ? this.chartData[datasourcecount].getSource().getTitle() : "");
			
			seriesID_unit.put(Index, this.chartData != null ? chartData[datasourcecount].getSource().getUnits() != null ?  chartData[datasourcecount].getSource().getUnits() : "" : "");
			
			datasetIDs.get(ID).add(Index);

			chart.getXYPlot().setDataset(Index, newDataset);
			
			chart.getXYPlot().mapDatasetToRangeAxis(Index, datasourcecount);
			
			XYItemRenderer r = null;
			
			if (!this.sourceRenderer.isEmpty()) {
				r = this.sourceRenderer.get(uniquekey);
				if (r != null) {
				    chart.getXYPlot().setRenderer(Index, r);
				}
			}
			if (r == null) {
				r = chart.getXYPlot().getRenderer(Index);	
			}
			if (r == null || !(r instanceof MyShapeRenderer)) {
				r = new MyShapeRenderer(profileline, datasourcecount);
				chart.getXYPlot().setRenderer(Index, r);
			}
			
			this.sourceRenderer.put(uniquekey, r);
			
			datasetIndex++;
		}
	}

	public XYItemRenderer getSourceRenderer(String sourceTitle) {
		XYItemRenderer r = null;
		if (!sourceRenderer.isEmpty()) {
		    r = sourceRenderer.get(sourceTitle);
		}
		return r;
	}

	public List<NumericMapDataSample> getNumericDataForPlots() {
		List<NumericMapDataSample> varnumsamples = new ArrayList<>();
		varnumsamples.addAll(numericDataForPlots);
		return varnumsamples;
	}

	public void saveAsText(File outputFile) throws FileNotFoundException {
		final String nullString = "N/A";

		log.println("Save as text requested to file " + outputFile.getName());
		String delim = ",";

		NumberFormat nf = NumberFormat.getInstance();
		nf.setGroupingUsed(false);
		nf.setMinimumFractionDigits(0);
		nf.setMinimumIntegerDigits(1);

		// Output header
		PrintStream ps = new PrintStream(new FileOutputStream(outputFile));
		ps.print("Distance (Km)" + delim + "Longitude" + delim + "Latitude");
		if (chartData != null) {
		for (int i = 0; i < chartData.length; i++)
			ps.print(delim + chartData[i].getSource().getTitle());
		}
		ps.println();

		// Output data
		for (NumericMapDataSample numericsample : numericDataForPlots) {
			int ID = numericsample.getUniqueID();										
			ProfileLine profileline = (ProfileLine) this.profileLView.getProfilelineByID(ID);
			if (profileline == null) continue;		
			if (isProfileNotVisibleInChart(ID)) continue;  //export only visible profile			
			double[] distances = numericsample.getDistances();
			Point2D[] samplePtsW = numericsample.getSamplePoints();
			double[][] sampleData = numericsample.getSampleData();
			for (int k = 0; k < numericsample.getNumSamples(); k++) {
				ps.print(nf.format(distances[k]));
				if (samplePtsW[k] != null) {
					Point2D ptSpatial =  Main.PO.convWorldToSpatial(samplePtsW[k]);
					// should output east-leading longitude, ocentric latitude
					ps.print(delim);
					ps.print(nf.format(360 - ptSpatial.getX()));
					ps.print(delim);
					ps.print(nf.format(ptSpatial.getY()));
				} else {
					ps.print(delim);
					ps.print(nullString);
					ps.print(delim);
					ps.print(nullString);
				}
				if (chartData != null) {
					for (int i = 0; i < chartData.length; i++) {
						ps.print(delim);
						ps.print(sampleData[k] == null ? nullString : nf.format(sampleData[k][i]));
					}
				}
				ps.println();
			}
	    }
	}
	

	@Override
	public void modelPropertyChange(PropertyChangeEvent evt) {
		String propName = evt.getPropertyName();
		Object newVal = evt.getNewValue();
		Supplier<IChartEventHandler> handler = EVENT_HANDLER.get(propName);
		if (handler != null) {
		    handler.get().handleEvent(newVal);
		}
	}		

	private void showPromptWhereToViewChart() {
		if (!this.isChartPanelDisplayed) {
			if (this.profileLView.chartFocusPanel != null) {
			if (!this.profileLView.chartFocusPanel.isShowing()) {
				showCallout(Main.testDriver.getQuickChartsLbl());
				this.isChartPanelDisplayed = true;
			}
		}
	  }	
	}

	private void disableChartSave() {
		saveAsTextMenuItem.setEnabled(false);
	}

	private void enableChartSave() {
		saveAsTextMenuItem.setEnabled(true);
	}	

	public void init() {
        controller.requestChartData();		
	}
	
	@Override
	public Dimension getPreferredSize() {
		Dimension preferred = super.getPreferredSize();
		preferred.width = Math.max(preferred.width, 800);
		return preferred;
	}	
	
	public static void setJTableColumnsWidth(JTable table, int tablePreferredWidth,
	        double... percentages) {
	    double total = 0;
	    for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
	        total += percentages[i];
	    }
	 
	    for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
	        TableColumn column = table.getColumnModel().getColumn(i);
	        column.setPreferredWidth((int)
	                (tablePreferredWidth * (percentages[i] / total)));
	    }
	}

	@Override
	public void tableChanged(TableModelEvent e) {
		if (!(e.getSource() instanceof ReadoutTableModel)) return;
		TableModel model = (ReadoutTableModel) e.getSource();
		ReadoutTableModel readoutmodel = ((ReadoutTableModel) model);
		List<PlotObject> modelData = readoutmodel.getAllPlotObjects();
		for (Iterator<PlotObject> itr = modelData.iterator(); itr.hasNext();) { // update plot visibility and
																				// color/style
																				// and "remember" these values
			PlotObject modelobj = itr.next();
			String uniqueid = modelobj.getUniqueID();
			XYItemRenderer r = null;
			if (!this.sourceRenderer.isEmpty()) {
				r = this.sourceRenderer.get(uniqueid);
				if (r != null) {
					Color color =  modelobj.getPlotColor();
					//if "many sources" then update from sourcerenderer
					if (color == null) {
						color = (Color) r.getSeriesPaint(0);
						modelobj.setPlotColor(color);
					}
					r.setSeriesVisible(0, modelobj.isVisible()); // only 1 series per renderer
					r.setSeriesPaint(0, color);
				}
			}
		}
		updateMaxAndSelectedPpd();
	}
	

	private void removeAllSeriesByDataset() {
		Set<Integer> setIDs = this.datasetIDs.keySet();	
		for (Integer datasetid : setIDs) {
			Set<Integer> seriesIDs = this.datasetIDs.get(datasetid);
			seriesIDs.stream().forEach(seriesID -> this.chart.getXYPlot().setDataset(seriesID, null));
		}	
		this.datasetIDs.clear();
		this.seriesID_maptitle.clear();
		this.seriesID_unit.clear();
		this.sourceRenderer.clear();
	}
	
	private void resetReadoutTable() {
		roTableModel.reset();		
	}

	public List<Integer> getListOfUniqueSeriesIDsForProfile(int profileID) {
		List<Integer> plotSeriesIDs = new ArrayList<>();
		Set<Integer> setIDs = datasetIDs.get(profileID);
		if (setIDs != null && !setIDs.isEmpty()) {
			plotSeriesIDs = new ArrayList<>(setIDs);
		}
		return plotSeriesIDs;
	}  
	
	public Map<Integer, Shape> getRequestedVisibleProfiles() {
		Map<Integer, Shape> varrequest = new HashMap<>();
		varrequest.putAll(this.request);
		return varrequest;
	}

	public NumericMapDataSample getNumericSampleByID(int ID) {
		for (NumericMapDataSample sample : numericDataForPlots) {
			if (sample.getUniqueID() == ID) {
				return sample;
			}
		}
		return null;
	}

	private void createCalloutUI() {
		JLabel dummy = new JLabel();
		EdgedBalloonStyle style = new EdgedBalloonStyle(ThemeSnackBar.getBackgroundStandard(), 
	                ThemeProvider.getInstance().getBackground().getBorder());
		 BalloonTip.setDefaultCloseButtonIcons(close, close, close);
		 myBalloonTip = new CustomBalloonTip(dummy,
				  dummy,
				  new Rectangle(),
				  style,
				  BalloonTip.Orientation.LEFT_ABOVE, BalloonTip.AttachLocation.CENTER,
				  10, 10,
				  true);	
		 myBalloonTip.setPadding(5);
		 JButton closebutton = BalloonTip.getDefaultCloseButton();
		 closebutton.setUI(new LikeLabelButtonUI());
		 myBalloonTip.setCloseButton(closebutton,false);		
		 myBalloonTip.setVisible(false);
	}	
	
	private void showCallout(Container parent2) {			
		if (myBalloonTip != null && parent2 != null) {
			if (parent2 instanceof JComponent) {
				JComponent comp = (JComponent) parent2;
				if (comp.getRootPane() == null) {
					return;
				}
				myBalloonTip.setAttachedComponent(comp);
				int xoffset = parent2.getWidth() / 2;
				int yoffset = parent2.getHeight();
				Rectangle rectoffset = new Rectangle(xoffset, yoffset - 12, 1, 1);
				Color foregroundtext = ThemeSnackBar.getForegroundStandard();
				String colorhex = edu.asu.jmars.ui.looknfeel.Utilities.getColorAsBrowserHex(foregroundtext);
				String html = "<html>" + "<p style=\"color:" + colorhex + "; padding:1em; text-align:center;\">" + "<b>"
						+ "View Chart" + "</b>" + "</p></html>";
				myBalloonTip.setTextContents(html);
				myBalloonTip.setOffset(rectoffset);
				myBalloonTip.setVisible(true);
				TimingUtils.showTimedBalloon(myBalloonTip, UserPrompt.LONG_TIME);
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
	
	
	// chart events
	private class CreateChartEventHandler implements IChartEventHandler {
		@Override
		public void handleEvent(Object newVal) {
			chartConfig = (Config) newVal;
			Map<Integer, Shape> inputprofiles = new HashMap<>();
			Pipeline[] newdata = null;
			if (chartConfig.getPipeline() != null) {
			      newdata = Arrays.stream(chartConfig.getPipeline()).toArray(Pipeline[]::new);	
			}
			CardLayout cl = (CardLayout) (cards.getLayout());
			if (newdata == null || newdata.length == 0) {
				cl.show(cards, EMPTYCMD);
			} else {
				cl.show(cards, CHARTCMD);
				initChartData(newdata);
				inputprofiles.putAll(chartConfig.getProfilesToChart());
				updateLegend(chartConfig);
				
				resetChartAxesAndRange();
				
				if (!inputprofiles.isEmpty()) {
					roTableModel.addData(profileChartView, chartConfig);
					ppdComboBox.removeItemListener(ppdactionListener);
					calculateMinOfAllMaxPPDsAndUpdateCombobox(inputprofiles);
					ppdComboBox.addItemListener(ppdactionListener);
					updatePlotSeries(inputprofiles, effRange);
				}
			}
		}

	
		private void updateLegend(Config chartConfig) {
			if (chartConfig.getConfigType() == ConfigType.ONENUMSOURCE) {
				MapSource source = chartConfig.getNumsourcesToChart().get(0);
				legendtext.setText(source.getTitle());
				legendline.setIcon(null);
			} else if (chartConfig.getConfigType() == ConfigType.MANYNUMSOURCES) {
				Map<Integer, Shape> profilemap = chartConfig.getProfilesToChart();
				if (profilemap.isEmpty()) {
					legendtext.setText("");
				    legendline.setIcon(null);
				} else {
					Shape shape = profilemap.entrySet().iterator().next().getValue();
					if (shape instanceof ProfileLine) {
						ProfileLine profile = (ProfileLine) shape;
						String txt = profile.getRename() != null ? profile.getRename() : profile.getIdentifier();
						ProfileTabelCellObject po = new ProfileTabelCellObject(txt);
						po.setColor(profile.getLinecolor());
						legendtext.setText(txt);
						legendline.setIcon(po.getLine());
					}
				}
			}
			
		}
	}
		

	private class PlotDataEventHandler implements IChartEventHandler {
		@Override
		public void handleEvent(Object newVal) {
			showPromptWhereToViewChart();
			Pair newval = (Pair) newVal;
			Pipeline[] sources = (Pipeline[]) newval.getKey();
			if (sources != null) {
				NumericMapDataSample numsample = (NumericMapDataSample) newval.getValue();								
				if ( !isDuplicateNumSourcePerProfile(numsample.getUniqueID())) {
						numericDataForPlots.add(numsample);
				}
				if (!plotSeries.isEmpty()) {
					populatePlot(sources, numsample);
					buildDataForChartReadoutTable(0);	
					enableChartSave();
				}
			}
		}

		private boolean isDuplicateNumSourcePerProfile(int ID) {
			for (NumericMapDataSample numsample : numericDataForPlots) {
				if (numsample.getUniqueID() == ID) {
					return true;
				}
			}
			return false;
		}
	}


	private class UpdateCrosshairEventHandler implements IChartEventHandler {
		@Override
		public void handleEvent(Object newVal) {
			Pair newval = (Pair) newVal;
			int ID = (Integer) (newval).getKey();
			Point2D worldCuePoint = (Point2D) (newval).getValue();
			updateCrosshairWithNewPosition(ID, worldCuePoint);
		}
	}	
	
	private class ResetNewConfig implements IChartEventHandler {
		@Override
		public void handleEvent(Object newVal) {
			chartData = null;
			plotSeries.clear();
			resetReadoutTable();
			removeAllSeriesByDataset(); 
		}
	}

	public void close() {
		if (morepropsframe != null) {
			if (morepropsframe.isVisible()) {
				morepropsframe.setVisible(false);
			}
			morepropsframe.dispose();
		}
	}
}
