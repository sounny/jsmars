package edu.asu.jmars.util.stable;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import edu.asu.jmars.util.Util;

public class SortingTableModel
	implements TableModel, TableModelListener, Sorter.Listener
{
	private TableModel unsortedModel;
	private Set listeners = new LinkedHashSet();
	private Sorter sorter;
	private boolean processingTableChanged;

	public SortingTableModel(Sorter sorter) {
		this.sorter = sorter;
	}

	public void setModel (TableModel unsortedModel) {
		if (this.unsortedModel != null)
			this.unsortedModel.removeTableModelListener(this);
		this.unsortedModel = unsortedModel;
		if (unsortedModel != null)
			unsortedModel.addTableModelListener(this);
		notify (new TableModelEvent (this, TableModelEvent.HEADER_ROW));
	}

	public void addTableModelListener(TableModelListener listener) {
		listeners.add(listener);
	}

	public void removeTableModelListener(TableModelListener listener) {
		listeners.remove(listener);
	}

	public Object getValueAt(int row, int col) {
		return unsortedModel.getValueAt(sorter.unsortRow(row), col);
	}

	public void setValueAt(Object value, int row, int col) {
		unsortedModel.setValueAt(value, sorter.unsortRow(row), col);
	}

	public int getRowCount() {
		return unsortedModel.getRowCount();
	}

	public int getColumnCount() {
		return unsortedModel.getColumnCount();
	}

	public String getColumnName(int col) {
		return unsortedModel.getColumnName(col);
	}

	public Class getColumnClass(int col) {
		return unsortedModel.getColumnClass(col);
	}

	public boolean isCellEditable(int row, int col) {
		return unsortedModel.isCellEditable(sorter.unsortRow(row), col);
	}

	/**
	 * Just fulfilling the interface contract...
	 */
	public void sortChangePre () {
		// no op
	}

	/**
	 * Notifies TableModelListeners that the table data has changed. This method
	 * will only do something when the reason for the change is an event from
	 * the Sorter indicating the sort columns have changed. Changes in the
	 * actual data from the underlying unsorted table model are processed in
	 * forward().
	 */
	public void sortChanged () {
		// Because we know the column sorts changed, we know the rows didn't,
		// so rather than sending a table-data-changed, send this more efficient
		// all-rows-changed.
		if (! processingTableChanged)
			notify (new TableModelEvent (this, 0, getRowCount()-1));
	}

	/**
	 * Immediately forwards the event to the sorter, then forwards the event
	 * to listeners on this.
	 */
	public void tableChanged(TableModelEvent e) {
		if (e != null && e.getType() == TableModelEvent.DELETE)
			forward (e);
		
		processingTableChanged = true;
		try {
			if (e == null || e.getFirstRow() == TableModelEvent.HEADER_ROW) {
				// on a structure change, we have to drop all sorts
				sorter.clearSorts();
			} else {
				// otherwise we just update using the old sorts
				sorter.sort ();
			}
		} finally {
			processingTableChanged = false;
		}

		if (e == null || e.getType() != TableModelEvent.DELETE)
			forward (e);
	}

	/**
	 * Converts an unsorted TableModel event into the sorted coordinates,
	 * replaces the event source with 'this', and fires the resulting event at
	 * our listeners (normally at least JTable.)
	 * 
	 * @throws IllegalArgumentException if the row range is inverted (last < first).
	 */
	private void forward (TableModelEvent e) {
		int first = e.getFirstRow();
		int last = e.getLastRow();
		
		if (e==null) {
			notify(null);
		} else if (e.getFirstRow()==TableModelEvent.HEADER_ROW) {
			notify(new TableModelEvent(this, TableModelEvent.HEADER_ROW, Integer.MAX_VALUE, TableModelEvent.ALL_COLUMNS));			
		}
		// if row range is in the unsortedModel's row domain
		else if (first >= 0 && last < sorter.getSize() && last >= first) {
			if (e.getType()!=TableModelEvent.UPDATE || !isSortedBy(e) ) {
				// send one event for each disjoint sorted index range
				int[] indices = new int[last - first + 1];
				for (int i = 0; i < indices.length; i++) {
					indices[i] = sorter.sortRow(i+first);
				}
				int[][] binned = Util.binRanges (indices);
				if (binned.length > 1) {
					notify (new TableModelEvent (this));
				} else {
					notify (new TableModelEvent (this, binned[0][0],
						binned[0][1], e.getColumn(), e.getType()));
				}
			} else {
				notify (new TableModelEvent (this, 0,
						unsortedModel.getRowCount()-1, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));
			}
		}
		else {
			// This clause will be reached whenever we can't figure out exactly what we're supposed to do
			// with the event provided.  Instead we'll send a 'everything changed' event, which will mostly
			// work but has the side effect of losing any selections the user had.
			
			// The only event that should be going through at
			// this point is the all rows updated event. Otherwise,
			// the range of rows is invalid.
			notify (new TableModelEvent (this, 0,
					Integer.MAX_VALUE, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));
		}
	}
	
	private boolean isSortedBy(TableModelEvent e) {
		if (sorter.getSorts().size() == 0)
			return false;
		if (e.getColumn() == TableModelEvent.ALL_COLUMNS)
			return true;
		for (Iterator it=sorter.getSorts().iterator(); it.hasNext(); ) {
			if (((TableColumn)it.next()).getModelIndex() == e.getColumn())
				return true;
		}
		return false;
	}
	private void notify(TableModelEvent e) {
		for (Iterator it = listeners.iterator(); it.hasNext();)
			((TableModelListener)it.next()).tableChanged(e);
	}
}


