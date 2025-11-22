package edu.asu.jmars.layer.shape2.drawingpalette.swing;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import edu.asu.jmars.ui.looknfeel.ThemeFont;

public class TextUnderImageLabel extends JLabel {
	
	public TextUnderImageLabel(String text) {
		super(text);
		setFont(new Font(ThemeFont.getFontFamily(), Font.PLAIN, 10));  //for standalon use "Arial"
		setHorizontalTextPosition(JLabel.CENTER);
		setVerticalTextPosition(JLabel.BOTTOM);		
		//etForeground(Color.WHITE);  //for standalone testing
	}
}


