package edu.asu.jmars.swing.landmark.search.swing;

import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import edu.asu.jmars.layer.nomenclature.NomenclatureLView.MarsFeature;
import edu.asu.jmars.swing.landmark.search.LandmarkSearchPanel;
import edu.asu.jmars.swing.landmark.search.LandmarkSearchPanel.SearchMode;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.TableCellBalloonTip;
import net.java.balloontip.styles.EdgedBalloonStyle;

public class LandmarkSearchTable extends JTable {
	TableModel tblModel;
	JTextField landmarkSearchInput;
	private TableCellBalloonTip myBalloonTip;
	private static final Color TOOLTIP_BACK = ThemeProvider.getInstance().getBackground().getContrast();
	private final static int PAGE_SIZE = 10;
	private LeftColumnRenderer leftRenderer;
	private DefaultTableCellRenderer rightRenderer, defaultRenderer;
	private MyLeftColumnHeaderRenderer myLeftColumnHeaderRenderer;
	private MyRightColumnHeaderRenderer myRightColumnHeaderRenderer;
	private MouseListener mymouseListener;
	private MouseMotionAdapter mymouseMotionAdapter;

	
	public LandmarkSearchTable(LandmarkSearchTableModel model, JTextField txt) {
		super(model);
		tblModel = model;
		landmarkSearchInput = txt;
		setToolTipText(null); // turn off tooltip for this table; we will use custom instead
		
		mymouseListener = new OnClickAction();
		mymouseMotionAdapter = new OnMouseMotion();
		
		leftRenderer = new LeftColumnRenderer(this);
		rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		defaultRenderer = new DefaultTableCellRenderer();
		defaultRenderer.setHorizontalAlignment(SwingConstants.LEFT);
		myLeftColumnHeaderRenderer = new MyLeftColumnHeaderRenderer();
		myRightColumnHeaderRenderer = new MyRightColumnHeaderRenderer();
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getTableHeader().setReorderingAllowed(false);
		
		setRenderers(SearchMode.LANDMARKS);
		
		addKeyListener(new OnKeyAction());
		
		createKeybindings();
		createTooltipUI();
	}

	public void setRenderers(SearchMode searchmode) {
		removeListeners();
		removeMouseMotionListeners();
		if (searchmode == SearchMode.LANDMARKS) {
			getColumnModel().getColumn(0).setHeaderRenderer(myLeftColumnHeaderRenderer);
			getColumnModel().getColumn(0).setCellRenderer(defaultRenderer);
			addMouseListener(mymouseListener);
			addMouseMotionListener(mymouseMotionAdapter);
		} else if (searchmode == SearchMode.BOOKMARKS) {
			//listeners will be added dynamically by leftRenderer
			leftRenderer.addListeners(this);					
			getColumnModel().getColumn(0).setHeaderRenderer(myLeftColumnHeaderRenderer);
			getColumnModel().getColumn(0).setCellRenderer(leftRenderer);
		}
			getColumnModel().getColumn(1).setHeaderRenderer(myRightColumnHeaderRenderer);
			getColumnModel().getColumn(1).setCellRenderer(rightRenderer);
	}

