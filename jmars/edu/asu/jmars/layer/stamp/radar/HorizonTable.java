package edu.asu.jmars.layer.stamp.radar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import edu.asu.jmars.swing.STable;

public class HorizonTable extends STable{
	private HorizonTableModel myModel;
	
	public HorizonTable(HorizonTableModel model){
		super();
		
		setUnsortedTableModel(model);
		
		myModel = model;
		//assign the show/hide cell renderer
		TableColumn showColumn = this.getColumn(myModel.SHOW_COL);
		showColumn.setCellEditor(new ShowHideCellEditor(new JCheckBox()));
		//assign the color cell renderer
		TableColumn colorColumn = this.getColumn(myModel.COLOR_COL);
		colorColumn.setCellRenderer(new ColorCellRenderer());
	}
	
	class ShowHideCellEditor extends DefaultCellEditor{

		public ShowHideCellEditor(JCheckBox checkBox) {
			super(checkBox);
		}
		
		public Object getCellEditorValue(){
			return ((JCheckBox)editorComponent).isSelected();
		}
		
		public Component getTableCellEditorComponent(JTable table, Object value,
				boolean isSelected, int row, int column){
			
			JCheckBox bx = (JCheckBox)editorComponent;
			
			JPanel pnl = new JPanel(new GridBagLayout());
			pnl.add(bx, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.PAGE_START, GridBagConstraints.NONE, new Insets(0,0,0,0), 0, 0));
			
			pnl.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
			bx.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());

			return pnl;
		}
	}
	
	
	class ColorCellRenderer implements TableCellRenderer{

		public Component getTableCellRendererComponent(JTable table,
				Object value, boolean isSelected, boolean hasFocus, int row,
				int column) {
			
			Color c = (Color) value;
			
			JPanel innerPnl = new JPanel();
			innerPnl.setBackground(c);
			
			JPanel outerPnl = new JPanel();
			outerPnl.setLayout(new BorderLayout());
			outerPnl.setBorder(new EmptyBorder(2, 2, 2, 2));
			outerPnl.add(innerPnl, BorderLayout.CENTER);
			
			if (isSelected) {
				outerPnl.setForeground(table.getSelectionForeground());
				outerPnl.setBackground(table.getSelectionBackground());
			} else {
				outerPnl.setForeground(table.getForeground());
				outerPnl.setBackground(table.getBackground());
			}
			
			return outerPnl;
		}
		
	}
}
