package edu.asu.jmars.swing;

import java.awt.*;
import javax.swing.*;

/**
 ** LayoutManager implementation that will set all components of a
 ** container to be overlapping, located at the origin, and the same
 ** size as their container (excluding the container's insets).
 **/

public class OverlapLayout implements LayoutManager
 {
	public void addLayoutComponent(String name, Component comp)
	 {
	 }

	public void removeLayoutComponent(Component comp)
	 {
	 }

	public Dimension minimumLayoutSize(Container parent)
	 {
		return  new Dimension(200,200);
	 }

	public Dimension preferredLayoutSize(Container parent)
	 {
		return  new Dimension(200,200);
	 }

	public void layoutContainer(Container parent)
	 {
		Insets in = parent.getInsets();
		int w = parent.getWidth() - in.left - in.right;
		int h = parent.getHeight() - in.bottom - in.top;
		int n = parent.getComponentCount();
		for(int i=0; i<n; i++)
		 {
			Component c = parent.getComponent(i);
			if(c instanceof JToolTip)
				continue;

			if(c.getX() != 0  ||  c.getY() != 0)
				c.setBounds(0, 0, w, h);
			else
				c.setSize(w, h);
		 }
	 }
 }
