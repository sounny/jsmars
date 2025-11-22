package edu.asu.jmars.ui.jmarscomponent;

import java.awt.Component;
import javax.swing.JTable;
import mdlaf.components.table.MaterialTableCellRenderer;

public class JMarsTableCellRenderer extends MaterialTableCellRenderer  {
	
	 @Override
	  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
	      //Insert here some common personalizing.
	      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
	      return this;
	  }	

}
