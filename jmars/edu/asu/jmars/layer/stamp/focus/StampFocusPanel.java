/**
 * 
 */
package edu.asu.jmars.layer.stamp.focus;

import javax.swing.JSplitPane;
import javax.swing.JTable;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.stamp.StampLayerWrapper;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.chart.ChartView;
import edu.asu.jmars.layer.stamp.radar.FilledStampRadarTypeFocus;
import edu.asu.jmars.layer.stamp.radar.RadarFocusPanel;

public class StampFocusPanel extends FocusPanel	{
	public StampTable     table;
	
	// Chart view attached to the main view.
	public ChartView chartView;	
	public OutlineFocusPanel outlinePanel;
	public SpectraView spectraView;
	public ScatterView scatterView;
	private RadarFocusPanel radarPanel;
	private FilledStampFocus filledPanel;
	
	DavinciFocusPanel davinci = null;
	
	public StampFocusPanel(final StampLView stampLView, StampLayerWrapper wrapper) {
		super(stampLView);

		StampLayer stampLayer = stampLView.stampLayer; 
		
		table = new StampTable(stampLView);
		//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		
		if (stampLayer.spectraPerPixel() || stampLayer.spectraData()) {
			spectraView = new SpectraView(stampLView);
		}
		
		scatterView = new ScatterView(stampLView);
		
		outlinePanel = new OutlineFocusPanel(stampLayer, table, scatterView, spectraView);
		add("Outlines", outlinePanel);
		
		
		if (wrapper!=null) {
			add("Filters", new FilterFocusPanel(stampLView, wrapper));
		}
		
        if(stampLayer.lineShapes()){
        	radarPanel = new RadarFocusPanel(stampLView);
        	add("Radargram", radarPanel);
        }
		
        //Rendered Tab
        // This can either be for a traditional stamp layer (2d stamps
        // on the surface of the body), or for a line stamp layer (some
        // kind of ground penetrating radar).
        if (stampLayer.enableRender()){
       		filledPanel = new FilledStampImageTypeFocus(stampLView);
        	add("Rendered", filledPanel);
        }
        else if(stampLayer.lineShapes()){
    		filledPanel = new FilledStampRadarTypeFocus(stampLView);
    		add("Rendered", filledPanel);
    	}else{
    		//this doesn't seem like it should be necessary, but if
    		// an instance of this panel is not made (even when there
    		// are no renderable objects) then the stamps do not load
    		filledPanel = new FilledStampFocus(stampLView);
    	}
        
        
		if (stampLView.stampLayer.getInstrument().equalsIgnoreCase("davinci")) {
			davinci = new DavinciFocusPanel(stampLayer);
			add("Davinci Connection", davinci);
		}
		
		if (!stampLayer.pointShapes()) {  // Currently no settings for PointShapes (MOLA).  All settings are on the outline tab
			add("Settings", new SettingsFocusPanel(stampLView));
		}
		
		if (wrapper!=null) {
			addTab("Query", new QueryFocusPanel(wrapper, stampLayer));
		}
        
        if (stampLayer.getParam(stampLayer.PLOT_UNITS).length()>0) {
        	chartView = new ChartView(stampLView);  		
        	add("Chart", chartView);
        }
        
        
		// add initial rows to the table
		table.dataRefreshed();
	}

	public void dispose() {
		table.getTableModel().removeAll();		
		if (davinci!=null) {
			davinci.dispose();
		}
	}
	
	public ChartView getChartView() {
		return chartView;
	}
	
	public RadarFocusPanel getRadarView(){
		return radarPanel;
	}
	
	public FilledStampFocus getRenderedView(){
		return filledPanel;
	}
	
	public SpectraView getSpectraView(){
		return spectraView;
	}
	
	public ScatterView getScatterView(){
		return scatterView;
	}
	
	public int[] getSelectedRows() {
		return table.getSelectedRows();
	}
			
	public void dataRefreshed() {
		table.dataRefreshed();
		outlinePanel.dataRefreshed();
	}
		      
    @SuppressWarnings("unchecked")
	public void updateData(Class[] newTypes, String[] newNames, String[] initialCols) {
    	table.updateData(newTypes, newNames, initialCols, false);
    	outlinePanel.setColumnColorOptions(newNames);
    	scatterView.updatePlot();
    }
}
