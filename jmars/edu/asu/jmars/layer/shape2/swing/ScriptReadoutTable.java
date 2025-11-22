package edu.asu.jmars.layer.shape2.swing;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import org.apache.commons.text.WordUtils;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.TableCellBalloonTip;
import net.java.balloontip.styles.ToolTipBalloonStyle;


public class ScriptReadoutTable extends JTable {
	private static final long serialVersionUID = 1L;
	private TableModel tblModel;
	private MouseMotionAdapter mymouseMotionAdapter;
	private TableCellBalloonTip myBalloonTip;
	private static final Color TOOLTIP_BACK = ThemeProvider.getInstance().getBackground().getContrast();
	private final static int PAGE_SIZE = 10;

	
	public ScriptReadoutTable(ScriptReadoutTableModel model) {
		super(model);	
		this.tblModel = model;
		
		mymouseMotionAdapter = new OnMouseMotion();
		
		TableColumnModel columnModel = this.getColumnModel();
	    for (int i = 0; i < columnModel.getColumnCount(); i++) {
	        TableColumn column = columnModel.getColumn(i);
	        column.setResizable(true);
	    }	
	    this.setRowSelectionAllowed(false);
	    this.getTableHeader().setReorderingAllowed(false);
	    createTooltipUI();
	    addMouseMotionListener(mymouseMotionAdapter);
	}	
	
	
	private void createTooltipUI() {
		JLabel dummy = new JLabel();
		ToolTipBalloonStyle style = new ToolTipBalloonStyle(TOOLTIP_BACK,
				ThemeProvider.getInstance().getBackground().getBorder());
		myBalloonTip = new TableCellBalloonTip(this, dummy, 0, 0, style, BalloonTip.Orientation.LEFT_ABOVE,
				BalloonTip.AttachLocation.ALIGNED, 40, 10, false);
	}

	private void showTooltip(String tip, Point whereToShow) {
		myBalloonTip.setAttachedComponent(this);
		myBalloonTip.setTextContents(tip);
		myBalloonTip.setCellPosition(this.rowAtPoint(whereToShow), this.columnAtPoint(whereToShow));
		myBalloonTip.setVisible(true);
	}

	public void hideTooltip() {
		if (myBalloonTip != null && myBalloonTip.isVisible()) {
			myBalloonTip.setVisible(false);
		}
	}	
	
	private class OnMouseMotion extends MouseMotionAdapter {
		@Override
		public void mouseMoved(MouseEvent e) {
			String tip = "";
			ScriptReadoutTable jTable = (ScriptReadoutTable) e.getSource();
			Point whereToShow = e.getPoint();
			int rowIndex = jTable.rowAtPoint(e.getPoint());
			int colIndex = jTable.columnAtPoint(e.getPoint());
			try {
				int modelRow = jTable.convertRowIndexToModel(rowIndex);
				int modelCol = jTable.convertColumnIndexToModel(colIndex);
				tip = (String) tblModel.getValueAt(modelRow, modelCol);
				if (tip.length() <= 40) { hideTooltip(); return; }
				tip = formatTooltip(tip);
			} catch (RuntimeException e1) {
			}
			showTooltip(tip, whereToShow);
		}

		private String formatTooltip(String tip) {
			String infohtml="<html>";
			String wrap;
			infohtml += "<p style=\"border:2px solid #3b3e45; padding:5px; margin:-4;\">&nbsp;";
			//wrap long strings
			wrap = WordUtils.wrap(tip, 60, "<br>", false);
			infohtml += wrap + "&nbsp;&nbsp;</p></html>";

			return infohtml;
		}
	}	
	
	@Override
	protected JTableHeader createDefaultTableHeader() {
		return new JTableHeader(columnModel) {
			public String getToolTipText(MouseEvent e) {
				java.awt.Point p = e.getPoint();
				int index = columnModel.getColumnIndexAtX(p.x);
				int realIndex = columnModel.getColumn(index).getModelIndex();
				return ((ScriptReadoutTableModel) tblModel).getColumnToolTips()[realIndex];
			}
		};
	}

}
