package edu.asu.jmars.layer.profile.config;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import edu.asu.jmars.layer.profile.swing.ButtonColumn;
import edu.asu.jmars.layer.profile.swing.LabelRendererLeftAlign;
import edu.asu.jmars.layer.profile.swing.TableHeaderLeftAligned;
import edu.asu.jmars.layer.profile.swing.XTableColumnModel;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;

public class SelectedNumSourcesTable extends JTable {
	private TableModel tblModel;
	private final XTableColumnModel xcolumnModel = new XTableColumnModel();
	private Color bordercolor = ThemeProvider.getInstance().getAction().getBorder();
	private boolean isMultiSelect;

	public SelectedNumSourcesTable(SelectedNumSourcesTableModel model) {
		super(model);
		this.tblModel = model;
		this.setColumnModel(xcolumnModel);
		this.createDefaultColumnsFromModel();	
		String osver = System.getProperty("os.name").toLowerCase();
		int keycode = (osver.indexOf("mac") != -1) ? KeyEvent.VK_BACK_SPACE : KeyEvent.VK_DELETE;

		// "trash" column
		setTypeSupport(ButtonColumn.class, new ButtonColumn(this, keycode, true, SwingConstants.RIGHT),
				new ButtonColumn(this, keycode, true, SwingConstants.RIGHT));

		setDefaultRenderer(String.class, new LabelRendererLeftAlign());		
		
		TableColumnModel columnModel = this.getColumnModel();

		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			TableColumn column = columnModel.getColumn(i);
			column.setResizable(true);
			column.setHeaderRenderer(new TableHeaderLeftAligned());
		}
		this.setRowSelectionAllowed(false);
		this.getTableHeader().setReorderingAllowed(false);
	}

	public void setTypeSupport(Class type, TableCellRenderer renderer, TableCellEditor editor) {
		setDefaultRenderer(type, renderer);
		setDefaultEditor(type, editor);
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component result = super.prepareRenderer(renderer, row, column);
		if (result instanceof JComponent) {
			if (column == 0 && (this.isMultiSelect == true)) {
				((JComponent) result).setBorder(BorderFactory
						.createCompoundBorder(new MatteBorder(1, 1, 1, 0, bordercolor), new EmptyBorder(5, 5, 5, 5)));
				((JComponent) result).setToolTipText(getValueAt(row, column).toString());
			} else if (column == 0 && (this.isMultiSelect == false)) {
				((JComponent) result).setBorder(BorderFactory
						.createCompoundBorder(new MatteBorder(1, 1, 1, 1, bordercolor), new EmptyBorder(5, 5, 5, 5)));
				((JComponent) result).setToolTipText(getValueAt(row, column).toString());
			} else if (column == (this.tblModel.getColumnCount() - 1)) {
				((JComponent) result).setBorder(BorderFactory
						.createCompoundBorder(new MatteBorder(1, 0, 1, 1, bordercolor), new EmptyBorder(5, 5, 5, 5)));
			} else {
				((JComponent) result).setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, bordercolor));
			}
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
				return ((SelectedNumSourcesTableModel) tblModel).getColumnToolTips()[realIndex];
			}
		};
	}
	
	// TITLE(0), REMOVE(1)
	public void updateColumnsVisibility(boolean isMultiSelect) {
		// one numeric source configuration, hide "Remove"/many num sources - show
		// "Remove"
		this.isMultiSelect = isMultiSelect;
		TableColumn colRemove = xcolumnModel.getColumnByModelIndex(1);
		if (colRemove != null) {
			xcolumnModel.setColumnVisible(colRemove, isMultiSelect);
		}
	}
 }