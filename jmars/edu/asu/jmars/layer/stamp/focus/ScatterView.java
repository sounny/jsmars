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
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.StampLayer.StampSelectionListener;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeChart;

public class ScatterView extends JPanel  implements StampSelectionListener {
	
	private StampLView myLView;
	private StampLayer myLayer;
	
	// JFreeChart stuff
	private JFreeChart chart;
	private ChartPanel chartPanel;
	
	private JFrame myFrame;
	
	private JComboBox<String> xAxisBx;
	private String[] columnNames;
	private JButton addYBtn;
	private ArrayList<YAxisSelection> selectedYs;
	private JPanel yDisplayPnl;
	private GridBagConstraints yGBC;
	private YAxesDialog yDialog;
	
    private int row = 0;
    private int pad = 1;
    private Insets in = new Insets(pad,pad,pad,pad);


    public ScatterView(StampLView stampLView){
    	myLView = stampLView;
    	myLayer = (StampLayer)myLView.getLayer();
    	selectedYs = new ArrayList<YAxisSelection>();
    	yDialog = new YAxesDialog();
    	
    	myLayer.addSelectionListener(this);
    	
    	buildUI();
    }

    private void buildUI(){
    	//create the chart
    	chart = ChartFactory.createScatterPlot(
    			null,					//Title
    			"",						//x-axis label
    			"",						//y-axis label
    			new XYSeriesCollection(),	//dataset
    			PlotOrientation.VERTICAL,	//orientation
    			false,					//legend
    			true,					//tooltips
    			false);					//urls
    	ThemeChart.configureUI(chart);
		XYItemRenderer rr = ((XYPlot) chart.getPlot()).getRenderer();
		rr.setSeriesPaint(0, ThemeChart.getPlotColor());
		
		chartPanel = new ChartPanel(chart, true);
		chartPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		
		
		chartPanel.addChartMouseListener(new ChartMouseListener() {
			
			@Override
			public void chartMouseMoved(ChartMouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void chartMouseClicked(ChartMouseEvent arg0) {
				ChartEntity ce = arg0.getEntity();
				
				if (ce instanceof XYItemEntity) {
					
					XYItemEntity item = (XYItemEntity)ce;
					
					XYDataset dataset = item.getDataset();
					int seriesIndex = item.getSeriesIndex();
					int itemIndex = item.getItem();

					// You have the dataset the data point belongs to, the index of the series in that dataset of the data point, and the specific item index in the series of the data point.
					XYSeries series = ((XYSeriesCollection)dataset).getSeries(seriesIndex);
					XYDataItem xyItem = series.getDataItem(itemIndex);
										
					StampShape ss = data2stamp.get(xyItem);
					if (ss==null) {
						return;
					}
					
					if (arg0.getTrigger().isControlDown()) {
						myLayer.toggleSelectedStamp(ss);
					} else {
						myLayer.clearSelectedStamps();
						myLayer.addSelectedStamp(ss);
					}
				} 
				
				updatePlot();
			}
		});
		
		//x-axis dropdown
		JLabel xColLbl = new JLabel("x-axis: ");
		xAxisBx = new JComboBox<String>();
		xAxisBx.addActionListener(xListener);
		JPanel xPnl = new JPanel();
		xPnl.add(xColLbl);
		xPnl.add(xAxisBx);
		
		//y-axis display
		addYBtn = new JButton(addYAct);
		yDisplayPnl = new JPanel();
		yDisplayPnl.setBorder(new TitledBorder("Y-Axes"));
		yGBC = new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, in, pad, pad);
		// put the add button in a panel so it's the same height as the yaxissselctions
		JPanel ybtnPnl = new JPanel();
		ybtnPnl.add(addYBtn);
		yDisplayPnl.add(ybtnPnl, yGBC);
		
		JPanel botPnl = new JPanel(new GridBagLayout());
		botPnl.setBorder(new EmptyBorder(5, 10, 10, 10));
		row = 0;
		botPnl.add(xPnl, new GridBagConstraints(0, row, 2, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, in, pad, pad));
		row++;
		botPnl.add(yDisplayPnl, new GridBagConstraints(0, row, 2, 1, 1, 0, GridBagConstraints.CENTER, pad, in, pad, pad));
		
		this.setLayout(new BorderLayout());
		this.add(chartPanel, BorderLayout.CENTER);
		this.add(botPnl, BorderLayout.SOUTH);
    }
    
    private AbstractAction addYAct = new AbstractAction("Add y-axis") {
		public void actionPerformed(ActionEvent e) {
			yDialog.showDialog();
		}
	};
    
    public void updateColumnArray(ArrayList<String> validNames){
    	String selectedItem = (String)xAxisBx.getSelectedItem();
    	columnNames = new String[validNames.size()];
    	int i=0;
    	for (String name : validNames){
    		columnNames[i++] = name;
    	}
    	
    	xAxisBx.setModel(new DefaultComboBoxModel<String>(columnNames));
    	
    	if (validNames.contains(selectedItem)) {
    		xAxisBx.setSelectedItem(selectedItem);
    	}
    }
    
