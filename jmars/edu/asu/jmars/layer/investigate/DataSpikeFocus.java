package edu.asu.jmars.layer.investigate;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import edu.asu.jmars.Main;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeChart;
import edu.asu.jmars.util.CSVFilter;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.asu.jmars.swing.ColorCombo;
import edu.asu.jmars.layer.InvestigateData;


public class DataSpikeFocus extends JPanel{
	//Attributes
	private InvestigateLView myLView;
	private InvestigateLayer myLayer;
	private InvestigateFocus myFP;
	private DataSpike myDS;
	private JFreeChart myChart;
	private double lat;
	private double lon;
	//GUI components
	private JPanel chartPnl;
	private JPanel eastPnl;
	private ChartPanel cPnl;
	private ChartReadOutTable chartTbl;
	private ChartReadOutTableModel chartModel;
	private JButton exChartBtn;
	private JLabel shapeLbl;
	private JLabel outlineLbl;
	private JLabel fillLbl;
	private JLabel colorLbl;
	private JLabel sizeLbl;
	private JComboBox shapeBx;
	private ColorCombo oColorBx;
	private ColorCombo fColorBx;
	private ColorCombo lColorBx;
	private JComboBox sizeBx;
	private JCheckBox labelChk;
	private JCheckBox markerChk;
	private JButton renameBtn;
	private JButton delBtn;
	private JButton exShapeBtn;
	private JButton exCSVBtn;
	private String[] shapeArr = {DataSpike.CIRCLE_STYLE, DataSpike.SQUARE_STYLE}; //TODO: add more shapes
	private Integer[] sizeArr = {10,11,12,13,14,15,16,17,18,19,20};
	
	public DataSpikeFocus(DataSpike ds, InvestigateLView lview){
		myLView = lview;
		myLayer = (InvestigateLayer)myLView.getLayer();
		myFP = (InvestigateFocus)lview.getFocusPanel();
		myDS = ds;
		myChart = ds.getChart();
		lat = myDS.getPoint().getY();
		lon = 360 - myDS.getPoint().getX();
		//TODO: limit decimals to 3 or 4 digits?
		layoutContents();
		ThemeChart.configureUI(myChart);
		formatChart();
	}
	
	
	private void formatChart(){
		XYItemRenderer rr = ((XYPlot) myChart.getPlot()).getRenderer();
		rr.setSeriesPaint(0, ThemeChart.getPlotColor());
		myChart.getXYPlot().setDomainCrosshairVisible(true);
		myChart.getXYPlot().setDomainCrosshairPaint(ThemeChart.getIndicatorColor());
		myChart.getXYPlot().setDomainCrosshairStroke(new BasicStroke(1.0f));
		myChart.getXYPlot().setDomainCrosshairLockedOnData(false);
		//mouse listener for readout table
		cPnl.addChartMouseListener(new ChartMouseListener() {
			public void chartMouseMoved(ChartMouseEvent event) {
				chartMouseMovedEventOccurred(event);
			}
			public void chartMouseClicked(ChartMouseEvent event) {
			}
		});		
	}

