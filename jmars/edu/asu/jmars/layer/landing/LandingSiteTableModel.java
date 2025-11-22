package edu.asu.jmars.layer.landing;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import edu.asu.jmars.layer.landing.LandingLView;
import edu.asu.jmars.swing.ColorCombo;

class LandingSiteTableModel extends DefaultTableModel {
	
	public static final int COMMENT_COLUMN = 0;
	public static final int COLOR_COLUMN = 1;
	public static final int CENTERLON_COLUMN = 2;
	public static final int CENTERLAT_COLUMN = 3;
	public static final int HORAXIS_COLUMN = 4;
	public static final int VERAXIS_COLUMN = 5;
	public static final int ANGLE_COLUMN = 6;
	
	private String[] columnNames;
    
    
    private LandingLView lview = null;
    
    
    public LandingSiteTableModel(LandingLView view) {
    	super();
    	lview = view;
    	
    	ArrayList<StatCalculator> scs = lview.getStatCalculators();
    	
    	int tableSize = 7 + scs.size();
    	
    	columnNames = new String[tableSize];
    	columnNames[0] = "Note";
    	columnNames[1] = "Color";
    	columnNames[2] = "Center Lon";
    	columnNames[3] = "Center Lat";
    	columnNames[4] = "Horizontal Axis";
    	columnNames[5] = "Vertical Axis";
    	columnNames[6] = "Angle";
    	
    	for(int i=7; i<tableSize; i++){
    		columnNames[i] = scs.get(i-7).getName();
    	}
    }
    
	public int getColumnCount() {
		return columnNames.length;
	}

	public String getColumnName(int columnIndex) {
		if (columnIndex==columnNames.length) {
			return "_landingSite";
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
	
	public synchronized void addRow(LandingSite newSite) {
		List<Vector> newRow = new ArrayList<Vector>();

		Vector v = new Vector();
		
		v.add(newSite.getComment());
		ColorCombo cc = new ColorCombo(newSite.getColor());
		v.add(cc);
		v.add(newSite.getLon());
		v.add(newSite.getLat());
		v.add(newSite.getHorizontalAxis());
		v.add(newSite.getVerticalAxis());
		v.add(newSite.getAngle()*180/Math.PI);
		//stats
		for(Stat st : newSite.getStats()){
			v.add(st);
		}
		v.add(newSite);
		newRow.add(v);
		addRows(newRow);
	}
	
	public int getRow(LandingSite s) {
		for(int i=0; i<getRowCount(); i++){
			Vector rowData = (Vector)dataVector.get(i);
			if(rowData.get(rowData.size()-1) == s){
				return i;
			}
		}
		return -1;
	}
	
	public synchronized void refreshRow(LandingSite site) {
		int row = getRow(site);

		Vector v = new Vector(columnNames.length+1);
		
		v.add(site.getComment());
		ColorCombo cc = new ColorCombo(site.getColor());
		v.add(cc);
		v.add(site.getLon());
		v.add(site.getLat());
		v.add(site.getHorizontalAxis());
		v.add(site.getVerticalAxis());
		v.add(site.getAngle()*180/Math.PI);
		//stats
		for(Stat st : site.getStats()){
			v.add(st);
		}
		v.add(site);

		dataVector.setElementAt(v, row);
		fireTableRowsUpdated(row, row);

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
		LandingSite ls = (LandingSite)v.elementAt(v.size()-1);

		if (columnIndex==columnNames.length) {
			return ls;
		} else if (columnIndex==COLOR_COLUMN){
			ColorCombo cc = (ColorCombo)v.elementAt(COLOR_COLUMN);
			return cc;
		} else if (columnIndex>=v.size()-1){
			return "<Data Loading>";
		} else {
			return super.getValueAt(rowIndex, columnIndex);
		}
	}

	public Object getValueAt(int rowIndex) {
		Vector v = (Vector)dataVector.elementAt(rowIndex);
		LandingSite ls = (LandingSite)v.elementAt(v.size()-1);
		return ls;
	}
	
   public boolean isCellEditable(int rowIndex, int columnIndex) {
	   if (columnIndex == COLOR_COLUMN || columnIndex == COMMENT_COLUMN
		|| columnIndex == ANGLE_COLUMN || columnIndex == CENTERLAT_COLUMN
		|| columnIndex == CENTERLON_COLUMN) { 
		   return true;	
	   } else {
		   return false;
	   }
    }				
   
   public Class getColumnClass(int c) {
       return getValueAt(0, c).getClass();
   }

   public void setValueAt(Object value, int row, int col) {
	   Vector v = (Vector)dataVector.elementAt(row);
	   LandingSite ls = (LandingSite)v.elementAt(v.size()-1);
	   if (col==COLOR_COLUMN && value instanceof Color) {
		   Color color = (Color)value;
		   ls.setColor(color);
		   ColorCombo cc = (ColorCombo)v.elementAt(this.COLOR_COLUMN);			
		   cc.setSelectedItem(color);
		   lview.drawSites();
		   lview.drawSelectedSites();
		   lview.repaint();
	   } else if(col == COMMENT_COLUMN && value instanceof JTextArea){
		   ls.setComment(((JTextArea)value).getText());
		   refreshRow(ls);
	   } else if(col == ANGLE_COLUMN && value instanceof Double){
		   //change angle in site
		   ls.setAngleInDeg((Double)value);
		   v.set(ANGLE_COLUMN, ls.getAngle()*180/Math.PI);
		   //update stats for site
		   ls.dirty = true;
		   lview.updateStats();
		   //refresh lview
		   lview.drawSites();
		   lview.drawSelectedSites();
		   lview.repaint();
	   } else if(col == CENTERLON_COLUMN && value instanceof Double){
		   if((Double)value >= -360 && (Double)value <= 360){
			   //change lon value in site
			   ls.setLon((Double)value);
			   v.set(CENTERLON_COLUMN, ls.getLon());
			   //update stat columns for site
			   ls.dirty = true;
			   lview.updateStats();		   
			   //update lview
			   lview.drawSites();
			   lview.drawSelectedSites();
			   lview.repaint();
		   }
	   } else if(col == CENTERLAT_COLUMN && value instanceof Double){
		   if((Double)value >= -90 && (Double)value <= 90){
			   //change lat value in site
			   ls.setLat((Double)value);
			   v.set(CENTERLAT_COLUMN, ls.getLat());
			   //update stats for site
			   ls.dirty = true;
			   lview.updateStats();
			   //refresh lview
			   lview.drawSites();
			   lview.drawSelectedSites();
			   lview.repaint();
		   }
	   }
       fireTableCellUpdated(row, col);
  }

}

