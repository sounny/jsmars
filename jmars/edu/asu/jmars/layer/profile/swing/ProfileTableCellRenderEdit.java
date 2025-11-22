package edu.asu.jmars.layer.profile.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;

public class ProfileTableCellRenderEdit extends AbstractCellEditor implements TableCellEditor, TableCellRenderer {
	JPanel panel;
	JPanel txtPanel;
	JPanel btnPanel;
	JLabel text;
	JLabel lineButton;
	ProfileTabelCellObject profileCellObj;

	public ProfileTableCellRenderEdit() {
		initUI();			
	}

	private void initUI() {
		text = new JLabel();
		FontMetrics fm = text.getFontMetrics(text.getFont());
		int lblTextWidth = fm.stringWidth("Profilewwwwwwwwwwwwwwwwwwwwwwwwwwww");
		Dimension dim = new Dimension(lblTextWidth, fm.getHeight());
		text.setPreferredSize(dim);
		text.setHorizontalAlignment(SwingConstants.LEFT);
		lineButton = new JLabel();
		panel = new JPanel(new BorderLayout());
		txtPanel = new JPanel(new BorderLayout());
		txtPanel.add(text, BorderLayout.WEST);
		btnPanel = new JPanel(new BorderLayout());
		btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 1, 5, 5));
		btnPanel.add(lineButton, BorderLayout.WEST);
		panel.add(btnPanel, BorderLayout.WEST);
		panel.add(txtPanel, BorderLayout.CENTER);
	}

	private void updateData(ProfileTabelCellObject cellobj, int row , JTable table) {
		this.profileCellObj = cellobj;

		text.setText("" + this.profileCellObj.getName());
		lineButton.setIcon(this.profileCellObj.getLine());

		if (row % 2 == 0) {
			panel.setBackground(table.getBackground());  
			txtPanel.setBackground(table.getBackground());  
			btnPanel.setBackground(table.getBackground()); 
			
		} else {
			panel.setBackground(ThemeProvider.getInstance().getRow().getAlternateback());
			txtPanel.setBackground(ThemeProvider.getInstance().getRow().getAlternateback());  
			btnPanel.setBackground(ThemeProvider.getInstance().getRow().getAlternateback()); 
		}
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {		
		ProfileTabelCellObject cellobj = (ProfileTabelCellObject) value;
		updateData(cellobj, row, table);
		return panel;
	}

	@Override
	public Object getCellEditorValue() {
		return null;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		ProfileTabelCellObject cellobj = (ProfileTabelCellObject) value;						
		updateData(cellobj, row, table);
		return panel;
	}
}
