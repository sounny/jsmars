/**
 **/
package edu.asu.jmars.layer.util.features;

import java.awt.Rectangle;
import java.util.*;

import javax.swing.ListSelectionModel;
import javax.swing.event.*;

import edu.asu.jmars.swing.STable;
import edu.asu.jmars.util.stable.Sorter;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.ObservableSet;
import edu.asu.jmars.util.ObservableSetListener;
import edu.asu.jmars.util.Util;

/**
 * Adapts selection changes between a FeatureCollection and a
 * ListSelectionModel, using a Sorter to convert between sorted and unsorted
 * coordinates, and the FeatureTableModel to ignore those events that are the
 * result of the tableModel's actions. *
 * <p>
 * When the FeatureTableModel is changing the ListSelectionModel through the
 * JTable, FeatureSelectionListener should ignore the generated events.
 * <p>
 * When the FeatureSelectionListener is changing the ListSelectionModel to match
 * the FeatureCollection's selections, it should ignore the return events.
 */
public class FeatureSelectionListener implements ListSelectionListener, ObservableSetListener<Feature>, FeatureListener
{
	static final DebugLog log = DebugLog.instance();

	private STable table;
	private FeatureCollection fc;
	private ObservableSet<Feature> selections;
	private ListSelectionModel listModel;
	private Sorter sorter;
	private volatile boolean listening = true;
	
	/**
	 * Override the default {#link SelectionHandler} to tailor how selection state is stored and updated.
	 */
	public FeatureSelectionListener(STable table, FeatureCollection fc, ObservableSet<Feature> selections) {
		this.table = table;
		this.listModel = table.getSelectionModel();
		this.fc = fc;
		this.selections = selections;
		this.sorter = table.getSorter();
		listModel.addListSelectionListener(this);
		selections.addListener(this);
		fc.addListener(this);
	}
	
	/**
	 * Update table row selection from the <em>selected</em> attribute
	 * in each Feature object.
	 */
	public void setFeatureSelectionsToTable () {
		listening = false;
		listModel.setValueIsAdjusting(true);
		try {
			listModel.clearSelection();

			int[] selectIdx = new int[fc.getFeatures().size()];
			int selectSize = 0, pos = 0;
			for (Feature f: fc.getFeatures()) {
				if (selections.contains(f)) {
					selectIdx[selectSize++] = sorter.sortRow(pos);
				}
				pos ++;
			}

			int[][] binned = Util.binRanges (selectIdx, selectSize);
			for (int i = 0; i < binned.length; i++)
				listModel.addSelectionInterval(binned[i][0],binned[i][1]);
		} finally {
			listening = true;
			listModel.setValueIsAdjusting(false);
		}
	}
	
	/**
	 * Remove this instance from listening to the underlying
	 * FeatureCollection and ListSelectionModel.
	 */
	public void disconnect () {
		listModel.removeListSelectionListener (this);
		selections.removeListener(this);
		fc.removeListener(this);
	}

	/**
	 * When the selected state of a Feature disagrees with the selection model
	 * in the table, update the Feature. Ignores events while the
	 * FeatureTableModel or this is sending events to the ListSelectionModel.
	 */
	/**
	 *  TODO: make this work right, and remove this note.
	 *  CAUTION: The removeSelectionInterval() calls in the receive()
	 *  method do not appear to cause proper changes in the lead & anchor
	 *  indices of the DefaultSelectionModel. As a consequence, when a
	 *  bunch of Features are removed from the FeatureCollection backing
	 *  the STable, the next ListSelectionEvent generated
	 *  still wants to go through the entire range that was in the 
	 *  TableModel before the removal.
	 */
	public void valueChanged(ListSelectionEvent e){
		if (! listening || ((table.getUnsortedTableModel() instanceof FeatureTableModel)
		&& ((FeatureTableModel)table.getUnsortedTableModel()).isSending ())) {
			return;
		}

		if (e.getValueIsAdjusting()){
			return;
		}

		listening = false;
		try {
			// only process events that are entirely good
			int first = e.getFirstIndex();
			int last  = e.getLastIndex();
			int maxSize = sorter.getSize () - 1;
			if (last > maxSize)
				last = maxSize;
			if (first < 0 || last < 0) {
				log.aprintln ("Invalid event from listModel");
				return;
			}

			Set<Feature> toEnable = new HashSet<Feature>();
			Set<Feature> toDisable = new HashSet<Feature>();

			for (int sortedIdx = first; sortedIdx <= last; sortedIdx++) {
				int unsortedIdx = sorter.unsortRow (sortedIdx);
				Feature feat = (Feature)fc.getFeatures().get(unsortedIdx);
				boolean tableSel = listModel.isSelectedIndex(sortedIdx);
				boolean featureSel = selections.contains(feat);
				if (featureSel != tableSel) {
					(tableSel ? toEnable : toDisable).add(feat);
				}
			}

			selections.removeAll(toDisable);
			selections.addAll(toEnable);
		} finally {
			listening = true;
		}
	}

	public void change(Set<Feature> added, Set<Feature> removed) {
		if (!listening) {
			return;
		}
		
		listening = false;
		try {
			// this is as fast as we can do it without changing the
			// SingleFeatureCollection data structure to provide more efficient
			// index lookups, or changing the table selection model to be backed by
			// our selection set
			setFeatureSelectionsToTable();
			if (added != null && added.size() > 0) {
				int row = listModel.getMaxSelectionIndex();
				Rectangle rect = table.getCellRect(row, 0, true);
				rect = rect.union(table.getCellRect(row, table.getColumnCount()-1, true));
				table.scrollRectToVisible(rect);
			}
		} finally {
			listening = true;
		}
	}

	public void receive(FeatureEvent e) {
		if (e.type == FeatureEvent.REMOVE_FEATURE) {
			listening = false;
			try {
				selections.removeAll(e.features);
			} finally {
				listening = true;
			}
		}
	}
} // end: class FeatureSelectionListener.java

