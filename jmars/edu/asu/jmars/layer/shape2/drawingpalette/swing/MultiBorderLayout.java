package edu.asu.jmars.layer.shape2.drawingpalette.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.util.Vector;

/**
 * Extends BorderLayout with multiple components in the northList, southList, eastList, westList
 * and centerList. Layout is used for correct working multiple toolbars.
 */
public class MultiBorderLayout extends BorderLayout { 
    /**
     * list of the northList region components
     */
    Vector northList = new Vector();
 
    /**
     * list of the southList region components
     */
    Vector southList = new Vector();
 
    /**
     * list of the westList region components
     */
    Vector westList = new Vector();
 
    /**
     * list of the eastList region components
     */
    Vector eastList = new Vector();
 
    /**
     * list of the centerList region components
     */
    Vector centerList = new Vector();
 
    /**
     * Constructs default layout instance.
     */
    public MultiBorderLayout() {
        super();
    }
 
    /**
     * Constructs new layout instance with defined parameters.
     *
     * @param hgap  the horizontal gap.
     * @param vgap  the vertical gap.
     */
    public MultiBorderLayout(int hgap, int vgap) {
        super(hgap, vgap);
    }
 
    /**
     * Adds the specified component to the layout, using the specified
     * constraint object. For border layouts, the constraint must be one of the
     * following constants: <code>NORTH</code>, <code>SOUTH</code>, <code>EAST</code>
     * , <code>WEST</code>, or <code>CENTER</code>. <p>
     *
     * Most applications do not call this method directly. This method is called
     * when a component is added to a container using the <code>Container.add</code>
     * method with the same argument types.
     *
     * @param name         The feature to be added to the LayoutComponent
     *      attribute.
     * @param comp         the component to be added.
     */
 
    //the method is deprecated but it's necessary to override it because current class extends
    //BorderLayout to provide multiple components (toolbars)
    public void addLayoutComponent(String name, Component comp) {

        synchronized (comp.getTreeLock()) {
            /*
             *  Special case:  treat null the same as "Center".
             */
            if (name == null) {
                name = "Center";
            }
 
            /*
             *  Assign the component to one of the known regions of the layout.
             */
            if ("Center".equals(name)) {
                centerList.add(comp);
            } else if ("North".equals(name)) {
                northList.insertElementAt(comp, 0);
            } else if ("South".equals(name)) {
                southList.add(comp);
            } else if ("East".equals(name)) {
                eastList.add(comp);
            } else if ("West".equals(name)) {
                westList.add(comp);
            } else {
                throw new IllegalArgumentException("cannot add to layout: unknown constraint: " + name);
            }

        }
    }
 
