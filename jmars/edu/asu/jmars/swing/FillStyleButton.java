/**
 * This class is used to allow the user to pick a Fill Pattern for the default style in
 * the custom shape layer.  This class is based off of the ColorButton that is used in
 * the same interface, as well as other locations in JMARS.
 */
package edu.asu.jmars.swing;

// generic java imports.
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeSupport;
import javax.swing.JButton;

import edu.asu.jmars.util.FillStyle;
import edu.asu.jmars.util.stable.FillStyleCellEditor;

public class FillStyleButton extends JButton {
	private PropertyChangeSupport stylePropertyChangeSupport = new PropertyChangeSupport(this);
	
	FillStyle myFillStyle;
	
	public FillStyleButton(FillStyle style) {
		super("Label String");  // Won't show up, but affects the size of the button
		myFillStyle = style;
		addActionListener(buttonPressed);
		setUI(new IconButtonUI());
	}
		
	private final ActionListener buttonPressed = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			FillStyleCellEditor fsce = new FillStyleCellEditor();
			
			fsce.showEditor(FillStyleButton.this, true);
			if (fsce.isInputAccepted()) {
				FillStyle oldStyle = myFillStyle;
				FillStyle newStyle = (FillStyle)fsce.getCellEditorValue();
				setStyle(newStyle);
				stylePropertyChangeSupport.firePropertyChange("background", oldStyle, newStyle);
			}
		}
	};
		
	public void setStyle(FillStyle newStyle) {
		myFillStyle = newStyle;
	}
	
	public FillStyle getStyle() {
		return myFillStyle;
	}
	
	/**
	 * Special painter that ensures any garbage behind the component is cleared
	 * before the partially transparent component is painted over it.
	 */
	@Override
	public void paintComponent(Graphics g) {		
		Graphics2D g2 = (Graphics2D)g;
		Dimension d = getSize();
		
		g2.setBackground(getBackground());
		g2.clearRect(0, 0, d.width, d.height);
		if (myFillStyle != null){
			g2.setPaint(myFillStyle.getPaint(1));
			g2.fill(new Rectangle2D.Double(0,0, getWidth(), getHeight()));
		}
	}	
}

