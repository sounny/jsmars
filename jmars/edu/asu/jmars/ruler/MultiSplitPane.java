/*
 * Copyright (c) 2001 Sun Microsystems, Inc.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *       Sun Microsystems, Inc. for Project JXTA."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Sun", "Sun Microsystems, Inc.", "JXTA" and "Project JXTA" must
 *    not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact Project JXTA at http://www.jxta.org.
 *
 * 5. Products derived from this software may not be called "JXTA",
 *    nor may "JXTA" appear in their name, without prior written
 *    permission of Sun.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL SUN MICROSYSTEMS OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of Project JXTA.  For more
 * information on Project JXTA, please see
 * <http://www.jxta.org/>.
 *
 * This license is based on the BSD license adopted by the Apache Foundation.
 *
 * 
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
import java.util.*;

import edu.asu.jmars.util.*;

public class MultiSplitPane extends JPanel 
{
	private static final DebugLog log = DebugLog.instance();

	// the height that a ruler should be when it is hidden.
	public static final int MINIMIZED_HEIGHT = 0; 

	// the list of all the components of the multiSplitPane.  
	// This list consists of panels and dragbars.
	private   ArrayList      components   = new ArrayList();

	// the main from in which the MultiSplitPane resides. 
	protected JFrame         frame        = null;
	
	
	// constructor.  The layout and the component listener are defined.
	public MultiSplitPane() 
	{
		super();
		setLayout( new VerticalLayout());
		addComponentListener( new MultiSplitPaneComponentAdapter());
	}



	// This is triggered when the entire frame is resized.  
	class MultiSplitPaneComponentAdapter extends ComponentAdapter {
		public void componentResized(ComponentEvent e){

			// If there are no components, which there wouldn't be if the ruler
			// manager was closed, don't do anything here.
			if (components.size()==0){
				return;
			}
			
			// Get the parameters of the top component.
			JComponent top = (JComponent)components.get(0);
			Rectangle topBounds    = top.getBounds();
			int topComponentHeight = topBounds.height;
			
			// If we are still in the constructor, don't do anything here.
			if (topComponentHeight== 0){
				return;
			}
			
			// Get the height of all the components (except the top component).
			int rulerHeight = 0;
			for (int i = 1; i < components.size(); i++) {
				JComponent comp = (JComponent) components.get(i);
				Rectangle compBounds = comp.getBounds();
				rulerHeight += compBounds.height;
			}
			
			// Get the amount that the ruler components should be moved up or down.
			int panelHeight = getBounds().height;
			int deltaHeight = panelHeight - (rulerHeight + topComponentHeight);
			
			// Set the parameters of the top component.
			topBounds.height = panelHeight - rulerHeight;
			top.setBounds( topBounds);
			
			Dimension topPrefSize  = top.getPreferredSize();
			topPrefSize.width = topBounds.width;
			topPrefSize.height= topBounds.height;
			top.setPreferredSize( topPrefSize);
			
			// Set the bounds of the non-top components.
			for (int i = 1; i < components.size(); i++) {
				JComponent comp = (JComponent) components.get(i);
				Rectangle compBounds = comp.getBounds();
				compBounds.y += deltaHeight;
				comp.setBounds( compBounds);
			}
			
			// set the pref size of the entire panel.
			setPreferredSize( new Dimension( getBounds().width, getBounds().height));
			packFrame();
		}
	} 
	

	// Adds the top component to the MultiSplitPane.  The top component is 
	// different from all the other components in that its dragbar contains 
	// a button for hiding/restoring all the non-top components.
	public Component setContent( JComponent component, JFrame frame) 
	{
		this.frame = frame;
		return addComponent( component);
	}


	// packs the frame such that the components are fit in the smallest size
	// that their preferredSize will allow.  Before this happens, we need
	// to set the preferred width of the components to the current width
	// so that the frame will not be jump to the original width when the packing 
	// occurs.
	public void packFrame()
	{
		// If frame has not been initialized (as it would not be if JMARS was started 
		// from a file), don't do any of this.
		if (frame==null){
			return;
		}


		RulerManager.Instance.updateHidingPanel();

		// This first pack is to get the componant holders to jive with the components.
		frame.pack();

 		Rectangle frameBounds   = getBounds();
		if (frameBounds.width == 0){
			return;
		}

		Dimension framePrefSize = getPreferredSize();

		int height = 0;
		for (int i = 0; i < components.size(); i++) {
  			JComponent comp = (JComponent) components.get(i);
			Rectangle compBounds = comp.getBounds();
			height += compBounds.height;
		}

		framePrefSize.height = height;
		setPreferredSize( framePrefSize);
		frameBounds.height = height;
		setBounds( frameBounds);

		// This second pack is the one that actually resizes everything.
		frame.pack();
	}


	// adds a component to the MultiSplitPane.
	public Component addComponent(JComponent component) 
	{
		ComponentHolder componentHolder = new ComponentHolder(component);
		DragBar dragBar = new DragBar(componentHolder);
		components.add(componentHolder);
		components.add(dragBar); 
		super.add(componentHolder);
		super.add(dragBar); 
		return componentHolder;
	} 


	// removes a component from the MultiSplitPane.
	public void removeComponent(JComponent component) 
	{
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof ComponentHolder) {
				ComponentHolder componentHolder = (ComponentHolder) comp; 
				if (componentHolder.component == component) {
					if ( !((BaseRuler)component).isHidden()){
						((BaseRuler)component).setRestoreHeight( componentHolder.getSize().height);
					}
					DragBar dragBar = (DragBar)components.get(i+1);
					components.remove(componentHolder);
					components.remove(dragBar);
					super.remove(componentHolder);
					super.remove(dragBar);
				}
			}
		}
	}

	public void resizeComponent( JComponent component, int h)
	{
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof ComponentHolder) {
				ComponentHolder componentHolder = (ComponentHolder) comp; 
				if (componentHolder.component.equals(component)) {
					Rectangle bound = componentHolder.getBounds();
					bound.height = h;
					componentHolder.setBounds( bound);
					Dimension d = new Dimension( bound.width, bound.height);
					componentHolder.setPreferredSize( d);
					componentHolder.component.setBounds( bound);
					componentHolder.component.setPreferredSize( d);
				}
			}
		}
	}


	// Unhides the specified component.  If the component was hidden, it is
	// restored to the height that it was when it was hidden and its 
	// corresponding dragbar can be moved again.
	public void showComponent(JComponent component) 
	{
		BaseRuler ruler = (BaseRuler)component;
		ruler.setHidden(false);
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof ComponentHolder) {
				ComponentHolder componentHolder = (ComponentHolder) comp; 
				if (componentHolder.component == component) {
					componentHolder.height  = ruler.getRestoreHeight();
					Dimension prefSize = componentHolder.component.getPreferredSize();
					prefSize.height = ruler.getRestoreHeight();
					componentHolder.component.setPreferredSize( prefSize);
					DragBar dragBar = (DragBar)components.get(i+1);
					dragBar.setMoveable(true);
				}
			}
		}
		packFrame();
	} 


	// Hides the specified component.  The component's corresponding dragbar is still 
	// visible, but when the component is hidden, it cannot be moved.
	public void hideComponent(JComponent component) 
	{
		BaseRuler ruler = (BaseRuler)component;
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof ComponentHolder) { 
				ComponentHolder componentHolder = (ComponentHolder) comp; 
				if (componentHolder.component == component) {
					if ( !ruler.isHidden()){
						ruler.setHidden(true);
						ruler.setRestoreHeight( componentHolder.getSize().height);
					}
					ruler.setHidden( true);
					componentHolder.restoreHeight = componentHolder.getSize().height;
					componentHolder.height = MINIMIZED_HEIGHT;
					Dimension prefSize = componentHolder.component.getPreferredSize();
					prefSize.height = 0;
					componentHolder.component.setPreferredSize( prefSize);
					DragBar dragBar = (DragBar)components.get(i+1);
					dragBar.setMoveable(false);
					Rectangle bounds = componentHolder.component.getBounds();
					bounds.height = MINIMIZED_HEIGHT;
					componentHolder.component.setBounds( bounds);
				}
			}
		}
		packFrame();
	} 
	

	// Unhides all rulers.  This undoes the action of hideAllComponents.
	public void showAllComponents()
	{
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof ComponentHolder) {
				ComponentHolder componentHolder = (ComponentHolder) comp;
				if (componentHolder.component instanceof BaseRuler){
					if (componentHolder.component.isVisible() == false){
						JComponent ruler = (JComponent)componentHolder.component;
						Dimension prefSize = ruler.getPreferredSize();
						prefSize.height = ((BaseRuler)ruler).getRestoreHeight();
						ruler.setPreferredSize( prefSize);
						((BaseRuler)ruler).setHidden(false);
						componentHolder.height  = ((BaseRuler)ruler).getRestoreHeight();
						DragBar dragBar = (DragBar)components.get(i+1);
						dragBar.setMoveable(true);
					}
				}
			}
		}
		packFrame();
	}


	// Hides all rulers.  
	public void hideAllComponents()
	{
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof ComponentHolder) { 
				ComponentHolder componentHolder = (ComponentHolder) comp;
				if (componentHolder.component instanceof BaseRuler){
					BaseRuler ruler = (BaseRuler)componentHolder.component;
					if ( !ruler.isHidden()){
						ruler.setHidden(true);
						ruler.setRestoreHeight(componentHolder.getSize().height);
					}
					componentHolder.height = MINIMIZED_HEIGHT; 
					DragBar dragBar = (DragBar)components.get(i+1);
					dragBar.setMoveable(false);
				}
			}
		}
		packFrame();
	}


	// Sets all the dragbars to a height of zero (if "hidden" is true) or returns
	// the dragbars to the default height (if "hidden" is false).  This is used in JMARS
	// when the RulerManager is first brought us (i.e. no rulers are defined) to hide
	// the fact that the Main Viewing Windows is actually a ruler.  When a ruler IS defined,
	// the dragbars are unhidden.
	public void hideAllDragbars( boolean hidden){
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof DragBar) {
				DragBar dragbar = (DragBar)comp;
				dragbar.setHidden( hidden);
			}
		}
	}
	
		

	// returns whether the specified component is hidden. 
	public boolean isHidden( JComponent component)
	{
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof ComponentHolder) { 
				ComponentHolder componentHolder = (ComponentHolder) comp; 
				if (componentHolder.component == component) {
					if (componentHolder.component instanceof BaseRuler){
						BaseRuler ruler = (BaseRuler)componentHolder.component;
						return ruler.isHidden();
					}
				}
			}
		}
		return false;
	}


	// returns whether all components are hidden.
	public boolean areAllComponentsHidden()
	{
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof ComponentHolder) { 
				ComponentHolder componentHolder = (ComponentHolder) comp;
				if (componentHolder.component instanceof BaseRuler){
					BaseRuler ruler = (BaseRuler)componentHolder.component;
					if ( !ruler.isHidden()){
						return false;
					}
				}
			}
		}
		return true;
	}


	// returns whether all components are unhidden.
	public boolean areAllComponentsUnHidden()
	{
		for (int i = 0; i < components.size(); i++) {
			JComponent comp = (JComponent) components.get(i); 
			if (comp instanceof ComponentHolder) { 
				ComponentHolder componentHolder = (ComponentHolder) comp;
				if (componentHolder.component instanceof BaseRuler){
					BaseRuler ruler = (BaseRuler)componentHolder.component;
					if ( ruler.isHidden()){
						return false;
					}
				}
			}
		}
		return true;
	}
		

	


}  // end: class MultiSplitPane
