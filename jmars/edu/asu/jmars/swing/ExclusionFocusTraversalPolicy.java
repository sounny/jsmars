package edu.asu.jmars.swing;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.LayoutFocusTraversalPolicy;

public class ExclusionFocusTraversalPolicy extends LayoutFocusTraversalPolicy {

	private List components = new ArrayList();

	public void addExcludedComponent(Component component) {
		components.add(component);
	}

	@Override
	protected boolean accept(Component aComponent) {
		if (components.contains(aComponent)) {
			return false;
		}
		return super.accept(aComponent);
	}
}