	private void removeListeners() {
		MouseListener[] mouseListeners = this.getMouseListeners();
		for (final MouseListener listener : mouseListeners) {
			if (listener instanceof LeftColumnRenderer.MouseEventReposter) {
				this.removeMouseListener(listener);
			} else if (listener instanceof OnClickAction) {
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

	private class OnClickAction implements MouseListener {
		@Override
		public void mouseClicked(MouseEvent e) {
			hideTooltip();
			LandmarkSearchTable jTable = (LandmarkSearchTable) e.getSource();
			int rowIndex = jTable.rowAtPoint(e.getPoint());
			String landmark = getLandmarkForSelectedRow(jTable, rowIndex);
			LandmarkSearchPanel.GOTO(landmark);
		}

		@Override
		public void mousePressed(MouseEvent e) {
			hideTooltip();
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			hideTooltip();
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			hideTooltip();
		}

		@Override
		public void mouseExited(MouseEvent e) {
			hideTooltip();
		}
	}

	private class OnMouseMotion extends MouseMotionAdapter {
		@Override
		public void mouseMoved(MouseEvent e) {
			String tip = "";
			LandmarkSearchTable jTable = (LandmarkSearchTable) e.getSource();
			Point whereToShow = e.getPoint();
			int rowIndex = jTable.rowAtPoint(e.getPoint());
			try {
				int modelRow = jTable.convertRowIndexToModel(rowIndex);
				MarsFeature mf = ((LandmarkSearchTableModel) jTable.getModel()).getLandmarkDataAt(modelRow);
				tip = mf.getPopupInfo(true);
			} catch (RuntimeException e1) {
			}
			showTooltip(tip, whereToShow);
		}
	}

	private class OnKeyAction extends KeyAdapter {
		@Override
		public void keyReleased(final java.awt.event.KeyEvent evt) {
			final int key = evt.getKeyCode();
			LandmarkSearchTable jTable = (LandmarkSearchTable) evt.getSource();
			if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN || key == KeyEvent.VK_LEFT || key == KeyEvent.VK_RIGHT
					|| key == KeyEvent.VK_PAGE_UP || key == KeyEvent.VK_PAGE_DOWN) {
				int row = jTable.getSelectedRow();
				hideTooltip();
			}
		}

		@Override
		public void keyPressed(final java.awt.event.KeyEvent evt) {
		}
	}

	public String getLandmarkForSelectedRow(LandmarkSearchTable jTable, int rowIndex) {
		String landmark = "";
		int rowcount = ((LandmarkSearchTableModel) jTable.getModel()).getRowCount();
		if (rowIndex < rowcount && rowIndex != -1) {
			int modelRow = jTable.convertRowIndexToModel(rowIndex);
			MarsFeature mf = ((LandmarkSearchTableModel) jTable.getModel()).getLandmarkDataAt(modelRow);
			landmark = mf.name;
		}
		return landmark;
	}

	private void createKeybindings() { // this solves JTable issue of moving to next row on ENTER
		this.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
				.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
		this.getActionMap().put("Enter", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent ae) {
				// do on JTable enter pressed
				hideTooltip();
				LandmarkSearchTable jTable = (LandmarkSearchTable) ae.getSource();
				int row = jTable.getSelectedRow();
				String landmark = getLandmarkForSelectedRow(jTable, row);
				LandmarkSearchPanel.GOTO(landmark);
			}
		});
	}

	private void createTooltipUI() {
		JLabel dummy = new JLabel();
		EdgedBalloonStyle style = new EdgedBalloonStyle(TOOLTIP_BACK,
				ThemeProvider.getInstance().getBackground().getBorder());
		myBalloonTip = new TableCellBalloonTip(this, dummy, 0, 0, style, BalloonTip.Orientation.LEFT_ABOVE,
				BalloonTip.AttachLocation.ALIGNED, 40, 10, false);
	}

	private void showTooltip(String tip, Point whereToShow) {
		myBalloonTip.setAttachedComponent(this);
		myBalloonTip.setTextContents(tip);
		myBalloonTip.setCellPosition(this.rowAtPoint(whereToShow), 0);
		myBalloonTip.setVisible(true);
	}

	public void hideTooltip() {
		if (myBalloonTip != null && myBalloonTip.isVisible()) {
			myBalloonTip.setVisible(false);
		}
	}

	public void setNextRowDown() {
		hideTooltip();
		int rowIndex = this.getSelectedRow();
		int newIndex = rowIndex + 1;
		if (newIndex >= 0 && newIndex < this.getRowCount()) {
			this.addRowSelectionInterval(newIndex, newIndex);
			scrollToMakeRowVisible(newIndex);
		}
	}

	public void setNextRowUp() {
		hideTooltip();
		int rowIndex = this.getSelectedRow();
		int newIndex = rowIndex - 1;
		if (newIndex < this.getRowCount() && newIndex >= 0) {
			this.addRowSelectionInterval(newIndex, newIndex);
			scrollToMakeRowVisible(newIndex);
		}
	}

	public void setPageDown() {
		hideTooltip();
		int rowIndex = this.getSelectedRow();
		int newIndex = rowIndex + PAGE_SIZE;
		if (newIndex >= 0 && newIndex < this.getRowCount()) {
			this.addRowSelectionInterval(newIndex, newIndex);
			scrollToMakeRowVisible(newIndex);
		}
	}

	public void setPageUp() {
		hideTooltip();
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
