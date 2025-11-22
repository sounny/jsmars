package edu.asu.jmars.layer;

import java.util.*;
import javax.swing.*;

/**
 ** Encapsulates a parent node in a tree of LView factories.
 **/
public abstract class MultiFactory {
	private String name;

	protected MultiFactory(String name) {
		this.name = name;
	}

	/**
	 ** Must return a List consisting of just MultiFactory and/or
	 ** LViewFactory instances.
	 **/
	protected abstract List getChildFactories();

	private JMenu createMenu() {
		JMenu menu = new JMenu(name);
		addAllToMenu(menu, getChildFactories());
		return menu;
	}

	public static void addAllToMenu(JMenu menu, List factories) {
		for (Iterator i = factories.iterator(); i.hasNext();) {
			Object child = i.next();
			if (child instanceof MultiFactory)
				menu.add(((MultiFactory) child).createMenu());
			else {
				for (JMenuItem childMenu: ((LViewFactory) child).createMenuItems())
					menu.add(childMenu);
			}
		}
	}

	void addDescendantsTo(List list) {
		for (Iterator i = getChildFactories().iterator(); i.hasNext();) {
			Object child = i.next();
			if (child instanceof MultiFactory)
				((MultiFactory) child).addDescendantsTo(list);
			else
				list.add(child);
		}
	}
}
