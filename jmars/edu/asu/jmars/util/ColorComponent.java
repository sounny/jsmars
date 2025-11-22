package edu.asu.jmars.util;

import java.awt.Color;

import javax.swing.JButton;




// An class that displays a button and allows the user to change a color of some
// component or other.  It is used in this application to change the color of the directional
// light and the color of the background.
public class ColorComponent extends JButton {
	private Color color;

	public ColorComponent(String l, Color c) {
		super(l);
		setColor(c);
		setFocusPainted(false);
		requestFocus();
	}

	// sets the background as the color of the button. If the color is lighter
	// than gray, then black is used for the color of the button's text instead
	// of white.
	public void setColor(Color c) {
		color = c;
		setBackground(c);
		if ((c.getRed() + c.getGreen() + c.getBlue()) > (128 + 128 + 128)) {
			setForeground(Color.black);
		} else {
			setForeground(Color.white);
		}
	}

	// returns the color that was previously defined.
	public Color getColor() {
		return color;
	}
}
