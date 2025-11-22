package edu.asu.jmars.layer.profile.manager;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import edu.asu.jmars.layer.profile.swing.BooleanTableCellRender;
import edu.asu.jmars.layer.profile.swing.ButtonColumn;
import edu.asu.jmars.layer.profile.swing.ProfileColorCellEditor;
import edu.asu.jmars.layer.profile.swing.ProfileColorCellRenderer;
import edu.asu.jmars.layer.profile.swing.ProfileTabelCellObject;
import edu.asu.jmars.layer.profile.swing.ProfileTableCellRenderEdit;
import edu.asu.jmars.layer.profile.swing.XTableColumnModel;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;

/**
 * A table that manages Profiles drawn in Main view.
 * The table allows editing of the profile line color, changing name, deleting and selecting which profiles to plot in chart.
 */
public class ProfileManagerTable extends JTable {
	private static final long serialVersionUID = 1L;
	private TableModel tblModel;
	private final XTableColumnModel xcolumnModel = new XTableColumnModel();
	private Color bordercolor = ThemeProvider.getInstance().getAction().getBorder();
	
	
	
	public ProfileManagerTable(ProfileManagerTableModel model) {
		super(model);		
		this.tblModel = model;	
		this.setColumnModel(xcolumnModel);
		this.createDefaultColumnsFromModel();		
		String osver = System.getProperty("os.name").toLowerCase();		
		int keycode = (osver.indexOf("mac") != -1) ? KeyEvent.VK_BACK_SPACE : KeyEvent.VK_DELETE;
		//"trash" column
		setTypeSupport(ButtonColumn.class, new ButtonColumn(this, keycode, true, SwingConstants.CENTER), 
				new ButtonColumn(this, keycode, true, SwingConstants.CENTER));				
		
		setDefaultRenderer(Boolean.class, new BooleanTableCellRender());
		
		setDefaultRenderer(String.class, new HoverWithIconRenderer());
		
		setDefaultRenderer(ProfileTabelCellObject.class, new ProfileTableCellRenderEdit());
		
		ProfileColorCellRenderer colorRender = new ProfileColorCellRenderer();
		colorRender.setEditable(true);
		colorRender.setShowAsSwatch(false);
		setTypeSupport(Color.class, colorRender, new ProfileColorCellEditor());		
		
		TableColumnModel columnModel = this.getColumnModel();
	    for (int i = 0; i < columnModel.getColumnCount(); i++) {
	        TableColumn column = columnModel.getColumn(i);
	        column.setResizable(true);
	    }	
	    this.setRowSelectionAllowed(true);
	    this.getTableHeader().setReorderingAllowed(false);
	}	
	
	public void setTypeSupport (Class type, TableCellRenderer renderer, TableCellEditor editor) {
		setDefaultRenderer(type, renderer);
		setDefaultEditor(type, editor);
	}	
	
	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component result = super.prepareRenderer(renderer, row, column);
		if (result instanceof JComponent) {
			JComponent jc = (JComponent)result;
			jc.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, bordercolor));
			
	     }
		return result;
	}
	
	
	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realIndex = columnModel.getColumn(index).getModelIndex();
				return ((ProfileManagerTableModel) tblModel).getColumnToolTips()[realIndex];
			}
		};
	}
//SHOW_HIDE(0),  TITLE(1),  COLOR(2), TITLE_AND_COLOR(3), DISTANCE(4), LATLON_COORDS(5), REMOVE(6) 
	public void updateColumnsVisibility(ProfileManagerMode mode) {
		if (mode == ProfileManagerMode.MANAGE) { // no checkbox selection; hide that column
			TableColumn colSelect = xcolumnModel.getColumnByModelIndex(0);
			xcolumnModel.setColumnVisible(colSelect, false);
			
			TableColumn colTitleAndColor = xcolumnModel.getColumnByModelIndex(3);
			xcolumnModel.setColumnVisible(colTitleAndColor, false);
			
		} else if (mode == ProfileManagerMode.SELECT_MANY) { // with checkbox selection; show that column
			TableColumn colSelect = xcolumnModel.getColumnByModelIndex(0);
			xcolumnModel.setColumnVisible(colSelect, true);
			
			TableColumn colTitleAndColor = xcolumnModel.getColumnByModelIndex(3); //show Name followed by color
			xcolumnModel.setColumnVisible(colTitleAndColor, true);
			
			TableColumn colDel = xcolumnModel.getColumnByModelIndex(6);  //no delete - hide that column
			xcolumnModel.setColumnVisible(colDel, false);
			
			TableColumn colTitle = xcolumnModel.getColumnByModelIndex(1);  //no title or color - just show Name-and-Color
			xcolumnModel.setColumnVisible(colTitle, false);	
			
			TableColumn colColor = xcolumnModel.getColumnByModelIndex(2); 
			xcolumnModel.setColumnVisible(colColor, false);			
		}
	}
}
