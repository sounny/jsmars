package edu.asu.jmars.layer.investigate;

import java.awt.Color;
import java.awt.Paint;
import java.util.ArrayList;

import javax.swing.table.DefaultTableModel;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;

public class ChartReadOutTableModel extends DefaultTableModel {
	//TODO: add an initial column that's a jcheckbox, "Show" column
	private final String[] columns = new String[]{"Color", "Layer", "ID", "Value", "Units"};
	//Make sure these constants match the order of in the columns array
	//TODO: private final int SHOW_COLUMN = 0;
	public final int COLOR_COLUMN = 0;
	public final int NAME_COLUMN = 1;
	public final int ID_COLUMN = 2;
	public final int VALUE_COLUMN = 3;
	public final int UNIT_COLUMN = 4;

	private ArrayList<String> titles = null;
	private ArrayList<String> ids = null;
	private ArrayList<Double> sampleData = null;
	private ArrayList<String> units = null;
	private JFreeChart chart = null;
	
	public ChartReadOutTableModel(ArrayList<String> titles, JFreeChart chart){
		this.titles = titles;
		this.chart = chart;
	}
	
	public String getColumnName(int col){
		return columns[col];
	}
	public Class<?> getColumnClass(int col){
		switch(col){
			case NAME_COLUMN : return String.class;
			case COLOR_COLUMN : return Color.class;
			case VALUE_COLUMN : return Number.class;
		}
		return Object.class;
	}
	public void setTitles(ArrayList<String> names){
		titles = names;
		fireTableDataChanged();
	}
	public int getRowCount() {
		if(titles == null){
			return 0;
		}else{
			return titles.size();
		}
	}
	public int getColumnCount() {
		return columns.length;
	}
	public Object getValueAt(int rowIndex, int columnIndex) {
		//return null for no entries
		if(titles == null || titles.size() == 0){
			return null;
		}
		//return the title for name column
		else if(columnIndex == NAME_COLUMN){
			return titles.get(rowIndex);
		}
		//return color from plot for the color column
		else if(columnIndex == COLOR_COLUMN){
			int seriesIndex = 0;
			XYPlot plot = chart.getXYPlot();
			if(rowIndex < plot.getDatasetCount() && seriesIndex < plot.getDataset(rowIndex).getSeriesCount()){
				Paint p = ((XYLineAndShapeRenderer)plot.getRenderer(rowIndex)).getSeriesPaint(seriesIndex);
				if(p instanceof Color){
					return (Color)p;
				}else{
					return null;
				}
			}
		}
		//return value for the value column
		else if(columnIndex == VALUE_COLUMN){
			if(sampleData != null && sampleData.size()>rowIndex){
				return new Double(sampleData.get(rowIndex));
			}
		}
		//return id for id column
		else if(columnIndex == ID_COLUMN){
			return ids.get(rowIndex);
		}
		//return units
		else if(columnIndex == UNIT_COLUMN){
			return units.get(rowIndex);
		}
		//else return some default 
		return null;
	}
	public boolean isCellEditable(int rowIndex, int colIndex){
		//only return true for the color column
		return (colIndex == COLOR_COLUMN);
	}
	public void setValueAt(Object value, int rowIndex, int colIndex){
		if(colIndex == COLOR_COLUMN){
			int seriesIndex = 0;
			XYPlot plot = chart.getXYPlot();
			if(rowIndex < plot.getDatasetCount() && seriesIndex < plot.getDataset(rowIndex).getSeriesCount()){
				((XYLineAndShapeRenderer)chart.getXYPlot().getRenderer(rowIndex)).setSeriesPaint(seriesIndex, (Color)value);
			}
		}
	}
	public void setSampleData(ArrayList<Double> newSampleData, ArrayList<String> newIds, ArrayList<String> newUnits){
		sampleData = newSampleData;
		ids = newIds;
		units = newUnits;
		fireTableDataChanged();
	}
	
}
