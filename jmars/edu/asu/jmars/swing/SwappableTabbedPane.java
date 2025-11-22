package edu.asu.jmars.swing;

import java.awt.*;
import javax.swing.*;

/**
 ** A simple extension of {@link JTabbedPane} that provides a function
 ** for swapping two tabs by index.
 **/
public class SwappableTabbedPane extends JTabbedPane
 {
    /**
     ** Swaps the components, icons, and labels at indexA and
     ** indexB. If indexA == indexB, has no effect.
     **
     ** @throws IndexOutOfBoundsException if either indexA or indexB
     ** is negative or greater than the tab count.
     **/
    public void swapTabsAt(int indexA, int indexB)
     {
	if(indexA == indexB)
	    return;

	if(indexA > indexB)
	 {
	    int tmp = indexA;
	    indexA = indexB;
	    indexB = tmp;
	 }

	String   titleA =       getTitleAt(indexA);
	Icon      iconA =        getIconAt(indexA);
	Component compA =   getComponentAt(indexA);
	String     tipA = getToolTipTextAt(indexA);

	String   titleB =       getTitleAt(indexB);
	Icon      iconB =        getIconAt(indexB);
	Component compB =   getComponentAt(indexB);
	String     tipB = getToolTipTextAt(indexB);

	removeTabAt(indexB);
	removeTabAt(indexA);

	insertTab(titleB, iconB, compB, tipB, indexA);
	insertTab(titleA, iconA, compA, tipA, indexB);
     }

    /**
     ** Moves the component, icon, and label at src to dst. If src ==
     ** dst, has no effect. If src+1 == dst, has no effect.
     **
     ** @throws IndexOutOfBoundsException if either src or dst is
     ** negative or greater than the tab count.
     **/
    public void moveTab(int src, int dst)
     {
	if(src == dst)
	    return;

	String   title =       getTitleAt(src);
	Icon      icon =        getIconAt(src);
	Component comp =   getComponentAt(src);
	String     tip = getToolTipTextAt(src);

	removeTabAt(src);
	insertTab(title, icon, comp, tip, dst);
     }
 }
