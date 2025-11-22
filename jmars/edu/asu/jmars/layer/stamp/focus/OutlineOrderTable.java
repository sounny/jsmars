package edu.asu.jmars.layer.stamp.focus;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import edu.asu.jmars.layer.profile.swing.ButtonColumn;
import edu.asu.jmars.layer.profile.swing.LabelRendererLeftAlign;
import edu.asu.jmars.layer.profile.swing.TableHeaderCenterAligned;
import edu.asu.jmars.layer.profile.swing.TableHeaderLeftAligned;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;

public class OutlineOrderTable extends JTable {
	private TableModel tblModel;
	private Color bordercolor = ThemeProvider.getInstance().getAction().getBorder();
	private boolean isMultiSelect;
	private int colUPIndex = 3;
	private int colDOWNIndex = 4;

	public OutlineOrderTable(OutlineOrderTableModel model) {
		super(model);
		this.tblModel = model;
		this.isMultiSelect = true;
		this.createDefaultColumnsFromModel();
		String osver = System.getProperty("os.name").toLowerCase();
		int keycode = (osver.indexOf("mac") != -1) ? KeyEvent.VK_BACK_SPACE : KeyEvent.VK_DELETE;

		// "trash", "move up", "move down" columns
		setTypeSupport(ButtonColumn.class, new ButtonColumn(this, keycode, true, SwingConstants.CENTER),
				new ButtonColumn(this, keycode, true, SwingConstants.CENTER));

		setDefaultRenderer(String.class, new LabelRendererLeftAlign());

		TableColumnModel columnModel = this.getColumnModel();

		TableColumn col0 = columnModel.getColumn(0);    //grab-bars
		col0.setHeaderRenderer(new TableHeaderLeftAligned());
		col0.setMaxWidth(35);
		
		TableColumn col1 = columnModel.getColumn(1);    //Field name
		col1.setHeaderRenderer(new TableHeaderLeftAligned());
		
		TableColumn col2 = columnModel.getColumn(2);    //direction of sorting (asc/desc)
		col2.setHeaderRenderer(new TableHeaderCenterAligned());
		
		TableColumn col3 = columnModel.getColumn(3);    //move up
		col3.setHeaderRenderer(new TableHeaderLeftAligned());
		col3.setMaxWidth(50);
		
		TableColumn col4 = columnModel.getColumn(4);    //move down
		col4.setHeaderRenderer(new TableHeaderLeftAligned());
		col4.setMaxWidth(50);
		
		TableColumn col5 = columnModel.getColumn(5);    //trash
		col5.setHeaderRenderer(new TableHeaderLeftAligned());
		col5.setMaxWidth(30);
		
		this.setRowSelectionAllowed(true);
		this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		this.getTableHeader().setReorderingAllowed(false);

		MouseHandler mymousehandler = new MouseHandler(this, (IReorderable) tblModel);
		this.addMouseListener(mymousehandler);
		this.addMouseMotionListener(mymousehandler);
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
						.createCompoundBorder(new MatteBorder(0, 1, 1, 0, bordercolor), new EmptyBorder(5, 5, 5, 5)));
			}  else if (column == (this.tblModel.getColumnCount() - 1)) {
				((JComponent) result).setBorder(BorderFactory
						.createCompoundBorder(new MatteBorder(0, 0, 1, 1, bordercolor), new EmptyBorder(5, 5, 5, 5)));
			} else {
				((JComponent) result).setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, bordercolor));
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
				return ((OutlineOrderTableModel) tblModel).getColumnToolTips()[realIndex];
			}
		};
	}

	private class MouseHandler extends MouseInputAdapter {
		private Integer row = null;
		private final WeakReference<JTable> table;
		private final WeakReference<IReorderable> tableModel;

		public MouseHandler(JTable table, IReorderable model) {
			this.table = new WeakReference<>(table);
			this.tableModel = new WeakReference<>(model);
		}

		@Override
		public void mousePressed(MouseEvent event) {
			JTable table;
			if ((table = this.table.get()) == null) {
				return;
			}
			int viewRowIndex = table.rowAtPoint(event.getPoint());
			row = table.convertRowIndexToModel(viewRowIndex);
			if (viewRowIndex >= 0 && viewRowIndex <= tblModel.getRowCount()-1) {
				setRowSelectionInterval(viewRowIndex, viewRowIndex);
			}
		}

		@Override
		public void mouseDragged(MouseEvent event) {
			JTable table;
			IReorderable tableModel;
			if ((table = this.table.get()) == null || (tableModel = this.tableModel.get()) == null) {
				return;
			}

			int viewRowIndex = table.rowAtPoint(event.getPoint());
			int currentRow = table.convertRowIndexToModel(viewRowIndex);
			if (viewRowIndex >= 0 && viewRowIndex <= tblModel.getRowCount()-1) {
				setRowSelectionInterval(viewRowIndex, viewRowIndex);
			}

			if (row == null || currentRow == row) {
				return;
			}

			int from = row;
			int to = currentRow;
			tableModel.reorder(from, to);
			row = currentRow;
		}
		
		@Override
		public void mouseClicked(MouseEvent e) {
			int rowIndex = rowAtPoint(e.getPoint());
			int columnIndex = columnAtPoint(e.getPoint());
			if (columnIndex == colUPIndex) {
				if (rowIndex > 0) {
					int to = rowIndex - 1;
					((IReorderable) tblModel).reorder(rowIndex, to);
					setRowSelectionInterval(to, to);
				}
			} else if (columnIndex == colDOWNIndex) {
				if (rowIndex < (tblModel.getRowCount()-1)) {										
					int to = rowIndex +1;
					((IReorderable) tblModel).reorder(rowIndex, to);
					setRowSelectionInterval(to, to);
				}				
			}
		}
	}

}