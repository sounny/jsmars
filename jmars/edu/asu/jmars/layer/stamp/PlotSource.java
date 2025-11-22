package edu.asu.jmars.layer.stamp;

import java.awt.Color;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class PlotSource {

	private String plotTitle;
	
	public PlotSource(String title) {
		plotTitle=title;
	}
	
	public String getPlotTitle() {
		return plotTitle;
	}


}
