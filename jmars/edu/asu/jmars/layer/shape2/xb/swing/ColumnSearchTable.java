package edu.asu.jmars.layer.shape2.xb.swing;

import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.swing.landmark.search.swing.MyLeftColumnHeaderRenderer;
import edu.asu.jmars.swing.landmark.search.swing.MyRightColumnHeaderRenderer;

public class ColumnSearchTable extends JTable {
	TableModel tblModel;
	private final static int PAGE_SIZE = 10;
	private DefaultTableCellRenderer rightRenderer, defaultRenderer;
	private MyLeftColumnHeaderRenderer myLeftColumnHeaderRenderer;
	private MyRightColumnHeaderRenderer myRightColumnHeaderRenderer;
	private MouseListener mymouseListener;
	private MouseMotionAdapter mymouseMotionAdapter;

	public ColumnSearchTable(ColumnSearchTableModel model) {
		super(model);
		tblModel = model;
		setToolTipText(null); // turn off tooltip for this table; we will use custom instead
		
		mymouseListener = new OnClickAction();
		mymouseMotionAdapter = new OnMouseMotion();
		
		setDragEnabled(true);

		rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		defaultRenderer = new DefaultTableCellRenderer();
		defaultRenderer.setHorizontalAlignment(SwingConstants.LEFT);
		myLeftColumnHeaderRenderer = new MyLeftColumnHeaderRenderer();
		myRightColumnHeaderRenderer = new MyRightColumnHeaderRenderer();
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getTableHeader().setReorderingAllowed(false);
		
		setRenderers();
		
		addKeyListener(new OnKeyAction());
		
		createKeybindings();
	}

	public void setRenderers() {
		removeListeners();
		removeMouseMotionListeners();
		getColumnModel().getColumn(0).setHeaderRenderer(myLeftColumnHeaderRenderer);
		getColumnModel().getColumn(0).setCellRenderer(defaultRenderer);
		addMouseListener(mymouseListener);
		addMouseMotionListener(mymouseMotionAdapter);
		getColumnModel().getColumn(1).setHeaderRenderer(myRightColumnHeaderRenderer);
		getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
	}

	private void removeListeners() {
		MouseListener[] mouseListeners = this.getMouseListeners();
		for (final MouseListener listener : mouseListeners) {
			if (listener instanceof OnClickAction) {
				this.removeMouseListener(listener);
			}
		}
	}
	
	private void removeMouseMotionListeners() {
		MouseMotionListener[] mouseMotionListeners = this.getMouseMotionListeners();
		for (final MouseMotionListener motion : mouseMotionListeners) {
			if (motion instanceof OnMouseMotion) {
				this.removeMouseMotionListener(motion);
			}
		}
	}

	private class OnClickAction extends MouseAdapter {
		@Override
		public void mouseClicked(MouseEvent e) {
			ColumnSearchTable jTable = (ColumnSearchTable) e.getSource();
			int rowIndex = jTable.rowAtPoint(e.getPoint());
			Field column = getColumnForSelectedRow(jTable, rowIndex);
			ColumnSearchPanel.INSERT(column);
		}
	}
	
	 Field getColumnForSelectedRow(ColumnSearchTable jTable, int rowIndex) {
		Field column = null;
		int rowcount = ((ColumnSearchTableModel) jTable.getModel()).getRowCount();
		if (rowIndex < rowcount && rowIndex != -1) {
			int modelRow = jTable.convertRowIndexToModel(rowIndex);
			Field field = ((ColumnSearchTableModel) jTable.getModel()).getColumnDataAt(modelRow);
			column = field;
		}
		return column;
	}

	private class OnMouseMotion extends MouseMotionAdapter {
		@Override
		public void mouseMoved(MouseEvent e) {
			ColumnSearchTable jTable = (ColumnSearchTable) e.getSource();
			int rowIndex = jTable.rowAtPoint(e.getPoint());
			try {
				int modelRow = jTable.convertRowIndexToModel(rowIndex);
				Field field = ((ColumnSearchTableModel) jTable.getModel()).getColumnDataAt(modelRow);
			} catch (RuntimeException e1) {
			}
		}
	}

	private class OnKeyAction extends KeyAdapter {
		@Override
		public void keyReleased(final java.awt.event.KeyEvent evt) {
			final int key = evt.getKeyCode();
			ColumnSearchTable jTable = (ColumnSearchTable) evt.getSource();
			if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT
					|| key == KeyEvent.VK_PAGE_UP || key == KeyEvent.VK_PAGE_DOWN) {
				int row = jTable.getSelectedRow();
			}
		}
	}

	private void createKeybindings() { // this solves JTable issue of moving to next row on ENTER
		this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
		this.getActionMap().put("Enter", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				// do on JTable enter pressed
				ColumnSearchTable jTable = (ColumnSearchTable) ae.getSource();
				int row = jTable.getSelectedRow();
				Field column = getColumnForSelectedRow(jTable, row);
				ColumnSearchPanel.INSERT(column);
			}
		});
	}


	public void setNextRowDown() {
		int rowIndex = this.getSelectedRow();
		int newIndex = rowIndex + 1;
		if (newIndex >= 0 && newIndex < this.getRowCount()) {
			this.addRowSelectionInterval(newIndex, newIndex);
			scrollToMakeRowVisible(newIndex);
		}
	}

	public void setNextRowUp() {
		int rowIndex = this.getSelectedRow();
		int newIndex = rowIndex - 1;
		if (newIndex < this.getRowCount() && newIndex >= 0) {
			this.addRowSelectionInterval(newIndex, newIndex);
			scrollToMakeRowVisible(newIndex);
		}
	}

	public void setPageDown() {
		int rowIndex = this.getSelectedRow();
		int newIndex = rowIndex + PAGE_SIZE;
		if (newIndex >= 0 && newIndex < this.getRowCount()) {
			this.addRowSelectionInterval(newIndex, newIndex);
			scrollToMakeRowVisible(newIndex);
		}
	}

	public void setPageUp() {
		int rowIndex = this.getSelectedRow();
		int newIndex = rowIndex - PAGE_SIZE;
		if (newIndex >= 0 && newIndex < this.getRowCount()) {
			this.addRowSelectionInterval(newIndex, newIndex);
			scrollToMakeRowVisible(newIndex);
		}
	}

	public void scrollToMakeRowVisible(int rowindex) {
		Rectangle cellRect = this.getCellRect(rowindex, 0, false);
		this.scrollRectToVisible(cellRect);
	}
}

