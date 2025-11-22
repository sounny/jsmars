package edu.asu.jmars.util.stable;

import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;

// Editor for Integer-classed table columns.
public class IntegerCellEditor extends DefaultCellEditor {
	public IntegerCellEditor(){
		super(getTextField());
	}

	public boolean isCellEditable(EventObject e){
		if (e instanceof MouseEvent) {
			int clickCount = ((MouseEvent)e).getClickCount();
			return (clickCount>1);
		} else {
			return false;
		}
	}

	public Object getCellEditorValue(){
		Integer integerValue;
		String str = (String)super.getCellEditorValue();
		try {
			integerValue = new Integer( str);
		} catch (Exception e) {
			integerValue = null;
		}
		return integerValue;
	}

	private static JTextField getTextField () {
		JTextField numberTextField = new JTextField();
		numberTextField.setHorizontalAlignment( JTextField.RIGHT);
		return numberTextField;
	}
}
