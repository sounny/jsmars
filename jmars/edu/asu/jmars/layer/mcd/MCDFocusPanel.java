package edu.asu.jmars.layer.mcd;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Vector;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.Layer.LView;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.swing.ExportTableAction;
import edu.asu.jmars.swing.ImportantMessagePanel;
import edu.asu.jmars.swing.TableColumnAdjuster;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeChart;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemePanel;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;

@SuppressWarnings("serial")
public class MCDFocusPanel extends FocusPanel{

	private JTable inputDataTbl;
	private JPanel inputTablePnl;
	private JTable resultsDataTbl;
	private JPanel resultsTablePnl;
	
	private JTable heightROTbl;
	private JTable pressureROTbl;
	private JTable lsROTbl;
	private JTable ltROTbl;
	private JScrollPane heightROSP;
	private JScrollPane pressureROSP;
	private JScrollPane lsROSP;
	private JScrollPane ltROSP;
	
	private JPanel heightROPnl;
	private JPanel pressureROPnl;
	private JPanel lsROPnl;
	private JPanel ltROPnl;
	
	private JButton addPointBtn;
	private JLabel noDataLbl1;
	private JLabel noDataLbl2;
	private JLabel noDataLbl3;
	private JLabel noDataLbl4;
	private ChartPanel heightCPnl;
	private ChartPanel pressureCPnl;
	private ChartPanel lsCPnl;
	private ChartPanel ltCPnl;
	private JFreeChart heightChart;
	private JFreeChart pressureChart;
	private JFreeChart lsChart;
	private JFreeChart ltChart;
	private Crosshair heightCH;
	private Crosshair pressureCH;
	private Crosshair lsCH;
	private Crosshair ltCH;
	private JScrollPane inputTableSP;
	private JScrollPane resultsTableSP;
	private JButton setInputBtn;
	private JButton createFromInputBtn;
	private JButton runMcdBtn;
	private JTextField lsTf;
	private JTextField hourTf;
	private JTextField heightTf;
	private JComboBox<Integer> scenarioBx;
	private JTextField elevationsTf;
	private JLabel lsLbl;
	private JLabel hourLbl;
	private JLabel heightLbl;
	private JLabel scenarioLbl;
	private JLabel elevationsLbl;
	private JLabel htHelpLbl;
	private JLabel ltHelpLbl;
	private JLabel lsHelpLbl;
	private JLabel scenHelpLbl;
	private JLabel elevationsHelpLbl;
	private JTabbedPane outputTp;
	private JCheckBox pointChk;
	private JCheckBox labelChk;
	private ColorCombo outlineCc;
	private ColorCombo fillCc;
	private ColorCombo labelCc;
	private JComboBox<Integer> sizeBx;
	private JLabel outlineLbl;
	private JLabel fillLbl;
	private JLabel sizeLbl;
	private JLabel fontLbl;
	
	private JComboBox<String> heightPlot = new JComboBox<String>();
	private JComboBox<String> pressurePlot = new JComboBox<String>();;
	private JComboBox<String> ltPlot = new JComboBox<String>();
	private JComboBox<String> lsPlot = new JComboBox<String>();
	
	private ArrayList<String> statFailures = new ArrayList<String>();
	
	private final int HEIGHT_DATA = 1;
	private final int LS_DATA = 2;
	private final int LT_DATA = 3;
	private final int PRESSURE_DATA = 4;
	
	//UI constants
	private int row = 0;
	private int col = 0;
	private int pad = 2;
	private Insets in = new Insets(pad,pad,pad,pad);
	
	private int textFieldSize = 8;
	private Dimension inputTableDim = new Dimension(100, 200);
	private Dimension resultsTableDim = new Dimension(100, 300);

	private DecimalFormat xFormat = new DecimalFormat("###,###");
	private Range heightRange = new Range(0,100000);
	private Range pressureRange = new Range(0, 600);
	private Range lsRange = new Range(0,360);
	private Range ltRange = new Range(0,24);
	private NumberTickUnit ltStep = new NumberTickUnit(2);
	private NumberTickUnit lsStep = new NumberTickUnit(45);
	private boolean flipXyHeightData = false;
	private boolean useLogscaleHeightData = false;
	
	ActionListener plotChangedListener;

	/**
	 * The selected MCD datapoint from the input data table. Is null if no
	 * selection is made
	 */
	private MCDDataPoint selectedDataPoint;
	
	private MCDLView myLView;
	private MCDLayer myLayer;
	
	/**
	 * Create the Focus panel for the MCD Layer.  Currently
	 * has a "Data" tab to display all the information for 
	 * the layer
	 * @param parent
	 */
	public MCDFocusPanel(LView parent) {
		super(parent, false);
		myLView = (MCDLView) parent;
		myLayer = (MCDLayer) myLView.getLayer();
		
		add("Data", createDataPanel());
		add("Results", createResultsPanel());
	}
	
	private JPanel createDataPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		Color back = ((ThemePanel)GUITheme.get("panel")).getBackground();
		panel.setBackground(back);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		//data table panel
		inputTablePnl = new JPanel(new BorderLayout());
		inputTablePnl.setBorder(new TitledBorder("Data Points"));
		inputDataTbl = createDataTable(true);
		inputTableSP = new JScrollPane(inputDataTbl);		
		addPointBtn = new JButton(addPointAct);
		JPanel addPnl = new JPanel();
		addPnl.add(addPointBtn);
		inputTablePnl.add(inputTableSP, BorderLayout.CENTER);
		inputTablePnl.add(addPnl, BorderLayout.SOUTH);	
		inputTablePnl.setPreferredSize(inputTableDim);
		inputTablePnl.setMinimumSize(inputTableDim);
		
		//display panel
		JPanel dispPnl = new JPanel(new GridBagLayout());
		dispPnl.setBorder(new TitledBorder("Display Options"));
		pointChk = new JCheckBox(checkboxAct);
		pointChk.setText("Point");
		outlineLbl = new JLabel("Outline:");
		Dimension boxDim = new Dimension(50,20);
		outlineCc = new ColorCombo(Color.BLACK);
		outlineCc.setPreferredSize(boxDim);
		outlineCc.addActionListener(boxListener);
		fillLbl = new JLabel("Fill:");
		fillCc = new ColorCombo(Color.WHITE);
		fillCc.setPreferredSize(boxDim);
		fillCc.addActionListener(boxListener);
		labelChk = new JCheckBox(checkboxAct);
		labelChk.setText("Label");
		sizeLbl = new JLabel("Size:");
		Vector<Integer> sizes = new Vector<Integer>();
		sizes.add(10);
		sizes.add(11);
		sizes.add(12);
		sizes.add(13);
		sizes.add(14);
		sizes.add(15);
		sizes.add(16);
		sizes.add(17);
		sizes.add(18);
		sizeBx = new JComboBox<Integer>(sizes);
		sizeBx.setPreferredSize(boxDim);
		sizeBx.addActionListener(boxListener);
		fontLbl = new JLabel("Fill:");
		labelCc = new ColorCombo(Color.WHITE);
		labelCc.setPreferredSize(boxDim);
		labelCc.addActionListener(boxListener);

