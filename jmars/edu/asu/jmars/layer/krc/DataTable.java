package edu.asu.jmars.layer.krc;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeTable;


public class DataTable extends JTable {
	private DataTableModel myModel;
	private Color missingPlot = ((ThemeTable)GUITheme.get("table")).getSpecialDataHighlight();
	
	
	public DataTable(DataTableModel model){
		super(model);
		myModel = model;
	}
	
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component c = super.prepareRenderer(renderer, row, column);
		KRCDataPoint dp = myModel.getDataPoint(row);
		
		Color fgColor = getForeground();
		
		//only colorize text for the results table (not input table)
		if(!myModel.isForInputTable()){
			if(dp.getDayData() == null || dp.getYearData() == null){
				fgColor = missingPlot;
			}
		}		
		c.setForeground(fgColor);		
		return c;
	}

	
	public String getToolTipText(MouseEvent event) {
		String tip = "";
	
		int row = rowAtPoint(event.getPoint());

		KRCDataPoint dp = myModel.getDataPoint(row);
		
		if(dp.getDayData() == null || dp.getYearData() == null){
			tip = "No KRC results exist for this data point with current inputs.";
		}
		
		return tip;
	}
	
}
