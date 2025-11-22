package edu.asu.jmars.util.stable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumn;

/**
 * Extends DefaultTableColumnModel to let the user filter which columns they
 * want to see via a column dialog. This class doesn't know about the component
 * it's connected to, so the containing class must call showDialog() in response
 * to some user action. This class aims to behave like DefaultTableColumnModel
 * if the column dialog is never shown.
 */
public class FilteringColumnModel extends DefaultTableColumnModel {
	/**
	 * All the columns, visible or not
	 */
	private List<TableColumn> allColumns = new ArrayList<TableColumn>();
	
	// Default columns to display initially, or to reset when requested by the user
	private String defaultCols[] = null;
	
	public interface FilterChangeListener {
		void filtersChanged();
	}
	private List<FilterChangeListener> listeners = new ArrayList<FilterChangeListener>();
	public void addListener(FilterChangeListener listener) {
		listeners.add(listener);
	}
	public void removeListener(FilterChangeListener listener) {
		listeners.remove(listener);
	}
	
	// Convert from visible index to allColumns index; the 'first available'
	// index in tableColumns is translated to the 'first available' index in
	// allColumns.
	private int visToAll(int visIndex) {
		if (visIndex < 0 || visIndex > tableColumns.size())
			return -1;
		else if (visIndex == tableColumns.size())
			return allColumns.size();
		else
			return allColumns.indexOf(getColumn(visIndex));
	}

	// return intersection of a and b, in a's order
	private List<?> intersection(List<?> a, List<?> b) {
		List<?> copyA = new ArrayList<Object>(a);
		copyA.retainAll(b);
		return copyA;
	}
	
	/**
	 * Returns unmodifiable List of all columns, visible or not.
	 */
	public List<TableColumn> getAllColumns () {
		return Collections.unmodifiableList (allColumns);
	}
	
	/**
	 * @return An arraylist of strings of all the column names
	 */
	public ArrayList<String> getColumnNames(){
		ArrayList<String> names = new ArrayList<String>();
		for (TableColumn column: getAllColumns()) {
			String name = column.getHeaderValue().toString();
			names.add(name);
		}
		return names;
	}

	/**
	 * Returns the TableColumn associated with the given identifier. The search
	 * is done over visible and hidden TableColumns.
	 * @return null if the identifier isn't on at least one TableColumn, or the
	 * reference to the first one found.
	 */
	public TableColumn getColumn (Object identifier) {
		for (TableColumn column: allColumns)
			if (column.getIdentifier().equals(identifier))
				return column;
		return null;
	}

	/**
	 * Returns the TableColumn associated with the given identifier. The search
	 * is done over visible and hidden TableColumns.
	 * @return null if the identifier isn't on at least one TableColumn, or the
	 * reference to the first one found.
	 */
	public TableColumn getVisColumn (Object identifier) {
		for (int i = 0; i < super.getColumnCount(); i++) {
			TableColumn column = super.getColumn(i);
			if (column.getIdentifier().equals(identifier))
				return column;
		}
		return null;
	}

	//
	// TableColumnModel overrides
	//

	/**
	 * Adds a column to the list of allColumns but does not show it. Does
	 * nothing if the given column is already in the model.
	 */
	public void addColumn(TableColumn col) {
		if (!allColumns.contains(col)) {
			super.addColumn(col);
			allColumns.add(col);
			fireFiltersChanged();
		}
	}
	
	public void removeAllColumns() {
        for (Object tc : allColumns) {
            super.removeColumn((TableColumn)tc);
        }
		allColumns.clear();
		fireFiltersChanged();
	}
	
	public void removeColumn(TableColumn column) {
		allColumns.remove(column);
		super.removeColumn(column);
		fireFiltersChanged();
	}

	public void moveColumn(int tableFrom, int tableTo) {
		// must call visToAll before moving the columns
		int allFrom = visToAll(tableFrom);
		int allTo = visToAll(tableTo);
		// must do this no matter what
		super.moveColumn(tableFrom, tableTo);
		// the rest of this method proceed only when the index _has_ changed
		if (tableFrom == tableTo)
			return;
		// do nothing if allFrom is not a valid position
		if (allFrom < 0 || allFrom >= allColumns.size())
			return;
		// update both column lists
		allColumns.add(allTo, allColumns.remove(allFrom));
		fireFiltersChanged();
	}

	/**
	 * Set all columns visible or hidden at once, firing a listener update at the end.
	 * The alternate approach triggered many updates if a table had lots of columns that were being toggled at once.
	 * @param vis
	 */
	public void setAllVisible(boolean vis) {
		for (TableColumn column : allColumns) {
			if (vis) {
				if (!tableColumns.contains(column)) {
					super.addColumn(column);
					int from = tableColumns.indexOf(column);
					int to = intersection(allColumns, tableColumns).indexOf(column);
					super.moveColumn(from, to);		
				}
			} else {
				super.removeColumn(column);				
			}
		}
			
		fireFiltersChanged();
	}
	
	/**
	 * Sets column visibility and if a change was made in the visible
	 * columns, notifies ColumnModel listeners. Two events are sent: the
	 * first adds the column to the end of the model, the second moves it to
	 * the proper index.
	 */
	public void setVisible(TableColumn column, boolean vis) {
		if (!allColumns.contains(column))
			return;
		if (tableColumns.contains(column) && !vis) {
			super.removeColumn(column);
		} else if (!tableColumns.contains(column) && vis) {
			super.addColumn(column);
			int from = tableColumns.indexOf(column);
			int to = intersection(allColumns, tableColumns).indexOf(column);
			super.moveColumn(from, to);
		}
		fireFiltersChanged();
	}
	
	/**
	 * Sets the default columns to display, if any
	 */
	public void setDefaultColumns(String defaultCols[]) {
		this.defaultCols = defaultCols;
	}

	/**
	 * Returns the default columns to display, if any have been specified
	 * @return a string array of default columns
	 */
	public String[] getDefaultColumns() {
		return defaultCols;
	}
	
	/** notifies that filters or columns changed */
	protected void fireFiltersChanged() {
		for (FilterChangeListener l: new ArrayList<FilterChangeListener>(listeners)) {
			l.filtersChanged();
		}
	}
	
	/**
	 * @return Returns an array of the visible columns' names
	 */
	public String[] getVisibleColumnNames(){
		String[] cols = new String[tableColumns.size()];
		int i = 0;
		for(TableColumn tc : tableColumns){
			cols[i++] = tc.getIdentifier().toString();
		}
		return cols;
	}
}
