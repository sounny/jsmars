package edu.asu.jmars.util.stable;

import java.util.Comparator;

import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * If a client adds instances of this class to the STable TableColumnModel,
 * it will use the included Comparator for sorting instead of relying on
 * cells to implement Comparable.
 */
public final class ComparableTableColumn extends TableColumn {
	private Comparator<Object> columnComparator;
	public ComparableTableColumn (Comparator<Object> comparator) {
		super ();
		this.columnComparator = comparator;
	}
	public ComparableTableColumn (int modelIndex, Comparator<Object> comparator) {
		super (modelIndex);
		this.columnComparator = comparator;
	}
	public ComparableTableColumn (int modelIndex, int width, Comparator<Object> comparator) {
		super (modelIndex, width);
		this.columnComparator = comparator;
	}
	public ComparableTableColumn (int modelIndex, int width, TableCellRenderer renderer, TableCellEditor editor, Comparator<Object> comparator) {
		super (modelIndex, width, renderer, editor);
		this.columnComparator = comparator;
	}
	public Comparator<Object> getComparator () {
		return columnComparator;
	}
}
