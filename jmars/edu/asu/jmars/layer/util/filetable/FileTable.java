package edu.asu.jmars.layer.util.filetable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.MultiFeatureCollection;
import edu.asu.jmars.ui.image.factory.ImageCatalogItem;
import edu.asu.jmars.ui.image.factory.ImageFactory;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeImages;
import edu.asu.jmars.util.History;

/**
 * Implements FileTable for ShapeLayer. This is a GUI component is basically
 * a JTable. It produces a MultiFeatureCollection for FeatureTable use.
 * The FileTable takes SingleFeatureCollection objects and displays them in a
 * tabular form. Fields displayed include:
 * <ul>
 * <li>File</li>
 * Which is the name of the file from the FeatureProvider associated
 * with the SingleFeatureCollection. If there is no FeatureProvider associated
 * with the SingleFeatureCollection, it defaults to "(default)".
 * <li>Features</li>
 * The count of the Feature objects in the FeatureProvider.
 * <li>Touched</li>
 * Signifies the fact that the SingleFeatureCollection has been modified, i.e.
 * Features have been added, removed, or modified.
 * <li>Default Flag</li>
 * A flag that indicates that the Feature has been set as the default
 * receiver for the add Feature operations.  
 * </ul>
 * 
 * @author saadat
 *
 */
public class FileTable extends JTable implements DefaultChangedListener {
	private final FileTableModel ftm = new FileTableModel();
	private final MultiFeatureCollection mfc = new MultiFeatureCollection();
	private final FileTableListSelectionListener selListener;
	