		row = 0; col = -1;
		dispPnl.add(pointChk, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		dispPnl.add(outlineLbl, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		dispPnl.add(outlineCc, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		dispPnl.add(fillLbl, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		dispPnl.add(fillCc, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		dispPnl.add(Box.createHorizontalStrut(5), new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL, in, pad, pad));
		dispPnl.add(labelChk, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		dispPnl.add(sizeLbl, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		dispPnl.add(sizeBx, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		dispPnl.add(fontLbl, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		dispPnl.add(labelCc, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		
		
		//input panel
		//TODO: make input text fields have a document filter 
		// allowing valid numbers to be entered
		JPanel inputPnl = new JPanel(new GridBagLayout());
		inputPnl.setBorder(new TitledBorder("Inputs"));
		String lsHint = "Range: 0-360";
		lsLbl = new JLabel("Ls:");
		lsLbl.setToolTipText(lsHint);
		lsTf = new JTextField(textFieldSize);
		lsTf.setToolTipText(lsHint);
		String hourHint = "Range: 0-24";
		hourLbl = new JLabel("Local Time (Hour):");
		hourLbl.setToolTipText(hourHint);
		hourTf = new JTextField(textFieldSize);
		hourTf.setToolTipText(hourHint);
		String heightHint = "Range: 0-100,000";
		heightLbl = new JLabel("Height above surface (m):");
		heightLbl.setToolTipText(heightHint);
		heightTf = new JTextField(textFieldSize);
		heightTf.setToolTipText(heightHint);
		scenarioLbl = new JLabel("Scenario:");
		Vector<Integer> scenarioList = new Vector<Integer>();
		for(int i=1; i<9; i++){
			scenarioList.add(i);
		}
		scenarioBx = new JComboBox<Integer>(scenarioList);
		elevationsLbl = new JLabel("Elevations:");
		String elevationsHint = "Example: 0,1,10,100,1000,10000,10000";
		elevationsLbl.setToolTipText(elevationsHint);
		elevationsTf = new JTextField("");
		elevationsTf.setToolTipText(elevationsHint);
		
		//help labels
		htHelpLbl = new JLabel("Used as a constant when computing Local Time and Ls plots");
		ltHelpLbl = new JLabel("Used as a constant when computing Height and Ls plots");
		lsHelpLbl = new JLabel("Used as a constant when computing Height and Local Time plots");
		scenHelpLbl = new JLabel("Please see info panel for description of each scenario");
		elevationsHelpLbl = new JLabel("Used as the discrete set of values to use when computing Height plots");
		Font plainFont = ThemeFont.getRegular();
		htHelpLbl.setFont(plainFont);
		lsHelpLbl.setFont(plainFont);
		ltHelpLbl.setFont(plainFont);
		scenHelpLbl.setFont(plainFont);
		elevationsHelpLbl.setFont(plainFont);
		
		JPanel buttonPnl = new JPanel();
		setInputBtn = new JButton(setInputsAct);
		createFromInputBtn = new JButton(createFromInputsAct);
		buttonPnl.add(setInputBtn);
		buttonPnl.add(Box.createHorizontalStrut(5));
		buttonPnl.add(createFromInputBtn);
		
		//disable all input fields because a row has not been selected
		refreshInputs();
		
		row = 0; col = -1;
		inputPnl.add(hourLbl, new GridBagConstraints(++col, row, 1, 1, 0.5, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(hourTf, new GridBagConstraints(++col, row, 1, 1, 0.5, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(ltHelpLbl, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(new JSeparator(), new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		row++; col = -1;
		inputPnl.add(lsLbl, new GridBagConstraints(++col, row, 1, 1, 0.5, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(lsTf, new GridBagConstraints(++col, row, 1, 1, 1, 0.5, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(lsHelpLbl, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(new JSeparator(), new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		row++; col = -1;
		inputPnl.add(heightLbl, new GridBagConstraints(++col, row, 1, 1, 0.5, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(heightTf, new GridBagConstraints(++col, row, 1, 1, 0.5, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(htHelpLbl, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(new JSeparator(), new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		row++; col = -1;
		inputPnl.add(scenarioLbl, new GridBagConstraints(++col, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(scenarioBx, new GridBagConstraints(++col, row, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(scenHelpLbl, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(new JSeparator(), new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		row++; col = -1;
		inputPnl.add(elevationsLbl, new GridBagConstraints(++col, row, 1, 1, 0.5, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(elevationsTf, new GridBagConstraints(++col, row, 1, 1, 1, 0.5, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
		inputPnl.add(elevationsHelpLbl, new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		inputPnl.add(new JSeparator(), new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		row++; col = -1;

		inputPnl.add(buttonPnl, new GridBagConstraints(++col, row, 8, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		//disable all display options until a row is selected
		refreshDisplayOptions();
		
		//add everything to display
		JPanel cenPnl = new JPanel(new GridBagLayout());		
		cenPnl.setBackground(back);
		row = 0;
		pad = 2;
		cenPnl.add(inputPnl, new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		cenPnl.add(Box.createVerticalStrut(5),new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
		cenPnl.add(dispPnl, new GridBagConstraints(0, ++row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));

		panel.add(inputTablePnl, BorderLayout.NORTH);
		panel.add(cenPnl, BorderLayout.CENTER);		
//		panel.setMinimumSize(new Dimension(200,0));
		
		plotChangedListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					refreshCharts();
				}
			};
			
		return panel;
	}
	
	private JPanel createResultsPanel(){
		JPanel panel = new JPanel(new BorderLayout());
		Color back = ((ThemePanel)GUITheme.get("panel")).getBackground();
		panel.setBackground(back);
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		panel.setPreferredSize(new Dimension(0, 750));
		
		//data table panel
		resultsTablePnl = new JPanel(new BorderLayout());
		resultsTablePnl.setBorder(new TitledBorder("Data Points"));
		resultsDataTbl = createDataTable(false);		
		resultsTableSP = new JScrollPane(resultsDataTbl);
		
		String runNote = "Running MCD may take ~30 seconds to complete, while running in the background. A dialog will notify you when finished.";
		ImportantMessagePanel notePnl = new ImportantMessagePanel(runNote);
		
		runMcdBtn = new JButton(runAct);
		runMcdBtn.setEnabled(false);
		JPanel runPnl = new JPanel(new GridBagLayout());
		
		runPnl.add(notePnl, new GridBagConstraints(0, 0, 1, 2, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(10,10,10,10), pad, pad));
		runPnl.add(runMcdBtn, new GridBagConstraints(0, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		
		resultsTablePnl.add(resultsTableSP, BorderLayout.CENTER);		
		resultsTablePnl.add(runPnl, BorderLayout.SOUTH);	
		resultsTablePnl.setPreferredSize(resultsTableDim);
		resultsTablePnl.setMinimumSize(resultsTableDim);
		
		//output charts
		heightChart = null;
		pressureChart = null;
		ltChart = null;
		lsChart = null;
		heightCPnl = new ChartPanel(heightChart, true);
		pressureCPnl = new ChartPanel(pressureChart, true);
		ltCPnl = new ChartPanel(ltChart, true);
		lsCPnl = new ChartPanel(lsChart, true);
		//add chart listener (to update the readout tables)
		heightCPnl.addChartMouseListener(heightChartListener);
		pressureCPnl.addChartMouseListener(pressureChartListener);
		ltCPnl.addChartMouseListener(ltChartListener);
		lsCPnl.addChartMouseListener(lsChartListener);
		//add crosshair overlay, so the chart doesn't need to be redrawn
		// when the crosshair moves
		CrosshairOverlay heightOverlay = new CrosshairOverlay();
		heightCH = new Crosshair(0, Color.BLUE, new BasicStroke(1.0f));
		heightOverlay.addDomainCrosshair(heightCH);
		heightCPnl.addOverlay(heightOverlay);
		CrosshairOverlay pressureOverlay = new CrosshairOverlay();
		pressureCH = new Crosshair(0, Color.BLUE, new BasicStroke(1.0f));
		pressureOverlay.addDomainCrosshair(pressureCH);
		pressureCPnl.addOverlay(pressureOverlay);
	    CrosshairOverlay ltOverlay = new CrosshairOverlay();
	    ltCH = new Crosshair(0, Color.BLUE, new BasicStroke(1.0f));
	    ltOverlay.addDomainCrosshair(ltCH);
	    ltCPnl.addOverlay(ltOverlay);
	    CrosshairOverlay lsOverlay = new CrosshairOverlay();
	    lsCH = new Crosshair(0, Color.BLUE, new BasicStroke(1.0f));
	    lsOverlay.addDomainCrosshair(lsCH);
	    lsCPnl.addOverlay(lsOverlay);
	    
		//add x/y flip option to the height panel
		heightCPnl.getPopupMenu().add(createXYFlipItem(HEIGHT_DATA));
		//add log scale option to height panel
		heightCPnl.getPopupMenu().add(createLogItem(HEIGHT_DATA));
	    
		//add csv export option to chart panels
	    heightCPnl.getPopupMenu().add(createCSVMenuItem(HEIGHT_DATA));
	    pressureCPnl.getPopupMenu().add(createCSVMenuItem(PRESSURE_DATA));
		ltCPnl.getPopupMenu().add(createCSVMenuItem(LT_DATA));
		lsCPnl.getPopupMenu().add(createCSVMenuItem(LS_DATA));
		String noDataStr = "No Data Available.  Please run MCD.";
		noDataLbl1 = new JLabel(noDataStr);
		noDataLbl2 = new JLabel(noDataStr);
		noDataLbl3 = new JLabel(noDataStr);
		noDataLbl4 = new JLabel(noDataStr);
		
		//add the readout tables to the day and year panels
		//height panel
		JPanel heightPnl = new JPanel(new BorderLayout());
		heightROTbl = createReadoutTable(HEIGHT_DATA);
		heightROSP = new JScrollPane(heightROTbl);

		//stick the scroll pane in a panel with a fixed size
		heightROPnl = new JPanel(new GridLayout(1,1));
		Dimension roSize = new Dimension(0,150);
		heightROPnl.setMinimumSize(roSize);
		heightROPnl.setPreferredSize(roSize);
		heightROPnl.add(heightROSP);
				
		//populate the height panel
		heightPnl.add(heightPlot, BorderLayout.NORTH);
		heightPnl.add(heightCPnl, BorderLayout.CENTER);
		heightPnl.add(heightROPnl, BorderLayout.SOUTH);		
		
		//pressure panel
		JPanel pressurePnl = new JPanel(new BorderLayout());
		pressureROTbl = createReadoutTable(PRESSURE_DATA);
		pressureROSP = new JScrollPane(pressureROTbl);

		//stick the scroll pane in a panel with a fixed size
		pressureROPnl = new JPanel(new GridLayout(1,1));
		pressureROPnl.setMinimumSize(roSize);
		pressureROPnl.setPreferredSize(roSize);
		pressureROPnl.add(pressureROSP);
				
		//populate the pressure panel
		pressurePnl.add(pressurePlot, BorderLayout.NORTH);
		pressurePnl.add(pressureCPnl, BorderLayout.CENTER);
		pressurePnl.add(pressureROPnl, BorderLayout.SOUTH);		
				
		//day panel
		JPanel ltPnl = new JPanel(new BorderLayout());
		ltROTbl = createReadoutTable(LT_DATA);
		ltROSP = new JScrollPane(ltROTbl);

		//stick the scroll pane in a panel with a fixed size
		ltROPnl = new JPanel(new GridLayout(1,1));
		ltROPnl.setMinimumSize(roSize);
		ltROPnl.setPreferredSize(roSize);
		ltROPnl.add(ltROSP);
		
		//populate the day panel
		ltPnl.add(ltPlot, BorderLayout.NORTH);
		ltPnl.add(ltCPnl, BorderLayout.CENTER);
		ltPnl.add(ltROPnl, BorderLayout.SOUTH);		

		//year panel
		JPanel lsPnl = new JPanel(new BorderLayout());
		lsROTbl = createReadoutTable(LS_DATA);
		lsROSP = new JScrollPane(lsROTbl);

		//stick the scroll pane in a panel with a fixed size
		lsROPnl = new JPanel(new GridLayout(1,1));
		lsROPnl.setMinimumSize(roSize);
		lsROPnl.setPreferredSize(roSize);
		lsROPnl.add(lsROSP);
		
		//populate the year panel
		lsPnl.add(lsPlot, BorderLayout.NORTH);
		lsPnl.add(lsCPnl, BorderLayout.CENTER);
		lsPnl.add(lsROPnl, BorderLayout.SOUTH);		 

		//output tabbed pane
		outputTp = new JTabbedPane(JTabbedPane.TOP);		
		outputTp.setPreferredSize(new Dimension(100,250));
		outputTp.add("Height".toUpperCase(), heightPnl);
		outputTp.add("Pressure".toUpperCase(), pressurePnl);
		outputTp.add("Local Time".toUpperCase(), ltPnl);
		outputTp.add("Ls".toUpperCase(), lsPnl);

		//refresh charts
		refreshCharts();
		
		//add everything for display
		panel.add(resultsTablePnl, BorderLayout.NORTH);
		panel.add(outputTp, BorderLayout.CENTER);		
		
		return panel;
	}
	
	private JMenuItem createCSVMenuItem(final int data){
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
						String name = f.getName();
						if(name.contains(".csv") || name.contains(".txt")){
							return true;
						}
						return false;
					}
				});
				
				int result = fileChooser.showSaveDialog(MCDFocusPanel.this);
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
							saveAsText(file, data);
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
	
	private void saveAsText(File outputFile, int data) throws FileNotFoundException{
		JFreeChart chart = null;
		String xAxis = "";
		if(data == HEIGHT_DATA){
			chart = heightChart;
			xAxis = "Height";
		}
		else if(data == PRESSURE_DATA){
			chart = pressureChart;
			xAxis = "Pressure";
		}		
		else if(data == LT_DATA){
			chart = ltChart;
			xAxis = "Hour";
		}
		else if(data == LS_DATA){
			chart = lsChart;
			xAxis = "Ls";
		}
		
		if(chart == null || xAxis.length()==0){
			return;
		}
		
		XYDataset dataset = chart.getXYPlot().getDataset(0);
		
		String delim = ",";
		PrintStream ps = new PrintStream(outputFile);
		//Header
		ps.print(xAxis);
		for(int i=0; i<dataset.getSeriesCount(); i++){
			ps.print(delim+dataset.getSeriesKey(i));
		}
		ps.println();
		
		//Data
		//cycle through all values of x
		for(int j=0; j<dataset.getItemCount(0); j++){
			//print the x value
			ps.print(dataset.getX(0, j));
			//print the y value of each plot at that x
			for(int i=0; i<dataset.getSeriesCount(); i++){
				ps.print(delim + dataset.getYValue(i, j));
			}
			ps.println();
		}
		
		ps.close();
	}
	
	private JMenuItem createXYFlipItem(final int dataType){
		JMenuItem itm = new JMenuItem("Flip X/Y Axis"); 
		
		itm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(dataType == HEIGHT_DATA){
					flipXyHeightData = !flipXyHeightData;
					refreshCharts();
				}
			}
		});
		
		return itm;
	}
	
	private JMenuItem createLogItem(final int dataType){
		final String logName = "Use Log Scale for Height Axis";
		final String linearName = "Use Linear Scale for Height Axis";
		final JMenuItem itm = new JMenuItem(logName);
		itm.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(dataType == HEIGHT_DATA){
					useLogscaleHeightData = !useLogscaleHeightData;
					refreshCharts();
					//change the item name
					if(useLogscaleHeightData){
						itm.setText(linearName);
					}else{
						itm.setText(logName);
					}
				}
			}
		});
		
		return itm;
	}
	
	
	private AbstractAction addPointAct = new AbstractAction("Create New Data Point...".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			new AddDataPointDialog(MCDFocusPanel.this.getFrame(), myLView, "", null, null, null, false);
		}
	};
	
	private AbstractAction checkboxAct = new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
			int imageState = myLayer.getStateId(MCDLayer.IMAGES_BUFFER);
			int labelState = myLayer.getStateId(MCDLayer.LABELS_BUFFER);

			if(e.getSource() == pointChk){
				//only set boolean and trigger state change if different
				boolean sel = pointChk.isSelected();
				if(sel != selectedDataPoint.showPoint()){
					selectedDataPoint.setShowPoint(sel);
					//update proper state id buffer
					myLayer.increaseStateId(MCDLayer.IMAGES_BUFFER);
				}
			}
			if(e.getSource() == labelChk){
				//only set boolean and trigger state change if different
				boolean sel = labelChk.isSelected();
				if(sel != selectedDataPoint.showLabel()){
					selectedDataPoint.setShowLabel(sel);
					//update proper state id buffer
					myLayer.increaseStateId(MCDLayer.LABELS_BUFFER);
				}
			}
			//update the display options
			refreshDisplayOptions();
			//refresh lview
			parent.repaint();
			
			//update 3d if something changed
			if(myLayer.getStateId(MCDLayer.IMAGES_BUFFER)!=imageState || myLayer.getStateId(MCDLayer.LABELS_BUFFER)!=labelState){
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
				}	
			}
		}
	};
	
	private ActionListener boxListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			int imageState = myLayer.getStateId(MCDLayer.IMAGES_BUFFER);
			int labelState = myLayer.getStateId(MCDLayer.LABELS_BUFFER);
			
			//outline color
			if(e.getSource() == outlineCc){
				//only set the color and trigger state change if it's different
				Color c = outlineCc.getColor();
				if(c != selectedDataPoint.getOutlineColor()){
					selectedDataPoint.setOutlineColor(c);
					//update proper state id buffer
					myLayer.increaseStateId(MCDLayer.IMAGES_BUFFER);
				}
			}
			//fill color
			if(e.getSource() == fillCc){
				//only set the color and trigger state change if it's different
				Color c = fillCc.getColor();
				if(c != selectedDataPoint.getFillColor()){
					selectedDataPoint.setFillColor(c);
					//update proper state id buffer
					myLayer.increaseStateId(MCDLayer.IMAGES_BUFFER);
				}			}
			//label size
			if(e.getSource() == sizeBx){
				//only set the size and trigger state change if it's different
				int size = (int)sizeBx.getSelectedItem();
				if(size != selectedDataPoint.getFontSize()){
					selectedDataPoint.setFontSize(size);
					//update proper state id buffer
					myLayer.increaseStateId(MCDLayer.LABELS_BUFFER);
				}
			}
			//label color
			if(e.getSource() == labelCc){
				//only set color and trigger state change if it's different
				Color c = labelCc.getColor();
				if(c != selectedDataPoint.getLabelColor()){
					selectedDataPoint.setLabelColor(c);
					//update proper state id buffer
					myLayer.increaseStateId(MCDLayer.LABELS_BUFFER);
				}
			}
			parent.repaint();
			
			//update 3d if something changed
			if(myLayer.getStateId(MCDLayer.IMAGES_BUFFER)!=imageState || myLayer.getStateId(MCDLayer.LABELS_BUFFER)!=labelState){
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
				}
			}
		}
	};
	
	private AbstractAction setInputsAct = new AbstractAction("Set Inputs for Selected Data Point".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			String check = checkInputs();
			if(!check.equals("")){
				String errorText = "One or more inputs is invalid. See the following failures:";
				String[] errors = check.split(",");
				for(String error : errors){
					errorText += "\n"+error;
				}
				Util.showMessageDialog(errorText, "Invalid Input(s)", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			setInputsOnDataPoint(selectedDataPoint);
			refreshCharts();
		}
	};
	
	private AbstractAction createFromInputsAct = new AbstractAction("Create New Data Point from Inputs".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			//create a new name based on the old one
			int counter = 0;
			String name = selectedDataPoint.getName();
			//if it already has a counter prefix, remove that 
			// before continuing
			if(name.contains("(")){
				name = name.substring(0, name.length()-4);
			}
			for(MCDDataPoint pt : myLayer.getMCDDataPoints()){
				if(pt.getName().contains(name)){
					counter++;
				}
			}
			name += " ("+counter+")";
			
			MCDDataPoint newPt = new MCDDataPoint(name, selectedDataPoint.getLat(), selectedDataPoint.getLon());
			setInputsOnDataPoint(newPt);
			
			//add to layer
			myLayer.addDataPoint(newPt);
			//update focus panel
			refreshDataTables();
			//refresh lview
			myLView.repaint();
		}
	};

	private AbstractAction runAct = new AbstractAction("Run MCD for Necessary Data Point(s)".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			
			//update the layer indicator while it's running
			myLayer.setStatus(Util.darkRed);
			
			Thread manager = new Thread(new Runnable() {
				public void run() {
					
				    runMcdBtn.setEnabled(false);
				    
					//get selected datapoints
					int[] rows = resultsDataTbl.getSelectedRows();
					for(int row : rows){
						MCDDataPoint dp = ((DataTableModel)resultsDataTbl.getModel()).getDataPoint(row);
						//only run mcd if it needs to be run
						if(dp.elevationData == null) { // || dp.lsData == null || dp.timeData == null || dp.pressureData == null){
							runMCD(dp);
						}
					}
					//once the output is generated, refresh the table and charts display
					refreshResultTable();
				    refreshCharts();
				    
				    //reset the selected rows
				    for(int row : rows){
				    	resultsDataTbl.getSelectionModel().addSelectionInterval(row, row);
				    }
				    
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							myLayer.setStatus(Util.darkGreen);
							Util.showMessageDialog("MCD Run Complete!", "Successful MCD Run", JOptionPane.INFORMATION_MESSAGE);
						}
					});
				}
			});
			
			manager.start();
		}
	};
	
	/**
	 * @return Returns true if all inputs are not empty and can be 
	 * parsed into Doubles.
	 */
	private String checkInputs(){		
		//comma separated list of failures
		String failures = "";
		//check to make sure the inputs aren't empty
		String lsTxt = lsTf.getText();
		if(lsTxt.length() == 0){
			failures += lsLbl.getText()+" was null."+",";
		}
		String hourTxt = hourTf.getText();
		if(hourTxt.length() == 0){
			failures += hourLbl.getText()+" was null."+",";
		}
		String heightTxt = heightTf.getText();
		if(heightTxt.length() == 0){
			failures += heightLbl.getText()+" was null."+",";
		}
		String elevationsTxt = elevationsTf.getText();
		if (elevationsTxt.length() == 0) {
			failures += elevationsLbl.getText()+" was null."+",";
		}
		
		//all inputs should be valid doubles
		double ls = -1;
		double hour = -1;
		double height = -1;
		try{
			ls = Double.parseDouble(lsTxt);
			hour = Double.parseDouble(hourTxt);
			height = Double.parseDouble(heightTxt);
		}catch(NumberFormatException e){
			failures += "One or more inputs was not a valid number."+",";
			return failures;
		}
		
		//l sub s should go from 0 to 360
		if(ls<0 || ls>360){
			failures += lsLbl.getText()+" must be between 0 to 360."+",";
		}
		//hour must go from 0 to 24
		if(hour<0 || hour>24){
			failures += hourLbl.getText()+" must be between 0 to 24."+",";
		}
		//height must be between 0 and 100,000
		if(height<0 || height>100000){
			failures += heightLbl.getText()+" must be between 0 to 100000.";
		}
		
		return failures;
	}
	
	private void runMCD(MCDDataPoint dp) {
		String serverURL = Config.get("mcd.url");
		
		// *** Note the updated URL below, accessing *** MRCRunner2 ***  Old versions of JMARS may still access MRCRunner
		//all data point related info
		String urlStr = "MCDRunner2?latitude="+dp.getLat()
				+"&longitude="+dp.getLon()+"&ls="+dp.getLSubS()
				+"&local_time="+dp.getHour()+"&height="+dp.getHeight()
				+"&scenario="+dp.getScenario()+"&elevations="+dp.getElevations()
				+"&version="+MCDLayer.SCRIPT_VERSION;
		
		//general script info
//		urlStr += "&process="+MCDLayer.POINT_MODE_PROCESS+"&version="+MCDLayer.SCRIPT_VERSION;
		//add user name
		urlStr += "&user="+Main.USER+"&product="+Main.PRODUCT+"&domain="+Main.AUTH_DOMAIN;
		
		int idx = urlStr.indexOf("?");
		
		String connStr = serverURL + urlStr.substring(0, idx);
		String dataStr = urlStr.substring(idx+1);
		
		String scriptOutput = "Script Output:";
		try{
			JmarsHttpRequest req = new JmarsHttpRequest(connStr, HttpRequestType.POST);
			req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
			req.addOutputData(dataStr);
			req.setConnectionTimeout(60*1000);
			req.setReadTimeout(600*1000);
			req.send();
			ObjectInputStream ois = new ObjectInputStream(req.getResponseAsStream());
			
			Integer numEntries = (Integer)ois.readObject();
						
			for (int i=0; i<numEntries; i++) {
				ArrayList<String> colNames = (ArrayList<String>)ois.readObject(); 
				ArrayList<ArrayList<Double>> colVals = (ArrayList<ArrayList<Double>>)ois.readObject();
				
				switch (i) {
					case 0:
						dp.elevationData=new MCDData("Elevation", colNames, colVals);
						break;
					case 1:
						dp.lsData=new MCDData("Ls", colNames, colVals);
						break;
					case 2:
						dp.timeData=new MCDData("Local Time", colNames, colVals);
						break;
					case 3:
						// TODO: Do we actually do this case, or just reuse elevation?
						dp.pressureData=new MCDData("Pressure", colNames, colVals);
						default:
				}
			}

			heightPlot.removeActionListener(plotChangedListener);
			pressurePlot.removeActionListener(plotChangedListener);
			lsPlot.removeActionListener(plotChangedListener);
			ltPlot.removeActionListener(plotChangedListener);

			DefaultComboBoxModel<String> ecmb = new DefaultComboBoxModel(dp.elevationData.columnNames.toArray());
			heightPlot.setModel(ecmb);
			heightPlot.setSelectedIndex(1);
			
			DefaultComboBoxModel<String> tcmb = new DefaultComboBoxModel(dp.timeData.columnNames.toArray());
			ltPlot.setModel(tcmb);
			ltPlot.setSelectedIndex(1);

			DefaultComboBoxModel<String> lcmb = new DefaultComboBoxModel(dp.lsData.columnNames.toArray());
			lsPlot.setModel(lcmb);
			lsPlot.setSelectedIndex(1);

			DefaultComboBoxModel<String> pcmb = new DefaultComboBoxModel(dp.elevationData.columnNames.toArray());
			pressurePlot.setModel(pcmb);
			pressurePlot.setSelectedIndex(1);

			heightPlot.addActionListener(plotChangedListener);
			pressurePlot.addActionListener(plotChangedListener);
			lsPlot.addActionListener(plotChangedListener);
			ltPlot.addActionListener(plotChangedListener);

			scriptOutput += (String) ois.readObject();
			ois.close();
			req.close();
		}
		catch(Exception e){
			e.printStackTrace();
			System.err.println(scriptOutput);
		}
	}
	
	
	private JTable createDataTable(boolean forInput){
		DataTableModel tm = new DataTableModel(myLayer.getMCDDataPoints(), forInput);
		JTable table = new DataTable(tm);
		ListSelectionModel lsm = table.getSelectionModel();
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		if(forInput){
			lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			lsm.addListSelectionListener(inputTableListener);
			table.addMouseListener(inputTableMouseListener);
		}else{
			//add a different listener for the resultTable
			lsm.addListSelectionListener(resultsTableListener);					
			table.addMouseListener(resultsTableMouseListener);
		}
		
		TableColumnAdjuster tca = new TableColumnAdjuster(table);
		tca.adjustColumns();
		
		return table;
	}
	
	private JTable createReadoutTable(int dataType){

		ArrayList<MCDDataPoint> data = new ArrayList<MCDDataPoint>();
		String tableModelType = "";
		JFreeChart chart = null;
		
		JComboBox plotChoice=null;
		//height plot
		if(dataType == HEIGHT_DATA){
			tableModelType = ReadoutTableModel.HEIGHT;
			chart = heightChart;
			plotChoice = heightPlot;
		}
		//pressure plot
		else if(dataType == PRESSURE_DATA){
			tableModelType = ReadoutTableModel.PRESSURE;
			chart = pressureChart;
			plotChoice = pressurePlot;
		}
		//day plot
		else if(dataType == LT_DATA){
			tableModelType = ReadoutTableModel.LT;
			chart = ltChart;
			plotChoice = ltPlot;
		}
		//year plot
		else if(dataType == LS_DATA){
			tableModelType = ReadoutTableModel.LS;
			chart = lsChart;
			plotChoice = lsPlot;
		}
		
		//if the chart has data, get the data points off the chart
		if(chart!=null){
			ArrayList<String> mcdNames = new ArrayList<String>();
			XYDataset dataset = chart.getXYPlot().getDataset();
			for(int i=0; i<dataset.getSeriesCount(); i++){
				mcdNames.add((String)chart.getXYPlot().getDataset().getSeriesKey(i));
			}
			//find the matching mcd points and add them to the data list
			for(MCDDataPoint dp : myLayer.getMCDDataPoints()){
				if(mcdNames.contains(dp.getName())){
					data.add(dp);
				}
			}
		}

		ReadoutTableModel tm = new ReadoutTableModel(this, tableModelType, data, chart, plotChoice);
		JTable table = new ReadoutTable(tm);

		return table;
	}
	
	private ListSelectionListener inputTableListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			int row = inputDataTbl.getSelectedRow();
			if(row == -1){
				selectedDataPoint = null;
			}else{
				selectedDataPoint = ((DataTableModel)inputDataTbl.getModel()).getDataPoint(row);
			}
			
			refreshInputs();
			refreshDisplayOptions();
			
			myLView.repaint();
			
			//update 3d too
			myLayer.increaseStateId(MCDLayer.IMAGES_BUFFER);
			myLayer.increaseStateId(MCDLayer.LABELS_BUFFER);
			if(ThreeDManager.isReady()){
				ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
			}
		}
	};
	
	private ListSelectionListener resultsTableListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			//check to see if the run button needs to be enabled
			runMcdBtn.setEnabled(false);
			int[] rows = resultsDataTbl.getSelectedRows();
			for(int row : rows){
				//if any of the datapoints has null chart data, enable the button
				MCDDataPoint dp = ((DataTableModel)resultsDataTbl.getModel()).getDataPoint(row);
				if(dp.elevationData == null) { // || dp.lsData == null || dp.timeData == null || dp.pressureData == null){
					runMcdBtn.setEnabled(true);
					break;
				}
			}
			
			refreshCharts();
		}
	};
	
	
	private MouseAdapter inputTableMouseListener = new MouseAdapter() {
		public void mouseClicked(MouseEvent e){
			//show the menu on right-click
			if(SwingUtilities.isRightMouseButton(e)){
				//the menu
				JPopupMenu menu = new JPopupMenu();
				//the menu items
				JMenuItem centerOnSel = new JMenuItem(centerOnDataPointAct);
				JMenuItem deleteSel = new JMenuItem(deleteSelectedAct);
				JMenuItem editSel = new JMenuItem(editSelectedAct);
				//add items to menu
				menu.add(centerOnSel);
				menu.add(deleteSel);
				menu.add(editSel);
				//disable menu options if no datapoint is selected
				if(selectedDataPoint == null){
					centerOnSel.setEnabled(false);
					deleteSel.setEnabled(false);
					editSel.setEnabled(false);
				}
				//show menu item
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
			//if double click, center on point
			if(e.getClickCount() == 2){
				centerOnDataPoint();
			}
		}
	};
	
	private MouseAdapter resultsTableMouseListener = new MouseAdapter() {
		public void mouseClicked(MouseEvent e){
			//show the menu on right-click
			if(SwingUtilities.isRightMouseButton(e)){
				// the menu
				JPopupMenu menu = new JPopupMenu();
				
				//menu item
				JMenuItem exportItm = new JMenuItem(new ExportTableAction(menu, "Export MCD Data Table", ",", resultsDataTbl));
				
				//populate menu
				menu.add(exportItm);
				
				//show menu item
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	};
	
	private AbstractAction centerOnDataPointAct = new AbstractAction("Center on selected MCD point") {
		public void actionPerformed(ActionEvent e) {
			if(selectedDataPoint!=null){
				centerOnDataPoint();
			}
		}
	};
	private void centerOnDataPoint(){
		if(selectedDataPoint!=null){
			//Get the spatial center from the selected site.
			//Convert to world point to pass to the location manager
			Point2D worldCenter = Main.PO.convSpatialToWorld(selectedDataPoint.getPoint());
			//recenter the mainview
			Main.testDriver.mainWindow.getLocationManager().setLocation(worldCenter, true);
		}
	}
	
	private AbstractAction deleteSelectedAct = new AbstractAction("Delete selected MCD point") {
		public void actionPerformed(ActionEvent e) {
			if(selectedDataPoint != null){
				//delete data point from layer
				myLayer.removeDataPoint(selectedDataPoint);
				//set selected to null
				selectedDataPoint = null;
				//update focus panel
				refreshDataTables();
				refreshDisplayOptions();
				refreshInputs();
				//refresh lview
				myLView.repaint();
				//update 3d too
				myLayer.increaseStateId(MCDLayer.IMAGES_BUFFER);
				myLayer.increaseStateId(MCDLayer.LABELS_BUFFER);
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
				}
			}
		}
	};
	
	private AbstractAction editSelectedAct = new AbstractAction("Edit MCD point...") {
		public void actionPerformed(ActionEvent e) {
			if(selectedDataPoint!=null){
				//show the edit dialog
				new AddDataPointDialog(MCDFocusPanel.this.getFrame(), myLView, "", null, null, selectedDataPoint, true);
			}
		}
	};
	
	/**
	 * Refresh the data table which shows all the mcd data points
	 */
	public void refreshDataTables(){
		//refresh input
		refreshInputTable();
		
		//refresh results
		refreshResultTable();
	}
	
	private void refreshInputTable(){
		inputTablePnl.remove(inputTableSP);
		inputDataTbl = createDataTable(true);
		inputTableSP = new JScrollPane(inputDataTbl);
		inputTablePnl.add(inputTableSP);
		inputTablePnl.validate();
		
		int row = inputDataTbl.getSelectedRow();
		if(row == -1){
			selectedDataPoint = null;
		}else{
			selectedDataPoint = ((DataTableModel)inputDataTbl.getModel()).getDataPoint(row);
		}
		refreshDisplayOptions();
		refreshInputs();
	}
	
	private void refreshResultTable(){
		resultsTablePnl.remove(resultsTableSP);
		resultsDataTbl = createDataTable(false);
		resultsTableSP = new JScrollPane(resultsDataTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		resultsTablePnl.add(resultsTableSP);
		resultsTablePnl.validate();
	}
	
	
	private void refreshDisplayOptions(){
		//disable all display options if no data point is selected
		if(selectedDataPoint == null){
			pointChk.setEnabled(false);
			outlineLbl.setEnabled(false);
			outlineCc.setEnabled(false);
			fillLbl.setEnabled(false);
			fillCc.setEnabled(false);
			labelChk.setEnabled(false);
			sizeLbl.setEnabled(false);
			sizeBx.setEnabled(false);
			fontLbl.setEnabled(false);
			labelCc.setEnabled(false);
		}else{
			//enable the checkboxes
			pointChk.setEnabled(true);
			labelChk.setEnabled(true);
			//set the field values based off the selected data point
			pointChk.setSelected(selectedDataPoint.showPoint());
			outlineCc.setColor(selectedDataPoint.getOutlineColor());
			fillCc.setColor(selectedDataPoint.getFillColor());
			labelChk.setSelected(selectedDataPoint.showLabel());
			sizeBx.setSelectedItem(selectedDataPoint.getFontSize());
			labelCc.setColor(selectedDataPoint.getLabelColor());
			//set whether they're enabled based off the check boxes
			//point
			outlineLbl.setEnabled(pointChk.isSelected());
			outlineCc.setEnabled(pointChk.isSelected());
			fillLbl.setEnabled(pointChk.isSelected());
			fillCc.setEnabled(pointChk.isSelected());
			//label
			sizeLbl.setEnabled(labelChk.isSelected());
			sizeBx.setEnabled(labelChk.isSelected());
			fontLbl.setEnabled(labelChk.isSelected());
			labelCc.setEnabled(labelChk.isSelected());
		}
	}
	
	private void refreshCharts(){
		//clear all charts to start with
		heightCPnl.removeAll();
		pressureCPnl.removeAll();
		ltCPnl.removeAll();
		lsCPnl.removeAll();
		heightChart = null;
		pressureChart = null;
		ltChart = null;
		lsChart = null;
		
		XYSeriesCollection heightData = new XYSeriesCollection();
		XYSeriesCollection pressureData = new XYSeriesCollection();
		XYSeriesCollection ltData = new XYSeriesCollection();
		XYSeriesCollection lsData = new XYSeriesCollection();
		
		for(int row : resultsDataTbl.getSelectedRows()){
			MCDDataPoint dp = ((DataTableModel)resultsDataTbl.getModel()).getDataPoint(row);
			if(dp.elevationData!=null){
				MCDData elevationData = dp.elevationData;
				
				ArrayList<Double> heightX = elevationData.columnVals.get(0);
				ArrayList<Double> heightY =elevationData.columnVals.get(heightPlot.getSelectedIndex());
				XYSeries data = new XYSeries(dp.getName());
		        for (int i=0; i<heightX.size(); i++) {
		            data.add(heightX.get(i), heightY.get(i));
		        }

				heightData.addSeries(data);
			}
			
			if(dp.timeData!=null){
				MCDData timeData = dp.timeData;
				
				ArrayList<Double> dataX = timeData.columnVals.get(0);
				ArrayList<Double> dataY =timeData.columnVals.get(ltPlot.getSelectedIndex());
				XYSeries data = new XYSeries(dp.getName());
		        for (int i=0; i<dataX.size(); i++) {
		            data.add(dataX.get(i), dataY.get(i));
		        }

		        ltData.addSeries(data);
			}

			if(dp.lsData!=null){
				MCDData seasonData = dp.lsData;
				
				ArrayList<Double> dataX = seasonData.columnVals.get(0);
				ArrayList<Double> dataY =seasonData.columnVals.get(lsPlot.getSelectedIndex());
				XYSeries data = new XYSeries(dp.getName());
		        for (int i=0; i<dataX.size(); i++) {
		            data.add(dataX.get(i), dataY.get(i));
		        }

		        lsData.addSeries(data);
			}
			
			// We reuse the elevationData object for the Pressure plot
			if(dp.elevationData!=null){
				MCDData elevationData = dp.elevationData;
				
				ArrayList<Double> dataX = elevationData.columnVals.get(2);
				ArrayList<Double> dataY = elevationData.columnVals.get(pressurePlot.getSelectedIndex());
				XYSeries data = new XYSeries(dp.getName());
		        for (int i=0; i<dataX.size(); i++) {
		            data.add(dataX.get(i), dataY.get(i));
		        }

		        pressureData.addSeries(data);
			}
			
		}
		
		if(heightData.getSeriesCount()>0) { // && lsData.getSeriesCount()>0 && ltData.getSeriesCount()>0){
			heightChart = createChart(heightData, HEIGHT_DATA);
			if (heightChart==null) return;
			ThemeChart.configureUI(heightChart);			  
		    XYItemRenderer rr = ((XYPlot) heightChart.getPlot()).getRenderer();
		    rr.setSeriesPaint(0, ThemeChart.getPlotColor());

		    pressureChart = createChart(pressureData, PRESSURE_DATA);
			ThemeChart.configureUI(pressureChart);			  
		    rr = ((XYPlot) pressureChart.getPlot()).getRenderer();
		    rr.setSeriesPaint(0, ThemeChart.getPlotColor());
		    
			ltChart = createChart(ltData, LT_DATA);
			ThemeChart.configureUI(ltChart);			  
		    rr = ((XYPlot) ltChart.getPlot()).getRenderer();
		    rr.setSeriesPaint(0, ThemeChart.getPlotColor());
			
			lsChart = createChart(lsData, LS_DATA);
			ThemeChart.configureUI(lsChart);			  
		    rr = ((XYPlot) lsChart.getPlot()).getRenderer();
		    rr.setSeriesPaint(0, ThemeChart.getPlotColor());		
		}else{
			heightCPnl.add(noDataLbl1);
			pressureCPnl.add(noDataLbl4);
			ltCPnl.add(noDataLbl2);
			lsCPnl.add(noDataLbl3);
			// refresh the readout tables
			refreshReadoutTables();
		}
		
		heightCPnl.setChart(heightChart);
		pressureCPnl.setChart(pressureChart);
		ltCPnl.setChart(ltChart);
		lsCPnl.setChart(lsChart);
	}

	private void refreshReadoutTables() {
		// height readout
		heightROPnl.removeAll();
		heightROTbl = createReadoutTable(HEIGHT_DATA);
		heightROSP = new JScrollPane(heightROTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		heightROPnl.add(heightROSP);
		heightROPnl.revalidate();
		// pressure readout
		pressureROPnl.removeAll();
		pressureROTbl = createReadoutTable(PRESSURE_DATA);
		pressureROSP = new JScrollPane(pressureROTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pressureROPnl.add(pressureROSP);
		pressureROPnl.revalidate();
		// day readout
		ltROPnl.removeAll();
		ltROTbl = createReadoutTable(LT_DATA);
		ltROSP = new JScrollPane(ltROTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		ltROPnl.add(ltROSP);
		ltROPnl.revalidate();
		// year readout
		lsROPnl.removeAll();
		lsROTbl = createReadoutTable(LS_DATA);
		lsROSP = new JScrollPane(lsROTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,	ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		lsROPnl.add(lsROSP);
		lsROPnl.revalidate();
	}

	private JFreeChart createChart(XYSeriesCollection dataset, int dataType) {

		Range range = null;
		String title = "";
		String xAxis = "";
		String yAxis = "Temperature (K)";
		NumberTickUnit step = null;
		LogarithmicAxis logAxis = null;
		PlotOrientation orientation = PlotOrientation.VERTICAL;
		
		// TODO: Should this be here, or should something pass in the Datapoint?
		int[] rows = resultsDataTbl.getSelectedRows();

		MCDDataPoint dp;
		if (rows.length>=1) {
			dp = ((DataTableModel)resultsDataTbl.getModel()).getDataPoint(rows[0]);
		} else {
			// TODO: Boom?
			return null;
		}
		
		//use day values
		if(dataType == HEIGHT_DATA){
			yAxis = dp.elevationData.columnNames.get(heightPlot.getSelectedIndex());
			
			title = yAxis + " at Height Above Surface";
			range = heightRange;
			xAxis = "Height above Surface (m)";
			if(flipXyHeightData){
				orientation = PlotOrientation.HORIZONTAL;
			}
			if(useLogscaleHeightData){
		    	logAxis = new LogarithmicAxis("Height above Surface (m)");
		    	logAxis.setRange(heightRange);
			}
		}
		if(dataType == PRESSURE_DATA){
			// Reuse elevation data for pressure
			yAxis = dp.elevationData.columnNames.get(pressurePlot.getSelectedIndex());
			
			title = yAxis + " over a Day";
			range = pressureRange;
//			step = pressureStep;
			xAxis = "Pressure";
		}
		if(dataType == LT_DATA){
			yAxis = dp.timeData.columnNames.get(ltPlot.getSelectedIndex());
			
			title = yAxis + " over a Day";
			range = ltRange;
			step = ltStep;
			xAxis = "Hour of Day";
		}
		//otherwise use year values
		else if (dataType == LS_DATA){
			yAxis = dp.lsData.columnNames.get(lsPlot.getSelectedIndex());
			
			title = yAxis + " over a Year";
			range = lsRange;
			step = lsStep;
			xAxis = "Ls";
		}
		
		JFreeChart chart = ChartFactory.createXYLineChart(
			title,				//title
			xAxis,		 		//x axis label
			yAxis,		 		//y axis label
			dataset,			//data
			orientation,		//orientation
			false,		 		//legend
			false, 				//tooltips
			false);				//urls
      
		
	    XYPlot plot = (XYPlot) chart.getPlot();
		
	    //format x-axis
	    NumberAxis xaxis = (NumberAxis) plot.getDomainAxis();
	    
	    ThemeChart.applyThemeToAxis(xaxis);	
	    
	    xaxis.setNumberFormatOverride(xFormat);
	    if(range != null){
	    	xaxis.setRange(range);
	    }
	    if(step != null){
	    	xaxis.setTickUnit(step);
	    }
	      
	    //format y-axis
	    NumberAxis yaxis = (NumberAxis) plot.getRangeAxis();
	    
	    ThemeChart.applyThemeToAxis(yaxis);	
	    
	    yaxis.setAutoRangeIncludesZero(false);
	    
	    //apply the log axis if necessary
	    if(useLogscaleHeightData && logAxis != null){
	    	//change the range axis (independent variable -- height)
	    	plot.setDomainAxis(logAxis);
	    }
	    
	    //format title size
	    chart.getTitle().setFont(ThemeFont.getBold().deriveFont(18f));
	    chart.getTitle().setPadding(16, 0, 16, 0);
	      
	    //add a progress listener, so whenever it's redrawn, it will trigger
	    // a refresh of the readout table
	    chart.addProgressListener(new ChartProgressListener() {
	    	public void chartProgress(ChartProgressEvent e) {
	    		if(e.getType() == ChartProgressEvent.DRAWING_FINISHED){
	    			refreshReadoutTables();
	    		}
	    	}
	    });
	      
	    return chart;
	}
	
	private ChartMouseListener heightChartListener = new ChartMouseListener() {
		public void chartMouseMoved(ChartMouseEvent e) {
			chartMouseMovedEventOccurred(heightCH, heightCPnl, e.getChart(), e);
		}
		public void chartMouseClicked(ChartMouseEvent arg0) {
		}
	};

	private ChartMouseListener pressureChartListener = new ChartMouseListener() {
		public void chartMouseMoved(ChartMouseEvent e) {
			chartMouseMovedEventOccurred(pressureCH, pressureCPnl, e.getChart(), e);
		}
		public void chartMouseClicked(ChartMouseEvent arg0) {
		}
	};

	private ChartMouseListener ltChartListener = new ChartMouseListener() {
		public void chartMouseMoved(ChartMouseEvent e) {
			chartMouseMovedEventOccurred(ltCH, ltCPnl, e.getChart(), e);
		}

		public void chartMouseClicked(ChartMouseEvent arg0) {
		}
	};
	
	private ChartMouseListener lsChartListener = new ChartMouseListener() {
		public void chartMouseMoved(ChartMouseEvent e) {
			chartMouseMovedEventOccurred(lsCH, lsCPnl, e.getChart(), e);
		}

		public void chartMouseClicked(ChartMouseEvent arg0) {
		}
	};
	
	private void chartMouseMovedEventOccurred(Crosshair ch, ChartPanel panel, JFreeChart chart, ChartMouseEvent e){
		XYPlot plot = chart.getXYPlot();
		ValueAxis xAxis = plot.getDomainAxis();
		
		Rectangle2D dataArea = panel.getScreenDataArea();
		double x = xAxis.java2DToValue(e.getTrigger().getX(), dataArea, plot.getDomainAxisEdge());
		
		//if the axis has been flipped use the mouse y coordinate
		// to get the proper x value
		if(flipXyHeightData){
			x = xAxis.java2DToValue(e.getTrigger().getY(), dataArea, plot.getDomainAxisEdge());
		}
		Range range = xAxis.getRange();
		
		//only update plot and table if the x is within the domain range
		if(range.contains(x)){
			double xItemValue = -1;
			double xDiff = Double.MAX_VALUE;
			//cycle through x values and find the one nearest to the mouse
			for(int i=0; i<plot.getDataset().getItemCount(0); i++){
				double xValue = plot.getDataset().getXValue(0, i);
				double diff = Math.abs(x-xValue);
				//approaching the nearest point
				if(diff < xDiff){
					xDiff = diff;
					xItemValue = xValue;
				}
				//leaving the nearest point, so stop searching
				else{
					break;
				}
			}
			
			ch.setValue(xItemValue);
			getReadoutTableModelForChart(chart).setXValue(xItemValue);
		}
		
	}
	
	private ReadoutTableModel getReadoutTableModelForChart(JFreeChart chart){
		if(chart == heightChart){
			return (ReadoutTableModel)heightROTbl.getModel();
		}
		else if(chart == pressureChart){
			return (ReadoutTableModel)pressureROTbl.getModel();
		}
		else if(chart == lsChart){
			return (ReadoutTableModel)lsROTbl.getModel();
		}
		else if(chart == ltChart){
			return (ReadoutTableModel)ltROTbl.getModel();
		}
		return null;
	}
	
	
	private void refreshInputs(){
		//clear input text
		lsTf.setText("");
		hourTf.setText("");
		heightTf.setText("");
		//if no data point is selected, clear all inputs and disable
		if(selectedDataPoint == null){
			//disable labels
			lsLbl.setEnabled(false);
			hourLbl.setEnabled(false);
			heightLbl.setEnabled(false);
			scenarioLbl.setEnabled(false);
			elevationsLbl.setEnabled(false);
			lsHelpLbl.setEnabled(false);
			ltHelpLbl.setEnabled(false);
			htHelpLbl.setEnabled(false);
			scenHelpLbl.setEnabled(false);
			elevationsHelpLbl.setEnabled(false);
			//disable input fields
			lsTf.setEnabled(false);
			hourTf.setEnabled(false);
			heightTf.setEnabled(false);
			scenarioBx.setEnabled(false);
			elevationsTf.setEnabled(false);
			//disable input buttons
			setInputBtn.setEnabled(false);
			createFromInputBtn.setEnabled(false);
		}
		//enable and populate the inputs based off the selected data point
		else{
			//enable labels
			lsLbl.setEnabled(true);
			hourLbl.setEnabled(true);
			heightLbl.setEnabled(true);
			scenarioLbl.setEnabled(true);
			elevationsLbl.setEnabled(true);
			lsHelpLbl.setEnabled(true);
			ltHelpLbl.setEnabled(true);
			htHelpLbl.setEnabled(true);
			scenHelpLbl.setEnabled(true);
			elevationsHelpLbl.setEnabled(true);
			//enable input fields
			lsTf.setEnabled(true);
			hourTf.setEnabled(true);
			heightTf.setEnabled(true);
			scenarioBx.setEnabled(true);
			elevationsTf.setEnabled(true);
			//populate input fields
			//keep track of any failures if map sampling doesn't have data
			statFailures.clear();
			lsTf.setText(getTextForInput(selectedDataPoint.getLSubS(), "L sub S"));
			hourTf.setText(getTextForInput(selectedDataPoint.getHour(), "Hour"));
			heightTf.setText(getTextForInput(selectedDataPoint.getHeight(), "Height"));
			scenarioBx.setSelectedItem(selectedDataPoint.getScenario());
			elevationsTf.setText(selectedDataPoint.getElevations());
			//if there were any failures tell the user
			if(statFailures.size()>0){
				String errorText = "The following stat(s) failed to find defaults:\n\n";
				for(String stat : statFailures){
					errorText+=stat+"\n";
				}
				errorText+="\nPlease enter the necessary input value(s).";
				
				Util.showMessageDialog(errorText, "Default(s) not found", JOptionPane.ERROR_MESSAGE);
			}
			
			//enable input buttons
			setInputBtn.setEnabled(true);
			createFromInputBtn.setEnabled(true);
		}
	}
	
	/**
	 * Converts the double value of an input to a String.  Also, checks
	 * for a Double.MAX_VALUE which can be returned for inputs that are
	 * map sampled, and returns an empty string.  Also adds the stat to the
	 * failure list to be displayed to the user.
	 * @param inputValue The value to convert to string
	 * @param stat The name of the stat that value is for
	 * @return A String object representing the double value, or an empty
	 * string if the value is Double.MIN_VALUE (Because that means the 
	 * map sampling failed).
	 */
	private String getTextForInput(double inputValue, String stat){
		String result = "";
		
		//if the map sampling failed, then the value will be Double.MIN_VALUE
		if(inputValue == Double.MAX_VALUE){
			result = "";
			statFailures.add(stat);
			
		}else{
			result = inputValue+"";
		}
		
		return result;
	}
	
	private void setInputsOnDataPoint(MCDDataPoint dp){
		dp.setLSubS(Double.parseDouble(lsTf.getText()));
		dp.setHour(Double.parseDouble(hourTf.getText()));
		dp.setHeight(Double.parseDouble(heightTf.getText()));
		dp.setScenario((Integer)scenarioBx.getSelectedItem());
		dp.setElevations(elevationsTf.getText());
	}
	
	/**
	 * @return The selected MCD data point shown in the
	 * data table
	 */
	public MCDDataPoint getSelectedDataPoint(){
		return selectedDataPoint;
	}
}