	public void chartMouseMovedEventOccurred(ChartMouseEvent e){
		Point2D pt = cPnl.translateScreenToJava2D(e.getTrigger().getPoint());
		XYPlot xyPlot = myChart.getXYPlot();
		Double indexDouble = xyPlot.getDomainAxis().java2DToValue(
					pt.getX(), 
					cPnl.getChartRenderingInfo().getPlotInfo().getDataArea(), 
					xyPlot.getDomainAxisEdge());
		//average between values (value 2 starts halfway through value 1)
		indexDouble += 0.5;
		int index = indexDouble.intValue();
		ArrayList<Double> sampleData = new ArrayList<Double>();
		ArrayList<String> ids = new ArrayList<String>();
		ArrayList<String> units = new ArrayList<String>();
		for(InvestigateData id : myDS.getInvData()){
			//Continue if the investigate data is not numeric
			if(id.getNumValSize()<=2){
				continue;
			}
			//In case one data set is longer than another,
			// don't try and set the new dataset to an index that is too
			// large, display value unavailable.
			if(index<id.getNumValSize() && index>-1){
				sampleData.add(id.getNumValue(index));
				ids.add(id.getNumKey(index));
				String unit = id.getNumUnit(index);
				if(unit == null){
					units.add("Not Avail.");
				}else{
					units.add(unit);
				}
			}else{
				sampleData.add(Double.NaN);
				ids.add("Not Avail.");
				units.add("Not Avail.");
			}
		}
		//set the chart crosshair
		xyPlot.setDomainCrosshairValue(index);
		//set the table value
		chartModel.setSampleData(sampleData, ids, units);
	}
	
	
	private void layoutContents(){
		setLayout(new BorderLayout());
		
	//Chart JPanel (Center)
		//center panel
		chartPnl = new JPanel();
		chartPnl.setBorder(new TitledBorder("Chart"));
		chartPnl.setLayout(new BorderLayout());
		String title = myDS.getName()+" - Location:("+lat+"N, "+lon+"E)";
		myChart.setTitle(title);		
		cPnl = new ChartPanel(myDS.getChart(), true);
		
		//Middle
		JLabel chartOpts = new JLabel("~~To see more options, please right click on chart.~~");
		chartOpts.setHorizontalTextPosition(JLabel.CENTER);
		Font optsFont = ThemeFont.getBold().deriveFont(15f);
		chartOpts.setFont(optsFont);
		
		//south panel
		ArrayList<String> titles = new ArrayList<String>();
		ArrayList<Double> data = new ArrayList<Double>();
		ArrayList<String> ids = new ArrayList<String>();
		ArrayList<String> units = new ArrayList<String>();
		for(InvestigateData id : myDS.getInvData()){
			//dont add non-numeric layers to the table
			if(id.getNumValSize()<3){
				continue;
			}
			titles.add(id.name);
			data.add(Double.NaN);
			ids.add("");
			units.add("");
		}
		chartModel = new ChartReadOutTableModel(titles, myChart);
		chartModel.setSampleData(data, ids, units);
		chartTbl = new ChartReadOutTable(chartModel);
		chartTbl.setRowSelectionAllowed(false);
		chartTbl.setPreferredScrollableViewportSize(chartTbl.getPreferredSize());
		chartTbl.setFillsViewportHeight(true);
		JScrollPane tableSP = new JScrollPane(chartTbl, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, 
									ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);

		JPanel cBotPnl = new JPanel();
		cBotPnl.setLayout(new BoxLayout(cBotPnl, BoxLayout.PAGE_AXIS));
		cBotPnl.setBorder(new EmptyBorder(10, 10, 0, 10));
		cBotPnl.add(chartOpts);
		cBotPnl.add(Box.createVerticalStrut(10));
		cBotPnl.add(tableSP);
		cBotPnl.add(Box.createVerticalStrut(10));

		chartPnl.add(cPnl, BorderLayout.CENTER);
		chartPnl.add(cBotPnl, BorderLayout.SOUTH); 
		
	//Settings And Export (East)
		eastPnl = new JPanel();
		eastPnl.setLayout(new BorderLayout());
		eastPnl.setBorder(new EmptyBorder(15,0,0,0));		
		
		//display panel (top)
		JPanel displayPnl = new JPanel();
		displayPnl.setBorder(new TitledBorder("Display Settings"));
		FormLayout dispLayout = new FormLayout("2dlu, right:pref, pref, 5dlu right:pref, 40dlu, 2dlu",  //columns
												"5dlu, pref, 5dlu, pref, 4dlu, pref, 12dlu, pref, 5dlu, pref, 4dlu, pref, 5dlu");//rows
		displayPnl.setLayout(dispLayout);
		
		JLabel markerLbl = new JLabel("<HTML><U><B>Marker</B></U></HTML>");
		
		shapeLbl = new JLabel("Style: ");
		shapeBx = new JComboBox<String>(shapeArr);
		shapeBx.setSelectedItem(myDS.getShapeStyle());
		shapeBx.addActionListener(boxListener);
		
		markerChk = new JCheckBox("Show Marker");
		markerChk.setSelected(myDS.isMarkerOn());
		markerChk.addActionListener(hideListener);
		
		outlineLbl = new JLabel("Outline: ");
		oColorBx = new ColorCombo(1);
		oColorBx.setColor(myDS.getOutlineColor());
		oColorBx.addActionListener(boxListener);
		
		fillLbl = new JLabel("Fill: ");
		fColorBx = new ColorCombo(1);
		fColorBx.setColor(myDS.getFillColor());
		fColorBx.addActionListener(boxListener);
		
		JLabel labelLbl = new JLabel("<HTML><U><B>Label</B></U></HTML>");
		
		sizeLbl = new JLabel("Size: ");
		sizeBx = new JComboBox(sizeArr);
		sizeBx.setSelectedItem(myDS.getLabelSize());
		sizeBx.addActionListener(boxListener);
		//--
		labelChk = new JCheckBox("Show Label");
		labelChk.setSelected(myDS.isLabelOn());
		labelChk.addActionListener(hideListener);
		//--
		colorLbl = new JLabel("Color: ");
		lColorBx = new ColorCombo(1);
		lColorBx.setColor(myDS.getLabelColor());
		lColorBx.addActionListener(boxListener);
		//--

		//add to display panel
		CellConstraints cc = new CellConstraints();
		displayPnl.add(markerLbl, cc.xyw(2, 2, 5, CellConstraints.CENTER, CellConstraints.CENTER));
		displayPnl.add(shapeLbl, cc.xy(2, 4));
		displayPnl.add(shapeBx, cc.xy(3, 4));
		displayPnl.add(markerChk, cc.xyw(5,4,2));
		displayPnl.add(outlineLbl, cc.xy(2,6));
		displayPnl.add(oColorBx, cc.xy(3,6));
		displayPnl.add(fillLbl, cc.xy(5,6));
		displayPnl.add(fColorBx, cc.xy(6,6));
		displayPnl.add(labelLbl, cc.xyw(2, 8, 5, CellConstraints.CENTER, CellConstraints.CENTER));
		displayPnl.add(sizeLbl, cc.xy(2, 10));
		displayPnl.add(sizeBx, cc.xy(3, 10));
		displayPnl.add(labelChk, cc.xyw(5, 10, 2));
		displayPnl.add(colorLbl, cc.xy(2, 12));
		displayPnl.add(lColorBx, cc.xy(3, 12));
		
		
		//export panel (middle)
		JPanel exportPnl = new JPanel();
		exportPnl.setBorder(new TitledBorder("Export Options"));
		exportPnl.setLayout(new GridLayout(3,1));
		exCSVBtn = new JButton(exCSVAct);		
		exChartBtn = new JButton("Export Chart".toUpperCase()); //TODO: write an exChartAct!
		exChartBtn.setEnabled(false);
		exShapeBtn = new JButton("Export Shape".toUpperCase()); //TODO: exShapeAct
		exShapeBtn.setEnabled(false);
		JPanel ex1 = new JPanel();
		ex1.add(exCSVBtn);
		JPanel ex2 = new JPanel();
		ex2.add(exChartBtn);
		JPanel ex3 = new JPanel();
		ex3.add(exShapeBtn);
		exportPnl.add(ex1);
		exportPnl.add(ex2);
		exportPnl.add(ex3);
		//bottom pnl
		JPanel dataspikePnl = new JPanel();
		dataspikePnl.setBorder(new TitledBorder("DataSpike Options"));
		dataspikePnl.setLayout(new GridLayout(2,1));
		delBtn = new JButton(delAct);
		renameBtn = new JButton(renameAct);
		JPanel ds1 = new JPanel();
		ds1.add(renameBtn);
		JPanel ds2 = new JPanel();
		ds2.add(delBtn);
		dataspikePnl.add(ds1);
		dataspikePnl.add(ds2);		
		
		//center panel (mid and bot)
		JPanel eastCenPnl = new JPanel();
		eastCenPnl.setLayout(new BoxLayout(eastCenPnl, BoxLayout.PAGE_AXIS));
		eastCenPnl.add(Box.createVerticalStrut(20));
		eastCenPnl.add(exportPnl);
		eastCenPnl.add(Box.createVerticalStrut(20));
		eastCenPnl.add(dataspikePnl);
		eastCenPnl.add(Box.createVerticalStrut(20));
		
		//Add to east
		eastPnl.add(displayPnl, BorderLayout.NORTH);
		eastPnl.add(eastCenPnl, BorderLayout.CENTER);
		
		
		add(chartPnl, BorderLayout.CENTER);
		add(eastPnl, BorderLayout.EAST);
	}
	
	
	Action renameAct = new AbstractAction("Rename DataSpike".toUpperCase()){
		public void actionPerformed(ActionEvent e) {
			String name = (String) Util.showInputDialog("DataSpike Name:","Rename DataSpike", JOptionPane.INFORMATION_MESSAGE, null, null, myDS.getName());
			//if canceled name is null...just return 
			if(name == null){
				return;
			}
			for(DataSpike ds : myLayer.getDataSpikes()){
				if (name.equals(ds.getName())){
					Util.showMessageDialog("That name is already in use.\nPlease choose another.","Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
			}
			
			//rename data spike object
			myDS.setName(name);
			//reset chart name
			String title = myDS.getName()+" - Location:("+lat+"N, "+lon+"E)";
			myChart.setTitle(title);			
			//reset tab name
			myFP.renameTab(DataSpikeFocus.this, name);
			//redraw lview (reset label)
			myLView.repaint();
			
			//update 3d
			myLayer.increaseStateId(InvestigateLayer.LABELS_BUFFER);
			if(ThreeDManager.isReady()){
				ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
			}
		}
	};
	
	
	Action delAct = new AbstractAction("Delete DataSpike".toUpperCase()){
		public void actionPerformed(ActionEvent e) {
			int response = Util.showConfirmDialog("Are you sure you would like to delete DataSpike '"+myDS.getName()+"'?",
														"Delete DataSpike",JOptionPane.YES_NO_OPTION);
			if(response == JOptionPane.YES_OPTION){
				myLayer.getDataSpikes().remove(myDS);
				myLView.repaint();
				myFP.setSelectedIndex(0);
				myFP.removeTabByComponent(DataSpikeFocus.this);
				
				//update 3d
				myLayer.increaseStateId(InvestigateLayer.IMAGES_BUFFER);
				myLayer.increaseStateId(InvestigateLayer.LABELS_BUFFER);
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
				}
			}
		}
	};
	
	
	Action exCSVAct = new AbstractAction("Export CSV".toUpperCase()){
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser(Util.getDefaultFCLocation());			
			CSVFilter filter = new CSVFilter();
			fc.setFileFilter(filter);
			int returnVal = fc.showSaveDialog(Main.mainFrame);
			if (returnVal == JFileChooser.APPROVE_OPTION){
				String fileName = fc.getSelectedFile().getPath();
				if (!fileName.contains(".csv")){
					fileName+=".csv";
				}	
				writeCSV(fileName);
			}
			
		}
		
	};
	
