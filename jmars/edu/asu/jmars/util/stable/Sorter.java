package edu.asu.jmars.util.stable;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import edu.asu.jmars.swing.tableheader.popup.SortDirection;

/**
 * The Sorter:
 * <ul>
 * <li>Keeps a map of sorted to unsorted indices and vice versa based on the
 * TableColumn objects selected for sorting.
 * <li>Sets an icon on the TableColumn headers to indicate how columns are
 * sorted.
 * <li>Listens for TableModelEvents from the unsorted model. Note that events
 * are sent directly from the SortingTableModel only to guarrantee the sort is
 * updated before any SortingTableModel listeners need the updated maps.
 * </ul>
 * TODO: currently you need to read the mutator methods carefully to know how
 * the sort will change. The complexity of this behavior is really driven by
 * mouse event handler code in STable, so this complexity should _be_ in STable.
 */
public class Sorter {
	private TableModel unsortedTableModel;

	private final int maxKeys = 2;
	private int[] s2u = new int[0];
	private int[] u2s = new int[0];

	// Map of TableColumn to Integer (-1 descending sort, +1 ascending sort)
	// Key order determines primacy, most signicant column first
	private Map<TableColumn,Integer> sorts = new LinkedHashMap<TableColumn,Integer>();

	private Set<Listener> listeners = new LinkedHashSet<Listener>();

	/**
	 * Sets the TableModel, and calls clearSorts()
	 */
	public void setModel(TableModel unsortedModel) {
		unsortedTableModel = unsortedModel;
		clearSorts();
	}
	
	/**
	 * Adds the given column as the last (least-significant) column to sort on.
	 * Replaces the existing least-significant column if the max number of sort
	 * columns is already in use. Does not call "rotate" as Direction is provided by user input
	 * from a header menu (new UI)
	 * @param secondaryAscending 
	 * @param direction of sort
	 */
	public void addSort(TableColumn column, SortDirection direction) {
		freeLastSortFor(column);
		sorts.put(column, direction.getDirection());
		sort();
	}
	
	/**
	 * Adds the given column as the last (least-significant) column to sort on.
	 * Replaces the existing least-significant column if the max number of sort
	 * columns is already in use. Will rotate an existing sort.
	 */
	public void addSort(TableColumn column) {
		freeLastSortFor(column);
		rotateSort(column);
		sort();
	}	
	
	/**
	 * Ensures the last (least-significant) sort slot is available for the given
	 * column; this has no effect if the given column is already part of the
	 * sorter.
	 */
	private void freeLastSortFor(TableColumn column) {
		if (!sorts.containsKey(column)) {
			// sorts.size() must be <= maxKeys - 1 so this sort has room
			TableColumn[] keys = (TableColumn[]) sorts.keySet().toArray(new TableColumn[0]);
			while (sorts.size() >= maxKeys) {
				sorts.remove(keys[sorts.keySet().size() - 1]);
			}
		}
	}
	
	/**
	 * Restores the natural ordering of the data.
	 */
	public void clearSorts() {
		sorts.clear();
		sort();
	}

	/**
	 * Remove all TableColumn objects in the given Collection, and call sort()
	 */
	public void removeSorts(Collection<TableColumn> c) {
		sorts.keySet().removeAll(c);
		sort();
	}

	/**
	 * Returns an unmodifiable List of the TableColumn sorts, in descending
	 * order of primacy.
	 */
	public List<TableColumn> getSorts() {
		return Collections.unmodifiableList(new ArrayList<TableColumn>(sorts.keySet()));
	}
	
	/**
	 * @param column Column to get the current state for.
	 * @return -1 if sorted descending, 0 if not sorted, 1 if sorted ascending.
	 */
	public int getDirection(TableColumn column) {
		Integer dir = sorts.get(column);
		return (dir == null ? 0 : dir.intValue());
	}
	
	/**
	 * Replaces all sorts with the given column. 
     * Does not call "rotate" as Direction is provided by user input
	 * from a header menu (new UI)
	 * @param direction 
	 * @param column to sort by
	 */
	public void setSort(TableColumn column, SortDirection direction) {
		sorts.keySet().retainAll(Collections.singletonList(column));
		sorts.put(column, direction.getDirection());
		sort();
	}
	
	/**
	 * Replaces all sorts with the given column. Will rotate the column if it
	 * was previously sorted.
	 */
	public void setSort(TableColumn column) {
		sorts.keySet().retainAll(Collections.singletonList(column));
		rotateSort(column);
		sort();
	}
	
	/**
	 * Removes the sort for the given column, preserving any other sorts.
	 */
	public void removeSort(TableColumn column) {
		sorts.remove(column);
		sort();
	}
	
	/**
	 * Replace the sort on the given column.
	 * @param column The column to set the sort for.
	 * @param direction -1 if sorted descending, 0 if not sorted, 1 if sorted ascending.
	 */
	public void setDirection(TableColumn column, int direction) {
		freeLastSortFor(column);
		sorts.put(column, new Integer(direction));
		sort();
	}
	
	/**
	 * @param row The unsorted index to sort.
	 * @return The sorted equivalent of <code>row</code>.
	 */
	public int sortRow(int row) {
		return u2s[row];
	}
	
	/**
	 * @param row The sorted index to unsort.
	 * @return The unsorted equivalent of <code>row</code>.
	 */
	public int unsortRow(int row) {
		return s2u[row];
	}
	
	/** @return a lookup table to map from unsorted array indices to sorted order. */
	public int[] getSortArray() {
		return (int[])u2s.clone();
	}
	
	/** @return a lookup table to map from sorted array indices to unsorted order. */
	public int[] getUnsortArray() {
		return (int[])s2u.clone();
	}
	
