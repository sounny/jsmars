/**
 * A class for a button that defines the color of another object.  
 * The button is displayed in an initial color.  Clicking on it 
 * brings up a color chooser dialog. The color of the
 * button changes to the color selected in this dialog.
 * The color of the button may be accessed by other objects.
 *
 *  @author  James Winburn MSSF-ASU  
 */
package edu.asu.jmars.swing;

// generic java imports.
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeSupport;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import edu.asu.jmars.util.stable.ColorCellEditor;

public class ColorButton extends JButton {
	private Color  color;
	private final boolean enableAlpha;
	private PropertyChangeSupport colorPropertyChangeSupport = new PropertyChangeSupport(this);
	private boolean colorSelected = false;
	Color foreground = null;
	
	public ColorButton(String l, Color c) {
		this(l, c, false);
	}
	
	public ColorButton(String l, Color c, boolean enableAlpha) {
		super(l);
		this.enableAlpha = enableAlpha;
		setColor(c);
		setFocusPainted(false);
		addActionListener(buttonPressed);
		setUI(new IconButtonUI());
	}
	
	private final ActionListener buttonPressed = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			if (enableAlpha) {
				ColorCellEditor ce = new ColorCellEditor(color);
				ce.showEditor(ColorButton.this, true);
				if (ce.isInputAccepted()) {
					setColor((Color)ce.getCellEditorValue());
				}
			} else {
				Color newColor = JColorChooser.showDialog (ColorButton.this, getText(), color);
				if (newColor != null) {
					setColor(newColor);
					colorPropertyChangeSupport.firePropertyChange("background", color, newColor);
					colorSelected= true;
				}
			}
		}
	};
	
	// sets the background as the color of the button.  If the color is lighter
	// than gray, then black is used for the color of the button's text instead
	// of white.
	public void setColor(Color c) {
		Color oldColor = color;		

		if (c != null) {
			color = enableAlpha ? c : dupColor(c, 255);
		}else{
			c = Color.DARK_GRAY;
		}
		setBackground(c);
		
		//if the button color is really light, then make the text white
		if ((c.getRed() + c.getGreen() + c.getBlue()) > (128 + 128 + 128) ) {
			setForeground(Color.black);	
			this.foreground = Color.black;
		} 
		//else make the text black
		else {
			setForeground(Color.white);	
			this.foreground = Color.white;
		}
		
		//However, if alpha is really light, then the deciding factor
		// is the parent background color
		if(getParent() != null){
			Color parentCol = getParent().getBackground();
			if(c.getAlpha() < 122){
				if((parentCol.getRed() + parentCol.getGreen() + parentCol.getBlue()) > 384){
					setForeground(Color.black);
					this.foreground = Color.black;
				}
				else{
					setForeground(Color.white);
					this.foreground = Color.white;
				}
			}
		}
	}
	
	/**
	 * Special painter that ensures any garbage behind the component is cleared
	 * before the partially transparent component is painted over it.
	 */
	@Override
	public void paintComponent(Graphics g) {		
		g.setColor(getParent().getBackground());			
		g.fillRoundRect(0, 0, getWidth(), getHeight(), 0, 0);
		if(color != null){
			setBackground(this.color);
		}
		if(this.foreground != null){
			setForeground(this.foreground);
		}
	     super.paintComponent(g);
	}
	
	public Color getColor() {
		return color;
	}
	
	

	public boolean isColorSelected() {
		return colorSelected;
	}

	public void setColorSelected(boolean colorSelected) {
		this.colorSelected = colorSelected;
	}

	private static Color dupColor(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}
}

