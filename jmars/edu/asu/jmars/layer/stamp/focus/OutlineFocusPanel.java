package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.lang3.ArrayUtils;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.stamp.SpectraUtil;
import edu.asu.jmars.layer.stamp.StampGroupComparator;
import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.focus.MultiExpressionDialog.ColumnExpression;
import edu.asu.jmars.layer.stamp.focus.OutlineOrderDialog.OrderRule;
import edu.asu.jmars.layer.util.features.FieldFormulaMethods;
import edu.asu.jmars.swing.BoundsPopupMenuListener;
import edu.asu.jmars.swing.FancyColorMapper;
import edu.asu.jmars.swing.OutlineIconButton;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.FilteringColumnModel;
import edu.asu.jmars.viz3d.ThreeDManager;
import gnu.jel.CompiledExpression;
import gnu.jel.DVMap;
import gnu.jel.Evaluator;
import gnu.jel.Library;

@SuppressWarnings("serial")
public class OutlineFocusPanel extends JPanel {

	// Constant for dynamic calculation of row num column.  Needs to a unique number that does not match a real column
	private static final int ROWNUM_COL = -7;
	
	private JFileChooser exportChooser;
	private StampLayer myLayer;
	StampLayerSettings mySettings;
	private StampTable myStampTable;
	private JLabel countLbl;
	private String recordCountStr = " Total records in current view";
	private JCheckBox limitToMainViewCBx;
	private JButton multiExpBtn;
	private MultiExpressionDialog multiExpDg;
	private JComboBox<String> columnBx;
	private OutlineOrderDialog orderDialog;
	private ScatterView scatterView;
	private JCheckBox hideOutofRangeCBx;
	private JTextField scaleTF;
	private JLabel minValueLbl;
	private JLabel maxValueLbl;
	private String minValStr = "Column Min: ";
	private String maxValStr = "Column Max: ";
	private JCheckBox lockColorRange;
	private JTextField absMinTF;
	private JTextField absMaxTF;
	private JTextField minTF;
	private JTextField maxTF;
	private FancyColorMapper mapper;
	private JXTaskPane spotPnl;
	private JRadioButtonMenuItem selStampsRB;
	private JRadioButtonMenuItem allStampsRB;
	private ButtonGroup exportStampsGroup = new ButtonGroup();
    final JCheckBoxMenuItem exportGeometry = new JCheckBoxMenuItem("Include geometry in export?");
    	
	private CompiledExpression compiledExpression = null;
	private BoundsPopupMenuListener cbListener = new BoundsPopupMenuListener(true, false);
	
	//UI constraints
	// Two formatters so that we can round in a way that includes our extreme values
	private DecimalFormat minFormat = new DecimalFormat();
	private DecimalFormat maxFormat = new DecimalFormat();
    private int row = 0;
    private int pad = 2;
    private Insets in = new Insets(pad,pad,pad,pad);
	
    private SpectraView spectraView;
    
    private static final Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();   
    private static final Icon downIcon = new ImageIcon(ImageFactory.createImage(ImageCatalogItem.CARET_DOWN_IMG
    		         .withDisplayColor(imgColor)
    		         .withWidth(10)
    		         .withHeight(10)));
    
    private DebugLog log = DebugLog.instance();
    
	// creates and returns a default unfilled stamp panel.
	public OutlineFocusPanel(final StampLayer stampLayer, final StampTable table, ScatterView scatter, SpectraView spectra)
	{
		myLayer = stampLayer;
		myStampTable = table;
		mySettings = myLayer.getSettings();
		spectraView = spectra;
		scatterView = scatter;
		multiExpDg = new MultiExpressionDialog(myLayer);	
		
		minFormat.setMaximumFractionDigits(4);
		minFormat.setGroupingUsed(false);
		minFormat.setRoundingMode(RoundingMode.FLOOR);

		maxFormat.setMaximumFractionDigits(4);
		maxFormat.setGroupingUsed(false);
		maxFormat.setRoundingMode(RoundingMode.CEILING);
		
		buildUI();
	}
	