	/**
	 * Returns the sorted equivalnent of the unsorted row indices.
	 * @param unsorted The indices to sort.
	 * @return Sorted indices corresponding to <code>unsorted</code>.
	 */
	public int[] sortRows(int[] unsorted){
		int[] sorted = new int[unsorted.length];
		for(int i=0; i<unsorted.length; i++)
			sorted[i] = u2s[unsorted[i]];
		return sorted;
	}
	
	/**
	 * Returns the unsorted equivalent of the sorted row indices.
	 * @param sorted The sorted indices to unsort.
	 * @return Unsorted indices corresponding to <code>sorted</code>.
	 */
	public int[] unsortRows(int[] sorted){
		int[] unsorted = new int[sorted.length];
		for(int i=0; i<sorted.length; i++)
			unsorted[i] = s2u[sorted[i]];
		return unsorted;
	}

	/**
	 * Returns the size of the domain that is being sorted. Valid indices to
	 * sort and unsort are in the closed interval [0, getSize()-1].
	 */
	public int getSize() {
		return s2u.length;
	}

	/**
	 * Rotates the sort of the given column. If it was not sorted, it
	 * becomes ascending, if it was ascending it becomes descending, if it
	 * was descending it becomes unsorted.
	 */
	private void rotateSort(TableColumn column) {
		// initial direction is ascending
		int dir = 1;
		// increment existing value if found
		if (sorts.containsKey(column))
			dir = ((((Integer) sorts.get(column)).intValue() + 2) % 3) - 1;
		sorts.put(column, new Integer(dir));
	}

	/**
	 * Called to resort the entire table. Usually done in response to a
	 * TableColumnModel or TableModel event.
	 */
	public void sort() {
		// notify all listeners that a sort change is about to occur
		for (Listener l: listeners) {
			l.sortChangePre();
		}

		// rebuild the entire sorting map

		int size = unsortedTableModel.getRowCount();

		Integer[] identityRows = new Integer[size];
		for (int i = 0; i < size; i++)
			identityRows[i] = new Integer(i);
		Arrays.sort (identityRows, new RowComparator());

		s2u = new int[size];
		u2s = new int[size];
		for (int i = 0; i < size; i++) {
			s2u[i] = identityRows[i].intValue();
			u2s[s2u[i]] = i;
		}

		// notify all listeners that a sort change has finished
		for (Listener l: listeners) {
			l.sortChanged();
		}
	}

	/**
	 * Add the given listener.
	 */
	public void addListener (Listener sl) {
		listeners.add (sl);
	}

	/**
	 * Remove the given listener.
	 */
	public void removeListener (Listener sl) {
		listeners.remove (sl);
	}

	/**
	 * Sorter.Listener is the mechanism by which Sorter notifies clients
	 * of programmatic changes in the sorted order.
	 */
	public interface Listener {
		/**
		 * Sorter is about to move around all rows, listeners who need
		 * to retain index-specific data should unsort now.
		 */
		public void sortChangePre ();

		/**
		 * Sorter has updated all rows for a new sort.
		 */
		public void sortChanged ();
	}

	/**
	 * Compares two row ids using the TableColumn sorts applied when this
	 * object is constructed.
	 */
	final class RowComparator implements Comparator<Integer> {
		private int columnCount;
		private Comparator<Object>[] comps;
		private int[] columns;
		private boolean[] ascends;

		/**
		 * Creates a RowComparator based on the current TableModel. If the
		 * TableModel changes, the RowComparator should be thrown away and
		 * recreated.
		 */
		public RowComparator() {
			// caching this data here to speed up comparison
			comps = new Comparator[sorts.size()];
			columns = new int[sorts.size()];
			ascends = new boolean[sorts.size()];
			columnCount = 0;
			for (TableColumn column: sorts.keySet()) {
				int dir = ((Integer) sorts.get(column)).intValue();
				if (dir != 0) {
					ascends[columnCount] = (dir == 1);
					columns[columnCount] = column.getModelIndex();
					if (column instanceof ComparableTableColumn)
						comps[columnCount] = ((ComparableTableColumn) column).getComparator();
					else
						comps[columnCount] = null;
					columnCount++;
				}
			}
		}

		/**
		 * Uses data cached at construction time to compare each cell in each
		 * row.
		 */
		public int compare(Integer o1, Integer o2) {
			return compare (((Integer)o1).intValue(), ((Integer)o2).intValue());
		}
		public final int compare(int row1, int row2) {
			for (int i = 0; i < columnCount; i++) {
				int result = 0;
				Object cell1 = unsortedTableModel.getValueAt(row1, columns[i]);
				Object cell2 = unsortedTableModel.getValueAt(row2, columns[i]);
				if (cell1 == null) {
					result = cell2 == null ? 0 : -1;
				} else if (cell2 == null) {
					result = cell1 == null ? 0 : 1;
				} else if (comps[i] != null) {
					result = comps[i].compare(cell1, cell2);
				} else if (cell1 instanceof String && cell2 instanceof String) {
                    result = String.CASE_INSENSITIVE_ORDER.compare((String)cell1, (String)cell2);
				} else if (cell1 instanceof Color && cell2 instanceof Color) {
					result = ((Color)cell1).getRGB()-((Color)cell2).getRGB();
				}
				else if (cell1 instanceof Comparable) {
					result = ((Comparable) cell1).compareTo(cell2);
				}
				if (result != 0)
					return (ascends[i] ? result : -result);
			}
			if (row1 < row2)
				return -1;
			else if (row1 > row2)
				return 1;
			else
				return 0;
		}
	}
}