	// Cycles through investigate data and prints out a csv	
	private void writeCSV(String saveFile){
		try{
			FileWriter wr = new FileWriter(saveFile);
			wr.append("Name, Value\n");
			for(int i=0; i<myDS.getInvData().size(); i++){
				wr.append(myDS.getInvData().get(i).getCSVDump());
				wr.append("\n");
			}
			wr.flush();
			wr.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}


	private ActionListener hideListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			int imageState = myLayer.getStateId(InvestigateLayer.IMAGES_BUFFER);
			int labelState = myLayer.getStateId(InvestigateLayer.LABELS_BUFFER);

			
			if(e.getSource() == labelChk){
				//only set boolean and trigger state change if different
				boolean sel = labelChk.isSelected();
				if(sel != myDS.isLabelOn()){
					myDS.setLabel(sel);
					//update proper state id buffer
					myLayer.increaseStateId(InvestigateLayer.LABELS_BUFFER);
				}
			}else if(e.getSource() == markerChk){
				boolean sel = markerChk.isSelected();
				if(sel != myDS.isMarkerOn()){
					myDS.setMarkerShow(sel);
					//update proper state id buffer
					myLayer.increaseStateId(InvestigateLayer.IMAGES_BUFFER);
				}
			}
			//refresh lview
			myLView.repaint();

			//update 3d if something changed
			if(myLayer.getStateId(InvestigateLayer.IMAGES_BUFFER)!=imageState || myLayer.getStateId(InvestigateLayer.LABELS_BUFFER)!=labelState){
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
				}
			}
		}
	};
	
	
	private ActionListener boxListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			int imageState = myLayer.getStateId(InvestigateLayer.IMAGES_BUFFER);
			int labelState = myLayer.getStateId(InvestigateLayer.LABELS_BUFFER);
			//outline color
			if(e.getSource() == oColorBx){
				//only set the color and trigger state change if it's different
				Color c = oColorBx.getColor();
				if(c != myDS.getOutlineColor()){
					myDS.setOutlineColor(c);
					//update proper state id buffer
					myLayer.increaseStateId(InvestigateLayer.IMAGES_BUFFER);
				}
			}
			//fill color
			if(e.getSource() == fColorBx){
				//only set the color and trigger state change if it's different
				Color c = fColorBx.getColor();
				if(c != myDS.getFillColor()){
					myDS.setFillColor(c);
					//update proper state id buffer
					myLayer.increaseStateId(InvestigateLayer.IMAGES_BUFFER);
				}
			}
			//label color
			if(e.getSource() == lColorBx){
				//only set the color and trigger state change if it's different
				Color c = lColorBx.getColor();
				if(c != myDS.getLabelColor()){
					myDS.setLabelColor(c);
					//update proper state id buffer
					myLayer.increaseStateId(InvestigateLayer.LABELS_BUFFER);
				}
			}
			//data spike shape
			if(e.getSource() == shapeBx){
				int shape = shapeBx.getSelectedIndex();
				
				//Circle
				if(shape == 0 && myDS.getShapeStyle( )!= DataSpike.CIRCLE_STYLE){
					myDS.setShapeStyle("Circle");
					//update proper state id buffer
					myLayer.increaseStateId(InvestigateLayer.IMAGES_BUFFER);
				}//Square
				else if(shape == 1 && myDS.getShapeStyle() != DataSpike.SQUARE_STYLE){
					myDS.setShapeStyle("Square");
					//update proper state id buffer
					myLayer.increaseStateId(InvestigateLayer.IMAGES_BUFFER);
				}
			}
			//label size
			if(e.getSource() == sizeBx){
				int size = sizeBx.getSelectedIndex();
				if(size != myDS.getLabelSize()){
					myDS.setLabelSize(size);
					//update proper state id buffer
					myLayer.increaseStateId(InvestigateLayer.LABELS_BUFFER);
				}
			}
			
			
			myLView.repaint();

			//update 3d if something changed
			if(myLayer.getStateId(InvestigateLayer.IMAGES_BUFFER)!=imageState || myLayer.getStateId(InvestigateLayer.LABELS_BUFFER)!=labelState){
				if(ThreeDManager.isReady()){
					ThreeDManager.getInstance().updateDecalsForLView(myLView, true);
				}
			}
		}
	};
}
