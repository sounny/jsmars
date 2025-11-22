/*
 *  This software was adapted from the MultiSplitPane code from the Sun JXTA project.
 *  It was modified for use in JMARS.  See MultiSplitPane.java for Sun and JXTA disclaimers.
 * 
 *  The implementation differs from the original in that there is a single title 
 *  bar between the top splitpane and all other splitpanes.  This titlebar allows the 
 *  hiding/showing of ALL the non-top rulers that are added to the pane.
 * 
 *  @author James Winburn MSFF-ASU  10/03
 */ 
package edu.asu.jmars.ruler;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import edu.asu.jmars.util.*;

public  class DragBar extends JPanel 
{
	private static final DebugLog log = DebugLog.instance();

	protected final int             height            = 7;
	protected final int             width             = 0;
	protected final Dimension       hiddenDimension   = new Dimension( 0,0);
	protected final Dimension       unhiddenDimension = new Dimension( width, height);
	protected final ComponentHolder correspondingComponentHolder;

	private boolean             dragging    = false;
	private boolean             isResizable = true;
	private int                 offset;
	private int                 componentY;
 	private MouseListener       mouseListener;
	private MouseMotionListener mouseMotionListener; 

	
	public DragBar(ComponentHolder componentHolder) 
	{
		setBorder( new SoftBevelBorder( BevelBorder.RAISED));
		
		this.correspondingComponentHolder = componentHolder;
		setCursor(Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
		
		mouseListener = new MouseAdapter() 
		{
			public void mouseReleased(MouseEvent me) {
				dragging = false;
			}
			
			public void mousePressed(MouseEvent me) {
				dragging = true;
				offset = me.getY();
				componentY = correspondingComponentHolder.getLocation().y;
				
				// If this is a right-click
				if(SwingUtilities.isRightMouseButton(me)) {
					
					//prepare menu items
					JPopupMenu popup = new JPopupMenu();
					
					// add a facility for hiding/unhiding all rulers at once.
					if (RulerManager.Instance.areAllComponentsHidden()){
						JMenuItem allComponents = new JMenuItem("Unhide All Rulers");
						allComponents.addActionListener( new ActionListener(){
							public void actionPerformed(ActionEvent e){
								RulerManager.Instance.showAllComponents();
								RulerManager.Instance.packFrame();
							}});
						popup.add( allComponents);
					}
					if (RulerManager.Instance.areAllComponentsUnHidden()){
						JMenuItem allComponents = new JMenuItem("Hide All Rulers");
						allComponents.addActionListener( new ActionListener(){
							public void actionPerformed(ActionEvent e){
								RulerManager.Instance.hideAllComponents();
								RulerManager.Instance.packFrame();
							}});
						popup.add( allComponents);
					}
					
					// bring up the right-click menu
					popup.show((Component)me.getSource(), me.getX(), me.getY());
				}  
			}
		};
		addMouseListener(mouseListener); 
		
		mouseMotionListener = new MouseMotionAdapter() {
			public void mouseDragged(MouseEvent evt) {
				int y = evt.getY() + getLocation().y;
				int newHeight = y - componentY - offset; 
				int minimumHeight = correspondingComponentHolder.getRealMinimumHeight();
				
				if (newHeight < minimumHeight)
					newHeight = minimumHeight;
				
				correspondingComponentHolder.height = newHeight;
				correspondingComponentHolder.invalidate();

				RulerManager.Instance.packFrame();
			}
		};
		addMouseMotionListener(mouseMotionListener);
	} 


	// If a panel of a MultiSplitPane is hidden, then the dragbar should not
	// be independantly movable.
	public void setMoveable(boolean resizable) 
	{
		if (!resizable) {
//			removeMouseListener(mouseListener);
			removeMouseMotionListener(mouseMotionListener);
		} else {
			if(!isResizable) {
//				addMouseListener(mouseListener);
				addMouseMotionListener(mouseMotionListener);
			}
		}
		isResizable = resizable;
	}


	// Methods and fields for maintaining the "hidden" status of the dragbar.
	private boolean             hidden      = false;
	public void setHidden( boolean h){
		hidden = h;
	}
	public boolean isHidden(){
		return hidden;
	}
	

	public Dimension getPreferredSize() {
		if (isHidden()){
			return hiddenDimension;
		} else {
			return unhiddenDimension;
		}
	}
	
	public Dimension getMinimumSize() {
		if (isHidden()){
			return hiddenDimension;
		} else {
			return unhiddenDimension;
		}
	}


} // end: class DragBar

	