	private void buildUI(){
		//top panel has find and export buttons
		JPanel top = new JPanel();
	    JButton findStamp = new JButton(findAct);

	    final JFileChooser fc = new JFileChooser(Util.getDefaultFCLocation());
        FileFilter ff = new FileFilter(){				
			public String getDescription() {
				return "Tab delimited file (.tab, txt)";
			}
			
			public boolean accept(File f) {
				if (f.isDirectory()) return true;
				if (f.getName().endsWith(".txt")) return true;
				if (f.getName().endsWith(".tab")) return true;

				return false;
			}
		};
		fc.setDialogTitle("Export Stamp Table");
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(ff);
        
        JMenuItem origExport = new JMenuItem(new AbstractAction("As Table...".toUpperCase()) {
	        public void actionPerformed(ActionEvent e){
	            File f;
	            
	            do {
	                if (fc.showSaveDialog(OutlineFocusPanel.this)
	                        != JFileChooser.APPROVE_OPTION)
	                    return;
	                f = fc.getSelectedFile();
	                
	                if (!f.getName().endsWith(".txt")&&!f.getName().endsWith(".tab")) {
	                	f=new File(f.getAbsolutePath()+".txt");
	                } 	
	            }
	            while( f.exists() && 
	                    JOptionPane.NO_OPTION == Util.showConfirmDialog(
                               "File already exists, overwrite?\n" + f,
                               "FILE EXISTS",
                               JOptionPane.YES_NO_OPTION,
                               JOptionPane.WARNING_MESSAGE
	                    )
	            );
	            try {
	                PrintStream fout = new PrintStream(new FileOutputStream(f));
	                
	                synchronized(myStampTable) {
	                    int rows = myStampTable.getRowCount();
	                    int cols = myStampTable.getColumnCount();
	                    
	                    // Output the header line
	                    if (exportGeometry.isSelected()) {
	                    	fout.print("geometry\tFeature:string\t");
	                    }
	                    for (int j=0; j<cols; j++)
	                        fout.print(myStampTable.getColumnName(j)
	                                   + (j!=cols-1 ? "\t" : "\n"));
	                    
	                    // Output the data
	                    for (int i=0; i<rows; i++) {
                        	if (exportGeometry.isSelected()) {
                        		String delimiter = "\t";
                        		StampShape stamp = myStampTable.getStamp(i);
                        		
                                //add the detector outlines
                                double stampPoints[] = stamp.getStamp().getPoints();
                                
                                fout.print("\"POLYGON((");
                                
                                for(int idx = 0; idx < stampPoints.length; idx+=2) {
                                	if (idx>0) {
                                        fout.print(delimiter);
                                	}
                                	fout.print(minFormat.format(360-stampPoints[idx]));
                                	fout.print(" ");
                                	fout.print(minFormat.format(stampPoints[idx+1]));
                                }
                                
                                fout.print("))\"");
                                fout.print(delimiter);
                                fout.print("polygon");
                                fout.print(delimiter);
                        	}
	                    	
	                        for (int j=0; j<cols; j++) {
	                            fout.print(myStampTable.getValueAt(i, j)
	                                       + (j!=cols-1 ? "\t" : "\n"));
	                        }
	                	}
	                    
	                    fout.close();
	                }
	            } 
	            catch(FileNotFoundException ex){
	                Util.showMessageDialog(                                             
	                		"Unable to open file!\n" + f,
                          "FILE OPEN ERROR",
                          JOptionPane.ERROR_MESSAGE
	                );
	            }
	        }
	    });
        
        origExport.setToolTipText("The original stamp table export format");
	    
        //selected vs. all stamps export
        selStampsRB = new JRadioButtonMenuItem("Selected Stamps");
        allStampsRB = new JRadioButtonMenuItem("All Stamps");
        exportStampsGroup.add(selStampsRB);
        exportStampsGroup.add(allStampsRB);
        allStampsRB.setSelected(true);
        
	    /// NEW EXPORT  
        JMenuItem csvExport = new JMenuItem(new SaveAction("As CSV...".toUpperCase(), ",")); 
	    
		JPopupMenu exportMnu = new JPopupMenu();
		exportMnu.add(allStampsRB);
		exportMnu.add(selStampsRB);
		exportMnu.add(new JSeparator());
		exportMnu.add(exportGeometry);
		exportMnu.add(new JSeparator());
		exportMnu.add(origExport);
		exportMnu.add(csvExport);

		//Create the export button with text and icon		
        OutlineIconButton exportBtn = new OutlineIconButton("Export Table".toUpperCase(), downIcon);
        exportBtn.setHorizontalTextPosition(SwingConstants.LEFT);
        //add a dropdown type menu to the Export Button
        exportBtn.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent evt){
        		exportMnu.show(exportBtn, 0, exportBtn.getHeight());
        	}
        });
         
        if(myLayer.getInstrument().equals("TES")){

        	JMenu davMenu = new JMenu("Davinci Vanilla File".toUpperCase());
        	davMenu.setToolTipText("Exports a file compatible with the davinci function 'split_ock'");
        	
        	JMenuItem davExportWithXAxis = new JMenuItem(new AbstractAction("Vanilla with X-Axis...".toUpperCase()) {
				public void actionPerformed(ActionEvent e) {
					davinciTesExport(true);
				}
			});
        	
        	JMenuItem davExportWithoutXAxis = new JMenuItem(new AbstractAction("Vanilla without X-Axis...".toUpperCase()) {
				public void actionPerformed(ActionEvent e) {
					davinciTesExport(false);
				}
			});
        	
        	davMenu.add(davExportWithXAxis);
        	davMenu.add(davExportWithoutXAxis);
        	exportMnu.add(davMenu);
        }
        
	    //add to top panel
		top.add(findStamp);
		top.add(exportBtn);
		
	    
		//bot panel has limit check box and count label
	    JPanel bot = new JPanel();
	    bot.setLayout(new GridBagLayout());
	    
	    countLbl = new JLabel(recordCountStr);
	    limitToMainViewCBx = new JCheckBox(limitMainViewAct);
	    limitToMainViewCBx.setSelected(mySettings.limitTableToMainView);
	    //Checkbox has to be in a panel in order to respect the gridbagconstraint centering
	    JPanel readoutPnl = new JPanel();
	    readoutPnl.add(countLbl);
	    readoutPnl.add(Box.createHorizontalStrut(10));
	    readoutPnl.add(limitToMainViewCBx);
	    
	    orderDialog = new OutlineOrderDialog(myLayer, this);
	    JPanel orderPnl = orderDialog.getPreview();
	    
	    JButton scatterBtn = new JButton(scatterAct);
	    
	    //add the multi expression btn regardless of stamp type
	    multiExpBtn = new JButton(multiExpAct);
	    multiExpBtn.setEnabled(false);
	    multiExpBtn.setToolTipText("Will be available when stamp columns finish loading");
	    
	    JPanel btnPnl = new JPanel();
	    btnPnl.add(multiExpBtn);
	    btnPnl.add(scatterBtn);
	    
	    //add to bottom panel
	    row = 0;
	    bot.add(readoutPnl, new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    row++;
    	bot.add(orderPnl, new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    row++;
	    bot.add(btnPnl, new GridBagConstraints(0, row, 1, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, in, pad, pad));
    	
	    //if this is a spectra layer, add a button to view the spectra view
	    if(myLayer.spectraPerPixel() || myLayer.spectraData()){
	    	JButton spectraBtn = new JButton(spectraAct);
	    	btnPnl.add(spectraBtn);
	    }
	    
	    //center panel has table and bot panel
	    JPanel newCenterPanel = new JPanel(new BorderLayout());
	    newCenterPanel.add(new JScrollPane(myStampTable), BorderLayout.CENTER);	    
	    newCenterPanel.add(bot, BorderLayout.SOUTH);
	    
	    //add everything to main panel
	    setLayout(new BorderLayout());
	    add(top,    BorderLayout.NORTH);
	    add(newCenterPanel, BorderLayout.CENTER);
	    
	    // if either of these conditions are met, there will be 
	    // spot panel added to the BorderLayout.SOUTH of the main panel
	    if (myLayer.spectraData() || myLayer.pointShapes()) {
	    	
	    //spot color panel -- includes color basis, min, max fields, and colorMapper
			spotPnl = new JXTaskPane();
			spotPnl.setLayout(new GridBagLayout());
			spotPnl.setTitle("Stamp Appearance");
			spotPnl.setAnimated(false);
			spotPnl.addComponentListener(spotPaneListener);
	    	
	    	//base color column (or expression if tes layer)
		    //column names to choose from
		    columnBx = new JComboBox<String>();
		    columnBx.addActionListener(columnBoxListener);
		    JPanel columnPnl = new JPanel();
		    columnPnl.add(new JLabel("Base color on:"));
		    columnPnl.add(columnBx);

			
		    //min, max labels and text fields
		    Dimension minDim = new Dimension(120,19);
		    minValueLbl = new JLabel(minValStr);
		    maxValueLbl = new JLabel(maxValStr);
			Color absValCol = new Color(100,100,100);  //TODO: get from theme
			absMinTF = new JTextField(10);
			absMinTF.setEditable(false);
			absMinTF.setForeground(absValCol);
			absMinTF.setMinimumSize(minDim);
			absMaxTF = new JTextField(10);
			absMaxTF.setEditable(false);
			absMaxTF.setForeground(absValCol);
			absMaxTF.setMinimumSize(minDim);
			minTF = new JTextField(10);
			minTF.setMinimumSize(minDim);
			minTF.addActionListener(valuesChanged);
			minTF.addFocusListener(focusChanged);
			maxTF = new JTextField(10);
			maxTF.setMinimumSize(minDim);
			maxTF.addActionListener(valuesChanged);
			maxTF.addFocusListener(focusChanged);
		    double min = mySettings.colorMin;
		    double max = mySettings.colorMax;
		    if (!Double.isNaN(min)) {
		    	minTF.setText(minFormat.format(mySettings.colorMin));
		    }
		    if (!Double.isNaN(max)) {
		    	maxTF.setText(maxFormat.format(mySettings.colorMax));
		    }

		    //put min/max labels and textfields in a panel
		    JPanel valuePnl = new JPanel(new GridBagLayout());
		    row = 0;
		    valuePnl.add(minValueLbl, new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(absMinTF, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(maxValueLbl, new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(absMaxTF, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));

		    lockColorRange = new JCheckBox("Lock color range");
		    lockColorRange.setSelected(mySettings.lockColorRange);
		    lockColorRange.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					mySettings.lockColorRange=lockColorRange.isSelected();
				}
			});
		    
		    row++;
		    valuePnl.add(new JLabel("Min value: "), new GridBagConstraints(0, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(minTF, new GridBagConstraints(1, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(new JLabel("Max value: "), new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
		    valuePnl.add(maxTF, new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.NONE, in, pad, pad));
	    	row++;
	    	GridBagConstraints lockCons = new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(pad, pad, 0, pad), pad, pad);
		    //if this is point shapes stamp layer, add a scale option
	    	if (myLayer.pointShapes() && !myLayer.fixedSpotSize()) {
	    		//if it's mola change the width of the lockConstrains ui
	    		lockCons.gridwidth = 3;
	    		
	    		scaleTF = new JTextField(10);
		  		scaleTF.setText(""+mySettings.getOriginMagnitude());
			    scaleTF.addActionListener(scaleListener);
				scaleTF.addActionListener(valuesChanged);
				scaleTF.addFocusListener(focusChanged);
	    		valuePnl.add(new JLabel("Spot size: "),new GridBagConstraints(2, row, 1, 1, 0, 0, GridBagConstraints.LINE_END, GridBagConstraints.NONE, in, pad, pad));
	    		valuePnl.add(scaleTF,new GridBagConstraints(3, row, 1, 1, 0, 0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, in, pad, pad));
	    	}
		    valuePnl.add(lockColorRange, lockCons);
			
	    	//only show hide for spectra, not points 
	    	//TODO: make this work for points?
	    	if(myLayer.spectraData()){
	    		hideOutofRangeCBx = new JCheckBox("Hide values outside of range");
				hideOutofRangeCBx.addActionListener(valuesChanged);
				hideOutofRangeCBx.setSelected(mySettings.hideValuesOutsideRange);
				//checkbox has to be in a panel in order to respect the gridbagconstraints centering
				JPanel hidePnl = new JPanel();
				hidePnl.add(hideOutofRangeCBx);
				valuePnl.add(hidePnl, new GridBagConstraints(2, row, 2, 1, 0.5, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(pad, pad, 0, pad), pad, pad));
	    	}
		    
	    	//color mapper
	    	mapper = new FancyColorMapper();
			if (mySettings.colorState!=null) {
				mapper.setState(mySettings.colorState);
			}
		    mapper.addChangeListener(mapperListener);
	    	

		    //Add everything to spot panel
			//always add the base color label
			row = 0;
			if(myLayer.spectraData() || myLayer.pointShapes()){
	    		spotPnl.add(columnPnl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(pad,pad,0,pad), pad, pad));
	       	}
	       	//min,max,scale values
	    	row++;
	    	spotPnl.add(valuePnl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(pad,pad,0,pad), pad, pad));
	    	//color mapper
	    	row++;
		    spotPnl.add(mapper, new GridBagConstraints(0, row, 2, 1, 1, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0,pad,pad,pad), pad, pad));

		    //put the spot panel into a jxtastpanecontainer
		    JXTaskPaneContainer spotContainer = new JXTaskPaneContainer();		   
		    spotContainer.add(spotPnl);
		    
		    //add panel to main panel
		    add(spotContainer, BorderLayout.SOUTH);
		    
		    this.setPreferredSize(new Dimension(467,700));
	    }
	}
	
	//logic for creating a table export that is compatible with the davinci function "split_ock"
	// (http://davinci.asu.edu/index.php?title=split_ock)
	// The logic checks to make sure ock, ick, det, and one spectra column are in the current
	// stamp table, and if so, then it puts them in the correct order and writes out the tab
	// separated vanilla file.
	// The boolean is for whether or not to also export the xaxis (works either way for "split_ock")
	private void davinciTesExport(boolean exportXAxis){
		LinkedHashMap<String,String> columnsAndHeaders = new LinkedHashMap<String,String>();
		// first, check to see if the necessary columns are present
		String ockStr = "orbit_counter_keeper";
		String ickStr = "instrument_time_count";
		String detStr = "detector";
		String errMessage = "In order to be exported in this format, the stamp table\n"
						+ "must have all of the following columns visble:\n\n"
				+ ockStr + "\n"
				+ ickStr + "\n"
				+ detStr + "\n"
				+ "\nAnd only one of the following columns visible:\n\n";
		// cycle through columns and create an array of names
		ArrayList<String> columnNames = new ArrayList<String>();
		for (int i=0; i<myStampTable.getColumnCount(); i++){
			columnNames.add(myStampTable.getColumnName(i));
		}
		
		boolean hasOck = columnNames.contains(ockStr);
		boolean hasIck = columnNames.contains(ickStr);
		boolean hasDet = columnNames.contains(detStr);
		
		int spectraCount = 0;
		String spectraCol = "";
		for(String dataType : myLayer.getSpectraColumns()){
			errMessage+= dataType+"\n";
			if(columnNames.contains(dataType)){
				spectraCol = dataType;
				spectraCount++;
			}
		}
		
		// only proceed if the table has ock, ick, det, and one spectra column added
		if(!(hasOck && hasIck && hasDet) || spectraCount!=1){
			Util.showMessageDialogObj(errMessage, "Incompatible Columns", JOptionPane.ERROR_MESSAGE);
			return;
		}
		else{
			//create the column and header map in the correct order and write out the file
			columnNames.remove(ockStr);
			columnNames.remove(ickStr);
			columnNames.remove(detStr);
			columnNames.remove(spectraCol);
			
			//proper order is: ock, ick, det, [all other columns], spectraColumn
			columnsAndHeaders.put(ockStr, "ock");
			columnsAndHeaders.put(ickStr, "ick");
			columnsAndHeaders.put(detStr, "det");
			for(String col : columnNames){
				columnsAndHeaders.put(col, col);
			}
			columnsAndHeaders.put(spectraCol, spectraCol);
			
			writeTableExport("\t", "null", columnsAndHeaders, false, exportXAxis, exportGeometry.isSelected());
		}
	}
	
	private Action scatterAct = new AbstractAction("Open Scatter View...".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			scatterView.showInFrame();
		}
	};
	
	private Action spectraAct = new AbstractAction("Open Spectra View...".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			spectraView.showInFrame();
		}
	};
	
	private Action multiExpAct = new AbstractAction("Create New Expression...".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			//set the location relative to the focus panel but centered vertically
			Point pt = OutlineFocusPanel.this.getLocationOnScreen();
			multiExpDg.setLocation(pt.x+20, pt.y);

			multiExpDg.setVisible(true);
		}
	};
	
	private Action findAct = new AbstractAction("Find stamp...".toUpperCase()) {
		public void actionPerformed(ActionEvent e) {
			String id = Util.showInputDialog(
                    "Enter a stamp id:", "Find stamp...", JOptionPane.QUESTION_MESSAGE);

                if (id == null) {
                    return;
                }
                
                StampShape stamp = myLayer.getStamp(id.trim());
                if (stamp!=null) {
                	myLayer.clearSelectedStamps();
                	myLayer.viewToUpdate.panToStamp(stamp);
                    return;
                }
                    
                Util.showMessageDialog("Can't find the stamp \"" + id
                                              + "\", are you sure\n"
                                              + "it meets your layer's selection criteria?",
                                              "Find stamp...",
                                              JOptionPane.ERROR_MESSAGE);
			
		}
	};
	
	private Action limitMainViewAct = new AbstractAction("Limit Records by Main View") {
		public void actionPerformed(ActionEvent e) {
			mySettings.limitTableToMainView=!mySettings.limitTableToMainView;
			myStampTable.dataRefreshed();
			dataRefreshed();
		}
	};
	
    private ActionListener valuesChanged = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			updateChanges();
		}
	};
	
	private FocusListener focusChanged = new FocusListener() {
		public void focusLost(FocusEvent e) {
			updateChanges();
		}
		public void focusGained(FocusEvent e) {
			// Nothing to do here
		}
	};
	
	private void updateChanges(){
		recalculateColors();
		updateSettings();
		myLayer.increaseStateId(StampLayer.OUTLINES_BUFFER);

		refreshViews();
	}
	
	private ActionListener columnBoxListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			String name = (String)columnBx.getSelectedItem();
			
			//update the expression
			ColumnExpression exp = myLayer.getColumnExpression(name);
			// first check the layer for the added expressions
			if(exp!=null){
				compiledExpression = exp.getCompiledExpression();
			}
			// its safe to set the expression to null
			else{
				compiledExpression = null;
			}
			recalculateMinMaxValues();
			mySettings.colorColumn = name;
			myLayer.increaseStateId(StampLayer.OUTLINES_BUFFER);
			refreshViews();		
		}
	};

	
	private ActionListener scaleListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			//in order to make sure that the LView and the focus panel are using the same settings object, we will get the 
      		//settings object out of the LView instead of using the final instance that is passed in
			mySettings.setOriginMagnitude(Double.parseDouble(scaleTF.getText()));
       		StampLView child = (StampLView) myLayer.viewToUpdate.getChild();
       		myLayer.viewToUpdate.clearLastOutlines();
   			myLayer.viewToUpdate.drawOutlines();
   			child.clearLastOutlines();
  			child.drawOutlines();
		}
	};
	
	private ChangeListener mapperListener = new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
            if (mapper.isAdjusting()){
                return;
            }
            recalculateColors();
            myLayer.increaseStateId(StampLayer.OUTLINES_BUFFER);
			mySettings.colorState=mapper.getState();
			refreshViews();			
		}
	};
	
	private ComponentListener spotPaneListener = new ComponentListener() {
		public void componentShown(ComponentEvent e) {}
		public void componentMoved(ComponentEvent e) {}
		public void componentHidden(ComponentEvent e) {}
		
		@Override
		public void componentResized(ComponentEvent e) {
			//only reset split pane if the spot panel is being expanded
			if(!spotPnl.isCollapsed()){
				if(OutlineFocusPanel.this.getParent() instanceof JSplitPane){
					((JSplitPane)OutlineFocusPanel.this.getParent()).resetToPreferredSizes();
				}
			}
		}
	};
	
	
	public boolean hideOutofRange() {
		if (hideOutofRangeCBx==null) return false;
		return hideOutofRangeCBx.isSelected();
	}
	
	
	
	public double getMinValue() {
		String val = minTF.getText();
		
		if (val==null || val.length()==0) {
			recalculateMinMaxValues();
		}
		return parseTextToDouble(minTF.getText());
	}
	
	public double getMaxValue() {
		return parseTextToDouble(maxTF.getText());
	}

	public void setMinValue(double newVal) {
		if (!Double.isNaN(newVal)) {
			minTF.setText(minFormat.format(newVal));
		} else {
			minTF.setText("Undefined");
		}
	}
	
	public void setMaxValue(double newVal) {
		if (!Double.isNaN(newVal)) {
			maxTF.setText(maxFormat.format(newVal));
		} else {
			maxTF.setText("Undefined");
		}		
	}
	
	/**
	 * Used when initially loading a stamp layer.  Sets the color expression
	 * (which is an 'old' concept) as the first ColumnExpression on the 
	 * MultiExpressionDialog, also sets the ColumnBox to have that column selected
	 * @param text  The expression text for the default color column 
	 * (The column is also known as the "Calculated_Value" column)
	 */
	public void setColorExpression(String text) {
		if (text==null) return;
		
		text = text.trim();
		
		//add the expression to the multiexp dialog
		//TODO: this should match the column name in the table, which is currently
		// "Calculated_Value", although "Default_Expression" is more consistent
		// with our new framework
		String defaultExpStr = "Calculated_Value";
		
		boolean success = multiExpDg.addExpression(defaultExpStr, text);
		
		if(success){
			columnBx.setSelectedItem(defaultExpStr);
		}
	}
	
	/**
	 * Add a ColumnExpression to the list which is kept in the MultiExpressionDialog
	 * @param newExp The new expression to add
	 * @return  Returns true if the expression has a unique name and was added successfully
	 */
	public boolean addColumnExpression(ColumnExpression newExp){
		return multiExpDg.addExpression(newExp);
	}
	
	
	/**
	 * Set the order column and direction, if an order is not
	 * already being used
	 * @param column  Column name to order by
	 * @param reverse True if descending 
	 */
	public void setOrderOption(String column, boolean reverse) {
		if (column==null) return;
		
		column = column.trim();
		
		if(orderDialog.getOrderRules().size()==0){
			if(column.length()>0){
				ArrayList<OrderRule> rules = new ArrayList<OrderRule>();
				rules.add(new OrderRule(column, reverse));
				orderDialog.addOrderRules(rules);
			}
		}
	}
	
	/**
	 * Set the order column and directions based on an array
	 * of OrderRules, if an order is not already being used
	 * @param rules  The list of OrderRules to use
	 */
	public void setOrderOption(ArrayList<OrderRule> rules){
		if(orderDialog.getOrderRules().size()==0){
			orderDialog.addOrderRules(rules);
		}
	}
	
	public void recalculateMinMaxValues() {
		// This synchronized block is required to prevent a race condition, where the minField or maxField can be set multiple times.
		// Apparently the setText on a TextField is NOT thread-safe, and you can end up with the value being duplicated multiple times.
		synchronized(minTF) {
			double min = Double.MAX_VALUE;
			double max = Double.NEGATIVE_INFINITY;
		
			ArrayList<StampShape> stamps = myLayer.getStamps();
		
			if (stamps==null) {
				return;
			}
			
			int columnToColor = getColorColumn();
			try {
				for (StampShape s : stamps) {
					if (s.isHidden()) continue;
					
					double v=Double.NaN;
					if (compiledExpression!=null) {
						StampAccessor sa[] = new StampAccessor[1];
						sa[0]=new StampAccessor(myLayer, s);
						
						try {
							Object o = compiledExpression.evaluate(sa);
							v = Double.parseDouble(o+"");
						} catch (Throwable e) {
//							System.err.println("exception on stamp: " + s.getId());
//							e.printStackTrace();
							// DO NOT BREAK, or the calculated value does not get set on the stamp shape
//    							break;
						}	
					} else {
						try {
							Object o = s.getStamp().getData()[columnToColor];
							v = Double.parseDouble(o+"");	
						} catch (Exception nfe) {
							// Ignore any number format exceptions
							// DO NOT CONTINUE, or the calculated value does not get set on the stamp shape
							//continue;
						}
					}
				
					s.setCalculatedValue(v);
					
					if (v == StampImage.IGNORE_VALUE) continue;
					if (v == -Float.MAX_VALUE) continue;   
					//	if (v == 0) continue;   // treat as an ignore value
					if (v>max) max = v;
					if (v<min) min = v;
				}
			
				if (min == Double.MAX_VALUE) {
					min = Double.NaN;
					absMinTF.setText("Undefined");
				} else {
					absMinTF.setText(minFormat.format(min));					
				}
				
				if (max == Double.NEGATIVE_INFINITY) {
					max = Double.NaN;
					absMaxTF.setText("Undefined");
				} else {
					absMaxTF.setText(maxFormat.format(max));					
				}
				
				if (!lockColorRange.isSelected()) {
					setMinValue(min);
					setMaxValue(max);
				}
				
				//to avoid a really bad loop, only recalculate colors if 
				// min and max values are not NaN
				if(!Double.isNaN(min) && !Double.isNaN(max)){
					recalculateColors();
				}
			} catch (Exception e) {
				e.printStackTrace();
				
				// TODO: Do something sensible here
			}
		}
	}
	
	
	public Color[] getColorMap() {
		return mapper.getColorScale().getColorMap();
	}
	
	private double parseTextToDouble(String valStr) {
		double value = Double.NaN;
				
		if (valStr!=null && valStr.length()>0) {
			try {
				value=Double.parseDouble(valStr);
			} catch (Exception e) {
				
			}
		}
		
		return value;	
	}
	
	public void setColumnColorOptions(String newNames[]) {

		//build several different lists of column names
		//color columns
	    ArrayList<String> colorColumns = new ArrayList<String>();
	    //number columns
	    ArrayList<String> numberColumns = new ArrayList<String>();
	    //expression columns --numbers and arrays (ie. emissivity, etc)
	    ArrayList<String> expColumns = new ArrayList<String>();
	    
	    String defaultColumn=myLayer.getParam(myLayer.DEFAULT_COLOR_COLUMN);
		String tipCol=myLayer.getParam(myLayer.TOOLTIP_COLUMN);
		
		//the columns get set initially on the layer before the table is 
		// created.  Once table is created, should access that directly
		// because the columns can change from the multi expression functionality
		boolean initialColumns = true;
		if(myStampTable.tableModel.getColumnCount()> myLayer.getColumnCount()){
			initialColumns = false;
		}
		
	    for (int i=0; i<newNames.length; i++) {
	    	Class columnType;
	    	if(initialColumns){
	    		columnType =  myLayer.getColumnClass(i);
	    	}else{
	    		columnType = myStampTable.tableModel.getColumnClass(i);
	    	}
	    	String name = newNames[i];
			
			if (columnType==null) continue;
			
			if (Number.class.isAssignableFrom(columnType)) {
				colorColumns.add(name);
				numberColumns.add(name);
				expColumns.add(name);
			}
			
			if (Color.class.isAssignableFrom(columnType)) {
				colorColumns.add(name);
			}
			
			if (columnType.isArray()){
				expColumns.add(name+"*");
			}
			//remove expressions from expList to prevent embedded expressions for now
			if(myLayer.getColumnExpression(name)!=null){
				expColumns.remove(name);
			}
	    }
	    
	    colorColumns.remove("Calculated Color");
	    
	    if (mySettings.colorColumn!=null && mySettings.colorColumn.length()>0) {
	    	defaultColumn = mySettings.colorColumn;
	    } else if (defaultColumn!=null && defaultColumn.length()>0) { 
	    	defaultColumn = defaultColumn;  // Yes, itself.
	    } else if (tipCol!=null && tipCol.length()>0) {
	    	defaultColumn = tipCol;
	    }
	    
	    String defaultOrderColumn = defaultColumn;
	    
	    if (mySettings.orderColumn!=null && mySettings.orderColumn.length()>0) {
	    	defaultOrderColumn = mySettings.orderColumn;
	    } else if (defaultColumn!=null && defaultColumn.length()>0) { 
	    	defaultOrderColumn = defaultColumn;
	    } else if (tipCol!=null && tipCol.length()>0) {
	    	defaultOrderColumn = tipCol;
	    }
	    
	    //sort the column lists
	    Collections.sort(colorColumns);
	    Collections.sort(numberColumns);
	    Collections.sort(expColumns);
	    
	    orderDialog.setColumnNames(colorColumns);
		if(orderDialog.getOrderRules().size()==0){
			if(mySettings.orderRules!=null){
				orderDialog.addOrderRules(mySettings.orderRules);
			}
			else if(defaultOrderColumn.length()>0){
				ArrayList<OrderRule> rules = new ArrayList<OrderRule>();
				rules.add(new OrderRule(defaultOrderColumn, mySettings.orderDirection));
				orderDialog.addOrderRules(rules);
			}
		}
		
		//update scatter view
		scatterView.updateColumnArray(numberColumns);
		//update and enable multiexpression dialog
		multiExpDg.updateColumnList(expColumns);
		multiExpBtn.setEnabled(true);
		multiExpBtn.setToolTipText("Opens a dialog for creating and editing expression-based stamp columns");
	    
	    //the next options are only valid for color by column layers
		if (!myLayer.colorByColumn()) return;
	    
	    columnBx.setModel(new DefaultComboBoxModel(colorColumns.toArray()));

	    if (defaultColumn.length()>0) {
	    	columnBx.setSelectedItem(defaultColumn);

	    	// Changing the selected column will overwrite the values in the user's session.  So re-overwrite them to the proper values again
	    	double min = mySettings.colorMin;
	    	double max = mySettings.colorMax;
	    	
	    	if (!Double.isNaN(min)) {
	    		minTF.setText(""+min);
	    	} 
	    	
	    	if (!Double.isNaN(max)) {
	    		maxTF.setText(""+max);
	    	}
	    } 
	    
	    recalculateMinMaxValues();

    	// Changing the selected column will overwrite the values in the user's session.  So re-overwrite them to the proper values again
    	double min = mySettings.colorMin;
    	double max = mySettings.colorMax;
    	
    	if (!Double.isNaN(min)) {
    		minTF.setText(""+min);
    	} 
    	
    	if (!Double.isNaN(max)) {
    		maxTF.setText(""+max);
    	}
	}
		
	public ArrayList<OrderRule> getOrderRules(){
		return orderDialog.getOrderRules();
	}
	
	/**
	 * Returns a StampGroupComparator object representing how the outlines for this stamp layer should be sorted
	 */
	public StampGroupComparator getOrderSort() {
		
		return orderDialog.getStampGroupComparator();
	}
	
	public int getColorColumn() {
		if (columnBx==null) return -1;

		String columnName = (String) columnBx.getSelectedItem();

		int cnt = myLayer.viewToUpdate.myFocus.table.getTableModel().getColumnCount();
			
		for (int i=0 ; i<cnt; i++) {
			String colName=myLayer.viewToUpdate.myFocus.table.getTableModel().getColumnName(i);
			if (colName.equalsIgnoreCase(columnName)) {
				return i;
			}
		}		
		return -1;		
	}
	
	public void dataRefreshed() {
		if (limitToMainViewCBx.isSelected()) {
			updateRecordCount(myLayer.viewToUpdate.stamps.length);
		} else {
			updateRecordCount(myLayer.getVisibleStamps().size());
		}

	}
	
	public CompiledExpression getExpression() {
		return compiledExpression;
	}

	private void updateRecordCount(int newCnt) {
		countLbl.setText(newCnt + recordCountStr);
	}
	
