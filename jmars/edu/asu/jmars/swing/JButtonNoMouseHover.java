package edu.asu.jmars.swing;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import mdlaf.components.button.MaterialButtonUI;

public class JButtonNoMouseHover extends JButton {

    public JButtonNoMouseHover() {
    }

    public JButtonNoMouseHover(Icon icon) {
        super(icon);
    }

    public JButtonNoMouseHover(String text) {
        super(text);
    }

    public JButtonNoMouseHover(Action a) {
        super(a);
    }

    public JButtonNoMouseHover(String text, Icon icon) {
        super(text, icon);
    }

    @Override
    protected void init(String text, Icon icon) {
        super.init(text, icon);       
        setUI(new JButtonNoMouseHoverUI());
    }

    private static class JButtonNoMouseHoverUI extends MaterialButtonUI{

        @Override
        public void installUI(JComponent c) {
            mouseHoverEnabled = false;
            super.installUI(c);
            super.borderEnabled = false;
            c.setFocusable(false);
        }
    }
}


