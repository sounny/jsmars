package edu.asu.jmars.layer.stamp.spectra;

import java.awt.Color;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class SpectraTable extends JTable{
	private SpectraTableModel myModel;
	
	public SpectraTable(SpectraTableModel model){
		super(model);
		myModel = model;
		
		setDefaultRenderer(Color.class, new ColorRenderer());
	}
	
	public String getToolTipText(MouseEvent event) {
		String tip = "";
		int row = rowAtPoint(event.getPoint());
		if(row>-1 && row<myModel.getRowCount()){
			SpectraObject so = myModel.getSpectra(row);
			tip = "Desc: "+so.getDesc();
		}
		
		return tip;
	}
}

class ColorRenderer extends DefaultTableCellRenderer {		
    @Override
	public void setForeground(Color c) {			
		super.setForeground(c);
	}

	@Override
	public void setBackground(Color c) {			
		super.setBackground(myColor);
	}

	Color myColor;
	
	public ColorRenderer() { 
    	super();
    }

    public void setValue(Object value) {
    	Color color = (Color) value;
    	myColor=color;
    	setForeground(color);
    	setBackground(color);
    }  
}
