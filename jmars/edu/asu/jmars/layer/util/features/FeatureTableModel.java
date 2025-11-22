package edu.asu.jmars.layer.util.features;

import java.util.*;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import edu.asu.jmars.util.stable.ComparableTableColumn;
import edu.asu.jmars.util.stable.FilteringColumnModel;

/**
 * Presents a TableModel view of a FeatureCollection for a JTable's use.
 * All rows are shown to the JTable, but not all columns because some
 * Field objects contain data that cannot be meaningfully displayed in
 * a JTable (see 'hiddenFields' for the list that is suppressed.)
 * 
 * The TableColumnModel is modified directly instead of sending HEADER_ROW
 * events for two reasons:
 * 1) When HEADER_ROW event is received by JTable, it disposes selections,
 * which we don't want, and
 * 2) We have Field-specific styling to apply to the TableColumn objects,
 * so we have to create them.
 * 
 * The fireTableDataChanged() event is sent when the underlying
 * FeatureCollection has a change. This is relatively expensive for small
 * changes, but the worst case is a total redraw of the displayed area of
 * the JTable, which is trivial. The events on the other hand can only
 * hold a single range of values. FeatureCollection events can have hundreds
 * of disjoint ranges, meaning hundreds of events and minutes to process a
 * table update. So, we just update all cells and replace the selections.
 */
