package edu.asu.jmars.layer.util.filetable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.swing.table.AbstractTableModel;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureEvent;
import edu.asu.jmars.layer.util.features.FeatureListener;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.History;
import edu.asu.jmars.util.Versionable;

public class FileTableModel extends AbstractTableModel
	implements FeatureListener, Versionable
{
	private static DebugLog log = DebugLog.instance();
	
	/**
	 * List of FeatureCollections that back this TableModel.
	 */
	private List<FeatureCollection> fcc = new ArrayList<FeatureCollection>();
	
	/**
	 * Current modification status of the FeatureCollection. A FeatureCollection
	 * is marked modified every time the FileTableModel is notified of a change
	 * made to the FeatureCollection.
	 */
	private Set<FeatureCollection> touched = new HashSet<FeatureCollection>();
	
	/**
	 * Marks the current default FeatureCollection. This FeatureCollection serves
	 * as the target for all add-Feature operations.
	 */
	private FeatureCollection defaultFC = null;

	/**
	 * Listeners listening to the DefaultChangedEvent.
	 */
	private List<DefaultChangedListener> listeners = new LinkedList<DefaultChangedListener>();

	/**
	 * History object to log changes to
	 */
	private History history;

	/**
	 * Column names, their types and default widths (for lack of a better place).
	 */
	public static final Object[][] columns = new Object[][] {
		{"*",        Boolean.class,  new Integer(1)}, // default indicator
		{"File",     String.class,  new Integer(200)},
		{"Features", Integer.class, new Integer(50)},
		{"Touched",  Boolean.class, new Integer(50)},
	};
	
	/**
	 * Column index enumerations.
	 */
	public static final int COL_IDX_DEFAULT_INDICATOR = 0;
	public static final int COL_IDX_FILE = 1;
	public static final int COL_IDX_COUNT = 2;
	public static final int COL_IDX_TOUCHED = 3;
	
	/**
	 * Provider name use when FeatureProvider is null for a given Feature.
	 */
	public static final String NULL_PROVIDER_NAME = "(untitled)";
	
	/**
	 * Default constructor.
	 */
	public FileTableModel() {
		super();
	}

	
	/**
	 * Realization of AbstractTableModel.getColumnCount().
	 */
	public int getColumnCount() {
		return columns.length;
	}

	/**
	 * Realization of AbstractTableModel.getRowCount().
	 */
	public int getRowCount() {
		return fcc.size();
	}

	/**
	 * Realization of AbstractTableModel.getValueAt(int,int).
	 */
	public Object getValueAt(int row, int col) {
		FeatureCollection fc = fcc.get(row);
		switch(col){
		case COL_IDX_DEFAULT_INDICATOR:
			return (fc == defaultFC)? Boolean.TRUE: Boolean.FALSE;
		case COL_IDX_FILE:
			if (fc.getFilename() == null)
				return NULL_PROVIDER_NAME;
			else
				return fc.getFilename();
		case COL_IDX_COUNT:
			return new Integer(fc.getFeatures().size());
		case COL_IDX_TOUCHED:
			return touched.contains(fc);
		}
		return null;
	}

	/**
	 * Overrides AbstractTableModel.getColumnClass(int).
	 */
	public Class<?> getColumnClass(int col){
		return (Class<?>)columns[col][1];
	}
	
	/**
	 * Overrides AbstractTableModel.getColumnName(int).
	 */
	public String getColumnName(int col){
		return (String)columns[col][0];
	}
	
	/**
	 * Overrides AbstractTableModel.isCellEditable(int,int).
	 */
	public boolean isCellEditable(int row, int col){
		return false;
	}
	
	
	/**
	 * Public interface to add a new FeatureCollection to the FileTable.
	 * @param fc
	 */
	public void add(FeatureCollection fc) {	
		removeUntitledIfEmptyAndUntouched();
		fcc.add(fc);
		touched.remove(fc);
		if (history != null)
			history.addChange (this, new AddChange (fc));
		fc.addListener(this);
		if (defaultFC == null){
			defaultFC = fc;
			fireDefaultChangeEvent(defaultFC);
		}

		int idx = fcc.size() - 1;
		fireTableRowsInserted(idx, idx);
		// NOTE: No notfication needs to be sent to the MFC since
		// additions do not change the contents of the MFC.
	}
	
	private void removeUntitledIfEmptyAndUntouched() {
		if (fcc.size() == 1) {
			FeatureCollection myfc = fcc.get(0);
			if (!touched.contains(myfc) && myfc.getFeatureCount() == 0 && myfc == defaultFC) {
				this.remove(myfc);
				defaultFC = null;
			}
		}
	}


	/**
	 * Public interface to remove a FeatureCollection from the FileTable.
	 * 
	 * @param fc FeatureCollection to remove.
	 * @return true if the removal was successful.
	 */
	public boolean remove(FeatureCollection fc){
		int idx = fcc.indexOf(fc);
		if (idx < 0)
			return false;

		history.addChange (this, new RemoveChange (fc));
		fc.removeListener(this);
		boolean result = fcc.remove(fc);
		touched.remove(fc);
		fireTableRowsDeleted(idx,idx);

		if (fc == defaultFC){
			if (idx >= fcc.size())
				idx = fcc.size()-1;
			defaultFC = (idx >= 0)? fcc.get(idx): null;
			fireDefaultChangeEvent(defaultFC);
			if (idx >= 0)
				fireTableRowsUpdated(idx, idx);
		}
		
		// NOTE: No need to send a notification to the MFC since the MFC
		// will be listening to the selection changes.
		return result;
	}
	
	/**
	 * Public interface to remove a collection of FeatureCollection from
	 * the FileTable.
	 * 
	 * @param fcl List of FeatureCollection objects.
	 * @return true if the entire removal was successful, false otherwise.
	 */
	public boolean removeAll(List<FeatureCollection> fcl) {
		int[] indices = new int[fcl.size()];
		boolean defaultDeleted = false;
		
		// Carry out the removal and collect deleted indices.
		Iterator<FeatureCollection> li = fcl.iterator();
		int i=0;
		while(li.hasNext()){
			FeatureCollection fc = li.next();
			fc.removeListener(this);
			indices[i] = fcc.indexOf(fc);
			if (fc == defaultFC)
				defaultDeleted = true;
			i++;
		}

		List<FeatureCollection> intersection = new LinkedList<FeatureCollection>(fcc);
		intersection.retainAll (fcl);
		for (Iterator<FeatureCollection> it=intersection.iterator(); it.hasNext(); ) {
			FeatureCollection sfc = it.next();
			history.addChange (this, new RemoveChange (sfc));
		}
		boolean result = fcc.removeAll(fcl);
		touched.removeAll(fcl);
		
		// Sort the collected indices for which we have to fire deleteRow events.
		Arrays.sort(indices);
		
		// Batch contiguous indices to generate the minimal amount of 
		// rowDeleted events. 
		// Process this list of indices in the reverse order. This will ensure
		// minimal disturbance in the data itself and will require the least
		// amount of redraws. Stop when a negative index is hit.
		int run = (indices.length > 0)? 1: 0;
		for(i=indices.length-2; i>=0; i--){
			if (indices[i] != (indices[i+1]-1) || indices[i] < 0){
				fireTableRowsDeleted(indices[i+1], indices[i+1]+run-1);
				run=0;
			}
			if (indices[i] < 0)
				break;
			
			run++;
		}
		if (run > 0 && indices[i+1] >= 0)
			fireTableRowsDeleted(indices[i+1], indices[i+1]+run-1);
		
		if (defaultDeleted){
			int defaultIndex = indices[indices.length-1];
			if (defaultIndex >= fcc.size())
				defaultIndex = fcc.size()-1;
			defaultFC = (defaultIndex >= 0)? fcc.get(defaultIndex): null;
			fireDefaultChangeEvent(defaultFC);
			if (defaultIndex >= 0)
				fireTableRowsUpdated(defaultIndex, defaultIndex);
		}
		
		return result;
	}
	
	/**
	 * Returns the entire collection of FeatureCollections which is a part
	 * of this FileTableModel.
	 * 
	 * @return The array of FeatureCollections that is a part of this FileTableModel. 
	 */
	public List<FeatureCollection> getAll() {
		return new ArrayList<FeatureCollection>(fcc);
	}

	/**
	 * Realization of FeatureEvent interface. Listens to changes made to
	 * the FeatureCollections that make rows of this table. Changes the
	 * modification status of a Feature on receiving modification
	 * FeatureEvents.
	 * <strong>CAUTION:</strong>FeaturEvents must be from a 
	 * FeatureCollection.
	 */
	public void receive(FeatureEvent e) {
		if (history.versionChanging())
			return;

		Set<FeatureCollection> modified = new HashSet<FeatureCollection>();

		switch(e.type) {
		case FeatureEvent.ADD_FEATURE:
		case FeatureEvent.REMOVE_FEATURE:
		case FeatureEvent.ADD_FIELD:
		case FeatureEvent.REMOVE_FIELD:
			if (e.source != null) {
				modified.add(e.source);
			}
			break;
		case FeatureEvent.CHANGE_FEATURE:
			for (Feature feat: e.features) {
				FeatureCollection fc = feat.getOwner();
				if (fc != null) {
					modified.add(fc);
				}
			}
			break;
		default:
			log.aprintln("Invalid event type "+e.type+" encountered. Ignoring!");
		}

		// Notify the table model of the modification by firing an updated event
		// on the view (JTable).
		for(FeatureCollection fc: modified) {
			setTouched(fc, true);
		}
	}

	/**
	 * Returns the FeatureCollection at the specified index.
	 * 
	 * @param index Index of the FeatureCollection requested.
	 * @return The FeatureCollection at the given index or null if
	 *         the given index is out of bounds.
	 */
	public FeatureCollection get(int index){
		if (index < 0 || index >= fcc.size())
			return null;
		return fcc.get(index);
	}
	
	/**
	 * Similar as {@linkplain #getTouched(int)} except that this method
	 * works on FeatureCollections as compared to their indices.
	 * 
	 * @param fc FeatureCollection to get the modification status of.
	 * @return Modification status of the FeatureCollection if it exists
	 *         in the TableModel or false otherwise.
	 */
	public boolean getTouched(FeatureCollection fc){
		return touched.contains(fc);
	}

	/**
	 * Sets the modification status of the given FeatureCollection.
	 * @param fc FeatureCollection to operate upon.
	 * @param flag Modification status to set.
	 */
	public void setTouched(FeatureCollection fc, boolean flag){
		boolean oldFlag = getTouched(fc);
		if (oldFlag != flag) {
			if (flag) {
				touched.add(fc);
			} else {
				touched.remove(fc);
			}
			int index = fcc.indexOf(fc);
			fireTableRowsUpdated(index, index);
			if (history != null) {
				history.addChange(this, new TouchedChange(fc, oldFlag, flag));
			}
		}
	}

	/**
	 * Returns the current default FeatureCollection, which will
	 * receive all the add Feature requests.
	 */
	public FeatureCollection getDefaultFeatureCollection(){
		return defaultFC;
	}

	/**
	 * Returns the index of the default FeatureCollection.
	 * @return index of the defaultFeatureCollection or -1 if there
	 *         is no default set.
	 */
	public int getDefaultFeatureCollectionIndex(){
		return fcc.indexOf(defaultFC);
	}

	/**
	 * Set the default FeatureCollection. The specified FeatureCollection
	 * must already be a part of the collection of FeatureCollection
	 * stored in the FileTableModel. If that is not the case, the
	 * default is unset.
	 * 
	 * @param fc FeatureCollection to set as default.
	 */
	public void setDefaultFeatureCollection(FeatureCollection fc){
		FeatureCollection oldDefault = defaultFC;
		defaultFC = fc;
		
		int oldIndex = fcc.indexOf(oldDefault);
		if (oldIndex >= 0 && oldIndex < fcc.size())
			fireTableRowsUpdated(oldIndex, oldIndex);
		
		int newIndex = fcc.indexOf(defaultFC);
		if (newIndex >= 0 && newIndex < fcc.size())  
			fireTableRowsUpdated(newIndex, newIndex);
		
		fireDefaultChangeEvent(defaultFC);
		
		if (history != null)
			history.addChange(this, new DefaultChange(oldDefault, defaultFC));
	}
	
	/**
	 * Fires default FeatureCollection change event.
	 * 
	 * @param defaultFC New default FeatureCollection.
	 */
	public void fireDefaultChangeEvent(FeatureCollection fc){
		DefaultChangedEvent e = null;
		
		for(Iterator<DefaultChangedListener> i = listeners.iterator(); i.hasNext(); ){
			DefaultChangedListener l = i.next();
			if (e == null)
				e = new DefaultChangedEvent(fc, fcc.indexOf(fc));
			l.defaultChanged(e);
		}
	}
	
	/**
	 * Adds a DefaultChangedEvent listener.
	 */
	public void addDefaultChangedListener(DefaultChangedListener l){
		listeners.add(l);
	}
	
	/**
	 * Removes a DefaultChangedEvent listener.
	 * 
	 * @return true if the removal was successful.
	 */
	public boolean removeDefaultChangedListener(DefaultChangedListener l){
		return listeners.remove(l);
	}
	
	/**
	 * Returns a list of all DefaultChangedEvent listeners registered
	 * with this FileTableModel.
	 * 
	 * @return A list of all DefaultChangedEvent listeners.
	 */
	public List<DefaultChangedListener> getDefaultChangedListeners(){
		return Collections.unmodifiableList(listeners);
	}

	/**
	 * Set the history to log changes to
	 */
	 public void setHistory (History history) {
		 this.history = history;
	 }

	/**
	 * Undo the given change
	 */
	public void undo (Object change) {
		if (change instanceof AddChange) {
			remove (((AddChange)change).fc);
			return;
		}
		if (change instanceof RemoveChange) {
			RemoveChange ch = (RemoveChange) change;
			add (ch.fc);
			setTouched (ch.fc, ch.dirty);
			return;
		}
		if (change instanceof TouchedChange) {
			TouchedChange c = (TouchedChange)change;
			setTouched(c.fc, c.before);
			return;
		}
		if (change instanceof DefaultChange) {
			setDefaultFeatureCollection(((DefaultChange)change).oldDefault);
			return;
		}
		log.println("Unhandled object "+(change==null? "null": change.getClass().getName())+" encountered.");
	}

	/**
	 * Redo the given change
	 */
	public void redo (Object change) {
		if (change instanceof AddChange) {
			AddChange ch = (AddChange) change;
			add (ch.fc);
			return;
		}
		if (change instanceof RemoveChange) {
			remove (((RemoveChange)change).fc);
			return;
		}
		if (change instanceof TouchedChange) {
			TouchedChange c = (TouchedChange)change;
			setTouched(c.fc, c.after);
			return;
		}
		if (change instanceof DefaultChange) {
			setDefaultFeatureCollection(((DefaultChange)change).newDefault);
			return;
		}
		log.println("Unhandled object "+(change==null? "null": change.getClass().getName())+" encountered.");
	}

	/*
	 * Change container objects to send to a History log. 
	 */

	class AddChange {
		public final FeatureCollection fc;
		public AddChange (FeatureCollection fc) {
			this.fc = fc;
		}
	}

	class RemoveChange {
		public final FeatureCollection fc;
		public final boolean dirty;
		public RemoveChange (FeatureCollection fc) {
			this.fc = fc;
			this.dirty = getTouched (fc);
		}
	}

	public class DefaultChange {
		public final FeatureCollection oldDefault;
		public final FeatureCollection newDefault;

		public DefaultChange(FeatureCollection oldDefault, FeatureCollection newDefault){
			this.oldDefault = oldDefault;
			this.newDefault = newDefault;
		}
	}

	public class TouchedChange {
		public final FeatureCollection fc;
		public final boolean before;
		public final boolean after;

		public TouchedChange(FeatureCollection fc, boolean before, boolean after){
			this.fc = fc;
			this.before = before;
			this.after = after;
		}
	}
}
