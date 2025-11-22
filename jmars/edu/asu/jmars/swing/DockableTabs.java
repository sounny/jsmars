package edu.asu.jmars.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.MouseInputListener;
import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.util.Util;

public class DockableTabs extends JTabbedPane {
	
	private Map<Component, Component> mapContainerToFocusPanel= new HashMap<>();
	
	public DockableTabs(){
		MouseInputListener myMouse = new MyMouse();
		addMouseListener(myMouse);
		addMouseMotionListener(myMouse);
	}
	
	public void activateTab(int idx) {
		setSelectedIndex(idx);
	}

	public void addTab(String name, Icon icon, Component comp) {
		insertTab(name.toUpperCase(), icon, comp, null, getComponentCount());
	}

	public int indexAtLocation(MouseEvent e) {
		JTabbedPane tabbedPane = (JTabbedPane) e.getSource();
		int tabIdx = tabbedPane.indexAtLocation(e.getX(),e.getY());
		if(tabIdx < 0)
			return  -1;

		Component tabComp = tabbedPane.getComponentAt(tabIdx);
		return  indexOfComponent(tabComp);
	}

	private class MyMouse extends MouseInputAdapter
	{
		int index = -1;
		JFrame dragged;
		Point dragOffset;

		public void mousePressed(MouseEvent e)
		{
			index = indexAtLocation(e.getX(), e.getY());
			int frozenTabs = LManager.getLManager().isDocked() ? 0 : 1;
			if(index < frozenTabs)
				index = -1;

			// If we hit a tab AND it wasn't one of the frozen ones
			if(index != -1)
			{
				dragOffset = e.getPoint();
				Rectangle tabBounds = getBoundsAt(index);
				dragOffset.y -= tabBounds.y;
			}

			if(SwingUtilities.isRightMouseButton(e)){
				final int tabIndex = indexAtLocation(e);
				
				AbstractAction delTab = new AbstractAction("Delete Tab"){
					public void actionPerformed(ActionEvent e){
						remove(tabIndex);
					}
				};
				
				JPopupMenu editMenu = new JPopupMenu();
				JMenuItem deleteItem = new JMenuItem(delTab);
				
				editMenu.add(deleteItem);
				
				if(tabIndex>0)
					editMenu.show(e.getComponent(), e.getX(), e.getY());	
			}

		}

		public void mouseDragged(MouseEvent e)
		{
			if(index == -1)
				return;

			if(dragged == null) {
				dragged = new TabFrame(index);
			}
			
			setDraggedLocationFor(dragged, e);
		}

		public void mouseReleased(MouseEvent e)
		{
			if(dragged == null)
				return;
			
			if(getComponentCount()>0)
				setSelectedIndex(0);
		
			if (index!=0) {
				Component containerforthisfp = getComponentAt(index);
				FocusPanel focuspanel = (FocusPanel)mapContainerToFocusPanel.get(containerforthisfp);
				JFrame released = focuspanel.getFrame();
				Point p = dragged.getLocation();
				dragged.setVisible(false);
				focuspanel.showInFrame();
				released.setVisible(false);
				released.setLocation(p);
				released.setVisible(true);				
			} else{
				Point p = dragged.getLocation();
				dragged.setVisible(false);
				LManager.getLManager().showInFrame(p.x, p.y);
			}

			dragged.setVisible(false);
			dragged = null;
			index = -1;
		}
	
		void setDraggedLocationFor(JFrame frame, MouseEvent e){
			Insets insets = frame.getInsets();
			Point draggedTo = e.getPoint();
			SwingUtilities.convertPointToScreen(draggedTo, e.getComponent());
			draggedTo.x -= dragOffset.x + insets.left;
			draggedTo.y -= dragOffset.y + insets.top;
				
			frame.setLocation(draggedTo);
			frame.setVisible(true);
		}
	}

	public class TabFrame extends JFrame {
		public TabFrame(int tabIndex) {
			super(tabIndex==0 ? "Layer Manager " : getTitleAt(tabIndex));

			setLayout(new BorderLayout());

			setIconImage(Util.getJMarsIcon());

			setUndecorated(true);
			setFocusableWindowState(false);

			Component c;
			
			if (tabIndex==0) {
				c = LManager.getLManager();
			} else {
				c = DockableTabs.this.getComponentAt(tabIndex);
			}
			
			Dimension originalSize = c.getSize();
		
			JLabel comp = new JLabel(new ImageIcon(Util.createImage(c, 0.3f)));
					
			add(comp, BorderLayout.CENTER);

			setSize(originalSize.width  +  getWidth() - comp. getWidth(),
					originalSize.height + getHeight() - comp.getHeight());
			
			setVisible(true);
		}
	}

	public void addDataEntry(Component container, FocusPanel focusPanel) {
		mapContainerToFocusPanel.put(container, focusPanel);		
	}
}