public class FeatureTableModel
	extends AbstractTableModel
	implements FeatureListener
{
	/**
	 * Fields that can be part of the FeatureCollection schema that should
	 * not be part of the TableModel columns.
	 */
	public static final Set<Field> defaultHiddenFields;
	static {
		Set<Field> hidden = new HashSet<Field>();
		hidden.add (Field.FIELD_PATH);
		defaultHiddenFields = Collections.unmodifiableSet(hidden);
	}

	private FeatureCollection fc;
	private FilteringColumnModel columnModel;
	private FeatureSelectionListener fsa;
	private int[] modelToSchema;
	private boolean sending;
	private Map<Class<?>, Comparator<?>> compMap = new HashMap<Class<?>,Comparator<?>> ();
	private final Set<Field> hiddenFields;

	/**
	 * Initializes the Fields from the FeatureCollection schema to show as columns
	 * in the TableModel, and adds this as a FeatureCollection listener.
	 */
	public FeatureTableModel(
			FeatureCollection fc,
			FilteringColumnModel columnModel,
			FeatureSelectionListener fsa)
	{
		this(fc, columnModel, fsa, defaultHiddenFields);
	}
	
	public FeatureTableModel(
			FeatureCollection fc,
			FilteringColumnModel columnModel,
			FeatureSelectionListener fsa,
			Set<Field> hiddenFields)
	{
		this.fc = fc;
		this.columnModel = columnModel;
		this.fsa = fsa;
		this.hiddenFields = new HashSet<Field>(hiddenFields);

		compMap.put (String.class, String.CASE_INSENSITIVE_ORDER);

		buildColumnLookups();
		
		// merge the fc schema with the given table column model by adding
		// columns for each field in the visible and schema sets that is not
		// already identified in the table column model, and removing each
		// column whose identifier is not in the schema and visible sets
		Set<Field> toShow = new LinkedHashSet<Field>(visibleFields (fc.getSchema()));
		for (Field f: toShow) {
			if (columnModel.getColumn(f) == null) {
				TableColumn tc = fieldToColumn(f);
				columnModel.addColumn (tc);
			}
		}
		for (TableColumn tc: new ArrayList<TableColumn>(columnModel.getAllColumns())) {
			if (!toShow.contains(tc.getIdentifier())) {
				columnModel.removeColumn(tc);
			}
		}
	}
	
	/**
	 * Filters the given List down to Field objects that are not hidden. 
	 */
	private List<Field> visibleFields (List<Field> fields) {
		List<Field> visible = new LinkedList<Field> (fields);
		visible.removeAll (hiddenFields);
		return visible;
	}

	/**
	 * Build the column lookup index from TableModel index to schema index.
	 * The fields in the FeatureCollection schema are shown in order, but
	 * some are inappropriate for a JTable and so are hidden here.
	 */
	private void buildColumnLookups () {
		List<Field> schema = fc.getSchema();
		modelToSchema = new int[visibleFields(schema).size()];
		int tableModelIndex = 0;
		int schemaIndex = 0;
		for (Field f: schema) {
			if (! hiddenFields.contains (f)) {
				modelToSchema[tableModelIndex++] = schemaIndex;
			}
			schemaIndex ++;
		}
	}

	/**
	 * Previous TableColumn model indices may become wrong after a Field is
	 * added or removed from the schema, so find and fix them.
	 */
	private void setColumnModelIndices() {
		List<Field> visible = visibleFields (fc.getSchema ());
		for (TableColumn column: columnModel.getAllColumns()) {
			int modelIndex = visible.indexOf (column.getIdentifier());
			if (column.getModelIndex() != modelIndex)
				column.setModelIndex(modelIndex);
		}
	}

	/**
	 * Constructs a new TableColumn from the given Field. The Field is set as
	 * the identifier for later use. Custom styling (such as Field-specific
	 * column sizing) should be done here.
	 */
	protected TableColumn fieldToColumn (Field f)
	{
		int modelIndex = visibleFields(fc.getSchema()).indexOf(f);
		Comparator comp = compForField (f);
		TableColumn newCol = new ComparableTableColumn(modelIndex, comp);
		newCol.setIdentifier(f);
		newCol.setHeaderValue(f.name);
		if (f == Field.FIELD_LABEL)
			newCol.setPreferredWidth(150);
		return newCol;
	}

	/**
	 * Returns a Comparator to use for the table cells of the given Field.
	 */
	private Comparator compForField (Field f) {
		if (compMap.containsKey(f.type))
			return (Comparator) compMap.get(f.type);
		else
			return null;
	}

	//
	// FeatureListener implementation
	//

	/**
	 * Notify the JTable of the add/remove/change events from the
	 * FeatureCollection. Column changes are handled directly by this method.
	 */
	public void receive( FeatureEvent e) {
		switch (e.type) {
		case FeatureEvent.ADD_FIELD:
		case FeatureEvent.REMOVE_FIELD:
			buildColumnLookups();
			setColumnModelIndices ();
			for (Iterator ai = visibleFields (e.fields).iterator(); ai.hasNext(); ) {
				if (e.type == FeatureEvent.ADD_FIELD)
					columnModel.addColumn (fieldToColumn ((Field)ai.next()));
				else {
					columnModel.removeColumn (columnModel.getColumn((Field)ai.next()));
				}
			}
			return;
		}

		// exclude the hidden fields
		if (e.fields != null) {
			List fields = new LinkedList (e.fields);
			fields.removeAll (hiddenFields);
			if (fields.size() == 0)
				return;
		}

		sending = true;
		try {
			// fire table changed
			int oldCount, newCount = fc.getFeatureCount();
			switch (e.type) {
			case FeatureEvent.ADD_FEATURE:
				oldCount = newCount - e.features.size();
				if (newCount > 0)
					fireTableRowsInserted(oldCount, newCount-1);
				break;
			case FeatureEvent.CHANGE_FEATURE:
				oldCount = newCount;
				break;
			case FeatureEvent.REMOVE_FEATURE:
				oldCount = newCount + e.features.size();
				if (oldCount > 0)
					fireTableRowsDeleted(newCount, oldCount-1);
				break;
			}
			if (newCount > 0)
				fireTableRowsUpdated(0, newCount-1);

			// update selections through feature selection listener
			fsa.setFeatureSelectionsToTable();
		} finally {
			sending = false;
		}
	} // end: receive()

	/**
	 * Return true while forwarding events from the FeatureCollection
	 * to the JTable.
	 */
	public boolean isSending () {
		return sending;
	}

	//
	// TableModel implementation
	//

	/**
	 * Returns the number of Fields in the schema of the FeatureCollection,
	 * less the number of fields being hidden.
	 */
	public int getColumnCount(){
		return modelToSchema.length;
	}

	/**
	 * returns the number of Features in the associated FeatureCollection.
	 */
	public int getRowCount(){
		return fc.getFeatures().size();
	}

	/**
	 * Returns the value at the row and column in the associated FeatureCollection.
	 */
	public Object getValueAt( int row, int col) {
		Feature feature = (Feature)fc.getFeatures().get(row);
		Field field = (Field)fc.getSchema().get(modelToSchema[col]);
		return feature.getAttribute( field);
	}

	/**
	 * Sets the feature and attribute of the FeatureCollection to the specified value.
	 * Note that we must not set off the FeatureListeners, lest we get into a loop
	 * of notifications.
	 */
	public void setValueAt( Object value, int row, int col){
		Feature feature = (Feature)fc.getFeatures().get(row);
		Field field = (Field)fc.getSchema().get(modelToSchema[col]);
		feature.setAttribute( field, value);
	}

	/**
	 * AbstractTableModel cells are not editable by default. The policy of this
	 * model is that a column is editable if the Field "editable" element is set
	 * to true.  Editors are set up for all ShapeFramework data types.  The user
	 * may override this by supplying their own editor for the column. 
	 * Editors are applied to individual columns with setCellEditor( String, CellEditor).
	 */
	public boolean isCellEditable( int row, int col){
		return ((Field)fc.getSchema().get(modelToSchema[col])).editable;
	}

	/**
	 * returns the class of the column.
	 */
	public Class getColumnClass( int col){
		return ((Field)fc.getSchema().get(modelToSchema[col])).type;
	}

	/** 
	 * Returns the name of the schema Field at the specified position.
	 */
	public String getColumnName(int col){
		return ((Field)fc.getSchema().get(modelToSchema[col])).name;
	}

	/**
	 * Returns the FeatureCollection backing this FeatureTableModel.
	 * 
	 * @return FeatureCollection backing this TableModel.
	 */
	public FeatureCollection getFeatureCollection(){
		return fc;
	}
} // end: FeatureTableModel

