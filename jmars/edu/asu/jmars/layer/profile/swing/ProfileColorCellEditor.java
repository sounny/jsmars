package edu.asu.jmars.layer.profile.swing;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.TableCellEditor;
import edu.asu.jmars.swing.AbstractCellEditor;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.stable.CustomColorPalette;

/**
 * TableCellEditor for Color objects.
 */
public class ProfileColorCellEditor extends AbstractCellEditor implements TableCellEditor {
	private Color color = Color.white;	
	private JColorChooser colorChooser = new JColorChooser();
	private JSlider alphaSlider = new JSlider(0,255,255);	
	private JDialog colorChooserDialog;
	private ProfileColorCellRenderer renderer;
	private boolean shown;
	private boolean acceptedInput = false;	
	private static DebugLog log = DebugLog.instance();
	
	public ProfileColorCellEditor() {
		this(Color.white);		
	}
	
	public ProfileColorCellEditor(Color defaultColor) {
		super();
		setColor(defaultColor);
		
		ActionListener okHandler = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// We reach here when user presses OK in the color chooser.
				// If the user did select a color, make it the current color.
				if (colorChooser.getColor() != null) {
					color = colorChooser.getColor();
					acceptedInput = true;
				} else {
					acceptedInput = false;
				}
				
				// Mark the end of editing operation.
				fireEditingStopped();
			}
		};
		ActionListener cancelHandler = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				acceptedInput = false;
				fireEditingCanceled();
			}
		};
	
		//add customized "Swatch" panel which returns color names via tooltips
		try {
			List<AbstractColorChooserPanel> colorchooserPanels = 
			        new ArrayList<>(Arrays.asList(colorChooser.getChooserPanels()));
			colorchooserPanels.remove(0);
			CustomColorPalette swatch = new CustomColorPalette();
			colorchooserPanels.add(0, swatch);
			colorChooser.setChooserPanels(colorchooserPanels.toArray(new AbstractColorChooserPanel[0]));
		} catch (Exception e1) {
			log.aprintln("ColorCellEditor: Failed to customize JColorChooser Swatch panel. Will use standrad panel.");			
		}
		
		colorChooserDialog = JColorChooser.createDialog(renderer, "Color Chooser",
			true, colorChooser, okHandler, cancelHandler);
		
		colorChooser.getSelectionModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent arg0) {
                Color color = colorChooser.getColor();            
                alphaSlider.setValue(color.getAlpha());	               
            }
        });		
		
		final JLabel alphaValue = new JLabel();	
		alphaSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				alphaValue.setText(alphaSlider.getValue()*100/255 + "% opaque");	
				Color mycolor = colorChooser.getColor();
				color = new Color(mycolor.getRed(),mycolor.getGreen(),mycolor.getBlue(),alphaSlider.getValue());							
				colorChooser.setColor(color);
			}
		});
		alphaValue.setText(alphaSlider.getValue()*100/255 + "% opaque");	

		Box alphaBox = Box.createHorizontalBox();
		alphaBox.setBorder(new EmptyBorder(8,8,1,8));
		alphaBox.add(alphaSlider);
		alphaBox.add(Box.createHorizontalStrut(4));
		alphaBox.add(alphaValue);		
		JPanel opacityPanel = new JPanel(new GridBagLayout());		
		
		opacityPanel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		final JLabel opacitylabel = new JLabel("Opacity:".toUpperCase());
		FontMetrics fm = opacitylabel.getFontMetrics(opacitylabel.getFont());
		int w = fm.stringWidth("OPACITY:");
		int h = fm.getHeight();
		Dimension size = new Dimension(w, h);	
		opacitylabel.setPreferredSize(size);   
		opacitylabel.setMinimumSize(size);
		opacitylabel.setMaximumSize(size);
		opacitylabel.setSize(size);

		size = new Dimension(300, 25);	
		alphaBox.setPreferredSize(size);   
		alphaBox.setMinimumSize(size);
		alphaBox.setMaximumSize(size);
		alphaBox.setSize(size);		
		opacityPanel.add(opacitylabel);     
		opacityPanel.add(alphaBox);	
		colorChooserDialog.getContentPane().add(opacityPanel, BorderLayout.NORTH);
		
		// Intercepts paint() to make sure we show the editor when we paint the
		// color editor's renderer -- we won't do this for tooltips, but we
		// always do it for a color cell we are editing.
		renderer = new ProfileColorCellRenderer(true) {
			public void paint(Graphics g) {
				super.paint(g);			
				showEditor(this, false);
			}
		};		
	}
	
	/** Sets the color field and alpha slider */
	public void setColor(Color color) {
		alphaSlider.setValue(color.getAlpha());			
		color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaSlider.getValue());
		colorChooser.setColor(color);
	}
	
	/** @return true if the dialog was closed by the user pressing okay, false otherwise. */
	public boolean isInputAccepted() {
		return acceptedInput;
	}
	
	public static void main(String[] args) {
		new ProfileColorCellEditor().showEditor(null, true);
	}
	
	/**
	 * Shows the editor if we have not already shown it in this editing session.
	 * The editor is created on the AWT thread, at some later time, so that the
	 * various threads that lead here can finish their work without waiting on
	 * the popup dialog.
	 **/
	public void showEditor(final Component parent, boolean block) {
		if (!shown) {
			shown = true;
			Runnable todo = new Runnable() {
				public void run() {
					colorChooserDialog.setLocationRelativeTo(parent);
					acceptedInput = false;
					colorChooserDialog.setVisible(true);
				}
			};
			if (block) {
				todo.run();
			} else {
				SwingUtilities.invokeLater(todo);
			}
		}
	}
	
	public boolean isCellEditable(EventObject e) {
		if (e instanceof MouseEvent) {
			MouseEvent m = (MouseEvent)e;
			return SwingUtilities.isLeftMouseButton(m) && m.getClickCount() == 1;
		} else {
			return false;
		}
	}
	
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		if (!(value instanceof Color)) {
			value = Color.white;
		}
		setColor((Color)value);
		shown = false;
		return renderer.getTableCellRendererComponent(table, value, isSelected, renderer.isFocusOwner(), row, column);
	}
	
	public Object getCellEditorValue() {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alphaSlider.getValue());
	}
	
	public boolean shouldSelectCell(EventObject evt){
		return true;
	}
	
	private static void removeTransparencySlider(JColorChooser jc) throws Exception {

	    AbstractColorChooserPanel[] colorPanels = jc.getChooserPanels();
	    for (int i = 1; i < colorPanels.length; i++) {
	        AbstractColorChooserPanel cp = colorPanels[i];	     

	        java.lang.reflect.Field f = cp.getClass().getDeclaredField("panel");
	        f.setAccessible(true);

	        Object colorPanel = f.get(cp);
	        java.lang.reflect.Field f2 = colorPanel.getClass().getDeclaredField("spinners");
	        f2.setAccessible(true);
	        Object spinners = f2.get(colorPanel);

	        Object transpSlispinner = Array.get(spinners, 3);
	        if (i == colorPanels.length - 1) {
	            transpSlispinner = Array.get(spinners, 4);
	        }
	        java.lang.reflect.Field f3 = transpSlispinner.getClass().getDeclaredField("slider");
	        f3.setAccessible(true);
	        JSlider slider = (JSlider) f3.get(transpSlispinner);
	        slider.setEnabled(false);
	        slider.setVisible(false);
	        java.lang.reflect.Field f4 = transpSlispinner.getClass().getDeclaredField("spinner");
	        f4.setAccessible(true);
	        JSpinner spinner = (JSpinner) f4.get(transpSlispinner);
	        spinner.setEnabled(false);
	        spinner.setVisible(false);

	        java.lang.reflect.Field f5 = transpSlispinner.getClass().getDeclaredField("label");
	        f5.setAccessible(true);
	        JLabel label = (JLabel) f5.get(transpSlispinner);
	        label.setVisible(false);
	    }
	}
	
}