public static final class StampAccessor extends DVMap implements Serializable{
	private static final long serialVersionUID = 1L;
	
	private final StampLayer stampLayer;
	private final StampShape stamp;
	
	/** Creates the name->field mapping for all possible fields. */
	public StampAccessor(StampLayer thisLayer, StampShape stamp) {
		stampLayer = thisLayer;
		this.stamp = stamp;
	}
		
	/**
	 * Called by the compiler to get the variable type, which the JEL
	 * assembler will use to determine which get<Type>Property() method to
	 * call. We fail any type for which this class does not have such a
	 * get<Type>Property() method.
	 */
	public String getTypeName(String name) {
		if (name.equalsIgnoreCase("rownum")) {
			return "Integer";
		}

		int cnt = stampLayer.getColumnCount();
		StampTableModel tableModel = stampLayer.viewToUpdate.getFocusPanel().table.tableModel;
		
		for (int i=0 ; i<cnt; i++) {
			String colName=stampLayer.getColumnName(i);
			if (colName.equalsIgnoreCase(name)) {				
				Class columnType = tableModel.getColumnClass(i);

				if (columnType==null) continue;
				
				if (String.class.isAssignableFrom(columnType)) {
					return "String";
				} else if (Boolean.class.isAssignableFrom(columnType)) {
					return "Boolean";
				} else if (Color.class.isAssignableFrom(columnType)) {
					return "Color";
				} else if (Byte.class.isAssignableFrom(columnType)) {
					return "Byte";
				} else if (Short.class.isAssignableFrom(columnType)) {
					return "Short";
				} else if (Integer.class.isAssignableFrom(columnType)) {
					return "Integer";
				} else if (Long.class.isAssignableFrom(columnType)) {
					return "Long";
				} else if (Float.class.isAssignableFrom(columnType)) {
					return "Float";
				} else if (Double.class.isAssignableFrom(columnType)) {
					return "Double";
				} else if (double[].class.isAssignableFrom(columnType)) {
					return "DoubleArray";
				} else if (float[].class.isAssignableFrom(columnType)) {
					return "DoubleArray";
				} else if (Double[].class.isAssignableFrom(columnType)) {
					return "DoubleArray";
				} else if (Float[].class.isAssignableFrom(columnType)) {
					return "DoubleArray";
				} else if (BigDecimal.class.isAssignableFrom(columnType)) {
					return "BigDecimal";
				} else {
//					System.out.println("type = " + columnType);
					// TODO: We get back an Object, even though it's ultimately a Float
					return "Object";
				}
				
			}
		}	
		return null;
	}
	
