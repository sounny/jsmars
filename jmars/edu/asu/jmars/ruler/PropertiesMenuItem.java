/**
 *  A class for a menu item that when selected brings up the properties dialog.
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
//import edu.asu.jmars.layer.groundtrack.*;



public 	class PropertiesMenuItem extends JMenuItem {
	JPanel         propPanel;
	AbstractAction action;
	public PropertiesMenuItem( JPanel p){
		super("Properties");
		propPanel = p;
		action = new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				// This doesn't return until the dialog is dismissed.
				if (RulerManager.propertiesDialog == null){
					System.out.println("no dialog setup.");
				} else {
					JTabbedPane tp = RulerManager.propertiesTabbedPane;
					tp.setSelectedComponent( propPanel);
					RulerManager.propertiesDialog.pack();
					RulerManager.propertiesDialog.setVisible(true);
				}
			}
		};
		addActionListener( action );
	}
}

