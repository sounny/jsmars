package edu.asu.jmars.layer.stamp.focus;

import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.REMOVE;
import static edu.asu.jmars.ui.image.factory.ImageCatalogItem.DRAG_DOTS;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.text.StringEscapeUtils;
import edu.asu.jmars.layer.profile.swing.ButtonColumn;
import edu.asu.jmars.layer.stamp.focus.OutlineOrderDialog.OrderRule;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;

public class OutlineOrderTableModel extends AbstractTableModel implements IReorderable {
	private static final long serialVersionUID = 1L;

	private final String FIELD_NAME = "Field Name";
	private final String SORT_ORDER = "Sort Order";
	private final String MOVE_UP = "UP";
	private final String MOVE_DOWN = "DOWN";
	private final String REMOVE_COL = "    ";
	private final String GRAB_ROW = "       ";
	
	private List<OrderRule> selectedNumSources = new ArrayList<>();	
	private static Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
	private static Icon trash = new ImageIcon(ImageFactory.createImage(REMOVE.withDisplayColor(imgColor)));
	private static Icon grab = new ImageIcon(ImageFactory.createImage(DRAG_DOTS.withDisplayColor(imgColor)));
	private  JLabel moveup;
	private  JLabel movedown;
	
	private DefaultTableModel defaultmodel;
	private Vector<String> defaultmodelcolumnNames = new Vector<>();
	private Vector<Vector<OrderRule>> defaultmodeldataVector = new Vector<>();
	
	private final String[] columnNames = new String[] {GRAB_ROW,          FIELD_NAME,  SORT_ORDER,  		MOVE_UP,      MOVE_DOWN,   REMOVE_COL };
	private final Class[] columnClass = new Class[] {ButtonColumn.class,  String.class, ButtonColumn.class,	String.class, String.class, ButtonColumn.class};

	protected String[] columnToolTips = {
			"<html><body><div>Press and hold 'grab bars' with left mouse button to drag row to reorder.</div>"
			+"<div>Note, you can also click anywhere in the row, except '-' icon,</div>"
			+"<div>then press and hold left mouse button to drag row to reorder.</div></body></html>",
		    "Outline ordering",
		    "<html><body>"
		    + "<div>Ascending, Sort Order arrow pointing UP, means the records with <b>lower</b> field values </div>"
		    +"<div>will be rendered on top of the records with <b>higher</b> field values.</div>"
		    + "<div>Descending, Sort Order arrow pointing DOWN, means the records with <b>higher</b> field values </div>"
		    + "<div>will be rendered on top of the refont cords with <b>lower</b> field values.</div></body></html>",
		    "<html><body><div>Click arrow to move row one position UP.</div></body></html>",
		    "<html><body><div>Click arrow to move row one position DOWN.</div></body></html>",
		    "<html><body>"
					+ "<div >Click to remove Field entry from this ordering view.</div>"
					+ "<div>Note, this action does <b>not</b> delete this Field from 'Choose a Field' selection list.</div></body></html>" 	
	 };
	
	
	public OutlineOrderTableModel() {
		defaultmodelcolumnNames.add("Order");
		defaultmodel = new DefaultTableModel(defaultmodeldataVector, defaultmodelcolumnNames) {
	            @Override
	            public Class<?> getColumnClass(int columnIndex) {
	                return OrderRule.class;
	            }
	        };
	        
	        moveup = new JLabel(StringEscapeUtils.unescapeHtml4(OutlineOrderDialog.HTML_UP_STR));
	        movedown = new JLabel(StringEscapeUtils.unescapeHtml4(OutlineOrderDialog.HTML_DOWN_STR));
	}	

	public void addData(List<OrderRule> orderrules) {
		selectedNumSources.clear();
		selectedNumSources.addAll(orderrules);
		fireTableDataChanged();
	}
	
	public void add(OrderRule newrule) {
		selectedNumSources.add(newrule);
		fireTableDataChanged();
	}	
	
	
	public List<OrderRule> getModelData() {
		List<OrderRule> modeldata = new ArrayList<>();
		modeldata.addAll(selectedNumSources);
		return modeldata;
	}
	
	public OrderRule getDataAt(int index) {
		if (index < 0 || index >= selectedNumSources.size()) { return null;}
		return selectedNumSources.get(index);
	}
	

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnClass[columnIndex];
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	public String[] getColumnToolTips() {
		return columnToolTips;
	}

	@Override
	public int getRowCount() {
		return selectedNumSources.isEmpty() ? 0 : selectedNumSources.size();
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (selectedNumSources.isEmpty())
			return null;

		OrderRule row = selectedNumSources.get(rowIndex);

		// order rule name 
		if (FIELD_NAME.equals(getColumnName(columnIndex))) {		
			String str = "<html> "+row.getColumnName();
			str += "</html>";
			return str;
		}	
		//its ordering direction
		else if (SORT_ORDER.equals(getColumnName(columnIndex))) {
			if(row.isAscending()){
				return OutlineOrderDialog.moveUPicon;
			} else {
				return OutlineOrderDialog.moveDOWNicon;
			}
		}
		
		//move up
		else if (MOVE_UP.equals(getColumnName(columnIndex))) {
				return moveup.getText();
		}
		
		//move down
		else if (MOVE_DOWN.equals(getColumnName(columnIndex))) {
				return movedown.getText();
		}

		//remove
		else if (REMOVE_COL.equals(getColumnName(columnIndex))) {
			return trash;
		}
		
		//grab bar
		else if (GRAB_ROW.equals(getColumnName(columnIndex))) {
			return grab;
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int colIndex) {
		if (REMOVE_COL.equals(getColumnName(colIndex)))
			return true;
		return false;
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		if (selectedNumSources.isEmpty())
			return;

		OrderRule row = selectedNumSources.get(rowIndex);

		 if (REMOVE_COL.equals(getColumnName(columnIndex))) {			
			if (row != null) {										
				this.selectedNumSources.remove(row);
				fireTableDataChanged();
			}
		}		
	}	


	@Override
	public void reorder(int from, int to) {
		if (from == to) {
			return;
		}
		if (from < 0 || from >= selectedNumSources.size()) { return;}
		if (to < 0 || to >= selectedNumSources.size()) { return;}
		
        List<OrderRule> initialData = new ArrayList<>();
        for (OrderRule or : selectedNumSources) {
        	initialData.add(or);
        }
        
        defaultmodeldataVector.clear();
        for (OrderRule order : initialData) {
            Vector<OrderRule> rowData = new Vector<>();
            rowData.add(order);
            defaultmodeldataVector.add(rowData);
        }

        // Move row "from" "to"
        int fromIndex = from;
        int toIndex = to;
        defaultmodel.moveRow(fromIndex, fromIndex, toIndex);

        // Update the original ArrayList with the changes made in the model
        initialData.clear();
        for (int i = 0; i < defaultmodel.getRowCount(); i++) {
            OrderRule order = (OrderRule) defaultmodel.getValueAt(i, 0);
            initialData.add(order);
        }
        
        selectedNumSources.clear();
        selectedNumSources.addAll(initialData);

		fireTableDataChanged();
	}
	
}

