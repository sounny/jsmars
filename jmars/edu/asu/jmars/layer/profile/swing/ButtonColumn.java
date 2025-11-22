package edu.asu.jmars.layer.profile.swing;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;

/**
 * The ButtonColumn class provides a renderer and an editor that looks like a
 * JButton. The renderer and editor will then be used for a specified column in
 * the table. The TableModel will contain the String to be displayed on the
 * button or Icon.
 */
public class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, MouseListener, ActionListener, MouseMotionListener {
	private JTable table;	
	private JLabel renderButton;
	private JLabel editButton;
	private Object editorValue;
	private boolean isButtonColumnEditor;
	private boolean isZebra = false;
	private JPanel renderpanel, editpanel;
	private int alignment = SwingConstants.RIGHT;
	
	
	
	public ButtonColumn(JTable table, int mnemonic, boolean zebra, int align) {
		this.table = table;			
		this.isZebra = zebra;	
		this.alignment = align;
		table.addMouseListener( this );	
		table.addMouseMotionListener(this);
		initUI();					
	}

	private void initUI() {
		if (this.alignment == SwingConstants.RIGHT) {
			createRenderPanelWithBorderLayout();
			createEditPanelWithBorderLayout();
		} else if (this.alignment == SwingConstants.CENTER) {
			createRenderPanelWithBoxLayout();
			createEditPanelWithBoxLayout();
		}   
	}

	private void createEditPanelWithBoxLayout() {
		editpanel = new JPanel();
		BoxLayout box2 = new BoxLayout(editpanel, BoxLayout.X_AXIS);
		editpanel.setLayout(box2);
		editpanel.add(Box.createHorizontalGlue());
		editButton = new JLabel();
		editButton.setToolTipText(null);
		editpanel.add(editButton);
		editpanel.add(Box.createHorizontalGlue());		
	}

	private void createEditPanelWithBorderLayout() {
		editpanel = new JPanel(new BorderLayout());
		editpanel.add(Box.createHorizontalGlue());
		editButton = new JLabel();
		editButton.setToolTipText(null);
		editpanel.add(editButton, BorderLayout.EAST);
		editpanel.add(Box.createHorizontalGlue());		
	}

	private void createRenderPanelWithBoxLayout() {
		renderpanel = new JPanel();
		BoxLayout box1 = new BoxLayout(renderpanel, BoxLayout.X_AXIS);
		renderpanel.setLayout(box1);
		renderpanel.add(Box.createHorizontalGlue());
		renderButton = new JLabel();
		renderButton.setToolTipText(null);
		renderpanel.add(renderButton);
		renderpanel.add(Box.createHorizontalGlue());		
	}

	private void createRenderPanelWithBorderLayout() {
		renderpanel = new JPanel(new BorderLayout());
		renderpanel.add(Box.createHorizontalGlue());
		renderButton = new JLabel();
		renderpanel.add(renderButton, BorderLayout.EAST);
		renderButton.setToolTipText(null);
		renderpanel.add(Box.createHorizontalGlue());
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		
		if (isZebra) {
			if (row % 2 == 0) {
				editpanel.setBackground(table.getBackground());				
			} else {
				editpanel.setBackground(ThemeProvider.getInstance().getRow().getAlternateback());  
			}			
		}
		
		if (isSelected) {
			editpanel.setForeground(table.getSelectionForeground());
			editpanel.setBackground(table.getSelectionBackground());
		} 
		
		if (value == null) {
			editButton.setText("");
			editButton.setIcon(null);
		} else if (value instanceof Icon) {
			editButton.setText("");
			editButton.setIcon((Icon) value);
		} else {
			editButton.setText(value.toString());
			editButton.setIcon(null);
		}		
		editButton.setToolTipText(null);
		this.editorValue = value;
		return editpanel;
	}

	@Override
	public Object getCellEditorValue() {
		return editorValue;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		if (isZebra) {
			if (row % 2 == 0) {
				renderpanel.setBackground(table.getBackground());  			
			} else {
				renderpanel.setBackground(ThemeProvider.getInstance().getRow().getAlternateback()); 
			}			
		}	
		
		if (isSelected) {
			renderpanel.setForeground(table.getSelectionForeground());
			renderpanel.setBackground(table.getSelectionBackground());
		} 
		
		if (value == null) {
			renderButton.setText("");
			renderButton.setIcon(null);
		} else if (value instanceof Icon) {
			renderButton.setText("");
			renderButton.setIcon((Icon) value);
			
		} else {
			renderButton.setText(value.toString());
			renderButton.setIcon(null);
		}
		renderButton.setToolTipText(null);
		return renderpanel;
	}
	

	/*
	 * When the mouse is pressed the editor is invoked. If you then drag the
	 * mouse to another cell before releasing it, the editor is still active. Make
	 * sure editing is stopped when the mouse is released.
	 */
	@Override
	public void mousePressed(MouseEvent e) {
		if (table.isEditing() && table.getCellEditor() == this)
			isButtonColumnEditor = true;
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if (isButtonColumnEditor && table.isEditing())
			table.getCellEditor().stopCellEditing();

		isButtonColumnEditor = false;
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	
	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void actionPerformed(ActionEvent e) {
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (isButtonColumnEditor && table.isEditing())
			table.getCellEditor().stopCellEditing();

		isButtonColumnEditor = false;
	}

	@Override
	public void mouseMoved(MouseEvent e) {
	}
}
