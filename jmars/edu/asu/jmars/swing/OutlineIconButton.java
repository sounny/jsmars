package edu.asu.jmars.swing;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;

import mdlaf.components.button.MaterialButtonUI;

/**
 * @author shay
 * 
 * This button looks like the normal 'outline buttons' in JMARS when an
 * icon is added to it.  The border still shows even when the icon is added,
 * and no focus 'dashed border' displays when clicking on the button (just
 * how other normal buttons react).
 *
 */
public class OutlineIconButton extends JButton{

	public OutlineIconButton() {
		super();
	}

	public OutlineIconButton(Action action) {
		super(action);
	}

	public OutlineIconButton(Icon icon) {
		super(icon);
	}

	public OutlineIconButton(String text, Icon icon) {
		super(text, icon);
	}

	public OutlineIconButton(String text) {
		super(text);
	}
	
    @Override
    protected void init(String text, Icon icon) {
        super.init(text, icon);       
        setUI(new OutlineIconButtonUI());
    }
	
	
    private static class OutlineIconButtonUI extends MaterialButtonUI{

    	@Override
        public void installUI(JComponent c) {
    		super.installUI(c);
    		//make sure the regular 'outline border' stays when an icon is added
    		buttonBorderToAll = true;
    		//disable the dashed 'inner border' when clicking on the button
    		button.setFocusable(false);
        }
    }

}
