package edu.asu.jmars.swing;

import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.ComponentUI;
import mdlaf.animation.MaterialUIMovement;
import mdlaf.components.button.*;

public class LikeDefaultButtonUI extends MaterialButtonUI {
	
	public static ComponentUI createUI(JComponent c) {
		return new LikeDefaultButtonUI();
	}

	@Override
	public void installUI(JComponent c) {
		mouseHoverEnabled = true;
		super.installUI(c);		
		super.borderEnabled = true;
		super.arch = 0;
	    c.setFocusable(false);
		 if (mouseHoverEnabled != null) {
                JButton b = (JButton) button;
                if (!b.isDefaultButton()) {
                	b.setBackground(defaultBackground);
                    b.setForeground(defaultForeground);
                    button.addMouseListener(MaterialUIMovement.getMovement(button, colorMouseHoverDefaultButton));
                    		
                }
            }			
	}
	
	@Override
    protected void paintFocus(Graphics g, AbstractButton b, Rectangle viewRect, Rectangle textRect, Rectangle iconRect) {
       paintBorderButton(g, b);
    }
}       




