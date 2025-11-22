package edu.asu.jmars.layer.shape2.xb.swing;

import java.awt.Cursor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.InvalidDnDOperationException;
import javax.swing.JTable;

class DragGestureHandler implements DragGestureListener {
	private JTable mytbl;

	public DragGestureHandler(JTable table) {
		this.mytbl = table;
	}

	@Override
	public void dragGestureRecognized(DragGestureEvent dge) {
		int selectedColumn = 0;  //mytbl.getSelectedColumn(); //use first column, index 0 for Field Name
		int selectedRow = mytbl.getSelectedRow();
		Object selectedValue = mytbl.getValueAt(selectedRow, selectedColumn);
		if (selectedValue instanceof String && mytbl instanceof ColumnSearchTable) {
			ColumnSearchTable cst = (ColumnSearchTable)mytbl;		  
			String user = (String)selectedValue;  //field name
			Transferable t = new UserTransferable(user);
			DragSource ds = dge.getDragSource();
			try {
				ds.startDrag(dge, new Cursor(Cursor.HAND_CURSOR), t, new DragSourceHandler());
			} catch (InvalidDnDOperationException ex) {

			}
		}

	}

}
