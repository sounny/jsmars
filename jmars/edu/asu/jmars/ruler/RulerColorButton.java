/**
 * A class for a button that defines the color of another object.  
 * The button is displayed in an initial color.  Clicking on it 
 * brings up a color chooser dialog. The color of the
 * button changes to the color selected in this dialog.
 * The color of the button may be accessed by other objects.
 *
 *  @author  James Winburn MSSF-ASU  
 */
package edu.asu.jmars.ruler;

// generic java imports.
import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import edu.asu.jmars.swing.ColorButton;

public class RulerColorButton extends ColorButton implements RulerComponent {
	public RulerColorButton( String l, Color c){
		super(l,c);
		addPropertyChangeListener("background", new PropertyChangeListener(){
			public void propertyChange(PropertyChangeEvent evt) {
				RulerManager.Instance.notifyRulerOfViewChange();
			}
		});
	}
}

