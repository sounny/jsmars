package edu.asu.jmars.swing;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import edu.asu.jmars.ui.looknfeel.ThemeProvider;
import mdlaf.components.button.MaterialButtonUI;

public class LikeLabelButtonUI extends MaterialButtonUI {
	private Color goldBackground = ThemeProvider.getInstance().getBackground().getHighlight();
	
	public static ComponentUI createUI(JComponent c) {
		return new LikeLabelButtonUI();
	}

	@Override
	public void installUI(JComponent c) {
		mouseHoverEnabled = false;
		super.installUI(c);
		super.background = goldBackground; //gold
		super.disabledBackground = goldBackground;		
		super.defaultBackground = goldBackground;
		super.borderEnabled = false;
		super.arch = 0;
	    c.setFocusable(false);
	}
	
	@Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
       paintBorderButton(g, b);
    }
}       

