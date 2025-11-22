package edu.asu.jmars.swing;

import javax.swing.event.*;

/* For some reason, there is an AncestorListener but no
   AncestorAdapter. Here's a quick implementation of one. */

public class AncestorAdapter implements AncestorListener
 {
	public void ancestorAdded(AncestorEvent e)
	 {
	 }

	public void ancestorRemoved(AncestorEvent e)
	 {
	 }

	public void ancestorMoved(AncestorEvent e)
	 {
	 }
 }
