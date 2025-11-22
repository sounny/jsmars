package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.block.LineBorder;
import org.jfree.chart.labels.XYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.spectra.SpectraMathUtil;
import edu.asu.jmars.layer.stamp.spectra.SpectraObject;
import edu.asu.jmars.layer.stamp.spectra.SpectraTable;
import edu.asu.jmars.layer.stamp.spectra.SpectraTableModel;
import edu.asu.jmars.swing.TableColumnAdjuster;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeChart;
import edu.asu.jmars.util.Util;

@SuppressWarnings("serial")
public class SpectraView extends JPanel {
	private static final long serialVersionUID = 1L;

	// Attached stampLView
	private StampLView stampLView;
	private StampLayerSettings settings;
	
	// JFreeChart related stuff
	private JFreeChart chart;
	private ChartPanel chartPanel;
	
	private JComboBox<String> columnBx;
	private JMenuItem controlsItm;
		
	//Spectra Table
	private SpectraTable spectraTbl;
	private JScrollPane tableSp;
	private JPanel tablePnl;
	private ArrayList<StampShape> markedStamps = new ArrayList<StampShape>();
	private ArrayList<SpectraObject> computedSpectra = new ArrayList<SpectraObject>();
	
	private JFrame myFrame;
	private JFrame controlFrame;
	private JCheckBox dataPtChk;
	private JCheckBox xFlipChk;
	private JCheckBox yFlipChk;
	private JCheckBox legendChk;
	private JTextField rangeMinTF;
	private JTextField rangeMaxTF;
	private JTextField domainMinTF;
	private JTextField domainMaxTF;
	private JButton resetRangeBtn;
	
	//defaults for options
	private boolean showLegend = true;
	private boolean showDataPts = false;
	private Boolean flipXAxis = null;
	private boolean flipYAxis = false;
	private Double minDomain;
	private Double maxDomain;
	private Double minRange;
	private Double maxRange;
	private Double minDomainDefault;
	private Double maxDomainDefault;
	private Double minRangeDefault;
	private Double maxRangeDefault;
	
	private HashMap<StampShape, Color> stamp2Color = new HashMap<StampShape, Color>();
	
	private DecimalFormat format = new DecimalFormat("#.###");
    private int row = 0;
    private int pad = 1;
    private Insets in = new Insets(pad,pad,pad,pad);
    
    /** When reselecting all rows from a stamp outlines selection,
     * use this variable to turn the table refresh off until all 
     * rows are reselected. */
    private boolean refreshData = true;
	
	public SpectraView(final StampLView stampLView){
		this.stampLView = stampLView;
		settings = stampLView.getSettings();
		
		//set defaults from settings if there are any 
		// (if this is being loaded from a session, perhaps)
		if(settings.showLegend!=null){
			showLegend = settings.showLegend;
		}
		if(settings.showDataPts!=null){
			showDataPts = settings.showDataPts;
		}
		if(settings.flipXAxis!=null){
			flipXAxis = settings.flipXAxis;
		}
		if(settings.flipYAxis!=null){
			flipYAxis = settings.flipYAxis;
		}
		if(settings.minDomain!=null){
			minDomain = settings.minDomain;
		}
		if(settings.maxDomain!=null){
			maxDomain = settings.maxDomain;
		}
		if(settings.minRange!=null){
			minRange = settings.minRange;
		}
		if(settings.maxRange!=null){
			maxRange = settings.maxRange;
		}
		
		buildUI();
	}
	
