package edu.asu.jmars.swing;

import edu.asu.jmars.util.*;
import java.util.*;
import javax.swing.event.*;
import javax.swing.table.*;

/**
 ** A proxying table model that adds support for sorting by a
 ** particular column, without actually modifying/sorting the
 ** underlying data.
 **
 ** @see #sort
 ** @see #unsort
 **/
public class SortedTableModel
 implements TableModel
 {
	private static final DebugLog log = DebugLog.instance();

	private EventListenerList listenerList = new EventListenerList();
	private Integer[] mapping = new Integer[0];
	private HashMap oldMappings = new HashMap();
	private TableModel model;

	public SortedTableModel(TableModel model)
	 {
		this.model = model;

		log.println("INIT unsorting");
		unsort();
		log.println("AFTER: INIT unsorting");

		model.addTableModelListener(
			new TableModelListener()
			 {
				public void tableChanged(TableModelEvent e)
				 {
					synchronized(SortedTableModel.this)
					 {
						log.println("NOTIFIED sorted model " +mapping.length);
						oldMappings.clear();
						unsort();
						log.println("AFTER: NOTIFIED sorted "+mapping.length);
					 }
				 }
			 }
			);
	 }

	private synchronized void fireTableChanged()
	 {
		log.println("FIRING sorted model " + mapping.length);
		TableModelEvent e = new TableModelEvent(this);
		Object[] listeners = listenerList.getListenerList();
		for (int i=listeners.length-2; i>=0; i-=2)
			( (TableModelListener) listeners[i+1] ).tableChanged(e);
		log.println("AFTER: FIRING sorted model " + mapping.length);
	 }

	/**
	 ** Given a row number from the underlying model, returns the
	 ** sorted-row number.
	 **/
	public int convertRowToSorted(int row)
	 {
		for(int i=0; i<mapping.length; i++)
			if(mapping[i].intValue() == row)
				return  i;

		// The following should NEVER happen!
		log.aprintln("TELL MICHAEL: INVALID SORTED TABLE STATE!!!!!!!!!!");
		log.aprintStack(-1);
		return  0;
	 }

	/**
	 ** Given a sorted-row number, returns the underlying model's row
	 ** number.
	 **/
	public int convertRowToUnsorted(int row)
	 {
		return  mapping[row].intValue();
	 }

	/**
	 ** Sets the model back to an "unsorted" state, reflecting the raw
	 ** underlying model's ordering.
	 **/
	public synchronized void unsort()
	 {
		mapping = (Integer[]) oldMappings.get(null);
		if(mapping == null)
		 {
			mapping = new Integer[model.getRowCount()];
			for(int i=0; i<mapping.length; i++)
				mapping[i] = new Integer(i);
			oldMappings.put(null, mapping);
		 }
		mapping = (Integer[]) mapping.clone();
		fireTableChanged();
	 }

	/**
	 ** Sorts the model's rows, based on the given column, in the
	 ** given order.
	 **/
	public synchronized void sort(int col, boolean ascending)
	 {
		Integer sortKey = new Integer(col);
		mapping = (Integer[]) oldMappings.get(sortKey);
		if(mapping == null)
		 {
			log.println("Sorting for class of type " +
						 model.getColumnClass(col).getName());
			Class colClass = model.getColumnClass(col);
			boolean comparable = Comparable.class.isAssignableFrom(colClass);
			if(colClass.equals(String.class))
				comparable = false;
			Comparator c = new ColumnComparator(col, comparable);

			unsort();
			Arrays.sort(mapping, 0, mapping.length, c);
			oldMappings.put(sortKey, mapping);
		 }

		if(!ascending)
		 {
			int len = mapping.length;
			Integer[] reversed = new Integer[len];
			for(int i=0; i<len; i++)
				reversed[i] = mapping[len - 1 - i];
			mapping = reversed;
		 }

		fireTableChanged();
	 }

	public void addTableModelListener(TableModelListener l)
	 {
		listenerList.add(TableModelListener.class, l);
	 }

	public Class getColumnClass(int col)
	 {
		return  model.getColumnClass(col);
	 }

	public int getColumnCount()
	 {
		return  model.getColumnCount();
	 }

	public String getColumnName(int col)
	 {
		return  model.getColumnName(col);
	 }

	public synchronized int getRowCount()
	 {
		return  mapping.length;
	 }

	public Object getValueAt(int row, int col)
	 {
		return  model.getValueAt(mapping[row].intValue(), col);
	 }

	public boolean isCellEditable(int row, int col)
	 {
		return  model.isCellEditable(mapping[row].intValue(), col);
	 }

	public void removeTableModelListener(TableModelListener l)
	 {
		listenerList.remove(TableModelListener.class, l);
	 }

	public void setValueAt(Object aValue, int row, int col)
	 {
		model.setValueAt(aValue, mapping[row].intValue(), col);
	 }

	private final class ColumnComparator
	 implements Comparator
	 {
		int col;
		boolean comparable;

		ColumnComparator(int col, boolean comparable)
		 {
			log.println("Sorting col " + col + " " + comparable);
			this.col = col;
			this.comparable = comparable;
		 }

		public final int compare(Object a, Object b)
		 {
			// Extract the value at that row
			int rowA = ( (Integer) a ).intValue();
			int rowB = ( (Integer) b ).intValue();
			Object valA = model.getValueAt(rowA, col);
			Object valB = model.getValueAt(rowB, col);

			// Null values are "greater" than all others
			if(valA == valB)      return  0; // The == is mostly to catch nulls
			else if(valA == null) return  +1;
			else if(valB == null) return  -1;

			// Depending on what's what, do the comparison
			if(comparable)
				return  ( (Comparable) valA ).compareTo(valB);
			else
				return  valA.toString().compareToIgnoreCase(valB.toString());
		 }
	 }
 }
