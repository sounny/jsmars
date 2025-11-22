package edu.asu.jmars.util.stable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.thoughtworks.xstream.XStream;

import edu.asu.jmars.Main;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.stable.Sorter.Listener;

/**
 * <p>Provides STable with persistent view settings outside of the JMARS session
 * file mechanism, and can connect listeners to the table to keep the settings
 * up to date.
 * 
 * <p>Each STable that should be persisted will need its own 'id'. The XML for the
 * table settings will be stored in "~/jmars/tableView<ID>.xml".
 * 
 * <p>When an STable is initially created, calling updateTable() will set the XML
 * settings into the table.
 * 
 * <p>Once connect is called, altering the column order, sort order, sort
 * directions, or show/hide changes will update the XML automatically.
 * 
 * <p>This class may also be constructed with a custom {@link TableStore} instance
 * to save and load table settings to and from some other location on a change.
 */
public class STableViewPersister {
	private static DebugLog log = DebugLog.instance();

	// the table to save to
	private STable table;

	// listen to table model structure and table model change events
	private TableModelListener tableModelListener = new TableModelListener() {
		public void tableChanged(TableModelEvent e) {
			if (e.getFirstRow() == TableModelEvent.HEADER_ROW
					|| e.getLastRow() == Integer.MAX_VALUE) {
				updateConfig();
			}
		}
	};

	// listen to sort change events
	private Listener sorterListener = new Listener() {
		public void sortChangePre() {
		}

		public void sortChanged() {
			updateConfig();
		}
	};

	// listen to column show/hide/move events
	private TableColumnModelListener columnListener = new TableColumnModelListener() {
		public void columnAdded(TableColumnModelEvent e) {
			updateConfig();
		}

		public void columnMoved(TableColumnModelEvent e) {
			updateConfig();
		}

		public void columnRemoved(TableColumnModelEvent e) {
			updateConfig();
		}

		public void columnSelectionChanged(ListSelectionEvent e) {
		}

		public void columnMarginChanged(ChangeEvent e) {
			updateConfig();
		}
	};
	
	private final TableStore store;
	
	/**
	 * Construct an instance of the table adapter for the given table and table
	 * ID. This instance can be used to serialize table view settinsg to
	 * 'tableView<ID>.xml', and to save settings changes back to it after
	 * connect() is called.
	 */
	public STableViewPersister(STable table, String configID) {
		this(table, new XmlStore(table, "tableView" + configID + ".xml"));
	}
	
	public STableViewPersister(STable table, TableStore store) {
		this.table = table;
		this.store = store;
	}
	
	/**
	 * Set the table's view settings to the last-saved state in the XML file.
	 * 
	 * If anything is wrong with deserialization, this method will catch the
	 * exception, log a message, and move on.
	 */
	public void updateTable() {
		log.println("Loading table settings from " + store);
		try {
			store.updateTable();
		} catch (Exception e) {
			log.aprintln("Error reading table settings:");
			log.aprintln(e);
		}
	}
	
	/**
	 * Updates with settings in the column model.
	 */
	public void updateConfig() {
		log.println("Saving table settings to " + store);
		try {
			store.updateStore();
		} catch (Exception e) {
			log.aprintln("Error saving table settings:");
			log.aprintln(e);
		}
	}
	
	/**
	 * Connects settings change listeners to the table so jmars.config is
	 * updated with changes in column width, visibility, and order.
	 */
	public void connect() {
		table.getModel().addTableModelListener(tableModelListener);
		table.getSorter().addListener(sorterListener);
		table.getColumnModel().addColumnModelListener(columnListener);
	}

	/**
	 * Disconnects the settings change listeners from the table.
	 */
	public void disconnect() {
		table.getModel().removeTableModelListener(tableModelListener);
		table.getSorter().removeListener(sorterListener);
		table.getColumnModel().removeColumnModelListener(columnListener);
	}
	
	/** A custom store for table settings */
	public interface TableStore {
		/** Called when a table setting changes, this should save the result of calling {@link STable#getViewSettings()}. */
		void updateTable() throws Exception;
		/** Called to restore the table settings, this should load the settings and pass them to {@link STable#setViewSettings(Map)}. */
		void updateStore() throws Exception;
	}
	
	/** The default table store is an xml file in ~/jmars */
	public static class XmlStore implements TableStore {
		// serialization mechanism
		private final STable table;
		private XStream xstream = new XStream() {
			protected boolean useXStream11XmlFriendlyMapper() {
				return true;
			}
		};

		// used to avoid unecessary saves while still catching them
		private Timer timer = new Timer();

		// the basename of the XML file to load from and save to
		private String filename;
		
		private TimerTask saveTask;
		
		private class SaveTask extends TimerTask {
			public void run() {
				log.println("Saving table settings to " + filename);
				try {
					String fullpath = Main.getJMarsPath() + filename;
					FileOutputStream fos = new FileOutputStream(fullpath);
					xstream.toXML(table.getViewSettings(), fos);
					log.println("Table settings updated");
				} catch (FileNotFoundException e) {
					log.println("Table settings could not be updated in " + filename);
				}
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						saveTask = null;
					}
				});
			}
		};
		
		public XmlStore(STable table, String filename) {
			this.table = table;
			this.filename = filename;
		}
		
		public String toString() {
			return filename;
		}
		
		public void updateTable() throws FileNotFoundException {
			String fullpath = Main.getJMarsPath() + filename;
			File file = new File(fullpath);
			if (file.exists() && file.canRead() && file.isFile()) {
				FileInputStream fis = new FileInputStream(fullpath);
				table.setViewSettings((Map) xstream.fromXML(fis));
				log.println("Table settings restored");
			} else {
				log.println("Table settings do not exist yet");
			}
		}
		
		public void updateStore() {
			// this operation is slightly expensive and we don't need to do it
			// on every change, so we wait a bit to compress rapid updates
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if (saveTask == null) {
						timer.schedule(saveTask = new SaveTask(), 1000);
					}
				}
			});
		}
	}
}
