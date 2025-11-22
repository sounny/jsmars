package edu.asu.jmars.layer.util.features;

import java.awt.Color;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import edu.asu.jmars.layer.shape2.ShapeLayer;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.History;
import edu.asu.jmars.util.LineType;
import edu.asu.jmars.util.ListType;
import edu.asu.jmars.util.ObservableSet;
import edu.asu.jmars.util.stable.ListTypeCellEditor;
import edu.asu.jmars.util.stable.ColorCellEditor;
import edu.asu.jmars.util.stable.ColorCellRenderer;
import edu.asu.jmars.util.stable.FillStyleCellEditor;
import edu.asu.jmars.util.stable.FillStyleTableCellRenderer;
import edu.asu.jmars.util.stable.FilteringColumnModel;
import edu.asu.jmars.util.stable.LineTypeCellEditor;
import edu.asu.jmars.util.stable.LineTypeTableCellRenderer;

/**
 * Produces an STable for viewing features. Editors and renderers are added for
 * color and line type. Column auto resizing is disabled. A FeatureTableModel is
 * created around the given FeatureCollection. A FeatureSelectionListener is
 * added to keep the STable's ListSelectionModel and the FIELD_SELECTED
 * attributes on Features in the FeatureCollection in sync. This will listen on
 * the FeatureCollection and send events on to the FeatureTableModel and then
 * the FeatureSelectionListener.
 */
public class FeatureTableAdapter implements FeatureListener
{
	private STable fst;
	private FeatureCollection fc;
	private FeatureSelectionListener fsl;
	private FeatureTableModel ftm;
	private History history;
	
	private ShapeLayer layer;	
	
	public FeatureTableAdapter (FeatureCollection fc, ObservableSet<Feature> selections, History history, ShapeLayer sl) {
		this.fc = fc;
		this.history = history;
		this.layer = sl;	
		
		fst = new STable() {
			// marks a history frame when the table cell editor is used
			public void editingStopped(ChangeEvent e) {
				FeatureTableAdapter.this.history.mark();
				super.editingStopped(e);
			}
		};
		
		fst.setAutoCreateColumnsFromModel(false);
		fst.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		fst.setShowGrid(true);	
		fst.setNullDisplayValue("Default");
		boolean isColorCellEditable = true;
		fst.setTypeSupport(Color.class, new ColorCellRenderer(isColorCellEditable), new ColorCellEditor());
		fst.setTypeSupport(LineType.class, new LineTypeTableCellRenderer(), new LineTypeCellEditor());
		
		fst.setTypeSupport(FillStyle.class, new FillStyleTableCellRenderer(), new FillStyleCellEditor());
		
		fst.setTypeSupport(ListType.class, new DefaultTableCellRenderer(), new ListTypeCellEditor(layer));
		fsl = new FeatureSelectionListener (fst, fc, selections);
		ftm = new FeatureTableModel (fc, (FilteringColumnModel)fst.getColumnModel(), fsl);
		fst.setUnsortedTableModel(ftm);	
		fsl.setFeatureSelectionsToTable();	
		// This class receives events on behalf of the table and selection
		// listener and forwards to guarrantee that the table gets them first.
		fc.addListener (this);
	}

	/**
	 * @return The STable component produced by this instance.
	 */
	public STable getTable () {
		return fst;
	}

	/**
	 * Sends incoming events from the FeatureCollection on to the FeatureTableModel
	 * and FeatureSelectionListener (in that order, since JTable can't make selections
	 * on data that doesn't exist yet.)
	 */
	public void receive (FeatureEvent event) {
		ftm.receive(event);
	}

	/**
	 * Clear various listeners as part of the cleanup.
	 */
	public void disconnect() {
		fc.removeListener (this);
		fsl.disconnect ();
	}
}