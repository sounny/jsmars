package edu.asu.jmars.layer.profile.chart.table;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import edu.asu.jmars.layer.profile.swing.ProfileColorCellEditor;
import edu.asu.jmars.layer.profile.swing.ProfileColorCellRenderer;
import edu.asu.jmars.layer.profile.swing.ProfileTabelCellObject;
import edu.asu.jmars.layer.profile.swing.ProfileTableCellRenderEdit;
import edu.asu.jmars.layer.profile.swing.ToggleBooleanCellRenderer;

/**
 * A table containing the readout values from the chart. There is one row per
 * data item in the plot. The table allows editing of the plot color and selecting which series to show/hide.
 */
public class ReadoutTable extends JTable {
	private static final long serialVersionUID = 1L;
	private TableModel tblModel;
	
	public ReadoutTable(ReadoutTableModel model) {
		super(model);	
		this.tblModel = model;
			
		String osver = System.getProperty("os.name").toLowerCase();		
		int keycode = (osver.indexOf("mac") != -1) ? KeyEvent.VK_BACK_SPACE : KeyEvent.VK_DELETE;
		
		setDefaultRenderer(ProfileTabelCellObject.class, new ProfileTableCellRenderEdit());
		
		ToggleBooleanCellRenderer toggleCellRender = new ToggleBooleanCellRenderer();
		setDefaultRenderer(Boolean.class, toggleCellRender);
		
		DefaultCellEditor dce = (DefaultCellEditor)this.getDefaultEditor(Boolean.class);
	    JCheckBox cbe = (JCheckBox)dce.getComponent();
	    cbe.setSelectedIcon(toggleCellRender.getToggleON());
	    cbe.setIcon(toggleCellRender.getToggleOFF());
		
		ProfileColorCellRenderer colorRender = new ProfileColorCellRenderer();
		colorRender.setEditable(true);
		colorRender.setShowAsSwatch(false);
		colorRender.setZebra(true);
		setTypeSupport(Color.class, colorRender, new ProfileColorCellEditor());		
						
		TableColumnModel columnModel = this.getColumnModel();
	    for (int i = 0; i < columnModel.getColumnCount(); i++) {
	        TableColumn column = columnModel.getColumn(i);
	        column.setResizable(true);
	    }	
	    this.setRowSelectionAllowed(false);
	    this.getTableHeader().setReorderingAllowed(false);
	}	
	
	public void setTypeSupport (Class type, TableCellRenderer renderer, TableCellEditor editor) {
		setDefaultRenderer(type, renderer);
		setDefaultEditor(type, editor);
	}	
	
	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realIndex = columnModel.getColumn(index).getModelIndex();
				return ((ReadoutTableModel) tblModel).getColumnToolTips()[realIndex];
			}
		};
	}
}