	public FileTable(History history) {
		super();
		setModel(ftm);
		ftm.setHistory(history);
		selListener = new FileTableListSelectionListener(ftm, mfc);
		getSelectionModel().addListSelectionListener(selListener);
		ftm.addDefaultChangedListener(this);

		for(int i=0; i<FileTableModel.columns.length; i++)
			if (i == FileTableModel.COL_IDX_DEFAULT_INDICATOR){
				getColumnModel().getColumn(i).setMaxWidth(((Integer)FileTableModel.columns[i][2]).intValue());
				getColumnModel().getColumn(i).setCellRenderer(new DefaultIndicatorRenderer());
			}
			else {
				getColumnModel().getColumn(i).setPreferredWidth(((Integer)FileTableModel.columns[i][2]).intValue());
			}

		/*
		 *  Change default SingleFeatureCollection on double-click on a row at the first column.
		 *  The request is transmitted to the FileTableModel which then distributes this
		 *  request to all the listeners, including the FileTable, which is a registered
		 *  listener on the FileTableModel.
		 */
		addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2){
					int col = columnAtPoint(e.getPoint());
					if (col == FileTableModel.COL_IDX_DEFAULT_INDICATOR){
						int row = rowAtPoint(e.getPoint());
						ftm.setDefaultFeatureCollection(ftm.getAll().get(row));
					}
				}
			}
		});
	}
	
	public FileTableModel getFileTableModel() {
		return ftm;
	}
	
	/** @return The selected FeatureCollection objects, in the order the user selected them. */
	public List<FeatureCollection> getSelectedFeatureCollections() {
		return new ArrayList<FeatureCollection>(selListener.getSelections());
	}
	
	/**
	 * Returns the MultiFeatureCollection produced by this FileTable. There
	 * is a single instance of this collection in the FileTable, thus
	 * subsequent calls to this method will return the same collection.
	 * This MultiFeatureCollection is automatically kept up to date with the
	 * current selections in the FileTable. This MultiFeatureCollection is
	 * also kept up to date with the current default SingleFeatureCollection as
	 * selected by the user.
	 */
	public MultiFeatureCollection getMultiFeatureCollection(){
		return mfc;
	}

	/*
	public class MFCUpdateListener implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting())
				return;

			Set selected = new HashSet();
			int[] selectedRows = getSelectedRows();
			for(int i=0; i<selectedRows.length; i++)
				selected.add(ftm.get(selectedRows[i]));

			Set existing = new HashSet(mfc.getSupportingFeatureCollections());
			Set removeSet = new HashSet(existing);
			removeSet.retainAll(selected);
			Set addSet = new HashSet(selected);
			addSet.removeAll(existing);

			for(Iterator i=removeSet.iterator(); i.hasNext(); ){
				mfc.removeFeatureCollection(i.next());
			}
			for(Iterator i=addSet.iterator(); i.hasNext(); ){
				mfc.addFeatureCollection(i.next());
			}
			// NOTE: There is not deselect event.
		}
	}
	*/

	/**
	 * Tie-up class that links the selection made in the FileTable to the
	 * contents of a MultiFeatureCollection. In other words, this class
	 * keeps the MultiFeatureCollection up to date.
	 * 
	 * @author saadat
	 *
	 */
	public static class FileTableListSelectionListener implements ListSelectionListener {
		/**
		 * Tracks which collections are selected in the file table; we do not simply
		 * use the list of collections in the MultiFeatureCollection so that it can
		 * be a superset of the FileTable contents, and also this retains the order of
		 * selection which affects the order of columns in the feature table.
		 */
		private final Set<FeatureCollection> lastSelections = new LinkedHashSet<FeatureCollection>();
		private final FileTableModel ftm;
		private final MultiFeatureCollection mfc;

		/**
		 * Constructs a FileTableListSelectionListener which ties the FileTable, 
		 * FileTableModel, the ListSelectionModel on the FileTable, and the
		 * MultiFeatureCollection.
		 * 
		 * @param ftm FileTableModel to get the selection data from.
		 * @param mfc MultiFeatureCollection to update in response to user selection.
		 */
		public FileTableListSelectionListener(FileTableModel ftm, MultiFeatureCollection mfc){
			this.ftm = ftm;
			this.mfc = mfc;
		}
		
		public List<FeatureCollection> getSelections() {
			return new ArrayList<FeatureCollection>(lastSelections);
		}
		
		/**
		 * Returns an array of row indices corresponding to the rows selected in
		 * the table. The array is guaranteed to be non-null.
		 * 
		 * @param e ListSelectionEvent to get the ListSelectionModel from.
		 * @return A non-null array of user selected indices.
		 */
		private int[] getSelectedRows(ListSelectionEvent e){
			ListSelectionModel lsm = (ListSelectionModel)e.getSource();
			if (lsm.isSelectionEmpty())
				return new int[0];

			int count = 0;
			for(int i=lsm.getMinSelectionIndex(); i<=lsm.getMaxSelectionIndex(); i++){
				if (lsm.isSelectedIndex(i))
					count++;
			}

			int[] selected = new int[count];
			int j = 0;
			for(int i=lsm.getMinSelectionIndex(); i<=lsm.getMaxSelectionIndex(); i++){
				if (lsm.isSelectedIndex(i))
					selected[j++] = i;
			}

			return selected;
		}

		/**
		 * ListSelectionListener interface realization method. Listens to ListSelectionEvents
		 * updates the MultiFeatureCollection appropriately. In order to get update the
		 * MultiFeatureCollection, it has to query the FileTableModel to get the 
		 * SingleFeatureCollections corresponding to the user selected rows.
		 */
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting())
				return;
			if (!(e.getSource() instanceof ListSelectionModel))
				return;
			
			int[] selectedRows = getSelectedRows(e);
			Set<FeatureCollection> selected = new LinkedHashSet<FeatureCollection>();
			for(int i=0; i<selectedRows.length; i++) {
				selected.add(ftm.get(selectedRows[i]));
			}
			
			for (FeatureCollection fc: new ArrayList<FeatureCollection>(lastSelections)) {
				if (!selected.contains(fc)) {
					mfc.removeFeatureCollection(fc);
					lastSelections.remove(fc);
				}
			}
			
			for (FeatureCollection fc: selected) {
				if (!lastSelections.contains(fc)) {
					mfc.addFeatureCollection(fc);
					lastSelections.add(fc);
				}
			}
		}
	}
	
	/**
	 * Tie-up class that links the defaultFeatureCollection selected by
	 * the user to the MultiFeatureCollection. This default value is
	 * produced by the TableModel as a result of UI interaction with
	 * the FileTable.
	 */
	public void defaultChanged(DefaultChangedEvent e) {
		mfc.setDefaultFeatureCollection(e.fc);
	};

	/**
	 * Renderer for the "default" flagging column. It displays a bullet when the
	 * source bullet image is available, or a "*" if the image is unavailable.
	 *  
	 * @author saadat
	 *
	 */
	static class DefaultIndicatorRenderer extends JLabel implements TableCellRenderer {
		private ImageIcon defaultIcon = null;

		public DefaultIndicatorRenderer(){
			super();
			setOpaque(true);
			setHorizontalAlignment(CENTER);
			setVerticalAlignment(CENTER);           
			Color imgColor = ((ThemeImages) GUITheme.get("images")).getFill();
			Image fileimg = ImageFactory.createImage(ImageCatalogItem.ACTIVE_FILE_IMG.withDisplayColor(imgColor));
					        					       
			defaultIcon = new ImageIcon(fileimg);			
		}
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
			setFont(table.getFont());
			if (isSelected){
				setBackground(table.getSelectionBackground());
				setForeground(table.getSelectionForeground());
			}
			else {
				setBackground(table.getBackground());
				setForeground(table.getForeground());
			}

			boolean isDefault = ((Boolean)value).booleanValue();
			if (isDefault){
				setText(defaultIcon==null? "*": "");
				setIcon(defaultIcon==null? null: defaultIcon);
			}
			else {
				setText(" ");
				setIcon(null);
			}

			if (defaultIcon != null)
				setSize(defaultIcon.getIconWidth(), defaultIcon.getIconHeight());

			return this;
		}
	}
	
	/** @return a new set of Field instances from each selected File, added in table row order. */
	public Set<Field> getSelectedFileFields() {
		Set<Field> fields = new LinkedHashSet<Field>();
		for (int idx: getSelectedRows()) {
			FeatureCollection fc = ftm.get(idx);
			fields.addAll(fc.getSchema());
		}
		return fields;
	}
}
