package edu.asu.jmars.util.stable;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.util.EventObject;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;

// Editor for Double-classed table columns.
public class DoubleCellEditor extends DefaultCellEditor {
	NumberFormat nf = null;
	static JTextField numberTextField = new JTextField();
	
	public DoubleCellEditor(){
		super(getTextField());
	}
	public DoubleCellEditor(NumberFormat nf){
		this();
		this.nf = nf;
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
		Double doubleValue;
		String str = (String)numberTextField.getText();
		try {
			doubleValue = new Double( str);
		} catch (Exception e) {
			doubleValue = null;
		}
		return doubleValue;
	}

	private static JTextField getTextField () {
		numberTextField.setHorizontalAlignment( JTextField.CENTER);
		return numberTextField;
	}
	
	public Component getTableCellEditorComponent(JTable table, Object value,
			boolean isSelected, int row, int column) {
		
		Double text = (Double)value;
		if(text == null){
			numberTextField.setText("null");
			return numberTextField;
		}
		if(nf!=null){
			numberTextField.setText(nf.format(text));
		}else{
			numberTextField.setText(text.toString());
		}
		
		return numberTextField;
	}
}
