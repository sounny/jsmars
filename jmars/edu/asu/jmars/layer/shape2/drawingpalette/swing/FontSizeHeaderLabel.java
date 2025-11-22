package edu.asu.jmars.layer.shape2.drawingpalette.swing;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import edu.asu.jmars.ui.looknfeel.ThemeFont;

public class FontSizeHeaderLabel extends JLabel {
	
	public FontSizeHeaderLabel(String text, int style, int fontsize) {
		super(text);
		setFont(new Font(ThemeFont.getFontFamily(), style, fontsize));
		//setFont(new Font("Arial", style, fontsize));  //"Arial" for standalone testing
		//setForeground(Color.WHITE);  //for standalone testing
	}
}
