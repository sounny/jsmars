package edu.asu.jmars.util.stable;

import java.awt.event.MouseEvent;
import java.util.EventObject;

import javax.swing.DefaultCellEditor;
import javax.swing.JTextField;

// Editor for String-classed table columns.
public class TextCellEditor
	extends DefaultCellEditor
{
	public TextCellEditor(){
		super( new JTextField());
	}
	public boolean isCellEditable(EventObject e){
		if (e instanceof MouseEvent) {
			int clickCount = ((MouseEvent)e).getClickCount();
			return (clickCount>1);
		} else {
			return false;
		}
	}
}
