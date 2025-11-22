package edu.asu.jmars.layer.stamp.focus;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.table.DefaultTableModel;

import edu.asu.jmars.layer.stamp.StampShape;

public class StampTableModel extends DefaultTableModel {
    private Class[] columnClasses=new Class[0];
    private String[] columnNames=new String[0];
    
    private HashMap<StampShape, Integer> rowMap = new HashMap<StampShape, Integer>();
	
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex==columnNames.length) {
			return StampShape.class;
		}
		
		Class columnClass = columnClasses[columnIndex];
		
		return columnClass;
	}

	public int getColumnCount() {
		if(columnNames == null){
			return 0;
		}
		return columnNames.length;
	}

	public String getColumnName(int columnIndex) {
		if (columnIndex==columnNames.length) {
			return "_stamp";
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
		rowMap.clear();
	}
	
	public synchronized void addRows(List<Vector> newRows) {
		int startingRow = dataVector.size();
		
		int rowCnt = startingRow;
		
		for(Vector v : newRows) {
			StampShape shape = (StampShape)v.elementAt(v.size()-1);
			rowMap.put(shape, new Integer(rowCnt++));
		}
		
		int endingRow = startingRow+newRows.size();
		dataVector.addAll(newRows);
        fireTableRowsInserted(startingRow, endingRow);			
	}
	
	public int getRow(StampShape s) {
		Integer row = rowMap.get(s);
		if (row!=null) {
			return row.intValue();
		} else {
			return -1;
		}
		
//		Enumeration e = dataVector.elements();
//		
//		int loopCnt=0;
//		while (e.hasMoreElements()) {
//			loopCnt++;
//			Vector v = (Vector)e.nextElement();
//			StampShape s2 = (StampShape)v.elementAt(v.size()-1);
//			
//			if (s==s2) {
//				System.out.println("loopCnt="+loopCnt);
//				return dataVector.indexOf(v);
//			}				
//		}
//		System.out.println("loopCnt="+loopCnt);
//
//		return -1;
	}
	
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (columnClasses[columnIndex]==null) {
			return "";
		}
				
		Vector v = (Vector)dataVector.elementAt(rowIndex);
		StampShape s = (StampShape)v.elementAt(v.size()-1);
		return s.getData(columnIndex);
	}

	public Object getValueAt(int rowIndex) {
		Vector v = (Vector)dataVector.elementAt(rowIndex);
		StampShape s = (StampShape)v.elementAt(v.size()-1);
		return s;
	}
	
   public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
    }				
   
	public boolean isFirstUpdate() {
    	if (columnClasses==null || columnClasses.length==0) {
    		return true;
    	}
    	return false;
    }	

	public void updateData(Class[] newTypes, String[] newNames) {
		columnClasses=newTypes;
		columnNames=newNames;
	}	
	
	/**
	 * @return The array of Column Classes
	 */
	public Class[] getColumnClasses(){
		return columnClasses;
	}
	
	/**
	 * @return The String array of Column Names
	 */
	public String[] getColumnNames(){
		return columnNames;
	}
}

