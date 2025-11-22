package edu.asu.jmars.swing;

import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeButton;
import mdlaf.animation.MaterialUIMovement;
import mdlaf.components.button.*;

public class IconButtonUI extends MaterialButtonUI {
	
	public static ComponentUI createUI(JComponent c) {
		return new IconButtonUI();
	}

	@Override
	public void installUI(JComponent c) {
		mouseHoverEnabled = false;
		super.installUI(c);
		super.background = ((ThemeButton) GUITheme.get("button")).getThemebackground();
		super.disabledBackground = ((ThemeButton) GUITheme.get("button")).getThemebackground();		
		super.defaultBackground = ((ThemeButton) GUITheme.get("button")).getThemebackground();
		super.borderEnabled = false;
		super.arch = 0;
	    c.setFocusable(true);
		 if (mouseHoverEnabled != null) {
                JButton b = (JButton) button;
                if (!b.isDefaultButton()) {
                    button.addMouseListener(MaterialUIMovement.getMovement(button, 
                    		((ThemeButton) GUITheme.get("button")).getAltOnhover()));
                }
            }			
	}
	
	@Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
       paintBorderButton(g, b);
    }
}       