    private void refreshYPanel(){
    	//refresh the plot
    	rebuildPlot();
    	
    	//remove everything
    	yDisplayPnl.removeAll();
    	//reset the column on the gridbagconstraints
    	yGBC.gridx = 0;
    	//add the add button
		// put the add button in a panel so it's the same height as the yaxissselctions
		JPanel ybtnPnl = new JPanel();
		ybtnPnl.add(addYBtn);
		yDisplayPnl.add(ybtnPnl, yGBC);
    	
    	//cycle through y-selections and add their panel
    	for(YAxisSelection y : selectedYs){
    		yGBC.gridx++;
    		yDisplayPnl.add(y.getDisplayPanel(), yGBC);
    	}
    	
    	//TODO: test which of these are needed
//    	yDisplayPnl.revalidate();
    	yDisplayPnl.repaint();
    }

    private ActionListener xListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			rebuildPlot();
		}
	};
	
	HashMap<XYDataItem, StampShape> data2stamp = new HashMap<XYDataItem, StampShape>();
	
	private XYSeriesCollection rebuildDataSet(int xIndex) {
		XYSeriesCollection data_series = new XYSeriesCollection();

		for(YAxisSelection y : selectedYs){
			int yIndex = myLayer.getColumnNum(y.getColumnName());

			XYSeries data = new XYSeries(y.getColumnName());
			XYSeries selected_data = new XYSeries("selected " + y.getColumnName());
			
			//cycle through all the stamps to create data entries
			for(StampShape ss : myLayer.getStamps()){
				Object xObj = ss.getData(xIndex);
				Object yObj = ss.getData(yIndex);
				//if either the x or y is null, skip this data point
				if(xObj == null || yObj == null){
					continue;
				}

				double xVal = Double.valueOf(xObj.toString());
				double yVal = Double.valueOf(yObj.toString());

				XYDataItem item = new XYDataItem(xVal, yVal);
				data2stamp.put(item, ss);

				if (myLayer.isSelected(ss)) {
					selected_data.add(item);
				} else {
					data.add(item);
				}
			}
			
			//add the data series
			data_series.addSeries(selected_data);	
			data_series.addSeries(data);
		}
		
		return data_series;
	}
	
	public void updatePlot() {
		if(xAxisBx.getSelectedIndex() != 0 && selectedYs.size()>0){
			String xLabel = xAxisBx.getSelectedItem().toString();
			
			//get the stamp column indices for the x and y values
			int xIndex = myLayer.getColumnNum(xLabel);
			
			chart.getXYPlot().setDataset(rebuildDataSet(xIndex));
		}
	}
	
	private void rebuildPlot(){
		String title = "";
		String xLabel = "";
		String yLabel = "";
		
		data2stamp.clear();
		
		XYSeriesCollection data_series = null;
		
		//if the x axis isn't chosen or no y-axes are chosen, don't any more work
		if(xAxisBx.getSelectedIndex() != 0 && selectedYs.size()>0){
			xLabel = xAxisBx.getSelectedItem().toString();
			
			//if just one y, set the title appropriately
			if(selectedYs.size() == 1 ){
				yLabel = selectedYs.get(0).getColumnName();
				title = yLabel +" vs. "+ xLabel + " Scatter Plot";
			}else{
				title = "Multi-Data vs. "+ xLabel+" Scatter Plot";
			}
			
			//get the stamp column indices for the x and y values
			int xIndex = myLayer.getColumnNum(xLabel);
			
			data_series = rebuildDataSet(xIndex);
		} 
		
		//recreate the chart
        chart = ChartFactory.createScatterPlot(
        		title,				//title
        		xLabel,		 		//x axis label
        		yLabel,
        		data_series, 		//data
        		PlotOrientation.VERTICAL,	//orientation
        		false,				//legend
        		true, 				//tooltips
        		false);				//urls
        
        ThemeChart.configureUI(chart);
        XYPlot plot = chart.getXYPlot();
		XYItemRenderer rr = plot.getRenderer();
		
		Color unselColor = ThemeChart.getPlotColor();
		Color inverseColor = new Color(getContrastVersionForColor(unselColor));
		
		ThemeChart.getIndicatorColor();
		
		// Selections are first, so they're drawn on top
		rr.setSeriesPaint(0, inverseColor);
		rr.setSeriesPaint(1, unselColor);
					 
		//set the colors for the yselections based off 
		// the colors generated by the plot
		// also set the proper y-axes for each series
		for (int i=0; i<plot.getSeriesCount(); i++){
			//TODO: this doesn't quite work yet
			//set the y-axis 
//			plot.setRangeAxis(yAxes.get(i));
//			plot.mapDatasetToRangeAxis(i, i);
			
			//set the color for the YAxisSelection
			String key = plot.getDataset().getSeriesKey(i).toString();
			for(YAxisSelection y : selectedYs){
				if(key.equalsIgnoreCase(y.getColumnName())){
					Color c = (Color)rr.getItemPaint(i, 0);
					y.setColor(c);
					break;
				}
			}
		}
		
		
        chartPanel.removeAll();
        chartPanel.setChart(chart);
        
	}
    
	
	/**
	 * Dispose any frame
	 */
	public void cleanUp(){
		if(myFrame!=null){
			myFrame.dispose();
		}
	}
	
	public void showInFrame(){
		//create the frame if it hasn't been created
		if(myFrame == null){
			myFrame = new JFrame();
			myFrame.setTitle("Scatter Viewer");
			myFrame.setContentPane(this);
			
			//set the location relative to the focus panel but centered vertically
			Point pt = myLView.getFocusPanel().getLocationOnScreen();
			Dimension prefSize = this.getPreferredSize();
			myFrame.setLocation(pt.x + (int)prefSize.getWidth()/4, pt.y + (int)prefSize.getHeight()/4);
			
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
	
	
	
	
	private class YAxisSelection{
		private String column;
		private JButton delBtn;
		private Color myColor;
		private JPanel displayPnl;
		
		YAxisSelection(String colName){
			column = colName;
//			myColor = Color.BLACK;
		}
		
		String getColumnName(){
			return column;
		}
		
		Color getColor(){
			return myColor;
		}
		
		JPanel getDisplayPanel(){
			if(displayPnl == null){
				buildDisplayPanel();
			}
			return displayPnl;
		}
		
		void setColor(Color color){
			myColor = color;
			//reset the display panel if the color
			// changes after the display is built
			displayPnl = null;
		}
		
		private void buildDisplayPanel(){
			displayPnl = new JPanel();
			JLabel colLbl = new JLabel(column);
			colLbl.setForeground(myColor);
			delBtn = new JButton(delAct);
			
			displayPnl.add(colLbl);
			displayPnl.add(delBtn);
		}
		
		private AbstractAction delAct = new AbstractAction("X") {
			public void actionPerformed(ActionEvent arg0) {
				selectedYs.remove(YAxisSelection.this);
				refreshYPanel();
			}
		};
	}
	
	class YAxesDialog extends JDialog{
		private JComboBox colBx;
		private JButton addBtn;
		private JButton cancelBtn;
		private ArrayList availCols;
		
		YAxesDialog(){
			super(myFrame, "Add Y-Axis", true);
			availCols = new ArrayList<String>();
			
			buildUI();
		}
		
		private void buildUI(){
			
			JLabel colLbl = new JLabel("New Y-Axis: ");
			updateColumnList();
			colBx = new JComboBox(new DefaultComboBoxModel(availCols.toArray()));
			
			addBtn = new JButton(addAct);
			cancelBtn = new JButton(cancelAct);
			
			JPanel centerPnl = new JPanel();
			centerPnl.add(colLbl);
			centerPnl.add(colBx);
			
			JPanel botPnl = new JPanel();
			botPnl.add(addBtn);
			botPnl.add(Box.createHorizontalStrut(5));
			botPnl.add(cancelBtn);
			
			JPanel mainPnl = new JPanel(new BorderLayout());
			mainPnl.add(centerPnl, BorderLayout.CENTER);
			mainPnl.add(botPnl, BorderLayout.SOUTH);
			
			setContentPane(mainPnl);
			pack();
		}
		
		private AbstractAction addAct = new AbstractAction("ADD") {
			public void actionPerformed(ActionEvent arg0) {
				YAxisSelection newY = new YAxisSelection(colBx.getSelectedItem().toString());
				selectedYs.add(newY);
				refreshYPanel();
				
				setVisible(false);
			}
		};

		private AbstractAction cancelAct = new AbstractAction("CANCEL") {
			public void actionPerformed(ActionEvent e) {
				colBx.setSelectedIndex(0);
				setVisible(false);
			}
		};
		
		private void updateColumnList(){
			//don't do anything if the column names aren't populated yet
			if(columnNames == null){
				return;
			}
			
			//clear the available columns list
			availCols.clear();
			//only add entries that haven't been used
			for(String colName : columnNames){
				boolean isUsed = false;
				for(YAxisSelection y : selectedYs){
					if(y.getColumnName().equals(colName)){
						isUsed = true;
						break;
					}
				}
				//if it hasn't been used, add it
				if(!isUsed){
					availCols.add(colName);
				}
			}
		}
		
		void showDialog(){
			updateColumnList();
			colBx.setModel(new DefaultComboBoxModel(availCols.toArray()));
			pack();
			
			setLocationRelativeTo(myFrame);
			setVisible(true);
		}
	}
	
	public void selectionsChanged() {
		updatePlot();
	}
	
	public void selectionsAdded(java.util.List<StampShape> newStamps) {
		updatePlot();
	}
	
    public static int shiftBand(int oldBand) {
    	int newBand = oldBand + 128;
    	if (newBand>255) {
    		newBand-=255;
    	}
    	return newBand;
    }
    
    
    public static int getContrastVersionForColor(Color color) {
    	if (color==null) return 0;
	    
	    int newRed = shiftBand(color.getRed());
	    int newBlue = shiftBand(color.getBlue());
	    int newGreen = shiftBand(color.getGreen());
	    
	    return new Color(newRed, newBlue, newGreen).getRGB();
    }
	
}