package edu.asu.jmars.layer.crater;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import javax.swing.table.DefaultTableModel;
import edu.asu.jmars.swing.ColorCombo;

class CraterTableModel extends DefaultTableModel {
	
	public static final int CENTERLON_COLUMN = 0;
	public static final int CENTERLAT_COLUMN = 1;
	public static final int DIAMETER_COLUMN = 2;
	public static final int COLOR_COLUMN = 4;
	public static final int COMMENT_COLUMN = 3;
	
    private String[] columnNames={"Center Lon", "Center Lat", "Diameter", "Note", "Color"};
    
    private CraterLView lview = null;
    
    public CraterTableModel() {
    	super();
    }
    
    public CraterTableModel(CraterLView view) {
    	super();
    	lview = view;
    }
    
	public int getColumnCount() {
		return columnNames.length;
	}

	public String getColumnName(int columnIndex) {
		if (columnIndex==columnNames.length) {
			return "_crater";
		}
		return columnNames[columnIndex];
	}

	public synchronized void removeAll() {
		int lastRow = dataVector.size();
		
		for (int i=0; i<lastRow; i++) {
			Vector v = (Vector)dataVector.elementAt(i);
			v.clear();
		}
		
		dataVector.removeAllElements();
		fireTableRowsDeleted(0,lastRow-1);
	}
	
	public synchronized void addRows(List<Vector> newRows) {
		int startingRow = dataVector.size();	
		int endingRow = startingRow+newRows.size();
		dataVector.addAll(newRows);
        fireTableRowsInserted(startingRow, endingRow);			
	}
	
	public synchronized void addRow(Crater newCrater) {
		List<Vector> newRow = new ArrayList<Vector>();
		
		Vector v = new Vector();
		
		v.add(newCrater.getLon());
		v.add(newCrater.getLat());
		v.add(newCrater.getDiameter());
		v.add(newCrater.getComment());
		ColorCombo cc = new ColorCombo(newCrater.getColor());
		v.add(cc);
		v.add(newCrater);
		
		newRow.add(v);
		
		addRows(newRow);
	}
	
	public int getRow(Crater s) {
		for(int i=0; i<dataVector.size(); i++){
			Vector rowVector = (Vector)dataVector.get(i);
			if(rowVector.get(rowVector.size()-1) == s){
				return i;
			}
		}
		return -1;
	}
	
	public synchronized void refreshRow(Crater crater) {
		int row = getRow(crater);
		
		Vector v=(Vector)dataVector.elementAt(row);
		v.clear();
		v.add(crater.getLon());
		v.add(crater.getLat());
		v.add(crater.getDiameter());
		v.add(crater.getComment());
		ColorCombo cc = new ColorCombo(crater.getColor());
		v.add(cc);
		v.add(crater);

		fireTableDataChanged();
	}
		
	public synchronized void removeRows(int rowsToRemove[]) {

		// Put the rows in numerical order 
		Arrays.sort(rowsToRemove);
		
		// Iterate from highest row number to lowest, so removing from
		// the vector doesn't change the row numbers of rows we haven't
		// removed yet
		for (int i=rowsToRemove.length-1; i>=0; i--) {
			int rowToRemove = rowsToRemove[i];
			dataVector.remove(rowToRemove);
			fireTableRowsDeleted(rowToRemove, rowToRemove);	
		}       			
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		Vector v = (Vector)dataVector.elementAt(rowIndex);
		if (columnIndex==columnNames.length) {
			Crater c = (Crater)v.elementAt(v.size()-1);
			return c;
		} else if (columnIndex==COLOR_COLUMN){
			ColorCombo cc = (ColorCombo)v.elementAt(COLOR_COLUMN);
			return cc;
		} else {
			return super.getValueAt(rowIndex, columnIndex);
		}
	}

	public Object getValueAt(int rowIndex) {
		Vector v = (Vector)dataVector.elementAt(rowIndex);
		Crater c = (Crater)v.elementAt(v.size()-1);
		return c;
	}
	
   public boolean isCellEditable(int rowIndex, int columnIndex) {
	   switch(columnIndex)
	   {
	   case CENTERLON_COLUMN:
	   case CENTERLAT_COLUMN:
	   case DIAMETER_COLUMN:
	   case COLOR_COLUMN:
	   		return true;
	   default:
		   return false;
	   }
    }				
   
   public Class getColumnClass(int c) {
       return getValueAt(0, c).getClass();
   }

   public void setValueAt(Object value, int row, int col) {
	   if(col == CENTERLON_COLUMN && value instanceof Double)
	   {
		   double number = Double.valueOf(value.toString());
		   // longitude should be between -360 and 360
		   number = (number < -360) ? number % (-360) : number; // JNN: added
		   number = (number > 360) ? number % 360 : number; // JNN: added
		   
		   Vector v = (Vector)dataVector.elementAt(row);
		   Crater c = (Crater)v.elementAt(v.size()-1);
		   c.setLon(number);
		   v.set(CENTERLON_COLUMN, c.getLon());
		   lview.drawCraters();
		   lview.repaint();			   
	   }
	   else if(col == CENTERLAT_COLUMN && value instanceof Double)
	   {
		   double number = Double.valueOf(value.toString());
		   // latitude should be between -90 and 90
		   if(number >= -90 && number <= 90) // JNN: added
		   {
			   Vector v = (Vector)dataVector.elementAt(row);
			   Crater c = (Crater)v.elementAt(v.size()-1);
			   c.setLat(number);
			   v.set(CENTERLAT_COLUMN, c.getLat());
			   lview.drawCraters();
			   lview.repaint();			   			   
		   }
	   }
	   else if(col == DIAMETER_COLUMN && value instanceof Double)
	   {
		   double number = Double.valueOf(value.toString());
		   // if number is less than or equal to zero, do nothing
		   if(number > 0) // JNN: added
		   {
			   Vector v = (Vector)dataVector.elementAt(row);
			   Crater c = (Crater)v.elementAt(v.size()-1);
			   c.setDiameter(number);
			   v.set(DIAMETER_COLUMN, c.getDiameter());
			   lview.drawCraters();
			   lview.repaint();			   
		   }
	   }
	   else if (col==COLOR_COLUMN && value instanceof Color) {
			Color color = (Color)value;
			Vector v = (Vector)dataVector.elementAt(row);
			Crater c = (Crater)v.elementAt(v.size()-1);
			c.setColor(color);
			ColorCombo cc = (ColorCombo)v.elementAt(this.COLOR_COLUMN);			
			cc.setSelectedItem(color);
			final CraterSettings settings = ((CraterLayer)lview.getLayer()).settings;
			if (settings.colorToNotesMap.containsKey(color)) {
				c.setComment(settings.colorToNotesMap.get(color));
				v.set(COMMENT_COLUMN, settings.colorToNotesMap.get(color));
			}
			lview.drawCraters();
			lview.repaint();
		} 
       fireTableCellUpdated(row, col);
  }

}

