package edu.asu.jmars.util.stable;

import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import edu.asu.jmars.layer.shape2.ColumnEditor;
import edu.asu.jmars.layer.shape2.ShapeFocusPanel;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.layer.util.features.CalculatedField;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.FieldList;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.ListType;

// cell editor for the List Type column
public class ListTypeCellEditor extends DefaultCellEditor {
	
	JComboBox<String> cb;
	ShapeLayer layer;
	
	Field myField;
	
	public ListTypeCellEditor(ShapeLayer sl){
		super(new JComboBox());
		
		layer = sl;
	}

	public ListTypeCellEditor(Field field){
		super(new JComboBox());
		
		myField = field;
	}


	public boolean isCellEditable(EventObject e){
		if (e instanceof MouseEvent) {
			int clickCount = ((MouseEvent)e).getClickCount();
			return (clickCount>1);
		} else {
			return false;
		}
	}

	public Object getCellEditorValue() {
		return cb.getSelectedItem();
	}
	
	public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected,
            int row, int column){

		Field f = myField;

		if (f==null) {
			cb = new JComboBox<String>();
			
			int[] rows = layer.getFileTable().getSelectedRows();
			if (rows == null || rows.length != 1) {
				return cb;
			}
	
	
			FeatureCollection fc = layer.getFileTable().getFileTableModel().get(rows[0]);
			
			List<Field> fields = fc.getSchema();
			
			String colName = table.getColumnName(column);
			
			for(Field field : fields){
				if(field.name.equals(colName)){
					f = field;
				}
			}
		}
		
		if(f!=null){
			FieldList fl = (FieldList)f;
			fl.getList();
						
			ListType list = fl.getList();
			
			cb = new JComboBox<String>(new Vector(list.getValues()));
			
			cb.addActionListener(new ActionListener() {	
				@Override
				public void actionPerformed(ActionEvent e) {
					fireEditingStopped();
				}
			});
			
			if(value!=null){
				cb.setSelectedItem(value);
			}
		}
		
		return cb;
	}
}