    /**
     * Removes the specified component from this border layout. This method is
     * called when a container calls its <code>remove</code> or <code>removeAll</code>
     * methods. Most applications do not call this method directly.
     *
     * @param comp  the component to be removed.
     */
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {

            southList.remove(comp);
            northList.remove(comp);
            centerList.remove(comp);
            westList.remove(comp);
            eastList.remove(comp);
        }
    }
 
    /**
     * Determines the minimum size of the <code>target</code> container using
     * this layout manager. <p>
     *
     * This method is called when a container calls its <code>getMinimumSize</code>
     * method. Most applications do not call this method directly.
     *
     * @param target  the container in which to do the layout.
     * @return        the minimum dimensions needed to lay out the subcomponents
     *      of the specified container.
     */
    public Dimension minimumLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
 
            Component c;
 
            if (eastList.size() > 0) {
                for (int i = 0; i < eastList.size(); i++) {
                    c = (Component) eastList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getMinimumSize();
                    dim.width += d.width + this.getHgap();
                    dim.height = Math.max(d.height, dim.height);
                }
            }
            if (westList.size() > 0) {
                for (int i = 0; i < westList.size(); i++) {
                    c = (Component) westList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getMinimumSize();
                    dim.width += d.width + this.getHgap();
                    dim.height = Math.max(d.height, dim.height);
                }
            }
            if (centerList.size() > 0) {
                for (int i = 0; i < centerList.size(); i++) {
                    c = (Component) centerList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getMinimumSize();
                    dim.width += d.width;
                    dim.height = Math.max(d.height, dim.height);
                }
            }
            if (northList.size() > 0) {
                for (int i = 0; i < northList.size(); i++) {
                    c = (Component) northList.get(i);
                    if (!c.isVisible()) {

                        continue;
                    }
                    Dimension d = c.getMinimumSize();
                    dim.width = Math.max(d.width, dim.width);
                    dim.height += d.height + this.getVgap();
                }
            }
            if (southList.size() > 0) {
                for (int i = 0; i < southList.size(); i++) {
                    c = (Component) southList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getMinimumSize();
                    dim.width = Math.max(d.width, dim.width);
                    dim.height += d.height + this.getVgap();
                }
            }
 
            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;
 
            return dim;
        }

    }
 
    /**
     * Determines the preferred size of the <code>target</code> container using
     * this layout manager, based on the components in the container. <p>
     *
     * Most applications do not call this method directly. This method is called
     * when a container calls its <code>getPreferredSize</code> method.
     *
     * @param target  the container in which to do the layout.
     * @return        the preferred dimensions to lay out the subcomponents of
     *      the specified container.
     */
    public Dimension prefferedLayoutSize(Container target) {
        synchronized (target.getTreeLock()) {
            Dimension dim = new Dimension(0, 0);
 
            Component c;
 
            if (eastList.size() > 0) {
                for (int i = 0; i < eastList.size(); i++) {
                    c = (Component) eastList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    dim.width += d.width + this.getHgap();
                    dim.height = Math.max(d.height, dim.height);
                }
            }

            if (westList.size() > 0) {
                for (int i = 0; i < westList.size(); i++) {
                    c = (Component) westList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    dim.width += d.width + this.getHgap();
                    dim.height = Math.max(d.height, dim.height);
                }
            }

            if (centerList.size() > 0) {
                for (int i = 0; i < centerList.size(); i++) {
                    c = (Component) centerList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    dim.width += d.width;
                    dim.height = Math.max(d.height, dim.height);
                }
            }

            if (northList.size() > 0) {
                for (int i = 0; i < northList.size(); i++) {
                    c = (Component) northList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    dim.width = Math.max(d.width, dim.width);
                    dim.height += d.height + this.getVgap();
                }
            }

            if (southList.size() > 0) {
                for (int i = 0; i < southList.size(); i++) {
                    c = (Component) southList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    dim.width = Math.max(d.width, dim.width);
                    dim.height += d.height + this.getVgap();
                }
            }
 
            Insets insets = target.getInsets();
            dim.width += insets.left + insets.right;
            dim.height += insets.top + insets.bottom;
 
            return dim;
        }
    }
 
    /**
     * Lays out the container argument using this border layout. <p>
     *
     * This method actually reshapes the components in the specified container
     * in order to satisfy the constraints of this <code>BorderLayout</code>
     * object. The <code>NORTH</code> and <code>SOUTH</code> components, if any,
     * are placed at the top and bottom of the container, respectively. The
     * <code>WEST</code> and <code>EAST</code> components are then placed on the
     * left and right, respectively. Finally, the <code>CENTER</code> object is
     * placed in any remaining space in the middle. <p>
     *
     * Most applications do not call this method directly. This method is called
     * when a container calls its <code>doLayout</code> method.
     *
     * @param target  the container in which to do the layout.
     */
    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int top = insets.top;
            int bottom = target.getHeight() - insets.bottom;
            int left = insets.left;
            int right = target.getWidth() - insets.right;
 
            Component c;
 
            if (northList.size() > 0) {
                for (int i = 0; i < northList.size(); i++) {
                    c = (Component) northList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    c.setSize(right - left, d.height);
                    c.setBounds(left, top, right - left, c.getHeight());
                    top += d.height;
                }
            }

            if (southList.size() > 0) {
                for (int i = 0; i < southList.size(); i++) {
                    c = (Component) southList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    c.setSize(right - left, d.height);
                    c.setBounds(left, bottom - d.height, right - left, c.getHeight());
                    bottom -= d.height;
                }
            }

            if (eastList.size() > 0) {
                for (int i = 0; i < eastList.size(); i++) {
                    c = (Component) eastList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    c.setSize(d.width, bottom - top);
                    c.setBounds(right - d.width, top, c.getWidth(), bottom - top);
                    right -= d.width;
                }
            }

            if (westList.size() > 0) {
                for (int i = 0; i < westList.size(); i++) {
                    c = (Component) westList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    Dimension d = c.getPreferredSize();
                    c.setSize(d.width, bottom - top);
                    c.setBounds(left, top, c.getWidth(), bottom - top);
                    left += d.width;
                }
            }

            if (centerList.size() > 0) {
                for (int i = 0; i < centerList.size(); i++) {
                    c = (Component) centerList.get(i);
                    if (!c.isVisible()) {
                        continue;
                    }
                    c.setBounds(left, top, right - left, bottom - top);
                }
            }

        }
    }
 
}