	private void buildUI(){
		//center panel is the chart
		chart = ChartFactory.createXYLineChart(
				null,			//Title
				"wave number",	//x axis
				"",				//y axis
				new XYSeriesCollection(),	//dataset
				PlotOrientation.VERTICAL,	//orientation
				showLegend,			//legend
				true,			//tooltips
				false);			//urls
		
		ThemeChart.configureUI(chart);
		XYItemRenderer rr = ((XYPlot) chart.getPlot()).getRenderer();
		rr.setSeriesPaint(0, ThemeChart.getPlotColor());
		
		chartPanel = new ChartPanel(chart, true);
		chartPanel.setBorder(new EmptyBorder(5,5,5,5));
		//add chart options to the right click menu
		controlsItm = new JMenuItem(showControlsAct);
		//enable once a selection has been made, this
		// avoids the chicken-egg problem with setting
		// the range for domain and range
		controlsItm.setEnabled(false);
		chartPanel.getPopupMenu().add(createCSVMenuItem());
		chartPanel.getPopupMenu().add(controlsItm);
		
		//column option
		JLabel columnLbl = new JLabel("Plot type: ");
		columnBx = new JComboBox<String>(stampLView.stampLayer.getSpectraColumns());
		columnBx.addActionListener(columnListener);
		if(settings.plotType!=null){
			columnBx.setSelectedItem(settings.plotType);
		}
		JPanel columnPnl = new JPanel();		
		columnPnl.add(columnLbl);
		columnPnl.add(columnBx);
		
		//table panel
		tablePnl = new JPanel(new GridLayout(1,1));
		tablePnl.setBorder(new TitledBorder("Spectra"));
		tablePnl.setPreferredSize(new Dimension(0,150));
		if (stampLView.stampLayer.spectraPerPixel()) {
			spectraTbl = createCRISMSpectraTable();
		} else {
			spectraTbl = createSpectraTable();
		}
		tableSp = new JScrollPane(spectraTbl);
		tablePnl.add(tableSp);
		
		//add to this		
		setLayout(new GridBagLayout());
		setBorder(new EmptyBorder(5, 5, 5, 5));
		
		row = 0;
		add(columnPnl,  new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		add(chartPanel, new GridBagConstraints(0, ++row, 2, 1, 1, .8, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
		add(tablePnl, new GridBagConstraints(0, ++row, 2, 1, 1, 0.3, GridBagConstraints.CENTER, GridBagConstraints.BOTH, in, pad, pad));
	}
	
	
	private void refreshTable(){
		//get the current selections if any
		int[] selectedRows = spectraTbl.getSelectedRows();
		
		tablePnl.remove(tableSp);
		if (stampLView.stampLayer.spectraPerPixel()) {
			spectraTbl = createCRISMSpectraTable();
			
			int lastRow = spectraTbl.getRowCount()-1;
			if (lastRow>=0) {
				spectraTbl.addRowSelectionInterval(lastRow, lastRow);
			}
		} else {
			spectraTbl = createSpectraTable();
			
			//reset the row selections if possible
			for(int selectedRow : selectedRows){
				if(selectedRow < spectraTbl.getRowCount()){
					spectraTbl.addRowSelectionInterval(selectedRow, selectedRow);
				}
			}
		}
				
		tableSp = new JScrollPane(spectraTbl);
		tablePnl.add(tableSp);
		tablePnl.validate();
	}
	
	
	private MouseListener tableListener = new MouseListener() {
		
		public void mouseReleased(MouseEvent e) {}
		public void mousePressed(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		
		public void mouseClicked(MouseEvent e) {

			if(SwingUtilities.isRightMouseButton(e)){
				final JPopupMenu menu = new JPopupMenu();
				
				final int[] selections = spectraTbl.getSelectedRows();
				Arrays.sort(selections);
				
				//use the table model to get selected spectraObjects
				final SpectraTableModel model = (SpectraTableModel) spectraTbl.getModel();

				String recordStr = "spectrum";
				if(selections.length>1){
					recordStr= "spectra";
				}
				
				JMenuItem renameItem = new JMenuItem(new AbstractAction("Rename spectrum...") {
					public void actionPerformed(ActionEvent e) {
						
						int row = selections[0];
						SpectraObject so =  model.getSpectra(row);
						String message = "Enter a new name for the selected spectrum.\n\n"
										+"Current name: '"+so.getName()+"'\n"
										+"Description: "+so.getDesc()+"\n ";
						
						String name = Util.showInputDialog(message, null);
						
						if(name!=null){
							so.setName(name);
							model.fireTableRowsUpdated(row, row);
							refreshSpectraData(false);
						}
					}
				});
				
				JMenuItem delItem = new JMenuItem(new AbstractAction("Remove "+recordStr) {
					public void actionPerformed(ActionEvent e) {
						//remove them in reverse order so there can 
						// never be an index out of bounds exception
						
						for(int i=selections.length-1; i>=0; i--){
							//row must be in the marked stamps list
							int row = selections[i];
							
							SpectraObject spectra = model.getSpectra(row);
							String desc = model.getSpectra(row).getDesc();
							int rowToRemove = -1;
							
							//check if it's marked spectra
							if(spectra.isMarked()){
								String id = desc.substring(0, desc.length()-1);
								
								for(int j=0; j<markedStamps.size(); j++){
									if(markedStamps.get(j).getId().equals(id)){
										rowToRemove = j;
										break;
									}
								}
								if(rowToRemove>-1){
									markedStamps.remove(rowToRemove);
								}

								// CRISM Spectra per point spectra
								if (spectra.myStamp!=null) {
									spectra.myStamp.spectraPoints.remove(spectra);
								}
							}
							//if the desc contains a paren it's a computed stamp, because
							// all computed stamps contain a paren
							else if(spectra.isComputed()){
								for(int j=0; i<computedSpectra.size(); j++){
									if(computedSpectra.get(j) == spectra){
										rowToRemove = j;
										break;
									}
								}
								if(rowToRemove>-1){
									computedSpectra.remove(rowToRemove);
								}
							}
						}
						
			    		stampLView.stampLayer.increaseStateId(StampLayer.IMAGES_BUFFER);
			    		stampLView.redrawEverything(true);
			    		
						refreshSpectraData(true);
					}
				});
				
				JMenuItem avgItem = new JMenuItem(new AbstractAction("Avg spectra") {
					public void actionPerformed(ActionEvent e) {
						SpectraObject newSpectra = SpectraMathUtil.avgSpectra(model.getSpectra(selections));
						computedSpectra.add(newSpectra);
						refreshTable();
					}
				});
				
				JMenuItem sumItem = new JMenuItem(new AbstractAction("Sum spectra") {
					public void actionPerformed(ActionEvent e) {
						SpectraObject newSpectra = SpectraMathUtil.sumSpectra(model.getSpectra(selections));
						computedSpectra.add(newSpectra);
						refreshTable();
						
					}
				});
				
				JMenuItem multiplyItem = new JMenuItem(new AbstractAction("Multiply spectra") {
					public void actionPerformed(ActionEvent e) {
						SpectraObject newSpectra = SpectraMathUtil.multiplySpectra(model.getSpectra(selections));
						computedSpectra.add(newSpectra);
						refreshTable();
						
					}
				});
				
				JMenuItem subtractItem = new JMenuItem(new AbstractAction("Subtract spectra...") {
					public void actionPerformed(ActionEvent e) {
						//Popup a dialog asking which record is subtracted from which
						ArrayList<SpectraObject> selectedSpectra = model.getSpectra(selections);
						String[] choices = {selectedSpectra.get(0).getDesc(), selectedSpectra.get(1).getDesc()};
						String firstSpectra = (String) Util.showInputDialog(
								"Which spectra should be 'A' in the equation: A-B?",
								"Subtraction Order", JOptionPane.QUESTION_MESSAGE, null,
								choices, choices[0]);
						
						SpectraObject newSpectra;
						if(selectedSpectra.get(0).getDesc().equals(firstSpectra)){
							newSpectra = SpectraMathUtil.subtractSpectra(selectedSpectra.get(0), selectedSpectra.get(1));
						}else{
							newSpectra = SpectraMathUtil.subtractSpectra(selectedSpectra.get(1), selectedSpectra.get(0));
						}
						computedSpectra.add(newSpectra);
						refreshTable();
					}
				});
				
				JMenuItem divideItem = new JMenuItem(new AbstractAction("Divide spectra...") {
					public void actionPerformed(ActionEvent e) {
						//Popup a dialog asking which record is divided from which
						ArrayList<SpectraObject> selectedSpectra = model.getSpectra(selections);
						String[] choices = {selectedSpectra.get(0).getDesc(), selectedSpectra.get(1).getDesc()};
						String firstSpectra = (String) Util.showInputDialog(
								"Which spectra should be 'A' in the equation: A/B?",
								"Division Order", JOptionPane.QUESTION_MESSAGE, null,
								choices, choices[0]);
						
						SpectraObject newSpectra;
						if(selectedSpectra.get(0).getDesc().equals(firstSpectra)){
							newSpectra = SpectraMathUtil.divideSpectra(selectedSpectra.get(0), selectedSpectra.get(1));
						}else{
							newSpectra = SpectraMathUtil.divideSpectra(selectedSpectra.get(1), selectedSpectra.get(0));
						}
						computedSpectra.add(newSpectra);
						refreshTable();
						
					}
				});
				
				JMenuItem pointNormItem = new JMenuItem(new AbstractAction("Point normalize spectra...") {
					public void actionPerformed(ActionEvent e) {
						//Popup a dialog asking for the wavelength and value
						JPanel inputPnl = new JPanel(new GridBagLayout());
						JLabel messageLbl = new JLabel("Please enter inputs for Point Normalization");
						JLabel wlLbl = new JLabel("Wavelength:");
						JLabel valLbl = new JLabel("Value:");
						JTextField wlTf = new JTextField(10);
						JTextField valTf = new JTextField(10);
						
						int row = 0;
						inputPnl.add(messageLbl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(Box.createVerticalStrut(3), new GridBagConstraints(0, ++row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(wlLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(wlTf, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(valLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(valTf, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						
						int result = Util.showConfirmDialog(inputPnl,
								"Enter Point Normalization Values", JOptionPane.OK_CANCEL_OPTION);
						
						if(result == JOptionPane.OK_OPTION){
						
							double wavelength = Double.parseDouble(wlTf.getText());
							double value = Double.parseDouble(valTf.getText());
							ArrayList<SpectraObject> selectedSpectra = model.getSpectra(selections);
							ArrayList<SpectraObject> newSpectra = SpectraMathUtil.pointNormalizeSpectra(selectedSpectra, wavelength, value);
							computedSpectra.addAll(newSpectra);
							refreshTable();
						}
					}
				});
				
				JMenuItem rangeNormItem = new JMenuItem(new AbstractAction("Range normalize spectra...") {
					public void actionPerformed(ActionEvent e) {
						//Popup a dialog asking for the x and y ranges
						JPanel inputPnl = new JPanel(new GridBagLayout());
						JLabel messageLbl = new JLabel("Please enter inputs for Range Normalization");
						JLabel xMinLbl = new JLabel("Min Wavelength:");
						JLabel xMaxLbl = new JLabel("Max Wavelength:");
						JLabel yMinLbl = new JLabel("Min Value:");
						JLabel yMaxLbl = new JLabel("Max Value:");
						JTextField xMinTf = new JTextField(10);
						JTextField xMaxTf = new JTextField(10);
						JTextField yMinTf = new JTextField(10);
						JTextField yMaxTf = new JTextField(10);
						int row = 0;
						inputPnl.add(messageLbl, new GridBagConstraints(0, row, 4, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(Box.createVerticalStrut(3), new GridBagConstraints(0, ++row, 4, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(xMinLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(xMinTf, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(xMaxLbl, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(xMaxTf, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(yMinLbl, new GridBagConstraints(0, ++row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(yMinTf, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(yMaxLbl, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
						inputPnl.add(yMaxTf, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
						
						int result = Util.showConfirmDialog(inputPnl,
								"Enter Range Normalization Values", JOptionPane.OK_CANCEL_OPTION);
						
						if(result == JOptionPane.OK_OPTION){
						
							try{
								double xMin = Double.parseDouble(xMinTf.getText());
								double xMax = Double.parseDouble(xMaxTf.getText());
								double yMin = Double.parseDouble(yMinTf.getText());
								double yMax = Double.parseDouble(yMaxTf.getText());
								ArrayList<SpectraObject> selectedSpectra = model.getSpectra(selections);
								ArrayList<SpectraObject> newSpectra = SpectraMathUtil.rangeNormalizeSpectra(selectedSpectra, xMin, xMax, yMin, yMax);
								computedSpectra.addAll(newSpectra);
								refreshTable();
							}catch(NumberFormatException ex){
								//if a bad entry is entered tell the user, and don't try to
								// to create new spectra object records
								Util.showMessageDialog("One or more entries was an invalid number.\n"
										+ "Please try again with valid numbers entries.",
										"Invalid Input", JOptionPane.ERROR_MESSAGE);
							}
						}
					}
				});
				
				//add tooltips to help the user
				renameItem.setToolTipText("Rename the spectrum");
				delItem.setToolTipText("Removes the spectrum from this table");
				avgItem.setToolTipText("Creates a new spectrum that is the average of all selected spectra");
				sumItem.setToolTipText("Creates a new spectrum that is the sum of all selected spectra");
				multiplyItem.setToolTipText("Creates a spectrum that is the product of all selected spectra");
				subtractItem.setToolTipText("Creates a spectrum that is the difference of one spectrum minus the other (order based on user selection)");
				divideItem.setToolTipText("Creates a spectrum that is the quotient of one spectrum divided by the other (based on user selection) where values divided by 0 will be represented as 0");
				pointNormItem.setToolTipText("Creates normalized spectra for all selected spectra -- normalizes by specifying a point (wavlength, value) and scales the rest of the spectra from that point");
				rangeNormItem.setToolTipText("Creates normalized spectra for all selected spectra -- normalizes by specifying an x and y range and shifts all values to fit the equation specified by those ranges");
				
				menu.add(renameItem);
				menu.add(delItem);
				menu.add(avgItem);
				menu.add(sumItem);
				menu.add(multiplyItem);
				menu.add(subtractItem);
				menu.add(divideItem);
				menu.add(pointNormItem);
				menu.add(rangeNormItem);
				
				//disable some items if selection lengths aren't met
				//rename only works if one row is selected
				if(selections.length!=1){
					renameItem.setEnabled(false);
				}
				//delete, normalize need at least one selection
				if(selections.length<1){
					delItem.setEnabled(false);
					rangeNormItem.setEnabled(false);
					pointNormItem.setEnabled(false);
				}
				//avg, sum, multiply need at least 2 selections
				if(selections.length<2){
					avgItem.setEnabled(false);
					sumItem.setEnabled(false);
					multiplyItem.setEnabled(false);
				}
				//subtract and divide need exactly 2 selections
				if(selections.length!=2){
					subtractItem.setEnabled(false);
					divideItem.setEnabled(false);
				}
				
				//if any selection is from the selected stamps, it cannot be deleted
				ArrayList<SpectraObject> selObjs = model.getSpectra(selections);
				for(SpectraObject so : selObjs){
					if(!so.isMarked() && !so.isComputed()){
						delItem.setEnabled(false);
						delItem.setToolTipText("Cannot remove spectra that are also selected in the layer");
						break;
					}
				}
				
				
				menu.show(e.getComponent(), e.getX(), e.getY());
			}
			
		}
	};
	
	private ListSelectionListener rowListener = new ListSelectionListener() {
		public void valueChanged(ListSelectionEvent e) {
			if(refreshData){
				refreshSpectraData(false);
			}
		}
	};
	
	
	private SpectraTable createSpectraTable(){
		//spectra table  has "marked" spectra, selections from the outline tab, and computed spectra
		ArrayList<SpectraObject> objs = new ArrayList<SpectraObject>();
		//add "marked" spectra
		objs.addAll(getSpectraFromStamps(markedStamps));
		//add selections from the outline tab
		ArrayList<StampShape> selStamps = new ArrayList<StampShape>();
		for(StampShape ss : stampLView.stampLayer.getSelectedStamps()){
			//don't add it if it's a marked stamp (no need for duplicates)
			if(!markedStamps.contains(ss)){
				selStamps.add(ss);
			}
		}
		objs.addAll(getSpectraFromStamps(selStamps));
		//add "computed" spectra
		for(SpectraObject so : computedSpectra){
			if(so.getType().equals((String)columnBx.getSelectedItem())){
				objs.add(so);
			}
		}
		
		SpectraTableModel tm = new SpectraTableModel(objs);
		SpectraTable table = new SpectraTable(tm);
		
		//add listeners
		table.addMouseListener(tableListener);
		table.getSelectionModel().addListSelectionListener(rowListener);
		
		TableColumnAdjuster tca = new TableColumnAdjuster(table);
		tca.adjustColumns();
		
		return table;
	}

	private SpectraTable createCRISMSpectraTable(){
		//spectra table  has "marked" spectra, selections from the outline tab, and computed spectra
		ArrayList<SpectraObject> objs = new ArrayList<SpectraObject>();
				
		if (stampLView.stamps!=null) {
			for (StampShape stamp : stampLView.stamps) {
				if (stamp.spectraPoints!=null && stamp.spectraPoints.size()>0) {
					objs.addAll(stamp.spectraPoints);
				}
			}
		}
		
		//add "computed" spectra
		for(SpectraObject so : computedSpectra){
			if(so.getType().equals((String)columnBx.getSelectedItem())){
				objs.add(so);
			}
		}
		
		SpectraTableModel tm = new SpectraTableModel(objs, true);
		SpectraTable table = new SpectraTable(tm);
		
		//add listeners
		table.addMouseListener(tableListener);
		table.getSelectionModel().addListSelectionListener(rowListener);
		
		TableColumnAdjuster tca = new TableColumnAdjuster(table);
		tca.adjustColumns();
		
		return table;
	}

	private ActionListener columnListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			refreshSpectraData(true);
			
			//TODO: resetting ranges might need to be changed once there
			// are actual default ranges that come from the stamp server
			//as long as the plot isn't null, reset it's ranges
			if(chart.getXYPlot().getDataRange(chart.getXYPlot().getDomainAxis()) != null &&
				chart.getXYPlot().getDataRange(chart.getXYPlot().getRangeAxis()) != null){
				
				//reset values to defaults no matter what, because the ranges
				// can be drastically different between datasets.
				setRangeValuesToDefaults();
				updateChartRange();
			}
		}
	};
	
	private Action showControlsAct = new AbstractAction("Chart Options") {
		public void actionPerformed(ActionEvent e) {
			//create the UI for the controls panel if it hasn't been done before
			if(controlFrame == null){
				controlFrame = new JFrame("Spectra Chart Options");
				JPanel mainPnl = new JPanel(new BorderLayout());				
				mainPnl.setBorder(new EmptyBorder(5, 5, 5, 5));
				
				JPanel controlPnl = new JPanel();
				controlPnl.setLayout(new BoxLayout(controlPnl, BoxLayout.PAGE_AXIS));
				controlPnl.setBorder(new TitledBorder("Chart Options"));
				
				//x-axis flip
				xFlipChk = new JCheckBox(xFlipAct);
				if(flipXAxis == null){
					flipXAxis = true;
				}
				xFlipChk.setSelected(flipXAxis);
				//y-axis flip
				yFlipChk = new JCheckBox(yFlipAct);
				yFlipChk.setSelected(flipYAxis);
				//show datapoints
				dataPtChk = new JCheckBox(showDataPtAct);
				dataPtChk.setSelected(showDataPts);
				//show legend
				legendChk = new JCheckBox(legendAct);
				legendChk.setSelected(showLegend);
				//domain (x-axis) min/max
				JLabel domainMinLbl = new JLabel("X Min: ");
				JLabel domainMaxLbl = new JLabel("X Max: ");
				JPanel axesPnl = new JPanel(new GridBagLayout());
				axesPnl.setBorder(new TitledBorder("Axes Ranges"));
				domainMinTF = new JTextField(6);
				domainMinTF.setText(format.format(minDomain));
				domainMinTF.addActionListener(rangeValueListener);
				domainMinTF.addFocusListener(rangeFocusListener);
				domainMaxTF = new JTextField(6);
				domainMaxTF.setText(format.format(maxDomain));
				domainMaxTF.addActionListener(rangeValueListener);
				domainMaxTF.addFocusListener(rangeFocusListener);
				row = 0;
				//range (y-axis) min/max
				JLabel rangeMinLbl = new JLabel("Y Min: ");
				JLabel rangeMaxLbl = new JLabel("Y Max: ");
				rangeMinTF = new JTextField(6);
				rangeMinTF.setText(format.format(minRange));
				rangeMinTF.addActionListener(rangeValueListener);
				rangeMinTF.addFocusListener(rangeFocusListener);
				rangeMaxTF = new JTextField(6);
				rangeMaxTF.setText(format.format(maxRange));
				rangeMaxTF.addActionListener(rangeValueListener);
				rangeMaxTF.addFocusListener(rangeFocusListener);
				resetRangeBtn = new JButton(resetRangeAct);
				row = 0;
				axesPnl.add(domainMinLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				axesPnl.add(domainMinTF, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
				axesPnl.add(domainMaxLbl, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				axesPnl.add(domainMaxTF, new GridBagConstraints(3, row, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
				row++;
				axesPnl.add(rangeMinLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				axesPnl.add(rangeMinTF, new GridBagConstraints(1, row, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
				axesPnl.add(rangeMaxLbl, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
				axesPnl.add(rangeMaxTF, new GridBagConstraints(3, row, 1, 1, 1, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
				row++;
				axesPnl.add(resetRangeBtn, new GridBagConstraints(0, row, 4, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));

				controlPnl.add(xFlipChk);
				controlPnl.add(yFlipChk);
				controlPnl.add(dataPtChk);
				controlPnl.add(legendChk);
				controlPnl.add(axesPnl);
				
				mainPnl.add(controlPnl);
				
				controlFrame.setContentPane(mainPnl);
				controlFrame.pack();
				controlFrame.setLocationRelativeTo(chartPanel);
			}
			
			//show control frame
			controlFrame.setVisible(true);
		}
	};
		
	private Action showDataPtAct = new AbstractAction("Show Datapoints") {
		public void actionPerformed(ActionEvent e) {
			showDataPts = dataPtChk.isSelected();
			XYPlot plot = (XYPlot)chart.getPlot();
			XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
	        renderer.setBaseShapesVisible(showDataPts);
	        
	        updateSettings();
		}
	};
	
	private Action xFlipAct = new AbstractAction("Flip X-Axis") {
		public void actionPerformed(ActionEvent e) {
			XYPlot plot = chart.getXYPlot();
			ValueAxis domain = plot.getDomainAxis();
			flipXAxis = !domain.isInverted();
			domain.setInverted(flipXAxis);
	        
	        updateSettings();
		}
	};
	
	private Action yFlipAct = new AbstractAction("Flip Y-Axis") {
		public void actionPerformed(ActionEvent e) {
			flipYAxis = yFlipChk.isSelected();
			XYPlot plot = chart.getXYPlot();
			ValueAxis range = plot.getRangeAxis();
			range.setInverted(flipYAxis);
	        
	        updateSettings();
		}
	};
	
	private Action legendAct = new AbstractAction("Show Legend") {
		public void actionPerformed(ActionEvent e) {
			showLegend = legendChk.isSelected();
			if(showLegend){
				LegendTitle legend = new LegendTitle(chart.getPlot());
	            legend.setMargin(new RectangleInsets(1.0, 1.0, 1.0, 1.0));
	            legend.setFrame(new LineBorder());
	            legend.setBackgroundPaint(Color.white);
	            legend.setPosition(RectangleEdge.BOTTOM);
				chart.addLegend(legend);
			}
			else{
				chart.removeLegend();
			}
	        
	        updateSettings();
		}
	};
	
	private ActionListener rangeValueListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			setRangeValues();
			updateChartRange();
	        
	        updateSettings();
		}
	};
	
	private FocusListener rangeFocusListener = new FocusListener() {
		public void focusLost(FocusEvent e) {
			setRangeValues();
			updateChartRange();
	        
	        updateSettings();
		}
		
		public void focusGained(FocusEvent e) {
			// Do nothing when focus gained
		}
	};
	
	private void setRangeValues(){
		try{
			minDomain = Double.parseDouble(domainMinTF.getText());
			maxDomain = Double.parseDouble(domainMaxTF.getText());
			minRange = Double.parseDouble(rangeMinTF.getText());
			maxRange = Double.parseDouble(rangeMaxTF.getText());
		}catch (Exception e){
			e.printStackTrace();
			Util.showMessageDialog("Invalid inputs. Inputs must be numbers.",
					"Invalid Range Inputs", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private Action resetRangeAct = new AbstractAction("Reset to Defaults".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			setRangeValuesToDefaults();
			updateChartRange();
	        
	        updateSettings();
		}
	};
	
	private void updateChartRange(){
		Range domainRange = new Range(minDomain, maxDomain);
		Range rangeRange = new Range(minRange, maxRange);
		
		XYPlot plot = (XYPlot) chart.getPlot();
		plot.getDomainAxis().setRange(domainRange);
		plot.getRangeAxis().setRange(rangeRange);
	}
	
	/**
	 * If using the defaults, make sure to set all the value variables
	 * as well as the UI if it exists.
	 */
	private void setRangeValuesToDefaults(){
		minDomain = minDomainDefault;
		maxDomain = maxDomainDefault;
		minRange = minRangeDefault;
		maxRange = maxRangeDefault;
		
		//update ui if not null
		if(domainMaxTF!=null){
			domainMinTF.setText(format.format(minDomain));
			domainMaxTF.setText(format.format(maxDomain));
			rangeMinTF.setText(format.format(minRange));
			rangeMaxTF.setText(format.format(maxRange));
		}
	}

	
	/**
	 * This method will check all the chart settings values
	 * in the StampLayerSettings class, and if any of them
	 * don't match the current chart settings in this class,
	 * it will update their values accordingly.  This method
	 * should be called after any of the local settings in
	 * this class may have changed.
	 */
	private void updateSettings(){
		//have to check for null on the settings. variables, because
		// the first time through, they should all be null
		if(settings.showLegend == null || showLegend != settings.showLegend){
			settings.showLegend = showLegend;
		}
		if(settings.showDataPts == null || showDataPts != settings.showDataPts){
			settings.showDataPts = showDataPts;
		}
		if(settings.flipXAxis == null || flipXAxis != settings.flipXAxis){
			settings.flipXAxis = flipXAxis;
		}
		if(settings.flipYAxis == null || flipYAxis != settings.flipYAxis){
			settings.flipYAxis = flipYAxis;
		}
		if(settings.minDomain == null || minDomain != settings.minDomain){
			settings.minDomain = minDomain;
		}
		if(settings.maxDomain == null || maxDomain != settings.maxDomain){
			settings.maxDomain = maxDomain;
		}
		if(settings.minRange == null || minRange != settings.minRange){
			settings.minRange = minRange;
		}
		if(settings.maxRange == null || maxRange != settings.maxRange){
			settings.maxRange = maxRange;
		}
		if(settings.plotType == null || !((String)columnBx.getSelectedItem()).equals(settings.plotType)){
			settings.plotType = (String)columnBx.getSelectedItem();
		}
	}
	
	private ArrayList<SpectraObject> getSpectraFromStamps(List<StampShape> stampData){
		ArrayList<SpectraObject> objs = new ArrayList<SpectraObject>();
		if(stampData.size()>0){
			
			String plotName = (String)columnBx.getSelectedItem();
			
			int index = 0;
			
			if (plotName.contains("[radiance]")) {
				plotName = "osl3c_rec_ioverf";
				index=0;
			} else if (plotName.contains("[uncertainty]")) {
				plotName = "osl3c_rec_ioverf";
				index=1;				
			} else if (plotName.contains("[quality]")) {
				plotName = "osl3c_rec_ioverf";
				index=2;
			}
			
			for(StampShape stamp : stampData) {
				
				//create SpectraObjects for each stamp
				SpectraObject so = null;
			
				String name = stamp.getSpectraName();
				boolean marked = false;
				//if it's a "marked" record, add an * to the name
				if(markedStamps.contains(stamp)){
					name += "*";
					marked = true;
				}
				
				double xValues[] = stamp.getXValues(plotName);
				
				Object val = stamp.getVal(plotName);
	
				if (val instanceof float[]) {
					float[] vals = (float[]) val;
	
					if (val!=null) {
						double[] xVals = new double[vals.length];
						double[] yVals = new double[vals.length];
						
						for (int i=0; i<vals.length; i++) {
							//TODO this check is only here because the xaxis and yxais lengths don't match. They SHOULD MATCH!
							//if the index is longer than the x axis, break the loop
							if(i>=xValues.length){
								break;
							}
							
							try {
								xVals[i] = xValues[i];
//								System.out.println(xVals[i]);
								yVals[i] = (double)vals[i];
							} catch (Exception e) {
								e.printStackTrace();
							}						
						}
						so = new SpectraObject(name, xVals, yVals, (String)columnBx.getSelectedItem(), marked, false);
					}
					
				} 
				else if (val instanceof double[]) {
					double[] vals = (double[]) val;
					
					if (vals!=null) {
						double[] xVals = new double[vals.length];
						double[] yVals = new double[vals.length];
						
						for (int i=0; i<vals.length; i++) {
							//TODO this check is only here because the xaxis and yxais lengths don't match. They SHOULD MATCH!
							//if the index is longer than the x axis, break the loop
							if(i>=xValues.length){
								break;
							}
							
							try {
								xVals[i] = xValues[i];
								yVals[i] = (double)vals[i];
							} catch (Exception e) {
								e.printStackTrace();
							}	
							so = new SpectraObject(name, xVals, yVals, (String)columnBx.getSelectedItem(), marked, false);
						}
					}
				} 
				else if (val instanceof Float[]) {
					Float[] vals = (Float[]) val;
					
					if (vals!=null) {
						double[] xVals = new double[vals.length];
						double[] yVals = new double[vals.length];
						
						for (int i=0; i<vals.length; i++) {
							//TODO this check is only here because the xaxis and yxais lengths don't match. They SHOULD MATCH!
							//if the index is longer than the x axis, break the loop
							if(i>=xValues.length){
								break;
							}
							
							try {
								xVals[i] = xValues[i];
								yVals[i] = (double)vals[i];
							} catch (Exception e) {
								e.printStackTrace();
							}
							so = new SpectraObject(name, xVals, yVals, (String)columnBx.getSelectedItem(), marked, false);
						}
					}
				} 
				else if (val instanceof Double[]) {
					Double[] vals = (Double[]) val;
					
					if (vals!=null) {
						double[] xVals = new double[vals.length];
						double[] yVals = new double[vals.length];
						
						for (int i=0; i<vals.length; i++) {
							//TODO this check is only here because the xaxis and yxais lengths don't match. They SHOULD MATCH!
							//if the index is longer than the x axis, break the loop
							if(i>=xValues.length){
								break;
							}
							
							try {
								xVals[i] = xValues[i];
								yVals[i] = (double)vals[i];
							} catch (Exception e) {
								e.printStackTrace();
							}
							so = new SpectraObject(name, xVals, yVals, (String)columnBx.getSelectedItem(), marked, false);
						}
					}
				} 
				else if (val instanceof float[][]) { 
					float[][] vals = (float[][]) val;
	
					if (vals!=null) {
						double[] xVals = new double[vals.length];
						double[] yVals = new double[vals.length];
						
						for (int i=0; i<vals[index].length; i++) {
							//TODO this check is only here because the xaxis and yxais lengths don't match. They SHOULD MATCH!
							//if the index is longer than the x axis, break the loop
							if(i>=xValues.length){
								break;
							}
							
							try {
								xVals[i] = xValues[i];
								yVals[i] = (double)vals[index][i];
							} catch (Exception e) {
								e.printStackTrace();
							}		
							so = new SpectraObject(name, xVals, yVals, (String)columnBx.getSelectedItem(), marked, false);
						}
					}
				}
				else if (val instanceof double[][]) { 
					double[][] vals = (double[][]) val;
	
					if (vals!=null) {
						double[] xVals = new double[vals.length];
						double[] yVals = new double[vals.length];
						
						for (int i=0; i<vals[index].length; i++) {
							//TODO this check is only here because the xaxis and yxais lengths don't match. They SHOULD MATCH!
							//if the index is longer than the x axis, break the loop
							if(i>=xValues.length){
								break;
							}
							
							try {
								xVals[i] = xValues[i];
								yVals[i] = (double)vals[index][i];
							} catch (Exception e) {
								e.printStackTrace();
							}
							so = new SpectraObject(name, xVals, yVals, (String)columnBx.getSelectedItem(), marked, false);
						}
					}
				} else {
					System.err.println("Not a recognized value format for spectra data!");
				}
					
				if(so!=null){
					objs.add(so);
				}
			}
		}
		
		return objs;
	}
	
	public void rebuildColorMap(List<StampShape> stampData){
		stamp2Color.clear();
		
		XYPlot plot = chart.getXYPlot();
		XYItemRenderer renderer = plot.getRenderer();
		
		//cycle through the stamp shapes and find the corresponding
		// plot item for each one. (there may be more plot items
		// than selected stamps, because of marked and calculated spectra)
		for(StampShape stamp : stampData){
			for (int i=0; i<plot.getSeriesCount(); i++) {
				String key = plot.getDataset().getSeriesKey(i).toString();
				if(key.contains(stamp.getId())){
					Color c = (Color)renderer.getItemPaint(i, 0);
					stamp2Color.put(stamp, c);
					break;
				}
			}
		}
	}
	
	public void refreshSpectraData(boolean refreshTable){
		//if the spectra table isn't built yet, it's not ready for a refresh
		if(spectraTbl == null){
			return;
		}
		if(refreshTable){
			refreshTable();
		}
		
		String plotType = (String)columnBx.getSelectedItem();
		
		ArrayList<SpectraObject> allObjs = new ArrayList<SpectraObject>();
		//get spectra from the spectra table selections
		int[] rows = spectraTbl.getSelectedRows();
		SpectraTableModel model = (SpectraTableModel)spectraTbl.getModel();
		allObjs.addAll(model.getSpectra(rows));
		
		
		if(allObjs.size()>0){
			
			XYSeriesCollection data_series = new XYSeriesCollection();
			
			for(SpectraObject so : allObjs){
				XYSeries data = new XYSeries(so.getName());
				
				double xValues[] = so.getXValues();
				double yValues[] = so.getYValues();
				
				for(int i=0; i<yValues.length; i++){
					data.add(xValues[i], yValues[i]);
				}
				data_series.addSeries(data);
			}
			
			
			//recreate the chart
	        chart = ChartFactory.createXYLineChart(
	        		null,		//title
	        		"wave number",		 		//x axis label
	        		"",		 	//y axis label
	        		data_series,			 		//data
	        		PlotOrientation.VERTICAL,	//orientation
	        		showLegend,		 				//legend
	        		true, 						//tooltips
	        		false);						//urls
	        
	        ThemeChart.configureUI(chart);
			XYItemRenderer rr = ((XYPlot) chart.getPlot()).getRenderer();
			rr.setSeriesPaint(0, ThemeChart.getPlotColor());
			
	        chartPanel.removeAll();
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
	        
	        //set whether to show datapoints or not
	        renderer.setBaseShapesVisible(showDataPts);
	        
			
			//Set units
			String units = stampLView.stampLayer.getSpectraAxisXUnitMap().get(plotType);
			if (units==null || units.trim().length()<1) {
				units = "<unknown>";
			}
			ValueAxis domainAxis = plot.getDomainAxis();
			ValueAxis rangeAxis = plot.getRangeAxis();
			domainAxis.setLabel(units);
			
	
			//if there is no data, the ranges will be null, so don't try and 
			// reset them in this case...check that they're both not null first
			if(plot.getDataRange(domainAxis) != null && plot.getDataRange(rangeAxis) != null){
				//if no defaults or values have been set, set the defaults
				// and set the values equal to those defaults
				if(minDomainDefault==null || maxDomainDefault==null || minRangeDefault==null || maxRangeDefault==null){
					//store the range default values
					minDomainDefault = plot.getDataRange(domainAxis).getLowerBound();
					maxDomainDefault = plot.getDataRange(domainAxis).getUpperBound();
					minRangeDefault = plot.getDataRange(rangeAxis).getLowerBound();
					maxRangeDefault = plot.getDataRange(rangeAxis).getUpperBound();
					
					setRangeValuesToDefaults();
				}else{
					//otherwise, grab the previous default values, to use to compare
					// against the set values and determine whether the values should change
					double prevMinDomainDef = minDomainDefault;
					double prevMaxDomainDef = maxDomainDefault;
					double prevMinRangeDef = minRangeDefault;
					double prevMaxRangeDef = maxRangeDefault;
					
					//store the new range default values
					minDomainDefault = plot.getDataRange(domainAxis).getLowerBound();
					maxDomainDefault = plot.getDataRange(domainAxis).getUpperBound();
					minRangeDefault = plot.getDataRange(rangeAxis).getLowerBound();
					maxRangeDefault = plot.getDataRange(rangeAxis).getUpperBound();
					
					if(minDomain == prevMinDomainDef && maxDomain == prevMaxDomainDef &&
							minRange == prevMinRangeDef && maxRange == prevMaxRangeDef){
						setRangeValuesToDefaults();
					}
				}
			}
			
			//use the values to update the range (these values will be the 
			// equal to the defaults if either they have never been set, 
			// or if the previous defaults were being used
			updateChartRange();
			
			//enable the controls button now that we have range defaults
			controlsItm.setEnabled(true);
	
			//only force the x-axis flip if the boolean has not been set
			if(flipXAxis == null){
				String reverse = stampLView.stampLayer.getSpectraAxisReverseMap().get(plotType);
				if (reverse!=null && reverse.trim().equalsIgnoreCase("true")) {
					flipXAxis = true;
				} else {
					flipXAxis = false;
				}
			}
			
			//set the x-axis orientation
			domainAxis.setInverted(flipXAxis);
			//set the y-axis orientation
			rangeAxis.setInverted(flipYAxis);
		}
		else{
			chartPanel.removeAll();
	        chart = ChartFactory.createXYLineChart(
	        		null,		//title
	        		"wave number",		 		//x axis label
	        		"",		 	//y axis label
	        		null,			 		//data
	        		PlotOrientation.VERTICAL,	//orientation
	        		showLegend,		 				//legend
	        		true, 						//tooltips
	        		false);						//urls
	        
	        ThemeChart.configureUI(chart);
			XYItemRenderer rr = ((XYPlot) chart.getPlot()).getRenderer();
			rr.setSeriesPaint(0, ThemeChart.getPlotColor());
	        
			chartPanel.setChart(chart);
			controlsItm.setEnabled(false);
		}
		
		
		// Store the color for each object
		
		for(int i=0; i<model.getRowCount(); i++) {
			getColorForSpectraObject(model.getSpectra(i));
		}
		
		stampLView.redrawEverything(true);
		repaint();
	}
	
	public void addSelectionsFromLView(){
		ArrayList<Integer> rows = new ArrayList<Integer>();
		
		SpectraTableModel model = (SpectraTableModel)spectraTbl.getModel();
		for(StampShape ss: stampLView.stampLayer.getSelectedStamps()){
			for(int i = 0; i<model.getRowCount(); i++){
				if(model.getSpectra(i).getName().contains(ss.getSpectraName())){
					rows.add(i);
					break;
				}
			}
		}
		
		//if too many selections are made then the legend starts to
		// take up a lot of room.  So disable the legend after 50
		if(model.getRowCount()>50){
			showLegend = false;
		}else{
			showLegend = true;
		}
		
		//turn off table refresh until all rows are selected
		refreshData = false;
		for(int row : rows){
			spectraTbl.addRowSelectionInterval(row, row);
		}
		//turn refresh back on and refresh the table once
		refreshData = true;
		refreshSpectraData(false);
	}
	
	/**
	 * Return the color used in the plot for this stamp, if any.  This allows the corresponding outline to be colored the same.
	 * @param stamp
	 * @return Color of the specified stamp
	 */
	public Color getColorForStamp(StampShape stamp) {
		return stamp2Color.get(stamp);
	}

	public Color getColorForSpectraObject(SpectraObject so) {
		XYPlot plot = chart.getXYPlot();
		XYItemRenderer renderer = plot.getRenderer();
		
		for (int i=0; i<plot.getSeriesCount(); i++) {
			String key = plot.getDataset().getSeriesKey(i).toString();
						
			if(key.contains(so.getName())){
				Color c = (Color)renderer.getItemPaint(i, 0);
				so.setColor(c);
				return c;
			}
		}
		
		so.setColor(Color.black);
		return Color.black;
	}
	
	/**
	 * Dispose any control frame
	 */
	public void cleanUp(){
		//chart controls
		if(controlFrame!=null){
			controlFrame.dispose();
		}
		if(myFrame!=null){
			myFrame.dispose();
		}
	}
	
	public void showInFrame(){
		//create the frame if it hasn't been created
		if(myFrame == null){
			myFrame = new JFrame();
			myFrame.setTitle("Spectral Viewer");
			myFrame.setContentPane(this);

			Dimension prefSize = this.getPreferredSize();

			//set the location relative to the focus panel but centered vertically
			if (stampLView.getFocusPanel().isShowing()) { // If we're clicking on the focus panel
				Point pt = stampLView.getFocusPanel().getLocationOnScreen();
				myFrame.setLocation(pt.x + (int)prefSize.getWidth()/4, pt.y + (int)prefSize.getHeight()/4);
			} else { // If we're somewhere else - eg. clicking on a CRISM Spectra outline
				Point pt = MouseInfo.getPointerInfo().getLocation();
				myFrame.setLocation(pt.x - (int)prefSize.getWidth()/4, pt.y - (int)prefSize.getHeight()/4);
			}
			
			myFrame.pack();
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
	
	public void addMarkedStamp(StampShape stamp){
		//only add it if it's not already in the list
		if(!markedStamps.contains(stamp)){
			markedStamps.add(stamp);
		}
		
		refreshTable();
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
						String name = f.getName();
						if(name.contains(".csv") || name.contains(".txt")){
							return true;
						}
						return false;
					}
				});
				
				int result = fileChooser.showSaveDialog(SpectraView.this);
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
							writeCSV(file);
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
		
		String xAxis = "wave number";

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
}


