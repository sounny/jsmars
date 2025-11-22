/* 
 *  This software was adapted from the MultiSplitPane code from the Sun JXTA project.
 *  It was modified for use in JMARS.
 * 
 *  The implementation differs from the original in that there is a single title 
 *  bar between the top splitpane.  This titlebar allows the hiding/showing of ALL the 
 *  rulers that are added to the pane.
 * 
 *  @author James Winburn MSFF-ASU  10/03
 */ 
package edu.asu.jmars.ruler;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.metal.*;
import java.util.*;


public class ComponentHolder extends JPanel {
	JComponent component;
	int height;
	int restoreHeight; 
	
	ComponentHolder(JComponent component) {
		super(new GridLayout(1,1));
		this.component = component;
		height = component.getPreferredSize().height;
		ComponentHolder.this.add(component);
	} 
	
	void setRestoreHeight(int restoreHeight) {
		this.restoreHeight = restoreHeight;
	} 
	
	int getRealMinimumHeight() {
		return component.getMinimumSize().height;
	}
	
	public Dimension getPreferredSize() {
		return new Dimension(component.getPreferredSize().width, height);
	}
	
	public void setPreferredSize( Dimension d) {
		height = d.height;
		component.setPreferredSize( d);
	}

	public Dimension getMinimumSize() {
		return new Dimension(component.getMinimumSize().width, height);
	}
	
	public Dimension getMaximumSize() {
		return new Dimension(component.getMaximumSize().width, height);
	}
} 

