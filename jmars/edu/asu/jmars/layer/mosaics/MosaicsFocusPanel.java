package edu.asu.jmars.layer.mosaics;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureEvent;
import edu.asu.jmars.layer.util.features.FeatureListener;
import edu.asu.jmars.layer.util.features.FeatureSelectionListener;
import edu.asu.jmars.layer.util.features.FeatureTableModel;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.FilteringColumnModel;

public class MosaicsFocusPanel extends FocusPanel {
	final MosaicsLView lview;
	STable table;
	JScrollPane tableSp;
	FeatureTableModel tableModel;
	FeatureSelectionListener selectionListener;
	
	public MosaicsFocusPanel(MosaicsLView parent) {
		super(parent, true);
		lview = parent;
		layoutComponents();
	}
	
	private void layoutComponents(){
		JPanel contents = new JPanel();
		contents.setLayout(new BorderLayout());
		contents.add(createTable(), BorderLayout.CENTER);
		add("Contents", contents);
	}
	
	private JScrollPane createTable(){
		table = new STable();
		table.setDescription("Mosaic");
		table.setAutoCreateColumnsFromModel(false);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		FeatureCollection fc = ((MosaicsLayer)lview.getLayer()).getFeatures();
		selectionListener = new FeatureSelectionListener(table, fc, lview.layer.selections);
		tableModel = new ROFeatureTableModel(fc, (FilteringColumnModel)table.getColumnModel(), selectionListener);
		table.setUnsortedTableModel(tableModel);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		fc.addListener(new FeatureListener(){
			public void receive(FeatureEvent e) {
				tableModel.receive(e);
			}
		});
		TableColumn tc = table.getColumn(FeatureProviderWMS.FIELD_ABSTRACT);
		if (tc != null)
			tc.setPreferredWidth(256);
		
		tableSp = new JScrollPane(table);
		installContextMenuOnFeatureTable(table, tableSp);
		
		return tableSp;
	}
	
	private void installContextMenuOnFeatureTable(
			final STable featureTable,
			final JScrollPane featureTableScrollPane)
	{
		final FeatureCollection fc = ((FeatureTableModel)featureTable.getUnsortedTableModel()).getFeatureCollection();

		featureTable.addMouseListener(new MouseAdapter(){
			public void mouseClicked(MouseEvent e){
				if (SwingUtilities.isRightMouseButton(e) && e.getClickCount() == 1){
					int row = featureTable.rowAtPoint(e.getPoint());
					
					if (row < 0 || row >= featureTable.getModel().getRowCount())
						return;
					
					featureTable.getSelectionModel().setSelectionInterval(row, row);
					int[] selectedRows = new int[]{ row }; //featureTable.getSelectedRows();
					
					JPopupMenu popup = new JPopupMenu();
					for(int i=0; i<selectedRows.length; i++){
						Feature f = fc.getFeature(featureTable.getSorter().unsortRow(selectedRows[i]));
						popup.add(lview.new CenterAtAction(f));
						popup.add(lview.new LoadMosaicAction(f));
						popup.add(lview.new ViewCitationAtAction(Util.getDisplayFrame(MosaicsFocusPanel.this), f));
					}

					// bring up the popup, but be sure it goes to the cursor position of 
					// the PANEL, not the position in the table.  If we don't do this, then
					// for a large table, the popup will try to draw itself beyond the 
					// screen.
					Point2D p = getScrollPaneRelativeCoordinates(e.getPoint(), featureTableScrollPane);
					if (selectedRows.length > 0)
						popup.show( featureTableScrollPane, (int)p.getX(), (int)p.getY());
				}
			}
			public void mousePressed(MouseEvent e){}
		});
	}

	/**
	 * Compute the specified table coordinates to scroll-pane viewport 
	 * relative coordinates. This is useful for displaying popup menus on a 
	 * JTable which is enclosed in a JScrollPane where the mouse click
	 * point has been received via a MouseEvent.
	 * 
	 * @param p Point in the JTable coordinates.
	 * @param sp Scrollpane containing the JTable.
	 * @return Point in JScrollPane viewport relative coordinates.
	 */
	private Point2D getScrollPaneRelativeCoordinates(Point2D p, JScrollPane sp){
		JViewport vp = sp.getViewport();
		return new Point2D.Double(
				p.getX() - vp.getViewPosition().x,
				p.getY() - vp.getViewPosition().y);
	}
	
	private static class ROFeatureTableModel extends FeatureTableModel {
		public ROFeatureTableModel(FeatureCollection fc,
				FilteringColumnModel columnModel, FeatureSelectionListener fsa) {
			super(fc, columnModel, fsa, combine(defaultHiddenFields,
					Collections.singleton(FeatureProviderWMS.FIELD_MAP_SOURCE)));
		}

		public boolean isCellEditable(int row, int col) {
			return false;
		}
		
		private static Set<Field> combine(Set<Field> s1, Set<Field> s2){
			Set<Field> result = new HashSet<Field>();
			result.addAll(s1);
			result.addAll(s2);
			return result;
		}
	}
}