	/**
	 * Called by the compiler to convert variable names into field indices,
	 * matching in a case-insensitive way.
	 */
	public Object translate(String name) {
		if (name.equalsIgnoreCase("rownum")) {
			return ROWNUM_COL;
		}
		int cnt = stampLayer.getColumnCount();
		
		for (int i=0 ; i<cnt; i++) {
			String colName=stampLayer.getColumnName(i);
			if (colName.equalsIgnoreCase(name)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Name " + name + " not found");
	}
	
	/**
	 * Called by the evaluator to get the value at the given column
	 * position. We don't optimize access to attributes, beyond the
	 * name->field lookup, because a Feature can contain hundreds of
	 * columns, and the time to optimize will greatly exceed the cost of a
	 * single lookup for a single column, which is probably the common case.
	 */
	public Object getProperty(int column) {
		if (column==ROWNUM_COL) {
			return stampLayer.getStamps().indexOf(stamp);
		}
		Object o = stamp.getData(column);
		return o; 
	}
	public String getStringProperty(int column) {
		return (String)getProperty(column);
	}
	
	// TODO: Return as 1 or 0?
	public Boolean getBooleanProperty(int column) {
		return (Boolean)getProperty(column);
	}
	public Color getColorProperty(int column) {
		return (Color)getProperty(column);
	}
	public Byte getByteProperty(int column) {
		return (Byte)getProperty(column);
	}
	public Short getShortProperty(int column) {
		return (Short)getProperty(column);
	}
	public Integer getIntegerProperty(int column) {
		return (Integer)getProperty(column);
	}
	public Long getLongProperty(int column) {
		return (Long)getProperty(column);
	}
	public Float getFloatProperty(int column) {
		Float f = (Float)getProperty(column);
		if (f==null) return Float.NaN;
		return f;
	}
	public Double getDoubleProperty(int column) {
		Double d = (Double)getProperty(column);
		if (d==null) return Double.NaN;
		return d;
	}
	
	// Any BigDecimals we have as parameters need to be converted into Doubles, because JEL doesn't support BigDecimals
	public Double getBigDecimalProperty(int column) {
		// TODO: Does this test for null need to be done in every one of these methods?  Can the others actually be null?
		Object o = getProperty(column);
		if (o==null) return Double.NaN;
		return(Double)((BigDecimal)o).doubleValue();
	}
	
	public Object getObjectProperty(int column) {
		return (Object)getProperty(column);
	}
	
	public Double[] getDoubleArrayProperty(int column) {
		Double doubles[] = null;

		Object o = getProperty(column);

		if (o==null) {
			doubles = new Double[0];
			return doubles;
		}

		if (o instanceof double[]) {
			double[] vals = (double[])o;
			
			doubles = new Double[vals.length];
			
			for (int i=0; i<vals.length; i++) {
				doubles[i]=vals[i];
			}
			return doubles;
		} 
		if (o instanceof Double[]) {
			Double[] vals = (Double[])o;

			doubles = new Double[vals.length];

			for (int i=0; i<vals.length; i++) {
				doubles[i]=vals[i];
			}
			return doubles;
		}

		if (o instanceof float[]) {
			float[] vals = (float[])o;

			doubles = new Double[vals.length];

			for (int i=0; i<vals.length; i++) {
				doubles[i]=(double)vals[i];
			}
			return doubles;
		} 

		if (o instanceof Float[]) {
			Float[] vals = (Float[])o;

			doubles = new Double[vals.length];

			for (int i=0; i<vals.length; i++) {
				doubles[i]=new Double(vals[i]);
			}
			return doubles;
		}
		else {
			// TODO: Hokey
			//System.out.println("bleh: " + o);
		}
		
		return doubles;
	}
}

	FileFilter textFormatFilter = new FileFilter(){				
		public String getDescription() {
			return "Tab delimited file (.tab, txt)";
		}
		
		public boolean accept(File f) {
			if (f.isDirectory()) return true;
			if (f.getName().endsWith(".txt")) return true;
			if (f.getName().endsWith(".tab")) return true;

			return false;
		}
	};
	
	FileFilter csvFormatFilter = new FileFilter(){				
		public String getDescription() {
			return "CSV files (.csv, .txt)";
		}
		
		public boolean accept(File f) {
			if (f.isDirectory()) return true;
			if (f.getName().endsWith(".csv")) return true;
			if (f.getName().endsWith(".txt")) return true;

			return false;
		}
	};

	
	
	private void writeTableExport(String delimiter, String nullValue, LinkedHashMap<String,String> columnToHeaderNames, boolean exportArrayCount, boolean exportXAxis, boolean saveGeometry){
		boolean selStamps = selStampsRB.isSelected();
		List<StampShape> selStampList = myLayer.getSelectedStamps();
		
		if(exportChooser == null){
		    exportChooser = new JFileChooser(Util.getDefaultFCLocation());
			exportChooser.setDialogTitle("Export Stamp Table");
	        exportChooser.setAcceptAllFileFilterUsed(false);
		}
        
        if (delimiter.equalsIgnoreCase(",")) {
        	exportChooser.setFileFilter(csvFormatFilter);
        } else {
        	exportChooser.setFileFilter(textFormatFilter);
        }
		        
        File selectedFile;
        
        while(true) {
        	int optionChose = exportChooser.showSaveDialog(OutlineFocusPanel.this); 
        	
        	// If the user didn't click the OK button, abort this whole process
        	if (optionChose != JFileChooser.APPROVE_OPTION) {
        		return;
        	}
        	
        	selectedFile = exportChooser.getSelectedFile();

        	if (delimiter.equalsIgnoreCase(",")) {
        		if (!selectedFile.getName().endsWith(".csv")&& !selectedFile.getName().endsWith(".txt")) {
        			selectedFile=new File(selectedFile.getAbsolutePath()+".csv");
        		}
        	}
        	else if (!selectedFile.getName().endsWith(".txt")&& !selectedFile.getName().endsWith(".tab")) {
            	selectedFile=new File(selectedFile.getAbsolutePath()+".txt");
            } 	

            if (selectedFile.exists()) {
            	int overwrite=Util.showConfirmDialog(
                       "File already exists, overwrite?\n" + selectedFile,
                       "FILE EXISTS",
                       JOptionPane.YES_NO_OPTION,
                       JOptionPane.WARNING_MESSAGE
                );
            	
            	if (overwrite == JOptionPane.NO_OPTION) {
            		continue;
            	}
            }
            
            break;
        }
        
        PrintStream fout=null;
        try {
            fout = new PrintStream(new FileOutputStream(selectedFile));
            
            synchronized(myStampTable) {
            	HashMap<String,Integer> maxRecordsPerColumn = new HashMap<String,Integer>();	
            	
                int cols = myStampTable.getColumnCount();
                int rows;
                //if using selected stamps, get number of rows from those
            	if(selStamps){
            		rows = selStampList.size();
            	}
            	//otherwise, get number of rows from stamp table
            	else{
            		rows = myStampTable.getRowCount();
            	}
                
                //if columns are passed in, use those instead
                if(columnToHeaderNames != null){
                	cols = columnToHeaderNames.size();
                }
                
                // Determine how many elements will be exported from each column.  Arrays will expand into multiple columns
                for (int j=0; j<cols; j++) {
                	String columnName;
                	if(columnToHeaderNames == null){
                		columnName = myStampTable.getColumnName(j);
                	}else{
                		columnName = new ArrayList<String>(columnToHeaderNames.keySet()).get(j);
                	}
                	
                	// Start with the assumption that each column is 1 element
                	maxRecordsPerColumn.put(columnName, 1);
                	
                	int colIndex = myLayer.getColumnNum(columnName);
                	
                	if (myStampTable.tableModel.getColumnClass(myLayer.getColumnNum(columnName)).isArray()) {
            			int curMax = maxRecordsPerColumn.get(columnName);
                		for (int i=0; i<rows; i++) {
                			Object dataVal;
                			
                			if(selStamps){
                				dataVal = selStampList.get(i).getData(colIndex);
                			}else{
                				dataVal = myStampTable.getStamp(i).getData(colIndex);
                			}
                			
                			if (dataVal==null) continue;
                			int n = java.lang.reflect.Array.getLength(dataVal);
                			curMax = Math.max(n, curMax);
                		}
            			maxRecordsPerColumn.put(columnName, curMax);
                	}	                	
                }
                
                if (saveGeometry) {
                	fout.print("geometry,Feature:string,");
                }
                
                // Output the header line
                for (int j=0; j<cols; j++) {
                	String columnName;
                	String headerName;
                	if(columnToHeaderNames == null){
                		columnName = myStampTable.getColumnName(j);
                		headerName = columnName;
                	}else{
                		//get the column name
                		columnName = new ArrayList<String>(columnToHeaderNames.keySet()).get(j);
                		//use it to get the header name, which is what we are writing out now
                		headerName = columnToHeaderNames.get(columnName);
                	}

                    if (j>0) {
                    	fout.print(delimiter);
                    }

                	int numValues = maxRecordsPerColumn.get(columnName);
                    if (numValues==1) {
                    	fout.print(headerName);
                    } else {
                    	for (int i=0; i<numValues; i++) {
                    		fout.print(headerName+"["+(i+1)+"]");
                    		fout.print(delimiter);
                    	}
                    	
                    	if(exportArrayCount){
	                    	// Add an extra header column for the number of values exported for this table column
	                    	fout.print(columnName+"SampleCount");
                    	}
                    	
                    	 if (exportXAxis) {
     	                	if(rows>0){
     	                		StampShape stamp = myStampTable.getStamp(0);
     	                		
     	                		double length = stamp.getXValues(columnName).length;
     		                	// Add a column with the number of columns exported for each array
     		            		for (int x=1; x<=length; x++) {
     			                	fout.print("xaxis["+(x)+"]");
     			                	fout.print(delimiter);
     		            		}
     	                	}
     	                }
                    }
                }
                
                fout.print("\n");

                NumberFormat format = NumberFormat.getNumberInstance();
                format.setMaximumFractionDigits(4);

                // Output the data
                for (int i=0; i<rows; i++) {
                	if (saveGeometry) {
                		StampShape stamp;
                		
                		if(selStamps){
                			stamp = selStampList.get(i);
                		}else{
                			stamp = myStampTable.getStamp(i);
                		}
                		
                        //add the detector outlines
                        double stampPoints[] = stamp.getStamp().getPoints();
                        
                        fout.print("\"POLYGON((");
                        
                        for(int idx = 0; idx < stampPoints.length; idx+=2) {
                        	if (idx>0) {
                                fout.print(delimiter);
                        	}
                        	fout.print(format.format(360-stampPoints[idx]));
                        	fout.print(" ");
                        	fout.print(format.format(stampPoints[idx+1]));
                        }
                        
                        fout.print("))\"");
                        fout.print(delimiter);
                        fout.print("polygon");
                        fout.print(delimiter);
                	}
                	
                	
                    for (int j=0; j<cols; j++) {
                    	if (j>0) {
                    		fout.print(delimiter);
                    	}
                    	
                    	//
	                	String columnName;
	                	if(columnToHeaderNames == null){
	                		columnName = myStampTable.getColumnName(j);
	                	}else{
	                		columnName = new ArrayList<String>(columnToHeaderNames.keySet()).get(j);
	                	}

                    	int columnNum = myLayer.getColumnNum(columnName);
                    	StampShape ss;
                		if(selStamps){
                			ss = selStampList.get(i);
                		}else{
                			ss = myStampTable.getStamp(i);
                		}
                    	
                    	Object dataVal = ss.getData(columnNum);
	                	
                    	
	                	//
	                	int numValues = maxRecordsPerColumn.get(columnName);
	                    if (numValues==1) {
	                    	fout.print(dataVal);
	                    } else {
	                    	for (int x=0; x<numValues; x++) {
	                    		Object[] dataArray = (Object[]) dataVal;
	                    		//if the array is empty, use the nullValue for each output
	                    		if(dataArray == null){
	                    			fout.print(nullValue);
	                    		}
	                    		//otherwise check the length and output the real value
	                    		else if(x<dataArray.length){
	                    			fout.print(dataArray[x]);
	                    		}
	                    		fout.print(delimiter);
	                    	}
	                    	if(exportArrayCount){
		                    	// Add an extra data column for the number of values exported for this table column
		                    	fout.print(numValues);
	                    	}
	                    	
		                    if(exportXAxis){
		                    	double[] vals = ss.getXValues(columnName);
		                    	for(int x=0; x<numValues; x++){
		                    		if(x<vals.length){
		                    			fout.print(vals[x]);
		                    		}
		                    		fout.print(delimiter);
		                    	}
		                    }
	                    }
                    }
                    
                    fout.print("\n");
                }
                Util.showMessageDialog("File saved successfully!", "Successful Export", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch(Exception e){
        	if(e instanceof FileNotFoundException){
        		Util.showMessageDialog(	                                              
            	"Unable to open file!\n" + selectedFile, "FILE OPEN ERROR", JOptionPane.ERROR_MESSAGE);
        	}else{
        		Util.showMessageDialog("File export unsuccessful.  Please see log for more information.", "Export Failed", JOptionPane.ERROR_MESSAGE);
        		log.aprint(e);
        	}
        } finally {
        	if (fout!=null) {
        		fout.close();
        	}
        }
	
	}
	
	class SaveAction extends AbstractAction {
		private String delimiter;
		private String nullVal = "";
		private LinkedHashMap<String,String> columnToHeaderNames;
        boolean exportArrayCount = true;
        boolean exportXAxis = false;
		
		SaveAction(String windowText) {
			this(windowText, "\t");
		}
		
		SaveAction(String windowText, String delimiterToUse) {
			this(windowText, delimiterToUse, null, true, false);
		}
		
		SaveAction(String windowText, String delimiterToUse, LinkedHashMap<String,String> columnsAndHeaders, boolean arrayCount, boolean xaxis){
			super(windowText);
			delimiter = delimiterToUse;
			columnToHeaderNames = columnsAndHeaders;
			exportArrayCount = arrayCount;
			exportXAxis = xaxis;
		}
		
		  
		public void actionPerformed(ActionEvent e){
			writeTableExport(delimiter, nullVal, columnToHeaderNames, exportArrayCount, exportXAxis, exportGeometry.isSelected());
		}
	}
	
	public void recalculateColors() {
		ArrayList<StampShape> stamps = myLayer.getStamps();
		
		if (stamps==null) {
			return;
		}

		for (StampShape s : stamps) {
			s.setCalculatedColor(null);
		}
		
		Color colors[] = getColorMap();
		
		double min = getMinValue();
		double max = getMaxValue();
		
		if (Double.isNaN(min)||Double.isNaN(max)) {
			return;
		}
		
		for (StampShape s : stamps) {
			double val = s.getCalculatedValue();
			float colorVal = (float)((val-min)/(max-min));
			
			// If max and min are the same, we divide by 0 and get a NaN above.  Compensate for that case here.
			if (max<=min) {
				colorVal = 0.5f;  // Arbitrarily colorize everything as the middle of the range
			}
			
			// Make outliers transparent?  Offer an option?
			if (colorVal<0) {
				colorVal=0.0f;
				if (hideOutofRange()) {
					continue;
				}
			}
			if (colorVal>1) {
				colorVal=1.0f;
				if (hideOutofRange()) {
					continue;
				}
			}
			
			if (Double.isNaN(colorVal)) {
				continue;
			}	
			
			// TODO: Add one to make colors match the old TES layer...
			int colorInt = (int)(colorVal * 255)+1;
			
			if (colorInt>255) colorInt=255;
			
			Color color = colors[colorInt];

			s.setCalculatedColor(color);
		}		
	}
	
	private void updateSettings(){
		mySettings.colorMin=getMinValue();
		mySettings.colorMax=getMaxValue();
		if(scaleTF != null && scaleTF.getText().length()>0){
			mySettings.setOriginMagnitude(Double.parseDouble(scaleTF.getText()));
		}
		if(hideOutofRangeCBx != null){
			mySettings.hideValuesOutsideRange=hideOutofRangeCBx.isSelected();
		}
	}
	
	
	void refreshViews() {
		myLayer.increaseStateId(StampLayer.OUTLINES_BUFFER);
		StampLView viewToUpdate = myLayer.viewToUpdate;
		StampLView childLView = (StampLView)viewToUpdate.getChild();
		viewToUpdate.clearLastOutlines();
		viewToUpdate.drawOutlines();
		
		childLView.clearLastOutlines();
		childLView.drawOutlines();
		
		myStampTable.repaint();
		
		if (ThreeDManager.isReady()) {
			//update the 3d view if has lview3d enabled
			LView3D view3d = viewToUpdate.getLView3D();
			if(view3d.isEnabled()){
				ThreeDManager mgr = ThreeDManager.getInstance();
				//If the 3d is already visible, update it
				if(view3d.isVisible()){
					mgr.updateDecalsForLView(viewToUpdate, true);
				}
			}
		}		
	}
	
	/**
	 * Update the stamp table by either adding or removing a column for
	 * an expression.  Also update the color combobox and the 
	 * MultiExpressionDialog with the updated column names
	 * @param newColName  The name of the expression column
	 * @param removeCol  If true, remove the column, if false, add the column
	 */
	public void updateColumns(String newColName, boolean removeCol){
		StampTableModel model = myStampTable.getTableModel();
		Class[] curColClasses = model.getColumnClasses();
		String[] curColNames = model.getColumnNames();
		
		//get the current visible columns to be able use for the update
		FilteringColumnModel colModel = (FilteringColumnModel) myStampTable.getColumnModel();
		String[] curVisibleCols = colModel.getVisibleColumnNames();

		
		String[] visibleCols = curVisibleCols;
		Class[] colClasses;
		String[] colNames;
		
		int index = 0;
		if(removeCol){
			colClasses = new Class[curColClasses.length-1];
			colNames = new String[curColNames.length-1];
			
			for(int i=0; i<curColNames.length; i++){
				if(curColNames[i].equals(newColName)){
					continue;
				}else{
					colClasses[index] = curColClasses[i];
					colNames[index] = curColNames[i];
					index++;
				}
			}
		}
		else{
			//create the new Class array entries
			Class[] newColClasses = {Double.class};
			visibleCols = ArrayUtils.addAll(curVisibleCols, newColName);
			//create total new column name and class arrays
			colClasses = ArrayUtils.addAll(curColClasses, newColClasses);
			colNames = ArrayUtils.addAll(curColNames, newColName);
		}
		
		myStampTable.updateData(colClasses, colNames, visibleCols, true);
		setColumnColorOptions(colNames);
		myStampTable.repaint();
	}

	
}

