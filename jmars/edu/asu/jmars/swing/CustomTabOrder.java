package edu.asu.jmars.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.ContainerOrderFocusTraversalPolicy;
import java.awt.FocusTraversalPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * <p>Provides a {@link FocusTraversalPolicy} implementation that lets the
 * programmer specify the exact ordering of the component focus cycle.
 * 
 * <p>The {@link ContainerOrderFocusTraversalPolicy} would do the same job, if it
 * honored {@link Component#isFocusable()}.
 * 
 * <p>The components will be cycled in the order given in the object passed to the
 * constructor.
 * 
 * <p>The default component is always the first component in the list.
 */
public class CustomTabOrder extends FocusTraversalPolicy {
	private final List<Component> order;
	public CustomTabOrder(Component[] ordered) {
		this(Arrays.asList(ordered));
	}
	public CustomTabOrder(List<Component> ordered) {
		super();
		this.order = new ArrayList<Component>(ordered);
		if (ordered.size() == 0) {
			throw new IllegalArgumentException("Must supply at least one component");
		}
	}
	public Component getComponentAfter(Container container, Component component) {
		int pos = order.indexOf(component);
		if (pos >= 0 && pos < order.size() - 1) {
			return order.get(pos+1);
		} else {
			return null;
		}
	}
	public Component getComponentBefore(Container container, Component component) {
		int pos = order.indexOf(component);
		if (pos > 0 && pos < order.size()) {
			return order.get(pos-1);
		} else {
			return null;
		}
	}
	public Component getDefaultComponent(Container container) {
		return getFirstComponent(container);
	}
	public Component getFirstComponent(Container container) {
		return order.get(0);
	}
	public Component getLastComponent(Container container) {
		return order.get(order.size()-1);
	}
}

