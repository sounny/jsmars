/**
 * An class for ruler menu items in the right-click popup menu.  
 *
 *  @author: James Winburn MSFF-ASU 
 */
package edu.asu.jmars.ruler;

// generic java imports 
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.lang.*;
import java.util.*;
import javax.swing.*;
import java.awt.font.TextAttribute;
import java.text.*;

// JMARS specific imports.
import edu.asu.jmars.*;
import edu.asu.jmars.layer.*;
import edu.asu.jmars.util.*;
import edu.asu.jmars.swing.*;
	
public class RulerCheckBoxMenuItem extends JCheckBoxMenuItem implements RulerComponent
{
	public RulerCheckBoxMenuItem( BaseRuler ruler ){
		super( ruler.getDescription() );
		final BaseRuler r = ruler;
		AbstractAction action =  new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (r.isHidden()){
					RulerManager.Instance.showComponent(r);
				} else {
					RulerManager.Instance.hideComponent(r);
				}
				RulerManager.Instance.packFrame();
			}
		};
		addActionListener( action);
	}
}



