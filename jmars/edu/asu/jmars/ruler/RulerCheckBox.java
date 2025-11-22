/**
 * A class that abstracts the behavior of a JCheckBox.
 * The checkbox is displayed in an initial state.  Clicking on it 
 * toggles the state and redisplays the rulers.
 * The state of the check box  may be accessed by other objects.
 *
 *  @author  James Winburn MSSF-ASU  
 */
package edu.asu.jmars.ruler;

// generic java imports.
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

public class RulerCheckBox extends JCheckBox implements RulerComponent
{
	boolean boolValue;

	public RulerCheckBox( String l, boolean b){
		super(l, b);
		setFocusPainted( false);
		boolValue = super.isSelected();
		addActionListener( new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				boolValue = !boolValue;
				setSelected( boolValue );
				RulerManager.Instance.notifyRulerOfViewChange();
			}
		});
	}


	public void setSelected( boolean  b){
		boolValue = b;
		super.setSelected( boolValue);
	}

	public boolean isSelected(){
		return boolValue;
	}

} // end: RulerCheckBox

